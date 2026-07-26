Pack hygiene + research errata — two small closures from the corpus adoption

You are working in `ehr-testing-tools`. The 2026-07-26 SimHospital corpus adoption session (`.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md`, session report in that session's record) left two deliberate residues for a follow-up: (1) `make pack` now inlines the 1.1 MB vendored corpus into every pack, and (2) `docs/research/HL7v2-sanitized-corpus-research.md` carries three claims that the adoption session's F-rows refined or corrected, with no pointer from the doc to the register. This session closes both. It is a small, doc-and-Makefile session: no `src/` changes, no test changes, no new F-rows.

Since the corpus adoption, the CLI-2 session has also landed (ADR-0012; `bin/ehr` is now the taught entry point, `make ehr` a compat spelling). It is irrelevant to this session's edits — the pack machinery was untouched — but it moved the pack targets' line positions, so locate by name, not by remembered line number, and leave the `ehr`/`bin/ehr` material alone.

Read first: `AGENTS.md`; `AUTHORS-GUIDE.md` §2 (the pack utilities); `Makefile` — the `PACK_ELIDE_PATTERN` variable near the top (line 20 at this writing) and both `pack:` and `pack-skills:` targets with their comments, wherever they now sit (~151 and ~178 at this writing) — read the `pack-skills` recipe carefully; the trap this prompt warns about below is real; `notes/facts-register.md` rows F24, F25, F28; `docs/research/HL7v2-sanitized-corpus-research.md` (skim for the three claim sites named in Step 2); `docs/README.md` (the research/ section); `test/fixtures/v2/simhospital/PROVENANCE.md`.

Commits from WSL. `make test` green before and after. Save this prompt to `.agents/prompts/2026-07-26-pack-elide-and-research-errata.md`; final commit archives it.

## Author rulings in effect

Written as settled. The author ratifies or strikes; do not relitigate.

1. The corpus bytes are elided from `make pack`; its provenance is not. `messages.out` alone leaves the pack. `PROVENANCE.md` and the vendored `LICENSE` stay packed — provenance is exactly what a pack-consuming session needs; the ER7 bytes are exactly what it doesn't.
2. `pack-skills` must not change. Its output today contains `.agents/skills/**` and `.agents/prompts/archive/**` and nothing else; that remains true after this session, byte-for-byte in file membership. This forces the variable split in Step 1 — the corpus cannot simply be appended to `PACK_ELIDE_PATTERN`, because `pack-skills` packs that pattern's matches.
3. Research docs are dated records; errata point, they don't rewrite. The body of `HL7v2-sanitized-corpus-research.md` is not edited. Corrections take the form of a dated errata block at the top of the file, each entry citing the register row that carries the evidence. This is a new convention; Step 2 establishes it, this doc is its first instance, and `docs/README.md` gets one sentence describing it.
4. Scope fence. No changes to: `messages.out` (any byte), `test/**`, `src/**`, `bin/ehr`, the `ehr` target and its CLI-2 compat comment, `pipeline.edn`, `use-cases.edn`, ADR bodies (ADR-0012 now exists; this session cites ADR-0011 and touches neither), facts-register rows (this session cites F24/F25/F28; it does not edit or add rows), handoffs, archived prompts, or the research doc's body text below the errata block.

## Step 1 — Split the elide pattern; elide the corpus from `pack`

All edits in `Makefile`, plus one paragraph in `AUTHORS-GUIDE.md` §2.

1. Replace the single pattern variable (currently line 20):

```make
PACK_SKILLS_PATTERN := ^\.agents/skills/|^\.agents/prompts/archive/
PACK_ELIDE_PATTERN  := $(PACK_SKILLS_PATTERN)|^test/fixtures/v2/simhospital/messages\.out$$
```

Note the `$$` — this is a Make file; a single `$` would be eaten by Make before the shell sees the ERE anchor. Keep (and extend) the existing comment above the variables: the pattern pair's contract is that `pack` elides `PACK_ELIDE_PATTERN` and `pack-skills` packs `PACK_SKILLS_PATTERN` — they are no longer the same set, and the comment must say why (ADR-0011 vendored data: bytes belong in git, not in session context; provenance stays packed).
2. `pack` recipe: the `grep -Ev` already uses `PACK_ELIDE_PATTERN` — unchanged. Update the header line (currently `echo "elides: .agents/skills, .agents/prompts/archive"`) to also name `test/fixtures/v2/simhospital/messages.out (corpus bytes; provenance stays packed)`.
3. `pack-skills` recipe: change its `grep -E '$(PACK_ELIDE_PATTERN)'` to `grep -E '$(PACK_SKILLS_PATTERN)'`. Its header's `includes only:` line is already accurate and stays.
4. `help` target: the `pack` line's parenthetical gains the corpus mention; the `pack-skills` line is already accurate and stays.
5. `AUTHORS-GUIDE.md` §2: the paragraph describing what `pack` elides gains the corpus bytes, with the one-line rationale (large, static, content-addressed by git; a session needs `PROVENANCE.md`, not the ER7) and a pointer to ADR-0011.

Verification (required, in-session, before commit):

```
make pack
grep -c "FILE: ./test/fixtures/v2/simhospital/messages.out" <pack output file>   # must be 0
grep -c "FILE: ./test/fixtures/v2/simhospital/PROVENANCE.md" <pack output file>  # must be 1
grep -c "FILE: ./test/fixtures/v2/simhospital/LICENSE" <pack output file>        # must be 1
make pack-skills
grep -c "simhospital" <skills pack output file>                                  # must be 0
grep "^elides:" <pack output file>                                               # names all three elisions
```

Also confirm the pack output files are already gitignored (they should be); if `git status` shows them untracked-and-unignored, stop — that is a pre-existing defect worth reporting, not silently fixing.

Stop-tripwire: if any verification grep count differs, do not adjust the greps — the recipe is wrong; fix the recipe or stop and report.

Commit: `make pack: elide vendored corpus bytes (provenance stays packed); pack-skills pattern decoupled`

## Step 2 — Errata convention; first instance

1. At the very top of `docs/research/HL7v2-sanitized-corpus-research.md` (above the title or immediately below it, whichever the file's structure makes cleaner), insert a clearly delimited block:

```markdown
> **Errata (2026-07-26).** This is a dated research record; its body
> is not rewritten. Corrections established during adoption
> (`.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md`)
> live in `notes/facts-register.md` and supersede the body where they
> conflict:
>
> * **Framing** — "segment delimiter CR" is correct but incomplete:
>   segments are CR-terminated *within* a message, messages are
>   separated by a blank LF line, and the final segment of each
>   message carries no CR. See **F25**.
> * **`custom-exporter-template` license** — stated flatly as
>   Apache-2.0 in the body; the repository carries no LICENSE
>   artifact and GitHub's license API returns 404 — the grant exists
>   only in its README ("Copyright 2023-2025 The MITRE Corporation").
>   Recorded as `license-stated-in-README`. See **F28**.
> * **Archive date** — the 2025-03-28 archive date is cited from
>   secondary reporting and is not independently confirmable via the
>   GitHub API (no `archived_at` field); observed facts are
>   `"archived": true` and last push 2024-03-20. See **F24**.
```

Reproduce the F-row citations' substance from the register rows themselves as committed — if the register's wording differs from the summaries above, the register wins; adjust the errata text, not the register (ruling 4).
2. `docs/README.md`, research/ section (item 8 of the reading walk): two edits in one touch. First, the bullet list of research docs currently omits `HL7v2-sanitized-corpus-research.md` — add it (a gap left by the adoption session, in scope here because this step edits that exact list). Second, one sentence establishing the convention — research documents are dated records; when later evidence refines or corrects one, a dated errata block at the top points to the superseding facts-register rows, and the body stands.

Stop-tripwire: if you find yourself wanting to edit body text to "reduce confusion," that is the convention working as designed telling you no. The errata block is the entire correction surface.

Commit: `Research errata convention established; first instance: HL7v2 corpus research (F24, F25, F28)`

## Step 3 — Session close

1. `make test`, `make coverage` (nothing should move — this session touched no `src/` or `test/` path; if either budges, stop), `make lint-pipeline`, `make lint-deps`.
2. Golden check: `make pipeline && make use-cases && git diff --exit-code docs/pipeline.md docs/use-cases.md docs/signature.edn` — any diff is scope breach, stop.
3. Brief session report: the six grep results from Step 1's verification, verbatim.
4. Move this prompt to `.agents/prompts/archive/`.
5. Final commit, `git push origin` from WSL; read the pre-push hook output; the session ends when the push is confirmed on the remote.

Stop-tripwires, collected

* Step 1: any verification grep off by any amount → recipe defect; fix the recipe or report, never the grep.
* Step 1: pack output files not gitignored → pre-existing defect; report, don't silently fix.
* Step 2: register wording conflicts with this prompt's errata summaries → register wins; adjust errata text only.
* Step 2: urge to edit research-doc body → prohibited; errata block only.
* Step 3: coverage or golden files move → scope breach; stop before further commits.

## Session deviation record (author ruling, 2026-07-26)

Step 1's verification block as issued above included `grep -c "simhospital" <skills pack output file>  # must be 0`. Running it against the actual `pack-skills` output returned **8**, not 0. Investigation traced every hit to prose mentions of "simhospital" inside `.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md` — the corpus-adoption session's own archived prompt, already committed (`f1bbe49`) before this session and correctly included in `pack-skills` under the unchanged `^\.agents/prompts/archive/` pattern. A parallel check, `grep -c "FILE: ./test/fixtures/v2/simhospital" <skills pack output file>`, returned 0: the corpus path itself never enters `pack-skills`.

Presented to the author as a stop-tripwire per the instruction above. Ruling: the recipe is correct; the verification command was defective as written — ruling 2's actual invariant is file membership, not a bare substring match, and the substring "simhospital" also legitimately appears in already-included archived-prompt prose. Replacing the check with the membership-scoped version (and adding a positive membership assertion) tests the stated invariant rather than adjusting a check to pass — the distinction the tripwire exists to protect. The corrected commands and their output are recorded verbatim in this session's report. This prompt file itself archives unedited, as issued; the deviation and ruling live in the session report, not in a retroactive edit here.
