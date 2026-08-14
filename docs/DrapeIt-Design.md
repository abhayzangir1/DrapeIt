# DrapeIt — UI/UX & Design System

**Version:** 1.0  
**Date:** 2026-08-14  
**Status:** Editorial Luxury Design Guidelines  

---

## 1. Design Direction & Brand Personality

### Visual Language:
* **Editorial Fashion Lookbook:** Restrained luxury, generous whitespace, warm neutral surfaces, large tactile imagery, and minimal chrome.
* **Tone:** Calm, precise, tactile, premium, and confident.
* **Forbidden Tropes:** No cyberpunk neon, no AI chatbot bubbles, no dashboard clutter, no unexplained percentage badges.

---

## 2. Color Palette & Design Tokens

```
┌─────────────────┬──────────────┬───────────────────────────────────────────┐
│ Token Name      │ Hex Value    │ Role / Usage                              │
├─────────────────┼──────────────┼───────────────────────────────────────────┤
│ Ink             │ #181512      │ Primary text, headings, prominent borders │
│ Warm Black      │ #26211C      │ Camera controls sheet, deep surfaces      │
│ Cream           │ #F7F2EA      │ Primary background canvas (light theme)   │
│ Sand            │ #E8DED0      │ Secondary container cards, pill tags      │
│ Stone           │ #C9BBAA      │ Subtle outlines, dividers, inactive states│
│ Muted           │ #8B7E70      │ Secondary body copy, caption text         │
│ Sienna Accent   │ #8F5945      │ Primary luxury CTA, active dial dot       │
│ Positive Match  │ #3F765A      │ Flattering score state (>= 86%)           │
│ Warning Match   │ #B07C31      │ Good/Balanced score state (70% - 85%)     │
│ Risk Match      │ #9B554A      │ Clash / Washout score state (< 70%)       │
└─────────────────┴──────────────┴───────────────────────────────────────────┘
```

---

## 3. Typography Hierarchy

* **Display (Editorial Titles):** `32sp - 40sp`, Serif / Luxury Sans, SemiBold, Tight Tracking (`-0.5sp`)
* **Screen Title:** `24sp - 28sp`, Bold, Neutral Tracking
* **Section Header:** `18sp - 20sp`, SemiBold
* **Body / Reasons:** `14sp - 16sp`, Regular, Line-Height `22sp`
* **Score Callout:** `32sp - 40sp`, ExtraBold, Numeric Alignment
* **Micro-Labels & Meta:** `11sp - 12sp`, SemiBold, All-Caps Tracking (`+1.0sp`)

---

## 4. Surfaces, Radii & Component Geometry

* **Large Modal / Sheets:** `28dp - 32dp` top rounded corners
* **Product & Comparison Cards:** `20dp - 24dp` rounded corners
* **Input Buttons & Chips:** `14dp - 16dp` rounded corners
* **Interactive Pills & Badges:** `999dp` fully rounded stadium shape
* **Button Heights:** Standard `52dp`, Compact `40dp`
* **Minimum Touch Target:** `48dp × 48dp`

---

## 5. Screen Specifications

### 5.1 Tab 1: Drape (Hero Experience)
* **Top Bar:** Mode Selector `[ 🔴 LIVE ]  [ 📸 PHOTO ]  [ ⚖️ COMPARE ]` + Lighting Quality Pill (`●●●●○ Good`).
* **Viewport:** Centered face reticle with dynamic color-coded gradient border.
* **Drape Area:** 100% solid opaque cloth anchored right below chin (MediaPipe #152) with physical fabric shaders.
* **Status Badge (Under Oval):** `94% • Flattering Harmony` with expandable "Why It Works" bullet list.
* **Bottom Controls Sheet:** Borderless horizontal camera dial (`[ FABRIC MATERIAL ]` vs. `[ COLORWAY ]`), HSV custom slider, and `[ ⚖️ Compare ]` / `[ 📸 AI Try-On ]` CTAs.

### 5.2 Compare Screen (3-Up Hero)
* **3-Card Split Grid:** Displays 3 candidate fabrics for the selected color (e.g. *Cotton 89% vs. Silk 96% vs. Linen 91%*).
* **Winner Highlight:** Gold luxury border + `"Best Match for You"` label on the winning card.
* **Comparative Insight:** Natural language explanation of why the winner's luster/contrast flatters the complexion.

### 5.3 Tab 2: Explore
* **Top Picks:** Curated Top 5 Best Colors with exact match percentages.
* **Material Guide:** Recommended fabrics for your undertone.
* **Occasion Filters:** *Everyday, Office, Evening, Formal*.

### 5.4 Tab 3: Looks
* **Proven Looks Feed:** Clean vertical lookbook of saved combinations showing photo, fabric, color, score, and reasons.
* **Try Anything:** Prominent upload button to drop any product screenshot (Zara, Pinterest, Amazon).
* **Find Similar:** 1-tap contextual product search.

### 5.5 Tab 4: Profile
* Personal skin profile ($L^*a^*b^*$ and Monk/Fitzpatrick classification).
* YouCam calibration status banner.
* Recalibrate button and lighting test utility.
