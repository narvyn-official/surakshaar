import {mkdtempSync,cpSync,readdirSync,readFileSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join,resolve} from 'node:path';
import {spawnSync} from 'node:child_process';
import {DatabaseSync} from 'node:sqlite';
const directory=mkdtempSync(join(tmpdir(),'surakshaar-worker-package-'));
try{
 cpSync(resolve('dist-safety-worker'),join(directory,'worker'),{recursive:true});
 const path=join(directory,'fixture.sqlite'),db=new DatabaseSync(path);
 for(const name of readdirSync('drizzle').filter(n=>/^\d{4}_.*\.sql$/.test(n)).sort())db.exec(readFileSync(join('drizzle',name),'utf8'));
 db.close();
 // Deliberately no project node_modules, provider credential, pending job or account authority.
 const run=spawnSync(process.execPath,[join(directory,'worker/worker.mjs'),'--once'],{cwd:directory,encoding:'utf8',timeout:10000,env:{PATH:process.env.PATH,SURAKSHA_DB_PATH:path,SAFETY_SCHEDULE_ENABLED:'false'}});
 if(run.status!==0)throw Error('Isolated worker package failed: '+(run.stderr||run.error?.message||run.status));
 console.log('Worker boots from an isolated package without project dependencies or provider requests.');
}finally{rmSync(directory,{recursive:true,force:true})}
