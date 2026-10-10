import {db} from './server';

/** Shared by both tutor routes: switching routes cannot multiply the account's budget. */
export async function consumeTutorBudget(account: string): Promise<Response | null> {
  const minute = Math.floor(Date.now() / 60000), hour = Math.floor(minute / 60);
  const result = await db().prepare('INSERT INTO tutor_budget(account,minute,count) VALUES(?,?,1) ON CONFLICT(account,minute) DO UPDATE SET count=count+1 WHERE count<10 RETURNING count').bind(account, minute).first<{count: number}>();
  if (!result) return Response.json({error: 'Tutor request limit reached. Use offline help or try again shortly.'}, {status: 429, headers: {'Cache-Control': 'no-store', 'Retry-After': '60'}});
  const hourly = await db().prepare('INSERT INTO tutor_hour_budget(account,hour,count) VALUES(?,?,1) ON CONFLICT(account,hour) DO UPDATE SET count=count+1 WHERE count<60 RETURNING count').bind(account, hour).first<{count: number}>();
  if (!hourly) return Response.json({error: 'Hourly tutor limit reached. Offline help remains available.'}, {status: 429, headers: {'Cache-Control': 'no-store', 'Retry-After': '3600'}});
  await db().prepare('DELETE FROM tutor_hour_budget WHERE hour<?').bind(hour - 24).run();
  await db().prepare('DELETE FROM tutor_budget WHERE minute<?').bind(minute - 60).run();
  return null;
}
