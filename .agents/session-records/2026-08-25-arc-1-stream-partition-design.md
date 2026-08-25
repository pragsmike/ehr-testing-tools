# 2026-08-25 — arc 1: stream-partition design ADR

## Scope

Traffic-scale arc 1 (`roadmap.md#stream-partition-design`, PRIORITY 2)
under `rulings.md#R-per-person-streams-before-generator-fixes`. A design
ADR, derived from the tree, plus one de-scaffold deletion. Payload
session under the moratorium; no `components/*/src` or
`components/*/test` change, and nothing re-pinned.

Delivered:

1. **Step 0** — `probe` deleted from `.agents/skills/` and its
   `.claude/skills/` mirror, own commit (`c1b996e`), author ruling
   2026-08-25 "Delete probe".
2. **Steps 1–4** — `notes/adr/0171-arc-1-rng-stream-partition-design.md`
   (Proposed): the draw-site census, the partition scheme with rejected
   alternatives, the migration test obligations, and six rulings as
   lettered options with a recommendation each. Roadmap row updated;
   `notes/ADRs.md` and `.agents/state-derived.md` regenerated.

## Red→green evidence highlights

A docs-only session; the proof is the suite staying green and the
generated surface staying fresh.

* Eight docs-tooling gates run locally before the Step 0 commit —
  `skill-mirror-currency`, `readme-presence`, `stale-path`,
  `state-derived`, `index-completeness`, `structure-currency`,
  `link-footnote-gate`, `citation-gate`: **44 tests, 594 assertions, 0
  failures**.
* `make state-derived` run twice (once per commit) and `make adr-index`
  once, so CI's freshness diff has nothing to catch. ADR count 168 → 169.
* Every `file:line` in ADR-0171 was verified against this clone by
  reading the cited line back; five citations drafted from a first pass
  were wrong by 1–5 lines and were corrected before commit
  (`engine.clj` loop 1709→1707, decide call 1729→1728, `init-queue`
  1656→1657, `registered-steps-for` 1653→1656, `weighted-pick`
  1328→1329, `churn.clj` `roll-gap` 159→154).

## Judgment calls and their ratification status

**Unratified — the six rulings ADR-0171 opens (A–F).** Each carries a
recommendation: A1 reuse `engine.clj`'s existing `mix64`; B1 mix the
`(parity, within-delivery)` pair from the start with the second pinned
at 0; C1 emission joins as a fifth family; D1 a top-level
`:stream-scheme` string, sibling of `:event-schema-version`; E1
FACILITY stays distinct from WORLD; F1 migrate in one session. Nothing
is executable until these are answered, and the roadmap row says so.

**Made in-session, disclosed:**

* **The locality property test cannot assert total byte-identity, and
  the ADR says so rather than promising it.** `engine.clj:480`
  (`bed-ready-location`, called from `decide :discharge`) draws to place
  a *different* patient, and `:bed-swap`/`:merge` pick partners from the
  whole population under a `(seq eligible)` guard. LOCALITY is therefore
  specified over PATIENT-scoped fields with the four WORLD sites
  excluded by name. A test written to the prompt's plainer wording would
  have been false at the first churned seed.
* **The `from` == `to` skip is scheduled INTO the migration commit, not
  before it** — the prompt's own instruction, restated in the ADR with
  the cost arithmetic (four gated corpora, one engine fixture, two
  conformance baselines, fourteen trace captures per reshuffle).
* **AGENTS.md's `.agents/skills/` bullet was rewritten to point at the
  README instead of re-listing the roster.** It enumerated ten skills
  and named seven that no longer existed. Fixing it by re-enumerating
  would have reproduced the failure; the pointer form is what ADR-0158
  did to the generated-surface bullet eight lines above it.

## Findings and HEAD landed

Nine premise mismatches between the session prompt and the tree are
recorded in ADR-0171 §1f, per fix-forward-with-disclosure (ADR-0001
R10). The four that changed the work:

* **`decide :discharge` draws.** A mechanical pass over `defmethod`
  bodies reports it draw-free; the draw is in the helper
  `bed-ready-location` between `:transfer` and `:discharge`. This is the
  row LOCALITY is most exposed to.
* **`:seeds` cannot hold the stream marker as typed.**
  `provenance/manifest.clj:96` is `[:map-of :keyword :int]` — `:int`
  values only. Ruling D exists because of this.
* **`:seeds` already carries two incompatible vocabularies**: sim writes
  `{:primary seed}`, corpus writes `{:master seed :clinician ...}` where
  `:clinician` is Synthea's own `-cs` flag, not a sim stream.
* **The defspec premise does not hold.**
  `rulings.md#R-defspec-seed-policy` is "seeds stay unpinned repo-wide";
  exactly **3** of **83** `defspec`s pin a `test.check` seed, all
  `20260825`, all arc-0 equivalence properties. A `test.check` seed pins
  generator *sampling*, not generator *output* — those three must stay
  green with **no re-pin**, and a red there is a finding. The obligation
  is restated in the ADR as one unpinned sweep, not a defspec re-pin.

Two figures the prompt named that the tree does not yield, disclosed
rather than repeated:

* **"352 test blocks"** matches nothing. Counted: **1,866**
  `deftest`/`defspec` blocks across **193** test `.clj` files
  (state-derived's generated **190** `*_test.clj` namespaces plus three
  fixture/helper files); **222** literal `:seed N` call sites across
  **52** test files.
* **`pick` (engine.clj:631)** is named `uniform-choice`, defined at
  `:629`. The line was right; the name was not.

Two stale citations found in live source and **not fixed** — the fence
forbids touching `components/*/src`, and both are one line each here per
the de-scaffold ruling's finding rule:

* `emit_hl7.clj:962` cites `engine.clj:1165-1183` for `assign-pathway`'s
  worked example; after arc 0 it is at `:1349`.
* `docs/dev/simulator-architecture.md:157` cites the RNG-path law as a
  rulings row titled "Measurements sample the claimed population,
  standing," AR-RL2-2. No such row exists; the live slug is
  `rulings.md#R-measure-claimed-population` (ADR-0093).

One ride-along already stale at `252fbeb`, fixed: AGENTS.md's skills
roster (above). `scenarios/SKILL.md`'s `/committee` and `/review` rows
remain dangling and were left alone — out of this session's scope,
named here.

**One in-session correction to my own arithmetic, caught after the ADR
commit and fixed in a follow-up.** §2(b)'s collision figure read
~2.7e-7 where the stated formula (n^2/2^65 at n = 10^6) gives
**~2.7e-8** -- a wrong exponent under a right method. Both figures now
carry their arithmetic inline (10^12 / 3.689e19, and 10^12 / 8.590e9 for
the rejected 32-bit `hash` alternative's ~116) so the number cannot be
read without the check that produces it. The comparison the paragraph
rests on is unchanged and was never in doubt: the 32-bit alternative is
worse by a factor of 4.3 billion.

HEAD landed: `c1b996e` (probe deletion), `906dd72` (ADR-0171), plus the
erratum commit above. No tag (de-scaffold ruling); CI green at the tip is
the marker.
