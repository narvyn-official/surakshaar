package com.narvyn.suraksha

import kotlin.math.*

/** Original illustrative construction details, in metres; not a service/repair or manufacturer model.
 * Bounded ribbons represent weave at inspection distance, rather than simulated microscopic fibres. */
internal object EquipmentDetailGeometry {
    private val steel=WorldEquipment.Material(floatArrayOf(.64f,.69f,.73f),.24f,.82f)
    private val brass=WorldEquipment.Material(floatArrayOf(.67f,.46f,.18f),.29f,.75f)
    private val ink=WorldEquipment.Material(floatArrayOf(.09f,.14f,.18f),.8f)
    private val paper=WorldEquipment.Material(floatArrayOf(.94f,.90f,.73f),.93f)
    private val red=WorldEquipment.Material(floatArrayOf(.71f,.035f,.045f),.62f)
    private val thread=WorldEquipment.Material(floatArrayOf(.76f,.78f,.69f),.98f)
    private val webbing=WorldEquipment.Material(floatArrayOf(.13f,.18f,.22f),.95f)
    private fun p(x:Float,y:Float,z:Float)=floatArrayOf(x,y,z)
    private fun WorldEquipment.Builder.slip(x:Float,y:Float,z:Float,w:Float,h:Float,warning:Boolean=false,back:Boolean=false){
        val face=if(back)-1f else 1f
        box(x,y,z,w,h,.0018f,.002f,paper)
        box(x,y+h*.30f,z+face*.0012f,w*.83f,h*.13f,.0006f,0f,if(warning)red else ink)
        for(i in 0..4)box(x+w*.04f,y+h*(.12f-i*.105f),z+face*.0013f,w*(if(i==4).44f else .66f),.0007f,.0005f,0f,ink)
        // Printed form boxes and a folded lower corner, not an invented inspection approval.
        for(i in 0..2)box(x-w*.37f,y-h*(.0f+i*.16f),z+face*.0014f,.002f,.002f,.0005f,0f,ink)
        box(x+w*.38f,y-h*.44f,z+face*.0025f,w*.20f,h*.13f,.0007f,0f,paper,rx=18f)
    }
    fun fire(b:WorldEquipment.Builder,pinRemoved:Boolean,damaged:Boolean,aimed:Boolean)=with(b){
        // Inspection tag sits clear of the gauge and operating lever. Eyelet, cord and knot are separate.
        slip(-.117f,.373f,.024f,.048f,.079f)
        torus(-.117f,.405f,.0255f,.0034f,.0009f,steel)
        hose(listOf(p(-.117f,.405f,.026f),p(-.120f,.436f,.023f),p(-.030f,.464f,.013f),p(-.052f,.427f,.024f),p(-.117f,.405f,.026f)),.00075f,thread)
        torus(-.115f,.408f,.027f,.0014f,.0007f,thread)
        // True helical ridge on the valve neck, not stacked rings.
        tube((0..96).map{val a=it*2*PI/24;p(cos(a).toFloat()*.0268f,.427f+it*.00015f,sin(a).toFloat()*.0268f)},.00065f,brass)
        // Valve pivot, washer, screw slot and retaining clip.
        cylinder(.016f,.488f,.024f,.012f,.004f,steel,rx=90f)
        box(.016f,.488f,.027f,.007f,.0013f,.001f,0f,ink,rz=30f)
        box(.091f,.475f,-.014f,.012f,.017f,.014f,.002f,steel)
        if(!aimed)for(i in 0..9)cylinder(.12f,.127f+i*.003f,-.006f,.030f,.0008f,steel)
        // Vessel seam and foot-ring drainage notches.
        for(i in 0..23){val a=i*2*PI/24;box(cos(a).toFloat()*.075f,.048f,sin(a).toFloat()*.075f,.005f,.006f,.003f,0f,ink)}
        if(!pinRemoved){
            // Numbered seal flag is illustrative, with no fabricated asset serial or compliance mark.
            slip(-.024f,.466f,.024f,.017f,.019f,warning=true)
            tube(listOf(p(-.034f,.478f,.020f),p(-.026f,.476f,.026f),p(-.024f,.474f,.024f)),.00055f,red)
        }else{
            // Removed pin and broken seal remain on the practice tray instead of disappearing.
            rod(p(-.25f,.045f,.14f),p(-.19f,.045f,.14f),.003f,steel)
            torus(-.266f,.045f,.14f,.015f,.0025f,steel)
            box(-.20f,.047f,.16f,.016f,.0015f,.009f,0f,red)
        }
        if(damaged)for(i in 0..9){val a=i*2*PI/10
            rod(p(.16f+cos(a).toFloat()*.006f,.35f,.015f+sin(a).toFloat()*.006f),p(.174f+cos(a).toFloat()*.009f,.329f,.026f+sin(a).toFloat()*.009f),.00065f,thread)}
    }
    fun gas(b:WorldEquipment.Builder)=with(b){
        slip(.063f,.195f,-.087f,.065f,.094f,back=true)
        // Back-case seam, belt clip hinge and textured grip ribs.
        for(x in listOf(-.115f,.115f))for(i in 0..13)box(x,.13f+i*.017f,0f,.009f,.002f,.086f,.0005f,ink)
        cylinder(0f,.361f,-.09f,.022f,.007f,steel,rx=90f)
        for(i in 0..3)cylinder(-.055f,.458f+i*.003f,0f,.028f,.001f,steel)
        for(i in -2..2)for(j in -1..1)cylinder(i*.013f,.405f+j*.006f,.065f,.003f,.0015f,steel,rx=90f)
        hose(listOf(p(-.08f,.42f,-.04f),p(-.16f,.44f,-.08f),p(-.20f,.31f,-.12f),p(-.14f,.22f,-.12f),p(-.08f,.42f,-.04f)),.002f,webbing)
    }
    fun machinery(b:WorldEquipment.Builder,locked:Boolean)=with(b){
        // Cable gland, spiral strain relief, motor casing split and fasteners.
        hose(listOf(p(-.22f,.38f,-.15f),p(-.23f,.24f,-.21f),p(.21f,.18f,-.19f),p(.28f,.20f,-.04f)),.008f,webbing)
        for(i in 0..5)cylinder(-.22f,.37f+i*.004f,-.15f,.024f,.002f,steel)
        for(x in listOf(-.25f,.18f))for(y in listOf(.115f,.40f)){cylinder(x,y,.153f,.012f,.003f,steel,rx=90f);box(x,y,.155f,.007f,.001f,.001f,0f,ink)}
        if(locked){
            slip(.328f,.174f,.146f,.060f,.09f,warning=true)
            torus(.328f,.211f,.148f,.003f,.001f,steel)
            tube(listOf(p(.328f,.211f,.148f),p(.29f,.281f,.128f),p(.32f,.223f,.145f),p(.328f,.211f,.148f)),.001f,thread)
            cylinder(.287f,.22f,.139f,.008f,.002f,steel,rx=90f)
            box(.287f,.219f,.141f,.0015f,.006f,.001f,0f,ink)
        }
        slip(-.09f,.43f,.035f,.075f,.032f,warning=true)
    }
    fun ppe(b:WorldEquipment.Builder)=with(b){
        // Visible under-brim suspension and textile cross-straps with stitch runs.
        for(z in listOf(-.09f,.07f)){
            hose(listOf(p(-.18f,.365f,z),p(-.08f,.33f,z),p(.08f,.33f,z),p(.18f,.365f,z)),.006f,webbing)
            for(i in 0..16){val t=i/16f;val stitchY=.323f-.0175f*t*(1f-t)
                rod(p(-.08f+i*.01f,stitchY,z-.003f),p(-.077f+i*.01f,stitchY,z+.003f),.00055f,thread)}
        }
        for(x in listOf(-.15f,.15f)){
            box(x,.348f,.06f,.023f,.022f,.008f,.002f,steel)
            box(x,.348f,.066f,.013f,.012f,.002f,0f,webbing)
        }
        slip(.06f,.338f,-.145f,.066f,.027f,back=true)
        // Fine ear-cushion seam. Kept away from safety lens transparency.
        for(x in listOf(-.253f,.253f))for(i in 0..13)box(x,.191f+i*.008f,.055f,.040f,.0008f,.001f,0f,thread)
    }
    fun emergency(b:WorldEquipment.Builder)=with(b){
        // First-aid supply card, folded instruction slip and case gasket are illustrative.
        slip(-.16f,.059f,.16f,.074f,.085f)
        for(i in 0..7)box(.12f+i*.015f,.042f,.15f,.0012f,.002f,.070f,0f,thread)
    }
}
