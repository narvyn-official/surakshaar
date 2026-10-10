"""Render saved source review scenes without re-exporting or modifying checked mesh bytes."""
import argparse,json,pathlib,sys
import bpy
from mathutils import Vector,Matrix

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--scene',type=pathlib.Path,required=True)
parser.add_argument('--manifest',type=pathlib.Path,required=True)
parser.add_argument('--output',type=pathlib.Path,required=True)
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:])
bpy.ops.wm.open_mainfile(filepath=str(args.scene.resolve()))
scene=bpy.context.scene;cam=scene.camera;manifest=json.loads(args.manifest.read_text())
args.output.mkdir(parents=True,exist_ok=True)
def aim(obj,target):
 direction=(Vector(target)-obj.location).normalized();right=direction.cross(Vector((0,1,0))).normalized();up=right.cross(direction)
 obj.rotation_euler=Matrix((right,up,-direction)).transposed().to_euler()
for layer in ['organs','skeleton','skin','muscles']:
 for obj in scene.objects:
  if 'layer' in obj:obj.hide_render=obj['layer']!=layer
 parts=[p for p in manifest['parts'] if p['layer']==layer]
 lo=min(p['bounds'][1] for p in parts);hi=max(p['bounds'][4] for p in parts)
 left=min(p['bounds'][0] for p in parts);right=max(p['bounds'][3] for p in parts)
 centre=(lo+hi)/2
 cam.data.ortho_scale=max(hi-lo,(right-left)*scene.render.resolution_y/scene.render.resolution_x)*1.14
 for suffix,x,z in [('',0,4)]+([('-three-quarter',1.8,3.5)] if layer in ['organs','muscles'] else []):
  cam.location=(x,centre,z);aim(cam,(0,centre,0))
  scene.render.filepath=str(args.output/(layer+suffix+'.png'));bpy.ops.render.render(write_still=True)
