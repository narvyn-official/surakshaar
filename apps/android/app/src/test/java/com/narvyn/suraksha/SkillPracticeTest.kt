package com.narvyn.suraksha
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SkillPracticeTest {
    private fun catalog()=JSONObject(File("../../../content/transfer-scenarios.json").readText())
    @Test fun everyDomainHasNormalAbnormalAndUncertainTransferAndRefusalCanSucceed(){
        val catalog=catalog();val scenes=catalog.getJSONArray("scenarios").objects()
        for(module in ProcedureCatalog.modules.keys)assertEquals(setOf("normal","abnormal","uncertain"),scenes.filter{it.getString("module")==module}.map{it.getString("condition")}.toSet())
        for(scene in scenes){var s=SkillPracticeSession.create(catalog,scene.getString("id"),"worker",10)
            while(!s.done){assertTrue(s.choose(s.current.getJSONArray("choices").objects().single{it.getBoolean("correct")}.getString("id"),s.data.getLong("updatedAt")+1));s=SkillPracticeSession.restore(JSONObject(s.data.toString()),catalog,"worker")}
            assertTrue(s.data.getBoolean("passed"));assertFalse(s.data.getBoolean("certifiable"));assertEquals("screen",s.data.getString("presentation"))
        }
    }
    @Test fun criticalFailureStopsAndCannotBeAveragedOrEdited(){
        val catalog=catalog();val s=SkillPracticeSession.create(catalog,"fire-electrical","worker",10)
        assertTrue(s.choose("skip",11));assertTrue(s.done);assertFalse(s.data.getBoolean("passed"));val original=s.data.toString()
        assertFalse(s.choose("check",12));assertEquals(original,s.data.toString())
        try{SkillPracticeSession.restore(JSONObject(original).put("passed",true),catalog,"worker");fail()}catch(_:IllegalArgumentException){}
        try{SkillPracticeSession.restore(s.data,catalog,"other");fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun replayPreservesOriginalVersionAndEvidence(){
        val s=ProcedureSession.create("gas","worker",true);s.choose(s.step.id+"-unsafe","screen",s.data.getLong("updatedAt")+1);val original=s.data.toString()
        val entries=DecisionReplay.entries(s.data);assertEquals(1,entries.size);assertFalse(entries[0].correct);assertEquals(original,s.data.toString())
    }
}
