import {DatabaseSync,backup} from 'node:sqlite';
import {readFileSync,readdirSync,mkdirSync,existsSync} from 'node:fs';
import {resolve,isAbsolute,dirname} from 'node:path';
import {createHash} from 'node:crypto';
const path=process.env.SURAKSHA_DB_PATH;
if(!path||!isAbsolute(path))throw new Error('Set an absolute SURAKSHA_DB_PATH');
const migrations=resolve(process.argv[2]??'drizzle');
mkdirSync(dirname(path),{recursive:true,mode:0o700});process.umask(0o077);
const existed=existsSync(path),db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON; PRAGMA busy_timeout=5000;');
try{
 if(existed){const directory=process.env.SURAKSHA_BACKUP_DIR;if(!directory||!isAbsolute(directory))throw new Error('Existing data requires an absolute SURAKSHA_BACKUP_DIR');mkdirSync(directory,{recursive:true,mode:0o700});await backup(db,resolve(directory,'before-migration-'+Date.now()+'.sqlite'))}
 db.exec('CREATE TABLE IF NOT EXISTS suraksha_schema_migrations(name TEXT PRIMARY KEY,digest TEXT NOT NULL,applied_at INTEGER NOT NULL)');
 let applied=0;for(const name of readdirSync(migrations).filter(name=>/^\d{4}_[a-z0-9_]+\.sql$/.test(name)).sort()){
  const sql=readFileSync(resolve(migrations,name),'utf8'),digest=createHash('sha256').update(sql).digest('hex');
  const old=db.prepare('SELECT digest FROM suraksha_schema_migrations WHERE name=?').get(name);
  if(old){if(old.digest!==digest)throw new Error('Previously applied migration changed: '+name);continue}
  db.exec('BEGIN IMMEDIATE');try{db.exec(sql);db.prepare('INSERT INTO suraksha_schema_migrations VALUES(?,?,?)').run(name,digest,Date.now());db.exec('COMMIT');applied++}catch(error){db.exec('ROLLBACK');throw error}
 }
 if(db.prepare('PRAGMA foreign_key_check').all().length)throw new Error('Foreign key check failed');
 if(db.prepare('PRAGMA integrity_check').get().integrity_check!=='ok')throw new Error('Integrity check failed');
 console.log(`SQLite migrations complete: ${applied} applied; integrity and foreign keys passed.`);
}finally{db.close()}
