package com.narvyn.suraksha

import android.content.SharedPreferences
import android.content.Intent
import android.os.Build
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LearnerAccountFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private fun views(view:View):List<View> = listOf(view)+(if(view is ViewGroup)(0 until view.childCount).flatMap{views(view.getChildAt(it))}else emptyList())
    private fun tap(text:String){onView(withText(text)).perform(revealOnPage(),click())}
    private fun field(s:ActivityScenario<LearnerCentreActivity>,id:Int,value:String){s.onActivity{it.findViewById<EditText>(id).setText(value)}}
    private fun waitFor(s:ActivityScenario<LearnerCentreActivity>,text:String){repeat(200){var found=false;s.onActivity{a->found=views(a.window.decorView).filterIsInstance<TextView>().any{it.text.toString().contains(text)}};if(found)return;Thread.sleep(100)};error("Account screen did not reach expected state: $text")}
    private fun restore(prefs:SharedPreferences,saved:Map<String,*>){val edit=prefs.edit().clear();for((key,value)in saved)when(value){is String->edit.putString(key,value);is Boolean->edit.putBoolean(key,value);is Int->edit.putInt(key,value);is Long->edit.putLong(key,value);is Float->edit.putFloat(key,value);is Set<*>->edit.putStringSet(key,value.filterIsInstance<String>().toSet())};assertTrue(edit.commit())}
    @Test fun validationRevealRecreationAndBackKeepAccountFlowUsable(){
        val api=AdminClient(context);assertFalse("Only run while signed out",api.hasSession());val prefs=context.getSharedPreferences("admin_session",0);val saved=prefs.all.toMap();val hi=Store(context).use{it.hi};Store(context).use{it.hi=false}
        try{api.configure("https://accounts.example.test");ActivityScenario.launch<LearnerCentreActivity>(Intent(context,LearnerCentreActivity::class.java)).use{s->
            waitFor(s,"Sign in to Suraksha");field(s,R.id.account_email,"invalid");field(s,R.id.account_password,"Local dummy text only");tap("Sign in")
            s.onActivity{assertNotNull(it.findViewById<EditText>(R.id.account_email).error)}
            tap("Show password");s.onActivity{a->val p=a.findViewById<EditText>(R.id.account_password);assertNull(p.transformationMethod);assertTrue(p.text.toString()=="Local dummy text only")}
            field(s,R.id.account_email,"learner@example.test");s.recreate();waitFor(s,"Sign in to Suraksha")
            s.onActivity{a->assertEquals("learner@example.test",a.findViewById<EditText>(R.id.account_email).text.toString());val p=a.findViewById<EditText>(R.id.account_password);assertEquals(0,p.length());assertTrue(p.transformationMethod is PasswordTransformationMethod);assertFalse(p.isSaveEnabled)}
            tap("Create account");waitFor(s,"Create Suraksha account");s.onActivity{assertTrue(it.findViewById<EditText>(R.id.account_password).autofillHints!!.contains("newPassword"))}
            pressBack();waitFor(s,"Sign in to Suraksha");tap("Forgot password?");waitFor(s,"Reset password");pressBack();waitFor(s,"Sign in to Suraksha")
        }}finally{api.clear();restore(prefs,saved);Store(context).use{it.hi=hi}}
    }
    @Test fun realPublicSignInPasswordChangeAndSignOutRetainOfflinePractice(){
        assertTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val api=AdminClient(context);assertFalse("Only run while signed out",api.hasSession());val prefs=context.getSharedPreferences("admin_session",0);val saved=prefs.all.toMap();val hi=Store(context).use{it.hi};Store(context).use{it.hi=false}
        val file=File(context.filesDir,"native-institution-fixture.json");assertTrue("Prepare disposable public preview fixture first",file.isFile);val fixture=JSONObject(file.readText());assertEquals("isolated-native-public-account",fixture.getString("purpose"));assertEquals(AdminClient.DEFAULT_ORIGIN,fixture.getString("origin"))
        val before=Store(context).use{it.export().apply{remove("exportedAt")}.toString()}
        try{api.configure(fixture.getString("origin"));ActivityScenario.launch<LearnerCentreActivity>(Intent(context,LearnerCentreActivity::class.java)).use{s->
            waitFor(s,"Sign in to Suraksha");field(s,R.id.account_email,fixture.getString("email"));field(s,R.id.account_password,fixture.getString("password"));tap("Sign in");waitFor(s,"Private practice & sync")
            assertTrue(api.hasSession());assertFalse(prefs.contains("cookies"));assertFalse(prefs.all.values.any{it.toString().contains(fixture.getString("password"))})
            tap("Account security");waitFor(s,"Update password");field(s,R.id.account_current_password,fixture.getString("password"));field(s,R.id.account_new_password,"Changed "+fixture.getString("password"));tap("Update password");waitFor(s,"Password updated.")
            s.onActivity{assertEquals(0,it.findViewById<EditText>(R.id.account_current_password).length());assertEquals(0,it.findViewById<EditText>(R.id.account_new_password).length())}
            tap("Back to account");tap("Sign out of this phone");waitFor(s,"Sign in to Suraksha");assertFalse(api.hasSession())
            tap("Forgot password?");waitFor(s,"Reset password");waitFor(s,"Email recovery is not enabled")
            assertEquals(before,Store(context).use{it.export().apply{remove("exportedAt")}.toString()})
        }}finally{api.clear();restore(prefs,saved);Store(context).use{it.hi=hi};file.delete()}
    }
}
