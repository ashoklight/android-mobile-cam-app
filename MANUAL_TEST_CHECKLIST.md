# Manual Test Checklist

Use this checklist to perform rigorous hands-on validation of the application on both physical test devices and emulators.

---

## Phase 1A – Onboarding, Permissions, and Navigation

| Test Case ID | Test Case Description | Expected Result | Pass / Fail | Comments |
| :--- | :--- | :--- | :---: | :--- |
| **TC-1A-01** | First Launch / Fresh Install | App launches directly into **Onboarding Screen**. Explanatory text describes the dynamic capability philosophy. | | |
| **TC-1A-02** | Core Camera Permission Deny | Tap "Grant Camera Access", select Deny. App stays on **Onboarding Screen**, showing Camera status as Grey (Un-granted) and does not proceed. | | |
| **TC-1A-03** | Core Camera Permission Grant | Tap "Grant Camera Access", select Allow. App automatically navigates and transitions to the **Home Screen (Dashboard)**. | | |
| **TC-1A-04** | Launch Already Granted | Close and relaunch the app. Since Camera permission is already granted, it bypasses the Onboarding Screen and opens directly to the **Home Screen**. | | |
| **TC-1A-05** | Dashboard Menu Navigation | Tap each menu button on the Home Screen: <br>- "Start Camera Engine" -> Opens **Camera Capture Viewfinder Screen** <br>- "Device Capability Report" -> Opens **Capabilities Screen** <br>- "Preferences & Diagnostics" -> Opens **Settings Screen** | | |
| **TC-1A-06** | Viewfinder Back Navigation | Inside **Camera Capture**, tap the Back Arrow in the top overlay. App returns cleanly to the **Home Screen**. | | |
| **TC-1A-07** | Capabilities Back Navigation | Inside **Device Capabilities**, tap the Back Arrow in the top app bar. App returns cleanly to the **Home Screen**. | | |
| **TC-1A-08** | Settings Back Navigation | Inside **Preferences & Diagnostics**, tap "Return to Dashboard" or the Back Arrow. App returns cleanly to the **Home Screen**. | | |
| **TC-1A-09** | Capture Result Flow | Inside **Camera Capture**, tap the Gallery shortcut or the main white shutter button. App navigates to **Capture Verification Screen**. | | |
| **TC-1A-10** | Verification Back Navigation | Inside **Capture Verification**, tap "Done" or the Back Arrow. App returns to **Camera Capture Screen**. | | |
| **TC-1A-11** | Screen Rotation / Lifecycle | Rotate the phone while on any of the placeholder screens. The layouts adjust responsively and state remains consistent without crashes. | | |
| **TC-1A-12** | Permission Revoked at System | While on **Home Screen**, send the app to background, open Android Settings, revoke Camera permission, and resume the app. App detects revocation and automatically redirects to the **Onboarding Screen**. | | |

---

## Phase 1B – Camera Capability Scanner & Data Models

| Test Case ID | Test Case Description | Expected Result | Pass / Fail | Comments |
| :--- | :--- | :--- | :---: | :--- |
| **TC-1B-01** | Open Capability Report | Scanned profiles are read successfully. Auditing overlay appears briefly, followed by the complete list of available camera IDs. | | |
| **TC-1B-02** | Lens Labels Check | Verify each lens card contains correct facing labels (e.g. `REAR CAMERA`, `FRONT SELFIE`) matching the physical lens configurations. | | |
| **TC-1B-03** | Capability Chip State | Green (Supported) and Red (Unsupported) chips for RAW, MANUAL, BURST, and SLOW-MO render dynamically according to lens profile capability set. | | |
| **TC-1B-04** | Section Expansion | Tap on a lens card to expand it. Detail cards for Sensor, Optical Lens, Photo Resolutions, Video, and Extensions appear with proper styling. | | |
| **TC-1B-05** | Rescan / Refresh | Tap "Rescan" in the top bar. Auditing loading overlay appears, rescans the device's lenses successfully, and refreshes the data without any crashes. | | |
| **TC-1B-06** | JSON Export Report | Tap "Export JSON" in the top bar. A success Toast notification displays: `Report exported: OmniCam_Capability_Report_YYYYMMDD_HHMMSS.json`. | | |
| **TC-1B-07** | Local JSON File Verification | Use a file manager to verify the `.json` report is successfully saved inside `/Android/data/com.pna.omnicamlab/files/documents/` and contains valid JSON keys. | | |
| **TC-1B-08** | Emulated / Missing Keys Safety | Run the app on an emulator. Verify the scanner safely handles any null or missing CameraCharacteristics keys and renders fallback indicators cleanly without crashes. | | |

---

## Phase 1D – Viewfinder Preview Session Management
 
| Test Case ID | Test Case Description | Expected Result | Pass / Fail | Comments |
| :--- | :--- | :--- | :---: | :--- |
| **TC-1D-01** | Live Viewfinder Preview | Open camera engine. Viewfinder renders live preview smoothly inside `CameraCaptureScreen` via compose-wrapped TextureView. Loading overlay appears and disappears properly. | | |
| **TC-1D-02** | Viewfinder Aspect Ratio | Rotate device between Portrait and Landscape. Matrix transformation ensures the viewfinder does not stretch, cropped using a center-crop strategy to fill the view. | | |
| **TC-1D-03** | Selfie Camera Mirroring | Select a FRONT camera lens. Viewfinder preview applies mirror matrix transformation (horizontal flip) correctly to provide a natural selfie view. | | |
| **TC-1D-04** | Lens Cycling & Switch | Tap the Camera Switch button. Viewfinder state transitions to "Switching Lenses..." (showing switching overlay), closes the current session fully, and opens the next lens safely without race conditions or freezes. | | |
| **TC-1D-05** | Lifecycle Teardown & Resume | Start viewfinder preview, press Home button to put app in background. Device logs show the Camera2 session and device are fully closed. Re-open/resume app: the preview session is automatically recreated and preview starts again. | | |
| **TC-1D-06** | Leaving Screen Teardown | Navigate back from Camera Capture screen using the Back button. Verify the camera device and capture session are closed immediately and background thread is quit safely. | | |
| **TC-1D-07** | Error Boundary Handling | Trigger a hardware/permission issue or simulated camera disconnect. Readable Error Card appears showing translated user message with debug details and buttons to return home or retry. | | |

---

## Phase 1D-R4 – Real-device TextureView Preview Orientation Calibration

| Test Case ID | Test Case Description | Expected Result | Pass / Fail | Comments |
| :--- | :--- | :--- | :---: | :--- |
| **TC-1D-R4-01** | Forced Transform Selector | Open camera engine. Verify debug overlay shows new calibration buttons: `AUTO`, `0°`, `90°`, `180°`, `270°`. | | |
| **TC-1D-R4-02** | Calibration Workflow (Poco F7) | Set device to Portrait. Cycle through `0°`, `90°`, `180°`, `270°` buttons. Record which angle renders the preview visually upright. | | |
| **TC-1D-R4-03** | Calibration Landscape Check | Rotate device to Landscape. Cycle through overrides. Record which rotation renders the landscape preview visually upright. | | |
| **TC-1D-R4-04** | Telemetry Verification | Verify that when clicking a calibration option, `Transform Source` displays `FORCED_<angle>`, `forcedPreviewRotationDegrees` displays the correct angle, and `activePreviewRotationDegrees` updates immediately. | | |
| **TC-1D-R4-05** | Skew and Stretch Free | Move and tilt the device slowly in forced modes. The preview renders with uniform aspect ratio (stretch-free, skew-free) center-cropped. | | |
| **TC-1D-R4-06** | Capture Orientation Decoupling | Capture a photo using a forced preview override. Verify the preview does not flicker/reset, and the saved photo in the result screen remains perfectly upright matching JPEG orientation rules. | | |

---

## Phase 1E – JPEG Still Capture & MediaStore Save

| Test Case ID | Test Case Description | Expected Result | Pass / Fail | Comments |
| :--- | :--- | :--- | :---: | :--- |
| **TC-1E-01** | Capture Shutter Lock | Open camera viewfinder. Tap Shutter. Verify shutter button and switcher button become locked/disabled during capture/saving state. | | |
| **TC-1E-02** | JPEG Resolution Capping | Capture photo on a high megapixel camera device. Check logs to verify selected JPEG resolution is capped at 16MP to prevent memory stress. | | |
| **TC-1E-03** | Viewfinder Resumption | Tap Shutter, wait for capture to complete. Viewfinder preview must resume automatically and smoothly. | | |
| **TC-1E-04** | Back/Front Orientation Math | Capture photos in Portrait and Landscape modes (including selfie mirroring). Verify image files in Gallery have correct rotation metadata matching orientation formula. | | |
| **TC-1E-05** | MediaStore Modern vs Legacy | Verify photos save to `Pictures/OmniCam/OmniCam_YYYYMMDD_HHMMSS_Photo/` using Scoped Storage on Android 10+ and standard files on legacy Android 9. | | |
| **TC-1E-06** | Navigation URI Transfer | Tap shutter. Verify app transitions safely to CaptureResultScreen, URL-encoding the savedUri without route format issues. | | |
| **TC-1E-07** | Downsampled Thumbnail Display | Verify CaptureResultScreen asynchronously reads and downsamples the image stream to render a high-quality preview thumbnail. | | |
| **TC-1E-08** | Passive Metadata Display | Verify CaptureResultScreen correctly extracts and renders ISO, Shutter Speed (Exposure Time), Aperture, and Focal Length, displaying "Not available" if missing. | | |
| **TC-1E-09** | Failure Cleanup | Simulate stream write failure. Verify the saver closes the output stream, deletes/cancels the pending MediaStore row, and displays a user error. | | |

---

## Future Phase Testing Indicators (Reference for Phase 1F - 1G)

- **Phase 1F**: Verify changing manual ISO, shutter, and focus values correctly writes parameters to the Camera2 request and displays verification differences side-by-side in the results.
- **Phase 1G**: Verify RAW toggle only appears if the lens supports it, and capturing saves matching `.jpg` and `.dng` filenames cleanly.
