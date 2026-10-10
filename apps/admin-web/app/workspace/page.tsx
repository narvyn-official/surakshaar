import {redirect} from 'next/navigation';
import {workspaceContext} from '@/lib/workspace-context';
import {isStaffRole,legacyTabs} from '@/lib/role-workspace';
import Dashboard from '@/components/training/Dashboard';
export const dynamic='force-dynamic';
export default async function Page(){const c=await workspaceContext();if(!c)redirect('/login');if(c.mfaRequired||c.workspaceUnavailable||!isStaffRole(c.role)||!legacyTabs(c.role).length)redirect(c.home);return <Dashboard/>;}
