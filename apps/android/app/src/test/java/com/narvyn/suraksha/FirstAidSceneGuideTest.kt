package com.narvyn.suraksha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirstAidSceneGuideTest {
    @Test fun everyShippedStepHasSpecificBilingualSceneWithActualBoundedGeometry(){
        val parts=FirstAidTeachingProps.parts
        assertEquals(parts.size,parts.map{it.id}.toSet().size)
        val definitions=FirstAidScenarioCatalog.all.map{it.id to it.steps}+listOf(FirstAidBay.SCENARIO_ID to FirstAidBay.steps)
        for((id,steps) in definitions)for(step in steps){
            val scene=requireNotNull(FirstAidSceneGuide.forStep(id,step.id)){"Missing $id/${step.id}"}
            assertTrue(scene.caption.isNotBlank());if(id!=FirstAidBay.SCENARIO_ID)assertTrue(scene.captionHi.any{it in '\u0900'..'\u097F'})
            assertTrue("Unbuilt props at ${scene.key}",FirstAidTeachingProps.groups.containsAll(scene.props))
            val visible=FirstAidTeachingProps.visible(scene);assertTrue(visible.isNotEmpty())
            assertTrue("Unbounded geometry ${scene.key}",visible.sumOf{it.vertices.size/18}<30_000)
        }
        for(p in parts){
            assertEquals(0,p.vertices.size%18);assertTrue(p.vertices.all{it.isFinite()})
            assertEquals("teaching",p.layer)
            assertTrue("Prop leaves the verified AR mat width: ${p.id}",p.bounds[0]>=-.49f&&p.bounds[3]<=.79f)
            assertTrue(p.bounds[1]>=0f&&p.bounds[4]<=1.9f)
            assertTrue(p.bounds[2]>=-.16f&&p.bounds[5]<=.45f)
        }
        assertNull(FirstAidSceneGuide.forStep("unknown","observe"));assertNull(FirstAidSceneGuide.byKey(null))
    }
    @Test fun burnCoolingClothingAndCoverHaveDifferentPhysicallyRelevantProps(){
        fun guide(step:String)=FirstAidSceneGuide.forStep("thermal-burn-response",step)!!
        val cool=guide("cool");val clothing=guide("clothing");val cover=guide("cover-help")
        assertTrue(cool.props.containsAll(setOf("tap","tray","water","sleeve")))
        assertEquals(setOf("water"),cool.motion)
        assertTrue("sleeve" in clothing.props);assertFalse("water" in clothing.props)
        assertTrue(cover.props.containsAll(setOf("film","sleeve","call")))
        assertFalse("water" in cover.props)
        val film=FirstAidTeachingProps.visible(cover).first{it.id=="teaching:film"}
        assertTrue("Loose film must remain above the forearm, not wrap underneath",film.bounds[2]>.03f&&film.bounds[5]<.06f)
        for(t in listOf(0f,.1f,.9f,5f,10_000f,Float.NaN))for(i in 0..11){
            val offset=FirstAidTeachingProps.offset("teaching:water-drop-$i",t,cool)
            assertTrue(offset.all{it.isFinite()});assertEquals(0f,offset[0],0f);assertEquals(0f,offset[1],0f)
            assertTrue(offset[2] in -.178f..0f)
        }
    }
    @Test fun embeddedObjectExceptionNeverShowsDirectPressureOnTheObject(){
        val direct=FirstAidSceneGuide.forStep("bleeding-injury","pressure")!!
        val embedded=FirstAidSceneGuide.forStep("bleeding-injury","embedded")!!
        assertTrue(direct.props.containsAll(setOf("dressing","pressure")));assertFalse("embedded" in direct.props)
        assertTrue(embedded.props.containsAll(setOf("embedded","side-dressings")))
        assertFalse("pressure" in embedded.props);assertFalse("dressing" in embedded.props)
        assertNull(FirstAidSceneGuide.actionForPart(embedded,"teaching:embedded"))
        assertEquals("pressure-around",FirstAidSceneGuide.actionForPart(embedded,"teaching:side-dressings"))
    }
    @Test fun alteredConsciousnessNeverOffersDrinksAndRecoveryHasArticulatedGuide(){
        val normal=FirstAidSceneGuide.forStep("unresponsive-normal-breathing","monitor")!!
        val changed=FirstAidSceneGuide.forStep("unresponsive-normal-breathing","change")!!
        assertTrue(normal.props.containsAll(setOf("breath","recovery-guide")))
        assertTrue("gasp" in changed.props);assertFalse("breath" in changed.props)
        for(step in listOf("confusion","swallow","handover")){
            val state=FirstAidSceneGuide.forStep("heat-illness-response",step)!!
            assertFalse("bottle" in state.props);assertTrue("fan" in state.props)
        }
        val seizure=FirstAidSceneGuide.forStep("seizure-response","mouth")!!
        assertTrue("no-mouth" in seizure.props);assertFalse("bottle" in seizure.props)
        val pose=FirstAidTeachingProps.parts.first{it.id=="teaching:recovery-guide-person"}
        assertTrue(pose.bounds[3]-pose.bounds[0]>.16f)
    }
    @Test fun directTargetsAlwaysRouteToAnExistingCorrectActionAndDoNotBypassTracking(){
        for(s in FirstAidScenarioCatalog.all)for(step in s.steps){
            val guide=FirstAidSceneGuide.forStep(s.id,step.id)!!
            for(p in FirstAidTeachingProps.visible(guide)){
                val id=FirstAidSceneGuide.actionForPart(guide,p.id)?:continue
                assertTrue("Invalid alias ${guide.key}:$id",step.actions.any{it.id==id&&it.correct})
                val bay=FirstAidBay.createScenario("learner","version",s.id,100)
                var at=101L
                while(bay.current.id!=step.id)bay.act(bay.current.actions.single{it.correct}.id,"3d",false,at++)
                assertEquals("tracking-unavailable",bay.act(id,"ar",false,at++).reason)
                assertEquals(step.id,bay.current.id)
                assertTrue(bay.act(id,"ar",true,at).advanced)
            }
        }
    }
    @Test fun contactPropsAreCalibratedToTheShippedBodyRatherThanHoveringAboveIt(){
        fun part(id:String)=FirstAidTeachingProps.parts.first{it.id=="teaching:$id"}
        for(id in listOf("cut","scald","limb")){
            assertTrue("$id floats above the skin",part(id).bounds[5]<.04f)
            assertTrue("$id is buried too deeply",part(id).bounds[2]>.012f)
        }
        for(id in listOf("tray","tap","fan","call","handover","legs-guide-board"))
            assertTrue("$id does not rest near the measured back plane",part(id).bounds[2] in -.16f..-.12f)
        assertTrue(part("cpr").bounds[2] in .11f.. .14f)
        assertTrue(CasualtyMarkers.centres.getValue("LEFT_FOREARM")[2]<.045f)
        assertTrue(CasualtyMarkers.centres.getValue("RIGHT_LOWER_LEG")[2]<.045f)
    }
    @Test fun apparatusBehindTheBodyOrAnOpaquePropCannotBeActivatedThroughIt(){
        val hits=listOf("teaching:sleeve" to .3f,"teaching:film" to .2f)
        assertNull(FirstAidTeachingPicking.front(hits,.1f))
        assertEquals("teaching:film",FirstAidTeachingPicking.front(hits,.4f))
        assertEquals("teaching:embedded",FirstAidTeachingPicking.front(listOf("teaching:embedded" to .1f,"teaching:side-dressings" to .2f),null))
        assertNull(FirstAidTeachingPicking.front(listOf("bad" to Float.NaN),null))
    }
    @Test fun legacyRecognitionRemainsExplicitAndSideRolledDecisionHasNoSupineCprHands(){
        for(step in FirstAidBay.steps.filter{it.partId!=null}){
            assertTrue(FirstAidSceneGuide.forStep(FirstAidBay.SCENARIO_ID,step.id)!!.caption.contains("select the"))
        }
        val analysis=FirstAidSceneGuide.forStep(FirstAidBay.SCENARIO_ID,"analysis")!!
        assertTrue("cpr" in analysis.props);assertFalse("clear" in analysis.props)
        assertTrue(analysis.caption.contains("resume CPR"))
        assertFalse("cpr" in FirstAidSceneGuide.forStep("unresponsive-normal-breathing","change")!!.props)
        val pressure=FirstAidSceneGuide.forStep("bleeding-injury","pressure")!!
        assertTrue(pressure.caption.contains("neither treatment time nor pressure"))
    }
    @Test fun presentationLeavesPreviouslySavedScenarioDefinitionsAndAnswersByteCompatible(){
        for(s in FirstAidScenarioCatalog.all){
            val bay=FirstAidBay.createScenario("learner","version",s.id,100)
            bay.act(bay.current.actions.single{it.correct}.id,"3d",false,101)
            val before=bay.data.toString();val fingerprint=s.fingerprint
            for(step in s.steps){val guide=FirstAidSceneGuide.forStep(s.id,step.id)!!;FirstAidTeachingProps.visible(guide)}
            assertEquals(fingerprint,s.fingerprint)
            assertEquals(before,FirstAidBay.restore(JSONObject(before),"learner","version").data.toString())
        }
    }
}
