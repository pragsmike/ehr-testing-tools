# Chapter 7 — Judging

Chapter 6 broke a file on purpose and watched the gate notice. This
chapter is about the gate itself — what "notice" actually means, which
of the three gates you'd reach for, and why a gate sometimes answers
neither yes nor no. Gating is structurally upstream of anything Chapter
8 teaches: it asks whether a file conforms to a standard it didn't
write and doesn't own — HL7 v2, FHIR — never whether the file matches
*your own* expectations. That second question, checking a file against
a caller's own expected corpus or assertions, is a different judge with
a different job; Chapter 8 is where it lives.

## Three gates, three different questions

`ehrt gate` dispatches to one of three engines, and they check
genuinely different things — a clean verdict from one is not a
prediction about another.

**`gate fhir`** runs the real official FHIR validator against the base
spec, plus whatever implementation guide the resource's own
`Resource.meta.profile` declares (no `-ig` flag needed to trigger that
— it auto-loads). This is the gate Chapter 6 already exercised: base
FHIR requirements like element cardinality and lexical formats, and
transitively, any real-world profile a file claims to follow.

**`gate v2`** runs the HAPI engine's base-structural tier against HL7
v2 messages — message-structure resolution, encoding/delimiter
well-formedness, and primitive data-type checks, all wired into parsing
itself. It has no concept of a conformance profile; it can't tell you
whether a field is *usually* populated, only whether the message
parses at all and whether the fields it does check are lexically valid.

**`gate v2-nist`** runs the NIST profile-tier engine against a specific
conformance-profile bundle you supply — usage, cardinality, length,
co-constraints, slicing, value-set bindings: everything `gate v2`'s
base-structural tier structurally cannot check. It needs `--profile`
explicitly; there's no default bundle to fall back to silently, because
there's no universally correct profile to assume.

Flags and defaults for all three: [`cli.md`](../cli.md#ehrt-gate),
linked rather than restated here.

## Verdict semantics: ok, rejected, and the honest third answer

A gate hands back a verdict per file — `:pass`, `:rejected`, or
`:no-verdict` — and the third one is worth understanding on its own
terms, not as a variant of the other two.

<img src="assets/verdict-ranking.svg" alt="The verdict decision: rejected dominates no-verdict dominates pass -- a file's verdict is the worst one among its findings, empty means pass" width="640" />

**`:rejected`** means the judge applied its criterion and found a real
violation — Chapter 6's own mutant, `:invalid`, "Unable to find
resourceType property." A rejected verdict is trustworthy: something
the judge actually checked came back wrong.

**`:pass`** means the judge applied its criterion and found nothing
wrong — but it's a claim about *what was checked*, not a guarantee of
correctness beyond that scope. A file that gates clean at `gate v2`
hasn't been checked against any profile at all; that tier doesn't own
that question.

**`:no-verdict`** is the one worth pausing on, because it answers a
different question than either of the above. It doesn't mean "the
message is fine" and it doesn't mean "the message is broken" — it means
the judge *couldn't fully apply its own criterion* to begin with.
Today, that happens two ways: a code bound to a terminology-server-
dependent system (LOINC, SNOMED, most `urn:oid:`-named systems) that
this workspace's offline validator has no way to check without a
terminology server; or a defective conformance profile — a bundle
whose own `PROFILE.xml` references a value set the engine can't
resolve. Either way, the honest answer is "I don't know," not "yes"
and not "no."

**Witnessed: a profile-tier run that comes back no-verdict**, the
committed CDC COVID19_ELR try-it bundle against its own real, if
imperfect, fixture message:

```bash
bin/ehrt gate v2-nist \
  test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt \
  --profile test-fixtures/v2-nist/COVID19_ELR-v2.3.1 --pretty
```

```
no-verdict  test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt  (473 findings)

totals: pass=0, rejected=0, indeterminate=0, no-verdict=1
by-code: value-set/VS Not Found=28, structure/O-Usage=109, structure/Usage=103, structure/Length Spec Error=221, structure/Dynamic Mapping Match=8, content/Constraint Success=4
```

This particular bundle's own `PROFILE.xml` references value sets the
NIST engine can't resolve — a defect in the profile, not in the
message — so the honest verdict is `:no-verdict`/`:profile-spec-error`,
carried in the report's own `:cause` field. `gate v2` gives you no such
signal at all; profiles are entirely outside its tier. A workflow that
wants a plain exit-code decision anyway has that option explicitly —
`--treat-no-verdict-as pass|rejected` — but the CLI's own default exit
code for a no-verdict aggregate (`3`, distinct from both `0` and `1`)
exists precisely so nothing inherits either polarity by accident.
[`cli.md`](../cli.md#exit-codes) is the full exit-code contract.

**Witnessed: base-structural HL7 v2, before and after a break** — the
same inject-expect loop Chapter 6 taught, run against `gate v2` this
time:

```bash
bin/ehrt gate v2 test-fixtures/v2/adt-a01-admit.hl7 --report out/calibration/before.edn
bin/ehrt corpus mutate --path test-fixtures/v2/adt-a01-admit.hl7 \
  --operator-id blank-required-field --locator-path MSH-9 \
  --out-dir out/calibration/blank-required-field
bin/ehrt gate v2 out/calibration/blank-required-field --report out/calibration/after.edn
```

```
before: {:pass 1, :rejected 0, :indeterminate 0, :no-verdict 0}, by-code {}
after:  {:pass 0, :rejected 1, :indeterminate 0, :no-verdict 0}, by-code {"hl7-exception" 1}
```

Blanking MSH-9, the message type, leaves HAPI unable to determine which
structure to parse the message into at all — a message-structure
resolution failure, `hl7-exception`, at the earliest possible point.

## The verdict is a dominance order, not a coin flip

A file can carry many findings at once. Its single reported verdict is
the *worst* one among them: `:rejected` dominates `:no-verdict`
dominates `:pass`, and a file with zero findings is `:pass` by
definition. This matters in practice on a real, profile-stamped corpus
— such a file can easily carry hundreds of terminology-suppressed
findings alongside one genuine, injected defect, and the aggregate
verdict has to pick one answer. The ranking says a confirmed rejection
still wins over "couldn't fully check everything," which in turn still
counts for more than a clean pass.

## Calibration: what each tier actually catches

Reading the three gates' own descriptions above tells you what each
one is *supposed* to check. Whether it actually convicts a specific
defect at a specific locator is a measured question, the same one
Chapter 6 raised about the operator catalog — [`judge-calibration.md`](../judge-calibration.md)
is the full, honest, per-operator answer for both formats, including
the defects that were probed and found undetectable at a given tier
rather than silently assumed caught. It's also where `--baseline`
mode lives: gating a real-world corpus straight against `gate fhir`
returns nearly every file rejected, most of it profile noise the file
always carried — `--baseline` answers the narrower, more useful
question of whether gating found anything *new*.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt gate v2-nist ... --profile ... --pretty` and its no-verdict output | `docs/use-cases/profile-tier-hl7v2-conformance-gating.md`; witnessed this session (`--pretty` totals/by-code, and the `:cause` field via `--json`) |
| `bin/ehrt gate v2 test-fixtures/v2/adt-a01-admit.hl7 --report ...` / mutate `blank-required-field` / `gate v2` after, before/after totals | `docs/use-cases/judge-tier-calibration-studies.md`; witnessed this session, byte-identical to the use case's own stated result |
