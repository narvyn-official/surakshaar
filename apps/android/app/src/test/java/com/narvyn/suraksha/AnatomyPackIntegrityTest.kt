package com.narvyn.suraksha

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AnatomyPackIntegrityTest {
    private fun archive(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> entries.forEach { (name, data) -> zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry() } }
    }.toByteArray()
    private fun directory(block: (File) -> Unit) { val root = Files.createTempDirectory("anatomy-pack-test").toFile(); try { block(root) } finally { root.deleteRecursively() } }
    @Test fun traversalAndAbsolutePathsCannotEscapeStorage() = directory { root ->
        listOf("../other/mesh.meshpack", "/tmp/model.meshpack", "anatomy/../../file", "anatomy\\body.meshpack", "anatomy//body.meshpack").forEach { path ->
            try { PackIntegrity.safeFile(root, path); fail("Accepted unsafe path $path") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(File(root, "anatomy/body.meshpack"), PackIntegrity.safeFile(root, "anatomy/body.meshpack"))
    }
    @Test fun expansionIsBoundedAndUnexpectedFilesAreRejected() = directory { root ->
        val bomb = archive("manifest.json" to ByteArray(2048))
        try { PackIntegrity.extract(bomb.inputStream(), root, 1024, { false }) { _, _ -> }; fail("Expanded past catalogue limit") } catch (_: IllegalArgumentException) { }
        assertFalse(File(root, "manifest.json").exists())
        val executable = archive("manifest.json" to "{}".toByteArray(), "anatomy/execute.sh" to "command".toByteArray())
        try { PackIntegrity.extract(executable.inputStream(), root, 4096, { false }) { _, _ -> }; fail("Accepted an unexpected asset") } catch (_: IllegalArgumentException) { }
        assertFalse(File(root, "anatomy/execute.sh").exists())
    }
    @Test fun resumeKeepsVerifiedFilesAndReplacesCorruption() = directory { root ->
        val model = ByteArray(48_000) { (it % 251).toByte() }
        val manifest = JSONObject().put("parts", JSONArray().put(JSONObject().put("id", "test").put("file", "anatomy/body.meshpack").put("bytes", model.size).put("sha256", PackIntegrity.sha256(model)))).toString().toByteArray()
        val zip = archive("manifest.json" to manifest, "anatomy/body.meshpack" to model, "LICENSE.txt" to "Licence notice".toByteArray())
        var stop = false
        try { PackIntegrity.extract(zip.inputStream(), root, 100_000, { stop }, PackIntegrity.sha256(manifest)) { done, _ -> if (done > manifest.size) stop = true }; fail("Cancellation was ignored") } catch (_: CancellationException) { }
        assertTrue(File(root, "manifest.json").exists())
        assertFalse("Part is not activated before complete extraction", File(root, "anatomy/body.meshpack").exists())
        PackIntegrity.extract(zip.inputStream(), root, 100_000, { false }, PackIntegrity.sha256(manifest)) { _, _ -> }
        val complete = File(root, "anatomy/body.meshpack")
        assertArrayEquals(model, complete.readBytes())
        complete.setLastModified(1_600_000_000_000L)
        PackIntegrity.extract(zip.inputStream(), root, 100_000, { false }, PackIntegrity.sha256(manifest)) { _, _ -> }
        assertEquals("Verified parts are reused", 1_600_000_000_000L, complete.lastModified())
        complete.writeBytes(ByteArray(model.size))
        PackIntegrity.extract(zip.inputStream(), root, 100_000, { false }, PackIntegrity.sha256(manifest)) { _, _ -> }
        assertArrayEquals("A corrupt partial file cannot survive resume", model, complete.readBytes())
    }
    @Test fun boundedReaderCannotAllocateAnUnboundedModel() {
        try { PackIntegrity.readBounded(ByteArray(4096).inputStream(), 100); fail("Reader exceeded limit") } catch (_: IllegalArgumentException) { }
    }
}
