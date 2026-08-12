# 2026-08-11 — Latency demo: same ground truth, two wires, the board as witness (ADR-0110)

## Scope

Author ruling, 2026-08-11, verbatim "demo session." — this session
executes the second half of the latency-realism arc ADR-0109 opened:
mechanism landed, this session supplies the demo. A sibling scenario
config (`demos/scenarios/ed-tuesday/config-latency.edn`), generated
alongside the existing base config at the same seed, and one witnessed
end-to-end run played into this workspace's own `--board` as the
downstream-receiver stand-in the author's own charter (ADR-0107) asks
this workspace to supply cases to. Zero `src`/`test` changes anywhere —
authorship over the landed mechanism, not a code change. Two commits:
`916de14` (config + README) and this record's own close-phase commit.

## Evidence highlights

**Ground-truth invariance, witnessed directly.** Both configs generated
at seed 20260811, same patients/reference-date/churn, separate out-dirs:
`diff out/scenarios/ed-tuesday-base/events.edn out/scenarios/
ed-tuesday-latency/events.edn` reports no differences; both files' own
SHA-256 digest is `b4e776f7...25e637f1`, identical — 383 ground-truth
events either way, `:latency` never reaching `engine/config-keys`
(ADR-0109's own guarantee, made visible rather than merely cited).

**The tuning was rejected once, live.** A first-drafted `:admission`
latency band (60-240 min, centered well above `:transfer`/`:discharge`)
produced disorder on roughly a quarter of every admitted patient at
this scenario's own seed — statistically overwhelming, not the
"occasional, visible" bar the driving prompt asked for, and not
clinically plausible. Retuned twice by live-probe against the actual
seed (counting real wire-order output, not calculating in the
abstract): the shipped ranges (`:admission` 15-90 min, `:transfer`/
`:discharge` 15-60 min, `:order-placed` 10-45 min, `:result-available`
20-120 min) produce disorder on exactly 8 of 92 admitted patients.

**The downstream witness, live.** Patient MRN000013 (Walker, William):
clinically admitted 03:36, discharged 04:13 — ordinary, log-order-
correct history. On the latency wire, the discharge message's own
sampled delay (20m54s) is shorter than the admission message's own
(1h00m46s), so discharge transmits first (04:33:54) and admission
transmits second (04:36:46). Played into `--board` at the base demo's
own `--board 60` cadence: the 04:33:54 snapshot shows Walker already
off the board (his discharge folded); the very next snapshot
(05:43:41) shows him back on the board as `inpatient` in `ED-H13` —
the same bed label the board independently shows occupied by a second
patient (Gonzalez, Emma) in that same snapshot. `fold-message`'s own
`:admission` case applies unconditionally (ADR-0109's Step 5 finding,
reproduced live rather than probed) — Walker's own ghost entry never
clears; it is still on the board at the run's own last snapshot. The
same patient in the base (no-latency) run appears exactly once,
admitted and never seen again once discharged — confirmed directly,
not assumed (`grep -c MRN000013` against both board-play outputs).

**Full gate**, run against the live tree before committing: `clojure
-M:poly check`: OK. Full local suite (`clojure -M:poly test :all
skip:integration`, unredirected capture): 612 occurrences of "0
failures, 0 errors," zero `FAIL`/`ERROR` anywhere — the SAME 612 figure
ADR-0109 reported, confirming zero test/src namespace changes.
`ehrt.docs-tooling.invocation-lint-test`: 4 tests, 199 assertions, 0/0
(up from ADR-0104's own 197 — exactly the two new generate/play command
blocks this session's own README section added, both resolving and
parsing under the fence-path machinery). `ehrt.cli.cli-parse-guard-
lint-test`: 4/22, 0/0, unchanged (`bases/cli` untouched).
`bin/verify-nist-lock`: OK, 6 coordinates matched. `gitleaks git
--staged -v` / `gitleaks detect`: no leaks found. Oracle bracket
(`bin/regression-oracle 2faa5ba 916de14`): `IDENTICAL: every root's
digest matches` — all 35 roots, matching the pre-analysis exactly (this
session's own footprint is a config file, README sections, and
close-phase files only).

## Judgment calls and their ratification status

- **The disorder rate was tuned down from the first draft, not up from
  zero.** The driving prompt's own bar was "at least one" visible
  reordering; the first draft cleared that bar trivially (roughly a
  quarter of admitted patients disordered) but at a rate that read as
  "the ED never charts admissions correctly," not a realistic,
  occasional lag. Retuned to 8/92 — comfortably above the "at least
  one" floor, clearly below "most of them." A judgment call, disclosed
  in `notes/adr/0110-*.md`'s own "The tuned profile" section, not a
  departure from the driving prompt's own "clinical plausibility"
  instruction.
- **`--board 60`, the base demo's own cadence, not a finer grid.** An
  early probe at `--board 3` (fine-grained, ~218 snapshots) located the
  disorder precisely but produced far more output than a README witness
  needs; re-probing at the base demo's own `--board 60` found the SAME
  disorder visible in exactly two consecutive snapshots — chosen for
  the README/ADR because it is reproducible with the same command
  style the base demo already documents, not a cherry-picked finer
  grid unique to this finding.
- **Out-dir naming (`ed-tuesday-base`/`ed-tuesday-latency`), not the
  base demo's own bare `out/scenarios/ed-tuesday`.** Needed distinct
  directories to run both configs at the same seed side by side and
  diff them; named to read clearly in the README's own generate
  commands. `out/` is gitignored — neither directory is committed.

## Findings and HEAD landed

No discrepancies found between the driving prompt's own Design section
and the live tree that would have forced a STOP-AND-REPORT. The tag
`stable-20260811-latency-second-clock` was created at `2faa5ba` (this
session's own Step 1), peeled ref verified exact match, remote unmoved
(`git fetch` + `git rev-parse origin/main` == session's own starting
HEAD).

**HEAD landed**: `916de14` (config + README), `dd084b5` (close-phase),
and one small fix-forward commit (`a462849`) resolving a placeholder
`notes/adr/0110-*.md` left in its own oracle-bracket section (the
close commit's own hash wasn't yet known when that section was first
drafted) — caught before push, corrected as a new commit rather than
an amend, all three pushed together. The last five `main` CI runs at
session start were all `completed`/`success`; this session's own push
also confirmed `completed`/`success` post-push.
