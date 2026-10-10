import {z} from 'zod';
import {access,db} from './server';
import {learnerAccess} from './learner';
import {centreId} from './learner-scope';
import {uuid} from './learning-centre';

export const inboxScope=z.object({scope:z.enum(['learner','educator']),workerId:uuid.optional(),owner:centreId.optional()}).refine(v=>v.scope!=='learner'||v.workerId!==undefined);
export const inboxItemId=z.string().regex(/^(assignment|cancelled|submission|review|observation):[a-f0-9-]{32,64}$/);
export type InboxRow={id:string;kind:string;assignment_id:string;worker_id:string;created_at:number;title:string;read_at:number|null;dismissed_at:number|null};
export async function authorisedInbox(scope:'learner'|'educator',workerId?:string,owner?:string){
 if(scope==='learner'){
  if(!workerId)throw new Error('Invalid learner profile.');const a=await learnerAccess(workerId,owner);return {scope,owner:a.owner,workerId,accountId:a.user.userId};
 }if(owner!==undefined)throw new Error('Invalid educator inbox filter.');const a=await access();return {scope,owner:a.owner,workerId:null,accountId:a.user.userId};
}
/** Structured destinations are re-resolved under current enrolment/workspace access. */
export async function inboxRows(context:Awaited<ReturnType<typeof authorisedInbox>>,itemId?:string,includeDismissed=false){
 const c=context;
 const learner=c.scope==='learner';
 const source=learner?`
 SELECT 'assignment:'||a.id AS id,'assignment' AS kind,a.id AS assignment_id,a.worker_id,a.created_at,'New learning assignment' AS title FROM learning_course_assignments a WHERE a.owner=? AND a.worker_id=?
 UNION ALL SELECT 'cancelled:'||a.id,'cancelled',a.id,a.worker_id,a.cancelled_at,'Assignment cancelled' FROM learning_course_assignments a WHERE a.owner=? AND a.worker_id=? AND a.cancelled_at IS NOT NULL
 UNION ALL SELECT 'review:'||e.id,'review',s.assignment_id,s.worker_id,e.created_at,'Educator feedback available' FROM learning_review_events e JOIN learning_course_submissions s ON s.owner=e.owner AND s.id=e.submission_id WHERE s.owner=? AND s.worker_id=? AND s.account_id=? AND s.withdrawn_at IS NULL AND e.outcome<>'correction-request'
 UNION ALL SELECT 'observation:'||o.id,'observation',o.assignment_id,a.worker_id,o.created_at,'Learning observation recorded' FROM learning_course_observations o JOIN learning_course_assignments a ON a.owner=o.owner AND a.id=o.assignment_id WHERE a.owner=? AND a.worker_id=? AND o.learner_account_id=?`
 :`SELECT 'submission:'||s.id AS id,'submission' AS kind,s.assignment_id,s.worker_id,s.created_at,'Shared practice ready for review' AS title FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.withdrawn_at IS NULL AND s.account_id<>?
 UNION ALL SELECT 'review:'||e.id,'review',s.assignment_id,s.worker_id,e.created_at,'Learner requested a correction' FROM learning_review_events e JOIN learning_course_submissions s ON s.owner=e.owner AND s.id=e.submission_id JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.withdrawn_at IS NULL AND e.outcome='correction-request' AND s.account_id<>?`;
 const bindings=learner?[c.owner,c.workerId,c.owner,c.workerId,c.owner,c.workerId,c.accountId,c.owner,c.workerId,c.accountId]:[c.owner,c.accountId,c.owner,c.accountId];
 const result=await db().prepare(`SELECT e.*,s.read_at,s.dismissed_at FROM (${source}) e LEFT JOIN learning_inbox_state s ON s.account_id=? AND s.item_id=?||':'||e.id WHERE 1=1${itemId?' AND e.id=?':''}${includeDismissed?'':' AND s.dismissed_at IS NULL'} ORDER BY e.created_at DESC,e.id DESC LIMIT ${itemId?1:101}`).bind(...bindings,c.accountId,c.owner,...(itemId?[itemId]:[])).all<InboxRow>();
 return result.results;
}
