/** Isolated synthetic HTTP checks. Never run against a live institution. */
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {randomUUID} from 'node:crypto';
import {writeFileSync} from 'node:fs';
import {dirname} from 'node:path';
import {studioScenarios,studioVersion} from '../lib/studio-learning';
import {defaultPolicy} from '../lib/institution';
const origin=process.env.BETTER_AUTH_URL!,path=process.env.SURAKSHA_DB_PATH!;
assert(origin==='http://127.0.0.1:5196'&&path.includes('/.runtime/institution-0.17/'));
const db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON;PRAGMA busy_timeout=5000');let checks=0,ip=0;
type Actor={id:string;email:string;password:string;cookie:string};
async function call(actor:Actor|null,route:string,body?:unknown,expected=200){const r=await fetch(origin+route,{method:body===undefined?'GET':'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:actor?.cookie??'','X-Real-IP':'198.51.100.'+((++ip%200)+1)},body:body===undefined?undefined:JSON.stringify(body)}),value=await r.json() as Record<string,any>;assert.equal(r.status,expected,route+' '+JSON.stringify(value));checks++;return {value,cookie:r.headers.getSetCookie().filter(c=>!c.includes('Max-Age=0')).map(c=>c.split(';')[0]).join('; ')};}
async function user(label:string){const email='qa-'+label+'-'+randomUUID()+'@example.test',password='Synthetic '+randomUUID(),r=await call(null,'/api/auth/sign-up/email',{email,password,name:'Synthetic '+label});return{id:r.value.user.id,email,password,cookie:r.cookie};}
async function approved(actor:Actor){db.prepare('INSERT INTO training_centres VALUES(?,?,?,?)').run(actor.id,'Synthetic institute '+actor.id.slice(0,4),'Synthetic only',Date.now());db.prepare("INSERT INTO centre_approvals(owner,status,requested_at,reason) VALUES(?,'approved',?,'Synthetic fixture')").run(actor.id,Date.now());}
try{
 const admin=await user('admin'),learner=await user('learner'),other=await user('other');await approved(admin);await approved(other);
 const actors:Record<string,Actor>={admin,learner,other};
 for(const role of ['trainer','examiner','certifier','assessor','author','reviewer','viewer']){const actor=await user(role);db.prepare('INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,?,1,?)').run(admin.id,actor.email,actor.id,role,Date.now());actor.cookie+='; suraksha_workspace='+encodeURIComponent(admin.id);actors[role]=actor;}
 for(const role of ['author','reviewer']){await call(actors[role],'/api/records',undefined,403);await call(actors[role],'/api/institution');await call(actors[role],'/api/identity?staff=1',undefined,403);}
 await call(actors.trainer,'/api/identity?staff=1',undefined,403);await call(actors.certifier,'/api/identity?staff=1',undefined,403);await call(actors.examiner,'/api/identity?staff=1');await call(learner,'/api/institution',undefined,403);
 const unit=randomUUID();await call(admin,'/api/institution',{action:'unit',id:unit,kind:'cohort',name:'Synthetic cohort',parentId:null});
 await call(other,'/api/institution',{action:'unit',id:randomUUID(),kind:'department',name:'Cross-tenant',parentId:unit},400);
 const batch={action:'import',id:randomUUID(),people:[{employeeNumber:'TEST-001',name:'Synthetic learner',sector:'Other'}],unitId:unit},imported=(await call(admin,'/api/institution',batch)).value,workerId=imported.workerIds[0];
 await call(admin,'/api/institution',batch);await call(admin,'/api/institution',{...batch,id:randomUUID()},400);await call(actors.examiner,'/api/institution',batch,403);
 const invite=(await call(admin,'/api/learners',{action:'invite',workerId,email:learner.email})).value;await call(learner,'/api/learner',{action:'join',workerId,invitation:invite.invitation});
 await call(admin,'/api/identity',{action:'policy',enabled:true,notice:'Synthetic fixture identity notice. Only active foreground pictures are captured for practice and reviewed by synthetic examiners. No real clinical or identity validation is performed in this test.',support:'Fixture administrator; supervised alternative available',retentionDays:7});
 await call(admin,'/api/institution',{action:'policy',previousVersion:0,policy:{...defaultPolicy,name:'Synthetic supervised policy',assessment:'snapshots',requirePractical:true}});
 await call(admin,'/api/institution',{action:'policy',previousVersion:0,policy:defaultPolicy},400);
 const scenario=studioScenarios.find(s=>s.id==='escape-first')!,assignment=(await call(actors.trainer,'/api/learning-centre',{action:'assign',id:randomUUID(),workerIds:[workerId],courseId:'fire',contentVersion:studioVersion,audience:'adult-learning',scenarioIds:[scenario.id],mode:'guided',dueAt:Date.now()+86400000,note:'Synthetic assignment'})).value.assignmentIds[0];
 const start={action:'start',owner:admin.id,workerId,assignmentId:assignment,scenarioId:scenario.id,adult:true,consent:true};await call(learner,'/api/identity',start,400);
 const jpeg=Buffer.concat([Buffer.from([255,216,255]),Buffer.alloc(128,7),Buffer.from([255,217])]).toString('base64');
 const reference=(await call(learner,'/api/identity-reference',{action:'submit',owner:admin.id,workerId,jpeg,adult:true,acknowledged:true})).value.id;
 await call(learner,'/api/identity',start,400);await call(other,'/api/identity-reference',{action:'review',id:reference,outcome:'approved',note:'Synthetic attended identity check fixture.',attendedCheck:true},400);
 await call(admin,'/api/identity-reference',{action:'review',id:reference,outcome:'approved',note:'Synthetic attended identity check fixture.',attendedCheck:true});
 let session=(await call(learner,'/api/identity',start)).value;assert.equal(db.prepare('SELECT reference_id FROM identity_sessions WHERE id=?').get(session.id)?.reference_id,reference);
 session=(await call(learner,'/api/identity',{action:'frame',id:session.id,challenge:session.challenge,jpeg,clientAt:Date.now()})).value;
 const frame=db.prepare('SELECT id FROM identity_frames WHERE session_id=?').get(session.id)!.id;
 await call(admin,'/api/identity?staff=1&frame='+frame,undefined,403);
 for(const step of scenario.steps){session=(await call(learner,'/api/identity',{action:'decision',id:session.id,revision:session.revision,choiceId:step.choices.find(c=>c.correct)!.id})).value;if(step.requiresObservation)session=(await call(learner,'/api/identity',{action:'observation',id:session.id,revision:session.revision})).value;session=(await call(learner,'/api/identity',{action:'continue',id:session.id,revision:session.revision})).value;}
 assert.equal(session.status,'active');assert.equal(session.awaitingFinalCapture,true);session=(await call(learner,'/api/identity',{action:'frame',id:session.id,challenge:session.challenge,jpeg,clientAt:Date.now()})).value;
 assert.equal(session.status,'submitted');await call(actors.examiner,'/api/identity',{action:'review',id:session.id,outcome:'accepted',note:'Synthetic reference and session reviewed.',referenceChecked:true});
 const submission=db.prepare('SELECT id FROM learning_course_submissions WHERE owner=? AND assignment_id=?').get(admin.id,assignment)!.id;
 await call(actors.examiner,'/api/learning-centre',{action:'review',id:randomUUID(),submissionId:submission,previousEventId:null,outcome:'reviewed',note:'Synthetic formative work reviewed against assignment.'});
 const award={action:'issue',id:randomUUID(),assignmentId:assignment,reason:'Synthetic course record for isolated integration validation.'};
 await call(actors.certifier,'/api/institution-awards',award,400);
 await call(actors.trainer,'/api/learning-centre',{action:'observe',id:randomUUID(),assignmentId:assignment,observedAt:Date.now(),setting:'in-person-training',rubric:{communication:true,sequence:true,safeDecisions:true},outcome:'observed',note:'Synthetic attended observation; no real person or skill assessed.'});
 await call(actors.trainer,'/api/institution-awards',award,403);await call(actors.certifier,'/api/institution-awards',award);await call(actors.certifier,'/api/institution-awards',award);
 const publicRecord=(await call(null,'/api/public-award?id='+award.id)).value;assert.equal(publicRecord.status,'Active');assert.equal(publicRecord.signatureVerified,true);assert.equal(publicRecord.worker_id,undefined);assert.equal(publicRecord.evidence,undefined);
 await call(learner,'/api/institution-awards?learner=1');const otherAwards=(await call(other,'/api/institution-awards')).value;assert.equal(otherAwards.awards.length,0);
 const replacement={...award,id:randomUUID(),replaceId:award.id};await call(actors.certifier,'/api/institution-awards',replacement);assert.equal((await call(null,'/api/public-award?id='+award.id)).value.status,'Replaced');
 await call(admin,'/api/institution-awards',{action:'rotate-key',reason:'Synthetic rotation test preserves historical signatures.'});assert.equal((await call(null,'/api/public-award?id='+replacement.id)).value.signatureVerified,true);
 await call(actors.certifier,'/api/institution-awards',{action:'revoke',id:replacement.id,reason:'Synthetic revocation of a test record.'});assert.equal((await call(null,'/api/public-award?id='+replacement.id)).value.status,'Revoked');
 const procedure={action:'procedure',id:randomUUID(),title:'Synthetic local procedure',body:'Synthetic training fixture. This is deliberately not an operational safety instruction. Use this record only to test author and reviewer separation.',sources:'Synthetic fixture specification, October 2026',language:'en',submit:true};await call(actors.author,'/api/institution',procedure);await call(actors.author,'/api/institution',{action:'procedure-review',id:procedure.id,revision:1,outcome:'approved',note:'Synthetic reviewer approval for test only.',qualification:'Synthetic test reviewer'},403);
 await call(actors.reviewer,'/api/institution',{action:'procedure-review',id:procedure.id,revision:1,outcome:'approved',note:'Synthetic reviewer approval for test only.',qualification:'Synthetic test reviewer'});
 await call(actors.reviewer,'/api/institution',{action:'procedure-review',id:procedure.id,revision:1,outcome:'withdrawn',note:'Stale revision must be rejected safely.',qualification:'Synthetic test reviewer'},400);
 await call(actors.reviewer,'/api/institution',{action:'procedure-review',id:procedure.id,revision:2,outcome:'withdrawn',note:'Synthetic withdrawal after testing approval.',qualification:'Synthetic test reviewer'});
 const programme={id:randomUUID(),cohortId:unit,courseId:'fire',contentVersion:studioVersion,audience:'adult-learning',scenarioIds:[scenario.id],mode:'guided',dueAt:Date.now()+86400000,note:'Synthetic cohort programme'};
 await call(actors.trainer,'/api/institution-programme',programme);await call(actors.trainer,'/api/institution-programme',programme);await call(other,'/api/institution-programme',programme,400);await call(actors.examiner,'/api/institution-programme',programme,403);
 const support=await user('supporter');actors.supporter=support;
 await call(other,'/api/learning-support',{action:'invite',workerId,label:'Synthetic profile',email:support.email,acknowledged:true},403);
 const supportCode=(await call(learner,'/api/learning-support',{action:'invite',workerId,label:'Synthetic profile',email:support.email,acknowledged:true})).value.code;
 await call(other,'/api/learning-support',{action:'accept',code:supportCode},400);await call(support,'/api/learning-support',{action:'accept',code:supportCode});await call(support,'/api/learning-support',{action:'accept',code:supportCode},400);
 const supportRows=(await call(support,'/api/learning-support')).value.supporting;assert.equal(supportRows.length,1);assert.equal(supportRows[0].payload,undefined);assert.equal(supportRows[0].frames,undefined);
 await call(learner,'/api/learning-support',{action:'revoke',id:supportRows[0].id});assert.equal((await call(support,'/api/learning-support')).value.supporting.length,0);
 await call(admin,'/api/institution',{action:'person',workerId,employeeNumber:'TEST-001',unitId:unit,archived:true});await call(learner,'/api/identity',start,403);
 assert.equal(db.prepare('PRAGMA integrity_check').get()?.integrity_check,'ok');assert.equal(db.prepare('PRAGMA foreign_key_check').all().length,0);
 writeFileSync(dirname(path)+'/browser-fixture.json',JSON.stringify({origin,actors,workerId,assignment,reference,awardId:award.id}),{mode:0o600});
 console.log(`Institution integration passed: ${checks} HTTP assertions; role isolation, tenant isolation, imports, policies, references, snapshots, observations, issuance, replacement, revocation, rotation, content review and archived membership. Synthetic data only.`);
}finally{db.close()}
