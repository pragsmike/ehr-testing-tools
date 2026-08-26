# Session prompt -- arc 3a: the engine folds the person stream (design ADR-0173)

Archived verbatim. Drove the session recorded at
`.agents/session-records/2026-08-25-arc-3a-demographic-fold-design.md`.

---

Context. HEAD ee573c4. `components/person-simulator` exists and nothing
calls it (ADR-0172, F1 proven: oracle IDENTICAL). Arc 3 makes the engine
consume it -- the first EXPECTED reshuffle since ADR-0171. The plan's
arc-3 paragraph bundles three folds (demographic timeline; scheduling
state R-mix-5; bed-status cycle R-mix-6). This session designs ONLY the
first: ADR-0173, the demographic fold plus the two hooks and the
identification flow. Scheduling and bed-status are arc 3b, not touched
here. No engine code; rulings come back lettered. Payload session.

Author statement 2026-08-25, verbatim, binding on the design: "our
population does include unhoused people showing up at, say, ED; and
unhoused unresponsive John Does." So: an unhoused person has a residence
STATE, not a `places.edn` row; an unresponsive arrival opens an
`:identity-unavailable` window with NO usable demographics; both must
reach the wire (PID with a placeholder name and no address) and both must
resolve later per G1 (fill or merge) or not at all.

Ride-alongs, docs-only, own commit first: (1) ADR-0172 status block gains
one line: "`t0` is the t0 CONTEXT map (`:master`, `:death-t`, persona
config), not an instant -- C1's 'as a t0 parameter' read literally"; (2)
limitations row 12: "a parent unhoused at delivery who later forms a
household heads two" with the guard 2b's record names; (3) a docs-tooling
gate `every-brick-test-path-is-composed-into-a-project`: for each
`components/*/test` and `bases/*/test`, at least one `projects/*/deps.edn`
composes the brick (2b found `poly test project:development` exits 0 on a
brick composed only into root aliases -- W-1's third shape). Born green.

Read first: ADR-0172 section 1 (the 31-site census -- your fold's
consumers), section 2 (event kinds, hooks, G1), section 4; `engine.clj`
`decide :registered` :483-540 (Persona sampled at :493 from the `:patient`
stream -- the seam this arc replaces), `patient-id-for` and `mix64`
(:225-260: patient id is minted from the arrival ordinal -- a returning
PERSON must resolve to the SAME patient, which this cannot do today),
`init-world` and `run` (the fold, :reinstate-index / :citation-index as
the carried-state precedent), `churn.clj` :60-135 (`:merge` today: two
engine-minted patients merged by lottery -- arc 3's identification merge
must compose with, not duplicate, it); `emit_hl7.clj:302`
`personas-by-patient-id` (ONE lookup shape, keyed by patient id --
becomes state-at-t); `check.clj` referential family (ADR-0163/0166 shape)
and `traffic-model.md` :18-30.

Step 1. Census the seam from the tree: every read of `:persona` or a
Persona field in engine/emit/check/provenance (ADR-0172 section 1 is the
list; re-derive, it is a week old), tagged: t0-only / state-at-event /
state-at-render. Every draw the `:patient` family loses when Persona
moves to the person (the 13 persona draws at :493) -- name the sites, so
the reshuffle is predicted, not discovered.

Step 2. The design. (a) Population: `run` calls `person-simulator/persons`
once for a configured pool (`:persons {:count n}`); arrivals SELECT a
person -- from which family, and how a person's repeat arrival maps to
the same patient/MRN (a `:person-id -> :patient-id` fold index; the first
arrival mints, later ones resolve). (b) Fold: person events applied to
world state in t-order interleaved with the engine's own -- residence,
coverage, identity-correction, death (`:person-death` for a known patient
is NOT a wire event, row 4) -- so `:registered` and every later event read
state-at-t. (c) Hooks: `:delivery` -> admission for the parent + a
newborn `:registered`; `:occupational-injury` -> ED arrival cause. (d)
Identification (G1 + the author statement): `:identity-unavailable` at
arrival -> placeholder registration (engine mints: alias name, no
address, new MRN); `:identity-resolution :fill` -> demographics filled in
place, same MRN; `:merge` -> the placeholder MRN merged into the person's
existing patient via the churn `:merge` path (same event shape, so
check.clj's merge invariants and the post-merge-shadow surface apply
unchanged). Unhoused residence state renders as PID-11 absent. (e) New
invariant family in check.clj: person-event referential (resolution
references its window; fill/merge reference the placeholder; a merge's
survivor is the person's prior patient; no demographics after death).
(f) Provenance stamps the persons config and pool size.

Step 3. Rulings, lettered with a recommendation: (A) arrival selection --
uniform from pool via WORLD, or person-side arrival propensity (v1: WORLD;
propensity is a hazard the charter did not draw); (B) placeholder MRN --
fresh MRN merged later (composes with churn) vs provisional MRN
overwritten (no merge event); (C) does `:person-death` for a person with
no compiled death ever reach the wire (row 4 says no; confirm or lift);
(D) whether arc 3a's execution re-pins with one declared oracle sweep or
lands the pool at `:count 0` first (corpus-identical) and turns it on in a
second commit (two sweeps, cleaner blame); (E) unhoused rendering: PID-11
absent vs a sentinel string -- check what real ED registration systems
emit and cite it. ADR-0173 Proposed; roadmap row one line; record one
page; push; CI is the gate; no tag.

Fences. No `components/*/src` change. Ride-along tests only as named.
Every line from your own clone at ee573c4+. Where ADR-0172's census and
the tree disagree, the tree, one record line. STOP if any ruled row of
ADR-0172 cannot be honoured by the fold as the tree stands.
