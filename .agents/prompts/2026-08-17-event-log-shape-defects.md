# Session prompt — event-log shape defects: Z-segment context, S-1 nil `:reason`, S-6 `:units`/`:unit`, S-4 confirmed, S-2/S-5 re-rowed — ADR-0150

**Archived verbatim.** Authored in the design channel, 2026-08-17;
executed 2026-08-18 under R30. The session's own record is
[`2026-08-17-event-log-shape-defects.md`](../session-records/2026-08-17-event-log-shape-defects.md);
the reasoning of record is `notes/adr/0150-event-log-shape-defects.md`.

---

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: `cfe6a73`
(ADR-0149 addendum; tree clean; CI green at `e6f9c13` per addendum run
32092909614; last tag `stable-20260817-demos-traces-gated` @ `e6f9c13`,
no tag owed). Roadmap row `roadmap.md#event-log-shape-defects` (OPEN
PRIORITY 1): "S-1/S-2/S-4/S-5/S-6 and the Z-segment context asymmetry
stay register rows by ruling (2026-08-16): describing current truth
first, then changing it under the versioned event contract, is the
point of the tier. Evidence and full write-up in
`.agents/plans/2026-08-16-event-log-census.md`, section 'Shape
defects'." Now that `demos/traces/` is gated (ADR-0149), changes here
get caught by `make traces` + CI freshness instead of drifting
silently -- that was the ordering reason.

### Channel probe at `cfe6a73` (re-derive every one)

* **Z-segment:** `emit_hl7.clj:492` hands `z-segments-for` a
  synthesized 7-key map inside `single-subject-message`, whose own arg
  is destructured `{:keys [...] :as ev}` (`:470`), so `ev` is in scope;
  every other family (`:524 :548 :698 :732 :824 :860`) passes `ev`.
  `render-z-field` renders empty on a missing key, so an ADT Z field
  bound to `:reason`, `:disposition`, `:warm-up`, ... is a silent blank
  today. Emitter-only, DECLARED oracle change: moves exactly those
  oracle roots whose site profile binds a Z field to a key outside the
  seven -- predict the set BEFORE the edit (ADR-0142's movers
  discipline, `:414`).
* **S-1:** `decide :admission` (`engine.clj:387`) and the
  outpatient-visit decide (`:692`) merge `:reason reason`
  unconditionally; module-compiled steps carry none (census: 221/221
  outpatient visits, 48/692 admissions -- exactly the
  `:citation`-bearing ones -- have `:reason nil`). `citation-fields`
  (`:366-375`) is the nil-dropping pattern.
* **S-6:** `ResultEntry` (`event_schema.clj:152-166`) is `:units`
  PLURAL with a NOTE naming this row; `:observation`/
  `:diagnostic-report` use `:unit` (`:113`, `:403`), and
  `engine.clj:968` already translates `:unit units`. `:units` on the
  event is written at `engine.clj:637` from the order-profile analyte
  (`order-profiles.edn:28`, `order_profiles.clj:44` schema); consumed
  at `emit_hl7.clj:646/654` (OBX-6) and `check_test.clj:426`.
  `:order-profiles` is a user-reachable `--config` key
  (`docs/cli.md:104`).
* **S-4:** `event_schema.clj:478` already references
  `engine/documented-step-rejection-reasons` -- the census's ask.
  Confirm and CLOSE, no code.
* **Contract gate:** `event_schema.clj:73` `schema-version` "1.0.0";
  `event-schema-test/non-additive-change-requires-a-version-bump`
  against `event-schema-baseline.edn` (FROZEN, not on docsgen;
  re-freeze ONLY when bumping, `make event-schema-freeze`,
  `Makefile:175`).
* **Freshness surfaces that will move and are gated:**
  `demos/traces/**` (`make traces`), `event-schema.edn`/
  `event-examples.edn`/`docs/formats.md` event-log section (docsgen),
  the manual's ground-truth-invariance digest (`b4e776f7…`, re-witness
  per ADR-0142), `events.edn` fixture(s) -- census the population
  before editing.
* **Rider evidence:** ADR-0149 finding 3 (push after `poly test
  brick:` -- second instance in two weeks after ADR-0147 S-7); no
  `rulings.md` row says full `make test` precedes every push -- R30
  step 4 assumes it.

Three src changes, each its own red-before-green commit; the schema
bump isolated to S-6.

### Read first

1. `.agents/plans/2026-08-16-event-log-census.md` §"Shape defects"
   (`:558-668`) -- verbatim rows S-1..S-6, Z-segment (`:522-556`).
2. `event_schema.clj` (`:30-80` bump policy, `ResultEntry`,
   `:step-rejected`, `write-baseline!`); `event_schema_test.clj`
   (`:105-140`); `event-schema-baseline.edn` header.
3. `engine.clj` `:360-395`, `:630-640`, `:686-696`, `:960-972`;
   `order_profiles.clj` `:40-48`; `emit_hl7.clj` `:419-500`
   (`z-segments-for`, `render-z-field`, `single-subject-message`),
   `:640-660`.
4. ADR-0141 (contract), ADR-0142 (movers discipline; declared oracle
   change; the ground-truth-invariance re-witness), ADR-0149 (traces
   gate, finding 3), ADR-0139 C-2.
5. `docs/site-profiles.md` (Z-segment binding surface),
   `docs/formats.md` event-log section, `docs/manual/` chapter carrying
   the invariance transcript; `bin/regression-oracle`;
   `rulings.md#R-tag-law`, `#R-session-verifies-ci-via-gh`,
   `#R-stop-only-on-two-defensible-readings`, `#R-red-pushed-with-green`,
   `#R-register-hygiene-at-close`; build-session skill; `:sim` reading set.

## Author rulings, verbatim

* **Next:** "go" on `[event-log-shape-defects]` opened as Z-segment ->
  S-1 -> S-6, S-2 folded into `[careplan-guard-resolution]`, S-5 its
  own row; full-suite-before-push rider (channel option (a) on both
  questions, author "go", 2026-08-17). S-1: `:reason` rides the
  nil-dropping `cond->`/`citation-fields` treatment, no
  compiler-supplied reason (Q2 "b"). Standing: sessions verify CI via
  `gh`; F-3 narrowed (both now rows).
* **Tag:** no tag owed at Step 0. This session's own close tag: pay
  in-session if its tip run concludes success while open, else next
  Step 0 -- say which (`R-session-verifies-ci-via-gh`).

## Step 0

Fresh clone, tip `cfe6a73`; `bin/preflight`; baseline `make test`
unpiped, MAKE_EXIT captured, reconcile vs ADR-0149's 346 blocks /
3,918 tests / 17,610 assertions; `poly check`; reading sets vs
baselines. Then, before ANY src edit, three predictions written into
the ADR: **(a)** Z-segment movers: from the live tree, the oracle roots
whose site profile binds any Z field to a key outside `{:event :t
:active-mrn :location :from :attending :participants}`;
`predicted-movers.txt` as ADR-0142 did. If the answer is ZERO roots,
say so -- the fix still lands (the demos/traces site-profiles trace and
a new test witness it). **(b)** S-1 population: which committed
ground-truth artifacts (traces, `events.edn`, manual digest) carry
`:reason nil` and will lose the key; whether `classify-change` calls
dropping a nil-valued key additive (`:reason`'s schema type decides --
read it, predict, then run). **(c)** S-6: every consumer of `:units`
(grep src, test, resources, docs, demos); the bump the policy owes for
a key rename (read `:30-80`).

## Step 1 -- Z-segment (emitter, declared oracle change)

Red: a test in `sim-emit-hl7` binding a Z field to `:reason` (or
another non-seven key) on an ADT trigger and asserting it renders on
the ADT message as it does on a non-ADT one. Green: `emit_hl7.clj:492`
passes `ev`; delete the seven-key literal; amend `z-segments-for`'s
docstring if it describes the asymmetry. Run `bin/regression-oracle
cfe6a73 HEAD`; actual movers vs prediction (a) exact, no residue, or
STOP (two readings). `make traces` regenerates whatever moves;
`:event-schema-version` unchanged (assert). Commits: "test: red -- ADT
Z-segments see the whole event, not seven keys (ADR-0150)" / "fix:
single-subject ADT passes `ev` to z-segments-for; declared oracle
change, movers = <n> predicted = <n> actual (ADR-0150)"

## Step 2 -- S-1 (engine, contract-neutral by prediction)

Red: an engine test compiling a module-sourced admission and
outpatient visit and asserting `(not (contains? ev :reason))`; plus a
hand-authored step WITH a reason still carrying it. Green: at `:387`
and `:692` build the event with `:reason` only when present -- extend
`citation-fields` to `[:reason :citation :conditions]` iff its
docstring/contract admits it (say so), else a sibling `cond->`. Then
`event-schema-test`: prediction (b) vs actual. If it calls the change
non-additive, STOP -- do not bump here; S-6 owns the bump and the two
would be conflated. Regenerate gated surfaces (`make docsgen`, `make
traces`); re-witness the manual's invariance digest per ADR-0142 and
record old/new. Commits: "test: red -- module-compiled encounters
carry no nil :reason (ADR-0150, S-1)" / "fix: :reason rides the
nil-dropping path; S-1 closed (ADR-0150)"

## Step 3 -- S-6 (schema rename, the ONE version bump)

Ruling for the scope collision the probe found: rename the EVENT key
only -- `ResultEntry` `:units` -> `:unit`; the order-profile analyte
config key `:units` STAYS (user-reachable `--config` surface,
`docs/cli.md:104`) and is translated at `engine.clj:637` exactly as
`:968` already does. Red: schema/emitter/check tests reading `:unit`
from a `:result-available` entry. Green: rename at `:637` (`:unit
(:units analyte)`), `event_schema.clj` `ResultEntry` (delete the S-6
NOTE, add a dated one-liner naming the translation),
`emit_hl7.clj:646/654`, `check_test.clj:426`, `formats.md` prose if it
names the key. Bump `schema-version` per the policy's own text (quote
it in the ADR), then `make event-schema-freeze` ONCE, `make docsgen`,
`make traces`; the manifest's `:event-schema-version` in a fresh `sim
run` shows the new value (assert in a test). Commits: "test: red --
:result-available entries carry :unit, singular (ADR-0150, S-6)" /
"feat!: event contract <old> -> <new>: ResultEntry :units renamed
:unit; baseline re-frozen (ADR-0150)"

## Step 4 -- register hygiene (docs-only)

S-4: cite `event_schema.clj:478` in the census file's row as CLOSED
with date. S-2: append to `[careplan-guard-resolution]` one line naming
S-2 (census `:571-604`, the D2-scope cause) so the row owns it; S-2
marked "folded" in the census. S-5: NEW `## Next` row
`[surge-policy-self-check-202]` (six-line cap): seed 202 `--churn`
ed-tuesday facility exits `:self-check-failed`
`:surge-only-when-earlier-rungs-exhausted` at `t 78480` (census
`:644-646`); wanted: repro test + fix. `[event-log-shape-defects]` ->
CLOSED under `## Done` naming the residue rows.

## Step 5 -- rider (register-only)

`rulings.md`, three lines, lint green: `R-full-suite-before-push` -- a
push is preceded by full `make test` unpiped with MAKE_EXIT recorded;
`poly test brick:` and `project:` are development aids, never the
pre-push gate, because tree-scanning gates live in other bricks --
ADR-0149 (finding 3; ADR-0147 S-7). Add the same sentence to the
build-session skill's push step iff that skill states the push step
(grep; mirror to `.claude/skills` by whatever the mirror rule is --
ADR-0143). Commit: "docs: R-full-suite-before-push as a standing row
(ADR-0149 f.3, ADR-0150)"

## Close (self-archive FIRST)

Archive this prompt to
`.agents/prompts/2026-08-17-event-log-shape-defects.md` and open the
paired session record BEFORE the rest of the close. Then ADR-0150
(three predictions vs actuals; the movers file; digest old/new; the
bump quoted; S-4/S-2/S-5 dispositions), roadmap edits above, session
record with `gh run view` id/conclusion, full `make test` unpiped and
reconciled per namespace vs Step 0, `bin/post-push-verify`, tag per
ruling. Commit: "docs: ADR-0150 -- event-log shape defects close"

## Fences

src: `emit_hl7.clj:492` (+ docstring), `engine.clj` `:387`/`:692`/
`:637` (+ `citation-fields` iff chosen), `event_schema.clj`
`ResultEntry` + `schema-version`, `emit_hl7.clj:646/654`, and their
tests; NO parser/Z-render logic change; NO order-profile config key
change; NO GMF/compile change (S-2 is folded, not fixed); NO
surge-policy change (S-5 is rowed, not fixed); ONE
`event-schema-freeze`, in Step 3 only; oracle change DECLARED in Step 1
only, IDENTICAL in Steps 2-3 (assert with `bin/regression-oracle` at
each step); each step red-then-green, pushed as pairs
(`R-red-pushed-with-green`); full `make test` before EVERY push (the
rider, applied to itself); no test deletions; exit codes unpiped;
anchored register edits; R-RP. READ-BACK names the fence: per
regenerated artifact, byte size before/after and change class (Z-seg
movers / nil-`:reason` drop / `:units`->`:unit` / bump stamp) -- an
"other" is a STOP.
