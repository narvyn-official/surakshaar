import {z} from 'zod';
import {getAppUser} from '@/lib/auth';
import {db,digest,json} from '@/lib/server';
import {launchBudget,launchFailure,launchResponse} from '@/lib/learning-centre-store';
import {validateLearningPractice} from '@/lib/learning-practice';
import {shareCommand,newShareToken,learningShareMetadata} from '@/lib/learning-record-share';
async function account(){const user=await getAppUser();if(!user)throw new Error('Sign in to manage practice sharing.');return user.userId}
async function source(accountId:string,workerId:string,attemptId:string){
 const row=await db().prepare('SELECT h.payload,h.digest FROM learning_practice_heads h JOIN learning_profile_accounts p ON p.worker_id=h.worker_id AND p.account_id=h.account_id WHERE h.account_id=? AND h.worker_id=? AND h.id=? AND NOT EXISTS(SELECT 1 FROM learner_links l WHERE l.worker_id=h.worker_id AND l.user_id IS NOT NULL AND l.user_id<>h.account_id)').bind(accountId,workerId,attemptId).first<{payload:string;digest:string}>();
 if(!row)throw new Error('Forbidden: no private practice copy exists in this account.');return row;
}
export async function GET(request:Request){try{
 const accountId=await account(),q=z.object({workerId:z.string().uuid(),attemptId:z.string().uuid()}).strict().parse(Object.fromEntries(new URL(request.url).searchParams));await source(accountId,q.workerId,q.attemptId);
 const result=await db().prepare('SELECT id,created_at,expires_at,revoked_at FROM learning_record_shares WHERE account_id=? AND worker_id=? AND attempt_id=? ORDER BY (revoked_at IS NULL AND expires_at>?) DESC,created_at DESC,id DESC LIMIT 51').bind(accountId,q.workerId,q.attemptId,Date.now()).all();
 return launchResponse({shares:result.results.slice(0,50),truncated:result.results.length>50,certifiable:false});
}catch(error){return launchFailure(error)}}
export async function POST(request:Request){try{
 const accountId=await account(),parsed=shareCommand.safeParse(await json(request,4096));if(!parsed.success)throw new Error('Invalid practice sharing command.');const c=parsed.data,now=Date.now();
 // Revocation is not rate limited: a spent creation quota must never trap an active share.
 if(c.action==='revoke'){
  const row=await db().prepare('SELECT id FROM learning_record_shares WHERE account_id=? AND id=? AND worker_id=? AND attempt_id=?').bind(accountId,c.id,c.workerId,c.attemptId).first();if(!row)throw new Error('Forbidden practice share.');
  await db().prepare('UPDATE learning_record_shares SET revoked_at=? WHERE account_id=? AND id=? AND revoked_at IS NULL').bind(now,accountId,c.id).run();return launchResponse({revoked:true,certifiable:false});
 }
 await launchBudget(accountId);
 const existing=await db().prepare('SELECT id FROM learning_record_shares WHERE account_id=? AND id=?').bind(accountId,c.id).first();if(existing)throw new Error('Record conflict: this share was already created. Its secret link is shown once. Review active shares, then revoke it before creating a replacement.');
 const row=await source(accountId,c.workerId,c.attemptId),record=validateLearningPractice(JSON.parse(row.payload),c.workerId),metadata=learningShareMetadata(record);
 const token=newShareToken(),hash=await digest({purpose:'formative-public-share-v1',token}),expires=now+c.expiresInDays*86400000;
 await db().batch([
  db().prepare('DELETE FROM learning_record_shares WHERE account_id=? AND (expires_at<? OR revoked_at<?)').bind(accountId,now-30*86400000,now-30*86400000),
  db().prepare('INSERT INTO learning_record_shares VALUES(?,?,?,?,?,?,?,?,?,NULL)').bind(accountId,c.id,c.workerId,c.attemptId,row.digest,hash,JSON.stringify(metadata),now,expires),
 ]);
 return launchResponse({id:c.id,path:'/practice-record#token='+token,expiresAt:expires,record:metadata,tokenShownOnce:true,certifiable:false},201);
}catch(error){const message=error instanceof Error?error.message:'';if(message.includes('UNIQUE constraint failed: learning_record_shares.account_id, learning_record_shares.id'))return launchResponse({error:'Record conflict: this share request already exists. Refresh the share list before retrying.'},409);if(message.includes('learning-share-limit'))return launchResponse({error:'Training share limit reached. Revoke unused links before creating another.'},409);if(message.includes('learning-share-source-conflict'))return launchResponse({error:'Forbidden: practice ownership changed.'},403);return launchFailure(error)}}
