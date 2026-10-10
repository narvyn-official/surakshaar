package com.narvyn.suraksha

import android.app.Activity
import android.os.Bundle
import android.widget.*
import org.json.JSONObject
import java.util.concurrent.Executors

/** Search stays on-device. Only explicit online tutor requests send the typed question. */
class QuestionGuideActivity:Activity(){
    private lateinit var identity:Store
    private lateinit var bank:JSONObject
    private lateinit var body:LinearLayout
    private lateinit var results:LinearLayout
    private lateinit var query:EditText
    private var savedQuery=""
    private var area:String?=null
    private var token=0
    private val jobs=Executors.newSingleThreadExecutor()
    private val back by lazy{AppBackNavigation(this){if(popContentPage())Unit else if(area!=null){area=null;render()}else finish()}}
    private fun t(en:String,hi:String)=if(identity.hi)hi else en
    override fun onCreate(state:Bundle?){super.onCreate(state);identity=Store(this);bank=JSONObject(assets.open("question-bank.json").bufferedReader().use{it.readText()});area=state?.takeIf{it.getString("worker")==identity.workerId}?.getString("area");savedQuery=state?.takeIf{it.getString("worker")==identity.workerId}?.getString("query")?:"";render()}
    private fun render(){
        if(::query.isInitialized)savedQuery=query.text.toString()
        token++;back.enabled(true)
        val root=column(16).apply{setBackgroundColor(Palette.canvas)};trainingInsets(root)
        root.add(label(t("Questions & help","प्रश्न और मदद"),24f,bold=true).asHeading(),bottom=10)
        body=column();root.addView(paged(body,identity.hi),LinearLayout.LayoutParams(-1,0,1f))
        body.add(label(t("Search stays on this phone. New detailed references are English drafts; Hindi topic summaries and reviewed Santali content remain pending. Avoid personal or medical information.","खोज इस फ़ोन पर रहती है। नई विस्तृत जानकारी अंग्रेज़ी प्रारूप है; हिन्दी सारांश और समीक्षा वाली संताली सामग्री बाकी है। निजी या चिकित्सा जानकारी न लिखें।"),13f,Palette.muted),bottom=10)
        query=EditText(this).apply{hint=t("Search a question","प्रश्न खोजें");contentDescription=hint;isSingleLine=true;minHeight=dp(48);setText(savedQuery);tag="question-guide-query"};body.add(query)
        body.add(action(t("Search offline guide","ऑफ़लाइन जानकारी खोजें"),false){search()},bottom=8)
        body.add(action(t("Ask online tutor","ऑनलाइन शिक्षक से पूछें"),false,ActionRole.NEUTRAL){askOnline()},bottom=10)
        results=column();body.add(results)
        root.add(action(t("Back","वापस"),false,ActionRole.NEUTRAL){if(area!=null){area=null;render()}else finish()},top=10)
        setContentView(root)
        bank.getJSONArray("areas").objects().filter{area==null||it.getString("id")==area}.forEach{group->
            if(area==null)results.add(action(group.local("title",identity.hi),false){area=group.getString("id");render()},bottom=8)
            else group.getJSONArray("cases").objects().forEach{item->addCase(item)}
        }
    }
    private fun addCase(item:JSONObject){results.add(action(item.getString("question"),false,ActionRole.NEUTRAL){
        val source=bank.getJSONObject("sources").getJSONObject(item.getString("sourceId"))
        val content=column(14)
        content.add(label(if(source.getString("status")=="draft")t("SAFETY DRAFT · COMPETENT REVIEW REQUIRED","सुरक्षा प्रारूप · विशेषज्ञ समीक्षा ज़रूरी")else t("PRODUCT REFERENCE","उत्पाद की जानकारी"),13f,Palette.amber,true),bottom=12)
        content.add(label(item.getString("behavior"),17f),bottom=12)
        content.add(label(t("Required context: ","ज़रूरी संदर्भ: ")+item.getString("requiredContext"),14f),bottom=10)
        content.add(label("Source: ${source.getString("title")} · ${source.getString("version")}\nCase ${item.getString("id")} · ${bank.getString("version")}",13f,Palette.muted))
        PageDialogBuilder(this).setTitle(item.getString("question")).setView(paged(content,identity.hi)).setPositiveButton(t("Close","बंद करें"),null).show()
    },bottom=8)}
    private fun search(){
        token++;results.removeAllViews()
        val terms=query.text.toString().lowercase().trim().split(Regex("\\s+")).filter{it.length>1}
        val cases=bank.getJSONArray("areas").objects().flatMap{it.getJSONArray("cases").objects()}
        val matches=cases.filter{q->terms.all{q.getString("question").lowercase().contains(it)||q.getString("behavior").lowercase().contains(it)}}
        results.add(label(t("${matches.size} matching references","${matches.size} जानकारी मिली"),15f,Palette.blue).asHeading(),bottom=10)
        if(matches.isEmpty())results.add(label(t("Try a shorter phrase, or return to a topic. An unmatched question is not permission to guess a procedure.","छोटा वाक्य लिखें या विषय पर लौटें। प्रश्न न मिलना प्रक्रिया का अनुमान लगाने की अनुमति नहीं है।")),bottom=10)
        matches.forEach{addCase(it)}
    }
    private fun askOnline(){
        val text=query.text.toString().trim()
        if(text.length !in 3..500){query.error=t("Enter a question of 3–500 characters","3–500 अक्षर का प्रश्न लिखें");return}
        if(identity.attempts().any{!it.optBoolean("finished")&&it.optString("kind")=="assessment"}){notice(t("Assessment in progress","मूल्यांकन चल रहा है"),t("Finish the saved assessment before requesting tutoring. No assessment answers are supplied.","शिक्षक से पूछने से पहले सहेजा मूल्यांकन पूरा करें। मूल्यांकन के उत्तर नहीं दिए जाते।"));return}
        if(!LearningSync.linked(this,identity.workerId)){notice(t("Link your account first","पहले खाता जोड़ें"),t("Use your account and training-centre action. Offline help remains available.","खाता और प्रशिक्षण केंद्र खोलें। ऑफ़लाइन मदद उपलब्ध है।"));return}
        val request=++token;val api=AdminClient(this);val origin=api.identityStamp()
        val status=label(t("Connecting · question only, no camera or audio upload","कनेक्ट हो रहा है · केवल प्रश्न, कैमरा या आवाज़ नहीं भेजी जाती"),14f,Palette.blue)
        results.removeAllViews();results.add(status)
        jobs.execute{try{
            val result=api.call("/api/tutor",JSONObject().put("workerId",identity.workerId).put("question",text).put("language",if(identity.hi)"hi"else"en"),expectedIdentity=origin)
            runOnUiThread{if(!isDestroyed&&request==token&&identity.isCurrentProfile()&&AdminClient(this).identityStamp()==origin){status.text=result.getString("answer");results.add(label(result.optString("sourceVersion","")+" · "+result.getString("status"),13f,Palette.muted),top=12)}}
        }catch(_:Exception){runOnUiThread{if(!isDestroyed&&request==token&&identity.isCurrentProfile())status.text=t("Tutor unavailable. Your offline guide and training still work. Try again when connected.","शिक्षक उपलब्ध नहीं। ऑफ़लाइन जानकारी और प्रशिक्षण चल सकते हैं। कनेक्शन पर फिर कोशिश करें।")}}}
    }
    @android.annotation.SuppressLint("GestureBackNavigation") @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){if(popContentPage())return;if(area!=null){area=null;render()}else finish()}
    override fun onSaveInstanceState(out:Bundle){out.putString("area",area);out.putString("query",query.text.toString());out.putString("worker",identity.workerId);super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();if(::identity.isInitialized&&!identity.isCurrentProfile())finish()}
    override fun onPause(){token++;super.onPause()}
    override fun onDestroy(){token++;back.close();jobs.shutdownNow();identity.close();super.onDestroy()}
}
