import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID, createHash} from 'node:crypto';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
import catalogue from '../../../content/learning/scenarios.json';
import {validateLearningPractice, assertLearningSuccessor, learningCanonical, learningPracticeEnvelope, type LearningPractice} from '../lib/learning-practice';
function complete(scenario = catalogue.scenarios[0], workerId = randomUUID()): LearningPractice {
  let at = Date.now() - 100000; const createdAt = at, answers: LearningPractice['answers'] = [];
  for (const step of scenario.steps) {
    const choice = step.choices.find(c => c.correct)!;
    answers.push({kind: 'decision', stepId: step.id, choiceId: choice.id, correct: true, hints: 0, at: ++at});
    if ('requiresObservation' in step && step.requiresObservation) answers.push({kind: 'observation', stepId: step.id, partId: step.part!, mode: '3d', at: ++at});
    answers.push({kind: 'continue', stepId: step.id, at: ++at});
  }
  return {id: randomUUID(), workerId, scenarioId: scenario.id, courseId: scenario.course as 'fire' | 'first-aid', contentVersion: catalogue.version, index: scenario.steps.length, answers, finished: true, guided: true, evidenceType: 'formative-decision-practice', reviewStatus: 'draft', createdAt, updatedAt: at, stageHints: 0, awaitingContinue: false};
}
function initial(): LearningPractice {const r = complete(); return {...r, answers: [], index: 0, finished: false, updatedAt: r.createdAt};}
function hash(r: LearningPractice) {return createHash('sha256').update(learningCanonical(r)).digest('hex');}
function database() {const db = new DatabaseSync(':memory:'); db.exec('PRAGMA foreign_keys=ON; CREATE TABLE auth_user(id TEXT PRIMARY KEY); CREATE TABLE learner_links(worker_id TEXT, user_id TEXT); INSERT INTO auth_user VALUES(\'account-a\'),(\'account-b\');'); db.exec(readFileSync(new URL('../drizzle/0014_learning_practice.sql', import.meta.url), 'utf8')); return db;}
function bind(db: DatabaseSync, worker: string, account = 'account-a') {db.prepare('INSERT INTO learning_profile_accounts VALUES(?,?,?)').run(worker, account, Date.now());}
function write(db: DatabaseSync, r: LearningPractice, previous: string | null = null, account = 'account-a') {return db.prepare('INSERT INTO learning_practice_snapshots VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(account_id,id,digest) DO NOTHING').run(account, r.id, r.workerId, r.courseId, r.contentVersion, hash(r), previous, r.updatedAt, Date.now(), learningCanonical(r));}

test('all launch scenarios replay separately from industrial certificates', () => {
  for (const s of catalogue.scenarios) {const r = complete(s), v = validateLearningPractice(r, r.workerId); assert.equal(v.evidenceType, 'formative-decision-practice'); assert.equal(v.finished, true);}
  const fire = complete(catalogue.scenarios.find(s => s.course === 'fire')!);
  assert.throws(() => validateLearningPractice({...fire, contentVersion: '0.4.0'}, fire.workerId), /version/);
  for (const extra of [{evidenceType: 'assessment'}, {score: 100}, {certifiable: true}, {reviewStatus: 'approved'}, {owner: 'another-account'}]) assert.throws(() => validateLearningPractice({...fire, ...extra}, fire.workerId));
});
test('incomplete records and retries with accurate hint counts replay', () => {
  const r = initial(), step = catalogue.scenarios[0].steps[0]; assert.doesNotThrow(() => validateLearningPractice(r, r.workerId));
  r.answers.push({kind: 'hint', stepId: step.id, at: ++r.updatedAt}); r.stageHints = 1;
  r.answers.push({kind: 'decision', stepId: step.id, choiceId: step.choices.find(c => !c.correct)!.id, correct: false, hints: 1, at: ++r.updatedAt}); r.lastCorrect = false;
  assert.doesNotThrow(() => validateLearningPractice(r, r.workerId));
  const t = structuredClone(r); (t.answers[1] as {hints: number}).hints = 0; assert.throws(() => validateLearningPractice(t, r.workerId), /hint count/);
});
test('forged results, skipped steps, future/backwards time and foreign profiles fail', () => {
  const r = complete(); assert.throws(() => validateLearningPractice(r, randomUUID()), /another profile/);
  const edits = [(x: LearningPractice) => {(x.answers[0] as {correct: boolean}).correct = false;}, (x: LearningPractice) => {x.answers.splice(1, 1);}, (x: LearningPractice) => {x.index = 0;}, (x: LearningPractice) => {x.answers[1].at = x.createdAt - 1;}, (x: LearningPractice) => {x.answers[0].at = Date.now() + 3600000;}, (x: LearningPractice) => {x.lastCorrect = false;}, (x: LearningPractice) => {x.courseId = 'fire';}];
  for (const edit of edits) {const x = structuredClone(r); edit(x); assert.throws(() => validateLearningPractice(x, r.workerId));}
});
test('required anatomy cannot be replaced by another part or skipped', () => {
  const s = catalogue.scenarios.find(s => s.steps.some(step => 'requiresObservation' in step && step.requiresObservation))!;
  const r = complete(s), missing = structuredClone(r); missing.answers = missing.answers.filter(e => e.kind !== 'observation'); assert.throws(() => validateLearningPractice(missing, r.workerId), /anatomy/);
  const wrong = structuredClone(r), observation = wrong.answers.find(e => e.kind === 'observation')!; if (observation.kind === 'observation') observation.partId = 'FMA-foreign-part'; assert.throws(() => validateLearningPractice(wrong, r.workerId), /observation/);
});
test('only completed practice permits bounded private reflections', () => {
  const started = initial(); started.answers.push({kind: 'teach-back', text: 'Reflection', at: ++started.updatedAt}); assert.throws(() => validateLearningPractice(started, started.workerId), /reflection/);
  const done = complete(); done.answers.push({kind: 'teach-back', text: 'Describe observations without inventing a diagnosis.', at: ++done.updatedAt}); assert.doesNotThrow(() => validateLearningPractice(done, done.workerId));
  const x = structuredClone(done); (x.answers.at(-1) as {text: string}).text = 'x'.repeat(1001); assert.throws(() => validateLearningPractice(x, done.workerId));
  assert.equal(learningPracticeEnvelope.safeParse({schemaVersion: 1, workerId: done.workerId, attempts: Array(21).fill(done)}).success, false);
});
test('successors retain identity, mode and exact original events', () => {
  const p = complete(), n = structuredClone(p); assert.doesNotThrow(() => assertLearningSuccessor(p, p));
  n.answers.push({kind: 'teach-back', text: 'A private reflection', at: ++n.updatedAt}); assert.doesNotThrow(() => assertLearningSuccessor(p, n)); assert.throws(() => assertLearningSuccessor(n, p), /cannot be changed/); assert.throws(() => assertLearningSuccessor(p, {...n, guided: false}), /identity/);
  const r = structuredClone(n); r.answers[0].at++; assert.throws(() => assertLearningSuccessor(p, r), /cannot be changed/); assert.throws(() => assertLearningSuccessor(p, {...p, lastCorrect: true}), /different state/);
});
test('SQLite profile binding, immutable snapshots and stale-parent guard reject competing writers', () => {
  const db = database(); try {
    const r = complete(); bind(db, r.workerId); write(db, r); write(db, r); assert.equal(db.prepare('SELECT count(*) AS total FROM learning_practice_snapshots').get()!.total, 1);
    const n = structuredClone(r); n.answers.push({kind: 'teach-back', text: 'First next event', at: ++n.updatedAt}); write(db, n, hash(r));
    const stale = structuredClone(r); stale.answers.push({kind: 'teach-back', text: 'Competing next event', at: ++stale.updatedAt}); assert.throws(() => write(db, stale, hash(r)), /stale-parent/); assert.equal(db.prepare('SELECT digest FROM learning_practice_heads').get()!.digest, hash(n));
    assert.throws(() => write(db, r, null, 'account-b'), /account-conflict/); assert.throws(() => db.prepare('UPDATE learning_practice_snapshots SET payload=?').run('{}'), /immutable/);
    db.prepare('DELETE FROM auth_user WHERE id=?').run('account-a'); assert.equal(db.prepare('SELECT count(*) AS total FROM learning_practice_snapshots').get()!.total, 0);
  } finally {db.close();}
});
test('SQLite protects an existing centre link and permits whole-batch rollback', () => {
  const db = database(); try {
    const r = complete(); db.prepare('INSERT INTO learner_links VALUES(?,?)').run(r.workerId, 'account-b'); db.exec('BEGIN');
    try {bind(db, r.workerId); write(db, r); assert.fail('Expected conflict');} catch (e) {assert.match(String(e), /account-conflict/); db.exec('ROLLBACK');}
    assert.equal(db.prepare('SELECT count(*) AS total FROM learning_profile_accounts').get()!.total, 0); assert.equal(db.prepare('SELECT count(*) AS total FROM learning_practice_heads').get()!.total, 0);
  } finally {db.close();}
});
