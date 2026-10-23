package com.narvyn.suraksha

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.cardboard.sdk.deviceparams.CardboardV1DeviceParams
import com.google.cardboard.sdk.deviceparams.DeviceParamsUtils
import com.google.cardboard.sdk.qrcode.CardboardParamsUtils

/** The sole native Cardboard seam. GL resources belong to the headset renderer's GL thread. */
internal object CardboardBridge {
    @Volatile private var loaded = try { System.loadLibrary("suraksha_cardboard"); true } catch (_: LinkageError) { false }
    private var sdkContext: Context? = null
    val nativeAvailable: Boolean get() = loaded

    @Synchronized fun initialise(activity: Activity) {
        check(loaded) { "Headset rendering is unavailable on this device." }
        if (sdkContext != null) return
        // Upstream keeps a process-global Context. Use a display-scoped application context,
        // so repeated setup / training Activities are never retained by that native reference.
        @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
        val displayContext = activity.applicationContext.createDisplayContext(display)
        val context = object : ContextWrapper(displayContext) {
            override fun startActivity(intent: Intent) = super.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        try { nativeInitialise(context); sdkContext = context }
        catch (error: LinkageError) { loaded = false; throw IllegalStateException("Headset rendering could not start.", error) }
    }

    fun hasViewerProfile(): Boolean = loaded && runCatching { profile() != null }.getOrDefault(false)
    fun useDefaultViewer() {
        val context = checkNotNull(sdkContext) { "Prepare headset setup first." }
        // Call only after an explicit Cardboard v1 choice. A generic preset is never silently
        // substituted for the manufacturer's lens spacing and distortion parameters.
        check(CardboardParamsUtils.writeDeviceParams(CardboardV1DeviceParams.build().toByteArray(), context)) {
            "The viewer profile could not be saved."
        }
    }
    fun scanViewer(activity: Activity) { initialise(activity); nativeScanViewer() }
    private fun profile(): ByteArray? {
        val context = sdkContext ?: return null
        val bytes = CardboardParamsUtils.readDeviceParams(context) ?: return null
        if (bytes.size !in 1..8192) return null
        val p = DeviceParamsUtils.parseCardboardDeviceParams(bytes) ?: return null
        if (p.screenToLensDistance !in .01f.. .2f || p.interLensDistance !in .03f.. .09f ||
            p.trayToLensDistance !in 0f.. .1f || p.leftEyeFieldOfViewAnglesCount != 4 ||
            p.leftEyeFieldOfViewAnglesList.any { !it.isFinite() || it !in 10f..85f } ||
            p.distortionCoefficientsCount > 8 || p.distortionCoefficientsList.any { !it.isFinite() || kotlin.math.abs(it)>10f }) return null
        return bytes
    }
    fun create(width: Int, height: Int): Long {
        require(width in 2..16384 && height in 2..16384)
        check(loaded && sdkContext != null) { "Prepare headset setup first." }
        return nativeCreate(width, height, checkNotNull(profile()) { "Scan a valid viewer profile first." })
            .also { check(it != 0L) { "The calibrated headset renderer could not start." } }
    }
    fun resume(handle: Long) = nativeResume(handle)
    fun pause(handle: Long) = nativePause(handle)
    fun recenter(handle: Long) = nativeRecenter(handle)
    fun destroy(handle: Long) = nativeDestroy(handle)
    fun headView(handle: Long, timeNs: Long, rotation: Int, out: FloatArray) {
        require(out.size == 16 && timeNs > 0); nativeHeadView(handle, timeNs, rotation, out)
    }
    fun projection(handle: Long, eye: Int, near: Float, far: Float, out: FloatArray) {
        require(eye in 0..1 && out.size == 16 && near.isFinite() && far.isFinite() && near > 0 && far > near)
        nativeProjection(handle, eye, near, far, out)
    }
    fun eyeFromHead(handle: Long, eye: Int, out: FloatArray) {
        require(eye in 0..1 && out.size == 16); nativeEyeFromHead(handle, eye, out)
    }
    fun composite(handle: Long, width: Int, height: Int, texture: Int, sharedCameraImage:Boolean=false) {
        require(width > 0 && height > 0 && texture > 0); nativeComposite(handle, width, height, texture, sharedCameraImage)
    }
    private external fun nativeInitialise(context: Context)
    private external fun nativeScanViewer()
    private external fun nativeCreate(width: Int, height: Int, profile: ByteArray): Long
    private external fun nativeResume(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeRecenter(handle: Long)
    private external fun nativeDestroy(handle: Long)
    private external fun nativeHeadView(handle: Long, timeNs: Long, rotation: Int, out: FloatArray)
    private external fun nativeProjection(handle: Long, eye: Int, near: Float, far: Float, out: FloatArray)
    private external fun nativeEyeFromHead(handle: Long, eye: Int, out: FloatArray)
    private external fun nativeComposite(handle: Long, width: Int, height: Int, texture: Int, sharedCameraImage:Boolean)
}
