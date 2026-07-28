2026-07-28 — SS-4 (build): sinks close the loop — manifest emission, the composability law, the stdout/MLLP sink
Context
SS-1..3 built sources; SS-4 finishes sinks and proves the design's central law: every sink's output is a valid source. Dir/file sinks gain manifest emission so their output directories are intake-ready exactly as sim's are; the law lands as a property test (write through a sink → intake the result → items and lineage survive); and the `stdout:` sink arrives with MLLP/er7/ndjson framing encode — whose composability proof is the loopback: tools piping its own output into its own stdin source. Write discipline completes with explicit `:overwrite`/`:append`, honestly per-kind (append is sound only where the framing concatenates).
One constraint is probed before anything is written: the manifest interop question between this repo and sim (two design options were framed for the author and remain undecided). If emitting a tools-side manifest that intake consumes requires resolving that decision — i.e. ManifestV1_1 cannot express tools-as-producer without abusing sim's schema — that is a STOP-AND-REPORT to the author, not a schema improvisation. Cross-repo formats do not evolve unilaterally from the consumer side.
Step 0 carries in the SS-3 checker findings: the design doc's D8 record-keeping exemption list is missing the spool's `:captured-at`; the wall-clock-derived spool path needs naming as a deliberate exception (captures are events — a stable path would trip fail-if-exists on every second capture); and the spool's check-then-write atomicity buys its guarantee with peak memory equal to the cap, a trade worth one stated sentence.
This session adds integration tests: T2 owed.
Read first

* `docs/source-sink-design.md` (sink rules, composability law, D8 and its dated notes, SS-4 plan row), `notes/ADRs.md` ADR-0014 (sim-side: manifests as the consumer's evidence), ADR-0017
* `src/ehr_testing_tools/corpus/manifest.clj` (what intake accepts from sim manifests — the schema authority for the probe), `intake.clj` (manifest enrichment path), `sink_write.clj`, `framing.clj` (encode side), `spool.clj`, `mutate.clj` (the writer that gains a Sink seam), `source_sink.clj`/`source_sink_url.clj`
* `test-integration/`: the real-pipe stdin test (the loopback's precedent), sim harness (manifest assertion patterns)

Author rulings

1. Step 0 is one dated note, three sentences, doc register only. Extend D8's named record-keeping exemptions with the capture manifest's `:captured-at`; name the wall-clock spool path as a deliberate event-not-generation exception with the fail-if-exists inversion rationale; state the spool's memory-for-atomicity trade and that it argues for a conservative default cap. No code.
2. The interop probe gates manifest emission. Determine whether a tools-written manifest can (a) validate against exactly what `manifest.clj`/intake accept today and (b) state its producer honestly (this repo, its version, the writing operation) without colonizing fields sim's schema means otherwise. If yes: emit that, and record in the design doc that tools writes the same manifest shape it reads, producer-distinguished. If no: STOP-AND-REPORT with the two framed interop options laid beside the concrete blocker — the author's pending decision just became load-bearing and gets made with this evidence in hand.
3. Dir/file sinks emit manifests; the manifest is the sink's word. Content: producer identity, the writing operation and its params (operator lineage when mutate is the writer), per-item content hashes, format, framing, written-at (a record-keeping field — add it to Step 0's note so the list is extended once, completely). Emission is atomic-adjacent: manifest written last, after all items land, so a torn write is detectable as items-without-manifest.
4. The composability law is a property test, stated per sink kind. Dir/file: for generated item sets, write → intake → the catalog's content hashes equal the written items' and `:origin` reflects the sink's manifest; runs hermetically with small generated corpora. Stdout: the law is the framing round-trip plus the loopback (ruling 5) — stated in the design doc as the byte-stream form of the law, not exempted from it.
5. The stdout sink and the loopback. `stdout:?framing=…&format=…` encodes items via `framing.clj` and writes bytes to stdout; no manifest (no directory — the design-doc law statement covers why). The loopback acceptance is integration-tier and REAL: `bin/ehr <emit through stdout:> | bin/ehr corpus intake 'stdin:?framing=…&format=…' --out …` — items in, catalog out, hashes equal. Which verb emits is the agent's call from what exists (mutate writing to a stdout sink is the natural candidate); if no verb can yet address a stdout sink, wiring mutate's output through Sink resolution IS in scope (ruling 6) — the loopback is not optional.
6. Writers gain the Sink seam additively. `mutate` (and intake's `--out`) resolve their output designator through Sink machinery: bare paths behave exactly as today (golden: existing tests unchanged), URL spellings gain the new kinds. No verb's flags change.
7. Write discipline, honestly per kind. `:overwrite` for dir/file (explicit, destructive, documented). `:append` only where the framing concatenates soundly — `er7-multi`, `ndjson`, `mllp` file sinks; REJECTED as `:append-unsound` for `bundle-entries` (a JSON document does not concatenate) and for dir sinks this session (append-to-corpus means manifest merge — recorded as an OPEN item, not improvised). Fail-if-exists stays the default everywhere.
8. Scope fence. No Blaze (D-b still gates it); no dir-sink append (OPEN item per ruling 7); no manifest schema evolution (ruling 2's stop governs); no framing-aware `dir:` sources (OPEN-5 untouched).
9. Tiers: T0 per commit; T1 + T2 + coverage before the final commit; wall times reported; pause before push.

Steps
Step 0 — The D8 register note
Per ruling 1. Commit: `docs: D8 exemption list completed (:captured-at, :written-at); spool path and memory-for-atomicity trades named (SS-3 checker findings)`
Step 1 — Interop probe, then manifest emission
Per rulings 2–3 (probe first; STOP if it bites), hermetic red→green. Commit: `feat: dir/file sinks emit intake-ready manifests — producer-distinguished, items-then-manifest ordering (SS-4)`
Step 2 — The composability property
Per ruling 4, dir/file form. Commit: `test: the composability law — sink output re-intakes with hashes and origin intact (SS-4 acceptance)`
Step 3 — stdout sink
Per ruling 5, encode side + the design-doc law statement. Commit: `feat: stdout sink with framing encode — the byte-stream form of the composability law (SS-4)`
Step 4 — Writers through the Sink seam + the loopback
Per rulings 5–6; the loopback lands in `test-integration/` and runs for real. Commit: `feat: mutate and intake --out resolve through Sink; the loopback — tools' stdout into tools' stdin, hashes equal (SS-4)`
Step 5 — Write discipline
Per ruling 7; the dir-append OPEN item recorded. Commit: `feat: :overwrite explicit; :append where framing concatenates, :append-unsound where it does not (SS-4)`
Step 6 — Strips, docs, cli regen
A loopback strip verified for real; affected strips re-verified; `make cli-doc`; freshness gates. Commit: `docs: loopback strip verified for real; cli regenerated (SS-4)`
Step 7 — Tiers, plan row, archive (pause before push)
Per ruling 9; SS-4 row → Done with fence notes. Commit: `prompts: archive 2026-07-28 ss-4 build session`
Final report
The interop probe's verdict and evidence (or the STOP report with the two options laid against the blocker); manifest field set as shipped; composability property parameters (item-set generation, iterations); the loopback's real command line and hash-equality evidence; per-kind append soundness table; new OPEN items; tier wall times + coverage; red→green evidence; deviations.
