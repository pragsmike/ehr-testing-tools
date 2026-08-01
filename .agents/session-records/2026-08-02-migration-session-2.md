# 2026-08-02 — Migration session 2: archives seal, indexes land

## Scope

Second build session of the approved migration
(`.agents/plans/2026-08-01-migration-report.md` + its own rulings
block). Executed items 1+12 (the `notes/prompts/` tombstone ratified
2026-08-01 is sealed with a per-push freshness gate) and items 4+11
(the `notes/` index plus the per-directory README-presence gate over
`.agents/` and `notes/`, frozen dirs exempt per ruling 6). Items 3(a),
5, 8, 10, 14 stayed fenced, per this session's own AR-5, for later
sessions. Standing ceremony (R30): WSL ext4 clone, fast-forwarded to
`origin/main` (`1dd98f8`) before work began; `/mnt/c` untouched, per the
dual-clone-hazard memory and this repo's own fence discipline.

## Red→green evidence highlights

Four live red→green cycles, all real (not inspected-only), each
reverted before the relevant checkpoint's commit:

- **`ehrt.docs-tooling.notes-prompts-frozen-test` (item 1a).** Seeded a
  stray file (`notes/prompts/2026-08-02-stray-test-file.md`) — failed,
  citing the exact filename as an unexpected addition. Reverted; green.
- **`stale_path_test.clj`'s archive-instruction addendum (item 12).**
  Seeded a scratch doc (`docs/_redgreen_scratch.md`) reading "Session
  prompts archive to notes/prompts/ from now on." — failed, citing the
  scratch path. Reverted; green. Also confirmed, via the file's own
  inline literal-string tests, that the one real historical reference
  in the corpus (`docs/dev/way-of-working.md`'s past-participle "the
  archived session prompt under `notes/prompts/` once step 12 lands
  it") does NOT trip the tripwire — verified both directions before
  either checkpoint's commit.
- **`ehrt.docs-tooling.readme-presence-test` (item 11).** Seeded an
  empty, README-less directory (`.agents/_redgreen_scratch_dir/`) —
  failed, naming the exact path. Reverted; green.
- **Full suite re-run** after every seed-and-revert cycle and again
  after the AR-4 doc edits (`notes/ADRs.md`, the migration report, the
  roadmap): `ehrt.docs-tooling.{readme-presence,skill-mirror-currency,
  stale-path,notes-prompts-frozen,structure-currency}-test` together —
  **16 tests, 304 assertions, 0 failures** at every green checkpoint.
  `clojure -M:poly check`: `OK` before both C1 and C2.

## Judgment calls and their ratification status

- **AR-1(b) sweep: nothing needed repointing.** Checked
  `docs/dev/way-of-working.md` and `AUTHORS-GUIDE.md` for current-tense
  instruction steering archives to `notes/prompts/`, as instructed.
  Finding: `AUTHORS-GUIDE.md` has zero references to `notes/prompts/`
  at all; `way-of-working.md`'s only reference is the past-participle
  historical narration named above, already correct, left untouched by
  design (AR-1(c)'s own "historical narrative stays" instruction).
  Not a judgment call so much as a verified negative finding — noted
  here since AR-1(b) named it as a thing to check, not assume.
- **Item 12's tripwire shape: verb-tense scoping, not a bare
  path-substring ban.** A literal ban on the string `notes/prompts/`
  would have immediately tripped on `way-of-working.md`'s own
  legitimate historical line — the checkpoint's own text ("historical
  narrative stays") makes clear that's wrong. Designed instead as a
  present-tense/imperative verb-phrase match (`archives? to`,
  `lands? in/at`, `goes? to`) immediately governing `notes/prompts`,
  which the file's own inline test cases (`archive-instruction-pattern-is-actually-caught-test`)
  prove both ways. **Not separately ratified by the author this
  session** (no author present mid-session to ask) — a design judgment
  within AR-1(c)'s own delegated scope, not a rule reversal; flagged
  here for visibility, low-risk (a docs-only tripwire, reversible).
- **Item 12's scan scope widened beyond `docs/**/*.md`.** The migration
  report's own item-12 text names `AGENTS.md`, `docs/**/*.md`, and
  "skill files" as the intended scope; the existing family's other
  checks scan `docs/` only. Read literally and implemented as stated
  (`AGENTS.md` + every `.agents/skills/**/SKILL.md` join the existing
  `docs/` scan for this one new check only, not the family's other
  checks) — confirmed via grep before writing the test that neither
  surface currently carries an instructional hit, so this widening
  landed green on the first run, not by accident.
- **README-presence gate scope: two explicit levels, not a full
  recursive walk.** "Every subdirectory of `.agents/` and `notes/`"
  read as: direct children of `.agents/`, direct children of
  `.agents/skills/` (one extra level, where the real per-skill
  directories live), and direct children of `notes/` — not recursing
  into a skill's own `references/`/`scripts/`/`templates/`/`agents/`
  subdirectories. This matches the migration report's own item-11
  enumeration exactly (18 required directories, 2 exempt) and avoids
  scope creep the report never asked for. Not separately ratified —
  a scoping judgment within AR-3's own delegated scope.
- **Roadmap sha-citation timing**, following the session-1 precedent
  exactly (verified against real git history before writing a line
  that would otherwise be self-referential): a Done-section row citing
  its own commit's sha cannot be authored inside that same commit.
  Item 1+12's row cites C1's sha (`6c3c494`, already known when C2 was
  authored); item 4+11's row was written with C2's own sha
  (`ab9fe5e`) filled in by this record's own commit (C3), the same
  deferred-fill pattern session 1's `1dd98f8` used for item 9's shas.

## Findings and HEAD landed

**README survey at session start (item 11):** 11 of 18 required
directories had no `README.md` — `.agents/skills/` itself and all 10
of its skill subdirectories (`committee`, `find-skills`, `handoff`,
`probe`, `repo-adaptation`, `review`, `scenarios`, `shared-skill-layout`,
`string-diagram`, `wsl-windows-git-hygiene`). The other 7 (`.agents/plans/`,
`.agents/prompts/`, `.agents/session-records/`, `.agents/memory/`,
`notes/prompts/`, plus the 2 ruling-6 exemptions `notes/sim/`/`notes/tools/`)
already had one or were exempt.

**AR-1(b) sweep hits:** none requiring a change — see the judgment-call
entry above; this is itself the finding.

**Post-push message verification:** both checkpoints verified —
`git log --format=%B -1` after each push diffed byte-for-byte (except a
single trailing-newline artifact from the message file's own EOF, not
content) against the `-F` message file that produced it. No mismatch,
no backtick-mangling, no dropped words — the known failure class this
verification step exists to catch did not recur this session.

**HEAD landed:** `ab9fe5e` ("docs: notes/ index lands, README-presence
gate over .agents and notes (frozen dirs exempt) -- migration items
4+11; roadmap and report annotated"), pushed. Full checkpoint shas: C1
(items 1+12) `6c3c494`; C2 (items 4+11 + AR-4 records) `ab9fe5e`. This
record and the prompt archive are C3, landing on the commit produced by
this same checkpoint — cited by this record's own filename per the same
self-reference convention `2026-08-01-migration-session-1.md` and
`2026-08-01-agent-ux-capture.md` both already used.
