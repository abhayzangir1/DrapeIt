# DrapeIt — Product Requirements Document (PRD)

**Version:** 2.0  
**Status:** Complete Implementation Baseline  
**Scope:** Android Native App (API 26+) + Perfect Corp YouCam Cloud AI Integration

---

## 1. Product Overview
**DrapeIt** is a fashion decision-support application that allows users to evaluate how real fabrics, textile textures, and exact colors harmonize with their complexion before buying clothes or ordering custom tailoring. It combines **real-time on-device computer vision and physical fabric shaders** with **YouCam S2S Cloud AI (Skin Tone Analysis and Clothes V3 Virtual Try-On)**.

### Product Workflow:
$$\text{Live AR Drape} \longrightarrow \text{PBR Material Shading} \longrightarrow \text{Compare Looks} \longrightarrow \text{Crop \& Prepare} \longrightarrow \text{AI Virtual Try-On}$$

---

## 2. Core Consumer Problem
When purchasing clothing online, consumers struggle to know:
1. **Complexion Harmony:** Does this specific hue and saturation clash with or wash out my skin tone?
2. **Fabric & Material Interaction:** How does the texture (e.g., sheen of silk, grain of leather, slub of linen, ribs of corduroy) alter how a color appears against the face?
3. **Alternative Comparison:** Which of 2 to 4 candidate colors/fabrics performs best on my face?
4. **Garment Isolation:** How can I test clothes from e-commerce screenshots without distracting backgrounds or hangers degrading the try-on?
5. **Photorealistic AI Try-On:** What will the finished garment look like realistically fitted on my body?

---

## 3. Product Principles
1. **Luminance-Preserving Textures:** Never paint flat color lines. Use real scanned micro-structures blended with user colors to simulate physical textiles.
2. **Local-First Real-Time AR:** The live camera draping loop runs 100% on-device at 60 FPS without network lag.
3. **Zero API Keys in Binary:** Protect third-party API credentials via a secure stateless proxy gateway.
4. **Seamless User Fallbacks:** If a user tests a garment without uploading a portrait, provide instant studio AI fit models rather than blocking the flow.
5. **Explainable Metrics:** Provide clear optical reasons for compatibility (Luminance Contrast, Chroma, Undertone Alignment) instead of arbitrary scores.

---

## 4. Feature Specifications

### 4.1 Real-Time AR Live Drape Studio
* **MediaPipe FaceMesh Tracking:** Tracks 478 landmarks (chin anchor point 152) to project a tailored cloth polygon across the user's chest.
* **Exponential Landmark Smoothing:** Applies a low-pass filter ($\alpha = 0.28$) on chin coordinates to remove camera jitter and give virtual cloth realistic stability.
* **Beard-Resilient Skin Sampler:** Samples forehead (landmarks 10, 151) and high sub-orbital cheeks (118, 347) above beard lines with luminance outlier trimming.
* **Live Perceptual Harmony Engine:** Computes CIEDE2000 color difference and CIELAB lightness contrast in real time.

### 4.2 Dual-Layer PBR Fabric Shaders (14 Materials)
Renders a 4-pass luminance-preserving composite using tileable bump maps (`res/drawable-nodpi/`) and Compose Canvas:
1. **Mulberry Silk (✨):** Anisotropic specular sheen and fluid drape.
2. **Lustrous Satin (💎):** Liquid gloss reflections.
3. **Genuine Leather (🧥):** Cellular Voronoi grain, fine creases, and edge highlights.
4. **Heritage Tweed (🧵):** Herringbone zig-zag bouclé weave with slub flecks.
5. **Plush Corduroy (👖):** 8-wale vertical parallel cord ridges with deep shadow valleys.
6. **Natural Linen (🌾):** Organic irregular slub crosshatch.
7. **Structured Denim (👖):** 45° 3x1 twill diagonal ribs.
8. **Plush Velvet (👑):** Dense cut-pile nap with inverted Fresnel retro-reflection.
9. **Pure Cashmere (🧣):** Brushed cloud fuzz and micro-fleece nap.
10. **Merino Wool (🐑):** Worsted interlocking yarn loop knit.
11. **Sheer Chiffon (🪶):** Translucent grid with light pass-through.
12. **Ribbed Knit (🧶):** 2x2 vertical ribbed knit channels.
13. **Organic Cotton (🌿):** Soft plain basketweave.
14. **Tech Polyester (🏃):** Technical micro-piqué athletic honeycomb mesh.

### 4.3 Universal 360° HSV Color Picker (16.7M Colors)
* **Interactive Color Wheel Dialog:** Full 360° Hue slider, Saturation/Value sliders, and live preview swatch.
* **Direct Hex Validation:** Accepts direct `#RRGGBB` hex text input with error handling.
* **Luxury Seasonal Presets:** 1-tap quick swatches (Royal Burgundy, Cobalt Navy, Deep Emerald, Terracotta, Midnight Charcoal, etc.).

### 4.4 Interactive On-Device Garment Cropper & Normalizer
* **Compose Viewport:** Pinch-to-zoom, pan, rotate 90°, and 3:4 portrait crop guides.
* **YouCam AI Canvas Normalization:** Composites the cropped garment onto a solid white background (`#FFFFFF`) with 5% padding, bounds to 1280px max edge, and exports optimized JPEG (88% quality) on `Dispatchers.Default` to maximize cloud VTO segmentation accuracy.

### 4.5 Photorealistic AI Virtual Try-On (YouCam Clothes V3)
* **Cloud VTO Integration:** Securely calls YouCam `/s2s/v2.0/task/cloth-v3` with `garmentCategory = "upper_body"`.
* **Missing Input Prompts:**
  * If no selfie is available, offers **`👤 Use AI Fit Model`** for instant studio testing.
  * If only a selfie is available, prompts to pick a curated palette look or upload a product photo.

### 4.6 Photo Compare Studio
* Multi-select 1 to 4 captured looks in a side-by-side comparison collage.
* Displays match scores, fabric specs, and **✨ WINNER** ribbons.
* Empty state CTA to launch the Drape Studio immediately.

### 4.7 Explore & Looks
* Curated occasion colorways across **Everyday**, **Office & Work**, **Evening Occasion**, and **Formal / Gala**.
* 1-tap `[ Drape ]` and `[ Try-On ]` action buttons on all occasion cards.
* Personal wardrobe memory storing saved outfits and suited colors.

---

## 5. Non-Goals (Explicit Exclusions)
* ❌ Social media feeds, public follower graphs, or likes.
* ❌ AI chatbot stylists ("Ask AI anything").
* ❌ Invasive ad-tracking networks or third-party behavioral analytics.
* ❌ Unexplainable "beauty rating" scores.
