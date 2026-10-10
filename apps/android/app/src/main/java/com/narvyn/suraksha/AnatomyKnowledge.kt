package com.narvyn.suraksha

/** Factual anatomy text, separate from scenario instructions and the geometry's provenance notice. */
internal data class AnatomyInfo(
    val title: String,
    val description: String,
    val location: String,
    val function: String,
    val learningNote: String? = null,
    val sourceUrls: List<String>,
    val isGroup: Boolean = false,
    val language: String = "en",
) {
    val introText: String get() = listOf("$title.", description, location, function).joinToString(" ")
}

/**
 * Exact BodyParts3D identifiers only. A nearby mesh, a scenario target or a translated name must
 * never select medical text. Descriptions concern the labelled structure, not an individual patient.
 * English and Hindi are authored here; other locales explicitly fall back to English.
 * Factual sources and review limitations: docs/learning-flow/anatomy-sources.md.
 */
internal object AnatomyKnowledge {
    private const val OPEN = "https://openstax.org/books/anatomy-and-physiology-2e/pages/"
    private const val NCBI = "https://www.ncbi.nlm.nih.gov/books/"
    private const val UPPER = OPEN + "11-5-muscles-of-the-pectoral-girdle-and-upper-limbs"
    private const val LOWER = OPEN + "11-6-appendicular-muscles-of-the-pelvic-girdle-and-lower-limbs"
    private const val NECK = OPEN + "11-3-axial-muscles-of-the-head-neck-and-back"
    private const val TRUNK = OPEN + "11-4-axial-muscles-of-the-abdominal-wall-and-thorax"
    private const val MODEL = "https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html"
    private data class Text(val en: String, val hi: String) {
        fun inLanguage(language: String) = if (language == "hi") hi else en
    }
    private data class Entry(
        val title: Text, val description: Text, val location: Text, val function: Text,
        val sources: List<String>, val group: Boolean = false,
    ) {
        fun localize(language: String) = AnatomyInfo(
            title.inLanguage(language), description.inLanguage(language), location.inLanguage(language),
            function.inLanguage(language), sourceUrls = sources, isGroup = group, language = language,
        )
    }
    private fun t(en: String, hi: String) = Text(en, hi)
    private val entries: Map<String, Entry> = buildMap {
        fun one(id: String, title: Text, description: Text, location: Text, function: Text,
                source: String, group: Boolean = false) {
            check(put(id, Entry(title, description, location, function,
                if (group) listOf(source, MODEL).distinct() else listOf(source), group)) == null)
        }
        fun pair(right: String, left: String, title: Text, description: Text, location: Text,
                 function: Text, source: String) {
            one(right, t("Right ${title.en}", "दायाँ भाग · ${title.hi}"), description, location, function, source)
            one(left, t("Left ${title.en}", "बायाँ भाग · ${title.hi}"), description, location, function, source)
        }

        one("FMA7163", t("Skin", "त्वचा"),
            t("The body's outer covering and protective barrier.", "शरीर का बाहरी आवरण और सुरक्षा की परत।"),
            t("Covers the outside of the body; deeper tissue lies beneath it.", "शरीर की बाहरी सतह पर; इसके नीचे गहरे ऊतक होते हैं।"),
            t("Limits water loss, senses touch and helps regulate temperature.", "पानी की हानि कम करती है, स्पर्श महसूस करती है और तापमान नियंत्रित करने में मदद करती है।"),
            OPEN + "5-1-layers-of-the-skin")
        one("FMA7485", t("Sternum", "उरोस्थि · छाती की बीच की हड्डी"),
            t("A flat bone at the front of the chest, also called the breastbone.", "छाती के सामने बीच में स्थित चपटी हड्डी।"),
            t("Between the right and left ribs; connected to rib cartilage and the collarbones.", "दाईं और बाईं पसलियों के बीच; पसलियों की उपास्थि और हंसली से जुड़ी होती है।"),
            t("Supports the rib cage and helps protect the organs within the chest.", "पसली-पिंजर को सहारा देती है और छाती के अंगों की रक्षा में मदद करती है।"),
            OPEN + "7-4-the-thoracic-cage")
        pair("FMA23130", "FMA23131", t("humerus", "ह्यूमरस · ऊपरी बाँह की हड्डी"),
            t("The long bone of the upper arm.", "ऊपरी बाँह की लंबी हड्डी।"),
            t("Between the shoulder and elbow.", "कंधे और कोहनी के बीच।"),
            t("Forms part of both joints and acts as a lever for arm muscles.", "दोनों जोड़ों का हिस्सा बनती है और बाँह की मांसपेशियों को गति देने का आधार देती है।"),
            OPEN + "8-2-bones-of-the-upper-limb")
        pair("FMA23464", "FMA23465", t("radius", "रेडियस · अग्रबाहु की हड्डी"),
            t("One of the two forearm bones, on the thumb side.", "अग्रबाहु की दो हड्डियों में से एक, अंगूठे की ओर।"),
            t("Between the elbow and wrist, beside the ulna.", "कोहनी और कलाई के बीच, अल्ना के पास।"),
            t("Helps support the wrist and turns around the ulna when the palm rotates.", "कलाई को सहारा देती है और हथेली घुमाते समय अल्ना के आसपास घूमती है।"),
            OPEN + "8-2-bones-of-the-upper-limb")
        pair("FMA24474", "FMA24475", t("femur", "फीमर · जाँघ की हड्डी"),
            t("The long, weight-bearing bone of the thigh.", "जाँघ की लंबी हड्डी जो शरीर का भार उठाती है।"),
            t("Between the hip and knee.", "कूल्हे और घुटने के बीच।"),
            t("Transfers body weight and provides attachment for muscles that move the hip and knee.", "शरीर का भार आगे पहुँचाती है; कूल्हा और घुटना चलाने वाली मांसपेशियाँ इससे जुड़ती हैं।"),
            OPEN + "8-4-bones-of-the-lower-limb")
        pair("FMA24477", "FMA24478", t("tibia", "टिबिया · पिंडली की मुख्य हड्डी"),
            t("The larger, inner bone of the lower leg, also called the shinbone.", "घुटने के नीचे पैर की बड़ी, अंदर की ओर स्थित हड्डी।"),
            t("From the knee to the ankle, beside the thinner fibula.", "घुटने से टखने तक, पतली फिबुला हड्डी के पास।"),
            t("Carries most of the lower leg's weight and contributes to the knee and ankle joints.", "पैर के निचले हिस्से में अधिकांश भार उठाती है और घुटने तथा टखने के जोड़ का हिस्सा है।"),
            OPEN + "8-4-bones-of-the-lower-limb")
        one("FMA7480", t("Rib cage", "पसली-पिंजर"),
            t("The curved ribs and associated chest framework shown together.", "घुमावदार पसलियाँ और छाती का संबंधित ढाँचा एक साथ दिखाया गया है।"),
            t("Around the chest, between the upper spine and breastbone.", "छाती के चारों ओर, ऊपरी रीढ़ और उरोस्थि के बीच।"),
            t("Shields the heart and lungs while allowing the chest to expand during breathing.", "हृदय और फेफड़ों की रक्षा करता है और साँस लेते समय छाती को फैलने देता है।"),
            OPEN + "7-4-the-thoracic-cage", group = true)
        one("FMA13478", t("Vertebral column", "रीढ़ की हड्डियों का स्तंभ"),
            t("A linked column of vertebrae, rather than one solid bone.", "कई जुड़ी हुई कशेरुकाओं का स्तंभ, एक अकेली ठोस हड्डी नहीं।"),
            t("Along the middle of the back, from the skull toward the pelvis.", "पीठ के बीच में, खोपड़ी से श्रोणि की ओर।"),
            t("Supports the head and trunk, permits movement and encloses the spinal canal.", "सिर और धड़ को सहारा देता है, गति की अनुमति देता है और मेरु नलिका को घेरता है।"),
            OPEN + "7-3-the-vertebral-column", group = true)
        one("FMA23881", t("Other bones · grouped", "अन्य हड्डियाँ · समूह"),
            t("Remaining source bones shown as a group, without individual bone labels.", "स्रोत की बाकी हड्डियाँ एक समूह में हैं; हर हड्डी का अलग नाम उपलब्ध नहीं है।"),
            t("Distributed across the skeleton; this selection can contain several separate bones.", "कंकाल के कई हिस्सों में; इस चयन में अलग-अलग हड्डियाँ हो सकती हैं।"),
            t("Bones provide support, protect organs and give muscles attachment points.", "हड्डियाँ सहारा देती हैं, अंगों की रक्षा करती हैं और मांसपेशियों के जुड़ने का आधार बनती हैं।"),
            OPEN + "6-1-the-functions-of-the-skeletal-system", group = true)

        one("FMA7394", t("Trachea · windpipe", "श्वासनली"),
            t("The air tube linking the voice box with the main airways to the lungs.", "स्वरयंत्र को फेफड़ों तक जाने वाले मुख्य वायुमार्गों से जोड़ने वाली नली।"),
            t("In the neck and upper chest, in front of the food pipe.", "गर्दन और ऊपरी छाती में, भोजन नली के सामने।"),
            t("Conducts air; cartilage supports the tube and its lining helps clear particles.", "हवा पहुँचाती है; उपास्थि इसे खुला रखती है और अंदर की परत कणों को बाहर निकालने में मदद करती है।"),
            "https://www.nhlbi.nih.gov/health/lungs/respiratory-system")
        one("FMA7309", t("Right lung", "दायाँ फेफड़ा"),
            t("The right breathing organ, divided into three lobes.", "दाईं ओर का श्वसन अंग, जिसमें तीन खंड होते हैं।"),
            t("Inside the right side of the rib cage, above the diaphragm.", "पसली-पिंजर के दाईं ओर, डायफ्राम के ऊपर।"),
            t("Tiny air sacs exchange oxygen and carbon dioxide with the blood.", "छोटी वायुकोषिकाएँ रक्त के साथ ऑक्सीजन और कार्बन डाइऑक्साइड का आदान-प्रदान करती हैं।"),
            "https://www.nhlbi.nih.gov/health/lungs/respiratory-system")
        one("FMA7310", t("Left lung", "बायाँ फेफड़ा"),
            t("The left breathing organ, with two lobes and space beside it for the heart.", "बाईं ओर का श्वसन अंग, जिसमें दो खंड होते हैं और पास में हृदय के लिए जगह होती है।"),
            t("Inside the left side of the rib cage, above the diaphragm.", "पसली-पिंजर के बाईं ओर, डायफ्राम के ऊपर।"),
            t("Tiny air sacs exchange oxygen and carbon dioxide with the blood.", "छोटी वायुकोषिकाएँ रक्त के साथ ऑक्सीजन और कार्बन डाइऑक्साइड का आदान-प्रदान करती हैं।"),
            "https://www.nhlbi.nih.gov/health/lungs/respiratory-system")
        one("FMA7088", t("Heart", "हृदय"),
            t("A muscular pump with four chambers.", "चार कक्षों वाला मांसपेशियों का पंप।"),
            t("Between the lungs, behind the breastbone; much of it lies left of the midline.", "फेफड़ों के बीच, उरोस्थि के पीछे; इसका अधिक भाग मध्य रेखा के बाईं ओर होता है।"),
            t("Pumps blood to the lungs for gas exchange and to the body to deliver oxygen and nutrients.", "गैसों के आदान-प्रदान के लिए रक्त फेफड़ों तक और ऑक्सीजन तथा पोषक तत्व पहुँचाने के लिए शरीर तक भेजता है।"),
            "https://www.nhlbi.nih.gov/health/heart/anatomy")
        one("FMA3734", t("Aorta", "महाधमनी"),
            t("The body's largest artery, carrying blood away from the heart.", "शरीर की सबसे बड़ी धमनी, जो रक्त को हृदय से बाहर ले जाती है।"),
            t("Starts at the left ventricle, arches in the chest and continues down through the trunk.", "बाएँ निलय से शुरू होकर छाती में मुड़ती है और धड़ में नीचे जाती है।"),
            t("Supplies oxygen-rich blood through branches to the body's tissues.", "इसकी शाखाएँ शरीर के ऊतकों तक ऑक्सीजन वाला रक्त पहुँचाती हैं।"),
            "https://www.nhlbi.nih.gov/health/heart/blood-flow")
        one("FMA50801", t("Brain", "मस्तिष्क"),
            t("The main processing organ of the nervous system.", "तंत्रिका तंत्र का मुख्य सूचना-संसाधन अंग।"),
            t("Inside the skull, connected to the spinal cord.", "खोपड़ी के अंदर, मेरुरज्जु से जुड़ा हुआ।"),
            t("Processes senses and supports thought, memory, movement and vital body functions.", "इंद्रियों की सूचना समझता है; सोच, स्मृति, गति और शरीर के आवश्यक कार्यों को नियंत्रित करता है।"),
            OPEN + "13-2-the-central-nervous-system")
        one("FMA7647", t("Spinal cord", "मेरुरज्जु"),
            t("A cord of nervous tissue connecting the brain with spinal nerves.", "तंत्रिका ऊतक की रज्जु, जो मस्तिष्क को मेरु तंत्रिकाओं से जोड़ती है।"),
            t("Inside the spinal canal; it is distinct from the bones of the spine.", "रीढ़ की मेरु नलिका के अंदर; यह रीढ़ की हड्डियों से अलग संरचना है।"),
            t("Carries signals between brain and body and supports reflex responses.", "मस्तिष्क और शरीर के बीच संकेत पहुँचाती है और प्रतिवर्ती क्रियाओं में भाग लेती है।"),
            OPEN + "13-2-the-central-nervous-system")
        one("FMA13295", t("Diaphragm", "डायफ्राम · मध्यपट"),
            t("A dome-shaped sheet of muscle used for breathing.", "साँस लेने में काम आने वाली गुंबद जैसी मांसपेशी की चादर।"),
            t("Below the lungs, separating the chest from the abdomen.", "फेफड़ों के नीचे, छाती और पेट को अलग करती है।"),
            t("Flattens as it contracts, increasing chest space to help draw air into the lungs.", "सिकुड़ने पर चपटी होती है, जिससे छाती में जगह बढ़ती है और हवा फेफड़ों में जाती है।"),
            "https://www.nhlbi.nih.gov/health/lungs/body-controls-breathing")
        one("FMA7197", t("Liver", "यकृत · जिगर"),
            t("A large organ that processes nutrients and produces bile.", "बड़ा अंग, जो पोषक तत्वों को संसाधित करता है और पित्त बनाता है।"),
            t("Mostly in the upper right abdomen, below the diaphragm.", "मुख्यतः पेट के ऊपर दाईं ओर, डायफ्राम के नीचे।"),
            t("Stores and processes nutrients; bile helps the intestine digest fats.", "पोषक तत्वों का भंडारण और संसाधन करता है; पित्त आँत में वसा पचाने में मदद करता है।"),
            "https://www.niddk.nih.gov/health-information/digestive-diseases/digestive-system-how-it-works")
        pair("FMA7204", "FMA7205", t("kidney", "गुर्दा"),
            t("One of two bean-shaped organs that filter the blood.", "सेम के बीज जैसे दो अंगों में से एक, जो रक्त छानते हैं।"),
            t("Toward the back of the abdomen, below the ribs, beside the spine.", "पेट के पीछे की ओर, पसलियों के नीचे और रीढ़ के पास।"),
            t("Removes waste and excess water as urine and helps balance salts and body fluids.", "अपशिष्ट और अतिरिक्त पानी मूत्र में निकालता है तथा लवण और शरीर के तरल का संतुलन बनाए रखता है।"),
            "https://www.niddk.nih.gov/health-information/kidney-disease/kidneys-how-they-work")

        pair("FMA13375", "FMA13376", t("pectoralis minor", "पेक्टोरालिस माइनर"),
            t("A small chest muscle beneath the pectoralis major.", "पेक्टोरालिस मेजर के नीचे की छोटी छाती की मांसपेशी।"),
            t("From the upper ribs to the front of the shoulder blade.", "ऊपरी पसलियों से कंधे की हड्डी के आगे के हिस्से तक।"),
            t("Draws the shoulder blade forward and downward and helps hold it steady.", "कंधे की हड्डी को आगे और नीचे खींचती है तथा स्थिर रखती है।"), NCBI + "NBK545241/")
        pair("FMA13398", "FMA13399", t("serratus anterior", "सेराटस एंटीरियर"),
            t("A rib-side muscle that controls the shoulder blade.", "पसलियों के किनारे की मांसपेशी, जो कंधे की हड्डी को नियंत्रित करती है।"),
            t("From the side of the upper ribs to the inner surface of the shoulder blade.", "ऊपरी पसलियों के किनारे से कंधे की हड्डी की अंदरूनी सतह तक।"),
            t("Pulls the shoulder blade forward, holds it against the chest and helps turn it upward.", "कंधे की हड्डी को आगे खींचती है, छाती से सटाए रखती है और ऊपर घुमाने में मदद करती है।"), NCBI + "NBK537148/")
        pair("FMA33586", "FMA33587", t("trapezius · upper part", "ट्रेपेज़ियस · ऊपरी भाग"),
            t("The descending upper fibres of the broad trapezius muscle.", "चौड़ी ट्रेपेज़ियस मांसपेशी के नीचे की ओर आते ऊपरी तंतु।"),
            t("From the back of the head and neck toward the shoulder.", "सिर और गर्दन के पीछे से कंधे की ओर।"),
            t("Raises the shoulder blade and works with other fibres to rotate it upward.", "कंधे की हड्डी को उठाता है और अन्य तंतुओं के साथ उसे ऊपर घुमाता है।"), NCBI + "NBK518994/")
        pair("FMA33584", "FMA33585", t("trapezius · middle part", "ट्रेपेज़ियस · मध्य भाग"),
            t("The transverse middle fibres of the trapezius muscle.", "ट्रेपेज़ियस मांसपेशी के आड़े जाने वाले मध्य तंतु।"),
            t("Across the upper back toward the spine of the shoulder blade.", "ऊपरी पीठ से कंधे की हड्डी की उभरी पट्टी की ओर।"),
            t("Pulls the shoulder blade toward the spine and helps steady it.", "कंधे की हड्डी को रीढ़ की ओर खींचता है और स्थिर रखता है।"), NCBI + "NBK518994/")
        pair("FMA33581", "FMA33583", t("trapezius · lower part", "ट्रेपेज़ियस · निचला भाग"),
            t("The ascending lower fibres of the trapezius muscle.", "ट्रेपेज़ियस मांसपेशी के ऊपर की ओर जाते निचले तंतु।"),
            t("From the lower thoracic back toward the shoulder blade.", "छाती के पीछे के निचले हिस्से से कंधे की हड्डी की ओर।"),
            t("Lowers the shoulder blade and helps rotate it upward with the upper fibres.", "कंधे की हड्डी को नीचे खींचता है और ऊपरी तंतुओं के साथ उसे ऊपर घुमाता है।"), NCBI + "NBK518994/")
        pair("FMA34680", "FMA34681", t("deltoid · front part", "डेल्टॉइड · आगे का भाग"),
            t("The clavicular, or front, portion of the shoulder's deltoid muscle.", "कंधे की डेल्टॉइड मांसपेशी का हंसली से जुड़ा आगे का भाग।"),
            t("At the front of the shoulder, from collarbone to upper-arm bone.", "कंधे के सामने, हंसली से ऊपरी बाँह की हड्डी तक।"),
            t("Raises the arm forward and helps turn it inward.", "बाँह को आगे उठाता है और अंदर घुमाने में मदद करता है।"), NCBI + "NBK537056/")
        pair("FMA34682", "FMA34683", t("deltoid · middle part", "डेल्टॉइड · मध्य भाग"),
            t("The acromial, or middle, portion of the deltoid muscle.", "डेल्टॉइड मांसपेशी का एक्रोमियन से जुड़ा मध्य भाग।"),
            t("Over the outer shoulder, from the shoulder tip to the upper-arm bone.", "कंधे के बाहरी हिस्से पर, कंधे के सिरे से ऊपरी बाँह की हड्डी तक।"),
            t("Lifts the arm out to the side.", "बाँह को शरीर से दूर बगल की ओर उठाता है।"), NCBI + "NBK537056/")
        pair("FMA34684", "FMA34685", t("deltoid · back part", "डेल्टॉइड · पीछे का भाग"),
            t("The spinal, or rear, portion of the deltoid muscle.", "डेल्टॉइड मांसपेशी का कंधे की हड्डी की पट्टी से जुड़ा पिछला भाग।"),
            t("At the back of the shoulder, from shoulder blade to upper-arm bone.", "कंधे के पीछे, कंधे की हड्डी से ऊपरी बाँह की हड्डी तक।"),
            t("Moves the arm backward and helps turn it outward.", "बाँह को पीछे ले जाता है और बाहर घुमाने में मदद करता है।"), NCBI + "NBK537056/")
        pair("FMA34690", "FMA34691", t("pectoralis major · upper part", "पेक्टोरालिस मेजर · ऊपरी भाग"),
            t("The collarbone portion of the large chest muscle.", "छाती की बड़ी मांसपेशी का हंसली से जुड़ा भाग।"),
            t("Upper front chest, from collarbone toward upper arm.", "छाती के ऊपर सामने, हंसली से ऊपरी बाँह की ओर।"),
            t("Helps raise the arm forward and draw it across the chest.", "बाँह को आगे उठाने और छाती के सामने लाने में मदद करता है।"), NCBI + "NBK545241/")
        pair("FMA79979", "FMA79980", t("pectoralis major · chest part", "पेक्टोरालिस मेजर · उरोस्थि-पसली भाग"),
            t("The breastbone-and-rib portion of the large chest muscle.", "छाती की बड़ी मांसपेशी का उरोस्थि और पसलियों से जुड़ा भाग।"),
            t("Across the front chest, from breastbone and rib cartilage toward upper arm.", "छाती के सामने, उरोस्थि और पसलियों की उपास्थि से ऊपरी बाँह की ओर।"),
            t("Draws the arm toward the body and turns it inward.", "बाँह को शरीर की ओर लाता है और अंदर घुमाता है।"), UPPER)
        pair("FMA45874", "FMA45875", t("pectoralis major · lower part", "पेक्टोरालिस मेजर · निचला भाग"),
            t("The lower, abdominal portion of the large chest muscle.", "छाती की बड़ी मांसपेशी का निचला, पेट की ओर वाला भाग।"),
            t("At the lower edge of the chest muscle, connected with abdominal-wall tissue.", "छाती की मांसपेशी के निचले किनारे पर, पेट की दीवार के ऊतक से जुड़ा हुआ।"),
            t("Works with the chest muscle to draw the arm inward.", "छाती की मांसपेशी के साथ बाँह को अंदर लाने में काम करता है।"), NCBI + "NBK545241/")
        pair("FMA37684", "FMA37685", t("biceps brachii · short head", "बाइसेप्स ब्रेकिआई · छोटा सिरा"),
            t("One of the two heads of the biceps at the front of the upper arm.", "ऊपरी बाँह के सामने की बाइसेप्स मांसपेशी के दो सिरों में से एक।"),
            t("Starts at the coracoid projection on the front of the shoulder blade.", "कंधे की हड्डी के सामने के कोरैकॉइड उभार से शुरू होता है।"),
            t("With the long head, bends the elbow and turns the forearm palm-up.", "लंबे सिरे के साथ कोहनी मोड़ता है और अग्रबाहु घुमाकर हथेली ऊपर करता है।"), NCBI + "NBK482410/")
        pair("FMA37686", "FMA37687", t("biceps brachii · long head", "बाइसेप्स ब्रेकिआई · लंबा सिरा"),
            t("The biceps head whose tendon begins above the shoulder socket.", "बाइसेप्स का वह सिरा, जिसका कंडरा कंधे के जोड़ के गड्ढे के ऊपर शुरू होता है।"),
            t("At the front of the upper arm, beside the short head.", "ऊपरी बाँह के सामने, छोटे सिरे के पास।"),
            t("With the short head, bends the elbow and turns the forearm palm-up.", "छोटे सिरे के साथ कोहनी मोड़ता है और अग्रबाहु घुमाकर हथेली ऊपर करता है।"), NCBI + "NBK482410/")
        pair("FMA37695", "FMA37696", t("triceps brachii · medial head", "ट्राइसेप्स ब्रेकिआई · अंदरूनी सिरा"),
            t("The deeper head of the three-headed muscle behind the upper arm.", "ऊपरी बाँह के पीछे की तीन सिरों वाली मांसपेशी का गहरा सिरा।"),
            t("On the back of the humerus, mostly beneath the other triceps heads.", "ह्यूमरस के पीछे, अधिकतर ट्राइसेप्स के दूसरे सिरों के नीचे।"),
            t("Works with the other heads to straighten the elbow.", "अन्य सिरों के साथ कोहनी सीधी करता है।"), NCBI + "NBK536996/")
        pair("FMA37697", "FMA37698", t("triceps brachii · lateral head", "ट्राइसेप्स ब्रेकिआई · बाहरी सिरा"),
            t("The outer head of the muscle behind the upper arm.", "ऊपरी बाँह के पीछे की मांसपेशी का बाहरी सिरा।"),
            t("On the outer back surface of the humerus.", "ह्यूमरस की बाहरी पिछली सतह पर।"),
            t("Works with the other heads to straighten the elbow.", "अन्य सिरों के साथ कोहनी सीधी करता है।"), NCBI + "NBK536996/")
        pair("FMA37699", "FMA37700", t("triceps brachii · long head", "ट्राइसेप्स ब्रेकिआई · लंबा सिरा"),
            t("The triceps head that crosses both the shoulder and elbow.", "ट्राइसेप्स का वह सिरा जो कंधे और कोहनी, दोनों जोड़ों को पार करता है।"),
            t("From below the shoulder socket down the back of the upper arm.", "कंधे के जोड़ के गड्ढे के नीचे से ऊपरी बाँह के पीछे नीचे तक।"),
            t("Straightens the elbow and assists backward movement of the arm.", "कोहनी सीधी करता है और बाँह को पीछे ले जाने में मदद करता है।"), NCBI + "NBK536996/")
        pair("FMA32544", "FMA32545", t("supraspinatus", "सुप्रास्पाइनेटस"),
            t("A small rotator-cuff muscle at the top of the shoulder blade.", "कंधे की हड्डी के ऊपरी हिस्से की छोटी रोटेटर-कफ मांसपेशी।"),
            t("Above the ridge on the back of the shoulder blade, passing under the shoulder tip.", "कंधे की हड्डी की पिछली उभरी पट्टी के ऊपर, कंधे के सिरे के नीचे से गुजरती है।"),
            t("Helps begin lifting the arm sideways and steadies the shoulder joint.", "बाँह को बगल में उठाना शुरू करने में मदद करती है और कंधे का जोड़ स्थिर रखती है।"), NCBI + "NBK537148/")
        pair("FMA32547", "FMA32548", t("infraspinatus", "इन्फ्रास्पाइनेटस"),
            t("A broad rotator-cuff muscle behind the shoulder.", "कंधे के पीछे की चौड़ी रोटेटर-कफ मांसपेशी।"),
            t("Below the ridge on the back of the shoulder blade.", "कंधे की हड्डी की पिछली उभरी पट्टी के नीचे।"),
            t("Turns the upper arm outward and helps keep it centred in the shoulder socket.", "ऊपरी बाँह को बाहर घुमाती है और कंधे के जोड़ में स्थिर रखने में मदद करती है।"), NCBI + "NBK513255/")
        pair("FMA32551", "FMA32552", t("teres major", "टेरीज़ मेजर"),
            t("A shoulder muscle near the lower outer edge of the shoulder blade.", "कंधे की हड्डी के निचले बाहरी किनारे के पास की मांसपेशी।"),
            t("From the lower shoulder blade toward the upper-arm bone.", "कंधे की हड्डी के निचले हिस्से से ऊपरी बाँह की हड्डी की ओर।"),
            t("Moves the arm backward, toward the body and inward in rotation.", "बाँह को पीछे और शरीर की ओर लाती है तथा अंदर घुमाती है।"), NCBI + "NBK546633/")
        pair("FMA32553", "FMA32554", t("teres minor", "टेरीज़ माइनर"),
            t("A narrow rotator-cuff muscle behind the shoulder.", "कंधे के पीछे की पतली रोटेटर-कफ मांसपेशी।"),
            t("Along the outer edge of the shoulder blade, above teres major.", "कंधे की हड्डी के बाहरी किनारे पर, टेरीज़ मेजर के ऊपर।"),
            t("Turns the upper arm outward and steadies the shoulder joint.", "ऊपरी बाँह को बाहर घुमाती है और कंधे का जोड़ स्थिर रखती है।"), NCBI + "NBK441844/")
        pair("FMA37665", "FMA37666", t("coracobrachialis", "कोरैकोब्रेकिआलिस"),
            t("A small muscle on the inner side of the upper arm.", "ऊपरी बाँह की अंदरूनी ओर की छोटी मांसपेशी।"),
            t("From the front projection of the shoulder blade to the humerus.", "कंधे की हड्डी के आगे के उभार से ह्यूमरस तक।"),
            t("Brings the arm forward and toward the body.", "बाँह को आगे और शरीर की ओर लाती है।"), NCBI + "NBK507841/")
        pair("FMA37668", "FMA37669", t("brachialis", "ब्रेकिआलिस"),
            t("A deep elbow-bending muscle beneath the biceps.", "बाइसेप्स के नीचे की गहरी मांसपेशी, जो कोहनी मोड़ती है।"),
            t("At the front of the lower upper arm, between humerus and ulna.", "ऊपरी बाँह के निचले हिस्से में सामने, ह्यूमरस और अल्ना के बीच।"),
            t("Bends the elbow regardless of which way the palm faces.", "हथेली किसी भी दिशा में हो, कोहनी मोड़ती है।"), NCBI + "NBK507841/")
        pair("FMA38486", "FMA38487", t("brachioradialis", "ब्रेकिओरेडियालिस"),
            t("A superficial forearm muscle on the thumb side.", "अग्रबाहु की सतह के पास, अंगूठे की ओर की मांसपेशी।"),
            t("From the outer lower humerus to the radius near the wrist.", "ह्यूमरस के निचले बाहरी हिस्से से कलाई के पास रेडियस तक।"),
            t("Bends the elbow, especially with the thumb pointing upward.", "कोहनी मोड़ती है, विशेषकर जब अंगूठा ऊपर की ओर हो।"), NCBI + "NBK526110/")
        pair("FMA13408", "FMA13409", t("sternocleidomastoid", "स्टर्नोक्लीडोमास्टॉइड"),
            t("A prominent strap-like muscle on the side of the neck.", "गर्दन के किनारे की पट्टी जैसी उभरी मांसपेशी।"),
            t("From breastbone and collarbone to the skull behind the ear.", "उरोस्थि और हंसली से कान के पीछे खोपड़ी तक।"),
            t("One side turns the face to the opposite side; both sides help bend the neck forward.", "एक तरफ की मांसपेशी चेहरा विपरीत ओर घुमाती है; दोनों मिलकर गर्दन आगे मोड़ने में मदद करती हैं।"), NECK)
        pair("FMA13336", "FMA13337", t("external oblique", "एक्सटर्नल ओब्लीक"),
            t("A broad outer muscle of the abdominal wall.", "पेट की दीवार की बाहरी चौड़ी मांसपेशी।"),
            t("Across the side and front of the abdomen, below the ribs.", "पसलियों के नीचे, पेट के किनारे और सामने।"),
            t("Helps bend and turn the trunk and compress the abdominal contents.", "धड़ को मोड़ने और घुमाने तथा पेट के भीतर दबाव बनाने में मदद करती है।"), TRUNK)

        pair("FMA22328", "FMA22329", t("gluteus maximus", "ग्लूटिअस मैक्सिमस"),
            t("The large, outer muscle of the buttock.", "नितंब की बड़ी बाहरी मांसपेशी।"),
            t("Behind the hip, over the deeper gluteal muscles.", "कूल्हे के पीछे, गहरी ग्लूटिअल मांसपेशियों के ऊपर।"),
            t("Extends the hip, helping you stand from a chair or climb stairs.", "कूल्हे को सीधा करती है; कुर्सी से उठने और सीढ़ियाँ चढ़ने में मदद करती है।"), NCBI + "NBK538193/")
        pair("FMA22330", "FMA22331", t("gluteus medius", "ग्लूटिअस मीडियस"),
            t("A fan-shaped muscle on the outer hip.", "कूल्हे की बाहरी ओर पंखे जैसी मांसपेशी।"),
            t("Between the outer pelvis and the upper femur, partly beneath gluteus maximus.", "श्रोणि की बाहरी सतह और फीमर के ऊपरी हिस्से के बीच; कुछ भाग ग्लूटिअस मैक्सिमस के नीचे।"),
            t("Moves the leg sideways and keeps the pelvis level when standing on one leg.", "पैर को बगल में ले जाती है और एक पैर पर खड़े होने पर श्रोणि को समतल रखती है।"), NCBI + "NBK557509/")
        pair("FMA22332", "FMA22333", t("gluteus minimus", "ग्लूटिअस मिनिमस"),
            t("A small, deep muscle on the outer hip.", "कूल्हे की बाहरी ओर की छोटी, गहरी मांसपेशी।"),
            t("Beneath gluteus medius, between pelvis and upper femur.", "ग्लूटिअस मीडियस के नीचे, श्रोणि और फीमर के ऊपरी हिस्से के बीच।"),
            t("Helps move the leg sideways and stabilise the pelvis during walking.", "पैर को बगल में ले जाने और चलते समय श्रोणि स्थिर रखने में मदद करती है।"), NCBI + "NBK556144/")
        pair("FMA22354", "FMA22355", t("sartorius", "सार्टोरियस"),
            t("A long, narrow muscle crossing the front of the thigh diagonally.", "जाँघ के सामने तिरछी गुजरने वाली लंबी, पतली मांसपेशी।"),
            t("From the outer front pelvis toward the inner knee.", "श्रोणि के आगे बाहरी हिस्से से घुटने के अंदर की ओर।"),
            t("Helps bend the hip and knee and turn the thigh outward.", "कूल्हा और घुटना मोड़ने तथा जाँघ बाहर घुमाने में मदद करती है।"), NCBI + "NBK532889/")
        pair("FMA38928", "FMA38929", t("rectus femoris", "रेक्टस फेमोरिस"),
            t("The quadriceps muscle that crosses both hip and knee.", "क्वाड्रिसेप्स की वह मांसपेशी जो कूल्हे और घुटने, दोनों जोड़ों को पार करती है।"),
            t("Down the middle of the front thigh, from pelvis toward kneecap.", "जाँघ के सामने बीच में, श्रोणि से घुटने की टोपी की ओर।"),
            t("Straightens the knee and helps raise the thigh forward.", "घुटना सीधा करती है और जाँघ आगे उठाने में मदद करती है।"), NCBI + "NBK500008/")
        pair("FMA38930", "FMA38931", t("vastus lateralis", "वैस्टस लैटरालिस"),
            t("The outer member of the quadriceps muscle group.", "क्वाड्रिसेप्स मांसपेशी समूह का बाहरी हिस्सा।"),
            t("Along the outer front thigh, between femur and kneecap.", "जाँघ के सामने बाहरी ओर, फीमर और घुटने की टोपी के बीच।"),
            t("Straightens the knee with the other quadriceps muscles.", "क्वाड्रिसेप्स की अन्य मांसपेशियों के साथ घुटना सीधा करती है।"), NCBI + "NBK500008/")
        pair("FMA38932", "FMA38933", t("vastus medialis", "वैस्टस मीडियालिस"),
            t("The inner member of the quadriceps muscle group.", "क्वाड्रिसेप्स मांसपेशी समूह का अंदरूनी हिस्सा।"),
            t("Along the inner front thigh, prominent just above the knee.", "जाँघ के सामने अंदर की ओर; घुटने के ठीक ऊपर अधिक उभरी होती है।"),
            t("Straightens the knee and helps guide the kneecap.", "घुटना सीधा करती है और घुटने की टोपी की गति को दिशा देने में मदद करती है।"), NCBI + "NBK500008/")
        pair("FMA38934", "FMA38935", t("vastus intermedius", "वैस्टस इंटरमीडियस"),
            t("The deep middle member of the quadriceps group.", "क्वाड्रिसेप्स समूह की बीच की गहरी मांसपेशी।"),
            t("On the front of the femur, beneath rectus femoris.", "फीमर के सामने, रेक्टस फेमोरिस के नीचे।"),
            t("Straightens the knee with the other quadriceps muscles.", "क्वाड्रिसेप्स की अन्य मांसपेशियों के साथ घुटना सीधा करती है।"), NCBI + "NBK500008/")
        pair("FMA22358", "FMA22359", t("semitendinosus", "सेमिटेंडिनोसस"),
            t("An inner hamstring muscle with a long lower tendon.", "जाँघ के पीछे अंदर की ओर की हैमस्ट्रिंग मांसपेशी, जिसका निचला कंडरा लंबा होता है।"),
            t("From the sitting bone down the inner back of the thigh to the tibia.", "बैठने वाली हड्डी से जाँघ के पीछे अंदर की ओर नीचे टिबिया तक।"),
            t("Bends the knee, extends the hip and helps turn the bent lower leg inward.", "घुटना मोड़ती है, कूल्हा सीधा करती है और मुड़ा निचला पैर अंदर घुमाने में मदद करती है।"), NCBI + "NBK546688/")
        pair("FMA22448", "FMA22449", t("semimembranosus", "सेमिमेम्ब्रेनोसस"),
            t("A broad, deep hamstring muscle on the inner back of the thigh.", "जाँघ के पीछे अंदर की ओर की चौड़ी, गहरी हैमस्ट्रिंग मांसपेशी।"),
            t("Beneath semitendinosus, running from sitting bone to upper tibia.", "सेमिटेंडिनोसस के नीचे, बैठने वाली हड्डी से ऊपरी टिबिया तक।"),
            t("Bends the knee, extends the hip and helps turn the bent lower leg inward.", "घुटना मोड़ती है, कूल्हा सीधा करती है और मुड़ा निचला पैर अंदर घुमाने में मदद करती है।"), NCBI + "NBK546688/")
        pair("FMA45888", "FMA45889", t("biceps femoris · long head", "बाइसेप्स फेमोरिस · लंबा सिरा"),
            t("The outer hamstring head that crosses both hip and knee.", "बाहरी हैमस्ट्रिंग का वह सिरा जो कूल्हे और घुटने, दोनों जोड़ों को पार करता है।"),
            t("From the sitting bone down the outer back thigh toward the fibula.", "बैठने वाली हड्डी से जाँघ के पीछे बाहरी ओर नीचे फिबुला की तरफ।"),
            t("Bends the knee, extends the hip and turns the bent lower leg outward.", "घुटना मोड़ता है, कूल्हा सीधा करता है और मुड़ा निचला पैर बाहर घुमाता है।"), NCBI + "NBK554598/")
        pair("FMA45891", "FMA45892", t("biceps femoris · short head", "बाइसेप्स फेमोरिस · छोटा सिरा"),
            t("The shorter biceps femoris head; it crosses the knee but not the hip.", "बाइसेप्स फेमोरिस का छोटा सिरा; यह घुटना पार करता है, कूल्हे का जोड़ नहीं।"),
            t("From the back of the femur toward the outer knee and fibula.", "फीमर के पीछे से घुटने की बाहरी ओर और फिबुला तक।"),
            t("Bends the knee and turns the bent lower leg outward.", "घुटना मोड़ता है और मुड़ा निचला पैर बाहर घुमाता है।"), NCBI + "NBK554598/")
        pair("FMA22456", "FMA22457", t("adductor longus", "एडक्टर लॉन्गस"),
            t("A triangular muscle of the inner thigh.", "जाँघ की अंदरूनी ओर की त्रिकोण जैसी मांसपेशी।"),
            t("From the pubic part of the pelvis toward the middle of the femur.", "श्रोणि की जघन हड्डी से फीमर के बीच के हिस्से की ओर।"),
            t("Draws the thigh toward the body's midline.", "जाँघ को शरीर की मध्य रेखा की ओर लाती है।"), NCBI + "NBK534775/")
        pair("FMA22459", "FMA22460", t("adductor magnus", "एडक्टर मैग्नस"),
            t("A large inner-thigh muscle with adductor and hamstring portions.", "जाँघ के अंदर की बड़ी मांसपेशी, जिसमें एडक्टर और हैमस्ट्रिंग भाग हैं।"),
            t("Between the lower pelvis and the back-inner surface of the femur.", "श्रोणि के निचले हिस्से और फीमर की अंदरूनी पिछली सतह के बीच।"),
            t("Draws the thigh inward; its hamstring portion helps extend the hip.", "जाँघ अंदर लाती है; इसका हैमस्ट्रिंग भाग कूल्हा सीधा करने में मदद करता है।"), NCBI + "NBK534775/")
        pair("FMA22544", "FMA22545", t("tibialis anterior", "टिबिआलिस एंटीरियर"),
            t("A muscle along the front of the shin.", "पिंडली के सामने की मांसपेशी।"),
            t("Beside the tibia, with a tendon crossing the front of the ankle.", "टिबिया के पास; इसका कंडरा टखने के सामने से गुजरता है।"),
            t("Lifts the front of the foot and turns the sole inward.", "पंजे का आगे का हिस्सा ऊपर उठाती है और तलवा अंदर घुमाती है।"), NCBI + "NBK539705/")
        pair("FMA22558", "FMA22559", t("soleus", "सोलियस"),
            t("A broad calf muscle beneath gastrocnemius.", "गैस्ट्रोक्नीमियस के नीचे पिंडली की चौड़ी मांसपेशी।"),
            t("Behind the lower leg, joining the Achilles tendon at the heel.", "पैर के निचले हिस्से के पीछे, एड़ी की ओर अकिलीस कंडरे से जुड़ती है।"),
            t("Raises the heel and helps maintain standing posture.", "एड़ी उठाती है और खड़े होने का संतुलन बनाए रखने में मदद करती है।"), NCBI + "NBK459362/")
        pair("FMA45957", "FMA45958", t("gastrocnemius · medial head", "गैस्ट्रोक्नीमियस · अंदरूनी सिरा"),
            t("The inner head of the large, two-headed calf muscle.", "पिंडली की बड़ी, दो सिरों वाली मांसपेशी का अंदरूनी सिरा।"),
            t("From the inner back of the knee toward the Achilles tendon.", "घुटने के पीछे अंदर की ओर से अकिलीस कंडरे की तरफ।"),
            t("Raises the heel and assists knee bending.", "एड़ी उठाता है और घुटना मोड़ने में मदद करता है।"), NCBI + "NBK532946/")
        pair("FMA45960", "FMA45961", t("gastrocnemius · lateral head", "गैस्ट्रोक्नीमियस · बाहरी सिरा"),
            t("The outer head of the large, two-headed calf muscle.", "पिंडली की बड़ी, दो सिरों वाली मांसपेशी का बाहरी सिरा।"),
            t("From the outer back of the knee toward the Achilles tendon.", "घुटने के पीछे बाहरी ओर से अकिलीस कंडरे की तरफ।"),
            t("Raises the heel and assists knee bending.", "एड़ी उठाता है और घुटना मोड़ने में मदद करता है।"), NCBI + "NBK532946/")

        // These meshes contain source remainders. Never imply that they identify a single muscle.
        fun group(id: String, title: Text, location: Text, function: Text, source: String) = one(id, title,
            t("Several remaining source muscles share this label; individual muscles are not identified here.",
                "स्रोत की कई बाकी मांसपेशियाँ इस नाम के अंतर्गत हैं; यहाँ उनकी अलग-अलग पहचान उपलब्ध नहीं है।"),
            location, function, source, group = true)
        group("FMA9616", t("Head muscles · grouped", "सिर की मांसपेशियाँ · समूह"),
            t("In the head region; a single selection may include separate surfaces.", "सिर के क्षेत्र में; एक चयन में अलग-अलग सतहें हो सकती हैं।"),
            t("Head muscles support actions such as facial expression and chewing; this group does not isolate one action.", "सिर की मांसपेशियाँ चेहरे के भाव और चबाने जैसे काम करती हैं; यह समूह किसी एक क्रिया को अलग नहीं दिखाता।"), NECK)
        group("FMA9617", t("Neck muscles · grouped", "गर्दन की मांसपेशियाँ · समूह"),
            t("Around the neck; individual muscle boundaries are not labelled in this group.", "गर्दन के आसपास; इस समूह में हर मांसपेशी की सीमा का अलग नाम नहीं है।"),
            t("Neck muscles position and support the head; their individual actions differ.", "गर्दन की मांसपेशियाँ सिर की स्थिति और सहारा बनाए रखती हैं; हर मांसपेशी का काम अलग होता है।"), NECK)
        group("FMA9619", t("Chest muscles · grouped", "छाती की मांसपेशियाँ · समूह"),
            t("Across the chest region; this is a regional collection.", "छाती के क्षेत्र में; यह उस क्षेत्र की कई मांसपेशियों का समूह है।"),
            t("Muscles in this region support chest-wall movement and posture; this group has no single assigned action.", "इस क्षेत्र की मांसपेशियाँ छाती की दीवार की गति और मुद्रा में सहायक हैं; समूह की कोई एक निर्धारित क्रिया नहीं है।"), TRUNK)
        group("FMA9621", t("Arm and hand muscles · grouped", "बाँह और हाथ की मांसपेशियाँ · समूह"),
            t("Across the upper limbs; use individually labelled muscles for a precise selection.", "ऊपरी अंगों में; सटीक पहचान के लिए अलग नाम वाली मांसपेशी चुनें।"),
            t("Upper-limb muscles move and stabilise the arms and hands; different muscles perform different actions.", "ऊपरी अंगों की मांसपेशियाँ बाँह और हाथ चलाती तथा स्थिर रखती हैं; अलग मांसपेशियों के काम अलग हैं।"), UPPER)
        group("FMA9622", t("Leg and foot muscles · grouped", "पैर और पंजे की मांसपेशियाँ · समूह"),
            t("Across the lower limbs; use individually labelled muscles for a precise selection.", "निचले अंगों में; सटीक पहचान के लिए अलग नाम वाली मांसपेशी चुनें।"),
            t("Lower-limb muscles support standing and movement; this group is not one muscle with one action.", "निचले अंगों की मांसपेशियाँ खड़े होने और चलने में सहायक हैं; यह एक क्रिया वाली अकेली मांसपेशी नहीं है।"), LOWER)
        group("FMA19086", t("Pelvic muscles · grouped", "श्रोणि की मांसपेशियाँ · समूह"),
            t("In the pelvis region; these source surfaces are not individually labelled.", "श्रोणि के क्षेत्र में; स्रोत की इन सतहों के अलग-अलग नाम उपलब्ध नहीं हैं।"),
            t("This label is a regional grouping and cannot establish one specific muscle function.", "यह नाम एक क्षेत्र के समूह का है; इससे किसी एक मांसपेशी का विशिष्ट काम तय नहीं किया जा सकता।"), LOWER)
        group("FMA22594", t("Spinal muscles · grouped", "रीढ़ के आसपास की मांसपेशियाँ · समूह"),
            t("Around the vertebral column; several muscles can be included.", "कशेरुका स्तंभ के आसपास; इसमें कई मांसपेशियाँ हो सकती हैं।"),
            t("Back muscles help support and move the spine; this collection does not distinguish individual actions.", "पीठ की मांसपेशियाँ रीढ़ को सहारा और गति देती हैं; इस समूह में अलग-अलग क्रियाओं की पहचान नहीं है।"), NECK)
    }

    val knownIds: Set<String> get() = entries.keys

    fun info(partId: String, language: String = "en"): AnatomyInfo? {
        val requested = language.substringBefore('-').substringBefore('_').lowercase(java.util.Locale.ROOT)
        return entries[partId]?.localize(if (requested == "hi") "hi" else "en")
    }
}
