# 2026-08-08 — Straddle fix: the legacy gate learns the span

## Scope

Session prompt naming AR-SF-0 through AR-SF-7, executing the ruled fix
for ADR-0085's own diagnosis (the colorectal investigation): the
straddling-encounter compile-layer defect in `ehrt.sim-trajectory.
compile-trajectory`'s own legacy `:pre-horizon` drop gate. The author
ruled shape (b) 2026-08-08, design channel, verbatim: "Accept
recommendation: (b) now, (a) recorded." This session generalizes the
Wave H `history-phase?` back-reference principle to the legacy path (a
`straddle-open?` fold state, the compile-time mirror of `mark-phase`'s
own `open-phase`), lands it under the full blast-radius protocol
(ADR-0082's own precedent), records shape (a) as the carry-across row's
own compile-layer half, and closes the colorectal Deferred row.

Preflight: working directory confirmed the ext4 clone, HEAD `b81b847`
exactly (the colorectal investigation, ADR-0085), branch up to date
with `origin/main`, working tree clean. `clojure -M:poly check`: OK.
Oracle pre-digest (`bin/regression-oracle b81b847 b81b847`): all 28
roots IDENTICAL, byte-for-byte, both sides the same commit. Last five
CI runs on `main` all `success`: `31269790361` (`b81b847`),
`31269357505`, `31266927895`, `31266367045`, `31263297709` — no red
window.

## Step 0 — Tag (AR-SF-0)

`stable-20260808-colorectal-investigation` did not already exist
locally or on the remote. Created annotated at `b81b847`, pushed. No
commit this step, per the prompt.

## Step 1 — Blast radius + scope probes (AR-SF-2, AR-SF-4)

Classified the 28 oracle roots by reading `digest.clj`'s own `roots`
map and `engine.clj`'s own `:registered` decide method: 3 not-in-path
(interpreter-batch roots, `compile-trajectory` never reached), 3
in-path `history? true` (gated identical by construction — the fix only
touches the legacy branch), 22 in-path legacy. A read-only probe
(`with-redefs` on `ehrt.sim-trajectory.interface/run-module`, zero
working-tree disturbance, each root's own private producer fn called
via `#'ehrt.oracle.digest/<root>-pair` to guarantee exact seed/
population parity with the oracle's own definition) scanned every
legacy root's own raw trajectories for straddling spans.

**One real mover found: `sleep-apnea`, 3 of 300 walks (#17, #58,
#269)**, identical shape all three — a `:wellness` encounter opening
just before the registration horizon, minting a pre-horizon `Sleep_
Apnea_Assessment` procedure, closing just after. Every other legacy
root: 0 spans, predicted identical.

**AR-SF-4 (history-mode scope probe): SOUND, no gap.** Traced `mark-
phase` (`gmf_interpreter.clj` ~2032-2043): a straddling encounter's
`open-phase` seeds from the opening's own raw phase and every
subsequent event — including the `:encounter-end` — inherits it
regardless of its own raw timestamp, dropping the whole span uniformly
under `history-phase?` already. Empirical probe (all three `history?
true` roots, including `urinary-tract-infections-history-engine`'s own
real straddling span): 0 unsound walks, no counterexample. Fix stays
legacy-path-only.

**Session STOPPED here, before any fix code, per AR-SF-2's own bar.**
The prediction table and the mover's walk-level evidence were reported
to the design channel. An independent sandbox verification re-derived
the tag/HEAD, the 28-root classification (correcting its own earlier
partial miscount of `attention-deficit-disorder` as legacy), the
`:supply-list` claim, the AR-SF-4 structural trace, and the vendored
`sleep_apnea.json` source's own shape — disclosing that the walk-level
3-of-300 count itself could not be re-run in that sandbox (no Maven
Central access). The author licensed `sleep-apnea` as the SOLE named
mover, with four binding terms (the bracket must match exactly; the
record carries walk-level evidence both sides plus independently
hand-verified digests; the record states explicitly that the new digest
is more correct, not merely different; the latent-defect finding is
recorded as intake, not acted). Full license text, verbatim: `notes/
adr/0086-straddle-fix.md`.

## Step 2 — The fix (AR-SF-1/3/7), commit `e2cef25`

**Red, witnessed in-session:** six new unit tests against the unfixed
tree (`ehrt.sim-trajectory.compile-trajectory-test`) — 7 failures (0
errors), exactly the new assertions.

**The fix:** a `straddle-open?` fold state (mirroring `mark-phase`'s
own `open-phase`) opens on a raw-pre-horizon `:encounter` drop; every
subsequent event, regardless of its own raw `:pre-horizon`, receives
the EXISTING pre-horizon disposition until the matching `:encounter-
end` closes the span. In-span attribution: an open-straddle interval
(not the `:references` back-edge), since most in-span event types carry
no back-reference to the encounter at all — the session's own design
work within the arm, stated and justified in ADR-0086.
`:suppressed-straddle-spans` (AR-SF-7) lands as a purely additive
return-map key, spans not events, every caller confirmed `:keys`-
selective first.

**Green:** `Ran 36 tests containing 70 assertions. 0 failures, 0
errors.` (63→70, +7 this session's own coverage).

**Colorectal acceptance (AR-SF-3):** `check/check-all`, 300 patients,
pin-verified checkout (`7e08387c68a7f0e21d13076609a159fd473fc902`):
`:status :ok` (0 violations) at all three seeds (20260802, 1, 42) —
fully extinguished, from 4/0/4 pre-fix.

**Full suite:** `clojure -M:poly test :all skip:integration`, every
project block 0 failures/0 errors; the disclosed `mutate-stdout-stdin-
real-loopback-test` flake did not fire.

**Oracle bracket:** `bin/regression-oracle b81b847 e2cef25
--declared-digest-change` — manifest diff confined to exactly one line,
`sleep-apnea.edn`: `a68c4fb7...` → `271df527...`, all 27 other roots
byte-identical. Matches the license's own bar exactly.

**Walk-level evidence, both sides (license term 2):** a disposable `git
worktree` at `b81b847` compared against the post-fix tree, intercepting
`compile-trajectory` itself for per-patient compiled `:steps`. Exactly
3 of 300 walks differ — #17, #58, #269, the SAME three predicted.
Pre-fix: a dangling `:outpatient-visit-end` with no preceding admission
(the defect). Post-fix: the straddling span drops in full, and the
loop finds the genuinely later, fully-in-horizon wellness encounter
(~353 days later) and compiles it normally — a real, complete encounter
pair replaces the phantom. Hand-verified independently of the harness
(`sleep-apnea-pair` called directly, `pr-str`'d, `sha256sum`'d outside
`bin/regression-oracle`): matches the oracle's own reported digest
exactly.

`git diff --cached --stat` reviewed before staging: exactly the two
intended files (`compile_trajectory.clj`, `compile_trajectory_test.
clj`). `gitleaks git --staged -v`: clean. Committed `e2cef25` ("fix:
straddling encounters drop whole — the legacy gate learns the span
(straddle fix, AR-SF-1/2/3)"). Pushed; post-push verification: one
delta, the known trailing-blank-line artifact. CI watched to
conclusion: run `31273576426`, `success`, 3m24s.

## Step 3 — Record (`b1a7a7a`)

`notes/adr/0086-straddle-fix.md` authored in full (the prediction
table, the STOP-AND-REPORT and license verbatim, both walk-level
evidence tables, the acceptance runs, the history-mode verdict, the
attribution rule chosen and justified, the AR-SF-7 disposition, the
intake finding). AR-SF-6 erratum appended to `notes/adr/0082-
encounterend-fix.md` (append-don't-erase), correcting the self-
contradicting seed-42 prose figure — verified, before writing, that the
archived colorectal-investigation prompt itself also propagated the
disputed figure (noted, not edited, per the frozen-provenance law).
`notes/ADRs.md` gained its index line; `notes/adr/README.md`'s file
count corrected 83→84 (`ls`, not arithmetic). Roadmap: the colorectal
Deferred row gained its own dated closure note ("this row CLOSED — see
Done, below") with its full prior text intact, never substituted
(AR-A-5); the carry-across row gained its AR-SF-5 dated note (shape (a)
recorded, row stays deferred); a Next-section intake row for the
colorectal vendoring payoff session; the Done pointer (`- 2026-08-08 —
straddle-fix — ADR-0086`).

`clojure -M:poly check`: OK. Targeted docs-tooling lint runs (`roadmap-
deferred-closure-lint-test`, `done-pointer-adr-test`, `index-
completeness-test`, `stale-path-test`, `structure-currency-test`,
`reading-set-budget-test`): all green before staging. Full suite run
twice before committing: both clean, 0 failures/0 errors, 521
assertions, no drift, the loopback flake did not fire either run.

`git diff --cached --stat` reviewed: the five intended files. `gitleaks
git --staged -v`: clean. Committed `b1a7a7a` ("docs: the straddle fix
recorded — colorectal's row closes, carry-across gains its compile-
layer half (ADR-0086)"). Pushed; post-push verification: one delta, the
known trailing-blank-line artifact. CI watched to conclusion: run
`31274259259`, `success`, 3m17s.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-straddle-fix.md` (the
driving prompt, archived verbatim) land together, indexed in both
READMEs' own entry lists.

## Successor tag debt

Recorded in `notes/adr/0086-straddle-fix.md`: the next session that
opens fresh work tags `stable-20260808-straddle-fix` at this session's
own closing tip.

## Judgment calls and their ratification status

- **In-span attribution rule (an open-straddle interval, not the
  `:references` back-edge)** — the session's own design work within
  the AR-SF-1 arm, licensed explicitly ("state the chosen rule and its
  evidence in ADR-0086"). Stated and justified there; not separately
  re-ratified, per that ruling's own terms.
- **`sleep-apnea`'s post-fix digest as the new standing baseline** —
  explicitly required and ratified by license term 3, stated in ADR-
  0086 with the correctness argument (a wire-impossible dangling
  terminal replaced by either nothing or a real, complete encounter
  pair).
- **The AR-SF-7 additive-key friction test** — the session's own
  determination (every caller `:keys`-selective, confirmed by grep
  before landing the key), matching the ruling's own explicit bar; no
  friction found, so the key landed as licensed, not escalated.

## Findings, disclosed not acted

- **The latent defect itself:** `sleep-apnea.json`'s vendored oracle
  baseline carried a malformed compiled shape (a dangling `:outpatient-
  visit-end` with no matching admission) since vendoring batch 1
  (ADR-0070) — invisible to byte-identity oracle checks because no
  oracle root runs `check/check-all`'s own invariant catalog. Named for
  review 2 and the pairing-as-data adequacy conversation, per the
  license's own term 4.
- **The incidental full-sweep result:** the other 27 oracle roots are
  now confirmed straddle-free — the first time any of the 28 roots'
  own straddle-freedom was actually checked, a stronger audit than any
  root has previously received.

## HEAD landed

`b1a7a7a` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).
