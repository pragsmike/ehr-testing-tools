# 2026-08-05 — Alignment fixes 2: the law reads the same everywhere, and three laws get teeth

## Scope

Session prompt naming AR-F2-0 through AR-F2-5. Prior: alignment fixes 1
landed and was design-channel-verified (`72add4a`,
`notes/adr/0050-alignment-fixes-1.md`), which correctly deferred its own
tag act rather than resolve a law-conflict ad hoc — `AGENTS.md`'s
standing tag-rule text had fallen out of sync with ADR-0049's AR-AU-0
the moment that ruling landed. This session: (1) reconciled
`AGENTS.md`'s tag rule with a dated note citing AR-AU-0 and this ADR's
own AR-F2-0; (2) executed both pending tag acts under the now-coherent
rule — verified `stable-20260805-alignment-audit` (already created,
apparently by the author directly, between sessions) and created
`stable-20260805-alignment-fixes-1`; (3) promoted three register-row S5/
A-5 prose invariants to gated tests: the sim-emit-hl7 dependency law,
the provenance leaf law, and root-alias completeness. Full account,
rulings verbatim: `notes/adr/0051-alignment-fixes-2.md`.

## Red→green evidence highlights

- **sim-emit-hl7 dependency gate.** A transient
  `[ehrt.corpus.interface :as corpus]` require added to
  `site_profile.clj` failed the new gate exactly as expected
  (`requires ehrt.* namespace(s) outside sim-model/its own component:
  ("ehrt.corpus.interface")`); reverted, tree confirmed clean, gate
  green again.
- **provenance leaf-law gate.** A transient `[ehrt.kernel.interface :as
  kernel]` require added to `manifest.clj` failed the same way
  (`requires ehrt.* namespace(s) outside provenance itself:
  ("ehrt.kernel.interface")`); reverted, tree confirmed clean.
- **Root-alias completeness gate.** Deleting the `poly/palgebra`
  `:dev` `:local/root` entry from the root `deps.edn` failed the gate
  (`components/bases directories missing from :dev's :local/root
  entries: #{"components/palgebra"}`); reverted, tree confirmed clean.
- `bin/regression-oracle 72add4a ab20b6f`: all eleven vendored-root
  batches byte-identical — expected, no `src/` logic changed at any
  point this session (only `AGENTS.md` prose, new `test/` files, and
  transient never-committed violations).
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  every checkpoint, 0 failures/0 errors, no `FAIL`/error markers in any
  captured log. `docs-tooling`'s test family: +12 deftests/+26
  assertions across the three new files.

## Judgment calls and disclosures

- **Tag-a premise mismatch, disclosed rather than silently
  absorbed.** The session's own read-first material (ADR-0050)
  described both tags as pending. Step 0 found
  `stable-20260805-alignment-audit` already live on origin at the exact
  ruled commit and message — consistent with "the author may always tag
  directly." Treated as a verify-only act for that tag, not re-created;
  recorded as a finding in ADR-0051 rather than assumed silently.
- **Register row S5's own file-count evidence ("13 files") didn't match
  this session's fresh count (14 pre-existing files).** Immaterial to
  the ruling's own instructions, disclosed in ADR-0051 rather than
  investigated further or silently corrected in the register itself.
- **AR-F2-3's provenance gate citation.** The session prompt's own
  narrative attributed provenance's malli-only `deps.edn` posture to
  register row B-9; the live register's B-9 is actually about a
  `valid?` naming collision, unrelated. The malli-only fact itself is
  true (confirmed directly against `components/provenance/deps.edn`),
  just not tied to that row id — the gate's own docstring cites AR-2/
  ADR-0043 as AR-F2-3 actually instructed, not the mismatched row.

## Findings and HEAD landed

No findings beyond the two disclosed premise/citation mismatches above,
both immaterial to the session's own execution. Commits, in order:
`3ee322f` (Step 1, AGENTS.md reconciliation), the two tag pushes
(`stable-20260805-alignment-fixes-1` created; `stable-20260805-alignment-audit`
verified pre-existing), `ab20b6f` (Step 2, three gates), and this
session's own closing records commit (Step 3).
