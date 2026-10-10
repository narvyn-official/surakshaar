import {z} from 'zod';
import {db,digest,json} from '@/lib/server';
import {centreId} from '@/lib/learner-scope';
import {learnerAccess} from '@/lib/learner';
import {assignmentProgress,enrolmentCommand,requireMatchingPractice,sharedPractice,uuid,type LaunchAssignment,type Submission} from '@/lib/learning-centre';
import {assignmentView,launchAssignment,launchBudget,launchFailure,launchResponse} from '@/lib/learning-centre-store';
import {validateLearningPractice} from '@/lib/learning-practice';

const query=z.object({workerId:uuid,owner:centreId.optional(),assignmentId:uuid.optional(),beforeAt:z.coerce.number().int().nonnegative().safe().optional(),beforeId:z.union([uuid,z.string().regex(/^[a-f0-9]{64}$/)]).optional()}).strict().refine(q=>(q.beforeAt===undefined)===(q.beforeId===undefined));
export async function GET(request:Request){try{
 const parsed=query.safeParse(Object.fromEntries(new URL(request.url).searchParams));if(!parsed.success)throw new Error('Invalid learner assignment filter.');const q=parsed.data,a=await learnerAccess(q.workerId,q.owner);
 if(q.assignmentId){
  const assignment=await launchAssignment(a.owner,q.assignmentId);if(assignment.worker_id!==q.workerId)throw new Error('Forbidden assignment.');
  const view=await assignmentView(assignment,a.user.userId,q.beforeAt!==undefined?{beforeAt:q.beforeAt,beforeId:q.beforeId!}:undefined);
  const candidates=await db().prepare("SELECT id,digest,payload FROM learning_practice_heads WHERE account_id=? AND worker_id=? AND course_id=? AND content_version=? AND json_extract(payload,'$.createdAt')>=? AND json_extract(payload,'$.finished')=1 AND NOT EXISTS(SELECT 1 FROM learning_course_submissions s WHERE s.owner=? AND s.assignment_id=? AND s.attempt_id=learning_practice_heads.id AND s.source_digest=learning_practice_heads.digest) ORDER BY updated_at DESC,id DESC LIMIT 200").bind(a.user.userId,q.workerId,assignment.course_id,assignment.content_version,assignment.created_at,a.owner,assignment.id).all<{id:string;digest:string;payload:string}>();
  const available=candidates.results.flatMap(row=>{try{const record=validateLearningPractice(JSON.parse(row.payload),q.workerId);requireMatchingPractice(assignment,record);return [{id:row.id,digest:row.digest,scenarioId:record.scenarioId,updatedAt:record.updatedAt,guided:record.guided}]}catch{return []}});
  return launchResponse({assignment:view,available,centre:a.centre,owner:a.owner,scope:'Formative practice only; original decisions and self-reported exploration, without private reflections.',certifiable:false});
 }
 const loaded=await db().prepare('SELECT * FROM learning_course_assignments WHERE owner=? AND worker_id=?'+(q.beforeAt!==undefined?' AND (created_at<? OR (created_at=? AND id<?))':'')+' ORDER BY created_at DESC,id DESC LIMIT 51').bind(a.owner,q.workerId,...(q.beforeAt!==undefined?[q.beforeAt,q.beforeAt,q.beforeId!]:[])).all<LaunchAssignment>();
 const rows=loaded.results.slice(0,50),ids=rows.map(row=>row.id),shared=ids.length?await db().prepare('SELECT DISTINCT assignment_id,scenario_id,withdrawn_at FROM learning_course_submissions WHERE owner=? AND account_id=? AND assignment_id IN ('+ids.map(()=>'?').join(',')+')').bind(a.owner,a.user.userId,...ids).all<Submission>():{results:[] as Submission[]};
 const assignments=rows.map(row=>({...row,scenarioIds:JSON.parse(row.scenario_ids),catalogue:JSON.parse(row.catalogue_snapshot),...assignmentProgress(row,shared.results)})),last=rows.at(-1);
 return launchResponse({centre:a.centre,owner:a.owner,assignments,nextCursor:loaded.results.length>50&&last?{beforeAt:last.created_at,beforeId:last.id}:null,certifiable:false});
}catch(error){return launchFailure(error)}}
export async function POST(request:Request){try{
 const parsed=enrolmentCommand.safeParse(await json(request,64000));if(!parsed.success)throw new Error('Invalid learner sharing command.');const c=parsed.data,a=await learnerAccess(c.workerId,c.owner),now=Date.now();await launchBudget(a.user.userId);
 if(c.action==='submit'){
  if(new Set(c.attemptIds).size!==c.attemptIds.length)throw new Error('Invalid duplicate practice selection.');
  const assignment=await launchAssignment(a.owner,c.assignmentId);if(assignment.worker_id!==c.workerId)throw new Error('Forbidden assignment.');
  const statements:D1PreparedStatement[]=[],sharedIds:string[]=[];
  for(const attemptId of c.attemptIds){
   const source=await db().prepare('SELECT payload,digest FROM learning_practice_heads WHERE account_id=? AND worker_id=? AND id=?').bind(a.user.userId,c.workerId,attemptId).first<{payload:string;digest:string}>();if(!source)throw new Error('No uploaded private practice found. Sync it first.');
   const record=validateLearningPractice(JSON.parse(source.payload),c.workerId);requireMatchingPractice(assignment,record);
   const id=await digest({owner:a.owner,assignment:c.assignmentId,attemptId,sourceDigest:source.digest});sharedIds.push(id);
   const previous=await db().prepare('SELECT withdrawn_at FROM learning_course_submissions WHERE owner=? AND id=?').bind(a.owner,id).first<{withdrawn_at:number|null}>();if(previous&&previous.withdrawn_at!==null)throw new Error('Record conflict: this snapshot was withdrawn. Share a new practice attempt if needed.');
   statements.push(db().prepare('INSERT INTO learning_course_submissions VALUES(?,?,?,?,?,?,?,?,?,?,NULL) ON CONFLICT(owner,id) DO NOTHING').bind(a.owner,id,c.assignmentId,c.workerId,a.user.userId,attemptId,source.digest,record.scenarioId,JSON.stringify(sharedPractice(record)),now));
  }
  await db().batch(statements);return launchResponse({shared:true,ids:sharedIds,reflectionIncluded:false,certifiable:false});
 }
 const submission=await db().prepare('SELECT * FROM learning_course_submissions WHERE owner=? AND id=? AND worker_id=? AND account_id=?').bind(a.owner,c.submissionId,c.workerId,a.user.userId).first<Submission>();if(!submission)throw new Error('No shared practice found for this account.');
 if(c.action==='withdraw'){
  await db().prepare('UPDATE learning_course_submissions SET withdrawn_at=? WHERE owner=? AND id=? AND account_id=? AND withdrawn_at IS NULL').bind(now,a.owner,c.submissionId,a.user.userId).run();return launchResponse({withdrawn:true,scope:'Future centre access is removed. Previously exported copies cannot be recalled.'});
 }
 if(submission.withdrawn_at!==null)throw new Error('Record conflict: this evidence has been withdrawn.');
 const previous=await db().prepare('SELECT * FROM learning_review_events WHERE owner=? AND id=?').bind(a.owner,c.id).first();
 if(previous){if(previous.actor_id!==a.user.userId||previous.submission_id!==c.submissionId||previous.note!==c.note||previous.outcome!=='correction-request')throw new Error('Record conflict: correction ID reused.');return launchResponse({saved:true,id:c.id,alreadySaved:true});}
 await db().prepare('INSERT INTO learning_review_events SELECT ?,?,?,?,?,?,?,MAX(?,COALESCE(MAX(created_at)+1,0)) FROM learning_review_events WHERE owner=? AND submission_id=?').bind(a.owner,c.id,c.submissionId,a.user.userId,c.previousEventId,'correction-request',c.note,now,a.owner,c.submissionId).run();return launchResponse({saved:true,id:c.id,originalEvidenceUnchanged:true});
}catch(error){return launchFailure(error)}}
