package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

/** An anchored, interruption-safe learning sequence. It never awards practical competence. */
class FirstAidBayActivity : Activity() {
    private lateinit var store: Store
    private lateinit var catalog: LearningCatalog
    private lateinit var records: SkillPracticeStore
    private lateinit var narrator: TrainingNarrator
    private var attempt: FirstAidBay? = null
    private var model: AnatomyData? = null
    private var scene: AnatomyScene? = null
    private var camera = false
    private var resumed = false
    private var feedback: FirstAidBay.Feedback? = null
    private var selectedScenario=FirstAidScenarioCatalog.DEFAULT_ID
    private var practising=false
    private var resumeChoicePending=false
    private var dock:AnatomyViewDock?=null
    private lateinit var dockHost:LinearLayout
    private lateinit var sceneRow:LinearLayout
    private lateinit var lessonPanel:LinearLayout
    private var shortScreen=false
    private val loader = Executors.newSingleThreadExecutor()
    private val back by lazy { AppBackNavigation(this) { goBack() } }
    private lateinit var stage: TextView
    private lateinit var cue: TextView
    private lateinit var status: TextView
    private lateinit var place: Button
    private lateinit var pause: Button
    private lateinit var actions: LinearLayout
    private lateinit var viewport: FrameLayout
    private var currentLayer: String? = null
    private var lastVisualKey=""
    private fun t(en:String,hi:String)=if(store.hi)hi else en

    private fun allowed() = store.isCurrentProfile() && getSharedPreferences("learning-profile", MODE_PRIVATE)
        .getString("context-${store.workerId}", "Independent learning") != "School"

    private fun currentDefinition(record:org.json.JSONObject):Boolean {
        if(record.optString("scenarioId")==FirstAidBay.SCENARIO_ID)return !record.has("scenarioFingerprint") && !record.has("scenarioDefinitionVersion")
        val definition=FirstAidScenarioCatalog.find(record.optString("scenarioId"))?:return false
        return record.optInt("scenarioDefinitionVersion")==definition.version && record.optString("scenarioFingerprint")==definition.fingerprint
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = Store(this); catalog = LearningCatalog(this)
        records = SkillPracticeStore(this, store.workerId, "first-aid-bay.db")
        narrator = TrainingNarrator(this) { store.hi }; back.enabled(true)
        selectedScenario=state?.getString("scenario")?:intent.getStringExtra("scenarioId")?:FirstAidScenarioCatalog.DEFAULT_ID
        if(FirstAidScenarioCatalog.find(selectedScenario)==null)selectedScenario=FirstAidScenarioCatalog.DEFAULT_ID
        if (!allowed()) { finish(); return }
        val savedId = state?.getString("attempt") ?: intent.getStringExtra("attemptId")
        val allRecords = records.records()
        val requested = savedId?.let { id -> allRecords.firstOrNull { it.optString("id") == id } }
        if (requested != null && (requested.optString("contentVersion") != catalog.version || requested.optInt("bayVersion") != FirstAidBay.VERSION || !currentDefinition(requested))) {
            val root = column(20).apply { setBackgroundColor(Palette.canvas) }
            root.add(label("Earlier practice record", 22f, Palette.ink, true).asHeading(), bottom = 12)
            val details = column()
            details.add(label("Content ${requested.optString("contentVersion")} · ${requested.optInt("index")} stages recorded. This older record is preserved without regrading.", 16f), bottom = 12)
            requested.optJSONArray("answers")?.objects()?.forEach { event ->
                details.add(label("${event.optString("state")} · ${event.optString("actionId", event.optString("kind"))}\n${event.optString("mode")} · ${event.optString("reason")}", 14f), bottom = 10)
            }
            root.addView(paged(details, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
            root.add(action("Back to progress", false) { finish() }, top = 10)
            setContentView(root); trainingInsets(root); return
        }
        val saved = allRecords.firstOrNull {
            (savedId == null || it.optString("id") == savedId) && it.optString("contentVersion") == catalog.version && currentDefinition(it) &&
                (savedId != null || if(intent.getBooleanExtra("legacyBay",false)) it.optString("scenarioId")==FirstAidBay.SCENARIO_ID else it.optString("scenarioId")==selectedScenario) &&
                (savedId != null || !it.optBoolean("finished") && !it.optBoolean("stopped"))
        }
        attempt = saved?.let { runCatching { FirstAidBay.restore(it, store.workerId, catalog.version) }.getOrNull() }
        if (saved != null && attempt == null || savedId != null && requested == null) {
            val root = column(20).apply { setBackgroundColor(Palette.canvas) }
            root.add(label("Record unavailable", 22f, Palette.ink, true), bottom = 12)
            root.add(label("This saved attempt could not be verified for the current learner. Its original data has not been changed.", 16f), bottom = 16)
            root.add(action("Back to progress") { finish() }); setContentView(root); trainingInsets(root); return
        }
        attempt?.definition?.id?.let { selectedScenario = it }
        camera = state?.getBoolean("camera") ?: (attempt?.events?.objects()?.lastOrNull { it.optString("kind") == "action" }?.optString("mode") == "ar")
        resumeChoicePending = state?.getBoolean("resumeChoicePending") ?: hasSavedDecisions()
        if (attempt?.finished == true || attempt?.stopped == true) summary() else introduction()
        loader.execute {
            runCatching { AnatomyData.load(this) }.fold({ loaded -> runOnUiThread {
                if (!isDestroyed && !isFinishing) { model = loaded; if (resumed && ::viewport.isInitialized) attachScene() }
            } }, { runOnUiThread {
                if (!isDestroyed && !isFinishing) notice("Body pack unavailable", "The anatomy pack could not be verified. Reinstall the current APK. Saved learning records remain available.")
            } })
        }
    }

    private fun hasSavedDecisions() = attempt?.let { bay ->
        !bay.finished && !bay.stopped && (bay.index > 0 || bay.events.objects().any { it.optString("kind") == "action" })
    } == true

    private fun newAttempt(): FirstAidBay {
        val legacy = attempt?.scenarioId == FirstAidBay.SCENARIO_ID || (attempt == null && intent.getBooleanExtra("legacyBay", false))
        return if (legacy) FirstAidBay.create(store.workerId, catalog.version)
            else FirstAidBay.createScenario(store.workerId, catalog.version, selectedScenario)
    }

    private fun resumeChoice() {
        val bay = attempt ?: return
        val root = column(20).apply { setBackgroundColor(Palette.canvas) }
        root.add(label(bay.title(store.hi), 22f, Palette.ink, true).asHeading(), bottom = 12)
        val body = column()
        body.add(label(t("Saved at step ${bay.index + 1} of ${bay.steps.size}", "चरण ${bay.index + 1} / ${bay.steps.size} पर सहेजा गया"), 18f, Palette.ink, true), bottom = 8)
        body.add(label(bay.current.title(store.hi), 16f, Palette.muted), bottom = 12)
        body.add(label(t("Earlier attempt stays saved.", "पिछला अभ्यास सहेजा रहेगा।"), 14f, Palette.muted), bottom = 16)
        body.add(label(t("Practice mode", "अभ्यास का तरीका"), 13f, Palette.muted), bottom = 6)
        val modes = LinearLayout(this).apply { isBaselineAligned = false }
        modes.addView(action(t("AR camera", "AR कैमरा"), camera) { camera = true; resumeChoice() }.apply {
            tag = "bay-resume-mode-ar"; isSelected = camera
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        modes.addView(action(t("3D screen", "3D स्क्रीन"), !camera) { camera = false; resumeChoice() }.apply {
            tag = "bay-resume-mode-3d"; isSelected = !camera
        }, LinearLayout.LayoutParams(0, -2, 1f))
        body.add(modes)
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action(t("Continue", "जारी रखें")) {
            if (!allowed()) { finish(); return@action }
            resumeChoicePending = false; introduction(); enter(camera)
        }.apply { tag = "bay-resume-continue" }, top = 12)
        root.add(action(t("Start from step 1", "चरण 1 से शुरू करें"), false) {
            if (!allowed()) { finish(); return@action }
            attempt = newAttempt(); save(); feedback = null
            resumeChoicePending = false; introduction(); enter(camera)
        }.apply { tag = "bay-resume-restart" }, top = 8)
        root.add(action(t("Back", "वापस"), false, ActionRole.NEUTRAL) { finish() }, top = 8)
        setContentView(root); trainingInsets(root)
    }

    private fun introduction() {
        practising=false;dock=null;scene?.close();scene=null;narrator.clear()
        if (resumeChoicePending && hasSavedDecisions()) { resumeChoice(); return }
        val root = column(20).apply { setBackgroundColor(Palette.canvas) }
        root.add(label(t("First-aid practice bay","प्राथमिक उपचार अभ्यास"), 22f, Palette.ink, true).asHeading(), bottom = 12)
        val body = column()
        val scenario=FirstAidScenarioCatalog.find(selectedScenario)!!
        body.add(label(attempt?.title(store.hi)?:scenario.title(store.hi),22f,Palette.ink,true),bottom=10)
        body.add(label(if(attempt!=null)t("Saved at step ${attempt!!.index+1}. Continue without losing earlier decisions.","चरण ${attempt!!.index+1} पर सहेजा गया। पिछले निर्णयों के साथ जारी रखें।")else scenario.summary(store.hi),16f),bottom=16)
        body.add(practiceTool(t("Choose situation","स्थिति चुनें"),PracticeIcon.TARGET){chooseSituation()},bottom=14)
        body.add(label(t("Inspect the virtual casualty, choose an action and learn from the result.","आभासी घायल व्यक्ति को देखें, निर्णय लें और परिणाम से सीखें।"),15f,Palette.muted),bottom=14)
        body.add(practiceTool(t("About this practice","अभ्यास के बारे में"),PracticeIcon.INFO){notice(t("Practice, not treatment","सीखने का अभ्यास"),t("Adult simulation. No real calls or physical treatment. Content and Hindi translation are source-checked drafts awaiting qualified review. Side-lying views are illustrative, not a recovery-position demonstration.","वयस्क का आभासी अभ्यास। कोई असली कॉल या उपचार नहीं। सामग्री और हिंदी अनुवाद के विशेषज्ञ अनुमोदन बाकी हैं। करवट का दृश्य केवल संकेत है, रिकवरी पोज़िशन का प्रदर्शन नहीं।"))})
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action(if (attempt == null) t("Start in AR","AR में शुरू करें") else t("Continue in AR","AR में जारी रखें")) { enter(true) }, top = 12)
        root.add(action(t("Use 3D practice","3D अभ्यास करें"), false) { enter(false) }, top = 8)
        root.add(action(t("Back","वापस"), false, ActionRole.NEUTRAL) { finish() }, top = 8)
        setContentView(root); trainingInsets(root)
    }

    private fun chooseSituation(){
        val list=FirstAidScenarioCatalog.all
        PageDialogBuilder(this).setTitle(t("Choose a situation","स्थिति चुनें")).setItems(list.map{it.title(store.hi)}.toTypedArray()){_,i->
            if (practising) { attempt?.pause(); save() }
            selectedScenario = list[i].id
            attempt = records.records().firstOrNull { it.optString("scenarioId") == selectedScenario &&
                it.optString("contentVersion") == catalog.version && !it.optBoolean("finished") && !it.optBoolean("stopped") && currentDefinition(it)
            }?.let { runCatching { FirstAidBay.restore(it, store.workerId, catalog.version) }.getOrNull() }
            camera = attempt?.events?.objects()?.lastOrNull { it.optString("kind") == "action" }?.optString("mode") == "ar"
            resumeChoicePending = hasSavedDecisions(); introduction()
        }.show()
    }

    private fun enter(ar: Boolean) {
        if (!allowed()) { finish(); return }
        if (ar && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 703); return
        }
        if (attempt == null) attempt = newAttempt()
        if (attempt!!.paused) attempt!!.resume()
        save(); camera = ar; feedback = null; build()
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 703) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) enter(true)
            else notice("Camera permission", "AR needs camera access. You can use 3D practice, or allow the camera in Android app settings.")
        }
    }

    private fun build() {
        practising=true;dock=null;lastVisualKey=""
        scene?.close(); scene = null; currentLayer = null
        shortScreen=resources.configuration.screenHeightDp<480
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(practiceTool("",PracticeIcon.BACK,t("Save and go back","सहेजें और वापस जाएँ")) { goBack() },LinearLayout.LayoutParams(dp(48),dp(48)))
        header.addView(label(t("First-aid practice","प्राथमिक उपचार"),19f,Palette.ink,true).asHeading().apply{
            setPadding(dp(8),0,0,0);maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
        },LinearLayout.LayoutParams(0,-2,1f))
        header.addView(action(t("More","और"),false,ActionRole.NEUTRAL){options()})
        root.add(header,bottom=if(shortScreen)4 else 8)
        stage=label("",17f,Palette.ink,true).asHeading()
        cue=label("",14f)
        status=label("Loading the offline body pack…",12f,Palette.muted)
        viewport=FrameLayout(this)
        dockHost=column(0).apply{visibility=View.GONE}
        lessonPanel=column(0)
        place=action(t("Place casualty","आभासी व्यक्ति रखें")){scene?.place()}.apply{isEnabled=false;visibility=if(camera)View.VISIBLE else View.GONE}
        actions=column(0)
        sceneRow=LinearLayout(this).apply{orientation=if(shortScreen)LinearLayout.HORIZONTAL else LinearLayout.VERTICAL}
        if(shortScreen){
            listOf(stage,cue,status).forEach{it.maxLines=1;it.ellipsize=android.text.TextUtils.TruncateAt.END}
            lessonPanel.add(stage,bottom=4);lessonPanel.add(cue,bottom=4);lessonPanel.add(status,bottom=6)
            lessonPanel.add(place,bottom=4);lessonPanel.add(actions)
            sceneRow.addView(viewport,LinearLayout.LayoutParams(0,-1,1f))
            val side=FrameLayout(this).apply{
                addView(lessonPanel,FrameLayout.LayoutParams(-1,-1));addView(dockHost,FrameLayout.LayoutParams(-1,-1))
            }
            sceneRow.addView(side,LinearLayout.LayoutParams(dp((resources.configuration.screenWidthDp*.4f).toInt().coerceIn(280,360)),-1).apply{marginStart=dp(8)})
            root.addView(sceneRow,LinearLayout.LayoutParams(-1,0,1f))
        }else{
            root.add(stage,bottom=6);root.add(cue,bottom=6);root.add(status,bottom=6)
            sceneRow.addView(viewport,LinearLayout.LayoutParams(-1,0,1f))
            sceneRow.addView(dockHost,LinearLayout.LayoutParams(-1,-2))
            root.addView(sceneRow,LinearLayout.LayoutParams(-1,0,1f))
            root.add(place,top=6);root.add(actions,top=8)
        }
        val toolbar=LinearLayout(this)
        pause=practiceTool(t("Pause","रोकें"),PracticeIcon.PAUSE){togglePause()}
        toolbar.addView(pause,LinearLayout.LayoutParams(0,-2,1f))
        toolbar.addView(practiceTool(t("View","दृश्य"),PracticeIcon.VIEW){viewControls()},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4)})
        toolbar.addView(practiceTool(t("Audio","आवाज़"),PracticeIcon.AUDIO){narrator.showControls()},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4)})
        toolbar.addView(practiceTool(t("Cue","संकेत"),PracticeIcon.INFO){cueDetails()},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4)})
        root.add(toolbar,top=if(shortScreen)4 else 8)
        setContentView(root);trainingInsets(root);attachScene();renderStage()
    }

    private fun attachScene() {
        val data = model ?: return
        if (!practising || !resumed || isFinishing || isDestroyed || !::viewport.isInitialized || scene != null || attempt?.finished == true || attempt?.stopped == true) return
        val next = AnatomyScene(this, data, camera, practice = camera, bay = true)
        next.change { it.copy(supine = true, skinOverlay = false,scale=if(camera).4f else 1f) }
        next.onStatus = { message ->
            status.text = if (camera) message else "3D practice · source-checked draft"
            place.isEnabled = next.canPlace && attempt?.paused == false
            place.visibility = if (camera && !next.placed && attempt?.paused == false) View.VISIBLE else View.GONE
            updateAvailability()
        }
        next.onPicked = { part, mode ->
            val bay = attempt
            if (bay != null && feedback == null && !bay.finished && bay.current.partId != null)
                act("select:${part.id}", if (mode == "ar-exploration") "ar" else "3d")
        }
        next.onToolPicked = { id, mode ->
            if (attempt?.current?.id == "aed_power" && feedback == null)
                act(if (id == FirstAidEquipment.POWER) "aed-power-on" else "inspect:$id", if (mode == "ar-exploration") "ar" else "3d")
        }
        next.onCuePicked={region,mode->
            val bay=attempt
            if(bay!=null&&feedback==null&&!bay.finished&&bay.currentVisual.bodyRegion.name==region){
                bay.current.actions.firstOrNull{it.spatial&&(it.id.startsWith("inspect:")||it.id=="apply-pressure")}?.let{act(it.id,if(mode=="ar-exploration")"ar"else"3d")}
            }
        }
        scene = next; viewport.removeAllViews(); viewport.addView(next, FrameLayout.LayoutParams(-1, -1))
        if (resumed && attempt?.paused == false) next.resume()
        showLayer()
    }

    private fun showLayer() {
        val bay = attempt ?: return; val data = model ?: return; if (bay.finished) return
        val layer = data.parts.firstOrNull { it.id == bay.current.partId }?.layer ?: "skin"
        if (layer != currentLayer) { scene?.layer(layer); scene?.change { it.copy(skinOverlay = layer!="skin") }; currentLayer = layer }
        val visual=bay.currentVisual
        val powerIndex=bay.steps.indexOfFirst{it.id=="aed_power"}
        val cueRegion=visual.bodyRegion.name.takeIf{it!="NONE"&&bay.current.actions.any{a->a.spatial&&(a.id.startsWith("inspect:")||a.id=="apply-pressure")}}
        scene?.change { it.copy(equipment = powerIndex>=0&&bay.index>=powerIndex, equipmentPowered = powerIndex>=0&&bay.index>powerIndex,
            casualtyDetail=when(bay.scenarioId){"bleeding-injury"->"bleeding";"thermal-burn-response"->"scald";"suspected-limb-injury"->"limb";else->null},
            bodyRoll=if(visual.pose==FirstAidScenarioCatalog.Pose.RECOVERY_SIDE)90f else 0f,cueRegion=cueRegion,cueHold=bay.current.actions.any{a->a.id=="apply-pressure"}) }
        val visualKey="${bay.current.id}:$layer:${visual.pose}"
        if(!camera&&lastVisualKey!=visualKey){scene?.reset();lastVisualKey=visualKey}
        // Keep the whole body visible in AR. The ordinary 3D view can frame a small landmark.
        if (!camera) bay.current.partId?.let { id -> data.parts.firstOrNull { it.id == id }?.let { scene?.focus(it) } }
    }

    private fun renderStage() {
        val bay = attempt ?: return
        if (bay.finished && feedback == null || bay.stopped) { summary(); return }
        if (!::actions.isInitialized) return
        actions.removeAllViews(); pause.text = if (bay.paused) t("Resume","जारी") else t("Pause","रोकें")
        pause.contentDescription=if(bay.paused)t("Resume practice","अभ्यास जारी रखें")else t("Pause practice","अभ्यास रोकें")
        setPracticeIcon(pause,if(bay.paused)PracticeIcon.PLAY else PracticeIcon.PAUSE)
        if (bay.paused) {
            stage.text = t("Practice paused","अभ्यास रुका है"); cue.text = t("Your progress is saved. Resume when your practice area is ready.","प्रगति सहेजी गई है। जगह तैयार होने पर जारी रखें।")
            place.visibility = View.GONE; status.text = t("View paused","दृश्य रुका है")
            return
        }
        val result = feedback
        if (result != null) {
            stage.text = if (result.advanced) t("Step recorded","चरण सहेजा गया") else t("Try that step again","यह चरण फिर करें")
            cue.text = result.message(store.hi)
            actions.add(action(if (bay.finished) t("Review practice","अभ्यास देखें") else if (result.advanced) t("Next step","अगला चरण") else t("Try again","फिर करें")) {
                feedback = null; renderStage()
            })
            narrator.showLocalized(result.message,result.messageHi); return
        }
        val step = bay.current
        stage.text = "${bay.index + 1} / ${bay.steps.size} · ${step.title(store.hi)}"
        cue.text = if(step.id=="pressure")t("Hold the wound marker until the ring fills. This gesture does not set a treatment duration or measure pressure.","घेरा भरने तक घाव के संकेत को दबाए रखें। इससे उपचार की अवधि तय नहीं होती और वास्तविक दबाव नहीं मापा जाता।")else if(bay.definition!=null)bay.currentVisual.label(store.hi) else if (step.instructionalPause) "Learning pause · select the ${model?.parts?.firstOrNull { it.id == step.partId }?.name ?: "named structure"}. Use Cue to inspect the authored condition." else when (step.id) {
            "safe_space" -> "Check your real practice area before continuing."
            "buddy" -> "The adult is unresponsive and only gasping. Give the virtual bystander a clear task."
            "begin_cpr" -> "Help is being obtained. Choose the next action in this simulation."
            "aed_power" -> "The virtual AED has arrived beside the body. Tap its green power control."
            "aed_diagram" -> "Use the pad diagrams on the AED you are trained to use."
            "clear" -> "Authored AED prompt: stand clear for analysis."
            "analysis" -> "Authored AED prompt: resume CPR. No rhythm has been measured."
            "resume_cpr" -> "Follow the authored prompt. Physical compressions are not measured."
            else -> "A simulated responder arrives. Give a concise handover."
        }
        if (step.partId != null) {
            actions.add(action("Find structure by name", false, ActionRole.NEUTRAL) { accessibleSelection() }.apply { tag = "bay-accessible" })
        } else if (step.id == "aed_power") {
            actions.add(action(t("Power button alternative · 3D","पावर बटन विकल्प · 3D"), false, ActionRole.NEUTRAL) { act("aed-power-on", "3d") })
        } else {
            // Neutral styling does not disclose the correct answer through colour.
            val choices=step.actions.shuffled(java.util.Random(bay.data.getString("id").hashCode().toLong() + bay.index))
            if((shortScreen||resources.configuration.fontScale>=1.6f)&&choices.size>1){
                actions.add(action(t("Choose the next action","अगला निर्णय चुनें"),false,ActionRole.NEUTRAL){
                    val stageId=step.id
                    PageDialogBuilder(this).setTitle(step.title(store.hi)).setItems(choices.map{it.label(store.hi)}.toTypedArray()){_,i->
                        if(attempt?.current?.id==stageId)act(choices[i].id,if(choices[i].spatial)"3d"else if(camera)"ar"else"3d")
                    }.show()
                }.apply{tag="bay-action";if(shortScreen)textSize=14f})
            }else choices.forEach { choice ->
                val name=choice.label(store.hi)+(if(choice.spatial&&camera)" · 3D"else "")
                actions.add(action(name, false, ActionRole.NEUTRAL) { act(choice.id, if(choice.spatial)"3d"else if (camera) "ar" else "3d") }.apply { tag = "bay-action" }, bottom = 5)
            }
        }
        showLayer(); updateAvailability()
        val gestureEn=if(step.id=="pressure")" Hold the wound marker until the ring fills. This screen gesture does not set a treatment duration or measure pressure."else ""
        val gestureHi=if(step.id=="pressure")" घेरा भरने तक घाव का संकेत दबाए रखें। यह स्क्रीन की क्रिया है; इससे उपचार की अवधि तय नहीं होती और वास्तविक दबाव नहीं मापा जाता।"else ""
        narrator.showLocalized(step.title+". "+step.cue+gestureEn,if(bay.definition!=null)step.titleHi+"। "+step.cueHi+gestureHi else null)
    }

    private fun updateAvailability() {
        if (!::actions.isInitialized || feedback != null) return
        actions.visibility=if(dock!=null||camera&&scene?.placed!=true)View.GONE else View.VISIBLE
        for (i in 0 until actions.childCount) {
            val view = actions.getChildAt(i)
            if (view.tag == "bay-action") view.isEnabled = attempt?.paused == false && scene?.interactionReady() == true
        }
    }

    private fun act(id: String, mode: String) {
        if (!allowed()) { finish(); return }
        val bay = attempt ?: return
        if (feedback != null || bay.paused) return
        feedback = bay.act(id, mode, scene?.interactionReady() == true)
        save(); renderStage()
    }

    private fun accessibleSelection() {
        val bay = attempt ?: return; val data = model ?: return
        if (bay.paused || bay.finished) return
        val stageId = bay.current.id
        val layer = data.parts.firstOrNull { it.id == bay.current.partId }?.layer ?: return
        val choices = data.parts.filter { it.layer == layer }.sortedBy { it.name }
        PageDialogBuilder(this).setTitle("Select a structure · 3D evidence")
            .setMessage("This button alternative records structure recognition in 3D, even when the AR camera is open.")
            .setItems(choices.map { it.name }.toTypedArray()) { _, i ->
                if (attempt?.current?.id == stageId && allowed()) { scene?.select(choices[i].id); act("select:${choices[i].id}", "3d") }
            }.show()
    }

    private fun togglePause() {
        val bay = attempt ?: return
        if (bay.paused) { bay.resume(); scene?.resume() } else { bay.pause(); scene?.pause(); narrator.pause() }
        save(); renderStage()
    }

    private fun cueDetails() {
        feedback?.let{notice(t("Action feedback","निर्णय की जानकारी"),it.message(store.hi));return}
        val step = attempt?.current ?: return
        notice(step.title(store.hi), step.cue(store.hi) + (if (step.instructionalPause) "\n\nLearning pause: do not delay real emergency help to explore anatomy." else "") + "\n\n"+t("Source-checked draft · qualified review pending","स्रोत से जाँचा प्रारूप · विशेषज्ञ समीक्षा बाकी")+"\n" + step.source)
    }

    private fun closeView(){dock=null;if(::lessonPanel.isInitialized)lessonPanel.visibility=View.VISIBLE;if(::dockHost.isInitialized){dockHost.removeAllViews();dockHost.visibility=View.GONE};if(::actions.isInitialized)actions.visibility=View.VISIBLE;if(::cue.isInitialized)cue.visibility=View.VISIBLE;updateAvailability()}
    private fun viewControls(){
        val current=scene?:return
        if(dock!=null){closeView();return}
        if(attempt?.paused==true)return
        actions.visibility=View.GONE;cue.visibility=View.GONE;if(shortScreen)lessonPanel.visibility=View.GONE
        dock=AnatomyViewDock(this,current,{closeView()},{rescan()},allowPlacementEdits=!camera)
        dockHost.removeAllViews();dockHost.addView(dock);dockHost.visibility=View.VISIBLE
        current.onControls={dock?.sync()}
    }
    private fun rescan(){closeView();scene?.pause();scene?.resume();renderStage()}
    private fun goBack(){
        if(dock!=null){closeView();return}
        if(practising){attempt?.pause();save();resumeChoicePending=hasSavedDecisions();introduction()}else finish()
    }
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 back compatibility") override fun onBackPressed(){goBack()}

    private fun options() {
        PageDialogBuilder(this).setTitle("Practice options").setItems(arrayOf("Retry camera / rescan", if (camera) "Switch to 3D practice" else "Switch to AR practice", "Source & current cue", "Stop this practice", t("Start from step 1", "चरण 1 से शुरू करें"))) { _, i -> when (i) {
            0 -> { if (attempt?.paused == true) attempt?.resume(); save(); rescan() }
            1 -> enter(!camera)
            2 -> cueDetails()
            3 -> { attempt?.stop(); save(); summary() }
            4 -> if (allowed()) { attempt = newAttempt(); save(); feedback = null; resumeChoicePending = false; build() }
        } }.show()
    }

    private fun summary() {
        practising=false;dock=null
        scene?.close(); scene = null; narrator.pause()
        val bay = attempt ?: return
        val root = column(20).apply { setBackgroundColor(Palette.canvas) }
        root.add(label(if (bay.finished) "Practice recorded" else "Practice stopped", 22f, Palette.ink, true).asHeading(), bottom = 16)
        val body = column()
        body.add(label(bay.title(store.hi),18f,Palette.ink,true),bottom=10)
        body.add(label("${bay.index} / ${bay.steps.size} stages · ${when (bay.presentation) { "ar" -> "AR interactions"; "3d" -> "3D interactions"; "mixed" -> "AR and 3D interactions"; else -> "no completed actions" }}", 16f, Palette.teal, true), bottom = 12)
        body.add(label("Saved on this learner profile. This record describes simulated decisions and structure recognition. It does not establish practical first-aid or CPR competence.", 16f), bottom = 12)
        body.add(action("Review recorded decisions", false) {
            val log = column(12)
            bay.events.objects().filter { it.optString("kind") == "action" }.forEach { event ->
                val step = bay.steps.first { it.id == event.getString("state") }
                val choice = step.actions.firstOrNull { it.id == event.optString("actionId") }
                log.add(label(step.title, 16f, Palette.ink, true), bottom = 4)
                log.add(label("${choice?.label ?: event.optString("actionId")} · ${event.getString("mode")}\n${event.getString("reason")}", 14f, Palette.muted), bottom = 14)
            }
            PageDialogBuilder(this).setTitle("Original decision record").setView(paged(log, store.hi)).setNegativeButton("Close", null).show()
        }, bottom = 10)
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action("Practise again") { attempt = null; resumeChoicePending = false; introduction() }, top = 10)
        root.add(action("Back to course", false) { finish() }, top = 8)
        setContentView(root); trainingInsets(root)
    }

    private fun save() { if (allowed()) attempt?.let { records.save(it.data) } }
    override fun onResume() { super.onResume(); resumed = true; if (!allowed()) { finish(); return }; attachScene(); if (attempt?.paused == false) scene?.resume() }
    override fun onPause() {
        resumed = false; scene?.pause(); narrator.pause()
        if (practising) { attempt?.pause(); save(); if (::actions.isInitialized && attempt?.finished == false) renderStage() }
        super.onPause()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (::narrator.isInitialized) { if (hasFocus) narrator.automatic() else narrator.pause() } }
    override fun onSaveInstanceState(out: Bundle) { out.putBoolean("camera",camera); out.putBoolean("resumeChoicePending",resumeChoicePending); out.putString("scenario",selectedScenario);attempt?.let { out.putString("attempt", it.data.getString("id")) }; super.onSaveInstanceState(out) }
    override fun onDestroy() { scene?.close(); narrator.close(); records.close(); store.close(); loader.shutdownNow(); back.close(); super.onDestroy() }
}
