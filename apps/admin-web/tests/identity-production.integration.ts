/** Runs only against a disposable local release database; never a production tenant. */
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {randomUUID} from 'node:crypto';
import {writeFileSync} from 'node:fs';
import {dirname,resolve} from 'node:path';
import {studioScenarios,studioVersion} from '../lib/studio-learning';
const origin=process.env.BETTER_AUTH_URL!,path=process.env.SURAKSHA_DB_PATH!;
assert(new URL(origin).hostname==='127.0.0.1'&&path.includes('/.runtime/spatial-studio-qa/'),'Isolated QA only');
const db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON;PRAGMA busy_timeout=5000;');
let actor=0;
async function call(route:string,body?:unknown,cookie='',ip='198.51.100.100'){
 const response=await fetch(origin+route,{method:body===undefined?'GET':'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:cookie,'X-Real-IP':ip},body:body===undefined?undefined:JSON.stringify(body)});
 const result=await response.json() as Record<string,any>;
 return {status:response.status,result,cookie:response.headers.getSetCookie().filter(c=>!c.includes('Max-Age=0')).map(c=>c.split(';')[0]).join('; ')};
}
async function user(approved=false){const email='qa-'+randomUUID()+'@example.test',password='Test '+randomUUID(),ip='198.51.100.'+(++actor);const r=await call('/api/auth/sign-up/email',{email,password,name:'Synthetic identity QA'},'',ip);assert.equal(r.status,200,JSON.stringify(r.result));const id=r.result.user.id as string;if(approved){db.prepare("INSERT INTO centre_approvals(owner,status,requested_at,reviewed_at,reviewed_by,reason) VALUES(?,'approved',?,?,'isolated-fixture','Synthetic release test')").run(id,Date.now(),Date.now());db.prepare("INSERT INTO training_centres(owner,name,site,updated_at) VALUES(?,'Identity QA Centre','Synthetic site',?)").run(id,Date.now());}return{id,email,password,cookie:r.cookie};}
async function ok(cookie:string,body:unknown){const r=await call('/api/identity',body,cookie);assert.equal(r.status,200,JSON.stringify(r.result));return r.result;}
try{
 const owner=await user(true),learner=await user(),outsider=await user(true),reviewer=await user();
 db.prepare("INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,'trainer',1,?)").run(owner.id,reviewer.email,reviewer.id,Date.now());reviewer.cookie+='; suraksha_workspace='+encodeURIComponent(owner.id);
 const workerId=randomUUID();db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run(owner.id,workerId,'Synthetic enrolled learner','Unspecified',Date.now());
 const invite=await call('/api/learners',{action:'invite',workerId,email:learner.email},owner.cookie);assert.equal(invite.status,200);
 assert.equal((await call('/api/learner',{action:'join',workerId,invitation:invite.result.invitation},learner.cookie)).status,200);
 let assignmentId='',schoolId='';const scenario=studioScenarios.find(s=>s.id==='escape-first')!;
 for(const [id,audience] of [[randomUUID(),'adult-learning'],[randomUUID(),'school-awareness']]){const assigned=await call('/api/learning-centre',{action:'assign',id,workerIds:[workerId],courseId:'fire',contentVersion:studioVersion,audience,scenarioIds:[scenario.id],mode:'guided',dueAt:Date.now()+86400000,note:'Synthetic release check'},owner.cookie);assert.equal(assigned.status,200,JSON.stringify(assigned.result));if(audience==='adult-learning')assignmentId=assigned.result.assignmentIds[0];else schoolId=assigned.result.assignmentIds[0];}
 await ok(owner.cookie,{action:'policy',enabled:true,notice:'Synthetic QA notice only. Active foreground camera captures are encrypted and visible only to assigned examiners. Contact the fixture examiner for a supervised alternative. Not a real institutional privacy notice.',support:'Synthetic centre administrator; attended alternative available',retentionDays:1});
 const start={action:'start',owner:owner.id,workerId,assignmentId,scenarioId:scenario.id,consent:true,adult:true};
 assert.equal((await call('/api/identity',{...start,consent:false},learner.cookie)).status,400);
 assert.equal((await call('/api/identity',{...start,assignmentId:schoolId},learner.cookie)).status,403);
 assert.equal((await call('/api/identity',start,outsider.cookie)).status,403);
 let s=await ok(learner.cookie,start);
 assert.equal((await call('/api/identity',{action:'decision',id:s.id,revision:0,choiceId:scenario.steps[0].choices[0].id},learner.cookie)).status,400);
 const bytes=Buffer.concat([Buffer.from([255,216,255]),Buffer.alloc(128,7),Buffer.from([255,217])]),frame={action:'frame',id:s.id,challenge:s.challenge,jpeg:bytes.toString('base64'),clientAt:Date.now()};
 s=await ok(learner.cookie,frame);assert.equal(s.frameCount,1);assert.equal((await call('/api/identity',frame,learner.cookie)).status,400);
 const stored=db.prepare('SELECT * FROM identity_frames WHERE session_id=?').get(s.id) as {id:string;ciphertext:string};assert(!stored.ciphertext.includes(frame.jpeg));
 assert.equal((await call('/api/identity?id='+s.id,undefined,outsider.cookie)).status,400);
 const forbidden=await fetch(origin+'/api/identity?staff=1&frame='+stored.id,{headers:{Cookie:outsider.cookie}});assert.equal(forbidden.status,400);
 const image=await fetch(origin+'/api/identity?staff=1&frame='+stored.id,{headers:{Cookie:owner.cookie}});assert.equal(image.status,200);assert.match(image.headers.get('cache-control')!,/no-store/);assert.deepEqual(Buffer.from(await image.arrayBuffer()),bytes);
 for(const step of scenario.steps){s=await ok(learner.cookie,{action:'decision',id:s.id,revision:s.revision,choiceId:step.choices.find(c=>c.correct)!.id});if(step.requiresObservation)s=await ok(learner.cookie,{action:'observation',id:s.id,revision:s.revision});s=await ok(learner.cookie,{action:'continue',id:s.id,revision:s.revision});}
 assert.equal(s.status,'submitted');assert.equal(s.certifiable,false);assert(db.prepare('SELECT 1 FROM learning_course_submissions WHERE owner=? AND assignment_id=? AND attempt_id=?').get(owner.id,assignmentId,s.practice.id));
 await ok(owner.cookie,{action:'review',id:s.id,outcome:'accepted',note:'Synthetic attended reference checked by fixture examiner.',referenceChecked:true});
 await ok(learner.cookie,{action:'appeal',id:s.id,note:'Synthetic learner requests a different examiner.'});
 assert.equal((await call('/api/identity',{action:'review',id:s.id,outcome:'accepted',note:'Same examiner must not resolve their own appealed decision.',referenceChecked:true},owner.cookie)).status,403);
 await ok(reviewer.cookie,{action:'review',id:s.id,outcome:'accepted',note:'Independent synthetic reference and review completed.',referenceChecked:true});
 await ok(learner.cookie,{action:'withdraw',id:s.id});assert.equal((db.prepare('SELECT count(*) AS n FROM identity_frames WHERE session_id=?').get(s.id) as {n:number}).n,0);
 const interrupted=await ok(learner.cookie,start);await ok(learner.cookie,{action:'interrupt',id:interrupted.id});assert.equal((await call('/api/identity',{...frame,id:interrupted.id,challenge:interrupted.challenge},learner.cookie)).status,400);
 const expired=await ok(learner.cookie,start);db.prepare('UPDATE identity_sessions SET expires_at=? WHERE id=?').run(Date.now()-1,expired.id);await fetch(origin+'/api/health');assert(!db.prepare('SELECT 1 FROM identity_sessions WHERE id=?').get(expired.id));
 // Preserve disposable cookies privately for browser coverage. Never package this file.
 writeFileSync(resolve(dirname(path),'browser-fixture.json'),JSON.stringify({origin,owner,learner,reviewer,assignmentId,workerId}),{mode:0o600});
 console.log('Identity integration passed: enrolment scope, adult consent, school exclusion, entry capture, replay rejection, encryption, private image access, server practice replay, review, independent appeal, withdrawal, interruption and expiry. No credential issued.');
}finally{db.close()}
