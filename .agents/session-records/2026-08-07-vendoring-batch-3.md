# 2026-08-07 — Vendoring batch 3: the families that travel — and the verbatim law gets teeth

## Scope

Session prompt naming AR-VB3-0 through AR-VB3-4, executing the
vendoring arc's fourth session — batch 3 of the design channel's own
curated plan, with a disclosed composition refinement, PLUS a rider
closing a real, pre-arc verbatim-law defect the batch-2 verification's
own full-table NOTICE re-hash surfaced. Full account, rulings, the
ADR-0071 correction, and the parity/oracle verification: `notes/ADRs.md`
ADR-0072.

Step 0 (preflight) confirmed the working directory is the ext4 clone,
tip `96424f8`, working tree clean. Baseline: `clojure -M:poly check`
OK; full suite green (`clojure -M:poly test`, every project 0
failures/0 errors); oracle pre-digest (direct `ehrt.oracle.digest/-main`
invocation via `:dev:test`) recorded for all twenty-three roots.
AR-VB3-0 executed directly: `stable-20260807-vendoring-batch-2` created
annotated at `96424f8`, pushed, verified — peeled ref resolves exactly.

Step 1 (`57ec4b7`, AR-VB3-R1) closed the verbatim-law defect. The
working tree's own on-disk `uti_recurrence.csv` (367 bytes, CRLF,
matching both upstream and its NOTICE row) turned out NOT to be what
the committed git blob actually held (357 bytes, LF-normalized by the
repo's own root `.gitattributes` rule) — `git status` had stayed clean
throughout because `text=auto`'s own CRLF-stripped comparison made the
two look identical despite differing raw bytes. Reproduced with a
genuine fresh materialization (`git worktree add`, and independently
`rm` + `git checkout-index -f`): a real checkout gets the WRONG bytes.
`ehrt.docs-tooling.notice-verbatim-test` written and run against that
genuinely-renormalized state: **RED**, the exact predicted mismatch.
Fix: `.gitattributes` gained `components/sim/resources/sim/modules/**
-text`; the correct bytes restored and staged. Re-materialization proof
repeated against the new staged blob: CRLF now survives a fresh
checkout. Gate re-run: **GREEN** (4 tests, 115 assertions). UTI
round-trip tests green (7 tests, 15 assertions). Full oracle re-digest:
zero movement among the 23 pre-existing roots, exactly as
`gmf.clj`'s own `split-lines` mechanism (splits `\r\n`/`\n` alike)
predicted.

Step 2 (`5d40d4e`, AR-VB3-1/2) vendored four of the five targeted
content-producers, module by module: `metabolic_syndrome_care.json`
(closure of 6, one shared member reused), `vhd_pulmonic.json`/
`vhd_tricuspid.json` (each a closure the census's own JSON-only metric
undercounts by two CSVs — the `heart/vhd_risks.json` submodule shared
between them, landed once), `med_rec.json` (single file, this batch's
own highest-volume module at [269 273 275] events). The fifth,
`colorectal_cancer.json`, FAILED its round-trip test: its own call into
the shared `anemia/anemia_sub.json` submodule sometimes lands outside
an open encounter, tripping the SAME `:encounter-end`/`:discharge`
invariant violations `anemia___unknown_etiology.json` already surfaced
in batch 2 (2 of 3 seeds tried rejected at 300 patients — 20260802 and
42, seed 1 clean). Per the standing fence, DEFERRED WHOLE — no NOTICE
row, no test, no oracle root. Thirteen new files landed across the four
modules; NOTICE gained thirteen rows plus a dated batch-3 section (the
four-landed/one-deferred narrative and the full `colorectal_cancer.
json` finding). `notice-verbatim-test` re-run against the updated
NOTICE: green, 141 assertions (69 total provenance rows). `clojure
-M:poly test` green across every project (0/0, confirmed by grepping
the entire run's own output); `clojure -M:poly check` OK.

Step 3 (`fc44369`, AR-VB3-2) added four new engine-layer roots to
`digest.clj` — purely additive. The official `bin/regression-oracle
5d40d4e fc44369 --declared-digest-change` bracket reported `DIFFERS` —
EXPECTED: the diff shows exactly four added lines and zero
changed/removed lines among the twenty-three pre-existing roots.

Step 4 (this record) authored `notes/adr/0072-vendoring-batch-3.md`
directly (including a dated correction of ADR-0071's own "56 rows, zero
problems" claim, append-don't-erase), appended its own index line to
`notes/ADRs.md`, corrected `notes/adr/README.md`'s own stale file count
(69→70, verified by `ls`), updated the roadmap's "Now" section (this
session's own close, successor tag debt named), added a dated note to
the `EncounterEnd` Deferred row naming `colorectal_cancer.json` as a
second blocked module, added a new "Next" backlog entry recording
AR-VB3-3's practitioner-UX direction verbatim, added the Done pointer
(`- 2026-08-07 — vendoring-batch-3 — ADR-0072`) in the same commit as
the index line, archived this prompt, and recorded this session.

## Red→green evidence highlights

The rider's own red was witnessed against a GENUINELY renormalized
working tree, not the long-lived session working tree's own bytes
(which turned out to already carry out-of-band-correct CRLF content,
never re-hashed against the git object store before this session) — a
plain `git checkout --`/`checkout-index -f` against the pre-fix commit
did NOT reproduce it (git's own stat-cache shortcut skipped the
rewrite when the working file already existed); `rm` first, then
`checkout-index -f`, forced the genuine rewrite and surfaced the real
357-byte LF bytes, matching what a fresh clone actually receives.

The four landed vendoring modules' own red was the same shape as
batch 2's: all four new tests required-and-failed
(`IllegalArgumentException: Cannot open <nil> as a Reader`) with their
own resource files moved aside as a batch, before any were restored.

`colorectal_cancer.json`'s own red never turned fully green — this
session's one genuine STOP-AND-REPORT, the load-clean sanity check
passed (the closure loads fine) but the engine-layer invariant check
failed, tracing to the SAME root cause as batch 2's own
`anemia___unknown_etiology.json` finding.

## Judgment calls and their ratification status

- **Composition refinement (AR-VB3-1)**: the concurred plan's own
  "pair/quartet" framing was read as five content-producers named
  across two families, not the families themselves — channel-inferred,
  disclosed in full in ADR-0072, licensed to escalate on author veto
  (none received).
- **`colorectal_cancer.json` bail-out verified across three seeds**
  (20260802, 1, 42), not one, before concluding it was the same genuine
  gap batch 2 found — the same population-scale diligence ADR-0070's
  `injuries.json` finding established. Channel-inferred, judged
  necessary for an honest STOP-AND-REPORT.
- **`vhd-pulmonic`/`vhd-tricuspid` test assertions narrowed to the real
  observed kind set** rather than the broader set most other vendored
  roots' tests assert on — found by direct `engine/run` + `frequencies`
  probing at the REPL before finalizing the assertion, disclosed rather
  than silently matching the template.
- **The RED witness for the rider used `rm` + `checkout-index -f`, not
  the first-tried `git checkout --`**, after the first attempt silently
  no-op'd (git's own stat-cache shortcut) — diagnosed by comparing
  `git show :path` (the index blob) against the working-tree bytes
  directly, judged necessary once the first RED attempt came back
  green when the math said it should not have.
- **Staging files before each `poly test` invocation mid-session** —
  the same operational workaround ADR-0070/0071 already named, repeated
  here.

## Verification block (for the record)

- `bin/regression-oracle 5d40d4e fc44369 --declared-digest-change`:
  `DIFFERS`, EXPECTED — four added roots, zero changed/removed among
  the twenty-three pre-existing ones.
- Full suite (`clojure -M:poly test`): green at the Step 0 baseline and
  again after every subsequent step, every project, 0/0 throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, every step: one delta each against
  the message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260807-vendoring-batch-2` peeled ref
  resolves to `96424f8` exactly.
- NOTICE hash cross-check: all thirteen new SHA-256 values re-derived
  by fresh `sha256sum` and matched; 69 total rows, all now re-verified
  by `notice-verbatim-test` itself, not merely a manual spot-check.
- `notice-verbatim-test`: witnessed RED against the genuinely
  fresh-materialized pre-fix tree, witnessed GREEN after the fix and
  again after the batch-3 NOTICE rows landed.
- Oracle zero-movement across the `uti_recurrence.csv` byte fix:
  confirmed both by manual pre/post digest diff and by the official
  harness, matching the `split-lines` mechanism's own prediction.

## Deviations, disclosed

Full account in `notes/adr/0072-vendoring-batch-3.md`'s own
"Deviations, disclosed" section and this session's own archived prompt
Deviation record: `colorectal_cancer.json` deferred whole; the
composition refinement (five content-producers, not the family
groupings); the `vhd-pulmonic`/`vhd-tricuspid` closure-file-count
undercount; the narrowed test assertions for both; the ADR-0071 "zero
problems" correction; `poly test`'s own change-detection gap.
