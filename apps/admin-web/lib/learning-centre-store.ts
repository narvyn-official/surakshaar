import {db,failure} from './server';
import {assignmentProgress,type LaunchAssignment,type Submission} from './learning-centre';

export function launchResponse(value:unknown,status=200){return Response.json(value,{status,headers:{'Cache-Control':'no-store'}})}
export function launchFailure(error:unknown){
 const message=error instanceof Error?error.message:'';
 if(message.includes('launch-assignment-identity-conflict'))return launchResponse({error:'Record conflict: assignment request identity was reused.'},409);
 if(message.includes('launch-review-stale-parent'))return launchResponse({error:'Record conflict: feedback changed. Refresh before adding another entry.'},409);
 if(/launch-(sharing|review|self-review)/.test(message))return launchResponse({error:'Forbidden: access or learner identity changed. Refresh before continuing.'},403);
 if(message.startsWith('Record conflict'))return launchResponse({error:message},409);
 return failure(error);
}
export async function launchAssignment(owner:string,id:string){
 const result=await db().prepare('SELECT a.*,w.name AS learner_name FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=? AND a.id=?').bind(owner,id).first<LaunchAssignment&{learner_name:string}>();
 if(!result)throw new Error('No launch-course assignment found.');return result;
}
export async function assignmentView(assignment:LaunchAssignment,accountId?:string,cursor?:{beforeAt:number;beforeId:string}){
 // A revoked/replaced enrolment cannot reveal the previous account's evidence.
 const result=await db().prepare('SELECT s.* FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.assignment_id=?'+(accountId?' AND s.account_id=?':'')+(cursor?' AND (s.created_at<? OR (s.created_at=? AND s.id<?))':'')+' ORDER BY s.created_at DESC,s.id DESC LIMIT 11').bind(assignment.owner,assignment.id,...(accountId?[accountId]:[]),...(cursor?[cursor.beforeAt,cursor.beforeAt,cursor.beforeId]:[])).all<Submission>();
 const shared=result.results.slice(0,10),ids=shared.map(s=>s.id),last=shared.at(-1);
 const reviews=ids.length?await db().prepare('SELECT e.* FROM learning_review_events e JOIN learning_course_submissions s ON s.owner=e.owner AND s.id=e.submission_id JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.assignment_id=? AND s.withdrawn_at IS NULL AND s.id IN ('+ids.map(()=>'?').join(',')+') ORDER BY e.created_at DESC,e.id DESC LIMIT 300').bind(assignment.owner,assignment.id,...ids).all():{results:[]};
 const observations=await db().prepare('SELECT o.* FROM learning_course_observations o JOIN learning_course_assignments a ON a.owner=o.owner AND a.id=o.assignment_id JOIN learner_links l ON l.owner=a.owner AND l.worker_id=a.worker_id AND l.user_id=o.learner_account_id WHERE o.owner=? AND o.assignment_id=? ORDER BY o.created_at DESC,o.id DESC LIMIT 100').bind(assignment.owner,assignment.id).all();
 const completion=await db().prepare('SELECT DISTINCT s.assignment_id,s.scenario_id,s.withdrawn_at FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.assignment_id=? AND s.withdrawn_at IS NULL').bind(assignment.owner,assignment.id).all<Submission>();
 return {...assignment,scenarioIds:JSON.parse(assignment.scenario_ids),catalogue:JSON.parse(assignment.catalogue_snapshot),...assignmentProgress(assignment,completion.results),
 submissions:shared.map(s=>({...s,evidence:s.withdrawn_at===null?JSON.parse(s.evidence):null})),reviews:reviews.results,observations:observations.results.map(o=>({...o,rubric:JSON.parse(String(o.rubric))})),nextSubmissionCursor:result.results.length>10&&last?{beforeAt:last.created_at,beforeId:last.id}:null,limits:{submissions:10,reviews:300,observations:100},certifiable:false};
}
export async function launchBudget(accountId:string){
 const minute=Math.floor(Date.now()/60000);
 const r=await db().prepare('INSERT INTO learning_sync_budget(account_id,minute,count) VALUES(?,?,1) ON CONFLICT(account_id,minute) DO UPDATE SET count=count+1 WHERE count<20 RETURNING count').bind(accountId,minute).first();
 if(!r)throw new Error('Training request limit reached. Wait a minute and retry.');
}
