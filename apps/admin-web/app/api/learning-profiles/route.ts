import {getAppUser} from '@/lib/auth';
import {db,failure} from '@/lib/server';
export const dynamic='force-dynamic';
export async function GET(){try{const user=await getAppUser();if(!user)throw Error('Sign in to continue.');const rows=await db().prepare('SELECT h.worker_id AS workerId,COUNT(*) AS records,MAX(h.updated_at) AS updatedAt FROM learning_practice_heads h JOIN learning_profile_accounts p ON p.worker_id=h.worker_id AND p.account_id=h.account_id WHERE h.account_id=? GROUP BY h.worker_id ORDER BY updatedAt DESC LIMIT 50').bind(user.userId).all();return Response.json({profiles:rows.results},{headers:{'Cache-Control':'private, no-store'}});}catch(e){return failure(e)}}
