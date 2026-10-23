package com.narvyn.suraksha

import org.json.JSONObject
import java.util.Collections

/** A frozen authored teaching view. No choice answers, assessment events or certificates live here. */
internal data class HandsLessonStep(
    val scenarioId:String,
    val courseId:String,
    val audience:String,
    val scenarioTitle:String,
    val contentVersion:String,
    val stepId:String,
    val index:Int,
    val count:Int,
    val title:String,
    val cue:String,
    val explanation:String,
    /** Palette family only. [renderModel] is the authoritative step-specific mesh. */
    val modelId:String,
    val renderModel:String,
    val kind:String,
    val teachingKey:String?,
    /** An isolated, explicitly declared anatomy specimen, not an observation receipt. */
    val partId:String?,
    val equipmentPartId:String?,
    /** Related learning reference; it must not replace a whole casualty teaching scene. */
    val observationPartId:String?,
    val sourceUrl:String?,
    val reviewStatus:String,
    val boundary:String,
) {
    val key:String get()="$scenarioId/$stepId"
    val hasPrevious:Boolean get()=index>0
    val hasNext:Boolean get()=index<count-1
    val certifiable:Boolean get()=false
    val practicalCompetence:String get()="not-assessed"
    /** The camera HUD uses the cue; this detail remains available in full through a separate panel. */
    val detailText:String get()=listOf(title,cue,explanation,boundary).filter{it.isNotBlank()}.joinToString("\n\n")
}

/**
 * Pure, transient teaching navigation over pinned bundled content. Next/Previous change only this
 * view. They cannot advance LearningAttempt, satisfy an observation, or create a completion record.
 * The Activity must additionally fence the active learner/profile context on every resumed entry.
 */
internal class HandsLessonSession private constructor(
    val scenarioId:String,
    val contentVersion:String,
    val school:Boolean,
    steps:List<HandsLessonStep>,
    initialIndex:Int,
) {
    val steps:List<HandsLessonStep> = Collections.unmodifiableList(steps.toList())
    var index:Int=initialIndex
        private set
    val current:HandsLessonStep get()=steps[index]
    val hasPrevious:Boolean get()=index>0
    val hasNext:Boolean get()=index<steps.lastIndex
    val certifiable:Boolean get()=false
    val practicalCompetence:String get()="not-assessed"

    fun previous():Boolean {if(!hasPrevious)return false;index--;return true}
    fun next():Boolean {if(!hasNext)return false;index++;return true}
    fun goTo(stepId:String):Boolean {
        val target=steps.indexOfFirst{it.stepId==stepId}
        if(target<0)return false
        index=target;return true
    }

    companion object {
        private val anatomyId=Regex("FMA[0-9]{1,8}")
        private val safeId=Regex("[a-z0-9][a-z0-9_-]{0,127}")
        private const val MAX_STEPS=256
        private const val DEFAULT_BOUNDARY="Inspection and decision rehearsal; physical competence requires supervised practice."
        private val fireVariants=setOf("fire","fire-damaged","fire-pin-removed","fire-aimed","fire-discharging")
        private fun identifier(value:String):String {require(safeId.matches(value)){"Unknown teaching identifier"};return value}
        private fun requiredText(source:JSONObject,key:String,limit:Int=8000):String {
            val value=source.getString(key)
            require(value.isNotBlank()&&value.length<=limit){"Teaching text is missing or too long"}
            return value
        }
        private fun optionalText(source:JSONObject,key:String):String? =
            source.optString(key).takeIf{it.isNotBlank()&&it!="null"}

        /**
         * [spatialSteps] is SpatialCatalog.data.getJSONObject("steps"), not caller-supplied scene
         * extras. All mappings for this scenario are checked before any part can be presented.
         */
        fun create(
            scenario:JSONObject,
            spatialSteps:JSONObject,
            contentVersion:String,
            school:Boolean,
            startStepId:String?=null,
        ):HandsLessonSession {
            require(contentVersion.isNotBlank()&&contentVersion.length<=256){"Teaching content version is missing"}
            val id=identifier(scenario.getString("id"))
            val course=scenario.getString("course")
            require(course in setOf("fire","first-aid")){"This course has no hands teaching path"}
            val audience=scenario.getString("audience")
            require(audience in setOf("adult","awareness")){"This lesson has no supported audience"}
            require(!school||audience=="awareness"){"School profiles use awareness lessons"}
            val title=requiredText(scenario,"title",512)
            val review=optionalText(scenario,"reviewStatus")?:"draft"
            val sourceSteps=scenario.getJSONArray("steps")
            require(sourceSteps.length() in 1..MAX_STEPS){"Teaching steps are missing or exceed the limit"}
            val seen=mutableSetOf<String>()
            val frozen=(0 until sourceSteps.length()).map{index->
                val step=sourceSteps.getJSONObject(index)
                val stepId=identifier(step.getString("id"))
                require(seen.add(stepId)){"Duplicate teaching step"}
                val plan=spatialSteps.optJSONObject("$id/$stepId")
                    ?:throw IllegalArgumentException("This teaching step has no declared spatial scene")
                require(plan.getString("audience")==audience){"Spatial scene audience does not match its lesson"}
                val renderModel=plan.getString("model")
                require(renderModel=="anatomy"||renderModel=="emergency"||renderModel in fireVariants||renderModel in FireScenePlan.propKinds){"This teaching model is unsupported"}
                val kind=plan.getString("kind")
                require(kind in setOf("teaching","anatomy","equipment")){"Unsupported teaching scene kind"}
                val teaching=optionalText(plan,"teachingKey")
                if(teaching!=null){
                    require(course=="first-aid"&&renderModel=="anatomy"&&kind=="teaching"){"Teaching props require an anatomy lesson"}
                    require(FirstAidSceneGuide.byKey(teaching)!=null){"The declared teaching illustration is unavailable"}
                    require(!school){"Adult treatment illustrations are outside the School path"}
                }
                val part=optionalText(plan,"part")
                val anatomyPart=part?.takeIf{anatomyId.matches(it)}
                if(renderModel=="anatomy")require(part==null||anatomyPart!=null){"Invalid anatomical structure identifier"}
                val related=optionalText(plan,"observationPart")?:optionalText(step,"part")
                if(related!=null)require(anatomyId.matches(related)){"Invalid related anatomical structure"}
                val source=optionalText(step,"source")
                if(source!=null)require(source.startsWith("https://")&&source.length<=2048){"Invalid lesson source"}
                val boundary=optionalText(plan,"boundary")?:DEFAULT_BOUNDARY
                require(boundary.length<=2000)
                HandsLessonStep(id,course,audience,title,contentVersion,stepId,index,sourceSteps.length(),
                    requiredText(step,"title",512),requiredText(step,"cue"),requiredText(step,"explanation"),
                    if(renderModel=="anatomy")"body"else"extinguisher",renderModel,kind,teaching,anatomyPart,
                    if(renderModel=="anatomy")null else part,related,source,review,boundary)
            }
            val initial=if(startStepId==null)0 else frozen.indexOfFirst{it.stepId==startStepId}
            require(initial>=0){"The requested teaching step does not belong to this lesson"}
            return HandsLessonSession(id,contentVersion,school,frozen,initial)
        }
    }
}
