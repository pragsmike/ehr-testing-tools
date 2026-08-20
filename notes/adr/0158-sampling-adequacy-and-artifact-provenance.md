## ADR-0158 — review-4 fix 4/4: sampling adequacy, and every artifact points back at its inputs

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-19.

### Context

The last of review 4's eight fix sessions, pairing plan Sessions **F**
(sampling adequacy and the register rows owed) and **H** (every artifact
names its own inputs) under the author's "Q3 pair small ones"
(2026-08-18). They share no surface. The arc CLOSE is a separate, later
session.

Author rulings taken here, all under the standing "Q1 accept all
recommendations" (2026-08-18): **R4-Q4 (a)** (gate the front door at
zero bare fences now, register the rest), **R4-Q5 (b)** (a staleness
tripwire over the five hand-owned SVGs) and **(d)** scoped to the
`trajectory-computation.md` mermaid block, **R4-Q9** (close
`#intake-staging-dir`, row the corpus-player slices).

Rows: D6-1, D7-3, D1-1, D8-1, D8-2 (partly), D5-2, D7-5, L3-3, L3-5,
L3-6, L3-7, L3-8, L3-11.

### Step 0 — the measurements everything below rests on

`bin/preflight` **exit 0**, every check disclosed: last five CI runs on
`main` all green; edit root `/home/mg/src/ehr-testing-tools`, not under
`/mnt/`, **`core.fileMode` true and `core.ignorecase` unset**; tree
clean including untracked; local HEAD `bdc10ee` == `origin/main`; last
stable tag `stable-20260819-review-4-fix-3-environment-and-result-or-loud`
@ `ae396cf`, HEAD untagged and **no tag owed at Step 0**.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **358 zero-failure blocks
/ 4,040 tests / 18,110 assertions**, reconciling exactly against
**ADR-0157's own Verification section**, which carries `358 / 4,040 /
18,110` as its CLOSE. `clojure -M:poly check` **OK**. Reading sets, from
the generated `state-derived.md`, all green: `:corpus` 1821/2045,
`:docs` 728/785, `:judge` 915/1000, `:onboarding` 1450/1530, `:sim`
1267/1405.

**The prompt's premise about the edit root was wrong, in the safe
direction, and this matters for the row it closes.** The prompt says a
session "cannot probe penny's edit root and must not claim to", so the
residue row should close as *author-confirmed* only. But this session
RUNS in that edit root — `git reflog` shows the clone of 2026-07-28 and
every session since committing here — so the author's payment is
directly verifiable, and was verified rather than taken on report:

- `git ls-files -s` `100644` files that are executable on disk: **0**
  (the row's own 360).
- CR bytes in the three named `openai.yaml` mirrors: **0** each (the
  row's own 3).
- `git status --porcelain=v1 --untracked-files=all`: empty.
- `bin/preflight` exit 0 with `OK: core.fileMode is true` and `OK:
  core.ignorecase is unset`.

So `roadmap.md#edit-root-worktree-residue` closes **author-paid and
session-verified**, which is strictly stronger than the close text the
prompt scripted.

The other five probes:

- **(a)** The defspec's facility, verbatim: `check_test.clj:487`,
  `{:id :t :wards [{:id :ed ... :beds 0 :surge-slots 15 ...} {:id :renal
  ... :beds 1 :surge-slots 0 ...}]}`, 150 trials, no churn, no pathways.
- **(b)** Bare fences on README+SETUP: **4**, as predicted. Instrument:
  `bin/fence-census` (ADR-0140), whose registry is
  `exercised-sources.edn`. Repo-wide 26 exercised / 50 bare of 76.
- **(c)** The five SVG banners' cited sources, and whether each moved:
  **4 of 5 RED**. Reviewed in Step 2 below; only ONE is a true stale
  asset.
- **(d)** The `%% Arrow N` numbering-source claim: **VERIFIED, and from
  two sides.** `parse_file` numbers by `enumerate(joined, start=1)` over
  the INPUT equations file's joined lines, and
  `docs_tooling/pipeline.clj`'s own `generated-comment-header` docstring
  says so in the repo's own words — *"the converter's `%% Arrow N`
  comments derive from the equations file's own line numbering, so this
  banner's length is load-bearing"*, which is why THAT banner is pinned
  at exactly four lines. Nothing reads the converter's OUTPUT back as
  input, so output lines cannot renumber anything. This decided that the
  28 artifacts' diffs would be banner-only; they were.
- **(e)** The converter write set: **28** — `docs/dev/pipeline.md`, 22
  `docs/use-cases/*.md`, `sim-theory-diagram.{md,mermaid}`, 3 palgebra
  `.mermaid`. `docs/use-cases.md` (the index) carries no mermaid block
  and is correctly excluded.

### D6-1 — the sample was in the wrong place, and the row's own remedy was not enough

The defspec named `every-m1-run-satisfies-the-invariant-catalog`
promised that **every** m1 run satisfies **the invariant catalog**, and
ran 150 trials against a FIXED facility that made ADR-0153's defect
unreachable at any trial count.

**Red-first on a historical tree**, the strongest form the plan names: a
scratch worktree at the pre-fix engine, the widened defspec
cherry-picked in, run there and at HEAD.

**A correction to the prompt.** It names `4d6ff78` as "the commit before
the 0153 fix". `4d6ff78` is ADR-0153's own ADDENDUM, which is after the
fix. The pre-fix engine is **`ceedcfd`** (ADR-0153's red commit; the fix
is `885b1c9`, and `git diff ceedcfd 885b1c9` is 48/11 lines in
`engine.clj` alone). All historical runs below are at `ceedcfd`, and
`check_test.clj` is byte-identical at `ceedcfd` and HEAD, so the
cherry-pick is clean.

**First historical red**, recorded as the plan asks: seed
**1787179118735**, failed after **24 trials** in 665 ms. The shrunk
counterexample: 21 patients, arrival-gap 44, facility ED 0/5, Renal
**1 bed / 2 surge**, Cardiology 2/1; pathway weights Renal 2 /
Cardiology 3 / Emergency 1; **churn `nil`**.

That last field is why this row's own remedy text is corrected here.
D6-1 asks for a generated facility with mixed bed classes plus churn.
Measured against `ceedcfd`:

| configuration | trials | violations |
|---|---|---|
| mixed wards + hot churn, DEFAULT (single-home) pathway | 400 | **0** |
| mixed wards + multi-home pathways, no churn | 400 | 2 (0.5%) |
| mixed wards + multi-home pathways + `churn/sample-profile` | 400 | 11 (2.8%) |

**Multiple home wards is the necessary ingredient, and the row does not
name it.** ADR-0153's route needs a bed to vacate in a ward *without*
pulling anyone home to it, and the default pathway admits every patient
to Renal, so every boarder's home ward is the ward it would be pulled
back into — a single-home-ward population can never produce it. Churn is
**amplifying, roughly six-fold, not required**; an earlier probe of this
session concluded churn was necessary, and the defspec's own first
failing trial refuted that.

The landed defspec generates the facility (Renal always mixed;
Cardiology surge may be 0 so the degenerate single-class shape stays in
the sample), a weighted three-ward pathway pool, and churn on two trials
in three using the repo's own sanctioned `churn/sample-profile` rather
than a hand-tuned one. Ward ids and names are fixed because
`default-provider-templates` is ward-eligible for exactly
`:ed`/`:renal`/`:cardiology` — a generated ward id would sample a config
error rather than a facility.

**Trial count moved from 150 to 300, on evidence.** At 150 the widened
sample went red in **5 of 6** historical runs; at 300, **8 of 8**, for
3.3 s against 2.0 s. In a session whose subject is sampling adequacy,
1.3 s was the wrong thing to save. At HEAD: **6 of 6 green at 300**,
plus 900 further trials green at 150 — 2,700 trials with no flake, and
no sign of `roadmap.md#bed-ready-vacancy-cascade` (a realism gap, not an
invariant violation) surfacing.

A mechanism-sanity case asserts the three generators actually vary what
they claim to: every sampled facility has a ward with both bed classes,
capacities vary, every pathway pool names more than one home ward, and
churn is present on some trials and absent on others.

### R4-Q4 (a) — the front door, and the ruling's own premise

`bare-on-README+SETUP = 0` is gated. The four fences it costs turned out
not to be four cheap ones:

| fence | disposition |
|---|---|
| `README.md:27` "See it run" | **exempt** — teaches `--rate 60`, "an hour of hospital time per minute"; weeks of wallclock on a ten-year corpus. Covered at a runnable rate by `bin/demo-exerciser-clinic-decade`. |
| `SETUP.md` `java -version` | **exercised** — by the new `bin/setup-verification-ladder`. |
| `SETUP.md` `sudo apt install` | **exempt** — installs system packages as root. |
| `SETUP.md` toolchain ladder | **exercised** — `bin/setup-verification-ladder`, verbatim. |
| `SETUP.md` `git clone` | **exempt** — a checker inside a clone cannot fresh-clone as a check OF that clone. |

Two mechanisms were needed and both are declared rather than worked
around. `bin/fence-census` gains a third disposition, **`exempt`**,
backed by `fence-exemptions.edn` where each row carries a reason a human
wrote; folding those fences into `exercised` would overstate what is
re-run, and leaving them `bare` makes the gate un-passable and therefore
ignored. **`bare` stays the default**, so the ratchet is real: a new
front-door fence is red until someone either exercises it or writes down
why they cannot.

`SETUP.md`'s verification ladder is **split** into a toolchain fence and
a clone-and-verify fence, so the exercised block is runnable in full
rather than matched on its first line while its `git clone` goes unrun —
the "gate whose population is narrower than the class it is read as
enforcing" pattern this review keeps finding. Reaching it needed
`:single-fence` to grow an optional **`:fence-index`** (the ladder is
SETUP.md's SECOND ```sh fence), which says the choice out loud instead
of re-tagging a fence's language until first-match lands right.

Census before and after: **26 exercised / 0 exempt / 50 bare of 76**
command fences, to **28 / 3 / 46 of 77** (the split adds one). Front
door: 4 bare to **0**. Population closed at 229 both times.

The remainder of R-F8's rule — the manual's 21 and use-cases' 13 — is
`roadmap.md#reader-path-fence-battery`, with its own session and the
primed-artifact-cache caveat recorded. Expect the front door's own
ratio there: some exercised, some needing declared exemptions.

### R4-Q5 (b) — a tripwire born red, with its one true finding rowed

`hand-owned-assets.edn` + `hand-owned-asset-freshness-test`: five rows,
each comparing its cited source's own last-change commit against a
recorded `:reviewed-at` witness. Also asserted: population closure
against the tree, both files existing, and **each asset's own banner
naming the source its row claims** — without that, the registry could
drift from the artifact it watches, which is L3-5's defect one level up.

Born **red on 4 of 5**, and the dispositions are the point:

| asset | source moved at | verdict |
|---|---|---|
| `gt-emitters.svg` | `dc13a17` (ADR-0142) | **fresh** — a 31-line addendum appended to section **5**; this asset's trigger names section **4**'s equations, and the addendum itself says "the arrow is untouched". |
| `straddle-timeline.svg` | `214b0ec` | **fresh** — the busy-tuesday to clinic-decade rename. Every changed line mentions it, and the cited batch values (`:interval-ms 3600000`, `:start-ms 1786406400000`) are byte-identical at HEAD. |
| `verdict-ranking.svg` | `2db2dee` | **fresh** — one Companion link-path rewrite in a dead-link sweep. The ranking is untouched. |
| `two-clocks.svg` | `dc13a17` (ADR-0142) | **STALE** |
| `inject-expect-loop.svg` | — | fresh, never fired. |

`two-clocks.svg` asserts *"exactly two timestamp-bearing fields this
workspace's emitter renders today are MSH-7 ... and EVN-2"*. ADR-0142
made that false — OBR-7 and OBX-14 now render on all three ORU shapes —
and its addendum names this very audit as what dated it: *"It no longer
is, for results."* The DRAWING (one ADT^A01 carrying two fields) is
still right for ADT; the audit sentence around it is not. Rowed as
`roadmap.md#two-clocks-asset-field-audit`, and the gate asserts that
anchor still exists, so retiring the finding turns the suite red rather
than quietly closing it.

**What this instrument trusts, stated plainly.** It compares whole FILES
while the banners' triggers name SECTIONS, so it is deliberately
over-sensitive: 3 of its 4 first reds were unrelated edits. That is not
noise to be tuned away — it is why the disposition is a recorded human
REVIEW, and `:reviewed-at` is where that review lands. A section-level
tripwire would be quieter and would need a section-extraction parser per
banner, each its own silent-mismatch risk.

R4-Q5 **(d)**, scoped to `trajectory-computation.md`'s embedded mermaid
block: a dated acceptance line at the block, so the next review stops
re-finding it as an unwatched surface.

### D1-1 — the rule, not a retro-edit

Four ADRs cite a suite figure to an ADR that does not carry it
(0150→0149, 0151→0150, 0152→0151, 0153→0152); the figure lives in the
session RECORD. Per R-RP these are **not** rewritten — they are listed
here and left.

The remedy is `build-session/SKILL.md` step 14: an ADR records its own
BASELINE, so a reconciliation sentence must cite the document that
CARRIES the figure. Worth recording that the practice had already
started one ADR ahead of the rule: of ADR-0150 through ADR-0157, **only
ADR-0157 records its own CLOSE figure** — which is why this ADR's own
Step 0 citation above resolves.

### Session H — six artifacts that did not name what moves them

- **L3-3.** `state_derived.clj` gains an `inputs` definition that
  `collect` **reads through** and `render` prints. One definition, so the
  emitted list and the actual reads cannot drift; a list merely placed
  beside `collect` would be one more hand-maintained claim about the
  code next to it, which is the defect class this row belongs to. The
  page now carries `## What this page reads` (14 inputs by kind and what
  each feeds) plus the 26 line-counted reading-set members — matching
  L3-3's own re-derived 26 exactly.
- **L3-6 / L3-7.** The converter emits its own four-line `%%` banner,
  which rides through every splice: the 4 committed `.mermaid` keep it
  directly, and `pipeline.md`, `sim-theory-diagram.md` and the 22
  use-case pages embed it inside their ```mermaid fences, where `%%` is a
  comment and renders as nothing. **Named at 1 of 28 before, 28 of 28
  after.** Diff class, checked before committing and as Step 0(d)
  predicted: **banner-only** — every non-`%%` changed line in the whole
  regeneration is in the converter's own source (4 deletions, all there).
- **L3-8.** `AGENTS.md`'s hand list of four is replaced by a pointer to
  `state-derived.md`'s own generated `## Generated surface` section,
  rendered from CI's freshness-diff list — which
  `docsgen-closure-test` already gates equal, both directions, to what
  the make recipes declare. Naming which source the pointer uses is
  part of the fix.
- **L3-11 / L3-7's second half.** `demos/traces/README.md` names `make
  traces` and `bin/regen-traces` at the front door, and all six
  per-trace READMEs carry a per-directory note. Re-derived at this tip:
  **2 of 6** per-dir READMEs mentioned `make traces` (the row said 3 of
  7) and **0** at the front door; now 6 of 6 and 1 of 1. The notes
  distinguish the **14 derived captures** from the **3 hand-authored
  `config*.edn` inputs** `bin/regen-traces` only reads — a distinction
  the first draft of those notes got wrong and this session corrected
  against the script.
- **L3-5.** `docs/formats.md`'s banner names BOTH inputs and the leaf
  target: `event_log_doc/render` slurps the schema AND the examples, and
  the examples supply every rendered example on the page.

### The oracle

The converter and the `state-derived` renderer are both off the digest
path. Predicted IDENTICAL and asserted: no oracle root or `digest.clj`
change in this session, and the suite's own oracle coverage is green at
every run below.

### Two departures from the prompt's own plan, disclosed

1. **The two roadmap CLOSURES are at the close commit, not Step 2.**
   `done-pointer-adr-test` requires a Done row to cite an ADR present in
   `notes/ADRs.md`, and ADR-0158 did not exist at Step 2. The gate said
   so; the rows moved.
2. **`done-pointer-adr-test`'s non-vacuity check was corrected.** It
   compared DISTINCT cited ADRs against BULLET count, a proxy that held
   only while no ADR had ever closed two roadmap rows — and this session
   closes two. It now counts bullets carrying a pointer (ADR or sha):
   immune to the duplicate, and strictly stronger, since a bullet that
   genuinely lost its pointer could previously hide behind any duplicate
   elsewhere. Fix-forward with disclosure, one defensible reading
   (`rulings.md#R-stop-only-on-two-defensible-readings`).

### Rows opened and closed

**Opened (4):** `#two-clocks-asset-field-audit` (the tripwire's one true
finding), `#reader-path-fence-battery` (R4-Q4 (a)'s deferred 34),
`#backtick-shorthand-and-denylist-widening` (D7-3's pair, ruled
2026-08-15 with no register home through one arc close and fourteen
ADRs), `#corpus-player-slices` (D7-5, chartered ADR-0014, never rowed;
unpriced, needs its own ruling).

**Closed (2):** `#edit-root-worktree-residue` (author-paid AND
session-verified, above), `#intake-staging-dir` (per R4-Q9's own
recommendation; nineteen days deferred with the absence of a trigger
declared in the row itself, so it could never fire — re-open on the
first real staging need, with the trigger stated then).

### Consequences

- The invariant catalog's own property test now samples the
  configuration space its name claims, and is proven to catch the one
  real `src` defect of the previous window.
- The front door cannot regress: a new README or SETUP command fence is
  red until exercised or exempted-with-a-reason.
- Five hand-owned SVGs are watched, and one of them is now known stale
  with a row someone must action.
- A converter change's blast radius is greppable from the artifacts.
- `AGENTS.md` no longer understates its own generated surface by 49
  files.

### Receipts

**Suite**, `make test` unpiped, wrapper ending `exit "$MAKE_EXIT"`,
`MAKE_EXIT=0` at every gate:

| point | blocks | tests | assertions |
|---|---|---|---|
| Baseline, `bdc10ee` | 358 | 4,040 | 18,110 |
| Session F close, `3c4e346` | 362 | 4,060 | 18,200 |
| Session H close, `d02a085` | 364 | 4,070 | 18,304 |
| This close | 364 | 4,070 | 18,304 |

The close run is identical to Session H's, which is the right answer: this
commit changes only ADR, register and record prose, plus the two generated
indexes that follow from them.

`clojure -M:poly check` **OK** at every gate. Zero failures, zero errors
at every gate; no test deleted, no red left planted.

**Fence census:** 26 exercised / 0 exempt / 50 bare of 76 command
fences, to **28 / 3 / 46 of 77**. Front door 4 bare to **0**. Population
closed at 228 then 229.

**Reading sets at close** (`R-register-hygiene-at-close`), all green,
no baseline moved: `:corpus` 1832/2045, `:docs` 735/785, `:judge`
922/1000, `:onboarding` 1482/1530, `:sim` 1274/1405.

**`bin/post-push-verify`:**

- after `3c4e346`: remote tip matches; every commit message in
  `bdc10ee..3c4e346` pure ASCII; CI run `32312928469`, later
  **completed / success**.
- after `d02a085`: remote tip matches; `3c4e346..d02a085` pure ASCII;
  CI run not yet indexed at the time of the check, disclosed.

**Tag:** none owed at Step 0. This session's own close tag is payable
in session if its tip run concludes success while the session is open
(`rulings.md#R-session-verifies-ci-via-gh`); the outcome is recorded in
the session record and, if paid after this commit, in an addendum here.

### Read-back against the fence

Files touched, against the prompt's own list: the defspec and its
generators (test ns, no engine or check logic change), the
`state-derived` renderer, the python converter (banner emission only),
docs-tooling tests, the exercised fences' script and register, the SVG
tripwire test and registry, `AGENTS.md`, READMEs, the `formats.md`
banner, `trajectory-computation.md` (one block), registers. Beyond the
list and disclosed above: `strip_fresh.clj` and `exercised_sources.clj`
(the `:fence-index` widening the front-door gate needed) and
`done_pointer_adr_test.clj` (the corrected vacuity measure). No oracle
root or digest change; no test deletions; every planted red withdrawn;
historical-tree work in a scratch worktree, never on HEAD's branch.
