# Charter — `judge-v2-hapi`

> **Draft for the author's edit.** Derived from
> `src/ehrt/judge_v2_hapi/interface.clj`, `v2.clj`, and the ADRs their
> docstrings cite. **UNCLEAR** marks a contract the shipped surface
> does not settle.

## 1. Mission

Judge HL7 v2 messages at the **base-structural tier**, backed by HAPI:
does the message parse, and is it structurally well-formed against the
base standard.

Extracted from `ehrt.judge.interface` at ADR-0011, the per-engine judge
split (`ehrt.judge.v2` → `ehrt.judge-v2-hapi.v2`).

## 2. Interface contract

- `gate-file` — judge one file, returning its verdict.
- `gate-dir` — judge every message under a directory.

Note what the seam does **not** say: this component's `src` requires
only `kernel` (`v2.clj:42`), **not `judge`** — so whether its verdict
is already a `judge` `Report` is not visible here. See UNCLEAR-VH3.

Both are **unqualified**, and that is a deliberate un-doing rather
than an accident: the `ehrt.judge.interface`-era
`v2-gate-file`/`v2-gate-dir` qualification existed **only** to
disambiguate against `ehrt.judge.fhir`'s own `gate-file`/`gate-dir` at
**one shared interface** (ADR-0002/ADR-0008). Now that each engine has
its own interface, **there is nothing left to qualify against here.**
The `tools` façade re-applied the qualification at its own re-export
layer until stage 3 dissolved those relays (ADR-0018); consumers call
these names directly now.

## 3. Data shapes owned

- The **HAPI engine binding**: how a HAPI parse failure becomes a
  `judge` finding.

It owns no verdict vocabulary — `Report`, findings and severity are
`judge`'s.

## 4. Invariants guaranteed

- **One tier, honestly named.** This engine answers base-structural
  questions only. It is a **complementary gate, not a replacement**
  for the profile tier (`judge-v2-nist`), and the two are siblings
  rather than alternatives.
- **Its names are stable at this seam.** The qualification history
  above is settled: `gate-file`/`gate-dir` are the names, and the
  `v2-*` forms are retired.

## 5. Non-goals

- **No profile-tier checking.** Profile usage, cardinality, length,
  conformance statements, co-constraints, slicing and value-set
  bindings are what this tier **structurally cannot** check — that is
  `judge-v2-nist`'s brief, by that engine's own statement.
- **No FHIR.** That is `judge-fhir-official`.
- **Defines no shared verdict vocabulary.** `Report`, findings and
  severity are `judge`'s.
- **Owns no corpus** and does no sampling.

## 6. Forbidden edges

Requires exactly `kernel` in `src` — and notably **not `judge`**,
unlike its `judge-fhir-official` sibling.

Must never require:

- **`judge-v2-nist`** or **`judge-fhir-official`** — the three engines
  are siblings; an edge between them would defeat the per-engine split
  and make one engine's availability depend on another's.
- **`corpus`** — `corpus` no longer requires any judge engine at all
  (ADR-0018), and the reverse edge would reintroduce the coupling that
  removal bought.
- **`bases/cli`**, **`sim`** and every simulator brick.

## UNCLEAR — the author's review queue

- **UNCLEAR-VH1 — this seam has no `gate-batch`, and its FHIR sibling
  does.** `judge-fhir-official` exports `gate-file`, `gate-dir` **and**
  `gate-batch`; this engine exports the first two. Two readings:
  *(a)* deliberate — batching is a FHIR-side concern (a bundle is
  already a batch), and an HL7 v2 directory scan is what `gate-dir`
  already is; *(b)* an asymmetry no one has needed to close yet. The
  same question applies to `judge-v2-nist`, which also has no
  `gate-batch`.
- **UNCLEAR-VH3 — two of the three engines do not require `judge`.**
  `judge`'s charter says its mission is the verdict vocabulary every
  engine reports in. But only `judge-fhir-official` requires
  `ehrt.judge.interface` in `src` (`fhir.clj:31`); this engine and
  `judge-v2-nist` require **only `kernel`**. Two readings: *(a)* these
  two engines return a plainer shape and something above them — the
  CLI, or a project test tree — lifts it into a `Report`, in which
  case `judge`'s "vocabulary every engine reports in" describes an
  aspiration rather than the wiring; *(b)* they do produce Reports,
  built from plain data without needing the constructors, in which
  case the shape is duplicated knowledge with no gate holding it to
  `Report`'s schema. This is the sharpest structural question the
  judge family raised, and the charter does not pick.
- **UNCLEAR-VH2 — no `make-validator`, unlike the NIST engine.**
  `judge-v2-nist` exports `make-validator` because it must be handed a
  profile bundle. This engine exports no constructor, implying its
  validator is ambient or built per call. Whether a caller can
  configure the HAPI validator at all — a version, a table set — is
  not answerable from this seam.
