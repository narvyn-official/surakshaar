import {workspacePage} from '@/lib/workspace-page';
import {InstitutionAwards} from '@/components/training/InstitutionAwards';
export const dynamic='force-dynamic';
export default async function Page(){const c=await workspacePage('/awards');return <InstitutionAwards learner={c.role==='learner'}/>}
