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
    private var page=0
    private var modelId="extinguisher"
    private var teachingKey:String?=null
    private var partId:String?=null
    private var entryNote:String?=null
    private var error:String?=null
    private lateinit var start:Button
    private lateinit var viewerStatus:TextView
    private var resumed=false
    private var permissionStartPending=false
    override fun onCreate(saved:Bundle?){
        super.onCreate(saved)
        headset=saved?.getBoolean("headset")?:intent.getBooleanExtra("headset",false)
        page=saved?.getInt("page")?:0
        space=saved?.getBoolean("space")?:false;rearCamera=saved?.getBoolean("rearCamera")?:false;privacy=saved?.getBoolean("privacy")?:false
        modelId=intent.getStringExtra("modelId")?.takeIf{it in HandsArSceneView.MODELS}?:"extinguisher"
        partId=intent.getStringExtra("partId")?.takeIf{it.matches(Regex("FMA[0-9]{1,8}"))}
        val requested=intent.getStringExtra("teachingKey")
        teachingKey=requested?.takeIf{FirstAidSceneGuide.byKey(it)!=null}
        if(requested!=null&&teachingKey==null)entryNote="This step illustration is unavailable. General model exploration is still available."
        profile=Store(this)
        profileContext=getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${profile.workerId}","")?:""
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
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putBoolean("headset",headset);out.putInt("page",page);out.putBoolean("space",space);out.putBoolean("rearCamera",rearCamera);out.putBoolean("privacy",privacy)}
    override fun onResume(){super.onResume();resumed=true;if(!profileCurrent()){permissionStartPending=false;scene?.pause();finish();return};if(active)scene?.resume()else if(::start.isInitialized){refreshReadiness();continuePermissionStart()}}
    override fun onPause(){resumed=false;scene?.pause();super.onPause()}
    override fun onDestroy(){scene?.close();scene=null;back.close();if(::profile.isInitialized)profile.close();super.onDestroy()}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);scene?.focus(hasFocus)}
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){goBack()}
    private fun goBack(){
        permissionStartPending=false
        scene?.let{it.back();return}
        if(popContentPage())return
        if(page>0){page--;renderSetup()}else finish()
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
        val root=shell(if(headset)"Live AR in a headset"else"Hands in live AR")
        val content=column(18)
        if(page==0){
            content.add(label("Control virtual models in your surroundings",23f,Palette.ink,true),bottom=10)
            content.add(label("Point and pinch to pick up a model. Move one hand to drag; pinch with both hands to resize and turn. Open your palm to cancel.",16f),bottom=14)
            val modes=LinearLayout(this)
            modes.addView(action("Handheld",!headset){headset=false;rearCamera=false;renderSetup()}.apply{tag="hands-ar-handheld"},LinearLayout.LayoutParams(0,dp(52),1f))
            modes.addView(action("Cardboard",headset){headset=true;renderSetup()}.apply{tag="hands-ar-headset"},LinearLayout.LayoutParams(0,dp(52),1f).apply{marginStart=dp(10)})
            content.add(modes,bottom=12)
            content.add(label(if(headset)"Experimental single-camera passthrough. Both eyes show the same rear-camera image through calibrated lenses. This does not provide stereo depth or reliable real-world distance."else"For two-hand gestures, place the phone on a stable stand. Keep the whole hand visible; show both hands open before pinching.",15f,Palette.muted),bottom=12)
            content.add(label("Choose virtual models only. The app does not identify arbitrary real objects, diagnose injuries or measure treatment skills.",15f,Palette.muted),bottom=12)
            entryNote?.let{content.add(label(it,15f,Palette.amber),bottom=12)}
            start=action("Continue"){page=1;renderSetup()}.apply{tag="hands-ar-start"}
        }else{
            content.add(label(if(headset)"Prepare before inserting the phone"else"Prepare your practice area",22f,Palette.ink,true),bottom=10)
            content.add(label("Use a well-lit textured table or floor. Camera images and hand points are processed on this phone; this app does not store or upload them. The MediaPipe SDK may send vendor usage and performance metrics.",15f),bottom=8)
            content.add(CheckBox(this).apply{tag="hands-ar-privacy";text="I understand camera and hand processing";isChecked=privacy;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->privacy=checked;refreshReadiness()}},bottom=4)
            content.add(CheckBox(this).apply{tag="hands-ar-space";text=if(headset)"I am seated, with clear space around my hands"else"My practice space is clear";isChecked=space;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->space=checked;refreshReadiness()}},bottom=4)
            if(headset){
                content.add(CheckBox(this).apply{tag="hands-ar-camera";text="The rear camera is uncovered";isChecked=rearCamera;minHeight=dp(48);setTextColor(Palette.ink);setOnCheckedChangeListener{_,checked->rearCamera=checked;refreshReadiness()}},bottom=8)
                content.add(label("Stay seated. Never walk, handle hot/sharp items or navigate a hazard through this camera view. Button: tap changes the highlighted control; hold activates it.",15f,Palette.amber),bottom=10)
                content.add(action("Scan the manufacturer's viewer QR",false){runCatching{CardboardBridge.scanViewer(this)}.onFailure{error="Viewer scanning could not start.";renderSetup()}},bottom=6)
                content.add(action("I use the original Google Cardboard v1",false){runCatching{CardboardBridge.initialise(this);CardboardBridge.useDefaultViewer()}.onFailure{error="That viewer profile could not be saved."};refreshReadiness()},bottom=6)
            }
            viewerStatus=label("",14f,Palette.muted).apply{tag="hands-ar-viewer-status"};content.add(viewerStatus,bottom=8)
            error?.let{content.add(label(it,15f,Palette.danger).apply{tag="hands-ar-error"},bottom=10)}
            content.add(label("Keep the whole hand in view. Point at a visible model and pinch; release to place. Ordinary touch controls remain available in handheld mode.",15f,Palette.muted),bottom=10)
            start=action(if(headset)"Start seated AR"else"Start live AR"){requestStart()}.apply{tag="hands-ar-start"}
        }
        root.addView(paged(content,false),LinearLayout.LayoutParams(-1,0,1f))
        root.add(start.apply{setPadding(dp(16),dp(8),dp(16),dp(8))},top=6,bottom=8)
        setContentView(root);root.requestApplyInsets();showSystemBars();refreshReadiness()
    }
    private fun refreshReadiness(){
        if(!::start.isInitialized)return
        if(page==0){start.isEnabled=true;return}
        val viewer=!headset||(CardboardBridge.nativeAvailable&&CardboardBridge.hasViewerProfile())
        start.isEnabled=canStart()
        if(::viewerStatus.isInitialized)viewerStatus.text=if(!headset)"Models and hand inference work on-device. ARCore support is required."else if(!viewer)"Scan your actual viewer's QR, or select v1 only for an original Cardboard v1."else if(resources.configuration.orientation!=Configuration.ORIENTATION_LANDSCAPE)"Viewer profile ready. Rotate the phone to landscape before starting."else"Viewer profile ready. If this is a different viewer, scan its own QR."
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
        val view=HandsArSceneView(this,headset,modelId,teachingKey,partId)
        scene=view;active=true
        view.tag="hands-ar-live"
        view.onExit={leaveWorkspace()}
        view.onFailure={message->error=message;leaveWorkspace()}
        view.onStatus={message->view.contentDescription=message}
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(headset)requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(view);view.focus(window.decorView.hasWindowFocus());if(headset)hideSystemBars()else showSystemBars()
        if(resumed)view.resume()
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
