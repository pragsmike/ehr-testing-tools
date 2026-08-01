# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (approved migration, sequenced — .agents/plans/2026-08-01-migration-report.md)
- (none — the fourteen-item migration report is fully executed as of
  migration session 6; see Done below)

## Next (backlog, no session scheduled)
- Pairing-as-data (review P3-3): mutate↔judge conviction registry — design pass in
  the design channel first; vocabulary is load-bearing
- Storefront demo fixture: minimal clean-gating FHIR fixture so the README's mutate
  demo shows a real accepted→rejected flip (2026-08-01 capture session finding)
- make quickstart → nightly integration workflow + single-```sh-fence guard in README
  (quickstart_fresh docstring corrected in same change)
- generator-source three-concerns split (ADR-0017 named-future)
- ehrt.corpus.display placement — presentation-leaning (ADR-0018 named-future)
- Markdown-table helper dedup (ADR-0018 named-future)

## Externals (author-only)
- NIST licensing inquiry: send the drafted gist (retires the confirmation-pending
  posture cited on the storefront Gate row)
- IG pinning: choose and commit the profile-tier conformance target (Gate row's
  other caveat)
- Clojars publish, when satisfied with the product (ruled 2026-07-31; ends the
  greenfield era — output formats freeze harder after first tag)
- SETUP rewalk by an unspoiled human reader (F3 superseded-pending-rewalk)
- Upstream the adapted repo-adaptation skill to pragsmike/skills (and cyberneutics
  if wanted) — AUTHOR ACTION named 2026-08-01
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): the
  fresh-session discovery probe is DONE — see Done section below. Remaining:
  fast-forward the /mnt/c clone to origin/main (several commits behind,
  including .claude/skills/) — AUTHOR ACTION named 2026-08-01

## Deferred (explicitly, with revisit triggers)
- P2-5 intake staging-dir behavior (deferred 2026-07-31)
- Reading-set budget numbers (charter §6: rule after real sizes are measured)
- Verdict-cache placement revisit (ADR-0011 note: second consumer, or never)
- Sim-manifest interop design between sim and corpus (pre-review open thread)

## Done (this session, 2026-08-01, migration session 1)
- Items 6+7: `agent/scenario-roster.md` merged into `.agents/skills/scenarios/roster.md`,
  `agent/` (singular) retired (47c815c)
- Item 13: `.agents/plans/roadmap.md` (this file) lands from the design-channel
  ledger handover (47c815c)
- Item 9: `.claude/skills/` mirror-with-gate lands (ADR-0024); end-to-end proof
  and the /mnt/c fast-forward are AUTHOR ACTION, see Externals above (a9e5be6,
  8df3cf3)

## Done (this session, 2026-08-02, migration session 2)
- Items 1+12: `notes/prompts/` sealed — `ehrt.docs-tooling.notes-prompts-frozen-test`
  pins the 29-file set, `stale_path_test.clj` gains the archive-instruction
  tripwire (both red→green live-proven) (6c3c494)
- Items 4+11: `notes/README.md` lands (six top-level files + three subdirs
  indexed, zone-marked); `.agents/skills/README.md` plus all 10 skill-directory
  READMEs land (mirrored to `.claude/skills/`); `ehrt.docs-tooling.readme-presence-test`
  enforces both trees going forward, `notes/sim/`/`notes/tools/` exempt
  (ruling 6) (ab9fe5e)

## Done (this session, 2026-08-02, migration session 3)
- Item 10: `ehrt.docs-tooling.index-completeness-test` lands — both directions,
  over `.agents/plans/`, `.agents/prompts/`, `.agents/session-records/`,
  `.agents/skills/`, `notes/`; `notes/prompts/` convention-exempt; ruling 6
  extended to completeness (77880f7)
- Item 3(a): sim citation-stubs pass, reading (a) — 8 F-rows and 10 ADRs cited
  at their live restatement site (4 were miscitations, fixed not just
  supplemented); `notes/facts-register.md` F20 stub names the two-file
  topology; full accounting in the session record (54ab3b6)

## Done (this session, 2026-08-02, migration session 4)
- Item 8: `.agents/reading-sets.edn` lands — five named sets, each path
  justified inline, gated by `ehrt.docs-tooling.reading-set-budget-test`
  (ghost-path + budget checks, both red→green live-proven); every
  `:budget-lines` is this session's own measured actual, corrected once
  more for this section's own weight (see the session record's "it
  caught itself twice" note): `:onboarding` 538, `:corpus` 1519, `:sim`
  574, `:judge` 644, `:docs` 433 — the baseline the author's future
  budget ruling (charter §6) now has a real number to cite (ab679c9)
- `components/sim/src`/`test` bare-`ADR-NNNN` docstring sweep (the "Next"
  backlog item migration session 3 named): 151 bare references across 39
  files classified and requalified — 149 to `sim/ADR-NNNN`, 1 to
  `tools/ADR-0015` (a cross-repo miscitation this sweep discovered), 1
  left deliberately bare (already correctly cites the live register);
  two further wrong-file-path miscitations also fixed; docstring/comment/
  fixture-remark edits only, zero behavior change; full one-to-one
  accounting in the session record (72f5542)

## Done (this session, 2026-08-02, migration session 5)
- Item 5: way-of-working session mechanics distilled into five repo-local
  skills -- `build-session`, `capture-session`, `extraction-stage`,
  `errata-sweep`, `session-prompt` -- each citing its own provenance in
  AGENTS.md/AUTHORS-GUIDE.md/notes/ADRs.md/named session records;
  mirrored to `.claude/skills/`, indexed in both READMEs, all three
  affected gates (readme-presence, index-completeness,
  skill-mirror-currency) green (60b9f87)
- Item 9's fresh-session discovery probe: CONFIRMED 2026-08-02 (external
  observation, no commit) -- a fresh, non-nested Claude Code session's
  own Skill listing showed `wsl-windows-git-hygiene` with its full
  description, closing the acceptance test migration session 1 could
  not self-administer. The paired AUTHOR ACTION (fast-forward `/mnt/c`)
  remains open, see Externals above.
- `build-session` added to all five `.agents/reading-sets.edn` sets
  (AR-3: ceremony applies regardless of task class); `session-prompt`
  named in `:onboarding`'s own comments for design-channel
  prompt-authoring sessions, not added as a budgeted path;
  `capture-session`/`extraction-stage`/`errata-sweep` stay excluded from
  every set, same reason the existing ten skills are (session-type, not
  domain-task-class). Budget deltas in the session record and
  `notes/ADRs.md` ADR-0023's own dated-note thread.

## Done (this session, 2026-08-02, migration session 6)
- Item 14 (last open item, migration report fully executed): use-cases
  split — `docs/use-cases.md` is now a generated index (one line per
  case, linking out), `docs/use-cases/<id>.md` is one standalone page
  per case, both from `components/corpus/docs/use-cases.edn` unchanged
  except for the internal cross-case and reference-doc links the file
  split itself requires (never a case's own narrative/strip/equations
  text). Content conservation proven one-to-one against the prior
  single-file rendering (script-diffed per case, modulo heading/banner/
  link-depth scaffolding); the CI freshness gate
  (`.github/workflows/test.yml`) now diffs `docs/use-cases/` alongside
  the index. Every repo citation of `docs/use-cases.md#<case>` swept to
  its per-case file. Full accounting in the session record (ceca0f7,
  plus this checkpoint's own commit).
