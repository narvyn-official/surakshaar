package com.narvyn.suraksha

import android.content.ContentValues
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic emulator-only journals. This suite does not establish tracking or physical competence. */
@RunWith(AndroidJUnit4::class)
class VrLearningStoreTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var original:String
    private lateinit var before:String
    private lateinit var first:String
    private lateinit var second:String
    private val fixtures=mutableListOf<String>()
    private val catalog get()=LearningCatalog(context)
    private fun switch(worker:String){Store(context).use{it.switchProfile(worker)}}
    private fun evidence()=Store(context).use{JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()}
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        Store(context).use{store->
            original=store.workerId;before=evidence()
            first=store.createProfile("Synthetic VR journal one","Unspecified");fixtures.add(first)
            second=store.createProfile("Synthetic VR journal two","Unspecified");fixtures.add(second)
            store.switchProfile(first)
        }
    }
    @After fun cleanup(){
        if(!::original.isInitialized)return
        switch(original)
        try{assertEquals("VR journal checks changed original assessments/credentials",before,evidence())}
        finally{
            for(worker in fixtures){
                VrLearningStore(context,worker).use{it.writableDatabase.delete("headset_practice","worker=?",arrayOf(worker))}
                Store(context).use{it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
            }
        }
    }
    private fun create(worker:String=first)=catalog.let{VrLearningSession.create(it.scenarios.first(),it.version,worker,99)}
    private fun choose(session:VrLearningSession,correct:Boolean,now:Long){
        session.choose(session.current.getJSONArray("choices").objects().first{it.getBoolean("correct")==correct}.getString("id"),now)
    }
    private fun restore(record:JSONObject)=catalog.let{VrLearningSession.restore(record,it.scenario(record.getString("scenarioId")),it.version,first)}

    @Test fun resumePreservesFeedbackAndHistoryWhileRejectingAnOlderWriter(){
        val session=create();choose(session,false,100)
        val wrong=session.data.toString()
        VrLearningStore(context,first).use{it.save(session)}
        val resumed=VrLearningStore(context,first).use{db->
            assertEquals(wrong,db.record(session.data.getString("id"))!!.toString())
            assertEquals(session.data.getString("id"),db.latest(session.data.getString("scenarioId"),catalog.version)!!.getString("id"))
            restore(db.record(session.data.getString("id"))!!)
        }
        assertTrue(resumed.awaitingRetry);resumed.retry(101);choose(resumed,true,102)
        VrLearningStore(context,first).use{db->
            db.save(resumed)
            assertThrows(IllegalArgumentException::class.java){db.save(session)}
            val read=restore(db.record(session.data.getString("id"))!!)
            assertTrue(read.awaitingContinue);assertEquals(0,read.index);assertEquals(resumed.data.toString(),read.data.toString())
            var now=103L
            while(!read.finished){
                if(read.current.optBoolean("requiresObservation"))read.observe(read.current.getString("part"),now++)
                if(!read.awaitingContinue)choose(read,true,now++)
                read.next(now++)
            }
            db.save(read)
            assertNull(db.latest(read.data.getString("scenarioId"),catalog.version))
            assertEquals(1,db.records().size);assertTrue(db.record(read.data.getString("id"))!!.getBoolean("finished"))
        }
    }

    @Test fun anotherProfileCannotReadOverwriteOrSmuggleAnUnbundledLesson(){
        val own=create();VrLearningStore(context,first).use{it.save(own)}
        val inactive=VrLearningStore(context,first)
        try{
            switch(second)
            assertThrows(IllegalStateException::class.java){inactive.records()}
            VrLearningStore(context,second).use{db->
                assertNull(db.record(own.data.getString("id")));assertTrue(db.records().isEmpty())
                assertThrows(IllegalArgumentException::class.java){db.save(own)}
                val collision=create(second);collision.data.put("id",own.data.getString("id"))
                assertThrows(IllegalArgumentException::class.java){db.save(collision)}
                val altered=JSONObject(own.scenario.toString()).put("title","Injected alternative lesson")
                val forged=VrLearningSession.create(altered,catalog.version,second,99)
                assertThrows(IllegalArgumentException::class.java){db.save(forged)}
                assertTrue(db.records().isEmpty())
            }
            switch(first)
            assertEquals(own.data.toString(),inactive.record(own.data.getString("id"))!!.toString())
        }finally{inactive.close()}
    }

    @Test fun corruptPayloadIsRejectedAndRetainedForRecovery(){
        val session=create();choose(session,false,100)
        VrLearningStore(context,first).use{db->
            db.save(session)
            val changed=JSONObject(session.data.toString()).apply{getJSONArray("answers").getJSONObject(0).put("correct",true)}.toString()
            assertEquals(1,db.writableDatabase.update("headset_practice",ContentValues().apply{put("payload",changed)},"id=? AND worker=?",arrayOf(session.data.getString("id"),first)))
            assertThrows(IllegalArgumentException::class.java){db.record(session.data.getString("id"))}
            assertThrows(IllegalArgumentException::class.java){db.records()}
            db.readableDatabase.rawQuery("SELECT payload FROM headset_practice WHERE id=? AND worker=?",arrayOf(session.data.getString("id"),first)).use{
                assertTrue(it.moveToFirst());assertEquals("Reading a failed record must not silently replace/delete it",changed,it.getString(0))
            }
        }
    }
}
