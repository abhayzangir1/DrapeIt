# Device Test Matrix

Target phones:

- OnePlus Nord CE6
- Samsung Galaxy F15

Do not copy assumed specifications from marketing pages into results. Record the actual manufacturer/model/OS/build and camera-control capability reported by each test device.

## Host-side automated gates

| Gate | Command | Last observed result |
|---|---|---|
| Core math/policy tests | `cd android; .\gradlew.bat :core:test --rerun-tasks` | Passed 2026-07-18: 30/30 tests |
| Android build + tests | `cd android; .\gradlew.bat :core:test :app:testDebugUnitTest` | Passed 2026-07-18: 45/45 tests (30 core + 15 app); debug and release assembled against the live Worker |
| Android lint | `cd android; .\gradlew.bat :app:lintDebug :app:lintRelease` | Passed 2026-07-18: 0 fatal/0 errors and 43 non-blocking warnings per variant |
| Worker tests | `cd worker; npm test` | Passed 2026-07-18: 51/51 tests |
| Worker typecheck | `cd worker; npm run typecheck` | Passed 2026-07-18: `tsc --noEmit` |
| Worker package | `cd worker; npx wrangler deploy --dry-run` | Passed 2026-07-18; live Worker also deployed and health-checked |

Android lint warnings are retained in the generated local report and are not represented as zero-warning cleanliness. They cover dependency/update suggestions, orientation/API resource guidance, and style/performance hints; no lint error remains.

## Record the actual devices

With Android platform tools on `PATH`:

```powershell
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint
```

Do not publish serial numbers. Record only the properties needed to reproduce camera behavior.

| Field | OnePlus Nord CE6 | Samsung Galaxy F15 |
|---|---|---|
| Manufacturer/model reported | Not recorded | Not recorded |
| Android release / API | Not recorded | Not recorded |
| Build fingerprint | Not recorded | Not recorded |
| DrapeProof commit/APK hash | Not recorded | Not recorded |
| Front camera ID | Not recorded | Not recorded |
| AE lock available | Not tested | Not tested |
| AWB lock available | Not tested | Not tested |

## Install the signed candidate APK

```powershell
adb install -r ".\outputs\DrapeProof-judge-release-2026-07-18.apk"
```

Capture the APK SHA-256 before the final two-device pass:

```powershell
Get-FileHash ".\outputs\DrapeProof-judge-release-2026-07-18.apk" -Algorithm SHA256
```

Expected SHA-256: `CEDAA97D410C8308EA6F81500E95EBBA0255A8FE5DA6D2FD9E7EA79646CE482E`.

If a debug-signed build of `com.drapeproof.mobile` is already installed, Android will reject the differently signed release update. Remove the old test installation only after preserving any local test records you need, then install the signed candidate fresh.

## Functional matrix

Use `PASS`, `FAIL`, or `BLOCKED`; attach screenshot/video/log references. Every cell is intentionally unclaimed until run.

| Test | OnePlus Nord CE6 | Samsung Galaxy F15 | Required evidence |
|---|---|---|---|
| Clean install and cold launch | NOT RUN | NOT RUN | 15-second screen recording |
| Camera permission allow | NOT RUN | NOT RUN | live preview appears |
| Camera permission deny/retry | NOT RUN | NOT RUN | app remains usable and explains need |
| Portrait layout / system back | NOT RUN | NOT RUN | no clipped controls or dead end |
| Front camera and MediaPipe face found | NOT RUN | NOT RUN | oval turns ready; no repeated analyzer error |
| Screen dims during capture | NOT RUN | NOT RUN | observed during session |
| Previous brightness restored on exit | NOT RUN | NOT RUN | before/after brightness check |
| Opening baseline reaches 18 readings | NOT RUN | NOT RUN | screen recording/result |
| AE/AWB capability truthfully reported | NOT RUN | NOT RUN | result text + log/capability note |
| Solid matte fabric ROI accepted | NOT RUN | NOT RUN | drape phase reaches 18 |
| Glossy/patterned fabric rejected or downgraded | NOT RUN | NOT RUN | failure/downgrade evidence |
| Closing baseline completes | NOT RUN | NOT RUN | result screen |
| Passing controlled pair shows all 3 signals | NOT RUN | NOT RUN | result screenshot with tier |
| Failed gate withholds apparent face shift | NOT RUN | NOT RUN | challenge-run screenshot |
| Retake another fabric | NOT RUN | NOT RUN | second result without relaunch |
| Save skin sample and Drape Record | NOT RUN | NOT RUN | record visible after app restart |
| Same-scene photo sample | NOT RUN | NOT RUN | tier + sampled points/result |
| Separate face/product photos | NOT RUN | NOT RUN | estimate tier + limitation |
| Android share-to-DrapeProof image intent | NOT RUN | NOT RUN | app opens Photo contrast with image |
| Catalog refuses rank without profile | NOT RUN | NOT RUN | missing-profile state |
| Soft/Balanced/Bold exact-SKU ranking | NOT RUN | NOT RUN | three selections; no crash |
| JSON export | NOT RUN | NOT RUN | inspect schema; confirm no image bytes |
| YouCam route reachable from Home | NOT RUN | NOT RUN | YouCam Lab screen |
| Worker degraded/offline state is clear | NOT RUN | NOT RUN | disable network/wrong URL |
| Session creation with judge code | NOT RUN | NOT RUN | session lifetime and health shown |
| Consent required before paid task | NOT RUN | NOT RUN | disabled/enabled run button |
| Facial Color Tones end to end | NOT RUN | NOT RUN | real task ID, result palette, unit delta |
| Clothes V3 source/reference VTO end to end | NOT RUN | NOT RUN | real task ID/result, unit delta |
| Resume saved task polling after relaunch | NOT RUN | NOT RUN | task resumes without duplicate task |
| VTO result saved privately | NOT RUN | NOT RUN | result persists; temporary URL not retained |
| Airplane-mode local capture | NOT RUN | NOT RUN | live/photo/catalog remain usable |
| 10-minute repeated capture / thermal behavior | NOT RUN | NOT RUN | no crash/ANR; note heat/battery |
| Clear app storage removes local data | NOT RUN | NOT RUN | records/results absent afterward |

## Measurement repeatability block

For each device, use the same participant, six coded matte fabrics, fixed position, and stable indirect light:

| Run | Fabric IDs | Repeats | OnePlus status | Samsung status | Data location |
|---|---|---:|---|---|---|
| Controlled pair | F01–F06 | 2 each | NOT RUN | NOT RUN | TBD |
| Auto single-frame control | F01–F06 | 2 each | NOT RUN | NOT RUN | TBD |
| No-change negative control | N01 | 2 | NOT RUN | NOT RUN | TBD |
| Changed-light challenge | C01 | 1 | NOT RUN | NOT RUN | TBD |
| Pose/scale challenge | C02 | 1 | NOT RUN | NOT RUN | TBD |

Analyze according to `EXPERIMENT_PLAN.md`. Do not mark the app “real-life accurate” from visual inspection alone.

## Log capture for a failure

```powershell
adb logcat -c
adb logcat | Select-String -Pattern 'drapeproof|CameraX|MediaPipe|AndroidRuntime'
```

Record the phase, lighting, device, APK hash, and whether the failure reproduces. Avoid uploading logs until checked for file paths, URIs, or other personal data.

## Exit criteria

A device is demo-ready only when:

- the install/launch/camera path has no blocker;
- actual AE/AWB support is documented and the evidence tier matches it;
- one stable controlled scan and one deliberate downgrade are recorded;
- photo, catalog, records, and JSON export work after relaunch;
- YouCam cloud tasks complete through the deployed Worker without exposing the API key;
- exact unit deltas are recorded; and
- all footage used in the submission came from the APK hash listed above.
