import {workspacePage} from '@/lib/workspace-page';
import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {InstitutionLibrary} from '@/components/studio/InstitutionLibrary';
export const dynamic='force-dynamic';
export default async function Page(){await workspacePage('/institute-library');return await getAppUser()?<InstitutionLibrary/>:<Login/>}
