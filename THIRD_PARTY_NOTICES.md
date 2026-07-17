# Third-party notices

## MediaPipe Face Landmarker

DrapeProof bundles Google's `face_landmarker.task` model and uses the MediaPipe Tasks Vision Android library for on-device facial landmarks.

- Model card: https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Face%20Mesh%20V2.pdf
- Android API documentation: https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker/android
- License: Apache License 2.0, as stated by the model card and MediaPipe project.
- Bundled model SHA-256: `64184E229B263107BC2B804C6625DB1341FF2BB731874B0BCC2FE6544E0BC9FF`.

MediaPipe output is used for regions of interest and capture-quality guidance. DrapeProof does not use it for identity recognition, attractiveness, medical, ethnicity, or demographic inference.

## AndroidX, Kotlin, and supporting libraries

The Android app uses AndroidX/Compose, Kotlin, CameraX, and related transitive libraries under their respective open-source licenses. Gradle dependency metadata is the source of truth for the exact resolved versions.

## Perfect Corp YouCam APIs

Perfect Corp APIs are accessed remotely through the secure Worker. No YouCam SDK, API key, or Perfect Corp model is redistributed in the APK. Use remains subject to the YouCam API and hackathon terms.

## Original generated demo asset

See `demo-assets/README.md`. The cobalt scarf image is visualization-only and is excluded from measurement evidence.
