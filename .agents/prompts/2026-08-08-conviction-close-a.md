# 2026-08-08 — ehr-testing-tools: the conviction arc closes, session A (appends + cadence)

## Context

Conventions read at HEAD `f8df2cc` (pairing registry, ADR-0088), design
channel, 2026-08-08, verified by fresh public clone. The conviction arc
(ADR-0085–0088: colorectal investigation → straddle fix → payoff →
pairing registry) closes. The author ruled 2026-08-08, verbatim:
**"Close. adopt, two close sessions."** — adopting ADR-0084's own
intake suggestion ("a first session scoped to Steps 0–1 only, a second
to Steps 2–3"), first executed HERE. This is **close session A**:
appends and cadence only. State regeneration, budgets, rotation, and
ADR-0089 belong to session B, which the design channel authors AFTER
independently verifying this session's landing — that verification gap
is the adopted pattern's whole point, not an accident of scheduling.

R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward,
record HEAD (expect `f8df2cc`; later escalates unless explained).
Commits land green.

## Read first

1. `notes/adr/0084-fidelity-arc-close.md` — the close precedent: Step 1's
   appends-plus-cadence shape (AR-FC-1/2), the `libs :outdated` report
   format, and the intake suggestion this split executes (~line 392).
2. `.agents/rulings.md` — the append target; mirror the existing arc
   sections' format exactly ("From the <arc> arc (ADR-NNNN–NNNN)").
3. `notes/adr/0085…0088` — the arc being distilled; the law texts below
   cite them.

## Author rulings

- **AR-CA-0 [A]** (ADR-0088, "Successor tag debt"): tag
  `stable-20260808-pairing-registry` at `f8df2cc`, Step 0, standing
  ceremony (design-channel verified 2026-08-08). Verify-and-disclose if
  present.
- **AR-CA-1 [A — ratified by the author's paste of this prompt; edit
  before pasting to amend]**: append to `.agents/rulings.md` a new
  section **"From the conviction arc (ADR-0085–0089)"** with exactly
  these two laws:
  (i) **Witnessed rows only** — the pairing registry (ADR-0088) holds
  per-operator rows that exist ONLY when the mutate→judge loop was
  actually executed against a real fixture; unwitnessed cells do not
  appear; every pinned expectation is MEASURED before it is written
  (a wrong first measurement is disclosed, never silently discarded —
  ADR-0087/0088's own precedent); tier promotions (report-only →
  gating) happen only by dated author ruling.
  (ii) **Licenses bind at their own granularity** — a licensed oracle
  mover is licensed by NAME and at the EVIDENCE GRANULARITY the license
  states (ADR-0086: `sleep-apnea`, walks #17/#58/#269); the
  post-change bracket must match at that granularity, and any
  deviation — a different root, a different walk set, a surprise
  identical — is a fresh STOP-AND-REPORT, never absorbed by the
  existing license.
- **AR-CA-2 [A — standing cadence rule]**: re-run `clojure -M:poly
  libs :outdated` and record the full report against the AR-FC-2
  baseline (ADR-0084) — diffs are NOTES for next-arc intake, never
  acts; no `deps.edn` edit under any finding.
- **AR-CA-3 [A]** (the pre-split adoption, recorded): this session's
  record states the ruling verbatim and that this close is its first
  execution; the formal adoption record lands in ADR-0089 (session B).
- **AR-CA-4 [C]** (the inter-session seam): this session's own closing
  tip is session B's Step-0 tag (`stable-20260808-conviction-appends`),
  to be created ONLY by session B after the design channel verifies
  this landing — record that debt in this session's record. No ADR
  this session; ADR-0089 is session B's.

## Steps

**Step 0 — Preflight + tag (AR-CA-0).** Standard preflight (clean tree,
HEAD `f8df2cc`, untracked disclosure, `clojure -M:poly check`, oracle
pre-digest `f8df2cc f8df2cc` — 29 roots IDENTICAL, last-five CI
disclosed). Tag. No commit.

**Step 1 — Appends + cadence (AR-CA-1/2).** The rulings section; the
cadence report captured for session B's ADR. Targeted gates green
(`clojure -M:poly check`; docs-tooling gates touching rulings.md, if
any). Full suite is NOT required this session (docs-only appends; the
suite ran green at `948f5e5` and no src path is touched — disclose this
reasoning in the record). `gitleaks` clean. Commit:

    docs: the conviction arc's law is appended — witnessed rows only, licenses bind at their own grain (arc close A, AR-CA-1/2)

Push; verify message; watch CI to conclusion.

**Step 2 — Ceremony.** Session record (including the AR-CA-3 statement,
the AR-CA-4 debt, and the cadence report verbatim) + this prompt
archived (`2026-08-08-conviction-close-a.md`), both READMEs, same
commit:

    docs: session record and prompt archive — conviction arc close A

Push; verify; watch CI.

## Fences

NO `state.md` edit (session B's — the staleness tripwire's citation
mechanics depend on it). NO budget re-derivation, NO Done rotation, NO
reading-set edit, NO ADR authoring, NO roadmap edit. No src/test/deps
touches of any kind.

## Close-out

Echo to chat: the two appended law texts as landed, the cadence report,
shas, CI status.

## Deviation record

**The tag-creation slip.** Step 0's tag was first created with plain
`git tag <name> <commit>` (no `-a`), landing a LIGHTWEIGHT tag —
inconsistent with every prior `stable-*` tag in this repo, all of which
are annotated with a message. Caught by this session's own verification
step (`git cat-file -t` returned `commit`, not `tag`) before any
downstream reliance. Corrected in place: the lightweight tag deleted
both locally and on the remote, recreated annotated (message
`stable-20260808-pairing-registry at f8df2cc (pairing registry landed,
ADR-0088)`), re-pushed, re-verified (peeled ref resolves exactly to
`f8df2cc`, both locally and via `git ls-remote`). No other deviation
from this prompt's own steps or fences.
