2026-07-28 — SS-3 (build): framing codecs as pure functions, the spool as the second unification, stdin as a source
Context
The design's framing axis (file ≠ item) gets its implementation: `er7-multi`, `ndjson`, `bundle-entries`, and `mllp` codecs as pure functions with stated laws, the 1,013-message SimHospital fixture (ADR-0011) as the `er7-multi` witness, and the spool — the second unification: just as generators reduce to `dir` sources by executing into a directory (SS-2), streaming and multi-item inputs reduce to `dir` sources by decoding their framing and spooling one file per item into a derived capture directory with a capture manifest. `stdin:` gains its handler through exactly that path, and a `file:` source whose `:framing` is not `:file-per-item` resolves through the same spool — one mechanism, no special cases, intake stays the single door.
The charset edge conditions flagged at capture time are resolved here by a scope law, not an assumption: framing is a byte-level concern; text decoding is not the codec's job (ruling 2).
This session adds real-pipe integration tests under `test-integration/`, which is on the ADR-0016 trigger list: T2 owed.
Read first

* `docs/source-sink-design.md` (framing axis, spool rule, §5 size-cap non-goal note, D8 record-keeping exemptions), SS-3 plan row
* `test/fixtures/v2/simhospital/messages.out` — PROBE IT (ruling 3) before writing the er7 grammar; `notes/ADRs.md` ADR-0011 (the corpus's provenance)
* `src/ehr_testing_tools/corpus/source_sink.clj` (`:framing` field), `source_sink_url.clj`, `generator_source.clj` (the resolve-to-dir precedent SS-3's spool mirrors), `intake.clj`, `sink_write.clj` (fail-if-exists discipline the spool reuses), `digest.clj`
* `cli.clj`/`help.clj` (intake wiring precedent from SS-2)

Author rulings

1. Codecs are pure and their laws are stated per codec, honestly. Namespace `corpus.framing`: for each codec, `decode` (bytes → seq of item byte-arrays) and `encode` (seq → bytes), no IO, no println, Result-valued on malformed input. Laws, property-tested:
   * `:mllp`, `:er7-multi`, `:ndjson` — byte-exact round-trip (encode ∘ decode = identity on valid input; decode ∘ encode = identity on item seqs).
   * `:bundle-entries` — entry-preserving, envelope-lossy: decode yields the entry resources; encode produces a canonical `collection` Bundle; the law is item-level identity, and the envelope loss (original Bundle id/type/fullUrl metadata) is stated in the docstring and the design doc, not discovered by a surprised user later.
   * `:file-per-item` — the identity framing, made explicit as the schema default.
2. Framing is bytes; charset is downstream. All byte-level codecs split on ASCII-stable delimiters (MLLP's 0x0B/0x1C 0x0D, MSH detection, 0x0A) and never decode payload text. The proof obligation: a test where an item payload containing non-UTF-8 bytes (a Latin-1 `ö`, say) survives `er7-multi` and `mllp` round-trips byte-identically. MSH-18's existence is noted in the framing ns docstring as the reason this law exists (the payload's declared charset is the parser tier's concern); no MSH-18 inspection is implemented this session.
3. The er7-multi grammar is probed, not assumed. Read the SimHospital fixture first: determine empirically how its 1,013 messages are delimited (line structure, MSH boundaries, trailing bytes), record the finding in one design-doc sentence, and write the grammar to match — MSH-start detection preferred over blank-line splitting if both fit, for robustness stated as such. The witness acceptance test: decode the fixture → exactly 1,013 items, every item starting `MSH`; encode → byte-identical to the original file. If the fixture's own structure defeats byte-exact re-encoding (trailing newline quirks and the like), that is a STOP-AND-REPORT finding about the law or the fixture, not something to normalize silently.
4. The spool is one mechanism with a cap and a manifest. `corpus.spool`: input byte-source + framing → derived capture dir (`target/spool/<content-or-time-derived name>` — reuse the fail-if-exists guard) with one file per item plus `capture-manifest.edn`: captured-at (wall-clock — extend D8's named record-keeping exemption list in the design doc), declared origin (`stdin` / the source path), framing, format, item count, and per-item sha256s. Default cap 1 GiB, `?max-bytes=` override; exceeding it is `:spool-cap-exceeded` with the partial spool deleted — a rejection, never a truncated corpus dressed as success (the UX-1 lesson, applied to streams). Result-valued throughout.
5. Two resolutions ride the spool. (a) `stdin:?format=…&framing=…` → read the stream, spool, yield the `dir:` Source. (b) A `file:` Source whose `:framing` ≠ `:file-per-item` resolves the same way. Hermetic tests inject an InputStream; the real-pipe test (`printf … | bin/ehr corpus intake 'stdin:?…' --out …`) is integration-tier. `dir:` sources remain `:file-per-item` only this session — a directory of multi-item files is a recorded OPEN item, not silently supported.
6. CLI and strips. Intake accepts the stdin spelling (SS-2's generator-URL wiring is the precedent); help text one mention; `make cli-doc`. One new use-cases strip — piping the SimHospital fixture (or a small excerpt) through stdin intake — verified for real; existing intake strips re-verified.
7. Scope fence. No sockets (nc is the transport, per the design); no MLLP stdout sink (SS-4); no MSH-18 parsing; no framing-aware mutation operators. Anything that wants to cross the fence gets a note in the plan, not an implementation.
8. Tiers: T0 per commit; T1 + T2 + `make coverage` before the final commit; wall times reported.

Steps
Step 1 — `:framing` explicit in the schemas
`:file-per-item` as stated default; parser accepts `framing` query param for the kinds that take it (red→green). Commit: `feat: framing is an explicit Source axis; :file-per-item the stated default (SS-3)`
Step 2 — Fixture probe, then the er7-multi codec
Ruling 3's probe (findings in the design doc), then the codec with the witness test and the charset-law test (ruling 2). Commit: `feat: er7-multi framing codec — grammar probed from the ADR-0011 fixture; 1013-message witness round-trips byte-exact (SS-3)`
Step 3 — ndjson and bundle-entries
Per ruling 1, laws as stated (envelope-lossiness documented). Commit: `feat: ndjson (byte-exact) and bundle-entries (entry-preserving, envelope-lossy) framing codecs (SS-3)`
Step 4 — mllp
Byte-exact round-trip + the charset-law test again over MLLP. Commit: `feat: mllp framing codec — envelope bytes only, transport stays nc (SS-3)`
Step 5 — The spool
Per ruling 4, hermetic. Commit: `feat: the spool — framed input to a dir-with-capture-manifest; 1GiB cap, :spool-cap-exceeded is a value (SS-3)`
Step 6 — stdin source + framed-file resolution
Per ruling 5; the OPEN item for multi-item dirs recorded. Commit: `feat: stdin: source and framed file: sources resolve through the spool — the second unification (SS-3)`
Step 7 — CLI, strip, docs
Per ruling 6; freshness gates. Commit: `docs: stdin-intake strip verified for real; cli regenerated; intake strips re-verified`
Step 8 — Tiers, plan row, archive, push (pause before push)
Per ruling 8; SS-3 row → Done with fence notes. Commit: `prompts: archive 2026-07-28 ss-3 build session`
Final report
The er7 grammar finding as probed; the witness result; the charset-law tests' payload bytes; spool cap behavior evidence; which resolutions rode the spool; the recorded OPEN item; strip verification; tier wall times + coverage; red→green evidence; deviations.
