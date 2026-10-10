import {isAbsolute} from 'node:path';
import {SqliteD1} from '../lib/runtime/sqlite-d1';
import {configFrom} from '../lib/safety/model';
import {scheduleDue,workOne} from '../lib/safety/worker';
const path=process.env.SURAKSHA_DB_PATH;
if(!path||!isAbsolute(path))throw new Error('Set an absolute private SURAKSHA_DB_PATH for the Safety Updates worker.');
const config=configFrom(process.env),database=new SqliteD1(path),db=database as unknown as D1Database;
let stopping=false;process.on('SIGINT',()=>{stopping=true});process.on('SIGTERM',()=>{stopping=true});
try{
 console.log('Safety Updates worker: public Fetch only; recurring collection '+(config.scheduleEnabled?'requires workspace opt-in':'disabled')+'.');
 do{
  await scheduleDue(db,config);
  const work=await Promise.all(Array.from({length:config.concurrency},()=>workOne(db,config)));
  if(process.argv.includes('--once'))break;
  if(!work.some(Boolean))await new Promise(resolve=>setTimeout(resolve,2000));
 }while(!stopping);
}catch{console.error('Safety Updates worker stopped. Check database migrations and backend configuration.');process.exitCode=1}finally{database.close()}
