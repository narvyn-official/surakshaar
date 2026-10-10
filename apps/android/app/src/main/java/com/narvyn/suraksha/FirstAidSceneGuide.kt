package com.narvyn.suraksha

/** Presentation only. Never changes the signed/pinned scenario definition or its saved answers. */
internal object FirstAidSceneGuide {
    const val VERSION = 1
    data class Scene(val key:String,val props:Set<String>,val caption:String,val captionHi:String,
        val motion:Set<String> = emptySet()) {
        fun caption(hi:Boolean)=if(hi)captionHi else caption
        val animated get()=motion.isNotEmpty()
    }
    private fun scene(key:String,props:String,en:String,hi:String,motion:String="")=Scene(key,
        props.split(' ').filter{it.isNotBlank()}.toSet(),en,hi,motion.split(' ').filter{it.isNotBlank()}.toSet())
    private val entries=linkedMapOf<String,Scene>()
    private fun add(scenario:String,step:String,props:String,en:String,hi:String,motion:String="") {
        val key="$scenario/$step";check(entries.put(key,scene(key,props,en,hi,motion))==null)
    }
    init {
        val collapse="adult-collapse-response"
        add(collapse,"safe_space","area","Clear the practice space around the adult.","व्यक्ति के आसपास अभ्यास की जगह खाली करें।")
        add(collapse,"response","response call","No response. Ask for emergency help and an AED.","कोई प्रतिक्रिया नहीं। आपातकालीन मदद और AED मँगाएँ।","response")
        add(collapse,"breathing","gasp call","Irregular gasps are not normal breathing. Illustrated cue.","अनियमित हाँफना सामान्य साँस नहीं है। यह चित्रित संकेत है।","gasp")
        add(collapse,"begin_cpr","cpr call","CPR illustration: hands at the centre of the chest.","CPR का चित्रण: हाथ छाती के बीच में रखें।","cpr")
        add(collapse,"aed_power","call aed-prompt","Tap the AED's green power button.","AED का हरा पावर बटन छुएँ।")
        add(collapse,"aed_diagram","aed-diagram","Read the pad diagrams on the actual AED; the packet is an illustration.","वास्तविक AED पर पैड के चित्र पढ़ें; यहाँ पैकेट का चित्रण है।")
        add(collapse,"clear","clear","Keep hands away while the AED analyses.","AED के विश्लेषण के दौरान व्यक्ति को न छुएँ।")
        add(collapse,"resume_cpr","cpr","The illustrated AED says resume CPR.","चित्रित AED का निर्देश है: CPR फिर शुरू करें।","cpr")
        add(collapse,"handover","handover","Tell responders what happened and what you did.","मदद करने वालों को घटना और किए गए उपचार बताएँ।")

        val recovery="unresponsive-normal-breathing"
        add(recovery,"safe_space","area","Check the area before approaching.","पास जाने से पहले जगह सुरक्षित जाँचें।")
        add(recovery,"response","response call","No response. Get emergency help.","कोई प्रतिक्रिया नहीं। आपातकालीन मदद बुलाएँ।","response")
        add(recovery,"breathing","breath call","Regular breathing is illustrated; keep checking the person.","नियमित साँस का चित्रण है; व्यक्ति की निगरानी करते रहें।","breath")
        add(recovery,"position","recovery-guide","Pose guide: bent knee, hand at cheek, open airway. No trauma in this case.","मुद्रा का चित्र: घुटना मुड़ा, हाथ गाल पर, वायुमार्ग खुला। इस स्थिति में चोट नहीं है।")
        add(recovery,"monitor","recovery-guide breath","Keep the airway open and watch breathing.","वायुमार्ग खुला रखें और साँस देखते रहें।","breath")
        add(recovery,"change","gasp call","The case changes to abnormal gasps: reassess and begin CPR.","स्थिति बदली: असामान्य हाँफना। फिर जाँचें और CPR शुरू करें।","gasp")
        add(recovery,"handover","handover call","Report the change in breathing and the care given.","साँस में बदलाव और दिए गए उपचार बताएँ।")

        val bleed="bleeding-injury"
        add(bleed,"safe_space","area","Check for hazards before giving help.","मदद देने से पहले खतरों को जाँचें।")
        add(bleed,"observe","cut","Inspect the marked forearm wound. No object in this first case.","चिह्नित अग्रबाहु का घाव देखें। इस पहली स्थिति में कोई वस्तु नहीं फँसी है।")
        add(bleed,"protect","cut glove gauze call","Use a barrier and a clean dressing; get help for severe bleeding.","सुरक्षा दस्ताने और साफ ड्रेसिंग लें; अधिक रक्तस्राव पर मदद बुलाएँ।")
        add(bleed,"pressure","cut dressing pressure","Press the clean dressing on the wound. The screen hold measures neither treatment time nor pressure.","घाव की साफ ड्रेसिंग पर सीधा दबाव दें। स्क्रीन दबाने से न उपचार का समय, न वास्तविक दबाव मापा जाता है।")
        add(bleed,"embedded","embedded side-dressings","New case: leave the object in place. Press on either side, not on it.","नई स्थिति: फँसी वस्तु वहीं रहने दें। उसके दोनों ओर दबाएँ, उसके ऊपर नहीं।")
        add(bleed,"handover","handover embedded side-dressings","Keep the object protected and tell responders what happened.","फँसी वस्तु को सुरक्षित रखें और मदद करने वालों को घटना बताएँ।")

        val burn="thermal-burn-response"
        add(burn,"safe_space","area source-off","The hot-water source is removed. Check your practice space.","गर्म पानी का स्रोत हट चुका है। अभ्यास की जगह जाँचें।")
        add(burn,"observe","scald source-off","Inspect the scald on the left forearm.","बाईं अग्रबाहु पर गर्म पानी से झुलसी जगह देखें।")
        add(burn,"cool","scald tap tray water sleeve","Cool running water for 20 minutes. This loop is a demonstration, not a timer.","20 मिनट ठंडे बहते पानी से ठंडा करें। यह चित्रण है, उपचार का टाइमर नहीं।","water")
        add(burn,"clothing","scald sleeve sleeve-edge","The folded sleeve is stuck to the burn. Leave it attached.","कपड़े की तह जली जगह से चिपकी है। इसे न खींचें।")
        add(burn,"cover-help","scald sleeve film call","After cooling, loosely cover this large burn and seek urgent care. Do not wrap the arm.","ठंडा करके इस बड़े जले भाग पर ढीला आवरण रखें और तुरंत चिकित्सा मदद लें। बाँह पर न लपेटें।")
        add(burn,"handover","scald sleeve film handover","Report the burn, cooling and attached clothing.","जलने, ठंडा करने और चिपके कपड़े की जानकारी दें।")

        val limb="suspected-limb-injury"
        add(limb,"safe_space","area","Check the area after the fall; do not move the injured person unnecessarily.","गिरने के बाद जगह जाँचें; घायल को बिना जरूरत न हिलाएँ।")
        add(limb,"observe","limb","Inspect the lower-leg injury marker without testing movement.","पैर को हिलाकर जाँचे बिना चोट का संकेत देखें।")
        add(limb,"support","limb supports","Pad around the leg in its found position; do not straighten it.","पैर जिस स्थिति में है, उसी में आसपास सहारा दें; सीधा न करें।")
        add(limb,"red-flags","limb supports urgent call","Reported numbness and coldness need urgent help.","सुन्नपन और ठंडापन बताया गया है; तुरंत मदद चाहिए।")
        add(limb,"comfort","limb supports blanket","Reassure the person and keep watching their condition.","व्यक्ति को भरोसा दें और स्थिति देखते रहें।")
        add(limb,"handover","limb supports handover","Describe the fall, pain and changes in sensation.","गिरने, दर्द और संवेदना में बदलाव बताएँ।")

        val seizure="seizure-response"
        add(seizure,"safe_space","area hazards","Look for nearby objects that could hurt the person.","आसपास की चोट पहुँचा सकने वाली वस्तुएँ देखें।")
        add(seizure,"observe","hazards seizure-cue","The case reports a seizure. Observe and note when it began.","इस स्थिति में दौरा बताया गया है। देखें और शुरू होने का समय नोट करें।")
        add(seizure,"call","hazards call","This is the first known seizure: call emergency help.","यह पहला ज्ञात दौरा है: आपातकालीन मदद बुलाएँ।")
        add(seizure,"protect","hazards-clear cushion clock","Clear nearby hazards and cushion the head. Do not restrain.","आसपास का खतरा हटाएँ और सिर के नीचे नरम सहारा दें। रोककर न पकड़ें।")
        add(seizure,"mouth","cushion no-mouth clock","Keep objects and fingers out of the mouth.","मुँह में कोई वस्तु या उँगली न डालें।")
        add(seizure,"after","cushion recovery-guide breath","Jerking has stopped and breathing is normal: use the recovery position.","झटके रुक गए और साँस सामान्य है: करवट की सुरक्षित मुद्रा दें।","breath")
        add(seizure,"handover","recovery-guide handover","Report what you observed and the seizure's duration.","देखे गए संकेत और दौरे की अवधि बताएँ।")

        val faint="fainting-response-bay"
        add(faint,"safe_space","area","The responsive adult is already lying down. Check for hazards.","प्रतिक्रिया दे रहा व्यक्ति पहले से लेटा है। जगह सुरक्षित जाँचें।")
        add(faint,"observe","response","Check response and the reported dizziness.","प्रतिक्रिया और बताए गए चक्कर को जाँचें।","response")
        add(faint,"support","legs-guide","Position illustration: lying down with legs gently raised if possible.","मुद्रा का चित्र: संभव हो तो लेटाकर पैर हल्के ऊपर रखें।")
        add(faint,"recover","sit-guide","When recovered, help them sit up gradually. Do not rush them to stand.","ठीक महसूस करने पर धीरे बैठने में मदद करें। जल्दी खड़ा न करें।")
        add(faint,"change","gasp call cpr","New case: unresponsive with abnormal gasps. Get help and start CPR.","नई स्थिति: प्रतिक्रिया नहीं और असामान्य हाँफना। मदद बुलाएँ और CPR शुरू करें।","gasp")
        add(faint,"handover","handover call","Report the collapse, response and breathing changes.","बेहोशी, प्रतिक्रिया और साँस में बदलाव बताएँ।")

        val heat="heat-illness-response"
        add(heat,"safe_space","area shade","The adult has been moved into a cool, shaded place.","व्यक्ति को ठंडी छायादार जगह लाया गया है।")
        add(heat,"observe","shade heat","The case reports heat, sweating and dizziness; the person answers clearly.","गर्मी, पसीना और चक्कर बताए गए हैं; व्यक्ति साफ जवाब दे रहा है।")
        add(heat,"cool","fan cloth bottle","Cool the skin and fan. Offer water only while alert and able to swallow.","त्वचा ठंडी करें और हवा दें। होश में और निगल सकने पर ही पानी दें।","fan")
        add(heat,"confusion","fan cloth urgent call","Confusion is an emergency. Call now and keep cooling.","भ्रम आपात स्थिति है। तुरंत मदद बुलाएँ और ठंडा करते रहें।","fan")
        add(heat,"swallow","fan cloth no-drink response","Now unresponsive: stop drinks and reassess breathing immediately.","अब प्रतिक्रिया नहीं: पीने को न दें और तुरंत साँस फिर जाँचें।","fan response")
        add(heat,"handover","fan cloth handover","Report heat exposure, cooling and the change in response.","गर्मी के संपर्क, ठंडा करने और प्रतिक्रिया के बदलाव बताएँ।","fan")

        // Keep historical twelve-stage records usable without changing their original questions.
        for(step in FirstAidBay.steps) {
            val source=entries["$collapse/${step.id}"]
            val props=if(step.id=="analysis")"cpr"else source?.props?.joinToString(" ")?:when(step.id){"chest"->"chest-landmark";"buddy"->"call";else->"response"}
            val name=when(step.id){"response"->"brain";"breathing"->"trachea";"chest"->"sternum";else->"named structure"}
            val caption=when{step.partId!=null->"Learning pause: select the $name. "+step.cue;step.id=="analysis"->"Authored AED prompt: resume CPR. No rhythm has been measured.";else->source?.caption?:step.cue}
            val captionHi=if(step.partId!=null)caption else source?.captionHi?:step.cueHi
            add(FirstAidBay.SCENARIO_ID,step.id,props,caption,captionHi,if(step.id=="analysis")"cpr"else source?.motion?.joinToString(" ")?:"")
        }
    }
    /** Direct targets are additional input paths for the existing action; they never define new answers. */
    fun actionForPart(scene:Scene,partId:String):String? {
        val id=partId.removePrefix("teaching:")
        return when(scene.key){
            "thermal-burn-response/cool"->if(id=="tap-control")"cool-running-water"else null
            "thermal-burn-response/clothing"->if(id=="sleeve"||id=="sleeve-edge")"leave-stuck"else null
            "thermal-burn-response/cover-help"->if(id=="film"||id=="film-open-edges")"cover-seek-care"else null
            "bleeding-injury/embedded"->if(id=="side-dressings"||id=="side-pressure")"pressure-around"else null
            "suspected-limb-injury/support"->if(id=="supports")"support-position"else null
            "seizure-response/protect"->if(id=="cushion")"clear-cushion-time"else null
            "heat-illness-response/cool"->if(id=="fan")"cool-and-water"else null
            else->null
        }
    }
    val all:List<Scene> get()=entries.values.toList()
    fun forStep(scenarioId:String,stepId:String):Scene?=entries["$scenarioId/$stepId"]
    fun byKey(key:String?):Scene?=key?.let(entries::get)
}
