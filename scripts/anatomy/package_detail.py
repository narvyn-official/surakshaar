#!/usr/bin/env python3
"""Create a deterministic, offline optional anatomy archive from a validated staged pack.

The original source meshes never enter the APK twice: only this archive and its
pinned catalogue are published to content/anatomy/optional/.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import zipfile

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('learning_validator',ROOT/'scripts/validate_learning_release.py')
validator=importlib.util.module_from_spec(spec);spec.loader.exec_module(validator)


def package(base:Path,detail:Path,output:Path)->dict:
    base_manifest=json.loads((base/'manifest.json').read_text())
    manifest_bytes=(detail/'manifest.json').read_bytes()
    manifest=json.loads(manifest_bytes)
    validator.validate_licensing(base_manifest,base.parent)
    validator.validate_licensing(manifest,detail.parent)
    assert manifest.get('quality')=='detail','Only a detailed-quality manifest belongs in this archive'
    expected={p['id']:p for p in base_manifest['parts']}
    actual={p['id']:p for p in manifest['parts']}
    assert len(actual)==len(manifest['parts']) and expected.keys()==actual.keys(),'Quality packs must have identical semantic IDs'
    max_delta=0.0
    for id,part in actual.items():
        reference=expected[id]
        for key in ('name','layer','sourceElements','sourceVersion','licence','licenceUrl'):
            assert reference.get(key)==part.get(key),(id,'quality changed semantics or rights',key)
        max_delta=max(max_delta,max(abs(a-b) for a,b in zip(reference['bounds'],part['bounds'])))
        validator.validate_mesh(part,detail.parent)
    # Decimation may shift extremal vertices slightly, never translate or re-scale a structure.
    assert max_delta<=.015,('Unexpected quality-pack alignment change in metres',max_delta)
    assert sum(p['vertices']//3 for p in manifest['parts'])<=500_000,'Detailed mobile budget exceeded'
    files={'manifest.json':manifest_bytes}
    for part in manifest['parts']:
        path=Path(part['file'])
        assert path.parent==Path('anatomy') and path.suffix=='.meshpack','Unexpected archive path'
        files[str(path)]=(detail/path.name).read_bytes()
    for name in ('LICENSE.txt','LICENSE-LUNGS.txt'):
        files[name]=(detail/name).read_bytes()
    output.mkdir(parents=True,exist_ok=True)
    archive=output/'body-detail.zip'
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_STORED) as stream:
        for name,data in sorted(files.items()):
            entry=zipfile.ZipInfo(name,date_time=(2026,10,10,0,0,0));entry.external_attr=0o100644<<16
            stream.writestr(entry,data)
    contents=archive.read_bytes()
    catalog={
        'id':'body-detail','version':manifest['version'],
        'archive':'anatomy/optional/body-detail.zip','bytes':len(contents),
        'sha256':hashlib.sha256(contents).hexdigest(),
        'unpackedBytes':sum(map(len,files.values())),
        'manifestSha256':hashlib.sha256(manifest_bytes).hexdigest(),
        'title':'Detailed anatomy',
        'description':'Install the bundled detailed model for closer exploration. Uses more storage and memory; clinical review remains pending.',
        'partCount':len(actual),'triangleCount':sum(p['vertices']//3 for p in manifest['parts']),
        'compatibleBaseVersion':base_manifest['version'],
        'maxPartBoundsDeltaMetres':max_delta,
        'licences':['CC-BY-4.0','CC-BY-SA-2.1-JP']
    }
    (output/'catalog.json').write_text(json.dumps(catalog,indent=2)+'\n')
    return catalog


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base',type=Path,required=True)
    parser.add_argument('--detail',type=Path,required=True)
    parser.add_argument('--output',type=Path,default=ROOT/'content/anatomy/optional')
    args=parser.parse_args()
    print(json.dumps(package(args.base,args.detail,args.output),indent=2))
