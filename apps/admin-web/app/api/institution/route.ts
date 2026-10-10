import {access,audit,db,digest,failure,json} from '@/lib/server';
import {freshUser} from '@/lib/auth';
import {permitted} from '@/lib/access';
import {institutionCommand} from '@/lib/institution';
import {currentInstitutionPolicy,requireInstitutionCapacity} from '@/lib/institution-store';
import {launchBudget} from '@/lib/learning-centre-store';
const reply=(data:unknown)=>Response.json(data,{headers:{'Cache-Control':'private, no-store'}});
export async function GET(){try{
 const a=await access('workspace'),canRead=permitted(a.role,'read'),canContent=permitted(a.role,'content');
 const [units,people,policy,procedures,plan]=await Promise.all([
 canRead?db().prepare('SELECT * FROM institution_units WHERE owner=? ORDER BY kind,name LIMIT 300').bind(a.owner).all():{results:[]},
 canRead?db().prepare('SELECT w.id,w.name,w.sector,p.employee_number,p.unit_id,p.archived_at,l.user_id IS NOT NULL AS linked FROM workers w LEFT JOIN institution_people p ON p.owner=w.owner AND p.worker_id=w.id LEFT JOIN learner_links l ON l.owner=w.owner AND l.worker_id=w.id WHERE w.owner=? ORDER BY w.name,w.id LIMIT 1000').bind(a.owner).all():{results:[]},
 currentInstitutionPolicy(a.owner),
 canContent?db().prepare('SELECT * FROM local_procedures WHERE owner=? ORDER BY created_at DESC LIMIT 100').bind(a.owner).all():{results:[]},
 canRead?db().prepare('SELECT name,learner_limit,valid_until,restricted,reason FROM institution_plans WHERE owner=?').bind(a.owner).first():null]);
 return reply({owner:a.owner,actor:a.user.userId,role:a.role,units:units.results,people:people.results,policy,procedures:procedures.results,plan,limits:{people:1000,units:300,procedures:100}});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const parsed=institutionCommand.safeParse(await json(request,150000));if(!parsed.success)throw Error('Invalid institution request. Check required fields and limits.');const c=parsed.data;
 const a=await access(c.action==='procedure'?'content':c.action==='procedure-review'?'content-review':'admin');await launchBudget(a.user.userId);const now=Date.now();
 if(c.action==='procedure'){
  const existing=await db().prepare('SELECT * FROM local_procedures WHERE owner=? AND id=?').bind(a.owner,c.id).first();
  if(existing){if(existing.author_id!==a.user.userId||existing.title!==c.title||existing.body!==c.body||existing.sources!==c.sources||existing.language!==c.language)throw Error('Record conflict: use a new ID for a content revision.');return reply({saved:true,id:c.id});}
  await db().batch([db().prepare('INSERT INTO local_procedures(owner,id,title,body,sources,language,author_id,created_at,status) VALUES(?,?,?,?,?,?,?,?,?)').bind(a.owner,c.id,c.title,c.body,c.sources,c.language,a.user.userId,now,c.submit?'submitted':'draft'),audit(a,'content.submit',c.id)]);return reply({saved:true,id:c.id});
 }
 if(c.action==='procedure-review'){
  await freshUser();const row=await db().prepare('SELECT * FROM local_procedures WHERE owner=? AND id=?').bind(a.owner,c.id).first();if(!row)throw Error('No local procedure found.');if(row.author_id===a.user.userId)throw Error('Forbidden: another qualified reviewer must review your content.');
  if(c.outcome==='withdrawn'?row.status!=='approved':row.status!=='submitted')throw Error('Record conflict: the content is not in a reviewable state.');
  const result=await db().batch([db().prepare('UPDATE local_procedures SET status=?,reviewer_id=?,reviewed_at=?,review_note=?,qualification=?,revision=revision+1 WHERE owner=? AND id=? AND revision=? AND status=?').bind(c.outcome,a.user.userId,now,c.note,c.qualification,a.owner,c.id,c.revision,row.status),audit(a,'content.'+c.outcome,c.id,{note:c.note},true)]);if(!result[0].meta.changes)throw Error('Record conflict: refresh the current content revision.');return reply({saved:true});
 }
 if(c.action==='policy'){
  await freshUser();const current=await currentInstitutionPolicy(a.owner);if(current.version!==c.previousVersion)throw Error('Record conflict: another administrator published a policy. Refresh first.');
  if(c.policy.assessment==='snapshots'&&!await db().prepare('SELECT 1 FROM identity_policies WHERE owner=? AND enabled=1').bind(a.owner).first())throw Error('Invalid policy: enable a reviewed adult camera notice and encryption first.');
  await db().batch([db().prepare('INSERT INTO institution_policies VALUES(?,?,?,?,?)').bind(a.owner,current.version+1,JSON.stringify(c.policy),a.user.userId,now),audit(a,'institution.policy',String(current.version+1))]);return reply({saved:true,version:current.version+1});
 }
 if(c.action==='archive-unit'){
  if(await db().prepare('SELECT 1 FROM institution_people WHERE owner=? AND unit_id=? AND archived_at IS NULL UNION ALL SELECT 1 FROM institution_units WHERE owner=? AND parent_id=? AND archived_at IS NULL').bind(a.owner,c.id,a.owner,c.id).first())throw Error('Invalid archive: move active people and child units first.');
  await db().batch([db().prepare('UPDATE institution_units SET archived_at=? WHERE owner=? AND id=? AND archived_at IS NULL').bind(now,a.owner,c.id),audit(a,'institution.unit-archive',c.id,{},true)]);return reply({saved:true});
 }
 const unitId=c.action==='unit'?c.parentId:c.unitId;
 if(unitId&&!await db().prepare('SELECT id FROM institution_units WHERE owner=? AND id=? AND archived_at IS NULL').bind(a.owner,unitId).first())throw Error('Invalid organisational unit.');
 if(c.action==='unit'){
  if(c.id===c.parentId)throw Error('Invalid unit parent.');
  const previous=await db().prepare('SELECT kind,name,parent_id FROM institution_units WHERE owner=? AND id=?').bind(a.owner,c.id).first();if(previous){if(previous.kind!==c.kind||previous.name!==c.name||previous.parent_id!==c.parentId)throw Error('Record conflict: unit already exists with different details.');return reply({saved:true});}
  await db().batch([db().prepare('INSERT INTO institution_units(owner,id,kind,name,parent_id,created_at) VALUES(?,?,?,?,?,?)').bind(a.owner,c.id,c.kind,c.name,c.parentId,now),audit(a,'institution.unit',c.id)]);return reply({saved:true});
 }
 if(c.action==='person'){
  if(!await db().prepare('SELECT id FROM workers WHERE owner=? AND id=?').bind(a.owner,c.workerId).first())throw Error('No learner found.');
  if(await db().prepare('SELECT 1 FROM institution_people WHERE owner=? AND employee_number=? COLLATE NOCASE AND worker_id<>?').bind(a.owner,c.employeeNumber,c.workerId).first())throw Error('Invalid employee number: already used in this institution.');
  if(!c.archived){const prior=await db().prepare('SELECT archived_at FROM institution_people WHERE owner=? AND worker_id=?').bind(a.owner,c.workerId).first();await requireInstitutionCapacity(a.owner,prior?.archived_at?1:0);}
  await db().batch([db().prepare('INSERT INTO institution_people VALUES(?,?,?,?,?) ON CONFLICT(owner,worker_id) DO UPDATE SET employee_number=excluded.employee_number,unit_id=excluded.unit_id,archived_at=excluded.archived_at').bind(a.owner,c.workerId,c.employeeNumber,c.unitId,c.archived?now:null),audit(a,'institution.person',c.workerId,{archived:c.archived})]);return reply({saved:true});
 }
 if(new Set(c.people.map(p=>p.employeeNumber.toLowerCase())).size!==c.people.length)throw Error('Invalid import: duplicate employee numbers.');
 const hash=await digest(c),old=await db().prepare('SELECT digest,result FROM institution_imports WHERE owner=? AND id=?').bind(a.owner,c.id).first<{digest:string;result:string}>();if(old){if(old.digest!==hash)throw Error('Record conflict: import ID reused with different data.');return reply(JSON.parse(old.result));}
 await requireInstitutionCapacity(a.owner,c.people.length);
 const statements:D1PreparedStatement[]=[],ids:string[]=[];
 for(const p of c.people){if(await db().prepare('SELECT 1 FROM institution_people WHERE owner=? AND employee_number=? COLLATE NOCASE').bind(a.owner,p.employeeNumber).first())throw Error('Invalid import: an employee number already exists. No rows were imported.');const id=crypto.randomUUID();ids.push(id);statements.push(db().prepare('INSERT INTO workers(owner,id,name,sector,updated_at) VALUES(?,?,?,?,?)').bind(a.owner,id,p.name,p.sector,now),db().prepare('INSERT INTO institution_people VALUES(?,?,?,?,NULL)').bind(a.owner,id,p.employeeNumber,c.unitId));}
 const result={saved:true,count:ids.length,workerIds:ids};statements.push(db().prepare('INSERT INTO institution_imports VALUES(?,?,?,?,?)').bind(a.owner,c.id,hash,JSON.stringify(result),now),audit(a,'institution.import',c.id,{count:ids.length}));await db().batch(statements);return reply(result);
}catch(e){return failure(e)}}
