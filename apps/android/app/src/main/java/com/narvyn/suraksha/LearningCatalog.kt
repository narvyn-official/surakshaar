package com.narvyn.suraksha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** The launch catalogue does not change historic industrial certification requirements. */
class LearningCatalog(private val context: Context) {
    val catalog=JSONObject(context.assets.open("learning/catalog.json").bufferedReader().use{it.readText()})
    val content=JSONObject(context.assets.open("learning/scenarios.json").bufferedReader().use{it.readText()})
    val version=content.getString("version")
    val courses=catalog.getJSONArray("active").objects()
    val scenarios=content.getJSONArray("scenarios").objects()
    fun course(id:String)=courses.first{it.getString("id")==id}
    fun scenario(id:String)=scenarios.first{it.getString("id")==id}
    private val archives = mutableMapOf<String, JSONObject?>()
    /** Resolve only declared bundled versions. Never substitute current wording for an older record. */
    fun contentForVersion(requested:String):JSONObject? {
        if (requested == version) return content
        if (requested !in catalog.optJSONArray("archivedVersions")?.strings().orEmpty()) return null
        return archives.getOrPut(requested) {
            runCatching { JSONObject(context.assets.open("learning/archive/$requested.json").bufferedReader().use { it.readText() }) }
                .getOrNull()?.takeIf { it.optString("version") == requested }
        }
    }
    fun scenarioForVersion(id:String, requested:String):JSONObject? = contentForVersion(requested)
        ?.getJSONArray("scenarios")?.objects()?.firstOrNull { it.getString("id") == id }
}

/** Deterministic, append-only formative evidence. Never an input to legacy certificate grading. */
class LearningAttempt(val data:JSONObject,val scenario:JSONObject) {
    val steps=scenario.getJSONArray("steps").objects()
    val events get()=data.getJSONArray("answers")
    val index get()=data.getInt("index")
    val finished get()=index>=steps.size
    val current get()=steps[index.coerceAtMost(steps.lastIndex)]
    val awaitingContinue get()=data.optBoolean("awaitingContinue")
    fun choices()=current.getJSONArray("choices").objects().shuffled(java.util.Random(data.getString("id").hashCode().toLong()+index))
    private fun clockNow()=maxOf(System.currentTimeMillis(),data.getLong("updatedAt"))
    private fun time(now:Long){require(now>=data.getLong("updatedAt")){"Record time cannot regress"}}
    fun answer(choice:String,now:Long=clockNow()):Boolean {
        time(now);check(!finished&&!awaitingContinue)
        val answer=current.getJSONArray("choices").objects().firstOrNull{it.getString("id")==choice}?:error("Unknown choice")
        val correct=answer.getBoolean("correct")
        events.put(JSONObject().put("stepId",current.getString("id")).put("choiceId",choice).put("correct",correct).put("at",now).put("hints",data.optInt("stageHints")).put("kind","decision"))
        data.put("awaitingContinue",correct).put("lastCorrect",correct).put("updatedAt",now)
        return correct
    }
    fun hint(now:Long=clockNow()) {time(now);check(!finished);data.put("stageHints",data.optInt("stageHints")+1).put("updatedAt",now);events.put(JSONObject().put("stepId",current.getString("id")).put("kind","hint").put("at",now))}
    fun advance(now:Long=clockNow()) {time(now);check(awaitingContinue&&!finished);check(!current.optBoolean("requiresObservation")||events.objects().any{it.optString("kind")=="observation"&&it.optString("stepId")==current.getString("id")&&it.optString("partId")==current.optString("part")}){"Explore the required anatomy first"};events.put(JSONObject().put("kind","continue").put("stepId",current.getString("id")).put("at",now));data.put("index",index+1).put("awaitingContinue",false).put("stageHints",0).put("updatedAt",now).put("finished",finished)}
    fun explain(text:String,now:Long=clockNow()) {time(now);check(finished);require(text.isNotBlank());events.put(JSONObject().put("kind","teach-back").put("text",text.trim().take(1000)).put("at",now));data.put("updatedAt",now)}
    fun observe(partId:String,mode:String,now:Long=clockNow()){time(now);check(!finished);require(partId==current.optString("part"));require(mode in setOf("3d","ar-exploration"));events.put(JSONObject().put("kind","observation").put("stepId",current.getString("id")).put("partId",partId).put("mode",mode).put("at",now));data.put("updatedAt",now)}
    companion object {
        fun restore(record:JSONObject,scenario:JSONObject,version:String,worker:String):LearningAttempt {
            require(record.getString("workerId")==worker && record.getString("contentVersion")==version)
            require(record.getString("scenarioId")==scenario.getString("id") && record.getString("courseId")==scenario.getString("course"))
            require(record.getString("evidenceType")=="formative-decision-practice")
            UUID.fromString(record.getString("id"))
            val replay=create(scenario,version,worker,record.getBoolean("guided"),record.getLong("createdAt"))
            for(event in record.getJSONArray("answers").objects()) {
                val now=event.getLong("at")
                if(event.has("stepId"))require(!replay.finished&&event.getString("stepId")==replay.current.getString("id"))
                when(event.getString("kind")) {
                    "decision"->{require(event.getInt("hints")==replay.data.optInt("stageHints"));require(replay.answer(event.getString("choiceId"),now)==event.getBoolean("correct"))}
                    "hint"->replay.hint(now)
                    "continue"->replay.advance(now)
                    "teach-back"->replay.explain(event.getString("text"),now)
                    "observation"->replay.observe(event.getString("partId"),event.getString("mode"),now)
                    else->error("Unknown learning event")
                }
            }
            require(record.getInt("index")==replay.index&&record.getBoolean("finished")==replay.finished&&record.optBoolean("awaitingContinue")==replay.awaitingContinue)
            require(record.optInt("stageHints")==replay.data.optInt("stageHints")&&record.getLong("updatedAt")==replay.data.getLong("updatedAt"))
            return LearningAttempt(record,scenario)
        }
        fun create(scenario:JSONObject,version:String,worker:String,guided:Boolean=true,now:Long=System.currentTimeMillis())=LearningAttempt(JSONObject()
            .put("id",UUID.randomUUID().toString()).put("workerId",worker).put("scenarioId",scenario.getString("id"))
            .put("courseId",scenario.getString("course")).put("contentVersion",version).put("index",0).put("answers",JSONArray())
            .put("finished",false).put("guided",guided).put("evidenceType","formative-decision-practice")
            .put("reviewStatus",scenario.getString("reviewStatus")).put("updatedAt",now).put("createdAt",now),scenario)
    }
}
