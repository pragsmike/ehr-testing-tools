## ADR-0159 — Repo-review 4 arc close: thirty-eight rows moved, every citation verified, and four things the audit found that the arc did not

**Status:** Accepted (author-directed, autonomous session per R30 — the
arc's own dedicated close session, the shape ADR-0139 set), 2026-08-20.

### Context

Prior: `notes/adr/0158-sampling-adequacy-and-artifact-provenance.md`
and its addendum closed the last of review 4's fix sessions. The arc in
full, in commit order from `0a07195` (ADR-0154's close) to `e967fd7`:

- `0a07195` — **ADR-0154**, the assessment: a 72-row register
  (`.agents/plans/2026-08-18-repo-review-findings.md`) and the
  mitigation plan (`.agents/plans/2026-08-18-repo-review-4-plan.md`),
  findings-only, nothing moved. 10 rulings owed, 8 sessions proposed.
- `660b7bf` and before — **ADR-0155**, fix 1, plan sessions **G + A**:
  the docsgen population closed as a class, and four harness surfaces
  that reported successes they did not have.
- `841fb75` / `04b6f66` / `c80b558` — **ADR-0156**, fix 2, **E + C**:
  the oracle states what it cannot see, and three laws get gates.
- `ae396cf` / `bdc10ee` — **ADR-0157**, fix 3, **B + D**: environment
  residue at its root, `R-io-result-or-loud` widened to the class it
  names, and a `commit-msg` hook.
- `3c4e346` / `d02a085` / `6ed767c` / `e967fd7` — **ADR-0158**, fix 4,
  **F + H**: sampling adequacy, and every artifact points back at its
  inputs.

The author's rulings of 2026-08-18 — *"Q1 accept all recommendations.
Q2 that order ok. Q3 pair small ones."* — fixed the order **G A E B C D
F H** and paired them, so the plan's eight sessions executed as four.
*"go"* (2026-08-19) chartered this close.

**This close audits the arc; it does not extend it.** Its own driving
prompt states the discipline: a defect found here is **rowed, not
fixed, however small**. Four were found and four are rowed. Nothing in
`src`, `test`, `bin` or any skill was touched.

### Decision

Author rulings, recorded verbatim. `[A]` author-ruled, `[C]`
channel-inferred.

**"go"** `[A, 2026-08-19]` — the arc closes in its own session, in
ADR-0139's shape. **This session.**

**`#oracle-coverage-roots` re-prioritised below live work**
`[A, 2026-08-19, channel-proposed and author-seen, unobjected]` —
executed below, PRIORITY 3 → 22, moved to the end of `## Next`. **No
other priority moved.** One further move looks arguable and is
**proposed here rather than taken**: `#attic-rotation-law` (PRIORITY 5)
now guards 69 `## Done` pointers against a section whose own header
says "current arc only", up from 44 at review 3's close and 66 at
review 4's finding day — it grows every close, including this one, and
it is the only row on the board whose cost rises with time. A ruling to
raise it, or to price it, belongs to the author.

**AR-AC4-1 `[C]`** — this ADR in ADR-0139's shape: the five audit
tables the prompt names ((a) citation verification, (b) residue
partition, (c) errata recording status, (d) ledger reconciliation, (e)
rulings landed), review 3's twelve-row watch-list dispositioned row by
row, review 5's own watch-list, and the cadence computation done from
the ruling's own text. **Executed**, below.

**AR-AC4-2 `[C]`** — no register row is rewritten and no review-day
score is edited. The register is history; this ADR carries the audit.
**Held** — the register's only changes are dated appends under the
residue rows and one header line.

### Step 0

`bin/preflight` plain, **exit 0, no findings**: last five CI runs on
`main` all green (`e967fd7`, `6ed767c`, `d02a085`, `3c4e346`,
`bdc10ee`, newest first); edit root `/home/mg/src/ehr-testing-tools`,
not under `/mnt/`; `core.fileMode` **true** and `core.ignorecase`
**unset** — ADR-0157's two new checks, still holding in the edit root
the author paid down at ADR-0158; working tree clean including
untracked; local HEAD `e967fd7c5dde…` matches `origin/main`; last
`stable-*` tag `stable-20260819-review-4-fix-4-sampling-and-provenance`
at `6ed767cc…`, HEAD untagged and **no tag owed**, as the prompt states.

Baseline `make test` to a log by redirect, `MAKE_EXIT` captured in its
own file, wrapper ending `exit "$MAKE_EXIT"`: **MAKE_EXIT=0, 364
zero-failure blocks / 4,070 tests / 18,304 assertions**,
`grep -cE '^(FAIL|ERROR) in'` = **0**. That reconciles **exactly**
against ADR-0158's own recorded close figure, which that ADR carries
itself — the practice `build-session` step 14 landed this arc, working
on its first use. `clojure -M:poly check` and `bin/verify-nist-lock`
green inside the target.

Reading sets, from the generated `state-derived.md` rather than from
prose: `:corpus` 1832/2045, `:docs` 735/785, `:judge` 922/1000,
`:onboarding` **1484/1530**, `:sim` 1274/1405. All under budget, no
baseline moved. `:onboarding`'s 46 lines is the tightest, and this
close spends into it. **See errata (8): 1484 is not the figure ADR-0158
recorded.**

**Docs-only, and the prediction stated up front:** close-suite delta
**ZERO**. Asserted in Verification.

---

## (a) Every FIXED append verified against the ADR that claims it

Method, scripted rather than sampled: extract every row whose first
cell is a `D<n>-<id>` or `L<n>-<id>` label (**72**, matching the
register's own count exactly); read the disposition cell — the LAST
cell in the eight dimension tables, the **second-to-last** in the three
sub-agent tables, which carry an extra `provenance` column; extract
every `ADR-015x` reference from it; then grep each cited ADR for the
row id under a word boundary that does not let `L2-1` match inside
`L2-10`.

**38 (row → ADR) pairs.** Every cited ADR exists. **Zero pairs where
the ADR does not claim the row.** The prompt's STOP condition did not
fire.

| citing ADR | rows | claimed by row id | claimed by substance only |
|---|---:|---:|---:|
| ADR-0155 | 12 | 11 | 1 (L2-1) |
| ADR-0156 | 11 | 6 | 5 (D2-6, D3-2, D4-4, L1-3, L1-5) |
| ADR-0157 | 2 | 1 | 1 (D4-1) |
| ADR-0158 | 13 | 13 | 0 |
| **total** | **38** | **31** | **7** |

**The seven, each verified by reading rather than by grep** — this is
the ADR-0139 precedent, where a moved cell "carried by substance and by
ruling name in the body" was accepted as claimed:

- **D2-6** (`--amend` has no law) → ADR-0156:13 names **R4-Q1**, and
  `rulings.md#R-amend-unpushed-message-only` is the row it landed.
- **D3-2** (`make ci-parity` is D3-1's third answer) → ADR-0156:13
  names **R4-Q7**; :225 records `repo-review/SKILL.md` D3 naming the
  target.
- **D4-4** (the auditor's own false green) → ADR-0156:14 names
  **R4-Q8**; :229 records the rubric's new zero-population sentence.
- **L1-3** (Z-segments were a quarter of the surface) → ADR-0156:170,
  *"ADR-0150 (a) generalized."*
- **L1-5** (the docstring's eleven roots vs the map's 35) →
  ADR-0156:179, *"The docstring states its own population."*
- **D4-1** (`.mkdirs`/`.delete` outside the lint) → ADR-0157:68-78 and
  its 13-site census table; the ADR's own title names the widening.
- **L2-1** (the mask migrated to the wrapper) → ADR-0155:141-144, *"a
  wrapper that captures `MAKE_EXIT` ENDS with `exit "$MAKE_EXIT"`"*, on
  all four surfaces.

**Misses: zero.** But the shape is worth naming, because it is this
arc's own D1-1 one column over: a register cell cites an ADR by number,
and in seven cases the ADR never names the row, so a reader following
the citation must reconstruct the substance to confirm the claim.
ADR-0158 landed the rule that a **suite figure** is cited to the
document that carries it; there is no sibling rule for a **row id**.
Rowed for review 5 (watch row W-3), not fixed — the seven ADRs are
history and `R-RP` keeps them so.

---

## (b) The residue partition — 34 rows, every one placed

72 rows total. 38 moved (a). **34 residue.** Every residue row falls
into exactly one bucket, and the bucket counts sum to the row count.

| bucket | rows | how it is carried |
|---|---:|---|
| **close-as-fine, CONFIRMED** | 24 | the register; nothing owed |
| **intake, carried by register + successor watch-list** | 8 | review 5's watch-list, below |
| **intake, ALSO rowed on the roadmap** | 1 | D7-2 → `roadmap.md#attic-rotation-law` |
| **superseded by its referent** | 1 | D2-5 → L2-1, FIXED ADR-0155 |
| **total** | **34** | |

24 + 8 + 1 + 1 = **34**; 38 + 34 = **72**. Matches.

**A definitional gap in the close's own charter, disclosed rather than
converted into eight findings.** The prompt's four buckets are
{close-as-fine CONFIRMED, intake→**rowed** (verify the roadmap row
exists), deferred-with-row, superseded}, and says a row fitting none is
a finding. Eight intake rows have **no roadmap row** — L1-6, L1-7,
L1-9, L2-7, L2-9, L3-12, L3-13, L3-16 — and would be eight findings
under a literal reading. They are not, and the precedent is exact:
review 3's three intake rows (D3-1, D6-4, D8-5) likewise had no roadmap
row and passed to review 4 on ADR-0139's watch-list, where all three
were then discharged or answered. **`intake` in this repo means
"recorded as a coverage gap, not chartered"** — the register plus the
successor review is its home, and `R-unregistered-request-gets-a-row`
governs unregistered *standing requests*, which these are not. The
fifth bucket is named here so review 5 does not re-litigate it.

**The `deferred-with-row` bucket is empty on the residue side, and that
is correct.** The 34-fence class is not residue: **D8-2 is a MOVED
row** — PARTLY FIXED by ADR-0158, front door gated at zero, remainder
rowed as `roadmap.md#reader-path-fence-battery`. The same holds for the
arc's other partial, **D3-1** (PARTLY FIXED ADR-0157, remainder
`#edit-root-worktree-residue`, since CLOSED by ADR-0158). Both partials
carry a row; neither is unplaced.

**Six close-as-fine rows re-derived at this tip rather than accepted**
(cheap probes only; the rest are marked confirmed-by-review):

| row | claim | re-derived |
|---|---|---|
| D1-3 | `state.md`'s 16 cited paths all resolve | **16/16**, once the five `.agents/`-relative cites are resolved from the file's own directory and `sim-theory.edn` from `components/sim/docs/` |
| D1-4 | `state.md` at 119/120 lines | **119**, one line of headroom, unchanged across the whole arc |
| D2-3 | skills mirror byte-identical, zero drift | **byte-identical**, same file set both sides, `diff -rq` silent — **but 59 files, not the 60 the row states**; see errata (9) |
| D3-3 | `.gitattributes` determinism; ADR-0149's CRLF finding stays closed | **0 CR bytes** in all six `demos/traces/**/ground-truth.edn` at HEAD in the edit root; `* text=auto eol=lf` plus the six named `-text` rationales (seven rules) intact |
| D5-4 | no second mirrored pair | confirmed; D5-4 repeats the same 60 |
| L2-11 | no live gate command piped into `tee`/`tail`/`head` | grep over `bin`, `.githooks`, `Makefile`, `.github`, `.agents/skills`: **zero hits** |

---

## (c) The arc's errata, and which are recorded in the repo

The prompt names seven. **Six are repo-recorded; one is not.** Three
more were found by this audit and are recorded here for the first time.
The test is the one review 5's history scan will apply: is the
correction in an ADR or a register, or only in a transcript?

| # | errata | recorded? | where |
|---:|---|---|---|
| 1 | `4d6ff78` called "the commit before the 0153 fix"; it is ADR-0153's **addendum** (pre-fix is `ceedcfd`) | **yes** | ADR-0158:98; record :40 |
| 2 | *"`state-derived.md` … counted by four reading sets"* | **NO** | see below |
| 3 | D6-1's own remedy text insufficient — mixed wards alone give 0.5%, churn is the second ingredient | **yes** | ADR-0158:87-120; record :77-80 |
| 4 | R4-Q4's premise that the four front-door fences are cheap — two cannot be run without lying about them | **yes** | ADR-0158:153-188 (the `exempt` disposition exists because of it) |
| 5 | the two PARTLY-FIXED rows (D3-1, D8-2), each with its remainder rowed | **yes** | register cells; `#edit-root-worktree-residue`, `#reader-path-fence-battery` |
| 6 | fix 2's own last commit carried a Unicode ellipsis and failed `post-push-verify` check 2, un-amendable by the law it had landed hours earlier | **yes** | ADR-0156:312-355; `#commit-msg-ascii-hook`, CLOSED ADR-0157 |
| 7 | fix 1 did not run the oracle; the gap was carried and closed by fix 2 | **yes** | ADR-0156:46-49 |
| 8 | ADR-0158 records `:onboarding` **1482/1530** at its close; the generated register it commits in the same commit says **1484** | **NO** | found here |
| 9 | the skills mirror is **59** files, not 60 — at review day and at HEAD | **NO** | found here |
| 10 | the fix sessions are titled 1/5, 2/5, 3/5, **4/4** | **NO** | found here |

**(2) — the four-reading-sets claim, and why it matters more than its
size.** The claim originates in the plan
(`2026-08-18-repo-review-4-plan.md`, Session H's **Watch** line) and
was copied verbatim into fix 4's prompt. Re-derived from
`.agents/reading-sets.edn`: **`state-derived.md` appears in ZERO
reading sets.** Exactly two paths are in four or more — `AGENTS.md` and
`build-session/SKILL.md`, both in all five — and those are what actually
moved fix 4's budgets. The session measured all five sets correctly and
reported them; it never noticed that the premise it was measuring
against was false. Under `R-RP` the Watch line itself stays as written;
this close appends a dated erratum beside it rather than editing it. It is
this arc's own carry-forward class: a statement about a **population**
written from memory, in a plan whose central finding is that
populations get asserted instead of enumerated.

**(8) — a suite-adjacent figure cited to a document that carries a
different one.** `git show 6ed767c:.agents/state-derived.md` and
`git show e967fd7:.agents/state-derived.md` both read `:onboarding | 10
| 1484 | 1530 | 1530 | 46`. ADR-0158's "Reading sets at close" section
and its session record both say **1482 / 48 headroom**. The other four
sets agree exactly. This is D1-1's class inside the section
`R-register-hygiene-at-close` exists to govern, in the last ADR of the
arc that fixed D1-1. Not rewritten (`R-RP`); listed.

**(9) — one un-re-derived count, propagated three times.** Register
D2-3, register D5-4 and the plan's Part 3 all say the mirror is 60
files. `git ls-tree -r --name-only 4d6ff78 .agents/skills | wc -l` =
**59**, and 59 at HEAD, both sides, same name set. The *finding* is
untouched — the mirror is byte-identical and its gate is complete — but
the figure was written once and carried twice without re-derivation,
which is the class review 3 named as *carry-forward* errata.

**(10) — a denominator that corrected itself silently.** ADR-0155,
0156 and 0157 are titled "fix 1/5", "2/5", "3/5"; ADR-0158 is titled
"fix **4/4**". The pairing ruling of 2026-08-18 turned eight sessions
into four, and the denominator followed at the last ADR without any
document saying it had. Harmless, and worth one line so a reader
counting titles does not go looking for a fifth.

**The close's own prompt, corrected in the tradition ADR-0139 set.** It
states *"48 `FIXED ADR-015x` appends; residue = 72 - 48 = 24-ish"*. The
mechanical count is **37 `FIXED ADR-015x` occurrences** (35 plain, 2
`PARTLY FIXED`), all 37 inside row cells, **38 rows moved** counting
D8-1's `CARRIED INTO`, and **34 residue**. 48 re-derives by no reading
of the register; the likeliest origin is **38 rows + 10 rulings**.
Corrected here rather than repeated, exactly as the review corrected
its own predecessor.

---

## (d) Ledger reconciliation — the roadmap row against the register

The `#repo-review-4` row's per-session tallies have never been checked
against the register as a set. They are checked here, mechanically, and
they hold.

| roadmap row says | rows it names | register extraction | delta |
|---|---:|---:|---:|
| FIX 1/5 (G+A) ADR-0155, **12 rows** | L3-1/L3-2/L3-4/L3-9/L3-10, L2-1..L2-6, L2-10 | **12** | **0** |
| FIX 2/5 (E+C) ADR-0156, **11 rows** | L1-1..L1-5, D2-1/D2-2/D2-4/D2-6, D3-2, D4-4 | **11** | **0** |
| FIX 3/5 (B+D) ADR-0157, **2 rows** + `#commit-msg-ascii-hook` | D4-1, D3-1 (partly) | **2** | **0** |
| FIX 4/4 (F+H) ADR-0158, **13 rows** | D6-1/D7-3/D1-1/D8-1/D8-2(part)/D5-2/D7-5, L3-3/L3-5/L3-6/L3-7/L3-8/L3-11 | **13** | **0** |
| **total** | | **38** | **0** |

**Ledger delta: ZERO, in every session and in the total.** Each fix
session's row named its rows by id at the time it landed them, and each
naming is exact. This is the first arc in which the ledger was written
incrementally by the sessions themselves rather than reconstructed at
the close, and it is the reason the reconciliation is a check rather
than a repair.

**And the arc's headline number, which the ledger makes safe to state:
every fix-session-candidate and every ruling-needed row moved.**

| original disposition | rows | moved | residue |
|---|---:|---:|---:|
| fix-session-candidate | 27 | **27** | 0 |
| ruling-needed | 10 | **10** | 0 |
| close-as-fine | 25 | 1 (D8-1, carried and re-measured) | 24 |
| intake | 9 | 0 | 9 |
| cross-reference | 1 | 0 | 1 |
| **total** | **72** | **38** | **34** |

Nothing the review proposed for a session or for a ruling is open by
neglect. What remains open is open **by its own disposition**.

---

## (e) The ten rulings, and where each landed

| ruling | subject | landed | how |
|---|---|---|---|
| **R4-Q1** (a) | `git commit --amend` had no law | ADR-0156 | `rulings.md#R-amend-unpushed-message-only`; `build-session` step 4 |
| **R4-Q2** (c) | `bin/preflight`'s exit code is a claim | ADR-0155 | `rulings.md#R-preflight-fail-closed`, `UNKNOWN:` branch |
| **R4-Q3** (a) | `post-push-verify` check 3 keeps AR-CI-4 | ADR-0155 | `gh` stderr no longer folded into a status field |
| **R4-Q4** (a) | the R-F8 fence rule, tiered | ADR-0158 | front door gated at zero bare; `#reader-path-fence-battery` for the 34 |
| **R4-Q5** (b)+(d) | hand-regenerated derived surfaces | ADR-0158 | `hand-owned-assets.edn` staleness tripwire over five SVGs; dated acceptance on the one mermaid block |
| **R4-Q6** (a),(a)/(b),(c) | the oracle's coverage claim | ADR-0156 | COVERAGE block; `#oracle-coverage-roots` priced and rowed; `R-oracle-script-contract` widened to `:require` |
| **R4-Q7** (a) | the cold-clone probe is `make ci-parity` | ADR-0156 | named in `repo-review/SKILL.md` D3 |
| **R4-Q8** (a) | a probe reporting zero asserts its population | ADR-0156 | `repo-review/SKILL.md` step 3 |
| **R4-Q9** | two register rows owed | ADR-0158 | `#intake-staging-dir` CLOSED; `#corpus-player-slices` opened |
| **R4-Q10** (d) | the docsgen/diff-list closure rule | ADR-0155 | closure gate over the leaf population |

**Ten of ten landed.** By session: ADR-0155 three (Q2, Q3, Q10),
ADR-0156 four (Q1, Q6, Q7, Q8), ADR-0158 three (Q4, Q5, Q9).
**ADR-0157 landed none** — it is the arc's one pure fix session, which
is why its register footprint is two rows and its roadmap line reads
smaller than its diff.

**`rulings.md` gains nothing at this close, and that is the finding.**
Grepped before assuming: the arc landed two new rows
(`R-preflight-fail-closed`, ADR-0155; `R-amend-unpushed-message-only`,
ADR-0156) and widened two in place (`R-oracle-script-contract`,
`R-io-result-or-loud`), 113 → **115** rows. Every law the arc made has
a row. The register is untouched here — except that widening the third
row left a hole, which is finding **F-2** below.

---

## Review 3's twelve-row watch-list, dispositioned across this arc

ADR-0139 handed review 4 twelve rows. The watch-list does not carry
forward silently: each is stated here, and review 5 gets a NEW list.

| inherited row | disposition at this close |
|---|---|
| **C-1** — ungated `.edn` → equations hop | **CLOSED** by ADR-0152, and closing it found the row **understated**: the `.edn` had already drifted and did not validate. |
| **C-2** — the CarePlan/Guard request | **ROWED, still OPEN** — `#careplan-guard-resolution` P4, and it grew: census S-2 folded in at ADR-0150. |
| **C-3** — attic rotation lapsed | **ROWED, OPEN, and worse each close** — `#attic-rotation-law` P5. `## Done` pointers: 44 at review 3's close, 66 at review 4's finding day, **69 today**, 70 after this commit. |
| **C-4** — `state_staleness_tripwire_test` enumerates filenames | **FIXED, better than recommended** — ADR-0139 offered two options; the fix took both (tree enumeration by first heading, **plus** a second gate asserting the filename convention). Verified at review 4 (D7-1). |
| **D8-5** — the live fence battery | **DISCHARGED** 2026-08-16 (ADR-0140); survivor re-measured at review 4 (D8-1) and again at ADR-0158; front door now gated at zero, remainder rowed. |
| **D3-1** — local cold-clone probe | **ANSWERED, and neither of its two options was right**: the method was never lost — `make ci-parity` (review-4 D3-2), now named in the rubric by R4-Q7. |
| **D6-4** — full window deviation read | **DISCHARGED** by review 4: all fourteen ADRs read in full, the cadence rule doing exactly what it was ruled for. |
| **D1-9 / D1-10** — shorthand and denylist widening | **ROWED at last** — `#backtick-shorthand-and-denylist-widening` P20 (ADR-0158, review-4 D7-3), after aging through one arc close and fourteen ADRs with no register home. Still open. |
| **D1-4** — compare the two sets, not their cardinalities | **HELD as method**, and it earned its keep: errata (9) above is exactly a cardinality carried without re-derivation. |
| **`:onboarding` headroom** | **RE-BASELINED** by ADR-0143's ratchet: 32 lines at review 3's close, 132 at review 4's Step 0, **46** today. Tightest of the five again. |
| **H-2 / H-3** — the two masked-exit classes | **THE WATCH FIRED**, exactly as written: L2-1 found a NEW masking shape (the wrapper's last command, not the gate's), fixed on four surfaces by ADR-0155. This is the watch-list mechanism paying for itself. |

**Ten of twelve closed, discharged, answered or fired; two (C-2, C-3)
live on as roadmap rows.** Review 4's own D7-1 scored ten-of-twelve on
finding day; the arc then rowed D1-9/D1-10, which is the eleventh and
twelfth.

---

## Review 5's inherited watch-list

Built from THIS arc. Each row states the probe, the dimension it
belongs to, and what would count as **fired**.

| # | item | dimension | probe | fired if |
|---|---|---|---|---|
| **W-1** | **The born-red gate discipline.** ADR-0158's SVG tripwire landed red on 4 of 5 and each red was dispositioned in writing, one of them into a roadmap row (`#two-clocks-asset-field-audit`) that the gate itself asserts still exists. That is a precedent, not a rule. | D2 | For every gate added since ADR-0159: was it born red? If so, where is the red's disposition recorded? | A gate was born red and its red was tuned away, or its finding has no row — or the practice has continued and now deserves a `rulings.md` row of its own. |
| **W-2** | **The `exempt` disposition has no ratchet.** `front_door_fence_gate_test` asserts (a) zero bare, (b) non-empty population, (c) no stale exemption, (d) every exemption carries a reason. **Nothing bounds the exempt COUNT.** `bare = 0` is satisfiable by exempting rather than exercising, with one plausible sentence per fence. | D2 | Count rows in `fence-exemptions.edn` against ADR-0158's **3**. For each new one, ask: could this fence have been exercised instead? | Exempt count rose and any new reason describes inconvenience rather than impossibility. |
| **W-3** | **An ADR that closes a register row does not have to name it.** 7 of this arc's 38 closures are carried by substance only (audit (a)). ADR-0158 landed the sibling rule for suite figures; the row-id case has none. | D7 | Re-run audit (a)'s script over review 5's own arc. | The ratio worsens, or a close finds a cell whose ADR genuinely does not claim it. |
| **W-4** | **`#two-clocks-asset-field-audit`** — the SVG's audit sentence is false since ADR-0142 (OBR-7/OBX-14 render on all three ORU shapes); the drawing is still right for ADT. | D5 | Is the row closed? Does `hand-owned-assets.edn` still name it? | Still open at review 5 — it will have outlived two arcs — or closed by retiring the finding rather than fixing the sentence. |
| **W-5** | **`#reader-path-fence-battery`** — R4-Q4 (a)'s deferred 34 (manual 21, use-cases 13). Priced real: several need a primed artifact cache, which is why D8-5 lapsed twice. | D8 | Re-measure with `bin/fence-census`; compare against ADR-0158's 28 exercised / 3 exempt / 46 bare of 77. | Bare count on the reader path did not fall, **or** it fell mostly by exemption (see W-2). |
| **W-6** | **The historical-red technique.** ADR-0158 proved D6-1's widened defspec red against a pre-fix engine in a scratch worktree, then green at HEAD. Used **once**. The prompt's own standard: worth a skill line once used twice. | D6 | Has any session since used it? | Used a second time and still not written down — then it is an instrument, and `build-session` owes it a step. |
| **W-7** | **Does any other property test have a fixed-shape blind spot?** D6-1's defect was not trial count but a fixed FACILITY that could not express the precondition. The workspace holds **80** `(defspec` forms across `components`, `bases` and `projects`, re-derived here (review 3's table recorded 105 under a counting definition this close did not reconcile — compare the SETS, D1-4). | D6 | For a sample of `defspec`s: what in the generator is FIXED, and can the fixed part express the branch the property vouches for? | Any defspec whose fixed shape structurally excludes a branch its own name claims. |
| **W-8** | **`state-derived` self-listing, adopted once.** L3-3 gave the renderer a single `inputs` declaration it emits as its own input list. Nothing else in the repo does this. | D5 | Which other generated artifacts hand-maintain a list of what they read? | A generated surface still hand-lists its inputs where the L3-3 treatment would apply. |
| **W-9** | **`R-preflight-fail-closed` in the wild.** Landed ADR-0155; this close is the first session after the arc to run `bin/preflight` cold. Exit 0, no friction. | D3 | Did any session between ADR-0159 and review 5 hit a `FINDING:`/`UNKNOWN:` it had to reason around? | A session reasoned around a non-zero preflight, or an `UNKNOWN:` was read as an OK. |
| **W-10** | **F-1 — a roadmap row can swallow another row's continuation lines** (finding below). | D7 | Re-run the F-1 probe: does every continuation line sit under the row whose subject it names? | Any row's continuation lines belong to a different row. |
| **W-11** | **F-2 — a widened `rulings.md` row need not say who widened it** (finding below). | D2 | Diff `rulings.md` across the review window; for each changed row, does the change name its ADR? | A clause landed with no attribution and no ADR names it either. |
| **W-12** | **The plan's own live falsehood** — errata (2). `state-derived.md` is in **zero** reading sets, and the plan still says four. | D1 | Grep the live plan/register surfaces for population claims and re-derive each. | Any live plan or register still asserts a population it never enumerated. |
| **W-13** | **`:onboarding` headroom, again.** 46 lines before this close, and this close spends into it. Tightest of five for the second review running. | D1 | Read `state-derived.md`'s reading-set table. | Under ~30 lines — expect to compact, and note that `R-budget-stop` makes the bump unavailable. |

**Thirteen rows.** Two candidates the prompt offered are deliberately
NOT on it: "D8-2's remainder" and "the 34-fence session" are **the same
roadmap row** and appear once, as W-5; and a `build-session` amendment
for the historical-red technique is W-6's *trigger*, not a row of its
own, because this close may not edit a skill.

---

## Four findings opened by this close (rowed, not fixed)

The close's fence is absolute: an audit that edits its subject is
neither. All four are registered and deliberately left.

**F-1 — a roadmap row swallowed another row's continuation lines, and
the row-contract gate cannot see it.** `## Done` reads:

    - CLOSED 2026-08-18 ADR-0150 **[event-log-shape-defects]** -- the Z-segment
    - CLOSED 2026-08-18 ADR-0152 **[sim-theory-edn-hop]**
      context asymmetry and S-6 fixed, S-4 confirmed closed with no code owed.
      Residue re-rowed rather than dropped: S-1 as `#reason-nil-drop-owes-a-bump`,
      S-2 folded into `#careplan-guard-resolution`, S-5 as
      `#surge-policy-self-check-202`.

`git log -L` on the row settles it: `eeb0299` (ADR-0150's own close)
wrote a four-line row; `c509e46` (ADR-0152's close) inserted its
one-line row **after the first line of it**. So ADR-0150's row now
states half a sentence, and ADR-0152's row carries five lines about the
event-log shape defects that have nothing to do with the sim-theory
hop. **`roadmap-lint-test` is green on both**, because the contract
gates the token, the slug, the six-line cap and the priority — row
SHAPE — and never asks whether a row's continuation lines are about the
row. Both resulting shapes are legal. This is the arc's own thesis
once more, in the register that holds the arc: **a gate whose
population is narrower than the class it is read as enforcing.** It
landed 2026-08-18, inside review 4's own window, with five of review
4's probes reading this file — and none of them looked at row
ownership.

**F-2 — the arc widened three `rulings.md` rows and attributed two.**
`R-oracle-script-contract` carries *"(widened ADR-0156)"*;
`R-io-result-or-loud` carries *"(ADR-0157)"*. `R-full-suite-before-push`
gained its whole `exit "$MAKE_EXIT"` clause at `660b7bf` (ADR-0155) —
and that commit also **deleted** the row's previous inline citation
(`ADR-0149 f.3`) while adding none. The row still reads `-- ADR-0150`,
so nothing in the register can route a reader from the wrapper clause
to the session that ruled it. `rulings-lint-test` is green: it gates
the row's shape and that the cited ADR resolves, not that every clause
names its own provenance.

**F-3 — the plan states a population it never enumerated, and the claim
is live.** Errata (2): `state-derived.md` is counted by **zero** reading
sets, not four. Rowed rather than corrected because correcting the plan
is editing the arc's own record, which this close may not do.

**F-4 — three figures in the arc's records disagree with the tree or
with the generated register.** Errata (8), (9) and (10): `:onboarding`
1482 vs the generated 1484, the mirror's 60 vs 59, and the 4/4
denominator. None changes a verdict; all three are the same
carry-forward class, and all three are listed under `R-RP` rather than
rewritten.

**F-1 and F-2 get a row of their own**, in ADR-0158's D7-3 shape (one
row for a pair ruled together): `roadmap.md#register-gate-row-ownership`,
PRIORITY 3 — both register contracts gate row SHAPE, not row OWNERSHIP,
and both are green over a live defect. **F-3 and F-4 get no row**: they
are errata standing under `R-RP`, recorded in the register's own close
note and in the plan, which is where a reader of those documents meets
them. All four are also on review 5's watch-list (W-10, W-11, W-12).

---

## Cadence — review 5's due point, computed from the ruling's own text

`.agents/rulings.md#R-review-cadence-in-adrs`:

> repo-review cadence is measured in ADRs, not days: the next review is
> chartered roughly **15 ADRs past the prior close** — ADR-0139

**Past the prior CLOSE, not the prior charter.** ADR-0139 worked the
arithmetic itself — *"This close is ADR-0139, so review 4 is chartered
at approximately ADR-0154"* — and review 4's assessment landed at
exactly ADR-0154. So:

**This close is ADR-0159. Review 5 is chartered at approximately
ADR-0174.**

The channel's own figure of ~ADR-0169 measures 15 from the **charter**
(0154), which is neither the row's wording nor its worked precedent. The
prompt instructed computing from the row's number rather than repeating
the channel's; done, and they differ by five ADRs. Recorded here so the
next session does not average them.

Review 4's window was ADR-0140 → ADR-0153, fourteen ADRs, and D6-4 —
review 3's one partial probe — was **discharged in full** at that width.
The cadence rule is working; there is no case for moving it.

---

## Verification

- `bin/preflight` plain, exit 0, all five checks reported above,
  including ADR-0157's two new edit-root checks.
- Full `make test`, unpiped to a log by redirect with `MAKE_EXIT`
  captured in its own file and the wrapper ending `exit "$MAKE_EXIT"`
  (`R-full-suite-before-push`), at Step 0 and again at the close.
  Figures in the session record, which is where a close figure lives
  (`build-session` step 14). **Docs-only, so the predicted delta is
  ZERO**; the record states the outcome.
- `clojure -M:poly check` green.
- **The oracle is untouched and unrun.** No `src`, no `test`, no
  resource, no oracle root, no digest source moved, so no root can have
  moved. Per `rulings.md#R-oracle-script-contract` an unrun oracle is
  **UNCLAIMED — not asserted-identical**: this ADR makes no
  regression-oracle claim and none is owed.
- `R-red-pushed-with-green` is **n/a**: this session is docs-only,
  plants no red and lands no enforcement test, so there is no red-first
  commit to pair.
- Audit (a) is scripted over all 72 rows, not sampled; its extraction
  reproduces the register's own 72 and its own per-section counts.
- `bin/post-push-verify` after the push, its three checks in the record.
- Reading sets re-measured at the close (`R-register-hygiene-at-close`),
  recorded in the session record.

### Deviations, this close's own

**The fence is widened by one file — `.agents/state.md` — and the
reason is a gate, not a preference.** The prompt's file list does not
name it. `ehrt.docs-tooling.state-staleness-tripwire-test` asserts
state.md's header cites the NEWEST arc-close ADR on disk, and this ADR's
own first heading says "arc close", so the moment this file existed the
tripwire's answer became ADR-0159 while state.md still cited ADR-0147.
Three paths were available and two are unavailable by ruling: leaving
the tree red is not a close; **renaming this ADR's heading to fall
outside the gate's population is forbidden outright** by
`rulings.md#R-never-dodge-a-gate-by-population`, which exists because
ADR-0139 faced this identical fork and named the dodge. That leaves one
defensible reading, so this is **fix-forward with disclosure** under
`rulings.md#R-stop-only-on-two-defensible-readings`, not a STOP — and
`rulings.md#R-state-regeneration` independently requires the
regeneration at every arc close regardless of what a prompt lists.

Post-ADR-0147 the obligation is bounded: state.md is the HAND-OWNED
half, with no `[V]` claims left to re-probe. What changed: the
regeneration header now cites ADR-0159 at `e967fd7`; the channel-errata
bullet carries review 4's own count; the population bullet is restated
as the CLASS both reviews found rather than review 3's wording alone;
the environment section gains ADR-0157's two edit-root checks,
`R-preflight-fail-closed`, `bin/ascii-scan` and `.githooks/commit-msg`.
The file was **119 lines before and 119 after** — four paragraphs were
compacted to pay for the additions, because `R-budget-stop` makes
raising the 120-line cap unavailable and D1-4 flagged the one line of
headroom as a tripwire for exactly this session.

**No other deviation.** No audit finding was fixed; F-1 through F-4 are
rowed. `rulings.md` is untouched, checked by grep before being asserted.

### Fences

Records-only, and held. Touched: this ADR; the register (**dated appends
under residue rows plus one header line and a close note — no review-day
row rewritten, no score edited**); the plan (Part 2's all-landed line and
one dated erratum); `.agents/plans/roadmap.md`; **`.agents/state.md`**
(the widening above); the session record and prompt archive; and the
three generated files that follow (`notes/ADRs.md`,
`.agents/state-derived.md`, both `INDEX.md`). **Zero `src`, zero `test`,
zero `bin`, zero skill edits, zero `rulings.md`.**

### Consequence

The review-4 arc closes with **every fix-session candidate and every
ruling-needed row moved** — 27 and 10, no exceptions — 38 rows in all,
10 rulings landed across three of the four fix sessions, and a ledger
that reconciles to zero in every session because the sessions wrote it
as they went instead of leaving it to the close.

What the close itself contributes is the part a tally cannot: **seven
citations that resolve only by substance, one prompt premise that is
false and still live, three carried figures that disagree with the
tree, and two registers whose own gates are narrower than the class
they are read as enforcing.** Review 3's thesis was *a probe whose
population is a registry rather than the tree*; review 4's was *a gate
whose population is narrower than the class it enforces*. This close
found two more instances of the second, in `roadmap.md` and
`rulings.md` — the two registers that hold the arcs themselves.

The instrument keeps working because the question keeps being asked
somewhere new. Review 5 inherits thirteen watch rows, a due point with
a number in it, and the same question: *how do I know this population
is all of them?*

### Roadmap row, verbatim before this close compacted it (ADR-0144's convention)

The `#repo-review-4` row carried the arc's ledger line by line while the
arc ran; the `## Done` row that replaces it states what remains and cites
this ADR for the rest. This is the rest, verbatim, at `e967fd7`:

    - OPEN **[repo-review-4]** PRIORITY 2 -- chartered roughly 15 ADRs past ADR-0139 by ADR count, not calendar (ruling Q3 "a.", 2026-08-15); inherits review 3's twelve-row watch-list and D8-5's survivor (56 of 74 command fences unexercised, ADR-0140).
      ASSESSMENT 2026-08-18 (ADR-0154): register `.agents/plans/2026-08-18-repo-review-findings.md`, plan `.agents/plans/2026-08-18-repo-review-4-plan.md`; 72 rows, 10 rulings owed, 8 fix sessions; author order G A E B C D F H, paired.
      FIX 1/5 (G+A) 2026-08-19 (ADR-0155), 12 rows: L3-1/L3-2/L3-4/L3-9/L3-10, L2-1..L2-6, L2-10.
      FIX 2/5 (E+C) 2026-08-19 (ADR-0156), 11 rows: L1-1..L1-5, D2-1/D2-2/D2-4/D2-6, D3-2, D4-4; R4-Q1/Q6/Q7/Q8 ruled.
      FIX 3/5 (B+D) 2026-08-19 (ADR-0157), 2 rows + #commit-msg-ascii-hook: D4-1 fixed, D3-1 PARTLY -- its gate landed and proved its own "verified safe" false; see #edit-root-worktree-residue.
      FIX 4/4 (F+H) 2026-08-19 (ADR-0158), 13 rows: D6-1/D7-3/D1-1/D8-1/D8-2(part)/D5-2/D7-5, L3-3/L3-5/L3-6/L3-7/L3-8/L3-11; R4-Q4/Q5/Q9 ruled. Arc close owed.
