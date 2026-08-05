<!-- Attic file: notes/adr/0005-the-ehr-sim-mount.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0005 — The `ehr sim` mount: `notes/tools/ADRs.md` ADR-0012 fulfilled, `notes/tools/ADRs.md` ADR-0013 decision 1 retired

**Status:** Accepted (author-directed, amending the session's own R20
in-session), 2026-07-28.

### Context

ADR-0004's own R19 restructuring left five `sim_*_test.clj` suites (plus
`smoke_test.clj`'s sim half) in `projects/conformance`, still resolving
sim through a sibling-checkout discovery order (`ehrt.tools.sim`'s
`:sim-dir` → `EHR_TESTING_SIM_DIR` → `../ehr-testing-sim`, `notes/tools/ADRs.md` ADR-0013)
that CI's cold clone never satisfies — they skip cleanly there rather
than fail, so they weren't part of ADR-0004's own red-CI fix, but they
also never ran for real anywhere CI could see.

This session's own initial proposal (documented in chat, not
committed) was a minimal repointing: keep the subprocess, retarget its
discovery default from the sibling checkout to `projects/sim` in this
same workspace, via a new `bin/sim` launcher mirroring `bin/ehr`'s own
shape. The author's own amended ruling rejected the subprocess
entirely: *"I'm ok with not using the subprocess at all, and simply
mounting sim's CLI tree as a subcommand of tools ehr... this was the
original design, and the subprocess technique was a way to get around
the fact that sim grew in a separate project that wasn't available to
all agents at the time."*

That recollection undersold its own case. `notes/tools/ADRs.md`
ADR-0013's own decision 1 gives the real, stronger reason: *"sim is a
private repo today and this repo is public (ADR-0008); a git or Maven
dependency from a public repo onto a private one breaks public CI
outright... and even once sim is public, a classpath dependency would
invert ADR-0012's own stated arrow... and tangle the two repos' version
lockstep."* [The quoted "ADR-0012" is `notes/tools/ADRs.md` ADR-0012 —
qualified fix-forward, 2026-07-30, not the workspace's own ADR-0012
below.] And `notes/tools/ADRs.md` ADR-0013 decision 5, verbatim: *"The
`ehr sim` mount remains DEFERRED (ADR-0012, unchanged)."* `notes/tools/ADRs.md`
ADR-0012 itself is the mount's own pre-existing design — five CLI
interface properties, verified against source, that a mount would rest
on, explicitly left unbuilt pending "the classpath question" resolving.
That question resolved the moment sim and tools became bricks in one
workspace (H2, this same week) — this record is that resolution,
exercised, not a new design.

### Decision

**Mount, in-process, via `ehrt.sim.interface` directly.**
`components/tools/src/ehrt/tools/sim.clj` (the adapter both
`ehrt.tools.corpus.generators`' `:sim` entry and the test harness
already drove) now calls `ehrt.sim.interface/run-command` directly —
no subprocess, no discovery order, no availability check. `bases/ehr-cli`
gains an `ehr sim run` group, dispatching through the same
`ehrt.tools.interface/sim-run!` re-export every other consumer already
used. `poly/sim` is a real `:local/root` dependency now, everywhere
`components/tools`' own compiled code loads (`projects/tools-cli`,
`projects/conformance`, `projects/integration` — transitively, since
`poly/tools` itself now requires `ehrt.sim.interface`; root `deps.edn`'s
own `:ehr`/`:dev` aliases).

**`notes/tools/ADRs.md` ADR-0013's direction invariant preserved, poly-enforced.** The rule
was never "sim and tools must never share a classpath" — AGENTS.md's
own constraints section already permitted `components/tools`/
`projects/conformance` depending on `components/sim`, the arrow
tools → sim, one-directional. `ehrt.tools.sim` requiring
`ehrt.sim.interface`, never the reverse, is exactly that arrow;
`poly check` enforces it structurally (a `sim` → `tools` require would
fail dependency-direction validation, not merely violate a convention).
What's retired is decision 1's *mechanism* (subprocess, because a
classpath dependency used to be structurally impossible across the
public/private, independently-versioned repo boundary) — the
motivating constraint, not the direction rule built on top of it.

**Sibling-discovery machinery removed as dead code, not left
permanently-true.** `available?`, `default-sim-repo-dir`,
`sim-dir-env-var`, and `sim-not-available` are gone from
`ehrt.tools.sim` and its `ehrt.tools.interface` re-export entirely —
not kept as a function that always returns `true`. `EHR_TESTING_SIM_DIR`
is no longer read anywhere in this workspace. `projects/conformance/test/ehrt/tools/sim_harness.clj`
(the project-local pass-through every `sim_*_test.clj` and `smoke_test.clj`
call through) lost its own `available?`/`absence-message` the same
way; its five consumers and `smoke_test.clj`'s sim half each lost their
own `(if-not (sim-harness/available?) (skip) (run-for-real))` wrapper,
now running unconditionally.

**One OS-level pipe test retained, deliberately, as the
consumer-fidelity witness.** Every in-process test proves the mount's
own *logic*; none of them prove `bin/ehr sim run --seed ... --patients ...`
— the actual invocation a human or script would type — still resolves,
parses its flags, and prints a real Result to stdout. `projects/conformance/test/ehrt/tools/sim_cli_real_invocation_test.clj`
is that proof, real `bin/ehr` subprocess and all, comment-marked as the
one deliberate exception to "everything else is in-process now" — same
real-subprocess discipline `mutate_stdout_stdin_loopback_test.clj` and
`stdin_intake_real_pipe_test.clj` already established, stdout and
stderr kept separate (a JVM/WSL warning on stderr must never corrupt
the EDN this test reads from stdout — caught once, by this test itself,
during this session; see the deviation record).

**Registered in help; docsgen deferred to its own owner.** `bases/ehr-cli/src/ehrt/ehr_cli/help.clj`'s
own `cli-spec` gained the `sim` group/`run` verb (flags matching
`run-command`'s own opts 1:1: `--seed`, `--patients`,
`--reference-date`, `--warm-up-seconds`, `--emit`, `--churn`,
`--config`); `help_test.clj`'s two hand-mirrored coverage structures
(`stub-key`, `known-dispatch-pairs`) updated to match, per `notes/tools/ADRs.md`
ADR-0012 property 4's own correction about what "mounting sim" does and doesn't
give for free. Regenerating `docs/cli.md` itself from this updated spec
is the pending closeout-sweep session's own step 4 (docsgen regen
tooling, a named row in this session's own carve-loss audit) — not
duplicated here; the spec data is ready for it.

**Sim stays out of any future published-library artifact (named,
not built).** `projects/tools-cli`'s own `poly/sim` dependency is for
the CLI mount only. ADR-0001 R3 already names `projects/tools-cli` as
this family's sole future publishable library, once H5 (Clojars/Maven
coordinates, still open) resolves; whatever that publishing mechanism
turns out to be must exclude sim's own code from the published
artifact's own coordinates when the time comes — recorded here as a
constraint on that future session, since this session is the one that
introduced the dependency it constrains.

**`bases/sim-cli` / `projects/sim` untouched, deliberately.** Sim's own
standalone CLI artifact and its composing project are exactly as this
session found them — confirmed via `git status` showing zero changes
under either path. Whether sim keeps a standalone CLI future
independent of the `ehr sim` mount is an explicitly deferred author
call, not decided by this record either way.

### `notes/tools/ADRs.md` ADR-0012's five properties, exercised

Each of the five interface commitments `notes/tools/ADRs.md` ADR-0012 recorded (before any
mount existed, so a later refactor couldn't silently break one without
noticing it was load-bearing) held, verified against source rather
than assumed: **(1)** `dispatch`'s own `[group action]`-in, Result-out
shape needed only a new `case` arm. **(2)** The single, host-side
`babashka.cli` spec (`core.clj`'s own `cli-spec`, distinct from
`help.clj`'s rendering spec of the same name) needed three new coerced
keys (`:patients`, `:warm-up-seconds`, `:churn`) and nothing else.
**(3)** Structural Result typing meant `run-command`'s own return value
needed no unwrapping, parsing, or reshaping at all — sim's Result maps
and tools' own are interchangeable by shape, not by shared code.
**(4)** The help-group data shape absorbed a new group with no changes
to its own renderers. **(5)** The `-fn` injection point
(`:sim-run-fn`, defaulting to the new `sim-run-command`) is what kept
`bases/ehr-cli`'s own CLI tests hermetic, exactly the property `notes/tools/ADRs.md` ADR-0012
named it for.

### Deviation record

**A genuine, previously-latent finding, surfaced by the mount, not
caused by it.** `sim_manifest_contract_test.clj` asserted
`(:generator :name)` equals `"ehr-testing-sim"` — sim's *pre-H2-rename*
self-identity. `components/sim/src/ehrt/sim/manifest.clj:77` has
reported `"ehrt.sim"` since ADR-0001's own mechanical rename. This test
path had never actually executed end to end before this session (always
skipped, local and CI both, for lack of a sibling checkout at the
moments it ran) — the in-process mount is what first let it run for
real, and it caught its own staleness on the first real run.
AUTHORS-GUIDE.md's two-failure-modes discipline: a sound check
disagreeing with reality is a finding (leave it red, `notes/tools/ADRs.md`
ADR-0013's own precedent); a check misencoding its own invariant is an escalation
(fix the check). This is the second kind — the rename was already
deliberate and ratified, so the test's own expectation was corrected
to `"ehrt.sim"`, not left red.

**The injection-seam convention, corrected mid-session to match this
codebase's own existing pattern, not invented fresh.** `ehrt.tools.sim/run!`'s
first draft took an injectable `:run-command-fn` as a *second*
argument (`(run! opts {:run-command-fn fake})`), modeled on no
particular precedent. `components/tools/test/ehrt/tools/corpus/generators_test.clj`'s
own `:sim` entry calls `(sim/run! params)` with one argument — the same
single-opts-map convention `ehrt.tools.corpus.generate/generate!`'s own
`:run-invocation`/`:resolve-artifact`/`:resolve-java-bin` already use,
proven by three tests that broke the moment `run!`'s real signature
diverged from what `generators.clj`'s already-committed `:execute-fn`
assumed. Fixed by moving `:run-command-fn` into the single opts map
(pulled out and `dissoc`'d before delegating, same shape as `:out-dir`)
— a real correction caught by the existing test suite, not a
hypothetical one.

**The witness test's own stderr-merge bug, caught by itself.**
`sim_cli_real_invocation_test.clj`'s first draft merged `bin/ehr`'s
stderr into stdout (`redirectErrorStream true`) before parsing stdout
as EDN — a JVM/WSL warning on stderr (this checkout's own documented
stale-fsmonitor-class warning, AUTHORS-GUIDE.md) corrupted the parse.
Fixed by keeping the streams separate and reading only stdout, the same
discipline `real-git-describe`'s own docstring already names for
exactly this reason.

**Named, disclosed, out of scope.** The five sim-consuming test
namespaces' own docstrings and prose still narrate a subprocess/
sibling-checkout world in places beyond the specific "Skips cleanly..."
sentences this session corrected (e.g. `sim_intake_test.clj`'s own
opening paragraph still frames itself as proving intake against "a
real *sim manifest*... something the unit-level fixtures... cannot
cover, since they build their own synthetic... values rather than
invoking sim" — still true, just no longer contingent on a sibling).
Not rewritten wholesale this session; the specific sentences asserting
skip-when-absent behavior that no longer exists were the correctness
bar, not full prose freshness.

---

