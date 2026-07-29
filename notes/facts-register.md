# Facts Register — ehr-testing (workspace)

<!-- Tracks load-bearing, externally verifiable assertions made anywhere in
     this workspace (docs, deps.edn, memory files) about tools, licenses,
     and ecosystem capabilities -- evidence and a last-verified date, so a
     claim can be checked instead of trusted. See AUTHORS-GUIDE.md §4 for
     the assert -> register -> date discipline this file exists to
     support. Instantiated 2026-07-28 (discipline-parity session, R25);
     see notes/discipline-parity-audit.md row M5/M5a for why this file
     didn't exist before this session and why the Index sub-table below
     is tools' own addition, adopted on top of sim's base F-row shape. -->

Fresh sequence, starting at F1 — **not** a continuation of
`notes/sim/facts-register.md`'s or `notes/tools/facts-register.md`'s own
numbering (those are frozen provenance, moved intact, F-rows not
migrated forward automatically per each file's own header). A
workspace-level claim that repeats one of theirs needs its own fresh row
here, re-verified — unless explicitly carried forward with an origin
citation, as F1 below does.

## Index

Hand-maintained, not enforced by a lint or CI check (same choice as
`notes/ADRs.md`'s own lack of a table of contents check). Update this
table in the same commit as any new F-row — one line per row: an
extractive digest (a clause already present in the full row below, not a
new characterization), the last-verified date, and the status column
verbatim.

| # | Claim digest | Last verified | Status |
|---|---|---|---|
| F1 | Sim's manifest generator identity is `"ehrt.sim"` (not `"ehr-testing-sim"`) since ADR-0001's rename; a stale test asserting the old identity went unexecuted until ADR-0005 first ran the path for real | 2026-07-28 | verified (carried forward, origin `notes/ADRs.md` ADR-0005) |
| F2 | `bases/sim-cli` (and its composing `projects/sim`) are DEPRECATED as of 2026-07-29 (R33, ADR-0009): kept working, own tests keep running, but the user path never mentions them and `AGENTS.md` marks the dev path deprecated. Retirement trigger (dated, not scheduled): retire when a future review finds no use of either outside their own test suites | 2026-07-29 | verified (this session's own ruling; re-check the trigger condition, not this claim, at that future review) |
| F3 | `SETUP.md`'s on-ramp (fresh clone → generated + judged corpus) re-validated 2026-07-29 by a fresh-context agent walk: no prior repo knowledge, started from the rendered root README alone. Reached a generated corpus, gated it (`gate v2` 5/5 pass, `gate fhir` correctly rejected a mutant), and checked it (7/7 pass) in 15 commands, one recovery step included. Four friction points found and fixed in `SETUP.md`'s Troubleshooting section the same session | `SETUP.md` §4 | This session's own fresh-agent walk (transcript: this session's own record). Bar cited: `notes/tools/prompts/2026-07-24-onboarding-wave.md`'s Step 4 self-audit acceptance test — the walk's own verbatim success-report text is not preserved anywhere in git history (tools-era sessions predate this workspace's own session-record discipline, `notes/discipline-parity-audit.md` row M13); cited from provenance instead, per that prompt's own acceptance criteria and the commits it produced (`dd6ba2d`, `09eb094`, `f4e5c76`) | 2026-07-29 | verified (this session's own walk; the *old* report's exact text is a named gap, not silently assumed) |

## Register

| # | Claim | Where asserted | Evidence | Last verified | Status |
|---|---|---|---|---|---|
| F1 | `components/sim/src/ehrt/sim/manifest.clj`'s generator identity has reported `"ehrt.sim"` since ADR-0001's own mechanical rename (2026-07-28) — not `"ehr-testing-sim"`, sim's pre-rename self-identity. This is a worked example for why this register exists: `projects/conformance/test/ehrt/tools/sim_manifest_contract_test.clj` asserted the *old* string, but that test path had never actually executed end to end before ADR-0005's in-process `ehr sim` mount — always skipped, local and CI both, for lack of a sibling checkout at the moments it previously ran. The mount is what first let it run for real, and it caught its own staleness on the first real run. Fixed (test corrected to `"ehrt.sim"`, not left red — AUTHORS-GUIDE.md's two-failure-modes discipline: the check misencoded its own invariant, so the check was the thing to fix, not reality). | `components/sim/src/ehrt/sim/manifest.clj:77`, `projects/conformance/test/ehrt/tools/sim_manifest_contract_test.clj` | `notes/ADRs.md` ADR-0005's own deviation record ("A genuine, previously-latent finding, surfaced by the mount, not caused by it") | 2026-07-28 | verified (carried forward from ADR-0005 at register instantiation, discipline-parity session) |
| F2 | `bases/sim-cli`/`projects/sim` deprecation (R33, ADR-0009, 2026-07-29): the CLI rename session's own author ruling marked sim's standalone CLI deprecated rather than removed, on the reasoning that `bin/ehrt sim run` (the in-process mount, ADR-0005) is now the presented surface for every user-facing doc, while `bases/sim-cli` stays buildable and tested for whoever still depends on the standalone artifact directly. Retirement trigger, named here so it isn't lost to prose: **retire when a review finds no use outside its own tests.** Not scheduled — this row is the pointer a future review checks, not a countdown. | `AGENTS.md` ("Landed so far"), `notes/ADRs.md` ADR-0009 | This session's own ruling (`notes/prompts/2026-07-29-ehr-testing-development-resumption.md`, R33) | 2026-07-29 | verified (a ruling, not an external fact — "verified" means the ruling is accurately transcribed here, not that the trigger has fired) |
| F3 | **The SETUP on-ramp claim: a fresh clone with no prior knowledge of this repo, starting only from the rendered root `README.md`, can reach a generated and judged corpus.** Re-validated 2026-07-29 (pre-takeover storefront-polish session, R40/R41): a fresh-context agent (no repo knowledge, no session memory, handed only the rendered `README.md` text and the instruction "get to a generated, judged corpus on this machine") worked the Quickstart end to end — `corpus generate` (real Synthea run), `corpus mutate`, `gate v2` (5/5 pass), `gate fhir` (correctly rejected the mutant, exit 1), `check` (7/7 pass) — in 15 commands including one self-recovered failure (a stale `target/corpus/synthea-s1-p5` directory from prior dev-machine activity, not a fresh-clone condition). R40's bar: a prior documented success report from a domain expert (a Python developer), against `ehr-testing-tools` pre-merge. That report's own verbatim text could not be located in git history — this workspace's `.agents/session-records/` discipline (`notes/discipline-parity-audit.md` M13) is sim-only in origin; tools-era sessions (including `notes/tools/prompts/2026-07-24-onboarding-wave.md`, the session that first built `SETUP.md` for exactly this trial audience) never committed a session report, only the prompt and the resulting commits. Cited from that provenance instead of the report itself, per AGENTS.md's "record it and ask" discipline (autonomous session: recorded, not blocked on). That prompt's own Step 4 acceptance test (five questions, each answerable with a file+section citation from a fresh clone) is the closest live equivalent of R40's bar, and this session's own walk clears a stricter version of it (real command execution, not a citation exercise). Four friction points the walk surfaced were fixed the same session: `corpus generate`'s `:out-dir-exists` rejection was undocumented; a harmless `run!` namespace-shadow warning leaks onto stdout; `gate ... --report` still dumps the full result to stdout alongside writing the file; `poly test :all`'s multi-minute runtime was unstated. All four are now `SETUP.md` §4 Troubleshooting entries. | `SETUP.md` §4, `README.md`'s Quickstart (the `corpus generate` rejection-behavior comment) | This session's own fresh-context agent walk (full transcript in this session's own record); `notes/tools/prompts/2026-07-24-onboarding-wave.md` Step 4 (bar cited from provenance); commits `dd6ba2d`/`09eb094`/`f4e5c76` (what that prompt actually produced) | 2026-07-29 | verified (this session's own walk is directly verified; the *prior* domain-expert report is a named provenance gap, not assumed to exist) |
