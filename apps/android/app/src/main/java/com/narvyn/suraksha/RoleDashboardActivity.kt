package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.concurrent.Executors

/** Account-scoped dashboard data and task links are authorised by the server. */
class RoleDashboardActivity:Activity(){
 private lateinit var api:AdminClient
 private lateinit var body:LinearLayout
 private val jobs=Executors.newSingleThreadExecutor()
 private var generation=0
 private var busy=false
 private var data:JSONObject?=null
 private fun screen(title:String){val root=column().apply{setBackgroundColor(Palette.canvas);fitsSystemWindows=true};val top=column().apply{setPadding(dp(20),dp(20),dp(20),dp(14));setBackgroundColor(Palette.surface)};top.add(label("SURAKSHAAR",12f,Palette.muted,true));top.add(label(title,24f,bold=true).asHeading(),top=8);root.add(top);body=column().apply{setPadding(dp(20),dp(16),dp(20),dp(24))};root.addView(paged(body,false),LinearLayout.LayoutParams(-1,0,1f));setContentView(root)}
 override fun onCreate(saved:Bundle?){super.onCreate(saved);api=AdminClient(this);screen("Your dashboard")}
 override fun onResume(){super.onResume();load()}
 override fun onDestroy(){generation++;jobs.shutdownNow();super.onDestroy()}
 private fun request(path:String,body:JSONObject?=null,done:(JSONObject)->Unit){if(busy)return;busy=true;val token=++generation;val stamp=api.identityStamp();jobs.execute{try{val result=api.forIdentity(stamp){if(body==null)api.call(path)else api.call(path,body)};val after=api.completedIdentityStamp();runOnUiThread{if(token!=generation||isDestroyed||isFinishing)return@runOnUiThread;busy=false;if(api.identityStamp()!=after){data=null;load();return@runOnUiThread};done(result)}}catch(e:Exception){runOnUiThread{if(token!=generation||isDestroyed||isFinishing)return@runOnUiThread;busy=false;screen("Workspace unavailable");this.body.add(label(e.message?:"Connect to load your current role and tools.",16f));button("Retry"){load()};button("Account sign-in"){startActivity(Intent(this,LearnerCentreActivity::class.java))};if(api.hasSession()&&RoleDashboardPolicy.learningRole(api.lastWorkspaceRole()))button("Continue offline learning"){startActivity(Intent(this,MainActivity::class.java))};if(!api.hasSession())button("Continue as guest"){startActivity(Intent(this,MainActivity::class.java));finish()}}}}}
 private fun load(){if(busy)return;if(!api.hasSession()){startActivity(Intent(this,LearnerCentreActivity::class.java));finish();return};screen("Opening your workspace…");body.add(label("Checking your account permissions.",15f,Palette.muted));request("/api/dashboard"){data=it;render(it)}}
 private fun button(title:String,primary:Boolean=false,work:()->Unit){body.add(action(title,role=if(primary)ActionRole.PRIMARY else ActionRole.NEUTRAL,onClick=work),top=10)}
 private fun web(path:String){if(RoleDashboardPolicy.localPath(path))startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(api.base+path)))}
 private fun render(value:JSONObject){val c=value.getJSONObject("context");val role=c.getString("role");api.rememberWorkspaceRole(role);screen(c.getString("label"));body.add(label(c.getJSONObject("user").getString("displayName"),18f,bold=true));c.optJSONObject("institution")?.let{body.add(label(it.getString("name"),15f,Palette.muted),top=6)}
 val choices=c.getJSONArray("workspaces").objects();if(role!="platform"&&choices.isNotEmpty())button("Choose institute"){PageDialogBuilder(this).setTitle("Your institutes").setItems(choices.map{it.getString("name")+" · "+it.getString("role")}.toTypedArray()){_,index->request("/api/admin/session",JSONObject().put("owner",choices[index].getString("owner"))){load()}}.show()}
 if(c.optBoolean("workspaceUnavailable")){body.add(label("Your previous workspace is unavailable. Choose an authorised institute to continue.",16f),top=18);button("Clear previous workspace"){request("/api/admin/session",JSONObject().put("clear",true)){load()}}}
 else if(c.optBoolean("mfaRequired")){val card=card(Palette.tealBg);card.add(label("Secure your account",22f,bold=true));card.add(label("Your role is assigned. Set up an authenticator before opening its records and tools. The browser uses its own sign-in session.",16f),top=12);body.add(card,top=20);button("Set up account security in browser",true){web("/account")}}
 else{
 if(c.optBoolean("restricted"))body.add(label("Your access is limited to assigned organisational units and their active subgroups. Institution-wide tools are unavailable.",15f,Palette.muted),top=12)
 val stats=value.getJSONArray("stats").objects();for(stat in stats){val card=card(Palette.surface);card.add(label(stat.getInt("value").toString(),30f,bold=true));card.add(label(stat.getString("label"),16f,bold=true),top=6);card.add(label(stat.getString("detail"),13f,Palette.muted),top=6);body.add(card,top=12)}
 body.add(label(if(RoleDashboardPolicy.learningRole(role))"Your learning tools" else "Your responsibilities",20f,bold=true).asHeading(),top=24)
 for((index,a) in value.getJSONArray("actions").objects().withIndex()){val path=a.getString("href");val id=a.getString("id");button(a.getString("title"),index==0){when{
 RoleDashboardPolicy.learningRole(role)&&id in setOf("learning","progress")->startActivity(Intent(this,MainActivity::class.java))
 role=="learner"&&id=="journey"->startActivity(Intent(this,LearnerCentreActivity::class.java))
 id=="coursework"->LearningCentreDialog.showEducator(this)
 path.startsWith("/workspace#")&&path.substringAfter('#') in RoleDashboardPolicy.legacySections(role)->startActivity(Intent(this,AdminActivity::class.java).putExtra("section",path.substringAfter('#')))
 else->web(path)
 }};body.add(label(a.getString("description")+(if(!path.startsWith("/workspace#")&&id!="coursework"&&!RoleDashboardPolicy.learningRole(role))" Opens in your browser." else ""),13f,Palette.muted),top=4)}
 val queue=value.getJSONArray("queue").objects();body.add(label(value.optString("queueTitle",if(role=="platform")"Institute applications" else if(role=="supporter")"Shared with you" else "Current work"),20f,bold=true).asHeading(),top=28);if(queue.isEmpty())body.add(label("Nothing waiting here yet.",15f,Palette.muted),top=10);for(q in queue){val card=card(Palette.surface);card.add(label(q.getString("title"),16f,bold=true));card.add(label(q.getString("detail"),13f,Palette.muted),top=8);card.add(action("Open",role=ActionRole.NEUTRAL){web(q.getString("href"))},top=10);body.add(card,top=10)}
 }
 button("Refresh dashboard"){load()};if(role!="platform")button("Institute access in browser"){web("/institute-access")};button("Account security in browser"){web("/account")};button("Sign out"){request("/api/auth/sign-out",JSONObject()){api.clear();data=null;startActivity(Intent(this,MainActivity::class.java));finish()}}
 body.add(label("SurakshaAR · ${c.getString("label")}\nAccess is checked by the service for every task.",12f,Palette.muted),top=24)
 }
}
