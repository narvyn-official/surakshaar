import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {account, localDatabase} from './auth-client.mjs';
import catalogue from '../../../content/learning/scenarios.json';
import type {LearningPractice} from '../lib/learning-practice';
const origin = 'http://localhost:5173';
const users: Awaited<ReturnType<typeof account>>[] = [];
async function call(person: typeof users[number] | null, path: string, body?: unknown) {
  const response = await fetch(origin + '/api/' + path, {method: body ? 'POST' : 'GET', headers: {Origin: origin, 'Content-Type': 'application/json', ...(person ? {Cookie: person.cookie} : {})}, body: body ? JSON.stringify(body) : undefined});
  return {status: response.status, data: await response.json() as Record<string, unknown>};
}
try {
  const owner = await account(); users.push(owner); const other = await account(); users.push(other);
  const scenario = catalogue.scenarios.find(s => s.id === 'school-fire-help')!;
  let at = Date.now() - 100000;
  const createdAt = at, answers: LearningPractice['answers'] = [];
  for (const step of scenario.steps) {
    const choice = step.choices.find(c => c.correct)!;
    answers.push({kind: 'decision', stepId: step.id, choiceId: choice.id, correct: true, hints: 0, at: ++at});
    answers.push({kind: 'continue', stepId: step.id, at: ++at});
  }
  const record: LearningPractice = {id: randomUUID(), workerId: randomUUID(), scenarioId: scenario.id, courseId: 'fire', contentVersion: catalogue.version, index: scenario.steps.length, answers, finished: true, guided: true, evidenceType: 'formative-decision-practice', reviewStatus: 'draft', createdAt, updatedAt: at, stageHints: 0, awaitingContinue: false};
  const envelope = (value: unknown) => ({schemaVersion: 1, workerId: record.workerId, attempts: [value]});
  const path = 'learning-practice?workerId=' + record.workerId;
  assert.equal((await call(null, path)).status, 401);
  assert.equal((await call(null, 'learning-practice', envelope(record))).status, 401);
  const imported = await call(owner, 'learning-practice', envelope(record)); assert.equal(imported.status, 200, JSON.stringify(imported)); assert.equal(imported.data.certifiable, false);
  assert.equal((await call(owner, 'learning-practice', envelope(record))).status, 200);
  assert.equal((await call(other, path)).status, 403);
  assert.equal((await call(other, 'learning-practice', envelope(record))).status, 403);
  assert.equal((await call(owner, 'learning-practice', envelope({...record, score: 100}))).status, 400);
  assert.equal((await call(owner, 'learning-practice', envelope({...record, guided: false}))).status, 409);
  const forged = structuredClone(record); if (forged.answers[0].kind === 'decision') forged.answers[0].correct = false;
  assert.equal((await call(owner, 'learning-practice', envelope(forged))).status, 400);
  const next = structuredClone(record); next.answers.push({kind: 'teach-back', text: 'Synthetic reflection, no personal information.', at: ++next.updatedAt});
  assert.equal((await call(owner, 'learning-practice', envelope(next))).status, 200);
  assert.equal((await call(owner, 'learning-practice', envelope(record))).status, 409);
  const another = {...record, id: randomUUID()}; assert.equal((await call(owner, 'learning-practice', envelope(another))).status, 200);
  const list = await call(owner, path + '&limit=1'); assert.equal(list.status, 200); assert.equal((list.data.records as unknown[]).length, 1);
  const cursor = list.data.nextCursor as {beforeAt: number; beforeId: string}; assert(cursor);
  const page2 = await call(owner, path + '&limit=1&beforeAt=' + cursor.beforeAt + '&beforeId=' + cursor.beforeId); assert.equal((page2.data.records as unknown[]).length, 1); assert.equal(page2.data.nextCursor, null);
  const context = {courseId: 'first-aid', contentVersion: catalogue.version, scenarioId: 'adult-collapse', objectiveId: 'safe', recentActionIds: [], hintLevel: 'controls', language: 'en', mode: 'learn', allowExternalCoaching: false};
  assert.equal((await call(null, 'tutor/context', context)).status, 401);
  const controls = await call(owner, 'tutor/context', context); assert.equal(controls.status, 200, JSON.stringify(controls)); assert.equal(controls.data.mode, 'authored-reference');
  assert.equal((await call(owner, 'tutor/context', {...context, hintLevel: 'hint'})).data.status, 'review-required');
  assert.equal((await call(owner, 'tutor/context', {...context, mode: 'assessment'})).data.status, 'assessment-blocked');
  assert.equal((await call(owner, 'tutor/context', {...context, courseId: 'fire'})).status, 400);
  assert.equal((await call(other, 'tutor/context', {...context, workerId: record.workerId})).status, 403);
  const db = localDatabase(); try {
    for (const table of ['attempts', 'credentials', 'certification_requests']) assert.equal(db.prepare(`SELECT count(*) AS total FROM ${table} WHERE owner=?`).get(owner.userId)!.total, 0);
    assert.equal(db.prepare('SELECT count(*) AS total FROM learning_practice_snapshots WHERE account_id=?').get(owner.userId)!.total, 3);
  } finally {db.close();}
  console.log('Learning integration PASS: independent account sync, auth/isolation, replay rejection, immutable appends, idempotency, pagination, private reflections, legacy certificate separation and contextual coach gates.');
} finally {
  const db = localDatabase(); try {
    for (const user of users) {
      for (const table of ['tutor_budget', 'tutor_hour_budget']) db.prepare(`DELETE FROM ${table} WHERE account=?`).run(user.userId);
      for (const table of ['learning_practice_snapshots', 'learning_practice_heads', 'learning_profile_accounts', 'learning_sync_budget']) db.prepare(`DELETE FROM ${table} WHERE account_id=?`).run(user.userId);
      db.prepare('DELETE FROM auth_user WHERE id=?').run(user.userId);
    }
  } finally {db.close();}
}
