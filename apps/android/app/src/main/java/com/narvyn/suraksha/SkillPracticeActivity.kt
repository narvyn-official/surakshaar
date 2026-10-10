package com.narvyn.suraksha

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.Random

/** New layout/cue variations and targeted retries; separate from certifiable knowledge evidence. */
class SkillPracticeActivity:Activity(){
    private lateinit var identity:Store
    private lateinit var history:SkillPracticeStore
    private lateinit var catalog:JSONObject
    private lateinit var body:LinearLayout
    private lateinit var narrator:TrainingNarrator
    private var session:SkillPracticeSession?=null
    private val back by lazy{AppBackNavigation(this){leaveTask()}}
    private fun t(en:String,hi:String)=if(identity.hi)hi else en
    override fun onCreate(state:Bundle?){
        super.onCreate(state);identity=Store(this);history=SkillPracticeStore(this,identity.workerId)
        catalog=JSONObject(assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
        narrator=TrainingNarrator(this){identity.hi}
        val id=state?.takeIf{it.getString("worker")==identity.workerId}?.getString("attempt")?:intent.getStringExtra("attemptId")
        if(id!=null)history.records().firstOrNull{it.optString("id")==id}?.let{session=runCatching{SkillPracticeSession.restore(it,catalog,identity.workerId)}.getOrNull()}
        render()
    }
    private fun leaveTask(){if(popContentPage())return;if(session!=null){session=null;render()}else finish()}
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){leaveTask()}
    override fun onSaveInstanceState(out:Bundle){out.putString("worker",identity.workerId);out.putString("attempt",session?.data?.getString("id"));super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();if(::identity.isInitialized&&!identity.isCurrentProfile())finish()}
    override fun onPause(){if(::narrator.isInitialized)narrator.pause();super.onPause()}
    override fun onWindowFocusChanged(focused:Boolean){super.onWindowFocusChanged(focused);if(::narrator.isInitialized){if(focused)narrator.automatic()else narrator.pause()}}
    override fun onDestroy(){back.close();narrator.close();history.close();identity.close();super.onDestroy()}
    private fun screen(title:String){
        back.enabled(true)
        val root=column(16).apply{setBackgroundColor(Palette.canvas)};trainingInsets(root)
        root.add(label(title,24f,bold=true).asHeading(),bottom=8)
        root.add(label(t("FORMATIVE SCREEN PRACTICE · SAFETY DRAFT","स्क्रीन पर सीखने का अभ्यास · सुरक्षा प्रारूप"),12f,Palette.amber,true),bottom=10)
        body=column();root.addView(paged(body,identity.hi),LinearLayout.LayoutParams(-1,0,1f))
        root.add(action(t("Save & back","सहेजें और वापस जाएँ"),false,ActionRole.NEUTRAL){leaveTask()},top=10)
        setContentView(root)
    }
    private fun begin(id:String){
        try{val next=SkillPracticeSession.create(catalog,id,identity.workerId);history.save(next.data);session=next;render()}
        catch(_:Exception){notice(t("Could not save","सहेजा नहीं जा सका"),t("The previous record is intact. Check storage and try again.","पिछला रिकॉर्ड सुरक्षित है। स्टोरेज जाँचकर फिर कोशिश करें।"))}
    }
    private fun render(){
        if(!identity.isCurrentProfile()){finish();return}
        narrator.clear()
        val s=session
        if(s==null){
            screen(t("Transfer challenge","बदले दृश्य में अभ्यास"))
            body.add(label(t("Inspect a changed arrangement, find a hidden fault, and choose a safe next step. English draft scenarios need competent safety and language review. No real equipment operation or AR evidence is recorded.","बदला दृश्य देखें, छिपी खराबी पहचानें और सुरक्षित अगला कदम चुनें। अंग्रेज़ी प्रारूप दृश्यों की सुरक्षा और भाषा समीक्षा बाकी है। असली उपकरण संचालन या AR प्रमाण दर्ज नहीं होता।"),15f),bottom=12)
            val objective=intent.getStringExtra("objectiveId")
            val module=intent.getStringExtra("moduleId")
            val scenarios=catalog.getJSONArray("scenarios").objects().filter{(objective==null||it.getString("objectiveId")==objective)&&(module==null||it.getString("module")==module)}
            history.records().firstOrNull{!it.optBoolean("finished")&&scenarios.any{s->s.getString("id")==it.optString("scenarioId")}}?.let{record->body.add(action(t("Resume saved challenge","सहेजा अभ्यास जारी रखें")){session=SkillPracticeSession.restore(record,catalog,identity.workerId);render()},bottom=12)}
            scenarios.forEach { scene->
                body.add(action(scene.local("title",identity.hi)+" · "+scene.getString("condition"),false){begin(scene.getString("id"))}.apply{tag="skill-scenario-${scene.getString("id")}"},bottom=8)
            }
            return
        }
        screen(s.scenario.local("title",identity.hi))
        if(s.done){
            body.add(label(if(s.data.getBoolean("passed"))t("Safe simulated decisions recorded","सुरक्षित काल्पनिक निर्णय दर्ज")else t("An unsafe decision needs practice","असुरक्षित निर्णय का अभ्यास चाहिए"),21f,if(s.data.getBoolean("passed"))Palette.success else Palette.danger,true).asHeading(),bottom=10)
            body.add(label(t("Screen simulation only. This result cannot change your original assessment or authorise work. Correct refusal is a successful response.","केवल स्क्रीन सिमुलेशन। यह परिणाम मूल मूल्यांकन नहीं बदलता और काम की अनुमति नहीं देता। सही इनकार सफल प्रतिक्रिया है।")),bottom=12)
            s.data.getJSONArray("answers").objects().forEach{e->val q=s.scenario.getJSONArray("questions").objects().single{it.getString("id")==e.getString("questionId")};body.add(label((if(e.getBoolean("correct"))"✓ "else"! ")+q.getString("prompt"),16f,bold=true),bottom=6);body.add(label(q.getString("explanation"),15f),bottom=14)}
            body.add(action(t("Practise another arrangement","दूसरी व्यवस्था में अभ्यास करें")){session=null;render()},bottom=8)
            body.add(action(t("Retry as a new practice record","नए अभ्यास रिकॉर्ड में फिर करें"),false){begin(s.scenario.getString("id"))},bottom=12)
            body.add(action(t("Audio","आवाज़"),false,ActionRole.NEUTRAL){narrator.showControls()},top=8)
            narrator.show(t("Practice complete. Review the explanations.","अभ्यास पूरा हुआ। व्याख्याएँ देखें।"));return
        }
        body.add(label(t("Draft English scenario · reviewed translation pending","अंग्रेज़ी प्रारूप दृश्य · समीक्षा वाला अनुवाद बाकी"),13f,Palette.amber),bottom=10)
        body.add(card(Palette.soft).apply{add(label(s.scenario.getString("context"),16f))},bottom=12)
        val recovery=getSharedPreferences("skill-recovery",MODE_PRIVATE);val key="${identity.workerId}:${s.data.getString("id")}:ack"
        if(recovery.getInt(key,0)<s.index){
            val q=s.scenario.getJSONArray("questions").getJSONObject(s.index-1)
            body.add(label(t("Decision saved","निर्णय सहेजा गया"),20f,bold=true).asHeading(),bottom=10)
            body.add(label(q.getString("explanation"),17f),bottom=12)
            body.add(action(t("Continue challenge","अभ्यास जारी रखें")){if(recovery.edit().putInt(key,s.index).commit())render()else notice("Could not save","Your decision remains saved. Try again.")})
            detailVoiceControls()
            narrator.show(q.getString("explanation"),"en-IN");return
        }
        val q=s.current
        body.add(label(t("Decision ${s.index+1} / 2","निर्णय ${s.index+1} / 2"),13f,Palette.blue),bottom=8)
        body.add(label(q.getString("prompt"),21f,bold=true).asHeading(),bottom=12)
        q.getJSONArray("choices").objects().shuffled(Random(s.data.getString("id").hashCode().toLong()+s.index)).forEach{choice->
            body.add(action(choice.getString("label"),false,ActionRole.NEUTRAL){
                if(!identity.isCurrentProfile()){finish();return@action}
                try{val candidate=SkillPracticeSession.restore(JSONObject(s.data.toString()),catalog,identity.workerId);if(candidate.choose(choice.getString("id"),maxOf(System.currentTimeMillis(),candidate.data.getLong("updatedAt")))){history.save(candidate.data);session=candidate;render()}}
                catch(_:Exception){notice(t("Could not save","सहेजा नहीं जा सका"),t("Your last saved decision is intact. Check storage and retry.","अंतिम सहेजा निर्णय सुरक्षित है। स्टोरेज जाँचकर फिर करें।"))}
            }.apply{tag="skill-choice-${choice.getString("id")}"},bottom=10)
        }
        detailVoiceControls()
        // New English-only drafts never silently speak Hindi or pretend to be translated.
        narrator.show(q.getString("prompt"),"en-IN")
    }
    private fun detailVoiceControls(){
        body.add(action(t("Audio","आवाज़"),false,ActionRole.NEUTRAL){narrator.showControls()},top=8)
    }
}
