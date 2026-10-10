# Samsung AR check — 10 October 2026

This is a short, interactive development check on a **Samsung SM-S921B, Android 16**, using Google Play Services for AR **1.56.262080393**. It is not the required mid-range benchmark or a controlled 20-minute test. The phone had already been used for repeated development sessions; starting temperature and other workload were not controlled.

## What worked

- APK `f93c3f78622cfc64a7daeb5e30470857391df17c0d2d5fb41b0d4a2653031b6a`: the user confirmed that the body stayed attached to the same surface during sideways movement and looking away/returning. This is user-reported stability, not measured drift.
- APK `0a3d28114da85d0bd189a91a4cedfe9796b4604e5dbbf3f4dc418829fba1df67`: installed in place without deleting application data. A new session reached surface-ready and **Body locked at 40%**. Logs confirm camera rendering with a placed anchor. Tracking recovered after an interruption; this does not establish the complete first-aid AR journey.
- From 13:48:40–13:48:55, four steady five-second intervals each rendered 152 frames: about **30.3 rendered fps**. This is render cadence, not 30 unique camera images per second; the driver separately recorded distinct frame timestamps.
- Later rendering intervals were around **20 fps**, consistent with the implemented severe-thermal cadence. A thermal snapshot subsequently reached Android status 4 (critical). The sustained 30-fps performance requirement therefore **has not passed**.

## Memory and tracking findings

A tool-captured snapshot during tracking loss reported **1,814,829 KiB total PSS**, including **1,585,710 KiB native heap PSS**. A later saved snapshot, taken **before** selecting 3D, had already dropped to **322,504 KiB total PSS / 122,483 KiB native heap PSS**. After selecting 3D, another tool snapshot reported **249,943 KiB total PSS / 78,073 KiB native heap PSS**.

The peak was transient; these observations do not establish a retained leak or its cause. Do not attribute the entire memory reduction to leaving AR. The peak snapshot was returned directly by the tool, while the later pre-exit dump is retained below.

The same session logged ARCore `VIO frequency low` at 3.37746 Hz and 8.87036 Hz, alongside native circular-allocation overflow messages. Those findings and the thermal reduction are unresolved performance evidence, not a successful endurance test. A native allocation trace and a cooled, controlled comparison against Google's reference sample are needed to identify attribution.

Google's [ARCore performance guidance](https://developers.google.com/ar/develop/performance) recommends monitoring thermals and VIO frequency and testing representative device classes. Its [Session API](https://developers.google.com/ar/reference/java/com/google/ar/core/Session) documents native resource ownership and explicit close. Our session ownership and asynchronous close barrier remain in place; this check does not justify bypassing tracking gates or periodically discarding valid anchors to disguise the issue.

## Changes after this measurement

APK `6492c376eaeaccbd9f829ae60d8489233d63e018a43abb99b87762567809c7d6` stops the continuous frame pump for static 3D exploration: initial display and actual control/selection changes request frames. Camera mode retains its frame pump, but samples thermal status once per second instead of every frame. The 30/20-fps and critical-temperature pause policies, placement clearance and interaction freshness checks remain unchanged.

The driver and scene also reuse a local camera wrapper within each frame rather than repeatedly acquiring it through the ARCore getter. No camera object is deliberately reused across frames.

These remove identified unnecessary work. They are **not claimed to resolve the native peak or prove sustained AR performance**. Unit/build checks passed; final interaction checks and installation are recorded in the main ledger. A cooled physical performance rerun remains required.

## Retained evidence

- [AR and driver timing log](../../artifacts/surakshaar-0.12.0/phone/final-build-ar.log) — includes earlier checkpoints; use the timestamps and PID described above.
- [Later pre-exit memory snapshot](../../artifacts/surakshaar-0.12.0/phone/final-ar-meminfo.txt).
- [ARCore performance excerpts](../../artifacts/surakshaar-0.12.0/phone/arcore-performance-excerpts.log).
- [After-session thermal snapshot](../../artifacts/surakshaar-0.12.0/phone/after-session-thermal.txt).

Real-room imagery is excluded from the demonstration deliverables. The demonstration video uses the emulator's explicit 3D mode.
