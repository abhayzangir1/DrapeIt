# Judge Test Guide

DrapeProof is an Android prototype for API 26 or newer. The judge APK is built against the live HTTPS Worker; the private judge access code is supplied out-of-band and is intentionally absent from this repository.

## Install

1. Download the signed judge APK and verify its published SHA-256.
2. On the phone, allow installation from the chosen file browser when Android asks.
3. Install and open **DrapeProof**.
4. Grant camera permission for the real-cloth path. If permission was permanently denied, use the app's **Open Settings** action and return to the app after enabling Camera.

Minimum supported version: Android 8.0 / API 26.

## Five-minute priority path

### 1. Real cloth

- Use a clean front camera, broad indirect daylight, and no beauty/filter mode.
- Hold the phone steady with the face centered and a neutral expression.
- Begin the opening baseline with no cloth in the marked lower zone.
- When prompted, place one **solid, matte, unpatterned** cloth below the face without covering the chin or cheeks.
- Remove it when prompted for the closing baseline.
- Review the evidence tier, cloth–skin separation, feature definition, and the apparent-shift field. Apparent shift is withheld when the controlled-pair gates do not pass.

For a deliberate downgrade check, repeat while changing the light or using a glossy/patterned fabric. The app should explain the failed gate instead of presenting a strong controlled-pair claim.

### 2. Photo contrast

- Use one photo containing both the face and fabric for the stronger photo tier, then tap representative cheek and fabric points.
- The app also accepts separate face and product/dress images, but clearly labels that result as a lower-confidence estimate because the lighting and camera pipelines differ.
- Photo measurement runs locally and does not upload the selected image.

### 3. Product decision

- Save a valid skin sample, open the exact-color catalog, and compare **Soft**, **Balanced**, and **Bold** intent.
- Rankings apply only to the six color variants of the demo SKU. They are contrast choices, not attractiveness or universal “best color” scores.

### 4. YouCam Lab

- Enter the private judge access code and connect to the displayed DrapeProof Worker host.
- Select a front-facing face JPEG for Facial Color Tones.
- Select a front-facing person image plus an apparel/dress reference for Clothes V3.
- Selecting files does not upload them. Read and tick the explicit cloud-consent control, then start the chosen task.
- Keep the app open while it uploads and polls. A task can take tens of seconds depending on the provider.
- Facial colors are supporting palette evidence. The Clothes result is a preview only and never changes the physical contrast score.

Cloud actions consume hackathon units. The screen reports the live feature costs and the protected reserve before submission.

## Privacy and limitations

- Live camera frames are analyzed in memory and are not saved by the capture path.
- Local photo contrast does not upload images.
- A cloud run sends the explicitly selected input(s) to YouCam through the secure Worker flow.
- The API key is never present in the APK.
- Camera colors remain device- and illumination-dependent. DrapeProof is not a colorimeter, medical tool, skin diagnosis, season classifier, or attractiveness score.
- Clear Android app storage or uninstall to remove local records and saved VTO results. Provider/server safety records are separate.

For the complete manual matrix, including failure evidence and a repeated-capture test, use [Device Test Matrix](DEVICE_TEST_MATRIX.md).
