import {db} from '@/lib/server';
import {version} from '@/package.json';
export const dynamic='force-dynamic';
export async function GET(){try{await db().prepare('DELETE FROM identity_sessions WHERE expires_at<=?').bind(Date.now()).run();await db().prepare('SELECT name FROM suraksha_schema_migrations ORDER BY name DESC LIMIT 1').first();return Response.json({status:'ready',version},{headers:{'Cache-Control':'no-store'}})}catch{return Response.json({status:'unavailable'},{status:503,headers:{'Cache-Control':'no-store'}})}}
