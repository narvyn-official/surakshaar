import {z} from 'zod';
import {freshUser,getAppUser} from '@/lib/auth';
import {db,digest,failure,json} from '@/lib/server';
import {launchBudget} from '@/lib/learning-centre-store';
const command=z.discriminatedUnion('action',[
 z.object({action:z.literal('invite'),workerId:z.string().uuid(),label:z.string().trim().min(2).max(80),email:z.email().max(254),acknowledged:z.literal(true)}).strict(),
 z.object({action:z.literal('accept'),code:z.string().regex(/^[a-f0-9]{64}$/)}).strict(),
 z.object({action:z.enum(['revoke','leave']),id:z.string().uuid()}).strict(),
]);
const reply=(data:unknown)=>Response.json(data,{headers:{'Cache-Control':'private, no-store'}});
export async function GET(){try{
 const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const now=Date.now();
 const [profiles,invited,supporting]=await Promise.all([
 db().prepare('SELECT worker_id FROM learning_profile_accounts WHERE account_id=? LIMIT 30').bind(user.userId).all(),
 db().prepare('SELECT id,label,invited_email,supporter_id IS NOT NULL AS accepted,expires_at,revoked_at FROM learning_support_links WHERE account_id=? ORDER BY created_at DESC LIMIT 100').bind(user.userId).all(),
 db().prepare("SELECT l.id,l.label,l.expires_at,(SELECT COUNT(DISTINCT json_extract(p.payload,'$.scenarioId')) FROM learning_practice_heads p WHERE p.account_id=l.account_id AND p.worker_id=l.worker_id AND json_extract(p.payload,'$.finished')=1) AS completed_scenarios,(SELECT MAX(updated_at) FROM learning_practice_heads WHERE account_id=l.account_id AND worker_id=l.worker_id) AS last_practice_at FROM learning_support_links l JOIN learning_profile_accounts a ON a.worker_id=l.worker_id AND a.account_id=l.account_id WHERE l.supporter_id=? AND l.revoked_at IS NULL AND l.expires_at>? ORDER BY l.label LIMIT 100").bind(user.userId,now).all()]);
 return reply({now,profiles:profiles.results,invited:invited.results,supporting:supporting.results,scope:'Read-only personal learning summary. No exam photos, answers, private reflections or certificate authority.'});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const user=await freshUser(),parsed=command.safeParse(await json(request));if(!parsed.success)throw Error('Invalid learning-support request.');const c=parsed.data,now=Date.now();await launchBudget(user.userId);
 if(c.action==='invite'){
  if(c.email.toLowerCase()===user.email.toLowerCase())throw Error('Invalid invitation: choose another person.');
  if(!await db().prepare('SELECT 1 FROM learning_profile_accounts WHERE worker_id=? AND account_id=?').bind(c.workerId,user.userId).first())throw Error('Forbidden: this personal learning profile is not yours.');
  const count=await db().prepare('SELECT COUNT(*) n FROM learning_support_links WHERE account_id=? AND revoked_at IS NULL AND expires_at>?').bind(user.userId,now).first<{n:number}>();if((count?.n??0)>=10)throw Error('Invalid invitation: revoke an old support link before adding another.');
  const code=crypto.randomUUID().replaceAll('-','')+crypto.randomUUID().replaceAll('-',''),id=crypto.randomUUID();await db().prepare('INSERT INTO learning_support_links VALUES(?,?,?,?,?,NULL,?,?,?,?,NULL)').bind(id,user.userId,c.workerId,c.label,c.email.toLowerCase(),await digest(code),now+7*86400000,now+90*86400000,now).run();return reply({saved:true,code,expiresAt:now+7*86400000});
 }
 if(c.action==='accept'){
  const result=await db().prepare('UPDATE learning_support_links SET supporter_id=?,invite_hash=NULL,invite_expires_at=NULL WHERE invite_hash=? AND invited_email=? AND invite_expires_at>? AND revoked_at IS NULL AND supporter_id IS NULL AND account_id<>?').bind(user.userId,await digest(c.code),user.email.toLowerCase(),now,user.userId).run();if(!result.meta.changes)throw Error('Invalid or expired invitation. Use the invited account or request a new code.');return reply({accepted:true});
 }
 const result=await db().prepare('UPDATE learning_support_links SET revoked_at=?,invite_hash=NULL WHERE id=? AND '+(c.action==='leave'?'supporter_id':'account_id')+'=? AND revoked_at IS NULL').bind(now,c.id,user.userId).run();if(!result.meta.changes)throw Error('No active support link found.');return reply({revoked:true});
}catch(e){return failure(e)}}
