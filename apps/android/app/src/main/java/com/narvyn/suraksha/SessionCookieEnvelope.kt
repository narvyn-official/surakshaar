package com.narvyn.suraksha

import org.json.JSONObject

/** The origin is encrypted with the jar. Old unbound jars require a fresh sign-in. */
internal object SessionCookieEnvelope {
    fun create(origin:String,cookies:JSONObject)=JSONObject().put("version",1).put("origin",origin).put("cookies",JSONObject(cookies.toString()))
    fun read(envelope:JSONObject,origin:String):JSONObject? {
        if(origin.isBlank()||envelope.optInt("version")!=1||envelope.optString("origin")!=origin)return null
        val jar=envelope.optJSONObject("cookies")?:return null
        return JSONObject(jar.toString())
    }
}
