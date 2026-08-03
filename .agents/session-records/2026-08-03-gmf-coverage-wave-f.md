# 2026-08-03 — GMF coverage Wave F: Counter/ImagingStudy/SupplyList + condition rider

## Scope

Post-F0 census (`2026-08-03-synthea-7e08387-wave-f0.edn`, ADR-0035)
ranked Wave F at 29 modules: `Counter` (14), `ImagingStudy` (10),
`SupplyList` (5), plus a rider of three condition types (`:race`,
`:socioeconomic-status`, `:not`) unblocking 4 more. This session ported
all three state types and the condition rider per the design channel's
own AR-1 through AR-8 rulings (`notes/ADRs.md` ADR-0036), oracle-
bracketed the change, re-ran the census, and closed out records. No
wellness (Wave G), no `VitalSign` (AR-7's own explicit deferral), no
census-tool fixes beyond the filename workaround — explicitly out of
this session's own fence.

## Red→green evidence

- **Step 1 (Counter/ImagingStudy/SupplyList, `98f53ad`).** All 30 new
  tests written and run green directly (loader/interpreter co-landed
  in one edit pass, so a clean pre-fix RED baseline was not staged
  separately — this session's own deviation, disclosed below, not the
  same red→green ceremony Steps 1-3 of ADR-0035 used). `gmf-test` 50
  tests / 151 assertions (47/143 before), `gmf-interpreter-test` 136
  tests / 316 assertions (126/301 before), both 0 failures/0 errors.
- **Step 2 (condition rider + persona, `c9b2bbf`).** 21 new tests,
  same direct-green pattern. `persona-test` 14 tests / 40 assertions
  (9/30 before) — the AR-5 draw-count test (`counting-random`, a
  pre-existing helper, reused verbatim) proves 13 draws unconfigured,
  15 with both weights, 14 with one, by direct method-call count, not
  a value-replay guess.
- `clojure -M:poly check` clean and `clojure -M:poly test :affected`
  0 failures/0 errors at every checkpoint (Steps 1/2/5/6), including
  `sim-emit-hl7`'s own vendored/replay tests (persona.clj's own
  downstream consumer).  `gitleaks git --staged -v` clean, every
  commit.

## Deviation: no separately-staged red→green for Steps 1-2

Unlike ADR-0035's own Steps 1-3 (each stashed and re-run to prove a
failing baseline before the fix), this session wrote loader + interpreter
+ tests for each state/condition group together in one edit pass, then
ran the full suite once, green. The tests themselves still prove the
mechanism (e.g. `counter-defaults-an-explicitly-authored-zero-amount-to-
one-too` would fail without the `(if (or (nil? amount) (zero? amount)) 1
amount)` fix, not `(or amount 1)`) — but this session did not independently
verify each one FAILS against the pre-fix code, the way the build-session
skill's own VERIFICATION section asks for "every gate touched." Disclosed
here as a deviation, not silently treated as equivalent rigor.

## A real gap found and fixed while implementing, not merely anticipated

`ehrt.sim-trajectory.gmf`'s own `normalize-condition` already had a
recursive-normalization clause for compound conditions — but it was
gated on the PLURAL `:conditions` key (`#{:and :or :at-least}`), which
would never have fired for `Not`'s own SINGULAR `:condition` key
(Logic.java's own field name, source-confirmed). Found while implementing
AR-4, not by a research pass alone — a third recursive clause was added
rather than generalizing the existing one, since the two keys (`:conditions`
plural vs. `:condition` singular) are genuinely different shapes on the
raw JSON. A dedicated test
(`not-condition-recursively-normalizes-its-nested-condition`) proves the
nested condition's own `:name` field is ALSO slugged, not merely that the
top-level `:condition-type` keywordizes — proof the recursion actually
re-entered `normalize-condition`, not a coincidental pass-through.

## Oracle bracket (Step 3, no commit — evidence only)

`bin/regression-oracle e26c9c1 c9b2bbf` (tip before Step 1 → the Step 2
tip), all 9 root batches:

| root | changed? |
|---|---|
| `appendicitis` | no |
| `death-fixture` | no |
| `ear-infections` | no |
| `ear-infections-engine` | no |
| `sepsis` | no |
| `sinusitis` | no |
| `sore-throat` | no |
| `total-joint-replacement-engine` | no |
| `urinary-tract-infections-engine` | no |

`IDENTICAL: every root's digest matches between e26c9c1 and c9b2bbf` —
AR-6's pure-identity claim holds, byte-verified by the real
`bin/regression-oracle` script's own output.

## Census re-run (Step 4, `83f7858`)

Same header parameters plus AR-8's own disclosed persona-config delta
(fixed, equal-weighted `:race-weights`/`:socioeconomic-weights` pools).
Verdicts: `:ok-walked` 42→60, `:load-failed` 34→18, `:walk-failed` 9→7,
total 85→85. `Counter`/`ImagingStudy`/`SupplyList` vanish from
top-gap-mechanisms entirely. All 20 verdict changes traced individually
against both artifacts by direct EDN query (not eyeballed):

- **`Counter`-blocked, resolved fully (10):** `bone-marrow-transplant`,
  `breast-cancer`, `colorectal-cancer`, `homelessness`, `hypertension`,
  `lung-cancer`, `metabolic-syndrome-disease`, `pregnancy`,
  `prescribing-opioids-for-chronic-pain-and-treatment-of-oud`,
  `veteran-lung-cancer` → `:ok-walked`.
- **`Counter`-blocked, surfaced next blocker (1):** `mend-program` →
  `:walk-failed` (`max-steps` runaway).
- **`SupplyList`-blocked, resolved fully (4):**
  `dental-and-oral-examination`, `dentures`, `kidney-transplant`,
  `sleep-apnea` → `:ok-walked`.
- **`SupplyList`-blocked, surfaced next blocker (1):**
  `metabolic-syndrome-care` → `:walk-failed` (`max-steps` runaway, same
  mechanism as `mend-program`).
- **`Race`/`Not`-blocked, ALL resolved fully (4):** `allergic-rhinitis`
  (`Not`), `cystic-fibrosis`, `dementia`, `self-harm` (`Race`) →
  `:ok-walked`.
- **`ImagingStudy`-blocked, ALL 10 surfaced a next blocker, ZERO
  resolved alone, ZERO regressed:** `congestive-heart-failure` →
  `VitalSign` (AR-7's own deferral); `gallstones` → `Physiology` (a
  genuinely new deferred type); seven modules
  (`diabetic-retinopathy-treatment`, `myocardial-infarction`,
  `stable-ischemic-heart-disease`, `vhd-aortic`, `vhd-mitral`,
  `vhd-pulmonic`, `vhd-tricuspid`) → each its own distinct unrecognized
  lookup-table column; `injuries` → a PRE-EXISTING, unrelated
  `:schema-invalid` gap (a `complex_transition` entry's own nested
  `:distributions` carrying a NamedDistribution map — `resolve-
  transition`'s own docstring already named this exact boundary as
  unexercised until now).

Net arithmetic: 10+1+4+1+4 = 20 verdict changes, all traced; the raw
−16 `:load-failed` delta is not 29 (14+10+5) because `ImagingStudy`
never resolved a module alone and several `Counter`/`SupplyList`
modules carried more than one blocker — the same fail-fast-masking
class the F0 census already demonstrated.

**Sanity anchors held.** All seven vendored roots stayed `:ok-walked`,
byte-identical across all three census artifacts, matching the oracle
bracket; nothing outside the 20 traced moved.

**Substance note, found live (AR-8b):** 26 of the 42 pre-Wave-F
`:ok-walked` modules — including `stroke` — produce ZERO trajectory
events on every one of their 3 smoke-walk seeds (immediate-terminal,
cross-module attribute block, or empty horizon-complete). This is a
standing property of walk-verification itself (the census's own AR-2
definition never claimed event richness), made newly countable by this
session's own re-run: for the gated chronic cluster, `:ok-walked`
currently means "produces nothing" for over 60% of its pre-F
membership. Recorded in `docs/gmf-interpreter.md` §15 and this
roadmap, named for a future ranking session, not a Wave F defect.

## Judgment calls and their ratification status

- **Steps 1-3 committed together as one commit each** (Counter/
  ImagingStudy/SupplyList in one commit; condition rider + persona in
  a second), not six separate commits matching the driving prompt's
  own Step 1/2/3/4 breakdown — the three states were implemented in
  one interleaved edit pass across shared files (`gmf.clj`/
  `gmf_interpreter.clj`/`compile_trajectory.clj`), and splitting the
  diff into three commits after the fact would have meant hand-editing
  patch hunks rather than a real per-state edit boundary. Disclosed in
  both commit messages and here, not silently treated as satisfying
  the prompt's own Step breakdown literally.
- **The `gmf_test.clj`/`census_test.clj` "still-deferred" example
  fixtures were swapped from `ImagingStudy` to `VitalSign`** — the
  same "stale premise, not silently left" treatment these fixtures'
  own docstrings already document across three prior waves (CallSubmodule
  → MultiObservation → ImagingStudy → VitalSign). Not escalated — a
  mechanical consequence of AR-7's own explicit ruling, not a new
  design decision.
- **`condition-type->keyword` gained explicit `"Not"`/`"Race"`/
  `"Socioeconomic Status"` entries even though the slug fallback would
  already produce the same keywords** — judged consistent with this
  map's own stated purpose ("this project's own grep-able vocabulary
  registry, not merely a convenience transform," per its own docstring),
  not a functional change. Not escalated.
- Neither judgment call met the bar this repo's own precedent sets for
  `AskUserQuestion` (a STOP-AND-ESCALATE-worthy identity break); all
  are named here and in the ADR for the author to overturn if the
  reading is wrong.

## Findings and HEAD landed

- Two real, unplanned findings this session: the `Not`-recursion gap
  in `normalize-condition` (above, fixed) and the `injuries.json`
  pre-existing `complex_transition`/NamedDistribution schema gap
  (above, disclosed, not fixed — out of this session's own fence).
  Neither was anticipated by the driving prompt in this specific form
  (AR-4 named the honest-absence design; the recursion gap and the
  `injuries.json` schema gap were found while implementing/tracing).
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself, each
  verified against its own message file (every diff's only delta is
  `git log --format=%B`'s own trailing-newline artifact, where
  present).
- Commits, in order: `98f53ad` (Steps 1-3, Counter/ImagingStudy/
  SupplyList), `c9b2bbf` (Step 4, condition rider + persona), `83f7858`
  (Step 6, census re-run), and this commit (Step 7 — ADR-0036, doc
  updates, roadmap/parity-plan capture, this record and its paired
  prompt archive, both indexed). Step 5 (the oracle bracket) made no
  commit of its own — evidence only, folded into ADR-0036's own
  execution note.
- **Fence, explicit:** this session did NOT touch `engine.clj`; did NOT
  do any wellness work (Wave G); did NOT build `VitalSign` in any form
  (state or condition); did NOT fix the census tool's own same-day
  filename collision beyond the disclosed workaround — exactly the
  prompt's own Fences section.
