# 2026-08-26 — arc 3b design: scheduling state, the bed-status cycle, and the encounter horizon (ADR-0174)

Repo `ehr-testing-tools`, WSL clone `/home/mg/src/ehr-testing-tools`,
branch `main`, HEAD at session start `febe594`. Payload session under
the de-scaffold moratorium: design only, no `components/*/src` change.
Ceremony R30 (commit and push at each checkpoint, unattended).

## The prompt, verbatim

Session prompt -- arc 3b design: scheduling state, the bed-status cycle,
and the encounter horizon (ADR-0174)

Context. HEAD febe594. Arc 3a is closed: the demographic fold is on in six
corpora. Arc 3's remaining folds are R-mix-5 (scheduling is STATE:
appointment new/reschedule/cancel/no-show as skeleton events) and R-mix-6
(bed-status is STATE: vacated->dirty->cleaning->ready, assignment gated on
ready) -- `rulings.md` :297-300, `docs/dev/traffic-model.md` :43-50. This
session designs both, plus the question arc 3a exposed and rowed as
`[multi-encounter-horizon]` (roadmap :126): a repeat arrival queues no
steps because `admission-only-when-new` (`check.clj:120`) IS the
single-encounter horizon (sim/ADR-0007 point 3) and `evolve :discharge`
never returns a patient to `:new`. Channel recommendation, for the ADR
to argue or refute from the tree: the horizon belongs to 3b, because
R-mix-5's own invariant "a scheduled encounter follows its appointment"
is empty unless a patient can have a SECOND encounter. Design ADR only,
no engine code, rulings lettered. Payload session.

Ride-along first, own commit: ADR-0173 §2(d) gains a dated deviation --
"the placeholder rule as written was unreachable (9 windows, 0.018% of a
200-person decade, first at day 418, arrivals inside 17 h); ruled (a)
2026-08-26: an `:identity-unavailable` window is itself an unidentified
ED presentation and mints the arrival" -- quoting part 4's record :64-80.

Read first: ADR-0168 §4, ADR-0169 (fold-carried state is the pattern:
`:reinstate-index`, `:citation-index`, `:person-index`,
`:registration-index`), ADR-0173 §2(b) (queue-seeding into the
`sorted-map` keyed `[t seq-no]` -- the seam every new skeleton family
uses); `engine.clj` `allocate` and the ladder, `bed-ready-location`
:1108-1160 (today "vacated" IS "ready": the just-vacated bed places the
boarder in the same instant -- R-mix-6 inserts a cycle between), the
`:bed-ready` flag :1093, `:outpatient-visit` (:751 census: 221/221 --
today's only non-admission encounter), `evolve :discharge`; `check.clj`
:120 and every invariant keyed on patient state rather than encounter;
`event_schema.clj` (1.4.0) and `PatientState`; corpus-player's bed-board
sink row (roadmap :143, ADR-0014) -- the consumer R-mix-6 names;
`docs/operational-models.md` (the ladder and the coupling it cites).

Step 1. Census from the tree: (i) every place "encounter" is implicit --
invariants, `PatientState` fields, emitters (PV1-19 visit number: what
is it today?), the check family -- tagged single-encounter-assumed /
encounter-agnostic; (ii) every bed-state read/write and the ladder's
rungs; (iii) every arrival source now (walk-in ordinal, delivery,
occupational-injury, identity window, repeat) and which could be
SCHEDULED. Counts, file:defn, no prose claims without a row.

Step 2. Design, each with rejected alternatives: (a) ENCOUNTER: an
`:encounter-id` minted at arrival and carried on every event of that
encounter; `PatientState` gains `:encounters` (history) and the current
one; `admission-only-when-new` becomes admission-only-when-no-open-
encounter; `evolve :discharge` closes the encounter; PV1-19 renders it;
MRN unchanged. State what every single-encounter-assumed site becomes
and which invariants split into per-encounter + per-patient forms.
(b) SCHEDULING: `:appointment` / `:reschedule` / `:appointment-cancel` /
`:no-show` skeleton kinds; a scheduled arrival references its
appointment; split of arrivals scheduled vs walk-in as WORLD config
(emission ratios reshuffle nothing, R-mix-7 -- but this split DOES draw:
say which family); return visits scheduled at discharge (follow-up
hazard) -- the first producer of second encounters; invariants per
traffic-model :45-47. (c) BED-STATUS: world-level per-bed state machine
vacated->dirty->cleaning->ready with durations drawn from FACILITY;
`allocate` gated on ready; `bed-ready-location` becomes the READY event
of the cycle, not the discharge; invariants (no assignment to a
non-ready bed; every ready follows a cleaning; occupancy-within-capacity
unchanged); what the bed-board sink consumes. (d) Contract: which kinds
reach the wire (S12/S13/S15/S26 for scheduling; A20 for bed status --
cite the v2 chapter, do not assume) vs stay skeleton-only; version bump
owed or not, by `classify-change`.

Step 3. Rulings, lettered, recommendation each -- at least: (A) encounter
horizon lifted in 3b or deferred; (B) `:encounter-id` derivation (mix64
of patient-id x ordinal, like everything else); (C) which scheduling
kinds reach the wire in v1; (D) bed-cycle durations FACILITY vs WORLD;
(E) landing order -- D1-style dark-then-on, and whether encounter,
scheduling, bed-status are one sweep or three (channel lean: encounter
first, alone, since everything else references it). ADR-0174 Proposed;
roadmap: `[engine-fold-extensions]` 3b half gains one line,
`[multi-encounter-horizon]` gains "placed: 3b (proposed)"; record one
page; push; CI is the gate; no tag.

Fences. No `components/*/src` change. Ride-along is the only ADR edit
outside ADR-0174. Every line from your clone at febe594+. Where a
ruled R-mix row and the tree cannot both be honoured, STOP and say which.

## Deviation record

Four, none of them a scope change, all disclosed in the session record.

1. **`:encounter-id` is minted at the ENCOUNTER OPENER, not "at
   arrival"** as step 2(a) words it. The tree forces it: an arrival and
   an encounter are not the same event, and 6 of ed-tuesday's 116
   patients and 112 of clinic-decade's 230 register and never open an
   encounter at all. Minting at arrival would give every one of them an
   encounter-id for an encounter that does not exist.
2. **Step 2(d)'s "cite the v2 chapter" is answered from the tree's own
   `hapi-structures-v24` 2.6.0 jar, not from a chapter.** No HL7 v2
   standard text is vendored in this repo and this ADR would not assert
   one from memory. What the jar proves is stated exactly.
3. **Two read-list pointers are cited differently in the ADR, and the
   reason is stated.** The `221/221` `:outpatient-visit` figure at
   `engine.clj:751` is a HISTORICAL census quoted inside `reason-field`'s
   docstring, not a live count -- the two demo corpora measured here
   produce 0 and 28 -- so the ADR uses its own measurements and cites
   751 only for what it is. `docs/operational-models.md` lives at
   `components/sim/docs/operational-models.md`.
4. **One roadmap row is reported rather than corrected.**
   `roadmap.md:143` calls the ADR-0014 bed-board sink an unpriced,
   unrowed slice; it landed under ADR-0067 and ships as `--board`. The
   fence allows two roadmap edits and this is neither, so it is
   disclosed for the author.

No STOP was owed. No ruled R-mix row and the tree were found in
irreconcilable conflict; the one real tension -- `R-mix-6` names the bed
board as a consumer, and the board reads MESSAGES while this ADR
recommends emitting none -- is resolvable by deferring the message
family, is named in section 2(c) and 2(d), and is what ruling C exists
to decide.
