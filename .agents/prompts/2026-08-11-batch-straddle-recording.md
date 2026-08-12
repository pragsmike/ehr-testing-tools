# 2026-08-11 — ehr-testing-tools: batch-straddle recording -- use case, rulings, and the user-guide opening (ADR-0112)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `ed5f51d` (ADR-0111's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim. A mid-session fence amendment (author-ruled, the count-lock
test bump licensed into commit 1) is recorded in `notes/adr/0112-*.md`'s
own Deviations block, not reproduced here -- the amendment is a
mid-session author communication, not part of the driving prompt.

## Original prompt (verbatim)

Session prompt — batch-straddle recording: use case, rulings, and the user-guide opening (ADR-0112)
You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This is a DOCS-AND-REGISTERS-ONLY session: zero `src` change anywhere, zero test-code change anywhere. It records three things the author ruled at the close of the ADR-0111 window: the batch-boundary-straddling encounter scenario's documentation placements, the rulings behind them, and the opening of the tool-specific user-guide work. Everything you need is pre-decided below — STOP-AND-REPORT on any conflict between this prompt and the tree; never resolve silently.
Read first

1. `notes/adr/0111-corpus-batching.md` — the batching ADR this session documents forward from.
2. `demos/scenarios/ed-tuesday/README.md`, the "Batched delivery" section (line ~268) — the witnessed run the new use case's commands and cross-links must match exactly.
3. `components/corpus/docs/use-cases.edn` — header comment (the catalog doctrine) plus two shape precedents: the `:play-a-generated-corpus-back-over-time` case (sim-generate command paired with a general `hl7v2-directory` equation input; footnote-marker ADR references) and the `:black-box-transform-surround` case (`{external: true}` stage syntax).
4. `.agents/rulings.md` — tail (the three ADR-0111 rulings; the new entries land after them in the same format).
5. `.agents/plans/roadmap.md` — the user-guide trigger note (the paragraph containing "PENDING AUTHOR RATIFICATION", ~line 139) and the Done section tail.
6. `docs/dev/simulator-architecture.md` — Read-first standing practice for any session near the sim family (ADR-0108); this session touches no sim code, but the demo it cross-links is sim-generated.

Author rulings, verbatim (record these; do not paraphrase in the
registers)
Ruled 2026-08-11, in the ADR-0111 window's batching-documentation exchange:
"We need to add this batch-boundary-straddling encounter message scenario to the documentation. Should it be a use case? It should be a demo, and featured prominently in the tool user guide, and in the more general EHR testing guide as it's something that happens in the real world and can trip up the unaware."
And, accepting the channel's proposed recording sequence:
"ok, but this session is getting old. Let's put that in the next session to record in the repo, and in the continuity prompt."
Step 0 — Preflight and tag ceremony (tag first, per ceremony)

* Fresh state: `git fetch`; confirm `origin/main` is at `ed5f51d` (ADR-0111 close). If it is not, STOP-AND-REPORT.
* Confirm the last five CI runs on `main` are green (`gh run list --limit 5 --branch main`). If the API rate-limits, disclose that in the session record per the standing structural-plus-transcribed pattern and proceed.
* Tag: `stable-20260811-corpus-batching`, ANNOTATED, at `ed5f51d`; push the tag; confirm the peeled ref matches `ed5f51dc2c81b05f978d54ca9181f4fa26e7db59` exactly.
* License: tag-law case (i), EARNED. The design channel verified the ADR-0111 landing by fresh public clone on 2026-08-11: lineage (b5b9b9e -> 1e0a1d6 -> d1f8fa1 -> ed5f51d), ASCII byte-check on all three commit messages (channel re-ran `LC_ALL=C grep` over `git log %B`, clean), and the four deep re-derivations the ADR-0111 close left INHERITED — all four completed and clean: (1) an independent partition-semantics model (half-open epoch-aligned buckets confirmed for ts >= 0; global cross-file MSH-7 sort with stable tie order; UTC DTM interpretation matches `parse-dtm-lenient`'s own contract; one benign channel note: Clojure `quot` truncates toward zero, so the half-open law would invert for pre-1970 timestamps — domain-irrelevant for MSH-7, recorded, not a defect); (2) the `:batch` codec against the v2 batch-protocol segment definitions (minimal BHS-1/BHS-2 legal, BTS-1 true-count verified on decode, byte-level throughout, round-trip closes including the empty-items leg); (3) the straddle witness's arithmetic against the demo's own witnessed epoch values (1786406400000 = 2026-08-11T00:00:00Z exactly; A01 00:30:26Z -> bucket [00:00,01:00), A03 01:34:19Z -> [01:00,02:00), adjacent; the 08:00Z–10:00Z gap real: batch-031 ends 1786521600000 = 08:00Z, batch-032 starts 1786528800000 = 10:00Z); (4) the two-file cross-file-ordering test's genuineness (synthetic `batch-msh` messages, epoch-1970 MSH-7, nothing sim-shaped; file b's bucket-1 message chronologically precedes file a's and the test asserts byte order inside the written file).

Step 1 — The use-case entry
Add the following case to `components/corpus/docs/use-cases.edn`, placed immediately after the `:play-a-generated-corpus-back-over-time` case (it is that case's transport-realism sibling). Content is given in full; adjust only EDN string-escaping mechanics, never wording. If the Malli schema (`ehrt.docs-tooling.usecases/UseCases`) rejects any field, STOP-AND-REPORT with the schema error rather than rewording.

```edn
{:id :supply-batch-straddling-traffic
 :title "Supply batch-straddling encounter traffic to a downstream"
 :audience "Teams testing whether a downstream receiver's encounter-completeness logic survives scheduled batch delivery -- where a clinically open encounter's messages arrive split across adjacent, individually transport-clean batches."
 :bring "A downstream receiver (yours, external to this repo) that consumes HL7 v2 batch files and decides, per encounter, whether it holds the full record set. The commands below generate the input corpus; any existing directory of valid v2 message files -- including a corpus this repo never generated -- batches identically."
 :get "Deterministic BHS/BTS batch files, epoch-aligned to the delivery schedule, every written file's BTS-1 message count decoded back and verified before success -- containing at least one encounter whose admission and discharge land in different batches. Transport-level completeness with clinical-level incompleteness: the exact case a receiver's \"do I have all of this encounter?\" decision needs, and one that trips up the unaware in real feeds."
 :maturity :usable
 :commands
 {:lines ["# Generate the ed-tuesday scenario's latency wire (283 messages,"
          "# seed-pinned, byte-reproducible)."
          "bin/ehrt corpus generate sim --seed 20260811 --patients 100 \\"
          "  --reference-date 2026-08-11 --churn \\"
          "  --config demos/scenarios/ed-tuesday/config-latency.edn \\"
          "  --out-dir out/scenarios/ed-tuesday-latency"
          ""
          "# Batch it hourly: buckets align to the clock hour against the Unix"
          "# epoch, BHS/BTS wrappers, BTS-1 self-verified on every write."
          "bin/ehrt corpus batch out/scenarios/ed-tuesday-latency --interval 60 \\"
          "  --out-dir out/scenarios/ed-tuesday-latency-batches"
          ""
          "# What you got: one file per occupied hour; interior empty hours are"
          "# simply absent (a named v1 deferral)."
          "ls out/scenarios/ed-tuesday-latency-batches"]
  :note "The witnessed straddling encounter -- Smith, James (MRN000002): admitted in `batch-000.hl7`, discharged in `batch-001.hl7`, both files individually BTS-verified clean -- is worked end to end, wrapper bytes and all, in [the ed-tuesday scenario's Batched delivery section](../../demos/scenarios/ed-tuesday/README.md#batched-delivery)[^adr-0111]. Flags and their defaults: [cli.md](../cli.md#ehrt-corpus-batch), or `ehrt help corpus` at the shell. The batcher is corpus-level and sim-separate by ruling: it reads any directory of valid `.hl7` files, so a foreign corpus is exactly as batchable as this scenario's own out-dir.\n\n[^adr-0111]: Design record [ADR-0111](../../notes/ADRs.md)."}
 :equations ["generator-config × sim-engine → generated-corpus  [EngineExecute]"
             "hl7v2-directory × delivery-interval → delivery-batches  [Batch]"
             "delivery-batches → receiver-completeness-decision  [YourReceiver]  {external: true}"]}

```

Then regenerate and gate:

* `make docsgen` (regenerates `docs/use-cases.md`, `docs/use-cases/` per-case pages, plus operators/cli/pipeline surfaces — `docs/cli.md`, `docs/pipeline.md`, `docs/operators.md` must come out BYTE-UNCHANGED; the use-cases surfaces are the only expected delta. A delta anywhere else is STOP-AND-REPORT).
* Full local gate: `make test` (poly check + full suite + verify-nist-lock). The co-verifying gates this step must pass by name: `ehrt.docs-tooling.usecases-test` (Malli), `ehrt.docs-tooling.invocation-lint-test` (the new case's `.edn` and generated `.md` are inside its scan roots), `ehrt.docs-tooling.link-footnote-gate-test` (no ADR token in user prose; the footnote-marker form above is the licensed shape), `make lint-pipeline` (the new equations carry no catalytic resources, so it must pass unchanged).

Commit 1 (message verbatim, ASCII):

```
docs: use case -- batch-straddling encounter traffic to a downstream (ADR-0112)

```

Step 2 — Rulings and roadmap
`.agents/rulings.md` — append, in the established entry format, after the ADR-0111 entries:

1. Batch-straddle documentation placements [A, ruled 2026-08-11]: carry BOTH author quotes from "Author rulings, verbatim" above, in full. Decision text: the batch-boundary-straddling encounter scenario gets three documentation placements — (a) a demo (landed, ADR-0111, `demos/scenarios/ed-tuesday/README.md` "Batched delivery"); (b) featured prominently in the tool-specific user guide (opened this session, see the roadmap row); (c) a treatment in the general EHR Testing Guide (Ch 24 "completeness illusion" section — the author's own queue, the guide is permanently outside this workspace per `AGENTS.md`). The use case landed this session (ADR-0112) executes the "Should it be a use case?" half of the ruling in the affirmative, per the sequence the author accepted.
2. User-guide trigger read [C, channel-read, recorded honestly for author correction at a glance]: the channel read the author's "featured prominently in the tool user guide" plus the subsequent "ok" (accepting the channel's proposed recording sequence) as RATIFYING the user-guide trigger — this workspace's own `--board` accepted as the downstream-receiver stand-in the trigger's language anticipated (both trigger conditions met, ADR-0110) — and as OPENING the tool-specific user-guide work. The author did not veto this reading when it was stated explicitly in the same exchange. Provenance is channel-read, not author-verbatim; the author may strike or correct it.

`.agents/plans/roadmap.md`:

1. Update the user-guide trigger paragraph (the one containing "PENDING AUTHOR RATIFICATION"): replace the PENDING language with a dated note (the session's own run date, ADR-0112) that the trigger is RATIFIED per the channel-read ruling above (cite `.agents/rulings.md`, the entry landed this session), with the channel-read provenance named in the note itself.
2. New Next row: Tool-specific user-guide design pass — status awaiting-design-pass; the design channel frames it (structure, audience voice, gap analysis over the accreted `docs/` skeleton); SETUP.md's unspoiled-human-reader rewalk is that pass's smoke test (the rewalk itself remains an author-only errand); the batch-straddle scenario is ruled "featured prominently" (rulings entry 1). Not chartered to any executing session yet.
3. New row, author's-queue (not a session charter): EHR Testing Guide Ch 24 "completeness illusion" section notes — the batch-straddle scenario's guide-side treatment; the channel may draft notes on request, grounded in the ADR-0111 demo's witnessed run; the guide lives outside this workspace.

Commit 2 (message verbatim, ASCII):

```
docs: record batch-doc-placement rulings and open user-guide design pass (ADR-0112)

```

Step 3 — ADR, close phase

* Self-archive this prompt to `.agents/prompts/` at the START of the close phase (standing discipline), before anything else in this step.
* `notes/adr/0112-batch-straddle-recording.md`: context (the two author quotes verbatim; the sequence accepted), decision (the three placements; the use case's full shape; the rulings and roadmap rows landed; the channel-read flag reproduced honestly), tag ceremony (Step 0's license reasoning, including the four channel re-derivations by name), oracle bracket, full gate, fences, index line. Update `notes/ADRs.md` (index entry) and `notes/adr/README.md` (count 109 -> 110, and its as-of line).
* Roadmap Done line: `- <run date> — batch-straddle-recording — ADR-0112` (the session's own run date, same convention as every prior line).
* Session record under `.agents/session-records/`.

Oracle bracket. Pre-analysis: pure identity on all 35 roots — this session touches `components/corpus/docs/use-cases.edn`, generated `docs/use-cases*` surfaces, `.agents/*`, `notes/*` only; no `src`, no test code, no demo scenario config, no emitter-adjacent anything. Run `bin/regression-oracle ed5f51d <final-commit>` and require `IDENTICAL` on every root. Any non-identity is STOP-AND-REPORT, not a rider.
Gates at close: `make test` green; `gitleaks git --staged -v` per commit and `gitleaks detect` pre-push; ASCII byte-check (`LC_ALL=C grep -n '[^ -~]'` over each `git log --format=%B`) on all three commit messages; push; confirm CI on `main` (or disclose rate-limiting per the standing pattern).
Commit 3 (message verbatim, ASCII):

```
docs: session record and prompt archive -- batch-straddle recording (ADR-0112)

```

Fences

* Touch ONLY: `components/corpus/docs/use-cases.edn`; the generated `docs/use-cases.md` + `docs/use-cases/*.md`; `.agents/rulings.md`; `.agents/plans/roadmap.md`; `.agents/prompts/*` (self-archive); `.agents/session-records/*`; `notes/adr/0112-*.md`; `notes/ADRs.md`; `notes/adr/README.md`.
* `docs/cli.md`, `docs/pipeline.md`, `docs/operators.md`: regenerated by docsgen but must be byte-unchanged — verify with `git status` after regen.
* ZERO changes under any `src/` or `test/` directory, any `demos/scenarios/*/config*.edn`, `docs/dev/`, `README.md`, `AUTHORS-GUIDE.md`, `Makefile`, or CI workflows.
* The fence rule, stated as a RULE (ADR-0099 precedent): this session may modify documentation-and-register surfaces named in the touch list above and nothing else; the list is illustration of the rule, not a substitute for it — a file outside both is STOP-AND-REPORT, never a judgment call.
* Do not regenerate `out/scenarios/*` — the use case's commands are documentation of the ADR-0111 witnessed run, not commands this session re-executes.

STOP-AND-REPORT on: schema rejection of the case as given; any docsgen delta outside the use-cases surfaces; any oracle non-identity; any conflict between this prompt's claims and the tree; anything this prompt failed to pre-decide.
