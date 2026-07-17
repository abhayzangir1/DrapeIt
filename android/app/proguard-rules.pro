# DrapeProof currently keeps release minification disabled for transparent hackathon review.
# If minification is enabled later, preserve MediaPipe task container metadata.
-keep class com.google.mediapipe.tasks.components.containers.** { *; }
