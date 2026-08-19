# Archived prompt: repo-review-4 (2026-08-18)

Session prompt -- repo review 4: the assessment (register + plan),
hybrid shape, no fixes -- ADR-0154

## Context

Claude Code under R30 in `ehr-testing-tools`, running the `repo-review`
skill (`.agents/skills/repo-review/SKILL.md`). HEAD at handoff:
`4d6ff78` (ADR-0153 addendum; tree clean; CI green at `5563f71` and
`4d6ff78`; last tag `stable-20260818-surge-policy-self-check-202`
@`5563f71`, no tag owed). Roadmap row `roadmap.md#repo-review-4` (OPEN
PRIORITY 2). This IS ADR-0154. The window under review is ADR-0140..0153
(fourteen ADRs, 2026-08-15 -> 08-18).

---

## The prompt, verbatim

Session prompt -- repo review 4: the assessment (register + plan), hybrid shape, no fixes -- ADR-0154

**Context**

Claude Code under R30 in ehr-testing-tools, running the `repo-review` skill (`.agents/skills/repo-review/SKILL.md`, 172 lines -- read it whole; this prompt binds it, it does not replace it). HEAD at handoff: 4d6ff78 (ADR-0153 addendum; tree clean; CI green at 5563f71 and 4d6ff78; last tag `stable-20260818-surge-policy-self-check-202` @5563f71, no tag owed). Roadmap row `roadmap.md#repo-review-4` (OPEN PRIORITY 2): "chartered at roughly 15 ADRs past ADR-0139, i.e. approximately ADR-0154; the standing cadence rule is ADR count, not calendar (ruling Q3 'a.', 2026-08-15). Inherits review 3's twelve-row watch-list, D8-5's one surviving row among them (56 of 74 command fences have no exerciser -- ruled out of the fix session, handed on in ADR-0140)." This IS ADR-0154. Sequencing gate per the skill: no arc is mid-flight (ADR-0153 closed and tagged) -- confirm at Step 0.

Prior assessment (the baseline this run scores against): review 3, `.agents/plans/2026-08-15-repo-review-findings.md` (415 lines) + `2026-08-15-repo-review-3-plan.md`, ADR-0136..0140, arc close ADR-0139 whose "Review 4's inherited watch-list" section (:464-482) is the twelve rows. The window under review is ADR-0140..0153 (fourteen ADRs, 2026-08-15 -> 08-18): fence battery (0140), event-log contract arc (0141-0142), compression arc (0143-0147), exercised-sources gate (0148), traces gate (0149), shape defects + contract 1.1.0/1.2.0 (0150-0151), sim-theory head hop (0152), surge fix (0153).

**Shape -- author ruling 2026-08-18, verbatim:** "Q1 c, Q2 register and separate fix session."

* (c) HYBRID. The coordinating session runs the eight-dimension battery ITSELF under a probe budget: at most 12 probes per dimension (96 cap), each recorded as dimension / method / expected / observed / verdict per skill step 3; when a dimension's budget runs out with probes un-run, the un-run probes are LISTED (skill: "probes that did not run -- named, not dropped"), never silently skipped. Sub-agents are dispatched for exactly THREE lines this window opened, one sub-agent each, each in its own fresh clone of 4d6ff78, no probe cap, findings returned as rows in the register's format:
  * L-1 ORACLE COVERAGE. What `bin/regression-oracle`'s 35 roots actually witness. Three findings in four sessions: Z-segments are outside it (no root builds a site profile -- ADR-0150 (a)); the ground-truth half is inside it (ADR-0151 (d), 32 movers); the whole capacity path is vacuous under it (no root emits a `:bed-ready` event -- ADR-0153). Enumerate, per root, which event kinds / emitter families / engine paths its digest can move on; name every documented engine or emitter surface NO root can move (the "IDENTICAL is vacuous here" set); check `digest.clj`'s own docstring claims against that. Deliverable: a coverage matrix (root x witnessed surface) and the vacuous set.
  * L-2 EXIT-CODE / HARNESS TRUTHFULNESS. ADR-0152 recorded a red suite reported as "exit code 0" by an `echo | tee` wrapper; the captured MAKE_EXIT caught it. Watch-list H-2/H-3 said "watch for a NEW way to mask an exit code". Enumerate every place a session's or CI's exit code passes through a pipe, wrapper, subshell, `tee`, `|| true`, background job, or make recipe (Makefile, `bin/*`, `.githooks/*`, `test.yml`, the build-session skill's taught commands); for each, state whether the true exit survives; the ADR-0152 instance verbatim. Deliverable: the pass-through matrix and any masking site not already gated.
  * L-3 GENERATED-SURFACE COMPLETENESS. Two sessions tripped an undocumented mover of `.agents/state-derived.md` (ADR count, test-namespace count -- ADR-0151, 0152); ADR-0152 also found a `.edn` that did not validate against the schema its header claimed. For every GENERATED artifact (the `docsgen` graph + freshness list + frozen baselines + `state-derived.md`): what inputs move it (from its generator's source, not its header), whether every input is tracked, whether the generator's inputs are documented at the artifact, and whether any generated file's header makes a claim its generator does not enforce. Deliverable: input->artifact matrix and the undocumented-mover list.

  Sub-agent findings are TRANSCRIPT-WITNESSED until the coordinator re-derives at least one cited artifact per finding in ITS OWN clone (a grep, a hash, a run) -- rows that survive re-derivation enter the register as ordinary rows; rows that fail it are recorded as "sub-agent claim, coordinator could not reproduce", never dropped and never promoted. The register states, per row, which it is.
* Q2: this session lands the REGISTER and the PLAN only (skill steps 1-5). NO fix, NO disposition beyond PROPOSED, NO skill amendment (an amendment is a plan item for ruling), NO roadmap row closed. Fix sessions are separate, per the author's rulings on the plan. Review 3 disposed in-session and handed 56/74 fences forward unfixed; this run hands the author a plan and stops.

**Read first**

1. `.agents/skills/repo-review/SKILL.md` whole; review 3's register, plan, prompt, and ADR-0139 :464-566 (watch-list + channel errata); ADR-0140 (what review 3 ruled out).
2. Every ADR 0140-0153 at heading depth PLUS each one's Deviations / findings / "things worth your attention" sections in full (this is skill step 2 -- do not sample it; the window is fourteen ADRs, read them); every session record 2026-08-15..08-18; the design-channel handoff `.agents/handoff/*` if present.
3. `.agents/rulings.md` whole (265+ lines) -- D2 maps every standing row to its gate; `.agents/state.md` (hazard register), `state-derived.md`; `roadmap.md` OPEN rows; `reading-sets.edn`.
4. `digest.clj`, `bin/regression-oracle`, `Makefile`, `test.yml`, `bin/preflight`, `bin/post-push-verify`, `bin/tag-ceremony`, `bin/close-scaffold`, `.githooks/*` (L-1/L-2/L-3 seeds; the coordinator reads them too, so it can re-derive).

**Author rulings, verbatim**

* "Q1 c, Q2 register and separate fix session." (2026-08-18)
* Cadence: ruling Q3 "a." 2026-08-15 -- ADR count; this is the run.
* Tag: no tag owed at Step 0. The review's own close tag: pay in-session if the tip run concludes success while open, else next Step 0 -- say which (`R-session-verifies-ci-via-gh`).

**Step 0 (skill step 1)**

Fresh clone, tip 4d6ff78; `bin/preflight` (last five CI runs disclosed); baseline `make test` unpiped, MAKE_EXIT captured, reconcile vs ADR-0153's 348 blocks / 3,960 tests / 17,758 assertions; `poly check`; reading sets vs baselines; oracle pre-digest over 35 roots kept as the run's artifact (L-1 needs it); confirm no arc mid-flight. Then re-derive review 3's own summary arithmetic from its per-dimension disposition counts (skill step 4's standing sub-step) and record it before drafting anything. Dispatch L-1/L-2/L-3 sub-agents NOW, in parallel with your own battery, each with: the tip, its line's charter above verbatim, the row format, and the instruction to return rows + the commands that produced each.

**Step 1 -- history scan (skill step 2)**

From the fourteen ADRs and their session records: every incident, deviation, disclosed self-inflicted red, prediction miss, and channel erratum, classified to a rubric dimension. Seeds this window itself named (verify each, do not carry): the executable-bit class hit twice (ADR-0147 S-7, ADR-0149) and now gated; `poly test brick:` as pre-push gate (ADR-0149 -> `R-full-suite-before-push`); the `echo | tee` exit masking (ADR-0152); `state-derived.md` undocumented movers (0151, 0152); the `.edn`-does-not-validate finding (0152, C-1 was UNDERSTATED); the four channel-erratum classes (history-as-current, carry-forward figures, unearned specificity incl. mechanisms that do not exist -- `docsgen_test` population, `valid-log?`, oracle IDENTICAL (d) -- and fence-names-wrong-instrument); one message-only `--amend` of an unpushed commit (ADR-0153, disclosed) -- a plan item: name the precedent or forbid it. Repeat-hit classes raise their dimension's severity, per the skill.

**Step 2 -- probe battery (skill step 3), eight dimensions, budget 12 each.**

Beyond the skill's own probes, this window's specific probes: D1: `state.md` pointer rot (every register/gate it names exists and runs -- ADR-0147 changed the probe's shape, the skill says so); `state-derived.md` `make state-derived` + diff; every stated count in the 0140-0153 ADRs' Verification sections re-derived (blocks/tests/assertions per ADR against `git show <sha>` -- sample all fourteen, it is fourteen numbers). D2: every `rulings.md` row -> its gate, incl. the rows added this window (`R-session-verifies-ci-via-gh`, `R-stop-only-on-two-defensible-readings`, `R-full-suite-before-push`, `R-exercised-implies-gated`, `R-red-pushed-with-green`, `R-budget-stop`, `R-register-hygiene-at-close`, `R-adr-index-generated`, `R-audience-has-entry-path`) -- a law with no gate is a finding; laws stated on multiple surfaces compared for drift (`R-law-surface-propagation` itself; the skills mirror in `.claude/skills`). D3: cold-clone (watch-list D3-1: restore the local method or retire it -- record which, propose the ruling); the ADR-0149 CRLF-in-worktree / `core.fileMode=false` clone facts as an environment-class probe. D4: `make test` timings vs the window's recorded ones (docsgen 36->119 s at ADR-0149; suite ~15 -> ~35 min mentioned across records -- measure, don't carry). D5: the freshness list vs the population of generated files (L-3 feeds this); `demos/traces` and `sim-theory-equations.txt` now on it -- what is still off it. D6: full-window deviation read (watch-list D6-4: fourteen ADRs is the narrowed window the cadence rule promised -- read all). D7: carried-item aging including the twelve watch-list rows (re-derive each row's CURRENT state: C-1 closed 0152; C-2 rowed with S-2; C-3 open; C-4 `state_staleness_tripwire_test` -- fixed or not; D8-5 fence battery ran 2026-08-16 (tag exists) -- its survivor; D1-9/D1-10; `:onboarding` headroom now 1,530 budget after the ratchet) plus rows outside any register: corpus-player slices (chartered ADR-0014, never a row -- a D7 probe that does not use `roadmap.md` as its exclusion oracle, per watch-list C-2's own instruction), the NIST licensing send, the guide's palgebra chapter rulings, `#intake-staging-dir`'s deferred trigger. D8: the fence battery's survivor (56/74 fences without exerciser -- re-count against `exercised-sources.edn` after ADR-0148/0149 added rows; the number moved, measure it).

**Step 3 -- the register (skill step 4)**

`.agents/plans/2026-08-18-repo-review-findings.md`, review 3's row format `id | probe | evidence | finding | recommendation | disposition` (disposition in {ruling-needed, fix-session-candidate, close-as-fine, intake} -- PROPOSED); scoreboard with review 3's scores beside; the probes-not-run list per dimension; the three sub-agent sections with per-row provenance (re-derived vs coordinator-could-not-reproduce); review 3's re-derived arithmetic. Nothing moves.

**Step 4 -- the plan (skill step 5)**

`.agents/plans/2026-08-18-repo-review-4-plan.md`: fix-session candidates batched into proposed sessions (small, fenced, each naming its co-landed gate); rulings needed as lettered options with a recommendation each (at minimum: the `--amend` precedent; D3-1 local cold-clone method; the docsgen per-push tier if D4 says it moved; whether L-1's vacuous set warrants new oracle roots or a coverage statement in `digest.clj`; any skill amendment); deliberately-fine list; probes-not-run. The plan goes to the author. Nothing executes.

**Close (self-archive FIRST)**

Archive to `.agents/prompts/2026-08-18-repo-review-4.md`; open the session record; then ADR-0154 (the assessment ADR: shape as ruled, budgets used per dimension, sub-agent provenance tallies, the scoreboard delta, the plan's location -- NOT an arc close; the arc closes after the fix sessions); roadmap: `#repo-review-4` STAYS OPEN, gains one line pointing at the register and plan (six-line cap); session record with `gh run view` id/conclusion; full `make test` reconciled per namespace vs Step 0 (expected delta ZERO -- no test added; if nonzero, explain); `bin/post-push-verify`; tag per ruling. Commit: "docs: ADR-0154 -- repo review 4 assessment: register and plan landed, nothing fixed"

**Fences**

NO src change; NO test change; NO fix of any finding, however small ("trivial ride-along" is a plan item, not an act); NO skill or rulings amendment; NO roadmap row closed; NO register other than the two new files + the ADR + records touched (state-derived regenerates by `make`, that is not a fence breach); sub-agent rows enter the register ONLY through coordinator re-derivation or with the could-not-reproduce label; probe budget 12/dimension enforced and reported; exit codes unpiped (L-2 is watching); `out/` cleared before runs; R-RP. READ-BACK names the fence: the ADR states files touched (expect: two plans, one ADR, one prompt archive, one session record, roadmap one line, state-derived regenerated) and the delta of the close-phase suite vs Step 0.

---

## Deviation record

See ADR-0154's own Deviations section for the full account. In summary:

1. **The prompt's D4 probe does not match the rubric's D4.** The prompt
   assigns `make test` timings to D4; the rubric's D4 is *error
   honesty*. Both were run — the error-honesty probes under D4, the
   timing measurement under Step 0 with a contention caveat — so neither
   the prompt's probe nor the rubric's dimension was dropped.
2. **The D1 count-chain probe was re-scoped, and the re-scoping is a
   finding.** The prompt asks to re-derive "every stated count in the
   0140-0153 ADRs' Verification sections ... against `git show <sha>`".
   Re-running fourteen full suites at fourteen commits is not feasible
   in one session; the probe run instead was the chain's own internal
   coherence plus one live anchor (this session's Step-0 measurement),
   which is what found **D1-1**.
3. **The prompt repeats the citation defect D1-1 names.** It asks to
   reconcile against "ADR-0153's 348 blocks / 3,960 tests / 17,758
   assertions". ADR-0153 does not carry those figures; its session
   record does. The substance reconciled exactly; the citation did not.
   Recorded as this window's fifth channel erratum rather than adapted
   around silently.
4. **The standing arithmetic sub-step needed a method correction.**
   Re-deriving review 3's summary from the LIVE register gives 42 rows,
   not 40 — because the arc's fix sessions overwrite disposition cells
   in place and added two rows mid-arc. Re-derived against the register
   as first committed (`bc6f46c`), review 3's arithmetic is **exact**.
5. **Sub-agent L-3's return is recorded per its actual outcome** in the
   register's L-3 section, per the "named, not dropped" law.
