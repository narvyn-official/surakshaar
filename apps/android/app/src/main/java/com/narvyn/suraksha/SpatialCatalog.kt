package com.narvyn.suraksha

import android.content.Context
import org.json.JSONObject

/** Shared presentation map. Decision authority stays in LearningAttempt / ProcedureSession. */
internal class SpatialCatalog(context:Context) {
    val data=JSONObject(context.assets.open("spatial/catalog.json").bufferedReader().use{it.readText()})
    fun step(scenario:String,step:String):JSONObject?=data.getJSONObject("steps").optJSONObject("$scenario/$step")
    fun parts(module:String)=data.getJSONObject("models").optJSONObject(module)?.getJSONArray("parts")?.objects().orEmpty()
    fun base(module:String)=when {
        module.startsWith("fire-") && module in setOf("fire-damaged","fire-pin-removed","fire-aimed","fire-discharging")->"fire"
        module.startsWith("machinery-")->"machinery"
        module=="ppe-damaged"->"ppe"
        else->module
    }
}
