import {lookup} from 'node:dns/promises';
import {request} from 'node:https';
import {isPublicAddress,sourceUrl} from './url-policy';
import {CollectionError} from './provider';
type Address={address:string;family:number};
export type PreflightNetwork={resolve:(host:string)=>Promise<Address[]>;status:(url:URL,address:Address)=>Promise<number>};
const pinnedNetwork:PreflightNetwork={
 resolve:host=>lookup(host,{all:true,verbatim:true}),
 status:(url,address)=>new Promise((resolve,reject)=>{
  const req=request(url,{method:'GET',signal:AbortSignal.timeout(10000),headers:{'User-Agent':'SurakshaAR-SafetyUpdates-Pilot/1.0','Accept':'text/html,application/json,text/plain','Range':'bytes=0-0'},lookup:(_host,options,cb)=>{if(options.all)cb(null,[address]);else cb(null,address.address,address.family)}},response=>{
   const status=response.statusCode??0;response.destroy();resolve(status);
  });
  req.on('error',e=>reject(new CollectionError(e.name==='AbortError'?'timeout':'source-unavailable',true)));req.end();
 })
};
/** Only the Node worker fetches sources. Pin the validated IP; refuse every redirect.
 * TinyFish separately documents private-IP/redirect rejection. Final URL must equal our approved URL.
 */
export async function publicPreflight(raw:string,hosts:string[],network:PreflightNetwork=pinnedNetwork){
 let url:URL;try{url=new URL(sourceUrl(raw,hosts))}catch{throw new CollectionError('unsafe-url')}
 let addresses:Address[],timer:ReturnType<typeof setTimeout>|undefined;
 try{addresses=await Promise.race([network.resolve(url.hostname),new Promise<never>((_,reject)=>{timer=setTimeout(()=>reject(new CollectionError('timeout',true)),5000);timer.unref()})])}catch(e){if(e instanceof CollectionError)throw e;throw new CollectionError('source-unavailable',true)}finally{clearTimeout(timer)}
 if(!addresses.length||addresses.some(a=>!isPublicAddress(a.address)))throw new CollectionError('unsafe-url');
 const status=await network.status(url,addresses[0]);
 if(status>=300&&status<400)throw new CollectionError('unsafe-url');
 if(status<200||status>=300)throw new CollectionError('source-unavailable');
}
