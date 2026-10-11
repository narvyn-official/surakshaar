/** Only the isolated synthetic QA service is supported; never a production database. */
import assert from 'node:assert/strict';import {DatabaseSync} from 'node:sqlite';import {readFileSync,writeFileSync} from 'node:fs';import {randomUUID} from 'node:crypto';
import {encryptEvidence} from '../lib/identity-evidence';
import {startPractice,practiceAction,studioVersion,studioScenarios} from '../lib/studio-learning';
const origin=process.env.BETTER_AUTH_URL!,path=process.env.SURAKSHA_DB_PATH!;assert(origin==='http://127.0.0.1:5196'&&path.includes('/.runtime/institution-0.17/'));
const f=JSON.parse(readFileSync('../../.runtime/roles-0.18/browser-fixture.json','utf8')),actors=f.actors,db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON;PRAGMA busy_timeout=5000');let checks=0;
async function call(actor:any,route:string,body?:unknown,status=200){const r=await fetch(origin+route,{method:body===undefined?'GET':'POST',headers:{Cookie:actor?.cookie??'',Origin:origin,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body),redirect:'manual'});assert.equal(r.status,status,route+' '+await r.clone().text());checks++;const t=r.headers.get('content-type');return t?.includes('json')?await r.json() as any:await r.text();}
const now=Date.now(),outside=randomUUID(),unit=randomUUID(),branch=randomUUID(),otherunit=randomUUID(),owner=actors.admin.id;
const oldPlacement=db.prepare('SELECT * FROM institution_people WHERE owner=? AND worker_id=?').get(owner,f.workerId)!;
const grant=(role:string,ids:string[])=>call(actors.admin,'/api/admin/manage',{action:'member',email:actors[role].email,role,active:true,scope:{mode:'units',unitIds:ids}});
try{
 const signin=await fetch(origin+'/api/auth/sign-in/email',{method:'POST',headers:{Origin:origin,'Content-Type':'application/json'},body:JSON.stringify({email:actors.admin.email,password:actors.admin.password})});assert.equal(signin.status,200);actors.admin.cookie=signin.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ');
 for(const [id,kind,name,parent] of [[branch,'branch','Scope branch '+now,null],[unit,'cohort','Scope child '+now,branch],[otherunit,'cohort','Other group '+now,null]])await call(actors.admin,'/api/institution',{action:'unit',id,kind,name,parentId:parent});
 db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run(owner,outside,'Outside scope '+now,'Other',now);db.prepare('INSERT INTO institution_people VALUES(?,?,?,?,NULL)').run(owner,outside,'OUT-'+now,otherunit);
 db.prepare('UPDATE institution_people SET unit_id=?,archived_at=NULL WHERE owner=? AND worker_id=?').run(unit,owner,f.workerId);
 const command={action:'assign',id:randomUUID(),workerIds:[outside],courseId:'fire',contentVersion:studioVersion,audience:'adult-learning',scenarioIds:['escape-first'],mode:'guided',dueAt:now+86400000,note:'Scope boundary fixture'};
 const forbiddenAssignment=(await call(actors.trainer,'/api/learning-centre',command)).assignmentIds[0];
 const evidenceAssignment=(await call(actors.trainer,'/api/learning-centre',{...command,id:randomUUID(),workerIds:[f.workerId]})).assignmentIds[0];
 const referenceId=randomUUID(),sessionId=randomUUID(),frameId=randomUUID(),image=Buffer.from('Synthetic encrypted evidence access test; not a real identity photograph.');
 db.prepare("UPDATE identity_references SET status='replaced' WHERE owner=? AND worker_id=? AND status='approved'").run(owner,f.workerId);
 db.prepare("INSERT INTO identity_references(owner,id,worker_id,account_id,ciphertext,digest,status,created_at,expires_at) VALUES(?,?,?,?,?,?,'approved',?,?)").run(owner,referenceId,f.workerId,actors.learner.id,encryptEvidence(image,owner+':reference:'+referenceId,process.env.IDENTITY_EVIDENCE_KEY),'synthetic',now,now+86400000);
 db.prepare("INSERT INTO identity_sessions(id,owner,worker_id,account_id,assignment_id,scenario_id,policy_revision,notice,support,status,practice,challenge,challenge_at,started_at,updated_at,expires_at,reference_id) VALUES(?,?,?,?,?,'escape-first',1,'Synthetic notice','Synthetic support','submitted',?,?,?,?,?,?,?)").run(sessionId,owner,f.workerId,actors.learner.id,evidenceAssignment,JSON.stringify(startPractice('escape-first',f.workerId,true)),randomUUID(),now,now,now,now+86400000,referenceId);
 db.prepare('INSERT INTO identity_frames(id,session_id,sequence,received_at,client_at,ciphertext) VALUES(?,?,0,?,?,?)').run(frameId,sessionId,now,now,encryptEvidence(image,sessionId+':'+frameId,process.env.IDENTITY_EVIDENCE_KEY));

 for(const role of ['trainer','examiner','certifier','assessor','viewer']){
  await grant(role,[branch]);const actor=actors[role],dashboard=await call(actor,'/api/dashboard');assert(dashboard.context.restricted);assert.equal(dashboard.stats[0].value,1);assert(dashboard.actions.every((a:any)=>!a.href.includes('/workspace')&&!a.href.includes('/audit')));
  const list=await call(actor,'/api/learning-centre');assert(list.workers.every((w:any)=>w.id===f.workerId));assert(list.assignments.every((a:any)=>a.worker_id===f.workerId));
  await call(actor,'/api/learning-centre?assignmentId='+forbiddenAssignment,undefined,403);await call(actor,'/api/learning-centre?assignmentId='+f.assignment);
  const csv=await call(actor,'/api/learning-centre?format=csv');assert(!csv.csv.includes('Outside scope'));
  const people=await call(actor,'/api/institution');assert(people.people.every((p:any)=>p.id===f.workerId));assert(people.units.every((u:any)=>[branch,unit].includes(u.id)));assert.equal(people.plan,null);
  await call(actor,'/api/institution-awards');
  for(const route of ['/api/admin/manage','/api/records','/api/learners','/api/room-journals','/api/credentials','/api/institution-audit','/api/safety-updates'])await call(actor,route,undefined,403);
 }
 await call(actors.trainer,'/api/learning-centre',{...command,id:randomUUID()},403);await call(actors.trainer,'/api/learning-centre',{action:'cancel',assignmentId:forbiddenAssignment,reason:'Outside scope cancellation'},403);
 await call(actors.admin,'/api/admin/manage',{action:'member',email:actors.viewer.email,role:'admin',active:true},400);
 await call(actors.admin,'/api/admin/manage',{action:'member',email:actors.viewer.email,role:'viewer',active:true,scope:{mode:'units',unitIds:[randomUUID()]}},400);
 await call(actors.examiner,'/api/identity-reference?staff=1&image='+referenceId);await call(actors.examiner,'/api/identity?staff=1&frame='+frameId);
 const before=await call(actors.examiner,'/api/identity?staff=1');assert(before.sessions.every((s:any)=>s.worker_id===f.workerId));
 // Moving a learner removes access immediately, including the underlying reference photograph.
 db.prepare('UPDATE institution_people SET unit_id=? WHERE owner=? AND worker_id=?').run(otherunit,owner,f.workerId);
 for(const role of ['trainer','examiner','certifier','assessor','viewer']){const actor=actors[role];assert.equal((await call(actor,'/api/dashboard')).stats[0].value,0);await call(actor,'/api/learning-centre?assignmentId='+f.assignment,undefined,403);assert.equal((await call(actor,'/api/institution-awards')).awards.length,0);}
 assert.equal((await call(actors.examiner,'/api/identity?staff=1')).sessions.length,0);
 await call(actors.examiner,'/api/identity-reference?staff=1&image='+referenceId,undefined,403);await call(actors.examiner,'/api/identity?staff=1&frame='+frameId,undefined,400);assert.equal((await call(actors.examiner,'/api/identity?staff=1&id='+sessionId)).frames.length,0);
 db.prepare('UPDATE institution_people SET unit_id=? WHERE owner=? AND worker_id=?').run(unit,owner,f.workerId);
 await call(actors.admin,'/api/admin/manage',{action:'member',email:actors.examiner.email,role:'examiner',active:false});await call(actors.examiner,'/api/learning-centre',undefined,403);await call(actors.admin,'/api/admin/manage',{action:'member',email:actors.examiner.email,role:'examiner',active:true});assert((await call(actors.examiner,'/api/dashboard')).context.restricted);
 // Actual assignment → private save → share → feedback → correction → withdrawal.
 const assignment=(await call(actors.trainer,'/api/learning-centre',{...command,id:randomUUID(),workerIds:[f.workerId]})).assignmentIds[0];
 let record=startPractice('escape-first',f.workerId,true);await call(actors.learner,'/api/learning-practice',{schemaVersion:1,workerId:f.workerId,attempts:[record]});
 const base='/api/learning-enrolment?workerId='+f.workerId+'&owner='+owner+'&assignmentId='+assignment;
 assert((await call(actors.learner,base)).drafts.some((r:any)=>r.id===record.id));
 for(const step of studioScenarios.find(s=>s.id==='escape-first')!.steps){record=practiceAction(record,{kind:'decision',choiceId:step.choices.find(c=>c.correct)!.id});if(step.requiresObservation)record=practiceAction(record,{kind:'observation'});record=practiceAction(record,{kind:'continue'});}
 await call(actors.learner,'/api/learning-practice',{schemaVersion:1,workerId:f.workerId,attempts:[record]});assert((await call(actors.learner,base)).available.some((r:any)=>r.id===record.id));
 const sharing={action:'submit',owner,workerId:f.workerId,assignmentId:assignment,attemptIds:[record.id],shareEvidence:true};const submission=(await call(actors.learner,'/api/learning-enrolment',sharing)).ids[0];await call(actors.learner,'/api/learning-enrolment',sharing);
 const review=randomUUID();await call(actors.examiner,'/api/learning-centre',{action:'review',id:review,submissionId:submission,previousEventId:null,outcome:'reviewed',note:'Synthetic practice feedback for the completed scope flow.'});
 await call(actors.learner,'/api/learning-enrolment',{action:'correction',id:randomUUID(),owner,workerId:f.workerId,submissionId:submission,previousEventId:review,note:'Synthetic learner correction; retain original decisions.'});
 assert.equal((await call(actors.learner,base)).assignment.reviews.length,2);
 await call(actors.learner,'/api/learning-enrolment',{action:'withdraw',owner,workerId:f.workerId,submissionId:submission});const viewed=await call(actors.examiner,'/api/learning-centre?assignmentId='+assignment);assert.equal(viewed.assignment.submissions[0].evidence,null);await call(actors.learner,'/api/learning-enrolment',sharing,409);
 await call(actors.other,base,undefined,403);
 writeFileSync('../../.runtime/completion-0.19/browser-fixture.json',JSON.stringify({...f,scopeBranch:branch,scopeUnit:unit,outside,otherunit,webAssignment:assignment}),{mode:0o600});
 console.log(JSON.stringify({checks,scopedRoles:5,descendants:true,movedLearner:true,revocation:true,protectedPaths:true,webSubmissionLifecycle:true}));
}finally{db.prepare('UPDATE institution_people SET unit_id=?,archived_at=? WHERE owner=? AND worker_id=?').run(oldPlacement.unit_id,oldPlacement.archived_at,owner,f.workerId);for(const role of ['trainer','examiner','certifier','assessor','viewer']){db.prepare("UPDATE team_members SET scope_mode='all',active=1 WHERE owner=? AND email=?").run(owner,actors[role].email);db.prepare('DELETE FROM team_member_units WHERE owner=? AND email=?').run(owner,actors[role].email);}db.close();}
