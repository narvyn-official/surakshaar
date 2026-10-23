package com.narvyn.suraksha

import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Local headset decisions only. Nothing in this journal is AR placement or practical competence. */
internal class VrLearningSession private constructor(val data:JSONObject,val scenario:JSONObject) {
    val steps=scenario.getJSONArray("steps").objects()
    val events get()=data.getJSONArray("answers")
    val index get()=data.getInt("index")
    val finished get()=index>=steps.size
    val awaitingContinue get()=data.getBoolean("awaitingContinue")
    val awaitingRetry get()=data.getBoolean("awaitingRetry")
    val lastCorrect get()=data.getBoolean("lastCorrect")
    val current get()=steps[index.coerceAtMost(steps.lastIndex)]

    fun choices():List<JSONObject> = current.getJSONArray("choices").objects()
        .shuffled(java.util.Random(data.getString("id").hashCode().toLong()+index))

    private fun clockNow()=maxOf(System.currentTimeMillis(),data.getLong("updatedAt"))
    private fun time(now:Long){require(now>=data.getLong("updatedAt")){"Headset record time cannot regress"}}
    private fun append(kind:String,now:Long,fields:JSONObject=JSONObject()){
        check(events.length()<MAX_EVENTS){"Headset practice journal is full; start a separate attempt"}
        fields.put("kind",kind).put("stepId",current.getString("id")).put("at",now).put("presentation",PRESENTATION)
        events.put(fields);data.put("updatedAt",now)
    }

    fun choose(choiceId:String,now:Long=clockNow()):Boolean {
        time(now);check(!finished&&!awaitingContinue&&!awaitingRetry){"Close feedback before another decision"}
        val choice=current.getJSONArray("choices").objects().firstOrNull{it.getString("id")==choiceId}
            ?:throw IllegalArgumentException("Unknown headset decision")
        val correct=choice.getBoolean("correct")
        append("decision",now,JSONObject().put("choiceId",choiceId).put("correct",correct))
        data.put("lastCorrect",correct).put("awaitingContinue",correct).put("awaitingRetry",!correct)
        return correct
    }

    /** Releasing a held gaze is not a retry. The learner explicitly acknowledges feedback. */
    fun retry(now:Long=clockNow()){
        time(now);check(!finished&&awaitingRetry&&!awaitingContinue){"No incorrect decision is waiting"}
        append("retry",now);data.put("awaitingRetry",false).put("lastCorrect",false)
    }

    fun next(now:Long=clockNow()){
        time(now);check(!finished&&awaitingContinue&&!awaitingRetry){"A correct decision and explicit Continue are required"}
        check(!current.optBoolean("requiresObservation")||events.objects().any{
            it.getString("kind")=="observation"&&it.getString("stepId")==current.getString("id")&&it.getString("partId")==current.getString("part")
        }){"Inspect this step's anatomy before continuing"}
        append("continue",now)
        data.put("index",index+1).put("awaitingContinue",false).put("awaitingRetry",false).put("lastCorrect",false)
            .put("finished",finished)
    }

    /** Call only for an explicit rendered-part inspection; dwell frames are not observations. */
    fun observe(partId:String,now:Long=clockNow()){
        time(now);check(!finished){"This headset practice is complete"}
        val expected=current.optString("part").takeIf{it.isNotBlank()&&it!="null"}
        require(expected!=null&&partId==expected){"Inspect the current step's named structure"}
        if(events.objects().any{it.getString("kind")=="observation"&&it.getString("stepId")==current.getString("id")&&it.getString("partId")==partId})return
        append("observation",now,JSONObject().put("partId",partId))
    }

    companion object {
        const val EVIDENCE="headset-decision-practice"
        const val PRESENTATION="headset-vr"
        const val MAX_EVENTS=4096
        const val MAX_RECORD_BYTES=1_000_000
        private val keys=setOf("schemaVersion","id","workerId","scenarioId","courseId","contentVersion","scenarioHash","createdAt","updatedAt",
            "evidenceType","presentation","certifiable","practicalCompetence","reviewStatus","index","finished","awaitingContinue","awaitingRetry","lastCorrect","answers")
        private val eventKeys=mapOf("decision" to setOf("kind","stepId","at","presentation","choiceId","correct"),
            "retry" to setOf("kind","stepId","at","presentation"),"continue" to setOf("kind","stepId","at","presentation"),
            "observation" to setOf("kind","stepId","at","presentation","partId"))
        private fun id(value:String){require(value.isNotBlank()&&value.length<=256)}
        private fun validateScenario(scenario:JSONObject){
            id(scenario.getString("id"));id(scenario.getString("course"))
            val steps=scenario.getJSONArray("steps").objects();require(steps.size in 1..256)
            require(steps.map{it.getString("id")}.toSet().size==steps.size)
            for(step in steps){
                id(step.getString("id"));val choices=step.getJSONArray("choices").objects()
                require(choices.size in 1..12&&choices.count{it.getBoolean("correct")}==1)
                require(choices.map{it.getString("id")}.toSet().size==choices.size)
                choices.forEach{id(it.getString("id"))}
                if(step.optBoolean("requiresObservation"))require(step.optString("part").let{it.isNotBlank()&&it!="null"})
            }
        }

        fun create(scenario:JSONObject,version:String,worker:String,now:Long=System.currentTimeMillis()):VrLearningSession {
            id(version);id(worker);require(now>=0);validateScenario(scenario)
            val frozen=JSONObject(scenario.toString())
            return VrLearningSession(JSONObject().put("schemaVersion",1).put("id",UUID.randomUUID().toString()).put("workerId",worker)
                .put("scenarioId",frozen.getString("id")).put("courseId",frozen.getString("course")).put("contentVersion",version)
                .put("scenarioHash",VrLearningHistory.hash(frozen)).put("createdAt",now).put("updatedAt",now)
                .put("evidenceType",EVIDENCE).put("presentation",PRESENTATION).put("certifiable",false).put("practicalCompetence","not-assessed")
                .put("reviewStatus",frozen.optString("reviewStatus","draft")).put("index",0).put("finished",false)
                .put("awaitingContinue",false).put("awaitingRetry",false).put("lastCorrect",false).put("answers",JSONArray()),frozen)
        }

        /** Replay proves consistency with the pinned lesson, not that a physical action took place. */
        fun restore(record:JSONObject,scenario:JSONObject,version:String,worker:String):VrLearningSession = try {
            require(record.toString().toByteArray(Charsets.UTF_8).size<=MAX_RECORD_BYTES)
            require(record.keys().asSequence().toSet()==keys){"Unexpected headset record fields"}
            require(record.getInt("schemaVersion")==1&&record.getString("workerId")==worker&&record.getString("contentVersion")==version)
            require(UUID.fromString(record.getString("id")).toString()==record.getString("id"))
            val replay=create(scenario,version,worker,record.getLong("createdAt"))
            replay.data.put("id",record.getString("id"))
            val events=record.getJSONArray("answers");require(events.length()<=MAX_EVENTS)
            for(event in events.objects()){
                val kind=event.getString("kind")
                require(event.keys().asSequence().toSet()==eventKeys[kind]){"Unexpected headset event fields"}
                require(event.getString("presentation")==PRESENTATION&&!replay.finished&&event.getString("stepId")==replay.current.getString("id"))
                val now=event.getLong("at")
                when(kind){
                    "decision"->require(replay.choose(event.getString("choiceId"),now)==event.getBoolean("correct"))
                    "retry"->replay.retry(now)
                    "continue"->replay.next(now)
                    "observation"->replay.observe(event.getString("partId"),now)
                    else->throw IllegalArgumentException("Unknown headset journal event")
                }
            }
            require(VrLearningHistory.canonical(record)==VrLearningHistory.canonical(replay.data)){"Headset journal state does not match its history"}
            VrLearningSession(JSONObject(record.toString()),replay.scenario)
        }catch(invalid:Exception){
            if(invalid is IllegalArgumentException)throw invalid
            throw IllegalArgumentException("This headset record cannot be verified",invalid)
        }
    }
}

/** Pure JSON comparison avoids relying on object insertion order across Android and JVM JSON. */
internal object VrLearningHistory {
    fun canonical(value:Any?):String=when(value){
        null,JSONObject.NULL->"null"
        is JSONObject->value.keys().asSequence().toList().sorted().joinToString(prefix="{",postfix="}"){JSONObject.quote(it)+":"+canonical(value.get(it))}
        is JSONArray->(0 until value.length()).joinToString(prefix="[",postfix="]"){canonical(value.get(it))}
        is String->JSONObject.quote(value)
        is Number,is Boolean->value.toString()
        else->throw IllegalArgumentException("Unsupported headset journal value")
    }
    fun hash(scenario:JSONObject)=MessageDigest.getInstance("SHA-256").digest(canonical(scenario).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun requireAppendOnly(old:JSONObject,next:JSONObject){
        val mutable=setOf("answers","index","finished","awaitingContinue","awaitingRetry","lastCorrect","updatedAt")
        require(old.keys().asSequence().toSet()==next.keys().asSequence().toSet())
        for(key in old.keys().asSequence().filter{it !in mutable})require(canonical(old.get(key))==canonical(next.get(key))){"Original headset record identity cannot change"}
        require(next.getLong("updatedAt")>=old.getLong("updatedAt"))
        val before=old.getJSONArray("answers");val after=next.getJSONArray("answers")
        require(after.length()>=before.length()){"Original headset decisions cannot be removed"}
        for(i in 0 until before.length())require(canonical(before.get(i))==canonical(after.get(i))){"Original headset decisions cannot change"}
    }
}
