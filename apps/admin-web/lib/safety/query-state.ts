import type {CollectionOrigin} from './model';

export type SafetyQuery={status:string;origin:CollectionOrigin;cursor:string|null};
export type SafetySnapshot<T>={queryKey:string;value:T};
type Outcome<T>={snapshot:SafetySnapshot<T>;error?:never}|{snapshot?:never;error:unknown};
export function safetyQueryKey(query:SafetyQuery){return JSON.stringify([query.status,query.origin,query.cursor]);}
export function visibleSafetyData<T>(snapshot:SafetySnapshot<T>|null,query:SafetyQuery):T|null{
 return snapshot?.queryKey===safetyQueryKey(query)?snapshot.value:null;
}

/** Owns query identity independently of a render's callbacks. Delayed mutations always refresh
 * the current selection, and old requests cannot replace a newer selection or its failure state.
 */
export class SafetyQueryLoader<T>{
 private query:SafetyQuery;
 private generation=0;
 constructor(initial:SafetyQuery){this.query={...initial};}
 select(query:SafetyQuery){
  if(safetyQueryKey(query)!==safetyQueryKey(this.query)){this.query={...query};this.invalidate();}
 }
 invalidate(){this.generation++;}
 async load(request:(path:string)=>Promise<T>):Promise<Outcome<T>|null>{
  const query={...this.query},queryKey=safetyQueryKey(query),generation=++this.generation;
  const current=()=>generation===this.generation&&queryKey===safetyQueryKey(this.query);
  const path=`/api/safety-updates?status=${encodeURIComponent(query.status)}&origin=${query.origin}${query.cursor?'&cursor='+encodeURIComponent(query.cursor):''}`;
  try{
   const value=await request(path);
   return current()?{snapshot:{queryKey,value}}:null;
  }catch(error){return current()?{error}:null;}
 }
}
