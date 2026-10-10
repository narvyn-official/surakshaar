import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,readdirSync} from 'node:fs';
import {DatabaseSync} from 'node:sqlite';
import {permitted,roles} from '../lib/access';
import {defaultPolicy,institutionPolicy,parsePeopleCsv} from '../lib/institution';
test('role separation keeps content authors away from people and photographs',()=>{
 for(const role of roles){assert.equal(permitted(role,'identity'),['examiner','assessor'].includes(role));assert.equal(permitted(role,'certify'),['certifier','assessor'].includes(role));}
 for(const role of ['author','reviewer'] as const){assert(!permitted(role,'read'));assert(!permitted(role,'write'));assert(permitted(role,'content'));}
 assert(permitted('examiner','review'));assert(!permitted('examiner','write'));assert(!permitted('admin','content-review'));
});
test('bulk enrolment previews quoted names and rejects ambiguous or duplicate identities',()=>{
 assert.deepEqual(parsePeopleCsv('employee_number,name,sector\r\nA01,"Yadav, Akash",Other\r\n'),[{employeeNumber:'A01',name:'Yadav, Akash',sector:'Other'}]);
 assert.throws(()=>parsePeopleCsv('employee_number,name,sector\na,One,Other\nA,Two,Other'),/duplicate/);
 assert.throws(()=>parsePeopleCsv('name,id\nOne,A'),/header/);
 assert.throws(()=>parsePeopleCsv('employee_number,name,sector\na,"Name,Other'),/incomplete/);
 assert.throws(()=>parsePeopleCsv('employee_number,name,sector\na,Name,Wrong'),/row 2/);
 assert(!institutionPolicy.safeParse({...defaultPolicy,maximumAttempts:0}).success);
 assert(!institutionPolicy.safeParse({...defaultPolicy,allowSelfCertification:true}).success);
});
test('institution migration enforces tenant identity, immutable policies and unique active issuers',()=>{
 const db=new DatabaseSync(':memory:');try{db.exec('PRAGMA foreign_keys=ON');for(const name of readdirSync('drizzle').filter(f=>f.endsWith('.sql')).sort())db.exec(readFileSync('drizzle/'+name,'utf8'));
 for(const owner of ['one','two'])db.prepare('INSERT INTO training_centres VALUES(?,?,?,?)').run(owner,owner,'Fixture',Date.now());
 db.prepare('INSERT INTO institution_units VALUES(?,?,?,?,?,NULL,?)').run('one','branch','branch','Main',null,Date.now());
 assert.throws(()=>db.prepare('INSERT INTO institution_units VALUES(?,?,?,?,?,NULL,?)').run('two','dept','department','Test','branch',Date.now()),/FOREIGN KEY/);
 db.prepare('INSERT INTO institution_policies VALUES(?,?,?,?,?)').run('one',1,JSON.stringify(defaultPolicy),'fixture',Date.now());
 assert.throws(()=>db.exec("UPDATE institution_policies SET version=2"),/immutable/);
 db.prepare('INSERT INTO institution_signing_keys VALUES(?,?,?,?,?,NULL)').run('one','k1','{}','cipher',Date.now());
 assert.throws(()=>db.prepare('INSERT INTO institution_signing_keys VALUES(?,?,?,?,?,NULL)').run('one','k2','{}','cipher',Date.now()),/UNIQUE/);
 db.prepare('INSERT INTO institution_plans VALUES(?,?,?,?,?,?,?,?)').run('one','Fixture',1,null,0,'Fixture allowance',Date.now(),'operator');
 db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run('one','worker1','One','Other',Date.now());
 assert.throws(()=>db.prepare('INSERT INTO workers VALUES(?,?,?,?,?)').run('one','worker2','Two','Other',Date.now()),/quota/);
 assert.equal(db.prepare('PRAGMA integrity_check').get()?.integrity_check,'ok');assert.equal(db.prepare('PRAGMA foreign_key_check').all().length,0);
 }finally{db.close()}
});
