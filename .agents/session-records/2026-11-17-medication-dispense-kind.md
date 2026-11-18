# `:medication-dispense` joins the log as an authored step: one kind, two settings (event schema 1.12.0, describe 1.3.0, ADR-0186)

2026-11-17. Base `dc58500f`. Ceremony: R30, taken from the session
prompt, which stated no prepare-only mode. `bin/preflight` exit 0, no
findings (last five CI runs green; HEAD matched origin/main; tree
clean; HEAD not tagged stable-*, disclosed). All commits were held
locally and pushed together with this record, so the red commit
`fd37f4f2` reached the remote only alongside its green successors and
no red tip reached CI.

## For downstream

**Event schema 1.12.0**: a new kind, `:medication-dispense` --
`:active-mrn`; `:setting`, required, `:inpatient` (a pharmacy dispense
to the unit, legal only inside an open encounter) or `:community` (a
fill of a prescription, legal with no encounter open);
`:order-event-id`, the log index of the `:medication-order` it fills,
same patient, typed `[:maybe :int]` as `:medication-end`'s is;
`:order-citation`, that order's citation, never nil; `:citation` when
the step states one; plus the four keys every event carries and the
`:encounter-id` stamp when `:encounters` is on. It does NOT repeat the
order's `:codes` and carries no fill number: join on
`:order-event-id`. A dispense is at or after its order and at or
before that order's end. No HL7 message and no FHIR resource yet.

**No existing corpus byte moves** (both instruments below: one added
line each, every existing root identical). No shipped scenario and no
vendored module produces the kind, and no rule derives a fill from an
order; **author the step in your own pathway to produce it today** --
`{:type :medication-dispense :setting :inpatient :order-citation {:module "my-pathway" :state :the-order}}`
after a `:medication-order` step carrying that same `:citation`.

## Author rulings

- 2026-11-17, "Agree on all. Go.", to three channel recommendations:
  one kind with a required `:setting` of `:inpatient` or `:community`;
  authored steps need no key and derived fills get an engine key
  `:dispense` gating only the derived half; a new small oracle root as
  a first baseline, leaving `dense-7500` byte-identical for now.
- 2026-11-17, earlier, "b": chronic orders refill at a fixed period. A
  later slice; the period is not ruled.

Everything else in the prompt was channel expectation, corrected from
the tree below.

## Where the tree differed from the prompt

1. **operators.clj gains nothing.** The prompt: "the kind sets in
   operators.clj gain it". Not done, on two grounds. (a)
   `therapeutic-intent-kinds` is the orphan operators' subject and they
   declare a four-finding set; reattributing a dispense also trips its
   own referential law, a fifth. (b) The mutation column: the tree's
   enforced rule
   (`every-schema-reference-field-is-covered-or-declared-empty-test`)
   is keyed by FIELD NAME, `:order-event-id` is already covered by two
   columns, and the gate stayed green untouched; a column needs a
   shipped log carrying dispenses to calibrate against, and there is
   none. Named as a follow-on in ADR-0186, as the prompt allowed.
2. **"The fleet fixture gains one step" is two steps in the death
   run.** The clinical fixture is a GMF module and cannot author a
   pathway step, and a dispense needs an order to cite. The death run
   is the last pathway run to supply any first example, so its two new
   events (an authored order and its `:inpatient` dispense) move no
   other kind's example and no log index another example cites.
3. **Describe is two rows, not one.** The prompt asked for a family row
   "counting by `:setting` if the catalog's shape allows it without a
   new mechanism". It does, as two rows --
   `:medication-dispense-inpatient` and `:medication-dispense-community`
   -- since a row's `:observed-by` is any predicate. One row cannot
   count by setting.
4. **A pin the prompt did not name.** sim's `run-test`
   `arc0-invariant-catalog` pins `check-all`'s reported names; its own
   message says a red is a STOP and not to re-pin. Re-derived: no
   FINDING moved (`:status :ok`, `:events` unchanged on all four
   corpora); the roster gained one row, vacuous on a log with no
   dispense. Extended 46 -> 47 as an ADDITION with its dated note, the
   file's own rule for an added row (seven earlier re-pins, same
   terms).
5. **The instruments say DIFFERS, not IDENTICAL.** Each exits 1 because
   the target has one more root; every existing root's digest is
   identical. That is the stated invariant, and ADR-0184's result too.
6. The bump is TAKEN, not owed (`classify-change` additive). The prompt
   said 1.11.0 -> 1.12.0 and that is what landed.

## Step 1 -- red (`fd37f4f2`)

By namespace with `clojure.test` (poly halts at the first failing
namespace): **2 failures, 3 errors, and one namespace that does not
load**, each for its stated reason --

- pathway-test (2 FAIL): `:medication-dispense` is not valid IR. The
  refusal test passes at the baseline -- a control.
- engine-test (3 ERROR): the pathways config assert refuses the step
  (twice); `No method in multimethod 'decide' for dispatch value:
  :medication-dispense`.
- check-test: `No such var:
  check/medication-dispense-references-existing-order-and-follows-it-in-time`,
  so its six new tests, the admitted-only one included, could not run
  red individually.

## Step 2 -- the kind (`d98613bd`)

As the commit message lists. `classify-change` against the frozen
1.11.0 baseline, before the re-freeze: `{:additive? true, :breaking
[]}`. Invariants: `run.clj` diff empty; step 1 green (11 tests, 26
assertions, by namespace); the ten affected bricks green (exit 0, 0
failures) on the second run -- the first found mismatch 4 above and
nothing else; `poly check` OK. The examples diff adds the
`:medication-dispense` example and moves nothing else. The catalog is
47 rows (43 + 2 + 1 + 1).

## Step 3 -- the producer (`c2466f9c`)

Config `{:seed 20261117 :patients 12 :arrival-gap 90}`, explicit
ordinals, no module. Even ordinals: an authored Renal stay (order,
`:inpatient` dispense, end, discharge). Odd ordinals: an authored
outpatient visit whose order is filled `:community` two hours after the
visit closes. **66 events**: 12 `:registered`, 6 `:admission`, 6
`:discharge`, 6 `:outpatient-visit`, 6 `:outpatient-visit-end`, 12
`:medication-order`, 6 `:medication-end`, 12 `:medication-dispense` (6
`:inpatient`, 6 `:community`); every `:order-event-id` a real order of
the same patient; `check-all` ok.

Landed as the oracle's 44th root, `dispense` (`engine-pair`, a FIRST
BASELINE; `witnessed-event-kinds` 30 of 31 -> 31 of 31,
`witnessed-message-types` unmoved; root-count pins 43 -> 44 and 40 ->
41) and as
`ehrt.sim-engine.engine-test/both-dispense-settings-reach-the-log-and-self-check-clean`.

**The instruments**, `dc58500f` -> `c2466f9c`:

* `bin/ground-truth-bracket dc58500f c2466f9c --declared-digest-change`
  -- **DIFFERS, exit 1, and the diff is one added line**:
  `+d67a3a6f1ffed5d4e2b2dd129d09bd8de361ef590a3a765811053822eba21bda  dispense.edn`.
  Every existing ground-truth root IDENTICAL; 3 interpreter batches
  skipped by name on both sides (appendicitis, ear-infections,
  sore-throat). Its banners: "coverage: 40 roots carry :ground-truth
  and are digested; 3 skipped" (the BASELINE side's count; the target
  lists 41), "declared-digest-change: yes", "THIS IS NOT A
  REGRESSION-ORACLE CLAIM: the :hl7 half of every root is excluded by
  construction".
* `bin/regression-oracle dc58500f c2466f9c --declared-digest-change`
  -- **DIFFERS, exit 1, and the diff is one added line**:
  `+bd731cabbd959f1eeba0202b1c2556244b55a65584665baafe698a7519e22d82  dispense.edn`.
  All 43 existing roots IDENTICAL. Run with the flag from the start:
  digest.clj differs outside its docstring by the new root, which is
  the declared change.

Roots whose digest moved: none. Roots added: `dispense`, target side
only. The instruments ran at `c2466f9c`; the three commits after it
touch describe, docs and the ADR, and no engine, emitter or digest
source.

## Step 4 -- describe 1.3.0 (`8cc11eb0`)

Two `:opt-in` rows, witness field `:order-event-id`; an `:inpatient`
dispense joins the `:during-encounter` predicate. The golden's whole
diff: the version string and the two rows, each
`:configured :unknown :observed 0 :witnesses []`. Pins moved:
describe-test, sim's `run-test` ("describe 1.3.0"), formats.md's
describe section. `ehrt.sim-check.describe-test`: 15 tests, 92
assertions, 0 failures.

## Step 5 -- docs (`ca964325`) and step 6 -- ADR-0186 (`644f6a83`)

As the commit messages list. No touched doc is a hand-owned asset
source (`hand-owned-assets.edn` checked). `make docsgen` exit 0 before
each commit, tree clean after it.

## Step 7 -- the suite

Full `make test`, unpiped to a log, wrapper ending `exit
"$MAKE_EXIT"`, at `644f6a83` (all six commits, before this record
existed): **MAKE_EXIT=0** -- 432 `Test results` lines, 29,669 passes, 0
failures, 0 errors; poly 28 min 18 s; its own `verify-nist-lock` six
matched. This record and the prompt archive were added after it, with
`make docsgen` and the docs-tooling brick tests; no second full run
(they are a docs-only addition, AGENTS.md 22).

One process slip, disclosed: the suite was first started with a shell
`&` instead of the harness's background mode, which left it running
with no completion notice. It was stopped by PID within seconds
(`make` 53182 and its shell), nothing else matched, and the run above
is a fresh one.

## Findings, not acted on

1. **The orphan operators' four-set is a property of the corpus, again**
   (mismatch 1). The day a shipped corpus carries a dispense, any
   operator that moves or drops an order or its end may trip the
   dispense law as an extra finding. Owed with the mutation column.
2. **A dispense of a pre-horizon order is not bounded by that order's
   end.** The law's straddle branch cannot find the end (it carries no
   index). No producer writes such a dispense. ADR-0186 names it.

CI green on the pushed tip is this session's close marker.

## Close

Pushed `dc58500f..3f92de1b` (seven commits, the red `fd37f4f2` with its
green successors). `bin/post-push-verify dc58500f 3f92de1b`: remote tip
matches; every commit message ASCII; CI not yet indexed at that moment
(disclosed by the script). **CI run 44205175355 completed `success`**
at `3f92de1bfcd684dd3bbc3662c6ea4d7741bdb80d`, attempt 1 -- the close
marker. Every background process this session started was enumerated
and had exited before this commit (two brick runs, the two instruments
in one job, one `make test`, one `gh run watch`, each by its own
completion notification; the one stray `make test` named above was
stopped by PID); no oracle worktree remained (`git worktree list`). No
tag paid.
