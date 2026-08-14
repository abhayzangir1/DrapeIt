# DrapeIt — Judge Testing Guide

**Target:** Android (API 26+)  
**App Name:** DrapeIt  
**APK Location:** `android/app/build/outputs/apk/debug/app-debug.apk`

---

## 1. Quick Installation

1. Install the debug APK directly via ADB:
   ```powershell
   & "C:\Users\abhay\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "android/app/build/outputs/apk/debug/app-debug.apk"
   ```
2. Open **DrapeIt** on your device.
3. Grant camera permission when requested to enable on-device FaceMesh tracking and AR draping.

---

## 2. 5-Minute Evaluator Walkthrough

### Step 1: Live AR Drape & PBR Material Shaders (`🪞 Drape Tab`)
* **Live AR Face Tracking:** Point the front camera at your face. The app automatically tracks your chin (MediaPipe Landmark 152) and drapes a virtual cloth polygon across your chest.
* **14 Physical Material Shaders:** Tap the fabric carousel at the bottom and switch between materials:
  * Observe the anisotropic specular sheen on **Silk** and **Satin**.
  * Observe the Voronoi tactile grain on **Leather**.
  * Observe the herringbone bouclé crosshatch on **Tweed**.
  * Observe the 8-wale vertical ridges on **Corduroy**.
  * Observe the sheer pass-through on **Chiffon**.
* **Head Motion Sheen:** Move your head slightly side-to-side; notice how the specular highlights sweep across the fabric surface dynamically based on your head yaw angle.

### Step 2: Universal 360° HSV Color Picker
* Tap the `🎨 Color Wheel` icon on the camera control bar.
* Adjust the 360° Hue spectrum slider and Saturation/Value sliders, or enter a direct `#RRGGBB` hex code (e.g. `#831843` or `#1E3A8A`).
* Notice how the physical fabric weave remains crisp and tactile over any selected color without being crushed by flat color overlays.

### Step 3: Interactive Garment Cropper & Normalizer (`✨ Looks Tab -> Try Anything`)
* Go to the **Looks** tab $\rightarrow$ select the **Try Anything** sub-tab.
* Tap **Upload Screenshot / Photo** and choose an apparel product photo from your gallery.
* In the **Garment Cropper Modal**, use 2-finger gestures to pinch-to-zoom, pan, or rotate the garment within the 3:4 portrait guide.
* Tap `✓ Use Garment` — the app crops the piece, automatically flattens it onto a clean `#FFFFFF` solid background with 5% padding, and normalizes it to max 1280px for the YouCam Clothes V3 AI engine.

### Step 4: AI Virtual Try-On (`📸 Try-On Studio`)
* From the Drape or Looks screen, tap `📸 AI Try-On`.
* If you haven't uploaded a selfie yet, the app prompts you with options:
  * Upload a portrait photo, or
  * Tap **`👤 Use AI Fit Model`** for an instant studio silhouette.
* Tap `✨ Generate Photorealistic Try-On` — the app dispatches the task to the YouCam Clothes V3 Cloud API via the secure Cloudflare Worker proxy, polls the task status with a pulsating progress indicator, and displays the high-fidelity fitted outfit.

### Step 5: Side-by-Side Photo Compare (`⚖️ Compare Tab`)
* From the top mode selector in the Drape tab, switch to `[ ⚖️ COMPARE ]`.
* Select 2 to 4 of your captured looks.
* Inspect the side-by-side collage showing CIEDE2000 harmony percentiles, fabric details, and gold **✨ WINNER** badges.

---

## 3. Privacy & Security Assertions

* **Zero Master Keys in APK:** The Android application never bundles third-party API credentials; all cloud calls use short-lived session tokens from the backend proxy.
* **On-Device Confidentiality:** Camera video streams and facial landmark coordinates remain strictly on-device in memory. Network calls only occur when the user explicitly triggers an AI Try-On or Skin Tone calibration task.
