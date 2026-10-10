package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class AnatomyMeshDecoderTest {
    private fun pack(count:Int,values:FloatArray=FloatArray(count*6){it*.001f},extra:ByteArray=byteArrayOf()):ByteArray {
        val raw=ByteBuffer.allocate(8+values.size*4+extra.size).order(ByteOrder.LITTLE_ENDIAN)
        raw.put("ARA1".toByteArray());raw.putInt(count);values.forEach{raw.putFloat(it)};raw.put(extra)
        return ByteArrayOutputStream().also{out->GZIPOutputStream(out).use{it.write(raw.array())}}.toByteArray()
    }
    @Test fun decodesAcrossStagingChunksWithoutChangingValues(){
        val values=FloatArray(1200*6){it*.001f};val decoded=AnatomyMeshDecoder.decode(pack(1200,values),100_000)
        assertArrayEquals(values,decoded.vertices,0f);assertEquals(8L+values.size*4,decoded.rawBytes)
    }
    @Test fun rejectsBudgetBeforeAllocatingTheDeclaredMesh(){assertThrows(IllegalArgumentException::class.java){AnatomyMeshDecoder.decode(pack(999_999,floatArrayOf()),1000)}}
    @Test fun rejectsTruncatedAndTrailingVertices(){
        assertThrows(IllegalStateException::class.java){AnatomyMeshDecoder.decode(pack(3,FloatArray(12)),1000)}
        assertThrows(IllegalStateException::class.java){AnatomyMeshDecoder.decode(pack(3,extra=byteArrayOf(1)),1000)}
    }
    @Test fun rejectsInvalidCountAndNonFiniteGeometry(){
        assertThrows(IllegalStateException::class.java){AnatomyMeshDecoder.decode(pack(4),1000)}
        assertThrows(IllegalStateException::class.java){AnatomyMeshDecoder.decode(pack(3,FloatArray(18){Float.NaN}),1000)}
    }
    @Test fun respectsLoaderCancellation(){
        try {Thread.currentThread().interrupt();assertThrows(java.util.concurrent.CancellationException::class.java){AnatomyMeshDecoder.decode(pack(3),1000)}}finally{Thread.interrupted()}
    }
}
