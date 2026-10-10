import catalogue from '../../../content/learning/catalog.json';
import scenarios from '../../../content/learning/scenarios.json';
import {validateLearningPractice,type LearningPractice} from './learning-practice';

export const studioCatalogue=catalogue;
export const studioScenarios=scenarios.scenarios;
export const studioVersion=scenarios.version;
export type StudioScenario=typeof studioScenarios[number];
export type StudioStep=StudioScenario['steps'][number];
export type StudioAudience='adult-learning'|'school-awareness';
export type StudioLanguage='en'|'hi';
export function localText(item:unknown,key:string,language:StudioLanguage):string {
 const row=(item??{}) as Record<string,unknown>;const result=language==='hi'?row[key+'Hi']:undefined;
 return typeof result==='string'&&result?result:typeof row[key]==='string'?row[key] as string:'';
}
export function allowedScenarios(course:string,audience:StudioAudience){
 const entry=catalogue.active.find(c=>c.id===course);const allowed=audience==='school-awareness'?entry?.awarenessScenarios:entry?.adultScenarios;
 return studioScenarios.filter(s=>allowed?.includes(s.id));
}
export function startPractice(scenarioId:string,workerId:string,guided:boolean,now=Date.now()):LearningPractice {
 const scenario=studioScenarios.find(s=>s.id===scenarioId);if(!scenario)throw new Error('Choose an available scenario.');
 return {id:crypto.randomUUID(),workerId,scenarioId,courseId:scenario.course as 'fire'|'first-aid',contentVersion:studioVersion,index:0,answers:[],finished:false,guided,evidenceType:'formative-decision-practice',reviewStatus:'draft',createdAt:now,updatedAt:now,stageHints:0,awaitingContinue:false};
}
export function practiceAction(record:LearningPractice,action:{kind:'decision';choiceId:string}|{kind:'continue'}|{kind:'hint'}|{kind:'observation'},now=Date.now()):LearningPractice {
 const scenario=studioScenarios.find(s=>s.id===record.scenarioId);const step=scenario?.steps[record.index];
 if(!step||record.finished)throw new Error('This practice is already complete.');
 const next=structuredClone(record),at=Math.max(now,record.updatedAt);next.updatedAt=at;
 if(action.kind==='decision'){
  if(record.awaitingContinue)throw new Error('Continue before choosing another answer.');
  const choice=step.choices.find(c=>c.id===action.choiceId);if(!choice)throw new Error('Choose an available answer.');
  next.answers.push({kind:'decision',stepId:step.id,choiceId:choice.id,correct:choice.correct,at,hints:record.stageHints??0});next.awaitingContinue=choice.correct;next.lastCorrect=choice.correct;
 }else if(action.kind==='hint'){
  if(!record.guided)throw new Error('Hints are available in guided practice.');
  next.answers.push({kind:'hint',stepId:step.id,at});next.stageHints=(record.stageHints??0)+1;
 }else if(action.kind==='observation'){
  if(!step.part)throw new Error('There is no required body structure at this step.');
  next.answers.push({kind:'observation',stepId:step.id,partId:step.part,mode:'3d',at});
 }else{
  if(!record.awaitingContinue)throw new Error('Choose the safe decision before continuing.');
  if(step.requiresObservation&& !record.answers.some(e=>e.kind==='observation'&&e.stepId===step.id))throw new Error('Explore the indicated structure before continuing.');
  next.answers.push({kind:'continue',stepId:step.id,at});next.index++;next.finished=next.index===scenario!.steps.length;next.awaitingContinue=false;next.stageHints=0;
 }
 return validateLearningPractice(next,record.workerId,at);
}
export type StudioSaved={schema:1;workerId:string;records:LearningPractice[]};
export function parseStudioSaved(raw:string|null):StudioSaved|null {
 if(!raw||raw.length>2_000_000)return null;
 try{const value=JSON.parse(raw);if(value.schema!==1||typeof value.workerId!=='string'||!/^([a-f0-9]{8}-[a-f0-9]{4}-[1-8][a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12})$/i.test(value.workerId)||!Array.isArray(value.records)||value.records.length>100)return null;const records=value.records.map((r:unknown)=>validateLearningPractice(r,value.workerId));return {schema:1,workerId:value.workerId,records};}catch{return null;}
}
