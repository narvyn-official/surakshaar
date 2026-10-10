import {z} from 'zod';
export const statuses=['new','needs-review','approved','rejected'] as const;
export const collectionOrigins=['all','tinyfish-fetch','fixture'] as const;
export type CollectionOrigin=typeof collectionOrigins[number];
export type ReviewStatus=typeof statuses[number];
export type Source={id:string;title:string;publisher:string;url:string;topic:'fire'|'first-aid';jurisdiction:string;rationale:string;status:'proposed'|'approved'|'disabled';revision:number;changed_at:number;decision_note:string;last_checked_at:number|null;last_success_at:number|null;last_error:string|null;latest_hash:string|null};
export type Update={id:string;source_id:string;title:string;publisher:string;source_url:string;published_date:string|null;retrieved_at:number;content_hash:string;excerpt:string;previous_excerpt:string|null;topic:string;jurisdiction:string;change_kind:string;origin:'tinyfish-fetch'|'fixture';status:ReviewStatus;notes:string;review_revision:number;reviewed_at:number|null;source_status:string;source_revision:number;current_revision:number};
export type Job={id:string;status:string;created_at:number;finished_at:number|null;error:string|null;total:number;done:number;failed:number};
export type SafetyConfig={key?:string;dailyCalls:number;dailyCostMicros:number;callCostMicros:number;concurrency:number;timeoutMs:number;maxAttempts:number;scheduleEnabled:boolean;hosts:string[];equipmentHosts:string[]};
export const defaultHosts=['sachet.ndma.gov.in','www.usfa.fema.gov','www.osha.gov','www.redcross.org','www.sja.org.uk','cpr.heart.org','www.nhs.uk'];
export function configFrom(env:Record<string,unknown>):SafetyConfig {
 const integer=(name:string,fallback:number,min:number,max:number)=>{const v=env[name];if(v===undefined||v==='')return fallback;const n=Number(v);if(!Number.isSafeInteger(n)||n<min||n>max)throw new Error('Invalid Safety Updates configuration: '+name);return n;};
 const equipmentHosts=typeof env.SAFETY_EQUIPMENT_HOSTS==='string'?env.SAFETY_EQUIPMENT_HOSTS.split(',').map(s=>s.trim().toLowerCase()).filter(Boolean):['www.kidde.com','www.zoll.com'];
 if(equipmentHosts.length>10||equipmentHosts.some(h=>!/^(?:[a-z0-9]+(?:-[a-z0-9]+)*\.)+[a-z]{2,}$/.test(h)))throw new Error('Invalid Safety Updates equipment hosts.');
 return {equipmentHosts,key:typeof env.TINYFISH_API_KEY==='string'?env.TINYFISH_API_KEY.trim()||undefined:undefined,dailyCalls:integer('SAFETY_DAILY_CALL_LIMIT',20,0,1000),dailyCostMicros:integer('SAFETY_DAILY_COST_MICROS',0,0,100_000_000),callCostMicros:integer('SAFETY_FETCH_COST_MICROS',0,0,10_000_000),concurrency:integer('SAFETY_CONCURRENCY',1,1,3),timeoutMs:integer('SAFETY_TIMEOUT_MS',35000,1000,90000),maxAttempts:integer('SAFETY_MAX_ATTEMPTS',2,1,3),scheduleEnabled:env.SAFETY_SCHEDULE_ENABLED==='true',hosts:typeof env.SAFETY_SOURCE_HOSTS==='string'?env.SAFETY_SOURCE_HOSTS.split(',').map(s=>s.trim().toLowerCase()).filter(Boolean):[...defaultHosts,...equipmentHosts]};
}
export const reviewCommand=z.object({action:z.literal('review'),id:z.string().min(1).max(100),status:z.enum(statuses),notes:z.string().trim().min(5).max(2000),revision:z.number().int().min(0)}).strict();
export const workspaceCommand=z.discriminatedUnion('action',[
 z.object({action:z.literal('collect')}).strict(),reviewCommand,
 z.object({action:z.literal('schedule'),intervalHours:z.number().int().min(24).max(720)}).strict(),
 z.object({action:z.literal('cancel'),id:z.string().min(1).max(100)}).strict()
]);
export const sourceCommand=z.discriminatedUnion('action',[
 z.object({action:z.literal('propose'),title:z.string().trim().min(3).max(200),publisher:z.string().trim().min(3).max(150),url:z.string().max(2000),topic:z.enum(['fire','first-aid']),jurisdiction:z.string().trim().min(2).max(120),rationale:z.string().trim().min(10).max(1000)}).strict(),
 z.object({action:z.literal('decide'),id:z.string().min(1).max(100),status:z.enum(['approved','disabled']),revision:z.number().int().min(1),note:z.string().trim().min(10).max(1000)}).strict()
]);
export function requireSafetyRole(role:string,action:'read'|'collect'|'review'|'schedule'|'cancel',status?:ReviewStatus){
 if(!['admin','trainer'].includes(role)||action==='schedule'&&role!=='admin'||action==='review'&&['approved','rejected'].includes(status??'')&&role!=='admin')throw new Error('Forbidden: Safety Updates requires an authorised trainer or administrator; final decisions require an administrator.');
}
export function requireSourceOperator(operator:boolean){if(!operator)throw new Error('Forbidden: public sources are managed by platform administrators.');}
export type PanelData={sources:Source[];updates:Update[];jobs:Job[];nextCursor:string|null;counts:Record<ReviewStatus,number>;collection:{lastSuccessfulAt:number|null;liveCount:number;fixtureCount:number};provider:{configured:boolean;workerOnline:boolean;dailyCalls:number;dailyLimit:number;schedulingEnabled:boolean;intervalHours:number};permissions:{review:boolean;manageSources:boolean};audit:{id:string;action:string;target:string;detail:string;at:number;actor_email:string}[]};
