# UX audit — findings register

Findings-only register for the UX-arc audit session
(`.agents/plans/2026-08-06-ux-arc-brief.md`, `notes/ADRs.md` ADR-0058).
Every row cites a probe; every row is a recommendation, never an
executed fix (AR-UA-1) — the only mutations this session made are the
standing-ceremony tag (AR-UA-0, licensed by ADR-0057 AR-T-1(ii), no
further license needed) and this register itself.

Row format: `id | area | probe | evidence | finding | recommendation
with reasoning | proposed disposition`. Disposition ∈ {ruling-needed,
fix-session-candidate (with suggested cluster), close-as-fine,
incomplete, **design-channel-draft**} — the last is new this session
(AR-UA-2): rows whose fix is wording the design channel must draft for
author ruling before any session touches the file.

Landed 2026-08-06 (UX audit session, `notes/ADRs.md` ADR-0058),
against tip `1d93e6c` (ADR-0057's own closing commit, tagged
`stable-20260806-tag-law` at Step 0 of this session).

---

## Seeded rows (U1–U5), updated with this session's evidence

**U1 — Demo README + config.edn invocations broken/stale, ungated. EXPANDED this session.**

| id | area | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|---|
| U1 | invocation | Full sweep (sub-agent) of every command fence/comment across `README.md`, `AUTHORS-GUIDE.md`, all 30 files under `docs/**/*.md`, and all 67 files under every `components/*/docs/**` tree — 106 candidate files, no truncation | ADR-0056's own probe found 6 demo `README.md` files + 2 `config.edn` header comments (8 surfaces) teaching the stale `clojure -M:cli run ...` form, all under `components/sim/docs/demos/`. This session's wider sweep **confirms all 8 and finds 3 more** the narrower prior scope missed: `components/sim-emit-hl7/docs/demos/README.md:3`, `components/sim-emit-hl7/docs/demos/site-profiles/README.md:12` and `:13` (the site-profiles demo tree, relocated out of `components/sim/docs/demos/` by ADR-0029, was never re-swept), and `docs/simulate-your-facility.md:170-171` — a **top-level `docs/` file**, not a component demo, also teaching the stale form. Total: **11 file:line groups, 14 individual stale command instances.** Separately, `components/sim/docs/demos/module-mix/README.md:13` and its `config.edn:5-6` twin carry a **second, independent defect**: `--format er7` given with no `--emit hl7` — per `docs/cli.md`, `--format er7` requires `--emit hl7`; as written, a corrected `bin/ehrt`-form version of this exact command would still error. Every other stale surface's flag pairing is otherwise valid. README.md's own 15 `bin/ehrt` invocations and the 14 generated `docs/use-cases/*.md` files' ~60 `bin/ehrt` invocations are internally 100% consistent and correct — the defect is confined to the demo/facility-doc class, not the taught convention generally. No doc anywhere demonstrates the `clojure -M:ehrt` alternate form (0 instances found) — only `bin/ehrt` (correct, ~90+ instances) and the stale `:cli` alias (14 instances) exist in the wild. | Sweep to `bin/ehrt` form across all 11 file:line groups in one pass (mechanical find/replace, `clojure -M:cli run` → `bin/ehrt sim run`); fix the `module-mix` pairing drift (add `--emit hl7`) in the same pass since it's adjacent text in the same two files. Then gate: an invocation-lint in the docs-tooling family forbidding `clojure -M:cli` on any live doc surface, scoped the same way this session's sweep was (`README.md`, `AUTHORS-GUIDE.md`, `docs/**/*.md`, `components/*/docs/**`) — mechanism, not vigilance, so a future demo doesn't silently reintroduce the alias. Recommend shape only; not built this session. | fix-session-candidate (cluster: demo/facility-doc invocation sweep + gate) |

**U2 — Help-spec voice and formatting. FULL APPENDIX built this session — see Area B below and the APPENDIX section.**

Folded into Area B's rows (B-1..B-6) and the APPENDIX inventory — the
seeding is confirmed real and is now fully enumerated, not
re-summarized here to avoid a second, drifting copy of the same data.

**U3 — Error-surface survey; `--config` open path FIRST. RESOLVED — audited, the finding is worse than the brief's own [audit] framing anticipated.**

See Area C, row C-1 (the `--config` finding itself) and C-2..C-5 (the
comparison survey). Summary: the `--config` path does not merely fail
to name the artifact — it bypasses the Result vocabulary entirely,
producing a raw JVM stack trace at the **wrong exit code** (1, not the
documented 2). Full detail in Area C.

**U4 — Near-miss suggestion on config-not-found. AUDITED — assessed, recommend defer.**

| id | area | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|---|
| U4 | error surfaces | Ran `bin/ehrt sim run --seed 1 --patients 1 --config <scratch>/near-miss.edn` where only a sibling `<scratch>/near-miss.md` existed (same stem, different extension) | The run never reaches any near-miss-suggestion logic — it crashes on the raw `slurp` before any error-handling code (see C-1) runs at all. No near-miss suggestion exists today, could not exist today without C-1 landing first. | U4 cannot land independently of C-1 — a near-miss suggestion needs a caught, categorized error to attach itself to. Recommend folding U4 into C-1's own fix-session as a small addition (`ehrt.kernel` likely already has a `similar-file` style helper worth checking for reuse; not confirmed this session) rather than treating it as a separate cluster. Nice-to-have, not required for C-1 to close. | fix-session-candidate (cluster: fold into C-1's own fix session, optional addition) |

**U5 — Invocation prominence. CONFIRMED, folds into U1 — no separate action.**

| id | area | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|---|
| U5 | invocation | Same sweep as U1 | `bin/ehrt` is the taught entry everywhere except the 11 file:line groups U1 already names. README.md and every generated use-case doc are exemplary; the demos/facility-doc class is the sole gap. | No separate recommendation — U1's fix-session closes U5 as a side effect. Closing as a distinct row rather than carrying it forward. | close-as-fine (subsumed by U1) |

---

## Area A — Invocation surfaces (brief §6 step 1, AR-UA-3)

Probed by a dedicated sub-agent, full sweep, no sampling (see U1 for
the headline finding). Remaining rows below are the sweep's secondary
observations.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| A-1 | Grep for `clojure -M:ehrt` (the documented bare alternate to `bin/ehrt`) across every scanned surface | Zero hits anywhere in the doc tree. | The alternate invocation form named in the CLI's own header comment (`bin/ehrt` "which cd's to workspace root precisely so cwd never matters... or bare `clojure -M:ehrt`", per ADR-0056's own probe #3) is never demonstrated to a user — nobody reading the docs would learn it exists. | Low priority (bin/ehrt is the better-taught, cwd-safe form and should stay primary) — but consider one mention in README's "What you get" or SETUP.md noting the bare form exists for scripting contexts where a wrapper script is inconvenient. Optional, not a defect. | close-as-fine (nice-to-have only) |
| A-2 | Cross-check every corrected form recommended in U1 against `docs/cli.md`'s current grammar (the generated ground truth) | All 11 fixes resolve cleanly to valid `bin/ehrt` invocations once `clojure -M:cli` → `bin/ehrt` and (module-mix only) `--emit hl7` is added; no other grammar drift found in the affected commands (seeds, flags, group/verb names all still valid). | The fix is mechanical, not a case-by-case redesign — confirms U1's own "mechanical find/replace" framing. | No new action — supports U1's recommendation. | close-as-fine |
| A-3 | `docs/simulate-your-facility.md:202` bare `ehrt sim identifiers --seed <seed> --patients <n> [--config <file>]` inline mention (placeholder flags, not a runnable fence) | This is the one instance, repo-wide, of the "referential bare `ehrt <verb>`" prose convention (used consistently elsewhere for man-page-style naming) carrying flag placeholders, making it read almost like a copyable command despite not being one. | Borderline — not miscategorized as broken (no stale alias, no wrong flags), but its form invites a reader to copy-paste placeholders literally. | If this doc is touched for U1's fix session anyway (it's one of the 11 file:line groups, via the separate stale-alias fence at line 170-171), consider wrapping this mention in a real fence with concrete example values instead of angle-bracket placeholders, matching the rest of the doc's style. Cosmetic, optional. | close-as-fine (cosmetic, fold into U1's pass if convenient) |

---

## Area B — Help voice (brief §6 step 1, AR-UA-4)

Probed directly this session against `bases/cli/src/ehrt/cli/help.clj`
(281 lines) in full, plus live transcripts of `bin/ehrt help` / `bin/ehrt` / `bin/ehrt sim`.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| B-1 | `grep -o 'ADR-[0-9]\{4\}'` scoped to `cli-spec`'s own data (lines 14–178, excluding the namespace docstring and `write-cli-md!`'s maintainer docstring, which are never rendered to a user) | **24 ADR-token citations** across the exit-codes table (1), 2 of 4 global-flag docs, and the doc/flag strings of 7 of 9 groups (artifact, corpus, gate, version, sim, show, play — `check` and `doctor` group docs are themselves ADR-free, though `check`'s doc carries a `ruling 7` citation instead, same class of defect). Distinct ADR numbers cited: 0004, 0005, 0010, 0012, 0013, 0014, 0015, 0016, 0017, 0019 (10 distinct ADRs). | Every one of these is maintainer-voice content on a user-facing surface — a user reading `ehrt help gate` sees "(ADR-0012)" with no way to look it up (ADRs live in `notes/adr/`, not shipped to an end user, and even if they were, "why should I care what ADR-0012 says" is not a question a first-time operator is asking). Exactly the brief's own U2 seeding, now fully counted. | Relocate every ADR citation out of `cli-spec`'s `:doc`/flag-doc strings into source comments beside the data (the reasoning stays, in maintainer territory) — the two-voices-two-homes principle (brief §3). This is wording work the design channel should draft (which citations become which comment, and where the user-facing sentence loses the parenthetical without losing meaning) before a fix session executes it. | design-channel-draft |
| B-2 | Same scope, grep for milestone/ruling tokens (`D[0-9]+`, `M[0-9][ab]?`, `SS-[0-9]`, `ruling [0-9]`) | 14 milestone-tag occurrences (`D4`, `D9`, `D10`×3, `D11`, `D12`, `D13`×4, `SS-2`×2, `SS-3`×2) plus 3 `ruling 7` citations (corpus, gate, and check group docs each carry one). | Same class of defect as B-1 — internal project-history shorthand meaningless to an operator who has never read the design-channel's own ruling register. `D13` alone appears 4 times (artifact's `--all` flag, version's group doc, doctor's group doc, artifact's own doc line) — the single most repeated agent-speak token in the spec. | Same recommendation as B-1 — same relocation pass, same design-channel-draft treatment, likely the same commit (both are "citation-shaped" tokens the two-voices principle treats identically). | design-channel-draft |
| B-3 | Grep for keyword-syntax (`:no-verdict`, `:generator`, `:play-input-unsupported`) and internal namespace/function references (`ehrt.sim.interface/run-command`, `corpus.intake/sniff-format`) leaking into `:doc` strings | 3 bare EDN keywords leak into user-facing prose (exit-code 3's own doc, `play`'s group doc, `sim version`'s own verb doc); 2 fully-qualified internal function references appear verbatim (`sim`'s own group doc names `ehrt.sim.interface/run-command`; `gate`'s own group doc names `corpus.intake/sniff-format`). | A user has no reason to know Clojure keyword syntax or this repo's own internal namespace layout — these are implementation details, not behavior descriptions, and they read as debug output rather than help text. | Same relocation-not-deletion treatment: describe the *behavior* in plain language ("gate sniffs which standard a file follows" rather than naming the function that does it); the internal reference moves to a source comment beside the spec entry. Design-channel-draft, likely folds into the same rewrite pass as B-1/B-2. | design-channel-draft |
| B-4 | Character-length measurement of all 91 `:doc` strings in `cli-spec` (Python regex extraction + `len()`), cross-checked against the live `bin/ehrt help`/`bin/ehrt` transcript | Longest four: `corpus generate`'s own doc (698 chars), `play`'s group doc (675), `gate`'s group doc (651), `corpus`'s own group doc (645). None of `render-flag`/`render-flags`/`render-verb`/`render-group`/`render-top-level` (the only rendering functions in `help.clj`, read in full) perform any line-wrapping or `textwrap`-style fill — each `:doc` string is emitted as one continuous string with no embedded newlines, relying entirely on the terminal's own soft-wrap. Confirmed live: the captured `bin/ehrt help` transcript shows `corpus`'s own group line as one unbroken paragraph with no continuation indentation, exactly as the source predicts. | The renderer doesn't wrap — it can't, because the data itself is undifferentiated prose. At a common 80-column terminal, a 650-700 character doc string soft-wraps across 8-9 lines with zero indentation to signal "this is still the corpus group's description," visually indistinguishable from the start of the next group's own line. This is a rendering-mechanism gap, not just a wording one — even a perfectly user-clean rewrite of these same doc strings would still wrap illegibly at their current length. | Two independent fixes, both needed: (a) content-side, shorten the four worst offenders as part of the same voice rewrite (a group's headline doc should be 1-2 sentences; today's are 4-6 packed into one run-on); (b) mechanism-side, consider a real `render-flags`/`render-group` line-wrap-with-hanging-indent (a small, well-tested pure-function addition, in scope for docs-tooling's existing style) so future long strings degrade gracefully instead of silently wrapping raw. (a) is design-channel-draft; (b) is a separate, small fix-session candidate the design channel should scope alongside the rewrite, not before it (shortening may make (b) less urgent, but not eliminate the general risk). | design-channel-draft (content) + fix-session-candidate (cluster: help-render line-wrap mechanism, sequenced after the content rewrite) |
| B-5 | Live transcript comparison: `bin/ehrt` (bare, no args) vs `bin/ehrt help` — byte-diffed the two captured transcripts | **Identical stdout, byte for byte** (`Usage: ehrt <group>...` through the exit-codes table) — but **`bin/ehrt` exits 2, `bin/ehrt help` exits 0.** Confirmed by direct capture (not piped, `$?` read directly): bare invocation's exit code is 2 (`result/error :cli-help`, per `core.clj`'s own dispatch, line ~1476 `(result/error :cli-help {:text ...})`, which `result->exit-code` maps to the catch-all 2); `help`'s own dispatch path apparently short-circuits to a genuine 0 before reaching that same result construction — the two code paths converge on identical text but diverge on exit code. | Same content, different exit code depending on *how a user asked for it* — a script that does `bin/ehrt || echo "usage shown"` behaves differently than the semantically-identical `bin/ehrt help || echo "usage shown"`. Whether "no group given" should be an operational error (2, "you did something wrong") or a success (0, "you asked for help and got it, same as `--help`") is a genuine design question, not obviously a bug — `--help` itself is documented to "exit 0 without running it," and bare invocation arguably falls in the same bucket. | Author ruling needed: is bare `bin/ehrt`'s exit 2 intentional (distinguishing "you forgot the group" from "you asked for help") or an oversight of the `--help`/`help` convention already established elsewhere in this same spec? Either answer is a one-line fix (change the exit code, or document the distinction in the exit-codes table itself, which currently doesn't mention this case at all). | ruling-needed |
| B-6 | Live transcript: `bin/ehrt sim` (a valid group, no verb given) | `{:status :error, :category :unknown-command, :payload {:args ["sim"], :valid-options ["artifact" "corpus" "gate" "check" "version" "doctor" "sim" "show" "play"]... }}` — wait, actual transcript shows `:valid-options ["run" "check" "identifiers" "version"]` (sim's own verb list, not the top-level group list) and `:hint "run: ehrt help"`. | The dispatcher correctly recognizes `sim` needs a verb and lists `sim`'s own valid verbs (not top-level groups) — the underlying logic is right. But the message reuses the exact same `:category :unknown-command` shape and the exact same generic hint (`"run: ehrt help"`) as a genuinely-unrecognized top-level command (compare Area C, `frobnicate`'s own transcript) — nothing in the rendered message distinguishes "you typed a real group but forgot its verb" from "you typed something that isn't a command at all." A more specific hint (`"run: ehrt help sim"`) would point a confused user directly at the one help page they need, instead of the generic top-level listing. | Small, low-risk fix: when the unrecognized token matches a real group name (the `:valid-options` list itself already proves the dispatcher knows this), tailor the hint to `ehrt help <that-group>` instead of the generic `ehrt help`. Group with the other error-message polish work (Area C). | fix-session-candidate (cluster: error-message hint specificity, pairs with Area C's error-naming work) |

---

## APPENDIX — Full classified inventory of `help.clj`'s user-facing strings (AR-UA-7's one complete deliverable)

Scope: every string in `cli-spec` (`bases/cli/src/ehrt/cli/help.clj`
lines 14–178) that `render-top-level`/`render-group`/`render-verb`
ever emits to a user — 91 `:doc` strings (group docs, verb docs, flag
docs) plus the exit-codes table (4 rows) and positional-docs (5
occurrences). The namespace docstring (lines 1–12) and `write-cli-md!`'s
own docstring (lines 262–281) are maintainer-only and excluded — they
are never rendered by any function in this file.

Classification key: **clean** = no defect; **agent-speak** = carries an
ADR/milestone/ruling citation, a bare EDN keyword, or an internal
namespace/function reference; **over-long** = >250 chars with no
internal structure, a soft-wrap risk per B-4; a string may carry both
`agent-speak` and `over-long`.

| Surface | Class(es) | Token(s) | Length |
|---|---|---|---|
| Exit code 0 doc | clean | — | 16 |
| Exit code 1 doc | clean | — | 25 |
| Exit code 2 doc | clean | — | 61 |
| Exit code 3 doc | agent-speak | `ADR-0010`, `:no-verdict` | 97 |
| `--json` global flag | clean | — | 47 |
| `--pretty` global flag | agent-speak | `ADR-0013` | 96 |
| `--edn` global flag | agent-speak | `ADR-0013` | 100 |
| `--help` global flag | clean | — | 53 |
| top-level doc | clean | — | 128 |
| `artifact` group doc | agent-speak | `ADR-0005` | 61 |
| `artifact fetch` verb doc | clean | — | 61 |
| `artifact fetch --all` flag | agent-speak, over-long | `D13` | 261 |
| `artifact resolve` verb doc | clean | — | 56 |
| `--name`/`--version`/`--lockfile` (artifact-flags, ×3) | clean | — | 27–45 |
| `corpus` group doc | agent-speak, over-long | `ruling 7`, `D4`, `ADR-0015`, `SS-2`, `SS-3` | 645 |
| `corpus generate` verb doc | agent-speak, over-long | `ADR-0015`×2, `D9`, `ADR-0019` | 698 |
| `corpus generate` flags (×15) | mostly clean | 1 cites nothing but is dense sim-vs-synthea dual-purpose prose | 15–90 typical |
| `corpus mutate` verb doc | clean | — | 74 |
| `corpus mutate` positional-doc | clean | — | 158 |
| `corpus intake` verb doc | agent-speak, over-long | `SS-2`, `SS-3` | 387 |
| `corpus intake` positional-doc | clean | — | 196 |
| `corpus operators` verb doc | agent-speak | references "docstring prose" (internal authoring convention) | 141 |
| `--format` (operators) | clean | — | 45 |
| `gate` group doc | agent-speak, over-long | `ADR-0012`, `D11`, `ADR-0019`, `ruling 7`, `corpus.intake/sniff-format` | 651 |
| `gate` positional-doc | clean | — | 148 |
| `gate v2` verb doc | clean (but names "HAPI," an unexplained third-party library) | — | 49 |
| `gate fhir` verb doc | clean | — | 51 |
| `gate fhir --no-verdict-cache` | agent-speak | `ADR-0016` | 106 |
| `gate v2-nist` verb doc | agent-speak, over-long | `ADR-0012` | 442 |
| `gate v2-nist --profile` | over-long (unusual: uses "Π", a Greek letter, in prose) | — | 335 |
| `gate-common-flags` (×4) | 1 agent-speak (`--treat-no-verdict-as` cites `ADR-0010`), rest clean | `ADR-0010` | 33–150 |
| `check` group doc | agent-speak | `ruling 7` | 174 |
| `check` flags (×6) | clean | — | 25–90 |
| `version` group doc | agent-speak | `D13` | 133 |
| `doctor` group doc | agent-speak, over-long | `D13` | 293 |
| `sim` group doc | agent-speak | `ADR-0005`, `ADR-0012`, `ehrt.sim.interface/run-command` | 129 |
| `sim run` verb doc | clean | — | 108 |
| `sim run` flags (×11) | clean | — | 20–271 (`--format`'s own doc is the longest at 271 but is example-bearing and clear, not flagged over-long by content, only by raw length) |
| `sim check` verb doc | clean, good worked example | — | 172 |
| `sim identifiers` verb doc | clean, over-long by length only | — | 296 |
| `sim identifiers` flags (×3) | clean | — | 22–90 |
| `sim version` verb doc | agent-speak | `:generator` (bare keyword) | 92 |
| `show` group doc | agent-speak, over-long | `ADR-0013`, "LF-joined segments" (implementation detail) | 449 |
| `show` positional-doc, `--path` flag | clean | — | 90, 27 |
| `play` group doc | agent-speak, over-long | `ADR-0014`, `ADR-0015`, `:play-input-unsupported`, "msg-%03d" (format-string internal detail) | 675 |
| `play` positional-doc | clean, over-long by length | — | 200 |
| `play --sink` flag | agent-speak | `ADR-0017`, `ADR-0014` | 233 |
| `play` other flags (×3: `--rate`, `--idle-cap`, `--ticker`) | clean | — | 34–190 |

**Totals:** 91 rendered `:doc` strings + 4 exit-code rows + 5
positional-docs = 100 user-facing strings surveyed, all 100
classified, none skipped. **24 distinct agent-speak citations**
(ADR/D/ruling tokens) across **19 strings**; **3 bare-keyword leaks**;
**2 internal-function-name leaks**; **11 strings exceed 250 characters**
with no internal wrap structure (the four worst are B-4's own
headline finding). Per-group defect concentration: `corpus`, `gate`,
and `play` carry the heaviest combined agent-speak + over-long burden
(their own group docs are 3 of the 4 longest strings in the entire
spec); `check`, `sim run`'s own flags, and `artifact resolve` are the
cleanest surfaces, near-zero defects.

This table is the design channel's own rewrite worklist (brief §6
step 2) — every row needing `design-channel-draft` treatment per B-1/
B-2/B-3 is enumerated here by exact surface, not by sample.

---

## Area C — Error surfaces (brief §6 step 1, AR-UA-5)

Probed directly this session: every `result/error`/`result/rejected`
call site in `bases/cli/src/ehrt/cli/core.clj` enumerated by grep (26
distinct sites), cross-checked against direct transcripts of 6
deliberately-triggered error conditions. All throwaway paths under the
session scratchpad (`/tmp/claude-1000/.../scratchpad/`), nothing
written into `config/` or the tracked tree; `out/corpus/sim-s1-p1`
(written once by an Area D probe) removed immediately after.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| C-1 | `bin/ehrt sim run --seed 1 --patients 1 --config <scratch>/does-not-exist.edn` (the brief's own [audit] open item, run FIRST) | `Execution error (FileNotFoundException) at java.io.FileInputStream/open0 (FileInputStream.java:-2). <path> (No such file or directory)` — a raw JVM stack-trace-style report, **exit code 1** (Clojure's own default uncaught-exception exit, not this CLI's documented 2 for "operational error"). Root cause read directly: `components/sim/src/ehrt/sim/run.clj:194-208`, `merge-config-file`, does `(edn/read-string (slurp path))` with no exception handling at all — every OTHER file-path-consuming operation in this codebase (checked: `--lockfile` on `ehrt version`, `gate PATH` via `sniff-gate-command`) goes through the `Result` vocabulary and returns a clean `{:status :error :category ... :payload {:path ...}}` naming the path; this is the sole exception. Confirmed the same crash reproduces identically via `bin/ehrt corpus generate sim --config <missing>` (same `merge-config-file` call, shared code path) and via a **malformed-but-existing** EDN config (`Execution error at ehrt.sim.run/merge-config-file (run.clj:207). EOF while reading`, exit 1) — the malformed case additionally leaks an internal namespace/line-number reference (`ehrt.sim.run/merge-config-file (run.clj:207)`) into the user-facing message and never names the config path at all. | This is not merely "doesn't name the artifact well" (the brief's own softer framing) — it's a structural gap: the one config-reading code path in the entire CLI surface that never entered the `Result` vocabulary at all, so it can't produce a clean error even in principle without a code change, and it violates the exit-code contract itself (1, meaning "legitimately rejected," when nothing was rejected — the program crashed). Every sibling code path (`--lockfile`, `--profile`, `PATH` for gate/show/play) demonstrates the correct pattern already exists in this codebase; `merge-config-file` just doesn't use it. | Wrap `merge-config-file`'s `slurp`+`edn/read-string` in the same Result-returning pattern its siblings already use: catch `java.io.FileNotFoundException` → `result/error :config-not-found {:path path}`; catch EDN parse failure → `result/error :config-unreadable {:path path :message ...}`. Both propagate through `run-command`/`identifiers-command`/`generate-sim-command` the same way `:missing-required-opt` already does (confirmed clean, see C-3) — no dispatch-layer change needed, only this one function. Small, well-scoped, single-file fix; the highest-priority row in this register (the brief's own opening incident traces directly to this class of defect). | fix-session-candidate (cluster: `--config` error handling — highest priority) |
| C-2 | `bin/ehrt sim run --patients 1` (missing required `--seed`) | `{:status :error, :category :missing-required-opt, :payload {:message "--seed is required (determinism is a feature, not a default)", :opt :seed}}`, exit 2 | Clean, exemplary: names the missing flag (`:opt :seed`), gives the reason in plain language, correct exit code. This is the pattern C-1 should follow. | No action — this is the positive control, cited by C-1's own recommendation. | close-as-fine |
| C-3 | `bin/ehrt frobnicate --seed 1` (unknown top-level command) | `{:status :error, :category :unknown-command, :payload {:args ["frobnicate"], :valid-options [...9 real groups...], :hint "run: ehrt help"}}`, exit 2 | Clean: names the bad input, lists every valid alternative, gives a next step. | No action. | close-as-fine |
| C-4 | `bin/ehrt sim run --seed 1 --patients 1 --bogus-flag foo` (unknown flag) | `{:status :ok, :payload {...ran successfully, :invocation {:opts {:seed 1, :patients 1, :bogus-flag "foo"}}...}}`, exit 0 — the run **succeeds silently**, with the bogus flag simply absorbed into `:opts` and echoed back in the manifest, never validated or rejected. | Unlike every other error class surveyed, an unrecognized flag is not an error at all — it's silently accepted and ignored (functionally: a typo in a flag name produces no feedback whatsoever, the run just proceeds as if the flag had never been given). A user who mistypes `--patiens` for `--patients`, for example, would get a successful-looking run with the WRONG value for the field they thought they were setting (silently falling back to `--patients`'s own default instead), no diagnostic at all. | This is arguably the most user-hostile gap in the entire error survey — silent misconfiguration is worse than a loud crash, because nothing signals that anything went wrong. Recommend validating opts against each verb's own declared flag set (which `help.clj`'s own `cli-spec` already enumerates exhaustively — the coverage-test infrastructure ADR-0013 introduced already cross-checks spec-vs-dispatch, so the data needed for this check already exists) and rejecting unknown flags with a `:category :unknown-flag {:flag ... :did-you-mean ...}`-shaped error. Sizeable (touches `cli/parse` or a new validation layer, not just error-message wording) — its own fix session, not folded into C-1. | fix-session-candidate (cluster: unknown-flag validation — new, standalone) |
| C-5 | `bin/ehrt version --lockfile <scratch>/no-such-lockfile.edn` | `{:status :error, :category :not-found, :payload {:path "<scratch>/no-such-lockfile.edn"}}`, exit 2 | Clean: names the path, correct category, correct exit code — confirms `artifact/read-lockfile` (the shared lockfile-reading path used by `version`/`fetch`/`resolve`/`corpus generate synthea`) already implements the pattern C-1 is missing. | No action — second positive control, further evidence C-1 is the outlier, not the norm. | close-as-fine |

---

## Area D — First contact (brief §6 step 1, AR-UA-6)

Probed directly this session; live transcripts captured, not simulated.

| id | probe | evidence | finding | recommendation | disposition |
|---|---|---|---|---|---|
| D-1 | Bare `bin/ehrt` (no args) — full transcript captured | Renders the complete `Usage:`/groups/global-flags/exit-codes block (30 lines) — **byte-identical** to `bin/ehrt help`'s own output (D-2) — but exits **2**, not 0. See B-5 for the full analysis; recorded here as the first-contact instance of the same evidence. | A stranger's very first command, if they type `ehrt` with no arguments the way many CLIs invite ("just run it and see"), gets what LOOKS like a successful help screen but returns a non-zero, "operational error" exit code — in a shell script or CI context (`bin/ehrt && echo ok`), this reads as failure. | See B-5's ruling-needed disposition — same finding, same recommendation, not duplicated as a second action item. | ruling-needed (see B-5, same finding) |
| D-2 | `bin/ehrt help` — full transcript captured | Same 30-line block, exit 0. Content itself: 9 groups listed with their full (often 400-700 char, per Area B) doc strings inline, no pagination, no `--brief`/summary mode. | As a piece of onboarding content, the SIGNAL (9 groups, a one-line-each summary, a pointer to `ehrt help <group>` for more) is buried in the NOISE of Area B's over-long strings — a first-time reader scanning this screen has to visually parse 4-6 run-on sentences per group before finding the actual group name's meaning. This is a direct, first-hand demonstration of B-4's finding, not a new defect — recorded here because AR-UA-6 asks this row to judge the EXPERIENCE, not just the source. | No new recommendation beyond B-1..B-4's rewrite + line-wrap work — this row is corroborating evidence for prioritizing that work, since it's the literal first thing `ehrt help` (the documented "when in doubt" command) shows a new user. | close-as-fine (evidence row, see B-1..B-4) |
| D-3 | `bin/ehrt sim` (a real group, no verb) | `{:status :error, :category :unknown-command, :payload {:args ["sim"], :valid-options ["run" "check" "identifiers" "version"], :hint "run: ehrt help"}}`, exit 2 | Same transcript as B-6; recorded here as a first-contact instance (a new user exploring by typing `ehrt sim` to "see what sim does," a natural first move, gets a generic error rather than sim's own verb list rendered as help). | See B-6's recommendation (tailor the hint to `ehrt help sim`) — not duplicated. | fix-session-candidate (see B-6, same finding) |
| D-4 | README Quickstart's own first two commands, run verbatim from a clean `out/` state: `bin/ehrt help` (D-2, above) then bare `bin/ehrt corpus generate` (no `--config`/`--out-dir`, the documented zero-flag default) | `{:status :ok, :payload {:out-dir "out/corpus/sim-s1-p1"}}` — one line, terse, immediately actionable (names the exact directory the corpus landed in). `out/corpus/sim-s1-p1` removed after the probe (gitignored, per `.gitignore:8`, never staged). | Sharp contrast with D-2: once a user is past the help screen and running an actual command, the SIGNAL:NOISE ratio flips completely — this is close to ideal terse output, no agent-speak, no citations, exactly the information needed. The remainder of the Quickstart (artifact fetches, Synthea generation, mutate/gate/check) was **not run this session** — those steps require network-fetched artifacts (Synthea, a JDK, the FHIR validator) and the audit's own fence (AR-UA-7, throwaway-and-scratch-only, no long-running network operations implied by the session's own scope) weighs against a multi-minute fetch chain for a UX-signal question already well-answered by the first two commands. Disclosed as incomplete, not silently skipped. | Judgment row, not an actionable finding: the CLI's own RUN output (as opposed to its HELP output) is consistently good — this is worth stating in the register precisely because it means the UX arc's real work is concentrated in HELP TEXT (Area B) and ERROR TEXT (Area C), not in command output generally. If a future session wants full Quickstart fidelity, it should budget for the network-dependent steps explicitly. | incomplete (help + first data-producing command run; artifact-fetch-dependent remainder not run, disclosed) |

---

**Register summary:** 5 seeded rows, 3 carrying their own disposition
(U1 expanded significantly — 8→11 file:line groups, +1 independent
flag-pairing defect, fix-session-candidate; U4 assessed and folded
into C-1, fix-session-candidate; U5 closed, subsumed by U1,
close-as-fine — U2 folds into Area B's own rows in full and U3
resolves into C-1, neither carries an independent disposition of its
own) plus 18 new rows across Areas A–D (A: 3, B: 6, C: 5, D: 4, plus
the APPENDIX's own 100-string complete inventory) — 21 total rows
carrying a disposition. Disposition counts: close-as-fine 8 (U5, A-1,
A-2, A-3, C-2, C-3, C-5, D-2), ruling-needed 1 (B-5; D-1 cites the
identical finding and is not double-counted), fix-session-candidate 6
(U1, U4, B-4's mechanism half, B-6, C-1 — flagged highest priority —
C-4; D-3 cites B-6's identical finding and is not double-counted),
design-channel-draft 4 (B-1, B-2, B-3, B-4's content half — the
APPENDIX is their shared worklist), incomplete 1 (D-4, disclosed,
network-dependent remainder not run). No area was left unprobed; the
APPENDIX inventory (AR-UA-7's one required-complete deliverable)
covers all 100 user-facing strings in `help.clj`, none skipped.
