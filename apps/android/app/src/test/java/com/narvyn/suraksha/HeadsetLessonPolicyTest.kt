package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HeadsetLessonPolicyTest {
    private fun lesson(id:String,course:String,audience:String)=JSONObject().put("id",id).put("course",course).put("audience",audience).put("steps",JSONArray().put(JSONObject().put("id","safe")))
    @Test fun schoolProfilesCannotChooseAdultLessonsOrUnknownDomains(){
        val adult=lesson("adult-collapse","first-aid","adult")
        val awareness=lesson("school-help","first-aid","awareness")
        val fire=lesson("escape-first","fire","awareness")
        val outside=lesson("unrelated","other","awareness")
        val scenarios=listOf(adult,awareness,fire,outside)
        assertEquals(listOf(awareness),HeadsetLessonPolicy.lessons(scenarios,"first-aid",true))
        assertEquals(listOf(adult,awareness),HeadsetLessonPolicy.lessons(scenarios,"first-aid",false))
        assertEquals(listOf(fire),HeadsetLessonPolicy.lessons(scenarios,"fire",true))
        assertFalse(HeadsetLessonPolicy.allowed(outside,false))
        assertFalse(HeadsetLessonPolicy.allowed(JSONObject().put("course","fire").put("audience","awareness"),true))
    }
    @Test fun headsetEntryRequiresEveryReadinessCondition(){
        assertTrue(HeadsetLessonPolicy.ready(true,true,true,true,true))
        for(missing in 0..4){
            val state=BooleanArray(5){it!=missing}
            assertFalse("Readiness gate $missing was bypassed",HeadsetLessonPolicy.ready(state[0],state[1],state[2],state[3],state[4]))
        }
    }
}
