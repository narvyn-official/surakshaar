package com.narvyn.suraksha

import android.content.Context
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

internal data class AnatomyPart(val id:String,val name:String,val layer:String,val explanation:String,val bounds:FloatArray,val colour:FloatArray,val vertices:FloatArray)
internal class AnatomyData(val manifest:JSONObject,val parts:List<AnatomyPart>) {
    val height=manifest.getDouble("heightMetres").toFloat()
    companion object {
        fun load(context:Context):AnatomyData {
            val store=AnatomyPackStore(context)
            val requested=store.requestedDetailed()
            store.activeDirectory()?.let { directory ->
                runCatching { return loadPack({name->PackIntegrity.safeFile(directory,name).inputStream()},"manifest.json","Detailed") }
                store.invalidate()
                return loadPack({name->context.assets.open(name)},"anatomy/manifest.json","Balanced").also { it.manifest.put("qualityFallback","The detailed pack could not be verified. Balanced models are in use; reinstall the optional pack in Offline content.") }
            }
            if(requested){store.invalidate();return loadPack({name->context.assets.open(name)},"anatomy/manifest.json","Balanced").also { it.manifest.put("qualityFallback","The detailed pack is incomplete. Balanced models are in use; reinstall the optional pack in Offline content.") }}
            return loadPack({name->context.assets.open(name)},"anatomy/manifest.json","Balanced")
        }
        private fun loadPack(open:(String)->java.io.InputStream,manifestPath:String,quality:String):AnatomyData {
            val manifest=JSONObject(open(manifestPath).use{String(PackIntegrity.readBounded(it,1_000_000),Charsets.UTF_8)})
            val entries=manifest.getJSONArray("parts").objects();require(entries.size in 1..1024)
            require(entries.map{it.getString("id")}.toSet().size==entries.size)
            var totalBytes=0L
            val parts=entries.map { p->
                require(p.getInt("bytes") in 1..32_000_000)
                val bytes=open(p.getString("file")).use{PackIntegrity.readBounded(it,p.getInt("bytes"))}
                check(bytes.size==p.getInt("bytes")) {"Anatomy pack size mismatch"}
                val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
                check(hash==p.getString("sha256")){"Anatomy pack integrity failure"}
                val raw=GZIPInputStream(bytes.inputStream()).use{PackIntegrity.readBounded(it,24_000_008)}
                totalBytes+=raw.size;require(totalBytes<=90_000_000){"Anatomy pack exceeds the memory budget"}
                val buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
                check(String(raw.copyOfRange(0,4),Charsets.US_ASCII)=="ARA1")
                buffer.position(4);val count=buffer.int;check(count in 3..1_000_000 && count%3==0 && raw.size==8+count*24)
                val vertices=FloatArray(count*6);buffer.asFloatBuffer().get(vertices);check(vertices.all{it.isFinite()})
                fun numbers(key:String)=p.getJSONArray(key).let{a->FloatArray(a.length()){a.getDouble(it).toFloat()}}
                AnatomyPart(p.getString("id"),p.getString("name"),p.getString("layer"),p.getString("explanation"),numbers("bounds"),numbers("colour"),vertices)
            }
            manifest.put("loadedQuality",quality)
            return AnatomyData(manifest,parts)
        }
    }
}

/** Nearest triangle intersection in model coordinates, with bounds rejection. Hidden meshes never enter this function. */
internal object AnatomyPicking {
    fun distance(vertices:FloatArray,bounds:FloatArray,origin:FloatArray,direction:FloatArray):Float? {
        if((origin+direction).any{!it.isFinite()})return null
        var near=0f;var far=Float.POSITIVE_INFINITY
        for(i in 0..2){
            if(kotlin.math.abs(direction[i])<1e-8f){if(origin[i]<bounds[i]||origin[i]>bounds[i+3])return null}
            else {val a=(bounds[i]-origin[i])/direction[i];val b=(bounds[i+3]-origin[i])/direction[i];near=maxOf(near,minOf(a,b));far=minOf(far,maxOf(a,b));if(far<near)return null}
        }
        var closest=Float.POSITIVE_INFINITY
        for(i in vertices.indices step 18){
            val ax=vertices[i];val ay=vertices[i+1];val az=vertices[i+2]
            val e1x=vertices[i+6]-ax;val e1y=vertices[i+7]-ay;val e1z=vertices[i+8]-az
            val e2x=vertices[i+12]-ax;val e2y=vertices[i+13]-ay;val e2z=vertices[i+14]-az
            val px=direction[1]*e2z-direction[2]*e2y;val py=direction[2]*e2x-direction[0]*e2z;val pz=direction[0]*e2y-direction[1]*e2x
            val det=e1x*px+e1y*py+e1z*pz;if(kotlin.math.abs(det)<1e-8f)continue
            val tx=origin[0]-ax;val ty=origin[1]-ay;val tz=origin[2]-az
            val u=(tx*px+ty*py+tz*pz)/det;if(u<0||u>1)continue
            val qx=ty*e1z-tz*e1y;val qy=tz*e1x-tx*e1z;val qz=tx*e1y-ty*e1x
            val v=(direction[0]*qx+direction[1]*qy+direction[2]*qz)/det;if(v<0||u+v>1)continue
            val d=(e2x*qx+e2y*qy+e2z*qz)/det;if(d>=0)closest=minOf(closest,d)
        }
        return closest.takeIf{it.isFinite()}
    }
}
