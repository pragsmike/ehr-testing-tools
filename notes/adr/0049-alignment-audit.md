## ADR-0049 — Alignment audit: the tree examined, findings registered, nothing moved

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: the alignment-riders session opened the arc, landed the audit
brief, and adopted live stable-tagging (`f1ceea7`'s own prior tip
`79b7a55`, `notes/adr/0048-alignment-riders.md`). This session is the
brief's own §6 Step 1: run the §4 probes in-workspace, land a findings
register, execute exactly two pre-ruled fix-forward acts, touch
nothing else. R30, docs-only, findings-only beyond Step 0.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's
own prompt):

**AR-AU-0 (tagging delegated — STANDING amendment to AR-R-2's
mechanics).** Sessions create and push stable tags when a prompt
licenses a specific tag at a specific commit; the license issues only
after design-channel verification of that landing, so the verification
gate is unchanged — only the hands are. This session creates annotated
tag `stable-20260805-alignment-riders` at `79b7a55` (message:
"alignment riders landed, design-channel-verified 2026-08-05
(ADR-0048)"), pushed via `git push origin <tag>`; if push auth fails,
fall back to `gh api repos/{owner}/{repo}/git/refs`. Going forward,
each session tags the PRIOR verified stable point when licensed; the
author may still tag directly at any time.

**AR-AU-1 (S7 fix-forward, pre-ruled).** The `:mllp` Deferred row's
`ehrt.tools.corpus.source-sink` corrects to the live namespace (verify
first: expected `ehrt.corpus-io.source-sink`), with a dated note. This
is the session's ONLY roadmap edit.

**AR-AU-2 (findings-only).** Every other defect, smell, or tempting
fix discovered this session becomes a register row — never an edit.
Acting on a finding is the worst outcome; a wrong-but-recorded finding
is cheap, an unrecorded fix is not.

**AR-AU-3 (the register).** Findings land at `.agents/plans/
2026-08-05-alignment-audit-findings.md` + index entry in `.agents/
plans/README.md`. Row format: `id | area | probe | evidence | finding
| recommendation with reasoning | proposed disposition`, disposition ∈
{ruling-needed, fix-session-candidate (with suggested cluster),
close-as-fine, incomplete}. Seeded rows S1–S7 from the brief updated
with this session's evidence; new rows follow, numbered A-n through
F-n by area.

**AR-AU-4 (report-only probes).** `clojure -M:poly libs :outdated`
output is a register row and nothing else — no upgrades, no `deps.edn`
edits. Same for reflection warnings, never-called interface vars, and
NOTICE-coverage gaps: rows, not fixes.

**AR-AU-5 (budget).** If an area's probes exceed reasonable effort,
land its partial rows marked `incomplete` with what remains — an
honest partial register outranks a complete-looking one. The register
may not be skipped.

### Step 0 — preflight, tag, S7 fix

Working directory confirmed `~/src/ehr-testing-tools` (ext4, not
`/mnt/c`); tip `79b7a55` exactly. Baseline full suite green (6
`Test results:` blocks, 0 failures/0 errors across all projects,
`clojure -M:poly test :all skip:integration`, exit 0).

**AR-AU-0, executed.** Annotated tag `stable-20260805-alignment-riders`
created at `79b7a55` with the exact prescribed message; pushed via
`git push origin stable-20260805-alignment-riders` — the primary path
succeeded (`gitleaks` pre-push scan clean, `[new tag]` confirmed by
git), so the `gh api` fallback was never needed. Verified live on
origin: `git ls-remote --tags origin` shows both the tag object
(`65d6f11...`) and its peeled ref resolving to `79b7a55` exactly.

**AR-AU-1, executed.** Verified the live namespace first
(`components/corpus-io/src/ehrt/corpus_io/source_sink.clj`'s own `ns`
form: `ehrt.corpus-io.source-sink`, matching the ruling's own
expectation exactly) before editing. `.agents/plans/roadmap.md`'s
`:mllp` Deferred row corrected: `ehrt.tools.corpus.source-sink` →
`ehrt.corpus-io.source-sink`, with the exact dated note the ruling
specified. This was the session's only roadmap edit — a second stale
reference in the SAME row (`ehrt.tools.corpus.framing`, also retired
at the same ADR-0017 stage) was found and left untouched per the
ruling's own single-edit fence, recorded as finding A-6 instead.

Committed `f1ceea7` ("docs: a stale namespace corrected in the
player's row; the first stable tag lands (alignment audit,
AR-AU-0/1)"), pushed, verified (one delta against the message file —
`git log --format=%B -1`'s own trailing-newline artifact, the known
harmless class).

### Step 1–2 — the register

`.agents/plans/2026-08-05-alignment-audit-findings.md` lands, index
entry in `.agents/plans/README.md`. Row count: **7 seeded rows**
(S1–S7, all re-verified with fresh evidence; S2, S4, S6 closed this
session/by the rider session, re-confirmed live; S7 is
recommendation-only per its own fence) **+ 40 new rows** across six
areas — A: 6, B: 13, C: 7, D: 4, E: 10, F: 7. Total 47 rows.

Disposition counts: **close-as-fine 26** (including several rows
whose disposition is folded into a cross-cutting row, e.g. F-2/F-3
into F-4), **ruling-needed 12**, **fix-session-candidate 10**,
**incomplete 3** (B-8, B-9 — `palgebra`/`provenance`'s own `valid?`
export, a bare-name-collision false-positive risk a qualified-alias
follow-up would resolve, not a coverage gap; B-12 — the reflection
sweep's own first-pass methodology caveat, whose underlying
deduped data in B-13 is trustworthy).

**Notable findings, not acted on (per AR-AU-2):** `poly check :dev`
surfaces a real, previously-silent warning (`oracle` unreachable from
`development`'s own composition, A-1); the NIST jar `file://`-mirror
end-state named as a future risk by the brief has NOT landed — the
workspace remains hit-nexus-live (A-4); S1's resource-nesting hazard
re-confirmed with a concrete rename recommendation (S1/C-1); the
brief's own §4.4 framing of what a `sim-emit-cda` sibling would copy
was corrected by evidence — the HL7-specific idioms it named
(site-profile, v2-replay) are wire-stream-format concerns a
document-shaped CDA sibling would NOT copy; `sim-emit-fhir`'s own
snapshot-at pattern is the real candidate (D-2); a landing spot for
the pairing-as-data registry was confirmed acyclic (`judge`,
recommended — D-3); of ADR-0017/0018's 7 named-futures, 4 turned out
to already be closed by mechanisms those ADRs didn't anticipate,
found only by re-checking the live tree rather than trusting the
ADRs' own "still open" framing (D-4); three of four Apache-2.0-sourced
vendored roots lack the license text itself, only a NOTICE narrative
citing it (F-4); `ehrt version`'s own `repo-identity` is literally
"pre-release" even though four `stable-*` tags now exist (F-5).

Full suite green throughout (511 assertions, 0 failures/0 errors,
re-run after Step 2's own edits). `clojure -M:poly check`: OK.

Committed `2246a41` ("docs: the audit findings register lands — the
tree examined, nothing moved (alignment audit, AR-AU-3)"), pushed,
verified (same trailing-newline artifact only).

### Step 3 — this record

This ADR authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own file count corrected 46→47 in the same
commit that makes it stale (the same fix-forward-in-place precedent
ADR-0048's own session set for this exact file); Done pointer added to
`.agents/plans/roadmap.md` (`- 2026-08-05 — alignment-audit —
ADR-0049`).

### Verification

- `bin/regression-oracle 79b7a55 2246a41` (baseline: this session's
  own pre-session tip; target: the tip immediately before this
  record's own closing commit — no `src` touched at any point this
  session): `IDENTICAL: every root's digest matches between 79b7a55
  and 2246a41` — all ELEVEN vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as expected for a docs-only session. No `--declared-digest-change`
  licensed or needed.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (511 assertions, 0 failures/0 errors) and again
  after Step 2 (same shape) — the arc's own edits (roadmap, findings
  register, README index entries, this ADR) are all docs, so no test
  count change is expected or found.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every checkpoint: each showed
  exactly one delta against its own message file — the known,
  harmless trailing-newline artifact prior sessions already name.

### Consequence

The alignment-audit brief's §4 checklist is fully probed across all
six areas; nothing was fixed beyond the two pre-ruled Step 0 acts. The
register is the deliverable: 47 rows, each citing its own probe and
evidence, ready for the author's per-cluster rulings. The first live
`stable-*` tag (`stable-20260805-alignment-riders`) is live on origin,
proving AR-AU-0's delegated-tagging mechanic end to end. A second
stable tag, `stable-20260805-alignment-audit`, is licensed for a
future session once the design channel verifies this landing by fresh
probe (per the brief's own close-out and AR-AU-0's standing mechanic).
