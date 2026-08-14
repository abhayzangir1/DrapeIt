# DrapeIt — System Architecture & Data Flow

**Version:** 1.0  
**Date:** 2026-08-14  

---

## 1. High-Level System Architecture

```
                          ┌───────────────────────────┐
                          │     SESSION INITIATION    │
                          └─────────────┬─────────────┘
                                        │
                             YouCam Skin Tone API
                              (Async Calibration)
                                        │
                                        ▼
                          ┌───────────────────────────┐
                          │   Personal Color Profile  │
                          │   (Encrypted Local Cache) │
                          └─────────────┬─────────────┘
                                        │
                                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      LANE 1: ON-DEVICE REAL-TIME LOOP                     │
│                                                                           │
│   CameraX Frame Input (Front Camera)                                      │
│        │                                                                  │
│   MediaPipe Face Mesh (478 Landmarks, Chin #152)                          │
│        │                                                                  │
│   Beard-Resilient Skin Sampler (Forehead 10,151 + High Cheeks 118,347)   │
│        │                                                                  │
│   Capture Quality & Lighting Gate (Lighting: ●●●●○ Good)                  │
│        │                                                                  │
│   CIELAB Compatibility Engine (ΔL* Contrast, Δh Hue, ΔC* Chroma)          │
│        │                                                                  │
│   100% Opaque Material Drape Canvas (Silk, Denim, Linen, Velvet, Wool)    │
│        │                                                                  │
│   Live Compatibility Score + "Why It Works" HUD                           │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                User Action
                                      │
                 ┌────────────────────┴────────────────────┐
                 ▼                                         ▼
         [ ⚖️ COMPARE MODE ]                     [ 📸 AI TRY-ON ]
                 │                                         │
        3-Up Fabric Split                         Cloudflare Worker
        (Cotton vs Silk vs Linen)                          │
                 │                                YouCam Clothes V3
                 │                                         │
                 │                                Photorealistic Look
                 │                                         │
                 └────────────────────┬────────────────────┘
                                      ▼
                          [ ✨ PROVEN LOOKS REPOSITORY ]
                                      │
                                      ▼
                             [ 🔍 FIND SIMILAR ]
```

---

## 2. Beard-Resilient Skin Sampling Flow

```
Camera Frame
    │
    ▼
MediaPipe Face Mesh Landmarks
    │
    ├─► Forehead Zone (Landmarks 10, 151, 9, 8) ────────┐
    ├─► Left High-Cheek Zone (Landmarks 118, 119) ──────┤
    ├─► Right High-Cheek Zone (Landmarks 347, 348) ─────┤
    └─► Nasal Bridge Zone (Landmarks 6, 197) ───────────┤
                                                        ▼
                                            Texture Energy / Variance Test
                                            (Rejects high-contrast stubble/hair)
                                                        │
                                                        ▼
                                            Luminance Outlier Trimming
                                            (Discards patches > 18 L* darker)
                                                        │
                                                        ▼
                                            CIELAB Trimmed Median Calculation
                                                        │
                                                        ▼
                                            Clean Skin Color Vector (L*, a*, b*)
```

---

## 3. Data Flow & Security Boundaries

* **On-Device Data (100% Private):**
  * Live camera stream, real-time facial landmark coordinates, frame-by-frame color calculations.
  * Encrypted personal profile cache in `context.filesDir`.
* **Network Data (Cloudflare Worker Proxy):**
  * Only transmitted when the user explicitly requests **YouCam Skin Calibration** or **YouCam AI Virtual Try-On**.
  * API tokens and secrets remain exclusively on Cloudflare Workers; zero API credentials in APK.
