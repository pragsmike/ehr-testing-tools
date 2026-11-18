# The `:dispense` key: one community fill derived for each order an outpatient visit wrote (ADR-0187; event schema 1.12.0 and describe 1.3.0 unchanged)

2026-11-18. Base `2aaefd6a`. Ceremony: R30, taken from the session
prompt, which stated no prepare-only mode. `bin/preflight` exit 0, no
findings (last five CI runs green; HEAD matched origin/main; tree
clean; HEAD not tagged stable-*, disclosed). All commits were held
locally and pushed together with this record, so the red commit
`b0d6096f` reached the remote only alongside its green successors and
no red tip reached CI.

## For downstream

**A new engine key, `:dispense`.** Set `:dispense true` in the run
config and every medication order an outpatient visit wrote, and left
open, gets one `:medication-dispense` with `:setting :community`. The
event has the shape event schema 1.12.0 already declares; **the schema
did not change**. Each derived fill:

- follows its visit's `:outpatient-visit-end` in the log, at that
  event's `:t`;
- carries no `:encounter-id`, with `:encounters` on or off, because it
  is after the visit;
- names its order by `:order-event-id` and `:order-citation`, and
  carries no `:citation` of its own;
- is the only fill of that order: one per order, however long the
  prescription runs.

Orders written during an admission get none, and an
emergency-department visit is an admission here. The key does not look
for a fill you authored: a pathway that authors a community dispense of
an order and runs with the key gets two.

**The key draws nothing, so it is the same hospital.** The same seed
with and without `:dispense` gives logs that differ by the dispenses
and by the log indices after them, and by nothing else: delete the
dispenses, renumber, and the keyless log is left, event for event.
Rendered HL7 v2 gains and loses no message; MSH-10 differs, because its
suffix is the event's log index.

**No existing corpus byte moves** (both instruments below: one added
line each, every existing root identical). No shipped scenario sets
the key. To get fills today, add `:dispense true` to your own config.

## Author rulings

Verbatim, from the prompt.

- 2026-11-17, "Agree on all. Go.", to: "Derived fills ... need an
  engine key. I recommend a `:dispense` key gating only the derived
  half".
- 2026-11-18, "2 ok as long as the time is after the encounter", to:
  "under `:dispense`, each in-log order written in a non-inpatient
  encounter gets one `:community` fill at the instant that encounter
  closes".

The channel's reading of the second, also from the prompt: the fill is
never inside the visit -- it follows the visit's closing event in the
log, at that event's `:t` or later, and does not carry the closed
visit's `:encounter-id`. That reading is what was built and what the
tests assert.

## Where the tree differed from the prompt

1. **The fills are queued, not returned beside the closing event.**
   The prompt expected `decide :outpatient-visit-end` to return its
   closing event followed by the dispenses: "One decide, no follow-up
   queued". The tree cannot emit them that way and meet the ruling.
   `fold/apply-events` stamps a batch off the world as it stood BEFORE
   the batch (`encounters/stamp-encounter`, `fold.clj` 1165), so a
   dispense in the closer's batch would carry the `:encounter-id` of
   the visit it is meant to follow. The decide returns the closer
   alone plus one `:schedule-followup` queue entry at the same `t`
   holding the dispense steps, the shape `decide :order` uses for its
   result. This is not the HALT the prompt names: the tree CAN emit a
   fill after the encounter, at the closing instant, without the
   closed visit's id, and does. The order's own instant was not used.
2. **Each derived fill is a second decide.** A consequence of 1. The
   queued step is ADR-0186's own authored step, so the event and its
   `:order-event-id` come from `decide :medication-dispense`,
   unchanged.
3. **Only an order still `:active` at the close is filled.** The
   prompt said one fill "per medication order this patient was written
   during that visit". ADR-0186's law bounds a dispense by its order's
   end, so a fill placed after the close, of an order the visit itself
   ended earlier, would be convicted by the catalog. Such an order
   gets none (`an-order-ended-inside-its-visit-gets-no-derived-fill`).
   No module root measured holds one.
4. **"Written during that visit" needs two readings.** A
   `MedicationOrderRecord` carries `:encounter-id` only when
   `:encounters` is on. With an id on the open encounter the records
   are matched by id; without one, by `:ordered-t` at or after the
   open encounter record's `:admitted-at`, which is enough because a
   patient has one encounter in such a run.
5. **Step 1 (e) is not observable as written.** "Every stream's
   position after the run equals the keyless run's" has no instrument:
   `run` returns no stream. The no-draw claim is tested where it can
   be -- `deriving-a-fill-takes-no-draw` hands the closing decide a
   `Random` and finds it where a fresh one starts, and ADR-0186's
   `a-medication-dispense-takes-no-draw` does the same for the queued
   step -- and it is implied by step 3's whole-log equality, which a
   single extra draw anywhere would break.
6. **Two of step 1's tests are green at the base, by construction.**
   (c) "an order written during an admission yields none" and (d)
   "without the key the same runs emit no dispense" are true before
   the key exists. Six of the eight tests were red, 12 assertions.
7. **`describe-version` did not move.** Its rule (`describe.clj` 47):
   a new key or row is minor, a changed meaning or removed key is
   major. The community row's `:configured-by` gained a second way to
   read `:yes`; no row or key was added and no report of a run without
   the key reads differently. The golden's diff is empty. This is a
   judgement the author may want to overrule with a patch bump.
8. **The HL7 half moves with the key.** Not a premise the prompt
   stated, but its "every byte ... unchanged" is about absent-key runs
   only, and a reader could take the erasure law further than it goes.
   With the key, 310 of the `osteoporosis` run's 312 messages differ
   from their keyless twins, in MSH-10 only: the control id's suffix
   is the event's log index. Stripped of that suffix the two message
   vectors are equal. The docs say so.
9. **Four present-tense "authored only" statements were left by
   ADR-0186.** Two in docstrings that read as current
   (`event_schema.clj`'s 1.12.0 entry, `digest.clj`'s `dispense-pair`)
   got dated addenda. Two are inside dated notes about what was true
   on 2026-11-17 (`digest.clj`'s `witnessed-event-kinds`, sim's
   `run_test.clj` 1021) and were left.
10. **The prompt did not name the validity table.**
    `components/sim/docs/patient-state-model.md` said "Authored only --
    no rule derives one from an order", and was corrected.

Not a difference: the key is `:dispense true`, travels through
`config-keys` and is stamped on the manifest's `:engine-params`, as
expected.

## Step 1 -- red (`b0d6096f`)

`ehrt.sim-engine.dispense-derivation-test`, eight tests. At `2aaefd6a`:
12 failures, 0 errors, in six tests, each because no key exists and
nothing derives a fill. Two tests green, per difference 6.

## Step 2 -- the key and the derivation (`abe7cb27`)

- `config.clj`: `:dispense` joins `config-keys`, twenty keys.
- `run.clj` (sim-engine): destructured, and carried on the world as
  `:dispense` -- `(when dispense true)`, nil unless the run opted in.
- `decide.clj`: `derived-community-fills`, private, and
  `decide :outpatient-visit-end` attaching `:schedule-followup` only
  when it returns steps.
- `sim/run.clj`: `:dispense` in the manifest's `:engine-params`
  `select-keys`.

`classify-change` against the frozen 1.12.0 baseline:

    {:additive? true, :breaking []}

and `(= (committed-baseline) (export))` is true, as is
`(= (committed-export) (export))`. No schema form moved.

**With the key absent no code path differs.** `derived-community-fills`
returns nil at its first test, `(:dispense world)`; `cond->` then
returns the map the decide always built. The world gains one key whose
value is nil. No other form reads the flag.

Gates: the eight tests green (1,947 assertions before step 3's test
joined); `clojure -M:poly test brick:sim-engine brick:sim` exit 0;
`clojure -M:poly check` OK.

## Step 3 -- the erasure law (`6fed4a0c`)

`erasing-the-derived-dispenses-leaves-the-keyless-run-event-for-event`.
For each run: the keyed log, every `:medication-dispense` deleted,
every log-index field renumbered, compared with `=` to the keyless
log. The reference fields are walked out of `event-schema/Event` by
the walk `ehrt.corpus.event-mutate-test/schema-reference-fields` makes
(that fn is private to a test in another brick, so the walk is
restated; it reads the same schema).

| Run | Keyless events | Fills | Equal |
|---|---|---|---|
| oracle `osteoporosis` config, verbatim | 1,080 | 156 | yes |
| oracle `sinusitis` config, verbatim | 81 | 7 | yes |
| `osteoporosis` with `:encounters` | 1,080 | 156 | yes |
| two authored orders per visit, 3 patients | 15 | 6 | yes |
| authored visits and stays, churn, `:encounters`, `:bed-cycle`, 40 patients | 451 | 30 | yes |

Green as first written. The law was not adjusted. The fifth run is
there so the erased log holds reference fields other than a dispense's
(17 `:cancel-transfer`, 3 `:cancel-discharge`, 2 `:cancel-admit`) and
events of other patients at the fills' own instants; `check-all` is
`:ok` on it keyed and keyless.

## Step 4 -- the producer (`b0fd6851`)

`osteoporosis-dispense`, the oracle's 45th root and 42nd engine-layer
pair: `osteoporosis-pair`'s config plus `:dispense true`. FIRST
BASELINE.

| | `osteoporosis` | `osteoporosis-dispense` |
|---|---|---|
| events | 1,080 | 1,236 |
| `:medication-order` | 156 | 156 |
| written in an `:outpatient-visit` | 156 | 156 |
| written in an `:admission` | 0 | 0 |
| written outside any encounter | 0 | 0 |
| `:medication-dispense` | 0 | 156, all `:community` |
| distinct orders named | -- | 156 |
| HL7 messages | 312 | 312 |

Root-count pins 44 -> 45 and 41 -> 42: `digest.clj`'s prose,
docs-tooling's `oracle_coverage_test.clj`, integration's
`oracle_coverage_test.clj`, and `state-derived.md` (regenerated in
`f4b9bfeb`, one commit later than it was owed).

### The instruments

Both run `2aaefd6a` -> `b0fd6851`, the root's own commit, with
`--declared-digest-change`. The three later commits change no source
form a root executes: `describe_catalog.clj` is off the oracle's path,
and the other two source edits are docstring addenda.

`bin/ground-truth-bracket`, exit 1:

    --- coverage: 41 roots carry :ground-truth and are digested; 3 skipped (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
    --- declared-digest-change: yes (soundness: no outside the leading docstring) ---
    --- THIS IS NOT A REGRESSION-ORACLE CLAIM: the :hl7 half of every root is excluded by construction ---
    DIFFERS: :ground-truth diverges between 2aaefd6a and b0fd6851 -- STOP, escalate (do not fix under this script)
    @@ -25,6 +25,7 @@
    +9b4f65362e322ef7baea7608bad1c4403fd59e4963a466873486c4a527dd74e2  osteoporosis-dispense.edn

One added line. Every existing root's ground-truth digest is
identical, `osteoporosis`'s among them
(`96b1a94b...` both sides).

`bin/regression-oracle`, exit 1:

    --- declared-digest-change: yes (soundness: no outside the leading docstring) ---
    DIFFERS: digests diverge between 2aaefd6a and b0fd6851 -- STOP, escalate (do not fix under this script)
    @@ -25,6 +25,7 @@
    +f463b5be91b1ab570923b1a077ff8fa3273e87b0d2e492d6a3b6e77bf9c9b73c  osteoporosis-dispense.edn

One added line. All 44 existing roots are identical, ground truth and
HL7 both.

An exit 1 that is only the added line is the expected result for a
first-baseline root, as in ADR-0186.

## Step 5 -- describe (`4e62cca7`)

`medication-dispense-configured` reads `:yes` for `:community` when
the run's options carry `:dispense`. New test
`the-dispense-key-configures-the-community-row-and-only-that-row`.
`git diff 2aaefd6a -- components/sim-check/test/ehrt/sim_check/fixtures/`
is empty. `clojure -M:poly test brick:sim-check` exit 0. Version
unchanged, per difference 7.

## Step 6 -- docs (`f4b9bfeb`) and step 7 -- ADR-0187 (`f09cd029`)

As the commit messages list. `make docsgen` exit 0;
`git diff --exit-code` clean after the commits. Leak check over
`git diff 2aaefd6a`: nothing identifies the downstream consumer; the
tree's phrase is the only reference.

## Step 8 -- the suite

Full `make test`, unpiped to a log, twice.

**The first run, at `f09cd029`, exited 2, and the cause was this
session's own conduct, not the code.** While it ran I archived the
prompt and wrote this record into the working tree. The suite's three
index-freshness tests
(`every-generated-record-index-matches-a-fresh-render-test`, twice, and
`state-derived-md-matches-a-fresh-render-test`) then found a session
record and a prompt the generated indexes did not list. 3 failures, 0
errors, all three in that one docs-tooling namespace; no other test
result line in the log reports a failure.

**The second run: `MAKE_EXIT=0`.** Run after `make docsgen`, on
`f09cd029` plus the archived prompt, the three regenerated indexes,
and this record as it stood before its suite and close sections were
written. That is the tree then committed, but for those two sections
of this file, which no test reads. Every `Test results` line in the
log reports 0 failures and 0 errors; poly's own clock, 29 minutes 3
seconds.

Nothing was touched in the tree while the second run was going.

## Findings, not acted on

- **Two orders of one citation in one visit would both be filled
  against the later order.** The queued step resolves by citation and
  takes the patient's latest. Neither module root measured holds such
  a pair (0 in `osteoporosis`, 0 in `sinusitis`). A fix carries the
  order's log index on its state record. Named in ADR-0187.
- **A fill can be stamped with a NEW encounter's id** if, under
  `:encounters`, another queue entry for the same patient opens an
  encounter at the closing instant with a sequence number below the
  fills'. Not observed: the churn run's 30 fills carry no
  `:encounter-id`. The patient's own remaining steps always sort after
  the fills.
- **`index-of-citation`'s first-match rule**, from the 2026-11-17
  diagnosis, is unchanged and still unruled. It governs pre-horizon
  end facts, not anything this key reads.

## Close

Pushed as `2aaefd6a..80ba0c60`, eight commits: `b0d6096f` (red tests),
`abe7cb27` (the key), `6fed4a0c` (the erasure law), `b0fd6851` (the
oracle root), `4e62cca7` (describe), `f4b9bfeb` (docs), `f09cd029`
(ADR-0187), `80ba0c60` (this record and the prompt archive).
`bin/post-push-verify 2aaefd6a 80ba0c60`: remote tip matches, no
leaks, every commit message ASCII.

CI run 44310571204 at `80ba0c60`: **success, on its second attempt.**
The first attempt failed in the `poly check` step before any test ran:
Maven Central answered the runner's request for
`org.clojure:clojure:pom:1.12.6` with HTTP 429, Too Many Requests. No
`deps.edn` is in this range. The failed job was rerun unchanged
(`gh run rerun --failed`) and concluded `success`.
