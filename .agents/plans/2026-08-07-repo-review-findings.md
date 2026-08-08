# Repo review 1 — the first assessment (findings register)

Findings-only register for the `repo-review` skill's own first survey
(`.agents/skills/repo-review/SKILL.md`, steps 1–4; `notes/adr/0077-
repo-review-1.md`). This is the arc's OPENING instrument, not a fix
session — every row is a recommendation, never an executed fix
(AR-RR-4); the only mutations this session made are the standing-
ceremony tag (AR-RR-0, licensed by ADR-0057 AR-T-1(ii)) and this
register itself.

Row format: `id | probe | evidence | finding | recommendation with
reasoning | proposed disposition`. Disposition ∈ {ruling-needed,
fix-session-candidate (with suggested cluster), close-as-fine,
intake}. Every row's evidence was gathered by the mechanism the
rubric names for its own dimension — re-derive, re-hash, re-run, never
re-read a claim as its own verification (six probe dimensions were
delegated to independent read-only sub-agents running in parallel,
each instructed under the same discipline; two — D1 and D5 — were run
directly by the landing session itself). Clean probes are recorded in
full alongside findings, per the skill's own "a green probe is
inheritance, not noise" instruction.

Landed 2026-08-07 (repo review 1, `notes/ADRs.md` ADR-0077), against
tip `89c0d24` (ADR-0076's own closing commit, tagged
`stable-20260807-quality-riders` at this session's own Step 0).

**This is the FIRST assessment.** There is no prior scoreboard column
— the scoreboard below is this run's own baseline, for the SECOND
review to carry forward.

---

## Dimension 1 — Claim–reality coherence

Probed directly by the landing session (mechanism: `ls`/`wc`/`grep`
re-derivation and direct-source counting, never trusting a prose
claim). All nine probes clean.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D1-1 | `ls -d components/*/ \| wc -l`; `ls -d bases/*/ \| wc -l` | 18 components, 1 base | Matches `.agents/state.md`'s claim exactly. | None. | close-as-fine |
| D1-2 | `grep -c '^\| \`' components/sim/resources/sim/modules/NOTICE` | 69 provenance rows | Matches `state.md`'s "69 provenance rows" exactly. | None. | close-as-fine |
| D1-3 | `find . -iname "vendored_*_test.clj" \| wc -l` (+ per-brick split) | 27 total (20 `sim-emit-hl7`, 7 `sim-trajectory`) | Matches `state.md`'s "27 files... 20... 7" exactly. | None. | close-as-fine |
| D1-4 | Direct read of `components/oracle/src/ehrt/oracle/digest.clj`'s `roots` map (not the shell script, the actual defining data structure) | 27 keys, hand-counted from the literal map | Matches `state.md`'s "27 roots" and the Step-0 oracle pre-digest's own bracket. | None. | close-as-fine |
| D1-5 | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | 23 files | Matches `state.md`'s "23 test files" exactly. | None. | close-as-fine |
| D1-6 | `ls notes/adr/*.md \| grep -v README \| wc -l` | 74 files | Matches `notes/adr/README.md`'s own self-disclosed "74 of them, as of ADR-0076" line exactly — that file's own staleness-tripwire framing ("a count that goes stale the moment the next ADR lands") is itself confirmed accurate and self-aware; this review's own ADR-0077 will make it 75. | None — this file already names its own decay correctly; nothing to fix. | close-as-fine |
| D1-7 | `git tag -l 'stable-*'` excluding the 3 frozen legacy tags | 29 live | `state.md`'s own last count (26, dated at the vendoring-arc close) is now stale by exactly 3 — `stable-20260807-ci-current` (ADR-0075), `stable-20260807-quality-riders` (ADR-0076), and this session's own `stable-20260807-quality-riders`... corrected: the 3 new tags since `state.md`'s last regeneration are `-ci-current`, `-quality-riders`, and this session's own predecessor tag was already `-quality-riders` — the arithmetic (26→29) is fully accounted for by the two rider sessions' own tags plus this session's Step-0 tag. Expected staleness: `state.md`'s own contract (AR-C-1) regenerates only at arc closes, and neither rider session nor this survey is an arc close. | None — the staleness is contractual, not a defect. | close-as-fine |
| D1-8 | Fresh `wc -l` sum of every `:paths` member per reading set in `.agents/reading-sets.edn`, compared against each set's stated `:budget-lines` | onboarding 1104/1240 (margin 136); corpus 1788/2040 (252); sim 843/915 (72); judge 901/980 (79, ~8% headroom); docs 727/775 (48, ~6% headroom, tightest) | All five sets are under budget; `ehrt.docs-tooling.reading-set-budget-test` stays green. `judge` and `docs` carry the thinnest margins, echoing the UX audit's own E-8 finding about `:docs`'s tight margin (then 93/775) — the margin has since tightened further in relative terms even as the absolute budget grew, because actual content grew alongside it. | No re-derivation owed today (nothing red). Watch `judge`/`docs` margins at the next arc-close re-derivation — if either goes red before then, the re-derivation trigger fires on ordinary content growth, not a session's fault. | close-as-fine (watch item) |
| D1-9 | Cross-reference: does `make docsgen` (D5-1) change any of the counts re-derived above? | No — `make docsgen` touched zero files this session (see D5). | Confirms D1's own re-derivations were against a genuinely current tree, not one docsgen would have silently altered underneath this probe. | None. | close-as-fine |

**Dimension 1 verdict: GREEN.** Nine probes, zero drift. Every claim
checked resolved exactly against the live tree.

---

## Dimension 2 — Guard coverage

Probed by a dedicated sub-agent (mechanism: read `.agents/rulings.md`
in full, grep+read each ruling's candidate enforcing test to confirm
the assertion actually tests the rule, not just a plausible filename).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D2-1 | Map all ~24 standing rulings in `.agents/rulings.md` to an enforcing `deftest`, confirmed by reading the test body | Six rulings have a confirmed, real enforcing test: AR-2 provenance leaf law (`provenance_leaf_law_test.clj`), AR-T-1/T-2 tag law (`tag_law_test.clj`, the six-surface drift tripwire), "two voices, two homes" (`help_voice_test.clj`), "errors name their artifact" (`core_test.clj`'s named-category dispatch tests), "vendored bytes are law" (`notice_verbatim_test.clj`), and AR-B-4's session-narrative hierarchy (`done_pointer_adr_test.clj`, one-directional: dangling pointers caught, orphaned index entries not). | The gated core of this repo's standing law is real and independently confirmed, not just claimed. | None for these six. | close-as-fine |
| D2-2 | Same sweep — rulings that are process/judgment disciplines, not testable invariants by nature | ~10 rulings (AR-M1-4 intake-front-door doctrine, AR-P-3/AR-P-4 oracle-script contract and "promotion doesn't improve," AR-D-6 counting-definition convention, AR-C-2 register's own append contract, AR-C-3 `/mnt/c` retirement, "transcript-witnessed is not repo-recorded," D-3 pairing-as-data's placement decision, "population-scale gate outranks the census sample," "audit evidence uses the mechanism it recommends") have no enforcing test and structurally can't — each is a one-time decision or an authoring discipline for humans/sessions, not a property of the tree a test can assert. | Correctly ungated; gating these would be gate-for-gate's-sake. | None. | close-as-fine |
| D2-3 | AR-M4-3 — `ehrt.sim.interface`'s façade is ruled "permanently frozen in surface: var list, names, and arities byte-identical" | No test walks the interface's var list/arities and diffs against a frozen baseline; the tests that DO reference `sim.interface` check dependency shape and invocation behavior, not surface identity. | A claim this repo treats as load-bearing (`corpus` depends on it in-process) has zero mechanical enforcement — a future accidental signature change would surface only as a downstream compile/runtime failure, not a named violation of the frozen-surface rule itself. | Add a small `deftest` that reads `ehrt.sim.interface`'s public vars (name + arity) via reflection/`ns-publics` and diffs against a committed baseline list — cheap, precedented by this repo's own "currency" gate family. | fix-session-candidate (cluster: façade surface-identity gate) |
| D2-4 | AR-C-1 — `.agents/state.md`'s regeneration contract ("regenerated... at each arc close, every `[V]` claim re-probed") | No test confirms `state.md` was actually regenerated at the last arc close, or flags staleness if a future close skips it — the contract is enforced entirely by session discipline. | The contract is real and (per D1, above) currently honored, but nothing would catch a lapse mechanically — the same shape of gap that let the tag law drift for nine sessions before ADR-0057 caught it (`.agents/rulings.md`'s own "law-surface propagation lesson"). | Author ruling needed: is a lightweight staleness check worth adding (e.g. asserting `state.md`'s own cited tip is within N commits of the latest arc-close commit), or is this an acceptable, deliberately-narrative surface that stays session-discipline-only? Either answer is defensible; this review surfaces the question rather than presuming the answer. | ruling-needed |
| D2-5 | AR-A-5 — the Deferred section's own standing contract ("a row that closes moves to Done WITH its notes intact... never left in place with a closure note substituted") | No test scans the Deferred section for in-place "RESOLVED"/"see Done" language. Precedent: `myocardial_infarction.json` violated exactly this pattern for an extended period (found by ADR-0047, fixed by ADR-0055/rider ADR-0048) before any session caught it — and nothing would catch a repeat today (confirmed clean currently, D7-3, but only by direct read, not by gate). | The rule has already been violated once and the fix was manual detection, not a gate. | Add a small docs-tooling lint scanning `.agents/plans/roadmap.md`'s Deferred section for rows containing "RESOLVED"/"CLOSED"/"see Done" — cheap, precedented, closes a gap that has already cost one real incident. | fix-session-candidate (cluster: Deferred-section in-place-closure lint) |
| D2-6 | AR-BB2-R — "tests build their own directories, standing" (player arc) | The one known violation (`merge-config-file-suggests-a-same-stem-sibling-file` reading the live `config/busy-weekday.md`) was fixed at its origin (ADR-0067) and again hardened this arc (ADR-0076's atomic-tempdir fix) — but no repo-wide lint scans test sources for a NEW test reading a live, untracked, mutable path outside the tracked-fixture carve-out. | The specific known instance is fixed twice over; the class of mistake it represents has no recurrence gate. | Cluster with D2-3/D2-5 — a docs-tooling lint family addition, same shape as `invocation-lint-test`, scanning test source for reads of paths outside `components/*/test-fixtures` / `config/synthea` that aren't build-time-generated. | fix-session-candidate (cluster: same lint-family session as D2-3/D2-5) |
| D2-7 | Is the "law stated on multiple surfaces needs a drift gate" LESSON itself generalized, or is `tag_law_test.clj` a single-instance response? | `tag_law_test.clj` is real and does exactly what it claims for the tag law's own six surfaces (D2-1). But AR-D-6's counting-definition phrasing is echoed near-verbatim across at least 5 session-prompt files with no gate; the Deferred-section contract (D2-5) and "two voices" doctrine each live in prose on more than one surface with only a single-surface (or zero) gate. | The repo learned this lesson once, concretely, and gated that one instance — but has not generalized it into a standing mechanism that would catch the NEXT multi-surface law drifting before a future session notices the hard way, the same way the tag law itself went nine sessions before ADR-0057 caught it. | Author ruling needed: worth building a general "declared multi-surface law → drift test" scaffold (a data-driven registry of {law, surfaces, forbidden-phrasing} that a single test iterates), or is catching these one at a time, as `tag_law_test.clj` did, an acceptable standing cost? This is a meta-finding about dimension 2 itself, not a single fix. | ruling-needed |
| D2-8 | Enumerate every check that runs ONLY in CI or ONLY on the author's own machine | `integration.yml` (scheduled/dispatch only, real network-fetched Synthea/JDK/FHIR-validator artifacts) is CI-only by design, documented in its own workflow comments. `bin/mirror-nist`, `bin/verify-nist-lock` (both read the author's own `~/.m2` NIST-jar cache, licensing-restricted from ever touching CI, per ADR-0053) and `bin/check-palgebra-drift` (reads a sibling `ehr-testing-sim` checkout, clean-skips if absent) are author-machine-only by design. `bin/regression-oracle` requires a dual-worktree classpath CI's single checkout can't provide, so it's session/author-machine-only too. | Every CI-only or author-only check found is ALREADY disclosed as such in its own header/workflow comment — no undisclosed environment-restricted surface exists. | None. | close-as-fine |

**Dimension 2 verdict: YELLOW.** Of ~24 standing rulings, 6 have a
confirmed gate and ~10 are correctly gateless-by-nature; 4 real gaps
found (D2-3, D2-5, D2-6, D2-7's meta-question), one of which (D2-5)
has already caused a real, previously-fixed incident. The CI/author-
only-check inventory (D2-8) is fully clean and disclosed.

---

## Dimension 3 — Environment independence

Probed by a dedicated sub-agent (mechanism: `gh run list`/`gh api`
re-derivation for the CI window, direct grep+read for every `defspec`
and I/O call site, `git check-attr` re-verification for
`.gitattributes` claims).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D3-1 | SOAK status of the `merge-config-file` flake fix (ADR-0076 AR-QR-2, landed `9cc3563`) — `gh run list` enumeration of every CI run on main since that commit, inclusive | Exactly 3 runs since `9cc3563` through today's tip (`89c0d24`): all green, zero recurrence of the named test. | Clean so far, but the sample (3 pushes) is far short of the fix's own stated bar ("roughly once every five to seven pushes"). Not evidence against the fix — evidence the soak claim is still genuinely open, not yet provable either way. | Carry forward as the next review's own watch item: re-run this exact probe (same mechanism) once ~10-15 more pushes have landed on main. A recurrence before then is a regression report against the fix (per ADR-0076's own stated target), not a fresh finding. | intake (next-review watch item) |
| D3-2 | Enumerate EVERY `defspec` repo-wide; determine whether each pins/logs its generator seed | 71 `defspec` forms found across `sim-model`, `sim-trajectory`, `sim-emit-fhir`, `sim-emit-hl7`, `sim-engine`, `sim-check`, `sim/identifiers_test.clj`. **Zero** pin an explicit `:seed`. `test.check`'s own default reporter DOES print the seed on any failure event (library-default behavior, not something any of these tests added deliberately) — which is how ADR-0076 recovered seed `-60645` for the one flake it found. | ADR-0076's own newly-found `sim-engine` flake is not a special case — it is representative of all 71 property tests in the repo. Every one is equally unreproducible-by-default on failure unless someone captures the printed seed from the CI log at the moment of failure, before logs age out. | Author ruling needed on shape: (a) pin seeds repo-wide (loses generator-diversity-across-runs, gains reproducibility), (b) leave unpinned but add a repo convention/helper that makes re-running a specific failed seed a one-line operation (a `defspec`-wrapping macro that logs `:seed` more durably, e.g. into a test-report artifact CI retains), or (c) rule the current library-default behavior sufficient and rely on CI log retention. This generalizes ADR-0076's own single-instance finding to its true, repo-wide scope. | ruling-needed |
| D3-3 | `.gitattributes` byte-determinism: verify every named `-text` pattern is actually in effect (`git check-attr`); separately, find any OTHER byte-precious (NOTICE/PROVENANCE-hashed) tree lacking `-text` protection | All 5 named `-text` patterns confirmed in effect on a real sample file each. **Finding:** `components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1/{PROFILE.xml,CONSTRAINTS.xml,VALUESETS-disabled.xml}` (sha256-hashed in `NOTICE.md`, pinned to a CDC upstream commit) and `components/corpus/test-fixtures/v2/simhospital/LICENSE` (sha256-hashed in `PROVENANCE.md`) all return `text: auto` — unprotected. Current on-disk hashes still match (no active corruption), but nothing prevents a future checkout/re-add from silently normalizing them, exactly the `uti_recurrence.csv` incident ADR-0072 already found and fixed once for a different tree. | Same hazard class the repo has already been bitten by once, sitting open in two more locations. | Extend `-text` protection to these 4 files (or their parent globs) — small, mechanical, same shape as AR-VB3-R1's own fix. | fix-session-candidate (cluster: `-text` protection extension, pairs with the notice-verbatim gate that would then need to cover these too if it doesn't already) |
| D3-4 | Ignored-boolean I/O in test helpers (`.mkdirs()`/`.delete()`/`.listFiles()` etc., unchecked return) | Pervasive across test setup/teardown (dozens of hits, `components/corpus`, `corpus-io`, `judge-fhir-official`, `kernel`, `docs-tooling`, `sim`, `bases/cli`, both integration/conformance projects) — all low-stakes (test fixture cleanup). The ONE checked instance in the whole tree is ADR-0076's own fix (`similar-sibling-config` + its test's self-diagnosis). **Adjacent production-code finding:** `components/kernel/src/ehrt/kernel/artifact.clj:123` — `(.renameTo tmp dest)` in the artifact-cache fetch path is unchecked; `renameTo` can return `false` (e.g. cross-filesystem rename, common on CI runners) without throwing, and the function still returns `{:status :ok, :cached false}` even though the file was never actually moved to `dest`. | The dozens of test-only hits are acceptable (teardown risk is low, not user-facing). The `artifact.clj:123` hit is different in kind: production code, on the artifact-fetch path, that can silently report success on a failed operation. | Check `.renameTo`'s boolean return in `artifact.clj:123`; name the failure the same way ADR-0076 named `.listFiles`'s. Small, single-function fix. | fix-session-candidate (cluster: same family as D4-1, below — silently-defeated I/O guards) |
| D3-5 | Untracked/author-local/network dependencies in the test suite beyond what D2-8 already named | `EHR_TESTING_TOOLS_CACHE`/`user.home` (artifact cache dir) and `COLUMNS` (terminal width) are both deliberately injectable seams, tested via their override hooks, never reading the real env var in a test. One hardcoded `/home/claude/nist/...` path exists in `components/corpus/docs/research/judge-v2-nist-spike.clj`, explicitly headed "NOT RUNNABLE HERE... kept verbatim as provenance," under `docs/research/`, not required by any test namespace. No shell-outs, no live network calls in any test file. | Clean — no undisclosed environment dependency. | None. | close-as-fine |

**Dimension 3 verdict: YELLOW.** One clean probe (D3-5), one genuine
open watch item still accumulating data (D3-1), and three real
findings — one a repo-wide generalization (71/71 unpinned `defspec`s,
D3-2), two mechanical and small (D3-3, D3-4).

---

## Dimension 4 — Error honesty

Probed by a dedicated sub-agent (mechanism: grep every nil-returning
I/O call, every `catch`, every cap/truncation, and every raw-parse
call site in `components/*/src` and `bases/*/src`; each hit read in
context, not pattern-matched blindly).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D4-1 | Nil-returning `.listFiles()`/`.list()` calls whose nil flows on silently | ADR-0076 fixed exactly ONE call site (`sim/run.clj`'s `similar-sibling-config`) this session. The identical nil-vs-empty-array conflation is unfixed in at least 9 other production call sites: `corpus/generate.clj:127`, `corpus/generator_source.clj:63`, `corpus-io/spool.clj:58`, `corpus-io/sink_write.clj:166`, `kernel/artifact.clj:195` (`extracted-already?`) and `:227` (`find-executable`), `sim-trajectory/census.clj:173`, `judge-fhir-official/fhir.clj:471`, `bases/cli/core.clj:375` and `:1039`. **Worst concrete consequence, traced to a demonstrated silent-success path:** `bases/cli/core.clj:620,628`'s `files-with-extension-in` feeds `mutate-command`'s file loop — if `.listFiles` returns `nil` on a real I/O failure (indistinguishable in code from a genuinely empty directory), the loop terminates immediately and `mutate-command` returns `{:status :ok, :payload {:count 0, :files []}}` — **a successful, zero-item result for what was actually a failed directory listing, with no error surfaced at all.** Several other hits (`generate.clj`, `generator_source.clj`, `spool.clj`, `sink_write.clj`, `artifact.clj`'s `extracted-already?`) gate a `:fail-if-exists`/`:out-dir-exists` safety check — an I/O-failure nil would silently read as "directory is empty/safe," defeating the exact guard it's meant to enforce. | This directly violates the UX arc's own standing rule ("errors name their artifact, standing," `.agents/rulings.md`) at repo-wide scale, with one instance (`mutate-command`) demonstrated to produce a clean, successful-looking, wrong answer rather than a loud failure — the single most severe finding of this review. | Generalize ADR-0076's own fix pattern (retry-once-on-nil, distinguish I/O failure from empty) across all 9+ call sites, prioritizing `bases/cli/core.clj`'s `files-with-extension-in`/`mutate-command` path first (the demonstrated silent-success case) and the `:fail-if-exists`-guarding call sites second (defeated safety checks). Sizeable but mechanical — same fix shape repeated, not a redesign. | fix-session-candidate (cluster: nil-`.listFiles` sweep, HIGHEST priority this register — pairs with D3-4's `artifact.clj:123` finding, same root cause class) |
| D4-2 | `catch` blocks that continue without a category | ~30 `catch` sites sampled in full. Every one is either a categorized `result/error`/`result/rejected` naming the failure, a DOCUMENTED best-effort degrade (git/java-version queries, verdict-cache miss, `sim/version.clj`'s nil-when-`.git`-absent contract — each with an explicit docstring naming the fallback as intentional), or a legitimate narrow rethrow of an unexpected exception type. | No swallow-and-silently-default pattern found. | None. | close-as-fine |
| D4-3 | Silent caps or truncations | No `(take N)` near I/O/reporting code found. The one real cap (`corpus-io/spool.clj`'s `read-capped`, reading `max-bytes+1` to detect overflow) is fully disclosed — the caller turns `:exceeded? true` into a named `result/rejected :spool-cap-exceeded`. | Clean. | None. | close-as-fine |
| D4-4 | Parses that default on malformed input | Nearly all guarded (`kernel/artifact.clj`, `sim/run.clj`, `corpus/intake.clj`, `judge/verdict_cache.clj` all wrap parse calls in categorized try/catch). One minor asymmetry: `sim-trajectory/gmf.clj:1556`'s weight-column `Double/parseDouble` has no guard, while its sibling age/time-column parse two lines above (1547-1548) does produce a named `result/rejected :malformed-lookup-table-range` — the unguarded case throws a raw, uncaught exception (loud, not silent, but inconsistent with the pattern immediately beside it). `sim-emit-hl7/v2_replay.clj:194,208` similarly unguarded, but explicitly parses only the tool's own emitted output for a self-consistency check, not external input — lower stakes. | Not a silent-default violation (both are loud crashes) — a minor consistency gap, not the class of defect this dimension centrally hunts. | Optional, low-priority: add the same named-rejection guard to `gmf.clj:1556`'s weight-column parse, matching its own sibling two lines up. | close-as-fine (cosmetic; noted for whoever next touches `gmf.clj`'s lookup-table parsing) |

**Dimension 4 verdict: RED.** Three of four probes are clean, but D4-1
is a demonstrated, repo-wide, currently-live violation of a standing
rule — including one call path that turns a real I/O failure into a
clean, successful, wrong answer with zero error surfaced. Severity,
not finding-count, drives this dimension's color.

---

## Dimension 5 — Mirror and derivation drift

Probed directly by the landing session (mechanism: `make docsgen`
regenerates every derived doc from its live source and the resulting
`git diff`/`git status` is the verification — no doc content re-read
as its own proof).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D5-1 | `make docsgen` (regenerates `docs/dev/pipeline.md`, `docs/use-cases.md` + 20 per-case pages, `docs/operators.md`, `docs/cli.md`) then `git status --porcelain` / `git diff --stat` | Every target regenerated; `git status --porcelain` empty before and after — **zero byte of diff anywhere.** | Every derived doc in the repo is byte-current with its live source, confirmed by actually regenerating them, not by trusting the last session that touched them. | None. | close-as-fine |
| D5-2 | Skill-mirror currency (`.agents/skills/*` vs `.claude/skills/*`, all 17 directories including `repo-review` itself) | Confirmed green via the full-suite run (`skill-mirror-currency-test`, 224 assertions, 0 failures) — the specific gate ADR-0076 confirmed already covered the newly-landed `repo-review` directory via its own glob, re-confirmed here at full scale. | No drift between mirrored skill homes. | None. | close-as-fine |
| D5-3 | Local-vs-CI generator currency: is each generated doc's freshness gated in the environment that can actually gate it? | `cli.md`/`operators.md` are gated LOCALLY (`bases/cli/test/ehrt/cli/help_test.clj`'s `cli-md-is-current-test`, `corpus/operators_doc_test.clj`'s equivalent — both landed by ADR-0075 AR-CI-2) in addition to CI. `docs/dev/pipeline.md`/`docs/use-cases.md` remain correctly CI-only, named as such in `docsgen_test.clj`'s own rewritten docstring (the `resource_equations_to_mermaid.py`/mermaid dependency, genuinely outside a JVM test). | The local/CI division ADR-0075 established matches what's actually gated where — no doc claims local coverage it doesn't have, and no CI-only doc is silently un-owned. | None. | close-as-fine |

**Dimension 5 verdict: GREEN.** Every derived-doc and mirrored-pair
probe came back clean, independently re-derived rather than assumed
current from the last session's own claim.

---

## Dimension 6 — Sampling adequacy

Probed by a dedicated sub-agent (mechanism: direct read of
`census.clj`'s actual counting logic, not its docstring; re-derived
binomial math `(1-p)^n` against disclosed real branch-probability
precedents; cross-checked every historical finding's CURRENT status
against the live tree rather than repeating the number from memory).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D6-1 | Read `sim-trajectory/census.clj`'s `:closure-file-count` computation in full — does it count CSV lookup tables, or JSON modules only? | Confirmed: in both the load-failed branch (`(count @fetched)`, populated only by the JSON-module resolver) and the ok-walked branch (`(count modules)`, the JSON-module map), CSV lookup tables are never counted — the exact mechanism ADR-0074 disclosed as an open, unfixed Deferred item ("a future session extending the census tool itself, not a vendoring session"). Still unfixed as of this session; ADR-0075/0076 don't touch it. | This metric has now caused a real, disclosed undercount three separate times across the vendoring arc (asthma 3→11, vhd-pulmonic/vhd-tricuspid 2→4 each) — a repeated, not hypothetical, cost. | This item is already correctly named and ruled-deferred by the vendoring arc itself (ADR-0071 AR-VB2-4) — this review's job is to re-confirm it's still open, not to re-propose it as new. Given the repeat-cost, recommend the author schedule it explicitly rather than let it continue as ambient Deferred-backlog debt. | intake (already-named, aging debt — re-confirmed live; escalate its priority given the 3x-repeat cost) |
| D6-2 | Census's default seed count vs. the disclosed branch-probability precedent it must detect | `default-seed-count` = 3 (confirmed in both `census.clj` and `census_test.clj`). `injuries.json`'s dental-referral loop fired on ~3.3% of walks (4/120 well-mixed seeds, ADR-0070) — expected hits in a 3-seed sample at that true rate ≈ 0.1, i.e. a near-certain miss, exactly what happened historically. | The census's 3-seed sample is mathematically inadequate against any branch below roughly 20-25% true probability (re-derived: 3 seeds gives ~50% detection odds only around p≈20%) — the census was never designed to catch rare branches, and the repo's own standing ruling already says so ("the population-scale gate outranks the census sample," `.agents/rulings.md`). | No new recommendation beyond confirming the existing ruling is doing exactly the job it should — the census is correctly treated as curation triage, never a vendoring license, and every session that flagged a module DID run the real population-scale check regardless of what the census said. | close-as-fine (the standing ruling already covers this; D6-1 is the separate, still-open metric bug) |
| D6-3 | Current status of `anemia___unknown_etiology.json`/`colorectal_cancer.json`'s shared EncounterEnd gap — still deferred, or fixed since? | Confirmed still deferred whole, unfixed, per `.agents/state.md`'s "Live work" section — a two-module blocker on the same root cause, unchanged since ADR-0072. | Not a sampling-power gap — the 300-patient population-scale round trip correctly caught this in both modules (12/17/6 violations at 300 patients for anemia's universal case; 2-of-3 seeds rejected for colorectal's non-universal case). The fix itself (an `EncounterEnd` interpreter design pass) is out of this dimension's own scope — already tracked as a named horizon item. | None new — the sampling mechanism worked as designed here; the open item is the FIX, already named in the roadmap/horizon, not a dimension-6 finding. | close-as-fine (sampling adequacy question itself is answered; the fix is D7's/roadmap's item, not duplicated here) |
| D6-4 | Re-derive `(1-p)^300` for the standing single-seed, 300-patient vendored round-trip convention, at p = 1%/3.3%/5% | P(zero occurrences in 300 patients): p=1% → 4.90%; p=3.3% → 0.0042%; p=5% → ~0%. | The convention is adequately powered for detection at any branch ≥3.3% true probability (near-certain), and marginal at the 1% end (~5% single-run false-negative risk) — but every session that flagged a module in practice ran 2-3 seeds at population scale once flagged (ADR-0071/0072), which drives the joint miss probability at p=1% down to ~0.012%, comfortably adequate. | The multi-seed-once-flagged practice is real and already closes the marginal case — but it lives only in session-to-session precedent, not written down as a standing convention. Recommend stating it explicitly (e.g. in `.agents/rulings.md`'s vendoring-arc entry) so a future session doesn't have to rediscover it from ADR archaeology. | ruling-needed (small — codify an already-followed practice, not a new decision) |
| D6-5 | `defspec` trial counts (50-200 across 71 specs, per D3-2's own enumeration) — adequate for what each actually asserts? | Every `defspec` found asserts a structural/deterministic invariant (RNG-draw counts, schema validity, "never throws," determinism-for-same-inputs) that either holds universally or fails deterministically — none asserts a rare clinical-branch outcome. Rare-branch coverage is correctly delegated to separate, much larger well-mixed-seed sweeps inside the vendored-module test files (200 to 4000 seeds, scaled per-module to the rarity of the content being chased — e.g. `vendored_sore_throat_test.clj` sweeps 4000). | Trial counts are adequately powered for their actual claims; the repo already separates "structural property, needs diversity not volume" from "rare clinical branch, needs volume," and scales each correctly. | None. | close-as-fine |

**Dimension 6 verdict: YELLOW.** One aged, repeat-cost, still-open
metric bug (D6-1) drives the color; the census's own known weakness
(D6-2) and the round-trip convention's own power (D6-4) are both
already correctly compensated for by standing practice, one of which
(D6-4) is only informally followed and worth writing down.

---

## Dimension 7 — Continuity integrity

Probed by a dedicated sub-agent (mechanism: sampled citation
resolution against real files, the docs-tooling continuity gates
re-run directly, and — the dimension's own primary deliverable — a
carried-item aging table built by grepping every arc close's own
horizon-note section for each item's exact restatement, not trusting
any single summary of "how old" an item is).

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D7-1 | Sample 18 `ADR-NNNN`/citation references across `state.md`, `rulings.md`, `roadmap.md`, and recent ADRs; confirm each resolves | All 18 resolve to real files with matching content, including 4 independently-checked commit SHAs. One nuance: `rulings.md` cites "AR-F1-6a"/"AR-F1-6b" for two ADR-0050 rulings whose source file actually labels them `A-3`/`D-3` under a single shared `AR-F1-6` heading — substance present, letter-suffix invented. | A cold reader following the literal `AR-F1-6a` string into ADR-0050 by search would not find it, though the content is one line away. | Small, cosmetic: correct `rulings.md`'s own citation to match ADR-0050's real labels (`A-3`/`D-3`), or fix ADR-0050 to add the sub-letters if that was the intended scheme. Either direction closes it. | fix-session-candidate (cluster: trivial, one-line citation correction — could ride along with any other docs-only session) |
| D7-2 | Directly re-run the pairing/index/done-pointer/skill-mirror gate family | `index-completeness-test` (43 assertions), `done-pointer-adr-test` (4), `skill-mirror-currency-test` (224), `prompt-record-pairing-test` (12) — all green, independently re-run (not merely trusted from the Step-0 full-suite baseline). | Confirms the continuity-gate family holds. | None. | close-as-fine |
| D7-3 | Attic-vs-live consistency — does the `myocardial_infarction.json` in-place-closure pattern (ADR-0047's own disclosed finding) recur anywhere in the current Deferred section? | `myocardial_infarction.json` appears only in the Done attic now, zero times in current Deferred — the historical drift is genuinely fixed (ADR-0055), not just relabeled. Scanned all 12 current Deferred rows: none reproduces the exact pattern (the one row with sub-items marked closed-in-place explicitly discloses its own relocation, which is the compliant shape, not the violation). One stylistic-only similarity noted in the Externals section (a "RESOLVED" bullet left in place) — but Externals carries no relocation contract, so this isn't a rule violation. | Clean. | None — though this is exactly the class of drift D2-5 recommends gating mechanically rather than re-verifying by hand each review. | close-as-fine |
| D7-4 | Carried-item aging — first-named ADR, count of arc-close horizon-notes restating it unchanged, current blocker, for each named watch-list item | See table below. | — | — | — |

**D7-4 aging table:**

| item | first named | closes restating it | age (closes) | current blocker |
|---|---|---|---|---|
| Pairing-as-data registry (landing spot `judge`) | ADR-0050 D-3 (2026-08-05) | ADR-0055, 0064, 0068, 0074 | **4** — independently re-confirmed, matching the session prompt's own assertion exactly | No design pass has started; landing spot ruled, design work not begun |
| Vital-sign/Wave-E cluster (CHF, contraceptives, covid19) | ADR-0036 AR-7 (2026-08-03) | 0074 only (1 horizon-note close; lived only as a live Deferred row, untracked in closes, before that) | 1 (tracked-horizon sense) — older in raw backlog terms | Partially unblocked: CHF/contraceptives now `:produces-content` post-Wave-VS; covid19 alone still `:zero-on-every-seed` |
| Wellness-encounters (design-collision framing) | ADR-0070 (2026-08-07) | Mentioned once in ADR-0074 prose, but dropped from its own quoted horizon-note bullet list — not restated by ADR-0075/0076 | ~0-1, and already at risk of silently falling off the tracked horizon | Needs its own design pass reconciling upstream wellness machinery with this project's wellness-cadence engine; not scheduled |
| Vendoring batch 4 (veteran family) | ADR-0073 (2026-08-07) | 0074 (origin close), restated by 0075/0076 riders | 0 arc closes since origin; 2 rider restatements | Author-scheduling only, correctly named as such |
| Publish-prep Externals (NIST licensing, IG pinning, Clojars F-5/F-6, SETUP rewalk) | ADR-0008 (NIST)/ADR-0049 (F-5/F-6), 2026-07-29/2026-08-05 | 0055, 0064, 0068, 0074 | **4**, identical block each time | All five rows are author-action only, correctly parked, none blocked on a build/design session |

Findings from the table:

| id | finding | recommendation | disposition |
|---|---|---|---|
| D7-5 | Pairing-as-data has genuinely aged 4 closes with zero movement, independently confirmed exact. | Author ruling needed: schedule the design pass now, or explicitly re-defer with a stated trigger condition (the register shouldn't just keep re-confirming the same count forever). | ruling-needed |
| D7-6 | Wellness-encounters was named once (ADR-0070) but already dropped out of the tracked-horizon chain the same day, at risk of silently vanishing from continuity narrative entirely — a genuine near-miss for "a cold reader can reconstruct the truth," this dimension's own core claim. | Re-surface it explicitly in the next horizon note (ADR-0077's own, or the next arc close's) so it doesn't fully disappear. | intake |
| D7-7 | Vital-sign/Wave-E and publish-prep Externals are aging in a healthy, tracked way (progressing or correctly parked) — worth recording as the positive control against D7-5/D7-6. | None — cite as evidence the aging mechanism mostly works, when items ARE restated. | close-as-fine |

**Dimension 7 verdict: GREEN.** The continuity mechanism largely
works — citations resolve, gates hold, the one known historical drift
stays fixed — with two items (D7-5, D7-6) that age imperfectly and
deserve explicit author attention rather than silent re-confirmation
next review.

---

## Dimension 8 — Operator experience

Probed by a dedicated sub-agent with live execution latitude (scratch
writes confined to a fresh `out/` directory, fully cleaned after;
zero tracked files touched, confirmed by `git status --porcelain`
before/after). Mechanism: every live command fence actually run from
workspace root against the real built `bin/ehrt`, not parsed as text.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D8-1 | Build `bin/ehrt`; run every live command fence across `README.md`, `AUTHORS-GUIDE.md`, `docs/**/*.md` (including all 20 generated per-use-case pages), `components/*/docs/**`, `demos/**/README.md` — ~28 fence groups | 26 of 28 groups pass exactly as documented, several reproducing the doc's own exact claimed numeric output (finding counts, snapshot counts, wallclock timings). Full Quickstart sequence (help through `poly test :all`) ran clean end to end, ~13 minutes, real network-fetched artifacts (cache-hit fast). `make integration` (real FHIR-validator subprocess) ran clean. | The overwhelming majority of the operator-facing surface works exactly as taught — a strong, direct, positive result, not merely an absence of complaints. | None for the 26 clean groups. | close-as-fine |
| D8-2 | README.md's own "What you get" fence (lines 77-84): `corpus mutate patient.json ...` | `patient.json` does not exist anywhere in the repo or a fresh workspace — the fence is illustrative narration, not literally runnable. Run verbatim, it throws a raw, unhandled Java `FileNotFoundException` stack trace at exit 1, not a clean CLI error. | A stranger copy-pasting this specific fence (unlike every other fence in the repo, which are all genuinely runnable) hits a confusing raw stack trace instead of either a working example or a clearly-marked "illustrative, not runnable" fence. | Either point the fence at a real, generatable path (matching every other doc's own convention) or explicitly mark it as narration. Small, single-file doc fix. | fix-session-candidate (cluster: README "What you get" fence + D8-3, same file/area) |
| D8-3 | Same fence, traced deeper: does `corpus mutate` on a missing path get the same clean `:file-not-found` envelope `gate`/`sim run` give for the identical error class elsewhere? | No — `corpus mutate` on a missing path produces the raw stack trace above; `gate`/`sim run`/`version --lockfile` all produce a clean, categorized `result/error` naming the path for the same class of failure (confirmed directly, D8's own CLI error matrix). | This is a live instance of the SAME root-cause family as D4-1 (an unwrapped I/O call outside the `Result` vocabulary) — `corpus mutate`'s file-reading path never entered the pattern the rest of the CLI already uses. | Wrap `corpus mutate`'s file-open path the same way `gate`/`sim run` already do — small, well-precedented, could land in the same session as D4-1's sweep given the shared root cause. | fix-session-candidate (cluster: same family as D4-1 — errors-name-their-artifact, `corpus mutate` instance) |
| D8-4 | CLI error matrix: bare invocation, unknown command, unknown flag, missing file — exit codes and categories vs `docs/cli.md`'s own exit-code table | Bare→0 (usage), unknown-command→2 (`:unknown-command`), unknown-flag→2 (`:unknown-flag`), missing-file (`gate`/`sim run --config`)→2 (`:config-not-found`/`:file-not-found`) — every probe lands in `docs/cli.md`'s documented "2 = operational error" bucket exactly. | Matches documentation exactly (the one exception, `corpus mutate`, is D8-3, already counted). | None beyond D8-3. | close-as-fine |
| D8-5 | README's own "See it run" two-commands-to-demo path (ADR-0073), run for real | `corpus generate sim ...` then `play ... --board 60 --rate 60` — both ran exactly as described; the bed-board ticker output matches the README's own framing precisely. | The repo's own front-door demo path works exactly as advertised. | None. | close-as-fine |

**Dimension 8 verdict: GREEN.** Overwhelmingly clean, strong positive
signal across ~28 fence groups; two related, small, same-root-cause
findings (D8-2/D8-3) that pair naturally with D4-1's own fix cluster
rather than standing alone.

---

## Scoreboard — FIRST ASSESSMENT (no prior column; this run is the baseline)

| dimension | verdict | findings (ruling-needed / fix-candidate / intake) | clean rows |
|---|---|---|---|
| D1 — Claim–reality coherence | **GREEN** | 0 / 0 / 0 | 9 |
| D2 — Guard coverage | **YELLOW** | 2 / 3 / 0 | 3 |
| D3 — Environment independence | **YELLOW** | 1 / 2 / 1 | 1 |
| D4 — Error honesty | **RED** | 0 / 1 / 0 | 3 |
| D5 — Mirror and derivation drift | **GREEN** | 0 / 0 / 0 | 3 |
| D6 — Sampling adequacy | **YELLOW** | 1 / 0 / 1 | 3 |
| D7 — Continuity integrity | **GREEN** | 1 / 1 / 1 | 4 |
| D8 — Operator experience | **GREEN** | 0 / 2 / 0 | 4 |

**Overall: 4 green, 3 yellow, 1 red, 0 first-run failures to report as
STOP-AND-REPORT-worthy** (every finding below is a recommendation
against a live, otherwise-healthy tree, not a broken build). The
single RED dimension (D4, error honesty) is driven by severity, not
volume — one repo-wide, demonstrated-live silent-success pattern
(D4-1) that shares its root cause with D3-4 and D8-2/D8-3, meaning the
register's ~5 related rows across three dimensions are plausibly ONE
fix-session cluster, not five independent efforts.

---

## Register summary

**44 total rows** across 8 dimensions (D1: 9, D2: 8, D3: 5, D4: 4, D5:
3, D6: 5, D7: 7 including the aging table's 3 derived findings, D8:
5). Disposition counts: **close-as-fine 26**, **fix-session-candidate
9**, **ruling-needed 6**, **intake 3**. No dimension was left unprobed;
every row's evidence was gathered by re-derivation, re-run, or direct
read-in-context — no claim was re-read as its own verification. The
seeded watch-list (AR-RR-2) is fully covered: SOAK status (D3-1),
`defspec` seed enumeration generalized repo-wide (D3-2), the
EncounterEnd gap re-confirmed still deferred (D6-3), census
JSON-only/three-seed power (D6-1/D6-2), carried-item aging for all
five named items (D7-4), the laws-to-gates map (D2-1/D2-2), CI-only/
author-only inventory (D2-8), and the operators-registry
shared-mutable-state pattern (folded into D2's guard-coverage sweep
and D4's catch-block sweep — no second polluter class found beyond the
one ADR-0075 already fixed in `operators_doc_test.clj`, confirmed
clean by the full-suite green run itself).

**The one cross-dimension pattern worth naming explicitly:** D3-4
(`artifact.clj:123`'s unchecked `.renameTo`), D4-1 (the 9+-site
`.listFiles` nil sweep, this register's highest-severity row), and
D8-2/D8-3 (`corpus mutate`'s unwrapped file-read) are the SAME root
cause — an I/O call outside this repo's own `Result`-vocabulary
convention — surfacing independently in three different dimensions'
probes. A single fix session scoped to "every I/O call in `src/` that
can fail silently instead of through `Result`" would likely close all
four rows at once, rather than four separate sessions each re-deriving
the same pattern.
