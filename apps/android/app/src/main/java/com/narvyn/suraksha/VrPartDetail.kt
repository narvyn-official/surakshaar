package com.narvyn.suraksha

/** A larger specimen uses the original structure, alongside its smaller whole-body context. */
internal data class VrPartDetail(val centre:FloatArray,val surfacePoint:FloatArray,val scale:Float) {
    companion object {
        fun forPart(part:AnatomyPart):VrPartDetail {
            val b=part.bounds
            val centre=FloatArray(3){(b[it]+b[it+3])/2}
            val extent=(0..2).maxOf{b[it+3]-b[it]}.coerceAtLeast(.01f)
            var front=Float.NEGATIVE_INFINITY;var point=centre
            for(i in part.vertices.indices step 18){
                val z=(part.vertices[i+2]+part.vertices[i+8]+part.vertices[i+14])/3
                if(z>front){front=z;point=FloatArray(3){axis->(part.vertices[i+axis]+part.vertices[i+6+axis]+part.vertices[i+12+axis])/3}}
            }
            return VrPartDetail(centre,point,.72f/extent)
        }
    }
}
