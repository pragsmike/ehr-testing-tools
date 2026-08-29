# Arc 4 sweep 5 — fan-out and the MLLP sink (ADR-0175 (f)+(g), ruling B1)

2026-08-28. Base `ba82f9f`; five commits plus this record. Ceremony: R30
(commit and push at each checkpoint), taken from the session prompt.
Author ruling 2026-08-28, collision option (b), is what this sweep is
built on.

`bin/preflight` ran first, **exit 0, no findings**: the last five CI runs
on main all green; repo root not under `/mnt/`; `core.fileMode` true and
`core.ignorecase` unset; working tree clean including untracked; local
HEAD matched `origin/main`; HEAD not tagged `stable-*` — disclosed and
correct, no tag is paid.

## The proof shape is DIFFERENT from sweeps 1–4, and that is the headline

Chatter, charges, the status ladder and SIU each put NEW BYTES on the
wire, so each owed a declared message-digest change at its own turn-on,
and each sweep's record is a story about which digest moved and why.

**Fan-out puts none.** It re-delivers bytes that already exist, into
spools of its own; MLLP delivers them over a socket. So BOTH brackets
read IDENTICAL at BOTH mechanism commits AND at the turn-on — the first
arc-4 sweep for which that is true of the message half too — and the
evidence lives one layer out, in the subsequence law, the spool digests
and the ACK pairing law.

## What landed

| sha | commit |
|---|---|
| `d41c51b` | step 0: the four scheduling kinds' `:doc` says what is true now |
| `048bc28` | fan-out — the subscriber table, DARK |
| `4e54e6f` | `:mllp` as a sink kind — the socket, with ACK pairing |
| `b7e0ac1` | fan-out TURNED ON in three scenario configs, and one MLLP leg |
| `db6a358` | re-witness the straddle timeline at the turn-on |

## The brackets — every line, every commit

```
bin/ground-truth-bracket d41c51b 048bc28
  IDENTICAL: every digested root's :ground-truth matches (38 roots)    GTB_EXIT=0
bin/regression-oracle    d41c51b 048bc28
  IDENTICAL: every root's digest matches                               RO_EXIT=0

bin/ground-truth-bracket 048bc28 4e54e6f
  IDENTICAL: every digested root's :ground-truth matches (38 roots)    GTB_EXIT=0
bin/regression-oracle    048bc28 4e54e6f
  IDENTICAL: every root's digest matches                               RO_EXIT=0

bin/ground-truth-bracket 4e54e6f db6a358
  IDENTICAL: every digested root's :ground-truth matches (38 roots)    GTB_EXIT=0
bin/regression-oracle    4e54e6f db6a358
  IDENTICAL: every root's digest matches                               RO_EXIT=0
```

Neither script was passed `--declared-digest-change` at any commit,
because none was owed at any commit. Coverage is the standing one: 38 of
41 roots carry `:ground-truth`; the three interpreter-layer batch roots
(`appendicitis`, `ear-infections`, `sore-throat`) write a vector of walks
and are skipped by name on both sides.

**No oracle root opted in, and none needed to.** The thing this sweep
produces is a SPOOL, and the oracle digests messages and ground truth.
That is a real coverage statement, not an omission: the sweep's own
claim is that the digested surface does not move, and it is measured
three times.

## The subsequence law, verbatim

> For every subscriber `s` and every base message vector `M`,
> `:indices` is a **strictly increasing** vector of positions in `M`, and
>
> ```
> (:messages s) = [ (mask s (M i)) | i <- (:indices s) ]
> ```
>
> where `mask` replaces exactly the MSH fields `s`'s own `:msh` map names
> — **MSH-3, MSH-4, MSH-5, MSH-6 and no others** — and is the IDENTITY
> when `s` names no `:msh`.
>
> Equivalently: with no `:msh` override a subscriber's spool is a
> **byte-exact subsequence** of the base spool; with one, byte-exact after
> masking, **in both spools**, exactly the overridden MSH field positions.

The mask is `fan-out/mask-msh` itself, exported for the gates so that a
property comparing two sides cannot compare against a second, hand-written
mask. The law is a property over generated subscriber tables
(`ehrt.sim-emit-hl7.fan-out-test`, 200 trials) and is restated over a real
run's messages in `ehrt.sim.fan-out-run-test`.

**The PV1-less rule**, written down rather than discovered: a
`:patient-classes` filter reads PV1-2, and `ADT^A20` is `[MSH EVN NPU]`
with no PV1 at all, so a class filter EXCLUDES every PV1-less message
unless the subscriber names that trigger explicitly in `:message-types`.

**The allow-list law**: a filter naming a `TYPE^TRIGGER` this emitter
cannot produce is `:unknown-fan-out-message-type`, rejected before the
engine runs — never a feed silently empty forever because somebody wrote
`ADT^A05`.

## The ACK pairing law, verbatim

> For a run that sends messages `m_0 .. m_{n-1}` over one connection, the
> **k-th ACK read from that socket is the acknowledgement of `m_k`**, and
> its MSA-2 must equal `m_k`'s own MSH-10. There is no ACK for a message
> never sent.

POSITIONAL, and load-bearing rather than convenient — the same author
ruling the fan-out obeys. MSA-1: `AA` continues; `AE`/`AR` abort delivery
at that message and every later one is skipped rather than pushed at a
receiver that has just refused; any other code, including enhanced-mode
`CA`/`CE`/`CR`, aborts as `:mllp-unrecognized-ack-code`; a missing ACK is
`:mllp-ack-timeout` by explicit SO_TIMEOUT.

## The duplicate-MSH-10 test, and its outcome

`ehrt.conformance.mllp-pairing-test` regenerates the gated
`seed-424242-clinic-decade` run and **measures the duplicates rather than
quoting them**:

```
seed-424242-clinic-decade duplicate MSH-10s:
  {"MRN000189-R01-119086260" 6, "MRN000189-R01-125739060" 2}
  (8 messages in 2 groups)
```

That is arc 4 sweep 3's finding 1 re-derived: `control-id-for` is
non-injective over `:result-available`, and the collision is the rowed
one (`-R01-`, the default ORU branch), which the gate also asserts so a
NEW collision shape cannot hide inside an old row.

It then delivers all **1,938** of that corpus's messages over a loopback
socket and asserts: every one sent, every one acknowledged, `:index`
`0..n-1` in order, MSA-2 equal to the sent MSH-10 **per pair**, the pair
count strictly GREATER than the distinct-id count — MSA-2 equality is not
a global bijection — and every message on the wire byte-identical. The
gate asserts the duplicates are still present FIRST: if
`roadmap.md#oru-control-id-collision` is ever fixed, it goes red rather
than passing over a corpus that no longer tests it.

## Spool digests, witnessed on this session's own exerciser run

| corpus | subscriber | messages | of base | sha256 |
|---|---|---|---|---|
| ed-tuesday | `adt-feed` | 273 | 1,554 | `3e92c86128f85720705d48d93a2d476bfbfdcb951421ace2c3abc5446e5005b4` |
| ed-tuesday | `lab-feed` | 100 | 1,554 | `1d17172ec6ae6716ce6fd089eb5915c1595d9f1f990c96eb11c4d78512adc902` |
| clinic-decade | `adt-feed` | 200 | 1,688 | `793147ee50fe72d782f731c9d2f3aae0ca57f1acc0bc692ece7efd84ce04ec19` |
| clinic-decade | `bed-feed` | 400 | 1,688 | `5f38b085c71ef85a5fd3e4cbe102a78adeedbddc08995f153441b2fff897c382` |

Each digest is re-derived by the exerciser with `cat msg-*.hl7 |
sha256sum` and compared against the sidecar — a digest written by the
same process that wrote the files proves nothing until something else
recomputes it.

The MLLP leg on the same run: **273 messages delivered to 127.0.0.1 and
273 acknowledged**, MSA-2 checked per pair.

`clinic-decade`'s `bed-feed` is the PV1-less rule shipped as a live
example: it names `:patient-classes #{:inpatient}` AND `"ADT^A20"`, and
the exerciser asserts both halves against the real run — **400 messages,
of which 300 are PV1-less `ADT^A20`s riding the explicit trigger, and
not one whose PV1-2 is anything but inpatient**. Delete `"ADT^A20"` from
that filter and 300 of the 400 vanish, silently, which is the failure the
rule exists to make legible.

## The red witnesses — seven, all real output

Fan-out (`red-fanout.log`):

* **(a)** `plan` walking the base vector in reverse — the rejected
  alternative's on-disk signature. **13 failures**; the property's own
  shrink reported `[[{:name :sub-0}]]`, the minimal table. This is the RED
  WITNESS ADR-0175 section 2(f) asked for by name.
* **(b)** the PV1-less escape clause deleted — the bed feed that names
  `ADT^A20` explicitly went from `[0 2 5 8 10]` to `[0 10]`.
* **(c)** both config-rejection branches disabled — not a soft failure but
  `ExceptionInfo: the engine ran`, which is the "before the engine" claim
  proved rather than asserted.
* **(d)** the subscriber-spool write disabled — 2 failures, 4 errors.

MLLP (`red-mllp.log`):

* **(e)** MSA-1 ignored (every code an accept) — including `MSA-1 "CE" was
  treated as an accept`.
* **(f)** delivery keyed on MSH-10 instead of position — **the twin was
  dropped**: 3 sent became 2, `["…119086260" "…119086260" "MRN2-A01-20"]`
  became `["…119086260" "MRN2-A01-20"]`. This is the author's collision
  ruling shown as a defect rather than argued.
* **(g)** a frame-byte literal reintroduced into the sink — the no-drift
  gate fired.

The reordering mutant is ALSO kept permanently, as
`the-subsequence-law-is-not-vacuous`, so the property cannot silently
become vacuous later.

## ADR premises contradicted

* **ADR-0175 §2(f)** — the shape sketch spells the class filter
  `:patient-class`. The value is a SET and the session prompt that ruled
  this sweep spells it `:patient-classes`; the plural is what shipped.
  Recorded rather than silently reconciled.
* **ADR-0175 §2(f)** — "a `dir:` Sink under the corpus root, one directory
  per `:name`". The spools are one level deeper, at
  `<out-dir>/fan-out/<name>/`, for a MEASURED reason: `ehrt play`, `ehrt
  corpus batch` and `gate v2` all read only the candidate files sitting
  DIRECTLY under the path they are given, while
  `ehrt.corpus.intake/source-files` is recursive (`file-seq`). One
  `fan-out/` parent keeps every subscriber spool in an obviously-derived
  subtree instead of scattering N sibling directories among the corpus's
  own `msg-*.hl7`.
* **The session prompt's own §1(i)** — "`spool-sim-output!` (or the seam
  the tree names) writes each subscriber's spool by FILTERING the base
  message vector". The parenthetical was needed: `corpus generate sim`
  reaches `spool-sim-output!` with a PAYLOAD only, never the config, so
  the filter cannot live there. The plan is made in `ehrt.sim.run`, where
  the config and the message vector are both in hand, and the spool writer
  filters nothing.
* **ADR-0175 §2(g)** — "the prompt sketched `--transport`". Confirmed and
  taken: `--sink mllp://host:port` cost no new flag. What the ADR did not
  say is that the printer's authority branch was UNREACHABLE until now —
  `:blaze` is recognized but not implemented, and the printer only ever
  sees implemented kinds — so `print-sink-designator` had never rendered
  an authority form at all before this sweep.

## Findings

1. **`clojure -M:poly check` passed over a file that would not compile,
   again.** An unescaped `"` inside a docstring I added to
   `source_sink.clj` silently terminated the string and produced `Too many
   arguments to def`. This is arc 4 sweep 4's own finding 3 recurring in
   the very next session — `check` is a dependency-graph gate, not a
   compiler, and the cheapest guard is to load the namespace once
   (`clojure -M:dev -e '(require ...)'`) after writing any docstring that
   quotes something.
2. **One whole-registry pin reddened, predictably.**
   `source_sink_test/sink-kinds-test` pins both kind sets exactly. Updated,
   and given a third assertion it did not have: `:blaze` is now the ONE
   recognized-but-unimplemented sink, stated as a set difference so the
   next kind to land cannot leave that claim stale. `poly test` again
   aborted at the first failing brick, so the tree-scanning gates were run
   directly before re-running the whole suite rather than discovered one at
   a time.
3. **The vocabulary gate caught a defect in its own fixture, which is the
   best thing a born-red gate can do.**
   `the-emitter-produces-nothing-outside-the-declared-vocabulary` reported
   an EMPTY message set on its first run: the `:scheduling` block I wrote
   for it used `:follow-up-rate` where the real key is `:follow-up {:rate
   … :interval-days …}`, so `run-command` rejected the whole run. A gate
   that asserted only "⊆ vocabulary" would have passed on that empty set;
   the "each declared add-on family is witnessed" half is what made it
   fail.
4. **The MLLP play leg's first run buried its own result and took 58
   seconds, and neither was MLLP's fault.** The payload carried all 273 ACK
   pairs (the CLI now merges counts only; the pairs stay in the component
   summary the gates read), and the 58 seconds were ENTIRELY pacing — that
   spool spans nineteen years of stream time, so at the taught `10^7` rate
   the pacer dominated a step that is about delivery. Both fixed in the
   turn-on commit.
5. **`ehrt play --sink` had never had a failing sink before.** `:file`
   cannot fail after the run starts; `:mllp` can, three ways. `play-command`
   now returns the sink's own error INSTEAD of a success, so a refused or
   unanswered delivery exits non-zero rather than printing a cheerful
   summary of an undelivered run. That is a new shape in that function and
   is disclosed rather than buried in the diff.
6. **The straddle tripwire's tenth fire moved nothing.** Every previous
   fire moved a value because a draw moved or a message count moved. This
   sweep moves neither, and the drawing's own values were re-witnessed
   one by one against the exerciser's real output rather than inferred:
   10/15/18 messages, `20260811003739+0000` and `20260811021037+0000`,
   and `grep -cF MRN000002 batch-001.hl7` still 0.

## The gates, with their own exit codes

Run on a clean tree, each to a full log with the exit code captured
explicitly:

```
make test          23,915 passes, 0 failures, 0 errors      MAKE_EXIT=0
make integration                                             MAKE_INTEGRATION_EXIT=0
```

`make integration` is where both demo exercisers run their new legs, and
it is not in `make test` and not in per-push CI. The ed-tuesday MLLP leg
takes **471 ms** for 273 messages once the pacing was taken out of it (it
was 58 seconds before, all of it the pacer).

Per-commit suite figures along the way: `d41c51b` 23,463 passes;
`048bc28` 23,685; `4e54e6f` 23,915 — all 0 failures, 0 errors,
`MAKE_EXIT=0`.

No tag is paid (de-scaffold ruling, 2026-08-25); CI green at the pushed
tip is the close marker.
