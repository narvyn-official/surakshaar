import {createHash} from 'node:crypto';
import {sourceUrl} from './url-policy';
import type {SafetyConfig,Source} from './model';
export class CollectionError extends Error {
 constructor(readonly code:string,readonly retryable=false,readonly retryAfterMs=0){super(code);}
}
import {errorLabels} from './messages';
export function errorCode(error:unknown){return error instanceof CollectionError&&error.code in errorLabels?error.code:'provider-unavailable';}
export function cleanText(value:string){return value.normalize('NFC').replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f\u202a-\u202e\u2066-\u2069]/g,'').replace(/<\/?[a-zA-Z][^>]*>/g,'').trim();}
export function normalizeContent(value:string){
 return cleanText(value).split(/\r?\n/).map(line=>line.replace(/^\s{0,3}#{1,6}\s+/,'').replace(/\*\*|__/g,'').replace(/\s+/g,' ').trim()).filter(line=>line&&!/^(?:cookie (?:settings|preferences)|privacy policy|all rights reserved\.?|skip to (?:main )?content)$/i.test(line)).join('\n');
}
export type Collected={title:string;publishedDate:string|null;content:string;hash:string;finalUrl:string;retrievedAt:number};
export function parseResult(body:unknown,source:Source,hosts:string[]):Collected {
 if(!body||typeof body!=='object')throw new CollectionError('malformed');
 const b=body as {results?:unknown;errors?:unknown};
 if(!Array.isArray(b.results)||!Array.isArray(b.errors)||b.results.length+b.errors.length!==1)throw new CollectionError('malformed');
 if(b.errors.length){const e=b.errors[0];if(!e||typeof e!=='object'||e.url!==source.url||typeof e.error!=='string')throw new CollectionError('malformed');if(['invalid_url','invalid_redirect_url'].includes(e.error))throw new CollectionError('unsafe-url');if(e.error==='timeout')throw new CollectionError('timeout',true);throw new CollectionError('source-unavailable',false);}
 const r=b.results[0];if(!r||typeof r!=='object'||r.url!==source.url||typeof r.final_url!=='string'||typeof r.text!=='string'||r.text.length>250000||r.title!==null&&typeof r.title!=='string'||r.published_date!==null&&r.published_date!==undefined&&typeof r.published_date!=='string'||r.format!=='markdown')throw new CollectionError('malformed');
 // A different URL is a new source proposal, never an implicit expansion of the allowlist.
 try{if(sourceUrl(r.final_url,hosts)!==source.url)throw new Error('redirect');}catch{throw new CollectionError('unsafe-url');}
 const title=r.title?cleanText(r.title).slice(0,200):source.title;
 const content=normalizeContent(r.text);if(content.length<80)throw new CollectionError('malformed');
 const date=r.published_date;
 const publishedDate=typeof date==='string'&&/^\d{4}-\d{2}-\d{2}(?:T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2}))?$/.test(date)&&Number.isFinite(Date.parse(date))&&Number.isFinite(Date.parse(date.slice(0,10)+'T00:00:00Z'))&&new Date(date.slice(0,10)+'T00:00:00Z').toISOString().slice(0,10)===date.slice(0,10)?date:null;
 const hash=createHash('sha256').update(JSON.stringify({title,content,publishedDate})).digest('hex');
 return {title,content,publishedDate,hash,finalUrl:source.url,retrievedAt:Date.now()};
}
export async function fetchPublicPage(source:Source,config:SafetyConfig,preflight:(url:string)=>Promise<void>,transport:typeof fetch=fetch):Promise<Collected> {
 if(!config.key)throw new CollectionError('missing-key');
 try{sourceUrl(source.url,config.hosts)}catch{throw new CollectionError('unsafe-url')}
 await preflight(source.url);
 try{
  const response=await transport('https://api.fetch.tinyfish.ai',{method:'POST',redirect:'error',signal:AbortSignal.timeout(config.timeoutMs+5000),headers:{'Content-Type':'application/json','X-API-Key':config.key},body:JSON.stringify({urls:[source.url],format:'markdown',links:false,image_links:false,iframe_links:false,ttl:0,per_url_timeout_ms:config.timeoutMs})});
  if(response.status===401||response.status===403)throw new CollectionError('provider-auth');
  if(response.status===402)throw new CollectionError('provider-quota');
  if(response.status===429){const raw=response.headers.get('retry-after')??'';const wait=/^\d+$/.test(raw)?Number(raw)*1000:Date.parse(raw)-Date.now();throw new CollectionError('rate-limit',true,Math.max(5000,Math.min(86400000,Number.isFinite(wait)?wait:30000)));}
  if(response.status>=500)throw new CollectionError('provider-unavailable',true);
  if(!response.ok)throw new CollectionError('provider-unavailable');
  return parseResult(await readProviderJson(response),source,config.hosts);
 }catch(error){if(error instanceof CollectionError)throw error;if(error instanceof Error&&['TimeoutError','AbortError'].includes(error.name))throw new CollectionError('timeout',true);throw new CollectionError('provider-unavailable',true);}
}
export async function readProviderJson(response:Response):Promise<unknown> {
  if(!response.headers.get('content-type')?.includes('application/json')||Number(response.headers.get('content-length'))>1_000_000)throw new CollectionError('malformed');
  const reader=response.body?.getReader();if(!reader)throw new CollectionError('malformed');let bytes=0,text='';const decoder=new TextDecoder('utf-8',{fatal:true});
  try{for(;;){const {done,value}=await reader.read();if(done)break;bytes+=value.byteLength;if(bytes>1_000_000)throw new CollectionError('malformed');text+=decoder.decode(value,{stream:true})}text+=decoder.decode()}finally{await reader.cancel().catch(()=>{});reader.releaseLock()}
  let result:unknown;try{result=JSON.parse(text)}catch{throw new CollectionError('malformed')}

 return result;
}
export function excerpts(content:string,previous?:string){
 if(!previous)return {excerpt:content.slice(0,1400),previousExcerpt:null};
 const nowLines=content.split('\n'),oldLines=previous.split('\n');const oldSet=new Set(oldLines),nowSet=new Set(nowLines);
 const added=nowLines.filter(l=>!oldSet.has(l)),removed=oldLines.filter(l=>!nowSet.has(l));
 return {excerpt:(added.join('\n')||content).slice(0,1400),previousExcerpt:(removed.join('\n')||previous).slice(0,1400)};
}
