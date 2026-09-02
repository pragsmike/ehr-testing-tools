# Charter — `judge-v2-nist`

> **Draft for the author's edit.** Derived from
> `src/ehrt/judge_v2_nist/interface.clj`, `v2.clj`,
> `docs/nist-mirror.md`, and the ADRs their docstrings cite.
> **UNCLEAR** marks a contract the shipped surface does not settle.

## 1. Mission

Judge HL7 v2 messages at the **profile tier**, backed by NIST: check a
message against an **IGAMT-exported conformance-profile bundle** — the
things the base-structural tier structurally cannot see.

Third judge engine, sibling of `judge-v2-hapi` (base-structural tier)
and `judge-fhir-official`. Direct-engine adoption at ADR-0012.

## 2. Interface contract

- `make-validator` — builds a validator from a conformance-profile
  bundle (Π). **`PROFILE.xml` is required**; `CONSTRAINTS.xml`,
  `VALUESETS.xml`, `VALUESETBINDINGS.xml`, `COCONSTRAINTS.xml` and
  `SLICINGS.xml` are optional. This constructor exists because, unlike
  the HAPI tier, this engine is meaningless without a profile.
- `gate-file` — judge one file against that validator.
- `gate-dir` — judge every message under a directory.

`gate-file`/`gate-dir` are **unqualified** for the same reason as
`judge-v2-hapi`'s: per-engine interfaces have nothing to qualify
against. The `tools` façade applied its own `v2-nist-*` qualification
at its re-export layer until stage 3 dissolved those relays (ADR-0018).

## 3. Data shapes owned

- The **conformance-profile bundle (Π)**: which files it must and may
  contain, and how they are read.
- The **validator** `make-validator` returns.
- The **msg-id contract** (ADR-0012) — how a message is identified in
  a NIST verdict.
- This engine's own **Cause** growth (ADR-0012): the finding causes
  the profile tier can report and the base tier cannot.

This component's `src` requires only `kernel` (`v2.clj:52`), **not
`judge`** — so whether its verdict is already a `judge` `Report` is
not visible at this seam. See UNCLEAR-VN3.

## 4. Invariants guaranteed

- **Complementary, not a replacement.** Stated on the seam itself
  (ADR-0012): this engine checks profile usage, cardinality and
  length, conformance statements, co-constraints, slicing, and
  value-set bindings — **what the HAPI tier structurally cannot** — and
  does not displace it.
- **`PROFILE.xml` is required; the other five are optional.** A bundle
  missing the required file is not a degraded validator, it is not a
  validator.
- **The engine is used directly** (ADR-0012), not through a
  reimplementation of its logic.

## 5. Non-goals

- **No base-structural tier.** That is `judge-v2-hapi`, and this
  engine does not replace it.
- **No FHIR.** That is `judge-fhir-official`.
- **Defines no shared verdict vocabulary.** `Report`, findings and
  severity are `judge`'s.
- **Does not author or vendor profiles.** It consumes an
  IGAMT-exported bundle; `docs/nist-mirror.md` and `bin/mirror-nist` /
  `bin/verify-nist-lock` cover how that material is obtained and
  pinned.

## 6. Forbidden edges

Requires exactly `kernel` in `src`.

Must never require:

- **`judge-v2-hapi`** or **`judge-fhir-official`** — the three engines
  are siblings. This one in particular must not depend on the HAPI
  tier merely because both judge v2: the whole claim that they are
  *complementary tiers* rests on each standing alone.
- **`corpus`** — no judge engine is a corpus dependency any more
  (ADR-0018).
- **`bases/cli`**, **`sim`** and every simulator brick.

## UNCLEAR — the author's review queue

- **UNCLEAR-VN1 — the bundle's own validity is not a seam concern.**
  `make-validator` is documented by which files are required and
  optional, but the seam does not say what it returns when
  `PROFILE.xml` is absent or malformed — a `kernel` `rejected`, an
  `error`, a throw, or a validator that fails every message. Given
  this is the one constructor in the judge family that can fail on
  *configuration* rather than on the message under test, it is the
  place a caller most needs the answer.
- **UNCLEAR-VN3 — this engine does not require `judge`.** Its `src`
  reaches only `kernel`, as `judge-v2-hapi`'s does, while
  `judge-fhir-official` requires `ehrt.judge.interface` directly.
  Whether these two engines therefore return a plainer shape that
  something above lifts into a `Report`, or build Reports from plain
  data with no gate holding them to the schema, is not answerable from
  any of the three seams. Raised identically on `judge-v2-hapi`'s
  charter (UNCLEAR-VH3); it is one question, not two.
- **UNCLEAR-VN2 — no `gate-batch`, matching `judge-v2-hapi` and
  differing from `judge-fhir-official`.** Whether batching is a
  FHIR-only affordance or an unclosed asymmetry across the three
  engines is not settled by any of the three seams. Raised identically
  on `judge-v2-hapi`'s charter.
