package com.narvyn.suraksha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Practice and objective revision have independent stores and evidence types. */
class LearningProgress(context:Context,private val worker:String):AutoCloseable {
    private val storage=SkillPracticeStore(context,worker,"launch-learning.db")
    private val revisions=SkillPracticeStore(context,worker,"learning-revision.db")
    fun records()=storage.records()
    fun save(attempt:LearningAttempt)=storage.save(attempt.data)
    fun revisionRecords()=revisions.records()
    fun saveRevision(revision:LearningRevision){require(revision.data.getString("workerId")==worker);revisions.save(revision.data)}
    fun weakObjectives(catalog:LearningCatalog,course:String?=null,awarenessOnly:Boolean=false)=
        LearningRevisionPlan.targets(records(),revisionRecords(),catalog.scenarios,catalog.version,worker)
            .filter{(course==null||it.scenario.getString("course")==course)&&(!awarenessOnly||it.scenario.getString("audience")=="awareness")}
    override fun close(){storage.close();revisions.close()}
}

data class LearningRevisionTarget(val scenario:JSONObject,val step:JSONObject,val misses:Int,val hints:Int,val lastEvidenceAt:Long)

/** Latest valid attempt per objective wins; only a later clean check resolves that revision item. */
object LearningRevisionPlan {
    fun targets(records:List<JSONObject>,revisions:List<JSONObject>,scenarios:List<JSONObject>,version:String,worker:String):List<LearningRevisionTarget>{
        val byId=scenarios.associateBy{it.getString("id")}
        val valid=records.mapNotNull{record -> runCatching {
            val scene=byId[record.getString("scenarioId")]?:return@runCatching null
            LearningAttempt.restore(record,scene,version,worker)
        }.getOrNull()}.sortedByDescending{it.data.getLong("updatedAt")}
        val byObjective = mutableMapOf<String, LearningRevisionTarget>()
        val revisionGroups = revisions.groupBy { it.optString("scenarioId") + "/" + it.optString("objectiveId") }
        for(a in valid) for(step in a.steps){
            val events=a.events.objects().filter{it.optString("stepId")==step.getString("id")&&it.optString("kind") in setOf("decision","hint")}
            if(events.isEmpty())continue
            val key=a.scenario.getString("id")+"/"+step.getString("id")
            val time=events.maxOf{it.getLong("at")}
            val misses=events.count{it.optString("kind")=="decision"&&!it.getBoolean("correct")}
            val hints=events.count{it.optString("kind")=="hint"}
            val prior=byObjective[key]
            if(prior!=null && (prior.lastEvidenceAt>time || (prior.lastEvidenceAt==time && prior.misses+prior.hints>=misses+hints)))continue
            byObjective[key]=LearningRevisionTarget(a.scenario,step,misses,hints,time)
        }
        val targets=byObjective.filter { (key,target) ->
            if(target.misses==0&&target.hints==0)return@filter false
            val checks=revisionGroups[key].orEmpty().mapNotNull { r -> runCatching {
                LearningRevision.restore(r,target.scenario,target.step,version,worker)
            }.getOrNull() }
            val latest=checks.maxByOrNull{it.lastCheckAt}
            !(latest?.let{it.passed&&it.clean&&it.lastCheckAt>target.lastEvidenceAt}==true)
        }.values
        return targets.sortedWith(compareByDescending<LearningRevisionTarget>{it.step.optBoolean("safetyCritical")}
            .thenByDescending{it.misses}.thenByDescending{it.hints}.thenBy{it.scenario.getString("id")}.thenBy{it.step.getString("id")})
    }
}

/** A single-objective knowledge check, not a whole lesson or a spatial/practical assessment. */
class LearningRevision(val data:JSONObject,val scenario:JSONObject,val step:JSONObject){
    val events get()=data.getJSONArray("answers")
    val passed get()=data.getBoolean("finished")
    val lastCheckAt get()=events.objects().filter{it.optString("kind") in setOf("decision","hint")}.maxOfOrNull{it.getLong("at")}?:data.getLong("createdAt")
    val clean get()=passed&&events.objects().none{it.optString("kind")=="hint"||(it.optString("kind")=="decision"&&!it.getBoolean("correct"))}
    fun choices()=step.getJSONArray("choices").objects().shuffled(java.util.Random(data.getString("id").hashCode().toLong()))
    private fun now()=maxOf(System.currentTimeMillis(),data.getLong("updatedAt"))
    private fun append(event:JSONObject,at:Long){require(at>=data.getLong("updatedAt"));events.put(event.put("at",at));data.put("updatedAt",at)}
    fun answer(choice:String,at:Long=now()):Boolean{
        check(!passed);require(at>=data.getLong("updatedAt"))
        val c=step.getJSONArray("choices").objects().firstOrNull{it.getString("id")==choice}?:error("Unknown revision choice")
        val correct=c.getBoolean("correct")
        append(JSONObject().put("kind","decision").put("choiceId",choice).put("correct",correct),at)
        data.put("finished",correct);return correct
    }
    fun hint(at:Long=now()){check(!passed);append(JSONObject().put("kind","hint"),at)}
    fun reflect(text:String,at:Long=now()){check(passed);require(text.isNotBlank());append(JSONObject().put("kind","teach-back").put("text",text.trim().take(1000)),at)}
    companion object {
        fun create(scenario:JSONObject,step:JSONObject,version:String,worker:String,at:Long=System.currentTimeMillis())=LearningRevision(JSONObject()
            .put("id",UUID.randomUUID().toString()).put("workerId",worker).put("scenarioId",scenario.getString("id")).put("objectiveId",step.getString("id"))
            .put("courseId",scenario.getString("course")).put("contentVersion",version).put("createdAt",at).put("updatedAt",at)
            .put("evidenceType","formative-objective-revision").put("finished",false).put("answers",JSONArray()),scenario,step)
        fun restore(record:JSONObject,scenario:JSONObject,step:JSONObject,version:String,worker:String):LearningRevision{
            require(record.getString("workerId")==worker&&record.getString("contentVersion")==version)
            require(record.getString("scenarioId")==scenario.getString("id")&&record.getString("courseId")==scenario.getString("course"))
            require(record.getString("objectiveId")==step.getString("id")&&record.getString("evidenceType")=="formative-objective-revision")
            UUID.fromString(record.getString("id"))
            val replay=create(scenario,step,version,worker,record.getLong("createdAt"))
            for(e in record.getJSONArray("answers").objects())when(e.getString("kind")){
                "decision"->require(replay.answer(e.getString("choiceId"),e.getLong("at"))==e.getBoolean("correct"))
                "hint"->replay.hint(e.getLong("at"))
                "teach-back"->replay.reflect(e.getString("text"),e.getLong("at"))
                else->error("Unknown revision event")
            }
            require(replay.passed==record.getBoolean("finished")&&replay.data.getLong("updatedAt")==record.getLong("updatedAt"))
            return LearningRevision(record,scenario,step)
        }
    }
}
