package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Calendar
import java.util.UUID

internal object LearningCentreDialog {
    fun showLearner(host:Activity,store:Store,assignmentId:String?=null,owner:String?=null):AlertDialog=CentreSheet(host,store,assignmentId,owner).show()
    fun showEducator(host:Activity,assignmentId:String?=null):AlertDialog=CentreSheet(host,null,assignmentId,null).show()
}

private class CentreSheet(host:Activity,store:Store?,private val initialAssignment:String?,private var owner:String?):LearningAccountSheet(host,store,if(store==null)"Learning workspace"else"My assignments") {
    private val learner get()=store!=null
    private var centreName=""
    private var role="viewer"
    private var actorId=""
    private var workspace=JSONObject()
    private var assignment=JSONObject()
    private fun date(value:Long)=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(java.util.Date(value))
    private fun workerQuery()="workerId=${store!!.workerId}&owner=${android.net.Uri.encode(owner)}"
    override fun initial(){if(learner)chooseCentre(owner){id,name->owner=id;centreName=name;if(initialAssignment!=null)detail(initialAssignment)else list()}else if(initialAssignment!=null)detail(initialAssignment)else list()}
    private fun list(cursor:JSONObject?=null){
        val path=(if(learner)"/api/learning-enrolment?${workerQuery()}"else"/api/learning-centre?")+(cursor?.let{"${if(learner)"&"else""}beforeAt=${it.getLong("beforeAt")}&beforeId=${it.getString("beforeId")}"}?:"")
        request(path){data->workspace=data;role=data.optString("role","viewer");actorId=data.optString("actorId");reset()
            text(t("Fire Safety & First Aid","अग्नि सुरक्षा और प्राथमिक उपचार"));text(t("Draft formative learning. These records do not issue a qualification.","मसौदा अभ्यास। इन रिकॉर्ड से योग्यता प्रमाणपत्र नहीं बनता।"),true)
            if(!learner&&role in listOf("admin","trainer"))button(t("Create assignment","अभ्यास कार्य बनाएँ"),true){create()}
            if(learner){text(centreName,true);button(t("Change institution","संस्था बदलें")){chooseCentre{newOwner,name->owner=newOwner;centreName=name;list()}}}
            if(learner)button(t("Private upload & saved practice","निजी अपलोड और सहेजा अभ्यास")){dialog.dismiss();LearningPracticeSyncDialog.show(host,store!!)}
            button(t("Notifications","सूचनाएँ")){dialog.dismiss();if(learner)LearningInboxDialog.show(host,store!!,owner)else LearningInboxDialog.showEducator(host)}
            val rows=data.getJSONArray("assignments").objects()
            if(rows.isEmpty())text(t("No assignments yet.","अभी कोई अभ्यास कार्य नहीं है।"))
            rows.forEach{row->button("${row.optString("learner_name").takeIf{it.isNotBlank()}?.plus(" · ")?:""}${row.getJSONObject("catalogue").optString("course")} · ${row.optString("status")}\n${row.optInt("completed")}/${row.optInt("required")} · ${date(row.getLong("due_at"))}"){detail(row.getString("id"))}}
            data.optJSONObject("nextCursor")?.let{next->button(t("Earlier assignments","पुराने अभ्यास कार्य")){list(next)}}
            if(cursor!=null)button(t("Latest assignments","नवीनतम अभ्यास कार्य")){list()}
            if(!learner)button(t("Export loaded assignment report","दिख रहे अभ्यास की रिपोर्ट निर्यात करें")){request("/api/learning-centre?format=csv"+(cursor?.let{"&beforeAt=${it.getLong("beforeAt")}&beforeId=${it.getString("beforeId")}"}?:"")){reply->share(reply.getString("csv"),t("Formative learning report","अभ्यास रिपोर्ट"))}}
        }
    }
    private fun detail(id:String,cursor:JSONObject?=null){request((if(learner)"/api/learning-enrolment?${workerQuery()}&assignmentId=$id"else"/api/learning-centre?assignmentId=$id")+(cursor?.let{"&beforeAt=${it.getLong("beforeAt")}&beforeId=${it.getString("beforeId")}"}?:"")){data->
        assignment=data.getJSONObject("assignment");if(!learner){role=data.optString("role","viewer");actorId=data.optString("actorId")};reset()
        val catalog=assignment.getJSONObject("catalogue")
        text(catalog.getString("course"));text("${assignment.getString("status")} · ${assignment.getString("mode")} · ${assignment.getString("audience")}")
        text("${t("Due","अंतिम तारीख")}: ${date(assignment.getLong("due_at"))}\n${assignment.getString("content_version")}",true)
        if(assignment.optString("note").isNotBlank())text(assignment.getString("note"))
        text(catalog.getJSONArray("scenarios").objects().joinToString("\n"){it.getString("title")})
        if(learner&&assignment.isNull("cancelled_at")){
            val installed=LearningCatalog(host)
            if(installed.version==assignment.getString("content_version"))button(t("Start assigned practice","दिया गया अभ्यास शुरू करें"),true){
                val scenes=catalog.getJSONArray("scenarios").objects()
                PageDialogBuilder(host).setTitle(t("Choose a new practice","नया अभ्यास चुनें")).setItems(scenes.map{it.getString("title")}.toTypedArray()){_,index->if(current()){
                    dialog.dismiss();host.startActivity(Intent(host,LearningScenarioActivity::class.java).putExtra("scenarioId",scenes[index].getString("id")).putExtra("newPractice",true).putExtra("guided",assignment.getString("mode")=="guided"))
                }}.show()
            }else text(t("This assignment uses a different content version. Its history remains readable; ask your educator to create a current-version assignment for new practice.","इस कार्य का पाठ संस्करण अलग है। इतिहास पढ़ सकते हैं; नए अभ्यास के लिए प्रशिक्षक से वर्तमान संस्करण का कार्य माँगें।"),true)
        }
        text(t("Practice completion and educator feedback are separate from qualification. School-awareness scope does not record guardian consent.","अभ्यास और प्रशिक्षक प्रतिक्रिया योग्यता से अलग हैं। विद्यालय जागरूकता से अभिभावक की सहमति दर्ज नहीं होती।"),true)
        if(learner&&assignment.isNull("cancelled_at"))button(t("Choose practice to share","साझा करने के लिए अभ्यास चुनें"),true){shareForm(data.getJSONArray("available").objects())}
        val submissions=assignment.getJSONArray("submissions").objects()
        if(submissions.isEmpty())text(t("No practice has been shared for this assignment.","इस कार्य के लिए कोई अभ्यास साझा नहीं हुआ है।"),true)
        submissions.forEach{row->button("${row.getString("scenario_id")} · ${if(row.isNull("withdrawn_at"))t("Shared practice","साझा अभ्यास")else t("Withdrawn","वापस लिया गया")}"){submission(row)}}
        assignment.optJSONObject("nextSubmissionCursor")?.let{next->button(t("Earlier shared practice","पुराना साझा अभ्यास")){detail(id,next)}}
        if(cursor!=null)button(t("Latest shared practice","नवीनतम साझा अभ्यास")){detail(id)}
        assignment.getJSONArray("observations").objects().forEach{row->button("${t("Observation","निरीक्षण")} · ${row.getString("outcome")} · ${date(row.getLong("observed_at"))}"){reset();text(row.getString("setting"));text(row.getString("note"));text(row.getJSONObject("rubric").toString());text(t("Educator-entered training observation. It does not certify practical competence.","प्रशिक्षक द्वारा दर्ज प्रशिक्षण निरीक्षण। यह व्यावहारिक योग्यता का प्रमाण नहीं है।"),true);button(t("Back to assignment","कार्य पर वापस")){detail(id)}}}
        if(!learner&&role in listOf("admin","trainer")&&assignment.isNull("cancelled_at")){
            button(t("Record observation","निरीक्षण दर्ज करें")){observation()}
            button(t("Cancel assignment","कार्य रद्द करें")){reset();val reason=field(t("Reason (at least 5 characters)","कारण (कम से कम 5 अक्षर)"),500);button(t("Confirm cancellation","रद्द करने की पुष्टि")){if(reason.text.trim().length>=5)request("/api/learning-centre",JSONObject().put("action","cancel").put("assignmentId",id).put("reason",reason.text.toString())){detail(id)}};button(t("Back","वापस")){detail(id)}}
        }
        button(t("Assignment list","अभ्यास कार्य सूची")){list()}
    }}
    private fun shareForm(available:List<JSONObject>,page:Int=0){reset();text(t("Share selected practice with this centre","चयनित अभ्यास इस केंद्र से साझा करें"));text(t("Staff receive decisions, mistakes, hints and self-reported exploration from the selected records. Private teach-back text is excluded. Withdraw stops future access, but cannot recall earlier exports.","कर्मचारियों को चयनित निर्णय, गलतियाँ, संकेत और बताए गए मॉडल निरीक्षण मिलेंगे। निजी लिखित विचार नहीं भेजे जाएँगे। वापस लेने से आगे का प्रवेश रुकेगा, पहले निर्यात की गई प्रतियाँ वापस नहीं आएँगी।"),true)
        if(available.isEmpty()){text(t("Complete a new practice in the assigned mode and content version, then upload it privately before sharing.","निर्धारित मोड और संस्करण में नया अभ्यास पूरा करें, फिर साझा करने से पहले निजी अपलोड करें।"));button(t("Private upload","निजी अपलोड")){dialog.dismiss();LearningPracticeSyncDialog.show(host,store!!)};button(t("Back to assignment","कार्य पर वापस")){detail(assignment.getString("id"))};return}
        val checks=available.drop(page*20).take(20).map{row->val check=CheckBox(host).apply{text="${row.getString("scenarioId")} · ${date(row.getLong("updatedAt"))}"};body.add(check,bottom=8);row to check}
        val consent=CheckBox(host).apply{text=t("I choose to share these records with this centre","मैं ये रिकॉर्ड इस केंद्र से साझा करना चुनता/चुनती हूँ")};body.add(consent,bottom=12)
        button(t("Share selected practice","चयनित अभ्यास साझा करें"),true){val selected=checks.filter{it.second.isChecked}.map{it.first.getString("id")};if(consent.isChecked&&selected.isNotEmpty())request("/api/learning-enrolment",JSONObject().put("action","submit").put("workerId",store!!.workerId).put("owner",owner).put("assignmentId",assignment.getString("id")).put("attemptIds",JSONArray(selected)).put("shareEvidence",true)){detail(assignment.getString("id"))}else text(t("Select a practice and confirm sharing first.","पहले अभ्यास चुनें और साझा करने की पुष्टि करें।"),true)}
        if((page+1)*20<available.size)button(t("Next practice choices · clears selection","अगले अभ्यास विकल्प · चयन हटेगा")){shareForm(available,page+1)}
        if(page>0)button(t("Previous choices · clears selection","पिछले विकल्प · चयन हटेगा")){shareForm(available,page-1)}
        button(t("Back to assignment","कार्य पर वापस")){detail(assignment.getString("id"))}
    }
    private fun submission(row:JSONObject){reset();text(row.getString("scenario_id"));val id=assignment.getString("id");val evidence=row.optJSONObject("evidence")
        if(evidence==null){text(t("Sharing withdrawn. Evidence is unavailable to the centre.","साझा करना वापस लिया गया। केंद्र के लिए प्रमाण उपलब्ध नहीं है।"));button(t("Back","वापस")){detail(id)};return}
        text("${evidence.optInt("decisions")} decisions · ${evidence.optInt("mistakes")} retries · ${evidence.optInt("hints")} hints");text(t("Self-reported virtual exploration; private reflections excluded. No practical skill score.","बताया गया काल्पनिक मॉडल निरीक्षण; निजी विचार शामिल नहीं। व्यावहारिक कौशल स्कोर नहीं है।"),true)
        button(t("Review event history","क्रिया इतिहास देखें")){events(row,0)}
        val reviews=assignment.getJSONArray("reviews").objects().filter{it.getString("submission_id")==row.getString("id")}
        reviews.forEach{event->text("${event.getString("outcome")} · ${date(event.getLong("created_at"))}\n${event.getString("note")}")}
        val previous=reviews.firstOrNull()?.getString("id")
        if(learner){button(t("Request correction","सुधार का अनुरोध")){feedback(row,previous,true)};button(t("Withdraw sharing","साझा करना वापस लें")){confirm(t("Withdraw this record?","यह रिकॉर्ड वापस लें?"),t("Future centre access is removed. Previously exported copies cannot be recalled.","आगे का केंद्र प्रवेश हट जाएगा। पहले निर्यात की गई प्रतियाँ वापस नहीं आएँगी।")){request("/api/learning-enrolment",JSONObject().put("action","withdraw").put("workerId",store!!.workerId).put("owner",owner).put("submissionId",row.getString("id"))){detail(id)}}}}
        else if(role in listOf("admin","trainer","examiner","assessor")&&row.getString("account_id")!=actorId)button(t("Add educator feedback","प्रशिक्षक प्रतिक्रिया जोड़ें"),true){feedback(row,previous,false)}
        button(t("Export this shared record","यह साझा रिकॉर्ड निर्यात करें")){share(JSONObject().put("assignment",assignment.getJSONObject("catalogue")).put("evidence",evidence).put("feedback",JSONArray(reviews)).put("certifiable",false).toString(2),t("Shared formative record","साझा अभ्यास रिकॉर्ड"))}
        button(t("Back to assignment","कार्य पर वापस")){detail(id)}
    }
    private fun events(row:JSONObject,page:Int){reset();val events=row.getJSONObject("evidence").getJSONArray("events").objects();val scenario=assignment.getJSONObject("catalogue").getJSONArray("scenarios").objects().find{it.getString("id")==row.getString("scenario_id")};val steps=scenario?.getJSONArray("steps")?.objects().orEmpty()
        events.drop(page*12).take(12).forEach{event->val step=steps.find{it.getString("id")==event.optString("stepId")};val choice=step?.optJSONArray("choices")?.objects()?.find{it.getString("id")==event.optString("choiceId")};text(listOf(event.getString("kind"),step?.optString("title")?:event.optString("stepId"),choice?.optString("label")?:event.optString("partId"),event.optString("mode")).filter{it.isNotBlank()}.joinToString(" · "),true)}
        if((page+1)*12<events.size)button(t("Next events","आगे की क्रियाएँ")){events(row,page+1)};if(page>0)button(t("Previous events","पिछली क्रियाएँ")){events(row,page-1)};button(t("Back to record","रिकॉर्ड पर वापस")){submission(row)}
    }
    private fun feedback(row:JSONObject,previous:String?,correction:Boolean){reset();val requestId=UUID.randomUUID().toString();text(if(correction)t("Request a correction without changing the original evidence","मूल प्रमाण बदले बिना सुधार माँगें")else t("Feedback on this shared snapshot","इस साझा प्रति पर प्रतिक्रिया"));val outcome=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,listOf(t("Reviewed","समीक्षा पूरी"),t("Repeat practice","अभ्यास दोहराएँ"),t("More information needed","और जानकारी चाहिए")))};if(!correction)body.add(outcome,bottom=10);val note=field(t("Note · 10–1,000 characters","टिप्पणी · 10–1,000 अक्षर"));button(t("Save feedback","प्रतिक्रिया सहेजें"),true){if(note.text.trim().length>=10){val payload=JSONObject().put("action",if(correction)"correction"else"review").put("id",requestId).put("submissionId",row.getString("id")).put("previousEventId",previous?:JSONObject.NULL).put("note",note.text.toString());if(correction)payload.put("workerId",store!!.workerId).put("owner",owner)else payload.put("outcome",listOf("reviewed","repeat-practice","needs-information")[outcome.selectedItemPosition]);request(if(correction)"/api/learning-enrolment"else"/api/learning-centre",payload){detail(assignment.getString("id"))}}else text(t("Add a note of at least 10 characters.","कम से कम 10 अक्षरों की टिप्पणी दें।"),true)};button(t("Cancel","रद्द करें")){submission(row)}}
    private fun observation(){reset();val requestId=UUID.randomUUID().toString();val observedAt=System.currentTimeMillis();text(t("Record a separate learning observation","अलग सीखने का निरीक्षण दर्ज करें"));text(t("Only record what you observed today. This does not create a practical qualification.","केवल आज देखा गया निरीक्षण दर्ज करें। इससे व्यावहारिक योग्यता नहीं बनती।"),true);val settings=listOf(t("In-person training","प्रत्यक्ष प्रशिक्षण"),t("Remote simulation","दूरस्थ सिमुलेशन"));val setting=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,settings)};body.add(setting,bottom=8);val rubric=listOf("communication","sequence","safeDecisions").map{key->val check=CheckBox(host).apply{text=when(key){"communication"->t("Clear communication","स्पष्ट संवाद");"sequence"->t("Steps in sequence","सही क्रम में चरण");else->t("Safe decisions","सुरक्षित निर्णय")}};body.add(check,bottom=8);key to check};val note=field(t("Observation note · 10–1,000 characters","निरीक्षण टिप्पणी · 10–1,000 अक्षर"));button(t("Save observation","निरीक्षण सहेजें"),true){if(note.text.trim().length>=10){val criteria=JSONObject();rubric.forEach{criteria.put(it.first,it.second.isChecked)};request("/api/learning-centre",JSONObject().put("action","observe").put("id",requestId).put("assignmentId",assignment.getString("id")).put("observedAt",observedAt).put("setting",if(setting.selectedItemPosition==0)"in-person-training"else"remote-simulation").put("rubric",criteria).put("outcome",if(rubric.all{it.second.isChecked})"observed"else"further-practice").put("note",note.text.toString())){detail(assignment.getString("id"))}}};button(t("Cancel","रद्द करें")){detail(assignment.getString("id"))}}
    private fun create(){reset();val requestId=UUID.randomUUID().toString();val workers=workspace.getJSONArray("workers").objects();if(workers.isEmpty()){text(t("Register a learner in the workspace roster first.","पहले केंद्र की सूची में शिक्षार्थी जोड़ें।"));button(t("Back","वापस")){list()};return}
        val catalogue=workspace.getJSONObject("catalogue");val courses=catalogue.getJSONArray("courses").objects();val worker=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,workers.map{it.optString("name")})};val course=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,courses.map{it.getString("title")})};val audience=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,listOf(t("School awareness","विद्यालय जागरूकता"),t("Adult learning","वयस्क सीखना")))};val mode=Spinner(host).apply{adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,listOf(t("Guided practice","मार्गदर्शित अभ्यास"),t("Independent practice","स्वतंत्र अभ्यास")))}
        listOf(t("Learner","शिक्षार्थी") to worker,t("Course","पाठ्यक्रम") to course,t("Audience scope","सीखने का दायरा") to audience,t("Practice mode","अभ्यास मोड") to mode).forEach{(label,input)->text(label,true);body.add(input,bottom=12)}
        text(t("School awareness assigns only help-seeking and evacuation scenarios. Consent arrangements belong to the institution.","विद्यालय जागरूकता में केवल मदद बुलाने और सुरक्षित निकलने के पाठ दिए जाएँगे। सहमति की व्यवस्था संस्था की ज़िम्मेदारी है।"),true)
        val due=Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,7);set(Calendar.HOUR_OF_DAY,18);set(Calendar.MINUTE,0)}
        button(t("Choose due date","अंतिम तारीख चुनें")){DatePickerDialog(host,{_,y,m,d->due.set(y,m,d,18,0);text("${t("Due","अंतिम तारीख")}: ${date(due.timeInMillis)}",true)},due.get(Calendar.YEAR),due.get(Calendar.MONTH),due.get(Calendar.DAY_OF_MONTH)).show()}
        val note=field(t("Instructions (optional)","निर्देश (वैकल्पिक)"),500)
        button(t("Review assignment","अभ्यास कार्य की समीक्षा"),true){val selected=courses[course.selectedItemPosition];val scenarioIds=selected.getJSONArray(if(audience.selectedItemPosition==0)"awarenessScenarios"else"adultScenarios");confirm(t("Create this assignment?","यह अभ्यास कार्य बनाएँ?"),"${workers[worker.selectedItemPosition].getString("name")} · ${selected.getString("title")}\n${scenarioIds.length()} scenarios · ${audience.selectedItem} · ${mode.selectedItem}\n${date(due.timeInMillis)}\n${catalogue.getString("version")}"){request("/api/learning-centre",JSONObject().put("action","assign").put("id",requestId).put("workerIds",JSONArray().put(workers[worker.selectedItemPosition].getString("id"))).put("courseId",selected.getString("id")).put("contentVersion",catalogue.getString("version")).put("audience",if(audience.selectedItemPosition==0)"school-awareness"else"adult-learning").put("scenarioIds",scenarioIds).put("mode",if(mode.selectedItemPosition==0)"guided"else"independent").put("dueAt",due.timeInMillis).put("note",note.text.toString())){list()}}};button(t("Cancel","रद्द करें")){list()}
    }
}
