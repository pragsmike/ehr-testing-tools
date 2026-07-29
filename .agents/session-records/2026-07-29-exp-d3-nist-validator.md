# 2026-07-29 — EXP-D3: offline NIST/CDC HL7 v2 profile validator characterized

## Scope

Autonomous session (R30), three checkpoints. EXP-D3
(`components/tools/docs/experiments.md`) had never executed. This
session pinned `CDCgov/lib-hl7v2-nist-validator` at its last tagged
release (`1.4.0`, commit `e059e58`), built it in a scratch directory
outside the workspace tree, proved it runs fully offline after one
dependency fetch, characterized `NistReport`'s actual shape and
classification filtering against both of the wrapper's own shipped
sample profiles, and tested the three named `judge.v2`-dropped
customer mutations against the profiles actually available. Full
reasoning-of-record: `components/tools/docs/experiments/EXP-D3.md`
(protocol) and `EXP-D3-results.md` (results).

## Red→green evidence highlights

`mvn package` against the pinned commit's **unmodified** `pom.xml`
(no SSL-insecure flags) — `BUILD SUCCESS`, all six NIST-origin
coordinates resolved from `hit-nexus.nist.gov` under default TLS
trust. 16/16 harness invocations (both shipped profiles ×
baseline + mutants) byte-identical between an online run and a
`unshare -r -n` network-isolated re-run; a direct `curl` to
`hit-nexus.nist.gov` from inside the isolated namespace returned no
response (exit `000`), confirming genuine isolation. All six
coordinates' sha256 cross-verified by two independent HTTP/TLS
clients (Maven's resolver, plain `curl`) — identical hashes both ways.
Classification-filtering finding verified by direct execution, not
just source reading: reflection on `ProfileManager`'s private
`validator` field exposed the raw `Report` alongside the wrapper's own
filtered `NistReport` for the same input, showing 2,014 of 2,663 raw
findings (75.6%) on the baseline message never reach `NistReport` at
all.

## Judgment calls and their ratification status

Autonomous session, no author present to ask — every call below is
this session's own, per the prompt's decision procedures, not
individually ratified yet:

- **Pinned the CDC wrapper at its last tagged release (1.4.0), not
  `main`.** `main` has moved to an in-progress `1.5.0` and, since
  commit `3fc7950` (2026-04-03), vendors the six NIST jars into
  `lib/` as a `file://` local Maven repo — a materially easier
  offline path, but one that would have dodged the SSL-bypass question
  the protocol's own STOP clause anticipated, and one the actual last
  *released* artifact doesn't carry. Judged that testing the released
  artifact, not the unreleased tree, is what an adopter would actually
  consume.
- **Wrote the protocol after already running `mvn dependency:resolve`
  once** (to confirm the 1.4.0-vs-`main` discrepancy existed before
  committing to a pin) — disclosed explicitly in the results file's
  own Protocol Amendments section rather than left implicit. Judged
  this doesn't invalidate the finding (re-confirmed identically during
  formal execution) but is worth naming per the house's loud-
  correction discipline.
- **Added three supplementary `PID-7` datetime mutants mid-session**
  after the registered `malformed-datetime-value` operator produced
  zero findings, to distinguish "the engine doesn't check this" from
  "this mutation didn't land." Judged additive under the protocol's
  own "classification is additive" acceptance framing (EXP-C5's own
  precedent), not a procedural violation.
- **Ran both of the wrapper's shipped sample profiles** (`TEST_PROF`,
  `COVID19_ELR-v2.3.1`) rather than just one, for cross-validation —
  the protocol allowed either; judged the extra profile cheap and
  worth the corroboration it produced (both profiles agreed on all
  three customers' convict/no-convict outcome).
- **Added six lockfile entries to `artifacts.lock.edn`** (Step 7's
  conditional branch), judging the URL+sha256 shape fits cleanly for
  these six specific leaf coordinates; explicitly did **not** lockfile
  the wrapper's other ~20 Maven-Central transitive dependencies
  (ordinary, provenance-uninteresting artifacts) — a decision made in
  the results file per the protocol's own framing, not pre-judged in
  the protocol.
- **Used `:kind :engine`** for the six new lockfile entries (no
  dedicated `:library` kind exists in the current schema) and prefixed
  each `:name` with `nist-` for uniqueness/clarity alongside the
  existing single-word names (`synthea`, `temurin-jdk`,
  `fhir-validator-cli`) — a naming choice, not a schema change.

## Findings and HEAD landed

Four checkpoints, four commits, all pushed (pre-push hook: WSL
provenance, gitleaks — no leaks — and `clojure -M:poly check`, both
green throughout):

1. `1dbe9b5` — `docs: EXP-D3 protocol -- offline NIST/CDC v2 profile
   validator`.
2. `692b06a` — `docs: EXP-D3 results -- offline NIST/CDC v2 validator
   characterized` (results file, `experiments.md` row update, facts
   register F4-F6).
3. `98e8bc4` — `chore: lockfile entries for NIST-origin v2-validation
   coordinates (EXP-D3)`.
4. This close-phase commit (session record, self-archived prompt).

**Central findings, stated plainly** (full detail in
`EXP-D3-results.md`):

- The SSL-verification-bypass STOP clause did **not** fire —
  `hit-nexus.nist.gov` resolves cleanly under default TLS trust today,
  contrary to the 1.4.0 release's own README instructions (which
  predate `main`'s later jar-vendoring commit).
- `NistReport`'s own classification filter silently drops
  `Affirmative` and `Validation Notes` findings (and, by source
  reading, `High Alert`/`Specification Error`) — 75.6% of the raw
  engine's own output on the wrapper's baseline message. This is
  exactly the classification (`Affirmative`) the engine uses to mean
  "this optional field/group is empty, and that's allowed" — which is
  why two of the three named customers don't convict against the
  profiles this session had available.
- No ADT-typed profile exists anywhere checked (the wrapper's own
  fixtures, NIST's `v2-validation`, IGAMT's own tree) — **customer
  conviction against the repo's own vendored SimHospital ADT corpus
  remains unproven**, named as the gap it is rather than inferred from
  the ORU profiles that were available. Against those two ORU
  profiles: 1 of 3 customers convicts (`corrupt-segment-name`); 2 do
  not (`drop-pid-segment`, `blank-non-header-field`), for a profile-
  authoring reason (`Usage="RE"` on both tested fields/groups) the
  results file is explicit is an *inference* about a stricter profile,
  not a proven result.

HEAD after this session's ceremony: this commit. No tags cut, no `gh`
mutations — both the author's own ceremony, untouched this session.

## Deviation record

**Protocol written after one round of informal dependency-resolution
reconnaissance**, rather than strictly before any execution touched
the target — disclosed in `EXP-D3-results.md`'s own Protocol
Amendments section (item 1) and in the Judgment Calls section above,
not silently absorbed. Does not change any finding's validity (the
build was re-run identically, formally, during the protocol's own
Step 2) but is named because the protocol's H1 hypothesis text reads
as forward-looking when the answer was, in fact, already known at
drafting time.

**Three supplementary mutants added mid-session**, beyond the
protocol's own registered operator set — disclosed as an amendment,
additive per the protocol's own acceptance framing, not a silent
scope change.

**Customer conviction against the SimHospital ADT corpus specifically
was not achieved** — the named, anticipated gap (no ADT profile
available without IGAMT authoring, out of an autonomous session's
reach), recorded honestly per the protocol's own decision procedure
rather than worked around or overclaimed from the ORU-profile data
that was available.

No test failures, no flakes, no environment surprises beyond the one
`JAVA_HOME` build hiccup (recorded, unrelated to NIST/SSL), no `gh`
mutations attempted, nothing in `.github/workflows/` touched, no `src/`
code landed — the experiment-only scope held throughout.
