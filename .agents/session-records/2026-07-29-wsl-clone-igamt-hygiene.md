# 2026-07-29 — WSL clone-location guidance, `.gitattributes` audit, EXP-D3 citation hygiene, IGAMT disclaimer capture

## Scope

Autonomous session (R30, ADR-0007), four docs/metadata-only jobs, no
`src/` changes, four checkpoints. Rev3 of a same-day prompt — rev1
(blanket `.gitattributes` rule) was author-vetoed, rev2 (no
`.gitattributes` exists) had a false premise from an `ls`-based probe
that hid the dotfile; neither ran. This session re-ran the audit for
real and found the existing file already covers every CR-carrying
tracked file, then did three unrelated hygiene/evidence jobs riding
alongside it: a SETUP.md Troubleshooting entry for the WSL 9p/drvfs
clone-location trap (verified against a real CI run before citing its
timing), origin-qualification of stale bare `F9`/`F14`/`F16` citations
into the frozen `notes/tools/facts-register.md`, a dated
1.6.3→1.7.3 supersession note in the pre-EXP-D3 research doc, and a
verbatim facts-register capture of the NIST hosted-IGAMT registration
disclaimer.

## Red→green evidence highlights

Docs-only session; the proof is audits and gates staying green, not a
red→green test cycle.

- **`.gitattributes` coverage audit (step 1).** First attempt (`git
  grep -Il $'\r'` through the `wsl -e bash -lc "..."` wrapper) returned
  27 files — wrong, and the root cause mattered: a raw CR byte embedded
  in a `git grep` pattern argument does not survive the Bash tool →
  `wsl.exe` → `bash -lc` crossing intact, so the pattern effectively
  matched everything. Confirmed directly: `git cat-file -p
  :components/sim/src/ehrt/sim/check.clj | grep -c $'\r'` returned `0`
  for a file `git grep $'\r'` had flagged. Re-ran as a byte-level scan
  (`git ls-files` + `git cat-file -p :<path>` + `grep -P '\r'`, CR
  expressed as a Perl-regex escape rather than a raw embedded byte) and
  got exactly 14 hits: the 13 real CR-carrying tracked files, all
  already `text: unset` per `git check-attr` (PASS — matches the
  channel's own cited count exactly), plus one false positive
  (`logo.png`, the CR is byte 5 of the PNG magic number, already
  `binary`-marked and irrelevant to line-ending hazard). No
  `.gitattributes` edit needed.
- **`autocrlf=true` clone confirmation (step 2).** `git -c
  core.autocrlf=true clone . /tmp/win-sim-clone`, sha256-compared all
  13 CR-carrying files against the original: all 13 `MATCH`. Scratch
  clone removed after.
- **CI reference-timing verification (step 3).** `gh run list` →
  fetched the most recent green per-push run's full log (`gh run view
  30501195915 --log`, sha `bdffdbc5`, `conclusion: success`). Confirmed
  it runs the real `test.yml` per-push lane (`poly check` then `poly
  test :all skip:integration`, no separate integration lane on push)
  and counted exactly 177 `Testing ehrt.*` namespace announcements —
  matching the extraction session's own local count exactly, `0
  failures, 0 errors` throughout. `poly test` step: 1m46s
  (23:58:39Z→00:00:25Z); full job: 2m11s (23:58:16Z→00:00:27Z).
  Recorded as `notes/facts-register.md` F7, cited from `SETUP.md` §4.
- **`artifacts.lock.edn` EDN read-check (step 5).** `clojure -Sdeps
  '{}' -M -e '(read-string (slurp "artifacts.lock.edn")) (println
  :edn-ok)'` → printed the full parsed structure plus `:edn-ok`,
  confirming the six `:license-note` edits didn't break the EDN.
- **Gate (step 7).** `clojure -M:poly check > /tmp/poly-check.log 2>&1;
  echo EXITCODE:$?` (exit code captured separately from the log,
  [[feedback-background-pipe-exitcode]]'s own discipline, out of an
  abundance of caution even though this run wasn't backgrounded): exit
  0, log tail `OK`. Full test suite not re-run — docs/metadata only,
  per the prompt's own step 7 allowance; nothing here touched a brick.

## Judgment calls and their ratification status

Autonomous session, no author present to ask — every call below is
this session's own, per the prompt's decision procedures, not
individually ratified yet:

- **Byte-level CR scan method substituted for the prompt's literal
  `git grep -Il $'\r'` suggestion**, once the first attempt's spurious
  27-file result was traced to the wrapper-crossing hazard above. The
  prompt's own step 1 already anticipated a possible under/over-count
  ("plus a scan for CR-carrying files git classifies as binary");
  treated the wrapper hazard as exactly the kind of "expected result
  doesn't hold, stop, record, adapt" case AGENTS.md's own R10 discipline
  covers, not silently patched over.
- **F7 (CI verification) and F8 (IGAMT disclaimer) both newly added**
  this session, "next free number" evaluated at the point each was
  actually drafted (step 3 then step 6), not reserved in advance — the
  register grew by two rows, not the one the ruling's singular "a new
  facts-register row" phrasing (for the IGAMT job specifically) might
  suggest in isolation; step 3's own ruling separately calls for its
  own new row.
- **Only F4 and F5 actually needed origin-qualification**, not F6 — the
  ruling's "rows F4–F6" was read as the area to inspect, and F6 (both
  Index and Register) had no bare `F9`/`F14`/`F16` citation on direct
  read. Fixed exactly what needed it, recorded the discrepancy in the
  archived prompt's deviation record rather than inventing a change to
  F6 to match the ruling's literal row span.
- **§D3 supersession note placed inline in the Maintenance line and
  echoed in the Evidence line**, both in
  `components/tools/docs/research/EHR-testing-tools-selection-research.md`,
  rather than a separate addendum paragraph — kept the correction next
  to the specific claim it corrects, matching how F4's own SSL-claim
  supersession note reads (inline in the facts-register row, not a
  separate document section).
- **IGAMT row's three scope notes labeled (i)/(ii)/(iii)** verbatim in
  the order the ruling gave them, each naming its own concrete
  downstream consequence (the open `tools/NIST-reply` inquiry; the
  `:license-note` obligation a future `:profile` artifact must carry;
  self-hosted IGAMT as the proprietary-work escape hatch) rather than
  compressing them into one paragraph — judged that future readers
  citing this row will want to pull one scope note out on its own.

## Findings and HEAD landed

Four checkpoints, four commits, each pushed immediately (R30 mode):

1. `1fe2e16` — `docs: SETUP troubleshooting -- WSL 9p clone/agent-tooling trap; CI reference timing verified` (`SETUP.md`, `notes/facts-register.md` F7). This push also carried the prior session's unpushed `bdffdbc` along with it.
2. `b58834c` — `docs: origin-qualify frozen-register citations; record 1.6.3->1.7.3 supersession (EXP-D3 errata)` (`artifacts.lock.edn`, `components/tools/docs/research/EHR-testing-tools-selection-research.md`, `notes/facts-register.md`).
3. `71a7dc9` — `docs: facts register -- IGAMT registration disclaimer captured verbatim (licensing evidence, profile-artifact notice obligation)` (`notes/facts-register.md` F8).
4. This session record + archived prompt (`notes/prompts/2026-07-29-ehr-testing-wsl-clone-igamt-hygiene.md`), committed and pushed as the close-phase checkpoint.

`clojure -M:poly check`: `OK` at close, no brick touched, no full-suite
re-run needed. gitleaks: clean at every push (no leaks, commit counts
scanned rising each time as expected).
