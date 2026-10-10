package com.narvyn.suraksha

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnatomyPackFlowTest {
    @Test fun bundledDetailCanPauseResumeRecoverFromCorruptionAndBeRemovedWithoutLosingCore() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=AnatomyPackStore(context)
        // This test must never replace a user's existing optional pack.
        assumeTrue(!store.installed()&&!store.paused()&&!File(context.filesDir,"content-packs/body-detail").exists())
        val prefs=context.getSharedPreferences("anatomy-quality",0);val hadChoice=prefs.contains("detailed");val oldChoice=prefs.getBoolean("detailed",false)
        val base=AnatomyData.load(context)
        try {
            var cancelled=false
            try { store.install({cancelled}){_,progress->if(progress>=35)cancelled=true};fail("Preparation did not pause") }catch(_:CancellationException){}
            assertTrue(store.paused());assertFalse(store.installed());assertFalse(store.detailedSelected())
            store.install({false}){_,_->}
            assertTrue(store.installed());assertTrue(store.detailedSelected());assertFalse(store.paused())
            val detail=AnatomyData.load(context)
            assertEquals("Detailed",detail.manifest.getString("loadedQuality"))
            assertEquals(base.parts.map{it.id}.toSet(),detail.parts.map{it.id}.toSet())
            assertTrue(detail.parts.sumOf{it.vertices.size}>base.parts.sumOf{it.vertices.size})
            val first=detail.manifest.getJSONArray("parts").getJSONObject(0)
            val corrupted=PackIntegrity.safeFile(store.activeDirectory()!!,first.getString("file"))
            corrupted.writeBytes(ByteArray(first.getInt("bytes")))
            val fallback=AnatomyData.load(context)
            assertEquals("Balanced",fallback.manifest.getString("loadedQuality"))
            assertTrue(fallback.manifest.getString("qualityFallback").isNotBlank())
            assertFalse("Corrupt content must be eligible for reinstall",store.installed())
            assertFalse(store.requestedDetailed())
            store.install({false}){_,_->};assertTrue(store.installed())
            store.remove();assertFalse(store.installed());assertFalse(store.paused())
            assertEquals(base.parts.map{it.id},AnatomyData.load(context).parts.map{it.id})
        }finally{
            store.remove()
            prefs.edit().apply{if(hadChoice)putBoolean("detailed",oldChoice)else remove("detailed")}.commit()
        }
    }
}
