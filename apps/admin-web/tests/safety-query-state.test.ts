import test from 'node:test';
import assert from 'node:assert/strict';
import {SafetyQueryLoader,visibleSafetyData,type SafetyQuery,type SafetySnapshot} from '../lib/safety/query-state';

// Local transport fixtures only. No TinyFish or application HTTP requests are made.
type Rows={updates:string[];counts:{new:number};nextCursor:string|null};
const all:SafetyQuery={status:'all',origin:'all',cursor:null};
const live:SafetyQuery={status:'all',origin:'tinyfish-fetch',cursor:null};
const fixtures:Rows={updates:['synthetic-fixture'],counts:{new:1},nextCursor:'fixture-cursor'};
const liveRows:Rows={updates:['synthetic-provider-mock'],counts:{new:2},nextCursor:'live-cursor'};
function deferred<T>(){
 let resolve!:(value:T)=>void,reject!:(reason:unknown)=>void;
 const promise=new Promise<T>((yes,no)=>{resolve=yes;reject=no});
 return {promise,resolve,reject};
}

test('a delayed mutation refreshes the current origin and page, not the callback creation query',async()=>{
 const requests=new SafetyQueryLoader<Rows>(all),paths:string[]=[];
 let snapshot:SafetySnapshot<Rows>|null=null;
 const transport=async(path:string)=>{paths.push(path);return path.includes('origin=tinyfish-fetch')?liveRows:fixtures;};
 // This callback is retained by the mutation exactly as load() is retained by the component.
 const refresh=async()=>{const result=await requests.load(transport);if(result?.snapshot)snapshot=result.snapshot;};
 await refresh();assert.deepEqual(visibleSafetyData(snapshot,all),fixtures);
 const post=deferred<void>();
 const pendingMutation=(async()=>{await post.promise;await refresh();})();
 const next:SafetyQuery={...live,status:'needs-review',cursor:'new page+/='};
 requests.select(next);
 assert.equal(visibleSafetyData(snapshot,next),null,'old rows, counts and cursor are hidden immediately');
 await refresh();assert.deepEqual(visibleSafetyData(snapshot,next),liveRows);
 post.resolve();await pendingMutation;
 assert.deepEqual(visibleSafetyData(snapshot,next),liveRows);
 const last=new URL(paths.at(-1)!,'https://example.test');
 assert.equal(last.searchParams.get('origin'),'tinyfish-fetch');
 assert.equal(last.searchParams.get('status'),'needs-review');
 assert.equal(last.searchParams.get('cursor'),'new page+/=');
 assert.equal(paths.filter(path=>path.includes('origin=all')).length,1,'post-mutation refresh cannot request the old query');
});

test('failed origin-change GET cannot reveal old rows, counts or pagination under the new selection',async()=>{
 const requests=new SafetyQueryLoader<Rows>(all);
 const initial=await requests.load(async()=>fixtures);assert(initial?.snapshot);
 const saved=initial.snapshot;
 requests.select(live);
 const result=await requests.load(async()=>{throw new Error('Synthetic connection failure');});
 assert(result&&'error' in result);assert.match(String(result.error),/Synthetic connection failure/);
 assert.equal(visibleSafetyData(saved,live),null);
 assert.deepEqual(saved.value,fixtures,'last successful global panel data remains available to Sources and Settings');
 const retry=await requests.load(async()=>liveRows);assert(retry?.snapshot);
 assert.deepEqual(visibleSafetyData(retry.snapshot,live),liveRows);
});

test('late success or failure from an old query cannot replace the current result',async()=>{
 for(const fail of [false,true]){
  const requests=new SafetyQueryLoader<Rows>(all),old=deferred<Rows>();
  const pending=requests.load(()=>old.promise);
  requests.select(live);
  const fresh=await requests.load(async()=>liveRows);assert(fresh?.snapshot);
  if(fail)old.reject(new Error('Old request failed'));else old.resolve(fixtures);
  assert.equal(await pending,null);
  assert.deepEqual(visibleSafetyData(fresh.snapshot,live),liveRows);
 }
});

test('query identity includes the cursor and invalidation rejects retired requests',async()=>{
 const requests=new SafetyQueryLoader<Rows>(live),pending=deferred<Rows>();
 const first=await requests.load(async()=>liveRows);assert(first?.snapshot);
 assert.equal(visibleSafetyData(first.snapshot,{...live,cursor:'second-page'}),null);
 assert.equal(visibleSafetyData(first.snapshot,{...live,status:'approved'}),null);
 const work=requests.load(()=>pending.promise);requests.invalidate();pending.resolve(liveRows);
 assert.equal(await work,null);
});
