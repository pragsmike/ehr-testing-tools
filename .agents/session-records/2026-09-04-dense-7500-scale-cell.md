# 2026-09-04 -- dense-7500: the documented scale cell, committed, and one cell that will not run

`docs/consuming-ground-truth.md`'s Scale table cited three cells nobody
could re-run. This session commits their configuration as a maintained
scenario, exercises it like every other scenario, and re-measures. Base
`e57be19`; ceremony R30 (commit and push at each checkpoint,
unattended), taken from the prompt. No sub-agents.

**It STOPS one step short of its own last two checkpoints, and says so
first.** Step 3's gate is `every run exits 0 (nonzero = STOP finding)`.
Six of the eight timed cells exit 0. The two `config-bare.edn` cells --
the "no opt-in key at all" row of the very table this session was
commissioned to re-measure -- exit **2** on `:capacity-exhausted`,
reproducibly. So the Scale table is **NOT rewritten** and the
`dense-scale-profile` row is **NOT closed**: the ruling that decision
needs is section 7's.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Last five CI runs
on main all green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`; tree clean including untracked; local HEAD matched
`origin/main`. One disclosure, and it is the correct state: HEAD is not
tagged `stable-*` -- no tag is paid.

## 1. THE FINDING THE SESSION OPENED WITH: the scratch did not survive

R-shape said to adopt penny's `dense-7500-v2.edn` if present and verify
the record's stated byte counts, else author from the table. **It is not
present.** Searched, in this order:

    find /home/mg -maxdepth 6 -name 'dense-*.edn'   -> nothing
    ls /home/mg /home/mg/{scratch,sp,dsc} /tmp      -> no spike/ dir, no
                                                       gen-config.py, no
                                                       gen-v2.py
    find / -xdev -name 'dense-7500*.edn'            -> nothing

The 2026-08-24 spike's own record called this out as its F1 -- "a figure
whose driver died is a figure nobody can check" -- and the 2026-08-29
close noted with some relief that the scratch had survived "for the
SECOND arc running". It did not survive the third. That is the whole
justification for this session existing, arriving as a measurement
rather than as an argument.

**So the skeleton is RE-AUTHORED, not adopted, and the difference is
load-bearing for everything below.** The 2026-08-24 record describes the
scenario in prose, not in bytes: it gives step COUNTS (21 / 8 / 12),
ward sizes, pathway weights, the arrival gap, the module list and the
every-eighth-ordinal cohort stride. It does not give the steps. Those
are this session's, and section 7 is where that turns out to matter.

The nine opt-in keys ARE frozen verbatim to the 2026-08-29 record's own
section-3 table (R-values), mismatches included and disclosed in the
file itself: `:scheduling` and `:chatter` carry a booked ambulatory
decade's numbers over acute traffic. `:persons :count` is the one value
that is a RULE rather than a copy -- twice the arrival count, so 15,000.

Every code in the file is already in this tree: SNOMED and RxNorm
concepts lifted from the vendored Synthea modules under
`components/sim/resources/sim/modules/`, LOINC from the same, and the
`:cbc`/`:bmp` order profiles are the shipped catalytic. No CPT.

## 2. C1 -- the configs (`9106dd5`)

Three files, and both derivations are CHECKED rather than asserted:

| check | result |
|---|---|
| `diff config.edn config-nobed.edn` | exactly one removed line, `:bed-cycle true`; zero added |
| `config-bare.edn` less its `}\n` vs `config.edn`'s first 152,872 bytes | sha256 `02d77b97...` on both sides |

The second is the 2026-08-29 record's own additive claim -- "the old
file byte for byte, minus only its closing brace, plus the nine keys" --
re-established on new bytes. The prefix arithmetic is `len(bare) - 2`,
and the exerciser re-derives it from the file rather than carrying
152,872 as a literal.

Smoke-tested before the commit: exit 0 and self-check clean at 40
patients (`sim run`, 18,628 events) and at 200 (`corpus generate sim`).

## 3. C2 -- the page, the exerciser, the register row (`0cc865d`)

README on ed-tuesday's shape; three taught commands, all
root-resolvable, all resolving under invocation-lint's path check. It
teaches `--format ground-truth` explicitly, at 750 arrivals rather than
7,500 -- that command is a shape demonstration, not the headline cell.

`bin/demo-exerciser-dense-7500` is the third `:demo-exerciser-fresh` row
and the FIRST whose exercised commands are minutes long. Integration
tier only, like both siblings; it is why that tier's wallclock moves.
Beyond exit codes it asserts the two derivations above, both re-derived
at runtime, plus that `--format ground-truth` wrote a bare top-level
vector rather than silently falling back to the EDN envelope.

The register's pinned row count moved 16 -> 17. `:witness` cites
ADR-0113's R3 -- the standing law the row implements -- rather than
inventing an ADR the de-scaffold ruling does not want.

**Gate:** `exercised-sources`, `exercised-sources-coverage`,
`demo-exerciser-fresh`, `invocation-lint` -- **26 tests, 388 assertions,
0 failures, exit 0**. All 17 register rows fresh; dense-7500 reports 5
taught lines on both sides. `make docsgen` moved no generated file.

**Disclosed, a near-miss of this session's own making:** the first gate
run was reported as `TEST_EXIT=0` while carrying a real failure, because
the command was piped through `tail`. The pin failure was visible in the
output and was acted on, but the exit code was the pipe's. Re-run
unpiped with the code captured explicitly; every gate run after that one
wrote to a log with `$?` captured (`rulings.md` / build-session
VERIFICATION, and this repo's own ADR-0152).

## 4. Step 3 -- the measurement

R-measure, followed exactly: the warm-up IS `bin/demo-exerciser-dense-7500`
run once (**exit 0, 329 s**), then two timed runs per cell, one JVM per
run, a FRESH spool target per run, seed 20260824 throughout,
`/usr/bin/time -v` around each, figures the mean of the two.

Host, sampled at every cell boundary: up 10 days 15 h, 15 GiB total with
8.7-8.9 GiB free throughout, load average 0.38 at session start and 1.69
at end -- this session's own runs, no external contention.

### 4a. The completing cells

| cell | arrivals | exit | events | messages | msg/event | process wall | peak RSS |
|---|---|---|---|---|---|---|---|
| `config.edn` | 7,500 | 0 | 166,295 | 224,645 | **1.3509** | 276.06 s | 2,204 MB |
| `config-nobed.edn` | 7,500 | 0 | 124,999 | 168,869 | **1.3510** | 220.55 s | 1,965 MB |
| `config.edn` | 750 | 0 | 33,274 | 41,768 | **1.2553** | 53.64 s | 1,166 MB |

**Both runs of every completing cell produced the IDENTICAL event and
message counts**, and so did the untimed probe that preceded them --
three independent JVMs on the 7,500 cell, 166,295 / 224,645 every time.

### 4b. The blocked cell -- THE STOP

| cell | arrivals | exit | wall to refusal | peak RSS |
|---|---|---|---|---|
| `config-bare.edn` run 1 | 7,500 | **2** | 42.07 s | 878 MB |
| `config-bare.edn` run 2 | 7,500 | **2** | 41.73 s | 884 MB |

Identical payloads, so this is deterministic and not a flake:

    {:status :error, :category :capacity-exhausted,
     :payload {:patient-id "PID-002963-bfc158cf", :ward "Surgery",
               :census {"Emergency"  {:occupied 219, :capacity 220}
                        "Medicine A" {:occupied 240, :capacity 240}
                        "Medicine B" {:occupied 200, :capacity 240}
                        "Surgery"    {:occupied 200, :capacity 200}}}}

**`:persons` is what keeps the census inside capacity, and that is the
opposite of what the three configs look like.** `config-bare.edn` is the
cheapest of the three by every other measure and is the only one that
stops. With `:persons` present an arrival BINDS to a person and a repeat
arrival of somebody already registered opens a second encounter rather
than a fresh concurrent stay; without it, all 7,500 arrivals are
distinct patients each holding a bed for their pathway's full dwell.
**The opt-in that looks like pure added volume is also a throttle.**

**It is a defect of the RE-AUTHORING, not a discovery about the
engine**, and this record will not dress it up as the latter. The
2026-08-29 `old` series ran this same skeleton at 7,500 with no opt-in
key and completed at 105,214 events. The original's step contents gave a
lower steady-state census than the ones authored here -- section 1's
prose-not-bytes gap, showing up as an exhausted ward.

### 4c. Why nothing was tuned

The prompt pre-committed the response: `nonzero = STOP finding`.
Independently, `rulings.md#R-stop-only-on-two-defensible-readings`
reaches the same place, because two readings are genuinely defensible
and they lead to different artifacts (section 7). Shortening the dwells
or enlarging Surgery would have changed `config.edn`'s bytes -- the
provenance R-shape names -- and invalidated the two cells that DID
complete, in the same session that committed them.

## 5. Step 4 -- the referential matrix, and the session's real prize

Per the ledger's own section-7 recipe, over `config.edn`'s 7,500-arrival
log. The two SHIPPED columns were counted through the live catalog's own
`:candidate-sites` predicates -- the ledger's stated authority -- and the
three population-gap columns through predicates reconstructed from
`operators.clj`'s `resolving-sites` and `referential-shapes` at the same
commit. Reconstructed, and said so.

| col | field | carrier | carriers | resolving sites |
|---|---|---|---|---|
| **A** | `:cancels-event-id` | the three cancels | 2,164 | **2,164** |
| B1 | `:order-event-id` | `:result-available` | 10,196 | **10,196** |
| **B2** | `:order-event-id` | `:medication-end` | 4,811 | **4,810** |
| **C** | `:start-event-id` | `:care-plan-end` | 3,449 | **3,449** |
| D | `:placeholder-event-id` | `:identity-fill` | 934 | **934** |

Column A, per kind: `:cancel-admit` 52, `:cancel-transfer` 2,063,
`:cancel-discharge` 49.

**ALL FIVE COLUMNS ARE POPULATED, AND THREE OF THEM WERE POPULATION GAPS
UNTIL THIS COMMIT.** The 2026-09-01 ledger recorded A, B2 and C as
convictable-in-principle but unwitnessable -- "no log this repository can
generate carries a single candidate site for them, measured over both
opt-in demo configs at their own documented invocations". That was true
of the two configs then in the tree and is now false. A needs a cancel,
which neither sibling produces; B2 and C need a `:medication-end` and a
`:care-plan-end`, which the pathways here author with citations and
neither sibling does.

**Cross-check, and it is exact.** The reconstruction and the shipped
predicates agree to the site on both shipped columns: B1 10,196 and D
934 by both routes, across all four applicable shapes. The one
discrepancy in the table is not one -- B2 has 4,811 carriers and 4,810
resolving sites, the single difference being one `:medication-end` whose
`:order-event-id` is nil, which `resolving-sites` correctly excludes.

**Report only, no commit** -- per the prompt. The ledger is not amended
here and no operator is registered: turning three population gaps into
three shipped operator families is its own priced work, and this session
was not it.

## 6. What was NOT done, and why

- **The Scale table is not rewritten** (step 5). One of its three rows
  cannot be re-measured on the committed scenario. Rewriting the other
  two would leave a published table mixing two generations of
  configuration, which is worse than leaving it whole.
- **`roadmap.md#dense-scale-profile` is neither closed nor created.**
  DISCLOSED as a premise mismatch: **no such row exists** -- grep finds
  the slug nowhere in the tree. `:onboarding` measures 1,487 lines
  against its 1,530 budget, so the 43 lines of headroom the prompt made
  the condition ARE there and the row was affordable. It is withheld on
  the other condition instead: the work it would name is not done.
- **No src file was touched.** Nothing under `components/*/src`, nothing
  under `sim-check`, and no measurement instrument was authored -- the
  cell driver is scratch, as the fences require.

## 7. FOR THE DESIGN CHANNEL -- the ruling this session stopped for

`config-bare.edn` at 7,500 arrivals stops on `:capacity-exhausted`. Two
readings, both defensible:

**(a) The committed bytes are right; the third row is not re-measurable.**
Keep `config.edn` as the provenance R-shape made it, leave the Scale
table's third row citing 2026-08-29 with a dated note saying the
configuration it was measured on no longer exists, and let
`config-bare.edn` stand as the 750-and-below config it demonstrably is.
Cheapest, and it keeps the two measured cells honest. Costs the
continuity series its 10^5 point permanently.

**(b) The skeleton is wrong and should be corrected.** The original
completed at this cell and this one does not, so the re-authoring
overshot on dwell time. Shorten `dense-inpatient`'s two long delays (or
raise Surgery and Medicine A), regenerate all three configs, re-commit
C1, and re-measure all eight cells. Gets a table whose three rows are
one generation. Costs another measurement sweep, and every figure in
sections 4a and 5 moves.

**A third option exists and is worth naming rather than leaving
implicit:** keep the bytes AND lower the bare cell's arrival count,
reporting the continuity row at whatever count `config-bare.edn` does
reach. That trades a same-count comparison for a completing one, which
may be the wrong trade for a row whose entire purpose is comparison.

**This session recommends (b)** and did not take it, because it changes
the artifact C1 already committed and R-shape names those bytes the
provenance. The recommendation is not the ruling.

One further item for the channel, unrelated to the stop: section 5's
result means `.agents/plans/2026-09-01-event-mutation-population-ledger.md`
section 6 now overstates the gaps. Three of its population gaps have a
population. Amending it, and deciding whether the nine referential
operator families extend to columns A/B2/C, is priced work this session
deliberately did not begin.

## 8. Close

**What this session establishes.**

- The scale scenario is **committed, runnable and gated**: three configs
  whose relationship is mechanically checked, a README, an exerciser,
  and a register row that fails the build if page and script drift.
- The 7,500 cell **completes and self-checks clean** -- exit 0, 276.06 s,
  2,204 MB, 166,295 events -- where the equivalent cell was BLOCKED for
  the whole of 2026-08-29 on four invariant families since fixed
  (TS-3, TS-4, TS-5, ADR-0177's A1).
- **The bed cycle is 24.8% of this corpus** (41,296 events, 55,776
  messages) at an msg/event ratio unmoved to four places -- it adds wire
  traffic in exact proportion to the log it adds.
- **All five referential carrier columns are populated**, closing three
  of the 2026-09-01 ledger's population gaps as a side effect.
- **One cell does not run**, deterministically, and the session stopped
  rather than tuning it. `:persons` throttles concurrent census; the
  config that opts into nothing is the only one that exhausts capacity.

**CI at the pushed tip.**  -- status
, conclusion **success**, headSha
. No tag paid.
 reported all three checks OK.

**What it leaves open.** Section 7's ruling, and everything downstream
of it: the Scale table, the `dense-scale-profile` row, and the ledger
amendment.
