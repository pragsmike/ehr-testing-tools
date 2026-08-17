## ADR-0140 — The fence battery's ruled fixes: seven findings dispositioned, one tool made diagnosable, D8-5 discharged

**Status:** Accepted (author-directed, autonomous session per R30 — the
battery's own fix session, chartered by the ruling below), 2026-08-16.

### Context

Prior: `30cc335` landed
[`.agents/plans/2026-08-16-fence-battery-findings.md`](../../.agents/plans/2026-08-16-fence-battery-findings.md),
the D8-5 live fence battery's register — 102 files, 202 fenced blocks,
58 bare command fences run one by one (GREEN 42, RED 7, YELLOW 5,
SKIPPED-WITH-REASON 4). That register **fixed nothing**: the
repo-review skill's inherited law makes every finding a row awaiting a
ruling. This ADR is the ruling executed.

The author's ruling, verbatim (2026-08-16):

> "Accept recommendations."

binding the register's per-finding recommendations and the session
shape: the fixes are their own micro-session with their own ADR, so
D8-5's lapse — a probe carried unrun across **two consecutive
reviews** — closes on the record before the event-log-contract arc
starts.

One finding was ruled OUT of this session by the same ruling: **R-F8**
(58 of 76 command fences have no exerciser) goes to review 4's D2 as a
standing policy question, recorded below, not implemented.

### Decision

Eight findings, three commits, one disposition each. No finding was
carried, silently narrowed, or re-opened.

| # | finding | ruled | verdict movement | landed |
|---|---|---|---|---|
| R-F1 | `04-time-on-the-wire.md:23` plays a corpus its own chapter creates 45 lines later | fix the page | **YELLOW → GREEN** (exit 0 from a cleared `out/`) | `14c9348` |
| R-F2 | `08-your-own-data.md:82` needs a Synthea corpus no chapter generates | fix the page | **YELLOW → GREEN** (exit 0, output matches the page's own `:totals {:pass 7 …}`) | `14c9348` |
| R-F3 | `polylith-brief.md` ×7 teach bare `poly`, not on PATH here | fix minimally, provenance-preserving | **RED ×7 → disclosed convention** (the seven fences are unchanged and still exit 127 if pasted verbatim; the note at the head of the document is what converts them from undisclosed breakage to a stated convention) | `14c9348` |
| R-F5 | `HL7v2-sanitized-corpus-research.md:126` curls `messages.out` into the repo root | fix the page | **YELLOW → GREEN**, and the tree stays clean: `git status --porcelain` after the re-run shows only this session's own edits | `14c9348` |
| R-F4 | `formats.md:506`/`:518` undeclared placeholders; `:518` teaches un-vendored `jet` | accept-with-disclosure | **YELLOW ×2 → disclosed**, and see the census note below — the enumerator now classifies both as `other`, not `command` | `43aec70` |
| R-F6 | `simulate-your-facility.md:169` needs an authored `stmarys.edn` | accept-with-disclosure | **SKIPPED → taught expectation** (exit 2 `:config-not-found` verified live, and the named alternative `--config demos/scenarios/ed-tuesday/config.edn` verified exit 0) | `43aec70` |
| R-F7 | the mutate→intake pipeline masks `:file-not-found` behind `:malformed-mllp-frame` | **fix the tool** (diagnosability) | **tool fixed**, red-first; before/after below | `07a9566` |
| R-F8 | 58 of 76 command fences have no exerciser | **NOT this session** — handed to review 4's D2 | unchanged; recorded below | — |

### R-F7 — the diagnosability fix, and what it is not

The input gap itself stays closed-as-fine (repo review 2's D8-9, "the
page's own *You bring* framing already sets the right expectation").
The defect ruled fixable was **diagnosability in the pipeline**:
`mutate` emits its honest envelope on stdout and exits 2, the pipe
hands that envelope to `intake`, and `intake` decoded it as corpus
bytes — reporting a fault in bytes the reader never wrote while the
real cause vanished.

Fixed at the intake seam (`ehrt.corpus-io.spool/spool!`), the **D4-3
pattern — distinguish before parsing**; `framing/decode` and every
engine are untouched.

**Before** (`in/v2-corpus` absent, the use-case page's own fence):

```
{:status :rejected, :category :malformed-mllp-frame, :payload {:pos 0, :hint "expected 0x0B start-of-block"}}
```
exit 1.

**After:**

```
{:status :error, :category :upstream-error, :payload {:origin "stdin", :upstream {:status :error, :category :file-not-found, :payload {:path "in/v2-corpus"}}, :hint "the command feeding this one failed; its own result envelope arrived here instead of corpus bytes"}}
```
exit 2 — the upstream envelope carried verbatim, and the pipeline's own
exit code now matches the exit code `mutate` actually returned.

**A second defect the red-first test found, worse than the register's
own row.** The register predicted `:malformed-mllp-frame` for empty
stdin. It is not what happens: `decode-mllp` on zero bytes returns
`ok []`, so empty stdin **succeeded** —

```
EMPTY_STDIN_EXIT=0
{:status :ok, :payload {:catalog [{… :path "capture-manifest.edn" …}], :intake-record {… :file-count 1 …}}}
```

— cataloging a corpus whose only member was the capture manifest the
spool had just written. A silent success on no input at all. Now:

```
EMPTY_STDIN_EXIT=1
{:status :rejected, :category :empty-input, :payload {:origin "stdin", :framing :mllp, :hint "nothing arrived to spool -- check that the producer on the other side of the pipe actually ran"}}
```

Recorded because the battery's row understated its own finding, and the
red-first discipline is what exposed the gap: had the test merely
asserted the predicted category, the empty case would still be green
and still be wrong.

**Red witnessed against exactly the unfixed code** (`clojure -M:poly
test brick:corpus-io`, before any `src` edit): `Ran 11 tests containing
44 assertions. 7 failures, 0 errors.` Green after: `44 passes, 0
failures, 0 errors.` The third new test —
`spool-does-not-mistake-corpus-bytes-for-an-upstream-envelope-test`,
a FHIR Bundle, which also begins with `{` — passed in both runs by
design: it fences the fix against over-reach rather than proving it.

The happy path was re-verified end to end (`test-fixtures/v2` through
the same real pipe): exit 0, catalog written.

**No generated doc was forced.** `intake`'s category list is rendered
into neither `docs/cli.md` (whose intake section is flags only) nor
`docs/formats.md` (which documents the envelope's *shape*, not a
per-command category vocabulary), so step 3's "regenerate what
`cli-md-is-current-test` gates" was a checked no-op, not a skipped one.

### The post-fix census, re-run rather than asserted

`bin/fence-census` at the tip, against the same enumerator that
produced the register:

| class | before (`30cc335`) | after |
|---|---|---|
| command / exercised | 18 | 18 |
| command / **bare** | **58** | **56** |
| output | 29 | 29 |
| other | 97 | 99 |
| **total** | **202** | **202** (closed) |
| files in scope | 102 | 102 |

**The population is unchanged; two fences left the command class.** A
full before/after diff of the census's own per-fence rows (baseline run
in a disposable worktree at `30cc335`) names them exactly: the two
`docs/formats.md` fences, `command/bare → other/-`, and nothing else
moved.

That was not an intended edit to the census. It is the enumerator's own
pre-existing rule — `NOT_A_COMMAND_RE`, "shapes that look command-ish
at the head but are not runnable text", which matches a `<placeholder>`
token — agreeing independently with R-F4's ruled disclosure. Marking
the placeholders as placeholders made the fences honestly
non-runnable, and the census reclassified them without being told to.
Recorded because a headline number moved and the reason is not the one
a reader would guess.

### R-F8 — handed to review 4, with the proposed rule quoted

Not implemented here, by ruling. Review 4's **D2** inherits it as a
standing policy question, with this as the proposed default:

> every fence a reader meets on the README / SETUP / manual / use-case
> path is exercised; developer-facing briefs and research notes are
> exercised only when they make claims about outputs; the census can
> gate bare-fence-count-on-reader-path = 0.

The battery can state the number (56 bare of 74 command fences at this
tip, 76% → 76%); only a ruling can say whether it is the right one.

### The stale-`out/` near-miss, as an incident class

The battery found this in itself and it belongs on the record as an
incident class, not as a footnote in a register: its first manual pass
ran against an `out/` the exercisers had populated, and returned RED
against the manual's **most emphatic** claim — that two same-seed runs
`diff -rq` to nothing. Re-probed from a cleared `out/`, the claim
holds.

**An accumulated `out/` is a registry standing in for a population** —
ADR-0139 rule 9 applied to a directory instead of a file. Any future
run of this battery that does not clear `out/` first will mis-score the
manual in both directions. This session therefore cleared `out/` before
every fence re-run and treated that as a precondition rather than a
nicety; each re-run exit code in the session record is from a cleared
`out/`.

### Deviations and premises that did not survive the tree

1. **R-F2's stated premise is false, and the fix is better for it.**
   The prompt (and the register's own row) said the Synthea corpus is
   "the one Chapter 2 generated," with the fix to be an anchor into
   Chapter 2. It is not: Chapter 2 generates `sim-s1-p1` only and
   explicitly defers "fetching Synthea" to later chapters
   (`02-setup-first-corpus.md:67`), and `git grep synthea-s1-p5` over
   `docs/manual/` returns Chapter 8 alone. The corpus comes from the
   root `README.md` Quickstart, which Chapter 8 already cites. Fixed
   forward to the register's own ruled substance — *"one line naming
   the prerequisite command"* — pointing at the Quickstart by anchor
   (`README.md#quickstart`, verified) and naming the two `artifact
   fetch` calls plus `corpus generate synthea`. A Chapter 2 anchor
   would have been a working link to a false claim.
2. **R-F5 needed one line more than the ruled minimum.** `curl -o
   out/simhospital/messages.out` fails from a cleared `out/` with no
   directory to write into, which would have left the fence RED after
   its own fix — a STOP condition. `mkdir -p out/simhospital` was added
   as the fence's first line. `out/` is gitignored
   (`.gitignore:8`, the ADR-0013 tool-owned output root), so the
   ruled goal — the fence never dirties a tracked surface — holds:
   verified by `git status --porcelain` immediately after the re-run.
3. **R-F1's ruled sentence names commands that exist.** The prompt's
   draft said "the two `sim run` invocations below"; the fences below
   are `bin/ehrt corpus generate sim`. Written to match the tree. Both
   halves of the sentence were verified: the two generate fences then
   the play fence, exit 0; and `bin/demo-exerciser-ed-tuesday`, exit 0,
   creating the same latency wire.
4. **`:onboarding` re-derived, and it disagrees with the register.**
   Measured at this tip by the gate's own method (`line-seq` over
   `.agents/reading-sets.edn`'s `:onboarding` `:paths`): **2660 / 2690,
   30 lines of headroom** before this session's own records, **2662 /
   2690, 28 lines** after them. The battery's register claimed
   2657/2690 after its own close. Three lines unaccounted for;
   re-derived rather than repeated, and **no bump is needed**. Disclosed
   because repeating a predecessor's number instead of re-deriving it
   is the exact habit `bin/fence-census` exists to break.

### Verification

- `bin/preflight` at session start: five green CI runs on `main`,
  ext4 edit root, tree clean including untracked, HEAD matched
  `origin/main` at `30cc335`, HEAD untagged (disclosed, no tag owed —
  this micro-session's own close tag is deferred to the next session's
  Step 0 under the standing conditional license, the ADR-0133/0135
  pattern). Both standing tags verified peeled on the remote:
  `stable-20260815-result-nodes^{}` = `b139de5…`,
  `stable-20260815-review-3-fixes^{}` = `b96c246…`.
- `clojure -M:poly check`: OK.
- Every fence re-run from a **cleared** `out/`; exit codes in the
  session record, not paraphrased.
- Red/green for R-F7 captured with the runs' own output (above).
- No regression-oracle claim is made or owed: no vendored root, no
  generator, and no converter changed.

### Fences

Deliberately NOT done here:

- **R-F8 is not implemented.** No exerciser was added, no
  `exercised-sources.edn` row was written, no census gate was armed.
  Review 4's ruling decides that.
- **The seven `poly` fences are not rewritten.** Move-don't-improve:
  the brief teaches upstream Polylith's own vocabulary, and the
  disclosure note preserves that provenance instead of forking it into
  a workspace dialect.
- **Chapter 4 is not reordered.** The "symptom first, then mechanism"
  pedagogy is the author's; the fix is one parenthetical, nothing else.
- **`jet` is not vendored**, by ruling.
- **No converter, generator, or engine changed**; zero vendored bytes.

### Consequence

**D8-5 is DISCHARGED.** The probe that lapsed across two consecutive
reviews has now run, been ruled on, and had every ruled finding landed
or explicitly handed on with its ruling quoted. Review 4 inherits one
open row from it (R-F8) and a working enumerator
([`bin/fence-census`](../../bin/fence-census)) rather than a method
description.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The fence battery's ruled fixes: seven findings dispositioned, one tool made diagnosable, D8-5 discharged — the D8-5 battery's own fix session, on the author ruling *"Accept recommendations."* (2026-08-16). Four pages fixed (ch4's play precondition, ch8's Synthea prerequisite, the polylith brief's `poly`-alias provenance note, the research note's `curl` target moved under `out/`), two accepted-with-disclosure (`formats.md`'s placeholders and optional un-vendored `jet`, `simulate-your-facility.md`'s authored config), and one tool fixed: `intake`'s stdin seam now distinguishes `:empty-input` and `:upstream-error` from `:malformed-mllp-frame` (the D4-3 pattern, engines untouched), so a failed upstream's own envelope survives the pipe verbatim and the pipeline exits 2 as the upstream did instead of 1. Red-first found a second defect the register had understated: empty stdin previously exited **0**, cataloging a corpus whose only member was the capture manifest. Post-fix census re-run — population unchanged at 102 files / 202 blocks, bare 58 → 56, the two moved fences named by a before/after diff against a disposable worktree at `30cc335` (the enumerator's own `NOT_A_COMMAND_RE` agreeing with R-F4's disclosure, not an edit to the census). R-F8 (76% of command fences unexercised) handed to review 4's D2 with its proposed rule quoted, not implemented. The stale-`out/` near-miss recorded as an incident class (ADR-0139 rule 9, applied to a directory). **D8-5 DISCHARGED**
