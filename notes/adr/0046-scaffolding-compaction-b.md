## ADR-0046 — Scaffolding compaction B: the ADR split and the roadmap rotation

**Status:** Accepted (author-ruled 2026-08-05, design channel, AR-B-1
through AR-B-5 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`). Executed same
day.

### Context

Session B of the scaffolding-compaction arc (A — riders, vestige
retirements, Deferred triage — landed same day as ADR-0045; C — the
continuity register, `.agents/state.md` — is pending author rulings,
not this session's scope). This is the arc's big relocation: history
moves to the attic in compact, indexed, byte-verbatim form; nothing is
deleted. `notes/ADRs.md` had grown to 10512 lines holding 43 entries
inline; `.agents/plans/roadmap.md` had grown to 1538 lines, 1338 of
them (87%) a reverse-chronological Done history a cold session had to
scroll past to reach Now/Next/Deferred. Every move's proof is a
byte-identity extraction diff, the same discipline this repo's own
`bin/regression-oracle` VERIFICATION section demands for behavioral
claims, applied here to a structural one.

### Decision

Ruled 2026-08-05, design channel, recorded verbatim:

**AR-B-1 (ADR split, per-ADR files).** Every `## ADR-NNNN` entry in
`notes/ADRs.md` moves VERBATIM to `notes/adr/NNNN-<slug>.md` (slug
from the entry's own title, lowercased-hyphenated). `notes/ADRs.md`
becomes the INDEX: header explaining the split (dated), then one line
per ADR — number, title, file link, and the entry's own status/arc if
its header states one. The standing-rulings register is session C's
deliverable, not built here.

**AR-B-2 (citation continuity, zero-sweep).** `notes/ADRs.md` REMAINS
the citation target: every existing "notes/ADRs.md ADR-NNNN" citation
repo-wide (including frozen archives) stays resolvable — the reader
lands on the index, the index points to the file. Bare `ADR-NNNN`
continues per the citation-qualification rule. NO citation sweep is
performed or needed; the ADR states this explicitly. No renumbering
(standing ruling, re-cited). NEW execution-record appends go directly
to the per-ADR file; the index line's status updates when an arc
closes.

**AR-B-3 (roadmap rotation).** `.agents/plans/roadmap.md`'s Done
history moves VERBATIM to `.agents/plans/roadmap-done-2026-07.md` and
`roadmap-done-2026-08.md` (split by each entry's own date; entries
straddling months stay whole in their start month). The live roadmap
keeps: Now, Next, Deferred, and a Done section holding ONLY the
current (compaction) arc as one-line pointers per AR-B-4, plus a dated
header line pointing at the archive files. Attic files get a two-line
header (what they are, moved verbatim, date).

**AR-B-4 (canonical hierarchy + gate).** Recorded as standing rule: the
ADR execution record is the sole narrative of a session; the session
record is the ceremony/verification log; the roadmap Done entry is a
one-line pointer (date, slug, ADR number); the prompt archive is
provenance. One new docs-tooling deftest: every Done one-liner in the
LIVE roadmap cites an ADR number that exists in the index (both-
direction checks are C's scope if wanted; this gate is one-direction,
cheap). Red→green proven live (corrupt one pointer, watch it fail,
restore).

**AR-B-5 (deferred to C, recorded).** The continuity register
(`.agents/state.md`) is session C's deliverable, design-channel-
authored with per-claim citations against THIS session's landed
layout. B records the ruling verbatim so the ADR trail shows the
approval date.

### The split map (AR-B-1)

43 entries, `notes/ADRs.md` lines 33–10512, moved to `notes/adr/` in
this file's own pre-split order (unchanged, not renumbered — the
register's numbering was already non-sequential in file order before
this session, e.g. ADR-0013 followed by ADR-0022; that order is
preserved exactly, not "fixed" into ascending order):

| ADR | file | original lines |
|---|---|---|
| ADR-0001 | `0001-migration-plan.md` | 33–419 |
| ADR-0002 | `0002-land-ehr-testing-tools.md` | 420–849 |
| ADR-0003 | `0003-pre-push-gate-doctrine.md` | 850–933 |
| ADR-0004 | `0004-carve-loss-audit.md` | 934–1152 |
| ADR-0005 | `0005-the-ehr-sim-mount.md` | 1153–1352 |
| ADR-0006 | `0006-discipline-parity-restored.md` | 1353–1505 |
| ADR-0007 | `0007-commit-push-restored-to-session-ritual.md` | 1506–1682 |
| ADR-0008 | `0008-kernel-and-judge-extraction.md` | 1683–1872 |
| ADR-0009 | `0009-cli-renamed-ehrt.md` | 1873–2009 |
| ADR-0010 | `0010-documentation-doctrine.md` | 2010–2136 |
| ADR-0011 | `0011-per-engine-judge-split.md` | 2137–2375 |
| ADR-0012 | `0012-judge-v2-nist-adopts-the-nist-engine-directly.md` | 2376–2689 |
| ADR-0013 | `0013-output-ux-doctrine.md` | 2690–2926 |
| ADR-0022 | `0022-sim-adopts-ehrt-kernel-result.md` | 2927–3074 |
| ADR-0021 | `0021-bases-sim-cli-projects-sim-retired.md` | 3075–3259 |
| ADR-0018 | `0018-tools-split-stage-3.md` | 3260–3609 |
| ADR-0017 | `0017-tools-split-stage-2.md` | 3610–3877 |
| ADR-0016 | `0016-tools-split-stage-1.md` | 3878–4113 |
| ADR-0014 | `0014-corpus-player.md` | 4114–4323 |
| ADR-0015 | `0015-cli-trial-ux.md` | 4324–4540 |
| ADR-0023 | `0023-agent-ux-charter-adopted.md` | 4541–4757 |
| ADR-0024 | `0024-claude-skills-carved-out-of-the-untracked-claude-ruling.md` | 4758–4865 |
| ADR-0025 | `0025-sim-split-s1-s2.md` | 4866–5141 |
| ADR-0026 | `0026-gmf-coverage-wave-a.md` | 5142–5273 |
| ADR-0027 | `0027-gmf-coverage-wave-b.md` | 5274–5461 |
| ADR-0028 | `0028-gmf-coverage-wave-c.md` | 5462–5650 |
| ADR-0029 | `0029-gmf-coverage-wave-d.md` | 5651–6538 |
| ADR-0030 | `0030-post-wave-d-cleanup.md` | 6539–6702 |
| ADR-0031 | `0031-parity-plan-rulings-wellness-semantics-overturn-defect-fix-sequencing.md` | 6703–6847 |
| ADR-0032 | `0032-procedure-duration-fix.md` | 6848–7024 |
| ADR-0033 | `0033-engine-closure-context-fix.md` | 7025–7210 |
| ADR-0034 | `0034-gmf-census-tool.md` | 7211–7402 |
| ADR-0035 | `0035-wave-f0.md` | 7403–7668 |
| ADR-0036 | `0036-wave-f.md` | 7669–7910 |
| ADR-0037 | `0037-gmf-coverage-wave-g.md` | 7911–8161 |
| ADR-0038 | `0038-wave-lc.md` | 8162–8348 |
| ADR-0039 | `0039-wave-vs.md` | 8349–8588 |
| ADR-0040 | `0040-gmf-coverage-wave-i.md` | 8589–8887 |
| ADR-0041 | `0041-gmf-coverage-wave-i2.md` | 8888–9100 |
| ADR-0042 | `0042-wave-h-pre-roll.md` | 9101–9342 |
| ADR-0043 | `0043-sim-split-b-m1.md` | 9343–10172 |
| ADR-0044 | `0044-standing-equipment-promotion.md` | 10173–10369 |
| ADR-0045 | `0045-scaffolding-compaction-a.md` | 10370–10512 |

Slugging note (disclosed, since AR-B-1's own "lowercased-hyphenated"
license under-specifies for a title carrying a parenthetical citation
or a long elaboration after its own colon/semicolon): each slug is the
title's own text up to its first `:` or `;` (parenthetical groups
stripped first), lowercased, non-alphanumeric runs collapsed to a
single hyphen. This is not an invented convention — it reproduces,
mechanically, the same slugs this repo's own session records already
chose by hand for two of these same entries (ADR-0044 → `standing-
equipment-promotion`, ADR-0045 → `scaffolding-compaction-a`), so the
rule is a description of existing practice, not a new one.

### The rotation map (AR-B-3)

33 Done entries, `.agents/plans/roadmap.md` (pre-rotation) lines
201–1538, in original (reverse-chronological) order. **Finding,
disclosed rather than silently adapted (build-session skill's own
"fix-forward with disclosure on premise mismatch" rule):** every one
of the 33 entries is dated 2026-08 — none is 2026-07. AR-B-3 names both
`roadmap-done-2026-07.md` and `roadmap-done-2026-08.md`; both are
created as ruled, but the July file necessarily holds zero entries
(its own two-line header states this plainly; nothing was found to
move there, nothing was silently omitted). This is a fact about the
roadmap's own history (the workspace's Done log did not accumulate
until after 2026-07-28's bootstrap, and even the earliest Done rows
are dated 2026-08-01), not an ambiguity in the ruling — no escalation
warranted.

All 33 entries land in `roadmap-done-2026-08.md`, verbatim, in their
original order — dates 2026-08-01 through 2026-08-05, spanning
migration sessions 1–6, sim split S1–S4/B M1–M4, GMF coverage Waves
A–I2, the two defect-fix sessions, post-Wave-D cleanup, the GMF
census, the docs coherence pass, standing-equipment promotion, and
scaffolding compaction A.

The live roadmap's new Done section holds one one-line pointer for
compaction A (already landed) plus — from this entry's own landing
commit — one for compaction B itself (AR-B-4's "one-line pointer: date,
slug, ADR number", the new gate's first real subject):

```
- 2026-08-05 — scaffolding-compaction-a — ADR-0045
- 2026-08-05 — scaffolding-compaction-b — ADR-0046
```

### Both proofs (AR-B-1/AR-B-3 verification)

**ADR split.** Scripted extraction (Python, `notes/ADRs.md` read once,
sliced at each `## ADR-` boundary, each per-ADR file written with a
2-line attic header + blank + the verbatim slice): concatenating all
43 per-ADR files' own content minus their 3-line (2 comment + 1 blank)
attic header, in this file's own pre-split order, and diffing against
`notes/ADRs.md`'s own lines 33–10512 (the pre-split entries' span) —
`reconstructed == original_span` in Python, exact string equality, no
diff produced. (`git show HEAD~3:notes/ADRs.md` — the pre-Step-1
commit — is the same content this check ran against, for a reader
verifying after the fact.)

**Roadmap rotation.** Same method: `roadmap-done-2026-08.md`'s own
content minus its 3-line header, diffed against
`.agents/plans/roadmap.md`'s pre-rotation lines 201–1538 (via `git
show HEAD~2:.agents/plans/roadmap.md`) — exact string equality, no
diff. The live roadmap's own pre-Done-section head (lines 1–200,
Now/Next/Externals/Deferred) was separately diffed against its own
pre-rotation self and found byte-identical — nothing there moved or
changed.

### Gates and full-suite verification (AR-B-4)

`clojure -M:poly check`: OK after every step. `clojure -M:poly test
:all skip:integration`: 511 passes / 0 failures / 0 errors, unchanged
in shape across all three steps (the new deftest file adds 4 forms to
that count, confirmed below — no existing test's pass count moved).

**The gate, red→green, live:** `ehrt.docs-tooling.done-pointer-adr-
test`'s real gate
(`every-done-pointer-cites-an-adr-that-exists-in-the-index-test`) was
run against the live files (green — ADR-0045 resolves), then the live
roadmap's own pointer was corrupted in place (`ADR-0045` → `ADR-9999`)
and the same test run again: **FAIL**, `"cites ADR number(s) not in
notes/ADRs.md's own index: [\"ADR-9999\"]"` — the gate fires. The
roadmap was restored from a pre-edit copy (`git diff` empty afterward,
confirmed) and the test re-run: green again. Not merely asserted — the
red run's own failure output is quoted above.

**Deftest parity, counting definition stated:** `git grep -o '(deftest
' <ref> -- '*.clj' | wc -l` (this repo's own established convention,
ADR-0045/ADR-0044) moved 1566 (baseline `3495181`) → 1570 (landing) —
a raw +4. Of those four, **+1 is "the gate"** AR-B-4 asks to ledger
(`every-done-pointer-cites-an-adr-that-exists-in-the-index-test`, the
one real enforcement deftest); the other **+3 are mechanism-sanity**
tests proving the gate's own extraction functions actually catch what
they claim to (`done-pointer-extraction-is-actually-caught-test`,
`indexed-adr-numbers-extraction-is-actually-caught-test`,
`a-dangling-done-pointer-is-caught-test`) — the same gate-plus-proof
pairing `ehrt.docs-tooling.reading-set-budget-test` and
`ehrt.docs-tooling.index-completeness-test` already established in
this suite. The ledger tracks the gate (+1); the sanity tests are the
proof the gate gates, not a second gate.

**Oracle bracket** (`bin/regression-oracle 3495181 cde6303`, pre-B tip
→ Step-3 landing — this session touches no `src` at all, so any digest
change would have been STOP-AND-ESCALATE): `IDENTICAL: every root's
digest matches between 3495181 and cde6303` — all ELEVEN vendored-root
batches (`appendicitis`, `death-fixture`, `ear-infections`, `ear-
infections-engine`, `ear-infections-history-engine`, `sepsis`,
`sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
`urinary-tract-infections-engine`, `urinary-tract-infections-history-
engine`) byte-identical, exactly as expected for a docs/notes/test-only
session.

**Budget re-derivation** (AR-B-4, re-applying AR-D-3's own formula):
`:onboarding` is the only reading set carrying `roadmap.md` (`notes/
ADRs.md` is not `:paths` in any set); its measured total dropped from
2405 (pre-rotation actual, budget already re-baselined 2026-08-05
same day, docs coherence pass) to 950 (post-rotation actual, driven
almost entirely by `roadmap.md`'s own 1342→204 line drop) —
re-derived budget 950 × 1.15 → 1092.5, rounded up to the nearest 5:
**1095**. `ehrt.docs-tooling.reading-set-budget-test`: green
throughout (a shrinking actual never risks the ceiling-only gate; the
re-derivation is a tightening, not a fix of a red).

### Fence

Nothing deleted — every byte relocates or stays; both extraction diffs
are the proof, not an assertion. No content rewrites beyond what
AR-B-1/AR-B-3 themselves license (the index lines, the attic headers,
the live Done section's own new one-line pointers) — two READMEs
(`notes/README.md`, `.agents/plans/README.md`) and one new
`notes/adr/README.md` gained the minimum bullet/index text this
session's own new directories and files mechanically require under
`ehrt.docs-tooling.readme-presence-test`/`index-completeness-test`
(disclosed here, not silent scope creep: neither AR-B-1 nor AR-B-3
names these files, but leaving either gate red was not an option).
No citation sweep (AR-B-2 makes it unnecessary — performing one anyway
would have been scope creep). No rulings register, no `state.md`
(session C, AR-B-5). No reading-set `:paths` membership changes. Frozen
archives (`notes/sim/`, `notes/tools/`, `notes/prompts/`) untouched —
`notes/adr/` is a NEW attic, not a modification of any of the three.
No ADR entry's boundaries were ambiguous (every one anchored on a
clean `^## ADR-NNNN — ` heading, none nested or malformed) — no
STOP-AND-ESCALATE fired.

---

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0046 (scaffolding compaction B)

- **Citation continuity, standing** (AR-B-2): `notes/ADRs.md` REMAINS
  the citation target forever — every "notes/ADRs.md ADR-NNNN"
  citation repo-wide stays resolvable (index → per-ADR file). No
  renumbering, ever — ADR numbers are load-bearing in immutable places
  this workspace cannot edit (commit messages, archived prompts).
- **The canonical session-narrative hierarchy** (AR-B-4, explicitly
  recorded as standing rule): the ADR execution record is the SOLE
  narrative of a session; the session record is the ceremony/
  verification log; the roadmap Done entry is a one-line pointer
  (date, slug, ADR number) gated by `ehrt.docs-tooling.done-pointer-
  adr-test`; the prompt archive is provenance. New execution-record
  appends go directly to the per-ADR file, never back through
  `notes/ADRs.md` itself.
