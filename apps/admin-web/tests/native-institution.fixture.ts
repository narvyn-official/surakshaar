/** Local-only, disposable fixture for the Android instrumentation workflow. Never deployed. */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {mkdtempSync,readFileSync,writeFileSync,unlinkSync,rmdirSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {account,localDatabase} from './auth-client.mjs';
import catalogue from '../../../content/learning/scenarios.json';

const origin='http://localhost:5173';
type Account=Awaited<ReturnType<typeof account>>;
async function call(person:Account,path:string,body:unknown){
 const response=await fetch(origin+'/api/'+path,{method:'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:person.cookie},body:JSON.stringify(body)});
 const data=await response.json();assert(response.ok,`Synthetic ${path} failed (${response.status})`);assert(data&&typeof data==='object'&&!Array.isArray(data));return data as Record<string,unknown>;
}
function cleanup(ids:string[]){const db=localDatabase();try{db.exec('PRAGMA foreign_keys=ON');for(const id of ids){
 const person=db.prepare('SELECT email FROM auth_user WHERE id=?').get(id);if(!person)continue;
 assert(typeof person.email==='string'&&/^qa-[a-f0-9-]+@example\.test$/.test(person.email),'Refusing to remove a non-synthetic account');
 for(const table of ['learning_review_events','learning_course_observations','learning_course_submissions','learning_course_assignments','learner_links','team_members','audit_log','workers','training_centres','centre_approvals'])db.prepare(`DELETE FROM ${table} WHERE owner=?`).run(id);
 for(const table of ['learning_inbox_state','learning_record_shares','learning_practice_snapshots','learning_practice_heads','learning_profile_accounts','learning_sync_budget'])db.prepare(`DELETE FROM ${table} WHERE account_id=?`).run(id);
 db.prepare('DELETE FROM auth_user WHERE id=?').run(id);
 }}finally{db.close()}}

if(process.argv[2]==='cleanup'){
 const path=process.argv[3];assert(path?.startsWith(join(tmpdir(),'suraksha-native-qa-'))&&path.endsWith('/fixture.json'));
 const fixture=JSON.parse(readFileSync(path,'utf8'));assert.equal(fixture.purpose,'isolated-native-institution-qr');cleanup(fixture.accountIds);unlinkSync(path);rmdirSync(path.slice(0,-'/fixture.json'.length));console.log('Synthetic native fixture removed.');
}else{
 const users:Account[]=[];
 try{
 const a=await account({approved:true});users.push(a);const b=await account({approved:true});users.push(b);const learner=await account();users.push(learner);const workerId=randomUUID();
 const db=localDatabase();try{for(const [person,name] of [[a,'Native QA North'],[b,'Native QA South']] as const){
  db.prepare('UPDATE training_centres SET name=? WHERE owner=?').run(name,person.userId);
  db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run(person.userId,workerId,'Synthetic native learner','Unspecified',Date.now());
 }}finally{db.close()}
 for(const person of [a,b]){const invitation=await call(person,'learners',{action:'invite',workerId,email:learner.email});await call(learner,'learner',{action:'join',workerId,invitation:invitation.invitation})}
 await call(a,'learning-centre',{action:'assign',id:randomUUID(),workerIds:[workerId],courseId:'fire',contentVersion:catalogue.version,audience:'school-awareness',scenarioIds:['school-fire-help'],mode:'guided',dueAt:Date.now()+86400000,note:'Synthetic native UI check'});
 let at=Date.now()-10000;const createdAt=at,scenario=catalogue.scenarios.find(s=>s.id==='school-fire-help')!,answers:unknown[]=[];
 for(const step of scenario.steps){answers.push({kind:'decision',stepId:step.id,choiceId:step.choices.find(c=>c.correct)!.id,correct:true,hints:0,at:++at});answers.push({kind:'continue',stepId:step.id,at:++at})}
 answers.push({kind:'teach-back',text:'Synthetic private reflection must remain private.',at:++at});
 const attemptId=randomUUID();await call(learner,'learning-practice',{schemaVersion:1,workerId,attempts:[{id:attemptId,workerId,scenarioId:scenario.id,courseId:'fire',contentVersion:catalogue.version,index:scenario.steps.length,answers,finished:true,guided:true,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt,updatedAt:at}]});
 const dir=mkdtempSync(join(tmpdir(),'suraksha-native-qa-'));const path=join(dir,'fixture.json');
 writeFileSync(path,JSON.stringify({purpose:'isolated-native-institution-qr',origin,accountIds:users.map(u=>u.userId),workerId,attemptId,email:learner.email,password:learner.password,north:a.userId,south:b.userId}),{mode:0o600});
 console.log('Private disposable fixture: '+path);
 }catch(error){cleanup(users.map(u=>u.userId));throw error}
}
