package com.narvyn.suraksha

import android.app.Activity
import android.graphics.RectF
import android.opengl.GLES20 as G
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** One head pose, two calibrated eye views, then the SDK lens-distortion compositor. */
internal class VrSceneView(private val host:Activity,@Volatile private var dwell:Boolean,private val preview:Boolean):GLSurfaceView(host),GLSurfaceView.Renderer {
    var onCommand:(String,Long)->Unit={_,_->}
    var onFailure:(String)->Unit={}
    var onReady:()->Unit={}
    private val pendingPage=AtomicReference<VrPage>()
    private data class Prepared(val page:VrPage,val panel:VrPanelPainter.Painted,val tools:VrPanelPainter.Painted)
    private val prepared=AtomicReference<Prepared>()
    private val painter=Executors.newSingleThreadExecutor()
    private val pendingBody=AtomicReference<AnatomyData>()
    @Volatile private var foreground=false
    @Volatile private var inputAllowed=false
    @Volatile private var buttonHeld=false
    @Volatile private var failed=false
    @Volatile var renderedRevision=-1L;private set
    @Volatile var frames=0L;private set
    @Volatile var focusedTarget:String?=null;private set
    @Volatile var renderedEyes=0;private set
    @Volatile var observedFramesPerSecond=0f;private set
    @Volatile private var resetRequested=false
    private var page:VrPage?=null;private var body:AnatomyData?=null
    private var anatomy:AnatomyRenderer?=null;private var world=WorldEquipment();private var graphics=VrGraphics()
    private var handle=0L;private var widthPx=0;private var heightPx=0
    private var fbo=0;private var stereoTexture=0;private var depth=0;private var renderWidth=0;private var renderHeight=0
    private var panelTexture=0;private var toolsTexture=0
    private var targets=emptyList<VrInteraction.Target>()
    private var pageHits=emptyList<VrInteraction.Target>()
    private val gate=VrGazeGate()
    private val head=FloatArray(16);private val inverseHead=FloatArray(16);private val eye=FloatArray(16);private val projection=FloatArray(16);private val view=FloatArray(16);private val vp=FloatArray(16)
    private val model=FloatArray(16);private val bodyModel=FloatArray(16);private val bodyMvp=FloatArray(16);private val inverseBody=FloatArray(16)
    private var modelScale=1f;private var modelYaw=0f
    private var lastNanos=0L;private var elapsed=0f;private var downAt=0L;private var touchCancelled=false;private var touchActive=false;private var statsAt=0L;private var statsFrames=0
    private var previewAim=floatArrayOf(0f,0f,-1f)
    private var lastRay=VrInteraction.Ray(FloatArray(3),floatArrayOf(0f,0f,-1f))
    private var selectedPart:AnatomyPart?=null
    private var partDetail:VrPartDetail?=null
    private val detailModel=FloatArray(16);private val inverseDetail=FloatArray(16);private val detailMvp=FloatArray(16)
    private var cpr:FirstAidCprPlacement.Calibration?=null
    private var sceneKey=""
    private val renderTransforms=mutableMapOf<String,FloatArray>()
    private val panelX=1.02f;private val panelY=.0f;private val panelZ=-2.9f;private val panelW=1.60f;private val panelH=2.13f
    private fun automaticTargetPresent(value:VrPage?=page)=dwell||value?.actions?.any{it.id=="dwell-mode"}==true||value?.toolbar?.any{it.id=="dwell-mode"}==true
    init {
        require(!preview||BuildConfig.DEBUG&&Build.HARDWARE in setOf("ranchu","goldfish"))
        tag="vr-canvas";contentDescription="Headset stereo training view. Use the viewer button to select the centre target."
        setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,0,16,0)
        preserveEGLContextOnPause=false;setRenderer(this);renderMode=RENDERMODE_CONTINUOUSLY
        isFocusable=true;isFocusableInTouchMode=true
    }
    fun present(value:VrPage){pendingPage.set(value);resetRequested=true;paintAsync(value)}
    private fun paintAsync(value:VrPage){
        if(painter.isShutdown)return
        painter.submit{
            try {
                val panel=VrPanelPainter.paint(value);val tools=VrPanelPainter.toolbar(value.toolbar)
                if(pendingPage.get()?.revision!=value.revision||painter.isShutdown){panel.bitmap.recycle();tools.bitmap.recycle();return@submit}
                prepared.getAndSet(Prepared(value,panel,tools))?.let{it.panel.bitmap.recycle();it.tools.bitmap.recycle()}
            }catch(e:Exception){post{if(foreground)onFailure("This lesson card could not be displayed. Remove the phone and return to setup.")}}
        }
    }
    fun body(value:AnatomyData){pendingBody.set(value)}
    fun allowInput(value:Boolean){inputAllowed=value;if(!value){touchCancelled=true;touchActive=false;buttonHeld=false};resetRequested=true}
    fun useDwell(value:Boolean){dwell=value;resetRequested=true}
    fun resumeRuntime(){if(foreground)return;foreground=true;failed=false;super.onResume()}
    fun pauseRuntime(){
        if(!foreground)return;foreground=false;resetRequested=true
        queueEvent{releaseGl()};super.onPause()
    }
    fun closeRuntime(){pauseRuntime();painter.shutdownNow();prepared.getAndSet(null)?.let{it.panel.bitmap.recycle();it.tools.bitmap.recycle()}}
    fun recenter(){queueEvent{if(handle!=0L)CardboardBridge.recenter(handle);previewAim=floatArrayOf(0f,0f,-1f);gate.reset(waitForExit=automaticTargetPresent())}}
    fun resizeModel(delta:Float){queueEvent{if(delta.isFinite()){modelScale=(modelScale*delta).coerceIn(.7f,1.35f);gate.reset(waitForExit=automaticTargetPresent())}}}
    fun rotateModel(){queueEvent{modelYaw=(modelYaw+25f)%360;gate.reset(waitForExit=automaticTargetPresent())}}
    fun resetModel(){queueEvent{modelScale=1f;modelYaw=0f;elapsed=0f;gate.reset(waitForExit=automaticTargetPresent())}}
    fun replayVisual(){queueEvent{elapsed=0f;gate.reset(waitForExit=automaticTargetPresent())}}
    /** Emulator-only camera control. It simulates viewing direction; it never creates tracking evidence. */
    fun previewAimAt(id:String?){
        check(preview)
        queueEvent{if(id==null){previewAim=floatArrayOf(0f,0f,1f);gate.reset()}else targets.firstOrNull{it.id==id}?.let{previewAim=VrInteraction.normalised(it.x,it.y,it.z);gate.reset()}}
    }
    fun trigger(){queueEvent{if(foreground&&inputAllowed&&!failed){gate.trigger(SystemClock.elapsedRealtime())?.let(::activate)}}}
    fun buttonDown(){buttonHeld=true}
    fun buttonUp(select:Boolean,longPress:Boolean=false){queueEvent{
        if(buttonHeld&&foreground&&inputAllowed&&!failed&&select){if(longPress){if(handle!=0L)CardboardBridge.recenter(handle);previewAim=floatArrayOf(0f,0f,-1f)}else gate.trigger(SystemClock.elapsedRealtime())?.let(::activate)}
        buttonHeld=false;gate.reset(waitForExit=automaticTargetPresent())
    }}
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{downAt=SystemClock.elapsedRealtime();touchCancelled=false;touchActive=inputAllowed;if(touchActive)buttonDown();return true}
            MotionEvent.ACTION_POINTER_DOWN->{touchCancelled=true;buttonUp(false);return true}
            MotionEvent.ACTION_UP->{buttonUp(touchActive&&!touchCancelled,SystemClock.elapsedRealtime()-downAt>=1100);if(touchActive&&!touchCancelled)performClick();touchActive=false;return true}
            MotionEvent.ACTION_CANCEL->{touchCancelled=true;touchActive=false;buttonUp(false);return true}
        };return true
    }
    override fun performClick():Boolean=super.performClick()
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        try {graphics=VrGraphics().also{it.create()};world=WorldEquipment().also{it.create()};anatomy=null;body=null;cpr=null;page=null;panelTexture=0;toolsTexture=0;renderedRevision=-1;lastNanos=0;statsAt=0;statsFrames=0;frames=0;pendingPage.get()?.let(::paintAsync)}
        catch(e:Exception){fail("The headset renderer could not start. Remove the phone and return to handheld learning.")}
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){
        if(failed||w<2||h<2)return
        try {
            if(handle!=0L){CardboardBridge.pause(handle);CardboardBridge.destroy(handle)}
            widthPx=w;heightPx=h;handle=CardboardBridge.create(w,h);check(handle!=0L)
            if(foreground&&!preview)CardboardBridge.resume(handle)
            allocateStereo();gate.reset(waitForExit=automaticTargetPresent());renderedRevision=-1
        }catch(e:Exception){fail("Viewer calibration could not load. Remove the phone and check its viewer profile.")}
    }
    private fun allocateStereo(){
        if(fbo!=0)G.glDeleteFramebuffers(1,intArrayOf(fbo),0);if(depth!=0)G.glDeleteRenderbuffers(1,intArrayOf(depth),0);if(stereoTexture!=0)G.glDeleteTextures(1,intArrayOf(stereoTexture),0)
        // Software-emulated stereo is a labelled development preview, with its own pixel budget.
        val scale=if(preview)minOf(.55f,1280f/widthPx,720f/heightPx)else minOf(.85f,2048f/widthPx,1152f/heightPx)
        renderWidth=((widthPx*scale).toInt()/2*2).coerceAtLeast(2);renderHeight=(heightPx*scale).toInt().coerceAtLeast(2)
        stereoTexture=IntArray(1).also{G.glGenTextures(1,it,0)}[0];G.glBindTexture(G.GL_TEXTURE_2D,stereoTexture)
        G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MIN_FILTER,G.GL_LINEAR);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MAG_FILTER,G.GL_LINEAR)
        G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_WRAP_S,G.GL_CLAMP_TO_EDGE);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_WRAP_T,G.GL_CLAMP_TO_EDGE)
        G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_RGB,renderWidth,renderHeight,0,G.GL_RGB,G.GL_UNSIGNED_BYTE,null)
        depth=IntArray(1).also{G.glGenRenderbuffers(1,it,0)}[0];G.glBindRenderbuffer(G.GL_RENDERBUFFER,depth);G.glRenderbufferStorage(G.GL_RENDERBUFFER,G.GL_DEPTH_COMPONENT16,renderWidth,renderHeight)
        fbo=IntArray(1).also{G.glGenFramebuffers(1,it,0)}[0];G.glBindFramebuffer(G.GL_FRAMEBUFFER,fbo)
        G.glFramebufferTexture2D(G.GL_FRAMEBUFFER,G.GL_COLOR_ATTACHMENT0,G.GL_TEXTURE_2D,stereoTexture,0);G.glFramebufferRenderbuffer(G.GL_FRAMEBUFFER,G.GL_DEPTH_ATTACHMENT,G.GL_RENDERBUFFER,depth)
        check(G.glCheckFramebufferStatus(G.GL_FRAMEBUFFER)==G.GL_FRAMEBUFFER_COMPLETE);G.glBindFramebuffer(G.GL_FRAMEBUFFER,0)
    }
    override fun onDrawFrame(gl:GL10?){
        if(!foreground||failed||handle==0L)return
        try {
            prepared.getAndSet(null)?.let{ready->
                val next=ready.page;val painted=ready.panel;val tool=ready.tools
                if(next.revision!=pendingPage.get()?.revision){painted.bitmap.recycle();tool.bitmap.recycle();return@let}
                panelTexture=graphics.texture(painted.bitmap,panelTexture)
                toolsTexture=graphics.texture(tool.bitmap,toolsTexture)
                pageHits=painted.hits.map{hit->target(hit,panelX,panelY,panelZ,panelW,panelH,VrPanelPainter.WIDTH.toFloat(),VrPanelPainter.HEIGHT.toFloat())}+
                    tool.hits.map{hit->target(hit,0f,-1.12f,-2.4f,2.6f,.39f,1200f,180f)}
                if(next.scene?.toString()!=sceneKey){modelScale=1f;modelYaw=0f;elapsed=0f;sceneKey=next.scene?.toString().orEmpty()}
                page=next;gate.reset(waitForExit=automaticTargetPresent(next));lastNanos=0
            }
            val next=page?:return
            pendingBody.get()?.takeIf{it!==body}?.let{loaded->body=loaded;anatomy?.release();anatomy=AnatomyRenderer(loaded,AnatomyClothing.parts+FirstAidTeachingProps.parts+FirstAidEquipment.parts).also{it.create()};cpr=FirstAidCprPlacement.forParts(loaded.parts)}
            if(resetRequested){gate.reset(waitForExit=automaticTargetPresent());resetRequested=false}
            val at=SystemClock.elapsedRealtimeNanos();val dt=if(lastNanos==0L)0f else ((at-lastNanos)/1e9f).coerceIn(0f,.05f);lastNanos=at
            if(!next.visualPaused)elapsed+=dt
            if(preview)Matrix.setLookAtM(head,0,0f,0f,0f,previewAim[0],previewAim[1],previewAim[2],0f,1f,0f)
            else {
                @Suppress("DEPRECATION") val rotation=host.windowManager.defaultDisplay.rotation
                CardboardBridge.headView(handle,at+30_000_000,rotation,head)
            }
            check(head.all{it.isFinite()}&&Matrix.invertM(inverseHead,0,head,0))
            val origin=floatArrayOf(inverseHead[12],inverseHead[13],inverseHead[14]);val direction=VrInteraction.normalised(-inverseHead[8],-inverseHead[9],-inverseHead[10])
            lastRay=VrInteraction.Ray(origin,direction)
            prepareBodyModel(next.scene)
            targets=pageHits+modelTargets(next.scene)
            val candidate=VrInteraction.nearest(lastRay,targets)
            val focused=candidate?.takeIf{!it.id.startsWith("observe:")||partHit()};focusedTarget=focused?.id
            val dwellHere=!buttonHeld&&(dwell||focused?.id=="dwell-mode")
            gate.frame(focused?.id,SystemClock.elapsedRealtime(),inputAllowed&&next.revision==pendingPage.get()?.revision,dwellHere)?.let(::activate)
            G.glBindFramebuffer(G.GL_FRAMEBUFFER,fbo);G.glDisable(G.GL_SCISSOR_TEST);G.glClearColor(.09f,.16f,.20f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT or G.GL_DEPTH_BUFFER_BIT)
            for(e in 0..1){
                G.glViewport(e*renderWidth/2,0,renderWidth/2,renderHeight)
                CardboardBridge.projection(handle,e,.08f,30f,projection);CardboardBridge.eyeFromHead(handle,e,eye)
                Matrix.multiplyMM(view,0,eye,0,head,0);Matrix.multiplyMM(vp,0,projection,0,view,0)
                drawScene(next,origin)
                // Spatial controls remain readable even where a plinth or teaching prop crosses them.
                G.glDisable(G.GL_DEPTH_TEST)
                graphics.plane(vp,panelX,panelY,panelZ,panelW,panelH,panelTexture)
                graphics.plane(vp,0f,-1.12f,-2.4f,2.6f,.39f,toolsTexture)
                if(focused!=null){
                    // An outline around the visible command, not colour-dependent feedback alone.
                    val c=floatArrayOf(.12f,.75f,.65f,1f);val t=focused;val z=t.z+.005f
                    graphics.plane(vp,t.x,t.y+t.height/2,z,t.width,.009f,colour=c);graphics.plane(vp,t.x,t.y-t.height/2,z,t.width,.009f,colour=c)
                    graphics.plane(vp,t.x-t.width/2,t.y,z,.009f,t.height,colour=c);graphics.plane(vp,t.x+t.width/2,t.y,z,.009f,t.height,colour=c)
                }
                val distance=focused?.let{VrInteraction.intersect(lastRay,it)}?.coerceIn(.5f,8f)?:2.4f
                // Each eye sees the same world-space reticle, at the selected target's depth.
                G.glDisable(G.GL_DEPTH_TEST)
                graphics.plane(vp,origin[0]+direction[0]*distance,origin[1]+direction[1]*distance,origin[2]+direction[2]*distance,.026f*distance,.026f*distance,ringProgress=gate.fraction)
            }
            G.glBindFramebuffer(G.GL_FRAMEBUFFER,0);G.glViewport(0,0,widthPx,heightPx);G.glDisable(G.GL_DEPTH_TEST);G.glDisable(G.GL_BLEND)
            CardboardBridge.composite(handle,widthPx,heightPx,stereoTexture)
            renderedEyes=2;renderedRevision=next.revision;frames++
            if(frames==1L)post{if(foreground)onReady()}
            if(statsAt==0L)statsAt=at
            statsFrames++
            if(at-statsAt>=2_000_000_000){observedFramesPerSecond=statsFrames*1e9f/(at-statsAt);statsFrames=0;statsAt=at}
        }catch(e:Exception){fail("The headset view was interrupted. Remove the phone; your last saved lesson step is retained.")}
    }
    private fun target(hit:VrPanelPainter.Hit,x:Float,y:Float,z:Float,w:Float,h:Float,pixelsW:Float,pixelsH:Float):VrInteraction.Target {
        val r=hit.rect
        return VrInteraction.Target(hit.id,x+(r.centerX()/pixelsW-.5f)*w,y+(.5f-r.centerY()/pixelsH)*h,z,r.width()/pixelsW*w,r.height()/pixelsH*h)
    }
    private fun prepareBodyModel(spec:VrSceneSpec?){
        Matrix.setIdentityM(bodyModel,0)
        Matrix.translateM(bodyModel,0,.05f,-.86f,-2.65f);Matrix.rotateM(bodyModel,0,90f+modelYaw,0f,1f,0f);Matrix.scaleM(bodyModel,0,modelScale,modelScale,modelScale)
        val supine=spec?.teachingKey!=null
        if(supine){Matrix.translateM(bodyModel,0,0f,.1461f,0f);Matrix.rotateM(bodyModel,0,-90f,1f,0f,0f)}
        else {Matrix.translateM(bodyModel,0,-.9f,-.5f,0f);Matrix.rotateM(bodyModel,0,-90f,0f,1f,0f)}
        val selected=body?.parts?.firstOrNull{it.id==spec?.part}
        if(selected!==selectedPart){selectedPart=selected;partDetail=selected?.let{VrPartDetail.forPart(it)}}
        partDetail?.let{d->
            Matrix.setIdentityM(detailModel,0);Matrix.translateM(detailModel,0,-.76f,-.15f,-2.0f);Matrix.rotateM(detailModel,0,modelYaw,0f,1f,0f)
            Matrix.scaleM(detailModel,0,d.scale*modelScale,d.scale*modelScale,d.scale*modelScale);Matrix.translateM(detailModel,0,-d.centre[0],-d.centre[1],-d.centre[2]);Matrix.invertM(inverseDetail,0,detailModel,0)
        }
        Matrix.invertM(inverseBody,0,bodyModel,0)
    }
    private fun modelTargets(spec:VrSceneSpec?):List<VrInteraction.Target> {
        if(spec==null)return emptyList()
        val part=selectedPart
        if(spec.model=="anatomy"&&part!=null&&spec.teachingKey==null){
            val d=partDetail?:return emptyList();val p=FloatArray(4);Matrix.multiplyMV(p,0,detailModel,0,d.surfacePoint+1f,0)
            return listOf(VrInteraction.Target("observe:${part.id}",p[0],p[1],p[2],.90f*modelScale,.90f*modelScale))
        }
        return listOf(VrInteraction.Target("inspect-model",-.85f,-.28f,-2.1f,1.4f,1.1f))
    }
    private fun drawScene(page:VrPage,eye:FloatArray){
        G.glEnable(G.GL_DEPTH_TEST)
        graphics.plane(vp,0f,-1.55f,-2f,14f,14f,colour=floatArrayOf(.31f,.39f,.42f,1f),tilt=-90f)
        graphics.plane(vp,0f,1.4f,-7f,14f,6f,colour=floatArrayOf(.18f,.28f,.32f,1f))
        for(x in -5..5)graphics.plane(vp,x.toFloat(),-1.548f,-2f,.008f,12f,colour=floatArrayOf(.39f,.49f,.50f,1f),tilt=-90f)
        for(z in -7..3)graphics.plane(vp,0f,-1.547f,z.toFloat(),12f,.008f,colour=floatArrayOf(.39f,.49f,.50f,1f),tilt=-90f)
        graphics.plane(vp,-.9f,-.88f,-2.7f,2.1f,1.65f,colour=floatArrayOf(.62f,.72f,.74f,1f),tilt=-90f)
        val spec=page.scene?:return
        if(spec.model!="anatomy"){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,-.85f,-.86f,-2.65f);Matrix.rotateM(model,0,modelYaw,0f,1f,0f);Matrix.scaleM(model,0,modelScale,modelScale,modelScale)
            world.draw(vp,model,spec.model,eye,clearDepth=false)
        }else{
            val data=body?:return;val renderer=anatomy?:return
            Matrix.multiplyMM(bodyMvp,0,vp,0,bodyModel,0)
            val skin=data.parts.filter{it.layer=="skin"}
            if(selectedPart!=null&&spec.teachingKey==null){
                renderer.draw(bodyMvp,bodyModel,skin+AnatomyClothing.parts,null,.16f,cameraPosition=eye)
                Matrix.multiplyMM(detailMvp,0,vp,0,detailModel,0)
                renderer.draw(detailMvp,detailModel,listOf(selectedPart!!),selectedPart!!.id,cameraPosition=eye)
            }else{
                renderer.draw(bodyMvp,bodyModel,skin+AnatomyClothing.parts,null,cameraPosition=eye)
            }
            val guide=FirstAidSceneGuide.byKey(spec.teachingKey)
            if(guide!=null){
                val parts=FirstAidTeachingProps.visible(guide)
                renderTransforms.clear()
                for(part in parts){
                    val offset=FirstAidTeachingProps.offset(part.id,elapsed,guide);val t=FloatArray(16);Matrix.setIdentityM(t,0);Matrix.translateM(t,0,offset[0],offset[1],offset[2])
                    if(FirstAidCprPlacement.usesLandmark(part.id))cpr?.translation?.let{Matrix.translateM(t,0,it[0],it[1],it[2])}
                    renderTransforms[part.id]=t
                }
                val opaque=parts.filterNot{it.id=="teaching:film"||it.id.startsWith("teaching:film-")};val film=parts-opaque.toSet()
                renderer.draw(bodyMvp,bodyModel,opaque,null,partTransforms=renderTransforms,cameraPosition=eye)
                if(film.isNotEmpty())renderer.draw(bodyMvp,bodyModel,film,null,.44f,partTransforms=renderTransforms,cameraPosition=eye)
                if(spec.teachingKey?.startsWith("adult-collapse-response/")==true)renderer.draw(bodyMvp,bodyModel,FirstAidEquipment.parts,null,cameraPosition=eye)
            }
        }
    }
    private fun activate(id:String){
        val p=page?:return
        if(p.revision!=pendingPage.get()?.revision||!foreground||!inputAllowed||failed)return
        if(id.startsWith("observe:")){
            if(!partHit()){gate.reset();return}
        }
        post{if(foreground&&inputAllowed&&!failed&&pendingPage.get()?.revision==p.revision)onCommand(id,p.revision)}
    }
    private fun partHit():Boolean {
        val part=selectedPart?:return false
        val origin=FloatArray(4);val direction=FloatArray(4)
        Matrix.multiplyMV(origin,0,inverseDetail,0,lastRay.origin+1f,0);Matrix.multiplyMV(direction,0,inverseDetail,0,lastRay.direction+0f,0)
        return AnatomyPicking.distance(part.vertices,part.bounds,origin.copyOf(3),direction.copyOf(3))!=null
    }
    private fun releaseGl(){
        gate.reset();if(handle!=0L){CardboardBridge.pause(handle);CardboardBridge.destroy(handle);handle=0}
        anatomy?.release();anatomy=null;graphics.release()
        if(fbo!=0)G.glDeleteFramebuffers(1,intArrayOf(fbo),0);if(depth!=0)G.glDeleteRenderbuffers(1,intArrayOf(depth),0)
        val textures=intArrayOf(stereoTexture,panelTexture,toolsTexture).filter{it!=0}.toIntArray();if(textures.isNotEmpty())G.glDeleteTextures(textures.size,textures,0)
        fbo=0;depth=0;stereoTexture=0;panelTexture=0;toolsTexture=0;page=null
    }
    private fun fail(message:String){if(failed)return;failed=true;G.glBindFramebuffer(G.GL_FRAMEBUFFER,0);G.glClearColor(0f,0f,0f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT);post{if(foreground)onFailure(message)}}
}
