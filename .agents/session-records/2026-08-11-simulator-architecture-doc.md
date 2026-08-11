# 2026-08-11 — Simulator architecture doc lands, made load-bearing by a co-landed purity lint (ADR-0108)

## Scope

Author charter, 2026-08-11 ("Good sequence," ratifying the design
channel's proposal): a dev-docs architecture document for the
simulator, made load-bearing by a co-landed purity lint, wired into
the agent reading path. `docs/dev/simulator-architecture.md` lands —
component inventory for the seven `sim-*` bricks plus `sim`, the
decide/evolve doctrine restated from `engine.clj`/`sim/ADR-0008`, the
mutable-state census, a palgebra section with the real `⨟`/`×`
operators, two honest wrinkles, the naturality witness cited by test
name, and the downstream-latency extension point in one sentence.
`ehrt.docs-tooling.sim-purity-lint-test` makes the census claim
checkable. Two commits: `62d1d5e` (doc + lint + wiring) and this
record's own close-phase commit.

## Red→green evidence highlights

The lint was red-proven non-vacuously this session: a temporary,
clearly marked atom planted in `components/sim-model/src/ehrt/
sim_model/config.clj` (an unallowlisted sim-family file) tripped
`no-mutable-state-primitives-outside-the-two-named-exceptions-test`
red, verbatim captured in `notes/adr/0108-*.md`; removed, `git diff
--stat` confirmed byte-identical to the pre-plant file, and the lint
re-ran green (5 tests, 14 assertions, 0/0) against the real tree.

Every architectural claim in the doc was verified against the live
tree while writing it, not paraphrased from the driving prompt: the
mutable-state census grep re-run live (one hit, `census.clj:407`); the
`decide`/`evolve` line citations read directly from `engine.clj`; the
`world'` one-fold-over-shared-`World` snippet quoted verbatim from
`run`'s own loop (`engine.clj:1534-1541`); the naturality witness
located by grep and cited by its real name and trial count
(`fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`,
150 trials); the `⨟`/`×` operators confirmed as `docs/dev/notation.md`'s
own existing convention, not invented for this doc.

Full local suite (`clojure -M:poly test :all skip:integration`): 608
occurrences of "0 failures, 0 errors," zero `FAIL`/`ERROR` anywhere,
4 minutes 43 seconds — unchanged in substance from the pre-session
baseline (this session added one new test namespace, touched zero
`src`). `clojure -M:poly check`: OK. `ehrt.cli.cli-parse-guard-lint-
test`: 4/22, 0/0. `bin/verify-nist-lock`: OK, 6 coordinates matched.
Oracle bracket (`bin/regression-oracle 5a2832f 62d1d5e`): `IDENTICAL:
every root's digest matches` — all 35 roots, matching the pre-analysis
exactly (a doc, a test file, and register wiring touch no `src`
anywhere).

## Judgment calls and their ratification status

- **The purity lint scans whole files, not per-`defn`.** The driving
  prompt's own Context asks for a file-level claim ("zero
  mutable-state primitives... across the seven sim-family bricks'
  src"), not a function-level one — purity has no `try`-style guard
  to distinguish, unlike the parse-guard lint's own function-granular
  ancestry tracking. The reader-based walker (never regex) is reused
  from that lint's own discipline, per the prompt's own "if it fits"
  instruction. Not a departure; disclosed in the prompt archive's own
  deviation record.
- **`ehrt.sim.version` allowlisted despite triggering no violation
  today.** Its only impurity (`git-sha`'s `.git/HEAD` read) isn't one
  of the five forms this lint polices. Listed anyway, mirroring the
  doc's own two-exception statement, so a future mutable-state
  primitive added to that namespace doesn't need a third allowlist
  entry invented on the spot. Disclosed, not ratified separately (a
  small implementation choice within the prompt's own instruction).
- **`.agents/reading-sets.edn`'s `:sim` budget re-baseline** (970 →
  1295) — the file's own standing "re-baseline when a session's own
  edit trips the budget" discipline, ADR-0107's own re-baseline is
  the precedent, not a fresh judgment call.

## Findings and HEAD landed

No discrepancies found between the doc's own claims and the live
tree — every architectural fact this session set out to cite matched
what the prompt's own Context predicted, so no STOP-AND-REPORT fired.
The tag `stable-20260811-injuries-arc-close` was created at `5a2832f`
(this session's own Step 1), peeled ref verified exact match, remote
unmoved.

**HEAD landed**: `62d1d5e` (doc + lint + wiring), plus this
close-phase commit, both pushed. The last five `main` CI runs at
session start were all `completed`/`success`; this session's own push
also confirmed `completed`/`success` post-push.
