# Session prompt — throughput spike: scaling exponent of generate→check
# (measurement session, pre-arc-1; program plan "Measurements that gate")

## Context

ADR-0168's program targets ~10^5 skeleton events/day. Plan appendix
estimates (labeled, unverified) predict minutes-class generation AFTER the
arc-3 scan fixes and warn the current decide-time scans are O(n^2)-shaped
(ADR-0164 patient-scoped them; it did not remove them). No dense-regime
throughput measurement exists: the 13.89s clinic-decade run is 343 events,
startup-dominated, and extrapolates to nothing. This spike measures the
scaling exponent BEFORE arc 1's design ADR is commissioned, because a
second quadratic (or a memory wall) would reshape arc 3's scope.

Measurement session: no src, no test, no schema changes land. Scratch
scenario configs and a small driver script are working artifacts — they
may live under a scratch dir and ride the close commit as session-record
appendix material if small, else are described in the record and not
committed. FINDINGS over fixes: any pathology found is rowed, never
patched here.

## Read first

1. .agents/plans/2026-08-24-traffic-scale-program.md — the spike's own
   spec ("Measurements that gate the program")
2. demos/scenarios/clinic-decade/config.edn and ed-tuesday/config.edn —
   scenario-config shape for the dense synthetic scenario
3. .agents/session-records/2026-08-24-suite-time-*.md — measurement
   conventions (quiet-machine health record incl. HOST-side sample:
   LoadPercentage + top-CPU; unpiped invocations; figures of record)
4. notes/adr/0164-*.md — the two patient-scoped scan sites the profile
   should watch

## Steps

0. Standing environment checks; HEAD ff45ad1 or descendant; tree clean.
   Health record per the 08-24 convention INCLUDING the Windows-side
   sample, before every timed run. No baseline suite (nothing lands in
   src; disclose).

1. Dense scenario design, disclosed in the record: a synthetic config
   whose event yield per patient is high and roughly constant — module
   mix drawn from the vendored set weighted toward high-emission modules,
   short-to-moderate horizon, churn on. Calibrate patient counts to hit
   ~10^3, ~10^4, and ~10^5 TOTAL events (three configs or one config at
   three patient counts; record achieved event counts, not targets).
   INVARIANT: every measured run must be self-check CLEAN — a violating
   corpus measures error paths, not generation. If the dense config
   trips an invariant: FINDING (a real defect at density), record and
   either adjust the mix to route around it (disclosed) or stop if it
   cannot be routed around.

2. Measurement matrix, one warm-up + two timed runs per cell, in-process
   timing around the generate and check phases separately (a tiny driver
   invoking the same fns bin/ehrt does — record which fns, so the path
   measured is the path shipped):
   - generate-only wall per scale point
   - check-only (invariant catalog) wall per scale point
   - peak RSS per scale point (record method)
   Derive the scaling exponent per phase from the three points
   (log-log slope). Two timed runs must agree within ~10% or a third
   decides; disagreement beyond that is itself a finding (machine or
   variance issue) — do not average over it silently.

3. Locate, don't fix: if either exponent is meaningfully >1, profile ONE
   run at the largest scale sufficiently to name the dominant site
   (namespace/fn level; async-profiler if available, else timing
   instrumentation in the driver). The ADR-0164 scan sites are the named
   suspects for check/decide; confirm or exonerate them SPECIFICALLY.
   Also record: does memory growth threaten held-whole at 10^5 (plan
   arc-3's streaming premise — confirm or weaken it with numbers).

4. Disposition into the plan (edit .agents/plans/2026-08-24-traffic-
   scale-program.md, appendix): replace the affected estimates with
   measured figures, each labeled MEASURED with date and run parameters;
   estimates not yet retired stay labeled as estimates. If a second
   quadratic or memory wall was found, add it to arc 3's scope list as a
   row item and a roadmap note on the arc-3 row (6-line cap; rotate
   first if Done is at cap — it was left at 24 lines).

5. Session record per standing structure: health records, the matrix,
   exponents, profile findings, config parameters verbatim (someone must
   be able to re-run this in a year), deviations. Self-archive prompt.
   ONE commit, docs/plan/record only:
   `docs: throughput spike -- generate/check scaling exponents at
   1e3/1e4/1e5 events, estimates retired to measurements, arc-3 scope
   confirmed or amended (session record <date>)`
   Local only; no push, no tag.

## Fences

- F1: no src/test/schema/vendored changes. Driver + configs are scratch.
- F2: every timed run on a verified-quiet machine, host-side sampled —
  a contended number is not a number (the Overwatch lesson, 08-24).
- F3: measured figures replace estimates ONLY where actually measured;
  no interpolation promoted to MEASURED.
- F4: run budget ~12 generation runs total (3 scales × 3 incl. warm-up,
  + profile + slack); a 10^5 run that exceeds ~30min wall is itself the
  finding — record it, do not wait it out repeatedly.
- F5: premise corrections (a config knob that doesn't exist, a phase
  boundary drawn differently in code than the prompt assumes) are
  findings — report what the tree actually has and measure that.
