# OmniCam Lab – Agent Task List

## Operating Rule

Do not build everything at once.

Work phase by phase.  
After every phase:

1. Implement only the listed scope.
2. Run build/tests.
3. Fix compile/runtime errors.
4. Update `PROJECT_STATUS.md`.
5. Update `CAMERA_FEATURE_MATRIX.md`.
6. Stop and report what was completed before moving to the next phase.

Never assume a camera feature is supported. Always detect it from Android camera APIs.

---

# Phase 0 – Project Skeleton

## Goal
Create a clean Android Kotlin Jetpack Compose project for OmniCam Lab.

## Tasks

- Create Android project with package:
  `com.pna.omnicamlab`
- Use Kotlin.
- Use Jetpack Compose Material 3.
- Add basic navigation.
- Add app theme.
- Add permissions handling structure.
- Add logging utility.
- Add empty placeholder screens:
  - Home
  - Capability Report
  - Camera
  - Settings
- Add documentation files:
  - `PROJECT_STATUS.md`
  - `CAMERA_FEATURE_MATRIX.md`
  - `MANUAL_TEST_CHECKLIST.md`

## Acceptance

- App builds.
- App launches.
- Home screen opens.
- Navigation works.
- No camera logic yet.

---

# Phase 1 – Camera Capability Scanner

## Goal
Detect and display all available camera capabilities.

## Tasks

Create data models:

- `CameraDeviceProfile`
- `LensProfile`
- `SensorProfile`
- `PhotoProfile`
- `VideoProfile`
- `ExtensionProfile`
- `CameraCapability`
- `HardwareLevel`
- `SupportState`

Implement scanner:

- Get all camera IDs.
- Read `CameraCharacteristics`.
- Detect:
  - camera facing
  - hardware level
  - logical multi-camera
  - physical camera IDs
  - sensor orientation
  - active array size
  - pixel array size
  - focal lengths
  - apertures
  - ISO range
  - exposure time range
  - max frame duration
  - minimum focus distance
  - focus calibration
  - AE modes
  - AF modes
  - AWB modes
  - exposure compensation range
  - flash availability
  - stabilization modes
  - JPEG sizes
  - RAW sizes
  - YUV sizes
  - RAW support
  - burst capability
  - high-speed video support
  - depth support
  - logical multi-camera support

Build UI:

- Capability report screen.
- Show one expandable card per camera.
- Show unsupported/missing values clearly.
- Add export-to-JSON button.

## Acceptance

- App lists all cameras.
- No crash on null/missing camera keys.
- Capability report is readable.
- JSON export works.
- Build passes.

---

# Phase 2 – Camera Preview

## Goal
Open live camera preview using Camera2.

## Tasks

- Implement `Camera2SessionManager`.
- Open selected camera safely.
- Create preview session.
- Show preview in Compose using `AndroidView`.
- Add camera switcher.
- Handle lifecycle:
  - app pause
  - app resume
  - screen rotation
  - camera close
- Show error if camera is unavailable.
- Use selected camera profile from Phase 1.

## Acceptance

- Rear camera preview works.
- Front/rear switching works if supported.
- App does not crash on pause/resume.
- Unsupported cameras show graceful error.

---

# Phase 3 – JPEG Capture

## Goal
Capture and save JPEG photos.

## Tasks

- Add JPEG `ImageReader`.
- Select best default JPEG resolution.
- Add shutter button.
- Capture JPEG still image.
- Save through MediaStore.
- Store session folder naming:
  `OmniCam_YYYYMMDD_HHMMSS_ModeName`
- File naming:
  `IMG_YYYYMMDD_HHMMSS_cameraId_001.jpg`
- Show capture result screen.
- Display:
  - thumbnail
  - file URI
  - camera ID
  - timestamp
  - basic metadata

## Acceptance

- JPEG capture works.
- File appears in gallery/media storage.
- Capture result screen opens.
- App remains stable after multiple captures.

---

# Phase 4 – Manual Photo Controls

## Goal
Expose manual controls only when supported.

## Tasks

Add dynamic manual controls:

- ISO
- shutter speed / exposure time
- focus distance
- exposure compensation
- white balance mode
- flash/torch if available

Rules:

- Manual ISO/shutter only if manual sensor capability exists.
- Manual focus only if minimum focus distance is available.
- RAW button only if RAW capability exists.
- Unsupported controls must be hidden or disabled with reason.
- Controls must have support badges:
  - Green: supported
  - Yellow: supported but unverified
  - Blue: extension only
  - Grey: software simulated
  - Red: unsupported

UI:

- Right-side manual control rail.
- Tap opens slider.
- Long press locks setting.
- Show Auto/Manual toggle for ISO, shutter, focus, WB.

## Acceptance

- Manual controls appear only on supported cameras.
- Unsupported controls do not crash the app.
- Changing supported values affects capture request.
- Build passes.

---

# Phase 5 – Requested vs Actual Capture Verification

## Goal
Verify whether the phone actually applied requested camera settings.

## Tasks

Create `CaptureVerification` model.

Capture and compare:

- requested ISO vs actual ISO
- requested exposure time vs actual exposure time
- requested focus distance vs actual lens focus distance
- requested white balance vs actual AWB mode
- requested exposure compensation vs actual value
- AE state
- AF state
- AWB state

Show warnings:

- “Device ignored requested ISO.”
- “Exposure time was clamped.”
- “Manual focus unavailable on this lens.”
- “Actual capture result differs from requested setting.”

## Acceptance

- Capture result screen shows requested and actual values.
- Mismatches are clearly shown.
- No false claim that feature worked unless CaptureResult confirms it.

---

# Phase 6 – RAW/DNG Capture

## Goal
Capture RAW + JPEG where supported.

## Tasks

- Detect RAW support from capability scanner.
- Add RAW output only where available.
- Configure RAW `ImageReader`.
- Capture RAW_SENSOR + JPEG together if possible.
- Save DNG using Android DNG APIs.
- Match file names:
  - `.jpg`
  - `.dng`
  - optional `.json`
- Add storage warning for RAW sessions.

## Acceptance

- RAW toggle only appears on RAW-supported cameras.
- RAW + JPEG capture works on supported devices.
- App gracefully disables RAW on unsupported devices.
- DNG file opens in compatible apps.

---

# Phase 7 – Histogram, Zebra, Focus Peaking Foundation

## Goal
Add real-time preview analysis overlays.

## Tasks

Implement preview frame analysis pipeline.

Add:

- histogram overlay
- zebra overexposure overlay
- focus peaking overlay foundation

Rules:

- Use efficient YUV/preview frame analysis.
- Keep frame rate stable.
- Allow toggling each overlay.

## Acceptance

- Histogram works.
- Zebra overlay works.
- Focus peaking basic edge overlay works.
- Overlays can be disabled.
- Preview remains usable.

---

# Phase 8 – Timelapse and Interval Capture

## Goal
Add practical interval photo capture.

## Tasks

Add Timelapse mode:

- interval setting
- number of shots
- infinite mode
- start/stop
- screen-on/foreground handling
- session folder
- missed-frame counter
- real interval measurement
- battery/storage warning

## Acceptance

- App captures repeated photos.
- Actual interval is logged.
- User can stop safely.
- Session metadata is saved.

---

# Phase 9 – Long Exposure Repeat / Lightning Mode

## Goal
Add storm/lightning-friendly long exposure loop.

## Tasks

Add Lightning / Storm mode:

- manual shutter default
- low ISO recommendation
- infinity focus shortcut
- continuous capture loop
- minimum gap between shots
- frame brightness scoring
- save/rank likely lightning frames
- optional auto-delete dark frames setting

## Acceptance

- Long exposure repeat works.
- Frames are scored.
- Likely lightning frames are marked.
- User can review best frames first.

---

# Phase 10 – Bracketing Modes

## Goal
Add exposure and focus bracketing.

## Tasks

Exposure bracketing:

- user chooses EV steps
- capture multiple exposures
- save as grouped set

Focus bracketing:

- user chooses near/far range
- number of steps
- capture focus sweep

## Acceptance

- Exposure bracket groups are saved.
- Focus bracket groups are saved.
- Unsupported manual focus disables focus bracketing.

---

# Phase 11 – Practical Recipe Modes

## Goal
Add useful real-world presets.

## Modes

Implement UI presets for:

- Auto+
- Pro Photo
- Night / Low Light
- Lightning / Storm
- Astro / Stars
- Macro
- Product / Jewelry
- Document Capture
- Action / Sports
- Timelapse
- Inspection / Microscope

Each recipe must:

- check camera support
- apply safe defaults
- explain unavailable features
- allow manual override

## Acceptance

- Recipes appear in mode carousel.
- Each recipe uses detected capabilities.
- No recipe enables impossible features.

---

# Phase 12 – Video Foundation

## Goal
Add basic video recording.

## Tasks

- Add video mode.
- Query supported video sizes.
- Query supported FPS ranges.
- Add recording start/stop.
- Save video through MediaStore.
- Add stabilization toggle if supported.
- Add microphone permission only when needed.

## Acceptance

- Basic video recording works.
- Video file saves.
- Unsupported FPS/resolution combinations are not shown.

---

# Phase 13 – Manual Video and High-Speed Video

## Goal
Add pro video controls.

## Tasks

- Manual ISO/shutter/WB/focus during video if supported.
- FPS selector.
- Bitrate selector.
- Stabilization selector.
- High-speed video mode if constrained high-speed capability exists.
- HDR/10-bit video if supported.

## Acceptance

- Manual video controls work where supported.
- High-speed mode only appears when available.
- App does not show fake slow-motion modes.

---

# Phase 14 – Computational Capture Foundation

## Goal
Prepare architecture for multi-frame processing.

## Tasks

Add processing module for:

- HDR merge placeholder
- noise stack placeholder
- light trails placeholder
- star trails placeholder
- focus stack placeholder
- document cleanup placeholder
- frame scoring module

Do not overbuild. Create clean interfaces first.

## Acceptance

- Processing module compiles.
- Existing capture still works.
- Future computational features can plug into session outputs.

---

# Phase 15 – Polish and Device Database

## Goal
Make the app useful across many phones.

## Tasks

- Add device capability profile export.
- Add human-readable report.
- Add troubleshooting screen.
- Add compatibility notes.
- Add session browser.
- Add storage cleanup tools.
- Add settings backup/export.
- Add crash-safe logging.

## Acceptance

- User can export phone capability report.
- User can diagnose why a feature is missing.
- App is ready for wider device testing.

---

# Agent Rules

- Do not skip phases.
- Do not implement future-phase features early unless required for architecture.
- Keep code modular.
- Do not use GPL code directly.
- Do not add heavy dependencies without reason.
- Always run Gradle build after major changes.
- Always update docs.
- Always document unsupported Android/OEM limitations.