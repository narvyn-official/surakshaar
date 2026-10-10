import {redirect} from 'next/navigation';
import {workspaceContext} from './workspace-context';
import {roleAllowsPage} from './role-workspace';
export async function workspacePage(path:string){const c=await workspaceContext();if(!c)redirect('/login?next='+encodeURIComponent(path));if(!roleAllowsPage(c.role,path))redirect(c.home+'?notice=unavailable');if((c.mfaRequired||c.workspaceUnavailable)&&path!=='/account')redirect(c.home);return c;}
