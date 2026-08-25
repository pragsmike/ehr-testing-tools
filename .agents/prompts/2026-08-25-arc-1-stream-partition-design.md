# Prompt archive — 2026-08-25, arc 1 stream-partition design

Archived per `.agents/prompts/README.md`. Pasted into a Claude Code
session at `252fbeb`; two commits (Step 0's `probe` deletion, then the
ADR), no tag, CI green at the tip is the close marker (de-scaffold
ruling, 2026-08-25). Paired record:
[`2026-08-25-arc-1-stream-partition-design.md`](../session-records/2026-08-25-arc-1-stream-partition-design.md).

Nine premise mismatches between this prompt and the tree are recorded in
ADR-0171 §1f and summarised in the session record — the prompt's `pick`
is `uniform-choice`, `:seeds` is `:int`-typed and cannot hold the marker
it proposes, `decide :discharge` draws through a helper, the defspec
re-pin premise does not hold under `R-defspec-seed-policy`, and "352
test blocks" matches no figure the tree yields.

## The prompt, verbatim

> Session prompt -- arc 1: stream-partition design ADR (traffic-scale program)
>
> Context. HEAD 252fbeb. Roadmap `[stream-partition-design]` PRIORITY 2 and
> plan `.agents/plans/2026-08-24-traffic-scale-program.md` "Arc 1" charter this
> session: a DESIGN ADR, no engine code. Governing ruling:
> `R-per-person-streams-before-generator-fixes` (rulings.md :306). The
> problem: one `java.util.Random` (engine.clj `run`, `(Random. ^long seed)`)
> feeds every decide for every patient, so any change to any draw reshuffles
> the whole corpus (Q3). Arc 0 proved the generator output-identical at 10^5
> events (ADR-0169); this arc designs the partition that lets arcs 2-4 change
> draws for one person without moving anyone else. Payload session under the
> moratorium; the ADR is the deliverable, and it must be derived from the tree.
>
> Read first: the plan's Arc 1 + Arc 2 paragraphs; ADR-0168 and ADR-0169;
> `notes/sim/ADRs.md` ADR-0009 (seed stability is within-version -- your
> version marker rides that policy); `engine.clj` `run` (the Random seeding and
> the fold), `decide :delay` (:414 -- `(rand-int-in rng from to)` draws even
> when from==to), `rand-int-in` (:223), `pick` (:631), the two documented
> always-consume-one-draw sites (:1360-1395); `churn.clj` :161-180 (the
> shared-RNG docstring is a named site); `components/sim/src/ehrt/sim/run.clj`
> :326-332 (the emit-latency stream is ALREADY a second, independently
> seeded Random -- precedent for partition); `provenance/manifest.clj` :65,
> :96 (`:seeds` is a `[:map-of :keyword :int]` -- the marker's home).
>
> Step 0. Delete `.agents/skills/probe` and `.claude/skills/probe` (author
> ruling 2026-08-25, "Delete probe"); update skills README. Own commit.
>
> Step 1. Draw-site census, from the tree. `grep -rn` every `.nextInt`,
> `.nextDouble`, `.nextLong`, `rand-int-in`, `pick`, and every `Random.`
> construction under `components/*/src`. Expect sites in: sim-engine
> (engine.clj, churn.clj), patient-simulator (gmf_interpreter.clj,
> census.clj), sim-model (persona.clj, facility.clj, config.clj),
> sim-emit-hl7 (emit_hl7.clj :908 fixed-seed providers, :984 latency), sim
> (run.clj), oracle (digest.clj -- test-fixture RNG, out of scope; say so).
> Table, one row per site: file:defn, what is drawn, whose outcome it
> decides (PATIENT / PERSON / WORLD / FACILITY / EMISSION), draw count
> fixed or data-dependent, and whether it currently reads the shared engine
> Random. This table IS the ADR's evidence; every later claim cites a row.
>
> Step 2. The scheme. Propose, with the alternatives you rejected:
> (a) stream families: per-patient, per-person (arc 2's newborns and
> households), world (arrivals, churn gaps, facility), emission; (b)
> derivation: master seed x family x stable id -> stream seed (SplitMix64 or
> `(Random. (hash ...))` -- state the mixing function and why collisions
> don't matter at 10^6); (c) newborns: seed from (master, parent person-id,
> birth ordinal) so a birth never reshuffles the world -- state what
> "ordinal" is when twins are excluded (plan lean); (d) from==to delay-skip:
> zero draws when from==to -- this is DRAW-AFFECTING, so it lands WITH the
> migration, never before; (e) provenance: `:seeds {:master n :stream-scheme
> <version-keyword>}` and what ADR-0009 says the marker licenses; (f) what
> `churn.clj`'s docstring argument (shared by design) becomes.
>
> Step 3. Migration test obligations, as test names + one-sentence
> assertions, no code: LOCALITY (mutate one patient's stream seed; every
> other patient's event subsequence byte-identical; the world stream
> untouched); DETERMINISM CONTINUITY (the existing defspecs' pinned seeds
> re-pin once under the new scheme, with a declared-oracle-change and one
> sweep); WITNESS COUNTS (`R-witness-population-is-counted`, now a gate in
> run_test.clj: the locality test asserts how many patients it moved);
> GATED-CORPORA RE-PIN (the four arc-0 fixtures move once; the F3 tripwire
> stays). State the expected blast radius: which of the 352 test blocks
> carry pinned outputs (grep `pinned`, the arc-0 fixtures, oracle roots).
>
> Step 4. ADR-0171 (confirm number free), Proposed status, sections: census
> table; scheme with rejected alternatives; rulings needed as lettered
> options with a recommendation each (at least: mixing function; newborn
> ordinal; whether emission joins the scheme or keeps run.clj's precedent;
> whether facility/provider draws are WORLD or FACILITY; migration in one
> session or two). Roadmap row: one line "design landed, ADR-0171, awaiting
> rulings". Session record one page. Push; CI is the gate. No tag.
>
> Fences. No change under `components/*/src` or `components/*/test`. No
> re-pinning of anything. Every file:line in the ADR is from your own
> clone at 252fbeb or later. If a site above is not what it is described
> as, say so in the census and in the record; do not describe around it.
