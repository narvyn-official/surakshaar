package com.narvyn.suraksha

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** Fixed balanced subset for stereo. Does not change the learner's handheld model-quality choice. */
internal object SpatialBodyAssets {
    fun loadBody(context:Context):AnatomyData {
        val manifest=JSONObject(context.assets.open("anatomy/manifest.json").bufferedReader().use{it.readText()})
        val steps=SpatialCatalog(context).data.getJSONObject("steps")
        val ids=steps.keys().asSequence().map{steps.getJSONObject(it)}.filter{it.optString("model")=="anatomy"}
            .map{it.optString("part")}.filter{it.startsWith("FMA")}.toSet()+"FMA7163"
        check(ids.size<=12){"Headset anatomy exceeds its bounded asset budget"}
        var decodedBytes=0L
        val parts=manifest.getJSONArray("parts").objects().filter{it.getString("id") in ids}.map{p->
            if(Thread.currentThread().isInterrupted)throw java.util.concurrent.CancellationException()
            val bytes=context.assets.open(p.getString("file")).use{PackIntegrity.readBounded(it,p.getInt("bytes"))}
            check(bytes.size==p.getInt("bytes"))
            check(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==p.getString("sha256"))
            val mesh=AnatomyMeshDecoder.decode(bytes,36_000_000-decodedBytes);decodedBytes+=mesh.rawBytes
            fun values(key:String)=p.getJSONArray(key).let{a->FloatArray(a.length()){a.getDouble(it).toFloat()}}
            AnatomyClothing.protectedSkin(AnatomyPart(p.getString("id"),p.getString("name"),p.getString("layer"),p.getString("explanation"),values("bounds"),values("colour"),mesh.vertices))
        }
        check(parts.map{it.id}.toSet()==ids){"A lesson's anatomical structure is unavailable"}
        return AnatomyData(JSONObject(manifest.toString()).put("loadedQuality","Headset balanced"),parts)
    }
}
