import {learnerScope,requireWorkerScope,scopeAssertion} from '@/lib/staff-scope';
import {z} from 'zod';
import {access,audit,db,digest,failure,json} from '@/lib/server';
import {freshUser,getAppUser} from '@/lib/auth';
import {learnerAccess,learnerLinks} from '@/lib/learner';
import {identityReady,validateJpeg,encryptEvidence,decryptEvidence} from '@/lib/identity-evidence';
import {env} from '@/lib/runtime-env';
import {launchBudget} from '@/lib/learning-centre-store';
const secret=()=>String(env.IDENTITY_EVIDENCE_KEY??'');
const command=z.discriminatedUnion('action',[
 z.object({action:z.literal('submit'),owner:z.string().max(300),workerId:z.string().uuid(),jpeg:z.string().max(300000),adult:z.literal(true),acknowledged:z.literal(true)}).strict(),
 z.object({action:z.literal('review'),id:z.string().uuid(),outcome:z.enum(['approved','rejected']),note:z.string().trim().min(20).max(1000),attendedCheck:z.literal(true)}).strict(),
 z.object({action:z.literal('withdraw'),id:z.string().uuid()}).strict(),
]);
const reply=(data:unknown)=>Response.json(data,{headers:{'Cache-Control':'private, no-store'}});
export async function GET(request:Request){try{
 const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const q=new URL(request.url).searchParams;
 if(q.get('staff')==='1'){
  const a=await access('workspace',true);if(!['admin','examiner','assessor'].includes(a.role))throw Error('Forbidden: reference approval requires an enrolment administrator or examiner.');
  if(q.has('image')){
   const row=await db().prepare("SELECT id,ciphertext,worker_id FROM identity_references WHERE owner=? AND id=? AND status NOT IN('withdrawn','rejected') AND expires_at>?").bind(a.owner,q.get('image'),Date.now()).first<{id:string;ciphertext:string;worker_id:string}>();if(!row)throw Error('No reference image found.');await requireWorkerScope(a,row.worker_id);await audit(a,'identity.reference-view',row.id).run();return new Response(new Uint8Array(decryptEvidence(row.ciphertext,a.owner+':reference:'+row.id,secret())),{headers:{'Content-Type':'image/jpeg','Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff'}});
  }
  const scope=learnerScope(a,'r.worker_id');
  const rows=await db().prepare('SELECT r.id,r.worker_id,r.status,r.created_at,r.expires_at,r.review_note,w.name FROM identity_references r JOIN workers w ON w.owner=r.owner AND w.id=r.worker_id WHERE r.owner=? AND r.expires_at>? AND '+scope.sql+' ORDER BY r.created_at DESC LIMIT 100').bind(a.owner,Date.now(),...scope.args).all();return reply({staff:true,references:rows.results,configured:identityReady(secret())});
 }
 const links=await learnerLinks(user.userId),rows=await db().prepare('SELECT owner,id,worker_id,status,created_at,expires_at,review_note FROM identity_references WHERE account_id=? AND expires_at>? ORDER BY created_at DESC LIMIT 50').bind(user.userId,Date.now()).all();return reply({staff:false,links,references:rows.results,configured:identityReady(secret())});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const parsed=command.safeParse(await json(request,330000));if(!parsed.success)throw Error('Invalid reference identity request.');const c=parsed.data,now=Date.now();
 if(c.action==='submit'){
  const a=await learnerAccess(c.workerId,c.owner);await launchBudget(a.user.userId);if(!identityReady(secret()))throw Error('Training identity storage is not configured.');
  if(!await db().prepare('SELECT 1 FROM identity_policies WHERE owner=? AND enabled=1').bind(a.owner).first())throw Error('Forbidden: the institution has not enabled an identity notice.');
  const count=await db().prepare('SELECT COUNT(*) n FROM identity_references WHERE account_id=? AND created_at>?').bind(a.user.userId,now-86400000).first<{n:number}>();if((count?.n??0)>=3)throw Error('Training reference limit reached. Contact your examiner or retry tomorrow.');
  const bytes=validateJpeg(c.jpeg),id=crypto.randomUUID(),cipher=encryptEvidence(bytes,a.owner+':reference:'+id,secret()),hash=await digest(c.jpeg);
  await db().prepare("INSERT INTO identity_references(owner,id,worker_id,account_id,ciphertext,digest,status,created_at,expires_at) VALUES(?,?,?,?,?,?,'pending',?,?)").bind(a.owner,id,c.workerId,a.user.userId,cipher,hash,now,now+30*86400000).run();return reply({saved:true,id,status:'pending'});
 }
 if(c.action==='withdraw'){
  const user=await freshUser();await db().prepare("UPDATE identity_references SET status='withdrawn',ciphertext='' WHERE id=? AND account_id=? AND status IN('approved','pending','replaced')").bind(c.id,user.userId).run();return reply({withdrawn:true});
 }
 const a=await access('workspace',true);await freshUser();if(!['admin','examiner','assessor'].includes(a.role))throw Error('Forbidden reference approval role.');
 const row=await db().prepare("SELECT * FROM identity_references WHERE owner=? AND id=? AND status='pending' AND expires_at>?").bind(a.owner,c.id,now).first<{worker_id:string;account_id:string}>();if(!row)throw Error('No pending reference found.');await requireWorkerScope(a,row.worker_id);if(row.account_id===a.user.userId)throw Error('Forbidden: you cannot approve your own reference.');
 if(!await db().prepare('SELECT 1 FROM learner_links WHERE owner=? AND worker_id=? AND user_id=?').bind(a.owner,row.worker_id,row.account_id).first())throw Error('Forbidden: learner enrolment changed.');
 const statements:D1PreparedStatement[]=[scopeAssertion(a,row.worker_id)];
 if(c.outcome==='approved')statements.push(db().prepare("UPDATE identity_references SET status='replaced' WHERE owner=? AND worker_id=? AND status='approved' AND EXISTS(SELECT 1 FROM identity_references WHERE owner=? AND id=? AND status='pending')").bind(a.owner,row.worker_id,a.owner,c.id));
 statements.push(db().prepare("UPDATE identity_references SET status=?,reviewed_by=?,reviewed_at=?,review_note=?,expires_at=?,ciphertext=CASE WHEN ?='rejected' THEN '' ELSE ciphertext END WHERE owner=? AND id=? AND status='pending'").bind(c.outcome,a.user.userId,now,c.note,now+(c.outcome==='approved'?90:7)*86400000,c.outcome,a.owner,c.id),audit(a,'identity.reference-'+c.outcome,c.id,{attendedCheck:true},true));
 await db().batch(statements);return reply({saved:true});
}catch(e){return failure(e)}}
