# Provenance — downstream calibration config

`config.edn` beside this file is a **third-party artifact**, not a
fixture this repository authored: it is the exact EDN a downstream QA
team supplied alongside a controlled calibration of `ehrt sim run`,
vendored byte-for-byte so this repository can reproduce what they
measured. Its bytes are the fixture. An edit to it — a reformat, a
newline normalization, a "harmless" key reorder — invalidates every
figure below and every SHA-256 comparison that cites them.

## Source

| | |
|---|---|
| Source | downstream QA team, controlled calibration (channel-held), reported 2026-09-02 |
| Received | 2026-09-02 |
| Their simulator commit | `386e738d95e49b0a2aefccfedbf20d172a1fcfa9` |
| Their simulator tree | `c8ab539224df649585610d969be509234184fa79` |
| Their reported version | `pre-release / stable-20260821-patient-simulator-charter-230-g386e738d` |
| Their event schema version | `1.8.0` |
| Seed | `424242` |
| Reference date | `2026-08-31` |
| Flags | `--churn --format ground-truth` |
| Their config path at measurement time | `/tmp/ehrt-maintainer-followup/config.edn` |

Their commit was 88 commits behind this repository's `main` at the time
this fixture was vendored; their own report notes that the requested
newer object was not present in their checkout, so no newer revision was
launched on their side.

## Bytes

| | |
|---|---|
| File | `config.edn` |
| Size | 2,911 bytes |
| sha256 | `4dd4a5c01a4d9fefce77f92b16526aaa38378dcc40a0a1d1d61ddc1f66f07e02` |

That hash is the fixture's identity. Any session citing a figure below
verifies it first.

## What the config asks for

Fifteen keys, of which the load-bearing ones for the results below are:
a three-ward facility whose emergency ward has **zero standing beds and
500 surge slots**, five weighted pathways plus four `:patient-ordinal`
module-only entries, four vendored GMF modules over a 365-day horizon,
`:persons {:count 20000 :years 20}`, and `:encounters`, `:bed-cycle`,
`:scheduling`, `:chatter`, `:charges`, `:ladders` and `:siu` all on. The
person fold is two orders of magnitude larger than the arrival count in
every run below, which is why the 100-arrival run already produces
23,961 events.

## Their exact command template

```text
./bin/ehrt sim run --seed 424242 --patients N --reference-date 2026-08-31 --churn --config /tmp/ehrt-maintainer-followup/config.edn --format ground-truth
```

The explicit per-run timeout was 15 minutes with a 30-second
kill-after grace period. No JVM heap settings were overridden.

## Their result table, verbatim

| arrivals | result | top-level events | wall time | user CPU | system CPU | max RSS | output bytes | notes |
| -------: | ------ | ---------------: | --------: | --------: | ----------: | -------: | ------------: | ----- |
| 100 | completed, exit 0 | 23,961 | 71.92 s | 107.71 s | 1.53 s | 1,122,512 kB | 7,988,897 | 20 event kinds |
| 500 | completed, exit 0 | 29,063 | 79.71 s | 119.79 s | 1.63 s | 1,184,328 kB | 9,751,861 | 21 event kinds; byte-identical to retained payload |
| 1,000 | completed, exit 0 | 35,408 | 87.92 s | 128.50 s | 1.58 s | 1,154,768 kB | 11,966,511 | 21 event kinds |
| 2,000 | structured error, exit 2 | — | 134.44 s | 175.44 s | 1.82 s | 1,350,432 kB | — | `:self-check-failed`; partial stdout 2,985,469 bytes discarded; not a timeout |

Their completed-output SHA-256 values:

- 100: `42acd268a5f506bf4f65bd087412421977fdc628479fd4ceef331c6d3f453119`
- 500: `434232a913c3389fdc3856f9a6eb14854ff6174499e8a5caa0643085824a03d5`
- 1,000: `ddcfc319ffed230a1ce2edd13f62f2fbfd4fd4264eface5bf6a37967ba2deb11`

Their own qualification, carried across so it is not lost: the four
measurements do not establish a performance law, and the completed
points are **not** consistent with a claim of super-linear wall-time
growth over these cases. The 2,000-arrival failure is a simulator
self-check error, not an HL7 capacity failure — no emit path was
exercised in any of these runs.

Their event counts exclude nested `:pre-horizon-facts`; that is this
repository's own counting rule ([`docs/formats.md`, "Read the top-level
vector only"](../../docs/formats.md#read-the-top-level-vector-only)),
so their counts and this repository's are the same quantity.

## Their environment

| | |
|---|---|
| OS | Linux, kernel `6.12.90`, `x86_64` |
| CPU | Intel Core i3-7100H @ 3.00GHz — 2 physical cores, 4 logical |
| RAM | 16,136,604 kB (~15.4 GiB) |
| JDK | OpenJDK 17.0.18, 64-bit Server VM |
| Estimated JVM default max heap | 3.85 GiB |
| Clojure CLI / tools.deps | `1.12.3.1577` |
| Heap override | none; default ergonomics |

**This repository's own machine differs**, and the difference is
material to any byte-for-byte comparison: JDK 21 rather than JDK 17.
A session reproducing their SHA-256 values records that as a disclosed
deviation rather than treating a mismatch as a defect on its face.

## Why this is vendored

To reproduce, shrink and diagnose the deterministic `:self-check-failed`
(exit 2) their 2,000-arrival run hit. The config is the whole of the
input — seed, arrival count, reference date and `--churn` are flags —
so without these exact bytes the failure is not reachable.
