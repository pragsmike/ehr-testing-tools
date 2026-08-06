## ADR-0058 — UX audit: every stranger-facing surface surveyed, nothing moved

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: the tag law landed and was design-channel-verified (`1d93e6c`,
`notes/adr/0057-tag-law.md`) — the fixed law's own first
purely-standing-ceremony tag (no specific license, rule (ii) only) was
this session's own Step 0 act. This is the UX arc's audit session per
its brief (`.agents/plans/2026-08-06-ux-arc-brief.md` §6 step 1):
survey every user-facing surface — invocations, help text, error
messages, the first-run experience — and land a findings register.
Findings-only, absolute: not one string changed, however trivial the
fix looked. The register format, fences, and honest-partial license
all follow the alignment audit's proven pattern
(`.agents/plans/2026-08-05-alignment-audit-findings.md`).

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-UA-0 (tag, standing ceremony — the first).** Per ADR-0057 AR-T-1
clause (ii), tag the predecessor's verified stable point: annotated
`stable-20260806-tag-law` at `1d93e6c`, message "tag law landed,
design-channel-verified 2026-08-06 (ADR-0057)"; push; verify. No
further license exists or is needed — this is the first tag executed
under standing ceremony, and this ADR says so.

**AR-UA-1 (findings-only, absolute).** Every defect found is a
register row, never an edit. `help.clj`, error strings, READMEs,
config comments: all read-only this session. The alignment audit's law
verbatim: acting on a finding is the worst outcome.

**AR-UA-2 (the register).** `.agents/plans/2026-08-06-ux-audit-findings.md`
+ index entry. Row format identical to the alignment register (id |
area | probe | evidence | finding | recommendation | proposed
disposition), dispositions from the same vocabulary plus one new
value: `design-channel-draft` — for rows whose fix is wording the
design channel must draft for author ruling before any session touches
the file. Seeds U1–U5 from the brief appear first, updated with this
session's evidence; ADR-0056's supplementary config.edn finding folds
into U1's row. New rows numbered by area.

**AR-UA-3 (area A — invocation surfaces).** Enumerate every command
fence and command-bearing comment in the tree's live docs: for each,
classify against the CLI's real grammar. Gate candidate row
(recommend shape, do not build): an invocation-lint in the
docs-tooling family forbidding `clojure -M:cli` on live doc surfaces.

**AR-UA-4 (area B — help voice).** Enumerate every user-visible string
in `help.clj`'s spec data: classify each as user-clean / agent-speak /
over-long. Counts by class per group go in the row; the full
classified inventory lands as an APPENDIX section of the register — it
is the design channel's rewrite worklist. Also probe `bin/ehrt help`'s
actual rendered output and attach a representative transcript.

**AR-UA-5 (area C — error surfaces).** Enumerate every operational-
error path (exit 2) reachable from `bases/cli` and `ehrt.sim.run`'s
command surface: for each, does the message name the concrete
artifact and offer a next step? The `--config` missing-file path FIRST
(the brief's own open `[audit]`). U4's near-miss idea gets its
assessment here.

**AR-UA-6 (area D — first contact).** Transcript, as a user would see
them: bare `bin/ehrt`; `bin/ehrt help`; `bin/ehrt sim`; the README
quickstart's first fence run verbatim from a clean checkout state.
Judgment rows are fine — UX quality is partly judgment — but every
judgment cites its transcript.

**AR-UA-7 (budget).** The alignment audit's honest-partial license
verbatim: an area's rows may land marked `incomplete` with what
remains; the register may not be skipped; the help-string APPENDIX
inventory (AR-UA-4) is the one deliverable that must be complete.

### Execution record

**Step 0 — preflight + tag.** Working directory confirmed
`~/src/ehr-testing-tools` (ext4, `df -T` reports `ext4`); tip `1d93e6c`
exactly; working tree clean apart from `config/busy-weekday.md`
(unrelated pre-existing untracked file, left alone, as every session
since the incident has correctly done). Baseline: `clojure -M:poly
check`: OK. Full suite (`clojure -M:poly test :all skip:integration`):
218 `Test results:` lines, 0 failures/0 errors. `gitleaks detect -v`:
671 commits scanned, no leaks. Oracle pre-digest (`bin/regression-
oracle 1d93e6c 1d93e6c`): all eleven roots IDENTICAL, soundness "yes
outside ns form."

AR-UA-0 executed: `stable-20260806-tag-law` did not exist locally or
on origin (`git tag -l 'stable-*'` and `git ls-remote --tags origin`
both checked); created annotated at `1d93e6c`, message "tag law
landed, design-channel-verified 2026-08-06 (ADR-0057)"; pushed;
verified — peeled ref `1d93e6c...` resolves exactly. **This is the
first tag this workspace has ever executed purely under AR-T-1's
standing-ceremony rule (ii), with no specific per-tag license in the
driving prompt** — ADR-0057's own closing note said the next session's
Step 0 would prove this half of the law end to end; this session is
that proof.

**Step 1 — Areas A–D (AR-UA-1..7).** All four areas probed, none left
unprobed. Area A delegated to a sub-agent (full sweep, 106 candidate
files, no truncation); Areas B/C/D probed directly (`help.clj` read in
full; six deliberate error-triggering commands run against throwaway
scratch paths, never `config/` or the tracked tree; five live CLI
transcripts captured). Full findings, evidence, and the complete
100-string help-text APPENDIX inventory:
`.agents/plans/2026-08-06-ux-audit-findings.md`.

**Headline findings** (full detail and reasoning in the register):

- **U1 (Area A), expanded.** The brief's own seeded finding (6 demo
  READMEs + 2 config.edn headers teaching the stale `clojure -M:cli`
  form) is confirmed and **enlarged**: the wider sweep this session ran
  found 3 more stale surfaces the narrower prior scope missed
  (`components/sim-emit-hl7/docs/demos/` — the site-profiles tree
  relocated by ADR-0029 was never re-swept — and a top-level
  `docs/simulate-your-facility.md` fence). 11 file:line groups, 14
  stale command instances total, plus one independent flag-pairing
  defect (`module-mix`'s own commands give `--format er7` with no
  `--emit hl7`, which `docs/cli.md` documents as required).
- **U3/C-1 (Area C), the brief's own open `[audit]` item, resolved and
  worse than framed.** `bin/ehrt sim run --config <missing>` does not
  merely fail to name the artifact — `ehrt.sim.run/merge-config-file`
  (`components/sim/src/ehrt/sim/run.clj:194-208`) calls `(edn/read-
  string (slurp path))` with no exception handling at all, the sole
  such gap in the codebase (every sibling file-path-consuming path —
  `--lockfile`, `gate PATH`, `--profile` — already uses the `Result`
  vocabulary correctly, confirmed by direct comparison). The user sees
  a raw JVM stack trace at **exit code 1**, not this CLI's own
  documented exit 2 for operational errors — the exit-code contract
  itself is violated, not just the message's tone. The same crash
  reproduces via `corpus generate sim --config <missing>` (shared code
  path) and via a malformed-but-existing config file (a different raw
  crash, also exit 1, also never naming the path). This is the exact
  class of failure the brief's own incident narrative (§2) describes.
- **B-1..B-4 (Area B), the full APPENDIX.** All 100 user-facing
  strings in `help.clj`'s `cli-spec` classified: 24 ADR-token
  citations across 19 strings (10 distinct ADRs), 14 milestone-tag
  occurrences, 3 `ruling N` citations, 3 bare-keyword leaks, 2
  internal-namespace-reference leaks, 11 strings over 250 characters
  with no wrap structure in the renderer (`render-flag`/`render-group`/
  `render-top-level`, read in full, perform no line-wrapping at all —
  confirmed live in the `bin/ehrt help` transcript, where the `corpus`
  group's own 645-character doc renders as one unbroken paragraph).
- **B-5/D-1, a live exit-code inconsistency.** Bare `bin/ehrt` and
  `bin/ehrt help` render byte-identical output but exit 2 and 0
  respectively — confirmed by direct, unpiped `$?` capture. Flagged
  ruling-needed: intentional distinction or an oversight of the
  `--help`/`help` convention already established elsewhere in the same
  spec.
- **C-4, a new defect the brief did not seed.** An unrecognized flag
  (`--bogus-flag foo`) is silently accepted and absorbed into `:opts`
  with no diagnostic at all — the run succeeds, echoing the bogus flag
  back in its own manifest. Assessed as the single most user-hostile
  gap surveyed: a typo'd flag name produces no feedback whatsoever,
  only a silently-wrong run.
- **D-4, a positive judgment finding.** Once past the help screen, the
  CLI's own RUN output is close to ideal — terse, no agent-speak, names
  the exact artifact produced (`bin/ehrt corpus generate`'s own one-
  line result). The UX arc's real work concentrates in HELP text and
  ERROR text, not command output generally; recorded so a future
  session doesn't over-scope into places that are already good.

Register summary (full accounting in the register's own closing
paragraph): 5 seeded rows (3 carrying their own disposition — U1, U4,
U5), 18 new rows across Areas A–D, 21 total rows carrying a
disposition. Disposition counts: close-as-fine 8, ruling-needed 1
(B-5/D-1, one finding), fix-session-candidate 6 (C-1 flagged highest
priority), design-channel-draft 4 (B-1/B-2/B-3/B-4's content half — the
APPENDIX is their shared worklist), incomplete 1 (D-4 — the Quickstart's
network-dependent artifact-fetch steps were not run this session,
disclosed, not silently skipped; the two non-network steps that answer
the UX-signal question were run and transcribed).

Commit (`.agents/plans/README.md` index entry + the register, staged
together, staging hygiene confirmed via `git diff --cached --stat`
before commit — exactly the two files, `config/busy-weekday.md`
confirmed not staged): `41c1bdb` ("docs: the ux audit register lands —
every stranger-facing surface surveyed, nothing moved (ux audit,
AR-UA-2..7)"). `gitleaks git --staged -v`: clean. Full suite
re-confirmed green post-edit before push: 218 `Test results:` lines, 0
failures/0 errors, identical shape to Step 0's own baseline. Pushed;
post-push verification: one delta against the message file, the known
harmless trailing-newline artifact.

**Step 2 — this record.** `notes/ADRs.md` gains this ADR's own index
line; `notes/adr/README.md`'s own file count corrected 55→56 ("as of
ADR-0058"). The Done pointer `- 2026-08-06 — ux-audit — ADR-0058`
lands in the live roadmap in the same commit as the index line.
Session record (`.agents/session-records/2026-08-06-ux-audit.md`) and
this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-audit.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.

### Verification

- `clojure -M:poly check`: OK, every step.
- Full suite (`clojure -M:poly test :all skip:integration`): green
  throughout, 218 `Test results:` lines, 0 failures/0 errors at Step 0's
  own baseline and again after the register landed, unchanged shape —
  expected for a docs-only session.
- `gitleaks`: clean at every scan (baseline `detect`, 671→672 commits
  across the session's two pushes; the staged scan before the register
  commit; both pushes' own pre-push hook runs).
- Post-push message verification: one delta against the message file
  at the register commit, the known harmless trailing-newline artifact.
- **Oracle bracket** (`bin/regression-oracle 1d93e6c 41c1bdb`, Step
  0's own tip to Step 1's own closing commit — no `src/` touched at any
  point this session): `IDENTICAL: every root's digest matches between
  1d93e6c and 41c1bdb` — all eleven vendored-root batches
  (`appendicitis`, `death-fixture`, `ear-infections`,
  `ear-infections-engine`, `ear-infections-history-engine`, `sepsis`,
  `sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  expected for a register-and-tags-only session; no
  `--declared-digest-change` licensed or needed. Any change would have
  been STOP-AND-ESCALATE per this session's own prompt.
- AR-UA-0's tag verification: transcript above, peeled ref resolving
  to `1d93e6c` exactly, first standing-ceremony-only tag on record.

### Fences (standing law applies unchanged, this session's own prompt)

Move-don't-improve; findings-only audits (AR-UA-1) — the only
mutations this session made are the tag, the register, and this
record. Deliberate error-triggering runs (AR-UA-5) used throwaway
paths under the session scratchpad and never wrote into `config/` or
the tracked tree; the one probe run that wrote into the tree
(`out/corpus/sim-s1-p1`, Area D's quickstart probe) landed in a
gitignored, never-staged directory and was removed immediately after.
`config/busy-weekday.md` remains untouched and untracked. No gates
built (recommend only, per AR-UA-3/C-4/B-4's own mechanism-side
recommendations). Frozen archives untouched (this ADR + index + Done
pointer are the sanctioned acts). Settled law not re-raised.

### Consequence

Every stranger-facing surface this workspace exposes has now been
surveyed once, with evidence, against a register the design channel
can rule on cluster by cluster — nothing moved, so the survey itself
carries no risk of having introduced what it was measuring. The
`--config` crash (C-1) is now a documented, highest-priority
fix-session candidate rather than a live, undiagnosed incident risk.
The help-text APPENDIX is a complete rewrite worklist, not a sample.
After landing: design channel verifies by fresh probe, then drafts (a)
the help-spec voice rewrite from the register's own appendix and (b)
per-cluster ruling recommendations for the author, exactly as the
alignment arc's own pattern ran. This landing's own tag rides the next
session's Step 0 under standing ceremony.

### Step 2 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line;
`notes/adr/README.md`'s own file count corrected 55→56 ("as of
ADR-0058"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-audit — ADR-0058
```

Session record (`.agents/session-records/2026-08-06-ux-audit.md`) and
this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-audit.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md` in
the same commit.
