## ADR-0057 — Tag law: the boundary moves to verification, where it always was

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the UX arc opened and was design-channel-verified (`56a2c21`,
`notes/adr/0056-ux-riders.md`). That session deferred AR-U0-2's two
licensed tags, citing the `build-session` skill's own rule #8 — correct
behavior at the time, because the tag law genuinely still said tags
stay author-only in every mode on every surface that session read. The
probe that followed found the actual cause: the tag law lived on FOUR
live surfaces (`AGENTS.md`, `AUTHORS-GUIDE.md`, the `build-session`
skill plus its `.claude/skills/` mirror, `.agents/state.md`) — a fifth,
`.agents/rulings.md`'s own AR-R-2, joined once this session's own fresh
grep ran — and ADR-0051's own 2026-08-05 reconciliation amended exactly
one of them (`AGENTS.md`). Every session since kept obeying the loudest
unamended surface (the skill rule an agent actually reads at session
start), which was correct behavior under contradictory law, not a
mistake to fix in any one session's own scope. The author now rules the
contradiction gone in the session-executes direction: this session
sweeps every surface, lands a gate against the retired phrasing
recurring, and executes the three pending tags under the fixed law.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's own
prompt):

**AR-T-1 (the law, restated once, canonically).** `stable-*` continuity
tags are SESSION ACTS: a session creates and pushes them (i) when its
prompt licenses a specific tag at a specific commit, and (ii) for its
own predecessor's design-channel-verified stable point as standing
ceremony, without bouncing to the author. Deferral of a licensed tag is
now the DEVIATION and requires a disclosed reason. The author may
always tag directly; a pre-existing tag at the exact commit/message is
verified-and-disclosed, never re-created. Release/version tags (`v*`)
remain AUTHOR ACTION — publication itself is author-gated, so its tags
are too. [Author: this carve-out encodes "stable tags flow, releases
stay yours"; strike it in your paste if you want literally all tags
session-executable.] ADR-0003's trust-boundary reasoning is superseded
in scope, not erased: the boundary made sense before the
verification-gated license mechanic existed (ADR-0049 AR-AU-0,
ADR-0051 AR-F2-0); the design channel's landing verification is now the
trust boundary, and the tag is its mechanical consequence. This ADR
records that supersession with both citations.

**AR-T-2 (every surface, one session — the propagation lesson
practiced, third instance recorded).** The canonical law from AR-T-1
lands on ALL surfaces that state tag law, enumerated by fresh grep (at
minimum: `AGENTS.md`'s tag rule, strengthened so execution is the
default and deferral the deviation; `AUTHORS-GUIDE.md` wherever it
states tag/AUTHOR-ACTION law; `.agents/skills/build-session/SKILL.md`
rule #8 rewritten — stable tags move OUT of the author-only list, `gh`
repo mutations/git surgery/external documents stay IN; the
`.claude/skills/build-session/` mirror updated identically,
byte-compared after; `.agents/state.md`'s tag-mechanic claim updated
with this session's probe). `.agents/rulings.md` gains a dated
amendment entry superseding the prior stable-tagging wording, citing
this ADR — appended per fix-forward discipline, not edited in place.
The sweep's surface table (file → old phrasing → new) is below.

**AR-T-3 (the gate — third instance earns mechanism).** New deftest in
the `docs-tooling` gate family: the retired formulations (`"stay
author-only in every mode"` and `"AUTHOR ACTION in every ceremony
mode"`) may not appear in live law surfaces in connection with tags —
encoded as: those phrases are forbidden outright on the enumerated live
surfaces (`AGENTS.md`, `AUTHORS-GUIDE.md`, both skill copies,
`state.md`, `rulings.md`), since after this session nothing true is
stated by them anywhere (non-tag AUTHOR-ACTION items get rephrased by
AR-T-2's rewrite without the retired formulations). Frozen archives,
ADRs, session records, and dated one-shots are OUT of scope — they
quote history. Red→green witnessed: the gate ran against the unswept
tree first (rule #8 tripped it), then sweep + gate landed in ONE
commit, green.

**AR-T-4 (the three tags, under the fixed law).** After AR-T-2's commit
is pushed: (a) `stable-20260805-alignment-fixes-5` at `2b3bb2b`, message
per ADR-0055/0056's prepared command; (b) `stable-20260805-alignment-close`
at `12d3aa3`, message per ADR-0056; (c) `stable-20260806-ux-riders` at
`56a2c21`, message `ux riders landed, design-channel-verified 2026-08-06
(ADR-0056)`. All annotated, pushed, verified on origin with peeled
refs. Pre-existing-at-exact-commit → verify-and-disclose.

### The surface table (AR-T-2)

| # | Surface | Old phrasing (retired) | New phrasing |
|---|---|---|---|
| 1 | `AGENTS.md` | "Two classes of action stay the author's alone under either mode: **tags** ... AUTHOR ACTION checkpoints (git surgery, placing external documents — things only the author does) stay author-only in every mode." | "One class of action stays the author's alone under either mode without exception: repo-level `gh` mutations, git surgery, and placing external documents. `stable-*` continuity tags are a SESSION ACT ... Deferring a licensed tag is now the deviation ... Git surgery and placing external documents stay the author's alone, unchanged, regardless of ceremony mode." |
| 2 | `AUTHORS-GUIDE.md` §1 | "Tags and repo-level `gh` mutations (create/delete/settings/visibility) are never delegated by either mode — see `AGENTS.md`." | "Repo-level `gh` mutations ..., git surgery, and placing external documents are never delegated by either mode. `stable-*` continuity tags ARE delegated, under license ... Every other tag class, release `v*` tags especially, stays undelegated." |
| 3 | `.agents/skills/build-session/SKILL.md` rule #8 | "**AUTHOR ACTION checkpoints stay author-only in every mode** — tags (the `stable-*` tag is the actual trust boundary, ADR-0003), and repo-level `gh` mutations ... Git surgery and placing external documents are AUTHOR ACTION too." | "**`stable-*` tags are a session act, under license; everything else below stays author-only regardless of ceremony mode.** ... deferring a licensed tag is now the deviation ... **Release `v*` tags, repo-level `gh` mutations ..., git surgery, and placing external documents remain AUTHOR ACTION**." |
| 4 | `.claude/skills/build-session/SKILL.md` (mirror) | identical to #3 | identical to #3, byte-compared after (`ehrt.docs-tooling.skill-mirror-currency-test` green) |
| 5 | `.agents/state.md` (tag mechanic) | "Six `stable-*` continuity tags live ... `-alignment-fixes-5` is licensed ... but not yet tagged (AUTHOR ACTION, prepared not executed ...); `-alignment-close` itself is licensed only after design-channel verification ..." | "Eight `stable-*` continuity tags live ... the author tagged the last two directly between the alignment arc's own close and this session, exactly at the commits and messages ADR-0055/0056 prepared (verified-and-disclosed ...)." |
| 6 | `.agents/rulings.md` AR-R-2 | "Tagging remains the author's act alone (R30)." | Superseded-in-place pointer note added (substance unedited) + new "From the tag-law session (ADR-0057)" section restating AR-T-1 as the standing replacement. |

### Red→green (AR-T-3)

Red, captured against the unswept tree (`clojure -M:poly test :all
skip:integration`, `ehrt.docs-tooling.tag-law-test`):

```
FAIL in (no-retired-tag-law-phrasing-on-live-surfaces-test) (tag_law_test.clj:62)
.agents/skills/build-session/SKILL.md still states the retired tag-law phrasing ["stay author-only in every mode"] ...
FAIL in (no-retired-tag-law-phrasing-on-live-surfaces-test) (tag_law_test.clj:62)
.claude/skills/build-session/SKILL.md still states the retired tag-law phrasing ["stay author-only in every mode"] ...
FAIL in (no-retired-tag-law-phrasing-on-live-surfaces-test) (tag_law_test.clj:62)
AGENTS.md still states the retired tag-law phrasing ["stay author-only in every mode"] ...

Ran 2 tests containing 12 assertions.
3 failures, 0 errors.
```

Exactly the three surfaces that carried the phrase at that instant
(`AUTHORS-GUIDE.md`, `.agents/state.md`, and `.agents/rulings.md`
stated the law loosely or via AR-R-2's shorter sentence, which did not
contain either exact retired string — confirmed by the same fresh grep
this session's Step 0 ran, not assumed). Green, same run repeated after
the sweep: `Ran 2 tests containing 12 assertions. 0 failures, 0
errors.` Both commits — the gate's addition and the sweep — landed
together (`263e36b`), per AR-T-3's own "sweep + gate land in ONE
commit" instruction.

### The three tag verifications (AR-T-4)

**(a) `stable-20260805-alignment-fixes-5`** — pre-existing at the exact
commit and message this session would have created (the author tagged
directly between ADR-0056's own landing and this session). Verified,
not re-created:

```
$ git ls-remote --tags origin | grep alignment-fixes-5
06ec3d13b4a33311a89dc49273501b0e9bc06463  refs/tags/stable-20260805-alignment-fixes-5
2b3bb2b4a99dd8c05622e7bfd92bd67ddf2d0f38  refs/tags/stable-20260805-alignment-fixes-5^{}
$ git tag -l -n99 stable-20260805-alignment-fixes-5
stable-20260805-alignment-fixes-5 alignment fixes 5 landed, design-channel-verified 2026-08-05 (ADR-0054)
```

Peeled ref `2b3bb2b...` matches the licensed commit exactly; message
matches ADR-0055's AR-AC-0 command verbatim.

**(b) `stable-20260805-alignment-close`** — same disposition, verified:

```
$ git ls-remote --tags origin | grep alignment-close
4c51c741826b8dfd2687e07964f68196a5c73045  refs/tags/stable-20260805-alignment-close
12d3aa33063f36ef1fad55e970240ea6cea986c4  refs/tags/stable-20260805-alignment-close^{}
$ git tag -l -n99 stable-20260805-alignment-close
stable-20260805-alignment-close alignment arc closed, design-channel-verified 2026-08-06 (ADR-0055)
```

Peeled ref `12d3aa3...` matches ADR-0056's own tip exactly; message
matches ADR-0056's AR-U0-2 command verbatim.

**(c) `stable-20260806-ux-riders`** — did not exist; created and pushed
by this session, the first tag executed under AR-T-1's standing rule
(i)-and-(ii):

```
$ git tag -a stable-20260806-ux-riders 56a2c21 \
    -m "ux riders landed, design-channel-verified 2026-08-06 (ADR-0056)"
$ git push origin stable-20260806-ux-riders
 * [new tag]  stable-20260806-ux-riders -> stable-20260806-ux-riders
$ git ls-remote --tags origin | grep ux-riders
8c4ed86367514ef42621948c52e26d451d54b6f1  refs/tags/stable-20260806-ux-riders
56a2c214d10076ad77de46357476c1cd056a59e7  refs/tags/stable-20260806-ux-riders^{}
```

Peeled ref `56a2c21...` matches the prompt's own named commit exactly.
Nine `stable-*` continuity tags now live (eight from before this
session plus this one), excluding the three frozen legacy tags.

### The third-instance note (joining the propagation lesson's register entry)

`.agents/rulings.md`'s own "Law-surface propagation lesson, standing"
entry (from ADR-0051 AR-F2-0 and ADR-0053 AR-F4-4) now carries a dated
amendment naming this as its third instance: `.agents/rulings.md`'s own
AR-R-2 stated tagging as author-only nine sessions after ADR-0049's
AR-AU-0 amended the mechanic, and was never corrected when ADR-0051's
AR-F2-0 reconciled `AGENTS.md` alone — the register meant to catch
exactly this class of drift was itself carrying the drift. Landed in
the same commit as the sweep (`263e36b`), not a separate append, since
AR-T-2 already required `.agents/rulings.md`'s own edit in that
commit.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan (baseline `detect`, 669→670 commits;
  the staged scan before the sweep commit; both pushes).
- Full suite (`clojure -M:poly test :all skip:integration`): 216
  `Test results:` lines / 0 failures / 0 errors at Step 0's own
  baseline (`56a2c21`); 218 lines / 0 failures / 0 errors after the
  sweep (the new `tag-law-test` namespace tested across two project
  classpaths, +2 lines, exactly the expected delta for one new
  namespace in a shared component).
- `ehrt.docs-tooling.tag-law-test`: red→green witnessed, transcript
  above.
- `ehrt.docs-tooling.skill-mirror-currency-test`: green throughout —
  the canonical `.agents/skills/build-session/SKILL.md` edit and its
  `.claude/skills/` mirror copy landed byte-identical (`cp -p`,
  `diff` confirmed empty).
- Post-push message verification: one delta against the message file,
  the known harmless trailing-newline artifact.
- **Oracle bracket** (`bin/regression-oracle 56a2c21 263e36b`): all
  eleven vendored-root batches (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) IDENTICAL — `IDENTICAL:
  every root's digest matches between 56a2c21 and 263e36b`; no
  `--declared-digest-change` licensed or needed, exactly expected for
  a docs/tests-only session. Any change would have been
  STOP-AND-ESCALATE per this session's own fence.
- AR-T-4's three tag verifications: transcripts above, peeled refs
  resolving to the named commits in every case.

### Fences

No `src/` changes — confirmed by the oracle bracket, not merely
asserted. The skill rewrite touched rule #8 and any other tag-stating
line only — the rest of the ceremony (staging hygiene, message-via-file,
gitleaks, push verification, premise-mismatch stops) is untouched.
`gh` repo-level mutations, git surgery, and external-document placement
remain author-only — this session narrowed nothing else. Frozen
archives untouched (this ADR + the index line + the rulings append are
the sanctioned acts). The UX audit is NOT this session —
`components/sim/... help.clj` and error strings were not read or
touched.

### Consequence

The tag law now reads the same on every surface that states it, and a
gate holds that true going forward — the retired formulations cannot
recur silently. Nine `stable-*` continuity tags live. The next
session's own Step 0 tags this landing (`stable-20260806-tag-law` at
this ADR's own tip) under AR-T-1's standing rule (ii), without further
license — the first tag executed purely as standing ceremony, not a
specifically-licensed act, proving that half of the new law end to end.
The UX audit session follows.

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line;
`notes/adr/README.md`'s own file count corrected 54→55 ("as of
ADR-0057"). Done pointer added in the same commit as the index line (so
`ehrt.docs-tooling.done-pointer-adr-test` never sees a dangling
reference):

```
- 2026-08-06 — tag-law — ADR-0057
```

Session record (`.agents/session-records/2026-08-06-tag-law.md`) and
this session's own driving prompt archived
(`.agents/prompts/2026-08-06-tag-law.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From the tag-law session (ADR-0057)

- **Stable-tag discipline, AMENDED 2026-08-06** (AR-T-1, STANDING,
  superseding AR-R-2's final sentence above): `stable-*` continuity
  tags are SESSION ACTS. A session creates and pushes one (i) when its
  own prompt licenses a SPECIFIC tag at a SPECIFIC commit, a license
  the design channel issues only after verifying the landing it names,
  or (ii) for its own predecessor's design-channel-verified stable
  point, as standing ceremony, without bouncing back to the author.
  **Deferring a licensed tag is now the deviation** and needs a
  disclosed reason — the inverse of AR-R-2's own default. The author
  may always tag directly, licensed or not; a tag already present at
  the exact commit and message a session would otherwise have created
  is verified and disclosed, never re-created. Release `v*` tags stay
  AUTHOR ACTION, unchanged — publication itself is author-gated, so its
  tags are too. `notes/ADRs.md` ADR-0003's original author-only
  trust-boundary reasoning is superseded in scope for this one class of
  tag, not erased: the design channel's own landing verification is now
  that boundary, and the tag is its mechanical consequence.
