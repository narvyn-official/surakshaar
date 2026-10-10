import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,readdirSync} from 'node:fs';
import {SqliteD1} from '../lib/runtime/sqlite-d1';
import {configFrom,requireSafetyRole,requireSourceOperator,sourceCommand,workspaceCommand,type Source} from '../lib/safety/model';
import {sourceUrl,isPublicAddress} from '../lib/safety/url-policy';
import {publicPreflight} from '../lib/safety/network';
import {CollectionError,fetchPublicPage,parseResult,normalizeContent} from '../lib/safety/provider';
import {enqueue,panelData,reviewUpdate,type SafetyActor} from '../lib/safety/store';
import {workOne,reserveBudget,scheduleDue} from '../lib/safety/worker';
const config=configFrom({TINYFISH_API_KEY:'synthetic-test-key'});
function fixture(){
 const raw=new SqliteD1(':memory:');for(const f of readdirSync('drizzle').filter(f=>/^\d{4}_.*\.sql$/.test(f)).sort())raw.sqlite.exec(readFileSync('drizzle/'+f,'utf8'));
 for(const id of ['a','b','trainer','viewer','certifier'])raw.sqlite.prepare('INSERT INTO auth_user(id,name,email,email_verified,created_at,updated_at) VALUES(?,?,?,0,?,?)').run(id,'Fixture '+id,id+'@example.test',Date.now(),Date.now());
 for(const id of ['a','b'])raw.sqlite.prepare("INSERT INTO centre_approvals(owner,status,requested_at,reason) VALUES(?,'approved',?,'Synthetic test fixture')").run(id,Date.now());
 for(const role of ['trainer','viewer','certifier'])raw.sqlite.prepare('INSERT INTO team_members(owner,email,user_id,role,active,updated_at) VALUES(?,?,?,?,1,?)').run('a',role+'@example.test',role,role,Date.now());
 raw.sqlite.prepare("UPDATE safety_sources SET status='approved' WHERE id='usfa-extinguishers'").run();
 const db=raw as unknown as D1Database;const actor=(id='a',role='admin'):SafetyActor=>({owner:id==='b'?'b':'a',role,user:{userId:id,email:id+'@example.test'}});
 const source=raw.sqlite.prepare("SELECT * FROM safety_sources WHERE id='usfa-extinguishers'").get() as unknown as Source;
 return {raw,db,actor,source};
}
const fixtureText='SYNTHETIC TEST CONTENT. Check that the exit is clear before selecting equipment. Do not enter a smoke-filled room. Follow the local emergency plan and staff instructions.';
const responseFor=(s:Source,text=fixtureText)=>({results:[{url:s.url,final_url:s.url,title:'Synthetic fixture title',text,published_date:null,format:'markdown'}],errors:[]});
test('strict roles, schema and scheduling defaults protect staff-only boundary',()=>{
 for(const role of ['viewer','certifier','learner','unknown'])for(const action of ['read','collect','review','schedule','cancel'] as const)assert.throws(()=>requireSafetyRole(role,action),/Forbidden/);
 assert.doesNotThrow(()=>requireSafetyRole('trainer','review','needs-review'));assert.throws(()=>requireSafetyRole('trainer','review','approved'));assert.throws(()=>requireSafetyRole('trainer','schedule'));assert.throws(()=>requireSourceOperator(false));
 assert(!workspaceCommand.safeParse({action:'collect',owner:'b'}).success);assert(!sourceCommand.safeParse({action:'propose',status:'approved'}).success);
 assert.equal(config.scheduleEnabled,false);assert.throws(()=>configFrom({SAFETY_CONCURRENCY:4}));assert.throws(()=>configFrom({SAFETY_DAILY_CALL_LIMIT:-1}));
 assert.equal(configFrom({TINYFISH_API_KEY:'   '}).key,undefined);
});
test('new burn and cut references are proposed and do not inherit approval',()=>{
 const f=fixture();try{
  for(const id of ['nhs-burns','nhs-cuts']){
   const source=f.raw.sqlite.prepare('SELECT * FROM safety_sources WHERE id=?').get(id) as unknown as Source;
   assert.equal(source.status,'proposed');assert.equal(source.last_success_at,null);assert.equal(sourceUrl(source.url,config.hosts),source.url);
  }
  assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_sources').get()!.n,10);
 }finally{f.raw.close()}
});
test('live provenance and filtered counts never turn fixtures into provider evidence',async()=>{
 const f=fixture();try{
  f.raw.sqlite.prepare(`INSERT INTO safety_versions(id,source_id,source_revision,title,publisher,source_url,retrieved_at,content_hash,content,excerpt,topic,jurisdiction,change_kind,origin) VALUES('demo',?,1,'Synthetic demo','Fixture',?,9999999999999,'fixture-hash','Fixture text','Fixture excerpt','fire','Test','baseline','fixture')`).run(f.source.id,f.source.url);
  const before=await panelData(f.db,f.actor(),config,false,'all',null);
  assert.deepEqual({...before.collection},{lastSuccessfulAt:null,liveCount:0,fixtureCount:1});
  assert.equal((await panelData(f.db,f.actor(),config,false,'all',null,'tinyfish-fetch')).updates.length,0);
  await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async s=>parseResult(responseFor(s),s,config.hosts));
  const live=await panelData(f.db,f.actor(),config,false,'all',null,'tinyfish-fetch');
  assert.equal(live.collection.liveCount,1);assert.equal(live.collection.fixtureCount,1);assert(live.collection.lastSuccessfulAt!<Date.now()+1000);
  assert.equal(live.updates.length,1);assert.equal(live.updates[0].origin,'tinyfish-fetch');assert.equal(live.counts.new,1);
  await reviewUpdate(f.db,f.actor(),live.updates[0].id,'needs-review','Fixture review',0);
  assert.equal((await panelData(f.db,f.actor('b'),config,false,'all',null,'tinyfish-fetch')).counts.new,1);
  const demo=await panelData(f.db,f.actor(),config,false,'all',null,'fixture');
  assert.equal(demo.updates.length,1);assert.equal(demo.updates[0].id,'demo');assert.equal(demo.counts['needs-review'],0);
 }finally{f.raw.close()}
});
test('URL policy rejects credentials, IPs, internal hosts, redirects, query tricks and scheme confusion',()=>{
 for(const raw of ['http://www.usfa.fema.gov/','https://www.usfa.fema.gov@127.0.0.1/','https://127.0.0.1/','https://[::1]/','https://2130706433/','https://metadata.google.internal/','https://www.usfa.fema.gov:444/','https://www.usfa.fema.gov/?url=http://localhost','https://www.usfa.fema.gov/%2e%2e/private','https://www.usfa.fema.gov/#private','javascript:alert(1)','file:///etc/passwd','https://www.usfa.fema.gov.evil.com/'])assert.throws(()=>sourceUrl(raw,config.hosts),raw);
 for(const ip of ['127.0.0.1','10.0.0.1','0.0.0.0','169.254.169.254','172.31.0.1','192.168.1.1','100.64.0.1','192.0.2.1','198.18.0.1','203.0.113.1','224.0.0.1','::1','::ffff:127.0.0.1','fd00::1','fe80::1','2001:db8::1','2002:7f00:1::','3fff::1'])assert.equal(isPublicAddress(ip),false,ip);
 for(const ip of ['8.8.8.8','1.1.1.1','2606:4700:4700::1111'])assert(isPublicAddress(ip));
});
test('provider schema rejects malformed and redirected data, preserves missing dates and meaningful words',()=>{
 const f=fixture();try{
 const body=responseFor(f.source);const parsed=parseResult(body,f.source,config.hosts);assert.equal(parsed.publishedDate,null);assert.equal(parsed.hash.length,64);
 assert.equal(normalizeContent('## Heading\nDo  not enter.\nCookie settings'),'Heading\nDo not enter.');assert.equal(normalizeContent('Keep <30 and >20 units.'),'Keep <30 and >20 units.');
 assert.notEqual(parseResult(responseFor(f.source,fixtureText.replace('Do not','Do')),f.source,config.hosts).hash,parsed.hash);
 assert.notEqual(parseResult(responseFor(f.source,fixtureText+' Stay 3 metres away.'),f.source,config.hosts).hash,parseResult(responseFor(f.source,fixtureText+' Stay 2 metres away.'),f.source,config.hosts).hash);
 for(const bad of [null,{}, {results:[],errors:[]}, {results:[...body.results,...body.results],errors:[]},{results:[{...body.results[0],text:{html:'bad'}}],errors:[]},{results:[{...body.results[0],final_url:'http://127.0.0.1/'}],errors:[]},{results:[{...body.results[0],final_url:'https://www.usfa.fema.gov/other'}],errors:[]}])assert.throws(()=>parseResult(bad,f.source,config.hosts));
 assert.equal(parseResult({results:[{...body.results[0],published_date:'not a date'}],errors:[]},f.source,config.hosts).publishedDate,null);
 assert.equal(parseResult({results:[{...body.results[0],published_date:'2026-02-30'}],errors:[]},f.source,config.hosts).publishedDate,null);
 assert.throws(()=>parseResult({results:[{...body.results[0],text:'x'.repeat(250001)}],errors:[]},f.source,config.hosts),/malformed/);
 }finally{f.raw.close()}
});
test('preflight rejects private or mixed DNS before connection, pins the selected IP, and refuses redirects',async()=>{
 const url='https://www.usfa.fema.gov/example',publicAddress={address:'8.8.8.8',family:4};let connections=0;
 for(const addresses of [[],[{address:'127.0.0.1',family:4}],[publicAddress,{address:'169.254.169.254',family:4}],[{address:'::ffff:127.0.0.1',family:6}]])await assert.rejects(publicPreflight(url,config.hosts,{resolve:async()=>addresses,status:async()=>{connections++;return 200}}),/unsafe-url/);
 assert.equal(connections,0);
 await publicPreflight(url,config.hosts,{resolve:async host=>{assert.equal(host,'www.usfa.fema.gov');return [publicAddress]},status:async (approved,address)=>{assert.equal(approved.href,url);assert.deepEqual(address,publicAddress);return 200}});
 for(const code of [301,302,307,308])await assert.rejects(publicPreflight(url,config.hosts,{resolve:async()=>[publicAddress],status:async()=>code}),/unsafe-url/);
 await assert.rejects(publicPreflight(url,config.hosts,{resolve:async()=>{throw Error('DNS failed')},status:async()=>200}),/source-unavailable/);
});
test('Fetch sends only public URLs, validates failures and never follows provider redirects',async()=>{
 const f=fixture();let calls=0;const transport:typeof fetch=async(url,init)=>{calls++;assert.equal(url,'https://api.fetch.tinyfish.ai');assert.equal(init?.redirect,'error');const body=JSON.parse(init?.body as string);assert.deepEqual(body.urls,[f.source.url]);assert.deepEqual(Object.keys(body).sort(),['format','iframe_links','image_links','links','per_url_timeout_ms','ttl','urls']);return Response.json(responseFor(f.source));};
 try{
 await assert.rejects(fetchPublicPage(f.source,{...config,key:undefined},async()=>{},transport),/missing-key/);assert.equal(calls,0);
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{throw new CollectionError('unsafe-url')},transport),/unsafe-url/);assert.equal(calls,0);
 await fetchPublicPage(f.source,config,async()=>{},transport);assert.equal(calls,1);
 for(const [status,code] of [[401,'provider-auth'],[402,'provider-quota'],[429,'rate-limit'],[503,'provider-unavailable']] as const){await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>new Response('{}',{status,headers:{'Retry-After':'120'}})),e=>e instanceof CollectionError&&e.code===code&&(status!==429||e.retryAfterMs===120000));}
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>{throw new DOMException('timeout','TimeoutError')}),/timeout/);
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>new Response('not-json',{headers:{'content-type':'application/json'}})),/malformed/);
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>new Response('{}',{headers:{'content-type':'application/json','content-length':'1000001'}})),/malformed/);
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>new Response(new ReadableStream({start(controller){controller.enqueue(new Uint8Array(1000001));controller.close()}}),{headers:{'content-type':'application/json'}})),/malformed/);
 await assert.rejects(fetchPublicPage(f.source,config,async()=>{},async()=>Response.json({results:[],errors:[{url:f.source.url,error:'invalid_redirect_url'}]})),/unsafe-url/);
 }finally{f.raw.close()}
});
test('async jobs deduplicate submissions and content; budgets are atomic and last success survives failures',async()=>{
 const f=fixture();try{
 await assert.rejects(enqueue(f.db,f.actor(),{...config,key:undefined}),/No TinyFish/);
 const [a,b]=await Promise.all([enqueue(f.db,f.actor(),config),enqueue(f.db,f.actor(),config)]);assert.equal(a.id,b.id);
 assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_jobs').get()!.n,1);
 let calls=0;const collect=async(s:Source)=>{calls++;return parseResult(responseFor(s),s,config.hosts)};
 await workOne(f.db,config,collect);assert.equal(calls,1);assert.equal(f.raw.sqlite.prepare('SELECT status FROM safety_jobs').get()!.status,'completed');
 await enqueue(f.db,f.actor('b'),config);await workOne(f.db,config,collect);assert.equal(calls,1,'shared recent public collection avoids a second provider call');
 f.raw.sqlite.exec('UPDATE safety_sources SET last_success_at=1');await enqueue(f.db,f.actor(),config);await workOne(f.db,config,collect);assert.equal(calls,2);assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,1);
 f.raw.sqlite.exec('UPDATE safety_sources SET last_success_at=1');await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async()=>{throw new CollectionError('provider-auth')});assert.equal(f.raw.sqlite.prepare("SELECT last_success_at FROM safety_sources WHERE id='usfa-extinguishers'").get()!.last_success_at,1);assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,1);
 f.raw.sqlite.exec('DELETE FROM safety_budget');const outcomes=await Promise.all(Array.from({length:10},()=>reserveBudget(f.db,{...config,dailyCalls:2})));assert.equal(outcomes.filter(Boolean).length,2);
 assert.equal(await reserveBudget(f.db,{...config,dailyCalls:20,callCostMicros:1,dailyCostMicros:0}),false);
 }finally{f.raw.close()}
});
test('review decisions, notes and audits stay scoped to the active workspace; stale/final decisions protected',async()=>{
 const f=fixture();try{
 await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async s=>parseResult(responseFor(s),s,config.hosts));const id=f.raw.sqlite.prepare('SELECT id FROM safety_versions').get()!.id as string;
 await reviewUpdate(f.db,f.actor('trainer','trainer'),id,'needs-review','Synthetic review note',0);
 await assert.rejects(reviewUpdate(f.db,f.actor('trainer','trainer'),id,'approved','Synthetic approval',1),/Forbidden/);
 await assert.rejects(reviewUpdate(f.db,f.actor(),id,'approved','Synthetic approval',0),/Record conflict/);
 await reviewUpdate(f.db,f.actor(),id,'approved','Synthetic staff-reference approval',1);
 await assert.rejects(reviewUpdate(f.db,f.actor('trainer','trainer'),id,'new','Reopen final decision',2),/Forbidden/);
 const a=await panelData(f.db,f.actor(),config,false,'all',null),b=await panelData(f.db,f.actor('b'),config,false,'all',null);
 assert.equal(a.updates[0].status,'approved');assert.equal(b.updates[0].status,'new');assert.equal(b.updates[0].notes,'');assert.equal(b.audit.length,0);assert(!('content' in a.updates[0]));
 await assert.rejects(reviewUpdate(f.db,f.actor('b'),id,'rejected','A guessed other-workspace revision',2),/Record conflict/);
 await reviewUpdate(f.db,f.actor('b'),id,'rejected','Separate institution decision',0);assert.equal((await panelData(f.db,f.actor(),config,false,'all',null)).updates[0].status,'approved');
 assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM attempts').get()!.n,0);assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM credentials').get()!.n,0);
 assert.throws(()=>f.raw.sqlite.exec("UPDATE safety_versions SET excerpt='changed'"),/immutable/);
 }finally{f.raw.close()}
});
test('retries bounded, partial failure retained, revoked authorisation prevents collection',async()=>{
 const f=fixture();try{
 f.raw.sqlite.exec("UPDATE safety_sources SET status='approved' WHERE id='sja-cpr'");await enqueue(f.db,f.actor(),config);
 let failures=0;const collect=async(s:Source)=>{if(s.id==='sja-cpr'){failures++;throw new CollectionError('timeout',true)}return parseResult(responseFor(s),s,config.hosts)};
 await workOne(f.db,config,collect);await workOne(f.db,config,collect);f.raw.sqlite.exec('UPDATE safety_job_items SET next_attempt_at=0');await workOne(f.db,config,collect);
 assert.equal(failures,2);assert.equal(f.raw.sqlite.prepare('SELECT status FROM safety_jobs').get()!.status,'partial');assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,1);
 const revokedJob=await enqueue(f.db,f.actor('trainer','trainer'),config);f.raw.sqlite.exec("UPDATE team_members SET active=0 WHERE user_id='trainer'");await workOne(f.db,config,()=>{throw Error('must not reach provider')});await workOne(f.db,config,()=>{throw Error('must not reach provider')});
 assert.equal(f.raw.sqlite.prepare('SELECT error FROM safety_jobs WHERE id=?').get(revokedJob.id)!.error,'access-revoked');
 await scheduleDue(f.db,config);assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_jobs').get()!.n,2);
 }finally{f.raw.close()}
});
test('cancellation and source changes during an in-flight request fence the result',async()=>{
 for(const mutation of ["UPDATE safety_jobs SET status='cancelled'","UPDATE safety_sources SET status='disabled',revision=revision+1","UPDATE centre_approvals SET status='suspended'"]){const f=fixture();try{await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async s=>{f.raw.sqlite.exec(mutation);return parseResult(responseFor(s),s,config.hosts)});assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,0)}finally{f.raw.close()}}
});
test('returning to earlier wording is a new change; repeated identical wording is deduplicated',async()=>{
 const f=fixture();try{
  for(const text of [fixtureText,fixtureText+' A changed number: 2.',fixtureText,fixtureText]){
   f.raw.sqlite.exec('UPDATE safety_sources SET last_success_at=1');await enqueue(f.db,f.actor(),config);await workOne(f.db,config,async s=>parseResult(responseFor(s,text),s,config.hosts));
  }
  assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,3);
  const last=f.raw.sqlite.prepare('SELECT previous_excerpt FROM safety_versions ORDER BY rowid DESC LIMIT 1').get()!;
  assert.match(last.previous_excerpt as string,/changed number: 2/);
 }finally{f.raw.close()}
});
test('workers serialise a source across workspaces, enforce concurrency and recover expired leases',async()=>{
 const f=fixture();try{
  await enqueue(f.db,f.actor(),config);await enqueue(f.db,f.actor('b'),config);
  let release:()=>void=()=>{},entered:()=>void=()=>{};const started=new Promise<void>(resolve=>{entered=resolve}),hold=new Promise<void>(resolve=>{release=resolve});
  const pending=workOne(f.db,{...config,concurrency:2},async s=>{entered();await hold;return parseResult(responseFor(s),s,config.hosts)});await started;
  assert.equal(await workOne(f.db,{...config,concurrency:2},async()=>{throw Error('duplicate call')}),false);release();await pending;
  await workOne(f.db,config,async()=>{throw Error('recent shared collection should be reused')});
  assert.equal(f.raw.sqlite.prepare('SELECT count(*) n FROM safety_versions').get()!.n,1);
 }finally{f.raw.close()}
 const recovery=fixture();try{
  await enqueue(recovery.db,recovery.actor(),config);recovery.raw.sqlite.exec("UPDATE safety_job_items SET status='running',attempts=2,lease_until=1; UPDATE safety_jobs SET status='running';");
  await workOne(recovery.db,config,async()=>{throw Error('exhausted attempt must not call provider')});assert.equal(recovery.raw.sqlite.prepare('SELECT status,error FROM safety_jobs').get()!.status,'failed');
 }finally{recovery.raw.close()}
});
