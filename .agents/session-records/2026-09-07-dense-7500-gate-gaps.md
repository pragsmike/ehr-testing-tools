# 2026-09-07 -- dense-7500 gate gaps: the exerciser joins `make integration`, the Scale table gets a gate

The two gates ADR-0180's **R-gate-gaps** commissioned alongside its seven sites,
rowed separately because neither is site work --
`roadmap.md#dense-7500-gate-gaps` PRIORITY 2, now CLOSED. Ceremony mode: R30
(commit and push at each checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-07-dense-7500-gate-gaps.md`](../prompts/2026-09-07-dense-7500-gate-gaps.md).
No ADR was written and none was owed: this session enacts a charter already
ruled, and the de-scaffold ruling writes no ADR for a register or build
decision.

Rulings in force: **R-gate-gaps** (ADR-0180), **R-figures-file** (new with this
prompt), **R-readme-first** (the exerciser's own header), **R-edit**, **R-cap**,
**R-pins**, **R-sentinel**, **R-empty-population-is-red**.

## 0. Preflight

`bin/preflight` reported four sections OK and one FINDING: last five CI runs on
`main` all green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`),
`core.fileMode` true, `core.ignorecase` unset, local HEAD `2ba3490c` equal to
`origin/main`, HEAD not tagged (DISCLOSED). The FINDING is the tree-clean check,
and it is **this session's own**: I had already applied step 2's edits before
running it. Nothing pre-existing was dirty -- the two paths it named are the two
step 2 committed. Session start and every baseline is `2ba3490c`.

**Step 1's own gate, before any edit.** `bin/demo-exerciser-dense-7500` at
`2ba3490c`: exit 0, 158.7 s wall (the script's own taught-command wall 152 s).
It asserts, and this is the list the prompt asked for:

- the **one-key derivation** -- `config-nobed.edn` differs from `config.edn` by
  exactly one removed line and zero added, and that line is `:bed-cycle true`
  (the step's own exit code 1 proves only that they differ at all);
- the **additive-prefix derivation** -- `config-bare.edn` less its closing brace
  and newline is byte-identical to `config.edn`'s first 154,196 bytes, sha-256
  `5c3d7659...`, with the length re-derived from the file rather than carried;
- the **ground-truth path** -- the redirected file is non-empty and starts `[`,
  which the exit code alone cannot see;
- the **witnessed counts**, re-read from the README at runtime rather than
  hard-coded: 222,819 spooled messages, 167,197 events at 7,500, 33,306 at 750;
- the **tree-clean postcondition** (ADR-0005).

All three counts were already exactly what `4ddf62c2` measured, which settled
before any work began that sites 5-7 moved the walls and not the log.

## 1. What landed

**`691db446` -- `demos: dense-7500 exerciser writes figures.edn`.**
`demos/scenarios/dense-7500/figures.edn` is now the one place this scenario's
figures live. The exerciser's count JVM echoes machine-readable `FIGURE` lines,
and a final step splices the file's `:asserted` block from that run's own
artefacts.

**`e3e6e44d` -- `docs: Scale table and dense-7500 rows gated against
figures.edn; re-measured at 2ba3490c`.**
`ehrt.docs-tooling.dense-7500-figures-test` (six claims, 53 assertions) holds
both quoting documents to that file, and both documents were re-measured.

**`df73b6c8` -- `build: dense-7500 exerciser joins make integration`.** Third
step of the target, after ed-tuesday and clinic-decade; the help text names all
three.

**`e6b3e4dd` -- `docs: the sim check catalog is 46, not 45`.** Errata this
session did not go looking for and could not finish without; section 5 has it.

## 2. The design decision, and why it is not what the prompt's parenthetical said

The prompt asked for `figures.edn` to carry, per cell, "events, messages,
msg/event, plus wall, sha, date, machine", written by the exerciser. **The
exerciser writes only the deterministic half.** R-figures-file's own second
sentence is what forces this: *process-wall cells are dated quotations of the
same file, never asserted against a live run.* A wall stamped live on every run
would churn a TRACKED file, and the script's tree-clean postcondition -- the
thing that makes this a gate rather than a report -- would have to be weakened
to tolerate the churn. One defensible reading, so fix-forward with disclosure
rather than a stop.

So the file has two halves and says so in its own header:

- **`:asserted`**, between markers, rewritten on every run. Only the cells the
  script generates an artefact for: `config.edn` at 7,500 (events, messages,
  msg/event) and at 750 (events). **The rewrite IS the assertion** -- a moved
  count writes a block that differs from the committed one and the tree-clean
  check fails carrying the diff. Nothing compares, so nothing can fall out of
  step with the file.
- **`:quoted`**, hand-measured, untouched by the script. Two kinds, for two
  different reasons: the two sibling configs' counts (the README teaches no
  command for either, so no run in this tree produces an artefact to count --
  that README's own standing note), and every process wall and peak RSS.

**What this does and does not cover, stated plainly.** The all-keys 7,500 cell's
counts are proven against a live run on every `make integration`. The other
figures are gated as *consistent quotations of one dated source*, which is
exactly the hole gap (b) named: three cells re-measured by hand with nothing
failing when they did not follow. A hand re-measure now edits one file, and both
documents go red until they follow.

## 3. The re-measurement

Method as both documents state it, and as the previous re-measure used: one
warm-up plus two timed runs per cell, one JVM per run, a fresh spool target per
run, `/usr/bin/time -v` around each, every figure the mean of the two timed
runs. Twelve generate runs plus one count JVM, ~25 minutes.

**"Both runs produced identical counts" is now checked by byte identity.** Each
cell's two timed `events.edn` were sha-256'd and compared instead of parsed
twice: stronger than count identity and one hash cheaper than a second 67 MB
EDN parse. All four cells: identical digests, identical message counts.

| cell | events | messages | wall `4ddf62c2` -> `2ba3490c` | peak RSS |
|---|---:|---:|---:|---:|
| `config.edn` @ 7,500 | 167,197 | 222,819 | 278.64 -> **106.25 s** | 2,415 -> 1,927 MB |
| `config-nobed.edn` @ 7,500 | 125,642 | 165,466 | 231.14 -> **93.34 s** | 2,178 -> 1,984 MB |
| `config-bare.edn` @ 7,500 | 100,868 | 65,457 | 142.12 -> **42.36 s** | 1,780 -> 1,135 MB |
| `config.edn` @ 750 | 33,306 | 40,291 | 54.37 -> **48.20 s** | 1,228 -> 1,183 MB |

**Not one count moved**, and the all-keys cell's `events.edn` hashes to
`096959857076f22901fa92a9fd71cca0edbc469b109118b6a469fcf0b49dc2b2` -- the same
digest `.agents/plans/2026-09-05-7-event-divergence.md` recorded for this
configuration before any of ADR-0180's sites landed. That is a corpus proven
byte-identical across the whole seven-site programme, on the one configuration
the oracle does not cover.

The 750 cell barely moves (1.13x against 2.5-3.4x) because it is dominated by
the same 15,000-person demographic timeline the programme did not touch --
the README already argues that the two cells are not one scenario a decade
apart, and this measurement is a second witness for it.

The superseded figures are kept, not deleted: `figures.edn`'s
`:provenance :prior` carries `4ddf62c2`'s eight numbers and its date.

## 4. Red first

Against the stale cells, with `figures.edn` re-measured and both documents not
yet edited, the new namespace failed **13 of its 53 assertions** and nothing
else in `brick:docs-tooling` moved: three Scale wall cells, eight README wall
and RSS cells, and both dated-quotation banners. After the documents were
edited, 0 failures.

## 5. Findings

- **`make integration` cannot gate an uncommitted change.** Every demo exerciser
  asserts a clean tree as its last act (ADR-0005), so the first attempt at step
  4's gate died 21m49s in, inside `bin/demo-exerciser-ed-tuesday`, carrying the
  two step-4 files as its diff. The commit therefore precedes its own gate, and
  its message says so. Pre-existing behaviour, newly consequential now that the
  target is where this scenario's gate lives.
- **A gate can be unreachable and stale at the same time, and this session found
  a second one.** `bin/usecase-ground-truth-oracle` asserts the invariant
  catalog's size so that prose cannot drift from it. The catalog went 45 -> 46
  on 2026-09-05; six places in the tree followed and three did not, and one of
  the three was that assertion. It runs in `make integration` and in nothing CI
  runs, so it had not executed since. Fixed forward in `e6b3e4dd`, as its own
  commit, because it blocked step 4's gate. This is the same shape as the row
  this session closed -- a gate reachable by nothing scheduled -- and it is
  worth reading as evidence that `make integration` needs running more often
  than once an arc, not as a one-off.
- **`4ddf62c2` predates site 1, not site 5.** The roadmap row reads as though the
  stale measurement was taken after the first four sites; `git rev-list` puts all
  fifteen of ADR-0180's payload commits after it. A first draft of the README
  paragraph said "four landings"; corrected to seven before commit.
- **A new test FILE still costs a `make state-derived` regeneration**, and it is
  the only thing that went red in the first green run: 228 -> 229 test
  namespaces, 48 -> 49 docs-tooling gates.
- **`config-nobed.edn`'s peak RSS is above its larger sibling's.** 1,984 MB
  against `config.edn`'s 1,927 on a smaller corpus, with its own two timed runs
  97 MiB apart. Reported as measured, not investigated -- the same disposition
  the 2026-09-05 session gave the same column, and both documents now say to
  read it as a GC high-water mark under a 3.88 GB default heap.
- **The peak-RSS unit was never stated anywhere.** `figures.edn` now fixes it as
  maximum resident set size in MiB and both documents repeat it; the previous
  figures cannot be reconciled to a unit from what was written down, which is
  why they are kept as `:prior` rather than compared arithmetically.

## 6. Gates

| gate | result |
|---|---|
| `bin/preflight` | one FINDING, this session's own dirty tree; all else OK |
| `bin/demo-exerciser-dense-7500` at the tip (step 1) | exit 0, 158.7 s |
| `make test` (step 2) | exit 0, 426 suites, 28,419 assertions, 21m11s |
| `bin/demo-exerciser-dense-7500` after the figures step | exit 0, 149.0 s, **tree clean** -- the rewrite is byte-identical |
| `brick:docs-tooling` red witness (step 3) | 13 failures of 53, exactly the stale cells |
| `make test` (step 3) | exit 0, 428 suites, 28,525 assertions, 21m06s |
| `brick:docs-tooling` after the prose correction | exit 0, 64 assertions |
| `make integration` (step 4) | exit 0, 27m05s, tree clean -- dense-7500 ran inside it, 163 s |

`make test`'s wall is unchanged across the payload: 21m11s before the new
namespace, 21m06s after it, and step 4 edits only `help` and `integration`.
