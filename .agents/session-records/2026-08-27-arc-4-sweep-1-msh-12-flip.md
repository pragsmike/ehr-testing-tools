# Arc 4 sweep 1 — the 2.4 flip (ADR-0175 ruling A1)

2026-08-27. Base `ba9c78c`, tip `<tip>`. Six commits. Ceremony: R30
(commit and push at each checkpoint), taken from the session prompt.

`bin/preflight` ran first, exit 0, no findings. One thing disclosed:
a run among the last five was still PENDING (`ba9c78c`) and was not
awaited to conclusion (AR-CI-4), not counted as red.

## What landed

| sha | commit |
|---|---|
| `053012c` | ADR-0175 Accepted — RULED A1 B1 C1 D1 E1 |
| `2ddda59` | `bin/ground-truth-bracket` + shared `bin/oracle-lib.sh` |
| `9f8c9f6` | index mode 100755 on the two new `bin/` files |
| `fabc749` | PID-13 as `(NNN)NNN-NNNN` (A1, 1 of 2) |
| `c7354d1` | MSH-12 `2.4` (A1, 2 of 2) |
| `0b595c3` | straddle-timeline tripwire re-review |

## Both brackets, per commit

The line that matters twice, and it is the same both times:

```
bin/ground-truth-bracket 892201a HEAD
--- coverage: 36 roots carry :ground-truth and are digested;
    3 skipped (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
--- THIS IS NOT A REGRESSION-ORACLE CLAIM: the :hl7 half of every root is
    excluded by construction ---
IDENTICAL: every digested root's :ground-truth matches between 892201a and HEAD (36 roots)
```

Run at `fabc749` and again at `0b595c3`. Ground truth did not move at
either commit — which is the whole claim of arc 4.

The message side, declared:

```
bin/regression-oracle 9f8c9f6 fabc749 --declared-digest-change   -> DIFFERS
bin/regression-oracle fabc749 0b595c3 --declared-digest-change   -> DIFFERS
```

39 roots each side, **0 added, 0 removed, 34 DIFFER, 5 IDENTICAL**, at
both commits.

**THE MOVER SET IS NOT THE ONE THE SWEEP EXPECTED.** The prediction was
36 engine-layer roots DIFFER and the 3 interpreter-layer batch roots
IDENTICAL. The truth is 34 and 5: `dermatitis` and `veteran-self-harm`
are engine-layer roots that HELD, because they emit NOTHING. Measured:

```
dermatitis         events 301  messages 0   {:registered 300, :care-plan-end 1}
veteran-self-harm  events 300  messages 0   {:registered 300}
```

Both kinds are on the emitter's deliberate-silence list, so each root's
`:hl7` half is `[]` and no emission change can move it. Two of 36
engine-layer roots are, for any wire-side sweep, a population of zero —
the vendored-module emission floor showing up inside the oracle itself.
An arc-4 sweep reading `IDENTICAL` on either is reading
`rulings.md#R-empty-population-is-red`, not evidence. This is exactly
why ADR-0175 section 4 says to re-derive the mover set at
implementation time rather than cite a sentence.

## The instrument E1 owed, and the ADR premise that made it necessary

ADR-0175 section 4 says `bin/regression-oracle` "reports IDENTICAL on
every root's `:ground-truth`". **It never could.**
`ehrt.oracle.digest/-main` writes the `{:ground-truth :hl7}` pair as ONE
file per root and the oracle sha256s the file, so an emission-only
change makes every engine-layer root DIFFER and leaves the oracle mute
on the half arc 4 promises did not move. Recorded in the ADR under E1
rather than worked around.

`bin/ground-truth-bracket` is what that sentence names: same disposable
worktrees, same synthetic classpath, same per-side resolution, same
`digest.clj` soundness check, digesting `(pr-str (:ground-truth root))`
alone. Both scripts source `bin/oracle-lib.sh` rather than carrying two
copies — pure code motion, the moved region byte-identical to
`bin/regression-oracle`'s own lines 62–245 (verified by `diff`), the
soundness block statement-identical modulo two added `local` decls.

Proven two ways before use:

* `bin/ground-truth-bracket 892201a HEAD` on its own commit —
  **IDENTICAL, 36 roots**, 3.4 min;
* mutation — one draw's range changed at
  `sim_model/persona.clj:293` on a throwaway commit — **DIFFERS, all 36
  roots**, exit 1. Commit discarded, tree restored.

Three properties are gated rather than left to that one run, since the
verdict is about a different commit pair every time: the `:hl7` half is
excluded and only it; a root with no `:ground-truth` is NAMED not
dropped; and an all-skipped population is RED, not IDENTICAL. Red
captured for all three — deleting the empty-population guard makes the
bracket print `IDENTICAL: ... (0 roots)` and exit 0, the vacuous verdict
verbatim.

## The structure-class table

237-message corpus, default profile, through the judge's own
`#'hapi/new-context`:

| MSH-9 | resolves to | n |
|---|---|---|
| ADT^A01 | `ADT_A01` | 40 |
| ADT^A02 | `ADT_A02` | 42 |
| ADT^A03 | `ADT_A03` | 40 |
| ADT^A04 | `ADT_A01` (eventmap alias) | 29 |
| ADT^A12 | `ADT_A09` (alias) | 5 |
| ADT^A40 | `ADT_A39` (alias) | 1 |
| ORM^O01 | `ORM_O01` | 40 |
| ORU^R01 | `ORU_R01` | 40 |

Zero `GenericMessage`. The three aliases are the vendored eventmap's
own, matching ADR-0175 section 1(iv). Both GATED corpora were checked
the same way rather than assumed: all 53 `sim-gate-loop` files and all
210 `full-capability` files resolve to real v2.4 classes.

Four registry families are NOT in this corpus and the test names them:
A11, A13, A17 (churn's lottery; A13 went unreached across all of arc 3b
too) and A20 (needs the `:bed-cycle` opt-in, which rides `:config`
through `ehrt.sim.run` and is out of `engine/run`'s reach). All four DO
resolve — that is ADR-0175's measurement over its own 747-message probe
corpus, not this gate's.

## Verdict counts, before and after — the number this sweep exists for

| gate | before (2.3, committed baseline) | after (2.4) |
|---|---|---|
| `gate v2`, sim-gate-loop | 53 pass / 0 rejected, `by-code {}` | 53 pass / 0 rejected, `by-code {}` |
| `gate v2`, full-capability | 210 pass / 0 rejected, `by-code {}` | 210 pass / 0 rejected, `by-code {}` |

**ZERO new findings, and there is no list of first-ten to give.** The
sweep expected some. It got none, and the class table above is what
makes that a result rather than a shrug: the gate genuinely parses
structures now and our corpus is clean. Neither baseline moved, so
neither was re-captured.

## What the flip actually buys — measured, and narrower than the ADR

ADR-0175 section 1(iv) describes the 2.3 state as "no segment order, no
cardinality, no required-segment check, no primitive typing". **Only the
last is restored.** One real A01, damaged four ways, gated at both
versions through `hapi/execute`:

| damage | 2.3 | 2.4 |
|---|---|---|
| malformed PID-7 primitive | clean | **CAUGHT** |
| required PID removed | clean | clean |
| unknown `ZZZ` segment | clean | clean |
| PID/EVN order swapped | clean | clean |

HAPI's `PipeParser` under `defaultValidation` stays lenient about which
segments are present, which it recognises, and their order, even with a
real structure resolved. So the flip buys PRIMITIVE TYPING across every
field of every message — precisely what caught PID-13 — and nothing
else. Pinned as a gate so "gate v2 is no longer vacuous" cannot grow
into "gate v2 checks structure". Structural conformance still needs the
profile tier, which ADR-0175 section 1(iii) records as unable to run
over this project's own corpus at all.

## The co-landing the suite forced

`v2-replay/tn->persona-phone` is `tn-field`'s inverse and landed WITH
it. v2-replay reconstructs ground-truth-shaped state from the wire, so
an emitter convention not inverted there makes the two disagree — and
they did: both emitter-coherence properties went red the moment
`tn-field` landed alone. That is the property doing its job, and it is
recorded rather than quietly fixed. A new 200-case spec names the
inverse pair directly, because the coherence properties catch a broken
pair only as `{:result false}` on a shrunk seed.

## Findings, one line each

1. **`bin/regression-oracle` cannot report per-half.** ADR-0175 section
   4 said it does. Corrected in the ADR under E1; the instrument built.
2. **Two oracle roots emit zero messages** (`dermatitis`,
   `veteran-self-harm`) and are a zero population for any wire sweep.
3. **The 2.4 flip buys typing only**, not structure — ADR-0175 section
   1(iv) is too broad. Gated.
4. **`v2_replay/parse-persona`'s docstring** claimed "PID-13/:ssn/:age
   are never rendered" while the next line read PID-13; stale since
   PID-13 joined the segment. Corrected in place.
5. **`ehrt.cli.executable-bits-test` requires mode 100755 on everything
   under `bin/`**, including files never exec'd. Both new files went in
   at 100644; `2ddda59` is therefore a red-first commit, pushed with
   `9f8c9f6`.
6. **`poly test project:conformance` runs BRICK tests only.** The
   project's own `test/` namespaces need `:project` or `:all`, and
   Polylith skips them when a brick failed. A new conformance gate can
   look green while never running — checked, and both new gates were
   confirmed executing inside the full `make test`.
7. **My own wrapper masked an exit code**, ADR-0155's shape in my
   hands: `( time make test ) > log 2>&1; echo "MAKE_EXIT=$?"` reported
   harness exit 0 while `MAKE_EXIT=2`. Later runs write the status to a
   sentinel file.
8. **AGENTS.md's new paragraph put `:docs` at 788 against a 785
   budget.** `rulings.md#R-budget-stop` says compact or STOP, never
   bump. Compacted to 785/785.

## Re-pinned

`absent-profile-renders-todays-hardcoded-msh-values` (the pin that made
the flip visible, now carrying the 2.3 escape hatch beside it); the
existing PID-13 assertion in `admission-pid-carries-demographic-fields`;
`demos/traces`' message captures across two `make traces` runs — every
changed line a PID line at commit 1, an MSH line at commit 2;
eleven PID excerpts and fifteen MSH excerpts across six trace READMEs
and two manual chapters; the SIU prose in both scenario READMEs; the
`straddle-timeline` tripwire.

**Did NOT move, and each is a witness that ground truth held:** the four
`arc0_gated_*` corpora (`git diff --stat` empty) and `run_test`'s
`arc0-pinned-digest`; the ground-truth EDN and FHIR bundle trace
captures; `demos/traces/emit-state/README.md`'s FHIR `telecom`, still
`349-906-1132` beside a PID now reading `(349)906-1132`;
`messages-aldric.txt`, whose profile declares `2.5.1` explicitly — the
override path proving itself; both conformance baselines.

## Gates

`make test` and `make integration` at the tip: see the close section
below. `clojure -M:poly check` green throughout.
