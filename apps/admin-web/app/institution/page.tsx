import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {InstitutionWorkspace} from '@/components/training/InstitutionWorkspace';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<InstitutionWorkspace/>:<Login/>}
