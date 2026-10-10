import {redirect} from 'next/navigation';
import {workspaceContext} from '@/lib/workspace-context';
export const dynamic='force-dynamic';
export default async function Page(){const c=await workspaceContext();redirect(c?.home??'/login');}
