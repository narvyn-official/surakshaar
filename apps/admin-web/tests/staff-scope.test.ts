import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readdirSync,readFileSync} from 'node:fs';
import {learnerScope,unitScope,scopeAssertion} from '../lib/staff-scope';
import {roleActions} from '../lib/role-workspace';
test('staff scope enforces tenant, descendants, archival and current membership in SQL',()=>{
 const db=new DatabaseSync(':memory:');try{
 db.exec('PRAGMA foreign_keys=ON');for(const f of readdirSync('drizzle').filter(f=>f.endsWith('.sql')).sort())db.exec(readFileSync('drizzle/'+f,'utf8'));
 for(const owner of ['a','b'])db.prepare('INSERT INTO training_centres VALUES(?,?,?,?)').run(owner,owner,'test',1);
 for(const [owner,id,parent] of [['a','dept',null],['a','child','dept'],['a','sibling',null],['b','dept',null]])db.prepare("INSERT INTO institution_units VALUES(?,?,'department',?,?,NULL,1)").run(owner,id,id,parent);
 db.prepare("INSERT INTO team_members(owner,email,user_id,role,active,updated_at,scope_mode) VALUES('a','s@test','s','examiner',1,1,'units')").run();db.prepare("INSERT INTO team_member_units VALUES('a','s@test','dept')").run();
 for(const [owner,id,unit] of [['a','one','dept'],['a','two','child'],['a','three','sibling'],['a','unplaced',null],['b','other','dept']]){db.prepare('INSERT INTO workers VALUES(?,?,?,\'Other\',1)').run(owner,id,id);db.prepare('INSERT INTO institution_people VALUES(?,?,?,?,NULL)').run(owner,id,id,unit);}
 const actor={owner:'a',restricted:true,user:{userId:'s',email:'s@test',displayName:'Scope test'}},scope=learnerScope(actor,'w.id');
 const visible=()=>db.prepare('SELECT w.id FROM workers w WHERE w.owner=? AND '+scope.sql+' ORDER BY w.id').all('a',...scope.args).map(r=>r.id);
 assert.deepEqual(visible(),['one','two']);
 db.exec("UPDATE institution_people SET unit_id='sibling' WHERE owner='a' AND worker_id='one'");assert.deepEqual(visible(),['two']);
 db.exec("UPDATE institution_units SET archived_at=2 WHERE owner='a' AND id='child'");assert.deepEqual(visible(),[]);
 db.exec("UPDATE institution_units SET archived_at=NULL WHERE owner='a' AND id='child'");db.exec("UPDATE team_members SET active=0 WHERE owner='a'");assert.deepEqual(visible(),[]);
 db.exec("UPDATE team_members SET active=1 WHERE owner='a'");db.exec("UPDATE institution_people SET archived_at=2 WHERE owner='a' AND worker_id='two'");assert.deepEqual(visible(),[]);
 assert.throws(()=>unitScope(actor,'w.id; DELETE FROM workers'),/internal scope/);
 assert.throws(()=>db.prepare("INSERT INTO team_member_units VALUES('a','s@test','missing')").run(),/FOREIGN KEY/);
 assert.equal(learnerScope({...actor,restricted:false},'w.id').sql,'1=1');
 }finally{db.close();}
});
test('scoped dashboards exclude institution-wide imports, content and audit',()=>{
 for(const role of ['trainer','examiner','certifier','assessor','viewer'] as const){const actions=roleActions(role,true);assert(actions.some(a=>a.href==='/coursework'));assert(actions.every(a=>!a.href.startsWith('/workspace')&&!a.href.includes('content')&&a.href!=='/audit'));}
});

import {SqliteD1} from '../lib/runtime/sqlite-d1';
test('scope changes roll back a consequential write in its own transaction',async()=>{
 const raw=new SqliteD1(':memory:');try{for(const f of readdirSync('drizzle').filter(f=>f.endsWith('.sql')).sort())raw.sqlite.exec(readFileSync('drizzle/'+f,'utf8'));
 raw.sqlite.exec("INSERT INTO training_centres VALUES('a','A','Test',1); INSERT INTO centre_approvals(owner,status,requested_at,reviewed_at,reviewed_by,reason) VALUES('a','approved',1,1,'test','fixture'); INSERT INTO workers VALUES('a','w','Original','Other',1); INSERT INTO team_members(owner,email,user_id,role,active,updated_at,scope_mode) VALUES('a','s@test','s','trainer',1,1,'all')");
 const actor={owner:'a',role:'trainer' as const,restricted:false,user:{userId:'s',email:'s@test',displayName:'Scope test'}},storage=raw as unknown as D1Database;
 await raw.batch([scopeAssertion(actor,'w',storage) as never]);assert.equal(raw.sqlite.prepare('SELECT COUNT(*) n FROM staff_scope_assertions').get()?.n,0);
 raw.sqlite.exec("UPDATE team_members SET scope_mode='units'");
 await assert.rejects(()=>raw.batch([raw.prepare("UPDATE workers SET name='Must rollback' WHERE owner='a'"),scopeAssertion(actor,'w',storage) as never]),/staff_scope_current/);
 assert.equal(raw.sqlite.prepare('SELECT name FROM workers').get()?.name,'Original');
 }finally{raw.close();}
});
