package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** Only server-authorised object destinations are opened; no arbitrary URL routing. */
internal object LearningInboxDialog {
    fun show(host:Activity,store:Store,owner:String?=null):AlertDialog=InboxSheet(host,store,owner).show()
    fun showEducator(host:Activity):AlertDialog=InboxSheet(host,null,null).show()
}

private class InboxSheet(host:Activity,store:Store?,private var owner:String?):LearningAccountSheet(host,store,"Notifications") {
    private var dismissed=false
    private val scope get()=if(store==null)"educator"else"learner"
    private fun payload(id:String,action:String)=JSONObject().put("scope",scope).put("itemId",id).put("action",action).also{store?.let{s->it.put("workerId",s.workerId).put("owner",owner)}}
    private fun label(kind:String)=when(kind){
        "assignment"->t("New assignment","नया अभ्यास कार्य")
        "cancelled"->t("Assignment cancelled","कार्य रद्द हुआ")
        "submission"->t("Practice ready for review","अभ्यास समीक्षा के लिए तैयार है")
        "review"->if(store==null)t("Correction requested","सुधार का अनुरोध")else t("Educator feedback","प्रशिक्षक की प्रतिक्रिया")
        "observation"->t("Learning observation","सीखने का निरीक्षण")
        else->t("Learning update","सीखने की सूचना")
    }
    override fun initial(){if(store!=null)chooseCentre(owner){id,_->owner=id;load()}else load()}
    private fun load(){request("/api/notifications?scope=$scope&showDismissed=$dismissed"+(store?.let{"&workerId=${it.workerId}&owner=${android.net.Uri.encode(owner)}"}?:"")){reply->
        reset();text(t("Learning notifications","सीखने की सूचनाएँ"));text(t("${reply.optInt("loadedUnread")} unread in this view · in-app only","इस दृश्य में ${reply.optInt("loadedUnread")} अपठित · केवल ऐप में"),true)
        val rows=reply.getJSONArray("items").objects()
        if(rows.isEmpty())text(t("No notifications here.","यहाँ कोई सूचना नहीं है।"))
        rows.forEach{row->val state=if(!row.isNull("dismissed_at"))t("Dismissed","हटाई गई")else if(row.isNull("read_at"))t("Unread","अपठित")else t("Read","पढ़ी गई");button("${label(row.getString("kind"))} · $state\n${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(row.getLong("created_at")))}"){detail(row)}}
        if(reply.optBoolean("truncated"))text(t("Showing the latest 100 updates. Older records remain in assignment history.","नवीनतम 100 सूचनाएँ दिखाई गई हैं। पुराने रिकॉर्ड अभ्यास इतिहास में हैं।"),true)
        button(if(dismissed)t("Hide dismissed","हटाई गई छिपाएँ")else t("Include dismissed","हटाई गई भी दिखाएँ")){dismissed=!dismissed;load()}
        if(store!=null)button(t("Change institution","संस्था बदलें")){chooseCentre{selected,_->owner=selected;load()}}
        button(t("Refresh","ताज़ा करें")){load()}
    }}
    private fun detail(row:JSONObject){reset();val id=row.getString("id");text(label(row.getString("kind")));text(t("Your access is checked again when you open the assignment.","कार्य खोलते समय आपकी अनुमति दोबारा जाँची जाएगी।"),true)
        button(t("Open assignment","अभ्यास कार्य खोलें"),true){request("/api/notifications",payload(id,"open")){reply->
            val destination=reply.getJSONObject("destination");val assignment=destination.getString("assignmentId")
            require(assignment.matches(Regex("[a-fA-F0-9-]{36}")))
            if(store==null){require(destination.getString("screen")=="educator-assignment");dialog.dismiss();LearningCentreDialog.showEducator(host,assignment)}
            else{require(destination.getString("screen")=="learner-assignment"&&destination.getString("workerId")==store.workerId&&destination.getString("owner")==owner);dialog.dismiss();LearningCentreDialog.showLearner(host,store,assignment,owner)}
        }}
        button(if(row.isNull("read_at"))t("Mark read","पढ़ा हुआ करें")else t("Mark unread","अपठित करें")){request("/api/notifications",payload(id,if(row.isNull("read_at"))"read"else"unread")){load()}}
        button(if(row.isNull("dismissed_at"))t("Dismiss notification","सूचना हटाएँ")else t("Restore notification","सूचना वापस लाएँ")){request("/api/notifications",payload(id,if(row.isNull("dismissed_at"))"dismiss"else"restore")){load()}}
        button(t("Back to notifications","सूचनाओं पर वापस")){load()}
    }
}
