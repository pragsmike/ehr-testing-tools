# Charter — `cli` (base)

> **Draft for the author's edit.** Derived from
> `bases/cli/src/ehrt/cli/{core,help,retired}.clj`, their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

**A base has no `interface.clj`**, so §2 documents the *operator*
contract — the command surface — rather than a var list. That is the
shape the completeness gate expects for a base.

## 1. Mission

Be the `ehrt` entry point: parse a command line, call **one**
capability function, print its result, and map that result to an exit
code — **and nothing else.**

ADR-0004. `ehrt.cli.core` is **the only namespace in the workspace
that prints.**

## 2. The operator contract

### Command groups

Nine groups, defined as data in `help.clj`'s `cli-spec`:

`artifact` · `corpus` · `gate` · `check` · `version` · `doctor` ·
`sim` · `show` · `play`

Each group carries its own `:doc`, a witnessed `:example`, and its
verbs; each verb carries its `:flags`. **`cli-spec`'s `:flags` IS the
flag whitelist** — a flag not declared there is not accepted.

### Output contract

- **EDN is canonical output. `--json` is a projection, never the
  source of truth.**
- **TTY-sensitive default** (ADR-0013): a real terminal gets a human
  summary via `render-pretty`; a pipe or redirect gets the unchanged
  EDN envelope.
- **Two deliberate exceptions**, both of which `main!` prints
  verbatim rather than passing through `render`/`render-pretty`:
  - **(DOC-1)** `ehrt help`, `ehrt help <group>`, and `--help`
    anywhere print plain human-readable usage text — they are for a
    human or an AI assistant at a shell, not a pipeline, so the
    EDN-out convention does not serve them. Marked
    `:category :cli-help`.
  - **(ADR-0013)** `ehrt show` always renders for eyes. Marked
    `:category :display-text`.

### Exit codes

Declared as data in `cli-spec`'s `:exit-codes`, and derived from the
result's status — the `kernel` `:ok`/`:rejected`/`:error` distinction
reaching the shell, so a `:rejected` (the check ran, the answer is no)
is distinguishable from an `:error` (it could not run).

### Entry point

`bin/ehrt` is the taught, cwd-safe invocation. `clojure -M:cli` is a
**dead alias** and is forbidden on every live doc surface by
`invocation-lint-test`; bare `clojure -M:ehrt` is a documented
alternate, untouched by that gate.

## 3. Data shapes owned

- **`cli-spec`** — the whole command surface as data: groups, verbs,
  flags, defaults, examples, exit codes, and the top-level doc. This
  is the base's real artifact, and it is what `write-cli-md!` renders
  into the CLI reference.
- The **result → exit code** mapping.
- The **retired-command register** (`retired.clj`) — commands that
  once existed and what they became.

## 4. Invariants guaranteed

- **A thin shell.** Parse, call, print, exit. No domain logic lives
  here; a command that needs work done calls a component's capability.
- **One printing namespace.** Everything else returns values.
- **EDN is the source of truth**, `--json` a projection.
- **The spec is the whitelist.** Flags, verbs and exit codes are data,
  which is what lets the CLI reference be *generated* from the same
  definition the parser uses rather than written alongside it.
- **A retired command does not silently vanish** — `retired.clj`
  keeps the register.
- **Examples are witnessed.** A group's `:example` is cited to a real
  fence in a real document (R3-B3-1, ADR-0118), not composed for the
  help text.

## 5. Non-goals

- **Implements no capability.** Every verb delegates.
- **Does not reach the simulator directly.** There is **no
  `ehrt.sim.interface` require in this base** — `ehrt sim run`,
  `check`, `identifiers` and `version` are mounted **through
  `corpus`'s sim adapter** (`sim-run!`, `sim-check!`,
  `sim-identifiers!`, `sim-version!`), in-process since ADR-0005. See
  UNCLEAR-CLI1.
- **Is not the docs generator.** It *calls* `docs-tooling`'s
  `write-cli-md!`; the relay in the other direction was ruled out for
  circularity (ADR-0016).
- **Not a library.** Nothing may depend on a base.

## 6. Forbidden edges

Requires, in `src`: `corpus`, `corpus-io`, `docs-tooling`, `judge`,
`judge-fhir-official`, `judge-v2-hapi`, `judge-v2-nist`, `kernel`,
`sim-emit-hl7`.

Since the `tools` façade retired at stage 3 (ADR-0018), **every alias
names the owning interface directly** — `kernel`'s
result/artifact/locator vocabulary, `judge`'s report vocabulary, and
the three gate engines were relay re-exports in `ehrt.tools.interface`
and are now reached at source.

Structural rules:

- **Nothing may require this base.** Bases are the top of the
  dependency order. `docs-tooling` in particular must not, and
  ADR-0016 records that finding.
- **No base-to-base edge** — `cli` is the only base today.
- It must not reach any component's **implementation** namespace, only
  its `interface`.

## UNCLEAR — the author's review queue

- **UNCLEAR-CLI1 — the simulator is reached through the corpus
  domain.** `ehrt sim …` is arguably the project's headline command
  group, and this base does not require `ehrt.sim.interface` at all —
  its only mentions in `core.clj` and `help.clj` are **docstring
  prose** (`core.clj:2375`, `:2381`, `:2437`, `:2589`, `:2605`;
  `help.clj:206`). The live path is `corpus`'s `sim-adapter`. Two
  readings: *(a)* deliberate and load-bearing — the adapter is where
  in-process mounting, option translation and the P3-6 parity mounts
  live, so one seam serves both `ehrt sim` and corpus-side callers;
  *(b)* residue — the adapter predates the sim split and the CLI could
  now call `sim` directly, leaving `corpus` out of a path it has no
  domain interest in. Note what makes this more than cosmetic: it is
  why `corpus`'s forbidden-edge list **cannot** include `sim`, and why
  `provenance` had to exist at all.
- **UNCLEAR-CLI2 — `check` and `gate` are separate groups.** Both
  names suggest validation, and `sim check`, top-level `check`, and
  `gate` are three different things reachable by similar words. What
  distinguishes them is not stated in a place a charter reader would
  find; `cli-spec`'s per-group `:doc` is the only answer, and it is
  one line each.
