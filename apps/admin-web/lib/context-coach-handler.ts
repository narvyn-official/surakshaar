import {coachRequestSchema, contextCoach, validateCoachContext, type CoachRegistry, type ProviderConfiguration} from './context-coach';
import {readJson} from './http';

type Dependencies = {
  registry: CoachRegistry;
  /** No worker ID means personal learning; supplied IDs must pass existing tenant/link checks. */
  authorize(workerId?: string): Promise<{accountId: string}>;
  budget(accountId: string): Promise<Response | null>;
  provider(): ProviderConfiguration;
};
export async function handleContextCoach(request: Request, dependencies: Dependencies): Promise<Response> {
  const parsed = coachRequestSchema.safeParse(await readJson(request, 4096));
  if (!parsed.success) throw new Error('Invalid contextual coaching request.');
  const input = parsed.data;
  const access = await dependencies.authorize(input.workerId);
  validateCoachContext(input, dependencies.registry);
  const limited = await dependencies.budget(access.accountId);
  if (limited) return limited;
  const reply = await contextCoach(input, dependencies.registry, dependencies.provider(), request.signal);
  return Response.json(reply, {headers: {'Cache-Control': 'no-store'}});
}
