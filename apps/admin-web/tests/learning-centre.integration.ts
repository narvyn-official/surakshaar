import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {account,localDatabase} from './auth-client.mjs';
import catalogue from '../../../content/learning/scenarios.json';
import type {LearningPractice} from '../lib/learning-practice';

// Synthetic local accounts only. Runs against the real local HTTP middleware and migrated D1 DB.
const origin='http://localhost:5173',users:Awaited<ReturnType<typeof account>>[]=[];
async function call(person:typeof users[number]|null,path:string,body?:unknown){const response=await fetch(origin+'/api/'+path,{method:body?'POST':'GET',headers:{Origin:origin,'Content-Type':'application/json',...(person?{Cookie:person.cookie}:{})},body:body?JSON.stringify(body):undefined});const text=await response.text();let data;try{data=JSON.parse(text)}catch{throw new Error(`Non-JSON local API ${path} status ${response.status}: ${text.slice(0,300)}`)}return {status:response.status,data};}
function fixture(action:(db:ReturnType<typeof localDatabase>)=>void){const db=localDatabase();try{action(db)}finally{db.close()}}
try{
 const owner=await account({approved:true});users.push(owner);const learner=await account();users.push(learner);const foreign=await account({approved:true});users.push(foreign);const staff=await account();users.push(staff);
 const worker=randomUUID();fixture(db=>{db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run(owner.userId,worker,'=Synthetic learner','Unspecified',Date.now());db.prepare("INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,'viewer',1,?)").run(owner.userId,staff.email,staff.userId,Date.now())});
 const reader={...staff,cookie:staff.cookie+'; suraksha_workspace='+owner.userId};
 assert.equal((await call(null,'learning-centre')).status,401);assert.equal((await call(learner,'learning-centre')).status,403);
 const invitation=await call(owner,'learners',{action:'invite',workerId:worker,email:learner.email});assert.equal(invitation.status,200,JSON.stringify(invitation));
 assert.equal((await call(foreign,'learner',{action:'join',invitation:invitation.data.invitation,workerId:worker})).status,400);
 assert.equal((await call(learner,'learner',{action:'join',invitation:invitation.data.invitation,workerId:worker})).status,200);
 const assignment={action:'assign',id:randomUUID(),workerIds:[worker],courseId:'fire',contentVersion:catalogue.version,audience:'school-awareness',scenarioIds:['school-fire-help'],mode:'guided',dueAt:Date.now()+86400000,note:'Synthetic supervised awareness practice'};
 assert.equal((await call(reader,'learning-centre',assignment)).status,403);
 const created=await call(owner,'learning-centre',assignment);assert.equal(created.status,200,JSON.stringify(created));assert.equal(created.data.certifiable,false);const id=created.data.assignmentIds[0];
 assert.deepEqual((await call(owner,'learning-centre',assignment)).data.assignmentIds,[id]);assert.equal((await call(owner,'learning-centre',{...assignment,note:'Changed same request'})).status,409);
 assert.equal((await call(reader,'learning-centre')).status,200);assert.equal((await call(foreign,'learning-centre?assignmentId='+id)).status,400);
 assert.equal((await call(foreign,'learning-enrolment?workerId='+worker)).status,403);
 let at=Date.now();const createdAt=at,scenario=catalogue.scenarios.find(s=>s.id==='school-fire-help')!,answers:LearningPractice['answers']=[];
 for(const step of scenario.steps){answers.push({kind:'decision',stepId:step.id,choiceId:step.choices.find(c=>c.correct)!.id,correct:true,hints:0,at:++at});answers.push({kind:'continue',stepId:step.id,at:++at})}
 answers.push({kind:'teach-back',text:'PRIVATE REFLECTION MUST STAY PRIVATE',at:++at});
 const record:LearningPractice={id:randomUUID(),workerId:worker,scenarioId:scenario.id,courseId:'fire',contentVersion:catalogue.version,index:scenario.steps.length,answers,finished:true,guided:true,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt,updatedAt:at};
 assert.equal((await call(learner,'learning-practice',{schemaVersion:1,workerId:worker,attempts:[record]})).status,200);
 const before=await call(owner,'learning-centre?assignmentId='+id);assert.equal(before.data.assignment.submissions.length,0);
 const share={action:'submit',workerId:worker,assignmentId:id,attemptIds:[record.id],shareEvidence:true};
 assert.equal((await call(learner,'learning-enrolment',{...share,shareEvidence:false})).status,400);
 const submitted=await call(learner,'learning-enrolment',share);assert.equal(submitted.status,200,JSON.stringify(submitted));assert.equal(submitted.data.reflectionIncluded,false);const submission=submitted.data.ids[0];
 assert.deepEqual((await call(learner,'learning-enrolment',share)).data.ids,[submission]);
 const detail=await call(owner,'learning-centre?assignmentId='+id);assert.equal(detail.data.assignment.status,'practice-complete');assert.equal(detail.data.assignment.certifiable,false);assert(!JSON.stringify(detail).includes('PRIVATE REFLECTION'));
 const report=await call(owner,'learning-centre?format=csv');assert(report.data.csv.includes("'=Synthetic learner"));
 const feedback={action:'review',id:randomUUID(),submissionId:submission,previousEventId:null,outcome:'needs-information',note:'Please explain the evacuation handover.'};
 assert.equal((await call(reader,'learning-centre',feedback)).status,403);
 assert.equal((await call(owner,'learning-centre',feedback)).status,200);assert.equal((await call(owner,'learning-centre',feedback)).status,200);
 assert.equal((await call(owner,'learning-centre',{...feedback,id:randomUUID()})).status,409);
 const correctionId=randomUUID();assert.equal((await call(learner,'learning-enrolment',{action:'correction',id:correctionId,workerId:worker,submissionId:submission,previousEventId:feedback.id,note:'Please keep this as an awareness exercise.'})).status,200);
 const observation={action:'observe',id:randomUUID(),assignmentId:id,observedAt:Date.now(),setting:'remote-simulation',rubric:{communication:true,sequence:true,safeDecisions:false},outcome:'further-practice',note:'Synthetic observation requires more practice.'};
 assert.equal((await call(owner,'learning-centre',observation)).status,200);assert.equal((await call(owner,'learning-centre',observation)).status,200);
 const inboxPath='notifications?scope=learner&workerId='+worker;
 const inbox=await call(learner,inboxPath);assert.equal(inbox.status,200,JSON.stringify(inbox));assert(inbox.data.items.some((v:{kind:string})=>v.kind==='review'));assert(inbox.data.items.some((v:{kind:string})=>v.kind==='observation'));
 const item='assignment:'+id,notification={scope:'learner',workerId:worker,itemId:item};
 for(const action of ['read','unread','dismiss','restore'])assert.equal((await call(learner,'notifications',{...notification,action})).status,200);
 const open=await call(learner,'notifications',{...notification,action:'open'});assert.deepEqual(open.data.destination,{screen:'learner-assignment',assignmentId:id,workerId:worker,owner:owner.userId});
 assert.equal((await call(foreign,'notifications',{...notification,action:'open'})).status,403);
 assert.equal((await call(learner,'learning-enrolment',{action:'withdraw',workerId:worker,submissionId:submission})).status,200);
 const withdrawn=await call(owner,'learning-centre?assignmentId='+id);assert.equal(withdrawn.data.assignment.completed,0);assert.equal(withdrawn.data.assignment.submissions[0].evidence,null);assert.equal(withdrawn.data.assignment.reviews.length,0);
 assert.equal((await call(learner,'learning-enrolment',share)).status,409);
 assert.equal((await call(owner,'learning-centre',{...feedback,id:randomUUID(),previousEventId:correctionId})).status,400);
 assert.equal((await call(learner,'notifications',{...notification,itemId:'review:'+feedback.id,action:'open'})).status,400);
 const later=Array.from({length:11},()=>({...record,id:randomUUID()}));
 assert.equal((await call(learner,'learning-practice',{schemaVersion:1,workerId:worker,attempts:later})).status,200);
 assert.equal((await call(learner,'learning-enrolment',{...share,attemptIds:later.map(v=>v.id)})).status,200);
 const firstPage=await call(owner,'learning-centre?assignmentId='+id);assert.equal(firstPage.data.assignment.submissions.length,10);const cursor=firstPage.data.assignment.nextSubmissionCursor;assert(cursor);
 const nextPage=await call(owner,'learning-centre?assignmentId='+id+'&beforeAt='+cursor.beforeAt+'&beforeId='+cursor.beforeId);assert.equal(nextPage.data.assignment.submissions.length,2);assert.equal(nextPage.data.assignment.nextSubmissionCursor,null);assert.equal(nextPage.data.assignment.status,'practice-complete');
 const seen=new Set(firstPage.data.assignment.submissions.map((s:{id:string})=>s.id));assert(nextPage.data.assignment.submissions.every((s:{id:string})=>!seen.has(s.id)));
 const available=await call(learner,'learning-enrolment?workerId='+worker+'&assignmentId='+id);assert.equal(available.data.available.length,0);
 // Same account/profile in two approved institutions: selection and notification state stay separate.
 fixture(db=>db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run(foreign.userId,worker,'Synthetic learner at second centre','Unspecified',Date.now()));
 const secondInvite=await call(foreign,'learners',{action:'invite',workerId:worker,email:learner.email});assert.equal(secondInvite.status,200);
 const joined=await call(learner,'learner',{action:'join',workerId:worker,invitation:secondInvite.data.invitation});assert.equal(joined.data.owner,foreign.userId);
 const linked=await call(learner,'learner');assert.equal(linked.data.links.filter((v:{worker_id:string})=>v.worker_id===worker).length,2);
 assert.equal((await call(learner,'learning-enrolment?workerId='+worker)).status,409);
 assert.equal((await call(learner,'learner?workerId='+worker)).status,400);
 assert.equal((await call(learner,inboxPath)).status,409);
 assert.equal((await call(learner,'learning-enrolment?workerId='+worker+'&owner='+staff.userId)).status,403);
 assert.equal((await call(learner,'learning-enrolment?workerId='+worker+'&owner='+owner.userId)).data.assignments.length,1);
 assert.equal((await call(learner,'learning-enrolment?workerId='+worker+'&owner='+foreign.userId)).data.assignments.length,0);
 assert.equal((await call(learner,'learning-enrolment?workerId='+worker+'&owner='+foreign.userId+'&assignmentId='+id)).status,400);
 assert.equal((await call(learner,'learning-enrolment',{...share,owner:foreign.userId})).status,400);
 const secondAssignment=await call(foreign,'learning-centre',{...assignment,id:randomUUID()});assert.equal(secondAssignment.status,200);
 assert.equal((await call(foreign,'learning-centre',{...observation,observedAt:Date.now(),assignmentId:secondAssignment.data.assignmentIds[0]})).status,200);
 const itemCommand={scope:'learner',workerId:worker,itemId:'observation:'+observation.id,action:'read',owner:owner.userId};
 assert.equal((await call(learner,'notifications',itemCommand)).status,200);
 const secondInbox=await call(learner,inboxPath+'&owner='+foreign.userId);assert.equal(secondInbox.data.items.find((v:{id:string})=>v.id===itemCommand.itemId).read_at,null);
 assert.equal((await call(learner,'notifications',{...itemCommand,owner:foreign.userId,action:'open'})).data.destination.owner,foreign.userId);
 assert.equal((await call(learner,'notifications',{...notification,owner:foreign.userId,action:'open'})).status,400);
 assert.equal((await call(learner,'tutor/readiness')).status,403);assert.equal((await call(reader,'tutor/readiness')).status,403);
 const readiness=await call(owner,'tutor/readiness');assert.equal(readiness.status,200);assert.equal(readiness.data.connection,'not-tested');assert.equal(readiness.data.productionReady,false);
 assert.equal((await call(reader,'tutor/readiness',{action:'probe',confirmExternalRequest:true})).status,403);
 assert.equal((await call(owner,'tutor/readiness',{action:'probe',confirmExternalRequest:false})).status,400);
 fixture(db=>{for(const table of ['attempts','credentials','certification_requests'])assert.equal(db.prepare(`SELECT count(*) AS n FROM ${table} WHERE owner=?`).get(owner.userId)!.n,0);db.prepare('DELETE FROM learner_links WHERE owner=? AND worker_id=?').run(owner.userId,worker)});
 assert.equal((await call(learner,inboxPath+'&owner='+owner.userId)).status,403);assert.equal((await call(learner,'learning-enrolment?workerId='+worker+'&owner='+owner.userId)).status,403);assert.equal((await call(owner,'learning-centre?assignmentId='+id)).data.assignment.observations.length,0);
 fixture(db=>db.prepare("UPDATE centre_approvals SET status='suspended' WHERE owner=?").run(owner.userId));assert.equal((await call(reader,'learning-centre')).status,403);
 console.log('Learning centre integration PASS: 65+ HTTP assertions for invitations, roles/tenants, version-pinned assignments, private sharing, immutable feedback, observation, inbox actions, revocation, multi-institution selection/state, admin-only AI readiness, CSV safety and legacy credential separation.');
}finally{
 fixture(db=>{for(const user of users){
  for(const table of ['learning_review_events','learning_course_observations','learning_course_submissions','learning_course_assignments','learner_links','team_members','audit_log','workers','training_centres','centre_approvals'])db.prepare(`DELETE FROM ${table} WHERE owner=?`).run(user.userId);
  for(const table of ['learning_inbox_state','learning_practice_snapshots','learning_practice_heads','learning_profile_accounts','learning_sync_budget'])db.prepare(`DELETE FROM ${table} WHERE account_id=?`).run(user.userId);
  db.prepare('DELETE FROM auth_user WHERE id=?').run(user.userId);
 }})
}
