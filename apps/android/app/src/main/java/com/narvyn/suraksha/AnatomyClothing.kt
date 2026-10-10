package com.narvyn.suraksha

import kotlin.math.*

/** Authored opaque training shorts, shared by exploration, placement previews and casualty poses. */
internal object AnatomyClothing {
    private const val LOW = .65f
    private const val HIGH = 1.01f
    private const val HALF_WIDTH = .178f

    /** Remove the covered external skin itself: coverage must survive close-up / inside-mesh views. */
    fun coveredTriangle(v:FloatArray, offset:Int):Boolean {
        val lowY=minOf(v[offset+1],v[offset+7],v[offset+13])
        val highY=maxOf(v[offset+1],v[offset+7],v[offset+13])
        val lowX=minOf(v[offset],v[offset+6],v[offset+12])
        val highX=maxOf(v[offset],v[offset+6],v[offset+12])
        return highY>=LOW && lowY<=HIGH && highX>=-HALF_WIDTH && lowX<=HALF_WIDTH
    }
    fun protectedSkin(part:AnatomyPart):AnatomyPart {
        if(part.layer!="skin")return part
        var kept=0
        for(i in part.vertices.indices step 18)if(!coveredTriangle(part.vertices,i))kept+=18
        val vertices=FloatArray(kept);var to=0
        for(i in part.vertices.indices step 18)if(!coveredTriangle(part.vertices,i)){
            part.vertices.copyInto(vertices,to,i,i+18);to+=18
        }
        return part.copy(vertices=vertices)
    }

    private data class Ring(val y:Float,val rx:Float,val rz:Float,val x:Float=0f,val z:Float=-.018f)
    private fun shell(id:String,rings:List<Ring>,colour:FloatArray,folds:Boolean=true):AnatomyPart {
        val vertices=ArrayList<Float>();val segments=80
        data class Face(val points:List<FloatArray>,val normal:FloatArray,val smooth:Boolean)
        val faces=ArrayList<Face>();val sums=HashMap<List<Int>,FloatArray>()
        fun key(p:FloatArray)=p.map{(it*1_000_000).roundToInt()}
        fun point(r:Ring,angle:Double):FloatArray {
            // Shallow longitudinal cloth folds, not added anatomical detail.
            val fold=if(folds).0016f*(sin(9*angle+r.y*7).toFloat()+.35f*sin(17*angle).toFloat())else 0f
            return floatArrayOf(r.x+(r.rx+fold)*cos(angle).toFloat(),r.y,r.z+(r.rz+fold)*sin(angle).toFloat())
        }
        fun triangle(a:FloatArray,b:FloatArray,c:FloatArray,smooth:Boolean=true){
            val ux=b[0]-a[0];val uy=b[1]-a[1];val uz=b[2]-a[2]
            val vx=c[0]-a[0];val vy=c[1]-a[1];val vz=c[2]-a[2]
            val nx=uy*vz-uz*vy;val ny=uz*vx-ux*vz;val nz=ux*vy-uy*vx
            val length=sqrt(nx*nx+ny*ny+nz*nz)
            if(length<1e-9f)return
            val points=listOf(a,b,c);val n=floatArrayOf(nx/length,ny/length,nz/length)
            faces+=Face(points,n,smooth)
            if(smooth)for(p in points){val sum=sums.getOrPut(key(p)){FloatArray(3)};for(axis in 0..2)sum[axis]+=n[axis]}
        }
        for(i in 0 until segments){
            val a=i*2*PI/segments;val b=(i+1)*2*PI/segments
            for(j in 0 until rings.lastIndex){
                val p=point(rings[j],a);val q=point(rings[j],b);val r=point(rings[j+1],b);val t=point(rings[j+1],a)
                triangle(p,q,r);triangle(p,r,t)
            }
            // Both ends are closed. Viewing up a hem or down the waistband cannot reveal skin.
            val top=rings.first();val bottom=rings.last()
            triangle(floatArrayOf(top.x,top.y,top.z),point(top,b),point(top,a),false)
            triangle(floatArrayOf(bottom.x,bottom.y,bottom.z),point(bottom,a),point(bottom,b),false)
        }
        for(face in faces)for(p in face.points){
            val n=if(face.smooth)sums.getValue(key(p))else face.normal
            val length=sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2])
            vertices.addAll(p.toList());vertices.addAll(n.map{it/length})
        }
        val v=vertices.toFloatArray()
        val bounds=FloatArray(6){axis->if(axis<3)(axis until v.size step 6).minOf{v[it]}else(axis-3 until v.size step 6).maxOf{v[it]}}
        return AnatomyPart(id,"Training shorts","clothing","Opaque illustrative clothing; not an anatomical structure.",bounds,colour,v)
    }

    val parts:List<AnatomyPart> by lazy {
        val navy=floatArrayOf(.12f,.23f,.32f);val trim=floatArrayOf(.19f,.34f,.44f)
        val result=mutableListOf(shell("shorts:hips",listOf(
            Ring(1.078f,.177f,.139f),Ring(1.04f,.182f,.144f),Ring(.96f,.190f,.165f),
            Ring(.87f,.192f,.165f),Ring(.78f,.185f,.147f),Ring(.735f,.182f,.140f)),navy))
        for(side in listOf(-1,1)){
            result+=shell("shorts:leg:$side",listOf(Ring(.835f,.102f,.151f,side*.088f),
                Ring(.72f,.102f,.135f,side*.088f),Ring(.63f,.098f,.122f,side*.088f),
                Ring(.57f,.094f,.112f,side*.088f)),navy)
            result+=shell("shorts:hem:$side",listOf(Ring(.590f,.096f,.115f,side*.088f),Ring(.568f,.096f,.115f,side*.088f)),trim,false)
        }
        result+=shell("shorts:waist",listOf(Ring(1.081f,.179f,.141f),Ring(1.052f,.184f,.146f)),trim,false)
        result
    }
}
