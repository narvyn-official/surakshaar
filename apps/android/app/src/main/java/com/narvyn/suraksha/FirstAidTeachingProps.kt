package com.narvyn.suraksha

import kotlin.math.*

/** Original, non-graphic teaching apparatus. All geometry is in the casualty's source frame. */
internal object FirstAidTeachingProps {
    data class Mesh(val group:String,val part:AnatomyPart)
    private val teal=floatArrayOf(.08f,.52f,.50f)
    private val blue=floatArrayOf(.17f,.47f,.80f)
    private val water=floatArrayOf(.28f,.72f,.94f)
    private val silver=floatArrayOf(.60f,.68f,.72f)
    private val white=floatArrayOf(.91f,.94f,.93f)
    private val dark=floatArrayOf(.13f,.20f,.27f)
    private val amber=floatArrayOf(.88f,.51f,.12f)
    private val cloth=floatArrayOf(.29f,.43f,.58f)
    private val skin=floatArrayOf(.72f,.44f,.32f)
    private val meshes:List<Mesh> by lazy { buildList {
        fun group(name:String,colour:FloatArray,id:String=name,build:Shape.()->Unit) {
            val shape=Shape().apply(build);add(Mesh(name,shape.part("teaching:$id",name,colour,sourceOffset(name))))
        }
        fun palm(name:String,x:Float,y:Float,z:Float) {
            group(name,blue){ellipsoid(x,y,z,.045f,.055f,.016f)
                for(i in 0..3)cylinder(floatArrayOf(x-.031f+i*.020f,y+.03f,z),floatArrayOf(x-.031f+i*.020f,y+.098f,z),.009f)
                cylinder(floatArrayOf(x-.037f,y,z),floatArrayOf(x-.060f,y+.040f,z),.012f)}
        }
        group("area",teal){for(x in listOf(-.41f,.41f))for(y in listOf(.07f,1.77f)){
            box(x,y,-.095f,.11f,.016f,.009f);box(x,y+if(y<1).045f else -.045f,-.095f,.014f,.10f,.009f)}}
        group("call",dark){box(.60f,1.53f,.02f,.115f,.20f,.026f)}
        group("call",teal,"call-screen"){box(.60f,1.53f,.035f,.096f,.155f,.003f)}
        group("call",white,"call-speaker"){box(.60f,1.61f,.038f,.026f,.003f,.003f)
            cylinder(floatArrayOf(.572f,1.51f,.04f),floatArrayOf(.578f,1.56f,.04f),.008f)
            cylinder(floatArrayOf(.578f,1.51f,.04f),floatArrayOf(.61f,1.495f,.04f),.008f)}
        group("handover",dark){box(.56f,.87f,-.045f,.25f,.34f,.023f)}
        group("handover",white,"handover-paper"){box(.56f,.87f,-.029f,.22f,.30f,.006f)}
        group("handover",teal,"handover-lines"){for(i in 0..3)box(.58f,.96f-i*.053f,-.024f,.14f,.008f,.004f)
            for(i in 0..3)box(.485f,.96f-i*.053f,-.023f,.014f,.014f,.006f)}
        group("response",teal){for(x in listOf(-.16f,.16f))ring(x,1.44f,.16f,.026f,.004f)}
        group("breath",teal){for(y in listOf(1.29f,1.34f,1.39f))arc(0f,y,.235f,.11f,.007f)}
        group("gasp",amber){for(i in 0..2)arc(0f,1.29f+i*.05f,.235f,.07f+i*.015f,.007f)}
        val chestY=FirstAidCprPlacement.AUTHOR_Y;val chestZ=FirstAidCprPlacement.AUTHOR_Z
        group("cpr",blue){
            // Heel over the lower sternum; fingers extend across, not toward the neck.
            // The small contact pad, palm and second hand share one calibrated transform.
            ellipsoid(0f,chestY,chestZ+.011f,.022f,.027f,.008f)
            ellipsoid(.025f,chestY,chestZ+.018f,.045f,.034f,.010f)
            ellipsoid(.022f,chestY,chestZ+.035f,.047f,.033f,.010f)
            for(i in 0..3){
                val y=chestY-.024f+i*.016f
                cylinder(floatArrayOf(.048f,y,chestZ+.028f),floatArrayOf(.103f,y,chestZ+.040f),.006f)
                cylinder(floatArrayOf(.051f,y+.007f,chestZ+.043f),floatArrayOf(.102f,y+.006f,chestZ+.042f),.005f)
            }
            cylinder(floatArrayOf(.002f,chestY-.026f,chestZ+.032f),floatArrayOf(.035f,chestY-.042f,chestZ+.043f),.008f)
        }
        // An open ring leaves the actual sternum selectable in legacy recognition practice.
        group("chest-landmark",teal){ring(0f,chestY,chestZ+.004f,.037f,.0025f)}
        group("aed-prompt",teal){box(.59f,1.14f,.022f,.10f,.008f,.005f);box(.54f,1.13f,.024f,.008f,.037f,.005f)}
        group("aed-diagram",white){box(.55f,.75f,.006f,.21f,.23f,.008f)}
        group("aed-diagram",blue,"aed-instructions"){box(.55f,.76f,.013f,.12f,.006f,.004f);box(.55f,.71f,.013f,.14f,.006f,.004f)
            ring(.55f,.81f,.013f,.025f,.006f)}
        group("clear",amber){for(x in listOf(-.39f,.39f))for(y in listOf(.58f,1.28f)){
            cylinder(floatArrayOf(x,y,.06f),floatArrayOf(x,y,.22f),.012f)
            box(x,y,.24f,.05f,.08f,.025f)}}
        group("cut",floatArrayOf(.55f,.24f,.23f)){ellipsoid(-.265f,1f,.103f,.032f,.039f,.006f)
            box(-.265f,1f,.110f,.041f,.006f,.004f)}
        group("scald",floatArrayOf(.75f,.37f,.29f)){ellipsoid(.265f,1f,.106f,.037f,.054f,.006f)}
        group("glove",blue){ellipsoid(-.40f,1.25f,.04f,.035f,.046f,.012f)
            for(i in 0..3)cylinder(floatArrayOf(-.427f+i*.018f,1.275f,.042f),floatArrayOf(-.427f+i*.018f,1.325f,.042f),.007f)}
        group("gauze",white){for(i in 0..2)box(-.38f,.79f,.018f+i*.008f,.12f,.12f,.009f)}
        // Sparse raised warp/weft and selvedge threads remain part of the relevant prop group.
        data class Weave(val group:String,val x:Float,val y:Float,val z:Float)
        for((g,x,y,z) in listOf(Weave("gauze",-.38f,.79f,.041f),Weave("dressing",-.265f,1f,.131f),Weave("cloth",0f,1.70f,.157f))){
            group(g,floatArrayOf(.71f,.77f,.76f),"$g-weave"){
                for(i in -10..10){
                    cylinder(floatArrayOf(x-.050f,y+i*.0045f,z),floatArrayOf(x+.050f,y+i*.0045f,z),.00045f)
                    cylinder(floatArrayOf(x+i*.0045f,y-.050f,z+.0006f),floatArrayOf(x+i*.0045f,y+.050f,z+.0006f),.00045f)
                }
                for(i in -8..8)cylinder(floatArrayOf(x-.050f,y+i*.005f,z),floatArrayOf(x-.054f,y+i*.005f+.001f,z),.00035f)
            }
        }
        group("dressing",white){foldedSheet(-.265f,1f,.125f,.11f,.14f,.004f)}
        palm("pressure",-.265f,1f,.164f)
        group("embedded",silver){box(-.265f,1f,.146f,.011f,.028f,.082f)}
        group("embedded",amber,"embedded-wound"){ellipsoid(-.265f,1f,.105f,.034f,.041f,.004f)}
        group("side-dressings",white){for(x in listOf(-.307f,-.223f))foldedSheet(x,1f,.126f,.045f,.13f,.005f)}
        group("side-dressings",blue,"side-pressure"){for(x in listOf(-.314f,-.216f))ellipsoid(x,1f,.152f,.020f,.039f,.013f)}
        group("source-off",silver){cylinder(floatArrayOf(.57f,.64f,-.08f),floatArrayOf(.57f,.64f,.06f),.060f)
            cylinder(floatArrayOf(.57f,.64f,.06f),floatArrayOf(.57f,.64f,.065f),.064f)
            cylinder(floatArrayOf(.62f,.64f,.035f),floatArrayOf(.67f,.65f,.07f),.011f)}
        group("source-off",dark,"source-handle"){arc(.57f,.64f,.095f,.041f,.008f)}
        group("tap",silver){box(.435f,1.08f,-.035f,.10f,.12f,.070f)
            cylinder(floatArrayOf(.435f,1.08f,-.015f),floatArrayOf(.435f,1.08f,.37f),.019f)
            cylinder(floatArrayOf(.435f,1.08f,.37f),floatArrayOf(.265f,1.03f,.37f),.021f)
            cylinder(floatArrayOf(.265f,1.03f,.37f),floatArrayOf(.265f,1.03f,.32f),.019f)}
        group("tap",blue,"tap-control"){cylinder(floatArrayOf(.435f,1.08f,.26f),floatArrayOf(.49f,1.08f,.26f),.015f)
            box(.49f,1.08f,.26f,.014f,.095f,.016f)}
        group("tray",silver){box(.265f,1f,-.053f,.19f,.25f,.022f)
            for(x in listOf(.172f,.358f))box(x,1f,-.025f,.012f,.25f,.056f)
            for(y in listOf(.88f,1.12f))box(.265f,y,-.025f,.19f,.012f,.056f)}
        group("tray",water,"tray-water"){box(.265f,1f,-.032f,.17f,.225f,.003f)}
        for(i in 0..11)group("water",water,"water-drop-$i"){
            val x=.265f+((i%3)-1)*.012f;val y=1.03f+((i%2)*2-1)*.006f
            ellipsoid(x,y,.305f,.007f,.007f,.020f)}
        group("sleeve",cloth){foldedSheet(.269f,1.075f,.129f,.105f,.086f,.008f)
            // A narrow adhered edge remains in place through cooling and covering.
            foldedSheet(.270f,1.035f,.118f,.068f,.044f,.003f)}
        group("sleeve-edge",amber){box(.27f,1.030f,.135f,.077f,.006f,.006f)}
        group("film",floatArrayOf(.76f,.87f,.91f)){foldedSheet(.269f,.986f,.15f,.132f,.132f,.002f)}
        group("film",white,"film-open-edges"){for(x in listOf(.203f,.335f))cylinder(floatArrayOf(x,.92f,.153f),floatArrayOf(x,1.05f,.153f),.0025f)}
        group("limb",floatArrayOf(.51f,.34f,.40f)){ellipsoid(-.09f,.35f,.118f,.041f,.061f,.005f)}
        group("supports",cloth){for(x in listOf(-.176f,.0f))ellipsoid(x,.34f,.045f,.046f,.20f,.049f)}
        group("urgent",amber){box(.48f,.47f,.03f,.14f,.15f,.02f)}
        group("urgent",dark,"urgent-mark"){box(.48f,.49f,.044f,.012f,.062f,.006f);box(.48f,.437f,.044f,.013f,.013f,.006f)}
        group("blanket",cloth){foldedSheet(0f,.95f,.235f,.33f,.19f,.006f)}
        group("hazards",amber){box(-.37f,1.60f,.05f,.12f,.11f,.12f);box(.37f,1.56f,.015f,.09f,.18f,.07f)}
        group("hazards-clear",amber){box(-.43f,.79f,.04f,.10f,.10f,.10f);box(.43f,.56f,.01f,.09f,.16f,.06f)}
        group("cushion",cloth){ellipsoid(0f,1.64f,-.134f,.145f,.16f,.020f)}
        group("clock",dark){cylinder(floatArrayOf(.57f,1.25f,.03f),floatArrayOf(.57f,1.25f,.055f),.065f)
            box(.57f,1.32f,.045f,.025f,.032f,.020f)}
        group("clock",white,"clock-face"){cylinder(floatArrayOf(.57f,1.25f,.057f),floatArrayOf(.57f,1.25f,.058f),.055f)}
        group("clock",dark,"clock-hands"){box(.57f,1.275f,.062f,.005f,.045f,.004f);box(.589f,1.25f,.062f,.038f,.005f,.004f)}
        group("seizure-cue",amber){for(x in listOf(-.24f,.24f))zigzag(x,1.43f,.13f)}
        group("no-mouth",silver){ellipsoid(.59f,1.47f,.04f,.024f,.040f,.008f)
            cylinder(floatArrayOf(.59f,1.44f,.04f),floatArrayOf(.59f,1.34f,.04f),.007f)}
        group("no-mouth",amber,"no-mouth-cross"){cross(.59f,1.43f,.064f,.065f)}
        group("shade",teal){box(.57f,1.45f,.35f,.34f,.38f,.018f)
            cylinder(floatArrayOf(.71f,1.58f,-.075f),floatArrayOf(.71f,1.58f,.34f),.010f)}
        group("heat",amber){for(x in listOf(-.12f,0f,.12f))zigzag(x,1.20f,.25f)}
        group("fan",dark){box(.58f,1.14f,-.058f,.15f,.15f,.04f)
            cylinder(floatArrayOf(.58f,1.14f,-.04f),floatArrayOf(.58f,1.14f,.13f),.016f)
            ring(.58f,1.14f,.14f,.096f,.012f)}
        group("fan",silver,"fan-blades"){for(angle in listOf(0.0,2*PI/3,4*PI/3)){
            val x=.58f+cos(angle).toFloat()*.040f;val y=1.14f+sin(angle).toFloat()*.040f
            ellipsoid(x,y,.145f,.036f,.032f,.006f)}
            ellipsoid(.58f,1.14f,.16f,.015f,.015f,.01f)}
        group("cloth",cloth){foldedSheet(0f,1.70f,.150f,.11f,.067f,.005f)}
        group("bottle",water){cylinder(floatArrayOf(.45f,.68f,-.072f),floatArrayOf(.45f,.68f,.07f),.031f)
            cylinder(floatArrayOf(.45f,.68f,.07f),floatArrayOf(.45f,.68f,.095f),.016f)}
        group("bottle",blue,"bottle-cap"){cylinder(floatArrayOf(.45f,.68f,.095f),floatArrayOf(.45f,.68f,.107f),.019f)}
        group("no-drink",white){box(.47f,.67f,.045f,.115f,.14f,.020f)}
        group("no-drink",amber,"no-drink-cross"){cross(.47f,.67f,.060f,.055f)}
        for(pose in listOf("recovery-guide","legs-guide","sit-guide")) {
            group(pose,teal,"$pose-board"){box(.55f,1.05f,-.070f,.31f,.82f,.012f)}
            group(pose,white,"$pose-person"){pose(pose)}
        }
    } }
    val parts:List<AnatomyPart> get()=meshes.map{it.part}
    val groups:Set<String> get()=meshes.map{it.group}.toSet()
    fun visible(scene:FirstAidSceneGuide.Scene?):List<AnatomyPart> = if(scene==null)emptyList()else meshes.filter{it.group in scene.props}.map{it.part}

    /** Every motion is small and periodic; no treatment time, force or breathing rate is measured. */
    fun offset(id:String,seconds:Float,scene:FirstAidSceneGuide.Scene):FloatArray {
        val t=if(seconds.isFinite())seconds.coerceAtLeast(0f) else 0f
        return when {
            id.startsWith("teaching:water-drop-")&&"water" in scene.motion -> {
                val n=id.substringAfterLast('-').toInt();floatArrayOf(0f,0f,-((t*.42f+n/12f)%1f)*.178f)
            }
            id=="teaching:breath"&&"breath" in scene.motion ->floatArrayOf(0f,0f,(sin(t*1.4f)*.008f).toFloat())
            id=="teaching:gasp"&&"gasp" in scene.motion ->floatArrayOf(0f,0f,(max(0.0,sin(t*2.8))*max(0.0,sin(t*.8))*.018).toFloat())
            id=="teaching:response"&&"response" in scene.motion ->floatArrayOf(0f,0f,(sin(t*2f)*.006f).toFloat())
            id=="teaching:cpr"&&"cpr" in scene.motion ->floatArrayOf(0f,0f,(-((1-cos(t*2*PI*110/60))/2)*FirstAidCprPlacement.MOTION_DEPTH).toFloat())
            else ->floatArrayOf(0f,0f,0f)
        }
    }
    fun fanAngle(seconds:Float)=if(seconds.isFinite())((seconds.coerceAtLeast(0f)*180f)%360f)else 0f

    // Calibrated against BOTH shipped FMA7163 meshes (balanced and optional detailed).
    // Front surfaces around the forearms/leg are z≈.021, chest≈.117, forehead≈.066;
    // the supine back plane is z≈-.146. Bake once so drawing, bounds and picking agree.
    private fun sourceOffset(group:String):Float=when(group){
        "scald","cut","embedded","tap","tray","water"->-.08f
        "sleeve","sleeve-edge","dressing","side-dressings"->-.10f
        "pressure"->-.12f
        "film"->-.11f
        "limb"->-.095f
        "supports"->-.137f
        "cloth"->-.080f
        "blanket","breath","gasp"->-.085f
        "response"->-.025f
        "area"->-.046f
        "call"->-.145f
        "handover"->-.085f
        "gauze"->-.150f
        "glove"->-.165f
        "source-off"->-.065f
        "clear"->-.194f
        "hazards","hazards-clear"->-.125f
        "clock"->-.171f
        "no-mouth"->-.170f
        "shade","bottle"->-.070f
        "fan"->-.065f
        "no-drink"->-.175f
        "urgent"->-.160f
        "recovery-guide","legs-guide","sit-guide"->-.068f
        "aed-prompt","aed-diagram"->-.035f
        else->0f
    }
    private class Shape {
        private val v=GeometryFloats()
        private fun vertex(p:FloatArray,n:FloatArray){for(a in p)v.add(a);for(a in n)v.add(a)}
        private fun tri(a:FloatArray,b:FloatArray,c:FloatArray,n:FloatArray){vertex(a,n);vertex(b,n);vertex(c,n)}
        fun box(x:Float,y:Float,z:Float,w:Float,h:Float,d:Float){
            val p=arrayOf(floatArrayOf(x-w/2,y-h/2,z-d/2),floatArrayOf(x+w/2,y-h/2,z-d/2),floatArrayOf(x+w/2,y+h/2,z-d/2),floatArrayOf(x-w/2,y+h/2,z-d/2),floatArrayOf(x-w/2,y-h/2,z+d/2),floatArrayOf(x+w/2,y-h/2,z+d/2),floatArrayOf(x+w/2,y+h/2,z+d/2),floatArrayOf(x-w/2,y+h/2,z+d/2))
            val f=arrayOf(intArrayOf(4,5,6,7),intArrayOf(1,0,3,2),intArrayOf(0,4,7,3),intArrayOf(5,1,2,6),intArrayOf(3,7,6,2),intArrayOf(0,1,5,4))
            val n=arrayOf(floatArrayOf(0f,0f,1f),floatArrayOf(0f,0f,-1f),floatArrayOf(-1f,0f,0f),floatArrayOf(1f,0f,0f),floatArrayOf(0f,1f,0f),floatArrayOf(0f,-1f,0f))
            f.forEachIndexed{i,q->tri(p[q[0]],p[q[1]],p[q[2]],n[i]);tri(p[q[0]],p[q[2]],p[q[3]],n[i])}
        }
        fun ellipsoid(x:Float,y:Float,z:Float,rx:Float,ry:Float,rz:Float){
            fun point(a:Double,b:Double):Pair<FloatArray,FloatArray>{val sx=(cos(a)*sin(b)).toFloat();val sy=(sin(a)*sin(b)).toFloat();val sz=cos(b).toFloat()
                val n=floatArrayOf(sx/rx,sy/ry,sz/rz);val length=sqrt(n.sumOf{(it*it).toDouble()}).toFloat();for(i in 0..2)n[i]/=length
                return floatArrayOf(x+sx*rx,y+sy*ry,z+sz*rz) to n}
            for(i in 0 until 12)for(j in 0 until 8){val a=i*2*PI/12;val b=(i+1)*2*PI/12;val c=j*PI/8;val d=(j+1)*PI/8
                val p=listOf(point(a,c),point(b,c),point(b,d),point(a,d));for(k in intArrayOf(0,1,2,0,2,3))vertex(p[k].first,p[k].second)}
        }
        fun cylinder(a:FloatArray,b:FloatArray,r:Float){
            val axis=FloatArray(3){b[it]-a[it]};val length=sqrt(axis.sumOf{(it*it).toDouble()}).toFloat();require(length>0f)
            for(i in 0..2)axis[i]/=length
            val helper=if(abs(axis[2])<.9f)floatArrayOf(0f,0f,1f)else floatArrayOf(0f,1f,0f)
            fun cross(x:FloatArray,y:FloatArray)=floatArrayOf(x[1]*y[2]-x[2]*y[1],x[2]*y[0]-x[0]*y[2],x[0]*y[1]-x[1]*y[0])
            val u=cross(axis,helper);val ul=sqrt(u.sumOf{(it*it).toDouble()}).toFloat();for(i in 0..2)u[i]/=ul;val vv=cross(axis,u)
            for(k in 0 until 12){val angles=listOf(k*2*PI/12,(k+1)*2*PI/12)
                val ns=angles.map{angle->FloatArray(3){u[it]*cos(angle).toFloat()+vv[it]*sin(angle).toFloat()}}
                val p=listOf(FloatArray(3){a[it]+r*ns[0][it]},FloatArray(3){a[it]+r*ns[1][it]},FloatArray(3){b[it]+r*ns[1][it]},FloatArray(3){b[it]+r*ns[0][it]})
                for(i in intArrayOf(0,1,2,0,2,3))vertex(p[i],ns[if(i==0||i==3)0 else 1])
                tri(a,p[1],p[0],FloatArray(3){-axis[it]});tri(b,p[3],p[2],axis)}
        }
        fun ring(x:Float,y:Float,z:Float,r:Float,thickness:Float){for(i in 0 until 24){val a=i*2*PI/24;val b=(i+1)*2*PI/24
            cylinder(floatArrayOf(x+cos(a).toFloat()*r,y+sin(a).toFloat()*r,z),floatArrayOf(x+cos(b).toFloat()*r,y+sin(b).toFloat()*r,z),thickness)}}
        fun arc(x:Float,y:Float,z:Float,r:Float,thickness:Float){for(i in 0 until 12){val a=i*PI/12;val b=(i+1)*PI/12
            cylinder(floatArrayOf(x+cos(a).toFloat()*r,y,z+sin(a).toFloat()*r*.22f),floatArrayOf(x+cos(b).toFloat()*r,y,z+sin(b).toFloat()*r*.22f),thickness)}}
        fun foldedSheet(x:Float,y:Float,z:Float,w:Float,h:Float,fold:Float){
            for(i in 0 until 10){val x0=x-w/2+w*i/10;val x1=x-w/2+w*(i+1)/10
                val z0=z+sin(i*PI*.6).toFloat()*fold;val z1=z+sin((i+1)*PI*.6).toFloat()*fold
                val a=floatArrayOf(x0,y-h/2,z0);val b=floatArrayOf(x1,y-h/2,z1);val c=floatArrayOf(x1,y+h/2,z1);val d=floatArrayOf(x0,y+h/2,z0)
                val n=floatArrayOf(-(z1-z0)/(x1-x0),0f,1f);val len=sqrt(n[0]*n[0]+1f);n[0]/=len;n[2]/=len;tri(a,b,c,n);tri(a,c,d,n)}
        }
        fun cross(x:Float,y:Float,z:Float,r:Float){cylinder(floatArrayOf(x-r,y-r,z),floatArrayOf(x+r,y+r,z),.006f);cylinder(floatArrayOf(x-r,y+r,z),floatArrayOf(x+r,y-r,z),.006f)}
        fun zigzag(x:Float,y:Float,z:Float){for(i in 0..2)cylinder(floatArrayOf(x+if(i%2==0)-.018f else .018f,y+i*.027f,z),floatArrayOf(x+if(i%2==0).018f else -.018f,y+(i+1)*.027f,z),.004f)}
        fun pose(kind:String){
            // A small articulated instruction mannequin on a separate board, never a deformed anatomy mesh.
            val cx=.54f;val hip=floatArrayOf(cx,.99f,-.018f)
            val shoulder=if(kind=="sit-guide")floatArrayOf(cx,1.12f,.22f)else floatArrayOf(cx,1.23f,-.005f)
            val head=if(kind=="sit-guide")floatArrayOf(cx,1.17f,.30f)else floatArrayOf(cx,1.34f,.005f)
            cylinder(hip,shoulder,.039f);ellipsoid(head[0],head[1],head[2],.041f,.052f,.041f)
            if(kind=="recovery-guide"){
                // Bent upper knee stabilises the side pose; hand reaches the cheek, free arm forward.
                val knee=floatArrayOf(cx+.10f,.88f,.01f);val foot=floatArrayOf(cx+.06f,.74f,.015f)
                cylinder(hip,knee,.022f);cylinder(knee,foot,.018f)
                cylinder(hip,floatArrayOf(cx,.76f,-.017f),.021f)
                cylinder(shoulder,floatArrayOf(cx+.10f,1.20f,.005f),.016f)
                cylinder(floatArrayOf(cx+.10f,1.20f,.005f),floatArrayOf(cx+.035f,1.32f,.01f),.015f)
                cylinder(shoulder,floatArrayOf(cx-.10f,1.23f,-.025f),.016f)
                cylinder(floatArrayOf(cx-.10f,1.23f,-.025f),floatArrayOf(cx-.10f,1.33f,-.025f),.014f)
            }else{
                for(dx in listOf(-.025f,.025f)){
                    val knee=floatArrayOf(cx+dx,.87f,if(kind=="legs-guide").055f else -.014f)
                    val foot=floatArrayOf(cx+dx,.72f,if(kind=="legs-guide").10f else -.014f)
                    cylinder(floatArrayOf(hip[0]+dx,hip[1],hip[2]),knee,.019f);cylinder(knee,foot,.017f)}
                for(dx in listOf(-.055f,.055f))cylinder(floatArrayOf(shoulder[0]+dx,shoulder[1],shoulder[2]),floatArrayOf(cx+dx,1f,-.014f),.015f)
            }
        }
        fun part(id:String,name:String,c:FloatArray,zOffset:Float):AnatomyPart {
            val data=v.toFloatArray();require(data.isNotEmpty());for(i in data.indices step 6)data[i+2]+=zOffset
            val b=FloatArray(6){axis->val a=axis%3;if(axis<3)(data.indices step 6).minOf{data[it+a]}else(data.indices step 6).maxOf{data[it+a]}}
            return AnatomyPart(id,name,"teaching","Original schematic teaching illustration; not measured patient physiology.",b,c,data)
        }
    }
}
