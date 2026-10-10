import {env} from '@/lib/runtime-env';
import {z} from 'zod';
import {learnerMembership} from '@/lib/learner';
import {failure,json} from '@/lib/server';
import {consumeTutorBudget} from '@/lib/tutor-budget';
import {httpProvider,tutor} from '@/lib/tutor';
const requestSchema=z.object({workerId:z.string().uuid(),question:z.string().trim().min(3).max(500),language:z.enum(['en','hi'])}).strict();

export async function POST(request:Request){try{
 const input=requestSchema.parse(await json(request,4096));
 const access=await learnerMembership(input.workerId);
 const limited=await consumeTutorBudget(access.user.userId);if(limited)return limited;
 const values=env as unknown as Record<string,string|undefined>;
 // Operator configuration is optional. No request/record/camera/audio telemetry is stored.
 const provider=values.TUTOR_PROVIDER_URL&&values.TUTOR_PROVIDER_KEY?httpProvider(values.TUTOR_PROVIDER_URL,values.TUTOR_PROVIDER_KEY):undefined;
 return Response.json(await tutor(input.question,input.language,provider),{headers:{'Cache-Control':'no-store'}});
}catch(e){return failure(e)}}
