import {env} from './runtime-env';
import type {Access} from './server';
export const scopedRoles = ['trainer','examiner','certifier','assessor','viewer'] as const;
export type StaffScope = {restricted:boolean};
const database=()=>env.DB as D1Database;
/** Scope is resolved from active membership on every request, never a client claim. */
export async function staffScope(owner:string,userId:string):Promise<StaffScope>{
 if(owner===userId)return {restricted:false};
 const row=await database().prepare('SELECT scope_mode FROM team_members WHERE owner=? AND user_id=? AND active=1').bind(owner,userId).first<{scope_mode:string}>();
 return {restricted:row?.scope_mode!=='all'};
}
// UNION (not UNION ALL) also bounds traversal if imported hierarchy data has a cycle.
export function unitScope(a:Pick<Access,'owner'|'user'|'restricted'>,unitColumn:string){
 if(!/^[a-z_]+\.[a-z_]+$/.test(unitColumn))throw Error('Invalid internal scope column.');
 return a.restricted?{sql:`EXISTS (WITH RECURSIVE allowed(id) AS (
 SELECT u.id FROM institution_units u JOIN team_member_units g ON g.owner=u.owner AND g.unit_id=u.id
 JOIN team_members m ON m.owner=g.owner AND m.email=g.email
 WHERE m.owner=? AND m.user_id=? AND m.active=1 AND m.scope_mode='units' AND u.archived_at IS NULL
 UNION SELECT child.id FROM institution_units child JOIN allowed parent ON child.parent_id=parent.id WHERE child.owner=? AND child.archived_at IS NULL
 ) SELECT 1 FROM allowed WHERE allowed.id=${unitColumn})`,args:[a.owner,a.user.userId,a.owner]}:{sql:'1=1',args:[]};
}
export function learnerScope(a:Pick<Access,'owner'|'user'|'restricted'>,workerColumn:string){
 if(!/^[a-z_]+\.[a-z_]+$/.test(workerColumn))throw Error('Invalid internal scope column.');
 const scope=unitScope(a,'scope_person.unit_id');
 return a.restricted?{sql:`EXISTS(SELECT 1 FROM institution_people scope_person WHERE scope_person.owner=? AND scope_person.worker_id=${workerColumn} AND scope_person.archived_at IS NULL AND ${scope.sql})`,args:[a.owner,...scope.args]}:scope;
}
export async function requireWorkerScope(a:Access,workerId:string){
 const scope=learnerScope(a,'w.id');
 if(!await database().prepare('SELECT 1 FROM workers w WHERE w.owner=? AND w.id=? AND '+scope.sql).bind(a.owner,workerId,...scope.args).first())throw Error('Forbidden: this learner is outside your assigned scope.');
}
/** Insert in the same batch as a consequential write, so a move/revocation cannot race the check. */
export function scopeAssertion(a:Access,workerId:string,storage:D1Database=database()){
 const scope=learnerScope(a,'w.id');
 return storage.prepare("INSERT INTO staff_scope_assertions(allowed) SELECT EXISTS(SELECT 1 FROM workers w JOIN centre_approvals c ON c.owner=w.owner AND c.status='approved' WHERE w.owner=? AND w.id=? AND "+scope.sql+" AND (?=w.owner OR EXISTS(SELECT 1 FROM team_members m WHERE m.owner=w.owner AND m.user_id=? AND m.active=1 AND m.role=? AND m.scope_mode=?)))").bind(a.owner,workerId,...scope.args,a.user.userId,a.user.userId,a.role,a.restricted?'units':'all');
}
