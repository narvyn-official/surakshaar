package com.narvyn.suraksha

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in local HTTP fixtures; never logs in to a live institute. */
@RunWith(AndroidJUnit4::class)
class RoleDashboardFlowTest {
 @Test fun eachAccountOpensItsAuthorisedNativeWorkspace(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val fixture=java.io.File(context.filesDir,"role-dashboard-fixture.json")
  assumeTrue("Install isolated synthetic fixtures to run this transport test",fixture.exists())
  val data=JSONObject(fixture.readText());assertEquals("http://127.0.0.1:5196",data.getString("origin"))
  val actors=data.getJSONObject("actors");val prefs=context.getSharedPreferences("admin_session",0);val saved=prefs.all.toMap();val api=AdminClient(context)
  try{
   api.configure(data.getString("origin"));api.remember(false)
   for(role in listOf("platform","admin","trainer","examiner","certifier","assessor","auditor","author","reviewer","viewer","learner","personal","supporter")){
    api.clear();val actor=actors.getJSONObject(role)
    for(attempt in 0..4){try{api.call("/api/auth/sign-in/email",JSONObject().put("email",actor.getString("email")).put("password",actor.getString("password")));break}catch(e:IllegalStateException){if(attempt==4||e.message?.startsWith("Too many attempts")!=true)throw e;Thread.sleep(16000)}}
    val expected=api.call("/api/dashboard");assertEquals(role,expected.getJSONObject("context").getString("role"))
    ActivityScenario.launch<RoleDashboardActivity>(Intent(context,RoleDashboardActivity::class.java)).use{scenario->
     var texts=listOf<String>();var ready=false
     for(attempt in 0..60){scenario.onActivity{a->texts=allText(a.window.decorView)};ready=texts.contains(expected.getJSONObject("context").getString("label"))&&texts.contains("Refresh dashboard");if(ready)break;Thread.sleep(100)}
     assertTrue("Dashboard ready for $role",ready);assertEquals(role,api.lastWorkspaceRole())
     val learning=RoleDashboardPolicy.learningRole(role)
     assertEquals(role,learning,texts.contains("Your learning tools"))
     assertEquals(role,!learning,texts.contains("Your responsibilities"))
     val actions=expected.getJSONArray("actions");for(i in 0 until actions.length())assertTrue("$role action visible",texts.contains(actions.getJSONObject(i).getString("title")))
     scenario.recreate()
    }
    api.call("/api/auth/sign-out",JSONObject());api.clear();assertEquals("",api.lastWorkspaceRole())
   }
  }finally{api.clear();val edit=prefs.edit().clear();saved.forEach{(k,v)->when(v){is String->edit.putString(k,v);is Boolean->edit.putBoolean(k,v)}};edit.commit();fixture.delete()}
 }
 private fun allText(view:View):List<String>{val result=mutableListOf<String>();if(view is TextView)result.add(view.text.toString());if(view is ViewGroup)for(i in 0 until view.childCount)result.addAll(allText(view.getChildAt(i)));return result}
}
