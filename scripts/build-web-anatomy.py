#!/usr/bin/env python3
"""Package the existing licensed balanced anatomy meshes for the browser viewer."""
import gzip, hashlib, json, struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / 'content/anatomy'
target = ROOT / 'apps/admin-web/public/models'
target.mkdir(parents=True, exist_ok=True)
manifest = json.loads((source / 'manifest.json').read_text())
binary = bytearray()
document = {'asset': {'version': '2.0', 'generator': 'SurakshaAR balanced anatomy packer'}, 'scene': 0,
            'scenes': [{'nodes': []}], 'nodes': [], 'meshes': [], 'materials': [], 'bufferViews': [], 'accessors': []}
catalogue = []
for part in manifest['parts']:
    # Internal anatomy is the browser teaching surface. Skin/clothing remains in the native renderer.
    if part['layer'] == 'skin':
        continue
    name = Path(part['file']).name
    if name != part['id'] + '.meshpack':
        raise ValueError('Unexpected anatomy file')
    compressed = (source / name).read_bytes()
    if len(compressed) != part['bytes'] or hashlib.sha256(compressed).hexdigest() != part['sha256']:
        raise ValueError('Anatomy integrity check failed')
    raw = gzip.decompress(compressed)
    count = struct.unpack_from('<I', raw, 4)[0]
    if raw[:4] != b'ARA1' or len(raw) != 8 + count * 24 or count % 3:
        raise ValueError('Invalid anatomy mesh')
    offset = len(binary)
    binary.extend(raw[8:])
    view = len(document['bufferViews'])
    document['bufferViews'].append({'buffer': 0, 'byteOffset': offset, 'byteLength': count * 24, 'byteStride': 24, 'target': 34962})
    position = len(document['accessors'])
    document['accessors'].extend([
        {'bufferView': view, 'byteOffset': 0, 'componentType': 5126, 'count': count, 'type': 'VEC3', 'min': part['bounds'][:3], 'max': part['bounds'][3:]},
        {'bufferView': view, 'byteOffset': 12, 'componentType': 5126, 'count': count, 'type': 'VEC3'},
    ])
    material = len(document['materials'])
    colour = part['colour'][:3] + [1]
    document['materials'].append({'name': part['layer'], 'pbrMetallicRoughness': {'baseColorFactor': colour, 'metallicFactor': 0, 'roughnessFactor': .72}, 'doubleSided': True})
    mesh = len(document['meshes'])
    document['meshes'].append({'name': part['id'], 'primitives': [{'attributes': {'POSITION': position, 'NORMAL': position + 1}, 'material': material}]})
    document['scenes'][0]['nodes'].append(len(document['nodes']))
    document['nodes'].append({'name': part['id'], 'mesh': mesh})
    catalogue.append({k: part[k] for k in ['id', 'name', 'layer', 'explanation']})
document['buffers'] = [{'byteLength': len(binary)}]
encoded = json.dumps(document, separators=(',', ':')).encode()
encoded += b' ' * (-len(encoded) % 4)
binary += b'\0' * (-len(binary) % 4)
glb = struct.pack('<4sII', b'glTF', 2, 12 + 8 + len(encoded) + 8 + len(binary)) + struct.pack('<I4s', len(encoded), b'JSON') + encoded + struct.pack('<I4s', len(binary), b'BIN\0') + binary
if len(glb) > 40_000_000:
    raise ValueError('Browser model exceeds the delivery bound')
(target / 'anatomy-balanced.glb').write_bytes(glb)
(target / 'anatomy-parts.json').write_text(json.dumps(catalogue, ensure_ascii=False))
for name in ['LICENSE.txt', 'LICENSE-LUNGS.txt']:
    (target / name).write_bytes((source / name).read_bytes())
(target / 'README.txt').write_text('SurakshaAR browser adaptation of the balanced anatomy pack. Internal anatomy only; no skin layer. Geometry and source attribution retain the terms in LICENSE.txt and LICENSE-LUNGS.txt. Original source/provenance is recorded in content/anatomy/manifest.json. Not for diagnosis or measurement of physical competence.\n')
print(f'Browser anatomy: {len(catalogue)} structures, {len(glb):,} bytes, SHA256 {hashlib.sha256(glb).hexdigest()}')
