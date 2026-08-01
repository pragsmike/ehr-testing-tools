# 2026-08-02 — Migration session 3: indexes become law, sim rows get their citations

## Scope

Third build session of the approved migration. Executed item 10 (the
index-completeness gate — indexes stop being prose and start being
checked) and item 3(a) (the sim citation-stubs pass, per ruling 2's
citation-only merge reading). Items 5, 8, 14 stayed fenced, per this
session's own AR-4. Standing ceremony (R30): WSL ext4 clone,
fast-forwarded to `origin/main` (`4092b4c`) before work began; `/mnt/c`
untouched.

## Red→green evidence highlights

Two live red→green cycles for the new index-completeness gate
(`ehrt.docs-tooling.index-completeness-test`), both real, both
reverted before C1's commit:

- **Missing-index seed.** An untracked file
  (`.agents/plans/_redgreen_scratch.md`) with no README entry — failed
  `every-real-item-is-indexed-test`, citing the exact filename. Reverted;
  green.
- **Ghost-entry seed.** A fabricated bullet line
  (`  * this-file-does-not-exist.md`) appended to `.agents/plans/README.md`
  — failed `every-indexed-item-is-real-test`, citing the exact ghost
  name. Reverted (confirmed `git diff --stat` clean against the
  original); green.

Full suite re-run after every seed-and-revert cycle and again after the
citation-stubs pass's own docstring edits:
`ehrt.docs-tooling.{index-completeness,readme-presence,skill-mirror-currency,
stale-path,notes-prompts-frozen,structure-currency}-test` together —
**21 tests, 347 assertions, 0 failures** at every green checkpoint.
`clojure -M:poly check`: `OK` before both C1 and C2.
`components/sim`'s own brick suite (`clojure -M:poly test :brick:sim`),
run once after all citation-stubs docstring edits since none of them
touch behavior: **71 tests, 284 assertions, 0 failures, 0 errors** —
confirms the citation edits are non-behavioral, as intended.

## Per-directory index-format choices (item 10)

Two shapes, both already established by prior sessions rather than
invented for this test:

- **Star-bullet** (`.agents/plans/`, `.agents/prompts/`,
  `.agents/session-records/`): `  * filename.md[ — description]` lines
  under a `## ... list`/`## Records` heading. Both directions checked
  by exact filename match.
- **Backtick-bullet** (`notes/README.md`, `.agents/skills/README.md`):
  `- **[`token`](...)**` or `- **`token`**` lines, one or more
  backtick-wrapped tokens per bullet. Extraction is line-anchored to
  `- **`-leading lines specifically, so an unrelated backtick token
  elsewhere in a README's own prose (e.g. `notes/README.md`'s later
  discussion of `.claude/skills/`'s mirror) is never mistaken for an
  index entry — verified by an inline test case
  (`backtick-bullet-extraction-is-actually-caught-test`'s third case).
- **Convention-exempt** (`notes/prompts/`): no per-file list (28 files
  would break the one-screen budget) — the README now states the
  `YYYY-MM-DD-<slug>.md` convention explicitly, and the gate checks
  every real file against that pattern instead of a literal list. No
  ghost-entry direction applies here (there is no list to have ghosts
  in).
- **Exempt entirely:** `notes/sim/`, `notes/tools/` (ruling 6, extended
  from README-presence to completeness — same directories, same
  frozen-provenance rationale, ratified by this session's own prompt
  dispatch, not a fresh open question). `.claude/skills/` is not
  walked — already indexed via `.agents/skills/README.md`, has its own
  separate drift gate (`skill-mirror-currency-test`).

## Item 3(a): full one-to-one accounting

Survey scope: `docs/**/*.md`, `AGENTS.md`, `README.md`, `notes/ADRs.md`,
`notes/facts-register.md`, and `components/sim/src/**/*.clj` namespace
docstrings, against all 22 rows of the frozen `notes/sim/facts-register.md`
and all 16 entries of the frozen `notes/sim/ADRs.md`. Delegated to an
Explore agent for the initial cross-reference (a 38-row search over a
large corpus); every hit it reported was independently verified against
the real file before any edit was made.

**Facts (22 total).**

| Row | Classification | Action |
|---|---|---|
| F1, F5, F10, F11, F12, F13, F16, F17, F18, F19, F20, F21, F22 | no-live-echo | none — nothing restates these live |
| F15 | already-cited | none — cited (non-canonical shorthand, unambiguous) at `notes/ADRs.md:180` and `docs/dev/way-of-working.md:80` |
| F2 | uncited-restatement | cited, `docs/glossary.md` (Synthea entry) |
| F3 | uncited-restatement | cited, `docs/glossary.md` (SimHospital entry) |
| F4 | uncited-restatement | cited, `AGENTS.md` + `docs/what-is-this.md` (CPT/SNOMED constraint) |
| F6 | uncited-restatement | cited, `docs/glossary.md` (Validators entry) + `docs/what-is-this.md` (syntactic-validity proof row) |
| F7 | **miscitation, fixed** | `order_profiles.clj` cited "`notes/facts-register.md` F7" (wrong file — the live register's own F7 is an unrelated CI-timing fact) → corrected to `sim/F7` |
| F8 | **miscitation, fixed** | `persona.clj` cited "`notes/facts-register.md` F8" (same bug) → corrected to `sim/F8` |
| F9 | **miscitation, fixed** | `emit_hl7.clj` cited "`notes/facts-register.md` F9" (same bug) → corrected to `sim/F9` |
| F14 | uncited-restatement | cited, `docs/glossary.md` (HTEST entry) |

**ADRs (16 total).**

| Row | Classification | Action |
|---|---|---|
| ADR-0005, ADR-0006, ADR-0016 | no-live-echo | none — moot post-merge (sibling-repo logistics, private remote, palgebra now native) |
| ADR-0001, ADR-0003, ADR-0015 | already-cited | none — cited correctly in `notes/ADRs.md` already |
| ADR-0002 | uncited + **miscitation, fixed** | cited: `docs/glossary.md` (Ground-truth log, Determinism), `docs/what-is-this.md` (terminology-correctness proof row), `AGENTS.md` (determinism clause), `pathway.clj`, `order_profiles.clj` (bare, requalified), `docs/site-profiles.md` (bare, requalified) |
| ADR-0004 | uncited-restatement | cited, `AGENTS.md` (test-first/properties clause) |
| ADR-0007 | uncited-restatement (bare in code) | cited/requalified: `docs/glossary.md` (NPI, Payer), `docs/what-is-this.md` (encounter horizon), `persona.clj` (×3), `config.clj` |
| ADR-0008 | uncited-restatement (bare in code) | cited/requalified: `docs/glossary.md` (decide/evolve), `engine.clj` namespace docstring (×2), `facility.clj` |
| ADR-0009 | already-cited (`notes/ADRs.md`) + **miscitation, fixed** | `engine.clj` separately cited "see `notes/ADRs.md` ADR-0009" (wrong file for the frozen claim) → corrected to `sim/ADR-0009`; `persona.clj`'s bare instances (×2) also requalified |
| ADR-0010 | uncited-restatement (bare in code) | cited/requalified: `docs/glossary.md` (MRN, Participants), `engine.clj`, `emit_hl7.clj` |
| ADR-0011 | uncited-restatement (bare in code) | cited/requalified: `docs/glossary.md` (Warm-up), `engine.clj` (×2), `emit_hl7.clj` (×2) |
| ADR-0012 | uncited-restatement | cited, `docs/glossary.md` (Step-rejected entry) |
| ADR-0013 | uncited-restatement (bare in code) | requalified, `gmf.clj` |
| ADR-0014 | uncited-restatement (bare in code) | requalified, `identifiers.clj` |

**Contradicting: none found.** Checked the two candidates most likely
to have drifted (ADR-0011's warm-up marked-vs-trimmed choice — the
frozen ADR left this open, the live docs' "marked, never trimmed" is a
resolution, not a reversal; ADR-0013 vs. the live artifact-fetch
mechanism — a wholly separate concern, not the same decision). No live
statement asserts the reverse of a frozen one.

**`notes/facts-register.md` gains F20**, a stub naming the two-file
topology (frozen sim register vs. live one) explicitly, matching the
convention `notes/ADRs.md`'s own preamble already states for ADR
citations.

## Judgment calls and their ratification status

- **Bounded scope on `components/sim/src` docstring fixes.** The
  survey found that `components/sim/src` docstrings carry far more bare
  `ADR-NNNN` references than the agent's own targeted quotes flagged —
  `engine.clj` alone has 40+ occurrences across its whole file, most in
  inline comments far from the namespace docstring. Per this file's own
  citation rule (`notes/ADRs.md`'s preamble: "a bare `ADR-00XX`... in
  any other workspace document... means this file's own record...
  never bare" for frozen-era ADRs), every one of these is technically
  non-compliant — but fixing all of them (likely 100+ instances across
  the whole `components/sim/src` tree) is not a "one session" task and
  was not attempted. **Bounded choice made:** fixed the specific
  instances the survey's own quoted evidence named, plus every bare
  reference within the SAME contiguous docstring block already being
  edited for a flagged instance (e.g. `engine.clj`'s full namespace
  docstring, once touched for its flagged lines, was made internally
  consistent rather than left partially fixed). Left untouched:
  `engine.clj`'s ~30 inline-comment instances outside that docstring,
  and any other component file's bare references the survey didn't
  specifically flag. Recorded as a named future-work item (roadmap
  "Next"), not silently left undiscovered or silently swept in full.
  **Not separately ratified** — no author present mid-session; a scope
  judgment within AR-2's own "one session" sizing, disclosed for
  review.
- **Four miscitations found and corrected, not just supplemented.**
  `order_profiles.clj` (F7), `persona.clj` (F8), and `emit_hl7.clj` (F9)
  all cited "`notes/facts-register.md` FN" — the LIVE register, whose
  own row FN is an unrelated fact — instead of the frozen `sim/FN` they
  actually meant; `engine.clj` separately cited "see `notes/ADRs.md`
  ADR-0009" for a claim that's actually `sim/ADR-0009`. These are
  citation-pointer bugs, not frozen-vs-live *content* conflicts, so
  AR-2(i)'s escalate-don't-reconcile clause (for contradictions) didn't
  apply — fixed directly, as fix-forward-with-disclosure, and named
  here rather than folded silently into the "citations added" count.
- **Item 10's directory-set scope**: same two-level enumeration
  (`.agents/`'s direct children, `.agents/skills/`'s direct children,
  `notes/`'s direct children) established for the README-presence gate
  (migration session 2), reused rather than redesigned, since item 10
  is explicitly "the same directories, now also checked for accuracy."
  Not separately ratified — matches session 2's own already-ratified
  scoping choice.
- **Ruling 6's extension to completeness**, per this session's own
  prompt text, is treated as ratified by dispatch (the prompt itself
  states the extension and asks it be recorded, not asks whether to
  make it) — recorded in `notes/ADRs.md` ADR-0023's dated-note thread
  as instructed, not re-litigated as an open question.

## Findings and HEAD landed

**components/sim/src bare-ADR-docstring debt**, named above and on the
roadmap's "Next" section: a real, sizable pre-existing gap (dating from
before the merge, when these docstrings' bare `ADR-NNNN` references
correctly meant sim's own then-current numbering) that this session's
narrower citation-stubs pass surfaced but did not fully remediate.

**Post-push message verification:** both checkpoints verified —
`git log --format=%B -1` after each push diffed byte-for-byte (except
the same single trailing-newline artifact from the message file's own
EOF seen in prior sessions, not content) against the `-F` message file
that produced it. No mismatch.

**HEAD landed:** `54ab3b6` ("docs: sim citation-stubs pass..."), pushed.
Full checkpoint shas: C1 (item 10) `77880f7`; C2 (item 3(a) + AR-3
records) `54ab3b6`. This record and the prompt archive are C3, landing
on the commit produced by this same checkpoint — cited by this
record's own filename per the self-reference convention prior sessions
already used.
