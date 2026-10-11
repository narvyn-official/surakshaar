import {DatabaseSync,backup} from 'node:sqlite';
import {mkdirSync,readdirSync,statSync,unlinkSync,renameSync} from 'node:fs';
import {isAbsolute,resolve} from 'node:path';
const source=process.env.SURAKSHA_DB_PATH,directory=process.env.SURAKSHA_BACKUP_DIR;
if(!source||!directory||!isAbsolute(source)||!isAbsolute(directory))throw new Error('Absolute database and backup paths are required');
process.umask(0o077);mkdirSync(directory,{recursive:true,mode:0o700});
const db=new DatabaseSync(source,{readOnly:true}),path=resolve(directory,'.pending-'+Date.now()+'.sqlite');
try{await backup(db,path)}finally{db.close()}
const restored=new DatabaseSync(path,{readOnly:true});
try{
 if(restored.prepare('PRAGMA integrity_check').get().integrity_check!=='ok'||restored.prepare('PRAGMA foreign_key_check').all().length)throw new Error('Backup verification failed');
}finally{restored.close()}
renameSync(path,path.replace('/.pending-','/scheduled-'));
// Delete only this service's scheduled backups, after a verified fresh backup.
for(const name of readdirSync(directory))if(/^scheduled-\d+\.sqlite$/.test(name)){
 const candidate=resolve(directory,name);
 if(statSync(candidate).mtimeMs<Date.now()-14*86400000)unlinkSync(candidate);
}
console.log('Private SQLite online backup verified; scheduled retention is 14 days.');
