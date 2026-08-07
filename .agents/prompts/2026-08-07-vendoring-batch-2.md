2026-08-07 — vendoring batch 2: the chronic clinic tail — and scenarios get a home
Session prompt (design channel, 2026-08-07; conventions and every claim below read at HEAD `d41a278` against a fresh public clone). Third session of the vendoring arc. Prior: batch 1 landed and was design-channel-verified (`d41a278`, ADR-0070) — five vendored (all 15 NOTICE hashes re-derived clean by the design channel against the vendored bytes), `injuries` deferred whole on the `broken_jaw` dental-referral loop (~3.3% of walks hit the 10,000-step backstop, verified at the census's own code path). Two batch-1 lessons bind THIS session, both recorded in ADR-0070: (1) the census's `:closure-file-count` counts JSON only — asthma's "3" was 11 with its CSVs — so closures are ENUMERATED FRESH from the pin checkout, never read off the artifact's metric; (2) the census's three-seed sample can miss population-scale failures — the 300-patient round-trip gate is the real filter, and every module keeps its whole-module bail-out. This session executes batch 2 of the concurred curation plan (ADR-0070's own recorded text): hypothyroidism, rheumatoid-arthritis, osteoarthritis, osteoporosis, anemia-unknown-etiology, attention-deficit-disorder, allergic-rhinitis, dermatitis — eight modules, each 1–2 JSON files by the artifact's (undercounting) metric, so the fresh enumeration is the first act and a materially larger total is disclosed, not absorbed. One rider, author-initiated (design channel, 2026-08-07): demo-scale SCENARIOS get a repo home. R30 ceremony. Read-first: ADR-0070 in full (the batch-1 mechanics this session repeats verbatim, the curation plan, both lessons); `components/sim/resources/sim/modules/NOTICE` (the row format, the batch-1 section as the model); `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_dementia_test.clj` and `vendored_asthma_test.clj` (the single-file and closure-bearing test patterns from batch 1); `components/oracle/src/ehrt/oracle/digest.clj`'s batch-1 dated note (root-addition mechanics, `--declared-digest-change` usage); `components/sim/docs/demos/README.md` (the ≤10-patient captured-trace convention the new scenarios home deliberately does NOT inherit). Author rulings (record verbatim in ADR-0071; `[A]` author-ruled, `[C]` channel-inferred)

1. AR-VB2-0 `[A — tag law, case (ii); debt recorded in ADR-0070]`. Annotated `stable-20260807-vendoring-batch-1` at `d41a278`, message `vendoring batch 1 landed, design-channel-verified 2026-08-07 (ADR-0070)`; push; verify peeled ref.
2. AR-VB2-R `[A for the need ("I want to have demo scenarios to include busy-tuesday", design channel 2026-08-07); C for the shape]` (the scenarios home). A new `components/sim/docs/scenarios/` convention lands, sibling of `demos/`: a scenario is a RUNNABLE configuration — `<name>/config.edn` plus `<name>/README.md` whose command fences are root-resolvable and gated by the same invocation-lint/fence-path machinery as every live doc — with NO captured trace (scenario output is population-scale; `demos/`' own ≤10-patient captured-trace convention explicitly does not apply, say so in the new `scenarios/README.md`). First scenario: `busy-tuesday/` — config content EXACTLY as this prompt's appendix states (the design channel authored it against the twelve-module tree; the session lands it verbatim, then LIVE-PROBES it: the README's own generate command runs to completion and `bin/ehrt play <out-dir> --board 60 --rate 100000` renders snapshots with content from the new modules — witnessed, recorded in ADR-0071). `demos/README.md` gains one cross-reference line to the new sibling. If any fence gate rejects the layout, fix the layout to satisfy the gate, never the reverse.
3. AR-VB2-1 `[C — batch-1 mechanics, repeated]` (the vendoring). For each of the eight: enumerate the FULL closure (JSON submodules AND lookup tables) from the pin-verified checkout (`/home/mg/synthea-checkout` at `7e08387c...f902`, verify-pin first); copy VERBATIM; SHA-256 each; one NOTICE row per file in the established format under a dated batch-2 section header. An edit-tempting file is STOP-AND-REPORT; a closure member already vendored is verified byte-identical, never re-copied blind (batch 1's shared-submodule precedent).
4. AR-VB2-2 `[C — batch-1 gate, repeated]` (the tests). One `vendored_<module>_test.clj` per module, engine-layer round trip on the batch-1 convention (seed 20260802, 300 patients, `:module-horizon-days` 36500 — deviate per module only if content demands it, disclosed) witnessed producing nonzero trajectory events AND nonzero rendered messages; red on the missing resource first, green after the copy, per module. A module that cannot be made to produce, or that trips a loader/interpreter gap at population scale (the injuries precedent), is DEFERRED WHOLE with a finding — the batch lands minus it.
5. AR-VB2-3 `[C — batch-1 mechanics, repeated]` (the roots). Each landed module joins `digest.clj` as a NEW engine-layer root, FIRST BASELINE, additive only; the official bracket runs with `--declared-digest-change`; the existing SIXTEEN batches (eleven pre-arc + batch 1's five) must be byte-identical across the session — any movement is STOP-AND-ESCALATE.
6. AR-VB2-4 `[C — intake, not acts]` (the census-refinement intake). ADR-0071 records as next-close intake, citing batch 1's lessons: (i) a census closure-file-count that counts data files too (the JSON-only undercount); (ii) a population-scale substance/walk check (the three-seed blind spot injuries proved) — both adjacent to the Deferred row's standing item (b), neither acted on here.
7. AR-VB2-5 `[C — scope]` (fences). NO module-content edits. NO loader/interpreter/engine/emitter changes. NO batch-3/4 modules. The scenario config lands VERBATIM from the appendix — a config that fails its live probe is STOP-AND-REPORT (the design channel's authoring is then wrong, and that's the design channel's to fix), never silently tuned. Standing untracked files untouched.

Steps Step 0 — Preflight + tag. Cwd ext4; tip `d41a278` or later-with-disclosure; pin-verify; full suite green baseline; oracle pre-digest (existing sixteen). Execute AR-VB2-0. Step 1 — Rider (AR-VB2-R). Scenarios home + busy-tuesday landed; fence gates green; the live probe witnessed. Commit: `docs: scenarios get a home — busy-tuesday is runnable from the tree (vendoring batch 2, AR-VB2-R)` Step 2 — Vendor + test, module by module (AR-VB2-1/2), red-then-green per module. ONE commit (or all-that-landed per the bail-out): `feat: five everyday ailments join the mix -- asthma, bronchitis, sleep-apnea, fibromyalgia, dementia; injuries deferred whole on a real interpreter gap (vendoring batch 1, AR-VB1-2/3)` Step 3 — Oracle roots (AR-VB2-3). Commit: `test: new oracle roots — first baselines for the batch-2 closures (vendoring batch 2, AR-VB2-3)` Step 4 — ADR-0071 + record (AR-VB2-4). Rulings verbatim; per-module closure/seed table; NOTICE hashes re-verified fresh; the intake notes; index line; README count by `ls`; Done pointer `- 2026-08-07 — vendoring-batch-2 — ADR-0071`. Full oracle bracket (`d41a278` → tip). Successor tag debt recorded IN THE ADR (`stable-20260807-vendoring-batch-2` at the closing tip). Session record + prompt self-archive. Final commit: `docs: vendoring batch 2 record — the clinic fills in (ADR-0071)` Fences Everything AR-VB2-5 names. The curation plan is executed, not extended. `[C]` rulings conflicting with the live tree fix forward and disclose; `[A]` ones escalate. After landing: design channel verifies by fresh probe — NOTICE re-hash, the scenario's fences resolved from a fresh clone, the board probe's witnessed output — then batches 3–4 await their rulings, and the arc close (with AR-VB2-4's intake) follows the last batch the author wants this arc.
Appendix — `components/sim/docs/scenarios/busy-tuesday/config.edn`, verbatim: {:pathway {:name "busy-tuesday" :steps []} :arrival-gap 5 :modules ["sore_throat" "sinusitis" "bronchitis" "asthma" "ear_infections" "urinary_tract_infections" "sleep_apnea" "fibromyalgia" "dementia" "appendicitis" "total_joint_replacement" "sepsis"] :module-assignment [{:module-id "sore_throat" :weight 15} {:module-id "sinusitis" :weight 12} {:module-id "bronchitis" :weight 10} {:module-id "asthma" :weight 10} {:module-id "ear_infections" :weight 8} {:module-id "urinary_tract_infections" :weight 8} {:module-id "sleep_apnea" :weight 7} {:module-id "fibromyalgia" :weight 6} {:module-id "dementia" :weight 6} {:module-id "appendicitis" :weight 6} {:module-id "total_joint_replacement" :weight 6} {:module-id "sepsis" :weight 6}] :module-initial-attributes {"total_joint_replacement" {:total-joint-replacement/joint-replacement "knee"}} :module-horizon-days 3650} (The landed file additionally carries the header comment from the design channel's delivered copy — command fences live in the README, root-resolvable, so the lint gate walks them.)

## Deviation record

- **Prompt named eight modules; seven landed, one deferred whole.**
  `anemia___unknown_etiology.json` was assessed and DEFERRED WHOLE per
  AR-VB2-5's own bail-out clause — a real, standing `gmf-interpreter`
  gap (an unconditional `:encounter-end` compiled from an upstream
  "close if open, else no-op" idiom) the census's own narrow three-seed
  sample never sampled (confirmed across three seeds at 300 patients,
  12/17/6 violations, not seed-tunable). Full finding:
  `notes/adr/0071-vendoring-batch-2.md` and `components/sim/resources/
  sim/modules/NOTICE`'s own dated section.
- **Two landed modules needed a real, disclosed test-configuration fix
  beyond the batch's own 300-patient/seed-20260802/36500-day
  convention** — `attention_deficit_disorder.json` needed `:history
  true` (a straddling-encounter case, ADR-0042's own mechanism, unused
  by every prior vendored root); `allergic_rhinitis.json` needed 3000
  patients, not 300 (its own low onset odds land in early childhood,
  always pre-registration at the batch convention's own population
  size). Both licensed by AR-VB2-2's own "deviate per module only if
  content demands it, disclosed" clause. Full account: ADR-0071's own
  Execution record.
- **Expected file count (naive sum 19) did not match the actual total
  (16), for reasons unrelated to batch-1's own JSON-vs-CSV
  undercount** (this batch had zero lookup-table CSVs) — a shared
  closure member (`anemia/anemia_sub.json`, counted twice, landed
  once) and an already-vendored reuse (`dme/wheelchair_end.json`) fully
  account for the gap. Full account: ADR-0071's own "Expected-count
  disclosure" section.
- **Commit messages for Step 2 were adapted from the prompt's own
  literal template text** (the session prompt's own Steps section
  named a batch-1-era placeholder message by copy-paste error; the
  actual Step 2 commit landed as `feat: the chronic clinic tail joins
  the mix -- seven ailments land, one deferred on a real interpreter
  gap (vendoring batch 2, AR-VB2-1/2)`, naming this session's own
  actual landed set and deferral) — fix-forward with disclosure, the
  same standing discipline ADR-0070's own prompt archive already
  applied to its own Steps text.
- **AR-VB2-4's own census-refinement intake landed as a dated note on
  the existing "Census tool refinements" Deferred row PLUS a new,
  separate Deferred row for the `:encounter-end` no-op gap** (the
  latter not explicitly named in the prompt's own AR-VB2-4 text, but a
  direct, disclosed consequence of the bail-out finding above needing
  its own revisit trigger, the same treatment `injuries.json`'s own
  batch-1 finding received).
