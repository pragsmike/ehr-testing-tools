# 2026-09-01 — application-path unification, stage 2: enabling the omitted pairs

Prompt archived at `.agents/prompts/2026-09-01-apply-unification-stage-2.md`.
Serves `roadmap.md#engine-namespace-extraction-and-apply-unification` (P5),
now solely this arc. Ceremony mode: R30 standing default (commit and push at
checkpoints, unattended) — the prompt states no prepare-only mode.

Twenty-two commits on `main`, parent `e9c01b1`, pushed in three spans:

| # | sha | what |
|---|---|---|
| — | `f84005a` | docs: C13 and C14 ruled and closed |
| — | `c4f6ddd` | refactor: site 3's projection becomes a literal — the DE-ALIAS |
| 1 | `649403e` | 1 × `:patient-bootstrap` |
| 2 | `e05da9c` | 1 × `:replay-entries` — **span 1 tip** |
| 3–10 | `0d0db6e` `2505a68` `396e047` `59605ed` `7658e82` `d771829` `15ea306` `b547f8f` | site 2's eight — **span 2 tip** |
| 11–19 | `00373db` `f34c423` `1aa5796` `ee2b01e` `162080a` `5f11ee7` `bc34aba` `ae93afe` `3abfa44` | site 3's nine — **span 3 tip** |
| 20 | (this record) | docs: stage-2 records |

## 0. Preflight

Both clone roots resolved. `~/src/ehr-testing-tools` (ext4, the clone of
record) was at `e9c01b1`, the sha the prompt names; every edit resolved under
it. `/mnt/c` was at `537f954` — EXPECTED and correct since `bin/sync-mnt-c`
was deleted at `e7646b5`. Nothing was written there.

**The checklist was re-verified against the P5 row rather than trusted.** The
census's section 3 and the row's own "STAGE 2's PAIR CHECKLIST" paragraph
agree exactly: 22 omitted pairs, 3 OUTPUT-MOVING (2 × A1, 2 × A2, 3 × A1, all
DECORATIONS), 19 INERT. Census order is section 3a → 3b → 3c, and the spans
follow it.

## 1. C13 and C14 — the stage-1 premise mismatch, resolved

Stage 1's §6 disclosed that no `C14` existed anywhere in the tree and left
both items where the repoint pass put them. The prompt carries the rulings, so
`f84005a` lands them.

**C13** — the 23 in-directory `docs/operational-models.md` citations under
`components/sim/docs/` are ruled ACCEPTABLE. The repoint pass had already
REFUTED them as a defect on its own evidence; the row closes.

**C14** — the two LIVE surfaces corrected, historical artifacts untouched.
`docsgen.clj:163` said `done-pointer-adr-test` "parses" the `- **ADR-NNNN**`
prefix and `help_voice_test.clj:66` said `tag-law-test` and
`-done-pointer-adr-test` "use" the pairing shape — both PRESENT TENSE for a
gate `e189418` deleted. Both now name the deletion, in the tree's own
established convention for this class.

**THE POPULATION WAS RE-DERIVED, AND THAT CHANGED WHAT NEEDED PAYING.** Of the
surfaces a fresh grep finds, `state_derived.clj` (2), `adr_index_test.clj` (2)
and `Makefile:255` (1) ALREADY name `e189418` as having deleted the gate they
cite. They are honest and were left alone — so C14's population was two files,
not five.

**ONE STALE LIVE SURFACE FOUND OUTSIDE C14'S NAMED TWO**, rowed rather than
widened into, on the planners-session precedent the repoint pass followed:
`roadmap_lint_test.clj:43` says `notes/prompts/` is "frozen by
`ehrt.docs-tooling.notes-prompts-frozen-test`", present tense, and `e189418`
deleted that gate too. It was NOT in the repoint pass's own count of 8, so it
is a new find rather than an unpaid one.

## 2. The de-alias — a finding before the first pair

**The 22 pairs were not 22 independently-enablable units at stage 1's shape,
and nothing in the prompt, the census or the P5 row had noticed.**
`fold/reinstated-projection` was `replay-projection` — an ALIAS, gated
`identical?` by `ehrt.sim-engine.apply-projection-test`, which is exactly what
census correction C5 asked stage 1 to make visible. One object means sites 2
and 3 share one projection, so enabling any site-2 pair would also have
enabled its site-3 twin in the same commit: eight commits covering sixteen
pairs, against a ruling that says one.

**It is FORCED, not a convenience, and ordering cannot avoid it.** The two
columns must genuinely diverge: 3 × A2 `:warm-up-mark` is predicted INERT
while its site-2 twin 2 × A2 is predicted OUTPUT-MOVING, so with the alias in
place the inert one could not land at all.

`c4f6ddd` gives site 3 its own literal, of the same VALUE, and flips the gate
from `identical?` to `not identical?` so a later session cannot silently
re-couple the columns. It enables nothing and is output-identical. Census
section 3d records it as the one row of the checklist that is not a pair.

## 3. The nineteen INERT pairs — ALL CONFIRMED, none refuted

**Not one INERT prediction was refuted**, so the fence that would have stopped
the span work ("if more than two INERT predictions refute, the census's cone
method itself is in question") never came near firing, and no bisect was owed.

Enabled in census order, ONE COMMIT EACH, enable-only: no accumulator logic
edited, no draw order changed, no `interface.clj` touched. Each commit moves
its own column in `apply-projection-test` and the matrix arithmetic in the
same commit, which is the gate stage 1 designed working as designed.

End state: **site 1 at 13 of 13 — full product, the ruled end state — site 2
at 11 of 13, site 3 at 12 of 13.** The three omitted cells are exactly the
three predicted OUTPUT-MOVING.

### 3a. The proof, span by span

| span | range | oracle | bracket | suite at tip |
|---|---|---|---|---|
| 1 | `e9c01b1..e05da9c` | **IDENTICAL**, 41 roots | **IDENTICAL**, 38 roots | EXIT 0 |
| 2 | `e05da9c..b547f8f` | **IDENTICAL**, 41 roots | **IDENTICAL**, 38 roots | EXIT 0 |
| 3 | `b547f8f..3abfa44` | **IDENTICAL**, 41 roots | **IDENTICAL**, 38 roots | EXIT 0 |

Six harness runs, **no declaration owed or made at any of them**, soundness
"yes outside the leading docstring" every time. The bracket's own coverage
line each time: `38 roots carry :ground-truth and are digested; 3 skipped (no
such key): appendicitis.edn, ear-infections.edn, sore-throat.edn`.

**Span-identity is not per-commit identity**, and that caveat stands: a delta
anywhere inside a span would have forced a bisect to the enabling commit. No
span moved, so none was owed.

### 3b. Three enabling diffs touched a call site, and why

Only the two TRANSIENT accumulators need a slot: `:log` and `:entries` are
`conj!`-ed and throw on nil, while every persistent concern — the three
indexes, `:log-mirror`, `:state-history` — builds from nil cleanly. So
1 × A13, 2 × A11 and 3 × A11 added a transient, and 3 × A2 added a parameter.
That asymmetry is a property of the concerns, not of the sites, and it is what
decided which sixteen of the nineteen diffs were pure set edits.

### 3c. Two findings the cones did not carry

* **`:log-mirror` REVERSES at sites 2 and 3.** The concern is `(assoc world'
  :ground-truth (into (:ground-truth world) events))`. Site 1's world always
  holds a `:ground-truth` VECTOR so `into` appends in log order; sites 2 and 3
  start from `{:patients {}}`, so it is `(into nil events)` — a LIST, in
  REVERSE order. Inert at both, because both discard the world, and NOT fixed:
  editing the accumulator while enabling is outside this session's fence. A
  consumer must seed `:ground-truth []` first. Rowed, not absorbed.
* **3 × A2 carries one DECLARED value.** `:warm-up-mark` takes a
  `:warm-up-seconds` parameter and a log has no source for one — which is
  precisely why its site-2 twin is output-moving. Site 3's call site declares
  `0`, the census's own named option, marking every event in that fallback's
  local copy `:warm-up false`. It is inert only because `evolve` reads
  `:warm-up` NOWHERE (measured: zero occurrences in `evolve.clj`) and site 3
  reads a PATIENT STATE. Named so a later consumer does not inherit it
  unknowingly.

### 3d. The suite, and one delta explained

`make test` EXIT 0 at every span tip. The settled tree reads **4,755 deftests
/ 24,193 assertions / 410 namespaces** — the stage-1 tree's ledger exactly,
UNMOVED by twenty-two commits, which is what enable-only diffs plus a gate
whose assertion COUNT does not depend on the sets it checks should do.

**The two span-tip runs read 24,191, and the −2 is the measurement harness,
not the spans.** They ran in disposable `git worktree`s, where `.git` is a
FILE (a gitdir pointer) rather than a directory, so
`ehrt.sim.version/git-sha` — which reads `.git/HEAD` with no subprocess —
returns nil, and
`generator-sha256-is-not-the-all-zero-placeholder-when-git-is-present` wraps
its single `is` in `(when (version/git-sha) ...)`. One assertion skipped, in
each of the two projects that run it. Located by diffing per-namespace counts
rather than reasoned: `ehrt.sim.version-test` is the ONLY namespace that
moves, 10 → 12, and no other count differs by a unit.

## 4. The three OUTPUT-MOVING pairs — PREPARED, NOT LANDED

None was landed. Each was prepared on a throwaway local branch
(`stage2-probe/*`, never pushed) and measured.

### 4a. THE ORACLE IS THE WRONG INSTRUMENT HERE, and says so itself

`bin/regression-oracle` reports **IDENTICAL for all three probes**. **That
verdict is VACUOUS and must not be read as a refutation.**
`components/oracle/src/ehrt/oracle/digest.clj`'s own coverage note lists
`engine/replay` among the things NO root reaches, and every root's digest is
`{:ground-truth :hl7}` — the log and the emitted messages, neither of which
passes through `replay` or through `reinstated-state`'s fallback. The one
partial exception is named in that same note: `demographic-fold` goes through
`ehrt.sim.run/run-command`, which SELF-CHECKS, so `check-all` — and with it
`replay` — runs over that one root's log inside the harness, and a FIRING
invariant would make the root throw rather than write a file. Findings are not
digested, so even there an IDENTICAL says the catalog passed, never what it
reported.

**This is the vacuous-gate shape census correction C3 warns about, arriving
from the other direction**, and reporting "IDENTICAL, therefore refuted" would
have been the session's own version of the mistake it inherited a correction
for. So each pair was measured DIRECTLY instead: `fold/apply-events` driven on
a real gated log under the site's projection with and without the pair, entry
by entry.

### 4b. 2 × A1 `:encounter-stamp` — PREDICTION REFUTED, measured identity

**Enabling diff** (`fold.clj`, one set literal, no call-site change):

```
-  #{:log-ordinal :reinstate-index :citation-index :registration-index
-    :patient-bootstrap :patient-state :bed-index :log-mirror
-    :log-accumulator :state-history :replay-entries})
+  #{:encounter-stamp :log-ordinal :reinstate-index :citation-index
+    :registration-index :patient-bootstrap :patient-state :bed-index
+    :log-mirror :log-accumulator :state-history :replay-entries})
```

**The delta: THERE IS NONE.** Measured on all three of the oracle's
encounter-carrying roots — the only three of the 41 whose logs contain an
`:encounter-id` at all — driving `replay` under both projections and comparing
every entry:

| root | log events | entries | first divergent entry |
|---|---|---|---|
| `encounter-horizon` | 170 | 170 / 170 | **none** |
| `chatter-charges` | 477 | 477 / 477 | **none** |
| `scheduling` | 487 | 487 / 487 | **none** |

No first divergent root, no byte span, because nothing diverges.

**WHY, and it is TWO INDEPENDENT REASONS.** Either alone would carry it:

1. **`stamp-encounter` is guarded on `contains?`.** Its first clause is `(if
   (or (contains? event :encounter-id) (two-encounter-event-types (:event
   event))) event ...)`, and its own docstring says why — "an opener already
   carries its own minted id and is left alone (`contains?`, not `some?` — a
   key that is there is there)". A replayed log's events were stamped inbound
   at site 1, so re-stamping them is the IDENTITY by construction.
2. **The decoration runs off the PRE-BATCH world, and at site 2 that is
   `{:patients {}}`.** `replay` hands the whole log to `apply-events` as ONE
   batch, so an event that carried NO id (site 1 stamps nothing after a
   discharge — "a pending lab result, a medication end at home") finds no
   patient, gets no id, and is left alone too.

**What the census got wrong, and it is a reading rather than an error of
method.** Section 3b's cone reasons about `evolve` folding `:encounter-id`
into conditions, observations, medication orders and care plans — all true —
but it never asks whether `stamp-encounter` would produce a DIFFERENT id, and
`stamp-encounter`'s docstring answers that in the sentence right beside the
one the census DID quote. The census quoted the "identity on a legacy run with
no `:encounters` opt-in" branch and not the `contains?` branch.

**Lettered dispositions.**

* **(a) LAND AS INERT.** The prediction is refuted by measurement on every
  encounter-carrying root the repository has. Site 2 reaches 12 of 13 and the
  pair costs one `contains?` per event. Cheapest route toward the ruled end
  state.
* **(b) LAND WITH A CO-LANDED IDENTITY GATE.** As (a), plus a test pinning
  that re-stamping an already-stamped log is the identity — because the
  inertness rests on `stamp-encounter`'s `contains?` guard and on the
  whole-log-as-one-batch shape, and BOTH are things a later session could
  change without knowing this pair depends on them. Recommended if (a) is
  taken at all.
* **(c) KEEP OMITTED.** On the grounds that a concern which is provably the
  identity at this site buys nothing but a guard to run, and that "full
  product at every site" is worth less than a projection that says what the
  site actually does. This is the only option under which the site-2 column
  stays a statement rather than a formality.

### 4c. 3 × A1 `:encounter-stamp` — PREDICTION REFUTED, same mechanism

**Enabling diff**: the same one set literal, on `reinstated-projection`.

**The delta: THERE IS NONE.** Site 3 reads `(:before (nth entries idx))` — one
patient state — so only that projection of the entries was compared, across
the same three roots: no divergent `:before` at any index, on any of them.
`encounter-horizon` carries 59 `:transfer`/`:discharge` events (log indexes
10, 22, 24, 25, 26, 27, 36, 37, 41, …), i.e. 59 indexes a reinstating cancel
could actually ask about, and not one of their `:before` values moves.

Both mechanisms from §4b apply unchanged. The census's cone for this pair is
narrower than its site-2 twin's but rests on the same unasked question.

**Lettered dispositions:** as §4b's (a)/(b)/(c), and this pair should be
disposed the SAME way as 2 × A1 — they are one mechanism seen from two sites,
and splitting them would leave the two columns disagreeing about a concern
that behaves identically at both. Taking (a) or (b) here puts site 3 at
**13 of 13**, full product.

### 4d. 2 × A2 `:warm-up-mark` — PREDICTION CONFIRMED, and the corpus cannot see it

**Enabling diff** (`fold.clj`: one set literal AND a call-site parameter,
because `replay`'s signature is `[ground-truth]` and a log carries no window):

```
+  #{:warm-up-mark :log-ordinal :reinstate-index :citation-index
+    :registration-index :patient-bootstrap :patient-state :bed-index
+    :log-mirror :log-accumulator :state-history :replay-entries})
...
+                            ;; PROBE ONLY -- a log carries no
+                            ;; warm-up window, so 0 is declared.
+                            :warm-up-seconds 0
```

**NO ORACLE ROOT CAN WITNESS THIS PAIR.** A grep for `:warm-up true` across
all 41 baseline outputs returns **ZERO** — every gated root runs with no
warm-up window, so every event is already `:warm-up false` and declaring 0
reproduces it exactly. That is the second, independent reason the probe's
IDENTICAL is vacuous, on top of §4a's.

**The delta, on a log that HAS a window.** Driven through
`ehrt.sim-engine.interface/run` at seed 424242, 3 patients, an
admission/delay/discharge pathway, `:warm-up-seconds 150` — 9 events, marks
`{true 2, false 7}`:

| | |
|---|---|
| first divergent entry | **index 0** |
| divergent entries | **2 of 9** |
| key that differs | `:event` only (`:before`/`:after`/`:world-*` unmoved) |
| the move | `:warm-up true` → `:warm-up false` |
| first differing byte | **425** |
| byte length | baseline **2184**, probe **2185** |

```
baseline ... "PID-000000-add15d94", :role :subject}], :warm-up true}, :patient-id ...
probe    ... "PID-000000-add15d94", :role :subject}], :warm-up false}, :patient-id ...
```

**AND WITH THE CORRECT WINDOW DECLARED IT IS THE IDENTITY: 0 of 9 entries
diverge at `:warm-up-seconds 150`.** So the pair does not move output because
recomputation disagrees with the log; it moves output because `replay` has
nowhere to get the window from, exactly as the census predicted, and the whole
question is whether the window should be threaded.

**One correction the census owes itself.** Its cone names `check.clj:184-185`'s
`:warm-up-mark-matches-window` as the consuming path that would go vacuous.
**It would not.** That invariant's signature is `[ground-truth
warm-up-seconds]` and its own docstring says "checkable without replay" — it
reads the LOG, never a replay entry, so enabling this pair cannot make it
compare a recomputation with itself. A sweep for every live reader of the
`:warm-up` KEY finds `check.clj:185` (the log), `event_schema.clj` (a schema
declaration), and `er7.clj:245` (prose naming what a Z-segment is NOT bound
to). **No live surface reads `:warm-up` off a replay entry at all** — so the
measured delta is real in `replay`'s return value and reaches no consumer. The
suite was run on the probe branch to check that claim rather than argue it:
**`make test` EXIT 0, 4,755 / 24,191 / 410** (the same worktree −2 of §3d),
zero failures, zero errors. **That green is weaker than it looks and is
reported as such**: no test in the tree drives `replay` over a WINDOWED log
at all. `check_test.clj`'s two warm-up cases call
`check/warm-up-mark-matches-window` directly on a hand-built log, and
`engine-run-warm-up-seconds-marks-exactly-the-window` reads `run`'s own
output — site 1. So the suite confirms that no gate fires today; it does not
independently prove that no consumer exists.

**Lettered dispositions.**

* **(a) BUG-IN-CURRENT — rejected on the evidence.** Nothing is wrong with the
  current behaviour: the log's own marks are authoritative and `replay` is
  right not to touch them.
* **(b) LOAD-BEARING, KEEP OMITTED PENDING.** `replay` has no source for the
  window; enabling it with a declared 0 measurably DESTROYS real information
  in the entries (2 of 9 above). Site 2 ends at 12 of 13 and the arc declares
  the omission permanent rather than pending. **This is the option the
  measurement most supports** — the census called this pair "the arc's
  clearest candidate for 'enabled, and the answer is no'", and it is.
* **(c) TAKE THE CHANGE DECLARED — thread the window.** Give `fold/replay` a
  `warm-up-seconds` parameter and pass it from the 16 live call sites. The
  measurement says this makes the pair the IDENTITY, so it reaches full
  product at zero output cost. The price is an API change to the most-called
  function in this arc, and a `replay` that re-derives a value the log already
  carries — which is the vacuous shape, just relocated from `check` to `fold`.
* **(d) TAKE IT WITH 0 DECLARED — rejected on the evidence**, and named only
  so it is not re-proposed: it is measurably lossy, and 3 × A2 landed with a
  declared 0 ONLY because site 3's read cannot reach `:warm-up` at all.

## 5. What this session deliberately did NOT do

* **No OUTPUT-MOVING pair was landed**, including the two whose predictions
  the measurements refute. The ruling says the author disposes and a later
  session lands; a refuted prediction is a finding to be disposed of, not a
  licence to take the change.
* **No accumulator logic was edited while enabling** — the `:log-mirror`
  reversal is rowed, not fixed.
* **No `interface.clj` edit, no draw-order change.** Site 3 is not deleted,
  though census 4d still predicts it can be, and 2 × A4 now gives that
  deletion its first-class source.
* **The O(N)-per-cancel win is not taken.** 3 × A4 puts the index where
  ADR-0169's 35.3%-of-generate cost can be paid off, and rewiring the fallback
  to read it is a change to the site rather than the enabling of a pair.
* **The three probe branches are local-only and were deleted at close.** Their
  diffs are in §4 above, which is where a later session should read them from.

## 6. Close

* **`clojure -M:poly check`** — `OK`.
* **`make test`** — EXIT 0 on the settled tree, 4,755 / 24,193 / 410. EXIT 0
  at both intermediate span tips, with the single −2 of §3d explained.
* **`bin/regression-oracle`** — IDENTICAL at all three spans, 41 roots, no
  declaration. Script output quoted in §3a.
* **`bin/ground-truth-bracket`** — IDENTICAL at all three spans, 38 roots, no
  declaration.
* **Push** — three pushes, `e9c01b1..e05da9c`, `e05da9c..b547f8f`,
  `b547f8f..3abfa44`, plus this record. `gitleaks` clean over 1,325 commits
  and the pre-push hook `OK` at each.
* **Post-push message verification** — all 21 code/docs commits diffed against
  the message files that produced them, every one clean. Nothing the WSL
  wrapper dropped.
