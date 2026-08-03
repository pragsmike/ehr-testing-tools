# 2026-08-03 — GMF census tool session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session with a default
working directory of `C:\Users\prags\Documents\ehr-testing-tools`
(`/mnt/c` under WSL). Preflight found `/mnt/c` behind origin (a stale
Windows-mount file-permission bit on `.git/logs/refs/remotes/origin/main`
had been silently failing `git fetch` there) and, once fetched, found
`/mnt/c` mechanically read-only with reject-all commit/push hooks
(`notes/ADRs.md` ADR-0030 J4, landed 2026-08-03 by an earlier session
the same day) — `/mnt/c` is NOT the clone of record. All real work for
this session therefore ran against the ext4 clone
(`~/src/ehr-testing-tools`, UNC `\\wsl.localhost\Ubuntu\home\mg\src\
ehr-testing-tools\...` for Read/Edit/Write, `wsl -e bash -lc "cd
~/src/ehr-testing-tools && ..."` for git/build), synced once via `bin/
sync-mnt-c` from the ext4 clone so `/mnt/c` also carries this session's
own starting point. `feedback-dual-clone-edit-hazard` (persistent
memory) was updated to record the guardrail; it previously (incorrectly,
pre-dating the guardrail) recommended treating `/mnt/c` as writable.
Preflight confirmed ADR-0031/ADR-0032/ADR-0033 all at `origin/main`
(tip `c0cdb3a`) and next ADR is 0034, as the prompt assumed.

## Prompt, verbatim

> 2026-08-03 — Build session: the GMF census tool (parity plan §3)
>
> Context
> The parity plan (APPROVED, ADR-0031) rules that every wave E–I takes its scope from a mechanical census, not hand-read surveys — three survey rows have been overturned by fetched evidence to date. Both defect-fix sessions AR-6 sequenced ahead of this one have landed (ADR-0032 Procedure duration, ADR-0033 engine closure context), so smoke-walk digests recorded now sit on final timing semantics. This session builds the census tool as a `sim-trajectory` DEV ENTRY POINT (ADR-0031 AR-1 — not a CLI verb), runs it against the full Synthea catalog at the pin, and commits the dated census artifact. The design channel pre-ruled the design questions below; cite, don't re-derive.
>
> Read first
>
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. `.agents/plans/2026-08-02-gmf-parity-plan.md` §3 (the spec this session implements) and §4 (the waves the census will rank)
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` (`load-module`, `load-closure`, Result shapes) and `gmf_interpreter.clj` (`run-module` arities, `max-steps` backstop)
> 4. `bin/oracle-src/ehrt/oracle/digest.clj` — the seed-derivation discipline (its own docstring: sequential small `java.util.Random` seeds are NOT independent — reuse its derivation approach), digest canonicalization, and persona-config usage
> 5. `components/sim-trajectory/docs/gmf-interpreter.md` — the prioritization table (the narrative frontier this census converts to data) and §13
> 6. `notes/ADRs.md` — ADR-0031 (AR-1/AR-4/AR-5), ADR-0032/0033 execution notes; next ADR expected 0034
> 7. The D3 throwaway closure-survey script if still present in history (`git log --all --oneline -- '*survey*'` — reuse ideas, not code, if found)
>
> Author rulings (design channel, 2026-08-03; record in ADR-0034)
>
> * AR-1 (input, pin verification). The entry point takes a filesystem path to a Synthea checkout and reads `src/main/resources/modules/**` — no network at run time, no vendoring of the catalog (installed ≠ used). The census artifact records the pin: if the path is a git checkout, `git rev-parse HEAD` must equal the interpreter doc's own pin (`7e08387c68a7f0e21d13076609a159fd473fc902`) or the run REFUSES (errors-as-values: an `:error` result naming the mismatch); if not a git checkout (tarball extract), the tool records a sha256 over the sorted relative-path + content of the modules tree and DISCLOSES pin-unverified-by-git in the artifact header. A census is a claim AT a pin; an artifact that cannot name its pin is not a census.
> * AR-2 (verdict vocabulary, v1). Per module (root + resolved closure): `:ok-walked`, `:load-failed`, `:walk-failed`, `:out-of-scope-by-ruling` (empty is fine; the category exists per ADR-0031 AR-4). Alongside the verdict, the gap detail §3 names: unrecognized state types, transition kinds, condition types, unresolved attributes, unresolved submodules/tables, closure file count. A walk that throws is a `:walk-failed` with the cause recorded — caught, recorded, census continues; the census itself NEVER aborts on a module's failure.
> * AR-3 (substitution tagging — load-bearing). Any module whose closure contains a `wellness: true` Encounter state gets `:disclosed-substitutions [:wellness-timing]` on its census entry, REGARDLESS of verdict — the Wave B create-now normalization (disclosed timing substitution, ADR-0031 AR-5(b)) means such modules may load and walk today under semantics upstream does not have. A substituted walk is never presented as upstream-faithful; Wave G's ledger is countable as exactly the entries carrying this tag. The detection is mechanical (closure scan), extensible to future substitution classes.
> * AR-4 (smoke-walk parameters). Interpreter-layer only (ADR-0031 AR-4's boundary — engine round trips stay per-vendored-root tests). Three seeds per module, derived per digest.clj's discipline; fixed persona-config and a fixed horizon bound (registration + a stated day count large enough to exercise content, small enough to stay fast — the session picks, states it, and records it) so no walk hits the max-steps throw as a matter of course. EVERY census parameter (pin, seeds, persona-config, horizon, tool version) goes in the artifact header: the census must be re-runnable to the byte. Digest: sha256 over a canonical printing of each walk's trajectory
>    * final status, per-seed, recorded per module.
> * AR-5 (artifact). EDN, committed at `components/sim-trajectory/docs/census/<date>-synthea-<pin7>.edn`, plus a short generated summary table (counts per verdict, top gap mechanisms by modules-blocked) appended as a dated section to the interpreter doc's prioritization area — the census SUPERSEDES the hand-read table as the frontier of record; annotate the old table as superseded (dated note, no deletion). The parity definition is now countable: zero `:load-failed` (minus `:out-of-scope-by-ruling`) and every walked module's digests recorded.
> * AR-6 (loose-thread bookkeeping, folded in — docs-only). In the records step: (a) roadmap Deferred gains a row for the regression-oracle tool defect ADR-0033 disclosed (digest.clj read from current checkout only — incompatible with API shape switches; manual per-worktree workaround is not the new normal); (b) the parity plan's Wave H row gains a dated pointer to the pre-horizon straddle finding (ADR-0033 execution note: UTI's mandatory Encounter straddling registration-t trips `:clinical-content-only-when-admitted` on 8 of 10 seeds; the UTI round-trip test's seed-777 dodge retires when H resolves the boundary — write that linkage down where H's design session will read it).
>
> Steps
> Step 0 — Preflight. Build-session preflight; verify ADR-0032 and ADR-0033 at origin; confirm next ADR is 0034. Obtain the Synthea checkout at the pin (clone locally if absent; AR-1's verification gates everything downstream).
> Step 1 — The tool. `ehrt.sim-trajectory.census` (dev-side per workspace convention for non-product entry points — follow how `bin/oracle-src` vs component code is split, and document the invocation in the namespace docstring and the interpreter doc): catalog discovery, closure resolution via `load-closure`, verdict + gap extraction (AR-2), substitution tagging (AR-3), smoke walks + digests (AR-4), EDN artifact emission (AR-5 header discipline). Co-landing: unit tests over inline fixture modules for each verdict class and the substitution tag — the census's own verdicts carry their invariants. Commit: `feat(sim-trajectory): GMF census tool -- load/walk verdicts, substitution tags, pinned artifact (ADR-0034)`
> Step 2 — The census run. Run against the pin; commit the artifact and the generated summary (interpreter-doc section + superseded note per AR-5). Sanity anchors before committing: the eight vendored roots must census as `:ok-walked` (any of them failing is a STOP-AND-ESCALATE — the census disagrees with landed reality, and which one is wrong is a finding, not a fix); the five wellness modules must carry the AR-3 tag if they appear walkable. Commit: `docs(sim-trajectory): first GMF census at pin 7e08387 -- frontier is now data (ADR-0034)`
> Step 3 — Records. ADR-0034 (rulings verbatim, attributed; execution note: verdict counts, anchor checks, anything the census overturned). AR-6's two bookkeeping items. Roadmap: census `Next` row → Done; note that E/F/G ordering now awaits the census ranking (a design-channel read, not this session's call). Session record + prompt self-archive + budget check. Commit: `docs: census session records -- parity frontier converted to data (archives prompt)`
> Fences
>
> * No wave E/F/G mechanism work, no matter what the census shows — the ranking read is the design channel's next move.
> * No engine changes, no loader changes: the census OBSERVES the interpreter as it stands. If observing requires changing the thing observed, that is an escalation.
> * If the census contradicts any landed claim (a vendored root failing, a "fixed" gap reappearing), STOP-AND-ESCALATE with the entry — evidence discipline over rulings, including this prompt's own.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation-record appendix (per the prompt's own Fences clause)

- **The prompt's own Step 2 sanity anchor named "the eight vendored
  roots"; the actual count is SEVEN**
  (`appendicitis`/`ear_infections`/`sepsis`/`sinusitis`/`sore_throat`/
  `total_joint_replacement`/`urinary_tract_infections` — confirmed by
  direct listing of `components/sim/resources/sim/modules/*.json` AND
  by `docs/gmf-interpreter.md`'s own pre-existing D3f regression-
  baseline prose, which already said "SEVEN"). No ruling anywhere in
  `notes/ADRs.md` or the parity plan ever said eight, so this is a
  correction against the prompt's own arithmetic, not a contradiction
  of a landed claim — the STOP-AND-ESCALATE fence names disagreement
  with LANDED REALITY, which this isn't. Disclosed rather than silently
  adjusted; all seven census `:ok-walked`, the anchor's own real intent
  held.
- **`load-closure` itself is not exception-free — a real finding, not a
  census-tool bug, but it required a census-tool defensive change to
  observe safely.** The first full-catalog run crashed outright:
  `ehrt.sim-trajectory.gmf/gmf-v2-timing->v1`'s `case` over a
  `gmf_version 2` distribution `:kind` has no clause for `GAUSSIAN`
  (found first) or `EXPONENTIAL` (found once `GAUSSIAN`-triggering
  modules were wrapped) — both throw raw `IllegalArgumentException`
  rather than a `:rejected` Result, unlike every OTHER load-time
  rejection this loader makes. `census-one` (`development/src/ehrt/
  sim_trajectory/census.clj`) now wraps `gmf/load-closure` in
  `try`/`catch`, converting an uncaught throw into a `:load-failed`
  verdict with the exception's own message/class in `:gap
  :other-rejections` — the SAME defensive discipline `walk-one` already
  applies to the interpreter one layer down, extended to the loader
  once the full sweep proved the loader needed it too. `gmf.clj` itself
  is untouched (the fence's own "no loader changes" line) — named as a
  Wave I / defect-fix candidate in `docs/gmf-interpreter.md` §15 and
  `notes/ADRs.md` ADR-0034's own execution note instead.
- **Two new condition-vocabulary gaps found live, never named in any
  prior wave:** `Race` and `Not` condition types (3 and 1 modules
  respectively) throw at `evaluate-condition`'s own default case. Not
  fixed (interpreter changes are out of this session's fence); recorded
  in §15 and ADR-0034 as new census-named gaps, distinct from the
  state-type gaps every prior wave already tracked.
- **`clojure -M:poly test :all skip:integration` does not exercise this
  session's own co-landing tests.** `clojure -M:poly ws get:projects`
  confirms the `dev` project (`development/src`'s own `:dev` alias)
  carries `:bricks-to-test []` — `poly test` runs per-project against a
  project's own bricks, and dev-entry-point code under `development/`
  is not a brick. Not a regression this session introduced (the SAME
  status `bin/oracle-src`'s own tooling already has, verified by
  precedent, not merely asserted) — disclosed rather than silently
  left unstated, and verified instead by direct `clojure -M:dev:test -e
  '...run-tests...'` invocation (0 failures, 0 errors, both runs this
  session).
