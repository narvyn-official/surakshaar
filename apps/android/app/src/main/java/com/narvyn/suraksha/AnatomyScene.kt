package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.*
import android.os.PowerManager
import android.os.SystemClock
import android.view.*
import android.widget.FrameLayout
import com.google.ar.core.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

internal data class AnatomyControls(val layer:String="organs",val selected:String?=null,val hidden:Set<String> = emptySet(),val isolated:Boolean=false,val yaw:Float=0f,val pitch:Float=0f,val distance:Float=2.6f,val panX:Float=0f,val panY:Float=0f,val scale:Float=1f,val edit:Boolean=false,val skinOverlay:Boolean=false,val supine:Boolean=false,val equipment:Boolean=false,val equipmentPowered:Boolean=false,val skinTone:Int=AnatomyMaterials.DEFAULT_SKIN_TONE,val bodyRoll:Float=0f,val cueRegion:String?=null,val casualtyDetail:String?=null,val cueHold:Boolean=false)

/** A single GL owner handles camera frames, placement and mesh picking. Never saved as AR evidence. */
internal class AnatomyScene(private val host:Activity,val data:AnatomyData,val camera:Boolean,private val practice:Boolean=false,private val bay:Boolean=false):FrameLayout(host),GLSurfaceView.Renderer {
    private val surface=GLSurfaceView(host)
    private data class HoldRequest(val serial:Long,val x:Float,val y:Float,val controls:AnatomyControls,val originX:Float=x,val originY:Float=y)
    private data class HoldProgress(val serial:Long,val fraction:Float,val at:Long)
    private val holdRequest=java.util.concurrent.atomic.AtomicReference<HoldRequest?>(null)
    private val holdProgress=java.util.concurrent.atomic.AtomicReference<HoldProgress?>(null)
    private val holdGate=CueHoldGate()
    private var holdSerial=0L
    private fun resetCueDwell(){holdGate.reset();holdProgress.set(null)}
    private fun cancelCueHold(){holdRequest.set(null);holdProgress.set(null);placementGuide.invalidate()}
    private val placementGuide=object:View(host){
        private val pen=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas:android.graphics.Canvas){
            holdRequest.get()?.let{request->
                pen.style=android.graphics.Paint.Style.STROKE;pen.strokeWidth=host.dp(4).toFloat();pen.color=Palette.line
                val r=host.dp(26).toFloat();val progress=holdProgress.get()?.takeIf{it.serial==request.serial}?.fraction?:0f
                canvas.drawCircle(request.x,request.y,r,pen);pen.color=Palette.blue
                canvas.drawArc(android.graphics.RectF(request.x-r,request.y-r,request.x+r,request.y+r),-90f,360f*progress,false,pen)
                surface.requestRender();postInvalidateOnAnimation()
            }
            if(!camera||placed)return
            pen.style=android.graphics.Paint.Style.STROKE;pen.strokeWidth=host.dp(2).toFloat();pen.color=if(canPlace)Palette.blue else android.graphics.Color.WHITE
            val x=width*.5f;val y=height*.62f
            canvas.drawCircle(x,y,host.dp(14).toFloat(),pen)
            pen.style=android.graphics.Paint.Style.FILL;canvas.drawCircle(x,y,host.dp(3).toFloat(),pen)
        }
    }
    private val equipment=if(bay)FirstAidEquipment.parts else emptyList()
    private val cueParts=if(bay)CasualtyMarkers.parts else emptyList()
    private val injuryParts=if(bay)CasualtyMarkers.injuryParts else emptyList()
    private val drapeParts=if(bay)listOf(CasualtyMarkers.drape)else emptyList()
    private val matParts=if(bay)listOf(CasualtyMarkers.mat)else emptyList()
    private val skinParts=data.parts.filter{it.layer=="skin"}
    private val skinBounds=skinParts.first().bounds
    private val renderer=AnatomyRenderer(data,equipment+cueParts+injuryParts+drapeParts+matParts)
    private val floorModel=FloatArray(16);private val floorMvp=FloatArray(16)
    private val release=ArSessionRelease()
    private var driver:NativeArDriver?=null
    private var anchor:Anchor?=null
    private var facing=Pose.IDENTITY
    private val dwell=ArSurfaceReadiness()
    private val placeRequest=AtomicBoolean(false)
    private var running=false
    @Volatile private var foreground=false
    private var closed=false
    private var installRequested=false
    @Volatile var controls=AnatomyControls();private set
    @Volatile var canPlace=false;private set
    @Volatile var placed=false;private set
    @Volatile private var validAt=0L
    private var widthPx=1;private var heightPx=1
    private var pendingFrame:FloatArray?=if(camera)null else layerBounds("organs")
    private val projection=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16);private val model=FloatArray(16);private val mvp=FloatArray(16);private val inverse=FloatArray(16)
    private val eye=FloatArray(3);private val inverseView=FloatArray(16)
    private var visible=emptyList<AnatomyPart>()
    private var lastControls:AnatomyControls?=null
    private var background:CameraImage?=null
    private var lastStatus=""
    private var resumeAt=0L
    private var frames=0;private var frameAt=0L
    private var nextRenderAt=0L
    private val power=host.getSystemService(PowerManager::class.java)
    private var thermal=PowerManager.THERMAL_STATUS_NONE
    private var thermalSampleAt=Long.MIN_VALUE
    var onStatus:(String)->Unit={}
    var onSelected:(AnatomyPart)->Unit={}
    var onPicked:(AnatomyPart,String)->Unit={_,_->}
    var onToolPicked:(String,String)->Unit={_,_->}
    var onCuePicked:(String,String)->Unit={_,_->}
    var onControls:(AnatomyControls)->Unit={}
    private var lastX=0f;private var lastY=0f;private var lastSpan=0f;private var lastCount=0;private var moved=false;private var gestureStart:AnatomyControls?=null
    private val ticker=object:Runnable {override fun run(){if(!foreground||!running||!camera)return
        val now=SystemClock.elapsedRealtime()
        if(thermalSampleAt==Long.MIN_VALUE||now-thermalSampleAt>=1000){thermal=power.currentThermalStatus;thermalSampleAt=now}
        if(camera&&thermal>=PowerManager.THERMAL_STATUS_CRITICAL){pause();status("Phone is too warm. Let it cool, then retry AR.");return}
        if(camera&&holdRequest.get()!=null&&now-validAt>150)cancelCueHold()
        surface.requestRender();if(camera&&!placed)placementGuide.postInvalidateOnAnimation()
        // Schedule against a deadline: adding a delay after UI work caused measurable cadence drift.
        nextRenderAt=maxOf(nextRenderAt+(if(thermal>=PowerManager.THERMAL_STATUS_SEVERE)50 else 33),SystemClock.uptimeMillis()+1)
        postDelayed(this,(nextRenderAt-SystemClock.uptimeMillis()).coerceAtLeast(1))}}
    init {
        surface.setEGLContextClientVersion(2);surface.setEGLConfigChooser(8,8,8,8,24,0);surface.preserveEGLContextOnPause=true
        surface.setRenderer(this);surface.renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY;surface.onPause()
        surface.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(surface,LayoutParams(-1,-1));setBackgroundColor(Palette.canvas)
        if(camera||bay)addView(placementGuide.apply{importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;isClickable=false},LayoutParams(-1,-1))
        surface.setOnTouchListener{_,event->touch(event)}
    }
    fun change(block:(AnatomyControls)->AnatomyControls){pendingFrame=null;val previous=controls;val next=block(previous);controls=if(camera&&placed&&!previous.edit&&!next.edit)next.copy(yaw=previous.yaw,scale=previous.scale)else next;onControls(controls);surface.requestRender()}
    fun layer(name:String){change{it.copy(layer=name,isolated=false,hidden=emptySet(),skinOverlay=false)};if(!camera)frameBounds(layerBounds(name))}
    private fun layerBounds(name:String):FloatArray {
        val parts=data.parts.filter{it.layer==name}
        return FloatArray(6){i->if(i<3)parts.minOf{it.bounds[i]}else parts.maxOf{it.bounds[i]}}
    }
    fun select(id:String,focus:Boolean=false){val part=data.parts.firstOrNull{it.id==id}?:return;change{it.copy(selected=id,layer=part.layer,hidden=it.hidden-id)};onSelected(part);if(focus)focus(part)}
    fun focus(part:AnatomyPart){
        if(camera)return
        frameBounds(part.bounds)
    }
    private fun frameBounds(b:FloatArray){
        if(widthPx<=1||heightPx<=1){pendingFrame=b.copyOf();return}
        val tangent=tan(Math.toRadians(21.0)).toFloat()
        val aspect=widthPx.toFloat()/heightPx
        if(controls.supine){
            val posed=CasualtyGeometry.bounds(b,controls.bodyRoll,CasualtyGeometry.lift(skinBounds,controls.bodyRoll))
            val radius=sqrt((posed[3]-posed[0]).pow(2)+(posed[4]-posed[1]).pow(2)+(posed[5]-posed[2]).pow(2))*.5f
            change{it.copy(yaw=-28f,pitch=8f,panX=(posed[0]+posed[3])/2,panY=(posed[2]+posed[5])/2+data.height/2,distance=(radius/minOf(tangent,tangent*aspect)*1.15f).coerceIn(.45f,5f))}
            return
        }
        val distance=(maxOf((b[4]-b[1])/2/tangent,(b[3]-b[0])/2/tangent/aspect)*1.2f+maxOf(.08f,b[5])).coerceIn(.35f,5f)
        change{it.copy(yaw=0f,pitch=0f,panX=(b[0]+b[3])/2,panY=(b[1]+b[4])/2-data.height/2,distance=distance)}
    }
    fun reset(){
        if(camera)return
        val shown=data.parts.filter{it.id !in controls.hidden && if(controls.isolated)it.id==controls.selected else it.layer==controls.layer}
        val bounds=if(shown.isEmpty())layerBounds(controls.layer)else FloatArray(6){i->if(i<3)shown.minOf{it.bounds[i]}else shown.maxOf{it.bounds[i]}}
        // Reset the camera only; keep the learner's selected layer, isolation and skin appearance.
        frameBounds(bounds)
    }
    fun interactionReady()=foreground&&(!camera||(placed&&SystemClock.elapsedRealtime()-validAt in 0..150))
    fun place(){if(camera&&canPlace)placeRequest.set(true)}
    fun move(){if(!camera||practice)return;surface.queueEvent{anchor?.detach();anchor=null;placed=false;dwell.reset();placeRequest.set(false)};change{it.copy(edit=true)}}
    private fun status(message:String){if(message==lastStatus)return;lastStatus=message;post{if(!closed)onStatus(message)}}
    fun resume(){
        if(closed||running)return;foreground=true
        if(camera){
            if(release.failed){status("The previous camera could not close. Restart the app before retrying.");return}
            if(release.busy){release.awaitAvailable{if(foreground&&!closed)resume()};status("Releasing the previous camera…");return}
            if(host.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){status("Camera permission is needed for AR. 3D exploration remains available.");return}
            try {
                if(ArCoreApk.getInstance().requestInstall(host,!installRequested)==ArCoreApk.InstallStatus.INSTALL_REQUESTED){installRequested=true;status("Complete the AR Services installation, then return.");return}
                driver=NativeArDriver(host);driver!!.resume()
            }catch(e:Exception){foreground=false;retire();status(ArCameraSupport.startupMessage(e,false));return}
        }
        resumeAt=SystemClock.elapsedRealtime();validAt=0;frameAt=resumeAt;frames=0;nextRenderAt=SystemClock.uptimeMillis();thermalSampleAt=Long.MIN_VALUE;running=true;surface.onResume();keepScreenOn=camera
        // Static exploration redraws only on a view/selection change. An idle body needs no frame pump.
        if(camera)post(ticker)else surface.requestRender()
    }
    fun pause(){cancelCueHold();foreground=false;release.cancelPendingResume();removeCallbacks(ticker);gestureStart=null;lastCount=0;canPlace=false;placeRequest.set(false);keepScreenOn=false
        if(running){surface.onPause();running=false};retire()
    }
    private fun retire(){
        val previous=driver?:return
        runCatching{previous.pause()};anchor?.detach();anchor=null;driver=null;placed=false;dwell.reset()
        release.retire(previous){if(!closed&&foreground)resume()}
    }
    fun close(){
        if(closed)return
        closed=true
        surface.queueEvent{
            // Activity.onPause may already have released the current EGL surface.
            if(android.opengl.EGL14.eglGetCurrentContext()!=android.opengl.EGL14.EGL_NO_CONTEXT){renderer.release();background?.release()}
            background=null
        }
        pause()
        // Detaching ends GLSurfaceView's GL thread and releases its preserved EGL context.
        removeView(surface)
    }
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        try {renderer.create();background=if(camera)CameraImage()else null;driver?.invalidateTexture()}
        catch(error:Exception){android.util.Log.e("AnatomyAR","Renderer initialisation failed",error);post{if(foreground){pause();status("The model renderer could not start. Return to the course and retry.")}}}
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){resetCueDwell();widthPx=w;heightPx=h;GLES20.glViewport(0,0,w,h);dwell.reset();canPlace=false;pendingFrame?.let{bounds->post{if(!closed)frameBounds(bounds)}}}
    override fun onDrawFrame(gl:GL10?){
        GLES20.glClearColor(.96f,.97f,.985f,1f);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if(!foreground)return
        val c=controls
        if(lastControls!=c){visible=data.parts.filter{it.id !in c.hidden && if(c.isolated)it.id==c.selected else it.layer==c.layer}+
            (if(c.equipment)equipment else emptyList())+cueParts.filter{it.id=="cue:${c.cueRegion}"}+injuryParts.filter{it.id=="injury:${c.casualtyDetail}"};lastControls=c}
        Matrix.setIdentityM(model,0)
        Matrix.setIdentityM(floorModel,0)
        var preview=false
        if(camera){
            try {
                val ar=driver?:return;val bg=background?:return
                ar.setCameraTextureName(bg.texture);ar.setDisplayGeometry(host.windowManager.defaultDisplay.rotation,widthPx,heightPx)
                val frame=ar.update();if(frame.timestamp>0)bg.draw(frame)
                val frameCamera=ar.currentCamera?:return
                val now=SystemClock.elapsedRealtime()
                if(!ar.snapshot(now).ready){resetCueDwell();validAt=0;canPlace=false;dwell.reset();placeRequest.set(false)
                    status(when(frameCamera.trackingFailureReason){TrackingFailureReason.INSUFFICIENT_LIGHT->"Add light and keep the camera uncovered.";TrackingFailureReason.INSUFFICIENT_FEATURES->"Aim at a textured floor; move the phone gently sideways.";TrackingFailureReason.EXCESSIVE_MOTION->"Move the phone more slowly.";else->"Finding tracking · aim at a clear, textured floor."})
                    if(now-resumeAt>45000)post{if(foreground){pause();status("Tracking could not start. Check lighting and retry AR, or use 3D.")}}
                    return
                }
                resumeAt=now
                frameCamera.getProjectionMatrix(projection,0,.03f,30f);frameCamera.getViewMatrix(view,0)
                val currentAnchor=anchor
                if(currentAnchor==null){
                    val hit=frame.hitTest(widthPx*.5f,heightPx*.62f).firstOrNull{h->(h.trackable as? Plane)?.let{it.type==Plane.Type.HORIZONTAL_UPWARD_FACING&&it.trackingState==TrackingState.TRACKING&&it.isPoseInPolygon(h.hitPose)}==true&&h.distance in .4f..3f}
                    if(hit==null){canPlace=false;placeRequest.set(false);dwell.reset();status("Aim the circle at a clear, textured surface; move gently sideways.");return}
                    val plane=hit.trackable as Plane
                    val facingPreview=RoomAnchorPose.facingOffset(hit.hitPose,frameCamera.pose)
                    val placementPose=RoomAnchorPose.model(hit.hitPose,facingPreview)
                    val bounds=skinBounds
                    val yaw=Math.toRadians(c.yaw.toDouble());val ca=cos(yaw).toFloat();val sa=sin(yaw).toFloat()
                    val placedBounds=if(c.supine)CasualtyGeometry.bounds(bounds,c.bodyRoll,CasualtyGeometry.lift(bounds,c.bodyRoll))else bounds
                    val zBounds=listOf((if(bay)minOf(placedBounds[2],-1.87f)else placedBounds[2])-.08f,(if(bay)maxOf(placedBounds[5],.13f)else placedBounds[5])+.08f)
                    val right=if(bay)maxOf(placedBounds[3],.78f)else placedBounds[3]
                    val footprint=listOf((if(bay)minOf(placedBounds[0],-.48f)else placedBounds[0])-.06f,right+.06f).flatMap{x->zBounds.map{z->floatArrayOf((x*ca+z*sa)*c.scale,0f,(-x*sa+z*ca)*c.scale)}}
                    if(!footprint.all{plane.isPoseInPolygon(Pose.makeTranslation(placementPose.transformPoint(it)))}){canPlace=false;placeRequest.set(false);dwell.reset();status(if(bay)"Scan a larger clear floor patch for the adult and AED."else "Scan a larger clear patch, or choose the 40% body size.");return}
                    val local=plane.centerPose.inverse().transformPoint(hit.hitPose.translation)
                    canPlace=dwell.observe(plane,local[0],local[2],frame.timestamp,now,0).ready
                    placementPose.toMatrix(model,0);preview=true
                    status(if(canPlace)"Surface ready · tap Place body." else "Hold this surface in view…")
                    if(placeRequest.getAndSet(false)&&canPlace){anchor=hit.createAnchor();facing=facingPreview;placed=true;canPlace=false;post{placementGuide.invalidate();change{it.copy(edit=false)}}}
                }else{
                    canPlace=false
                    if(currentAnchor.trackingState!=TrackingState.TRACKING){resetCueDwell();validAt=0;status("Body tracking paused · look back at the placement area.");return}
                    RoomAnchorPose.model(currentAnchor.pose,facing).toMatrix(model,0)
                    status(if(c.edit)"Edit placement · rotate or scale, then lock." else "Body locked · ${(c.scale*100).roundToInt()}% of source size")
                }
                Matrix.rotateM(model,0,c.yaw,0f,1f,0f);Matrix.scaleM(model,0,c.scale,c.scale,c.scale)
                System.arraycopy(model,0,floorModel,0,16)
                if(c.supine){Matrix.translateM(model,0,0f,CasualtyGeometry.lift(skinBounds,c.bodyRoll),0f);Matrix.rotateM(model,0,-90f,1f,0f,0f);Matrix.rotateM(model,0,c.bodyRoll,0f,1f,0f)}
                validAt=now
            }catch(e:Exception){resetCueDwell();validAt=0;canPlace=false;post{if(foreground){pause();status("Camera interrupted. Retry AR or continue in 3D.")}};return}
        }else{
            Matrix.perspectiveM(projection,0,42f,widthPx.toFloat()/heightPx,.02f,30f)
            if(c.supine){
                val yaw=Math.toRadians(c.yaw.toDouble());val elevation=Math.toRadians((52f-c.pitch).coerceIn(8f,88f).toDouble())
                val centreZ=-data.height/2+c.panY;val centreY=.14f
                Matrix.setLookAtM(view,0,c.panX+(sin(yaw)*cos(elevation)*c.distance).toFloat(),centreY+(sin(elevation)*c.distance).toFloat(),centreZ+(cos(yaw)*cos(elevation)*c.distance).toFloat(),c.panX,centreY,centreZ,0f,1f,0f)
                Matrix.translateM(model,0,0f,CasualtyGeometry.lift(skinBounds,c.bodyRoll),0f);Matrix.rotateM(model,0,-90f,1f,0f,0f);Matrix.rotateM(model,0,c.bodyRoll,0f,1f,0f)
            }else{
                Matrix.setLookAtM(view,0,c.panX,data.height/2+c.panY,c.distance,c.panX,data.height/2+c.panY,0f,0f,1f,0f)
                Matrix.translateM(model,0,0f,data.height/2,0f);Matrix.rotateM(model,0,c.pitch,1f,0f,0f);Matrix.rotateM(model,0,c.yaw,0f,1f,0f);Matrix.translateM(model,0,0f,-data.height/2,0f)
            }
            status("3D exploration · ${if(c.yaw%360f==0f)"front" else "rotated"} · left/right refer to the person")
        }
        Matrix.multiplyMM(vp,0,projection,0,view,0);Matrix.multiplyMM(mvp,0,vp,0,model,0);Matrix.invertM(inverse,0,mvp,0)
        Matrix.invertM(inverseView,0,view,0);eye[0]=inverseView[12];eye[1]=inverseView[13];eye[2]=inverseView[14]
        try {
        if(bay&&c.supine){
            Matrix.multiplyMM(floorMvp,0,vp,0,floorModel,0)
            renderer.draw(floorMvp,floorModel,matParts,null,if(camera).35f else 1f,c.skinTone,cameraPosition=eye)
        }
        renderer.draw(mvp,model,visible,if(c.equipmentPowered)FirstAidEquipment.DISPLAY else c.selected,if(preview).4f else 1f,c.skinTone,cameraPosition=eye)
        if(bay&&c.layer=="skin")renderer.draw(mvp,model,drapeParts,null,if(preview).4f else 1f,c.skinTone,cameraPosition=eye)
        if(c.skinOverlay&&c.layer!="skin"&&!c.isolated)renderer.draw(mvp,model,skinParts,null,.15f,c.skinTone,cameraPosition=eye)
        }catch(error:Exception){
            resetCueDwell();validAt=0;canPlace=false
            android.util.Log.e("AnatomyAR","Model rendering interrupted",error)
            post{if(foreground){pause();status("The model could not be drawn. Return to the course and retry; saved progress is kept.")}}
            return
        }
        val request=holdRequest.get();val sampledAt=SystemClock.elapsedRealtime()
        if(request!=null&&request.controls==c&&c.cueHold&&interactionReady()){
            val ray=ray(request.x,request.y)
            val inside=c.cueRegion?.let{CasualtyMarkers.hit(it,ray.first,ray.second)}==true
            holdProgress.set(HoldProgress(request.serial,holdGate.sample(request.serial,inside,sampledAt),sampledAt))
        }else{holdGate.reset();holdProgress.set(null)}
        frames++;val at=SystemClock.elapsedRealtime();if(at-frameAt>=5000){
            val runtime=Runtime.getRuntime()
            android.util.Log.i("AnatomyAR","camera=$camera parts=${visible.size} frames=$frames intervalMs=${at-frameAt} placed=$placed thermal=$thermal nativeAllocatedKiB=${android.os.Debug.getNativeHeapAllocatedSize()/1024} javaUsedKiB=${(runtime.totalMemory()-runtime.freeMemory())/1024}")
            frameAt=at;frames=0
        }
    }
    private fun ray(x:Float,y:Float):Pair<FloatArray,FloatArray>{
        fun unproject(z:Float):FloatArray{val p=FloatArray(4);Matrix.multiplyMV(p,0,inverse,0,floatArrayOf(2*x/widthPx-1,1-2*y/heightPx,z,1f),0);return FloatArray(3){p[it]/p[3]}}
        val a=unproject(-1f);val b=unproject(1f);return a to FloatArray(3){b[it]-a[it]}
    }
    private fun pick(x:Float,y:Float,expectedControls:AnatomyControls?=null){
        surface.queueEvent {
            if(!foreground||lastControls!=controls||expectedControls!=null&&expectedControls!=controls||(camera&&(!placed||SystemClock.elapsedRealtime()-validAt !in 0..150)))return@queueEvent
            val (a,d)=ray(x,y)
            val pickedControls=controls
            val cue=pickedControls.cueRegion
            if(cue!=null&&CasualtyMarkers.hit(cue,a,d)){
                post{if(foreground&&controls==pickedControls&&interactionReady())onCuePicked(cue,if(camera)"ar-exploration"else"3d")};return@queueEvent
            }
            val hit=visible.filter{it.layer!="cue-detail"}.mapNotNull{part->AnatomyPicking.distance(part.vertices,part.bounds,a,d)?.let{part to it}}.minByOrNull{it.second}?.first?:return@queueEvent
            post{if(foreground){
                val mode=if(camera)"ar-exploration" else "3d"
                if(hit.layer=="cue")onCuePicked(hit.id.removePrefix("cue:"),mode)
                else if(hit.layer=="equipment")onToolPicked(hit.id,mode)
                else{change{it.copy(selected=hit.id)};onSelected(hit);onPicked(hit,mode)}
            }}
        }
    }
    private fun touch(e:MotionEvent):Boolean {
        val count=e.pointerCount;val x=(0 until count).sumOf{e.getX(it).toDouble()}.toFloat()/count;val y=(0 until count).sumOf{e.getY(it).toDouble()}.toFloat()/count
        val span=if(count>=2)hypot(e.getX(1)-e.getX(0),e.getY(1)-e.getY(0))else 0f
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{holdSerial++;holdRequest.set(if(controls.cueHold&&interactionReady())HoldRequest(holdSerial,x,y,controls)else null);holdProgress.set(null);placementGuide.invalidate();gestureStart=controls;moved=false;lastCount=1;lastX=x;lastY=y;lastSpan=span;parent?.requestDisallowInterceptTouchEvent(true)}
            MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_POINTER_UP->{cancelCueHold();moved=true;lastCount=0;lastX=x;lastY=y;lastSpan=span}
            MotionEvent.ACTION_MOVE->{
                if(lastCount==count){val dx=x-lastX;val dy=y-lastY;if(abs(dx)+abs(dy)>2)moved=true
                    holdRequest.get()?.let{request->
                        if(hypot(x-request.originX,y-request.originY)>host.dp(24))cancelCueHold()
                        else holdRequest.set(request.copy(x=x,y=y))
                    }
                    if(!controls.cueHold&&!practice&&(!camera||controls.edit))change{c->
                        if(count>=2){val ratio=if(lastSpan>1)span/lastSpan else 1f
                            if(camera)c.copy(scale=(c.scale*ratio).coerceIn(.3f,1.5f))
                            else c.copy(distance=(c.distance/ratio).coerceIn(.3f,5f),panX=(c.panX-dx/widthPx*c.distance).coerceIn(-2f,2f),panY=(c.panY+dy/heightPx*c.distance).coerceIn(-2f,2f))
                        }else c.copy(yaw=(c.yaw+dx*.4f)%360f,pitch=if(camera)0f else(c.pitch+dy*.3f).coerceIn(-65f,65f))
                    }
                };lastCount=count;lastX=x;lastY=y;lastSpan=span
            }
            MotionEvent.ACTION_UP->{val request=holdRequest.get();val progress=holdProgress.get();val held=request!=null&&request.controls==controls&&progress?.serial==request.serial&&progress.fraction>=1f&&SystemClock.elapsedRealtime()-progress.at in 0..150;cancelCueHold();if(if(controls.cueHold)held else !moved){performClick();pick(e.x,e.y,request?.controls)};gestureStart=null;lastCount=0;parent?.requestDisallowInterceptTouchEvent(false)}
            MotionEvent.ACTION_CANCEL->{cancelCueHold();gestureStart?.let{start->change{start}};gestureStart=null;lastCount=0;parent?.requestDisallowInterceptTouchEvent(false)}
        };return true
    }
    override fun performClick():Boolean{super.performClick();return true}
    private class CameraImage {
        val texture:Int
        private val program:Int
        private val vertices=floats(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));private val uv=floats(FloatArray(8))
        init{
            val textures=IntArray(1);GLES20.glGenTextures(1,textures,0);texture=textures[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture)
            for(p in listOf(GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_TEXTURE_MAG_FILTER))GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,p,GLES20.GL_LINEAR)
            for(p in listOf(GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_TEXTURE_WRAP_T))GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,p,GLES20.GL_CLAMP_TO_EDGE)
            fun shader(t:Int,s:String):Int {
                val shader=GLES20.glCreateShader(t);check(shader!=0){"Camera shader allocation failed"}
                GLES20.glShaderSource(shader,s);GLES20.glCompileShader(shader)
                val ok=IntArray(1);GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0)
                if(ok[0]==0){val log=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);error("Camera shader failed: $log")}
                return shader
            }
            var vs=0;var fs=0;var linked=0
            try {
                vs=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 p;attribute vec2 uv;varying vec2 tex;void main(){gl_Position=vec4(p,0.,1.);tex=uv;}")
                fs=shader(GLES20.GL_FRAGMENT_SHADER,"#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES camera;varying vec2 tex;void main(){gl_FragColor=texture2D(camera,tex);}")
                linked=GLES20.glCreateProgram();check(linked!=0){"Camera program allocation failed"}
                GLES20.glAttachShader(linked,vs);GLES20.glAttachShader(linked,fs);GLES20.glLinkProgram(linked)
                val ok=IntArray(1);GLES20.glGetProgramiv(linked,GLES20.GL_LINK_STATUS,ok,0)
                check(ok[0]!=0){"Camera program failed: "+GLES20.glGetProgramInfoLog(linked)}
                program=linked
            }catch(error:RuntimeException){
                if(linked!=0)GLES20.glDeleteProgram(linked);GLES20.glDeleteTextures(1,textures,0);throw error
            }finally{if(vs!=0)GLES20.glDeleteShader(vs);if(fs!=0)GLES20.glDeleteShader(fs)}
        }
        fun draw(frame:Frame){
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);vertices.position(0);uv.position(0);frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,vertices,Coordinates2d.TEXTURE_NORMALIZED,uv)
            GLES20.glUseProgram(program);val p=GLES20.glGetAttribLocation(program,"p");val u=GLES20.glGetAttribLocation(program,"uv")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"camera"),0)
            GLES20.glVertexAttribPointer(p,2,GLES20.GL_FLOAT,false,0,vertices);GLES20.glVertexAttribPointer(u,2,GLES20.GL_FLOAT,false,0,uv);GLES20.glEnableVertexAttribArray(p);GLES20.glEnableVertexAttribArray(u);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(p);GLES20.glDisableVertexAttribArray(u)
        }
        fun release(){GLES20.glDeleteProgram(program);GLES20.glDeleteTextures(1,intArrayOf(texture),0)}
        private fun floats(v:FloatArray)=ByteBuffer.allocateDirect(v.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(v);position(0)}
    }
}
