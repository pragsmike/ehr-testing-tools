# DOC-1 — CLI help surface: `ehr help`, operator listing, honest errors

You are working in `ehr-testing-tools` (public). This session executes DOC-1 of `.agents/plans/user-docs.md`: the `ehr` CLI gains a help surface (`ehr help`, `ehr help <group>`, `--help`), an operator-listing verb, and a bounded error-message improvement — the first wave of the user-documentation plan, done as code because help text at the shell serves the task-first practitioner and her AI assistant at once, and because every later reference doc gets shorter once `ehr help` is authoritative. This wave adds surface; it changes no existing behavior. The existing verbs' outputs, flags, and exit codes are a contract (ADR-0004, extended by ADR-0010): if any existing test needs editing to stay green, the contract moved — stop and report rather than edit the test.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `.agents/plans/user-docs.md` (the DOC-1 section — this session's scope fence), `src/ehr_testing_tools/cli.clj` (the whole file: `parse`, `dispatch`, `result->exit-code`, `main!`'s injectable boundaries — your test seam), `src/ehr_testing_tools/corpus/operators.clj` (the registry the listing verb reads; its docstring's dropped-candidates paragraph), `src/ehr_testing_tools/result.clj` (error/rejected constructors — the error pass extends payloads, not shapes), `notes/ADRs.md` ADR-0004 (CLI contract) and ADR-0010 (exit code 3), `README.md`'s Quickstart (the flag surface as currently documented — you will verify it against source, not trust it), `test/ehr_testing_tools/cli_test.clj` (how dispatch/main! are tested today). Ritual: commit → `git push origin`. Save this prompt to `.agents/prompts/2026-07-25-doc1-cli-help.md`; final commit archives it to `.agents/prompts/archive/`.

Author rulings in effect: **Help is plain text, not EDN** — `ehr help*` and `--help` print human-readable usage and exit 0; they are for humans and agents at a shell, not pipelines, and are the one deliberate exception to the EDN-out convention (document the exception in `cli.clj`'s ns docstring). **Bare `ehr` stays an error** — it prints the top-level usage text but keeps exit code 2; an incomplete invocation is operationally an error and agents keying on exit codes need that honesty. **Help is data-first** — one spec structure (per group: verbs; per verb: flags with one-line docs, positional-arg shape, a shared exit-code table 0/1/2/3) rendered to text — the spec does not drive parsing (parsing stays exactly as-is this session), but a test asserts the spec's coverage so the two can't silently diverge. **The listing verb is `ehr corpus operators`** (author to ratify or substitute before pasting) — a normal EDN result honoring `--json`, reading the registry only; dropped candidates are docstring prose, not registry data, so the verb does not list them — its help text points at `docs/judge-calibration.md` instead. **The error pass is bounded to the enumerable-options family** — unknown command/action and `:invalid-operator` gain payloads naming the valid options plus a help pointer; no other error site changes. **No new docs files** — DOC-3 owns `docs/cli.md`; this session touches README only where Step 4 says.

## Step 0 — Inventory the real surface (evidence over memory)

From source, not README: enumerate every command group, verb, positional-arg convention (the gate/check positional-path special cases in `dispatch`'s `cond`), and every flag each command function actually reads (`fetch-command` through `check-command`, plus `generate/generate!`'s opts). Enumerate every `result/error` / `result/rejected` call site reachable from the CLI and classify which belong to the enumerable-options family. Record both inventories in the commit message body (they are this session's ground truth and Step 1's input). If the README quickstart documents a flag the source does not read, or vice versa, note the discrepancy in the inventory and report it at close-out — do not fix README prose beyond Step 4's scope.

Commit: `DOC-1: inventory CLI surface and error sites (evidence for help spec)`.

## Step 1 — The help spec and its renderer

A `cli-spec` data structure in `cli.clj` (or a sibling `cli.help` namespace if `cli.clj` would grow past taste — implementer's discretion, dependency direction unchanged) covering: every group/verb from Step 0's inventory, each flag with a one-line doc and default where one exists, positional conventions stated (`gate fhir|v2 PATH`, `check DIR`), and the exit-code table (0 ran-and- passed / 1 ran-and-rejected / 2 operational error / 3 no-verdict aggregate under default policy, per `result->exit-code`'s own docstring — cite ADR-0004/0010 in the spec, don't restate the reasoning). A pure renderer: spec → top-level usage string, spec + group → group usage string. Unit tests: rendering is exercised through pure functions; a coverage test walks `dispatch`'s group/verb `case` branches (they are enumerable in source) against the spec so a future verb added without spec coverage fails a test.

Commit: `DOC-1: help spec (data-first) + renderer, coverage-tested against dispatch`.

## Step 2 — Wire help into dispatch and main!

`ehr help` and bare `ehr` → top-level usage; `ehr help <group>` and `ehr <group> --help` (and `--help` after any verb) → group usage. Help paths print plain text via the injectable `println-fn` and return exit 0 through the normal `main!` flow — no new exit mechanism; bare `ehr` prints the same top-level text but flows to exit 2 (ruling above). `--help` interception happens before command execution — a `--help`'d verb must not run its side effects (tests prove this with the injectable `-fn` seams: the command fn is not called). All existing tests green untouched — the tripwire from the preamble applies with full force here, since `dispatch`'s signature and the render/exit contract are the two things most tempting to "clean up." Don't.

Commit: `DOC-1: ehr help / --help wired; bare ehr prints usage, keeps exit 2`.

## Step 3 — `ehr corpus operators`

New verb reading `corpus/operators.clj`'s registry: for each operator, `:id`, `:format`, `:version`, `:locator-required?`, and the contract's `:type`/`:target`. Result is a normal `result/ok` EDN map (so `--json` works for free through `render`); sorted stably (format, then id) so output is diffable. Optional `--format fhir|v2` filter if it falls out naturally; skip it if it complicates parsing (parsing stays as-is — the ruling). Help spec gains the verb. Unit test asserts the ten current operators appear and that the verb is a pure registry read (no filesystem, no subprocess).

Commit: `DOC-1: ehr corpus operators — registry listing verb (EDN/--json)`.

## Step 4 — Honest errors, bounded; README pointer

Unknown group and unknown action payloads gain `:valid-options` (from the help spec — one source of truth) and `:hint "run: ehr help"`. `:invalid-operator`'s payload gains the valid IDs for the requested format (extend the payload map in `operators.clj`'s validation; result shape and category unchanged — callers matching on `:invalid-operator` are unaffected, and a test proves the category survived). No other error site changes, per the ruling. README's Quickstart gains exactly two lines: one introducing `make ehr ARGS="help"` at the top of the code block, one noting `ehr corpus operators` where the mutate example names its operator. `make help`'s `ehr` line gains a pointer to `ehr help`.

Commit: `DOC-1: enumerable-options errors name their options; README/make-help pointers`.

## Step 5 — Close out

Full suite + both lints green; golden check clean (`make pipeline && make use-cases && git diff --exit-code docs/pipeline.md docs/use-cases.md` — this session must not move a generated doc; a diff there is scope creep, stop). Run the new surface once for real: `make ehr ARGS="help"`, `make ehr ARGS="help gate"`, `make ehr ARGS="corpus operators"`, one deliberately wrong verb — confirm text, exit codes (0/0/0/2), and the options-bearing error payload by eye. Update `.agents/plans/user-docs.md`'s tracker: DOC-1 row → Done with an itemized summary and the prompt path; note any Step-0 README/source flag discrepancies in the row or as a report to the author. Archive this prompt.

Commit: `DOC-1 complete: CLI help surface landed (archives prompt)`.
