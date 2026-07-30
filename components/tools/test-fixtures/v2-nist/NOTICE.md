# Notice — NIST v2-validation Π fixture (`COVID19_ELR-v2.3.1`)

This directory holds a third-party artifact, not fixtures this repo
authored: a conformance-profile bundle (Π) and one companion ER7 test
message, vendored from CDC's own test resources.

## Upstream

| | |
|---|---|
| Project | `CDCgov/lib-hl7v2-nist-validator` |
| Repository | `https://github.com/CDCgov/lib-hl7v2-nist-validator` |
| Pinned commit (clone HEAD at retrieval) | `eeac90c5f88dca3018992005232acdf3da644d88` |
| Source paths | `src/test/resources/COVID19_ELR-v2.3.1/` (bundle), `src/test/resources/covidELR/` (message) |
| License | Apache License 2.0 (repo-root `LICENSE`, "Copyright 2024 CDC.gov") |
| Retrieved | 2026-07-30 |

## Files and sha256

| File | sha256 |
|---|---|
| `COVID19_ELR-v2.3.1/PROFILE.xml` | `5a709a2f719b2aa3ae900afba600f31e087ff3ee5a87bb550794f6b635fe4704` |
| `COVID19_ELR-v2.3.1/CONSTRAINTS.xml` | `9ed06afd7dc8fe2d0a2f418b28f15d7e0788a1259f570b14ece8911ee1dea0ee` |
| `COVID19_ELR-v2.3.1/VALUESETS-disabled.xml` | `a2f7c1c8242386edada70493b1563f9a2c85e9ebb12c7f3a20d97ef10edfa3e8` |
| `covidELR/231HL7TestFilewithHHSData.txt` | `83d3241d68e474f2ce1cf759a4379614447a67154ea9ce2dc6a9f36466d574fc` |

`VALUESETS-disabled.xml` is committed under that exact name, as shipped
upstream — not renamed, not "fixed" to `VALUESETS.xml`. The disabled
naming is load-bearing: `judge-v2-nist.v2/make-validator` only wires a
`:value-sets` artifact when a file matching `bundle-files`' own
`VALUESETS.xml` (or the `VALUESETBINDINGS.xml`/`VALUSETBINDINGS.xml`
spelling-drift pair) is present under that literal name, so this bundle
deliberately validates with value-set checking absent — the fixture the
`:terminology-suppressed` no-verdict path needs. No `VALUESETBINDINGS.xml`,
`COCONSTRAINTS.xml`, or `SLICINGS.xml` ships in this profile at all
(upstream's own layout, not a selection this repo made).

## Stand-in status

This is a stand-in, not this project's own conformance profile: until a
project-authored IGAMT export replaces it, `judge-v2-nist`'s
engine-in-the-loop tests validate against CDC's own profile bundle,
chosen because it is a real, upstream-shipped, Apache-2.0-licensed Π
fixture rather than a hand-authored one. `notes/facts-register.md` F8
(the IGAMT registration disclaimer, captured verbatim 2026-07-29) is the
notice obligation that will apply to a *future*, project-authored,
NIST-hosted-IGAMT-exported profile bundle — the redistribute/modify
clause requiring a derived-from notice on export, and a modified notice
if edited. This fixture is CDC's own vendored test resource, not an
IGAMT export this project produced, so F8's own clause does not attach
to it directly; it is cited here only so a future session replacing this
stand-in with a real export knows where that obligation is recorded.

## Framing — do not let git translate newlines

`covidELR/231HL7TestFilewithHHSData.txt` is ER7 wire format: segments
are `\r`-terminated, not `\n`. `.gitattributes` carries
`components/tools/test-fixtures/v2-nist/covidELR/*.txt -text` for
exactly the reason `components/tools/test-fixtures/v2/*.hl7` and
`components/sim/docs/demos/**/messages*.txt` already do — a checkout
that normalized line endings would silently rewrite those bytes and
invalidate the engine-in-the-loop test's own pinned finding counts.

## Consumers

`components/tools/test/ehrt/tools/corpus/v2_nist_engine_test.clj` (or
this fixture's actual test-namespace name, see that file directly) —
the engine-in-the-loop test that builds a validator from this bundle
via `ehrt.judge-v2-nist.interface/make-validator` and gates the
companion message through it, pinning the measured verdict, cause, and
per-area finding counts.
