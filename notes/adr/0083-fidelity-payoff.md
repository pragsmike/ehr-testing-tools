## ADR-0083 — Fidelity payoff: anemia comes home — and colorectal's real blocker gets its true name

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the EncounterEnd fix (`notes/adr/0082-encounterend-fix.md`,
tip `82d1753`) closed the interpreter gap structurally and, as part of
its own in-session proof, ran both modules the fidelity arc's own
payoff rider had named — `anemia___unknown_etiology.json` and
`colorectal_cancer.json` — against the pin-verified checkout at the
deferrals' own seeds. The evidence RESHAPED this session's own brief:
the payoff named two modules, but the evidence licenses only one.
`anemia___unknown_etiology.json`'s own violations are extinguished
completely post-fix (clean at all three of ADR-0071's own seeds,
20260802/1/42, 300 patients each). `colorectal_cancer.json`'s own
violations are BYTE-IDENTICAL pre- and post-fix, with a raw-trajectory
scan finding zero dangling `:encounter-end` references anywhere in its
300 seed-42 walks — ADR-0072's own "same EncounterEnd gap" diagnosis
was WRONG for colorectal, and its real blocker
(`:clinical-content-only-when-admitted` violations downstream of clean
trajectories) is a separate, undiagnosed defect. This session therefore
vendors anemia and reclassifies colorectal under its own true name,
rather than landing both as a mini-batch.

Read-first: ADR-0082 in full (the fix, the trace finding, the
colorectal evidence — this session's own ground truth); ADR-0071's own
deferral section (the anemia finding, its own persona-config fix); the
vendoring mechanics exemplar (`vendored_hypothyroidism_test.clj` —
anemia's own closure shape is its sibling, root plus the
ALREADY-VENDORED `anemia/anemia_sub.json`); `components/oracle/src/
ehrt/oracle/digest.clj`'s own root-addition mechanics; the roadmap's
own narrowed Deferred row.

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]`
channel-inferred.

1. **AR-FP-0** `[A — tag law, case (ii); debt recorded in ADR-0082]`.
   `stable-20260808-encounterend-fix` annotated and pushed at
   `82d1753ac20a6f81387a19fa4872f3ff1e385e32` (ADR-0082's own closing
   tip), message "encounterend fix landed, design-channel-verified
   2026-08-08 (ADR-0082)"; peeled ref verified both locally and via
   `git ls-remote origin refs/tags/stable-20260808-encounterend-fix^{}`,
   both resolving exactly. **Executed Step 0.**

2. **AR-FP-1** `[C — the vendoring, standing mechanics]` (anemia comes
   home). `anemia___unknown_etiology.json` vendored VERBATIM from the
   pin-verified checkout (`/home/mg/synthea-checkout`,
   `7e08387c68a7f0e21d13076609a159fd473fc902` — root file only;
   `anemia/anemia_sub.json` was already in-tree via `hypothyroidism`'s
   own closure, re-verified byte-identical against the checkout and
   the NOTICE table before reuse, the batch-1 shared-submodule
   precedent). SHA-256
   `e954b7cb1fe81b301726baa82d5fa83124f64e052f100ebb87a0a6c5d5e5c896`,
   8761 bytes, already covered by the ADR-0072 `-text` rule. One
   NOTICE row landed under a dated section citing this ADR and the
   two-arc deferral story. The committed round-trip test
   (`vendored_anemia_test.clj`) witnessed red in-session against the
   missing classpath resource (`Cannot open <nil> as a Reader`, the
   resource file moved aside then restored — never a working-tree
   stash), committed green — at the deferral's own evidence parameters
   (seeds 20260802/1/42, 300 patients, race-weighted `:persona-config`,
   ADR-0071's own finding first requiring it): real compiled content,
   zero `check/check-all` violations at every seed, real rendered
   HL7. A second deftest pins `:suppressed-encounter-ends` — the A5
   arm's own zero-cost counter (ADR-0082 R2) — across a well-mixed-seed
   300-walk sweep per deferral seed, at the interpreter layer directly
   (`sim-trajectory/run-module`, the same call shape `census.clj`'s own
   `walk-one` uses, since `engine/run` never surfaces this field): real,
   empirically-run totals, not estimated — 33 (mixer 20260802), 23
   (mixer 1), 20 (mixer 42), every affected walk suppressing exactly
   once at this population. A new first-baseline oracle root
   (`anemia-pair`, race-weighted the same way) joins `digest.clj` — the
   existing TWENTY-SEVEN roots stay byte-identical, confirmed both by a
   manual pre/post manifest diff (one addition, zero changes/removals)
   and the official `bin/regression-oracle 841df9a 85ba040
   --declared-digest-change` bracket (`DIFFERS`, EXPECTED — the diff
   shows exactly one added line, `anemia.edn`, zero changed among the
   27 pre-existing roots).

3. **AR-FP-2** `[C — the reclassification, from ADR-0082's evidence]`
   (colorectal's true name). The roadmap's own "EncounterEnd
   no-op-when-nothing-open" Deferred row CLOSED — both modules it ever
   blocked are resolved, neither by the row's own revisit trigger:
   anemia vendors (above); colorectal was NEVER actually blocked by
   this gap (a dated erratum, append-don't-erase, closes the row) —
   the same in-session raw-trajectory scan that cleared anemia found
   zero dangling `:encounter-end` references anywhere in colorectal's
   own 300 seed-42 walks, and its violations sit byte-identical before
   and after the fix landed. ADR-0072's own diagnosis ("same root
   cause, not a new gap") was plausible BY ADJACENCY — the same shared
   `anemia/anemia_sub.json` submodule, the same violation-invariant
   family — never itself probe-verified by a trajectory scan the way
   anemia's own finding always was; this session's own probe is the
   first scan colorectal's blocker ever received, and it overturns the
   inference. ADR-0072's own colorectal-deferral section receives a
   dated erratum (append-don't-erase) recording the correction and
   pointing here. A NEW Deferred row names colorectal's real,
   still-undiagnosed blocker by its own violation class
   (`:clinical-content-only-when-admitted` at 2-of-3 seeds, 300
   patients each — ADR-0072's own original counts, reconfirmed
   byte-identical post-fix by ADR-0082) — UNDIAGNOSED, one compile
   layer downstream of the interpreter, mechanism unknown. Revisit
   trigger: a future session's own dedicated investigation — intake
   for the fidelity arc's own close (ADR-0084). The audit-evidence-
   mechanism rule cited once more (the same lesson AR-EE-1c already
   named correcting ADR-0071): a diagnosis by adjacency is not a
   diagnosis by evidence, and this arc's own probe overturned one.

4. **AR-FP-3** `[C — scope]` (fences). Src/resource changes ONLY: the
   anemia root file, its NOTICE row, its test, its oracle root. NO
   colorectal vendoring, NO investigation of its residual defect
   (intake, not act). NO other interpreter/engine/emitter changes.
   Existing 27 oracle roots identical (confirmed above); anemia's root
   is a declared first baseline. Standing untracked files untouched.

### Execution record

**Step 0 (no commit, executed directly).** Cwd confirmed the ext4
clone (`~/src/ehr-testing-tools`), tip `82d1753`, working tree clean.
The pin-verified checkout re-confirmed at its own recorded commit
(`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean); the
in-tree `anemia/anemia_sub.json` re-hashed against it, byte-identical
(`bde888cb...`, matching the NOTICE row's own hash since batch 2).
`clojure -M:poly check` OK; full suite green (`clojure -M:poly test`,
521 assertions, 0 failures/0 errors, matching ADR-0082's own reported
baseline exactly). Last-five CI runs on `main` disclosed: four green,
one red (`31258465363`, `deabbbd`'s own push) — already disclosed and
closed within ADR-0082's own session (fixed forward the same session
by `eb214ea`, 15-minute window); no NEW red window at this session's
own Step 0. Oracle pre-digest (direct `ehrt.oracle.digest/-main`
invocation, all twenty-seven roots) recorded to a scratch manifest.
AR-FP-0 executed directly: `stable-20260808-encounterend-fix` created
annotated at `82d1753`, pushed, verified — peeled ref resolves exactly
both locally and via `git ls-remote`.

**Step 1 (`841df9a`, AR-FP-1).** `anemia___unknown_etiology.json`
copied byte-verbatim from the pin-verified checkout;
`vendored_anemia_test.clj` authored and witnessed red (resource moved
aside, `Cannot open <nil> as a Reader`), then green after restoring the
resource (2 tests, 13 assertions, 0/0). NOTICE gained one new row plus
a dated section; `ehrt.docs-tooling.notice-verbatim-test` re-run:
green, 4 tests, 143 assertions (up from 141, the new row's own two
assertions). Full suite (`clojure -M:poly test`): green throughout, 0
failures/0 errors, confirmed by grep across the entire run's own
output. `clojure -M:poly check` OK. `gitleaks git --staged -v`: clean.
Post-push verification: one delta, the known trailing-blank-line
artifact.

**Step 2 (`85ba040`, AR-FP-1/2).** `digest.clj` gained one new producer
function (`anemia-pair`) and one new `roots` map entry — purely
additive, every existing producer function and root entry
byte-unchanged (confirmed by diff before staging). Manual pre/post
digest comparison (Step 0's own scratch manifest vs. a fresh run
against the edited tree): the twenty-seven pre-existing roots'
`.edn` output byte-identical, one addition (`anemia.edn`). The
official standing harness, `bin/regression-oracle 841df9a 85ba040
--declared-digest-change`, reported `DIFFERS` — EXPECTED, per the
ADR-0070/0071/0072 precedent: the diff shows exactly one ADDED line
(`anemia.edn`) and ZERO removed or changed lines among the
twenty-seven pre-existing roots. The roadmap's "EncounterEnd
no-op-when-nothing-open" row gained its closing dated note (a single
ALL-CAPS `CLOSED` marker plus a "see Done" disclosure, satisfying
`ehrt.docs-tooling.roadmap-deferred-closure-lint-test` — re-run green,
6 tests, 7 assertions) and a new, colorectal-only Deferred row landed
immediately after it. ADR-0072's own colorectal-deferral section
gained its dated erratum. Full suite green throughout (0 failures/0
errors, confirmed by grep). `clojure -M:poly check` OK. `gitleaks git
--staged -v`: clean. Post-push verification: one delta, the known
trailing-blank-line artifact.

**Step 3 (this record).** `notes/adr/0083-fidelity-payoff.md` authored
directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own stale file count corrected (80→81,
verified by `ls`); roadmap Done pointer
(`- 2026-08-08 — fidelity-payoff — ADR-0083`) added; session record and
prompt archive land in the same commit.

### Verification

- `bin/regression-oracle 841df9a 85ba040 --declared-digest-change`:
  `DIFFERS`, EXPECTED — one added root, zero changed/removed among the
  twenty-seven pre-existing ones (the diff output itself is the
  evidence, not a count comparison).
- Manual pre/post digest comparison (Step 0 baseline vs. Step-2 tree,
  direct `ehrt.oracle.digest/-main` invocation, no worktree): the
  twenty-seven pre-existing roots' `.edn` manifests byte-identical.
- `vendored_anemia_test.clj`: witnessed RED against the moved-aside
  resource (`Cannot open <nil> as a Reader`), witnessed GREEN after
  restoring it (2 tests, 13 assertions, 0/0) — real compiled content,
  zero invariant-catalog violations at all three deferral seeds, real
  rendered HL7, and the pinned `:suppressed-encounter-ends` totals
  (33/23/20) all reproduced by this session's own live run, not
  guessed.
- `ehrt.docs-tooling.notice-verbatim-test`: green, 4 tests, 143
  assertions (up from 141).
- `ehrt.docs-tooling.roadmap-deferred-closure-lint-test`: green, 6
  tests, 7 assertions — the closing dated note's own ALL-CAPS `CLOSED`
  marker carries its required "see Done" disclosure.
- Full suite (`clojure -M:poly test`): green at every step, 0
  failures/0 errors throughout, confirmed by grep across each run's
  entire output, not just the tail.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session; the
  pre-push hook's own re-scan clean on both pushes.
- Post-push message verification, both steps: one delta each against
  the message file, the known harmless trailing-blank-line artifact.
- Tag verification: `stable-20260808-encounterend-fix` peeled ref
  resolves to `82d1753` exactly, both locally and via `git ls-remote`.
- NOTICE hash cross-check: the new SHA-256 re-derived by fresh
  `sha256sum` against the vendored bytes and matched against the
  table, before commit and again authoring this record.
- CI: last-five on `main` at session start disclosed above (four
  green, one already-closed red from ADR-0082's own session, no new
  red window at this session's own Step 0).

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260808-fidelity-payoff` at THIS session's own closing tip**
— the same tag-law case (ii) pattern every prior close in this repo
has used for its own predecessor.

### Index line

```
- 2026-08-08 — fidelity-payoff — ADR-0083
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 80→81, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

The fidelity arc's own oldest deferral closes — `anemia___unknown_
etiology.json` was assessed and deferred whole at vendoring batch 2
(2026-08-07), and comes home here, one fix and one session later,
pinned forever by a committed test that would catch any future
regression in the A5 no-op arm as a moved integer, not a silent pass.
`colorectal_cancer.json`'s own misdiagnosis corrects by the SAME kind
of evidence that closed the interpreter gap in the first place — a
trajectory scan, not an inference — leaving it deferred under its own
true name rather than a borrowed one. Untouched, carried forward from
ADR-0082's own horizon note: the pairing-as-data registry session,
Wave E's risk-attribute/vital-sign register, vendoring batch 4 (the
veteran family), the census closure-count refinement, publish-prep
(F-5/F-6 + F-7), review 2, `sim-emit-cda`, the fixture-relocation and
ADR-footnote Next rows. **What DOES change:** the vendored-module count
rises to twenty-eight content-producing engine-layer oracle roots;
colorectal's own residual defect is now correctly named and awaits its
own investigation session, intake for the fidelity arc's own close.

### Consequence

The payoff rider lands narrower than its own brief proposed, and
disclosed as such rather than forced to fit: one module vendors clean
on schedule, the other's own blocker is renamed rather than
re-deferred under a diagnosis this session's own evidence overturns.
The arc's own recurring lesson — a check (or an inference) that
verifies one property does not verify a different one later cited in
its name — lands a second time, this time on an inference rather than
a check, closing the loop ADR-0082's own AR-EE-1c erratum opened.
