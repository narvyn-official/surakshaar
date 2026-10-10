import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,readdirSync} from 'node:fs';
import {SqliteD1} from '../lib/runtime/sqlite-d1';
import {configFrom,type Source} from '../lib/safety/model';
import {equipmentCommand} from '../lib/safety/search-model';
import {parseSearch,runEquipmentSearch,searchEquipment,searchHistory} from '../lib/safety/search';
import {changeLessonLink,lessonPanel} from '../lib/safety/lessons';
import {lessonCommand} from '../lib/safety/lesson-model';
import {fetchPublicPage,parseResult} from '../lib/safety/provider';
import {enqueue,type SafetyActor} from '../lib/safety/store';
import {workOne} from '../lib/safety/worker';
const config=configFrom({TINYFISH_API_KEY:'synthetic-test-key',SAFETY_DAILY_CALL_LIMIT:5});
const query={action:'search' as const,host:'www.zoll.com',model:'AED Plus',publicOnly:true as const};
const result={title:'Synthetic manual',url:'https://www.zoll.com/example.pdf',snippet:'Synthetic reference result',host:'www.zoll.com'};
function fixture(){
 const raw=new SqliteD1(':memory:');for(const f of readdirSync('drizzle').filter(f=>/^\d{4}_.*\.sql$/.test(f)).sort())raw.sqlite.exec(readFileSync('drizzle/'+f,'utf8'));
 for(const id of ['a','b','trainer'])raw.sqlite.prepare('INSERT INTO auth_user(id,name,email,email_verified,created_at,updated_at) VALUES(?,?,?,0,?,?)').run(id,'Fixture',id+'@example.test',Date.now(),Date.now());
 for(const id of ['a','b'])raw.sqlite.prepare("INSERT INTO centre_approvals(owner,status,requested_at,reason) VALUES(?,'approved',?,'Synthetic')").run(id,Date.now());
 raw.sqlite.prepare("INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES('a','trainer@example.test','trainer','trainer',1,?)").run(Date.now());
 const actor=(owner='a',role='admin'):SafetyActor=>({owner,role,user:{userId:role==='trainer'?'trainer':owner,email:owner+'@example.test'}});
 return {raw,db:raw as unknown as D1Database,actor};
}
test('equipment input and output constrain public data and exact manufacturer hosts',()=>{
 assert(equipmentCommand.safeParse(query).success);
 for(const q of [{...query,publicOnly:false},{...query,owner:'b'},{...query,model:'name@example.com'},{...query,model:'site:evil.com'}])assert(!equipmentCommand.safeParse(q).success);
 assert.throws(()=>configFrom({SAFETY_EQUIPMENT_HOSTS:'www.zoll.com/path'}));
 const body={results:[result,...['http://www.zoll.com/a','https://www.zoll.com.evil.com/a','https://127.0.0.1/','https://www.zoll.com/a?secret=1','javascript:alert(1)'].map(url=>({...result,url})),result]};
 assert.deepEqual(parseSearch(body,'www.zoll.com'),[result]);
 assert.throws(()=>parseSearch({results:[{...result,snippet:42}]},query.host),/malformed/);
 assert.equal(parseSearch({results:[{...result,title:'<script>text</script>'}]},query.host)[0].title,'text');
});
test('Search transport has a fixed endpoint, server key, India locale and validated response; quota is not retried',async()=>{
 let calls=0;
 const rows=await searchEquipment(query,config,async(input,init)=>{calls++;const url=new URL(String(input));assert.equal(url.origin,'https://api.search.tinyfish.ai');assert.equal(url.searchParams.get('include_domains'),query.host);assert.equal(url.searchParams.get('location'),'IN');assert.equal(url.searchParams.get('page'),'0');assert.equal(init?.redirect,'error');assert.equal((init?.headers as Record<string,string>)['X-API-Key'],config.key);return Response.json({results:[result]})});
 assert.deepEqual(rows,[result]);assert.equal(calls,1);
 for(const [status,code] of [[401,'provider-auth'],[402,'provider-quota'],[429,'rate-limit'],[503,'provider-unavailable']] as const)await assert.rejects(searchEquipment(query,config,async()=>new Response('{}',{status})),new RegExp(code));
 await assert.rejects(searchEquipment({...query,host:'evil.com'},config),/unsafe-url/);
 await assert.rejects(searchEquipment(query,config,async()=>new Response('x'.repeat(1_000_001),{headers:{'Content-Type':'application/json'}})),/malformed/);
});
test('Search caches per workspace, shares Fetch budget and bounds concurrency without exposing other history',async()=>{
 const f=fixture();try{
 let calls=0;const collect=async()=>{calls++;return [result]};
 const first=await runEquipmentSearch(f.db,f.actor(),query,config,async()=>true,collect);
 const again=await runEquipmentSearch(f.db,f.actor(),query,config,async()=>true,collect);assert.equal(again.cached,true);assert.equal(first.id,again.id);assert.equal(calls,1);
 assert.equal((await searchHistory(f.db,'b')).length,0);
 await runEquipmentSearch(f.db,f.actor('b'),query,config,async()=>true,collect);assert.equal(calls,2);
 f.raw.sqlite.prepare("UPDATE safety_sources SET status='approved' WHERE id='usfa-extinguishers'").run();
 await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async s=>parseResult({results:[{url:s.url,final_url:s.url,title:'Fixture',published_date:null,format:'markdown',text:'SYNTHETIC PUBLIC REFERENCE. '+ 'Only a fixture for deterministic reference tests. '.repeat(4)}],errors:[]},s,config.hosts));
 assert.equal(f.raw.sqlite.prepare('SELECT calls FROM safety_budget').get()!.calls,3);
 f.raw.sqlite.exec('UPDATE safety_worker_slots SET expires_at=9999999999999');
 await assert.rejects(runEquipmentSearch(f.db,f.actor(),{...query,model:'Other model'},config,async()=>true,collect),/No provider slot/);
 f.raw.sqlite.exec('UPDATE safety_worker_slots SET expires_at=0');
 await assert.rejects(runEquipmentSearch(f.db,f.actor(),{...query,model:'Other model'},config,async()=>false,collect),/Forbidden/);
 await assert.rejects(runEquipmentSearch(f.db,f.actor('a','viewer'),query,config,async()=>true,collect),/Forbidden/);
 }finally{f.raw.close()}
});
test('Search failure preserves history and releases leases; revoked access never saves results',async()=>{
 const f=fixture();try{
 let authorised=0;
 await assert.rejects(runEquipmentSearch(f.db,f.actor(),query,config,async()=>++authorised===1,async()=>[result]),/access-revoked/);
 const row=(await searchHistory(f.db,'a'))[0];assert.equal(row.status,'failed');assert.deepEqual(row.results,[]);assert.equal(row.error,'access-revoked');
 assert.equal(f.raw.sqlite.prepare('SELECT sum(expires_at) AS n FROM safety_worker_slots').get()!.n,0);
 await assert.rejects(runEquipmentSearch(f.db,f.actor(),query,{...config,dailyCalls:0},async()=>true,async()=>{throw Error('must not run')}),/budget/);
 assert.equal(f.raw.sqlite.prepare('SELECT calls FROM safety_budget').get()!.calls,1);
 }finally{f.raw.close()}
});
test('document download handlers omit unsupported HTML selectors without bypassing preflight',async()=>{
 const source={url:'https://www.zoll.com/example.ashx',title:'Fixture'} as Source;let checked=false;
 await fetchPublicPage(source,config,async()=>{checked=true},async(_url,init)=>{assert(checked);const body=JSON.parse(String(init?.body));assert(!('exclude_selectors' in body));return Response.json({results:[{url:source.url,final_url:source.url,title:null,published_date:null,format:'markdown',text:'SYNTHETIC DOCUMENT. '.repeat(10)}],errors:[]})});
});
test('lesson links isolate tenants and roles, reopen on new references, reject stale decisions and never publish content',async()=>{
 const f=fixture();try{
 const panel=await lessonPanel(f.db,f.actor()),target=panel.targets.find(t=>t.course==='fire')!;
 const command={action:'link' as const,sourceId:'usfa-extinguishers',contentVersion:panel.contentVersion,scenarioId:target.id,stepId:target.steps[0].id,aspect:'model-detail' as const,rationale:'Synthetic test: inspect the model label.'};
 assert(lessonCommand.safeParse(command).success);assert(!lessonCommand.safeParse({...command,owner:'b'}).success);
 await assert.rejects(changeLessonLink(f.db,f.actor(),{...command,stepId:'invalid'}),/Invalid/);
 await assert.rejects(changeLessonLink(f.db,f.actor(),{...command,contentVersion:'old'}),/Record conflict/);
 await changeLessonLink(f.db,f.actor('a','trainer'),command);
 await assert.rejects(changeLessonLink(f.db,f.actor(),command),/already exists/);
 assert.equal((await lessonPanel(f.db,f.actor('b'))).links.length,0);
 let link=(await lessonPanel(f.db,f.actor())).links[0];assert.equal(link.state,'source-disabled');
 f.raw.sqlite.exec("UPDATE safety_sources SET status='approved' WHERE id='usfa-extinguishers'");
 const addVersion=(id:string,origin='tinyfish-fetch')=>f.raw.sqlite.prepare("INSERT INTO safety_versions(id,source_id,source_revision,title,publisher,source_url,retrieved_at,content_hash,content,excerpt,topic,jurisdiction,change_kind,origin) VALUES(?,'usfa-extinguishers',1,'Synthetic','Synthetic','https://www.usfa.fema.gov/example',? ,?,'Synthetic text','Synthetic excerpt','fire','Test','changed',?)").run(id,Date.now(),id,origin);
 addVersion('fixture','fixture');assert.equal((await lessonPanel(f.db,f.actor())).links[0].state,'awaiting-reference');
 addVersion('live1');link=(await lessonPanel(f.db,f.actor())).links[0];assert.equal(link.state,'needs-review');
 const assess={action:'assess' as const,id:link.id,revision:link.revision,versionId:'live1',decision:'no-change' as const,notes:'Synthetic evidence: no editorial change.'};
 await assert.rejects(changeLessonLink(f.db,f.actor('a','trainer'),assess),/Forbidden/);
 await assert.rejects(changeLessonLink(f.db,f.actor('b'),assess),/Record conflict/);
 await changeLessonLink(f.db,f.actor(),assess);assert.equal((await lessonPanel(f.db,f.actor())).links[0].state,'no-change');
 await assert.rejects(changeLessonLink(f.db,f.actor(),assess),/Record conflict/);
 addVersion('live2');assert.equal((await lessonPanel(f.db,f.actor())).links[0].state,'needs-review');
 await assert.rejects(changeLessonLink(f.db,f.actor(),{...assess,revision:2}),/Record conflict/);
 await changeLessonLink(f.db,f.actor(),{...assess,revision:2,versionId:'live2',decision:'revision-needed'});
 assert.equal((await lessonPanel(f.db,f.actor())).links[0].state,'revision-needed');
 f.raw.sqlite.exec("UPDATE safety_sources SET revision=2 WHERE id='usfa-extinguishers'");assert.equal((await lessonPanel(f.db,f.actor())).links[0].state,'awaiting-reference');
 await assert.rejects(changeLessonLink(f.db,f.actor('b'),{action:'unlink',id:link.id,revision:3}),/Record conflict/);
 await changeLessonLink(f.db,f.actor(),{action:'unlink',id:link.id,revision:3});assert.equal((await lessonPanel(f.db,f.actor())).links.length,0);
 for(const table of ['attempts','credentials'])assert.equal(f.raw.sqlite.prepare(`SELECT count(*) n FROM ${table}`).get()!.n,0);
 assert.equal(f.raw.sqlite.prepare("SELECT count(*) n FROM audit_log WHERE action='safety.lesson-assessed'").get()!.n,2);
 }finally{f.raw.close()}
});
