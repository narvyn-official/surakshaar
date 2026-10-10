import {getAppUser,freshUser,isOperator} from '@/lib/auth';
import {access,db,failure,json} from '@/lib/server';
import {env} from '@/lib/runtime-env';
import {configFrom,requireSafetyRole,requireSourceOperator,sourceCommand} from '@/lib/safety/model';
import {sourceUrl} from '@/lib/safety/url-policy';
const headers={'Cache-Control':'no-store'};
export async function GET(){try{
 const user=await getAppUser();if(!user)throw new Error('Sign in to continue.');const operator=isOperator(user.userId);
 if(!operator)requireSafetyRole((await access()).role,'read');
 const sources=await db().prepare('SELECT * FROM safety_sources ORDER BY topic,title LIMIT 100').all();
 const audit=operator?await db().prepare('SELECT * FROM safety_source_audit ORDER BY at DESC,id DESC LIMIT 50').all():{results:[]};
 return Response.json({sources:sources.results,operator,audit:audit.results},{headers});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const user=await freshUser();requireSourceOperator(isOperator(user.userId));
 const parsed=sourceCommand.safeParse(await json(request,8000));if(!parsed.success)throw new Error('Invalid public source proposal or decision.');
 const c=parsed.data,now=Date.now();
 if(c.action==='propose'){
  const url=sourceUrl(c.url,configFrom(env as unknown as Record<string,unknown>).hosts),id=crypto.randomUUID();
  const result=await db().batch([
   db().prepare("INSERT OR IGNORE INTO safety_sources(id,title,publisher,url,topic,jurisdiction,rationale,changed_at,changed_by) SELECT ?,?,?,?,?,?,?,?,? WHERE (SELECT count(*) FROM safety_sources)<100").bind(id,c.title,c.publisher,url,c.topic,c.jurisdiction,c.rationale,now,user.userId),
   db().prepare("INSERT INTO safety_source_audit SELECT ?,?,?, 'proposed',?,?,1 WHERE changes()>0").bind(crypto.randomUUID(),id,user.userId,c.rationale,now)]);
  if(!result[0].meta.changes)throw new Error('Record conflict: this URL already exists or the 100-source pilot limit was reached.');
 }else{
  const source=await db().prepare('SELECT url FROM safety_sources WHERE id=?').bind(c.id).first<{url:string}>();if(!source)throw new Error('Unknown public source.');
  if(c.status==='approved')sourceUrl(source.url,configFrom(env as unknown as Record<string,unknown>).hosts);
  const result=await db().batch([
   db().prepare('UPDATE safety_sources SET status=?,revision=revision+1,changed_at=?,changed_by=?,decision_note=? WHERE id=? AND revision=? AND status<>?').bind(c.status,now,user.userId,c.note,c.id,c.revision,c.status),
   db().prepare('INSERT INTO safety_source_audit SELECT ?,?,?,?,?,?,? WHERE changes()>0').bind(crypto.randomUUID(),c.id,user.userId,c.status,c.note,now,c.revision+1)]);
  if(!result[0].meta.changes)throw new Error('Record conflict: this source has changed. Refresh the registry.');
 }
 return Response.json({saved:true},{headers});
}catch(e){return failure(e)}}
