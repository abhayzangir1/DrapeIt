# DrapeProof

**Color evidence, not color rules.**

DrapeProof is an Android-first decision tool for a question that product pages do not answer well: *how strongly will this exact color contrast with my face in the light I am actually standing in?* It measures one real, solid-matte fabric beside the user's face in a controlled camera sequence, explains the result as three separate signals, and ranks color variants of one SKU for the user's chosen `Soft`, `Balanced`, or `Bold` contrast intent.

The product deliberately does **not** assign a season, attractiveness score, diagnosis, or universal “best color.” Generated virtual-try-on pixels are kept separate from measurement evidence.

## Why this is more than a color picker

Phone cameras change exposure and white balance when a cloth enters the frame. A single screenshot can therefore measure the camera's reaction instead of the cloth–face relationship. DrapeProof's live protocol:

1. dims the display to reduce screen color spill;
2. collects 18 stable opening-baseline readings;
3. attempts to lock front-camera auto-exposure and auto-white-balance;
4. collects 18 readings with one solid-matte fabric below the face;
5. removes the fabric and collects 18 closing-baseline readings; and
6. withholds the strongest claim if pose, focus, clipping, temporal stability, control, or baseline-drift gates fail.

The result is a **Contrast Vector**:

- **Cloth–skin separation** — CIEDE2000 distance and signed CIELAB lightness difference.
- **Feature definition** — eyes, eyebrows, and lips measured against captured skin, with a baseline change when available.
- **Apparent face shift** — a camera-recorded cheek/under-chin change, shown only for a passing controlled pair. It is never described as an intrinsic skin-color change.

See [Evidence model](docs/EVIDENCE_MODEL.md) for the exact computation and downgrade rules.

## Product paths

- **Real-cloth scan:** on-device CameraX + MediaPipe capture with opening/drape/closing baselines and hard quality gates.
- **Photo contrast:** locally sample a cheek and fabric from one shared scene, or from separate face and product photos with a clearly lower evidence tier.
- **Exact-color catalog:** rank six demo variants of `DP-MATTE-01` only within that SKU. The current catalog uses screen hex specifications and labels that limitation in every saved record.
- **Drape Records:** retain sampled colors, evidence tier, scoring version, and limitations in private app storage; no live-capture face image is saved.
- **YouCam Lab:** explicit opt-in cloud actions for YouCam Facial Color Tones and Scarf virtual try-on, with a provider-aware Clothes V3 fallback. Picking an image does not upload it; a separate consent checkbox and run action are required.

The bundled cobalt scarf reference is an original generated demo asset and is excluded from measurement evidence; its hash and provenance are recorded in [`demo-assets/README.md`](demo-assets/README.md).

## Architecture

```mermaid
flowchart LR
    U["Android user"] --> C["Controlled CameraX capture"]
    C --> M["On-device MediaPipe ROIs"]
    M --> K["Pure Kotlin color and evidence core"]
    P["Local or shared photos"] --> K
    K --> R["Private Drape Records"]
    K --> G["Exact-SKU intent ranking"]
    U -->|"explicit consent and run"| W["Cloudflare Worker"]
    W -->|"server-side bearer key"| Y["YouCam APIs"]
    Y --> F["Facial Color Tones"]
    Y --> V["Scarf or Clothes VTO"]
    F -->|"secondary palette"| U
    V -->|"visualization only"| U
```

- [`android/app`](android/app) contains the Compose UI, controlled capture, local photo workflow, storage, catalog, and opt-in YouCam client.
- [`android/core`](android/core) is a platform-independent Kotlin library for sRGB→XYZ→CIELAB conversion, CIEDE2000, robust statistics, gates, evidence policy, ranking, and record validation.
- [`worker`](worker) is a TypeScript Cloudflare Worker that holds the YouCam API key, issues short-lived sessions, validates uploads/tasks, normalizes upstream responses, keeps private Scarf inputs in R2, and atomically protects paid operations/reserve state in a SQLite-backed Durable Object.

More detail: [Architecture](docs/ARCHITECTURE.md) · [Privacy](docs/PRIVACY.md)

## Build the Android app

Prerequisites:

- Java 17
- Android SDK Platform 36 and Build Tools 36.0.0
- an Android device or emulator with API 26+

From PowerShell:

```powershell
cd android
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat :core:test assembleDebug
```

The debug APK is produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

The development machine also has an ignored portable JDK under `work/toolchain/jdk17`. To use it without hard-coding its versioned folder:

```powershell
cd android
$repoRoot = (Resolve-Path ..).Path
$javaExe = Get-ChildItem -LiteralPath "$repoRoot\work\toolchain\jdk17" -Recurse -Filter java.exe |
  Where-Object { $_.FullName.EndsWith('bin\java.exe') } |
  Select-Object -First 1 -ExpandProperty FullName
$env:JAVA_HOME = Split-Path (Split-Path $javaExe -Parent) -Parent
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :core:test assembleDebug
```

To target a deployed Worker, inject its HTTPS origin at build time:

```powershell
.\gradlew.bat assembleDebug -PDRAPEPROOF_API_BASE_URL=https://your-worker.example.workers.dev
```

For an emulator plus local Wrangler, the debug manifest alone permits cleartext loopback:

```powershell
.\gradlew.bat assembleDebug -PDRAPEPROOF_API_BASE_URL=http://10.0.2.2:8787
```

Release builds remain HTTPS-only. The default `https://api.drapeproof.app` is a placeholder unless that hostname has been deployed.

## Run the secure Worker

Prerequisites: current Node.js/npm and a YouCam API key.

```powershell
cd worker
npm ci
Copy-Item .dev.vars.example .dev.vars
# Put local secrets in .dev.vars; this file is git-ignored.
npm test
npm run typecheck
npm run dev
```

At minimum, set `YOUCAM_API_KEY`, an independent `SESSION_SECRET`, and `JUDGE_ACCESS_CODE`. Deployment requires the checked-in SQLite-backed `PAID_TASK_LEDGER` Durable Object binding/migration and a Cloudflare KV binding named `DRAPEPROOF_STATE`. The default Scarf provider also requires a private R2 Standard bucket bound as `IMAGE_STORE`; apply and verify [`worker/r2-lifecycle.json`](worker/r2-lifecycle.json) before deployment. See [`worker/.dev.vars.example`](worker/.dev.vars.example), [`worker/wrangler.jsonc`](worker/wrangler.jsonc), and [`worker/README.md`](worker/README.md).

The zero-spend configuration is designed for the hackathon's 1,000 promotional units and current Cloudflare free-plan allowances. The Worker starts from a configured 1,000-unit baseline, transactionally protects a 300-unit floor, binds each paid request to a persistent operation ID, queries the account's feature-cost endpoint before admission, and fails closed when cost or persistent state cannot be verified. Set the baseline from the real dashboard immediately before deployment; outside YouCam usage is not automatically synchronized.

## Verification status

This table is intentionally stricter than “the code exists.” It should be updated only from observed commands or device/API evidence.

| Gate | Current evidence |
|---|---|
| Pure Kotlin core tests | **Passed locally on 2026-07-17:** `:core:test --rerun-tasks`; 30 test methods present |
| Android build + local tests | **Passed locally on 2026-07-17:** `:core:test :app:testDebugUnitTest :app:assembleDebug`; 39/39 tests (30 core + 9 capture-derivation) |
| Android lint | **Passed locally on 2026-07-17:** 0 errors, 42 non-blocking warnings, 1 hint |
| Debug APK | **Produced from final source:** 70,864,839 bytes, SHA-256 `964B18608461F967FC0821CB94357E2DBDF07EAAEFC71B526969C03DAEF559C1`; physical install still pending |
| Worker unit/integration tests | **Passed locally on 2026-07-17:** 50/50 tests in 3 files, including R2 and atomic paid-operation recovery |
| Worker TypeScript typecheck | **Passed locally on 2026-07-17:** `tsc --noEmit` |
| Worker package dry run | **Passed locally on 2026-07-17:** Wrangler recognized the Durable Object binding and produced a 98.30 KiB gzip bundle; no deployment/API call |
| OnePlus Nord CE6 live capture | **Not yet device-validated** |
| Samsung Galaxy F15 live capture | **Not yet device-validated** |
| Deployed Worker health/session | **Not deployed or live-verified** |
| Real YouCam Facial Color Tones task | **Not run; requires API key and spends units** |
| Real YouCam Scarf VTO task | **Not run; requires API key, KV/R2/Durable Object deployment, and units** |

Use the [device test matrix](docs/DEVICE_TEST_MATRIX.md) and [submission checklist](docs/SUBMISSION_CHECKLIST.md) before making a “working end-to-end” claim.

## Hackathon fit and deadline

DrapeProof targets the **Skin AI + Apparel VTO** topic: YouCam Facial Color Tones supplies a secondary facial palette, while Scarf VTO visualizes the selected physical reference. The app's original measurement layer turns those services into a purchasing workflow rather than a one-call wrapper.

The official submission deadline is **August 17, 2026 at 11:45 a.m. EDT** (**9:15 p.m. IST**). The [Devpost overview](https://youcam-api.devpost.com/) requires a working web/mobile prototype using at least one YouCam API, repository and testing instructions, screenshots, a text description, and a public 1–3 minute demo video. The [official rules](https://youcam-api.devpost.com/rules) control if anything differs here.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Evidence model](docs/EVIDENCE_MODEL.md)
- [Experiment plan](docs/EXPERIMENT_PLAN.md)
- [Device test matrix](docs/DEVICE_TEST_MATRIX.md)
- [Privacy and data handling](docs/PRIVACY.md)
- [2:45 demo script](docs/DEMO_SCRIPT.md)
- [Submission checklist](docs/SUBMISSION_CHECKLIST.md)
- [Third-party notices and model hash](THIRD_PARTY_NOTICES.md)

## Scope and claims

DrapeProof is a shopping decision aid, not a scientific colorimeter, medical device, skin diagnostic, or statement about attractiveness. Camera-derived colors remain device- and illumination-dependent. Current MVP inputs are solid, matte fabrics; texture, gloss, translucency, patterns, metamerism, display calibration, and store-light equivalence are outside the validated scope.

Licensed under the [MIT License](LICENSE).
