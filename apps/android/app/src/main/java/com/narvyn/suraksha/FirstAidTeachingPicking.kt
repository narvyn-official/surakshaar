package com.narvyn.suraksha

/** Distances are parameters along identical world near/far rays, not normalised local metres. */
internal object FirstAidTeachingPicking {
    fun front(teachingHits:List<Pair<String,Float>>,bodyDistance:Float?):String? {
        val nearest=teachingHits.filter{it.second.isFinite()&&it.second>=0f}.minByOrNull{it.second}?:return null
        return nearest.first.takeIf{bodyDistance==null||nearest.second<=bodyDistance+0.000001f}
    }
}
