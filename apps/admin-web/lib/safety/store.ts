import type {Job,PanelData,SafetyConfig,Source,Update,ReviewStatus,CollectionOrigin} from './model';
import {requireSafetyRole} from './model';
export type SafetyActor={owner:string;role:string;user:{userId:string;email:string}};
export function safetyAudit(db:D1Database,a:SafetyActor,action:string,target:string,detail:unknown,conditional=false){
 return db.prepare('INSERT INTO audit_log(id,owner,actor,actor_email,action,target,detail,at) SELECT ?,?,?,?,?,?,?,?'+(conditional?' WHERE changes()>0':''))
 .bind(crypto.randomUUID(),a.owner,a.user.userId,a.user.email,'safety.'+action,target,JSON.stringify(detail),Date.now());
}
export async function actorStillAuthorised(db:D1Database,owner:string,actor:string){
 const centre=await db.prepare("SELECT 1 AS ok FROM centre_approvals WHERE owner=? AND status='approved'").bind(owner).first();if(!centre)return false;
 if(owner===actor)return !!await db.prepare('SELECT id FROM auth_user WHERE id=?').bind(actor).first();
 return !!await db.prepare("SELECT 1 AS ok FROM team_members WHERE owner=? AND user_id=? AND active=1 AND role IN ('admin','trainer')").bind(owner,actor).first();
}
export async function enqueue(db:D1Database,a:SafetyActor,config:SafetyConfig){
 requireSafetyRole(a.role,'collect');
 if(!config.key)throw new Error('No TinyFish credential is configured. Ask the server administrator to set TINYFISH_API_KEY.');
 if(config.dailyCalls===0||config.callCostMicros>config.dailyCostMicros)throw new Error('No collection budget is available. Ask the server administrator to check the limits.');
 const existing=await db.prepare("SELECT id FROM safety_jobs WHERE owner=? AND status IN ('queued','running')").bind(a.owner).first<{id:string}>();if(existing)return {id:existing.id,duplicate:true};
 const sources=await db.prepare("SELECT id FROM safety_sources WHERE status='approved'").all();if(!sources.results.length)throw new Error('No public sources are approved yet. A platform administrator must approve a proposed source first.');
 const id=crypto.randomUUID(),now=Date.now();
 const result=await db.batch([
  db.prepare(`INSERT OR IGNORE INTO safety_jobs(id,owner,requested_by,created_at,status) SELECT ?,?,?,?,'queued' WHERE (SELECT count(*) FROM safety_jobs WHERE owner=? AND created_at>?)<4 AND NOT EXISTS(SELECT 1 FROM safety_jobs WHERE owner=? AND status IN ('queued','running'))`).bind(id,a.owner,a.user.userId,now,a.owner,now-3600000,a.owner),
  db.prepare("INSERT INTO safety_job_items(job_id,source_id,source_revision) SELECT ?,id,revision FROM safety_sources WHERE status='approved' AND EXISTS(SELECT 1 FROM safety_jobs WHERE id=?)").bind(id,id),
  db.prepare("INSERT INTO audit_log(id,owner,actor,actor_email,action,target,detail,at) SELECT ?,?,?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM safety_jobs WHERE id=?)").bind(crypto.randomUUID(),a.owner,a.user.userId,a.user.email,'safety.collect',id,'{"scope":"staff-reference-only"}',now,id)
 ]);
 if(result[0].meta.changes)return {id,duplicate:false};
 const raced=await db.prepare("SELECT id FROM safety_jobs WHERE owner=? AND status IN ('queued','running')").bind(a.owner).first<{id:string}>();if(raced)return {id:raced.id,duplicate:true};
 throw new Error('No more than four checks per workspace are allowed each hour. Try later.');
}
export async function reviewUpdate(db:D1Database,a:SafetyActor,id:string,status:ReviewStatus,notes:string,revision:number){
 requireSafetyRole(a.role,'review',status);
 const previous=await db.prepare('SELECT status FROM safety_reviews WHERE owner=? AND version_id=?').bind(a.owner,id).first<{status:ReviewStatus}>();
 if(a.role!=='admin'&&previous&&['approved','rejected'].includes(previous.status))throw new Error('Forbidden: only administrators can reopen a final decision.');
 const result=await db.batch([
  db.prepare(`INSERT INTO safety_reviews(owner,version_id,status,notes,revision,reviewed_by,reviewed_at)
   SELECT ?,v.id,?,?,1,?,? FROM safety_versions v JOIN safety_sources s ON s.id=v.source_id
   WHERE v.id=? AND (?<>'approved' OR (s.status='approved'))
    AND (?=0 OR EXISTS(SELECT 1 FROM safety_reviews WHERE owner=? AND version_id=v.id))
   ON CONFLICT(owner,version_id) DO UPDATE SET status=excluded.status,notes=excluded.notes,revision=safety_reviews.revision+1,reviewed_by=excluded.reviewed_by,reviewed_at=excluded.reviewed_at
   WHERE safety_reviews.revision=? AND (?='admin' OR safety_reviews.status NOT IN ('approved','rejected'))`).bind(a.owner,status,notes,a.user.userId,Date.now(),id,status,revision,a.owner,revision,a.role),
  safetyAudit(db,a,'review',id,{status,notes,revision:revision+1,scope:'staff-reference-only'},true)
 ]);
 if(!result[0].meta.changes)throw new Error('Record conflict: the decision or source approval changed. Refresh before reviewing again.');
}
export async function panelData(db:D1Database,a:SafetyActor,config:SafetyConfig,operator:boolean,filter:string,cursor:string|null,origin:CollectionOrigin='all'):Promise<PanelData>{
 requireSafetyRole(a.role,'read');
 let beforeAt=Number.MAX_SAFE_INTEGER,beforeId='';
 if(cursor){try{const p=JSON.parse(atob(cursor));if(!Number.isSafeInteger(p.at)||typeof p.id!=='string'||p.id.length>100)throw new Error();beforeAt=p.at;beforeId=p.id}catch{throw new Error('Invalid Safety Updates page cursor.')}}
 const where=filter==='all'?'':"AND COALESCE(r.status,'new')=?";
 const values=filter==='all'?[]:[filter];
 const originWhere=origin==='all'?'':'AND v.origin=?';
 const originValues=origin==='all'?[]:[origin];
 const [sources,updates,jobs,counts,audit,budget,worker,schedule,collection]=await Promise.all([
  db.prepare('SELECT * FROM safety_sources ORDER BY topic,title LIMIT 100').all<Source>(),
  db.prepare(`SELECT v.id,v.source_id,v.title,v.publisher,v.source_url,v.published_date,v.retrieved_at,v.content_hash,v.excerpt,v.previous_excerpt,v.topic,v.jurisdiction,v.change_kind,v.origin,v.source_revision,s.revision AS current_revision,s.status AS source_status,COALESCE(r.status,'new') AS status,COALESCE(r.notes,'') AS notes,COALESCE(r.revision,0) AS review_revision,r.reviewed_at FROM safety_versions v JOIN safety_sources s ON s.id=v.source_id LEFT JOIN safety_reviews r ON r.version_id=v.id AND r.owner=? WHERE (v.retrieved_at<? OR (v.retrieved_at=? AND v.id<?)) ${where} ${originWhere} ORDER BY v.retrieved_at DESC,v.id DESC LIMIT 21`).bind(a.owner,beforeAt,beforeAt,beforeId,...values,...originValues).all<Update>(),
  db.prepare(`SELECT j.id,j.status,j.created_at,j.finished_at,j.error,count(i.source_id) AS total,sum(CASE WHEN i.status IN ('succeeded','unchanged','failed') THEN 1 ELSE 0 END) AS done,sum(CASE WHEN i.status='failed' THEN 1 ELSE 0 END) AS failed FROM safety_jobs j LEFT JOIN safety_job_items i ON i.job_id=j.id WHERE j.owner=? GROUP BY j.id ORDER BY j.created_at DESC LIMIT 10`).bind(a.owner).all<Job>(),
  db.prepare(`SELECT COALESCE(r.status,'new') AS status,count(*) AS count FROM safety_versions v LEFT JOIN safety_reviews r ON r.version_id=v.id AND r.owner=? WHERE 1=1 ${originWhere} GROUP BY COALESCE(r.status,'new')`).bind(a.owner,...originValues).all<{status:ReviewStatus;count:number}>(),
  db.prepare("SELECT id,action,target,detail,at,actor_email FROM audit_log WHERE owner=? AND action LIKE 'safety.%' ORDER BY at DESC,id DESC LIMIT 30").bind(a.owner).all<PanelData['audit'][number]>(),
  db.prepare('SELECT calls FROM safety_budget WHERE day=?').bind(new Date().toISOString().slice(0,10)).first<{calls:number}>(),
  db.prepare('SELECT max(heartbeat_at) AS heartbeat FROM safety_worker_slots').first<{heartbeat:number|null}>(),
  db.prepare('SELECT interval_hours,enabled FROM safety_schedules WHERE owner=?').bind(a.owner).first<{interval_hours:number;enabled:number}>(),
  db.prepare("SELECT (SELECT max(last_success_at) FROM safety_sources) AS lastSuccessfulAt, (SELECT count(*) FROM safety_versions WHERE origin='tinyfish-fetch') AS liveCount, (SELECT count(*) FROM safety_versions WHERE origin='fixture') AS fixtureCount").first<PanelData['collection']>()
 ]);
 const rows=updates.results.slice(0,20),last=rows.at(-1);
 return {collection:collection??{lastSuccessfulAt:null,liveCount:0,fixtureCount:0},sources:sources.results,updates:rows,jobs:jobs.results,nextCursor:updates.results.length>20&&last?btoa(JSON.stringify({at:last.retrieved_at,id:last.id})):null,counts:{new:0,'needs-review':0,approved:0,rejected:0,...Object.fromEntries(counts.results.map(r=>[r.status,r.count]))},provider:{configured:!!config.key,workerOnline:!!worker?.heartbeat&&Date.now()-worker.heartbeat<120000,dailyCalls:budget?.calls??0,dailyLimit:config.dailyCalls,schedulingEnabled:config.scheduleEnabled&&!!schedule?.enabled,intervalHours:schedule?.interval_hours??168},permissions:{review:a.role==='admin',manageSources:operator},audit:audit.results};
}
