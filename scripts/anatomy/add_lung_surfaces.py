#!/usr/bin/env python3
"""Add official, separately licensed 4.3 lung parenchyma to the 4.0 base pack.
Run AFTER build_pack.py and BEFORE optimise_blender.py. Inputs remain original source coordinates.
"""
import argparse,pathlib,sys,json,gzip,struct,hashlib,zipfile,math
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('source',type=pathlib.Path);parser.add_argument('lungs',type=pathlib.Path);parser.add_argument('--output-dir',type=pathlib.Path)
args=parser.parse_args();root=pathlib.Path(__file__).resolve().parents[2];src=args.source;lung=args.lungs;out=args.output_dir or root/'content/anatomy'
z=zipfile.ZipFile(src/'isa_BP3D_4.0_obj_99.zip');skin=z.read('isa_BP3D_4.0_obj_99/FJ2810.obj').decode()
pts=[list(map(float,x.split()[1:4])) for x in skin.splitlines() if x.startswith('v ')]
lo=[min(p[i] for p in pts) for i in range(3)];hi=[max(p[i] for p in pts) for i in range(3)];cx=(lo[0]+hi[0])/2;cy=(lo[1]+hi[1])/2
m=json.loads((out/'manifest.json').read_text());lm=json.loads((lung/'lung-manifest.json').read_text())
for side,id in [('left','FMA7310'),('right','FMA7309')]:
 vals=[];sources=[]
 for part in lm['parts']:
  if part['side']!=side:continue
  path=lung/'objs'/pathlib.Path(part['file']).name
  assert hashlib.sha256(path.read_bytes()).hexdigest()==part['fileSha256']
  vs=[];ns=[];faces=[]
  for line in path.read_text().splitlines():
   t=line.split()
   if not t:continue
   if t[0]=='v':vs.append(tuple(map(float,t[1:4])))
   elif t[0]=='vn':ns.append(tuple(map(float,t[1:4])))
   elif t[0]=='f':
    v=[tuple(int(n) if n else 0 for n in a.split('/')) for a in t[1:]]
    faces.extend([[v[0],v[k],v[k+1]] for k in range(1,len(v)-1)])
  for face in faces:
   p=[vs[t[0]-1] for t in face];a=[p[1][i]-p[0][i] for i in range(3)];b=[p[2][i]-p[0][i] for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
   for point,index in zip(p,face):
    n=ns[index[2]-1] if len(index)>2 and index[2]>0 else normal;length=math.sqrt(sum(v*v for v in n))or 1
    vals.extend([(point[0]-cx)*.001,(point[2]-lo[2])*.001,-(point[1]-cy)*.001,n[0]/length,n[2]/length,-n[1]/length])
  sources.append({k:part[k] for k in ['fj_id','fma_id','name','fileSha256']})
 payload=gzip.compress(b'ARA1'+struct.pack('<I',len(vals)//6)+struct.pack('<%sf'%len(vals),*vals),9,mtime=0);(out/f'{id}.meshpack').write_bytes(payload)
 p=next(p for p in m['parts'] if p['id']==id)
 p.update(name=side+' lung',file=f'anatomy/{id}.meshpack',vertices=len(vals)//6,bytes=len(payload),sha256=hashlib.sha256(payload).hexdigest(),bounds=[min(vals[i::6]) for i in range(3)]+[max(vals[i::6]) for i in range(3)],sourceElements=[p['fj_id'] for p in sources],sourceParts=sources,sourceVersion='4.3',source=lm['source'],licence=lm['licence'],licenceUrl=lm['licenceUrl'],attribution=lm['attribution'],changes='Nine original parenchymal segments grouped per lung, transformed from millimetres to metres in the unchanged shared body coordinate frame. Mobile decimation follows. No manual anatomical fitting.')
 p.pop('sourceTriangles',None)
 p['explanation']=f'The {side} lung is on the person’s {side}. This surface combines nine source parenchymal segments. It is static adult anatomy, not a breathing simulation. Abnormal gasping is not normal breathing.'
m['additionalSources']=[{k:lm[k] for k in ['sourceVersion','source','mappingSource','downloadEndpoint','downloadSha256','licence','licenceUrl','licenceEvidence','attribution']}]
m['limitations']=[s for s in m['limitations'] if not s.startswith('Lung groups currently')]
m['limitations'].append('Lung surfaces are a separately licensed 4.3 subset in the 4.x coordinate system; qualified alignment and anatomical review remain pending.')
(out/'manifest.json').write_text(json.dumps(m,indent=2)+'\n')
(out/'LICENSE-LUNGS.txt').write_text((lung/'LICENSE.txt').read_text()+'\nDistributed derivatives: grouped per lung, transformed to metres and decimated for mobile rendering. SurakshaAR lung derivatives retain CC BY-SA 2.1 JP.\n')
with(out/'LICENSE.txt').open('a') as f:f.write('\nEXCEPTION: FMA7309.meshpack and FMA7310.meshpack are derivatives of separately downloaded BodyParts3D 4.3 lung assets under CC BY-SA 2.1 Japan. See LICENSE-LUNGS.txt; the default CC BY 4.0 notice does not replace their ShareAlike terms.\n')
print('Added left and right lung surfaces with separate ShareAlike notices')
