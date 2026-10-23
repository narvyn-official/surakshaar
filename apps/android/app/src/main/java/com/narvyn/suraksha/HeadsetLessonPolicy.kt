package com.narvyn.suraksha

import org.json.JSONObject

/** Shared headset entry policy; hiding a picker entry is not the runtime access check. */
internal object HeadsetLessonPolicy {
    val courses=setOf("fire","first-aid")
    fun allowed(scenario:JSONObject,school:Boolean)=scenario.optString("course") in courses&&
        (!school||scenario.optString("audience")=="awareness")&&scenario.optJSONArray("steps")?.length()?.let{it>0}==true
    fun lessons(scenarios:List<JSONObject>,course:String,school:Boolean)=scenarios.filter{it.optString("course")==course&&allowed(it,school)}
    fun ready(nativeReady:Boolean,trackingReady:Boolean,viewerReady:Boolean,spaceReady:Boolean,lessonAllowed:Boolean)=
        nativeReady&&trackingReady&&viewerReady&&spaceReady&&lessonAllowed
}
