/** Explicit operator diagnostic: two public requests, shared limits, no source decisions or learning writes. */
import {isAbsolute} from 'node:path';
import {SqliteD1} from '../lib/runtime/sqlite-d1';
import {configFrom,type Source} from '../lib/safety/model';
import {CollectionError,errorCode,fetchPublicPage} from '../lib/safety/provider';
import {publicPreflight} from '../lib/safety/network';
import {searchEquipment} from '../lib/safety/search';
import {reserveBudget} from '../lib/safety/worker';
if(!process.argv.includes('--live'))throw Error('Pass --live to authorize two public provider diagnostic requests. No source approval or curriculum change is made.');
if(!isAbsolute(process.env.SURAKSHA_DB_PATH??''))throw Error('Set the private database path.');
const raw=new SqliteD1(process.env.SURAKSHA_DB_PATH!),db=raw as unknown as D1Database,config=configFrom(process.env),token=crypto.randomUUID(),now=Date.now();
let slot:{slot:number}|null=null;
try{
 if(!config.key)throw new CollectionError('missing-key');
 slot=await db.prepare('UPDATE safety_worker_slots SET token=?,expires_at=? WHERE slot=(SELECT slot FROM safety_worker_slots WHERE slot<=? AND expires_at<? ORDER BY slot LIMIT 1) RETURNING slot').bind(token,now+180000,config.concurrency,now).first<{slot:number}>();
 if(!slot)throw new Error('Provider busy');
 const source=await db.prepare("SELECT * FROM safety_sources WHERE id='usfa-extinguishers'").first<Source>();if(!source)throw new Error('Diagnostic source unavailable');
 if(!await reserveBudget(db,config))throw new CollectionError('budget');
 const page=await fetchPublicPage(source,config,url=>publicPreflight(url,config.hosts));
 console.log(JSON.stringify({check:'tinyfish-fetch',status:'passed',characters:page.content.length,sha256:page.hash,sourceDecisionChanged:false}));
 if(!await reserveBudget(db,config))throw new CollectionError('budget');
 const results=await searchEquipment({action:'search',host:'www.zoll.com',model:'ZOLL AED Plus',publicOnly:true},config);
 console.log(JSON.stringify({check:'tinyfish-search',status:'passed',validatedResults:results.length,sourceDecisionChanged:false}));
}catch(error){console.error(JSON.stringify({check:'tinyfish-provider',status:'failed',code:errorCode(error)}));process.exitCode=1}
finally{if(slot)await db.prepare('UPDATE safety_worker_slots SET token=NULL,expires_at=0 WHERE slot=? AND token=?').bind(slot.slot,token).run();raw.close()}
