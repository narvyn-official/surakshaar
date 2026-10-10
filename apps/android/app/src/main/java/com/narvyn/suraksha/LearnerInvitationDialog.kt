package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** Generates a private code for deliberate manual sharing; never sends a message. */
internal object LearnerInvitationDialog {
    fun show(host:Activity,worker:JSONObject):AlertDialog=InvitationSheet(host,worker).show()
}
private class InvitationSheet(host:Activity,private val worker:JSONObject):LearningAccountSheet(host,null,"Learner enrolment") {
    override fun initial(){request("/api/learners"){data->
        reset();text(worker.getString("name"));text(worker.getString("id"),true)
        val link=data.getJSONArray("links").objects().find{it.getString("worker_id")==worker.getString("id")}
        if(link!=null){text(link.getString("email"));text(if(link.isNull("linked_at"))t("Invitation pending","आमंत्रण लंबित")else t("Learner account linked","शिक्षार्थी खाता जुड़ा है"))}
        if(link==null||link.isNull("linked_at")){
            val email=field(t("Learner's account email","शिक्षार्थी खाते का ईमेल"),254).apply{inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;setSingleLine();setText(link?.optString("email")?:"")}
            text(t("Creating a code replaces an earlier pending invitation. No email is sent. The learner must sign in with this email and confirm enrolment on the matching phone profile.","कोड बनाने से पुराना लंबित निमंत्रण बदल जाएगा। ईमेल नहीं भेजा जाता। शिक्षार्थी को इसी ईमेल से प्रवेश करके सही फ़ोन प्रोफ़ाइल में नामांकन की पुष्टि करनी होगी।"),true)
            button(t("Create private invitation","निजी आमंत्रण बनाएँ"),true){val address=email.text.toString().trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.error=t("Enter a valid email","सही ईमेल दें");return@button};request("/api/learners",JSONObject().put("action","invite").put("workerId",worker.getString("id")).put("email",address)){reply->
                reset();text(t("Invitation ready · no email sent","आमंत्रण तैयार · ईमेल नहीं भेजा गया"));text(address);text(t("Expires: ","समाप्ति: ")+DateFormat.getDateTimeInstance().format(Date(reply.getLong("expiresAt"))))
                text(t("Share only with this invited learner. This code does not grant educator or certification access.","केवल आमंत्रित शिक्षार्थी को दें। यह कोड प्रशिक्षक या प्रमाणन का अधिकार नहीं देता।"),true)
                body.add(host.label(reply.getString("invitation"),14f).apply{setTextIsSelectable(true)},bottom=10)
                button(t("Share invitation link","आमंत्रण लिंक साझा करें")){share(reply.getString("url"),t("Private learner invitation","निजी शिक्षार्थी आमंत्रण"))}
                button(t("Back","वापस")){initial()}
            }}
        }
        if(link!=null)button(if(link.isNull("linked_at"))t("Revoke invitation","आमंत्रण रद्द करें")else t("Unlink learner account","शिक्षार्थी खाता अलग करें")){confirm(t("Remove this centre link?","यह केंद्र संबंध हटाएँ?"),t("The learner loses centre access. Existing records remain retained, but private practice is not reassigned to another account.","शिक्षार्थी का केंद्र प्रवेश हटेगा। मौजूदा रिकॉर्ड रहेंगे; निजी अभ्यास दूसरे खाते को नहीं दिया जाएगा।")){request("/api/learners",JSONObject().put("action","unlink").put("workerId",worker.getString("id"))){initial()}}}
    }}
}
