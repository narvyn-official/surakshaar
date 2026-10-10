import {z} from 'zod';
import {db,digest,json,failure} from '@/lib/server';
import {shareToken,publicLearningShare} from '@/lib/learning-record-share';
export async function POST(request:Request){try{
 const parsed=z.object({token:shareToken}).strict().safeParse(await json(request,1024));if(!parsed.success)throw new Error('Invalid practice share token.');
 const hash=await digest({purpose:'formative-public-share-v1',token:parsed.data.token});
 const row=await db().prepare('SELECT metadata,created_at,expires_at,revoked_at FROM learning_record_shares WHERE token_hash=?').bind(hash).first<{metadata:string;created_at:number;expires_at:number;revoked_at:number|null}>();
 return Response.json(publicLearningShare(row),{headers:{'Cache-Control':'no-store','X-Robots-Tag':'noindex','Referrer-Policy':'no-referrer'}});
}catch(error){return failure(error)}}
