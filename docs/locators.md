# Locators

A **locator** names one exact spot inside one exact file. It is what
`ehrt corpus mutate --locator-path ...` takes, and what a finding in a
gate report points back at. There are two grammars, one per format —
FHIR paths into parsed JSON, and HL7 v2 segment/field addresses — and
they have nothing in common but the job.

Which grammar applies is decided by the operator, not by you: every
operator declares its `:format`, and `ehrt corpus mutate` parses
`--locator-path` under that format's grammar. See
[operators.md](operators.md) for which operators need a locator (today:
all of them) and [cli.md](cli.md#ehrt-corpus-mutate) or
`ehrt help corpus` for where the flag goes.

This page describes what the parsers in
[`ehrt.kernel.locator`](../components/kernel/src/ehrt/kernel/locator.clj)
and [`ehrt.tools.corpus.er7`](../components/tools/src/ehrt/tools/corpus/er7.clj)
actually accept — those files are authoritative, and every example
below is pinned to them by
[`ehrt.tools.locators-doc-test`](../components/tools/test/ehrt/tools/locators_doc_test.clj)
(kept in `components/tools/test/` rather than kernel's own, since it
also pins `ehrt.tools.corpus.er7` — ADR-0008's own deviation record),
which runs in the ordinary `make test`. If a grammar changes and this
page doesn't, that test fails.

---

## FHIR locators

### The grammar

A FHIR locator is a **data path**: dot-separated segments, each a field
name with an optional bracketed array index.

```
path    := segment ( "." segment )*
segment := name ( "[" digits "]" )?
name    := ( letter | "_" ) ( letter | digit | "_" )*
```

Indices are **zero-based**, like the JSON arrays they index. At most one
index per segment — `entry[0][1]` is not a path. A name may start with
an underscore; whether a given file actually has such a key is a
question about that file, not about the grammar.

**The grammar is fully anchored**, exactly like the v2 grammar below: a
separator with nothing after it (`entry[0].resource.`) is a parse error,
not a path quietly read without its trailing dot. *(Tightened by LOC-1,
2026-07-25 — until then the trailing dot was silently dropped by the
path split before the grammar saw it, and the two grammars disagreed
about it. They no longer do.)*

This is deliberately an operational *subset* of FHIRPath, not an
implementation of it: no wildcards, no `where(...)` filters, no
functions, no type operators. It exists to name one exact spot in one
exact datum, which is all mutation needs.

### What a path is resolved against

The whole parsed file, from its root — not from a resource. A generated
Synthea corpus is one **Bundle** per file, so a path to something inside
a patient resource starts by naming the bundle entry it lives in:

```
entry[0].resource.gender
│       │  │        └── the element you're naming
│       │  └── each Bundle entry wraps its resource under "resource"
│       └── zero-based index into the Bundle's "entry" array
└── the Bundle's own "entry" field, at the root of the file
```

A path resolves to a value the way `get-in` would: each segment steps
one level down. Nothing is searched for — if `entry[0]` isn't the entry
you meant, the locator names a different spot, and the mutation lands
there.

### Paths that parse

| Locator | Steps to |
|---|---|
| `gender` | the root object's `gender` |
| `resourceType` | the root object's `resourceType` |
| `entry[0].resource.gender` | first entry's resource's `gender` |
| `entry[0].resource.birthDate` | first entry's resource's `birthDate` |
| `entry[0].resource.active` | first entry's resource's `active` |
| `entry[0].resource.name[0].given[0]` | first given name of the first name |
| `entry[2].resource.identifier[1].type.coding[0].code` | a code deep inside the third entry |
| `_birthDate` | the root object's `_birthDate` |

### Strings that are not paths

Each of these is refused with `:invalid-fhir-path`, before any file is
touched:

| Not a path | Why |
|---|---|
| `` (empty) | a locator must name something |
| `0entry` | a name can't start with a digit |
| `entry[x]` | an index must be digits |
| `entry[-1]` | no negative indices; indices are zero-based, not signed |
| `entry.0.resource` | an index goes in brackets, not between dots |
| `entry[0]..resource` | empty segment |
| `entry[0]resource` | brackets must end the segment |
| `entry[0][1]` | at most one index per segment |
| `entry[0].resource.` | anchored: nothing after the separator |

---

## HL7 v2 locators

### The grammar

A v2 locator is HL7's own segment/field addressing, in six forms from
coarsest to finest:

```
locator := segment ( "[" repeat "]" )?
           ( "-" field ( "[" repeat "]" )?
             ( "." component ( "." subcomponent )? )? )?

segment := uppercase-letter ( uppercase-letter | digit ) ( uppercase-letter | digit )
repeat, field, component, subcomponent := positive integer
```

Three rules follow from that, and they catch most mistakes:

- **Segment names are exactly three characters**, uppercase, starting
  with a letter. `PID` and `ZZ1` are segment names; `pid`, `P1`, and
  `PIDX` are not.
- **Every number is 1-based and positive.** HL7 numbers fields and
  components from 1, so `0` and negatives aren't expressible in the
  grammar at all rather than accepted and then rejected somewhere else.
- **The grammar is fully anchored.** A separator with nothing after it
  (`PID-`, `PID-3.`) fails to match, rather than parsing a partial path.

### Locators that parse

| Locator | Names |
|---|---|
| `PID` | the segment as a whole |
| `MSH` | the header segment as a whole |
| `OBX[2]` | the second `OBX` segment in the message |
| `PID-3` | field 3 of the first `PID` |
| `MSH-2` | field 2 of `MSH` (the encoding characters) |
| `MSH-7` | field 7 of `MSH` |
| `MSH-9` | field 9 of `MSH` (the message type) |
| `PID-3[2]` | the second repetition of `PID-3` |
| `PID-5.1` | component 1 of field 5 |
| `PID-5.1.2` | subcomponent 2 of component 1 of field 5 |
| `OBX[2]-5.1` | component 1 of field 5 of the second `OBX` |
| `ZZ1-1` | field 1 of a `Z`-segment |

Segment-level locators exist because some defects target the segment as
a whole — corrupting its name, dropping it — rather than one of its
fields.

### Strings that are not locators

Each of these is refused with `:invalid-v2-path`:

| Not a locator | Why |
|---|---|
| `` (empty) | a locator must name something |
| `pid` | segment names are uppercase |
| `P1` | segment names are exactly three characters |
| `PIDX` | likewise — four is too many |
| `1ID` | a segment name starts with a letter |
| `PID.3` | a field is introduced by `-`, not `.` |
| `PID-` | anchored: nothing after the separator |
| `PID-3.` | likewise |
| `PID-0` | fields are numbered from 1 |
| `PID[0]` | repetitions are numbered from 1 |
| `PID-3[0]` | likewise |
| `PID-3.1.2.4` | there is no level below subcomponent |
| `MSH-1` | `MSH-1` is the field separator itself — see below |

### Where a locator lands: the MSH off-by-one

Every segment in a message is a run of fields separated by the field
separator — canonically `|`. For every segment except `MSH`, field *N*
sits at position *N*, counting the segment name itself as position 0.

`MSH` is off by one, and the reason is that `MSH-1` **is the field
separator character**. A message declares its own delimiters in its
header: the character right after the literal `MSH` is the field
separator, and the four characters after that (`^~\&` canonically) are
the component, repetition, escape, and subcomponent separators. So
`MSH-1` is consumed by the very split that produces the fields — it
never appears as a field of its own — and `MSH-2`, the encoding
characters, is therefore the *first* thing after the segment name.

Concretely, for `MSH`, field *N* (for *N* ≥ 2) lands at position
*N* − 1. Walk it against a real message:

```
MSH|^~\&|SND|FAC|RCV|FAC|20260101||ADT^A01^ADT_A01|MSG1|P|2.4
PID|1||12345||Doe^John||19800101
```

| Locator | Lands on |
|---|---|
| `MSH-2` | `^~\&` |
| `MSH-7` | `20260101` |
| `MSH-9` | `ADT^A01^ADT_A01` |
| `PID-5` | `Doe^John` |
| `PID-7` | `19800101` |

**You can't write `MSH-1`.** There is no field there to name, so the
parser refuses it — with `:invalid-v2-path`, before any file is touched,
and with a hint that explains itself:

> MSH-1 is the field separator character itself (the character right after the literal "MSH"), not an addressable field: the split that produces a segment's fields consumes it, so it holds no position of its own. The encoding characters are MSH-2.

So: `MSH-2` when you mean the encoding characters, and nothing at all
when you mean the field separator — a delimiter is not addressable.
Every other `MSH` field is an ordinary locator, and field 1 is ordinary
data in every *other* segment: `PID-1` and `ZZ1-1` parse fine.

*(Tightened by LOC-1, 2026-07-25. Until then `MSH-1` parsed like any
other field locator and then, sitting below the *N* ≥ 2 shift, resolved
onto `MSH-2`'s position — silently addressing the encoding characters,
which is almost certainly not what anyone meant. The off-by-one above is
unchanged; what changed is that no locator can walk into it.)*

### Components and subcomponents parse, but resolve at the field

The v2 mutation substrate is **field-granular**: a field's components,
repetitions, and subcomponents are carried verbatim inside that field's
string value and are not decomposed further. A locator naming a
component or subcomponent parses fine and then resolves at the field it
names, ignoring the finer part. Against the message above, `PID-5.1`
and `PID-5` both land on the whole string `Doe^John`.

The grammar supports the finer forms because it is the more stable of
the two layers, and future operators will need them. Until an operator
does, treat `PID-5.1` as a more precise way of writing `PID-5` — not as
a different target.

### Parsing and resolving are two different failures

A locator can be perfectly well-formed and still name nothing in a
particular message. `PID-99` and `NK1-2` both parse; against the
two-segment message above, neither resolves — the field is past the end
of the segment, and the segment isn't in the message at all.

`ehrt corpus mutate` checks that a locator resolves *before* it invokes
any operator, so a locator that doesn't resolve stops the batch with an
error rather than silently mutating nothing. The two failures reach you
differently: a bad grammar is `:invalid-v2-path` (or
`:invalid-fhir-path`), a good locator pointing at nothing is a
resolution failure on a specific file.

---

## Where this comes from

| Claim | Authority |
|---|---|
| FHIR path grammar, and what it rejects | `ehrt.kernel.locator/fhir-data-path` |
| v2 path grammar, and what it rejects | `ehrt.kernel.locator/v2-data-path` |
| The `MSH` off-by-one, and field positions generally | `ehrt.tools.corpus.er7/field-index` |
| Segment occurrence and resolution against a message | `ehrt.tools.corpus.er7/resolve-locator` |
| Field granularity of the v2 substrate | `ehrt.tools.corpus.er7` namespace docstring |
| Every example on this page | `test/ehr_testing_tools/locators_doc_test.clj` |

Related: [operators.md](operators.md) (which operator to pair a locator
with), [cli.md](cli.md#ehrt-corpus-mutate) (the flags around it),
[formats.md](formats.md) (how a locator comes back to you in a report's
findings).
