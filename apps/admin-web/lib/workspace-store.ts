import {env} from './runtime-env';
import {roles,type Role} from './access';
export type StaffWorkspace={owner:string;role:Role;name:string;site:string};
export async function staffWorkspaces(userId:string):Promise<StaffWorkspace[]>{
 const db=env.DB as D1Database;const result=await db.prepare("SELECT c.owner AS owner,'admin' AS role,c.name AS name,c.site AS site FROM training_centres c JOIN centre_approvals a ON a.owner=c.owner AND a.status='approved' WHERE c.owner=? UNION ALL SELECT c.owner AS owner,m.role AS role,c.name AS name,c.site AS site FROM team_members m JOIN training_centres c ON c.owner=m.owner JOIN centre_approvals a ON a.owner=c.owner AND a.status='approved' WHERE m.user_id=? AND m.active=1 AND c.owner<>? ORDER BY name,owner LIMIT 200").bind(userId,userId,userId).all<StaffWorkspace>();return result.results.filter(r=>roles.includes(r.role)).map(r=>({owner:r.owner,role:r.role,name:r.name,site:r.site}));
}
export function selectedWorkspace(cookie:string){const value=cookie.split(';').map(v=>v.trim()).find(v=>v.startsWith('suraksha_workspace='))?.slice('suraksha_workspace='.length);if(!value)return null;try{return decodeURIComponent(value)}catch{return '__invalid__'}}
export function defaultStaffWorkspace(choices:StaffWorkspace[],userId:string){return choices.find(w=>w.owner===userId)??choices[0]??null;}
