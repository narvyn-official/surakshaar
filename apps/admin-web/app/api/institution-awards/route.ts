import {learnerScope,requireWorkerScope,scopeAssertion} from '@/lib/staff-scope';
import {z} from 'zod';
import {access,audit,db,canonical,failure,json} from '@/lib/server';
import {freshUser,getAppUser} from '@/lib/auth';
import {awardKey,awardStatus,awardStorageReady} from '@/lib/institution-awards';
import {currentInstitutionPolicy,requireInstitutionCapacity} from '@/lib/institution-store';
import {launchBudget} from '@/lib/learning-centre-store';
const command=z.discriminatedUnion('action',[
 z.object({action:z.literal('issue'),id:z.string().uuid(),assignmentId:z.string().uuid(),reason:z.string().trim().min(10).max(1000),replaceId:z.string().uuid().optional()}).strict(),
 z.object({action:z.literal('revoke'),id:z.string().uuid(),reason:z.string().trim().min(10).max(1000)}).strict(),
 z.object({action:z.literal('rotate-key'),reason:z.string().trim().min(10).max(1000)}).strict(),
]);
const reply=(data:unknown)=>Response.json(data,{headers:{'Cache-Control':'private, no-store'}});
export async function GET(request:Request){try{
 const learner=new URL(request.url).searchParams.get('learner')==='1';
 if(learner){const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const workerId=new URL(request.url).searchParams.get('workerId');if(workerId&&!z.string().uuid().safeParse(workerId).success)throw Error('Invalid learner profile.');const rows=await db().prepare('SELECT id,payload,issued_at,expires_at,revoked_at,replaced_by FROM institution_awards WHERE account_id=?'+(workerId?' AND worker_id=?':'')+' ORDER BY issued_at DESC LIMIT 200').bind(user.userId,...(workerId?[workerId]:[])).all();return reply({awards:rows.results.map(row=>({...row,payload:JSON.parse(String(row.payload)),status:awardStatus(row as never)})),role:'learner',assignments:[]});}
 const a=await access('read',true),scope=learnerScope(a,'r.worker_id'),assignmentScope=learnerScope(a,'a.worker_id'),rows=await db().prepare('SELECT id,assignment_id,payload,issued_at,expires_at,revoked_at,replaced_by,revocation_reason FROM institution_awards r WHERE owner=? AND '+scope.sql+' ORDER BY issued_at DESC LIMIT 200').bind(a.owner,...scope.args).all();
 const assignments=await db().prepare('SELECT a.id,a.course_id,w.name,a.mode,a.created_at FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id WHERE a.owner=? AND a.cancelled_at IS NULL AND '+assignmentScope.sql+' ORDER BY a.created_at DESC LIMIT 200').bind(a.owner,...assignmentScope.args).all();
 return reply({role:a.role,configured:awardStorageReady(),awards:rows.results.map(row=>({...row,payload:JSON.parse(String(row.payload)),status:awardStatus(row as never)})),assignments:assignments.results});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const input=command.safeParse(await json(request));if(!input.success)throw Error('Invalid institution record request.');const c=input.data,a=await access(c.action==='issue'?'certify':c.action==='revoke'?'revoke':'admin',c.action!=='rotate-key');await freshUser();await launchBudget(a.user.userId);const now=Date.now();
 if(c.action==='rotate-key'){
  await db().batch([db().prepare('UPDATE institution_signing_keys SET retired_at=? WHERE owner=? AND retired_at IS NULL').bind(now,a.owner),audit(a,'award.key-retired',a.owner,{reason:c.reason})]);await awardKey(a.owner);return reply({rotated:true});
 }
 if(c.action==='revoke'){
  const record=await db().prepare('SELECT worker_id FROM institution_awards WHERE owner=? AND id=?').bind(a.owner,c.id).first<{worker_id:string}>();if(!record)throw Error('No active institution record found.');await requireWorkerScope(a,record.worker_id);
  const result=await db().batch([db().prepare('UPDATE institution_awards SET revoked_at=?,revocation_reason=? WHERE owner=? AND id=? AND revoked_at IS NULL').bind(now,c.reason,a.owner,c.id),audit(a,'award.revoke',c.id,{reason:c.reason},true),scopeAssertion(a,record.worker_id)]);if(!result[0].meta.changes)throw Error('No active institution record found.');return reply({revoked:true});
 }
 const old=await db().prepare('SELECT assignment_id,issued_by,worker_id FROM institution_awards WHERE owner=? AND id=?').bind(a.owner,c.id).first();if(old){await requireWorkerScope(a,String(old.worker_id));if(old.assignment_id!==c.assignmentId||old.issued_by!==a.user.userId)throw Error('Record conflict: record ID reused.');return reply({saved:true,id:c.id});}
 await requireInstitutionCapacity(a.owner);
 const assignment=await db().prepare('SELECT a.*,w.name AS learner_name,l.user_id,c.name AS centre_name,c.site FROM learning_course_assignments a JOIN workers w ON w.owner=a.owner AND w.id=a.worker_id JOIN learner_links l ON l.owner=a.owner AND l.worker_id=a.worker_id JOIN training_centres c ON c.owner=a.owner WHERE a.owner=? AND a.id=? AND a.cancelled_at IS NULL').bind(a.owner,c.assignmentId).first<{worker_id:string;user_id:string;learner_name:string;course_id:string;scenario_ids:string;policy_version:number|null;content_version:string;mode:string;centre_name:string;site:string}>();
 if(!assignment?.user_id)throw Error('No active linked assignment found.');await requireWorkerScope(a,assignment.worker_id);if(assignment.user_id===a.user.userId)throw Error('Forbidden: you cannot issue your own learning record.');
 if(await db().prepare('SELECT 1 FROM institution_people WHERE owner=? AND worker_id=? AND archived_at IS NOT NULL').bind(a.owner,assignment.worker_id).first())throw Error('Forbidden: learner membership is archived.');
 const {version,policy}=await currentInstitutionPolicy(a.owner,assignment.policy_version),evidence:{scenario:string;submission:string;review:string;reviewer:string}[]=[],checks:{sql:string;args:unknown[]}[]=[];
 for(const scenario of JSON.parse(assignment.scenario_ids) as string[]){
  const row=await db().prepare("SELECT s.id,r.id AS review_id,r.actor_id,r.outcome FROM learning_course_submissions s LEFT JOIN learning_review_events r ON r.owner=s.owner AND r.submission_id=s.id AND r.id=(SELECT id FROM learning_review_events WHERE owner=s.owner AND submission_id=s.id ORDER BY created_at DESC,id DESC LIMIT 1) WHERE s.owner=? AND s.assignment_id=? AND s.scenario_id=? AND s.account_id=? AND s.withdrawn_at IS NULL ORDER BY s.created_at DESC,s.id DESC LIMIT 1").bind(a.owner,c.assignmentId,scenario,assignment.user_id).first<{id:string;review_id:string;actor_id:string;outcome:string}>();
  if(!row||row.outcome!=='reviewed')throw Error('Invalid issuance: every assigned scenario needs current reviewed evidence.');if(policy.separateIssuer&&row.actor_id===a.user.userId)throw Error('Forbidden: this policy requires a separate reviewer and issuer.');
  evidence.push({scenario,submission:row.id,review:row.review_id,reviewer:row.actor_id});
  checks.push({sql:"EXISTS(SELECT 1 FROM learning_course_submissions s WHERE s.owner=? AND s.id=? AND s.withdrawn_at IS NULL AND s.id=(SELECT newer.id FROM learning_course_submissions newer WHERE newer.owner=s.owner AND newer.assignment_id=s.assignment_id AND newer.scenario_id=s.scenario_id AND newer.account_id=s.account_id AND newer.withdrawn_at IS NULL ORDER BY newer.created_at DESC,newer.id DESC LIMIT 1) AND ?=(SELECT id FROM learning_review_events WHERE owner=s.owner AND submission_id=s.id ORDER BY created_at DESC,id DESC LIMIT 1))",args:[a.owner,row.id,row.review_id]});
  if(policy.assessment==='snapshots'){
   const identity=await db().prepare("SELECT id,revision FROM identity_sessions WHERE owner=? AND assignment_id=? AND scenario_id=? AND account_id=? AND status='accepted' AND expires_at>? ORDER BY started_at DESC LIMIT 1").bind(a.owner,c.assignmentId,scenario,assignment.user_id,now).first<{id:string;revision:number}>();if(!identity)throw Error('Invalid issuance: accepted identity evidence is required for every scenario.');
   checks.push({sql:"EXISTS(SELECT 1 FROM identity_sessions s JOIN identity_references r ON r.owner=s.owner AND r.id=s.reference_id WHERE s.id=? AND s.status='accepted' AND s.revision=? AND r.status IN('approved','replaced') AND r.ciphertext<>'')",args:[identity.id,identity.revision]});
  }
 }
 const observation=await db().prepare('SELECT id,instructor_id,outcome,setting FROM learning_course_observations WHERE owner=? AND assignment_id=? AND learner_account_id=? ORDER BY created_at DESC,id DESC LIMIT 1').bind(a.owner,c.assignmentId,assignment.user_id).first<{id:string;instructor_id:string;outcome:string;setting:string}>();
 if((policy.requirePractical||policy.assessment==='supervised')&&(!observation||observation.outcome!=='observed'||observation.setting!=='in-person-training'))throw Error('Invalid issuance: a satisfactory in-person observation is required.');
 if(observation&&policy.separateIssuer&&observation.instructor_id===a.user.userId)throw Error('Forbidden: the observation and issuance must be independently reviewed.');
 if(observation)checks.push({sql:'?=(SELECT id FROM learning_course_observations WHERE owner=? AND assignment_id=? AND learner_account_id=? ORDER BY created_at DESC,id DESC LIMIT 1)',args:[observation.id,a.owner,c.assignmentId,assignment.user_id]});
 if(c.replaceId&&!await db().prepare('SELECT 1 FROM institution_awards WHERE owner=? AND id=? AND assignment_id=? AND replaced_by IS NULL AND revoked_at IS NULL').bind(a.owner,c.replaceId,c.assignmentId).first())throw Error('Invalid replacement: choose an active record for this assignment.');
 const expiresAt=now+policy.certificateDays*86400000,key=await awardKey(a.owner);
 const payload=canonical({schema:1,id:c.id,kid:key.kid,issuer:assignment.centre_name,issuerSite:assignment.site,learner:assignment.learner_name,course:assignment.course_id,contentVersion:assignment.content_version,policyVersion:version,mode:assignment.mode,identityMethod:policy.assessment,observation:observation?.outcome==='observed'?observation.setting:'not-assessed',scope:'Institution-issued formative learning record. Draft curriculum; not an accredited qualification or authorisation to work.',evidence,issuedAt:now,expiresAt,replaces:c.replaceId??null}),signature=key.sign(payload);
 const statements:D1PreparedStatement[]=[];
 if(c.replaceId)statements.push(db().prepare('UPDATE institution_awards SET replaced_by=? WHERE owner=? AND id=? AND replaced_by IS NULL AND revoked_at IS NULL').bind(c.id,a.owner,c.replaceId));
 // Constraint failure rolls back replacement too. Evidence is rechecked in the insertion transaction.
 statements.push(db().prepare('INSERT INTO institution_awards(owner,id,assignment_id,worker_id,account_id,kid,payload,signature,issued_by,issued_at,expires_at) SELECT ?,CASE WHEN '+checks.map(check=>check.sql).join(' AND ')+' THEN ? ELSE NULL END,?,?,?,?,?,?,?,?,?').bind(a.owner,...checks.flatMap(check=>check.args),c.id,c.assignmentId,assignment.worker_id,assignment.user_id,key.kid,payload,signature,a.user.userId,now,expiresAt),audit(a,'award.issue',c.id,{reason:c.reason,replaces:c.replaceId??null},true));
 statements.push(scopeAssertion(a,assignment.worker_id));
 const result=await db().batch(statements);if(!result[c.replaceId?1:0].meta.changes)throw Error('Record conflict: evidence changed during issuance. Refresh and retry.');return reply({saved:true,id:c.id});
}catch(e){return failure(e)}}
