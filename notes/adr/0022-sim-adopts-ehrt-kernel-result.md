<!-- Attic file: notes/adr/0022-sim-adopts-ehrt-kernel-result.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0022 — Sim adopts `ehrt.kernel.result`; its own copied envelope (`sim/ADR-0001` point 4) is retired, promise honored

**Status:** Accepted (status line added ADR-0143, 2026-08-16, from `notes/ADRs.md`'s own index row).

### Context

`ehrt.sim.result` was a deliberate ~30-line copy of the result-not-throw
doctrine, carried over from `ehr-testing-sim`'s life as a standalone
repo. Its own docstring stated the copy's expiry condition explicitly:
copied, not shared, *because* the dependency arrow had to point
tools → sim only across two separate repos, with a named escape
hatch — "if a third repo ever needs the doctrine, extract a shared
microlib then — not before" (`notes/sim/ADRs.md` ADR-0001 point 4,
frozen provenance). `notes/2026-07-30-refactoring-review.md` P3-5
flagged this ns as a live candidate the moment `components/kernel`
existed as exactly that microlib (ADR-0008, extracted for judge and
corpus's own shared need) — sitting in the SAME workspace, on the SAME
classpath, as sim, since the 2026-07-28 bootstrap (`sim/ADR-0001`'s own
merge). P3-5 was deliberately scoped to run *after* the `bases/sim-cli`/
`projects/sim` retirement (P3-6, ADR-0021, fired 2026-08-01) — a
single-CLI world shrinks the baseline surface this session's own fence
has to hold constant.

**Shape comparison (done before any code changed, per this session's
own scope fence):** `ehrt.sim.result` and `ehrt.kernel.result` declare
the identical malli schema (`{:status [:enum :ok :rejected :error]
:category {:optional true} :keyword :payload :any}`) and the identical
seven-function surface (`ok`/`rejected`/`error`/`ok?`/`rejected?`/
`error?`/`valid?`), byte-for-byte matching docstrings on four of the
seven. No mismatch, no arm sim relies on that kernel lacks — no
escalation needed.

### Decision

**AR-2, mechanical adoption — moved, not improved:**

1. `components/sim/src/ehrt/sim/result.clj` deleted outright, no
   tombstone. It had no dedicated test file (`ehrt.kernel.result-test`
   already covers the identical shape) so nothing else to delete.
2. Every requiring namespace repointed: five `src` (`interface`,
   `identifiers`, `check`, `run`, `gmf`) and seven `test`
   (`engine-test`, `check-test`, `run-test`, `identifiers-test`,
   `gmf-test`, `vendored-module-test`, `vendored-appendicitis-test`) —
   twelve files total, one require-line swap each, alias (`result`)
   unchanged so every call site (`result/ok`, `result/rejected?`, etc.)
   needed no further edit. **Finding, not assumed:** the require does
   NOT target `ehrt.kernel.result` directly — `poly check` failed with
   five `Error 101: Illegal dependency on namespace kernel.result …
   Use kernel.interface instead` the first time this was tried (poly's
   brick-boundary rule: cross-brick access is through a component's own
   `interface` namespace only, the same rule every existing
   kernel-consumer in this workspace already follows — judge, corpus,
   bases/cli all require `ehrt.kernel.interface`, never
   `ehrt.kernel.result` bare). Fixed by requiring
   `ehrt.kernel.interface` instead (it re-exports `ok`/`ok?`/
   `rejected`/`rejected?`/`error`/`error?`/`valid?` verbatim as
   delegating vars) — `poly check` clean after.
3. **The new `sim → kernel` edge, documented, not just coded:**
   `AGENTS.md`'s Constraints section, `docs/dev/architecture.md`'s
   mermaid diagram and bricks table (the `components/sim` row no longer
   truthfully says "never depends on anything," which it did before
   this session), and the `poly/kernel` comment in every deps.edn that
   already composes both bricks (root `deps.edn`'s `:dev`/`:ehrt`
   aliases, `projects/conformance`, `projects/integration`,
   `projects/ehrt-cli`) — all updated. **No new `:local/root` wiring
   needed anywhere:** dependency wiring lives at the project level in
   this workspace (confirmed by grep — no component `deps.edn` anywhere
   carries a sibling-brick entry), and kernel was already composed in
   every project that also composes sim, because corpus already
   required kernel directly everywhere sim is required. `workspace.edn`'s
   `:necessary` overrides are unaffected — adding a real `:require` edge
   only ever *adds* reachability, never removes it, and `poly check`
   returned clean with no warning 207 anywhere; not independently
   re-derived by clearing overrides (no override touches sim or kernel
   today, so there was nothing to re-derive).
4. **`ehrt.corpus.sim-adapter` needed no code change.** Characterized
   before touching anything: it requires `ehrt.sim.interface` only,
   never `ehrt.sim.result` directly, and its four functions
   (`run!`/`check!`/`identifiers!`/`version!`) delegate straight
   through with no envelope unwrapping, parsing, or reshaping — its own
   docstring already asserted the two Result vocabularies were
   "structurally, not just nominally, the same shape" (ADR-0012
   property 3). That claim is now literally true (same namespace, not
   a coincidentally-matching copy); the docstring text was left
   as-is — updating prose that was already accurate, merely for a
   sharper "why," is exactly the kind of improvement AR-2 fences out.

**AR-1/AR-3, the fence — held:** a full byte-capture baseline (`ehrt
sim run` at two seeds with `--emit hl7`/`--emit fhir`/`--format er7`/
`--format ground-truth`; the `sim run --format ground-truth | sim
check` pipe; `sim identifiers`; `sim version`; `corpus generate sim`;
and six representative error paths — missing `--seed`, a bad `--seed`
value, `:out-dir-exists`, malformed stdin to `sim check`, empty stdin,
missing `--seed` on `identifiers`) was captured against pristine HEAD
`4bf9be0` (via a `git stash`/`stash pop` round-trip run *after* AR-2's
edits, to get a true pre-edit comparison without losing the day's
work) and re-captured after. Every output was byte-identical except:
directory-path artifacts that differed only in this session's own
before/after out-dir naming tag (confirmed identical once substituted
back), and one JVM temp-report filename
(`/tmp/clojure-<random-long>.edn`) in a babashka.cli error message —
inherent per-invocation randomness in the error-reporting library
itself, not a function of this change (the actual error text is
identical). No emitted byte changed anywhere, including error
rendering, where the Result envelope's own `:status`/`:category`/
`:payload` keys are literally what reaches stdout — the exact leak path
this fence was watching for. `poly check`: OK. Per-push lane (`poly
test :all skip:integration`): **168 `Testing ehrt.*` namespaces before
and after** (git-stash-verified against the same HEAD, not merely
assumed), 0 failures/0 errors both times, identical final per-project
tallies (e.g. 71 tests / 284 assertions in the largest suite, matching
digit for digit).

**Self-containment story, amended, not reverted.** `sim/ADR-0001`
point 4's own escape hatch — extract a shared microlib once a third
consumer needs the doctrine — is **honored by this session, not
broken**: `components/kernel` already was that microlib, born from
judge and corpus's own shared need (ADR-0008), independently of and
prior to sim's own move into this workspace. Adopting it is point 4's
promise finally cashed in, not a reversal of it. The narrower rule
`sim/ADR-0001` was actually protecting — `components/sim` must never
depend on anything corpus-derived — is **untouched and exactly as
strict as before**: kernel is not corpus-derived (it predates
`components/corpus`'s own extraction and is the shared foundation
judge, corpus, and `bases/cli` already depend on), so this is a new,
narrowly-scoped edge, not an exception carved into the old one.
Acceptance instruments — judge's verdict vocabulary, the corpus
domain, its generators/mutators/checks/operator registries — remain
exactly as forbidden to sim as they always were; only the error-
plumbing vocabulary is now shared. `notes/sim/ADRs.md` itself stays
frozen, byte-identical provenance (R8) — this record, not that file,
is where the amendment lives, cited origin-qualified
(`sim/ADR-0001` point 4).

### Consequence

`components/sim/src/ehrt/sim/result.clj` no longer exists; every sim
namespace that used to construct or inspect a Result map now does so
through `ehrt.kernel.interface`, identically in every observable way.
One workspace-wide result vocabulary, not two structurally-identical
ones — `ehrt.kernel.result`'s own docstring ("pattern nursery #11")
is now sim's docstring too, by reference rather than by copy. A future
session reasoning about "does sim depend on anything outside itself"
should read this ADR, not assume the pre-2026-08-01 answer (zero
dependencies) still holds.

**Status.** Accepted (author-ruled 2026-08-01, P3-5), 2026-08-01.

---

