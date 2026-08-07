2026-08-07 — vendoring batch 3: the families that travel — and the verbatim law gets teeth Session prompt (design channel, 2026-08-07; conventions and every claim below read at HEAD `96424f8` against a fresh public clone). Fourth session of the vendoring arc. Prior: batch 2 landed and was design-channel-verified (`96424f8`, ADR-0071) — seven vendored, `anemia___unknown_etiology` deferred on the EncounterEnd-idiom gap. The verification's full-table NOTICE re-hash surfaced a REAL pre-arc defect this session's rider fixes under the author's ruling ("do what you think will work, but remember git may rewrite line endings," design channel 2026-08-07): `lookup_tables/uti_recurrence.csv` has never been byte-verbatim — upstream at the pin is CRLF (367 bytes, sha256 `baf597d27a7c139f962b7a100ff02abfcdc616c540478c7867e888305965aeda` — EXACTLY what its NOTICE row has recorded since 2026-08-02), but the repo's own `.gitattributes` root rule (`* text=auto eol=lf`) normalized the vendored copy to LF at landing (357 bytes, `b83c2960...`), so the NOTICE row was honest about upstream while the disk bytes silently violated the verbatim law, and every subsequent "re-verified" claim — ADR-0071's own "56 rows, zero problems" included — repeated the claim without executing the mechanism. Scope probe-confirmed: the other nine CSVs and every JSON are upstream-verbatim (LF upstream); this ONE file is CRLF upstream. Behavior probe-confirmed: `gmf.clj`'s own table parser uses `str/split-lines` (splits `\r?\n`), so restoring the true bytes parses identically — oracle movement is NOT expected, and any would be STOP-AND-REPORT. The repo already owns the fix idiom: `.gitattributes`' own `-text` entries for ER7 fixtures, commented with this same hazard. This session also executes batch 3 of the curation plan WITH a disclosed composition refinement (below), and records the author's next direction as intake. R30 ceremony. Read-first: `.gitattributes` in full (the root normalization rule and the `-text` precedent comments); `components/sim/resources/sim/modules/NOTICE` (the uti_recurrence row, the batch-2 section as the model); `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` ~1504–1540 (the parser's BOM-strip docstring — it already documents this file's quirky provenance — and its `split-lines`); ADR-0070/0071 (batch mechanics, both lessons, the anemia gap); the substance artifact rows for the batch-3 candidates; `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_hypothyroidism_test.clj` or siblings (the closure-bearing test pattern batch 2 refined). Author rulings (record verbatim in ADR-0072; `[A]` author-ruled, `[C]` channel-inferred)

1. AR-VB3-0 `[A — tag law, case (ii); debt recorded in ADR-0071]`. Annotated `stable-20260807-vendoring-batch-2` at `96424f8`, message `vendoring batch 2 landed, design-channel-verified 2026-08-07 (ADR-0071)`; push; verify peeled ref.
2. AR-VB3-R1 `[A for the fix mandate and the line-ending caution; C for the shape]` (the verbatim law gets teeth). Three parts, one step: (i) `.gitattributes` gains `components/sim/resources/sim/modules/** -text` with a comment in the file's established style — the vendored-modules tree is byte-verbatim BY LAW (NOTICE's own "no reformatting" clause), so line-ending normalization has no business anywhere in it; this is the author-flagged git-rewrites-line-endings hazard, closed at the layer that caused it. (ii) `uti_recurrence.csv` re-vendors byte-exact from the pin (CRLF, 367 bytes; its EXISTING NOTICE row — which always recorded the upstream hash — becomes true WITHOUT being edited; say so in ADR-0072). (iii) The co-landed gate: a docs-tooling test (`notice_verbatim_test.clj`) that parses every NOTICE table row and re-hashes the named file's on-disk bytes against the row's recorded hash — the exact sweep that caught this, promoted from design-channel probe to standing gate. NATURAL RED against the pre-fix tree (the uti_recurrence mismatch), witnessed, green after (ii). Verify the uti round-trip tests and the oracle show zero movement (the `split-lines` probe predicts none; any movement is STOP-AND-REPORT, never a silent declare). ADR-0072's record carries a dated correction of ADR-0071's "56 rows, zero problems" claim — append-don't-erase, the errata discipline.
3. AR-VB3-1 `[C — composition refinement, DISCLOSED deviation from the concurred plan, the author may veto]` (what batch 3 actually vendors). The concurred plan named "the metabolic-syndrome pair, the vhd quartet, colorectal-cancer, med-rec." The round-trip gate (nonzero content, witnessed) makes zero-substance modules unvendorable by this arc's own standard, and one-module-per-patient assignment (Wave G's own standing deferral) gives "the pair/quartet travels together" no runtime meaning today. Batch 3 therefore vendors the FIVE content-producers: metabolic_syndrome_care (139 events, closure ~6 JSON by the undercounting metric — enumerate fresh), vhd_pulmonic (3), vhd_tricuspid (3), colorectal_cancer (34), med_rec (269). The three zero-substance family members (metabolic_syndrome_disease, vhd_aortic, vhd_mitral) are RECORDED as not-vendorable-under-the-gate with their family context, joining the attribute-blocked set — revisit trigger: Wave E's register, or multi-module assignment (Wave G) giving family pairing a runtime meaning. Filenames from the checkout, ids from the census — never guessed.
4. AR-VB3-2 `[C — batch mechanics, third repetition]` (vendor + test + roots). Identical discipline to ADR-0070/0071: fresh closure enumeration from the pin-verified checkout; verbatim copy (the new `-text` rule now protects any CRLF member — note in ADR-0072 whether any batch-3 file IS CRLF upstream); SHA-256 rows under a dated batch-3 NOTICE section; per-module red-then-green round-trip at the batch convention (seed 20260802, 300 patients, 36500 horizon-days; deviate per module only as content demands, disclosed — batch 2's `:history`/population precedents); whole-module bail-out on any gap; each landed module a FIRST-BASELINE oracle root, additive; the existing TWENTY-THREE batches byte-identical throughout, `--declared-digest-change` for the additions per the script's own mechanics.
5. AR-VB3-3 `[A — the author's next direction, recorded verbatim as intake, not acted]` (the practitioner-UX horizon). The author ruled (design channel, 2026-08-07): "let's work more on the UX for practitioners, particularly the demos. I want to move the sim demos to a top-level demo place, and feature them in the intro materials." Recorded in ADR-0072 as the post-batch-3 direction — a design pass in the design channel first (the demos/scenarios homes, their relation to the top-level README and SETUP, the fence-gate implications of moving gated docs). NOTHING moves this session.
6. AR-VB3-4 `[C — scope]` (fences). NO module-content edits. NO loader/interpreter/engine/emitter changes. NO batch-4 modules, no wellness-encounters. NO demo/scenario relocation (AR-VB3-3 is intake). The `.gitattributes` edit touches ONLY the new vendored-modules rule. Standing untracked files untouched.

Steps Step 0 — Preflight + tag. Cwd ext4; tip `96424f8` or later-with-disclosure; pin-verify; full suite green baseline; oracle pre-digest (existing twenty-three). Execute AR-VB3-0. Step 1 — Rider (AR-VB3-R1): gate red witnessed against the live mismatch; `.gitattributes` rule + byte-exact re-vendor; gate green; uti round-trips green; oracle spot-check zero movement. Commit: `fix: the verbatim law gets teeth — uti_recurrence is byte-true, gated, and git can no longer rewrite it (vendoring batch 3, AR-VB3-R1)` Step 2 — Vendor + test, module by module (AR-VB3-1/2), red-then-green per module. ONE commit (or all-that-landed): `feat: the families' content-producers join the mix — metabolic care, two valves, colorectal, med-rec (vendoring batch 3, AR-VB3-1/2)` Step 3 — Oracle roots (AR-VB3-2). Commit: `test: new oracle roots — first baselines for the batch-3 closures (vendoring batch 3, AR-VB3-2)` Step 4 — ADR-0072 + record (AR-VB3-3 recorded; the ADR-0071 correction; per-module table; NOTICE re-hash now BY THE GATE, cite its green run). Index line; README count by `ls`; Done pointer `- 2026-08-07 — vendoring-batch-3 — ADR-0072`. Full oracle bracket (`96424f8` → tip). Successor tag debt IN THE ADR (`stable-20260807-vendoring-batch-3` at the closing tip). Session record + prompt self-archive. Final commit: `docs: vendoring batch 3 record — seventeen ailments, and the NOTICE cannot lie again (ADR-0072)` Fences Everything AR-VB3-4 names. The composition refinement (AR-VB3-1) is the channel's, disclosed — if the author's paste comes back with a veto, the concurred-plan composition escalates instead of executing. `[C]` rulings conflicting with the live tree fix forward and disclose; `[A]` ones escalate. After landing: design channel verifies by fresh probe — the gate re-run against the fresh clone, upstream-vs-disk for every batch-3 file, the refinement's recorded reasons — then the practitioner-UX design pass (AR-VB3-3's subject) opens in the design channel: the top-level demo home, the intro-materials feature, and what happens to `demos/`' captured-trace convention and `scenarios/` under the move.

## Deviation record

- **Prompt named five content-producers; four landed, one deferred
  whole.** `colorectal_cancer.json` was assessed and DEFERRED WHOLE per
  the AR-VB1-6/AR-VB2-5 bail-out precedent — its own call into the
  shared `anemia/anemia_sub.json` submodule sometimes lands outside an
  open encounter, tripping the SAME `:encounter-end` gap
  `anemia___unknown_etiology.json` already surfaced in batch 2 (2 of 3
  seeds tried rejected at 300 patients). Full finding:
  `notes/adr/0072-vendoring-batch-3.md` and `components/sim/resources/
  sim/modules/NOTICE`'s own dated section. The prompt's own Steps
  section named the batch as "the families' content-producers ...
  metabolic care, two valves, colorectal, med-rec" — the actual landed
  set drops colorectal; the Step 2 commit message was adapted
  accordingly (see below), not left naming a module that didn't land.
- **Commit messages adapted from the prompt's own literal template
  text**, the same disclosed pattern ADR-0071's own prompt archive
  named: Step 2 landed as `feat: the families' content-producers join
  the mix -- metabolic care, two valves, med-rec (vendoring batch 3,
  AR-VB3-1/2)` (colorectal dropped, matching the actual landed set,
  plus a body paragraph naming the deferral and the three zero-
  substance siblings); Step 4's actual final commit message differs
  from the prompt's own literal Step 4 text (`docs: vendoring batch 3
  record -- seventeen ailments, and the NOTICE cannot lie again
  (ADR-0072)`) because "seventeen ailments" counted a composition
  (five content-producers) that itself changed to four landed — the
  actual final commit message is authored fresh against what actually
  landed, named in this record's own final-commit line, not copied
  from a prompt template built against a plan that shifted twice
  (family refinement, then the colorectal bail-out).
- **The RED witness for the rider's own gate required a second
  materialization method.** The first attempt (`git checkout --
  <path>`) silently no-op'd — git's own stat-cache shortcut skipped
  the rewrite because the working tree file already existed with
  matching stat info, even though its raw bytes differed from what a
  genuine checkout produces. Diagnosed by comparing `git show :path`
  (the index blob's own hash) directly against the working-tree
  bytes; `rm` followed by `git checkout-index -f` forced the genuine
  rewrite and surfaced the real mismatch. Not named in the prompt's
  own text (which anticipated "the gate red witnessed against the live
  mismatch" without specifying the mechanics), disclosed here as the
  actual method used.
- **AR-VB3-2's own "existing TWENTY-THREE batches" count verified
  exact** — `digest.clj`'s own `roots` map held exactly 23 entries
  before this session's edits, matching the prompt's own claim without
  correction.
- **`vhd-pulmonic`/`vhd-tricuspid` test assertions narrowed to the real
  observed kind set** (`:outpatient-visit`/`:outpatient-visit-end`
  only) rather than the broader kind set most other vendored roots'
  tests assert on — found by direct `engine/run` + `frequencies`
  probing, disclosed rather than silently matching the template; not
  explicitly anticipated by the prompt's own text.
