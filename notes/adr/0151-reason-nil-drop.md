## ADR-0151 — census S-1 lands under its own bump (1.1.0 -> 1.2.0), and the deprecation clause gains a no-external-consumer waiver

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-18.

### Context

ADR-0150 wrote and proved census S-1's fix and then STOPPED it, for a
reason that was about the contract rather than the code: `:reason` is
a REQUIRED key of a `{:closed true}` map, so dropping it when nil
forces `{:optional true}`, which `classify-change` calls breaking.
The bump that buys is one S-1 may not share with S-6, so the row
survived as `roadmap.md#reason-nil-drop-owes-a-bump`, PRIORITY 1, with
ADR-0150 preserving the diff.

Two author rulings, 2026-08-18, opened this session: *"No consumers
yet, relax deprecation rules for now. Accept recommendations. go."* —
Q1 (a), the deprecation window is WAIVED while the event contract has
no consumer outside this repository, written into the policy so the
waiver expires by itself; Q2 (a), S-1 lands now, as its own session,
under its own bump.

### Step 0 — baseline, and four predictions written before any `src` edit

`bin/preflight` findings, disclosed:

- Last five CI runs on `main` all green. **One better than the
  prompt's own Context stated:** it recorded CI green at `eeb0299`
  per ADR-0150's addendum; the addendum's own commit `d4e73fc` is
  green too (run at `2026-08-18T13:06:24Z`), so the session's baseline
  tip is itself CI-verified, not merely its parent.
- Edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`; tree
  clean including untracked; local HEAD `d4e73fc` == `origin/main`;
  last tag `stable-20260818-event-log-shape-defects` @ `eeb0299`, HEAD
  untagged and **no tag owed** at Step 0.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **346** zero-failure
blocks / **3,926** tests / **17,638** assertions — reconciling exactly
against ADR-0150's addendum (346 / 3,926 / 17,638). `clojure -M:poly
check` **OK**. Reading sets all green, from the generated
`state-derived.md` rather than from prose: `:corpus` 1801/2045,
`:docs` 708/785, `:judge` 895/1000, `:onboarding` 1396/1530, `:sim`
1247/1405.

**(a) `classify-change` rows — exactly the two ADR-0150 recorded.**
`branches` keys on top-level entries per kind and compares
`[optional? value-schema]` per key; `:reason` is declared identically
at `event_schema.clj:274` (`:admission`) and `:387`
(`:outpatient-visit`), and nowhere else on an event kind that this
change touches — `:step-rejected`'s `:492` is a DIFFERENT key (an
`:enum` read from `engine/documented-step-rejection-reasons`, never
nil, not being made optional). So the predicted output is two rows and
no third:

    BREAKING: :admission: key changed: :reason (required -> optional)
    BREAKING: :outpatient-visit: key changed: :reason (required -> optional)

**(b) The bump the policy owes**, quoted from the clause that owes it
(`schema-version`'s own docstring):

> MINOR or MAJOR for anything else -- a key removed, an optional key
> made required, a value schema changed, a kind removed.

A required key made optional is `classify-change`'s own enumerated
BREAKING case, so a bump is owed. Predicted **1.1.0 -> 1.2.0, MINOR**
— exactly one key of two kinds changes cardinality, no kind is
removed, no value schema moves, and nothing a 1.1.0-era consumer read
is renamed.

**(c) Committed artifacts carrying `:reason nil` today**, measured
from the tree rather than assumed, each with the count of key
occurrences that will disappear:

| artifact | `:reason` total | nil | non-nil | predicted |
|---|---|---|---|---|
| `demos/traces/emit-state/ground-truth.edn` | 3 | 3 | 0 | MOVES, −3 |
| `demos/traces/module-mix/ground-truth.edn` | 59 | 59 | 0 | MOVES, −59 |
| `demos/traces/order-result/ground-truth.edn` | 3 | 3 | 0 | MOVES, −3 |
| `demos/traces/boarding-transfer/ground-truth.edn` | 25 | 0 | 25 | FROZEN |
| `demos/traces/persona-enriched/ground-truth.edn` | 5 | 0 | 5 | FROZEN |
| `demos/traces/site-profiles/ground-truth.edn` | 2 | 0 | 2 | FROZEN |
| `resources/sim-engine/event-examples.edn` | 3 | 1 | 2 | MOVES, −1 |
| `resources/sim-engine/event-schema.edn` | 3 | — | — | MOVES, schema text + bump stamp |
| `resources/sim-engine/event-schema-baseline.edn` | 3 | — | — | MOVES, the ONE re-freeze |
| `docs/formats.md` (`EVENT-LOG-GENERATED` region) | — | — | — | MOVES, schema text + bump stamp |

The three FROZEN traces are the hand-authored-pathway ones, and their
freeze is the control: it is what makes the change "module-compiled
encounters stop saying nothing" rather than ":reason went away".

The manual's ground-truth-invariance digest (`docs/manual/04-time-on-
the-wire.md:107-108`, `d00bf49c…` after ADR-0150) is predicted to
**MOVE**: `ed-tuesday` runs module-compiled encounters. Re-witnessed
at Step 3 rather than trusted.

**(d) THE ORACLE — the prompt's own prediction (d) is WRONG, and this
is the session's first finding.** The channel predicted
`bin/regression-oracle d4e73fc HEAD` IDENTICAL, reasoning that
`:reason` is not rendered by any HL7 builder on the oracle's five-arg
emitter path. That grep is correct and was re-run: the only three
occurrences of `reason` in `sim-emit-hl7`/`sim-emit-fhir` `src` are
prose (`emit_hl7.clj:433`'s own ADR-0150 note, `v2_replay.clj:340`,
`emit_fhir.clj:40`), so **no HL7 or FHIR byte moves**.

But the emitter is only half of what the oracle digests.
`digest.clj:165-172`'s `engine-pair` returns

    {:ground-truth ground-truth :hl7 (vec messages)}

— the **ground-truth event log itself** is digested, which is exactly
the artifact S-1 changes. Every `engine-pair` root is a
`{:pathway {:name "module-only" :steps []}}` run, so every encounter
in it is module-compiled, and `compile_trajectory.clj:211-213`'s
`encounter->step` emits `:admission`/`:outpatient-visit` steps
carrying **no `:reason`** — which is the defect S-1 exists to fix.
"Oracle IDENTICAL" is therefore not merely unlikely, it is
**unsatisfiable by any correct implementation of S-1**.

Predicted, from a PRE-change digest run over all 35 roots at `d4e73fc`
(the ADR-0142 method, verbatim: derive the mover set from the live
tree in both halves before any `src` edit) — **32 movers, 3
identical**. The three identical ones are precisely the three
`interpreter-batch` roots, which digest compiled TRAJECTORIES and
never run the engine at all:

    appendicitis 0    sore-throat 0    ear-infections 0

and the 32 movers, with the `:reason nil` count that makes each one
move:

    veteran-substance-abuse-treatment 300   metabolic-syndrome-care 300
    med-rec 300                             bronchitis 294
    injuries 277                            total-joint-replacement-engine 233
    colorectal 196                          urinary-tract-infections-history-engine 163
    osteoporosis 154                        veteran-prostate-cancer 138
    urinary-tract-infections-engine 135     sleep-apnea 40
    death-fixture 33                        osteoarthritis 31
    veteran-ptsd 24                         sinusitis 18
    veteran-lung-cancer 15                  anemia 12
    ear-infections-history-engine 10        ear-infections-engine 10
    sepsis 9                                dementia 7
    asthma 5                                vhd-tricuspid 4
    vhd-pulmonic 4                          hypothyroidism 4
    attention-deficit-disorder 4            allergic-rhinitis 4
    fibromyalgia 3                          veteran-self-harm 1
    rheumatoid-arthritis 1                  dermatitis 1

So the predicted verdict is `DIFFERS` (exit 1) with a mover set of
exactly those 32 and an identical set of exactly those 3 — a
**declared oracle change** in ADR-0142's own established sense, not an
unexplained one.

### Step 1 — the policy amendment, landed FIRST and on its own

The waiver lands before the fix, so S-1 lands under the amended law
rather than in front of it. Two surfaces state this law and both move
in the one commit (`rulings.md#R-law-surface-propagation`):

- `event_schema.clj`, `schema-version`'s docstring — the deprecation
  sentence keeps its original text and gains the waiver after it: the
  window is waived *while the event contract has no consumer outside
  this repository (no Clojars publication, no downstream repo pinning
  `:event-schema-version`)*; the waiver **expires on the first such
  consumer**, at which point the clause binds unamended **and nothing
  further need be edited for it to** — the expiry is a condition, not
  a future edit somebody has to remember; and each removal made under
  the waiver says so in its own version note, so the waiver leaves a
  trail rather than a silence.
- `docs/formats.md`'s Stability section — the same rule in the
  reader-facing voice, cited by footnote rather than as a bare token
  (ADR-0102, the gate ADR-0150 tripped on its own first attempt).

**The 1.1.0 note is amended in place and DATED, not silently
rewritten.** ADR-0150 closed that note with *"A future removal with
any distance from publication owes the window."* Under the waiver that
sentence is no longer the law, so it is replaced — but the replacement
says what it replaced, and the original disclosure paragraph above it
is left standing verbatim. 1.1.0 is re-read as the first removal made
under the waiver rather than as a violation of a rule that has since
changed.

**One self-inflicted red, disclosed rather than quietly fixed.** The
first Step 1 suite run came back `MAKE_EXIT=2` after 7 blocks:

    Syntax error compiling def at (ehrt/sim_engine/event_schema.clj:73:1).
    java.lang.RuntimeException: Too many arguments to def

The amendment quoted the sentence it was replacing, and the quotation
marks around it were written unescaped into a Clojure docstring, which
terminated the string early and turned `schema-version` into a
three-argument `def`. Reworded to quote by dash rather than by
quotation mark, since a docstring quoting its own prior text is a
shape this file will meet again. Worth recording for the same reason
ADR-0150 recorded its ADR-token gate trip: the compiler caught it on
the first run that could see it, and a docs-only-looking commit is
exactly the kind that tempts a session to skip the full suite.
