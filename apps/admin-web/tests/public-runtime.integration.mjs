/** Explicit disposable smoke on the dedicated Suraksha preview. No email or provider calls. */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {existsSync,readFileSync,writeFileSync} from 'node:fs';
const origin='https://surakshaar.213-210-36-82.sslip.io';
assert.equal(process.env.SURAKSHA_ALLOW_PREVIEW_QA,'1');
const fixturePath=process.env.SURAKSHA_QA_FIXTURE;assert(fixturePath?.startsWith('/tmp/suraksha-public-qa-'));
assert(!existsSync(fixturePath),'Clean the previous disposable fixture before starting another run');
const fixture={purpose:'suraksha-public-smoke',accounts:[]};
async function call(path,body,cookie='',source=origin){
 const response=await fetch(origin+path,{method:body===undefined?'GET':'POST',headers:{Origin:source,'Content-Type':'application/json',Cookie:cookie},body:body===undefined?undefined:JSON.stringify(body)});
 const data=await response.json();return {status:response.status,data,cookie:response.headers.getSetCookie().filter(value=>!value.includes('Max-Age=0')).map(value=>value.split(';')[0]).join('; ')};
}
const cap=await call('/api/account-capabilities');assert.equal(cap.status,200);assert.equal(cap.data.passwordRecovery,false);
for(let i=0;i<2;i++){
 const email=`qa-${randomUUID()}@example.test`,password=`Private QA ${randomUUID()}`;
 const reply=await call('/api/auth/sign-up/email',{email,password,name:'Disposable Suraksha QA'});assert.equal(reply.status,200);
 fixture.accounts.push({id:reply.data.user.id,email,password,cookie:reply.cookie});writeFileSync(fixturePath,JSON.stringify(fixture),{mode:0o600});assert(reply.cookie.includes('session_token'));
}
const [a,b]=fixture.accounts;
const learner=await call('/api/learner',undefined,a.cookie);assert.equal(learner.status,200);assert.equal(learner.data.user.email,a.email);
assert.equal((await call('/api/admin/session',undefined,a.cookie)).data.current,null);
const catalogue=JSON.parse(readFileSync(new URL('../../../content/learning/scenarios.json',import.meta.url),'utf8'));
const scenario=catalogue.scenarios.find(value=>value.id==='school-fire-help'),workerId=randomUUID(),attemptId=randomUUID();let at=Date.now()-10000;const createdAt=at,answers=[];
for(const step of scenario.steps){answers.push({kind:'decision',stepId:step.id,choiceId:step.choices.find(value=>value.correct).id,correct:true,hints:0,at:++at});answers.push({kind:'continue',stepId:step.id,at:++at})}
answers.push({kind:'teach-back',text:'Synthetic private reflection.',at:++at});
fixture.workerId=workerId;fixture.attemptId=attemptId;writeFileSync(fixturePath,JSON.stringify(fixture),{mode:0o600});
const record={id:attemptId,workerId,scenarioId:scenario.id,courseId:'fire',contentVersion:catalogue.version,index:scenario.steps.length,answers,finished:true,guided:true,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt,updatedAt:at};
assert.equal((await call('/api/learning-practice',{schemaVersion:1,workerId,attempts:[record]},a.cookie)).status,200);
assert.equal((await call('/api/learning-practice?workerId='+workerId,undefined,b.cookie)).status,403);
const sharing=await call('/api/learning-practice/shares',{action:'create',id:randomUUID(),workerId,attemptId,expiresInDays:1,confirmPublicMetadata:true},a.cookie);assert.equal(sharing.status,201);
const token=new URL(sharing.data.path,origin).hash.slice('#token='.length);const visible=await call('/api/public-learning-record',{token});assert.equal(visible.data.status,'active');assert.equal(visible.data.certifiable,false);assert(!JSON.stringify(visible.data).includes(a.email));assert(!JSON.stringify(visible.data).includes('Synthetic private reflection'));
assert.equal((await call('/api/learning-practice/shares',{action:'revoke',id:sharing.data.id,workerId,attemptId},a.cookie)).status,200);assert.equal((await call('/api/public-learning-record',{token})).data.status,'revoked');
assert.equal((await call('/api/learning-practice?workerId='+workerId,undefined,a.cookie)).data.records.length,1);
const foreign=await call('/api/auth/sign-out',{},a.cookie,'https://untrusted.example');assert([400,403].includes(foreign.status));
assert.equal((await call('/api/learner',undefined,a.cookie)).status,200);
assert.equal((await call('/api/auth/sign-out',{},a.cookie)).status,200);assert.equal((await call('/api/learner',undefined,a.cookie)).status,401);
assert.equal((await call('/api/learner',undefined,b.cookie)).status,200);
const signed=await call('/api/auth/sign-in/email',{email:a.email,password:a.password});assert.equal(signed.status,200);a.cookie=signed.cookie;
writeFileSync(fixturePath,JSON.stringify(fixture),{mode:0o600});
console.log('Public HTTPS QA passed: real accounts, private persistence and isolation, anonymous QR verify/revoke, no auto admin, foreign-origin denial, session logout isolation, re-login; recovery remains disabled. Synthetic fixture retained privately for restart proof and cleanup.');
