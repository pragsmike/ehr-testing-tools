# 2026-08-29 — the consumer contract: lift the moratorium, row the queue, and document ground-truth consumption

Repo `ehr-testing-tools`, WSL clone `/home/mg/src/ehr-testing-tools`,
branch `main`, HEAD at session start `da21a28`. Docs-only session: no
`src`, no `test`, no runtime config. Ceremony R30 (commit and push at
each checkpoint, unattended).

## The prompt, verbatim

Session prompt -- the consumer contract: lift the moratorium, row the
queue, and document ground-truth consumption

Context. HEAD da21a28. TS-1..5 are closed by sha; both 10^5 cells are
MEASURED; the traffic-scale program is complete in fact. The downstream
consumer's primary want is the RICH GROUND-TRUTH EVENT STREAM: how to
invoke `ehrt` to get it, how to parameterize the mix, what the stream
contains, and what is guaranteed about it. Today that contract is
scattered across ADRs, `docs/dev/traffic-model.md`, generated surfaces,
and eleven session records. This session writes it down where a consumer
looks -- from SHIPPED surfaces only (`docs/`, `ehrt --help` verbs,
`docs/formats.md`), as an unspoiled reader: anything you must open
`notes/adr/` or a session record to learn is, by definition, a gap.

Step 0, own commit, docs-only. (i) `AGENTS.md:23`: the moratorium
paragraph gains its closing line -- "LIFTED 2026-08-29: arc 1 landed
41081dd (2026-08-25); guide chapter 35 landed ehr-testing-guide@1dbe6a5
(2026-08-29)" -- the condition's two halves cited by sha. (ii) Two
`## Next` rows, author-ruled 2026-08-29, in order: `[engine-namespace-
extraction-and-apply-unification]` -- "intra-brick extraction of
engine.clj (4,705 lines) and emit_hl7.clj (2,498) into cohesive
namespaces behind unchanged interfaces, FOLLOWED by application-path
unification (decide-drawn / module-compiled / churn-injected events
through one apply choke point); one program, each commit output-
identical and bracket-proven; TS-3's A' span-gate paid part; the
unified path is also event-stream mutation's injection point" -- and
`[event-stream-mutation]` -- "mutation moves to the ground-truth event
stream (emitters inherit mutations); ADR-0166's test-side event
mutations promoted to a shipped operator catalog with `check` as
oracle; file-level operators remain only for lowering-layer faults;
AFTER the extraction row; design ADR first."

Step 1. The audit. As the unspoiled consumer, attempt from shipped
surfaces alone: (a) generate a rich stream (which verb, which
`--format` -- help.clj :224 names "ground..." something: read the verb
table fresh); (b) parameterize the mix -- every opt-in key with an
authored example and its effect on the mix (the key catalog exists in
NO shipped doc: verify); (c) learn the vocabulary -- `formats.md`'s
per-kind examples and where clinical/operational semantics live;
(d) learn the guarantees. Produce the gap list, each entry "a consumer
cannot learn X from any shipped surface", with the ADR/record where X
actually lives. This list is the record's centerpiece; the doc is its
discharge.

Step 2. `docs/consuming-ground-truth.md`, written for the consumer, in
the manual's voice, covering at minimum: the invocation (verb, format,
output shape of the event stream); the opt-in key catalog with one
authored example config and per-key mix effects; the determinism
contract (seed + config + version, within-version per the manifest's
`:stream-scheme`, what invalidates reproduction); identity semantics
(person / patient / MRN / encounter / placeholder / fill / merge --
the MPI-consumer's table); time semantics (`:t`, horizon, log vs wire
order); the semantic warranty -- what `ehrt sim check`'s catalog
certifies on a clean stream, AND its exclusions: open rows quoted
(P9's never-closing re-opens; the §2.4 latent path made visible by
`no-event-references-a-merged-placeholder`), hazard rates authored-
provisional, the ADR-0172/0173 limitation summaries; the measured
scale envelope (10^5 walls, msg/event 1.2823/1.3574 and its FALL with
scale from 1.63x at 10^3, the 10^6 heap arithmetic, ground-truth-only
is cheap); provenance (what the manifest stamps, how to cite a corpus
in a bug report); fault injection AS IT IS TODAY (churn = event-level
by config; `corpus mutate` = file-level; event-level operator catalog
is the rowed future -- point at the row, promise nothing). Every
figure from the tree or the plan appendix, cited; no new claims.

Step 3. Absorb `[post-partition-narrative-refresh]` (P2): the 43
stale tokens across the six named files re-derived from regenerated
runs at HEAD; row CLOSED. Cross-link the new doc from `docs/README.md`
and `what-is-this.md`. `make docsgen` + full `make test`; push; CI;
no tag. Record: the gap list, what each gap discharged into, the six
files' before/after token counts. Roadmap before record.

Fences. Docs-only -- no src, no test, no config. No claim without a
tree or appendix citation. No promise of unbuilt features. The
warranty section states exclusions with the same prominence as
guarantees. Rows verbatim as ruled above.

## Two premises the session corrected

Both are recorded in
[`../session-records/2026-08-29-consumer-contract.md`](../session-records/2026-08-29-consumer-contract.md)
section 3, and both were fix-forward with disclosure rather than
stop-and-report, since neither has a second defensible reading.

1. **`no-event-references-a-merged-placeholder` exists nowhere in the
   tree.** The companion gate TS-4 actually landed is
   `no-resolution-after-a-placeholder-is-consumed`.
2. **msg/event RISES with scale rather than falling.** The `v2` series
   reads 1.050 -> 1.217 -> 1.357 across 10^3 -> 10^4 -> 10^5, and the
   TS-4 close says it "is still climbing".

A third figure, `engine.clj (4,705 lines)`, was true at `1b4e264` and
is 4,884 at `da21a28`; the row carries the correction.
