import {DatabaseSync} from 'node:sqlite';
import {performance} from 'node:perf_hooks';

/** SQLite implementation of only the parameterized D1 surface used by this application.
 * One connection and synchronous transactions prevent interleaving between batch statements.
 * No SQL is derived from request identifiers; callers retain their existing bound parameters.
 */
export class SqliteD1 {
  readonly sqlite:DatabaseSync;
  constructor(path:string){
    this.sqlite=new DatabaseSync(path);
    this.sqlite.exec('PRAGMA foreign_keys=ON; PRAGMA busy_timeout=5000; PRAGMA journal_mode=WAL; PRAGMA synchronous=FULL;');
  }
  prepare(sql:string){return new SqliteStatement(this,sql,[])}
  async batch(statements:SqliteStatement[]){
    if(statements.some(statement=>statement.owner!==this))throw new Error('Statement belongs to another database');
    this.sqlite.exec('BEGIN IMMEDIATE');
    try{const results=statements.map(statement=>statement.execute());this.sqlite.exec('COMMIT');return results}
    catch(error){this.sqlite.exec('ROLLBACK');throw error}
  }
  async exec(sql:string){const start=performance.now();this.sqlite.exec(sql);return {count:1,duration:performance.now()-start}}
  close(){this.sqlite.close()}
}
export class SqliteStatement {
  constructor(readonly owner:SqliteD1,private readonly sql:string,private readonly values:unknown[]){}
  bind(...values:unknown[]){return new SqliteStatement(this.owner,this.sql,values)}
  private statement(){return this.owner.sqlite.prepare(this.sql)}
  private parameters(){return this.values.map(value=>{
    if(value===null||typeof value==='string'||typeof value==='number'||typeof value==='bigint'||value instanceof Uint8Array)return value;
    if(value instanceof ArrayBuffer)return new Uint8Array(value);
    throw new Error('Unsupported SQL parameter');
  })}
  execute(){
    const start=performance.now(),statement=this.statement(),parameters=this.parameters();
    const results=statement.columns().length?statement.all(...parameters):[];
    const outcome=statement.columns().length?null:statement.run(...parameters);
    // changes() and last_insert_rowid() must be read before any subsequent batch statement.
    const info=outcome??this.owner.sqlite.prepare('SELECT changes() AS changes,last_insert_rowid() AS lastInsertRowid').get()!;
    return {success:true,results,meta:{changes:Number(info.changes),last_row_id:Number(info.lastInsertRowid),duration:performance.now()-start,rows_read:results.length,rows_written:Number(info.changes)}};
  }
  async all<T=Record<string,unknown>>(){return this.execute() as unknown as {success:boolean;results:T[];meta:Record<string,number>}}
  async run(){return this.execute()}
  async first<T=Record<string,unknown>>(column?:string):Promise<T|null>{
    const row=this.statement().get(...this.parameters());if(!row)return null;
    if(column!==undefined){if(!(column in row))throw new Error('Unknown SQL column');return row[column] as T}
    return row as T;
  }
  async raw(options?:{columnNames?:boolean}){
    const statement=this.statement();statement.setReturnArrays(true);
    const rows=statement.all(...this.parameters());
    return options?.columnNames?[statement.columns().map(column=>column.name),...rows]:rows;
  }
}
