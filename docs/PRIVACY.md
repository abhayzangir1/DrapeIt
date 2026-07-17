# Privacy and Data Handling

This document describes the current prototype implementation. It is not a substitute for a production privacy policy, data-processing agreement, YouCam terms review, or jurisdiction-specific legal advice.

## Default local paths

### Live real-cloth capture

- Camera frames are analyzed on-device by CameraX, MediaPipe, and the local Kotlin measurement layer.
- Frames are held only long enough to create ROI readings. The app does not write the live camera frames to disk.
- A saved skin profile contains a sampled skin hex value, evidence tier, source label, and timestamp—not a face image.
- A saved Drape Record contains measurements and limitations—not image bytes.

### Photo contrast

- The user explicitly chooses or shares an image and taps the cheek/fabric sample points.
- Patch sampling and color math run on-device.
- The measurement path does not upload the selected photo.
- If saved, only the sampled colors, tier, metrics, and limitations enter the Drape Record.

### Catalog and records

- Latest skin sample uses app-private `SharedPreferences`.
- Up to 100 lightweight records are stored in `filesDir/drape_records.json` using an atomic staging write.
- JSON leaves the app only when the user taps **Share JSON** and chooses a share destination.
- Android backup is disabled in the main manifest.

## Optional YouCam cloud paths

YouCam Lab is deliberately separate from local measurement.

1. Choosing a face/scarf file is a local action and does not upload.
2. The user must check an explicit cloud-consent box.
3. The user must tap a feature-specific run button.
4. The app requests a short-lived Worker session; the YouCam API key remains a server secret.

### Facial Color Tones

- One JPEG is uploaded directly from the app to a presigned destination returned through the Worker.
- The Worker creates and polls the YouCam Facial Color Tones task.
- The app receives a normalized palette: skin color is required; eye, eyebrow, and lip colors are optional.
- The current UI keeps the palette in memory and persists opaque task/operation IDs only for safe resume and paid-request reconciliation.

### Scarf virtual try-on

- The app sends a face image and scarf reference through signed upload tickets to a private R2 Standard bucket bound as `IMAGE_STORE`.
- Worker metadata makes source/reference objects inaccessible after 86,400 seconds (24 hours). A mandatory R2 lifecycle rule removes untouched objects automatically; Cloudflare may physically remove an expired object during the lifecycle processing window. Upload tickets default to 10 minutes; signed read URLs default to 15 minutes.
- YouCam fetches those short-lived signed URLs to run Scarf VTO.
- On success, the app immediately copies the trusted result image into app-private `filesDir/youcam-results` (bounded to 20 MiB) and stores the local path. The temporary result URL is not persisted.
- The private Worker image store, YouCam processing/retention, and the local VTO result are three distinct storage locations with different controls.

### Clothes V3 fallback

If `VTO_PROVIDER=clothes`, uploads use YouCam-provided tickets rather than DrapeProof's private Scarf image store. The same explicit consent and run action apply.

## Data inventory

| Data | Location | Default lifetime | Leaves device? |
|---|---|---|---|
| Live analysis frames | Memory | One analysis operation | No |
| Latest sampled skin hex/tier/source/time | App-private preferences | Until clear/uninstall/overwrite | No by default |
| Drape Records | App-private JSON | Up to 100 records; until clear/uninstall | Only on explicit Share JSON |
| Selected photo URI/decoded bitmap | Activity/document provider + memory | UI/session dependent | No for local Photo contrast |
| Worker session token | App memory | 30 minutes by default | Sent only to Worker |
| Opaque YouCam task IDs | App-private preferences | Until app storage is cleared or replaced | Yes, between app and Worker |
| Paid operation IDs | App-private preferences + Worker Durable Object | Cleared locally after confirmed admission; indeterminate server records remain reserved until manual reconciliation | Yes, between app and Worker |
| Facial source JPEG | YouCam upload destination | Governed by YouCam service | Yes, only after consent + run |
| Scarf source/reference images | Private Worker R2 bucket | Inaccessible after 24 hours by default; lifecycle deletion follows | Yes, then fetched by YouCam |
| VTO result | App-private file | Until app storage is cleared/uninstalled or overwritten | Downloaded from trusted YouCam result URL |
| YouCam API key | Worker secret | Until rotated/deleted | Only in Worker→YouCam authorization header |
| Request-rate counters | Worker state KV | Window dependent | Server-side only |
| Budget, daily count, request fingerprint, unit reservation | SQLite-backed paid-task Durable Object | Ledger/budget-version dependent; unknown outcomes deliberately retained | Server-side only |

## Security controls implemented

- Main Android build rejects cleartext traffic; only the debug manifest permits cleartext for local emulator development.
- Worker session tokens are HMAC-signed, audience-bound, bounded in length, and expire.
- The mandatory judge access code is compared through a keyed constant-time verification path; missing access/state configuration fails closed.
- Browser CORS uses exact allowed origins; no wildcard is emitted.
- Request JSON is limited to 16 KiB and parsed with strict schemas.
- Images must be JPEG/PNG with matching extension/type and smaller than 10 MiB.
- Private Scarf tickets bind file ID, expiry, content type, and content length into the signature.
- Task/file IDs are constrained opaque values, not arbitrary paths.
- Direct image URL mode requires explicit HTTPS host allowlisting and rejects credentials, nonstandard ports, localhost, IP literals, and `.local` hosts.
- Upstream origin/path and returned media hosts are allowlisted; responses are size-bounded and normalized.
- Responses use no-store/security headers and do not echo the YouCam key.
- Paid actions are rate-limited and guarded by verified feature cost plus a transactional Durable Object admission that binds a UUID-v4 operation ID to the exact request fingerprint, reserve, and daily cap.
- Accepted-operation replay returns the original task ID. Indeterminate outcomes keep their reservation and block replacement instead of automatically retrying a possibly charged task.

## Controls still required before public judging

- Review the current [Perfect Corp. terms](https://www.perfectcorp.com/business/terms-of-service) and account-specific API data handling.
- Deploy production Worker secrets, the state KV namespace, the checked-in SQLite Durable Object migration/binding, and the private R2 bucket; never commit `.dev.vars`.
- Apply `worker/r2-lifecycle.json`, verify the enabled `media/` rule in Cloudflare, and keep the logical media TTL at or below the lifecycle age.
- Rotate any API key that has ever appeared in console output, screenshots, source, or APK resources.
- Use a dedicated `SESSION_SECRET`; do not rely on key-derived signing in production.
- Share a limited judge access code out-of-band and rotate it after judging.
- Confirm logs/observability do not capture image URLs, request bodies, authorization headers, or personal data.
- Add an in-app delete-all control before any broader user study. The current prototype relies on Android **Clear storage** or uninstall for local deletion; this does not erase provider records or the server safety ledger.
- Obtain explicit participant consent for any face photo or demo footage; define who can access it and when it will be deleted.
- Do not use minors' images in testing/demo without an appropriate reviewed process.

## User-facing truth

DrapeProof should say:

- local capture stays on-device;
- selecting a cloud image is not an upload;
- a run action sends the selected inputs to the secure Worker/Perfect Corp.;
- cloud tasks spend units;
- VTO is illustrative and not measurement evidence; and
- clearing app storage/uninstalling currently removes local records/results, but not provider records or server-side paid-operation safety entries.

It should not say “images are never stored” because Scarf inputs have bounded private R2 retention and successful VTO images are saved privately on the phone. It should not promise deletion from YouCam without verified service-specific retention evidence.
