# 2026-08-16 — clinical time on the result wire: OBR-7 and OBX-14 (ADR-0142)

Autonomous (R30). Baseline `c90c9bd`; ADR-0142. Four commits,
`760f81d..14e718f` plus this close. A latency-shifted `ORU^R01` now
carries both clocks, so a downstream receiver handed a late result can
back-date it from the message rather than guess.

## Step 0 — preflight, disclosed in full

`bin/preflight` (plain), every line as printed:

- **FINDING: a red run among the last five CI runs on `main`.**
  `0f647ea` (run 31962855332), 2026-08-16T17:51:29Z. **Read to
  conclusion rather than passed**, since a probabilistic red hides
  behind any single green: `FAIL in (every-real-item-is-indexed-test)
  (index_completeness_test.clj:134)`, "42 passes, 1 failures, 0
  errors", exit 1 — the event-log-contract arc's own index-completeness
  late catch, which that session recorded and fixed within the same
  arc. Every commit after it is green, the tip included. No live
  defect.
- Edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/` — OK.
- Tree clean including untracked — OK.
- Local HEAD `c90c9bdb…` matched `origin/main` — OK.
- Last `stable-*` tag `stable-20260815-review-3-fixes`; **DISCLOSED:
  HEAD not tagged** — discharged immediately by the two tag payments.

**Both licensed tags paid, in date order, through `bin/tag-ceremony`
with `--push`, each ending in its own peeled-ref verification.** These
discharge the close tags BOTH predecessor sessions recorded as
deferred — the event-log-contract session having explicitly recorded
that it could not pay the fence-battery one because the author-side CI
relay was absent from its prompt. This prompt carried it.

```
OK: remote peeled ref for 'stable-20260816-fence-battery' is 24f351d…, matches target exactly
OK: remote peeled ref for 'stable-20260816-event-log-contract' is c90c9bd…, matches target exactly
```

Re-verified independently with `git tag --points-at` on both shas and
`git ls-remote --tags`.

**Baseline:** `make test` unpiped, full log to file, `MAKE_EXIT=$?`
captured → `MAKE_EXIT=0`, **332** zero-failure blocks, **17,054**
passes, zero `FAIL`/`ERROR`. Reconciles **exactly** with ADR-0141.
`clojure -M:poly check`: OK.

**A counting near-miss, recorded rather than quietly fixed.** The first
block count came back **664** and was re-derived instead of reported:
`make test` states each namespace's result twice (a bare `0 failures, 0
errors.` line and the coloured `Test results: N passes, …` line), so a
pattern matching both double-counts. Three independent counts agree at
332. The countable signature is only as good as the pattern, which is
the same lesson D2-6 taught about pipes.

## Step 1 — census and prediction (`760f81d`)

**Field audit re-derived from the tree, not copied from ADR-0109.**
`OBR-7` and `OBX-14` appear nowhere in `emit_hl7.clj` in any spelling;
`hl7-timestamp` is called at exactly seven sites, three being the ADT
builders' `clinical-ts`/`transmit-ts` pairs and four the order/result
builders' transmit-only. ORC-9 explicitly out of scope. The prompt's
own cite ":626-770" for the three ORU builders is :649-767 — corrected
from the tree.

**Mover prediction, population-closure law, both halves from the live
tree:** the root set read from `digest.clj:544-579` (**35**, counted
from the map) and the per-root content from this session's own
PRE-digest run over all 35. **14 movers, 21 identical.** The prompt's
"27-ish predicted-identical" does not survive the derivation and is
corrected to 21 — precisely the class ADR-0139 rule 9 names.

Two findings fell out of the census:

1. **`ORM^O01` count is ZERO across all 35 roots.** No oracle root
   emits an order message, so the oracle could never have witnessed an
   ORM change — an argument for freezing ORM rather than moving it
   unwitnessed, which is where Q3 landed.
2. **Every `OBR|` in the digest set sits inside an ORU**, so the
   ORU-keyed prediction is complete for OBR-7 as well as OBX-14.

**Strip census** over the scan-root class, tracked files only: exactly
**three** files carry an `OB[RX]|` strip, all under `demos/traces/`,
and `demos/traces/**` is verified **ungated** — no `Makefile` target
(checked against all 17), no workflow, no test, only `.gitattributes`
`-text`. **Two of the prompt's three known-from-clone hits carry no
strip at all** (`demos/traces/README.md` and
`demos/traces/emit-state/README.md`, the latter having one prose
mention that explicitly declines to quote the strip and stays true
after the change) and are corrected rather than trusted.
`docs/formats.md` describes no ORU field list, so its Step 4 item was a
no-op.

## Step 1.5 — a scope collision, reported not resolved

The prompt's Context said "OBR-7 wherever `obr-segment` renders", but
`obr-segment` also renders in `orm-message` (`emit_hl7.clj:646`), while
the same prompt's Step 3 fence, Fences block and STOP-AND-REPORT list
all held ORM untouched, and its mover rule and test set were ORU-only.
Reported to the author rather than silently narrowed or silently
widened. **Ruled: "Results only; ORM byte-frozen"** — recorded as Q3 in
ADR-0142 and `.agents/rulings.md`, with ORC-9 registered as a named
revisit rather than absorbed.

## Step 2 — red first (`ca64f5a`)

A **sibling** file, `result_clock_test.clj`, not more of
`latency_test.clj`, and the new file's docstring says why: half of what
it asserts is about plain `emit`, which "the second clock" would
misdescribe. `latency_test`'s 100-trial identity property stays where
it is and is re-run rather than duplicated.

Fields read off the RAW ER7 text rather than through the parser's
accessors, so an absent trailing field reads as `nil` — the red has to
be "the field is not there", unambiguously.

```
Ran 6 tests containing 58 assertions.
20 failures, 0 errors.
{:test 6, :pass 38, :fail 20, :error 0, :type :summary}

expected: (= expected (raw-field obr 7))
  actual: (not (= "20240101010300+0000" nil))
```

Checkpoint isolation was satisfied by construction — no src change
existed in the tree yet, so no stash was needed. Two of the six tests
were green in the red run **by design** (the per-shape identity witness
and the contract-neutrality assertion) and had to stay green after;
disclosed rather than glossed. The run was `clojure -M -e`, which does
not exit nonzero on failure, so the claim rests on the assertion text
and summary map, never on an exit status — stated in the commit message
too.

## Step 3 — green, gate, oracle (`14e718f`)

`obr-segment` gains a 3-arity (OBR-5/6 empty, OBR-7 clinical);
`obx-segment` and `observation-obx-segment` gain the pad and OBX-14;
the three ORU builders compute and thread `clinical-ts`. `orm-message`
keeps calling the 2-arity, so its call site has a literally empty diff.

**Gate:** `make test` unpiped, `MAKE_EXIT=0`, **334** blocks /
**17,176** passes, zero `FAIL`/`ERROR`, reconciled **per namespace**:
+116 (new namespace × 2 contexts), +4 (the re-baseline × 2), +2 (a
docs-tooling lint whose assertion count tracks the number of test files
it scans) = 122, matching exactly. `latency-test` unchanged at 171
assertions — the identity property held straight through.

**Run hygiene, disclosed:** the first post-change run began before two
docstring edits landed, so it is not what the ADR claims. A second,
clean run with `out/` cleared and the tree exactly as committed
returned identical figures, and that is the reported gate.

**Oracle bracket** — `bin/regression-oracle c90c9bd HEAD`, exit 1
(`DIFFERS`, the declared-change signal). Soundness line confirms
`digest.clj` identical on both sides, so both ran the same instrument.
Mover set extracted mechanically and `diff`ed against the Step 1
prediction file:

```
predicted: 14  actual: 14
EXACT MATCH -- prediction == actual, no residue either way
```

The other 21 roots byte-identical. No STOP-AND-REPORT fired.

**One pinned test re-baselined, with disclosure in the test itself**
(`observation-emits-oru-with-one-obx-and-no-orc-or-obr`, `(= 7 …)` →
`(= 15 …)` plus OBX-7/8 asserted empty). It is a semantic change, not
just a count: ADR-0029's D1 property was phrased as field ABSENCE, and
reaching OBX-14 pads through OBX-7/8; what D1 actually cared about
survives and is now asserted as emptiness. One further docstring
amendment in `latency-test` is recorded explicitly as **not** a
re-baseline — its `testing` string claimed more than its assertions
ever tested, and no assertion changed.

## Step 4 — docs, and a finding from running

**`events.edn` did not move, anywhere.** The manual's own
ground-truth-invariance transcript was re-witnessed rather than
trusted: `diff` silent, both digests `b4e776f7…` — byte-for-byte the
value already committed in the chapter. The STOP-AND-REPORT condition
never approached firing.

**No `config-latency.edn` widening was needed or made** — Step 4's
conditional never engaged, that file already covering `:order-placed`
and `:result-available`, verified by reading it before acting.

**The ungated-tree finding, discovered by doing the work.** Regenerating
`demos/traces/order-result/messages.txt` for the new fields also picked
up **12 changed `PV1` lines** — drift from the site-profiles
milestone's trailing positional fields that had sat there through
multiple sessions, and that had made the sibling README's "same as
`order-result/messages.txt`" claim FALSE. Both files are regenerated
(byte-identical again at 5,822 bytes) with the two change classes
**separated in dated errata rather than blended**, so no later reader
is told ADR-0142 moved PV1. The missing gate is a roadmap row with a
proposed shape, not a silent fix.

**The ORM freeze is witnessed on a committed artifact, not only
asserted:** `emit-state/messages.txt` was current, so its regeneration
isolates this session's change exactly — of its six `OBR` lines (three
ORM, three ORU), only the three ORU ones moved. `ground-truth.edn` in
both demo directories is untouched.

Manual chapter 4 gains "When the result is late", built on a strip
regenerated this session (seed 20260811, `out/` cleared first):
MRN000005's CBC resulted `03:22:00Z`, transmitted `04:07:40Z` — MSH-7
moving 45m40s while OBR-7/OBX-14 are the same bytes on both wires and
the message's position moves `msg-020` → `msg-023` under the
transmit-time sort. The chapter's "exactly two timestamp-bearing
fields" sentence is given a dated update rather than a silent rewrite.
Architecture §5 gains a dated addendum.

## Deviations, dated 2026-08-16

1. **The Step 2 push was held** until Step 3 was green and both were
   pushed together. R30 says push at each checkpoint; pushing a
   knowingly-red commit alone would have put a red CI run on `main` —
   manufacturing exactly the finding class this session had already had
   to disclose one instance of at Step 0. `bin/post-push-verify` ran
   over the whole range, so the red commit's message was ASCII-checked
   like any other.
2. **One ride-along register line**, disclosed in place: the roadmap's
   `## Done` list had no ADR-0141 entry, and adding only ADR-0142's
   would have left a visible gap in the sequence. One line, annotated
   in the file itself.
3. **Three prompt figures corrected from the tree**: "27-ish" → 21
   predicted-identical roots; ":626-770" → :649-767; `components/oracle`
   → `components/oracle/src/ehrt/oracle/digest.clj:544-579`.

## Commits

| sha | what |
|---|---|
| `760f81d` | ADR-0142 opens — field audit, mover prediction, strip census |
| `ca64f5a` | red — OBR-7/OBX-14 on all three ORU shapes, unshifted under `emit-wire` |
| `14e718f` | green — the fields render; movers as predicted |
| this | docs — manual ch. 4, architecture §5, demos regenerated, registers |

`bin/post-push-verify` ran after every push, all three checks recorded
each time; `gitleaks` clean on every commit.

**No tag owed or taken beyond Step 0's two.** This session's own close
tag defers to the next session's Step 0 under the standing conditional
licence.
