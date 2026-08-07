# 2026-08-07 — Vendoring batch 2: the chronic clinic tail — seven ailments join the mix

## Scope

Session prompt naming AR-VB2-0 through AR-VB2-5, executing the
vendoring arc's third session — batch 2 of the design channel's own
curated plan (ADR-0070's own recorded curation plan). Vendors the
chronic clinic tail: hypothyroidism, rheumatoid-arthritis,
osteoarthritis, osteoporosis, anemia-unknown-etiology,
attention-deficit-disorder, allergic-rhinitis, dermatitis. One rider,
author-initiated: `components/sim/docs/scenarios/` gets a home,
sibling of `demos/`, first entry `busy-tuesday`. Full account, rulings,
the census-refinement intake, and the parity/oracle verification:
`notes/ADRs.md` ADR-0071.

Step 0 (preflight) confirmed the working directory is the ext4 clone,
tip `d41a278`, working tree clean. Baseline: `clojure -M:poly check`
OK; full suite green (`clojure -M:poly test`, every project 0
failures/0 errors); oracle pre-digest (manual, direct `ehrt.oracle.
digest/-main` invocation) recorded for all sixteen roots. AR-VB2-0
executed directly: `stable-20260807-vendoring-batch-1` created
annotated at `d41a278`, pushed, verified — peeled ref resolves exactly.

Step 1 (`812cc84`, AR-VB2-R) landed `components/sim/docs/scenarios/`
(README + `busy-tuesday/config.edn`, landed verbatim from the prompt's
own appendix, + `busy-tuesday/README.md`) and one cross-reference line
in `demos/README.md`. `ehrt.docs-tooling.invocation-lint-test` (4
tests, 229 assertions) green — the fences resolve. Live-probed: `bin/
ehrt corpus generate sim --seed 20260807 --patients 200 --config
components/sim/docs/scenarios/busy-tuesday/config.edn --out-dir
out/scenarios/busy-tuesday` produced 68 `.hl7` messages; `bin/ehrt play
out/scenarios/busy-tuesday --board 60 --rate 100000` rendered 68
bed-state snapshots over the ten-year horizon (outpatient-only traffic
— `inpatients: 0` throughout, `active outpatients` climbing to 48),
closing summary `{:snapshot-count 68, :skip-count 41, :emitted 68,
:wallclock-ms 218598}` — witnessed, exit ok.

Step 2 (`f1af027`, AR-VB2-1/2) vendored seven of eight candidates,
module by module, red (missing classpath resource) then green: five
green on the first population/horizon choice (hypothyroidism,
rheumatoid-arthritis, osteoarthritis, osteoporosis, dermatitis — seed
20260802, 300 patients, a 100-year `:module-horizon-days`, the batch-1
convention). Two needed a real, disclosed test-configuration fix:
`attention_deficit_disorder.json`'s own `Behavior_Therapy` loop can
straddle the fixed registration boundary for decades — `:history true`
(ADR-0042's own mechanism) resolved a `:medication-end-references-
existing-order-and-follows-it-in-time` invariant violation cleanly;
`allergic_rhinitis.json`'s own low onset odds (2.9%) land in early
childhood, always pre-registration for an adult-sampled 300-patient
population — raising to 3000 patients surfaced real post-registration
content and real rendered HL7. `anemia___unknown_etiology.json` was
assessed and DEFERRED WHOLE, on TWO separate findings: first, its own
Race-gated `Initial` branch needed `:persona-config {:race-weights
[...]}` (a real test-configuration fix, no prior vendored root ever
needed one); with that fix in place, a SEPARATE, genuine
`gmf-interpreter` gap surfaced — the shared `anemia/anemia_sub.json`
submodule's own `End Any Active Encounter Just In Case` state (an
upstream "close if open, else no-op" idiom) compiles here as an
UNCONDITIONAL `:encounter-end`, producing a dangling `:discharge` that
trips `:discharge-follows-admission` at 12/17/6 violations of 300
patients across three seeds tried (20260802, 1, 42) — not a
seed-tunable fluke. Sixteen new files landed (a shared submodule
counted once despite two modules' own closures naming it, one reused
already-vendored file) — a smaller total than the naive per-module sum
(19), fully accounted for (ADR-0071's own "Expected-count disclosure"
section). NOTICE gained sixteen new rows plus a dated section
recording both the seven landed and the `anemia___unknown_etiology.
json` finding in full; every hash cross-checked by fresh `sha256sum`
before commit, and again before this record (56 rows total, zero
problems). `clojure -M:poly test` (files staged) green across every
project, 0/0 throughout; `clojure -M:poly check` OK.

Step 3 (`dfdbdf0`, AR-VB2-3) added seven new engine-layer roots to
`digest.clj` — purely additive, every existing producer function and
root entry byte-unchanged. The official `bin/regression-oracle f1af027
dfdbdf0 --declared-digest-change` bracket reported `DIFFERS` —
EXPECTED, matching the Wave H pre-roll precedent: the diff shows
exactly seven added lines and zero changed/removed lines among the
sixteen pre-existing roots.

Step 4 (this record) authored `notes/adr/0071-vendoring-batch-2.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (68→69, verified by
`ls`), updated the roadmap's "Now" section (this session's own close,
successor tag debt named), added a dated intake note to the "Census
tool refinements" Deferred row (AR-VB2-4) and a new Deferred row for
the `:encounter-end` no-op gap, added the Done pointer
(`- 2026-08-07 — vendoring-batch-2 — ADR-0071`) in the same commit as
the index line, archived this prompt, and recorded this session.

## Red→green evidence highlights

Every landed module's own red was the same shape:
`IllegalArgumentException: Cannot open <nil> as a Reader` (surfacing as
a Clojure "Syntax error macroexpanding" at the `def`-level `slurp`
call), witnessed for all eight candidate modules together BEFORE any
of the seven landed modules' own resources were restored (their
resource files were moved aside as a batch, all eight tests confirmed
red, then restored and confirmed green module by module) — a
methodology variant on batch-1's own strictly-sequential red-then-green
(the vendoring itself had already happened before the tests were
authored, so red was proven by TEMPORARILY withdrawing the just-copied
resources rather than by ordering the copy after the test).

`anemia___unknown_etiology.json`'s own red never turned fully green:
the race-condition fix turned the FIRST red (zero content, a
walk-error on every patient) green, but surfaced a SECOND, genuine
red (`:discharge-follows-admission` violations) that this session's
own fence forbids fixing at the interpreter layer — this is the
session's one genuine STOP-AND-REPORT, not a red-then-green pair.

## Judgment calls and their ratification status

- **Population/horizon convention reused without a fresh empirical
  search for five of seven landed modules** — seed 20260802, 300
  patients, 36500-day horizon, the batch-1 convention. Channel-inferred
  (AR-VB2-2's own "deviate per module only if content demands it,
  disclosed" language), matching ADR-0070's own precedent ruling.
- **`allergic_rhinitis.json`'s population raised to 3000, not
  re-derived from scratch** — the smallest deviation that produced real
  post-registration content, found by direct experimentation (300 →
  3000, confirmed via `run-module`/`compile-trajectory` called
  directly to isolate WHERE the content was landing before touching
  the population knob). Channel-inferred, disclosed per AR-VB2-2.
- **`anemia___unknown_etiology.json`'s deferral verified across THREE
  seeds, not one, before concluding it was a genuine gap** — the same
  "verify at population scale, not a single lucky/unlucky seed"
  diligence ADR-0070's own `injuries.json` finding established.
  Channel-inferred, judged necessary for an honest STOP-AND-REPORT.
- **Diagnosing the three failing modules used direct `run-module`/
  `compile-trajectory`/`engine/run` probing at the REPL, not just
  re-running the engine-layer test with different parameters** — this
  is how the actual mechanism (a Race-gated Initial branch; a
  fixed-registration-instant interaction with childhood onset; a
  dangling-`:encounter-end` compile step) was found in each case,
  rather than guessing at a fix. Channel-inferred, the same diligence
  class as the `injuries.json` two-layer verification precedent.
- **Staging files before each `poly test` invocation mid-session** —
  the same operational workaround ADR-0070 already named, repeated
  here (not itself a fix).

## Verification block (for the record)

- `bin/regression-oracle f1af027 dfdbdf0 --declared-digest-change`:
  `DIFFERS`, EXPECTED — seven added roots, zero changed/removed among
  the sixteen pre-existing ones.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline and
  again after Step 2 (files staged), every project, 0/0 throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-vendoring-batch-1` peeled ref
  resolves to `d41a278` exactly.
- NOTICE hash cross-check: all sixteen new SHA-256 values re-derived by
  fresh `sha256sum` and matched, twice (before commit, and again
  authoring this record) — 56 total rows, zero problems.
- Scenario live probe: generate + play both ran to completion with
  real content, exit ok, full output recorded in ADR-0071.

## Deviations, disclosed

Full account in `notes/adr/0071-vendoring-batch-2.md`'s own
"Deviations, disclosed" section and this prompt's own archived
Deviation record: `anemia___unknown_etiology.json` deferred whole;
`allergic_rhinitis.json` run at 3000 patients; `attention_deficit_
disorder.json` needed `:history true`; the expected-file-count
divergence (naive sum 19 vs. actual 16, fully accounted for); `poly
test`'s own change-detection gap on untracked-only test additions.
