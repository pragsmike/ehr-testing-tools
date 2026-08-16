# Chapter 8 — Your own data

Every chapter so far worked with data this workspace generated itself
— `sim`, Synthea, a mutant, a batch. This chapter is about the other
case: a corpus that arrived from somewhere else — a vendor delivery, a
partner's export, a foreign pipeline's own output — that you now need
to catalog, check, and keep an eye on over time. Nothing here assumes
you can reproduce the file the way Chapter 2's determinism contract let
you reproduce a generated one; the whole point is that you can't. What
you *can* do is give it the same provenance discipline this workspace
gives its own output, catalog it honestly as foreign, and hold it
against expectations you state explicitly.

## Cataloging what you didn't generate

`ehrt corpus intake` is the front door for a corpus you didn't
generate — cataloging it, one entry per file, by content hash rather
than by filename or a trust assumption. Every entry lands in the
[catalog](../glossary.md) tagged `:foreign` in its own
[corpus layer](../glossary.md), and the batch as a whole gets one
[intake record](../glossary.md) naming the source and the date you
received it.

**Witnessed: cataloging a delivered corpus** — copied verbatim from
[Acceptance QA of delivered/vendor corpora](../use-cases/acceptance-qa-of-vendor-corpora.md):

```bash
VENDOR_CORPUS=test-fixtures/v2
bin/ehrt corpus intake --path $VENDOR_CORPUS \
  --label acme-delivery --received 2026-07-26 \
  --out out/acceptance/intake
cat out/acceptance/intake/intake-record.edn
```

```clojure
{:origin "acme-delivery", :date "2026-07-26", :file-count 8,
 :catalog-hash "ecfc531791c4283c857739f2b656cdf2d670e72114078374059bf93131db33aa"}
```

(`:file-count 8` counts every regular file under the directory,
recursively — the five `.hl7` messages plus the simhospital sidecar's
three provenance files; the gates later in this chapter take only the
five `.hl7`, which is why their totals read 5.)

Every catalog entry alongside it carries its own content hash — the
same `sha256` identity a [lineage](../glossary.md) record uses to name
a mutant's parent — and `:layer :foreign`, `:origin "acme-delivery"`,
`:received "2026-07-26"` copied onto every one of the eight files this
delivery actually contained. The `catalog-hash` on the intake record
itself is the sha256 of the catalog's own bytes, so "this is the
delivery I accepted" stays checkable against the record later, rather
than against memory ([formats.md](../formats.md)).

**Why `--received` takes a real date, not a seed.** Every other
timestamp-shaped input this workspace takes — `--reference-date` on
`corpus generate`, `--seed` — is pinned so the *output* is
deterministic: the same input always produces the same bytes.
`--received` is a different kind of field entirely. It doesn't
describe anything this workspace computed; it records a fact about the real
world — the day this batch actually arrived at your door — and that
fact isn't something any generator could derive from the corpus's own
content. Defaulting it to today's date is therefore honest rather than
a determinism gap: a foreign corpus's arrival date is provenance about
an event outside this workspace, not data this workspace produced, so
the determinism contract Chapter 2 taught never applied to it in the
first place. Pass `--received` explicitly, as the strip above does,
when you're cataloging a delivery after the fact rather than the day
it landed.

## Checking against expectations

Cataloging tells you what arrived. `ehrt check` tells you whether what
arrived is what you expected — the corpus's second judge, alongside
Chapter 7's gates, but asking a different kind of question. A gate
checks a file against a standard it didn't write; `check` compares a
candidate directory against *your own* expected corpus, or against
explicit assertions you state yourself.

**Witnessed: golden equivalence, a corpus checked against itself** —
copied verbatim from the root `README.md`'s own Quickstart:

(This checks the Synthea corpus the
[Quickstart](../../README.md#quickstart) generates, which no chapter of
this manual creates for you — run its `bin/ehrt artifact fetch --name
synthea …` / `--name temurin-jdk …` pair and then `bin/ehrt corpus
generate synthea` first, or regenerate them if `out/` was cleared.)

```bash
bin/ehrt check out/corpus/synthea-s1-p5/fhir --expected out/corpus/synthea-s1-p5/fhir
```

Re-derived fresh this session, a freshly generated `out/corpus/synthea-s1-p5`:

```clojure
{:status :ok,
 :payload
 {:run {:check {:name "check", :version "v1"},
        :candidate-dir "out/corpus/synthea-s1-p5/fhir",
        :expected-dir "out/corpus/synthea-s1-p5/fhir",
        :assertions [{:kind :matches-expected}],
        :pair-by :path, :canonicalizers []},
  :totals {:pass 7, :rejected 0, :indeterminate 0, :no-verdict 0},
  :by-code {},
  :files [{:path "Abdul218_Schoen8_352cccfc-0946-b8f0-a793-a1897e7f48b6.json",
           :verdict :pass, :finding-count 0, :findings []}
          ;; ... four more patient files and both info files, all :pass ...
          ]}}
```

Checking a corpus against itself is the trivial case — every file
paired with itself always passes — but it's the same code path a real
comparison uses: pair a candidate against an expected directory by
path (or by content hash, `--pair-by hash`), canonicalize if the two
sides have superficial differences you don't care about, and report
`content-mismatch`/`missing-file`/`extra-file` for whatever doesn't
line up. `ehrt check` produces the exact same `Report` shape the gates
do — same `:totals`, same `:files`, same finding envelope — but every
verdict it emits is binary. There's no `:no-verdict` here the way
Chapter 7 taught: an expected corpus or an explicit assertion either
matches or it doesn't, so there's no partial-check state for a third
answer to name.

Golden equivalence — comparing a whole candidate directory against a
whole expected one — is one mode. The other is a per-file assertion
vocabulary you state explicitly in an `--assertions` EDN file instead
of, or alongside, an expected corpus: `:present`/`:absent` (does a
datum exist at a locator), `:value` (does it equal a stated value),
`:count` (does a repeated element occur the right number of times),
`:schema` (does a datum validate against a registered schema). The
[check report reference](../formats.md#the-check-report) is the
complete vocabulary and every code it emits, per assertion kind, in
one table — not restated here.

## Baselining: watching the same corpus over time

`check` compares two corpora once. `--baseline` mode, on the gates
themselves, answers a narrower and more common question: gating the
*same* corpus repeatedly, has anything genuinely new shown up since
the last run you trusted?

**Witnessed: pin a baseline, then gate relative to it** — copied
verbatim from
[Regression baselining / drift detection](../use-cases/regression-baselining.md):

```bash
# The run you trust becomes the baseline. Keep this file.
bin/ehrt gate v2 test-fixtures/v2 --report out/regression/baseline.edn

# ...later, the same corpus again -- but judged relative to that
# baseline: a finding counts toward rejection only if its
# {severity, code, locator-path} isn't already in the baseline for
# that same file. The exit code follows the relative view, so a
# corpus that is identically noisy to its baseline still exits 0.
bin/ehrt gate v2 test-fixtures/v2 \
  --report out/regression/today.edn \
  --baseline out/regression/baseline.edn
```

Re-run fresh this session, exit code `0` both times: the baseline and
today's run over the same unchanged fixtures both come back
`{:pass 5, :rejected 0, :indeterminate 0, :no-verdict 0}`, and the
`--baseline` payload's own `:relative` half agrees with `:absolute` —
nothing new to report, because nothing changed. That's the honest
result for an unchanged corpus, not a weaker test: `--baseline` mode
answers *did anything new appear*, and matching is exact on the
`{severity, code, locator-path}` triple, so a finding whose message
text drifts still matches but one whose locator moves does not
([formats.md](../formats.md#baseline-mode-changes-the-payloads-shape)).
The payload's shape changes under `--baseline`, too — it's no longer a
bare report but `{:absolute <Report> :relative <Report>}`, `:absolute`
always the unfiltered truth, `:relative` the filtered view the exit
code actually follows. Chapter 7's dominance ordering still governs
`:absolute`; `:relative` is simpler by design — always `:pass` or
`:rejected`, never `:no-verdict`, even when the absolute verdict was.

## Closing pointers: the reference estate from here

This manual has been teaching from the practitioner's side — you,
running commands, reading what comes back. Cataloging, checking, and
baselining are also where a second reader enters: someone who never
runs `ehrt` at all, who receives a `report.edn`, a `manifest.edn`, or a
directory of lineage records from someone who did, and has to make
sense of it in Python, a warehouse, or a dashboard. That reader's own
front door is [`formats.md`](../formats.md) — every shape this
workspace emits, field by field, including the `--json` projection for
a reader with no EDN parser to hand. Alongside it,
[`locators.md`](../locators.md) is the grammar behind every
`:locator` a finding or a lineage record ever names — what a FHIR data
path or an HL7 v2 segment/field address actually means, including the
`MSH` off-by-one that trips up a first read of a v2 finding. Neither
page repeats what this chapter already taught; both are the place to
go next once you're the one on the other end of the pipeline.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt corpus intake --path $VENDOR_CORPUS --label acme-delivery --received 2026-07-26 --out ...` and its intake record | `docs/use-cases/acceptance-qa-of-vendor-corpora.md`; witnessed this session against `test-fixtures/v2` |
| `bin/ehrt check out/corpus/synthea-s1-p5/fhir --expected out/corpus/synthea-s1-p5/fhir` | `README.md`, Quickstart; witnessed this session against a freshly generated five-patient Synthea corpus |
| `bin/ehrt gate v2 test-fixtures/v2 --report ... [--baseline ...]` (baseline pair) | `docs/use-cases/regression-baselining.md`; witnessed this session, both runs exit `0` |
