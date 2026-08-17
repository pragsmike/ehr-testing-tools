## ADR-0073 — Demos front door: the operator surface moves to the front, and the README shows it running

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: vendoring batch 3 landed and was design-channel-verified
(`notes/adr/0072-vendoring-batch-3.md`, tip `721adb6`). This session is
the vendoring arc's fifth — a docs-relocation session, NOT a vendoring
batch (batch 4 stays unruled, unscheduled). It executes the author's
own direction, recorded verbatim as intake in ADR-0072's own AR-VB3-3:
"let's work more on the UX for practitioners, particularly the demos.
I want to move the sim demos to a top-level demo place, and feature
them in the intro materials." A design-channel proposal on the
structure was ruled by the author on three counts before this session's
own prompt was written (design channel, 2026-08-07): top-level `demos/`
with `scenarios/`/`traces/` subdirectories; vacated component paths get
pointer READMEs (the `CLAUDE.md`→`AGENTS.md` pattern); the
sim-emit-hl7 `site-profiles` demo moves too, back into the same tree as
its now-sibling demos.

Doctrinal ground, both restated in `.agents/rulings.md`: two-voices-two-
homes (from ADR-0062 — demos are operator-facing product surface, the
component-local docs tree is a maintainer home) and promotion moves
equipment, it does not improve it (AR-P-4, from ADR-0044 — files move
verbatim; the only in-transit edits are path citations, a mechanical
consequence of the move).

Read-first covered `.gitattributes` in full, every demo/scenario
README and config, `CLAUDE.md`'s pointer-stub pattern,
`.agents/reading-sets.edn`, the docs-tooling gate sources (not just
`.agents/state.md`'s summary of them), and the top-level `README.md`/
`SETUP.md` heads.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-DM-0 `[A — tag law, case (ii); debt recorded in ADR-0072]`.**
Annotated `stable-20260807-vendoring-batch-3` at `721adb6`, message
"vendoring batch 3 landed, design-channel-verified 2026-08-07
(ADR-0072)"; pushed; peeled ref verified — resolves exactly to
`721adb6`.

**AR-DM-1 `[A — the three rulings above; C for mechanics]` (the
move).** `git mv`, verbatim: `components/sim/docs/scenarios/*` →
`demos/scenarios/`; `components/sim/docs/demos/*` → `demos/traces/`;
`components/sim-emit-hl7/docs/demos/site-profiles/` →
`demos/traces/site-profiles/`. Seven precious `messages*.txt`
transcripts sha256-witnessed identical before and after. The
`.gitattributes` `-text` pattern rewritten to
`demos/traces/**/messages*.txt` in the SAME commit as the files it
protects. Three vacated directories each keep one pointer README.
Path citations swept to root-resolvable forms, live docs repo-wide,
frozen archives left untouched. `.agents/reading-sets.edn` carries no
member path under any moved tree — confirmed, nothing to update.

**AR-DM-2 `[C — authored content, landed verbatim from the appendix;
the author has seen it]` (the front door + the intro feature).**
`demos/README.md` and the top-level `README.md`'s new "See it run"
section land per the design channel's own appendix. `SETUP.md` gains
one cross-link line to `demos/`; the two named use-case pages gain
cross-links. Live probe: the appendix's own two commands run to
completion from a clean `out/` state, the board renders, witnessed
below.

**AR-DM-3 `[C — scope]` (fences).** Docs + `.gitattributes` pattern
relocation only — no `src/`, no `test/`, no gate-code changes, no
config/resource behavior changes, no demo-content rewrites beyond the
path-citation class, no batch-4 vendoring. The oracle bracket must
show all twenty-seven batches identical. Standing untracked files
untouched.

### Byte-witness table (AR-DM-1)

Every `messages*.txt` under the moved trees, sha256 before and after
`git mv`, identical:

| file (new path) | sha256 |
|---|---|
| `demos/traces/boarding-transfer/messages.txt` | `decf0cbf5af00d101137e8363775ee37976290385b503b8f180606f4889b970c` |
| `demos/traces/emit-state/messages.txt` | `71c4107163a907c10f27ff7ce7317a84f2701b3770338a29f736fb31aa4fb30d` |
| `demos/traces/module-mix/messages.txt` | `80755696d3e44a8df957c39008df872979629cc6e4082cd0962a442a77cbfb4c` |
| `demos/traces/order-result/messages.txt` | `76cb687c2923cdbcad4e728fc284f58bd1267dd27b5a44d004da35fa14313303` |
| `demos/traces/persona-enriched/messages.txt` | `45ac033a9853a09cc5750c745aeb638842e3f2cbc586a0a5415095ca03705947` |
| `demos/traces/site-profiles/messages-aldric.txt` | `66d7e010c2d1a58608b820054d9de4d4397c39a3faf743bdef1afee3611ac075` |
| `demos/traces/site-profiles/messages-default.txt` | `2f75e1c90114b646dc7794484f8ba54227a859339b7ec675a19816793df6131a` |

`git check-attr text` re-confirmed `-text` (unset) at all seven new
paths after the `.gitattributes` rewrite landed in the same commit.

### Sweep enumeration (AR-DM-1)

Path citations updated to root-resolvable forms, inside the moved
files' own fences/cross-references:

- `demos/traces/order-result/README.md`, `demos/traces/emit-state/README.md`
  (two commands), `demos/traces/module-mix/README.md`,
  `demos/scenarios/busy-tuesday/README.md`,
  `demos/scenarios/busy-tuesday/config.edn` (self-citing header
  comment), `demos/traces/site-profiles/README.md` — `--config` fence
  values retargeted.
- `demos/scenarios/README.md` — its sibling changed NAME, not just
  location (`components/sim/docs/demos/` → `demos/traces/`, not
  `demos/`, since the new top-level `demos/` is a different document
  entirely, the front door): the "Sibling of `../demos/`" link and two
  bare-text `demos/` mentions became `../traces/`/`traces/`.
- `demos/traces/README.md` — the `site-profiles/` Contents bullet and
  its closing parenthetical previously described site-profiles as
  relocated to another component's own doc tree; since this move puts
  it back as a direct sibling, that description would now be actively
  false, not merely a stale path token, so both were rewritten to
  match the shape of every other Contents bullet (a judgment call
  under AR-DM-1's "mechanical consequence of the move," disclosed
  here rather than silently taken as a prose improvement, AR-P-4).

Live docs repo-wide citing the old paths, swept the same commit:
`docs/site-profiles.md` (one live citation retargeted; its own dated
D1a-rider historical note, correctly narrating a past repair, left
untouched), `docs/simulate-your-facility.md` (the `demos/` link
retargeted), `components/corpus/test-fixtures/v2-nist/NOTICE.md` (a
`.gitattributes`-precedent citation retargeted),
`projects/conformance/test-fixtures/sim-configs/full-capability.edn`
(a comment-only citation retargeted — no functional resource change).

Confirmed left untouched, correctly: every frozen archive hit
(`notes/adr/`, `notes/prompts/`, `.agents/session-records/`,
`.agents/prompts/`, dated one-shot `.agents/plans/*.md` files) — these
narrate history, not current state, per the same scoping discipline
`ehrt.docs-tooling.stale-path-test`'s own family uses throughout. One
non-archive hit fenced out deliberately:
`components/docs-tooling/test/ehrt/docs_tooling/invocation_lint_test.clj:159-160`
carries the literal string `components/sim/docs/demos/module-mix/
config.edn` as mechanism-sanity fixture data (proving the flag-value
extractor works, not asserting the path resolves) — a `test/` file,
out of scope under AR-DM-3's own fence.

### Deviations, disclosed

- **Fence language, `See it run`'s own command block: `` ```sh `` →
  `` ```bash ``, not the appendix's literal tag.**
  `ehrt.docs-tooling.quickstart-fresh-test` anchors README.md's
  Quickstart extraction on the literal string `` ```sh `` — the FIRST
  such fence in the file, not one scoped to any heading. Landing "See
  it run" (placed before Quickstart) with `` ```sh `` made it that
  first fence, hijacking the extraction: witnessed RED
  (`readme-count` 5 vs. `script-count` 15). Fixed forward to
  `` ```bash `` — accepted equally by
  `ehrt.docs-tooling.invocation-lint-test`'s own fence scanner (which
  matches `bash` or `sh`), invisible to `quickstart-fresh-test`'s
  literal anchor, and already the convention every demo README in this
  repo uses. Witnessed GREEN after (both gates).
- **A stale `out/corpus/busy-tuesday` directory removed before the live
  probe.** Left over from an unrelated prior-session run (different
  seed/module mix, dated 2026-08-06) at the exact output path the
  appendix's own command writes to. `generate`'s own byte-reproducible
  never-overwrite contract refused to run over it, naming `rm -rf
  out/corpus/busy-tuesday` as its own remedy. Removed — a stale
  generated artifact, not the "standing untracked files" AR-DM-3's own
  fence protects (source-adjacent scratch work, not a disposable
  `out/` directory the tool itself tells you to clear).
- **`demos/traces/README.md`'s `site-profiles/` bullet and
  `demos/scenarios/README.md`'s `demos/`→`traces/` renames** — see the
  Sweep enumeration, above; both disclosed there as judgment calls
  under the mechanical-consequence clause, not silent prose
  improvements.

### Execution record

**Step 0 (preflight + tag).** Cwd confirmed the ext4 clone (`df -T .`
→ `ext4`), tip `721adb6`, working tree clean. `clojure -M:poly check`
OK; full suite green (`clojure -M:poly test`, 484 test blocks, 0
failures/0 errors, confirmed failure-free across the entire run's own
output by grep); oracle pre-digest (`bin/regression-oracle 721adb6
721adb6`) confirmed all twenty-seven existing roots IDENTICAL.
AR-DM-0 executed directly: `stable-20260807-vendoring-batch-3` created
annotated at `721adb6` (unsigned, matching every predecessor tag's own
format), pushed, peeled ref verified — resolves exactly to `721adb6`.

**Step 1 (`f07684c`, AR-DM-1).** Pre-move sha256 recorded for all seven
`messages*.txt` transcripts. `git mv` landed all twenty-seven files as
detected renames; `.gitattributes` rewritten in the same commit;
`git check-attr text` confirmed `-text` at the new paths before commit.
Three pointer READMEs written
(`components/sim/docs/demos/README.md`,
`components/sim/docs/scenarios/README.md`,
`components/sim-emit-hl7/docs/demos/README.md`). Sweep enumerated
above landed. Post-move sha256 re-derived: identical hash sets,
confirmed by direct diff. `clojure -M:poly check` OK; full suite green
(484 test blocks, 0/0, whole-log grep); all four named docs-tooling
gates exercised and green (`invocation-lint-test` 195 assertions,
`readme-presence-test` 23, `stale-path-test` 166,
`reading-set-budget-test` 15 — the last trivially, since no reading
set carries a member path under either moved tree).
`gitleaks git --staged -v`: clean. Pushed; post-push verification: one
delta, the known trailing-newline artifact.

**Step 2 (`d0296d2`, AR-DM-2).** `demos/README.md` and README.md's "See
it run" section landed per the appendix (fence-language deviation
above, disclosed). `SETUP.md` cross-link landed. `components/corpus/
docs/use-cases.edn`'s two named cases gained cross-link sentences;
`make use-cases` regenerated `docs/use-cases.md` and
`docs/use-cases/*.md` — `git status` confirmed only the two intended
pages changed. Live probe: stale `out/corpus/busy-tuesday` removed
(disclosed above); `bin/ehrt corpus generate sim --seed 5 --patients
200 --reference-date 2026-08-04 --churn --config demos/scenarios/
busy-tuesday/config.edn --out-dir out/corpus/busy-tuesday` →
`{:status :ok, :payload {:out-dir "out/corpus/busy-tuesday"}}`, 74
messages; `bin/ehrt play out/corpus/busy-tuesday --board 60 --rate 60`
ran to completion → `{:status :ok, :payload {:unparseable-count 0,
:snapshot-count 74, :skip-count 48, :rate 60.0, :idle-cap-ms 5000,
:wallclock-ms 244298, :stream-span-ms 307855920000, :clamped-count 0,
:emitted 74, :unfolded-count 0, :sink "ticker"}}` — every one of the
74 messages rendered a board snapshot, none dropped. `clojure -M:poly
check` OK; full suite green (314 assertions, 0/0, whole-log grep) —
first pass caught `quickstart-fresh-test` RED (the fence-language
collision, disclosed above), fixed forward, re-verified GREEN both
targeted and full-suite. `gitleaks git --staged -v`: clean. Pushed;
post-push verification: one delta, the known trailing-newline
artifact.

**Step 3 (this record).** Oracle bracket `bin/regression-oracle
721adb6 d0296d2`: all twenty-seven roots IDENTICAL. README count under
`demos/` confirmed by `ls`/`find`: 10. `notes/ADRs.md` index line
appended. Roadmap Done pointer lands. Session record and prompt
archive land, both indexed in their own READMEs.

This session's own successor tag debt: `stable-20260807-demos-
front-door` at this session's own closing tip is owed to the next
session's own Step 0, per tag law (ADR-0057 AR-T-1) — not created
here (no ruling licensed it at this session's own closing commit).

### Verification

- `bin/regression-oracle 721adb6 d0296d2`: `IDENTICAL: every root's
  digest matches` — all twenty-seven roots, the diff output itself the
  evidence, not a count comparison. Docs-only move, zero code touched,
  matching expectation exactly.
- Byte-witness: seven `messages*.txt` transcripts, sha256 before and
  after `git mv`, identical hash sets (direct diff, above);
  `git check-attr text` confirmed `-text` applies at every new path.
- Full suite (`clojure -M:poly test`): green at every checkpoint (Step
  0 baseline, post-Step-1, post-Step-2), 0 failures/0 errors,
  confirmed failure-free across each ENTIRE run's own output by grep,
  not just the tail.
- `clojure -M:poly check`: OK, every step.
- The four named docs-tooling gates
  (`ehrt.docs-tooling.invocation-lint-test`,
  `ehrt.docs-tooling.stale-path-test`,
  `ehrt.docs-tooling.readme-presence-test`,
  `ehrt.docs-tooling.reading-set-budget-test`): all green post-move.
  Correction to the driving prompt's own framing: `readme-presence-test`
  does not actually scope over `demos/` (its scan roots are `.agents/`
  and `notes/` only) — it ran green because it was untouched by this
  move, not because it judged the new tree; the ten READMEs under
  `demos/` are a convention this session followed, not a gate this
  session satisfied.
- `ehrt.docs-tooling.quickstart-fresh-test`: witnessed RED (readme-count
  5 vs. script-count 15) before the fence-language fix, GREEN after —
  the disclosed deviation's own proof.
- Live probe (AR-DM-2): both See-it-run commands ran to completion from
  a genuinely clean `out/` state; the board rendered a snapshot for
  every one of 74 emitted messages; transcript recorded in the
  Execution record, above.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-vendoring-batch-3` peeled ref
  resolves to `721adb6` exactly.

### Standing gate note (for the state.md regeneration this arc's close will run)

`.agents/reading-sets.edn` needed no path or budget change this
session — confirmed by direct grep of every set's own `:paths` against
both moved trees before AR-DM-1 landed: zero members under either.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Demos front door: the operator surface moves to the front, and the README shows it running — `demos/scenarios/`+`demos/traces/` land at the repo root, byte-witnessed, pointer READMEs left behind; README.md gains a "See it run" section, live-probed to a rendered bed board in two commands
