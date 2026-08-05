<!-- Attic file: notes/adr/0012-judge-v2-nist-adopts-the-nist-engine-directly.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0012 — `judge-v2-nist` adopts the NIST engine directly: msg-id contract, Cause growth, fixture provenance

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30. msg-id `ex-info` mechanism (below) ratified `[A]` (ADR-0007 provenance tag), 2026-07-30, judge-v2-nist follow-through session.

**Note (2026-07-30, added by the judge-v2-nist follow-through session):**
this record shares its number with the frozen `notes/tools/ADRs.md`
ADR-0012 (the `ehr sim` mount's own pre-existing design, cited
origin-qualified throughout `notes/ADRs.md` ADR-0005 above). Per this
file's own preamble citation rule: a bare `ADR-0012` anywhere in this
workspace's live documents means *this* record; the tools-era one is
always cited as `notes/tools/ADRs.md` ADR-0012 or `tools/ADR-0012`.

### Context

ADR-0011 built the per-engine judge seam and named `judge-v2-nist` (NIST
HL7 v2 `v2-validation`, profile-aware) as the third sibling, explicitly
not landed that session — pending EXP-D3. EXP-D3 (2026-07-29) recorded
the six NIST-origin Maven coordinates resolve cleanly from NIST's own
Nexus (`hit-nexus.nist.gov`) and characterized the CDC wrapper's own
report-filtering behavior (`docs/experiments/EXP-D3-results.md`). A
Cowork cloud session (2026-07-30) then spiked the engine directly from
Clojure — execution-verified, not read-verified — and found a
Java-friendly synchronous API (`hl7.v2.validation.SyncHL7Validator`)
this workspace's own platform research doc
(`components/tools/docs/research/NIST-HL7v2-dev-test-platform.md` §D.5)
had not surfaced; its own notes and script are archived verbatim at
`components/tools/docs/research/judge-v2-nist-spike-notes.md` and
`judge-v2-nist-spike.clj`. This record is the landing session that
followed, same day, and the decisions it had to make that the spike
itself left open.

### Decision

**Direct engine, not the CDC wrapper — supersedes the platform research
doc's own §D.5 recommendation.** `components/judge-v2-nist` depends on
the three `gov.nist` coordinates (`hl7-v2-validation`, `hl7-v2-parser`,
`hl7-v2-profile`, all 1.7.3) directly; `gov.cdc:lib-hl7v2-nist-validator`
is never a dependency, only a worked-example citation. Reasons, in order
of weight: **(1)** the wrapper's own `ProfileManager.filterAndConvert`
keeps only 4 of the underlying engine's 8 classification strings
(`Error`/`Warning`/`Alert`/`Informational`), silently discarding
`Affirmative`/`Informational`-adjacent/`Specification Error` signal —
EXP-D3's own Round 3 measured this at 75.6% of raw findings dropped
against the wrapper's shipped baseline message, and `Affirmative` is
exactly the classification meaning "this optional/RE field is empty,
and that's fine," load-bearing for this workspace's own verdict policy
and the mutate↔judge alignment loop the module docstring names. **(2)**
the wrapper's own documented version drift (README claims 1.6.3, last
published POM pins 1.6.10, unreleased `main` pins 1.7.3 — platform
research doc §D.4) is a supply-chain smell this workspace does not need
to inherit once the underlying coordinates are directly resolvable.
**(3)** hit-nexus resolving cleanly (EXP-D3) removes the platform
research doc's own stated reason for preferring the wrapper (§D.2: the
NIST coordinates aren't independently resolvable) — that premise no
longer holds, so the recommendation built on it no longer holds either.
The one Scala surface the direct path crosses
(`scala.jdk.javaapi.CollectionConverters/asJava`, once, at validator
construction) is not enough interop friction to outweigh (1)–(3).

**msg-id contract: explicit when a profile declares more than one,
never picked implicitly.** `ehrt.judge-v2-nist.v2/execute` refuses (via
`ex-info`, `{:type :ambiguous-msg-id :msg-ids [...]}`) when the profile
bundle's own `:msg-ids` has more than one entry and the caller passed no
explicit `:msg-id` — a single-id profile needs no `:msg-id` at all.
Sorting (or any other implicit tie-break) was considered and rejected:
picking a message-id ordering by convenience would silently validate
against the wrong message type on a plural profile, exactly the kind of
caller mistake `judge-v2-hapi`'s own `gate-file` docstring already
distinguishes from an operational condition. Neither sibling engine
(`judge-v2-hapi`, `judge-fhir-official`) has a genuine precedent for a
caller-contract violation of this shape (both are throw-free,
result-not-throw throughout, and neither has a concept requiring
caller disambiguation) — this is the *fallback* case named in this
session's own decision procedure: no precedent existed, so this
executes ex-info (a programming defect in the *call*, not an engine
verdict about the message under test, must not masquerade as
`:rejected` or `:no-verdict`), and it is flagged here for author
ratification rather than treated as settled convention.

**Ratification (2026-07-30, `[A]`, ADR-0007 provenance tag — direct
author ruling, judge-v2-nist follow-through session).** The msg-id
`ex-info` mechanism above is ratified. The doctrinal basis is
`AGENTS.md`'s own pre-existing Result-not-throw carve-out — "Exceptions
are for programmer error only" — which this session's own sibling-engine
grep looked one layer below: it searched the two sibling *engines* for a
caller-contract-violation precedent and found none, but the precedent
was never going to live in an engine; it lives in the workspace-wide
Result-not-throw rule itself. An ambiguous `:msg-id` — the caller
failing to disambiguate a plural-id profile — is a defect in the
*call*, not an engine or data outcome, so the mechanism was
doctrine-consistent all along. Standing boundary, stated once so no
future engine re-litigates it: engine and data outcomes are values
(findings, verdicts, `:check-exception` captures, `kernel/error`
results); caller-contract violations are programmer error and fail fast
via `ex-info` with descriptive data.

**`ehrt.judge.finding/Cause` grows its second specimen:
`:profile-spec-error`.** The enum is now
`[:enum :terminology-suppressed :profile-spec-error]`. The NIST
engine's own `Specification Error` classification means the
conformance profile (Π) itself is defective (e.g. references a value
set that doesn't exist) — the criterion could not be fully applied to
the message under test, distinct from `:terminology-suppressed` (the
criterion is sound, an external resource is merely absent).
`judge-v2-nist.v2/interpret` returns `:cause :profile-spec-error`
directly for Specification-Error captures; the spike's own
`:proposed-cause` rider (a placeholder for exactly this ADR) is deleted,
and its one specimen test renamed and updated to assert the real cause.
Co-landed with `judge-v2-nist` itself in one commit, per this
workspace's own co-landing discipline (a new engine step's invariants
ship with it, not after).

**Fixture provenance: a stand-in, not this project's own profile.**
`components/tools/test-fixtures/v2-nist/` vendors CDC's own
`COVID19_ELR-v2.3.1` Π bundle (`PROFILE.xml`, `CONSTRAINTS.xml`,
`VALUESETS-disabled.xml` — as shipped, not renamed) plus one companion
ER7 message, from the author's local
`~/Documents/NIST/lib-hl7v2-nist-validator` clone (HEAD
`eeac90c5f88dca3018992005232acdf3da644d88`), Apache-2.0, full
provenance and per-file sha256s in that directory's own `NOTICE.md`.
This is an explicit stand-in until a project-authored IGAMT export
replaces it — `notes/facts-register.md` F8 (the IGAMT registration
disclaimer, captured verbatim 2026-07-29) names the derived-from/
modified-notice obligation that *future* export will carry; F8 does not
attach to this vendored CDC test resource directly (it is CDC's own
fixture, not an IGAMT export this project produced), and `NOTICE.md`
says so, so a future replacement session knows where that obligation is
recorded rather than re-deriving it.

**No-vendor posture reaffirmed; mirror/fork deferred, dated.** Jars are
never vendored into this repo — they resolve from `hit-nexus.nist.gov`
via each affected `deps.edn`'s own `:mvn/repos` entry (root, plus every
project whose own `deps.edn` resolves independently:
`projects/conformance`, `projects/integration`, `projects/ehrt-cli` —
`poly test :all` does not inherit root's `:mvn/repos`, a real finding
this session hit directly rather than one the spike's own wiring notes
anticipated) into the local `~/.m2` cache, matching the ADR-0005
amendment's (2026-07-24, `notes/tools/ADRs.md`) no-redistribution
posture, reaffirmed rather than revisited by this session. Mirroring
the six resolved jars into a `file://` repo (CDC's own pattern, named in
the spike's own notes) is deferred to a future session, not built or
scheduled here — noted as a real future risk (hit-nexus has no stated
SLA and changed operators, NIST → Prometheus Computing, August 2026)
but out of this session's own scope.

**Engine version reads "unknown" for this engine — a real, disclosed
finding, not a bug.** Unlike `judge-v2-hapi`'s HAPI jars (Maven-built,
carry `META-INF/maven/.../pom.properties`),
`gov.nist:hl7-v2-validation:1.7.3` packages no Maven metadata at all
(confirmed by direct jar inspection) — `v2/engine-version` correctly
falls back to `"unknown"`, the same fallback path judge-v2-hapi's own
`hapi-version` already has for exactly this case, not a defect
introduced by this landing.

**Interface re-export, CLI expansion deferred.** `ehrt.tools.interface`
re-exports `v2-nist-make-validator`/`v2-nist-gate-file`/
`v2-nist-gate-dir`, same qualification discipline ADR-0011 established,
with one documented signature difference: this tier's `gate-file`/
`gate-dir` take a validator-state map (from `make-validator`, built once
per Π bundle and reused across files, since context construction
dominates cost), not a bare path — Π is an input at this tier, not a
fixed dependency. A real `bases/cli` `gate v2-nist` verb (a bundle-dir
flag, validator-state caching across a single invocation) is deliberately
NOT built this session — real design work, not a re-export — and is
named here as follow-on, not silently dropped.

**Verification.** `clojure -M:poly check`: green throughout (each
addition verified incrementally: component landing, tools-interface
requiring the new component, the three affected projects'
`:mvn/repos`/`poly/judge-v2-nist` wiring). `clojure -M:poly test :all
skip:integration`: full log captured directly (`> file 2>&1; echo
EXITCODE:$?`, no pipe, per this workspace's own tail-masks-exit-code
lesson) — `EXITCODE:0`, 181 test namespaces, zero `FAIL`/error markers
beyond the expected `0 failures, 0 errors` on every namespace, up from
this session's own 177-namespace baseline (`judge-v2-nist`'s own two
new test namespaces). All six NIST jar sha256s re-verified against
`artifacts.lock.edn`'s existing EXP-D3 entries (resolved fresh via
hit-nexus into `~/.m2`, byte-for-byte match, dated verification line
appended to each entry's own `:license-note`).

### Deviation record

**`:mvn/repos` is not inherited from root `deps.edn` by `poly test
:all`'s own per-project resolution — a real finding, not anticipated by
the spike's own wiring notes.** The spike's own `NOTES.md` named adding
`:mvn/repos` "to the ROOT deps.edn" as the one step needed; that is
sufficient for `clojure -M:dev:test`-style invocations (which resolve
against root `deps.edn` directly) but not for `poly test :all`, which
resolves each of `projects/conformance`, `projects/integration`, and
`projects/ehrt-cli`'s own `deps.edn` independently. Found by actually
running the full suite after the tools-interface wiring landed (`poly
check` passed; `poly test :all` failed on artifact resolution) — fixed
by repeating the same `:mvn/repos` entry in each of the three affected
project `deps.edn` files, same discipline `poly/judge-v2-nist`'s own
local-root entry already needed at that same layer (ADR-0011's own
"flat, project-level convention" — no component `deps.edn` carries a
sibling `poly/X` entry; each project names every brick and every
external repo it needs directly).

**Measured engine-in-the-loop numbers matched the spike's finding
counts exactly, but not its verdict/cause.** The spike's own `NOTES.md`
predicted `:no-verdict/:terminology-suppressed` for the COVID19_ELR
fixture (473 findings: structure 441, value-set 28, content 4 — this
session's own measurement matches every one of those counts exactly,
confirming the wiring is unchanged). The measured *cause* is
`:profile-spec-error`, not `:terminology-suppressed` — not a wiring
discrepancy from the spike, but a direct, expected consequence of this
same session's own Cause-growth decision above: the spike's code
returned `:terminology-suppressed` for Specification-Error captures
only because `:profile-spec-error` did not yet exist in the shared
enum at spike time; `interpret` returns the real cause now that it
does. Recorded here so a future reader comparing this session's pinned
test numbers against the spike's own prose doesn't mistake the cause
difference for an unexplained divergence.

**Fixture layout matched the spike's own description exactly.** CDC's
`COVID19_ELR-v2.3.1` bundle, as found in the author's local clone,
carries exactly `PROFILE.xml`, `CONSTRAINTS.xml`, and
`VALUESETS-disabled.xml` — no `VALUESETBINDINGS.xml`/
`COCONSTRAINTS.xml`/`SLICINGS.xml`, matching the spike notes' own
"Wiring into the workspace" step 5 description with no deviation to
record on this axis.

**`projects/ehrt-cli`'s own `:coverage` alias widened for consistency,
beyond this session's own literal step list.** The step list named only
root `deps.edn`'s `:dev`/`:ehrt`/`:test` and the three projects'
`:deps`/`:mvn/repos`; `:coverage`'s own `-p`/`-s` path lists (measure-
and-report, ADR-0004 posture, no enforcement gate) were widened to
include `components/judge-v2-nist/{src,test}` anyway, matching every
sibling engine's own existing entries there — mechanical, low-risk,
and leaving the new component invisible to coverage measurement seemed
a worse default than the small addition.

**CLI/help.clj gate-verb expansion, named but not built.** Per this
session's own step 7 permission ("if that expansion balloons... skip
it, note it as follow-on work"): a real `gate v2-nist` CLI verb needs a
profile-bundle-dir flag this tier doesn't share with `gate v2`/`gate
fhir` (both take a bare PATH) and validator-state reuse across a single
CLI invocation (building a fresh `SyncHL7Validator` per file would
defeat the whole point of `make-validator`'s own "build once per
bundle" discipline) — real design work belonging to a future session,
not a mechanical re-export.

### Ruling — 2026-07-31 (judge-family parity pass, P2-2)

The 2026-07-30 refactoring review (`notes/2026-07-30-refactoring-review.md`,
finding 6) found `gate-file`/`gate-dir` asymmetric between the two live
v2 engines: this ADR's own `gate-file` threw a raw
`FileNotFoundException` across the component interface on a missing
path (`judge-v2-hapi/gate-file` returned `kernel/error :file-not-found`
instead); `gate-dir` returned a bare `{filename result}` map with no
kernel envelope and walked recursively (`file-seq`), where
`judge-v2-hapi/gate-dir` returned `kernel/ok {:results [...]}` and
walked flat (`.listFiles`). Ruled (author, 2026-07-31, P2-2/AR-1):

- **Recursive is the shared rule for every engine's `gate-dir`.**
  `judge-v2-nist`'s own `file-seq` behavior is the standard;
  `judge-v2-hapi/gate-dir` changed to match (its `hl7-files-in` now
  walks `file-seq` instead of `.listFiles`) — a deliberate behavior
  change, not a bug fix, pinned by a cross-engine contract test
  (`projects/conformance/test/ehrt/tools/judge_engine_parity_test.clj`)
  against a fixture tree with one nested subdirectory.
- **Both engines return the kernel envelope from both functions.**
  `judge-v2-nist/gate-file` now returns `kernel/ok {:verdict :findings
  :path [:cause]}` or `kernel/error :file-not-found` (never throws);
  `judge-v2-nist/gate-dir` now returns `kernel/ok {:results [...]}` —
  the same shape `judge-v2-hapi` already produced.
- **The `bases/cli` compensating adapter simplified accordingly.**
  `v2-nist-gate-file*`/`v2-nist-gate-dir*` (`bases/cli/src/ehrt/cli/core.clj`)
  dropped their own `.isFile` pre-check and hand-rolled fail-fast
  directory composition — they now delegate straight to the engine,
  catching only the engine's own `:ambiguous-msg-id` ex-info. The CLI's
  missing-file exit code and message are unchanged from the user's
  perspective (pinned by
  `v2-nist-gate-command-missing-file-is-a-named-error-not-a-crash-test`,
  `bases/cli/test/ehrt/cli/core_test.clj`).
- Co-landed: `judge-v2-nist`'s own component test
  (`v2_engine_test.clj`) now validates its real-engine findings against
  `ehrt.judge.finding/Finding` and `valid-cause-pairing?`, giving it the
  test-tier dependency on `judge` finding 6c named as missing —
  mirroring what `judge-v2-hapi/v2_test.clj` already did.

### Ruling — 2026-07-31 (NIST artifact channel, P2-3)

The 2026-07-30 refactoring review (finding 8) found the six NIST jars
above resolving through *two* channels at once: real classpath loading
goes through `deps.edn`'s `:mvn/repos nist-hit` entry (into `~/.m2`,
confirmed working — the integration lane exercises the engine live from
there), while the same six coordinates also carry
`artifacts.lock.edn` rows that `ehrt doctor`'s `check-artifact-cache`
expected to be fetched into the content-addressed artifact cache —
failing on any machine where the engine itself ran fine, contradicting
this ADR's own engine-onboarding checklist item 4 ("resolves to exactly
one of" the three lockfile targets). Ruled (author, 2026-07-31, P2-3/AR-2,
option (a) of the two named in the review): the six rows **stay** in
`artifacts.lock.edn` as provenance/license records — the
`:use-permitted--unstated--confirmation-pending` posture and its
evidence trail live there, not scattered into `deps.edn` comments — and
each gains `:resolved-via :deps-edn`
(`components/kernel/src/ehrt/kernel/artifact.clj`'s `Artifact` schema,
optional key, default implied `:artifact-cache` when absent).
`check-artifact-cache` (`bases/cli/src/ehrt/cli/core.clj`) skips
cache-checking any row so marked, and says so in its `:detail` line —
the rows remain listed by name/version, just never asked whether
they're in `~/.m2`'s sibling cache. Recorded in
`docs/dev/engine-onboarding.md` checklist item 4 as a dated note.
The spike's own file://-mirror end-state (vendoring the six jars the
way CDC's own wrapper does, named as a future risk in this ADR's own
"Fixture layout" deviation-record entry above, given `hit-nexus`'s lack
of a stated SLA) remains open and unaffected by this ruling — it would,
if built, flip these rows back toward `:artifact-cache`.

---

