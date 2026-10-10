package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** Online institution records remain distinct from local self-directed progress. */
internal object InstitutionRecordsDialog {
    fun show(host: Activity, store: Store) = RecordsSheet(host, store).show()
    fun library(host: Activity, store: Store) = LibrarySheet(host, store).show()
}

private class RecordsSheet(host: Activity, private val profile: Store) : LearningAccountSheet(host, profile, "Institution learning records") {
    override fun initial() {
        request("/api/institution-awards?learner=1&workerId=${Uri.encode(profile.workerId)}") { result ->
            reset()
            text(t("Institution-issued records", "संस्था द्वारा जारी रिकॉर्ड"))
            text(t("These records describe reviewed learning. They are not government accreditation or permission to perform hazardous work.", "ये रिकॉर्ड समीक्षा किए गए सीखने का विवरण हैं। ये सरकारी मान्यता या जोखिम भरा काम करने की अनुमति नहीं हैं।"), true)
            val rows = result.getJSONArray("awards").objects()
            if (rows.isEmpty()) text(t("No institution-issued records for this profile yet.", "इस प्रोफ़ाइल के लिए अभी कोई संस्था रिकॉर्ड नहीं है।"))
            rows.forEach { row ->
                val payload = row.getJSONObject("payload")
                text("${payload.getString("course")} · ${row.getString("status")}")
                text("${payload.getString("issuer")} · ${payload.getString("learner")}", true)
                text(t("Expires: ", "समाप्ति: ") + DateFormat.getDateInstance().format(Date(row.getLong("expires_at"))), true)
                button(t("View signed verification", "हस्ताक्षर सत्यापन देखें")) {
                    val id = row.getString("id")
                    if (runCatching { java.util.UUID.fromString(id) }.isSuccess) host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(api.base + "/verify-learning?id=" + Uri.encode(id))))
                }
            }
            button(t("Refresh records", "रिकॉर्ड रीफ़्रेश करें")) { initial() }
        }
    }
}

private class LibrarySheet(host: Activity, store: Store) : LearningAccountSheet(host, store, "Institution procedure library") {
    override fun initial() = chooseCentre { owner, name ->
        request("/api/institution-library?owner=${Uri.encode(owner)}") { result ->
            reset(); text(name)
            text(t("Only institution-reviewed procedures are listed. Read their equipment, setting and training limits. A connection is required to check withdrawals.", "यहाँ केवल संस्था द्वारा समीक्षा की गई प्रक्रियाएँ हैं। उपकरण, स्थान और प्रशिक्षण की सीमाएँ पढ़ें। वापस ली गई प्रक्रियाओं की जाँच के लिए इंटरनेट चाहिए।"), true)
            val rows = result.getJSONArray("procedures").objects()
            if (rows.isEmpty()) text(t("No approved local procedures are available.", "कोई स्वीकृत स्थानीय प्रक्रिया उपलब्ध नहीं है।"))
            rows.forEach { row -> button(row.getString("title")) { detail(owner, row.getString("id")) } }
        }
    }
    private fun detail(owner: String, id: String) {
        // Re-fetch at opening so withdrawn content is not opened from a stale list.
        request("/api/institution-library?owner=${Uri.encode(owner)}") { result ->
            reset()
            val row: JSONObject? = result.getJSONArray("procedures").objects().find { it.getString("id") == id }
            if (row == null) text(t("This procedure is no longer available.", "यह प्रक्रिया अब उपलब्ध नहीं है।"))
            else { text(row.getString("title")); text(row.getString("body")); text(row.getString("sources"), true); text(row.getString("review_note"), true) }
            button(t("Back to library", "पुस्तकालय में वापस")) { initial() }
        }
    }
}
