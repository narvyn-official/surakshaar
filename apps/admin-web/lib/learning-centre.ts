import {z} from 'zod';
import catalogue from '../../../content/learning/catalog.json';
import scenarios from '../../../content/learning/scenarios.json';
import {centreId} from './learner-scope';
import type {LearningPractice} from './learning-practice';

export const uuid = z.string().uuid();
const stamp = z.number().int().nonnegative().safe();
export const reviewOutcomes = ['reviewed', 'repeat-practice', 'needs-information'] as const;
export const centreCommand = z.discriminatedUnion('action', [
  z.object({action:z.literal('assign'), id:uuid, workerIds:z.array(uuid).min(1).max(200), courseId:z.enum(['fire','first-aid']), contentVersion:z.string().max(100), audience:z.enum(['school-awareness','adult-learning']), scenarioIds:z.array(z.string().max(100)).min(1).max(50), mode:z.enum(['guided','independent']), dueAt:stamp, note:z.string().trim().max(500)}).strict(),
  z.object({action:z.literal('cancel'), assignmentId:uuid, reason:z.string().trim().min(5).max(500)}).strict(),
  z.object({action:z.literal('review'), id:uuid, submissionId:z.string().regex(/^[a-f0-9]{64}$/), previousEventId:uuid.nullable(), outcome:z.enum(reviewOutcomes), note:z.string().trim().min(10).max(1000)}).strict(),
  z.object({action:z.literal('observe'), id:uuid, assignmentId:uuid, observedAt:stamp, setting:z.enum(['in-person-training','remote-simulation']), rubric:z.object({communication:z.boolean(),sequence:z.boolean(),safeDecisions:z.boolean()}).strict(), outcome:z.enum(['observed','further-practice']), note:z.string().trim().min(10).max(1000)}).strict(),
]);
export const enrolmentCommand = z.discriminatedUnion('action', [
  z.object({action:z.literal('submit'),workerId:uuid,owner:centreId.optional(),assignmentId:uuid,attemptIds:z.array(uuid).min(1).max(20),shareEvidence:z.literal(true)}).strict(),
  z.object({action:z.literal('withdraw'),workerId:uuid,owner:centreId.optional(),submissionId:z.string().regex(/^[a-f0-9]{64}$/)}).strict(),
  z.object({action:z.literal('correction'),id:uuid,workerId:uuid,owner:centreId.optional(),submissionId:z.string().regex(/^[a-f0-9]{64}$/),previousEventId:uuid.nullable(),note:z.string().trim().min(10).max(1000)}).strict(),
]);
export type LaunchAssignment = {id:string;owner:string;worker_id:string;course_id:string;content_version:string;audience:string;scenario_ids:string;mode:string;due_at:number;created_at:number;cancelled_at:number|null;catalogue_snapshot:string;note:string};
export type Submission = {id:string;assignment_id:string;account_id:string;worker_id:string;scenario_id:string;source_digest:string;created_at:number;withdrawn_at:number|null;evidence:string};
export const launchCatalogue = {version:scenarios.version,courses:catalogue.active.map(course=>({id:course.id,title:course.title,titleHi:course.titleHi,adultScenarios:course.adultScenarios,awarenessScenarios:course.awarenessScenarios})),scenarios:scenarios.scenarios.map(s=>({id:s.id,courseId:s.course,title:s.title,reviewStatus:s.reviewStatus})),certifiable:false};

export function validateAssignment(command: Extract<z.infer<typeof centreCommand>,{action:'assign'}>, now=Date.now()) {
  if(command.contentVersion!==scenarios.version) throw new Error('Unsupported assignment content version. Refresh the catalogue.');
  if(new Set(command.workerIds).size!==command.workerIds.length||new Set(command.scenarioIds).size!==command.scenarioIds.length) throw new Error('Invalid duplicate learner or scenario.');
  const course=catalogue.active.find(c=>c.id===command.courseId);
  const allowed=command.audience==='school-awareness'?course?.awarenessScenarios:course?.adultScenarios;
  if(!allowed||command.scenarioIds.some(s=>!allowed.includes(s))) throw new Error('Invalid scenario selection for this course and audience.');
  if(command.dueAt<=now||command.dueAt>now+366*86400000*2) throw new Error('Invalid due date. Choose a future date within two years.');
  return {version:scenarios.version,course:course!.title,audience:command.audience,reviewStatus:'draft',certifiable:false,scenarios:command.scenarioIds.map(id=>{const s=scenarios.scenarios.find(s=>s.id===id)!;return {id:s.id,title:s.title,steps:s.steps.map(step=>({id:step.id,title:step.title,choices:step.choices.map(c=>({id:c.id,label:c.text}))}))};})};
}
/** Deliberate minimum: private free-text teach-back is never shared with a centre. */
export function sharedPractice(record:LearningPractice) {
  return {id:record.id,workerId:record.workerId,scenarioId:record.scenarioId,courseId:record.courseId,contentVersion:record.contentVersion,createdAt:record.createdAt,updatedAt:record.updatedAt,finished:record.finished,guided:record.guided,evidenceType:record.evidenceType,reviewStatus:record.reviewStatus,certifiable:false,reflectionIncluded:false,
    events:record.answers.filter(e=>e.kind!=='teach-back'),
    decisions:record.answers.filter(e=>e.kind==='decision').length,
    mistakes:record.answers.filter(e=>e.kind==='decision'&&!e.correct).length,
    hints:record.answers.filter(e=>e.kind==='hint').length};
}
export function requireMatchingPractice(assignment:LaunchAssignment,record:LearningPractice) {
  if(assignment.cancelled_at!==null)throw new Error('Record conflict: this assignment has been cancelled.');
  if(record.workerId!==assignment.worker_id||record.courseId!==assignment.course_id||record.contentVersion!==assignment.content_version||!JSON.parse(assignment.scenario_ids).includes(record.scenarioId))throw new Error('Invalid practice for this assignment or content version.');
  if(!record.finished||record.guided!==(assignment.mode==='guided')||record.createdAt<assignment.created_at)throw new Error('Invalid practice: complete a new assigned-mode attempt after the assignment was created.');
}
export function assignmentProgress(assignment:LaunchAssignment,records:Submission[],now=Date.now()) {
  const required=JSON.parse(assignment.scenario_ids) as string[];
  const completed=new Set(records.filter(s=>s.assignment_id===assignment.id&&s.withdrawn_at===null).map(s=>s.scenario_id));
  const complete=required.every(id=>completed.has(id));
  return {required:required.length,completed:required.filter(id=>completed.has(id)).length,status:assignment.cancelled_at!==null?'cancelled':complete?'practice-complete':assignment.due_at<now?'overdue':'assigned',certifiable:false};
}
export function csvCell(value:unknown) {
  const text=String(value??'');return '"'+(/^[\s]*[=+@-]/.test(text)?"'":"")+text.replaceAll('"','""')+'"';
}
