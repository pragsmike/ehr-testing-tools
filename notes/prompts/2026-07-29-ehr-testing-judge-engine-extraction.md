2026-07-29 — Extract per-engine judge components: `judge-v2-hapi`, `judge-fhir-official`; `judge` keeps the verdict vocabulary

Repo: `github.com/pragsmike/ehr-testing-tools`, main at `c563793` (or later — probe first). Autonomous session per R30: commit AND push at each checkpoint; hooks gate (WSL provenance, gitleaks, `poly check`); tags and repo-level `gh` mutations are the author's alone. Ask nothing mid-session — decision procedures below; skip-and-report anything they don't cover.

Context

`components/judge` today holds two engines and the verdict vocabulary in one brick: `ehrt.judge.v2` (HAPI hl7v2, in-process, base-structural tier), `ehrt.judge.fhir` (official HL7 FHIR validator CLI, pinned artifact, subprocess), and the vocabulary/machinery trio `finding`/`report`/ `verdict-cache`. A second v2 engine (NIST `v2-validation` via CDC's `lib-hl7v2-nist-validator` wrapper, profile-aware tier) is on the board (EXP-D3, its own session) — per-engine components are the seam it lands into. Channel-verified 2026-07-29 (re-verify, don't trust): `finding`/`report`/ `verdict-cache` require kernel only; each engine requires the vocabulary and kernel, never another engine — the split is structurally clean.

Prior art: ADR-0008 is the extraction playbook (census the real callers, size interfaces by grep, wire `poly/X` at project level, qualify collisions, verify by full suite). Follow it.

Rulings

* [A] Extract per-engine judge components now (author, 2026-07-29: "Let's extract the validators now"). Naming pattern is engine-qualified: "I like judge-v2-hapi etc."
* [C] Applying the pattern: `components/judge-v2-hapi` (from `ehrt.judge.v2`), `components/judge-fhir-official` (from `ehrt.judge.fhir`). `components/judge` keeps its name and the vocabulary (`finding`/`report`/`verdict-cache`). `judge-v2-nist` is NOT created this session — it lands after EXP-D3, into the seam this session builds.
* [C] `verdict-cache` stays in `judge`, not `judge-fhir-official`, despite having (verify by census) only one consumer today: it keys on engine-name/version + input hash generically, and the planned NIST engine is its expected second consumer. Disclose the single-consumer fact in the ADR; author may veto post-hoc.
* [C] Namespaces: `ehrt.judge.interface` keeps the vocabulary exports (`Report`, `build-report`, `diff-reports`, `baseline-relative-report`, `report-valid?`, `finding-valid?`); each engine component gets its own `ehrt.judge-v2-hapi.interface` / `ehrt.judge-fhir-official.interface` exporting its gate functions (sized by census — the current `v2-gate-file`/`v2-gate-dir`/`fhir-gate-file`/`fhir-gate-dir`/ `fhir-gate-batch` names may simplify to unqualified `gate-file`/`gate-dir` now that each lives in its own component and the cross-engine collision ADR-0008 qualified them for no longer exists at one interface — your call by the collision facts, record it either way). Implementation namespace names within each component are the session's own mechanical choice.
* [C] ZERO behavior change. `bin/ehrt gate v2` / `gate fhir` / `check` output byte-identical before and after (characterization step below). `Finding`'s `:engine` name strings (`"hapi-hl7v2"`, the fhir engine's) are data, not code organization — unchanged.
* [C] Dependency disposition by census, ADR-0008's method: HAPI v2 Maven coordinates (`hapi-base`, `hapi-structures-v24`) move to `judge-v2-hapi/deps.edn`. The `hapi-fhir-base`/`hapi-fhir-structures-r4` pair — which ADR-0008 already disclosed as having NO live `:import` anywhere — moves to `judge-fhir-official/deps.edn` with the same disclosure carried forward. Do NOT drop them; dropping is an author call ("superseded requires a load-bearing inventory" — cite the ADR-0008 disclosure and name the drop as an open author decision in the ADR).

Out of scope, deliberately

Site-config (the per-site conformance-contract concept), profile selection flags (`--engine`, `--profile`), the NIST engine itself, any `gate` CLI surface change, any sim-side change. These are the next wave, gated on EXP-D3 evidence and author rulings already queued in the design channel.

Read first

`notes/ADRs.md` ADR-0008 (the playbook) and ADR-0002 (interface-sizing method); `components/judge/src/ehrt/judge/*.clj` (all six files); `components/judge/deps.edn`; `ehrt.tools.interface` (what it re-exports judge-wise) and `ehrt.tools.check` (the report-aggregation consumer); `bases/cli/src/ehrt/cli/core.clj` (gate dispatch); every project `deps.edn` plus root `deps.edn` (`:dev`/`:test`/`:ehrt`); `AGENTS.md`.

Steps

1. Probe. Fresh state, current head, `clojure -M:poly check` green before touching anything.
2. Census. Every real `:require`/`:import` of `ehrt.judge.*` across the whole tree (src + test + projects). Build the disposition table (namespace → callers → destination component) exactly as ADR-0008's census table does. If the census contradicts a ruling above (e.g. an engine namespace requiring the other engine, or `verdict-cache` consumed by v2), STOP that disposition and record — evidence over ruling.
3. Characterize. Before any move: capture `bin/ehrt gate v2` and `bin/ehrt gate fhir --report` output (and `check`) on a small fixture set to files; these are this session's own byte baselines. (Note ADR-0009's disclosure: no committed baseline exists — you are creating this session's own, not regenerating one.) Commit checkpoint: `docs: judge extraction census and characterization baseline`
4. Extract. `git mv`-based moves into `components/judge-v2-hapi` and `components/judge-fhir-official`; interfaces sized per census; deps moved per the ruling; `poly/judge-v2-hapi` + `poly/judge-fhir-official` added everywhere `poly/judge` appears (root `:dev`/`:test`/`:ehrt`, `projects/ehrt-cli`, `projects/conformance`, `projects/integration`). Update `ehrt.tools.interface`'s re-exports and every consumer's requires. Test namespaces move with their engines; vocabulary tests stay in `judge`. Watch for the ADR-0008 class of traps: a test pinning material from two components at once (keep it in the downstream component), collisions revealed only at namespace load (fix by qualification, record).
5. Verify. `clojure -M:poly check` green; `poly deps` shows `judge-v2-hapi → {judge, kernel}`, `judge-fhir-official → {judge, kernel}`, `judge → kernel` only, no engine→engine arrow, tools → both engines + judge; full `clojure -M:poly test :all skip:integration` — capture the complete untruncated log and the run's own exit code directly (no pipe in the middle — see the 2026-07-29 errata session record's `tail` lesson). Re-run the step-3 characterization commands; diff against the baselines: byte-identical.
6. ADR. Author `notes/ADRs.md` ADR-0011: the census table, interface sizing, the verdict-cache placement disclosure, the hapi-fhir-pair disclosure + open drop decision, the collision/simplification record, and the motivation (per-engine seam ahead of NIST adoption; the translator-under-test use case, author-described 2026-07-29). Commit: `feat: extract judge-v2-hapi and judge-fhir-official; judge keeps the verdict vocabulary (ADR-0011)`
7. Docs sweep, narrow. `docs/dev/architecture.md`'s bricks table/diagram and `AGENTS.md`'s "Landed so far" gain the two components (dev path — Polylith vocabulary is fine there). The user path (`docs/`) should need nothing: verify by grep that no user-path doc names `components/judge` internals; if one does, record it — do not silently fix (ADR-0010 declares doc rows before writing). Commit: `docs: architecture and AGENTS reflect the judge engine split (ADR-0011)`

Decision procedures

* Census contradicts a [C] ruling → follow the census, record the deviation, flag for author veto in the ADR.
* A test fails after the move for reasons unrelated to the move (flake, environment) → re-run once; if it persists, STOP and report — do not patch test content to green.
* Anything requiring a `gate` CLI flag or output change to complete the refactor → STOP; that contradicts the zero-behavior-change ruling.

Close phase

Self-archive this prompt to `notes/prompts/` at the START of the close phase. Session record per `AGENTS.md`. Deviation record — a section literally so titled — REQUIRED: empty is valid ("none"); absent is not.
