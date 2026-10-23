package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.opengl.GLSurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.`is`
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real SDK optics and rendered stereo on emulator. Preview head direction is explicitly synthetic. */
@RunWith(AndroidJUnit4::class)
class VrHeadsetFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private lateinit var original:String;private lateinit var worker:String;private lateinit var before:String
    private var originalHindi=false
    private fun field(value:Any,name:String)=value.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(value)
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun waitFor(message:String,test:()->Boolean){repeat(300){if(test())return;Thread.sleep(100)};error(message)}
    @Before fun isolate(){
        assumeTrue(BuildConfig.DEBUG&&Build.HARDWARE in setOf("ranchu","goldfish"))
        Store(context).use{original=it.workerId;originalHindi=it.hi;before=it.attempts().toString()+it.credentials().toString();worker=it.createProfile("Synthetic headset flow","Unspecified");it.switchProfile(worker);it.hi=false}
        ActivityScenario.launch<HeadsetActivity>(Intent(context,HeadsetActivity::class.java).putExtra("scenarioId","escape-first").putExtra("preview",true)).use{a->
            a.onActivity{CardboardBridge.initialise(it);CardboardBridge.useDefaultViewer()}
            // First-use Android confirmation is handled before inserting a viewer, just as in setup.
            repeat(15){
                ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Got it")?.firstOrNull{it.text?.toString()=="Got it"}?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(100)
            }
        }
    }
    @After fun restore(){
        if(!::worker.isInitialized)return
        context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
        VrLearningStore(context,worker).use{it.writableDatabase.delete("headset_practice","worker=?",arrayOf(worker))}
        Store(context).use{it.switchProfile(original);it.hi=originalHindi;it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
        Store(context).use{assertEquals("VR altered original assessments/credentials",before,it.attempts().toString()+it.credentials().toString())}
    }
    private fun intent(id:String)=Intent(context,VrTrainingActivity::class.java).putExtra("scenarioId",id).putExtra("englishAccepted",true).putExtra("preview",true)
    private fun session(a:ActivityScenario<VrTrainingActivity>):VrLearningSession{lateinit var s:VrLearningSession;a.onActivity{s=field(it,"session") as VrLearningSession};return s}
    private fun view(a:ActivityScenario<VrTrainingActivity>):VrSceneView{lateinit var v:VrSceneView;a.onActivity{v=field(it,"scene") as VrSceneView};return v}
    private fun settled(a:ActivityScenario<VrTrainingActivity>){
        waitFor("Stereo frame did not settle"){
            var ready=false;a.onActivity{host->
                val v=field(host,"scene") as? VrSceneView
                val page=field(host,"displayed") as? VrPage
                ready=v!=null&&page!=null&&v.renderedEyes==2&&v.frames>=3&&v.renderedRevision==page.revision
            };ready
        }
    }
    private fun command(a:ActivityScenario<VrTrainingActivity>,id:String){
        settled(a);val v=view(a);val before=v.frames;v.previewAimAt(id)
        try{waitFor("Visible headset target not reached: $id"){v.focusedTarget==id&&v.frames>before+1}}
        catch(e:Exception){capture("failed-target");throw IllegalStateException("$id: page=${aPage(a).title}, focused=${v.focusedTarget}, actions=${aPage(a).actions.map{it.id}}, frames=${v.frames}",e)}
        // A mechanical viewer generates touch down/up without Espresso's window-coordinate delay.
        val revision=aPage(a).revision
        a.onActivity{val at=SystemClock.uptimeMillis();v.onTouchEvent(MotionEvent.obtain(at,at,MotionEvent.ACTION_DOWN,10f,10f,0));v.onTouchEvent(MotionEvent.obtain(at,at,MotionEvent.ACTION_UP,10f,10f,0))}
        ins.waitForIdleSync()
        if(id !in setOf("rotate","larger","smaller","fit","replay","recenter"))waitFor("Short viewer press did not activate $id"){aPage(a).revision>revision}
        Thread.sleep(150)
    }
    private fun capture(name:String){
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("No emulator capture")
        File(context.getExternalFilesDir("validation"),"vr-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun gaze(a:ActivityScenario<VrTrainingActivity>,id:String){
        settled(a);val v=view(a);val revision=aPage(a).revision
        val visible=aPage(a)
        check(visible.actions.any{it.id==id}||visible.toolbar.any{it.id==id}||id.startsWith("observe:")) { "The test requested an invisible command: $id on ${visible.title}" }
        v.useDwell(true);val before=v.frames;v.previewAimAt(null)
        waitFor("Gaze did not leave the old target"){v.focusedTarget==null&&v.frames>before+1}
        v.previewAimAt(id)
        waitFor("Dwell did not activate $id"){aPage(a).revision>revision}
        v.useDwell(false);settled(a)
    }
    /** Separate input route: these results must never be reported as mechanical-button validation. */
    @Test fun gazeCompletesFireEvacuation()=gazeCompletesScenario("escape-first")
    @Test fun gazeCompletesAdultCollapseWithRequiredObservation()=gazeCompletesScenario("adult-collapse")
    @Test fun gazeCompletesScaldWithStepSpecificModels()=gazeCompletesScenario("thermal-burn")
    private fun gazeCompletesScenario(id:String){
            ActivityScenario.launch<VrTrainingActivity>(intent(id)).use{a->
                settled(a);gaze(a,"begin")
                if(id!="escape-first")waitFor("Anatomy did not load"){field(view(a),"anatomy")!=null}
                while(!session(a).finished){
                    val s=session(a)
                    if(id=="thermal-burn"&&s.current.getString("id") in setOf("cool","clothing","cover"))capture("gaze-scald-${s.current.getString("id")}")
                    gaze(a,"choose:"+s.choices().single{it.getBoolean("correct")}.getString("id"))
                    if(s.current.optBoolean("requiresObservation")){
                        gaze(a,"observe:"+s.current.getString("part"));capture("gaze-observation");gaze(a,"return")
                    }
                    gaze(a,"next")
                }
                capture("gaze-complete-$id");assertFalse(session(a).data.getBoolean("certifiable"))
            }
    }
    @Test fun gazeOptionalAnatomyAndControlsDoNotInventEvidenceOrRepeat(){
        ActivityScenario.launch<VrTrainingActivity>(intent("sprain-support")).use{a->
            settled(a);gaze(a,"begin");waitFor("Tibia model did not load"){field(view(a),"anatomy")!=null}
            val saved=session(a).data.toString();gaze(a,"observe:FMA24478")
            a.onActivity{assertEquals("inspect",field(it,"mode"))};assertEquals(saved,session(a).data.toString());capture("gaze-tibia-detail")
            val v=view(a);val oldScale=field(v,"modelScale") as Float
            v.useDwell(true);val before=v.frames;v.previewAimAt(null);waitFor("Gaze did not leave the model command"){v.focusedTarget==null&&v.frames>before+1};v.previewAimAt("larger")
            waitFor("Model did not enlarge"){(field(v,"modelScale") as Float)>oldScale}
            Thread.sleep(2200);assertEquals("A held gaze must not repeatedly enlarge the model",oldScale*1.15f,field(v,"modelScale") as Float,.001f);v.useDwell(false)
            gaze(a,"return");gaze(a,"pause");gaze(a,"controls");gaze(a,"dwell-mode");gaze(a,"dwell-mode")
            Thread.sleep(2200);a.onActivity{assertEquals("Turning dwell off must not turn itself back on",false,field(it,"dwellSelection"))}
            assertEquals(saved,session(a).data.toString())
        }
    }
    @Test fun stereoButtonLessonSupportsWrongFeedbackPauseResumeAndCompletion(){
        val id:String
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
            settled(a);command(a,"begin");val v=view(a);assertEquals(2,v.renderedEyes)
            id=session(a).data.getString("id")
            val matrices=CountDownLatch(1);var left=0f;var right=0f
            v.queueEvent{val handle=field(v,"handle") as Long;val l=FloatArray(16);val r=FloatArray(16);CardboardBridge.eyeFromHead(handle,0,l);CardboardBridge.eyeFromHead(handle,1,r);left=l[12];right=r[12];matrices.countDown()}
            assertTrue(matrices.await(5,TimeUnit.SECONDS));assertTrue("Two eyes must have real SDK offsets",kotlin.math.abs(left-right)>.03f);assertEquals(0f,left+right,.005f)
            val beforeResize=v.frames
            v.queueEvent{v.onSurfaceChanged(null,v.width,v.height)}
            waitFor("Same-context surface change lost the lesson card"){v.frames>beforeResize+2&&v.renderedRevision==aPage(a).revision}
            capture("fire-stereo")
            val wrong=session(a).choices().first{!it.getBoolean("correct")}.getString("id")
            command(a,"choose:$wrong");assertTrue(session(a).awaitingRetry);assertEquals(0,session(a).index)
            capture("feedback-stereo");command(a,"retry")
            val correct=session(a).choices().single{it.getBoolean("correct")}.getString("id")
            command(a,"choose:$correct");assertTrue(session(a).awaitingContinue);command(a,"next");assertEquals(1,session(a).index)
            val bytes=session(a).data.toString()
            ins.uiAutomation.executeShellCommand("input keyevent 4").close()
            waitFor("Back must open stereo pause"){var yes=false;a.onActivity{yes=field(it,"mode")=="pause"&&!it.isFinishing};yes}
            settled(a);capture("pause-stereo");assertEquals(bytes,session(a).data.toString())
            command(a,"resume");assertEquals(1,session(a).index)
        }
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
            settled(a);assertEquals(id,session(a).data.getString("id"));command(a,"resume")
            while(!session(a).finished){val s=session(a);command(a,"choose:"+s.choices().single{it.getBoolean("correct")}.getString("id"));command(a,"next")}
            settled(a);capture("complete-stereo")
            val data=session(a).data;assertEquals("headset-vr",data.getString("presentation"));assertFalse(data.getBoolean("certifiable"));assertEquals("not-assessed",data.getString("practicalCompetence"))
            assertEquals(id,VrLearningStore(context,worker).use{it.record(id)!!.getString("id")})
        }
    }
    @Test fun scaldStepsUseDifferentStereoModelsAndRecoverAfterRecreation(){
        ActivityScenario.launch<VrTrainingActivity>(intent("thermal-burn")).use{a->
            settled(a);command(a,"begin");val v=view(a)
            waitFor("Body did not load"){field(v,"anatomy")!=null}
            val scenes=mutableSetOf<String>()
            while(!session(a).finished){
                settled(a);scenes+=(field(aPage(a),"scene") as VrSceneSpec).teachingKey.orEmpty()
                if(session(a).current.getString("id") in setOf("cool","clothing","cover"))capture("scald-${session(a).current.getString("id")}")
                val s=session(a);command(a,"choose:"+s.choices().single{it.getBoolean("correct")}.getString("id"));command(a,"next")
            }
            assertTrue("Scald steps must not repeat a single scene",scenes.size>=3)
            val old=session(a).data.toString();a.recreate();settled(a);assertEquals(old,session(a).data.toString())
            waitFor("Body must reupload after EGL recreation"){field(view(a),"anatomy")!=null}
            capture("scald-recreated")
        }
    }
    @Test fun bothRequiredStructuresCanBeSelectedOnTheirActualMesh(){
        for(lesson in listOf("adult-collapse","recovery-monitor")){
            ActivityScenario.launch<VrTrainingActivity>(intent(lesson)).use{a->
                settled(a);command(a,"begin")
                waitFor("Anatomy did not load"){field(view(a),"anatomy")!=null}
                var observed=0
                while(!session(a).finished){
                    val s=session(a)
                    command(a,"choose:"+s.choices().single{it.getBoolean("correct")}.getString("id"))
                    if(s.current.optBoolean("requiresObservation")){
                        val part=s.current.getString("part")
                        command(a,"observe:$part");settled(a)
                        assertEquals("inspect",run{var m="";a.onActivity{m=field(it,"mode") as String};m})
                        assertTrue(aPage(a).body.contains(AnatomyKnowledge.info(part)!!.introText.take(30)))
                        assertTrue(session(a).events.objects().any{it.optString("kind")=="observation"&&it.optString("partId")==part})
                        capture("anatomy-$part");observed++;command(a,"return")
                    }
                    command(a,"next")
                }
                assertEquals("Required spatial inspection was never reached",1,observed)
            }
        }
    }
    @Test fun heldCancelledAndUnfocusedInputsCannotChooseAnAnswer(){
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
            settled(a);command(a,"begin");val v=view(a)
            val target="choose:"+session(a).choices().first().getString("id")
            val saved=session(a).data.toString()
            v.useDwell(true);v.previewAimAt(target)
            waitFor("Target not ready"){v.focusedTarget==target}
            val at=SystemClock.uptimeMillis()
            a.onActivity{v.onTouchEvent(MotionEvent.obtain(at,at,MotionEvent.ACTION_DOWN,10f,10f,0))}
            Thread.sleep(2100)
            assertEquals("A recenter hold must not complete dwell",saved,session(a).data.toString())
            a.onActivity{v.onTouchEvent(MotionEvent.obtain(at,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,10f,10f,0))}
            Thread.sleep(250);assertEquals(saved,session(a).data.toString())
            v.useDwell(false);v.previewAimAt(target);waitFor("Target not ready"){v.focusedTarget==target}
            a.onActivity{it.onKeyUp(KeyEvent.KEYCODE_ENTER,KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER))}
            Thread.sleep(150);assertEquals("Orphan key-up selected an answer",saved,session(a).data.toString())
            a.onActivity{v.onTouchEvent(MotionEvent.obtain(at,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,10f,10f,0))}
            Thread.sleep(150);assertEquals("Orphan touch-up selected an answer",saved,session(a).data.toString())
            a.onActivity{it.onWindowFocusChanged(false);v.trigger()}
            Thread.sleep(250);assertEquals(saved,session(a).data.toString())
            a.onActivity{assertEquals("pause",field(it,"mode"));it.onWindowFocusChanged(true)}
            command(a,"resume");assertEquals(saved,session(a).data.toString())
        }
    }
    @Test fun severeThermalStatusBlocksResumeAndDropsModelWorkload(){
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
            settled(a);command(a,"begin");val saved=session(a).data.toString()
            try{
                ins.uiAutomation.executeShellCommand("cmd thermalservice override-status 3").close()
                waitFor("Thermal pause not shown"){aPage(a).title=="Let the phone cool"}
                settled(a);assertNull(aPage(a).scene);assertTrue(aPage(a).actions.none{it.id=="resume"})
                command(a,"check-cooling");assertEquals("Let the phone cool",aPage(a).title);assertEquals(saved,session(a).data.toString())
            }finally{ins.uiAutomation.executeShellCommand("cmd thermalservice reset").close()}
            waitFor("Thermal recovery not shown"){aPage(a).actions.any{it.id=="resume"}}
            command(a,"resume");assertEquals(saved,session(a).data.toString())
        }
    }
    @Test fun recordEmulatorRenderingMetricsForFireAndAnatomy(){
        for(id in listOf("escape-first","thermal-burn"))ActivityScenario.launch<VrTrainingActivity>(intent(id)).use{a->
            settled(a);gaze(a,"begin");val v=view(a)
            if(id=="thermal-burn")waitFor("Body did not load"){field(v,"anatomy")!=null}
            Thread.sleep(6000);settled(a)
            val report=JSONObject().put("scenario",id).put("hardware",Build.HARDWARE).put("preview",true)
                .put("frames",v.frames).put("fpsLast2Seconds",v.observedFramesPerSecond)
                .put("renderWidth",field(v,"renderWidth")).put("renderHeight",field(v,"renderHeight"))
            File(context.getExternalFilesDir("validation"),"vr-metrics-$id.json").writeText(report.toString(2))
            assertTrue("Stereo renderer stopped",v.frames>20)
        }
    }
    @Test fun gazeFallbackKeepsGazeEnabledWhenAlreadySelectedInSetup(){
        val prefs=context.getSharedPreferences("headset-controls",0);val prior=if(prefs.contains("dwell"))prefs.getBoolean("dwell",false)else null
        try{ActivityScenario.launch<VrTrainingActivity>(intent("escape-first").putExtra("dwell",true)).use{a->
            settled(a);view(a).previewAimAt("dwell-mode")
            waitFor("Gaze fallback did not start the lesson"){var yes=false;a.onActivity{yes=field(it,"mode")=="lesson"};yes}
            a.onActivity{assertEquals(true,field(it,"dwellSelection"))}
            settled(a);view(a).previewAimAt("pause")
            waitFor("Gaze fallback left the learner without input"){var yes=false;a.onActivity{yes=field(it,"mode")=="pause"};yes}
        }}finally{prefs.edit().apply{if(prior==null)remove("dwell")else putBoolean("dwell",prior)}.commit()}
    }
    @Test fun headsetVoiceToggleDoesNotInheritOrChangeHandheldMute(){
        val handheld=context.getSharedPreferences("room-voice",0);val headset=context.getSharedPreferences("headset",0)
        val oldAutomatic=if(handheld.contains("automatic"))handheld.getBoolean("automatic",true)else null
        val oldAudio=if(headset.contains("audio"))headset.getBoolean("audio",true)else null
        try{
            handheld.edit().putBoolean("automatic",false).commit();headset.edit().putBoolean("audio",false).commit()
            ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
                settled(a);command(a,"begin");command(a,"pause");command(a,"audio")
                a.onActivity{assertTrue((field(it,"narrator") as TrainingNarrator).automaticEnabled)}
                assertFalse("Headset voice changed handheld settings",handheld.getBoolean("automatic",true))
                assertTrue(headset.getBoolean("audio",false))
                command(a,"audio");a.onActivity{assertFalse((field(it,"narrator") as TrainingNarrator).automaticEnabled)}
            }
        }finally{
            handheld.edit().apply{if(oldAutomatic==null)remove("automatic")else putBoolean("automatic",oldAutomatic)}.commit()
            headset.edit().apply{if(oldAudio==null)remove("audio")else putBoolean("audio",oldAudio)}.commit()
        }
    }
    @Test fun optionalAnatomyDetailsAreAvailableWithoutInventingObservationEvidence(){
        val body=VrAssets.loadBody(context)
        val plans=SpatialCatalog(context).data.getJSONObject("steps")
        val expected=plans.keys().asSequence().map{plans.getJSONObject(it)}.filter{it.optString("model")=="anatomy"}
            .map{it.optString("part")}.filter{it.startsWith("FMA")}.toSet()+"FMA7163"
        assertEquals(expected,body.parts.map{it.id}.toSet())
        for(id in listOf("adult-choking","sprain-support"))ActivityScenario.launch<VrTrainingActivity>(intent(id)).use{a->
            settled(a);command(a,"begin");waitFor("Anatomy did not load"){field(view(a),"anatomy")!=null}
            while(aPage(a).scene?.part.isNullOrEmpty()){
                val s=session(a);command(a,"choose:"+s.choices().single{it.getBoolean("correct")}.getString("id"));command(a,"next");settled(a)
            }
            val s=session(a);assertTrue(s.current.optString("part").isEmpty());val saved=s.data.toString()
            val part=aPage(a).scene!!.part!!
            if(aPage(a).scene!!.teachingKey!=null){command(a,"inspect-model");settled(a)}
            command(a,"observe:$part");settled(a)
            a.onActivity{assertEquals("inspect",field(it,"mode"))}
            assertEquals("Opening an optional description must not create credit",saved,session(a).data.toString())
            capture("optional-$part");command(a,"return");assertEquals(saved,session(a).data.toString())
        }
    }
    private fun aPage(a:ActivityScenario<VrTrainingActivity>):VrPage{lateinit var p:VrPage;a.onActivity{p=field(it,"displayed") as VrPage};return p}
    @Test fun directAdultEntryIsDeniedForSchoolAndEnglishConsentIsRequired(){
        context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit()
        ActivityScenario.launch<VrTrainingActivity>(intent("adult-collapse")).use{a->a.onActivity{assertTrue(views(it.window.decorView).any{v->v.tag=="vr-unavailable"})}}
        context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first").putExtra("englishAccepted",false)).use{a->a.onActivity{assertTrue(views(it.window.decorView).any{v->v.tag=="vr-unavailable"})}}
        assertTrue(VrLearningStore(context,worker).use{it.records().isEmpty()})
    }
    @Test fun everyAuthoredLessonCanBePaintedWithoutLosingAnAction(){
        val catalog=LearningCatalog(context)
        for(s in catalog.scenarios)for(step in s.getJSONArray("steps").objects()){
            val body=step.getString("cue")+"\n\n"+step.getString("question")
            val pages=VrTextPages.split(body)
            for(text in pages){
                val actions=step.getJSONArray("choices").objects().map{VrAction(it.getString("id"),it.getString("text"))}+if(pages.size>1)listOf(VrAction("more","Read more"))else emptyList()
                val painted=VrPanelPainter.paint(VrPage(1,step.getString("title"),s.getString("title"),text,actions,"English draft · headset practice"))
                assertEquals(actions.map{it.id},painted.hits.map{it.id});assertTrue(painted.hits.all{it.rect.bottom<=VrPanelPainter.HEIGHT});painted.bitmap.recycle()
            }
        }
    }
    /** Generated colour bands exercise GPU texture routing; they are not camera/hand evidence. */
    @Test fun monoCameraSharesTheImageWhileVrKeepsSeparateEyeRegions(){
        ActivityScenario.launch<VrTrainingActivity>(intent("escape-first")).use{a->
            settled(a);val v=view(a);val done=CountDownLatch(1);var failure:Throwable?=null
            v.queueEvent{
                val names=IntArray(1)
                try{
                    val handle=field(v,"handle") as Long
                    val pixels=java.nio.ByteBuffer.allocateDirect(64*8*4)
                    repeat(8){repeat(64){column->
                        val colour=if(column<25)intArrayOf(255,0,0)else if(column<39)intArrayOf(0,255,0)else intArrayOf(0,0,255)
                        pixels.put(colour[0].toByte()).put(colour[1].toByte()).put(colour[2].toByte()).put(255.toByte())
                    }};pixels.position(0)
                    android.opengl.GLES20.glGenTextures(1,names,0);android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D,names[0])
                    for(option in intArrayOf(android.opengl.GLES20.GL_TEXTURE_MIN_FILTER,android.opengl.GLES20.GL_TEXTURE_MAG_FILTER))android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D,option,android.opengl.GLES20.GL_LINEAR)
                    for(option in intArrayOf(android.opengl.GLES20.GL_TEXTURE_WRAP_S,android.opengl.GLES20.GL_TEXTURE_WRAP_T))android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D,option,android.opengl.GLES20.GL_CLAMP_TO_EDGE)
                    android.opengl.GLES20.glTexImage2D(android.opengl.GLES20.GL_TEXTURE_2D,0,android.opengl.GLES20.GL_RGBA,64,8,0,android.opengl.GLES20.GL_RGBA,android.opengl.GLES20.GL_UNSIGNED_BYTE,pixels)
                    fun sample(eye:Int):IntArray{
                        val result=java.nio.ByteBuffer.allocateDirect(4)
                        android.opengl.GLES20.glReadPixels(v.width*(1+eye*2)/4,v.height/2,1,1,android.opengl.GLES20.GL_RGBA,android.opengl.GLES20.GL_UNSIGNED_BYTE,result)
                        return IntArray(3){result.get(it).toInt() and 255}
                    }
                    CardboardBridge.composite(handle,v.width,v.height,names[0])
                    val left=sample(0);val right=sample(1)
                    assertTrue("Stereo left eye must use its red region: ${left.toList()}",left[0]>180&&left[1]<80&&left[2]<80)
                    assertTrue("Stereo right eye must use its blue region: ${right.toList()}",right[2]>180&&right[0]<80&&right[1]<80)
                    CardboardBridge.composite(handle,v.width,v.height,names[0],sharedCameraImage=true)
                    for(eye in 0..1){val colour=sample(eye);assertTrue("Mono eye $eye must sample the same green image centre: ${colour.toList()}",colour[1]>180&&colour[0]<80&&colour[2]<80)}
                    assertEquals(android.opengl.GLES20.GL_NO_ERROR,android.opengl.GLES20.glGetError())
                }catch(error:Throwable){failure=error}finally{if(names[0]!=0)android.opengl.GLES20.glDeleteTextures(1,names,0);done.countDown()}
            }
            assertTrue("Compositor GPU check timed out",done.await(10,TimeUnit.SECONDS));failure?.let{throw it}
        }
    }

}
