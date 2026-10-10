import {z} from 'zod';
import {centreId} from '@/lib/learner-scope';
import {db,json} from '@/lib/server';
import {authorisedInbox,inboxRows,inboxScope,inboxItemId} from '@/lib/learning-inbox';
import {launchFailure,launchResponse} from '@/lib/learning-centre-store';

const query=z.object({scope:z.enum(['learner','educator']),workerId:z.string().uuid().optional(),owner:centreId.optional(),showDismissed:z.enum(['true','false']).default('false')}).strict();
const command=z.object({scope:z.enum(['learner','educator']),workerId:z.string().uuid().optional(),owner:centreId.optional(),itemId:inboxItemId,action:z.enum(['read','unread','dismiss','restore','open'])}).strict();
export async function GET(request:Request){try{
 const parsed=query.safeParse(Object.fromEntries(new URL(request.url).searchParams));if(!parsed.success||!inboxScope.safeParse(parsed.data).success)throw new Error('Invalid inbox filter.');const q=parsed.data,c=await authorisedInbox(q.scope,q.workerId,q.owner),rows=await inboxRows(c,undefined,q.showDismissed==='true'),items=rows.slice(0,100);
 return launchResponse({items,loadedUnread:items.filter(v=>v.read_at===null&&v.dismissed_at===null).length,truncated:rows.length>100,scope:c.scope,owner:c.owner,channel:'in-app only',asOf:Date.now()});
}catch(error){return launchFailure(error)}}
export async function POST(request:Request){try{
 const parsed=command.safeParse(await json(request,16000));if(!parsed.success||!inboxScope.safeParse(parsed.data).success)throw new Error('Invalid inbox action.');const q=parsed.data,c=await authorisedInbox(q.scope,q.workerId,q.owner),item=(await inboxRows(c,q.itemId,true))[0];if(!item)throw new Error('No notification available. Its object or your access may have changed.');
 const now=Date.now(),read=q.action==='read'||q.action==='open'?now:q.action==='unread'?null:item.read_at,dismissed=q.action==='dismiss'?now:q.action==='restore'?null:item.dismissed_at;
 await db().prepare('INSERT INTO learning_inbox_state VALUES(?,?,?,?,?) ON CONFLICT(account_id,item_id) DO UPDATE SET read_at=excluded.read_at,dismissed_at=excluded.dismissed_at,updated_at=excluded.updated_at').bind(c.accountId,c.owner+':'+item.id,read,dismissed,now).run();
 return launchResponse({saved:true,readAt:read,dismissedAt:dismissed,...(q.action==='open'?{destination:{screen:c.scope==='learner'?'learner-assignment':'educator-assignment',assignmentId:item.assignment_id,workerId:item.worker_id,owner:c.owner}}:{})});
}catch(error){return launchFailure(error)}}
