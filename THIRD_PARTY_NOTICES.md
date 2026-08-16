# Third-Party Notices

## MediaPipe Face Landmarker

DrapeIt bundles Google's `face_landmarker.task` model and uses the MediaPipe Tasks Vision Android library for on-device facial landmarks.

- Model card: https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Face%20Mesh%20V2.pdf
- Android API documentation: https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker/android
- License: Apache License 2.0, as stated by the model card and MediaPipe project.

MediaPipe output is used solely for regions of interest and capture-quality guidance. DrapeIt does not use it for identity recognition, attractiveness, medical, ethnicity, or demographic inference.

## AndroidX, Jetpack Compose, and Supporting Libraries

The Android app uses AndroidX/Compose, Kotlin, CameraX, and related transitive libraries under their respective open-source licenses. Gradle dependency metadata is the source of truth for the exact resolved versions.

## Perfect Corp YouCam APIs

Perfect Corp APIs are accessed remotely through the secure proxy Worker. No YouCam SDK, API key, or Perfect Corp model is redistributed in the APK.
