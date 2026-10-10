import {z} from 'zod';
import {access,audit,db,digest,failure,json} from '@/lib/server';
import {getAppUser,freshUser} from '@/lib/auth';
import {learnerAccess,learnerLinks} from '@/lib/learner';
import {encryptEvidence,decryptEvidence,identityReady,nextCapture,validateJpeg} from '@/lib/identity-evidence';
import {practiceAction,startPractice} from '@/lib/studio-learning';
import {sharedPractice} from '@/lib/learning-centre';
import {learningCanonical} from '@/lib/learning-practice';
import {env} from '@/lib/runtime-env';
import type {LearningPractice} from '@/lib/learning-practice';
const secret=()=>String(env.IDENTITY_EVIDENCE_KEY??'');
const uuid=z.string().uuid(),text=z.string().trim().min(1).max(500);
const command=z.discriminatedUnion('action',[
 z.object({action:z.literal('policy'),enabled:z.boolean(),notice:z.string().trim().min(80).max(4000),support:text,retentionDays:z.number().int().min(1).max(30)}).strict(),
 z.object({action:z.literal('start'),owner:text,workerId:uuid,assignmentId:uuid,scenarioId:text,consent:z.literal(true),adult:z.literal(true)}).strict(),
 z.object({action:z.literal('frame'),id:uuid,challenge:uuid,jpeg:z.string().max(300000),clientAt:z.number().int().nonnegative()}).strict(),
 z.object({action:z.enum(['decision','continue','hint','observation']),id:uuid,revision:z.number().int().nonnegative(),choiceId:text.optional()}).strict(),
 z.object({action:z.enum(['interrupt','withdraw']),id:uuid}).strict(),
 z.object({action:z.literal('appeal'),id:uuid,note:z.string().trim().min(10).max(1000)}).strict(),
 z.object({action:z.literal('review'),id:uuid,outcome:z.enum(['accepted','needs-review']),note:z.string().trim().min(20).max(1000),referenceChecked:z.literal(true)}).strict(),
]);
type Session={id:string;owner:string;worker_id:string;account_id:string;assignment_id:string;scenario_id:string;status:string;practice:string;revision:number;challenge:string|null;challenge_at:number;frames:number;started_at:number;updated_at:number;expires_at:number;review_note:string|null;reviewed_by:string|null;appeal:string|null;notice:string;support:string};
function respond(value:unknown,status=200){return Response.json(value,{status,headers:{'Cache-Control':'private, no-store'}})}
async function purge(){await db().prepare('DELETE FROM identity_sessions WHERE expires_at<=?').bind(Date.now()).run();}
async function ownSession(id:string){const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const session=await db().prepare('SELECT * FROM identity_sessions WHERE id=? AND account_id=? AND expires_at>?').bind(id,user.userId,Date.now()).first<Session>();if(!session)throw Error('No identity session found.');await learnerAccess(session.worker_id,session.owner);return session;}
function view(s:Session){const p=JSON.parse(s.practice) as LearningPractice;return {id:s.id,owner:s.owner,workerId:s.worker_id,scenarioId:s.scenario_id,status:s.status,practice:p,revision:s.revision,challenge:s.challenge,captureAt:s.challenge_at,frameCount:s.frames,expiresAt:s.expires_at,reviewNote:s.review_note,appeal:s.appeal,notice:s.notice,support:s.support,certifiable:false};}
export async function GET(request:Request){try{
 await purge();const q=new URL(request.url).searchParams;
 if(q.get('staff')==='1'){
  const a=await access();if(a.role==='viewer')throw Error('Forbidden: identity evidence requires an examiner role.');
  if(q.has('frame')){const frame=await db().prepare('SELECT f.*,s.id AS sid FROM identity_frames f JOIN identity_sessions s ON s.id=f.session_id WHERE f.id=? AND s.owner=? AND s.expires_at>? AND s.status<>?').bind(q.get('frame'),a.owner,Date.now(),'withdrawn').first<{id:string;sid:string;ciphertext:string}>();if(!frame)throw Error('No evidence found.');await audit(a,'identity.evidence-view',frame.sid).run();const bytes=decryptEvidence(frame.ciphertext,frame.sid+':'+frame.id,secret());return new Response(new Uint8Array(bytes),{headers:{'Content-Type':'image/jpeg','Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff'}});}
  const policy=await db().prepare('SELECT * FROM identity_policies WHERE owner=?').bind(a.owner).first();
  const sessions=await db().prepare("SELECT s.id,s.worker_id,w.name,s.scenario_id,s.status,s.frames,s.started_at,s.expires_at,s.review_note,s.appeal,s.reviewed_by FROM identity_sessions s JOIN workers w ON w.owner=s.owner AND w.id=s.worker_id WHERE s.owner=? ORDER BY s.started_at DESC LIMIT 100").bind(a.owner).all();
  const frames=q.has('id')?await db().prepare('SELECT f.id,f.sequence,f.received_at,f.client_at FROM identity_frames f JOIN identity_sessions s ON s.id=f.session_id WHERE s.owner=? AND s.id=? AND s.status<>? ORDER BY f.sequence').bind(a.owner,q.get('id'),'withdrawn').all():{results:[]};
  return respond({policy,sessions:sessions.results,frames:frames.results,canConfigure:a.role==='admin',configured:identityReady(secret())});
 }
 if(q.has('id'))return respond(view(await ownSession(q.get('id')!)));
 const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const links=await learnerLinks(user.userId),assignments=[];
 for(const link of links){const result=await db().prepare("SELECT a.id,a.owner,a.worker_id,a.scenario_ids,a.note,p.notice,p.support,p.retention_days,p.enabled FROM learning_course_assignments a JOIN identity_policies p ON p.owner=a.owner WHERE a.owner=? AND a.worker_id=? AND a.cancelled_at IS NULL AND a.audience='adult-learning' AND p.enabled=1 ORDER BY a.created_at DESC LIMIT 30").bind(link.owner,link.worker_id).all();assignments.push(...result.results.map(row=>({...row,centre:link.centre,learner:link.name})));}
 const sessions=await db().prepare('SELECT id,scenario_id,status,started_at,review_note,expires_at FROM identity_sessions WHERE account_id=? ORDER BY started_at DESC LIMIT 50').bind(user.userId).all();
 return respond({assignments,sessions:sessions.results,configured:identityReady(secret())});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const parsed=command.safeParse(await json(request,330000));if(!parsed.success)throw Error('Invalid identity request.');const c=parsed.data,now=Date.now();await purge();
 if(c.action==='policy'){
  const a=await access('admin');await freshUser();if(c.enabled&&!identityReady(secret()))throw Error('Training identity storage is not configured.');
  await db().batch([db().prepare('INSERT INTO identity_policies VALUES(?,?,?,?,?,1,?,?) ON CONFLICT(owner) DO UPDATE SET enabled=excluded.enabled,notice=excluded.notice,support=excluded.support,retention_days=excluded.retention_days,revision=identity_policies.revision+1,updated_at=excluded.updated_at,updated_by=excluded.updated_by').bind(a.owner,c.enabled?1:0,c.notice,c.support,c.retentionDays,now,a.user.userId),audit(a,'identity.policy',a.owner,{enabled:c.enabled,retentionDays:c.retentionDays})]);return respond({saved:true});
 }
 if(c.action==='review'){
  const a=await access();await freshUser();if(a.role==='viewer')throw Error('Forbidden examiner role.');
  const s=await db().prepare('SELECT * FROM identity_sessions WHERE id=? AND owner=?').bind(c.id,a.owner).first<Session>();if(!s)throw Error('No identity session found.');if(s.account_id===a.user.userId||s.reviewed_by===a.user.userId&&s.appeal)throw Error('Forbidden: use an independent examiner for self-review or an appeal.');if(!['submitted','accepted','needs-review'].includes(s.status))throw Error('Invalid review: session must be submitted.');
  const reviewed=await db().batch([db().prepare('UPDATE identity_sessions SET status=?,reviewed_by=?,reviewed_at=?,review_note=?,appeal=NULL,revision=revision+1 WHERE id=? AND owner=? AND revision=?').bind(c.outcome,a.user.userId,now,c.note,s.id,a.owner,s.revision),audit(a,'identity.review',s.id,{outcome:c.outcome,note:c.note,referenceChecked:true},true)]);if(!reviewed[0].meta.changes)throw Error('Record conflict: another reviewer updated this session.');return respond({saved:true,certifiable:false});
 }
 if(c.action==='start'){
  if(!identityReady(secret()))throw Error('Training identity storage is not configured.');const a=await learnerAccess(c.workerId,c.owner);
  const assignment=await db().prepare("SELECT * FROM learning_course_assignments WHERE owner=? AND worker_id=? AND id=? AND audience='adult-learning' AND cancelled_at IS NULL").bind(a.owner,c.workerId,c.assignmentId).first<{scenario_ids:string;mode:string;content_version:string}>();
  const policy=await db().prepare('SELECT * FROM identity_policies WHERE owner=? AND enabled=1').bind(a.owner).first<{notice:string;support:string;revision:number;retention_days:number}>();if(!assignment||!policy||!JSON.parse(assignment.scenario_ids).includes(c.scenarioId))throw Error('Forbidden assignment or disabled identity policy.');
  const exists=await db().prepare("SELECT COUNT(*) AS n FROM identity_sessions WHERE account_id=? AND started_at>?").bind(a.user.userId,now-86400000).first<{n:number}>();if((exists?.n??0)>=10)throw Error('Training identity session limit reached. Retry tomorrow.');
  const id=crypto.randomUUID(),practice=startPractice(c.scenarioId,c.workerId,assignment.mode==='guided',now);if(practice.contentVersion!==assignment.content_version)throw Error('Unsupported assignment content version. Ask your trainer for a current assignment.');
  await db().prepare("INSERT INTO identity_sessions(id,owner,worker_id,account_id,assignment_id,scenario_id,policy_revision,notice,support,status,practice,challenge,challenge_at,started_at,updated_at,expires_at) VALUES(?,?,?,?,?,?,?,?,?,'active',?,?,?,?,?,?)").bind(id,a.owner,c.workerId,a.user.userId,c.assignmentId,c.scenarioId,policy.revision,policy.notice,policy.support,JSON.stringify(practice),crypto.randomUUID(),now,now,now,now+policy.retention_days*86400000).run();
  return respond(view(await ownSession(id)));
 }
 const s=await ownSession(c.id);
 if(c.action==='withdraw'){await db().batch([db().prepare("UPDATE identity_sessions SET status='withdrawn',challenge=NULL,revision=revision+1,updated_at=? WHERE id=?").bind(now,s.id),db().prepare('DELETE FROM identity_frames WHERE session_id=?').bind(s.id)]);return respond({withdrawn:true});}
 if(c.action==='appeal'){if(!['accepted','needs-review'].includes(s.status))throw Error('Invalid appeal: a review decision is required.');await db().prepare("UPDATE identity_sessions SET appeal=?,status='needs-review',revision=revision+1,updated_at=? WHERE id=?").bind(c.note,now,s.id).run();return respond(view(await ownSession(s.id)));}
 if(c.action==='interrupt'){await db().prepare("UPDATE identity_sessions SET status='interrupted',challenge=NULL,revision=revision+1,updated_at=? WHERE id=? AND status='active'").bind(now,s.id).run();return respond({interrupted:true});}
 if(s.status!=='active')throw Error('Invalid session: start a new practice.');
 const enabled=await db().prepare('SELECT 1 FROM identity_policies WHERE owner=? AND enabled=1').bind(s.owner).first();if(!enabled)throw Error('Forbidden: the institution disabled identity sessions.');
 if(now-s.started_at>45*60000||now>s.challenge_at+60000){await db().prepare("UPDATE identity_sessions SET status='interrupted',challenge=NULL,revision=revision+1 WHERE id=? AND status='active'").bind(s.id).run();throw Error('Invalid session: camera checkpoint expired. Restart when ready.');}
 if(c.action==='frame'){
  if(s.challenge!==c.challenge||now<s.challenge_at-1500||s.frames>=60)throw Error('Invalid camera checkpoint.');const photo=validateJpeg(c.jpeg),id=crypto.randomUUID(),encrypted=encryptEvidence(photo,s.id+':'+id,secret()),challenge=crypto.randomUUID(),captureAt=nextCapture(now);
  const result=await db().batch([db().prepare("INSERT INTO identity_frames SELECT ?,id,frames+1,?,?,? FROM identity_sessions WHERE id=? AND status='active' AND challenge=? AND revision=?").bind(id,now,c.clientAt,encrypted,s.id,c.challenge,s.revision),db().prepare('UPDATE identity_sessions SET frames=frames+1,challenge=?,challenge_at=?,updated_at=? WHERE id=? AND challenge=? AND changes()>0').bind(challenge,captureAt,now,s.id,c.challenge)]);
  if(!result[0].meta.changes)throw Error('Record conflict: camera checkpoint was already used.');return respond(view(await ownSession(s.id)));
 }
 if(!('revision' in c))throw Error('Invalid identity action.');
 if(s.frames<1)throw Error('Invalid session: capture the entry selfie first.');if(c.revision!==s.revision)throw Error('Record conflict: reload your current practice.');
 const action=c.action==='decision'?{kind:'decision' as const,choiceId:c.choiceId??''}:{kind:c.action as 'continue'|'hint'|'observation'};
 const practice=practiceAction(JSON.parse(s.practice),action,now),status=practice.finished?'submitted':'active';
 const statements=[db().prepare("UPDATE identity_sessions SET practice=?,status=?,revision=revision+1,updated_at=? WHERE id=? AND status='active' AND revision=?").bind(JSON.stringify(practice),status,now,s.id,s.revision)];
 if(practice.finished){
  const hash=await digest(practice),submissionId=await digest({owner:s.owner,assignment:s.assignment_id,attemptId:practice.id,sourceDigest:hash});
  statements.push(db().prepare('INSERT INTO learning_profile_accounts(worker_id,account_id,bound_at) VALUES(?,?,?) ON CONFLICT(worker_id) DO NOTHING').bind(s.worker_id,s.account_id,now));
  statements.push(db().prepare("INSERT INTO learning_practice_snapshots(account_id,id,worker_id,course_id,content_version,digest,previous_digest,updated_at,imported_at,payload) SELECT ?,?,?,?,?,?,NULL,?,?,? WHERE EXISTS(SELECT 1 FROM identity_sessions WHERE id=? AND status='submitted' AND revision=?) ON CONFLICT(account_id,id,digest) DO NOTHING").bind(s.account_id,practice.id,s.worker_id,practice.courseId,practice.contentVersion,hash,practice.updatedAt,now,learningCanonical(practice),s.id,s.revision+1));
  statements.push(db().prepare("INSERT INTO learning_course_submissions SELECT ?,?,?,?,?,?,?,?,?,?,NULL WHERE EXISTS(SELECT 1 FROM identity_sessions WHERE id=? AND status='submitted' AND revision=?) ON CONFLICT(owner,id) DO NOTHING").bind(s.owner,submissionId,s.assignment_id,s.worker_id,s.account_id,practice.id,hash,practice.scenarioId,JSON.stringify(sharedPractice(practice)),now,s.id,s.revision+1));
 }
 const result=await db().batch(statements);if(!result[0].meta.changes)throw Error('Record conflict: reload your current practice.');return respond(view(await ownSession(s.id)));
}catch(e){return failure(e)}}
