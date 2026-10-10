import {workspacePage} from '@/lib/workspace-page';
import {AuditWorkspace} from '@/components/workspace/AuditWorkspace';
export const dynamic='force-dynamic';
export const metadata={title:'Institution audit'};
export default async function Page(){await workspacePage('/audit');return <AuditWorkspace/>;}
