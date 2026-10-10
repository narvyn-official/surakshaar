package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.widget.EditText
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Lifecycle-owned account panel. Server permissions stay authoritative for every action. */
internal abstract class LearningAccountSheet(protected val host:Activity,protected val store:Store?,private val heading:String) {
    protected val api=AdminClient(host.applicationContext)
    private val identity=api.identityStamp()
    protected val hi=store?.hi?:Store(host).use{it.hi}
    protected val body=host.column(12)
    private val pages=host.paged(body,hi)
    private val status=host.label("",14f,Palette.muted)
    private val handler=Handler(Looper.getMainLooper())
    private val executor=Executors.newSingleThreadExecutor()
    private var pending:Future<*>?=null
    private var sequence=0
    private var closed=false
    protected var busy=false
    protected lateinit var dialog:AlertDialog
    private val lifecycle=object:Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity:Activity){if(activity===host&&::dialog.isInitialized)dialog.dismiss()}
        override fun onActivityDestroyed(activity:Activity){if(activity===host&&::dialog.isInitialized)dialog.dismiss()}
        override fun onActivityCreated(activity:Activity,state:Bundle?){}
        override fun onActivityStarted(activity:Activity){}
        override fun onActivityResumed(activity:Activity){}
        override fun onActivityStopped(activity:Activity){}
        override fun onActivitySaveInstanceState(activity:Activity,state:Bundle){}
    }
    protected fun t(en:String,hindi:String)=if(hi)hindi else en
    protected fun current()=!closed&&!host.isFinishing&&!host.isDestroyed&&api.identityStamp()==identity&&(store==null||store.isCurrentProfile())
    fun show():AlertDialog {
        body.add(status,bottom=8)
        status.accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
        dialog=PageDialogBuilder(host).setTitle(heading).setView(pages).setNegativeButton(t("Close","बंद करें"),null).create()
        dialog.setOnDismissListener{closed=true;sequence++;pending?.cancel(true);executor.shutdownNow();handler.removeCallbacksAndMessages(null);host.application.unregisterActivityLifecycleCallbacks(lifecycle)}
        host.application.registerActivityLifecycleCallbacks(lifecycle);dialog.show()
        if(api.hasSession())initial()else signIn()
        return dialog
    }
    protected abstract fun initial()
    protected fun reset(){body.removeAllViews();body.add(status,bottom=8);pages.firstPage();status.text=""}
    protected fun text(value:String,small:Boolean=false){body.add(host.label(value,if(small)13f else 16f,if(small)Palette.muted else Palette.ink),bottom=10)}
    protected fun button(label:String,primary:Boolean=false,action:()->Unit){body.add(host.action(label,false,if(primary)ActionRole.PRIMARY else ActionRole.NEUTRAL){if(!busy&&current())action()},bottom=8)}
    protected fun field(label:String,max:Int=1000):EditText {
        val input=EditText(host).apply{minHeight=host.dp(48);maxLines=4;filters=arrayOf(InputFilter.LengthFilter(max));id=android.view.View.generateViewId();setTextColor(Palette.ink);contentDescription=label;importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO}
        body.add(host.label(label,14f,Palette.muted,true).apply{labelFor=input.id},top=8);body.add(input,bottom=10);return input
    }
    protected fun request(path:String,payload:JSONObject?=null,done:(JSONObject)->Unit){
        if(busy||!current())return
        busy=true;val token=++sequence;status.text=t("Connecting…","कनेक्ट हो रहा है…")
        pending=executor.submit{try{val result=api.call(path,payload,expectedIdentity=identity);handler.post{if(token!=sequence||closed)return@post;busy=false;if(!current()){dialog.dismiss();return@post};status.text="";try{done(result)}catch(_:Exception){status.text=t("Could not display the record. Close and reopen this panel.","रिकॉर्ड नहीं दिखा। पैनल बंद करके दोबारा खोलें।")}}}catch(e:Exception){handler.post{if(token!=sequence||closed)return@post;busy=false;if(!current()){dialog.dismiss();return@post};status.text=e.message?:t("Unable to connect. Your local learning is unchanged.","कनेक्शन नहीं हो सका। स्थानीय सीखने के रिकॉर्ड सुरक्षित हैं।")}}}
    }
    protected fun confirm(title:String,message:String,action:()->Unit){PageDialogBuilder(host).setTitle(title).setMessage(message).setNegativeButton(t("Cancel","रद्द करें"),null).setPositiveButton(t("Continue","जारी रखें")){_,_->if(current())action()}.show()}
    protected fun share(value:String,title:String){if(!current())return;try{host.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value).putExtra(Intent.EXTRA_SUBJECT,title),title))}catch(_:android.content.ActivityNotFoundException){status.text=t("No export app is available.","निर्यात के लिए ऐप उपलब्ध नहीं है।")}}
    /** Revalidate membership for every new panel; never select one of several centres by row order. */
    protected fun chooseCentre(owner:String?=null,done:(String,String)->Unit){
        val profile=store?:return
        request("/api/learner"){reply->
            val links=reply.getJSONArray("links").objects().filter{it.getString("worker_id")==profile.workerId}
            val chosen=if(owner!=null)links.find{it.getString("owner")==owner}else links.singleOrNull()
            if(chosen!=null){done(chosen.getString("owner"),chosen.getString("centre"));return@request}
            reset();text(t("Choose institution","संस्था चुनें"))
            if(owner!=null)text(t("Your previous institution is no longer available. Choose a current enrolment.","पहली संस्था अब उपलब्ध नहीं है। वर्तमान नामांकन चुनें।"),true)
            if(links.isEmpty()){
                text(t("No approved institution is linked to this learner profile. Private learning still works.","इस शिक्षार्थी प्रोफ़ाइल से कोई स्वीकृत संस्था जुड़ी नहीं है। निजी सीखना उपलब्ध है।"))
                button(t("Manage enrolment","नामांकन प्रबंधित करें")){dialog.dismiss();host.startActivity(Intent(host,LearnerCentreActivity::class.java))}
            }else links.forEach{link->button(link.getString("centre")){done(link.getString("owner"),link.getString("centre"))}}
        }
    }
    private fun signIn(){reset();text(t("Connect your Suraksha account to view these records. Offline learning remains available.","इन रिकॉर्ड के लिए अपना सुरक्षा खाता जोड़ें। ऑफ़लाइन सीखना उपलब्ध है।"));button(t("Open account","खाता खोलें"),true){dialog.dismiss();host.startActivity(Intent(host,if(store==null)AdminActivity::class.java else LearnerCentreActivity::class.java))}}
}
