import test from 'node:test';
import assert from 'node:assert/strict';
import {SqliteD1} from '../lib/runtime/sqlite-d1';
test('SQLite D1 preserves bound text, nullable values, ordered raw columns and first-column reads',async()=>{
 const db=new SqliteD1(':memory:');try{
 await db.exec('CREATE TABLE items(id INTEGER PRIMARY KEY,value TEXT,note TEXT)');
 const text="x'); DROP TABLE items; --";
 const inserted=await db.prepare('INSERT INTO items(value,note) VALUES(?,?)').bind(text,null).run();assert.equal(inserted.meta.changes,1);
 assert.equal(await db.prepare('SELECT value FROM items').first('value'),text);
 assert.deepEqual(await db.prepare('SELECT id AS duplicate,value AS duplicate FROM items').raw(),[[1,text]]);
 assert.equal((await db.prepare('SELECT note FROM items').all()).results[0].note,null);
 }finally{db.close()}
});
test('SQLite D1 batch rolls back every statement on failure and keeps changes() sequencing',async()=>{
 const db=new SqliteD1(':memory:');try{
 await db.exec('CREATE TABLE items(id INTEGER PRIMARY KEY); CREATE TABLE audit(value INTEGER)');
 await assert.rejects(db.batch([db.prepare('INSERT INTO items VALUES(?)').bind(1),db.prepare('INSERT INTO items VALUES(?)').bind(1)]));
 assert.equal(await db.prepare('SELECT COUNT(*) n FROM items').first('n'),0);
 await db.batch([db.prepare('INSERT INTO items VALUES(1)'),db.prepare('INSERT INTO audit SELECT changes()')]);
 assert.equal(await db.prepare('SELECT value FROM audit').first('value'),1);
 }finally{db.close()}
});
test('SQLite D1 enforces foreign keys, rejects foreign-database batches and rejects unsupported bindings',async()=>{
 const db=new SqliteD1(':memory:'),other=new SqliteD1(':memory:');try{
 await db.exec('CREATE TABLE parents(id INTEGER PRIMARY KEY); CREATE TABLE children(id INTEGER REFERENCES parents(id))');
 await assert.rejects(db.prepare('INSERT INTO children VALUES(?)').bind(10).run());
 await assert.rejects(db.batch([other.prepare('SELECT 1')]));
 await assert.rejects(db.prepare('SELECT ?').bind(undefined).run());
 }finally{db.close();other.close()}
});
