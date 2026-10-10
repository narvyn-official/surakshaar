import {db,failure,json} from '@/lib/server';
import {env} from '@/lib/runtime-env';
import {referenceAccess} from '@/lib/safety/access';
import {equipmentCommand} from '@/lib/safety/search-model';
import {runEquipmentSearch,searchHistory} from '@/lib/safety/search';
import {configFrom} from '@/lib/safety/model';
import {CollectionError} from '@/lib/safety/provider';
import {displayError} from '@/lib/safety/messages';
const headers={'Cache-Control':'no-store'};
export async function GET(){try{
 const a=await referenceAccess(),config=configFrom(env as unknown as Record<string,unknown>);
 return Response.json({hosts:config.equipmentHosts,configured:!!config.key,operator:a.operator,searches:await searchHistory(db(),a.owner)},{headers});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const a=await referenceAccess(),c=equipmentCommand.safeParse(await json(request,2000));if(!c.success)throw new Error('Invalid equipment search. Use only a public make and model, select a manufacturer host and confirm the public-data notice.');
 const result=await runEquipmentSearch(db(),a,c.data,configFrom(env as unknown as Record<string,unknown>),async()=>{try{const next=await referenceAccess();return next.owner===a.owner&&next.user.userId===a.user.userId}catch{return false}});
 return Response.json(result,{headers});
}catch(e){if(e instanceof CollectionError)return Response.json({error:displayError(e.code)},{status:e.code==='budget'||e.code==='rate-limit'?429:503,headers});return failure(e)}}
