2026-07-29 — User-path errata: retired sim mechanism claims (sibling-checkout / subprocess / repo-as-sibling)

Repo: `github.com/pragsmike/ehr-testing-tools`, main at `7092bc7` (`stable-ehrt-1`). Autonomous session per R30: commit AND push at each checkpoint; hooks gate (WSL provenance, gitleaks, `poly check`); tags and repo-level `gh` mutations are the author's alone. Ask nothing mid-session — decision procedures below; skip-and-report anything they don't cover.

Context

ADR-0005 (`notes/ADRs.md`) mounted sim in-process: `ehrt.tools.sim` calls `ehrt.sim.interface/run-command` directly — no subprocess, no sibling checkout, no availability check (read that file's ns docstring; it is the reasoning-of-record for why the old mechanism is gone). ADR-0010/R34: the user path (root `docs/`) never names Polylith, `components/` paths, or pre-merge repos as architecture; the only external repo it may acknowledge is `ehr-testing-guide`.

The author verified live (2026-07-29, real terminal run from a plain clone with no sibling checkout): `bin/ehrt corpus intake 'sim:?seed=42&patients=5&emit=hl7' --label sim-traffic --out /tmp/corpus` succeeds — 10 messages plus sim's `manifest.edn` cataloged with full provenance. The stale doc claim is refuted by execution, not merely by ADR citation.

The user path contradicts this in three places (channel-probed 2026-07-29 against a fresh clone at `7092bc7` — re-verify, don't trust):

1. `components/tools/docs/use-cases.edn`, strip `:simulator-traffic-as-intake-source`: `:bring` claims a sibling `ehr-testing-sim` checkout, subprocess-only per ADR-0013; the fenced command comment (`:commands :lines` 1–2) repeats it; `:equations` names `sim-subprocess` as an input. `docs/use-cases.md` is GENERATED from this file (`make docsgen`) — never hand-edit the `.md`.
2. `docs/simulate-your-facility.md` (hand-authored, user path): "ask about the tools sibling" (Network-delivery bullet) and "injected downstream by the [tools](https://github.com/pragsmike/ehr-testing-tools) sibling" (Broken-feed bullet) — presents tools as a separate sibling project and links this repo's own GitHub URL as if external. Both capabilities are `ehrt` verbs in this same workspace now.
3. `docs/glossary.md` (~line 560) calls `ehr-testing-guide` "the sibling repository" — LEGITIMATE (the guide is genuinely external and is the one acknowledged repo). It stays; it is listed so the sweep records it as kept, not missed.

Rulings

* [A] Fix the stale mechanism/sibling claims in the user path (author instruction, 2026-07-29: "write the prompt to fix that").
* [C] `:bring` becomes just the seed (e.g. "A seed."); the two stale comment lines are deleted, not reworded — the command needs no caveat since there is nothing to set up. If you judge a one-line replacement clearer than deletion, it must not mention Polylith, repo history, subprocesses, or any repo but `ehr-testing-guide` (R34).
* [C] The strip's equation renames `sim-subprocess` → `sim-engine` (matching `docs/dev/source-sink-design.md` Part VIII's `EngineExecute` framing, where the engine is the catalytic resource; "subprocess" asserts a mechanism that no longer exists).
* [C] `simulate-your-facility.md`: both "tools sibling" phrasings become in-tool references — the damage/mutation capability is `ehrt corpus mutate` (link `use-cases.md` or `operators.md`); the MLLP/file-reader guidance points at the existing stdin/framing material where apt. Drop the self-link to this repo's GitHub URL. Keep the substance (what's out of scope, where damage is injected); only the two-project framing goes.
* [C] Historical surfaces untouched: `docs/dev/`, `notes/`, ADRs, archived prompts, and session records keep every ADR-0013/sibling mention exactly as written (ADR-0009's historical-prose discipline).

Read first
`notes/ADRs.md` ADR-0005 + ADR-0010 (and ADR-0009's historical-prose section); `components/tools/src/ehrt/tools/sim.clj` ns docstring; `components/tools/docs/use-cases.edn` strip `:simulator-traffic-as-intake-source`; `docs/simulate-your-facility.md`; `AGENTS.md`; `Makefile` (docsgen/use-cases targets).

Steps

1. Re-run the inventory (evidence over this prompt): `grep -rn "ehr-testing-sim\|sibling\|ADR-0013\|subprocess" docs/ components/tools/docs/*.edn`, excluding `docs/dev/`. Compare against the three findings above; any hit this prompt didn't name gets a disposition (fix / historical-stays / legitimate-stays) in the deviation record. `docs/use-cases.md`'s own hits are the generated shadow of finding 1 — fixed by regeneration, never by edit. Note: `use-cases.edn` ~line 93 and the `:note` near ~282 say "subprocess" about the FHIR validator — TRUE claims about `gate fhir`'s mechanism, not sim; they stay.
2. Edit `components/tools/docs/use-cases.edn` per the rulings.
3. `make docsgen` (or the narrower use-cases target). Verify `git diff` touches only `use-cases.edn` and generated output, and the generated diff is exactly the strips your source edit governs. Unexpected generated churn: stop, record, don't hand-revert.
4. Edit `docs/simulate-your-facility.md` per the ruling (hand-authored — direct edit is correct here).
5. Cold-reader link check on both changed docs: every `[text](target)` resolves on disk (ADR-0010's own verification style — script the walk, don't eyeball it).
6. Gate: `make ci-parity` if feasible, else `clojure -M:poly check` plus the docsgen test namespace(s). Hooks gate the push regardless.
7. Commit and push (one commit unless step 1 grew the scope): `docs: user path drops retired sim sibling/subprocess claims (ADR-0005; R34 sweep)`

Decision procedures

* A grep hit whose register (user-path voice vs historical record) is ambiguous: leave it, record it.
* A generated file differing beyond your source edit's reach: stop that step, record, continue the rest.
* Anything suggesting the in-process claim is itself wrong (code contradicting ADR-0005): STOP the fix entirely and report — evidence over ruling.

Close phase
Self-archive this prompt to `notes/prompts/` at the START of the close phase. Session record per `AGENTS.md`. Deviation record REQUIRED: empty is valid ("none"); absent is not.

## Deviation record (dated, self-archived per this workspace's own convention)

Applied as given, no author present to ask (autonomous session, R30) — one clarification and zero true deviations from the ruled scope:

**Step 1's re-run inventory matched the prompt's own three findings exactly, no new hit.** `grep -rn "ehr-testing-sim\|sibling\|ADR-0013\|subprocess" docs/ components/tools/docs/*.edn`, excluding `docs/dev/`, both before and after the edits, turned up nothing beyond: the three named findings (fixed); the FHIR-validator subprocess mentions the prompt already named as TRUE and out of scope (`docs/cli.md`, `docs/formats.md`, `docs/use-cases.md`/`.edn`'s own validator lines, `docs/glossary.md:225`); `docs/glossary.md:560`'s `ehr-testing-guide` sibling citation (kept, as ruled); and `docs/glossary.md:100`, a citation of `ADR-0013/ADR-0015` naming the *legacy-floor/full-capability baseline* register — a register pointer, not a sibling/subprocess mechanism claim, and not naming "sibling" or "subprocess" itself (matched only because the regex caught the ADR number). Left as-is; recorded here per the decision procedure for an ambiguous-register hit, though this one reads unambiguous on inspection.

**Chose the deletion form for the two stale `:commands :lines` comments**, not a one-line replacement — the ruling's own preferred default, since the command genuinely needs no caveat now.

**`simulate-your-facility.md`'s MLLP-framing pointer** links `docs/use-cases.md`'s "Mutate's own output, piped straight into intake" strip (the one place in the user path demonstrating `?framing=mllp` end to end), anchor computed by the same GFM slug rule the doc's own existing anchors use (`#generate-controlled-fault-data` confirmed as a working precedent) and verified against the actual generated heading text, not assumed. The damage/mutation pointer links `operators.md` (the mutation-operator catalog `ehrt corpus mutate` applies) rather than `use-cases.md`, since operators.md is the more direct answer to "where is damage injected."

**`make ci-parity` was started, then aborted deliberately.** It clones from `.` (i.e. `HEAD`), which at that point in the session was the last *committed* state — running it before this session's own commit would have validated stale code, not the actual edits. Ran `clojure -M:poly check` (green) and the full `clojure -M:poly test :all skip:integration` per-push lane instead, against the real working tree, per the ruling's own "if not feasible" branch — feasibility here turns on commit-order, not on the check being unavailable.

Everything else executed as specified.
