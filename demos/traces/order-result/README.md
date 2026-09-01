# Demo: order/result (ORM^O01 / ORU^R01), through the CLI post-Task-0

> **Generated, byte-exact.** The captured artifacts in this directory --
> ground-truth.edn, messages.txt -- are written by `bin/regen-traces`, run
> via `make traces` (a `make docsgen` leaf). They are byte-for-byte
> capture of the command below, not prose about it, so hand-editing one
> makes it a fiction: change the command or the engine and regenerate.
> CI freshness-diffs `demos/traces/` whole. This README itself is
> hand-owned. (ADR-0158, review-4 register row L3-7.)
> The `config*.edn` beside them is the opposite: a hand-authored INPUT
> `bin/regen-traces` reads with `--config`, never writes.

Re-run of the M3 order/result cycle, but THROUGH the CLI this time —
the M4 Task 0 wiring fix's own proof: `:pathways` (an authored pathway
containing an `:order` step) now reaches the engine from
`ehrt.sim.run/run-command`, not just from a direct
`engine/run` API call the way `engine-test` already exercised it.
Before Task 0, this exact config would have silently run the DEFAULT
pathway instead — no error, just the wrong traffic, invisible to
anyone watching only the CLI.

## Command

```bash
bin/ehrt sim run --seed 42 --patients 3 \
  --config demos/traces/order-result/config.edn --emit hl7
```

[`config.edn`](config.edn) supplies `:pathways` — the data-heavy key
with no flag of its own, per `run-command`'s own `--config`
passthrough (Milestone M4 Task 0).

## What to look for

- [`ground-truth.edn`](ground-truth.edn): every patient's log opens
  with `:registered` (Persona, M4), then `:admission`, `:order-placed`,
  `:result-available`, `:discharge` — the authored pathway exactly, for
  all 3 patients.
- [`messages.txt`](messages.txt): 12 messages (4 per patient — ADT^A01,
  ORM^O01, ORU^R01, ADT^A03). `:registered` renders no message of its
  own (ADR-0012's own precedent: truth about the run, not wire traffic
  a real ADT/ORM/ORU feed would carry).

## Excerpt: ORM^O01 then ORU^R01 for patient 1 (MRN000001), verbatim from `messages.txt`

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||ORM^O01|MRN000001-O01-0|P|2.4
PID|1||MRN000001||Garcia^Sandra||19520726|F|||914 Fairview Blvd^^Salt Lake City^UT^84101||(349)906-1132
PV1|1|I|Renal^^RENAL-04^general-hospital||||4255631598^Chen^Amara|||||||||||||||||||||||||||||
ORC|NW|MRN000001-O01-0
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN

MSH|^~\&|EHR-TESTING-SIM|SIM|||20240101012500+0000||ORU^R01|MRN000001-R01-5100|P|2.4
PID|1||MRN000001||Garcia^Sandra||19520726|F|||914 Fairview Blvd^^Salt Lake City^UT^84101||(349)906-1132
PV1|1|I|Renal^^RENAL-04^general-hospital||||4255631598^Chen^Amara|||||||||||||||||||||||||||||
ORC|NW|MRN000001-R01-5100
OBR|1|||58410-2^CBC panel - Blood by Automated count^LN|||20240101012500+0000
OBX|1|NM|6690-2^Leukocytes [#/volume] in Blood by Automated count^LN||7.4|K/uL|4.5-11.0|N||||||20240101012500+0000
OBX|2|NM|789-8^Erythrocytes [#/volume] in Blood by Automated count^LN||5.69|M/uL|4.2-5.9|N||||||20240101012500+0000
OBX|3|NM|718-7^Hemoglobin [Mass/volume] in Blood^LN||8.2|g/dL|12.0-17.5|L||||||20240101012500+0000
OBX|4|NM|4544-3^Hematocrit [Volume Fraction] of Blood by Automated count^LN||46.8|%|36.0-50.0|N||||||20240101012500+0000
OBX|5|NM|777-3^Platelets [#/volume] in Blood by Automated count^LN||418.0|K/uL|150-450|N||||||20240101012500+0000
```

(Segments are shown one per line here for readability; the real wire
format in `messages.txt` uses `\r`, HL7v2's actual segment delimiter.)
PID is enriched with the same patient's Persona-sampled demographics as
every other message type — order/result messages are not a special
case (`ehrt.sim-emit-hl7.segments/pid-segment` applies uniformly). Note
OBX-8 `L` (abnormal-low) on the hemoglobin result, `N` on the other
four — computed truth from
`ehrt.sim-engine.order-profiles/abnormal-flag`, never a re-derivation
at emit time.

**Two clocks on the result, one on the order (ADR-0142, 2026-08-16).**
The ORU carries `OBR-7` and, on every OBX, `OBX-14` —
`20240101012500+0000`, the result event's own clinical instant. Here
they equal `MSH-7` exactly, because this demo runs without `:latency`:
one instant, three fields. Under a latency profile they diverge, and
that divergence is the point — MSH-7 moves to when the message was
*transmitted* while OBR-7/OBX-14 stay at when the result was
*observed*, so a downstream receiver handed a late result can still
back-date it. [Manual chapter 4](../../../docs/manual/04-time-on-the-wire.md)
shows that case on a real lagged result.

The ORM above is deliberately *not* symmetric: its `OBR` still ends at
OBR-4, no OBR-7. An order's clinical-time field would mean the
observation time of an observation that has not happened yet; the field
actually owed there is ORC-9 (transaction time), and it is a named
revisit rather than a silent ride-along. Author ruling, 2026-08-16:
"Results only; ORM byte-frozen."

**Errata, 2026-08-16 (ADR-0142).** Regenerating this demo for the field
above also picked up drift that had been sitting here unnoticed: the
`PV1` segments in `messages.txt` and in the excerpt above had been
captured *before* PV1 gained its trailing positional fields (the
site-profiles milestone's PV1-36 disposition), and nothing regenerates
or freshness-checks `demos/traces/**` — no `Makefile` target, no CI
job, no test, only `.gitattributes`' `-text` byte protection. So the
strip drifted silently and stayed drifted. **Errata, 2026-08-17
(ADR-0149): there is now a `Makefile` target (`make traces`), a CI job
(the generated-doc freshness diff) and a test
(`ehrt.docs-tooling.traces-fresh-test`); `messages.txt` also gained the
one trailing newline this file's hand capture had dropped, +1 byte.** Both classes of change are
now landed together and are distinguished here rather than blended: 18
changed lines are ADR-0142's own (3 OBR-7, 15 OBX-14, across the three
ORU messages) and 12 are the pre-existing PV1 catch-up. The missing
gate is registered as a roadmap row; this note is the interim record.
