# docs/

Find yourself in the list below and follow that path — each one is a
handful of steps and ends wherever that audience's actual question gets
answered. [`docs/dev/AUDIENCES.md`](dev/AUDIENCES.md)'s
[Audience](dev/AUDIENCES.md#audience) section is the canonical
register these paths are keyed off; this page just routes.

Everything under this directory is complete at this level — no
Polylith vocabulary (component, base, project, brick), no repository
history to read first, and no discussion of this workspace's own
internal directory layout as architecture. A `components/...` string
still appears here and there as a literal filesystem path — real test
fixtures a copy-pasted command needs (`test-fixtures/v2`,
the same path the root README's own Quickstart uses), or a hyperlink
out to supplementary component-adjacent material for a reader who
wants more depth than this page promises. Neither requires knowing
what a "component" is; you're not meant to explore that directory,
only paste the path or follow the link. If you're maintaining or
extending this workspace rather than using it, go to
[`docs/dev/`](dev/) instead.

If a term below is unfamiliar — judge, verdict, findings, gate,
baseline, and the rest of the conformance vocabulary — **[`glossary.md`](glossary.md)**
is the authoritative definition set for this repo and its family.

## I don't know what this is yet

Start at [`what-is-this.md`](what-is-this.md) — the problem, who it's
for, what it does and doesn't do, and the evidence behind each claim.
Then the root [`README.md`](../README.md#quickstart)'s Quickstart —
one command at a time, real output, nothing to install beyond the
prerequisites in [`SETUP.md`](../SETUP.md).

## Task-first practitioner

You have a task: generate test traffic, break it on purpose, gate it,
or check it against your own expectations.

- **Want the guided, narrative version instead of jumping straight to
  a task?** [`docs/manual/`](manual/) is the learn-it path over this
  same reference estate, chaptered, starting from the sixty-second
  proof.

1. [`README.md`](../README.md#quickstart)'s Quickstart — run every
   stage once, for real, in order.
2. [`use-cases.md`](use-cases.md) — the full catalog of what you can do
   with this, formally, each anchored to what it actually composes
   from and each with a **You type:** strip you can paste directly.
3. [`cli.md`](cli.md) — every command group, verb, and flag `bin/ehrt`
   accepts; the CLI's own `ehrt help <group>` teaches the same spec.
4. [`operators.md`](operators.md) — every registered mutation operator,
   by id, for `ehrt corpus mutate --operator-id`.
5. [`locators.md`](locators.md) — the FHIR/v2 locator grammar
   `--locator-path` accepts.
6. [`simulate-your-facility.md`](simulate-your-facility.md) and
   [`site-profiles.md`](site-profiles.md) — modeling your own
   hospital's local dialect for `ehrt sim run`.
7. [`judge-calibration.md`](judge-calibration.md) — reading a gate's
   verdicts and findings in bulk, and what "no-verdict" means
   operationally.

## I have my own format

You need this traffic in a shape this workspace doesn't ship — a
proprietary interface, an internal schema, a vendor's flat file. You
don't need our emitters; you need the log underneath them.

[`use-cases/custom-emitter-from-the-event-log.md`](use-cases/custom-emitter-from-the-event-log.md)
is the path end to end: one command for the log, two worked example
emitters, and a way to check your own coverage.
[`formats.md`](formats.md#the-event-log)'s "The event log" is the
contract it's written against — 21 closed event kinds, generated from a
committed schema. The narrative version is the manual's
[Chapter 3](manual/03-a-simulated-hospital.md#the-log-underneath-every-message), "The log underneath
every message".

This is a different audience from "Downstream data consumer" below, and
the difference is worth a sentence: that reader never runs the CLI and
reads what a run *produced*; you run it once and then write code.

## Downstream data consumer

You read `report.edn`, `manifest.edn`, or lineage records — via
`--json` or EDN directly — and never run the CLI yourself.

[`formats.md`](formats.md) is the reference: the report, check report,
manifest, and lineage record shapes, each field table citing its
schema and backed by a real captured output.

## Evaluator

Deciding whether to adopt this at all, no task yet.
[`what-is-this.md`](what-is-this.md)'s Scope section names what this
workspace explicitly does not do; the root [`README.md`](../README.md)'s
maturity table is the actual per-capability contract with readers.

## Guide reader

Arriving from [`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
with a method chapter in hand, looking for which tool serves it — see
[`docs/dev/AUDIENCES.md`](dev/AUDIENCES.md)'s own referral-trigger
sections for the chapter-to-capability map.

## Maintaining or extending this workspace

Not what this page is for — go to [`docs/dev/`](dev/).
