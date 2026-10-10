package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A separately attributed formative record. It cannot repair an assessment or issue a credential. */
class SkillPracticeSession private constructor(val data:JSONObject, private val catalog:JSONObject) {
    val scenario get()=catalog.getJSONArray("scenarios").objects().single{it.getString("id")==data.getString("scenarioId")}
    val index get()=data.getJSONArray("answers").length()
    val done get()=data.getBoolean("finished")
    val current get()=scenario.getJSONArray("questions").getJSONObject(index)
    fun choose(optionId:String,now:Long):Boolean {
        if(done || now<data.getLong("updatedAt"))return false
        val question=current
        val option=question.getJSONArray("choices").objects().firstOrNull{it.getString("id")==optionId}?:return false
        data.getJSONArray("answers").put(JSONObject().put("questionId",question.getString("id")).put("optionId",optionId).put("correct",option.getBoolean("correct")).put("time",now))
        data.put("updatedAt",now)
        if(!option.getBoolean("correct") || index==scenario.getJSONArray("questions").length())data.put("finished",true).put("passed",option.getBoolean("correct"))
        return true
    }
    companion object {
        fun create(catalog:JSONObject,scenarioId:String,worker:String,now:Long=System.currentTimeMillis()):SkillPracticeSession {
            require(worker.isNotBlank()&&now>=0)
            require(catalog.getJSONArray("scenarios").objects().any{it.getString("id")==scenarioId})
            return SkillPracticeSession(JSONObject().put("schemaVersion",1).put("contentVersion",catalog.getString("version")).put("id",UUID.randomUUID().toString()).put("scenarioId",scenarioId).put("workerId",worker).put("createdAt",now).put("updatedAt",now).put("answers",JSONArray()).put("finished",false).put("passed",false).put("presentation","screen").put("certifiable",false),catalog)
        }
        fun restore(record:JSONObject,catalog:JSONObject,worker:String):SkillPracticeSession {
            require(record.getInt("schemaVersion")==1&&record.getString("contentVersion")==catalog.getString("version"))
            require(record.getString("workerId")==worker&&record.getString("presentation")=="screen"&&!record.getBoolean("certifiable"))
            require(UUID.fromString(record.getString("id")).toString()==record.getString("id"))
            val rebuilt=create(catalog,record.getString("scenarioId"),worker,record.getLong("createdAt"))
            rebuilt.data.put("id",record.getString("id"))
            record.getJSONArray("answers").objects().forEach { event ->
                require(!rebuilt.done&&event.getString("questionId")==rebuilt.current.getString("id"))
                require(rebuilt.choose(event.getString("optionId"),event.getLong("time")))
                require(event.getBoolean("correct")==rebuilt.data.getJSONArray("answers").getJSONObject(rebuilt.index-1).getBoolean("correct"))
            }
            require(record.getLong("updatedAt")==rebuilt.data.getLong("updatedAt")) { "Inconsistent practice time" }
            for(key in listOf("finished","passed"))require(record.getBoolean(key)==rebuilt.data.getBoolean(key)) { "Inconsistent practice result" }
            return rebuilt
        }
    }
}

/** Read-only projection from immutable original procedure events; replay does not write evidence. */
object DecisionReplay {
    data class Entry(val step:ProcedureCatalog.Step,val choice:ProcedureCatalog.Action,val correct:Boolean,val presentation:String)
    fun entries(record:JSONObject):List<Entry> {
        val original=ProcedureSession.restore(record)
        return original.data.getJSONArray("events").objects().filter{it.getString("type")=="action"}.map { e ->
            val step=original.definition.steps.single{it.id==e.getString("step")}
            Entry(step,step.actions.single{it.id==e.getString("action")},e.getBoolean("correct"),e.getString("presentation"))
        }
    }
}
