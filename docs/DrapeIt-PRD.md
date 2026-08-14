# DrapeIt — Product Requirements Document (PRD)

**Version:** 1.0  
**Date:** 2026-08-14  
**Status:** Final Product Direction for YouCam API Hackathon  

---

## 1. Product Definition
**DrapeIt** is a premium Android fashion decision-support application that helps users determine whether a specific color + fabric material combination complements their unique complexion, understand the optical reasons why, compare alternatives across fabrics, and validate the winning look with photorealistic **YouCam AI Virtual Try-On**.

### Core Product Promise:
$$\text{Measure} \longrightarrow \text{Drape} \longrightarrow \text{Compare} \longrightarrow \text{Validate} \longrightarrow \text{Save} \longrightarrow \text{Find}$$

*DrapeIt is deliberately focused on pre-purchase pre-wear decision support. It is not an AI chatbot stylist, a social network, a shopping link farm, or a beauty-scoring app.*

---

## 2. The Real Consumer Problem
When shopping online or ordering custom clothing/tailoring, users can see a garment's picture but cannot answer:
1. **Complexion Harmony:** *Does this exact color work with my skin tone and undertone?*
2. **Material Impact:** *Does the fabric weave (sheen of silk vs. matte of cotton vs. texture of linen) alter how the color interacts with my face?*
3. **Comparative Evaluation:** *Which of several candidate fabrics or colors is best for me?*
4. **Explainability:** *Why does one option work significantly better than another?*
5. **Photorealistic Validation:** *What will the actual finished garment look like on my body?*

---

## 3. Target Users
* **Primary:** Online fashion shoppers seeking pre-purchase confidence to eliminate returns and color regret.
* **Secondary:** Custom tailoring, made-to-order, bridal, and formalwear buyers where fabrics cannot be returned once cut.
* **Tertiary:** Fashion-conscious individuals seeking a personalized, explainable color palette and wardrobe memory.

---

## 4. Product Principles
1. **Reduce Uncertainty, Don't Increase Choice:** Guide the user toward their top 3–5 winning matches rather than overwhelming them with thousands of arbitrary RGB swatches.
2. **Explain the Score:** Never present an unexplained percentage. Every score exposes the clear underlying factors (Contrast, Hue Harmony, Chroma).
3. **Local-First & Fast:** The live camera draping loop runs 100% on-device at 60 FPS without waiting on network calls.
4. **Visibly Load-Bearing YouCam Integration:** Perfect Corp's YouCam APIs provide specialized intelligence (asynchronous skin calibration and high-fidelity virtual try-on) as the payoff.
5. **Lighting & Quality as Part of Measurement:** Proactively assess ambient lighting and guide the user to trustworthy conditions.
6. **Editorial Luxury Design:** Hide technical colorimetry complexity behind calm, elegant typography and warm neutral surfaces.

---

## 5. Core Experience & User Journey

### 5.1 Measure
* The app evaluates camera framing, face visibility, ambient illumination, and color temperature.
* A one-time asynchronous call to YouCam Skin Tone API establishes and refines a cached personal color profile.

### 5.2 Drape
* The user selects a fabric material (Silk, Cotton, Linen, Denim, Velvet, Wool) and a color.
* A 100% solid opaque virtual cloth drape dynamically anchors below the tracked chin (MediaPipe Landmark 152) and renders physical weave textures in real time.
* The on-device engine calculates the *Perceptual Color Compatibility* score with instant feedback.

### 5.3 Compare (Hero Feature)
* The user views a 3-way split comparison:
  * **Fabric Comparison:** Same color $\times$ 3 fabrics (e.g. *Burgundy Cotton 89% vs. Burgundy Silk 96% [Winner] vs. Burgundy Linen 91%*).
  * **Color Comparison:** Same fabric $\times$ 3 colors.
* Clear plain-language explanation: *"Silk's specular sheen adds high-contrast definition around your complexion without overpowering the deep burgundy hue."*

### 5.4 Validate
* The winning look is submitted to **YouCam Clothes V3 Virtual Try-On API** to render the complete photorealistic garment on the user's photo.

### 5.5 Save (Proven Looks)
* Saves winning combinations into the user's personal wardrobe memory with the exact score, breakdown, and reasoning.

### 5.6 Find Similar
* Generates a precise product search intent (`"Burgundy Silk Relaxed Shirt"`) with direct retailer search links, replacing generic shopping feeds.

---

## 6. Information Architecture & Navigation

The application uses a unified **4-tab bottom navigation**:

```
        Drape            Explore           Looks          Profile
          🪞                🔍               ✨              👤
```

* **🪞 Tab 1: Drape (Hero Screen):**
  * Top Mode Selector: `[ 🔴 LIVE CAMERA ]` | `[ 📸 PHOTO DRAPE ]` | `[ ⚖️ COMPARE ]`
  * Dynamic Chin-Anchored Opaque Cloth Overlay
  * Live Compatibility Score Badge & Dynamic Face Oval Border
  * "Why It Works" Slide-Up Breakdown
  * Native Camera Dial: Fabric Material vs. Colorway + Custom HSV Picker
  * Primary CTAs: `[ ⚖️ Compare ]` and `[ 📸 AI Try-On (Powered by YouCam) ]`
* **🔍 Tab 2: Explore:**
  * Curated Top 5 Best Colors & Recommended Fabrics
  * Occasion Presets (*Everyday, Office, Evening, Formal*)
* **✨ Tab 3: Looks:**
  * **Proven Looks:** Cards containing photo, fabric, color, match %, and reasons
  * **Try Anything:** Upload any garment/marketplace screenshot (Zara, Amazon, Pinterest) to evaluate and try on
  * **Find Similar:** Direct search query generation
* **👤 Tab 4: Profile:**
  * Personal Color Profile & Undertone Classification
  * YouCam Calibration Status (`✓ Calibrated with Perfect Corp AI`)
  * Re-calibration & Settings

---

## 7. Functional Requirements

### FR-01: Capture Quality & Environmental Lighting Check
* The app shall continuously evaluate framing, exposure, face visibility, and ambient color cast.
* Displays a live confidence indicator (e.g. `●●●●○ Good (94% Confidence)`).
* Displays clear guidance when lighting is suboptimal (e.g. `⚠ Warm indoor lighting detected — face a window for best accuracy`).

### FR-02: Beard & Facial Hair-Resilient Skin Sampling
* The skin sampling engine shall sample **4–6 upper facial landmarks**:
  * Forehead Center / Glabella (Landmarks 10, 151, 9) — *100% hair-free anchor*
  * High Inner Sub-Orbital Cheeks (Landmarks 118, 119 and 347, 348) — *safely above beard/mustache line*
  * Upper Nasal Bridge (Landmarks 6, 197)
* Automatically rejects patches with high texture variance (hair/stubble) or extreme lightness drops ($L^*$ outlier trimming).
* Aggregates valid skin patches using a **trimmed median in CIELAB space**.

### FR-03: Real-Time Drape (60 FPS On-Device)
* The virtual cloth drape shall dynamically anchor to MediaPipe chin landmark 152.
* Rendered with a **100% opaque solid base ($A=1.0$)** to eliminate background or clothing bleed-through.
* Infuses physical weave shaders: specular sheen for Silk/Satin, 45° twill ribs for Denim, horizontal slubs for Linen, directional pile nap for Velvet, and dense matte for Wool/Cotton.

### FR-04: Explainable Compatibility Engine
* Calculates compatibility using perceptual color metrics:
  * **Luminance Contrast ($\Delta L^*$):** Prevents washing out or blending into skin tone.
  * **Chroma Relationship ($\Delta C^*$):** Evaluates saturation harmony.
  * **Hue & Undertone Alignment ($\Delta h$):** Detects warm vs. cool undertone harmony.
* Result bands:
  * **$86\% - 100\%$:** *Strong Compatibility* (Emerald Green)
  * **$70\% - 85\%$:** *Good Compatibility* (Amber Gold)
  * **$48\% - 69\%$:** *Mixed / Contrast Risk* (Orange)
  * **$< 48\%$:** *Weak Compatibility* (Crimson Red)

### FR-05: Score Explainability
* Every result presents a clear natural-language explanation (e.g. `✓ High lightness contrast`, `✓ Warm undertone alignment`, `✓ Silk sheen enhances complexion`).

### FR-06: 3-Up Compare Mode
* Enables side-by-side comparison of 3 candidate fabrics or 3 candidate colors on the user's face with a highlighted "Winner" badge and comparative explanation.

### FR-07: YouCam Skin Tone Calibration (Lane B)
* Calls YouCam Skin Tone / Facial Color Tone API asynchronously to calibrate the user's skin profile and store it in local encrypted cache.

### FR-08: YouCam AI Virtual Try-On (Lane C)
* Submits the chosen look to YouCam Clothes V3 API via Cloudflare Worker proxy to generate a photorealistic garment visualization.

### FR-09: Try Anything
* Enables uploading product images or screenshots from any store/social platform, evaluating their compatibility and invoking YouCam Try-On.

### FR-10: Proven Looks & Find Similar
* Persists winning combinations with complete metadata.
* Provides 1-tap deep-search queries based on exact fabric, color, and fit.

---

## 8. Explicit Non-Goals (Out of Scope)
* ❌ Social feeds, followers, or likes.
* ❌ AI chatbot stylists ("Ask AI anything").
* ❌ Generic outfit generators and generic shopping feeds.
* ❌ Dogmatic "100% Scientifically Proven Beauty Scores".
* ❌ Bloated multi-folder digital wardrobe managers.
* ❌ Lengthy onboarding questionnaires before camera access.

---

## 9. Hackathon Submission Criteria Alignment
* **Technological Implementation (25%):** Non-trivial 3-tier architecture combining on-device 60fps CV (MediaPipe Face Mesh + CIELAB engine) with cloud YouCam Skin and Clothes V3 APIs.
* **Design (25%):** Coherent editorial luxury design system (warm palette, generous spacing, native camera dials, live status HUD).
* **Potential Impact (25%):** Directly solves apparel return rates and color regret in online shopping and custom tailoring.
* **Quality of Idea (25%):** Highly differentiated *Material-Aware* color intelligence replacing generic "seasonal color" apps.
