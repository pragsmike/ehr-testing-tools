# `sim describe`: what a corpus proves, versus what it was configured to make possible (ADR-0183)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-29-sim-describe.md`, which records
> where the live tree differed from this prompt. `sim check` reads the
> bare vector only, so the envelope is `describe`'s own addition.
> `ehrt.sim.interface` is frozen, so `describe-command` there needed an
> author ruling (BASELINE EDIT 2). Three of step 3's five measure names
> had no committed definition, so each is defined in its row.

Context: downstream (2026-09-29) wants a deterministic, versioned, machine-readable report answering
"what did this corpus actually contain, versus what was merely configured to be possible?" --
identity, kind counts and subject participation, temporal range, configured vs observed scenario
families INCLUDING configured-with-zero-witnesses, small exact witnesses (subject, stable log
position, t, related ids), and named relationship predicates. Report what the corpus proves; no
inferred narrative. First concrete case: the dense-7500 module cohort is enumerated to ordinal 7,496,
so a 28k run has a 7,500 run's module fraction. Baseline 1880b76d; schema 1.10.0; `bin/event-census`
stays what it is (a version-agnostic structural census; this is not that).

Read first:
- components/sim/src/ehrt/sim/run.clj:389-440 (check-command: stdin + --config contract); manifest.clj (build)
- bases/cli/src/ehrt/cli/help.clj:207-235; bases/cli/src/ehrt/cli/core.clj (the "check" dispatch, both sites)
- components/sim-engine/src/ehrt/sim_engine/fold.clj:1340-1370 (replay records); log_index.clj:92-340
- components/sim-check/src/ehrt/sim_check/check.clj:199-320, 1222-1300, 1355-1385 (the relationships as the invariants state them)
- components/sim-engine/src/ehrt/sim_engine/config.clj (config-keys); components/sim/src/ehrt/sim/run.clj:306-360 (effective-churn-profile)
- bin/event-census-src/ehrt/event_census/census.clj:1-40 (the reader contract: bare vector OR envelope)
- docs/consuming-ground-truth.md (where a consumer reads); docs/formats.md (a versioned format lives here)
- .agents/plans/2026-09-05-performance-measurement/raw/a22500-persons.census.md (what a hand census looked like)

Author ruling 2026-09-29: "Go" -- slice 1 lands now. Channel expectations (not rulings; correct from
the tree): the verb is `ehrt sim describe`, same input contract as `sim check` (log on stdin, bare
vector or envelope; `--config` optional); implementation `ehrt.sim-check.describe`, exported through
`sim-check/interface` and `ehrt.sim/interface describe-command`; output is a `result/ok` EDN map
carrying its OWN `:describe-version "1.0.0"`; `--format text` is a concise human view of the same map.

Steps:
1. Red tests (sim-check + sim): (a) determinism -- describing the `immunization` root twice yields
   byte-identical EDN; (b) a golden report for that root, committed, asserted `=`; (c) configured-with-
   zero-witnesses -- a config enabling a churn type at a probability that a 3-patient run cannot realize
   yields a family row `:configured :yes :observed 0 :witnesses []`; (d) bare-vector input reports
   `:identity {:source :bare-log ...}` and every configured column `:unknown`, never a guess; (e) a
   pathway-name family reports `:observed :unprovable` with the reason.  Invariant: red at 1880b76d
   because the namespace and verb do not exist.  Gate: brick:sim-check, brick:sim show these only.
2. The report. ONE pass over `engine/replay` records; accumulators hold counts and at most k witnesses
   (`--witnesses`, default 3, by ascending log index); no per-event retention beyond replay's own.
   Sections, every collection sorted so equal inputs give equal bytes: `:identity` (manifest seed,
   config sha, schema version, generator version, invocation churn flag; or bare-log + input sha256 +
   event count), `:counts` (per kind; distinct subjects; per-kind subject participation), `:temporal`
   (min/max :t, ISO when the manifest gives a reference date), `:families`, `:predicates`.
   Witness shape: `{:index i :patient-id .. :t .. :encounter-id .. :related [..]}` -- log index is the
   stable position (it is also every MSH-10's suffix since ADR-0181).
   Invariant: the report never contains a claim the log does not carry; an unknown is `:unknown`.
   Gate: step 1 (a)(b)(d) green.
3. The family catalog as DATA in one file: rows `{:family kw :configured-by (fn [config invocation])
   :observed-by (fn [record]) :witness-fields [..]}`. Slice-1 rows: each opt-in family and its kinds
   (churn x6, scheduling outcomes x4, bed-cycle, repeat-encounter, persons-bound, placeholder,
   immunization); observed modules by `:citation :module` against configured `:modules`; pathways by
   name (configured only; observed :unprovable -- slice 2); and the measures the tree has hand-run:
   result-after-merge, reinstated-stay-without-closer, transfer-between-order-and-result,
   bed-reoccupied-by-someone-else, pending-result-at-discharge.
   Invariant: every row's `:observed-by` is a predicate over a replay record or a pair the invariants
   already define -- cite the invariant in the row.  Gate: step 1 (c)(e) green.
4. Predicates section, the four asked for, as counted relationships with witnesses: same-subject
   opener/closer pairs; before (order before its result); during-encounter (clinical event inside its
   open encounter, by :encounter-id when stamped, by replay state otherwise); result-belongs-to-order
   (via :order-event-id, through merges as check.clj resolves it).
   Invariant: counts equal what the corresponding invariant would count.  Gate: an assertion per
   predicate against `check-all` on the same log.
5. Verb + docs: help.clj entry (cli.md regenerates); consuming-ground-truth.md "Describing a corpus"
   section with the module-cohort example as the configured-vs-observed row; formats.md gains the
   report as a versioned format.  Invariant: `make docsgen && git diff --exit-code` clean; poly check OK.
6. Measure: describe the dense-7500 750 cell and (if the host allows) the 7,500 cell; record wall and
   peak RSS beside `sim check`'s on the same log.  Invariant: memory is replay's, not the report's.
7. ADR-0183 (Accepted): the surface, its version contract, the unknown/unprovable rule, slice 2 named
   (manifest assignment record). No roadmap row.  Gate: notes/ADRs.md regenerated.
8. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries
   the downstream line: verb, input contract, `:describe-version`, and the module-cohort row it reports.
