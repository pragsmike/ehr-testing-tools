# 2026-08-08 — Fidelity payoff: anemia comes home — and colorectal's real blocker gets its true name

## Scope

Session prompt naming AR-FP-0 through AR-FP-3, executing the fidelity
arc's own payoff rider — reshaped by the EncounterEnd fix's own
in-session evidence (ADR-0082): the brief named two modules
(`anemia___unknown_etiology.json`, `colorectal_cancer.json`) as a
mini-batch, but the evidence licenses only one. Src/resource edits
confined to the anemia root file, its NOTICE row, its test, and its
oracle root, per the prompt's own fence — no colorectal vendoring, no
investigation of its own residual defect (intake, not act), no other
interpreter/engine/emitter changes.

Preflight: working directory confirmed the ext4 clone, HEAD `82d1753`
exactly (ADR-0082's own closing tip), working tree clean. The
pin-verified checkout (`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`) re-confirmed at its own
recorded commit; the in-tree `anemia/anemia_sub.json` re-hashed against
it, byte-identical. `clojure -M:poly check` OK; full suite green (521
assertions, 0/0, matching ADR-0082's own reported baseline exactly);
oracle pre-digest (all 27 roots) recorded to a scratch manifest;
last-five CI runs on `main` disclosed (four green, one already-closed
red from ADR-0082's own session — no new red window at this session's
own Step 0). AR-FP-0 executed directly:
`stable-20260808-encounterend-fix` annotated and pushed at `82d1753`,
peeled ref verified both locally and via `git ls-remote`.

## Steps and commits

**Step 1 (`841df9a`, AR-FP-1).** `anemia___unknown_etiology.json`
copied byte-verbatim from the pin-verified checkout (root file only —
`anemia/anemia_sub.json` already vendored via `hypothyroidism`'s own
closure, reused). `vendored_anemia_test.clj` authored, witnessed red
(the resource moved aside, `Cannot open <nil> as a Reader`, then
restored — no working-tree stash), committed green (2 tests, 13
assertions) — real compiled content, zero `check/check-all` violations
at all three of the deferral's own seeds (20260802/1/42, 300 patients,
race-weighted `:persona-config`), real rendered HL7, and a pinned
`:suppressed-encounter-ends` total per seed (33/23/20 — real,
empirically-run numbers, not estimated) via a direct interpreter-layer
walk sweep (`sim-trajectory/run-module`, the same call shape
`census.clj`'s own `walk-one` uses, since `engine/run` never surfaces
this field). NOTICE gained one row plus a dated section;
`notice-verbatim-test` re-run green (143 assertions, up from 141).
Full suite green throughout. Pushed; post-push message verified (one
delta, the known trailing-blank-line artifact).

**Step 2 (`85ba040`, AR-FP-1/2).** `digest.clj` gained `anemia-pair`
(race-weighted, same convention) as the 28th root, purely additive —
the 27 pre-existing roots confirmed byte-identical both by manual
manifest diff and the official `bin/regression-oracle 841df9a 85ba040
--declared-digest-change` bracket (`DIFFERS`, EXPECTED — one addition,
zero changes). The roadmap's "EncounterEnd no-op-when-nothing-open"
Deferred row CLOSED (a dated note, the ALL-CAPS `CLOSED` marker plus
"see Done", satisfying `roadmap-deferred-closure-lint-test`) — both
modules it ever blocked are resolved: anemia vendors; colorectal was
NEVER actually blocked by this gap. The same in-session raw-trajectory
scan that cleared anemia (ADR-0082) found zero dangling
`:encounter-end` references in colorectal's own 300 seed-42 walks,
violations byte-identical pre/post-fix — ADR-0072's own "same root
cause" diagnosis was plausible by adjacency, never probe-verified,
corrected by a dated erratum appended to `notes/adr/0072-vendoring-
batch-3.md`. A new, colorectal-only Deferred row names its real,
undiagnosed blocker (`:clinical-content-only-when-admitted`) — revisit
trigger: its own investigation session, intake for the arc's own
close. Full suite green throughout. Pushed; post-push message verified
(one delta, the known artifact).

**Step 3 (this record).** `notes/adr/0083-fidelity-payoff.md` authored
directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own file count corrected (80→81, verified by
`ls`); roadmap Done pointer added; session record and prompt archive
land; successor tag debt recorded in the ADR
(`stable-20260808-fidelity-payoff` at this session's own closing tip).

## The evidence that reshaped the brief

The brief's own payoff rider named two modules as a mini-batch. The
EncounterEnd fix's own in-session proof (ADR-0082, AR-EE-3), run
against the pin-verified checkout at both deferrals' own exact seeds,
licensed only one: anemia's violations fully extinguished (0/300 at
all three seeds), colorectal's byte-identical pre/post-fix with zero
dangling ends found anywhere in its own raw walks. This session treats
that finding as ground truth rather than re-deriving it — the vendoring
proceeds for anemia only, and colorectal's own roadmap row is corrected
by erratum rather than left to imply a gap the evidence already closed.

## Verification

See `notes/adr/0083-fidelity-payoff.md`'s own Verification section for
the full account (digests, hashes, suite counts, gitleaks, CI runs, tag
verification) — not restated here.

## Deviations, disclosed

- **The brief's own "anemia and colorectal as a mini-batch" framing
  narrows to anemia-only** — disclosed in ADR-0083, the roadmap, and
  this record, per the evidence ADR-0082's own session already
  recorded, not re-derived or silently narrowed here.
- **The resource-then-test ordering ran backward from the batch
  precedent's own "test first, red on missing resource" sequence** —
  the anemia root file was copied to disk before
  `vendored_anemia_test.clj` was authored; red was still witnessed
  honestly by moving the resource aside and re-running before
  restoring it and confirming green, satisfying the same red-then-green
  discipline via a different mechanical path, disclosed here rather
  than silently presented as the standard ordering.
