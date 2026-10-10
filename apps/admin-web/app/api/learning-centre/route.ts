import {z} from 'zod';
import {access,audit,db,digest,json} from '@/lib/server';
import {centreCommand,validateAssignment,launchCatalogue,uuid,csvCell,assignmentProgress,type LaunchAssignment,type Submission} from '@/lib/learning-centre';
import {assignmentView,launchAssignment,launchBudget,launchFailure,launchResponse} from '@/lib/learning-centre-store';

const query=z.object({assignmentId:uuid.optional(),beforeAt:z.coerce.number().int().nonnegative().safe().optional(),beforeId:z.union([uuid,z.string().regex(/^[a-f0-9]{64}$/)]).optional(),format:z.enum(['json','csv']).default('json')}).strict().refine(q=>(q.beforeAt===undefined)===(q.beforeId===undefined));
export async function GET(request:Request){try{
 const a=await access(),parsed=query.safeParse(Object.fromEntries(new URL(request.url).searchParams));if(!parsed.success)throw new Error('Invalid assignment filter.');const q=parsed.data;
 if(q.assignmentId)return launchResponse({assignment:await assignmentView(await launchAssignment(a.owner,q.assignmentId),undefined,q.beforeAt!==undefined?{beforeAt:q.beforeAt,beforeId:q.beforeId!}:undefined),role:a.role,actorId:a.user.userId,certifiable:false});
 const result=await db().prepare('SELECT a.*,w.name AS learner_name FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=?'+(q.beforeAt!==undefined?' AND (a.created_at<? OR (a.created_at=? AND a.id<?))':'')+' ORDER BY a.created_at DESC,a.id DESC LIMIT 51').bind(a.owner,...(q.beforeAt!==undefined?[q.beforeAt,q.beforeAt,q.beforeId!]:[])).all<LaunchAssignment&{learner_name:string}>();
 const loaded=result.results.slice(0,50),ids=loaded.map(row=>row.id);
 const shared=ids.length?await db().prepare('SELECT DISTINCT s.assignment_id,s.scenario_id,s.withdrawn_at FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.assignment_id IN ('+ids.map(()=>'?').join(',')+')').bind(a.owner,...ids).all<Submission>():{results:[] as Submission[]};
 const assignments=loaded.map(assignment=>({...assignment,scenarioIds:JSON.parse(assignment.scenario_ids),catalogue:JSON.parse(assignment.catalogue_snapshot),...assignmentProgress(assignment,shared.results)}));
 const last=assignments.at(-1),nextCursor=result.results.length>50&&last?{beforeAt:last.created_at,beforeId:last.id}:null;
 if(q.format==='csv')return launchResponse({filename:'surakshaar-formative-assignments.csv',csv:[['Course','Content version','Learner','Audience','Practice mode','Status','Completed scenarios','Required scenarios','Due UTC','Evidence scope'],...assignments.map(v=>[v.course_id,v.content_version,v.learner_name,v.audience,v.mode,v.status,v.completed,v.required,new Date(v.due_at).toISOString(),'Formative practice only; no certification'])].map(row=>row.map(csvCell).join(',')).join('\r\n'),loaded:assignments.length,nextCursor,certifiable:false});
 const workers=await db().prepare('SELECT w.id,w.name,l.linked_at FROM workers w LEFT JOIN learner_links l ON l.owner=w.owner AND l.worker_id=w.id WHERE w.owner=? ORDER BY w.name,w.id LIMIT 1000').bind(a.owner).all();
 return launchResponse({catalogue:launchCatalogue,assignments,workers:workers.results,nextCursor,role:a.role,actorId:a.user.userId,owner:a.owner,certifiable:false});
}catch(error){return launchFailure(error)}}

export async function POST(request:Request){try{
 const a=await access('write'),parsed=centreCommand.safeParse(await json(request,64000));if(!parsed.success)throw new Error('Invalid educator command. Check the fields and limits.');const c=parsed.data,now=Date.now();await launchBudget(a.user.userId);
 if(c.action==='assign'){
  const snapshot=validateAssignment(c,now),statements:D1PreparedStatement[]=[],ids:string[]=[];
  for(const workerId of c.workerIds){
   if(!await db().prepare('SELECT id FROM workers WHERE owner=? AND id=?').bind(a.owner,workerId).first())throw new Error('No learner found in this centre.');
   const hash=await digest({owner:a.owner,batch:c.id,workerId}),id=`${hash.slice(0,8)}-${hash.slice(8,12)}-4${hash.slice(13,16)}-8${hash.slice(17,20)}-${hash.slice(20,32)}`;ids.push(id);
   const previous=await db().prepare('SELECT course_id,content_version,audience,scenario_ids,mode,due_at,note FROM learning_course_assignments WHERE owner=? AND id=?').bind(a.owner,id).first();
   if(previous&&JSON.stringify([previous.course_id,previous.content_version,previous.audience,previous.scenario_ids,previous.mode,previous.due_at,previous.note])!==JSON.stringify([c.courseId,c.contentVersion,c.audience,JSON.stringify(c.scenarioIds),c.mode,c.dueAt,c.note]))throw new Error('Record conflict: assignment request ID was reused with different details.');
   statements.push(db().prepare('INSERT INTO learning_course_assignments(owner,id,worker_id,course_id,content_version,audience,scenario_ids,catalogue_snapshot,mode,due_at,created_at,created_by,note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(owner,id) DO NOTHING').bind(a.owner,id,workerId,c.courseId,c.contentVersion,c.audience,JSON.stringify(c.scenarioIds),JSON.stringify(snapshot),c.mode,c.dueAt,now,a.user.userId,c.note));
   statements.push(audit(a,'launch.assignment',id,{workerId,courseId:c.courseId,contentVersion:c.contentVersion},true));
  }await db().batch(statements);return launchResponse({saved:true,assignmentIds:ids,certifiable:false});
 }
 if(c.action==='cancel'){
  await launchAssignment(a.owner,c.assignmentId);await db().batch([db().prepare('UPDATE learning_course_assignments SET cancelled_at=?,cancel_reason=? WHERE owner=? AND id=? AND cancelled_at IS NULL').bind(now,c.reason,a.owner,c.assignmentId),audit(a,'launch.cancel',c.assignmentId,{},true)]);return launchResponse({cancelled:true});
 }
 if(c.action==='observe'){
  const assignment=await launchAssignment(a.owner,c.assignmentId),link=await db().prepare('SELECT user_id FROM learner_links WHERE owner=? AND worker_id=?').bind(a.owner,assignment.worker_id).first<{user_id:string|null}>();
  if(!link?.user_id||link.user_id===a.user.userId)throw new Error('Forbidden: a linked learner and a separate educator are required for observation.');
  if(assignment.cancelled_at!==null)throw new Error('Record conflict: assignment is cancelled.');
  if(c.observedAt>now||c.observedAt<assignment.created_at)throw new Error('Invalid observation time. It must follow the assignment and not be in the future.');
  if(c.outcome==='observed'&&Object.values(c.rubric).some(v=>!v))throw new Error('Invalid observation outcome: mark further practice when a criterion is not met.');
  const old=await db().prepare('SELECT * FROM learning_course_observations WHERE owner=? AND id=?').bind(a.owner,c.id).first();if(old){if(old.assignment_id!==c.assignmentId||old.instructor_id!==a.user.userId||old.learner_account_id!==link.user_id||old.observed_at!==c.observedAt||old.setting!==c.setting||old.rubric!==JSON.stringify(c.rubric)||old.outcome!==c.outcome||old.note!==c.note)throw new Error('Record conflict: observation request ID was reused.');return launchResponse({saved:true,id:c.id,alreadySaved:true,certifiable:false});}
  await db().batch([db().prepare('INSERT INTO learning_course_observations VALUES(?,?,?,?,?,?,?,?,?,?,?)').bind(a.owner,c.id,c.assignmentId,a.user.userId,link.user_id,c.observedAt,c.setting,JSON.stringify(c.rubric),c.outcome,c.note,now),audit(a,'launch.observation',c.id,{assignmentId:c.assignmentId})]);return launchResponse({saved:true,id:c.id,certifiable:false});
 }
 const submission=await db().prepare('SELECT account_id,withdrawn_at FROM learning_course_submissions WHERE owner=? AND id=?').bind(a.owner,c.submissionId).first<{account_id:string;withdrawn_at:number|null}>();
 if(!submission||submission.withdrawn_at!==null)throw new Error('No shared submission available.');if(submission.account_id===a.user.userId)throw new Error('Forbidden: you cannot review your own learning evidence.');
 const previous=await db().prepare('SELECT * FROM learning_review_events WHERE owner=? AND id=?').bind(a.owner,c.id).first();
 if(previous){if(previous.submission_id!==c.submissionId||previous.actor_id!==a.user.userId||previous.outcome!==c.outcome||previous.note!==c.note)throw new Error('Record conflict: feedback ID was reused.');return launchResponse({saved:true,id:c.id,alreadySaved:true});}
 await db().batch([db().prepare('INSERT INTO learning_review_events SELECT ?,?,?,?,?,?,?,MAX(?,COALESCE(MAX(created_at)+1,0)) FROM learning_review_events WHERE owner=? AND submission_id=?').bind(a.owner,c.id,c.submissionId,a.user.userId,c.previousEventId,c.outcome,c.note,now,a.owner,c.submissionId),audit(a,'launch.review',c.submissionId,{outcome:c.outcome})]);return launchResponse({saved:true,id:c.id,certifiable:false});
}catch(error){return launchFailure(error)}}
