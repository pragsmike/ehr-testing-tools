## ADR-0115 — Review-3's rulings land: three questions, three clusters chartered

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

`notes/adr/0114-review-3-user-surface.md` closed review-3's own
seven-battery survey with three `ruling-needed` register rows
(`.agents/plans/2026-08-12-review-3-user-surface-findings.md`):
R3-B1-1 (`--out-dir`'s double meaning across `corpus generate`/
`mutate`/`batch` versus `gate fhir`), R3-B1-4 (`--seed`'s
required-vs-defaulted split between `corpus generate` and `sim
run`/`sim identifiers`), and R3-B1-7 (`--received`'s wall-clock
default on `corpus intake`, precedent-setting for the class
"provenance metadata about a real-world act"). The design channel
framed each as a small multiple-choice question; the author ruled,
verbatim, 2026-08-12: *"Q1 a. Q2 a. Q3 a."* This is a REGISTERS-ONLY
session in the ADR-0093 lineage (a review's own rulings-landing step,
same pattern as ADR-0093 for repo review 2): it records the rulings,
fixes the findings register forward, and charters the register's
twelve `fix-session-candidate` rows into three fix clusters plus a
design-channel-draft queue note. Zero `src`, zero `test/`, zero
`docs/` changes; zero fixes of any finding executed here — that work
is the three chartered clusters' own future sessions.

### Tag ceremony

`git fetch` confirmed `origin/main` at `d508cd6`
(`d508cd6ee3a5a6fda64c0007b9ac57855ad5acc5`, ADR-0114 close) at
session start. License: tag-law case (i) — the design channel's own
2026-08-12 verification of the ADR-0114 landing (fresh clone; lineage
ea4346c→9f7697a→d0679e9→aeb45ab→d508cd6; ASCII clean on all four
messages; footprint exact to the fence plus one gate-forced companion,
`.agents/plans/README.md`, disclosed in ADR-0114's own deviations;
zero-`src` diff re-deriving the oracle identity basis; riders
content-verified; the register's own summary arithmetic independently
recounted and confirmed). CI: last five `test`-lane runs on `main`
checked at session start — four `success`, one `failure`
(`31598555300`/`d0679e9`), the interim CI-red window ADR-0114 itself
disclosed and fixed forward at `d508cd6`; no new red. `stable-
20260812-review-3` tagged ANNOTATED at `d508cd6`; pushed; peeled ref
confirmed `d508cd6ee3a5a6fda64c0007b9ac57855ad5acc5` — exact match.

### Decision

**[A] RQ1 — `--out-dir`'s double meaning** (R3-B1-1). Options: (a)
rename `gate fhir`'s flag to `--scratch-dir` so `--out-dir` means one
thing repo-wide (a protected artifact, collision-refused); (b) keep
both, document the difference; (c) make `gate fhir`'s protected too.
RULED (a). Concrete meaning: the rename is chartered to fix cluster A
below; until it lands, `--out-dir`'s canonical repo-wide meaning is
`corpus generate`'s (a protected artifact).

**[A] RQ2 — `--seed`'s required-vs-defaulted split** (R3-B1-4).
Options: (a) ruled deliberate — `corpus generate` is the ergonomic
front door (defaults), `sim run`/`sim identifiers` are the strict
engine tier (require) — recorded as design, plus a one-line help note
naming the tiering; (b) default everywhere; (c) require everywhere.
RULED (a). Concrete meaning: the split is design, not drift; the help
note (small) is chartered to fix cluster A; future front-door/engine
flag decisions in this workspace cite this ruling rather than
re-litigate the pattern.

**[A] RQ3 — `--received`'s wall-clock default, precedent-setting**
(R3-B1-7). Question: is provenance metadata about a real-world act
(the class `corpus intake`'s own catalog record exemplifies) inside or
outside the determinism law D8/D9 states? Options: (a) outside — a
foreign corpus's arrival date is genuinely wall-clock provenance; the
default stands and a CLASS EXEMPTION is recorded so future
provenance-of-real-world-acts flags cite it rather than re-litigate
flag by flag; (b) inside — require the flag explicitly, no wall-clock
defaults anywhere. RULED (a). Concrete meaning: the exemption's scope
is exactly "provenance metadata recording a real-world act" — anything
that generates or transforms corpus CONTENT remains fully inside the
determinism law, unaffected.

All three recorded verbatim in `.agents/rulings.md`, "From ADR-0115"
(new dated section, matching the file's own existing shape).

**[A] Register fix-forward.** In the findings register: R3-B1-1's
disposition moved `ruling-needed` → `fix-session-candidate (cluster
A)`; R3-B1-4's moved to `fix-session-candidate (cluster A, small: the
help note only)`; R3-B1-7's moved to `closed-by-ruling` (no fix
session needed — the exemption itself is the resolution). Each row's
recommendation cell gained a one-line `RULED (a), ADR-0115 RQx,
2026-08-12` citation. The summary table's own note claiming a
cross-referencing row is "marked '(x-ref)'" was corrected — no row
ever carried that literal token; the actual marker is a citation,
"(see R3-Bx-y, not double-counted)" style text — and a dated
correction paragraph was added beneath the table, disclosing the fix
and confirming counts are unaffected (independently recounted by the
design channel, 2026-08-12). One further sentence, licensed by this
session's own driving prompt, states plainly that the table is the
review's own review-time snapshot and three of its `ruling-needed`
entries have since moved (cited above) — the rows, not the table,
carry current state, so a future reader comparing the two isn't
looking at a contradiction.

**[A] Fix clusters chartered** (`.agents/plans/roadmap.md`, Next
section, three new rows plus a queue note):

- **Fix cluster A — CLI validation and error quality.** Contains the
  register's own HIGHEST-PRIORITY finding. Members: R3-B2-1 (`check`
  target validation — HIGHEST), R3-B2-2 (parse-error translation),
  R3-B2-3 + R3-B4-1 (`corpus intake --out` validation-or-derivation,
  one fix), R3-B1-5 (missing-required-flag exit-code/category
  unification), R3-B1-3 (`synthea:` source-scoping validator
  extension), R3-B2-5 + R3-B3-3 (`help <unknown-group>` validation),
  R3-B1-1 (the `--scratch-dir` rename, RQ1 above), R3-B1-4 (the
  tiering help note, RQ2 above). A `src` session; its own future
  prompt (channel-drafted) pre-analyzes the oracle bracket — error-path
  changes are expected oracle-neutral, but that session declares it,
  not this one.
- **Fix cluster B — help-surface enrichment.** Members: R3-B3-2
  (verb-level help narrowing), R3-B3-1's own mechanism half (the
  "Example:" render slot — content is design-channel-draft, queued
  below, not this cluster's own scope).
- **Fix cluster C — doc drift and gate scan-roots.** Members: R3-B5-3
  (`demos/traces` stale refs + widen the invocation gate's scan roots
  to `demos/**`), R3-B5-4 (issue template fix + consider `.github/**`
  in scan roots). Docs-only session.
- **Queue note (not a session row):** the design-channel-draft queue —
  R3-B3-1's own Example-line content (one runnable invocation per
  group, sourced from `docs/use-cases/*.md`), and the B-3/B-4
  carry-forward wording halves (R3-B3-4) — the channel drafts, the
  author rules, no session chartered until then.

Review-3's own existing roadmap row gained one dated note: rulings
landed (this ADR), clusters chartered; the arc's remaining steps are
the three cluster sessions, then the user manual design pass.

### Deviations, dated 2026-08-12

**One gate-forced companion edit.** Charting the three clusters onto
`.agents/plans/roadmap.md`'s Next section grew that file from 746 to
781 lines — an `:onboarding` reading-set member
(`.agents/reading-sets.edn`) — which tripped
`ehrt.docs-tooling.reading-set-budget-test` red:
`:onboarding` measured 1734 lines against its own 1705-line budget.
Not named by filename in the driving prompt's own fence list, but
licensed by the prompt's own standing-practice note 2 (a gate-forced
companion to a named fenced surface, `.agents/plans/roadmap.md`, is
inside the fence by rule). `.agents/reading-sets.edn` re-baselined
under its own standing formula (actual x1.15, rounded up to the
nearest 5): 1734 x 1.15 = 1994.1 → 1995; budget moved 1705 → 1995, one
new dated re-derivation comment added matching the file's own existing
history-comment shape. `make test` confirmed green after the
re-baseline. No other deviation: every Read-first document matched
this session's own characterization of it; all three rulings, all
three register-row updates, and all three cluster rows plus the queue
note landed exactly as the driving prompt specified, verbatim where
the prompt gave verbatim text.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots — registers and notes
only (`.agents/rulings.md`, the findings register, `.agents/plans/
roadmap.md`, `.agents/reading-sets.edn`'s gate-forced companion edit),
zero `src`, zero `docs/`.

**Bracket result.** `bin/regression-oracle d508cd6 ed00e3a` (`ed00e3a`:
this session's own commit 1, the rulings-and-charter commit, run
before the close-phase commit, per this session's own driving prompt's
step ordering — commit 2 is prompt-archive/session-record/ADR text
only, guaranteed zero-`src` by the fence): `IDENTICAL: every root's
digest matches between d508cd6 and ed00e3a` — all 35 roots, matching
the pre-analysis exactly; no STOP-AND-REPORT needed.

### Full gate

`make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration`): green, both before and after the
`reading-sets.edn` re-baseline — every namespace 0 failures/0 errors.
`bin/verify-nist-lock`: OK, all 6 hit-nexus-sourced coordinates match
`artifacts.lock.edn` exactly. `gitleaks git --staged -v` (pre-commit,
each checkpoint) and `gitleaks detect` (pre-push): no leaks found,
across both commits. ASCII byte-check on both commit messages: clean.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): four `success`, one `failure`
(`31598555300`/`d0679e9`) — the ADR-0114-disclosed interim CI-red
window, already fixed forward at `d508cd6`; no new red among the five.

### Fences

Touched: `.agents/rulings.md` (the ADR-0115 rulings entry);
`.agents/plans/2026-08-12-review-3-user-surface-findings.md` (three
row dispositions, the summary-table note correction);
`.agents/plans/roadmap.md` (the three cluster rows, the queue note,
review-3's own dated note, the Done pointer);
`.agents/reading-sets.edn` (gate-forced companion, disclosed above);
`.agents/prompts/*` (self-archive plus its README index line);
`.agents/session-records/*` (this session's own record plus its README
index line); `notes/adr/0115-*.md` (this file); `notes/ADRs.md`;
`notes/adr/README.md`. ZERO changes under any `src/` or `test/` path
anywhere; zero fixes of any register finding, however trivial — no
file outside the list above was touched.

### Index line

```
- 2026-08-12 — review-3-rulings-landing — ADR-0115
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)
