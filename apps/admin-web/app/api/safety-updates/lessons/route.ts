import {access,db,failure,json} from '@/lib/server';
import {lessonCommand} from '@/lib/safety/lesson-model';
import {changeLessonLink,lessonPanel} from '@/lib/safety/lessons';
const headers={'Cache-Control':'no-store'};
export async function GET(){try{return Response.json(await lessonPanel(db(),await access()),{headers})}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const a=await access('write'),parsed=lessonCommand.safeParse(await json(request,5000));if(!parsed.success)throw new Error('Invalid lesson reference action.');
 await changeLessonLink(db(),a,parsed.data);return Response.json({saved:true,scope:'review-dependency-only'},{headers});
}catch(e){return failure(e)}}
