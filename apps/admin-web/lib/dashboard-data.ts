import {learnerScope} from './staff-scope';
import {db} from './server';
import {workspaceContext,type WorkspaceContext} from './workspace-context';
import {workspaceDefinitions,roleActions} from './role-workspace';
export type DashboardData={context:WorkspaceContext;generatedAt:number;stats:{label:string;value:number;detail:string}[];queue:{id:string;title:string;detail:string;href:string}[];actions:typeof workspaceDefinitions.platform.actions};
export async function dashboardData(contextOnly=false):Promise<DashboardData>{
 const context=await workspaceContext();if(!context)throw Error('Sign in to open your dashboard.');const now=Date.now(),{role,user,institution}=context,out:DashboardData={context,generatedAt:now,stats:[],queue:[],actions:roleActions(role,context.restricted)};
 if(contextOnly||context.mfaRequired||context.workspaceUnavailable)return out;
 async function stat(label:string,sql:string,args:unknown[],detail:string){const row=await db().prepare(sql).bind(...args).first<{n:number}>();out.stats.push({label,value:Number(row?.n??0),detail});}
 const count=(table:string,where:string)=>'SELECT COUNT(*) AS n FROM '+table+' WHERE '+where;
 if(role==='platform'){
  for(const [status,label] of [['approved','Active institutes'],['pending','Awaiting approval'],['suspended','Suspended institutes']])await stat(label,count('centre_approvals','status=?'),[status],'Current institute approval status');
  await stat('Restricted plans',count('institution_plans','restricted=1 OR (valid_until IS NOT NULL AND valid_until<=?)'),[now],'New enrolment and issuance restrictions');
  const rows=await db().prepare("SELECT c.owner,c.name,c.site,a.requested_at FROM centre_approvals a JOIN training_centres c ON c.owner=a.owner WHERE a.status='pending' ORDER BY a.requested_at LIMIT 8").all<{owner:string;name:string;site:string;requested_at:number}>();out.queue=rows.results.map(r=>({id:r.owner,title:r.name,detail:'Application · '+r.site,href:'/platform?section=institutes'}));return out;
 }
 if(institution){const owner=institution.owner;
  if(context.restricted){
   const actor={owner,user,restricted:true},scope=learnerScope(actor,'a.worker_id'),people=learnerScope(actor,'w.id');
   await stat('Learners in your scope','SELECT COUNT(*) n FROM workers w WHERE w.owner=? AND '+people.sql,[owner,...people.args],'Active people in assigned organisational units');
   await stat('Current course assignments','SELECT COUNT(*) n FROM learning_course_assignments a WHERE a.owner=? AND a.cancelled_at IS NULL AND '+scope.sql,[owner,...scope.args],'Only assignments in your current scope');
   const rows=await db().prepare('SELECT a.id,a.course_id,w.name,a.due_at FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=? AND a.cancelled_at IS NULL AND '+scope.sql+' ORDER BY a.created_at DESC LIMIT 8').bind(owner,...scope.args).all<{id:string;course_id:string;name:string;due_at:number}>();
   out.queue=rows.results.map(r=>({id:r.id,title:r.name+' · '+r.course_id,detail:'Due '+new Date(r.due_at).toISOString().slice(0,10),href:'/coursework'}));return out;
  }
  if(role==='author'||role==='reviewer'){
   const scope=role==='author'?'author_id=?':'author_id<>?';
   for(const [status,label] of [['submitted',role==='author'?'Awaiting review':'Ready for review'],['approved','Approved procedures'],['rejected','Returned procedures']])await stat(label,count('local_procedures','owner=? AND '+scope+' AND status=?'),[owner,user.userId,status],'Local institution content');
   const rows=await db().prepare('SELECT id,title,status,language FROM local_procedures WHERE owner=? AND '+scope+' ORDER BY created_at DESC LIMIT 8').bind(owner,user.userId).all<{id:string;title:string;status:string;language:string}>();out.queue=rows.results.map(r=>({...r,detail:r.status+' · '+r.language,href:'/institution?section=content'}));return out;
  }
  if(role==='auditor'){
   await stat('Events in 30 days',count('audit_log','owner=? AND at>=?'),[owner,now-30*86400000],'Event metadata only');
   await stat('Active staff',count('team_members','owner=? AND active=1'),[owner],'Appointed colleagues, excluding the owner');
   await stat('Revoked records',count('institution_awards','owner=? AND revoked_at IS NOT NULL'),[owner],'Institution learning records');
   const rows=await db().prepare('SELECT id,action,at FROM audit_log WHERE owner=? ORDER BY at DESC,id DESC LIMIT 8').bind(owner).all<{id:string;action:string;at:number}>();out.queue=rows.results.map(r=>({id:r.id,title:r.action,detail:new Date(r.at).toISOString(),href:'/audit'}));return out;
  }
  if(['examiner','assessor'].includes(role)){
   await stat('Identity awaiting review',count('identity_sessions',"owner=? AND status='submitted' AND expires_at>?"),[owner,now],'Adult sessions, private examiner access');
   await stat('Open identity appeals',count('identity_sessions',"owner=? AND appeal IS NOT NULL AND status='needs-review' AND expires_at>?"),[owner,now],'Require a different examiner');
  }
  if(['certifier','assessor','viewer'].includes(role)){
   await stat('Active learning records',count('institution_awards','owner=? AND revoked_at IS NULL AND replaced_by IS NULL AND expires_at>?'),[owner,now],'Signed institution-issued records');
   await stat('Expired learning records',count('institution_awards','owner=? AND revoked_at IS NULL AND replaced_by IS NULL AND expires_at<=?'),[owner,now],'Review before any new issuance');
  }
  if(['admin','trainer','viewer'].includes(role))await stat('Active learners','SELECT COUNT(*) AS n FROM workers w LEFT JOIN institution_people p ON p.owner=w.owner AND p.worker_id=w.id WHERE w.owner=? AND p.archived_at IS NULL',[owner],'Registered people, excluding archived access');
  if(role==='admin'){
   await stat('Active staff',count('team_members','owner=? AND active=1'),[owner],'Appointed colleagues, excluding you');
   await stat('Enrolment references',count('identity_references',"owner=? AND status='pending' AND expires_at>?"),[owner,now],'Awaiting attended enrolment approval');
  }
  if(['trainer','examiner','assessor'].includes(role))await stat('Shared work without feedback',"SELECT COUNT(*) AS n FROM learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id WHERE s.owner=? AND s.withdrawn_at IS NULL AND NOT EXISTS(SELECT 1 FROM learning_review_events r WHERE r.owner=s.owner AND r.submission_id=s.id AND r.outcome<>'correction-request')",[owner],'Shared snapshots with no educator feedback yet');
  if(out.stats.length<4)await stat('Current course assignments',count('learning_course_assignments','owner=? AND cancelled_at IS NULL'),[owner],'Includes completed practice awaiting record review');
  const rows=await db().prepare('SELECT a.id,a.course_id,a.due_at,w.name FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=? AND a.cancelled_at IS NULL ORDER BY a.created_at DESC LIMIT 8').bind(owner).all<{id:string;course_id:string;due_at:number;name:string}>();out.queue=rows.results.map(r=>({id:r.id,title:r.name+' · '+r.course_id,detail:'Due '+new Date(r.due_at).toISOString().slice(0,10),href:role==='certifier'?'/awards':'/coursework'}));return out;
 }
 if(role==='supporter'){
  const rows=await db().prepare('SELECT id,label,expires_at FROM learning_support_links WHERE supporter_id=? AND revoked_at IS NULL AND expires_at>? ORDER BY created_at DESC LIMIT 20').bind(user.userId,now).all<{id:string;label:string;expires_at:number}>();out.stats=[{label:'Active shared summaries',value:rows.results.length,detail:'Up to 20 current links shown; learners control access'}];out.queue=rows.results.map(r=>({id:r.id,title:r.label,detail:'Access until '+new Date(r.expires_at).toISOString().slice(0,10),href:'/learning-support'}));return out;
 }
 await stat('Completed account practices',count('learning_practice_heads',"account_id=? AND json_extract(payload,'$.finished')=1"),[user.userId],'Backed-up practice; unsynced device work is not included');
 await stat('Practice in progress',count('learning_practice_heads',"account_id=? AND json_extract(payload,'$.finished')=0"),[user.userId],'Backed-up attempts ready to resume on their device');
 await stat('Active institution records',count('institution_awards','account_id=? AND revoked_at IS NULL AND replaced_by IS NULL AND expires_at>?'),[user.userId,now],'Your own issued records');
 if(role==='learner'){
 const rows=await db().prepare("SELECT a.id,a.course_id,a.due_at,c.name FROM learning_course_assignments a JOIN learner_links l ON l.owner=a.owner AND l.worker_id=a.worker_id JOIN training_centres c ON c.owner=a.owner JOIN centre_approvals p ON p.owner=a.owner AND p.status='approved' LEFT JOIN institution_people i ON i.owner=a.owner AND i.worker_id=a.worker_id WHERE l.user_id=? AND a.cancelled_at IS NULL AND i.archived_at IS NULL ORDER BY a.due_at LIMIT 8").bind(user.userId).all<{id:string;course_id:string;due_at:number;name:string}>();out.queue=rows.results.map(r=>({id:r.id,title:r.course_id+' · '+r.name,detail:'Due '+new Date(r.due_at).toISOString().slice(0,10),href:'/learn'}));
 }return out;
}
