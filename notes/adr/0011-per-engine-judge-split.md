<!-- Attic file: notes/adr/0011-per-engine-judge-split.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0011 — Per-engine judge split: `judge-v2-hapi` and `judge-fhir-official`; `judge` keeps the verdict vocabulary

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

ADR-0008 extracted `components/judge` out of `components/tools`,
landing two gate engines (`ehrt.judge.v2`, the in-process HAPI HL7v2
base-structural judge; `ehrt.judge.fhir`, the official HL7 FHIR
validator, pinned subprocess) and a shared verdict vocabulary
(`finding`/`report`/`verdict-cache`) together in one brick. A second v2
engine (NIST `v2-validation`, profile-aware, via CDC's
`lib-hl7v2-nist-validator` wrapper) is on the board as its own future
session (EXP-D3) — this session's own charge (author, 2026-07-29:
*"Let's extract the validators now... I like judge-v2-hapi etc."*) is
to build the per-engine seam EXP-D3 lands into, and to name it for the
translator-under-test use case this workspace is heading toward:
gating the SAME input through multiple named engines and comparing
their verdicts, which reads far more naturally as
`judge-v2-hapi/gate-file` vs. `judge-v2-nist/gate-file` than as two
functions sharing one brick, disambiguated only by a naming prefix.

### Decision

**Landing shape:** `components/judge-v2-hapi` (from `ehrt.judge.v2`),
`components/judge-fhir-official` (from `ehrt.judge.fhir`).
`components/judge` keeps its name and the vocabulary trio
(`finding`/`report`/`verdict-cache`). `judge-v2-nist` is explicitly NOT
created this session — EXP-D3 lands into this seam later, a separate
session.

**Census (namespace -> real callers, whole-tree grep, not prose) —
confirms the session's own premise rather than contradicting it:**

| Namespace | Real callers | Disposition |
|---|---|---|
| `ehrt.judge.v2` | `ehrt.judge.interface` only | -> `ehrt.judge-v2-hapi.v2` |
| `ehrt.judge.fhir` | `ehrt.judge.interface` only | -> `ehrt.judge-fhir-official.fhir` |
| `ehrt.judge.finding` | `ehrt.judge.v2` (**turned out unused, see deviation record**), `ehrt.judge.fhir`, `ehrt.judge.report`, `ehrt.judge.interface`, both engines' own test namespaces | stays in `judge` |
| `ehrt.judge.report` | `ehrt.judge.interface` only | stays in `judge` |
| `ehrt.judge.verdict-cache` | `ehrt.judge.fhir` only | stays in `judge` (see disclosure below) |
| `ehrt.judge.interface` | `ehrt.tools.interface`, `ehrt.tools.check`, `v2_contract_pairing_test.clj`, `check_test.clj` | narrows to vocabulary-only |

No engine-to-engine `:require` in either direction — confirmed, not
merely assumed. Every downstream consumer of the gate functions
(`bases/cli/src/ehrt/cli/core.clj`,
`projects/integration/test/ehrt/tools/contract_pairing_test.clj`) goes
through `ehrt.tools.interface` only, never `ehrt.judge.interface`
directly, which is why the zero-behavior-change contract below rests
entirely on `ehrt.tools.interface`'s own re-exported names, not on
`ehrt.judge.interface`'s.

**verdict-cache placement — disclosed, not silently resolved.**
`ehrt.judge.verdict-cache` has exactly ONE real consumer today
(`ehrt.judge-fhir-official.fhir`), which on its face argues for moving
it there alongside its only caller. Kept in `judge` anyway, per this
session's own ruling: it keys generically on engine name/version +
input content hash + argv shape, nothing FHIR-specific, and the
planned NIST v2 engine (EXP-D3) is its expected second consumer.
Disclosed here for the author to veto post-hoc if the single-consumer
fact changes the calculus.

**Superseded 2026-07-31 (author ruling, P2-4, review finding 7).** The
expected-second-consumer justification above did not materialize:
`judge-v2-nist` landed (ADR-0012, 2026-07-30) without ever touching
`verdict-cache` — `ehrt.judge-fhir-official.fhir` remains the sole
consumer. Ruled anyway to leave `verdict-cache` in `judge` for now
(fix-forward, not a code move) — the generic key shape argument above
still holds on its own, independent of consumer count, and a single
extraction 2 for a still-single consumer is deferred until one of two
concrete triggers fires: (i) a second real consumer actually appears
(not merely planned), or (ii) `judge`'s own tools-split (this same
review's §5.1(a), stage 3, which narrows `tools` to its domain and
would touch every judge-adjacent boundary at once — the natural point
to re-derive this placement alongside everything else moving). No code
changed by this ruling; `verdict-cache`'s existing tests and consumers
are unaffected.

**The HAPI FHIR/HL7v2 Maven-coordinate pair, moved on again.** ADR-0008
moved `ca.uhn.hapi.fhir/hapi-fhir-base` and
`ca.uhn.hapi.fhir/hapi-fhir-structures-r4` into `components/judge/deps.edn`
alongside `judge.fhir`, disclosing at the time that nothing in this
workspace `:import`s either class directly. That disclosure is carried
forward unchanged: the pair moved on to
`components/judge-fhir-official/deps.edn` with `judge.fhir` itself,
still with no live `:import` anywhere (re-verified, whole-tree grep,
this session). NOT dropped — "superseded requires a load-bearing
inventory" (ADR-0008's own phrase) — whether to drop them is named
here as an OPEN AUTHOR DECISION, not resolved by this session.
`ca.uhn.hapi/hapi-base`/`hapi-structures-v24` (the HL7v2 pair,
genuinely `:import`ed in `judge-v2-hapi.v2`) moved to
`components/judge-v2-hapi/deps.edn`. `org.clojure/data.json` moved from
`components/judge/deps.edn` to `components/judge-fhir-official/deps.edn`
alongside its one real consumer (`judge.fhir`'s own JSON
parse/serialize) — `metosin/malli` stays in `judge` (both `finding` and
`report` still use it).

**Interface simplification: unqualified `gate-file`/`gate-dir`, not
carried-forward qualification.** The `v2-gate-file`/`v2-gate-dir`/
`fhir-gate-file`/`fhir-gate-dir`/`fhir-gate-batch` qualification
(ADR-0002, restated ADR-0008) existed only to disambiguate two engines
sharing ONE interface (`ehrt.tools.interface`, then
`ehrt.judge.interface`). Each engine now has its own interface with
nothing left to collide against, so
`ehrt.judge-v2-hapi.interface`/`ehrt.judge-fhir-official.interface`
export plain `gate-file`/`gate-dir`(`/gate-batch`).
`ehrt.tools.interface` re-applies its OWN `v2-`/`fhir-` qualification
at its own re-export layer (now sourced from the two new interfaces
directly instead of from `ehrt.judge.interface`), so every name it
re-exports is byte-identical to before this session — the
zero-behavior-change contract lives entirely at that layer.
`ehrt.judge.interface` narrows to the verdict vocabulary only
(`Report`/`build-report`/`diff-reports`/`baseline-relative-report`/
`report-valid?`/`finding-valid?`), plus two NEW re-exports found
necessary only by running `poly check` after the move (see the
deviation record): `worst-of` and four `verdict-cache-*` functions.

**Test namespaces moved with their engines** (`ehrt.judge-v2-hapi.v2-test`,
`ehrt.judge-fhir-official.fhir-test`); vocabulary tests
(`report-test`, `finding-test`, `verdict-cache-test`) stayed in
`judge`. Both moved test namespaces still `:require [ehrt.judge.finding
:as finding]` directly (calling `finding/valid?`/`finding/valid-cause-pairing?`)
rather than through `ehrt.judge.interface` — `poly check` does not flag
this (Polylith's brick-isolation enforcement applies to `:default`/src
profile namespaces, not `:test`), so left as-is rather than rewritten
for its own sake; disclosed here as an observed asymmetry, not a
violation.

**Dependency wiring** follows the established flat, project-level
convention (ADR-0008's own deviation record: no component `deps.edn`
anywhere carries a `poly/X :local/root` entry for a sibling brick).
`poly/judge-v2-hapi` and `poly/judge-fhir-official` added everywhere
`poly/judge` already appears: root `deps.edn` (`:dev`/`:test`/`:ehrt`),
`projects/ehrt-cli` (including its `:coverage` alias's `-p`/`-s` path
lists), `projects/conformance`, `projects/integration`.
`ehrt.tools.lint`'s `target-2-deps-edn-paths` (the deps-lint mechanism
verifying a catalytic-resource `deps.edn` coordinate actually resolves)
widened to include both new components' `deps.edn` files, same
rationale ADR-0008 already used when it first widened this list.

**Verification.** `clojure -M:poly check`: green (after the deviation
record's own fix, below). `clojure -M:poly deps`:
`judge-fhir-official` -> `{judge, kernel}`; `judge-v2-hapi` -> `{kernel}`
(real, src) and `{judge}` (test-only — see deviation record);
`judge` -> `{kernel}` only; no engine-to-engine arrow either direction;
`tools` -> `{kernel, judge, judge-v2-hapi, judge-fhir-official,
palgebra, sim}`. `clojure -M:poly test :all skip:integration`: exit
code and full log captured directly (`> file 2>&1; echo EXITCODE:$?`,
no pipe) per the sim-sibling errata session's own `tail`-masks-exit-code
lesson -- `EXITCODE:0`, 20m35s, three projects (`conformance`,
`ehrt-cli`, `sim`), zero `FAIL`/error markers anywhere in the 1416-line
log beyond the expected `0 failures, 0 errors` on every namespace;
`ehrt.judge.verdict-cache-test`/`report-test`/`finding-test` (vocabulary,
stayed in `judge`) and `ehrt.judge-v2-hapi.v2-test`/
`ehrt.judge-fhir-official.fhir-test` (moved with their engines) all ran,
each project pulling in the `conformance` project's own brick list
confirming the wiring directly: *"Running tests from the conformance
project, including 7 bricks and 1 project: judge, judge-fhir-official,
judge-v2-hapi, kernel, palgebra, sim, tools, conformance."*
`bin/ehrt gate v2`, `bin/ehrt gate fhir --report`, and `bin/ehrt check`
re-run against the exact fixture set and commands
`notes/judge-engine-extraction-characterization.md` recorded before the
move: all three `--report` EDN files and all three stdout logs (module
the process's own PID-independent `EXIT_*` line) byte-for-byte
IDENTICAL to that baseline (`diff`, zero output on every one of the six
comparisons).

### Deviation record

**`poly check` found two real Polylith interface violations the
ruling's own census didn't anticipate, because they were legal before
this session and illegal after.** `ehrt.judge-v2-hapi.v2` and
`ehrt.judge-fhir-official.fhir` both directly `:require`d
`ehrt.judge.finding` (and `fhir` additionally `ehrt.judge.verdict-cache`)
— fine while all three lived in one brick, `Error 101: Illegal
dependency` once `v2`/`fhir` moved to their own bricks and `finding`/
`verdict-cache` stayed behind, since Polylith requires cross-brick
access to go through the target brick's own `interface` namespace, not
its internals. Resolved two different ways, by what the census under
Step 2 couldn't show (nothing calls a namespace it doesn't use):

1. `judge-v2-hapi.v2`'s own `:require` of `ehrt.judge.finding` turned
   out to be dead code — grepped for `finding/` call sites inside the
   file and found none; the `raw->finding` local function builds plain
   maps, no schema validation call. Removed the require entirely,
   rather than routing a genuinely unused import through an interface.
   Consequence, visible in `poly deps`: `judge-v2-hapi`'s only REAL
   (src) dependency is `kernel`; `judge` shows up only as a **test**-alias
   dependency (its own test suite's `finding/valid?` assertions) — a
   cleaner graph than the ruling anticipated, not a violation of it.
2. `judge-fhir-official.fhir` genuinely calls `ehrt.judge.finding/worst-of`
   and four `ehrt.judge.verdict-cache` functions (`cache-key`, `lookup`,
   `store!`, `default-cache-dir`) — real, load-bearing cross-brick
   calls now that `fhir` lives apart from `finding`/`verdict-cache`.
   Fixed by widening `ehrt.judge.interface` to re-export all five
   (`worst-of`, `verdict-cache-key`, `verdict-cache-lookup`,
   `verdict-cache-store!`, `verdict-cache-default-dir` — no collision
   with any existing export, left unqualified) and routing
   `judge-fhir-official.fhir` through that interface instead of
   `ehrt.judge.finding`/`ehrt.judge.verdict-cache` directly. This is the
   same class of trap ADR-0008's own deviation record named (a problem
   invisible to static census, caught only by actually running the
   tool) — recorded here per the same fix-forward-with-disclosure
   discipline, not folded silently into the interface-sizing section
   above as though it were foreseen.

**Pre-existing prose staleness found, not chased (out of this
session's own narrow docs-sweep scope).**
`components/tools/docs/pipeline.edn` (component-adjacent, not the
user path, not one of the two files this session's own Step 7 named)
cites `ehrt.tools.judge.fhir/verdict-mapping-version` — stale from
before ADR-0008 even (the real namespace has been `ehrt.judge.fhir`,
now `ehrt.judge-fhir-official.fhir`, since ADR-0008 landed). Named here
per ADR-0010's "declare doc rows before writing" discipline rather than
silently fixed outside this session's own declared scope.

**Found: three user-path docs DO name judge internals, contrary to
this session's own working assumption that the grep would come back
clean.** `docs/formats.md`, `docs/glossary.md`, and
`docs/judge-calibration.md` (all user path per ADR-0010's own
disposition) cite `ehrt.judge.fhir`/`ehrt.judge.v2` directly (as
"Schema:"/vocabulary citations for `Report`/`Finding`/`Verdict`/`Cause`
and, in `glossary.md`, as prose naming which library backs which
judge). Two of these citations are now stale by this session's own
move (`ehrt.judge.fhir` -> `ehrt.judge-fhir-official.fhir`,
`ehrt.judge.v2` -> `ehrt.judge-v2-hapi.v2`); the `ehrt.judge.report`/
`ehrt.judge.finding` citations in the same files remain accurate
(unmoved). Per this session's own Step 7 instruction ("if one does,
record it — do not silently fix") and ADR-0010's own "declare doc rows
before writing" discipline, this is recorded rather than resolved:
whether these three docs' own namespace citations count as the kind of
Polylith/internal detail R34 excludes from the user path, or as
legitimate API/schema reference material a user path doc may cite, is
an open author call this session does not make unilaterally. If the
author rules they should be fixed, the two stale citations are the
only ones that actually changed.

---

