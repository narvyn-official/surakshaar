import {redirect} from 'next/navigation';
import {getAppUser} from '@/lib/auth';
import {LearningStudio} from '@/components/studio/LearningStudio';
export const dynamic='force-dynamic';
export default async function Page(){if(await getAppUser())redirect('/dashboard');return <LearningStudio user={null}/>;}
