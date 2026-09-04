## ADR-0178 — `:window-close-t` is ABSENT, never nil, and the run path validates its own schema

**Status:** Accepted (author-ruled R-fix / R-gate / R-time, 2026-09-05;
build session same day, R30). Closes finding 2 of
`.agents/session-records/2026-09-05-p7-stop-derivation.md`, and
`roadmap.md#cancel-invariant-has-no-time-clause` alongside it.

### Context

Every line number below is at `d55b90d`, read this session.

The P7 derivation stopped because the population it was about to build
an oracle on **violates the event schema on its parent log**, for
reasons no operator causes. Three `:registered` events carry
`:window-close-t nil` where the contract declares
`[:window-close-t {:optional true} :int]`
(`event_schema.clj:626`). Malli's `{:optional true}` permits the key to
be **absent**; it does not permit it to be present and nil. The three
arrive with the `:persons` layer, so no choice of `--patients` avoids
them: 3 at every arrival count from 1 to 40.

The mechanism is two lines that disagree with each other, one layer
apart:

- **`run.clj:146`** — `placeholder-registration`'s
  `(cond-> ... (some? (:branch window)) (assoc :window-close-t (:until-t window)))`
  deliberately OMITS the key when the window never resolves. Its
  docstring (`run.clj:118-145`) is emphatic that this is a correction
  the tree forced (ADR-0173 section 2(d)): the person died inside the
  window, so "the ENGINE declines to promise a close instant it already
  knows will never come".
- **`decide.clj:331`** — the `placeholder?` branch of
  `decide :registered`'s `cond->` re-adds it UNCONDITIONALLY, beside
  `:alias-name` and `:residence`. The compiled entry has no such key,
  so the destructured `window-close-t` is `nil` and the emitted event
  carries `:window-close-t nil`. **The omission upstream is undone one
  layer down.**

Every `:persons` config therefore ships a log whose own manifest
`:event-schema-version "1.8.0"` claims a conformance it does not have
(`docs/consuming-ground-truth.md`, the sim-manifest table), and
**nothing on the run path checks whole-event validity** — the
self-check runs `check-all`, and no row of that catalog validates an
event against the published contract.

**Why it stayed invisible.** Nothing in the tree reads the distinction
between absent and nil. Three assertions look like they guard it and
cannot, because `nil?` is true either way:

1. `persons_test.clj:651` — `(is (nil? (:window-close-t (first ph))) ...)`.
2. `persons_test.clj:844` — `(is (every? #(nil? (:window-close-t %)) ph) ...)`.
3. `check.clj:1596` — `every-placeholder-registration-is-resolved-or-still-open`
   classifies `(nil? window-close-t)` as `:unjudgeable`, identically to
   absent, so the checker tolerates it too.

All three assert the engine's INTENT in the one form that cannot
distinguish that intent from the defect. **This is the ADR-0166 shape
again** — a distinction one surface draws that no gate reads — one
layer further out: ADR-0166 closed a paired terminal event no invariant
covered; this closes a schema the run path never applied to itself.

### Decision

Three rulings, author-given 2026-09-05, executed here.

**R-fix — the key is ABSENT, never nil.** `decide.clj`'s `placeholder?`
branch assocs `:window-close-t` only when `run.clj:146` supplied it, in
its own `(some? window-close-t)` clause. **No schema change: 1.8.0
stands.** The contract was always right; the log becomes conformant to
it. A schema version bump would have been the wrong instrument — it
would record a change in what the contract permits, and nothing about
what it permits changed.

**R-gate — the run path validates its own schema.** `check-all`'s
catalog gains `every-event-is-schema-valid`: every event in the log
validates against `ehrt.sim-engine.interface/valid-event?`, the
published `Event` schema. It is registered **FIRST in reporting order**,
which is where a whole-event well-formedness claim belongs — every
other row presumes it.

This does **not** violate the independent-judge doctrine
(`check.clj:572`, `:599`: "calling the engine's own index-builder here
would prove only that the engine agrees with itself"). The judge may
not reuse the engine's own *decisions* — its fold, its bed arithmetic,
its supersession table. A schema is not a decision; it is the published
contract the log claims to satisfy, the same artifact
`make event-schema-export` freezes and `docs/formats.md` documents for
consumers. Validating against the contract is exactly what an
independent judge does.

**R-time — the cancel invariant gains its missing time clause.**
`cancel-references-existing-uncancelled-event` gains a fifth disjunct:
a cancel's `:t` may not be BEFORE its target's `:t`. Same invariant
name, no new finding class. This is the checker gap finding 1 of the
STOP record measured and
`roadmap.md#cancel-invariant-has-no-time-clause` rowed: the invariant's
four existing disjuncts are a missing target, a wrong target kind, a
target not naming the cancel's own patient, and a target already
cancelled by an earlier cancel of the same kind — and unlike the
`:medication-end` and `:care-plan-end` spans (ADR-0166 and its twin) it
never compared `(:t target)` with `(:t event)` at all. Equality is
permitted: a same-batch cancel shares its target's instant, which
`:transfer-in-error` produces by construction.

### Payload effect, measured rather than predicted

The removal of `:window-close-t nil` pairs is a **payload move**: keys
disappear from `:registered` events. Per R-sweep the expected movers
are the `:persons` roots and NOTHING else. Measured at `d55b90d`
before the fix:

| oracle root | `:persons` | placeholder registrations | `:window-close-t` present | nil pairs |
|---|---|---|---|---|
| `demographic-fold` | `{:count 240 :years 20}` | 10 | 10 | **0** |
| `encounter-horizon` | `{:count 20 :years 20}` | 0 | 0 | **0** |
| `chatter-charges` | `{:count 160 :years 20}` | 10 | 10 | **0** |

Those three are the **only** roots of the 41 that carry `:persons` at
all; the other 38 cannot produce a placeholder registration and so
cannot carry the key. Every placeholder in all three resolves, so
**no oracle root moves** — the expected-mover set is an upper bound
that measurement narrows to empty, and `bin/ground-truth-bracket` is
expected IDENTICAL rather than declared.

What DOES move is larger populations, where a person dies inside an
open window:

| population | invocation | nil pairs before | after |
|---|---|---|---|
| `demos/scenarios/dense-7500/config.edn` | seed 5, `--patients 20 --churn` | 3 of 1,064 keys | 0 |
| `test-fixtures/downstream-calibration/config.edn` | seed 424242, `--patients 500` | 7 of 1,460 keys | 0 |
| same | seed 424242, `--patients 1000` | 7 of 1,460 keys | 0 |

`decide.clj:331` is the ONLY producer of the key in the tree, so every
removed pair is a registration field and the diff has exactly one shape.

**The two downstream-calibration outputs move off the published
SHA-256s**, and that is the correct outcome stated plainly rather than
discovered later: at `d55b90d` this repository reproduces
`434232a913c3389fdc3856f9a6eb14854ff6174499e8a5caa0643085824a03d5`
(500) and
`ddcfc319ffed230a1ce2edd13f62f2fbfd4fd4264eface5bf6a37967ba2deb11`
(1,000) byte for byte, which is what
`roadmap.md#cancel-transfer-reinstates-a-new-subject` witnessed on
2026-09-03. After this ADR it does not, because the downstream team's
own simulator carries the same defect and their payload carries the
same 7 nil pairs. `test-fixtures/downstream-calibration/PROVENANCE.md`
records THEIR values and is unedited; the roadmap row's own dated
claim gains the dated clause that supersedes it.

### Not changed

- **The event schema.** `[:window-close-t {:optional true} :int]` stands
  verbatim, and `:event-schema-version` stays `"1.8.0"`.
- **`run.clj:146`.** It was already right; the whole defect is that its
  correctness was discarded downstream.
- **`every-placeholder-registration-is-resolved-or-still-open`.** Its
  `(nil? window-close-t) :unjudgeable` clause stays as written and is
  now exactly right rather than accidentally tolerant: after R-fix a
  destructured absent key is nil and nothing else is, so the clause
  reads the absence it always meant to read.
- **The three `nil?` sites' intent.** The two `persons_test.clj`
  assertions become absence assertions
  (`(not (contains? e :window-close-t))`) per R-tests — the same claim,
  in the form that can tell it from the defect.
- **`:cancel-admit` / `:cancel-transfer` / `:cancel-discharge` decide
  behaviour.** R-time is a checker change only; no engine emission
  moves.

### Consequences

- The catalog count pin moves 45 -> 46 and the reporting order shifts
  by one, `every-event-is-schema-valid` first. Per R-pins that is its
  own commit naming the count. **Which invariant moved: none of the
  findings** — all four gated corpora were measured schema-valid
  (0 invalid of 1,213 / 1,774 / 1,412 / 97 events) and carry zero
  inverted cancels, so both new clauses are silent on every pinned
  corpus.
- `docs/consuming-ground-truth.md`'s invariant list gains the name at
  the head of the list. Its manifest section (`:615-635`) is unchanged:
  the `:event-schema-version "1.8.0"` claim it documents was the thing
  that was false, and this ADR makes it true rather than restating it.
- A consumer who was tolerating `:window-close-t nil` sees the key
  simply absent, which is what `{:optional true}` always promised.
