import {z} from 'zod';
export const aspects=['instruction','model-detail','assessment'] as const;
export const aspectNames={'instruction':'Instruction','model-detail':'3D / AR detail or label','assessment':'Practice question'};
const id=z.string().min(1).max(100);
export const lessonCommand=z.discriminatedUnion('action',[
 z.object({action:z.literal('link'),sourceId:id,contentVersion:id,scenarioId:id,stepId:id,aspect:z.enum(aspects),rationale:z.string().trim().min(10).max(1000)}).strict(),
 z.object({action:z.literal('unlink'),id,revision:z.number().int().min(1)}).strict(),
 z.object({action:z.literal('assess'),id,revision:z.number().int().min(1),versionId:id,decision:z.enum(['no-change','revision-needed']),notes:z.string().trim().min(10).max(2000)}).strict(),
]);
export type LessonCommand=z.infer<typeof lessonCommand>;
export type LessonTarget={id:string;title:string;course:string;steps:{id:string;title:string;question:string;part:string|null}[]};
export type LessonLink={id:string;source_id:string;content_version:string;scenario_id:string;step_id:string;aspect:typeof aspects[number];rationale:string;revision:number;reviewed_version_id:string|null;decision:'no-change'|'revision-needed'|null;review_notes:string;reviewed_at:number|null;source_title:string;source_status:string;source_revision:number;latest_id:string|null;latest_revision:number|null;latest_at:number|null;state:'awaiting-reference'|'source-disabled'|'curriculum-changed'|'needs-review'|'no-change'|'revision-needed'};
export type LessonPanel={contentVersion:string;targets:LessonTarget[];links:LessonLink[];sources:{id:string;title:string;status:string}[];canReview:boolean};
