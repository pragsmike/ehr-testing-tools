# SimHospital corpus adoption — vendor, prove, register

You are working in `ehr-testing-tools`. This session partially closes the
longest-standing external blocker: the foreign v2 corpus sample (P7
residue). `docs/research/HL7v2-sanitized-corpus-research.md` (committed
2026-07-25) identified Google SimHospital's bundled 1,013-message HL7
v2.3 artifact — Apache-2.0, synthetic by construction, partly
longitudinal — as the best freely available corpus. Its central
quantitative claims were independently reproduced before this prompt was
written (message count, type distribution, patient count, checksum —
figures below). This session vendors that artifact into the tree with
provenance, proves `corpus.er7` and `corpus.intake` against it, records
the mechanism decision as an ADR, and makes the EXP-A3 edit the
2026-07-25 handoff's next-steps item 2 asked for.

**Read first:** `AGENTS.md`; `AUTHORS-GUIDE.md`;
`docs/research/HL7v2-sanitized-corpus-research.md` (all of it — its §5
sanitization gate and Phase B are referenced below); `notes/ADRs.md`
(0008–0010 for register style); `notes/facts-register.md` (row format;
note the highest row number — DOC-3 may have added rows since this prompt
was written, so "next free" below means what you observe, not what this
prompt assumed); the P7 row and residue notes in
`.agents/plans/corpus-foundations.md`;
`src/ehr_testing_tools/corpus/er7.clj` and `intake.clj`; two fixtures in
`test/fixtures/v2/` for conventions; `.agents/handoffs/handoff-2026-07-25.md`
(next-steps item 2).

Commits from WSL. `make test` green before and after every commit. Save
this prompt to `.agents/prompts/2026-07-26-simhospital-corpus-adoption.md`;
the final commit archives it.

## Author rulings in effect

Written as settled. The author ratifies or strikes; do not relitigate
them mid-session.

1. **Vendor, not registry.** The corpus commits to the tree. The
   artifact registry (`artifacts.lock.edn`, `ehr artifact fetch`)
   remains for large, generated, or license-encumbered artifacts (JDK,
   Synthea, validator CLI, the eventual IG). Criteria and the boundary
   go in ADR-0011 (Step 1). Rationale in brief: 1.1 MB of Apache-2.0
   ASCII is redistributable; registry-fetched inputs are barred from
   `test/` by the hermeticity rule, and this corpus's whole purpose is
   fast-path test input; upstream is archived, so fetch-at-use has a
   single point of permanent failure that vendoring extinguishes.
2. **Probes are one-time; tests guard path behavior.** Exhaustive
   verification (all 1,013 messages) runs once, in this session, and is
   recorded as F-rows with evidence and date. The committed tests assert
   over a small hazard-selected slice plus a cheap presence/framing
   guard. Corpus integrity is revision control's concern — git
   content-addresses every byte; the suite does not re-prove it per
   push. A future session that "helpfully" promotes the exhaustive probe
   into the recurring suite is reverting a deliberate decision.
3. **Location:** `test/fixtures/v2/simhospital/` — the corpus, upstream
   `LICENSE`, and `PROVENANCE.md` live together.
4. **Internal test input only.** No derived corpus is published or
   redistributed without walking the research doc's §5 sanitization
   gate. The artifact contains realistic UK-flavored demographics
   including valid-format NHS numbers; that is acceptable for committed
   test input and is stated in `PROVENANCE.md` so nobody later ships a
   derivative unsanitized.
5. **Scope fence.** This session does not extend the v2 operator
   catalog, does not touch `test-integration/`, does not modify
   `pipeline.edn` or `use-cases.edn`, and does not begin the research
   doc's Phase B (pathway-driven generation of the missing ORM/A02/A03
   story). Phase B is a future session with its own prompt.

## Reference figures (reproduced 2026-07-25, pre-session)

Fetched from
`raw.githubusercontent.com/google/simhospital/master/docs/artifacts/messages.out`:

* sha256 `fa9719a5f157391dcf78197e4239bce8af0382ae40b903d019a2773a1a9ff520`;
  1,158,713 bytes.
* 1,013 messages, HL7 v2.3, segment delimiter CR (`\r`, classic ER7 —
  not LF).
* Types: 400 `ADT^A01`, 610 `ORU^R01`, 2 `MDM^T02`, 1 `ADT^A34`.
* 403 distinct PID-3.1 MRNs. Messages-per-patient: 3×1, 345×2, 17×3,
  3×4, 3×5, 10×6, 4×7, 13×8, 4×9, 1×14.
* PID-3 carries repetitions (MRN + NHS number) in at least some
  messages.

Your Step 2 fetch must reproduce the checksum and byte count exactly.
Your Step 4/5 probes must reproduce the counts. Discrepancies are stop
conditions, not things to adjust the prompt's numbers toward.

## Step 1 — ADR-0011: data artifacts vendor; engine artifacts register

House format. **Context:** first external data artifact adoption; the
registry mechanism exists but was built for engines. **Decision:** small
redistributable data vendors into the tree with a provenance sidecar;
large, generated, or license-encumbered artifacts stay registry-fetched;
name the criteria (size order-of-magnitude, license permits
redistribution, role as fast-path test input vs. integration-tier
configuration). Name this corpus as the first instance and the research
doc's Phase B bulk output as the anticipated registry-side case.
**Alternatives rejected:** registry-pinning the corpus (hermeticity rule
would demote its consumers to the nightly tier; availability depends
forever on an archived upstream URL); committing without provenance
(fixtures precedent covers five hand-written messages, not a sourced
artifact whose license and origin carry obligations).

Commit: `ADR-0011: external data artifacts vendor into the tree; engine artifacts stay in the registry`

## Step 2 — Resolve, fetch, verify

Resolve `google/simhospital` `master` to its commit SHA via the GitHub
API (`https://api.github.com/repos/google/simhospital/branches/master`).
Fetch `docs/artifacts/messages.out` and `LICENSE` at that SHA — the SHA,
never the branch ref; archived is a social status, not a technical
guarantee. Verify sha256 and byte count against the reference figures.

**Stop-tripwire:** checksum or size mismatch means upstream moved under
us — halt, report what you observed, adopt nothing.

No commit yet; Step 3 commits the verified files.

## Step 3 — Vendor with provenance and F-rows

Create `test/fixtures/v2/simhospital/` containing:

* `messages.out` — byte-identical to the fetch; no reframing, no newline
  translation (the CR framing is part of what the substrate must
  handle).
* `LICENSE` — upstream's Apache-2.0 text, per its notice requirement.
* `PROVENANCE.md` — upstream URL pinned to the resolved commit SHA; the
  sha256 and byte count; retrieval date; upstream archive status
  (archived 2025-03-28, last source push 2024-03-20, per the research
  doc); message count and type distribution; the NHS-number caveat and
  the §5 gate pointer (ruling 4); one line noting the file is consumed
  by tests via CR framing and must never be checked out with newline
  translation (add a `.gitattributes` entry — `messages.out -text` — in
  the same commit).

Same commit, F-rows at the next free numbers, house evidence discipline:

* **License:** the vendored artifact is Apache-2.0 — evidence: upstream
  `LICENSE` at the pinned SHA, vendored copy's path.
* **Upstream status:** repository archived 2025-03-28 — evidence: the
  research doc's citation plus the GitHub API response you observed in
  Step 2.
* **Structural counts:** 1,013 messages / 400-610-2-1 by type / 403 MRNs
  / checksum — evidence: your own reproduction commands (e.g. `tr '\r'
  '\n' < messages.out`, `grep -c`, the awk PID tally) run this session,
  with their output, dated.

Commit: `Vendor SimHospital 1013-message corpus with provenance manifest (ADR-0011)`

## Step 4 — er7 round-trip: exhaustive probe, then the slice test

Order matters (ADR-0006: the failing test precedes the implementation it
justifies; here the probe precedes the test that encodes its findings).

**Probe (one-time, this session):** split the corpus into its 1,013
messages on CR framing and round-trip every one through `corpus.er7`
split/join, asserting byte-identity. Scratch script, not committed.
Record as an F-row: claim (er7 round-trips the entire vendored corpus
byte-identically), evidence (the command, the 1,013/1,013 result), date.

**Stop-tripwire:** any message that fails round-trip is a finding about
er7, not a fixture defect. Stop, record the message verbatim and the
divergence in the session report, do not patch er7 to pass, do not
proceed to the slice test. That outcome is a successful session with a
different deliverable.

**Committed test** (test-first: write it against the not-yet-written
loader helper, watch it fail, then make it green):

* A corpus-loading helper (in test support code, not `src/`) that reads
  `test/fixtures/v2/simhospital/messages.out` and CR-splits it.
* Presence/framing guard: the file loads and splits to exactly 1,013
  messages. This is path-and-framing behavior — a rename, a missing
  file, or a CRLF-mangling checkout fails here loudly instead of
  confusingly downstream. It is not integrity auditing (ruling 2).
* Round-trip byte-identity over a hazard-selected slice, each member
  named in a comment for why it earns its place, mirroring how the
  adversarial fixtures were chosen: (a) a message whose PID-3 carries
  repetitions (MRN + NHS number); (b) an `ORU^R01` with a long OBX tail;
  (c) the lone `ADT^A34`. Select by stable structural predicate (e.g.
  first message matching), not by byte offset.

Red→green evidence in the session report.

Commit: `corpus.er7: round-trip proven over the SimHospital corpus (exhaustive probe registered; slice test committed)`

## Step 5 — intake: exhaustive probe, then the slice test

Same shape. P5 built the intake route; P7 never exercised it against a
foreign corpus; this is its first real consumer.

**Probe (one-time):** intake the full corpus via `corpus.intake`; assert
catalog-entry and intake-record shape over all messages and the
403-distinct-patient count. F-row with command, counts, date.

**Committed test:** intake the Step 4 slice through the same public
entry points; assert record shape on those messages. The 403 count lives
in the register, not in a per-push assertion.

**Stop-tripwire:** if intake's design cannot accommodate the corpus
without `src/` changes beyond a small, obviously-correct fix, stop and
report rather than redesign — intake evolution is its own session.

Commit: `corpus.intake: first foreign-corpus consumer (SimHospital); exhaustive probe registered`

## Step 6 — Documentation closure

* `.agents/plans/corpus-foundations.md`, P7 row: the foreign-corpus
  residue moves from "still awaited" to partially satisfied — a
  synthetic foreign corpus is vendored and proven; a partner-team sample
  remains desirable for the relationship purpose; event coverage is
  narrow (no ORM/A02/A03 — the research doc's Phase B is the named
  follow-on).
* `docs/experiments.md`, EXP-A3 row: the two-sentence edit handoff
  next-steps item 2 requested, recording the re-inflation path with the
  research doc as citation — no Synthea v2 exporter exists (issue #535);
  the viable route is MITRE's official custom-exporter template plus
  HAPI; MayaMaker is prior art (ADT-only, C#, GPL-3.0, stale) to mine,
  not adopt. Two sentences; the row stays backlog.
* Do not edit the 2026-07-25 handoff — handoffs are dated records.

Commit: `plans/experiments: foreign-corpus blocker partially closed; EXP-A3 re-inflation path recorded`

## Step 7 — Session close

1. `make test` (green), `make coverage` (floor 85 holds — the new tests
   should not move `src/` coverage; if the floor trips, something in
   this session wrote `src/` code it shouldn't have — stop and
   reassess), `make lint-pipeline`, `make lint-deps`.
2. Golden check: `make pipeline && make use-cases && git diff
   --exit-code docs/pipeline.md docs/use-cases.md docs/signature.edn`.
   Any diff is scope creep (ruling 5) — stop.
3. Session report in the house location: probe results (both), red→green
   evidence, F-row numbers minted, anything stopped on.
4. Move this prompt to `.agents/prompts/archive/`.
5. Final commit, then `git push origin` from WSL — read the pre-push
   hook's output; the session is not over until the push is confirmed on
   the remote.

## Stop-tripwires, collected

* Step 2: upstream checksum/size mismatch → halt, adopt nothing.
* Step 4: any round-trip failure → er7 finding; report, don't patch.
* Step 5: intake needs redesign → report, don't redesign.
* Step 7: coverage floor trips or golden files diff → scope breach;
  reassess before any further commit.
* Anywhere: a reference figure fails to reproduce → the discrepancy is
  the deliverable; record it, do not adjust toward this prompt.
