/** Production standalone runtime smoke test. Only loopback hosts; generated accounts in isolated SQLite. */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
const origin=process.env.TEST_ORIGIN??'http://127.0.0.1:5189';
assert(['127.0.0.1','localhost'].includes(new URL(origin).hostname),'Only isolated loopback QA is allowed');
async function call(path,body,cookie='',source=origin){
 const response=await fetch(origin+path,{method:body===undefined?'GET':'POST',headers:{Origin:source,'Content-Type':'application/json',Cookie:cookie},body:body===undefined?undefined:JSON.stringify(body)});
 const data=await response.json();return {status:response.status,data,cookie:response.headers.getSetCookie().filter(value=>!value.includes('Max-Age=0')).map(value=>value.split(';')[0]).join('; ')};
}
const email=`qa-${randomUUID()}@example.test`,password=`Private QA ${randomUUID()}`;
const signup=await call('/api/auth/sign-up/email',{email,password,name:'Synthetic Node account'});assert.equal(signup.status,200);assert(signup.cookie.includes('session_token'));
const learner=await call('/api/learner',undefined,signup.cookie);assert.equal(learner.status,200);assert.equal(learner.data.user.email,email);assert.equal(learner.data.links.length,0);
const admin=await call('/api/admin/session',undefined,signup.cookie);assert.equal(admin.status,200);assert.equal(admin.data.current,null);
const unauth=await call('/api/learner');assert.equal(unauth.status,401);
const cross=await call('/api/auth/change-password',{currentPassword:password,newPassword:`Changed ${password}`,revokeOtherSessions:true},signup.cookie,'https://untrusted.example');assert([400,403].includes(cross.status));
const reset=await call('/api/auth/request-password-reset',{email});assert.equal(reset.status,503,'No mail delivery is configured in isolated QA');
const logout=await call('/api/auth/sign-out',{},signup.cookie);assert.equal(logout.status,200);assert.equal((await call('/api/learner',undefined,signup.cookie)).status,401);
const wrong=await call('/api/auth/sign-in/email',{email,password:`Wrong ${password}`});assert.equal(wrong.status,401);
const login=await call('/api/auth/sign-in/email',{email,password});assert.equal(login.status,200);assert(login.cookie.includes('session_token'));
const changedPassword=`Changed ${password}`;const change=await call('/api/auth/change-password',{currentPassword:password,newPassword:changedPassword,revokeOtherSessions:true},login.cookie);assert.equal(change.status,200);
assert.equal((await call('/api/auth/sign-in/email',{email,password})).status,401);
const secondLogin=await call('/api/auth/sign-in/email',{email,password:changedPassword});assert.equal(secondLogin.status,200);
const attacker=await call('/api/auth/sign-up/email',{email:`qa-${randomUUID()}@example.test`,password,name:'Other synthetic account'});assert.equal(attacker.status,200);
assert.equal((await call('/api/learning-practice?workerId='+randomUUID(),undefined,attacker.cookie)).status,200);
assert([400,403,404].includes((await call('/api/learning-practice/shares',{action:'revoke',id:randomUUID(),workerId:randomUUID(),attemptId:randomUUID()},attacker.cookie)).status));
assert.equal((await call('/api/auth/sign-out',{},secondLogin.cookie)).status,200);assert.equal((await call('/api/auth/sign-out',{},attacker.cookie)).status,200);
console.log('Standalone Node auth smoke passed: signup, signin, server identity, no auto staff access, origin rejection, recovery-unavailable truth, logout invalidation, password change and share ownership. Synthetic isolated accounts only.');
