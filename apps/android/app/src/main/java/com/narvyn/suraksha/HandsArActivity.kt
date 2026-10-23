package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

/** Explicit camera/hand privacy and viewer calibration precede the live exploration workspace. */
class HandsArActivity:Activity(){
    private val back by lazy{AppBackNavigation(this){goBack()}}
    private lateinit var profile:Store
    private var profileContext=""
    private var scene:HandsArSceneView?=null
    private var headset=false
    private var space=false
    private var rearCamera=false
    private var privacy=false
    private var active=false
    private var page=1
    private var modelId="extinguisher"
    private var teachingKey:String?=null
    private var partId:String?=null
    private var entryNote:String?=null
    private var error:String?=null
    private lateinit var start:Button
    private lateinit var viewerStatus:TextView
    private var resumed=false
    private var permissionStartPending=false
    private var lesson:HandsLessonSession?=null
    private var lessonStepId:String?=null
    private lateinit var narrator:TrainingNarrator
    override fun onCreate(saved:Bundle?){
        super.onCreate(saved)
        headset=saved?.getBoolean("headset")?:intent.getBooleanExtra("headset",false)
        page=1
        space=saved?.getBoolean("space")?:false;rearCamera=saved?.getBoolean("rearCamera")?:false;privacy=saved?.getBoolean("privacy")?:false
        modelId=intent.getStringExtra("modelId")?.takeIf{it in HandsArSceneView.MODELS}?:"extinguisher"
        partId=intent.getStringExtra("partId")?.takeIf{it.matches(Regex("FMA[0-9]{1,8}"))}
        val requested=intent.getStringExtra("teachingKey")
        teachingKey=requested?.takeIf{FirstAidSceneGuide.byKey(it)!=null}
        if(requested!=null&&teachingKey==null)entryNote="This step illustration is unavailable. General model exploration is still available."
        profile=Store(this)
        profileContext=getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${profile.workerId}","")?:""
        narrator=TrainingNarrator(this){profile.hi}
        lessonStepId=saved?.getString("lessonStepId")?:intent.getStringExtra("stepId")
        intent.getStringExtra("scenarioId")?.let{id->
            lesson=runCatching{
                val catalog=LearningCatalog(this)
                HandsLessonSession.create(catalog.scenario(id),SpatialCatalog(this).data.getJSONObject("steps"),catalog.version,profileContext=="School",lessonStepId)
            }.getOrNull()
            if(lesson==null){
                page=-1
                val root=column(18).apply{tag="hands-ar-unavailable";setBackgroundColor(Palette.canvas)}
                root.add(label("Lesson unavailable",23f,Palette.ink,true).asHeading(),bottom=12)
                root.add(label("This lesson or step is unavailable for the current learner. Return to Learn and choose a supported lesson."),bottom=16)
                root.add(action("Return to learning"){finish()})
                setContentView(root);back.enabled(true);return
            }
            lesson!!.current.let{step->modelId=step.modelId;teachingKey=step.teachingKey;partId=step.partId;lessonStepId=step.stepId}
        }
        run{
            val school=profileContext=="School"
            if(school&&teachingKey!=null){
                val scenario=LearningCatalog(this).scenarios.firstOrNull{it.optString("id")==teachingKey!!.substringBefore('/')}
                if(scenario==null||!HeadsetLessonPolicy.allowed(scenario,true)){teachingKey=null;partId=null;entryNote="School profiles use general model exploration. Adult treatment illustrations are unavailable here."}
            }
        }
        runCatching{CardboardBridge.initialise(this)}
        back.enabled(true);renderSetup()
    }
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putBoolean("headset",headset);out.putInt("page",page);out.putBoolean("space",space);out.putBoolean("rearCamera",rearCamera);out.putBoolean("privacy",privacy);out.putString("lessonStepId",lessonStepId)}
    override fun onResume(){super.onResume();resumed=true;if(!profileCurrent()){permissionStartPending=false;scene?.pause();finish();return};if(active)scene?.resume()else if(::start.isInitialized){refreshReadiness();continuePermissionStart()}}
    override fun onPause(){resumed=false;scene?.pause();super.onPause()}
    override fun onDestroy(){scene?.close();scene=null;back.close();if(::narrator.isInitialized)narrator.close();if(::profile.isInitialized)profile.close();super.onDestroy()}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);scene?.focus(hasFocus)}
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){goBack()}
    private fun goBack(){
        permissionStartPending=false
        scene?.let{it.back();return}
        if(popContentPage())return
        finish()
    }
    private fun shell(title:String):LinearLayout {
        val root=column().apply{tag="hands-ar-root";setBackgroundColor(Palette.canvas)}
        root.setOnApplyWindowInsetsListener{v,i->
            if(Build.VERSION.SDK_INT>=30){val insets=i.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());v.setPadding(insets.left,insets.top,insets.right,insets.bottom)}
            else v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom)
            i
        }
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(6),dp(16),dp(6));setBackgroundColor(Palette.surface)}
        top.addView(practiceTool("",PracticeIcon.BACK,"Back"){goBack()}.apply{tag="hands-ar-back"},LinearLayout.LayoutParams(dp(48),dp(48)))
        top.addView(label(title,19f,Palette.ink,true).asHeading(),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(12)})
        root.add(top);return root
    }
    private fun renderSetup(){
        active=false
        fun t(en:String,hi:String)=if(profile.hi)hi else en
        val root=shell(t("Learn in AR","AR में सीखें"))
        val content=column(16)
        val compact=resources.configuration.screenHeightDp<500&&resources.configuration.screenWidthDp>resources.configuration.screenHeightDp
        if(compact)content.setPadding(dp(8),dp(8),dp(8),dp(8))
        val left=if(compact)column()else content
        val right=if(compact)column()else content
        left.add(label(lesson?.current?.scenarioTitle?:t("Your space. Your hands.","आपकी जगह। आपके हाथ।"),if(compact)19f else 22f,Palette.ink,true).asHeading(),bottom=if(compact)4 else 10)
        val modes=LinearLayout(this)
        modes.addView(action(t("Phone","फ़ोन"),!headset){if(headset){headset=false;space=false;rearCamera=false};renderSetup()}.apply{tag="hands-ar-handheld";isSelected=!headset},LinearLayout.LayoutParams(0,dp(if(compact)48 else 52),1f))
        modes.addView(action(t("Cardboard","कार्डबोर्ड"),headset){if(!headset){headset=true;space=false;rearCamera=false};renderSetup()}.apply{tag="hands-ar-headset";isSelected=headset},LinearLayout.LayoutParams(0,dp(if(compact)48 else 52),1f).apply{marginStart=dp(8)})
        left.add(modes,bottom=if(compact)4 else 12)
        if(!compact)left.add(label(t("Point + pinch to select. Drag with one hand; use two hands to resize and turn.","इशारा और पिंच करके चुनें। एक हाथ से खिसकाएँ; दो हाथों से आकार और दिशा बदलें।"),15f),bottom=10)
        left.add(label(if(compact)t("On-device camera · SDK metrics","कैमरा फ़ोन पर · SDK मेट्रिक्स")else t("Camera and hand processing stay on this phone. MediaPipe may send usage and performance metrics.","कैमरा और हाथों की प्रोसेसिंग इसी फ़ोन पर होती है। MediaPipe उपयोग और प्रदर्शन की जानकारी भेज सकता है।"),14f,Palette.muted),bottom=if(compact)4 else 8)
        left.add(action(t("Privacy & controls","निजता और नियंत्रण"),false,ActionRole.NEUTRAL){
            PageDialogBuilder(this).setTitle(t("Camera, hands & controls","कैमरा, हाथ और नियंत्रण"))
                .setMessage(t("The app does not store or upload camera images or hand points. MediaPipe may send vendor usage/performance metrics.\n\nKeep the whole hand visible. Point and pinch to pick up; release to place. Two pinches resize and turn. Open your palm to cancel. Touch and viewer-button alternatives remain available.\n\nForeground masking is not measured depth and can miss thin fingers. The eye control enables see-through assistance.\n\nCardboard shows the same rear-camera image to both eyes through calibrated lenses. Keep the rear camera uncovered. Stay seated, never walk or handle real hazards through this view.\n\nThis is illustrative practice, not diagnosis or supervised practical certification.","ऐप कैमरे की तस्वीरें या हाथों के बिंदु सहेजता या अपलोड नहीं करता। MediaPipe उपयोग और प्रदर्शन की जानकारी भेज सकता है।\n\nपूरा हाथ कैमरे में रखें। इशारा और पिंच से चुनें, छोड़कर रखें। दो पिंच से आकार और दिशा बदलें। खुली हथेली से रद्द करें। टच और व्यूअर बटन भी उपलब्ध हैं।\n\nफ़ोरग्राउंड मास्क वास्तविक गहराई नहीं मापता और पतली उँगलियाँ छूट सकती हैं। आँख वाला नियंत्रण पारदर्शी दृश्य देता है।\n\nकार्डबोर्ड में दोनों आँखों को एक ही रियर-कैमरा दृश्य मिलता है। कैमरा खुला रखें। बैठे रहें; इस दृश्य से चलें या असली खतरे न सँभालें।\n\nयह सीखने का चित्रण है, निदान या व्यावहारिक प्रमाणन नहीं।"))
                .setPositiveButton(t("Done","हो गया"),null).show()
        },bottom=if(compact)2 else 8)
        (if(compact)left else right).add(CheckBox(this).apply{tag="hands-ar-privacy";text=t("I understand camera processing","मैं कैमरा प्रोसेसिंग समझता/समझती हूँ");isChecked=privacy;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->privacy=checked;refreshReadiness()}},bottom=4)
        right.add(CheckBox(this).apply{tag="hands-ar-space";text=if(headset)t("I am seated in a clear area","मैं खाली जगह में बैठा/बैठी हूँ")else t("My practice space is clear","मेरे अभ्यास की जगह खाली है");isChecked=space;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->space=checked;refreshReadiness()}},bottom=4)
        if(headset){
            right.add(CheckBox(this).apply{tag="hands-ar-camera";text=t("Rear camera is uncovered","रियर कैमरा खुला है");isChecked=rearCamera;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->rearCamera=checked;refreshReadiness()}},bottom=4)
            right.add(label(t("Stay seated. Single-camera view; no stereo depth.","बैठे रहें। एक कैमरे का दृश्य; स्टीरियो गहराई नहीं।"),13f,Palette.amber),bottom=6)
            right.add(action(t("Viewer settings","व्यूअर सेटिंग"),false){
                PageDialogBuilder(this).setTitle(t("Calibrate your viewer","अपना व्यूअर कैलिब्रेट करें"))
                    .setItems(arrayOf(t("Scan manufacturer's viewer QR","निर्माता का व्यूअर QR स्कैन करें"),t("I use original Google Cardboard v1","मैं मूल Google Cardboard v1 उपयोग करता/करती हूँ"))){_,choice->
                        runCatching{if(choice==0)CardboardBridge.scanViewer(this)else{CardboardBridge.initialise(this);CardboardBridge.useDefaultViewer()}}
                            .onFailure{error="Viewer calibration could not start.";renderSetup()}
                        refreshReadiness()
                    }.setNegativeButton(t("Cancel","रद्द करें"),null).show()
            },bottom=4)
        }
        viewerStatus=label("",13f,Palette.muted).apply{tag="hands-ar-viewer-status"};right.add(viewerStatus,bottom=6)
        if(lesson!=null&&!compact)right.add(label(t("Draft lesson text and voice: English. App language stays unchanged.","इस ड्राफ्ट पाठ का विवरण और आवाज़ अंग्रेज़ी में हैं। ऐप की चुनी हुई भाषा नहीं बदलेगी।"),14f,Palette.amber).apply{tag="hands-ar-content-language"},bottom=6)
        entryNote?.let{right.add(label(it,14f,Palette.amber),bottom=6)}
        error?.let{right.add(label(it,14f,Palette.danger).apply{tag="hands-ar-error"},bottom=6)}
        if(compact){val columns=LinearLayout(this).apply{isBaselineAligned=false};columns.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(18)});columns.addView(right,LinearLayout.LayoutParams(0,-2,1f));content.add(columns)}
        start=action(if(lesson!=null)t("Start lesson in English","अंग्रेज़ी में शुरू करें")else if(headset)t("Start seated AR","बैठकर AR शुरू करें")else t("Start live AR","लाइव AR शुरू करें")){requestStart()}.apply{tag="hands-ar-start"}
        root.addView(paged(content,profile.hi),LinearLayout.LayoutParams(-1,0,1f))
        root.add(start,bottom=8)
        setContentView(root);root.requestApplyInsets();showSystemBars();refreshReadiness()
    }
    private fun refreshReadiness(){
        if(!::start.isInitialized)return
        val viewer=!headset||(CardboardBridge.nativeAvailable&&CardboardBridge.hasViewerProfile())
        start.isEnabled=canStart()
        if(::viewerStatus.isInitialized)viewerStatus.text=if(!headset)"ARCore required · on-device hand controls"else if(!viewer)"Calibrate your viewer to start."else if(resources.configuration.orientation!=Configuration.ORIENTATION_LANDSCAPE)"Viewer ready · rotate to landscape to start."else"Viewer calibrated"+(if(lesson!=null)" · draft English lesson"else"")
    }
    private fun profileCurrent()=::profile.isInitialized&&profile.isCurrentProfile()&&getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${profile.workerId}","")==profileContext
    private fun canStart()=page==1&&profileCurrent()&&space&&privacy&&(!headset||(rearCamera&&CardboardBridge.nativeAvailable&&CardboardBridge.hasViewerProfile()&&resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE))
    private fun requestStart(){
        if(!canStart()||!resumed)return
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){permissionStartPending=true;requestPermissions(arrayOf(Manifest.permission.CAMERA),CAMERA_REQUEST);return}
        startWorkspace()
    }
    private fun continuePermissionStart(){
        if(!permissionStartPending||!resumed||!canStart()||checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return
        permissionStartPending=false
        startWorkspace()
    }
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){
        super.onRequestPermissionsResult(code,permissions,results)
        if(code==CAMERA_REQUEST){
            if(!profileCurrent()){permissionStartPending=false;finish();return}
            if(results.firstOrNull()==PackageManager.PERMISSION_GRANTED){continuePermissionStart();if(scene==null)refreshReadiness()}
            else {permissionStartPending=false;if(page==1){error="Camera permission is required for live AR. You can still return to ordinary 3D learning.";renderSetup()}}
        }
    }
    private fun startWorkspace(){
        if(!resumed||!canStart()||checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return
        error=null
        if(headset&&resources.configuration.orientation!=Configuration.ORIENTATION_LANDSCAPE){
            // A configuration change returns to the consent screen instead of silently opening a
            // camera after Activity recreation. Rotate first, then explicitly start in landscape.
            error="Rotate the phone to landscape, then start again.";renderSetup();return
        }
        val view=HandsArSceneView(this,headset,modelId,teachingKey,partId,lesson,profile.hi)
        scene=view;active=true
        view.tag="hands-ar-live"
        view.onExit={leaveWorkspace()}
        view.onFailure={message->error=message;leaveWorkspace()}
        view.onStatus={message->view.contentDescription=message}
        view.onStepChanged={id->lessonStepId=id}
        view.onInstruction={text,language->narrator.show(text,language)}
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(headset)requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        val stage=android.widget.FrameLayout(this).apply{setBackgroundColor(if(headset)android.graphics.Color.BLACK else Palette.canvas)}
        stage.addView(view,android.widget.FrameLayout.LayoutParams(-1,-1))
        if(!headset)stage.setOnApplyWindowInsetsListener{root,insets->
            if(Build.VERSION.SDK_INT>=30){val b=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(b.left,b.top,b.right,b.bottom)}
            else root.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        setContentView(stage);stage.requestApplyInsets();view.focus(window.decorView.hasWindowFocus());if(headset)hideSystemBars()else showSystemBars()
        if(resumed)view.resume()
        lesson?.current?.let{narrator.show(it.detailText,"en-IN")}
    }
    private fun leaveWorkspace(){scene?.close();scene=null;active=false;window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;page=1;renderSetup()}
    override fun dispatchKeyEvent(event:KeyEvent):Boolean{
        val view=scene
        if(headset&&view!=null&&event.keyCode in setOf(KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_HEADSETHOOK)){
            if(event.action==KeyEvent.ACTION_DOWN){if(event.repeatCount==0)view.buttonDown()}
            else if(event.action==KeyEvent.ACTION_UP)view.buttonUp(event.isCanceled)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    @Suppress("DEPRECATION")
    private fun showSystemBars(){if(Build.VERSION.SDK_INT>=30)window.insetsController?.show(WindowInsets.Type.systemBars())else window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE}
    @Suppress("DEPRECATION")
    private fun hideSystemBars(){if(Build.VERSION.SDK_INT>=30){window.insetsController?.systemBarsBehavior=android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;window.insetsController?.hide(WindowInsets.Type.systemBars())}else window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY}
    companion object {private const val CAMERA_REQUEST=731}
}
