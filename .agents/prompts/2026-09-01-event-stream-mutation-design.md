# 2026-09-01 — event-stream mutation: the design ADR, plus the future-features docs rider

**Repo:** `pragsmike/ehr-testing-tools`. **Clone:** `~/src/ehr-testing-tools`
(ext4, WSL — the sole clone of record; `/mnt/c` is retired,
`rulings.md#R-mnt-c-retired`). **HEAD at session start:** `f402868`,
branch `main`, working tree clean.

## The prompt, verbatim

> SESSION: event-stream mutation (P6) — the design ADR, plus the
> future-features docs rider
> Repo: pragsmike/ehr-testing-tools, tip (f402868 or descendant).
> Roadmap row P6 (author-ruled 2026-08-29): mutation moves to the
> ground-truth event stream so emitters INHERIT mutations; ADR-0166's
> test-side event mutations promote to a shipped operator catalog with
> `check` as the oracle; file-level operators remain ONLY for
> lowering-layer faults. Design ADR FIRST — this session writes the
> ADR and the rider; it implements NOTHING.
>
> STEPS (one gate each; full make test per push — docs-only, so the
> suite guards only the doc gates)
> 1. PROBE the mutation surface as it exists: ADR-0166 and its
>    test-side event mutations; every file-level operator and its
>    fault class; check's finding vocabulary; fold/apply-events'
>    signature and the census §3e omission. Gate: inventory in the
>    record with file:line at your sha.
> 2. DRAFT the ADR (payload decision — ADR is the right register):
>    (i) the operator catalog: each operator as a pure
>    (events, seed) → events' function with a NAMED defect class;
>    (ii) the injection contract: where mutation sits relative to
>    decide and apply-events (channel expectation: a post-decide,
>    pre-apply transform on the ground-truth log, so ALL emitters and
>    all three apply sites see one mutated truth — correct from the
>    tree if the pipeline disagrees); (iii) lineage: parent run,
>    operator, seed recorded per mutated corpus, fixed draw
>    consumption on the mutation stream per the opt-in-key law
>    (absent = byte-identical); (iv) the oracle loop: inject class X,
>    expect check finding class X and nothing else — the closed loop,
>    stated as the acceptance test; (v) explicitly out of scope:
>    lowering-layer faults (file operators keep them), foreign-corpus
>    mutation (future-features). Present every genuinely open design
>    choice as LETTERED OPTIONS with a recommendation — the author
>    rules before any implementation session. Commit the ADR per the
>    docsgen/ADR-index conventions. Gate: ADR-index green.
> 3. THE RIDER — docs/future-features.md (author-ruled 2026-08-30):
>    consumer-voiced menu framed as a torture-kit for HL7 handling
>    systems across three fault layers — CONTENT (bytes wrong inside
>    a message: this row's event mutation; foreign-corpus span-splice
>    mutation with differential-judge oracle and parent-hash lineage),
>    STREAM (sequence wrong, messages intact: drop/duplicate/reorder/
>    delay injector, a pure function on emitted sequences), TRANSPORT
>    (MLLP framing/truncation/resets). State the layer-boundary
>    semantic that makes the kit coherent: a duplicate EVENT means the
>    world had two occurrences (receiver must keep both); a duplicate
>    MESSAGE means the world had one (receiver must dedupe). One-line
>    design stance per entry; NO internal sizing. Link it: one "what
>    it doesn't do yet" line in README, one sentence at
>    consuming-ground-truth.md's exclusions. Gate: suite green;
>    state-derived LAST.
> 4. Push; CI via gh; close marker. Record: the ADR's lettered
>    questions surfaced verbatim for the author's ruling.
> FENCES: no src edits anywhere; no operator implementation; the ADR
> proposes, the author disposes.
> SELF-ARCHIVE: prompt and record in the final push.

A single mid-session instruction followed, verbatim: *"Always use WSL
clone. Never use Windows clone."* Already the case — the Windows clone
was read once at session start to observe it was stale, and never
written.

## Deviation record

**D1 — the ADR's Q1 recommendation contradicts the prompt's own stated
channel expectation.** The prompt licensed exactly this
(*"correct from the tree if the pipeline disagrees"*), and the tree
disagrees on four independent readings, set out in the record's section
3(a) and in the ADR's own section 2(ii) and Q1. Recommended instead: a
post-run, whole-log stage outside `engine/run`. The prompt's stated
requirement — every emitter inherits one mutated truth — is met by that
shape and met more cleanly. **This is a recommendation in a Proposed
ADR, not a decision**; Q1 letters all three readings so the author rules
on the evidence.

**D2 — nine lettered questions, where the prompt named five design
components.** Q6 (an operator the catalog cannot convict) and Q9 (whether
a mutant stays schema-valid) were not anticipated by the prompt and came
out of the probe: Q6 because two precedents in this repository point
opposite ways, Q9 because the event schema's `:int` versus `[:maybe
:int]` split decides which cells of the operator matrix can exist at
all. Both change what gets built, so both are lettered rather than
absorbed.

**D3 — "fixed draw consumption on the mutation stream per the
opt-in-key law" is satisfied vacuously under the recommended shape, and
the ADR says so rather than manufacturing a key.** A stage outside `run`
has no `engine/config-keys` entry to be absent from, so byte-identity is
structural. `:mutation` is nonetheless proposed as a RESERVED RNG family
tag 6, unused, on ADR-0171's own `:person` precedent, so a later session
that does want a run-seed-derived mutation stream adds rows rather than
re-keying the table.

**D4 — no oracle run.** No `components/*/src` file changed, so
`bin/regression-oracle` and `bin/ground-truth-bracket` would have
reported IDENTICAL for structural reasons and said nothing. Declared
rather than run, per the standing preference for measurement over
ceremony.
