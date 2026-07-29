2026-07-29 — EXP-D3: build and run the CDC/NIST HL7 v2 profile validator fully offline; characterize its report

Repo: `github.com/pragsmike/ehr-testing-tools`, main at whatever head the judge-engine extraction session left (probe first; that session runs before this one). Autonomous session per R30: commit AND push at each checkpoint; hooks gate; tags are the author's alone. Ask nothing mid-session — decision procedures below; skip-and-report anything they don't cover.

Context

EXP-D3 (`components/tools/docs/experiments.md`, row EXP-D3) has never executed. Its scope was already narrowed by the 2026-07-24 deep research (facts register F14): the six vendored NIST-origin Maven coordinates fetch from the confirmed-live `hit-nexus.nist.gov`; no separate mirroring machinery is needed. What remains is the offline wrapper build itself, plus characterizing what the engine actually reports.

Why now: the author has ruled per-engine judge components (extraction session, 2026-07-29) and profile-aware v2 validation as the mechanism for per-site conformance customization (Z-segments, segment inclusion per message type, value-set bindings, conditional predicates, co-constraints — the things HAPI's base-structural tier cannot check). This experiment supplies the evidence every pending ruling needs: the verdict mapping (which NIST classifications map to which verdict arms), the profile-reference shape for the future site-config, and whether the three named customers convict.

The three named customers (facts register F22, `corpus/operators.clj` docstring, `docs/judge-calibration.md` CAL-1): the v2 defect operators P7 probed and dropped as unconvictable at `judge.v2`'s base-structural tier — dropping the PID segment, corrupting a non-header segment's name, blanking a non-header field. They graduate from dropped to registered the day a resident, profile-aware judge can convict all three.

Prior findings that bound this session (read them, don't rediscover):

* `components/tools/docs/research/EHR-testing-tools-selection-research.md` §D3: the CDC wrapper (`github.com/CDCgov/lib-hl7v2-nist-validator`, Apache-2.0, latest release 1.4.0) wraps NIST `v2-validation` 1.6.3. Inputs: an IGAMT-exported profile folder — `PROFILE.xml` required; `CONSTRAINTS.xml`, `VALUESETS.xml`, `VALUESETBINDINGS.xml`, `COCONSTRAINTS.xml`, `SLICINGS.xml` optional; exact case-sensitive names. API: `ProfileManager(ProfileFetcher, profileName).validate(message) → NistReport`, classifications ERROR/WARNING/ALERT/AFFIRMATION.
* Facts register F9: the claim that the upstream build disables SSL verification was CORRECTED — no such config exists. If your build appears to demand `-Dmaven.wagon.http.ssl.insecure=true` or any SSL-verification-disabling flag, that contradicts F9 — STOP and report with evidence; do not proceed with verification disabled.
* Facts register F14 + `notes/tools/ADRs.md` ADR-0005 amendment: the six NIST-origin coordinates are `license-unstated`, posture `:use-permitted--unstated--confirmation-pending` — fetched by users from NIST's official channel at their own initiative, never vendored, never redistributed, never entering anything this repo ships or CI fetches. This session's fetches are exactly that user-initiated class.

Rulings

* [A] Execute EXP-D3 (author, 2026-07-29: "Need to do D3").
* [C] This is an EXPERIMENT: no `src/` code lands. Deliverables are the protocol, the results doc, facts-register rows, and (conditionally, below) lockfile entries. The `judge-v2-nist` component is the NEXT wave, gated on this session's evidence plus the author's verdict-mapping ruling.
* [C] Nothing NIST-related enters `.github/workflows/` (either lane) this session. The nightly integration lane's artifact fetches stay exactly as they are.
* [C] Verdict-mapping is NOT decided here. Record observed classification behavior; the mapping (which classifications reject) is an author ruling made on this evidence.

Read first
`components/tools/docs/experiments.md` (EXP-D3 row, and EXP-C5's protocol/results as the house style for characterizing a validator); `components/tools/docs/experiments/results-template.md` and `results-rubric.md`; the research doc §D3 (above); facts register F9/F14/F22; `notes/tools/ADRs.md` ADR-0005 (artifact registry doctrine + amendment); `artifacts.lock.edn`; `docs/judge-calibration.md` CAL-1; `components/tools/test-fixtures/v2/simhospital/` (the vendored ADT fixture corpus).

Steps

1. Protocol first. Author `components/tools/docs/experiments/EXP-D3.md` per the house template: hypothesis, method, acceptance questions. The acceptance questions, at minimum: (a) does the wrapper + NIST engine build and run with zero network after an initial user-initiated dependency fetch? (b) what does a `NistReport` actually carry, field by field? (c) which classifications occur in practice against valid and mutated messages? (d) do the three named customers convict under an applicable profile? (e) what is the exact transitive jar inventory (coordinates, resolved URLs, sha256s) a future lockfile entry set needs? Commit: `docs: EXP-D3 protocol -- offline NIST/CDC v2 profile validator`
2. Scratch build, not workspace build. Work in a scratch directory OUTSIDE the workspace tree (e.g. `/tmp/exp-d3/`): a minimal `deps.edn` (or the wrapper's own build) adding `hit-nexus.nist.gov` as a Maven repo. The workspace's own `deps.edn` files gain NOTHING this session. Record every resolved artifact: coordinate, repo URL, jar sha256 — this inventory is acceptance question (e).
3. Offline proof. After the initial fetch, sever network for the validation runs (the EXP-C5 precedent: `unshare -r -n`). A run that still works is the offline claim's evidence; a run that doesn't is a finding, not a failure of the session.
4. Characterize against the wrapper's own shipped test profiles/messages first. Capture `NistReport` verbatim (redacting nothing — it's synthetic data). Enumerate observed classifications and their triggering conditions.
5. Then the repo's own fixtures. The vendored SimHospital corpus is ADT; profile applicability is the constraint (a profile is message-type specific). Decision procedure: if an applicable ADT profile exists among the wrapper's or NIST's published sample bundles, run the three named customers' mutants against it and record convict/no-convict per operator. If NO applicable ADT profile is available without authoring one in IGAMT (a web tool + account, out of an autonomous session's reach), record that as the named gap, characterize with whatever profile/message pairs ARE available, and state plainly that customer conviction remains unproven. Authoring a site ADT profile in IGAMT then becomes an author task the results doc names.
6. Results. `components/tools/docs/experiments/EXP-D3-results.md` per the rubric; update the EXP-D3 row in `experiments.md`; facts-register rows for (at minimum) the offline-build claim and the jar inventory, each with evidence and dates. Commit: `docs: EXP-D3 results -- offline NIST/CDC v2 validator characterized`
7. Lockfile entries, conditionally. IF the artifact registry's URL+sha256 download model fits the six-coordinate fetch cleanly (each jar a direct `hit-nexus` URL), add the entries to `artifacts.lock.edn` with `license-status :use-permitted--unstated--confirmation-pending` and the hashes from step 2. IF the shape doesn't fit (transitive graphs are Maven's job, not a URL list's), record the documented jar inventory in the results doc INSTEAD and name lockfile-vs-`:mvn/repos` as a decision for the adoption wave. Do not force the registry shape. Commit (if taken): `chore: lockfile entries for NIST-origin v2-validation coordinates (EXP-D3)`

STOP clauses

* Any licensing surface worse than the recorded `license-unstated` posture (a coordinate carrying a restrictive license text, a fetch requiring acceptance of terms): STOP the fetch, record, report — ENF-1's own rule.
* Any need to disable SSL verification (contradicts F9): STOP, report with the exact failing command and error.
* `hit-nexus.nist.gov` unreachable or coordinates absent: that is itself a finding (F14 re-verification failed) — record it, close the session honestly; do not hunt mirrors.

Close phase
Self-archive this prompt to `notes/prompts/` at the START of the close phase. Session record per `AGENTS.md`. Deviation record — a section literally so titled — REQUIRED: empty is valid ("none"); absent is not.
