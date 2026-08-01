# Roadmap — rolling plan and backlog

Updated by sessions in the same commit as work that changes a row. Successor to the
design channel's chat-resident ledger (retired 2026-08-01). Cite sources; one line
per item; done items move to the bottom of their section with a date and sha.

## Now (approved migration, sequenced — .agents/plans/2026-08-01-migration-report.md)
- Items 1+12: notes/prompts tombstone ratified + tripwire extension (ruled 2026-08-01)
- Item 3(a): sim register citation-stubs pass (citation-only ruled 2026-08-01)
- Items 4+11: notes/ index + README-presence gate (frozen dirs exempt)
- Item 10: index-completeness gate (after 1, 4, 11)
- Item 5: way-of-working → skills distillation (build-session incl. full ceremony,
  capture-session, extraction-stage, errata-sweep, session-prompt) — after item 9
- Item 8: .agents/reading-sets.edn, placeholder budgets = actuals (charter R-D)
- Item 14: use-cases split per review P3-1 (notes/2026-07-30-refactoring-review.md §5.2)

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
