import {unitScope} from '@/lib/staff-scope';
import {z} from 'zod';
import {access,db,digest,failure,json} from '@/lib/server';
import {POST as assignCourse} from '@/app/api/learning-centre/route';
const command=z.object({id:z.string().uuid(),cohortId:z.string().uuid(),courseId:z.enum(['fire','first-aid']),contentVersion:z.string().max(100),audience:z.enum(['school-awareness','adult-learning']),scenarioIds:z.array(z.string().max(100)).min(1).max(50),mode:z.enum(['guided','independent']),dueAt:z.number().int().positive(),note:z.string().trim().max(500)}).strict();
export async function POST(request:Request){try{
 const a=await access('write',true),parsed=command.safeParse(await json(request));if(!parsed.success)throw Error('Invalid cohort programme.');const {cohortId,...c}=parsed.data;
 const scope=unitScope(a,"u.id");
 if(!await db().prepare("SELECT 1 FROM institution_units u WHERE u.owner=? AND u.id=? AND u.kind='cohort' AND u.archived_at IS NULL AND "+scope.sql).bind(a.owner,cohortId,...scope.args).first())throw Error('No active cohort found.');
 const hash=await digest(parsed.data);
 let batch=await db().prepare('SELECT digest,worker_ids FROM institution_programme_batches WHERE owner=? AND id=?').bind(a.owner,c.id).first<{digest:string;worker_ids:string}>();
 if(!batch){
  const rows=await db().prepare('SELECT worker_id FROM institution_people WHERE owner=? AND unit_id=? AND archived_at IS NULL ORDER BY worker_id LIMIT 201').bind(a.owner,cohortId).all<{worker_id:string}>();
  if(!rows.results.length||rows.results.length>200)throw Error('Invalid cohort size: assign a cohort of 1–200 active people.');
  await db().prepare('INSERT INTO institution_programme_batches VALUES(?,?,?,?,?) ON CONFLICT(owner,id) DO NOTHING').bind(a.owner,c.id,hash,JSON.stringify(rows.results.map(r=>r.worker_id)),Date.now()).run();
  batch=await db().prepare('SELECT digest,worker_ids FROM institution_programme_batches WHERE owner=? AND id=?').bind(a.owner,c.id).first();
 }
 if(!batch||batch.digest!==hash)throw Error('Record conflict: this programme request ID was reused.');
 const headers=new Headers(request.headers);headers.delete('content-length');return assignCourse(new Request(request.url,{method:'POST',headers,body:JSON.stringify({action:'assign',...c,workerIds:JSON.parse(batch.worker_ids)})}));
}catch(e){return failure(e)}}
