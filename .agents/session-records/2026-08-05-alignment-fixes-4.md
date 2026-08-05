# 2026-08-05 — Alignment fixes 4: offline determinism without redistribution — the NIST mirror lives user-side, the lockfile grows teeth

## Scope

Session prompt naming AR-F4-0 through AR-F4-5. Prior: alignment fixes 3
landed and was design-channel-verified (`57ba010`,
`notes/adr/0052-alignment-fixes-3.md`). Register row A-4
(`.agents/plans/2026-08-05-alignment-audit-findings.md`) found this
workspace still resolving the six NIST HL7 v2 engine coordinates live
from `hit-nexus.nist.gov`, a no-SLA host that just changed operators —
and found the existing fix prescription (vendor the jars into an
in-repo `file://` mirror, `components/judge-v2-nist/deps.edn`'s own
comment block, sourced from the archived spike-notes document) FORECLOSED
by `notes/tools/ADRs.md` ADR-0005's 2026-07-24 amendment: these
coordinates are use-permitted only as a user-initiated fetch, and this
repo redistributes none of them. This session built the ruled
alternative — a mirror the USER builds outside this repo
(`bin/mirror-nist`), mechanized lockfile verification
(`bin/verify-nist-lock`, wired into `make test`), and swept every
surface still prescribing the foreclosed path. Full account, rulings
verbatim: `notes/adr/0053-alignment-fixes-4.md`.

## Red→green evidence highlights

- **A genuine gate catch, this session's own real red→green.**
  `ehrt.cli.executable-bits-test` failed during Step 3's own suite run:
  `bin/mirror-nist`/`bin/verify-nist-lock` were executable on disk but
  still `100644` in the git index (this clone's `core.fileMode=false`
  hid the mismatch locally — a fresh CI clone would not have). Fixed
  with `git update-index --chmod=+x` on both files; full suite re-run
  green immediately after (214 `Test results:` lines, 0
  failures/errors). Landed in this step's own commit, not amended into
  the already-pushed Step 1 commit.

- **`bin/verify-nist-lock`, scratch-fixture red.** Against a scratch
  repo dir with one deliberately wrong-sha jar: `MISMATCH ... exit 1`,
  the offending coordinate named with both the expected and actual
  sha256. Against an empty scratch dir: distinct `not yet resolved ...
  exit 2`, naming all six missing coordinates — proving the two
  failure exits are genuinely distinct, not the same code reused.
- **`bin/verify-nist-lock`, real-cache green.** Against `~/.m2/repository`
  (already populated by prior sessions): `OK: 6 hit-nexus-sourced
  coordinate(s) match artifacts.lock.edn exactly`, exit 0.
- **`bin/mirror-nist`, same red/green shape.** The scratch mismatch
  aborted with nothing copied (`$SCRATCH_DEST` never created); the real
  proving run against `~/.m2` built `~/.ehrt/nist-mirror/` with all 12
  files (jar + pom × 6) at correct Maven-layout paths, verified by
  `find`.
- **Makefile wiring, live.** `make verify-nist-lock` and `make
  mirror-nist` both run standalone; a full `make test` run (green,
  `clojure -M:poly check` OK, 214 `Test results:` lines, 0 failures)
  printed `bin/verify-nist-lock`'s own `OK` line as the lane's last
  step — the wiring is exercised, not just declared.
- **`components/judge-v2-nist/deps.edn` diff confirmed comment-only**
  by direct `git diff` read: `:deps`, `:mvn/repos`'s guidance, and
  `:aliases` are byte-identical to before; only the "Determinism note"
  paragraph's prose changed.
- `bin/regression-oracle 57ba010 d43c143`: all eleven vendored-root
  batches byte-identical — expected, since this session touched no
  `src/` at all.

## Judgment calls and disclosures

- **Makefile wiring target determined by inspection: `test`.**
  `projects/conformance/deps.edn` declares both the `nist-hit`
  `:mvn/repos` entry and `poly/judge-v2-nist` — confirmed by direct
  read and by facts register F9 — so `make test`'s own `clojure -M:poly
  test :all skip:integration` step is where the six coordinates
  actually resolve. Neither `integration` nor `quickstart` (the
  prompt's own named candidates) turned out to be it. `ci-parity`'s own
  independent inline replica of the two `poly` commands was left
  unmodified, deliberately — out of this ruling's own named scope.
- **Tree-wide sweep found one real annotate-not-edit case.**
  `components/corpus/docs/research/judge-v2-nist-spike-notes.md` — "Archived
  verbatim from a Cowork cloud session," the in-repo landed copy of the
  design-channel wiring notes themselves — still prescribed the
  foreclosed `file://` pattern in its own item 4, dated six days after
  ADR-0005's amendment (the spike session never accounted for it). Per
  this session's own fence, a dated annotation was added above the
  file's existing archival header; the original body, including item
  4's own prose, is untouched byte-for-byte. One other hit
  (`.agents/prompts/2026-08-05-alignment-audit.md`, an already-archived
  session prompt) was correctly historical, no action. `notes/tools/ADRs.md`
  ADR-0005's own original "Consequence" clause was found already
  superseded in place by its own inline 2026-07-24 amendment — the ADR
  discipline working as designed, not a gap.
- **`artifacts.lock.edn` pom sha256 extension declined, disclosed.**
  Offered as optional by AR-F4-4(c); judged out of scope — Maven's own
  resolver validates POM structure at use time, and the jar sha256
  already carries this repo's actual supply-chain concern.
- **AR-F4-2's environment fence held throughout, verified both
  directions.** `~/.m2/settings.xml` did not exist before this session
  and does not exist after it; the only path written outside the repo
  tree is `~/.ehrt/nist-mirror/`, exactly the licensed proving-run side
  effect AR-F4-1 names.
- No other deviation. All six NIST coordinates were already resolved
  in `~/.m2` at session start (prior sessions' own doing); Step 0's
  "force jar resolution if absent" contingency never fired, disclosed
  as a non-event rather than silently skipped.

## Findings and HEAD landed

No findings beyond the disclosures above, all immaterial to execution
— the register's own A-4 risk now has a mechanized, license-respecting
answer, and no other row was touched. Commits, in order: tag
`stable-20260805-alignment-fixes-3` (AR-F4-0, created and pushed at
`57ba010`), `a659cbf` (Step 1, the two scripts + Makefile wiring),
`d43c143` (Step 2, the surface sweep + ops doc), and this session's own
closing records commit (Step 3).
