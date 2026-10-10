import {db} from './server';
import {defaultPolicy,type InstitutionPolicy} from './institution';
export async function currentInstitutionPolicy(owner:string,version?:number|null){
 if(version===null)return {version:0,policy:defaultPolicy};
 const row=await db().prepare('SELECT version,body FROM institution_policies WHERE owner=?'+(version?' AND version=?':'')+' ORDER BY version DESC LIMIT 1').bind(owner,...(version?[version]:[])).first<{version:number;body:string}>();
 return {version:row?.version??0,policy:row?JSON.parse(row.body) as InstitutionPolicy:defaultPolicy};
}
export async function requireInstitutionCapacity(owner:string,adding=0){
 const plan=await db().prepare('SELECT * FROM institution_plans WHERE owner=?').bind(owner).first<{restricted:number;valid_until:number|null;learner_limit:number}>();
 if(plan&&(plan.restricted||plan.valid_until!==null&&plan.valid_until<=Date.now()))throw Error('Forbidden: new enrolment, assignments and issuance are restricted. Existing records, review and revocation remain available.');
 if(plan&&adding){const total=await db().prepare('SELECT COUNT(*) AS n FROM workers w LEFT JOIN institution_people p ON p.owner=w.owner AND p.worker_id=w.id WHERE w.owner=? AND p.archived_at IS NULL').bind(owner).first<{n:number}>();if((total?.n??0)+adding>plan.learner_limit)throw Error('Invalid enrolment: the institution learner allowance would be exceeded.');}
}
