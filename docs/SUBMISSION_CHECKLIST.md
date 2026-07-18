# Submission Checklist

The source of truth is the [Devpost overview](https://youcam-api.devpost.com/) and [official rules](https://youcam-api.devpost.com/rules). Re-check both before submitting because the rules can change.

## Fixed dates

- Registration: July 1, 2026 12:00 p.m. ET – August 17, 2026 11:45 a.m. ET.
- Submission: July 6, 2026 12:00 p.m. ET – **August 17, 2026 11:45 a.m. EDT**.
- India equivalent deadline: **August 17, 2026 9:15 p.m. IST**.
- Judging: August 18, 2026 12:00 p.m. ET – August 31, 2026 11:45 a.m. ET.
- Winners: on or around September 4, 2026 3:00 p.m. ET.

Submit early enough to recover from upload, video-processing, or Devpost problems. The official time, not this conversion, controls.

## Eligibility and ownership — entrant must confirm

- [ ] Entrant is of legal majority and not resident/domiciled in an excluded territory.
- [ ] Team/organization has named an authorized representative if applicable.
- [ ] Project is original, owned by the entrant/team, and does not infringe copyright, trademark, patent, privacy, publicity, contract, or other rights.
- [ ] Every third-party SDK/API/data/asset is authorized and license obligations are met.
- [ ] Project was created during the submission period, or any pre-existing project was significantly updated and the update is explained.
- [ ] No disqualifying sponsor/administrator financial or preferential support applies.
- [ ] Entrant accepts YouCam/Perfect Corp. and Devpost terms and can participate in a winner exit interview/blog if selected.

## Product gates

- [x] Android project, pure Kotlin core, Worker source, and MIT license exist in the repository.
- [x] Local capture implements opening/drape/closing baselines and evidence downgrade logic.
- [x] Photo, exact-SKU catalog, local records, and opt-in YouCam UI exist in source.
- [x] YouCam API key is server-side only in design/source.
- [x] Core test suite passed locally on 2026-07-18 with `--rerun-tasks`.
- [x] Android core/app tests and forced debug/release assembly passed locally on 2026-07-18; 45/45 tests passed and both APK variants embed the deployed Worker URL.
- [x] Android debug/release lint passed locally on 2026-07-18 with 0 fatal/0 errors and 43 non-blocking warnings per variant.
- [x] Worker tests (51/51), typecheck, and Wrangler package dry-run with the Durable Object and KV bindings passed locally on 2026-07-18.
- [ ] OnePlus Nord CE6 device matrix complete.
- [ ] Samsung Galaxy F15 device matrix complete.
- [x] Deployed HTTPS Worker health/session/credit routes verified at `https://drapeproof-api.drapeproof-abhay.workers.dev`.
- [x] Real Facial Color Tones task succeeded at `high` strictness; exact 20-unit delta and one refunded rejected input are recorded.
- [x] Configured Clothes V3 task succeeded; exact 2-unit delta and trusted result download hash are recorded.
- [ ] A deliberate bad capture downgrades/withholds instead of emitting a controlled-pair claim.
- [ ] No crash/ANR, secret, private image, or fake result blocker remains.

Do not mark the prototype “functional end to end” until every unchecked gate required by the filmed path is complete.

## Zero-spend deployment

- [ ] Confirm the promotional-unit expiry date; live reconciled balance is 1,018 as of 2026-07-18.
- [x] Create the YouCam API key and store it only as an encrypted Worker secret.
- [x] Create and bind Cloudflare `DRAPEPROOF_STATE` KV; preserve the checked-in `PAID_TASK_LEDGER` binding and SQLite migration.
- [x] Deploy and confirm `/healthz` reports state store, paid ledger, access gate, API key, and Clothes V3 provider as configured.
- [x] Keep R2 disabled for the validated Clothes V3 path; apply `worker/r2-lifecycle.json` before any future switch to Scarf.
- [x] Set independent `SESSION_SECRET`, private `JUDGE_ACCESS_CODE`, and native-app CORS policy that rejects browser origins.
- [x] Select and validate `VTO_PROVIDER=clothes`; do not claim the unprovisioned Scarf/R2 path in the demo.
- [x] Reconcile `UNIT_BUDGET=1018`, rotate `UNIT_BUDGET_ID`, keep `CREDIT_FLOOR=300`, and preserve conservative rate/daily limits.
- [x] Build and sign the APK with the real Worker HTTPS URL via `-PDRAPEPROOF_API_BASE_URL=...`.
- [ ] Confirm free-tier request/KV quotas and no payment method surprise in both dashboards.

## Judge package

Official rules require a working project be available free of charge for judging/testing through the judging period.

- [ ] Repository URL works without requesting access, **or** private repository is shared with `contact_event@PerfectCorp.com`.
- [x] Repository contains all necessary source, non-secret assets, license, and setup/testing instructions.
- [ ] Release/test APK is attached at a stable public URL or otherwise made available free of charge.
- [ ] APK SHA-256 and tested commit are documented.
- [ ] Judge Worker and access code remain available through August 31, 2026.
- [x] `JUDGE_TEST_GUIDE.md` states target Android/API, install steps, permissions, lighting/fabric setup, cloud consent, and expected task wait.
- [ ] No API key, `.dev.vars`, private participant image, personal file path, device serial, or access code appears in Git history/artifacts.
- [ ] Fresh judge-path dry run succeeds from a non-developer device/network.

## Devpost fields

- [ ] Project name: DrapeProof.
- [ ] Topic: Skin AI + Apparel VTO (update only if the final functioning integration differs).
- [ ] One-sentence pitch leads with the purchase problem, not the technology.
- [ ] Text description explains consumer/retail value, core features, YouCam APIs used, architecture, what is original, challenges, accomplishments, learning, and next steps.
- [ ] Description states that VTO is visualization-only and camera colors are device/illumination dependent.
- [ ] Technology list is accurate: Android/Kotlin/Compose, CameraX, MediaPipe, Cloudflare Worker/TypeScript, YouCam Facial Color Tones, and the actually validated VTO provider.
- [ ] Public repository/testing URL entered.
- [ ] Screenshots included and checked for privacy/rights.
- [ ] Public demo video URL entered.
- [ ] Every required Devpost field saved and final preview reviewed in English.

## Demo video (1–3 minutes)

- [ ] Final duration is between 1:00 and 3:00.
- [ ] Shows the project functioning on the target phone, end to end.
- [ ] Explains the YouCam APIs actually used.
- [ ] Shows a real accepted task/result, not a mock presented as real.
- [ ] Shows the controlled real-cloth feature and evidence tier.
- [ ] Shows VTO kept separate from measurement.
- [ ] Uploaded publicly to YouTube (preferred) or Vimeo.
- [ ] No unlicensed music, third-party trademarks, copyrighted product imagery, notifications, secrets, or non-consenting faces.
- [ ] Captions/English translation included as needed.
- [ ] Public URL works in a logged-out browser and HD processing is complete.

Use `DEMO_SCRIPT.md` as the timed shot list.

## Judging-criteria audit

The four criteria are equally weighted.

### Technological implementation

- [ ] Show two real YouCam functions or explain precisely why the final validated set is smaller.
- [ ] Point judges to the non-trivial controlled capture, color math, evidence policy, and secure/cost-protected Worker.
- [ ] Automated gates are green and reproducible; manual/device/API evidence is attached.

### Design

- [ ] First-time user understands real cloth vs photo vs VTO paths without narration.
- [ ] Consent, paid units, uncertainty, and downgrade states are legible.
- [ ] Result answers a purchase choice in one flow; no debug-only screen appears in final footage.

### Potential impact

- [ ] State the audience and costly guess being reduced: choosing an exact product color/avoiding uncertain returns.
- [ ] Demonstrate how exact-SKU intent translates evidence into a purchase action.
- [ ] Avoid unsupported market-size, return-reduction, or accuracy percentages.

### Quality of idea

- [ ] Explain the non-obvious insight: the camera itself reacts to cloth, so evidence requires a paired controlled protocol.
- [ ] Show how local physical contrast, Facial Color Tones, and VTO have different evidence roles.
- [ ] Publish limitations and failed-gate behavior as strengths of the design.

## Final 24 hours

- [ ] Freeze feature work except submission blockers.
- [ ] Run clean Android/Worker builds and all tests from the tagged commit.
- [ ] Install the exact release/test APK on both target phones.
- [ ] Re-run one live scan, one photo path, records export, Facial Color Tones, and VTO.
- [ ] Record remaining units, API-key expiry/rotation plan, Worker/KV health, and judge code.
- [ ] Tag the tested commit and calculate artifact hashes.
- [ ] Verify every public link in an incognito/logged-out browser.
- [ ] Submit before the deadline; save confirmation screenshots/email.
- [ ] Make no substantive post-deadline submission change unless Devpost/Sponsor expressly permits it.

## Honest status at repository creation

Source implementation and core tests exist. Physical-device validation, production deployment, paid YouCam tasks, screenshots, public video, judge package, and Devpost submission are still manual and incomplete. Keep this paragraph until those facts change; replace it only with dated, linkable evidence.
