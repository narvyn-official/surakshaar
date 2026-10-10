import {access,db,failure,json} from '@/lib/server';
import {env} from '@/lib/runtime-env';
import {isOperator} from '@/lib/auth';
import {configFrom,requireSafetyRole,statuses,workspaceCommand,collectionOrigins,type CollectionOrigin} from '@/lib/safety/model';
import {enqueue,panelData,reviewUpdate,safetyAudit} from '@/lib/safety/store';
const headers={'Cache-Control':'no-store'};
export async function GET(request:Request){try{
 const a=await access(),q=new URL(request.url).searchParams,filter=q.get('status')??'all',origin=q.get('origin')??'all';
 if(!collectionOrigins.includes(origin as CollectionOrigin))throw new Error('Invalid collection origin.');
 if(!['all',...statuses].includes(filter))throw new Error('Invalid review status.');
 return Response.json(await panelData(db(),a,configFrom(env as unknown as Record<string,unknown>),isOperator(a.user.userId),filter,q.get('cursor'),origin as CollectionOrigin),{headers});
}catch(e){return failure(e)}}
export async function POST(request:Request){try{
 const a=await access('write'),parsed=workspaceCommand.safeParse(await json(request,8000));if(!parsed.success)throw new Error('Invalid Safety Updates action or fields.');const c=parsed.data;
 requireSafetyRole(a.role,c.action,c.action==='review'?c.status:undefined);
 if(c.action==='collect')return Response.json(await enqueue(db(),a,configFrom(env as unknown as Record<string,unknown>)),{status:202,headers});
 if(c.action==='review')await reviewUpdate(db(),a,c.id,c.status,c.notes,c.revision);
 if(c.action==='schedule')await db().batch([
  db().prepare('INSERT INTO safety_schedules(owner,interval_hours,enabled,changed_by,changed_at) VALUES(?,?,0,?,?) ON CONFLICT(owner) DO UPDATE SET interval_hours=excluded.interval_hours,enabled=0,changed_by=excluded.changed_by,changed_at=excluded.changed_at,next_run_at=NULL').bind(a.owner,c.intervalHours,a.user.userId,Date.now()),
  safetyAudit(db(),a,'schedule-configured',a.owner,{intervalHours:c.intervalHours,enabled:false})]);
 if(c.action==='cancel'){
  const result=await db().batch([db().prepare("UPDATE safety_jobs SET status='cancelled',finished_at=? WHERE id=? AND owner=? AND status IN ('queued','running')").bind(Date.now(),c.id,a.owner),safetyAudit(db(),a,'cancel',c.id,{},true)]);
  if(!result[0].meta.changes)throw new Error('Record conflict: this job is no longer active in your workspace.');
 }
 return Response.json({saved:true,scope:'staff-reference-only'},{headers});
}catch(e){return failure(e)}}
