## ADR-0116 — Engine seed contract: non-negative, validated at entry

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

`.agents/rulings.md` "From ADR-0114" R8 chartered a future investigation
of `ehrt.sim-engine.engine-test`'s
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
defspec, which drew `seed` from `gen/large-integer` (the full long
range, negatives included) and had been observed to fail: once at seed
`7844068501` (`failing-size 110`, ADR-0112's own disclosure), and again
in CI at commit `ed00e3a` at a different seed, `1786546687672`,
`failing-size 126`, shrunk smallest `[-3377439408979484]` — a single
negative long (ADR-0115's own disclosure). The seed contract was
documented nowhere: both `--seed` rows the driving prompt's evidence
base named in `docs/cli.md` said "(integer)" unqualified, and the
engine interface stated no constraint. This session's own author
ruling, R9 (below), resolved the classification question the channel
framed: negative seeds are out of contract, not a legal input the
engine's arithmetic must tolerate.

### Tag ceremony

`git fetch` confirmed `origin/main` at `c6deb5a`
(`c6deb5a233477e562c3edcd2833798fab4f2719c`, ADR-0115 close) at session
start. License: tag-law case (i) — the design channel's own 2026-08-12
verification of the ADR-0115 landing (fresh clone; lineage; ASCII x3;
footprint exact including the disclosed reading-set re-baseline whose
arithmetic the channel re-derived independently; zero-`src` diff;
register row flips and cluster charters content-verified; CI
confirmed green on all three commits by direct API read). `stable-
20260812-review-3-rulings` tagged ANNOTATED at `c6deb5a`; pushed;
peeled ref confirmed `c6deb5a233477e562c3edcd2833798fab4f2719c` — exact
match.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success` —
`c6deb5a` (ADR-0115 fix-forward, 4m23s), `e31492b` (ADR-0115 session
record, 3m14s), `ed00e3a` (ADR-0115 rulings landing, 4m13s), `d508cd6`
(ADR-0114 fix-forward, 4m24s), `aeb45ab` (ADR-0114 session record,
4m38s) — no red among the five.

### Decision

**R9 [A, ruled 2026-08-12, on the channel's own framing].** Question:
is the seed contract (a) non-negative longs — engine validates at
entry, generator constrained to contract, contract stated in the docs
— or (b) all longs legal, making this an engine arithmetic bug? RULED
verbatim: *"a"*. Full text and the generalizable lesson recorded in
`.agents/rulings.md`, "From ADR-0116."

**Step 1 evidence (captured before any fix, per red-before-green):**

1. **The shrunk witness, directly.** `engine/run` evaluated at
   `{:seed -3377439408979484 :patients 4 :pathways [...] :modules
   [...] :module-assignment [...] :module-horizon-days 3650}` (the
   defspec's own exact config) did NOT throw — it ran to completion
   and `check/check-all` on the result returned:
   ```
   :status :rejected
   :category :invariant-violation
   :payload {:violations
             [{:invariant
               :medication-end-references-existing-order-and-follows-it-in-time,
               :patient-id "PID-000003-1a0eb69f",
               :at 178620}]}
   ```
   Confirms the classification: a negative seed reaches the invariant
   catalog unvalidated and can produce a real, silent violation, not
   merely an arithmetic curiosity.
2. **The two recorded defspec seeds, pinned via `clojure.test.check/
   quick-check`, 150 trials each (matching the defspec).**
   - `:seed 1786546687672` — **fails**, shrinks to `[-3377439408979484]`
     exactly as ADR-0115 recorded (`:num-tests 127, :failing-size 126`).
     Confirmed deterministic on a second run.
   - `:seed 7844068501` — **passed clean, 150/150 trials**, both times
     run. This does NOT match the evidence base's implicit claim that
     both recorded seeds are equally-confirmed per-seed repros.
     Reading `notes/adr/0112-batch-straddle-recording.md` directly
     (lines 117–129, 152–153) shows why: the "cleared on re-run"
     disclosure there was against a FRESH, unpinned seed
     (`1786504775396`), never against `7844068501` itself — the
     defspec draws its own random seed each run, so that re-run never
     actually tested whether `7844068501` reproduces. It was never
     confirmed as a per-seed-deterministic repro by anyone, and still
     isn't. Per author ruling (mid-session, below), this session
     proceeds on seed `1786546687672` alone as the operative repro,
     without re-litigating R9's classification, which the surviving
     evidence still fully supports.
3. **CLI reachability, pre-fix.** `bin/ehrt sim run --seed -1
   --patients 1` — succeeded: exit 0, `{:status :ok, :payload
   {:ground-truth [...three events...], ...}}`. Negative seeds were
   fully reachable from the user surface with zero validation
   anywhere in the path.
4. **Red regression deftest.** `run-rejects-negative-seed-with-clean-
   error` (`components/sim-engine/test/ehrt/sim_engine/engine_test.clj`)
   asserts `engine/run {:seed -3377439408979484 :patients 1}` returns
   `result/error :invalid-seed`. Run against the pre-fix tree: 3
   assertion failures (expected `result/error?` true, got a full run
   map with no `:status` key at all) — 314/317 other assertions in the
   namespace unaffected. This is the red half.

**The fix, three parts plus two widenings (all ruled, all evidenced):**

1. **Engine entry validation** (`components/sim-engine/src/ehrt/
   sim_engine/engine.clj`). `run`'s `:pre` clause is unchanged
   (`(some? seed)` etc. still throw as before — out of scope, not the
   ruled contract); a new guard, `(if (neg? seed) (result/error
   :invalid-seed {:key :seed :value seed :expected "a non-negative
   integer"}) (let [rng ...] ...))`, wraps the existing body — a
   single `if`, nothing else in the run path touched. **R10 [A, ruled
   2026-08-12]:** `engine.clj` had no existing invalid-option envelope
   to match (only the `:pre` clause's own `AssertionError` throw,
   confirmed by grep and by `components/sim/src/ehrt/sim/run.clj:164`'s
   own comment naming `:pre` assertions as the mechanism) — Read-first's
   own instruction was to STOP-AND-REPORT rather than invent a
   convention, which this session did before touching git. Options
   offered: (a) adopt `ehrt.kernel.interface`'s result-not-throw
   doctrine; (b) extend the `:pre` clause, matching the engine's
   actual but unstructured practice; (c) `ex-info` with a structured
   payload. RULED (a). `engine.clj` gains its first `ehrt.kernel.
   interface` dependency.
2. **Contract statement.** `run`'s own `:seed` docstring line gains
   "non-negative" plus the `result/error :invalid-seed` pointer. The
   two `--seed` doc strings entirely and solely governed by this
   contract — `sim run` and `sim identifiers`
   (`bases/cli/src/ehrt/cli/help.clj`) — gain "(non-negative)"/
   "non-negative"; `make cli-doc` regenerated `docs/cli.md`, delta
   exactly those two rows. A THIRD `--seed` row exists in
   `docs/cli.md` (`corpus generate`, "patient/master-generation seed
   ... shared by both sources") that the driving prompt's own evidence
   base undercounted (it said "the two `--seed` rows," there are
   three). Verified via `ehrt.corpus.sim_adapter/run!`
   (`components/corpus/src/ehrt/corpus/sim_adapter.clj`): this flag IS
   dual-source — for `:sim` it delegates in-process to the same
   `run-command` this session fixed (so the contract already applies
   transitively, no code change needed there); for `:synthea` it feeds
   an entirely different, external generator with its own, unrelated
   seed semantics. Appending "(non-negative)" to a flag description
   shared by both sources would misstate the `:synthea` half, so this
   row is deliberately left untouched, disclosed here rather than
   guessed at.
3. **Generator to contract, widened.** The originally-fenced site
   (`engine_test.clj:1172`, `mixed-authored-and-compiled-run-satisfies-
   the-full-invariant-catalog`)'s `gen/large-integer` became
   `(gen/large-integer* {:min 0})`. **Mid-session finding:** running
   the full `engine-test` namespace after this single-site fix
   surfaced a NEW failure in an untouched sibling defspec,
   `history-mode-straddling-encounter-drops-in-full-post-straddle-
   content-lands-invariant-holds`, at a freshly-drawn negative seed
   (`-1`) — a direct, witnessed consequence of the guard clause: ANY
   defspec drawing an unconstrained seed into `engine/run` is equally
   exposed. **Fence widened by author ruling** (verbatim): *"Ruled:
   widen the fence — sweep the full class this session. R9's contract
   makes every unconstrained seed generator feeding engine/run (or any
   wrapper of it) the same defect; fixing one instance while nineteen
   known instances remain would convert a known flake into a standing
   repo-wide one."* Conditions given: re-derive the inventory by
   extension-blind grep across every test tree (not trust the
   illustrative ~20-site estimate); per-site verification that each
   generated value actually flows to `:seed` on `engine/run` or a
   wrapper, ambiguous sites reported not guessed; uniform edit only;
   defspec names read as "all valid seeds" under the new contract,
   left unrenamed.

   **Full census** (extension-blind grep for `gen/large-integer`
   across every `*.clj` in the repo, then per-site verification —
   grepping each candidate file for any `engine/run`/`run-command`/
   `identifiers-command`/`engine-run-fn` reference before touching
   it): **24 sites total feed a generated `:seed` into `engine/run` or
   a wrapper** (the 1 originally fenced, plus 23 more), across 7
   files:
   - `components/sim-engine/test/ehrt/sim_engine/engine_test.clj`:
     lines 60 (`every-run-satisfies-invariant-catalog`), 66
     (`determinism-holds-for-all-seeds`), 74
     (`patient-state-is-a-fold-of-the-log`), 204
     (`bed-ready-transfer-relieves-the-longest-waiting-boarder`), 519
     (`every-churned-run-satisfies-the-invariant-catalog` — this
     defspec ALSO pins its own outer quick-check `:seed -60645`, an
     orthogonal, still-open flake investigation of its own, ADR-0076
     quality riders; this session's edit only removes the
     negative-seed hazard from its `seed` generator, it does not
     investigate or resolve ADR-0076), 702
     (`order-and-result-round-trip-through-run-for-any-seed`), 757
     (`outpatient-visits-never-occupy-a-bed-for-any-seed`), 1172 (the
     originally-fenced site), 1249
     (`history-mode-straddling-encounter-drops-...` — the site whose
     failure surfaced this whole widening).
   - `components/sim/test/ehrt/sim/identifiers_test.clj`: lines 88
     (`identifiers-command-is-deterministic`), 100
     (`identifiers-command-is-complete-against-a-real-run`).
   - `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj`:
     lines 30 (`emit-wire-with-absent-nil-or-empty-offsets-is-byte-
     identical-to-emit`), 42
     (`plan-latency-with-an-absent-profile-draws-and-discards-and-
     returns-empty`). The SAME defspec's own second generator,
     `rng-seed` at line 44, feeds `(Random. rng-seed)` for
     `plan-latency` directly, never `engine/run` — verified, left
     untouched.
   - `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/emit_hl7_test.clj`:
     lines 52, 64, 552 (via the file's own `run-with-order` helper,
     confirmed to call `engine/run` directly), 722, 908, 934.
   - `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj`:
     lines 243, 262.
   - `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/emit_fhir_test.clj`:
     lines 148, 184.
   - `components/sim-check/test/ehrt/sim_check/check_test.clj`: line
     434 (feeds `engine/run` at line 440).

   **Verified NOT affected, left untouched** (confirmed zero
   `engine/run`-family references anywhere in the file, or confirmed
   the generated value feeds something else entirely):
   `components/sim-engine/test/ehrt/sim_engine/churn_test.clj` (5
   sites — feed `(Random. seed)` directly to `churn/inject`, a pure
   function);
   `components/sim-engine/test/ehrt/sim_engine/order_profiles_test.clj`
   (2 — feed `order-profiles/sample-analyte-value`);
   `engine_test.clj:102` (`patient-id-for-differs-by-seed` — calls
   `engine/patient-id-for` directly, not `run`);
   `components/sim-model/test/ehrt/sim_model/{config,persona,facility}_test.clj`;
   `components/sim-trajectory/test/ehrt/sim_trajectory/{gmf_horizon,
   compile_trajectory,gmf_interpreter,vendored_module,
   vendored_sore_throat,vendored_tjr,vendored_appendicitis,
   vendored_injuries,vendored_ear_infections,vendored_uti}_test.clj`
   (all confirmed zero `engine/run`-family references);
   `components/corpus-io/test/ehrt/corpus_io/canonicalizers_test.clj`
   (a different generator variable, `n`, already constrained).
4. **Caller-propagation gap, found and fixed (second widening).**
   Witnessing the CLI's post-fix behavior (the author's own explicit
   instruction) surfaced a second, distinct bug: `bin/ehrt sim run
   --seed -1 --patients 1` returned `{:status :ok, :payload
   {:ground-truth nil, ... :summary {:events 0}}}` at **exit 0** —
   the engine's new `result/error` was silently swallowed.
   Root cause: `ehrt.sim.run/run-command`
   (`components/sim/src/ehrt/sim/run.clj:396`) and
   `ehrt.sim.identifiers/identifiers-command`
   (`components/sim/src/ehrt/sim/identifiers.clj:125`) both blindly
   destructure `engine-run-fn`'s return value — safe when `engine/run`
   could only throw or succeed, broken now that it can also return an
   error map (destructuring yields all-`nil` keys, `check/check-all`
   runs vacuously "ok" against nothing, and the whole thing wraps in
   `result/ok`). Fixed by author ruling: both functions now bind the
   raw `engine-result`, check `(result/error? engine-result)` as the
   first `cond`/branch, and propagate it as-is — the same convention
   every other branch in both functions already follows. Post-fix:
   `bin/ehrt sim run --seed -1 --patients 1` and `bin/ehrt sim
   identifiers --seed -1 --patients 1` both now return `{:status
   :error, :category :invalid-seed, :payload {:key :seed, :value -1,
   :expected "a non-negative integer"}}` at **exit 2**; `--seed 42`
   sanity-checked unaffected (exit 0, real output). Recorded as a
   standing lesson, `.agents/rulings.md` "From ADR-0116" R11: auditing
   every caller of a function whose return contract gains a new
   Result-typed branch is not optional when the function used to only
   throw-or-succeed.

**Green evidence, post-fix:** the new regression deftest passes; both
previously-run quick-checks re-run green (150/150 trials each) at
their own recorded seeds; the full `engine-test` namespace (10
defspecs, several plain deftests) plus `identifiers-test`,
`latency-test`, `emit-hl7-test`, `v2-replay-test`, `emit-fhir-test`,
`check-test`, `run-test`, and the untouched `churn-test`/
`order-profiles-test` (control group, confirming the sweep didn't
touch what it shouldn't have) — 265+113 tests, 919+421 assertions
across the two verification passes, 0 failures, 0 errors. Full `make
test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration` + `bin/verify-nist-lock`): green throughout, 0
FAIL/ERROR anywhere in the complete run; NIST lock OK, all 6
hit-nexus-sourced coordinates matched.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots. Every oracle root
uses a small positive seed; the only behavioral change is a guard
clause rejecting negative seeds at entry, unreachable from any root;
the cli-spec doc-string change is help text, not behavior; every
generator change is test code; the two caller-propagation fixes only
add a branch that fires on `result/error?`, never true for any
positive-seed root.

**Bracket result.** `bin/regression-oracle c6deb5a fc72f54`
(`fc72f54`: this session's own commit 1, the fix commit, run before
the close-phase commit, per this session's own driving prompt's step
ordering): `IDENTICAL: every root's digest matches between c6deb5a and
fc72f54` — all 35 roots, matching the pre-analysis exactly; no
STOP-AND-REPORT needed.

### Full gate

`make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration` + `bin/verify-nist-lock`): green — 0 FAIL/ERROR
anywhere in the complete run (every printed per-namespace summary
reported 0 failures/0 errors); `bin/verify-nist-lock`: OK, all 6
hit-nexus-sourced coordinates matched. `gitleaks git --staged -v`
(pre-commit) and `gitleaks detect` (pre-push): no leaks found.

### Deviations, dated 2026-08-12

Four STOP-AND-REPORT findings surfaced during this session, each
resolved by an explicit author ruling before proceeding (all recorded
above in Decision, cross-referenced here for the deviation record):

1. **No existing engine invalid-option convention** — Read-first's own
   named STOP condition, hit as-designed; resolved by R10 (adopt
   `ehrt.kernel.interface`).
2. **Seed `7844068501` did not reproduce when pinned** — the driving
   prompt's own STOP condition ("if either does NOT reproduce...");
   resolved by author ruling to proceed on seed `1786546687672` alone,
   without re-litigating R9.
3. **The engine-side fix broke ~20 unfenced defspecs repo-wide** — not
   a named STOP condition in the driving prompt, but squarely within
   its spirit (a conflict between the prompt's single-generator fence
   and the tree's actual behavior); resolved by the fence-widening
   ruling and the 24-site sweep documented above.
4. **`run-command`/`identifiers-command` silently swallowed the new
   error** — surfaced while executing the author's own explicit
   instruction to witness the CLI's post-fix behavior; resolved by a
   second fence-widening ruling, the caller-propagation fix documented
   above.

No other deviation: every Read-first document matched this session's
own characterization of it once these four findings were resolved; the
three-part fix, the sweep, and the caller fix all landed exactly as
ruled.

### Fences

Touched (final, widened): `components/sim-engine/src/ehrt/sim_engine/
engine.clj` (the guard clause + contract docstring + new `ehrt.kernel.
interface` dependency); `components/sim-engine/test/ehrt/sim_engine/
engine_test.clj` (9 generator sites + the new regression deftest);
`components/sim/test/ehrt/sim/identifiers_test.clj` (2 generator
sites); `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/
latency_test.clj` (2 of 3 generator sites); `components/sim-emit-hl7/
test/ehrt/sim_emit_hl7/emit_hl7_test.clj` (6 generator sites);
`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj` (2
generator sites); `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/
emit_fhir_test.clj` (2 generator sites); `components/sim-check/test/
ehrt/sim_check/check_test.clj` (1 generator site); `components/sim/
src/ehrt/sim/run.clj` and `components/sim/src/ehrt/sim/identifiers.clj`
(caller-propagation fix, gate-forced by finding 4 above); `bases/cli/
src/ehrt/cli/help.clj` (two `--seed` doc strings); `docs/cli.md`
(regenerated, exactly those two rows); `.agents/rulings.md`;
`.agents/plans/roadmap.md`; `.agents/prompts/*`; `.agents/
session-records/*`; `notes/adr/0116-*.md` (this file); `notes/ADRs.md`;
`notes/adr/README.md`. The sim purity lint's allowlist was NOT touched
(a guard clause and a Result-typed branch introduce no mutable state).
No file outside this list was touched.

### Index line

```
- 2026-08-12 — engine-seed-contract — ADR-0116
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)
