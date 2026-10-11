import {learnerScope} from './staff-scope';
import {db} from './server';
import {workspaceContext,type WorkspaceContext} from './workspace-context';
import {workspaceDefinitions,roleActions} from './role-workspace';
export type DashboardData={context:WorkspaceContext;generatedAt:number;stats:{label:string;value:number;detail:string}[];queue:{id:string;title:string;detail:string;href:string}[];queueTitle?:string;actions:typeof workspaceDefinitions.platform.actions};
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
  if(role==='author'||role==='reviewer'){
   const scope=role==='author'?'author_id=?':"author_id<>? AND status<>'draft'";
   if(role==='author')await stat('Saved drafts',count('local_procedures','owner=? AND '+scope+" AND status='draft'"),[owner,user.userId],'Your private drafts, ready to continue');
   out.queueTitle=role==='author'?'Drafts and returned procedures':'Procedures awaiting review';
   for(const [status,label] of [['submitted',role==='author'?'Awaiting review':'Ready for review'],['approved','Approved procedures'],['rejected','Returned procedures']])await stat(label,count('local_procedures','owner=? AND '+scope+' AND status=?'),[owner,user.userId,status],'Local institution content');
   const rows=await db().prepare('SELECT id,title,status,language FROM local_procedures WHERE owner=? AND '+scope+(role==='author'?" AND status IN ('draft','rejected')":" AND status='submitted'")+' ORDER BY created_at DESC,id DESC LIMIT 8').bind(owner,user.userId).all<{id:string;title:string;status:string;language:string}>();out.queue=rows.results.map(r=>({...r,detail:r.status+' · '+r.language,href:'/institution?section=content'}));return out;
  }
  if(role==='auditor'){
   await stat('Events in 30 days',count('audit_log','owner=? AND at>=?'),[owner,now-30*86400000],'Event metadata only');
   await stat('Active staff',count('team_members','owner=? AND active=1'),[owner],'Appointed colleagues, excluding the owner');
   await stat('Revoked records',count('institution_awards','owner=? AND revoked_at IS NOT NULL'),[owner],'Institution learning records');
   const rows=await db().prepare('SELECT id,action,at FROM audit_log WHERE owner=? ORDER BY at DESC,id DESC LIMIT 8').bind(owner).all<{id:string;action:string;at:number}>();out.queue=rows.results.map(r=>({id:r.id,title:r.action,detail:new Date(r.at).toISOString(),href:'/audit'}));return out;
  }
  const actor={owner,user,restricted:context.restricted};
  // Dashboard summaries use the same unit scope as task APIs, then exclude archived people.
  function active(column:string){const scoped=learnerScope(actor,column);return {sql:scoped.sql+` AND NOT EXISTS(SELECT 1 FROM institution_people archived WHERE archived.owner=? AND archived.worker_id=${column} AND archived.archived_at IS NOT NULL)`,args:[...scoped.args,owner]};}
  const people=active('w.id'),assignments=active('a.worker_id'),records=active('r.worker_id'),identity=active('s.worker_id'),submitted=active('s.worker_id');
  if(context.restricted||['admin','trainer','viewer'].includes(role))await stat(context.restricted?'Learners in your scope':'Active learners','SELECT COUNT(*) n FROM workers w WHERE w.owner=? AND '+people.sql,[owner,...people.args],'Active people within your current access');
  const identityWhere="s.owner=? AND s.expires_at>? AND s.account_id<>? AND EXISTS(SELECT 1 FROM learner_links l WHERE l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id) AND "+identity.sql;
  if(['examiner','assessor'].includes(role)){
   await stat('Identity awaiting review',count('identity_sessions s',identityWhere+" AND s.status='submitted'"),[owner,now,user.userId,...identity.args],'Unexpired sessions awaiting an independent examiner');
   await stat('Open identity appeals',count('identity_sessions s',identityWhere+" AND s.appeal IS NOT NULL AND s.status='needs-review' AND s.reviewed_by<>?"),[owner,now,user.userId,...identity.args,user.userId],'Only appeals you may independently review');
  }
  if(['certifier','assessor','viewer'].includes(role)){
   await stat('Active learning records',count('institution_awards r','r.owner=? AND r.revoked_at IS NULL AND r.replaced_by IS NULL AND r.expires_at>? AND '+records.sql),[owner,now,...records.args],'Signed records for active learners in your access');
   await stat('Expired learning records',count('institution_awards r','r.owner=? AND r.revoked_at IS NULL AND r.replaced_by IS NULL AND r.expires_at<=? AND '+records.sql),[owner,now,...records.args],'Historical status; new issuance requires checking evidence');
  }
  if(role==='admin'){
   await stat('Active staff',count('team_members','owner=? AND active=1'),[owner],'Appointed colleagues, excluding you');
   const refs=active('r.worker_id');
   await stat('Enrolment references',count('identity_references r',"r.owner=? AND r.status='pending' AND r.expires_at>? AND "+refs.sql),[owner,now,...refs.args],'Awaiting attended enrolment approval');
  }
  const pendingWork="s.owner=? AND s.withdrawn_at IS NULL AND s.account_id<>? AND a.cancelled_at IS NULL AND "+submitted.sql+" AND COALESCE((SELECT r.outcome FROM learning_review_events r WHERE r.owner=s.owner AND r.submission_id=s.id ORDER BY r.created_at DESC,r.id DESC LIMIT 1),'correction-request')='correction-request'";
  const submissionJoin='learning_course_submissions s JOIN learner_links l ON l.owner=s.owner AND l.worker_id=s.worker_id AND l.user_id=s.account_id JOIN learning_course_assignments a ON a.owner=s.owner AND a.id=s.assignment_id';
  if(['admin','trainer','examiner','assessor'].includes(role))await stat('Shared work awaiting feedback','SELECT COUNT(*) n FROM '+submissionJoin+' WHERE '+pendingWork,[owner,user.userId,...submitted.args],'Active shared submissions requiring an independent educator');
  await stat('Current course assignments',count('learning_course_assignments a','a.owner=? AND a.cancelled_at IS NULL AND '+assignments.sql),[owner,...assignments.args],'Active-learner assignments, including work already shared');
  if(['examiner','assessor'].includes(role)){
   out.queueTitle='Evidence awaiting your review';
   const sessions=await db().prepare("SELECT s.id,s.status,s.appeal,w.name FROM identity_sessions s JOIN workers w ON w.owner=s.owner AND w.id=s.worker_id WHERE "+identityWhere+" AND (s.status='submitted' OR (s.status='needs-review' AND s.appeal IS NOT NULL AND s.reviewed_by<>?)) ORDER BY s.started_at LIMIT 4").bind(owner,now,user.userId,...identity.args,user.userId).all<{id:string;name:string;status:string;appeal:string|null}>();
   out.queue=sessions.results.map(r=>({id:r.id,title:r.name,detail:r.appeal?'Identity appeal':'Identity evidence awaiting review',href:'/identity-review'}));
   const work=await db().prepare('SELECT s.id,s.assignment_id,w.name,a.course_id FROM '+submissionJoin+' JOIN workers w ON w.owner=s.owner AND w.id=s.worker_id WHERE '+pendingWork+' ORDER BY s.created_at LIMIT 4').bind(owner,user.userId,...submitted.args).all<{id:string;assignment_id:string;name:string;course_id:string}>();
   out.queue.push(...work.results.map(r=>({id:r.id,title:r.name+' · '+r.course_id,detail:'Shared work awaiting feedback',href:'/coursework?assignment='+r.assignment_id})));return out;
  }
  out.queueTitle=role==='certifier'?'Assignments to check for issuance':'Current course assignments';
  const withoutRecord=role==='certifier'?" AND NOT EXISTS(SELECT 1 FROM institution_awards r WHERE r.owner=a.owner AND r.assignment_id=a.id AND r.revoked_at IS NULL AND r.replaced_by IS NULL AND r.expires_at>?)":'';
  const rows=await db().prepare('SELECT a.id,a.course_id,a.due_at,w.name FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=? AND a.cancelled_at IS NULL AND '+assignments.sql+withoutRecord+' ORDER BY a.created_at DESC,a.id DESC LIMIT 8').bind(owner,...assignments.args,...(role==='certifier'?[now]:[])).all<{id:string;course_id:string;due_at:number;name:string}>();
  out.queue=rows.results.map(r=>({id:r.id,title:r.name+' · '+r.course_id,detail:role==='certifier'?'No active record. Check all issuance prerequisites.':'Due '+new Date(r.due_at).toISOString().slice(0,10),href:role==='certifier'?'/awards':'/coursework?assignment='+r.id}));return out;
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
