# 2026-08-05 — Alignment fixes 3: sim-model's resources take their own name, and the nesting rule gets its gate

## Scope

Session prompt naming AR-F3-0 through AR-F3-4. Prior: alignment fixes 2
landed and was design-channel-verified (`2599355`,
`notes/adr/0051-alignment-fixes-2.md`). This session executed the ruled
S1 fix (register rows S1/C-1): renamed
`components/sim-model/resources/sim/` to `resources/sim-model/`
(history-preserving `git mv`, zero content bytes changed), repointed
`persona.clj`'s three `io/resource` path strings, and co-landed a new
gate (`ehrt.docs-tooling.resource-nesting-test`) enforcing that every
`components/*/resources` directory nests under exactly one directory
named for its own brick. First src-touching session of this arc — the
regression-oracle bracket was the session's own stated verdict
condition. Full account, rulings verbatim:
`notes/adr/0052-alignment-fixes-3.md`.

## Red→green evidence highlights

- **Resource-nesting gate, genuine red before any rename.** Written
  against the still-unrenamed tree, the gate failed exactly as
  predicted: `components/sim-model/resources top-level entries ("sim")
  -- expected exactly one directory named "sim-model"` (1 failure, 0
  errors, 11 assertions). Rename landed; re-run: 0 failures, 0 errors,
  11 assertions — the same file, same run, tree changed underneath it.
- `git diff --cached --stat -M`: all four moved resource files (`NOTICE`,
  `given-names.edn`, `places.edn`, `surnames.edn`) showed as pure
  renames, 0 lines changed — byte-identical content at new paths.
- Full suite (`clojure -M:poly test :all skip:integration`): 212
  `Test results:` lines at Step 0 baseline → 214 after the new gate
  namespace landed (+2, one namespace × two projects that include
  docs-tooling), 0 failures/errors at every checkpoint. `clojure -M:poly
  check`: OK throughout.
- `bin/regression-oracle 2599355 3f43a46`: all eleven vendored-root
  batches byte-identical — the session's own stated verdict condition,
  met. The demographics EDN content moved unchanged; only the resource
  path prefix `persona.clj` resolves through changed.

## Judgment calls and disclosures

- **Citation sweep found two hits, both dispositioned historical, zero
  edits made.** `notes/adr/0025-sim-split-s1-s2.md`'s own account of
  the 2026-08-02 S1 extraction, and
  `.agents/plans/2026-08-05-alignment-audit-findings.md`'s row F-2
  (describing NOTICE files read at audit time) both cite the pre-rename
  path — correctly, as a record of what was true when each was
  written. Kept per AR-F1-2's judgment rule; ADR-0025 additionally
  fenced from editing this session (its closure is recorded in
  ADR-0052, not written into the attic file itself).
- **The driving prompt's own file-path shorthand
  (`components/sim_model/...`, underscore) didn't resolve directly** —
  the real component directory uses a dash
  (`components/sim-model/...`); the underscore convention applies only
  to the Clojure source subpath beneath it. A typo in the prompt's own
  shorthand, not a premise mismatch — resolved immediately by `find`,
  and every substantive probe claim (call-site count, moving set,
  seven-dir census) matched exactly once the real path was used.
- No other deviation. The Step 0 census matched the prompt's own probe
  in every particular — three call sites, four-file moving set,
  sim-model the sole nonconformer among seven `components/*/resources`
  directories, none under `bases/*`.

## Findings and HEAD landed

No findings beyond the two disclosed items above, both immaterial to
execution. Commits, in order: `3f43a46` (Step 1, rename + gate +
sweep), tag `stable-20260805-alignment-fixes-2` (AR-F3-0, created and
pushed at `2599355`), and this session's own closing records commit
(Step 2).
