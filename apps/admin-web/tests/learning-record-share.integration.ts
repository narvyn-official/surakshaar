import assert from 'node:assert/strict';
import {randomUUID,createHash} from 'node:crypto';
import {account,localDatabase} from './auth-client.mjs';
import catalogue from '../../../content/learning/scenarios.json';
import type {LearningPractice} from '../lib/learning-practice';
const origin='http://localhost:5173',users:Awaited<ReturnType<typeof account>>[]=[];
async function call(person:typeof users[number]|null,path:string,body?:unknown){const response=await fetch(origin+'/api/'+path,{method:body?'POST':'GET',headers:{Origin:origin,'Content-Type':'application/json',...(person?{Cookie:person.cookie}:{})},body:body?JSON.stringify(body):undefined});assert(response.headers.get('cache-control')?.split(',').map(v=>v.trim()).includes('no-store'));return {status:response.status,data:JSON.parse(await response.text())};}
function fixture(action:(db:ReturnType<typeof localDatabase>)=>void){const db=localDatabase();try{action(db)}finally{db.close()}}
try{
 const owner=await account(),other=await account();users.push(owner,other);const worker=randomUUID();let at=Date.now()-10000;const createdAt=at,scene=catalogue.scenarios.find(s=>s.id==='school-fire-help')!,answers:LearningPractice['answers']=[];
 for(const step of scene.steps){answers.push({kind:'decision',stepId:step.id,choiceId:step.choices.find(c=>c.correct)!.id,correct:true,hints:0,at:++at});if(step.requiresObservation)answers.push({kind:'observation',stepId:step.id,partId:step.part!,mode:'3d',at:++at});answers.push({kind:'continue',stepId:step.id,at:++at})}
 answers.push({kind:'teach-back',text:'PRIVATE reflection that must never be published',at:++at});
 const record:LearningPractice={id:randomUUID(),workerId:worker,courseId:'fire',scenarioId:scene.id,contentVersion:catalogue.version,finished:true,guided:true,index:scene.steps.length,answers,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt,updatedAt:at};
 assert.equal((await call(owner,'learning-practice',{schemaVersion:1,workerId:worker,attempts:[record]})).status,200);
 const query='learning-practice/shares?workerId='+worker+'&attemptId='+record.id;
 assert.equal((await call(null,query)).status,401);assert.equal((await call(other,query)).status,403);
 const command={action:'create',id:randomUUID(),workerId:worker,attemptId:record.id,expiresInDays:7,confirmPublicMetadata:true};
 assert.equal((await call(owner,'learning-practice/shares',{...command,confirmPublicMetadata:false})).status,400);
 assert.equal((await call(owner,'learning-practice/shares',{...command,token:'chosen'})).status,400);
 assert.equal((await call(other,'learning-practice/shares',command)).status,403);
 const created=await call(owner,'learning-practice/shares',command);assert.equal(created.status,201);assert.equal(created.data.certifiable,false);assert.equal(created.data.tokenShownOnce,true);
 const token=new URLSearchParams(created.data.path.split('#')[1]).get('token')!;assert.match(token,/^[A-Za-z0-9_-]{43}$/);
 assert.equal((await call(owner,'learning-practice/shares',command)).status,409);
 const active=await call(null,'public-learning-record',{token});assert.equal(active.status,200);assert.equal(active.data.status,'active');assert.equal(active.data.certifiable,false);assert.equal(active.data.record.reviewStatus,'draft');assert.equal(active.data.record.contentVersion,catalogue.version);
 const body=JSON.stringify(active.data);for(const forbidden of [worker,record.id,owner.email,owner.userId,'PRIVATE reflection','answers','correct','source_digest'])assert(!body.includes(forbidden));
 const hash=createHash('sha256').update(JSON.stringify({purpose:'formative-public-share-v1',token})).digest('hex');
 fixture(db=>{const row=db.prepare('SELECT * FROM learning_record_shares WHERE account_id=? AND id=?').get(owner.userId,command.id)!;assert.equal(row.token_hash,hash);assert(!JSON.stringify(row).includes(token));for(const table of ['credentials','certification_requests'])assert.equal(db.prepare(`SELECT count(*) AS n FROM ${table} WHERE owner=?`).get(owner.userId)!.n,0)});
 const list=await call(owner,query);assert.equal(list.data.shares.length,1);assert(!JSON.stringify(list.data).includes(token));assert(!JSON.stringify(list.data).includes(hash));
 assert.equal((await call(null,'public-learning-record',{token:'A'.repeat(43)})).data.status,'unavailable');assert.equal((await call(null,'public-learning-record',{token:'malformed'})).status,400);
 const revoke={action:'revoke',id:command.id,workerId:worker,attemptId:record.id};assert.equal((await call(other,'learning-practice/shares',revoke)).status,403);
 assert.equal((await call(owner,'learning-practice/shares',revoke)).status,200);assert.equal((await call(owner,'learning-practice/shares',revoke)).status,200);
 const revoked=await call(null,'public-learning-record',{token});assert.equal(revoked.data.status,'revoked');assert.equal(revoked.data.record,undefined);
 const incomplete={...record,id:randomUUID(),index:0,answers:[],finished:false,updatedAt:record.createdAt};assert.equal((await call(owner,'learning-practice',{schemaVersion:1,workerId:worker,attempts:[incomplete]})).status,200);assert.equal((await call(owner,'learning-practice/shares',{...command,id:randomUUID(),attemptId:incomplete.id})).status,400);
 console.log('Formative QR integration PASS: private source replay, explicit consent, anonymous metadata, 256-bit hashed-only token, one-time secret delivery, account isolation, idempotent revocation, invalid-token handling and no legacy certificate issuance.');
}finally{fixture(db=>{for(const user of users)db.prepare('DELETE FROM auth_user WHERE id=?').run(user.userId)})}
