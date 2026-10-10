package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import org.json.JSONObject

/** A course is a bounded lesson selector, not a long feed of duplicate cards. */
class LaunchCourseActivity : Activity() {
    private lateinit var store: Store
    private lateinit var catalog: LearningCatalog
    private val back by lazy { AppBackNavigation(this) { finish() } }
    private var courseId = "first-aid"
    private var selectedLesson: String? = null
    private fun t(en: String, hi: String) = if (store.hi) hi else en
    private fun copy(key: String): String {
        val values = catalog.catalog.getJSONObject("labels").getJSONObject(key)
        return values.getString(if (store.hi) "hi" else "en")
    }
    private fun context() = getSharedPreferences("learning-profile", MODE_PRIVATE)
        .getString("context-${store.workerId}", "Independent learning")!!
    private fun contextLabel(value: String) = if (store.hi)
        catalog.catalog.getJSONObject("contextLabelsHi").optString(value, value) else value

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = Store(this)
        catalog = LearningCatalog(this)
        courseId = intent.getStringExtra("courseId")
            ?.takeIf { id -> catalog.courses.any { it.getString("id") == id } } ?: "first-aid"
        selectedLesson = state?.getString("selectedLesson")
        back.enabled(true)
    }

    override fun onResume() {
        super.onResume()
        if (!store.isCurrentProfile()) { finish(); return }
        render()
    }

    private fun lessons(): List<JSONObject> = catalog.scenarios.filter {
        it.getString("course") == courseId && (context() != "School" || it.getString("audience") == "awareness")
    }.sortedBy { if (context() == "School" && it.getString("setting") == "school") 0 else 1 }

    private fun validRecords(lessons: List<JSONObject>): List<JSONObject> {
        val byId = lessons.associateBy { it.getString("id") }
        return LearningProgress(this, store.workerId).use { it.records() }.filter { record ->
            val lesson = byId[record.optString("scenarioId")] ?: return@filter false
            runCatching { LearningAttempt.restore(record, lesson, catalog.version, store.workerId) }.isSuccess
        }
    }

    private fun openLesson(lesson: JSONObject) {
        if (!store.isCurrentProfile() || lessons().none { it.getString("id") == lesson.getString("id") }) return
        selectedLesson = lesson.getString("id")
        val saved = validRecords(listOf(lesson)).firstOrNull { !it.optBoolean("finished") }
        startActivity(Intent(this, LearningScenarioActivity::class.java).putExtra("scenarioId", selectedLesson)
            .putExtra("attemptId", saved?.optString("id")))
    }

    private fun render() {
        val course = catalog.course(courseId)
        val school = context() == "School"
        val lessons = lessons()
        val records = validRecords(lessons)
        val unfinished = records.filter { !it.optBoolean("finished") }
        val completed = records.filter { it.optBoolean("finished") }.map { it.getString("scenarioId") }.toSet()
        val resume = unfinished.firstOrNull { it.getString("scenarioId") == selectedLesson } ?: unfinished.firstOrNull()
        val selected = lessons.firstOrNull { it.getString("id") == resume?.optString("scenarioId") }
            ?: lessons.firstOrNull { it.getString("id") !in completed } ?: lessons.firstOrNull()
        selectedLesson = selected?.getString("id")
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(action("‹", false, ActionRole.NEUTRAL) { finish() }.apply {
            contentDescription = copy("back")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        head.addView(label(course.getString(if (store.hi) "titleHi" else "title"), 22f, Palette.ink, true)
            .asHeading().apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        root.add(head, bottom = 10)
        val body = column()
        body.add(action(contextLabel(context()) + " · " + t("Change", "बदलें"), false, ActionRole.NEUTRAL) {
            chooseContext()
        }.apply { tag = "learning-context"; textSize = 13f }, bottom = 10)
        if (store.hi) body.add(label(t("Lessons in English · translation under review", "पाठ अंग्रेज़ी में · अनुवाद की समीक्षा जारी"), 12f, Palette.amber), bottom = 8)
        if (selected == null) {
            body.add(label(t("No lessons available in this learning context.", "इस संदर्भ में कोई पाठ उपलब्ध नहीं है।"), 16f), bottom = 12)
        } else {
            body.add(card().apply {
                add(label(if (resume != null) t("CONTINUE LESSON", "पाठ जारी रखें") else t("NEXT LESSON", "अगला पाठ"), 12f, Palette.blue, true), bottom = 6)
                add(label(selected.getString("title"), 20f, Palette.ink, true).asHeading(), bottom = 6)
                val status = if (resume != null) t("Saved on this phone · step ${resume.optInt("index") + 1} of ${selected.getJSONArray("steps").length()}",
                    "इस फ़ोन पर सहेजा · चरण ${resume.optInt("index") + 1} / ${selected.getJSONArray("steps").length()}")
                    else t("${selected.getJSONArray("steps").length()} decisions · ${completed.size} of ${lessons.size} lessons complete",
                        "${selected.getJSONArray("steps").length()} निर्णय · ${lessons.size} में से ${completed.size} पाठ पूरे")
                add(label(status, 13f, Palette.muted), bottom = 12)
                add(action(if (resume != null) copy("continue") else if (selected.getString("id") in completed) copy("again") else copy("start")) {
                    openLesson(selected)
                }.apply { tag = "launch-${selected.getString("id")}" })
            }, bottom = 10)
            body.add(action(t("All lessons (${lessons.size})", "सभी पाठ (${lessons.size})"), false, ActionRole.NEUTRAL) {
                chooseLesson(lessons, records)
            }.apply { tag = "choose-lesson" }, bottom = 10)
        }
        val revisionCount = LearningProgress(this, store.workerId).use { it.weakObjectives(catalog, courseId, school).size }
        if (revisionCount > 0) body.add(action(t("Review tricky decisions ($revisionCount)", "कठिन निर्णय दोहराएँ ($revisionCount)"), false, ActionRole.REVIEW) {
            startActivity(Intent(this, LearningScenarioActivity::class.java).putExtra("revision", true).putExtra("courseId", courseId))
        }.apply { tag = "course-revision" }, bottom = 10)
        if (courseId == "first-aid") {
            val practice = if (school) null else action(t("Casualty practice", "घायल व्यक्ति का अभ्यास"), false, ActionRole.CAMERA) {
                if (store.isCurrentProfile() && context() != "School") startActivity(Intent(this, FirstAidBayActivity::class.java))
            }.apply { tag = "first-aid-bay" }
            val anatomy = action(t("Explore body", "शरीर देखें"), false, ActionRole.CAMERA) {
                if (store.isCurrentProfile()) startActivity(Intent(this, AnatomyActivity::class.java))
            }.apply { tag = "explore-body" }
            if (practice == null) body.add(anatomy, bottom = 10) else addActions(body, practice, anatomy)
        } else if (!school) {
            addActions(body,
                action(t("Practise in AR", "AR में अभ्यास"), false, ActionRole.CAMERA) { firePracticeMenu(true) }.apply { tag = "fire-ar-practice" },
                action(t("Practise in 3D", "3D में अभ्यास"), false, ActionRole.CAMERA) { firePracticeMenu(false) }.apply { tag = "fire-3d-practice" })
        }
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action(t("Course info · draft content", "पाठ की जानकारी · समीक्षा बाकी"), false, ActionRole.NEUTRAL) {
            courseTools(selected)
        }.apply { tag = "course-tools" }, top = 8)
        setContentView(root)
        trainingInsets(root)
    }

    private fun addActions(body: LinearLayout, first: android.widget.Button, second: android.widget.Button) {
        if (resources.configuration.fontScale >= 1.5f) {
            body.add(first, bottom = 8); body.add(second, bottom = 10); return
        }
        val row = LinearLayout(this).apply { isBaselineAligned = false }
        row.addView(first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        row.addView(second, LinearLayout.LayoutParams(0, -2, 1f))
        body.add(row, bottom = 10)
    }

    /** Picking a lesson is the start action; a second confirmation screen adds no information. */
    private fun chooseLesson(lessons: List<JSONObject>, records: List<JSONObject>) {
        val choices = lessons.map { lesson ->
            val id = lesson.getString("id")
            val saved = records.firstOrNull { it.optString("scenarioId") == id && !it.optBoolean("finished") }
            lesson.getString("title") + when {
                saved != null -> t(" · resume", " · जारी रखें")
                records.any { it.optString("scenarioId") == id && it.optBoolean("finished") } -> t(" · complete", " · पूरा")
                else -> ""
            }
        }
        PageDialogBuilder(this).setTitle(t("Choose a lesson to start", "शुरू करने के लिए पाठ चुनें"))
            .setItems(choices.toTypedArray()) { _, index -> openLesson(lessons[index]) }
            .setNegativeButton(t("Back", "वापस"), null).show()
    }

    private fun courseTools(selected: JSONObject?) {
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        selected?.let { actions.add(t("Lesson overview", "पाठ की जानकारी") to { lessonDetails(it) }) }
        if (courseId == "fire" && context() != "School") {
            actions.add(t("Inspect equipment in 3D", "3D में उपकरण देखें") to {
                if (store.isCurrentProfile() && context() != "School") startActivity(Intent(this, EquipmentActivity::class.java).putExtra("moduleId", "fire"))
            })
            actions.add(t("Saved room practice", "सहेजा गया कमरे का अभ्यास") to { roomRecords() })
        }
        actions.add(t("Content review status", "सामग्री की समीक्षा") to {
            notice(t("Draft learning content", "समीक्षा बाकी है"), "Version ${catalog.version}\n\n" +
                t("Source-checked draft. Clinical, fire-instructor and translation approval are pending. These lessons do not award a certificate or establish practical skill.",
                    "स्रोतों से जाँचा गया प्रारूप। चिकित्सकीय, अग्नि प्रशिक्षक और अनुवाद समीक्षा बाकी है। ये पाठ प्रमाणपत्र या व्यावहारिक कौशल की पुष्टि नहीं करते।"))
        })
        PageDialogBuilder(this).setTitle(t("Course info & tools", "पाठ की जानकारी और साधन"))
            .setItems(actions.map { it.first }.toTypedArray()) { _, index -> if (store.isCurrentProfile()) actions[index].second() }
            .setNegativeButton(t("Done", "पूरा हुआ"), null).show()
    }

    private fun firePracticeMenu(camera: Boolean) {
        val choices = FireRoomScenarioCatalog.all
        PageDialogBuilder(this).setTitle(if (camera) t("AR fire practice", "AR अग्नि अभ्यास") else t("3D fire practice", "3D अग्नि अभ्यास"))
            .setItems(choices.map { it.title(store.hi) }.toTypedArray()) { _, index -> startFire(choices[index].id, camera) }
            .setNegativeButton(t("Back", "वापस"), null).show()
    }

    private fun startFire(id:String,camera:Boolean){
        if(store.isCurrentProfile() && context()!="School") startActivity(Intent(this,RoomMissionActivity::class.java)
            .putExtra("moduleId","fire").putExtra("camera",camera).putExtra("fireScenarioId",id))
    }

    private fun roomRecords() {
        val records = RoomMissionStore(this, store.workerId).use { it.records().filter { record -> record.optString("module") == "fire" } }
        if (records.isEmpty()) { notice(t("Room practice", "कमरे का अभ्यास"), t("No saved room sessions for this learner yet.", "इस शिक्षार्थी का कमरे का अभ्यास अभी नहीं सहेजा गया।")); return }
        val body = column(12)
        body.add(label(t("Saved virtual actions. Camera mode alone does not prove tracking, anchor stability or physical competence.", "सहेजी गई काल्पनिक क्रियाएँ। कैमरा मोड अपने आप ट्रैकिंग या व्यावहारिक कौशल का प्रमाण नहीं है।"), 14f, Palette.muted), bottom = 12)
        records.forEach { record ->
            val stamp = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(record.getLong("updatedAt")))
            val camera = record.optString("mode") == "camera"
            body.add(action(stamp + " · " + if(camera) t("Camera session", "कैमरा अभ्यास") else t("Screen session", "स्क्रीन अभ्यास"), false, ActionRole.NEUTRAL) {
                if (store.isCurrentProfile() && context() != "School") startActivity(Intent(this, RoomMissionActivity::class.java)
                    .putExtra("moduleId", "fire").putExtra("camera", camera).putExtra("resumeId", record.getString("id"))
                    .putExtra("recall",record.optJSONObject("coaching")?.optString("mode")=="recall")
                    .putExtra("explosionRisk",record.getJSONObject("mission").optJSONObject("scenario")?.optBoolean("explosionRisk")==true)
                    .putExtra("fireScenarioId",record.getJSONObject("mission").optJSONObject("scenario")?.optString("fireScenarioId")?.takeIf{it.isNotBlank()}))
            }, bottom = 8)
        }
        PageDialogBuilder(this).setTitle(t("Saved room practice", "सहेजा गया कमरे का अभ्यास")).setView(paged(body, store.hi))
            .setNegativeButton(t("Done", "पूरा हुआ"), null).show()
    }

    private fun lessonDetails(lesson: JSONObject) {
        notice(t("Lesson overview", "पाठ की जानकारी"), lesson.optJSONObject("contextExamples")?.optString(context()).orEmpty() + "\n\n" +
            lesson.getJSONArray("objectives").strings().joinToString("\n\n") +
            "\n\n" + lesson.getJSONArray("prerequisites").strings().joinToString("\n") +
            "\n\n" + t("Decision practice only. Use approved supervised training for physical skills.", "केवल निर्णय अभ्यास। व्यावहारिक कौशल के लिए मान्य, प्रशिक्षक की देखरेख वाला प्रशिक्षण लें।"))
    }

    private fun chooseContext() {
        val contexts = catalog.catalog.getJSONArray("contexts").strings()
        PageDialogBuilder(this).setTitle(copy("context"))
            .setMessage(t("School opens awareness lessons. Other contexts include adult responder learning. Account permissions do not change.",
                "विद्यालय संदर्भ में जागरूकता पाठ खुलते हैं। अन्य संदर्भों में वयस्कों के पाठ भी हैं। खाते की अनुमतियाँ नहीं बदलतीं।"))
            .setItems(contexts.map { contextLabel(it) }.toTypedArray()) { _, i ->
                getSharedPreferences("learning-profile", MODE_PRIVATE).edit().putString("context-${store.workerId}", contexts[i]).apply()
                selectedLesson = null
                render()
            }.show()
    }

    override fun onSaveInstanceState(out: Bundle) { out.putString("selectedLesson", selectedLesson); super.onSaveInstanceState(out) }
    override fun onDestroy() { back.close(); store.close(); super.onDestroy() }
}
