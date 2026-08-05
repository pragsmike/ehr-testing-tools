<!-- Attic file: notes/adr/0006-discipline-parity-restored.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0006 — Discipline parity restored: guides, live registers, sweep completion

**Status:** Accepted (author-directed, commits delegated for this
session), 2026-07-28.

### Context

A review pass against the public `ehr-testing-tools` clone found the
workspace's discipline apparatus — guides, registers, `.agents/`
substrate — below sim's and tools' combined peak strength: several
mechanisms each parent carried were never unioned in, the reserved
`ADR-0003` slot sat empty, `.claude/` was untracked but not gitignored,
docsgen regen tooling was disconnected, and (found only once this
session's own audit looked) root `test/`/`test-integration/` fixtures
contradicted a claim about their own disposition that, on direct
re-reading, ADR-0002 never actually made. This record closes that pass.

### Decision

**R24–R29** (full text: this session's own prompt,
`notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`), summarized
by what each produced:

- **R24** (union method) → `notes/discipline-parity-audit.md`: 24
  mechanism rows (M1–M24), every one dispositioned ADOPT/ADAPT/RETIRE,
  zero UNDECIDED at gate time.
- **R25** (live infrastructure) → `notes/facts-register.md` (fresh
  sequence, F1 carried forward from ADR-0005 with origin citation),
  `.agents/{memory,plans,session-records}/` (README-stub contracts,
  empty substrate — `.agents/handoffs/` deliberately NOT instantiated,
  audit row M14, this workspace's checkpoint model reduces the need
  sim's and tools' async multi-session model had for it).
- **R26** (doctrine promotion) → `AUTHORS-GUIDE.md` §7, five lessons
  (index-not-tree, local-state-not-clone-state, cwd=workspace-root,
  superseded-needs-inventory, poly-enforced-dependency-direction), each
  citing the ADR it was mined from. Landed inside the guide-union
  commit (`a48aae6`) rather than its own — see the deviation record.
- **R27** (pre-push doctrine) → ADR-0003, filled into its reserved
  slot; hook header rewritten to match; dry-run verified against the
  real hook.
- **R28** (fixture ownership) → fixtures relocated to
  `components/tools/test-fixtures/`/`projects/conformance/test-fixtures/`;
  cwd-relative literal paths **kept**, not converted to `io/resource` —
  re-examined against a wider call-site survey than ADR-0002's own
  (roughly a third of the 11 consuming test files exercise real
  CLI path-handling, not fixture lookup, and would break under a
  classpath-resource conversion regardless of brick placement); ADR-0002
  erratum written correcting a mischaracterization of what that ADR had
  claimed, not a mischaracterization the ADR itself made.
- **R29** (top-level tidy) → `doc/` merged into `docs/`;
  `notes/tools/agent/` (singular, one file) merged into
  `notes/tools/agents/` (plural); four deliberate root residents
  (`bin/`, `config/`, `resources/synthea-default.properties`,
  `artifacts.lock.edn`) recorded in `notes/carve-loss-audit.md` as
  accepted warts, three with a named exit plan and one likely
  permanent.

**The audit as method, restated for citation** (AUTHORS-GUIDE.md §7d):
this session's own step 1 is the second time this workspace has run a
full mechanism/path inventory before touching anything (the first,
`notes/carve-loss-audit.md`, ADR-0004) — both times, the inventory
surfaced real findings a narrower, targeted look would have missed
(this session: the scenario-roster live-operational gap, the missing
`.gitattributes` `-text` overrides, the broken `tools/ADR-0011` link,
the stale pre-carve namespace path in `PROVENANCE.md`). Recorded as a
repeatable method, not a one-off.

### The two-thirds-strength assessment, answered

The session that requested this record characterized the workspace's
discipline apparatus as running below sim's peak strength. What this
session actually closes: every artifact-level gap the audit found now
has a disposition and, where ADOPT/ADAPT, a landed fix. What it does
**not** close, honestly: parity of *artifacts* is not parity of
*practice*. A live facts register, a session-records directory, and a
staging-hygiene ritual are apparatus — whether they get used
correctly, unprompted, by the next session that doesn't have this
session's prompt telling it to, is a separate and harder claim this
record cannot make on its own evidence. The author's own post-session
action A4 (below) names the actual test.

### Deviation record

**Precondition stanza (step 0).** Clean tree, `HEAD == origin/main`
(`cc8f5e9`), per-push CI green on `HEAD` (run `30417940625`, verified
via `gh run list`), `clojure -M:poly check` green locally (WSL,
`openjdk 21.0.7`, `poly 0.3.32`) — all four confirmed before any file
was touched.

**Disposition-table counts.** 24 rows (M1–M24), 0 UNDECIDED at gate
time (step 2 passed without an author stop).

**Commit-boundary slip, self-caught (step 3→5 seam).** The R26 doctrine
section (step 5's own content) was written directly into the guide-union
commit (`a48aae6`) instead of its own later commit — noticed only after
the fact, while drafting this record, not caught by the staging-hygiene
ritual in real time despite that ritual being written in the very same
commit that violated it. Not corrected by amending `a48aae6` (this
repo's own no-rewrite discipline, `AUTHORS-GUIDE.md` §1's "create a NEW
commit" convention) — recorded here as the actual, honest account:
step 5 produced no commit of its own because there was nothing left to
commit by the time it was reached. The ritual's own real test is
whether the *next* session's commits stay in bounds, not whether this
one's retrospective diagnosis was instant.

**R27's "connection-close incident" — evidence gap, disclosed.** ADR-0003
cites this as R27's stated motive; this workspace's own tree carries no
record of it beyond the terse commit `1ebf4ce "Don't run tests on
pre-push."` itself. Recorded precisely as evidenced, not embellished —
see ADR-0003's own "Honest evidence note."

**The old sweep prompt could not be archived as written — it was never
written.** `notes/ADRs.md` ADR-0004 already disclosed this (the session
that would have authored it stopped at its own step 0). Step 6e's
instruction to archive it with a superseded-by note was executed as a
placeholder file explaining the gap
(`notes/prompts/2026-07-28-ehr-testing-h2-closeout-sweep.md`), not a
fabricated prompt body.

**ADR-0002 mischaracterization, corrected in the erratum, not in this
record.** This session's own prompt described ADR-0002 as having
claimed a completed `io/resource` conversion. Re-read directly, it
never did — the erratum (ADR-0002, above) corrects the *characterization*
of the record, not the record's own content, which needed no
correction on this point.

**Two operational gaps found and fixed, neither named in the session's
own prompt.** (1) `agent/scenario-roster.md` — the live path
`scenarios`/`probe` need — was missing from the entire tree even after
ADR-0005's skills union; restored from frozen provenance. (2)
`.gitattributes` never carried tools' own pre-carve `-text` overrides
for the v2 HL7 fixtures; added at the fixtures' new location, corpus
bytes verified intact (sha256 match) before the gap was closed.

**Verification, full chain.** `clojure -M:poly check`: green, twice
(after the sweep commit, after the tidy commit). `clojure -M:poly test
:all skip:integration`: 0 failures / 0 errors, twice (after the
interface/docsgen changes, after the fixture relocation) — the second
run is the empirical confirmation that cwd-relative fixture resolution
genuinely works across brick boundaries (`components/tools`,
`bases/ehr-cli`, `projects/conformance`), not merely assumed from
reading `deps.edn`. `make ci-parity`: green, fresh clone, cold artifact
cache, run against the committed fixture-relocation state specifically
(R28's own stated bar — this is exactly the class of change where
working-tree green can lie). Final grep sweep: zero stale
`test/fixtures`/`test-integration/fixtures`/`doc/`-path hits outside
frozen provenance, archived prompts, and this record's own erratum
text.

Self-archived to `notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`.

---

