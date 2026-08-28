# Chapter 5 — Batch delivery

Chapter 4 gave one message two clocks. This chapter gives a whole
*shift* one more piece of realism most real interfaces actually run:
delivery isn't continuous. Real EHR feeds rarely stream message by
message forever — most interfaces batch their traffic on a schedule,
hourly or nightly, using HL7 v2's own batch protocol. This is the
manual's featured chapter — the case the author's own charter singled
out by name for exactly this placement: "featured prominently" in the
tool-specific user manual, because it's something that happens in real
feeds and can trip up the unaware. Everything below builds toward one
patient, split across two files that are each, individually, perfectly
clean.

## `ehrt corpus batch`, and why it doesn't know about the simulator

[`docs/cli.md`](../cli.md#ehrt-corpus-batch) describes `ehrt corpus
batch` in one line worth sitting with: it partitions every HL7 v2
message under a directory into schedule-aligned delivery batches,
sorted by MSH-7 across every candidate file, and writes one BHS/BTS-
wrapped `batch-NNN.hl7` per occupied interval. It is a **corpus-level**
tool, deliberately separate from the simulator that (in this manual)
happens to produce its input. That separation was an explicit author
ruling, not an implementation convenience: asked whether the batcher
should work only on this workspace's own generated corpora, the
author's answer was "It should work on any corpus, even an existing
directory of foreign (but valid) message files." `ehrt corpus batch`
takes any directory of valid `.hl7` files — `ed-tuesday`'s own
latency out-dir happens to be one, not a special case the batcher
knows about.

**Run the batcher over the same latency wire Chapter 4 generated,
hourly** — copied verbatim from the demo README's own "Batched
delivery" section:

```bash
bin/ehrt corpus batch out/scenarios/ed-tuesday-latency --interval 60 \
  --out-dir out/scenarios/ed-tuesday-latency-batches
```

`--interval` is minutes (60 here — hourly), and it's a required flag
with no default: [`docs/cli.md`](../cli.md#ehrt-corpus-batch) states
why plainly — "there is no universally sensible schedule to assume."
Buckets align to the Unix epoch, so hourly batches align to the clock
hour, daily batches to UTC midnight, regardless of what the corpus's
own reference date happens to be.

## The witnessed batch listing

Witnessed this session (same seed-20260811 run Chapter 4 generated:
283 messages across 34 occupied hourly buckets, `2026-08-11T00:00Z`
through `2026-08-12T13:00Z`), re-derived by fresh regeneration from
`demos/scenarios/ed-tuesday/config-latency.edn` — byte-identical to
`ed-tuesday`'s own README:

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 3,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 4,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 5,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-030.hl7, one per occupied hour ...
   {:file "batch-031.hl7", :count 1,
    :start-ms 1786518000000, :end-ms 1786521600000, :verified true}
   {:file "batch-032.hl7", :count 2,
    :start-ms 1786528800000, :end-ms 1786532400000, :verified true}
   {:file "batch-033.hl7", :count 1,
    :start-ms 1786536000000, :end-ms 1786539600000, :verified true}],
  :span {:earliest-ms 1786406400000, :latest-ms 1786539600000}}}
```

**Every one of the 34 written files self-verified.**
`write-and-verify-batch!` decodes what it just wrote straight back and
checks BTS-1 against the real message count before ever reporting
success — `:verified true` on all 34 is that check, exercised, not
merely claimed. That word matters for what comes next: "verified"
here means transport-level verified, and transport-level is exactly
the level this chapter is about to show you isn't the whole story.

**The interior gap is real, and it's disclosed, not hidden.**
`batch-031` spans `[07:00Z, 08:00Z)` and `batch-032` spans `[10:00Z,
11:00Z)` on 2026-08-12 — the two hours between them, `08:00Z`–`10:00Z`,
carried no traffic at all in this run's own tail and are simply
absent, never written as an empty file. This is a named v1 deferral
(an interior empty batch isn't represented, only skipped), not a bug
— worth knowing before you write a receiver that assumes every
interval in a span gets its own file.

## The wrapper itself

`batch-000.hl7`, head and tail — copied verbatim from the demo
README:

```
$ head -c 100 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7
BHS|^~\&

MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003026+0000||ADT^A01|MRN000002-A01-360|P|2.4EVN|A0
$ tail -c 45 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | cat -A
dicare-advantage|Medicare Advantage^M$
$
BTS|3$
$
```

`BHS|^~\&` opens the batch; `BTS|3` closes it, `BTS-1` naming the true
count of 3 messages this file actually carries — the minimal,
deterministic field set the batcher's own design rules for v1: no
creation-time field populated at all, so the
[determinism](../glossary.md) contract
Chapter 2 proved for the simulator holds here trivially, rather than
by threading a wall clock through and hoping it doesn't leak into the
bytes.

That truncated `head -c 100` cuts off mid-segment — it's Smith,
James's own admission message, the same patient this chapter is
building toward. Its full MSH segment, witnessed this session by
fresh regeneration (byte-identical to the values `ed-tuesday`'s own
README states):

```
MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003026+0000||ADT^A01|MRN000002-A01-360|P|2.4
```

## The straddle: one encounter, two individually clean files

**Smith, James (MRN000002, bed ED-H05)**, admitted (A01) and
discharged (A03) both on the same, ordinary shift — nothing dramatic
happened to Smith clinically, which is precisely the point. His
admission message transmits at `2026-08-11T00:30:26Z`, landing in
`batch-000.hl7`'s window `[00:00Z, 01:00Z)`. His discharge message
transmits at `2026-08-11T01:34:19Z` — one clock-hour later, the very
next occupied batch, `batch-001.hl7`'s window `[01:00Z, 02:00Z)`. The
two lines themselves, witnessed this session by fresh regeneration,
MSH segments byte-faithful:

```
$ grep 'MRN000002-A01' out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | tr '\r' '\n' | head -1
MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003026+0000||ADT^A01|MRN000002-A01-360|P|2.4

$ grep 'MRN000002-A03' out/scenarios/ed-tuesday-latency-batches/batch-001.hl7 | tr '\r' '\n' | head -1
MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811013419+0000||ADT^A03|MRN000002-A03-2280|P|2.4
```

(HL7 v2 segments are `\r`-terminated, not `\n`-terminated — `tr` makes
each segment its own shell line so `head -1` isolates just the MSH
segment; `bin/demo-exerciser-ed-tuesday`'s own straddle assertion
greps for the same MSH-10 control-ID prefixes shown here,
`MRN000002-A01-` in `batch-000.hl7` and `MRN000002-A03-` in
`batch-001.hl7`, unparsed segment text and all — this chapter's own
excerpt is that same check, made visible.)

Notice what actually pushed the discharge into the next batch: it
isn't that Smith's own [encounter](../glossary.md) ran long. His EVN-2 clinical times —
admitted `00:06:00Z`, discharged `00:38:00Z` — sit comfortably inside
one hour, entirely within `batch-000`'s own window. It's the
discharge message's own sampled transmit delay, Chapter 4's own second
clock, that carried its MSH-7 across the `01:00Z` boundary and into
the next file. The straddle you're about to reason about is the
direct, compounding effect of the previous chapter's own mechanism
meeting a delivery schedule — not a new, unrelated phenomenon.

**A receiver holding only `batch-000.hl7` has a transport-complete
file.** Three of three messages present, exactly as `BTS-1` declares,
`:verified true` — by every transport-level measure, nothing is wrong
with this file. And yet, clinically, Smith's own encounter is only
half there: his admission, and nothing else for him. Nothing in the
batch protocol itself says otherwise; `batch-000.hl7`'s own BTS-1
count checks out whether or not any of the encounters it carries are
clinically finished.

<img src="assets/straddle-timeline.svg" alt="One encounter bar for Smith, James (MRN000002) crossing the 01:00Z batch boundary, both adjacent batch windows drawn individually clean" width="640" />

## The question this teaches, not the flags that answer it

The lesson isn't "remember to pass `--baseline`" or any other flag —
it's a question a receiver has to ask itself, one this workspace can't
ask on its behalf: **do I have all of this encounter?** Transport-level
completeness — every `BTS-1` count checks out, exactly as this run's
own 34-for-34 self-verification shows — says nothing about
clinical-level completeness — whether an encounter's own full record
set has actually arrived yet. Smith's own case is exactly the input a
receiver's own "do I have all of this?" decision needs to be tested
against, and precisely the shape of case nobody hand-authoring a fixed
test set chooses to build on purpose: it takes an actual scheduler
drawing a batch boundary through the middle of a real encounter's own
timeline, which only happens when the traffic is genuinely running,
not imagined. If your own receiver logic has an answer for Smith, this
is how you'd find out. If it doesn't yet, this is the case that would
have told you so quietly, in production, months from now.

[`docs/use-cases/supply-batch-straddling-traffic.md`](../use-cases/supply-batch-straddling-traffic.md)
is the reference walkthrough for generating this exact input on its
own, flags and defaults linked from there
([`docs/cli.md`](../cli.md#ehrt-corpus-batch)) — this chapter taught
the *why*; that page is where you go to run it against your own
receiver. The full scenario, wrapper bytes and all, is
`ed-tuesday`'s own [Batched
delivery](../../demos/scenarios/ed-tuesday/README.md#batched-delivery)
section — this chapter's own excerpts are drawn from it, not composed
for the occasion.

**A taxonomy note worth carrying forward.** `ed-tuesday`'s own README
states this precisely, for the record: transport realism — Chapter 4's
delayed individual transmission, this chapter's schedule batching —
simulates **correct** transport behaviors, deterministically. Mutation
(a later chapter's own subject) deliberately injects **incorrect**
content, with an expected finding attached. Nothing about Smith's own
straddle is wrong, malformed, or a defect anywhere in this workspace
— it's what correct, real transport actually does, and a downstream
receiver has to be ready for it regardless.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt corpus batch out/scenarios/ed-tuesday-latency --interval 60 ...` | `demos/scenarios/ed-tuesday/README.md`, "Batched delivery" |
| The 34-batch listing (`:status :ok, :payload {...}`) | `demos/scenarios/ed-tuesday/README.md`, "Batched delivery"; re-derived byte-identical by fresh regeneration this session |
| `head -c 100`/`tail -c 45` of `batch-000.hl7` | `demos/scenarios/ed-tuesday/README.md`, "Batched delivery" > "The wrapper itself" |
| Smith's full A01 MSH segment | witnessed this session, fresh regeneration; extends the README's own truncated `head -c 100` excerpt to the full segment, same message |
| Smith's A03 MSH segment (`batch-001.hl7`) | witnessed this session, fresh regeneration; matches the MSH-7 value (`2026-08-11T01:34:19Z`) the README's own "A straddling encounter" section states in prose |
| Smith's EVN-2 clinical times (`00:06:00Z`, `00:38:00Z`) | witnessed this session, fresh regeneration against `demos/scenarios/ed-tuesday/config-latency.edn`, seed 20260811 |

All values above were produced by re-running `demos/scenarios/ed-tuesday/README.md`'s
own commands against this session's own tree
(`bin/ehrt corpus generate sim` / `bin/ehrt corpus batch`, seed 20260811,
`config-latency.edn`) rather than assumed from the README's prose alone
— every batch count, every `:verified` value, and both of Smith's own
MSH-7 timestamps matched the README's own witnessed run exactly, byte
for byte. No divergence found.
