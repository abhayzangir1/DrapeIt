# DrapeIt — Technical Requirements & Specification (TRD)

**Version:** 2.0  
**Target:** Android 14+ (compileSdk 36, targetSdk 36, minSdk 26)  
**Modules:** `:app` (Jetpack Compose UI & CameraX), `:core` (Pure Kotlin Colorimetry Engine)  
**Backend:** Cloudflare Workers (TypeScript) with Durable Object Budget Ledger

---

## 1. Technical Architecture Overview

DrapeIt decouples local real-time perception from heavy cloud generative AI:
1. **On-Device 60 FPS Perception Loop:** CameraX + MediaPipe FaceMesh + Exponential Landmark Filtering + Beard-Resilient Skin Sampling + CIELAB Perceptual Engine + 4-Pass PBR Textile Shaders.
2. **Interactive Preparation Pipeline:** Compose-native Pinch/Zoom Garment Cropper + Solid White Background Normalizer.
3. **Stateless Cloud AI Gateway:** Cloudflare Worker proxy connecting to Perfect Corp's YouCam Skin Tone Analysis and YouCam Clothes V3 Virtual Try-On APIs.

---

## 2. Technology Stack

* **Client Core:** Kotlin 2.3.20, Coroutines 1.10.2, Jetpack Compose (BOM 2026.03.01), Material 3.
* **Computer Vision:** Google MediaPipe Tasks Vision 0.10.35 (`FaceLandmarker` with 478 3D landmarks) + CameraX 1.6.1.
* **Color Science:** Platform-independent pure Kotlin `:core` library (CIELAB 1976, CIEDE2000 $\Delta E_{00}$, sRGB $\leftrightarrow$ CIELAB $\leftrightarrow$ XYZ conversions).
* **Shading Engine:** Dual-Layer PBR Shading via Compose Canvas, `ImageShader(TileMode.Repeated)`, `BlendMode.Overlay` / `BlendMode.Hardlight` / `BlendMode.Softlight` / `BlendMode.Multiply`.
* **Backend API Gateway:** TypeScript Cloudflare Worker with SQLite-backed Durable Object for transactional unit budget tracking.
* **APIs Integrated:** YouCam S2S Cloud `/s2s/v2.0/task/cloth-v3` and `/s2s/v2.0/task/skin-tone-analysis`.

---

## 3. Computer Vision & Landmark Smoothing

### A. Face Tracking & Landmark Smoothing
MediaPipe provides normalized 3D facial landmarks at 60 FPS. Raw tracking data exhibits micro-jitter when hand-held; to achieve physical cloth stability, coordinates are filtered using an exponential low-pass filter:

$$\hat{x}_t = \hat{x}_{t-1} + \alpha \cdot (x_t - \hat{x}_{t-1}), \quad \alpha = 0.28$$
$$\hat{y}_t = \hat{y}_{t-1} + \alpha \cdot (y_t - \hat{y}_{t-1}), \quad \alpha = 0.28$$

* **Chin Anchor (Landmark 152):** Used as the primary top anchor for the chest drape polygon.
* **Head Pose Yaw Estimation:** Derived from normalized cheek offsets $(\Delta x_{\text{chin}} - 0.5) \cdot \pi$ and passed to shaders to drive specular sheen angles dynamically.

### B. Beard-Resilient Skin Sampling
To prevent facial hair, stubble, or shadows from distorting color calculations:
1. **Upper-Facial Sampling Zones:**
   * Forehead Glabella (Landmarks 10, 151, 9, 8)
   * High Sub-Orbital Cheeks (Landmarks 118, 119 and 347, 348)
   * Upper Nasal Bridge (Landmarks 6, 197)
2. **Variance & Luminance Outlier Trimming:** Discards sample patches with high local luminance variance ($\sigma^2 > \text{threshold}$) or luminance $L^*$ more than 18 units darker than the forehead baseline.
3. **Median CIELAB Aggregation:** Computes the trimmed median $(L^*, a^*, b^*)$ vector across surviving patches.

---

## 4. Perceptual Color Compatibility Engine

Converts sampled skin $(L^*_{\text{skin}}, a^*_{\text{skin}}, b^*_{\text{skin}})$ and candidate fabric color $(L^*_{\text{fabric}}, a^*_{\text{fabric}}, b^*_{\text{fabric}})$ to calculate:

1. **Lightness Contrast ($\Delta L^*$):**
   $$\Delta L^* = |L^*_{\text{skin}} - L^*_{\text{fabric}}|$$
   Penalizes washed-out combinations ($\Delta L^* < 12$), rewards clean facial separation ($20 \le \Delta L^* \le 55$).

2. **Chroma Saturation ($\Delta C^*$):**
   $$C^* = \sqrt{a^{*2} + b^{*2}}, \quad \Delta C^* = |C^*_{\text{skin}} - C^*_{\text{fabric}}|$$

3. **Hue & Undertone Alignment ($\Delta h$):**
   $$h = \operatorname{atan2}(b^*, a^*)$$

4. **Overall Harmony Score:**
   $$\text{Score} = \left(0.40 \cdot S_{\text{contrast}} + 0.35 \cdot S_{\text{undertone}} + 0.25 \cdot S_{\text{chroma}}\right) \times 100\%$$

---

## 5. Dual-Layer PBR Fabric Shading Engine

To render authentic textiles without 3D polygon meshes, `FabricTextureShader.kt` executes a **4-pass luminance-preserving composite** using 14 seamless tileable grayscale bump maps:

```kotlin
data class FabricMaterial(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val luster: FabricLuster,
    val drape: FabricDrape,
    val breathability: String,
    val weaveType: String,
    val blendMode: BlendMode = BlendMode.Overlay,
    val textureAlpha: Float = 0.92f,
    val aoAlpha: Float = 0.28f,
    val specularStrength: Float = 0.15f,
    val anisotropy: Float = 0.0f,
    val sheen: Float = 0.0f,
)
```

### Composite Passes:
1. **Base Pass:** Solid fill with user's selected `#HEX` color.
2. **Micro-Weave Pass:** Repeating `ImageShader(tileBitmap, TileMode.Repeated)` rendered with `BlendMode.Overlay` / `BlendMode.Hardlight` / `BlendMode.Softlight` at `textureAlpha`.
3. **Ambient Occlusion Pass:** Radial gradient from neck center with `BlendMode.Multiply` at `aoAlpha`.
4. **Specular Sheen Pass:** Linear gradient sweep centered at `width * (0.50 + sin(yaw) * 0.35)` with `BlendMode.Overlay`.
5. **Velvet Fresnel Pass:** Rim highlight gradient with `BlendMode.Screen` at `sheen` intensity.

---

## 6. On-Device Garment Cropping & Normalization

* **Interactive Gesture Viewport:** Handles 2-finger pinch-to-zoom, pan translation, and 90° rotation inside a 3:4 aspect bounding frame.
* **YouCam AI Canvas Normalization:** Renders the cropped garment onto an off-screen `1024x1280` canvas with solid white background (`#FFFFFF`) and 5% padding.
* **Encoding:** Scales down to $\le 1280\text{px}$ longest edge and compresses to 88% quality sRGB JPEG on `Dispatchers.Default`.

---

## 7. Security & Cloudflare Proxy Gateway

* **Client Security:** Zero API keys bundled in APK. The app acquires temporary session tokens from `POST /v1/session`.
* **State & Quota Ledger:** The Worker uses a SQLite-backed Durable Object (`PAID_TASK_LEDGER`) to track unit costs (2 units for Clothes V3, 20 units for Skin Tone Analysis) and enforce safety floors.
* **Offline Resilience:** If network connectivity is lost, on-device AR live drape, PBR material shaders, and photo compare continue operating with 0% feature loss.
