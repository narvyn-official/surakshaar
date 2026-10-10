"""Run with Blender --background --python scripts/anatomy/optimise_blender.py -- <repo>.
Decimate static source meshes, preserving each FMA mesh boundary. Save a review scene.
"""
import argparse,bpy,json,pathlib,sys,gzip,struct,hashlib,math
from mathutils import Vector,Matrix
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('root',type=pathlib.Path);parser.add_argument('--asset-dir',type=pathlib.Path);parser.add_argument('--review-dir',type=pathlib.Path);parser.add_argument('--profile',choices=['balanced','detail'],default='balanced');parser.add_argument('--skip-renders',action='store_true')
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:]);root=args.root;folder=args.asset_dir or root/'content/anatomy';manifest=json.loads((folder/'manifest.json').read_text())
profile_budgets={'balanced':{'skin':36000,'skeleton':63000,'muscles':110000,'organs':40000},'detail':{'skin':72000,'skeleton':110000,'muscles':215000,'organs':75000}}
budgets=profile_budgets[args.profile]
# Water-fill the layer budget, retaining complete smaller meshes before apportioning the remainder.
targets={}
for layer,budget in budgets.items():
 pending={p['id']:p['vertices']//3 for p in manifest['parts'] if p['layer']==layer}
 remaining=min(budget,sum(pending.values()))
 while pending:
  weight=sum(n**.65 for n in pending.values())
  allocation={id:remaining*(n**.65)/weight for id,n in pending.items()}
  full=[id for id,n in pending.items() if allocation[id]>=n]
  if full:
   for id in full:targets[id]=pending[id];remaining-=pending.pop(id)
   continue
  targets.update({id:int(n) for id,n in allocation.items()})
  for id in sorted(pending,key=lambda id:allocation[id]-int(allocation[id]),reverse=True)[:remaining-sum(int(n) for n in allocation.values())]:targets[id]+=1
  break
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
for part in manifest['parts']:
 assert part['file'].endswith('.meshpack'), 'Use .meshpack: Android packaging rewrites .gz filenames and bytes'
 raw=gzip.decompress((folder/pathlib.Path(part['file']).name).read_bytes());n=struct.unpack_from('<I',raw,4)[0]
 vals=struct.unpack_from('<%sf'%(n*6),raw,8);points=[];indices=[];unique={}
 for i in range(0,len(vals),6):
  key=tuple(vals[i:i+3])
  if key not in unique:unique[key]=len(points);points.append(key)
  indices.append(unique[key])
 faces=[];seen_faces=set()
 for i in range(0,len(indices),3):
  face=tuple(indices[i:i+3]);key=tuple(sorted(face))
  if len(set(face))==3 and key not in seen_faces:faces.append(face);seen_faces.add(key)
 mesh=bpy.data.meshes.new(part['id']);mesh.from_pydata(points,[],faces);mesh.validate();mesh.update()
 obj=bpy.data.objects.new(part['id']+' '+part['name'],mesh);bpy.context.collection.objects.link(obj);bpy.context.view_layer.objects.active=obj;obj.select_set(True)
 original=len(mesh.polygons);target=targets[part['id']]
 if original>target:
  mod=obj.modifiers.new('Mobile surface budget','DECIMATE');mod.ratio=target/original;mod.use_collapse_triangulate=True;bpy.ops.object.modifier_apply(modifier=mod.name)
 mesh=obj.data
 # Decimation may leave zero-area triangles. Remove them before recomputing smooth
 # normals; otherwise GLES normalize(vec3(0)) has undefined shading behavior.
 mesh.calc_loop_triangles();clean_faces=[];dropped=0
 for tri in mesh.loop_triangles:
  a,b,c=(mesh.vertices[idx].co for idx in tri.vertices)
  if (b-a).cross(c-a).length_squared<=1e-20:dropped+=1
  else:clean_faces.append(tuple(tri.vertices))
 if dropped:
  cleaned=bpy.data.meshes.new(part['id']+'-clean')
  cleaned.from_pydata([tuple(v.co) for v in mesh.vertices],[],clean_faces)
  cleaned.validate();cleaned.update();obj.data=cleaned;bpy.data.meshes.remove(mesh);mesh=cleaned
 for p in mesh.polygons:p.use_smooth=True
 mesh.update();mesh.calc_loop_triangles();values=[];fallbacks=0
 for tri in mesh.loop_triangles:
  a,b,c=(mesh.vertices[idx].co for idx in tri.vertices)
  face_normal=(b-a).cross(c-a).normalized()
  for idx in tri.vertices:
   v=mesh.vertices[idx];normal=v.normal
   if not all(math.isfinite(x) for x in normal) or normal.length_squared<=1e-20:normal=face_normal;fallbacks+=1
   else:normal=normal.normalized()
   assert all(math.isfinite(x) for x in v.co) and .99<=normal.length<=1.01,(part['id'],'invalid export geometry')
   values.extend(v.co);values.extend(normal)
 assert values,(part['id'],'empty mesh after cleanup')
 payload=b'ARA1'+struct.pack('<I',len(values)//6)+struct.pack('<%sf'%len(values),*values);data=gzip.compress(payload,9,mtime=0);(folder/pathlib.Path(part['file']).name).write_bytes(data)
 part.setdefault('sourceTriangles',original);part['inputTriangles']=original;part['triangles']=len(values)//18;part['exportCleanup']={'droppedDegenerateTriangles':dropped,'faceNormalFallbackVertices':fallbacks};part['vertices']=len(values)//6;part['bytes']=len(data);part['sha256']=hashlib.sha256(data).hexdigest();part['bounds']=[min(values[i::6]) for i in range(3)]+[max(values[i::6]) for i in range(3)]
 mat=bpy.data.materials.new(part['name']);mat.use_nodes=True;bs=mat.node_tree.nodes.get('Principled BSDF')
 # Manifest colours are display sRGB. Blender shader inputs are scene-linear.
 colour=tuple(v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in part['colour'])
 mat.diffuse_color=(*colour,1);bs.inputs['Base Color'].default_value=(*colour,1)
 bs.inputs['Roughness'].default_value={'skin':.72,'skeleton':.74,'muscles':.56,'organs':.50}[part['layer']]
 bs.inputs['Subsurface Weight'].default_value=.035 if part['layer'] in ['skin','muscles','organs'] else 0
 bs.inputs['Specular IOR Level'].default_value=.22
 mesh.materials.append(mat)
 obj['semantic_id']=part['id'];obj['layer']=part['layer'];obj.hide_render=part['layer']!='organs';obj.select_set(False)
 print(part['id'],original,'->',len(mesh.loop_triangles),'degenerate removed',dropped,'normal fallbacks',fallbacks,flush=True)
manifest['version']='bodyparts3d-4.0-mobile3-'+args.profile
manifest['quality']=args.profile
manifest['triangleBudgets']=budgets
manifest['triangleCount']=sum(p['vertices']//3 for p in manifest['parts'])
assert manifest['triangleCount']<=sum(budgets.values()),'Global mobile triangle budget exceeded'
change='Blender 5.2 LTS decimation per semantic structure and smooth-normal regeneration; no remapping of labels. Detailed source geometry is reduced; qualified visual/anatomical review remains pending.'
manifest['changes']=manifest['changes'].replace(' '+change,'').rstrip()+' '+change
cleanup='Degenerate triangles removed; exceptional zero-length vertex normals use geometric face normals.'
if cleanup not in manifest['changes']:manifest['changes']+=' '+cleanup
(folder/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE';scene.render.resolution_x=900;scene.render.resolution_y=1200;scene.render.resolution_percentage=100
scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs['Color'].default_value=(.075,.09,.12,1);scene.world.node_tree.nodes['Background'].inputs['Strength'].default_value=.35
scene.view_settings.view_transform='AgX';scene.view_settings.look='AgX - Medium High Contrast';scene.view_settings.exposure=0
scene.render.image_settings.file_format='PNG'
def aim(obj,target):
 direction=(Vector(target)-obj.location).normalized();right=direction.cross(Vector((0,1,0))).normalized();up=right.cross(direction)
 obj.rotation_euler=Matrix((right,up,-direction)).transposed().to_euler()
bpy.ops.object.camera_add(location=(0,.86,4));cam=bpy.context.object;aim(cam,(0,.86,0));cam.data.type='ORTHO';cam.data.ortho_scale=2.0;scene.camera=cam
for loc,power,size in [((-1.4,2.3,2.4),150,2.0),((1.5,.8,1.0),65,2.0),((.8,1.4,-1.5),100,1.4)]:
 bpy.ops.object.light_add(type='AREA',location=loc);light=bpy.context.object;light.data.energy=power;light.data.shape='DISK';light.data.size=size;aim(light,(0,.9,0))
out=args.review_dir or root/'artifacts/anatomy';out.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'surakshaar-body-review.blend'))
for layer in ([] if args.skip_renders else ['organs','skeleton','skin','muscles']):
 for obj in scene.objects:
  if 'layer' in obj:obj.hide_render=obj['layer']!=layer
 active=[p for p in manifest['parts'] if p['layer']==layer]
 bottom=min(p['bounds'][1] for p in active);top=max(p['bounds'][4] for p in active)
 left=min(p['bounds'][0] for p in active);right=max(p['bounds'][3] for p in active)
 middle=(bottom+top)/2;cam.location=(0,middle,4);aim(cam,(0,middle,0));cam.data.ortho_scale=max(top-bottom,(right-left)*scene.render.resolution_y/scene.render.resolution_x)*1.14
 scene.render.filepath=str(out/f'{layer}.png');bpy.ops.render.render(write_still=True)
 if layer in ['organs','muscles']:
  cam.location=(1.8,middle,3.5);aim(cam,(0,middle,0));scene.render.filepath=str(out/f'{layer}-three-quarter.png');bpy.ops.render.render(write_still=True)
print('Optimised total triangles',sum(p['vertices']//3 for p in manifest['parts']),flush=True)
