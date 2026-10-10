import {env} from 'cloudflare:workers';
import {getAppUser} from '@/lib/auth';
import {learnerMembership} from '@/lib/learner';
import {failure} from '@/lib/server';
import {handleContextCoach} from '@/lib/context-coach-handler';
import {contextCoachRegistry} from '@/lib/context-coach-registry';
import {configuredContextCoach} from '@/lib/context-coach-provider';
import {consumeTutorBudget} from '@/lib/tutor-budget';

export async function POST(request: Request) {
  try {
    return await handleContextCoach(request, {
      registry: contextCoachRegistry,
      async authorize(workerId) {
        if (workerId) return {accountId: (await learnerMembership(workerId)).user.userId};
        const user = await getAppUser();
        if (!user) throw new Error('Sign in to use contextual coaching.');
        return {accountId: user.userId};
      },
      budget: consumeTutorBudget,
      provider: () => configuredContextCoach(env as unknown as Record<string, string | undefined>),
    });
  } catch (error) {return failure(error);}
}
