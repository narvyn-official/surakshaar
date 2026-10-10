import {workspacePage} from '@/lib/workspace-page';
import {IdentityEnrolment} from '@/components/studio/IdentityEnrolment';
export const dynamic='force-dynamic';
export default async function Page(){const c=await workspacePage('/identity-enrolment');return <IdentityEnrolment staff={c.role==='admin'}/>}
