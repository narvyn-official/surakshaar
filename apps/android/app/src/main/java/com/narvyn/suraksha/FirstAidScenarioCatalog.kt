package com.narvyn.suraksha

import org.json.JSONArray
import java.security.MessageDigest

/** Authored adult simulations, not a diagnostic system or a demonstration of every first-aid skill. */
object FirstAidScenarioCatalog {
    const val DEFAULT_ID = "adult-collapse-response"
    enum class Pose { SUPINE, RECOVERY_SIDE }
    enum class BodyRegion { NONE, CHEST, RIGHT_FOREARM, LEFT_FOREARM, RIGHT_LOWER_LEG, HEAD, WHOLE_BODY }
    enum class Breathing { NOT_ASSESSED, NORMAL, ABNORMAL }
    enum class Response { RESPONSIVE, UNRESPONSIVE, RECOVERING }
    data class VisualCue(val pose:Pose,val bodyRegion:BodyRegion,val breathing:Breathing,val response:Response,val label:String,val labelHi:String){
        fun label(hindi:Boolean)=if(hindi)labelHi else label
    }
    data class Scenario(val id:String,val title:String,val titleHi:String,val summary:String,val summaryHi:String,val steps:List<FirstAidBay.Step>){
        val version=1
        val reviewStatus="draft"
        val casualtyAge="adult"
        val checkedOn="2026-10-10"
        val localeStatus=mapOf("en" to "authored-draft","hi" to "authored-draft-unreviewed","sat" to "not-authored")
        val sources get()=steps.map{it.source}.distinct()
        fun title(hindi:Boolean)=if(hindi)titleHi else title
        fun summary(hindi:Boolean)=if(hindi)summaryHi else summary
        /** Persist the exact content identity. Later edits cannot silently reinterpret old events. */
        val fingerprint:String by lazy {
            val canonical=JSONArray(listOf(id,version,title,titleHi,summary,summaryHi,JSONArray(steps.map { s ->
                JSONArray(listOf(s.id,s.title,s.titleHi,s.cue,s.cueHi,s.source,s.partId,s.instructionalPause,
                    s.visual?.let { v -> JSONArray(listOf(v.pose.name,v.bodyRegion.name,v.breathing.name,v.response.name,v.label,v.labelHi)) },
                    JSONArray(s.actions.map { a -> JSONArray(listOf(a.id,a.label,a.labelHi,a.correct,a.feedback,a.feedbackHi,a.spatial)) })))
            }))).toString()
            MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
        }
    }
    private const val BLS="https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/adult-basic-life-support-guidelines"
    private const val FIRST_AID="https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/first-aid-guidelines"
    private const val RECOVERY="https://www.sja.org.uk/first-aid-advice/recovery-position/"
    private const val BLEED="https://www.sja.org.uk/first-aid-advice/severe-bleeding/"
    private const val BURN="https://www.nhs.uk/conditions/burns-and-scalds/"
    private const val LIMB_RED_FLAGS="https://www.nhs.uk/conditions/sprains-and-strains/"
    private const val FRACTURE="https://www.sja.org.uk/first-aid-advice/fractures-and-broken-bones/"
    private const val SEIZURE="https://www.sja.org.uk/first-aid-advice/seizure/"
    private const val FAINT="https://www.sja.org.uk/first-aid-advice/fainting/"
    private const val HEAT="https://www.nhs.uk/conditions/heat-exhaustion-heatstroke/"
    val LEGACY_VISUAL=VisualCue(Pose.SUPINE,BodyRegion.NONE,Breathing.NOT_ASSESSED,Response.UNRESPONSIVE,
        "Earlier adult-collapse model lesson; cue is authored, not measured.","पुराना वयस्क के गिरने का मॉडल अभ्यास; संकेत पहले से लिखा गया है, मापा नहीं गया।")
    private fun gaspingCue(previous:VisualCue) = previous.copy(response=Response.UNRESPONSIVE,breathing=Breathing.ABNORMAL,
        label="Authored report: unresponsive with abnormal gasps; breathing is not animated or measured.",
        labelHi="दिया गया विवरण: प्रतिक्रिया नहीं और असामान्य हाँफना; साँस की गति दिखाई या मापी नहीं जाती।")
    private fun visual(region:BodyRegion,breathing:Breathing,response:Response,en:String,hi:String,pose:Pose=Pose.SUPINE)=VisualCue(pose,region,breathing,response,en,hi)
    private fun action(id:String,en:String,hi:String,correct:Boolean,why:String,whyHi:String,spatial:Boolean=false)=
        FirstAidBay.Action(id,en,correct,why,spatial,hi,whyHi)
    private fun step(id:String,en:String,hi:String,cue:String,cueHi:String,source:String,visual:VisualCue,vararg actions:FirstAidBay.Action)=
        FirstAidBay.Step(id,en,cue,null,actions.toList(),source,false,hi,cueHi,visual)
    private fun setup(v:VisualCue)=step("safe_space","Prepare the practice area","अभ्यास की जगह जाँचें",
        "Keep the real space clear. This virtual adult is not a real patient. No actual call or treatment is performed.",
        "असली जगह खाली रखें। यह आभासी वयस्क है, असली मरीज नहीं। कोई वास्तविक कॉल या उपचार नहीं होगा।",FIRST_AID,v,
        action("ready-space","My practice area is clear","अभ्यास की जगह खाली है",true,"Keep real surroundings in view; pause whenever necessary.","असली आसपास की जगह देखते रहें; ज़रूरत हो तो अभ्यास रोकें।"),
        action("ignore-space","Start without looking around","आसपास देखे बिना शुरू करें",false,"Watching a phone must not create another injury.","फ़ोन देखते समय किसी नई चोट का जोखिम न लें।"))
    private fun handover(v:VisualCue)=step("handover","Give a factual handover","देखी हुई बातों की जानकारी दें",
        "A simulated responder arrives. Share observations, changes, actions and times that are known.",
        "आभासी सहायता पहुँच गई है। जो देखा, जो बदलाव हुए, किए गए काम और ज्ञात समय बताएँ।",FIRST_AID,v,
        action("handover-observations","Report observations and actions","देखी हुई बातें और किए गए काम बताएँ",true,"The record shows simulation choices, not a medical diagnosis or practical qualification.","रिकॉर्ड में अभ्यास के निर्णय हैं; यह बीमारी की पहचान या व्यावहारिक योग्यता का प्रमाण नहीं।"),
        action("diagnose-cause","State a definite diagnosis from the model","मॉडल से पक्का निदान बताएँ",false,"An authored model cannot diagnose a real person.","पहले से बनाए गए मॉडल से असली व्यक्ति की बीमारी का निदान नहीं हो सकता।"))
    private fun adult_collapse_response():Scenario {
        val initial=visual(BodyRegion.CHEST,Breathing.NOT_ASSESSED,Response.UNRESPONSIVE,"Adult lying still; response and breathing cues are authored.","वयस्क लेटा है; प्रतिक्रिया और साँस के संकेत पहले से लिखे गए हैं।")
        val gasping=initial.copy(breathing=Breathing.ABNORMAL,label="Authored report: irregular gasps, not normal breathing. Breathing is not animated.",labelHi="दिया गया विवरण: अनियमित हाँफना, सामान्य साँस नहीं। साँस की गति नहीं दिखाई जाती।")
        return Scenario("adult-collapse-response","Adult collapse: recognise and respond","वयस्क का गिरना: पहचानें और मदद करें","An unresponsive adult, abnormal breathing and a simulated AED sequence.","बिना प्रतिक्रिया वाला वयस्क, असामान्य साँस और आभासी AED का क्रम।",listOf(
            setup(initial),
            step("response","Get help for unresponsiveness","प्रतिक्रिया न मिले तो मदद बुलाएँ","The virtual adult does not respond. A bystander is available.","आभासी वयस्क प्रतिक्रिया नहीं देता। पास में एक व्यक्ति है।",BLS,initial,
                action("call-help-aed","You: call 112; bring an AED and report back","आप 112 पर कॉल करें; AED लाएँ और बताएँ",true,"Get help promptly and assess breathing. This does not place a real call.","तुरंत मदद लें और साँस जाँचें। यह वास्तविक कॉल नहीं करता।",false),
                action("wait-to-wake","Wait for them to wake up","खुद जागने का इंतज़ार करें",false,"Waiting can delay an urgent response.","इंतज़ार से ज़रूरी सहायता में देरी हो सकती है।",false)),
            step("breathing","Observe the breathing cue","साँस का संकेत देखें","Help is being requested. Read the chest cue: only irregular gasps are reported; the model does not animate breathing.","मदद बुलाई जा रही है। सीने का संकेत पढ़ें: अनियमित हाँफना बताया है; मॉडल साँस की गति नहीं दिखाता।",BLS,gasping,
                action("inspect:breathing","Inspect the authored breathing cue","दिया गया साँस का संकेत देखें",true,"Gasping is not normal breathing. The app has not measured anyone’s breathing.","हाँफना सामान्य साँस नहीं है। ऐप ने किसी की साँस मापी नहीं है।",true)),
            step("begin_cpr","Act on abnormal breathing","असामान्य साँस पर कार्रवाई करें","The adult is unresponsive and not breathing normally.","वयस्क प्रतिक्रिया नहीं देता और सामान्य साँस नहीं लेता।",BLS,gasping,
                action("begin-cpr-sequence","Start the CPR response; follow dispatcher guidance","CPR का क्रम शुरू करें; आपातकालीन निर्देश मानें",true,"This rehearses the decision to begin. Practise compressions only on a suitable manikin.","यह शुरू करने के निर्णय का अभ्यास है। छाती दबाने का अभ्यास केवल उचित पुतले पर करें।",false),
                action("compress-peer","Practise compressions on another learner","दूसरे शिक्षार्थी की छाती दबाकर अभ्यास करें",false,"Do not perform practice compressions on another person.","अभ्यास के लिए किसी असली व्यक्ति की छाती न दबाएँ।",false)),
            step("aed_power","The AED arrives","AED आ गया","The virtual device is available.","आभासी उपकरण उपलब्ध है।",BLS,gasping,
                action("aed-power-on","Turn on the virtual AED","आभासी AED चालू करें",true,"Follow its prompts. No real medical device is connected.","उसके निर्देश मानें। कोई वास्तविक चिकित्सा उपकरण जुड़ा नहीं है।",true),
                action("aed-guess-shock","Give a shock immediately","तुरंत झटका दें",false,"Do not guess whether a shock is needed.","झटके की ज़रूरत का अनुमान न लगाएँ।",false)),
            step("aed_diagram","Follow the pad diagram","पैड के चित्र का पालन करें","The device shows its pad instructions.","उपकरण पैड लगाने के निर्देश दिखाता है।",BLS,gasping,
                action("follow-device-diagram","Follow the AED pad diagrams and prompts","AED के पैड चित्र और निर्देश मानें",true,"This acknowledges the diagram; physical pad placement is not measured.","यह चित्र समझने का रिकॉर्ड है; वास्तविक पैड लगाने की जाँच नहीं।",false),
                action("invent-pad-position","Ignore the diagrams","चित्र अनदेखा करें",false,"Use the device-specific instructions, not a guess.","अनुमान के बजाय उसी उपकरण के निर्देश मानें।",false)),
            step("clear","Keep everyone clear","सबको दूर रखें","The authored AED prompt says it is analysing.","आभासी AED का संकेत है: विश्लेषण चल रहा है।",BLS,gasping,
                action("stand-clear","Nobody touches during analysis","विश्लेषण के समय कोई न छुए",true,"Keep clear during analysis and if a shock is advised.","विश्लेषण और झटका देने के निर्देश पर सब दूर रहें।",false),
                action("touch-during-analysis","Keep touching the casualty","व्यक्ति को छूते रहें",false,"Touching conflicts with the AED’s clear instruction.","छूना AED के दूर रहने के निर्देश के विरुद्ध है।",false)),
            step("resume_cpr","Follow the next prompt","अगला निर्देश मानें","Authored prompt: no shock advised; resume CPR. This is a fixed learning cue, not a rhythm reading.","दिया गया संकेत: झटका नहीं; CPR फिर शुरू करें। यह तय अभ्यास संकेत है, दिल की जाँच नहीं।",BLS,gasping,
                action("resume-cpr-sequence","Resume CPR as prompted","निर्देश के अनुसार CPR फिर शुरू करें",true,"Keep following the AED and emergency guidance.","AED और आपातकालीन निर्देशों का पालन करते रहें।",false),
                action("stop-after-analysis","Assume no shock means recovery","झटका न लगने को ठीक होना मानें",false,"No-shock advice does not establish recovery.","झटका न देने का निर्देश ठीक होने का प्रमाण नहीं।",false)),
            handover(gasping)
        ))
    }

    private fun unresponsive_normal_breathing():Scenario {
        val lying=visual(BodyRegion.CHEST,Breathing.NOT_ASSESSED,Response.UNRESPONSIVE,"Unresponsive adult; breathing has not yet been assessed.","वयस्क प्रतिक्रिया नहीं देता; साँस अभी नहीं जाँची गई।")
        val normal=lying.copy(breathing=Breathing.NORMAL,label="Authored cue: normal breathing, no suspected trauma.",labelHi="दिया गया संकेत: सामान्य साँस, चोट का संदेह नहीं।")
        val side=normal.copy(pose=Pose.RECOVERY_SIDE,label="Side-lying illustration; not a complete positioning demonstration.",labelHi="करवट की सांकेतिक स्थिति; पूरा तरीका दिखाने वाला प्रदर्शन नहीं।")
        return Scenario("unresponsive-normal-breathing","Unresponsive but breathing","प्रतिक्रिया नहीं, लेकिन साँस सामान्य","Recognise normal breathing, protect the airway and notice deterioration.","सामान्य साँस पहचानें, वायुमार्ग की सुरक्षा और बिगड़ती हालत पर ध्यान दें।",listOf(
            setup(lying),
            step("response","Summon help","मदद बुलाएँ","The adult is unresponsive. There is no reported fall or injury.","वयस्क प्रतिक्रिया नहीं देता। गिरने या चोट की जानकारी नहीं है।",BLS,lying,
                action("call-emergency","Get emergency help now","तुरंत आपातकालीन मदद लें",true,"Assess breathing while help is being requested.","मदद बुलाते समय साँस जाँचें।",false),
                action("give-water","Give water to wake them","जगाने के लिए पानी पिलाएँ",false,"An unresponsive person may not swallow safely.","प्रतिक्रिया न देने वाला व्यक्ति सुरक्षित रूप से निगल नहीं सकता।",false)),
            step("breathing","Inspect normal breathing","सामान्य साँस का संकेत देखें","The authored chest report says breathing is regular and normal. This is written scenario information, not animated or measured breathing.","सीने के लिखे विवरण में साँस नियमित और सामान्य बताई है। यह दृश्य की जानकारी है, दिखाई या मापी गई साँस नहीं।",RECOVERY,normal,
                action("inspect:breathing","Inspect the normal-breathing cue","सामान्य साँस का संकेत देखें",true,"Normal breathing changes the response; continue monitoring.","सामान्य साँस होने पर प्रतिक्रिया बदलती है; निगरानी जारी रखें।",true)),
            step("position","Protect the airway","वायुमार्ग सुरक्षित रखें","Normal breathing continues and no trauma is suspected in this case.","इस स्थिति में सामान्य साँस जारी है और चोट का संदेह नहीं है।",RECOVERY,normal,
                action("recovery-position","Use the recovery-position response","रिकवरी पोज़िशन का क्रम अपनाएँ",true,"The next view is illustrative. Learn the actual positioning technique with a qualified instructor.","अगला दृश्य सांकेतिक है। सही तरह करवट देने का तरीका योग्य प्रशिक्षक से सीखें।",false),
                action("ignore-airway","Leave them unobserved on their back","पीठ के बल छोड़कर बिना निगरानी चले जाएँ",false,"Airway protection and monitoring still matter.","वायुमार्ग की सुरक्षा और निगरानी अभी भी ज़रूरी हैं।",false)),
            step("monitor","Stay and reassess","साथ रहें और फिर जाँचें","The side-lying illustration is shown. The authored report says breathing remains normal; the model does not animate breathing.","करवट की सांकेतिक स्थिति दिखाई है। लिखे विवरण में साँस सामान्य बताई है; मॉडल साँस की गति नहीं दिखाता।",RECOVERY,side,
                action("monitor-breathing","Keep checking breathing and response","साँस और प्रतिक्रिया देखते रहें",true,"Report changes to emergency services.","बदलाव की जानकारी आपातकालीन सेवा को दें।",false),
                action("leave-alone","Leave because the position is set","करवट देने के बाद अकेला छोड़ दें",false,"The position does not remove the need to monitor.","करवट देने के बाद भी निगरानी ज़रूरी है।",false)),
            step("change","Recognise deterioration","बिगड़ती हालत पहचानें","New authored cue: breathing becomes abnormal gasps.","नया संकेत: साँस अब असामान्य हाँफने जैसी है।",BLS,side.copy(breathing=Breathing.ABNORMAL,label="New cue: abnormal gasping.",labelHi="नया संकेत: असामान्य हाँफना।"),
                action("change-to-cpr","Update emergency help and begin the CPR response","आपातकालीन सेवा को बताएँ और CPR का क्रम शुरू करें",true,"Do not continue the normal-breathing pathway after breathing changes.","साँस बदलने पर सामान्य साँस वाला क्रम जारी न रखें।",false),
                action("leave-on-side","Keep waiting without reassessment","फिर जाँचे बिना इंतज़ार करें",false,"Abnormal breathing requires immediate reassessment and action.","असामान्य साँस पर तुरंत फिर जाँच और कार्रवाई चाहिए।",false)),
            handover(gaspingCue(lying))
        ))
    }

    private fun bleeding_injury():Scenario {
        val injury=visual(BodyRegion.RIGHT_FOREARM,Breathing.NORMAL,Response.RESPONSIVE,"Authored forearm bleeding cue; no real injury detection.","बाँह से खून बहने का दिया गया संकेत; असली चोट की पहचान नहीं।")
        return Scenario("bleeding-injury","A bleeding forearm","बाँह से खून बहना","Recognise serious bleeding, apply simulated pressure and escalate.","गंभीर रक्तस्राव पहचानें, आभासी दबाव का क्रम और मदद लें।",listOf(
            setup(injury),
            step("observe","Locate the injury cue","चोट का संकेत पहचानें","The virtual adult is lying down, alert, with heavy bleeding from the forearm.","आभासी वयस्क लेटा है, होश में है और बाँह से अधिक खून बह रहा है।",BLEED,injury,
                action("inspect:forearm","Inspect the forearm cue","बाँह का संकेत देखें",true,"Observation identifies the authored injury location only.","इससे केवल अभ्यास में दी गई चोट की जगह पहचानी जाती है।",true)),
            step("protect","Protect yourself and get help","अपनी सुरक्षा और मदद","You have disposable gloves and a clean dressing; no object is embedded.","दस्ताने और साफ पट्टी उपलब्ध हैं; घाव में कोई वस्तु नहीं फँसी।",BLEED,injury,
                action("barrier-call","Use a barrier and request emergency help","सुरक्षा अवरोध इस्तेमाल करें और आपातकालीन मदद बुलाएँ",true,"Avoid blood contact; severe bleeding needs prompt help.","खून के सीधे संपर्क से बचें; गंभीर रक्तस्राव में तुरंत मदद चाहिए।",false),
                action("bare-hands","Ignore protection and handle blood","सुरक्षा छोड़कर खून छुएँ",false,"Use available protection against blood exposure.","खून के संपर्क से बचने के लिए उपलब्ध सुरक्षा अपनाएँ।",false)),
            step("pressure","Control the bleeding","खून रोकने का प्रयास","The wound has no embedded object.","घाव में कोई वस्तु नहीं फँसी है।",BLEED,injury,
                action("apply-pressure","Apply firm direct pressure with the dressing","पट्टी से घाव पर सीधा दृढ़ दबाव दें",true,"This selects simulated pressure; the phone does not measure force or bleeding control.","यह आभासी दबाव का निर्णय है; फ़ोन दबाव या खून रुकना नहीं मापता।",true),
                action("wash-first","Wash instead of controlling heavy bleeding","अधिक खून रोकने के बजाय पहले धोएँ",false,"Do not delay control of severe bleeding for cleaning.","सफाई के लिए गंभीर रक्तस्राव रोकने में देरी न करें।",false)),
            step("embedded","Apply the exception","अलग स्थिति समझें","Learning variation: a piece of glass is embedded in a wound.","बदली हुई अभ्यास स्थिति: घाव में काँच फँसा है।",BLEED,injury,
                action("pressure-around","Leave it in place; press beside it, not on it","उसे न निकालें; उसके आसपास दबाव दें, उस पर नहीं",true,"Seek urgent care; removing it can worsen bleeding.","तुरंत चिकित्सा सहायता लें; निकालने से खून बढ़ सकता है।",false),
                action("pull-glass","Pull the glass out","काँच बाहर खींचें",false,"Do not remove an embedded object yourself.","फँसी हुई वस्तु खुद न निकालें।",false)),
            handover(injury)
        ))
    }

    private fun thermal_burn_response():Scenario {
        val injury=visual(BodyRegion.LEFT_FOREARM,Breathing.NORMAL,Response.RESPONSIVE,"Authored forearm scald cue; the overlay does not grade burn depth.","बाँह पर जलने का दिया गया संकेत; दृश्य से जलने की गहराई नहीं जाँची जाती।")
        return Scenario("thermal-burn-response","A hot-water scald","गरम पानी से जलना","Cool a thermal scald, avoid harmful remedies and recognise referral needs.","गरम पानी से जले भाग को ठंडा करें, गलत उपायों से बचें और मदद पहचानें।",listOf(
            setup(injury),
            step("observe","Identify the thermal injury","गर्मी से लगी चोट पहचानें","An alert adult is lying comfortably after a hot-water spill. The heat source is removed.","गरम पानी गिरने के बाद होश में वयस्क आराम से लेटा है। गर्मी का स्रोत हटा दिया गया है।",BURN,injury,
                action("inspect:burn","Inspect the authored scald cue","जलने का दिया गया संकेत देखें",true,"This is a hot-water scald case, not chemical or electrical injury.","यह गरम पानी से जलने की स्थिति है, रसायन या बिजली की चोट नहीं।",true)),
            step("cool","Cool the affected area","जले भाग को ठंडा करें","Cool running water is safely available.","सुरक्षित रूप से ठंडा बहता पानी उपलब्ध है।",BURN,injury,
                action("cool-running-water","Cool with cool running water for 20 minutes","ठंडे बहते पानी से 20 मिनट ठंडा करें",true,"Keep the rest of the person warm; the on-screen sequence is not an actual treatment timer.","बाकी शरीर गर्म रखें; स्क्रीन का क्रम वास्तविक उपचार का टाइमर नहीं है।",false),
                action("ice-remedy","Use ice, butter or toothpaste","बर्फ, मक्खन या टूथपेस्ट लगाएँ",false,"Avoid these remedies; they are not the cooling method taught here.","ये उपाय न करें; यहाँ ठंडा करने के लिए इनका उपयोग नहीं है।",false)),
            step("clothing","Do not pull stuck material","चिपकी हुई चीज़ न खींचें","A sleeve has stuck to the injured skin.","बाँह का कपड़ा जली त्वचा से चिपका है।",BURN,injury,
                action("leave-stuck","Leave stuck material in place","चिपकी चीज़ को वहीं रहने दें",true,"Remove nearby loose items only when safe; leave attached material.","सुरक्षित हो तो आसपास की ढीली चीज़ हटाएँ; चिपकी चीज़ रहने दें।",false),
                action("tear-clothing","Tear the stuck sleeve away","चिपकी बाँह फाड़कर हटाएँ",false,"Pulling attached material can worsen injury.","चिपका कपड़ा खींचने से चोट बढ़ सकती है।",false)),
            step("cover-help","Cover and escalate","ढकें और सहायता लें","The burn is cooled. In this variation it covers a large area.","जला भाग ठंडा किया गया है। इस बदली हुई स्थिति में बड़ा हिस्सा जला है।",BURN,injury,
                action("cover-seek-care","Cover loosely with a suitable clean covering; seek urgent care","उचित साफ आवरण से ढीला ढकें; तुरंत चिकित्सा सहायता लें",true,"Do not wrap tightly. Large or deep burns need urgent assessment.","कसकर न लपेटें। बड़े या गहरे जले भाग की तुरंत जाँच चाहिए।",false),
                action("tight-wrap","Wrap tightly and skip medical assessment","कसकर लपेटें और चिकित्सा जाँच छोड़ दें",false,"A tight covering and delayed assessment are unsafe choices.","कसकर ढकना और जाँच में देरी असुरक्षित हैं।",false)),
            handover(injury)
        ))
    }

    private fun suspected_limb_injury():Scenario {
        val injury=visual(BodyRegion.RIGHT_LOWER_LEG,Breathing.NORMAL,Response.RESPONSIVE,"Authored lower-leg pain and swelling; no fracture diagnosis.","निचले पैर में दर्द और सूजन का संकेत; फ्रैक्चर का निदान नहीं।")
        return Scenario("suspected-limb-injury","A suspected lower-leg injury","निचले पैर में चोट का संदेह","Support a painful limb and recognise serious warning signs.","दर्द वाले पैर को सहारा दें और गंभीर संकेत पहचानें।",listOf(
            setup(injury),
            step("observe","Notice the injury location","चोट की जगह देखें","An adult is already lying after a fall and reports lower-leg pain.","वयस्क गिरने के बाद पहले से लेटा है और निचले पैर में दर्द बताता है।",FRACTURE,injury,
                action("inspect:leg","Inspect the lower-leg cue","निचले पैर का संकेत देखें",true,"Location is authored. The camera cannot tell a sprain from a fracture.","जगह पहले से तय है। कैमरा मोच और फ्रैक्चर में अंतर नहीं बता सकता।",true)),
            step("support","Keep it supported","सहारा बनाए रखें","Movement increases pain.","हिलने से दर्द बढ़ता है।",FRACTURE,injury,
                action("support-position","Support in the position found; arrange medical care","मिली हुई स्थिति में सहारा दें; चिकित्सा सहायता लें",true,"Avoid unnecessary movement or testing the limb.","अनावश्यक हिलाना या पैर की परीक्षा लेना टालें।",false),
                action("straighten-limb","Straighten it forcefully","ज़ोर से सीधा करें",false,"Do not attempt to realign a suspected fracture.","फ्रैक्चर के संदेह में खुद सीधा करने की कोशिश न करें।",false)),
            step("red-flags","Escalate serious signs","गंभीर संकेत पर मदद लें","Authored report: the foot feels numb and cold, and a bystander reports a change in leg shape. These findings are reported, not demonstrated by the mesh.","दिया गया विवरण: पैर सुन्न और ठंडा महसूस होता है; पास का व्यक्ति आकार बदलना बताता है। ये बताई गई बातें हैं, मॉडल में दिखाई नहीं गईं।",LIMB_RED_FLAGS,injury,
                action("urgent-help","Request emergency medical help","आपातकालीन चिकित्सा मदद बुलाएँ",true,"Report the changed sensation and appearance without making a diagnosis.","निदान किए बिना संवेदना और आकार का बदलाव बताएँ।",false),
                action("walk-test","Make them walk to test it","जाँचने के लिए चलाएँ",false,"Weight-bearing tests can worsen the injury.","चलाकर जाँचने से चोट बढ़ सकती है।",false)),
            step("comfort","Wait safely","सुरक्षित इंतज़ार करें","Help is on the way and the adult remains responsive.","मदद आ रही है और वयस्क प्रतिक्रिया दे रहा है।",FRACTURE,injury,
                action("reassure-monitor","Reassure and watch for changes","भरोसा दिलाएँ और बदलाव देखें",true,"Keep support in place and share changes with responders.","सहारा बनाए रखें और बदलाव सहायता देने वालों को बताएँ।",false),
                action("leave-alone","Leave them unattended","अकेला छोड़ दें",false,"Continue observing rather than assuming the condition cannot change.","यह मानने के बजाय देखते रहें कि हालत बदल नहीं सकती।",false)),
            handover(injury)
        ))
    }

    private fun seizure_response():Scenario {
        val active=visual(BodyRegion.HEAD,Breathing.NOT_ASSESSED,Response.UNRESPONSIVE,"Authored seizure report; jerking and breathing movements are not animated in this model.","दौरे का लिखा विवरण; इस मॉडल में झटके और साँस की गति नहीं दिखाई जाती।")
        val normal=active.copy(breathing=Breathing.NORMAL,label="Authored update: jerking has stopped and breathing is normal. These movements are not animated.",labelHi="दिया गया नया विवरण: झटके रुके और साँस सामान्य है। ये गतियाँ नहीं दिखाई जातीं।")
        return Scenario("seizure-response","Protect during a seizure","दौरे के समय सुरक्षा","Reduce injury, time the event and reassess breathing afterwards.","चोट से बचाएँ, समय नोट करें और बाद में साँस फिर जाँचें।",listOf(
            setup(active),
            step("observe","Observe the cue","संकेत देखें","The authored scene describes an adult with jerking movements on the ground.","दिए गए दृश्य में ज़मीन पर पड़े वयस्क को झटके आते बताए गए हैं।",SEIZURE,active,
                action("inspect:response","Inspect the authored response cue","दिया गया प्रतिक्रिया का संकेत देखें",true,"This is scenario information, not camera detection of a seizure.","यह अभ्यास की जानकारी है, कैमरे से दौरे की पहचान नहीं।",true)),
            step("call","Get emergency help","आपातकालीन मदद लें","This is their first known seizure in the authored case.","दिए गए मामले में यह उनका पहला ज्ञात दौरा है।",SEIZURE,active,
                action("call-emergency","Call for emergency help now","अभी आपातकालीन मदद बुलाएँ",true,"First seizure, prolonged or repeated events, injury or breathing problems need emergency help.","पहला, लंबा या बार-बार दौरा, चोट या साँस की समस्या में आपातकालीन मदद चाहिए।",false),
                action("wait-ignore","Ignore it because some seizures stop","कुछ दौरे रुक जाते हैं, इसलिए अनदेखा करें",false,"This case has a reason for urgent medical help.","इस मामले में तत्काल चिकित्सा मदद की ज़रूरत है।",false)),
            step("protect","Reduce injury","चोट का जोखिम कम करें","Objects are nearby.","पास में कुछ वस्तुएँ हैं।",SEIZURE,active,
                action("clear-cushion-time","Clear hazards, cushion the head and note the time","खतरे हटाएँ, सिर को नरम सहारा दें और समय नोट करें",true,"Do not restrain the person.","व्यक्ति को ज़बरदस्ती पकड़कर न रोकें।",false),
                action("restrain","Hold their limbs down","हाथ-पैर दबाकर रोकें",false,"Restraint can cause injury.","ज़बरदस्ती रोकने से चोट लग सकती है।",false)),
            step("mouth","Keep the mouth clear","मुँह में कुछ न डालें","A bystander offers a spoon.","पास का व्यक्ति चम्मच देने को कहता है।",SEIZURE,active,
                action("nothing-mouth","Put nothing in the mouth","मुँह में कुछ न डालें",true,"Do not offer food or drink until fully recovered.","पूरी तरह ठीक प्रतिक्रिया आने तक खाने-पीने को न दें।",false),
                action("spoon-mouth","Place the spoon between the teeth","दाँतों के बीच चम्मच रखें",false,"Objects in the mouth can injure or obstruct.","मुँह में वस्तु चोट या रुकावट पैदा कर सकती है।",false)),
            step("after","Recheck after jerking stops","झटके रुकने के बाद फिर जाँचें","Authored update: jerking has stopped; the adult remains unresponsive with normal breathing and no suspected trauma. The model does not animate these movements.","दिया गया नया विवरण: झटके रुके, वयस्क प्रतिक्रिया नहीं देता, साँस सामान्य है और चोट का संदेह नहीं। मॉडल ये गतियाँ नहीं दिखाता।",RECOVERY,normal,
                action("recovery-monitor","Use the recovery response and monitor breathing","रिकवरी पोज़िशन का क्रम अपनाएँ और साँस देखें",true,"If breathing becomes abnormal, switch to the emergency CPR response.","साँस असामान्य हो तो आपातकालीन CPR का क्रम अपनाएँ।",false),
                action("leave-asleep","Leave them to sleep alone","अकेला सोने दें",false,"Keep monitoring while help is coming.","मदद आने तक देखते रहें।",false)),
            handover(normal.copy(pose=Pose.RECOVERY_SIDE))
        ))
    }

    private fun fainting_response_bay():Scenario {
        val awake=visual(BodyRegion.HEAD,Breathing.NORMAL,Response.RESPONSIVE,"Adult already lying down, reporting dizziness; responsive.","वयस्क पहले से लेटा है, चक्कर बताता है और प्रतिक्रिया देता है।")
        return Scenario("fainting-response-bay","Feeling faint and recovering","बेहोशी जैसा लगना और सँभलना","Help a responsive adult avoid a fall and recognise an emergency change.","होश में वयस्क को गिरने से बचाएँ और गंभीर बदलाव पहचानें।",listOf(
            setup(awake),
            step("observe","Listen to the symptom cue","लक्षण का संकेत सुनें","The virtual adult says they feel faint and has lain down safely; no injury is reported.","आभासी वयस्क बेहोशी जैसा लगना बताता है और सुरक्षित लेट गया है; चोट की जानकारी नहीं।",FAINT,awake,
                action("inspect:response","Inspect the responsive-adult cue","प्रतिक्रिया देने वाले वयस्क का संकेत देखें",true,"A responsive person feeling faint is different from someone remaining unresponsive.","होश में बेहोशी जैसा लगना और लगातार प्रतिक्रिया न देना अलग स्थितियाँ हैं।",true)),
            step("support","Give space and support","जगह और सहारा दें","They remain alert while lying down.","लेटे हुए वे होश में हैं।",FAINT,awake,
                action("support-rest","Let them rest lying down; raise legs slightly if possible","लेटे आराम करने दें; संभव हो तो पैर थोड़ा ऊँचा करें",true,"Reassure and give space; the straight model does not demonstrate leg positioning.","भरोसा और जगह दें; सीधा मॉडल पैर उठाने का तरीका नहीं दिखाता।",false),
                action("stand-quickly","Pull them quickly upright","जल्दी खड़ा कर दें",false,"Sudden standing may make them feel faint again.","अचानक खड़े होने पर फिर बेहोशी जैसा लग सकता है।",false)),
            step("recover","Allow gradual recovery","धीरे सँभलने दें","They now say they feel better.","अब वे बेहतर लगना बताते हैं।",FAINT,awake.copy(response=Response.RECOVERING),
                action("rise-gradually","Help them sit up slowly when ready","तैयार होने पर धीरे बैठने में मदद दें",true,"Observe for recurring symptoms; the model stays lying as an illustration.","लक्षण लौटें तो ध्यान दें; सांकेतिक मॉडल लेटा रहता है।",false),
                action("resume-exercise","Return to exercise immediately","तुरंत व्यायाम शुरू कराएँ",false,"Do not rush recovery or assume the cause is harmless.","जल्दी न करें और कारण को मामूली न मानें।",false)),
            step("change","Change the response if needed","ज़रूरत पर प्रतिक्रिया बदलें","Alternative cue: the adult stops responding and has abnormal breathing.","बदला हुआ संकेत: वयस्क प्रतिक्रिया नहीं देता और साँस असामान्य है।",BLS,gaspingCue(awake),
                action("emergency-cpr","Get emergency help and begin the CPR response","आपातकालीन मदद लें और CPR का क्रम शुरू करें",true,"Do not wait for a presumed faint to pass when breathing is abnormal.","साँस असामान्य हो तो इसे सामान्य बेहोशी मानकर इंतज़ार न करें।",false),
                action("give-drink","Give a drink to wake them","जगाने के लिए पानी पिलाएँ",false,"Do not give oral fluid to an unresponsive person.","प्रतिक्रिया न देने वाले व्यक्ति को मुँह से तरल न दें।",false)),
            handover(gaspingCue(awake))
        ))
    }

    private fun heat_illness_response():Scenario {
        val awake=visual(BodyRegion.WHOLE_BODY,Breathing.NORMAL,Response.RESPONSIVE,"Authored heat exposure, dizziness and sweating; adult resting in shade.","गर्मी, चक्कर और पसीने का संकेत; वयस्क छाया में आराम कर रहा है।")
        val ill=awake.copy(label="New authored cue: confusion and poor coordination.",labelHi="नया दिया गया संकेत: भ्रम और तालमेल की कमी।")
        return Scenario("heat-illness-response","Heat illness and deterioration","गर्मी की बीमारी और बिगड़ती हालत","Cool an alert adult, then respond immediately to confusion.","होश में वयस्क को ठंडा करें; भ्रम होने पर तुरंत मदद लें।",listOf(
            setup(awake),
            step("observe","Recognise the heat cue","गर्मी का संकेत पहचानें","An adult rests after heavy outdoor work. They are dizzy, sweaty and answer clearly.","बाहर मेहनत के बाद वयस्क आराम करता है। चक्कर, पसीना है और साफ जवाब देता है।",HEAT,awake,
                action("inspect:heat-cue","Inspect the authored heat-illness cue","गर्मी की बीमारी का दिया संकेत देखें",true,"The phone does not measure temperature or diagnose heat illness.","फ़ोन तापमान नहीं मापता और बीमारी का निदान नहीं करता।",true)),
            step("cool","Start cooling","ठंडा करना शुरू करें","They are in a cool place, fully alert and able to swallow.","वे ठंडी जगह पर हैं, पूरे होश में हैं और निगल सकते हैं।",HEAT,awake,
                action("cool-and-water","Cool the skin, remove excess layers and offer water","त्वचा ठंडी करें, अतिरिक्त कपड़े हटाएँ और पानी दें",true,"Use cool water and fanning; stay with them.","ठंडा पानी और हवा इस्तेमाल करें; साथ रहें।",false),
                action("keep-working","Ask them to finish the work","पहले काम पूरा करने को कहें",false,"Continuing heat exposure can worsen illness.","गर्मी में बने रहने से हालत बिगड़ सकती है।",false)),
            step("confusion","Do not wait after deterioration","हालत बिगड़े तो इंतज़ार न करें","They become confused and uncoordinated.","वे भ्रमित हो जाते हैं और तालमेल खो देते हैं।",HEAT,ill,
                action("call-and-cool","Call emergency help now and keep cooling","अभी आपातकालीन मदद बुलाएँ और ठंडा करना जारी रखें",true,"Serious signs need immediate help; do not wait for a timer.","गंभीर संकेत में तुरंत मदद चाहिए; टाइमर का इंतज़ार न करें।",false),
                action("wait-thirty","Wait before deciding whether to call","कॉल का निर्णय लेने से पहले इंतज़ार करें",false,"Confusion is an urgent change in this case.","इस मामले में भ्रम गंभीर बदलाव है।",false)),
            step("swallow","Respond to loss of consciousness","होश न रहे तो क्रम बदलें","Alternative cue: the adult becomes unresponsive. Breathing needs reassessment.","बदला हुआ संकेत: वयस्क प्रतिक्रिया नहीं देता। साँस फिर जाँचनी है।",FIRST_AID,ill.copy(response=Response.UNRESPONSIVE,breathing=Breathing.NOT_ASSESSED,label="Authored update: no response. Breathing must be reassessed; no measurement is supplied.",labelHi="दिया गया नया विवरण: प्रतिक्रिया नहीं। साँस फिर जाँचनी है; कोई माप नहीं दिया गया।"),
                action("reassess-breathing","Stop oral drinks; reassess breathing and follow emergency guidance","पिलाना रोकें; साँस फिर जाँचें और आपातकालीन निर्देश मानें",true,"Normal versus abnormal breathing determines the next response; do not force fluid.","सामान्य या असामान्य साँस से अगला क्रम तय होगा; ज़बरदस्ती तरल न दें।",false),
                action("force-water","Pour water into the mouth","मुँह में पानी डालें",false,"An unresponsive person cannot be assumed to swallow safely.","प्रतिक्रिया न देने वाले को सुरक्षित निगल सकने वाला न मानें।",false)),
            handover(ill.copy(response=Response.UNRESPONSIVE,breathing=Breathing.NOT_ASSESSED,label="Authored update: no response. Breathing must be reassessed; no measurement is supplied.",labelHi="दिया गया नया विवरण: प्रतिक्रिया नहीं। साँस फिर जाँचनी है; कोई माप नहीं दिया गया।"))
        ))
    }
    val all:List<Scenario> = listOf(adult_collapse_response(), unresponsive_normal_breathing(), bleeding_injury(), thermal_burn_response(), suspected_limb_injury(), seizure_response(), fainting_response_bay(), heat_illness_response())
    fun find(id:String):Scenario?=all.firstOrNull{it.id==id}
}
