package com.narvyn.suraksha

/** Presentation of existing authored Fire decisions; not a second curriculum or safety engine. */
object FireScenePlan {
    val supportedPhases=setOf("ALARM","EXIT","EQUIPMENT","LABEL_CHECK","CONDITION_CHECK","HOSE_CHECK","DAMAGED_UNIT","PIN","AIM","SWEEP","WITHDRAW","EVACUATE","ROUTE_RECHECK","ASSEMBLY","REPORT","FIRE_HANDOVER","COMPLETE")
    val contextProps=linkedMapOf("solids" to "fire-paper-fuel","liquid" to "fire-diesel","electrical" to "fire-electrical","gas" to "fire-gas-cylinder","metal" to "fire-metal-swarf","cooking" to "fire-fryer","sand" to "fire-sand-bucket")
    val propKinds=setOf("fire-exit","fire-blocked-exit","fire-alarm-stand","fire-paper-fuel","fire-obstruction","fire-smoke-cue","fire-handover","fire-rollcall","fire-unconfirmed")+contextProps.values
    private val assemblyPhases=setOf("ASSEMBLY","REPORT","FIRE_HANDOVER","COMPLETE")
    private val retreatPhases=setOf("WITHDRAW","EVACUATE","ROUTE_RECHECK")+assemblyPhases
    data class Plan(val phase:String,val showEquipment:Boolean,val equipmentFlags:Set<String>,val showAlarm:Boolean,
                    val showUnconfirmedUnit:Boolean,val smoke:Boolean,val exitKind:String,val blockedHazardRoute:Boolean,
                    val showRollCall:Boolean,val showHandover:Boolean,val focusY:Float) {
        val signature get()=listOf(phase,showEquipment,equipmentFlags.sorted(),showAlarm,showUnconfirmedUnit,smoke,exitKind,blockedHazardRoute,showRollCall,showHandover,focusY).joinToString("|")
    }
    fun forPhase(phase:String,condition:String="good",routeChanged:Boolean=false,explosionRisk:Boolean=false,held:Boolean=false):Plan {
        val damaged=condition=="damaged"
        val retreat=phase in retreatPhases
        val flags=buildSet {
            if(damaged)add("fire-damaged")
            // A damaged unit never acquires a removed pin, aimed nozzle, or discharge.
            if(!damaged && !explosionRisk && phase in setOf("AIM","SWEEP","WITHDRAW"))add("fire-pin")
            if(!damaged && !explosionRisk && phase in setOf("SWEEP","WITHDRAW"))add("fire-aim")
            if(!damaged && !explosionRisk && held && phase=="SWEEP")add("fire-squeeze")
            if(retreat || explosionRisk)add("fire-withdraw")
        }
        return Plan(phase,!(routeChanged && retreat),flags,phase=="ALARM",phase=="EQUIPMENT",
            explosionRisk || retreat,
            if(phase in assemblyPhases)"assembly" else if(routeChanged)"blocked-exit" else "exit",
            phase in setOf("EXIT","EQUIPMENT","EVACUATE"),phase in assemblyPhases,
            phase in setOf("REPORT","FIRE_HANDOVER","COMPLETE"),
            when(phase){"CONDITION_CHECK"->.455f;"HOSE_CHECK","DAMAGED_UNIT"->.34f;else->.26f})
    }
    data class Inspection(val id:String,val title:String,val titleHi:String,val detail:String,val detailHi:String,val phase:String,val condition:String="good") {
        fun title(hi:Boolean)=if(hi)titleHi else title
        fun detail(hi:Boolean)=if(hi)detailHi else detail
        val plan get()=forPhase(phase,condition)
    }
    // These are inspection specimens, not extra completed lessons or real equipment readings.
    val inspections=listOf(
        Inspection("ready","Intact training unit","सही प्रशिक्षण उपकरण","Inspect the label, safety parts and intact hose. This specimen represents the authored paper/wood lesson; colour alone does not establish suitability.","लेबल, सुरक्षा भाग और सही होज़ देखें। यह नमूना काल्पनिक कागज़/लकड़ी पाठ का है; केवल रंग से उपयुक्तता तय नहीं होती।","LABEL_CHECK"),
        Inspection("gauge","Gauge inspection","गेज की जाँच","This authored needle is in the indicated range. Zoom only enlarges the illustration; the phone does not measure an extinguisher's pressure.","काल्पनिक सुई चिह्नित सीमा में है। ज़ूम केवल चित्र बड़ा करता है; फ़ोन अग्निशामक का दबाव नहीं मापता।","CONDITION_CHECK"),
        Inspection("damaged","Low gauge & split hose","कम गेज और फटी होज़","Compare the displaced needle and open hose split. The lesson requires rejecting this unit, evacuating and reporting the defect; do not repair or test-discharge it during a fire.","सुई की स्थिति और होज़ का खुला कट देखें। पाठ में यह उपकरण छोड़ना, निकलना और खराबी बताना है; आग के समय मरम्मत या जाँच के लिए डिस्चार्ज न करें।","DAMAGED_UNIT","damaged"),
        Inspection("pin","Pin removed","पिन निकला हुआ","The retaining pin is shown outside the valve. This is one authored component state, not permission or a complete operating procedure.","सुरक्षा पिन वाल्व से बाहर दिखाया गया है। यह केवल एक काल्पनिक भाग की स्थिति है, अनुमति या पूरी संचालन विधि नहीं।","AIM"),
        Inspection("aim","Hose directed","होज़ की दिशा","Inspect the changed hose and nozzle position. Use the guided lesson for the decision sequence. Model spacing is not an operating distance.","होज़ और नोज़ल की बदली स्थिति देखें। निर्णय के क्रम के लिए निर्देशित पाठ करें। मॉडल की दूरी संचालन दूरी नहीं है।","SWEEP")
    )
    fun inspection(id:String)=inspections.firstOrNull{it.id==id}?:inspections.first()
    data class ViewPreset(val yaw:Float=0f,val pitch:Float=0f,val focusY:Float=.26f)
    /** Show defining top surfaces at selection, rather than requiring discovery by rotation. */
    fun viewForCase(id:String?)=when(id){
        "cooking"->ViewPreset(-22f,24f,.20f)
        "metal"->ViewPreset(-18f,36f,.10f)
        "sand"->ViewPreset(-18f,28f,.20f)
        "liquid"->ViewPreset(-25f,22f,.20f)
        "solids"->ViewPreset(-20f,25f,.12f)
        "electrical","gas"->ViewPreset(-18f,8f,.26f)
        else->ViewPreset()
    }
}
