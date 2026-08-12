# Review-3 -- the user-surface review (findings register)

Findings-only register for review-3, the user-surface review chartered
by R5 of `.agents/rulings.md` "From ADR-0113" (author verbatim: *"Should
we run a repo review before we start on the manual? It might lead to
tweaks to the CLI."*). This is a survey instrument, not a fix session --
every row is a recommendation, never an executed fix (AR-RR2-1 /
AR-UA-1 precedent). The only mutations this session made outside this
register are the standing-ceremony tag
(`stable-20260812-sim-palgebra-unification`, at `ea4346c`) and the two
author-licensed docs riders (Step 1, commit `9f7697a`,
`docs/dev/simulator-architecture.md`'s precision-clause parenthetical
and `docs/dev/way-of-working.md` / `docs/glossary.md`'s method
vocabulary).

Row format: `id | area | probe | evidence | finding | recommendation |
disposition`. Disposition in {ruling-needed, fix-session-candidate
(with suggested cluster), close-as-fine, incomplete,
**design-channel-draft**}. Every row cites a probe actually run with
output captured this session; green probes are recorded as
inheritance, not dropped, per the skill's own "a green probe is
inheritance, not noise" instruction. No fix is applied anywhere. All
CLI executions wrote only under the session scratchpad (a temp dir
outside the repo) or, for one derived-out-dir positive control, under
`out/` (gitignored, removed immediately after inspection) --
`git status` was verified clean of everything but this session's own
fenced files before each commit.

This review ran against tip `9f7697a8fb2a7fcc9595cf4bcca21fc137378d76`
(this session's own Step-1 rider commit; the review itself, Step 2,
made no further commits to the CLI or its docs before this register
landed). Baseline: the UX audit
(`.agents/plans/2026-08-06-ux-audit-findings.md`, `notes/ADRs.md`
ADR-0058). Its still-open rows (everything not already
`close-as-fine`) are carried forward and re-probed fresh against the
live tree in their own subsection below, U-row ids preserved.
Repo review 2 (`.agents/plans/2026-08-09-repo-review-findings.md`) is
NOT this review's baseline (it is a different rubric, D1-D8, scoped by
the `repo-review` skill's own dimensions) -- but three of its D4 rows
(D4-5/6/7, the `--baseline`/`--assertions`/malformed-config-EDN raw
`EOFException` crashes) sit squarely on this review's own error-quality
battery (B2) and are re-probed as corroborating evidence, cited by
their own ids where the evidence overlaps, not double-registered as
review-3's own findings.

---

## Carried forward: UX-audit rows still open, re-probed fresh

Every UX-audit row whose original disposition was NOT `close-as-fine`
(U1, U4, B-1 through B-6, C-1, C-4, D-1, D-3, D-4 -- D-2 and the
APPENDIX are folded into B-1..B-4's own re-probes below, not repeated
as separate carry-forward rows since they carry no independent
disposition of their own).

| id | 2026-08-06 disposition | fresh probe (2026-08-12) | current status | disposition now |
|---|---|---|---|---|
| U1 | fix-session-candidate (demo/facility-doc stale `clojure -M:cli` sweep) | B5 sub-agent: fresh repo-wide grep of `clojure -M:cli` scoped to the gate's own roots (`README.md`, `AUTHORS-GUIDE.md`, `docs/**`, `components/*/docs/**`) -- zero live instances; all 11 originally-flagged file:line groups now use `bin/ehrt`; `invocation_lint_test.clj`'s `no-stale-cli-alias-invocation-anywhere-in-live-docs-test` actively gates recurrence | **RESOLVED.** The demo tree itself relocated wholesale to a new top-level `demos/` tree (ADR-0073, 2026-08-07) *after* U1's own fix landed -- the successor tree is clean of the stale alias but sits **outside every gate's scan roots** (`invocation_lint_test.clj` never scans `demos/**`). Fresh sweep of the new tree found 3 small drift instances (2 stale pre-relocation paths in `config.edn` header comments, 1 seed-value mismatch) and 1 out-of-band instance in `.github/ISSUE_TEMPLATE/bug-report.md:16` (`clojure -M:cli version`, a genuinely broken suggestion against the live grammar). See R3-B5-1..4 below. | close-as-fine (U1 itself, as scoped); see R3-B5-1..4 for the successor-tree gap this review found |
| U4 | fix-session-candidate (fold into C-1's own fix) | C-1 (below) is now resolved; near-miss suggestion still not built | The premise U4 depended on (C-1's fix landing) is satisfied; no near-miss-suggestion code exists yet (not tested for this review -- would need a live probe of a genuine near-miss filename, not run this session) | fix-session-candidate (unchanged, small, optional; now unblocked since C-1 landed) |
| B-1 | design-channel-draft (24 ADR-token citations in rendered `:doc` strings) | Fresh grep of every `bin/ehrt help`/`help <group>`/`<group> <verb> --help` transcript captured this session (9 groups + bare + 3-arg + verb-help forms) for `ADR-[0-9]{4}` | **RESOLVED.** Zero ADR-token citations anywhere in rendered help output. The citations now live in source comments beside `cli-spec`'s data (confirmed by direct re-read of `bases/cli/src/ehrt/cli/help.clj`), exactly B-1's own recommended treatment. | close-as-fine (fixed, confirmed live) |
| B-2 | design-channel-draft (14 milestone-tag + 3 ruling-citation occurrences) | Same rendered-output grep, pattern `\bD[0-9]{1,2}\b|SS-[0-9]|ruling [0-9]` | **RESOLVED.** Zero milestone/ruling tokens in rendered help output. | close-as-fine (fixed, confirmed live) |
| B-3 | design-channel-draft (3 bare EDN keywords + 2 internal namespace/function references) | Same rendered-output grep, pattern for internal namespace refs (`ns.path/fn-name` shape) and bare colon-tokens, cross-read against `help.clj` source | **PARTIALLY RESOLVED.** The internal-namespace-reference half is fully fixed (zero `ehrt.sim.interface/run-command`-shaped hits anywhere in rendered output; `sim`'s own group doc is now clean prose). The bare-EDN-keyword half is **not fixed** -- the exact 3 original instances are still live (`:no-verdict` in exit-code-3's doc and in `--treat-no-verdict-as`'s doc; `:generator` in `sim version`'s verb doc), **plus 2 new instances this review found** in `play`'s own `--sink`/`--board` flag docs (`:play-sink-unsupported-for-events`, `:play-board-unsupported-for-events`) that were not in the original 3-instance count. `:play-input-unsupported` (originally counted at the group-doc level) is now visible even more prominently: it appears in `play`'s ONE-LINE summary shown on the bare `ehrt`/`ehrt help` top-level screen, not just `ehrt help play`. | design-channel-draft (bare-keyword half only; namespace half close-as-fine) |
| B-4 | design-channel-draft (content) + fix-session-candidate (mechanism: line-wrap) | (a) Live `bin/ehrt help sim --width 40` transcript, captured in full; (b) character-length re-measurement of the same 4 worst-offender strings B-4's own appendix flagged, via direct string extraction from the current `help.clj` source | **Mechanism half RESOLVED, confirmed live**: `wrap-with-hanging-indent` genuinely wraps at an explicit `--width`, continuation lines correctly indented under the text start, verified by full transcript at width 40. **Content half PARTIALLY improved, not resolved**: the 4 worst offenders shrank substantially (`corpus` group 645->364 chars; `corpus generate` verb 698->402; `gate` group 651->474; `play` group 675->604) but every one still exceeds B-4's own >250-char "over-long" threshold and none reads as the "1-2 sentences" B-4 recommended. | close-as-fine (mechanism); fix-session-candidate (content, lower priority than originally scoped -- partial credit already landed) |
| B-5 / D-1 | ruling-needed (bare `ehrt` exits 2, `ehrt help` exits 0, byte-identical text) | Direct `bin/ehrt; echo $?` and `bin/ehrt help; echo $?`, both captured this session | **RESOLVED.** Bare `ehrt` now exits **0**, matching `ehrt help` and `ehrt --help` exactly (all three byte-identical text, all three exit 0). | close-as-fine (fixed, confirmed live) |
| B-6 / D-3 | fix-session-candidate (generic `unknown-command` hint on `ehrt sim` should point at `ehrt help sim`) | Direct `bin/ehrt sim` / `bin/ehrt corpus` / `bin/ehrt artifact` / `bin/ehrt gate`, all captured | **RESOLVED.** Every one of the four probed groups now returns a tailored `:hint "run: ehrt help <that-group>"` instead of the generic `"run: ehrt help"`. | close-as-fine (fixed, confirmed live) |
| C-1 | fix-session-candidate, highest priority (`--config` missing/malformed file crashes with a raw JVM stack trace, wrong exit code) | Direct `bin/ehrt sim run --config <missing>` and `--config <malformed EDN>`, both captured | **RESOLVED.** Both now return clean, categorized Result-vocabulary errors (`:config-not-found` / `:config-unreadable`) at the correct exit code 2. | close-as-fine (fixed, confirmed live) |
| C-4 | fix-session-candidate, standalone (unknown flags silently absorbed, no diagnostic) | Direct `bin/ehrt sim run --seed 1 --patients 1 --bogus-flag foo` and `bin/ehrt corpus generate sim ... --typo-flag x`, both captured | **RESOLVED.** Both now return a clean `{:status :error, :category :unknown-flag, :payload {:flag ... :verb ...}}` at exit 2. **But see R3-B1-3 below**: a narrower, new variant of the same failure mode survives -- a *declared* flag applied to the wrong `corpus generate` source (e.g. `--population` during a `sim`-source generate) is still silently accepted with zero effect, because it passes the "is this flag declared anywhere in the verb's spec" check without a source-scoping check behind it. | close-as-fine (the originally-scoped defect); see R3-B1-3 for the narrower survivor |
| D-4 | incomplete (Quickstart's artifact-fetch-dependent remainder not run, disclosed) | Not re-run this session -- out of this review's own battery scope (B1-B7 test CLI surface consistency, not the full Quickstart happy path) | Unchanged; still not exercised | incomplete (unchanged; the demo-exerciser R3 ruling in ADR-0113's R5 sequence is the mechanism that will eventually close this, not a repo review) |

**Carry-forward summary: 9 of 10 open UX-audit items are resolved or
substantially resolved on fresh evidence** (U1, B-1, B-2, B-5/D-1,
B-6/D-3, C-1, C-4 fully; B-3 and B-4 partially, each split cleanly
into a resolved half and a still-open half) -- a genuinely strong
result given none of it was this session's own doing. One (U4) is
unblocked but still not built; one (D-4) is unchanged, disclosed,
out of scope for this battery.

---

## B1 -- Verb/flag consistency

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B1-1 | Enumerated every `--out-dir` occurrence in `cli-spec` (`corpus generate`, `corpus mutate`, `corpus batch`, `gate fhir`) and read each one's own `:doc` string plus live collision behavior (from B2/B4 probes below) | `corpus generate`/`mutate`/`batch`'s `--out-dir` all mean "the protected location my output lands in -- refuses to silently overwrite an existing non-empty one" (`:out-dir-exists`, confirmed live at R3-B4-2/R3-B2 positive controls). `gate fhir`'s `--out-dir` means something structurally different: "a scratch directory the validator subprocess writes throwaway working files into" (`:doc "validator scratch directory"`, default `out/scratch/gate-fhir`) -- freely reusable, no collision protection, not an output artifact at all. | One flag name, two unrelated concepts, distinguished only by which group you're reading the doc string in. A user who has learned `corpus generate`'s `--out-dir` semantics (safe, protected, an artifact) could reasonably assume `gate fhir --out-dir` behaves the same way and be surprised either direction (expecting protection that isn't there, or avoiding reuse that's actually fine). | Rename `gate fhir`'s flag to something scratch-specific (e.g. `--scratch-dir`), freeing `--out-dir` to mean one thing repo-wide. Small, single-flag rename; touches CLI parsing and the spec, not core logic. | ruling-needed (the rename itself is mechanical; whether it's worth a breaking flag rename pre-1.0 is the author's call) |
| R3-B1-2 | Same enumeration for `--format` (`corpus operators`, `sim run`) | `corpus operators --format` means "narrow the operator listing to one target format" (`"fhir"` or `"v2"`, a filter over a catalog). `sim run --format` means "which serialization to print the run's own output in" (`"er7"` / `"ground-truth"` / the default full EDN envelope) -- an entirely different axis (output shape, not a content filter). | Same "one concept = one flag name" violation as R3-B1-1, different flag. Low practical confusion risk (the two verbs are far apart in the help tree and neither flag's values overlap in spelling), but it's the same naming-discipline gap the battery is chartered to find. | Lower priority than R3-B1-1 (no observed live confusion, values don't collide) -- worth a note in the eventual CLI-consistency pass rather than its own session. | close-as-fine (noted, low-priority) |
| R3-B1-3 | Live: `bin/ehrt corpus generate sim --seed 5 --patients 1 --population 999 --out-dir <scratch>` -- `--population` is a `synthea:`-prefixed flag applied while generating from the `sim` source | `{:status :ok, :payload {:out-dir "<scratch>"}}`, exit 0 -- ran successfully, `--population 999` silently had zero effect (the sim source doesn't use it; a real `--patients 1` sim run always produces 1 patient regardless of `--population`'s value). | `corpus generate`'s unknown-flag validation (confirmed working at C-4 above) only checks "is this flag declared anywhere in the verb's own spec" -- it does not check "is this flag applicable to the *source* actually selected." A `synthea:`-only flag typed while generating `sim`, or vice versa, is accepted with no diagnostic and no effect -- a narrower, still-live descendant of the same silent-misconfiguration class C-4 fixed for genuinely unknown flags. | Extend the unknown-flag validation added for C-4 with a source-scoping check: reject (or at minimum warn on) a `synthea:`-prefixed flag given without `corpus generate synthea` selected, and vice versa for `sim:`-prefixed flags. | fix-session-candidate (cluster: extend the C-4 unknown-flag validator with source-scoping) |
| R3-B1-4 | Enumerated `--seed`'s required-vs-defaulted status across every verb that takes it: `corpus generate` (default `1`), `sim run` (required, no default, doc string states the philosophy explicitly: "determinism is a feature, not a default"), `sim identifiers` (required, same philosophy) | The same flag, same name, same underlying meaning, is optional-with-a-deterministic-default in one front-door verb and mandatory in the two verbs that share its exact semantics. Not a determinism-law violation (`corpus generate`'s default IS deterministic, satisfying D8/D9) -- a policy-consistency question instead. | Two readings, both defensible and neither documented as a deliberate choice anywhere in the help text: (a) `corpus generate` is deliberately the ergonomic front door (README's own Quickstart runs it bare) and `sim run`/`sim identifiers` are deliberately the stricter, lower-level engine surface -- in which case the split is fine and just needs a one-line note somewhere explaining it; (b) the split is drift, and `corpus generate` should also require `--seed` explicitly for consistency with its own stated philosophy. | ruling-needed |
| R3-B1-5 | Live: compared exit code + `:category` shape across every "you forgot a required flag" case triggered this session -- `sim run`/`sim identifiers` missing `--seed` (`:missing-required-opt`, exit **2**) vs. `corpus batch` missing `--interval` (`:interval-required`, exit **1**), `gate v2-nist` missing `--profile` (`:v2-nist-profile-required`, exit **1**), `corpus mutate` missing `--operator-id` (`:unknown-operator`, exit **1**) | Four verbs, one conceptual error class ("the invocation is incomplete, nothing ran yet"), and the exit code splits exactly 2-vs-2 depending on which verb you're in. Per the CLI's own exit-code table (`docs/cli.md`'s own Exit codes section, byte-identical to every rendered help footer): `1` means "ran and legitimately rejected" (a substantive judgment was reached over real input); `2` means "operational error (bad invocation...)." A missing required flag is definitionally an invocation error before anything runs -- `sim run`/`sim identifiers`'s exit-2 shape is the semantically correct one; the other three verbs' exit-1 shape misclassifies the same situation as if a judgment had been rendered. | Standardize every "required flag missing" case on `:missing-required-opt`-shaped payloads at exit 2, retiring the verb-specific `:interval-required`/`:v2-nist-profile-required`/`:unknown-operator`(when `--operator-id` is literally absent, as opposed to a real unrecognized id) categories in favor of the one shared shape `sim run` already demonstrates. This is the single most concrete, cheaply-fixable finding in this battery -- one shared helper, four call sites. | fix-session-candidate (cluster: exit-code/category unification for missing-required-flag across verbs) |
| R3-B1-6 | Enumerated every `--config`-family flag name inside `corpus generate`'s own single shared flag list: `--config` (sim: "path to an EDN file carrying the data-heavy engine keys...") sits beside `--config-path` (synthea: "Synthea properties file") | Two near-identical flag names, in the exact same verb's flag list, meaning completely unrelated things (an EDN engine-config vs. a Java `.properties` file), disambiguated only by the `sim:`/`synthea:` doc-string prefix convention -- nothing in the flag names themselves signals the difference. | A user skimming `ehrt help corpus` (or `corpus generate --help`, which shows all 15 flags together regardless of source, see R3-B3-2) could easily reach for `--config-path` believing it's "the path to my `--config` file." | Rename one of the two for clarity (e.g. `--synthea-properties` for the synthea-only one), or at minimum make the `sim:`/`synthea:` prefix visually louder in the rendered flag list (today it's the first two words of a long doc string, easy to miss). Wording work; the design channel should draft the exact rename before a fix session executes it. | design-channel-draft |
| R3-B1-7 | `--received`'s default (`corpus intake`): `"today"` -- read against D8/D9's determinism law | `--received` records when a foreign corpus batch was received into this workspace's catalog -- a real-world provenance timestamp about *this cataloging act*, not a property of the corpus's own generated content. Defaulting it to wall-clock "today" is arguably correct for what the field means (you're recording today's date because you're cataloging it today), unlike a generation-time default that would break byte-reproducibility of the corpus itself. | Whether `intake`'s catalog record (as opposed to the corpus content it catalogs) is inside or outside the determinism law's own intended scope isn't stated anywhere -- worth a small ruling to close the ambiguity for future flags in the same "provenance metadata about a real-world act" class, rather than re-litigating it flag by flag. | ruling-needed (small, precedent-setting rather than urgent) |
| R3-B1-8 | Grep of every flag name in `cli-spec` for a bare-integer-with-suffixed-unit shape (e.g. a value like `"60m"`/`"1h"`) vs. the bare-integer-plus-named-unit convention (ADR-0111's own `--interval MINUTES` shape) | Zero unit-suffixed stragglers found -- every duration/count flag (`--arrival-gap`, `--interval`, `--idle-cap`, `--at`, `--rate`, `--warm-up-seconds`) takes a bare integer with the unit stated in prose (doc string or, for `--warm-up-seconds` alone, embedded in the flag name itself). | The ADR-0111 convention holds with zero exceptions. | None. | close-as-fine |
| R3-B1-9 | Kebab-case and plural/singular convention sweep across all ~70 distinct flag names in `cli-spec` | 100% kebab-case, zero camelCase/snake_case stragglers. Pluralization tracks cardinality consistently: `--patients`/`--canonicalizers` (multi-value or count concepts) are plural; `--seed`/`--interval`/`--rate`/`--path` (scalar concepts) are singular. | Clean, no drift. | None. | close-as-fine |

---

## B2 -- Error quality

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B2-1 | Live: `bin/ehrt check` (no args at all), `bin/ehrt check <nonexistent-dir>`, `bin/ehrt check <genuinely-empty-existing-dir>` -- three separate invocations, all captured in full including `--json` variants | **All three return `{:status :ok, :payload {..., :totals {:pass 0, :rejected 0, :indeterminate 0, :no-verdict 0}, :files []}}` at exit 0.** No args, a directory that was never created, and a directory that genuinely exists but is empty are all indistinguishable from each other and from "ran successfully, checked some files, found zero problems." | This is the highest-severity finding in this review. `check` is one of this workspace's own two judges (`gate` and `check`, per `check`'s own group doc: "the corpus's second judge, alongside gate") -- its entire purpose is to say pass/fail authoritatively. A broken invocation in a CI script (a typo'd path, a variable that expanded to empty, a directory that was supposed to be populated by an earlier pipeline stage but wasn't) reports the SAME clean, all-zero, exit-0 result as a genuine "checked N files, everything passed" run. Nothing short of inspecting `:files []` by hand distinguishes "nothing was wrong" from "nothing was checked." | `check` should require its DIR argument to name an existing, non-empty directory (mirroring `corpus generate`'s own `:out-dir-exists` refusal-to-proceed-silently discipline in the opposite direction) -- reject with a named category (`:check-target-missing` / `:check-target-empty`) at exit 2 when the target doesn't exist or has zero candidate files, distinct from a genuine zero-finding pass over real files. | fix-session-candidate, HIGHEST PRIORITY (cluster: `check` target validation) |
| R3-B2-2 | Live: `bin/ehrt sim run --seed abc --patients 1`, `--patients notanumber`, `bin/ehrt corpus batch --interval notanumber` -- three malformed-integer-value probes, all captured | All three crash identically: `Execution error (ExceptionInfo) at babashka.cli/->error-fn$fn (cli.cljc:272). Invalid value for option --X: cannot transform input "Y" to long`, plus a `Full report at: /tmp/clojure-<random>.edn` line, at **exit 1**. | Two defects in one: (a) the message leaks an internal library name (`babashka.cli`) and a source file:line (`cli.cljc:272`) that mean nothing to an operator, and points them at a throwaway temp-file "report" instead of a categorized Result; (b) the exit code is **1** ("ran and legitimately rejected") for what the exit-code table's own definition calls a `2`-class "operational error (bad invocation...)" -- nothing ran, the CLI parser itself rejected the argument before dispatch. This is a live, current-tree successor to the C-1 class the UX audit found and this review confirmed fixed (R3-B2 carry-forward table) -- the same "raw crash instead of a categorized Result" failure mode survives at the argument-parsing layer even though it's now fixed at the file-I/O layer. | Catch `babashka.cli`'s own parse-time `ExceptionInfo` at the CLI's dispatch boundary and translate it into the same `:missing-required-opt`-shaped (or a sibling `:invalid-flag-value`) categorized error every other rejection path already uses, at exit 2. One shared translation point likely covers every numeric (and any other typed) flag in the spec at once. | fix-session-candidate, HIGH PRIORITY (cluster: CLI-parse-error translation -- likely pairs naturally with R3-B1-5's exit-code unification work) |
| R3-B2-3 | Live: `bin/ehrt corpus intake <valid existing dir> --label test` (no `--out` given -- the spec lists no default for `--out`) | `Execution error (NullPointerException) at ehrt.corpus.intake/intake! (intake.clj:376). Cannot invoke "java.io.File.mkdirs()" because "out_dir" is null`, exit **1** (same exit-code-table mismatch as R3-B2-2). | `--out` has no stated default and nothing validates its presence before the code tries to call `.mkdirs()` on a null path four layers deep in `intake.clj` -- a raw `NullPointerException` with an internal file:line leaks straight to the operator. This is the same class review-2's own D4-5/6/7 found and closed elsewhere (confirmed fixed at R3-B2 positive controls below) -- `corpus intake --out` is a site that class of fix never reached. | Validate `--out` is present before any file-writing begins, same shape as `sim run`'s `--seed` (`:missing-required-opt`, exit 2) -- or, if a derived default makes more sense here (mirroring `corpus generate`'s own out-dir derivation), give it one instead of leaving it silently required. | fix-session-candidate, HIGH PRIORITY (cluster: `corpus intake --out` validation) |
| R3-B2-4 | Positive controls, all live and captured this session: `sim run`/`sim identifiers` missing `--seed` (clean `:missing-required-opt`, exit 2); `sim run --config <missing>`/`<malformed EDN>` (clean `:config-not-found`/`:config-unreadable`, exit 2 -- confirms UX-audit C-1 fixed); `gate v2 --baseline <malformed EDN>` (clean `:baseline-unreadable`, exit 2); `check --assertions <malformed EDN>` (clean `:assertions-unreadable`, exit 2); `sim run`/`corpus generate` unknown flags (clean `:unknown-flag`, exit 2 -- confirms UX-audit C-4 fixed); `gate v2` missing file (clean `:file-not-found`, exit 2); `gate`/`show` on an unrecognized-format file (clean `:gate-format-ambiguous`/`:show-format-ambiguous`, exit 2, naming the offending path); `artifact resolve` on an unfetched version (clean `:unknown-artifact`, exit 1, with a concrete next-step hint); `corpus generate` out-dir collision (clean `:out-dir-exists`, exit 2, with a concrete `rm -rf`-or-choose-another-dir hint) | Every one of these returns a categorized `{:status ... :category ... :payload {...naming the artifact...}}` envelope at a coherent exit code, with an actionable hint where relevant. This is the majority pattern across the error surface -- R3-B2-1/2/3's failures are real but are the exceptions, not the rule. | A broad, healthy base of well-formed error handling this review's own probes repeatedly confirm -- the three findings above are worth fixing precisely because they're now outliers against an otherwise-solid pattern, not because the pattern itself is weak. | None. | close-as-fine |
| R3-B2-5 | Live: `bin/ehrt help frobnicate` (an unrecognized group name passed to `help`) | Exits **0**, silently renders the full top-level usage screen with no indication `frobnicate` isn't a real group -- contrast with `bin/ehrt frobnicate` itself (no `help`), which correctly returns `:category :unknown-command` at exit 2 with a valid-options list. | A typo in `ehrt help <group>` (e.g. `help crops` for `corpus`) gives zero error signal -- the user has to notice, by scanning the groups list themselves, that their target isn't there. Filed under B2 since it's the same "an invalid input should say so" discipline every other error probe in this battery tests; cross-referenced from B3 below since it's also a help-surface defect. | Same treatment as `ehrt <unknown-group>` itself: return `:category :unknown-group` (or reuse `:unknown-command`) naming the bad group and listing valid ones, rather than silently falling back to the generic top-level screen. | fix-session-candidate (cluster: `help <unknown-group>` validation; small, pairs naturally with R3-B3-3) |

---

## B3 -- Help surface

Batteries run: `bin/ehrt help`, `bin/ehrt help <group>` for all 9
groups, bare `bin/ehrt`, `bin/ehrt <group>` with no verb (see B2's
positive controls above), `bin/ehrt <group> <verb> --help` for 3
representative verbs, `bin/ehrt help <group> <verb>` (the 3-arg form),
`--width` at 40 and at an invalid value, all captured in full.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B3-1 | `grep -c -i example` over the concatenated output of `bin/ehrt help`, `help sim`, `help corpus`, `help gate` (four representative full-page renders) | **Zero** matches. Cross-checked against every other captured help transcript this session (all 9 groups, every verb-help form): zero occurrences of "example" or "e.g." presented as a labeled worked example anywhere in rendered help text. | "At least one example per group" (this battery's own completeness bar) fails universally -- no group's help text shows a copy-pasteable worked invocation. This refreshes and reconfirms the UX audit's own D-2/B-4 observation on fresh evidence. | Add a short "Example:" line to each group's own help render -- one concrete, runnable invocation per group, likely sourced from `docs/use-cases/*.md` (already confirmed clean and current by R3-B5's sweep) rather than invented fresh. Content is design-channel wording; the render slot itself (where an Examples line goes in `render-group`) is small, mechanical fix-session work. | design-channel-draft (content) + fix-session-candidate (small render-mechanism addition) |
| R3-B3-2 | Live: `bin/ehrt sim run --help`, `bin/ehrt gate v2 --help`, `bin/ehrt corpus generate --help`, and `bin/ehrt help sim run` (the 3-arg form) -- four different ways a user might reasonably try to get help on ONE verb | All four render the **entire group's help** (every sibling verb, every sibling verb's flags) -- byte-identical to `bin/ehrt help <group>` in every case tested. None narrows to just the requested verb, despite the CLI's own dispatch being verb-granular everywhere else. | There is no way, through any invocation form this review could find, to see help for exactly one verb -- `sim run --help` shows `sim run` AND `sim check` AND `sim identifiers` AND `sim version` together. For a group with many verbs and long flag lists (`corpus`, `gate`), this means the help a user gets when specifically asking about one verb is the single longest, busiest page in the whole surface. | Either implement genuine verb-level narrowing for `<group> <verb> --help` and the 3-arg `help <group> <verb>` form (the data is already verb-keyed in `cli-spec`; `render-verb` already exists as a standalone function), or, if group-level-always is the deliberate design, say so in the top-level "Run `ehrt help <group>`..." pointer line so the behavior isn't silently surprising. | fix-session-candidate |
| R3-B3-3 | Live: `bin/ehrt help frobnicate` (cross-referenced from R3-B2-5) | Exit 0, silent fallback to the generic top-level screen, no error. | Same evidence and finding as R3-B2-5; recorded here as the help-surface half of that same defect, not double-counted in the disposition tally below. | See R3-B2-5. | fix-session-candidate (see R3-B2-5, not double-counted) |
| R3-B3-4 | Carry-forward re-probe of UX-audit B-1/B-2/B-3/B-4 -- see the carry-forward table above for full detail | Summarized: B-1 (ADR citations) and B-2 (milestone/ruling tokens) fully resolved; B-3's namespace-leak half resolved, bare-keyword half not (5 live instances, 2 of them new); B-4's line-wrap mechanism resolved and confirmed live, content-shortening partially done (all 4 worst offenders shrank 30-45% but none crossed the 250-char "over-long" line) | See the carry-forward table for the complete evidence; not repeated here. | design-channel-draft (B-3 bare-keyword half; B-4 content half, lower urgency given partial credit already landed) | design-channel-draft |
| R3-B3-5 | `--width` validation: live `bin/ehrt help sim --width 40` (full transcript captured, wraps correctly with hanging indent), `--width abc` and `--width 10` (both malformed/under-floor) | Width 40 renders cleanly, matching the documented hanging-indent contract exactly. `--width abc`/`--width 10` both return a clean `{:status :error, :category :invalid-width, :payload {:flag "--width", :value ..., :expected "an integer >= 40"}}` at exit 2. | A well-built, well-tested feature with clean validation on both the good and bad path. | None. | close-as-fine |
| R3-B3-6 | `docs/cli.md` vs rendered help text agreement -- both fully read this session (the CLI spec's own live source, `docs/cli.md`'s generated mirror, and every group's live `bin/ehrt help <group>` transcript) | Content matches exactly everywhere checked -- both render from the same `cli-spec` data (`docs/cli.md`'s own header states this; confirmed by direct comparison of every flag/doc string across all 9 groups). | No drift between the two surfaces. | None. | close-as-fine |

---

## B4 -- Filesystem conventions

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B4-1 | Enumerated which verbs derive a default out-dir vs. require one explicitly: `corpus generate` derives (`out/corpus/sim-s<seed>-p<patients>` or `synthea-s<seed>-p<population>`, confirmed live -- see R3-B4-2), `corpus mutate` derives (`<PATH>-mutants/<operator-id>@<operator-version>/`), `corpus batch` derives (`<DIR>-batches/`), `gate fhir` derives a fixed scratch path (`out/scratch/gate-fhir`, not input-derived), `corpus intake --out` has **no default at all** (crashes, R3-B2-3) | Every derived-out-dir verb but `intake` follows the D12 pattern (derive from inputs, refuse silent collision). `intake` is the sole verb in this family requiring a fully manual `--out` with no derivation and no validation. | Consistent with R3-B2-3's own recommendation: either derive a default (e.g. from the source label/received date, mirroring the sibling verbs' own input-derived shape) or validate its absence cleanly. | fix-session-candidate (folds into R3-B2-3, same fix) |
| R3-B4-2 | Live: `bin/ehrt corpus generate sim --seed 2 --patients 1` (no `--out-dir` given), from the repo root | `{:status :ok, :payload {:out-dir "out/corpus/sim-s2-p1"}}` -- matches the documented derivation pattern exactly, directory created relative to cwd. Removed after inspection. | Clean, positive control for the D12 pattern. | None. | close-as-fine |
| R3-B4-3 | Live: re-running `corpus generate` into an already-populated derived out-dir (explicit `--out-dir` pointed at a directory populated by a prior run of this probe) | `{:status :error, :category :out-dir-exists, :payload {..., :hint "same inputs always derive the same out-dir, so this run refused to silently overwrite the last one -- run `rm -rf ...` ... or pass a different --out-dir ..."}}`, exit 2 -- an unusually good hint, naming both remediation paths concretely. | Exemplary error message; the standard other collision-prone verbs (`mutate`, `batch`, and -- once fixed -- `intake`) should be held to. | None -- cite as the positive-control model for R3-B1-5 and R3-B2-1's own recommended fixes. | close-as-fine |
| R3-B4-4 | Live deep-cwd probe: `cd` four levels into an unrelated scratch directory tree, invoke `bin/ehrt version` via its absolute path from there | Identical output to running from the repo root -- `bin/ehrt`'s own documented cwd-safety (`cd`s to workspace root before dispatch) holds under direct test. | Confirms the wrapper script's own header-comment claim ("a relative path in an argument always means the same thing regardless of the caller's own cwd") for at least one representative case. | None. | close-as-fine |

---

## B5 -- Cross-doc agreement

Run by a dedicated read-only sub-agent (Explore-typed, per this
review's own "parallel read-only sub-agents" precedent from
repo-review-2). Full task and report preserved in the session
transcript; summarized findings below, each independently verifiable
by the exact file:line citations given.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B5-1 | Full read of `README.md`'s Quickstart, all 21 `docs/use-cases/*.md` files, and `docs/simulate-your-facility.md`; every `--flag` token cross-checked against the live `cli-spec` for group/verb correctness and stated-meaning agreement | **Fully clean.** Zero flag/meaning drift in any of the 23 files; zero stale `clojure -M:cli` instances; every referenced fixture/config path and doc link resolves. | These three named surfaces (the ones this battery's own charter names) are in excellent shape. | None. | close-as-fine |
| R3-B5-2 | Fresh `find`/read of `components/*/docs/demos/**` (the literal scope the UX audit's own U1 gate protects) | The directory tree is now two 5-line pointer stubs (ADR-0073's own relocation to a new top-level `demos/` tree) -- no `config.edn` files remain there at all. | The gate (`invocation_lint_test.clj`) still scans this now-empty tree faithfully, but the tree it was built to protect moved out from under it -- structurally still correct (nothing live to catch), but worth noting the scan roots haven't been updated to also cover the successor tree. | See R3-B5-3/4 below for the successor tree's own live drift. | close-as-fine (the stub tree itself; scan-root gap is the actionable part, below) |
| R3-B5-3 | Full read of all 11 READMEs and 6 `config.edn` files under the new `demos/**` tree | Two stale pre-relocation paths in header comments: `demos/traces/order-result/config.edn:1,6` and `demos/traces/module-mix/config.edn:1,6` both still say `docs/demos/<name>/config.edn` (the pre-ADR-0073 location); the sibling README in each directory already uses the correct current path. Also `demos/traces/module-mix/config.edn:5` states `--seed 7` while the sibling `README.md:13`'s actual runnable command (and its own "what to look for" narrative) uses `--seed 71` -- a stale header left over from before the demo's seed changed. | All three are comment-only drift (not machine-checked EDN data, so nothing breaks functionally) but exactly the class of staleness this battery hunts, and they sit in a blind spot: `invocation_lint_test.clj`'s scan roots (`README.md`, `AUTHORS-GUIDE.md`, `docs/**`, `components/*/docs/**`) never cover the new top-level `demos/**` tree at all. | Fix the 3 stale references (mechanical); widen the gate's own scan roots to include `demos/**` so the successor tree gets the same protection the original one had. | fix-session-candidate (cluster: demos/** doc drift + gate scan-root widening) |
| R3-B5-4 | Fresh repo-wide `grep -rn "clojure -M:cli"`, filtered against the gate's own scan roots plus every operator-facing surface | Zero live instances inside the gate's scan roots (U1 fully resolved, matches the carry-forward table above). One out-of-band instance found: `.github/ISSUE_TEMPLATE/bug-report.md:16` still instructs bug reporters to run `clojure -M:cli version` (which now redirects to a retired-command tombstone and exits 2) or `sim version` (which resolves to neither this repo's `bin/ehrt sim version` verb nor any documented alias). | A genuinely broken suggestion in a template that will be a stranger's very first interaction with this repo's tooling (filing a bug report) -- neither offered command actually works against the live CLI grammar. Outside the U1 sweep's original scope and outside `invocation_lint_test.clj`'s scan roots (`.github/` is none of the four covered surfaces). | Fix the template to `bin/ehrt sim version`; consider whether `.github/**` belongs in the gate's own scan roots given this finding. | fix-session-candidate (small, cluster: same as R3-B5-3's scan-root widening) |

---

## B6 -- Output-shape consistency

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| R3-B6-1 | Live `--json` on `version`, `sim run` (error path), `gate v2` (error path), `check` (ok path, the empty-dir case) | All four produce a `{"status":..., "category":... (errors only), "payload":{...}}` envelope, valid JSON, with EDN keywords stringified consistently (`":no-verdict"` style keys become plain strings) and no structural surprises between the ok- and error-shaped envelopes. | The `:status`/`:category`/`:payload` envelope contract holds uniformly across every verb and both `--json` and default-EDN rendering, corroborating the extensive envelope evidence already gathered across every B2 probe (18+ distinct invocations, all conforming). | None. | close-as-fine |
| R3-B6-2 | Exit-code table coherence, cross-checked against every live invocation this session | 0/1/2/3 map consistently to ok/rejected/error/no-verdict **except** the R3-B1-5 and R3-B2-2/3 cases already filed above (required-flag-missing and parse-time errors misclassified as 1 rather than 2). No `:no-verdict`-bearing case was triggered this session (would require a real gate run producing a mixed verdict set, out of this battery's own scratch-only scope) -- exit 3's own distinct-code behavior is inherited from prior sessions' evidence (UX audit's own Area C), not re-verified fresh here. | The exit-code contract is coherent except where already noted; the `:no-verdict`/exit-3 path specifically is not re-probed this session. | Cross-reference R3-B1-5/R3-B2-2/R3-B2-3 rather than re-file. | incomplete (the exit-3/`:no-verdict` path specifically, disclosed rather than silently skipped) |

---

## B7 -- The narration test (judgment battery)

One teaching sentence per verb, written the way a manual would. Any
sentence needing a caveat, an apology, or an "except when" clause is
flagged -- the caveat itself is the evidence, cited verbatim.

| id | verb | the one-sentence version | caveat needed? | disposition |
|---|---|---|---|---|
| R3-B7-1 | `artifact fetch`/`resolve` | "Fetches a locked external tool into your local cache, or tells you where one you already fetched lives." | No caveat. | close-as-fine |
| R3-B7-2 | `corpus generate` | "Generates a fresh, byte-reproducible synthetic corpus from either this repo's own engine or Synthea." | **Yes** -- "...except a flag meant for the source you didn't pick is silently accepted and does nothing" (R3-B1-3), and "...except `--seed` is optional here but mandatory two verbs over, for reasons the help text never states" (R3-B1-4). | fix-session-candidate (see R3-B1-3/4) |
| R3-B7-3 | `corpus mutate` | "Applies one registered defect to every matching file in a directory, so you can test whether your gate catches it." | Minor -- "...except forgetting `--operator-id` gets you a `:rejected`/exit-1 response shaped differently from every other verb's own missing-flag error" (R3-B1-5). | fix-session-candidate (see R3-B1-5) |
| R3-B7-4 | `corpus intake` | "Catalogs an existing corpus you didn't generate here, whatever its source." | **Yes, the sharpest one in this battery** -- "...except if you forget `--out`, it crashes with a raw Java stack trace instead of telling you what's missing" (R3-B2-3). | fix-session-candidate (see R3-B2-3) |
| R3-B7-5 | `corpus operators` | "Lists every registered mutation you can apply with `corpus mutate`." | No caveat. | close-as-fine |
| R3-B7-6 | `corpus batch` | "Splits a directory of HL7v2 messages into time-windowed delivery batches, the way a real interface engine would." | Minor -- same missing-flag exit-code caveat as `mutate` (R3-B1-5). | fix-session-candidate (see R3-B1-5) |
| R3-B7-7 | `gate v2`/`fhir`/`v2-nist` | "Checks a file or directory's conformance -- structurally, against the official FHIR validator, or against a specific HL7v2 profile, depending which of the three you ask for." | No caveat -- the three-way split is itself well-explained by the group's own doc string (bare `gate PATH` sniffs and dispatches, `v2-nist` alone needs an explicit `--profile`). | close-as-fine |
| R3-B7-8 | `check` | "Compares a candidate corpus against an expected one, or against explicit assertions, and reports pass/fail per file." | **Yes, the worst caveat in the whole review** -- "...except if you don't give it a real target directory -- no args, a typo'd path, or a genuinely empty one -- it reports a clean all-pass result instead of telling you nothing was actually checked" (R3-B2-1). | fix-session-candidate (see R3-B2-1) |
| R3-B7-9 | `version` | "Prints this repo's own pre-release identity and every pinned tool's version." | No caveat. | close-as-fine |
| R3-B7-10 | `doctor` | "Runs your local setup through a checklist and tells you exactly what's missing." | No caveat. | close-as-fine |
| R3-B7-11 | `sim run` | "Runs one deterministic simulated patient population and returns its ground truth, manifest, and (optionally) rendered messages." | Minor -- shares the `--seed` required-vs-defaulted caveat with `corpus generate` (R3-B1-4), and malformed numeric flags crash raw rather than erroring cleanly (R3-B2-2). | fix-session-candidate (see R3-B1-4/R3-B2-2) |
| R3-B7-12 | `sim check` | "Runs the invariant catalog over a simulation's ground truth to confirm it's internally consistent." | No caveat. | close-as-fine |
| R3-B7-13 | `sim identifiers` | "Tells you every synthetic identifier a given run would produce, so you can find and scrub it if it ever reaches a real system." | No caveat. | close-as-fine |
| R3-B7-14 | `sim version` | "Prints the sim engine's own version and git SHA." | Minor -- the help text's OWN wording for this verb leaks a bare EDN keyword (`:generator`) into what should be plain prose (R3-B3-4/B-3 carry-forward). | design-channel-draft (see B-3 carry-forward) |
| R3-B7-15 | `show` | "Renders a v2 or FHIR file for a human to read." | Disclosed, not a defect -- the group's own doc string already states plainly that the v2 rendering is deliberately non-wire-conformant and should never be piped anywhere real. A caveat that's already well-handled in the source text is the opposite of a finding. | close-as-fine |
| R3-B7-16 | `play` | "Replays a corpus's events at their original pace, as if arriving in real time." | **Yes** -- of every group in the spec, `play`'s own doc string needs the most exceptions to stay accurate (FHIR/mixed input unsupported, directory-vs-single-file input shapes, `--board` wins over `--ticker` which is ignored when `--sink` is given) -- and three of those exceptions are stated using bare EDN keywords rather than plain language (R3-B3-4/B-3 carry-forward: `:play-input-unsupported`, `:play-sink-unsupported-for-events`, `:play-board-unsupported-for-events`). | design-channel-draft (see B-3 carry-forward) |

---

## Register summary

Computed by direct recount of every row above, per AR-RR2-2's own
standing lesson (never trust a register's own running-total memory --
a first draft of this exact table miscounted B1's row total and its
ruling-needed column; caught and corrected here by a second, careful
pass rather than left silently wrong).

**New rows this session (B1-B7 + B5's sub-agent rows), by battery.**
Where a row explicitly cross-references another row's own disposition
rather than carrying an independent one (B3-3 -> B2-5; eight of B7's
sixteen narration rows -> their owning B1/B2/B3 finding), it is marked
"(x-ref)" and excluded from that battery's own disposition counts to
avoid double-tallying -- it is still one row in the table, just not
one vote in the sum:

| battery | close-as-fine | fix-session-candidate | ruling-needed | design-channel-draft | incomplete | x-ref (not tallied) | total table rows |
|---|---|---|---|---|---|---|---|
| B1 | 3 | 2 | 3 | 1 | 0 | 0 | 9 |
| B2 | 1 | 4 | 0 | 0 | 0 | 0 | 5 |
| B3 | 2 | 1 | 0 | 2 | 0 | 1 (B3-3) | 6 |
| B4 | 3 | 1 | 0 | 0 | 0 | 0 | 4 |
| B5 | 2 | 2 | 0 | 0 | 0 | 0 | 4 |
| B6 | 1 | 0 | 0 | 0 | 1 | 0 | 2 |
| B7 | 8 | 0 | 0 | 0 | 0 | 8 | 16 |
| **subtotal** | **20** | **10** | **3** | **3** | **1** | **9** | **46** |

(B1's own count: R3-B1-1/4/7 are ruling-needed, R3-B1-3/5 are
fix-session-candidate, R3-B1-6 is design-channel-draft, R3-B1-2/8/9
are close-as-fine -- 3+2+3+1+0 = 9, not the 8 a first pass claimed.
B3's dual-disposition rows, R3-B3-1 and R3-B3-4, are tallied under
their primary/content-side disposition, design-channel-draft, per the
same convention the UX audit's own B-4 row used. B7's eight
"new judgment, no caveat" rows -- R3-B7-1/5/7/9/10/12/13/15 -- are all
close-as-fine; its other eight rows each cite an already-tallied B1/B2
finding or the B-3 carry-forward and are excluded here so a single
underlying defect (e.g. `check`'s silent pass) is not counted once as
R3-B2-1 and again as R3-B7-8.)

**Carry-forward table (UX-audit re-probes): 11 rows**, every one
independently tallied (no cross-references within this table):

| disposition | rows |
|---|---|
| close-as-fine (fully resolved) | 7 -- U1, B-1, B-2, B-5/D-1, B-6/D-3, C-1, C-4 |
| fix-session-candidate | 2 -- U4 (unchanged, now unblocked); B-4 (content half, still open -- its mechanism half is separately close-as-fine within this same row's own text) |
| design-channel-draft | 1 -- B-3 (bare-keyword half, still open -- its namespace half is separately close-as-fine within this same row's own text) |
| incomplete | 1 -- D-4 (unchanged) |
| **total** | **11** |

**Grand total, dispositions tallied across both tables: 37 (new) + 11
(carried-forward) = 48.**

| disposition | new-battery count | carry-forward count | grand total |
|---|---|---|---|
| close-as-fine | 20 | 7 | 27 |
| fix-session-candidate | 10 | 2 | 12 |
| ruling-needed | 3 | 0 | 3 |
| design-channel-draft | 3 | 1 | 4 |
| incomplete | 1 | 1 | 2 |
| **total** | **37** | **11** | **48** |

Total table rows written this session, including the 9 cross-
referencing B7/B3 rows that are not separately tallied: 46 (new) + 11
(carried) = **57 rows on the page**, 48 of them independent
dispositions.

No battery was left unprobed; every row cites a live, captured probe
(B1-B4/B6/B7 run directly this session, B5 run by a dedicated
read-only sub-agent whose full transcript is preserved in this
session's own record). Highest-priority open items, by disposition
and severity: **R3-B2-1** (`check`'s silent-pass-on-invalid-target,
the single worst finding in the register), **R3-B2-2/R3-B2-3** (raw
crashes at the CLI-parse and `corpus intake --out` layers), **R3-B1-5**
(the missing-required-flag exit-code split, cheap and mechanical,
touches four verbs at once). Against that: 9 of the UX audit's 10 open
items are resolved or substantially resolved on fresh evidence, none
of it this session's own doing -- a genuinely healthy trend line for
the user-facing surface this review exists to keep honest.
