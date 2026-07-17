# Evidence Model

## Principle

DrapeProof reports what the captured evidence supports and downgrades everything else. It never converts a weak input into a strong personal claim through UI wording.

All local color calculations start from encoded sRGB samples, converted through D65 XYZ to CIELAB. Distances use CIEDE2000 (`ΔE00`). Because phone camera pipelines and illumination are not colorimetric instruments, results are most defensible for **relative comparisons within one controlled session or one exact SKU**, not as universal physical color coordinates.

## Contrast Vector

### 1. Cloth–skin separation

For the drape phase:

```text
separation = ΔE00(drape skin Lab, fabric Lab)
ΔL*       = fabric L* - drape skin L*
```

- Positive `ΔL*`: fabric captured lighter than the skin ROI.
- Negative `ΔL*`: fabric captured darker.
- Magnitude at or below 1.0 L* is labeled similar lightness.

This is a difference, not a goodness score. Higher separation is not inherently better.

### 2. Feature definition

For every available iris-center, eyebrow, and lip sample:

```text
component(feature) = ΔE00(feature Lab, skin Lab)
current definition = median(available components)
definition change  = current definition - opening-baseline definition
```

Missing feature ROIs are omitted. A current median is withheld if none is available. The UI describes a recorded change in feature definition; it does not infer attractiveness, age, health, mood, or makeup quality.

### 3. Apparent face shift

Only a passing controlled pair can enable this signal:

```text
cheek shift     = ΔE00(opening cheek, drape cheek)
under-chin shift= ΔE00(opening under-chin, drape under-chin)
aggregate shift = median(available cheek and under-chin shifts)
skin ΔL*        = drape skin L* - opening skin L*
```

At least one paired cheek/under-chin ROI must exist. Otherwise the signal is withheld. The result describes the **camera-recorded appearance under this session's controls**; it is not an assertion that intrinsic skin color changed.

The closing baseline is used as a drift gate. It is not averaged into the opening baseline or used to erase a real drape-phase response.

## Controlled-pair quality gates

Current `QualityThresholds` defaults:

| Gate | Passing threshold |
|---|---:|
| Absolute yaw | `≤ 5°` |
| Absolute pitch | `≤ 5°` |
| Absolute roll | `≤ 5°` |
| Face-scale change | `≤ 3%` |
| Measurement-ROI clipped-pixel fraction | `< 1%` across cheek anchors and drape fabric patch |
| No-fabric baseline luminance coefficient of variation | `≤ 3%` across opening + closing frames |
| Accepted drape frames | `≥ 15` (capture targets 18) |
| Median temporal skin ROI distance | `≤ 1.5 ΔE00` |
| Opening-to-closing skin baseline drift | `≤ 2.0 ΔE00` |
| Expression | neutral according to landmark heuristic |
| Eyes | open according to landmark heuristic |
| Occlusion | required feature ROIs present |
| Sharpness | gradient-energy check passes |
| Flicker | not detected |
| Exposure | AE lock applied and supported |
| White balance | AWB lock applied and supported |
| Fabric region | enough samples and bounded channel deviation |

An unavailable ambient-light metric is a warning in the core policy. In the current Android capture, the coefficient of variation comes only from accepted opening and closing face-luminance samples rather than a hardware ambient-light sensor. Drape-phase luminance is deliberately excluded so the cloth-induced signal cannot disqualify itself. Clipping is sampled only in cheek/fabric evidence patches, summarized separately per phase, then combined by the worst phase; unrelated background, hair, or window pixels cannot stall collection and clean baselines cannot hide a clipped drape. Pose similarly uses the worst absolute phase median, while face scale checks both drape and closing medians against opening. Frames with a blink, non-neutral mouth, blur, or missing feature ROIs are not accepted into the 18-frame target.

These are pre-release engineering thresholds, not clinically validated boundaries. `EXPERIMENT_PLAN.md` defines how to test and revise them without cherry-picking.

## Evidence tiers

The tier is computed from input structure plus the quality result:

| Tier | Required evidence | Permitted claims |
|---|---|---|
| `CONTROLLED_PAIR` | Eligible same-scene input, opening and closing baselines, all controlled-pair gates pass | All three signals, including explicitly camera-recorded apparent face shift |
| `SAME_SCENE` | Eligible face and fabric in one image/session, but no passing full controlled pair | Cloth–skin separation; no paired apparent-face-shift claim |
| `SEPARATE_PHOTO_ESTIMATE` | Eligible face photo and separate product/fabric photo | Approximate cloth–skin separation only, with cross-lighting limitation |
| `PREVIEW_ONLY` | Incomplete/ineligible measurement or generated visualization | Visual preview only; no personalized measurement/rank |

The core `EvidencePolicy.highestSupported` prevents UI code from promoting a lower-evidence input. The live camera flow downgrades a failed controlled session to same-scene evidence and withholds apparent face shift. The photo flow never computes feature-definition change or apparent face shift because it has no controlled baseline/drape sequence.

## Photo sampling

The user taps a cheek and an exact fabric region. The app samples a small median patch locally:

- **Same scene:** both samples come from one image, sharing exposure and white balance. This is stronger than separate photos but weaker than a locked opening/drape/closing sequence.
- **Separate photos:** the skin and product can have different illumination, camera processing, and compression. The result is labeled an estimate and inherits that limitation.

The app does not automatically segment a garment in photo mode; the user's explicit sample points are the provenance.

## Exact-SKU intent ranking

Ranking answers “which variant within this product family is closest to my requested contrast?” It does not rank unrelated products.

1. Compute cloth–skin `ΔE00` for eligible variants of one SKU.
2. Assign midrank percentiles within that SKU; tied separations receive the same midpoint percentile.
3. Match the selected intent target:
   - `Soft`: 0.20
   - `Balanced`: 0.50
   - `Bold`: 0.80
4. Order by distance to target, then lower uncertainty, then stable variant ID.

At least three eligible variants are required. Preview-only profiles are not eligible. Evidence-tier uncertainty values in the demo catalog are 0.5, 1.5, 4.0, and 8.0 ΔE00 for controlled, same-scene, separate-photo, and preview-only inputs respectively; they break equal intent distances but are not statistical confidence intervals.

The current six-color `DP-MATTE-01` catalog is a demonstrator. Its hex values are screen specifications, not measured physical swatches. A production retailer integration must bind each candidate to an exact SKU/variant and measured or controlled catalog provenance.

## Drape Records

The current lightweight local record contains:

- record ID and timestamp;
- source path and evidence tier;
- optional intent;
- SKU, variant ID/name, skin/fabric hex;
- separation `ΔE00`, signed `ΔL*`, scoring version; and
- limitations preserved with the decision.

It contains no raw live-camera image. The richer core record schema additionally models device/camera-control metadata, a full Contrast Vector, quality snapshot, product provenance, ranking snapshot, and opaque YouCam task states; the current UI persistence does not yet populate that complete schema.

## Claim discipline

Allowed:

- “This fabric captured with higher/lower separation than that variant in this session.”
- “Your selected Bold intent targets a higher percentile within these six variants.”
- “The camera recorded an apparent cheek/under-chin shift under passing locked controls.”
- “This separate-photo result is lower-evidence because lighting differs.”

Not allowed:

- “This is your best/most flattering color.”
- seasonal typing or universal undertone rules;
- attractiveness, health, diagnosis, age, ethnicity, or psychological inference;
- “true” physical color or real-world equivalence without calibration evidence;
- using generated VTO pixels as a color measurement;
- comparing percentiles across different SKUs as if they share one scale.
