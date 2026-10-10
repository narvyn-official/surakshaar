import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {InstitutionPractice} from '@/components/studio/InstitutionPractice';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<InstitutionPractice/>:<Login/>}
