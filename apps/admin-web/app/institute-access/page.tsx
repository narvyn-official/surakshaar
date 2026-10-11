import {workspacePage} from '@/lib/workspace-page';
import {InstituteAccess} from '@/components/workspace/InstituteAccess';
export const dynamic='force-dynamic';
export const metadata={title:'Institute access'};
export default async function Page(){const context=await workspacePage('/institute-access');return <InstituteAccess email={context.user.email} platform={context.role==='platform'}/>;}
