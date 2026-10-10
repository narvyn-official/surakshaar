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
        // Verified geometry contains no learner data and is read-only after construction.
        // Sharing live arrays prevents overlapping Activities from duplicating a detailed pack.
        // A weak reference lets Android reclaim everything when no scene needs it.
        private var cachedKey=""
        private var cached=java.lang.ref.WeakReference<AnatomyData>(null)
        private fun shared(key:String,read:()->AnatomyData):AnatomyData {
            cached.get()?.takeIf{cachedKey==key}?.let{return it}
            return read().also{cachedKey=key;cached=java.lang.ref.WeakReference(it)}
        }
        @Synchronized fun load(context:Context):AnatomyData {
            if(Thread.currentThread().isInterrupted)throw java.util.concurrent.CancellationException("Anatomy loading cancelled")
            val store=AnatomyPackStore(context)
            val preparationFallback=store.prepareDefaultQuality { Thread.currentThread().isInterrupted }
            fun balanced(message:String?=null):AnatomyData {
                val model=shared("bundled-balanced"){loadPack({name->context.assets.open(name)},"anatomy/manifest.json","Balanced")}
                return if(message==null)model else AnatomyData(JSONObject(model.manifest.toString()).put("qualityFallback",message),model.parts)
            }
            val requested=store.requestedDetailed()
            store.activeDirectory()?.let { directory ->
                try { return shared("detailed:${directory.canonicalPath}:${store.catalogue?.optString("sha256")}"){loadPack({name->PackIntegrity.safeFile(directory,name).inputStream()},"manifest.json","Detailed")} }
                catch(cancelled:java.util.concurrent.CancellationException){throw cancelled}
                catch(invalid:Exception){ /* Verified in-memory geometry is reused; new input must pass integrity checks. */ }
                store.invalidate()
                return balanced("The detailed pack could not be verified. Balanced models are in use; reinstall the optional pack in Offline content.")
            }
            if(requested){store.invalidate();return balanced("The detailed pack is incomplete. Balanced models are in use; reinstall the optional pack in Offline content.")}
            return balanced(preparationFallback)
        }
        private fun loadPack(open:(String)->java.io.InputStream,manifestPath:String,quality:String):AnatomyData {
            val manifest=JSONObject(open(manifestPath).use{String(PackIntegrity.readBounded(it,1_000_000),Charsets.UTF_8)})
            val entries=manifest.getJSONArray("parts").objects();require(entries.size in 1..1024)
            require(entries.map{it.getString("id")}.toSet().size==entries.size)
            var totalBytes=0L
            val parts=entries.map { p->
                if(Thread.currentThread().isInterrupted)throw java.util.concurrent.CancellationException("Anatomy loading cancelled")
                require(p.getInt("bytes") in 1..32_000_000)
                val bytes=open(p.getString("file")).use{PackIntegrity.readBounded(it,p.getInt("bytes"))}
                check(bytes.size==p.getInt("bytes")) {"Anatomy pack size mismatch"}
                val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
                check(hash==p.getString("sha256")){"Anatomy pack integrity failure"}
                val decoded=AnatomyMeshDecoder.decode(bytes,90_000_000-totalBytes)
                totalBytes+=decoded.rawBytes
                val vertices=decoded.vertices
                fun numbers(key:String)=p.getJSONArray(key).let{a->FloatArray(a.length()){a.getDouble(it).toFloat()}}
                AnatomyPart(p.getString("id"),p.getString("name"),p.getString("layer"),p.getString("explanation"),numbers("bounds"),numbers("colour"),vertices).let(AnatomyClothing::protectedSkin)
            }
            manifest.put("loadedQuality",quality)
            return AnatomyData(manifest,parts)
        }
    }
}

/** Stream into primitive storage; enforce the decompressed budget before allocating geometry. */
internal object AnatomyMeshDecoder {
    data class Decoded(val vertices:FloatArray,val rawBytes:Long)
    fun decode(bytes:ByteArray,remainingBudget:Long):Decoded=GZIPInputStream(bytes.inputStream()).use { input ->
        fun readFully(target:ByteArray,length:Int){var offset=0;while(offset<length){val n=input.read(target,offset,length-offset);check(n>0){"Anatomy mesh is truncated"};offset+=n}}
        val header=ByteArray(8);readFully(header,8)
        check(String(header,0,4,Charsets.US_ASCII)=="ARA1")
        val count=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4)
        check(count in 3..1_000_000 && count%3==0)
        val rawBytes=8L+count*24L;require(rawBytes<=remainingBudget){"Anatomy pack exceeds the memory budget"}
        // A growing decompressed ByteArray used to duplicate the largest skin mesh
        // during scene transitions. Only a small staging buffer is needed here.
        val result=FloatArray(count*6);val chunk=ByteArray(24*1024);var offset=0
        while(offset<result.size){
            if(Thread.currentThread().isInterrupted)throw java.util.concurrent.CancellationException("Anatomy loading cancelled")
            val floats=minOf(chunk.size/4,result.size-offset);readFully(chunk,floats*4)
            ByteBuffer.wrap(chunk,0,floats*4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(result,offset,floats);offset+=floats
        }
        check(input.read()==-1){"Unexpected trailing anatomy data"};check(result.all{it.isFinite()});Decoded(result,rawBytes)
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
