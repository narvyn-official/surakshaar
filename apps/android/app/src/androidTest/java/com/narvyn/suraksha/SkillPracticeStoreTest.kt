package com.narvyn.suraksha
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SkillPracticeStoreTest {
    @Test fun appendOnlyOwnerScopeAndFailedCommitPreserveLastRecord(){
        val c=InstrumentationRegistry.getInstrumentation().targetContext;val dbName="skill-fixture-${UUID.randomUUID()}.db"
        val catalog=JSONObject(c.assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()});val session=SkillPracticeSession.create(catalog,"fire-electrical","owner")
        try{
            SkillPracticeStore(c,"owner",dbName).use{db->db.save(session.data);val original=session.data.toString();session.choose("check",session.data.getLong("updatedAt")+1)
                db.writableDatabase.execSQL("CREATE TRIGGER injected_write_failure BEFORE INSERT ON practice BEGIN SELECT RAISE(ABORT,'synthetic storage failure'); END")
                try{db.save(session.data);fail("Write should fail")}catch(_:android.database.sqlite.SQLiteException){}
                assertEquals(original,db.records().single().toString());db.writableDatabase.execSQL("DROP TRIGGER injected_write_failure");db.save(session.data)
                val tampered=JSONObject(session.data.toString());tampered.getJSONArray("answers").getJSONObject(0).put("optionId","skip")
                try{db.save(tampered);fail("History rewrite accepted")}catch(_:IllegalArgumentException){}
                assertEquals(session.data.toString(),db.records().single().toString())
            }
            SkillPracticeStore(c,"other",dbName).use{other->assertTrue(other.records().isEmpty());try{other.save(session.data);fail("Other owner accepted")}catch(_:IllegalArgumentException){}}
            SkillPracticeStore(c,"owner",dbName).use{assertEquals(session.data.toString(),it.records().single().toString())}
        }finally{c.deleteDatabase(dbName)}
    }
}
