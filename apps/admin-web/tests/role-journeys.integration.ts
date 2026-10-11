/** Destructive tests are restricted to the isolated synthetic role-workflow database. */
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync,writeFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
import {startPractice,practiceAction,studioScenarios} from '../lib/studio-learning';
const origin=process.env.BETTER_AUTH_URL!,path=process.env.SURAKSHA_DB_PATH!,fixture=process.env.SURAKSHA_ROLE_FIXTURE!;
assert(origin==='http://127.0.0.1:5196'&&path.includes('/.runtime/institution-0.17/role-workflows-')&&fixture.includes('/role-workflows-'));
const f=JSON.parse(readFileSync(fixture,'utf8')),actors=f.actors;
const db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON;PRAGMA busy_timeout=5000');let checks=0,ip=0;
async function call(actor:any,route:string,body?:unknown,status=200){const r=await fetch(origin+route,{method:body===undefined?'GET':'POST',redirect:'manual',headers:{Cookie:actor?.cookie??'',Origin:origin,'Content-Type':'application/json','X-Real-IP':'198.51.100.'+(++ip%200+1)},body:body===undefined?undefined:JSON.stringify(body)});assert.equal(r.status,status,route+' '+await r.clone().text());checks++;return r;}
async function data(actor:any,route:string,body?:unknown,status=200){return await(await call(actor,route,body,status)).json() as any;}
async function create(label:string){const email='journey-'+label+'-'+randomUUID()+'@example.test',password='Synthetic only '+randomUUID(),r=await call(null,'/api/auth/sign-up/email',{email,password,name:'Synthetic '+label}),v=await r.json() as {user:{id:string}};return {id:v.user.id,email,password,cookie:r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ')};}
try{
 // Reset only the disposable fixture's request counters between failed test reruns.
 db.exec('DELETE FROM learning_sync_budget');
 // Reauthenticate sensitive decision-makers without printing passwords/cookies.
 for(const a of Object.values(actors) as any[]){const r=await call(null,'/api/auth/sign-in/email',{email:a.email,password:a.password});a.cookie=r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ');}
 // Discoverable onboarding, including before MFA and with a revoked workspace selection.
 await call(null,'/studio');await call(null,'/institute-access',undefined,307);await call(null,'/api/admin/session',undefined,401);
 for(const role of ['platform','admin','trainer','examiner','certifier','assessor','auditor','author','reviewer','viewer','learner','personal','supporter'])await call(actors[role],'/institute-access');
 const invited=await create('invited-examiner');actors.invited=invited;
 const invitation=(await data(actors.admin,'/api/admin/manage',{action:'member',email:invited.email,role:'examiner',active:true})).invitation;
 await call(actors.personal,'/api/admin/session',{invitation},400);
 const accepted=await call(invited,'/api/admin/session',{invitation});assert(accepted.headers.get('set-cookie')?.includes('suraksha_workspace='));assert.equal((await data(invited,'/api/dashboard')).context.role,'examiner');
 await call(invited,'/api/admin/session',{invitation},400);
 await data(actors.admin,'/api/admin/manage',{action:'member',email:invited.email,role:'examiner',active:false});
 const stale={...invited,cookie:invited.cookie+'; suraksha_workspace='+actors.admin.id};await call(stale,'/institute-access');assert.equal((await data(stale,'/api/dashboard')).context.workspaceUnavailable,true);await call(stale,'/api/learning-centre',undefined,403);await call(stale,'/api/admin/session',{clear:true});
 // An applicant becomes an administrator only after the independent platform decision.
 const applicant=await create('applicant');actors.applicant=applicant;
 await data(applicant,'/api/governance',{action:'request',name:'Synthetic applicant institute',site:'Isolated role QA'});
 assert.equal((await data(applicant,'/api/governance')).centres[0].status,'pending');assert.equal((await data(applicant,'/api/dashboard')).context.role,'personal');await call(applicant,'/api/institution',undefined,403);
 await call(applicant,'/api/governance',{action:'approve',owner:applicant.id,reason:'Self approval must not grant administration.'},403);
 await data(actors.platform,'/api/governance',{action:'reject',owner:applicant.id,reason:'Synthetic return so applicant can correct its request.'});
 await data(applicant,'/api/governance',{action:'request',name:'Synthetic approved institute',site:'Corrected isolated site'});
 await data(actors.platform,'/api/governance',{action:'approve',owner:applicant.id,reason:'Synthetic independent approval for workflow testing.'});
 assert.equal((await data(applicant,'/api/dashboard')).context.role,'admin');await call(applicant,'/api/institution');
 await data(actors.platform,'/api/platform-settings',{owner:applicant.id,name:'Synthetic manual plan',learnerLimit:10,validUntil:null,restricted:false,reason:'Synthetic contract configuration, no payment provider.'});
 assert.equal((await data(applicant,'/api/institution')).plan.learner_limit,10);
 await data(actors.platform,'/api/governance',{action:'suspend',owner:applicant.id,reason:'Synthetic suspension must remove institutional access.'});await call(applicant,'/api/institution',undefined,403);assert.equal((await data(applicant,'/api/governance')).centres[0].status,'suspended');
 await data(actors.platform,'/api/governance',{action:'approve',owner:applicant.id,reason:'Synthetic restoration after suspension verification.'});
 await call(actors.admin,'/api/platform-settings',undefined,403);await call(actors.examiner,'/api/platform-settings',undefined,403);
 // Complete editable-draft journey and server-enforced author/reviewer separation.
 const draft={action:'procedure',id:randomUUID(),title:'Synthetic draft lifecycle',body:'Synthetic workflow fixture only. This is not an operational safety instruction. These words deliberately test saved drafts and independent review.',sources:'Synthetic test specification, 11 October 2026',language:'en',submit:false};
 await data(actors.author,'/api/institution',draft);await data(actors.author,'/api/institution',draft);
 assert((await data(actors.author,'/api/institution')).procedures.some((p:any)=>p.id===draft.id));
 for(const role of ['reviewer','trainer','admin'])assert(!(await data(actors[role],'/api/institution')).procedures.some((p:any)=>p.id===draft.id),role+' cannot see another author draft');
 await call(actors.reviewer,'/api/institution',{...draft,id:randomUUID(),submit:true},403);
 await call(actors.trainer,'/api/institution',{...draft,action:'procedure-update',revision:1},400);
 const edit={...draft,action:'procedure-update',revision:1,title:'Synthetic edited draft'};await data(actors.author,'/api/institution',edit);await data(actors.author,'/api/institution',edit);
 await call(actors.author,'/api/institution',{...edit,title:'Stale tab must not overwrite'},400);
 const submitted={...edit,revision:2,submit:true};await data(actors.author,'/api/institution',submitted);await data(actors.author,'/api/institution',submitted);
 assert((await data(actors.reviewer,'/api/dashboard')).queue.some((q:any)=>q.id===draft.id));
 await call(actors.author,'/api/institution',{...submitted,revision:3,title:'Submitted history cannot change'},400);
 const decision={action:'procedure-review',id:draft.id,revision:3,outcome:'rejected',note:'Synthetic feedback: add the missing reviewed source detail.',qualification:'Synthetic QA reviewer, no clinical qualification claimed'};
 await call(actors.author,'/api/institution',decision,403);await data(actors.reviewer,'/api/institution',decision);
 assert((await data(actors.author,'/api/dashboard')).queue.some((q:any)=>q.id===draft.id));
 const revised={...draft,id:randomUUID(),title:'Synthetic resubmitted revision',submit:true};await data(actors.author,'/api/institution',revised);await data(actors.reviewer,'/api/institution',{...decision,id:revised.id,revision:1,outcome:'approved'});
 assert((await data(actors.learner,'/api/institution-library?owner='+actors.admin.id)).procedures.some((p:any)=>p.id===revised.id));
 await call(actors.author,'/api/institution',{...revised,action:'procedure-update',revision:2,title:'Cannot rewrite approved history'},400);
 await data(actors.reviewer,'/api/institution',{...decision,id:revised.id,revision:2,outcome:'withdrawn'});assert(!(await data(actors.learner,'/api/institution-library?owner='+actors.admin.id)).procedures.some((p:any)=>p.id===revised.id));
 // Even a later role change does not allow the author to approve their own submitted content.
 const own={...draft,id:randomUUID(),submit:true};await data(actors.author,'/api/institution',own);
 db.prepare("UPDATE team_members SET role='reviewer' WHERE owner=? AND user_id=?").run(actors.admin.id,actors.author.id);
 try{await call(actors.author,'/api/institution',{...decision,id:own.id,revision:1,outcome:'approved'},403);}finally{db.prepare("UPDATE team_members SET role='author' WHERE owner=? AND user_id=?").run(actors.admin.id,actors.author.id);}
 // Read-only and specialist roles cannot create policy, staff, observations or awards.
 for(const role of ['examiner','certifier','assessor','auditor','author','reviewer','viewer','learner','personal','supporter']){
  await call(actors[role],'/api/admin/manage',{action:'member',email:'blocked@example.test',role:'admin',active:true},403);
 }
 for(const role of ['admin','trainer','examiner','auditor','author','reviewer','viewer','learner','personal','supporter'])await call(actors[role],'/api/institution-awards',{action:'issue',id:randomUUID(),assignmentId:f.assignment,reason:'Forbidden issuance role regression test.'},403);
 for(const role of ['certifier','auditor','author','reviewer','viewer','learner','personal','supporter'])await call(actors[role],'/api/learning-centre',{action:'review',id:randomUUID(),submissionId:'0'.repeat(64),previousEventId:null,outcome:'reviewed',note:'Forbidden feedback role regression.'},403);
 for(const role of ['trainer','certifier','auditor','author','reviewer','viewer','learner','personal','supporter'])await call(actors[role],'/api/identity?staff=1',undefined,403);
 const audit=await data(actors.auditor,'/api/institution-audit?format=csv');assert(audit.events.every((e:any)=>Object.keys(e).sort().join(',')==='action,actor,at,id'));assert(!audit.csv.includes(actors.learner.email));
 // Combined assessor can review, but cannot override independent-issuer policy.
 const detail=(await data(actors.assessor,'/api/learning-centre?assignmentId='+f.assignment)).assignment,submission=detail.submissions.find((s:any)=>s.evidence);
 assert(submission);const previous=detail.reviews.find((r:any)=>r.submission_id===submission.id);
 await data(actors.assessor,'/api/learning-centre',{action:'review',id:randomUUID(),submissionId:submission.id,previousEventId:previous?.id??null,outcome:'reviewed',note:'Synthetic combined-assessor review of existing completed evidence.'});
 const award={action:'issue',id:randomUUID(),assignmentId:f.assignment,reason:'Synthetic separate-issuer workflow acceptance check.'};
 await call(actors.assessor,'/api/institution-awards',award,403);await data(actors.certifier,'/api/institution-awards',award);
 assert.equal((await data(null,'/api/public-award?id='+award.id)).signatureVerified,true);
 // Personal practice belongs to its account and shares only an explicitly requested summary.
 const worker=randomUUID();let practice=startPractice('escape-first',worker,true);for(const step of studioScenarios.find(s=>s.id==='escape-first')!.steps){practice=practiceAction(practice,{kind:'decision',choiceId:step.choices.find(c=>c.correct)!.id});if(step.requiresObservation)practice=practiceAction(practice,{kind:'observation'});practice=practiceAction(practice,{kind:'continue'});}
 await data(actors.personal,'/api/learning-practice',{schemaVersion:1,workerId:worker,attempts:[practice]});
 assert((await data(actors.personal,'/api/learning-profiles')).profiles.some((p:any)=>p.workerId===worker));
 assert(!(await data(actors.learner,'/api/learning-profiles')).profiles.some((p:any)=>p.workerId===worker));
 await call(actors.learner,'/api/learning-practice?workerId='+worker,undefined,403);
 const sharing=await data(actors.personal,'/api/learning-support',{action:'invite',workerId:worker,label:'Synthetic personal summary',email:actors.supporter.email,acknowledged:true});
 await data(actors.supporter,'/api/learning-support',{action:'accept',code:sharing.code});let summaries=(await data(actors.supporter,'/api/learning-support')).supporting;const summary=summaries.find((r:any)=>r.label==='Synthetic personal summary');assert(summary);assert.equal(summary.completed_scenarios,1);assert(!('payload' in summary));assert(!('frames' in summary));
 await data(actors.supporter,'/api/learning-support',{action:'leave',id:summary.id});summaries=(await data(actors.supporter,'/api/learning-support')).supporting;assert(!summaries.some((r:any)=>r.id===summary.id));
 // Dashboard evidence and queues respect archive state even for whole-institute examiners.
 const before=await data(actors.examiner,'/api/dashboard');const pendingBefore=before.stats.find((s:any)=>s.label==='Identity awaiting review').value;
 db.prepare('UPDATE institution_people SET archived_at=? WHERE owner=? AND worker_id=?').run(Date.now(),actors.admin.id,f.workerId);
 try{const after=await data(actors.examiner,'/api/dashboard');assert.equal(after.stats.find((s:any)=>s.label==='Identity awaiting review').value,0);assert.equal(after.queue.length,0);assert(pendingBefore>=0);}finally{db.prepare('UPDATE institution_people SET archived_at=NULL WHERE owner=? AND worker_id=?').run(actors.admin.id,f.workerId);}
 assert.equal(db.prepare('PRAGMA integrity_check').get()?.integrity_check,'ok');assert.equal(db.prepare('PRAGMA foreign_key_check').all().length,0);
 writeFileSync(fixture,JSON.stringify({...f,actors,draftId:draft.id,procedureId:own.id}),{mode:0o600});
 console.log(JSON.stringify({httpChecks:checks,roles:13,onboarding:'pass',applicationGovernance:'pass',draftLifecycle:'pass',serverPermissionDenials:'pass',personalSupportSharing:'pass',dashboardArchive:'pass',syntheticOnly:true}));
}finally{db.close();}
