package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import org.json.JSONObject

class DecisionReplayActivity:Activity(){
    private lateinit var identity:Store
    private var record:JSONObject?=null
    private var index=0
    private val back by lazy{AppBackNavigation(this){if(popContentPage())Unit else if(index>0){index--;render()}else finish()}}
    override fun onCreate(state:Bundle?){super.onCreate(state);identity=Store(this);index=state?.getInt("index")?:0
        record=ProcedureStore(this,identity.workerId).use{it.records()}.firstOrNull{it.optString("id")==intent.getStringExtra("procedureId")}
        if(record==null||!record!!.optBoolean("finished")){finish();return};render()
    }
    private fun t(en:String,hi:String)=if(identity.hi)hi else en
    private fun render(){
        val source=record?:return
        val entries=runCatching{DecisionReplay.entries(source)}.getOrElse{notice("Replay unavailable","The original record is preserved.");finish();return}
        if(entries.isEmpty()){finish();return};index=index.coerceIn(0,entries.lastIndex);val entry=entries[index]
        val root=column(16).apply{setBackgroundColor(Palette.canvas)};trainingInsets(root);back.enabled(true)
        root.add(label(t("Decision replay","निर्णय फिर देखें"),24f,bold=true).asHeading(),bottom=10)
        root.add(label(t("${index+1} / ${entries.size} · procedure content v${source.getInt("catalogVersion")}","${index+1} / ${entries.size} · प्रक्रिया सामग्री v${source.getInt("catalogVersion")}"),13f,Palette.blue),bottom=12)
        val body=column();root.addView(paged(body,identity.hi),LinearLayout.LayoutParams(-1,0,1f))
        body.add(label(entry.step.text(identity.hi),22f,bold=true).asHeading(),bottom=12)
        body.add(card(if(entry.correct)Palette.successBg else Palette.dangerBg).apply{add(label(t("Recorded choice: ","दर्ज विकल्प: ")+entry.choice.text(identity.hi),17f,bold=true),bottom=10);add(label(if(entry.correct)t("Appropriate simulated action","उपयुक्त काल्पनिक क्रिया")else t("Sequence diverged here","क्रम यहाँ अलग हुआ"),15f))},bottom=12)
        body.add(label(entry.step.explain(identity.hi),17f),bottom=12)
        body.add(label(t("Evidence: ${entry.presentation}. Original events remain unchanged. Draft safety content needs competent review.","प्रमाण: ${entry.presentation}। मूल घटनाएँ अपरिवर्तित हैं। सुरक्षा प्रारूप की विशेषज्ञ समीक्षा ज़रूरी है।"),13f,Palette.muted),bottom=10)
        if(!entry.correct){
            val catalog=JSONObject(assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
            val objective=catalog.getJSONArray("objectives").objects().firstOrNull{it.getJSONArray("procedureSteps").strings().contains(entry.step.id)}
            body.add(action(t("Practise this weak objective","इस कमज़ोर कौशल का अभ्यास करें")){startActivity(Intent(this,SkillPracticeActivity::class.java).putExtra("objectiveId",objective?.getString("id")))},bottom=10)
        }
        if(index<entries.lastIndex)root.add(action(t("Next decision","अगला निर्णय")){index++;render()},top=8)
        root.add(action(t("Return to progress","प्रगति पर लौटें"),false,ActionRole.NEUTRAL){finish()},top=8)
        setContentView(root)
    }
    @android.annotation.SuppressLint("GestureBackNavigation") @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){if(popContentPage())return;if(index>0){index--;render()}else finish()}
    override fun onSaveInstanceState(out:Bundle){out.putInt("index",index);super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();if(::identity.isInitialized&&!identity.isCurrentProfile())finish()}
    override fun onDestroy(){back.close();identity.close();super.onDestroy()}
}
