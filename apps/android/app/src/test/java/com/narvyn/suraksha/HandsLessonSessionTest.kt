package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class HandsLessonSessionTest {
    private fun json(path:String)=JSONObject(File("../../../$path").readText())
    private fun content()=json("content/learning/scenarios.json")
    private fun scenarios()=content().getJSONArray("scenarios").let{a->(0 until a.length()).map{a.getJSONObject(it)}}
    private fun plans()=json("content/spatial/catalog.json").getJSONObject("steps")
    private fun scenario(id:String)=scenarios().single{it.getString("id")==id}
    private fun session(id:String,start:String?=null,school:Boolean=false)=HandsLessonSession.create(scenario(id),plans(),content().getString("version"),school,start)

    @Test fun everyAuthoredStepRetainsItsExactSceneAndTeachingText(){
        val plans=plans();var count=0
        for(scenario in scenarios()){
            val session=HandsLessonSession.create(scenario,plans,content().getString("version"),false)
            val steps=scenario.getJSONArray("steps")
            assertEquals(steps.length(),session.steps.size)
            for(i in 0 until steps.length()){
                val step=steps.getJSONObject(i);val plan=plans.getJSONObject("${scenario.getString("id")}/${step.getString("id")}")
                val view=session.current
                assertEquals(step.getString("title"),view.title)
                assertEquals(step.getString("cue"),view.cue)
                assertEquals(step.getString("explanation"),view.explanation)
                assertEquals(plan.getString("model"),view.renderModel)
                assertEquals(scenario.getString("audience"),view.audience)
                assertEquals(i,view.index);assertEquals(steps.length(),view.count)
                assertFalse(view.certifiable);assertEquals("not-assessed",view.practicalCompetence)
                if(i<steps.length()-1)assertTrue(session.next())else assertFalse(session.next())
                count++
            }
        }
        assertEquals("All current authored views must be covered",94,count)
    }

    @Test fun kitchenAndDamagedExtinguisherNeverFallBackToTheSameGenericModel(){
        val choices=session("agent-choice")
        assertEquals("fire-fryer",choices.current.renderModel)
        assertTrue(choices.next());assertEquals("fire-electrical",choices.current.renderModel)
        assertTrue(choices.goTo("sand"));assertEquals("fire-sand-bucket",choices.current.renderModel)
        val damaged=session("extinguisher-check","fault")
        assertEquals("fire-damaged",damaged.current.renderModel)
        assertEquals("fibre",damaged.current.equipmentPartId)
        assertNull(damaged.current.partId)
    }

    @Test fun scaldCoolingAttachedClothingAndCoveringUseDistinctAuthoredProps(){
        val lesson=session("thermal-burn","cool")
        val cool=lesson.current
        assertEquals("thermal-burn-response/cool",cool.teachingKey)
        assertTrue(FirstAidSceneGuide.byKey(cool.teachingKey)!!.props.containsAll(setOf("tap","water","sleeve")))
        assertTrue(lesson.next());assertEquals("thermal-burn-response/clothing",lesson.current.teachingKey)
        assertTrue(FirstAidSceneGuide.byKey(lesson.current.teachingKey)!!.props.contains("sleeve"))
        assertFalse(FirstAidSceneGuide.byKey(lesson.current.teachingKey)!!.props.contains("water"))
        assertTrue(lesson.next());assertEquals("thermal-burn-response/cover-help",lesson.current.teachingKey)
        assertTrue(FirstAidSceneGuide.byKey(lesson.current.teachingKey)!!.props.contains("film"))
    }

    @Test fun relatedAnatomyCannotReplaceTheWholeCasualtyTeachingScene(){
        val cpr=session("adult-collapse","cpr").current
        assertEquals("body",cpr.modelId)
        assertEquals("anatomy",cpr.renderModel)
        assertEquals("adult-collapse-response/begin_cpr",cpr.teachingKey)
        assertNull("A related sternum is not an isolated scene selection",cpr.partId)
        assertEquals("FMA7485",cpr.observationPartId)
        val limb=session("sprain-support","cold").current
        assertEquals("FMA24478",limb.partId)
        assertNull(limb.teachingKey)
    }

    @Test fun navigationIsBoundedAndNeverMutatesAuthoredContentOrAssessments(){
        val original=scenario("escape-first");val originalJson=original.toString()
        val sourcePlans=plans();val originalPlans=sourcePlans.toString()
        val lesson=HandsLessonSession.create(original,sourcePlans,"test-version",false,"role")
        assertEquals(2,lesson.index);assertTrue(lesson.hasPrevious);assertTrue(lesson.hasNext)
        assertTrue(lesson.previous());assertEquals("exit",lesson.current.stepId)
        assertTrue(lesson.goTo("notice"));assertFalse(lesson.previous());assertEquals(0,lesson.index)
        assertFalse(lesson.goTo("unknown"));assertEquals(0,lesson.index)
        repeat(100){lesson.next()};assertEquals(5,lesson.index);assertFalse(lesson.hasNext)
        repeat(100){lesson.previous()};assertEquals(0,lesson.index)
        assertEquals(originalJson,original.toString());assertEquals(originalPlans,sourcePlans.toString())
        assertFalse(lesson.certifiable);assertEquals("not-assessed",lesson.practicalCompetence)
    }

    @Test fun aFrozenViewDoesNotAdoptLaterJsonMutations(){
        val source=scenario("school-fire-help");val plans=plans()
        val lesson=HandsLessonSession.create(source,plans,"pinned-version",true)
        val before=lesson.current
        source.getJSONArray("steps").getJSONObject(0).put("cue","REPLACED")
        plans.getJSONObject(before.key).put("model","fire-damaged")
        assertEquals(before,lesson.current);assertNotEquals("REPLACED",lesson.current.cue)
        assertEquals("fire-alarm-stand",lesson.current.renderModel)
        assertEquals("pinned-version",lesson.current.contentVersion)
        assertThrows(UnsupportedOperationException::class.java){(lesson.steps as MutableList<HandsLessonStep>).clear()}
    }

    @Test fun schoolCanOnlyOpenAwarenessLessonsAndNoTreatmentProps(){
        val plans=plans()
        for(scenario in scenarios()){
            if(scenario.getString("audience")=="awareness"){
                val lesson=HandsLessonSession.create(scenario,plans,"school-version",true)
                assertTrue(lesson.steps.all{it.audience=="awareness"&&it.teachingKey==null})
            }else assertThrows(IllegalArgumentException::class.java){HandsLessonSession.create(scenario,plans,"school-version",true)}
        }
        val school=scenario("school-fire-help")
        val injected=plans()
        injected.getJSONObject("school-fire-help/alarm").put("model","anatomy").put("kind","teaching").put("teachingKey","adult-collapse-response/begin_cpr")
        assertThrows(IllegalArgumentException::class.java){HandsLessonSession.create(school,injected,"school-version",true)}
    }

    @Test fun invalidAndCrossScenarioEntriesFailClosedBeforePresentation(){
        assertThrows(IllegalArgumentException::class.java){session("thermal-burn","not-a-step")}
        assertThrows(IllegalArgumentException::class.java){session("thermal-burn","fault")}
        val scenario=scenario("thermal-burn")
        fun rejects(change:(JSONObject,JSONObject)->Unit){
            val s=JSONObject(scenario.toString());val p=plans();change(s,p)
            assertThrows(IllegalArgumentException::class.java){HandsLessonSession.create(s,p,"test",false)}
        }
        rejects{_,p->p.remove("thermal-burn/cool")}
        rejects{_,p->p.getJSONObject("thermal-burn/cool").put("audience","awareness")}
        rejects{_,p->p.getJSONObject("thermal-burn/cool").put("model","untrusted-object")}
        rejects{_,p->p.getJSONObject("thermal-burn/cool").put("teachingKey","unknown/step")}
        rejects{s,_->val steps=s.getJSONArray("steps");steps.put(JSONObject(steps.getJSONObject(0).toString()))}
        rejects{s,_->s.put("course","unknown-course")}
        rejects{s,_->s.getJSONArray("steps").getJSONObject(0).put("source","javascript:open()")}
        rejects{s,_->s.put("steps",JSONArray())}
    }
}
