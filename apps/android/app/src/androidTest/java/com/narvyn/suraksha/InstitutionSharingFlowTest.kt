package com.narvyn.suraksha

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.hamcrest.Matchers.startsWith
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.text.DateFormat
import java.util.Date

/** Real local backend + actual native panels. Disposable fixture, never a learner-device test. */
@RunWith(AndroidJUnit4::class)
class InstitutionSharingFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private fun views(view:View):List<View> = listOf(view)+(if(view is ViewGroup)(0 until view.childCount).flatMap{views(view.getChildAt(it))}else emptyList())
    private fun waitFor(message:String,check:()->Boolean){repeat(200){if(check())return;Thread.sleep(100)};error(message)}
    private fun texts(dialog:AlertDialog):List<String>{var result=emptyList<String>();ins.runOnMainSync{result=views(dialog.window!!.decorView).filterIsInstance<TextView>().map{it.text.toString()}};return result}
    private fun tap(text:String){onView(withText(text)).perform(revealOnPage(),click())}
    private fun screenshot(name:String){ins.waitForIdleSync();val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable");File(context.getExternalFilesDir("validation"),name+".png").outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle()}
    private fun restore(prefs:SharedPreferences,saved:Map<String,*>){val edit=prefs.edit().clear();for((key,value)in saved)when(value){is String->edit.putString(key,value);is Boolean->edit.putBoolean(key,value);is Int->edit.putInt(key,value);is Long->edit.putLong(key,value);is Float->edit.putFloat(key,value);is Set<*>->edit.putStringSet(key,value.filterIsInstance<String>().toSet())};assertTrue(edit.commit())}
    private fun publicRecord(origin:String,token:String):JSONObject{
        val connection=URI(origin+"/api/public-learning-record").toURL().openConnection()as HttpURLConnection
        try{connection.requestMethod="POST";connection.connectTimeout=12000;connection.readTimeout=20000;connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Origin",origin)
            connection.outputStream.use{it.write(JSONObject().put("token",token).toString().toByteArray())};assertEquals(200,connection.responseCode)
            return JSONObject(connection.inputStream.bufferedReader().use{it.readText()})
        }finally{connection.disconnect()}
    }

    @Test fun nativeInstitutionSelectionAndExplicitFormativeQrRevocation(){
        assertTrue("Synthetic fixture requires an emulator",Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val api=AdminClient(context);assertFalse("Run signed out; never replace a learner session",api.hasSession())
        val fixtureFile=File(context.filesDir,"native-institution-fixture.json")
        assertTrue("Prepare the disposable local fixture first",fixtureFile.isFile)
        val fixture=JSONObject(fixtureFile.readText());assertEquals("isolated-native-institution-qr",fixture.getString("purpose"));assertEquals("http://localhost:5173",fixture.getString("origin"))
        val worker=fixture.getString("workerId");val original=Store(context).use{it.workerId}
        val prefs=context.getSharedPreferences("admin_session",0);val savedPrefs=prefs.all.toMap()
        val preferences=context.getSharedPreferences("preferences",0);val savedProfilePrefs=preferences.all.toMap()
        val evidenceBefore=Store(context).use{JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()}
        var createdProfile=false
        try{
            Store(context).use{store->
                assertFalse(store.profiles().any{it.getString("id")==worker})
                store.writableDatabase.insertOrThrow("worker_profiles",null,ContentValues().apply{put("id",worker);put("name","Synthetic native learner");put("sector","Unspecified");put("created_at",System.currentTimeMillis())});createdProfile=true
                store.switchProfile(worker);store.hi=false
            }
            api.configure(fixture.getString("origin"));api.remember(false)
            api.call("/api/auth/sign-in/email",JSONObject().put("email",fixture.getString("email")).put("password",fixture.getString("password")))
            assertTrue(api.hasSession());assertFalse(prefs.contains("cookies"))
            val linked=api.call("/api/learner").getJSONArray("links").objects().filter{it.getString("worker_id")==worker};assertEquals(2,linked.size)
            Store(context).use{store->ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use{scenario->
                lateinit var dialog:AlertDialog
                scenario.onActivity{dialog=LearningCentreDialog.showLearner(it,store)}
                waitFor("Native membership choices did not load"){texts(dialog).containsAll(listOf("Native QA North","Native QA South"))}
                screenshot("institution-membership-choice")
                tap("Native QA North")
                waitFor("North institution did not load"){texts(dialog).contains("Change institution")&&texts(dialog).any{it.startsWith("Fire Safety ·")}}
                assertTrue(texts(dialog).contains("Native QA North"));assertFalse(texts(dialog).contains("No assignments yet."))
                tap("Change institution");waitFor("Institution chooser did not reopen"){texts(dialog).contains("Native QA South")};tap("Native QA South")
                waitFor("South institution did not load"){texts(dialog).contains("No assignments yet.")}
                assertTrue(texts(dialog).contains("Native QA South"));assertFalse(texts(dialog).any{it.startsWith("Fire Safety ·")})
                screenshot("institution-south-isolation")
                ins.runOnMainSync{dialog.dismiss()}
                scenario.onActivity{dialog=SavedPracticeDialog.show(it,store)}
                waitFor("Private practice list did not load"){texts(dialog).any{it.startsWith("school-fire-help ·")}}
                onView(withText(startsWith("school-fire-help ·"))).perform(revealOnPage(),click())
                waitFor("Private practice detail unavailable"){texts(dialog).contains("Practice QR sharing")};tap("Practice QR sharing")
                waitFor("Share controls unavailable"){texts(dialog).contains("Create a 7-day share QR")}
                val sharePath="/api/learning-practice/shares?workerId=$worker&attemptId=${fixture.getString("attemptId")}";assertEquals(0,api.call(sharePath).getJSONArray("shares").length())
                tap("Create a 7-day share QR");onView(withText("Publish this metadata?")).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()))
                tap("Cancel");assertEquals("Consent cancellation must create nothing",0,api.call(sharePath).getJSONArray("shares").length())
                tap("Create a 7-day share QR");tap("Continue")
                waitFor("Generated QR screen unavailable"){texts(dialog).contains("Formative practice · 7-day share")}
                onView(withContentDescription("Practice share QR")).perform(revealOnPage())
                var bitmap:Bitmap?=null;ins.runOnMainSync{bitmap=((views(dialog.window!!.decorView).filterIsInstance<ImageView>().single{it.contentDescription=="Practice share QR"}).drawable as BitmapDrawable).bitmap.copy(Bitmap.Config.ARGB_8888,false)}
                val image=requireNotNull(bitmap);val pixels=IntArray(image.width*image.height);image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
                val decoded=QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width,image.height,pixels)))).text;image.recycle()
                val url=URI(decoded);assertEquals(fixture.getString("origin"),"${url.scheme}://${url.authority}");assertEquals("/practice-record",url.path);assertNull(url.query)
                val token=url.fragment.removePrefix("token=");assertTrue(token.matches(Regex("[A-Za-z0-9_-]{43}")))
                val active=publicRecord(fixture.getString("origin"),token);assertEquals("active",active.getString("status"));assertFalse(active.getBoolean("certifiable"))
                for(privateValue in listOf(worker,fixture.getString("email"),fixture.getString("attemptId"),"Synthetic private reflection","answers"))assertFalse("Public metadata contained private evidence",active.toString().contains(privateValue))
                screenshot("formative-share-qr")
                tap("Revoke link");waitFor("Revoked share history unavailable"){texts(dialog).any{it.startsWith("Revoked")}}
                assertEquals("revoked",publicRecord(fixture.getString("origin"),token).getString("status"));assertFalse(publicRecord(fixture.getString("origin"),token).has("record"))
                val shares=api.call(sharePath).getJSONArray("shares");assertEquals(1,shares.length())
                val revokedAt=shares.getJSONObject(0).getLong("revoked_at")
                assertTrue("Revoked history must show revocation time, not future expiry",texts(dialog).contains("Revoked · "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(revokedAt))))
                assertEquals(1,api.call("/api/learning-practice?workerId=$worker").getJSONArray("records").length())
                screenshot("formative-share-revoked");ins.runOnMainSync{dialog.dismiss()}
            }}
        }finally{
            api.clear();restore(prefs,savedPrefs)
            if(createdProfile)Store(context).use{it.switchProfile(original);it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
            restore(preferences,savedProfilePrefs);fixtureFile.delete()
            assertEquals(evidenceBefore,Store(context).use{JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()})
        }
    }
}
