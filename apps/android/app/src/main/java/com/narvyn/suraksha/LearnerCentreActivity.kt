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
    companion object { const val EXTRA_DESTINATION="account_destination" }
    private var page="login"
    private var emailDraft=""
    private var nameDraft=""
    private var rememberDraft=false
    private var accountEmail=""
    private var accountName=""
    private var requestedDestination="account"
    private var destinationOpened=false
    private lateinit var status:TextView
    private val passwordFields=mutableListOf<EditText>()
    private val backNavigation by lazy { AppBackNavigation(this){goBack()} }
    override fun onCreate(state:Bundle?){
        super.onCreate(state);identity=Store(this);api=AdminClient(this)
        emailDraft=state?.getString("email").orEmpty();nameDraft=state?.getString("name")?:identity.name;rememberDraft=state?.getBoolean("remember")?:false
        signup=state?.getBoolean("signup")?:false;challenge=state?.getBoolean("challenge")?:false
        requestedDestination=intent.getStringExtra(EXTRA_DESTINATION).takeIf{it in listOf("account","assignments","saved")}?:"account"
        destinationOpened=state?.getBoolean("destinationOpened")?:false
        load()
    }
    override fun onSaveInstanceState(state:Bundle){captureDraft();state.putString("email",emailDraft);state.putString("name",nameDraft);state.putBoolean("remember",rememberDraft);state.putBoolean("signup",signup);state.putBoolean("challenge",challenge);state.putBoolean("destinationOpened",destinationOpened);super.onSaveInstanceState(state)}
    override fun onResume(){super.onResume();if(::identity.isInitialized&&!identity.isCurrentProfile())finish()}
    override fun onPause(){passwordFields.forEach{it.transformationMethod=android.text.method.PasswordTransformationMethod.getInstance();(it.parent as? android.view.ViewGroup)?.findViewWithTag<Button>("password-toggle")?.let{toggle->toggle.text=t("Show password","पासवर्ड दिखाएँ");toggle.contentDescription=toggle.text}};super.onPause()}
    override fun onDestroy(){backNavigation.close();generation++;jobs.shutdownNow();passwordFields.forEach{it.text.clear()};identity.close();super.onDestroy()}
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 back compatibility") override fun onBackPressed(){goBack()}
    private fun goBack(){captureDraft();if(popContentPage())return;if(busy){generation++;busy=false;if(page in listOf("login","signup","challenge"))api.clear()};when(page){"signup","recovery","challenge"->{if(challenge)api.clear();challenge=false;signup=false;login()};"join","security","institution","choose"->accountHome();else->finish()}}
    private fun screen(title:String,key:String="account"){
        page=key;passwordFields.clear()
        val root=column(16).apply{setBackgroundColor(Palette.canvas)}
        root.setOnApplyWindowInsetsListener{v,i->if(android.os.Build.VERSION.SDK_INT>=30){val b=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(dp(16)+b.left,b.top,dp(16)+b.right,b.bottom)}else v.setPadding(dp(16)+i.systemWindowInsetLeft,i.systemWindowInsetTop,dp(16)+i.systemWindowInsetRight,i.systemWindowInsetBottom);i}
        root.add(label(title,24f,bold=true).asHeading(),top=16,bottom=8)
        status=label("",14f,Palette.muted).apply{visibility=android.view.View.GONE;accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE;tag="account-status"};root.add(status,bottom=6)
        body=column();root.addView(paged(body,identity.hi),LinearLayout.LayoutParams(-1,0,1f))
        root.add(action(t("Continue offline learning","ऑफ़लाइन सीखना जारी रखें"),false){finish()},top=10,bottom=10)
        setContentView(root);backNavigation.enabled(true)
    }
    private fun input(title:String,type:Int=InputType.TYPE_CLASS_TEXT,key:String="",newPassword:Boolean=false):EditText{
        val group=column();body.add(group,top=10)
        val v=EditText(this).apply{
            id=when(key){"email"->R.id.account_email;"name"->R.id.account_name;"password"->R.id.account_password;"current-password"->R.id.account_current_password;"new-password"->R.id.account_new_password;else->android.view.View.generateViewId()}
            tag="account-$key";contentDescription=title;inputType=type;minHeight=dp(48);setSingleLine();textSize=16f;setTextColor(Palette.ink)
            background=softSurface(Palette.surface,12,interactive=true,inset=true);setPadding(dp(12),dp(8),dp(12),dp(8));isSaveEnabled=false
            imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
        val caption=label(title,14f,Palette.muted,true).apply{labelFor=v.id};group.add(caption,bottom=4);group.add(v)
        when(type and InputType.TYPE_MASK_VARIATION){
            InputType.TYPE_TEXT_VARIATION_PASSWORD->{
                passwordFields.add(v);v.setAutofillHints(if(newPassword)"newPassword"else android.view.View.AUTOFILL_HINT_PASSWORD)
                v.transformationMethod=android.text.method.PasswordTransformationMethod.getInstance()
                lateinit var toggle:Button
                toggle=action(t("Show password","पासवर्ड दिखाएँ"),false){val start=v.selectionStart;val end=v.selectionEnd;val hidden=v.transformationMethod!=null;v.transformationMethod=if(hidden)null else android.text.method.PasswordTransformationMethod.getInstance();toggle.text=t(if(hidden)"Hide password"else"Show password",if(hidden)"पासवर्ड छिपाएँ"else"पासवर्ड दिखाएँ");toggle.contentDescription=toggle.text;v.requestFocus();if(start>=0&&end>=0)v.setSelection(start.coerceAtMost(v.length()),end.coerceAtMost(v.length()))}
                toggle.tag="password-toggle";toggle.contentDescription=toggle.text;group.add(toggle,top=4)
            }
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS->v.setAutofillHints(android.view.View.AUTOFILL_HINT_USERNAME,android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS)
            else->if(key=="name")v.setAutofillHints(android.view.View.AUTOFILL_HINT_NAME)else v.importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO
        }
        return v
    }
    private fun captureDraft(){if(!::body.isInitialized)return;body.findViewById<EditText>(R.id.account_email)?.let{emailDraft=it.text.toString()};body.findViewById<EditText>(R.id.account_name)?.let{nameDraft=it.text.toString()};(body.findViewWithTag<android.view.View>("account-remember") as? CheckBox)?.let{rememberDraft=it.isChecked}}
    private fun enable(view:android.view.View,value:Boolean){view.isEnabled=value;if(view is android.view.ViewGroup)for(i in 0 until view.childCount)enable(view.getChildAt(i),value)}
    private fun message(value:String){status.text=value;status.visibility=android.view.View.VISIBLE}
    private fun submitOnKeyboard(field:EditText,action:()->Unit){field.imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE;field.setOnEditorActionListener{_,id,event->if(id==android.view.inputmethod.EditorInfo.IME_ACTION_DONE||event?.keyCode==android.view.KeyEvent.KEYCODE_ENTER&&event.action==android.view.KeyEvent.ACTION_UP){action();true}else false}}
    private fun validEmail(field:EditText):Boolean{if(!android.util.Patterns.EMAIL_ADDRESS.matcher(field.text.toString().trim()).matches()){field.error=t("Enter a valid email address","सही ईमेल पता दर्ज करें");field.requestFocus();return false};return true}
    private fun validPassword(field:EditText,new:Boolean):Boolean{val minimum=if(new)15 else 1;if(field.length() !in minimum..128){field.error=t(if(new)"Use 15–128 characters; spaces are allowed"else"Enter your password (up to 128 characters)",if(new)"15–128 अक्षर रखें; स्पेस रख सकते हैं"else"पासवर्ड दर्ज करें (अधिकतम 128 अक्षर)");field.requestFocus();return false};return true}
    private fun task(work:()->JSONObject,identityChange:Boolean=false,done:(JSONObject)->Unit){
        if(busy)return;busy=true;val token=++generation;val before=api.identityStamp();enable(body,false);message(t("Connecting…","कनेक्ट हो रहा है…"))
        (getSystemService(INPUT_METHOD_SERVICE)as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(body.windowToken,0)
        jobs.execute{try{val result=api.forIdentity(before){work()};val replyIdentity=api.completedIdentityStamp();runOnUiThread{
            if(token!=generation||isFinishing||isDestroyed||!identity.isCurrentProfile())return@runOnUiThread
            busy=false;enable(body,true);status.visibility=android.view.View.GONE
            if(api.identityStamp()!=replyIdentity||!identityChange&&before!=replyIdentity){login();return@runOnUiThread};done(result)
        }}catch(e:Exception){runOnUiThread{
            if(token!=generation||isFinishing||isDestroyed||!identity.isCurrentProfile())return@runOnUiThread
            busy=false;enable(body,true)
            if(e is AdminClient.SignedOut||e is AdminClient.SessionChanged||!identityChange&&before!=api.identityStamp()){challenge=false;login()}
            message(when(e){is java.io.IOException->t("Cannot reach the account service. Check your connection or Connection settings. Offline learning is still available.","खाता सेवा से संपर्क नहीं हुआ। कनेक्शन या कनेक्शन सेटिंग जाँचें। ऑफ़लाइन सीखना उपलब्ध है।");else->e.message?:t("Could not complete this action. Try again.","क्रिया पूरी नहीं हुई। फिर प्रयास करें।")})
        }}}
    }
    private fun load(){if(!api.hasSession()){login();return};screen(t("Suraksha account","सुरक्षा खाता"));body.add(action(t("Try again","फिर प्रयास करें"),false){load()},top=12);body.add(action(t("Connection settings","कनेक्शन सेटिंग"),false){server()},top=8);task({api.call("/api/learner")}){r->
        val user=r.getJSONObject("user");userId=user.getString("userId");accountEmail=user.optString("email");accountName=user.optString("displayName")
        links=r.getJSONArray("links").objects().filter{it.getString("worker_id")==identity.workerId};accountHome()
        if(!destinationOpened){destinationOpened=true;when(requestedDestination){"assignments"->LearningCentreDialog.showLearner(this,identity);"saved"->SavedPracticeDialog.show(this,identity)}}
    }}
    private fun accountHome(){screen(t("Suraksha account","सुरक्षा खाता"));body.add(label(accountName.ifBlank{t("Signed in","प्रवेश हो गया")},20f,bold=true),bottom=4);body.add(label(accountEmail,14f,Palette.muted),bottom=12)
        body.add(label(t("This phone: ${identity.name.ifBlank{"Learner"}}. Practice stays private until you choose to upload or share it.","यह फ़ोन: ${identity.name.ifBlank{"शिक्षार्थी"}}। अपलोड या साझा करने तक अभ्यास निजी रहता है।"),14f),bottom=12)
        body.add(action(t("My role dashboard","मेरी भूमिका का डैशबोर्ड"),false){startActivity(Intent(this,RoleDashboardActivity::class.java))},bottom=8)
        body.add(action(t("Private practice & sync","निजी अभ्यास और सिंक")){LearningPracticeSyncDialog.show(this,identity)},bottom=8)
        body.add(action(t("Institution assignments","संस्था के अभ्यास कार्य"),false){LearningCentreDialog.showLearner(this,identity)},bottom=8)
        body.add(action(t("Institution-issued records","संस्था द्वारा जारी रिकॉर्ड"),false){InstitutionRecordsDialog.show(this,identity)},bottom=8)
        body.add(action(t("Reviewed local procedures","समीक्षित स्थानीय प्रक्रियाएँ"),false){InstitutionRecordsDialog.library(this,identity)},bottom=8)
        body.add(action(t("Join or manage institution","संस्था जोड़ें या प्रबंधित करें"),false){if(links.isEmpty())join()else chooseCentre()},bottom=8)
        body.add(action(t("Account security","खाता सुरक्षा"),false){security()},bottom=8)
        body.add(action(t("Assigned identity practice (browser)","पहचान सहित निर्धारित अभ्यास (ब्राउज़र)"),false){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(api.base+"/identity")))},top=10);logout()
    }
    private fun selectCentre(link:JSONObject){owner=link.getString("owner");centreName=link.getString("centre");showRecord()}
    private fun chooseCentre(){screen(t("Choose institution","संस्था चुनें"),"choose");links.forEach{link->body.add(action(link.getString("centre")){selectCentre(link)},bottom=10)};body.add(action(t("Join another institution","दूसरी संस्था से जुड़ें"),false){join()},top=8)}
    private fun login(){screen(t(if(challenge)"Confirm sign-in"else if(signup)"Create Suraksha account"else"Sign in to Suraksha",if(challenge)"प्रवेश की पुष्टि"else if(signup)"सुरक्षा खाता बनाएँ"else"सुरक्षा में साइन इन"),if(challenge)"challenge"else if(signup)"signup"else"login")
        if(challenge){val code=input(t("Authenticator or recovery code","ऑथेंटिकेटर या रिकवरी कोड"),key="code");val backup=CheckBox(this).apply{text=t("Use recovery code","रिकवरी कोड उपयोग करें")};body.add(backup)
            val verify={if(code.text.isBlank())code.error=t("Enter your code","कोड दर्ज करें")else task({api.call("/api/auth/two-factor/"+(if(backup.isChecked)"verify-backup-code"else"verify-totp"),JSONObject().put("code",code.text.toString().trim()))},identityChange=true){challenge=false;code.text.clear();startActivity(Intent(this,RoleDashboardActivity::class.java));finish()}}
            body.add(action(t("Verify sign-in","प्रवेश सत्यापित करें")){verify()},top=12);submitOnKeyboard(code){verify()};body.add(action(t("Back to sign in","साइन इन पर वापस"),false){api.clear();challenge=false;login()},top=8);return
        }
        body.add(label(t("Save private practice and connect to your institution.","निजी अभ्यास सहेजें और अपनी संस्था से जुड़ें।"),15f,Palette.muted),bottom=6)
        val name=if(signup)input(t("Full name","पूरा नाम"),key="name").apply{setText(nameDraft)}else null
        val email=input(t("Email address","ईमेल पता"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,"email").apply{setText(emailDraft)}
        val password=input(t(if(signup)"Password · 15–128 characters"else"Password",if(signup)"पासवर्ड · 15–128 अक्षर"else"पासवर्ड"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,"password",signup)
        val remember=CheckBox(this).apply{tag="account-remember";text=t("Keep signed in on this private phone","इस निजी फ़ोन पर प्रवेश बनाए रखें");isChecked=rememberDraft};body.add(remember,top=6)
        val submit={
            captureDraft()
            if(api.base.isBlank())server()
            else if(validEmail(email)&&validPassword(password,signup)){
                if(signup&&name!!.text.toString().trim().length !in 1..100){name.error=t("Use 1–100 characters","1–100 अक्षर रखें");name.requestFocus()}
                else{api.remember(remember.isChecked);val payload=JSONObject().put("email",email.text.toString().trim()).put("password",password.text.toString()).put("rememberMe",remember.isChecked);if(signup)payload.put("name",name!!.text.toString().trim())
                    task({api.call("/api/auth/${if(signup)"sign-up"else"sign-in"}/email",payload)},identityChange=true){r->password.text.clear();if(r.optBoolean("twoFactorRedirect")){challenge=true;login()}else{signup=false;startActivity(Intent(this,RoleDashboardActivity::class.java));finish()}}
                }
            }
        }
        body.add(action(t(if(signup)"Create account"else"Sign in",if(signup)"खाता बनाएँ"else"साइन इन")){submit()}.apply{tag="account-submit"},top=10);submitOnKeyboard(password){submit()}
        body.add(action(t(if(signup)"Already have an account? Sign in"else"Create account",if(signup)"खाता है? साइन इन करें"else"खाता बनाएँ"),false){captureDraft();signup=!signup;login()},top=6)
        if(!signup)body.add(action(t("Forgot password?","पासवर्ड भूल गए?"),false){captureDraft();recovery()},top=6)
        body.add(action(t("Connection settings","कनेक्शन सेटिंग"),false){captureDraft();server()},top=6)
        if(api.base.startsWith("http://"))body.add(label(t("Local preview connection. Internet accounts need a configured HTTPS service.","स्थानीय प्रीव्यू कनेक्शन। इंटरनेट खाते के लिए HTTPS सेवा चाहिए।"),12f,Palette.muted),top=8)
    }
    private fun recovery(){screen(t("Reset password","पासवर्ड रीसेट करें"),"recovery");body.add(label(t("Enter your Suraksha account email. You will receive a link if recovery is available for that account.","सुरक्षा खाते का ईमेल दर्ज करें। उस खाते के लिए रिकवरी उपलब्ध होने पर लिंक मिलेगा।"),15f,Palette.muted))
        val email=input(t("Email address","ईमेल पता"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,"email").apply{setText(emailDraft)}
        val send={captureDraft();if(validEmail(email))task({api.call("/api/auth/request-password-reset",JSONObject().put("email",email.text.toString().trim()))}){message(t("If an account matches, check your inbox for a reset link. The link expires after 15 minutes. If it does not arrive, contact your institution.","खाता मिलने पर इनबॉक्स में रीसेट लिंक देखें। लिंक 15 मिनट बाद समाप्त होगा। न मिलने पर संस्था से संपर्क करें।"))}}
        var available=true
        val sendButton=action(t("Send reset link","रीसेट लिंक भेजें")){if(available)send()};body.add(sendButton,top=12);submitOnKeyboard(email){if(available)send()}
        body.add(action(t("Back to sign in","साइन इन पर वापस"),false){captureDraft();signup=false;login()},top=8);body.add(action(t("Connection settings","कनेक्शन सेटिंग"),false){server()},top=8)
        task({api.call("/api/account-capabilities")}){reply->available=reply.optBoolean("passwordRecovery");if(!available){sendButton.visibility=android.view.View.GONE;(email.parent as android.view.View).visibility=android.view.View.GONE;message(t("Password recovery is unavailable on this service. Email recovery is not enabled. Your password has not changed.","इस सेवा पर पासवर्ड रिकवरी उपलब्ध नहीं है। ईमेल रिकवरी चालू नहीं है। आपका पासवर्ड नहीं बदला है।"))}}
    }
    private fun server(){val input=EditText(this).apply{setText(api.base);contentDescription=t("Account service address","खाता सेवा पता");hint="https://accounts.example.org";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI;isSingleLine=true;importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO}
        val dialog=PageDialogBuilder(this).setTitle(t("Account connection","खाता कनेक्शन")).setMessage(t("Use the HTTPS address supplied by your institution. Changing the service signs this phone out; offline practice is kept.","संस्था का HTTPS पता उपयोग करें। सेवा बदलने पर फ़ोन साइन आउट होगा; ऑफ़लाइन अभ्यास सुरक्षित रहेगा।")).setView(input).setNegativeButton(t("Cancel","रद्द करें"),null).setPositiveButton(t("Save","सहेजें"),null).create()
        dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{try{api.configure(input.text.toString());challenge=false;dialog.dismiss();load()}catch(e:Exception){input.error=e.message}}};dialog.show()
    }
    private fun security(){screen(t("Account security","खाता सुरक्षा"),"security");body.add(label(accountEmail,14f,Palette.muted))
        val current=input(t("Current password","वर्तमान पासवर्ड"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,"current-password")
        val next=input(t("New password · 15–128 characters","नया पासवर्ड · 15–128 अक्षर"),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,"new-password",true)
        val change={if(validPassword(current,false)&&validPassword(next,true))task({api.call("/api/auth/change-password",JSONObject().put("currentPassword",current.text.toString()).put("newPassword",next.text.toString()).put("revokeOtherSessions",true))}){current.text.clear();next.text.clear();message(t("Password updated. Other sessions are signed out.","पासवर्ड बदल गया। अन्य सत्र साइन आउट हो गए।"))}}
        body.add(action(t("Update password","पासवर्ड बदलें")){change()},top=12);submitOnKeyboard(next){change()};body.add(action(t("Connection settings","कनेक्शन सेटिंग"),false){server()},top=8);body.add(action(t("Back to account","खाते पर वापस"),false){accountHome()},top=8)
    }
    private fun join(){screen(t("Join an institution","संस्था से जुड़ें"),"join");body.add(action(t("Private practice and saved copies","निजी अभ्यास और सहेजी प्रतियाँ")){LearningPracticeSyncDialog.show(this,identity)},bottom=12);body.add(label(identity.name.ifBlank{"Learner"},20f,bold=true));body.add(label(t("Share this learner ID with your trainer to register the correct profile:","सही प्रोफ़ाइल दर्ज करने के लिए यह शिक्षार्थी ID प्रशिक्षक को दें:")),top=12);body.add(label(identity.workerId,14f).apply{setTextIsSelectable(true)},top=8);body.add(label(t("No records will be merged with another learner.","दूसरे शिक्षार्थी के रिकॉर्ड मिलाए नहीं जाएँगे।"),13f,Palette.muted),top=12)
        val code=input(t("Private invitation code or link","निजी आमंत्रण कोड या लिंक")).apply{setText(invitation);importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO}
        body.add(action(t("Scan invitation QR","आमंत्रण QR स्कैन करें"),false){IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setBeepEnabled(false).initiateScan()},top=10)
        body.add(action(t("Confirm enrolment","नामांकन की पुष्टि")){val raw=code.text.toString().trim();val token=if(raw.startsWith("http"))Uri.parse("https://local/?"+Uri.parse(raw).fragment).getQueryParameter("invite")?:raw else raw;task({api.call("/api/learner",JSONObject().put("action","join").put("invitation",token).put("workerId",identity.workerId))}){invitation="";load()}},top=16);logout()
    }
    private fun showRecord(industrial:Boolean=false){screen(t("My training centre","मेरा प्रशिक्षण केंद्र"),"institution");task({api.call("/api/learner?workerId=${identity.workerId}&owner=${Uri.encode(owner)}")}){data->
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
    private fun logout(){body.add(action(t("Sign out of this phone","इस फ़ोन से साइन आउट"),false){task({api.call("/api/auth/sign-out",JSONObject())},identityChange=true){api.clear();accountEmail="";accountName="";challenge=false;signup=false;login()}},top=12)}
    @Deprecated("Compatibility") override fun onActivityResult(code:Int,result:Int,data:Intent?){super.onActivityResult(code,result,data);IntentIntegrator.parseActivityResult(code,result,data)?.contents?.let{invitation=it;join()}}
}
