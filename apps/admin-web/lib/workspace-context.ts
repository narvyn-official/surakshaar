import {headers} from 'next/headers';
import {getAppUser,isOperator} from './auth';
import {db} from './server';
import {env} from './runtime-env';
import {staffWorkspaces,selectedWorkspace,defaultStaffWorkspace,type StaffWorkspace} from './workspace-store';
import {dashboardPath,workspaceDefinitions,type WorkspaceRole} from './role-workspace';
export type WorkspaceContext={user:{userId:string;email:string;displayName:string};role:WorkspaceRole;label:string;home:string;institution:StaffWorkspace|null;workspaces:StaffWorkspace[];mfaRequired:boolean;workspaceUnavailable:boolean};
export async function workspaceContext():Promise<WorkspaceContext|null>{
 const user=await getAppUser();if(!user)return null;
 const choices=await staffWorkspaces(user.userId),selected=selectedWorkspace((await headers()).get('cookie')??''),preferred=selected?choices.find(w=>w.owner===selected):defaultStaffWorkspace(choices,user.userId),unavailable=!!selected&&!preferred;
 const institution=preferred??defaultStaffWorkspace(choices,user.userId);let role:WorkspaceRole;
 if(isOperator(user.userId))role='platform';else if(institution)role=institution.role;
 else if(await db().prepare("SELECT 1 FROM learner_links l JOIN centre_approvals c ON c.owner=l.owner AND c.status='approved' LEFT JOIN institution_people p ON p.owner=l.owner AND p.worker_id=l.worker_id WHERE l.user_id=? AND p.archived_at IS NULL LIMIT 1").bind(user.userId).first())role='learner';
 else if(await db().prepare('SELECT 1 FROM learning_support_links WHERE supporter_id=? AND revoked_at IS NULL AND expires_at>? LIMIT 1').bind(user.userId,Date.now()).first())role='supporter';else role='personal';
 const staff=!['learner','personal','supporter'].includes(role);const factor=staff?await db().prepare('SELECT two_factor_enabled FROM auth_user WHERE id=?').bind(user.userId).first<{two_factor_enabled:number}>():null;
 return {user,role,label:workspaceDefinitions[role].label,home:dashboardPath(role),institution:role==='platform'?null:institution,workspaces:choices,mfaRequired:staff&&env.REQUIRE_STAFF_MFA!=='false'&&!factor?.two_factor_enabled,workspaceUnavailable:role!=='platform'&&unavailable};
}
