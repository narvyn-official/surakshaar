import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
const load=p=>JSON.parse(readFileSync(new URL(p,import.meta.url)));
assert.deepEqual(load('../content/curriculum.json'),load('../apps/admin-web/lib/curriculum.json'));
assert.deepEqual(load('../content/trusted-issuer.json'),load('../apps/admin-web/lib/trusted-issuer.json'));
console.log('Android and dashboard curriculum and issuer trust match.');

const bank=load('../content/question-bank.json'),transfer=load('../content/transfer-scenarios.json');
assert.deepEqual(bank,load('../apps/admin-web/lib/question-bank.json'));
const cases=bank.areas.flatMap(a=>a.cases);
assert.equal(cases.length,81);assert.equal(new Set(cases.map(c=>c.id)).size,cases.length);
for(const c of cases){for(const key of ['question','behavior','requiredContext','objective','sourceId','online','offline'])assert.ok(c[key],`${c.id}: ${key}`);assert.ok(bank.sources[c.sourceId]);assert.equal(c.verification.status,'Not tested','Authored expectations must not masquerade as behavior proof');}
assert.equal(transfer.reviewStatus,'draft');assert.equal(transfer.scenarios.length,15);
for(const objective of transfer.objectives){for(const key of ['recognise','decide','do','refuseOrEscalate','misconception','observableEvidence','remediation','sourceId','contentVersion'])assert.ok(objective[key],`${objective.id}: ${key}`);assert.equal(objective.reviewer,null);assert.equal(objective.reviewStatus,'draft');assert.ok(bank.sources[objective.sourceId]);}
for(const module of ['fire','gas','machinery','ppe','emergency'])assert.deepEqual(new Set(transfer.scenarios.filter(s=>s.module===module).map(s=>s.condition)),new Set(['normal','abnormal','uncertain']));
for(const scene of transfer.scenarios){assert.ok(transfer.objectives.some(o=>o.id===scene.objectiveId));assert.equal(scene.questions.length,2);for(const q of scene.questions){assert.equal(q.choices.filter(c=>c.correct).length,1);assert.ok(q.explanation);assert.equal(q.critical,true)}}
console.log('81 questions, 13 draft objectives and 15 domain variations validated; no safety approval inferred.');
