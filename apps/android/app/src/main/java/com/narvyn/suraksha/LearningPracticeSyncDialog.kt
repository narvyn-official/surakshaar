package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Explicit, private upload only. The host must dismiss this dialog onPause/onDestroy.
 * No local record is changed, restored, linked to a centre, or used to issue a certificate. */
internal object LearningPracticeSyncDialog {
    fun show(host:Activity,store:Store):AlertDialog = PracticeSyncSheet(host,store).show()
}

/** Pure request planning, shared by the UI and boundary tests. JSON copies preserve snapshot values. */
internal object LearningPracticeUploadPlan {
    const val MAX_ATTEMPTS=20
    const val MAX_BODY_BYTES=900_000
    const val MAX_RECORD_BYTES=200_000
    fun bytes(value:JSONObject)=value.toString().toByteArray(Charsets.UTF_8).size
    fun envelope(worker:String,records:List<JSONObject>)=JSONObject().put("schemaVersion",1)
        .put("workerId",worker).put("attempts",JSONArray(records))
    fun batches(worker:String,records:List<JSONObject>):List<JSONObject> {
        require(UUID.fromString(worker).toString()==worker.lowercase()){ "Invalid local profile ID." }
        val result=mutableListOf<JSONObject>()
        val current=mutableListOf<JSONObject>()
        val seen=mutableSetOf<String>()
        for(raw in records){
            val record=JSONObject(raw.toString())
            require(record.getString("workerId")==worker){ "A record belongs to another profile." }
            require(seen.add(record.getString("id"))){ "Duplicate practice identity." }
            require(bytes(record)<=MAX_RECORD_BYTES){ "A practice record exceeds the upload size limit." }
            if(current.size==MAX_ATTEMPTS||bytes(envelope(worker,current+record))>MAX_BODY_BYTES){
                require(current.isNotEmpty())
                result.add(envelope(worker,current.toList()));current.clear()
            }
            current.add(record)
            require(bytes(envelope(worker,current))<=MAX_BODY_BYTES)
        }
        if(current.isNotEmpty())result.add(envelope(worker,current.toList()))
        return result
    }
    fun verifyReply(reply:JSONObject,batch:JSONObject){
        require(reply.getInt("schemaVersion")==1&&reply.getString("evidenceType")=="formative-decision-practice"&&reply.has("certifiable")&&!reply.getBoolean("certifiable")){ "The service returned an unexpected upload receipt." }
        val expected=batch.getJSONArray("attempts").objects().associateBy{it.getString("id")}
        val accepted=reply.getJSONArray("accepted").objects()
        require(accepted.size==expected.size&&accepted.map{it.getString("id")}.toSet()==expected.keys){ "The service did not confirm the complete batch." }
        for(item in accepted){
            require(item.getString("digest").matches(Regex("[a-f0-9]{64}"))&&item.getLong("updatedAt")==expected.getValue(item.getString("id")).getLong("updatedAt")){ "The service returned an unexpected practice receipt." }
        }
    }
}

private class PracticeSyncSheet(private val host:Activity,private val store:Store){
    private val api=AdminClient(host.applicationContext)
    private val identityStamp=api.identityStamp()
    private val origin=api.base
    private val worker=store.workerId
    private val profileName=store.name
    private val hindi=store.hi
    private val handler=Handler(Looper.getMainLooper())
    private val executor=Executors.newSingleThreadExecutor()
    private var pending:Future<*>?=null
    @Volatile private var closed=false
    @Volatile private var serial=0
    @Volatile private var stopRequested=false
    private var sending=false
    private var accountId=""
    private var accountEmail=""
    private var plan:List<JSONObject> = emptyList()
    private var total=0
    private var incompatible=0
    private var invalid=0
    private var reflectionCount=0
    private var eventCount=0
    private var batchIndex=0
    private var confirmed=0
    private val body=host.column(12)
    private val panel=host.paged(body,hindi)
    private lateinit var dialog:AlertDialog
    private fun t(en:String,hi:String)=if(hindi)hi else en
    private fun current()=!closed&&store.isCurrentProfile()&&store.workerId==worker&&api.identityStamp()==identityStamp
    private fun alive()=current()&&!host.isFinishing&&!host.isDestroyed
    private fun reset(title:String){body.removeAllViews();panel.firstPage();body.add(host.label(title,17f,Palette.blue,true).asHeading(),bottom=12)}
    private fun text(en:String,hi:String,muted:Boolean=false){body.add(host.label(t(en,hi),if(muted)13f else 15f,if(muted)Palette.muted else Palette.ink),bottom=10)}
    fun show():AlertDialog {
        dialog=PageDialogBuilder(host).setTitle(t("Private practice sync","निजी अभ्यास सिंक")).setView(panel)
            .setNegativeButton(t("Close","बंद करें"),null).create()
        dialog.setOnDismissListener{closed=true;serial++;pending?.cancel(true);executor.shutdownNow();handler.removeCallbacksAndMessages(null)}
        dialog.show();load();return dialog
    }
    private fun accountButton(){body.add(host.action(t("Open Account","खाता खोलें"),false){dialog.dismiss();host.startActivity(Intent(host,LearnerCentreActivity::class.java))},top=8)}
    private fun load(){
        if(!alive()){dialog.dismiss();return}
        if(!api.hasSession()){
            reset(t("Sign in to save a private copy","निजी प्रति सहेजने के लिए साइन इन करें"))
            text("Your Fire and First Aid practice stays on this phone. A Suraksha account is needed only for this optional upload; centre enrolment is not required.","आग और प्राथमिक सहायता का अभ्यास इस फ़ोन पर रहता है। इस वैकल्पिक अपलोड के लिए सुरक्षा खाता चाहिए; केंद्र में नामांकन ज़रूरी नहीं है।")
            accountButton();return
        }
        val token=++serial
        reset(t("Preparing a private preview…","निजी विवरण तैयार हो रहा है…"))
        text("Checking your signed-in account and reading this profile’s local practice. No practice records are being uploaded.","साइन इन खाता जाँचा जा रहा है और इस प्रोफ़ाइल का स्थानीय अभ्यास पढ़ा जा रहा है। अभ्यास रिकॉर्ड अपलोड नहीं हो रहे हैं।")
        pending=executor.submit{
            try{
                check(current())
                val user=readAccount()
                check(current())
                val catalogue=LearningCatalog(host.applicationContext)
                val records=LearningProgress(host.applicationContext,worker).use{it.records()}
                val eligible=mutableListOf<JSONObject>()
                var old=0;var rejected=0;var reflections=0;var events=0
                for(record in records){
                    val version=record.optString("contentVersion")
                    val scenario=catalogue.scenarioForVersion(record.optString("scenarioId"),version)
                    if(scenario==null){old++;continue}
                    val valid=runCatching{
                        LearningAttempt.restore(record,scenario,version,worker)
                        require(record.getString("reviewStatus")=="draft")
                        require(LearningPracticeUploadPlan.bytes(record)<=LearningPracticeUploadPlan.MAX_RECORD_BYTES)
                        val answers=record.getJSONArray("answers").objects();require(answers.size<=1000)
                        require(answers.filter{it.optString("kind")=="teach-back"}.all{it.getString("text").length<=1000})
                    }.isSuccess
                    if(!valid){rejected++;continue}
                    eligible.add(record)
                    events+=record.getJSONArray("answers").length()
                    reflections+=record.getJSONArray("answers").objects().count{it.optString("kind")=="teach-back"}
                }
                val batches=LearningPracticeUploadPlan.batches(worker,eligible)
                handler.post{
                    if(closed||token!=serial)return@post
                    if(!alive()){dialog.dismiss();return@post}
                    accountId=user.getString("id");accountEmail=user.getString("email")
                    total=records.size;incompatible=old;invalid=rejected;reflectionCount=reflections;eventCount=events
                    plan=batches;batchIndex=0;confirmed=0;renderPreview()
                }
            }catch(error:Exception){handler.post{
                if(closed||token!=serial)return@post
                if(!alive()){if(!showExpiredSession(error))dialog.dismiss();return@post}
                reset(t("Preview unavailable","विवरण उपलब्ध नहीं"))
                text("No upload was started. Your local practice is unchanged.","अपलोड शुरू नहीं हुआ। आपका स्थानीय अभ्यास नहीं बदला है।")
                errorText(error)
                body.add(host.action(t("Try preview again","विवरण फिर देखें")){load()});accountButton()
            }}
        }
    }
    private fun readAccount():JSONObject {
        val user=api.call("/api/auth/get-session",expectedIdentity=identityStamp).optJSONObject("user")?:throw AdminClient.SignedOut()
        require(user.optString("id").isNotBlank()&&user.optString("email").isNotBlank()){ "The service did not identify the signed-in account." }
        return user
    }
    private fun plannedCount()=plan.sumOf{it.getJSONArray("attempts").length()}
    private fun renderPreview(){
        reset(t("Review this upload","अपलोड का विवरण देखें"))
        body.add(host.label(accountEmail,16f,Palette.ink,true),bottom=6)
        body.add(host.label(api.base,13f,Palette.muted).apply{setTextIsSelectable(true)},bottom=12)
        text("Profile: ${profileName.ifBlank{"Learner"}} · ${worker.take(8)}","प्रोफ़ाइल: ${profileName.ifBlank{"शिक्षार्थी"}} · ${worker.take(8)}")
        text("${plannedCount()} of $total local practice records · $eventCount events · $reflectionCount written reflections","$total स्थानीय अभ्यास रिकॉर्ड में से ${plannedCount()} · $eventCount घटनाएँ · $reflectionCount लिखित चिंतन")
        if(incompatible>0)text("$incompatible records use an unavailable content version and will not be uploaded.","$incompatible रिकॉर्ड का पाठ संस्करण उपलब्ध नहीं है। वे फ़ोन पर रहेंगे और अपलोड नहीं होंगे।")
        if(invalid>0)text("$invalid records could not pass the local integrity or size checks. They remain untouched and are excluded.","$invalid रिकॉर्ड स्थानीय अखंडता या आकार की जाँच पास नहीं कर सके। उन्हें बदले बिना अपलोड से बाहर रखा गया है।")
        if(plan.isEmpty()){
            text("There are no compatible records to upload. Continue learning offline, then open this screen again.","अपलोड करने योग्य रिकॉर्ड नहीं हैं। ऑफ़लाइन सीखना जारी रखें, फिर यह स्क्रीन खोलें।")
        }else{
            text("The preview includes completed and unfinished Fire / First Aid practice. Already accepted identical snapshots can be sent again safely.","इसमें आग / प्राथमिक सहायता का पूरा और अधूरा अभ्यास शामिल है। पहले स्वीकार हुई समान प्रतियाँ फिर भेजना सुरक्षित है।",true)
            body.add(host.action(t("Review data & continue","डेटा देखें और आगे बढ़ें")){renderConsent()},top=6)
        }
        body.add(host.action(t("Read saved private copies","सहेजी निजी प्रतियाँ पढ़ें"),false,ActionRole.NEUTRAL){dialog.dismiss();SavedPracticeDialog.show(host,store)},top=10)
        body.add(host.action(t("Refresh preview","विवरण ताज़ा करें"),false,ActionRole.NEUTRAL){load()},top=10)
    }
    private fun renderConsent(){
        if(!alive()){dialog.dismiss();return}
        reset(t("Keep a copy in this account","इस खाते में एक प्रति रखें"))
        body.add(host.label(accountEmail,16f,Palette.ink,true),bottom=6)
        text("Send ${plannedCount()-confirmed} remaining records to ${api.base}. This shares profile and attempt IDs, course/version, timestamps, choices including mistakes, hints, exploration events, and any optional written teach-back reflections.","बाकी ${plannedCount()-confirmed} रिकॉर्ड ${api.base} को भेजें। इनमें प्रोफ़ाइल और प्रयास के ID, पाठ/संस्करण, समय, गलतियों सहित विकल्प, संकेत, अन्वेषण और वैकल्पिक लिखित चिंतन शामिल हैं।")
        text("They stay private to this account. They are not sent to an AI coach or automatically shared with an institution. Practice sync does not certify clinical or practical skills.","ये इस खाते तक निजी रहेंगे। इन्हें AI कोच को नहीं भेजा जाता और संस्था से अपने-आप साझा नहीं किया जाता। यह सिंक चिकित्सकीय या व्यावहारिक कौशल का प्रमाणन नहीं है।")
        text("The first upload binds this local profile to this account; reassignment is unavailable. Server snapshots are retained until account deletion under the account policy. Individual snapshot deletion and restore are not available here; backup retention is separate.","पहला अपलोड इस स्थानीय प्रोफ़ाइल को इस खाते से जोड़ता है; इसे दूसरे खाते को देना उपलब्ध नहीं है। खाता नीति के अनुसार खाता मिटने तक सर्वर पर प्रतियाँ रहती हैं। यहाँ अलग प्रति मिटाना या वापस लाना उपलब्ध नहीं है; बैकअप की अवधि अलग है।",true)
        val consent=CheckBox(host).apply{
            text=t("Upload this profile’s practice and reflections to this account","इस प्रोफ़ाइल का अभ्यास और चिंतन इस खाते में अपलोड करें")
            isChecked=false;minHeight=host.dp(48);setTextColor(Palette.ink)
        }
        body.add(consent,bottom=8)
        val send=host.action(t("Upload private copy","निजी प्रति अपलोड करें")){
            if(!consent.isChecked)return@action
            stopRequested=false;sendNext()
        }.apply{isEnabled=false}
        consent.setOnCheckedChangeListener{_,checked->send.isEnabled=checked}
        body.add(send)
        body.add(host.action(t("Back to preview","विवरण पर वापस जाएँ"),false,ActionRole.NEUTRAL){renderPreview()},top=8)
    }
    private fun renderSending(){
        reset(if(stopRequested)t("Stopping after this request…","इस अनुरोध के बाद रुक रहा है…")else t("Uploading private practice…","निजी अभ्यास अपलोड हो रहा है…"))
        body.add(host.label(t("$confirmed of ${plannedCount()} records confirmed","${plannedCount()} में से $confirmed रिकॉर्ड की पुष्टि हुई"),18f,Palette.ink,true).apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE},bottom=10)
        text("Batch ${batchIndex+1} of ${plan.size}. Your local records stay unchanged.","${plan.size} में से समूह ${batchIndex+1}। स्थानीय रिकॉर्ड नहीं बदलते।")
        text("Stopping or closing prevents further requests. A request already sent may still finish on the server.","रोकने या बंद करने से आगे के अनुरोध रुकेंगे। पहले से भेजा अनुरोध सर्वर पर पूरा हो सकता है।",true)
        body.add(host.action(t("Stop after current request","इस अनुरोध के बाद रोकें"),false,ActionRole.NEUTRAL){stopRequested=true;renderSending()}.apply{isEnabled=!stopRequested})
    }
    private fun sendNext(){
        if(sending)return
        if(!alive()){dialog.dismiss();return}
        if(batchIndex>=plan.size){renderDone();return}
        if(stopRequested){renderStopped();return}
        if(!api.hasSession()){renderError(AdminClient.SignedOut(),false);return}
        sending=true;renderSending()
        val token=++serial
        val batch=plan[batchIndex]
        pending=executor.submit{
            var posted=false
            try{
                check(current())
                // Recheck the actual account as well as the local generation before every write.
                val user=readAccount()
                check(current())
                require(user.getString("id")==accountId){ "The signed-in account changed. Reopen sync and review the account." }
                if(stopRequested){
                    handler.post{
                        if(closed||token!=serial)return@post
                        sending=false
                        if(alive())renderStopped()else dialog.dismiss()
                    }
                    return@submit
                }
                if(Thread.currentThread().isInterrupted)throw InterruptedException()
                check(current())
                posted=true
                val reply=api.call("/api/learning-practice",batch,expectedIdentity=identityStamp)
                LearningPracticeUploadPlan.verifyReply(reply,batch)
                handler.post{
                    if(closed||token!=serial)return@post
                    sending=false
                    if(!alive()){dialog.dismiss();return@post}
                    confirmed+=batch.getJSONArray("attempts").length();batchIndex++
                    if(batchIndex>=plan.size)renderDone()else if(stopRequested)renderStopped()else sendNext()
                }
            }catch(error:Exception){handler.post{
                if(closed||token!=serial)return@post
                sending=false
                if(!alive()){
                    // A 401 changes the generation; show only a generic expiry, never old account data.
                    if(!showExpiredSession(error))dialog.dismiss();return@post
                }
                renderError(error,posted)
            }}
        }
    }
    private fun renderDone(){
        reset(t("Private copy confirmed","निजी प्रति की पुष्टि हुई"))
        text("The service confirmed $confirmed practice snapshots in $accountEmail. Some may have already existed; this is not a count of newly created records.","सेवा ने $accountEmail में $confirmed अभ्यास प्रतियों की पुष्टि की। कुछ पहले से मौजूद हो सकती हैं; यह नए रिकॉर्ड की संख्या नहीं है।")
        text("Your local records are unchanged. Further practice stays on this phone until you choose to upload again. No certificate is issued by this sync.","स्थानीय रिकॉर्ड नहीं बदले। आगे का अभ्यास आपके फिर अपलोड करने तक इस फ़ोन पर रहेगा। इस सिंक से प्रमाणपत्र जारी नहीं होता।")
        if(incompatible+invalid>0)text("${incompatible+invalid} excluded local records were not uploaded.","बाहर रखे गए ${incompatible+invalid} स्थानीय रिकॉर्ड अपलोड नहीं हुए।")
        body.add(host.action(t("Done","पूरा हुआ")){dialog.dismiss()})
    }
    private fun renderStopped(){
        reset(t("Upload stopped","अपलोड रोका गया"))
        text("$confirmed of ${plannedCount()} records were confirmed. ${plannedCount()-confirmed} remain in this preview. Your local practice is unchanged.","${plannedCount()} में से $confirmed रिकॉर्ड की पुष्टि हुई। इस विवरण में ${plannedCount()-confirmed} बाकी हैं। स्थानीय अभ्यास नहीं बदला।")
        body.add(host.action(t("Review remaining upload","बाकी अपलोड का विवरण देखें")){renderConsent()})
    }
    private fun renderError(error:Exception,requestSent:Boolean){
        reset(t("Upload needs attention","अपलोड पर ध्यान दें"))
        text("$confirmed records were confirmed before this problem. Your local history is unchanged.","समस्या से पहले $confirmed रिकॉर्ड की पुष्टि हुई। स्थानीय इतिहास नहीं बदला।")
        errorText(error)
        if(requestSent)text("This batch is not confirmed. If a response was lost, the service may already have it; retrying the identical snapshot is safe. A history conflict must be reviewed, not overwritten.","इस समूह की पुष्टि नहीं हुई। उत्तर खोने पर सेवा को यह पहले मिल चुका हो सकता है; वही प्रति फिर भेजना सुरक्षित है। इतिहास का टकराव होने पर समीक्षा चाहिए, उसे बदला नहीं जाएगा।",true)
        else text("This retry did not send the batch. A previous unconfirmed request may still have been accepted.","इस कोशिश में समूह नहीं भेजा गया। पिछला अपुष्ट अनुरोध स्वीकार हुआ हो सकता है।",true)
        if(error is AdminClient.SignedOut)accountButton()
        else body.add(host.action(t("Review before retrying","फिर कोशिश से पहले विवरण देखें")){renderConsent()},top=8)
        text("For a clock error, correct the phone’s date and time. For a limit, wait or review account storage. For a conflict, keep both histories and contact support; this screen cannot merge them.","समय की त्रुटि पर फ़ोन की तारीख और समय ठीक करें। सीमा आने पर प्रतीक्षा करें या खाते का भंडारण देखें। टकराव होने पर दोनों इतिहास रखें और सहायता लें; यह स्क्रीन उन्हें मिला नहीं सकती।",true)
    }
    private fun errorText(error:Exception){
        val message=error.message?.take(400)?.takeIf{it.isNotBlank()}?:t("Check the connection and account service, then try again.","कनेक्शन और खाता सेवा जाँचें, फिर कोशिश करें।")
        body.add(host.label(message,14f,Palette.danger).apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE},bottom=12)
    }
    private fun showExpiredSession(error:Exception):Boolean{
        if(error !is AdminClient.SignedOut||closed||host.isFinishing||host.isDestroyed||!store.isCurrentProfile()||api.base!=origin||api.hasSession())return false
        reset(t("Your account session ended","आपका खाता सत्र समाप्त हो गया"))
        text("No further upload requests will be sent. Your local practice is unchanged. Previously sent requests may have been accepted; after signing in, review a fresh preview before uploading again.","आगे अपलोड अनुरोध नहीं भेजे जाएँगे। स्थानीय अभ्यास नहीं बदला। पहले भेजे अनुरोध स्वीकार हुए हो सकते हैं; साइन इन के बाद दोबारा अपलोड से पहले नया विवरण देखें।")
        accountButton();return true
    }
}
