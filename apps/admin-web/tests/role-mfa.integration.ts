/** Run after role browser/native checks, with REQUIRE_STAFF_MFA=true on the isolated server. */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
import {base32} from '@better-auth/utils/base32';
import {createOTP} from '@better-auth/utils/otp';
const origin=process.env.BETTER_AUTH_URL!,fixture=process.env.SURAKSHA_ROLE_FIXTURE!;
assert(origin==='http://127.0.0.1:5196'&&fixture.includes('/.runtime/institution-0.17/role-workflows-'));
const f=JSON.parse(readFileSync(fixture,'utf8'));let checks=0,ip=0;
async function request(cookie:string,path:string,body?:unknown,status=200){const r=await fetch(origin+path,{method:body===undefined?'GET':'POST',redirect:'manual',headers:{Origin:origin,'Content-Type':'application/json',Cookie:cookie,'X-Real-IP':'198.51.100.'+(++ip%200+1)},body:body===undefined?undefined:JSON.stringify(body)});assert.equal(r.status,status,path);checks++;return r;}
for(const role of ['platform','admin','trainer','examiner','certifier','assessor','auditor','author','reviewer','viewer']){
 const actor=f.actors[role],r=await request('','/api/auth/sign-in/email',{email:actor.email,password:actor.password}),cookie=r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ');
 const d=await(await request(cookie,'/api/dashboard')).json() as any;assert.equal(d.context.role,role);assert.equal(d.context.mfaRequired,true);assert.deepEqual(d.stats,[]);assert.deepEqual(d.queue,[]);
 await request(cookie,'/institute-access');await request(cookie,'/account');
 await request(cookie,role==='platform'?'/api/platform-settings':role==='auditor'?'/api/institution-audit':'/api/institution',undefined,403);
}
for(const role of ['learner','personal','supporter']){const actor=f.actors[role],r=await request('','/api/auth/sign-in/email',{email:actor.email,password:actor.password}),cookie=r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ');assert.equal((await(await request(cookie,'/api/dashboard')).json() as any).context.mfaRequired,false);}
// Existing administrator is enrolled through actual TOTP endpoints, not a database flag.
const actor=f.actors.admin,jar=new Map<string,string>();
async function auth(path:string,body?:unknown,status=200){const r=await request([...jar].map(([k,v])=>k+'='+v).join('; '),'/api/auth/'+path,body,status);for(const c of r.headers.getSetCookie()){const [kv]=c.split(';'),i=kv.indexOf('=');if(/Max-Age=0(?:;|$)/.test(c))jar.delete(kv.slice(0,i));else jar.set(kv.slice(0,i),kv.slice(i+1));}return await r.json() as any;}
await auth('sign-in/email',{email:actor.email,password:actor.password});const enabled=await auth('two-factor/enable',{password:actor.password}),secret=new URL(enabled.totpURI).searchParams.get('secret')!;
const otp=await createOTP(new TextDecoder().decode(base32.decode(secret))).totp();await auth('two-factor/verify-totp',{code:otp});
let cookie=[...jar].map(([k,v])=>k+'='+v).join('; ');const active=await(await request(cookie,'/api/dashboard')).json() as any;assert.equal(active.context.mfaRequired,false);assert(active.stats.length>0);await request(cookie,'/api/institution');
await auth('sign-out',{});jar.clear();assert.equal((await auth('sign-in/email',{email:actor.email,password:actor.password})).twoFactorRedirect,true);assert.equal(await auth('get-session'),null);
await request([...jar].map(([k,v])=>k+'='+v).join('; '),'/api/institution',undefined,401);
await auth('two-factor/verify-backup-code',{code:enabled.backupCodes[0]});cookie=[...jar].map(([k,v])=>k+'='+v).join('; ');await request(cookie,'/api/institution');
// A new staff member can accept an invitation with MFA still required on protected work.
const email='mfa-invite-'+randomUUID()+'@example.test',password='Synthetic MFA '+randomUUID(),signup=await request('','/api/auth/sign-up/email',{email,password,name:'Synthetic MFA invitee'}),newCookie=signup.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ');
const invite=await(await request(cookie,'/api/admin/manage',{action:'member',email,role:'examiner',active:true})).json() as any;
await request(newCookie,'/institute-access');await request(newCookie,'/api/admin/session',{invitation:invite.invitation});const assigned=await(await request(newCookie,'/api/dashboard')).json() as any;assert.equal(assigned.context.role,'examiner');assert.equal(assigned.context.mfaRequired,true);await request(newCookie,'/api/identity?staff=1',undefined,403);
await auth('sign-out',{});jar.clear();await auth('sign-in/email',{email:actor.email,password:actor.password});const replay=await fetch(origin+'/api/auth/two-factor/verify-backup-code',{method:'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:[...jar].map(([k,v])=>k+'='+v).join('; ')},body:JSON.stringify({code:enabled.backupCodes[0]})});assert(replay.status>=400);
console.log(JSON.stringify({httpChecks:checks,staffRolesGated:10,nonStaffUnmonitored:3,totpEnrolment:true,passwordOnlyDenied:true,recoveryCodeOneUse:true,invitationBeforeMfa:true}));
