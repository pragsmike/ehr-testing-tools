# 2026-08-12 — Sim palgebra unification, and the manual-arc rulings recorded (ADR-0113)

## Scope

A DOCS-AND-REGISTERS-ONLY session with two jobs: (1) record the batch
of author rulings (R1-R7) from the 2026-08-12 design exchanges — the
"user manual" naming, the manual's own shape and sequence, the demo
exerciser, the audience-register paring, and the palgebra placement
itself; (2) extend `docs/dev/simulator-architecture.md` §4 into the
full palgebra unification R7 ruled, citing `components/corpus/docs/
palgebra-design.md` and `components/sim-trajectory/docs/
trajectory-computation.md` both ways, every claim cited to a
re-verified witnessing test. Zero `src` change, zero test-code change,
zero generated-doc change anywhere. Two content commits plus this
record's own close-phase commit: `9ff7fea` (rulings + roadmap),
`662f038` (the unification).

## Evidence highlights

**Every witness citation re-verified at its own path:line before
landing, none moved or renamed.** The driving prompt named four tests
by name; each was read directly from the live tree before being cited:
`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`
(`latency_test.clj:29`, exact); `batch-round-trip-property-test`
(`framing_test.clj:266`) plus its four sibling round-trips, confirmed
by `grep -n '^(deftest\|^(defspec'` against the live file rather than
assumed from memory (`:29` is actually
`file-per-item-round-trip-property-test`, `:121`
`er7-multi-round-trip-property-test`, `:142`
`ndjson-round-trip-property-test`, `:224`
`mllp-round-trip-property-test` — all four read and confirmed exact,
not just line-number-matched); `play-command-at-huge-rate-matches-
show-identity-test` (`core_test.clj:2800`) and its file-sink sibling
(`:2827`); `fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-
identity` (`emit_fhir_test.clj:147`, 150-trial `defspec`). All four
also reran live under this session's own `make test`, green.

**The reading-set budget checked before writing, not after.** The
`:sim` reading set's own budget (1295 lines, re-baselined at ADR-0108)
was measured against this session's actual doc growth before the
unification subsection was written: `wc -l` across the set's six paths
summed to 1245 (`docs/dev/simulator-architecture.md` alone grew from
300 to 392 lines with the new subsection) — under budget, no
re-baseline needed. Confirmed again live by
`ehrt.docs-tooling.reading-set-budget-test` in the full `make test`
run (5/5 assertions green).

**Full gate, run once, clean.** `make test`: `poly check` OK; 308 test
namespaces, 0 failures/0 errors throughout, including
`ehrt.sim-engine.engine-test` (ADR-0112's own disclosed seed-dependent
flake) running clean on this session's own single pass — no re-run
needed. `bin/verify-nist-lock`: OK, 6 coordinates matched. `gitleaks`:
no leaks, staged or pre-push.

**Oracle bracket.** `bin/regression-oracle 3545026 662f038`:
`IDENTICAL: every root's digest matches` — all 35 roots, exactly
matching the pre-analysis (this session's own footprint is five design
docs and `.agents/*`/`notes/*` registers, none of them any oracle
root's own `src`).

## Judgment calls and their ratification status

- **The R1 naming-token correction's scope.** The driving prompt asked
  for "any other 'user guide' token in this file [roadmap.md] and in
  `.agents/rulings.md`'s live entries" to be fixed to "user manual,"
  while explicitly preserving every VERBATIM quote of the author's own
  past "user guide" phrasing unchanged. This required a line-by-line
  read of every occurrence to distinguish quoted text (inside `*"…"*`
  markers, or a directly quoted fragment in straight quotes) from
  prose describing the same referent — a judgment call applied
  consistently, not author-verbatim per instance. Not separately
  ratified; the driving prompt's own instruction is the ratification,
  applied as literally as the two categories allowed.
- **R4's execution deferral.** `docs/dev/AUDIENCES.md` (the "Seven
  segments" header R4 corrects) is outside this session's own fence —
  the driving prompt names it "executed by a later session, recorded
  now." The ruling is recorded in `.agents/rulings.md` and a roadmap
  row opened; the file itself is untouched this session, exactly as
  fenced.
- **Two exploratory commands that misfired, self-corrected, no tree
  impact.** An early attempt to scope a component-level test run via
  `clojure -M:poly test :all skip:integration :project/docs-tooling`
  instead ran a much broader set of processes (visible in `ps aux` as
  `corpus mutate`/`corpus intake` CLI invocations) — killed rather than
  investigated, since the full `make test` gate at Step 3 was always
  going to supersede it. Neither attempt touched any file or committed
  anything; not a fence issue, just wasted background compute,
  disclosed here for completeness rather than treated as a deviation.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own pre-decided
content and the live tree that forced a STOP-AND-REPORT. All six
Read-first documents matched the prompt's own characterization of
them; all four witness tests existed at their exact cited path:line.

The tag `stable-20260811-batch-straddle-recording` was created at
`3545026` (this session's own Step 0), peeled ref verified exact
match, remote unmoved (`git fetch` + `git rev-parse origin/main`
confirmed `3545026` at session start; the last five `main` CI runs
were all `completed`/`success`, and CI on `3545026` itself came back
green at this session's own preflight — the one dimension the design
channel's own prior verification had left unconfirmed, per the split
license disclosed in `notes/adr/0113-*.md`'s own tag-ceremony section).

**HEAD landed**: `9ff7fea` (rulings + roadmap), `662f038` (the
unification), and this record's own close-phase commit.
