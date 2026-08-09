## ADR-0092 — Repo review 2: the second assessment — one red closes, two greens don't hold

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

Prior: `notes/adr/0091-storefront-fixture.md` closed the storefront
session, the conviction arc's own trailing act. This session is the
repo's second periodic quality review (`repo-review` skill,
`.agents/skills/repo-review/SKILL.md`), opened by the author's own
ruling, verbatim: **"Review 2."** Scope is the skill's steps 1-4 (the
prior-arithmetic re-derivation, the history scan, the eight-dimension
probe battery, the dated register) plus the step-5 mitigation-plan
DRAFT — rulings are the author's, this session proposes
(AR-RR2-1). Nothing moves beyond the register, this ADR, and ceremony
files: no fix, however cheap, no roadmap Deferred/Next content edit,
no law append, no state.md regeneration, no src/test/deps touch
anywhere (the ADR-0049/0058 discipline, restated by the skill itself).

R30 ceremony. Read-first: `.agents/skills/repo-review/SKILL.md` in
full, its two homes byte-diffed as the session's own first D5 probe
(clean); `.agents/plans/2026-08-07-repo-review-findings.md` in full
(the baseline scoreboard, the watch items); `notes/adr/0077-repo-
review-1.md` and `0078-result-or-loud.md` (the prior review's landing
shape and its fix arc); every ADR from `0081-fidelity-riders.md`
through `0091-storefront-fixture.md` in full (eleven files, the
window's own primary record), plus the session records paired with
each. The full register lands at `.agents/plans/2026-08-09-repo-
review-findings.md` — this ADR restates only the narrative, the
scoreboard, and the plan draft, per this repo's own standing
narrative-hierarchy convention (the register is the survey artifact
itself, not duplicated here).

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-09). `[A]`
author-ruled, `[C]` channel-inferred.

**AR-RR2-0 `[A — tag law, case (ii); debt recorded in ADR-0091]`.**
Annotated `stable-20260809-storefront-fixture` at `451d159`, message
"storefront fixture landed, design-channel-verified 2026-08-09
(ADR-0091)"; pushed; peeled ref verified (`git ls-remote --tags
origin` resolves `stable-20260809-storefront-fixture^{}` to `451d159`
exactly). **Executed Step 0.**

**AR-RR2-1 `[A — the review mandate, "Review 2"; C for scoping]`.**
Steps 1-4 of the skill in full, plus the step-5 plan draft. **Executed**
— 76 rows across 8 dimensions, 57 close-as-fine, 8 fix-session-
candidate, 5 ruling-needed, 5 intake, 1 explicitly non-tallied
cross-reference row; full detail and the two-column scoreboard live in
the register, restated below.

**AR-RR2-2 `[A — the skill's own standing step]`.** Before drafting
this run's own register, review 1's own summary arithmetic re-derived
directly from its per-dimension rows. **Executed** — this session's
own independent recount reproduces review 1's own 2026-08-07
fix-forward correction exactly (45 disposition-carrying rows, 28/9/5/3,
46 total including the D7-4 pointer row): no further drift found. Full
working shown in the register's own AR-RR2-2 section.

**AR-RR2-3 `[C — the history-scan window]`.** The window from review
1's own date to this session's own tip — the tail of the quality-review
arc (0079-0080), the fidelity arc (0081-0084), the conviction arc
(0085-0089), vendoring batch 4 (0090), the storefront (0091) —
swept for every disclosed deviation and incident, each dimension-
classified. **Executed** — ten named-minimum incidents found, read at
their primary source, and classified (the register's own History-scan
section, rows H-1 through H-10); every incident folded into its owning
dimension's table rather than double-counted. The two repeat-incident
classes the rubric's own repeat-raises-severity instruction singles
out: the warm-artifact-cache family (ADR-0004's pre-window origin,
then `cd08b20` in-window — drives D3's own YELLOW) and the
"diagnosis by adjacency is not a diagnosis" lesson (AR-EE-1c then
AR-FP-2, now itself codified as a standing ruling rather than left to
repeat a third time).

**AR-RR2-4 `[C — the headline re-scores]`.** D4 re-run fresh
(nil-flowing I/O, category-less catches, silent caps, defaulting
parses), the result-or-loud gates verified by reading them AND by the
probes' own results; D2's CI-lane map and new-ruling-to-gate map;
D3's fresh-clone probe with GENUINELY cold artifact caches; D7's
carried-item aging, all eight named candidates checked. **Executed in
full** — see the dimension sections below and the register's own
detail.

**AR-RR2-5 `[C — probe hygiene]`.** Every probe row: dimension,
command/method, expected, observed, verdict — negative results
included. **Executed** — the register's own 76 rows include every
clean probe alongside every finding; zero STOP-AND-REPORT-worthy
active defects were found (every finding is a recommendation against a
live, otherwise-healthy tree).

### The scoreboard

| dimension | review 1 (2026-08-07) | review 2 (2026-08-09) | movement |
|---|---|---|---|
| D1 — Claim-reality coherence | GREEN | GREEN | unchanged |
| D2 — Guard coverage | YELLOW | YELLOW | unchanged (different findings underneath) |
| D3 — Environment independence | YELLOW | YELLOW | unchanged (stronger evidence, held by a repeat incident) |
| D4 — Error honesty | **RED** | **GREEN** | **improved** |
| D5 — Mirror and derivation drift | GREEN | GREEN | unchanged |
| D6 — Sampling adequacy | YELLOW | YELLOW | unchanged |
| D7 — Continuity integrity | GREEN | **YELLOW** | **regressed** |
| D8 — Operator experience | GREEN | **YELLOW** | **regressed** |

**Review 1: 4 green / 3 yellow / 1 red. Review 2: 3 green / 5 yellow /
0 red.** The movement is genuinely mixed. This repo's single most
severe finding across both review cycles — D4's repo-wide,
demonstrated-live silent-success I/O pattern — is closed, and this
session independently re-verified the close from fresh evidence
(reading and RUNNING the recurrence gate, an independent grep sweep,
three new function-level live-execution probes) rather than trusting
the fix session's own account. Set against that: two dimensions clean
last review (D7, D8) each surface one real, evidenced regression
pattern this time, and both trace to the identical root shape — **a
fix or a gate that closed the literal trigger a prior probe happened
to exercise, while leaving a structurally adjacent, more general
trigger of the same class open**: D8-3 found the "missing path"
fix (AR-RL-3) never covered "path exists but is unreadable," the exact
same raw-stack-trace symptom, in the same three commands; D7-7/D7-8
found the "re-surface it explicitly" fix for wellness-encounters
(review-1's own D7-6) held for exactly one restatement before the same
horizon-note-drop pattern recurred in two DIFFERENT items, because
none of the three ever gained a structural roadmap-row anchor. A third
instance of the same underlying shape — a fix that closes one instance
of a class without closing the class — appears in D2's own new finding
(D2-18): the `2088763` classpath break and `cd08b20`'s warm-cache
break are two closely related "the fast/local/per-push lane
structurally can't see this" incidents, landing in the SAME session
(ADR-0091), with zero mechanical gate added for either.

### D4 verdict, in full: the headline finding

Review 1's D4-1 was the single highest-severity row across both
reviews: `bases/cli/core.clj`'s `mutate-command` could return
`{:status :ok, :payload {:count 0, :files []}}` for a directory
listing that had actually failed at the OS level — a real I/O failure
producing a clean, successful, WRONG answer with zero error surfaced,
in direct violation of this repo's own "errors name their artifact"
standing rule. Fix session 1 (`notes/adr/0078-result-or-loud.md`)
claimed to close it with a shared `ehrt.kernel.io` helper, an
eleven-site conversion sweep, and a recurrence gate
(`io_vocabulary_lint_test.clj`). This review's own D4 probe did not
trust that claim — it re-derived it from scratch: `mutate-command` and
`files-with-extension-in` were read directly and call-graph-traced
through to `ehrt.kernel.io/list-files`, confirming the guarded helper
is genuinely in the path, not merely referenced in prose. The
recurrence gate was READ in full (not just its filename) and RUN
directly (`clojure -M:dev:test`, bypassing the broad suite) — 3 tests,
95 assertions, 0 failures, against HEAD. An independent grep sweep
(the probe's own regex, not copied from the gate) reproduced the
gate's own finding exactly: two allowlisted sites, zero violations,
allowlist unexpanded. Every commit landed since the fix (34 changed
files across colorectal payoff, the straddle fix, the pairing
registry, and the storefront) was checked and introduces no new bare
I/O call. **D4 moves from RED to GREEN on this evidence.** The same
probe, extended past ADR-0078's own named scope
(`.listFiles`/`.list`/`.renameTo` only) into adjacent CLI flag-file
reads, found three small, lower-severity siblings (D4-5/6/7:
unguarded `edn/read-string`/`json/read-str` reads in `mutate-command`,
`gate-command --baseline`, `check-command --assertions`, each
demonstrated live to raise a raw, uncaught Java exception on plausible
malformed input) — loud crashes, not silent successes, so they do not
reopen the D4-1 verdict, but they are real, live, and named as
fix-session candidates below.

### The plan draft (step 5) — for the author's ruling

Everything below is a PROPOSAL. Rulings are the author's.

#### Fix-session clusters (each with a co-landed gate)

**Cluster A — CI/gate wiring (D2-18, D2-4).** Two "a check exists (or
should exist) but doesn't run where it matters" gaps, same shape, same
file family (`test.yml`/`Makefile`). (i) A static docs-tooling gate,
reader-based like its siblings (`sim_emit_hl7_dependency_test.clj`'s
own extraction method): for every project in `workspace.edn`, parse
each composed brick's TEST-tree requires, resolve to the owning brick,
assert it's in the composing project's own `deps.edn` — closes the
`2088763` classpath-break class structurally, not just this one
instance. (ii) Add `bin/verify-nist-lock` as an explicit `test.yml`
step (or fold it into the canonical "Full suite" command every session
already runs), restoring the enforcement surface its own header
comment has claimed for three arcs without actually having it.
Co-landed gate: the new static-gate test itself (i); (ii) needs no new
gate, only correct wiring, verified by disclosing a real "not yet
resolved" trip in a scratch scenario before landing.

**Cluster B — the CLI parse-guard family (D4-5, D4-6, D4-7, D8-3).**
Four unguarded reads sharing one root cause and one already-precedented
fix shape in this same codebase (`kernel/artifact.clj/read-lockfile`'s
categorized-rejection pattern, `sim/run.clj`'s config-loader
try/catch): `mutate-command`'s per-file JSON read, `gate-command
--baseline`'s EDN read, `check-command --assertions`'s EDN read
(D4-5/6/7), and `corpus mutate`/`gate`/`show`'s file-open path on a
permission-denied (not just missing) target (D8-3, AR-RL-3's own
incomplete fix). One small session: wrap each read in a
try/catch-around-the-read, matching the sibling pattern exactly, no
new design. Co-landed gate: extend `io_vocabulary_lint_test.clj`'s own
family (or a sibling lint) to also flag a bare `edn/read-string`/
`json/read-str`/`slurp` on an operator-supplied CLI path with no
enclosing `try` in the same function — the same shape of static check
that already works for the `.listFiles`/`.renameTo` class.

**Cluster C — trivial, ride-along fixes.** D8-7 (one-line README path
fix, `docs/adr/` -> `notes/adr/`); D8-4 (route bare/`help`-level
unknown flags through the same `:unknown-flag` category subcommands
already use, or explicitly document the tolerance — author's call,
see rulings below); D7-7/D7-8 (land a one-line Deferred or Next row
in `roadmap.md` for wellness-encounters and the `notice_verbatim_test`
coverage gap, giving both a structural anchor). None of these needs
its own session; any doc-touching session can carry them.

#### Rulings-needed (options + recommendation each)

1. **D8-6 — the front-door demo's honesty gap.** README's "See it
   run" front door is mechanically exact (byte-reproducible, exit 0)
   but its "a busy Tuesday... live bed board" framing doesn't match
   what actually renders (a decade-spanning idle-skip, one inpatient
   admitted). The sibling `demos/scenarios/busy-tuesday/README.md`
   already discloses this honestly. Options: (a) carry the same
   disclosure prose up into README's own "See it run" section; (b)
   pick different demo parameters that show a visibly busier board
   within the front door's own wallclock budget. **Recommendation:
   (a)** — cheaper, preserves the deterministic byte-reproducible
   fence this review just re-verified, and matches the disclosure-
   over-engineering-around pattern this repo already trusts elsewhere
   (D8's own "narration, clearly marked" precedent from review 1).

2. **D6-2 — codify "match the measurement's RNG path to the claimed
   population."** ADR-0087's own disclosed, self-caught sampling-
   transfer miss is a real, generalizable lesson, currently only in
   ADR prose. Options: (a) adopt as a standing ruling, the same shape
   AR-RL-5(5) used for the multi-seed-once-flagged practice; (b) leave
   as precedent-only, to be rediscovered from ADR archaeology if it
   recurs. **Recommendation: (a)** — cheap, and review 1 already
   proved what happens to un-codified practices (D6-4 had to ask for
   exactly this once before).

3. **D7-7/D7-8's broader policy — mandatory roadmap anchors for
   horizon-note-only items.** Beyond the two specific rows (Cluster C,
   above), this review found direct A/B evidence within its own
   window: the one comparable aged item WITH a roadmap anchor
   (the census undercount, D7-9) self-healed after a single missed
   restatement; the two WITHOUT one did not recover across three.
   Options: (a) rule that any item surviving past one arc close purely
   in horizon-note prose must gain a `roadmap.md` Deferred/Next row in
   the SAME close that first restates it; (b) leave to session
   discretion, unchanged. **Recommendation: (a)** — the evidence for
   it is inside this very review, not speculative.

4. **D7-13 — the Vital-sign/Wave-E cluster, now 4 closes stalled.**
   Options: (a) schedule the Wave E design session explicitly; (b)
   park it with a named revisit trigger, the same shape Externals
   uses successfully (7 closes, zero drift risk, because it's
   correctly parked rather than ambiently carried). **Recommendation:
   (b)** — four closes of identical restatement with zero movement is
   evidence this is backlog, not urgent; naming a trigger (e.g. "the
   next content-vendoring session with a vital-sign-adjacent
   candidate") converts it from ambient debt to correctly-parked, the
   same fix that already worked for Externals.

5. **H-6 — the em-dash commit-message flattening's verification-scope
   gap.** The standing post-push check verifies "committed matches the
   message FILE," never "the file matches authoring intent" — a gap
   that let a formatting drift through undetected in-session, caught
   only by an external channel report one session later. Options: (a)
   add a byte-level non-ASCII-character disclosure step to the
   standard post-push verification; (b) accept as an author-side
   authoring discipline with no mechanical gate, the same class of
   judgment D2's own "not every law needs a gate" rulings already
   apply to process disciplines. **Recommendation: (b)** — the actual
   content drift here is cosmetic (a dash character, not a fact or a
   number), and this session's own ASCII-only commit-message practice
   already prevents recurrence going forward without needing a new
   gate.

6. **D6-1 — the census `:closure-file-count` undercount, now aged
   through six horizon-note restatements since review 1's own
   "escalate priority" ask went unactioned.** Options: (a) schedule a
   small census-tool session now (the fix is well-understood: extend
   the JSON-module resolver's counting to the CSV lookup-table
   resolver too); (b) explicitly re-defer with a stated trigger, the
   same pattern as Wave E above. **Recommendation: (a)** — unlike Wave
   E, this is a small, mechanical, already-diagnosed fix with a
   repeat-cost track record (3 prior undercounts); it does not need
   the "trigger" treatment Wave E's genuinely-open-ended design
   question needs.

#### Deliberately-fine (named, no action)

D1 in full (module/tag/test-file/budget counts all reconcile exactly,
including a from-scratch 6-row NOTICE gap explained precisely by
state.md's own citation tag). D5 in full (docsgen zero-diff,
skill-mirror byte-identical across all 17 pairs including the skill
conducting this very review). D2-13 through D2-17 (all six of review
1's fix-session gates confirmed genuinely real by direct read and, in
two cases, live re-run — not trusted from their own closing ADRs'
narration). D3-1 (the cold-cache fresh-clone probe, unambiguously
green). D6-3/D6-5/D6-6 (the round-trip convention, the `defspec`
population, the seed policy — all unchanged, all re-derived). D6-4
(the three skipped NIST cells — correctly disclosed as un-witnessable
or noise-floor-masked, not a coverage gap to close). D7-1/D7-2/D7-4/
D7-5 (the citation sweep at 2x sample size, the continuity-gate family
at exact baseline plus a genuinely new fifth gate, attic-vs-live
consistency). D7-10/D7-11/D7-12 (the seed-pin positive control,
Externals' healthy 7-close parking, vendoring batch 4's full
resolution). D8-1/D8-2/D8-5/D8-9 (the fence sweep, the "What you get"
re-verification, help-text wrapping at three widths, a trivial
prerequisite-naming nit).

#### Carried to review 3 (intake, unchanged disposition or strengthened)

D3-2 (both flakes' SOAK, now on a larger, still-clean sample — re-run
once the sample roughly doubles again). H-3 / the oracle blind spot
(a structural instrument limitation, not a small-session fix — worth
the author's own explicit acknowledgment that byte-identity oracles
are a floor, not a ceiling, on semantic correctness). D7-14 (the
ADR-footnote-fork backlog row — one restatement away from becoming a
third D7-7-class miss; the next session touching the Next section
should re-cite it). D8-8 (`make quickstart`'s own full timing —
this review's own probe killed it at ~10 minutes rather than let it
run past the prior review's ~13-minute baseline; re-run in isolation
with a >15-minute budget for a clean reading).

#### This session's own successor tag debt

The next session that opens fresh work tags
`stable-20260809-repo-review-2` at THIS session's own closing tip,
under standing ceremony — the tag-law case (ii) pattern every prior
close in this repo has used for its own predecessor.

### Verification

- `clojure -M:poly check`: OK, Step 0.
- Oracle pre-digest (`bin/regression-oracle 451d159 451d159`): all
  THIRTY-FOUR roots confirmed IDENTICAL, soundness "yes outside ns
  form" — the expected trivial result of a tip-against-itself bracket,
  and an independent re-confirmation of the 34-root count D1-4 re-
  derives from the digest source directly.
- Cold-cache fresh-clone full suite (D3-1, this review's own headline
  probe, not a routine preflight re-run): a genuine fresh `git clone`
  with `HOME` repointed to an empty temp directory (no `~/.m2`,
  `~/.gitlibs`, `~/.deps.clj` — confirmed absent before the run) and
  the artifact cache directory repointed likewise. `poly check`: OK.
  `poly test :all skip:integration`: 293 namespace blocks, 14183
  passes, **0 failures, 0 errors, anywhere**, test-phase wall-clock
  5m24s from cold. This IS this session's own full-suite baseline —
  no separate warm-cache preflight run was needed or performed, since
  the cold run is strictly the stronger claim.
- All CI lanes' latest conclusions disclosed at Step 0 (not just the
  push lane — the lane-visibility lesson from H-4/D2-18 applied to
  this session's own ceremony first): `test` lane green at tip
  `451d159` (and the prior ~30 pushes); `Integration` lane's last two
  runs both green (`31312272026` post-`c690ec3` fix, and the routine
  scheduled run since) — no open red on either lane at Step 0.
- `gitleaks git --staged -v`: clean at every commit this session; the
  pre-push hook ran it again on every push, clean throughout.
- Post-push message verification: every commit's pushed message
  diffed against its own message file.
- Tag verification: `stable-20260809-storefront-fixture` peeled ref
  resolves to `451d1591a04522838d91e02beab9acdccbf444d8` exactly
  (`git ls-remote --tags origin`).
- `git status --porcelain`: clean before this session's first tool
  call, clean after every background probe agent's own live-execution
  work (D8's `make quickstart`/`make integration`/`play` runs
  included), clean at Step 3's own commit boundary — confirmed
  directly, not assumed, at each of these points.

### Fences

Everything AR-RR2-1 through AR-RR2-5 name, held: no src/test/gate
change, no doc-content fix, no roadmap Deferred/Next content edit
(the Done pointer below is ceremony, not content), no law append, no
`state.md` regeneration. Every fix-shaped temptation this session's
own probes surfaced — the CLI parse-guard cluster, the classpath
static gate, the roadmap-anchor policy, the front-door honesty gap —
became a register row and a plan-draft item, never fixed in passing.
The register and this ADR's own plan draft are PROPOSALS; the author
rules on them in the design channel after this lands.

### Index line

```
- 2026-08-09 — repo-review-2 — ADR-0092
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 89->90, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated unchanged

This session was the arc's own survey, not a fix session — everything
named in the plan draft above (the two fix-session clusters, the six
rulings-needed, the intake items) awaits the author's own rulings
before any fix session runs. Untouched, carried forward unchanged from
ADR-0090/0091: vendoring's own remaining backlog (the two deferred
veteran modules under their true diagnosed names), the fixture-
relocation backlog row (actively growing, healthy), publish-prep
Externals (7 closes, correctly parked).

**What DOES change:** after design-channel verification of this
session's own landing (the register's row-level evidence sampled and
re-derived independently, this ADR's own scoreboard cross-checked
against the register), the author rules on the plan draft above, and
the ruled fix sessions follow under the standing arc pattern, closing
with a scored delta the same way ADR-0080 closed review 1's own arc.

### Consequence

The repo's second periodic quality review is landed: eight dimensions,
every probe recorded (57 of 76 rows clean, including the clean ones
per the skill's own "a green probe is inheritance, not noise"
instruction), the prior review's own summary arithmetic independently
re-derived and confirmed exact, the ten named-minimum window incidents
swept and dimension-classified. The repo's single most severe
finding across both review cycles — a repo-wide, demonstrated-live
silent-success I/O pattern — is confirmed genuinely closed, not merely
claimed closed, by running the gate and re-deriving the grep rather
than trusting the fix session's own account. Set against that win: two
dimensions clean at review 1 each show a real, evidenced regression
this cycle, both tracing to the same underlying shape — a fix or gate
that closed the specific trigger a prior probe happened to hit while
leaving a structurally adjacent, more general trigger of the identical
class open. Nothing moved beyond the register, this ADR, and ceremony:
the plan draft awaits the author's own rulings before any fix session
runs.
