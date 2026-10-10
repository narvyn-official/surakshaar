package com.narvyn.suraksha

/** Frozen v1 authored situations. They describe virtual equipment, never diagnose a real fire/unit. */
object FireRoomScenarioCatalog {
    const val DEFAULT_ID = "inspection-and-withdrawal"
    const val DAMAGED_ID = "damaged-unit-evacuation"
    const val DEFINITION_VERSION = 1
    const val CHECKED_ON = "2026-10-10"
    const val USE_SOURCE = "https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/use"
    const val CONDITION_SOURCE = "https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/"
    const val MANUFACTURER_SOURCE = "https://brand.kidde.com/m/f7fe5f74821aca5/original/Rechargeable-FX-User-Manual.pdf"
    const val ROUTE_SOURCE = "https://www.osha.gov/etools/evacuation-plans-procedures/eap/elements"

    data class Scenario(val id:String, val equipmentCondition:String, val title:String, val titleHi:String,
                        val summary:String, val summaryHi:String) {
        val definitionVersion = DEFINITION_VERSION
        val reviewStatus = "draft"
        val checkedOn = CHECKED_ON
        val sources = listOf(USE_SOURCE, CONDITION_SOURCE, MANUFACTURER_SOURCE, ROUTE_SOURCE)
        fun title(hi:Boolean) = if(hi)titleHi else title
        fun summary(hi:Boolean) = if(hi)summaryHi else summary
        val phases:List<String> get() = listOf("ALARM", "EXIT", "EQUIPMENT", "LABEL_CHECK") +
            (if(equipmentCondition=="damaged")listOf("DAMAGED_UNIT") else listOf("CONDITION_CHECK", "HOSE_CHECK")) +
            listOf("PIN", "AIM", "SWEEP", "WITHDRAW", "ROUTE_RECHECK", "ASSEMBLY", "REPORT", "FIRE_HANDOVER", "COMPLETE")
    }

    val all = listOf(
        Scenario(DEFAULT_ID, "good", "Inspect, respond, withdraw", "जाँचें, प्रतिक्रिया दें, हटें",
            "Adult authorised-role drill: a small, contained paper/wood fire; alarm and emergency call are part of the scenario. Inspect the labelled dry-powder unit, then stop when conditions worsen and choose the clear alternate exit. No live fire, real discharge or authorisation is involved.",
            "वयस्क अधिकृत भूमिका का अभ्यास: कागज़/लकड़ी की छोटी सीमित आग; अलार्म और आपात कॉल दृश्य का हिस्सा हैं। लेबल वाले शुष्क पाउडर उपकरण की जाँच करें; स्थिति बिगड़ने पर रुकें और खुला वैकल्पिक निकास चुनें। यह वास्तविक आग, डिस्चार्ज या अनुमति नहीं है।"),
        Scenario(DAMAGED_ID, "damaged", "Reject a damaged unit", "खराब उपकरण को अस्वीकार करें",
            "The same authored paper/wood situation, but the extinguisher has a low pressure indication and split hose. Refuse it, evacuate through the clear alternative and report the defect from assembly. Do not search for another extinguisher while the fire develops.",
            "उसी काल्पनिक कागज़/लकड़ी की स्थिति में अग्निशामक का दबाव कम और होज़ फटी है। उपकरण न लें, खुले वैकल्पिक मार्ग से निकलें और एकत्र स्थल से खराबी बताएँ। आग बढ़ते समय दूसरा अग्निशामक खोजने न रुकें।")
    )
    fun find(id:String):Scenario? = all.firstOrNull { it.id==id }
}
