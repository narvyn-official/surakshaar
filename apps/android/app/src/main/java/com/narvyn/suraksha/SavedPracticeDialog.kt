package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.UUID
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** Read-only retrieval. Opening a server copy never overwrites newer local practice. */
internal object SavedPracticeDialog {
    fun show(host:Activity,store:Store):AlertDialog=SavedPracticeSheet(host,store).show()
}
private class SavedPracticeSheet(host:Activity,store:Store):LearningAccountSheet(host,store,"Saved private practice") {
    private fun localized(value:JSONObject,key:String)=if(hi)value.optString(key+"Hi",value.optString(key))else value.optString(key)
    override fun initial(){load()}
    private fun load(cursor:JSONObject?=null){request("/api/learning-practice?workerId=${store!!.workerId}&limit=10"+(cursor?.let{"&beforeAt=${it.getLong("beforeAt")}&beforeId=${it.getString("beforeId")}"}?:"")){reply->
        require(reply.getString("evidenceType")=="formative-decision-practice"&&!reply.getBoolean("certifiable"));reset()
        text(t("Private copies in this account","इस खाते की निजी प्रतियाँ"));text(t("Read-only account history. Nothing here replaces local progress or shares it with a centre.","केवल पढ़ने वाला खाता इतिहास। इससे स्थानीय प्रगति नहीं बदलती और केंद्र को कुछ साझा नहीं होता।"),true)
        val records=reply.getJSONArray("records").objects();if(records.isEmpty())text(t("No uploaded practice yet.","अभी कोई अपलोड किया अभ्यास नहीं है।"))
        records.forEach{record->require(record.getString("workerId")==store.workerId);button("${record.getString("scenarioId")} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(record.getLong("updatedAt")))}"){detail(record)}}
        reply.optJSONObject("nextCursor")?.let{next->button(t("Earlier copies","पुरानी प्रतियाँ")){load(next)}}
        if(cursor!=null)button(t("Latest copies","नवीनतम प्रतियाँ")){load()}
    }}
    private fun detail(record:JSONObject){reset();val catalog=LearningCatalog(host);val version=record.getString("contentVersion");val scenario=catalog.scenarioForVersion(record.getString("scenarioId"),version)
        val replay=scenario?.let{runCatching{LearningAttempt.restore(record,it,version,store!!.workerId)}.getOrNull()}
        text(scenario?.let{localized(it,"title")}?:record.getString("scenarioId"));text(version,true)
        if(replay==null){text(t("This copy cannot be replayed with the installed content. Its original data remains available for export.","इंस्टॉल पाठ से इस प्रति की जाँच नहीं हो सकी। मूल डेटा निर्यात के लिए उपलब्ध है।"))}
        else{text(t("${replay.index}/${replay.steps.size} stages · ${if(replay.finished)"complete"else"in progress"}","${replay.index}/${replay.steps.size} चरण · ${if(replay.finished)"पूरा"else"जारी"}"));text(t("Practice decisions only; no qualification or clinical skill assessment.","केवल अभ्यास निर्णय; योग्यता या चिकित्सकीय कौशल का आकलन नहीं।"),true)}
        if(record.optBoolean("finished"))button(t("Practice QR sharing","अभ्यास QR साझा करना")){shares(record)}
        button(t("Read event history","क्रिया इतिहास पढ़ें")){events(record,0)}
        button(t("Export private copy","निजी प्रति निर्यात करें")){confirm(t("Export this private record?","निजी रिकॉर्ड निर्यात करें?"),t("The export includes your choices and written reflections. Choose a destination you trust; this is separate from centre sharing.","निर्यात में आपके विकल्प और लिखित विचार होंगे। भरोसेमंद जगह चुनें; यह केंद्र से साझा करने से अलग है।")){share(record.toString(2),t("Private practice copy","निजी अभ्यास प्रति"))}}
        button(t("Back to saved copies","सहेजी प्रतियों पर वापस")){load()}
    }
    private fun shares(record:JSONObject){request("/api/learning-practice/shares?workerId=${store!!.workerId}&attemptId=${record.getString("id")}"){reply->
        reset();text(t("Share anonymous practice metadata","अनाम अभ्यास विवरण साझा करें"));text(t("Anyone with your QR can view the course, exercise, mode, content version and draft review status. No name, learner ID, email, answers or reflection is published. This is not a professional certificate.","QR रखने वाला कोई भी व्यक्ति पाठ्यक्रम, अभ्यास, मोड, पाठ संस्करण और मसौदा स्थिति देख सकता है। नाम, शिक्षार्थी ID, ईमेल, उत्तर या निजी विचार प्रकाशित नहीं होते। यह व्यावसायिक प्रमाणपत्र नहीं है।"),true)
        val requestId=UUID.randomUUID().toString()
        button(t("Create a 7-day share QR","7 दिन का साझा QR बनाएँ"),true){confirm(t("Publish this metadata?","यह विवरण प्रकाशित करें?"),t("This creates a public link accessible to anyone who receives it. You can revoke future access here. Copies already saved by others cannot be recalled. The secret link is shown once.","यह सार्वजनिक लिंक बनाएगा, जिसे पाने वाला कोई भी खोल सकता है। आगे का प्रवेश यहीं वापस ले सकते हैं। पहले सहेजी गई प्रतियाँ वापस नहीं आएँगी। गुप्त लिंक केवल एक बार दिखेगा।")){
            request("/api/learning-practice/shares",JSONObject().put("action","create").put("id",requestId).put("workerId",store.workerId).put("attemptId",record.getString("id")).put("expiresInDays",7).put("confirmPublicMetadata",true)){created->showQr(record,created)}
        }}
        val rows=reply.getJSONArray("shares").objects();if(rows.isEmpty())text(t("No public share links exist for this copy.","इस प्रति का कोई सार्वजनिक लिंक नहीं है।"),true)
        rows.forEach{row->val revoked=!row.isNull("revoked_at");val active=!revoked&&row.getLong("expires_at")>System.currentTimeMillis()
            val statusDate=if(revoked)row.getLong("revoked_at")else row.getLong("expires_at")
            text((if(revoked)t("Revoked · ","वापस लिया गया · ")else if(active)t("Active until ","इस तारीख तक सक्रिय ")else t("Expired ","समय समाप्त "))+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(statusDate)),true)
            if(active)button(t("Revoke this share","यह साझा लिंक वापस लें")){confirm(t("Revoke this link?","यह लिंक वापस लें?"),t("Its QR will stop showing metadata. Your private practice is retained.","QR विवरण दिखाना बंद कर देगा। निजी अभ्यास सुरक्षित रहेगा।")){revoke(record,row.getString("id"))}}
        }
        if(reply.optBoolean("truncated"))text(t("Active links are shown first, followed by recent history (50 total). At most five may be active for a copy.","सक्रिय लिंक पहले, फिर हाल का इतिहास दिखता है (कुल 50)। एक प्रति के अधिकतम पाँच लिंक सक्रिय हो सकते हैं।"),true)
        button(t("Refresh share links","साझा लिंक ताज़ा करें")){shares(record)}
        button(t("Back to private copy","निजी प्रति पर वापस")){detail(record)}
    }}
    private fun revoke(record:JSONObject,id:String){request("/api/learning-practice/shares",JSONObject().put("action","revoke").put("id",id).put("workerId",store!!.workerId).put("attemptId",record.getString("id"))){shares(record)}}
    private fun showQr(record:JSONObject,created:JSONObject){
        require(!created.getBoolean("certifiable"));val path=created.getString("path");require(path.matches(Regex("/practice-record#token=[A-Za-z0-9_-]{43}")));val url=api.base+path
        reset();text(t("Formative practice · 7-day share","सीखने का अभ्यास · 7 दिन का साझा लिंक"));text(created.getJSONObject("record").getString("scenario"))
        val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,640,640);val bitmap=Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888)
        for(y in 0 until 640)for(x in 0 until 640)bitmap.setPixel(x,y,if(matrix[x,y])Color.BLACK else Color.WHITE)
        body.add(ImageView(host).apply{setImageBitmap(bitmap);adjustViewBounds=true;maxHeight=host.dp(240);contentDescription=t("Practice share QR","अभ्यास साझा QR")},bottom=12)
        text(t("Online verification requires access to this account server. Keep the link private; it is shown only on this screen. This QR does not verify your identity or practical skills.","ऑनलाइन जाँच के लिए इस खाता सर्वर का प्रवेश चाहिए। लिंक निजी रखें; यह केवल इस स्क्रीन पर दिखता है। QR पहचान या व्यावहारिक कौशल नहीं जाँचता।"),true)
        button(t("Share link","लिंक साझा करें"),true){share(url,t("Formative practice record","सीखने का अभ्यास रिकॉर्ड"))}
        button(t("Revoke link","लिंक वापस लें")){revoke(record,created.getString("id"))}
        button(t("Manage share links","साझा लिंक प्रबंधित करें")){shares(record)}
    }
    private fun events(record:JSONObject,page:Int){reset();val history=record.getJSONArray("answers").objects();val scenario=LearningCatalog(host).scenarioForVersion(record.getString("scenarioId"),record.getString("contentVersion"));val steps=scenario?.getJSONArray("steps")?.objects().orEmpty()
        history.drop(page*10).take(10).forEach{event->val step=steps.find{it.getString("id")==event.optString("stepId")};val choice=step?.optJSONArray("choices")?.objects()?.find{it.getString("id")==event.optString("choiceId")};text(listOf(event.getString("kind"),step?.let{localized(it,"title")}?:event.optString("stepId"),choice?.let{localized(it,"text")}?:event.optString("choiceId"),event.optString("text")).filter{it.isNotBlank()}.joinToString("\n"))}
        if((page+1)*10<history.size)button(t("Next events","अगली क्रियाएँ")){events(record,page+1)}
        if(page>0)button(t("Previous events","पिछली क्रियाएँ")){events(record,page-1)}
        button(t("Back to copy","प्रति पर वापस")){detail(record)}
    }
}
