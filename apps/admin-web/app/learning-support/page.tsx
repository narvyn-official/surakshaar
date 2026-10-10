import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {LearningSupport} from '@/components/studio/LearningSupport';
export const dynamic='force-dynamic';
export default async function Page(){return await getAppUser()?<LearningSupport/>:<Login/>}
