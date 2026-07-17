# Experiment Plan

## Decision to validate

Does DrapeProof's locked, gated, multi-frame protocol produce more repeatable cloth–face contrast evidence than a normal auto-camera single-frame capture, on the two available Android devices?

This plan tests measurement **repeatability and downgrade behavior**. It does not claim a phone camera is a physical colorimeter, and it does not test attractiveness or seasonal color theory.

## Preregistered hypotheses

- **H1 — repeatability:** repeated controlled captures of the same person/fabric/device have a smaller absolute difference in cloth–skin `ΔE00` than repeated ungated auto captures.
- **H2 — false-shift control:** controlled sessions with no fabric change have smaller apparent face shift and opening/closing baseline drift than ungated captures.
- **H3 — ordering stability:** within one device, controlled captures preserve the separation ordering of the same six fabrics more consistently across repeats.
- **H4 — honest failure:** when AE/AWB locking is unavailable or lighting/pose changes, the protocol downgrades evidence instead of emitting a controlled-pair apparent-shift result.
- **H5 — cross-device direction:** the two devices broadly agree on within-person fabric ordering even if their absolute Lab/ΔE values differ.

The null for H1–H3 is no improvement over the control protocol. H4 is a deterministic product requirement rather than a statistical hypothesis.

## Protocols

### Control: auto single frame

- Front camera with default auto-exposure and auto-white-balance.
- One baseline frame and one fabric frame.
- No 3A lock, temporal median, stability gate, or closing baseline.
- Same ROI definitions and color math as treatment.

This control is an experiment harness/manual capture procedure, **not a user-facing mode in the current APK**. Implement or document the harness before collecting data; do not substitute arbitrary screenshots after results are visible.

### Treatment: DrapeProof controlled pair

- Screen brightness set to the app's 0.16 capture value.
- Opening baseline, drape, and closing baseline.
- 18 target readings per phase, at least 140 ms apart.
- AE/AWB locks applied when supported.
- Existing pose, scale, clipping, temporal stability, expression, eye, occlusion, blur, flicker, control, fabric-region, and baseline-drift gates.
- Median ROI aggregation and evidence-tier policy exactly as shipped.

## Materials and participants

Minimum useful hackathon validation target:

- 5 consenting adult participants, chosen for variation in captured skin lightness rather than demographic inference;
- 6 solid, matte, opaque fabric colors with no pattern or sheen;
- OnePlus Nord CE6 and Samsung Galaxy F15;
- 2 repeats of each protocol for every person × fabric × device combination; and
- 2 no-change negative-control repeats per person × device, using the same neutral/charcoal fabric or no-fabric state as specified before collection.

This is a product repeatability study, not a population study. Report the exact achieved sample count and all exclusions; do not silently replace this target with a larger-sounding number.

## Environment control

- Use one room position with broad indirect daylight or one stable diffuse lamp; do not mix daylight and colored indoor light.
- Mount or brace the phone at eye height, approximately 50–70 cm from the face. Record the actual distance used and keep it fixed.
- Disable vendor beauty/skin-smoothing filters if exposed by the camera path.
- Keep background, phone orientation, participant position, and fabric-to-jaw distance constant.
- Let each device reach room temperature; avoid charging during the sequence if heat changes performance.
- Clean the lens before each block.
- Keep the fabric flat, fill the dashed ROI, and avoid shading the face.
- Record time of day, lighting description, device/build identifiers, and whether AE/AWB locks were reported available.

Do not use a display as the fabric reference; emitted light does not model reflected cloth color.

## Randomization and blinding

1. Assign fabrics opaque IDs `F01`–`F06` before collection.
2. Generate a random fabric order for each participant/device block.
3. Alternate which protocol runs first by participant and device.
4. Save raw numeric outputs with participant aliases, never names.
5. Freeze the analysis columns, exclusion rules, and acceptance thresholds below before looking at group results.
6. Analyze IDs first; reveal fabric names only for the demo narrative after tables are final.

## Recorded fields

For every run:

- participant alias, fabric ID, device, OS/build, app commit, protocol, repeat, timestamp;
- AE lock available/applied, AWB lock available/applied;
- accepted frame counts and completion time;
- each quality metric, failure code, warning code, and final evidence tier;
- median skin/fabric Lab and sRGB/hex samples;
- separation `ΔE00`, `ΔL*`, feature-definition values, apparent-shift values when allowed;
- opening/closing baseline `ΔE00` drift; and
- operator notes made before viewing the result.

Raw face images are not required for this study. If video is recorded for debugging or the demo, obtain separate explicit consent and define a deletion date.

## Outcomes

### Primary

For each matched person × fabric × device × protocol pair:

```text
repeat error = abs(separation ΔE00 repeat 1 - repeat 2)
```

Report median, MAD, 90th percentile, and every individual repeat error by protocol. The primary comparison is the paired treatment-minus-control repeat error; report the median paired improvement and a bootstrap 95% interval if a reproducible analysis script is added.

### Secondary

- opening/closing baseline drift distribution;
- apparent face shift in no-change controls;
- controlled-pair pass rate and failure-code distribution;
- capture completion time and retry count;
- per-device temporal ROI median distance;
- Spearman rank correlation of the six fabric separations across repeats and across devices; and
- intent recommendation agreement for Soft/Balanced/Bold within the six-variant set.

Do not summarize only successful sessions. Gate failures are product evidence.

## Acceptance gates for the hackathon claim

These are engineering release targets, not universal perceptual truth:

| Claim | Minimum evidence before using it |
|---|---|
| “More repeatable than auto capture” | Treatment median repeat error is at least 25% lower than control, and no device reverses the direction materially |
| “Stable enough for within-session comparison” | Treatment median repeat error `≤ 2.0 ΔE00` and 90th percentile `≤ 4.0 ΔE00` |
| “Closing baseline catches drift” | Every deliberately changed-light/pose challenge is downgraded or its relevant gate fails |
| “Works on both target phones” | Complete matrix on both devices with no crash/data-loss blocker and documented 3A capability |
| “Ordering transfers across devices” | Cross-device Spearman `ρ ≥ 0.80` for the pooled within-person six-fabric ordering, with individual distributions shown |

If a gate fails, narrow the statement. Examples: “repeatable on Samsung Galaxy F15 under tested indirect daylight,” or “directional within-device ranking only.” Do not tune thresholds on the same dataset and present them as preregistered.

## Challenge tests

Run these outside the primary stable-light block to verify failure behavior:

- turn on a warm lamp halfway through the sequence;
- move the phone enough to exceed the 3% face-scale gate;
- rotate/turn the face beyond 5°;
- introduce a glossy or patterned fabric;
- partially cover the face or fabric ROI;
- create highlight clipping near a window;
- run a camera where AE or AWB lock reports unavailable; and
- compare same-scene vs deliberately mismatched separate photos.

Expected result: downgrade/withhold, not a “controlled pair” claim.

## Analysis integrity

- Preserve the first complete dataset and the code/commit used to generate it.
- List missing runs, retries, and exclusions with reasons.
- Separate exploratory plots from preregistered outcomes.
- Do not infer causality from feature-definition changes.
- Do not pool absolute Lab values across phones as if calibrated.
- Publish negative findings and device disagreements in the submission limitations.

## Current status

**Planned, not executed.** No participant/device results or statistical outcomes are claimed in the repository at this time. The pure Kotlin math/gate tests are automated; this experiment still requires the two physical phones and controlled human capture sessions.
