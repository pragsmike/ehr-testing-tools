2026-07-29 — Pre-takeover storefront polish (agent) + takeover runbook (author)

PART I — Agent session: storefront polish and SETUP re-validation

Context

The workspace at `40492d4` is about to take over the `pragsmike/ehr-testing-tools` repo via fast-forward push (verified: tools' old tip `f848b67` is an ancestor of `main`). The takeover must be a step down for NO audience relative to the old tools storefront. A side-by-side README comparison found four concrete regressions and one re-validation debt; this session fixes them as the LAST commits before the takeover push. Write all names and URLs for the DESTINATION (`ehr-testing-tools`) — they will be briefly wrong on the interim repo and correct the moment the fast-forward lands; the interim repo is retiring anyway.

Autonomous, per R30 ritual: commit+push per checkpoint, staging hygiene, session record, facts-register entries, archive prompt at start of close.

Author rulings

R40 [A] The SETUP on-ramp previously earned a documented success report from a domain expert (a Python developer) against the old tools repo. That report is the bar: the re-validated walk must be at least that good. The old report is preserved (or cited from provenance) as evidence lineage; the new walk's record supersedes it as the live claim.

R41 [C] The re-validation reader is a FRESH agent context given ONLY the rendered README and the instruction "get to a generated, judged corpus on this machine" — no repo knowledge, no memory of this project. Friction points are findings; the walk's transcript summary goes in the session record.

R42 [C] Storefront claims keep the workspace's evidence norm: every Maturity-table Evidence cell links to its receipt (EXP record, test namespace, or ADR). No unlinked "proven."

Steps

1. README parity fixes (the four found regressions, one commit):
   a. H1 and self-name → `ehr-testing-tools` (destination-named).
   b. CI badge for `pragsmike/ehr-testing-tools/actions/workflows/test.yml` — note the workflow FILENAME is `test.yml`, not the old repo's `ci.yml`; a 404 badge is worse than none.
   c. Restore the persona sentence: interface analysts, QA engineers, data engineers — not necessarily Clojure programmers (adapt the old README's wording; it earned its keep).
   d. Evidence links per R42, and surface the SETUP.md AI-assistant copy-paste on-ramp in the first screenful, as the old README did.
   COMMIT/PUSH `docs: storefront parity -- badge, personas, evidence links, SETUP on-ramp surfaced`

2. Self-reference sweep: every live-doc URL of `github.com/pragsmike/ehr-testing` → `.../ehr-testing-tools` (README, docs/, dev docs, workflow comments). Frozen provenance and ADR records keep their historical URLs — citations, not voice. Grep-verified zero live-doc hits of the interim URL.
   COMMIT/PUSH `docs: self-references point at the surviving repo`

3. Storefront parity audit: `notes/storefront-parity-audit.md` — rows are positioning.md's seven audiences (practitioners row split to name domain experts/informaticists); columns are click-depth from rendered README to: why-care, first runnable command, full command reference, evidence, LICENSE/NOTICE, contributing. Fill for OLD tools README (from `stable-pre-monorepo`) and NEW. Rule: no cell worsens. Any worsened cell → fix now if one commit's work, else record as a named finding with the author's decision required BEFORE takeover (list at session end).
   COMMIT/PUSH `docs: storefront parity audit -- no audience steps down`

4. SETUP re-validation walk (R40/R41): run the fresh-context walk. Fix what it finds in SETUP.md/quickstart (friction fixes only — no redesign). Record: old report cited, new walk result, time-to-first-corpus if measurable. Facts-register entry: the on-ramp claim, its evidence, its date.
   COMMIT/PUSH `docs: SETUP re-validated against the prior domain-expert bar`

5. Close: archive prompt (first), session record, final `poly check` + per-push lane + ci-parity, report any before-takeover decision items from step 3.
   COMMIT/PUSH `docs: pre-takeover session record and archived prompt`

PART II — Author runbook: the takeover (your ceremony, in order)

T1. Pre-flight on `ehr-testing-tools` (GitHub settings):
    - Branch protection on `main`: must permit your fast-forward push (relax if needed; restore after).
    - Actions: enabled.

T2. From the workspace clone:
```
git remote add tools git@github.com:pragsmike/ehr-testing-tools.git
git push tools main:main # fast-forward, non-destructive
git push tools --tags
```

T3. Watch the first Actions run on ehr-testing-tools → green; README badge renders green.

T4. About block on ehr-testing-tools: description (one sentence, the README's pitch), topics (hl7, fhir, ehr, test-data, clojure, synthetic-data …), website (guide or docs), social preview (logo.png). This lives outside git — checklist, not commit.

T5. Retire `ehr-testing-sim`: final commit replacing README with a pointer ("continues as part of ehr-testing-tools"), then Archive (public, read-only banner) — or Private if you prefer it off the profile; history is preserved inside the survivor either way.

T6. Retire interim `ehr-testing`: same pointer-then-archive, or delete (days old, no audience).

T7. Local clones: `git remote set-url origin git@github.com:pragsmike/ehr-testing-tools.git`.

T8. Tag ceremony, on the survivor, after green CI: `git tag stable-ehrt-1 && git push tools stable-ehrt-1` (name is yours; this tag certifies the takeover state and becomes the incremental-test baseline).

T9. Tell the design channel it's done — the doer-checker pass runs against the surviving repo.

## Deviation record (dated, self-archived per this workspace's own convention)

Applied as given; two departures worth recording rather than silently absorbing:

**R40's cited "documented success report" could not be located verbatim.** This workspace's own `.agents/session-records/` discipline is sim-only in origin (`notes/discipline-parity-audit.md` row M13) — tools-era sessions never committed a session report to git, only the session prompt and the commits it produced. The closest live provenance is `notes/tools/prompts/2026-07-24-onboarding-wave.md`'s own Step 4 ("Legibility self-audit," a same-session, cite-your-source acceptance test, not an independent trial) and the three commits it produced (`dd6ba2d`, `09eb094`, `f4e5c76`). Cited from that provenance instead of the report itself, per AGENTS.md's "record it and ask" discipline — recorded here and in `notes/facts-register.md` F3, not blocked on (autonomous session, no author present to ask).

**Step 3's audit found one cell that actually worsened** (Evaluator row, Scope, root README 0→1 clicks after the audience-fork README rewrite moved Scope to `docs/what-is-this.md`). Fixed in the same commit as the audit, per step 3's own rule (one-commit-fixable worsened cells get fixed, not deferred) — a short inline `## Scope` section restored to root `README.md`, pointing at the fuller doc for depth. One softer finding (F-G1, signposting rather than depth) carried forward as the session's own single before-takeover decision item — see `notes/storefront-parity-audit.md`.

Everything else in Part I executed as specified; Part II is the author's own subsequent ceremony, not executed by this session.
