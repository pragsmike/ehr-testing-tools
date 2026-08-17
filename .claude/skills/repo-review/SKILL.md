# Repo Review

Periodic, rubric-driven quality review of this repository: mine the
record for what changed and what failed, run a probe battery across
eight quality dimensions, land a dated assessment register, generate a
mitigation plan for the author's ruling, and execute the ruled fixes
under the standing arc pattern. This skill is the generalization of the
alignment arc (ADR-0048–0055) and the UX arc (ADR-0056–0064): survey
under a lens, register findings, rule, fix with co-landed gates, close.
What it adds is the ROTATING LENS — the rubric below — because every
one-lens pass leaves the other lenses' failures silent (the flaky
temp-dir test survived the hermeticity lens that created it; the
LF-normalized CSV survived a correct hash; the stale cli.md survived a
green local suite; ADR-0075 records all three).

## Use this skill when

- The author asks for a repo quality review, assessment, or
  improvement pass, or a periodic review comes due (cadence: the
  author's call; the assessment artifact records the date either way).

## Do not use this skill when

- A specific incident is already in hand with a known fix — that is an
  ordinary ruled fix session (`build-session`), not a review.
- An arc is mid-flight. Reviews open arcs; they do not interrupt them.

## The rubric — eight dimensions, probes for each

Score each dimension green / yellow / red by finding count and
severity — never a numeric grade. Carry the PRIOR assessment's scores
forward in the new artifact so drift is visible across runs. Every
probe follows the standing law: audit evidence uses the mechanism it
recommends — re-derive, re-hash, re-run; never re-read a claim as its
own verification. A sibling law: every probe states its population and
enumerates it from the tree, never from the registry under audit — the
make graph, a scan-root list, and the intake registers are themselves
audit subjects, and equating any of them with the population converts
their omissions into silent green verdicts. The first question of every
probe is "how do I know this is all of them?"

1. **Claim–reality coherence.** Registers, docs, NOTICEs vs the tree.
   Probes: re-derive every stated count (`wc`, `ls`, fresh grep);
   re-hash every stated hash against on-disk bytes; re-resolve every
   cited path, with scan roots covering every tracked doc surface
   including `components/*/docs/` (the scan-root class has two recorded
   hits: E-5, 2026-08-05, and the sim-theory recipe path, 2026-08-14 —
   never a root narrower than the tree again). For the continuity
   register the probe has changed shape (ADR-0147): `.agents/state.md`
   no longer carries `[V]` claims to sample — it is hand-owned
   judgement, capped and linted — and every count it used to carry is
   generated into `.agents/state-derived.md`. Probe THAT by regenerating
   (`make state-derived`) and diffing, and probe `state.md` for pointer
   rot instead: does every register it names still exist, and does every
   gate it cites still run?
2. **Guard coverage.** Every law has a gate; every gate runs where
   someone looks. Probes: enumerate `.agents/rulings.md`'s standing
   rulings and map each to its enforcing test (a law with no gate is a
   finding); enumerate checks that run ONLY in CI or ONLY on the
   author's machine; enumerate laws stated on multiple surfaces with
   no drift gate.
3. **Environment independence.** Truth is fresh-clone truth. Probes:
   the full suite from a genuinely fresh clone; `.gitattributes`
   byte-determinism for every byte-precious tree; test helpers'
   tmpdir/parallelism/file-descriptor assumptions (ignored boolean
   returns on `.mkdirs`/`.delete` are findings); anything depending on
   untracked files, author-local checkouts, or network.
4. **Error honesty.** Failures name their artifact; no error absorbed
   as an answer. Probes: grep for nil-returning I/O calls whose nil
   flows on silently (`listFiles`, `list`, `resource`); `catch` blocks
   that continue without a category; silent caps/truncations; parses
   that default on malformed input. Each hit is read in context, not
   auto-flagged.
5. **Mirror and derivation drift.** Probes: byte-diff every mirrored
   pair (skills, any dual-homed doc); regenerate every derived doc and
   compare (`make docsgen` and any sibling generators) — where "every
   derived doc" is enumerated from the tree first (grep tracked files
   for generation banners, converter references, and embedded
   regeneration recipes), diffed against the make graph's targets; a
   derived artifact with no registered regeneration path is a finding
   (class: unregistered derivation) regardless of its current
   freshness; confirm each
   generator's currency is gated LOCALLY where JVM-derivable, and that
   CI-only checks are named as such where they live.
6. **Sampling adequacy.** Test statistics must reach the claims'
   population. Probes: for every sampled verdict (census seeds,
   property trials, round-trip populations), compare the sample's
   power against the rarest branch it vouches for; list verdicts
   whose sample a known branch probability outruns (the census's 3
   seeds vs injuries' 3.3% branch is the canonical miss).
7. **Continuity integrity.** A cold reader can reconstruct the truth.
   Probes: citation-resolution sweep; pairing/index/done-pointer gates
   green; attic-vs-live consistency; carried-item aging — any intake
   or design item carried through two or more arc closes is NAMED as
   aged, with its blocker stated (the pairing-as-data precedent).
   Header-resident requests: grep tracked files for standing requests
   embedded outside the registers ("standing request", "TODO", "FIXME",
   "regenerate", "next session"); any request not mirrored in a
   register row is a finding (class: unregistered standing request),
   aged from its first appearance in git history, since the aging probe
   above structurally cannot see it.
8. **Operator experience.** The stranger-facing surfaces work as
   taught. Probes: execute every live command fence; the
   bare-invocation / unknown-flag / missing-file matrix against the
   built CLI; help at 40/80/120 columns; the README's own
   two-commands-to-demo path, run for real.

## Procedure

1. **Preflight (R30 ceremony).** Fresh clone or verified-clean ext4
   tree at the design-channel-verified tip; full-suite baseline; CI
   conclusion for the last five runs on main disclosed; oracle
   pre-digest.
2. **History scan.** Since the prior assessment (or the repo's start,
   first run): read the arc closes, session records, and Deviations
   sections landed in the window; extract every incident, disclosed
   deviation, and finding; classify each against the rubric's
   dimensions. An incident class with repeat hits raises its
   dimension's severity.
3. **Probe battery.** Run the rubric top to bottom. Record each probe
   as: dimension, probe command or method, expected, observed,
   verdict. Negative results are recorded, not dropped — a clean probe
   is evidence the next review inherits.
4. **The assessment register.** A dated artifact at
   `.agents/plans/<date>-repo-review-findings.md`, in the audit
   register format the alignment and UX arcs established: one row per
   finding, with dimension, evidence, severity, and a PROPOSED
   disposition (fix-session candidate / ruling-needed / close-as-fine
   / intake). Include the dimension scoreboard with prior scores
   alongside. Nothing moves in this step — the register is a survey,
   not an act (the ADR-0049/0058 discipline). Before drafting THIS
   run's own register, re-derive the PRIOR assessment's own summary
   arithmetic directly from its per-dimension disposition counts —
   never trusted from its own summary line (repo review 1's own
   summary claimed 44 rows/26 close-as-fine/6 ruling-needed; a direct
   count came to 45/28/5, corrected fix-forward, `notes/adr/0078-
   result-or-loud.md` AR-RL-R). A register's own arithmetic is exactly
   the kind of claim this rubric says to re-derive, not trust — this
   probe generalizes that lesson into a standing step rather than
   leaving it a one-time correction.
5. **The plan, for ruling.** From the register: batch fix-session
   candidates into proposed sessions (small, fenced, each with its
   co-landed gate); state the rulings needed with options and a
   recommendation each; name what is deliberately fine. The plan goes
   to the author. RULINGS ARE THE AUTHOR'S — this skill proposes.
6. **Execution.** Ruled fixes run as ordinary `build-session` sessions
   under the arc pattern: riders → fix clusters with red-first gates →
   close with rulings appends, state regeneration, budgets, rotation,
   tags. Every fix lands with the gate that makes its failure class
   recur-proof — a fix without a gate is half a fix.
7. **Close the loop.** The arc close records: findings fixed / ruled /
   accepted / intake, the scoreboard's movement, and the NEXT review's
   inherited watch-list (probes that were yellow, samples that were
   marginal, carried items and their age).

## Output

- The dated assessment register (step 4) with the dimension scoreboard.
- The mitigation plan for the author's ruling (step 5).
- On the author's rulings: session prompts and executed fix sessions
  per the standing ceremony, and an arc close that scores the delta.

## Done when

- [ ] Every rubric dimension has recorded probe results, clean ones
      included.
- [ ] Every finding has evidence gathered by the mechanism it
      recommends, and a proposed disposition.
- [ ] The scoreboard shows this run beside the prior run.
- [ ] The plan reached the author; nothing beyond the register and
      plan landed without a ruling.
- [ ] Every executed fix carries a co-landed gate against recurrence.
