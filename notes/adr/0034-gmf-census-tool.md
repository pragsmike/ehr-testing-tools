<!-- Attic file: notes/adr/0034-gmf-census-tool.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0034 — GMF census tool: load/walk verdicts, substitution tags, first pinned artifact (`.agents/plans/2026-08-02-gmf-parity-plan.md` §3, ADR-0031 AR-1/AR-4)

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-6 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
same day.

### Context

Both defect-fix sessions ADR-0031 AR-6 sequenced ahead of the census
(ADR-0032 Procedure duration, ADR-0033 engine closure context) had
landed at origin before this session started, so a smoke-walk digest
recorded now sits on final timing semantics. This session builds the
census tool as a `sim-trajectory` dev entry point (ADR-0031 AR-1 — not
a CLI verb), runs it against the full Synthea catalog at the pin, and
commits the dated census artifact.

### Decision

Ruled 2026-08-03, design channel, recorded verbatim:

**AR-1 (input, pin verification).** The entry point takes a filesystem
path to a Synthea checkout and reads `src/main/resources/modules/**` —
no network at run time, no vendoring of the catalog (installed ≠
used). The census artifact records the pin: if the path is a git
checkout, `git rev-parse HEAD` must equal the interpreter doc's own pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) or the run REFUSES
(errors-as-values: an `:error` result naming the mismatch); if not a
git checkout (tarball extract), the tool records a sha256 over the
sorted relative-path + content of the modules tree and DISCLOSES
pin-unverified-by-git in the artifact header. A census is a claim AT a
pin; an artifact that cannot name its pin is not a census.

**AR-2 (verdict vocabulary, v1).** Per module (root + resolved
closure): `:ok-walked`, `:load-failed`, `:walk-failed`,
`:out-of-scope-by-ruling` (empty is fine; the category exists per
ADR-0031 AR-4). Alongside the verdict, the gap detail names:
unrecognized state types, transition kinds, condition types,
unresolved attributes, unresolved submodules/tables, closure file
count. A walk that throws is a `:walk-failed` with the cause recorded
— caught, recorded, census continues; the census itself NEVER aborts
on a module's failure.

**AR-3 (substitution tagging — load-bearing).** Any module whose
closure contains a `wellness: true` Encounter state gets
`:disclosed-substitutions [:wellness-timing]` on its census entry,
REGARDLESS of verdict — the Wave B create-now normalization (disclosed
timing substitution, ADR-0031 AR-5(b)) means such modules may load and
walk today under semantics upstream does not have. A substituted walk
is never presented as upstream-faithful; Wave G's ledger is countable
as exactly the entries carrying this tag. The detection is mechanical
(closure scan), extensible to future substitution classes.

**AR-4 (smoke-walk parameters).** Interpreter-layer only (ADR-0031
AR-4's boundary — engine round trips stay per-vendored-root tests).
Three seeds per module, derived per `digest.clj`'s discipline; fixed
persona-config and a fixed horizon bound (registration + a stated day
count large enough to exercise content, small enough to stay fast — the
session picks, states it, and records it) so no walk hits the
max-steps throw as a matter of course. EVERY census parameter (pin,
seeds, persona-config, horizon, tool version) goes in the artifact
header: the census must be re-runnable to the byte. Digest: sha256
over a canonical printing of each walk's trajectory + final status,
per-seed, recorded per module.

**AR-5 (artifact).** EDN, committed at `components/sim-trajectory/docs/
census/<date>-synthea-<pin7>.edn`, plus a short generated summary table
(counts per verdict, top gap mechanisms by modules-blocked) appended as
a dated section to the interpreter doc's prioritization area — the
census SUPERSEDES the hand-read table as the frontier of record;
annotate the old table as superseded (dated note, no deletion). The
parity definition is now countable: zero `:load-failed` (minus
`:out-of-scope-by-ruling`) and every walked module's digests recorded.

**AR-6 (loose-thread bookkeeping, folded in — docs-only).** In the
records step: (a) roadmap Deferred gains a row for the regression-
oracle tool defect ADR-0033 disclosed (`digest.clj` read from current
checkout only — incompatible with API shape switches; manual
per-worktree workaround is not the new normal); (b) the parity plan's
Wave H row gains a dated pointer to the pre-horizon straddle finding
(ADR-0033 execution note: UTI's mandatory Encounter straddling
registration-t trips `:clinical-content-only-when-admitted` on 8 of 10
seeds; the UTI round-trip test's seed-777 dodge retires when H resolves
the boundary — written down where H's design session will read it).

### Execution note (filled same day, 2026-08-03)

`ehrt.sim-trajectory.census` (`development/src`, per AR-1): pin
verification (`verify-pin`), catalog discovery (85 top-level `*.json`
under `src/main/resources/modules/`, `lookup_tables/` excluded), closure
resolution via `ehrt.sim-trajectory.gmf/load-closure` with a
fetch-tracking `resolve-fn` (so `wellness-substitution?` can scan
whatever a `:load-failed` closure managed to read before failing, AR-3's
own verdict-independence), AR-2's gap extraction across nested
`:submodule-rejected` chains, AR-4's mixer-derived 3-seed smoke walk +
sha256 digest (mixer-seed `20260803`, registration at age 30, horizon
50 further years — one fixed, global choice, stated in the artifact
header, not tuned per module). Co-landing: 5 tests / 20 assertions,
one inline fixture per verdict class plus the substitution tag, proven
green (`development/test`, wired into the root `:test` alias).

**Disclosed, not fixed: `clojure -M:poly test :all skip:integration`
never runs these tests.** `clojure -M:poly ws get:projects` confirms
the `dev` project (`development/src`'s own `:dev` alias) carries
`:bricks-to-test []` — `poly test` runs per-project against a
project's own bricks, and `development/` is not a brick. Verified
instead by direct invocation (`clojure -M:dev:test -e
'...run-tests...'`): 0 failures, 0 errors. The same not-poly-tested
status `bin/oracle-src`'s own tooling already has — not a new gap this
session introduced, named for whoever next touches dev-entry-point test
coverage.

**A second real finding, this one in the thing being observed:**
`ehrt.sim-trajectory.gmf/gmf-v2-timing->v1`'s own `case` over a
`gmf_version 2` distribution `:kind` has clauses for `UNIFORM`/`EXACT`
only (D3c finding 1's own original scope) — a real `GAUSSIAN` (4
modules) or `EXPONENTIAL` (7 modules) kind throws a raw
`IllegalArgumentException` rather than a `:rejected` Result, found live
by the full-catalog sweep no hand survey ever exercised enough
`gmf_version 2` content to catch. Per this session's own fence (the
census observes the loader, never changes it), `census-one` wraps
`load-closure` in `try`/`catch` so this one finding does not abort the
run — the same defensive discipline `walk-one` already applies to the
interpreter one layer down. Named for a future defect-fix or Wave I
session, not fixed here.

**First census, pin `7e08387c68a7f0e21d13076609a159fd473fc902`
(`git rev-parse HEAD` verified against a live checkout, not the
sha256-content fallback), committed at `components/sim-trajectory/docs/
census/2026-08-03-synthea-7e08387.edn`:**

| Verdict | Count |
|---|---:|
| `:ok-walked` | 40 |
| `:load-failed` | 39 |
| `:walk-failed` | 6 |
| `:out-of-scope-by-ruling` | 0 |

85 modules total; 19 carry AR-3's `:wellness-timing` tag (nearly 4× AR-5(a)'s
own five-module hand survey, all five included). Top `:load-failed`
mechanisms: `Counter` (11), `ImagingStudy` (10), the `gmf_version 2`
loader-exception finding above (11 combined), `SupplyList` (3),
`AllergyOnset`/`VitalSign`/`Vaccine` (1 each), an unrecognized
lookup-table column (1). `:walk-failed` mechanisms: two condition types
never named in §2's own vocabulary — `Race` (3 modules), `Not` (1
module) — plus a `max-steps` runaway in 2 modules (`med_rec`,
`veteran_substance_abuse_treatment`).

**Sanity anchors (this session's own STOP-AND-ESCALATE gate) both
held.** All SEVEN currently-vendored roots (`appendicitis`,
`ear-infections`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement`, `urinary-tract-infections`) census
`:ok-walked` — the driving prompt's own text said "eight"; the actual
count, confirmed by direct listing of `components/sim/resources/sim/
modules/*.json` AND by `docs/gmf-interpreter.md`'s own D3f
regression-baseline prose ("all SEVEN currently-vendored roots"), is
seven. A small premise correction against the session's own prompt,
disclosed here rather than silently adapted — no ruling anywhere in
this file or the parity plan ever said eight, so nothing is being
contradicted, only corrected. All five of ADR-0031 AR-5(a)'s named
wellness modules (`epilepsy`, `mTBI`, `atrial_fibrillation`,
`osteoporosis`, `med_rec`) carry the AR-3 tag regardless of verdict
(four `:ok-walked`, `med_rec` `:walk-failed` on the max-steps finding
above) — confirming AR-3's own verdict-independence claim empirically,
not just by construction.

`docs/gmf-interpreter.md` gains a new dated §15 (the full breakdown
above, plus the ranked-mechanism tables) and a superseded note on §8's
own hand-scouted prioritization table (kept, annotated, not deleted).
AR-6's two bookkeeping items land in `.agents/plans/roadmap.md`'s
Deferred section and `.agents/plans/2026-08-02-gmf-parity-plan.md`'s
own Wave H row, respectively — both as dated pointers, not new design
work.

`clojure -M:poly check`: OK, every checkpoint. `clojure -M:poly test
:all skip:integration`: 193 passes / 0 failures / 0 errors, unchanged
by this session (no product-brick code touched — `development/` and
`docs/` only). `gitleaks git --staged -v`: clean, every commit.

### Fence

No wave E/F/G mechanism work, no matter what the census shows — the
ranking read is the design channel's next move, not this session's.
No engine changes, no loader changes: the census OBSERVES the
interpreter as it stands, including the one place (the `gmf_version 2`
timing case above) where observing meant hardening the OBSERVER against
a throw, never changing the thing observed. `ehrt.sim-trajectory.gmf`
and `ehrt.sim-trajectory.gmf-interpreter` are untouched by this
session's own product-brick diff.

---

