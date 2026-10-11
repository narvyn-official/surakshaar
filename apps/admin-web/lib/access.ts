export const roles = ['admin','trainer','viewer','certifier','examiner','assessor','author','reviewer','auditor'] as const;
export type Role = typeof roles[number];
export type Permission = "workspace" | "read" | "write" | "admin" | "certify" | "revoke" | "review" | "identity" | "content" | "content-write" | "content-review" | "audit";
export function permitted(role: Role, permission: Permission) {
  if (!roles.includes(role)) return false;
  if(permission==='workspace')return true;
  if(permission==='audit')return ['admin','auditor','viewer','certifier','assessor'].includes(role);
  if(role==='auditor')return false;
  if(permission==='content')return ['admin','trainer','author','reviewer'].includes(role);
  if(permission==='content-write')return ['admin','trainer','author'].includes(role);
  if(permission==='content-review')return ['reviewer'].includes(role);
  if(permission==='read')return !['author','reviewer'].includes(role);
  if(permission==='review')return ['admin','trainer','examiner','assessor'].includes(role);
  if(permission==='identity')return ['examiner','assessor'].includes(role);
  if(permission==='certify')return role==='certifier'||role==='assessor';
  if(permission==='revoke')return ['certifier','assessor','admin'].includes(role);
  if(permission==='admin')return role==='admin';
  return permission==='write'&&(role==='admin'||role==='trainer');
}
export function assignmentStatus(row: {cancelled_at: number | null; due_at: number; completed_at: number | null}, now: number) {
  return row.cancelled_at !== null ? "cancelled" : row.completed_at !== null ? "complete" : row.due_at < now ? "overdue" : "assigned";
}
