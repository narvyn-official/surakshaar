import {z} from 'zod';
import current from '../../../content/learning/scenarios.json';
import archive from '../../../content/learning/archive/learning-2026.10.10-draft1.json';
import type {LearningPractice} from './learning-practice';
export const shareToken=z.string().regex(/^[A-Za-z0-9_-]{43}$/);
export const shareCommand=z.discriminatedUnion('action',[
 z.object({action:z.literal('create'),id:z.string().uuid(),workerId:z.string().uuid(),attemptId:z.string().uuid(),expiresInDays:z.union([z.literal(1),z.literal(7),z.literal(30)]),confirmPublicMetadata:z.literal(true)}).strict(),
 z.object({action:z.literal('revoke'),id:z.string().uuid(),workerId:z.string().uuid(),attemptId:z.string().uuid()}).strict(),
]);
export function newShareToken(){return btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(32)))).replaceAll('+','-').replaceAll('/','_').replace(/=+$/,'')}
/** Deliberate projection. Never publish identity, answers, reflection, scores or internal record IDs. */
export function learningShareMetadata(record:LearningPractice){
 if(!record.finished)throw new Error('Invalid public share: complete the practice first.');
 const catalogue=record.contentVersion===current.version?current:record.contentVersion===archive.version?archive:null;
 const scenario=catalogue?.scenarios.find(s=>s.id===record.scenarioId&&s.course===record.courseId);
 if(!scenario)throw new Error('Unsupported public practice content version.');
 return {course:record.courseId==='fire'?'Fire Safety':'First Aid',scenario:scenario.title,contentVersion:record.contentVersion,mode:record.guided?'Guided practice':'Independent practice',completed:true,evidenceType:'formative-decision-practice',reviewStatus:'draft',certifiable:false,scope:'Recorded learning decisions only. Not a professional certificate, identity proof, practical skill assessment or clinical approval.'};
}
export function publicLearningShare(row:{metadata:string;expires_at:number;revoked_at:number|null;created_at:number}|null,now=Date.now()){
 if(!row)return {schemaVersion:1,checkedAt:now,status:'unavailable',certifiable:false};
 if(row.revoked_at!==null)return {schemaVersion:1,checkedAt:now,status:'revoked',certifiable:false};
 if(row.expires_at<=now)return {schemaVersion:1,checkedAt:now,status:'expired',certifiable:false};
 return {schemaVersion:1,checkedAt:now,status:'active',certifiable:false,record:JSON.parse(row.metadata) as ReturnType<typeof learningShareMetadata>,sharedAt:row.created_at,expiresAt:row.expires_at};
}
