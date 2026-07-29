# Output formats

This page is for the reader on the *other* end of the pipeline: you get
a `report.edn`, a `manifest.edn`, or a directory of lineage records from
someone who ran these tools, and you have to load them into Python, a
warehouse, or a dashboard. You don't need to run the CLI, and nothing
here assumes you can read Clojure.

Everything the tools emit is **EDN**, and every command also accepts
`--json` to project the same data to JSON. EDN is the canonical form;
JSON is a projection over it, not a second code path. If you are reading
from Python, `--json` is the supported route — see
[Reading these from Python](#reading-these-from-python) below.

Three sources of output, and they are not the same thing:

| Where | What you get |
|---|---|
| **stdout** | the full result envelope — `:status`, an optional `:category`, and `:payload` |
| **`--report <path>`** | the report **alone**, unwrapped, always EDN — even when `--json` was also passed |
| **files on disk** | `manifest.edn` beside a generated corpus; `lineage/*.lineage.edn` beside mutants; `operation-manifest.edn` beside a mutant batch |

That first distinction catches people: `ehr gate v2 ... --report r.edn --json`
writes EDN to `r.edn` and prints JSON to stdout, and the two are *not*
the same shape — the file has no `status` wrapper. To get JSON on disk,
redirect stdout.

See [cli.md](cli.md) for which commands take `--report` and `--json`.

---

## The report

The single most important shape here. Both gates (`ehr gate v2`,
`ehr gate fhir`) and `ehr check` produce it — `ehr check` reuses the
judges' aggregation verbatim rather than defining its own — so one
reader handles all three.

Schema: `ehr-testing-tools.judge.report/Report`.

| Field | Type | Meaning |
|---|---|---|
| `:run` | map | free-form metadata about this run: which judge, what it was pointed at, what it was configured with. Shape differs per command (below). |
| `:totals` | map | file counts per verdict. Always all four keys, zeros included. |
| `:by-code` | map | finding code → how many findings carried it, across every file |
| `:files` | vector | one entry per file judged |

`:totals` is `ehr-testing-tools.judge.report/Totals`: `:pass`,
`:rejected`, `:indeterminate`, `:no-verdict`, each an integer. These
count **files**, not findings.

### A file entry

Schema: `ehr-testing-tools.judge.report/FileEntry`.

| Field | Type | Present | Meaning |
|---|---|---|---|
| `:path` | string | always | the file this entry is about |
| `:verdict` | keyword | always | one of the four verdicts, below |
| `:finding-count` | integer | always | how many findings, for a scan that doesn't walk `:findings` |
| `:findings` | vector | always, today | the findings themselves. Optional in the schema only so that reports captured before the field existed still validate. |
| `:id` | string | optional | carried through when the judge supplied one |
| `:cause` | keyword | **iff** `:verdict` is `:no-verdict` | why the judge couldn't fully apply its criterion |
| `:no-verdict-causes` | map | when any finding carried a cause | cause → count, over every finding in the file |

`:no-verdict-causes` is the one field worth understanding before you
aggregate. A file's `:verdict` is a *projection*: one keyword standing
in for a richer state, and it discards information. A file with both a
genuine violation and a check the judge couldn't complete comes out
`:rejected` — the violation dominates. `:no-verdict-causes` is how that
file still reports its own partiality, independently of which keyword
won. If you are building a coverage metric, read it; if you are building
a pass/fail gate, `:verdict` is enough.

### The four verdicts

Vocabulary: `ehr-testing-tools.judge.finding/Verdict`.

| Verdict | Means |
|---|---|
| `:pass` | checked, and clean |
| `:rejected` | checked, and something is genuinely wrong |
| `:no-verdict` | the judge could not fully *apply* its criterion — e.g. it ran without terminology, offline. Always paired with a `:cause`. |
| `:indeterminate` | **reserved.** Nothing in this repo produces it anymore. It stays in the vocabulary because reports captured before the split still serialize it. |

`:no-verdict` is the arm that surprises people, and it exists on
purpose: "passed" is meant to mean *checked and clean*, not *clean on
what we managed to check*. It gets its own CLI exit code (`3`) for the
same reason — so no workflow silently inherits a policy for it. The
reasoning is [ADR-0010](../notes/ADRs.md); this page does not restate it.

Causes are `ehr-testing-tools.judge.finding/Cause`, a deliberately small
enum. Today it has exactly one member, `:terminology-suppressed`. Expect
it to grow; don't assume the set.

When ordering matters: `:rejected` beats `:no-verdict` beats
`:indeterminate` beats `:pass`, and a file with no findings at all is
`:pass`.

### A finding

Schema: `ehr-testing-tools.judge.finding/Finding` — one shape for every
judge, regardless of format.

| Field | Type | Present | Meaning |
|---|---|---|---|
| `:severity` | keyword | always | `:fatal`, `:error`, `:warning`, or `:information` |
| `:code` | string | always | the engine's own code for this kind of issue; this is what `:by-code` counts |
| `:locator` | map | always | `{:format ... :path ...}` — see [locators.md](locators.md) |
| `:message` | string | always | the engine's own diagnostic text |
| `:engine` | map | always | `{:name ... :version ...}` — which engine said so, at which version |
| `:native-ref` | any | optional | the engine's own raw reference, unnormalized, for auditing |

Two fields on top of that are **format-specific extensions**, added by
the FHIR judge only. Don't expect them from `ehr gate v2` or
`ehr check`:

| Field | Meaning |
|---|---|
| `:disposition` | this one finding's contribution to the verdict: `:pass`, `:rejected`, or `:no-verdict`. Recorded per finding, for auditability. |
| `:cause` | rides alongside `:disposition`, present iff it is `:no-verdict` |

`:disposition` deliberately is not called `:policy`. It records a fact
about the finding — what this issue contributes to the judgment — not a
decision about what to *do* about it; the word `policy` is reserved for
that layer. That distinction is [ADR-0009](../notes/ADRs.md); again, not
restated here.

`:native-ref` differs by engine, because it is the engine's own words:
the v2 judge emits `{:class ... :location ...}` (the exception it
caught), the FHIR judge emits `{:expression [...]}` (the validator's own
path expressions). Treat it as opaque unless you're auditing.

### `:run`, per command

Free-form by design, so it differs:

```clojure
;; ehr gate v2 / ehr gate fhir
{:gate :v2, :path "components/tools/test-fixtures/v2/adt-a01-admit.hl7"}

;; ehr check
{:check {:name "check", :version "v1"},
 :candidate-dir "…/check-cand",
 :expected-dir  "…/check-exp",     ; nil when only per-file assertions ran
 :assertions    [{:kind :matches-expected}],
 :pair-by       :path,
 :canonicalizers []}
```

### A whole small report

Real output, from `ehr gate v2` against one mutated fixture:

```clojure
{:run {:gate :v2, :path "…/v2-mutant"},
 :totals {:pass 0, :rejected 1, :indeterminate 0, :no-verdict 0},
 :by-code {"hl7-exception" 1},
 :files
 [{:path "…/v2-mutant/adt-a01-admit.hl7",
   :verdict :rejected,
   :finding-count 1,
   :findings
   [{:severity :error,
     :code "hl7-exception",
     :locator {:format :v2, :path "MSH"},
     :message "Can't determine message structure from MSH-9: null …",
     :engine {:name "hapi-hl7v2", :version "2.6.0"},
     :native-ref {:class "ca.uhn.hl7v2.HL7Exception", :location nil}}]}]}
```

### Baseline mode changes the payload's shape

If the run passed `--baseline`, the payload is **not** a report. It is a
map of two:

```clojure
{:absolute <Report>, :relative <Report>}
```

`:absolute` is the ordinary report, unchanged — findings are never
hidden. `:relative` recounts each file against the baseline, so only
findings that weren't already there count toward rejection. Two things
to know before consuming `:relative`: its verdicts are always binary
(`:pass` or `:rejected` — never `:no-verdict`, even when the absolute
verdict was), and matching is exact on the `severity`/`code`/
`locator-path` triple, so a finding whose message text drifts still
matches but one whose locator moves does not. See
[judge-calibration.md](judge-calibration.md) for when this mode is the
right tool.

---

## The check report

`ehr check` produces the same `Report` shape as the gates — same
`:totals`, same `:files`, same finding envelope. What differs is the
vocabulary of the codes it emits, and its `:run` map (above).

Codes you'll see from `ehr check`, and nowhere else:

| `:code` | Raised when |
|---|---|
| `content-mismatch` | a paired candidate/expected file differs after canonicalization |
| `missing-file` | present in the expected corpus, absent from the candidate |
| `extra-file` | present in the candidate, absent from the expected corpus |
| `unparseable-content` | canonicalization needed to parse a file and couldn't |
| `unparseable-datum` | a per-file assertion needed to parse a file and couldn't |
| `absent` / `present` | a `:present` / `:absent` assertion did not hold |
| `value-mismatch` | a `:value` assertion found a different value, or no value |
| `count-mismatch` | a `:count` assertion's comparison failed |
| `unknown-schema` / `schema-invalid` | a `:schema` assertion named an unregistered schema, or the datum failed it |

Every verdict `ehr check` produces is binary, `:pass` or `:rejected`.
Its criterion is always fully applicable — an expected corpus or an
explicit assertion either matches or it doesn't — so there is no
partiality for `:no-verdict` to name. `:totals` still carries all four
keys; the last two are always zero.

One real entry, from a candidate that differed from its expected file:

```clojure
{:path "patient.json",
 :verdict :rejected,
 :finding-count 1,
 :findings [{:severity :error,
             :code "value-mismatch",
             :locator {:format :fhir, :path "gender"},
             :message "expected \"female\" at gender, found \"male\"",
             :engine {:name "check", :version "v1"}}]}
```

Note `:engine` here names the check vocabulary's own version (`v1`),
not an external engine — the same slot, a different kind of occupant.

---

## The corpus manifest

Written as `manifest.edn` in a generated corpus's `--out-dir`. It is
the provenance record: everything that was pinned when this corpus was
made, so someone else can make the same one.

Schema: `ehr-testing-tools.corpus.manifest/ManifestV1_1` — the version
`ehr corpus generate` produces today.

| Field | Type | Meaning |
|---|---|---|
| `:schema-version` | string `"1.1"` | see the versioning note below |
| `:stage` | keyword | which pipeline stage produced this manifest (`:generate`) |
| `:generator` | map | `{:name :version :sha256}` — the engine artifact, by content hash |
| `:runtime` | map, optional | the JVM artifact that ran it, same shape. **Absent** when `--java-bin` was passed explicitly, because fabricating an artifact record for an unmanaged JVM would be a lie. |
| `:seeds` | map | keyword → integer, e.g. `{:master 100 :clinician 555}` |
| `:engine-params` | map | free-form engine parameters, e.g. `{:reference-date "20260101"}` |
| `:config` | map | `{:path :sha256}` — the config file, by content hash |
| `:invocation` | map | the full subprocess record: command, args, exit code, timing, stdout/stderr paths and their hashes |
| `:canonicalizers-applied` | vector | ordered `[id version]` pairs; `[]` when none |
| `:environment` | map | `{:locale :timezone :jvm-version}` |

**On versions.** `:schema-version` is the *string* `"1.1"`, not a
number — deliberately, because the change from v1 was additive
restructuring within the same lineage rather than a new epoch. Two older
schemas exist and are frozen, not migrated: `ManifestV0` and
`ManifestV1`, both with an *integer* `:schema-version` (`0` and `1`).
Nothing regenerates or reinterprets an old manifest, so a corpus
generated last year still reads as what it was. **Branch on the type as
well as the value** — a consumer that assumes `:schema-version` is a
number will break on `"1.1"`, and one that assumes it's a string will
break on old records.

The two seed fields are worth calling out, because they're the reason a
corpus is reproducible at all: `:master` seeds patient generation and
`:clinician` seeds practitioner generation. Both are pinned because
pinning only the first still leaves output non-deterministic.
`:engine-params`' `:reference-date` is pinned for the same reason —
without it the generator works relative to wall-clock *now*.

A real manifest (paths and hashes elided):

```clojure
{:schema-version "1.1",
 :stage :generate,
 :generator {:name "synthea", :version "4.0.0", :sha256 "ed43c20a…"},
 :runtime {:name "temurin-jdk", :version "21.0.12+8", :sha256 "e4446ff0…"},
 :seeds {:master 100, :clinician 555},
 :engine-params {:reference-date "20260101"},
 :config {:path "config/synthea/synthea.properties", :sha256 "ead0388b…"},
 :invocation {:command "…/bin/java", :args ["…"], :exit-code 0,
              :started-at "2026-07-24T12:19:40.710909Z", :duration-ms 40696,
              :stdout-path "…", :stdout-sha256 "c264eaf5…",
              :stderr-path "…", :stderr-sha256 "65928446…",
              :dir nil, :env {}},
 :canonicalizers-applied [],
 :environment {:locale "en-US", :timezone "UTC", :jvm-version "21.0.12"}}
```

---

## The lineage record

Written one per mutant, as `<output-dir>/lineage/<filename>.lineage.edn`
— a subdirectory rather than sidecars interleaved with the data, so you
can glob the data and the provenance separately.

Schema: `ehr-testing-tools.lineage/LineageRecord`.

| Field | Type | Meaning |
|---|---|---|
| `:id` | sha256 hex | this record's own identity |
| `:parent` | sha256 hex | content hash of the input datum |
| `:produced` | sha256 hex | content hash of the output datum |
| `:stage` | keyword | which stage produced it (`:mutate`) |
| `:transformation` | map | `{:operator {:id :version} :locator {...} :contract {...}}` — exactly which operator, at which locator, under which contract |

Two properties make these useful as a graph rather than as notes:

- **`:id` is the content hash of the record's own remaining fields**,
  computed rather than supplied. A record cannot be inconsistent with
  its claimed identity, and you can verify one by rehashing it.
- **Records are append-only.** There is no amend operation. A correction
  is a *new* record pointing at whatever it corrects. So joining
  `:parent` to `:produced` across a directory gives you the real
  derivation graph, with no in-place edits to reconcile.

```clojure
{:id       "539b14ab…",
 :parent   "7147d7db…",
 :produced "79b9de10…",
 :stage :mutate,
 :transformation
 {:operator {:id :blank-required-field, :version "1"},
  :locator  {:format :v2, :path "MSH-9"},
  :contract {:type :violates,
             :target "blanks the field at the locator, violating …"}}}
```

The `:contract` map is copied from the operator's registry entry — see
[operators.md](operators.md) for the full catalog.

---

## The operation manifest

Written as `operation-manifest.edn` in a mutant batch's own `--out-dir`,
alongside the mutants and their `lineage/` sidecars — written last, after
every item in the batch. It is a *different* provenance record from the
corpus manifest above, on purpose (D-d, `docs/source-sink-design.md` Part
III.5; `ADR-0020`): the corpus manifest states *engine* provenance (which
artifact ran, under which config); this one states *transformation*
lineage (these input hashes, this operator, these output hashes) for an
in-process write that never ran an external engine at all.

Schema: `ehr-testing-tools.corpus.operation-manifest/OperationManifestV1`.

| Field | Type | Meaning |
|---|---|---|
| `:manifest-kind` | `:operation` | discriminates this file from a corpus manifest at a glance |
| `:schema-version` | `1` | this schema's own version, unrelated to the corpus manifest's `"1.1"` |
| `:producer` | map | `{:name :identity :git}` — this repo's own honest identity (the same identity `ehr version` reports). No `:sha256`: an absent field is honest, a fabricated one is not |
| `:operation` | map | `{:kind :operator-id :operator-version :locator}` — what was done. `:kind` is `:mutate` today, the one operation that writes a batch this way |
| `:written-at` | string | a record-keeping date — when the manifest was written, not a generation input |
| `:format` / `:framing` | keyword | read straight off the sink that wrote the batch |
| `:items` | vector of maps | one per file in the batch: `{:name :sha256 :input-hash}` — `:input-hash` (optional) is the content hash of whatever that item was derived from, present only where the write actually knew it |

A real manifest (from the "Generate controlled-fault data" strip above):

```clojure
{:manifest-kind :operation,
 :schema-version 1,
 :producer {:name "ehr-testing-tools", :identity "pre-release", :git "318953e"},
 :operation {:kind :mutate,
             :operator-id :remove-required-element,
             :operator-version "1",
             :locator {:format :fhir, :path "entry[0].resource.gender"}},
 :written-at "2026-07-28",
 :format :fhir-json,
 :framing :file-per-item,
 :items [{:name "Brandon214_Rosenbaum794_….json",
          :sha256 "95070c9d…",
          :input-hash "1284fd04…"}]}
```

`:items[].sha256` and `:input-hash` here are exactly the same mutant's
own lineage record's `:produced` and `:parent` — no separate hashing, no
chance of the two disagreeing. `ehr corpus intake` recognizes this file
automatically (directory-scoped, the same mechanism the corpus manifest
above uses) and attaches `:operation-provenance` to every catalog entry
in that directory; a directory carrying *both* a corpus manifest and an
operation manifest is rejected `:ambiguous-sidecars` — never resolved by
picking one.

---

## The `--json` projection

`--json` projects the whole result envelope, not just the payload. The
mapping, stated from real captured output rather than assumed:

| EDN | JSON |
|---|---|
| a keyword map key, `:finding-count` | an object key, `"finding-count"` — the colon is dropped, hyphens are kept |
| a keyword *value*, `:rejected` | a string, `"rejected"` |
| `nil` | `null` |
| an integer | a number |
| an empty vector / map | `[]` / `{}` |
| a string containing `/` | the `/` is escaped as `\/` — valid JSON, and any parser decodes it back to `/` |

Keyword keys and keyword values both become plain strings, which means
JSON loses the distinction between the string `"pass"` and the keyword
`:pass`. Nothing in these shapes relies on that distinction, so the
projection is lossless in practice — but it is why EDN, not JSON, is the
canonical form.

The same v2 gate result, projected:

```json
{"status": "rejected",
 "category": "gate-rejected",
 "payload": {
   "run": {"gate": "v2", "path": "target\/doc3-evidence\/v2-mutant"},
   "totals": {"pass": 0, "rejected": 1, "indeterminate": 0, "no-verdict": 0},
   "by-code": {"hl7-exception": 1},
   "files": [{"path": "…", "verdict": "rejected", "finding-count": 1,
              "findings": [{"severity": "error", "code": "hl7-exception",
                            "locator": {"format": "v2", "path": "MSH"},
                            "message": "…",
                            "engine": {"name": "hapi-hl7v2", "version": "2.6.0"},
                            "native-ref": {"class": "ca.uhn.hl7v2.HL7Exception",
                                           "location": null}}]}]}}
```

`:status` is `"ok"`, `"rejected"`, or `"error"`; `:category` appears
only on the latter two. The exit code is the more reliable thing to
branch on in a script — see [cli.md](cli.md#exit-codes).

One exception, so it doesn't surprise you: `ehr help` and `--help` print
plain text, not EDN or JSON. They are for a human or an assistant at a
shell, not for a pipeline.

### Reading these from Python

`--json` plus `json.load` is the supported path, and it is the one to
take. Every shape on this page projects to ordinary JSON objects,
arrays, strings, numbers, and `null` — there is nothing exotic to
special-case.

Reading the EDN directly is possible but is not the supported path: the
Python standard library has no EDN parser, so you'd be taking a
third-party dependency to read a format whose JSON projection is
already produced for you. The one place this bites is `--report`, which
always writes EDN. If you want a report file in JSON, redirect stdout
from a `--json` run instead of reading the `--report` file — remembering
that stdout carries the `status`/`payload` envelope around the report,
and the `--report` file does not.

For reading verdicts in bulk — what a `:no-verdict` rate actually means,
which findings are worth alerting on — see
[judge-calibration.md](judge-calibration.md).

---

## Where this comes from

| Shape | Schema | Verified against |
|---|---|---|
| Report, totals, file entry | `ehr-testing-tools.judge.report/Report` | live `ehr gate v2` runs, passing and rejected, 2026-07-25 |
| Verdicts, causes, findings | `ehr-testing-tools.judge.finding/Finding`, `/Verdict`, `/Cause` | the same runs |
| FHIR findings' `:disposition` / `:cause` | `ehr-testing-tools.judge.fhir/interpret` | a live `ehr gate fhir` run against a real mutant bundle, 2026-07-25 — 6554 findings, all three dispositions present |
| Check report and its codes | `ehr-testing-tools.check` | live `ehr check` runs in both golden-equivalence and per-file-assertion modes, 2026-07-25 |
| Corpus manifest | `ehr-testing-tools.corpus.manifest/ManifestV1_1` | a real generated corpus's `manifest.edn` |
| Lineage record | `ehr-testing-tools.lineage/LineageRecord` | a real mutant's lineage sidecar |
| Operation manifest | `ehr-testing-tools.corpus.operation-manifest/OperationManifestV1` | a real `corpus mutate` batch's `operation-manifest.edn`, 2026-07-28 |
| The `--json` mapping | — | the captured JSON output of the runs above, not inferred from the projection's source |

Semantics cited, never restated here: [ADR-0009](../notes/ADRs.md)
(judge vs. gate, and why the per-finding field is `:disposition` rather
than `:policy`) and [ADR-0010](../notes/ADRs.md) (the `:no-verdict` arm,
its `:cause` channel, and its own exit code).
