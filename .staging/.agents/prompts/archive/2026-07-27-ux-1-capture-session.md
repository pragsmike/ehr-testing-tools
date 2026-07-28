# 2026-07-27 — UX-1 (`ehr-testing-tools`): CLI ergonomics capture — deterministic defaults, one flag vocabulary, sniffing gate; coordinated with SS-1

## Context

The CLI's common operations are flag-heavy and its flag vocabulary is
inconsistent: `ehr corpus generate` requires six flags for a basic run;
the same concept is spelled `--lockfile` here and `--lockfile-path`
there, `--out` / `--output-dir` / `--out-dir` name three output
concepts, and inputs arrive as `--input`, `--source-dir`, or a
positional depending on the verb. This is pre-release — ADR-0004's own
positioning is that interfaces may still move, and the Clojars/Maven
decision has not been taken — so breaking flag changes are cheap
exactly now and become migration debt at first release. SS-1
(`docs/source-sink-design.md`) will introduce the URL/source-sink
surface over these same inputs and outputs; this capture RULES the
ergonomics so SS-1 and the CLI change the surface once, together.

This is a **capture session**: it lands a design section, an ADR, and
amended SS plan rows. No `src/` code. The defaults table below is
author-ratified except where marked OPEN; the governing law is stated
in ruling 1 and is not negotiable.

## Read first

- `src/ehr_testing_tools/cli/help.clj` (`cli-spec` — the single source
  `docs/cli.md` and `ehr help` render from), `docs/cli.md`
- `docs/source-sink-design.md` (D4 maps-canonical/URLs-surface; SS-1
  row in `.agents/plans/corpus-foundations.md`)
- `src/ehr_testing_tools/corpus/generate.clj` (the pinned-constant
  precedent: `default-locale`, `default-timezone`),
  `corpus/intake.clj` (format sniffing), `corpus/operators.clj`
  (registry shape, for default locators)
- `docs/use-cases.md` (the ten verified command strips — every one is
  re-verified in the build session that implements this capture)
- `SETUP.md` (first-run walkthrough; `ehr doctor`'s checklist source)

## Author rulings

1. **The determinism law of defaults.** A flag may default only to a
   pinned constant or a value derived deterministically from other
   pinned inputs. No default may read the clock, the environment,
   the network, or the machine. (`--received` on `corpus intake`
   currently defaults to `today` — that is a *record-keeping* date, not
   a generation input, and is the one deliberate exemption; the ADR
   names it and says why lineage dates may be wall-clock while
   generation inputs may not.)
2. **Ratified defaults for `corpus generate`** (zero-flag happy path):
   `--seed 1`; `--clinician-seed` defaults to the value of `--seed`;
   `--reference-date 20260101` (a named pinned constant beside
   `default-locale`, with a comment stating it is intentionally frozen,
   not "today"); `--population 5` (small enough for a fast first run);
   `--output-dir` derived as `target/corpus/synthea-s<seed>-p<pop>`;
   `--config-path` defaults to a minimal properties file SHIPPED in
   `resources/` (authored in the build session; its content is part of
   the pin). `ehr corpus generate` with no flags must therefore be
   byte-reproducible across machines given the same pinned artifacts —
   state this as the acceptance property.
3. **One flag vocabulary, table ratified:**
   | Concept | Spelling | Replaces |
   |---|---|---|
   | lockfile path | `--lockfile` | `--lockfile-path` |
   | output file | `--out` | — |
   | output directory | `--out-dir` | `--output-dir` |
   | primary input | positional `PATH` (with `--path` as the explicit twin, existing gate/check precedent) | `--input`, `--source-dir` |
   Old spellings are REMOVED, not aliased — pre-release, one vocabulary,
   no deprecation shims to carry into release. `docs/cli.md` and
   `ehr help` regenerate from the spec so they cannot drift.
4. **`ehr gate PATH` sniffs.** Bare `gate` dispatches per file via the
   existing intake sniffing heuristic; `gate v2` / `gate fhir` remain
   as explicit overrides and are what mixed-format directories require
   (a sniff-dispatched directory containing both formats is an error
   naming the override, not a silent split — OPEN-1 below if the author
   prefers silent per-file dispatch).
5. **`corpus mutate` defaults:** `--out-dir` derives as
   `<input>-mutants/<operator-id>@<version>/`; each registry entry MAY
   declare `:default-locator` (its canonical conviction target — the
   catalog already records per-operator doc and conviction evidence);
   `--locator-path` remains required for operators that do not declare
   one. No operator's default locator is invented in the capture —
   declaring them is build-session work against `judge-calibration.md`.
6. **New conveniences, ruled in:** `ehr version` (prints the repo
   version-of-record and the pinned artifact versions from the
   lockfile); `ehr artifact fetch --all` (every artifact in the
   lockfile — collapses the three-incantation T2 setup);
   `ehr doctor` (runs SETUP.md's verification checklist as a command:
   WSL detection where relevant, java resolution via the registry,
   artifact cache presence, exit 0/1/2 per the existing ladder).
   `doctor`'s checklist content is drawn from SETUP.md so the two
   cannot disagree — the capture states this as a freshness obligation
   (same pattern as cli.md).
7. **Coordination with SS-1 is a sequencing rule:** the flag-vocabulary
   table and the URL surface land in the SAME build session (SS-1 grows
   to include this capture's CLI changes, or a UX build session lands
   immediately before it and SS-1 rebases — agent proposes which in the
   plan amendment, author decides at build time). `use-cases.md`'s ten
   command strips are re-verified end-to-end in whichever session
   changes the surface; the quickstart's structural enforcement must
   stay green with the new zero-flag `generate` as its first command.
8. **OPEN (recorded, not resolved):** OPEN-1 mixed-format directory
   behavior under bare `gate` (error-naming-override vs silent per-file
   dispatch); OPEN-2 whether `--population 5` or `1` (speed vs
   corpus-usefulness of the default); OPEN-3 whether `ehr doctor`
   belongs in the first release or after.

## Steps

### Step 1 — Design section

Append a "CLI ergonomics" section to `docs/source-sink-design.md` (it
is the document SS-1 builds from): rulings 1–7 as numbered decisions
continuing the existing D-numbering, the defaults table, the flag
vocabulary table, the acceptance property of ruling 2, OPEN-1..3
alongside D-a..D-c.

**Commit:** `docs: CLI ergonomics ruled — determinism law of defaults, one flag vocabulary, sniffing gate (capture)`

### Step 2 — ADR

Next number: the determinism law of defaults (with the `--received`
exemption reasoned); the pre-release-breaking-changes ruling (old
spellings removed, no aliases, and WHY now: the Clojars/Maven decision
is the deadline after which this becomes migration debt); the
one-surface-change coordination with SS-1. Alternatives considered:
deprecation aliases (rejected pre-release — shims with no users);
wall-clock defaults for convenience (rejected — the tool's own reason
for existing); leaving generate flag-heavy (rejected — the zero-flag
run is the quickstart's first impression and the acceptance property
makes it a reproducibility demo, not a convenience).

**Commit:** `adr: CLI ergonomics — determinism law of defaults, single flag vocabulary, pre-release breaking window`

### Step 3 — Plan amendment

Amend the SS-1 row (or insert a UX-1 build row immediately before it —
ruling 7's proposal, marked for author decision) naming: cli-spec
changes, the shipped default properties file, sniffing gate, the three
conveniences, use-cases re-verification, quickstart re-verification.
Test-first obligations and verification tier (T0 + T1; T2 only if
`corpus/generate.clj` or gate seams change per the trigger list).

**Commit:** `plans: UX build work staged, coordinated with SS-1 (one surface change)`

### Step 4 — Archive, push

Archive this prompt; push per the ritual.

**Commit:** `prompts: archive 2026-07-27 ux-1 capture session`

## Final report

D-numbers taken, the SS-1-vs-UX-1-build sequencing proposal made
(ruling 7), OPEN items as recorded, deviations.
