package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** Step-linked inspection. Never submits answers or creates an observation/competence receipt. */
class LearningSpatialActivity:Activity(){
 private lateinit var store:Store
 private lateinit var plan:JSONObject
 private lateinit var step:JSONObject
 private lateinit var host:FrameLayout
 private lateinit var status:TextView
 private lateinit var placement:Button
 private var equipment:ProcedureSceneView?=null
 private var body:AnatomyScene?=null
 private var data:AnatomyData?=null
 private var camera=false
 private var foreground=false
 private var generation=0
 private var loading=false
 private val loader=Executors.newSingleThreadExecutor()
 private val back by lazy{AppBackNavigation(this){finish()}}
 private fun allowed()=store.isCurrentProfile() && (getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${store.workerId}","")!="School"||plan.getString("audience")=="awareness")
 override fun onCreate(saved:Bundle?){super.onCreate(saved);store=Store(this)
  val scenarioId=intent.getStringExtra("scenarioId")?:run{finish();return};val stepId=intent.getStringExtra("stepId")?:run{finish();return}
  plan=SpatialCatalog(this).step(scenarioId,stepId)?:run{finish();return}
  step=LearningCatalog(this).scenarios.firstOrNull{it.getString("id")==scenarioId}?.getJSONArray("steps")?.objects()?.firstOrNull{it.getString("id")==stepId}?:run{finish();return}
  if(!allowed()){finish();return}
  val root=column(12).apply{setBackgroundColor(Palette.canvas)}
  root.setOnApplyWindowInsetsListener{v,i->if(android.os.Build.VERSION.SDK_INT>=30){val b=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(dp(12)+b.left,b.top,dp(12)+b.right,b.bottom)};i}
  root.add(action("‹ Return to this step",false,ActionRole.NEUTRAL){finish()}.apply{tag="spatial-step-return"})
  root.add(label(step.getString("title"),20f,Palette.ink,true).asHeading(),bottom=6)
  root.add(label("3D / AR inspection · ${plan.getString("kind")}",12f,Palette.blue),bottom=6)
  host=FrameLayout(this);root.addView(host,LinearLayout.LayoutParams(-1,0,1f))
  status=label("Preparing the step scene…",13f,Palette.muted).apply{accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;maxLines=3;tag="spatial-step-status"};root.add(status,top=8,bottom=8)
  val row=LinearLayout(this)
  fun button(title:String,tagName:String,act:()->Unit){row.addView(action(title,false,ActionRole.NEUTRAL,act).apply{tag=tagName;textSize=13f},LinearLayout.LayoutParams(0,-2,1f))}
  button("3D view","spatial-step-3d"){camera=false;attach()}
  button("Place in AR","spatial-step-ar"){
   PageDialogBuilder(this).setTitle("Choose a clear practice space").setMessage("Place this illustrative scene on an empty surface, away from real hazards. The camera is used for placement, not hazard detection. No camera images are saved by this viewer.")
    .setPositiveButton("Continue"){_,_->if(allowed()){camera=true;if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.CAMERA),76)else attach()}}
    .setNegativeButton("Cancel",null).show()
  }
  button("Inspect","spatial-step-inspect"){inspect()}
  button("Hands AR","spatial-step-hands"){
   if(allowed())startActivity(android.content.Intent(this,HandsArActivity::class.java)
    .putExtra("modelId",if(plan.getString("model")=="anatomy")"body" else "extinguisher")
    .putExtra("teachingKey",plan.optString("teachingKey"))
    .putExtra("partId",plan.optString("part")))
  }
  placement=action("Place at camera centre",false,ActionRole.CAMERA){body?.place();equipment?.placeAtCameraCentre()}.apply{tag="spatial-step-place";visibility=View.GONE}
  root.add(placement,bottom=6);root.add(row,bottom=8)
  root.add(action("Step cue",false,ActionRole.NEUTRAL){PageDialogBuilder(this).setTitle(step.getString("title")).setMessage(step.getString("cue")+"\n\nReturn to the lesson to make your decision. Inspection does not complete this step.").setPositiveButton("Close",null).show()})
  setContentView(root);root.requestApplyInsets();back.enabled(true);attach()
 }
 private fun attach(){
  if(!allowed())return
  placement.visibility=if(camera)View.VISIBLE else View.GONE
  equipment?.close();equipment=null;body?.close();body=null;host.removeAllViews();generation++
  if(plan.getString("model")!="anatomy"){
   equipment=ProcedureSceneView(this).also{scene->host.addView(scene,FrameLayout.LayoutParams(-1,-1));scene.useImmersive(true)
    scene.configure(ProcedureSceneView.Scene(plan.getString("model"),step.getString("id"),emptyList(),emptySet(),enabled=false,illustrationOnly=true),store.hi){_,_,_->}
    scene.onStatus={status.text=it};scene.useCamera(camera);if(foreground)scene.resume()}
  }else if(data!=null){
   body=AnatomyScene(this,data!!,camera,bay=plan.getString("kind")=="teaching").also{scene->host.addView(scene,FrameLayout.LayoutParams(-1,-1));scene.onStatus={status.text=it};scene.onSelected={status.text=it.name};scene.onTeachingPicked={id,_->status.text=id.removePrefix("teaching:").replace('-',' ')}
    val teaching=plan.optString("teachingKey").takeIf{it.isNotEmpty()}
    scene.change{it.copy(layer=if(teaching!=null)"skin" else "skeleton",supine=teaching!=null,teachingKey=teaching,equipment=teaching?.startsWith("adult-collapse-response/")==true,teachingPaused=true)}
    if(teaching!=null)scene.focusTeachingStep()else plan.optString("part").takeIf{it.isNotEmpty()}?.let{scene.select(it,true)}
    if(foreground)scene.resume()}
  }else if(!loading){
   loading=true;status.text="Loading the body and teaching props…"
   loader.submit{val loaded=runCatching{AnatomyData.load(this)};runOnUiThread{loading=false;if(isFinishing||isDestroyed)return@runOnUiThread
    loaded.onSuccess{data=it;attach()}.onFailure{status.text="Model could not load. Use the step cue, or retry 3D view."}}}
  }
 }
 private fun inspect(){
  if(!allowed())return
  if(body!=null){
   val scene=body!!
   if(camera){PageDialogBuilder(this).setTitle("AR placement").setItems(arrayOf("Place at centre","Move placement","Return to 3D close-up")){_,i->when(i){0->scene.place();1->scene.move();else->{camera=false;attach()}}}.setNegativeButton("Close",null).show();return}
   val teaching=plan.optString("teachingKey").takeIf{it.isNotEmpty()}
   if(teaching!=null){scene.focusTeachingStep();status.text="Focused teaching props. Drag to orbit; use the view options below."}
   val options=arrayOf("Focus step","Rotate left","Rotate right","Zoom in","Zoom out","Reset view","Teaching parts")
   PageDialogBuilder(this).setTitle("Inspection controls").setItems(options){_,i->when(i){
    0->if(teaching!=null)scene.focusTeachingStep()else scene.select(plan.optString("part"),true)
    1->scene.change{it.copy(yaw=it.yaw-20f)};2->scene.change{it.copy(yaw=it.yaw+20f)}
    3->scene.change{it.copy(distance=(it.distance*.8f).coerceAtLeast(.35f))};4->scene.change{it.copy(distance=(it.distance*1.25f).coerceAtMost(5f))}
    5->scene.change{it.copy(yaw=0f,pitch=0f,distance=2.6f,panX=0f,panY=0f)}
    6->{val guide=FirstAidSceneGuide.byKey(teaching);val parts=FirstAidTeachingProps.visible(guide)+(if(scene.controls.equipment)FirstAidEquipment.parts else emptyList())
      if(parts.isEmpty())status.text="This view uses anatomy. Select the structure in the body explorer."
      else PageDialogBuilder(this).setTitle("Inspect a teaching detail").setItems(parts.map{it.id.removePrefix("teaching:").removePrefix("trainer-aed-").replace('-',' ')}.toTypedArray()){_,index->scene.focusTeachingPart(parts[index].id);status.text=parts[index].id.removePrefix("teaching:").replace('-',' ')}.setNegativeButton("Close",null).show()}
    else->Unit
   }}.setNegativeButton("Close",null).show()
  }else{
   val catalog=SpatialCatalog(this);val base=catalog.base(plan.getString("model"))
   if(catalog.parts(base).isNotEmpty())startActivity(android.content.Intent(this,EquipmentActivity::class.java).putExtra("moduleId",base).putExtra("fireSceneId",if(plan.getString("model")=="fire-damaged")"damaged" else "ready"))
   else PageDialogBuilder(this).setTitle("Scene inspection").setMessage(step.getString("cue")+"\n\nThis miniature illustrates the decision context; it cannot identify a real safe route.").setPositiveButton("Close",null).show()
  }
 }
 override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){super.onRequestPermissionsResult(code,permissions,results);if(code==76){camera=results.firstOrNull()==PackageManager.PERMISSION_GRANTED;attach();if(!camera)status.text="Camera access declined. 3D inspection remains available."}}
 override fun onResume(){super.onResume();foreground=true;if(::plan.isInitialized){if(!allowed()){finish();return};equipment?.resume();body?.resume()}}
 override fun onPause(){foreground=false;equipment?.pause();body?.pause();super.onPause()}
 override fun onDestroy(){equipment?.close();body?.close();equipment=null;body=null;data=null;if(::host.isInitialized)host.removeAllViews();loader.shutdownNow();back.close();if(::store.isInitialized)store.close();super.onDestroy()}
}
