import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID,randomBytes} from 'node:crypto';
import {allowedScenarios,parseStudioSaved,practiceAction,startPractice,studioScenarios} from '../lib/studio-learning';
import {decryptEvidence,encryptEvidence,identityReady,nextCapture,validateJpeg} from '../lib/identity-evidence';
test('all studio scenarios complete only after safe decisions and required exploration',()=>{
 for(const scenario of studioScenarios){let at=Date.now()-10000;let record=startPractice(scenario.id,randomUUID(),true,at++);
 for(const step of scenario.steps){assert.throws(()=>practiceAction(record,{kind:'continue'},at++));const wrong=step.choices.find(c=>!c.correct);if(wrong){record=practiceAction(record,{kind:'decision',choiceId:wrong.id},at++);assert.equal(record.awaitingContinue,false);}
 record=practiceAction(record,{kind:'decision',choiceId:step.choices.find(c=>c.correct)!.id},at++);
 if(step.requiresObservation){assert.throws(()=>practiceAction(record,{kind:'continue'},at++));record=practiceAction(record,{kind:'observation'},at++);}
 record=practiceAction(record,{kind:'continue'},at++);}
 assert.equal(record.finished,true);assert.equal(record.index,scenario.steps.length);assert(parseStudioSaved(JSON.stringify({schema:1,workerId:record.workerId,records:[record]})));
 assert.throws(()=>practiceAction(record,{kind:'hint'}));}
});
test('school catalogue stays separate and malformed local state cannot become progress',()=>{assert(allowedScenarios('fire','school-awareness').every(s=>s.audience==='awareness'));assert.equal(parseStudioSaved('{invalid'),null);assert.equal(parseStudioSaved(JSON.stringify({schema:1,workerId:'someone',records:[]})),null);});
test('private identity evidence authenticates its binding and rejects tampering',()=>{const key=Buffer.from(randomBytes(32)).toString('base64'),photo=Buffer.concat([Buffer.from([255,216,255]),Buffer.alloc(128,7),Buffer.from([255,217])]);assert(identityReady(key));assert(!identityReady('bad'));assert.deepEqual(validateJpeg(photo.toString('base64')),photo);assert.throws(()=>validateJpeg(Buffer.from('not a JPEG').toString('base64')));const sealed=encryptEvidence(photo,'session:frame',key);assert(!sealed.includes(photo.toString('base64')));assert.deepEqual(decryptEvidence(sealed,'session:frame',key),photo);assert.throws(()=>decryptEvidence(sealed,'other:frame',key));assert.throws(()=>decryptEvidence(sealed,'session:frame',Buffer.from(randomBytes(32)).toString('base64')));const changed=Buffer.from(sealed,'base64');changed[30]^=1;assert.throws(()=>decryptEvidence(changed.toString('base64'),'session:frame',key));});
test('capture schedule remains bounded without a continuous recording loop',()=>{assert.equal(nextCapture(1000,()=>0),31000);assert(nextCapture(1000,()=>.999999)<91000);});
