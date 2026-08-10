## ADR-0099 — Fixture relocation: the demos front door mechanic runs on test-fixtures/

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-10.

### Context

Prior: `notes/adr/0081-fidelity-riders.md` opened the fidelity arc and,
as an author backlog addition (AR-FR-2(a)), named a Next row: move
`components/corpus/test-fixtures/v2/simhospital` and its
`components/corpus/test-fixtures/v2-nist` sibling to a top-level home,
so demos can use them — flagging that both trees are NOTICE/PROVENANCE-
hashed and `-text` protected, so the demos-front-door mechanic
(`notes/adr/0073-demos-front-door.md` AR-DM-1: same-commit
`.gitattributes` moves, byte-witnessing, pointer-README stubs) applies.
The roadmap row itself noted `components/corpus/test-fixtures/fhir/`
(ADR-0091, storefront fixture) as a third member, not preempted.

This session executes that row. Two rulings were taken the day before
this session ran (design channel, 2026-08-09, recorded here per Step 8
and `.agents/rulings.md`'s own "From ADR-0099" section): the target
home is root-level `test-fixtures/` ("Q1 a."), and the ENTIRE tree
moves as one subtree-whole `git mv`, all four subtrees together — `v2/`,
`v2-nist/`, `fhir/`, and a fourth, roadmap-unnamed member,
`reports/pre-split-baseline.edn` ("Q2 a."), disclosed here as riding
along beyond the row's three named members.

R30 ceremony. Read-first: `.agents/plans/roadmap.md`'s Fixture
relocation Next row; `notes/adr/0073-demos-front-door.md` in full (the
mechanic reused); `notes/adr/0081-fidelity-riders.md` (the row's
origin); `.gitattributes` (all five patterns and comments);
`test-fixtures/v2/simhospital/PROVENANCE.md` and
`test-fixtures/v2-nist/NOTICE.md` (the prose citations that update);
`components/docs-tooling/test/ehrt/docs_tooling/
test_source_live_path_lint_test.clj`, `notice_verbatim_test.clj`,
`stale_path_test.clj`, `license_text_pointer_test.clj`;
`.agents/rulings.md` (tag law AR-T-1, ASCII-first verification).

**A material correction to the driving prompt's own channel-probed
sweep inventory, found by re-deriving rather than trusting it** (see
Sweep enumeration, below): the prompt's own claim of "16 `.clj` files
(63 lines)" undercounted nothing about the FILE set (a fresh `grep -rl`
across `*.clj` independently found the identical 16 files) but named
none of thirteen additional LIVE, non-`.clj` files a repo-wide
(`--exclude-dir=.git`) grep surfaced: most critically
`components/judge/resources/judge/pairing-registry.edn`, a runtime
resource whose twelve `:fixture`/`:profile` string values are the
ACTUAL paths `judge`'s own registry loads fixtures from — leaving it
unswept would not have been a doc-staleness gap, it would have broken
every pairing-registry-driven judge test the moment the fixture tree
moved. The other twelve: `components/corpus/docs/use-cases.edn` (the
generator SOURCE six of `docs/use-cases/*.md`'s pages render from —
the prompt's own "four" `docs/use-cases/*.md` was itself stale; the
live count is six), `docs/README.md`, `docs/dev/source-sink-design.md`,
`docs/formats.md`, `docs/judge-calibration.md`, `deps.edn` and
`projects/ehrt-cli/deps.edn` (both comment-only, explaining cwd-
relative resolution), and the two conformance baseline files
`projects/conformance/test-fixtures/reports/sim-v2-{gate,full-
capability}-baseline.edn` (comment-only, citing the OLD path of the
`reports/` member this same session also moves). All fixed forward in
the move commit, disclosed here rather than silently absorbed — see
Deviations.

### Decision

Author rulings, recorded verbatim (design channel, 2026-08-09, the day
before this session ran). `[A]` author-ruled, `[C]` channel-inferred.

1. **Target home** `[A, "Q1 a."]`. The new home is a root-level
   `test-fixtures/` directory, sibling to `demos/` — not nested under
   any component.
2. **Scope** `[A, "Q2 a."]`. The ENTIRE tree moves in one subtree-whole
   `git mv`: `v2/`, `v2-nist/`, `fhir/`, and `reports/` together — the
   last unnamed in ADR-0081's own row, riding along on this ruling,
   disclosed here.
3. **The roadmap row itself** `[A]`, including the wrinkles it names:
   `.gitattributes` protection, the demos-front-door mechanic, the
   live-path lint's blessed roots.
4. **Mechanics** `[C, verify-then-act]`: every item under "Channel-
   probed facts" in the driving prompt, including the zero-edit
   allowlist expectation (confirmed) and the sweep inventory (found
   incomplete — see Context and Sweep enumeration).

### Byte-witness table

sha256, all 15 files, before (`components/corpus/test-fixtures/`, tag
`stable-20260809-permission-legs-and-bare-flags` at `8d4b1ee`) and
after (`test-fixtures/`, commit `4cb139d`) the `git mv` — identical for
every actual fixture byte; only `NOTICE.md`/`PROVENANCE.md` change
(their own prose path citations, not fixture content):

| file (new path) | sha256 (pre = post, unless noted) |
|---|---|
| `test-fixtures/fhir/storefront-patient.json` | `1a596630ff177869d606f6dde3755c8b7d60060bbd932ddc45c0429f6e83705f` |
| `test-fixtures/reports/pre-split-baseline.edn` | `4bb48eb4c631da54b458698584ea5100702840970d1c19038a9d18f75c4cb6bd` |
| `test-fixtures/v2-nist/COVID19_ELR-v2.3.1/CONSTRAINTS.xml` | `9ed06afd7dc8fe2d0a2f418b28f15d7e0788a1259f570b14ece8911ee1dea0ee` |
| `test-fixtures/v2-nist/COVID19_ELR-v2.3.1/PROFILE.xml` | `5a709a2f719b2aa3ae900afba600f31e087ff3ee5a87bb550794f6b635fe4704` |
| `test-fixtures/v2-nist/COVID19_ELR-v2.3.1/VALUESETS-disabled.xml` | `a2f7c1c8242386edada70493b1563f9a2c85e9ebb12c7f3a20d97ef10edfa3e8` |
| `test-fixtures/v2-nist/NOTICE.md` | pre `a52ee8e4bf99bed8be3e3f1f8a45665b21fe9ae56761d1cf91c3456dd45d3f7c`, post `ed0567db61c66e25ea19bc03c01cb12ad01b1a68ed80a08ddbc145718d601101` (prose path citations edited, hash table untouched) |
| `test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt` | `83d3241d68e474f2ce1cf759a4379614447a67154ea9ce2dc6a9f36466d574fc` |
| `test-fixtures/v2/adt-a01-admit-repeated-identifiers.hl7` | `0aa93e3b6b9795c3d160efdf6dbf9cd099ad4e6ebd63aa7e0420bb46872e05ec` |
| `test-fixtures/v2/adt-a01-admit.hl7` | `7147d7db67afb06b42044589ea6ebb01570cd8387919bf1356843b8ed7f4d4de` |
| `test-fixtures/v2/adt-a02-transfer.hl7` | `9963b46b0ae22e4d8431e67748daffdcf161f32d4d8425da48fa14d9884a685f` |
| `test-fixtures/v2/adt-a03-discharge.hl7` | `1165d9e35ebf6ccb4ab9b49ed8f04fb3f0689fd6b489978cf40002634cb164c6` |
| `test-fixtures/v2/adt-a08-update-trailing-empty-fields.hl7` | `fd1af0c9a4a55c2829445fd8efc2ffbb9a6379d0de97a20db23874b718464c63` |
| `test-fixtures/v2/simhospital/LICENSE` | `58d1e17ffe5109a7ae296caafcadfdbe6a7d176f0bc4ab01e12a689b0499d8bd` |
| `test-fixtures/v2/simhospital/PROVENANCE.md` | pre `20fe8b61b52d8ff7cfa2cf95fb59d5d9647c35b01694c62467a845fcedce8de6`, post `0e0ab927564615624b5693cd9f51ab913b84d8109c3b6f66f1e59c9951853b79` (prose path citations edited, hash table untouched) |
| `test-fixtures/v2/simhospital/messages.out` | `fa9719a5f157391dcf78197e4239bce8af0382ae40b903d019a2773a1a9ff520` |

`git check-attr text` re-confirmed `unset` (i.e. `-text` applies) at
all eleven `-text`-matched paths both before the move (old paths) and
after (new paths).

### Sweep enumeration

Path citations updated to root-resolvable `test-fixtures/...` form, all
in the move commit:

- **`.gitattributes`** — all five `-text` patterns, comments' own path
  mentions included.
- **`test-fixtures/v2-nist/NOTICE.md`**, **`test-fixtures/v2/
  simhospital/PROVENANCE.md`** — every prose path citation (the
  `.gitattributes` cross-reference sentences, the repetition-
  preservation fixture cross-reference); hash tables untouched, since
  `notice-verbatim-test` resolves filenames relative to each NOTICE
  file's own directory and the subtree moved whole.
- **16 `.clj` files** (re-derived by `grep -rl` against `*.clj`,
  matching the driving prompt's own file count exactly, though not its
  line count — 59 lines actually changed, not 63):
  `bases/cli/src/ehrt/cli/{core,help}.clj` (the two doc-string
  literals — the `--profile` flag hint and the v2-nist try-it string),
  `bases/cli/test/ehrt/cli/core_test.clj`,
  `components/corpus-io/test/ehrt/corpus_io/er7_test.clj`,
  `components/corpus/test/ehrt/corpus/{display,mutate,player}_test.clj`,
  `components/corpus/test/ehrt/corpus/simhospital_corpus.clj`,
  `components/corpus/test/ehrt/corpus/v2_contract_pairing_test.clj`,
  `components/docs-tooling/test/ehrt/docs_tooling/
  license_text_pointer_test.clj` (the `canonical-source-file` literal),
  `components/judge-v2-hapi/test/ehrt/judge_v2_hapi/v2_test.clj`,
  `components/judge-v2-nist/test/ehrt/judge_v2_nist/v2_engine_test.clj`,
  `components/judge/test/ehrt/judge/report_test.clj`,
  `projects/conformance/test/ehrt/conformance/{judge_engine_parity,
  mutate_stdout_stdin_loopback,sim_gate_loop}_test.clj`.
- **Thirteen additional live, non-`.clj` files, found by re-deriving
  the sweep from a repo-wide grep rather than trusting the driving
  prompt's own list (see Context)**:
  `components/judge/resources/judge/pairing-registry.edn` (twelve
  RUNTIME `:fixture`/`:profile` values — the critical miss, see
  Context), `components/corpus/docs/use-cases.edn` (the generator
  source; `make use-cases` re-derived `docs/use-cases/*.md`),
  `docs/README.md`, `docs/dev/source-sink-design.md`, `docs/formats.md`,
  `docs/judge-calibration.md`, `deps.edn`, `projects/ehrt-cli/deps.edn`,
  `projects/conformance/test-fixtures/reports/sim-v2-gate-baseline.edn`,
  `projects/conformance/test-fixtures/reports/sim-v2-full-capability-
  baseline.edn`.
- **Generated docs, regenerated via `make docsgen`** (which also runs
  `make use-cases`): `docs/cli.md` (follows `help.clj`) and six
  `docs/use-cases/*.md` pages (follows `use-cases.edn`) —
  `acceptance-qa-of-vendor-corpora.md`,
  `judge-tier-calibration-studies.md`, `judge-user-supplied-data.md`,
  `piped-hl7-traffic-as-intake-source.md`,
  `profile-tier-hl7v2-conformance-gating.md`,
  `regression-baselining.md` (the prompt's own claim of "four" pages
  was itself stale — the live count, confirmed by grep before and
  after regeneration, is six; the other fourteen `docs/use-cases/*.md`
  pages carry no fixture-path citation and were correctly left
  byte-unchanged by the regenerator).
- **README.md, AUTHORS-GUIDE.md, `bin/ehrt` (comment line), `bin/
  quickstart-demo`** — per the driving prompt's own named sweep,
  confirmed live and swept.
- **Pointer README**: `components/corpus/test-fixtures/README.md`
  (new — the vacated directory had no README before this move; ADR-0073
  stub style).
- **`.agents/reading-sets.edn`** — confirmed by direct grep, zero
  member paths under either the old or new tree, no edit needed.
- **`.agents/plans/roadmap.md`** — the Fixture relocation row itself
  cites the old path in its own prose; left untouched by the move
  commit (it moves to the Done section, rewritten, in this session's
  own close-phase commit, matching every prior arc's own two-commit
  shape for a Next-row-to-Done transition).

Confirmed left untouched, correctly (frozen archives, the same scoping
`stale-path-test`'s own family uses): every `notes/adr/*.md` hit
(`0054`, `0060`, `0073`, `0081`, `0084`, `0088`, `0091`), every
`.agents/prompts/*.md` and `.agents/session-records/*.md` hit (all
dated, all pre-2026-08-10), the four dated one-shot
`.agents/plans/2026-08-0{5,6,7,9}-*.md` files, and
`components/corpus/docs/experiments/{EXP-B2,EXP-B2-results}.md` —
component-owned, dated experiment-record docs narrating a specific past
protocol run (the same historical-narration class ADRs and session
records carry), outside `stale-path-test`'s own `docs/` scan root and
outside this session's own live-doc fence for the same reason.

### Deviations, disclosed

- **The driving prompt's own sweep inventory was incomplete — widened,
  not deferred.** See Context and Sweep enumeration above for the full
  accounting. The most consequential miss,
  `components/judge/resources/judge/pairing-registry.edn`, was a
  functional-correctness gap, not a doc-staleness one: left unswept,
  every pairing-registry-driven judge test would have failed the
  moment the fixture tree moved (a red the full suite run below would
  have caught immediately, but only after the move commit already
  landed with a real defect in it). Per this repo's own fix-forward-
  with-disclosure discipline, the sweep was widened to match the
  actual live tree (re-derived by grep, not assumed) and landed in the
  SAME move commit — no code-logic change anywhere, every added edit
  is a path-string literal, consistent with the driving prompt's own
  fence ("every code edit is a path string").
- **`docs/use-cases/*.md` page count**: the driving prompt's own claim
  of "four" was stale; the live count (confirmed both before
  regeneration, by grep, and after, by `git status`) is six. No
  action beyond noting the correction — `make use-cases` regenerates
  exactly the pages whose source content changed, nothing more,
  nothing less, confirmed by `git status` showing precisely those six
  pages modified and no others.

### Execution record

**Step 1 (tag).** HEAD confirmed `8d4b1ee`, matching `origin/main`.
`stable-20260809-permission-legs-and-bare-flags` created annotated at
`8d4b1ee`, pushed; peeled ref verified (`git ls-remote origin
'refs/tags/stable-20260809-permission-legs-and-bare-flags^{}'`)
resolves exactly to `8d4b1eeefca7a8c70381820f7fa5f9f8148f8eb8`.

**Step 2 (pre-move witness).** sha256sum on all 15 files under
`components/corpus/test-fixtures/`; `git check-attr text` on all eleven
`-text`-matched files, all `unset`. Both recorded into the Byte-witness
table above.

**Step 3 (`4cb139d`, the move).** `git mv components/corpus/test-
fixtures test-fixtures` — all 15 files detected as renames (`git
status` confirmed `R` for 13, `RM` for the two prose-edited files).
`.gitattributes` rewritten in the same commit. NOTICE.md/PROVENANCE.md
prose swept (one citation in PROVENANCE.md missed on the first pass —
`test-fixtures/v2/adt-a01-admit-repeated-identifiers.hl7`'s own
repetition-preservation cross-reference — caught by the post-edit
repo-wide grep and fixed before commit, not after). The widened sweep
(30 files total: 16 `.clj` + 14 non-`.clj`) landed via a single `sed`
pass (`components/corpus/test-fixtures` → `test-fixtures`, an
unambiguous prefix substitution with no other meaning of that string
anywhere in the swept files) plus the two hand-edited prose files.
`make docsgen` regenerated `docs/cli.md` and the six affected
`docs/use-cases/*.md` pages; `git status` confirmed no other generated
page moved. Pointer README written. `.agents/reading-sets.edn`
confirmed clean. Final repo-wide grep (`--exclude-dir=.git`) confirmed
the only remaining hits were frozen archives plus
`.agents/plans/roadmap.md` (handled in Step 8's own commit). Staged;
`git diff --cached --stat` reviewed (54 files, 139 insertions/125
deletions, zero fixture-byte insertions/deletions — matching the "move,
don't improve" expectation exactly); `gitleaks git --staged -v` clean.
Committed (`4cb139d`), pushed. Post-push: one delta against the message
file, the known harmless trailing-newline artifact; ASCII check empty.

**Step 4 (post-move witness).** sha256sum at all 15 new paths,
diffed byte-for-byte against Step 2's own pre-move file: zero
difference (`diff` exit 0). `git check-attr text` re-confirmed `unset`
at all eleven new `-text` paths. `git status`: clean.

**Step 5 (lint verification).** All six named gates run explicitly (as
part of the full local suite, Step 7): `test-source-live-path-lint-test`
(129 assertions), `notice-verbatim-test` (157), `provenance-leaf-law-
test` (8), `stale-path-test` (167), `license-text-pointer-test` (13),
`quickstart-fresh-test` (14) — all green, ZERO edits to the live-path
lint's allowlist (`["test-fixtures" "config/synthea" "resources"]`
already matches the new root path literally, confirmed before relying
on it and confirmed again by the test's own green run).

**Step 6 (oracle bracket).** `bin/regression-oracle
stable-20260809-permission-legs-and-bare-flags 4cb139d`: all 34 roots
IDENTICAL — the diff output itself the evidence, matching the
Context's own prediction exactly (no `src/` touched, the two `.clj`
`src` edits both doc-string literals).

**Step 7 (full gate).** `clojure -M:poly check`: OK. Full local suite
(`clojure -M:poly test :all skip:integration`): 14,315 passes, 0
failures, 0 errors, confirmed failure-free across the entire run's own
output by grep (not just the tail); `ehrt.cli.cli-parse-guard-lint-test`
included in that run, 18/18 assertions green (unaffected — neither
this session's edits touch a bare-read call site the lint scans).
`bin/verify-nist-lock`: OK, all six NIST coordinates matched.

**Step 8 (this record).** Fixture relocation Next row removed from
`.agents/plans/roadmap.md`, Done line appended. `.agents/rulings.md`
gains "From ADR-0099" recording Q1/Q2 verbatim. `notes/ADRs.md` index
line appended. `notes/adr/README.md` count corrects 96→97, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic. Session
prompt self-archived to `.agents/prompts/2026-08-10-fixture-
relocation.md`; session record written to `.agents/session-records/
2026-08-10-fixture-relocation.md`; both indexed in their own READMEs.

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260810-fixture-relocation` at THIS session's own closing
tip, under standing ceremony** — the same tag-law case (ii) pattern
every prior close in this repo has used for its own predecessor.

### Index line

```
- 2026-08-10 — fixture-relocation — ADR-0099
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Verification

- `bin/regression-oracle stable-20260809-permission-legs-and-bare-flags
  4cb139d`: `IDENTICAL: every root's digest matches` — all 34 roots,
  the diff output itself the evidence, not a count comparison.
- Byte-witness: 13 of 15 fixture files sha256-identical before and
  after (the two prose files, NOTICE.md and PROVENANCE.md, changed
  only in their own path-citation text, disclosed and expected); `git
  check-attr text` confirmed `unset` at every `-text` path, both
  before and after.
- `clojure -M:poly check`: OK.
- Full suite (`clojure -M:poly test :all skip:integration`): 14,315
  passes, 0 failures, 0 errors, confirmed failure-free across the
  ENTIRE run's own output by grep.
- The six named docs-tooling gates: all green, confirmed individually
  in the full-suite log — `test-source-live-path-lint-test` (129
  assertions, ZERO allowlist edits), `notice-verbatim-test` (157),
  `provenance-leaf-law-test` (8), `stale-path-test` (167),
  `license-text-pointer-test` (13), `quickstart-fresh-test` (14).
- `bin/verify-nist-lock`: OK, 6/6 coordinates matched.
- `gitleaks git --staged -v`: clean before the move commit.
- Post-push message verification: one delta, the known harmless
  trailing-newline artifact; ASCII check empty.
- Tag verification: `stable-20260809-permission-legs-and-bare-flags`
  peeled ref resolves to `8d4b1eeefca7a8c70381820f7fa5f9f8148f8eb8`
  exactly.

### Consequence

The fixture tree that gated demos-visible fixtures behind a component-
internal path now sits at a top-level `test-fixtures/`, symmetric with
`demos/` — both the sim demos (ADR-0073) and the test fixtures now live
where an operator, not just a maintainer, can find them. The
`reports/` member rides along disclosed rather than left half-moved by
the roadmap row's own incomplete naming. The session's own sweep
discipline caught and fixed a functional gap the driving prompt's own
channel-probed inventory missed — `pairing-registry.edn`'s runtime
fixture paths — before it ever reached a red test, by re-deriving the
sweep from the live tree instead of trusting the prompt's own count.
