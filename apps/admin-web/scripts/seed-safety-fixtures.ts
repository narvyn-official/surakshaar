/** Explicit local-only fixtures. No HTTP bypass, no provider calls, no production seed. */
import {realpathSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import {hashPassword} from 'better-auth/crypto';
const path=process.env.SURAKSHA_DB_PATH??'',origin=process.env.BETTER_AUTH_URL??'';
if(process.env.SURAKSHA_SAFETY_FIXTURES!=='true'||!realpathSync(path).includes('/.runtime/safety-pilot/')||!/^http:\/\/localhost:\d+$/.test(origin))throw new Error('Fixtures require an explicit flag, localhost and an isolated .runtime/safety-pilot database.');
const db=new DatabaseSync(path),now=Date.now();db.exec('PRAGMA foreign_keys=ON; PRAGMA busy_timeout=5000;');
if(db.prepare("SELECT count(*) n FROM auth_user WHERE id NOT LIKE 'safety-fixture-%'").get()!.n!==0){db.close();throw new Error('Refusing to modify a database containing non-fixture accounts.');}
try{
 const password=await hashPassword('Local Safety Fixture 2026!');
 for(const role of ['admin','other','trainer','viewer','certifier','learner']){
  const id='safety-fixture-'+role,email='safety-'+role+'@example.test';
  db.prepare('INSERT OR IGNORE INTO auth_user(id,name,email,email_verified,created_at,updated_at) VALUES(?,?,?,0,?,?)').run(id,'LOCAL FIXTURE '+role,email,now,now);
  db.prepare("INSERT OR IGNORE INTO auth_account(id,user_id,account_id,provider_id,password,created_at,updated_at) VALUES(?,?,?,'credential',?,?,?)").run(id,id,id,password,now,now);
  if(['admin','other'].includes(role)){
   db.prepare("INSERT OR IGNORE INTO centre_approvals(owner,status,requested_at,reviewed_at,reviewed_by,reason) VALUES(?,'approved',?,?,'local-fixture','SYNTHETIC TEST CENTRE')").run(id,now,now);
   db.prepare("INSERT OR IGNORE INTO training_centres(owner,name,site,updated_at) VALUES(?,?,?,?)").run(id,'LOCAL FIXTURE · '+role,'Synthetic review workspace',now);
  }else if(role!=='learner')db.prepare('INSERT OR IGNORE INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,?,1,?)').run('safety-fixture-admin',email,id,role,now);
 }
 if(process.argv.includes('--with-updates'))for(const [id,sourceId,topic,title,text] of [
  ['safety-fixture-fire','usfa-extinguishers','fire','Fixture: a fire reference changed','SYNTHETIC EXCERPT FOR UI TESTING. This is invented test content, not safety guidance or a live TinyFish result. Use this row to test review notes and status transitions.'],
  ['safety-fixture-aid','sja-cpr','first-aid','Fixture: a first aid reference changed','SYNTHETIC EXCERPT FOR UI TESTING. This row exercises the first aid review queue. It is not a clinical recommendation or material from the named publisher.']
 ]){
  const source=db.prepare('SELECT * FROM safety_sources WHERE id=?').get(sourceId)!;
  db.prepare("INSERT OR IGNORE INTO safety_versions(id,source_id,source_revision,title,publisher,source_url,retrieved_at,content_hash,content,excerpt,topic,jurisdiction,change_kind,origin) VALUES(?,?,?,?,?,?,? ,?,?,?,?,?,'baseline','fixture')").run(id,sourceId,source.revision,title,source.publisher,source.url,now,id,text,text,topic,source.jurisdiction);
 }
 console.log('Local Safety Updates fixtures prepared. All generated content is labelled fixture; no provider was called.');
}finally{db.close()}
