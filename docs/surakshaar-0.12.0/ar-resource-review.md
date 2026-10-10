# AR resource review

Reviewed 10 October 2026. This is a source and dependency review prompted by a transient physical-device native-memory peak. It is not a native heap profile, proof of a persistent leak, or evidence that the peak has been fixed. Device measurements belong in the [phone performance report](phone-performance.md) and must retain their tested APK hashes. The source adjustments described below are included in final candidate `6492c376eaeaccbd9f829ae60d8489233d63e018a43abb99b87762567809c7d6`; that candidate has not received a cooled physical AR performance rerun.

## Confirmed allocation reduction

The resolved dependency is `com.google.ar:core:1.56.0`. Inspection of its `classes.jar` with the bundled JDK's `javap -c -p` shows that `Frame.getCamera()` constructs a new `Camera`; its constructor calls `nativeAcquireCamera`, and its finalizer calls `nativeReleaseCamera`. This is a native-backed wrapper acquisition, not merely a Kotlin property lookup. The inspected AAR was `core-1.56.0.aar`, Gradle artifact directory `cd1c7cde413fd8b99fd95371ba3c1e1a75ff550`.

Before the adjustment, a distinct tracked frame acquired four wrappers in `NativeArDriver.update()` and two more in the placed path in `AnatomyScene.onDrawFrame()`; the unplaced pose query added another. Both functions now retain one local camera variable for their own current frame. The placed path therefore goes from six acquisitions to two without changing the driver API or retaining a camera across frames. Google's [Hello AR Java sample](https://github.com/google-ar/arcore-android-sdk/blob/master/samples/hello_ar_java/app/src/main/java/com/google/ar/core/examples/java/helloar/HelloArActivity.java) likewise obtains a camera once in its frame handler and passes that local object to its helpers.

The wrapper reduction is verified by source inspection. The driver's reported update timing now also includes its single camera getter, so future timing comparisons must account for that small measurement-boundary change. Its impact on native heap, frame time and battery is **not yet measured**. Deferred native-wrapper cleanup is a possible contributor to temporary allocation pressure, not an established explanation for the measured peak. No manual finalizer calls, forced garbage collection, native-handle reflection or cross-frame camera cache were introduced.

## Lifecycle and rendering checks

- The scene has one session driver and one placement anchor. The reviewed draw path does not retain historical frames, camera images, depth images or point clouds. Timing history is bounded to 300 samples. ARCore `Pose` in this dependency stores a quaternion and translation array; the retained scan origin is a single small pose, not a growing native-handle collection.
- Mesh data is uploaded to static GL buffers during renderer creation; draw calls do not allocate fresh mesh buffers. The camera background has fixed small UV/vertex buffers. These paths do not explain a continuous large per-frame mesh allocation.
- The GL surface is paused before the AR session is paused; the anchor is detached, and session close is queued off the UI thread behind a process-wide release barrier. That follows the lifecycle requirements in Google's [Session API](https://developers.google.com/ar/reference/java/com/google/ar/core/Session): explicitly close sessions, avoid use during/after close, and keep the potentially slow close off the main thread.
- Idle 3D exploration now requests a frame only on resume, viewport change or a model/control interaction. Its touch/selection/layer/focus paths already request rendering. The continuous frame pump applies only to camera AR.
- Thermal status is now sampled once per second rather than at every render tick. The existing approximately 30fps normal / 20fps severe-thermal schedule and critical-temperature pause remain. A 20fps observation alone does not prove a thermal transition; the actual thermal status must be logged alongside the cadence.

No persistent native leak was established by this review. The explicit GL-buffer/program cleanup path could be strengthened for repeated scene entry and partial renderer creation failure; current normal resource lifetime relies on EGL context teardown. This is a follow-up lifecycle improvement, not a demonstrated cause of the within-session peak. The still-running placement-guide invalidation and repeated locked-status formatting are small remaining avoidable workloads; their memory/performance impact is unmeasured.

## Required follow-up measurements

1. Reproduce the same scan, placement, tracking interruption and recovery on a cooled device. Record the exact APK, AR Services/device versions, session epoch, thermal status and selected render interval with each sample. Do not collect camera images or incidental people for this diagnostic.
2. Sample Java heap and native allocated heap separately from process PSS/RSS; those metrics are not interchangeable. Correlate transient peaks with tracking transitions and garbage collection rather than inferring a leak from a single snapshot.
3. If the peak recurs, use a native allocation trace to identify whether allocations originate in ARCore, graphics, or application code. Do not call the wrapper reduction a memory fix without a comparable before/after trace.
4. Exercise repeated AR entry/exit and a complete 20-minute learning session on a named mid-range ARCore-supported phone, then re-run performance checks with the intended release build. Google's [performance guidance](https://developers.google.com/ar/develop/performance) calls for representative devices and thermal monitoring; a brief flagship debug-build sample cannot substitute for those checks.

The observed peak fell substantially while AR was still active, before switching to 3D. It must not be described as a persistent leak or as memory fully reclaimed by the 3D transition. Source optimisations and successful automated checks do not establish sustained AR performance.
