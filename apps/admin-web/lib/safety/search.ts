import {createHash} from 'node:crypto';
import {CollectionError,cleanText,errorCode,readProviderJson} from './provider';
import {sourceUrl} from './url-policy';
import {reserveBudget} from './worker';
import {requireSafetyRole,type SafetyConfig} from './model';
import {safetyAudit,type SafetyActor} from './store';
import type {EquipmentQuery,EquipmentResult,EquipmentSearch} from './search-model';

export function parseSearch(body:unknown,host:string):EquipmentResult[]{
 if(!body||typeof body!=='object'||!('results' in body)||!Array.isArray(body.results)||body.results.length>100)throw new CollectionError('malformed');
 const results:EquipmentResult[]=[],seen=new Set<string>();
 for(const item of body.results){
  if(!item||typeof item!=='object'||typeof item.url!=='string'||item.url.length>2000||typeof item.title!=='string'||item.title.length>2000||typeof item.snippet!=='string'||item.snippet.length>20000)throw new CollectionError('malformed');
  let url:string;try{url=sourceUrl(item.url,[host])}catch{continue}
  if(seen.has(url))continue;seen.add(url);
  results.push({url,host,title:cleanText(item.title).slice(0,200)||host,snippet:cleanText(item.snippet).slice(0,800)});
 }
 return results.slice(0,10);
}
export async function searchEquipment(query:EquipmentQuery,config:SafetyConfig,transport:typeof fetch=fetch){
 if(!config.key)throw new CollectionError('missing-key');
 if(!config.equipmentHosts.includes(query.host))throw new CollectionError('unsafe-url');
 const url=new URL('https://api.search.tinyfish.ai');
 url.search=new URLSearchParams({query:query.model+' official instruction manual safety specifications',include_domains:query.host,location:'IN',language:'en',domain_type:'web',page:'0'}).toString();
 try{
  const response=await transport(url,{method:'GET',redirect:'error',headers:{'X-API-Key':config.key,Accept:'application/json'},signal:AbortSignal.timeout(Math.min(config.timeoutMs,35000))});
  if(response.status===401||response.status===403)throw new CollectionError('provider-auth');
  if(response.status===402)throw new CollectionError('provider-quota');
  if(response.status===429)throw new CollectionError('rate-limit');
  if(!response.ok)throw new CollectionError('provider-unavailable');
  return parseSearch(await readProviderJson(response),query.host);
 }catch(error){if(error instanceof CollectionError)throw error;if(error instanceof Error&&['TimeoutError','AbortError'].includes(error.name))throw new CollectionError('timeout');throw new CollectionError('provider-unavailable')}
}
type SearchRow=Omit<EquipmentSearch,'results'>&{results:string};
export async function searchHistory(db:D1Database,owner:string):Promise<EquipmentSearch[]>{
 const rows=await db.prepare('SELECT id,model,host,created_at,status,results,error FROM safety_searches WHERE owner=? ORDER BY created_at DESC,id DESC LIMIT 10').bind(owner).all<SearchRow>();
 return rows.results.map(row=>({...row,results:JSON.parse(row.results),...(row.status==='pending'&&Date.now()-row.created_at>180000?{status:'failed' as const,error:'interrupted'}:{})}));
}
/** Shares the same atomic request budget and concurrency leases as Fetch. No automatic retries. */
export async function runEquipmentSearch(db:D1Database,a:SafetyActor,query:EquipmentQuery,config:SafetyConfig,authorised:()=>Promise<boolean>,collect=searchEquipment){
 requireSafetyRole(a.role,'collect');
 if(!config.equipmentHosts.includes(query.host))throw new Error('Invalid manufacturer host. Choose a configured host.');
 if(!config.key)throw new CollectionError('missing-key');
 const now=Date.now(),cacheKey=createHash('sha256').update(JSON.stringify([query.model.toLowerCase(),query.host])).digest('hex');
 const cached=await db.prepare("SELECT id FROM safety_searches WHERE owner=? AND cache_key=? AND status='completed' AND created_at>? ORDER BY created_at DESC LIMIT 1").bind(a.owner,cacheKey,now-3600000).first<{id:string}>();
 if(cached)return {id:cached.id,cached:true};
 const token=crypto.randomUUID();
 const slot=await db.prepare('UPDATE safety_worker_slots SET token=?,expires_at=? WHERE slot=(SELECT slot FROM safety_worker_slots WHERE slot<=? AND expires_at<? ORDER BY slot LIMIT 1) RETURNING slot').bind(token,now+180000,config.concurrency,now).first<{slot:number}>();
 if(!slot)throw new Error('No provider slot is available. A collection or search is running; retry shortly.');
 let id:string|null=null;
 try{
  if(!await authorised())throw new Error('Forbidden: workspace access changed.');
  const proposed=crypto.randomUUID();
  const inserted=await db.prepare("INSERT INTO safety_searches(id,owner,requested_by,cache_key,model,host,created_at,status) SELECT ?,?,?,?,?,?,?,'pending' WHERE (SELECT count(*) FROM safety_searches WHERE owner=? AND created_at>?)<4 AND NOT EXISTS(SELECT 1 FROM safety_searches WHERE owner=? AND cache_key=? AND status='pending' AND created_at>?)").bind(proposed,a.owner,a.user.userId,cacheKey,query.model,query.host,now,a.owner,now-3600000,a.owner,cacheKey,now-180000).run();
  if(!inserted.meta.changes)throw new Error('No more searches are available now. A matching search is running or the four-per-hour workspace limit was reached.');
  id=proposed;
  if(!await reserveBudget(db,config))throw new CollectionError('budget');
  const results=await collect(query,config);
  if(!await authorised())throw new CollectionError('access-revoked');
  await db.batch([
   db.prepare("UPDATE safety_searches SET status='completed',results=? WHERE id=? AND owner=?").bind(JSON.stringify(results),id,a.owner),
   safetyAudit(db,a,'equipment-search',id,{host:query.host,resultCount:results.length})
  ]);
  return {id,cached:false};
 }catch(error){if(id)await db.prepare("UPDATE safety_searches SET status='failed',error=? WHERE id=? AND owner=?").bind(errorCode(error),id,a.owner).run();throw error}
 finally{await db.prepare('UPDATE safety_worker_slots SET token=NULL,expires_at=0 WHERE slot=? AND token=?').bind(slot.slot,token).run()}
}
