# 2026-08-07 — demos front door

Repo: `ehr-testing-tools`, WSL ext4 clone (`~/src/ehr-testing-tools`).
HEAD at prompt receipt: `721adb6` (vendoring batch 3, ADR-0072).
Session closed at `d0296d2` before this record's own commit lands.

## Prompt, verbatim

2026-08-07 — demos front door: the operator surface moves to the front — and the README shows it running

Session prompt (design channel, 2026-08-07; conventions and every claim below read at HEAD `721adb6` against a fresh public clone). Fifth session of the vendoring arc — a docs-relocation session, NOT a vendoring batch (batch 4 unruled, unscheduled). Executes the author's own direction recorded as ADR-0072's own AR-VB3-3 intake ("move the sim demos to a top-level demo place, and feature them in the intro materials") under the design-channel proposal the author ruled on three counts (design channel, 2026-08-07): structure = top-level `demos/` with `scenarios/` and `traces/` subdirectories; vacated paths get POINTER READMEs (the `CLAUDE.md`→`AGENTS.md` pattern); the sim-emit-hl7 `site-profiles` demo MOVES too. Doctrinal ground: two-voices-two-homes (`.agents/rulings.md`, from ADR-0062) — demos are operator-facing product surface; the component-local docs tree is a maintainer home. Governing discipline: promotion moves equipment, it does not improve it (`.agents/rulings.md`, AR-P-4) — files move verbatim; the ONLY in-transit edits are path citations inside the moved files' own fences and cross-references, each a mechanical consequence of the move. CRITICAL, probe-backed: `.gitattributes` protects `components/sim/docs/demos/**/messages*.txt -text` — the captured ER7 transcripts carry literal `\r` separators and are byte-precious; the pattern MUST move to the new path IN THE SAME COMMIT as the files, or a later checkout normalizes them — the exact hazard class AR-VB3-R1 just spent a rider closing, on this repo's own written warning in that very file. R30 ceremony.

Author rulings (AR-DM-0 through AR-DM-3), Steps 0-3, Fences, and Appendix A/B (the verbatim `demos/README.md` content and the top-level README's "See it run" section) — full text recorded verbatim in `notes/adr/0073-demos-front-door.md`'s own Decision section rather than duplicated here a second time.

## Deviation record

Every deviation from this prompt's own literal text, disclosed and
reasoned: `notes/adr/0073-demos-front-door.md`'s own "Deviations,
disclosed" section. Summary: the "See it run" fence landed as
`` ```bash `` rather than the appendix's own `` ```sh `` (a real
gate collision, `ehrt.docs-tooling.quickstart-fresh-test`, caught red
and fixed forward); a stale prior-session `out/corpus/busy-tuesday`
directory was removed before the live probe; two moved READMEs'
cross-reference prose was rewritten beyond a single path token where
the move made the surrounding sentence itself false, not merely
stale-pathed.
