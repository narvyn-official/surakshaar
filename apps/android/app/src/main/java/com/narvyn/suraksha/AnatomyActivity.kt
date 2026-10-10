package com.narvyn.suraksha

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import java.util.concurrent.Executors

/** Native full-viewport exploration. All gesture operations have labelled button alternatives. */
class AnatomyActivity:Activity(){
    private var scene:AnatomyScene?=null
    private var data:AnatomyData?=null
    private var resumed=false
    private var camera=false
    private var restored:AnatomyControls?=null
    private lateinit var narrator:TrainingNarrator
    private var selected:AnatomyPart?=null
    private var observationMode="3d"
    private val observation get()=intent.getBooleanExtra("observation",false)
    private var confirm:Button?=null
    private val loader=Executors.newSingleThreadExecutor()
    private val back by lazy{AppBackNavigation(this){backOneStep()}}
    private lateinit var status:TextView
    private lateinit var name:TextView
    private lateinit var summary:TextView
    private lateinit var detailButton:Button
    private lateinit var focusButton:Button
    private val hi get()=getSharedPreferences("preferences",MODE_PRIVATE).getBoolean("hi",false)
    private fun t(en:String,hiText:String)=if(hi)hiText else en
    private fun knowledge(part:AnatomyPart)=AnatomyKnowledge.info(part.id,if(hi)"hi" else "en")
    private fun partName(part:AnatomyPart)=knowledge(part)?.title?:part.name.replaceFirstChar{it.uppercase()}
    private lateinit var place:Button
    private lateinit var placement:LinearLayout
    private lateinit var inspector:LinearLayout
    private lateinit var dockHolder:LinearLayout
    private var dock:AnatomyViewDock?=null
    override fun onCreate(state:Bundle?){super.onCreate(state);camera=state?.getBoolean("camera")?:false;back.enabled(true);narrator=TrainingNarrator(this){hi}
        state?.getFloatArray("view")?.takeIf{it.size==6}?.let{v->restored=AnatomyControls(layer=state.getString("layer")?:"organs",selected=state.getString("selected"),yaw=v[0],pitch=v[1],distance=v[2],panX=v[3],panY=v[4],scale=v[5],skinTone=state.getInt("skinTone",AnatomyMaterials.DEFAULT_SKIN_TONE),skinOverlay=state.getBoolean("skinOverlay"),hidden=state.getStringArrayList("hidden")?.toSet().orEmpty(),isolated=state.getBoolean("isolated"))}
        val loading=column(20).apply{setBackgroundColor(Palette.canvas);add(label("Explore the body",22f,Palette.ink,true));add(label("Checking and loading the offline anatomy pack…",15f,Palette.muted),top=16);add(action("Back",false){finish()},top=16)}
        setContentView(loading);trainingInsets(loading)
        // Reuse the already-verified immutable geometry across a configuration change. Holding
        // two detailed packs while the old Activity is retiring can exhaust a mid-range heap.
        data=lastNonConfigurationInstance as? AnatomyData
        if(data!=null)return
        loader.execute{runCatching{AnatomyData.load(this)}.fold({model->runOnUiThread{if(!isDestroyed&&!isFinishing){data=model;if(resumed)showLoadedModel()}}},{runOnUiThread{if(!isDestroyed&&!isFinishing){loading.add(label("The body pack could not be verified. Reinstall the current APK to restore it. Learning records are unchanged.",15f,Palette.danger),top=12)}}})}
    }
    private fun showLoadedModel(){
        val model=data?:return
        build()
        intent.getStringExtra("partId")?.takeIf{restored==null}?.let{id->if(observation){model.parts.firstOrNull{it.id==id}?.let{p->scene?.layer(p.layer);scene?.focus(p)}}else scene?.select(id,true)}
        model.manifest.optString("qualityFallback").takeIf{it.isNotBlank()}?.let{notice("Using balanced models",it)}
    }
    private fun build(){
        if(isFinishing||isDestroyed)return
        val previous=scene?.controls?:restored;dock=null;scene?.close();val model=data?:return
        val root=column().apply{setBackgroundColor(Palette.canvas)}
        val header=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        header.addView(action("‹",false,ActionRole.NEUTRAL){backOneStep()}.apply{contentDescription="Back to learning"},LinearLayout.LayoutParams(dp(48),dp(48)))
        header.addView(label("Explore the body",19f,Palette.ink,true).asHeading().apply{setPadding(dp(10),0,0,0)},LinearLayout.LayoutParams(0,-2,1f))
        header.addView(action("Info",false,ActionRole.NEUTRAL){information()});root.add(header,bottom=8)
        if(observation){val target=model.parts.firstOrNull{it.id==intent.getStringExtra("partId")};root.add(label("Find: "+(target?.name?:"the named structure"),16f,Palette.blue,true),bottom=6)}
        status=label("Loading scene…",12f,Palette.muted);root.add(status,bottom=6)
        val next=AnatomyScene(this,model,camera);scene=next;previous?.let{p->next.change{p.copy(edit=false)}}
        next.onStatus={text->status.text=text;place.isEnabled=next.canPlace;placement.visibility=if(camera&&!next.placed)android.view.View.VISIBLE else android.view.View.GONE}
        next.onSelected={part->observationMode="3d";showSelection(part);name.announceForAccessibility(name.text)}
        next.onPicked={_,mode->observationMode=mode}
        val sideControls=resources.configuration.screenHeightDp<480&&resources.configuration.screenWidthDp>=600
        val workspace=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        workspace.addView(next,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
        placement=LinearLayout(this).apply{visibility=if(camera)android.view.View.VISIBLE else android.view.View.GONE}
        place=action("Place body"){next.place()}.apply{isEnabled=false}
        placement.addView(place,LinearLayout.LayoutParams(0,-2,1f))
        val size=action("${"%.0f".format(next.controls.scale*100)}%",false,ActionRole.NEUTRAL){
            PageDialogBuilder(this).setTitle("Body size").setItems(arrayOf("Tabletop · 40% of source size","Source size · 100% (1.72 m)")){_,i->next.change{it.copy(scale=if(i==0).4f else 1f)}}.show()
        }.apply{contentDescription="Change body size"}
        placement.addView(size,LinearLayout.LayoutParams(dp(88),-2).apply{marginStart=dp(6)})
        next.onControls={c->
            size.text="${"%.0f".format(c.scale*100)}%";dock?.sync()
            if(::summary.isInitialized) syncSelection(c)
        }
        root.add(placement,top=6)
        inspector=column(10).apply{background=shape(Palette.surface,12,Palette.line)}
        val selectionRow=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        name=label(t("Tap a body part","शरीर का अंग चुनें"),16f,Palette.ink,true).apply{tag="anatomy-part-name";maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END}
        selectionRow.addView(name,LinearLayout.LayoutParams(0,-2,1f))
        focusButton=practiceTool("",PracticeIcon.TARGET,t("Focus selected part","चुना अंग पास से देखें")){selected?.let{scene?.focus(it)}}.apply{isEnabled=false;tag="anatomy-focus"}
        selectionRow.addView(focusButton,LinearLayout.LayoutParams(dp(48),dp(48)))
        detailButton=practiceTool("",PracticeIcon.INFO,t("About selected part","चुने अंग की जानकारी")){details()}.apply{isEnabled=false;tag="anatomy-details"}
        selectionRow.addView(detailButton,LinearLayout.LayoutParams(dp(48),dp(48)))
        selectionRow.addView(practiceTool("",PracticeIcon.AUDIO,t("Audio controls","आवाज़ नियंत्रण")){narrator.showControls()},LinearLayout.LayoutParams(dp(48),dp(48)))
        inspector.add(selectionRow)
        summary=label(t("Select on the model, or find a part in Parts.","मॉडल पर चुनें या अंगों की सूची में खोजें।"),14f,Palette.muted).apply{tag="anatomy-part-description";maxLines=if(sideControls)2 else 3;ellipsize=android.text.TextUtils.TruncateAt.END}
        inspector.add(summary,top=2)
        val sidePanel=if(sideControls)column().also{workspace.addView(it,LinearLayout.LayoutParams(dp(320),-1).apply{marginStart=dp(8)})}else null
        if(sidePanel!=null)sidePanel.add(inspector) else root.add(inspector,top=8,bottom=6)
        dockHolder=column().apply{visibility=android.view.View.GONE}
        if(sidePanel!=null)sidePanel.addView(dockHolder,LinearLayout.LayoutParams(-1,-1))else root.add(dockHolder,bottom=6)
        val tools=LinearLayout(this)
        fun tool(text:String,action:()->Unit){tools.addView(this.action(text,false,ActionRole.NEUTRAL,action).apply{setPadding(dp(4),dp(8),dp(4),dp(8));textSize=13f},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(4)})}
        tool("Layers"){closeControls();layers()};tool("Parts"){closeControls();parts()};tool("View"){viewControls()};tool(if(camera)"3D" else "AR"){switchMode(!camera)}
        root.add(tools)
        if(observation){confirm=action("Use this structure"){
            val part=selected?:return@action
            if(part.id!=intent.getStringExtra("partId"))return@action
            setResult(RESULT_OK,android.content.Intent().putExtra("partId",part.id).putExtra("mode",observationMode));finish()
        }.apply{isEnabled=false};root.add(confirm!!,top=6)}
        setContentView(root);trainingInsets(root);syncSelection(next.controls);if(resumed)next.resume()
    }
    private fun switchMode(ar:Boolean){
        if(ar&&checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CAMERA),701);return}
        camera=ar;build()
    }
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,results:IntArray){super.onRequestPermissionsResult(code,permissions,results);if(code==701&&results.firstOrNull()==PackageManager.PERMISSION_GRANTED)switchMode(true)}
    private fun layers(){val layers=listOf("skin","skeleton","muscles","organs");PageDialogBuilder(this).setTitle("Body layers").setItems(arrayOf("External skin","Skeleton","Selected muscles","Organs & airway")){_,i->scene?.layer(layers[i])}.show()}
    private fun parts(){
        val model=data?:return;val panel=column(12);val search=EditText(this).apply{hint="Find a structure";setSingleLine();minimumHeight=dp(48)}
        val list=ListView(this).apply{contentDescription=t("Body parts","शरीर के अंग")}
        val empty=label(t("No matching body part. Try a shorter name.","यह अंग नहीं मिला। छोटा नाम खोजें।"),14f,Palette.muted).apply{visibility=android.view.View.GONE}
        var matches=model.parts.sortedBy{it.name}
        fun populate(){val q=search.text.toString().trim();matches=model.parts.filter{partName(it).contains(q,true)||it.name.contains(q,true)||it.id.equals(q,true)}.sortedBy{it.name};list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,matches.map{partName(it)});empty.visibility=if(matches.isEmpty())android.view.View.VISIBLE else android.view.View.GONE}
        search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int)=Unit;override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int)=populate();override fun afterTextChanged(s:Editable?)=Unit})
        panel.add(search);panel.add(empty,top=6);panel.addView(list,LinearLayout.LayoutParams(-1,dp(minOf(320,(resources.configuration.screenHeightDp*.45f).toInt()))));populate()
        val dialog=android.app.AlertDialog.Builder(this).setTitle("Structures").setView(panel).setNegativeButton("Close",null).create()
        list.setOnItemClickListener{_,_,position,_->scene?.select(matches[position].id,true);dialog.dismiss()};dialog.show();dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    private fun syncSelection(controls:AnatomyControls){
        val part=data?.parts?.firstOrNull{it.id==controls.selected && it.id !in controls.hidden && (controls.isolated||it.layer==controls.layer)}
        showSelection(part)
    }
    private fun showSelection(part:AnatomyPart?){
        val changed=selected?.id!=part?.id
        selected=part
        confirm?.isEnabled=part!=null&&part.id==intent.getStringExtra("partId")
        detailButton.isEnabled=part!=null;focusButton.isEnabled=part!=null&&!camera
        if(part==null){
            name.text=t("Tap a body part","शरीर का अंग चुनें")
            summary.text=t("Select on the model, or find a part in Parts.","मॉडल पर चुनें या अंगों की सूची में खोजें।")
            narrator.clear();return
        }
        val info=knowledge(part)
        name.text=partName(part)
        summary.text=info?.description?:t("A description is not available for this structure yet.","इस संरचना का विवरण अभी उपलब्ध नहीं है।")
        if(changed)narrator.show(info?.introText?:name.text.toString()+". "+summary.text,if(info?.language=="hi"||info==null&&hi)"hi-IN" else "en-IN")
    }
    private fun details(){
        val part=selected?:return
        val info=knowledge(part)
        val panel=column(12)
        if(info==null)panel.add(label(t("A description is not available for this structure yet.","इस संरचना का विवरण अभी उपलब्ध नहीं है।"),16f))
        else {
            panel.add(label(info.description,16f),bottom=12)
            panel.add(label(t("Where it is","कहाँ है"),13f,Palette.blue,true),bottom=3)
            panel.add(label(info.location,15f),bottom=10)
            panel.add(label(t("What it does","क्या करता है"),13f,Palette.blue,true),bottom=3)
            panel.add(label(info.function,15f),bottom=10)
            info.learningNote?.takeIf{it.isNotBlank()}?.let{panel.add(label(it,13f,Palette.muted),bottom=10)}
            panel.add(label(t("Left and right refer to the person shown.","दायाँ और बायाँ मॉडल में दिखाए व्यक्ति के अनुसार हैं।"),12f,Palette.muted),bottom=10)
        }
        lateinit var dialog:android.app.AlertDialog
        val tools=LinearLayout(this)
        fun tool(text:String,action:()->Unit){tools.addView(this.action(text,false,ActionRole.NEUTRAL,action).apply{textSize=12f;setPadding(dp(4),dp(6),dp(4),dp(6))},LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(4)})}
        tool(if(scene?.controls?.isolated==true)t("Whole layer","पूरी परत") else t("Show alone","अकेला दिखाएँ")){scene?.change{it.copy(isolated=!it.isolated)};dialog.dismiss()}
        tool(t("Hide","छिपाएँ")){scene?.change{it.copy(hidden=it.hidden+part.id,selected=null,isolated=false)};dialog.dismiss()}
        if(info!=null)tool(t("Sources","स्रोत")){dialog.dismiss();sources(part)}
        panel.add(tools)
        dialog=android.app.AlertDialog.Builder(this).setTitle(partName(part)).setView(paged(panel,hi)).setNegativeButton(t("Close","बंद करें"),null).create()
        dialog.setOnDismissListener{detailButton.requestFocus()}
        dialog.show()
    }
    private fun sources(part:AnatomyPart){
        val info=knowledge(part)?:return
        val panel=column(12)
        panel.add(label(t("Anatomy reference material. The model and translations still need qualified educational review.","शरीर रचना के संदर्भ स्रोत। मॉडल और अनुवाद की योग्य विशेषज्ञ से शैक्षिक समीक्षा अभी बाकी है।"),14f,Palette.muted),bottom=12)
        info.sourceUrls.forEachIndexed{index,url->panel.add(action(t("Reference ${index+1}","संदर्भ ${index+1}"),false){runCatching{startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}.onFailure{notice(t("Reference link","संदर्भ लिंक"),url)}},bottom=6)}
        android.app.AlertDialog.Builder(this).setTitle(t("Sources · ","स्रोत · ")+partName(part)).setView(paged(panel,hi)).setNegativeButton(t("Close","बंद करें"),null).show()
    }
    private fun viewControls(){
        if(dock!=null){closeControls();return}
        val view=scene?:return
        inspector.visibility=android.view.View.GONE
        dock=AnatomyViewDock(this,view,{closeControls()},{build()})
        dockHolder.removeAllViews();dockHolder.addView(dock);dockHolder.visibility=android.view.View.VISIBLE
    }
    private fun closeControls(){
        dock=null
        if(::dockHolder.isInitialized){dockHolder.removeAllViews();dockHolder.visibility=android.view.View.GONE}
        if(::inspector.isInitialized)inspector.visibility=android.view.View.VISIBLE
    }
    private fun backOneStep(){if(dock!=null)closeControls()else finish()}
    @Suppress("DEPRECATION")
    @android.annotation.SuppressLint("GestureBackNavigation") // API 33+ uses the registered AppBackNavigation callback; this override serves Android 10–12.
    @Deprecated("Android 10–12 compatibility") override fun onBackPressed(){backOneStep()}
    private fun information(){
        val model=data?:return
        val additional=model.manifest.optJSONArray("additionalSources")?.objects().orEmpty().joinToString("\n\n") { source ->
            listOf(source.optString("name"),source.optString("attribution"),source.optString("licence"),source.optString("licenceUrl")).filter{it.isNotBlank()}.joinToString("\n")
        }
        notice("Model coverage","${model.parts.size} named structures / groups · ${"%.2f".format(model.height)} m source height · ${model.manifest.optString("loadedQuality","Balanced")} detail\n\nDrag to rotate, pinch to zoom, two fingers to pan. Parts and View provide button alternatives. AR uses the physical camera; edit mode changes the body only. Skin appearance is illustrative and never used to judge a condition.\n\n${model.manifest.getString("attribution")}\n${model.manifest.getString("licenceUrl")}\n\n$additional\n\n"+model.manifest.getJSONArray("limitations").strings().joinToString("\n\n"))
    }
    override fun onResume(){super.onResume();resumed=true;if(scene==null&&data!=null)showLoadedModel()else scene?.resume()}
    override fun onPause(){resumed=false;scene?.pause();narrator.pause();super.onPause()}
    override fun onWindowFocusChanged(focus:Boolean){super.onWindowFocusChanged(focus);if(::narrator.isInitialized){if(focus)narrator.automatic()else narrator.pause()}}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putBoolean("camera",camera);scene?.controls?.let{c->out.putString("layer",c.layer);out.putString("selected",c.selected);out.putFloatArray("view",floatArrayOf(c.yaw,c.pitch,c.distance,c.panX,c.panY,c.scale));out.putInt("skinTone",c.skinTone);out.putBoolean("skinOverlay",c.skinOverlay);out.putBoolean("isolated",c.isolated);out.putStringArrayList("hidden",ArrayList(c.hidden))}}
    @Deprecated("Activity retains immutable geometry across configuration changes")
    override fun onRetainNonConfigurationInstance():Any?=data
    override fun onDestroy(){
        loader.shutdownNow()
        scene?.let{retired->retired.close();(retired.parent as? android.view.ViewGroup)?.removeView(retired)}
        scene=null;data=null;selected=null;dock=null
        if(::place.isInitialized)place.setOnClickListener(null)
        if(::placement.isInitialized)placement.removeAllViews()
        if(::dockHolder.isInitialized)dockHolder.removeAllViews()
        // Android may retain a destroyed Activity briefly. Its retired scene need not retain meshes.
        findViewById<android.view.ViewGroup>(android.R.id.content)?.removeAllViews()
        narrator.close();back.close();super.onDestroy()
    }
}
