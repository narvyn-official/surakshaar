import {DatabaseSync} from 'node:sqlite';
import {isAbsolute} from 'node:path';
const path=process.env.SURAKSHA_DB_PATH;
if(!path||!isAbsolute(path))throw Error('An absolute private database path is required.');
const db=new DatabaseSync(path);db.exec('PRAGMA foreign_keys=ON; PRAGMA secure_delete=ON; PRAGMA busy_timeout=5000;');
try{const result=db.prepare('DELETE FROM identity_sessions WHERE expires_at<=?').run(Date.now());db.exec('PRAGMA wal_checkpoint(TRUNCATE)');console.log('Expired identity sessions removed: '+result.changes);}finally{db.close()}
