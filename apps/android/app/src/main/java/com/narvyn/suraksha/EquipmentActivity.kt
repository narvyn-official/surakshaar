package com.narvyn.suraksha

import android.app.Activity
import android.os.Bundle
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.*
import android.widget.LinearLayout
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Fixed-screen equipment explorer. Inspection controls never displace the model into a page. */
class EquipmentActivity:Activity(){
 private lateinit var surface:GLSurfaceView
 private var dock:View?=null
 private lateinit var root:LinearLayout
 private lateinit var workspace:LinearLayout
 @Volatile private var yaw=0f
 @Volatile private var distance=1.25f
 @Volatile private var pitch=0f
 @Volatile private var focusY=.26f
 private var hi=false
 private var module="fire"
 private var describeView:()->Unit={}
 private val back by lazy{AppBackNavigation(this){if(dock!=null)closeDock()else finish()}}
 private fun t(en:String,hindi:String)=if(hi)hindi else en
 @android.annotation.SuppressLint("GestureBackNavigation")
 @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){if(dock!=null)closeDock()else super.onBackPressed()}
 override fun onCreate(state:Bundle?){super.onCreate(state)
  module=intent.getStringExtra("moduleId")?.takeIf{it in listOf("fire","gas","machinery","ppe","emergency")}?:"fire"
  hi=Store(this).use{it.hi}
  yaw=state?.getFloat("yaw")?:0f;distance=(state?.getFloat("distance")?:1.25f).coerceIn(.38f,3f);pitch=state?.getFloat("pitch")?:0f;focusY=state?.getFloat("focusY")?:.26f
  root=column(12).apply{setBackgroundColor(Palette.canvas);accessibilityPaneTitle=t("Equipment explorer","उपकरण देखें")}
  root.setOnApplyWindowInsetsListener{v,i->if(android.os.Build.VERSION.SDK_INT>=30){val b=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(12)+b.left,b.top,dp(12)+b.right,b.bottom)}else{v.setPadding(dp(12)+i.systemWindowInsetLeft,i.systemWindowInsetTop,dp(12)+i.systemWindowInsetRight,i.systemWindowInsetBottom)};i}
  val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
  header.addView(practiceTool("",PracticeIcon.BACK,t("Back to learning","सीखने पर लौटें")){finish()},LinearLayout.LayoutParams(dp(48),dp(48)))
  header.addView(label(t("Equipment explorer","उपकरण देखें"),18f,Palette.ink,true).asHeading(),LinearLayout.LayoutParams(0,-2,1f))
  header.addView(practiceTool("",PracticeIcon.RESET,t("Reset equipment view","उपकरण दृश्य रीसेट करें")){resetView()},LinearLayout.LayoutParams(dp(48),dp(48)))
  root.add(header)
  surface=GLSurfaceView(this);surface.setEGLContextClientVersion(2);surface.setEGLConfigChooser(8,8,8,8,16,0);surface.preserveEGLContextOnPause=true;surface.tag="equipment-surface"
  surface.contentDescription=t("Training model. Drag to rotate, pinch to zoom. View provides button controls.","प्रशिक्षण मॉडल। खींचकर घुमाएँ, दो उँगलियों से ज़ूम करें। दृश्य में बटन नियंत्रण हैं।")
  val equipment=WorldEquipment();val projection=FloatArray(16);val view=FloatArray(16);val vp=FloatArray(16);val model=FloatArray(16);val eye=floatArrayOf(0f,.38f,distance)
  surface.setRenderer(object:GLSurfaceView.Renderer{
   override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){GLES20.glClearColor(.965f,.973f,.99f,1f);equipment.create()}
   override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,38f,w.toFloat()/h.coerceAtLeast(1),.05f,10f)}
   override fun onDrawFrame(gl:GL10?){GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT);eye[1]=focusY+.12f;eye[2]=distance;Matrix.setLookAtM(view,0,eye[0],eye[1],eye[2],0f,focusY,0f,0f,1f,0f);Matrix.multiplyMM(vp,0,projection,0,view,0);Matrix.setIdentityM(model,0);Matrix.rotateM(model,0,yaw,0f,1f,0f);Matrix.translateM(model,0,0f,focusY,0f);Matrix.rotateM(model,0,pitch,1f,0f,0f);Matrix.translateM(model,0,0f,-focusY,0f);equipment.draw(vp,model,module,eye)}
  });surface.renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY
  var lastX=0f;var lastY=0f;var multi=false
  val scale=ScaleGestureDetector(this,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(d:ScaleGestureDetector):Boolean{distance=(distance/d.scaleFactor).coerceIn(.38f,3f);surface.requestRender();return true}})
  surface.setOnTouchListener{_,e->scale.onTouchEvent(e);when(e.actionMasked){
   MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;multi=false;true}
   MotionEvent.ACTION_POINTER_DOWN->{multi=true;true}
   MotionEvent.ACTION_MOVE->{if(!multi&&!scale.isInProgress&&e.pointerCount==1){yaw+=(e.x-lastX)*.35f;pitch=(pitch+(e.y-lastY)*.2f).coerceIn(-60f,60f);surface.requestRender()};lastX=e.x;lastY=e.y;true}
   MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{multi=false;describeView();true}
   else->true
  }}
  workspace=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  workspace.addView(surface,LinearLayout.LayoutParams(0,-1,1f))
  root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
  val status=label("",12f,Palette.muted).apply{tag="equipment-view-state";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;maxLines=2}
  describeView={status.text=t("Training model · ${(125f/distance).toInt()}% zoom · ${((yaw%360+360)%360).toInt()}°", "प्रशिक्षण मॉडल · ${(125f/distance).toInt()}% ज़ूम · ${((yaw%360+360)%360).toInt()}°")};describeView();root.add(status,top=6,bottom=6)
  val actions=LinearLayout(this).apply{tag="equipment-actions";gravity=Gravity.CENTER_VERTICAL}
  actions.addView(practiceTool(t("View","दृश्य"),PracticeIcon.VIEW,t("Equipment view controls","उपकरण दृश्य नियंत्रण")){if(dock==null)openDock()else closeDock()},LinearLayout.LayoutParams(0,-2,1f))
  actions.addView(practiceTool(t("Parts","पुर्ज़े"),PracticeIcon.INFO,t("Inspect equipment parts","उपकरण के पुर्ज़े जानें")){showParts()},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(6)})
  actions.addView(practiceTool(t("Practice","अभ्यास"),PracticeIcon.TARGET,t("Practice finding parts","पुर्ज़े पहचानने का अभ्यास")){startActivity(android.content.Intent(this,ComponentPracticeActivity::class.java).putExtra("moduleId",module))},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(6)})
  root.add(actions,bottom=8);setContentView(root);root.requestApplyInsets();back.enabled(true)
 }
 private fun updateView(){surface.requestRender();describeView()}
 private fun resetView(){yaw=0f;pitch=0f;distance=1.25f;focusY=.26f;updateView()}
 private fun closeDock(){dock?.let{(it.parent as? ViewGroup)?.removeView(it)};dock=null}
 private fun openDock(){
  val panel=column(6).apply{tag="equipment-view-dock";background=shape(Palette.surface,12,Palette.line)};dock=panel
  fun row()=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false}
  fun LinearLayout.choice(text:String,description:String=text,act:()->Unit){addView(action(text,false,ActionRole.NEUTRAL,act).apply{contentDescription=description;textSize=12f;minHeight=dp(48);minimumHeight=dp(48);setPadding(dp(5),dp(5),dp(5),dp(5))},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(3)})}
  val preset=row();preset.choice(t("Front","सामने")){yaw=0f;pitch=0f;updateView()};preset.choice(t("Back","पीछे")){yaw=180f;pitch=0f;updateView()};preset.choice(t("Side","बगल")){yaw=90f;pitch=0f;updateView()};preset.choice(t("Done","पूर्ण")){closeDock()};panel.add(preset)
  val adjust=row();adjust.choice("−",t("Zoom out","ज़ूम घटाएँ")){distance=(distance*1.18f).coerceAtMost(3f);updateView()};adjust.choice("+",t("Zoom in","ज़ूम बढ़ाएँ")){distance=(distance/1.18f).coerceAtLeast(.38f);updateView()};adjust.choice("←",t("Rotate left","बाएँ घुमाएँ")){yaw-=15f;updateView()};adjust.choice("→",t("Rotate right","दाएँ घुमाएँ")){yaw+=15f;updateView()};panel.add(adjust)
  val detail=row()
  if(module=="fire"){
   detail.choice(t("Label","लेबल")){yaw=0f;pitch=0f;focusY=.256f;distance=.46f;updateView()}
   detail.choice(t("Gauge","गेज")){yaw=0f;pitch=0f;focusY=.455f;distance=.38f;updateView()}
  }else{detail.choice("↑",t("Tilt up","ऊपर झुकाएँ")){pitch=(pitch-15f).coerceAtLeast(-60f);updateView()};detail.choice("↓",t("Tilt down","नीचे झुकाएँ")){pitch=(pitch+15f).coerceAtMost(60f);updateView()}}
  detail.choice(t("Fit","समायोजित"),t("Reset equipment view","उपकरण दृश्य रीसेट करें")){resetView()};panel.add(detail)
  if(resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE){
   dock=paged(panel,hi).apply{tag="equipment-view-dock"}
   workspace.addView(dock,LinearLayout.LayoutParams(0,-1,1f))
  }else root.addView(panel,root.childCount-1,LinearLayout.LayoutParams(-1,-2))
 }
 private fun showParts(){
   val parts=when(module){
    "fire"->listOf(t("Vessel & label","पात्र और लेबल") to t("Confirm the actual extinguisher type and label with a competent trainer. This generic model does not identify a fire class or authorise use.","योग्य प्रशिक्षक से असली अग्निशामक का प्रकार और लेबल समझें। यह सामान्य मॉडल आग का वर्ग नहीं बताता और उपयोग की अनुमति नहीं देता।"),t("Valve, lever & pin","वाल्व, लीवर और पिन") to t("Parts and operation vary by extinguisher. The shaped lever, retaining pin and hose support recognition, not an operating sequence.","अग्निशामक के अनुसार पुर्ज़े और उपयोग बदलते हैं। लीवर, पिन और होज़ पहचान के लिए हैं, संचालन के चरण नहीं हैं।"),t("Illustrative gauge","काल्पनिक गेज") to t("The gauge has no valid pressure reading. Inspection must follow the actual equipment instructions.","गेज कोई मान्य दबाव नहीं बताता। असली उपकरण के निर्देश से जाँच करें।"))
    "gas"->listOf(t("Display & sensor ports","डिस्प्ले और सेंसर छिद्र") to t("SIM marks a simulated display. This model and your phone do not measure gas or oxygen. Use only suitable, checked instruments as trained.","SIM काल्पनिक डिस्प्ले बताता है। मॉडल और फ़ोन गैस या ऑक्सीजन नहीं मापते। प्रशिक्षण के अनुसार केवल उपयुक्त, जाँचे हुए उपकरण उपयोग करें।"),t("Housing & controls","खोल और नियंत्रण") to t("Housing, buttons and clip help identify parts. This is not a real detector’s calibration or bump-test procedure.","खोल, बटन और क्लिप पुर्ज़ों की पहचान के लिए हैं। यह असली डिटेक्टर की कैलिब्रेशन या बम्प टेस्ट प्रक्रिया नहीं है।"))
    "machinery"->listOf(t("Guard & fasteners","गार्ड और कसने वाले पुर्ज़े") to t("The mesh guard separates people from moving parts. Report missing or damaged guards; never reach through them.","जाली गार्ड लोगों को चलते पुर्ज़ों से अलग करता है। गायब या खराब गार्ड की सूचना दें; अंदर हाथ न डालें।"),t("Stop & lock","स्टॉप और ताला") to t("A stop control or fitted lock does not prove effective isolation. Only authorised people follow the equipment-specific procedure.","स्टॉप नियंत्रण या लगा ताला प्रभावी अलगाव साबित नहीं करता। केवल अधिकृत लोग उपकरण-विशिष्ट प्रक्रिया मानें।"))
    "emergency"->listOf(t("Reporting radio","सूचना रेडियो") to t("This generic radio cannot transmit. Report from a safe place using the site's approved communication procedure and equipment.","यह सामान्य रेडियो प्रसारण नहीं कर सकता। सुरक्षित जगह से स्थल की स्वीकृत संचार प्रक्रिया और उपकरण से सूचना दें।"),t("First-aid case","प्राथमिक सहायता किट") to t("Locate the trained first aider and approved supplies before work. This closed case is an illustration; it does not teach treatment.","काम से पहले प्रशिक्षित प्राथमिक सहायक और स्वीकृत सामग्री की जगह जानें। यह बंद किट चित्र है; इससे उपचार नहीं सिखाया जाता।"),t("Assembly marker","एकत्र होने की जगह का चिह्न") to t("Follow the assigned safe route and report at the designated assembly point. This model cannot determine a real safe route or locate missing people.","निर्धारित सुरक्षित रास्ते से एकत्र होने की जगह जाएँ और उपस्थिति बताएँ। यह मॉडल असली सुरक्षित रास्ता नहीं बता सकता और लापता लोगों को नहीं ढूँढ सकता।"))
    else->listOf(t("Helmet & suspension","हेलमेट और अंदर की पट्टियाँ") to t("Shell, brim and suspension are separate parts. Check actual equipment for damage and correct fit; this model is not an approval mark.","खोल, किनारा और अंदर की पट्टियाँ अलग पुर्ज़े हैं। असली उपकरण में नुकसान और सही फिट जाँचें; यह स्वीकृति चिह्न नहीं है।"),t("Eye & hearing protection","आँख और सुनने की क्षमता की सुरक्षा") to t("Lenses, frame, side arms and ear cushions show fit surfaces. Required protection depends on the task assessment; this is not a complete PPE kit.","लेंस, फ्रेम, किनारे की डंडियाँ और कान की गद्दियाँ फिट की जगह दिखाते हैं। ज़रूरी सुरक्षा काम के आकलन पर निर्भर है; यह पूरा पीपीई किट नहीं है।"))
   }
   PageDialogBuilder(this).setTitle(t("Inspect the illustration","चित्र के पुर्ज़े जानें")).setItems(parts.map{it.first}.toTypedArray()){_,which->PageDialogBuilder(this).setTitle(parts[which].first).setMessage(parts[which].second).setPositiveButton(t("Close","बंद करें"),null).show()}.setNegativeButton(t("Close","बंद करें"),null).show()
 }
 override fun onResume(){super.onResume();surface.onResume()}
 override fun onPause(){surface.onPause();super.onPause()}
 override fun onDestroy(){back.close();super.onDestroy()}
 override fun onSaveInstanceState(out:Bundle){out.putFloat("yaw",yaw);out.putFloat("distance",distance);out.putFloat("pitch",pitch);out.putFloat("focusY",focusY);super.onSaveInstanceState(out)}
}
