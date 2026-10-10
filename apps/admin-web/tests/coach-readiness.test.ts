import {test} from 'node:test';
import assert from 'node:assert/strict';
import {coachReadiness,handleCoachReadiness,probeCoach} from '../lib/coach-readiness';
import {contextCoachRegistry as registry} from '../lib/context-coach-registry';
const values={COACH_EXTERNAL_ENABLED:'true',COACH_PROVIDER_URL:'https://fixture.example/classify',COACH_PROVIDER_KEY:'secret-placeholder-do-not-expose',COACH_PROVIDER_NAME:'Synthetic provider',COACH_PROVIDER_PRIVACY_URL:'https://fixture.example/privacy',COACH_PROVIDER_DATA_USE:'Synthetic testing only'};
const get=()=>new Request('https://suraksha.example/api/tutor/readiness');
const post=(body:unknown,origin?:string)=>new Request(get(),{method:'POST',headers:{'Content-Type':'application/json',...(origin?{Origin:origin}:{})},body:JSON.stringify(body)});
const command={action:'probe',confirmExternalRequest:true};
test('readiness reports only configuration presence and never credentials or false connection success',()=>{
 const reply=coachReadiness(values,registry),text=JSON.stringify(reply);assert.equal(reply.configuration,'configured');assert.equal(reply.connection,'not-tested');assert.equal(reply.productionReady,false);assert.equal(reply.reviewedScenarios,0);assert(!text.includes(values.COACH_PROVIDER_KEY));assert(!text.includes(values.COACH_PROVIDER_URL));assert.deepEqual(reply.missingFields,[]);
 assert.equal(coachReadiness({},registry).configuration,'disabled');assert.equal(coachReadiness({COACH_EXTERNAL_ENABLED:'true'},registry).configuration,'missing-config');assert.equal(coachReadiness({...values,COACH_PROVIDER_URL:'http://localhost'},registry).configuration,'invalid-config');
});
test('admin authorization precedes configuration inspection, budget and probe',async()=>{
 let touched=false;const deps={registry,async authorizeAdmin(){throw new Error('Forbidden admin role');},values(){touched=true;return values;},async budget(){touched=true;return null;}};
 for(const request of [get(),post(command)])await assert.rejects(handleCoachReadiness(request,deps),/Forbidden/);assert.equal(touched,false);
});
test('readiness GET makes no model calls and uses no-store',async()=>{
 let calls=0;const reply=await handleCoachReadiness(get(),{registry,async authorizeAdmin(){return{accountId:'admin'};},values:()=>values,async budget(){calls++;return null;},provider(){calls++;return{status:'disabled'};}});assert.equal(calls,0);assert.equal(reply.headers.get('cache-control'),'no-store');assert.equal(((await reply.json()) as {connection:string}).connection,'not-tested');
});
test('probe sends only synthetic controls and accepts allowlisted card selection',async()=>{
 let calls=0;const result=await probeCoach({status:'configured',provider:{async select(input){calls++;assert.deepEqual(input.context.recentActionIds,[]);assert.equal(input.context.hintLevel,'controls');assert.equal(Object.hasOwn(input.context,'workerId'),false);assert(input.candidates.every(c=>c.explanationId.startsWith('controls-')));return{explanationId:input.candidates[0].explanationId,actionIds:input.candidates[0].actionIds};}}},registry,new AbortController().signal);
 assert.equal(calls,1);assert.equal(result.status,'passed');assert.equal(result.learnerDataSent,false);assert.equal(result.safetyApproval,false);assert.equal(result.productionReady,false);
});
test('missing or invalid provider output is unavailable rather than a passing probe',async()=>{
 assert.equal((await probeCoach({status:'missing-config'},registry,new AbortController().signal)).status,'unavailable');
 for(const output of [{explanationId:'diagnose',actionIds:[]},{explanationId:'controls-return-en',actionIds:['show-step'],text:'unreviewed treatment'}])assert.equal((await probeCoach({status:'configured',provider:{async select(){return output;}}},registry,new AbortController().signal)).status,'unavailable');
});
test('probe requires explicit confirmation and disallows supplied learner data or cross-origin requests',async()=>{
 let spent=0;const deps={registry,async authorizeAdmin(){return{accountId:'admin'};},values:()=>values,async budget(){spent++;return null;}};
 for(const body of [{action:'probe'}, {...command,workerId:'someone'}, {...command,confirmExternalRequest:false}])await assert.rejects(handleCoachReadiness(post(body),deps),/Invalid/);
 await assert.rejects(handleCoachReadiness(post(command,'https://attacker.example'),deps),/origin/);assert.equal(spent,0);
});
test('explicit probes honor the shared tutor rate limit before reading provider configuration',async()=>{
 let touched=false;const reply=await handleCoachReadiness(post(command),{registry,async authorizeAdmin(){return{accountId:'admin'};},values(){touched=true;return values;},async budget(){return new Response(null,{status:429});}});assert.equal(reply.status,429);assert.equal(touched,false);
});
