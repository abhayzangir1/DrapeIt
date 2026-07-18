# DrapeProof API Worker

Cloudflare Worker that keeps the YouCam API key out of the Android APK. It issues short-lived signed sessions, creates private upload tickets, starts Facial Color Tones and virtual try-on tasks, polls task status, and atomically protects the configured unit reserve through a SQLite-backed Durable Object.

Facial Color and Clothes V3 uploads go directly to YouCam presigned destinations. Scarf source/reference images use a private R2 Standard bucket bound as `IMAGE_STORE` because the official Scarf task accepts URLs rather than YouCam file IDs. R2 provides globally strong read-after-write consistency, so YouCam can retrieve an image immediately from another PoP. Each private upload and read is protected by a short-lived, purpose-bound HMAC token; image IDs alone reveal nothing.

## Providers

- `clothes` (**checked-in and deployed**): AI Clothes V3 selected with `VTO_PROVIDER=clothes`; both inputs use YouCam-provided upload tickets.
- `scarf` (optional code path): official `/s2s/v2.0/task/scarf` endpoint with custom source/reference URLs, `female|male` gender, and a fixed named style. It requires private R2 storage and is not provisioned in the validated hackathon deployment.

There is no automatic cross-provider retry. The defensive code fallback for an unset/unknown provider is Scarf; without `IMAGE_STORE`, health is degraded and its private upload/task routes return `503`.

## Local setup

```powershell
cd worker
npm install
Copy-Item .dev.vars.example .dev.vars
# Edit .dev.vars locally. It is ignored by Git.
npm test
npm run typecheck
npm run dev
```

Required secrets:

```text
YOUCAM_API_KEY=<dashboard key>
JUDGE_ACCESS_CODE=<private judge code, at least 8 characters>
```

Recommended secrets/configuration:

```text
SESSION_SECRET=<independent long random signing secret>
ALLOWED_ORIGINS=https://your-judge-site.pages.dev
```

Native Android calls normally have no `Origin` header and are accepted. Browser requests are accepted only when their exact origin is in `ALLOWED_ORIGINS`; wildcard CORS is never used.

## Production deployment

1. Create state KV: `npx wrangler kv namespace create DRAPEPROOF_STATE`.
2. Keep the checked-in `PAID_TASK_LEDGER` Durable Object binding and `new_sqlite_classes` migration.
3. Select `VTO_PROVIDER=clothes`. Clothes requires no DrapeProof R2 bucket because its inputs use YouCam upload destinations.
4. Add secrets with `npx wrangler secret put YOUCAM_API_KEY`, `JUDGE_ACCESS_CODE`, and `SESSION_SECRET`.
5. Set `UNIT_BUDGET` to the dashboard units currently available and use a fresh `UNIT_BUDGET_ID` for that baseline.
6. Configure `ALLOWED_ORIGINS` and limits, then run `npm test`, `npm run typecheck`, and `npm run deploy`. Wrangler creates/migrates the SQLite-backed Durable Object namespace.

To use the optional Scarf adapter instead, create a private Standard bucket, bind it as `IMAGE_STORE`, apply `r2-lifecycle.json` with `npm run r2:lifecycle`, and verify the enabled `expire-drapeproof-media-24h` rule before switching the provider.

Do not deploy Scarf without lifecycle verification. The Worker stores an `expiresAt` timestamp, refuses every read after exactly 24 hours, and opportunistically deletes an expired object when accessed. The mandatory R2 lifecycle rule deletes untouched objects automatically. Cloudflare notes that physical lifecycle removal can occur within 24 hours after the expiration time, so the privacy guarantee is **inaccessible after 24 hours**, with background deletion following the bucket lifecycle schedule.

Use the R2 **Standard** storage class. Cloudflare's current free tier includes 10 GB-month of Standard storage, one million Class A operations, ten million Class B operations, and free egress; Infrequent Access is not eligible for that free tier. See [R2 pricing](https://developers.cloudflare.com/r2/pricing/) and [R2 consistency](https://developers.cloudflare.com/r2/reference/consistency/).

`PaidTaskLedger` uses the SQLite storage backend required on the Workers Free plan. The demo is far below the published free daily row limits, but verify the current account plan and limits before deployment. See [Durable Objects pricing](https://developers.cloudflare.com/durable-objects/platform/pricing/).

`DRAPEPROOF_STATE` and `PAID_TASK_LEDGER` are mandatory: without either, the Worker reports degraded health and paid paths fail closed. Session issuance also refuses missing state KV. The Worker fails closed when `JUDGE_ACCESS_CODE` is missing, too short, malformed, or incorrect. KV request-rate counters remain defense-in-depth; paid admission, daily count, operation ID, and reserve accounting are serialized in one Durable Object selected by `UNIT_BUDGET_ID`. The Worker queries the official paginated feature-cost API and matches `run_task_url` before each new paid operation. The legacy balance endpoint is not a required gate because it uses a different legacy access-token contract.

## API

All JSON responses use `Cache-Control: no-store` and include `X-Request-Id`. Except for signed media transfer, `/healthz`, and `/v1/session`, routes require `Authorization: Bearer <session token>`.

### Create a session

```http
POST /v1/session
Content-Type: application/json

{"accessCode":"judge-code"}
```

`accessCode` is always required and must match the configured secret. There is no ungated mode. Tokens expire after 30 minutes by default and cannot exceed one hour.

### Request upload tickets

```http
POST /v1/uploads
Authorization: Bearer <session>
Content-Type: application/json

{
  "feature": "try-on",
  "files": [
    {"contentType":"image/jpeg","fileName":"person.jpg","fileSize":547541},
    {"contentType":"image/png","fileName":"apparel.png","fileSize":248121}
  ]
}
```

For Facial Color Tones, use `feature: "facial-colors"` with exactly one JPEG. The response contains opaque `fileId` values and signed `PUT` requests. Upload each exact file using every returned header.

With Scarf, the upload token is bound to the exact ID, MIME type, byte length, purpose, and expiry. Bytes are written to private R2, become immediately globally readable through the Worker binding, and become inaccessible after 24 hours. With Clothes or Facial Color, the returned destination is YouCam's presigned upload URL.

### Start Facial Color Tones

```http
POST /v1/tasks/facial-colors
Authorization: Bearer <session>
Content-Type: application/json

{"operationId":"<uuid-v4>","sourceFileId":"<uploaded-file-id>","faceAngleStrictness":"strict"}
```

### Start Clothes V3 try-on

```http
POST /v1/tasks/try-on
Authorization: Bearer <session>
Content-Type: application/json

{
  "operationId": "<uuid-v4>",
  "sourceFileId": "<person-file-id>",
  "referenceFileId": "<apparel-file-id>",
  "garmentCategory": "auto"
}
```

The deployed request accepts `sourceFileId` plus `referenceFileId` or a provider `templateId`. Supported garment categories are `auto`, `upper_body`, `lower_body`, `full_body`, and `shoes`.

### Optional Scarf try-on

When `VTO_PROVIDER=scarf`, the task request additionally uses `gender` and a deterministic `style`. Gender is required unless `SCARF_DEFAULT_GENDER` is securely configured. Allowed styles are `style_french_elegance`, `style_light_luxury`, `style_cottagecore`, `style_modern_chic`, and `style_bohemian`; `random` is rejected.

The Worker resolves Scarf's two private IDs into 15-minute HMAC-signed `/media/...` URLs visible only to YouCam.

For controlled demos only, the route can instead accept `sourceImageUrl` plus `referenceImageUrl`; both must use public HTTPS hosts explicitly listed in `SCARF_ALLOWED_IMAGE_HOSTS`. Localhost, IP literals, credentials, nonstandard ports, and unlisted hosts are rejected.

Every paid create request requires a client-generated UUID-v4 `operationId`. It is transactionally bound to the exact feature and request fingerprint. Replaying an accepted operation returns the original task ID without calling YouCam again. Reusing it with different inputs is rejected.

### Reconcile a paid operation

```http
GET /v1/operations/<operation-id>
Authorization: Bearer <session>
```

The read-only result is `accepted`, `operation_pending`, `unknown_reconcile`, or `rejected`. If the upstream outcome is indeterminate, DrapeProof marks it `UNKNOWN_RECONCILE`, keeps its units reserved, and refuses replacement admission for that operation. There is intentionally no public auto-release endpoint: compare the operation and YouCam usage records manually before any administrative budget reset.

### Poll

```http
GET /v1/tasks/facial-colors/<task-id>
GET /v1/tasks/try-on/<task-id>
Authorization: Bearer <session>
```

The Worker returns only normalized states:

- `running` with `retryAfterSeconds`;
- `success` with whitelisted facial colors or a trusted HTTPS result URL;
- `error` with a bounded code/message.

Poll every two seconds until `success` or `error`, with a bounded retry policy. YouCam requires polling within its retention window.

### Budget and feature costs

```http
GET /v1/credits
Authorization: Bearer <session>
```

Returns the configured budget baseline, atomically reserved units, estimated remaining units, protected floor, active provider, and live feature costs. If the official feature-cost pages cannot be safely matched to the exact task path, paid task creation fails closed with `feature_cost_unavailable`.

## Security properties

- The YouCam bearer token exists only in Worker secrets and outbound upstream headers.
- Health is ready only when the API key, judge access gate, state KV, paid-task Durable Object, and selected VTO storage are all configured.
- Missing judge access, state KV, or paid ledger fails closed; there is no public/default or local ungated session mode.
- Request bodies and authorization values are never logged by application code.
- JSON bodies are limited to 16 KiB; strict schemas reject unknown fields.
- File IDs and task IDs are opaque constrained identifiers.
- Scarf bytes require a short-lived signed `PUT`, live in private R2 under an unguessable ID, and require another short-lived signed `GET`.
- Object metadata enforces an exact 24-hour logical expiry; expired reads delete immediately, and the mandatory `r2-lifecycle.json` rule removes untouched `media/` objects automatically.
- Direct Scarf URLs require an explicit HTTPS hostname allowlist and SSRF-oriented checks.
- YouCam upload/result URLs must be HTTPS and hosted by approved Perfect/Makeupar/AWS domains.
- Upstream responses are bounded and normalized; raw headers and debug bodies are not forwarded.
- Feature costs are paginated and matched by exact task path before every new paid operation. Durable Object transactions atomically enforce fingerprint-bound idempotency, reserve admission, and the daily cap.
- Known provider rejection releases its reservation exactly once. Timeouts/5xx and commit uncertainty retain the reservation as `UNKNOWN_RECONCILE` so an ambiguous task cannot be charged twice through retry.

## Configuration reference

| Variable | Checked deployment | Purpose |
|---|---:|---|
| `CREDIT_FLOOR` | `300` | Minimum units protected from task creation |
| `JUDGE_ACCESS_CODE` | required | Private session gate, minimum 8 characters |
| `UNIT_BUDGET` | `1018` | Reconciled baseline; reset only from the dashboard balance |
| `UNIT_BUDGET_ID` | `hackathon-2026-07-18-1018-reconciled` | Versioned ledger key for this baseline |
| `VTO_PROVIDER` | `clothes` | Deployed Clothes path; Scarf is optional and requires R2 |
| `SCARF_DEFAULT_GENDER` | unset | Optional `female` or `male` default |
| `SCARF_ALLOWED_IMAGE_HOSTS` | unset | Exact/wildcard hosts for controlled direct-URL demos |
| `MEDIA_TTL_SECONDS` | `86400` | Logical image accessibility, maximum 24 hours |
| `MEDIA_READ_TTL_SECONDS` | `900` | YouCam signed-read lifetime |
| `UPLOAD_TICKET_TTL_SECONDS` | `600` | Private upload-ticket lifetime |
| `SESSION_TTL_SECONDS` | `1800` | Session lifetime, clamped to 300-3600 seconds |
| `RATE_LIMIT_SESSION` | `5/min` | Session attempts per client |
| `RATE_LIMIT_UPLOAD` | `20/min` | Upload-ticket requests per client |
| `RATE_LIMIT_TASK` | `10/min` | Task attempts per client |
| `RATE_LIMIT_POLL` | `60/min` | Poll/budget requests per client |
| `MAX_TASKS_PER_DAY` | `40` | Atomic UTC-day paid-operation cap in the ledger |

Before the public demo, set `UNIT_BUDGET` to the dashboard units remaining and use a fresh `UNIT_BUDGET_ID`. Per-feature costs are fetched from YouCam and are never hardcoded.
