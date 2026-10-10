package com.narvyn.suraksha

import kotlin.math.*

/** Shared source-to-floor geometry for a lying teaching model, not a positioning procedure. */
internal object CasualtyGeometry {
    fun point(p:FloatArray, roll:Float, lift:Float):FloatArray {
        val r=Math.toRadians(roll.toDouble());val c=cos(r).toFloat();val s=sin(r).toFloat()
        return floatArrayOf(p[0]*c+p[2]*s, -p[0]*s+p[2]*c+lift, -p[1])
    }
    fun lift(bounds:FloatArray,roll:Float):Float {
        val r=Math.toRadians(roll.toDouble());val c=cos(r).toFloat();val s=sin(r).toFloat()
        val x=if(-s>=0)bounds[0]else bounds[3];val z=if(c>=0)bounds[2]else bounds[5]
        return -(-x*s+z*c)+.02f
    }
    fun bounds(source:FloatArray,roll:Float,lift:Float):FloatArray {
        val corners=listOf(source[0],source[3]).flatMap{x->listOf(source[1],source[4]).flatMap{y->listOf(source[2],source[5]).map{z->point(floatArrayOf(x,y,z),roll,lift)}}}
        return FloatArray(6){axis->if(axis<3)corners.minOf{it[axis]}else corners.maxOf{it[axis-3]}}
    }
}

/** Non-graphic, labelled cue markers. Their colour does not diagnose a real injury. */
internal object CasualtyMarkers {
    val centres=mapOf("CHEST" to floatArrayOf(0f,1.30f,.17f),"HEAD" to floatArrayOf(0f,1.64f,.14f),
        "RIGHT_FOREARM" to floatArrayOf(-.265f,1.00f,.09f),"LEFT_FOREARM" to floatArrayOf(.265f,1.00f,.09f),
        "RIGHT_LOWER_LEG" to floatArrayOf(-.09f,.35f,.11f),"WHOLE_BODY" to floatArrayOf(0f,1.10f,.21f))
    fun hit(region:String,origin:FloatArray,direction:FloatArray):Boolean {
        val centre=centres[region]?:return false
        if(abs(direction[2])<1e-6f)return false
        val t=(centre[2]-origin[2])/direction[2]
        if(t<0f)return false
        val x=origin[0]+direction[0]*t-centre[0];val y=origin[1]+direction[1]*t-centre[1]
        val radius=if(region=="CHEST"||region=="WHOLE_BODY").09f else .068f
        return x*x+y*y<=radius*radius
    }
    /** Small authored visual cues, deliberately non-graphic and not injury classification. */
    val injuryParts:List<AnatomyPart> by lazy {
        listOf(Triple("bleeding","RIGHT_FOREARM",floatArrayOf(.48f,.09f,.10f)),
            Triple("scald","LEFT_FOREARM",floatArrayOf(.77f,.26f,.21f)),
            Triple("limb","RIGHT_LOWER_LEG",floatArrayOf(.43f,.28f,.36f))).map{(id,region,colour)->
            val centre=centres.getValue(region);val v=mutableListOf<Float>()
            fun point(a:Double,r:Float):FloatArray = floatArrayOf(centre[0]+cos(a).toFloat()*.038f*r,centre[1]+sin(a).toFloat()*.066f*r,centre[2]-.016f+.007f*(1-r))
            for(i in 0 until 40){val a=i*2*PI/40;val b=(i+1)*2*PI/40
                for(p in listOf(point(a,0f),point(a,1f),point(b,1f))){v.addAll(p.toList());v.addAll(listOf(0f,0f,1f))}
            }
            AnatomyPart("injury:$id","Authored $id cue","cue-detail","Non-graphic authored cue; not a diagnostic appearance.",
                floatArrayOf(centre[0]-.04f,centre[1]-.07f,centre[2]-.02f,centre[0]+.04f,centre[1]+.07f,centre[2]),colour,v.toFloatArray())
        }
    }
    val parts:List<AnatomyPart> by lazy { centres.map{(region,c)->
        val values=mutableListOf<Float>();val radius=if(region=="CHEST"||region=="WHOLE_BODY").075f else .052f
        for(i in 0 until 40){val a=i*2*PI/40;val b=(i+1)*2*PI/40
            for((r,t) in listOf(radius to a,radius to b,radius*.65f to a,radius*.65f to a,radius to b,radius*.65f to b)){
                values.addAll(listOf(c[0]+cos(t).toFloat()*r,c[1]+sin(t).toFloat()*r,c[2],0f,0f,1f))
            }
        }
        AnatomyPart("cue:$region","Simulated ${region.lowercase().replace('_',' ')} cue","cue","Authored simulation cue; tap to inspect.",
            floatArrayOf(c[0]-radius,c[1]-radius,c[2]-.005f,c[0]+radius,c[1]+radius,c[2]+.005f),floatArrayOf(.94f,.50f,.12f),values.toFloatArray())
    }}
    val mat:AnatomyPart by lazy {
        val v=mutableListOf<Float>();val points=listOf(floatArrayOf(-.48f,.001f,.13f),floatArrayOf(.48f,.001f,.13f),floatArrayOf(.48f,.001f,-1.87f),floatArrayOf(-.48f,.001f,-1.87f))
        for(i in listOf(0,1,2,0,2,3)){v.addAll(points[i].toList());v.addAll(listOf(0f,1f,0f))}
        AnatomyPart("casualty-mat","Illustrative practice mat","environment","Virtual practice-space reference.",floatArrayOf(-.48f,0f,-1.87f,.48f,.001f,.13f),floatArrayOf(.77f,.83f,.87f),v.toFloatArray())
    }
}
