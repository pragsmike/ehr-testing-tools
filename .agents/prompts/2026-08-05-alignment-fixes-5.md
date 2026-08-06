# 2026-08-05 — Alignment fixes 5

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `05d6ed1` at session start (`notes/adr/0053-alignment-fixes-4.md`,
alignment fixes 4, design-channel-verified). Session record:
[`2026-08-05-alignment-fixes-5.md`](../session-records/2026-08-05-alignment-fixes-5.md).
Decision-of-record: `notes/adr/0054-alignment-fixes-5.md`.

## Prompt, verbatim

2026-08-05 — alignment fixes 5: the license text travels with the content — F-4 closes, gated
Session prompt (design channel, 2026-08-05). Prior: alignment fixes 4 landed and was design-channel-verified (`05d6ed1`). This session closes the arc's last fix cluster: register rows F-2/F-3/F-4 — of the four Apache-2.0-sourced vendored roots, only `components/corpus/test-fixtures/v2/simhospital/` carries the actual license TEXT; the other three rely on NOTICE narrative alone, and Apache-2.0 §4(a) expects the text to travel with redistributed content. The author ruled the register's option (a): ONE shared license-text file, cross-referenced from the three NOTICEs. Pre-tag is the cheap moment. Small session, two commits. R30 ceremony. Read-first: register rows F-1 through F-4 verbatim; the three target NOTICE files; `components/corpus/test-fixtures/v2/simhospital/LICENSE` (the in-repo copy source).
Precision that governs scope: the three roots NEEDING the cross-ref are (1) Synthea GMF modules — `components/sim/resources/sim/modules/NOTICE`, (2) CDC NIST validator fixtures — `components/corpus/test-fixtures/v2-nist/NOTICE.md`, (3) Synthea-derived `wellness-cadence.edn` — `components/sim-trajectory/resources/sim-trajectory/NOTICE`. The demographics NOTICE is NOT a target (F-2: hand-curated original content, no Apache obligation) and `components/sim/NOTICE` is touched only if inspection shows it too asserts Apache-2.0 sourcing without a text pointer (disclose either way).
Author rulings (record verbatim in ADR-0054)

1. AR-F5-0 (tag). Annotated tag `stable-20260805-alignment-fixes-4` at `05d6ed1`, message `alignment fixes 4 landed, design-channel-verified 2026-08-05 (ADR-0053)`; push; verify on origin.
2. AR-F5-1 (the shared text). `LICENSES/Apache-2.0.txt` at repo root (REUSE-style layout — extensible if other third-party licenses ever arrive). Content: byte-copy of `v2/simhospital/LICENSE` AFTER verifying that file is the unmodified canonical Apache-2.0 text (inspect: standard preamble, §§1–9, unfilled appendix or Google's standard-form appendix — if it carries source-specific modifications beyond the appendix, STOP-AND-REPORT; the canonical text must come from a clean source, and which source needs a ruling, not improvisation). The repo's own root `LICENSE` (MIT) is NOT edited — the two grants are siblings, not competitors; a one-line "third-party license texts live in LICENSES/" note MAY be added to the README's license section if one exists (disclose placement or absence).
3. AR-F5-2 (three cross-refs, dated). Each target NOTICE gains a short dated line: the complete Apache License 2.0 text is vendored at `LICENSES/Apache-2.0.txt`, added 2026-08-05 per ADR-0054 (register F-4). NOTICE narrative is otherwise UNTOUCHED — these files carry provenance claims (what was extracted, from where, under what) that are evidence artifacts; the cross-ref is an append, not an edit.
4. AR-F5-3 (the gate, co-landed). New deftest in the `docs-tooling` gate family: (a) `LICENSES/Apache-2.0.txt` exists and is byte-identical to `v2/simhospital/LICENSE`; (b) every NOTICE file in the tree whose text cites the Apache license ALSO contains the string `LICENSES/Apache-2.0.txt` — so a future vendored root that writes an Apache-citing NOTICE without the pointer trips the gate. Exclusion semantics: NOTICEs citing no license (pure-provenance notes) are out of scope; encode by content-test ("cites Apache"), not by path allowlist. Red→green witnessed: run the gate BEFORE the cross-refs land — the three targets must trip it (genuine red, like the nesting gate's); then gate + text + cross-refs land in ONE commit, green.
5. AR-F5-4 (determinism). The oracle bracket (`05d6ed1` → tip) must show all ELEVEN batches identical. The NOTICE inside `sim/modules/` sits beside the digested module inputs — this session touches NO `.json`/`.edn`/`.csv` content anywhere, and the oracle is the proof. Any digest change: STOP-AND-ESCALATE, nothing licensed.

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `05d6ed1` or later-with-disclosure; verify the simhospital LICENSE is canonical per AR-F5-1; inspect `components/sim/NOTICE` for the disclose-either-way call; full suite green baseline; oracle pre-digest. Execute AR-F5-0.
Step 1 — Red, then text + cross-refs + gate (AR-F5-1/2/3). Gate written; run against the unmodified tree; capture the red (three trips). Land `LICENSES/Apache-2.0.txt`, the three (or four, disclosed) NOTICE appends, the optional README line, and the gate in ONE commit, green; full suite green; `poly check` OK. Commit: `docs: the license text travels with the content — three NOTICEs point home, gated (alignment fixes 5, AR-F5-1/2/3)`
Step 2 — ADR-0054 + record. ADR-0054: rulings verbatim; the canonicality verification evidence; the red transcript; the sim/NOTICE disposition; F-2/F-3/F-4 closure narrative. Index line; Done pointer `- 2026-08-05 — alignment-fixes-5 — ADR-0054`. Oracle bracket per AR-F5-4. Session record + prompt self-archive. Final commit: `docs: alignment fixes 5 record — the last fix cluster closes (ADR-0054)`
Fences
No `src/` changes at all. No content edits to any digested resource (`.json`/`.edn`/`.csv`) — NOTICE appends and the new LICENSES file only. NOTICE narrative bodies untouched (appends only, AR-F5-2). Root `LICENSE` (MIT) untouched. No gate weakening. Frozen archives untouched (ADR-0054 + index sanctioned). This is the arc's LAST fix session — anything new found is a register-style note in ADR-0054 for the arc close, never an act.
After landing: design channel verifies by fresh probe, then the ARC CLOSE session follows — the three pending rulings-register appends (AR-F1-6's two + the law-surface lesson with both instances), `state.md` regeneration per AR-C-1, the first `libs :outdated` report under the new A-3 cadence, reading-set budget re-derivation per AR-D-3 if owed, and the arc's final tags.

## Notable deviations, disclosed

- **Two precision gaps between the prompt's stated premise and the
  live tree, surfaced at Step 0 preflight, put to the author rather
  than guessed.** `sim-trajectory/NOTICE` (named target 3) cites no
  "Apache" text at all as written, so a content-test gate cannot flag
  it pre-fix the way the other two named targets can; `demographics/
  NOTICE` (explicitly out of scope, F-2) contains the literal string
  "Apache-2.0" as background context, which a naive substring gate
  would false-positive on forever. Both resolved by direct question
  before any code was written — recorded verbatim as AR-F5-2a/AR-F5-3a
  in `notes/adr/0054-alignment-fixes-5.md`'s own Decision section, not
  buried in a findings appendix.
- **`components/sim/NOTICE` confirmed and treated as the disclosed
  fourth target**, per the prompt's own "disclose either way" clause —
  inspection found it does assert Apache-2.0 governance without a
  pointer.
- **AR-F5-0's tag was executed by the author, not the session.** Tag
  creation/push is AUTHOR ACTION per the build-session skill (ADR-0003's
  own trust boundary), regardless of ceremony mode — the session
  prepared the exact command, the author ran it, the session verified
  the result on origin afterward (peeled ref resolves to `05d6ed1`,
  message matches verbatim).
- No other deviation. The gate landed in one commit alongside the text
  and cross-refs, per AR-F5-3's own "land in ONE commit, green"
  instruction; the red was captured as a separate, prior run against
  the unmodified tree, not committed.
