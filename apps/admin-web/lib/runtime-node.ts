import {isAbsolute} from 'node:path';
import {SqliteD1} from './runtime/sqlite-d1';
let database:SqliteD1|undefined;
function storage(){
  if(database)return database;
  const path=process.env.SURAKSHA_DB_PATH;
  if(!path||!isAbsolute(path))throw new Error('An absolute private database path is required');
  database=new SqliteD1(path);return database;
}
/** Reads secrets at runtime only; nothing is copied into public bundles. */
export const env=new Proxy({}as Record<string,unknown>&{DB:D1Database},{get:(_target,key)=>key==='DB'?storage():typeof key==='string'?process.env[key]:undefined});
