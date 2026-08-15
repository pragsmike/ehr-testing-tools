# EXP-C5 — Offline validator behavior

**Objective.** Characterize the official FHIR validator's (`validator_cli.jar`,
`artifacts.lock.edn` kind `:engine`, name `fhir-validator-cli`) verdicts fully
offline (`-tx n/a`, pinned inputs, no network) on (a) valid Synthea R4
files and (b) mutants of those files produced by all five registered FHIR
defect operators (`ehr-testing-tools.corpus.operators`). Classify every
distinct issue *category* the validator emits — `{severity, code,
expression-shape}` — with a representative example, identifying which
categories are terminology-suppressed or terminology-degraded when run
offline (known upstream behavior: locally packaged ValueSets can still
fail to validate a code — observe, don't fight it).

**Decision informed.** The verdict-mapping table in `gate.fhir`: which
issue categories map to `:rejected` (an error that is not
terminology-suppressed), which contribute `:indeterminate` findings
(terminology-suppressed), and which are `:pass`-with-findings
(warning/information).

**Apparatus.** `fhir-validator-cli` 6.9.12 (`validator_cli.jar`,
`artifacts.lock.edn`, sha256 verified against a direct re-download —
facts register [F18](../../../../notes/facts-register.md)), run under the
already-locked `temurin-jdk` 17.0.19+10 `:runtime` artifact (pattern
[#15](../../../../notes/tools/agents/memory/patterns.md): the JVM that actually runs the
validator is the pinned one, resolved through the artifact registry,
never `PATH`). A sample corpus generated via `corpus.generate` at
EXP-A4/EXP-B2's pinned settings (seed 100, clinician-seed 555,
reference-date 20260101, population 8); three patient Bundle files
selected (first three by filename sort: `Brandon214...`, `Cathie710...`,
`Daniel959...`) as the valid-file baseline; mutants of each, one per
registered operator, produced via `ehr corpus mutate`:

| Operator | Locator |
|---|---|
| `remove-required-element` | `entry[0].resource.gender` |
| `duplicate-element` | `entry[0].resource.gender` |
| `invalid-code-value` | `entry[0].resource.gender` |
| `malformed-date` | `entry[0].resource.birthDate` |
| `wrong-type-value` | `entry[0].resource.multipleBirthBoolean` |

3 valid files + 5 operators × 3 files = 18 validator invocations total.

**Procedure.**

1. Generate the sample corpus; select the three files; produce the 15
   mutants per the table above.
2. **Package-cache priming (online, once, explicitly not part of the
   offline measurement):** run the validator once against one input
   file with real network access, so it resolves and caches
   `hl7.fhir.r4.core#4.0.1` and any implementation guide packages
   referenced by the input's own `Resource.meta.profile` (Synthea R4
   output declares US Core profiles; the validator auto-resolves
   declared profiles by design — this is documented behavior, not an
   `-ig` flag we chose to pass, and there is no discovered flag to
   suppress it). This is deliberately outside the offline measurement:
   the FHIR package cache (`~/.fhir/packages`) is package-manager-style
   local state, analogous to a Maven `~/.m2` cache, not part of what
   this experiment characterizes as "offline validator behavior."
3. **Offline runs (measured):** for each of the 18 inputs, invoke
   `validator_cli.jar -version 4.0 -tx n/a -output=<file>.json <input>`
   inside `unshare -r -n` (network-namespace isolation, matching the
   hermeticity-check discipline in `AGENTS.md`) so any residual network
   attempt is a hard failure the run must survive on cache alone, not a
   silent success. `-Duser.home` is forced explicitly ahead of `-jar`
   (pattern [#15](../../../../notes/tools/agents/memory/patterns.md) applies here too: `unshare -r`
   remaps the effective uid, which changes how the JVM resolves
   `user.home` via the OS user database — forcing it keeps the FHIR
   package cache path pointed at the real cache regardless of the
   namespace's uid remap).
4. Parse each run's OperationOutcome JSON (`.issue[]`); for each input,
   tabulate a `{severity, code}` histogram. For every mutant, diff its
   histogram and its issues' `expression` values against the
   corresponding valid file's own baseline run to isolate which issues
   are *new* — introduced by the mutation — versus baseline noise every
   file in this corpus carries regardless of any defect.
5. For every distinct `{severity, code}` category observed anywhere
   (baseline or mutant-introduced), classify it:
   - **base-spec-error** — a genuine base-FHIR structural/type/
     cardinality violation, `severity: error`.
   - **profile-error** — an error attributable to a declared
     `meta.profile` constraint (e.g. US Core) rather than the base
     spec, `severity: error`.
   - **terminology-suppressed** — the issue's own diagnostics text
     states the code could not be checked (typically "without using
     server" / "doesn't provide any codes" / "doesn't provide any
     codes so the code cannot be validated") — an offline-degraded
     terminology check, not a genuine pass or fail.
   - **advisory** — `severity: warning` or `severity: information` that
     is not terminology-suppressed (a real but non-blocking observation).
   Every category gets a representative example (one real issue's
   `severity`/`code`/`expression`/`diagnostics`), not just a label.

**Expected artifacts.**

- `docs/experiments/EXP-C5-results.md` (this template:
  `docs/experiments/results-template.md`, adapted the same way
  EXP-B2's results file adapted it — a classification table in place of
  the byte-diff-oriented pin/control/canonicalize columns — self-scored
  against `docs/experiments/results-rubric.md`).
- `docs/experiments.md`'s EXP-C5 row updated with the executed date.
- `artifacts.lock.edn`'s `fhir-validator-cli` entry (already added
  alongside this protocol, per the session's own commit order).

**Acceptance.** Every `{severity, code}` category observed across all 18
runs is classified with an example, or an explicit "none observed" row
is not required (categories are additive, discovered from real output,
not enumerated in advance); the verdict-mapping table `gate.fhir` will
carry is derived directly from this classification, not re-litigated.

**Pre-authorized decision rule** (from the design channel, ADR-consistent
with the author rulings in the P5 prompt): `error`-severity issues that
are *not* terminology-suppressed map to `:rejected`; `warning`/
`information`-severity issues map to `:pass` with findings recorded;
terminology-suppressed issues (any severity) map to `:indeterminate`
findings. Overall file verdict is worst-of `:rejected` > `:indeterminate`
> `:pass`. Profile-vs-base-spec attribution (the base-spec-error /
profile-error split above) is recorded as a classification finding but
does **not** change the mapping in `gate.fhir` this session — the gate
targets whatever the validator actually checks given the input's own
declared profile, with no `-ig` pinned; splitting profile-errors out of
the rejection path is future work if the motivating deployment's corpus
turns out to require it.

**Stop condition.** A `{severity, code}` category whose diagnostics text
doesn't fit any of the four classification buckets above is recorded as
an open finding, not silently dropped or force-fit. Effort cap: this
protocol classifies observed categories: it does not investigate *why*
the validator's terminology or profile-resolution logic behaves a given
way beyond what's needed to classify the observed diagnostics text, and
it does not attempt to make every issue on the corpus disappear (some
categories, e.g. profile-driven US-Core warnings on non-US-Core-authored
Synthea output, are expected noise this experiment characterizes rather
than eliminates).
