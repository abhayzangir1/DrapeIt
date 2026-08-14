# DrapeIt — Video Demo Script (2:30 Target)

This script is structured for a 2-to-3 minute video demonstration matching the exact codebase and features of **DrapeIt**.

---

## Pre-Recording Checklist
* Install the tested build (`android/app/build/outputs/apk/debug/app-debug.apk`) on a physical Android device.
* Ensure front camera is clean and lighting is clear.
* Have 1 or 2 clothing screenshots / garment photos ready in the phone gallery for the Try-On cropper demo.
* Ensure screen recording is active with clear audio commentary.

---

## Shot List & Narration Walkthrough

### 0:00–0:25 — The Problem
* **Visual:** Show online clothing store product pages with color swatches, highlighting the uncertainty shoppers face before buying.
* **Narration:**
  > "When shopping online for clothing or custom tailoring, seeing a product swatch doesn't tell you how that color and fabric material will actually look against your unique complexion. DrapeIt solves this by combining real-time on-device AR draping with YouCam S2S Cloud AI for photorealistic virtual try-on."

### 0:25–1:00 — Live AR Drape & 14 PBR Material Shaders
* **Visual:** Open **DrapeIt** $\rightarrow$ Navigate to `🪞 Drape`. Point the front camera at your face.
  * Show the MediaPipe FaceMesh automatically tracking the chin (landmark 152) and draping the virtual cloth smoothly across the chest.
  * Show the live perceptual harmony score and CIELAB contrast indicators updating in real time.
  * Switch between different materials in the bottom carousel: **Mulberry Silk** (anisotropic sheen), **Genuine Leather** (Voronoi grain), **Heritage Tweed** (herringbone bouclé), and **Plush Corduroy** (8-wale vertical ridges).
  * Tilt the head slightly side-to-side to show the specular highlights sweeping across the fabric based on head yaw angle.
* **Narration:**
  > "In the Live AR Drape Studio, DrapeIt tracks facial landmarks at 60 frames per second using MediaPipe on-device. An exponential low-pass filter removes tracking jitter so the virtual cloth rests naturally. Instead of flat color drawings, our 4-pass PBR shader renders real physical textiles—like the grain of leather, the weave of tweed, or the anisotropic sheen of silk that shifts dynamically as you move your head."

### 1:00–1:25 — Universal 360° HSV Color Picker
* **Visual:** Tap the `🎨 Color Wheel` icon on the camera bar.
  * Adjust the 360° Hue spectrum slider and Saturation/Value sliders.
  * Enter a direct Hex code (`#831843`). Show the fabric texture staying crisp and tangible over the new color.
* **Narration:**
  > "Users aren't locked into restrictive color palettes. Our Universal Color Wheel gives access to 16.7 million colors with direct hex input, preserving the physical shadows and highlights of the material over any hue."

### 1:25–1:55 — On-Device Garment Cropping & Preparation
* **Visual:** Navigate to `✨ Looks` $\rightarrow$ `Try Anything`.
  * Tap **Upload Screenshot / Photo** and choose an e-commerce garment screenshot.
  * In the **Garment Cropper Modal**, demonstrate 2-finger pinch-to-zoom, panning, and 90° rotation inside the 3:4 crop guide.
  * Tap `✓ Use Garment` — show the prepared thumbnail flattened onto a solid white `#FFFFFF` background.
* **Narration:**
  > "For AI virtual try-on, background clutter and hangers degrade segmentation. DrapeIt includes an on-device interactive cropper that lets users pinch, zoom, and frame the garment. The app automatically composites it onto a clean solid white canvas with 5% padding and normalizes it for YouCam's AI engine."

### 1:55–2:20 — YouCam Cloud AI Virtual Try-On (Clothes V3)
* **Visual:** Tap `📸 AI Try-On`.
  * If testing without a personal selfie, show the prompt modal and tap `👤 Use AI Fit Model` for an instant studio silhouette.
  * Tap `✨ Generate Photorealistic Try-On`.
  * Show the pulsating progress indicator while the task polls YouCam's `/s2s/v2.0/task/cloth-v3` API via Cloudflare Worker proxy.
  * Show the returned high-resolution try-on result fitted onto the model.
* **Narration:**
  > "Next, DrapeIt dispatches the task to Perfect Corp's YouCam Clothes V3 API through our secure stateless Cloudflare Worker proxy. No master API keys are stored on the phone. If a user doesn't have a selfie ready, our instant AI Fit Model allows immediate testing. The generated result realistically drapes the garment onto the body."

### 2:20–2:30 — Photo Compare & Conclusion
* **Visual:** Switch to `[ ⚖️ COMPARE ]` in the Drape tab.
  * Show a 2-up or 3-up side-by-side comparison of saved looks with CIEDE2000 harmony scores and the gold **WINNER** badge.
* **Narration:**
  > "Finally, the Compare Studio lets shoppers evaluate multiple candidate looks side-by-side with objective colorimetry scoring. DrapeIt turns online shopping hesitation into clear, confident decisions."
