# 2026-07-29 — User-path errata: retired sim mechanism claims (sibling-checkout / subprocess)

## Scope

Autonomous session (R30, ADR-0007), single checkpoint. The user path
(root `docs/`) still described sim as a separate sibling project
reached over a subprocess — stale since ADR-0005 mounted it in-process
(`ehrt.tools.sim` calling `ehrt.sim.interface/run-command` directly).
The author had live-verified a fresh clone with no sibling checkout
running `bin/ehrt corpus intake 'sim:...'` successfully, refuting the
docs by execution. This session found and fixed the three contradicting
spots the prompt named, confirmed no others existed, and left every
historical/dev-path citation of the retired mechanism untouched.

## Red→green evidence highlights

Docs-only change; the proof is the suite staying green, not a
red→green cycle. `clojure -M:poly check`: `OK`. Full per-push lane
(`clojure -M:poly test :all skip:integration`) run twice — first
capture accidentally truncated by a `| tail -100` pipe (invalidating
its own exit code, since a piped command's exit status is the last
stage's, not `clojure`'s); re-run with output captured whole to
`/tmp/polytest-full.log` and the real exit code captured separately:
exit 0, 177 namespace summaries, 354 `0 failures, 0 errors` hits, zero
`FAIL`/`ERROR`/`Exception` anywhere in the complete, untruncated log
(9m20s wall clock). `make ci-parity` was started once, then aborted
deliberately mid-run — it clones from `.` (`HEAD`), and at that point
`HEAD` was still the *pre-edit* commit, so a ci-parity pass there would
have validated stale code, not this session's own changes; re-running
it post-push was judged redundant given the direct `poly check` +
`poly test :all` results already cover what it would add, and the
pre-push hook itself re-runs `poly check` regardless.

## Judgment calls and their ratification status

Autonomous session, no author present to ask — every call below is
this session's own, per the prompt's decision procedures, not
individually ratified yet:

- **Deletion over rewording** for the two stale `:commands :lines`
  comments in `use-cases.edn` (prompt's own stated default once no
  caveat is needed).
- **Equation rename** `sim-subprocess` → `sim-engine` in the same
  strip, per the prompt's own ruling (matches
  `docs/dev/source-sink-design.md` Part VIII's `EngineExecute`
  framing).
- **Link targets chosen for `simulate-your-facility.md`**: the MLLP-
  framing pointer goes to `docs/use-cases.md`'s "Mutate's own output,
  piped straight into intake" strip (the one user-path place actually
  demonstrating `?framing=mllp` end to end); the damage-injection
  pointer goes to `docs/operators.md` (the mutation-operator catalog)
  rather than `use-cases.md` — judged the more direct answer to "where
  is damage injected," not dictated verbatim by the prompt.
- **Anchor verified, not assumed**: the `use-cases.md` anchor was
  computed by the same GFM slug rule the doc's own existing anchors
  already use (cross-checked against `#generate-controlled-fault-data`,
  a working precedent in the same file), then confirmed against the
  actual generated heading text and re-verified with a small link-
  resolution script over both changed docs (ADR-0010's own "script the
  walk, don't eyeball it" discipline) — one pre-existing, unrelated
  false positive (a directory link, `-f` test doesn't match directories)
  found and dismissed, not silently ignored.
- **`docs/glossary.md:100`'s `ADR-0013/ADR-0015` citation left
  untouched**, judged unambiguous rather than merely "ambiguous, leave
  it": it names the legacy-floor/full-capability *baseline register*,
  not a sibling/subprocess mechanism claim, and doesn't say "sibling"
  or "subprocess" itself — the grep matched only on the ADR number.

## Findings and HEAD landed

Re-running the prompt's own inventory grep before and after the edit
found no hit beyond what the prompt already named — no scope growth,
one commit. HEAD after this session's ceremony: `7588eac` (`docs: user
path drops retired sim sibling/subprocess claims (ADR-0005; R34
sweep)`), pushed clean (pre-push hook: WSL provenance, gitleaks — no
leaks, 363 commits scanned — and `poly check`, all green).
