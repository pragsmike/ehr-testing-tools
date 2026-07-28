# DOC-4 — Runnable strips: use cases gain their commands, single source of truth

You are working in `ehr-testing-tools` (public). This session executes
DOC-4 of `.agents/plans/user-docs.md`. The author has ruled the route
(record the ratification at close-out, decided 2026-07-26): **single
source of truth** — `docs/use-cases.edn` gains a `:commands` field and
the `use-cases.md` renderer renders it, so the strips live in the same
freshness-gated generated document as the equations they ground, and
the golden check inherits them. The audit finding this closes: 14
formal use cases, zero `ehr` invocations — every reader who finds
their case must reverse-engineer the shell from the README. After this
session, a use case answers "what do I type" in the same breath as
"what do I get." Two disciplines govern the data: **no command lands
in the EDN unverified** — every internally-drivable strip is actually
run, once, locally, with dated evidence in its batch's commit body
(formats.md's capture style; this is run-once evidence, not
enforcement — enforcement is DOC-5's job, do not build scripts or CI
for strips this session); and **no command is invented for cases the
repo can't drive** — external-stage and `:planned` cases get an honest
stub rendered from what's already in their `:bring`, not a
hypothetical invocation.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md` (§6 — strips are the most
copy-pasted surface this plan produces), `.agents/plans/user-docs.md`
(DOC-4 section + the route decision being recorded),
`docs/use-cases.edn` (whole file — every case's `:maturity`, `:bring`,
`:get`, and equations; the maturity taxonomy in the header comment is
the drivability rubric's raw material), `src/ehr_testing_tools/
usecases.clj` (the Malli schema `:commands` joins and the renderer it
extends), `docs/use-cases.md` (current rendered shape — where in a
case's layout the strip belongs), `README.md`'s Quickstart lines
104–150 (the invocation convention — `make ehr ARGS="..."` — and the
only strips that exist today, which the corresponding cases' `:commands`
should agree with, not diverge from), `docs/cli.md`, `docs/operators.md`,
`docs/locators.md` (what strips link instead of restating — flag
tables, operator ids, locator syntax), `docs/judge-calibration.md`
(what the gate/judge strips link for verdict reading),
`test-integration/ehr_testing_tools/contract_pairing_test.clj` (the
exemplar one use case names — its strip should point at it, not
duplicate it). Ritual: commit → `git push origin`. Save this prompt
to `.agents/prompts/2026-07-26-doc4-runnable-strips.md`; final commit
archives it to `.agents/prompts/archive/`.

Author rulings in effect: **Invocation convention is the README's** —
`make ehr ARGS="..."` exactly as the quickstart writes it (verified:
that is the convention at HEAD); strips and quickstart must agree
wherever they cover the same ground, and if a strip improves on a
quickstart line, the README line changes too, in the same batch
commit, so the two never teach different spellings. **`:commands`
shape is implementer's discretion** within these constraints: the
rendered result is one copy-pasteable fenced block per case (prose
annotations outside the fence or as `#` comment lines within it —
nothing a paste would break), placeholders for user-supplied values
are visually unmistakable (the quickstart's `$PATIENT_FILE` style),
and the schema validates whatever shape you choose. **Drivability is
per-case and evidence-based** — classify each of the 14 at Step 0
from its own `:maturity`/`:bring`/equations: `:usable`/
`:experimental` cases are expected drivable end-to-end;
`:illustrative` cases get a strip only if the composition genuinely
runs from shipped stages (run it to find out — if it doesn't run, it
gets a stub that says exactly which stage is missing, which is more
honest than the audit's silence); `:planned`/external cases get the
stub. **Stubs render from data too** — a case without `:commands`
renders a short "you bring" sentence derived from `:bring`, plus,
where an external dependency is the blocker, the plan pointer (house
precedent: DOC-2's gap sentences). **Long runs go to the
background** — the generation-pipeline strips take real minutes
(integration-tier runtime, warm artifact cache expected from WSL);
run them in the background with a scheduled check-in per the standing
caution, don't sit on them. **Optional, author may strike:
`ehr corpus operators` surfaces `:doc`** — the listing verb's rows
gain the one-line description DOC-3 put in the registry (additive key
in the result; only the operators-verb tests enumerated at Step 0 may
change; `cli.md` regenerates only if the verb's help line changes,
and the golden check must come back clean either way). **Ride-along
record repair** (house precedent, one line): the plan's DOC-3
close-out records the suite at 466/1391 while LOC-1's measured
baseline at the same commit was 467/1403; run `make test` at HEAD,
and annotate or correct whichever line is stale — dated, in the
LOC-1-nit style, its own micro-commit or folded into close-out.

## Step 0 — Evidence: the drivability table and the schema ground

One commit, evidence in the body. The drivability table: all 14 case
ids × (maturity, expected classification, the specific reason —
"needs foreign v2 corpus, external" / "composable from Generate ⨟
Mutate ⨟ Gate, expect runnable" / etc.). The schema ground: the
current `UseCases` Malli shape and the renderer's per-case layout,
plus where the strip slots in it. The quickstart↔case
correspondence: which README lines already cover which case ids
(expected: roughly three). If the optional `:doc` step is in effect,
enumerate the operators-verb tests (the only tests allowed to change
all session — this session's data work should otherwise touch no
test).

Commit: `DOC-4: drivability table + schema ground (evidence before data)`.

## Step 1 — Schema + renderer

`:commands` (your chosen shape) joins the schema as optional;
the renderer renders it as the fenced strip in the case layout, and
renders the data-derived stub for cases without it (per the ruling —
external blocker cases carry the plan pointer). Regenerate;
`use-cases.md` at this commit shows stubs where stubs belong and no
fabricated commands anywhere. Golden check clean (regeneration
committed). Renderer changes covered by `usecases`' existing test
pattern if one exists; if none exists, a minimal render test for
both arms (strip present / stub) — additive.

Commit: `DOC-4: use-cases schema + renderer learn :commands (strips + honest stubs)`.

## Steps 2–4 — The data, in three verified batches

Batch order groups by what a verification run costs, cheap first:
**(2) corpus-side cases** — generation, intake, mutation, the
operator-catalog-facing cases; **(3) gate/judge-side cases** — gate
fhir, gate v2, check, baseline mode, calibration-facing cases;
**(4) the composition cases + stubs** — contract-pairing (whose strip
points at the exemplar suite per the Read-first), any `:illustrative`
composition that Step 0 expected runnable, and the final stub-only
cases. Each batch: write the `:commands` data, run every strip in the
batch end-to-end (background + check-in for the long ones), capture
dated evidence in the commit body (command, exit code, one-line
outcome — not full transcripts), regenerate, commit. Where a strip
and a README quickstart line cover the same ground, they agree by the
end of the batch (README edits ride in the batch commit per the
ruling). Cross-link, don't restate: operator ids point at
operators.md, locator syntax at locators.md, flags at cli.md /
`ehr help`, verdict reading at judge-calibration.md. A strip that
fails to run is not massaged until it passes silently — it either
gets fixed and re-run clean, or the case reclassifies to a stub with
the failure recorded; if the failure looks like a repo bug rather
than a strip bug, stop the batch and report (LOC-1's genus — a wart
found by probing is a finding, not an obstacle).

Commits: `DOC-4: strips — corpus-side cases (run-verified)` /
`DOC-4: strips — gate and judge cases (run-verified)` /
`DOC-4: strips — composition cases + honest stubs (run-verified)`.

## Step 5 (optional, per the ruling) — `ehr corpus operators` surfaces `:doc`

Additive key in the verb's rows; enumerated tests updated; `cli.md`
regenerated only if its rendered content actually changes; golden
check clean.

Commit: `DOC-4: corpus operators listing surfaces :doc (additive)`.

## Step 6 — Close out

Extended golden check clean; full suite + both lints green; link
check across `use-cases.md` (it just gained many relative links) and
any touched README lines. Plan updates: DOC-4 tracker row → Done with
itemized summary, per-case final classification (strip vs. stub, any
Step-0 expectation that flipped on evidence), and the prompt path;
the Open decisions section records the route ratification (single
source of truth, decided 2026-07-26); the suite-count record repair
lands if it didn't already. Archive this prompt.

Commit: `DOC-4 complete: use cases answer "what do I type" (archives prompt)`.
