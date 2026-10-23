# Publication checks — 23 October 2026

This publication uses a clean checkout of `narvyn-official/surakshaar`, based on `7cb08e1ac0f72137774f847fabe6fb324135cddd`. Only the new Android headset/hand work, its offline assets, tests, guides and release documentation were added. Existing web/backend source, lesson catalogues, curriculum, assessment answers and certification workflows are unchanged. The original working tree and index were preserved; unrelated files were not copied.

| Check | Result |
| --- | --- |
| Fresh-checkout Android source build | Pass: `testDebugUnitTest lintDebug assembleDebug`; 368 JVM tests, zero failures/errors/skips. |
| Application/library lint | Pass with three precisely baselined upstream `MissingPermission` findings and visible advisory warnings. |
| Pinned SDK | Pass: 130 Cardboard/generated source hashes and native pose/lens host checks. |
| Release artifact | Signature and 16 KiB ZIP alignment pass; SHA-256 matches the 22 October validated APK. |
| Source parity | Android, content and scripts match the local build inputs; IDE state/caches are excluded. |
| Publication content | No credential-pattern matches, signing keys, runtime state, databases or files over 50 MB in the staged change. Scoped checks are not an exhaustive security audit. |
| Whitespace | Authored files pass. Unchanged hash-pinned vendor/generated code, upstream patch context and license notices retain upstream whitespace. |
| Device testing | Not repeated; 20 selected Android tests and three large-text variants remain the documented emulator evidence. Physical live-hand/headset acceptance is still pending. |
| Backend deployment / recurring collection | Not performed or enabled. |

## Reviewed upstream lint baseline

The fresh multi-module lint run initially reported three permission-analysis errors in Google's pinned QR camera source. `CameraSourcePreview.surfaceCreated` and `onLayout` catch `SecurityException` around `startIfReady`; `QrCodeCaptureActivity.onResume` checks CAMERA before starting its camera and `startCameraSource` also catches `SecurityException`. Lint does not infer the existing helper guard/exception path. The library baseline records only these exact three findings. No check is globally disabled; other findings and future new findings remain visible. Pinned vendor bytes were preserved. This follows Android's documented [lint baseline mechanism](https://developer.android.com/studio/write/lint#snapshot).

The only post-APK-build configuration change is this lint baseline declaration and XML. It does not change the packaged production source/assets. The signed APK remains `53ff5e7ee29b46fe6437d4cdee738db79329ea4bb09bac3e03edb6d85e216274`, 96,316,896 bytes, version 0.20.0-dev (38). Detailed packaging and earlier emulator reports accompany this directory.

This is a public **development preview**, with documented unverified physical-device behaviour; it is not a production-readiness acceptance.
