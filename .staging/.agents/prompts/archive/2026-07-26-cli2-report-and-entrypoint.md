# CLI-2 — Report writes conform to ADR-0004; bin/ehr becomes the entry point

You are working in `ehr-testing-tools` (public). This session executes
CLI-2, the micro-wave answering DOC-4's two deliberately-unacted
findings (`.agents/plans/user-docs.md`, DOC-4 close-out report), plus
one stale-docstring rider. Two phases. **Phase A** (uncontested):
`--report <path>` into a missing directory stops failing with a raw
`FileNotFoundException` and starts conforming to ADR-0004; the strips'
`mkdir -p` workarounds are deleted; `contract_pairing_test.clj`'s ns
docstring loses its stale test incantation; and — added by the author
mid-planning — the ehr-testing-sim mounting note is recorded as
ADR-0012 with its dispatch-site pointer (Step A4), so the CLI
properties sim relies on are commitments *before* any session
refactors near them, this one included. **Phase B** (author
ratifies the whole phase or strikes the whole phase — it is a
convention change and cannot land by halves): a `bin/ehr` wrapper
becomes the taught entry point, because `make ehr` structurally cannot
carry the CLI's 0/1/2/3 exit contract — GNU make exits 2 for any
failed recipe, which DOC-4 measured. Step B0 proves that
infeasibility claim before anything is built on it: if a probe shows
make *can* propagate, stop Phase B and report — the premise, which is
the prompt-writer's, would be wrong, and that has happened before
(DOC-1). Phase B includes a **narrow SETUP.md fence lift**: SETUP is
out of scope by standing ruling (externally validated by the trial
cohort), but it carries five live `make ehr` sites, and a convention
change that skips them teaches two spellings — so the lift permits
mechanical `make ehr ARGS="X"` → `bin/ehr X` substitutions in
SETUP.md and nothing else, with a close-out note that SETUP has
drifted from its validated text and may warrant a cohort re-check.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`,
`.agents/plans/user-docs.md` (DOC-4's findings report — the problem
statements, with measurements), `src/ehr_testing_tools/cli.clj` (the
`--report` write sites in the gate and check paths, and the error
categories in play), `src/ehr_testing_tools/result.clj`,
`notes/ADRs.md` ADR-0004 (the contract the fix conforms to),
`Makefile`'s `ehr:` recipe (what bin/ehr wraps: `clojure -M -m
ehr-testing-tools.cli $(ARGS)`), `docs/use-cases.edn` (the strips
carrying `mkdir -p` workarounds and, if Phase B runs, the
`make ehr` spellings), `src/ehr_testing_tools/usecases.clj`'s
renderer literals and `docs/cli.md`'s generated preamble (convention
statements that regenerate), `test/ehr_testing_tools/usecases_test.clj`
(four `make ehr` expectation sites — Phase B's enumerated test
touches), `test-integration/ehr_testing_tools/contract_pairing_test.clj`
(the stale docstring), `notes/ehr-testing-sim-mounting-note.md`
(the cross-repo note — the author places this file in the repo before
this session runs; if it is absent, stop and ask rather than working
from memory of it), `README.md` Quickstart and `SETUP.md` (the
live convention surface — grep for every `make ehr` yourself; the
history rule applies: archived prompts and handoffs stay put).
Ritual: commit → `git push origin`. Save this prompt to
`.agents/prompts/2026-07-26-cli2-report-and-entrypoint.md`; final
commit archives it.

Author rulings in effect: **`--report` creates the parent** — the
user said where they want the file; `io/make-parents` before the
write, and residual IO failures (unwritable path, disk full) become a
categorized `result/error` (a new additive category such as
`:report-write-failed`, payload carrying the path and the cause's
message — DOC-1's honest-error register), never an uncaught throw.
Applies to every `--report` write site, gate and check both, and to
nothing else this session. **Strips lose the workaround and are
re-verified** — the `mkdir -p` lines and their why-comments leave
`use-cases.edn`; each edited strip re-runs once with dated evidence
(the cheap gate/check strips — do not re-run the long generation
pipelines for an edit that doesn't touch them). **The docstring fix
is doc-only** — the incantation aligns with what `README.md` now
teaches for integration runs; no assertion changes in that file.
**Phase B's wrapper is minimal** — `bin/ehr`: bash, resolves the repo
root from its own location, `exec`s the same `clojure -M -m` the make
recipe runs, so equivalence is by construction; a header comment
notes the WSL2 expectation. Its exit-code fidelity is proven by
run-once evidence covering all four codes (0: `help`; 2: an unknown
verb; 1: a rejecting `gate v2` fixture run; 3: a no-verdict aggregate
under the default policy — Step B0 locates or synthesizes the 3-case
from fixtures) recorded in the commit body, plus one automated smoke
(`bin/ehr help` exits 0 and emits usage) wired wherever the fast
lint tier already runs — if wiring it is not a one-liner, note it
for ENF instead of building CI surface. **`make ehr` survives** as a
compatibility spelling — recipe unchanged, but its `make help` line
and Makefile comment now name `bin/ehr` as primary and state the
exit-code collapse in one clause. **The convention sweep is
complete or not at all** — every live `make ehr` teaching site
(README, SETUP per the fence lift, use-cases.edn strips and notes,
renderer/preamble literals, AGENTS.md's one site if it is live
instruction) flips to `bin/ehr` in one commit, generated docs
regenerated; the three verdict-branching strips' direct-invocation
notes are then deleted — they exist only because make swallowed the
codes. The four `usecases_test.clj` expectation sites are Phase B's
only permitted existing-test edits. **ADR-0012 records commitments,
not designs** — it distills the note's five load-bearing CLI
properties and the two manifest commitments (version-don't-mutate;
the binding contract test lives in tools' `test-integration/` once
sim is mounted) as refactor-stable interface commitments, each stated
with its safe/breaking line; verify the note's factual claims against
source while writing (the babashka.cli single-parse claim, the
manifest version names) and record what *is*, not what the note says;
mount-time design choices (dependency coordinates, optional loading,
the merged-spec assertion, the contract test itself) are explicitly
out of ADR scope and out of this session — they belong to the future
mount session. The note itself commits verbatim as provenance; the
ADR links it. And Phase B must close with one recorded check:
`bin/ehr` sits entirely outside the parse-and-dispatch boundary, so
properties 1 and 2 are untouched — state that in B1's commit body,
don't leave it implicit. **A quoting bonus is worth one
sentence** — `bin/ehr gate v2 …` needs none of `ARGS="…"`'s nested
quoting (a WSL pain DOC-1 hit); the README may say so where it
introduces the wrapper, once.

## Phase A

**Step A1 — the fix.** `io/make-parents` + categorized residual
failure at every `--report` write site; tests: nested nonexistent
path now succeeds (both gate and check), a genuinely unwritable path
yields the new category with exit 2, and the category-survival test
gains the new member. Existing tests untouched.
Commit: `CLI-2: --report creates parents; residual IO failures are categorized (ADR-0004)`.

**Step A2 — strips shed the workaround.** The `mkdir -p` lines and
their explanatory comments leave the affected strips; regenerate;
re-run the edited strips once, dated evidence in the body.
Commit: `CLI-2: strips drop mkdir workaround (re-verified)`.

**Step A3 — the docstring.**
Commit: `CLI-2: contract-pairing ns docstring drops stale incantation`.

**Step A4 — ADR-0012 and the dispatch-site pointer.** Commit the
note verbatim to `notes/ehr-testing-sim-mounting-note.md` (provenance;
if the author already placed it there, just stage it). Write ADR-0012
per the ruling — the five properties with safe/breaking lines, the
two manifest commitments, factual claims re-verified against source,
a link to the note. At the dispatch site in `cli.clj`, the note's
suggested summary comment, with ADR-0012's number filled in and
nothing speculative added (no `"sim"` arm exists yet — the comment
guards the properties, it does not pre-build the mount).
Commit: `CLI-2: ADR-0012 — the CLI properties ehr-testing-sim mounts against (note vendored)`.

## Phase B (ratified as a whole, or struck as a whole)

**Step B0 — prove the premise.** A three-line probe: a make target
whose recipe exits 3, run, `echo $?`. Expected: make exits 2 and the
3 is unrecoverable by any recipe-level construct (`.SHELLSTATUS`
included — check it). Record the probe verbatim in the commit body.
Also locate the exit-3 fixture case for the fidelity evidence, and
grep the complete live `make ehr` site list (expected files: README,
SETUP ×5, use-cases.edn, use-cases.md, Makefile, AGENTS.md ×1,
usecases.clj literals, usecases_test.clj ×4 — verify, don't inherit).
If the probe shows propagation is possible, stop Phase B, report;
the wrapper may still be the right call but the author decides on
true premises.
Commit: `CLI-2: probe — make cannot carry the exit contract (evidence); convention site list`.

**Step B1 — the wrapper, proven.** `bin/ehr` per the ruling,
executable bit set (mind WSL vs. NTFS on the mode bit — verify git
records 100755), four-code evidence, the smoke check.
Commit: `CLI-2: bin/ehr — the exit contract survives the entry point`.

**Step B2 — the sweep.** Every live site in one commit, per the
ruling; regenerate; edited strips re-run once (cheap ones; the
convention edit does not touch the long pipelines' semantics — the
wrapper's by-construction equivalence plus the four-code proof
covers them, say so in the body); direct-invocation notes deleted;
SETUP substitutions mechanical-only.
Commit: `CLI-2: bin/ehr is the taught entry point everywhere (make ehr stays as compat)`.

## Close out

Extended golden check clean (use-cases.md and cli.md regenerate
intentionally, committed); full suite + both lints; link check on
touched docs. Plan updates: CLI-2 section (interlude) + tracker row →
Done, recording ADR-0012's landing and the entry-point decision (option (b), decided
2026-07-26, premise proven at B0) — or, if Phase B was struck or
stopped, exactly what did land and why; the SETUP drift note for the
author. Archive this prompt.
Commit: `CLI-2 complete (archives prompt)`.
