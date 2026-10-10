package com.narvyn.suraksha

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket

@RunWith(AndroidJUnit4::class)
class RoleDashboardOfflineTest {
 @Test fun cachedLearnerCanPractiseOfflineAndOwnerDoesNotReceiveLearningTools(){
  val instrument=InstrumentationRegistry.getInstrumentation();val context=instrument.targetContext
  val prefs=context.getSharedPreferences("admin_session",0);val saved=prefs.all.toMap();val api=AdminClient(context)
  val server=ServerSocket(0,10,java.net.InetAddress.getByName("127.0.0.1"))
  val failing=java.util.concurrent.atomic.AtomicBoolean(false);val role=java.util.concurrent.atomic.AtomicReference("personal")
  val serving=Thread{
   try{while(!server.isClosed)server.accept().use{socket->
    val reader=socket.getInputStream().bufferedReader();val request=reader.readLine()?:return@use;var length=0
    while(true){val line=reader.readLine()?:break;if(line.isEmpty())break;if(line.startsWith("Content-Length:",true))length=line.substringAfter(':').trim().toInt()}
    val chars=CharArray(length);var read=0;while(read<length){val n=reader.read(chars,read,length-read);if(n<0)break;read+=n}
    val signIn=request.contains("/api/auth/sign-in/");val fail=!signIn&&failing.get()
    val title=if(role.get()=="platform")"Platform owner" else "Personal learner"
    val c=JSONObject().put("role",role.get()).put("label",title).put("user",JSONObject().put("displayName","Synthetic offline fixture")).put("workspaces",JSONArray())
    val result=if(signIn)JSONObject() else JSONObject().put("context",c).put("stats",JSONArray()).put("actions",JSONArray()).put("queue",JSONArray())
    val bytes=result.toString().toByteArray();socket.getOutputStream().apply{write(("HTTP/1.1 ${if(fail)503 else 200} OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n"+(if(signIn)"Set-Cookie: better-auth.session_token=synthetic-offline; Path=/; HttpOnly\r\n"else "")+"\r\n").toByteArray());write(bytes);flush()}
   }}catch(_:java.net.SocketException){}
  }.apply{isDaemon=true;start()}
  try{
   api.configure("http://127.0.0.1:${server.localPort}");api.remember(false)
   for(value in listOf("personal","platform")){
    role.set(value);failing.set(false);api.clear();api.call("/api/auth/sign-in/email",JSONObject())
    ActivityScenario.launch<RoleDashboardActivity>(Intent(context,RoleDashboardActivity::class.java)).use{scenario->
     awaitText(scenario,"Refresh dashboard");assertEquals(value,api.lastWorkspaceRole())
     instrument.uiAutomation.takeScreenshot()?.let{bitmap->java.io.File(context.filesDir,"native-role-$value.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
     failing.set(true);scenario.recreate();val texts=awaitText(scenario,"Workspace unavailable")
     assertEquals(value,value=="personal",texts.contains("Continue offline learning"))
    }
    api.clear();assertEquals("",api.lastWorkspaceRole())
   }
  }finally{server.close();serving.join(1500);api.clear();val edit=prefs.edit().clear();saved.forEach{(k,v)->when(v){is String->edit.putString(k,v);is Boolean->edit.putBoolean(k,v)}};edit.commit()}
 }
 private fun awaitText(scenario:ActivityScenario<RoleDashboardActivity>,value:String):List<String>{var texts=listOf<String>();for(attempt in 0..70){scenario.onActivity{texts=allText(it.window.decorView)};if(texts.contains(value))return texts;Thread.sleep(100)};fail("Missing expected UI: $value");return texts}
 private fun allText(view:View):List<String>{val out=mutableListOf<String>();if(view is TextView)out.add(view.text.toString());if(view is ViewGroup)for(i in 0 until view.childCount)out.addAll(allText(view.getChildAt(i)));return out}
}
