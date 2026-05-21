# Camera Feature Matrix

This matrix tracks the implementation status of all current and planned hardware/software features, clearly showing what is verified, pending, simulated, or restricted by the Android Camera2 API and OEM layers.

---

## Core Feature Roadmap

| Feature / Capability | Supported (API) | UI Support Badge | Phase Status | Verification Method |
| :--- | :---: | :---: | :---: | :--- |
| **Project Skeleton & Navigation** | Yes | N/A | **Phase 1A: COMPLETED** | Automated build & navigation check |
| **Permissions Handling** | Yes | N/A | **Phase 1A: COMPLETED** | Manual runtime grant check |
| **Placeholder Screens** | Yes | N/A | **Phase 1A: COMPLETED** | Manual navigation check |
| **Camera ID & Lenses List** | Safe Fallback | N/A | **Phase 1B: COMPLETED** | unit tests + CameraManager query |
| **Characteristics Mapping** | Safe Fallback | N/A | **Phase 1B: COMPLETED** | unit tests + Characteristics mock |
| **Capability Report UI** | Safe Fallback | N/A | **Phase 1B: COMPLETED** | Expandable list scroll verification |
| **Export Report as JSON** | Safe Fallback | N/A | **Phase 1B: COMPLETED** | Local storage write check |
| **Viewfinder Preview (Camera2)** | Safe Fallback | N/A | **Phase 1D: COMPLETED** | Compose AndroidView surface render |
| **Lens Switcher** | Device-Specific | N/A | **Phase 1D: COMPLETED** | Switch cameras in active session |
| **JPEG Still Capture** | Yes | N/A | **Phase 1E: COMPLETED** | ImageReader callback + output file |
| **MediaStore Saving (JPEG)** | Yes | N/A | **Phase 1E: COMPLETED** | Gallery insertion check |
| **Manual ISO Slider** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest sensitivity override |
| **Manual Shutter Slider** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest exposure time override |
| **Manual Focus Slider** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest focus distance override |
| **Manual EV Exposure Comp** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest aeCompensation override |
| **Manual AWB Mode Select** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest awbMode override |
| **Flash / Torch Mode** | Device-Specific | Green / Red | Phase 1F: PENDING | CaptureRequest flashMode override |
| **Control Verification (Actuals)** | Yes | Blue / Yellow | Phase 1F: PENDING | CaptureResult vs Request parameters |
| **RAW/DNG Still Capture** | Device-Specific | Blue / Red | Phase 1G: PENDING | DngCreator + sensor capture |
| **Video Recording** | Safe Fallback | N/A | Phase 3: PENDING | MediaRecorder/MediaCodec pipeline |
| **Computational Stack Overlays** | Simulated | Grey | Phase 4: PENDING | Canvas pixel analysis overlays |

---

## Dynamic UI Support Badge Rules

OmniCam Lab relies on dynamic visual feedback to explain hardware realities instead of hiding unsupported features:

- <span style="color:green">**Green**</span>: **SUPPORTED_VERIFIED** — CameraCharacteristics officially reports the feature, and it is fully active.
- <span style="color:gold">**Yellow**</span>: **SUPPORTED_UNVERIFIED** — CameraCharacteristics reports support, but active hardware/OEM limits are unverified in runtime.
- <span style="color:blue">**Blue**</span>: **EXTENSION_ONLY** — Only available using Vendor/OEM Extensions (e.g. Bokeh, Night, HDR).
- <span style="color:grey">**Grey**</span>: **SOFTWARE_SIMULATED** — Simulates manual settings or frame processing in software (e.g. simulated Kelvin WB).
- <span style="color:red">**Red (Disabled)**</span>: **UNSUPPORTED** — Exposes "Unsupported on this lens" in tooltip when tapped.
