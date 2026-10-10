package com.narvyn.suraksha

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.zip.ZipInputStream

/** Optional geometry is app-owned content; removing it never touches identity or learning databases. */
internal class AnatomyPackStore(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.filesDir, "content-packs")
    private val preferences = app.getSharedPreferences("anatomy-quality", Context.MODE_PRIVATE)
    val catalogue: JSONObject? get() = runCatching {
        app.assets.open("anatomy/optional/catalog.json").use { JSONObject(String(PackIntegrity.readBounded(it, 65_536), Charsets.UTF_8)) }
    }.getOrNull()

    private fun descriptor(): JSONObject = requireNotNull(catalogue) { "This build has no optional body pack." }.also {
        require(it.getString("id") == "body-detail")
        require(it.getString("version").matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")))
        require(it.getString("archive") == "anatomy/optional/body-detail.zip")
        require(it.getLong("bytes") in 1..80_000_000)
        require(it.getLong("unpackedBytes") in 1..160_000_000)
        require(it.getString("sha256").matches(Regex("[a-f0-9]{64}")))
        require(it.getString("manifestSha256").matches(Regex("[a-f0-9]{64}")))
    }
    private fun destination(d: JSONObject) = File(root, "body-detail/${d.getString("version")}")
    private fun staging(d: JSONObject) = File(root, "body-detail/${d.getString("version")}.partial")
    fun installed(): Boolean = runCatching {
        val d = descriptor(); val target = destination(d)
        File(target, ".verified").inputStream().use { String(PackIntegrity.readBounded(it, 64), Charsets.US_ASCII) } == d.getString("sha256") &&
            File(target, "manifest.json").inputStream().use { PackIntegrity.sha256(PackIntegrity.readBounded(it, 1_000_000)) } == d.getString("manifestSha256")
    }.getOrDefault(false)
    fun activeDirectory(): File? = if (preferences.getBoolean("detailed", false) && installed()) destination(descriptor()) else null
    fun requestedDetailed() = preferences.getBoolean("detailed", false)
    /** Called by the background model loader. An explicit balanced choice is never overridden. */
    fun prepareDefaultQuality(cancelled: () -> Boolean): String? {
        if (preferences.contains("detailed") && !requestedDetailed()) return null
        return try {
            if (installed()) selectDetailed(true) else install(cancelled) { _, _ -> }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Do not loop failed preparations on every scene opening. Offline content offers retry.
            preferences.edit().putBoolean("detailed", false).apply()
            "Detailed models could not be prepared. Balanced models are ready; choose Detailed in Offline content to retry."
        }
    }
    fun invalidate() {
        preferences.edit().putBoolean("detailed",false).apply()
        runCatching { File(destination(descriptor()), ".verified").delete() }
    }
    fun detailedSelected() = activeDirectory() != null
    fun selectDetailed(value: Boolean) {
        require(!value || installed()) { "Install the detailed pack first." }
        preferences.edit().putBoolean("detailed", value).apply()
    }
    fun paused(): Boolean = runCatching { staging(descriptor()).exists() }.getOrDefault(false)
    fun installedBytes(): Long = runCatching { destination(descriptor()).walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0)
    fun remove() = synchronized(lease) {
        check(!busy) { "Pause installation before removing this pack." }
        selectDetailed(false)
        File(root, "body-detail").deleteRecursively().also { check(it) { "The pack could not be completely removed. Try again." } }
    }
    fun install(cancelled: () -> Boolean, progress: (String, Int) -> Unit) {
        synchronized(lease) { check(!busy) { "A pack installation is already running." }; busy = true }
        try {
            val d = descriptor(); val scratch = staging(d)
            check(root.exists() || root.mkdirs()) { "Content storage is unavailable." }
            check(root.usableSpace >= d.getLong("unpackedBytes") + 20_000_000) { "Free some storage before installing this pack." }
            if (installed()) { selectDetailed(true); return }
            progress("Checking the included archive", 0)
            // The archive identity is pinned by the catalogue shipped with this application.
            app.assets.open(d.getString("archive")).use { input ->
                val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(32_768); var bytes = 0L
                while (true) {
                    if (cancelled()) throw CancellationException()
                    val n = input.read(buffer); if (n < 0) break
                    bytes += n; require(bytes <= d.getLong("bytes")); digest.update(buffer, 0, n)
                    progress("Checking the included archive", (bytes * 15 / d.getLong("bytes")).toInt())
                }
                check(bytes == d.getLong("bytes") && PackIntegrity.hex(digest.digest()) == d.getString("sha256")) { "The optional archive failed its integrity check." }
            }
            scratch.mkdirs()
            app.assets.open(d.getString("archive")).use { archive ->
                PackIntegrity.extract(archive, scratch, d.getLong("unpackedBytes"), cancelled, d.getString("manifestSha256")) { done, total ->
                    progress("Preparing offline models", 15 + (done * 75 / total.coerceAtLeast(1)).toInt())
                }
            }
            if (cancelled()) throw CancellationException()
            progress("Verifying model files", 92)
            val manifest = File(scratch, "manifest.json")
            val manifestBytes=manifest.inputStream().use { PackIntegrity.readBounded(it, 1_000_000) }
            check(PackIntegrity.sha256(manifestBytes) == d.getString("manifestSha256")) { "The model manifest did not match this release." }
            val data = JSONObject(String(manifestBytes, Charsets.UTF_8))
            val parts = data.getJSONArray("parts").objects()
            require(parts.size in 1..1024)
            require(parts.map { it.getString("id") }.toSet().size == parts.size)
            parts.forEach { part ->
                if (cancelled()) throw CancellationException()
                val file = PackIntegrity.safeFile(scratch, part.getString("file"))
                require(part.getInt("bytes") in 1..32_000_000)
                check(file.length() == part.getLong("bytes") && file.inputStream().use { PackIntegrity.sha256(PackIntegrity.readBounded(it, part.getInt("bytes"))) } == part.getString("sha256")) { "A model file failed verification." }
            }
            // Commit only after verification; a cancelled/interrupted preparation is never activated.
            File(scratch, ".verified").writeText(d.getString("sha256"))
            val target = destination(d)
            if (target.exists()) check(target.deleteRecursively())
            check(scratch.renameTo(target)) { "The verified pack could not be activated." }
            selectDetailed(true); progress("Detailed body is ready", 100)
        } finally { synchronized(lease) { busy = false } }
    }
    companion object { private val lease = Any(); @Volatile private var busy = false }
}

/** Bounded, path-safe extraction. Only entries matching the bundled manifest can be reused after pause. */
internal object PackIntegrity {
    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    fun sha256(bytes: ByteArray) = hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    fun safeFile(root: File, name: String): File {
        require(name.isNotBlank() && name.length <= 240 && !name.contains('\\')) { "Invalid pack path." }
        require(!name.startsWith('/') && name.split('/').none { it == ".." || it == "." || it.isEmpty() }) { "Invalid pack path." }
        val target = File(root, name)
        require(target.canonicalPath.startsWith(root.canonicalPath + File.separator)) { "Pack path escapes storage." }
        return target
    }
    fun readBounded(input: InputStream, limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(16_384)
        while (true) { val n = input.read(buffer); if (n < 0) break; require(output.size() + n <= limit) { "Model data is larger than allowed." }; output.write(buffer, 0, n) }
        return output.toByteArray()
    }
    fun extract(input: InputStream, root: File, maximum: Long, cancelled: () -> Boolean, manifestHash: String? = null, progress: (Long, Long) -> Unit) {
        require(maximum in 1..160_000_000)
        var total = 0L; val names = mutableSetOf<String>(); val buffer = ByteArray(32_768)
        val previousManifest = File(root, "manifest.json").takeIf { it.isFile && it.length() <= 1_000_000 }
            ?.readBytes()?.takeIf { manifestHash != null && sha256(it) == manifestHash }
        val verifiedParts = previousManifest?.let { JSONObject(String(it, Charsets.UTF_8)).getJSONArray("parts").objects().associateBy { part -> part.getString("file") } }.orEmpty()
        ZipInputStream(input).use { zip ->
            while (true) {
                if (cancelled()) throw CancellationException()
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) { zip.closeEntry(); continue }
                require(names.add(entry.name) && names.size <= 1100) { "Duplicate or excessive pack files." }
                val target = safeFile(root, entry.name)
                require(entry.name == "manifest.json" || entry.name.startsWith("anatomy/") && entry.name.endsWith(".meshpack") || entry.name.matches(Regex("LICENSE[A-Za-z0-9._-]*\\.txt"))) { "Unsupported pack file." }
                target.parentFile!!.mkdirs()
                val expected = verifiedParts[entry.name]
                val reusable = target.isFile && when {
                    entry.name == "manifest.json" -> previousManifest != null
                    expected != null -> target.length() == expected.getLong("bytes") && sha256(target.readBytes()) == expected.getString("sha256")
                    else -> false
                }
                val partial = File(target.parentFile, target.name + ".writing")
                (if (reusable) null else partial.outputStream()).use { out ->
                    var fileBytes = 0L
                    while (true) {
                        if (cancelled()) throw CancellationException()
                        val n = zip.read(buffer); if (n < 0) break
                        total += n; fileBytes += n
                        require(total <= maximum && fileBytes <= 32_000_000) { "Pack exceeds its storage limit." }
                        out?.write(buffer, 0, n); progress(total, maximum)
                    }
                    out?.fd?.sync()
                }
                if (!reusable) { if (target.exists()) check(target.delete()); check(partial.renameTo(target)) }
                zip.closeEntry()
            }
        }
        require("manifest.json" in names) { "Pack manifest is missing." }
    }
}
