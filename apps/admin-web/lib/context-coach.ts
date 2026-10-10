import {z} from 'zod';

const id = z.string().min(1).max(100).regex(/^[A-Za-z0-9][A-Za-z0-9_.:-]*$/);
export const coachRequestSchema = z.object({
  workerId: z.string().uuid().optional(),
  courseId: z.enum(['fire', 'first-aid']),
  contentVersion: id,
  scenarioId: id,
  objectiveId: id,
  selectedObjectId: id.optional(),
  recentActionIds: z.array(id).max(12).default([]),
  hintLevel: z.enum(['controls', 'explain', 'hint', 'reflect']),
  language: z.enum(['en', 'hi', 'sat']),
  mode: z.enum(['learn', 'assessment']),
  allowExternalCoaching: z.boolean().default(false),
}).strict();
export type CoachRequest = z.infer<typeof coachRequestSchema>;
export type CoachActionId = 'inspect-selection' | 'show-step' | 'open-sources' | 'open-replay';
export type CoachSource = {
  id: string; version: string; title: string; url?: string;
  status: 'implementation-reference' | 'approved' | 'draft';
  reviewer?: string | null;
};
export type CoachCard = {
  id: string; text: string; language: 'en' | 'hi' | 'sat'; sourceId: string;
  actionIds: CoachActionId[]; intent: CoachRequest['hintLevel'];
  requiresSelection?: boolean;
};
export type CoachStep = {id: string; objectIds: string[]; actionIds: string[]; cards: CoachCard[]};
export type CoachRegistry = {
  version: string;
  sources: CoachSource[];
  scenarios: {id: string; courseId: CoachRequest['courseId']; reviewStatus: string; reviewer: string | null; steps: CoachStep[]}[];
};
export type CoachCandidate = {explanationId: string; text: string; actionIds: CoachActionId[]};
export type CoachSelection = {explanationId: string; actionIds: CoachActionId[]};
export type CoachProviderInput = {
  // Explicit projection: account, worker, free text, pictures and voice are never sent.
  context: Pick<CoachRequest, 'courseId' | 'contentVersion' | 'scenarioId' | 'objectiveId' | 'selectedObjectId' | 'recentActionIds' | 'hintLevel' | 'language'>;
  candidates: CoachCandidate[];
};
export type CoachProvider = {select(input: CoachProviderInput, signal: AbortSignal): Promise<unknown>};
export type CoachProviderDisclosure = {name: string; privacyUrl: string; dataUse: string};
export type ProviderConfiguration =
  | {status: 'configured'; provider: CoachProvider; disclosure?: CoachProviderDisclosure}
  | {status: 'disabled' | 'missing-config' | 'invalid-config'};
export type CoachReply = {
  schemaVersion: 1;
  status: 'supported' | 'review-required' | 'assessment-blocked' | 'language-unavailable' | 'unavailable';
  mode: 'authored-reference' | 'provider-selection';
  aiStatus: 'not-requested' | 'disabled' | 'missing-config' | 'invalid-config' | 'review-required' | 'assessment-blocked' | 'language-unavailable' | 'unavailable' | 'selected';
  text: string;
  contentVersion: string;
  language: CoachRequest['language'];
  responseLanguage: CoachRequest['language'];
  explanationId: string | null;
  actions: {id: CoachActionId; label: string}[];
  sources: CoachSource[];
  providerDisclosure: CoachProviderDisclosure | null;
};
export class CoachContextError extends Error {}
const messages = {
  review: {
    en: 'This lesson is a draft awaiting qualified review. AI cannot turn draft material into approved safety instructions. You can inspect the model, read the source notes or continue the clearly labelled draft lesson.',
    hi: 'इस पाठ की योग्य विशेषज्ञ द्वारा समीक्षा बाकी है। AI मसौदे को स्वीकृत सुरक्षा निर्देश नहीं बना सकता। आप मॉडल और स्रोत देख सकते हैं या स्पष्ट रूप से चिह्नित मसौदा पाठ पढ़ सकते हैं।',
  },
  assessment: {
    en: 'Coaching is unavailable during an assessment. Finish or leave the attempt, then review your recorded decisions.',
    hi: 'मूल्यांकन के दौरान सहायता उपलब्ध नहीं है। प्रयास पूरा करें या उससे बाहर आएँ, फिर अपने दर्ज निर्णयों की समीक्षा करें।',
  },
  language: {en: 'Reviewed Santali coaching is not available. The app will not silently replace your selected language. Choose another available language or continue the lesson without AI.'},
  unavailable: {en: 'No reviewed explanation matches this context. Use the lesson source notes or ask your instructor.', hi: 'इस संदर्भ के लिए समीक्षा किया गया स्पष्टीकरण उपलब्ध नहीं है। पाठ के स्रोत देखें या प्रशिक्षक से पूछें।'},
};
const labels: Record<CoachActionId, {en: string; hi: string}> = {
  'inspect-selection': {en: 'Inspect selection', hi: 'चयनित भाग देखें'},
  'show-step': {en: 'Return to lesson', hi: 'पाठ पर लौटें'},
  'open-sources': {en: 'View sources', hi: 'स्रोत देखें'},
  'open-replay': {en: 'Review decisions', hi: 'निर्णयों की समीक्षा'},
};
function blank(input: CoachRequest, status: CoachReply['status'], aiStatus: CoachReply['aiStatus'], text: string): CoachReply {
  return {schemaVersion: 1, status, mode: 'authored-reference', aiStatus, text, contentVersion: input.contentVersion, language: input.language, responseLanguage: input.language === 'sat' ? 'en' : input.language, explanationId: null, actions: [], sources: [], providerDisclosure: null};
}
export function validateCoachContext(input: CoachRequest, registry: CoachRegistry) {
  if (input.contentVersion !== registry.version) throw new CoachContextError('Unsupported coaching content version. Update the learning pack.');
  const scenario = registry.scenarios.find(s => s.id === input.scenarioId && s.courseId === input.courseId);
  const step = scenario?.steps.find(s => s.id === input.objectiveId);
  if (!scenario || !step) throw new CoachContextError('Invalid coaching scenario or objective.');
  if (input.selectedObjectId && !step.objectIds.includes(input.selectedObjectId)) throw new CoachContextError('Invalid selected object for this objective.');
  const allowedActions = new Set(scenario.steps.flatMap(s => s.actionIds));
  if (input.recentActionIds.some(action => !allowedActions.has(action))) throw new CoachContextError('Invalid action history for this scenario.');
  return {scenario, step};
}
const selectionSchema = z.object({
  explanationId: id,
  actionIds: z.array(z.enum(['inspect-selection', 'show-step', 'open-sources', 'open-replay'])).max(4),
}).strict();
export function validCoachSelection(value: unknown, candidates: CoachCandidate[]): CoachSelection | null {
  const parsed = selectionSchema.safeParse(value);
  if (!parsed.success) return null;
  const selected = candidates.find(c => c.explanationId === parsed.data.explanationId);
  if (!selected || new Set(parsed.data.actionIds).size !== parsed.data.actionIds.length || parsed.data.actionIds.some(a => !selected.actionIds.includes(a))) return null;
  return parsed.data;
}
/** Pure content selection. This module has no score, certificate, database or scene-state writer. */
export async function contextCoach(input: CoachRequest, registry: CoachRegistry, configuration: ProviderConfiguration = {status: 'missing-config'}, signal?: AbortSignal): Promise<CoachReply> {
  const {scenario, step} = validateCoachContext(input, registry);
  const language = input.language === 'hi' ? 'hi' : 'en';
  if (input.mode === 'assessment') return blank(input, 'assessment-blocked', 'assessment-blocked', messages.assessment[language]);
  if (input.language === 'sat') return blank(input, 'language-unavailable', 'language-unavailable', messages.language.en);
  // A checked source URL is not an approval. Both scenario and exact source need qualified approval.
  if (input.hintLevel !== 'controls' && (scenario.reviewStatus !== 'approved' || !scenario.reviewer?.trim())) {
    return blank(input, 'review-required', 'review-required', messages.review[language]);
  }
  const candidates = step.cards.filter(card => {
    const source = registry.sources.find(s => s.id === card.sourceId);
    const reviewed = source?.status === 'approved' && Boolean(source.reviewer?.trim());
    const productControl = card.intent === 'controls' && source?.status === 'implementation-reference';
    return card.intent === input.hintLevel && card.language === input.language && (!card.requiresSelection || Boolean(input.selectedObjectId)) && (reviewed || productControl);
  });
  if (!candidates.length) return blank(input, 'unavailable', 'unavailable', messages.unavailable[language]);
  let chosen = candidates[0];
  let actionIds = chosen.actionIds;
  let aiStatus: CoachReply['aiStatus'] = input.allowExternalCoaching ? configuration.status === 'configured' ? 'unavailable' : configuration.status : 'not-requested';
  if (input.allowExternalCoaching && configuration.status === 'configured' && !signal?.aborted) {
    const controller = new AbortController();
    const cancel = () => controller.abort();
    signal?.addEventListener('abort', cancel, {once: true});
    const timer = setTimeout(cancel, 3000);
    const safeCandidates = candidates.map(card => ({explanationId: card.id, text: card.text, actionIds: card.actionIds}));
    const context: CoachProviderInput['context'] = {
      courseId: input.courseId, contentVersion: input.contentVersion, scenarioId: input.scenarioId, objectiveId: input.objectiveId,
      ...(input.selectedObjectId ? {selectedObjectId: input.selectedObjectId} : {}),
      recentActionIds: input.recentActionIds, hintLevel: input.hintLevel, language: input.language,
    };
    try {
      const raw = await Promise.race([
        configuration.provider.select({context, candidates: safeCandidates}, controller.signal),
        new Promise<null>(resolve => controller.signal.addEventListener('abort', () => resolve(null), {once: true})),
      ]);
      const selected = validCoachSelection(raw, safeCandidates);
      if (selected && !controller.signal.aborted) {
        chosen = candidates.find(c => c.id === selected.explanationId)!;
        actionIds = selected.actionIds;
        aiStatus = 'selected';
      }
    } catch { /* A provider failure returns the authored reference; no generated prose is displayed. */ }
    finally {clearTimeout(timer); controller.abort(); signal?.removeEventListener('abort', cancel);}
  }
  const source = registry.sources.find(s => s.id === chosen.sourceId)!;
  return {...blank(input, 'supported', aiStatus, chosen.text), mode: aiStatus === 'selected' ? 'provider-selection' : 'authored-reference', explanationId: chosen.id, actions: actionIds.map(id => ({id, label: labels[id][language]})), sources: [source], providerDisclosure: configuration.status === 'configured' ? configuration.disclosure ?? null : null};
}
