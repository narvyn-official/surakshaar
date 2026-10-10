import {CollectionError,errorCode,excerpts,fetchPublicPage,type Collected} from './provider';
import {publicPreflight} from './network';
import {actorStillAuthorised,enqueue} from './store';
import type {SafetyConfig,Source} from './model';
type Item={job_id:string;source_id:string;source_revision:number;attempts:number;lease_token:string};
export async function reserveBudget(db:D1Database,config:SafetyConfig,now=Date.now()){
 const result=await db.prepare(`INSERT INTO safety_budget(day,calls,cost_micros) SELECT ?,1,? WHERE ?>0 AND ?<=?
 ON CONFLICT(day) DO UPDATE SET calls=safety_budget.calls+1,cost_micros=safety_budget.cost_micros+excluded.cost_micros
 WHERE safety_budget.calls<? AND safety_budget.cost_micros+excluded.cost_micros<=?`).bind(new Date(now).toISOString().slice(0,10),config.callCostMicros,config.dailyCalls,config.callCostMicros,config.dailyCostMicros,config.dailyCalls,config.dailyCostMicros).run();
 return result.meta.changes===1;
}
async function finishJobs(db:D1Database){
 await db.prepare(`UPDATE safety_jobs SET finished_at=?,status=CASE WHEN NOT EXISTS(SELECT 1 FROM safety_job_items i WHERE i.job_id=safety_jobs.id AND i.status='failed') THEN 'completed' WHEN EXISTS(SELECT 1 FROM safety_job_items i WHERE i.job_id=safety_jobs.id AND i.status IN ('succeeded','unchanged')) THEN 'partial' ELSE 'failed' END,
 error=(SELECT error FROM safety_job_items i WHERE i.job_id=safety_jobs.id AND i.status='failed' ORDER BY i.source_id LIMIT 1)
 WHERE status IN ('queued','running') AND NOT EXISTS(SELECT 1 FROM safety_job_items i WHERE i.job_id=safety_jobs.id AND i.status IN ('queued','running'))`).bind(Date.now()).run();
}
/** One bounded attempt. Queue and leases survive server/worker restarts; no fire-and-forget request work. */
export async function workOne(db:D1Database,config:SafetyConfig,collect:(source:Source)=>Promise<Collected>=s=>fetchPublicPage(s,config,url=>publicPreflight(url,config.hosts))){
 const now=Date.now(),token=crypto.randomUUID(),until=now+180000;
 const slot=await db.prepare('UPDATE safety_worker_slots SET token=?,expires_at=?,heartbeat_at=? WHERE slot=(SELECT slot FROM safety_worker_slots WHERE slot<=? AND expires_at<? ORDER BY slot LIMIT 1) RETURNING slot').bind(token,until,now,config.concurrency,now).first<{slot:number}>();
 if(!slot)return false;
 try{
  await db.prepare("UPDATE safety_job_items SET status='failed',error='interrupted',lease_token=NULL WHERE status='running' AND lease_until<? AND attempts>=?").bind(now,config.maxAttempts).run();
  await finishJobs(db);
  const item=await db.prepare(`UPDATE safety_job_items SET status='running',attempts=attempts+1,lease_token=?,lease_until=?
   WHERE (job_id,source_id)=(SELECT i.job_id,i.source_id FROM safety_job_items i JOIN safety_jobs j ON j.id=i.job_id WHERE j.status IN ('queued','running') AND ((i.status='queued' AND i.next_attempt_at<=?) OR (i.status='running' AND i.lease_until<?)) AND i.attempts<? AND NOT EXISTS(SELECT 1 FROM safety_job_items other WHERE other.source_id=i.source_id AND other.status='running' AND other.lease_until>? AND other.lease_token<>?) ORDER BY j.created_at,i.source_id LIMIT 1) RETURNING *`).bind(token,until,now,now,config.maxAttempts,now,token).first<Item>();
  if(!item)return false;
  await db.prepare("UPDATE safety_jobs SET status='running' WHERE id=? AND status='queued'").bind(item.job_id).run();
  const job=await db.prepare('SELECT owner,requested_by FROM safety_jobs WHERE id=?').bind(item.job_id).first<{owner:string;requested_by:string}>();
  const source=await db.prepare('SELECT * FROM safety_sources WHERE id=?').bind(item.source_id).first<Source>();
  let attempted=false;
  try{
   if(!job||!await actorStillAuthorised(db,job.owner,job.requested_by))throw new CollectionError('access-revoked');
   if(!source||source.status!=='approved'||source.revision!==item.source_revision)throw new CollectionError('source-changed');
   const cached=source.last_success_at&&Date.now()-source.last_success_at<300000?await db.prepare("SELECT id FROM safety_versions WHERE source_id=? AND source_revision=? AND origin='tinyfish-fetch' LIMIT 1").bind(source.id,source.revision).first():null;
   if(cached){await db.prepare("UPDATE safety_job_items SET status='unchanged',error=NULL,lease_token=NULL,lease_until=NULL WHERE job_id=? AND source_id=? AND lease_token=?").bind(item.job_id,item.source_id,token).run();await finishJobs(db);return true;}
   if(!config.key)throw new CollectionError('missing-key');
   if(!await reserveBudget(db,config))throw new CollectionError('budget');
   attempted=true;const result=await collect(source);
   if(!await actorStillAuthorised(db,job.owner,job.requested_by))throw new CollectionError('access-revoked');
   const latest=await db.prepare('SELECT content,content_hash FROM safety_versions WHERE source_id=? AND source_revision=? AND origin=? ORDER BY rowid DESC LIMIT 1').bind(source.id,source.revision,'tinyfish-fetch').first<{content:string;content_hash:string}>();
   const {excerpt,previousExcerpt}=excerpts(result.content,latest?.content);
   const guard=`EXISTS(SELECT 1 FROM safety_job_items i JOIN safety_jobs j ON j.id=i.job_id JOIN safety_sources s ON s.id=i.source_id WHERE i.job_id=? AND i.source_id=? AND i.lease_token=? AND i.status='running' AND j.status='running' AND s.status='approved' AND s.revision=i.source_revision)`;
   const args=[item.job_id,source.id,token];
   const changes=await db.batch([
    db.prepare(`INSERT OR IGNORE INTO safety_versions(id,source_id,source_revision,title,publisher,source_url,published_date,retrieved_at,content_hash,content,excerpt,previous_excerpt,topic,jurisdiction,change_kind,origin) SELECT ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'tinyfish-fetch' WHERE ?=1 AND ${guard}`).bind(crypto.randomUUID(),source.id,source.revision,result.title,source.publisher,result.finalUrl,result.publishedDate,result.retrievedAt,result.hash,result.content,excerpt,previousExcerpt,source.topic,source.jurisdiction,latest?'changed':'baseline',latest?.content_hash===result.hash?0:1,...args),
    db.prepare(`UPDATE safety_sources SET last_checked_at=?,last_success_at=?,last_error=NULL,latest_hash=? WHERE id=? AND ${guard}`).bind(Date.now(),result.retrievedAt,result.hash,source.id,...args),
    db.prepare(`UPDATE safety_job_items SET status=?,error=NULL,lease_token=NULL,lease_until=NULL WHERE job_id=? AND source_id=? AND lease_token=? AND ${guard}`).bind(latest?.content_hash===result.hash?'unchanged':'succeeded',...args,...args)
   ]);
   if(!changes[2].meta.changes)throw new CollectionError('source-changed');
  }catch(error){
   const code=errorCode(error),retry=error instanceof CollectionError&&error.retryable&&item.attempts<config.maxAttempts;
   const delay=retry?Math.max(5000*2**(item.attempts-1),error instanceof CollectionError?error.retryAfterMs:0):0;
   await db.batch([
    db.prepare("UPDATE safety_sources SET last_checked_at=?,last_error=? WHERE id=? AND revision=? AND ?=1 AND EXISTS(SELECT 1 FROM safety_job_items WHERE job_id=? AND source_id=? AND lease_token=?)").bind(Date.now(),code,item.source_id,item.source_revision,attempted&&!['access-revoked','source-changed'].includes(code)?1:0,item.job_id,item.source_id,token),
    db.prepare('UPDATE safety_job_items SET status=?,error=?,next_attempt_at=?,lease_token=NULL,lease_until=NULL WHERE job_id=? AND source_id=? AND lease_token=?').bind(retry?'queued':'failed',code,Date.now()+delay,item.job_id,item.source_id,token)
   ]);
  }
  await finishJobs(db);return true;
 }finally{await db.prepare('UPDATE safety_worker_slots SET token=NULL,expires_at=0,heartbeat_at=? WHERE slot=? AND token=?').bind(Date.now(),slot.slot,token).run()}
}
/** Two independent opt-ins are required: deployment flag AND a workspace row enabled by an operator.
 * The pilot UI can only configure an interval and leaves the row disabled.
 */
export async function scheduleDue(db:D1Database,config:SafetyConfig){
 if(!config.scheduleEnabled||!config.key)return;
 const rows=await db.prepare('SELECT owner,interval_hours FROM safety_schedules WHERE enabled=1 AND (next_run_at IS NULL OR next_run_at<=?) ORDER BY next_run_at LIMIT 10').bind(Date.now()).all<{owner:string;interval_hours:number}>();
 for(const r of rows.results){
  if(!await actorStillAuthorised(db,r.owner,r.owner))continue;
  const claimed=await db.prepare('UPDATE safety_schedules SET next_run_at=? WHERE owner=? AND enabled=1 AND (next_run_at IS NULL OR next_run_at<=?)').bind(Date.now()+r.interval_hours*3600000,r.owner,Date.now()).run();
  if(claimed.meta.changes){try{await enqueue(db,{owner:r.owner,role:'admin',user:{userId:r.owner,email:'scheduled-collection'}},config)}catch{/* Keep bounded scheduling; no secrets/provider payloads in logs. */}}
 }
}
