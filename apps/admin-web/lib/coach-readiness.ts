import {z} from 'zod';
import {readJson} from './http';
import {contextCoach,type CoachRegistry,type ProviderConfiguration} from './context-coach';
import {configuredContextCoach} from './context-coach-provider';
const required=['COACH_PROVIDER_URL','COACH_PROVIDER_KEY','COACH_PROVIDER_NAME','COACH_PROVIDER_PRIVACY_URL','COACH_PROVIDER_DATA_USE'] as const;
export function coachReadiness(values:Record<string,string|undefined>,registry:CoachRegistry) {
 const configuration=configuredContextCoach(values);
 return {
  schemaVersion:1,configuration:configuration.status,connection:'not-tested',
  missingFields:required.filter(key=>!values[key]?.trim()),
  disclosure:configuration.status==='configured'?configuration.disclosure??null:null,
  contentVersion:registry.version,
  scenarioCount:registry.scenarios.length,
  reviewedScenarios:registry.scenarios.filter(s=>s.reviewStatus==='approved'&&s.reviewer?.trim()).length,
  safetyCoaching:'requires-approved-scenario-and-source',
  capabilities:['authored-control-card-selection'],languages:['en','hi'],
  unsupported:['clinical-diagnosis','live-emergency-guidance','assessment-scoring','certification','unreviewed-safety-instructions','santali-coaching'],
  credentialValuesExposed:false,productionReady:false,
 };
}
export async function probeCoach(configuration:ProviderConfiguration,registry:CoachRegistry,signal:AbortSignal) {
 const scenario=registry.scenarios.find(s=>s.steps.length),step=scenario?.steps[0];
 if(!scenario||!step)throw new Error('No authored control context is available.');
 const started=performance.now();
 const reply=await contextCoach({courseId:scenario.courseId,contentVersion:registry.version,scenarioId:scenario.id,objectiveId:step.id,recentActionIds:[],hintLevel:'controls',language:'en',mode:'learn',allowExternalCoaching:true},registry,configuration,signal);
 return {status:reply.aiStatus==='selected'?'passed':'unavailable',providerStatus:reply.aiStatus,elapsedMs:Math.round(performance.now()-started),scope:'synthetic-controls-only',learnerDataSent:false,safetyApproval:false,productionReady:false};
}
type Dependencies={registry:CoachRegistry;authorizeAdmin():Promise<{accountId:string}>;values():Record<string,string|undefined>;budget(accountId:string):Promise<Response|null>;provider?(values:Record<string,string|undefined>):ProviderConfiguration};
/** Readiness never contacts a provider; an explicitly confirmed probe uses no learner data. */
export async function handleCoachReadiness(request:Request,deps:Dependencies) {
 const account=await deps.authorizeAdmin();
 if(request.method==='GET')return Response.json(coachReadiness(deps.values(),deps.registry),{headers:{'Cache-Control':'no-store'}});
 if(request.method!=='POST')return new Response(null,{status:405,headers:{Allow:'GET, POST'}});
 const command=z.object({action:z.literal('probe'),confirmExternalRequest:z.literal(true)}).strict().safeParse(await readJson(request,1024));
 if(!command.success)throw new Error('Invalid readiness probe. Confirm the synthetic external request.');
 const limited=await deps.budget(account.accountId);if(limited)return limited;
 const values=deps.values(),configuration=(deps.provider??configuredContextCoach)(values);
 return Response.json(await probeCoach(configuration,deps.registry,request.signal),{headers:{'Cache-Control':'no-store'}});
}
