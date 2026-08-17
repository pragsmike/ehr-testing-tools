## ADR-0130 — Busy-tuesday exerciser: marker widening landed, row deferred on a real slug EDN round-trip defect

**Status:** Accepted (author-directed, autonomous session per R30, TWO
in-session STOP-AND-REPORTs, both ruled), 2026-08-14.

### Context

Chartered from `.agents/plans/roadmap.md`'s own "Demo exerciser
(busy-tuesday)" row (ADR-0120, "not chartered to any executing session
yet"), ruled front-of-queue 2026-08-13. R3 (`notes/ADRs.md` ADR-0113,
author verbatim): *"The demos must be known to work, and exercised as
documented to make sure they actually play out as written."* The
row's own charter, restated by the driving prompt: extend the
`ehrt.docs-tooling.exercised-sources` register (ADR-0129) with one new
row for `demos/scenarios/busy-tuesday/README.md`, "without any code
change, only data" — if a code change proved genuinely necessary, STOP
and report why.

### Step 0 — Ceremony and conditional tag

`bin/preflight` at session start: last five CI runs on `main` all
green (`3b30abae`, `594e4881`, `cd82421e`, `35bad55e`, `4bd7a177`);
edit-root ext4; tree clean; local HEAD matched `origin/main` at
`3b30abaecb5917a731e65f3c4ab507d6a9048856`; last `stable-*` tag
`stable-20260813-hardening`, HEAD not yet tagged. Per the driving
prompt's own conditional license (the design channel could not confirm
CI on the nine ADR-0129 commits directly, API rate-limited; the
last-five-runs check substitutes), tag paid: `stable-20260813-strip-
executability` created ANNOTATED at `3b30aba`, pushed, peeled ref
verified exact match. Oracle pre-digest (`bin/regression-oracle 3b30aba
3b30aba`): IDENTICAL, all 35 roots — the same-ref sanity check this
repo's own precedent uses at session start.

### STOP-AND-REPORT 1 — register inexpressibility, verified live

Before writing a single register row, the driving prompt's own
contingency ("if you find a code change genuinely necessary, STOP and
report why") was checked against the tree rather than assumed clear.
Two claims, both verified empirically, not just by reading:

- **`ehrt.docs-tooling.demo-exerciser-fresh/script-command-lines`
  hardwired ed-tuesday's own literal BEGIN/END marker text as private
  constants inside the function body**, not a parameter — confirmed by
  building a correctly-labeled, honestly-named fake
  `bin/demo-exerciser-busy-tuesday` fixture (markers reading "BEGIN
  busy-tuesday commands...") and calling `script-command-lines`
  against it directly: returned `nil`. `ehrt.docs-tooling.strip-
  fresh`'s own `:demo-exerciser-fresh` case never passed a register
  row's own `:marker-open`/`:marker-close` keys through either, so a
  busy-tuesday row would have carried those keys as silent dead data.
- **`ehrt.docs-tooling.strip-fresh/single-fence-command-lines` only
  captures the FIRST matching fence** — busy-tuesday's README has
  THREE separate `` ```bash `` fences (Generate, Play `--board`, Play
  `events.edn`), confirmed live: `single-fence-command-lines` against
  the real README returned only the Generate command, silently
  dropping both Play commands. `:paired` does not apply either (no
  fence is immediately followed by an output fence in this README).

None of the four existing extraction kinds expressed the row as pure
data. Reported to the author; **ruled (a):** widen the fence to a
minimal parameterization exactly as proposed.

### Checkpoint A — the widening, landed

`ehrt.docs-tooling.demo-exerciser-fresh`: `script-command-lines`
gained a 3-arity overload (`marker-open`, `marker-close`), the 1-arity
form delegating to it with ed-tuesday's own literal markers as
defaults (extracted into `default-marker-open`/`default-marker-close`
private constants); `check` gained the same `:marker-open`/
`:marker-close` keys in its options map, `:or`-defaulted the same way.
`ehrt.docs-tooling.strip-fresh`'s own `:demo-exerciser-fresh` branch in
`check-entry` now passes a register row's own `:marker-open`/
`:marker-close` through rather than dropping them. Every pre-ADR-0130
call site stays byte-identical in behavior (same defaults in, same
results out) — every existing test in `demo-exerciser-fresh-test`
stayed unmodified and green throughout.

**Red-before-green, disposable-stash isolation** (the build-session
skill's own checkpoint-isolation practice, item 7): new synthetic tests
added first (non-ed-tuesday marker pairs, in both
`demo-exerciser-fresh-test` and `strip-fresh-test`), then the two src
files (`demo_exerciser_fresh.clj`, `strip_fresh.clj`) disposably
stashed, isolating exactly the unfixed code:

```
FAIL in (check-honors-explicit-marker-open-and-close-test)
expected: ok?
  actual: false
ERROR in (script-command-lines-honors-a-non-ed-tuesday-marker-pair-test)
  actual: clojure.lang.ArityException: Wrong number of args (3) passed to:
  ehrt.docs-tooling.demo-exerciser-fresh/script-command-lines
```

**A live demonstration of the skill's own warning, not just its
text.** That `ArityException` aborted the ENTIRE `component:docs-
tooling` project's test run before `strip-fresh-test` even started —
polylith's own "aborts on the FIRST uncaught exception" behavior,
exactly as item 7 describes. `strip-fresh-test`'s own isolated red had
to be captured with a direct namespace invocation
(`clojure.test/run-tests` outside `poly test`) instead:

```
FAIL in (check-entry-demo-exerciser-fresh-honors-a-non-ed-tuesday-marker-pair-test)
  actual: (not (true? false))
FAIL in (check-entry-demo-exerciser-fresh-catches-an-altered-script-line-test)
  actual: (not (= "bin/ehrt help --typo" :ehrt.docs-tooling.demo-exerciser-fresh/missing))
Ran 26 tests containing 50 assertions.
3 failures, 0 errors.
```

`git stash pop`, re-run both namespaces directly: `Ran 35 tests
containing 66 assertions. 0 failures, 0 errors.` Green.

**A second, smaller test-correctness fix, forced by the same
widening's own downstream register growth (not a Checkpoint-A defect
itself).** `ehrt.docs-tooling.citation-gate-test`'s own
`uncovered-against-pre-session-register-finds-the-real-dimension-1-
gaps-test` simulated "the register as it stood before ADR-0129" by
filtering on `#{:quickstart-fresh :demo-exerciser-fresh}` extraction
kinds — a correct proxy for "exactly the two original rows" only as
long as those two kinds summed to two rows total. Adding the (later
reverted, see below) busy-tuesday row broke the proxy's own sanity
assertion (`(= 2 (count pre-session-rows))` saw 3). Retargeted the
filter to `:script` name (`#{"bin/quickstart-demo"
"bin/demo-exerciser-ed-tuesday"}`), which keeps the test's own
documented intent accurate regardless of how many future rows share an
extraction kind — a one-line, disclosed test fix, not a scope
expansion.

### Skill sentence (Step 1(iv))

`build-session/SKILL.md` (+ `.claude/` mirror, diffed identical):
appended to item 7 (Checkpoint isolation), sanctioning a small
session-record checkpoint commit ahead of the final `make integration`
run whenever that run's own tree-clean postcondition would otherwise
fail solely because this session's own in-progress `.agents/` files
are still uncommitted — ADR-0129's own discovered practice, now
written down rather than re-discovered per session. Budget check
before committing: `:sim`'s five tracked paths (including this skill
file) measured 1304 lines against its own 1495-line budget after the
edit — 191 lines of headroom, no bump needed.

### The drafted row, script, and real run — where the second STOP came from

With the widening landed and proven, the busy-tuesday register row was
added (`:demo-exerciser-fresh`, its own honest markers: `# BEGIN
busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/
README.md)` / `# END busy-tuesday commands`), pointing at a script
that did not yet exist. Freshness case red-witnessed live
(`check-entry` on the real row: `:ok? false, :script-count 0,
:divergence {... :script :ehrt.docs-tooling.demo-exerciser-fresh/
missing}`). `bin/demo-exerciser-busy-tuesday` was then written, modeled
on `bin/demo-exerciser-ed-tuesday`'s own shape, with one deliberate
departure the driving prompt required: its own two invariant checks
(the "every board snapshot reads `inpatients: 0`" claim and the full
closing-summary EDN map) were extracted from the README's own prose AT
RUNTIME via a small inline `clojure -M:dev:test -e` call reusing
`ehrt.docs-tooling.strip-fresh/subset-match?` (already generic,
unmodified) rather than hand-copied as bash literals — so a future
re-witness of these figures (ADR-0103's own precedent, which DID move
this exact scenario's numbers once already) cannot silently desync
from the check. Freshness case re-witnessed green (`:ok? true,
:readme-count 5, :script-count 5`). `make test`: green (632 "0
failures, 0 errors" blocks, `bin/verify-nist-lock` OK).

**The real end-to-end run, in-session, real artifacts** (seed 20260807,
200 patients): commands 1 and 2 succeeded, and the seed-determinism
contract reproduced EXACTLY — `:emitted 68, :snapshot-count 48,
:skip-count 41`, `inpatients: 0` on every one of 48 board snapshots.
Command 3 failed:

```
{:status :error, :category :play-input-unreadable,
 :payload {:path "out/scenarios/busy-tuesday/events.edn",
           :message "Invalid number: -5-day"}}
```

### The defect, full disclosure

**Root cause, traced to source.** `ehrt.sim-trajectory.gmf/slug`
(`components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj:45-55`):

```clojure
(defn slug
  [s]
  (-> s str/lower-case (str/replace #"[_\s]+" "-")))
```

lower-cases a raw GMF name and collapses runs of underscores/
whitespace to a single hyphen, but sanitizes NO other punctuation.
`(keyword (slug s))` (line 63) then wraps the result verbatim. Upstream
Synthea state names are free text and can legitimately carry a comma —
`components/sim/resources/sim/modules/uti/abx_tx.json` (part of busy-
tuesday's own twelve-module mix) names two states `"Cipro 500, 5 day"`
and `"Cipro 250, 3 day"`. `slug` turns the first into
`"cipro-500,-5-day"`; `keyword` wraps it to `:cipro-500,-5-day`.

**The specimen.** `:cipro-500,-5-day` prints via `pr-str` without
complaint — `keyword` places no restriction on its argument string's
characters, and the printer just emits them. But reading that printed
text back is not a valid round trip: the EDN/Clojure reader treats a
comma as whitespace, full stop, so the token splits into `:cipro-500,`
(the keyword ends at the comma, which is then consumed as separator)
and a SEPARATE bare token `-5-day` — which the reader tries to parse
as a number (leading `-` followed by a digit) and fails on, since it
has trailing non-numeric characters. Ten instances of `:nitro-5-day`/
`:cepha-5-day` (which have no embedded comma, since "Nitro 5 day"/
"Cepha 5 day" have none in that position) print and read back fine;
only the `abx_tx.json` states whose own upstream English name embeds a
comma break. `:cipro-500,-5-day` and `:amxclav-500,-5-day` were the two
distinct broken tokens found in this run's own `events.edn`.

**The law violated, stated plainly.** This project constructs many
keywords from upstream free text via `slug`/`keyword` and later
`pr-str`s the structures containing them to disk (`events.edn`) or
reads them back (`ehrt play events.edn`). The implicit contract every
other keyword this project constructs satisfies is a round-trip
identity: emit composed with read is identity — `(= k (edn/read-string
(pr-str k)))` for every keyword `k` this project's own compiler
produces. `slug`'s incomplete sanitization violates that law for any
upstream state name carrying a comma (or, unaudited, potentially other
reader-significant characters — semicolons, unmatched quotes,
leading digits after a hyphen run). This is a NEW finding — no prior
ADR or roadmap entry names it; searched before writing this ADR,
confirmed absent.

**Scope, disclosed honestly, not chased further.** The HL7 v2 wire
path (`bin/ehrt play --board`, command 2, same run) never hit this —
`:citation {:state ...}` is ground-truth-only metadata, never rendered
onto the wire, so it never round-trips through `pr-str`/`edn/read-
string` on that path. Whether any OTHER already-vendored module (of
the 35 oracle roots, or the wider vendored catalog beyond it) carries
a comma or other reader-unsafe character in a state name was NOT
surveyed this session — that census is chartered as a required step of
the future fix session below, not performed here (out of this
session's own fence: no `sim-trajectory`/module-content edit is
licensed by this driving prompt).

### STOP-AND-REPORT 2 and disposition

Reported to the author in full (root cause, specimen, the emit-
composed-with-read law, scope). **Ruled (b), reduced close:**

- **Land:** Checkpoint A (the marker-open/marker-close widening, both
  test namespaces), the citation-gate-test filter retarget, the skill
  sentence, and a dated roadmap correction.
- **Revert:** the exercised-sources count-lock back to 7 (from the
  drafted 8); do NOT land the busy-tuesday register row, script, or
  Makefile line. The design is preserved in this record and this
  session's own archived prompt (the drafted script's full text is
  reproduced in the Appendix below, for direct recovery).
- **Close this ADR partial-with-open-rows**, per the ADR-0125
  precedent (a fail-grade finding closes its own arc/session with the
  finding landed as open backlog rows, not silently re-attempted or
  left unrecorded) — full disclosure above, plus two new
  `.agents/plans/roadmap.md` Next-section rows: (1) the slug EDN-
  round-trip fix itself, chartered as an `:sim`-family engine session
  with a red-before-green PROPERTY test and a MANDATORY declared-
  oracle-change assessment before landing (since `slug` compiles every
  module's own state/attribute names, not only `abx_tx.json`'s — a fix
  could move any OTHER already-vendored module's own compiled keyword
  value, and therefore its emitted ground truth, if that module's own
  state names carry a character `slug` doesn't currently sanitize); (2)
  scenario rename + busy-tuesday exerciser completion, sequenced AFTER
  (1), the scenario's own future name left as an explicit open
  question for the author rather than assumed.

### Revert, verified clean

`bin/demo-exerciser-busy-tuesday` deleted (never committed — added and
unstaged in the same working session, never reached a commit).
`out/scenarios/busy-tuesday` (gitignored) removed. `Makefile`: diffed
against `HEAD` after reverting the added line — EMPTY diff, exact
byte-identity restored. `components/docs-tooling/test/ehrt/docs_tooling/
exercised_sources_test.clj`: diffed against `HEAD` after reverting the
count-lock and the busy-tuesday-specific test — EMPTY diff, exact
byte-identity restored. `exercised-sources.edn`: the busy-tuesday row
removed; the header comment's own `:demo-exerciser-fresh` description
kept (describes the now-landed widening mechanism itself, reworded to
describe it as available to a FUTURE row rather than claiming
busy-tuesday's own row already exists). `strip_fresh.clj`'s own
namespace docstring similarly reworded. `strip_fresh_test.clj`: the one
live-delegation test naming the real busy-tuesday row/script removed;
the two SYNTHETIC marker-pair tests (which prove Checkpoint A's own
passthrough generically, on fixtures, not on the real row) kept.

Full `make test` re-run after every revert: green throughout (final
run: exit 0, `bin/verify-nist-lock` OK, zero FAIL/ERROR beyond
generative-test `:result true` lines). Direct namespace run of all four
touched test namespaces together: `Ran 44 tests containing 92
assertions. 0 failures, 0 errors.`

### Oracle bracket

Pre-analysis: pure identity expected — every landed change is
`components/docs-tooling/{src,test}` (not an oracle root) plus
`Makefile` (net no-op after revert), `.agents/`, `notes/`. `bin/
regression-oracle 3b30abaecb5917a731e65f3c4ab507d6a9048856
06aec01669a91273fe8ce6a0b84b017042f0f228` (the session's own two real
commits — `b3483dc0`, the widening; `06aec016`, this record and
close): **IDENTICAL, all 35 roots.** Matches the pre-analysis exactly.
Full receipts in this session's own record,
`.agents/session-records/2026-08-14-busy-tuesday-exerciser-
deferred.md`.

### Verification

`clojure -M:poly check`: OK. `make test`: green multiple times across
the session (after Checkpoint A landed; after the citation-gate/
count-lock fixes; after the revert) — see the session record for each
run's own tally. `gitleaks git --staged -v` clean at every commit
(tag-ceremony's own invocation and each commit's own staging pass).
Exec bit: N/A this close (the one new file, `bin/demo-exerciser-busy-
tuesday`, was deleted before commit — no new executable lands).

### Fences

Touched: `components/docs-tooling/{src,test}/ehrt/docs_tooling/
{demo_exerciser_fresh,strip_fresh}.clj` + their test files,
`components/docs-tooling/test/ehrt/docs_tooling/citation_gate_test.clj`,
`components/docs-tooling/resources/docs-tooling/exercised-sources.edn`
(comment only, net content unchanged in row count), `.agents/skills/
build-session/SKILL.md` + `.claude/` mirror (one sentence), `.agents/
plans/roadmap.md` (dated correction + two new rows), `.agents/
rulings.md`, `notes/ADRs.md`, `notes/adr/0130-*.md` (this file),
`.agents/state.md`, `.agents/session-records/*`, `.agents/prompts/*`.
`Makefile` touched then reverted to byte-identity. `bin/demo-exerciser-
busy-tuesday` drafted then deleted, never committed. ZERO
`demos/scenarios/*/README.md` edits (no figure/README edit, per the
driving prompt's own instruction on invariant non-reproduction —
applied here to the more fundamental command failure this session
actually hit). ZERO `sim-trajectory`/module-content edit — the slug
defect is disclosed, not fixed, per this session's own fence.

### Deviations

**Both STOP-AND-REPORTs are disclosed as license, not deviation** — the
driving prompt's own fence names both trigger classes explicitly
("register inexpressibility"; "invariant non-reproduction" / "red").
Stopping twice and asking is compliance with the driving prompt, not a
departure from it. Both rulings and their dispositions are recorded in
`.agents/rulings.md` "From ADR-0130" and executed above.

**The original Step 1 commit message template
(`feat: busy-tuesday exerciser -- register row, script, integration
wiring; checkpoint-commit practice in skill (ADR-0130)`) no longer
describes what landed** — corrected to name the parameterization
actually shipped; see the real commit message below.

### Index line

```
- 2026-08-14 — busy-tuesday-exerciser-deferred — ADR-0130
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Appendix — the drafted, never-committed `bin/demo-exerciser-busy-tuesday`

Preserved verbatim for direct recovery by the future session that
lands row (2) above, once the slug defect is fixed. This is the
EXACT text drafted and used for this session's own real end-to-end
run (the one that caught the defect) — not re-derived from memory.

```bash
#!/usr/bin/env bash
# Runs demos/scenarios/busy-tuesday/README.md's own fenced command
# sequence, verbatim, under per-step exit-code assertions plus the
# README's own named invariants -- R3 (notes/ADRs.md ADR-0113, author
# verbatim): "The demos must be known to work, and exercised as
# documented to make sure they actually play out as written."
# Generalizes bin/demo-exerciser-ed-tuesday's own shape (ADR-0120) to a
# second scenario README (ADR-0130) -- same expect/expect_eval wrapper,
# same tree-clean postcondition, but this one's invariant checks
# RE-DERIVE the README's own witnessed figures LIVE from the README's
# own prose at runtime (the inline `inpatients: 0` claim and the
# inline closing-summary EDN map), rather than hand-copying them into
# this script as bash literals -- a future re-witness of these figures
# (the ADR-0103 boundary-catch-up precedent, which DID move this exact
# scenario's own numbers once already) changes the README and this
# script's own check stays correct with zero edit here.
#
# The block between the BEGIN/END markers is read by
# ehrt.docs-tooling.strip-fresh (via the exercised-sources register)
# to prove this script and the README's own three fenced commands
# teach the identical commands, in the identical order. Keep every
# taught line inside that block, one statement per README command, and
# do not "improve" a command here -- an improvement belongs in the
# README first (AUTHORS-GUIDE.md sec7's craft discipline). Integration-
# tier only (`make integration`), never per-push CI -- a 200-patient,
# ten-year-horizon corpus generation plus two board plays is real work,
# the same tier bin/demo-exerciser-ed-tuesday's own siblings already use.
set -uo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)" || exit 2
cd -- "$repo_root" || exit 2

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

STEP=0
LOG_DIR="$(mktemp -d)"
export LOG_DIR
trap 'rm -rf "$LOG_DIR"' EXIT

# expect CODE CMD...  Same shape as bin/demo-exerciser-ed-tuesday's own
# wrapper: runs CMD with its own argv (no shell reinterpretation),
# asserts exit status CODE, tees stdout/stderr to this step's own log
# file under LOG_DIR (echoed back live for a human running this script).
expect() {
  local want="$1"; shift
  STEP=$((STEP + 1))
  "$@" > "$LOG_DIR/step-$STEP.out" 2> "$LOG_DIR/step-$STEP.err"
  local got=$?
  cat "$LOG_DIR/step-$STEP.out"
  cat "$LOG_DIR/step-$STEP.err" >&2
  if [ "$got" -ne "$want" ]; then
    fail "'$*' exited $got, expected $want"
  fi
}

echo "== demo-exerciser-busy-tuesday: demos/scenarios/busy-tuesday/README.md, verbatim, asserted =="

started_s="$(date +%s)"

# Script-only apparatus, not a taught line (outside BEGIN/END, so
# demo-exerciser-fresh's freshness check never sees it): the README's
# own Generate section states --out-dir is rejected if it already
# exists and is non-empty -- this script must be safely re-runnable, a
# first-time reader's own repo starts clean and never needs this.
rm -rf out/scenarios/busy-tuesday

# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)
expect 0 bin/ehrt corpus generate sim --seed 20260807 --patients 200 \
  --config demos/scenarios/busy-tuesday/config.edn \
  --out-dir out/scenarios/busy-tuesday

expect 0 bin/ehrt play out/scenarios/busy-tuesday --board 60 --rate 100000

expect 0 bin/ehrt play out/scenarios/busy-tuesday/events.edn --rate 100000
# END busy-tuesday commands

ended_s="$(date +%s)"
echo "== demo-exerciser-busy-tuesday: full run wallclock $((ended_s - started_s))s =="

echo "== demo-exerciser-busy-tuesday: checking named invariants (re-derived live from the README) =="

# "every board snapshot reads `inpatients: 0`" -- the exact literal is
# read from the README's own prose at runtime, never hand-copied here,
# so a future re-witness that changes this figure (ADR-0103's own
# precedent for this exact scenario) cannot silently desync from this
# check. Step 2 is the --board play.
clojure -M:dev:test -e '
(require (quote [clojure.string :as str]))
(let [readme (slurp "demos/scenarios/busy-tuesday/README.md")
      claim-match (re-find #"every board snapshot reads `([^`]+)`" readme)]
  (if-not claim-match
    (do (println "FAIL: could not find the README'"'"'s own \"every board snapshot reads ...\" inline claim")
        (System/exit 1))
    (let [claim (second claim-match)
          out-lines (str/split-lines (slurp (str (System/getenv "LOG_DIR") "/step-2.out")))
          snapshot-lines (filter #(str/includes? % "inpatients:") out-lines)]
      (if (and (seq snapshot-lines) (every? #(str/includes? % claim) snapshot-lines))
        (do (println (str "OK: all " (count snapshot-lines) " board-snapshot lines contain the README'"'"'s own re-derived claim " (pr-str claim)))
            (System/exit 0))
        (do (println (str "FAIL: not every board-snapshot line contains " (pr-str claim) " -- the README'"'"'s own zero-inpatient claim does not hold on this run"))
            (doseq [l snapshot-lines] (println "  " l))
            (System/exit 1))))))
' || fail "zero-inpatient invariant did not hold (see FAIL detail above)"

# "The run's own closing summary: `{:unparseable-count 0, ...}`" -- the
# whole map is read from the README's own prose at runtime (a Clojure
# regex, (?s) mode, over the slurped file -- the map's own text wraps
# across several physical markdown lines), then subset-matched against
# the real, freshly captured run's own final printed EDN envelope.
# :wallclock-ms is excluded as genuinely run-volatile (disclosed,
# per this session's own fence); every other key -- :emitted 68,
# :snapshot-count 48, :skip-count 41, :unparseable-count 0, :rate,
# :idle-cap-ms, :stream-span-ms, :clamped-count, :unfolded-count,
# :sink -- must match exactly, or this is a FINDING, not a silently
# tolerated drift.
clojure -M:dev:test -e '
(require (quote [clojure.edn :as edn])
         (quote [clojure.string :as str])
         (quote [ehrt.docs-tooling.strip-fresh :as sf]))
(let [readme (slurp "demos/scenarios/busy-tuesday/README.md")
      summary-match (re-find #"(?s)The run.s own closing summary: `(\{.*?\})`" readme)]
  (if-not summary-match
    (do (println "FAIL: could not find the README'"'"'s own closing-summary inline EDN span")
        (System/exit 1))
    (let [expected (dissoc (edn/read-string (second summary-match)) :wallclock-ms)
          out-lines (remove str/blank? (str/split-lines (slurp (str (System/getenv "LOG_DIR") "/step-2.out"))))
          actual (:payload (edn/read-string (last out-lines)))]
      (if (sf/subset-match? expected actual)
        (do (println (str "OK: closing-summary invariants match the README'"'"'s own re-derived claim (:wallclock-ms excluded as run-volatile): " (pr-str expected)))
            (System/exit 0))
        (do (println "FAIL: closing-summary mismatch -- seed 20260807 determinism did not reproduce")
            (println (str "  expected (README, :wallclock-ms excluded): " (pr-str expected)))
            (println (str "  actual:   " (pr-str actual)))
            (System/exit 1))))))
' || fail "closing-summary invariant did not hold (see FAIL detail above)"

# Tree-clean postcondition (ADR-0005's own discipline, mirrored from
# bin/demo-exerciser-ed-tuesday): every write path above lands under
# the gitignored /out/ -- a full run must leave the tracked tree
# exactly as it found it.
dirty="$(git status --porcelain)"
if [ -n "$dirty" ]; then
  fail "tree not clean after a full run (ADR-0005 postcondition violated):
$dirty"
fi

echo "== demo-exerciser-busy-tuesday: every command asserted, every named invariant held, tree clean =="
```

This script's own real run (this session, seed 20260807) is the one
whose output is quoted above under "The drafted row, script, and real
run": it correctly asserted commands 1-2, correctly re-derived and
matched both invariants against the real captured output, and then
correctly FAILED at command 3 on the real, previously-undisclosed
defect — it did exactly what it was built to do.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Busy-tuesday exerciser: marker widening landed, row deferred on a real slug EDN round-trip defect — tags `stable-20260813-strip-executability` at `3b30aba` (ADR-0129's own close, CI-verified via `bin/preflight`'s last-five-runs check per this session's own conditional license); TWO in-session STOP-AND-REPORTs, both ruled. First: the row's own "without any code change, only data" charter did NOT hold -- `ehrt.docs-tooling.demo-exerciser-fresh`'s own script-side marker text was hardwired to ed-tuesday literally (verified empirically, not just read), and `:single-fence` only captures a doc's FIRST matching fence, short of busy-tuesday's own three separate `` ```bash `` fences -- ruled (a): widen the fence to a minimal, backward-compatible `marker-open`/`marker-close` parameterization, landed with red-before-green proof via disposable-stash isolation in both `demo-exerciser-fresh-test` and `strip-fresh-test` (the isolation itself catching a live demonstration of polylith's own abort-on-first-exception behavior); forced a second, one-line test-correctness fix in `citation-gate-test` (a pre-session-register simulation's own extraction-kind filter, now retargeted to script name). Second: the drafted (then reverted) busy-tuesday row/script's own real end-to-end run reproduced the seed-20260807 determinism contract exactly (`:emitted 68, :snapshot-count 48, :skip-count 41`, zero inpatients throughout) on commands 1-2, then hit a genuine, previously-undisclosed defect on command 3 -- `ehrt.sim-trajectory.gmf/slug` never sanitizes commas out of raw upstream Synthea state names before constructing a keyword (`uti/abx_tx.json`'s own `"Cipro 500, 5 day"` -> `:cipro-500,-5-day`), producing a keyword that `pr-str`s fine but is not re-readable EDN, breaking `ehrt play events.edn`'s own read-back -- an emit-composed-with-read identity law this project's own compiled keywords otherwise satisfy, violated for this specimen. Ruled (b), reduced close: land the marker widening, the citation-gate fix, and the skill sentence (checkpoint-commit practice, ADR-0129's own discovered practice, now written into `build-session/SKILL.md` + `.claude/` mirror); revert the busy-tuesday register row/script/Makefile line and the register count-lock (7, unchanged) -- the design is fully preserved in this record's own Appendix; close this ADR partial-with-open-rows per the ADR-0125 precedent, with two new roadmap Next-section rows (the slug fix itself, chartered as an `:sim`-family engine session with a mandatory declared-oracle-change assessment; scenario rename + exerciser completion, sequenced after it, the scenario's own future name left open for the author). Zero `demos/` README edits, zero `sim-trajectory`/module-content edits -- the oracle holds pure identity across all 35 roots

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0130 (busy-tuesday exerciser: marker widening landed, row
deferred on a real slug EDN round-trip defect; ruled 2026-08-14, both
rulings restated verbatim from this session's own chat exchange)

- **Register inexpressibility, ruled (a)** [A, 2026-08-14, "Ruled (a):
  fence widened to the minimal parameterization exactly as you
  proposed — marker-open/marker-close params with ed-tuesday defaults,
  strip-fresh passes register keys through, existing ed-tuesday
  row/script/tests byte-unmodified and green, red-before-green on the
  new path. Correct the roadmap row's 'only data' claim in place in
  Step 2, dated, citing ADR-0130. Resume."]: executed exactly as
  ruled — `ehrt.docs-tooling.demo-exerciser-fresh`'s own `script-
  command-lines`/`check` widened to an explicit `marker-open`/
  `marker-close` pair (ed-tuesday's own literal markers as the
  default), `ehrt.docs-tooling.strip-fresh`'s own `:demo-exerciser-
  fresh` case in `check-entry` now passes a register row's own markers
  through; red-before-green proven via disposable-stash isolation,
  both `demo-exerciser-fresh-test` and `strip-fresh-test`; the
  roadmap's own "Demo exerciser (busy-tuesday)" row corrected in place
  with a dated 2026-08-14 note, citing ADR-0130.
- **Slug defect, ruled (b), reduced close** [A, 2026-08-14, "Ruled (b),
  reduced close: land Checkpoint A (parameterization + both test
  namespaces), the citation-gate filter retarget, the skill sentence,
  and the dated roadmap correction. Revert the exercised-sources
  count-lock to 7 and do NOT land the busy-tuesday register row,
  script, or Makefile line — the design is preserved in this prompt's
  archive and the ADR. Close ADR-0130 as partial-with-open-rows per
  the ADR-0125 precedent: full slug-defect disclosure (root cause, the
  :cipro-500,-5-day specimen, the emit ⨟ read = id framing), plus two
  roadmap rows: (1) slug EDN-round-trip fix — engine session,
  red-before-green property test, mandatory declared-oracle-change
  assessment; (2) scenario rename + exerciser completion, sequenced
  after (1), name slot open for author ruling. Everything else per the
  original Step 2."]: executed exactly as ruled — the busy-tuesday
  register row, drafted script, and Makefile line all reverted to
  byte-identity with `HEAD`, the count-lock reverted to 7; ADR-0130
  closed partial-with-open-rows, full disclosure landed in
  `notes/adr/0130-*.md` (including the drafted script's own full text,
  preserved verbatim in that record's Appendix for direct recovery);
  two new `.agents/plans/roadmap.md` Next-section rows chartered as
  ruled.
