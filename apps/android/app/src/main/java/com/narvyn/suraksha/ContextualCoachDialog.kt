package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Authored assistance is always local. External selection is a separate, per-request opt-in.
 * The host must dismiss the returned dialog onPause/onDestroy and supply a current-step guard. */
internal object ContextualCoachDialog {
    fun show(
        host:Activity,
        store:Store,
        contentVersion:String,
        scenario:JSONObject,
        step:JSONObject,
        recentActionIds:List<String> = emptyList(),
        selectedObjectId:String? = null,
        assessment:Boolean = false,
        isCurrent:()->Boolean = {true},
        onAction:(String)->Unit = {}
    ):AlertDialog = CoachSheet(host,store,contentVersion,scenario,step,recentActionIds,selectedObjectId,assessment,isCurrent,onAction).show()
}

private class CoachSheet(
    private val host:Activity,
    private val store:Store,
    private val version:String,
    scenario:JSONObject,
    step:JSONObject,
    recent:List<String>,
    selectedObjectId:String?,
    private val assessment:Boolean,
    private val stillCurrent:()->Boolean,
    private val onAction:(String)->Unit
) {
    // Snapshot the context: an asynchronous reply cannot drift onto a later objective.
    private val scenario=JSONObject(scenario.toString())
    private val step=JSONObject(step.toString())
    private val selectedObject=selectedObjectId?.takeIf{it==this.step.optString("part")}
    private val actions=recent.filter { id -> this.scenario.getJSONArray("steps").objects().any { s -> s.getJSONArray("choices").objects().any { it.getString("id")==id } } }.takeLast(12)
    private val api=AdminClient(host.applicationContext)
    private val stamp=api.identityStamp()
    private val worker=store.workerId
    private val hindi=store.hi
    private val executor=Executors.newSingleThreadExecutor()
    private val handler=Handler(Looper.getMainLooper())
    private var pending:Future<*>?=null
    private var serial=0
    private var closed=false
    private val body=host.column(12)
    private val panel=host.paged(body,hindi)
    private lateinit var dialog:AlertDialog
    private fun t(en:String,hi:String)=if(hindi)hi else en
    fun show():AlertDialog {
        dialog=PageDialogBuilder(host).setTitle(t("Learning help","सीखने में मदद")).setView(panel)
            .setNegativeButton(t("Close","बंद करें"),null).create()
        dialog.setOnDismissListener { closed=true;serial++;pending?.cancel(true);executor.shutdownNow();handler.removeCallbacksAndMessages(null) }
        renderLocal();dialog.show();return dialog
    }
    private fun current():Boolean = !closed&&!host.isFinishing&&!host.isDestroyed&&store.isCurrentProfile()&&store.workerId==worker&&api.identityStamp()==stamp&&stillCurrent()
    private fun reset(){body.removeAllViews();panel.firstPage()}
    private fun title(text:String){body.add(host.label(text,14f,Palette.blue,true).asHeading(),bottom=10)}
    private fun localButton(){body.add(host.action(t("Offline guidance","ऑफ़लाइन मार्गदर्शन"),false,ActionRole.NEUTRAL){serial++;pending?.cancel(true);renderLocal()},top=12)}
    private fun renderLocal(){
        reset()
        if(assessment){title(t("Assessment in progress","मूल्यांकन चल रहा है"));body.add(host.label(t("Finish or leave the assessment before opening learning help.","सीखने में मदद खोलने से पहले मूल्यांकन पूरा करें या उससे बाहर आएँ।")));return}
        title(t("Authored guidance · offline","लिखित मार्गदर्शन · ऑफ़लाइन"))
        if(hindi)body.add(host.label("इस नए पाठ का विस्तृत मार्गदर्शन अभी अंग्रेज़ी में है।",13f,Palette.muted),bottom=8)
        body.add(host.label(step.getString("explanation"),16f),bottom=12)
        body.add(host.label(t("Source-checked draft. Qualified review is pending; this is not live AI.","स्रोतों से जाँचा गया मसौदा। योग्य विशेषज्ञ की समीक्षा बाकी है; यह लाइव AI नहीं है।"),13f,Palette.muted),bottom=12)
        body.add(host.action(t("Source & review status","स्रोत और समीक्षा"),false,ActionRole.NEUTRAL){renderSource()},bottom=8)
        body.add(host.action(t("Online controls help","नियंत्रणों की ऑनलाइन मदद"),false){request("controls",false)})
    }
    private fun renderSource(){
        reset();title(t("Lesson source","पाठ का स्रोत"))
        val source=step.optString("source")
        body.add(host.label(source,14f).apply{setTextIsSelectable(true)},bottom=12)
        if(safeUrl(source))body.add(host.action(t("Open source","स्रोत खोलें"),false){openLink(source)},bottom=10)
        body.add(host.label(t("Version: ","संस्करण: ")+version,13f,Palette.muted),bottom=8)
        body.add(host.label(t("Draft · no qualified reviewer recorded. A source link is not approval of this lesson.","मसौदा · योग्य समीक्षक दर्ज नहीं है। स्रोत का लिंक पाठ की मंज़ूरी नहीं है।"),15f))
        localButton()
    }
    private fun request(intent:String,external:Boolean){
        if(!current()){dialog.dismiss();return}
        if(!api.hasSession()){
            reset();title(t("Online help needs an account","ऑनलाइन मदद के लिए खाता चाहिए"))
            body.add(host.label(t("Offline guidance is available without signing in. You can connect your Suraksha account from Account.","बिना साइन इन किए ऑफ़लाइन मार्गदर्शन उपलब्ध है। खाता अनुभाग से अपना सुरक्षा खाता जोड़ सकते हैं।"),16f),bottom=12)
            body.add(host.action(t("Open Account","खाता खोलें"),false){dialog.dismiss();host.startActivity(Intent(host,LearnerCentreActivity::class.java))})
            localButton();return
        }
        val requestId=++serial
        reset();title(t("Loading learning help…","सीखने की मदद लोड हो रही है…"))
        body.add(host.label(if(external)t("Only course context and recent choice IDs are sent to the configured provider.","केवल पाठ का संदर्भ और हाल के विकल्पों के ID चयनित सेवा को भेजे जाते हैं।")else t("Checking authored help on your account service. No request is sent to an AI provider.","आपकी खाता सेवा पर लिखित मदद देखी जा रही है। AI सेवा को अनुरोध नहीं भेजा जाता।"),15f))
        localButton()
        val payload=JSONObject().put("courseId",scenario.getString("course")).put("contentVersion",version)
            .put("scenarioId",scenario.getString("id")).put("objectiveId",step.getString("id"))
            .put("recentActionIds",JSONArray(actions)).put("hintLevel",intent).put("language",if(hindi)"hi"else"en")
            .put("mode",if(assessment)"assessment"else"learn").put("allowExternalCoaching",external)
        selectedObject?.let{payload.put("selectedObjectId",it)}
        if(LearningSync.linked(host,worker))payload.put("workerId",worker)
        pending=executor.submit {
            try {
                val reply=api.call("/api/tutor/context",payload,expectedIdentity=stamp)
                handler.post {
                    if(closed||requestId!=serial)return@post
                    if(!current()){dialog.dismiss();return@post}
                    if(reply.optInt("schemaVersion")!=1||reply.optString("contentVersion")!=version){renderFailure();return@post}
                    renderOnline(reply)
                }
            }catch(_:Exception){handler.post {if(!closed&&requestId==serial){if(current())renderFailure()else dialog.dismiss()}}}
        }
    }
    private fun renderFailure(){
        reset();title(t("Online help unavailable","ऑनलाइन मदद उपलब्ध नहीं"))
        body.add(host.label(t("Your lesson and offline guidance still work. Check the connection or account service and try again.","आपका पाठ और ऑफ़लाइन मार्गदर्शन उपलब्ध हैं। कनेक्शन या खाता सेवा जाँचकर फिर कोशिश करें।"),16f),bottom=10)
        body.add(host.action(t("Retry controls help","नियंत्रणों की मदद फिर खोलें"),false){request("controls",false)})
        localButton()
    }
    private fun renderOnline(reply:JSONObject){
        reset()
        val ai=reply.optString("mode")=="provider-selection"&&reply.optString("aiStatus")=="selected"
        title(if(ai)t("AI-selected authored help","AI से चुनी गई लिखित मदद")else t("Authored help · account service","लिखित मदद · खाता सेवा"))
        if(reply.optString("responseLanguage")!=if(hindi)"hi"else"en")body.add(host.label(t("This reply is in English.","यह उत्तर अंग्रेज़ी में है।"),13f,Palette.muted),bottom=8)
        body.add(host.label(reply.optString("text").take(3000),16f).apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE},bottom=12)
        val permitted=setOf("inspect-selection","show-step","open-sources","open-replay")
        reply.optJSONArray("actions")?.objects()?.take(4)?.forEach { action ->
            val id=action.optString("id")
            if(id in permitted)body.add(host.action(action.optString("label").take(80),false,ActionRole.NEUTRAL){
                if(!current()){dialog.dismiss();return@action}
                if(id=="open-sources")renderSource()else{dialog.dismiss();onAction(id)}
            },bottom=8)
        }
        val disclosure=reply.optJSONObject("providerDisclosure")
        if(disclosure==null){
            body.add(host.label(t("Live AI selection is unavailable for this request. Authored help remains available.","इस अनुरोध के लिए लाइव AI चयन उपलब्ध नहीं है। लिखित मदद उपलब्ध है।"),13f,Palette.muted),top=8)
        }else if(!ai){
            body.add(host.action(t("Optional AI selection","वैकल्पिक AI चयन"),false){renderConsent(disclosure)},top=8)
        }
        // Requesting an explanation checks the approval gate; it never upgrades this draft.
        if(reply.optString("status")=="supported")body.add(host.action(t("Check lesson coaching availability","पाठ की कोचिंग की उपलब्धता देखें"),false,ActionRole.NEUTRAL){request("explain",false)},top=8)
        localButton()
    }
    private fun safeUrl(value:String):Boolean = runCatching {val uri=java.net.URI(value);uri.scheme=="https"&&!uri.host.isNullOrBlank()&&uri.userInfo==null}.getOrDefault(false)
    private fun openLink(value:String){
        if(!safeUrl(value)||!current())return
        try{host.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(value)));dialog.dismiss()}
        catch(_:android.content.ActivityNotFoundException){body.add(host.label(t("No browser is available. You can select and copy the source address.","ब्राउज़र उपलब्ध नहीं है। स्रोत का पता चुनकर कॉपी कर सकते हैं।"),14f,Palette.danger),top=10)}
    }
    private fun renderConsent(disclosure:JSONObject){
        reset();title(t("Optional provider processing","वैकल्पिक सेवा द्वारा संसाधन"))
        body.add(host.label(disclosure.optString("name").take(100),17f,Palette.ink,true),bottom=8)
        body.add(host.label(disclosure.optString("dataUse").take(600),15f),bottom=8)
        val privacy=disclosure.optString("privacyUrl").take(500)
        body.add(host.label(privacy,13f,Palette.muted).apply{setTextIsSelectable(true)},bottom=10)
        if(safeUrl(privacy))body.add(host.action(t("Open provider privacy notice","सेवा की गोपनीयता सूचना खोलें"),false,ActionRole.NEUTRAL){openLink(privacy)},bottom=10)
        body.add(host.label(t("This request shares course, step, selected item and recent choice IDs. No name, camera, microphone, notes or separate score is sent. Choice IDs may reveal your decisions. School users should follow their institution’s consent process.","यह अनुरोध पाठ, चरण, चयनित भाग और हाल के विकल्पों के ID साझा करता है। नाम, कैमरा, माइक्रोफ़ोन, टिप्पणियाँ या अलग स्कोर नहीं भेजे जाते। विकल्पों के ID से आपके निर्णय पता चल सकते हैं। स्कूल के उपयोगकर्ता संस्था की सहमति प्रक्रिया का पालन करें।"),14f),bottom=10)
        val consent=CheckBox(host).apply{text=t("Allow for this request","इस अनुरोध के लिए अनुमति दें");isChecked=false;minHeight=host.dp(48)}
        body.add(consent,bottom=8)
        val send=host.action(t("Use AI selection","AI चयन का उपयोग करें")){request("controls",true)}.apply{isEnabled=false}
        consent.setOnCheckedChangeListener { _,checked -> send.isEnabled=checked }
        body.add(send);localButton()
    }
}
