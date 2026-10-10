package com.narvyn.suraksha

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Explicit two-build fixture, excluded from the ordinary *Test.kt matrix. Synthetic data only. */
class UpgradeContinuityCheck {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun seedV010(){
        assertEquals(23,context.packageManager.getPackageInfo(context.packageName,0).longVersionCode.toInt())
        Store(context).use{store->val prior=store.workerId;val id=store.createProfile("Upgrade continuity QA","Mining");store.switchProfile(id)
            Store(context).use{learner->val record=ProcedureSession.create("fire",learner.workerId,false);record.choose(record.step.id+"-unsafe","screen",record.data.getLong("updatedAt")+1);ProcedureStore(context,id).use{it.save(record.data)}
                check(context.getSharedPreferences("upgrade-qa",0).edit().putString("prior",prior).putString("worker",id).putString("record",record.data.toString()).commit())}
        }
    }
    @Test fun verifyV011(){
        assertEquals(24,context.packageManager.getPackageInfo(context.packageName,0).longVersionCode.toInt())
        val prefs=context.getSharedPreferences("upgrade-qa",0);val id=requireNotNull(prefs.getString("worker",null));val original=requireNotNull(prefs.getString("record",null))
        Store(context).use{store->assertEquals(id,store.workerId);assertEquals("Upgrade continuity QA",store.name)
            assertEquals(original,ProcedureStore(context,id).use{it.records().single().toString()});assertTrue(ProcedureSession.restore(org.json.JSONObject(original)).done)
            SkillPracticeStore(context,id).use{assertTrue(it.records().isEmpty())}
            store.switchProfile(requireNotNull(prefs.getString("prior",null)));ProcedureStore(context,id).use{it.writableDatabase.delete("procedures","worker=?",arrayOf(id))};store.writableDatabase.delete("worker_profiles","id=?",arrayOf(id));prefs.edit().clear().commit()
        }
    }
}
