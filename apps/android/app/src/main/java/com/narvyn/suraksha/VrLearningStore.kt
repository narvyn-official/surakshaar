package com.narvyn.suraksha

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID
import org.json.JSONObject

/** Private, profile-scoped headset journals; deliberately separate from assessment/sync tables. */
internal class VrLearningStore(context:Context,private val worker:String):SQLiteOpenHelper(context.applicationContext,"vr-learning.db",null,1) {
    private val app=context.applicationContext
    private val catalog by lazy{LearningCatalog(app)}
    init{require(worker.isNotBlank()&&worker.length<=256)}
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE headset_practice(id TEXT PRIMARY KEY,worker TEXT NOT NULL,scenario TEXT NOT NULL,version TEXT NOT NULL,updated INTEGER NOT NULL,payload TEXT NOT NULL)")
        db.execSQL("CREATE INDEX headset_worker_recent ON headset_practice(worker,updated DESC)")
        db.execSQL("CREATE INDEX headset_resume ON headset_practice(worker,scenario,version,updated DESC)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        throw IllegalStateException("Unsupported headset journal migration: $oldVersion to $newVersion")
    }
    private fun active(){Store(app).use{check(it.workerId==worker&&it.isCurrentProfile()){"The active learner changed. Reopen headset learning."}}}
    private fun validate(record:JSONObject):VrLearningSession {
        val version=record.getString("contentVersion")
        val scenario=catalog.scenarioForVersion(record.getString("scenarioId"),version)
            ?:throw IllegalArgumentException("The original headset lesson is not available on this phone")
        return VrLearningSession.restore(record,scenario,version,worker)
    }
    private fun read(cursor:Cursor):JSONObject {
        val payload=cursor.getString(cursor.getColumnIndexOrThrow("payload"))
        require(payload.toByteArray(Charsets.UTF_8).size<=VrLearningSession.MAX_RECORD_BYTES)
        val record=JSONObject(payload)
        require(record.getString("id")==cursor.getString(cursor.getColumnIndexOrThrow("id"))&&
            record.getString("workerId")==cursor.getString(cursor.getColumnIndexOrThrow("worker"))&&
            record.getString("scenarioId")==cursor.getString(cursor.getColumnIndexOrThrow("scenario"))&&
            record.getString("contentVersion")==cursor.getString(cursor.getColumnIndexOrThrow("version"))&&
            record.getLong("updatedAt")==cursor.getLong(cursor.getColumnIndexOrThrow("updated")))
        return validate(record).data
    }

    /** Corrupt/unavailable records fail visibly through the caller; reading never deletes evidence. */
    @Synchronized fun records():List<JSONObject>{
        active()
        return readableDatabase.rawQuery("SELECT * FROM headset_practice WHERE worker=? ORDER BY updated DESC,rowid DESC",arrayOf(worker)).use{cursor->
            buildList{while(cursor.moveToNext())add(read(cursor))}
        }
    }
    @Synchronized fun record(id:String):JSONObject? {
        active();require(UUID.fromString(id).toString()==id)
        return readableDatabase.rawQuery("SELECT * FROM headset_practice WHERE worker=? AND id=?",arrayOf(worker,id)).use{if(it.moveToFirst())read(it)else null}
    }
    /** Most recent unfinished compatible attempt; completed attempts stay in records(). */
    @Synchronized fun latest(scenarioId:String,version:String):JSONObject? {
        active()
        return readableDatabase.rawQuery("SELECT * FROM headset_practice WHERE worker=? AND scenario=? AND version=? ORDER BY updated DESC,rowid DESC",arrayOf(worker,scenarioId,version)).use{cursor->
            var found:JSONObject?=null
            while(found==null&&cursor.moveToNext()){val item=read(cursor);if(!item.getBoolean("finished"))found=item}
            found
        }
    }
    @Synchronized fun save(session:VrLearningSession){
        active()
        // Validate against bundled/archive content, not a caller-supplied scenario or feedback flag.
        val snapshot=validate(JSONObject(session.data.toString())).data
        val db=writableDatabase
        db.beginTransaction()
        try{
            active()
            db.rawQuery("SELECT * FROM headset_practice WHERE id=?",arrayOf(snapshot.getString("id"))).use{cursor->
                if(cursor.moveToFirst()){
                    require(cursor.getString(cursor.getColumnIndexOrThrow("worker"))==worker){"This headset record belongs to another learner"}
                    VrLearningHistory.requireAppendOnly(read(cursor),snapshot)
                }
            }
            val values=ContentValues().apply{
                put("id",snapshot.getString("id"));put("worker",worker);put("scenario",snapshot.getString("scenarioId"))
                put("version",snapshot.getString("contentVersion"));put("updated",snapshot.getLong("updatedAt"));put("payload",snapshot.toString())
            }
            check(db.insertWithOnConflict("headset_practice",null,values,SQLiteDatabase.CONFLICT_REPLACE)!=-1L){"Headset progress could not be saved"}
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
}
