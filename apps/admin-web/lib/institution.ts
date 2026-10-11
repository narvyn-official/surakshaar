import {z} from 'zod';
export const institutionPolicy=z.object({
 name:z.string().trim().min(2).max(120),
 assessment:z.enum(['self-directed','supervised','snapshots']),
 maximumAttempts:z.number().int().min(1).max(20),
 requirePractical:z.boolean(),separateIssuer:z.boolean(),
 certificateDays:z.number().int().min(1).max(366),
 support:z.string().trim().min(10).max(500),
 privacyNotice:z.string().trim().min(80).max(4000),
}).strict();
export type InstitutionPolicy=z.infer<typeof institutionPolicy>;
export const defaultPolicy:InstitutionPolicy={name:'Formative learning',assessment:'self-directed',maximumAttempts:3,requirePractical:false,separateIssuer:true,certificateDays:180,support:'Contact your institution administrator for assistance or an alternative assessment.',privacyNotice:'Your institution uses assigned learning records to provide feedback. Personal learning stays separate. Camera checks are disabled unless separately explained and enabled for adult learners. Contact your institution to request correction or review.'};
const uuid=z.string().uuid();
export const importPerson=z.object({employeeNumber:z.string().trim().min(1).max(80),name:z.string().trim().min(1).max(80),sector:z.enum(['Unspecified','Mining','Steel','Mica','Other']).default('Unspecified')}).strict();
export const institutionCommand=z.discriminatedUnion('action',[
 z.object({action:z.literal('unit'),id:uuid,kind:z.enum(['branch','department','cohort']),name:z.string().trim().min(2).max(100),parentId:uuid.nullable()}).strict(),
 z.object({action:z.literal('archive-unit'),id:uuid}).strict(),
 z.object({action:z.literal('import'),id:uuid,people:z.array(importPerson).min(1).max(200),unitId:uuid.nullable()}).strict(),
 z.object({action:z.literal('person'),workerId:uuid,employeeNumber:z.string().trim().min(1).max(80),unitId:uuid.nullable(),archived:z.boolean()}).strict(),
 z.object({action:z.literal('policy'),previousVersion:z.number().int().nonnegative(),policy:institutionPolicy}).strict(),
 z.object({action:z.literal('procedure'),id:uuid,title:z.string().trim().min(3).max(150),body:z.string().trim().min(80).max(15000),sources:z.string().trim().min(10).max(2000),language:z.enum(['en','hi']),submit:z.boolean()}).strict(),
 z.object({action:z.literal('procedure-update'),id:uuid,revision:z.number().int().positive(),title:z.string().trim().min(3).max(150),body:z.string().trim().min(80).max(15000),sources:z.string().trim().min(10).max(2000),language:z.enum(['en','hi']),submit:z.boolean()}).strict(),
 z.object({action:z.literal('procedure-review'),id:uuid,revision:z.number().int().positive(),outcome:z.enum(['approved','rejected','withdrawn']),note:z.string().trim().min(20).max(2000),qualification:z.string().trim().min(10).max(500)}).strict(),
]);
/** Strict small CSV: quoted commas/newlines are supported, no formulas executed. */
export function parsePeopleCsv(text:string){
 if(text.length>50000)throw Error('Invalid import: use at most 50 KB.');
 const rows:string[][]=[];let row:string[]=[],field='',quoted=false;
 for(let i=0;i<text.length;i++){const c=text[i];if(c==='"'){if(quoted&&text[i+1]==='"'){field+='"';i++;}else if(!quoted&&field.length)throw Error('Invalid CSV quoting.');else quoted=!quoted;}
 else if(c===','&&!quoted){row.push(field.trim());field='';}
 else if((c==='\n'||c==='\r')&&!quoted){if(c==='\r'&&text[i+1]==='\n')i++;row.push(field.trim());if(row.some(Boolean))rows.push(row);row=[];field='';}else field+=c;}
 if(quoted)throw Error('Invalid CSV: a quoted field is incomplete.');row.push(field.trim());if(row.some(Boolean))rows.push(row);
 const header=rows.shift()?.map(v=>v.replace(/^\uFEFF/,'').toLowerCase());
 if(header?.join(',')!=='employee_number,name,sector')throw Error('Invalid CSV header. Use employee_number,name,sector.');
 if(!rows.length||rows.length>200)throw Error('Invalid import: supply 1–200 people.');
 const people=rows.map((r,i)=>{if(r.length!==3)throw Error(`Invalid CSV row ${i+2}: expected three fields.`);const parsed=importPerson.safeParse({employeeNumber:r[0],name:r[1],sector:r[2]||'Unspecified'});if(!parsed.success)throw Error(`Invalid CSV row ${i+2}: check name, employee number and sector.`);return parsed.data;});
 if(new Set(people.map(p=>p.employeeNumber.toLowerCase())).size!==people.length)throw Error('Invalid import: duplicate employee numbers.');return people;
}
