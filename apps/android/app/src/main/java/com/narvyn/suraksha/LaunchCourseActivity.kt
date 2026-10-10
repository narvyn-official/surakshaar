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

    private fun render() {
        val course = catalog.course(courseId)
        val school = context() == "School"
        val lessons = catalog.scenarios.filter {
            it.getString("course") == courseId && (!school || it.getString("audience") == "awareness")
        }.sortedBy { if (school && it.getString("setting") == "school") 0 else 1 }
        val selected = lessons.firstOrNull { it.getString("id") == selectedLesson } ?: lessons.first()
        selectedLesson = selected.getString("id")
        val index = lessons.indexOf(selected)
        val records = LearningProgress(this, store.workerId).use { it.records() }
        val latest = records.firstOrNull {
            it.getString("scenarioId") == selectedLesson && it.getString("contentVersion") == catalog.version
        }
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(action("‹", false, ActionRole.NEUTRAL) { finish() }.apply {
            contentDescription = copy("back")
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        head.addView(label(course.getString(if (store.hi) "titleHi" else "title"), 22f, Palette.ink, true)
            .asHeading().apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        root.add(head, bottom = 14)
        val body = column()
        body.add(label(course.getString(if (store.hi) "subtitleHi" else "subtitle"), 15f, Palette.muted), bottom = 12)
        body.add(action(contextLabel(context()) + " · " + t("Change", "बदलें"), false, ActionRole.NEUTRAL) {
            chooseContext()
        }.apply { tag = "learning-context" }, bottom = 12)
        if (store.hi) body.add(label(copy("pendingTranslation"), 13f, Palette.amber), bottom = 10)
        body.add(card().apply {
            add(label(t("Lesson ${index + 1} of ${lessons.size}", "पाठ ${index + 1} / ${lessons.size}"), 12f, Palette.blue), bottom = 8)
            add(label(selected.getString("title"), 22f, Palette.ink, true).asHeading(), bottom = 8)
            val state = when {
                latest?.optBoolean("finished") == true -> t("Decision practice complete", "निर्णय अभ्यास पूरा")
                latest != null -> t("Practice saved · ready to continue", "अभ्यास सहेजा गया · आगे बढ़ें")
                else -> t("${selected.getJSONArray("steps").length()} stages · ${if (selected.getString("audience") == "adult") "adult scenario" else "awareness"}",
                    "${selected.getJSONArray("steps").length()} चरण · ${if (selected.getString("audience") == "adult") "वयस्क स्थिति" else "जागरूकता"}")
            }
            add(label(state, 13f, Palette.muted), bottom = 12)
            add(action(if (latest == null) copy("start") else if (latest.optBoolean("finished")) copy("again") else copy("continue")) {
                startActivity(Intent(this@LaunchCourseActivity, LearningScenarioActivity::class.java)
                    .putExtra("scenarioId", selected.getString("id")))
            }.apply { tag = "launch-${selected.getString("id")}" }, bottom = 8)
            add(action(t("What you will learn", "इस पाठ में क्या सीखेंगे"), false, ActionRole.NEUTRAL) {
                lessonDetails(selected)
            })
        }, bottom = 12)
        val chooser = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        chooser.addView(action("‹", false, ActionRole.NEUTRAL) {
            selectedLesson = lessons[index - 1].getString("id"); render()
        }.apply { isEnabled = index > 0; contentDescription = t("Previous lesson", "पिछला पाठ") }, LinearLayout.LayoutParams(dp(48), dp(48)))
        chooser.addView(action(t("Choose lesson", "पाठ चुनें"), false, ActionRole.NEUTRAL) {
            PageDialogBuilder(this).setTitle(t("Choose lesson", "पाठ चुनें"))
                .setItems(lessons.map { it.getString("title") }.toTypedArray()) { _, i ->
                    selectedLesson = lessons[i].getString("id"); render()
                }.show()
        }.apply { tag = "choose-lesson" }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, dp(8), 0) })
        chooser.addView(action("›", false, ActionRole.NEUTRAL) {
            selectedLesson = lessons[index + 1].getString("id"); render()
        }.apply { isEnabled = index < lessons.lastIndex; contentDescription = t("Next lesson", "अगला पाठ") }, LinearLayout.LayoutParams(dp(48), dp(48)))
        body.add(chooser, bottom = 12)
        val revisionCount = LearningProgress(this, store.workerId).use { it.weakObjectives(catalog, courseId, school).size }
        if (revisionCount > 0) body.add(action(t("Review tricky decisions ($revisionCount)", "कठिन निर्णय दोहराएँ ($revisionCount)"), false, ActionRole.REVIEW) {
            startActivity(Intent(this, LearningScenarioActivity::class.java).putExtra("revision", true).putExtra("courseId", courseId))
        }.apply { tag = "course-revision" }, bottom = 10)
        if (courseId == "first-aid") {
            if (!school) body.add(action(t("First-aid practice bay", "प्राथमिक उपचार अभ्यास"), false, ActionRole.CAMERA) {
                startActivity(Intent(this, FirstAidBayActivity::class.java))
            }.apply { tag = "first-aid-bay" }, bottom = 10)
            body.add(action(copy("body"), false, ActionRole.CAMERA) {
                startActivity(Intent(this, AnatomyActivity::class.java))
            }.apply { tag = "explore-body" }, bottom = 10)
        } else if (!school) {
            body.add(action(t("Fire equipment & room AR", "अग्नि सुरक्षा उपकरण और AR"), false, ActionRole.CAMERA) {
                firePracticeMenu()
            }, bottom = 10)
        }
        root.addView(paged(body, store.hi), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action(copy("review"), false, ActionRole.NEUTRAL) {
            notice(t("Draft learning content", "समीक्षा बाकी है"), "Version ${catalog.version}\n\n" +
                t("Source-checked draft. Clinical, fire-instructor and translation approval are pending. These lessons do not award a certificate or establish practical skill.\n\nPlanned chapters: ",
                    "स्रोतों से जाँचा गया प्रारूप। चिकित्सकीय, अग्नि प्रशिक्षक और अनुवाद समीक्षा बाकी है। ये पाठ प्रमाणपत्र या व्यावहारिक कौशल की पुष्टि नहीं करते।\n\nआगे जोड़े जाने वाले अध्याय: ") +
                catalog.content.optJSONArray("plannedChapters")?.strings().orEmpty().joinToString(", "))
        }, top = 8)
        setContentView(root)
        trainingInsets(root)
    }

    private fun firePracticeMenu() {
        val choices = FireRoomScenarioCatalog.all
        val labels = choices.map { it.title(store.hi) } + listOf(t("Inspect equipment in 3D", "3D में उपकरण देखें"), t("Saved room practice", "सहेजा गया कमरे का अभ्यास"))
        PageDialogBuilder(this).setTitle(t("Fire practice", "अग्नि सुरक्षा अभ्यास"))
            .setItems(labels.toTypedArray()) { _, index ->
                if (!store.isCurrentProfile() || context() == "School") return@setItems
                if(index == choices.size) startActivity(Intent(this, EquipmentActivity::class.java).putExtra("moduleId", "fire"))
                else if(index == choices.size+1) roomRecords()
                else {
                    val lesson = choices[index]
                    PageDialogBuilder(this).setTitle(lesson.title(store.hi)).setMessage(lesson.summary(store.hi))
                        .setPositiveButton(t("Start AR", "AR शुरू करें")) { _, _ -> startFire(lesson.id,true) }
                        .setNeutralButton(t("Use 3D", "3D में करें")) { _, _ -> startFire(lesson.id,false) }
                        .setNegativeButton(t("Back", "वापस"),null).show()
                }
            }.show()
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
