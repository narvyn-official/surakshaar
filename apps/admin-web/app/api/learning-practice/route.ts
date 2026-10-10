import {z} from 'zod';
import {getAppUser} from '@/lib/auth';
import {db, digest, failure, json} from '@/lib/server';
import {learningPracticeEnvelope, validateLearningPractice, assertLearningSuccessor, learningCanonical} from '@/lib/learning-practice';

async function account() {const user = await getAppUser(); if (!user) throw new Error('Sign in to sync learning practice.'); return user.userId;}
async function profileAccess(accountId: string, workerId: string) {
  const binding = await db().prepare('SELECT account_id FROM learning_profile_accounts WHERE worker_id=?').bind(workerId).first<{account_id: string}>();
  const other = await db().prepare('SELECT 1 AS found FROM learner_links WHERE worker_id=? AND user_id IS NOT NULL AND user_id<>? LIMIT 1').bind(workerId, accountId).first();
  if ((binding && binding.account_id !== accountId) || other) throw new Error('Forbidden: this learning profile belongs to another account.');
}
function response(value: unknown, status = 200) {return Response.json(value, {status, headers: {'Cache-Control': 'no-store'}});}
function safeFailure(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (/learning-profile-account-conflict/.test(message)) return response({error: 'Forbidden: this learning profile belongs to another account.'}, 403);
  if (/learning-record-stale-parent|learning-record-identity-conflict/.test(message)) return response({error: 'Record conflict: newer formative history is already saved. Reload before syncing.'}, 409);
  if (/learning-record-storage-limit/.test(message)) return response({error: 'Training record storage limit reached. Existing records remain saved.'}, 409);
  if (message.startsWith('Record conflict')) return response({error: message}, 409);
  return failure(error);
}
export async function POST(request: Request) {
  try {
    const accountId = await account();
    const parsed = learningPracticeEnvelope.safeParse(await json(request, 900000));
    if (!parsed.success) throw new Error('Invalid formative sync envelope.');
    const minute = Math.floor(Date.now() / 60000);
    const budget = await db().prepare('INSERT INTO learning_sync_budget(account_id,minute,count) VALUES(?,?,1) ON CONFLICT(account_id,minute) DO UPDATE SET count=count+1 WHERE count<20 RETURNING count').bind(accountId, minute).first();
    if (!budget) return Response.json({error: 'Training sync limit reached. Your local records remain safe; retry shortly.'}, {status: 429, headers: {'Cache-Control': 'no-store', 'Retry-After': '60'}});
    await db().prepare('DELETE FROM learning_sync_budget WHERE minute<?').bind(minute - 60).run();
    const {workerId} = parsed.data;
    await profileAccess(accountId, workerId);
    const attempts = parsed.data.attempts.map(raw => validateLearningPractice(raw, workerId));
    if (new Set(attempts.map(a => a.id)).size !== attempts.length) throw new Error('Invalid duplicate formative record.');
    const statements = [db().prepare('INSERT INTO learning_profile_accounts(worker_id,account_id,bound_at) VALUES(?,?,?) ON CONFLICT(worker_id) DO NOTHING').bind(workerId, accountId, Date.now())];
    const accepted: {id: string; digest: string; updatedAt: number}[] = [];
    for (const record of attempts) {
      const previous = await db().prepare('SELECT payload,digest FROM learning_practice_heads WHERE account_id=? AND id=?').bind(accountId, record.id).first<{payload: string; digest: string}>();
      if (previous) assertLearningSuccessor(validateLearningPractice(JSON.parse(previous.payload), workerId), record);
      const hash = await digest(record);
      accepted.push({id: record.id, digest: hash, updatedAt: record.updatedAt});
      statements.push(db().prepare('INSERT INTO learning_practice_snapshots(account_id,id,worker_id,course_id,content_version,digest,previous_digest,updated_at,imported_at,payload) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(account_id,id,digest) DO NOTHING')
        .bind(accountId, record.id, workerId, record.courseId, record.contentVersion, hash, previous?.digest ?? null, record.updatedAt, Date.now(), learningCanonical(record)));
    }
    await db().batch(statements);
    return response({schemaVersion: 1, evidenceType: 'formative-decision-practice', certifiable: false, accepted});
  } catch (error) {return safeFailure(error);}
}
const querySchema = z.object({workerId: z.string().uuid(), limit: z.coerce.number().int().min(1).max(10).default(10), beforeAt: z.coerce.number().int().nonnegative().safe().optional(), beforeId: z.string().uuid().optional()}).strict().refine(q => (q.beforeAt === undefined) === (q.beforeId === undefined));
export async function GET(request: Request) {
  try {
    const accountId = await account();
    const parsed = querySchema.safeParse(Object.fromEntries(new URL(request.url).searchParams));
    if (!parsed.success) throw new Error('Invalid formative record filter.');
    const q = parsed.data;
    await profileAccess(accountId, q.workerId);
    const rows = await db().prepare('SELECT id,updated_at,payload FROM learning_practice_heads WHERE account_id=? AND worker_id=?' + (q.beforeAt !== undefined ? ' AND (updated_at<? OR (updated_at=? AND id<?))' : '') + ' ORDER BY updated_at DESC,id DESC LIMIT ?')
      .bind(accountId, q.workerId, ...(q.beforeAt !== undefined ? [q.beforeAt, q.beforeAt, q.beforeId!] : []), q.limit + 1).all<{id: string; updated_at: number; payload: string}>();
    const visible = rows.results.slice(0, q.limit), last = visible.at(-1);
    return response({schemaVersion: 1, evidenceType: 'formative-decision-practice', certifiable: false, records: visible.map(row => JSON.parse(row.payload)), nextCursor: rows.results.length > q.limit && last ? {beforeAt: last.updated_at, beforeId: last.id} : null});
  } catch (error) {return safeFailure(error);}
}
