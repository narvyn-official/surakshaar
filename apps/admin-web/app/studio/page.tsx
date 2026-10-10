import {getAppUser} from '@/lib/auth';
import {LearningStudio} from '@/components/studio/LearningStudio';
export const dynamic='force-dynamic';
export default async function Page(){const user=await getAppUser();return <LearningStudio user={user}/>;}
