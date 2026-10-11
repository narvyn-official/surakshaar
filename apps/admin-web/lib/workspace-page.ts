import {redirect} from 'next/navigation';
import {workspaceContext} from './workspace-context';
import {roleAllowsPage,roleActions} from './role-workspace';
export async function workspacePage(path:string){const c=await workspaceContext();if(!c)redirect('/login?next='+encodeURIComponent(path));if(!roleAllowsPage(c.role,path)||c.restricted&&path!=='/account'&&path!=='/information'&&path!=='/institute-access'&&!roleActions(c.role,true).some(a=>a.href.split(/[?#]/)[0]===path))redirect(c.home+'?notice=unavailable');if((c.mfaRequired||c.workspaceUnavailable)&&path!=='/account'&&path!=='/institute-access'&&path!=='/information')redirect(c.home);return c;}
