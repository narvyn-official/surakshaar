import {z} from 'zod';
import catalogue from '../../../content/learning/scenarios.json';
import draft1 from '../../../content/learning/archive/learning-2026.10.10-draft1.json';

const timestamp = z.number().int().nonnegative().safe();
const key = z.string().min(1).max(100).regex(/^[A-Za-z0-9][A-Za-z0-9_.:-]*$/);
const event = z.discriminatedUnion('kind', [
  z.object({kind: z.literal('decision'), stepId: key, choiceId: key, correct: z.boolean(), at: timestamp, hints: z.number().int().nonnegative().max(1000)}).strict(),
  z.object({kind: z.literal('hint'), stepId: key, at: timestamp}).strict(),
  z.object({kind: z.literal('continue'), stepId: key, at: timestamp}).strict(),
  z.object({kind: z.literal('observation'), stepId: key, partId: key, mode: z.enum(['3d', 'ar-exploration']), at: timestamp}).strict(),
  z.object({kind: z.literal('teach-back'), text: z.string().trim().min(1).max(1000), at: timestamp}).strict(),
]);
const attemptSchema = z.object({
  id: z.string().uuid(), workerId: z.string().uuid(), scenarioId: key, courseId: z.enum(['fire', 'first-aid']), contentVersion: key,
  index: z.number().int().nonnegative(), answers: z.array(event).max(1000), finished: z.boolean(), guided: z.boolean(),
  evidenceType: z.literal('formative-decision-practice'), reviewStatus: z.literal('draft'), createdAt: timestamp, updatedAt: timestamp,
  stageHints: z.number().int().nonnegative().max(1000).optional(), awaitingContinue: z.boolean().optional(), lastCorrect: z.boolean().optional(),
}).strict();
export const learningPracticeEnvelope = z.object({schemaVersion: z.literal(1), workerId: z.string().uuid(), attempts: z.array(z.unknown()).min(1).max(20)}).strict();
export type LearningPractice = z.infer<typeof attemptSchema>;
export function learningCanonical(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(learningCanonical).join(',')}]`;
  if (value !== null && typeof value === 'object') return `{${Object.entries(value).sort(([a], [b]) => a.localeCompare(b)).map(([k, v]) => `${JSON.stringify(k)}:${learningCanonical(v)}`).join(',')}}`;
  return JSON.stringify(value);
}
/** Replays Android LearningAttempt. Observation events remain self-reported, not hardware-attested. */
export function validateLearningPractice(raw: unknown, workerId: string, now = Date.now()): LearningPractice {
  const parsed = attemptSchema.safeParse(raw);
  if (!parsed.success) throw new Error('Invalid formative learning record.');
  const record = parsed.data;
  if (record.workerId !== workerId) throw new Error('Forbidden: formative record belongs to another profile.');
  const version = record.contentVersion === catalogue.version ? catalogue : record.contentVersion === draft1.version ? draft1 : null;
  if (!version) throw new Error('Unsupported formative learning content version.');
  const scenario = version.scenarios.find(s => s.id === record.scenarioId && s.course === record.courseId);
  if (!scenario || scenario.reviewStatus !== record.reviewStatus) throw new Error('Invalid formative learning scenario or review status.');
  if (record.createdAt > now + 300000) throw new Error('Invalid device clock: formative record is in the future.');
  let index = 0, hints = 0, awaiting = false, updated = record.createdAt;
  let lastCorrect: boolean | undefined;
  const observed = new Set<string>();
  for (const item of record.answers) {
    if (item.at < updated || item.at > now + 300000) throw new Error('Invalid formative event time.');
    const step = scenario.steps[index];
    if ('stepId' in item && (!step || item.stepId !== step.id)) throw new Error('Invalid formative event sequence.');
    switch (item.kind) {
      case 'decision': {
        if (!step || awaiting || item.hints !== hints) throw new Error('Invalid formative decision order or hint count.');
        const choice = step.choices.find(c => c.id === item.choiceId);
        if (!choice || choice.correct !== item.correct) throw new Error('Invalid formative decision result.');
        awaiting = choice.correct; lastCorrect = choice.correct; break;
      }
      case 'hint': if (!step) throw new Error('Invalid hint after completion.'); hints++; break;
      case 'observation':
        if (!step?.part || item.partId !== step.part) throw new Error('Invalid anatomical observation.');
        observed.add(step.id); break;
      case 'continue':
        if (!step || !awaiting || ('requiresObservation' in step && step.requiresObservation && !observed.has(step.id))) throw new Error('Invalid advancement: decision or required anatomy exploration is incomplete.');
        index++; hints = 0; awaiting = false; break;
      case 'teach-back': if (index !== scenario.steps.length) throw new Error('Invalid reflection before completion.'); break;
    }
    updated = item.at;
  }
  if (record.index !== index || record.finished !== (index === scenario.steps.length) || (record.awaitingContinue ?? false) !== awaiting || (record.stageHints ?? 0) !== hints || record.updatedAt !== updated || (record.lastCorrect !== undefined && record.lastCorrect !== lastCorrect)) throw new Error('Invalid formative state: event replay does not match the record.');
  return record;
}
export function assertLearningSuccessor(previous: LearningPractice, next: LearningPractice) {
  const immutable: (keyof LearningPractice)[] = ['id', 'workerId', 'scenarioId', 'courseId', 'contentVersion', 'guided', 'evidenceType', 'reviewStatus', 'createdAt'];
  if (immutable.some(k => previous[k] !== next[k])) throw new Error('Record conflict: formative identity or learning mode changed.');
  if (next.answers.length < previous.answers.length || previous.answers.some((e, i) => learningCanonical(e) !== learningCanonical(next.answers[i]))) throw new Error('Record conflict: original formative events cannot be changed.');
  if (next.answers.length === previous.answers.length && learningCanonical(previous) !== learningCanonical(next)) throw new Error('Record conflict: an unchanged event history has different state.');
}
