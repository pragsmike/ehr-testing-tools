## ADR-0086 — The straddle fix: the legacy gate learns the span

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: `notes/adr/0085-colorectal-investigation.md` diagnosed
`colorectal_cancer.json`'s own `:clinical-content-only-when-admitted`/
`:discharge-follows-admission` violations to `ehrt.sim-trajectory.compile-
trajectory/compile-trajectory`'s own LEGACY (`history?` false) pre-horizon
drop clauses: they test only an event's own raw `:pre-horizon` flag, with
no back-reference check against the encounter it belongs to. A straddling
encounter — opened pre-horizon (dropped), closed and/or containing
clinical content post-horizon (compiled normally) — produces compiled
clinical-content and terminal-discharge steps with no matching compiled
admission step. Two proposed fix shapes were named, not built (ADR-0085
Step 1's own fence): (a) synthesize a compiled opening step at the
horizon boundary for the straddling case, or (b) generalize the Wave H
`history-phase?` back-reference principle to the legacy path.

The author ruled the arm 2026-08-08, design channel, verbatim: **"Accept
recommendation: (b) now, (a) recorded."** This session executes shape
(b) and records shape (a) as the carry-across row's own compile-layer
half (AR-SF-5). This is the first compile-layer semantics change since
Wave H — ADR-0082's own blast-radius protocol (predict every oracle
root, STOP-AND-REPORT any mover, bracket confirming the prediction
exactly) applies in full.

Read-first (this session): `notes/adr/0085-colorectal-investigation.md`;
`notes/adr/0082-encounterend-fix.md` (the blast-radius protocol as
executed once before, the mover discipline, the seed-42 prose figure
AR-SF-6 corrects); `compile_trajectory.clj`'s legacy drop clauses,
`pre-horizon-dropped-types`, `pre-horizon-fact-types`, `history-phase?`;
`components/oracle/src/ehrt/oracle/digest.clj`'s own 28 roots.

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]`
channel-inferred.

1. **AR-SF-0** `[A — tag law, case (ii)]`. `stable-20260808-colorectal-
   investigation` annotated and pushed at `b81b847` (ADR-0085's own
   closing tip, confirmed HEAD at session start, clean tree). Tag did
   not already exist locally or on the remote; created fresh.
   **Executed Step 0.**

2. **AR-SF-1** `[A, ruled 2026-08-08, verbatim above]`. Shape (b)
   implemented: a `straddle-open?` fold state (the compile-time mirror
   of `gmf-interpreter/mark-phase`'s own `open-phase` — the SAME "one
   in-flight span, encounters never nest in this project's own GMF
   subset" invariant) opens the moment a raw-pre-horizon `:encounter` is
   dropped; every subsequent event — regardless of its OWN raw
   `:pre-horizon` — receives the EXISTING pre-horizon disposition
   (`pre-horizon-dropped-types` drops, `pre-horizon-fact-types` becomes
   a registration fact) until the matching `:encounter-end` closes the
   span. **In-span membership attribution, the session's own design
   work within the arm:** an open-straddle interval (a single boolean),
   not the `:references` back-edge — because most in-span event types
   (`:procedure`, `:observation`, `:supply-list`, `:care-plan-start`,
   ...) carry no `:references` back to the encounter at all (only
   `:encounter-end`/`:medication-end`/`:care-plan-end`/`:condition-end`
   ever do), an interval state is the only attribution that covers every
   member type uniformly. This exactly mirrors `mark-phase`'s own
   `open-phase` mechanism (already ratified, already solving the
   identical problem for `history?` true) at compile time instead of
   interpreter time — not a new design, a generalization of an existing
   one to a second layer.

3. **AR-SF-2** `[A — standing rule, fidelity arc, blast-radius protocol
   in full]`. Every oracle root's own seed/population was walked
   read-only (raw trajectories captured via `with-redefs` on
   `ehrt.sim-trajectory.interface/run-module`, the exact boundary
   ADR-0085's own probe used, zero working-tree disturbance) and scanned
   for straddling spans (an `:encounter` with raw `:pre-horizon` true
   whose own `:encounter-end`, resolved via `:references`, has raw
   `:pre-horizon` false) BEFORE any src edit. Classification (28 roots,
   read from `digest.clj`'s own `roots` map):
   - **Not-in-path (3):** `appendicitis`/`sore-throat`/`ear-infections`
     — interpreter-batch roots, call `run-module` only, never
     `engine/run`, so `compile-trajectory` is never reached.
   - **In-path, `history? true` (3):** `urinary-tract-infections-
     history-engine`/`ear-infections-history-engine`/`attention-
     deficit-disorder` — gated identical BY CONSTRUCTION (the fix only
     touches the `(and (not history?) ...)` legacy branch).
   - **In-path, legacy (22):** the roots the fix can actually change.

   **The prediction table (legacy roots, walks/spans at each root's own
   seed/population):**

   | root | walks | spans | predicted |
   |---|---|---|---|
   | sinusitis | 30 | 0 | identical |
   | death-fixture | 200 | 0 | identical |
   | sepsis | 500 | 0 | identical |
   | ear-infections-engine | 300 | 0 | identical |
   | urinary-tract-infections-engine | 300 | 0 | identical |
   | total-joint-replacement-engine | 300 | 0 | identical |
   | asthma | 300 | 0 | identical |
   | bronchitis | 300 | 0 | identical |
   | **sleep-apnea** | 300 | **3** | **MOVER** |
   | fibromyalgia | 300 | 0 | identical |
   | dementia | 300 | 0 | identical |
   | hypothyroidism | 300 | 0 | identical |
   | rheumatoid-arthritis | 300 | 0 | identical |
   | osteoarthritis | 300 | 0 | identical |
   | osteoporosis | 300 | 0 | identical |
   | allergic-rhinitis | 3000 | 0 | identical |
   | dermatitis | 300 | 0 | identical |
   | metabolic-syndrome-care | 300 | 0 | identical |
   | vhd-pulmonic | 300 | 0 | identical |
   | vhd-tricuspid | 300 | 0 | identical |
   | med-rec | 300 | 0 | identical |
   | anemia | 300 | 0 | identical |

   One mover predicted: `sleep-apnea`, 3 of 300 walks (#17, #58, #269),
   identical shape all three — a `:wellness` encounter opening just
   before the registration horizon, minting a pre-horizon `Sleep_Apnea_
   Assessment` procedure, closing just after. **Session STOPPED here,
   before any fix code, per AR-SF-2's own bar.** Full evidence below
   (Step 1).

4. **AR-SF-3** `[C — acceptance bar]`. Met: `colorectal_cancer.json` at
   300 patients, `:status :ok` (0 violations) at all three seeds
   (20260802, 1, 42 — the multi-seed-once-flagged law's own set, pin
   `/home/mg/synthea-checkout` at `7e08387c68a7f0e21d13076609a159fd473
   fc902` verified first, `git rev-parse HEAD` direct). Co-landed tests:
   six new unit tests on minimal synthetic straddle trajectories,
   written RED against the unfixed tree, GREEN post-fix (see Step 2).

5. **AR-SF-4** `[C — history-mode scope probe]`. **SOUND, no gap** —
   history mode already handles the straddle via `mark-phase`'s own
   AR-2 encounter-anchored phase inheritance, confirmed both by reading
   the mechanism and by an empirical probe. See Step 1's own history-
   mode section.

6. **AR-SF-5** `[A, ruled 2026-08-08: "(a) recorded"]`. Dated note
   appended to the roadmap's own Carry-across emission Deferred row
   (below), recording shape (a) — synthesize a compiled opening at the
   horizon boundary — as that row's own compile-layer half, citing this
   ADR's own straddle-detection machinery as the shared prerequisite.
   Row stays deferred, trigger unchanged.

7. **AR-SF-6** `[C — erratum rider, fix-forward law]`. Dated erratum
   appended to `notes/adr/0082-encounterend-fix.md`, correcting its
   seed-42 prose figure. See Step 3, below.

8. **AR-SF-7** `[C — suppression visibility, the R2 precedent]`.
   **Landed.** `:suppressed-straddle-spans` (spans, not events) is a
   purely additive key on `compile-trajectory`'s existing return map.
   Every caller was confirmed `:keys`-selective before landing it
   (`engine.clj`'s own `decide :registered`, every test file's own
   destructure) — zero friction found, the R2 precedent (`:suppressed-
   encounter-ends`) applies cleanly.

### Step 1 — Blast radius, evidenced

**The mover, walk-level (raw trajectory, all three walks identical
shape):**

```
idx 118 :encounter    t=20077 pre-horizon=true  class=:wellness references=nil
idx 119 :procedure    t=20077 pre-horizon=true  codes=[sleep apnea assessment]
idx 120 :supply-list  t=20098 pre-horizon=false
idx 121 :encounter-end t=20098 pre-horizon=false references=118
```

(walk #17: idx 118-121 as shown; walk #58: idx 180-183, t=20080/20102;
walk #269: idx 182-185, t=20083/20112 — same shape, same six-event
window, shifted only by seed-specific timing.)

**History-mode soundness (AR-SF-4):** traced `mark-phase`
(`gmf_interpreter.clj` ~2032-2043): an `:encounter` event's `open-phase`
seeds from its own raw phase (`:history`, since pre-horizon); every
subsequent event — INCLUDING the `:encounter-end` — inherits that phase
regardless of its own raw timestamp, until the matching `:encounter-end`
clears it. A straddling span therefore already reads `:phase :history`
in full, dropping uniformly under `history-phase?` — exactly shape (b),
already built, for the `history?` true path. Empirical probe (all three
`history? true` oracle roots, `urinary-tract-infections-history-engine`'s
own real straddling span included): **0 unsound walks** — every event in
every straddling span read `:phase :history`, no counterexample. Fix
stays legacy-path-only per the fence.

### STOP-AND-REPORT and license (AR-SF-2)

The session reported the prediction table and the mover's evidence,
then stopped and awaited a license before any src edit. The author's
license, via the design channel, verbatim:

> LICENSE (AR-SF-2, ruled by the author via the design channel,
> 2026-08-08): **`sleep-apnea` is licensed as this session's SOLE named
> mover.** Proceed to Step 2 under the following terms, which bind the
> bracket and the record:
>
> 1. The post-change bracket runs `--declared-digest-change` naming
>    `sleep-apnea` alone. It must show EXACTLY 27 roots IDENTICAL and
>    `sleep-apnea` DIFFERENT, at exactly the 3-of-300-walk granularity
>    predicted (#17, #58, #269). Any other root moving, or sleep-apnea
>    NOT moving, or a different walk set, is a fresh STOP-AND-REPORT —
>    the license does not stretch.
> 2. ADR-0086 carries the mover's walk-level evidence both sides: the
>    three walks' compiled tails pre-fix (the dangling terminal) and
>    post-fix (dropped), plus hand-verified sleep-apnea digests
>    pre/post, independent of the harness — ADR-0082's own mover
>    discipline.
> 3. ADR-0086 states explicitly that sleep-apnea's post-fix digest is
>    the new standing baseline, and why it is MORE correct than the one
>    it replaces.
> 4. The latent-defect finding is recorded as INTAKE, not acted: a
>    vendored oracle root shipped a malformed compiled shape (dangling
>    terminal) invisible to byte-identity; named for review 2 and the
>    pairing-as-data adequacy conversation. The probe's incidental
>    full-sweep result (27 roots straddle-free) is recorded alongside
>    it.
>
> All other rulings, fences, and steps unchanged.

An independent sandbox verification (design channel, prior to the
license) re-derived the tag/HEAD, the 28-root classification (including
correcting its own earlier partial read that had miscounted `attention-
deficit-disorder` as legacy — it is `:history true`, `digest.clj` line
375), the `:supply-list` unconditional-log-only claim, the AR-SF-4
structural trace, and confirmed `sleep_apnea.json`'s own vendored source
structurally contains the exact shape the walk evidence shows (a
wellness-class `Encounter`, assessment `Procedure`s, an `End Wellness
Visit` `EncounterEnd`) — disclosing that the walk-level 3-of-300 COUNT
itself could not be re-run in that sandbox (no Maven Central access),
verified structurally and by the session's own transcribed probe
instead.

### Step 2 — The fix, evidenced

**Red, witnessed in-session (`clojure -M:dev:test`, `ehrt.sim-
trajectory.compile-trajectory-test`):** six new unit tests against the
unfixed tree — `legacy-straddling-encounter-emits-nothing-and-nothing-
orphaned`, `legacy-straddling-encounter-in-span-facts-still-become-
registration-facts`, `legacy-post-straddle-horizon-encounter-still-
compiles-normally`, `legacy-non-straddling-fully-pre-horizon-encounter-
unaffected`, `suppressed-straddle-spans-counts-spans-not-events` — 7
failures (0 errors) against the unfixed tree, exactly the new
assertions, nothing pre-existing disturbed.

**Green, post-fix:** `Ran 36 tests containing 70 assertions. 0
failures, 0 errors.` (up from 63 assertions pre-session, the +7 this
session's own new coverage).

**Colorectal acceptance (AR-SF-3):** `check/check-all` at 300 patients,
`engine/run` + the closure loaded exactly as ADR-0085's own probe did
(pin-verified checkout, `anemia/anemia_sub` resolved via a filesystem
`resolve-call-path`, no `:persona-config` override):

| seed | pre-fix violations | post-fix |
|---|---|---|
| 20260802 | 4 (3 clinical-content, 1 discharge) | `:status :ok` — **0** |
| 1 | 0 | `:status :ok` — **0** |
| 42 | 4 | `:status :ok` — **0** |

Fully extinguished at all three seeds.

**Full suite** (`clojure -M:poly test :all skip:integration`): every
project block `0 failures, 0 errors`; `ehrt.sim-trajectory.compile-
trajectory-test` (appearing twice across project groupings, standard for
this workspace): `Ran 36 tests containing 70 assertions` both times. The
disclosed `mutate-stdout-stdin-real-loopback-test` flake did NOT fire.

**Oracle bracket** (`bin/regression-oracle b81b847 e2cef25
--declared-digest-change`): manifest diff confined to EXACTLY one line —
`sleep-apnea.edn`: `a68c4fb7...` → `271df527...` — all 27 other roots
byte-identical, matching the license's own bar exactly.

**Walk-level evidence, both sides (license term 2), via a disposable
`git worktree` at `b81b847` (pre-fix) compared against the current tree
(post-fix), intercepting `compile-trajectory` itself to capture
per-patient compiled `:steps` for all 300 `sleep-apnea` walks:**

Exactly 3 of 300 walks differ — #17, #58, #269, the SAME three the
prediction named, no surprise mover or surprise-identical.

- **Walk #17, pre-fix (the dangling terminal, the defect):**
  `[{:type :delay :from 12960 :to 12960} {:type :outpatient-visit-end
  :citation {:module sleep-apnea :state :end-wellness-visit}}]` — a
  checkout with no checkin.
- **Walk #17, post-fix (the correction):**
  `[{:type :delay :from 508320 ...} {:type :outpatient-visit ...}
  {:type :procedure :codes [sleep apnea assessment] ...} {:type :delay
  ...} {:type :outpatient-visit-end ...}]` — the straddling span drops
  in FULL (no phantom checkout), and the loop finds the genuinely LATER,
  fully-in-horizon wellness encounter (~353 days later) and compiles it
  normally — a real, complete admission-to-discharge pair replaces the
  phantom.
- **Walk #58/#269:** same shape (dangling terminal → dropped span, real
  later encounter compiles); walk #58's later encounter carries no
  procedure (compiles to exactly `:outpatient-visit`/`:outpatient-
  visit-end`), walk #269's carries one, matching each walk's own actual
  content.

**License term 3 — the new baseline is more correct:** the pre-fix
digest (`a68c4fb7...`) enshrined a compiled `:outpatient-visit-end` step
with no preceding `:admission`/`:outpatient-visit` — a wire-impossible
shape (a discharge summary for a stay that, per this run's own compiled
ground truth, never began). The post-fix digest (`271df527...`) replaces
it with either nothing (walk-level: the straddling span drops whole,
clinically correct — the patient's actual first FULLY OBSERVED wellness
visit is later) or a real, complete encounter pair once the genuinely
later horizon-phase encounter is reached. `271df527...` is licensed here
as the new standing oracle baseline for `sleep-apnea`.

**Hand-verified digests, independent of the harness (license term 2):**
`sleep-apnea-pair` called directly in a scratch session (not through
`bin/regression-oracle`), `pr-str`'d to a file, `sha256sum`'d
independently: post-fix `271df527a5989e69366eaf1ac9f7d71fa3f61527941e6
23475682ab49b147d5f` — matches the oracle's own reported target digest
exactly.

**AR-SF-7 disposition:** landed. `:suppressed-straddle-spans` counts
spans, not events (a synthetic test with 2 in-span observation events
inside one span asserts count `1`, not `2`); a fully-pre-horizon,
non-straddling span (open AND close both raw pre-horizon) is explicitly
NOT counted (its own unit test asserts `0`) — this is the "existing
disposition," unchanged, not a new leniency.

**Intake, per license term 4:** `sleep-apnea.json`'s vendored oracle
baseline shipped a malformed compiled shape (a dangling `:outpatient-
visit-end` with no matching admission) since vendoring batch 1
(ADR-0070) — invisible to byte-identity oracle checks because no oracle
root runs `check/check-all`'s own invariant catalog, only raw digest
comparison. This session's own blast-radius sweep is the first time any
of the 28 oracle roots' own straddle-freedom was actually checked; the
other 27 came back straddle-free (a stronger audit than any root has
previously received). Both facts — the latent-defect class and the
now-audited-clean 27 — are named here for review 2 and the pairing-as-
data adequacy conversation, not acted on further this session.

### Confirmation

- `clojure -M:poly check`: OK, Step 0 and before the commit.
- Oracle pre-digest (Step 0): `bin/regression-oracle b81b847 b81b847` —
  IDENTICAL, all 28 roots, byte-for-byte (self-bracket).
- Last five CI runs on `main` at session start: all `success` —
  `31269790361` (`b81b847`), `31269357505`, `31266927895`,
  `31266367045`, `31263297709`. No red window.
- `gitleaks git --staged -v`: clean, both commits this session.
- Commit `e2cef25` ("fix: straddling encounters drop whole — the legacy
  gate learns the span (straddle fix, AR-SF-1/2/3)"). Pushed; post-push
  verification (`git log --format=%B -1` diffed against the source
  message file): one delta, the known trailing-blank-line artifact. CI
  watched to conclusion: run `31273576426`, `success`, 3m24s.

### Successor tag debt, recorded here

The next session that opens fresh work tags `stable-20260808-straddle-
fix` at THIS session's own closing tip — the same tag-law case (ii)
pattern every prior close in this repo has used for its own
predecessor.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The straddle fix: the legacy gate learns the span — the blast-radius probe finds one real mover, `sleep-apnea` (3 of 300 walks, a dangling `:outpatient-visit-end` already shipped since vendoring batch 1), STOP-AND-REPORTs, and is licensed by name; shape (b) lands (a compile-time mirror of `mark-phase`'s own straddle inheritance, generalized to the legacy path), red-witnessed then green, `colorectal_cancer.json` clean at all three seeds, the oracle bracket matches the prediction exactly, `sleep-apnea`'s post-fix digest licensed as the new baseline (more correct, not merely different); an ADR-0082 erratum corrects a self-contradicting seed-42 prose figure, and the carry-across row gains shape (a)'s own compile-layer note
