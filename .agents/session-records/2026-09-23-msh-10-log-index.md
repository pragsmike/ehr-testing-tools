# MSH-10 carries the log index — ADR-0181 candidate 3 lands

2026-09-23. Base `2b52fc57`; **four commits**; this record and the CI
marker ride the last. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt, which stated no
prepare-only mode.

The sibling record `2026-09-23-oru-control-id.md` is the other half of
this day: that session measured the defect and halted for a ruling,
this one executes the ruling it handed back.

`bin/preflight` ran **mid-session rather than first** — disclosed, not
excused; the same deviation the sibling record names, for the same
reason (the session opened by reproducing ADR-0181's counts). Its
findings at that point were this session's own three landed commits
ahead of `origin/main`, plus the standing HEAD-not-tagged disclosure.
Everything else green: last five CI runs on `main` all green at
`2b52fc57`/`e81f7026`, repo root `/home/mg/src/ehr-testing-tools` (not
under `/mnt/`), `core.fileMode` true, `core.ignorecase` unset, working
tree clean including untracked.

## The ruling this session executes

ADR-0181 (Proposed, `2b52fc57`) measured `control-id-for`'s default
`mrn-trigger-t` branch non-injective in five event classes and left
three candidate key shapes standing. The author ruled **"1. 3"** —
candidate 3, the log index, which the 2026-08-28 fan-out ruling had
already made this project's identity of record. The marker was
channel-*recommended* as `#` and explicitly not ruled; this session
verified it (step 3) rather than assuming it, and it stands.

## Checkpoints

| | commit | what |
|---|---|---|
| C1 | `85eb29ea` | the red tests — unit law and the all-message gate |
| C2 | `32344ca9` | the fix, the five funnels, the fixture moves, the rebuilt tripwire |
| C3 | `2c1ae8b9` | ADR-0181 Accepted, the row closed, nine readers corrected |
| C4 | this commit | record, prompt archive, indices |

## Step 1 — the red, reproduced first

Before writing anything, ADR-0181's own table was re-measured at
`2b52fc57` over the four `gated-runs` roots and the dense-7500 750
cell. It reproduced **exactly**, class for class:

| root | events | dup groups | messages |
|---|---|---|---|
| `seed-202-ed-tuesday` | 1,213 | 0 | 0 |
| `seed-424242-clinic-decade` | 1,774 | **2** | 8 |
| `seed-5-clinic-decade` | 1,412 | 0 | 0 |
| `adhd-seed-45` | 97 | 0 | 0 |
| `dense-7500` @750 | 33,306 | **43** | 86 |

with the dense cell's 43 splitting 25 `:transfer`/`:transfer`, 8
`:cancel-transfer`, 6 `:result-available`+`:observation`, 2
`:result-available`+`:diagnostic-report`, 2 `:merge`/`:merge` — ADR-0181
finding 3, unchanged. Measured off ground truth and again off the
rendered messages; the two agree message for message.

**One figure of ADR-0181 did not reproduce**, and it is corrected in
that record rather than here: its `adhd-seed-45` row reads 66 events
where `run_test`'s own `gated-runs` opts, verbatim and complete,
produce 97. The duplicate-group count is 0 either way, so no finding of
that record moves.

The two red tests (C1) were written to fail as **assertions** at
`2b52fc57`, not as a namespace that would not load: both hold the stamp
key and the marker as literals rather than as emitter vars, which did
not exist yet. 8 failures in the unit namespace, 7 in the gate — the
gate's being 2 injectivity failures (the two roots that collide) plus 5
"the marker reaches the wire" failures, one per root.

## Step 2 — the shape, and the five funnels

Every arm of `control-id-for` now ends in `log-index-marker` plus the
event's own 0-based log position. `stamp-log-index` is the one
stamping function, called at `emit`, `emit-wire`, `plan-latency`,
`plan-ladders` and `ehrt.sim.identifiers`. They stamp the same whole
log through the same function because they must AGREE event for
event — `emit-wire` looks a latency offset up under the id
`plan-latency` minted, and a ladder rung rides its basis event's — and
five independent `map-indexed`s is the kind of agreement that holds
until it doesn't. The stamp goes on before any filter, never inside
one, or the index would number the survivors rather than the log.

`control-id-for` **throws** on a registered event with no stamp. That
throw is not defensive decoration: it is what found every one of the
nine fixture moves below, each a test that had been handing a raw log
event to the emitter. The registry is consulted first, so an
unregistered event is still nil and the raw-log walkers are untouched.

**Invariant held, with one disclosed correction.** The prompt's step 2
asked that `git diff -- components/sim-engine components/sim-check` be
empty. Both components' **src** is byte-unchanged; one sim-engine TEST
file, `bed_cycle_test.clj`, moved, because it calls
`messages/event->messages` directly at one site. The prompt's step-5
fixture list was sim-emit-hl7-scoped and did not anticipate it.

## Step 3 — the marker, verified rather than assumed

The dense-7500 750 cell's full wire rendered at `2b52fc57` (disposable
worktree) and at the landing tree — 40,291 messages both sides:

* duplicate MSH-10 groups **43 → 0**, messages sharing an id **86 → 0**
* 21,518 ids moved, 18,773 did not; **0 prefix-law violations**
* stripping every `#<digits>` from the after-wire reproduces the
  before-wire **byte for byte** — the marker and index are the only
  bytes this change moves anywhere
* longest MSH-10 **46 → 52** chars, at
  `MRN002807-APT-002806-00-01263947-S26-634683859#33299`; max index
  33,304

Parsers, over 543 messages (the 86 formerly-colliding ones plus the
first 25 of each of the 20 MSH-9 families):

* `ehrt.corpus-io.er7/parse` — all 40,291 messages round-trip exactly
  on both sides, MSH field counts stable, its read of MSH-10 equal to a
  raw split. `corpus-io/message-control-id` agrees on both sides.
* `gate v2` (HAPI tier) — 543/543 **pass**, zero findings, both sides.
* `gate v2-nist` against `COVID19_ELR-v2.3.1` — **identical
  finding-class histogram, code for code and count for count**, 13
  classes. `structure/Length Spec Error` is 17,069 on both sides, which
  is the length question answered: the six extra characters bought no
  new length finding. Per-file verdict, finding count and codes
  identical across all 543 at both tiers.

`#` stands. It is not an HL7 v2 delimiter, so it needs no escaping and
moves no field boundary — and it is `#` rather than `-` for a reason
that is a live collision rather than taste: every restatement already
mints `mrn-trigger-t-<ordinal>`, so `-` would have given the two
families one four-part shape and collided them the first time a log
index equalled an ordinal.

## Step 4 — the tripwire, rebuilt not weakened

`ehrt.conformance.mllp-pairing-test` opened with "the duplicates are
STILL THERE", which this change makes not merely vacuous but **false**.
Retired. The pairing law is now stated over the same real
`seed-424242` spool with a **hand-built** duplicate pair appended, and
the namespace says why that is the right shape rather than a
concession: a `--sink mllp://` carries FOREIGN corpora, which may
repeat an MSH-10 for any reason of their own, so positional pairing has
to survive a duplicate whoever minted it. A new first assertion states
this corpus's own injectivity, because the pairing assertions are built
on top of it — the only duplicate they meet must be the hand-built one.

**No test asserts any more that a shipped corpus carries duplicate
MSH-10s.**

## Step 5 — fixture moves, nine namespaces

`ladders`, `siu`, `charges`, `chatter`, `latency`, `result-clock`,
`vendored-sepsis`, `emit-hl7`, and `sim-engine`'s `bed-cycle`. Each
moved literal names which side of the marker it asserts: ground-truth
ids gained `#<index>`; **restatement and rung ids did not move and are
absent from every replacement table**, and neither did DFT ids. No
assertion was weakened from `=` to `includes?`.

One selector changed rather than one literal. `siu_test` found "the
messages at t=200" by splitting MSH-10 on `-` and taking the tail; `t`
is no longer the last part, so it now names the instant and the marker
together (`-200#\d+$`). Its equality assertion is left exact.

**Two of the prompt's step-5 names did not move, and the reason is
worth keeping.** `identifiers_test` quotes no id at all: its
completeness property extracts MSH-10 from a REAL emission and asserts
the inventory is a superset, so it holds unchanged — and now proves
something it could not before, that the fifth funnel's stamping agrees
with the emitter's. Of the `vendored_*` family only `vendored_sepsis`
moved, and not for a literal either: it picks its `:diagnostic-report`
event out of the raw log to mint an id with, so it needed the stamped
log, not a new expectation.

## Step 6 — the bracket and the oracle

`bin/ground-truth-bracket 2b52fc57 HEAD` (no flag): **IDENTICAL** on
every one of the 38 digested roots; 3 skipped for carrying no
`:ground-truth` key (`appendicitis`, `ear-infections`, `sore-throat`).
Ground truth did not move.

`bin/regression-oracle 2b52fc57 HEAD --declared-digest-change`:
**DIFFERS**, declared, exit 1. **36 roots differ and 5 are IDENTICAL**,
where the prompt predicted three.

The two extra are not a finding but they are not the prompt's rule
either, so they are named: `dermatitis` and `veteran-self-harm` carry
an `:hl7` key that is **present and empty** — 301 and 300 ground-truth
events, zero messages — so they have no MSH-10 to move. Measured, not
inferred. The rule that actually holds is *every root that renders at
least one message differs, and only those*; the three the prompt named
are the batch roots that carry no `:hl7` key at all.

## Step 7 — docs

ADR-0181 Proposed → Accepted, with "The shape that landed" carrying the
key, the marker, the mechanism and every measurement above. Its old
Fence is kept verbatim and marked superseded rather than deleted: it is
what the measurement was published under. ADR-0175's "Control ids must
change shape" paragraph gains a dated pointer.
`planners/assign-restatement-ordinals`' docstring said "a ground-truth
event's own id has NO ordinal suffix" — true of the three-part key,
false from C2, corrected in place with the marker named as what carries
the claim now.

`roadmap.md#oru-control-id-collision` → **CLOSED** with `32344ca9`,
compacted to one pointer line under `## Done` and out of `## Next`
entirely. `roadmap-lint-test` is what taught that: closing the row in
place went red three ways at once — no valid status token (the sha must
be bare, not backticked), a CLOSED row outside `## Done`, and a
duplicate `PRIORITY 4` once the new residual row took a number. The
residual row inherits the closed row's vacated **PRIORITY 3** and its
file position, so `## Next` stays unique and ascending. The Done line
went in at 493 characters against R-cap's 480 and was compacted to 416.

`make docsgen && git diff --exit-code` clean on the committed tree.
`figures.edn` untouched — a field moved, no count did.

**One stale quotation found outside the prompt's reading set.**
`docs/manual/05-batch-delivery.md` quotes two MSH segments as
"witnessed this session by fresh regeneration, MSH segments
byte-faithful". Both carried pre-change control ids, so the chapter's
own claim about its own bytes had become false. Re-generated (the same
`corpus generate sim` + `corpus batch` pair `bin/demo-exerciser-ed-
tuesday` runs) and updated to `MRN000002-A01-360#3` and
`MRN000002-A03-5040#15`, with a footnote saying why and confirming the
exerciser's own straddle grep still matches — it greps the PREFIX,
which is exactly what the id was before this change. The footnote form
is not decoration: `link-footnote-gate-test` forbids a visible
`ADR-NNNN` token in `docs/` prose, so the record is cited by footnote.

**Nine readers corrected, and four of them keep their argument.**
`corpus_io/mllp` and `corpus_io/mllp-test` keep positional pairing
because foreign corpora exist, not because ours collide; `fan_out` and
`sim/run` keep the log index as identity and now record that ADR-0181
*vindicated* that ruling rather than retiring it — the record cites
`fan_out`'s own sentence as the reason the log index was already the
project's identity of record. A blanket "CLOSED" would have lost that.

## The one residual, rowed rather than fixed

**`messages/dft-message` mints its own MSH-10 inline — `mrn-P03-t` —
without going through `control-id-for`.** So DFT^P03 keeps a three-part
key and stays non-injective for two encounter closes of one patient at
one second. 2,609 of the dense cell's 40,291 messages are DFT^P03 and
not one of their ids moved.

Found by the before/after render, not by reasoning: the histogram of
"MSH-10 did not move" came back
`{A08 5286, A31 6026, A28 2796, ORM^O01 1028, ORU^R01 1028, DFT^P03
2609}` and the first five are restatements, by design. The sixth was
not supposed to be there.

It is the same defect the closed row named, in the one family that does
not read the fixed function. Fixing it is a **builder** change, which
the prompt's step 2 explicitly fenced out ("No builder changes"), so it
is rowed as `roadmap.md#dft-control-id-collision` (PRIORITY 4) rather
than smuggled in. It is not witnessed live: no duplicate DFT id occurs
in any root the new gate runs, so like the closed row's own
`:result-available` case it is reachable-in-principle. The new gate
would catch it the day one appears.

## Gates added

* `ehrt.sim-emit-hl7.control-id-log-index-test` — the local law over
  hand-built events, including the fail-closed throw and the prefix
  law.
* `ehrt.sim.run-test/control-id-for-is-injective-over-every-corpus-this-lane-runs`
  — the **all-message gate** ADR-0181 said was missing: MSH-10 read off
  the WIRE (never re-derived from the log, which would check the stamp
  against itself) over the four gated corpora **plus the dense-7500 750
  cell**. That cell is not a gated corpus and is there for one reason:
  four of the five collision classes occur in NO other root any `make`
  target runs, so a gate without it would watch the minority. Cost
  disclosed in its own def: ~29 s of the per-push lane, run once, and
  the first time any lane measures that cell at all.

## Step 8 — the suite

`make test` unpiped, redirected to a file, wrapper ending
`exit "$MAKE_EXIT"`:

    MAKE_EXIT=0

430 `Test results:` lines, **28,627 passes, 0 failures, 0 errors**,
`clojure -M:poly check` OK, `bin/verify-nist-lock` clean. CI green on
the pushed tip is the close marker.

It took three runs to get there, and the two reds are worth naming
because neither was the change itself:

1. `roadmap-lint-test`, 3 failures — closing the row in place instead
   of moving it to `## Done` (see step 7).
2. `state-derived-test`, 3 failures — this record and its prompt
   archive are new FILES, so the two `INDEX.md`s and
   `.agents/state-derived.md` went stale the moment they were written.
   `make docsgen` is what reconciles that, and it has to run AFTER the
   record exists, not before.

## Downstream notice

For the 32k-subject corpus at `e81f7026` and any other consumer holding
ids from before `32344ca9`:

* **Every MSH-10 changes.** The old id is a strict **PREFIX** of the
  new one, in every arm, so a held id finds its successor by prefix.
* **The suffix is `#` plus the event's 0-based LOG INDEX** — its
  position in the ground-truth event vector. It is a join key from any
  message back to the event that produced it, with no lookup table.
* **Ground truth is unchanged**, byte for byte, proven by
  `bin/ground-truth-bracket` across all 38 digested roots. Only the
  wire moved.
* **Restatement ids did not move.** Chatter's A08/A31/A28 and the
  ladder's O01/R01 still mint `mrn-trigger-t-<ordinal>`; the `#` marker
  is what keeps the two families disjoint.
* **DFT^P03 ids did not move either**, and that is the residual above,
  not a design: they still carry `mrn-P03-t`.

## What a successor should know

* **The gate to keep honest** is
  `control-id-for-is-injective-over-every-corpus-this-lane-runs`. It
  carries the dense-7500 750 cell for a stated reason (four of five
  collision classes occur nowhere else) and costs ~29 s. Do not drop
  that cell to save the wall clock without replacing the coverage.
* **`control-id-for` throws now.** Any new caller must stamp first, and
  must stamp the WHOLE log before filtering. That is the single
  mistake this change's own fixture moves were, nine times over.
* **The marker is `#` for a reason that is testable**, not a style
  choice: it is what keeps ground-truth ids and restatement ids
  disjoint. If it ever changes, `assign-restatement-ordinals`' own
  docstring and `log-index-marker`'s both have to move with it.
* **`#dft-control-id-collision` is the unfinished half.**
