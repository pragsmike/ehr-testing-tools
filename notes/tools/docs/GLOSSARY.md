# Glossary

This is the **authoritative home** of the family's conformance-and-gating
vocabulary — judge, verdict, findings, gate, baseline, and the terms
that collide with clinical usage in a healthcare tooling project
([ADR-0018](../notes/ADRs.md)). The sibling
[`ehr-testing-sim`](https://github.com/pragsmike/ehr-testing-sim) repo's
own `docs/GLOSSARY.md` uses these same terms and points here rather than
redefining them. Every entry below cites its register of record — an
ADR, a namespace, or a doc — so this page is downstream of doctrine, not
a second source of it: if wording here ever disagrees with the cited
register, the register wins and the disagreement is a bug in this page.

## Conformance & gating vocabulary

**Judge.** A component that examines one artifact against one tier of
checks — e.g. the base-structural v2 judge (`judge.v2`, over HAPI), or
the base-spec FHIR judge (`judge.fhir`, over the official validator).
*Judging* is the act; a judge decides, it does not act on what it
decided — that's the Gate's job, not the judge's (`docs/palgebra-design.md`
D1/D2, the judge/gate factorization). Register: `docs/palgebra-design.md`
D1–D2, [ADR-0009](../notes/ADRs.md).

**Verdict.** A judge's per-artifact classification, four arms:
`:pass` / `:rejected` / `:indeterminate` / `:no-verdict`. `:indeterminate`
is **RESERVED**: kept in the enum only because old, pre-split baseline
reports still serialize it, but nothing in this repo has produced it
since R3 — the case it used to name (a check needing a terminology tier
the judge lacks) is now `:no-verdict` instead. `:no-verdict` is always
paired with a `:cause` keyword in a sibling field, present if and only
if the verdict is `:no-verdict` (Malli-enforced,
`valid-cause-pairing?`) — `:terminology-suppressed` (the validator ran
offline, so a terminology-bound code was never actually checked) is the
worked example and, as of this writing, the only member of the `Cause`
enum. `worst-of`'s composition law ranks
`:pass` < `:indeterminate` < `:no-verdict` < `:rejected`: a confirmed
violation dominates the aggregate over incidental partiality elsewhere
in the same file (the R3 rationale — see `finding.clj`'s own docstring
for why the first draft's opposite ordering was falsified by
measurement). The CLI's exit-code ladder follows: `0` pass, `1`
rejected, `2` operational error, `3` the aggregate contains
`:no-verdict` under the default policy — `--treat-no-verdict-as
pass|rejected` is the explicit opt-in to fold it into an existing
polarity. Register: `src/ehr_testing_tools/judge/finding.clj`
(`Verdict`, `Cause`, `VerdictOutcome`, `worst-of`), [ADR-0010](../notes/ADRs.md)
as amended by R3, `docs/palgebra-design.md` D10, `src/ehr_testing_tools/cli/help.clj`
(`exit-codes`, `--treat-no-verdict-as`).

**Error (vs. rejected).** An *error* is the judge itself failing
operationally — it could not run (a bad invocation, a missing
artifact, a crashed subprocess). A crashed judge yields an error, never
a verdict. Rejected is an answer; error is the absence of the ability
to answer. Keeping these apart is load-bearing: a corpus full of
rejections is information, a corpus full of errors is a broken
harness. Register: `src/ehr_testing_tools/cli/help.clj` (`exit-codes`,
code `2`), [ADR-0010](../notes/ADRs.md)'s consequence section (the
CLI's exit-code mapping).

**Findings.** The itemized, located reasons attached to any
non-`:pass` verdict: each names the check that fired, where in the
artifact, and the stated reason (`Finding`'s `:severity` / `:code` /
`:locator` / `:message` / `:engine`). Findings are the actionable
content of a verdict — and, in the cross-repo consumer loop, the
currency in which the gate reports what a producer (sim, or anything
else feeding this repo's Gate) should fix. "Findings, not failures" is
that loop's assertion discipline: integration tests assert the gate
*runs and verdicts*, never that everything passes — a corpus that's too
well-behaved to reject anything is itself measured, not assumed.
Register: `src/ehr_testing_tools/judge/finding.clj` (`Finding`),
[ADR-0013](../notes/ADRs.md) (decision 3, "findings, not failures").

**Report.** The aggregate a gate run produces over a corpus: the
verdict table plus all findings, one entry per file. Register:
`ehr-testing-tools.judge.report`, `docs/formats.md`.

**Baseline.** A pinned, committed report with a provenance header
(date, the commit it was generated against, reason). Deltas against a
baseline are how change is *reviewed*: a new corpus is diffed, findings
are read, and only then is the baseline regenerated — ratification by
regeneration, with the history in the headers ([ADR-0013](../notes/ADRs.md)
decision 4). The cross-repo consumer loop maintains **two** baselines,
not one: a **legacy-floor** baseline (the plainest default pathway —
cheap, long-running, proves the judge still runs clean over the
simplest traffic) and a **full-capability** baseline (a wider,
deliberately-scoped reference corpus exercising the current breadth of
message types — order/result, module cohorts, the full churn trigger
family). Neither supersedes the other: a floor and a breadth picture
measure different things, and folding one into the other would either
destroy the floor's fixed-minimal property or leave the breadth picture
permanently stale. Future milestones are candidate third/fourth
baselines under the same policy, each scoped and named for what it
actually covers. Register: [ADR-0013](../notes/ADRs.md) (baseline-delta
discipline), [ADR-0015](../notes/ADRs.md) (the two-baseline decision).

**Gate.** The workflow that runs judges across a corpus and acts on
their verdicts — the CLI verb `ehr gate` genuinely is a gate (its
exit-code mapping is policy; `--baseline` is an explicit policy
argument), where the libraries underneath (`judge.fhir`, `judge.v2`)
are judges. Contrast **Judge**: a judge only decides, gating is what
happens with the decision. Register: `docs/palgebra-design.md` D1
(three layers: observe → judge → act) and D12 (the CLI verb keeps the
name "gate"; the libraries use judge vocabulary).

**Diagnosis** (never a judge term). *Diagnosis* names a clinical
determination elsewhere in the project family — content a simulator
*generates*, the thing that gets an ICD-10-CM code and rides in a DG1
segment. It is never used for a judge's explanation of a verdict — that
is **Findings**, above. The restriction is deliberate: in a healthcare
tooling project, "the gate's diagnosis" would be parsed clinically by
half the audience, and findings/diagnosis already carries a pleasant
non-colliding coincidence (clinicians also call itemized observed facts
"findings"), so keeping the words apart costs nothing and avoids a real
misreading. Register: this repo's own **Findings** entry above and
[ADR-0013](../notes/ADRs.md); the ruling itself is a family convention,
first stated in `ehr-testing-sim`'s `docs/GLOSSARY.md` colliding-terms
section.

**InjectChurn** (disambiguation). `ehr-testing-sim`'s `InjectChurn`
transform weaves coherent *operational* churn — cancel/reschedule
events, bed swaps, merges — into a simulated pathway; it is not this
repo's fault injection. Fault injection — deliberately breaking a
message or bundle to violate a stated conformance constraint — lives
here, in the mutation operators (see **Corpus vocabulary** below).
The names sound alike; the concepts don't share a register. Register:
`docs/operators.md` (this repo's fault injection); `ehr-testing-sim`'s
own glossary owns `InjectChurn` itself.

## Corpus vocabulary

A short, bounded set of terms for this repo's own corpus-construction
layer (generation, mutation, cataloging) — sourced only from existing
docstrings and docs already in this repo, not new doctrine.

**Operator.** A registered defect transform, applied by
`--operator-id`/`--operator-version` at a `--locator-path`, to every
matching file under `PATH` (positional, or `--path`). Each operator names what it edits (the
change) and its contract (which base-spec constraint the edited file
now violates). Register: `docs/operators.md`.

**Mutant.** The file an operator produces — a deliberately broken
variant of a base bundle, with a lineage record tracing it back to
where it came from. Register: `docs/formats.md` ("The lineage record"),
`src/ehr_testing_tools/corpus/operators.clj`.

**Lineage.** The provenance record (`<output-dir>/lineage/<filename>.lineage.edn`)
tracing a mutant back to its parent's content hash, the operator and
locator applied, and the contract violated — content-addressed and
append-only, so a directory of lineage records is the real derivation
graph. Register: `ehr-testing-tools.lineage/LineageRecord`,
`docs/formats.md`.

**Catalog.** The index `corpus.intake` builds over a corpus: one entry
per item, carrying id, layer, format, a lineage ref where one exists,
and tags. Meaning lives in the data, not in filenames. Register:
`src/ehr_testing_tools/corpus/intake.clj` (`CatalogEntry`).

**Intake.** The ingestion route that catalogs a corpus — generated by
this repo or foreign to it — and, when a manifest sidecar is present
and valid, enriches every catalog entry in that same directory with the
manifest's own provenance. An absent or invalid sidecar leaves the
catalog unenriched, recorded as a note, never an error. Register:
`src/ehr_testing_tools/corpus/intake.clj`.

**Manifest sidecar.** A `manifest.edn` file beside a generated (or
intaken) corpus, validating against `corpus.manifest/ManifestV1_1`,
naming the generator, seed, and settings that produced the corpus it
sits beside. Register: `src/ehr_testing_tools/corpus/intake.clj`,
`docs/formats.md`.

**Corpus layer.** The `:layer` field on a catalog entry, naming a
corpus item's provenance kind — currently `:foreign`, the tag intake
gives every entry from a corpus this repo did not generate (a partner
export, a vendored fixture). Register: `src/ehr_testing_tools/corpus/intake.clj`
(`CatalogEntry`'s `:layer`).
