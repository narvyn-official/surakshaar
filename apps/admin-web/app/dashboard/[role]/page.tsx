import {redirect} from 'next/navigation';
import {workspaceContext} from '@/lib/workspace-context';
import {RoleDashboard} from '@/components/workspace/RoleDashboard';
export const dynamic='force-dynamic';
export async function generateMetadata(){const c=await workspaceContext();return {title:c?c.label+' dashboard':'Your dashboard'};}
export default async function Page({params,searchParams}:{params:Promise<{role:string}>;searchParams:Promise<{notice?:string}>}){const c=await workspaceContext();if(!c)redirect('/login');if((await params).role!==c.role)redirect(c.home);return <RoleDashboard context={c} unavailable={(await searchParams).notice==='unavailable'}/>;}
