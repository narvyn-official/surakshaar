import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
import catalogue from '../../../content/learning/scenarios.json';
import archived from '../../../content/learning/archive/learning-2026.10.10-draft1.json';
import {centreCommand,validateAssignment,sharedPractice,requireMatchingPractice,assignmentProgress,csvCell,type LaunchAssignment,type Submission} from '../lib/learning-centre';
import {validateLearningPractice,type LearningPractice} from '../lib/learning-practice';

function assign(){return {action:'assign' as const,id:randomUUID(),workerIds:[randomUUID()],courseId:'fire' as const,contentVersion:catalogue.version,audience:'school-awareness' as const,scenarioIds:['school-fire-help'],mode:'guided' as const,dueAt:Date.now()+86400000,note:''};}
function record(version=archived.version,scenario=archived.scenarios.find(s=>s.id==='school-fire-help')!):LearningPractice{
 let at=Date.now()-10000;const createdAt=at,answers:LearningPractice['answers']=[];
 for(const step of scenario.steps){answers.push({kind:'decision',stepId:step.id,choiceId:step.choices.find(c=>c.correct)!.id,correct:true,hints:0,at:++at});if(step.requiresObservation)answers.push({kind:'observation',stepId:step.id,partId:step.part!,mode:'3d',at:++at});answers.push({kind:'continue',stepId:step.id,at:++at});}
 return {id:randomUUID(),workerId:randomUUID(),courseId:'fire',scenarioId:scenario.id,contentVersion:version,answers,index:scenario.steps.length,finished:true,guided:true,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt,updatedAt:at};
}
function assignment(r:LearningPractice):LaunchAssignment{return {id:randomUUID(),owner:'owner',worker_id:r.workerId,course_id:r.courseId,content_version:r.contentVersion,audience:'school-awareness',scenario_ids:JSON.stringify([r.scenarioId]),mode:'guided',due_at:Date.now()+10000,created_at:r.createdAt-10,cancelled_at:null,catalogue_snapshot:'{}',note:''};}
test('assignments pin current content and school awareness never expands to adult intervention',()=>{
 const a=assign();assert(centreCommand.safeParse(a).success);assert.equal(validateAssignment(a).certifiable,false);
 for(const changed of [{...a,scenarioIds:['extinguisher-check']},{...a,scenarioIds:['school-fire-help','school-fire-help']},{...a,workerIds:[a.workerIds[0],a.workerIds[0]]},{...a,contentVersion:'unknown'},{...a,dueAt:Date.now()-1}])assert.throws(()=>validateAssignment(changed));
 assert.equal(centreCommand.safeParse({...a,approved:true}).success,false);
});
test('old content is replayed against its exact bundled archive',()=>{
 const r=record();assert.doesNotThrow(()=>validateLearningPractice(r,r.workerId));
 assert.throws(()=>validateLearningPractice({...r,contentVersion:'unknown-archive'},r.workerId),/version/);
});
test('shared evidence excludes teach-back while retaining authored decisions and draft status',()=>{
 const r=record();r.answers.push({kind:'teach-back',text:'PRIVATE MEDICAL DETAIL DO NOT SHARE',at:++r.updatedAt});
 const shared=sharedPractice(r);assert(!JSON.stringify(shared).includes('PRIVATE'));assert.equal(shared.reflectionIncluded,false);assert.equal(shared.certifiable,false);assert.equal(shared.reviewStatus,'draft');assert.equal(shared.events.length,r.answers.length-1);
});
test('sharing requires complete matching mode, profile, course, version and new attempt',()=>{
 const r=record(),a=assignment(r);assert.doesNotThrow(()=>requireMatchingPractice(a,r));
 for(const value of [{...r,guided:false},{...r,finished:false},{...r,workerId:randomUUID()},{...r,contentVersion:'different'},{...r,courseId:'first-aid' as const},{...r,createdAt:a.created_at-1}])assert.throws(()=>requireMatchingPractice(a,value));
 assert.throws(()=>requireMatchingPractice({...a,cancelled_at:Date.now()},r),/cancelled/);
});
test('withdrawn evidence cannot complete an assignment and no completion issues credentials',()=>{
 const r=record(),a=assignment(r),s={assignment_id:a.id,scenario_id:r.scenarioId,withdrawn_at:null} as Submission;
 assert.equal(assignmentProgress(a,[s]).status,'practice-complete');assert.equal(assignmentProgress(a,[s]).certifiable,false);
 assert.equal(assignmentProgress(a,[{...s,withdrawn_at:Date.now()}]).completed,0);assert.equal(assignmentProgress({...a,due_at:1},[]).status,'overdue');assert.equal(assignmentProgress({...a,cancelled_at:1},[s]).status,'cancelled');
});
test('CSV prevents formula interpretation and quotes embedded punctuation',()=>{
 assert.equal(csvCell('=HYPERLINK("bad")'),'"\'=HYPERLINK(""bad"")"');assert.equal(csvCell('Learner, A'),'"Learner, A"');assert.equal(csvCell(' @SUM(A1)'),'"\' @SUM(A1)"');
});
function database(){const db=new DatabaseSync(':memory:');db.exec(`PRAGMA foreign_keys=ON;
 CREATE TABLE auth_user(id TEXT PRIMARY KEY);INSERT INTO auth_user VALUES('learner'),('owner'),('other');
 CREATE TABLE workers(owner TEXT,id TEXT,PRIMARY KEY(owner,id));INSERT INTO workers VALUES('owner','worker');
 CREATE TABLE centre_approvals(owner TEXT,status TEXT);INSERT INTO centre_approvals VALUES('owner','approved');
 CREATE TABLE learner_links(owner TEXT,worker_id TEXT,user_id TEXT);INSERT INTO learner_links VALUES('owner','worker','learner');
 CREATE TABLE team_members(owner TEXT,user_id TEXT,active INTEGER,role TEXT);
 CREATE TABLE learning_practice_snapshots(account_id TEXT,id TEXT,worker_id TEXT,digest TEXT);INSERT INTO learning_practice_snapshots VALUES('learner','attempt','worker','source');`);
 db.exec(readFileSync(new URL('../drizzle/0015_learning_centres.sql',import.meta.url),'utf8'));
 db.exec(readFileSync(new URL('../drizzle/0016_learning_centre_guards.sql',import.meta.url),'utf8'));
 db.exec(readFileSync(new URL('../drizzle/0017_learning_account_cleanup.sql',import.meta.url),'utf8'));
 db.exec(`INSERT INTO learning_course_assignments VALUES('owner','assignment','worker','fire','v1','school-awareness','["scenario"]','{}','guided',100,1,'owner','',NULL,NULL);
 INSERT INTO learning_course_submissions VALUES('owner','submission','assignment','worker','learner','attempt','source','scenario','{}',2,NULL);`);return db;}
test('database rejects self-review, foreign role, stale parent and evidence replacement',()=>{
 const db=database();try{
  const insert=db.prepare('INSERT INTO learning_review_events VALUES(?,?,?,?,?,?,?,?)');
  assert.throws(()=>insert.run('owner','self','submission','learner',null,'reviewed','Must not pass',3),/self-review/);
  assert.throws(()=>insert.run('owner','other','submission','other',null,'reviewed','Must not pass',3),/role-conflict/);
  insert.run('owner','one','submission','owner',null,'needs-information','Valid feedback',3);
  assert.throws(()=>insert.run('owner','stale','submission','owner',null,'reviewed','Stale feedback',4),/stale-parent/);
  insert.run('owner','two','submission','learner','one','correction-request','Learner correction',4);
  assert.throws(()=>db.exec("UPDATE learning_review_events SET note='rewrite'"),/immutable/);
  assert.throws(()=>db.exec("UPDATE learning_course_submissions SET evidence='{\"score\":100}'"),/immutable/);
  db.exec("UPDATE learning_course_submissions SET withdrawn_at=5");assert.throws(()=>insert.run('owner','three','submission','owner','two','reviewed','Gone evidence',6),/access-conflict/);
 }finally{db.close()}
});
test('sharing source and current enrolment must remain valid at the transaction boundary',()=>{
 const db=database();try{db.exec("DELETE FROM learner_links");assert.throws(()=>db.exec("INSERT INTO learning_course_submissions VALUES('owner','second','assignment','worker','learner','attempt','source','scenario','{}',7,NULL)"),/access-conflict/);}finally{db.close()}
});

test('assignment and observation commit checks reject revoked or self educator access',()=>{
 const db=database();try{
  const observation=db.prepare('INSERT INTO learning_course_observations VALUES(?,?,?,?,?,?,?,?,?,?,?)');
  assert.throws(()=>observation.run('owner','self-observation','assignment','learner','learner',3,'remote-simulation','{}','further-practice','Self observation',4),/access-conflict/);
  observation.run('owner','one-observation','assignment','owner','learner',3,'remote-simulation','{}','further-practice','Observed practice',4);
  assert.throws(()=>db.exec("UPDATE learning_course_observations SET note='rewrite'"),/immutable/);
  db.exec("UPDATE centre_approvals SET status='suspended'");
  assert.throws(()=>observation.run('owner','suspended','assignment','owner','learner',3,'remote-simulation','{}','further-practice','Suspended role',4),/access-conflict/);
  assert.throws(()=>db.exec("INSERT INTO learning_course_assignments VALUES('owner','next','worker','fire','v1','school-awareness','[]','{}','guided',100,1,'owner','',NULL,NULL)"),/access-conflict/);
 }finally{db.close()}
});
test('assignment identity is immutable and withdrawal cannot silently reshare evidence',()=>{
 const db=database();try{
  assert.throws(()=>db.exec("INSERT INTO learning_course_assignments VALUES('owner','assignment','worker','fire','v2','school-awareness','[]','{}','guided',100,1,'owner','',NULL,NULL) ON CONFLICT(owner,id) DO NOTHING"),/identity-conflict/);
  assert.throws(()=>db.exec("UPDATE learning_course_assignments SET content_version='v2'"),/immutable/);
  db.exec("UPDATE learning_course_submissions SET withdrawn_at=10");assert.throws(()=>db.exec("UPDATE learning_course_submissions SET withdrawn_at=NULL"),/withdrawal-immutable/);
 }finally{db.close()}
});

test('learner account deletion removes shared evidence without review foreign-key failure',()=>{
 const db=database();try{
  db.exec("INSERT INTO learning_review_events VALUES('owner','feedback','submission','owner',NULL,'reviewed','Synthetic feedback',3)");
  db.exec("INSERT INTO learning_course_observations VALUES('owner','observation','assignment','owner','learner',3,'remote-simulation','{}','further-practice','Synthetic observation',4)");
  db.exec("DELETE FROM auth_user WHERE id='learner'");
  for(const table of ['learning_course_submissions','learning_review_events','learning_course_observations'])assert.equal(db.prepare('SELECT count(*) AS n FROM '+table).get()!.n,0);
  assert.equal(db.prepare('SELECT count(*) AS n FROM learning_course_assignments').get()!.n,1);
 }finally{db.close()}
});

test('inbox upgrade preserves single-tenant state and never guesses for colliding legacy event IDs',()=>{
 const db=database();try{
  db.exec("INSERT INTO auth_user VALUES('second-owner'); INSERT INTO workers VALUES('second-owner','worker'); INSERT INTO centre_approvals VALUES('second-owner','approved'); INSERT INTO learner_links VALUES('second-owner','worker','learner');");
  db.exec("INSERT INTO learning_course_assignments VALUES('second-owner','assignment','worker','fire','v1','school-awareness','[]','{}','guided',100,1,'second-owner','',NULL,NULL)");
  db.exec("INSERT INTO learning_inbox_state VALUES('learner','assignment:assignment',8,9,10); INSERT INTO learning_inbox_state VALUES('learner','submission:submission',8,9,10)");
  const migration=readFileSync(new URL('../drizzle/0018_learning_inbox_scope.sql',import.meta.url),'utf8');db.exec(migration);
  assert.equal(db.prepare('SELECT count(*) AS n FROM learning_inbox_state').get()!.n,1);
  assert.deepEqual({...db.prepare('SELECT * FROM learning_inbox_state').get()!},{account_id:'learner',item_id:'owner:submission:submission',read_at:8,dismissed_at:9,updated_at:10});
  db.exec(migration);assert.equal(db.prepare('SELECT count(*) AS n FROM learning_inbox_state').get()!.n,1);
 }finally{db.close()}
});
