# OmniCam Lab - Project Status

## Project Overview
- **Project Name**: OmniCam Lab / SensorCam Pro
- **Internal Package Name**: `com.pna.omnicamlab`
- **Current Active Phase**: Phase 1E (JPEG Still Capture & MediaStore Save) Completed
- **Compile Status**: Compiling and Building Successfully
- **Last Updated**: 2026-05-20

---

## Technical Specifications
- **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Camera2 API, CameraX (helpers), Coroutines + Flow, MediaStore, JUnit 4/5.
- **Min SDK**: API 28 (Android 9.0)
- **Target SDK**: API 34+

---

## Current Status Checklist

### [x] Phase 1A: Project Skeleton, Permissions & Navigation
- [x] Package successfully refactored to `com.pna.omnicamlab` across all directory structures
- [x] Material 3 dark-themed high-fidelity color tokens configured (`Theme.kt`, `Color.kt`)
- [x] Gradle build files successfully updated with Compose BOM, Navigation 3, and Compose Material Icons
- [x] Cleaned up `AndroidManifest.xml` (removed deprecated package attribute, configured version-guarded permissions)
- [x] Implemented type-safe structured logging (`OmniLogger.kt`)
- [x] Implemented Compose Navigation routes wiring all placeholder screens (`Navigation.kt`, `NavigationKeys.kt`)
- [x] Built all placeholder screens with high-fidelity UI design matching guidelines:
  - `HomeScreen.kt` (Dashboard & dynamic routing checks)
  - `OnboardingScreen.kt` (Interactive permission status card and requested workflow)
  - `CapabilityReportScreen.kt` (Audit loading panel)
  - `CameraCaptureScreen.kt` (Pro camera viewfinder controls skeleton & right rail)
  - `CaptureResultScreen.kt` (Requested-vs-Actual comparison table layout)
  - `SettingsScreen.kt` (Diagnostics panel)
- [x] Successfully compiled using Gradle task `:app:assembleDebug` in 2m 35s on API 36

### [x] Phase 1B: Camera Capability Scanner & Data Models
- [x] Define capability profiles (`CameraDeviceProfile`, `LensProfile`)
- [x] Query and map `CameraCharacteristics` securely via `CameraManager`
- [x] Sort output resolutions, dynamic warnings, and level indicators
- [x] Add unit tests for sorting resolution sizes and parsing profiles without native Camera bindings
- [x] Implement capability data models (`CameraDeviceProfile`, `LensProfile`)
- [x] Implement `CapabilityScanner` to query `CameraManager` and safely extract `CameraCharacteristics`
- [x] Design and build `CapabilityReportScreen` showing details per lens
- [x] Implement export capability report to JSON button

### [x] Phase 1D: Camera2 Viewfinder Preview Session Management
- [x] Granular camera error mapping (`CameraError.kt`)
- [x] Camera session runtime state machine (`CameraSessionState.kt`)
- [x] Asynchronous background thread operation (`HandlerThread` in `Camera2SessionManager.kt`)
- [x] Idempotent resource cleanup and leak-free session closing (`closeCamera()`)
- [x] Mutex-guarded lens switching preventing race conditions (`switchCamera()`)
- [x] Non-stretching viewfinder preview matrix transformations (`CameraPreviewSurface.kt`)
- [x] Main-safe StateFlow UI state updates (`CameraPreviewViewModel.kt`)
- [x] Aspect-ratio matching preview resolution selection (`PreviewSizeSelector.kt`)
- [x] High-fidelity Compose layout with status displays and error boundaries (`CameraCaptureScreen.kt`)
- [x] Unit tests for aspect ratio matching and 4K filtering (`PreviewSizeSelectionTest.kt`)

### [x] Phase 1E: JPEG Still Capture & MediaStore Save
- [x] Memory-safe JPEG resolution selector capped at 16MP (`JpegSizeSelector.kt`)
- [x] JPEG rotation math complying with Camera2 recommendation (`CameraOrientationHelper.kt`)
- [x] Asynchronous MediaStore scoped storage saver with rollback logic on failure (`MediaStorePhotoSaver.kt`)
- [x] Strict single-session dual-surface still capture pipeline (`Camera2SessionManager.kt`)
- [x] Clean preview resumption after capture completes
- [x] Strict state transitions locking switcher, shutter, and back buttons during capture/save
- [x] URL-encoding URIs during Compose navigation routing to avoid character breaking
- [x] High-fidelity Capture Result Screen with asynchronous downsampled bitmap preview (`CaptureResultScreen.kt`)
- [x] Unit tests for size capping (`JpegSizeSelectorTest.kt`), orientation math (`CameraOrientationHelperTest.kt`), and target path generation (`MediaStorePathTest.kt`)

---

## Implementation Plan Overview
We have created a comprehensive, production-grade technical design plan at [implementation_plan.md](file:///C:/Users/npoor/.gemini/antigravity/brain/07ac48a8-6cb4-4aca-902c-123667cf432f/implementation_plan.md).

---

## Next Steps
1. **Begin Phase 1F (Manual Controls)**: Implement manual sliders for ISO, Shutter, Focus, and EV override along with active metadata capture comparison table.
