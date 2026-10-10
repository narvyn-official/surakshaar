#!/usr/bin/env python3
"""Reproducible native-geometry -> GLB pipeline. Run SpatialAssetContractTest with SURAKSHA_EXPORT_PATH first.
No downloaded assets, execution hooks, external textures or remote buffers are admitted.
"""
import json, struct, hashlib, gzip, sys
from pathlib import Path
from collections import defaultdict
ROOT=Path(__file__).resolve().parents[1]
source=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'.runtime/spatial-upgrade/meshes'
target=ROOT/'apps/admin-web/public/models/spatial';target.mkdir(parents=True,exist_ok=True)
entries=json.loads((source/'meshes.json').read_text())
class GLB:
 def __init__(self):
  self.bin=bytearray();self.doc={'asset':{'version':'2.0','generator':'SurakshaAR native geometry bridge 1'},'scene':0,'scenes':[{'nodes':[]}],'nodes':[],'meshes':[],'materials':[],'bufferViews':[],'accessors':[]};self.triangles=0
 def buffer(self,data,stride=None,target=34962):
  while len(self.bin)%4:self.bin.append(0)
  view={'buffer':0,'byteOffset':len(self.bin),'byteLength':len(data),'target':target}
  if stride:view['byteStride']=stride
  self.bin.extend(data);self.doc['bufferViews'].append(view);return len(self.doc['bufferViews'])-1
 def node(self,id,raw,group=''):
  if len(raw)%144:raise ValueError('Non-triangular data')
  groups=defaultdict(list)
  for v in struct.iter_unpack('<12f',raw):groups[v[6:]].append(v[:6])
  primitives=[]
  for mat,vertices in groups.items():
   unique={};indices=[];data=bytearray();lo=[float('inf')]*3;hi=[-float('inf')]*3
   for v in vertices:
    packed=struct.pack('<6f',*v)
    if packed not in unique:
     unique[packed]=len(unique);data.extend(packed)
     for i in range(3):lo[i]=min(lo[i],v[i]);hi[i]=max(hi[i],v[i])
    indices.append(unique[packed])
   count=len(unique);view=self.buffer(data,24);a=len(self.doc['accessors'])
   self.doc['accessors'] += [{'bufferView':view,'byteOffset':0,'componentType':5126,'count':count,'type':'VEC3','min':lo,'max':hi},{'bufferView':view,'byteOffset':12,'componentType':5126,'count':count,'type':'VEC3'}]
   indexview=self.buffer(struct.pack('<%dI'%len(indices),*indices),target=34963)
   self.doc['accessors'].append({'bufferView':indexview,'componentType':5125,'count':len(indices),'type':'SCALAR'})
   material=len(self.doc['materials']);self.doc['materials'].append({'pbrMetallicRoughness':{'baseColorFactor':list(mat[:3])+[1],'roughnessFactor':mat[3],'metallicFactor':mat[4]},'emissiveFactor':[c*mat[5] for c in mat[:3]],'doubleSided':True})
   primitives.append({'attributes':{'POSITION':a,'NORMAL':a+1},'indices':a+2,'material':material});self.triangles+=len(indices)//3
  mesh=len(self.doc['meshes']);self.doc['meshes'].append({'name':id,'primitives':primitives});self.doc['scenes'][0]['nodes'].append(len(self.doc['nodes']));self.doc['nodes'].append({'name':id,'mesh':mesh,'extras':{'group':group}})
 def save(self,name):
  self.doc['buffers']=[{'byteLength':len(self.bin)}];doc=json.dumps(self.doc,separators=(',',':')).encode();doc+=b' '*(-len(doc)%4);self.bin+=b'\0'*(-len(self.bin)%4)
  raw=struct.pack('<4sII',b'glTF',2,28+len(doc)+len(self.bin))+struct.pack('<I4s',len(doc),b'JSON')+doc+struct.pack('<I4s',len(self.bin),b'BIN\0')+self.bin
  if len(raw)>24_000_000:raise ValueError('Model exceeds asset budget')
  (target/(name+'.glb')).write_bytes(raw)
  return {'id':name,'bytes':len(raw),'triangles':self.triangles,'sha256':hashlib.sha256(raw).hexdigest()}
manifest=[];props=GLB();surface=GLB()
for e in entries:
 raw=(source/e['file']).read_bytes()
 if len(raw)!=e['vertices']*48:raise ValueError('Native export length mismatch')
 if e['id'].startswith(('teaching_','trainer-')):props.node(e['id'],raw,e['group'])
 elif e['id'].startswith(('clothing:','clothing_','training-','shorts_')):surface.node(e['id'],raw,e['group'])
 else:
  g=GLB();g.node(e['id'],raw);manifest.append(g.save(e['id']))
manifest.append(props.save('teaching-props'))
anatomy=json.loads((ROOT/'content/anatomy/manifest.json').read_text())
for p in anatomy['parts']:
 if p['layer']!='skin':continue
 compressed=(ROOT/'content/anatomy'/Path(p['file']).name).read_bytes()
 if hashlib.sha256(compressed).hexdigest()!=p['sha256']:raise ValueError('Skin integrity')
 raw=gzip.decompress(compressed);expanded=bytearray()
 for v in struct.iter_unpack('<6f',raw[8:]):expanded.extend(struct.pack('<12f',*v,*p['colour'][:3],.83,0,0))
 surface.node(p['id'],expanded,'skin')
manifest.append(surface.save('body-surface'))
# Publish skin as a selectable metadata entry; its geometry is loaded from body-surface.glb.
meta_path=target.parent/'anatomy-parts.json'
meta=json.loads(meta_path.read_text())
for p in anatomy['parts']:
 if p['layer']=='skin' and not any(a['id']==p['id'] for a in meta):meta.append({k:p[k] for k in ['id','name','layer','explanation']})
meta_path.write_text(json.dumps(meta,ensure_ascii=False))
(target/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
(target/'teaching-scenes.json').write_bytes((source/'teaching-scenes.json').read_bytes())
(target/'procedures.json').write_bytes((source/'procedures.json').read_bytes())
(target/'NOTICE.txt').write_text('Equipment and teaching prop geometry: original SurakshaAR authored educational illustrations. Generic construction, not manufacturer measurements, a repair manual, or physical skill evidence. Body surface: BodyParts3D / Anatomography, DBCLS; CC BY 4.0, see ../LICENSE.txt. Anatomy lung assets remain separately licensed under ../LICENSE-LUNGS.txt. Coordinates: metres, Y-up. Generated from native source; no external model URLs.\n')
print(len(manifest),'GLBs;',sum(x['bytes'] for x in manifest),'bytes;',sum(x['triangles'] for x in manifest),'triangles across all optional variants')
