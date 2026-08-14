# DrapeIt — Technical Requirements & Design (TRD)

**Version:** 1.0  
**Date:** 2026-08-14  
**Status:** Final Technical Specification for YouCam API Hackathon  

---

## 1. Technical Objective
Architect and build a high-performance native Android application delivering:
1. **60 FPS On-Device Real-Time Loop:** CameraX + MediaPipe Face Mesh + Beard-Resilient Skin Sampler + Local CIELAB Perceptual Compatibility Engine + Material Weave Shader Drape.
2. **Asynchronous Cloud AI Calibration & Generation:** Cloudflare Worker proxy connecting to Perfect Corp's YouCam Skin Tone API (one-time calibration) and YouCam Clothes V3 API (photorealistic virtual try-on).
3. **Zero-Friction Local Persistence:** Encrypted local cache for personal color profiles and Proven Looks.

---

## 2. Technology Stack & Platform

* **Client Platform:** Android 14 (API 34) target, minSDK 26
* **Language & UI:** Kotlin 2.0 + Jetpack Compose + Material 3 Foundation with Custom Editorial Luxury Design System
* **Computer Vision:** Google MediaPipe Face Mesh (478 3D landmarks on-device) + CameraX 1.3
* **Color Science Engine:** CIELAB (CIE 015:2018), CIEDE2000 $\Delta E_{00}$, sRGB linear transformation, unconstrained optical harmony
* **Backend API Gateway:** Cloudflare Workers (TypeScript) with sub-second proxying, payload validation, and task polling
* **Partner APIs:** Perfect Corp. YouCam REST APIs (Skin Tone / Facial Color Analysis + Clothes V3 VTO)
* **Local Persistence:** Encrypted SharedPreferences + Private App Storage (`context.filesDir/avatars`)

---

## 3. System Architecture & 3-Lane Concurrency Model

```
                                    DRAPEIT
                                       │
                      ┌────────────────┴────────────────┐
                      │                                 │
           ┌──────────▼──────────┐           ┌──────────▼──────────┐
           │ LANE A: REAL-TIME   │           │ LANE B: CALIBRATION │
           │ (On-Device 60 FPS)  │           │ (YouCam Skin API)   │
           └──────────┬──────────┘           └──────────┬──────────┘
                      │                                 │
         CameraX Frame Acquisition              One-time snapshot
                      │                                 │
         MediaPipe Face Mesh (Chin #152)        YouCam Skin Tone API
                      │                                 │
         Beard-Resilient Skin Sampling                  ▼
                      │                     ┌───────────────────────┐
         Capture Quality Evaluation         │ Personal Color Profile│
                      │                     │  (Encrypted Cache)    │
         CIELAB Compatibility Engine ◄──────┤                       │
                      │                     └───────────────────────┘
         100% Opaque Material Drape                     │
                      │                                 │
                      ▼                                 │
           Live Score + "Why" HUD                       │
                      │                                 │
                      ├─────────────────────────────────┘
                      │
               User taps "AI Try-On"
                      │
                      ▼
           ┌─────────────────────┐
           │ LANE C: GENERATION  │
           │ (YouCam Clothes V3) │
           └──────────┬──────────┘
                      │
           Cloudflare Worker Gateway
                      │
           YouCam Clothes V3 VTO Task
                      │
                      ▼
           Photorealistic Try-On Image
                      │
                      ▼
           [ Proven Looks Repository ] ──► [ Find Similar ]
```

---

## 4. Computer Vision & Beard-Resilient Skin Sampling

### A. Hair-Immune Facial Sample Landmarks
To ensure accurate skin tone readings across all users (including men with full beards, mustaches, goatees, or heavy stubble), sampling is restricted to **upper-facial hair-immune zones**:
1. **Mid-Forehead & Glabella:** Landmarks `10`, `151`, `9`, `8` *(100% hair-free anchor)*
2. **High Inner Sub-Orbital Cheeks:** Landmarks `118`, `119` (left) and `347`, `348` (right) *(positioned directly under the tear trough, safely above any natural beard line)*
3. **Upper Nasal Bridge:** Landmarks `6`, `197`
4. **Upper Malar Temples:** Landmarks `127`, `356`

### B. Patch Variance & Outlier Rejection Algorithm
For each sample patch ($12 \times 12$ pixels):
1. **Texture Energy / Variance Test:** Calculate local luminance variance $\sigma^2$. If $\sigma^2 > \text{Threshold}_{\text{smooth}}$, the patch contains high-frequency hair/stubble texture and is discarded.
2. **Luminance Outlier Trimming:** If a patch's luminance $L^*$ is $> 18$ units darker than the forehead anchor, it is flagged as hair/shadow occlusion and discarded.
3. **CIELAB Trimmed Median:** Surviving patch colors are converted to CIELAB, and the median $(L^*, a^*, b^*)$ vector is extracted.

### C. Chin Tracking for Drape Geometry
* Landmark `152` (bottom of chin) and landmarks `234`/`454` (jaw ear base) define the dynamic drape polygon, updating at 60 FPS to follow head and neck movements smoothly.

---

## 5. Capture Quality & Lighting Engine

The app evaluates 5 real-time quality signals before finalizing measurements:
$$\text{Confidence} = 0.30 \cdot S_{\text{exposure}} + 0.25 \cdot S_{\text{colorCast}} + 0.20 \cdot S_{\text{faceVisibility}} + 0.15 \cdot S_{\text{framing}} + 0.10 \cdot S_{\text{stability}}$$

* **$S_{\text{exposure}}$:** Evaluates relative luminance ($0.20 \le Y \le 0.85$). Flags overexposure / underexposure.
* **$S_{\text{colorCast}}$:** Evaluates white-point chromatic shift. Flags warm indoor incandescent lighting ($> 3000\text{K}$ shift).
* **$S_{\text{faceVisibility}}$:** Validates MediaPipe facial landmark visibility scores ($> 0.85$).
* **$S_{\text{framing}}$:** Confirms face oval center is within the central $50\%$ viewport bounding box.
* **$S_{\text{stability}}$:** Checks inter-frame motion delta to discard motion blur.

---

## 6. Perceptual Color Compatibility Engine

Converts sampled skin $(L^*_{\text{skin}}, a^*_{\text{skin}}, b^*_{\text{skin}})$ and candidate fabric color $(L^*_{\text{fabric}}, a^*_{\text{fabric}}, b^*_{\text{fabric}})$ to calculate:

### 1. Luminance Contrast ($\Delta L^*$)
$$\Delta L^* = |L^*_{\text{skin}} - L^*_{\text{fabric}}|$$
* Penalizes colors with $\Delta L^* < 12$ (washes out facial features).
* Rewards moderate-to-high contrast ($20 \le \Delta L^* \le 55$).

### 2. Chroma Saturation Relationship ($\Delta C^*$)
$$C^* = \sqrt{a^{*2} + b^{*2}}, \quad \Delta C^* = |C^*_{\text{skin}} - C^*_{\text{fabric}}|$$
* Penalizes hyper-saturated neon shades that overpower subtle skin tones.

### 3. Hue & Undertone Alignment ($\Delta h$)
$$h = \operatorname{atan2}(b^*, a^*)$$
* Evaluates warm vs. cool undertone compatibility.
* Identifies discordant sickly/ashen color clashes, dropping compatibility scores realistically into the $15\% - 45\%$ range.

### 4. Overall Compatibility Score
$$\text{Score} = \left(0.40 \cdot S_{\text{contrast}} + 0.35 \cdot S_{\text{undertone}} + 0.25 \cdot S_{\text{chroma}}\right) \times 100\%$$

---

## 7. Material-Aware Fabric Model & Shaders

Each fabric is modeled with physical optical coefficients:

```kotlin
data class FabricProfile(
    val id: String,
    val name: String,
    val weaveType: WeaveType,
    val roughness: Float,
    val luster: Float,
    val specularStrength: Float,
    val absorption: Float,
    val textureStrength: Float
)
```

### Hero Shading Pipelines:
* **Mulberry Silk / Satin:** `Brush.verticalGradient` with specular sheen crest bands ($A=0.38$) and liquid drape trough shadows.
* **Structured Denim:** 45° diagonal 3x1 twill weave shadow lines + double contrast collar top-stitching.
* **Natural Linen:** Horizontal and vertical organic slub weave ridges ($18\text{dp}$ interval).
* **Plush Velvet:** `Brush.horizontalGradient` with deep directional light absorption and velvety pile edge sheen.
* **Merino Wool & Cashmere:** Dense, soft matte diffuse micro-grain.

---

## 8. State Machine & Architecture

```kotlin
enum class DrapeAppState {
    UNINITIALIZED,
    PERMISSION_REQUIRED,
    CALIBRATING,
    READY,
    LIVE_DRAPE,
    PHOTO_MODE,
    COMPARE,
    TRY_ANYTHING,
    TRY_ON_PROCESSING,
    TRY_ON_RESULT,
    SAVED,
    ERROR
}
```

---

## 9. Cloud Gateway & YouCam Integration

* **Endpoint:** `POST https://youcam-proxy.drapeit.workers.dev/v1/tasks/try-on`
* **Security:** API keys and credentials reside exclusively in Cloudflare Worker environment secrets. Zero hardcoded secrets in APK.
* **Resilience:** If network is unavailable or rate-limited, local 60 FPS live drape continues uninterrupted.
