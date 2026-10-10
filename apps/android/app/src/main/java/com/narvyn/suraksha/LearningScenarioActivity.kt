package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import org.json.JSONObject

/** Each screen has one task; retries and observations remain separate evidence events. */
class LearningScenarioActivity : Activity() {
    private lateinit var store: Store
    private lateinit var catalog: LearningCatalog
    private lateinit var progress: LearningProgress
    private lateinit var narrator: TrainingNarrator
    private var attempt: LearningAttempt? = null
    private var recordsMode = false
    private var revisionMode = false
    private var revision: LearningRevision? = null
    private var revisionCourse: String? = null
    private var blocked = false
    private var resumeChoicePending = false
    private var pendingStep: String? = null
    private var pendingPart: String? = null
    private var guidanceDialog: AlertDialog? = null
    private val back by lazy { AppBackNavigation(this) { finish() } }
    private val partNames by lazy {
        JSONObject(assets.open("anatomy/manifest.json").bufferedReader().use { it.readText() })
            .getJSONArray("parts").objects().associate { it.getString("id") to it.getString("name") }
    }
    private fun t(en: String, hi: String) = if (store.hi) hi else en
    private fun school() = getSharedPreferences("learning-profile", MODE_PRIVATE)
        .getString("context-${store.workerId}", "Independent learning") == "School"
    private fun allowed(scenario: JSONObject) = !school() || scenario.getString("audience") == "awareness"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = Store(this)
        catalog = LearningCatalog(this)
        progress = LearningProgress(this, store.workerId)
        // Authored clinical text is English. Shared narration reports missing translation instead of silently switching languages.
        narrator = TrainingNarrator(this) { false }
        recordsMode = intent.getBooleanExtra("showRecords", false)
        revisionMode = intent.getBooleanExtra("revision", false)
        revisionCourse = intent.getStringExtra("courseId")?.takeIf { id -> catalog.courses.any { it.getString("id") == id } }
        pendingStep = state?.getString("pendingStep")
        pendingPart = state?.getString("pendingPart")
        back.enabled(true)
        if (revisionMode) {
            val savedId = state?.getString("revisionId")
            revision = progress.revisionRecords().firstOrNull { it.optString("id") == savedId }?.let { record ->
                runCatching {
                    val scenario = catalog.scenario(record.getString("scenarioId"))
                    val step = scenario.getJSONArray("steps").objects().first { it.getString("id") == record.getString("objectiveId") }
                    LearningRevision.restore(record, scenario, step, catalog.version, store.workerId).takeIf { allowed(scenario) }
                }.getOrNull()
            }
            if (revision == null) nextRevision()
        } else if (!recordsMode) {
            val id = intent.getStringExtra("scenarioId") ?: "adult-collapse"
            val scenario = catalog.scenarios.firstOrNull { it.getString("id") == id } ?: run { finish(); return }
            blocked = !allowed(scenario)
            if (!blocked) {
                val savedId = state?.getString("attemptId") ?: intent.getStringExtra("attemptId")
                val saved = if (intent.getBooleanExtra("newPractice", false) && state == null) null else progress.records().firstOrNull {
                    it.getString("scenarioId") == id && it.getString("contentVersion") == catalog.version &&
                        if (savedId != null) it.optString("id") == savedId else !it.optBoolean("finished")
                }
                attempt = if (saved != null) runCatching { LearningAttempt.restore(saved, scenario, catalog.version, store.workerId) }.getOrNull() else null
                if ((saved != null || savedId != null) && attempt == null) {
                    notice(t("Saved record needs review", "सहेजे गए रिकॉर्ड की समीक्षा चाहिए"),
                        t("The previous record cannot be safely resumed. It is retained unchanged; a new practice will start.", "पिछला अभ्यास सुरक्षित रूप से जारी नहीं हो सकता। उसका रिकॉर्ड रखा गया है; नया अभ्यास शुरू होगा।"))
                }
                resumeChoicePending = state?.getBoolean("resumeChoicePending") ?: (attempt?.let {
                    !it.finished && (it.index > 0 || it.events.objects().any { event -> event.optString("kind") in setOf("decision", "observation", "hint") })
                } == true)
                if (attempt == null) attempt = LearningAttempt.create(scenario, catalog.version, store.workerId, intent.getBooleanExtra("guided", true)).also { progress.save(it) }

            }
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (!store.isCurrentProfile()) { finish(); return }
        if (revisionMode && revision?.scenario?.let { !allowed(it) } == true) {
            revision = null; nextRevision(); render(); return
        }
        if (!recordsMode && attempt?.scenario?.let { !allowed(it) } == true) {
            blocked = true
            narrator.pause()
            render()
        }
    }

    private fun observed(a: LearningAttempt): Boolean = a.events.objects().any {
        it.optString("kind") == "observation" && it.optString("stepId") == a.current.getString("id") &&
            it.optString("partId") == a.current.optString("part")
    }
    private fun needsObservation(a: LearningAttempt) = a.current.optBoolean("requiresObservation") && !observed(a)

    private fun render() {
        if (revisionMode) { renderRevision(); return }
        val a = attempt
        if (resumeChoicePending && !recordsMode && !blocked && a != null && !a.finished) { resumeChoice(a); return }
        if (recordsMode || blocked || a?.finished == true) narrator.clear()
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(action("‹", false, ActionRole.NEUTRAL) { finish() }.apply {
            contentDescription = t("Back", "वापस")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(label(if (recordsMode) t("Practice records", "अभ्यास रिकॉर्ड") else if (blocked) t("Awareness learning", "जागरूकता पाठ") else a!!.scenario.getString("title"),
            18f, Palette.ink, true).asHeading().apply { setPadding(dp(10), 0, dp(6), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        if (!recordsMode && !blocked) header.addView(action(t("More", "और"), false, ActionRole.NEUTRAL) { lessonTools() }.apply { tag = "learning-tools" })
        root.add(header, bottom = 12)
        val body = column()
        when {
            blocked -> {
                body.add(label(t("Choose an awareness lesson", "जागरूकता पाठ चुनें"), 24f, Palette.ink, true).asHeading(), bottom = 12)
                body.add(label(t("School learning focuses on recognising danger, getting help and leaving safely. This adult responder lesson is outside that path.",
                    "विद्यालय में खतरा पहचानना, मदद लेना और सुरक्षित निकलना सिखाया जाता है। वयस्कों का यह पाठ इस संदर्भ में उपलब्ध नहीं है।"), 16f), bottom = 16)
                body.add(action(t("Open awareness lessons", "जागरूकता पाठ खोलें")) {
                    val course = attempt?.scenario?.getString("course") ?: catalog.scenarios.firstOrNull {
                        it.getString("id") == intent.getStringExtra("scenarioId")
                    }?.getString("course") ?: "first-aid"
                    startActivity(Intent(this, LaunchCourseActivity::class.java).putExtra("courseId", course)); finish()
                })
            }
            recordsMode -> records(body)
            a!!.finished -> finishPanel(body, a)
            else -> {
                val step = a.current
                val guided = a.data.optBoolean("guided")
                body.add(label("${a.index + 1} / ${a.steps.size} · " + t(if (guided) "Guided practice" else "Independent practice", if (guided) "मार्गदर्शित अभ्यास" else "स्वतंत्र अभ्यास"), 12f, Palette.blue), bottom = 8)
                if (store.hi) body.add(label(t("English lesson · translation pending", "अंग्रेज़ी पाठ · अनुवाद की समीक्षा बाकी"), 12f, Palette.amber), bottom = 8)
                body.add(label(step.getString("title"), 22f, Palette.ink, true).asHeading(), bottom = 12)
                when {
                    needsObservation(a) -> {
                        val part = step.getString("part")
                        body.add(label(step.getString("cue"), 16f), bottom = 14)
                        body.add(card().apply {
                            add(label(t("Find ${partNames[part] ?: "the structure"}", "शरीर में संरचना पहचानें"), 18f, Palette.ink, true), bottom = 8)
                            add(action(t("Find in body", "शरीर में पहचानें"), role = ActionRole.CAMERA) { openAnatomy(a) }.apply { tag = "required-anatomy-observation" })
                        }, bottom = 12)
                        body.add(label(t("Body exploration only · not a physical skills test", "शरीर की पहचान का अभ्यास · व्यावहारिक कौशल की जाँच नहीं"), 13f, Palette.muted))
                    }
                    a.awaitingContinue -> {
                        body.add(label(t("Decision saved", "निर्णय सहेजा गया"), 14f, Palette.success, true), bottom = 10)
                        body.add(label(step.getString("explanation"), 17f), bottom = 16)
                        val part = step.optString("part").takeIf { it.isNotBlank() && it != "null" }
                        if (part != null) body.add(action(if (observed(a)) t("Explore again", "फिर देखें") else t("Explore the anatomy", "शरीर की संरचना देखें"), false, ActionRole.CAMERA) {
                            openAnatomy(a)
                        }, bottom = 10)
                        if (observed(a)) body.add(label(t("Anatomy observation saved · physical skill separate", "संरचना की पहचान सहेजी गई · व्यावहारिक कौशल अलग है"), 13f, Palette.muted))
                    }
                    else -> {
                        body.add(label(step.getString("cue"), 16f), bottom = 12)
                        body.add(label(step.getString("question"), 17f, Palette.ink, true), bottom = 12)
                        if (a.data.has("lastCorrect") && !a.data.optBoolean("lastCorrect")) {
                            body.add(label(t("Try again. Your earlier choice remains in the record.", "फिर कोशिश करें। पिछला निर्णय रिकॉर्ड में बना रहेगा।"), 13f, Palette.danger), bottom = 8)
                        }
                        a.choices().forEach { choice ->
                            body.add(action(choice.getString("text"), false, ActionRole.NEUTRAL) {
                                if (!store.isCurrentProfile() || !allowed(a.scenario)) { blocked = true; narrator.pause(); render(); return@action }
                                val correct = a.answer(choice.getString("id")); progress.save(a); render()
                                if (!correct) narrator.show(if (guided) "Review the cue. " + step.getString("explanation") else "Try again. Guidance is available if you need it.")
                            }.apply { tag = "learning-choice-${choice.getString("id")}"; textSize = 15f; gravity = Gravity.START or Gravity.CENTER_VERTICAL }, bottom = 8)
                        }
                    }
                }
                narrator.show(when {
                    needsObservation(a) -> step.getString("cue") + ". Find " + (partNames[step.getString("part")] ?: "the structure") + " in the body viewer."
                    a.awaitingContinue -> step.getString("explanation")
                    else -> step.getString("title") + ". " + step.getString("cue") + ". " + step.getString("question")
                })
            }
        }
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        if (a != null && !a.finished && !blocked) {
            val footer = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            footer.addView(action(t("Help & sources", "मदद और स्रोत"), false, ActionRole.NEUTRAL) { guidance(a) }, LinearLayout.LayoutParams(0, -2, 1f))
            if (a.awaitingContinue) footer.addView(action(if (a.index == a.steps.lastIndex) t("Review practice", "अभ्यास की समीक्षा") else t("Continue", "आगे बढ़ें")) {
                if (!store.isCurrentProfile() || !allowed(a.scenario)) { blocked = true; narrator.pause(); render(); return@action }
                if (needsObservation(a)) { openAnatomy(a); return@action }
                a.advance(); a.data.remove("lastCorrect"); progress.save(a); render()
            }.apply { tag = "learning-next"; isEnabled = !needsObservation(a) }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
            root.add(footer, top = 8)
        }
        setContentView(root)
        trainingInsets(root)
    }

    private fun resumeChoice(a: LearningAttempt) {
        narrator.clear()
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        root.add(label(a.scenario.getString("title"), 22f, Palette.ink, true).asHeading(), bottom = 12)
        val body = column()
        body.add(label(t("Saved at step ${a.index + 1} of ${a.steps.size}", "चरण ${a.index + 1} / ${a.steps.size} पर सहेजा गया"), 18f, Palette.ink, true), bottom = 8)
        body.add(label(a.current.getString("title"), 16f, Palette.muted), bottom = 12)
        body.add(label(t("Earlier attempt stays saved.", "पिछला अभ्यास सहेजा रहेगा।"), 14f, Palette.muted))
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action(t("Continue", "जारी रखें")) {
            if (!store.isCurrentProfile() || !allowed(a.scenario)) { finish(); return@action }
            resumeChoicePending = false; render()
        }.apply { tag = "learning-resume-continue" }, top = 12)
        root.add(action(t("Start from step 1", "चरण 1 से शुरू करें"), false) {
            if (!store.isCurrentProfile() || !allowed(a.scenario)) { finish(); return@action }
            restartLesson(a)
        }.apply { tag = "learning-resume-restart" }, top = 8)
        root.add(action(t("Back", "वापस"), false, ActionRole.NEUTRAL) { finish() }, top = 8)
        setContentView(root); trainingInsets(root)
    }

    private fun restartLesson(a: LearningAttempt) {
        if (!store.isCurrentProfile() || !allowed(a.scenario)) { finish(); return }
        narrator.clear()
        attempt = LearningAttempt.create(a.scenario, catalog.version, store.workerId, a.data.optBoolean("guided", true)).also { progress.save(it) }
        resumeChoicePending = false; pendingStep = null; pendingPart = null; render()
    }

    private fun lessonTools() {
        val a = attempt ?: return
        val choices = mutableListOf(t("Audio", "आवाज़"))
        if (a.index > 0 || a.events.length() > 0) choices.add(t("Start from step 1", "चरण 1 से शुरू करें"))
        PageDialogBuilder(this).setTitle(t("Lesson options", "पाठ के विकल्प"))
            .setItems(choices.toTypedArray()) { _, index -> if (index == 0) audio() else restartLesson(a) }
            .setNegativeButton(t("Back", "वापस"), null).show()
    }

    @Suppress("DEPRECATION")
    private fun openAnatomy(a: LearningAttempt) {
        if (a.finished || blocked || !store.isCurrentProfile() || !allowed(a.scenario)) return
        pendingPart = a.current.optString("part").takeIf { it.isNotBlank() && it != "null" } ?: return
        pendingStep = a.current.getString("id")
        narrator.pause()
        startActivityForResult(Intent(this, AnatomyActivity::class.java).putExtra("partId", pendingPart).putExtra("observation", true), 702)
    }

    @Deprecated("Activity result callback remains compatible with existing Activity-based app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 702) return
        val a = attempt
        val part = data?.getStringExtra("partId")
        val mode = data?.getStringExtra("mode")
        if (resultCode == RESULT_OK && a != null && !a.finished && store.isCurrentProfile() && allowed(a.scenario) &&
            pendingStep == a.current.getString("id") && pendingPart == part && part == a.current.optString("part") &&
            mode in setOf("3d", "ar-exploration")) {
            a.observe(part!!, mode!!); progress.save(a)
        }
        pendingStep = null; pendingPart = null
        render()
    }

    private fun guidance(a: LearningAttempt) {
        if (!store.isCurrentProfile() || !allowed(a.scenario)) return
        if (!a.awaitingContinue) { a.hint(); progress.save(a) }
        narrator.pause()
        val stepId = a.current.getString("id")
        guidanceDialog = ContextualCoachDialog.show(
            host = this,
            store = store,
            contentVersion = catalog.version,
            scenario = a.scenario,
            step = a.current,
            recentActionIds = a.events.objects().filter { it.optString("kind") == "decision" }.map { it.getString("choiceId") },
            selectedObjectId = if (observed(a)) a.current.optString("part") else null,
            assessment = false,
            isCurrent = { attempt === a && !a.finished && a.current.getString("id") == stepId && allowed(a.scenario) },
            onAction = { action ->
                when (action) {
                    "inspect-selection" -> openAnatomy(a)
                    "show-step" -> render()
                    "open-replay" -> replay(a.data)
                }
            }
        )
    }

    private fun finishPanel(body: LinearLayout, a: LearningAttempt) {
        val decisions = a.events.objects().filter { it.optString("kind") == "decision" }
        val misses = decisions.count { !it.optBoolean("correct") }
        val hints = a.events.objects().count { it.optString("kind") == "hint" }
        val observations = a.events.objects().filter { it.optString("kind") == "observation" }.map { it.optString("stepId") }.distinct().size
        body.add(label(t("Practice complete", "अभ्यास पूरा"), 24f, Palette.ink, true).asHeading(), bottom = 10)
        body.add(label(t("${a.steps.size} decisions · $misses retries · $hints hints", "${a.steps.size} निर्णय · $misses दोबारा प्रयास · $hints संकेत"), 14f, Palette.muted), bottom = 8)
        if (observations > 0) body.add(label(t("$observations anatomy stages explored", "$observations चरणों में संरचना देखी गई"), 14f, Palette.blue), bottom = 8)
        body.add(label(t("Saved on this phone. Physical skills need instructor observation.", "इस फ़ोन पर सहेजा गया। व्यावहारिक कौशल की जाँच प्रशिक्षक करेंगे।"), 14f, Palette.muted), bottom = 16)
        val weak = progress.weakObjectives(catalog, a.scenario.getString("course"), school())
        val next = nextLesson(a)
        if (weak.isNotEmpty()) body.add(action(t("Review tricky decisions (${weak.size})", "कठिन निर्णय दोहराएँ (${weak.size})")) {
            if (store.isCurrentProfile() && allowed(a.scenario)) {
                startActivity(Intent(this, LearningScenarioActivity::class.java).putExtra("revision", true).putExtra("courseId", a.scenario.getString("course")))
                finish()
            }
        }.apply { tag = "start-targeted-revision" }, bottom = 8)
        if (next != null) body.add(action(t("Next lesson", "अगला पाठ"), primary = weak.isEmpty()) {
            if (store.isCurrentProfile() && allowed(next)) {
                startActivity(Intent(this, LearningScenarioActivity::class.java).putExtra("scenarioId", next.getString("id")))
                finish()
            }
        }.apply { tag = "learning-next-lesson"; contentDescription = t("Next lesson: ", "अगला पाठ: ") + next.getString("title") }, bottom = 8)
        body.add(action(t("Review my decisions", "मेरे निर्णय देखें"), false, ActionRole.NEUTRAL) { replay(a.data) }.apply { tag = "learning-replay" }, bottom = 8)
        body.add(action(t("More practice", "और अभ्यास"), false, ActionRole.NEUTRAL) { completionTools(a) }.apply { tag = "learning-completion-tools" }, bottom = 8)
        body.add(action(t("Back to course", "पाठ्यक्रम पर वापस"), primary = next == null && weak.isEmpty(), role = if (next == null && weak.isEmpty()) ActionRole.PRIMARY else ActionRole.NEUTRAL) { finish() })
    }

    private fun nextLesson(a: LearningAttempt): JSONObject? {
        val lessons = catalog.scenarios.filter { it.getString("course") == a.scenario.getString("course") && allowed(it) }
        val byId = lessons.associateBy { it.getString("id") }
        val completed = progress.records().mapNotNull { record ->
            val lesson = byId[record.optString("scenarioId")] ?: return@mapNotNull null
            runCatching { LearningAttempt.restore(record, lesson, catalog.version, store.workerId) }
                .getOrNull()?.takeIf { it.finished }?.scenario?.getString("id")
        }.toSet()
        return lessons.firstOrNull { it.getString("id") !in completed }
    }

    private fun completionTools(a: LearningAttempt) {
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        actions.add(t("Try without guidance", "बिना मार्गदर्शन अभ्यास करें") to {
            attempt = LearningAttempt.create(a.scenario, catalog.version, store.workerId, false).also { progress.save(it) }; render()
        })
        actions.add(t("Explain a decision", "अपना निर्णय समझाएँ") to { reflect(a) })
        transferTargets(a.scenario).forEach { target ->
            actions.add(t("Try: ", "अभ्यास: ") + target.getString("title") to {
                if (allowed(target)) {
                    startActivity(Intent(this, LearningScenarioActivity::class.java)
                        .putExtra("scenarioId", target.getString("id")).putExtra("guided", false).putExtra("newPractice", true))
                    finish()
                }
            })
        }
        if (a.scenario.getString("course") == "fire" && !school()) actions.add(t("Practise in AR", "AR में अभ्यास") to {
            if (!school()) startActivity(Intent(this, RoomMissionActivity::class.java).putExtra("moduleId", "fire").putExtra("camera", true))
        })
        PageDialogBuilder(this).setTitle(t("More practice", "और अभ्यास"))
            .setItems(actions.map { it.first }.toTypedArray()) { _, index ->
                if (store.isCurrentProfile() && allowed(a.scenario)) actions[index].second()
            }.setNegativeButton(t("Back", "वापस"), null).show()
    }

    private fun reflect(a: LearningAttempt) {
        val text = EditText(this).apply {
            hint = a.scenario.optString("teachBack", "Which cue mattered, and why?")
            minLines = 3; maxLines = 6; setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        PageDialogBuilder(this).setTitle(t("Explain your decision", "अपना निर्णय समझाएँ"))
            .setMessage(t("Optional reflection, kept on this phone. Avoid personal health details. AI does not grade it.", "वैकल्पिक उत्तर, इसी फ़ोन पर सहेजा जाएगा। निजी स्वास्थ्य जानकारी न लिखें। AI इसका मूल्यांकन नहीं करता।"))
            .setView(text).setPositiveButton(t("Save", "सहेजें")) { _, _ ->
                if (text.text.isNotBlank() && store.isCurrentProfile() && allowed(a.scenario)) {
                    a.explain(text.text.toString()); progress.save(a)
                    notice(t("Reflection saved", "उत्तर सहेजा गया"), t("Read it in this attempt’s replay.", "इसे अभ्यास की समीक्षा में देख सकते हैं।"))
                }
            }.setNegativeButton(t("Cancel", "रद्द करें"), null).show()
    }

    private fun records(body: LinearLayout) {
        val records = progress.records()
        if (records.isEmpty()) body.add(label(t("No practice yet. Choose Fire Safety or First Aid in Learn.", "अभी कोई अभ्यास नहीं है। सीखें में अग्नि सुरक्षा या प्राथमिक उपचार चुनें।"), 16f))
        records.forEach { record ->
            val scenario = catalog.scenarioForVersion(record.getString("scenarioId"), record.getString("contentVersion"))
            body.add(action((scenario?.getString("title") ?: record.getString("scenarioId")) +
                if (record.optBoolean("finished")) t(" · complete", " · पूरा") else t(" · in progress", " · जारी"), false) { replay(record) }, bottom = 10)
        }
        val revised = progress.revisionRecords()
        if (revised.isNotEmpty()) body.add(action(t("Objective revision records (${revised.size})", "निर्णय सुधार रिकॉर्ड (${revised.size})"), false) {
            val details = column(12)
            revised.forEach { record ->
                val scene = catalog.scenarioForVersion(record.optString("scenarioId"), record.optString("contentVersion"))
                val step = scene?.getJSONArray("steps")?.objects()?.firstOrNull { it.getString("id") == record.optString("objectiveId") }
                details.add(label((step?.optString("title") ?: record.optString("objectiveId")) + " · " + record.optString("contentVersion"), 15f, Palette.ink, true), bottom = 6)
                record.getJSONArray("answers").objects().forEach { event ->
                    val wording = step?.optJSONArray("choices")?.objects()?.firstOrNull { it.optString("id") == event.optString("choiceId") }?.optString("text")
                    details.add(label(when(event.optString("kind")) {
                        "decision" -> (wording ?: event.optString("choiceId")) + if(event.optBoolean("correct")) " · appropriate decision" else " · retry"
                        "teach-back" -> "Reflection: " + event.optString("text")
                        else -> "Guidance requested"
                    }, 14f), bottom = 10)
                }
            }
            PageDialogBuilder(this).setTitle(t("Local objective revision", "स्थानीय निर्णय सुधार")).setView(paged(details, store.hi)).setNegativeButton(t("Done", "पूरा हुआ"), null).show()
        }, bottom = 10)
        body.add(label(t("Local practice records are separate from industrial certificates.", "स्थानीय अभ्यास रिकॉर्ड औद्योगिक प्रमाणपत्रों से अलग हैं।"), 13f, Palette.muted), top = 12)
    }

    private fun replay(record: JSONObject) {
        val scenario = catalog.scenarioForVersion(record.getString("scenarioId"), record.getString("contentVersion"))
        val body = column(12)
        if (scenario == null) body.add(label(t("Original lesson wording is unavailable for this version. Stored event identifiers are shown unchanged.",
            "इस संस्करण के मूल पाठ उपलब्ध नहीं हैं। सहेजे गए रिकॉर्ड के ID बिना बदलाव दिखाए गए हैं।"), 13f, Palette.muted), bottom = 12)
        body.add(label("${record.getString("contentVersion")} · " + t("original events", "मूल रिकॉर्ड"), 12f, Palette.muted), bottom = 12)
        record.getJSONArray("answers").objects().forEach { event ->
            val step = scenario?.getJSONArray("steps")?.objects()?.firstOrNull { it.getString("id") == event.optString("stepId") }
            val text = when (event.optString("kind")) {
                "decision" -> {
                    val choice = step?.getJSONArray("choices")?.objects()?.firstOrNull { it.getString("id") == event.getString("choiceId") }
                    (step?.getString("title") ?: event.optString("stepId")) + "\n" + (choice?.getString("text") ?: event.optString("choiceId")) +
                        if (event.optBoolean("correct")) t("\nAppropriate decision", "\nउचित निर्णय") else t("\nRetry needed", "\nदोबारा प्रयास")
                }
                "hint" -> t("Hint requested", "संकेत माँगा गया") + " · ${step?.getString("title") ?: event.optString("stepId")}"
                "observation" -> t("Anatomy explored", "संरचना देखी गई") + " · ${partNames[event.optString("partId")] ?: event.optString("partId")}\n" +
                    if (event.optString("mode") == "ar-exploration") t("AR exploration · not physical skill assessment", "AR में देखा · व्यावहारिक कौशल की जाँच नहीं") else t("3D exploration · not physical skill assessment", "3D में देखा · व्यावहारिक कौशल की जाँच नहीं")
                "teach-back" -> t("Your reflection", "आपका उत्तर") + "\n${event.optString("text")}"
                else -> null
            }
            if (text != null) body.add(label(text, 15f), bottom = 16)
        }
        PageDialogBuilder(this).setTitle(t("Decision replay", "निर्णयों की समीक्षा"))
            .setView(paged(body, store.hi)).setNegativeButton(t("Done", "पूरा हुआ"), null).show()
    }

    private fun nextRevision(exclude:String? = null) {
        if (!store.isCurrentProfile()) return
        val target = progress.weakObjectives(catalog, revisionCourse, school())
            .firstOrNull { it.scenario.getString("id") + "/" + it.step.getString("id") != exclude }
        revision = target?.let { target ->
            val saved = progress.revisionRecords().firstOrNull { !it.optBoolean("finished") &&
                it.optString("scenarioId") == target.scenario.getString("id") && it.optString("objectiveId") == target.step.getString("id") &&
                it.optString("contentVersion") == catalog.version }
            (saved?.let { runCatching { LearningRevision.restore(it, target.scenario, target.step, catalog.version, store.workerId) }.getOrNull() }
                ?: LearningRevision.create(target.scenario, target.step, catalog.version, store.workerId)).also { progress.saveRevision(it) }
        }
    }

    private fun revisionCurrent(r:LearningRevision) = revision === r && store.isCurrentProfile() && allowed(r.scenario)

    private fun renderRevision() {
        val r = revision
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        root.add(action(t("‹ Back to learning", "‹ पाठ्यक्रम पर वापस"), false, ActionRole.NEUTRAL) { finish() }, bottom = 12)
        val body = column()
        body.add(label(t("Targeted revision", "ज़रूरत के अनुसार अभ्यास"), 23f, Palette.ink, true).asHeading(), bottom = 10)
        if (r == null) {
            narrator.clear()
            val remaining = progress.weakObjectives(catalog, revisionCourse, school()).size
            body.add(label(if (remaining == 0) t("No tricky decisions waiting", "कोई कठिन निर्णय बाकी नहीं") else t("$remaining decision still needs revision", "$remaining निर्णय का सुधार बाकी है"), 20f, Palette.ink, true), bottom = 12)
            body.add(label(if (remaining == 0) t("Your latest recorded decisions have no unresolved retries or requested hints. This is a revision queue, not a competence rating.", "नवीनतम निर्णयों में कोई लंबित सुधार नहीं है। यह अभ्यास की सूची है, कौशल का प्रमाण नहीं।") else t("The decision you skipped remains saved. Return when ready to try it without help.", "छोड़ा हुआ निर्णय सूची में है। तैयार होने पर बिना मदद फिर अभ्यास करें।"), 16f), bottom = 12)
            if (remaining > 0) body.add(action(t("Return to revision", "सुधार पर लौटें")) { nextRevision(); render() }, bottom = 10)
            body.add(action(t("Back to course", "पाठ्यक्रम पर वापस"), false) { finish() })
        } else {
            val step = r.step
            body.add(label(r.scenario.getString("title"), 13f, Palette.muted), bottom = 6)
            body.add(label(step.getString("title"), 20f, Palette.ink, true).asHeading(), bottom = 12)
            if (r.passed) {
                body.add(label(t("Decision revisited", "निर्णय का फिर अभ्यास किया"), 14f, Palette.success, true), bottom = 8)
                body.add(label(step.getString("explanation"), 16f), bottom = 12)
                body.add(label(if (r.clean) t("A fresh answer without hints is saved. Your earlier attempt remains unchanged.", "बिना संकेत का नया उत्तर सहेजा गया। पिछला अभ्यास नहीं बदला।") else t("Guidance or retries helped this time. Try once more without help when ready.", "इस बार सहायता या दोबारा प्रयास की ज़रूरत पड़ी। तैयार होने पर बिना मदद फिर करें।"), 14f, Palette.muted), bottom = 12)
                body.add(action(t("Explain why", "कारण समझाएँ"), false) {
                    val input = EditText(this).apply { hint = r.scenario.optString("teachBack"); minLines = 2; maxLines = 4 }
                    PageDialogBuilder(this).setTitle(t("Your reasoning", "आपका कारण")).setMessage(t("Optional local reflection. Avoid health details about real people. It is not AI-graded or uploaded.", "वैकल्पिक स्थानीय उत्तर। किसी की निजी स्वास्थ्य जानकारी न लिखें। इसे AI नहीं जाँचता और यह अपलोड नहीं होता।"))
                        .setView(input).setPositiveButton(t("Save", "सहेजें")) { _, _ -> if (revisionCurrent(r) && input.text.isNotBlank()) { r.reflect(input.text.toString()); progress.saveRevision(r) } }
                        .setNegativeButton(t("Cancel", "रद्द करें"), null).show()
                }, bottom = 8)
                if (!r.clean) body.add(action(t("Try without help", "बिना मदद फिर करें")) {
                    if (revisionCurrent(r)) { revision = LearningRevision.create(r.scenario, r.step, catalog.version, store.workerId).also { progress.saveRevision(it) }; render() }
                }, bottom = 8)
                body.add(action(t("Next revision", "अगला सुधार"), false) {
                    if (revisionCurrent(r)) { nextRevision(r.scenario.getString("id") + "/" + step.getString("id")); render() }
                }, bottom = 8)
                addTransferAction(body, r.scenario)
            } else {
                body.add(label(step.getString("cue"), 16f), bottom = 10)
                body.add(label(step.getString("question"), 17f, Palette.ink, true), bottom = 12)
                r.choices().forEach { choice -> body.add(action(choice.getString("text"), false, ActionRole.NEUTRAL) {
                    if (revisionCurrent(r)) {
                        val right = r.answer(choice.getString("id")); progress.saveRevision(r); render()
                        if (!right) notice(t("Recheck the cue", "संकेत फिर देखें"), t("This answer needs another look. Your choice is saved; guidance is available.", "इस उत्तर पर फिर विचार करें। निर्णय सहेजा गया है; मदद उपलब्ध है।"))
                    }
                }.apply { tag = "revision-choice-${choice.getString("id")}"; textSize = 15f; gravity = Gravity.START or Gravity.CENTER_VERTICAL }, bottom = 8) }
                body.add(action(t("Why & source", "कारण और स्रोत"), false) {
                    if (revisionCurrent(r)) { r.hint(); progress.saveRevision(r); sourceExplanation(step) }
                }, bottom = 8)
            }
            if (r.passed) body.add(action(t("Read the source", "स्रोत देखें"), false, ActionRole.NEUTRAL) { sourceExplanation(step) }, bottom = 8)
            narrator.show(if (r.passed) step.getString("explanation") else step.getString("title") + ". " + step.getString("cue") + ". " + step.getString("question"))
            body.add(action(t("Audio", "आवाज़"), false, ActionRole.NEUTRAL) { audio() }, bottom = 8)
            body.add(label(t("One decision check. Whole-lesson, AR and physical skills are separate.", "एक निर्णय का अभ्यास। पूरा पाठ, AR और व्यावहारिक कौशल अलग हैं।"), 12f, Palette.muted), top = 8)
        }
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root); trainingInsets(root)
    }

    private fun sourceExplanation(step:JSONObject) {
        val uri = android.net.Uri.parse(step.getString("source"))
        PageDialogBuilder(this).setTitle(t("Why this decision matters", "यह निर्णय क्यों ज़रूरी है"))
            .setMessage(step.getString("explanation") + "\n\n" + step.getString("source"))
            .setPositiveButton(t("Open source", "स्रोत खोलें")) { _, _ ->
                if (uri.scheme == "https" && !uri.host.isNullOrBlank()) runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    .onFailure { notice(t("Browser unavailable", "ब्राउज़र उपलब्ध नहीं"), step.getString("source")) }
            }.setNegativeButton(t("Done", "पूरा हुआ"), null).show()
    }

    private fun transferTargets(scenario: JSONObject) = scenario.optJSONArray("transferScenarioIds")?.strings().orEmpty().mapNotNull { id ->
        catalog.scenarios.firstOrNull { it.getString("id") == id && allowed(it) }
    }

    private fun addTransferAction(body:LinearLayout, scenario:JSONObject) {
        val targets = transferTargets(scenario)
        if (targets.isEmpty()) return
        body.add(action(t("Apply it in another situation", "दूसरी स्थिति में लागू करें"), false, ActionRole.REVIEW) {
            PageDialogBuilder(this).setTitle(t("Transfer challenge", "नई स्थिति में अभ्यास"))
                .setMessage(t("Use the cues in a different situation. Hints stay optional; this remains formative practice.", "नई स्थिति के संकेतों से निर्णय लें। संकेत वैकल्पिक हैं; यह सीखने का अभ्यास है।"))
                .setItems(targets.map { it.getString("title") }.toTypedArray()) { _, i ->
                    if (store.isCurrentProfile() && allowed(targets[i])) startActivity(Intent(this, LearningScenarioActivity::class.java)
                        .putExtra("scenarioId", targets[i].getString("id")).putExtra("guided", false).putExtra("newPractice", true))
                }.show()
        }.apply { tag = "transfer-challenge" }, bottom = 8)
    }

    private fun audio() { narrator.showControls() }
    override fun onSaveInstanceState(out: Bundle) { out.putString("attemptId", attempt?.data?.optString("id")); out.putBoolean("resumeChoicePending", resumeChoicePending); out.putString("revisionId", revision?.data?.optString("id")); out.putString("pendingStep", pendingStep); out.putString("pendingPart", pendingPart); super.onSaveInstanceState(out) }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (::narrator.isInitialized) { if (hasFocus) narrator.automatic() else narrator.pause() } }
    override fun onPause() { guidanceDialog?.dismiss(); guidanceDialog = null; if (::narrator.isInitialized) narrator.pause(); super.onPause() }
    override fun onDestroy() {
        guidanceDialog?.dismiss(); if (::narrator.isInitialized) narrator.close(); if (::progress.isInitialized) progress.close()
        if (::store.isInitialized) store.close(); back.close(); super.onDestroy()
    }
}
