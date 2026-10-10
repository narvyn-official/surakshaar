import {realpathSync} from 'node:fs';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
const origin=process.env.BETTER_AUTH_URL??'',path=process.env.SURAKSHA_DB_PATH??'';
if(!/^http:\/\/localhost:\d+$/.test(origin)||!realpathSync(path).includes('/.runtime/safety-pilot/'))throw new Error('Run only against the isolated local Safety Updates fixture service.');
const db=new DatabaseSync(path);db.exec('PRAGMA busy_timeout=5000;');
if(db.prepare("SELECT count(*) n FROM auth_user WHERE id NOT LIKE 'safety-fixture-%'").get()!.n!==0){db.close();throw new Error('Refusing tests against non-fixture accounts.');}
const cookies:Record<string,string>={};let checks=0;
async function call(role:string|null,route='safety-updates',body?:unknown,extra:Record<string,string>={}){
 const response=await fetch(origin+'/api/'+route,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json',Origin:origin,...(role?{Cookie:cookies[role]}:{}),...extra},body:body===undefined?undefined:JSON.stringify(body)});return {status:response.status,data:await response.json() as any};
}
function status(actual:{status:number},expected:number){checks++;assert.equal(actual.status,expected);}
try{
 db.exec("DELETE FROM safety_reviews; DELETE FROM safety_jobs; DELETE FROM safety_source_audit; DELETE FROM audit_log WHERE action LIKE 'safety.%'; UPDATE safety_sources SET status='proposed',revision=1,decision_note='' WHERE id='usfa-extinguishers';");
 for(const role of ['admin','other','trainer','viewer','certifier','learner']){
  const login=()=>fetch(origin+'/api/auth/sign-in/email',{method:'POST',headers:{'Content-Type':'application/json',Origin:origin},body:JSON.stringify({email:'safety-'+role+'@example.test',password:'Local Safety Fixture 2026!'})});let r=await login();if(r.status===429){const seconds=Math.min(65,Math.max(1,Number(r.headers.get('retry-after'))||60));await r.arrayBuffer();await new Promise(resolve=>setTimeout(resolve,(seconds+1)*1000));r=await login();}assert.equal(r.status,200);cookies[role]=r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ')+( ['trainer','viewer','certifier'].includes(role)?'; suraksha_workspace=safety-fixture-admin':'');
 }
 status(await call(null),401);for(const role of ['learner','viewer','certifier']){status(await call(role),403);status(await call(role,'safety-updates',{action:'collect'}),403)}
 status(await call('trainer'),200);status(await call('admin'),200);
 const live=await call('admin','safety-updates?origin=tinyfish-fetch');status(live,200);assert(live.data.updates.every((u:{origin:string})=>u.origin==='tinyfish-fetch'));
 const fixtures=await call('admin','safety-updates?origin=fixture');status(fixtures,200);assert(fixtures.data.updates.every((u:{origin:string})=>u.origin==='fixture'));
 status(await call('admin','safety-updates?origin=invalid'),400);
 status(await call('other','safety-updates',undefined,{Cookie:cookies.other+'; suraksha_workspace=safety-fixture-admin'}),403);
 status(await call('admin','safety-updates',{action:'collect',owner:'safety-fixture-other'}),400);
 status(await call('admin','safety-updates',{action:'collect'},{Origin:'https://untrusted.example'}),400);
 status(await call('trainer','safety-updates',{action:'schedule',intervalHours:24}),403);
 status(await call('admin','safety-updates',{action:'schedule',intervalHours:48}),200);
 assert.equal(db.prepare("SELECT enabled FROM safety_schedules WHERE owner='safety-fixture-admin'").get()!.enabled,0);
 status(await call('trainer','safety-updates/sources',{action:'decide',id:'usfa-extinguishers',status:'approved',revision:1,note:'Synthetic test only'}),403);
 status(await call('other','safety-updates/sources',{action:'decide',id:'usfa-extinguishers',status:'approved',revision:1,note:'Synthetic test only'}),403);
 status(await call('admin','safety-updates/sources',{action:'propose',title:'Synthetic test source',publisher:'Synthetic test',url:'https://127.0.0.1/',topic:'fire',jurisdiction:'Test',rationale:'Synthetic test only'}),400);
 status(await call('admin','safety-updates/sources',{action:'decide',id:'usfa-extinguishers',status:'approved',revision:1,note:'LOCAL FIXTURE: test source approval only, not a real administrative decision.'}),200);
 status(await call('admin','safety-updates/sources',{action:'decide',id:'usfa-extinguishers',status:'disabled',revision:1,note:'Stale test decision'}),400);
 const job=await call('trainer','safety-updates',{action:'collect'});status(job,202);const duplicate=await call('trainer','safety-updates',{action:'collect'});status(duplicate,202);assert.equal(job.data.id,duplicate.data.id);
 assert(!(await call('other')).data.jobs.some((j:{id:string})=>j.id===job.data.id));
 status(await call('other','safety-updates',{action:'cancel',id:job.data.id}),400);status(await call('trainer','safety-updates',{action:'cancel',id:job.data.id}),200);
 const review={action:'review',id:'safety-fixture-fire',revision:0,status:'needs-review',notes:'LOCAL FIXTURE review note'};
 status(await call('trainer','safety-updates',review),200);status(await call('trainer','safety-updates',{...review,revision:1,status:'approved'}),403);
 status(await call('admin','safety-updates',{...review,revision:1,status:'approved'}),200);status(await call('admin','safety-updates',{...review,revision:1,status:'rejected'}),400);
 const other=await call('other');assert.equal(other.data.updates.find((u:{id:string})=>u.id===review.id).notes,'');assert.equal(other.data.audit.length,0);
 status(await call('trainer','safety-updates',{...review,revision:2,status:'new'}),403);
 status(await call('admin','safety-updates?status=invalid'),400);status(await call('admin','safety-updates?cursor=not-base64'),400);
 const registry=await call('admin','safety-updates/sources');assert(registry.data.audit.length>0);
 for(const table of ['attempts','credentials'])assert.equal(db.prepare(`SELECT count(*) n FROM ${table}`).get()!.n,0);
 console.log(`Safety HTTP integration passed: ${checks} status assertions plus authentication, tenant isolation, job idempotency, audit, stale review and curriculum separation checks. No provider requests made.`);
}finally{
 // Local synthetic database only: restore all shipped source proposals for the UI demonstration.
 db.exec("UPDATE safety_sources SET status='proposed',revision=1,decision_note='' WHERE id='usfa-extinguishers'; DELETE FROM safety_reviews; DELETE FROM safety_jobs; DELETE FROM safety_source_audit; DELETE FROM audit_log WHERE action LIKE 'safety.%';");db.close();
}
