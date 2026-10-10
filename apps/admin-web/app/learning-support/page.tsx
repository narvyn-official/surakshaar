import {workspacePage} from '@/lib/workspace-page';
import {getAppUser} from '@/lib/auth';
import Login from '@/components/training/Login';
import {LearningSupport} from '@/components/studio/LearningSupport';
export const dynamic='force-dynamic';
export default async function Page(){await workspacePage('/learning-support');return await getAppUser()?<LearningSupport/>:<Login/>}
