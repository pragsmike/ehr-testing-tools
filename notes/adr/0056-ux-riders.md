## ADR-0056 — UX riders: the arc opens — brief lands, tags licensed, the compaction pointers come home

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the alignment arc closed and was design-channel-verified
(`12d3aa3`, `notes/adr/0055-alignment-arc-close.md`). A real
first-contact CLI failure on 2026-08-06 opened the UX arc — a user
ran a design-channel-supplied command from the demo docs' invocation
form and hit a three-failure cascade (stale alias, opaque error,
agent-voice help). Its brief, `.agents/plans/2026-08-06-ux-arc-brief.md`,
was placed in the working tree by the author. This is the arc's small
opening session: land the brief, execute the alignment arc's two
pending tags (licensed, not executed — see below), and rotate the
leftover scaffolding-compaction Done pointers the alignment close
disclosed but did not sweep. Docs and tags only; no `src/`, no
help-text, no error-message changes land here (AR-U0-4) — the UX
audit session owns the survey. R30 ceremony.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-U0-1 (brief lands, verified not trusted).** The brief lands with
its `.agents/plans/README.md` index entry. Before landing, re-probe
its probeable claims: no `:cli` alias in root `deps.edn` (nor in
`components/sim/deps.edn`); the demo READMEs' `clojure -M:cli` fences
exist as described (enumerate the files — the count goes in
ADR-0056); `bin/ehrt`'s cd-to-root behavior per its own header. A
claim that fails probe is corrected in the brief with a dated note,
never silently kept or dropped.

**AR-U0-2 (the two pending tags — R1).** Per the reconciled mechanic:
(a) annotated tag `stable-20260805-alignment-fixes-5` at `2b3bb2b`,
message `alignment fixes 5 landed, design-channel-verified 2026-08-05
(ADR-0054)`; (b) annotated tag `stable-20260805-alignment-close` at
`12d3aa3`, message `alignment arc closed, design-channel-verified
2026-08-06 (ADR-0055)`. Both pushed, both verified on origin with
peeled refs resolving to the named commits. If either already exists
at the exact commit/message (the author may have tagged directly),
verify and disclose rather than re-create — the fixes-2 precedent.

**AR-U0-3 (compaction pointers rotate — R2).** The three Done pointers
(scaffolding-compaction-a/b/c, ADR-0045/0046/0047) relocate verbatim
from the roadmap's Done section to `.agents/plans/roadmap-done-2026-08.md`
under a dated header (`## Scaffolding-compaction arc — closed
2026-08-05, rotated 2026-08-06 (ADR-0045–0047; rotation deferred at
its own close, disclosed in ADR-0055)`), placed BEFORE the
alignment-arc section so the attic reads chronologically.
Relocation-not-rewrite. The Done section afterward holds exactly two
entries: ADR-0055's pointer and, once Step 2 lands it, ADR-0056's.

**AR-U0-4 (nothing else).** No UX fixes of any kind land this
session — not one help string, not one error message, however small
the temptation. The audit session owns the survey; this session opens
the arc, only.

### Execution record

**Step 0 — preflight + probes.** Working directory confirmed
`~/src/ehr-testing-tools` (ext4, `df -T` reports `ext4`); tip `12d3aa3`
exactly; working tree clean apart from the brief itself (untracked,
placed by the author) and one unrelated untracked file
(`config/busy-weekday.md`, left alone — not this session's own). AR-U0-1's
three probes, tabled:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | No `:cli` alias in root `deps.edn` or `components/sim/deps.edn` | `grep -n ':cli' deps.edn`; direct read of `components/sim/deps.edn`'s own `:aliases` map | **HELD.** Root `deps.edn` carries no `:cli` alias; `components/sim/deps.edn` declares only `:test`. The brief's claim stands unchanged. |
| 2 | Demo READMEs teach the stale `clojure -M:cli run ...` form | `grep -rl "clojure -M:cli" components/sim/docs/demos/ --include="README.md"` | **HELD, enumerated.** Six README.md files carry the stale form: `components/sim/docs/demos/README.md`, `boarding-transfer/README.md`, `emit-state/README.md`, `order-result/README.md`, `module-mix/README.md`, `persona-enriched/README.md`. Supplementary, not claimed by the brief: two demo `config.edn` files (`order-result/config.edn`, `module-mix/config.edn`) also carry the same stale form in a header comment — a finding for the audit session's own U1 row, not a correction to this brief (the brief's own claim was scoped to READMEs and holds exactly as stated). |
| 3 | `bin/ehrt` cd's to the workspace root so cwd never matters | Direct read of `bin/ehrt`'s own header comment and its `cd -- "$repo_root"` line | **HELD.** The header states exactly this rationale; the script's third executable line performs the `cd` before anything else runs. |

All three claims held; no dated correction owed to the brief.

Baseline: `clojure -M:poly check`: OK. Full suite (`clojure -M:poly
test :all skip:integration`): 216 `Test results:` lines, 0
`FAIL`/`ERROR`/`Exception` anywhere. `gitleaks detect -v`: 667 commits
scanned, no leaks. Oracle pre-digest (`bin/regression-oracle 12d3aa3
12d3aa3`): all eleven roots IDENTICAL, soundness "yes outside ns
form" — the harness confirmed sound before this session's own changes
land.

**AR-U0-2 — the two tags (AUTHOR ACTION, not executed).** Neither tag
exists yet: `git tag -l 'stable-*'` lists no `alignment-fixes-5` or
`alignment-close` tag, and `git ls-remote --tags origin | grep -E
'alignment-fixes-5|alignment-close'` returns nothing — the fixes-2
verify-and-disclose shortcut does not apply here; both remain fully
pending. Per this workspace's standing ceremony law (`AGENTS.md`,
`AUTHORS-GUIDE.md` §1, ADR-0055's own AR-AC-0 precedent), tags are
AUTHOR ACTION in every ceremony mode, including R30 — this session
licenses them, does not create them. Exact commands for the author, in
sequence (the second is licensed only after the first lands and is
verified, per the standing after-landing sequence):

```sh
git tag -a stable-20260805-alignment-fixes-5 2b3bb2b \
  -m "alignment fixes 5 landed, design-channel-verified 2026-08-05 (ADR-0054)"
git push origin stable-20260805-alignment-fixes-5
git ls-remote --tags origin | grep alignment-fixes-5
```

```sh
git tag -a stable-20260805-alignment-close 12d3aa3 \
  -m "alignment arc closed, design-channel-verified 2026-08-06 (ADR-0055)"
git push origin stable-20260805-alignment-close
git ls-remote --tags origin | grep alignment-close
```

**Step 1 — brief + rotation (AR-U0-1/AR-U0-3).** The brief landed
verbatim (114 lines, non-empty) with its `.agents/plans/README.md`
index entry. The rotation: the three scaffolding-compaction Done
pointers relocated verbatim from the live roadmap's own Done section
to `.agents/plans/roadmap-done-2026-08.md`, under the new header named
in AR-U0-3, placed immediately before the existing `## Alignment arc —
closed 2026-08-05 (ADR-0048–0055)` header so the attic reads
chronologically.

Rotation before/after, the live roadmap's own Done section:

*Before:*
```
- 2026-08-05 — scaffolding-compaction-a — ADR-0045
- 2026-08-05 — scaffolding-compaction-b — ADR-0046
- 2026-08-05 — scaffolding-compaction-c — ADR-0047
- 2026-08-05 — alignment-arc-close — ADR-0055
```

*After (Step 1; Step 2 below adds this ADR's own pointer):*
```
- 2026-08-05 — alignment-arc-close — ADR-0055
```

One commit, both edits (both `.agents/plans/` hygiene): `2991a70`
("docs: the UX arc opens — its brief lands, the compaction pointers
come home (ux riders, AR-U0-1/3)"). Staging hygiene: `git diff
--cached --stat` reviewed before commit — exactly the four files in
scope (`2026-08-06-ux-arc-brief.md`, `.agents/plans/README.md`,
`roadmap-done-2026-08.md`, `roadmap.md`); `config/busy-weekday.md`
confirmed NOT staged (unrelated pre-existing untracked file, left
alone). `gitleaks git --staged -v`: clean. Full suite re-confirmed
green post-edit before push: 216 `Test results:` lines, 0
`FAIL`/`ERROR`/`Exception` (rerun with the raw log captured this time,
ANSI-code-tolerant grep — the first pass's `tail`-only capture had
already shown the same shape). Pushed; post-push verification: one
delta against the message file, the known harmless trailing-newline
artifact.

**Step 2 — this record.** `notes/ADRs.md` gains this ADR's own index
line; `notes/adr/README.md`'s own file count corrected 53→54 ("as of
ADR-0056"), the same staleness-at-count-instant pattern named at every
prior ADR landing. The Done pointer `- 2026-08-06 — ux-riders —
ADR-0056` lands in the live roadmap in the same commit as the index
line (so `ehrt.docs-tooling.done-pointer-adr-test`'s dangling-reference
gate never sees a pointer citing an ADR number not yet indexed) —
final Done-section shape:
```
- 2026-08-05 — alignment-arc-close — ADR-0055
- 2026-08-06 — ux-riders — ADR-0056
```
Session record (`.agents/session-records/2026-08-06-ux-riders.md`) and
this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-riders.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md`
in the same commit.

### Verification

- `bin/regression-oracle 12d3aa3 2991a70` (baseline: this session's own
  pre-session tip; target: Step 1's own commit, the tip immediately
  before this record's own closing commit — no `src/` touched at any
  point this session, including Step 2): `IDENTICAL: every root's
  digest matches between 12d3aa3 and 2991a70` — all eleven
  vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  expected for a docs-and-tags-only session; no `--declared-digest-change`
  licensed or needed. Any change would be STOP-AND-ESCALATE per this
  session's own prompt.
- Full suite (`clojure -M:poly test :all skip:integration`): green
  throughout, 216 `Test results:` lines, 0 failures/0 errors, matching
  Step 0's own baseline shape exactly at every checkpoint — the
  index-completeness and done-pointer-adr gates hold through the
  rotation.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history): clean at Step 0 baseline.
- Post-push message verification: one delta against the message file
  at Step 1, the known harmless trailing-newline artifact.
- AUTHOR ACTION items (both tags, AR-U0-2) named above with exact
  commands, licensed, not executed by this session.

### Fences (standing law applies unchanged, this session's own prompt)

Move-don't-improve; the brief outranks this prompt's summaries of it;
evidence outranks every voice in the brief. Frozen archives untouched.
No `src/`, no `test/`, no config, no gates, no `help.clj`, no error
strings (AR-U0-4). After landing: design channel verifies by fresh
probe, then drafts the UX audit session against the landed brief; this
landing's own tags ride that session's Step 0.
