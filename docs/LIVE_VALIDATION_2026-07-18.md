# Live Validation — 2026-07-18

This report separates observed live evidence from the one remaining device gate. It contains no API key, session secret, judge access code, private image, or signing password.

## Outcome

- Cloudflare Worker is live at `https://drapeproof-api.drapeproof-abhay.workers.dev`.
- Deployed Worker version: `6cafc67d-f1c3-4bab-9bc1-90ca3d0d3fab`.
- YouCam Facial Color Tones completed successfully through the Worker.
- YouCam Clothes V3 completed successfully through the Worker.
- The current Android release was built against the live Worker and signed as an APK v2/v3 package.
- APK-tested source commit: `7acc7b9` (`Complete live DrapeProof YouCam deployment`).
- Automated Android and Worker gates pass.
- A physical install/camera run on the OnePlus Nord CE6 or Samsung Galaxy F15 is **not yet observed**. The project must not be described as fully device-validated until that run is recorded.

## Live Worker evidence

Observed against the deployed URL:

| Check | Result |
|---|---|
| `/healthz` | `ok`; Clothes provider, KV state, paid ledger, access gate, and upstream key reported configured |
| Browser request from a disallowed origin | Rejected with `403` |
| Credits without a bearer session | Rejected with `401` |
| Response caching | `Cache-Control: no-store` |
| Framing/security | CSP denies all/default framing; `X-Frame-Options: DENY` |
| Request tracing | `X-Request-Id` present |
| Feature-cost lookup | Facial Color `20` units; Clothes V3 `2` units |

The YouCam bearer key, session signing secret, and judge access code are encrypted Worker secrets and are not present in the Android package or repository.

## Live YouCam evidence

### Facial Color Tones

The first input was rejected by YouCam as `error_face_angle_downward`; its 20-unit reservation was refunded by YouCam. A cropped, front-facing image then succeeded at `high` strictness and returned the normalized palette:

- skin: `#bb9981`
- eyes: `#463124` (`Brown`)
- eyebrow: `#5b3f32`
- lips: `#ad6967`

The upstream hair label was visibly inconsistent with the dark-haired input. DrapeProof therefore treats the YouCam palette as supporting information only; its core contrast calculation uses locally sampled skin, eyes, eyebrows, and lips and does not use the VTO output or the YouCam hair label.

### Clothes V3

A source image and apparel reference were uploaded with YouCam-provided tickets. The Clothes V3 task succeeded and returned a trusted result that downloaded as `73,415` bytes. The result host passed the Worker allowlist, and the downloaded SHA-256 began `A8A29C68718406E4`.

This result is visualization evidence only. Generated pixels do not enter physical contrast measurement, ranking, or a Drape Record.

## Unit reconciliation

| Event | Delta | Observed balance |
|---|---:|---:|
| Hackathon code redeemed | +1,000 | 1,000 |
| API key free trial grant | +40 | 1,040 |
| Rejected Facial task | -20 | 1,020 |
| Rejected Facial task refund | +20 | 1,040 |
| Successful Clothes V3 task | -2 | 1,038 |
| Successful Facial task | -20 | 1,018 |

The deployed ledger baseline is `1,018` units. It protects a `300`-unit floor, so `718` units were available to new admitted operations immediately after reconciliation.

## Automated and package evidence

| Gate | Result |
|---|---|
| Pure Kotlin core | 30/30 tests passed |
| Android app + core | 45/45 tests passed |
| Android lint | Debug and release: 0 fatal, 0 errors, 43 non-blocking warnings each |
| Worker | 51/51 tests passed |
| Worker typecheck | `tsc --noEmit` passed |
| Wrangler package | Deployment dry-run passed |
| Signed APK | 64,017,523 bytes; SHA-256 `CEDAA97D410C8308EA6F81500E95EBBA0255A8FE5DA6D2FD9E7EA79646CE482E` |
| APK signature | RSA-4096; APK Signature Scheme v2 and v3 verified |

Two production-relevant regressions are covered by tests: global `fetch` is invoked with the correct Cloudflare runtime receiver, and session tokens must use their exact canonical base64url encoding.

## Remaining evidence gate

Connect either target phone with USB debugging enabled and accept the computer's RSA prompt. Then complete the priority path in [Judge Test Guide](JUDGE_TEST_GUIDE.md) and record the results in [Device Test Matrix](DEVICE_TEST_MATRIX.md). Required proof is:

1. clean install and cold launch of the signed APK;
2. camera permission allow and permanent-denial recovery;
3. one passing opening/drape/closing scan with a solid matte cloth;
4. one deliberate downgrade or withheld apparent-shift result;
5. photo contrast and exact-SKU ranking;
6. consent-gated Facial Color and Clothes V3 calls;
7. relaunch/resume behavior and crash log check.

Until this is done, the accurate status is: **cloud/API/release verified; physical Android camera path pending**.
