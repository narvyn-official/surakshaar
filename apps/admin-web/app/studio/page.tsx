import {redirect} from 'next/navigation';
import {workspaceContext} from '@/lib/workspace-context';
import {LearningStudio} from '@/components/studio/LearningStudio';
export const dynamic='force-dynamic';
export default async function Page(){const c=await workspaceContext();if(c&&!['personal','learner'].includes(c.role))redirect(c.home);return <LearningStudio user={c?.user??null}/>;}
