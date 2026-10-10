#!/usr/bin/env python3
"""Reproducible BodyParts3D OBJ -> GLES interleaved position/normal pack.
No fabricated geometry; conversion preserves source FMA IDs and reports omitted coverage.
Usage: python3 scripts/anatomy/build_pack.py /tmp/surakshaar-anatomy-source
"""
import argparse,csv,gzip,hashlib,json,math,pathlib,struct,sys,zipfile
from collections import defaultdict
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('source',type=pathlib.Path);parser.add_argument('--output-dir',type=pathlib.Path)
args=parser.parse_args();src=args.source;out=args.output_dir or pathlib.Path(__file__).resolve().parents[2]/'content/anatomy';out.mkdir(parents=True,exist_ok=True)
names={};groups={};group_trees={}
for kind in ['isa','partof']:
 for row in list(csv.reader((src/f'{kind}_parts_list_e.txt').open(),delimiter='\t'))[1:]:names[row[0]]=row[-1]
 g=defaultdict(set)
 for row in list(csv.reader((src/f'{kind}_element_parts.txt').open(),delimiter='\t'))[1:]:g[row[0]].add(row[-1])
 group_trees[kind]=g
 for id,files in g.items():groups[id]=files
z=zipfile.ZipFile(src/'isa_BP3D_4.0_obj_99.zip');files={pathlib.PurePosixPath(n).stem:n for n in z.namelist() if n.endswith('.obj')}
parsed={}
def obj(id):
 if id in parsed:return parsed[id]
 v=[];n=[];f=[]
 for line in z.read(files[id]).decode().splitlines():
  p=line.split()
  if not p:continue
  if p[0]=='v':v.append(tuple(map(float,p[1:4])))
  elif p[0]=='vn':n.append(tuple(map(float,p[1:4])))
  elif p[0]=='f':
   face=[tuple(int(t) if t else 0 for t in x.split('/')) for x in p[1:]]
   for k in range(1,len(face)-1):f.append([face[0],face[k],face[k+1]])
 parsed[id]=(v,n,f);return parsed[id]
# Source CT coordinates are mm, z superior. Establish anatomical left from named kidneys.
left=[p[0] for f in groups['FMA7205'] for p in obj(f)[0]]
right=[p[0] for f in groups['FMA7204'] for p in obj(f)[0]]
hand=1 if sum(left)/len(left)>sum(right)/len(right) else -1
skin=[p for f in groups['FMA7163'] for p in obj(f)[0]]
mins=[min(p[i] for p in skin) for i in range(3)];maxs=[max(p[i] for p in skin) for i in range(3)]
cx=(mins[0]+maxs[0])/2;cy=(mins[1]+maxs[1])/2
# X anatomical left, Y superior, Z anterior. Original anterior verified by sternum vs vertebral column.
def mean_y(id):
 a=[p[1] for f in groups[id] for p in obj(f)[0]];return sum(a)/len(a)
front=1 if mean_y('FMA7485')>mean_y('FMA13478') else -1
# Winding reverses if coordinate determinant is negative; transform normals accordingly.
determinant=-hand*front
selected=[];used=set()
def add(id,layer,explicit=None,label=None):
 ids=(groups.get(id,set()) if explicit is None else explicit)-used
 ids={f for f in ids if f in files}
 if not ids:return
 used.update(ids);selected.append((id,label or names.get(id,id),layer,sorted(ids)))
for id in ['FMA7163']:add(id,'skin')
for id in ['FMA7485','FMA23130','FMA23131','FMA24474','FMA24475','FMA23464','FMA23465','FMA24477','FMA24478','FMA7480','FMA13478']:add(id,'skeleton')
add('FMA23881','skeleton',groups['FMA23881']|groups['FMA5018'])
for id in ['FMA7394','FMA7309','FMA7310','FMA7088','FMA3734','FMA50801','FMA7647','FMA13295','FMA7197','FMA7204','FMA7205']:add(id,'organs')
# Named muscle subdivisions retain their IDs; no claim of a complete muscular system.
terms=['pectoralis','deltoid','biceps brachii','triceps brachii','trapezius','gluteus maximus','rectus femoris','vastus lateralis','tibialis anterior','gastrocnemius','soleus','latissimus dorsi','rectus abdominis']
for id,name in names.items():
 if ('right' in name or 'left' in name) and any(t in name for t in terms) and not any(t in name for t in ['artery','vein','nerve','zone','insertion','origin']):add(id,'muscles')
extra_terms=['external oblique','sternocleidomastoid','sartorius','semitendinosus','semimembranosus','adductor longus','adductor magnus','brachioradialis','vastus medialis','vastus intermedius','biceps femoris','brachialis','serratus anterior','infraspinatus','supraspinatus','teres major','teres minor','gluteus medius','gluteus minimus']
for id,name in names.items():
 if ('right' in name or 'left' in name) and any(t in name for t in extra_terms) and not any(t in name for t in ['artery','vein','nerve','zone','insertion','origin','tendon']):add(id,'muscles')
# Regional source groups fill omitted hand, forearm, neck, back and leg coverage.
# Their labels explicitly identify a remainder; no individual muscle is invented or relabelled.
for id in ['FMA9616','FMA9617','FMA9619','FMA9620','FMA9621','FMA9622','FMA19086','FMA22594']:
 add(id,'muscles',group_trees['isa'][id],label='Additional source '+names[id])
manifest=dict(version='bodyparts3d-4.0-mobile1',source='https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html',licence='CC-BY-4.0',licenceUrl='https://dbarchive.biosciencedbc.jp/en/bodyparts3d/lic.html',attribution='BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International',sourceArchiveSha256=hashlib.sha256((src/'isa_BP3D_4.0_obj_99.zip').read_bytes()).hexdigest(),changes='Selected structures; original low-resolution OBJ triangulation converted to metre-based position/normal buffers; named structures grouped without remeshing. Original CT pose retained.',units='metres',axes='X anatomical left, Y superior, Z anterior',heightMetres=(maxs[2]-mins[2])*.001,reviewStatus='Qualified anatomy review pending',limitations=['Single adult source anatomy; no child or infant model.','Lung groups currently show branching airway/vessel structures, not a complete outer lung surface.','Low-resolution surfaces; no skin histology, injury simulation, rigging or validated physiological animation.','Skeletal system includes a remainder group; not every bone has an individual label.','Muscle coverage limited to listed parts.','Colour is illustrative; original scan-derived surfaces have no photographic texture.','No clinical assessment or certification.'],parts=[])
manifest['limitations']=[s for s in manifest['limitations'] if s!='Muscle coverage limited to listed parts.']
manifest['limitations'].append('Expanded regional muscle coverage is source-limited; grouped remnants are not individually labelled. This is not a complete anatomical atlas.')
manifest['coverage']={'muscleSourceElements':sum(len(ids) for _,_,layer,ids in selected if layer=='muscles'),'muscleGroups':sum(layer=='muscles' for _,_,layer,_ in selected),'anatomicalProportions':'Original scan coordinate frame retained; no cosmetic reshaping or mirrored replacement anatomy.'}
colours={'skin':[.55,.33,.21],'skeleton':[.82,.78,.65],'muscles':[.54,.19,.18],'organs':[.61,.30,.31]}
organ_colours={'FMA7394':[.78,.70,.58],'FMA7309':[.68,.40,.43],'FMA7310':[.68,.40,.43],'FMA7088':[.60,.19,.22],'FMA3734':[.60,.16,.17],'FMA50801':[.78,.63,.59],'FMA7647':[.80,.72,.59],'FMA13295':[.61,.24,.25],'FMA7197':[.37,.16,.14],'FMA7204':[.49,.20,.17],'FMA7205':[.49,.20,.17]}
explanations={'FMA7485':'The sternum is the breastbone at the front of the chest. It is a location landmark, not a measure of compression depth.','FMA7394':'The trachea carries air towards the lungs. This isolated view explains location; it does not establish whether a real airway is open.','FMA7309':'The right lung is on the person’s right, which appears on your left in a front view. Abnormal gasping is not normal breathing.','FMA7310':'The left lung is on the person’s left. The two lungs have different shapes; they are not mirrored duplicates.','FMA7088':'The heart lies between the lungs, mostly behind the sternum. This static surface does not simulate a heart rhythm.','FMA50801':'The brain depends on circulation and oxygen. This model cannot diagnose injury or responsiveness.','FMA13295':'The diaphragm separates chest and abdomen and participates in breathing. This pack shows its shape without a physiological simulation.'}
for id,name,layer,ids in selected:
 if id in ['FMA7309','FMA7310']:name='branching structures of '+name
 values=[]
 for mesh in ids:
  vs,ns,fs=obj(mesh)
  for face in fs:
   if determinant<0:face=list(reversed(face))
   ps=[vs[t[0]-1] for t in face]
   a=[ps[1][i]-ps[0][i] for i in range(3)];b=[ps[2][i]-ps[0][i] for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
   if determinant<0:normal=[-n for n in normal]
   for p,t in zip(ps,face):
    n=ns[t[2]-1] if len(t)>2 and t[2]>0 else normal
    length=math.sqrt(sum(x*x for x in n)) or 1
    values.extend([(p[0]-cx)*.001*hand,(p[2]-mins[2])*.001,(p[1]-cy)*.001*front,n[0]/length*hand,n[2]/length,n[1]/length*front])
 bounds=[min(values[i::6]) for i in range(3)]+[max(values[i::6]) for i in range(3)]
 payload=b'ARA1'+struct.pack('<I',len(values)//6)+struct.pack('<%sf'%len(values),*values)
 # A neutral extension prevents AAPT from transparently expanding a .gz asset.
 data=gzip.compress(payload,compresslevel=9,mtime=0);(out/f'{id}.meshpack').write_bytes(data)
 c=organ_colours.get(id,colours[layer])
 if layer=='muscles':
  variation=(int(hashlib.sha256(id.encode()).hexdigest()[:2],16)/255-.5)*.045
  c=[c[0]+variation,c[1]+variation*.5,c[2]+variation*.4]
 manifest['parts'].append(dict(id=id,name=name,layer=layer,file=f'anatomy/{id}.meshpack',bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),vertices=len(values)//6,bounds=bounds,colour=c,sourceElements=ids,explanation=explanations.get(id,f'{name.capitalize()} — source-labelled adult anatomy. Left and right refer to the person, not the viewer. Detailed clinical interpretation is pending qualified review.')))
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
for obsolete in out.glob('*.meshpack'):
 if obsolete.stem not in {part['id'] for part in manifest['parts']}:obsolete.unlink()
(out/'LICENSE.txt').write_text(manifest['attribution']+'\n\nCC BY 4.0 International: https://creativecommons.org/licenses/by/4.0/\nDataset permission: '+manifest['licenceUrl']+'\nLicence update checked 2026-10-10.\n'+manifest['changes']+'\nNo endorsement is implied.\n')
print('Parts',len(manifest['parts']),'triangles',sum(p['vertices']//3 for p in manifest['parts']),'gzip bytes',sum(p['bytes'] for p in manifest['parts']),'height',manifest['heightMetres'],'orientation',hand,front)
