package com.narvyn.suraksha

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject

/** Learner-only enrolment and records; accounts never acquire a staff role through this screen. */
class LearnerCentreActivity:Activity(){
    private lateinit var identity:Store
    private lateinit var api:AdminClient
    private lateinit var body:LinearLayout
    private var busy=false
    private var generation=0
    private val jobs=java.util.concurrent.Executors.newSingleThreadExecutor()
    private var signup=false
    private var userId=""
    private var owner=""
    private var centreName=""
    private var links=listOf<JSONObject>()
    private var challenge=false
    private var invitation=""
    private fun t(en:String,hi:String)=if(identity.hi)hi else en
    override fun onCreate(state:Bundle?){super.onCreate(state);identity=Store(this);api=AdminClient(this);load()}
    override fun onResume(){super.onResume();if(::identity.isInitialized&&!identity.isCurrentProfile())finish()}
    override fun onDestroy(){generation++;jobs.shutdownNow();identity.close();super.onDestroy()}
    private fun screen(title:String){val root=column(16).apply{setBackgroundColor(Palette.canvas)};root.setOnApplyWindowInsetsListener{v,i->if(android.os.Build.VERSION.SDK_INT>=30){val b=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(dp(16)+b.left,b.top,dp(16)+b.right,b.bottom)}else v.setPadding(dp(16)+i.systemWindowInsetLeft,i.systemWindowInsetTop,dp(16)+i.systemWindowInsetRight,i.systemWindowInsetBottom);i};root.add(label(title,24f,bold=true).asHeading(),top=16,bottom=12);body=column();root.addView(paged(body,identity.hi),LinearLayout.LayoutParams(-1,0,1f));root.add(action(t("Back to offline learning","ऑफ़लाइन सीखने पर लौटें"),false){finish()},top=12,bottom=12);setContentView(root)}
    private fun input(title:String,type:Int=InputType.TYPE_CLASS_TEXT):EditText{val v=EditText(this).apply{inputType=type;minHeight=dp(52);setSingleLine();setTextColor(Palette.ink);setAutofillHints(if(type and InputType.TYPE_TEXT_VARIATION_PASSWORD!=0)android.view.View.AUTOFILL_HINT_PASSWORD else android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS)};val label=label(title,14f,bold=true);v.id=android.view.View.generateViewId();label.labelFor=v.id;body.add(label,top=12);body.add(v);return v}
    private fun task(work:()->JSONObject,identityChange:Boolean=false,done:(JSONObject)->Unit){
        if(busy)return;busy=true;val token=++generation;val before=api.identityStamp()
        jobs.execute{try{val result=api.forIdentity(before){work()};val replyIdentity=api.completedIdentityStamp();runOnUiThread{
            if(token!=generation||isFinishing||isDestroyed||!identity.isCurrentProfile())return@runOnUiThread
            busy=false;if(api.identityStamp()!=replyIdentity||!identityChange&&before!=replyIdentity){login();return@runOnUiThread};done(result)
        }}catch(e:Exception){runOnUiThread{
            if(token!=generation||isFinishing||isDestroyed||!identity.isCurrentProfile())return@runOnUiThread
            busy=false;if(e is AdminClient.SignedOut||e is AdminClient.SessionChanged||!identityChange&&before!=api.identityStamp())login()
            notice(t("Could not complete","पूरा नहीं हो सका"),e.message?:"Try again")
        }}}
    }
    private fun load(){if(!api.hasSession()){login();return};screen(t("Connecting to your centre…","केंद्र से जुड़ रहे हैं…"));task({api.call("/api/learner")}){r->userId=r.getJSONObject("user").getString("userId");links=r.getJSONArray("links").objects().filter{it.getString("worker_id")==identity.workerId};val selected=links.find{it.getString("owner")==owner}?:links.singleOrNull();if(selected!=null)selectCentre(selected)else if(links.isEmpty())join()else chooseCentre()}}
    private fun selectCentre(link:JSONObject){owner=link.getString("owner");centreName=link.getString("centre");showRecord()}
    private fun chooseCentre(){screen(t("Choose institution","संस्था चुनें"));links.forEach{link->body.add(action(link.getString("centre")){selectCentre(link)},bottom=10)};body.add(action(t("Join another institution","दूसरी संस्था से जुड़ें"),false){join()},top=8);logout()}
    private fun login(){screen(t(if(signup)"Create learner account"else"Learner sign in",if(signup)"शिक्षार्थी खाता बनाएँ"else"शिक्षार्थी साइन इन"));body.add(label(t("Practice works offline. Sign in to enrol, sync and follow certification.","अभ्यास ऑफ़लाइन चलता है। नामांकन, सिंक और प्रमाणपत्र के लिए प्रवेश करें।"),15f),bottom=8)
        if(challenge){val code=input(t("Authenticator or recovery code","ऑथेंटिकेटर या रिकवरी कोड"));val backup=CheckBox(this).apply{text=t("Use recovery code","रिकवरी कोड उपयोग करें")};body.add(backup);body.add(action(t("Verify sign-in","प्रवेश सत्यापित करें")){task({api.call("/api/auth/two-factor/"+if(backup.isChecked)"verify-backup-code"else"verify-totp",JSONObject().put("code",code.text.toString()))},identityChange=true){challenge=false;code.text.clear();load()}},top=16);return}
        val name=if(signup)input(t("Full name","पूरा नाम")).apply{setText(identity.name);setAutofillHints(android.view.View.AUTOFILL_HINT_NAME)}else null
        val email=input(t("Email","ईमेल"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password=input(t("Password","पासवर्ड"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        body.add(action(t("Show / hide password","पासवर्ड दिखाएँ / छिपाएँ"),false){val start=password.selectionStart;password.transformationMethod=if(password.transformationMethod==null)android.text.method.PasswordTransformationMethod.getInstance()else null;password.setSelection(start.coerceAtLeast(0))},top=6)
        val remember=CheckBox(this).apply{text=t("Keep signed in on this private phone","इस निजी फ़ोन पर प्रवेश बनाए रखें")};body.add(remember,top=8)
        body.add(action(t(if(signup)"Create account"else"Sign in",if(signup)"खाता बनाएँ"else"साइन इन")){
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()||password.length()<(if(signup)15 else 1)){notice(t("Check details","विवरण जाँचें"),t("Enter an email and password. New passwords need at least 15 characters.","ईमेल और पासवर्ड दें। नए पासवर्ड में कम से कम 15 अक्षर हों।"));return@action}
            api.remember(remember.isChecked);val payload=JSONObject().put("email",email.text.toString().trim()).put("password",password.text.toString()).put("rememberMe",remember.isChecked);if(signup)payload.put("name",name!!.text.toString().trim())
            task({api.call("/api/auth/${if(signup)"sign-up"else"sign-in"}/email",payload)},identityChange=true){r->password.text.clear();if(r.optBoolean("twoFactorRedirect")){challenge=true;login()}else load()}
        },top=16)
        body.add(action(t(if(signup)"Already registered? Sign in"else"Create account",if(signup)"खाता है? प्रवेश करें"else"खाता बनाएँ"),false){signup=!signup;login()},top=8)
        body.add(action(t("Forgot password?","पासवर्ड भूल गए?"),false){task({api.call("/api/auth/request-password-reset",JSONObject().put("email",email.text.toString().trim()))}){notice(t("Recovery requested","रिकवरी अनुरोध"),t("If this account exists, use its configured recovery channel. Contact your institution if the link does not arrive.","खाता होने पर निर्धारित रिकवरी माध्यम जाँचें। लिंक न मिले तो संस्था से संपर्क करें।"))}},top=8)
        body.add(label(t("Account service: ${api.base}. Offline learning remains available.","खाता सेवा: ${api.base}। ऑफ़लाइन सीखना उपलब्ध है।"),13f,Palette.muted),top=12)
    }
    private fun join(){screen(t("Your learning account","आपका सीखने का खाता"));body.add(action(t("Private practice and saved copies","निजी अभ्यास और सहेजी प्रतियाँ")){LearningPracticeSyncDialog.show(this,identity)},bottom=12);body.add(label(identity.name.ifBlank{"Learner"},20f,bold=true));body.add(label(t("Share this learner ID with your trainer to register the correct profile:","सही प्रोफ़ाइल दर्ज करने के लिए यह शिक्षार्थी ID प्रशिक्षक को दें:")),top=12);body.add(label(identity.workerId,14f).apply{setTextIsSelectable(true)},top=8);body.add(label(t("No records will be merged with another learner.","दूसरे शिक्षार्थी के रिकॉर्ड मिलाए नहीं जाएँगे।"),13f,Palette.muted),top=12)
        val code=input(t("Private invitation code or link","निजी आमंत्रण कोड या लिंक")).apply{setText(invitation);importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO}
        body.add(action(t("Scan invitation QR","आमंत्रण QR स्कैन करें"),false){IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setBeepEnabled(false).initiateScan()},top=10)
        body.add(action(t("Confirm enrolment","नामांकन की पुष्टि")){val raw=code.text.toString().trim();val token=if(raw.startsWith("http"))Uri.parse("https://local/?"+Uri.parse(raw).fragment).getQueryParameter("invite")?:raw else raw;task({api.call("/api/learner",JSONObject().put("action","join").put("invitation",token).put("workerId",identity.workerId))}){invitation="";load()}},top=16);logout()
    }
    private fun showRecord(industrial:Boolean=false){screen(t("My training centre","मेरा प्रशिक्षण केंद्र"));task({api.call("/api/learner?workerId=${identity.workerId}&owner=${Uri.encode(owner)}")}){data->
        body.add(label(data.getString("centre"),20f,bold=true));body.add(label(identity.name,16f),top=8)
        body.add(action(t("Fire Safety & First Aid assignments","अग्नि सुरक्षा और प्राथमिक उपचार कार्य")){LearningCentreDialog.showLearner(this,identity,owner=owner)},top=12)
        body.add(action(t("Learning notifications","सीखने की सूचनाएँ"),false){LearningInboxDialog.show(this,identity,owner)},top=8)
        if(!industrial){
            body.add(action(t("Private saved practice","निजी सहेजा अभ्यास"),false){LearningPracticeSyncDialog.show(this,identity)},top=8)
            body.add(action(t("Industrial certification records","औद्योगिक प्रमाणन रिकॉर्ड"),false){showRecord(true)},top=8)
            if(links.size>1)body.add(action(t("Change institution","संस्था बदलें"),false){chooseCentre()},top=8)
            body.add(action(t("Join another institution","दूसरी संस्था से जुड़ें"),false){join()},top=8);logout();return@task
        }
        body.add(action(t("Back to institution","संस्था पर वापस"),false){showRecord()},top=8)
        body.add(label(t("Sync shares the industrial learning record with $centreName. Fire and First Aid practice uses separate, explicit sharing.","सिंक से औद्योगिक सीखने का रिकॉर्ड $centreName से साझा होगा। अग्नि और प्राथमिक उपचार अभ्यास अलग से स्पष्ट सहमति पर साझा होता है।"),14f,Palette.muted),top=12)
        body.add(label(t("Industrial certification records","औद्योगिक प्रमाणन रिकॉर्ड"),18f,bold=true),top=20)
        val syncLabel=label(LearningSync.status(this,identity.workerId),14f,Palette.muted);body.add(syncLabel,top=12)
        body.add(action(t("Sync industrial records","औद्योगिक रिकॉर्ड सिंक करें")){syncIndustrial(syncLabel)},top=12)
        body.add(action(t("Reset pending transfer","लंबित ट्रांसफर रीसेट करें"),false){PageDialogBuilder(this).setTitle(t("Reset transfer copy?","ट्रांसफर प्रति रीसेट करें?"))
            .setMessage(t("Your local learning remains saved. The next sync rebuilds the transfer for the selected institution. Previously shared records stay with their institution.","स्थानीय सीखना सुरक्षित रहेगा। अगला सिंक चुनी संस्था के लिए ट्रांसफर बनाएगा। पहले साझा रिकॉर्ड उसी संस्था में रहेंगे।"))
            .setNegativeButton(t("Cancel","रद्द करें"),null).setPositiveButton(t("Reset","रीसेट")){_,_->try{LearningSync.resetPending(this,identity.workerId);syncLabel.text=t("Transfer reset. Local learning retained.","ट्रांसफर रीसेट। स्थानीय सीखना सुरक्षित है।")}catch(e:Exception){syncLabel.text=e.message}}.show()},top=8)
        data.getJSONArray("courses").objects().forEach{course->val card=card();card.add(label(Curriculum(this).module(course.getString("id")).local("title",identity.hi),18f,bold=true));course.getJSONArray("steps").objects().forEach{s->card.add(label((if(s.optBoolean("done"))"✓ "else"○ ")+s.getString("label"),14f),top=8)}
            val existing=data.getJSONArray("requests").objects().any{it.optString("attempt_id")==course.optString("attemptId")}
            if(course.getJSONArray("steps").objects().all{it.optBoolean("done")}&&!existing)card.add(action(t("Request independent review","स्वतंत्र समीक्षा माँगें"),false){note(t("Evidence note for reviewer","समीक्षक के लिए प्रमाण विवरण")){value->task({api.call("/api/learner",JSONObject().put("action","request").put("workerId",identity.workerId).put("owner",owner).put("attemptId",course.getString("attemptId")).put("note",value))}){showRecord()}}},top=12)
            body.add(card,top=16)}
        data.getJSONArray("requests").objects().forEach{q->val card=card();card.add(label(t("Certification: ","प्रमाणपत्र: ")+q.getString("status"),17f,bold=true));card.add(label(q.optString("review_reason").takeUnless{it=="null"}?:q.getString("request_note")),top=8)
            if(q.getString("status")=="needs-information")card.add(action(t("Provide information","जानकारी दें")){note(t("Response to reviewer","समीक्षक को उत्तर")){value->task({api.call("/api/learner",JSONObject().put("action","clarify").put("workerId",identity.workerId).put("owner",owner).put("requestId",q.getString("id")).put("note",value))}){showRecord()}}},top=12);body.add(card,top=16)}
        body.add(label(t("Approved credentials appear in My record after sync. Practical observations and the printable certificate are available in the learner web portal.","सिंक के बाद स्वीकृत प्रमाणपत्र मेरा रिकॉर्ड में दिखेंगे। व्यावहारिक निरीक्षण और छपने योग्य प्रमाणपत्र वेब पोर्टल में हैं।"),14f,Palette.muted),top=16)
        body.add(action(t("Open learner portal","शिक्षार्थी पोर्टल खोलें"),false){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(api.base+"/learn")))},top=10);logout()
    }}
    private fun syncIndustrial(label:TextView){
        try{LearningSync.bind(this,identity.workerId,userId,owner)}catch(e:Exception){label.text=e.message;return}
        val expected=api.identityStamp();val centre=owner;label.text=t("Syncing…","सिंक हो रहा है…")
        LearningSync.schedule(this){message->runOnUiThread{if(!isFinishing&&!isDestroyed&&identity.isCurrentProfile()&&api.identityStamp()==expected&&owner==centre){label.text=message;if(message.startsWith("Synced"))showRecord(true)}}}
    }
    private fun note(title:String,save:(String)->Unit){val field=EditText(this).apply{minLines=2;hint=t("10–500 characters","10–500 अक्षर")};val d=PageDialogBuilder(this).setTitle(title).setView(field).setNegativeButton(t("Cancel","रद्द"),null).setPositiveButton(t("Submit","भेजें"),null).create();d.setOnShowListener{d.getButton(-1).setOnClickListener{val value=field.text.toString().trim();if(value.length !in 10..500)field.error=t("Use 10–500 characters","10–500 अक्षर रखें")else{d.dismiss();save(value)}}};d.show()}
    private fun logout(){body.add(action(t("Sign out of this phone","इस फ़ोन से साइन आउट"),false){task({api.call("/api/auth/sign-out",JSONObject())},identityChange=true){api.clear();login()}},top=16)}
    @Deprecated("Compatibility") override fun onActivityResult(code:Int,result:Int,data:Intent?){super.onActivityResult(code,result,data);IntentIntegrator.parseActivityResult(code,result,data)?.contents?.let{invitation=it;join()}}
}
