package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Versioned adult first-aid simulations. Owns sequence/evidence, never clinical skill grading. */
class FirstAidBay private constructor(val data: JSONObject) {
    data class Action(val id: String, val label: String, val correct: Boolean, val feedback: String, val spatial: Boolean = false,
        val labelHi: String = label, val feedbackHi: String = feedback) {
        fun label(hindi:Boolean)=if(hindi)labelHi else label
        fun feedback(hindi:Boolean)=if(hindi)feedbackHi else feedback
    }
    data class Step(val id: String, val title: String, val cue: String, val partId: String?, val actions: List<Action>, val source: String = GUIDELINE, val instructionalPause: Boolean = partId != null,
        val titleHi:String=title, val cueHi:String=cue, val visual:FirstAidScenarioCatalog.VisualCue?=null) {
        fun title(hindi:Boolean)=if(hindi)titleHi else title
        fun cue(hindi:Boolean)=if(hindi)cueHi else cue
    }
    data class Feedback(val accepted: Boolean, val advanced: Boolean, val message: String, val reason: String, val messageHi:String=message) {
        fun message(hindi:Boolean)=if(hindi)messageHi else message
    }
    val scenarioId get()=data.getString("scenarioId")
    val definition get()=FirstAidScenarioCatalog.find(scenarioId)
    val steps:List<Step> = if(scenarioId==SCENARIO_ID) Companion.steps else requireNotNull(FirstAidScenarioCatalog.find(scenarioId)){"Unknown first-aid scenario"}.steps
    fun title(hindi:Boolean=false)=definition?.title(hindi) ?: if(hindi)"वयस्क के गिरने का पुराना अभ्यास" else "Adult collapse · earlier practice"
    val currentVisual get()=current.visual ?: FirstAidScenarioCatalog.LEGACY_VISUAL
    val index get() = data.getInt("index")
    val finished get() = index == steps.size
    val paused get() = data.getBoolean("paused")
    val stopped get() = data.getBoolean("stopped")
    val current get() = steps[index.coerceAtMost(steps.lastIndex)]
    val events get() = data.getJSONArray("answers")
    val presentation: String get() {
        val modes = events.objects().filter { it.optBoolean("advanced") }.map { it.getString("mode") }.toSet()
        return when { modes.isEmpty() -> "unstarted"; modes.size > 1 -> "mixed"; else -> modes.single() }
    }
    private fun clockNow() = maxOf(System.currentTimeMillis(), data.getLong("updatedAt"))
    private fun time(now: Long) { require(now >= data.getLong("updatedAt")) { "Learning time cannot move backwards" } }

    /** All AR progression requires tracking. Accessible 3D interaction remains explicitly 3D. */
    fun act(actionId: String, mode: String, validTracking: Boolean, now: Long = clockNow()): Feedback {
        time(now)
        require(mode in setOf("ar", "3d"))
        require(actionId.isNotBlank() && actionId.length <= 120)
        if (finished || stopped) return Feedback(false, false, "This attempt has ended. Start a new practice to retry.", "ended", "यह अभ्यास समाप्त हो चुका है। फिर करने के लिए नया अभ्यास शुरू करें।")
        val step = current
        val action = step.actions.firstOrNull { it.id == actionId }
        val reason = when {
            paused -> "paused"
            mode == "ar" && !validTracking -> "tracking-unavailable"
            action == null -> "unknown-action"
            !action.correct -> "incorrect"
            else -> "advanced"
        }
        val advanced = reason == "advanced"
        val message = when (reason) {
            "paused" -> "Practice is paused. Resume before interacting."
            "tracking-unavailable" -> "Tracking is unavailable. Look back at the practice area; this action did not advance the lesson."
            "unknown-action" -> if (step.partId != null && actionId.startsWith("select:")) "Find the named structure for this stage. The selected part was recorded without advancing." else "That action belongs to a different stage. Follow the current instruction."
            else -> action!!.feedback
        }
        append("action", mode, validTracking, now, actionId, advanced, reason)
        if (advanced) data.put("index", index + 1).put("finished", finished)
        val hindi=when(reason){
            "paused"->"अभ्यास रुका हुआ है। कार्रवाई से पहले जारी करें।"
            "tracking-unavailable"->"ट्रैकिंग उपलब्ध नहीं है। अभ्यास की जगह की ओर देखें; यह कार्रवाई दर्ज हुई लेकिन पाठ आगे नहीं बढ़ा।"
            "unknown-action"->"यह कार्रवाई इस चरण की नहीं है। वर्तमान निर्देश देखें।"
            else->action!!.feedbackHi
        }
        return Feedback(true, advanced, message, reason, hindi)
    }

    fun pause(now: Long = clockNow()): Boolean {
        time(now); if (paused || stopped || finished) return false
        append("pause", "control", false, now); data.put("paused", true); return true
    }
    fun resume(now: Long = clockNow()): Boolean {
        time(now); if (!paused || stopped || finished) return false
        append("resume", "control", false, now); data.put("paused", false); return true
    }
    /** Stopping is an appropriate learning outcome, never a penalty. Retry creates a separate attempt. */
    fun stop(now: Long = clockNow()): Boolean {
        time(now); if (stopped || finished) return false
        append("stop", "control", false, now); data.put("stopped", true).put("paused", false); return true
    }

    private fun append(kind: String, mode: String, tracking: Boolean, now: Long, actionId: String? = null, advanced: Boolean = false, reason: String = kind) {
        events.put(JSONObject().put("id", UUID.randomUUID().toString()).put("state", current.id).put("index", index)
            .put("kind", kind).put("mode", mode).put("validTracking", tracking).put("at", now)
            .put("advanced", advanced).put("reason", reason).apply { actionId?.let { put("actionId", it) } })
        data.put("updatedAt", now)
    }

    companion object {
        const val VERSION = 1
        const val SCENARIO_ID = "first-aid-adult-collapse-bay"
        const val GUIDELINE = "https://cpr.heart.org/en/resuscitation-science/cpr-and-ecc-guidelines/adult-basic-life-support"
        const val ALGORITHM = "https://cpr.heart.org/-/media/CPR-Files/CPR-Guidelines-Files/2025-Algorithms/Algorithm-BLS-Adult-Lay-Rescuer-250514.pdf"
        const val EMERGENCY_SOURCE = "https://112.gov.in/"
        private fun good(id: String, label: String, feedback: String, spatial: Boolean = false) = Action(id, label, true, feedback, spatial)
        private fun wrong(id: String, label: String, feedback: String) = Action(id, label, false, feedback)
        val steps = listOf(
            Step("safe_space", "Prepare the practice area", "This is an authored adult-collapse simulation. Use a clear practice area. Keep the real surroundings in view; the app cannot declare them safe.", null,
                listOf(good("ready-space", "My practice space is clear", "Practice setup acknowledged. You may pause or stop at any stage."), wrong("ignore-space", "Start without checking the area", "Check your real practice space first. Do not walk into hazards while watching the phone."))),
            Step("response", "Notice the response cue", "Authored cue: the virtual adult does not respond. Select the brain to explore the connection with awareness. This selection is not a responsiveness examination.", "FMA50801",
                listOf(good("select:FMA50801", "Identify the brain", "Brain identified in the model. Now inspect the authored breathing cue.", true))),
            Step("breathing", "Recognise abnormal breathing", "Authored cue: only irregular gasps, with no normal breathing. Select the trachea. The animation or label is illustrative; the camera does not examine a real person.", "FMA7394",
                listOf(good("select:FMA7394", "Identify the airway", "The adult is unresponsive with abnormal breathing. Activate emergency help.", true))),
            Step("buddy", "Delegate help clearly", "A virtual bystander is waiting for a specific task. No real call is made.", null,
                listOf(good("call-help-aed", "You: call 112, bring an AED, report back", "The virtual buddy acknowledges the request. Start the CPR sequence while the AED is obtained."), wrong("vague-help", "Someone should do something", "Name the helper and give a clear task."), wrong("wait-for-aed", "Wait for the AED before any action", "Do not delay the CPR sequence while waiting for an AED.")), EMERGENCY_SOURCE),
            Step("chest", "Find a chest landmark", "Identify the sternum on the adult model. Finding a landmark does not measure hand pressure, depth, breathing or the quality of CPR.", "FMA7485",
                listOf(good("select:FMA7485", "Identify the sternum", "Sternum identified. A training manikin and qualified instructor are needed for practical compression skills.", true))),
            Step("begin_cpr", "Begin the simulated sequence", "In this authored emergency, help is being obtained. Move the learning sequence to CPR; do not perform compressions on another learner.", null,
                listOf(good("begin-cpr-sequence", "Begin CPR sequence learning", "Sequence acknowledged. No physical compression or ventilation has been assessed."), wrong("compress-peer", "Practise compressions on a classmate", "Never practise chest compressions on another learner. Use an appropriate training manikin."))),
            Step("aed_power", "The virtual AED arrives", "Select the virtual power control. This trainer is not connected to a medical device.", null,
                listOf(good("aed-power-on", "Turn on the virtual AED", "The virtual device is on. Follow its instructions and pad diagrams.", true), wrong("aed-guess-shock", "Deliver a shock immediately", "Follow the AED prompts; do not guess whether a shock is needed.")), ALGORITHM),
            Step("aed_diagram", "Follow the device diagram", "Inspect the learning prompt about the device's pad diagrams. This activity does not grade pad placement or claim a clinically reviewed pad-position model.", null,
                listOf(good("follow-device-diagram", "I will follow the AED's pad diagrams", "Diagram-following acknowledged. This is sequence knowledge, not verified physical pad placement.", true), wrong("invent-pad-position", "Ignore the device diagrams", "Use the AED's diagrams and instructions; this model is not a substitute for them.")), ALGORITHM),
            Step("clear", "Keep clear for analysis", "Authored AED cue: stand clear. Practise a clear instruction to the virtual buddy.", null,
                listOf(good("stand-clear", "Everyone clear; no one touching", "Clear instruction recorded. Follow the device through its analysis stage."), wrong("touch-during-analysis", "Keep touching during analysis", "Keep everyone clear when the AED instructs you to do so.")), ALGORITHM),
            Step("analysis", "Follow the AED prompt", "The authored AED has completed its analysis and prompts resuming CPR. No rhythm was measured; this is a predefined learning state.", null,
                listOf(good("follow-aed-prompt", "Follow the displayed AED instruction", "Prompt recognised. Resume the CPR sequence as instructed."), wrong("override-aed", "Override the prompt and guess", "Follow the AED instructions; do not invent a rhythm diagnosis.")), ALGORITHM),
            Step("resume_cpr", "Resume the sequence", "Resume CPR learning as prompted. The trainer does not assess compression depth, force, recoil or ventilation.", null,
                listOf(good("resume-cpr-sequence", "Resume CPR sequence learning", "Sequence resumed. Continue following emergency-service and AED guidance."), wrong("stop-after-analysis", "Assume analysis means recovery", "Analysis does not establish recovery. Continue according to the AED and emergency guidance.")), ALGORITHM),
            Step("handover", "Give a concise handover", "A simulated responder arrives. Describe the authored observations and actions, without claiming a diagnosis.", null,
                listOf(good("handover-observations", "Report response, breathing, help and AED actions", "Learning bay complete. Your simulated actions are saved; practical competence has not been assessed."), wrong("diagnose-cause", "State a definite cause without evidence", "Report what you observed and did. Avoid an unsupported diagnosis.")))
        )

        fun create(worker: String, contentVersion: String, now: Long = System.currentTimeMillis()): FirstAidBay {
            require(worker.isNotBlank() && contentVersion.isNotBlank() && now >= 0)
            return FirstAidBay(JSONObject().put("schemaVersion", 1).put("bayVersion", VERSION).put("id", UUID.randomUUID().toString())
                .put("workerId", worker).put("scenarioId", SCENARIO_ID).put("courseId", "first-aid").put("contentVersion", contentVersion)
                .put("index", 0).put("answers", JSONArray()).put("paused", false).put("stopped", false).put("finished", false)
                .put("reviewStatus", "draft").put("certifiable", false).put("practicalCompetence", "not-assessed")
                .put("evidenceType", "simulated-spatial-learning").put("createdAt", now).put("updatedAt", now))
        }

        fun createScenario(worker:String,contentVersion:String,scenarioId:String=FirstAidScenarioCatalog.DEFAULT_ID,now:Long=System.currentTimeMillis()):FirstAidBay {
            val definition=requireNotNull(FirstAidScenarioCatalog.find(scenarioId)){"Unsupported first-aid scenario"}
            val record=create(worker,contentVersion,now).data
                .put("scenarioId",scenarioId).put("scenarioDefinitionVersion",definition.version).put("scenarioFingerprint",definition.fingerprint)
            return FirstAidBay(record)
        }

        fun restore(record: JSONObject, worker: String, contentVersion: String): FirstAidBay {
            require(record.getInt("schemaVersion") == 1 && record.getInt("bayVersion") == VERSION)
            require(record.getString("workerId") == worker && record.getString("contentVersion") == contentVersion)
            val scenarioId=record.getString("scenarioId")
            val definition=if(scenarioId==SCENARIO_ID)null else requireNotNull(FirstAidScenarioCatalog.find(scenarioId)){"Unknown first-aid scenario"}
            if(definition!=null)require(record.getInt("scenarioDefinitionVersion")==definition.version && record.getString("scenarioFingerprint")==definition.fingerprint){"Scenario definition has changed; retain the original record without regrading"}
            else require(!record.has("scenarioDefinitionVersion")&&!record.has("scenarioFingerprint")){"Legacy scenario metadata cannot change"}
            require(record.getString("courseId") == "first-aid")
            require(record.getString("reviewStatus") == "draft" && !record.getBoolean("certifiable"))
            require(!record.has("passed") && !record.has("score")) { "This learning bay does not award a skill grade" }
            require(record.getString("evidenceType") == "simulated-spatial-learning" && record.getString("practicalCompetence") == "not-assessed")
            require(UUID.fromString(record.getString("id")).toString() == record.getString("id"))
            val replay = if(definition==null)create(worker, contentVersion, record.getLong("createdAt"))
                else createScenario(worker,contentVersion,scenarioId,record.getLong("createdAt"))
            val ids = mutableSetOf<String>()
            for (event in record.getJSONArray("answers").objects()) {
                require(UUID.fromString(event.getString("id")).toString() == event.getString("id") && ids.add(event.getString("id")))
                require(!replay.finished && !replay.stopped)
                require(event.getInt("index") == replay.index && event.getString("state") == replay.current.id)
                val now = event.getLong("at")
                when (event.getString("kind")) {
                    "action" -> require(replay.act(event.getString("actionId"), event.getString("mode"), event.getBoolean("validTracking"), now).accepted)
                    "pause" -> require(replay.pause(now))
                    "resume" -> require(replay.resume(now))
                    "stop" -> require(replay.stop(now))
                    else -> error("Unknown bay event")
                }
                val derived = replay.events.getJSONObject(replay.events.length() - 1)
                for (key in listOf("kind", "state", "index", "mode", "validTracking", "advanced", "reason")) require(event.get(key) == derived.get(key)) { "Inconsistent bay event: $key" }
                require(event.getLong("at") == derived.getLong("at"))
            }
            require(record.getInt("index") == replay.index && record.getBoolean("finished") == replay.finished)
            require(record.getBoolean("paused") == replay.paused && record.getBoolean("stopped") == replay.stopped)
            require(record.getLong("updatedAt") == replay.data.getLong("updatedAt"))
            return FirstAidBay(JSONObject(record.toString()))
        }
    }
}
