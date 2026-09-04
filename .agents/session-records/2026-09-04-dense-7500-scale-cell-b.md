# 2026-09-04 -- dense-7500 (b): the skeleton corrected, all four cells measured, the Scale table rewritten

The session before this one committed the dense-7500 scenario and then
stopped: `config-bare.edn` at 7,500 arrivals died on
`:capacity-exhausted` after ~2,963 arrivals, so the Scale table's third
row had no re-measurement and was left whole rather than half-rewritten.
The design channel ruled option (b) -- correct the skeleton. This
session does that, re-measures every cell, and rewrites the table. Base
`e1baf4d`; ceremony R30 (commit and push at each checkpoint,
unattended), taken from the prompt. No sub-agents.

**All eight timed runs exit 0**, including both runs of the cell that
was BLOCKED, so nothing in this session stopped short.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Last five CI runs
on main all green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`; working tree clean including untracked; local HEAD matched
`origin/main`. One disclosure, and it is the correct state: HEAD is not
tagged `stable-*` -- no tag is paid.

## 1. THE DERIVATION -- Little's law, one ward at a time

The prompt asked for the dwell bound before any edit, and the arithmetic
turned out to name the defect exactly rather than merely bound it.

`:arrival-gap 2` feeds `rand-int-in world-rng 0 2` (`sim_engine/run.clj`
:239), a uniform integer 0-2 minutes, so **lambda = 1.00 arrivals per
minute**. The module cohort claims every eighth ordinal (12.5%) and the
remaining 87.5% splits by weight 45 / 35 / 20, giving per-pathway
arrival rates of **0.394 / 0.306 / 0.175 per minute**. A `:delay` is
`rand-int-in rng from to` in minutes (`decide.clj` :1020), so a
pathway's mean dwell on a ward is the sum of the `(from + to) / 2` it
spends there. Census is rate times dwell.

| ward | beds + surge | fed by | dwell before | census before | dwell now | census now |
|---|---|---|---|---|---|---|
| Emergency | 180 + 40 = 220 | all three, pre-transfer | 142.5 / 247.5 / 60 | 142.4 | 50 / 247.5 / 60 | 106.0 |
| Medicine A | 200 + 40 = 240 | `dense-inpatient` | 1,020 | **401.6** | 255 | **100.4** |
| Surgery | 160 + 40 = 200 | `dense-surgical` | 990 | 173.3 | 990 | 173.3 |
| Medicine B | 200 + 40 = 240 | overflow only | -- | -- | -- | -- |

**Medicine A at 401.6 against a 240-bed ward is the whole defect** --
167% of the ward before a single overflow. It also explains the shape of
the refusal payload, which named *Surgery* and not Medicine A:
`ehrt.sim-model.facility/allocate`'s ladder is home licensed, home
surge, **other inpatient LICENSED** (never their surge), then ED surge.
So a Medicine A overrun spills into Medicine B's and Surgery's licensed
beds until Surgery's own traffic has nowhere left, which is exactly the
`Surgery 200/200, Medicine A 240/240, Medicine B 200/240` census the
blocked run reported -- Medicine B showing 200 of 240 because its 40
surge slots are unreachable from another ward's overflow.

`dense-inpatient`'s five delays were shortened 30-90 / 45-120 / 120-480
/ 240-720 / 120-360 **to** 10-30 / 15-45 / 30-120 / 60-180 / 30-90:
an ED dwell of 50 minutes and a Medicine A dwell of 255. Ward sizes did
not move (R-b). Nothing else in the file changed except comments.

**The bound is a mean-value bound and the record says so.** It carries
no variance term, and no census at all for the module cohort, whose walk
length belongs to the vendored modules. It is what SIZED the delays; the
run is what gated them.

## 2. C1 -- the corrected skeleton (`826294e`)

Both derivation rules were first checked against the PREVIOUS committed
siblings, reproducing them byte for byte, before the correction was
applied -- so the rules are known to be the rules that produced what was
already in the tree:

    sed -n '1,/^ :module-horizon-days /p' config.edn > /tmp/bare.check
    printf '}\n' >> /tmp/bare.check ; cmp /tmp/bare.check config-bare.edn   -> BARE_RULE_OK
    grep -v '^ :bed-cycle true$' config.edn > /tmp/nobed.check
    cmp /tmp/nobed.check config-nobed.edn                                   -> NOBED_RULE_OK

Both are now stated in the README **as commands** rather than only as
checks, which is what the prompt asked for: a reader edits `config.edn`
and re-runs two lines.

**Gate, both halves.**

| half | result |
|---|---|
| one-key derivation | `diff` is exactly one removed line and zero added -- `2113d2112`, ` :bed-cycle true` |
| additive prefix | `config-bare.edn` less its `}\n` is `config.edn`'s first **154,196** bytes, sha256 `5c3d7659...` on both sides |
| capacity | `bin/ehrt sim run --patients 7500 --config config-bare.edn --format ground-truth` **exit 0, 2:14.91, peak RSS 1,620 MB** |

The prompt predicted the failure mode would show in ~42 s if the
correction had not worked. It did not show.

**One thing was corrected beyond the delays, because the new census
table sits beside it.** The `:wards` block claimed the steady-state
census never reaches the surge rungs. It does: Surgery's 173.3 is above
its 160 licensed beds, so `dense-surgical` uses that ward's surge
routinely. The claim is narrowed to the ladder as a whole -- 273.7
inpatient census against 680 reachable inpatient beds -- which is what
the sizing actually buys.

## 3. Step 2 -- the failing run, the counts, the warm-up

`bin/demo-exerciser-dense-7500`, run once against the corrected
scenario with the README's old cells still in place, **failed exactly
where the prompt said it would and nowhere else**:

    == checking named derivations ==
      one-key derivation: config-nobed.edn is config.edn less exactly ':bed-cycle true'
      additive prefix: config-bare.edn's own 154196 bytes ARE config.edn's prefix, sha256 5c3d7659...
      ground-truth path: out/scenarios/dense-7500-750.edn is a bare top-level vector, 12282999 bytes
    == checking the README's own witnessed figures ==
    FAIL: witnessed figures: the 7,500 cell spooled 222748 messages, the
    README witnesses 224645 -- seed 20260824 determinism did not reproduce

Full run wallclock 338 s. `fail` exits at the first failure, so the
message count is the only figure that run PRINTED; the two event counts
were taken from the artefacts it had already written
(`out/scenarios/dense-7500/events.edn` 167,190,
`out/scenarios/dense-7500-750.edn` 33,303), one JVM, after the run.
Those three were written into the README's 7,500 and 750 rows.

**The warm-up re-run, and a disclosure about its exit code.** The
re-run's three named derivations and **all three witnessed-figure checks
passed**:

      witnessed figures: 222748 spooled messages match the README's own re-derived claim
      witnessed figures: the 7,500 corpus holds 167190 events, as the README witnesses
      witnessed figures: the 750 ground-truth vector holds 33303 events, as the README witnesses

and then it **exited 1** on its own last assertion, ADR-0005's
tree-clean postcondition, naming ` M demos/scenarios/dense-7500/README.md`
-- the uncommitted README edit the step had just instructed, which is
the very edit those three checks had validated. The step forbids a
commit, so there is no ordering in which that assertion could have held.
It is taken as the warm-up on the substance (it did the full 7,500
generate and the 750 run, which is what a warm-up is for) and the
exit-1 is disclosed here rather than described as a pass. **Section 7
records the clean exit-0 run of the same script after C2 landed.**

## 4. Step 2 -- the eight timed cells

R-measure exactly: warm-up (above), then two timed runs per cell, one
JVM per run, a FRESH spool target per run, seed 20260824 throughout,
`/usr/bin/time -v` around each, cells strictly sequential and never
overlapping. The driver is scratch and is NOT promoted to `bin/`.

**Every run exits 0.** The wall reported is the whole `bin/ehrt corpus
generate sim` PROCESS, JVM startup included.

### 4a. Per-run appendix

| cell | config | arrivals | run | exit | process wall | peak RSS | events | messages |
|---|---|---|---|---|---|---|---|---|
| 1 | `config.edn` | 7,500 | 1 | 0 | 278.14 s | 2,178.0 MB | 167,190 | 222,748 |
| 1 | `config.edn` | 7,500 | 2 | 0 | 284.78 s | 2,240.3 MB | 167,190 | 222,748 |
| 2 | `config-nobed.edn` | 7,500 | 1 | 0 | 226.94 s | 1,802.6 MB | 125,825 | 164,217 |
| 2 | `config-nobed.edn` | 7,500 | 2 | 0 | 225.57 s | 1,928.5 MB | 125,825 | 164,217 |
| 3 | `config-bare.edn` | 7,500 | 1 | 0 | 145.68 s | 1,896.9 MB | 100,884 | 65,239 |
| 3 | `config-bare.edn` | 7,500 | 2 | 0 | 142.90 s | 1,737.4 MB | 100,884 | 65,239 |
| 4 | `config.edn` | 750 | 1 | 0 | 54.25 s | 1,229.5 MB | 33,303 | 40,281 |
| 4 | `config.edn` | 750 | 2 | 0 | 53.44 s | 1,161.7 MB | 33,303 | 40,281 |

**Both runs of every cell produced the identical event and message
counts**, and cell 1's counts also match the two exerciser runs that
preceded them -- four independent JVMs on the 7,500 all-keys cell,
167,190 / 222,748 every time.

Host, sampled at every one of the eight cell boundaries: up 10 days
~17 h, 14.28-14.45 GiB free throughout, one-minute load average between
0.54 and 1.83, all of it this session's own runs.

### 4b. The means, which are what the docs carry

| cell | arrivals | events | messages | msg/event | process wall | peak RSS |
|---|---|---|---|---|---|---|
| `config.edn` | 7,500 | 167,190 | 222,748 | **1.3323** | 281.46 s | 2,209 MB |
| `config-nobed.edn` | 7,500 | 125,825 | 164,217 | **1.3051** | 226.25 s | 1,866 MB |
| `config-bare.edn` | 7,500 | 100,884 | 65,239 | **0.6467** | 144.29 s | 1,817 MB |
| `config.edn` | 750 | 33,303 | 40,281 | **1.2095** | 53.84 s | 1,196 MB |

**THE BARE CELL IS THE POINT OF THE SESSION.** 100,884 events, 65,239
messages, exit 0, in 144.29 s. Its msg/event of **0.6467** lands within
0.6% of the 2026-08-29 `old` continuity series' **0.643** on a
configuration that no longer exists -- an independent corroboration
that the corrected skeleton is still the same shape, arriving from a
direction this session did not arrange.

Three derived figures moved with it, and one claim died:

- **The bed cycle is 41,365 events, 24.7% of the corpus**, and 58,531
  messages with them.
- **The cycle is message-RICHER than the log it rides on.** 1.3323 with
  it against 1.3051 without, so its own events carry 1.4150 messages
  each. **The previous generation's "unmoved to four places" claim
  (1.3509 vs 1.3510) is DEAD** -- it was a property of the old dwells,
  not of the bed cycle, and it is replaced rather than quietly dropped.
- **Messages per event still climbs**, 1.2095 at 750 to 1.3323 at 7,500,
  and the README's warning that the pair is a direction and not a decade
  is unchanged and still applies.

## 5. Step 4 -- the referential matrix, re-run over the corrected cell

Per the ledger's own section-7 recipe, over `config.edn`'s 7,500-arrival
log at the corrected skeleton. The two SHIPPED columns were counted
through the live catalog's own `:candidate-sites` predicates; the three
population-gap columns through predicates RECONSTRUCTED from
`operators.clj`'s `resolving-sites` -- carrier, integer field, index in
range. Reconstructed, and said so. The probe is scratch, re-authored
from the ledger rather than recovered, and NOT promoted to `bin/`.

| col | field | carrier | carriers | resolving sites |
|---|---|---|---|---|
| **A** | `:cancels-event-id` | the three cancels | 2,230 | **2,230** |
| B1 | `:order-event-id` | `:result-available` | 10,253 | **10,253** |
| **B2** | `:order-event-id` | `:medication-end` | 4,885 | **4,884** |
| **C** | `:start-event-id` | `:care-plan-end` | 3,486 | **3,486** |
| D | `:placeholder-event-id` | `:identity-fill` | 943 | **943** |

Column A, per kind: `:cancel-admit` 54, `:cancel-transfer` 2,125,
`:cancel-discharge` 51.

**All five columns are still populated after the correction**, which is
the thing that had to be re-established: three of them were population
gaps until this scenario landed, and a shorter dwell could have starved
any of them. The cross-check is exact on both shipped columns -- B1
10,253 and D 943 by BOTH routes, across all four (B1) and all five (D)
applicable shapes. B2's one-site gap is the same benign shape the
previous measurement found: one `:medication-end` whose
`:order-event-id` is nil, which `resolving-sites` correctly excludes.

**Report only.** The ledger is not amended and no operator is
registered; ledger section 6 is untouched, per the fence.

## 6. C2 -- the Scale table, the README, the exerciser note

`docs/consuming-ground-truth.md#scale`'s three rows are rewritten to the
means above and now cite `demos/scenarios/dense-7500/`, with the column
renamed **process wall** and a paragraph saying what that means: the
whole `corpus generate` process under `/usr/bin/time -v`, not an
in-process phase total, and therefore not comparable line-for-line with
the in-process figures below it. The method pointer above the table was
repointed at the scenario README; the 2026-08-24 programme's appendix is
still cited, for the paragraphs that are still its.

**The three paragraphs below the table are LABELLED, not re-measured**
-- the 2026-08-29 programme's own measurement on a configuration that no
longer exists, kept for the decade-over-decade direction each reports.
Their absolute figures do not reconcile with the table above and the
label says so in the document rather than only here.

**One paragraph was added where `--patients` is explained, and it does
not say what the prompt asked it to say.** See section 8.

The scenario README carries the four-cell table, both walls and both RSS
columns, and the re-measured referential matrix. `bin/demo-exerciser-
dense-7500`'s own "what is not checked here" note was corrected: it said
`config-bare.edn` has no row because it cannot produce one, and both of
those halves are now false.

**Gate: full `make test`.** **MAKE_EXIT=0**, 4,815 tests, 25,700
assertions, 0 failures, 0 errors, over every project. The run went to a
log with `$?` captured explicitly and the wrapper ended in `exit
"$MAKE_EXIT"` -- ADR-0152's own mask, not repeated here.

## 7. Verification after C2

`bin/demo-exerciser-dense-7500`, run a third time with the tree clean at
`963902d` (the roadmap row of section 9 was `git stash`-parked for the
duration so it could not trip the postcondition again, and popped back
afterwards):

    == full run wallclock 333s ==
    == checking named derivations ==
      one-key derivation: config-nobed.edn is config.edn less exactly ':bed-cycle true'
      additive prefix: config-bare.edn's own 154196 bytes ARE config.edn's prefix, sha256 5c3d7659...
      ground-truth path: out/scenarios/dense-7500-750.edn is a bare top-level vector, 12282999 bytes
    == checking the README's own witnessed figures ==
      witnessed figures: 222748 spooled messages match the README's own re-derived claim
      witnessed figures: the 7,500 corpus holds 167190 events, as the README witnesses
      witnessed figures: the 750 ground-truth vector holds 33303 events, as the README witnesses
    == every command asserted, every named derivation held, tree clean ==

**EX3_EXIT=0.** That is the clean exit the warm-up could not have, and
it is the run this record rests the "exercised as documented" claim on.

`make test` was then run a SECOND time, after this record, the prompt
archive, the regenerated indexes and the roadmap row were all in the
tree, because a tree-scanning gate lives in a brick none of those
belong to (`rulings.md#R-full-suite-before-push`). **MAKE_EXIT=0**, the
identical 4,815 tests and 25,700 assertions -- the added documents cost
no assertion, which is the expected shape for a change that adds no
test file.

**DISCLOSED, and it is R-edit's own hazard, fired by this session on the
paragraph directly above.** That paragraph was first written through an
UNQUOTED heredoc inside a `wsl -e bash -lc` wrapper, so the outer shell
command-substituted both of its backtick spans before python saw the
string: `` `make test` `` was EXECUTED -- it really did start a suite
run, which died on a `.cpcache` permission error -- and its echoed first
line `clojure -M:poly check` was substituted into the document, while
`` `rulings.md#R-full-suite-before-push` `` was substituted to nothing
and left an empty pair of parentheses. R-edit exists for exactly this
and names the remedy: a script FILE, never an inline wrapper string.
Every other edit in this session went through one; this one did not, and
the repair did. It is the THIRD instance of this class across the two
dense-7500 sessions, which is why it is written here at length rather
than quietly corrected.

## 8. THE PREVIOUS RECORD'S MECHANISM WAS WRONG, and this one corrects it

The 2026-09-04 (a) record, this scenario's README, and the prompt that
drove this session all state that **`:persons` is what keeps the census
inside capacity** -- "without it, all 7,500 arrivals are distinct
patients each holding a bed for their pathway's full dwell". The prompt
instructed that sentence be carried into
`docs/consuming-ground-truth.md`. **It is not true, and it was not
written.**

`:persons` does not release a bed. `sim_engine/run.clj`'s `owner-ordinal`
resolves a repeat arrival to the FIRST arrival's ordinal, so a person
selected twice gets one patient id across two arrivals -- it merges
RECORDS. Both arrivals still arrive, both still admit, both still hold a
bed.

**The key that actually spreads the census is `:scheduling`**, and it is
in the nine and absent from the bare config. `decide :appointment`
(`decide.clj` :652) sets `scheduled-t` to the booking instant PLUS
`:lead-seconds`, and the pre-loop books `:scheduled-fraction` of all
arrivals with a lead of `:lead-time-days` (`run.clj` :268-305). The
outcome bands (`appointment-outcome`, :616) are cancel / reschedule /
no-show / kept in that order. At this file's own `0.70 / [3 21] / 0.08 /
0.15`, that defers 54% of a 5.2-day arrival stream across a three-week
window and drops 16% of it outright -- which is why `config.edn` and
`config-nobed.edn` completed at 7,500 while `config-bare.edn` did not.

This is fix-forward with disclosure rather than STOP-AND-REPORT: only
one reading is defensible once the code is read, so
`rulings.md#R-stop-only-on-two-defensible-readings` does not fire. The
corrected mechanism is what went into the docs, in both places the old
one appeared, and the sentence the prompt dictated appears nowhere.

## 9. What was NOT done, and the fences

- **No src file was touched.** Nothing under `components/*/src`, nothing
  under `bases/`. The only executable edited is
  `bin/demo-exerciser-dense-7500`, and only a comment block in it.
- **No new `bin/` script.** Both instruments -- the cell driver and the
  referential probe -- are scratch, and both are reproduced in this
  record's own sections rather than committed.
- **`.agents/plans/2026-09-01-event-mutation-population-ledger.md`
  section 6 is untouched**, as the fence requires. It still overstates
  the gaps; amending it stays priced work nobody has commissioned.
- **The 2026-08-29 figures are left standing everywhere they are
  history** -- `.agents/plans/2026-08-24-traffic-scale-program.md`, the
  roadmap's Done ledger, and the earlier session records. A sweep
  confirmed no LIVE doc surface still carries a superseded figure.

## 10. Close

**What this session establishes.**

- **The scenario is corrected and all four cells run.** The blocked
  10^5 cell exits 0 at 100,884 events in 144.29 s; the continuity
  series keeps its 10^5 point.
- **The Scale table's three rows are one generation of one committed
  configuration** for the first time since the table existed, and every
  one of them is re-runnable from the tree.
- **The dwell arithmetic is published, not just applied.** A future
  session that changes a pathway's delays has the bound and the ladder
  in front of it, and knows which ward binds.
- **All five referential carrier columns survive the correction**, and
  the two shipped ones cross-check to the site.
- **The previous generation's explanation of its own blocked cell was
  wrong**, and the correction is a mechanism read out of the engine
  rather than another inference from a run that stopped.

**CI at the pushed tip.** `gh run view 33879081513` -- status
`completed`, conclusion **success**, headSha
`2c46079d46600d4888355fc8f74111c23c535747`. Three commits pushed,
`e1baf4d..2c46079`.

**No tag paid.** `bin/post-push-verify e1baf4d 2c46079` reported all
three checks OK: `origin/main` matches HEAD, every commit message in the
range is pure ASCII, and the CI run was reported once (in_progress at
the time, disclosed as not awaited by the script itself, and awaited to
`success` by this session afterwards).

**What it leaves open.** Nothing this session was commissioned for. Two
things it deliberately did not begin, both named in section 9: the
ledger's section 6 still overstates three population gaps, and whether
the nine referential operator families extend to columns A / B2 / C is
priced work with no row.
