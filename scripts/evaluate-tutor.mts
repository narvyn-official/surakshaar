import assert from 'node:assert/strict';
import {writeFileSync,readFileSync,existsSync} from 'node:fs';
import {createHash} from 'node:crypto';
import bank from '../apps/admin-web/lib/question-bank.json';
import {tutor} from '../apps/admin-web/lib/tutor';
const cases=[];
const nativeLog='artifacts/qa-0.11-current/ordinary.log';
const nativeMeta='artifacts/qa-0.11-current/build-device.json';
const apk='artifacts/surakshaar-0.11.0-dev-debug.apk';
const nativeCurrent=existsSync(nativeLog)&&existsSync(nativeMeta)&&existsSync(apk)&&
 /OK \(\d+ tests\)/.test(readFileSync(nativeLog,'utf8'))&&
 JSON.parse(readFileSync(nativeMeta,'utf8')).apkSha256===createHash('sha256').update(readFileSync(apk)).digest('hex');
const apiLog='artifacts/tutor-0.11-integration.json';
const apiPassed=existsSync(apiLog)&&JSON.parse(readFileSync(apiLog,'utf8')).checks.every((c:{status:string})=>c.status==='Pass');
const journeyLog='artifacts/journey-0.11-integration.log';
const journeyPassed=existsSync(journeyLog)&&readFileSync(journeyLog,'utf8').includes('Journey integration PASS');
const groups:Record<string,{scope:string;gap:string;tests:string[]}>= {
 start:{scope:'Native navigation, lesson bookmarks, Back and visible narration controls.',gap:'Real spoken output, call/audio interruptions and complete Santali remain unverified.',tests:['MainNavigationTest','AndroidBackAndLearningTest','LearningWorkspaceTest']},
 account:{scope:'Native worker/profile isolation and encrypted queue account binding; local linked-account tutor denial.',gap:'Complete deployed enrolment/recovery and shared-phone usability review remain separate gates.',tests:['WorkerProfileStoreTest','WorkerProfileLifecycleTest','LearningJourneyFlowTest']},
 ar:{scope:'Emulator placement/permission/recovery state and screen-fallback guards.',gap:'No AR services or physical phone: surface acquisition, real placement, tracking and calls are not proven.',tests:['ArRuntimePermissionsTest','ArRecoveryTest','CameraFallbackTest','RoomPlacementPreviewTest']},
 fire:{scope:'Existing screen fire mission/recall and procedure interactions; new critical transfer decisions replayed in model tests.',gap:'All new safety scenarios are drafts. No current physical fire AR or competent agent/equipment/site review.',tests:['RoomMissionFlowTest','RoomRecallFlowTest','ProcedureFlowTest','LearningWorkspaceTest']},
 gas:{scope:'Existing screen gas boundary/attendant/PPE/buddy behavior and procedure flow; draft-source label inspected.',gap:'No real gas sensing or physical gas AR proof. Approved permit, instrument and rescue procedures need review.',tests:['RoomMissionFlowTest','ProcedureFlowTest','LearningWorkspaceTest']},
 machinery:{scope:'Existing screen procedure and authored stored-energy/guard/handover draft decisions replayed in model tests.',gap:'Not machinery-specific reviewed AR; source/energy-isolation applicability requires expert approval.',tests:['ProcedureFlowTest']},
 ppe:{scope:'Existing screen procedure and authored damage/compatibility/fit-limit draft decisions replayed in model tests.',gap:'No real fit or exposure validation, expert review or full practical competence observation.',tests:['ProcedureFlowTest']},
 emergency:{scope:'Screen evacuation/blocked-route/accountability procedure and authored critical transfer model decisions.',gap:'Site route, role and emergency-plan review plus real-device practical trials remain unverified.',tests:['EmergencyFlowTest','ProcedureFlowTest']},
 learning:{scope:'Demonstration no-credit boundary, read-only decision divergence, targeted formative model records and separate passport labels.',gap:'Objective mappings and learning effectiveness need instructional review; explain-back and delayed transfer evaluation are missing.',tests:['LearningJourneyFlowTest','LearningWorkspaceTest','ProcedureFlowTest']},
 credential:{scope:'Current native signature/QR/expiry regression uses synthetic tokens; existing protocol retained.',gap:'This does not prove current deployed independent issuance, correction or public privacy workflow. See local journey integration separately.',tests:['GovernanceCredentialTest','CredentialExpiryTest']},
 ai:{scope:'Bounded product-reference policy plus local authenticated API quotas, draft-source refusal and cross-account denial.',gap:'No live provider, approved safety retrieval, explain-back, audio/transcription or real learner evaluation.',tests:['tutor.test.ts','test-tutor-local.mjs']},
 offline:{scope:'Bundled content, saved-state restoration, encrypted failed-upload retry/account binding, transaction-failure fixture and emulator heat guard.',gap:'Physical restart/process death under storage exhaustion, real thermal/battery behavior and full production-signed upgrades remain unverified.',tests:['LearningJourneyFlowTest','SkillPracticeStoreTest','ArRecoveryTest','RoomMissionWorkspaceTest']},
};
const scopedPass:Record<string,string>={
 'start-01':'Training/course/lesson action reached in native UI.',
 'start-06':'System Back and overlay return destinations exercised in the emulator.',
 'account-01':'Signed-out local lessons, procedures and formative practice exercised.',
 'account-04':'Failed queue retry remains encrypted and refuses a different account; synthetic transport only.',
 'ar-07':'Permission/AR-unavailable alternatives preserve screen attribution and do not award camera actions.',
 'learning-01':'Original stopped procedure displays its recorded divergence without modifying its evidence.',
 'ai-01':'Unknown or unapproved context safely clarifies within the deterministic reference service.',
 'ai-02':'Conflicting-procedure request clarifies; English/Hindi code-switch fixture passed.',
 'ai-03':'Unspecified machine/operation safely clarifies.',
 'ai-05':'Bypass/privilege/assessment requests refuse without a provider call.',
 'ai-06':'Private-record requests refuse and both cross-account API directions return 403.',
 'ai-07':'Provider failure/timeout has deterministic fallback; bundled guide needs no provider.',
 'offline-03':'Synthetic failed retry preserves identical payload; unchanged evidence is not resent.',
};
for(const group of bank.areas)for(const c of group.cases){const reply=await tutor(c.question,'en');const source=bank.sources[c.sourceId as keyof typeof bank.sources];assert.ok(reply.answer);assert.ok(['supported','clarify','declined'].includes(reply.status));if(source.status==='draft'){assert.equal(reply.status,'clarify');assert.equal(reply.reason,'review-required')}
const spec=groups[group.id];const checked=group.id==='ai'?apiPassed:nativeCurrent;
cases.push({id:c.id,area:group.id,intendedUser:['credential','ai'].includes(group.id)?'Learner / authorised staff / verifier as applicable':'Learner',objective:c.objective,question:c.question,requiredContext:c.requiredContext,expectedBehavior:c.behavior,source:source.title,sourceVersion:source.version,sourceStatus:source.status,sourceReference:'url' in source?source.url:source.path,sourceReviewer:source.reviewer,expectedResponse:c.response,online:c.online,offline:c.offline,languages:c.languages,policyResult:'Pass',policyScope:'Exact-question response/source boundary, not expert validation or every field behavior',actualReplyStatus:reply.status,actualReplyReason:reply.reason,actualBehaviorResult:checked?(scopedPass[c.id]?'Pass':'Partial'):'Not tested',actualBehaviorScope:checked?(scopedPass[c.id]??spec.scope):'Current matching build evidence unavailable',method:spec.tests.join('; '),evidence:['artifacts/web-0.11-tests.log',...(checked?[group.id==='ai'?apiLog:nativeLog]:[]),...(journeyPassed&&['account','credential','learning','offline'].includes(group.id)?[journeyLog]:[])],gap:spec.gap})}
writeFileSync('docs/surakshaar-0.11.0/question-coverage.json',JSON.stringify({date:'2026-10-04',version:bank.version,scope:'Every supplied question; Pass means only its explicit emulator/local service scope. Policy responses do not prove safety accuracy or physical AR.',cases},null,2)+'\n');
const columns=['id','area','intendedUser','objective','question','requiredContext','expectedBehavior','source','sourceVersion','sourceStatus','sourceReference','sourceReviewer','expectedResponse','online','offline','languages','policyResult','policyScope','actualReplyStatus','actualReplyReason','actualBehaviorResult','actualBehaviorScope','method','evidence','gap'];
const csv=(value:unknown)=>'"'+(typeof value==='object'?JSON.stringify(value):String(value??'')).replaceAll('"','""')+'"';
writeFileSync('docs/surakshaar-0.11.0/question-coverage.csv',columns.join(',')+'\n'+cases.map(c=>columns.map(k=>csv(c[k as keyof typeof c])).join(',')).join('\n')+'\n');
console.log(`${cases.length} question cases evaluated. Product/field behavior is not promoted to Pass from policy evaluation.`);
