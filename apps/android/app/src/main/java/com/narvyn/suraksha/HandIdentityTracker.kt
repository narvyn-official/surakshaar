package com.narvyn.suraksha

import kotlin.math.hypot

/** Conservative screen-space identity association, not biometric identity or metric hand pose. */
internal class HandIdentityTracker {
    private var previous=emptyList<TrackedHand>()
    private var epoch=Long.MIN_VALUE
    private var next=0L
    fun associate(input:List<TrackedHand>,generation:Long,viewAspectRatio:Float=1f):Pair<List<TrackedHand>,Boolean> {
        if(generation!=epoch){previous=emptyList();epoch=generation;next=0}
        require(input.size<=2)
        if(input.isEmpty()){previous=emptyList();return emptyList<TrackedHand>() to true}
        fun distance(a:TrackedHand,b:TrackedHand):Float {
            val aw=a.landmarks[0];val bw=b.landmarks[0]
            return hypot((aw.x-bw.x)*viewAspectRatio,aw.y-bw.y)
        }
        // Crossing/overlapping wrists cannot safely preserve an in-progress gesture's owner.
        var stable= !(input.size==2 && distance(input[0],input[1])<.10f)
        val used=HashSet<String>()
        val output=input.map { hand ->
            val candidates=previous.filter { it.id !in used &&
                (it.handedness==hand.handedness || it.handedness==Handedness.UNKNOWN || hand.handedness==Handedness.UNKNOWN) }
                .map { it to distance(it,hand) }.filter { it.second<.25f }.sortedBy { it.second }
            val ambiguous=candidates.size>1 && candidates[1].second-candidates[0].second<.06f
            if(ambiguous)stable=false
            val match=if(stable&&!ambiguous)candidates.firstOrNull()?.first else null
            val id=match?.id ?: "hand-$generation-${++next}"
            used+=id
            TrackedHand(id,hand.handedness,hand.confidence,hand.landmarks)
        }
        // A distinct second hand can join a held primary hand. A replacement/jump cannot inherit it.
        if(previous.any{old->output.none{it.id==old.id}} && output.any{fresh->previous.none{it.id==fresh.id}})stable=false
        previous=output
        return output to stable
    }
}
