# Charter — `judge-fhir-official`

> **Draft for the author's edit.** Derived from
> `src/ehrt/judge_fhir_official/interface.clj`, `fhir.clj`, and the
> ADRs their docstrings cite. **UNCLEAR** marks a contract the shipped
> surface does not settle.

## 1. Mission

Judge FHIR resources with the **official HL7 FHIR validator** — the
FHIR-side engine of the three, and the only one that reaches `judge`'s
own vocabulary directly.

Extracted from `ehrt.judge.interface` at ADR-0011, the per-engine judge
split (`ehrt.judge.fhir` → `ehrt.judge-fhir-official.fhir`).

## 2. Interface contract

- `gate-file` — judge one file.
- `gate-dir` — judge every resource under a directory.
- `gate-batch` — judge a batch. The one batching affordance in the
  judge family; neither v2 engine has a counterpart (see UNCLEAR-FO1).

All three are **unqualified**. The `ehrt.judge.interface`-era
`fhir-gate-file`/`fhir-gate-dir`/`fhir-gate-batch` qualification
existed only to disambiguate against `ehrt.judge.v2`'s own
`gate-file`/`gate-dir` at **one shared interface** (ADR-0002/ADR-0008);
now that each engine has its own interface, **there is nothing left to
qualify against here.** The `tools` façade re-applied the `fhir-*`
qualification at its re-export layer until stage 3 dissolved those
relays (ADR-0018).

## 3. Data shapes owned

- The **official-validator binding**: how the HL7 FHIR validator's
  output becomes a `judge` finding, and how its severities map onto
  `judge`'s severity order.

Verdict shapes themselves — `Report`, findings, `worst-of` — are
`judge`'s, and this engine reaches them through
`ehrt.judge.interface` (`fhir.clj:31`).

## 4. Invariants guaranteed

- **Verdicts speak `judge`'s vocabulary.** This is the one engine for
  which the shipped surface makes that structurally visible: it
  genuinely calls `judge`'s `worst-of` and four `verdict-cache`
  functions, and those calls are the reason those five vars are on
  `judge`'s interface at all.
- **Cross-brick reach goes through the interface.** Those five calls
  were legal before the split (same brick) and illegal after; ADR-0011
  fixed that **by routing through `judge`'s interface rather than
  narrowing the call away** — a fact found by actually running
  `poly check` after the move, not by static review.
- **Verdicts are cacheable**, via `judge`'s verdict cache, keyed by
  `verdict-cache-key`.
- **Its names are stable at this seam**: the `fhir-*` forms are
  retired.

## 5. Non-goals

- **No HL7 v2.** Both v2 tiers are other bricks — `judge-v2-hapi`
  (base-structural) and `judge-v2-nist` (profile).
- **Defines no shared verdict vocabulary**; it consumes `judge`'s.
- **Owns no corpus** and does no sampling.
- **Does not vendor the validator.** The official validator is a
  fetched artifact, resolved through `kernel`'s artifact cache.

## 6. Forbidden edges

Requires `judge` and `kernel` in `src` — and it is the **only** judge
engine that requires `judge`.

Must never require:

- **`judge-v2-hapi`** or **`judge-v2-nist`** — the three engines are
  siblings; an edge between them would defeat the per-engine split.
- **`corpus`** — no judge engine is a corpus dependency any more
  (ADR-0018). Note the direction that *does* exist historically:
  `corpus` used to relay this engine's gates, and stage 3 removed
  that.
- **`bases/cli`**, **`sim`** and every simulator brick.

## UNCLEAR — the author's review queue

- **UNCLEAR-FO1 — `gate-batch` exists only here.** Whether batching
  is intrinsically a FHIR affordance (a Bundle is already a batch, so
  the operation means something specific) or is a general capability
  the two v2 engines simply never grew, is not settled by any of the
  three seams. Raised from the other side on both v2 charters.
- **UNCLEAR-FO2 — the verdict cache is reachable by one engine.**
  `judge` exports four `verdict-cache-*` functions, and the seam
  records that they are there because *this* engine calls them. So
  caching is available to the FHIR tier and, as far as the shipped
  surfaces show, unused by either v2 tier. Two readings: *(a)*
  deliberate — official-validator runs are expensive in a way HAPI and
  NIST runs are not, so caching is worth its complexity only here;
  *(b)* incidental — the cache landed with this engine and nobody has
  wired the others. The cost asymmetry makes (a) plausible, but
  nothing on the seam states it.
