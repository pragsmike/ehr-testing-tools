# 2026-09-08 -- nightly wiring: integration.yml runs `make integration`

Session prompt, archived verbatim as driven. Record:
[`../session-records/2026-09-08-nightly-wiring.md`](../session-records/2026-09-08-nightly-wiring.md).

---

Nightly wiring: integration.yml runs make integration -- 2026-09-08

Context. Since ADR-0120 (2026-08-12) the nightly/dispatch job
.github/workflows/integration.yml has invoked `clojure -M:poly test
:all project:integration` directly, so none of the ten exerciser
scripts that `make integration` runs (Makefile:74-86) has ever run in
CI, while each script's header claims "integration-tier only (`make
integration`)". ADR-0120:115-129 left the wiring "for a future session
to decide"; the author has ruled it. One file changes. The exercisers
are OUT OF FENCE: a runner-side failure in one is a finding to report,
not a script to patch here. Before starting, confirm tip 4cb76175's own
per-push run is green (`gh run list`) -- it postdates the last marker.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
.github/workflows/integration.yml (89 lines: header :1-30, steps
:85-89); Makefile:66-86; notes/adr/0120-manual-s2-exerciser-and-
chapter3.md:115-129 and :270-276; bin/demo-exerciser-dense-7500:1-24
and :262-275 (tree-clean postcondition; the figures.edn :asserted
rewrite IS the assertion); demos/scenarios/dense-7500/figures.edn:1-35
and :49-55 (7,500 cell: 106 s wall, 1.9 GB peak RSS on penny; a runner
will be slower -- a wall is not a gate).

Author rulings, binding:
- Nightly wiring (a), channel ruling, restated here because it is not
  yet tree-recorded: integration.yml's test step becomes `make
  integration`; the Makefile is the single definition of the
  integration tier; the per-push lane (test.yml) admits no exerciser.
- R-move-not-improve: one file. No exerciser, Makefile, or AGENTS.md
  edit; no header "improvements" beyond the truth-correction below.
- R-sentinel: read `gh run view <id>` yourself; no sleep waiters.

Steps.
1. Edit integration.yml: replace :88-89 with a step named
   `make integration` running `make integration`; keep `poly check`
   (:85-86) before it. Rewrite the header's first paragraph (:1-10) to
   say what the job now runs -- the integration suite under
   out/test-tmp (4cb76175's tmpdir discipline rides in) plus the ten
   exercisers -- and that this closes ADR-0120's scope note. Keep the
   ENF-1 stop-clause and cold-cache paragraphs verbatim. Add one
   sentence: the three primed artifacts serve the suite only; the
   exercisers are sim-only, and gov.nist already resolves from
   hit-nexus for project:integration, so the job gains no new network.
   Invariant: `git diff --stat` shows exactly one file; test.yml
   untouched. Gate: `make integration` green locally on a clean tree
   (exercisers refuse a dirty one -- stage nothing else first).
   Commit: "ci: integration.yml runs make integration -- the ten
   exercisers reach the nightly job (closes ADR-0120's scope note)"
2. Push; confirm the per-push run green. Then `gh workflow run
   integration.yml`, record the run id, and poll `gh run view <id>`
   to conclusion. Warm artifact cache is acceptable for THIS proof:
   the change under test is the wiring, and no exerciser fetches an
   artifact. Invariant: the log shows the `make integration` step and
   each exerciser's own final summary line. Gate: conclusion success.
   If an exerciser fails on the runner: STOP, paste that step's tail
   into the session record, do not patch -- report the class.
3. Session record .agents/session-records/2026-09-08-nightly-wiring.md
   (archive this prompt): carry the ruling text above into the tree;
   record the dispatch run id and its wall as a dated quotation
   (R-figures-file's own rule for walls). Close-marker commit records
   the per-push CI success sha and the dispatch run id. Push; confirm
   the marker's own run green.
