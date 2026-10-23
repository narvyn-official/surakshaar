# Live AR with hands

The hands workspace is an exploration mode alongside the existing handheld lessons and Cardboard VR lessons. Open **Home → Hands AR**, **Explore → Live AR with hands**, or the headset chooser's **Live AR with hands** action. Body exploration and step inspection also link to the workspace.

## Controls

| Action | Control |
|---|---|
| Point | Keep the whole hand visible and aim with the index fingertip. The visible cursor follows the detected hand. |
| Grab a virtual model | Show a neutral hand, then pinch thumb and index finger over the model. |
| Move | Keep pinching and move the hand. The model moves on its placement plane, within bounds. |
| Place | Release the pinch. |
| Resize and turn | Show both hands, then pinch with each. Change their separation or angle. |
| Cancel a grab | Deliberately open the palm. Tracking loss or delayed input also cancels the uncommitted manipulation. |
| Touch alternative | Handheld touch and the labelled model controls remain available. Use a stable phone stand for two-hand gestures. |
| Headset button alternative | A short press cycles the highlighted command; hold to activate it. Pause and Exit remain in both eye views. |

Virtual extinguishers, an AED trainer, the clothed body and linked anatomical/teaching details are illustrative learning models. The workspace does not recognise arbitrary physical objects, measure treatment skill or create assessment/certification evidence. Return to the lesson for its decision and explanation.

## Camera and viewer setup

Use a well-lit, textured table or floor with clear space. Explicit camera/hand processing consent precedes inference. Handheld use needs ARCore support and camera permission. Headset use additionally needs the rear camera uncovered, landscape orientation, the actual viewer's calibration and a clear seated area. Google's original Cardboard v1 settings apply only to that viewer.

Headset live AR is **experimental monoscopic passthrough**: both eyes receive the same phone-camera image through the Cardboard lens compositor. This cannot provide true stereo camera depth or reliable real-hand occlusion. It is unsuitable for walking, judging real-world distances or navigating hazards. The separate VR lessons continue to use their own stereoscopic scene rendering.

## Implementation and privacy

Google MediaPipe Tasks Vision **1.1.1** runs the bundled, hash-verified hand landmark model on-device. It reads a bounded copy of ARCore's current CPU image, rather than opening a second camera. Image/display mapping is captured while the ARCore frame is current. Conversion and inference use one background worker with at most one frame in flight. Input is paced up to 20 Hz and slows automatically when inference cost rises; this is a configured ceiling, not a measured device frame rate.

Camera frames and landmarks are transient: this application does not log, save or upload them. MediaPipe may send its own usage/performance metrics; this distinction is disclosed before enabling hands. No supported public SDK telemetry opt-out was found. Offline inference does not require downloading a model or calling a generative AI service.

The gesture controller requires neutral rearming, palm-relative pinch thresholds and stable hand identities. A speed-adaptive pointer filter reduces still-hand jitter while following deliberate movement; it does not predict unobserved hand positions. Selection locks at the first deliberate pinch sample. Opening the fingers commits the last held point, without dragging during release confirmation. It locks the selected target, rejects duplicate/old input and cancels after stale results, orientation changes, focus loss or ambiguous hands. The renderer owns mesh picking, placement-plane dragging and bounded object transforms. Two-hand movement, scale and turn share the actual grip midpoint, including an off-centre grip; rotation remains continuous across the half-turn boundary. A short display interpolation fills the gaps between inference updates. Moving one remaining touch finger after a two-finger gesture rebases its grab offset rather than jumping. ARCore owns camera pose in live AR; Cardboard supplies lens correction without starting a competing head tracker.

Model/SDK versions, hashes, notices, official documentation and fixture provenance are in [the engine documentation](../hand-tracking/ENGINE.md) and [upstream record](../hand-tracking/UPSTREAM.json). Verification results and physical-test limits are recorded in [the dated report](VALIDATION-2026-10-22.md).

## Current follow-up

The 0.20.1 local follow-up renders the single-camera headset scene once, then lets both calibrated eye meshes sample it. Separate VR eye images remain separate. The HUD reuses its texture and updates at most 10 times per second; gesture state changes do not force repeated texture allocation. [Current validation and physical-device blockers](VALIDATION-2026-10-23.md).
