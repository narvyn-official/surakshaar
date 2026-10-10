import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,readdirSync,mkdtempSync,mkdirSync,rmSync,writeFileSync,statSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {spawnSync} from 'node:child_process';
import {DatabaseSync} from 'node:sqlite';
import {randomUUID} from 'node:crypto';
import {drizzle} from 'drizzle-orm/d1';
import {betterAuth} from 'better-auth/minimal';
import {drizzleAdapter} from '@better-auth/drizzle-adapter';
import * as schema from '../db/auth-schema';
import {SqliteD1} from '../lib/runtime/sqlite-d1';

function userId(data:unknown):string{
 assert(data&&typeof data==='object'&&'user' in data);
 const user=data.user;assert(user&&typeof user==='object'&&'id' in user&&typeof user.id==='string');return user.id;
}
function migrated(){
 const db=new SqliteD1(':memory:');
 for(const name of readdirSync('drizzle').filter(n=>/^\d{4}_[a-z0-9_]+\.sql$/.test(n)).sort())db.sqlite.exec(readFileSync(join('drizzle',name),'utf8'));
 return db;
}

test('real Drizzle auth schema preserves independent sessions and server-side revocation',async()=>{
 const db=migrated();try{
  const origin='http://127.0.0.1:5197';
  const auth=betterAuth({baseURL:origin,secret:randomUUID()+randomUUID(),database:drizzleAdapter(drizzle(db as unknown as D1Database),{provider:'sqlite',schema,transaction:false}),emailAndPassword:{enabled:true,minPasswordLength:15},session:{cookieCache:{enabled:false}},logger:{disabled:true}});
  async function call(path:string,body:unknown,cookie=''){
   const response=await auth.handler(new Request(origin+'/api/auth/'+path,{method:body===undefined?'GET':'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:cookie},body:body===undefined?undefined:JSON.stringify(body)}));
   return {status:response.status,data:await response.json(),cookie:response.headers.getSetCookie().map(v=>v.split(';')[0]).join('; ')};
  }
  const password='Disposable '+randomUUID();
  const a=await call('sign-up/email',{name:'Synthetic A',email:randomUUID()+'@example.test',password});
  const b=await call('sign-up/email',{name:'Synthetic B',email:randomUUID()+'@example.test',password});
  assert.equal(a.status,200);assert.equal(b.status,200);assert(a.cookie.includes('session_token'));assert.notEqual(userId(a.data),userId(b.data));
  assert.equal(userId((await call('get-session',undefined,a.cookie)).data),userId(a.data));
  assert.equal(userId((await call('get-session',undefined,b.cookie)).data),userId(b.data));
  assert.equal((await call('get-session',undefined)).data,null);
  const sessions=await db.prepare('SELECT user_id,expires_at FROM auth_session ORDER BY user_id').raw();assert.equal(sessions.length,2);assert(sessions.every(row=>Number((row as unknown[])[1])>Date.now()));
  assert.equal((await call('sign-out',{},a.cookie)).status,200);
  assert.equal((await call('get-session',undefined,a.cookie)).data,null);
  assert.equal(userId((await call('get-session',undefined,b.cookie)).data),userId(b.data));
  // Revocation removes the server-side session; the other account stays signed in.
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM auth_session WHERE user_id=?').bind(userId(a.data)).first('n'),0);
  await db.prepare('DELETE FROM auth_user WHERE id=?').bind(userId(b.data)).run();
  assert.equal((await call('get-session',undefined,b.cookie)).data,null);
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM auth_session').first('n'),0);
 }finally{db.close()}
});

test('real snapshot triggers reject a second account and roll back earlier batch writes and audit side effects',async()=>{
 const db=migrated();try{
  for(const id of ['review-a','review-b'])await db.prepare('INSERT INTO auth_user(id,name,email,email_verified,created_at,updated_at) VALUES(?,?,?,0,1,1)').bind(id,id,id+'@example.test').run();
  await db.prepare('INSERT INTO learning_profile_accounts VALUES(?,?,?)').bind('profile-a','review-a',1).run();
  const snapshot=(account:string,id:string)=>db.prepare('INSERT INTO learning_practice_snapshots VALUES(?,?,?,?,?,?,?,?,?,?)').bind(account,id,'profile-a','fire','test-content',id+'-digest',null,2,2,'{"finished":true}');
  await assert.rejects(db.batch([snapshot('review-a','first'),snapshot('review-b','second')]),/learning-profile-account-conflict/);
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM learning_practice_snapshots').first('n'),0);
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM learning_practice_heads').first('n'),0);
  await snapshot('review-a','first').run();
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM learning_practice_heads WHERE account_id=?').bind('review-b').first('n'),0);
  await db.exec('CREATE TABLE adapter_review_audit(value INTEGER)');
  await db.batch([db.prepare('UPDATE auth_user SET name=? WHERE id=?').bind('changed','missing'),db.prepare('INSERT INTO adapter_review_audit SELECT changes() WHERE changes()>0')]);
  assert.equal(await db.prepare('SELECT COUNT(*) n FROM adapter_review_audit').first('n'),0);
  await db.batch([db.prepare('UPDATE auth_user SET name=? WHERE id=?').bind('changed','review-a'),db.prepare('INSERT INTO adapter_review_audit SELECT changes() WHERE changes()>0')]);
  assert.equal(await db.prepare('SELECT value FROM adapter_review_audit').first('value'),1);
  assert.deepEqual(await db.prepare('PRAGMA foreign_key_check').raw(),[]);
 }finally{db.close()}
});

test('migration runner is idempotent, creates private backup, detects modified history and rolls back failing migration',()=>{
 const directory=mkdtempSync(join(tmpdir(),'suraksha-node-review-'));
 const path=join(directory,'data.sqlite'),backups=join(directory,'backups'),migrationDir=join(directory,'migrations');
 const run=(migrations=resolve('drizzle'))=>spawnSync(process.execPath,['scripts/migrate-node.mjs',migrations],{encoding:'utf8',env:{...process.env,SURAKSHA_DB_PATH:path,SURAKSHA_BACKUP_DIR:backups},timeout:15000});
 try{
  const first=run();assert.equal(first.status,0,first.stderr);const second=run();assert.equal(second.status,0,second.stderr);assert.match(second.stdout,/0 applied/);
  assert.equal(statSync(path).mode&0o077,0);assert(readdirSync(backups).some(n=>n.endsWith('.sqlite')));
  const db=new DatabaseSync(path);const total=Number(db.prepare('SELECT COUNT(*) n FROM suraksha_schema_migrations').get()!.n);db.close();assert.equal(total,readdirSync('drizzle').filter(n=>n.endsWith('.sql')).length);
  // Isolated migration directories deliberately exercise failure without touching repository migrations.
  mkdirSync(migrationDir);
  writeFileSync(join(migrationDir,'0000_motionless_jane_foster.sql'),'SELECT 1;');assert.notEqual(run(migrationDir).status,0);
  rmSync(join(migrationDir,'0000_motionless_jane_foster.sql'));
  writeFileSync(join(migrationDir,'9999_review_failure.sql'),'CREATE TABLE review_partial(id INTEGER); INSERT INTO missing_review_table VALUES(1);');assert.notEqual(run(migrationDir).status,0);
  const checked=new DatabaseSync(path);try{assert.equal(checked.prepare("SELECT COUNT(*) n FROM sqlite_master WHERE name='review_partial'").get()!.n,0);assert.equal(checked.prepare('SELECT COUNT(*) n FROM suraksha_schema_migrations').get()!.n,total)}finally{checked.close()}
 }finally{rmSync(directory,{recursive:true,force:true})}
});
