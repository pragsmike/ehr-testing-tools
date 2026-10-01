# Chapter 1 — What this is

## The sixty-second proof

One command, no downloads, no config — copied verbatim from the root
[`README.md`](../../README.md#quickstart)'s own Quickstart:

```sh
bin/ehrt corpus generate
```

Real output, witnessed this session:

```clojure
{:status :ok, :payload {:out-dir "out/corpus/sim-s1-p1"}}
```

`out/corpus/sim-s1-p1` now holds four real files: `events.edn` (the
[ground-truth log](../glossary.md) — every admission, transfer, order, and discharge this
one simulated patient generated), `manifest.edn` (the reproducibility
record — the seed, the config, everything needed to regenerate this
exact corpus byte for byte), and two real HL7v2 messages,
`msg-000.hl7`/`msg-001.hl7`. Nothing downloaded, nothing configured,
and — Chapter 2's own punchline — nothing about this run that a second,
identical run won't reproduce exactly.

## Two phenomena hand-built data never has

A folder of sample HL7 messages someone wrote by hand tends to be
clinically tidy: messages arrive in the order they're supposed to,
encounters are self-contained, and nobody accidentally invents the
awkward cases real transport actually produces. This workspace's own
`ed-tuesday` scenario — the manual's one running example — has already
produced two of them, witnessed, not hypothesized.

### The wire that arrives out of order

Real transport delays messages — differently, per message type, per
event. [`ed-tuesday`](../../demos/scenarios/ed-tuesday/README.md)'s
"The second clock" section plays the exact same ground truth onto two
wires: one instant, one latency-realistic. On the latency wire, patient
Johnson, Michael (MRN000005)'s own two messages arrive in the reverse
of the order they were written in — the discharge first, and the
admission *last* — because a shorter sampled delay on a later event
outruns a longer one on an earlier event. The board, folding messages
strictly in the order they arrive, shows this once the admission lands:

```
-- board snapshot: 2026-08-14T06:43:23Z --

Emergency:
  ED-H01  Martin, William  MRN MRN000018  inpatient  attending: 5761303028
  ED-H03  Williams, John  MRN MRN000036  inpatient  attending: 5761303028
  ED-H04  Martinez, John  MRN MRN000009  inpatient  attending: 5761303028
  ED-H10  Miller, Robert  MRN MRN000096  inpatient  attending: 5761303028
  ED-H11  Taylor, Jennifer  MRN MRN000035  inpatient  attending: 5761303028
  ED-H16  Johnson, Michael  MRN MRN000005  inpatient  attending: 5761303028

Renal:
  RENAL-04  Wilson, Jessica  MRN MRN000024  inpatient  attending: 5761303028

inpatients: 5  active outpatients: 0  discharged: 64  merged: 1
```

Read the `ED-H16` line. Johnson was admitted at `01:12` and discharged
at `01:51` that morning, and the board's own previous snapshot showed
his bed `(cleaning)` — the bed cycle's view of his stay had already
moved on. Then his discharge arrived for a patient the board had never
seen, and his admission fifteen minutes after it put him on the board,
`inpatient` in a bed he had already left — where he stays, twenty
years of stream time later, at the run's last snapshot. He is one of
6 (of 111 admitted patients, that seed) whose own admission message
trails its own later event on the wire. Nobody drafting sample messages
by hand writes this on purpose — producing it for real takes an actual
second, independently-seeded transmit-delay clock sampling per message
type, which is precisely the kind of machinery a hand-built fixture
never runs.

### The encounter split across two deliveries

Real interfaces batch traffic on a schedule — hourly, nightly — using
HL7's own BHS/BTS batch protocol. `ed-tuesday`'s own "Batched delivery"
section runs the batcher hourly over that same latency wire:

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 10,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 15,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 18,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-490.hl7, one per occupied hour ...
   ],
  :span {:earliest-ms 1786406400000, :latest-ms 2416752000000}}}
```

(Excerpted for the three batches that matter here — the full 491-batch
listing is in the source, linked above.) Patient Hernandez, Sandra
(MRN000002, bed ED-H09) is admitted in
`batch-000.hl7` and discharged in `batch-002.hl7` — two clock-hours
later, skipping the batch in between entirely. A receiver holding the
first batch, or the first two, has transport-complete, `BTS`-verified
files (every message each declares is actually present) whose clinical
content is nonetheless half there.
Nobody hand-authoring a test set chooses to split one
[encounter](../glossary.md) across
two separately-valid delivery files on purpose — it takes an actual
scheduler drawing a batch boundary through the middle of a real
encounter's own timeline, which only happens when the traffic is
genuinely running, not imagined.

## Honest scope

This workspace is pre-release and says so plainly: the root
[`README.md`](../../README.md#maturity)'s maturity table is the actual,
per-capability contract with readers — not every capability here is
past "experimental" yet, and that table names which is which.
[`docs/what-is-this.md`](../what-is-this.md#scope--what-this-deliberately-does-not-do)
states what this workspace deliberately does not do: semantic
correctness checking (that's your own code, against your own
transforms), full terminology validation against licensed vocabularies,
production message routing, and a hosted validation service. Read both
before you decide this is the right tool for your task — not after.
