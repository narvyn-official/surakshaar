# Anatomy GL lifecycle review

Reviewed 10 October 2026 against the working tree after the 117-part material/asset refinement. This is a source review and a single Android screenshot inspection, not a repeated-entry memory test. It does not establish the cause of the earlier transient ARCore memory peak.

Implementation follow-up: the first three corrections below are now applied. The [0.12.1 verification report](../surakshaar-0.12.1/verification.md) records five successful normal enter/finish cycles with GL-thread counts returning to baseline. This adds lifecycle evidence; it does not supersede the physical AR/memory checks still required below.

## Android behavior

The Android 16 AOSP implementation provides these relevant guarantees:

- `setRenderer` starts its GL thread immediately. A never-attached view has a finalizer fallback for stopping that thread.
- Pausing releases the EGL surface and clears the current context. Preserving the context retains its resources, but does not keep it current.
- Queued events can run while paused; the queue does not establish a current context.
- Detaching the view requests thread exit and waits. The thread's `finally` destroys its surface and context, regardless of the preserve-on-pause option.

Source: [AOSP Android 16 GLSurfaceView](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/opengl/java/android/opengl/GLSurfaceView.java), especially lines 227–236, 348–361, 630–638, 1183–1211, 1326–1382 and 1613–1621. Retrieved source SHA-256: `49581ab937901f9b476ac6ec9c414f63bcb98fa25ac6904eb38dcc5b0fa37b43`. The [Android renderer contract](https://developer.android.com/reference/android/opengl/GLSurfaceView.Renderer) also states that context loss deletes associated GL resources automatically. These sources establish framework behavior; OEM runtime behavior has not been profiled here.

## Findings in the reviewed app

`AnatomyScene.close()` queues `renderer.release()` and camera-background deletion, then pauses. When invoked after `Activity.onPause`, there is already no current EGL context, so those GL deletion calls cannot be relied upon. The normal mode rebuild subsequently replaces the content view and detaches the previous surface; framework context destruction is the definitive cleanup in that path. This is an ineffective explicit deletion path, **not evidence of a persistent GPU leak**.

Two asynchronous-loading edges merit separate guards. `AnatomyActivity` originally accepted a load completion whenever it was not destroyed, including while finishing. `FirstAidBayActivity.attachScene()` originally rejected completed attempts but not stopped attempts; after Stop shows the summary, an initialized viewport field can still refer to the detached practice layout. A late model load can construct an unused scene there. No EGL surface has been established for that never-attached scene, but its thread cleanup is then dependent on eventual reclamation rather than a normal detach.

## Minimal proposed correction

1. Run explicit GL deletion only when `EGL14.eglGetCurrentContext()` is not `EGL14.EGL_NO_CONTEXT`. Treat framework context destruction as the fallback when the scene is already paused.
2. During terminal scene close, pause/retire camera work and remove the attached surface child on the UI thread. This makes final GL-thread/context teardown happen within close rather than depend on a subsequent activity layout replacement. Do not force a paused camera view to resume just to delete resources.
3. Reject late loader callbacks for finishing/destroyed activities. Require an active practice screen and an attempt that is neither stopped nor finished before building a bay scene. The viewport attachment flag alone is insufficient: a legitimate freshly constructed layout may not have attached yet.
4. For a stronger never-attached guarantee, defer `setRenderer` until the scene's first real attachment and guard request/pause/event calls until initialization. This is a larger lifecycle change than the first three corrections and should receive its own enter/close-before-attach test.

No manual finalizer invocation, private GL-thread reflection, forced collection, or arbitrary wait is recommended. Native source changes belong to the scene/activity owners; the above records the review proposal, not an implementation claim.

## Verification still required

Exercise foreground close, close after pause, AR/3D switches, immediate Back during asset loading, stopping the bay before loading completes, background/resume and activity recreation. Count live GL threads and sample native/graphics memory after each stable cycle; record the exact APK and thermal state. Ensure camera release precedes a subsequent session and verify that returning to a paused scene redraws correctly. Heap totals need not immediately return to their initial value because runtime and driver caches can remain.

The inspected `/tmp/anatomy-dock-angle.png` is an actual Android render supplied by the parent agent. It shows the new tissue shader working and the model remaining visible above the bounded controls. Lung segmentation and external-face source limitations remain visible. It verifies neither performance nor teardown and is not a claim of photorealism or clinical validation.
