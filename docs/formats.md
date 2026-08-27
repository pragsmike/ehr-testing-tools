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

That first distinction catches people: `ehrt gate v2 ... --report r.edn --json`
writes EDN to `r.edn` and prints JSON to stdout, and the two are *not*
the same shape — the file has no `status` wrapper. To get JSON on disk,
redirect stdout.

One more shape this page carries, and it is not a report: the
**ground-truth event log** — `ehrt sim run --format ground-truth`'s own
output, and the `events.edn` beside a generated sim corpus. **If you came
here to render simulated traffic into a format this project does not
ship, skip straight to [The event log](#the-event-log)**; that section is
the contract you want, and everything between here and it is about
reports.

See [cli.md](cli.md) for which commands take `--report` and `--json`.

---

## The report

The single most important shape here. Both gates (`ehrt gate v2`,
`ehrt gate fhir`) and `ehrt check` produce it — `ehrt check` reuses the
judges' aggregation verbatim rather than defining its own — so one
reader handles all three.

Schema: `ehrt.judge.report/Report`.

| Field | Type | Meaning |
|---|---|---|
| `:run` | map | free-form metadata about this run: which judge, what it was pointed at, what it was configured with. Shape differs per command (below). |
| `:totals` | map | file counts per verdict. Always all four keys, zeros included. |
| `:by-code` | map | finding code → how many findings carried it, across every file |
| `:files` | vector | one entry per file judged |

`:totals` is `ehrt.judge.report/Totals`: `:pass`,
`:rejected`, `:indeterminate`, `:no-verdict`, each an integer. These
count **files**, not findings.

### A file entry

Schema: `ehrt.judge.report/FileEntry`.

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

Vocabulary: `ehrt.judge.finding/Verdict`.

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
reasoning is in the design record[^tools-adr-0010]; this page does not restate it.

Causes are `ehrt.judge.finding/Cause`, a deliberately small
enum. Today it has exactly one member, `:terminology-suppressed`. Expect
it to grow; don't assume the set.

When ordering matters: `:rejected` beats `:no-verdict` beats
`:indeterminate` beats `:pass`, and a file with no findings at all is
`:pass`.

### A finding

Schema: `ehrt.judge.finding/Finding` — one shape for every
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
the FHIR judge only. Don't expect them from `ehrt gate v2` or
`ehrt check`:

| Field | Meaning |
|---|---|
| `:disposition` | this one finding's contribution to the verdict: `:pass`, `:rejected`, or `:no-verdict`. Recorded per finding, for auditability. |
| `:cause` | rides alongside `:disposition`, present iff it is `:no-verdict` |

`:disposition` deliberately is not called `:policy`. It records a fact
about the finding — what this issue contributes to the judgment — not a
decision about what to *do* about it; the word `policy` is reserved for
that layer. That distinction is in the design record[^adr-0009]; again, not
restated here.

`:native-ref` differs by engine, because it is the engine's own words:
the v2 judge emits `{:class ... :location ...}` (the exception it
caught), the FHIR judge emits `{:expression [...]}` (the validator's own
path expressions). Treat it as opaque unless you're auditing.

### `:run`, per command

Free-form by design, so it differs:

```clojure
;; ehrt gate v2 / ehrt gate fhir
{:gate :v2, :path "test-fixtures/v2/adt-a01-admit.hl7"}

;; ehrt check
{:check {:name "check", :version "v1"},
 :candidate-dir "…/check-cand",
 :expected-dir  "…/check-exp",     ; nil when only per-file assertions ran
 :assertions    [{:kind :matches-expected}],
 :pair-by       :path,
 :canonicalizers []}
```

### A whole small report

Real output, from `ehrt gate v2` against one mutated fixture:

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

`ehrt check` produces the same `Report` shape as the gates — same
`:totals`, same `:files`, same finding envelope. What differs is the
vocabulary of the codes it emits, and its `:run` map (above).

Codes you'll see from `ehrt check`, and nowhere else:

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

Every verdict `ehrt check` produces is binary, `:pass` or `:rejected`.
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

## The event log

**If you are here to translate simulated traffic into a format we do not
ship, this is the section you want.** The ground-truth event log is the
richest semantic form this project produces — everything else (HL7v2
messages, FHIR bundles, the check report) is a *projection* of it. Writing
your own emitter against the log means you are reading the same primitive
our own emitters read, rather than reverse-engineering ours.

Where you get it:

| Command | What lands |
|---|---|
| `ehrt sim run --seed N --format ground-truth` | the **bare EDN vector**, nothing around it — pipe it straight to a file or to `ehrt sim check` |
| `ehrt sim run --seed N` | the full result envelope: `{:status :ok :payload {:ground-truth [...] :manifest {...} :summary {...}}}` |
| `ehrt corpus generate sim --out-dir D` | `D/events.edn` — the same EDN vector `--format ground-truth` emits |

Two traps, both stated plainly because both were found by running the
commands rather than by reading them:

- **`--format ground-truth --json` emits EDN, not JSON.** The flags read as
  if they compose; they do not, and `--format` wins. If you want JSON, use
  the envelope form (`--json` with no `--format`) and take
  `payload.ground-truth`.
- **`events.edn` and a redirected `--format ground-truth` are not quite
  byte-identical**: the printed form ends with a trailing newline, the file
  does not. The EDN *value* is the same either way, so any reader parses
  them identically — but a checksum comparison across the two will differ by
  that one byte.

### Ordering

The vector is in run order, and **`:t` never decreases within a run**. That
is a real guarantee, not an accident of construction — it is what lets
`ehrt corpus play` pace a log by `:t` alone, and it means you may stream the
vector in order rather than sorting it.

It is a *run*-level property. Concatenating two runs breaks it, and nothing
in an event marks a run boundary — so do not concatenate logs and expect
monotonicity to survive. Note also that `:t` is **seconds elapsed since the
run began**, not a wall-clock instant: the log carries no absolute time at
all. Anchoring to a real date happens at emit time, from `--reference-date`.

### EDN conventions

- **Keywords** are used for the event discriminator, enumerated values, and
  every map key. `:event`, `:role`, `:placement`, `:disposition` are all
  keywords, not strings.
- **Sets** appear in exactly one place: `:merge`'s `:merged-mrns`. Sets are
  unordered; do not depend on iteration order.
- **Maps keyed by data** appear in exactly one place: `:bed-swap`'s `:swap`
  is keyed by patient-id.
- **No instants.** There is no `#inst` anywhere in an event log — the only
  time-like field is `:t`, an integer. Nothing here needs a date parser.
- **Regex patterns**, where the schema constrains a string, are written
  `[:re "…"]` with the pattern as a plain string. The dialect is
  **`java.util.regex`** — if you are validating in another language, that is
  whose flavour of regex you have been handed.

### JSON

JSON is derived from the EDN, by the same projection [described
below](#the-json-projection), and EDN is canonical. The rules that matter
for this shape, all confirmed against real captured output:

| EDN | JSON |
|---|---|
| `:event :admission` | `"event": "admission"` — keyword keys lose the colon, keyword values become strings, hyphens are kept |
| `:t 3600` | `3600` — a number, never a formatted date |
| `#{"MRN000029"}` (a set) | `["MRN000029"]` — an array, in unspecified order |
| `{"PID-000000-…" {…}}` (`:swap`) | an object; the patient-id keys are already strings and survive intact |
| `nil` | `null` |

The lossy step is that JSON cannot tell the keyword `:admission` from the
string `"admission"`. Nothing in this shape depends on that distinction,
which is why the projection is safe in practice — and why EDN, not JSON,
is the form the contract is stated in.

### Stability

The event log is a **public, versioned contract**, and the version is
recorded in every run's `manifest.edn` as `:event-schema-version`. So a
log always carries the version of the contract that produced it.

- **Additive change does not bump the version.** A new event kind, or a new
  *optional* key on an existing kind, is non-breaking: nothing you already
  read has moved.
- **Anything else bumps it** — a key removed, an optional key made
  required, a value schema changed, a kind removed.
- **A key or kind slated for removal is marked deprecated here for one
  minor release before it goes**, so you get a release in which to notice
  — **waived for now.** While the event contract has no consumer outside
  this repository (it is not published to Clojars, and no downstream repo
  pins `:event-schema-version`), there is no release in between for a
  deprecation window to protect, so removals land directly. The waiver
  expires on the first such consumer, at which point the rule above binds
  as written; every removal made under it says so in the version's own
  note in the schema source.[^waiver-0151]

This is enforced rather than promised: a frozen copy of the last versioned
contract is committed alongside the current one, and the build fails if a
non-additive difference appears without a version bump.

The machine-readable contract is
[`event-schema.edn`](../components/sim-engine/resources/sim-engine/event-schema.edn)
— a self-contained [malli](https://github.com/metosin/malli) schema with
every reference inlined, readable as plain EDN without running Clojure. The
section below is generated from it.

<!-- EVENT-LOG-GENERATED-BEGIN -->

<!-- Generated by `make formats-event-log` (on `make docsgen`) from BOTH components/sim-engine/resources/sim-engine/event-schema.edn (the vocabulary and shapes) and components/sim-engine/resources/sim-engine/event-examples.edn (every rendered example below). Either one moves this block. Do not edit between the markers. -->

Event schema version: **`1.7.0`** — stamped into every run's `manifest.edn` as `:event-schema-version`.

### Read the top-level vector only

**This is the one thing most likely to go wrong, so it comes first.**

The log is a vector of events. Some of those events carry *nested* maps that
have an `:event` key of their own, drawn from a **different vocabulary** —
a `:registered` event's `:pre-horizon-facts` (clinical history that predates
the run's window) and an encounter's `:conditions`.

Those nested names are: `:condition-onset`, `:condition-end`, `:medication-order`, `:medication-end`, `:care-plan-start`, `:care-plan-end`.

And 4 of them — `:care-plan-end`, `:care-plan-start`, `:medication-end`, `:medication-order` — **are also top-level event kinds, with entirely different keys.**

So: iterate the top-level vector. Do not walk the tree looking for `:event`.
A tree-walking consumer will find pre-horizon facts, mistake them for log
events, and emit clinical activity that never happened during the run.

Nothing here is being renamed to make the collision go away — the nested
names are the vocabulary the trajectory layer genuinely uses. It is
documented instead.

### The vocabulary

There are exactly **28** event kinds. The set is closed; a
reader may treat an unknown `:event` value as a contract violation rather
than as something to skip.

`:admission` `:appointment` `:appointment-cancel` `:bed-status-change` `:bed-swap` `:cancel-admit` `:cancel-discharge` `:cancel-transfer` `:care-plan-end` `:care-plan-start` `:coverage-change` `:demographic-update` `:diagnostic-report` `:discharge` `:medication-end` `:medication-order` `:merge` `:no-show` `:observation` `:order-placed` `:outpatient-visit` `:outpatient-visit-end` `:procedure` `:registered` `:reschedule` `:result-available` `:step-rejected` `:transfer`

Every event of every kind carries these four keys:

| Key | Value | Meaning |
|---|---|---|
| `:event` | keyword | which kind this is — the discriminator |
| `:t` | integer | seconds from the start of the run, not a wall-clock instant |
| `:participants` | vector of maps | `{:patient-id :role}` — who this event is about |
| `:warm-up` | boolean | whether `:t` fell inside the configured warm-up window |

**`:active-mrn` is *not* one of them.** It is absent from `:bed-swap`,
`:merge`, and `:step-rejected`. Partition a log by `:participants`, never by
`:active-mrn`.

### Every kind

Each entry below gives the kind's meaning, which patient-state transition it
drives, its keys, and one **real** example — every example was produced by an
actual engine run, not hand-written.

#### `:admission`

A patient is admitted to a bed, allocated by the ward ladder.

**State transition:** `:new` -> `:admitted`; sets `:location`, `:home-ward`, `:attending`, `:admitted-at`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-id` | optional | string |
| `:attending` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:conditions` | optional | vector of map with keys `:event`, `:codes`, `:citation`, `:references` |
| `:encounter-id` | optional | string |
| `:event` | always | `:admission` |
| `:forced` | always | boolean |
| `:home-ward` | always | string |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:person-event-id` | optional | string |
| `:reason` | optional | string or map with keys `:system`, `:code`, `:display`, or nil |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:home-ward "ED",
 :participants [{:patient-id "PID-000000-055bdef6", :role :subject}],
 :active-mrn "MRN000001",
 :attending "3408513729",
 :warm-up false,
 :reason "Chest pain",
 :event :admission,
 :t 0,
 :location {:ward "ED", :bed "ED-H01", :placement :surge},
 :forced false}
```

#### `:appointment`

A future visit is booked for a patient (HL7v2 SIU^S12 -- deliberately unrendered in 1.7.0, see ruling C).

**State transition:** Opens the patient's `:appointment` record; terminal only when an encounter keeps it, a cancel closes it, or a no-show closes it.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-class` | always | one of `:inpatient`, `:emergency`, `:outpatient`, `:preadmit`, `:recurring`, `:obstetrics` |
| `:appointment-id` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:encounter-id` | optional | string |
| `:event` | always | `:appointment` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:reason` | optional | string |
| `:scheduled-t` | always | integer |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :appointment,
 :t 0,
 :active-mrn "MRN000003",
 :appointment-id "APT-000002-00-53068423",
 :scheduled-t 432000,
 :appointment-class :inpatient,
 :participants [{:patient-id "PID-000002-ae4a6586", :role :subject}],
 :warm-up false}
```

#### `:appointment-cancel`

A booked appointment is cancelled before its instant (HL7v2 SIU^S15 -- deliberately unrendered in 1.7.0).

**State transition:** Closes the open record TERMINALLY, outcome `:cancelled`. No encounter follows.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-id` | always | string |
| `:encounter-id` | optional | string |
| `:event` | always | `:appointment-cancel` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :appointment-cancel,
 :t 0,
 :active-mrn "MRN000004",
 :appointment-id "APT-000003-00-d8a9101a",
 :participants [{:patient-id "PID-000003-cbbefdb8", :role :subject}],
 :warm-up false}
```

#### `:bed-status-change`

A bed changes housekeeping status: vacated to `:dirty`, then `:cleaning`, then `:ready` (HL7v2 A20).

**State transition:** Changes no patient's state at all; the bed's own status moves `:from` -> `:to`.

| Key | Required | Value |
|---|---|---|
| `:bed` | always | string |
| `:encounter-id` | optional | string |
| `:event` | always | `:bed-status-change` |
| `:from` | always | one of `:ready`, `:occupied`, `:dirty`, `:cleaning` |
| `:last-patient-id` | optional | string |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:to` | always | one of `:ready`, `:occupied`, `:dirty`, `:cleaning` |
| `:ward` | always | string |
| `:warm-up` | always | boolean |

```clojure
{:ward "Renal",
 :participants [{:bed-id "RENAL-01", :ward "Renal", :role :subject}],
 :last-patient-id "PID-000000-e0bbdb7b",
 :warm-up false,
 :event :bed-status-change,
 :from :occupied,
 :bed "RENAL-01",
 :t 3600,
 :to :dirty}
```

#### `:bed-swap`

Two admitted patients exchange beds in one atomic event (HL7v2 A17).

**State transition:** Both stay `:admitted`; each takes the other's `:location`.

| Key | Required | Value |
|---|---|---|
| `:encounter-id` | optional | string |
| `:event` | always | `:bed-swap` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:swap` | always | map of string -> map with keys `:active-mrn`, `:from`, `:to`, `:attending`, `:encounter-id` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :bed-swap,
 :t 0,
 :participants
 [{:patient-id "PID-000000-e0bbdb7b", :role :subject}
  {:patient-id "PID-000001-4baa5dc0", :role :subject}],
 :swap
 {"PID-000000-e0bbdb7b"
  {:active-mrn "MRN000001",
   :from {:ward "Renal", :bed "RENAL-01", :placement :licensed},
   :to {:ward "Renal", :bed "RENAL-H01", :placement :surge},
   :attending "0096803644"},
  "PID-000001-4baa5dc0"
  {:active-mrn "MRN000002",
   :from {:ward "Renal", :bed "RENAL-H01", :placement :surge},
   :to {:ward "Renal", :bed "RENAL-01", :placement :licensed},
   :attending "7038801222"}},
 :warm-up false}
```

#### `:cancel-admit`

An admission is retracted as never having happened (HL7v2 A11).

**State transition:** `:admitted` -> `:new`; clears `:location` and `:home-ward`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:cancels-event-id` | always | integer |
| `:encounter-id` | optional | string |
| `:event` | always | `:cancel-admit` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :cancel-admit,
 :t 0,
 :active-mrn "MRN000003",
 :cancels-event-id 6,
 :participants [{:patient-id "PID-000002-61b10361", :role :subject}],
 :warm-up false}
```

#### `:cancel-discharge`

A discharge is retracted and the patient reinstated (HL7v2 A13).

**State transition:** `:discharged` -> `:admitted`; restores `:location`, `:home-ward`, `:attending`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string, or nil |
| `:cancels-event-id` | always | integer |
| `:encounter-id` | optional | string |
| `:event` | always | `:cancel-discharge` |
| `:home-ward` | always | string, or nil |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement`, or nil |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:home-ward "Renal",
 :cancels-event-id 11,
 :participants [{:patient-id "PID-000000-e0bbdb7b", :role :subject}],
 :active-mrn "MRN000001",
 :attending "0096803644",
 :warm-up false,
 :event :cancel-discharge,
 :t 0,
 :location {:ward "Renal", :bed "RENAL-01", :placement :licensed}}
```

#### `:cancel-transfer`

A transfer is retracted and the patient reinstated to where they were (HL7v2 A12).

**State transition:** Stays `:admitted`; restores the pre-transfer `:location` and `:home-ward`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:cancels-event-id` | always | integer |
| `:encounter-id` | optional | string |
| `:event` | always | `:cancel-transfer` |
| `:home-ward` | always | string, or nil |
| `:in-error` | optional | boolean |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement`, or nil |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:home-ward "Renal",
 :cancels-event-id 8,
 :participants [{:patient-id "PID-000000-e0bbdb7b", :role :subject}],
 :active-mrn "MRN000001",
 :warm-up false,
 :event :cancel-transfer,
 :in-error true,
 :t 0,
 :location {:ward "Renal", :bed "RENAL-01", :placement :licensed}}
```

#### `:care-plan-end`

A care plan closes, resolved to its start by CITATION rather than by log position.

**State transition:** Closes the matching `:care-plans` entry.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:care-plan-citation` | always | map with keys `:module`, `:state`, or nil |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:encounter-id` | optional | string |
| `:event` | always | `:care-plan-end` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:start-event-id` | always | integer, or nil |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :care-plan-end,
 :t 13046400,
 :active-mrn "MRN000002",
 :start-event-id 7,
 :care-plan-citation {:module "schema-fixture-mod", :state :the-plan},
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :stop-plan},
 :warm-up false}
```

#### `:care-plan-start`

A care plan is opened, optionally listing its planned activities.

**State transition:** Opens an `:active` entry in the patient's `:care-plans` accumulator.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:activities` | optional | vector of map with keys `:system`, `:code`, `:display` |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | always | vector of map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:care-plan-start` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :care-plan-start,
 :t 12182400,
 :active-mrn "MRN000002",
 :codes
 [{:system :snomed, :code "324911001", :display "Antibiotic therapy"}],
 :activities
 [{:system :snomed,
   :code "710824005",
   :display "Assessment of health"}],
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :the-plan},
 :warm-up false}
```

#### `:coverage-change`

A patient's insurance coverage changed: a new payer, with the payer they held before it. Deliberately renders no HL7 message of its own in 1.3.0 -- the change is visible in the IN1 of the next admission message the patient receives.

**State transition:** Writes `:payer` in `:demographics`; `:persona` (the t0 sample) is untouched.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:cause` | always | one of `:employment`, `:age-65`, `:loss`, `:eligibility` |
| `:encounter-id` | optional | string |
| `:event` | always | `:coverage-change` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:payer` | always | map with keys `:id`, `:name`, `:type` |
| `:person-event-id` | always | string |
| `:prior-payer` | optional | map with keys `:id`, `:name`, `:type` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:cause :employment,
 :participants [{:patient-id "PID-000000-00cc4d19", :role :subject}],
 :prior-payer {:id "medicare-65", :name "Medicare", :type :medicare},
 :active-mrn "MRN000001",
 :payer
 {:id "fixture-payer", :name "Fixture Health", :type :commercial},
 :warm-up false,
 :person-event-id "fixture-person-a#1",
 :event :coverage-change,
 :t 7200}
```

#### `:demographic-update`

One demographic fact about a patient changed between encounters: an address, a legal name, a corrected date of birth. Deliberately renders no HL7 message of its own in 1.3.0 -- the change is visible in the PID of every message the patient receives after it.

**State transition:** Writes one field of `:demographics`; `:persona` (the t0 sample) is untouched.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:cause` | always | one of `:residence-move`, `:residence-loss`, `:identity-correction`, `:identity-fill` |
| `:encounter-id` | optional | string |
| `:event` | always | `:demographic-update` |
| `:field` | always | one of `:residence`, `:name`, `:dob`, `:identity` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:person-event-id` | always | string |
| `:persona` | optional | map with keys `:name`, `:sex`, `:dob`, `:age`, `:address`, `:phone`, `:ssn`, `:payer`, `:race`, `:socioeconomic-category`, `:state` |
| `:placeholder-event-id` | optional | integer, or nil |
| `:prior-value` | optional | :multi or map with keys `:family`, `:given` or string matching `^\d{4}-\d{2}-\d{2}$` or one of `:known`, `:placeholder` |
| `:residence` | optional | :multi |
| `:t` | always | integer |
| `:value` | always | :multi or map with keys `:family`, `:given` or string matching `^\d{4}-\d{2}-\d{2}$` or one of `:known`, `:placeholder` |
| `:warm-up` | always | boolean |

```clojure
{:prior-value
 {:status :housed,
  :address
  {:street "1180 Sunset Blvd",
   :city "Denver",
   :state "CO",
   :zip "80202"}},
 :cause :residence-move,
 :participants [{:patient-id "PID-000000-00cc4d19", :role :subject}],
 :value
 {:status :housed,
  :address
  {:street "2 Fixture Way",
   :city "Shelbyville",
   :state "IL",
   :zip "62565"}},
 :active-mrn "MRN000001",
 :field :residence,
 :warm-up false,
 :person-event-id "fixture-person-a#0",
 :event :demographic-update,
 :t 3600}
```

#### `:diagnostic-report`

A panel of observations reported together as one document -- ONE event carrying all children, never one event per child.

**State transition:** Appends one `:observations` entry per child.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | optional | vector of map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:diagnostic-report` |
| `:observations` | always | vector of map with keys `:codes`, `:value`, `:unit`, `:value-code`, `:category`, `:reference-range`, `:interpretation` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :diagnostic-report,
 :t 12182400,
 :active-mrn "MRN000002",
 :observations
 [{:codes [{:system :loinc, :code "6690-2", :display "Leukocytes"}],
   :value 10.2,
   :unit "K/uL",
   :category "laboratory"}
  {:codes [{:system :snomed, :code "10828004", :display "Positive"}],
   :value-code
   {:system :snomed, :code "10828004", :display "Positive"},
   :category "laboratory"}],
 :codes [{:system :loinc, :code "58410-2", :display "CBC panel"}],
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :the-panel},
 :warm-up false}
```

#### `:discharge`

A patient leaves; an expired disposition marks a death, which vacates no bed.

**State transition:** `:admitted` -> `:discharged` (or `:expired`); sets `:discharged-at`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | optional | vector of map with keys `:system`, `:code`, `:display` |
| `:disposition` | optional | one of `:expired` |
| `:encounter-id` | optional | string |
| `:event` | always | `:discharge` |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :discharge,
 :t 0,
 :active-mrn "MRN000002",
 :location {:ward "ED", :bed "ED-H03", :placement :surge},
 :attending "3408513729",
 :participants [{:patient-id "PID-000001-14cf8bfe", :role :subject}],
 :warm-up false}
```

#### `:medication-end`

A medication course ends, resolved to its order by CITATION rather than by log position.

**State transition:** Closes the matching `:medication-orders` entry.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:encounter-id` | optional | string |
| `:event` | always | `:medication-end` |
| `:order-citation` | always | map with keys `:module`, `:state`, or nil |
| `:order-event-id` | always | integer, or nil |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :medication-end,
 :t 13046400,
 :active-mrn "MRN000002",
 :order-event-id 6,
 :order-citation {:module "schema-fixture-mod", :state :the-med},
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :stop-med},
 :warm-up false}
```

#### `:medication-order`

A medication is prescribed.

**State transition:** Opens an `:active` entry in the patient's `:medication-orders` accumulator.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | always | vector of map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:medication-order` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :medication-order,
 :t 12182400,
 :active-mrn "MRN000002",
 :codes
 [{:system :rxnorm, :code "308182", :display "Amoxicillin 250 MG"}],
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :the-med},
 :warm-up false}
```

#### `:merge`

Two patient records are found to be one person; the survivor absorbs the merged record's MRNs (HL7v2 A40).

**State transition:** Survivor keeps its status and gains the merged MRNs; the merged patient becomes `:merged` and emits nothing further.

| Key | Required | Value |
|---|---|---|
| `:cause` | optional | one of `:identification` |
| `:encounter-id` | optional | string |
| `:event` | always | `:merge` |
| `:merged-mrn` | always | string |
| `:merged-mrns` | always | set of string |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:person-event-id` | optional | string |
| `:surviving-mrn` | always | string |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :merge,
 :t 0,
 :participants
 [{:patient-id "PID-000000-e0bbdb7b", :role :survivor}
  {:patient-id "PID-000001-4baa5dc0", :role :merged}],
 :surviving-mrn "MRN000001",
 :merged-mrn "MRN000002",
 :merged-mrns #{"MRN000002"},
 :warm-up false}
```

#### `:no-show`

A booked appointment's instant arrives and the patient does not (HL7v2 SIU^S26 -- deliberately unrendered in 1.7.0).

**State transition:** Closes the open record TERMINALLY, outcome `:no-show`, and opens NOTHING -- which is exactly why a no-show cannot be derived from an encounter.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-id` | always | string |
| `:encounter-id` | optional | string |
| `:event` | always | `:no-show` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :no-show,
 :t 86400,
 :active-mrn "MRN000005",
 :appointment-id "APT-000004-00-a18fc750",
 :participants [{:patient-id "PID-000004-f75e1f5e", :role :subject}],
 :warm-up false}
```

#### `:observation`

An unsolicited clinical finding, not tied to any order -- a single measured or coded value.

**State transition:** Appends one entry to the patient's `:observations` accumulator.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:category` | optional | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | always | vector of map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:observation` |
| `:interpretation` | optional | one of `:normal`, `:low`, `:high` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:reference-range` | optional | map with keys `:low`, `:high` |
| `:t` | always | integer |
| `:unit` | optional | string |
| `:value` | optional | number? |
| `:value-code` | optional | map with keys `:system`, `:code`, `:display` |
| `:warm-up` | always | boolean |

```clojure
{:category "vital-signs",
 :unit "Cel",
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :value 37.6,
 :active-mrn "MRN000002",
 :warm-up false,
 :citation {:module "schema-fixture-mod", :state :the-observation},
 :event :observation,
 :t 12182400,
 :codes [{:system :loinc, :code "8310-5", :display "Body temperature"}]}
```

#### `:order-placed`

A diagnostic order is placed against an order profile.

**State transition:** No state change; the log itself is the record.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string |
| `:concept` | always | map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:order-placed` |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:profile` | always | keyword |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:participants [{:patient-id "PID-000000-055bdef6", :role :subject}],
 :active-mrn "MRN000001",
 :attending "3408513729",
 :warm-up false,
 :event :order-placed,
 :concept
 {:system :loinc,
  :code "58410-2",
  :display "CBC panel - Blood by Automated count"},
 :t 0,
 :location {:ward "ED", :bed "ED-H01", :placement :surge},
 :profile :cbc}
```

#### `:outpatient-visit`

An ambulatory encounter opens; it occupies no bed (HL7v2 A04).

**State transition:** `:new` -> `:admitted` with `:class` `:outpatient` and a nil `:location` -- the one sanctioned admitted-without-a-bed case.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-id` | optional | string |
| `:attending` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:conditions` | optional | vector of map with keys `:event`, `:codes`, `:citation`, `:references` |
| `:encounter-id` | optional | string |
| `:event` | always | `:outpatient-visit` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:reason` | optional | string or map with keys `:system`, `:code`, `:display`, or nil |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :outpatient-visit,
 :t 12182400,
 :active-mrn "MRN000002",
 :attending "9294586943",
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :visit},
 :warm-up false}
```

#### `:outpatient-visit-end`

An ambulatory encounter closes. Deliberately renders no HL7 message -- many real ambulatory feeds send an A04 and nothing else.

**State transition:** `:admitted` -> `:discharged`; sets `:discharged-at`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string, or nil |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:encounter-id` | optional | string |
| `:event` | always | `:outpatient-visit-end` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :outpatient-visit-end,
 :t 13046400,
 :active-mrn "MRN000002",
 :attending "9294586943",
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :visit-end},
 :warm-up false}
```

#### `:procedure`

A procedure is performed, cited back to the module state that produced it.

**State transition:** No state change; the log itself is the record.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:citation` | optional | map with keys `:module`, `:state` |
| `:codes` | always | vector of map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:procedure` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :procedure,
 :t 12182400,
 :active-mrn "MRN000002",
 :codes
 [{:system :snomed,
   :code "80146002",
   :display "Excision of appendix"}],
 :participants [{:patient-id "PID-000001-01564f61", :role :subject}],
 :citation {:module "schema-fixture-mod", :state :the-procedure},
 :warm-up false}
```

#### `:registered`

A patient enters the run: identity assigned, demographics sampled, any pre-horizon clinical history attached.

**State transition:** Creates the patient's fold origin; `:status` stays `:new`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:alias-name` | optional | map with keys `:family`, `:given` |
| `:encounter-id` | optional | string |
| `:event` | always | `:registered` |
| `:identity` | optional | one of `:placeholder` |
| `:mother-patient-id` | optional | string |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:person-id` | optional | string |
| `:persona` | always | map with keys `:name`, `:sex`, `:dob`, `:age`, `:address`, `:phone`, `:ssn`, `:payer`, `:race`, `:socioeconomic-category`, `:state` |
| `:pre-horizon-facts` | optional | vector of map with keys `:event`, `:codes`, `:citation`, `:references` |
| `:residence` | optional | :multi |
| `:t` | always | integer |
| `:warm-up` | always | boolean |
| `:window-close-t` | optional | integer |

```clojure
{:event :registered,
 :t 0,
 :active-mrn "MRN000001",
 :persona
 {:name {:family "Hernandez", :given "Sophia"},
  :sex :female,
  :dob "2024-12-24",
  :age 0,
  :address
  {:street "35 Aspen Way",
   :city "Albuquerque",
   :state "NM",
   :zip "87102"},
  :phone "749-382-7301",
  :ssn "900-97-1836",
  :payer {:id "self-pay", :name "Self-Pay", :type :self-pay}},
 :participants [{:patient-id "PID-000000-3cb13d09", :role :subject}],
 :warm-up false}
```

#### `:reschedule`

A booked appointment moves to a different instant (HL7v2 SIU^S14 -- deliberately unrendered in 1.7.0).

**State transition:** Moves `:scheduled-t` on the OPEN record and is NOT terminal; the id is kept rather than re-minted.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:appointment-id` | always | string |
| `:encounter-id` | optional | string |
| `:event` | always | `:reschedule` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:prior-scheduled-t` | always | integer |
| `:scheduled-t` | always | integer |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :reschedule,
 :t 0,
 :active-mrn "MRN000008",
 :appointment-id "APT-000007-00-1885e567",
 :prior-scheduled-t 259200,
 :scheduled-t 604800,
 :participants [{:patient-id "PID-000007-33c2d0f6", :role :subject}],
 :warm-up false}
```

#### `:result-available`

An order's results come back, one entry per analyte, with abnormal flags already computed against each reference range.

**State transition:** Appends to the patient's `:observations` accumulator.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string |
| `:concept` | always | map with keys `:system`, `:code`, `:display` |
| `:encounter-id` | optional | string |
| `:event` | always | `:result-available` |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:order-event-id` | always | integer |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:profile` | always | keyword |
| `:results` | always | vector of map with keys `:concept`, `:unit`, `:value`, `:reference-range`, `:abnormal-flag` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:participants [{:patient-id "PID-000000-055bdef6", :role :subject}],
 :active-mrn "MRN000001",
 :attending "3408513729",
 :warm-up false,
 :order-event-id 4,
 :event :result-available,
 :concept
 {:system :loinc,
  :code "58410-2",
  :display "CBC panel - Blood by Automated count"},
 :t 3960,
 :location {:ward "ED", :bed "ED-H01", :placement :surge},
 :profile :cbc,
 :results
 [{:concept
   {:system :loinc,
    :code "6690-2",
    :display "Leukocytes [#/volume] in Blood by Automated count"},
   :unit "K/uL",
   :value 2.0,
   :reference-range {:low 4.5, :high 11.0},
   :abnormal-flag :low}
  {:concept
   {:system :loinc,
    :code "789-8",
    :display "Erythrocytes [#/volume] in Blood by Automated count"},
   :unit "M/uL",
   :value 3.8,
   :reference-range {:low 4.2, :high 5.9},
   :abnormal-flag :low}
  {:concept
   {:system :loinc,
    :code "718-7",
    :display "Hemoglobin [Mass/volume] in Blood"},
   :unit "g/dL",
   :value 16.4,
   :reference-range {:low 12.0, :high 17.5},
   :abnormal-flag :normal}
  {:concept
   {:system :loinc,
    :code "4544-3",
    :display
    "Hematocrit [Volume Fraction] of Blood by Automated count"},
   :unit "%",
   :value 47.7,
   :reference-range {:low 36.0, :high 50.0},
   :abnormal-flag :normal}
  {:concept
   {:system :loinc,
    :code "777-3",
    :display "Platelets [#/volume] in Blood by Automated count"},
   :unit "K/uL",
   :value 302.0,
   :reference-range {:low 150, :high 450},
   :abnormal-flag :normal}]}
```

#### `:step-rejected`

A step was attempted and declined as illegal for this patient's current state -- truth about the run, never wire traffic.

**State transition:** None, by construction: evolve folds it as identity.

| Key | Required | Value |
|---|---|---|
| `:attempted-step` | always | map with keys `:type` |
| `:encounter-id` | optional | string |
| `:event` | always | `:step-rejected` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:reason` | always | one of `:illegal-bed-swap`, `:illegal-cancel-admit`, `:illegal-cancel-discharge`, `:illegal-cancel-discharge-bed-reoccupied`, `:illegal-cancel-transfer`, `:illegal-cancel-transfer-bed-reoccupied`, `:illegal-merge` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:event :step-rejected,
 :t 0,
 :participants [{:patient-id "PID-000003-9d0fdcf4", :role :subject}],
 :attempted-step {:type :cancel-admit},
 :reason :illegal-cancel-admit,
 :warm-up false}
```

#### `:transfer`

An admitted patient moves to another bed, either by a pathway step or because a bed they were waiting for came free.

**State transition:** Stays `:admitted`; rewrites `:location` and `:home-ward`.

| Key | Required | Value |
|---|---|---|
| `:active-mrn` | always | string |
| `:attending` | always | string |
| `:bed-ready` | always | boolean |
| `:encounter-id` | optional | string |
| `:event` | always | `:transfer` |
| `:forced` | always | boolean |
| `:from` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:home-ward` | always | string |
| `:location` | always | map with keys `:ward`, `:bed`, `:placement` |
| `:participants` | always | vector of map with keys `:patient-id`, `:role` or map with keys `:bed-id`, `:ward`, `:role` |
| `:placement` | optional | one of `:licensed`, `:surge` |
| `:t` | always | integer |
| `:warm-up` | always | boolean |

```clojure
{:home-ward "Renal",
 :bed-ready false,
 :participants [{:patient-id "PID-000000-055bdef6", :role :subject}],
 :active-mrn "MRN000001",
 :attending "3408513729",
 :warm-up false,
 :event :transfer,
 :from {:ward "ED", :bed "ED-H01", :placement :surge},
 :t 3600,
 :location {:ward "Renal", :bed "RENAL-01", :placement :licensed},
 :forced false}
```

<!-- EVENT-LOG-GENERATED-END -->

---

## The corpus manifest

Written as `manifest.edn` in a generated corpus's `--out-dir`. It is
the provenance record: everything that was pinned when this corpus was
made, so someone else can make the same one.

Schema: `ehrt.provenance.manifest/ManifestV1_1` — the version
`ehrt corpus generate` produces today.

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

Schema: `ehrt.corpus.lineage/LineageRecord`.

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
III.5; [^adr-0020]): the corpus manifest states *engine* provenance (which
artifact ran, under which config); this one states *transformation*
lineage (these input hashes, this operator, these output hashes) for an
in-process write that never ran an external engine at all.

Schema: `ehrt.corpus-io.operation-manifest/OperationManifestV1`.

| Field | Type | Meaning |
|---|---|---|
| `:manifest-kind` | `:operation` | discriminates this file from a corpus manifest at a glance |
| `:schema-version` | `1` | this schema's own version, unrelated to the corpus manifest's `"1.1"` |
| `:producer` | map | `{:name :identity :git}` — this repo's own honest identity (the same identity `ehrt version` reports). No `:sha256`: an absent field is honest, a fabricated one is not |
| `:operation` | map | `{:kind :operator-id :operator-version :locator}` — what was done. `:kind` is `:mutate` today, the one operation that writes a batch this way |
| `:written-at` | string | a record-keeping date — when the manifest was written, not a generation input |
| `:format` / `:framing` | keyword | read straight off the sink that wrote the batch |
| `:items` | vector of maps | one per file in the batch: `{:name :sha256 :input-hash}` — `:input-hash` (optional) is the content hash of whatever that item was derived from, present only where the write actually knew it |

A real manifest (from the "Generate controlled-fault data" strip above):

```clojure
{:manifest-kind :operation,
 :schema-version 1,
 :producer {:name "ehrt", :identity "pre-release", :git "318953e"},
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
chance of the two disagreeing. `ehrt corpus intake` recognizes this file
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

One exception, so it doesn't surprise you: `ehrt help` and `--help` print
plain text, not EDN or JSON. They are for a human or an assistant at a
shell, not for a pipeline.

### Display is not wire format

`ehrt show PATH`[^adr-0013] renders a file for a human — one thing this
page doesn't otherwise cover, since it never emits a Result envelope at
all. It's pretty-always: no flag, no TTY check, `ehrt show FILE | less`
just works. HL7 v2 (ER7) renders one segment per line, a blank line
between messages; FHIR JSON renders pretty-printed.

The ER7 rendering is **deliberately nonconformant ER7** — real ER7
segments are separated by a bare carriage return, unreadable in a
terminal, which is the whole reason `show` exists; its LF-joined output
must never be piped anywhere a real HL7 v2 consumer (an MLLP listener,
`gate v2`, anything expecting the wire format) sits. The eyes/pipes
split here is structural — a distinct verb, not a flag on a
wire-emitting path — precisely so that mistake isn't a flag away.
`show` is read-only: it never modifies the file it renders.

`ehrt play PATH`[^adr-0014] is `show` plus time: it paces the same
file's messages against their own MSH-7 timestamps (`--rate`
stream-seconds per wallclock-second, `--idle-cap` capping any single
wait) instead of rendering them all at once — at an arbitrarily large
`--rate`, it renders identically to `show`. Paced emission through
`--sink` (a `file:` designator this session) writes bytes
**byte-identical** to what an unpaced batch write through the same
designator would produce; pacing changes *when* bytes move, never
*which* bytes — timing is the instrument's own concern, entirely
outside this section's own artifact-vs-display doctrine.

### Every other command also senses a terminal now[^adr-0013]

Beyond `show`, every envelope-emitting command (`gate`, `generate`,
`mutate`, `intake`, and kin) picks a **human summary** by default when
stdout is a real terminal, and the unchanged EDN envelope when it's
piped or redirected — nothing above this section changes for a script
that already redirects or pipes output. `--pretty` forces the human
summary even into a pipe; `--edn` forces the raw envelope even at a
terminal; `--json` behaves exactly as this page already describes,
regardless of any of the above. `--report` files are untouched: always
EDN, always the bare report.

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

### Reading these from a shell

`--json | jq` is the zero-install route, and for most shell use it's
the right one — every command already accepts `--json`, so there's
nothing to install beyond `jq` itself. Both strips below are
illustrative shapes rather than runnable commands — `<…>` marks a path
you supply:

```sh
bin/ehrt gate v2 <your-corpus-dir> --json | jq '.payload.totals'
```

For querying EDN directly, or for rescuing an existing `--report` file
(always EDN, never affected by `--json`) into something `jq` can read
without a full rerun — [`jet`](https://github.com/borkdude/jet)
(borkdude) reads and writes EDN, JSON, and Transit, and is this
family's natural `jq`-equivalent for EDN. Its own README is the
authoritative reference for its query syntax; the conversion path alone
already covers the common rescue case. `jet` is an optional external
tool — it is neither vendored here nor on this workspace's PATH, so
install it yourself from
[its own repository](https://github.com/borkdude/jet) before running
the strip below:

```sh
# Rescue an existing --report EDN file into JSON for jq.
jet --to json < <your-report>.edn | jq '.totals'
```

Neither `--json` nor `jet` changes what's canonical: EDN is still the
source of truth ([above](#the-json-projection)); both are just ways to
read it from a shell instead of a REPL.

---

## Where this comes from

| Shape | Schema | Verified against |
|---|---|---|
| Report, totals, file entry | `ehrt.judge.report/Report` | live `ehrt gate v2` runs, passing and rejected, 2026-07-25 |
| Verdicts, causes, findings | `ehrt.judge.finding/Finding`, `/Verdict`, `/Cause` | the same runs |
| FHIR findings' `:disposition` / `:cause` | `ehrt.judge.fhir/interpret` | a live `ehrt gate fhir` run against a real mutant bundle, 2026-07-25 — 6554 findings, all three dispositions present |
| Check report and its codes | `ehrt.corpus.check` | live `ehrt check` runs in both golden-equivalence and per-file-assertion modes, 2026-07-25 |
| Event log, all 23 kinds | `ehrt.sim-engine.event-schema/Event` | the census's own 4,997 events across eleven corpora (`.agents/plans/2026-08-16-event-log-census.md`), reconciled against every `{:event ...}` construction site in the engine; the section above is GENERATED from the committed schema, and every example in it came out of a real engine run |
| Corpus manifest | `ehrt.provenance.manifest/ManifestV1_1` | a real generated corpus's `manifest.edn` |
| Lineage record | `ehrt.corpus.lineage/LineageRecord` | a real mutant's lineage sidecar |
| Operation manifest | `ehrt.corpus-io.operation-manifest/OperationManifestV1` | a real `corpus mutate` batch's `operation-manifest.edn`, 2026-07-28 |
| The `--json` mapping | — | the captured JSON output of the runs above, not inferred from the projection's source |

Semantics cited, never restated here: the judge/gate split[^adr-0009]
(judge vs. gate, and why the per-finding field is `:disposition` rather
than `:policy`) and the `:no-verdict` design[^tools-adr-0010] (the `:no-verdict` arm,
its `:cause` channel, and its own exit code).

[^adr-0009]: Design record [ADR-0009](../notes/ADRs.md).
[^tools-adr-0010]: Design record [tools/ADR-0010](../notes/tools/ADRs.md).
[^adr-0013]: Design record [ADR-0013](../notes/ADRs.md).
[^adr-0014]: Design record [ADR-0014](../notes/ADRs.md).
[^adr-0020]: Design record [ADR-0020](../notes/ADRs.md).
[^waiver-0151]: Design record [ADR-0151](../notes/ADRs.md).
