# UX arc — the CLI speaks to users, not to agents

A working brief for the design channel and the Code sessions it spawns.
Authored 2026-08-06 against tip `12d3aa3` (alignment arc closed, ADR-0055).
Origin: a real first-contact failure — the author ran a design-channel-supplied
command from the demo docs' invocation form and hit a three-failure cascade
(stale alias, opaque error, agent-voice help). Every seeded finding below
traces to that transcript or to residuals the alignment arc left pending.

## 1. Purpose

The tools work; the *experience of reaching them* doesn't. This arc makes the
user-facing surfaces — invocations, help text, error messages — speak to a
human operator who has never read an ADR, without losing one byte of the
maintainer-facing record. Pre-publication is the moment: these surfaces freeze
hardest of all after the first public tag, because they are what strangers see
first.

## 2. The incident, as evidence (2026-08-06 transcript)

1. Demo READMEs (`components/sim/docs/demos/*/README.md`) teach
   `clojure -M:cli run ...`. No `:cli` alias exists in the root `deps.edn`
   (probed); the canonical entry is `bin/ehrt` (which cd's to workspace root
   precisely so cwd never matters) or bare `clojure -M:ehrt`. The demos also
   omit the `sim` group and carry the `--emit hl7`/`--format er7` pairing
   inconsistently. The quickstart gate asserts README fence == script for the
   TOP-LEVEL README only; demo READMEs are ungated invocation surfaces.
2. The failure mode of the stale alias is maximally opaque: clojure warns
   about the undeclared alias, then treats the first argument (`run`) as a
   script file — `FileNotFoundException: run`. Not our error message, but our
   docs walked the user into it.
3. The design channel itself propagated the stale invocation without probing
   the alias — unearned specificity. Docs that teach commands are law-surfaces
   for users; the law-surface propagation lesson (rulings register, alignment
   arc) applies to them too.
4. The help spec (`bases/cli/src/ehrt/cli/help.clj`) is written in maintainer
   voice on a user-facing surface: ADR citations, milestone tags ("M5b"),
   internal law names ("binds-at-emit-time-only"), long unbroken prose lines.
5. The user's config file was written as `.md` but referenced as `.edn` — an
   ordinary slip the CLI could soften (name the missing path; optionally note
   a near-miss sibling) and the error surface may not currently name the path
   at all [audit — the --config open path's failure message was not verified
   by the design channel; the incident never reached it].

## 3. Principles for the arc (proposed law, author to ratify)

- **Two voices, two homes.** User-facing surfaces (--help output, error
  messages, README command fences, demo READMEs) speak operator language:
  what it does, how to run it, what went wrong, what to do next. Maintainer
  content (ADR citations, milestone history, internal law names) lives in
  docstrings, `docs/dev/`, and the generated maintainer docs — never deleted,
  only relocated. The help spec is data; the rewrite is editing that data
  once, and `write-cli-md!`/docsgen inherit it.
- **Errors name the artifact.** Every operational error (exit 2) names the
  concrete thing it couldn't find or parse — the file path, the flag, the
  module name — and where useful, one next step. `:module-not-found
  {:module ...}` is the in-repo precedent to generalize.
- **Invocation docs are gated like the README.** The quickstart pattern
  (fence == tested script) extends to demo READMEs, or demo fences are
  asserted against the help spec's own invocation grammar — mechanism, not
  vigilance, per the arc before this one.
- **The exit-code contract is already good** (0/1/2/3, `bin/ehrt`'s header)
  and is not touched — this arc changes what the surfaces SAY, never what
  they return.

## 4. Seeded findings

- **U1** — Demo README invocations broken/stale (alias, group, flag pairing);
  ungated. Fix: sweep to `bin/ehrt` form + gate.
- **U2** — Help-spec voice and formatting: agent-speak out of user surface
  (relocated, not deleted); line discipline; per-verb examples; a tripwire-style
  gate candidate (no `ADR-` tokens in user-facing help strings).
- **U3** — Error-surface survey: every exit-2 path in `bases/cli` and
  `ehrt.sim.run` checked for names-the-artifact compliance; `--config` open
  path first [audit].
- **U4** — Near-miss suggestion on config-not-found (sibling file with same
  stem, different extension) — nice-to-have; audit decides if it lands or
  stays a row.
- **U5** — Invocation prominence: `bin/ehrt` is the taught entry everywhere;
  the README already does this — the demos and any other command-bearing doc
  must match.

## 5. Residuals folded in (pre-ruled, ride the opening session)

- **R1** — Two pending tags: `stable-20260805-alignment-fixes-5` at `2b3bb2b`
  (licensed, commands in ADR-0055) and `stable-20260805-alignment-close` at
  `12d3aa3` (licensed by the design channel's close verification).
- **R2** — The three compaction-arc Done pointers (ADR-0045/46/47) rotate to
  the attic under a dated header, closing the disclosed leftover from the
  alignment close (ADR-0055's scope-precision note).

## 6. Session shape

0. **Opening rider session** (small, docs+tags): land this brief + index
   entry; execute R1 (both tags) and R2 (rotation); ADR-0056.
1. **UX audit session**: survey every user-facing string surface (help spec,
   all exit-2 error paths, README + demo READMEs + any doc bearing a command
   fence); findings register, register-only, same row format and fences as
   the alignment audit (`.agents/plans/2026-08-05-alignment-audit-findings.md`
   is the pattern).
2. **Design-channel ruling pass** on the register; the voice-rewrite of the
   help spec likely wants a design-channel draft before its fix session
   (wording is the work).
3. **Fix sessions** per ruled cluster, gates co-landed; then arc close per
   the now-standing pattern (rulings appends, state regeneration, budgets,
   rotation, tags).

## 7. Fences (standing law applies unchanged)

Move-don't-improve; findings-only audits; oracle identity for every session
that doesn't change emitted bytes (none here should — help text and error
messages are not wire output; the oracle proves it); frozen archives;
annotate-not-rewrite; the register read-only once landed; evidence outranks
every voice in this brief.
