package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView

/** All touch setup happens before inserting the phone. The lesson itself owns stereo controls. */
class HeadsetActivity:Activity(){
    private lateinit var store:Store
    private lateinit var catalog:LearningCatalog
    private val back by lazy{AppBackNavigation(this){goBack()}}
    private var course="fire"
    private var selected:String?=null
    private var offset=0
    private var readyPage=false
    private var clearSpace=false
    private var dwell=false
    private var busy=false
    private lateinit var readiness:TextView
    private lateinit var start:Button
    private val hi get()=store.hi
    private val school get()=getSharedPreferences("learning-profile",MODE_PRIVATE).getString("context-${store.workerId}","")=="School"
    private val compact get()=resources.configuration.screenWidthDp>=600&&resources.configuration.screenHeightDp<480
    private val preview get()=BuildConfig.DEBUG&&Build.HARDWARE in setOf("ranchu","goldfish")&&intent.getBooleanExtra("preview",false)
    private fun t(en:String,hindi:String)=if(hi)hindi else en
    private fun lessons()=HeadsetLessonPolicy.lessons(catalog.scenarios,course,school)
    private fun lesson()=catalog.scenarios.firstOrNull{it.optString("id")==selected&&HeadsetLessonPolicy.allowed(it,school)}
    override fun onCreate(state:Bundle?){
        super.onCreate(state);store=Store(this);catalog=LearningCatalog(this)
        course=state?.getString("course")?.takeIf{it in HeadsetLessonPolicy.courses}?:"fire"
        selected=state?.getString("selected")?:intent.getStringExtra("scenarioId")
        lesson()?.let{course=it.getString("course")}
        offset=state?.getInt("offset")?:0
        readyPage=state?.getBoolean("readyPage")?:(lesson()!=null)
        clearSpace=state?.getBoolean("clearSpace")?:false
        dwell=state?.getBoolean("dwell")?:getSharedPreferences("headset-controls",MODE_PRIVATE).getBoolean("dwell",false)
        runCatching{CardboardBridge.initialise(this)}
        back.enabled(true);render()
    }
    override fun onResume(){
        super.onResume()
        if(!::store.isInitialized)return
        if(!store.isCurrentProfile()){finish();return}
        busy=false
        if(readyPage){
            if(lesson()==null){readyPage=false;selected=null;render()}else refreshReadiness()
        }
    }
    override fun onSaveInstanceState(out:Bundle){
        super.onSaveInstanceState(out);out.putString("course",course);out.putString("selected",selected)
        out.putInt("offset",offset);out.putBoolean("readyPage",readyPage);out.putBoolean("clearSpace",clearSpace);out.putBoolean("dwell",dwell)
    }
    override fun onDestroy(){back.close();if(::store.isInitialized)store.close();super.onDestroy()}
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){goBack()}
    private fun goBack(){if(readyPage){readyPage=false;clearSpace=false;render()}else finish()}
    private fun render(){
        if(!store.isCurrentProfile()){finish();return}
        val root=column().apply{setBackgroundColor(Palette.canvas);tag="headset-setup"}
        root.setOnApplyWindowInsetsListener{v,i->
            if(Build.VERSION.SDK_INT>=30){val b=i.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom)}
            else v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom)
            i
        }
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(4),dp(16),dp(4));setBackgroundColor(Palette.surface)}
        top.addView(practiceTool("",PracticeIcon.BACK,t("Back","वापस")){goBack()}.apply{tag="headset-back"},LinearLayout.LayoutParams(dp(48),dp(48)))
        top.addView(label(t("Headset VR","हेडसेट VR"),18f,Palette.ink,true).asHeading(),LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(8)})
        top.addView(TextView(this).apply{setCompoundDrawables(headsetIcon(Palette.teal,26),null,null,null);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(32),dp(32)))
        root.add(top)
        if(readyPage&&lesson()!=null)renderReady(root)else renderLessons(root)
        setContentView(root);root.requestApplyInsets()
        // Android's first-use immersive confirmation must appear while touch setup is accessible.
        if(Build.VERSION.SDK_INT>=30){
            window.insetsController?.systemBarsBehavior=android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(readyPage)window.insetsController?.hide(WindowInsets.Type.systemBars())
            else window.insetsController?.show(WindowInsets.Type.systemBars())
        }else{
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility=if(readyPage)View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY else View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    private fun renderLessons(root:LinearLayout){
        readyPage=false
        val content=column(if(compact)12 else 18)
        if(!compact){
            content.add(label(t("Choose a VR lesson.","VR पाठ चुनें।"),25f,Palette.ink,true).asHeading(),bottom=6)
            content.add(label(t("Look at an action, then press your headset button.","क्रिया की ओर देखें, फिर हेडसेट का बटन दबाएँ।"),14f,Palette.muted),bottom=16)
        }
        val choiceSide=if(compact)column()else content
        val listSide=if(compact)column()else content
        if(compact)choiceSide.add(label(t("Choose a lesson.","पाठ चुनें।"),20f,Palette.ink,true).asHeading(),bottom=10)
        val tabs=LinearLayout(this).apply{isBaselineAligned=false;orientation=if(compact)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL}
        listOf("fire" to t("Fire Safety","अग्नि सुरक्षा"),"first-aid" to t("First Aid","प्राथमिक उपचार")).forEachIndexed{index,(id,title)->
            tabs.addView(action(title,false,ActionRole.NEUTRAL){course=id;offset=0;render()}.apply{
                tag="headset-course-$id";isSelected=course==id
                background=headsetSurface(if(isSelected)Palette.tealBg else Palette.surface,if(isSelected)Palette.teal else Palette.line,14)
                setTextColor(if(isSelected)Palette.teal else Palette.ink);contentDescription=title+if(isSelected)t(", selected",", चयनित")else""
            },if(compact)LinearLayout.LayoutParams(-1,-2).apply{if(index==0)bottomMargin=dp(8)}else LinearLayout.LayoutParams(0,-2,1f).apply{if(index==0)marginEnd=dp(8)})
        }
        choiceSide.add(tabs,bottom=12)
        choiceSide.add(action(t("Live AR with hands","हाथों से लाइव AR"),false,ActionRole.CAMERA){
            if(store.isCurrentProfile())startActivity(Intent(this,HandsArActivity::class.java).putExtra("headset",true))
        }.apply{tag="headset-live-ar";textSize=13f},bottom=10)
        val available=lessons()
        val pageSize=if(compact)2 else 4
        offset=(offset.coerceAtLeast(0)/pageSize*pageSize).coerceAtMost(((available.size-1).coerceAtLeast(0)/pageSize)*pageSize)
        val list=column().apply{tag="headset-lessons"}
        if(available.isEmpty())list.add(label(t("No VR lessons are available for this learning profile.","इस सीखने की प्रोफ़ाइल के लिए VR पाठ उपलब्ध नहीं हैं।"),15f,Palette.muted))
        available.drop(offset).take(pageSize).forEach{scenario->
            val lessonTitle=scenario.optString("title")
            val steps=scenario.getJSONArray("steps").length()
            list.add(action(lessonTitle+"\n"+t("$steps steps · English lesson","$steps चरण · अंग्रेज़ी पाठ"),false,ActionRole.NEUTRAL){
                if(store.isCurrentProfile()&&HeadsetLessonPolicy.allowed(scenario,school)){
                    selected=scenario.getString("id");clearSpace=false;readyPage=true;render()
                }
            }.apply{
                tag="headset-lesson-${scenario.getString("id")}";gravity=Gravity.START or Gravity.CENTER_VERTICAL
                minHeight=dp(if(compact)60 else 68);minimumHeight=minHeight;textSize=14f;setPadding(dp(14),dp(10),dp(14),dp(10))
                background=headsetSurface(Palette.surface,Palette.line,15)
                setTextColor(Palette.ink);contentDescription=lessonTitle+". "+t("$steps steps. Open headset setup.","$steps चरण। हेडसेट सेटअप खोलें।")
                val begin=text.indexOf('\n')+1;text=android.text.SpannableString(text).apply{setSpan(android.text.style.RelativeSizeSpan(.82f),begin,length,0);setSpan(android.text.style.ForegroundColorSpan(Palette.muted),begin,length,0)}
            },bottom=8)
        }
        listSide.add(list,bottom=4)
        if(available.size>pageSize){
            val navigation=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
            navigation.addView(action(t("Previous","पिछले"),false,ActionRole.NEUTRAL){offset-=pageSize;render()}.apply{tag="headset-lessons-previous";isEnabled=offset>0;textSize=13f},LinearLayout.LayoutParams(0,-2,1f))
            navigation.addView(label("${offset/pageSize+1} / ${(available.size+pageSize-1)/pageSize}",13f,Palette.muted).apply{gravity=Gravity.CENTER;tag="headset-lessons-page";contentDescription=t("Lesson page ${offset/pageSize+1}","पाठ पृष्ठ ${offset/pageSize+1}")},LinearLayout.LayoutParams(0,-2,.7f))
            navigation.addView(action(t("Next","अगले"),false,ActionRole.NEUTRAL){offset+=pageSize;render()}.apply{tag="headset-lessons-next";isEnabled=offset+pageSize<available.size;textSize=13f},LinearLayout.LayoutParams(0,-2,1f))
            listSide.add(navigation,bottom=10)
        }
        choiceSide.add(label(t(if(school)"School awareness lessons · learning records stay separate from practical certification."else"VR decision practice · progress stays on this phone.",if(school)"स्कूल जागरूकता पाठ · सीखने के रिकॉर्ड और व्यावहारिक प्रमाणन अलग हैं।"else"VR निर्णय अभ्यास · प्रगति इसी फ़ोन पर सहेजी जाती है।"),12f,Palette.muted),top=4)
        if(compact){
            val columns=LinearLayout(this).apply{isBaselineAligned=false;gravity=Gravity.TOP}
            columns.addView(choiceSide,LinearLayout.LayoutParams(0,-2,.85f).apply{marginEnd=dp(16)})
            columns.addView(listSide,LinearLayout.LayoutParams(0,-2,1.15f));content.add(columns)
        }
        root.addView(paged(content,hi),LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun renderReady(root:LinearLayout){
        val selectedLesson=lesson()?:return
        val content=column(if(compact)12 else 18)
        val summary=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false}
        summary.addView(label(selectedLesson.getString("title"),if(compact)18f else 21f,Palette.ink,true).asHeading().apply{tag="headset-selected-title"},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(8)})
        summary.addView(action(t("Change","बदलें"),false,ActionRole.NEUTRAL){readyPage=false;clearSpace=false;render()}.apply{tag="headset-change-lesson";textSize=13f},LinearLayout.LayoutParams(-2,-2))
        val left=if(compact)column()else content
        val right=if(compact)column()else content
        left.add(summary,bottom=if(compact)8 else 12)
        val viewer=column(12).apply{background=softSurface(Palette.surface,16)}
        viewer.add(label(t("VIEWER","व्यूअर"),11f,Palette.teal,true),bottom=4)
        readiness=label("",13f,Palette.muted).apply{tag="headset-readiness";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
        viewer.add(readiness,bottom=8)
        val calibration=LinearLayout(this).apply{isBaselineAligned=false}
        calibration.addView(action(t("Scan viewer QR","व्यूअर QR स्कैन करें"),false,ActionRole.CAMERA){scanViewer()}.apply{tag="headset-scan-viewer";textSize=13f;isEnabled=CardboardBridge.nativeAvailable},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(8)})
        calibration.addView(action(t("Cardboard v1","Cardboard v1"),false,ActionRole.NEUTRAL){
            PageDialogBuilder(this).setTitle(t("Google Cardboard v1 only","केवल Google Cardboard v1"))
                .setMessage(t("Use these lens settings only for an original Cardboard v1 viewer. For a different headset, scan its manufacturer’s QR code.","ये लेंस सेटिंग केवल मूल Cardboard v1 के लिए हैं। दूसरे हेडसेट के लिए निर्माता का QR कोड स्कैन करें।"))
                .setPositiveButton(t("Use Cardboard v1","Cardboard v1 चुनें")){_,_->runCatching{CardboardBridge.useDefaultViewer()};refreshReadiness()}
                .setNegativeButton(t("Cancel","रद्द करें"),null).show()
        }.apply{tag="headset-default-viewer";textSize=13f;isEnabled=CardboardBridge.nativeAvailable},LinearLayout.LayoutParams(0,-2,1f))
        viewer.add(calibration);left.add(viewer,bottom=if(compact)0 else 12)
        val controls=column(12).apply{background=shape(Palette.tealBg,16)}
        controls.add(label(t("LOOK, THEN PRESS","देखें, फिर दबाएँ"),11f,Palette.teal,true),bottom=4)
        controls.add(label(t(if(compact)"Look at an action. Press to select; hold to recenter."else"Aim the centre dot at an action. Press the headset button to select. Hold the button to recenter.",if(compact)"क्रिया देखें। चुनने के लिए बटन दबाएँ; दृश्य बीच में लाने के लिए दबाए रखें।"else"बीच के बिंदु को क्रिया पर रखें। चुनने के लिए हेडसेट बटन दबाएँ। दृश्य बीच में लाने के लिए बटन दबाए रखें।"),if(compact)13f else 14f,Palette.ink),bottom=4)
        controls.add(CheckBox(this).apply{
            tag="headset-dwell";text=t("Use gaze dwell instead","नज़र टिकाकर चुनें");textSize=14f;setTextColor(Palette.ink);buttonTintList=ColorStateList.valueOf(Palette.teal)
            minHeight=dp(48);isChecked=dwell;setOnCheckedChangeListener{_,checked->dwell=checked;getSharedPreferences("headset-controls",MODE_PRIVATE).edit().putBoolean("dwell",checked).apply()}
        })
        right.add(controls,bottom=if(compact)4 else 10)
        right.add(CheckBox(this).apply{
            tag="headset-clear-space";text=t("I’m seated in a clear area","मैं खाली जगह में बैठा/बैठी हूँ");textSize=14f;setTextColor(Palette.ink);buttonTintList=ColorStateList.valueOf(Palette.teal)
            minHeight=dp(48);isChecked=clearSpace;setOnCheckedChangeListener{_,checked->clearSpace=checked;refreshReadiness()}
        },bottom=4)
        right.add(label(t(if(compact)"Stay seated. Remove the viewer if uncomfortable."else"Keep still; look around without walking. Remove the viewer if you feel uncomfortable. Set up accounts before inserting the phone.",if(compact)"बैठे रहें। असहज लगे तो व्यूअर उतारें।"else"चलें नहीं; बैठकर आसपास देखें। असहज महसूस हो तो व्यूअर उतार दें। फ़ोन लगाने से पहले खाता सेट करें।"),12f,Palette.muted),bottom=if(compact)4 else 8)
        if(hi)right.add(label("पाठ और आवाज़ अभी अंग्रेज़ी में हैं।",12f,Palette.amber),bottom=4)
        if(preview)right.add(label("EMULATOR PREVIEW · no head tracking",11f,Palette.amber,true).apply{tag="headset-preview-label"},bottom=4)
        if(compact){
            val columns=LinearLayout(this).apply{isBaselineAligned=false;gravity=Gravity.TOP}
            columns.addView(left,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(12)})
            columns.addView(right,LinearLayout.LayoutParams(0,-2,1f));content.add(columns)
        }
        root.addView(paged(content,hi),LinearLayout.LayoutParams(-1,0,1f))
        val footer=column(12).apply{background=shape(Palette.surface,0)}
        start=action(t("Start in English","अंग्रेज़ी में शुरू करें")){launch()}.apply{tag="headset-start";contentDescription=t("Start English headset lesson","अंग्रेज़ी हेडसेट पाठ शुरू करें")}
        footer.add(start);root.add(footer);refreshReadiness()
    }
    private fun sensorsReady():Boolean{
        val sensors=getSystemService(SensorManager::class.java)
        return sensors?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!=null&&sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null
    }
    private fun refreshReadiness(){
        if(!readyPage||!::readiness.isInitialized)return
        val native=CardboardBridge.nativeAvailable
        val tracking=sensorsReady()||preview
        val viewer=native&&runCatching{CardboardBridge.hasViewerProfile()}.getOrDefault(false)
        val allowed=lesson()!=null&&store.isCurrentProfile()
        readiness.text=when{
            !native->t("VR engine unavailable in this build. Use handheld learning.","इस बिल्ड में VR इंजन उपलब्ध नहीं है। फ़ोन पर सीखें।")
            !tracking->t("This phone needs a gyroscope and accelerometer for headset tracking. Handheld 3D remains available.","हेडसेट ट्रैकिंग के लिए फ़ोन में जाइरोस्कोप और एक्सेलेरोमीटर चाहिए। फ़ोन पर 3D उपलब्ध है।")
            !viewer->t("Scan the QR code on your viewer to match its lenses.","लेंस मिलाने के लिए अपने व्यूअर पर लगा QR कोड स्कैन करें।")
            else->t("Viewer profile loaded · "+if(preview)"emulator preview"else"motion sensors available","व्यूअर प्रोफ़ाइल तैयार · "+if(preview)"एमुलेटर पूर्वावलोकन"else"गति सेंसर उपलब्ध हैं")
        }
        if(::start.isInitialized)start.isEnabled=!busy&&HeadsetLessonPolicy.ready(native,tracking,viewer,clearSpace,allowed)
    }
    private fun scanViewer(){
        if(!store.isCurrentProfile()||!CardboardBridge.nativeAvailable)return
        runCatching{CardboardBridge.scanViewer(this)}.onFailure{
            notice(t("Viewer scan unavailable","व्यूअर स्कैन उपलब्ध नहीं"),t("The QR scanner could not open. Check camera access and retry.","QR स्कैनर नहीं खुल सका। कैमरा अनुमति जाँचें और दोबारा कोशिश करें।"))
        }
    }
    private fun launch(){
        if(busy||!store.isCurrentProfile())return
        val scenario=lesson()?:return
        val viewer=CardboardBridge.nativeAvailable&&runCatching{CardboardBridge.hasViewerProfile()}.getOrDefault(false)
        if(!HeadsetLessonPolicy.ready(CardboardBridge.nativeAvailable,sensorsReady()||preview,viewer,clearSpace,HeadsetLessonPolicy.allowed(scenario,school))){refreshReadiness();return}
        busy=true;refreshReadiness()
        try{
            startActivity(Intent(this,VrTrainingActivity::class.java).putExtra("scenarioId",scenario.getString("id")).putExtra("dwell",dwell).putExtra("englishAccepted",true).putExtra("preview",preview))
        }catch(_:Exception){
            busy=false;refreshReadiness()
            notice(t("Could not start VR","VR शुरू नहीं हुआ"),t("Return to the lesson picker and retry. Your saved learning is unchanged.","पाठ सूची पर लौटकर दोबारा कोशिश करें। आपका सहेजा सीखने का रिकॉर्ड सुरक्षित है।"))
        }
    }
}
