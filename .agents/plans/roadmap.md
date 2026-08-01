# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (approved migration, sequenced — .agents/plans/2026-08-01-migration-report.md)
- Item 5: way-of-working → skills distillation (build-session incl. full ceremony,
  capture-session, extraction-stage, errata-sweep, session-prompt) — after item 9
- Item 8: .agents/reading-sets.edn, placeholder budgets = actuals (charter R-D)
- Item 14: use-cases split per review P3-1 (notes/2026-07-30-refactoring-review.md §5.2)

## Next (backlog, no session scheduled)
- components/sim/src bare-ADR-docstring sweep: many bare, mis-qualified
  `ADR-NNNN` references beyond the ones migration session 3's own citation-stubs
  pass flagged and fixed — `engine.clj` alone has 40+ remaining — a dedicated
  sweep, not attempted exhaustively in that session (2026-08-02 finding)
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
- Item 9 (ADR-0024, landed 2026-08-01 as mirror-with-gate, not symlinks): run a
  fresh, non-nested `claude -p`/session and confirm a previously-invisible skill
  now appears in its listing (this session couldn't self-administer that check);
  fast-forward the /mnt/c clone to pick up .claude/skills/ (and the several
  commits it was already behind) — AUTHOR ACTION named 2026-08-01

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
