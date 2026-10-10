package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject

internal object CoachReadinessDialog {
    fun show(host:Activity):AlertDialog=CoachReadinessSheet(host).show()
}
private class CoachReadinessSheet(host:Activity):LearningAccountSheet(host,null,"AI service readiness") {
    override fun initial(){load()}
    private fun load(){request("/api/tutor/readiness"){data->
        reset();val configured=data.getString("configuration")=="configured"
        text(if(configured)t("Provider configured · connection not tested","प्रदाता कॉन्फ़िगर है · कनेक्शन नहीं जाँचा गया")else t("External AI is unavailable","बाहरी AI उपलब्ध नहीं है"))
        text(t("Authored offline guidance remains available. This service only selects existing control explanations; it cannot diagnose, score or certify.","लिखित ऑफ़लाइन मार्गदर्शन उपलब्ध है। सेवा केवल मौजूदा नियंत्रण स्पष्टीकरण चुनती है; यह निदान, अंक या प्रमाणन नहीं करती।"),true)
        text(t("Reviewed scenarios: ${data.optInt("reviewedScenarios")} / ${data.optInt("scenarioCount")}","समीक्षा किए गए पाठ: ${data.optInt("reviewedScenarios")} / ${data.optInt("scenarioCount")}"))
        text(data.getString("contentVersion"),true)
        if(!configured){text(t("Ask the service operator to complete the backend provider configuration. No credentials are entered or shown in this app.","सेवा संचालक से बैकएंड प्रदाता कॉन्फ़िगरेशन पूरा करने को कहें। ऐप में क्रेडेंशियल दर्ज या दिखाए नहीं जाते।"),true)}
        data.optJSONObject("disclosure")?.let{disclosure->text(disclosure.getString("name"));text(disclosure.getString("dataUse"),true);text(disclosure.getString("privacyUrl"),true)}
        if(configured)button(t("Test provider connection","प्रदाता कनेक्शन जाँचें"),true){confirm(t("Send a synthetic test?","कृत्रिम परीक्षण भेजें?"),t("One controls-only request is sent to the configured provider and may count toward its usage. No learner record, answer, camera or voice is sent. A successful test is not safety approval.","कॉन्फ़िगर प्रदाता को एक नियंत्रण संबंधी परीक्षण भेजा जाएगा, जो उपयोग में गिना जा सकता है। शिक्षार्थी रिकॉर्ड, उत्तर, कैमरा या आवाज़ नहीं भेजी जाती। सफल परीक्षण सुरक्षा स्वीकृति नहीं है।")){
            request("/api/tutor/readiness",JSONObject().put("action","probe").put("confirmExternalRequest",true)){result->reset();text(if(result.optString("status")=="passed")t("Provider contract test passed","प्रदाता अनुबंध परीक्षण सफल")else t("Provider unavailable · authored guidance retained","प्रदाता उपलब्ध नहीं · लिखित मार्गदर्शन सुरक्षित"));text("${result.optLong("elapsedMs")} ms",true);text(t("Synthetic controls only. No clinical or fire-instructor approval.","केवल कृत्रिम नियंत्रण परीक्षण। चिकित्सकीय या अग्नि प्रशिक्षक की मंज़ूरी नहीं।"),true);button(t("Back to readiness","तैयारी स्थिति पर वापस")){load()}}
        }}
        button(t("Refresh configuration","कॉन्फ़िगरेशन ताज़ा करें")){load()}
    }}
}
