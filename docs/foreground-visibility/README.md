# Foreground visibility in spatial learning

The live hand workspace draws the camera, virtual scene, foreground camera pixels, pointer and controls in that order. A fresh neural person mask and a conservative local hand-reveal region control the foreground pass. The same captured camera rays are mapped into each calibrated lens before Cardboard distortion. This does not create stereo camera depth. See the [24 October camera-AR verification](../headset-ar/VERIFICATION-2026-10-24.md) for the current version; the 23 October report records the previous implementation.

The existing single MediaPipe worker publishes hand landmarks first, then optionally runs person segmentation on the same bounded, rotated ARCore image. Segmentation is throttled and skipped when the hand result is already over budget. There is no second camera session. The adaptive inference pacer accounts for the combined processing time. Inputs remain on the phone and are erased/recycled after inference; masks remain transient in memory. The already disclosed MediaPipe vendor usage/performance metrics still apply.

Masks expire after 180 ms, are rejected on session/geometry changes, and use the captured crop and display transform. Camera samples for the overlay come from the current camera texture. A stale mask cannot be silently reused.

Segmentation is **foreground priority**, not physical depth testing. Detected people can appear in front even if they stand behind a virtual object. Thin fingers and fast motion can be missed. Fresh, confident landmarks additionally restore current camera pixels in a soft local region around fingers and palms. This intentionally includes some surrounding background; it is approximate visibility assistance, not an inferred anatomical mesh or precise occlusion. Local hand regions expire after 160 ms and are cleared on pause, geometry changes and disabling hands. Global see-through remains an explicit alternative and a fallback when only older hand results are available.

Only camera AR remains in the live workspace. The virtual practice room and separate VR player have been removed; historical VR decision records remain read only. Placement requires a mapped real surface. Headset capture uses the configured GPU camera aspect, independently of each eye's screen width. All camera headset use remains seated and experimental until tested with the actual phone and viewer.

## Model and rights

- Official model: [Selfie Segmenter float16 version 1](https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/1/selfie_segmenter.tflite).
- Local asset: `content/models/foreground/selfie_segmenter.tflite`.
- SHA-256: `191ac9529ae506ee0beefa6b2c945a172dab9d07d1e802a290a4e4038226658b`.
- [Official model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf), page 1 declares Apache License 2.0. Its thin-feature limitations are material to this use.
- Existing `com.google.mediapipe:tasks-vision:1.1.1` is reused; no dependency or cloud provider was added.
- [Android Image Segmenter guide](https://developers.google.com/edge/mediapipe/solutions/vision/image_segmenter/android) and [model documentation](https://developers.google.com/edge/mediapipe/solutions/vision/image_segmenter) checked on 23 October 2026.
- [ARCore Depth documentation](https://developers.google.com/ar/develop/java/depth/developer-guide) was reviewed. This pass does not enable Depth or claim metric depth; support varies by device.

Official portrait/expected-mask fixtures are stored only in `androidTest/assets/foreground/`, under the upstream [test-data package's Apache notice](https://github.com/google-ai-edge/mediapipe/blob/v1.1.1/mediapipe/tasks/testdata/vision/BUILD). They are not shipped as learner content or presented as live output. The GPU test uses a generated red camera texture and blue geometry background.

## Teaching boundary

Hands AR can open at the current authored step and browse all 94 mapped steps with exact scene variants, cues, props and descriptions. Previous/Next only changes this view. It cannot create assessment answers, anatomy observation receipts, certificates or supervised practical competence. School restrictions are enforced again at entry and profile changes retire the old workspace. Untranslated draft lessons declare English and require an explicit “Start lesson in English” action; the app language preference is preserved. Standalone authored Hindi anatomy descriptions and first-aid captions use Hindi narration.
