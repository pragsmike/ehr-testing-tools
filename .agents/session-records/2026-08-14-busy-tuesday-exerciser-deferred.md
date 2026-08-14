# 2026-08-14 -- Busy-tuesday exerciser: marker widening landed, row deferred on slug EDN round-trip defect (ADR-0130)

Chartered from `.agents/plans/roadmap.md`'s own "Demo exerciser
(busy-tuesday)" row, ruled front-of-queue 2026-08-13. Full analysis in
`notes/adr/0130-busy-tuesday-exerciser-deferred.md`; this record is the
chronological receipts trail.

## Step 0 -- Ceremony + conditional tag

`bin/preflight`:

```
== bin/preflight (main) ==

-- 1. Last five CI runs on main --
  green  3b30abae  2026-08-14T01:14:23Z  docs: session record close-out -- make integration + oracle at close ...
  green  594e4881  2026-08-14T00:58:57Z  docs: session record and prompt archive -- strip executability closed...
  green  cd82421e  2026-08-14T00:41:10Z  docs: session record checkpoint for Step 4 -- citation gate (ADR-0129)
  green  35bad55e  2026-08-14T00:35:38Z  feat: citation gate -- manual strip sources must be register-exercise...
  green  4bd7a177  2026-08-14T00:21:02Z  docs: session record checkpoint for Step 3 -- five strip exercisers (...
OK: last five runs all green (or none found)

-- 2. Edit-root confirmation --
OK: repo root '/home/mg/src/ehr-testing-tools' is not under /mnt/

-- 3. Tree-clean check (untracked included) --
OK: working tree clean, including untracked files

-- 4. HEAD-vs-remote tip match --
OK: local HEAD (3b30abaecb5917a731e65f3c4ab507d6a9048856) matches origin/main

-- 5. Last stable-* tag / HEAD tagged? --
Last stable-* tag: stable-20260813-hardening (56613c75c35bd1de5e9a66fb57edd84848196a6b)
DISCLOSED: HEAD is not currently tagged stable-*

== bin/preflight complete ==
```

HEAD confirmed `3b30abaecb5917a731e65f3c4ab507d6a9048856`, matching the
driving prompt's own stated premise. Per the driving prompt's own
conditional license (last-five-runs green substitutes for the
channel's own rate-limited nine-commit check), tag paid:

```
OK: created annotated tag 'stable-20260813-strip-executability' at 3b30abaecb5917a731e65f3c4ab507d6a9048856
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260813-strip-executability -> stable-20260813-strip-executability
OK: pushed refs/tags/stable-20260813-strip-executability
OK: remote peeled ref for 'stable-20260813-strip-executability' is 3b30abaecb5917a731e65f3c4ab507d6a9048856, matches target exactly
```

Oracle pre-digest (`bin/regression-oracle 3b30aba 3b30aba`): IDENTICAL,
all 35 roots (same-ref sanity check).

## STOP-AND-REPORT 1 -- register inexpressibility

Before writing any register row, the row's own "without any code
change, only data" premise was checked live rather than assumed.

`demo-exerciser-fresh/script-command-lines` against a correctly-named
busy-tuesday fixture (markers reading "BEGIN busy-tuesday
commands..."):

```
script-command-lines result (should be non-nil if markers matched):
nil
```

`strip-fresh/single-fence-command-lines` against the real busy-tuesday
README, `"bash"` fence-lang:

```
"bin/ehrt corpus generate sim --seed 20260807 --patients 200 \\"
"  --config demos/scenarios/busy-tuesday/config.edn \\"
"  --out-dir out/scenarios/busy-tuesday"
```

-- only the Generate command; the two Play commands (separate `bash`
fences) are silently dropped. Fence structure confirmed via
`strip-fresh`'s own `fenced-blocks`: 3 `bash` blocks (1/1/1 lines) + 1
untitled illustrative block (3 lines) -- no adjacency for `:paired`
either.

Reported. Author ruled (a): widen the fence to a minimal
parameterization.

## Checkpoint A -- the widening

`demo_exerciser_fresh.clj`: `script-command-lines` gained a 3-arity
overload (`marker-open`, `marker-close`), 1-arity delegates with
ed-tuesday's own literal markers as defaults; `check` gained the same
keys, `:or`-defaulted. `strip_fresh.clj`'s `:demo-exerciser-fresh`
case now passes a row's own `:marker-open`/`:marker-close` through.

New synthetic tests added first, then both src files disposably
stashed (checkpoint isolation, `git stash push --keep-index`) for an
isolated red capture:

```
FAIL in (check-honors-explicit-marker-open-and-close-test)
expected: ok?
  actual: false
ERROR in (script-command-lines-honors-a-non-ed-tuesday-marker-pair-test)
  actual: clojure.lang.ArityException: Wrong number of args (3) passed to:
  ehrt.docs-tooling.demo-exerciser-fresh/script-command-lines
```

That `ArityException` aborted the WHOLE `component:docs-tooling`
project run before `strip-fresh-test` started (`Ran 9 tests containing
16 assertions. 2 failures, 1 errors.` -- for `demo-exerciser-fresh-test`
alone; `strip-fresh-test` never printed) -- polylith's own
abort-on-first-uncaught-exception behavior, live, exactly as
SKILL.md item 7 describes. `strip-fresh-test`'s own isolated red
captured with a direct `clojure.test/run-tests` invocation instead:

```
FAIL in (check-entry-demo-exerciser-fresh-honors-a-non-ed-tuesday-marker-pair-test)
  actual: (not (true? false))
FAIL in (check-entry-demo-exerciser-fresh-catches-an-altered-script-line-test)
  actual: (not (= "bin/ehrt help --typo" :ehrt.docs-tooling.demo-exerciser-fresh/missing))
Ran 26 tests containing 50 assertions.
3 failures, 0 errors.
```

`git stash pop`; both namespaces direct-run again:

```
Testing ehrt.docs-tooling.demo-exerciser-fresh-test
Testing ehrt.docs-tooling.strip-fresh-test

Ran 35 tests containing 66 assertions.
0 failures, 0 errors.
```

Green. Checkpoint A proven red-before-green.

## A second, forced test fix

`citation-gate-test`'s own `uncovered-against-pre-session-register-
finds-the-real-dimension-1-gaps-test` simulated the ADR-0129
pre-session register by filtering on `#{:quickstart-fresh
:demo-exerciser-fresh}` -- a correct proxy only while those kinds
summed to exactly two rows. Adding the (later reverted) busy-tuesday
row broke its own sanity assertion:

```
FAIL in (uncovered-against-pre-session-register-finds-the-real-dimension-1-gaps-test)
sanity: exactly the two pre-existing pairs
expected: (= 2 (count pre-session-rows))
  actual: (not (= 2 3))
```

Retargeted to `:script` name (`#{"bin/quickstart-demo"
"bin/demo-exerciser-ed-tuesday"}`) -- keeps the test's own documented
intent accurate regardless of future rows sharing a kind. Also bumped
`exercised-sources-test`'s own count-lock 7->8 at this point (later
reverted, see below).

## Skill sentence + budget check

`build-session/SKILL.md` (+ `.claude/` mirror, `diff` confirmed
identical): one sentence appended to item 7, sanctioning a session-
record checkpoint commit ahead of `make integration` when its own
tree-clean postcondition would otherwise fail solely on in-progress
`.agents/` files (ADR-0129's own discovered practice).

`:sim` budget check: `wc -l` over the five tracked paths (including
the now-edited skill file) = 1304, against the 1495 budget -- 191
lines headroom, no bump needed.

`make test`: green (632 "0 failures, 0 errors" blocks,
`bin/verify-nist-lock` OK) after the widening + citation-gate fix +
skill sentence, before the busy-tuesday row was even added.

## The drafted row, script, and real run

Register row added (`:demo-exerciser-fresh`, honest busy-tuesday
markers), pointing at a not-yet-existing script. Live freshness check:

```
{:ok? false, :readme-count 5, :script-count 0,
 :divergence {:index 0, ... :script :ehrt.docs-tooling.demo-exerciser-fresh/missing}}
```

Red witnessed. `bin/demo-exerciser-busy-tuesday` written (full text in
`notes/adr/0130-*.md`'s own Appendix), exec bit set and verified
`100755` via `git update-index --chmod=+x` + `git ls-files -s`. Live
freshness check again:

```
{:ok? true, :readme-count 5, :script-count 5, :divergence nil}
```

Green. `make test`: green (632 blocks again, after adding the
`exercised-sources-test` busy-tuesday-row test and the live
`strip-fresh-test` delegation test).

**Real end-to-end run, in-session, real artifacts** (seed 20260807,
200 patients). Commands 1-2 succeeded; the seed-determinism contract
reproduced exactly:

```
{:status :ok, :payload {:unparseable-count 0, :snapshot-count 48, :skip-count 41,
 :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 218826,
 :stream-span-ms 279155640000, :clamped-count 0, :emitted 68,
 :unfolded-count 0, :sink "ticker"}}
```

-- matching the README's own witnessed `:emitted 68, :snapshot-count
48, :skip-count 41` exactly; `inpatients: 0` on all 48 snapshots.
Command 3 failed:

```
{:status :error, :category :play-input-unreadable,
 :payload {:path "out/scenarios/busy-tuesday/events.edn",
           :message "Invalid number: -5-day"}}
FAIL: 'bin/ehrt play out/scenarios/busy-tuesday/events.edn --rate 100000' exited 2, expected 0
```

## Root-causing the defect

```
$ grep -oE '.{20}-5-day.{5}' out/scenarios/busy-tuesday/events.edn | sort -u
, :state :cipro-500,-5-day}, :r
:state :amxclav-500,-5-day}, :r
x_tx", :state :cepha-5-day}, :r
x_tx", :state :nitro-5-day}, :r
```

`:cepha-5-day`/`:nitro-5-day` read back fine (no embedded comma).
`:cipro-500,-5-day`/`:amxclav-500,-5-day` do not -- traced to
`components/sim/resources/sim/modules/uti/abx_tx.json`:

```
"Cipro 500, 5 day": { ... }
"Cipro 250, 3 day": { ... }
```

and `ehrt.sim-trajectory.gmf/slug`
(`components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj:45-55`):

```clojure
(defn slug
  [s]
  (-> s str/lower-case (str/replace #"[_\s]+" "-")))
```

-- never sanitizes the comma; `(keyword (slug s))` (line 63) wraps it
verbatim into `:cipro-500,-5-day`, which `pr-str`s cleanly but is not
re-readable EDN (the reader treats the comma as whitespace, splitting
the token). Full disclosure, the emit-composed-with-read framing, and
scope (HL7 v2 wire path unaffected; other modules unsurveyed) in
`notes/adr/0130-*.md`.

## STOP-AND-REPORT 2 and the reduced close

Reported in full. Author ruled (b): land Checkpoint A, the
citation-gate fix, the skill sentence, and a dated roadmap correction;
revert the busy-tuesday row/script/Makefile line and the count-lock;
close ADR-0130 partial-with-open-rows (ADR-0125 precedent), two new
roadmap rows.

Revert executed:
- `bin/demo-exerciser-busy-tuesday` deleted (`rm -f` + `git restore
  --staged`, never committed); `out/scenarios/busy-tuesday` (gitignored)
  removed.
- `Makefile`: line removed; `git diff HEAD -- Makefile` -- EMPTY.
- `exercised_sources_test.clj`: count-lock reverted to 7, busy-tuesday
  test removed, docstring reverted; `git diff HEAD --
  .../exercised_sources_test.clj` -- EMPTY (exact byte-identity).
- `exercised-sources.edn`: busy-tuesday row removed; header comment
  kept, reworded to describe the widening as available to a FUTURE row
  rather than claiming busy-tuesday's own row exists.
- `strip_fresh.clj`/`strip_fresh_test.clj`: docstrings/comments
  reworded the same way; the one live busy-tuesday-row test removed,
  the two synthetic marker-pair tests (proving the widening generically)
  kept.

`clojure -M:poly check`: OK. `make test` re-run after the full revert:

```
EXIT: 0
```

green, no FAIL/ERROR (`grep` swept for both, only generative
`:result true` lines matched). Direct run of all four touched
namespaces together:

```
Testing ehrt.docs-tooling.demo-exerciser-fresh-test
Testing ehrt.docs-tooling.strip-fresh-test
Testing ehrt.docs-tooling.citation-gate-test
Testing ehrt.docs-tooling.exercised-sources-test

Ran 44 tests containing 92 assertions.
0 failures, 0 errors.
```

## Records

`notes/adr/0130-busy-tuesday-exerciser-deferred.md` (this session's
own full account, including the drafted script's own verbatim
Appendix); `notes/ADRs.md` index line; `.agents/rulings.md` "From
ADR-0130" (both rulings, verbatim); `.agents/plans/roadmap.md`: the
"Demo exerciser (busy-tuesday)" row amended in place with a dated
2026-08-14 correction (the "only data" claim), two new Next-section
rows (the slug fix, chartered as an `:sim`-family engine session with
a mandatory declared-oracle-change assessment; scenario rename +
exerciser completion, sequenced after it, name left open); `.agents/
state.md` updated; this record + its paired prompt archive
(`.agents/prompts/2026-08-14-busy-tuesday-exerciser-deferred.md`),
scaffolded via `bin/close-scaffold --expect-tag stable-20260813-strip-
executability@3b30abaecb5917a731e65f3c4ab507d6a9048856` (verified
locally and on remote before scaffolding).

## Close

Two commits, per the driving prompt's own Step 1/Step 2 split (Step
1's own commit message corrected to describe what actually landed):

1. `feat: demo-exerciser-fresh marker widening; checkpoint-commit
   practice in skill (ADR-0130)` -- the parameterization, both test
   namespaces, the citation-gate filter retarget, the skill sentence.
2. `docs: session record and prompt archive -- busy-tuesday exerciser
   deferred, slug defect disclosed (ADR-0130)` -- this record, the ADR,
   the roadmap/rulings/state.md updates, the prompt archive.

Final `make integration`, oracle bracket, and both commits' own
receipts appended below after landing.

<!-- APPEND-AFTER-CLOSE -->

