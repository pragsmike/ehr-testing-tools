## ADR-0150 — event-log shape defects: the Z-segment context asymmetry and `:units` closed, S-1 stopped by its own contract gate

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-18.

### Context

`roadmap.md#event-log-shape-defects` carried six items out of the
event-log contract arc by explicit ruling (2026-08-16, ADR-0141):
S-1, S-2, S-4, S-5, S-6 and the Z-segment context asymmetry stay
register rows, *"describing current truth first, then changing it
under the versioned event contract"* being the point of the tier the
contract publishes at. ADR-0149 gated `demos/traces/**` first, on the
ordering argument that this session's changes move those traces and
nothing would have noticed; that gate is now the reason this session
can claim byte-frozen rather than assert it.

### Step 0 — baseline and three predictions

`bin/preflight` findings, disclosed before proceeding:

- **A RED run among the last five on `main`** — `32091482306` @
  `76b4e20d`, step `poly test :all skip:integration`, one failure,
  `tracked-scripts-are-executable-in-the-index-test`
  (`executable_bits_test.clj:55`). It is ADR-0149's own executable-bit
  miss: `git diff --summary 76b4e20 e6f9c13` reads `mode change 100644
  => 100755 bin/regen-traces`. Green at `e6f9c13` (32092909614) and
  twice at `cfe6a73`. Not a live defect, and the third instance of this
  clone's `core.fileMode=false` biting a new `bin/` script.
- Edit root on ext4, tree clean including untracked, HEAD ==
  `origin/main` at `cfe6a73`, last tag
  `stable-20260817-demos-traces-gated` @ `e6f9c13`, HEAD untagged and
  no tag owed.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **346** zero-failure
blocks / **3,918** tests / **17,610** assertions — reconciling exactly
against ADR-0149's 346 / 3,918 / 17,610. `clojure -M:poly check` OK.
Reading sets all green (`:corpus` 1799/2045, `:docs` 706/785,
`:judge` 893/1000, `:onboarding` 1378/1530, `:sim` 1245/1405).

Then, **before any `src` edit**, three predictions written from the
live tree (ADR-0142's movers discipline, applied to all three steps
rather than only the declared-oracle one).

**(a) Z-segment movers: ZERO** — and for a structural reason, not a
lucky one. `components/oracle/src/ehrt/oracle/digest.clj:171` holds
the only emitter call the oracle makes,
`(emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)`
— the FIVE-argument arity. `emit_hl7.clj:930` is the six-arg arity that
takes `site-profile`; the five-arg one supplies none, and
`z-segments-for` iterates `(:z-segments site-profile)`, which is nil.
No oracle root constructs a site profile at all, so no oracle root can
witness a Z-segment change of any kind. **(a2)** the committed traces
do not move either: `demos/traces/site-profiles/config-aldric.edn` is
the only committed profile carrying `:z-segments`, and its three fields
are `[:persona :payer :id]`, `[:persona :payer :type]` and a literal —
`:persona` being `assoc`ed on by `context-for-event`
(`emit_hl7.clj:400`), it resolves identically through the seven-key
subset and through `ev`. The new test is therefore the **sole** witness,
which is what makes its red load-bearing.

**(b) S-1 is NOT contract-neutral** — the premise of the step's own
title fails. `:reason` at `event_schema.clj:260` (`:admission`) and
`:373` (`:outpatient-visit`) is in both places

    [:reason [:maybe [:or :string sim-model/Concept]]]

with no `{:optional true}`: a REQUIRED key of a `{:closed true}` map
whose value may be nil. Dropping the key when nil forces the entry to
`{:optional true}` or every module-compiled encounter stops validating.
`classify-change`'s own docstring enumerates the case — *"BREAKING
(bump owed): ... a required key made optional"* — so the predicted
verdict is two breaking rows, `:admission` and `:outpatient-visit`,
each `key changed: :reason (required -> optional)`.

**(c) S-6** — the event-side `:units` census, and the bump owed. Write
at `engine.clj:637`; reads at `engine.clj:967-968` (which already
translates to `:unit`) and `emit_hl7.clj:646,654` (OBX-6); schema at
`event_schema.clj:155,161`; one test at `sim-check/check_test.clj:426`;
generated `event-schema.edn:393`, `event-examples.edn` (5),
`demos/traces/emit-state/ground-truth.edn` (15),
`demos/traces/order-result/ground-truth.edn` (15), `docs/formats.md`
(6, all inside the `EVENT-LOG-GENERATED` region at `:348-1089`); frozen
`event-schema-baseline.edn:392`. Deliberately untouched by the author's
scope ruling: the 13 analyte entries in `order-profiles.edn`,
`order_profiles.clj:44`, `order_profiles_test.clj:34` and
`docs/cli.md:104` — `:order-profiles` is a user-reachable `--config`
key — **plus one the channel probe did not name**,
`sim-trajectory/resources/sim-trajectory/vital-signs.edn` (`:38` and 24
entries), a different config table feeding `:observation`, which
already emits `:unit`. Predicted class: `branches` keys on top-level
entries per kind, so a rename inside the nested `ResultEntry` surfaces
as `:result-available: key changed: :results (value schema changed)`,
and the policy at `event_schema.clj:82-84` owes **1.0.0 -> 1.1.0**.

One probe figure corrected from the tree: `ed-tuesday`'s config carries
`{:type :order :profile :cbc}` at two pathway sites, so the manual's
ground-truth-invariance digest **does** sit downstream of S-6.

### Step 1 — the Z-segment asymmetry (emitter, declared oracle change)

`single-subject-message` built a seven-key map and handed THAT to
`z-segments-for`; the six other builders handed `ev`. `render-z-field`
renders an empty field rather than throwing on an unbound path, so a
site profile binding a Z field to `:reason`, `:home-ward`,
`:disposition`, `:warm-up`, `:forced`, `:bed-ready` or `:citation` got
a blank on every ADT message, a value everywhere else, and no
diagnostic either way.

The red is the census's own four-row reproduction turned into one
test: one run, one template, two families.

    FAIL (emit_hl7_test.clj:996)
    expected: (= "false" (message/get-field-first-value adt "ZWU" 1))
      actual: (not (= "false" ""))
    FAIL (emit_hl7_test.clj:998)
    expected: (= (:home-ward admission) (... adt "ZWU" 2))
      actual: (not (= "Renal" ""))
    201 passes, 2 failures

Both ORM control assertions passed, which is what makes it an
asymmetry test and not merely a Z-segment test. The template binds
`:warm-up` (universal — an empty rendering can only be the bug) and
`:home-ward` (ADT-only — so its emptiness on the ORM message is DATA,
and the test asserts that too rather than leaving absence unexamined).

Green: the literal deleted, `ev` passed, `z-segments-for`'s docstring
carrying a dated paragraph naming what changed. All seven call sites
now pass the same map.

**Prediction vs actual: EXACT MATCH, no residue.**

    predicted movers  0        actual movers  0
    predicted traces  frozen   actual traces  frozen

`bin/regression-oracle cfe6a73 <green>` — exit 0, `IDENTICAL: every
root's digest matches`, with the soundness line reading `IDENTICAL
outside the (ns ...) form` so both sides ran the same instrument.
`make traces` regenerated every trace and `git status demos/traces/`
came back empty.

`docs/site-profiles.md`'s claim that "any path into the per-render
context ... resolves today" was FALSE for the ADT family and is now
true. It is dated in place rather than rewritten: the sentence did not
change, the emitter caught up to it.

**A gate caught the first attempt at that doc edit**, and it is worth
recording rather than quietly fixing:
`no-visible-adr-token-in-prose-test` failed with `docs/site-profiles.md
has visible ADR-NNNN token(s) in prose: #{"ADR-0150"}`. `docs/` prose
cites ADRs through footnotes, never as bare tokens (ADR-0102). The
note was reshaped to `[^adr-0150]` with its definition beside the
others. The gate did its job on the first run that could see the edit.

### Step 2 — S-1 STOPPED, by the step's own pre-committed instruction

The prompt titles Step 2 "S-1 (engine, **contract-neutral by
prediction**)" and names the response if the premise fails: *"If it
calls the change non-additive, STOP — do not bump here; S-6 owns the
bump and the two would be conflated."* The premise failed, and
prediction (b) said so before the edit.

Actual, from `classify-change` over the frozen baseline and the live
export:

    baseline-version: 1.0.0
    live schema-version: 1.0.0
    additive?: false
      BREAKING: :admission: key changed: :reason (required -> optional)
      BREAKING: :outpatient-visit: key changed: :reason (required -> optional)

**EXACT MATCH with prediction (b), no residue in either direction.**

It is not an implementation choice. `:reason` is a REQUIRED key of a
`{:closed true}` map, so there are exactly two shapes: keep the schema
and stop emitting the key (every module-compiled encounter fails
`valid-event?`, property test red), or mark the entry
`{:optional true}` (breaking by the classifier's own docstring). There
is no third. S-1 cannot land without a bump, and the bump it needs
belongs to the step this session is fenced out of conflating it with.

Both halves were nevertheless written and RUN before reverting, so
what follows is measured rather than asserted:

- red, unfixed engine — `Ran 1 tests containing 4 assertions. 2
  failures, 0 errors.`, the two nil cases failing (`expected: (not
  (contains? (first events) :reason))`, `actual: (not (not true))`),
  the two hand-authored-reason cases passing — the census's own
  221/221-vs-real-string split;
- green, with `reason-field` beside `citation-fields` — `Ran 1 tests
  containing 4 assertions. 0 failures, 0 errors.`

`citation-fields` was NOT extended, and the prompt asked this be said
out loud: its docstring scopes it to glass-box TRACEABILITY of what
the compiler supplied (`:citation`/`:conditions`, gmf-interpreter.md
section 6 obligations 1/3), while `:reason` is clinical content a
hand-authored step supplies. Same nil-dropping shape, different reason
to exist, so a sibling function rather than a widened one.

Nothing was committed. The three files are back to `cfe6a73` content
and were preserved outside the tree so the follow-on row does not
re-derive them.

### Step 3 — S-6, the one version bump

Author ruling on the scope collision the channel probe found, executed
as given: rename the EVENT key only. `ResultEntry`'s `:units` becomes
`:unit`; the order-profile ANALYTE key stays `:units`, being a
user-reachable `--config` surface (`docs/cli.md`, `:order-profiles`),
and `engine.clj`'s one result-construction site translates between
them — the same one-place translation `evolve :result-available`
already performed downstream, which now reads `:unit` straight through
instead.

Red across four surfaces, three of which failed:

    event_schema_test/result-entries-carry-unit-singular   3 of 5
    emit_hl7_test/oru-obx6-...-singular-unit               3 of 4
    manifest_test/manifest-carries-the-bumped-...-version  1 of 1

The sharpest line is the emitter's — `expected: (= (:unit first-entry)
(... "OBX" 6))`, `actual: (not (= nil "K/uL"))` — which states the
defect exactly: OBX-6 is correct today only because the emitter
reaches for the plural spelling.

**DISCLOSED, and not counted among the reds:** the fourth surface,
`check_test`'s `legit-order-result-log` fixture, does not go red on
its own. `check.clj:426` destructures `:value`/`:reference-range`/
`:abnormal-flag` and never reads the unit at all. Its edit is fixture
fidelity to the renamed contract, not a gated behaviour, and reporting
it as a red would have inflated the evidence.

**The bump, quoted from the policy it obeys** (`event_schema.clj`,
`schema-version`'s own docstring):

> MINOR or MAJOR for anything else -- a key removed, an optional key
> made required, a value schema changed, a kind removed.

Predicted class (c) and actual agree: `branches` keys on top-level
entries per kind, so the nested rename surfaces as `:result-available:
key changed: :results (value schema changed)`. **1.0.0 -> 1.1.0**,
MINOR because exactly one key of one nested schema moved.

**A second clause of that same policy was NOT honoured, and this is
the session's own disclosure rather than a finding against it.** The
policy continues: *"A key or kind slated for removal is marked
deprecated in `docs/formats.md` for one minor release BEFORE it goes,
so a consumer gets a release in which to notice."* No deprecation
release was run. 1.0.0 was published 2026-08-16, two days before this
change, with `ResultEntry`'s own docstring already naming `:units` as
census defect S-6 at the moment of publication — there is no consumer
release in between for a window to protect. The reasoning is recorded
in `schema-version`'s docstring rather than only here, so the next
removal is told plainly that it owes the window.

**The regenerated surfaces, by byte size and change class** — the
READ-BACK fence this session's prompt set, with an "other" as a STOP:

| artifact | before | after | Δ | change class |
|---|---|---|---|---|
| `resources/sim-engine/event-examples.edn` | 9,102 | 9,097 | −5 | 5× `:units`→`:unit` |
| `resources/sim-engine/event-schema.edn` | 19,800 | 19,799 | −1 | 1× rename + 1× bump stamp |
| `resources/sim-engine/event-schema-baseline.edn` | — | — | — | 1× rename + 1× bump stamp (the ONE re-freeze) |
| `demos/traces/emit-state/ground-truth.edn` | 8,785 | 8,770 | −15 | 15× `:units`→`:unit` |
| `demos/traces/order-result/ground-truth.edn` | 8,785 | 8,770 | −15 | 15× `:units`→`:unit` |
| `docs/formats.md` | 57,524 | 57,518 | −6 | 5× rename, 1× key-table row, 1× bump stamp |

Every changed line across all six is one of exactly two declared
classes — the rename (52 lines) or the bump stamp (2 lines). No
"other" appeared, so no STOP fired. The other four trace directories
(`boarding-transfer`, `module-mix`, `persona-enriched`,
`site-profiles`) carry no `:result-available` and did not move.

**The manual's ground-truth-invariance digest, re-witnessed rather
than trusted** (ADR-0142's discipline). Both of chapter 4's own
commands re-run with the out-dirs cleared first:

    diff  ... silent
    d00bf49c5df558b0fba91465090d533c09213d3183e500d0e903483f0c6842ca  .../ed-tuesday-base/events.edn
    d00bf49c5df558b0fba91465090d533c09213d3183e500d0e903483f0c6842ca  .../ed-tuesday-latency/events.edn

Old `b4e776f7…`, new `d00bf49c…`. **The digest MOVED and the property
it witnesses did not**: `diff` is still silent and the two out-dirs
still agree, which is the whole point of that transcript. The move is
the rename and nothing else — the regenerated `events.edn` contains
zero `:units` and 140 `:unit`. The chapter's evidence register now
records the move, why it happened, and that the property survived it,
rather than showing a digest that would silently fail to reproduce.

One channel-probe figure corrected from the tree: the probe treated
the manual digest as a surface that "will move" only if `:reason`
moved. It moves under S-6, because `ed-tuesday`'s config carries
`{:type :order :profile :cbc}` at two pathway sites.

**The manifest assertion the prompt asked for, from a real run rather
than a unit fixture:** `out/scenarios/ed-tuesday-base/manifest.edn`
reads `:event-schema-version "1.1.0"`, and `manifest_test`'s own case
asserts it so a future bump cannot forget the stamp.

### Step 4 — register hygiene

**S-4: CONFIRMED CLOSED, no code owed.** The census asked that
`:step-rejected`'s `:reason` schema reference the engine's own var
rather than an observation-derived set, so a schema narrowed to the 1
of 7 reasons that happened to occur could not reject a legal log. It
already does: `event_schema.clj:478` reads `[:reason (into [:enum]
(sort engine/documented-step-rejection-reasons))]` against
`engine.clj:486-496`'s seven-member set, and the comment above it
already names census S-4. Re-derived at `cfe6a73`, marked CLOSED with
its date in the census file.

**S-2: FOLDED into `roadmap.md#careplan-guard-resolution`.** The
census's own corrected row establishes that the nils are not a
mechanism failure — Step 2's fixture module resolves both fields end
to end — but an unported resolution shape: 4 of the 12 vendored
`CarePlanEnd` states cite by `referenced_by_attribute`, which
`ehrt.sim-trajectory.gmf` leaves UNDECLARED because the declared D2
vendoring scope exercises neither `:assign-to-attribute` nor
`:referenced-by-attribute`. That is the same D2-scope cause the
care-plan row already owns, so it becomes one row rather than two.

**S-5: its own row**, `roadmap.md#surge-policy-self-check-202`.

**S-1: RE-ROWED, not dropped.** The row it earns is narrower and more
useful than the one it came from, because this session established
mechanically what it costs: the fix is written and proven, and what
blocks it is one version bump it may not share with S-6.

### Step 5 — the rider

`R-full-suite-before-push` joins `.agents/rulings.md`. ADR-0149's
finding 3 was the second instance in two weeks of a push preceded by
`poly test brick:` rather than the full suite (after ADR-0147's S-7),
and no register row said otherwise — R30's step 4 merely assumed it.
The rule names why the shortcut is wrong rather than only that it is:
tree-scanning gates live in bricks other than the one being changed,
so a brick-scoped run is structurally blind to them.

**This session is its own evidence for the rule.** The Step 1 doc edit
was to `docs/site-profiles.md`; the gate that caught it,
`no-visible-adr-token-in-prose-test`, lives in `docs-tooling`. A
`poly test brick:sim-emit-hl7` run — the natural one for a change to
the HL7 emitter — could not have seen it. The full suite did, on the
first run that could.

**The skill mirror, and a fence honoured rather than stretched.** The
prompt conditions the skill edit on the skill stating a push step
(`iff`). Grepped: `.agents/skills/build-session/SKILL.md` mentions
pushing in steps 3, 5, 10 and 16, but has no step that instructs the
push itself — step 10 is `post-push-verify`, step 16 is
red-with-green. Rather than invent a step, the sentence was appended
to the Verification bullet that already states this exact discipline
(*"Every gate run goes to a full log with its exit code captured
explicitly"*), which is an amendment to an existing statement rather
than new structure. `.claude/skills` re-synced with `cp -p` and
`diff -r .agents/skills .claude/skills` is empty (ADR-0143).

### The one prediction that had residue, and how it surfaced

Prediction (c) claimed a complete census of `:units` consumers. Its
`src` half was complete and correct. Its TEST half was **one short**,
and the full suite is what found it:

    FAIL in (result-available-emits-oru-with-one-obx-per-analyte-in-profile-order)
    (emit_hl7_test.clj:565)
    OBX-6: units
    expected: (= units (field 6))
      actual: (not (= nil "K/uL"))          ... and four more, one per analyte

The cause is a method error worth naming so it is not repeated. Two
greps built that census: one for the KEYWORD `:units`, which found
`check_test.clj:426`; and one for the bare word `units`, which was
filtered with `grep -v test/` to keep the `src` list clean. The missed
site destructures with `{:keys [concept units ...]}` — the bare
SYMBOL, in a test file. It fell in the gap between the two greps'
exclusions. Re-run properly (`grep -rnw units` over `components/`,
`bases/`, `bin/`, `development/`, no test exclusion), the census is
clean: every surviving occurrence is either prose about this rename,
the deliberately-retained config key, or `v2_replay`'s docstring —
and `v2_replay/parse-obx` itself already wrote `:unit`, since it
builds an ObservationRecord rather than a ResultEntry.

This is the rider rule earning its place twice in one session. A
`poly test brick:sim-emit-hl7` run WOULD have caught this one; a
census trusted instead of a gate would not have.

### Close

**S-6 closed; the Z-segment asymmetry closed; S-4 confirmed closed
with no code owed; S-2 folded; S-5 rowed; S-1 stopped and re-rowed.**

`roadmap.md#event-log-shape-defects` moves to `## Done` naming exactly
that residue, so the row's disappearance does not imply six closures
where there were four.

What this session actually settles, beyond the three fixes: the event
contract's versioning machinery has now been exercised end to end by a
real change rather than by its own unit tests. A rename was proposed,
classified mechanically as breaking, bumped, re-frozen, re-exported,
re-rendered into `docs/formats.md`, stamped into a real run's manifest,
and re-witnessed against the manual's own transcript — and the gate
refused to let the change through until each of those had happened.
S-1's STOP is the same machinery working: a change that should be easy
was correctly identified as one the contract charges for, and it
stopped rather than quietly conflating its bill with S-6's.

### A second pinned test re-baselined, and why the literal was the bug

The third gate run surfaced one more, from a different direction:

    FAIL in (the-result-clock-increment-does-not-touch-the-event-log-contract)
    (result_clock_test.clj:243)
    expected: (= "1.0.0" engine/event-schema-version)
      actual: (not (= "1.0.0" "1.1.0"))

ADR-0142 asserted its own contract-neutrality by pinning the version
to the literal `"1.0.0"`, twice, across both halves of the
two-artifact gate. That pin conflated two different things: the
property ADR-0142 owned — that ITS change moved nothing — and the
accident of the number the contract happened to sit at while it did.

The property was proven when ADR-0142 landed green, and is history.
The literal is not: left alone it breaks every later LEGITIMATE bump,
and what it teaches the session that hits it is to edit the line
rather than think about it. **Re-baselining it to `"1.1.0"` would have
been the wrong fix** — it would assert nothing about ADR-0142, and
would break again at 1.2.0.

So the change is SEMANTIC, in ADR-0142's own precedent for re-
baselining a pinned test. The live, version-independent property that
survives is asserted instead: the emitter component's own view of the
contract version agrees with the committed export's stamp. Same two
assertions, same two halves of the gate, no literal to re-baseline
next time, and the docstring records what was pinned, why it was
wrong, and which change broke it.

A sweep for the same class found no other hard-pinned contract
version: `grep '"1\.0\.0"'` across `components/`, `bases/` and
`development/` returns only unrelated artifact/tool versions.

### Receipts

    push 1   cfe6a73..ee63a7b   both red/green pairs
    push 2   ee63a7b..eeb0299   the close
    CI       run 32137738307 @ eeb0299 -- completed, success
    tag      stable-20260818-event-log-shape-defects @ eeb0299,
             paid in session, remote peeled ref verified

`bin/post-push-verify` ran after both pushes: remote tip matched, every
commit message in range pure ASCII, CI reported once per AR-CI-4.
