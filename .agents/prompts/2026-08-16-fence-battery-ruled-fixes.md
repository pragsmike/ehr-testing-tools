# Archived prompt: fence-battery-ruled-fixes (2026-08-16)

Verbatim, as received. Landed as `notes/adr/0140-fence-battery-ruled-fixes.md`.

---

SESSION PROMPT — fence-battery ruled fixes (micro-session; closes D8-5 with ADR-0140)

Drafted by the design channel, 2026-08-16, against a fresh public clone at `30cc335` (battery register landed; tree clean; tags `stable-20260815-result-nodes` and `stable-20260815-review-3-fixes` both verified on the remote). Channel independently re-derived the battery's population census (102 files, 201 vs registered 202 fenced blocks — one-block noise, exact file count), the register's verdict counts from its rows (42/7/5/4 = 58), and the seven-RED root cause (26 bare `poly` invocations in one dev-facing brief that documents the `clojure -M:poly` alternative at :318 and :551).

**Author rulings, verbatim**

* "Accept recommendations." (2026-08-16) — binding the channel's per-finding recommendations, each stated below at its step, and the session shape (a): the battery's fixes are their own micro-session with their own ADR, so D8-5's lapse closes on the record before the event-log-contract arc starts.
* R-F8 (76% of command fences unexercised) is NOT this session's work: ruled to review 4's D2 as a standing policy question, with the channel's proposed rule as the default — "every fence a reader meets on the README / SETUP / manual / use-case path is exercised; developer-facing briefs and research notes are exercised only when they make claims about outputs; the census can gate bare-fence-count-on-reader-path = 0." Record it in the ADR as handed to review 4; do not implement.

**Read first**

* `.agents/plans/2026-08-16-fence-battery-findings.md` — the whole register, especially the "What needs your ruling" table (:325-334) and each finding's row for the exact evidence (exit codes, the clean-`out/` re-probe note)
* `bin/fence-census` (the enumerator; re-run it at close for the post-fix count)
* The eight touched files, named per step below

**Step 0 — Preflight**

`bin/preflight` plain; verify both standing tags: `stable-20260815-result-nodes^{}` = `b139de5…`, `stable-20260815-review-3-fixes^{}` = `b96c246…`. Baseline `30cc335` or descendant. No tag owed by this session (the micro-session's own close tag is deferred to the next session's Step 0 under the standing conditional license, ADR-0133/0135's pattern). `out/` must be CLEARED before any fence re-run in this session — the battery's own near-miss (a stale `-first-run` dir produced a false RED against the manual's most emphatic claim) makes a clean `out/` a precondition, not a nicety.

**Step 1 — Page fixes (R-F1, R-F2, R-F3, R-F5), one commit**

R-F1 — `docs/manual/04-time-on-the-wire.md`, the play fence at :24. Ruled: fix the page. The fence plays `out/scenarios/ed-tuesday-latency`, which the chapter creates at :72-77. Minimal fix: one sentence immediately before the fence — "(This plays the latency wire that the two `sim run` invocations below create; run those first, or run `bin/demo-exerciser-ed-tuesday` once.)" — do NOT reorder the chapter's sections (move-don't-improve; the pedagogy of "symptom first, then mechanism" is the author's, and this fix preserves it). Re-run the fence from a cleared `out/` after running the two `--out-dir` fences: exit 0.

R-F2 — `docs/manual/08-your-own-data.md:82`. Ruled: fix the page. The fence checks `out/corpus/synthea-s1-p5/fhir`, which Chapter 2 generated and Chapter 8 never re-creates. Minimal fix: one sentence before the fence pointing at Chapter 2's corpus by name ("the Synthea corpus you generated in [Chapter 2](02-setup-first-corpus.md#…) — regenerate it if `out/` was cleared"), anchor resolved against Chapter 2's real heading. Re-run from a cleared `out/` after regenerating: exit 0.

R-F3 — `docs/dev/migration/polylith-brief.md`, the seven bare `poly` fences. Ruled: fix minimally, provenance-preserving. Do NOT rewrite the fences: add ONE provenance note at the top of the document — "This brief teaches the standalone `poly` binary as the upstream Polylith docs do; in this workspace `poly` is not on PATH and every `poly <cmd>` below runs as `clojure -M:poly <cmd>` (see §Install and §Cheat sheet)." The seven fences stay verbatim; the note converts their RED to a disclosed convention. Verify the note's two section references resolve.

R-F5 — `components/corpus/docs/research/HL7v2-sanitized-corpus-research.md:126`. Ruled: fix the page. The `curl -L` writes `messages.out` into the repo root (the battery's staging hygiene caught it). Change the output path to `out/simhospital/messages.out` (or `/tmp/…` if the note's later steps assume a scratch location — read the following fences to decide, and keep them consistent). Verify `out/` is gitignored (it is; say so in the record).

Commit (message-via-file, ASCII):

```
docs: four battery-ruled page fixes -- ch4 play precondition, ch8
corpus provenance, polylith-brief poly-alias note, research-note
curl target (fence-battery R-F1/R-F2/R-F3/R-F5)
```

**Step 2 — Accept-with-disclosure (R-F4, R-F6), one commit**

R-F4 — `docs/formats.md:506` and `:518`. Ruled: accept-with-disclosure. Mark the placeholder paths as placeholders (`<your-corpus-dir>` style, or a lead-in sentence "illustrative shape, not a runnable command"), and note `jet` as an optional external tool at :518 with a one-clause pointer to its source. Do NOT vendor `jet`.

R-F6 — `docs/simulate-your-facility.md:169`. Ruled: accept-with-disclosure. One sentence before the fence: "save the config block above as `stmarys.edn` (or point `--config` at one of the runnable configs under `demos/`)." The `:config-not-found` exit is then a taught expectation, not a surprise.

Commit:

```
docs: two battery-ruled disclosures -- formats.md placeholders and
optional jet, simulate-your-facility config precondition
(fence-battery R-F4/R-F6)
```

**Step 3 — The tool fix (R-F7), red-first, one commit**

Ruled: fix the tool. Read the register row (:198) precisely: the input gap itself is already close-as-fine (review 2 D8-9, "You bring"); the defect is DIAGNOSABILITY in the pipeline. `mutate` emits its honest `{:status :error :category :file-not-found}` (exit 2) — but its stdout is piped into `intake`, which reads an empty/error stream and reports `:malformed-mllp-frame`, hiding the real cause from the reader.

1. Read `intake`'s stdin path and its `:malformed-mllp-frame` producer. Red-first test: `intake` given EMPTY stdin (and, separately, stdin carrying an EDN error envelope from an upstream `ehrt` command) must report a distinct category — `:empty-input` for empty, and for the upstream-error case either pass the upstream envelope through or report `:upstream-error` carrying it — never `:malformed-mllp-frame`. Witness RED against current code.
2. Minimal fix at the intake seam (the D4-3 pattern: distinguish before parsing; engines untouched). GREEN.
3. Regenerate anything `cli-md-is-current-test` gates if `intake`'s category list is rendered into `docs/cli.md`; update the one line in `docs/formats.md`'s intake section if categories are listed there — nothing else.
4. Re-run the use-case page's fence with `in/v2-corpus` absent: the reader now sees the honest cause. Record the before/after output in the session record.

STOP-AND-REPORT if the honest fix cannot stay inside `intake`'s stdin path (e.g. it requires changing `mutate`'s stdout contract or the pipe convention) — that is a design change, not a diagnosability fix, and needs the author.

Commit:

```
fix: intake distinguishes empty stdin and upstream error envelopes
from malformed MLLP frames (fence-battery R-F7, diagnosability;
D4-3 pattern)
```

**Step 4 — Records and close**

1. ADR-0140 (verify next free number): the battery's disposition table — every ruled finding with its verdict movement (RED→GREEN, YELLOW→disclosed, tool fixed), R-F8 handed to review 4 with the proposed rule quoted, the near-miss (stale `out/`) recorded as an incident class the battery itself found (rule 9), and D8-5 marked DISCHARGED — its lapse closed on the record.
2. Re-run `bin/fence-census` after the fixes; record the post-fix census (population unchanged; verdicts moved) in the ADR.
3. Register: append a dated close note under its ruling table pointing at ADR-0140; do not rewrite rows.
4. Roadmap: D8-5's row → CLOSED; Done pointer; re-derive `:onboarding` (was 2657/2690, 33 headroom) — bump with dated comment only if forced.
5. Rulings rows: "Accept recommendations." verbatim with the eight per-finding glosses.
6. `bin/close-scaffold`; session record (each fence's re-run exit from a cleared `out/`, R-F7's red/green, the before/after pipeline output); prompt archive.
7. Full `make test` unpiped, `MAKE_EXIT` captured. Predict blocks before running: 640 + delta for R-F7's test — if it lands in an existing CLI test namespace, 640; if a new namespace, +2 per project context that runs it. Reconcile; explain any mismatch.
8. Push; `bin/post-push-verify` (no arguments).

**Fences**

* Touch ONLY: the four Step-1 pages, the two Step-2 pages, `intake`'s stdin path + its test (+ any generated doc its category change forces), the register's close note, `.agents/plans/roadmap.md`, `.agents/rulings.md`, ADR-0140 + index line, session record, prompt archive.
* Page fixes are one-sentence-or-note each: no reordering, no restyling, no touching the fences' command text except R-F5's output path (and R-F4's placeholder markers, which are the ruled fix).
* Zero converter/generator changes; zero vendored bytes; `out/` cleared before every fence re-run.
* STOP-AND-REPORT: R-F7's fix escaping intake's stdin path; any page fix needing more than the ruled minimum; a fence re-run from a cleared `out/` still non-zero after its fix; block count unexplained; `:onboarding` not re-derivable.
