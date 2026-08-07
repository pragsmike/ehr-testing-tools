## ADR-0076 — Quality riders: the review arc opens — the skill lands, the flake gets its fix, preflight widens its gaze

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: `notes/adr/0075-ci-current.md` closed CI red on `main` for the
first time since ADR-0065, and named (not fixed) an intermittent
failure in `ehrt.sim.run-test/merge-config-file-suggests-a-same-stem-
sibling-file` for next-arc intake. This session's own driving prompt
opens the quality-review arc: the `repo-review` skill lands (a
periodic, rubric-driven review discipline generalizing the alignment
and UX arcs), the named flake gets its fix, and preflight's own CI
check widens from one run to five.

**A premise mismatch, disclosed before any work landed.** The prompt's
own premise: the `repo-review` `SKILL.md` pair sits UNTRACKED in the
working tree, hand-placed by the author, awaiting this session's first
commit. The live tree at session start told a different story: the
pair was already committed directly to `main` as `74ebc6b` ("Added
repo-review skill."), outside any session ceremony — no checkpoint-
style message, no session record, no ADR entry — and CI at that exact
tip was **red**. Root cause, traced before proceeding: `readme-
presence-test` failed because `.agents/skills/repo-review` had no
`README.md` (every one of the other fifteen skill directories in both
`.agents/skills/` and `.claude/skills/` has one). The two SKILL.md
copies were verified byte-identical to each other (the STOP-AND-REPORT
trigger the prompt named for a divergence did not fire); the skill-
mirror-currency gate was checked and found to already cover
`repo-review/` via its own `file-seq` glob, needing no enumeration
extended. Disclosed to the author via `AskUserQuestion` before any git
operation beyond inspection; ruled: fix forward now — add the missing
READMEs, restore green CI, disclose the ceremony bypass here, and
continue the arc as planned. AR-QR-1's own text had already ruled the
substance of the fix in advance ("fix the layout to satisfy any gate
that does, never the reverse"); the only open question was the
ceremony gap around the commit that had already landed, and that is
what this Context section records.

R30 ceremony. Read-first (this session): both placed `repo-review/
SKILL.md` copies; `components/sim/test/ehrt/sim/run_test.clj`
`temp-dir-path*` and `merge-config-file-suggests-a-same-stem-sibling-
file`; `components/sim/src/ehrt/sim/run.clj` `similar-sibling-config`;
`notes/adr/0075-ci-current.md`; both `build-session` `SKILL.md`
copies; `components/docs-tooling/test/ehrt/docs_tooling/readme_
presence_test.clj` and `skill_mirror_currency_test.clj` (read live,
mid-session, to diagnose the CI-red finding — not part of the
prompt's own named read-first list, but the fix-forward discipline
required tracing the actual gate before writing to satisfy it).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-QR-0 `[A — tag law, case (ii); debt recorded in ADR-0075]`.**
Annotated `stable-20260807-ci-current` at `9acb79b`, message "ci
current landed, design-channel-verified 2026-08-07 (ADR-0075)";
pushed; peeled ref verified (`git ls-remote --tags origin` resolves
`stable-20260807-ci-current^{}` to `9acb79b` exactly). **Executed Step
0, this session.**

**AR-QR-1 `[A for the skill's adoption (the author placed it); C for
landing mechanics]` (the skill lands).** The two placed copies verified
byte-identical to each other. **Executed with one disclosed deviation**
(see Context, above): the landing commit itself had already happened
outside this session, off-ceremony, and had broken CI. This session's
own AR-QR-1 commit (`d0129b9`) therefore did not land the SKILL.md
pair — that had already happened — but landed the fix the pair's own
absence of a README had made necessary: `README.md` added to both
`.agents/skills/repo-review/` and `.claude/skills/repo-review/`
(byte-identical), the top-level `.agents/skills/README.md` and its
mirror gain the missing index entry (`index-completeness-test` had
also gone red once the README existed but the index didn't cite it —
a second, cascading gate hit, fixed in the same commit). Mirror gate
(`skill-mirror-currency-test`) confirmed already covering the new
directory via its own glob — no enumeration to extend. No other gate
(readme-presence or sibling) objected once these landed; full suite
green after (511 passes, 0 failures, 0 errors).

**AR-QR-2 `[A for the fix mandate; C for the shape]` (the flake fix).**
Three parts, all executed: (i) `temp-dir-path*` (`components/sim/
test/ehrt/sim/run_test.clj`) replaces the delete-then-mkdirs race with
`java.nio.file.Files/createTempDirectory` — atomic, throwing, no
ignored booleans; all three callers unchanged (the string-path
contract is preserved). (ii) `similar-sibling-config` (`components/
sim/src/ehrt/sim/run.clj`) now retries `.listFiles` once on a nil
result (an I/O failure, distinct from an empty directory's own empty
array) before returning nil exactly as before — production behavior
stays best-effort (a did-you-mean is decoration; its absence must
never fail the error path it decorates), but the failure mode is
named in the docstring instead of silently absorbed. (iii) the
sibling-suggestion test's own failing assertion gains a self-
diagnosis message: on failure it prints the temp directory's `.list()`
contents and the raw `.listFiles()` result (nil shown explicitly, not
coerced away), so a future CI fire carries its own evidence. **Red-
first note, disclosed rather than fabricated:** neither change has a
demonstrable local red-first proof. The race itself does not reproduce
locally (ADR-0075's own finding too — green in every local run that
session made). The nil-`.listFiles` path has no portable, deterministic
way to force locally without introducing new environment-dependent
flakiness of its own (root-vs-non-root permission semantics differ
across CI runners) — manufacturing that risk would cut against the
same "environment independence" concern this whole arc exists to
surface. Verified instead by full suite green before and after (511
passes, 0 failures, 0 errors both times) and `poly check` OK.
**THE PROOF IS A SOAK, NOT A RUN:** this fix is a hypothesis with a
stated mechanism, not a witnessed-red-then-green repair. The flake
fired roughly one push in five to seven per ADR-0075's own five-
commit sample; this session's own two pushes after the fix landed
(`9cc3563`, `9a34409`) both came back CI-green with no `merge-config-
file` failure — two data points toward the soak, not a proof by
themselves. The target the fix must beat: it should stop firing
roughly once every five to seven pushes; a session finding it fire
again after this ADR is a regression report against this fix, not a
fresh unrelated finding.

**AR-QR-3 `[A for the policy need; C for the shape]` (preflight
widens, sessions never wait).** Both `build-session` `SKILL.md` copies'
Step-0 "Done when" line amended: check the LAST FIVE runs' conclusions
on main, not the latest alone; disclose all five; a red anywhere in
the five is a finding to report before proceeding. Watch-to-conclusion
stays reserved for a session whose own claim is about CI (the AR-CI-4
precedent); ordinary sessions disclose and proceed, never block on a
running job. Verified byte-identical between copies after editing. The
roadmap's Externals section gains one author-action row: enable
GitHub's workflow-failure notification email for this repository (a
one-time settings toggle, closing the nobody-watching gap at zero
session cost). **Executed** (commit `9a34409`).

**AR-QR-4 `[C — scope]` (fences).** Held: src edits confined to
`run_test.clj`'s helper + sibling test and `run.clj`'s `similar-
sibling-config` (plus the README/index fix AR-QR-1's own text
licensed for the gate the already-landed commit tripped); skill files
land as placed (already had; this session did not re-author their
content); the build-session amendment is the one Step-0 line in both
copies; no assessment work this session (the register and scoreboard
are the next session's, run under the skill itself). **One fence
required a live judgment call, disclosed here rather than silently
applied:** a full-suite run taken for Step 3's own verification
surfaced a THIRD, previously unnamed intermittent failure —
`ehrt.sim-engine.engine-test/every-churned-run-satisfies-the-
invariant-catalog` (`components/sim-engine/test/ehrt/sim_engine/
engine_test.clj:503`), a `defspec` property test whose own generator
seed is not pinned across runs. Failed once (seed `-60645`, 12
patients, at `components/sim-engine`'s churn-profile invariant check),
passed clean on an immediate re-run with the identical tree (511
passes, 0 failures, 0 errors) — confirmed intermittent, not a
regression from this session's own touches, none of which reach
`sim-engine`. AR-QR-4's own fence ("no other flake-hardening sweeps
however tempting — candidates the probe battery will find belong to
the register") is held: named here, disclosed to the author, NOT
fixed this session — the assessment session's own probe battery
(dimension 3, environment independence; dimension 6, sampling
adequacy) is where a third flake belongs, the same way ADR-0075 named
its own second finding for a session other than the one that found it.
`config/busy-weekday.md` untouched. The oracle bracket shows all
twenty-seven batches identical (see Verification).

### Skill provenance

`repo-review`'s `SKILL.md` was authored by the design channel,
2026-08-07, crystallized from the incident history `notes/adr/0075-
ci-current.md` names in its own docstring (the flaky temp-dir test
that survived the hermeticity lens that created it; the LF-normalized
CSV that survived a correct hash, ADR-0072; the stale `cli.md` that
survived a green local suite, ADR-0075 itself) — the ROTATING LENS its
own eight-dimension rubric adds over the alignment/UX arc pattern
(survey → register → rule → fix → close), because each of those three
incidents passed clean under whatever ONE lens was watching at the
time. Hand-placed by the author into both skill homes; landed into
this repo's own ceremony (index entries, README, mirror parity) by
this session, per AR-QR-1.

### Execution record

**Step 0 — preflight + tag.** Working directory confirmed the ext4
clone; tip `74ebc6b` (one commit past the prompt's own stated `9acb79b`
— the premise mismatch, Context above). `clojure -M:poly check`: OK.
Full suite baseline: RED, one failure, reproducing CI's own
`readme-presence-test` finding exactly (`.agents/skills/repo-review`
missing `README.md`) — a locally-reproduced confirmation of the CI-red
finding, not a fresh local-only bug. Last-five CI conclusions on main
disclosed (the check this session's own AR-QR-3 widens, exercised
early): `74ebc6b` failure, then four consecutive successes back to
`31221625531`. AR-QR-0 executed: tag created, pushed, peeled ref
verified.

**Step 1 — fix-forward, the skill lands (AR-QR-1).** README gap fixed
(two skill-home READMEs, two index-entry mirrors); full suite
confirmed green (511 passes, 0 failures, 0 errors). Committed
`d0129b9` ("docs: the repo-review skill lands in both homes -- the
lens that rotates (quality riders, AR-QR-1)"), pushed, verified.

**Step 2 — the flake fix (AR-QR-2).** `temp-dir-path*` rewritten
atomic; `similar-sibling-config` gains the nil-vs-empty distinction
and a retry; the sibling-suggestion test gains self-diagnosis. Full
suite green before and after. Committed `9cc3563` ("fix: the temp dir
is atomic, the nil is named, the failure diagnoses itself (quality
riders, AR-QR-2)"), pushed, verified.

**Step 3 — preflight widens (AR-QR-3).** Both `build-session`
`SKILL.md` copies amended, verified byte-identical; roadmap Externals
row added. A full-suite verification run surfaced the third flake
named under AR-QR-4 above; an immediate re-run confirmed it
intermittent and unrelated to this session's own touches. Committed
`9a34409` ("docs: preflight reads five runs deep, and the author gets
an email (quality riders, AR-QR-3)"), pushed, verified.

**Step 4 (this entry) — ADR-0076 + record.** This file lands;
`notes/ADRs.md` gains its index line; `notes/adr/README.md`'s own file
count corrects 73→74 (verified by `ls`, not arithmetic). Roadmap Done
pointer appended:

```
- 2026-08-07 — quality-riders — ADR-0076
```

### This close's own mechanical debt, recorded here

**The next session that opens fresh work tags `stable-20260807-
quality-riders` at THIS session's own closing tip, under standing
ceremony.** No tag is created by this session for its own closing tip
— tag law's own case (ii) licenses a session to tag its PREDECESSOR's
verified stable point, not its own mid-flight tip; this session
inherits `stable-20260807-ci-current` (AR-QR-0, above) and passes its
own tag forward exactly the same way.

### The horizon, restated unchanged

This session opened the quality-review arc's own instrument; it did
not touch the horizon `notes/adr/0074-vendoring-arc-close.md` named
(EncounterEnd, Wave E's own register, vendoring batch 4, pairing-as-
data, publish-prep). None of those were in scope here and none were
touched. **What DOES change:** the design channel's own next act is
now named — after fresh-probe verification of this session's landing
(the skill pair byte-diffed against its own delivered copy, the
helper's new shape read, the widened preflight line in both mirrors),
the ASSESSMENT session gets its prompt: `repo-review`'s own steps 1–4
(history scan, probe battery, dated register, scoreboard), producing
the arc's first dated register and mitigation plan, returning to the
design channel for the author's rulings before any fix session runs.

### Findings for next-arc / review intake

Two intermittent failures are now on record, neither fixed by design
(one by this session's own explicit fence, one because it was found
mid-session and is out of this session's own named scope):

1. **`ehrt.sim.run-test/merge-config-file-suggests-a-same-stem-
   sibling-file`** — the TOCTOU race AR-QR-2 addresses. Not closed by
   this ADR; closed only by a soak across future CI runs beating the
   roughly-one-in-five-to-seven prior rate (see AR-QR-2, above, for
   the target and the two data points gathered so far).
2. **`ehrt.sim-engine.engine-test/every-churned-run-satisfies-the-
   invariant-catalog`** — a `defspec` property test, seed not pinned
   across runs, failed once locally (seed `-60645`, 12 patients) and
   passed clean on immediate re-run with an identical tree. Newly
   found this session (AR-QR-4, above); not investigated further —
   whether this is a real churn-profile invariant bug that only a rare
   seed surfaces, or a property purely of test.check's own unpinned
   exploration, is unknown and belongs to the assessment session's own
   probe battery (dimension 3, environment independence; dimension 6,
   sampling adequacy — the rubric's own stated home for exactly this
   shape of finding).

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): red at
  Step 0 baseline (one failure, `readme-presence-test`, matching CI
  exactly); green after Step 1 (511/0/0); green before and after Step
  2 (511/0/0 both); one intermittent failure surfaced during Step 3's
  own verification run (`every-churned-run-satisfies-the-invariant-
  catalog`, unrelated component), green on immediate re-run (511/0/0)
  — see Findings, above.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` also ran automatically on every push (pre-push hook),
  clean throughout (729→732 commits scanned across this session's
  three pushes).
- Post-push message verification, every commit this session: one
  delta each against its message file, the known harmless trailing-
  blank-line artifact.
- `bin/regression-oracle 9acb79b 9a34409` (Step 3's own closing tip,
  the last commit touching any digest-relevant path this session):
  all twenty-seven vendored-root batches confirmed IDENTICAL, soundness
  "yes outside ns form" — this session's own touches were test/doc/
  skill-file only (`components/sim/test`, `components/sim/src/ehrt/
  sim/run.clj`'s helper function, `.agents`/`.claude` skill and plan
  docs) — no digest-relevant path in any oracle-covered component, and
  this ADR's own closing commit (Step 4) touches only `notes/` and
  `.agents/`, so the bracket is not re-run against it.
- Tag verification: `stable-20260807-ci-current` peeled ref resolves
  to `9acb79b` exactly (`git ls-remote --tags origin`).
- CI, this session's own three pushes: `d0129b9` success, `9cc3563`
  success, `9a34409` success (each checked directly, not merely
  assumed) — the widened five-run check AR-QR-3 itself specifies was
  exercised at Step 0 preflight (see Execution record) rather than
  only after landing it.

### Fences

Everything AR-QR-4 names, held, including the one live judgment call
it required (the third flake, disclosed and deferred rather than
fixed). No cli-spec change, no workflow-file change, no Makefile
change. `config/busy-weekday.md` untouched. No assessment work this
session — the register and scoreboard are the next session's own, run
under `repo-review` itself.

### Consequence

The `repo-review` skill is live in both skill homes, its own landing
gap (a bypassed ceremony, a broken README gate) found, fixed forward,
and disclosed rather than quietly absorbed into a tidier narrative.
The flaky `merge-config-file` test named in ADR-0075 has a fix with a
stated mechanism and a stated target rate — proven by soak, not by a
single green run, the same discipline this repo already holds
regression-oracle claims to. Preflight itself widened: a session
starting fresh now reads five runs deep instead of one, closing the
exact gap that let a probabilistic red hide behind an all-green
"latest run" check. A third, independent intermittent failure
surfaced in the course of this session's own verification work rather
than being sought out — named for the assessment session rather than
folded into this one's scope, the same restraint ADR-0075 showed for
the flake this session just fixed. The quality-review arc's own first
instrument is now landed; its first survey is the next session's own
act.
