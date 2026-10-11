/** Isolated synthetic fixtures only: role dispatch, permission boundaries and access recovery. */
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync,writeFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
const origin=process.env.BETTER_AUTH_URL!,path=process.env.SURAKSHA_DB_PATH!;
assert(origin==='http://127.0.0.1:5196'&&path.includes('/.runtime/institution-0.17/'));
const db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON;PRAGMA busy_timeout=5000');
const f=JSON.parse(readFileSync(process.env.SURAKSHA_TEST_FIXTURE??'../../.runtime/institution-0.17/browser-fixture.json','utf8')),actors=f.actors;
let checks=0,ip=0;
async function call(actor:any,route:string,body?:unknown,status=200,base=origin){const r=await fetch(base+route,{method:body===undefined?'GET':'POST',redirect:'manual',headers:{Cookie:actor?.cookie??'',Origin:base,'Content-Type':'application/json','X-Real-IP':'198.51.100.'+(++ip%200+1)},body:body===undefined?undefined:JSON.stringify(body)});assert.equal(r.status,status,route+' '+await r.clone().text());checks++;return r;}
async function data(actor:any,route='/api/dashboard'){return await (await call(actor,route)).json() as Record<string,any>;}
async function create(role:string){const email='role-'+role+'-'+randomUUID()+'@example.test',password='Fixture only '+randomUUID(),r=await call(null,'/api/auth/sign-up/email',{email,password,name:'Synthetic '+role}),v=await r.json() as Record<string,any>;const actor={id:v.user.id,email,password,cookie:r.headers.getSetCookie().filter(v=>!v.includes('Max-Age=0')).map(v=>v.split(';')[0]).join('; ')};actors[role]=actor;return actor;}
try{
 const auditor=await create('auditor');await create('personal');
 db.prepare('INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,?,1,?)').run(actors.admin.id,auditor.email,auditor.id,'auditor',Date.now());
 db.prepare('UPDATE institution_people SET archived_at=NULL WHERE owner=? AND worker_id=?').run(actors.admin.id,f.workerId);
 db.prepare('UPDATE learning_support_links SET revoked_at=NULL,expires_at=? WHERE supporter_id=?').run(Date.now()+86400000,actors.supporter.id);
 actors.platform=actors.other;
 for(const role of ['platform','admin','trainer','examiner','certifier','assessor','auditor','author','reviewer','viewer','learner','personal','supporter']){
  const a=actors[role];a.cookie=a.cookie.split('; ').filter((s:string)=>!s.startsWith('suraksha_workspace=')).join('; ');
  const d=await data(a);assert.equal(d.context.role,role);assert.equal(d.context.home,'/dashboard/'+role);assert(d.stats.length>0,role+' stats');assert.equal(d.context.mfaRequired,false);checks+=4;
  const root=await call(a,'/',undefined,307);assert.equal(root.headers.get('location'),'/dashboard');
  const dispatch=await call(a,'/dashboard',undefined,307);assert.equal(dispatch.headers.get('location'),d.context.home);
  const wrong=await call(a,'/dashboard/not-my-role',undefined,307);assert.equal(wrong.headers.get('location'),d.context.home);
  if(!['personal','learner'].includes(role)){await call(a,'/studio',undefined,307);assert(d.actions.every((v:any)=>!v.href.startsWith('/studio')&&!v.href.startsWith('/identity?')));checks++;}
 }
 await call(null,'/api/dashboard',undefined,401);await call(actors.personal,'/platform',undefined,307);await call(auditor,'/coursework',undefined,307);
 for(const route of ['/api/records','/api/admin/manage','/api/institution','/api/learning-centre','/api/institution-awards','/api/identity?staff=1','/api/identity-reference?staff=1'])await call(auditor,route,undefined,403);
 await call(auditor,'/api/institution',{action:'unit',id:randomUUID(),kind:'cohort',name:'Forbidden',parentId:null},403);
 await call(auditor,'/api/institution-audit',{},405);
 const auditId=randomUUID(),auditAction='role.fixture.'+auditId;db.prepare('INSERT INTO audit_log VALUES(?,?,?,?,?,?,?,?)').run(auditId,actors.admin.id,actors.admin.id,actors.admin.email,auditAction,'private-target',JSON.stringify({private:'must-never-appear'}),Date.now());
 const audit=await data(auditor,'/api/institution-audit?action='+auditAction+'&format=csv');assert.equal(audit.events.length,1);assert.deepEqual(Object.keys(audit.events[0]).sort(),['action','actor','at','id']);assert(!JSON.stringify(audit).includes('private-target'));assert(!JSON.stringify(audit).includes('must-never-appear'));assert(!audit.csv.includes(actors.admin.email));checks+=5;
 // Explicit unknown or revoked selection must not fall back to another tenant's records.
 const invalid={...actors.examiner,cookie:actors.examiner.cookie+'; suraksha_workspace='+actors.other.id};const locked=await data(invalid);assert.equal(locked.context.workspaceUnavailable,true);assert.deepEqual(locked.stats,[]);assert.deepEqual(locked.queue,[]);checks+=3;
 await call(invalid,'/api/records',undefined,403);await call(invalid,'/coursework',undefined,307);await call(invalid,'/api/admin/session',{clear:true});
 const selected=await call(actors.examiner,'/api/admin/session',{owner:actors.admin.id});assert(selected.headers.get('set-cookie')?.includes(actors.admin.id));
 db.prepare('UPDATE team_members SET active=0 WHERE owner=? AND user_id=?').run(actors.admin.id,auditor.id);
 const stale={...auditor,cookie:auditor.cookie+'; suraksha_workspace='+actors.admin.id};const revoked=await data(stale);assert.equal(revoked.context.role,'personal');assert.deepEqual(revoked.stats,[]);await call(stale,'/api/institution-audit',undefined,403);checks+=2;
 db.prepare('UPDATE team_members SET active=1 WHERE owner=? AND user_id=?').run(actors.admin.id,auditor.id);
 // Multi-institute role changes require a membership and follow the selected workspace.
 db.prepare('INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,?,1,?)').run(actors.other.id,auditor.email,auditor.id,'reviewer',Date.now());
 const switched={...auditor,cookie:auditor.cookie+'; suraksha_workspace='+actors.other.id};assert.equal((await data(switched)).context.role,'reviewer');await call(switched,'/api/institution-audit',undefined,403);checks++;
 db.prepare('DELETE FROM team_members WHERE owner=? AND user_id=?').run(actors.other.id,auditor.id);
 assert.equal(db.prepare('PRAGMA integrity_check').get()?.integrity_check,'ok');
 writeFileSync(process.env.SURAKSHA_ROLE_FIXTURE??'../../.runtime/roles-0.18/browser-fixture.json',JSON.stringify({...f,actors}),{mode:0o600});
 console.log('Role integration passed: '+checks+' assertions, 13 role homes, default and explicit workspace selection, metadata-only audit, mutation denial, revocation and tenant isolation.');
}finally{db.close()}
