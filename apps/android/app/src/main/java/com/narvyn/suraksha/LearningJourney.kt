package com.narvyn.suraksha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Completion is explicit, profile/version scoped, and never inferred from opening a screen. */
class LearningJourney(private val context:Context,private val learner:Store,private val curriculum:Curriculum) {
    data class Stage(val id:String,val en:String,val hi:String,val complete:Boolean)
    private val prefs=context.getSharedPreferences("learning-path",Context.MODE_PRIVATE)
    private fun key(module:String)="${learner.workerId}:${curriculum.version}:$module"
    fun finishReading(module:String){check(learner.isCurrentProfile());check(prefs.edit().putBoolean(key(module),true).commit())}
    fun finishDemonstration(module:String){check(learner.isCurrentProfile());check(prefs.edit().putBoolean(key(module)+":demo",true).commit())}
    fun stages(module:String):List<Stage>{
        val records=ProcedureStore(context,learner.workerId).use{it.records()}.filter{it.optString("module")==module&&it.optInt("catalogVersion")==ProcedureCatalog.VERSION}
        val independent=records.firstOrNull{!it.optBoolean("guided")}
        val assessment=learner.attempts().firstOrNull{it.optString("moduleId")==module&&it.optString("kind")=="assessment"&&it.optBoolean("finished")}
        val challenges=SkillPracticeStore(context,learner.workerId).use{it.records()}
        val catalog=JSONObject(context.assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
        val moduleScenes=catalog.getJSONArray("scenarios").objects().filter{it.getString("module")==module}.map{it.getString("id")}.toSet()
        return listOf(Stage("learn","Read or listen","पढ़ें या सुनें",prefs.getBoolean(key(module),false)),
            Stage("demonstration","Watch a demonstration","प्रदर्शन देखें",prefs.getBoolean(key(module)+":demo",false)),
            Stage("guided","Guided procedure rehearsal","निर्देशित प्रक्रिया अभ्यास",records.any{it.optBoolean("guided")&&it.optJSONObject("result")?.optBoolean("complete")==true}),
            Stage("independent","Independent scenario check","स्वतंत्र दृश्य जाँच",independent?.optJSONObject("result")?.optBoolean("complete")==true),
            Stage("review","Review & transfer practice","समीक्षा और बदले दृश्य में अभ्यास",challenges.any{it.optString("scenarioId") in moduleScenes&&it.optBoolean("finished")&&it.optBoolean("passed")}),
            Stage("assessment","Knowledge assessment","ज्ञान मूल्यांकन",assessment?.optString("contentVersion")==curriculum.version&&assessment.optJSONObject("result")?.optBoolean("passed")==true))
    }
    fun export()=JSONObject().put("contentVersion",curriculum.version).put("lessons",JSONArray(curriculum.modules.filter{prefs.getBoolean(key(it.getString("id")),false)}.map{it.getString("id")}))
}
