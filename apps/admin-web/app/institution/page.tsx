import {workspacePage} from '@/lib/workspace-page';
import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {InstitutionWorkspace} from '@/components/training/InstitutionWorkspace';
export const dynamic='force-dynamic';
export default async function Page(){await workspacePage('/institution');return await getAppUser()?<InstitutionWorkspace/>:<Login/>}
