## ADR-0072 — Vendoring batch 3: the families that travel — and the verbatim law gets teeth

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: vendoring batch 2 landed and was design-channel-verified
(`notes/adr/0071-vendoring-batch-2.md`, tip `96424f8`) — seven
vendored, `anemia___unknown_etiology.json` deferred whole on a real
dangling-`:encounter-end` `gmf-interpreter` gap. This session is the
vendoring arc's fourth. Its own driving prompt (design channel,
2026-08-07) named a real, pre-arc defect the batch-2 verification's own
full-table NOTICE re-hash surfaced: `lookup_tables/uti_recurrence.csv`
has never actually been byte-verbatim on disk in a fresh checkout —
upstream at the pin is CRLF (367 bytes, `baf597d2...`, exactly what its
NOTICE row has recorded since 2026-08-02), but the repo's own root
`.gitattributes` rule (`* text=auto eol=lf`) silently normalized the
COMMITTED blob to LF (357 bytes, `b83c2960...`) — the NOTICE row stayed
honest about upstream while a fresh clone's own checked-out bytes
diverged from it, undetected by every prior "re-verified"/"zero
problems" claim (ADR-0071's own included), none of which had ever
mechanically re-hashed the disk bytes against the table. The author's
own ruling on the fix ("do what you think will work, but remember git
may rewrite line endings," design channel 2026-08-07) is recorded
verbatim under AR-VB3-R1, below.

Read-first: `.gitattributes` in full; `components/sim/resources/sim/
modules/NOTICE` (the uti_recurrence row, the batch-2 section as model);
`components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` ~1504–1540
(the parser's BOM-strip docstring and its `split-lines`, which splits
`\r\n`/`\n` alike); ADR-0070/0071 (batch mechanics, the anemia gap); the
substance artifact rows for the batch-3 candidates
(`components/sim-trajectory/docs/census/2026-08-07-synthea-7e08387-
substance.edn`); `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/
vendored_hypothyroidism_test.clj` and siblings (the closure-bearing test
pattern).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-VB3-0 `[A — tag law, case (ii); debt recorded in ADR-0071]`.**
Annotated `stable-20260807-vendoring-batch-2` at `96424f8`, message
"vendoring batch 2 landed, design-channel-verified 2026-08-07
(ADR-0071)"; pushed; peeled ref verified — resolves exactly to
`96424f8`.

**AR-VB3-R1 `[A for the fix mandate and the line-ending caution; C for
the shape]` (the verbatim law gets teeth).** Three parts, one step:
(i) `.gitattributes` gains `components/sim/resources/sim/modules/**
-text`, the same fix already used for this repo's HL7 ER7 fixtures —
the vendored-modules tree is byte-verbatim BY LAW (NOTICE's own "no
reformatting" clause), so line-ending normalization has no business
anywhere in it. (ii) `uti_recurrence.csv` re-vendored byte-exact from
the pin (CRLF, 367 bytes) — its EXISTING NOTICE row (which always
recorded the upstream hash) becomes true WITHOUT being edited. (iii)
The co-landed gate: `ehrt.docs-tooling.notice-verbatim-test`
(`notice_verbatim_test.clj`), a docs-tooling test that parses every
NOTICE file's own provenance table and re-hashes each named file's
on-disk bytes against the row's recorded SHA-256 — the exact sweep that
caught this, promoted from a design-channel probe to a standing gate.
NATURAL RED witnessed against the pre-fix tree (see Execution record);
green after (ii). Uti round-trip tests and a full oracle re-digest both
showed zero movement, exactly as the `split-lines` mechanism predicted.

**AR-VB3-1 `[C — composition refinement, DISCLOSED deviation from the
concurred plan]` (what batch 3 actually vendors).** The concurred plan
(`notes/adr/0070-vendoring-batch-1.md`) named "the metabolic-syndrome
pair, the vhd quartet, colorectal-cancer, med-rec." The round-trip gate
(nonzero content, witnessed) makes zero-substance modules unvendorable
by this arc's own standard, and one-module-per-patient assignment
(Wave G's own standing deferral) gives "the pair/quartet travels
together" no runtime meaning today. Batch 3 therefore targeted the FIVE
content-producers (census substance artifact, ids/files/counts fresh
from the artifact, never guessed):

| id | file | `:closure-file-count` | `:event-counts` |
|---|---|---|---|
| metabolic-syndrome-care | `metabolic_syndrome_care.json` | 6 | [139 139 139] |
| vhd-pulmonic | `vhd_pulmonic.json` | 2 | [3 3 3] |
| vhd-tricuspid | `vhd_tricuspid.json` | 2 | [3 3 3] |
| colorectal-cancer | `colorectal_cancer.json` | 2 | [0 34 0] |
| med-rec | `med_rec.json` | 1 | [269 273 275] |

Of these five, **four landed**; `colorectal_cancer.json` was assessed
and DEFERRED WHOLE (below) — a further, session-live deviation from the
plan, disclosed, not silently absorbed. The three zero-substance family
siblings (`metabolic_syndrome_disease.json`, `vhd_aortic.json`,
`vhd_mitral.json` — all `:zero-on-every-seed` in the census substance
artifact) are RECORDED as not-vendorable-under-the-gate, joining the
attribute-blocked set — revisit trigger: multi-module patient
assignment (Wave G) giving family pairing a runtime meaning, or the
census register's own future word on either family.

**AR-VB3-2 `[C — batch mechanics, third repetition]` (vendor + test +
roots).** Identical discipline to ADR-0070/0071: fresh closure
enumeration from the pin-verified checkout, byte-verbatim copy, SHA-256
rows under a dated batch-3 NOTICE section, per-module red-then-green
round trip at the batch convention (seed 20260802, 300 patients, 36500
horizon-days, no deviation needed this batch), whole-module bail-out
on a real gap, each landed module a FIRST-BASELINE oracle root,
additive. See Execution record.

**AR-VB3-3 `[A — the author's next direction, recorded verbatim as
intake, not acted]` (the practitioner-UX horizon).** The author ruled
(design channel, 2026-08-07): "let's work more on the UX for
practitioners, particularly the demos. I want to move the sim demos to
a top-level demo place, and feature them in the intro materials."
Recorded in `.agents/plans/roadmap.md`'s own "Next" section as the
post-batch-3 direction — a design pass in the design channel first (the
demos/scenarios homes, their relation to the top-level README and
SETUP, the fence-gate implications of moving gated docs). NOTHING moves
this session.

**AR-VB3-4 `[C — scope]` (fences).** Held exactly: no module-content
edits; no loader/interpreter/engine/emitter changes (the bail-out
precedent fired for `colorectal_cancer.json`, below); no batch-4
modules, no wellness-encounters; no demo/scenario relocation (AR-VB3-3
is intake only); the `.gitattributes` edit touches ONLY the new
vendored-modules rule; standing untracked files untouched.

### Expected-count disclosure (AR-VB3-1/2)

Fresh closure enumeration against the pin checkout (never read off the
census artifact's own `:closure-file-count`, per ADR-0070's own
AR-VB1-2 lesson): THIRTEEN new files land across the four landed
modules (a fourteenth, `colorectal_cancer.json` itself, was copied to
disk then removed again on the bail-out, below — never landed, no
NOTICE row).

| Module | `:closure-file-count` | Fresh enumeration | Actual NEW files landed |
|---|---|---|---|
| metabolic-syndrome-care | 6 | root + 4 `metabolic_syndrome/` submodules + shared `anemia_sub` (6 JSON, 0 CSV) | **5** (anemia_sub already vendored, reused) |
| vhd-pulmonic | 2 (UNDERCOUNT — JSON-only) | root + `heart/vhd_risks.json` + 2 CSVs (4 files) | **4** |
| vhd-tricuspid | 2 (UNDERCOUNT — JSON-only) | root + shared `heart/vhd_risks.json` + 2 CSVs (4 files) | **3** (`vhd_risks.json` already landed via vhd-pulmonic, reused) |
| med-rec | 1 | root only | **1** |
| colorectal-cancer | 2 | root + shared `anemia_sub` | **0** (deferred whole; `anemia_sub` already vendored) |
| **Total** | **13** | | **13** |

The `vhd-pulmonic`/`vhd-tricuspid` undercount repeats `asthma.json`'s
own batch-1 finding exactly (JSON-only metric, CSVs invisible to it) —
disclosed, not a new class of surprise. Every one of the fourteen files
copied to disk this session (the fourteenth being
`colorectal_cancer.json` itself, copied then removed again on the
bail-out) was confirmed LF upstream — none of this batch's own new
files are CRLF at the source, unlike `uti_recurrence.csv`; the new
`-text` rule protects them regardless, going forward.

### Execution record

**Step 0 (preflight + tag).** Cwd confirmed the ext4 clone, tip
`96424f8`, working tree clean. `clojure -M:poly check` OK; full suite
green (`clojure -M:poly test`, every project 0 failures/0 errors, 314
assertions in the last-run project block); oracle pre-digest (direct
`ehrt.oracle.digest/-main` invocation via `:dev:test`, all
twenty-three existing roots) recorded to a scratch manifest. AR-VB3-0
executed directly: `stable-20260807-vendoring-batch-2` created
annotated at `96424f8`, pushed, verified — peeled ref resolves exactly.

**Step 1 (`57ec4b7`, AR-VB3-R1).** The design-channel probe's finding
re-derived and witnessed live: the working tree's own on-disk
`uti_recurrence.csv` (367 bytes, `baf597d2...`, matching upstream and
the NOTICE row) was NOT what the committed blob held (`git show
HEAD:...` → 357 bytes, `b83c2960...`) — `git status` reported clean
throughout because `text=auto`'s own CRLF-stripped comparison made the
two look identical despite differing raw bytes. A genuine fresh
materialization (`git worktree add`, and independently, `rm` +
`git checkout-index -f`) confirmed a real checkout gets the WRONG
357-byte bytes. `notice_verbatim_test.clj` written; run against that
genuinely-renormalized state: **RED**, one failure, the exact mismatch
predicted (`baf597d2...` expected, `b83c2960...` actual). Fix applied:
`.gitattributes` gained the `-text` rule; the correct 367-byte bytes
restored to the working tree and staged (`git add` under the new
attribute stores raw bytes, no filter). Re-materialization proof
repeated (`rm` + `git checkout-index -f` against the NEW staged blob):
CRLF bytes now survive a genuine fresh checkout. `notice_verbatim_test.
clj` re-run: **GREEN**, 4 tests, 115 assertions, 0 failures. UTI
round-trip tests (`ehrt.sim-emit-hl7.vendored-uti-test`,
`ehrt.sim-trajectory.vendored-uti-test`) green, 7 tests, 15 assertions.
Full oracle re-digest (all 23 roots) diffed byte-for-byte against the
Step 0 baseline: **zero movement**, exactly as `gmf.clj`'s own
`split-lines` mechanism predicted (it splits `\r\n`/`\n` alike, so
restoring the true CRLF bytes parses identically). `clojure -M:poly
check` OK; full suite green (73 passes, 0/0). `gitleaks git --staged
-v`: clean. Post-push verification: one delta, the known
trailing-newline artifact.

**Step 2 (`5d40d4e`, AR-VB3-1/2).** Fresh closure enumeration (above)
against the pin checkout (`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean, hash
re-verified). Byte-verbatim copy, `metabolic_syndrome_care.json`'s own
closure landed clean first try. `vhd_pulmonic.json`/`vhd_tricuspid.
json` landed clean, their own real content turning out to be ENTIRELY
`:outpatient-visit`/`:outpatient-visit-end` pairs at 300 patients (no
`:condition-onset`/`:medication-order` observed) — consistent with the
census's own tiny `[3 3 3]` event-counts, disclosed in both tests'
assertions rather than silently narrowed to match the broader kind set
other vendored roots' tests use. `med_rec.json` landed clean, single
file. `colorectal_cancer.json` FAILED its own round-trip test on first
run: `:discharge-follows-admission`/`:clinical-content-only-when-
admitted` violations, traced to the SAME shared `anemia/anemia_sub.
json` submodule's own unconditional `:encounter-end` idiom
`anemia___unknown_etiology.json` already surfaced in batch 2 — this
module's own call path into it sometimes lands outside an open
encounter (2 of 3 seeds tried rejected at 300 patients: 20260802 and 42
each 4 violations, seed 1 clean). Per AR-VB3-4's own fence (no
interpreter changes), `colorectal_cancer.json` DEFERRED WHOLE — its
copied bytes and its own test file removed before staging; no NOTICE
row, no test, no oracle root. NOTICE gained thirteen new rows plus a
dated batch-3 section (the four-landed/one-deferred narrative and the
full `colorectal_cancer.json` finding) plus the `colorectal_cancer.
json` deferred-whole paragraph; every hash cross-checked by fresh
`sha256sum` against the table before commit.
`ehrt.docs-tooling.notice-verbatim-test` re-run against the updated
NOTICE: green, 4 tests, 141 assertions (69 total provenance rows × 2
assertions + 3 mechanism-sanity tests). `clojure -M:poly check` OK;
full suite green (0 failures/0 errors throughout, confirmed by grepping
the entire run's own output, not just the last project's tail).
`gitleaks git --staged -v`: clean. Post-push verification: one delta,
the known trailing-newline artifact.

**Step 3 (`fc44369`, AR-VB3-2).** `digest.clj` gained four new producer
functions (`metabolic-syndrome-care-pair`/`vhd-pulmonic-pair`/
`vhd-tricuspid-pair`/`med-rec-pair`) and four new `roots` map entries —
purely additive, every existing producer function and root entry
byte-unchanged. Direct `ehrt.oracle.digest/-main` invocation against
the edited tree wrote 27 `.edn` files (23 pre-existing + 4 new);
diffed byte-for-byte against the Step-1 post-fix baseline: the 23
pre-existing roots identical, exactly 4 new files. The official
standing harness, `bin/regression-oracle 5d40d4e fc44369
--declared-digest-change`, reported `DIFFERS` (exit 1) — EXPECTED, per
the ADR-0070/0071 precedent: the diff shows exactly four ADDED lines
(`med-rec.edn`/`metabolic-syndrome-care.edn`/`vhd-pulmonic.edn`/
`vhd-tricuspid.edn`) and ZERO removed or changed lines among the
twenty-three pre-existing roots. `clojure -M:poly check` OK. Post-push
verification: one delta, the known trailing-newline artifact.

**Step 4 (this record).** `notes/adr/0072-vendoring-batch-3.md`
authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own stale file count corrected (69→70, verified
by `ls`); roadmap's "Now" section updated to this session's own close
(successor-tag debt, AR-VB3-3's own direction summarized), the
`EncounterEnd` Deferred row gained a dated note naming
`colorectal_cancer.json` as a second blocked module, a new "Next"
backlog entry records AR-VB3-3 verbatim, and the Done pointer
(`- 2026-08-07 — vendoring-batch-3 — ADR-0072`) landed in the same
commit as the index line; session record and prompt archive land in
the same commit.

This session's own successor tag debt: `stable-20260807-vendoring-
batch-3` at this session's own closing tip is owed to the next
session's own Step 0, per tag law (ADR-0057 AR-T-1) — not created here
(no ruling licensed it at this session's own closing commit).

### ADR-0071 correction (dated, append-don't-erase)

ADR-0071's own Execution record claims "every hash cross-checked by
fresh `sha256sum` against the table before commit, and again authoring
this record (56 rows total, zero problems)." That claim was TRUE of
every hash it actually checked, but it never checked whether the
COMMITTED blob for `uti_recurrence.csv` (landed 2026-08-02, before
ADR-0071's own session) still matched its own on-disk working-tree
bytes at the git-object level — a re-hash of the working tree is not a
re-hash of what a fresh clone actually receives, and this session found
they had silently diverged (see AR-VB3-R1, above). ADR-0071's own "zero
problems" is corrected here, dated 2026-08-07: one problem existed,
undetected by that session's own (and every prior session's own)
verification method, closed by this session's `notice_verbatim_test.
clj` gate. Not erased — appended, per this project's own errata
discipline.

### `colorectal_cancer.json`: assessed, DEFERRED WHOLE

Full finding recorded in `components/sim/resources/sim/modules/
NOTICE`'s own dated section, summarized here. This module calls the
SAME shared `anemia/anemia_sub.json` submodule `hypothyroidism.json`
already calls cleanly (batch 2) — but `colorectal_cancer.json`'s own
call into `Anemia_Submodule` does not always land inside an open
encounter, so `anemia_sub.json`'s own `End Any Active Encounter Just In
Case` state (the SAME standing `ehrt.sim-trajectory.gmf-interpreter`
gap `anemia___unknown_etiology.json` already surfaced) sometimes fires
with no encounter open. Confirmed this session at 300 patients across
three seeds (20260802, 1, 42): 2 of 3 rejected (4 violations each), one
clean — not universal every seed the way the first finding was, but a
real, non-negligible rate at population scale, the SAME root cause
already disclosed and understood, not a new gap. Per AR-VB3-4's own
fence, `colorectal_cancer.json` is NOT vendored — no NOTICE row, no
test, no oracle root; its own closure contributes ZERO new files
regardless (`anemia/anemia_sub.json` was already vendored via
`hypothyroidism.json`'s own closure and stays exactly as it is,
untouched). Revisit trigger: the SAME one `anemia___unknown_etiology.
json` already named — a future session willing to extend
`emit-and-advance`'s own `:encounter-end` case to no-op when no
encounter is open — tracked as the SAME roadmap Deferred row.

**Dated erratum (2026-08-08, `notes/ADRs.md` ADR-0083, append-don't-
erase):** the diagnosis above — "the SAME standing `ehrt.sim-
trajectory.gmf-interpreter` gap `anemia___unknown_etiology.json`
already surfaced," "same root cause, not a new gap" — is CORRECTED,
not erased. The EncounterEnd fix (ADR-0082) closed that gap and, as
part of its own in-session proof, ran the same raw-trajectory scan
against `colorectal_cancer.json`'s own 300 seed-42 walks that it ran
for `anemia___unknown_etiology.json`: ZERO dangling `:encounter-end`
references anywhere, and `colorectal_cancer.json`'s own violations sit
BYTE-IDENTICAL before and after the fix landed — a fix with nothing to
correct in this module's own walks. This session's own diagnosis was
never itself probe-verified against a trajectory scan; it inferred the
same cause from the same shared `anemia/anemia_sub.json` submodule and
the same violation invariant family (`:discharge-follows-admission`/
`:clinical-content-only-when-admitted`) — plausible by adjacency, not
by evidence. `colorectal_cancer.json`'s real blocker is a separate,
still-undiagnosed defect one compile layer downstream of the
interpreter — see `.agents/plans/roadmap.md`'s own Deferred row, under
its own true name, and ADR-0083 for the correcting record. The lesson,
restated: a check (or an inference) that verifies one property does not
verify a different one later cited in its name — the SAME lesson
ADR-0082's own AR-EE-1c erratum (correcting ADR-0071) already named,
now confirmed a second time by this session's own inference, not a
check.

### Verification

- `bin/regression-oracle 5d40d4e fc44369 --declared-digest-change`:
  `DIFFERS`, EXPECTED — four added roots, zero changed/removed among
  the twenty-three pre-existing ones (the diff output itself is the
  evidence, not a count comparison).
- Manual pre/post digest comparison (Step 0 baseline vs. Step-1
  post-fix vs. Step-3 tree, direct `ehrt.oracle.digest/-main`
  invocation, no worktree): the twenty-three pre-existing roots'
  `.edn` manifests byte-identical at every checkpoint, including across
  the `uti_recurrence.csv` byte fix (zero movement, as predicted).
- `notice_verbatim_test.clj`: witnessed RED against the genuinely
  fresh-materialized pre-fix tree (one failure, the exact predicted
  mismatch), witnessed GREEN after the fix and again after the batch-3
  NOTICE rows landed (141 assertions, 0 failures).
- UTI round-trip tests (`ehrt.sim-emit-hl7.vendored-uti-test`,
  `ehrt.sim-trajectory.vendored-uti-test`): green after the byte fix, 7
  tests, 15 assertions, 0/0.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline
  (314 assertions in the tail project, 0/0, confirmed failure-free
  across the ENTIRE run's own output by grep, not just the tail) and
  again after every subsequent step, every project, 0/0 throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-vendoring-batch-2` peeled ref
  resolves to `96424f8` exactly.
- NOTICE hash cross-check: all thirteen new SHA-256 values re-derived
  by fresh `sha256sum` against the vendored bytes and matched against
  the table before commit; 69 total rows, all re-verified by the new
  gate itself (not merely a manual spot-check, for the first time).
- colorectal_cancer.json bail-out: three seeds tried (20260802, 1, 42),
  2 of 3 rejected, 4 violations each on the two rejected seeds — not a
  seed-tunable fluke at population scale.

### Deviations, disclosed

- **`colorectal_cancer.json` deferred whole** — see the dedicated
  section above; the largest single deviation from AR-VB3-1's own
  five-content-producer composition, disclosed in full. The batch
  commit message and NOTICE both name it.
- **Composition refinement (AR-VB3-1)** — batch 3 vendored the five
  content-producers named across the family groupings rather than the
  concurred plan's own "pair/quartet" framing, a channel-inferred,
  disclosed deviation the concurred plan's own composition never
  explicitly resolved (see AR-VB3-1's own reasoning, above).
- **`vhd-pulmonic`/`vhd-tricuspid` closure-file-count undercount** —
  the same JSON-only metric gap `asthma.json` found in batch 1,
  repeated here; disclosed in the Expected-count section, not a new
  class of surprise.
- **`vhd-pulmonic`/`vhd-tricuspid` test assertions narrowed to the
  REAL observed kind set** (`:outpatient-visit`/`:outpatient-visit-end`
  only, no `:condition-onset`/`:medication-order`) rather than the
  broader kind set most other vendored roots' tests assert on —
  disclosed, matching the real, tiny content these closures actually
  produce at 300 patients.
- **ADR-0071's own "zero problems" claim corrected** — see the
  dedicated dated-correction section above, append-don't-erase.
- **`poly test`'s own change-detection gap on untracked-only test
  additions** — the same operational finding ADR-0070/0071 already
  named, repeated here (staged before every `poly test` invocation, not
  itself fixed, out of this session's own scope).
