# 2026-08-05 — Alignment fixes 3

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `2599355` at session start (`notes/adr/0051-alignment-fixes-2.md`,
alignment fixes 2, design-channel-verified). Session record:
[`2026-08-05-alignment-fixes-3.md`](../session-records/2026-08-05-alignment-fixes-3.md).
Decision-of-record: `notes/adr/0052-alignment-fixes-3.md`.

## Prompt, verbatim

2026-08-05 — alignment fixes 3: sim-model's resources take their own name, and the nesting rule gets its gate

Session prompt (design channel, 2026-08-05). Prior: alignment fixes 2 landed and was design-channel-verified (`2599355`); the tag mechanic is now coherent across all surfaces. This session executes the ruled S1 fix (register row S1/C-1): `components/sim-model/resources/sim/` renames to `resources/sim-model/`, with the resource-nesting gate co-landed in the same commit. First src-touching session of the arc — the oracle bracket is the whole point of the ceremony here. Small session; the fences are tight. R30 ceremony. Read-first: register rows S1 and C-1; `notes/adr/0025-*.md`'s disclosed tolerance (resolve filename via index); `components/sim_model/src/ehrt/sim_model/persona.clj` lines ~50–70; the three fixes-2 gates as style models.
Design-channel probe results this prompt builds on (re-verify in Step 0, don't trust): exactly THREE `io/resource` call sites resolve `"sim/demographics/..."` paths, all in `persona.clj` (lines ~57/62/67), none elsewhere workspace-wide; the moving set is four files (`given-names.edn`, `surnames.edn`, `places.edn`, `NOTICE`); all seven `components/*/resources/` dirs conform to the nesting rule EXCEPT sim-model.
Author rulings (record verbatim in ADR-0052)

1. AR-F3-0 (tag). Per the reconciled mechanic (AGENTS.md + AR-AU-0), create annotated tag `stable-20260805-alignment-fixes-2` at `2599355`, message `alignment fixes 2 landed, design-channel-verified 2026-08-05 (ADR-0051)`; push; verify on origin.
2. AR-F3-1 (the rename). `git mv components/sim-model/resources/sim components/sim-model/resources/sim-model` (history-preserving), plus exactly the three path-string edits in `persona.clj` (`"sim/demographics/..."` → `"sim-model/demographics/..."`). Resource CONTENT is untouched — byte-identical files at new paths; verify with `git diff --stat -M` showing pure renames for the four files. ADR-0025's tolerance closes with this session; ADR-0052 cites it as the origin and the register rows as the trigger.
3. AR-F3-2 (the gate, co-landed). New deftest in the `docs-tooling` gate family: every `components/*/resources` directory that exists contains exactly one top-level entry, a directory named after its brick. No allowlist — post-rename, all seven conform; if Step 0's probe finds an eighth nonconforming dir this prompt doesn't know about, STOP-AND-ESCALATE rather than allowlist it. Docstring cites AR-F3-2, register rows S1/C-1, and ADR-0025's closed tolerance. Red→green is NATURAL here: run the gate against the pre-rename tree first, capture the genuine red (sim-model trips), then land rename + gate in the SAME commit, green.
4. AR-F3-3 (citation sweep, same commit). Fresh grep for `resources/sim/demographics` and `sim/demographics` as TEXT across docs (`AGENTS.md`, `AUTHORS-GUIDE.md`, `components/*/docs/`, component READMEs, `.agents/` live surfaces): any current-tense citation of the old path updates; explicitly historical mentions keep per AR-F1-2's judgment rule, disposition table in ADR-0052. Expected small or empty — the paths were internal.
5. AR-F3-4 (determinism bracket is the verdict). Oracle bracket (`2599355` → tip) must show all ELEVEN batches identical — the demographics EDN content is unchanged, so persona generation and every downstream byte must be too. ANY digest change means the rename touched something this analysis missed: STOP-AND-ESCALATE with the diff, revert nothing, land nothing further. No `--declared-digest-change` is licensed.

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `2599355` or later-with-disclosure; re-verify the probe results above (call-site count, moving set, seven-dir conformance census); full suite green baseline; oracle pre-digest. Execute AR-F3-0.
Step 1 — Red, then rename + gate + sweep (AR-F3-1/2/3). Write the gate; run against the unrenamed tree; capture the red transcript. Execute the rename + the three path edits + any AR-F3-3 citation updates. Gate green; full suite green; `poly check` OK. ONE commit: `refactor: sim-model's resources take their own name — the last nesting drift closes, gated (alignment fixes 3, AR-F3-1/2/3)`
Step 2 — ADR-0052 + record. ADR-0052: rulings verbatim; the red transcript; the rename's `--stat -M` evidence; the sweep disposition table; ADR-0025 tolerance closure; assertion-count delta. Index line; Done pointer `- 2026-08-05 — alignment-fixes-3 — ADR-0052`. Oracle bracket per AR-F3-4 — the session's verdict. Session record + prompt self-archive. Final commit: `docs: alignment fixes 3 record — a small rename, fully bracketed (ADR-0052)`
Fences
The ONLY src edit is `persona.clj`'s three path strings. The only resource change is the `git mv` — zero content bytes differ. No other gates touched; no gate weakening. No deps.edn/workspace.edn edits. Frozen archives untouched (ADR-0052 + index sanctioned; ADR-0025 is CITED as closed, not edited — its closure lives in ADR-0052). Deferred clusters untouched (NIST mirroring is session 4; LICENSE work session 5 — the NOTICE file MOVES here but its content and coverage questions stay F-4's). If the Step 0 census contradicts the probe results in any particular, STOP-AND-REPORT before the rename — the design channel's analysis is an input, not a license to proceed against contrary evidence.
After landing: design channel verifies by fresh probe; session 4 (NIST jar mirroring) follows, and this landing's tag rides its Step 0.

## Notable deviations, disclosed

- **The prompt's own file-path shorthand
  (`components/sim_model/src/ehrt/sim_model/persona.clj`, underscore
  form) didn't resolve** — the real path uses a dash for the component
  directory (`components/sim-model/src/...`), underscore only for the
  Clojure-convention source subpath (`ehrt/sim_model/`). Resolved
  immediately by `find`; no premise mismatch in substance, just the
  shorthand's own typo — not worth a STOP given the rest of the probe
  (call-site count, moving set, seven-dir census) matched exactly once
  the real path was used.
- No other deviation: every probe result re-verified in Step 0 matched
  the prompt's own claims exactly (three call sites, four-file moving
  set, seven-dir census with sim-model the sole nonconformer); the
  citation sweep came back with exactly the two historical hits
  expected ("small or empty"); the oracle bracket showed all eleven
  batches identical, the session's own stated verdict condition.
