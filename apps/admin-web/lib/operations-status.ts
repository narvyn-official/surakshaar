import {readFile} from 'node:fs/promises';
import {z} from 'zod';
import {env} from './runtime-env';
const report=z.object({schema:z.literal(1),checkedAt:z.number().int().positive(),status:z.enum(['pass','fail']),checks:z.array(z.object({id:z.string().max(60),label:z.string().max(100),status:z.enum(['pass','fail']),detail:z.string().max(300)}).strict()).max(10)}).strict();
export async function operationsStatus(){
 const path=env.SURAKSHA_OPS_STATUS_FILE;if(typeof path!=='string'||!path.startsWith('/'))return {state:'unconfigured' as const,report:null};
 try{const raw=await readFile(path);if(raw.byteLength>10000)throw Error('size');const value=report.parse(JSON.parse(raw.toString()));return {state:Date.now()-value.checkedAt>10*60000||value.checkedAt>Date.now()+60000?'stale' as const:'current' as const,report:value};}catch{return {state:'unavailable' as const,report:null};}
}
