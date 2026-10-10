import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {planForStep,spatialSteps,partsForModel,modelVariants} from '../lib/spatial-catalog';
import scenarios from '../../../content/learning/scenarios.json';
import models from '../public/models/spatial/manifest.json';
import scenes from '../public/models/spatial/teaching-scenes.json';
import anatomy from '../../../content/anatomy/manifest.json';
test('all current steps have explicit scenes and preserve audience and observation identity',()=>{
 let count=0;const ids=new Set(models.map(m=>m.id)),body=new Set(anatomy.parts.map(p=>p.id));
 for(const scenario of scenarios.scenarios)for(const step of scenario.steps){count++;const plan=planForStep(scenario.id,step.id);assert.ok(plan,scenario.id+'/'+step.id);assert.equal(plan.audience,scenario.audience);assert.ok(plan.model==='anatomy'||ids.has(plan.model));if(plan.teachingKey)assert.ok(plan.teachingKey in scenes);if(plan.model==='anatomy'&&plan.part)assert.ok(body.has(plan.part));if(step.part)assert.equal(plan.observationPart,step.part);if(scenario.audience==='awareness')assert.notEqual(plan.kind,'teaching');}
 assert.equal(count,94);assert.equal(Object.keys(spatialSteps).length,count);
});
test('every equipment variant and close-up point is bounded and present',()=>{
 for(const variants of Object.values(modelVariants))for(const v of variants){assert.ok(models.some(m=>m.id===v.id));const parts=partsForModel(v.id);assert.ok(parts.length>=5);for(const p of parts){assert.equal(p.point.length,3);assert.ok(p.point.every(n=>Number.isFinite(n)&&Math.abs(n)<2));assert.ok(p.distance>=.1&&p.distance<=1);}}
});
test('GLB assets are self-contained and within individual delivery budgets',()=>{
 for(const m of models){const bytes=readFileSync(new URL('../public/models/spatial/'+m.id+'.glb',import.meta.url));const view=new DataView(bytes.buffer,bytes.byteOffset,bytes.byteLength);assert.equal(bytes.length,m.bytes);assert.equal(view.getUint32(0,true),0x46546c67);assert.equal(view.getUint32(8,true),bytes.length);const json=JSON.parse(bytes.subarray(20,20+view.getUint32(12,true)).toString());assert.ok(json.buffers.every((b:{uri?:string})=>!b.uri));assert.ok(!json.images?.some((b:{uri?:string})=>b.uri));assert.ok(m.bytes<24_000_000);assert.ok(m.triangles>0&&m.triangles<100_000);}
});
import {spatialProcedures,replayRehearsal,rehearsalModel,parseRehearsal,type RehearsalEvent} from '../lib/spatial-rehearsal';
test('all 55 restored native procedure steps reject skipped or unknown actions and replay to completion',()=>{
 assert.equal(spatialProcedures.reduce((n,m)=>n+m.steps.length,0),55);
 for(const module of spatialProcedures){const events:RehearsalEvent[]=[];assert.throws(()=>replayRehearsal(module.id,[{kind:'continue'}]));assert.throws(()=>replayRehearsal(module.id,[{kind:'choose',id:'forged'}]));
  for(const step of module.steps){const bad=step.actions.find(a=>!a.correct)!;events.push({kind:'choose',id:bad.id});assert.equal(replayRehearsal(module.id,events).correct,false);events.push({kind:'choose',id:step.actions.find(a=>a.correct)!.id});assert.equal(replayRehearsal(module.id,events).correct,true);events.push({kind:'continue'});}
  const state=replayRehearsal(module.id,events);assert.equal(state.finished,true);assert.equal(state.mistakes,module.steps.length);assert.deepEqual(parseRehearsal(JSON.stringify({version:2,module:module.id,events}),module.id),events);assert.deepEqual(parseRehearsal('{broken',module.id),[]);
 }
});
test('withdrawal never depicts an active discharge; model states use real assets',()=>{
 assert.equal(rehearsalModel('fire','fire-withdraw',['fire-pin','fire-aim','fire-squeeze']),'fire-exit');
 for(const module of spatialProcedures){const done:string[]=[];for(const step of module.steps){assert.ok(models.some(m=>m.id===rehearsalModel(module.id,step.id,done)));done.push(step.actions.find(a=>a.correct)!.id);}}
});
