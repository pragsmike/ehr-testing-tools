# Docs errata: third-party-sources and two future-features entries (docs-only)

2026-09-30. Base `ce0520d1`. Ceremony: R30 (commit and push), taken
from the session prompt, which stated no prepare-only mode. Docs-only:
no source, config or test file changed. Brief: the "Stale text noticed,
not edited" list in `2026-09-30-related-work-essays.md`. Author ruling,
verbatim: "Write that prompt."

`bin/preflight` ran first, **exit 0, no findings**: last five CI runs
green (tip `ce0520d1` included), repo root not under `/mnt/`,
`core.fileMode` true, `core.ignorecase` unset, tree clean including
untracked, HEAD matched `origin/main`, HEAD not tagged `stable-*`
(disclosed, no tag owed). `git pull --ff-only`: already up to date.

## For downstream

> `components/sim/docs/third-party-sources.md` now says the GMF
> interpreter is built, counts the vendored Synthea modules (66 JSON
> modules plus 14 lookup tables, 80 files), says which half of the
> HAPI/NIST validation is in per-push CI, and states truthfully that
> Synthea's jar runs as a subprocess in one corpus lane. In
> `docs/future-features.md`, the transport entry no longer says nothing
> uses a socket, and the summarize entry's *Today:* line names `ehrt sim
> describe`. No code, config or test moved.

## Step 1 -- Synthea entry

**Interpreter status.** `components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj`
exists (with `gmf_interpreter_test.clj`), and
`components/sim/docs/sim-theory.edn`'s `:trajectory` stage (label
`RunModules`, lines 40-46) reads `:status :built`, catalytic
`["gmf-module-set" "gmf-interpreter"]`. Its non-test consumers include
`sim_engine/run.clj` and `sim_engine/assignment.clj`, so it is wired, not
just present.

**Count, and its commands** (run from the repo root, D =
`components/sim/resources/sim/modules`):

| command | result |
|---|---|
| `find $D -name '*.json' \| wc -l` | 66 |
| `ls $D/*.json \| wc -l` (top level) | 31 |
| `find $D -mindepth 2 -name '*.json' \| wc -l` | 35 |
| `find $D -mindepth 2 -name '*.json' \| cut -d/ -f2 \| sort -u` (run inside D) | 11 subdirectories: anemia dermatitis dme heart injuries medications metabolic_syndrome snf total_joint_replacement uti veterans |
| `find $D/lookup_tables -type f \| wc -l` | 14 (all `.csv`) |
| `grep -c '^\| \`' $D/NOTICE` | 80 |
| `diff` of NOTICE's first-column filenames against `find . -type f ! -name NOTICE` (inside D) | empty: one NOTICE row per file, 80 = 66 + 14 |

Edits: line 21 (built, names the namespace and component), line 23
("The 85 GMF module JSONs ... planned input" became the counted subset,
its location and its NOTICE pinning).

**Disclosed widening, inside the same entry:** line 28's SNOMED
paragraph named "the vendored Synthea module
(`resources/modules/sinusitis.json`)" and `resources/modules/NOTICE`.
Neither path exists (`ls resources/modules`: no such directory). The
related-work record listed it as its third stale item under this file.
It now reads "the vendored Synthea modules
(`components/sim/resources/sim/modules/`)" and "that directory's
`NOTICE`". Nothing else in the paragraph changed.

## Step 2 -- HAPI/NIST entry

"Not wired into CI yet" was false in part. What the tree does:

- **HAPI, wired.** `.github/workflows/test.yml:166` runs `clojure
  -M:poly test :all skip:integration`, which runs `projects/conformance`.
  Its `sim_gate_loop_test.clj` runs sim (`{:seed 42 :patients 20 :churn
  true :emit "hl7"}`) and gates every message through
  `ehrt.judge-v2-hapi.interface` against
  `projects/conformance/test-fixtures/reports/sim-v2-gate-baseline.edn`.
  `v2_structure_resolution_test.clj` pins that this tier checks types,
  not structure.
- **NIST, engine only.** The same step runs `judge-v2-nist`'s three
  test namespaces, and `test.yml:169` runs `bin/verify-nist-lock`. The
  profile tier needs a bundle, and the only one in the tree is
  `test-fixtures/v2-nist/COVID19_ELR-v2.3.1` (`find . -path '*v2-nist*'
  -type d`), which ADR-0175 section 1(iii) records as unable to run over
  this project's own corpus.
- **Measured in CI, not assumed:** run 36728470936 (tip `ce0520d1`)'s
  log shows `Testing ehrt.judge-v2-nist.v2-engine-test`, `...v2-test`,
  `...taxonomy-currency-test` and `Testing
  ehrt.conformance.sim-gate-loop-test` under `poly test :all
  skip:integration`.

The entry now says which half is wired, with both workflow lines.

## Step 3 -- the load-bearing distinction

**How Synthea runs: a separate process. The STOP branch did not fire.**
`ehrt.corpus.generate` (`components/corpus/src/ehrt/corpus/generate.clj`)
builds `["-jar" jar-path "-s" ...]` (`synthea-args`, line 102) and hands
it to `run-invocation` with the resolved Temurin `java` binary as
`:command` (lines 280-296). No `deps.edn` anywhere names Synthea
(`grep -i synthea` over root, projects, components and bases deps: only
comments), and no source loads the jar (`grep` for
`synthea-with-dependencies`, `add-lib`, `URLClassLoader`,
`DynamicClassLoader`: no hits). The jar is never auto-fetched
(`generate.clj`'s docstring, line 234); `ehrt.kernel.artifact/fetch`
and `resolve` both check its SHA-256 against `artifacts.lock.edn`
(lines 106-149, 181). No Synthea jar is tracked (`git ls-files | grep
-i 'synthea.*\.jar'`: empty).

Edits: the heading line is scoped to the tier-1 sources ("Of the tier-1
sources, only the parser is linked"), "nothing in this repo executes
their code" is replaced with the subprocess fact and its lane, the
glass-box bullet says the one upstream binary runs outside the sim
pipeline, and the license bullet says running the jar is use, not
redistribution.

**Noted, not edited:** the HAPI and NIST jars ARE `deps.edn`
dependencies (the judge components). The old heading "Only the parser
is a dependency" was false about the repo as a whole for that reason
too; the new heading is scoped to tier 1, so it no longer claims that.
Line 34's "The only runtime code dependency of the three" is the same
tier-1 scope and was left alone.

## Step 4 -- transport entry

The fact is corrected: `docs/cli.md:320` (`play --sink`) documents
`mllp://HOST:PORT` sending each message and reading its ACK, failing on
AE/AR, an unrecognized code or a missing ACK. The entry now says so,
says that is delivery rather than fault work, and stays on the menu.

**Supersession check, and the answer is "in part".** The footnote cited
ADR-0102, whose ruling row is `rulings.md#R-mllp-abandoned` ("no
transport work follows from it without a fresh ruling"). ADR-0175
(Accepted) is that fresh ruling for delivery only: ruling B1,
2026-08-27, "fan-out and MLLP as the player slices", with section (g)
designing the sink; it landed as `4e54e6f1` (2026-08-28, record
`2026-08-28-arc-4-sweep-5-fan-out-mllp.md`). No record rules transport
fault injection in. ADR-0176 restates that `R-mllp-abandoned` binds it,
but ADR-0176 is **Proposed** (`notes/ADRs.md:241`), so it is not cited
as a decision. The footnote therefore keeps ADR-0102 as the stop and
names ADR-0175 as the one exception; it implies no decision the tree
does not record. The prose names no ADR (marker-only).

## Step 5 -- summarize entry

The *Today:* line was read against ADR-0183 and the code, not the ADR
alone. `ehrt.sim-check.describe/describe` returns `:counts {:events
:subjects :by-kind}` (per-kind events and subjects), `:temporal` (the
`:t` range), `:families` and `:predicates` (`describe.clj:327-333`).
The catalog's measure rows (`describe_catalog.clj:203-240`) include
`:result-after-merge`, `:pending-result-at-discharge` and
`:transfer-between-order-and-result`, which are the inventory's three
"results landing after their subject moved, was discharged or was
merged" columns. Not covered, and so still on the menu: messages per
type (input is the event log), encounter counts, a two-run diff, merges
absorbing a bed-holder, cancels reinstating a bed (the
`:bed-reoccupied-by-someone-else` row counts REJECTED cancels, a
different thing), and peak ward census against capacity.

## Suite

`make docsgen` (log, exit 0) regenerated exactly the two INDEX.md count
lines plus their new rows and `.agents/state-derived.md`'s two cells
(session records 270 -> 271, archived prompts 262 -> 263);
`demos/traces/` came back byte-identical.

`make test`, one full run from the start, log with `MAKE_EXIT`
captured: **GREEN, MAKE_EXIT=0**. 432 `Ran ... tests` blocks summing to
5,069 tests and 29,323 assertions, zero `FAIL in`/`ERROR in` lines,
zero non-`0 failures, 0 errors` summaries; `bin/verify-nist-lock`: "OK:
6 hit-nexus-sourced coordinate(s) match artifacts.lock.edn exactly".
The docs-tooling gates ran inside it. One wording edit to
`docs/future-features.md` ("each with witnesses" -> "with witnesses
located by log index") landed after the run was launched but before its
first test namespace ran (`grep -c '^Testing ehrt'` read 0), so the run
covers the committed bytes. `make integration` was not run (docs-only;
W-1 stands).

## Close

One commit, pushed. Push verification and the CI run at the pushed tip
are reported in the session's final message; the record is committed
with the change it describes, so it cannot carry its own tip's CI id.
No roadmap row changed. No tag owed.
