# Chapter 6 — Breaking data on purpose

Every chapter so far generated data that's supposed to be right —
conforming, reproducible, realistic in the ways Chapters 3 through 5
taught. This chapter breaks it, deliberately, and keeps a receipt for
every break. That's the whole idea: a validation suite that never sees
a bad file is exactly as informative as one that never runs at all.
`ehrt corpus mutate` exists to supply the bad files, each one traceable
back to the exact defect planted in it.

## Named injury, not random corruption

Mutation here isn't fuzzing. You pick one registered operator, one
locator naming exactly where in the file it acts, and `ehrt corpus
mutate` edits precisely that — nothing else in the file moves. The
output is never handed to you bare: a lineage record ships alongside
every mutant, naming the operator, the locator, and the base-spec
constraint the result now violates. [`docs/formats.md`](../formats.md#the-lineage-record)
is the field-by-field reference; this chapter is about what the loop
is *for*.

**One canonical file, one deliberate break** — copied verbatim from
`README.md`'s own "What you get" section:

```bash
bin/ehrt corpus mutate test-fixtures/fhir/storefront-patient.json \
  --operator-id remove-required-element \
  --locator-path entry[0].resource.resourceType \
  --out-dir out/demo-mutants
```

The lineage record this run writes, witnessed this session:

```
{:parent "3f5f4d3f085e8c9c8697a3f01326dbc535a6de252ee936228f9a8c46b69a0954",
 :stage :mutate,
 :transformation
 {:operator {:id :remove-required-element, :version "1"},
  :locator {:format :fhir, :path "entry[0].resource.resourceType"},
  :contract
  {:type :violates,
   :target "removes the element at the locator path, violating that element's minimum-cardinality constraint (Element.min >= 1 per the base FHIR StructureDefinition for whichever element the locator names)"}},
 :produced "4c9d5b987196a16b392b49f3ce0a7a14e475155e0b282b8d92040f7d87f9fce4",
 :id "1acdf4a273382b57ee93a258248d56d4837d3bb2841121850c4d3b9a3994dc38"}
```

`:parent` names the exact canonical file this mutant descends from by
content hash, `:transformation` names the operator and locator you
chose, and `:contract` states — in words, before you ever gate anything
— which base-spec rule the edit now breaks. A batch of mutants also
gets a self-description one level up, the same producer/per-item shape
`ehrt corpus intake` recognizes automatically:

```
{:manifest-kind :operation, :schema-version 1,
 :operation
 {:kind :mutate, :operator-id :remove-required-element, :operator-version "1",
  :locator {:format :fhir, :path "entry[0].resource.resourceType"}},
 :items
 [{:name "storefront-patient.json",
   :sha256 "4c9d5b987196a16b392b49f3ce0a7a14e475155e0b282b8d92040f7d87f9fce4",
   :input-hash "3f5f4d3f085e8c9c8697a3f01326dbc535a6de252ee936228f9a8c46b69a0954"}]}
```

Both records exist so that six months from now, looking at a strange
file in some `out/` directory, you can answer "what's wrong with this,
on purpose?" without re-deriving it from the bytes.

## Choosing an operator, not enumerating them

[`operators.md`](../operators.md) is the full catalog — every
registered operator, what it edits, and the constraint it violates.
This chapter won't repeat that table; it's a reference to bookmark,
not to read start to finish. What's worth teaching instead is how to
pick a row out of it.

Start from the question you actually have, not the list of edits on
offer. You don't want "an operator that changes a date field" — you
want to know whether your own pipeline rejects a message when a
required timestamp is unparseable. That question names the *contract*
you want broken, and every operator's own **Contract** line in
`operators.md` states its contract in exactly those terms — "violates
that field's required primitive data type," "violates the minimum-
cardinality constraint," "violates HL7 v2's own encoding-characters
well-formedness rule." Read down the Contract column until one matches
the claim you actually want tested, then check its **What it does**
line for how the edit gets there. The `remove-required-element` mutant
above wasn't chosen because deleting a field is easy — it was chosen
because "a genuinely required element is missing" is the specific
claim `resourceType` lets you test cleanly (`Element.min >= 1` in the
base FHIR spec, no profile involved).

## Inject-X, expect-X

Here's the idea the rest of this chapter is building toward, stated
plainly: **the defect class you inject should surface as the matching
finding class when you gate the result.** Break a required-cardinality
rule, expect a required-cardinality finding. If the gate says something
else, or says nothing, that's not merely a curiosity — it's a
measurement of what the validator downstream actually checks, which is
the whole reason to inject a *named* defect instead of a random one.

<img src="assets/inject-expect-loop.svg" alt="A defect class injected by Mutate surfaces as the matching finding class at Gate -- the gate is the oracle" width="700" />

Gate the mutant built above against the official FHIR validator:

```bash
bin/ehrt gate fhir out/demo-mutants
```

```clojure
{:status :rejected, :category :gate-rejected,
 :payload
 {:run {:gate :fhir, :path "out/demo-mutants"},
  :totals {:pass 0, :rejected 1, :indeterminate 0, :no-verdict 0},
  :by-code {"invalid" 1, "invariant" 1},
  :files
  [{:path "out/demo-mutants/storefront-patient.json",
    :verdict :rejected,
    :finding-count 2,
    :findings
    [{:severity :fatal, :code "invalid",
      :locator {:format :fhir, :path "Bundle.entry[0].resource"},
      :message "Unable to find resourceType property", ...}
     {:severity :error, :code "invariant",
      :locator {:format :fhir, :path "Bundle.entry[0]"},
      :message "Constraint failed: bdl-5: 'must be a resource unless there's a request or response'", ...}]}]}}
```

Two findings, and both trace back to the one deliberate edit: deleting
`resourceType` is directly "unable to find resourceType property," and
losing the resource's own type is exactly what makes the bundle entry
fail its own `bdl-5` "must be a resource" rule downstream. Nothing here
was inherited from elsewhere in the file — this fixture gates clean
before the mutation (one `:warning`-severity best-practice note, not a
rejection), so both new findings are earned by the mutation alone. That
flip, `:pass` to `:rejected`, driven by nothing but the one edit named
in the lineage record above, is the loop this chapter teaches: inject
a named defect, expect its matching finding at the gate.

## The catalog doesn't promise conviction

Reading `operators.md`'s Contract column tells you what an operator
*claims* to break. It does not tell you whether a given judge tier
actually notices — that's a measured property of the judge, not a
property of the operator, and the two can come apart in real ways
(a lexical-format check that only fires on a non-empty value, a
tier with no terminology server at all). [`judge-calibration.md`](../judge-calibration.md)
is where every operator in the catalog was actually run against a real
judge and the result recorded, including the candidates that were
probed and dropped because they didn't convict at any tier. Chapter 7
picks up from exactly this point — what each gate checks, and what a
`:pass` from one honestly does and doesn't tell you.

## Testing your own validation, not just this workspace's

Everything above uses this repo's own gates to close the loop, because
they're the honest, already-measured example to show. But the same
mutant corpus is exactly what you'd feed *your own* validation logic to
ask the same question about it: of the defects this catalog injects,
how many does your own code actually catch? [Mutation-adequacy of your
own checks](../use-cases/mutation-adequacy-of-your-own-checks.md) names
that use case directly — this repo drives the mutation half, the
scoring loop around your own validator is yours to write. A validation
suite that never rejects a mutant is exactly as informative as one that
always does.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt corpus mutate test-fixtures/fhir/storefront-patient.json ...` | `README.md`, "What you get" |
| The lineage record (`:parent`/`:transformation`/`:contract`/`:produced`/`:id`) | witnessed this session, fresh regeneration against `test-fixtures/fhir/storefront-patient.json` |
| The operation manifest (`:manifest-kind`/`:operation`/`:items`) | witnessed this session, same run |
| `bin/ehrt gate fhir out/demo-mutants` and its rejected report | `README.md`, "What you get"; re-derived byte-identical by fresh regeneration this session |
