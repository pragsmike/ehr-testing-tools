# 2026-08-04 — Wave H: pre-roll — the history phase lands, GMF parity arc COMPLETE

## Scope

Wave H (pre-roll) was ratified as the SOLE remaining GMF-coverage
wave once ADR-0041 declared parity, ordered LAST specifically so its
history-phase design would exercise against the complete walking
catalog (ADR-0039 AR-7). The driving prompt ratified six author
rulings (`notes/ADRs.md` ADR-0042, AR-1 through AR-6): phase-marked
events minted by one interpreter walk (AR-1), encounter-anchored
phase inheritance resolving the UTI straddle (AR-2), a config opt-in
identity-preserving gate (AR-3), the seed-777 retirement (AR-4), an
oracle identity + new-baseline obligation (AR-5), and — the one that
gated everything else — a mandatory reconciliation read of the
existing `:pre-horizon-facts` mechanism and `check.clj`'s own
`:clinical-content-only-when-admitted` invariant BEFORE any edit,
with an explicit STOP-AND-ESCALATE clause if AR-1/AR-2 didn't cleanly
subsume them. Five checkpoints: config + phase boundary, compile
filter + straddle inheritance, seed-777 retirement + wellness-fold
evidence, oracle, records. Executed in one pass, five commits.

## Red→green evidence

- **Step 0 (AR-6's reconciliation read).** Read `engine.clj`'s
  `:registered` decide method, `compile-trajectory`'s own dropped-
  types/fact-types split, and `check.clj:438`
  (`clinical-content-only-when-admitted`) before any edit. Found: the
  straddle bug lives in `compile-trajectory`'s own PER-EVENT
  `:pre-horizon` split — an Encounter that opens pre-horizon (dropped)
  but closes post-horizon (its own raw `:t` past `registration-t`)
  compiles a real, orphaned `:encounter-end`. Concluded AR-1/AR-2
  cleanly subsume the mechanism (no conflict, no escalation) —
  recorded in full in ADR-0042's own "AR-6's own reconciliation"
  section, not merely summarized.
- **Step 1 (`98f099b`).** `run-module` gains a purely additive
  `history?` arity; `mark-phase` mints `:phase` (`:history`/
  `:horizon`) via AR-2's own encounter-anchored inheritance, alongside
  the unchanged `:pre-horizon` boolean. `engine.clj` gains the
  `:history` config key (destructured, defaulted `false`, threaded to
  `init-world`). Red evidence: a new property test
  (`history-flag-off-is-byte-identical-to-the-pre-h-call`, 150 seeds)
  proving the gated-off arity is byte-identical to the pre-H one, both
  in output and in the absence of the `:phase` key entirely; a scripted
  test proving `:phase` marks agree with `:pre-horizon` for a
  non-straddling walk; a state-sharing test extending the pre-existing
  guard-across-the-boundary property to `:phase`.
- **Step 2 (`73bb26f`).** `compile-trajectory` gains a purely additive
  `history?` arity — false stays the EXACT legacy code path (same two
  clauses, same output); true is a single uniform drop by `:phase`, no
  bucketing. Red evidence: three unit tests against directly-
  constructed `:phase`-marked fixtures (uniform drop across every
  event type; a straddling encounter emits nothing and nothing
  orphaned; the post-straddle horizon encounter still compiles
  normally, `[:outpatient-visit :outpatient-visit-end]`). The strongest
  evidence: a NEW engine-level property test running 150 random seeds
  against a purpose-built module whose own Encounter is GUARANTEED to
  straddle (`:persona-config {:age-min 0 :age-max 0}` bounds the
  DOB-to-registration-t gap to 3–365 days via `persona.clj`'s own
  birth-year/month/day derivation; the module's own Encounter closes
  500 days after opening, comfortably past that window) — the full
  invariant catalog holds and no `:procedure` from inside the
  straddled encounter ever reaches ground truth, for EVERY seed tried,
  not a hand-picked one.
- **Step 3 (`9240db8`).** `vendored_uti_test.clj` drops seed 777 for
  the config-default ordinary seed (20260802) under `:history true`.
  FIRST RUN FAILED RED — a genuinely new finding, not the straddle
  this session anticipated: `medication-end-references-existing-order-
  and-follows-it-in-time` tripped, because a `:medication-end` firing
  OUTSIDE any encounter (after its own order's encounter already
  closed) doesn't inherit phase from anything and reads its own raw
  `:horizon`, orphaning a reference to a dropped `:medication-order`.
  Fixed by `history-phase?` — one `:references` hop past AR-2's own
  rule, judged a narrow, principled extension (same "no orphaned
  reference to something dropped" principle), not an AR-6 conflict
  (recorded in ADR-0042, not silently folded into AR-2's own text).
  Re-run GREEN. Also lands the ear-infections wellness-fold
  interpreter-layer proof (`a-pre-registration-wellness-tick-folds-
  state-without-riding-the-wire`): a real wellness-triggered encounter
  found via the file's own established well-mixed-seed search, its
  `:phase :history` confirmed, its own compiled step confirmed absent.
- **Step 4 (`6a587ff`).** `digest.clj` gains two history-mode batches.
  `bin/regression-oracle 537f954 6a587ff` (pre-session tip → this
  step's own commit): all NINE pre-existing roots IDENTICAL; the two
  new roots DIFFER as expected (baseline silently ignores `:history`,
  target genuinely gates on it) — confirmed by inspecting the full
  manifest diff, not merely trusting the script's own DIFFERS verdict.
  A nice independent check: at baseline, `ear-infections-history-
  engine`'s own digest EQUALS `ear-infections-engine`'s own baseline
  digest exactly (`:history true` silently inert, byte for byte).
- Full regression sweep across every namespace this session touched or
  could plausibly have perturbed (`gmf-horizon-test`, `gmf-interpreter-
  test`, `compile-trajectory-test`, `engine-test`, `run-test`,
  `vendored-ear-infections-test` ×2, `vendored-uti-test`,
  `vendored-tjr-test`): 0 failures, 0 errors throughout every
  checkpoint. `clojure -M:poly check` green before every push.
  `gitleaks git --staged -v` clean on every commit; every push
  verified against its own message file.

## Judgment calls and their ratification status

- **AR-6's own reconciliation conclusion (subsume, don't escalate).**
  Recorded verbatim in ADR-0042's own dedicated section — this is a
  pointer, not a repeat. Not escalated because the read found a clean
  fit: the legacy path is genuinely untouched code, and the new path's
  own uniform drop + encounter-anchored inheritance is a strict
  superset of what `check.clj`'s invariant needs.
- **The Step 3 finding (`:medication-end` etc. orphaning outside any
  encounter) implemented, not escalated.** Judged a narrow, mechanical
  extension of AR-2's own STATED principle ("no orphaned reference to
  something dropped"), not a new design surface requiring the
  author's own call — the fix is one function
  (`history-phase?`), reuses the SAME `referenced-event` back-edge
  `compile-trajectory` already resolves for citation purposes, and
  changes nothing about the ALREADY-ratified encounter-anchored rule.
  Not yet ratified by the author; flagged here for review, per the
  same disclosure discipline this project's ADRs already practice for
  live findings.
- **The two-arity design (interpreter mints `:phase` in ADDITION to
  `:pre-horizon`, never replacing it) was not explicitly specified by
  the ruling text** ("history-phase events are MINTED... with a
  `:phase :history` mark" doesn't say whether `:pre-horizon` survives).
  Chosen because the interpreter-layer oracle batches
  (`appendicitis`/`sore-throat`/`ear-infections`) digest the RAW
  `:trajectory` directly — renaming or removing `:pre-horizon` would
  have changed those digests even at `history?` false, breaking AR-5's
  own pure-identity bracket before it could even be tested. Confirmed
  correct by the oracle run itself (Step 4): those three roots'
  digests are untouched.
- **`persona-config {:age-min 0 :age-max 0}` as the guaranteed-straddle
  mechanism (Step 2's own property test)**, rather than a hand-picked
  seed against a real vendored closure. A deliberate choice to prove
  the MECHANISM property-generally before Step 3 proves it against a
  real closure with an ordinary seed — not requested verbatim by the
  prompt, but directly serves AR-2's own "assert the straddle
  resolves" spirit one step earlier and more rigorously (150 seeds,
  not one).

## Findings and HEAD landed

- **GMF PARITY ARC COMPLETE.** Wave H was the sole remaining wave
  (ADR-0039 AR-7's own re-ordering, ADR-0041 AR-4's own PARITY
  ACHIEVED declaration). No further GMF-coverage wave is scheduled;
  the parity plan's own Status header and H row both carry closing
  dated notes; the roadmap moves attention to the non-GMF fronts
  (census tool refinements, Wave G's own attachment deferral, the
  standing tooling/design backlog).
- **Carry-across (mid-stay-at-window-open emission) is the one named
  future this wave leaves open** — a straddling encounter yields NO
  in-window wire traffic for that patient, a disclosed v1 cost AR-2
  itself names. Moved to the roadmap's own Deferred section with its
  revisit trigger (a test scenario needing mid-stay-at-window-open
  realism).
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself.
- Commits, in order: `98f099b` (Step 1), `73bb26f` (Step 2), `9240db8`
  (Step 3), `6a587ff` (Step 4), and this commit (Step 5 — ADR-0042,
  the parity plan's own dated notes and Status-header amendment, the
  interpreter doc's own §3 IMPLEMENTED note, roadmap capture, this
  record and its paired prompt archive, both indexed).
- **Fence, explicit:** no carry-across implementation, no history-
  census, no backloaded emission in any form (ADR-0031 AR-3 stands,
  unchanged) — exactly the driving prompt's own Fences section. The
  Step 3 finding (above) is the one deviation from the prompt's own
  anticipated scope, disclosed at the point found, implemented per
  fix-forward-with-disclosure rather than escalated (judgment call
  recorded above, not yet author-ratified).
