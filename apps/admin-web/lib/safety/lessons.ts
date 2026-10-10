import scenarios from '../../../../content/learning/scenarios.json';
import {requireSafetyRole} from './model';
import {safetyAudit,type SafetyActor} from './store';
import type {LessonCommand,LessonLink,LessonPanel} from './lesson-model';
export const referenceTargets=scenarios.scenarios.map(s=>({id:s.id,title:s.title,course:s.course,steps:s.steps.map(t=>({id:t.id,title:t.title,question:t.question,part:t.part}))}));
export function linkState(link:Omit<LessonLink,'state'>):LessonLink['state']{
 if(link.content_version!==scenarios.version||!referenceTargets.some(t=>t.id===link.scenario_id&&t.steps.some(s=>s.id===link.step_id)))return 'curriculum-changed';
 if(link.source_status!=='approved')return 'source-disabled';
 if(!link.latest_id||link.latest_revision!==link.source_revision)return 'awaiting-reference';
 if(link.reviewed_version_id!==link.latest_id||!link.decision)return 'needs-review';
 return link.decision;
}
export async function lessonPanel(db:D1Database,a:SafetyActor):Promise<LessonPanel>{
 requireSafetyRole(a.role,'read');
 const [links,sources]=await Promise.all([
  db.prepare(`SELECT l.*,s.title AS source_title,s.status AS source_status,s.revision AS source_revision,v.id AS latest_id,v.source_revision AS latest_revision,v.retrieved_at AS latest_at
   FROM safety_lesson_links l JOIN safety_sources s ON s.id=l.source_id
   LEFT JOIN safety_versions v ON v.id=(SELECT id FROM safety_versions WHERE source_id=l.source_id AND origin='tinyfish-fetch' ORDER BY rowid DESC LIMIT 1)
   WHERE l.owner=? ORDER BY l.created_at DESC,l.id DESC LIMIT 200`).bind(a.owner).all<Omit<LessonLink,'state'>>(),
  db.prepare('SELECT id,title,status FROM safety_sources ORDER BY title LIMIT 100').all<LessonPanel['sources'][number]>()
 ]);
 // Do not return actor identifiers or private records from other workspaces.
 return {contentVersion:scenarios.version,targets:referenceTargets,links:links.results.map(l=>({...l,state:linkState(l)})),sources:sources.results,canReview:a.role==='admin'};
}
export async function changeLessonLink(db:D1Database,a:SafetyActor,c:LessonCommand){
 requireSafetyRole(a.role,'review');
 if(c.action==='link'){
  if(c.contentVersion!==scenarios.version)throw new Error('Record conflict: the curriculum changed. Reload before linking a step.');
  const target=referenceTargets.find(s=>s.id===c.scenarioId),step=target?.steps.find(s=>s.id===c.stepId);
  if(!target||!step)throw new Error('Invalid lesson step.');
  const result=await db.batch([
   db.prepare(`INSERT OR IGNORE INTO safety_lesson_links(id,owner,source_id,content_version,scenario_id,step_id,aspect,rationale,created_by,created_at)
    SELECT ?,?,?,?,?,?,?,?,?,? WHERE (SELECT count(*) FROM safety_lesson_links WHERE owner=?)<200 AND EXISTS(SELECT 1 FROM safety_sources WHERE id=?)`)
    .bind(crypto.randomUUID(),a.owner,c.sourceId,c.contentVersion,c.scenarioId,c.stepId,c.aspect,c.rationale,a.user.userId,Date.now(),a.owner,c.sourceId),
   safetyAudit(db,a,'lesson-linked',c.sourceId,{scenario:c.scenarioId,step:c.stepId,aspect:c.aspect},true)
  ]);
  if(!result[0].meta.changes)throw new Error('Record conflict: the link already exists, source is unavailable or the 200-link workspace limit was reached.');
  return;
 }
 if(a.role!=='admin')throw new Error('Forbidden: an administrator must remove links or record impact decisions.');
 if(c.action==='unlink'){
  const result=await db.batch([db.prepare('DELETE FROM safety_lesson_links WHERE id=? AND owner=? AND revision=?').bind(c.id,a.owner,c.revision),safetyAudit(db,a,'lesson-unlinked',c.id,{},true)]);
  if(!result[0].meta.changes)throw new Error('Record conflict: this link changed or is unavailable. Reload.');return;
 }
 const link=(await lessonPanel(db,a)).links.find(l=>l.id===c.id);
 if(!link||link.content_version!==scenarios.version||['curriculum-changed','source-disabled','awaiting-reference'].includes(link.state))throw new Error('Record conflict: collect an approved source for this curriculum before reviewing its impact.');
 const result=await db.batch([
  db.prepare(`UPDATE safety_lesson_links SET reviewed_version_id=?,decision=?,review_notes=?,reviewed_by=?,reviewed_at=?,revision=revision+1 WHERE id=? AND owner=? AND revision=? AND content_version=?
   AND ?=(SELECT id FROM safety_versions WHERE source_id=safety_lesson_links.source_id AND origin='tinyfish-fetch' ORDER BY rowid DESC LIMIT 1)
   AND EXISTS(SELECT 1 FROM safety_sources s JOIN safety_versions v ON v.source_id=s.id AND v.source_revision=s.revision WHERE s.id=safety_lesson_links.source_id AND s.status='approved' AND v.id=?)`)
   .bind(c.versionId,c.decision,c.notes,a.user.userId,Date.now(),c.id,a.owner,c.revision,scenarios.version,c.versionId,c.versionId),
  safetyAudit(db,a,'lesson-assessed',c.id,{version:c.versionId,decision:c.decision,notes:c.notes},true)
 ]);
 if(!result[0].meta.changes)throw new Error('Record conflict: a newer reference or decision is available. Reload before reviewing.');
}
