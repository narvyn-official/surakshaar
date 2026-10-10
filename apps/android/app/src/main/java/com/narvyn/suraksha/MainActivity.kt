package com.narvyn.suraksha

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.view.*
import android.widget.*
import com.google.ar.core.ArCoreApk
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity: Activity() {
    private lateinit var store: Store
    private lateinit var curriculum: Curriculum
    private val learningCatalog by lazy{LearningCatalog(this)}
    private lateinit var body: LinearLayout
    private lateinit var pages: PagedPanel
    private var page="home"
    private val pageHistory=mutableListOf<String>()
    private val backNavigation by lazy { AppBackNavigation(this){navigateBack()} }
    private var selected="fire"
    private var lessonIndex=0
    private var session: TrainingSession?=null
    private var pendingExportId: String?=null
    private var pendingExportWorker: String?=null
    private fun pendingExportFile()=pendingExportId?.let { java.io.File(cacheDir,"pending-export-$it") }
    private var exportBytes: ByteArray?
        get() {
            check(store.isCurrentProfile() && pendingExportWorker==store.workerId) { "The active learner changed." }
            return pendingExportFile()?.takeIf { it.exists() }?.readBytes()
        }
        set(value) {
            if(value==null) { pendingExportFile()?.delete();pendingExportId=null;pendingExportWorker=null;return }
            check(store.isCurrentProfile()) { "The active learner changed." }
            val id=UUID.randomUUID().toString();val file=java.io.File(cacheDir,"pending-export-$id")
            file.writeBytes(value)
            pendingExportFile()?.delete();pendingExportId=id;pendingExportWorker=store.workerId
        }
    private lateinit var narrator:TrainingNarrator
    private fun pauseSpeech(){if(::narrator.isInitialized)narrator.pause()}
    private var spokenLearningKey:String?=null
    private val accessJobs=java.util.concurrent.Executors.newSingleThreadExecutor()
    private var authorisedStaff=false
    private var accessRevision=0
    private var learningOverlay:Dialog?=null
    private val compactWorkspace get()=resources.configuration.screenWidthDp>=600&&resources.configuration.screenHeightDp<480
    private val hi get()=store.hi
    private fun t(en: String,hiText: String)=if(hi)hiText else en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);store=Store(this);curriculum=Curriculum(this)
        pendingExportId=savedInstanceState?.getString("pendingExportId")?.takeIf { runCatching { UUID.fromString(it).toString()==it }.getOrDefault(false) }
        pendingExportWorker=savedInstanceState?.getString("pendingExportWorker")
        pageHistory.addAll(savedInstanceState?.getStringArrayList("pageHistory").orEmpty());page=savedInstanceState?.getString("page")?:"home";selected=savedInstanceState?.getString("selected")?:"fire"
        lessonIndex=savedInstanceState?.getInt("lessonIndex")?:0
        savedInstanceState?.getString("session")?.let{store.attempt(it)?.let{data->session=TrainingSession(data,curriculum.module(data.getString("moduleId")))}}
        if(page=="training"&&session==null)page="home"
        narrator=TrainingNarrator(this){store.hi}
        render()
    }
    override fun onResume(){super.onResume();if(::store.isInitialized && !store.isCurrentProfile()){narrator.clear();spokenLearningKey=null;store.close();store=Store(this);session=null;page="home";pageHistory.clear();lessonIndex=0;exportBytes=null;render()}else if(::store.isInitialized && page in listOf("records","module","home","courses")) render();if(::store.isInitialized&&LearningSync.linked(this,store.workerId))LearningSync.schedule(this);refreshWorkspaceAccess()}
    override fun onSaveInstanceState(out: Bundle) {super.onSaveInstanceState(out);out.putStringArrayList("pageHistory",ArrayList(pageHistory));out.putString("page",page);out.putString("selected",selected);out.putInt("lessonIndex",lessonIndex);out.putString("session",session?.data?.getString("id"));out.putString("pendingExportId",pendingExportId);out.putString("pendingExportWorker",pendingExportWorker)}
    override fun onDestroy(){learningOverlay?.dismiss();learningOverlay=null;accessRevision++;accessJobs.shutdownNow();backNavigation.close();pauseSpeech();narrator.close();store.close();super.onDestroy()}
    override fun onPause(){learningOverlay?.dismiss();learningOverlay=null;pauseSpeech();super.onPause()}
    override fun onWindowFocusChanged(focused:Boolean){super.onWindowFocusChanged(focused);if(focused)narrateVisibleLearning()else pauseSpeech()}
    private fun narrateVisibleLearning(){
        if(!::store.isInitialized||!::curriculum.isInitialized||!::narrator.isInitialized||!hasWindowFocus()||isFinishing||!store.isCurrentProfile()||!getSharedPreferences("room-voice",MODE_PRIVATE).getBoolean("automatic",true))return
        if(getSystemService(android.view.accessibility.AccessibilityManager::class.java).isTouchExplorationEnabled)return
        val pair=when(page){
            "lesson"->{val item=curriculum.module(selected).getJSONArray("lessons").optJSONObject(lessonIndex)?:return;"lesson:$selected:$lessonIndex:$hi" to (item.local("title",hi)+". "+item.local("body",hi))}
            "training"->{val s=session?:return;if(s.finished)return;val q=s.current;val feedback=s.data.optBoolean("awaitingContinue");"${s.data.optString("id")}:${s.index}:$feedback:$hi" to if(feedback)if(s.guided)q.local("explanation",hi)else t("Answer saved. Continue when ready.","उत्तर सहेजा गया। तैयार होने पर आगे बढ़ें।")else q.local("prompt",hi)+". "+s.options().joinToString(". "){it.local("text",hi)}}
            else->return
        }
        if(spokenLearningKey==pair.first){narrator.automatic();return}
        spokenLearningKey=pair.first;speak(pair.second,true)
    }
    private fun parentPage()=when(page){"training","lesson","practice"->"module";"verify","legacy-records"->"records";"library","module"->"courses";else->"home"}
    // API 33+ uses AppBackNavigation and PagedPanel callbacks; retain this fallback for API 29–32.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){if(page!="home"||(::pages.isInitialized&&pages.hasPrevious))navigateBack()else super.onBackPressed()}
    private fun navigateBack(){
        pauseSpeech()
        if(::pages.isInitialized&&pages.previousPage())return
        if(page=="lesson"&&lessonIndex>0){lessonIndex--;saveLessonPosition(lessonIndex);render();return}
        page=if(pageHistory.isNotEmpty())pageHistory.removeAt(pageHistory.lastIndex)else parentPage()
        render()
    }
    private fun go(to: String){pauseSpeech();
        if(to!=page){
            if(to=="home")pageHistory.clear()
            else if(to in pageHistory){while(pageHistory.isNotEmpty()&&pageHistory.last()!=to)pageHistory.removeAt(pageHistory.lastIndex);pageHistory.removeAt(pageHistory.lastIndex)}
            else pageHistory.add(page)
        }
        if(to=="training"){session?.let{if(!it.finished&&it.data.optString("mode")=="arcore"){it.data.put("mode","hybrid");store.save(it)}}};page=to;render()}

    private fun render(){
        backNavigation.enabled(page!="home")
        val root=column().apply{setBackgroundColor(Palette.canvas)}
        root.setOnApplyWindowInsetsListener{v,i->if(android.os.Build.VERSION.SDK_INT>=30){val b=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom)}else{v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom)};i}
        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(4),dp(16),dp(4));setBackgroundColor(Palette.surface)}
        if(page in listOf("module","lesson","practice","training","verify","help","library","legacy-records")) {
            header.addView(practiceTool("",PracticeIcon.BACK,t("Back","वापस")){navigateBack()}.apply{tag="main-back"},LinearLayout.LayoutParams(dp(48),dp(48)))
        } else {
            header.addView(ImageView(this).apply{setImageDrawable(appIcon("shield"));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(28),dp(28)))
        }
        header.addView(label(getString(R.string.app_name),16f,Palette.ink,true).apply{setPadding(dp(8),0,dp(8),0)},LinearLayout.LayoutParams(0,-2,1f))
        header.addView(action(if(hi)"हिन्दी ▾" else "EN ▾",false,role=ActionRole.NEUTRAL){language()}.apply {
            textSize=13f;minHeight=dp(48);minimumHeight=dp(48);setPadding(dp(10),dp(8),dp(10),dp(8));contentDescription=t("Choose language: English","भाषा चुनें: हिन्दी")
        },LinearLayout.LayoutParams(-2,-2))
        header.addView(action("",false,role=ActionRole.NEUTRAL){profileMenu()}.apply {
            tag="main-profile";contentDescription=t("Your learning profile","आपकी सीखने की प्रोफ़ाइल")
            setCompoundDrawables(appIcon("user",Palette.ink,22),null,null,null);setPadding(dp(13),dp(12),dp(13),dp(12));minHeight=dp(48);minimumHeight=dp(48)
        },LinearLayout.LayoutParams(dp(48),dp(48)).apply{marginStart=dp(6)})
        root.add(header)
        body=column(if(compactWorkspace&&page in listOf("home","courses"))10 else 18).apply{tag="main-page-$page"};pages=paged(body,hi).apply{onPageChanged={backNavigation.enabled(page!="home"||hasPrevious)}}
        root.addView(pages,LinearLayout.LayoutParams(-1,0,1f))
        when(page){"home"->home();"courses"->courses();"library"->library();"module"->module();"lesson"->lesson();"practice"->practiceOptions();"training"->training();"records"->records();"legacy-records"->legacyRecords();"help"->help();"verify"->verifyPage();else->home()}
        body.accessibilityPaneTitle=when(page){"home"->t("Learning home","सीखने का होम");"courses"->t("Course catalog","पाठ्यक्रम सूची");"library"->t("Industrial learning library","औद्योगिक सीखने की लाइब्रेरी");"lesson"->t("Lesson ${lessonIndex+1}","पाठ ${lessonIndex+1}");"practice"->t("Practice options","अभ्यास के विकल्प");"module"->curriculum.module(selected).local("title",hi);"training"->t("Training","प्रशिक्षण");"records"->t("My learning record","मेरा सीखने का रिकॉर्ड");"verify"->t("Verify a record","रिकॉर्ड जाँचें");else->t("Help","मदद")}
        if(page=="lesson")root.add(lessonNavigation())
        root.addView(View(this).apply{setBackgroundColor(Palette.line)},LinearLayout.LayoutParams(-1,dp(1)))
        val nav=LinearLayout(this).apply{setPadding(dp(16),dp(6),dp(16),dp(6));setBackgroundColor(Palette.surface);gravity=Gravity.CENTER_VERTICAL}
        listOf(Triple("home","Home","होम"),Triple("courses","Learn","सीखें"),Triple("records","Progress","प्रगति")).forEach{(dest,en,h)->
            val active=page==dest||(dest=="courses"&&page in listOf("module","training","lesson","practice","library"))||(dest=="records"&&page in listOf("verify","legacy-records"))
            nav.addView(action(t(en,h),false,role=ActionRole.NEUTRAL){go(dest)}.apply {
                tag="main-nav-$dest";isSelected=active;textSize=12f;minHeight=dp(if(compactWorkspace)48 else 62);minimumHeight=dp(if(compactWorkspace)48 else 62)
                setPadding(dp(4),dp(6),dp(4),dp(6));compoundDrawablePadding=dp(3)
                setTextColor(if(active)Palette.blue else Palette.muted)
                background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x18086f62),shape(if(active)Palette.soft else Palette.surface,16),null)
                val icon=if(dest=="home")homeIcon(if(active)Palette.blue else Palette.muted)else appIcon(if(dest=="courses")"learn"else dest,if(active)Palette.blue else Palette.muted,22)
                if(compactWorkspace){setCompoundDrawables(icon,null,null,null);compoundDrawablePadding=dp(8)}else setCompoundDrawables(null,icon,null,null)
                contentDescription=t(en,h)+if(active)t(", selected",", चयनित")else ""
            },LinearLayout.LayoutParams(0,-2,1f).apply{if(dest!="help")marginEnd=dp(8)})
        }
        root.add(nav);setContentView(root);root.requestApplyInsets();root.post{narrateVisibleLearning()}
    }
    private fun title(text: String,sub: String?=null){body.add(label(text,24f,Palette.ink,true).asHeading(),bottom=6);sub?.let{body.add(label(it,15f,Palette.muted),bottom=16)}}
    private fun chip(text: String,colour: Int=Palette.soft,ink: Int=Palette.blue)=label(text,12f,ink).apply{background=shape(colour,8);setPadding(dp(10),dp(6),dp(10),dp(6))}
    private fun refreshWorkspaceAccess(){
        authorisedStaff=false
        val api=AdminClient(this);if(!api.hasSession())return
        val token=++accessRevision;val worker=store.workerId;val stamp=api.identityStamp()
        accessJobs.execute{val allowed=runCatching{val result=api.call("/api/admin/session",expectedIdentity=stamp);result.optJSONObject("current")!=null||result.optJSONArray("workspaces")?.length()?.let{it>0}==true||result.optBoolean("operator")}.getOrDefault(false)
            runOnUiThread{if(!isDestroyed&&token==accessRevision&&store.isCurrentProfile()&&worker==store.workerId&&AdminClient(this).identityStamp()==stamp)authorisedStaff=allowed}}
    }
    private fun profileMenu(){
        val items=mutableListOf(t("Account & training centre","खाता और प्रशिक्षण केंद्र"),t("Edit my profile","मेरी प्रोफ़ाइल बदलें"),t("Learners on this phone","इस फ़ोन के शिक्षार्थी"),t("Help & settings","मदद और सेटिंग"),t("Offline content","ऑफ़लाइन सामग्री"),t("Learning notifications","सीखने की सूचनाएँ"),t("Centre staff invitation","केंद्र स्टाफ का निमंत्रण"))
        if(authorisedStaff)items.add(t("Manage training","प्रशिक्षण प्रबंधन"))
        PageDialogBuilder(this).setTitle(t("Learn workspace","सीखने की जगह")).setItems(items.toTypedArray()){_,i->when(i){
            0->startActivity(Intent(this,LearnerCentreActivity::class.java))
            1->profile()
            2->startActivity(Intent(this,WorkerProfilesActivity::class.java))
            3->go("help")
            4->startActivity(Intent(this,OfflineContentActivity::class.java))
            5->LearningInboxDialog.show(this,store)
            else->startActivity(Intent(this,AdminActivity::class.java))
        }}.show()
    }
    private fun moduleColour(id:String)=when(id){"fire"->Palette.amber;"gas"->Palette.violet;"machinery"->Palette.blue;"emergency"->Palette.danger;else->Palette.teal}
    private fun pair(first:Button,second:Button){
        if(resources.configuration.fontScale>=1.5f){body.add(first,bottom=8);body.add(second,bottom=8);return}
        val row=LinearLayout(this).apply{isBaselineAligned=false;gravity=Gravity.TOP}
        listOf(first,second).forEachIndexed{index,button->
            button.textSize=14f;button.minHeight=dp(76);button.minimumHeight=dp(76);button.setPadding(dp(10),dp(10),dp(10),dp(10))
            row.addView(button,LinearLayout.LayoutParams(0,-2,1f).apply{if(index==0)marginEnd=dp(8)})
        }
        body.add(row,bottom=8)
    }
    private fun language(){
        PageDialogBuilder(this).setTitle(t("Choose language","भाषा चुनें")).setItems(arrayOf("English","हिन्दी","Santali · review pending",if(getSharedPreferences("room-voice",MODE_PRIVATE).getBoolean("automatic",true))t("Mute automatic instructions","अपने आप निर्देश बोलना बंद करें")else t("Enable automatic instructions","अपने आप निर्देश बोलना चालू करें"))){_,which->
            if(which==3){narrator.setAutomatic(!narrator.automaticEnabled);spokenLearningKey=null;render()}
            else if(which==2)notice("Santali", "Native-speaker review and recordings are required before Santali lessons can be released. Hindi and English are available.")
            else {pauseSpeech();store.hi=which==1;render()}
        }.show()
    }
    private fun profile(){
        val field=EditText(this).apply{setSingleLine();hint=t("Your name","आपका नाम");setText(store.name);setPadding(dp(20),dp(16),dp(20),dp(16))}
        val sectors=listOf("Unspecified","Mining","Steel","Mica","Other")
        val sector=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,if(hi)listOf("नहीं चुना","खनन","इस्पात","अभ्रक","अन्य")else sectors);setSelection(sectors.indexOf(store.sector).coerceAtLeast(0));minimumHeight=dp(56)}
        val form=column(16);form.add(field);form.add(label(t("Work sector","काम का क्षेत्र"),14f),top=12);form.add(sector)
        PageDialogBuilder(this).setTitle(t("Your learning profile","आपकी सीखने की प्रोफ़ाइल")).setMessage(t("Stored on this phone. No email or account required.","इस फ़ोन पर सुरक्षित। ईमेल या खाते की ज़रूरत नहीं।")).setView(form).setPositiveButton(t("Save","सहेजें")){_,_->store.name=field.text.toString().trim().take(80);store.sector=sectors[sector.selectedItemPosition];render()}.setNegativeButton(t("Cancel","रद्द करें"),null).show()
    }
    private fun launchCourse(id:String){startActivity(Intent(this,LaunchCourseActivity::class.java).putExtra("courseId",id))}
    private fun learningText(value:JSONObject,key:String)=if(hi)value.optString(key+"Hi").ifBlank{value.optString(key)}else value.optString(key)
    private fun account(){startActivity(Intent(this,LearnerCentreActivity::class.java).putExtra(LearnerCentreActivity.EXTRA_DESTINATION,"account"))}
    private data class HomePractice(val title:String,val course:String,val updatedAt:Long,val completed:Boolean,val resumable:Boolean,val detail:String,val intent:Intent)
    /** Read-only summaries use validated current-content records belonging to the selected learner. */
    private fun homePractices():List<HomePractice>{
        val launch=learningCatalog
        val school=getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${store.workerId}","")=="School"
        val scenarios=launch.scenarios.associateBy{it.getString("id")}
        val records=LearningProgress(this,store.workerId).use{it.records()}
        // The scenario host resumes its newest unfinished record. Never promise an older one instead.
        val resumable=records.filter{!it.optBoolean("finished")&&it.optString("contentVersion")==launch.version}.distinctBy{it.optString("scenarioId")}.map{it.optString("id")}.toSet()
        val decisions=records.mapNotNull{r->runCatching{
            val scenario=scenarios[r.getString("scenarioId")]?:return@runCatching null
            if(school&&scenario.optString("audience")!="awareness")return@runCatching null
            val a=LearningAttempt.restore(r,scenario,launch.version,store.workerId)
            HomePractice(learningText(scenario,"title"),r.getString("courseId"),r.getLong("updatedAt"),a.finished,!a.finished&&r.getString("id") in resumable,
                t("${a.index} of ${a.steps.size} steps complete","${a.steps.size} में से ${a.index} चरण पूरे"),
                Intent(this,LearningScenarioActivity::class.java).putExtra("scenarioId",r.getString("scenarioId")).putExtra("attemptId",r.getString("id")))
        }.getOrNull()}
        val bays=if(school)emptyList()else SkillPracticeStore(this,store.workerId,"first-aid-bay.db").use{it.records()}.mapNotNull{r->runCatching{
            val a=FirstAidBay.restore(r,store.workerId,launch.version)
            HomePractice(a.title(hi),"first-aid",r.getLong("updatedAt"),a.finished,!a.finished&&!a.stopped,
                t("${a.index} of ${a.steps.size} steps complete","${a.steps.size} में से ${a.index} चरण पूरे"),
                Intent(this,FirstAidBayActivity::class.java).putExtra("attemptId",r.getString("id")))
        }.getOrNull()}
        val rooms=if(school)emptyList()else RoomMissionStore(this,store.workerId).use{it.records()}.filter{it.optString("module")=="fire"}.mapNotNull{r->runCatching{
            require(r.getString("workerId")==store.workerId)
            val a=RoomMission.restore(r.getJSONObject("mission"));val coaching=RoomCoaching.restore(r.getJSONObject("coaching"),"fire")
            val camera=r.getString("mode")=="camera"
            HomePractice(a.selectedScenario?.title(hi)?:t("Fire response practice","अग्नि प्रतिक्रिया अभ्यास"),"fire",r.getLong("updatedAt"),a.completed,!a.completed,
                t(if(camera)"Camera AR · saved practice"else"3D · saved practice",if(camera)"कैमरा AR · सहेजा अभ्यास"else"3D · सहेजा अभ्यास"),
                Intent(this,RoomMissionActivity::class.java).putExtra("moduleId","fire").putExtra("camera",camera)
                    .putExtra("resumeId",r.getString("id")).putExtra("recall",coaching.recall).putExtra("explosionRisk",a.explosionRisk).putExtra("fireScenarioId",a.fireScenarioId))
        }.getOrNull()}
        return (decisions+bays+rooms).sortedByDescending{it.updatedAt}
    }
    private fun home(){
        val history=homePractices();val next=history.firstOrNull{it.resumable}
        if(!compactWorkspace)title(t("Your learning","आपकी सीख"))
        if(!compactWorkspace&&store.name.isNotBlank())body.add(label(store.name,14f,Palette.muted).apply{tag="main-home-learner"},bottom=12)
        val focus=column(if(compactWorkspace)10 else 14).apply{background=shape(Palette.tealBg,18);tag="main-home-focus"}
        if(compactWorkspace&&store.name.isNotBlank())focus.add(label(store.name,12f,Palette.teal).apply{tag="main-home-learner"},bottom=4)
        if(next==null){
            focus.add(label(t(if(history.isEmpty())"Start your first practice"else"Ready for your next practice?",if(history.isEmpty())"अपना पहला अभ्यास शुरू करें"else"अगले अभ्यास के लिए तैयार?"),if(compactWorkspace)16f else 19f,Palette.ink,true).asHeading(),bottom=6)
            if(!compactWorkspace)focus.add(label(t("Choose Fire Safety or First Aid in Learn.","सीखें में अग्नि सुरक्षा या प्राथमिक उपचार चुनें।"),14f,Palette.muted),bottom=12)
            focus.add(action(t("Browse courses","पाठ्यक्रम देखें")){go("courses")}.apply{tag="main-browse-courses"})
        }else{
            if(!compactWorkspace)focus.add(label(t("CONTINUE LEARNING","सीखना जारी रखें"),12f,Palette.teal,true),bottom=6)
            focus.add(label(next.title,if(compactWorkspace)16f else 19f,Palette.ink,true).asHeading().apply{tag="main-continue-title"},bottom=6)
            focus.add(label(next.detail,14f,Palette.muted).apply{tag="main-continue-detail"},bottom=12)
            focus.add(action(t("Resume practice","अभ्यास जारी रखें")){startActivity(next.intent)}.apply{tag="main-continue";contentDescription=t("Resume ","जारी रखें: ")+next.title})
        }
        val details=if(compactWorkspace)column()else body
        if(!compactWorkspace)body.add(focus,bottom=14)
        val completed=history.count{it.completed};val inProgress=history.count{it.resumable}
        val summary=if(history.isEmpty())t("No practice saved yet","अभी कोई अभ्यास नहीं सहेजा गया")else t("$completed completed · $inProgress in progress","$completed पूरे · $inProgress जारी")
        details.add(action(if(compactWorkspace)summary else t("Your progress","आपकी प्रगति")+"\n"+summary,false,ActionRole.REVIEW){go("records")}.apply{
            tag="main-home-progress";contentDescription=t("Your progress. ","आपकी प्रगति। ")+summary;textSize=14f;gravity=Gravity.START or Gravity.CENTER_VERTICAL
            setCompoundDrawables(appIcon("records",Palette.violet,22),null,null,null);compoundDrawablePadding=dp(10)
        },bottom=12)
        history.firstOrNull{it.completed}?.let{recent->
            if(!compactWorkspace)details.add(label(t("LATEST COMPLETED PRACTICE","हाल में पूरा अभ्यास"),12f,Palette.muted,true),bottom=4)
            details.add(label(if(compactWorkspace)t("Latest: ","हाल का: ")+recent.title else recent.title+" · "+date(recent.updatedAt),if(compactWorkspace)12f else 14f,Palette.ink).apply{tag="main-home-recent"},bottom=if(compactWorkspace)6 else 12)
        }
        val savedSignIn=AdminClient(this).hasSession()
        details.add(action(t(if(savedSignIn)"Account & sync"else"Sign in or create account",if(savedSignIn)"खाता और सिंक"else"साइन इन या खाता बनाएँ"),false,ActionRole.NEUTRAL){account()}.apply{
            tag="main-home-account";setCompoundDrawables(appIcon("user",Palette.ink,20),null,null,null);compoundDrawablePadding=dp(8)
        },bottom=6)
        val status=if(compactWorkspace)t(if(savedSignIn)"Saved sign-in"else"Offline learning available",if(savedSignIn)"साइन इन सहेजा है"else"ऑफ़लाइन सीखना उपलब्ध है")else t(if(savedSignIn)"Saved sign-in · learning works offline."else"Not signed in · learning works offline.",
            if(savedSignIn)"साइन इन सहेजा है · सीखना ऑफ़लाइन चलता है।"else"साइन इन नहीं है · सीखना ऑफ़लाइन चलता है।")
        details.add(label(status,12f,Palette.muted).apply{tag="main-home-account-status"})
        if(compactWorkspace){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.TOP;isBaselineAligned=false}
            row.addView(focus,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(12)})
            row.addView(details,LinearLayout.LayoutParams(0,-2,1f));body.add(row)
        }
    }
    private fun launchCards(){
        val row=if(compactWorkspace)LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false}else null
        learningCatalog.courses.forEachIndexed{index,course->
            val id=course.getString("id");val accent=if(id=="fire")Palette.fire else Palette.teal
            val item=card().apply{
                tag="main-course-card-$id"
                add(label(learningText(course,"title"),if(compactWorkspace)17f else 20f,Palette.ink,true).asHeading().apply{setCompoundDrawables(appIcon(course.getString("icon"),accent,28),null,null,null);compoundDrawablePadding=dp(10)},bottom=if(compactWorkspace)10 else 6)
                if(!compactWorkspace)add(label(learningText(course,"subtitle"),14f,Palette.muted),bottom=12)
                add(action(t("Open course","पाठ्यक्रम खोलें"),false){launchCourse(id)}.apply{tag="course-$id";contentDescription=t("Open ","खोलें: ")+learningText(course,"title")})
            }
            if(row==null)body.add(item,bottom=14)else row.addView(item,LinearLayout.LayoutParams(0,-2,1f).apply{if(index==0)marginEnd=dp(12)})
        }
        row?.let{body.add(it,bottom=10)}
    }
    private fun courses(){
        if(!compactWorkspace)title(t("Learn","सीखें"),t("Choose a course to explore and practise.","समझने और अभ्यास के लिए पाठ्यक्रम चुनें।"))
        launchCards();body.add(action(t("Earlier industrial courses","पुराने औद्योगिक पाठ्यक्रम"),false,ActionRole.NEUTRAL){go("library")}.apply{tag="main-learning-library"})
    }
    private fun homeIcon(colour:Int):android.graphics.drawable.Drawable=object:android.graphics.drawable.Drawable(){
        private val pen=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=colour;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
        override fun draw(canvas:Canvas){canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/24f,bounds.height()/24f)
            val outline=Path().apply{moveTo(3f,11f);lineTo(12f,3f);lineTo(21f,11f);moveTo(5f,10f);lineTo(5f,21f);lineTo(10f,21f);lineTo(10f,15f);lineTo(14f,15f);lineTo(14f,21f);lineTo(19f,21f);lineTo(19f,10f)};canvas.drawPath(outline,pen);canvas.restore()}
        override fun setAlpha(alpha:Int){pen.alpha=alpha}
        override fun setColorFilter(filter:ColorFilter?){pen.colorFilter=filter}
        @Deprecated("Drawable opacity") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }.apply{setBounds(0,0,dp(22),dp(22))}
    private fun library(){
        title(t("Learning library","सीखने की लाइब्रेरी"),t("Earlier industrial courses and saved records remain available.","पुराने औद्योगिक पाठ्यक्रम और सहेजे रिकॉर्ड उपलब्ध हैं।"))
        curriculum.modules.forEach{m->val id=m.getString("id")
            body.add(action(m.local("title",hi),false){selected=id;go("module")}.apply{tag="legacy-$id"},bottom=10)
        }
    }
    /** All modules remain one tap away; the home no longer needs a vertical catalogue. */
    private fun modulePicker(){
        val list=column(16);val dialog=PageDialogBuilder(this).setTitle(t("Choose your training","अपना प्रशिक्षण चुनें"))
            .setView(paged(list,hi)).setNegativeButton(t("Cancel","रद्द करें"),null).create()
        curriculum.modules.forEach{m->
            val id=m.getString("id")
            val item=LinearLayout(this).apply{
                tag="main-module-$id";gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8))
                background=shape(if(id==selected)Palette.tealBg else Palette.surface,14,Palette.line)
            }
            item.addView(label(m.local("title",hi),15f,Palette.ink,true).asHeading(),LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(8)})
            item.addView(action(t("Start learning","सीखना शुरू करें"),false,role=ActionRole.LEARN){selected=id;lessonIndex=0;dialog.dismiss();go("module")}.apply{
                textSize=13f;setPadding(dp(8),dp(8),dp(8),dp(8));contentDescription=t("Start learning: ","सीखना शुरू करें: ")+m.local("title",hi)
            },LinearLayout.LayoutParams(dp(106),-2))
            list.add(item,bottom=8)
        }
        dialog.show()
    }
    private fun journey()=LearningJourney(this,store,curriculum)
    private fun openStage(stage:String){when(stage){"learn"->openLessons();"demonstration"->startActivity(Intent(this,DemonstrationActivity::class.java).putExtra("moduleId",selected));"review"->startActivity(Intent(this,SkillPracticeActivity::class.java).putExtra("moduleId",selected));"guided","independent"->startActivity(Intent(this,ProcedureActivity::class.java).putExtra("moduleId",selected).putExtra("guided",stage=="guided").putExtra("camera",true));else->chooseAssessment()}}
    private fun module(){
        val m=curriculum.module(selected)
        body.add(label(t("${m.getString("duration")} MIN · OFFLINE LESSONS","${m.getString("duration")} मिनट · ऑफ़लाइन पाठ"),12f,moduleColour(selected),true),bottom=6)
        title(m.local("title",hi),m.local("subtitle",hi))
        val previous=store.attempts().firstOrNull{it.optString("moduleId")==selected&&it.optString("kind")=="assessment"&&it.optBoolean("finished")}
        body.add(label(if(previous==null)t("Understand → demonstration → guided practice → independent practice → review → assessment.","सीखने का क्रम: पाठ → प्रदर्शन → निर्देशित अभ्यास → स्वतंत्र अभ्यास → समीक्षा → मूल्यांकन।")else if(!previous.getJSONObject("result").optBoolean("passed"))t("Recommended next: revisit explanations and practise before reassessment.","अगला सुझाव: दोबारा मूल्यांकन से पहले व्याख्या पढ़ें और अभ्यास करें।")else t("Assessment passed. Keep practising recall; certification requires an independent review.","मूल्यांकन पास। याद करके अभ्यास करते रहें; प्रमाणपत्र के लिए स्वतंत्र समीक्षा आवश्यक है।"),14f,Palette.muted),bottom=12)
        val stages=journey().stages(selected)
        val next=stages.firstOrNull{!it.complete}
        val path=card();stages.forEachIndexed{i,stage->path.add(label("${if(stage.complete)"✓" else (i+1).toString()+"."} ${if(hi)stage.hi else stage.en}",14f,if(stage.complete)Palette.teal else Palette.ink),bottom=6)};body.add(path,bottom=12)
        val primary=column(14).apply{background=shape(Palette.tealBg,18)}
        primary.add(action(if(next!=null)t("Continue: ${next.en}","जारी रखें: ${next.hi}")else t("View certification progress","प्रमाणपत्र की प्रगति देखें")){if(next!=null)openStage(next.id)else startActivity(Intent(this,LearnerCentreActivity::class.java))}.apply{tag="main-start-training"})
        primary.add(label(t("Your progress is specific to this learner. Every practice remains available.","प्रगति इस शिक्षार्थी की है। हर अभ्यास उपलब्ध है।"),13f,Palette.teal),top=8)
        body.add(primary,bottom=14)
        body.add(action(t("Course actions & practice options","पाठ्यक्रम क्रियाएँ और अभ्यास विकल्प"),false,ActionRole.NEUTRAL){
            val labels=arrayOf(t("Read or listen to lessons","पाठ पढ़ें या सुनें"),t("Watch a worked example","पूरा उदाहरण देखें"),t("Guided procedure rehearsal","निर्देशित प्रक्रिया अभ्यास"),t("Independent scenario check","स्वतंत्र दृश्य जाँच"),t("Transfer challenge & hidden faults","बदला दृश्य और छिपी खराबियाँ"),t("Take an assessment","मूल्यांकन शुरू करें"),t("Explore in 3D","3D में देखें"),t("More practice options","अभ्यास के और विकल्प"))
            PageDialogBuilder(this).setTitle(t("Course actions","पाठ्यक्रम क्रियाएँ")).setItems(labels){_,i->when(i){0->openLessons();1->openStage("demonstration");2->openStage("guided");3->openStage("independent");4->openStage("review");5->chooseAssessment();6->startActivity(Intent(this,EquipmentActivity::class.java).putExtra("moduleId",selected));else->go("practice")}}.show()
        }.apply{tag="main-course-actions"},bottom=10)
        body.add(action(t("Questions about this training","इस प्रशिक्षण के प्रश्न"),false,ActionRole.NEUTRAL){startActivity(Intent(this,QuestionGuideActivity::class.java))},bottom=10)
        body.add(label(t("Local practice remains unlocked. An account-linked certification request needs the required completed evidence; demonstrations and transfer practice cannot issue credentials.","स्थानीय अभ्यास खुला है। खाते से जुड़ी प्रमाणपत्र समीक्षा के लिए पूरा प्रमाण चाहिए; प्रदर्शन और बदले दृश्य का अभ्यास प्रमाणपत्र जारी नहीं करते।"),13f,Palette.muted))
    }
    private fun startRoomMission(recall:Boolean){
        startActivity(Intent(this,RoomMissionActivity::class.java).putExtra("moduleId",selected).putExtra("guided",!recall).putExtra("camera",true).putExtra("recall",recall))
    }
    private fun chooseAssessment(){
        PageDialogBuilder(this).setTitle(t("Assessment mode","मूल्यांकन का तरीका")).setItems(arrayOf(t("On-screen decisions","स्क्रीन पर निर्णय"),t("Camera AR","कैमरा AR"))){_,i->startTraining(false,i==1)}.show()
    }
    private fun lessonKey()="${store.workerId}:${curriculum.version}:$selected"
    private fun openLessons(){lessonIndex=getSharedPreferences("lesson-bookmarks",MODE_PRIVATE).getInt(lessonKey(),0).coerceAtLeast(0);go("lesson")}
    private fun saveLessonPosition(index:Int){getSharedPreferences("lesson-bookmarks",MODE_PRIVATE).edit().putInt(lessonKey(),index).apply()}
    private fun lesson(){
        val m=curriculum.module(selected);val lessons=m.getJSONArray("lessons").objects()
        if(lessons.isEmpty()){body.add(label(t("No lessons are available for this module.","इस मॉड्यूल के पाठ उपलब्ध नहीं हैं।")));body.add(action(t("Back to module","मॉड्यूल पर वापस जाएँ")){go("module")},top=16);return}
        lessonIndex=lessonIndex.coerceIn(0,lessons.lastIndex)
        val item=lessons[lessonIndex]
        body.add(label(m.local("title",hi),14f,Palette.muted),bottom=8)
        body.add(chip(t("LESSON ${lessonIndex+1} OF ${lessons.size}","पाठ ${lessonIndex+1} / ${lessons.size}")),bottom=16)
        title(item.local("title",hi))
        body.add(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{
            max=lessons.size;progress=lessonIndex+1;progressTintList=android.content.res.ColorStateList.valueOf(Palette.teal)
            contentDescription=t("Lesson ${lessonIndex+1} of ${lessons.size}","पाठ ${lessonIndex+1} / ${lessons.size}")
        },bottom=20)
        body.add(card().apply{add(label(item.local("body",hi),18f).apply{tag="main-lesson-content"})},bottom=16)
        body.add(label(t("Your reading position is saved for this learner. Reading is not an assessment result.","इस शिक्षार्थी की पढ़ने की जगह सहेजी जाती है। पढ़ना मूल्यांकन परिणाम नहीं है।"),13f,Palette.muted),bottom=8)
        body.add(action(t("Audio","आवाज़"),false,role=ActionRole.LEARN){narrator.show(item.local("title",hi)+". "+item.local("body",hi));narrator.showControls()}.apply{tag="main-lesson-listen"})
    }
    private fun lessonNavigation():LinearLayout {
        val lessons=curriculum.module(selected).getJSONArray("lessons")
        val dock=LinearLayout(this).apply{isBaselineAligned=false;setPadding(dp(18),dp(8),dp(18),dp(8));setBackgroundColor(Palette.surface)}
        if(lessonIndex>0)dock.addView(action(t("Previous","पिछला"),false,role=ActionRole.NEUTRAL){lessonIndex--;saveLessonPosition(lessonIndex);go("lesson")}.apply{tag="main-lesson-previous";textSize=14f},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(8)})
        dock.addView(action(if(lessonIndex==lessons.length()-1)t("Finish reading","पढ़ना पूरा करें")else t("Next lesson","अगला पाठ")){
            if(lessonIndex==lessons.length()-1){journey().finishReading(selected);saveLessonPosition(0);go("module");LearningSync.schedule(this)}else{lessonIndex++;saveLessonPosition(lessonIndex);go("lesson")}
        }.apply{tag="main-lesson-next";textSize=14f},LinearLayout.LayoutParams(0,-2,if(lessonIndex>0)1.5f else 1f))
        return dock
    }
    private fun practiceOptions(){
        title(t("Choose your practice","अपना अभ्यास चुनें"),curriculum.module(selected).local("title",hi))
        body.add(action(t("Full AR scenario rehearsal","पूरा AR दृश्य अभ्यास"),false,role=ActionRole.CAMERA){openStage("guided")},bottom=12)
        body.add(action(t("Independent AR scenario check","स्वतंत्र AR दृश्य जाँच"),false,role=ActionRole.REVIEW){openStage("independent")},bottom=12)
        if(selected in listOf("fire","gas")) {
            body.add(label(t("Room missions","कमरे में मिशन"),18f,Palette.ink,true).asHeading(),bottom=10)
            body.add(action(t("Start immersive AR mission","इमर्सिव AR मिशन शुरू करें"),role=ActionRole.CAMERA){startRoomMission(false)},bottom=10)
            body.add(action(t("Remember, then do · AR challenge","याद करके करें · AR अभ्यास"),false,role=ActionRole.REVIEW){startRoomMission(true)},bottom=20)
            if(selected=="fire")body.add(action(t("Explosion-risk evacuation drill","विस्फोट खतरा निकासी अभ्यास"),false,role=ActionRole.DANGER){startActivity(Intent(this,RoomMissionActivity::class.java).putExtra("moduleId","fire").putExtra("camera",true).putExtra("explosionRisk",true))},bottom=20)
            body.add(label(t("Procedure practice","प्रक्रिया का अभ्यास"),18f,Palette.ink,true).asHeading(),bottom=10)
            body.add(action(t("Practise the full procedure","पूरी प्रक्रिया का अभ्यास करें"),false,role=ActionRole.LEARN){startActivity(Intent(this,ProcedureActivity::class.java).putExtra("moduleId",selected).putExtra("guided",true))},bottom=10)
            body.add(action(t("Independent procedure check","स्वतंत्र प्रक्रिया जाँच"),false,role=ActionRole.REVIEW){startActivity(Intent(this,ProcedureActivity::class.java).putExtra("moduleId",selected).putExtra("guided",false))},bottom=20)
        }
        body.add(label(t("Decision practice","निर्णयों का अभ्यास"),18f,Palette.ink,true).asHeading(),bottom=10)
        body.add(action(t("Guided practice","निर्देशित अभ्यास"),false,role=ActionRole.LEARN){startTraining(true,false)},bottom=10)
        body.add(action(t("Practise with camera AR","कैमरा AR में अभ्यास करें"),false,role=ActionRole.CAMERA){startTraining(true,true)},bottom=20)
        val m=curriculum.module(selected)
        body.add(action(t("What you’ll learn","आप क्या सीखेंगे"),false,role=ActionRole.NEUTRAL){
            val objectives=m.getJSONArray("objectives")
            notice(t("What you’ll learn","आप क्या सीखेंगे"),(0 until objectives.length()).joinToString("\n\n"){objectives.getJSONArray(it).getString(if(hi)1 else 0)})
        })
    }
    private fun speak(text: String,automatic:Boolean=false){
        if(!store.isCurrentProfile())return
        narrator.show(text,if(hi)"hi-IN"else"en-IN")
        if(!automatic)narrator.replay()
    }
    private fun startTraining(guided: Boolean,ar: Boolean){
        PageDialogBuilder(this).setTitle(t("Check your training area","प्रशिक्षण क्षेत्र जाँचें")).setMessage(t("Use a clear, safe area away from machinery. All hazards and readings are simulated. ${if(guided)"You will get feedback after every choice." else "No hints during assessment. A critical unsafe choice ends the attempt."}","मशीनों से दूर, खुली सुरक्षित जगह में अभ्यास करें। खतरे और रीडिंग काल्पनिक हैं। ${if(guided)"हर विकल्प के बाद प्रतिक्रिया मिलेगी।" else "मूल्यांकन में संकेत नहीं मिलेंगे। गंभीर असुरक्षित निर्णय पर प्रयास समाप्त होगा।"}"))
            .setPositiveButton(t("I’m in a safe area","मैं सुरक्षित जगह पर हूँ")){_,_->
                if(ar){val availability=ArCoreApk.getInstance().checkAvailability(this);if(availability.isUnsupported){notice(t("AR is unavailable","AR उपलब्ध नहीं"),t("This phone does not support ARCore. Use on-screen practice.","यह फ़ोन ARCore का समर्थन नहीं करता। स्क्रीन पर अभ्यास करें।"));return@setPositiveButton}}
                session=TrainingSession.create(curriculum.module(selected),store.workerId,curriculum.version,guided,if(ar)"arcore" else "screen");store.save(session!!)
                if(ar){startActivityForResult(Intent(this,ArActivity::class.java).putExtra("attemptId",session!!.data.getString("id")),20)}else go("training")
            }.setNegativeButton(t("Not now","अभी नहीं"),null).show()
    }
    private fun training(){
        val s=session?:return go("home");val m=curriculum.module(s.data.getString("moduleId"))
        if(s.finished){result(s);return}
        body.add(chip(if(s.guided)t("GUIDED PRACTICE","निर्देशित अभ्यास") else t("ASSESSMENT · NO HINTS","मूल्यांकन · कोई संकेत नहीं")),bottom=8)
        body.add(label(m.local("title",hi),15f,Palette.ink,true),bottom=4)
        body.add(label(t("Question ${s.index+1} of ${s.questions.size}","सवाल ${s.index+1} / ${s.questions.size}"),13f,Palette.muted),bottom=8)
        body.add(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=s.questions.size;progress=s.index+1;progressTintList=android.content.res.ColorStateList.valueOf(Palette.blue)},bottom=20)
        val q=s.current
        val scene=card(Palette.soft);scene.add(label(q.local("prompt",hi),18f,Palette.ink,true));body.add(scene,bottom=12)
        if(s.data.optBoolean("awaitingContinue")){
            if(s.guided){val good=s.data.optBoolean("lastCorrect");val feedback=card(if(good)Palette.successBg else Palette.amberBg);feedback.add(label(if(good)t("Safe understanding","सही समझ")else t("Let’s learn from this","इससे सीखें"),20f,if(good)Palette.success else Palette.amber,true),bottom=10);feedback.add(label(q.local("explanation",hi)));body.add(feedback,bottom=16)}else body.add(chip(t("Answer saved","उत्तर सहेजा गया")),bottom=16)
            body.add(action(t("Continue","आगे बढ़ें")){s.advance();store.save(s);render()})
        }else{
            s.options().forEach{option->body.add(action(option.local("text",hi),false,ActionRole.LEARN){s.answer(option.getString("id"));store.save(s);render()},bottom=8)}
            if(s.guided)body.add(action(t("Read the question aloud","सवाल सुनें"),false){speak(q.local("prompt",hi))},top=4)
        }
        body.add(label(t("Saved automatically on this phone.","इस फ़ोन पर अपने आप सहेजा जाता है।"),14f,Palette.muted),top=20)
    }
    private fun result(s: TrainingSession){
        val r=s.data.getJSONObject("result");val passed=r.getBoolean("passed");val m=curriculum.module(s.data.getString("moduleId"))
        title(if(s.guided)t("Practice complete","अभ्यास पूरा हुआ") else if(passed)t("Assessment passed","मूल्यांकन पास") else t("Let’s practise again","फिर से अभ्यास करें"),m.local("title",hi))
        val c=card(if(passed||s.guided)Palette.successBg else Palette.amberBg)
        c.add(label("${r.getInt("correct")} / ${r.getInt("total")}",36f,Palette.ink,true),bottom=8)
        c.add(label(t("Decisions answered correctly","सही निर्णय")),bottom=12)
        if(r.getJSONArray("criticalFailures").length()>0)c.add(label(t("A critical safety decision needs more practice.","एक महत्वपूर्ण सुरक्षा निर्णय का और अभ्यास चाहिए।"),16f,Palette.danger,true))
        body.add(c,bottom=16)
        body.add(label(t("Practical observation: not assessed. This pilot result is not a statutory safety certificate.","व्यावहारिक निरीक्षण: मूल्यांकन नहीं हुआ। यह पायलट परिणाम वैधानिक सुरक्षा प्रमाणपत्र नहीं है।"),14f,Palette.muted),bottom=16)
        body.add(action(t("Review my decisions","मेरे निर्णय देखें"),false,role=ActionRole.REVIEW){review(s)},bottom=12)
        if(!s.guided&&passed)body.add(action(t("Save completion receipt (PDF)","पूर्णता रसीद सहेजें (PDF)")){saveReceipt(s)},bottom=12)
        body.add(action(t("Practise again","फिर अभ्यास करें"),false){selected=m.getString("id");startTraining(true,false)},bottom=12)
        body.add(action(t("My learning record","मेरा सीखने का रिकॉर्ड"),false){go("records")})
    }
    private fun review(s: TrainingSession){
        val text=s.events.filter{it.optString("type")=="answer"}.joinToString("\n\n"){e->val q=s.questions.first{it.getString("id")==e.getString("questionId")};val o=q.getJSONArray("options").objects().first{it.getString("id")==e.getString("optionId")};"${if(o.optBoolean("correct"))"✓" else "!"} ${q.local("prompt",hi)}\n${o.local("text",hi)}\n${q.local("explanation",hi)}"}
        notice(t("Your decisions","आपके निर्णय"),text)
    }
    private fun records(){
        title(t("Your progress","आपकी प्रगति"),t("Learning and practical skills are recorded separately.","सीखना और व्यावहारिक कौशल अलग दर्ज होते हैं।"))
        val launch=LearningCatalog(this)
        val records=LearningProgress(this,store.workerId).use{it.records()}
        launch.courses.forEach{course->
            val ids=records.filter{it.optString("courseId")==course.getString("id")&&it.optString("contentVersion")==launch.version&&it.optBoolean("finished")}.map{it.optString("scenarioId")}.toSet()
            body.add(card().apply{add(label(course.getString("title"),18f,Palette.ink,true).asHeading(),bottom=6);add(label("${ids.size} scenario practices complete",14f,Palette.muted))},bottom=12)
        }
        body.add(action(t("Practice history & reflections","अभ्यास इतिहास और विचार")){startActivity(Intent(this,LearningScenarioActivity::class.java).putExtra("showRecords",true))},bottom=10)
        body.add(action(t("First-aid bay records","प्राथमिक उपचार अभ्यास रिकॉर्ड"),false,ActionRole.NEUTRAL){bayRecords()},bottom=10)
        body.add(action(t("Sync private learning records","निजी सीखने के रिकॉर्ड सिंक करें"),false,ActionRole.NEUTRAL){
            learningOverlay?.dismiss();learningOverlay=LearningPracticeSyncDialog.show(this,store)
        },bottom=10)
        body.add(action(t("Account & institution","खाता और संस्था"),false,ActionRole.NEUTRAL){startActivity(Intent(this,LearnerCentreActivity::class.java))},bottom=10)
        body.add(action(t("Earlier records & credentials","पुराने रिकॉर्ड और प्रमाणपत्र"),false,ActionRole.NEUTRAL){go("legacy-records")})
    }
    private fun bayRecords(){
        val records=SkillPracticeStore(this,store.workerId,"first-aid-bay.db").use{it.records()}
        if(records.isEmpty()){notice(t("First-aid bay","प्राथमिक उपचार अभ्यास"),t("No practice saved yet. Open First Aid to begin.","अभी कोई अभ्यास नहीं सहेजा गया है। शुरू करने के लिए प्राथमिक उपचार खोलें।"));return}
        val panel=column(12)
        val dialog=PageDialogBuilder(this).setTitle(t("First-aid practice records","प्राथमिक उपचार अभ्यास रिकॉर्ड")).setView(paged(panel,hi)).setNegativeButton(t("Close","बंद करें"),null).create()
        records.forEach{record->
            val date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT).format(java.util.Date(record.getLong("updatedAt")))
            panel.add(action("$date · ${record.optInt("index")}/${FirstAidBay.steps.size}",false,ActionRole.NEUTRAL){
                dialog.dismiss();startActivity(Intent(this,FirstAidBayActivity::class.java).putExtra("attemptId",record.getString("id")))
            },bottom=8)
        };dialog.show()
    }
    private fun legacyRecords(){

        title(t("Earlier industrial records","पुराने औद्योगिक रिकॉर्ड"),store.name.ifBlank{t("Historical assessments and credentials.","पुराने मूल्यांकन और प्रमाणपत्र।")})
        body.add(action(t("Objective skill passport","कौशल की प्रगति"),false,ActionRole.REVIEW){skillPassport()},bottom=12)
        body.add(action(t("Account, pending reviews & credentials","खाता, लंबित समीक्षा और प्रमाणपत्र"),false,ActionRole.NEUTRAL){startActivity(Intent(this,LearnerCentreActivity::class.java))},bottom=12)
        body.add(action(t("Procedure & transfer history","प्रक्रिया और बदले दृश्य का इतिहास"),false,ActionRole.NEUTRAL){practiceHistory()}.apply{tag="main-practice-history"},bottom=12)

        val attempts=store.attempts();if(attempts.isEmpty())body.add(card().apply{add(label(t("Your first lesson is waiting.","आपका पहला पाठ तैयार है।"),21f,Palette.ink,true),bottom=12);add(label(t("Finish a practice to see your progress here.","अपनी प्रगति यहाँ देखने के लिए अभ्यास पूरा करें।")))},bottom=20)
        attempts.forEach{a->val m=curriculum.module(a.getString("moduleId"));val c=card();c.add(label(m.local("title",hi),20f,Palette.ink,true),bottom=8)
            val finished=a.optBoolean("finished");val passed=a.optJSONObject("result")?.optBoolean("passed")==true
            val status=if(!finished)t("In progress","जारी है") else if(a.getString("kind")=="practice")t("Practised","अभ्यास किया") else if(passed)t("Simulation passed","सिमुलेशन पास") else t("More practice needed","और अभ्यास चाहिए")
            c.add(chip(status),bottom=12);c.add(label(date(a.getLong("startedAt"))+" · "+a.getString("mode"),13f,Palette.muted),bottom=12)
            c.add(action(if(finished)t("View result","परिणाम देखें")else t("Continue","जारी रखें"),false){session=TrainingSession(a,m);selected=m.getString("id");go("training")});body.add(c,bottom=14)
        }
        val rooms=RoomMissionStore(this,store.workerId).use{it.records()}
        if(rooms.isNotEmpty()){
            rooms.firstOrNull{it.getJSONObject("mission").optInt("version")==RoomMission.VERSION&&!it.getJSONObject("mission").optBoolean("completed")}?.let{record->
                body.add(action(t("Resume room practice","कमरे का अभ्यास जारी रखें"),role=ActionRole.PRIMARY){
                    startActivity(Intent(this,RoomMissionActivity::class.java).putExtra("moduleId",record.getString("module"))
                        .putExtra("camera",record.getString("mode")=="camera").putExtra("recall",record.optJSONObject("coaching")?.optString("mode")=="recall")
                        .putExtra("explosionRisk",record.getJSONObject("mission").optJSONObject("scenario")?.optBoolean("explosionRisk")==true)
                        .putExtra("resumeId",record.getString("id")))
                },top=12,bottom=10)
            }
            body.add(label(t("${rooms.size} room practice records · simulation evidence","${rooms.size} कमरे के अभ्यास रिकॉर्ड · सिमुलेशन प्रमाण"),14f,Palette.muted),top=16,bottom=8)
            body.add(action(t("Export room practice journals","कमरे के अभ्यास रिकॉर्ड भेजें"),false,role=ActionRole.LEARN){
                try{exportBytes=RoomJournalExport.create(this,store).toString(2).toByteArray();createDocument("application/json","suraksha-room-practice.json")}
                catch(_:Exception){notice(t("Could not export practice","अभ्यास निर्यात नहीं हुआ"),t("Reopen your records and try again. Large journals may need trainer assistance.","रिकॉर्ड फिर खोलकर कोशिश करें। बड़े रिकॉर्ड के लिए प्रशिक्षक सहायता लें।"))}
            },bottom=12)
        }
        if(attempts.any{it.optBoolean("finished")})body.add(action(t("Export records for trainer","प्रशिक्षक के लिए रिकॉर्ड भेजें")){exportBytes=store.export().toString(2).toByteArray();createDocument("application/json","suraksha-training-record.json")},top=8,bottom=12)
        body.add(action(t("Verify a QR record","QR रिकॉर्ड जाँचें"),false,role=ActionRole.PRIMARY){go("verify")},bottom=12)
        store.credentials().forEach{c->body.add(action(t("View signed pilot credential","हस्ताक्षरित पायलट प्रमाणपत्र देखें"),false,role=ActionRole.PRIMARY){showCredential(c)},bottom=12)}
        body.add(action(t("Edit my profile","मेरी प्रोफ़ाइल बदलें"),false,role=ActionRole.NEUTRAL){profile()})
    }
    private fun help(){
        title(t("Here to help","आपकी मदद के लिए"))
        val guide=card()
        listOf(t("1. Choose a lesson and learn the steps.","1. पाठ चुनें और चरण सीखें।"),t("2. Practise with guidance, then try an assessment.","2. निर्देशों के साथ अभ्यास करें, फिर मूल्यांकन करें।"),t("3. Your progress is saved after every answer.","3. हर उत्तर के बाद प्रगति सहेजी जाती है।"),t("4. Export records for your trainer. Completed practice does not authorise hazardous work.","4. प्रशिक्षक के लिए रिकॉर्ड भेजें। अभ्यास पूरा करना खतरनाक काम की अनुमति नहीं है।")).forEachIndexed{i,text->guide.add(label(text,14f),top=if(i==0)0 else 12)}
        body.add(guide,bottom=16)
        body.add(action(t("Learners on this phone","इस फ़ोन के शिक्षार्थी"),false,role=ActionRole.NEUTRAL){startActivity(Intent(this,WorkerProfilesActivity::class.java))},bottom=10)
        body.add(action(t("Edit my profile","मेरी प्रोफ़ाइल बदलें"),false,role=ActionRole.NEUTRAL){profile()},bottom=10)
        body.add(action(t("Questions & offline guide","प्रश्न और ऑफ़लाइन जानकारी"),false){startActivity(Intent(this,QuestionGuideActivity::class.java))},bottom=10)
        if(authorisedStaff)body.add(action(t("Manage training","प्रशिक्षण प्रबंधन"),false,ActionRole.NEUTRAL){startActivity(Intent(this,AdminActivity::class.java))},bottom=10)
        body.add(action(t("Choose language","भाषा चुनें"),false,role=ActionRole.NEUTRAL){language()},top=8,bottom=12)
        body.add(action(t("Check AR support","AR समर्थन जाँचें"),false,role=ActionRole.CAMERA){ArCoreApk.getInstance().checkAvailabilityAsync(this){a->notice(t("AR support","AR समर्थन"),a.name)}},bottom=12)
        body.add(label(t("Version ${packageManager.getPackageInfo(packageName,0).versionName} • Pilot content requires safety review. Santali lessons await native-speaker review. Audio uses installed offline Android voices.","संस्करण ${packageManager.getPackageInfo(packageName,0).versionName} • पायलट सामग्री की सुरक्षा समीक्षा ज़रूरी है। संताली पाठों की स्थानीय वक्ता समीक्षा बाकी है। आवाज़ Android की इंस्टॉल ऑफ़लाइन आवाज़ से आती है।"),14f,Palette.muted))
    }
    private fun skillPassport(){
        val catalog=JSONObject(assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
        val procedures=ProcedureStore(this,store.workerId).use{it.records()}
        val transfer=SkillPracticeStore(this,store.workerId).use{it.records()}
        val content=column(14)
        content.add(label(t("Knowledge, simulated performance and practical observation are separate. This passport is not work authorisation.","ज्ञान, काल्पनिक प्रदर्शन और व्यावहारिक निरीक्षण अलग हैं। यह प्रगति काम की अनुमति नहीं है।"),15f),bottom=12)
        catalog.getJSONArray("objectives").objects().forEach{objective->
            val steps=objective.getJSONArray("procedureSteps").strings().toSet()
            val relevant=procedures.firstOrNull{r->r.optString("module")==objective.getString("module")&&r.getJSONArray("events").objects().any{it.optString("step") in steps}}
            val actions=relevant?.let{runCatching{DecisionReplay.entries(it)}.getOrDefault(emptyList()).filter{it.step.id in steps}}
            val latestTransfer=transfer.firstOrNull{r->catalog.getJSONArray("scenarios").objects().any{it.getString("id")==r.optString("scenarioId")&&it.getString("objectiveId")==objective.getString("id")}}
            val needs=actions?.any{!it.correct}==true||latestTransfer?.let{it.optBoolean("finished")&&!it.optBoolean("passed")}==true
            val state=if(needs)t("Needs practice","अभ्यास चाहिए")else if(actions?.isNotEmpty()==true||latestTransfer!=null)t("Simulation evidence only","केवल सिमुलेशन प्रमाण")else t("Not yet demonstrated","अभी प्रदर्शित नहीं")
            content.add(label(objective.local("title",hi),18f,bold=true).asHeading(),bottom=6)
            content.add(label(state,14f,if(needs)Palette.amber else Palette.muted),bottom=6)
            content.add(label(t("Practical observation: not verified in this local passport. Content: transfer-1 draft.","व्यावहारिक निरीक्षण: स्थानीय प्रगति में सत्यापित नहीं। सामग्री: transfer-1 प्रारूप।"),13f,Palette.muted),bottom=8)
            if(relevant?.optBoolean("finished")==true)content.add(action(t("Replay original decisions","मूल निर्णय फिर देखें"),false){startActivity(Intent(this,DecisionReplayActivity::class.java).putExtra("procedureId",relevant.getString("id")))},bottom=8)
            content.add(action(t("Practise this objective","इस कौशल का अभ्यास करें"),false){startActivity(Intent(this,SkillPracticeActivity::class.java).putExtra("objectiveId",objective.getString("id")))},bottom=16)
        }
        learningOverlay=PageDialogBuilder(this).setTitle(t("Objective skill passport","कौशल की प्रगति")).setView(paged(content,hi)).setPositiveButton(t("Close","बंद करें"),null).create().also{it.show()}
    }
    private fun practiceHistory(){
        val owner=store.workerId;val content=column(14)
        val procedures=ProcedureStore(this,owner).use{it.records()};val practices=SkillPracticeStore(this,owner).use{it.records()}
        if(procedures.isEmpty()&&practices.isEmpty())content.add(label(t("No procedure or transfer records yet. Open a course to begin.","प्रक्रिया या बदले दृश्य का रिकॉर्ड अभी नहीं है। पाठ्यक्रम खोलकर शुरू करें।")))
        procedures.forEach{record->
            content.add(label(ProcedureCatalog.modules.getValue(record.getString("module")).text(hi)+" · "+if(record.optBoolean("guided"))t("guided","निर्देशित")else t("independent","स्वतंत्र"),17f,bold=true).asHeading(),bottom=6)
            content.add(label(date(record.getLong("createdAt"))+" · v${record.getInt("catalogVersion")} · "+if(record.optBoolean("finished"))t("finished simulation","सिमुलेशन समाप्त")else t("in progress","जारी है"),13f,Palette.muted),bottom=8)
            content.add(action(if(record.optBoolean("finished"))t("Replay original decisions","मूल निर्णय फिर देखें")else t("Resume saved procedure","सहेजी प्रक्रिया जारी रखें"),false){
                if(!store.isCurrentProfile()||store.workerId!=owner){learningOverlay?.dismiss();return@action}
                startActivity(if(record.optBoolean("finished"))Intent(this,DecisionReplayActivity::class.java).putExtra("procedureId",record.getString("id"))else Intent(this,ProcedureActivity::class.java).putExtra("procedureId",record.getString("id")).putExtra("moduleId",record.getString("module")).putExtra("guided",record.getBoolean("guided")))
            }.apply{tag="history-procedure-${record.getString("id")}"},bottom=16)
        }
        practices.forEach{record->content.add(label("${record.getString("scenarioId")} · "+t("formative screen practice","सीखने का स्क्रीन अभ्यास"),16f,bold=true),bottom=6);content.add(action(t("View or resume transfer record","बदले दृश्य का रिकॉर्ड देखें या जारी रखें"),false){if(store.isCurrentProfile()&&store.workerId==owner)startActivity(Intent(this,SkillPracticeActivity::class.java).putExtra("attemptId",record.getString("id")))},bottom=16)}
        learningOverlay=PageDialogBuilder(this).setTitle(t("Procedure & transfer history","प्रक्रिया और बदले दृश्य का इतिहास")).setView(paged(content,hi)).setPositiveButton(t("Close","बंद करें"),null).create().also{it.show()}
    }
    private fun verifyPage(){
        title(t("Verify a record","रिकॉर्ड जाँचें"),t("Scan a receipt or signed training credential.","रसीद या हस्ताक्षरित प्रशिक्षण प्रमाणपत्र स्कैन करें।"))
        body.add(action(t("Scan QR code","QR कोड स्कैन करें")){IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt(t("Scan training QR","प्रशिक्षण QR स्कैन करें")).setBeepEnabled(false).setOrientationLocked(false).initiateScan()},bottom=16)
        body.add(action(t("Open credential text file","प्रमाणपत्र की टेक्स्ट फ़ाइल खोलें"),false){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain"),30)},bottom=16)
        val field=EditText(this).apply{hint=t("Or paste the QR text","या QR का पाठ चिपकाएँ");inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;minLines=3;setTextColor(Palette.ink);setPadding(dp(16),dp(12),dp(16),dp(12));background=shape(Color.WHITE,12,Palette.line)}
        body.add(field,bottom=12);body.add(action(t("Check record","रिकॉर्ड जाँचें"),false){checkRecord(field.text.toString())})
    }
    private fun checkRecord(raw: String){
        val value=raw.trim()
        try {
            if(value.startsWith("SURAKSHA:CREDENTIAL:") || Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matches(value)){val credential=CredentialVerifier.verify(this,value);store.saveCredential(credential);showCredential(credential)
            } else if(value.startsWith("SURAKSHA:RECEIPT:")){
                val p=JSONObject(String(android.util.Base64.decode(value.removePrefix("SURAKSHA:RECEIPT:"),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)))
                notice(t("Unverified completion receipt","असत्यापित पूर्णता रसीद"),t("This is a device-generated receipt, not an issuer-signed certificate. Module: ${p.getString("moduleId")}\nPractical observation: not assessed.","यह फ़ोन से बनी रसीद है, जारीकर्ता का हस्ताक्षरित प्रमाणपत्र नहीं। पाठ: ${p.getString("moduleId")}\nव्यावहारिक निरीक्षण नहीं हुआ।"))
            } else notice(t("Cannot verify this record","यह रिकॉर्ड सत्यापित नहीं हो सका"),t("No trusted issuer is configured for this QR. Ask your trainer to verify it in the dashboard. Never treat an unknown QR as a valid certificate.","इस QR के लिए विश्वसनीय जारीकर्ता नहीं है। प्रशिक्षक से डैशबोर्ड में जाँच करवाएँ। अज्ञात QR को वैध प्रमाणपत्र न मानें।"))
        }catch(_:Exception){notice(t("Invalid QR","अमान्य QR"),t("This is not a readable Suraksha training record.","यह पढ़ने योग्य सुरक्षा प्रशिक्षण रिकॉर्ड नहीं है।"))}
    }
    private fun showCredential(saved:JSONObject){
        val c=try { CredentialVerifier.verify(this,saved.getString("token")) } catch(_:Exception) { notice(t("Cannot verify credential","प्रमाणपत्र सत्यापित नहीं हो सका"),t("The saved credential is not valid under the installed issuer trust.","इंस्टॉल जारीकर्ता भरोसे के अनुसार सहेजा प्रमाणपत्र मान्य नहीं है।"));return }
        val box=column(20);box.add(label(t("Signature verified offline","हस्ताक्षर ऑफ़लाइन सत्यापित"),20f,Palette.success,true),bottom=12)
        box.add(label(curriculum.module(c.getString("moduleId")).local("title",hi)),bottom=8)
        saved.optString("onlineStatus").takeIf{it.isNotBlank()}?.let{box.add(label(t("Last server status: ","अंतिम सर्वर स्थिति: ")+it+" · "+date(saved.optLong("statusCheckedAt")),14f,Palette.muted),bottom=12)}
        val expiry=c.optString("expiryStatus")
        val validity=when {
            c.optBoolean("issuedInFuture") -> t("Device date is earlier than issue date. Check the clock before relying on validity.","फ़ोन की तारीख जारी होने से पहले है। वैधता पर भरोसा करने से पहले घड़ी जाँचें।")
            expiry=="expired" -> t("Training validity expired. Ask your trainer about renewal.","प्रशिक्षण की वैधता समाप्त है। नवीनीकरण के लिए प्रशिक्षक से पूछें।")
            expiry=="within-validity" -> t("Recorded validity ends: ","दर्ज वैधता समाप्ति: ")+date(c.getLong("expiresAt"))
            else -> t("No validity end date was recorded. Ask your trainer about current requirements.","वैधता समाप्ति की तारीख दर्ज नहीं है। मौजूदा ज़रूरतों के लिए प्रशिक्षक से पूछें।")
        }
        box.add(label(validity,16f,if(expiry=="expired" || c.optBoolean("issuedInFuture"))Palette.amber else Palette.muted),bottom=12)
        box.add(label(t("Pilot simulation • ${c.getInt("score")}%\nPractical observation: not assessed.\nRevocation status: unknown offline. Ask your trainer for current status.","पायलट सिमुलेशन • ${c.getInt("score")}%\nव्यावहारिक निरीक्षण नहीं हुआ।\nऑफ़लाइन निरस्तीकरण की स्थिति अज्ञात है। वर्तमान स्थिति प्रशिक्षक से पूछें।"),14f,Palette.muted),bottom=12)
        val matrix=MultiFormatWriter().encode("SURAKSHA:CREDENTIAL:"+c.getString("token"),BarcodeFormat.QR_CODE,500,500)
        val bitmap=Bitmap.createBitmap(500,500,Bitmap.Config.ARGB_8888);for(x in 0 until 500)for(y in 0 until 500)bitmap.setPixel(x,y,if(matrix[x,y])Color.BLACK else Color.WHITE)
        box.add(ImageView(this).apply{setImageBitmap(bitmap);adjustViewBounds=true;contentDescription="Signed pilot credential QR"})
        PageDialogBuilder(this).setTitle(t("Pilot credential","पायलट प्रमाणपत्र")).setView(paged(box,hi)).setPositiveButton(t("Done","ठीक है"),null).show()
    }
    private fun createDocument(mime:String,name:String){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,name),10)}
    @Deprecated("Compatibility") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(!store.isCurrentProfile()) { exportBytes=null;return }
        val scan=IntentIntegrator.parseActivityResult(requestCode,resultCode,data);if(scan!=null){scan.contents?.let{checkRecord(it)};return}
        if(requestCode==30&&resultCode==RESULT_OK){try{data?.data?.let{uri->val text=contentResolver.openInputStream(uri)?.use{run{val output=java.io.ByteArrayOutputStream();val buf=ByteArray(1024);var n=it.read(buf);while(n!=-1){require(output.size()+n<=6000);output.write(buf,0,n);n=it.read(buf)};output.toString("UTF-8")}}?:error("No file");checkRecord(text)}}catch(_:Exception){notice("Cannot read credential","Choose a valid credential text file.")};return}
        if(requestCode==10) {
            try { if(resultCode==RESULT_OK) {
                val bytes=exportBytes ?: error("No pending export")
                val uri=data?.data ?: error("No destination")
                val output=contentResolver.openOutputStream(uri) ?: error("Destination unavailable")
                output.use { it.write(bytes) }
                Toast.makeText(this,t("Saved","सहेजा गया"),Toast.LENGTH_SHORT).show()
            } } catch(_:Exception) { notice(t("Could not save","सहेजा नहीं जा सका"),t("Return to the learner's record and export again.","शिक्षार्थी के रिकॉर्ड में लौटकर फिर निर्यात करें।")) }
            finally { exportBytes=null }
            return
        }
        if(requestCode==20){session?.data?.getString("id")?.let{store.attempt(it)?.let{a->session=TrainingSession(a,curriculum.module(a.getString("moduleId")))}};go("training")}
    }
    private fun saveReceipt(s:TrainingSession){
        val payload=JSONObject().put("attemptId",s.data.getString("id")).put("moduleId",s.data.getString("moduleId")).put("status","unverified-receipt")
        val qr="SURAKSHA:RECEIPT:"+android.util.Base64.encodeToString(payload.toString().toByteArray(),android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        val matrix=MultiFormatWriter().encode(qr,BarcodeFormat.QR_CODE,250,250)
        val bitmap=Bitmap.createBitmap(250,250,Bitmap.Config.ARGB_8888);for(x in 0 until 250)for(y in 0 until 250)bitmap.setPixel(x,y,if(matrix[x,y])Color.BLACK else Color.WHITE)
        val pdf=PdfDocument();val p=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,1).create());val c=p.canvas;val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(Color.WHITE);paint.color=Palette.blue;c.drawRect(0f,0f,595f,14f,paint)
        fun line(text:String,y:Float,size:Float=16f){paint.color=Palette.ink;paint.textSize=size;c.drawText(text,44f,y,paint)}
        line("SURAKSHAAR",72f,24f);line("PILOT COMPLETION RECEIPT",110f,18f)
        line(store.name.ifBlank{"Learner"}.take(48),165f,22f);line(curriculum.module(s.data.getString("moduleId")).local("title",false),205f)
        line("Simulation score: ${s.data.getJSONObject("result").getInt("score")}%",245f)
        line("Practical observation: NOT ASSESSED",280f);line("Issuer validation: PENDING",310f)
        c.drawBitmap(bitmap,44f,350f,paint);line(date(System.currentTimeMillis()),650f)
        line("Not a statutory safety certificate or work authorisation.",690f,14f)
        line("Attempt: ${s.data.getString("id")}",725f,11f)
        pdf.finishPage(p);val output=java.io.ByteArrayOutputStream();pdf.writeTo(output);pdf.close();exportBytes=output.toByteArray();createDocument("application/pdf","suraksha-completion-receipt.pdf")
    }
    private fun date(time:Long)=SimpleDateFormat("dd MMM yyyy, HH:mm",if(hi)Locale.forLanguageTag("hi-IN")else Locale.ENGLISH).format(Date(time))
}
