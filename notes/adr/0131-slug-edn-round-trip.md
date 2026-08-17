## ADR-0131 — Slug EDN round-trip fix + module-load injectivity guard: census, declared-oracle-change prediction

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-14.

### Context

Chartered from `.agents/plans/roadmap.md`'s own "Slug EDN-round-trip
fix" row (ADR-0130, front-of-queue by sequencing, 2026-08-13 ruling).
`ehrt.sim-trajectory.gmf/slug` (`components/sim-trajectory/src/ehrt/
sim_trajectory/gmf.clj:45-55`) lower-cases a raw GMF name and folds
runs of underscores/whitespace to `-`, but sanitizes no other
punctuation; `keyword` then wraps the result verbatim. Upstream
Synthea state names are free text and can carry a comma
(`uti/abx_tx.json`'s own `"Cipro 500, 5 day"`) or other reader-
significant characters — `slug` violates this project's own informal
law, `(= k (edn/read-string (pr-str k)))`, emit composed with read is
identity. Witnessed live in ADR-0130: `bin/ehrt play out/scenarios/
busy-tuesday/events.edn --rate 100000` failed with `{:category
:play-input-unreadable, :payload {:message "Invalid number: -5-day"}}`.

This session executes the chartered fix under two author rulings
(verbatim, 2026-08-13, `.agents/rulings.md` "From ADR-0131"):

- **Q1 [A, "Q1 a."]:** sanitization = fold exactly the non-EDN-
  keyword-legal characters to `-`, collapse runs, trim edge hyphens.
  EDN legality defines the fold set; nothing more.
- **Q2 [A, "Q2 b."]:** the injectivity (collision) guard lands
  WARN-mode — a loud per-collision warning at module load, load
  proceeds; escalation to hard-error is chartered into a new rider
  row, triggered by that row's per-pair module corrections landing.
  Module JSONs are NOT edited this session (vendored verbatim, ADR-0071
  AR-VB2-R precedent).

### Step 0 — Ceremony and conditional tag

`bin/preflight`: last five CI runs on `main` all green, including the
three commits the driving prompt's own conditional tag license names
(`b3483dc0`, `06aec016`, `ef15885a`); edit-root ext4; tree clean; local
HEAD matched `origin/main` at `ef15885ae4cd1b35ee052843734c5dd902523a86`;
last `stable-*` tag `stable-20260813-strip-executability` at `3b30aba`,
HEAD not yet tagged. License satisfied: `bin/tag-ceremony
stable-20260813-busy-tuesday-deferral ef15885a... --push` — created
ANNOTATED at `ef15885`, pushed, peeled ref verified exact match.

**Oracle PRE-digest, all 35 roots** (direct `ehrt.oracle.digest/-main`
invocation against this checkout at HEAD `ef15885`, the baseline this
session's declaration measures against — the official `bin/
regression-oracle` bracket, HEAD vs the fix commit, is deferred to
Step 4, which is the actual required-by-ceremony use of that script):

```
9734d07a…87c07  allergic-rhinitis.edn      6ad02f82…721ed  ear-infections.edn
4c325dd5…86196  anemia.edn                 4e14b2e6…8c1ba  fibromyalgia.edn
89bc2090…0686c2d appendicitis.edn          b9891660…4a1c95 hypothyroidism.edn
e764d000…44e535844c67 asthma.edn           50c0f458…423eb0 injuries.edn
9b0b9511…6531b  attention-deficit-disorder.edn c8d5e47a…8712a med-rec.edn
17cc1541…340480 bronchitis.edn             9a129916…6ffda  metabolic-syndrome-care.edn
85f57ba3…326ed  colorectal.edn             eb07b1c7…9ef5   osteoarthritis.edn
28087e14…fbf8602 death-fixture.edn         129117f9…675399 osteoporosis.edn
35dc0ae1…3b25d0 dementia.edn               6d403a96…18ff8  rheumatoid-arthritis.edn
36162969…ceaf0  dermatitis.edn             f0b8160d…af74b3 sepsis.edn
5a631475…742303 ear-infections-engine.edn  e9931b60…885531 sinusitis.edn
37885c66…6b34af4 ear-infections-history-engine.edn 271df527…b147d5f sleep-apnea.edn
                                            b451881e…0daa9  sore-throat.edn
818bff1c…70cb103 total-joint-replacement-engine.edn
97bece7c…572e04b0 urinary-tract-infections-engine.edn
ecc49eb4…534d3  urinary-tract-infections-history-engine.edn
2097308e…c0d2fe veteran-lung-cancer.edn
eb633864…ba6ef7 veteran-prostate-cancer.edn
50405114…d136c3 veteran-ptsd.edn
ea2f635d…82955c veteran-self-harm.edn
bda25035…9b5fe  veteran-substance-abuse-treatment.edn
e222bfd0…afefd  vhd-pulmonic.edn
4441c499…33ef2  vhd-tricuspid.edn
```

(35 files, full 64-hex digests in this session's own scratch manifest;
truncated here for table width — the point is a recorded baseline
exists, not the hex itself, which Step 4's official bracket re-derives
independently.)

### Step 1 — Census, re-derived against the live tree

Both defect classes re-derived from scratch across all 66 module JSONs
(`find components/sim/resources/sim/modules -name "*.json"`, recursive
— the flat `components/sim/resources/sim/modules/*.json` glob alone
misses 35 of the 66, which live one level down in 12 subdirectories:
`heart/`, `uti/`, `dermatitis/`, `medications/`, `anemia/`, `dme/`,
`total_joint_replacement/`, `metabolic_syndrome/`, `snf/`, `injuries/`,
`veterans/`). Every JSON object key at every depth was walked (not
only the top-level `states` map), since `kebab-key` applies uniformly
at every depth during parse.

**The fold-set boundary, verified empirically, not assumed.** Q1(a)
defines the fold set as "non-EDN-keyword-legal characters." Rather
than hand-derive this from memory of the reader grammar, every
printable ASCII character 33–126 plus the six whitespace characters
was round-trip-tested directly against `clojure.edn/read-string`:
build `(keyword (str "x" c "y"))`, check `(= k (edn/read-string
(pr-str k)))`. The illegal set that failed round-trip:

```
" ( ) , ; @ [ \ ] ^ ` { } ~
```

— comma and the thirteen characters Clojure's reader treats as
terminating macros — plus the six whitespace characters (space, tab,
newline, CR, FF, VT), which the pre-existing `\s` fold already
handles. Confirmed **legal** (no fold needed, matching the driving
prompt's own pre-probe claim): `? ' & % # $ = < > * + ! . -` all
round-tripped clean. This is the fold set Step 3's fix implements.

#### Defect 1 — breaker keys (illegal chars, pre-fix EDN-unreadable)

| Module | Raw key | Chars |
|---|---|---|
| `uti/abx_tx.json` | `AmxClav 500, 5 day` | comma |
| `uti/abx_tx.json` | `AmxClav 875, 7 day` | comma |
| `uti/abx_tx.json` | `AmxClav 875, 10 day` | comma |
| `uti/abx_tx.json` | `Cipro 250, 3 day` | comma |
| `uti/abx_tx.json` | `Cipro 500, 5 day` | comma |
| `injuries/broken_jaw.json` | `Posttreatment stabilization, orthodontic device` | comma |
| `veteran_lung_cancer.json` | `Sputum Cytology (Phelgm)` | parens |
| `veteran_lung_cancer.json` | `Thoracentesis (Fluid)` | parens |
| `veteran_lung_cancer.json` | `Needle Biopsy (Cells)` | parens |
| `veteran_lung_cancer.json` | `Bronchoscopy (Tube)` | parens |

**10 breaker keys, 3 modules — exact match to the driving prompt's own
pre-probe** (10 specimens, 3 modules, the same three files and comma/
parens breakdown).

#### Defect 2 — collisions under the (unchanged by this fix) `_`/whitespace fold

| Module | Slug | Colliding raw keys |
|---|---|---|
| `colorectal_cancer.json` | `postoperative-care` | `Postoperative Care` / `Postoperative_Care` |
| `hypothyroidism.json` | `hypothyroidism` | `Hypothyroidism` / `hypothyroidism` |
| `injuries.json` | `end-dme` | `End DME` / `End_DME` |
| `injuries.json` | `postoperative-care` | `Postoperative Care` / `Postoperative_Care` |
| `sleep_apnea.json` | `2nd-assessment` | `2nd Assessment` / `2nd_Assessment` |
| `sleep_apnea.json` | `intraoral-appliance` | `Intraoral Appliance` / `Intraoral_Appliance` |
| `sleep_apnea.json` | `home-cpap-unit` | `Home CPAP Unit` / `Home_CPAP_Unit` |
| `sleep_apnea.json` | `nasal-mask-supplies` | `Nasal Mask Supplies` / `Nasal_Mask_Supplies` |
| `veteran_ptsd.json` | `columbia-suicide-risk-assessment` | `Columbia Suicide Risk Assessment` / `Columbia_Suicide_Risk_Assessment` |
| `veteran_ptsd.json` | `phq2-q9-assessment` | `PHQ2_Q9 Assessment` / `PHQ2_Q9_Assessment` |

**10 collision pairs across 5 DISTINCT module files** (`colorectal_
cancer.json`, `hypothyroidism.json`, `injuries.json`, `sleep_apnea.
json`, `veteran_ptsd.json`) — computed both under the CURRENT
(pre-fix) slug and under the POST-fix slug (Q1(a)'s widened fold set);
the two collision sets are **IDENTICAL** — the fix neither introduces
a new collision nor resolves an existing one, since every colliding
pair here differs only by case/underscore/whitespace, none of which
Q1(a)'s new fold characters touch.

**Discrepancy found and disclosed, per this session's own re-derive
mandate:** the driving prompt's own channel pre-probe states "10
pairs, 8 modules." The pair count (10) and per-module pair breakdown
(hypothyroidism ×1, veteran_ptsd ×2, colorectal_cancer ×1, sleep_apnea
×4, injuries ×2 = 10) both match exactly. The **module count does
not** — 5 distinct files carry these 10 pairs, not 8. Re-checked twice
(a direct Python walk of all 66 files' `states` keys, and a
cross-check restricting to just the 5 named files) — both agree on 5.
No fourth or additional collision-bearing module was found anywhere
in the 66-file tree. This is a real, disclosed pre-probe error, not a
live-tree finding requiring any fix — flagged per this session's own
verification discipline, not silently repeated.

### Oracle root movement prediction (declared-oracle-change declaration)

Every one of the 35 roots' own module set was resolved from the live
tree (`ehrt.sim-trajectory.interface/load-closure`, direct invocation
— not read off a prior artifact) and cross-checked against the two
census tables above.

**3 defect-1 breaker files exist; 4 roots structurally include one:**

| Root | Breaker module in closure | Empirically reached at this root's own seed/population? |
|---|---|---|
| `urinary-tract-infections-engine` | `uti/abx_tx.json` (seed 777, 300p) | **YES** — pre-fix digest already contains `:amxclav-500,-5-day` etc. (grepped directly) |
| `urinary-tract-infections-history-engine` | `uti/abx_tx.json` (same closure, seed 20260802, 300p, `:history true`) | **YES** — same 5 broken keywords present |
| `injuries` | `injuries/broken_jaw.json` (seed 20260802, 300p) | **YES** — `:posttreatment-stabilization,-orthodontic-device` present |
| `veteran-lung-cancer` | `veteran_lung_cancer.json` itself (singleton, seed 20260802, 300p) | **NO** — grepped the pre-fix digest for all 4 breaker state names (`sputum`, `thoracentesis`, `biopsy`, `bronchoscopy`) case-insensitively: zero occurrences. These 4 states ARE reachable in the module graph (each referenced twice — once as its own key, once as a transition target — confirmed by direct JSON inspection) but this root's own fixed seed/population never walks a patient into them. |

**Declared prediction, refining the naive "closure includes a breaker
→ moves" rule with this empirical check** (the naive rule alone would
wrongly predict `veteran-lung-cancer` moves; the digest only captures
a root's own RETURNED trajectory/HL7 content, never the whole compiled
module, so an unreached state's own corrected punctuation cannot
appear in the digested bytes regardless of what the module compiles
to):

- **MOVE (3 roots):** `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`, `injuries`.
- **NO MOVE, despite containing a breaker module (1 root):**
  `veteran-lung-cancer` — empirically dark branches at this seed.
- **NO MOVE (31 roots):** everything else, including the 5 whose
  closures carry a defect-2 collision module (below) — collision
  detection is a WARN-mode side effect (console output), never part of
  a producer function's own RETURNED value that `digest.clj`'s `-main`
  `pr-str`s and hashes; `ehrt.oracle.digest/-main` (`components/
  oracle/src/ehrt/oracle/digest.clj:589-591`) only ever captures `(f)`'s
  return value, confirmed by direct read — no stdout/stderr capture
  exists in this path for a warning to land in.

**Roots that will emit a collision WARNING at load (no byte
movement predicted):** `sleep-apnea` (4 pairs), `hypothyroidism` (1
pair, in its own closure member `hypothyroidism.json`), `colorectal`
(1 pair, in its own closure member `colorectal_cancer.json`),
`veteran-ptsd` (2 pairs), `injuries` (2 pairs, IN ADDITION to moving
on the `broken_jaw.json` breaker fix — the only root in both lists).

**8 distinct roots touched by either defect** (3 movers + 1
empirically-dark-breaker + 4 warn-only, `injuries` double-counted as
mover+warner collapses to this total): `urinary-tract-infections-
engine`, `urinary-tract-infections-history-engine`, `injuries`,
`veteran-lung-cancer`, `sleep-apnea`, `hypothyroidism`, `colorectal`,
`veteran-ptsd`. The remaining 27 roots are untouched by either census
and are predicted pure-identity.

This declaration is recorded BEFORE any `src` edit, per the driving
prompt's own MANDATORY declared-oracle-change requirement. Step 4
re-runs the official `bin/regression-oracle` bracket and the prediction
above must match exactly, or this session STOPs.

### Step 2 — Red witness

Both new tests witnessed RED against genuinely pre-fix code (no fix
had yet been applied — no stash isolation needed, the natural
sequencing already isolates it): the collision-guard tests failed on
empty-string warnings (`(not (str/includes? "" "sleep-apnea"))` etc.,
the guard did not exist yet); the round-trip property ERRORED on its
first shrunk failing case (`"a,"`, `RuntimeException: Invalid
constituent character: ~` surfacing during shrinking — the OLD `slug`
lets reader-illegal characters straight through, so `edn/read-string`
itself throws, not just returns a mismatched value) and the two
concrete comma/parens specimen tests failed on the exact un-folded
strings (`"cipro-500,-5-day"` vs the expected `"cipro-500-5-day"`).
`slug-is-idempotent` passed even pre-fix (the OLD fold set is
trivially a fixed point of itself, independent of whether commas are
handled — not a defect-1 witness, but a real invariant kept green
across the fix). 10 failures + 1 error, out of 220 assertions.

### Step 3 — The fix

`slug` (Q1(a)): folds comma plus the reader's own thirteen terminating-
macro characters, alongside the pre-existing `_`/whitespace fold;
collapses runs; trims edges. The module-load injectivity guard
(Q2(b), WARN-mode): a second, string-keyed parse of `json-text`'s own
top-level `"states"` object (before `kebab-key` has silently folded
any collision), grouped by post-slug key, one `*err*` warning per
collision group naming the module id, the folded key, and every raw
name that produced it; `load-module` calls this as a side effect
before parsing, never touching its own return value — the escalation
a future rider session charters is a mode switch at
`handle-state-name-collision!`'s one call site, not a rewrite.

Both new tests green (75 tests, 220 assertions, 0 failures/errors).
Full `make test`: green (632 "0 failures, 0 errors" blocks, matching
this session's own pre-fix baseline count — no OTHER test moved,
no count-lock tripped). `clojure -M:poly check`: OK.

### Step 4 — Oracle verdict: prediction vs actual

`bin/regression-oracle ef15885a e1a9b9a5` (HEAD at Step 0 vs the fix
commit): soundness check IDENTICAL outside `digest.clj`'s own `(ns
...)` form; result **DIFFERS** — EXPECTED, a declared change, per the
ADR-0071/ADR-0086 precedent (a `DIFFERS` exit is this script's own
literal semantics for any digest delta, declared or not; the
declaration is what makes it expected rather than a STOP).

| Root | Predicted | Actual | Match? |
|---|---|---|---|
| `urinary-tract-infections-engine` | MOVE | MOVED (`97bece7c…` → `2c3203c9…`) | ✅ |
| `urinary-tract-infections-history-engine` | MOVE | MOVED (`ecc49eb4…` → `bbd33893…`) | ✅ |
| `injuries` | MOVE | MOVED (`50c0f458…` → `2cbb97ee…`) | ✅ |
| `veteran-lung-cancer` | NO MOVE (breaker unreached) | byte-identical (`2097308e…`) | ✅ |
| `sleep-apnea` | WARN, NO MOVE | 4 warnings emitted (`nasal-mask-supplies`/`2nd-assessment`/`home-cpap-unit`/`intraoral-appliance`), byte-identical | ✅ |
| `hypothyroidism` | WARN, NO MOVE | 1 warning (`hypothyroidism`), byte-identical | ✅ |
| `colorectal` | WARN, NO MOVE | 1 warning (`postoperative-care`), byte-identical | ✅ |
| `veteran-ptsd` | WARN, NO MOVE | 2 warnings (`phq2-q9-assessment`/`columbia-suicide-risk-assessment`), byte-identical | ✅ |
| all other 27 roots | NO MOVE, no warning | byte-identical, silent | ✅ |

**Every prediction matched exactly** — 3 roots moved (and only those
3), `veteran-lung-cancer`'s empirically-refined no-move prediction
held, every one of the 10 census-predicted collision warnings fired
with the exact predicted module/key/raw-name content, and the
remaining 27 roots stayed silently byte-identical. No STOP condition.

**Re-baseline, per the declared-change ceremony's own mechanism**
(there is no persisted baseline artifact this repo commits — `bin/
regression-oracle` always diffs two live git refs — so "re-baseline"
is this record itself): `injuries`, `urinary-tract-infections-engine`,
and `urinary-tract-infections-history-engine`'s own post-fix digests
(`e1a9b9a5` and onward) are the licensed new baseline, the SAME
"more correct, not merely different" framing ADR-0086 used for its own
one-root mover — the emit-composed-with-read law these three roots'
own compiled ground truth now actually satisfies is the correctness
gain, not an incidental side effect.

### Acceptance — busy-tuesday regenerated, the previously-failing command witnessed green

`bin/ehrt corpus generate sim --seed 20260807 --patients 200 --config
demos/scenarios/busy-tuesday/config.edn --out-dir out/scenarios/
busy-tuesday`: `{:status :ok, :payload {:out-dir "out/scenarios/
busy-tuesday"}}` (4 collision warnings from `sleep_apnea.json`, part
of this scenario's own twelve-module mix, printed to `*err*` — expected,
disclosed, no run-blocking effect).

`bin/ehrt play out/scenarios/busy-tuesday --board 60 --rate 100000`
(README's own second command) — closing summary: `{:status :ok,
:payload {:unparseable-count 0, :snapshot-count 48, :skip-count 41,
:emitted 68, :unfolded-count 0, :clamped-count 0, :sink "ticker"}}`;
`inpatients: 0` on every one of the 48 board snapshots (grepped
directly, only value seen). **Byte-for-byte the SAME witnessed figures
ADR-0130 recorded (`68/48/41`, `inpatients: 0` throughout)** — no
README figure moved.

`bin/ehrt play out/scenarios/busy-tuesday/events.edn --rate 100000`
(README's own third command — the ONE that failed in ADR-0130 with
`{:category :play-input-unreadable, :payload {:message "Invalid
number: -5-day"}}`): now completes — `{:status :ok, :payload
{:unparseable-count 0, :skip-count 49, :rate 100000.0, :emitted 367,
:sink "ticker"}}`. This is the FIRST time this exact command has ever
completed for this scenario (no prior witnessed figure exists to
compare against, since it always failed before this fix) — its own
`367`/`49` figures are a new, first-witnessed baseline, not a
regression against anything.

No README edit, no figure edit — every existing witnessed figure held.
`out/scenarios/busy-tuesday` (gitignored) left in place per the
standing convention; tree clean per `git status --porcelain`.

### Fences

Committed this step: this ADR file, `notes/ADRs.md` index line,
`.agents/plans/roadmap.md`'s slug row (prediction recorded in place).
Zero `src`/`test` touched — docs-only, per the driving prompt's own
Step 1 charter.

Committed Step 3: `components/sim-trajectory/{src,test}/ehrt/sim_
trajectory/{gmf.clj,gmf_test.clj}` only.

Committed Step 4 (this section): this ADR file only (evidence/record);
zero `src`/`test`/module JSON touched — the oracle bracket and
acceptance run are read-only verification acts, not fixes.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Slug EDN round-trip fix + module-load injectivity guard: census, declared-oracle-change prediction (Step 1 of 5) — pays tag `stable-20260813-busy-tuesday-deferral` at `ef15885` (ADR-0130's own close, `bin/preflight` all-green on the three named commits, conditional license satisfied); re-derives both defect censuses across all 66 module JSONs recursively (the flat top-level glob alone misses 35 of the 66, one level down in 12 subdirectories) -- defect 1 (illegal EDN chars, empirically fold-set-verified against `clojure.edn/read-string` round-trip): 10 breaker keys, 3 modules (`uti/abx_tx.json` x5 comma, `injuries/broken_jaw.json` x1 comma, `veteran_lung_cancer.json` x4 parens), EXACT match to the driving prompt's own pre-probe; defect 2 (collisions under the unchanged `_`/whitespace fold, identical pre-fix and post-fix): 10 pairs across **5** distinct module files (`colorectal_cancer.json`, `hypothyroidism.json`, `injuries.json`, `sleep_apnea.json`, `veteran_ptsd.json`) -- pair count and per-module breakdown match the pre-probe exactly, but the pre-probe's own "8 modules" figure is WRONG (actual 5, double-checked two ways), disclosed as a found pre-probe discrepancy, not a live-tree finding. Resolves every one of the 35 oracle roots' own module closure from the live tree (`load-closure`, direct invocation) and predicts movement empirically, not just structurally: 3 roots MOVE (`urinary-tract-infections-engine`/`-history-engine` via `uti/abx_tx.json`, `injuries` via `injuries/broken_jaw.json` -- all three grep-confirmed against the pre-fix oracle digest to already contain the broken comma-keywords); 1 root (`veteran-lung-cancer`) structurally contains a breaker module but its 4 breaker states are grep-confirmed UNREACHED at that root's own seed/population (zero occurrences in the pre-fix digest) -- predicted NOT to move, refining the naive closure-inclusion rule; 4 more roots (`sleep-apnea`, `hypothyroidism`, `colorectal`, `veteran-ptsd`) plus `injuries` again will emit a collision WARNING at load but are predicted NOT to move, since `digest.clj`'s own `-main` only ever captures a producer function's RETURNED value, never stdout/stderr (confirmed by direct read of `components/oracle/src/ehrt/oracle/digest.clj:589-591`) -- 27 of 35 roots predicted pure-identity, untouched by either census. Oracle PRE-digest, all 35 roots, recorded as this declaration's own baseline. Declared BEFORE any `src` edit, per the driving prompt's own mandatory declared-oracle-change requirement; Step 4 (a future commit) re-runs the official `bin/regression-oracle` bracket and this prediction must match exactly. Docs-only -- zero `src`/`test` touched
