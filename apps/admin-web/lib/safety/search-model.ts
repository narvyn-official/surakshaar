import {z} from 'zod';
export const equipmentCommand=z.object({
 action:z.literal('search'),host:z.string().min(3).max(200),
 model:z.string().trim().min(2).max(100).regex(/^[\p{L}\p{N} .()+/\-]+$/u,'Use a public make and model only.'),
 publicOnly:z.literal(true),
}).strict();
export type EquipmentQuery=z.infer<typeof equipmentCommand>;
export type EquipmentResult={title:string;url:string;snippet:string;host:string};
export type EquipmentSearch={id:string;model:string;host:string;created_at:number;status:'pending'|'completed'|'failed';results:EquipmentResult[];error:string|null};
export type EquipmentPanel={hosts:string[];configured:boolean;operator:boolean;searches:EquipmentSearch[]};
