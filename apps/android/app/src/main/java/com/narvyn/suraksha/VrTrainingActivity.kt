package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.concurrent.Executors

/** Authentication/setup happen before insertion. Every in-viewer action stays stereoscopic. */
class VrTrainingActivity:Activity() {
    private lateinit var identity:Store
    private lateinit var journals:VrLearningStore
    private lateinit var catalog:LearningCatalog
    private lateinit var spatial:SpatialCatalog
    private lateinit var narrator:TrainingNarrator
    private lateinit var scene:VrSceneView
    private var session:VrLearningSession?=null
    private var ready=false;private var active=false;private var entered=false;private var bodyLoading=false
    private var preview=false;private var audio=true;private var visualsPaused=false;private var dwellSelection=false
    private var mode="lesson";private var returnMode="lesson";private var textPage=0;private var libraryPage=0;private var libraryCourse=""
    private var revision=0L;private var displayed:VrPage?=null;private var textPagesCount=1
    private var savedCandidate:JSONObject?=null;private var armedKey:Int?=null;private var armedAt=0L;private var cooling=false;private var thermalRegistered=false
    private var errorMessage="";private var inspection="";private var inspectionPart:String?=null;private var interruption=""
    private val loader=Executors.newSingleThreadExecutor()
    private val back by lazy{AppBackNavigation(this){pauseLesson()}}
    private val thermal=PowerManager.OnThermalStatusChangedListener{status->
        val wasCooling=cooling;cooling=status>=PowerManager.THERMAL_STATUS_SEVERE
        if(cooling&&active&&ready){interruption="The phone is warm. Remove the viewer and let it cool before continuing.";pauseLesson();if(audio)narrator.read(interruption,"en-IN")}
        else if(wasCooling&&active&&ready){interruption="The phone has cooled. Resume when you feel comfortable.";show()}
    }
    private fun allowed(s:JSONObject)=identity.isCurrentProfile()&&
        (getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${identity.workerId}","")!="School"||s.optString("audience")=="awareness")
    override fun onCreate(state:Bundle?){
        super.onCreate(state);identity=Store(this);catalog=LearningCatalog(this);spatial=SpatialCatalog(this)
        preview=BuildConfig.DEBUG&&Build.HARDWARE in setOf("ranchu","goldfish")&&intent.getBooleanExtra("preview",false)
        val requested=intent.getStringExtra("scenarioId")
        val lesson=catalog.scenarios.firstOrNull{it.getString("id")==requested}
        if(lesson==null||!allowed(lesson)){unavailable("This lesson is not available for the current learner.");return}
        if(!intent.getBooleanExtra("englishAccepted",false)){unavailable("Complete headset setup and choose the lesson language first.");return}
        if(!preview&&getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_GYROSCOPE)==null){unavailable("This phone has no gyroscope for headset tracking. Handheld learning is available.");return}
        try{CardboardBridge.initialise(this);check(CardboardBridge.nativeAvailable&&CardboardBridge.hasViewerProfile())}
        catch(e:Exception){unavailable("Set up your viewer profile before starting headset learning.");return}
        journals=VrLearningStore(this,identity.workerId)
        try{
            val restored=state?.getString("headsetAttempt")?.let{journals.record(it)}
            val prior=restored?:journals.latest(lesson.getString("id"),catalog.version)
            if(prior!=null){session=VrLearningSession.restore(prior,lesson,catalog.version,identity.workerId);savedCandidate=prior;mode=if(restored==null)"resume-choice"else"pause"}
            else {session=VrLearningSession.create(lesson,catalog.version,identity.workerId);journals.save(session!!);mode="ready"}
        }catch(e:Exception){unavailable("Saved headset practice could not be verified. The original record has been retained. Return to setup to choose another lesson.");return}
        narrator=TrainingNarrator(this){false}.also{it.languageProvider={"en-IN"}}
        audio=getSharedPreferences("headset",MODE_PRIVATE).getBoolean("audio",narrator.automaticEnabled)
        narrator.automaticOverride=audio
        requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
        dwellSelection=intent.getBooleanExtra("dwell",false)
        scene=VrSceneView(this,dwellSelection,preview).also{v->
            v.onCommand={id,token->if(active&&ready&&hasWindowFocus()&&token==displayed?.revision&&identity.isCurrentProfile())command(id)}
            v.onReady={if(active&&ready)speakCurrent()}
            v.onFailure={message->if(active){narrator.pause();v.pauseRuntime();ready=false;unavailable(message)}}
        }
        setContentView(scene);ready=true;back.enabled(true)
        @Suppress("DEPRECATION")
        if(Build.VERSION.SDK_INT>=30){window.insetsController?.systemBarsBehavior=android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;window.insetsController?.hide(WindowInsets.Type.systemBars())}
        else window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        getSystemService(PowerManager::class.java).addThermalStatusListener(mainExecutor,thermal);thermalRegistered=true
        show();ensureBody()
    }
    private fun unavailable(message:String){
        ready=false;errorMessage=message
        val root=column(24).apply{setBackgroundColor(Palette.canvas);tag="vr-unavailable"}
        root.add(label("Headset setup",26f,Palette.ink,true).asHeading(),bottom=16)
        root.add(label(message,17f),bottom=20)
        root.add(action("Return to setup"){startActivity(Intent(this,HeadsetActivity::class.java));finish()}.apply{tag="vr-return-setup"},bottom=10)
        root.add(action("Handheld learning",false,ActionRole.NEUTRAL){finish()})
        setContentView(paged(root,identity.hi))
    }
    private fun sessionMode():String {
        val s=session?:return "library"
        return if(s.finished)"complete"else if(s.awaitingContinue||s.awaitingRetry)"feedback"else"lesson"
    }
    private fun setMode(value:String){mode=value;if(value!="inspect")inspectionPart=null;textPage=0;show()}
    private fun pauseLesson(){if(!ready)return;narrator.pause();if(mode!="pause")returnMode=mode;setMode("pause")}
    private fun currentScene():VrSceneSpec? {
        val s=session?:return null
        val plan=spatial.step(s.scenario.getString("id"),s.current.getString("id"))?:return null
        if(mode=="inspect"&&inspectionPart!=null)return VrSceneSpec("anatomy",null,inspectionPart,"Anatomy detail")
        val observing=s.current.optBoolean("requiresObservation")&&!hasObservation(s)
        val part=if(observing)s.current.getString("part")else plan.optString("part").takeIf{it.isNotEmpty()}
        return VrSceneSpec(plan.getString("model"),if(observing)null else plan.optString("teachingKey").takeIf{it.isNotEmpty()},part,s.current.getString("title"))
    }
    private fun hasObservation(s:VrLearningSession)=s.events.objects().any{it.optString("kind")=="observation"&&it.optString("stepId")==s.current.getString("id")&&it.optString("partId")==s.current.optString("part")}
    private fun show(){
        if(!ready||!identity.isCurrentProfile())return
        val s=session?:return
        val title:String;val body:String;val actions:List<VrAction>
        var stepTitle=s.current.getString("title");var eyebrow="${s.scenario.getString("title")} · ${s.index.coerceAtMost(s.steps.lastIndex)+1}/${s.steps.size}"
        var toolbar=listOf(VrAction("pause","Pause"),VrAction("replay","Replay"),VrAction("recenter","Recenter"))
        when(mode){
            "ready"->{title="Your headset is ready";body="Sit comfortably. Look at Begin, then press the viewer button. Hold the button to recenter. If the button does not respond, look at Use gaze selection until its ring fills.";actions=listOf(VrAction("begin","Begin lesson"),VrAction("dwell-mode","Use gaze selection · look here"),VrAction("exit","Exit headset"))}
            "resume-choice"->{title="Continue or restart?";body="Saved at step ${s.index+1}: $stepTitle. Earlier practice remains saved.";actions=listOf(VrAction("resume","Continue"),VrAction("restart-confirm","Start from step 1"))}
            "pause"->{title=if(cooling)"Let the phone cool"else"Take your time";body=interruption.ifEmpty{"Your step is saved. Stay seated in a clear space. Hold the viewer button to recenter, or select Recenter below."};actions=if(cooling)listOf(VrAction("check-cooling","Check phone temperature"),VrAction("exit","Exit headset"))else listOf(VrAction("resume","Resume lesson"),VrAction("library","Choose another lesson"),VrAction("controls","Viewer and model controls"),VrAction("exit","Exit headset"));toolbar=listOf(VrAction("audio",if(audio)"Mute voice"else"Voice on"),VrAction("motion",if(visualsPaused)"Play visual"else"Pause visual"),VrAction("recenter","Recenter"))}
            "controls"->{title="Make it comfortable";body="The headset follows your head direction. Models can be enlarged and rotated while your viewpoint stays in place.";actions=listOf(VrAction("inspect-controls","Model controls"),VrAction("dwell-mode",if(dwellSelection)"Use the viewer button"else"Use gaze selection · look here"),VrAction("pause","Return to pause menu"))}
            "courses"->{title="What will you practise?";eyebrow="Your learning library";body="Choose a course. Lessons run offline, with the same reviewed-status labels as handheld learning.";actions=listOf(VrAction("course:fire","Fire Safety"),VrAction("course:first-aid","First Aid"),VrAction("return","Return to current lesson"))}
            "library"->{
                val lessons=catalog.scenarios.filter{it.getString("course")==libraryCourse&&allowed(it)}
                val pages=((lessons.size+2)/3).coerceAtLeast(1);libraryPage=libraryPage.coerceIn(0,pages-1)
                title=if(libraryCourse=="fire")"Fire Safety"else"First Aid";eyebrow="Lesson ${libraryPage+1} of $pages"
                body="Point at a lesson and press the viewer button."
                actions=lessons.drop(libraryPage*3).take(3).map{VrAction("lesson:${it.getString("id")}",it.getString("title"))}+
                    if(pages>1)listOf(VrAction("library-more","More lessons"))else emptyList()
                toolbar=listOf(VrAction("courses","Courses"),VrAction("return","Return"),VrAction("recenter","Recenter"))
            }
            "feedback"->{
                title=if(s.lastCorrect)"Why this helps"else"Review and try again"
                val inspect=s.awaitingContinue&&s.current.optBoolean("requiresObservation")&&!hasObservation(s)
                body=s.current.getString("explanation")+if(inspect)"\n\nBefore continuing, point at the highlighted ${AnatomyKnowledge.info(s.current.getString("part"))?.title?:"body part"} and press the viewer button to inspect it."else""
                actions=listOf(VrAction(if(s.awaitingRetry)"retry"else if(inspect)"inspect-prompt"else"next",if(s.awaitingRetry)"Try again"else if(inspect)"Show inspection guidance"else"Next step"),VrAction("source","Step source"))
            }
            "complete"->{title="Lesson complete";body="You completed ${s.steps.size} decisions. This headset record is saved on this phone. Physical skills still need supervised practice.";actions=listOf(VrAction("library","Choose another lesson"),VrAction("restart-confirm","Practise again"),VrAction("exit","Exit headset"));eyebrow="Your headset learning record"}
            "source"->{title="Source for this step";body=s.current.getString("source");actions=listOf(VrAction("return","Return to lesson"));eyebrow="Authored draft · source reference"}
            "inspect"->{title="Look closer";body=inspection.ifEmpty{"Look at the model and turn your head naturally. These illustrative models show the lesson context; they do not inspect a real casualty or piece of equipment."};actions=listOf(VrAction("rotate","Rotate model"),VrAction("larger","Larger model"),VrAction("smaller","Smaller model"),VrAction("return","Return to lesson"));toolbar=listOf(VrAction("fit","Reset model"),VrAction("replay","Replay"),VrAction("recenter","Recenter"))}
            "error"->{title="Unable to continue";body=errorMessage;actions=listOf(VrAction("return","Return to saved step"),VrAction("exit","Exit headset"))}
            else->{title=stepTitle;body=s.current.getString("cue")+"\n\n"+s.current.getString("question");actions=s.choices().map{VrAction("choose:${it.getString("id")}",it.getString("text"))}}
        }
        val pages=VrTextPages.split(body);textPagesCount=pages.size;textPage=textPage.coerceIn(0,pages.lastIndex)
        val finalActions=actions+if(pages.size>1)listOf(VrAction("read-more","Read more (${textPage+1}/${pages.size})"))else emptyList()
        // Menus with four commands use the toolbar for paging instead of hiding a command.
        val shownActions=if(finalActions.size>4)actions else finalActions
        if(finalActions.size>4)toolbar=listOf(VrAction("read-more","Read more"),VrAction("return","Return"),VrAction("recenter","Recenter"))
        val footer=if(preview)"EMULATOR PREVIEW · no physical head tracking"else"Headset practice · English draft · saved locally"
        val sceneSpec=if(cooling)null else currentScene()
        val page=VrPage(++revision,title,eyebrow,pages[textPage],shownActions,footer,s.index.toFloat()/s.steps.size,sceneSpec,visualsPaused||mode in setOf("pause","source","library","courses","resume-choice","ready"),toolbar)
        displayed=page;scene.present(page)
        if(mode !in setOf("pause","library","courses","resume-choice"))speakCurrent()
    }
    private fun speakCurrent(){
        if(!active||!ready||!audio||!hasWindowFocus())return
        val p=displayed?:return
        // Choice labels include deliberate errors and are never spoken as directions.
        narrator.show(p.title+". "+p.body,"en-IN")
    }
    private fun mutate(block:(VrLearningSession)->Unit):Boolean {
        val s=session?:return false
        if(!allowed(s.scenario)){finish();return false}
        val old=JSONObject(s.data.toString())
        return try{block(s);journals.save(s);true}catch(e:Exception){
            session=VrLearningSession.restore(old,s.scenario,catalog.version,identity.workerId)
            errorMessage="The last saved step is still available. Check the phone's storage before continuing. If a required body part has not been inspected, return and select the highlighted part first."
            narrator.pause();setMode("error");false
        }
    }
    private fun command(id:String){
        val p=displayed?:return
        val permitted=p.actions.any{it.id==id}||p.toolbar.any{it.id==id}||id=="inspect-model"||id.startsWith("observe:")
        if(!permitted||!identity.isCurrentProfile())return
        when {
            id.startsWith("choose:")->{if(mode!="lesson")return;if(mutate{it.choose(id.removePrefix("choose:"))})setMode("feedback")}
            id.startsWith("observe:")->{
                val part=id.removePrefix("observe:");val s=session?:return
                if(mode !in setOf("lesson","feedback","inspect"))return
                // Presentation-only anatomy details never create an invented lesson observation.
                if(!s.finished&&s.current.optString("part")==part&&!mutate{it.observe(part)})return
                inspectionPart=part;inspection=AnatomyKnowledge.info(part)?.introText.orEmpty();setMode("inspect")
            }
            id.startsWith("course:")->{libraryCourse=id.removePrefix("course:");libraryPage=0;setMode("library")}
            id.startsWith("lesson:")->{selectLesson(id.removePrefix("lesson:"))}
            else->when(id){
                "begin"->setMode(sessionMode())
                "next"->{if(mutate{it.next()})setMode(sessionMode())}
                "retry"->{if(mutate{it.retry()})setMode("lesson")}
                "resume"->{cooling=getSystemService(PowerManager::class.java).currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE;if(cooling){interruption="The phone is still warm. Remove the viewer and let it cool.";setMode("pause")}else{interruption="";setMode(sessionMode())}}
                "check-cooling"->{cooling=getSystemService(PowerManager::class.java).currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE;interruption=if(cooling)"The phone is still warm. Remove the viewer and let it cool."else"The phone has cooled. Resume when you feel comfortable.";setMode("pause")}
                "return"->{setMode(sessionMode())}
                "restart-confirm"->{val old=session!!;session=VrLearningSession.create(old.scenario,catalog.version,identity.workerId);try{journals.save(session!!);savedCandidate=null;setMode("lesson")}catch(e:Exception){session=old;errorMessage="A new attempt could not be saved. Your existing attempt is retained.";setMode("error")}}
                "library","courses"->setMode("courses")
                "library-more"->{libraryPage++;val count=catalog.scenarios.count{it.getString("course")==libraryCourse&&allowed(it)};if(libraryPage*3>=count)libraryPage=0;setMode("library")}
                "pause"->pauseLesson()
                "controls"->setMode("controls")
                "dwell-mode"->{dwellSelection=if(mode=="ready")true else !dwellSelection;scene.useDwell(dwellSelection);getSharedPreferences("headset-controls",MODE_PRIVATE).edit().putBoolean("dwell",dwellSelection).apply();setMode(if(mode=="ready")sessionMode()else"controls")}
                "read-more"->{textPage=(textPage+1)%textPagesCount;show()}
                "replay"->{scene.replayVisual();if(audio){val q=displayed!!;narrator.read(q.title+". "+q.body,"en-IN")}}
                "recenter"->scene.recenter()
                "source"->setMode("source")
                "inspect-model"->{val part=currentScene()?.part;inspectionPart=part?.takeIf{AnatomyKnowledge.info(it)!=null};inspection=inspectionPart?.let{AnatomyKnowledge.info(it)?.introText}.orEmpty();setMode("inspect")}
                "inspect-controls"->{inspection="";inspectionPart=null;setMode("inspect")}
                "inspect-prompt"->{inspection="Point the centre reticle at the highlighted ${AnatomyKnowledge.info(session!!.current.getString("part"))?.title?:"structure"} in the body, then press the viewer button. Selecting the actual model opens its description. Rotate the model if needed.";setMode("inspect")}
                "rotate"->scene.rotateModel()
                "larger"->scene.resizeModel(1.15f)
                "smaller"->scene.resizeModel(1/1.15f)
                "fit"->scene.resetModel()
                "motion"->{visualsPaused=!visualsPaused;show()}
                "audio"->{audio=!audio;getSharedPreferences("headset",MODE_PRIVATE).edit().putBoolean("audio",audio).apply();narrator.automaticOverride=audio;if(!audio)narrator.pause();show();if(audio){val q=displayed!!;narrator.read(q.title+". "+q.body,"en-IN")}}
                "exit"->{narrator.pause();finish()}
            }
        }
    }
    private fun selectLesson(id:String){
        val lesson=catalog.scenarios.firstOrNull{it.getString("id")==id&&allowed(it)}?:return
        try{
            val prior=journals.latest(id,catalog.version)
            session=if(prior!=null)VrLearningSession.restore(prior,lesson,catalog.version,identity.workerId)else VrLearningSession.create(lesson,catalog.version,identity.workerId).also{journals.save(it)}
            scene.resetModel();setMode(if(prior!=null)"resume-choice"else"lesson");ensureBody()
        }catch(e:Exception){errorMessage="The chosen lesson could not be opened. Existing headset records have been retained.";setMode("error")}
    }
    private fun ensureBody(){
        if(!ready||bodyLoading||session?.scenario?.optString("course")!="first-aid")return
        bodyLoading=true
        loader.submit{val result=runCatching{VrAssets.loadBody(this)};runOnUiThread{
            if(isDestroyed||isFinishing||!ready||!identity.isCurrentProfile())return@runOnUiThread
            result.onSuccess{scene.body(it)}.onFailure{errorMessage="The body model could not load. Return to handheld learning to check offline content.";setMode("error")}
        }}
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean {
        if(ready&&keyCode in setOf(KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_SPACE)){if(event.repeatCount==0){armedKey=keyCode.takeIf{active&&hasWindowFocus()};armedAt=android.os.SystemClock.elapsedRealtime();if(armedKey!=null)scene.buttonDown()};return true}
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode:Int,event:KeyEvent):Boolean {
        if(ready&&keyCode in setOf(KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_SPACE)){if(armedKey==keyCode){scene.buttonUp(!event.isCanceled&&hasWindowFocus(),android.os.SystemClock.elapsedRealtime()-armedAt>=1100);armedKey=null};return true}
        return super.onKeyUp(keyCode,event)
    }
    override fun onResume(){super.onResume();active=true;if(ready){if(!identity.isCurrentProfile()||session?.scenario?.let{!allowed(it)}==true){finish();return};cooling=getSystemService(PowerManager::class.java).currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE;if(entered||cooling){mode="pause";if(cooling)interruption="The phone is warm. Remove the viewer and let it cool before continuing.";show()};entered=true;scene.allowInput(hasWindowFocus());scene.resumeRuntime()}}
    override fun onPause(){active=false;armedKey=null;if(ready){scene.allowInput(false);narrator.pause();scene.pauseRuntime();mode="pause"};super.onPause()}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(!hasFocus)armedKey=null;if(ready){scene.allowInput(hasFocus&&active);if(!hasFocus){narrator.pause();if(!cooling)interruption="Practice paused while another screen needed your attention.";pauseLesson()}else speakCurrent()}}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);session?.let{out.putString("headsetAttempt",it.data.getString("id"))}}
    override fun onDestroy(){
        if(::scene.isInitialized)scene.closeRuntime()
        if(thermalRegistered)getSystemService(PowerManager::class.java).removeThermalStatusListener(thermal)
        loader.shutdownNow();if(::narrator.isInitialized)narrator.close();if(::journals.isInitialized)journals.close();if(::identity.isInitialized)identity.close();back.close();super.onDestroy()
    }
}
