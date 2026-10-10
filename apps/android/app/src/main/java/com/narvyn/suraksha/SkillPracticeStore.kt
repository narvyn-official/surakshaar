package com.narvyn.suraksha

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class SkillPracticeStore(context:Context,private val worker:String,name:String="skill-practice.db"):SQLiteOpenHelper(context,name,null,1) {
    override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE practice(id TEXT PRIMARY KEY,worker TEXT NOT NULL,updated INTEGER NOT NULL,payload TEXT NOT NULL)");db.execSQL("CREATE INDEX practice_worker ON practice(worker,updated)")}
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
    fun records():List<JSONObject> = readableDatabase.rawQuery("SELECT payload FROM practice WHERE worker=? ORDER BY updated DESC,rowid DESC",arrayOf(worker)).use{c->buildList{while(c.moveToNext())add(JSONObject(c.getString(0)))}}
    fun save(record:JSONObject){
        require(record.getString("workerId")==worker)
        writableDatabase.beginTransaction()
        try {
            writableDatabase.rawQuery("SELECT worker,payload FROM practice WHERE id=?",arrayOf(record.getString("id"))).use{c->if(c.moveToFirst()){
                require(c.getString(0)==worker)
                val old=JSONObject(c.getString(1));require(old.getString("scenarioId")==record.getString("scenarioId")&&old.getString("contentVersion")==record.getString("contentVersion"))
                val events=old.getJSONArray("answers");val next=record.getJSONArray("answers");require(next.length()>=events.length())
                for(i in 0 until events.length())require(events.getJSONObject(i).toString()==next.getJSONObject(i).toString()){"Original practice evidence cannot change"}
            }}
            check(writableDatabase.insertWithOnConflict("practice",null,ContentValues().apply{put("id",record.getString("id"));put("worker",worker);put("updated",record.getLong("updatedAt"));put("payload",record.toString())},SQLiteDatabase.CONFLICT_REPLACE)!=-1L)
            writableDatabase.setTransactionSuccessful()
        }finally{writableDatabase.endTransaction()}
    }
}
