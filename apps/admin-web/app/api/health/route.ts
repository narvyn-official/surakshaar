import {db} from '@/lib/server';
import packageInfo from '@/package.json';
export const dynamic='force-dynamic';
export async function GET(){try{await db().prepare('SELECT scope_mode FROM team_members LIMIT 1').first();await db().prepare('SELECT name FROM suraksha_schema_migrations ORDER BY name DESC LIMIT 1').first();return Response.json({status:'ready',version:packageInfo.version},{headers:{'Cache-Control':'no-store'}})}catch{return Response.json({status:'unavailable'},{status:503,headers:{'Cache-Control':'no-store'}})}}
