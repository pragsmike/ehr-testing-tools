## ADR-0120 — User manual S2: demo exerciser and chapter 3, a simulated hospital

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

S2 of the five-session user-manual arc (ADR-0119's own charter),
landing R3's own charter — author verbatim, `notes/ADRs.md` ADR-0113:
*"The demos must be known to work, and exercised as documented to make
sure they actually play out as written."* — as a demo exerciser for
`ed-tuesday`, co-landed with Chapter 3
(`docs/manual/03-a-simulated-hospital.md`), matching the R3/R5 sequence
already ruled ("the demo exerciser co-landed with the first chapter
that cites a demo"). Read first:
`bin/quickstart-demo` + the `quickstart`/`quickstart-fresh` Makefile
targets and `ehrt.docs-tooling.quickstart-fresh` (the pattern this
generalizes); `demos/scenarios/ed-tuesday/README.md` (every fenced
command block and each section's own stated invariants);
`docs/dev/simulator-architecture.md` section 4 and
`components/sim-trajectory/docs/trajectory-computation.md` (chapter
3's own two-spaces story and figure); `.agents/rulings.md` R2/R3/R7.

### Tag ceremony

`origin/main` at `800ae28`
(`800ae285e857d92b308420871479e6d363ce1a55`, ADR-0119 close) at session
start — matched the driving prompt's own stated premise exactly. The
last five `main` CI runs (`gh run list --limit 5 --branch main`,
checked at session start): all `completed`/`success` —
`800ae28` (4m40s), `0b6d74f` (3m39s), `a9a0bbf` (4m3s), `ab11d7b`
(4m37s), `b711aa6` (3m37s) — no red among the five.

Tag `stable-20260812-manual-s1` created ANNOTATED at `800ae28`; pushed;
peeled ref verified exact (`git rev-parse
stable-20260812-manual-s1^{commit}` and `git rev-list -n1
stable-20260812-manual-s1` both return `800ae28...`, matching). License:
case (i), channel fresh-clone verification 2026-08-12 per the driving
prompt's own citation (lineage, ASCII x3, zero `src`/`test`, excerpt
fidelity byte-checked against the demo README, paring and provenance
markings confirmed), CI confirmed green per this preflight.

### Decision

#### Commit 1 (`07dbc5d`) — the demo exerciser

**Red first.** `ehrt.docs-tooling.demo-exerciser-fresh` generalizes
`quickstart-fresh`'s single-fence, single-BEGIN/END-block identity
check to a scenario README with SEVERAL fenced blocks — some bare
` ```bash ` command fences, some plain ` ``` ` transcript blocks mixing
a `$ `-prefixed command with its own witnessed output inline (the
README's own "Ground truth is invariant" and "The wrapper itself"
sections). Extraction walks every fenced block in document order and
keeps exactly the command lines a reader would type: a line starting
`bin/ehrt ` opens a taught command; a line starting `$ ` is a transcript
command with the prompt stripped; a line right after a kept line ending
in `\` is a continuation, kept verbatim; every other fenced line —
prose, board-snapshot or JSON-payload output — is skipped. Run against
the tree before `bin/demo-exerciser-ed-tuesday` existed: RED, exactly
as predicted — `readme-count 21, script-count 0`, first divergence at
index 0 (`bin/ehrt corpus generate sim --seed 20260811 --patients 100
\` present in the README, `::missing` in the script). The other five
tests in the same file (pure extraction-logic tests against synthetic
fixtures, no dependency on the real script existing) were already
green, proving the extraction functions themselves catch what they
claim to before the real-tree test is ever trusted.

**Dry-run verification before writing the script.** Every one of the
11 taught commands (21 physical lines counting continuations) was run
manually against the live tree first, to catch any README/tree
divergence before committing to a design: all 11 succeeded exactly as
the README's own witnessed output states — the corpus-generate payload,
the play/board snapshot counts (34 and 33), the base/latency
`sha256sum` digest (`b4e776f7...`, identical to the README's own
printed value), the 34-batch listing (34 `:verified true`, 0
`:verified false`, 34 files on disk), and the straddle grep
(`MRN000002-A01-` in `batch-000.hl7`, `MRN000002-A03-` in
`batch-001.hl7`) all matched byte-for-byte. No divergence found; no
STOP-AND-REPORT needed.

**The exerciser.** `bin/demo-exerciser-ed-tuesday` mirrors
`quickstart-demo`'s own shape (a `set -uo pipefail` script, `expect`/
`expect_eval` wrappers asserting per-step exit codes, a BEGIN/END
marker pair the freshness test reads) with one addition: each wrapper
call also tees its own stdout/stderr to a per-step log file
(`$LOG_DIR/step-N.out`), a side effect invisible to the taught command's
own argv, so a specific step's real, freshly generated output can be
checked against a named invariant AFTER the taught block — without the
capture mechanism ever touching the identity-checked command text.
Named invariants asserted, every value copied from the README's own
witnessed prose, never hand-computed:

- **Ground-truth determinism** — step 6 (`diff` between the base and
  latency `events.edn`) exits 0 (the wrapper's own assertion) AND its
  own captured stdout is empty (`[ -s ... ]` check), matching the
  README's literal "diff reports no differences."
- **The second-clock digest identity** — step 7's own captured
  `sha256sum` output is parsed for both digest fields and compared for
  equality; `diff`'s own exit code proves non-difference but not digest
  equality specifically, so this is re-derived independently rather
  than assumed from step 6 alone.
- **The 34-batch `:verified true` listing** — step 9's own captured
  stdout is grepped for `:verified true` (expect 34) and `:verified
  false` (expect 0); the batch directory's own file count is checked
  independently (`ls batch-*.hl7 | wc -l`, expect 34) — three
  independent measurements of the same "34, all verified" claim, not
  one.
- **The straddle membership** — `grep -qF 'MRN000002-A01-'
  batch-000.hl7` and `grep -qF 'MRN000002-A03-' batch-001.hl7` against
  the files step 9 actually wrote.
- **Tree-clean postcondition** (mirrored from `bin/quickstart-demo`'s
  own ADR-0005 discipline) — every write lands under the gitignored
  `out/`; `git status --porcelain` empty after a full run.

**Integration wiring.** The `Makefile`'s `integration` target gains one
line, `bin/demo-exerciser-ed-tuesday`, after the existing `poly test
:all project:integration` invocation — the one hook the driving prompt
licensed. The fast identity test (`demo_exerciser_fresh_test.clj`) is
an ordinary `docs-tooling` brick test, part of the `conformance`
project, so it runs in the normal per-push lane
(`clojure -M:poly test :all skip:integration`) without any extra
wiring. **Scope note, disclosed:** `.github/workflows/integration.yml`
invokes `clojure -M:poly test :all project:integration` directly, not
`make integration` — the nightly/dispatch-only CI job does not
currently run the new exerciser. The driving prompt's own fence named
only the Makefile as the touch point for this hook
("Makefile edit is in-fence for exactly this hook"); wiring the GitHub
workflow itself to call `make integration` (or to add its own
`bin/demo-exerciser-ed-tuesday` step) is left for a future session to
decide, not assumed here.

**A real, unrelated finding, fixed forward.** The first `make test` run
against the staged checkpoint went RED on
`ehrt.cli.executable-bits-test`'s own `tracked-scripts-are-executable-
in-the-index-test`: `bin/demo-exerciser-ed-tuesday` was `chmod +x` on
disk but the git index still held it as `100644` (a fresh clone would
see it as non-executable even though the local working tree ran it
fine). Fixed exactly as the test's own failure message prescribed:
`git update-index --chmod=+x bin/demo-exerciser-ed-tuesday`. Re-run:
green, 535 assertions, 0 failures, 0 errors, `bin/verify-nist-lock` OK.

**The witnessed `make integration` run.** The first attempt (staged
but not yet committed) failed at the very last line — every real
invariant (diff, digest equality, batch count/verified, straddle) held,
but the tree-clean postcondition tripped on this session's own
not-yet-committed checkpoint-1 and chapter-3 files, a false positive of
running the exerciser mid-development rather than a real defect.
Resolved by committing checkpoint 1, then `git stash push -u` on the
still-uncommitted chapter-3 files to get a truly clean tree matching
`HEAD`, then re-running: **green, exact output**
`== demo-exerciser-ed-tuesday: every command asserted, every named
invariant held, tree clean ==`, no `FAIL` line anywhere in the log. The
stash was popped immediately after to restore the chapter-3 work.

#### Commit 2 (`9473c81`) — chapter 3, a simulated hospital

`docs/manual/03-a-simulated-hospital.md`: `sim run` (the strict
engine-tier verb, `docs/cli.md`) contrasted with `corpus generate
sim`'s own ergonomic front door (the RQ2 tiering ADR-0115 already
ruled), grounded in ed-tuesday's own Generate strip — copied verbatim
from the demo README, output witnessed fresh this session
(`{:status :ok, :payload {:out-dir "out/scenarios/ed-tuesday"}}`).
Site profiles: linked (`docs/site-profiles.md`), never restated —
the truth-invariance guarantee (two profiles, one seed, byte-identical
ground truth) named but not re-derived. Scripted-versus-generative
patients: ed-tuesday's own `config.edn` has both, side by side —
five hand-authored `:pathways` (scripted) and an eight-patient GMF
`:module-assignment` tail (generative, walked under the run's seeded
RNG) — both resolved into the same pathway IR before the engine ever
sees which produced them
(`trajectory-computation.md` section 2, cited); the chapter also
discloses the demo's own honest finding that the thin generative tail
produced zero live encounters at this scenario's short horizon,
matching the README's own disclosure rather than glossing over it.
The two-spaces story: script space (plan) versus truth space (fact),
retold accessibly per R7, extended one layer down to `GT`'s own two
independent emitters (`emitH`/`emitF`) — the founding thesis
("formats are just emitters of the patient state machine") stated as
the organizing idea, the naturality-square coherence claim (both
emitters agree on patient identity, proven by a 150-trial property
test) noted in the figure's own caption text, the formalism itself
linked (`simulator-architecture.md` section 4) never taught.

**The figure.** `docs/manual/assets/gt-emitters.svg`: hand-authored,
not mechanically generated (an HTML comment in the SVG source cites
`docs/dev/simulator-architecture.md` section 4 as its own content
source, per R6's own derive-from-data discipline) — three boxes (`GT`,
`ER7*`, `FHIR*`) and two labeled arrows (`emitH`, `emitF`), styled
minimally, no dark-mode variant (a static repo asset embedded in
Markdown, not a themed Artifact).

**Front-page rider.** `docs/manual/00-front.md`: Chapter 3's entry
drops its working-title marker and gains a firm one-liner reflecting
what actually landed (`sim run`/ed-tuesday, site profiles link,
scripted-versus-generative, the two-spaces story); the arc-status
prose moves from "Chapters 1–2 are this session's own delivery" to
"Chapters 1–3 are landed," and "Chapters 3–8" narrows to "Chapters
4–8" throughout; the currency contract's single-commit citation
(`6b48e81`) is generalized to name each chapter's own witnessing
commit, since Chapter 3's own strips were witnessed against a later
commit than Chapters 1–2's.

**Strip source citations, per strip:**

| strip | source |
|---|---|
| `bin/ehrt corpus generate sim --seed 20260811 ...` (Ch3 Generate) | `demos/scenarios/ed-tuesday/README.md`, "Generate" |

No other command strip appears in Chapter 3 — every other reference
(site profiles, the two-spaces story, the figure) is prose plus a
link, not a composed or copied invocation.

### Oracle bracket

Pre-analysis: pure identity expected — every file touched this session
is `bin/demo-exerciser-ed-tuesday` (new tooling that reads the tree and
writes only to gitignored `out/`), two new `docs-tooling` `src`/`test`
files (a docs-tooling identity check, no engine/sim/judge dependency),
`docs/manual/*` (new/edited docs), `docs/manual/assets/*` (new SVG),
and `Makefile` (one added line); nothing touches any oracle root's own
`src`.

`bin/regression-oracle 800ae28 9473c81` →
**`IDENTICAL: every root's digest matches between
800ae285e857d92b308420871479e6d363ce1a55 and
9473c813d754d21b23900acb566813d47a4011b3`**, all 35 roots. Matches the
pre-analysis exactly.

### Verification

`clojure -M:poly check`: OK, before each commit. `make test` (full
suite): run RED once against checkpoint 1 alone —
`ehrt.docs-tooling.demo-exerciser-fresh-test`'s own
`committed-readme-and-script-agree-test` failed as designed (the R3
red-evidence capture, above) before the script existed; a second,
unrelated RED (`ehrt.cli.executable-bits-test`) surfaced after the
script was written but before its executable bit was staged, fixed as
described above; GREEN after the fix (535 assertions, 0 failures, 0
errors, `bin/verify-nist-lock` OK) — run again for checkpoint 2, GREEN
(same counts, docs-only diff). `make integration`: run once against a
staged-but-uncommitted tree (false-positive tree-dirty fail, every real
invariant held — see Commit 1 above); run again against a truly clean
tree matching `HEAD` after committing checkpoint 1 and stashing
checkpoint 2's own WIP — **GREEN**, the witnessed run this ADR cites.
`gitleaks git --staged -v`: clean, both commits. `git diff --cached
--stat` reviewed before each commit: exactly the fenced files. Post-push
verification: both commits' pushed messages diffed against their own
message files — the only delta either time was `git log --format=%B`'s
own trailing-blank-line formatting artifact; the ASCII-only check
(`git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`) empty both
times.

### Deviations

**No premise mismatch.** Every Read-first document matched its own
characterization in the driving prompt; the tag license's stated
preflight conditions (origin at `800ae28`, CI green) held exactly;
every command in `demos/scenarios/ed-tuesday/README.md` ran exactly as
written, with no divergence from the README's own witnessed output —
the STOP-AND-REPORT clause this session's own prompt named for exactly
that case never fired.

**Two findings, both fixed forward, neither a design ambiguity:** the
executable-bit gap (Commit 1, above) and the tree-dirty false positive
from running the exerciser mid-development before its own commit
landed (also Commit 1, above). Both are mechanical — a git-index state
fix and a run-ordering fix — and both were resolved without pausing for
author input, on the same reasoning ADR-0119's own sequencing-conflict
deviation named: no design option foreclosed, no ambiguity for the
author to resolve, no push ever carried a knowingly-failing test.

**The GitHub Actions integration workflow scope note** (Commit 1,
above) is disclosed as a deviation-adjacent finding, not fixed: the
driving prompt's own fence licensed only the Makefile as the
integration-tier touch point, so the nightly CI workflow's own direct
`poly test` invocation (bypassing `make integration`) was left as
found, not edited.

### Fences

Touched: `bin/demo-exerciser-ed-tuesday` (new);
`components/docs-tooling/src/ehrt/docs_tooling/demo_exerciser_fresh.clj`
(new, "beside quickstart-fresh"); `components/docs-tooling/test/ehrt/
docs_tooling/demo_exerciser_fresh_test.clj` (new); `Makefile` (one
line, the `integration` target's own hook, plus its `help` text);
`docs/manual/03-a-simulated-hospital.md` (new); `docs/manual/assets/
gt-emitters.svg` (new); `docs/manual/00-front.md` (Chapter 3's
one-liner, the arc-status prose, the currency contract); `.agents/
rulings.md` (untouched — no mid-session ruling occurred);
`.agents/plans/roadmap.md`; `notes/adr/0120-user-manual-s2.md` (this
file); `notes/ADRs.md`; `notes/adr/README.md`; `.agents/session-
records/*`; `.agents/prompts/*`. ZERO engine/sim/judge `src`. ZERO
edits to `demos/scenarios/ed-tuesday/README.md` (no divergence found,
so no fix was needed). ZERO edits to Chapters 1–2
(`01-what-this-is.md`, `02-setup-first-corpus.md`).

### Index line

```
- 2026-08-12 — manual-s2-exerciser-and-chapter3 — ADR-0120
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)
