package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20 as G
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/** An exploration workspace. It never writes lesson answers, certificates or competence evidence. */
internal class HandsArSceneView(
    private val host:Activity,
    val headset:Boolean,
    initialModel:String="extinguisher",
    private val teachingKey:String?=null,
    private val partId:String?=null,
    private val lesson:HandsLessonSession?=null,
    private val hindi:Boolean=false,
):FrameLayout(host),GLSurfaceView.Renderer {
    private val surface=GLSurfaceView(host)
    private val accessibleControls=FrameLayout(host)
    private var accessibleControlIds=emptyList<String>()
    private var accessibleControlKey=""
    private val release=ArSessionRelease()
    private var driver:NativeArDriver?=null
    private val tracker=HandTrackingEngine(host.applicationContext)
    private val gestures=HandGestureController()
    private val worker=Executors.newSingleThreadExecutor{Thread(it,"hands-ar-model")}
    private val pendingBody=AtomicReference<AnatomyData?>()
    @Volatile private var bodyLoadError:String?=null
    private var body:AnatomyData?=null
    private var anatomy:AnatomyRenderer?=null
    private lateinit var graphics:HandsArGraphics
    private lateinit var equipment:WorldEquipment
    private var anchor:Anchor?=null
    private var modelId=initialModel.takeIf{it in MODELS}?:"extinguisher"
    private var offsetX=0f;private var offsetZ=0f;private var scale=1f;private var yaw=0f
    private var committed=HandModelPose()
    private val motion=HandModelMotion()
    private var lastRenderAt=0L
    private var drag:Drag?=null
    private var twoHandBase:TwoHandBase?=null
    private var cursors=emptyList<HandGestureEvent.Cursor>()
    private var handFrame:HandTrackingFrame?=null
    private var foregroundMask:ForegroundMask?=null
    private var foregroundUnavailable=false
    private var seeThrough=false
    private var visibilityAssist=false
    private var virtualRoom=false
    private var detailPage=0
    private var detailPages=1
    private var room:VrGraphics?=null
    private val roomVp=FloatArray(16)
    private var renderModel=lesson?.current?.renderModel?:if(modelId=="extinguisher")"fire" else "anatomy"
    private var elapsed=0f
    private val animatedParts=mutableMapOf<String,FloatArray>()
    private val equipmentPicks=mutableMapOf<String,AnatomyPart>()
    private var inspectionText:String?=null
    private var inspectionLanguage="en-IN"
    private fun activeTeachingKey()=if(lesson!=null)lesson.current.teachingKey else teachingKey
    private fun activePartId()=if(lesson!=null)lesson.current.partId else partId
    private var lastProcessedCapture=-1L
    private var trackingMessage="Move the phone slowly over a textured table or floor."
    private var handMessage="Preparing hand controls…"
    private var latestNotice="Point with your index finger. Pinch to pick up a virtual object."
    private var statusKey=""
    private var hudKey=""
    private var hud=Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888)
    private val pen=Paint(Paint.ANTI_ALIAS_FLAG)
    private var panel=0
    private var paused=false
    private var handEnabled=true
    private var selectedControl=0
    private var lastHudAt=0L
    private var frameReady=false
    private var validAt=0L
    private var surfaceWidth=1;private var surfaceHeight=1;private var logicalWidth=1
    private var renderWidth=1;private var renderHeight=1
    private var fbo=0;private var depth=0;private var stereoTexture=0;private var cardboard=0L
    private var installRequested=false
    @Volatile private var foreground=false
    @Volatile private var hasFocus=true
    private var running=false
    private var closed=false
    private var failed=false
    private var canPlace=false
    private var cooling=false
    private val power=host.getSystemService(PowerManager::class.java)
    private var thermal=PowerManager.THERMAL_STATUS_NONE
    private var thermalAt=0L
    private var touchDownAt=0L;private var touchX=0f;private var touchY=0f
    private var touchMoved=false;@Volatile private var touchActive=false;private var touchSpan=0f;private var touchAngle=0f
    private var touchTransform:TwoHandBase?=null
    @Volatile private var controllerArmed=false;private var controllerDownAt=0L
    private var currentFrame:Frame?=null
    private val projection=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val inverseVp=FloatArray(16)
    private val model=FloatArray(16);private val inverseModel=FloatArray(16);private val mvp=FloatArray(16)
    private val eye=FloatArray(3)
    private var firePick:AnatomyPart?=null
    private var visibleParts=emptyList<AnatomyPart>()
    private var partTransforms=emptyMap<String,FloatArray>()
    var onStatus:(String)->Unit={}
    var onExit:()->Unit={}
    var onFailure:(String)->Unit={}
    var onInstruction:(String,String)->Unit={_,_->}
    var onStepChanged:(String)->Unit={}
    val isPaused get()=paused
    private data class TwoHandBase(val pose:HandModelPose,val pivotX:Float,val pivotZ:Float)
    private data class Drag(val before:HandModelPose,val plane:HandDragPlane,val grabX:Float,val grabZ:Float,val handId:String?=null)
    private data class Ray(val origin:FloatArray,val direction:FloatArray)
    private data class Hit(val id:String,val bounds:RectF)
    private fun commands():List<String> = when {
        paused -> listOf("resume","exit")
        panel==1 -> listOf("smaller","larger","rotate-right","options","tools")
        panel==2 -> listOf("extinguisher","aed","body","hands","tools")
        panel==3 -> listOf("view","details","voice","reset","tools")
        panel==4 -> listOf("detail-back","detail-next","voice","tools")
        lesson!=null -> listOf("previous","next","place","visibility","tools","pause")
        else -> listOf("place","models","visibility","tools","pause","exit")
    }
    private fun commandName(id:String)=when(id){"place"->if(anchor==null)"Place" else "Re-place";"models"->"Models";"tools"->if(panel>0)"Done" else "Controls";"options"->"Options";"smaller"->"− Size";"larger"->"+ Size";"rotate-left"->"Turn left";"rotate-right"->"Turn 15°";"reset"->"Reset";"resume"->if(cooling)"Cooling" else "Resume";"exit"->"Exit";"pause"->"Pause";"extinguisher"->"Extinguisher";"aed"->"AED trainer";"body"->if(activePartId()!=null)"Body / part" else "Body";"hands"->if(handEnabled)"Hands on" else "Hands off";"visibility"->if(seeThrough)"Solid view"else"See-through";"view"->if(virtualRoom)"Camera view"else"Virtual room";"details"->"Step details";"voice"->"Read aloud";"previous","detail-back"->"Previous";"next","detail-next"->"Next";else->id}
    private fun hits():List<Hit> {
        val ids=commands();val margin=.035f;val gap=.012f;val w=(1f-margin*2-gap*(ids.size-1))/ids.size
        val h=(host.dp(54).toFloat()/surfaceHeight).coerceIn(.065f,.14f)
        return ids.mapIndexed{i,id->Hit("command:$id",RectF(margin+i*(w+gap),.965f-h,margin+i*(w+gap)+w,.965f))}
    }
    init {
        setBackgroundColor(Color.BLACK)
        surface.setEGLContextClientVersion(2);surface.setEGLConfigChooser(8,8,8,8,24,0);surface.preserveEGLContextOnPause=true
        surface.setRenderer(this);surface.renderMode=GLSurfaceView.RENDERMODE_CONTINUOUSLY;surface.onPause()
        surface.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(surface,LayoutParams(-1,-1));surface.setOnTouchListener{_,event->touch(event)}
        if(!headset){addView(accessibleControls,LayoutParams(-1,-1));accessibleControls.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;updateAccessibleControls()}
        worker.execute{try{val data=loadBody();if(!closed)pendingBody.set(data)}catch(e:Exception){bodyLoadError="The body model is unavailable. Extinguisher and AED exploration still work."}}
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);positionAccessibleControls()}
    private fun commandEnabled(id:String)=when(id){
        "place"->canPlace&&!paused&&!cooling
        "resume"->!cooling
        "smaller"->inputReady()&&scale>.3001f
        "larger"->inputReady()&&scale<1.4999f
        "rotate-left","rotate-right","reset"->inputReady()
        "body"->body!=null
        "previous"->lesson?.hasPrevious==true
        "next"->lesson?.hasNext==true
        "detail-back"->detailPage>0
        "detail-next"->detailPage<detailPages-1
        else->true
    }
    private fun updateAccessibleControls(){
        if(headset)return
        val ids=commands();val names=ids.map(::commandName);val enabled=ids.map(::commandEnabled)
        val key=ids.joinToString()+names.joinToString()+enabled.joinToString()+"/$surfaceWidth/$surfaceHeight"
        if(key==accessibleControlKey)return
        accessibleControlKey=key
        post{
            if(closed||headset)return@post
            if(accessibleControlIds!=ids){
                accessibleControlIds=ids.toList();accessibleControls.removeAllViews()
                for(id in ids){
                    val button=Button(host).apply{
                        text="";tag="hands-ar-control-$id";isAllCaps=false
                        minimumHeight=0;minimumWidth=0;setPadding(0,0,0,0)
                        background=android.graphics.drawable.StateListDrawable().apply{
                            addState(intArrayOf(android.R.attr.state_focused),host.shape(Color.TRANSPARENT,6,Palette.teal))
                            addState(intArrayOf(),android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                        }
                        setOnClickListener{command(id)}
                    }
                    accessibleControls.addView(button,LayoutParams(1,1))
                }
            }
            positionAccessibleControls()
            for(i in ids.indices){accessibleControls.getChildAt(i).apply{contentDescription=names[i];isEnabled=enabled[i]}}
        }
    }
    private fun positionAccessibleControls(){
        if(headset||width<=0||height<=0)return
        val ids=accessibleControlIds
        val rectangles=hits()
        for(i in 0 until accessibleControls.childCount){
            val child=accessibleControls.getChildAt(i);val r=rectangles.getOrNull(i)?.bounds?:continue
            child.layoutParams=LayoutParams((width*r.width()).toInt().coerceAtLeast(1),(height*r.height()).toInt().coerceAtLeast(1)).apply{leftMargin=(width*r.left).toInt();topMargin=(height*r.top).toInt()}
        }
    }
    private fun loadBody():AnatomyData {
        val context=host.applicationContext
        val base=VrAssets.loadBody(context)
        val requested=partId?:return base
        if(base.parts.any{it.id==requested})return base
        val manifest=JSONObject(context.assets.open("anatomy/manifest.json").bufferedReader().use{it.readText()})
        val entry=manifest.getJSONArray("parts").objects().firstOrNull{it.getString("id")==requested}
        if(entry==null){bodyLoadError="That anatomical structure is unavailable. The whole body is shown instead.";return base}
        val bytes=context.assets.open(entry.getString("file")).use{PackIntegrity.readBounded(it,entry.getInt("bytes"))}
        check(bytes.size==entry.getInt("bytes"))
        check(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==entry.getString("sha256"))
        val used=base.parts.sumOf{it.vertices.size.toLong()*4+8}
        val mesh=AnatomyMeshDecoder.decode(bytes,36_000_000-used)
        fun values(key:String)=entry.getJSONArray(key).let{a->FloatArray(a.length()){a.getDouble(it).toFloat()}}
        val part=AnatomyClothing.protectedSkin(AnatomyPart(requested,entry.getString("name"),entry.getString("layer"),entry.getString("explanation"),values("bounds"),values("colour"),mesh.vertices))
        return AnatomyData(base.manifest,base.parts+part)
    }
    fun resume() {
        if(closed||running)return
        foreground=true
        if(release.failed){fail("The previous camera could not close. Restart the app before retrying.");return}
        if(release.busy){release.awaitAvailable{if(foreground&&!closed)resume()};status("Releasing the previous camera…");return}
        if(host.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){status("Allow camera access to see the room and use hand controls.");return}
        try {
            if(ArCoreApk.getInstance().requestInstall(host,!installRequested)==ArCoreApk.InstallStatus.INSTALL_REQUESTED){installRequested=true;status("Complete the AR Services installation, then return.");return}
            if(driver==null)driver=NativeArDriver(host)
            driver!!.resume();tracker.setEnabled(handEnabled);running=true;failed=false
            surface.onResume();keepScreenOn=true
        }catch(e:Exception){fail(ArCameraSupport.startupMessage(e,false))}
    }
    fun pause() {
        foreground=false;release.cancelPendingResume();tracker.setEnabled(false);keepScreenOn=false
        if(running){
            // The GL owner finishes before Session.pause. An ordinary foreground interruption keeps
            // its Session and anchors, while cancelling uncommitted manipulation.
            surface.queueEvent{cancelManipulation();destroyStereo();currentFrame=null;frameReady=false}
            surface.onPause();running=false;runCatching{driver?.pause()}
        }
        controllerArmed=false;touchActive=false
    }
    fun close() {
        if(closed)return
        pause();closed=true;tracker.close();worker.shutdownNow();pendingBody.set(null)
        anchor?.detach();anchor=null
        val previous=driver;driver=null
        if(previous!=null)release.retire(previous){}
        removeView(surface);hud.recycle()
    }
    fun focus(value:Boolean){hasFocus=value;if(!value){controllerArmed=false;touchActive=false;surface.queueEvent{cancelManipulation()}}}
    fun command(value:String){if(closed)return;surface.queueEvent{activate(value)}}
    fun back(){if(!running){post{onExit()};return};surface.queueEvent{when{panel>0->{panel=0;cancelManipulation();hudKey=""};paused->post{onExit()};else->{paused=true;cancelManipulation();latestNotice="Paused. Resume here or leave the workspace.";hudKey=""}}}}
    fun buttonDown(){if(!foreground||!hasFocus)return;controllerArmed=true;controllerDownAt=SystemClock.elapsedRealtime()}
    fun buttonUp(cancelled:Boolean){
        val armed=controllerArmed;controllerArmed=false
        if(!armed||cancelled||!foreground||!hasFocus)return
        val held=SystemClock.elapsedRealtime()-controllerDownAt
        surface.queueEvent {
            cancelManipulation()
            if(held>=700)activate(commands()[selectedControl.coerceIn(0,commands().lastIndex)])
            else{selectedControl=(selectedControl+1)%commands().size;hudKey=""}
        }
    }
    private fun fail(message:String){if(closed)return;failed=true;tracker.setEnabled(false);post{if(!closed){pause();onFailure(message)}}}
    private fun status(message:String){if(message==statusKey)return;statusKey=message;post{if(!closed)onStatus(message)}}
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        try {
            graphics=HandsArGraphics().also{it.create()};equipment=WorldEquipment().also{it.create()};room=VrGraphics().also{it.create()}
            anatomy=AnatomyRenderer(body?:AnatomyData(JSONObject().put("heightMetres",1.8),emptyList()),AnatomyClothing.parts+FirstAidEquipment.parts+FirstAidTeachingProps.parts).also{it.create()}
            driver?.invalidateTexture();fbo=0;depth=0;stereoTexture=0;cardboard=0L;hudKey=""
            val mesh=equipment.exportMesh("fire")!!;val vertices=FloatArray(mesh.capacity()/12*6);val source=mesh.duplicate().apply{clear()}
            for(i in vertices.indices step 6){for(j in 0..5)vertices[i+j]=source.get(i/6*12+j)}
            val b=FloatArray(6){i->if(i<3)(vertices.indices step 6).minOf{vertices[it+i]}else(vertices.indices step 6).maxOf{vertices[it+i-3]}}
            firePick=AnatomyPart("extinguisher","Extinguisher","equipment","Generic training model",b,floatArrayOf(.8f,.1f,.1f),vertices)
            equipmentPicks.clear();equipmentPicks["fire"]=firePick!!
            lesson?.let{modelId=it.current.modelId;renderModel=it.current.renderModel}
        }catch(e:Exception){fail("The live AR renderer could not start. Return and retry.")}
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int){
        if(width<2||height<2)return
        try {
            surfaceWidth=width;surfaceHeight=height;logicalWidth=if(headset)width/2 else width
            val hudHeight=(800f*height/logicalWidth).toInt().coerceIn(640,3200)
            if(hud.height!=hudHeight){hud.recycle();hud=Bitmap.createBitmap(800,hudHeight,Bitmap.Config.ARGB_8888)}
            cancelManipulation();hudKey="";frameReady=false
            if(headset){destroyStereo();cardboard=CardboardBridge.create(width,height);allocateStereo()}
        }catch(e:Exception){fail("The viewer profile could not load. Remove the phone and check its calibration.")}
    }
    private fun allocateStereo(){
        val factor=minOf(1f,1600f/surfaceWidth,900f/surfaceHeight)
        renderWidth=(logicalWidth*factor).toInt().coerceAtLeast(2);renderHeight=(surfaceHeight*factor).toInt().coerceAtLeast(2)
        val names=IntArray(1);G.glGenTextures(1,names,0);stereoTexture=names[0];G.glBindTexture(G.GL_TEXTURE_2D,stereoTexture)
        for(p in intArrayOf(G.GL_TEXTURE_MIN_FILTER,G.GL_TEXTURE_MAG_FILTER))G.glTexParameteri(G.GL_TEXTURE_2D,p,G.GL_LINEAR)
        for(p in intArrayOf(G.GL_TEXTURE_WRAP_S,G.GL_TEXTURE_WRAP_T))G.glTexParameteri(G.GL_TEXTURE_2D,p,G.GL_CLAMP_TO_EDGE)
        G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_RGB,renderWidth,renderHeight,0,G.GL_RGB,G.GL_UNSIGNED_BYTE,null)
        G.glGenRenderbuffers(1,names,0);depth=names[0];G.glBindRenderbuffer(G.GL_RENDERBUFFER,depth);G.glRenderbufferStorage(G.GL_RENDERBUFFER,G.GL_DEPTH_COMPONENT16,renderWidth,renderHeight)
        G.glGenFramebuffers(1,names,0);fbo=names[0];G.glBindFramebuffer(G.GL_FRAMEBUFFER,fbo)
        G.glFramebufferTexture2D(G.GL_FRAMEBUFFER,G.GL_COLOR_ATTACHMENT0,G.GL_TEXTURE_2D,stereoTexture,0);G.glFramebufferRenderbuffer(G.GL_FRAMEBUFFER,G.GL_DEPTH_ATTACHMENT,G.GL_RENDERBUFFER,depth)
        check(G.glCheckFramebufferStatus(G.GL_FRAMEBUFFER)==G.GL_FRAMEBUFFER_COMPLETE);G.glBindFramebuffer(G.GL_FRAMEBUFFER,0)
        // ARCore is the sole camera/pose owner. The Cardboard head tracker is never resumed here.
    }
    private fun destroyStereo(){
        if(cardboard!=0L){CardboardBridge.destroy(cardboard);cardboard=0L}
        if(fbo!=0)G.glDeleteFramebuffers(1,intArrayOf(fbo),0)
        if(depth!=0)G.glDeleteRenderbuffers(1,intArrayOf(depth),0)
        if(stereoTexture!=0)G.glDeleteTextures(1,intArrayOf(stereoTexture),0)
        fbo=0;depth=0;stereoTexture=0
    }
    override fun onDrawFrame(gl:GL10?){
        if(!foreground||failed||!running||!hasFocus)return
        val d=driver?:return
        try {
            if(headset&&cardboard==0L){cardboard=CardboardBridge.create(surfaceWidth,surfaceHeight);allocateStereo()}
            val now=SystemClock.elapsedRealtime()
            if(now-thermalAt>=1000){thermal=power.currentThermalStatus;thermalAt=now}
            if(thermal>=PowerManager.THERMAL_STATUS_CRITICAL){fail("The phone is too warm for live AR. Remove it from the viewer and let it cool before retrying.");return}
            cooling=thermal>=PowerManager.THERMAL_STATUS_SEVERE
            if(cooling){paused=true;latestNotice="Phone is warm. Let it cool before resuming."}
            d.setCameraTextureName(graphics.cameraTexture)
            @Suppress("DEPRECATION") val rotation=host.windowManager.defaultDisplay.rotation
            d.setDisplayGeometry(rotation,logicalWidth,surfaceHeight)
            val frame=d.update();currentFrame=frame
            val camera=d.currentCamera?:return
            camera.getProjectionMatrix(projection,0,.05f,30f);camera.getViewMatrix(view,0);Matrix.multiplyMM(vp,0,projection,0,view,0)
            frameReady=camera.trackingState==TrackingState.TRACKING&&d.snapshot().ready&&Matrix.invertM(inverseVp,0,vp,0)
            if(frameReady)validAt=now
            val pose=camera.pose;eye[0]=pose.tx();eye[1]=pose.ty();eye[2]=pose.tz()
            // A normal pause keeps safe HUD hand controls available, but targetAt excludes models.
            // Heat and lost AR tracking freeze every camera-derived interaction.
            val interaction=frameReady&&!cooling&&hasFocus
            tracker.offer(frame,rotation,logicalWidth,surfaceHeight,interaction,d.sessionEpoch)
            val snapshot=tracker.latestSnapshot(now)
            handMessage=snapshot.message
            foregroundMask=snapshot.foreground?.takeIf{interaction&&it.freshAt(now)}
            foregroundUnavailable=snapshot.foregroundUnavailable
            trackingMessage=when{paused&&cooling->"Paused · cooling";paused->"Paused";!frameReady->"Mapping paused · use a well-lit textured surface";anchor==null->"Map a table or floor, then Place";anchor?.trackingState!=TrackingState.TRACKING->"Anchor paused · keep the surface in view";else->"${commandName(modelId)} · ${"%.0f".format(scale*100)}% · anchored"}
            pendingBody.getAndSet(null)?.let{loaded->body=loaded;anatomy?.release();anatomy=AnatomyRenderer(loaded,AnatomyClothing.parts+FirstAidEquipment.parts+FirstAidTeachingProps.parts).also{it.create()}}
            updateModel()
            if(!interaction){cancelManipulation();cursors=emptyList();handFrame=null}
            else{
                val next=snapshot.frame
                if(!touchActive&&!controllerArmed){
                    if(next!=null&&next.captureTimeMs!=lastProcessedCapture){lastProcessedCapture=next.captureTimeMs;handFrame=next;consume(gestures.process(next,now,::targetAt))}
                    consume(gestures.tick(now))
                }
                if(handFrame?.let{now-it.captureTimeMs>HandGestureController.CAPTURE_FRESH_MS}!=false){handFrame=null;cursors=emptyList()}
            }
            val visibleHands=handFrame?.hands.orEmpty()
            visibilityAssist=seeThrough||(visibleHands.isNotEmpty()&&foregroundMask?.covers(visibleHands)!=true)
            val dt=if(lastRenderAt==0L)0f else (now-lastRenderAt).coerceAtLeast(0)/1000f
            if(!paused&&!cooling)elapsed+=dt.coerceAtMost(.1f)
            lastRenderAt=now;motion.advance(transform(),dt);updateModel()
            canPlace=interaction&&(virtualRoom||floorHit(frame,.5f,.62f)!=null)
            if(now-lastHudAt>=100||lastHudAt==0L){paintHud();lastHudAt=now}
            if(headset){
                G.glBindFramebuffer(G.GL_FRAMEBUFFER,fbo);G.glClearColor(0f,0f,0f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT or G.GL_DEPTH_BUFFER_BIT)
                // Share one monoscopic render at the existing per-eye resolution. VR still
                // renders separate eye images; this never invents stereo camera depth.
                G.glViewport(0,0,renderWidth,renderHeight);drawMono(frame)
                G.glBindFramebuffer(G.GL_FRAMEBUFFER,0);G.glViewport(0,0,surfaceWidth,surfaceHeight);G.glDisable(G.GL_DEPTH_TEST);G.glDisable(G.GL_BLEND)
                CardboardBridge.composite(cardboard,surfaceWidth,surfaceHeight,stereoTexture,sharedCameraImage=true)
            }else{G.glBindFramebuffer(G.GL_FRAMEBUFFER,0);G.glViewport(0,0,surfaceWidth,surfaceHeight);G.glClearColor(0f,0f,0f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT or G.GL_DEPTH_BUFFER_BIT);drawMono(frame)}
            updateAccessibleControls()
            status("$trackingMessage. $handMessage")
        }catch(e:Exception){fail("Live AR was interrupted. Return and retry; no learning records were changed.")}
    }
    private fun updateModel(){
        val a=anchor
        if(a==null||a.trackingState!=TrackingState.TRACKING){visibleParts=emptyList();return}
        val visible=motion.visible
        a.pose.toMatrix(model,0);Matrix.translateM(model,0,visible.x,0f,visible.z);Matrix.rotateM(model,0,visible.yaw,0f,1f,0f)
        partTransforms=emptyMap()
        val selectedPartId=activePartId()
        val guide=if(modelId=="body"&&selectedPartId==null)FirstAidSceneGuide.byKey(activeTeachingKey())else null
        val parts=when(modelId){"aed"->FirstAidEquipment.parts;"body"->body?.parts?.let{all->
            val selected=selectedPartId?.let{id->all.filter{it.id==id}}?.takeIf{it.isNotEmpty()}
            if(selected!=null)selected+AnatomyClothing.shownWith(selected)else all.filter{it.layer=="skin"}+AnatomyClothing.parts
        }.orEmpty();else->listOfNotNull(equipmentPick(renderModel))}+
            (if(guide?.key?.startsWith("adult-collapse-response/")==true)FirstAidEquipment.parts else emptyList())+
            (guide?.let{FirstAidTeachingProps.visible(it)}?:emptyList())
        visibleParts=parts
        if(parts.isEmpty())return
        val b=FloatArray(6){i->if(i<3)parts.minOf{it.bounds[i]}else parts.maxOf{it.bounds[i]}}
        val baseScale=if(modelId=="body"&&selectedPartId!=null)(.65f/(0..2).maxOf{b[it+3]-b[it]}.coerceAtLeast(.03f)).coerceAtMost(5f)else if(modelId=="body").48f else 1f
        Matrix.scaleM(model,0,visible.scale*baseScale,visible.scale*baseScale,visible.scale*baseScale)
        if(guide!=null){
            val skin=body?.parts?.firstOrNull{it.layer=="skin"}?.bounds?:b
            val lift=CasualtyGeometry.lift(skin,0f);val posed=CasualtyGeometry.bounds(skin,0f,lift)
            Matrix.translateM(model,0,-(posed[0]+posed[3])/2,lift,-(posed[2]+posed[5])/2);Matrix.rotateM(model,0,-90f,1f,0f,0f)
            val placement=body?.let{FirstAidCprPlacement.forParts(it.parts)}
            partTransforms=parts.filter{it.id.startsWith("teaching:")}.associate{part->
                val t=animatedParts.getOrPut(part.id){FloatArray(16)};Matrix.setIdentityM(t,0)
                val offset=FirstAidTeachingProps.offset(part.id,elapsed,guide);Matrix.translateM(t,0,offset[0],offset[1],offset[2])
                if(FirstAidCprPlacement.usesLandmark(part.id))placement?.translation?.let{p->Matrix.translateM(t,0,p[0],p[1],p[2])}
                part.id to t
            }
        }else Matrix.translateM(model,0,-(b[0]+b[3])/2,-b[1],-(b[2]+b[5])/2)
        Matrix.invertM(inverseModel,0,model,0);Matrix.multiplyMM(mvp,0,vp,0,model,0)
    }
    private fun drawMono(frame:Frame){
        graphics.camera(frame,draw=!virtualRoom)
        G.glClear(G.GL_DEPTH_BUFFER_BIT)
        if(virtualRoom){
            G.glClearColor(.13f,.20f,.23f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT)
            anchor?.takeIf{it.trackingState==TrackingState.TRACKING}?.let{a->
                val transform=FloatArray(16);a.pose.toMatrix(transform,0);Matrix.multiplyMM(roomVp,0,vp,0,transform,0)
                G.glEnable(G.GL_DEPTH_TEST)
                room?.plane(roomVp,0f,-.02f,0f,2.5f,2f,colour=floatArrayOf(.65f,.72f,.72f,1f),tilt=-90f)
                room?.plane(roomVp,0f,.6f,-1.1f,3.5f,1.5f,colour=floatArrayOf(.27f,.39f,.42f,1f))
            }
        }
        if(anchor?.trackingState==TrackingState.TRACKING&&frameReady&&!cooling){
            if(modelId=="extinguisher")equipment.draw(vp,model,renderModel,eye,clearDepth=false)
            else{
                val film=visibleParts.filter{it.id=="teaching:film"||it.id.startsWith("teaching:film-")}
                anatomy?.draw(mvp,model,visibleParts-film.toSet(),null,partTransforms=partTransforms,cameraPosition=eye)
                if(film.isNotEmpty())anatomy?.draw(mvp,model,film,null,.44f,partTransforms=partTransforms,cameraPosition=eye)
            }
        }
        graphics.foreground(foregroundMask,visibilityAssist)
        G.glDisable(G.GL_DEPTH_TEST);graphics.marks(cursors,handFrame?.hands.orEmpty(),logicalWidth,surfaceHeight);graphics.hud()
    }
    private fun equipmentPick(id:String):AnatomyPart?=equipmentPicks[id]?:equipment.exportMesh(id)?.let{mesh->
        val vertices=FloatArray(mesh.capacity()/12*6);val source=mesh.duplicate().apply{clear()}
        for(i in vertices.indices step 6)for(j in 0..5)vertices[i+j]=source.get(i/6*12+j)
        val b=FloatArray(6){i->if(i<3)(vertices.indices step 6).minOf{vertices[it+i]}else(vertices.indices step 6).maxOf{vertices[it+i-3]}}
        AnatomyPart(id,id,"equipment","Authored training illustration",b,floatArrayOf(.8f,.1f,.1f),vertices).also{equipmentPicks[id]=it}
    }
    private fun transform()=HandModelPose(offsetX,offsetZ,scale,yaw)
    private fun apply(t:HandModelPose,immediate:Boolean=false){
        val value=t.bounded();offsetX=value.x;offsetZ=value.z;scale=value.scale;yaw=value.yaw
        if(immediate)motion.snap(value)
        updateModel();hudKey=""
    }
    private fun cancelManipulation(){
        drag?.let{apply(it.before,true)};drag=null;twoHandBase=null
        gestures.interrupt();cursors=emptyList();touchTransform=null
    }
    private fun consume(events:List<HandGestureEvent>){
        val next=mutableListOf<HandGestureEvent.Cursor>()
        for(event in events)when(event){
            is HandGestureEvent.Cursor->next+=event
            is HandGestureEvent.Start->{
                if(event.targetId.startsWith("command:")){/* Activate once on release, so an open palm can cancel. */}
                else if(event.targetId=="object")beginDrag(event.x,event.y,event.handId)
                else if(event.targetId=="surface"){latestNotice="Release the pinch to place here. Open your palm to cancel."}
            }
            is HandGestureEvent.Move->if(event.targetId=="object")moveDrag(event.x,event.y)
            is HandGestureEvent.End->{
                if(event.targetId.startsWith("command:")&&!event.dragged&&targetAt(event.x,event.y)==event.targetId)activate(event.targetId.removePrefix("command:"))
                else if(event.targetId=="object"&&drag!=null){committed=transform();drag=null;twoHandBase=null;latestNotice="Object placed. Pinch again to move it.";if(!event.dragged)inspectAt(event.x,event.y)}
                else if(event.targetId=="surface")place(event.x,event.y)
            }
            is HandGestureEvent.Cancel->{drag?.let{apply(it.before,true)};drag=null;twoHandBase=null;latestNotice="Movement cancelled. Show a neutral hand to start again."}
            is HandGestureEvent.TransformStart->if(event.targetId=="object"){
                val point=drag?.let{planePoint(event.centreX,event.centreY,it.plane)}
                val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)}
                twoHandBase=local?.let{TwoHandBase(transform(),it[0],it[2])}
                latestNotice="Two hands hold the model. Move together; spread to resize; turn to rotate."
            }
            is HandGestureEvent.TransformMove->if(event.targetId=="object"&&inputReady())twoHandBase?.let{base->
                val point=drag?.let{planePoint(event.centreX,event.centreY,it.plane)}
                val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)}
                if(local!=null)apply(HandModelTransforms.twoHand(base.pose,base.pivotX,base.pivotZ,local[0],local[2],event.scale,-Math.toDegrees(event.rotationRadians.toDouble()).toFloat()))
            }
            is HandGestureEvent.TransformEnd->{
                if(event.cancelled)drag?.let{apply(it.before,true)}
                else drag?.let{held->
                    // The primary pinch still owns the transaction. Rebase its grab offset after
                    // a two-hand resize, preserving the original rollback snapshot until End.
                    val cursor=next.firstOrNull{it.handId==held.handId}
                    val point=cursor?.let{planePoint(it.x,it.y,held.plane)}
                    val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)}
                    if(local!=null)drag=held.copy(grabX=local[0]-offsetX,grabZ=local[2]-offsetZ)
                    latestNotice="Keep pinching to move. Release your primary pinch to place."
                }
                twoHandBase=null
            }
        }
        if(next.isNotEmpty())cursors=next
    }
    private fun inputReady()=foreground&&hasFocus&&!paused&&!cooling&&frameReady&&SystemClock.elapsedRealtime()-validAt in 0..150&&anchor?.trackingState==TrackingState.TRACKING
    private fun targetAt(x:Float,y:Float):String? {
        if(!x.isFinite()||!y.isFinite()||x !in 0f..1f||y !in 0f..1f)return null
        hits().firstOrNull{it.bounds.contains(x,y)}?.let{return it.id}
        if(panel==4)return null
        if(!frameReady||paused||cooling)return null
        if(anchor==null)return if(virtualRoom||currentFrame?.let{floorHit(it,x,y)}!=null)"surface"else null
        if(!inputReady())return null
        val ray=ray(x,y)?:return null
        return if(objectDistance(ray)!=null)"object"else null
    }
    private fun objectDistance(ray:Ray):Float? {
        return objectHit(ray)?.second
    }
    private fun objectHit(ray:Ray):Pair<AnatomyPart,Float>? {
        val o=FloatArray(4);val direction=FloatArray(4)
        Matrix.multiplyMV(o,0,inverseModel,0,ray.origin+1f,0);Matrix.multiplyMV(direction,0,inverseModel,0,ray.direction+0f,0)
        return visibleParts.mapNotNull{part->
            val t=partTransforms[part.id]
            val point=if(t==null)o.copyOf(3)else FloatArray(3){o[it]-t[12+it]}
            // Preserve the unnormalised local direction: its triangle-hit parameter is
            // affine invariant, so this remains the original world ray's distance.
            AnatomyPicking.distance(part.vertices,part.bounds,point,direction.copyOf(3))?.takeIf{it in .05f..4f}?.let{part to it}
        }.minByOrNull{it.second}
    }
    private fun inspectAt(x:Float,y:Float){
        val ray=ray(x,y)?:return;val part=objectHit(ray)?.first?:return
        inspectionText=when{
            modelId=="body"&&part.id.startsWith("FMA")->AnatomyKnowledge.info(part.id,if(hindi)"hi"else"en")?.let{"${it.title}\n\n${it.description}"}?:"${part.name}\n\n${part.explanation}"
            modelId=="extinguisher"->{
                val local=FloatArray(4)
                val hit=objectHit(ray)?:return
                val point=FloatArray(4){i->if(i==3)1f else ray.origin[i]+ray.direction[i]*hit.second}
                Matrix.multiplyMV(local,0,inverseModel,0,point,0)
                val components=if(renderModel in setOf("fire","fire-damaged","fire-pin-removed","fire-aimed","fire-discharging"))SpatialCatalog(host.applicationContext).parts("fire")else emptyList()
                val declared=components.map{component->val p=component.getJSONArray("point");component to (0..2).sumOf{axis->val d=local[axis]-p.getDouble(axis);d*d}}
                    .filter{it.second<=.075*.075}.minByOrNull{it.second}?.first
                if(declared!=null)declared.getString("name")+"\n\n"+declared.getString("description")
                else lesson?.current?.detailText?:"Fire extinguisher\n\nInspect the instruction label, pin, gauge and hose. Use the guided Fire Safety course to rehearse the correct sequence; this generic illustration does not establish the suitability of real equipment."
            }
            part.id.startsWith("teaching:")->FirstAidSceneGuide.byKey(activeTeachingKey())?.caption(hindi)?:part.explanation
            else->part.name+"\n\n"+part.explanation
        }
        inspectionLanguage=if(hindi&&((part.id.startsWith("FMA")&&AnatomyKnowledge.info(part.id,"hi")!=null)||(part.id.startsWith("teaching:")&&FirstAidSceneGuide.byKey(activeTeachingKey())!=null)))"hi-IN"else"en-IN"
        panel=4;detailPage=0;hudKey="";val detail=inspectionText!!;val language=inspectionLanguage;post{onInstruction(detail,language)}
    }
    private fun ray(x:Float,y:Float):Ray?{
        if(!frameReady)return null
        val a=FloatArray(4);val b=FloatArray(4)
        Matrix.multiplyMV(a,0,inverseVp,0,floatArrayOf(x*2-1,1-y*2,-1f,1f),0);Matrix.multiplyMV(b,0,inverseVp,0,floatArrayOf(x*2-1,1-y*2,1f,1f),0)
        if(abs(a[3])<1e-7||abs(b[3])<1e-7)return null
        val origin=FloatArray(3){a[it]/a[3]};val v=FloatArray(3){b[it]/b[3]-origin[it]};val l=sqrt(v.sumOf{(it*it).toDouble()}).toFloat()
        return if(l.isFinite()&&l>1e-7f)Ray(origin,FloatArray(3){v[it]/l})else null
    }
    private fun planePoint(x:Float,y:Float,plane:HandDragPlane):FloatArray?{
        val r=ray(x,y)?:return null
        return plane.intersect(r.origin,r.direction)
    }
    private fun beginDrag(x:Float,y:Float,handId:String?=null){
        if(!inputReady())return
        val a=anchor?:return;val ray=ray(x,y)?:return;val distance=objectDistance(ray)?:return
        val grip=FloatArray(3){ray.origin[it]+ray.direction[it]*distance}
        val plane=HandDragPlane.atGrip(grip,ray.direction)
        val local=a.pose.inverse().transformPoint(grip)
        inspectionText=null
        drag=Drag(transform(),plane,local[0]-offsetX,local[2]-offsetZ,handId);latestNotice="Holding object · move one hand; pinch with both hands to resize and turn."
    }
    private fun moveDrag(x:Float,y:Float){
        val held=drag?:return;if(!inputReady()){cancelManipulation();return}
        val p=planePoint(x,y,held.plane)?:return;val local=anchor?.pose?.inverse()?.transformPoint(p)?:return
        apply(transform().copy(x=local[0]-held.grabX,z=local[2]-held.grabZ))
    }
    private fun floorHit(frame:Frame,x:Float,y:Float)=frame.hitTest(x*logicalWidth,y*surfaceHeight).firstOrNull{
        val p=it.trackable as? Plane;p!=null&&p.type==Plane.Type.HORIZONTAL_UPWARD_FACING&&p.trackingState==TrackingState.TRACKING&&p.isPoseInPolygon(it.hitPose)&&it.distance in .25f..3f
    }
    private fun place(x:Float=.5f,y:Float=.62f){
        if(!frameReady||paused||cooling){latestNotice="Map a surface before placing an object.";return}
        val frame=currentFrame?:return;val hit=floorHit(frame,x,y)
        if(hit==null&&!virtualRoom){latestNotice="No mapped surface here. Aim at a textured table or floor.";return}
        try{val replacement=if(hit!=null)hit.createAnchor()else{
            val camera=driver?.currentCamera?.pose?:return
            val forward=camera.rotateVector(floatArrayOf(0f,0f,-1f))
            val length=hypot(forward[0],forward[2]).coerceAtLeast(.1f)
            driver?.anchorAt(com.google.ar.core.Pose.makeTranslation(camera.tx()+forward[0]/length*.9f,camera.ty()-.5f,camera.tz()+forward[2]/length*.9f))?:return
        };val old=anchor;anchor=replacement;old?.detach();apply(HandModelPose(),true);committed=transform();latestNotice=if(virtualRoom)"Virtual practice table placed. Stay seated; it is not a real surface."else"Anchored. Point at the model, then pinch to move it."}
        catch(e:Exception){latestNotice="Placement did not complete. The previous anchor is retained."}
    }
    private fun activate(id:String){
        if(!foreground||!hasFocus||id !in commands()||!commandEnabled(id))return
        cancelManipulation()
        when(id){
            "exit"->post{onExit()}
            "pause"->{paused=true;latestNotice="Paused. Resume here or leave the workspace."}
            "resume"->{if(!cooling){paused=false;latestNotice="Show a neutral hand, then point and pinch."}}
            "place"->place()
            "models"->panel=2
            "tools"->panel=if(panel==0)1 else 0
            "options"->panel=3
            "visibility"->{seeThrough=!seeThrough;latestNotice=if(seeThrough)"See-through assistance on. Camera remains visible through models."else"Foreground mask on. Thin fingers may be missed; use See-through if needed."}
            "view"->{virtualRoom=!virtualRoom;latestNotice=if(virtualRoom)"Virtual practice view. Camera still runs for hand controls. Stay seated."else"Camera view. Virtual objects remain anchored."}
            "details"->{inspectionText=null;panel=4;detailPage=0}
            "detail-back"->{detailPage=(detailPage-1).coerceAtLeast(0)}
            "detail-next"->{detailPage=(detailPage+1).coerceAtMost(detailPages-1)}
            "voice"->post{onInstruction(instructionText(),instructionLanguage())}
            "previous","next"->{
                val changed=if(id=="previous")lesson?.previous()==true else lesson?.next()==true
                if(changed){val step=lesson!!.current;modelId=step.modelId;renderModel=step.renderModel;panel=0;detailPage=0;elapsed=0f;animatedParts.clear();inspectionText=null;apply(HandModelPose(),true);committed=transform();latestNotice=step.cue;post{onStepChanged(step.stepId);onInstruction(step.detailText,"en-IN")}}
            }
            "hands"->{handEnabled=!handEnabled;tracker.setEnabled(handEnabled);latestNotice=if(handEnabled)"Hand controls enabled."else"Hand controls off. Touch or the viewer button still works."}
            "smaller"->if(inputReady())apply(transform().copy(scale=scale/1.15f))
            "larger"->if(inputReady())apply(transform().copy(scale=scale*1.15f))
            "rotate-left"->if(inputReady())apply(transform().copy(yaw=yaw-15f))
            "rotate-right"->if(inputReady())apply(transform().copy(yaw=yaw+15f))
            "reset"->if(inputReady()){apply(HandModelPose(),true);committed=transform();latestNotice="Model size, turn and local position reset. Its surface anchor is retained."}
            in MODELS->{if(id=="body"&&body==null){latestNotice=bodyLoadError?:"Preparing the body model…"}else{modelId=id;renderModel=if(id=="extinguisher")"fire"else"anatomy";apply(HandModelPose(),true);committed=transform();panel=0;latestNotice="${commandName(id)} selected. Its position remains anchored."}}
        }
        selectedControl=selectedControl.coerceAtMost(commands().lastIndex);hudKey=""
        updateAccessibleControls()
    }
    private fun touch(event:MotionEvent):Boolean{
        if(headset){when(event.actionMasked){MotionEvent.ACTION_DOWN->buttonDown();MotionEvent.ACTION_UP->buttonUp(false);MotionEvent.ACTION_CANCEL->buttonUp(true)};return true}
        val x=(event.x/width.coerceAtLeast(1)).coerceIn(0f,1f);val y=(event.y/height.coerceAtLeast(1)).coerceIn(0f,1f)
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{touchActive=true;touchMoved=false;touchDownAt=event.eventTime;touchX=x;touchY=y;surface.queueEvent{cancelManipulation();if(targetAt(x,y)=="object")beginDrag(x,y)}}
            MotionEvent.ACTION_POINTER_DOWN->{if(event.pointerCount==2){
                touchMoved=true;touchSpan=hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));touchAngle=atan2(event.getY(1)-event.getY(0),event.getX(1)-event.getX(0))
                val cx=(event.getX(0)+event.getX(1))/(2*width);val cy=(event.getY(0)+event.getY(1))/(2*height)
                surface.queueEvent{val point=drag?.let{planePoint(cx,cy,it.plane)};val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)};touchTransform=local?.let{TwoHandBase(transform(),it[0],it[2])}}
            }}
            MotionEvent.ACTION_MOVE->{if(!touchActive)return true;if(hypot(x-touchX,y-touchY)>.012f)touchMoved=true
                if(event.pointerCount>=2){
                    val span=hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));val angle=atan2(event.getY(1)-event.getY(0),event.getX(1)-event.getX(0))
                    val ratio=if(touchSpan>1f)span/touchSpan else 1f;val degrees=-Math.toDegrees((angle-touchAngle).toDouble()).toFloat()
                    val cx=(event.getX(0)+event.getX(1))/(2*width);val cy=(event.getY(0)+event.getY(1))/(2*height)
                    surface.queueEvent{if(inputReady())touchTransform?.let{base->
                        val point=drag?.let{planePoint(cx,cy,it.plane)};val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)}
                        if(local!=null)apply(HandModelTransforms.twoHand(base.pose,base.pivotX,base.pivotZ,local[0],local[2],ratio,degrees))
                    }}
                }
                else surface.queueEvent{moveDrag(x,y)}
            }
            MotionEvent.ACTION_POINTER_UP->{
                if(event.pointerCount==2){
                    val remaining=if(event.actionIndex==0)1 else 0
                    val rx=event.getX(remaining)/width;val ry=event.getY(remaining)/height
                    surface.queueEvent{
                        touchTransform=null
                        drag?.let{held->val point=planePoint(rx,ry,held.plane);val local=point?.let{anchor?.pose?.inverse()?.transformPoint(it)}
                            if(local!=null)drag=held.copy(grabX=local[0]-offsetX,grabZ=local[2]-offsetZ)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP->{if(!touchActive)return true;touchActive=false;val moved=touchMoved;surface.queueEvent{
                if(drag!=null){committed=transform();drag=null;touchTransform=null;if(!moved)inspectAt(x,y)}
                else if(!moved){val target=targetAt(x,y);if(target?.startsWith("command:")==true)activate(target.removePrefix("command:"))else if(target=="surface")place(x,y)}
            }}
            MotionEvent.ACTION_CANCEL->{touchActive=false;surface.queueEvent{cancelManipulation()}}
        };return true
    }
    private fun instructionText():String=inspectionText?:lesson?.current?.detailText?:when{
        modelId=="body"&&activePartId()!=null->body?.parts?.firstOrNull{it.id==activePartId()}?.let{part->AnatomyKnowledge.info(part.id,if(hindi)"hi"else"en")?.let{"${it.title}. ${it.description}"}?:"${part.name}. ${part.explanation}"}?:bodyLoadError?:"Preparing the selected anatomical structure…"
        activeTeachingKey()!=null&&modelId=="body"->FirstAidSceneGuide.byKey(activeTeachingKey())?.caption(hindi)?:latestNotice
        else->latestNotice
    }
    private fun instructionLanguage()=if(inspectionText!=null)inspectionLanguage else if(lesson!=null)"en-IN"else if(hindi&&modelId=="body"&&(activeTeachingKey()!=null||activePartId()?.let{AnatomyKnowledge.info(it,"hi")!=null}==true))"hi-IN"else"en-IN"
    private fun paintHud(){
        val text=lesson?.current?.cue?:instructionText()
        val foregroundState=when{visibilityAssist->if(seeThrough)"See-through"else"Visibility assist";foregroundMask!=null->"Foreground on";foregroundUnavailable->"Mask unavailable · use eye control";else->"Waiting for fresh foreground"}
        val key="$trackingMessage|$handMessage|$text|$panel|$selectedControl|$canPlace|$handEnabled|$bodyLoadError|$foregroundState|$virtualRoom|$detailPage"
        if(key==hudKey)return;hudKey=key
        val c=Canvas(hud);c.drawColor(Color.TRANSPARENT,android.graphics.PorterDuff.Mode.CLEAR)
        pen.color=0xf020343d.toInt();c.drawRoundRect(RectF(24f,20f,776f,166f),18f,18f,pen)
        pen.color=Color.WHITE;pen.textSize=29f;pen.typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)
        val heading=lesson?.current?.let{"${it.index+1}/${it.count}  ${it.title}"}?:trackingMessage
        drawLine(c,heading,44f,61f,710f)
        pen.textSize=26f;pen.color=0xffc5e8e1.toInt();drawLine(c,handMessage,44f,100f,710f)
        pen.textSize=22f;pen.color=0xffe0e8ec.toInt();drawLine(c,(if(virtualRoom)"Virtual practice"else if(headset)"Single-camera AR"else"Camera AR")+" · "+foregroundState,44f,139f,710f)
        val barTop=hits().first().bounds.top*hud.height
        val captionBottom=barTop-14f
        val captionTop=if(panel==4)184f else maxOf(190f,captionBottom-180f)
        pen.color=0xf0edf2f4.toInt();c.drawRoundRect(RectF(24f,captionTop,776f,captionBottom),16f,16f,pen)
        pen.color=Palette.ink;pen.textSize=29f
        if(panel==4){
            val lines=wrappedLines(instructionText(),710f)
            val count=((captionBottom-captionTop-60)/38).toInt().coerceAtLeast(1)
            detailPages=((lines.size+count-1)/count).coerceAtLeast(1);detailPage=detailPage.coerceIn(0,detailPages-1)
            lines.drop(detailPage*count).take(count).forEachIndexed{i,line->c.drawText(line,44f,captionTop+41+i*38,pen)}
            pen.textSize=22f;c.drawText("Details ${detailPage+1}/$detailPages · practice only",44f,captionBottom-12,pen)
        }else wrap(c,text,44f,captionTop+41,710f,4)
        for((i,hit)in hits().withIndex()){
            val r=RectF(hit.bounds.left*800,hit.bounds.top*hud.height,hit.bounds.right*800,hit.bounds.bottom*hud.height)
            val selected=headset&&i==selectedControl
            pen.color=if(!commandEnabled(hit.id.removePrefix("command:")))Palette.muted else if(selected)Palette.tealBg else if(hit.id=="command:exit")Palette.danger else Palette.blue;c.drawRoundRect(r,12f,12f,pen)
            pen.color=if(selected)Palette.ink else Color.WHITE;pen.textSize=27f;pen.textAlign=Paint.Align.CENTER
            val name=commandName(hit.id.removePrefix("command:"))
            if(hit.id=="command:visibility"){
                pen.style=Paint.Style.STROKE;pen.strokeWidth=3f
                val cx=r.centerX();val cy=r.centerY();val eye=android.graphics.Path().apply{moveTo(cx-21,cy);quadTo(cx,cy-25,cx+21,cy);quadTo(cx,cy+25,cx-21,cy)}
                c.drawPath(eye,pen);c.drawCircle(cx,cy,7f,pen)
                if(seeThrough)c.drawLine(cx-22,cy+20,cx+22,cy-20,pen)
                pen.style=Paint.Style.FILL
            }else fit(c,name,r.centerX(),r.centerY()+7,r.width()-8)
        }
        pen.textAlign=Paint.Align.LEFT;pen.color=Color.WHITE;pen.textSize=20f
        drawLine(c,if(headset)"Button: tap to move focus · hold to select"else"Point + pinch · open palm cancels",28f,hud.height-8f,744f)
        if(anchor==null&&!paused){pen.color=if(canPlace)0xff8df3d9.toInt()else Color.WHITE;pen.style=Paint.Style.STROKE;pen.strokeWidth=3f;c.drawCircle(400f,hud.height*.5f,15f,pen);pen.style=Paint.Style.FILL}
        graphics.hud(hud)
    }
    private fun fit(c:Canvas,value:String,x:Float,y:Float,max:Float){val old=pen.textSize;while(pen.measureText(value)>max&&pen.textSize>15)pen.textSize-=1;c.drawText(value,x,y,pen);pen.textSize=old}
    private fun drawLine(c:Canvas,value:String,x:Float,y:Float,max:Float){var s=value;while(pen.measureText(s)>max&&s.length>1)s=s.dropLast(1);c.drawText(if(s!=value)s.dropLast(1)+"…"else s,x,y,pen)}
    private fun wrappedLines(value:String,max:Float):List<String>{
        val lines=mutableListOf<String>()
        for(paragraph in value.split('\n')){
            var remaining=paragraph.trim()
            if(remaining.isEmpty()){lines.add("");continue}
            while(remaining.isNotEmpty()){
                val count=pen.breakText(remaining,true,max,null).coerceAtLeast(1)
                val boundary=if(count<remaining.length)remaining.lastIndexOf(' ',count).takeIf{it>0}?:count else count
                lines.add(remaining.take(boundary));remaining=remaining.drop(boundary).trimStart()
            }
        }
        return lines
    }
    private fun wrap(c:Canvas,value:String,x:Float,y:Float,max:Float,lines:Int){val all=wrappedLines(value,max);all.take(lines).forEachIndexed{i,line->c.drawText(if(i==lines-1&&all.size>lines)line.dropLast(2)+"…"else line,x,y+i*36,pen)}}
    private fun normaliseAngle(value:Float):Float {if(!value.isFinite())return 0f;var v=value%360;if(v>180)v-=360;if(v< -180)v+=360;return v}
    companion object {val MODELS=setOf("extinguisher","aed","body")}
}
