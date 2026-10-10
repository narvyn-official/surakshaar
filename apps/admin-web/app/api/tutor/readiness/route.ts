import {env} from '@/lib/runtime-env';
import {access,failure} from '@/lib/server';
import {contextCoachRegistry} from '@/lib/context-coach-registry';
import {handleCoachReadiness} from '@/lib/coach-readiness';
import {consumeTutorBudget} from '@/lib/tutor-budget';
async function handle(request:Request){try{return await handleCoachReadiness(request,{
 registry:contextCoachRegistry,
 async authorizeAdmin(){const a=await access('admin');return {accountId:a.user.userId};},
 values:()=>env as unknown as Record<string,string|undefined>,budget:consumeTutorBudget,
})}catch(error){return failure(error)}}
export const GET=handle;
export const POST=handle;
