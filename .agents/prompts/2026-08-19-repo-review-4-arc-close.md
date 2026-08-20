# Archived prompt: repo-review-4-arc-close (2026-08-19)

Verbatim session prompt, archived per charter R-A. Landed as ADR-0159.

---

Session prompt -- repo-review-4 arc close: the residue dispositioned,
every FIXED append verified, review 5's inheritance written, the row
closed -- ADR-0159

Context

Claude Code under R30 in ehr-testing-tools, closing the repo-review-4 arc. HEAD at handoff: e967fd7 (ADR-0158 addendum; tree clean; CI green at 6ed767c and e967fd7; last tag `stable-20260819-review-4-fix-4- sampling-and-provenance` @6ed767c, no tag owed). The arc: assessment ADR-0154 (72-row register, 10 rulings, 8 planned sessions), fix sessions ADR-0155..0158 (author-paired G+A, E+C, B+D, F+H; all ten R4-Q rulings landed). Precedent for this session's shape: ADR-0139 (review 3's arc close) -- read its structure; this ADR is its sibling, including the discipline that the close AUDITS the arc rather than extends it. NO new fixes: a defect found here is rowed, not fixed, however small (the close is an audit; an audit that edits its subject is neither).

Channel anchors at e967fd7 (re-derive):

* Roadmap `#repo-review-4` (P2) carries the arc ledger: 12+11+2(+hook) +13ish rows across the four ADRs -- the row's per-session tallies vs the register's actual FIXED appends have NEVER been cross-checked as a set; that reconciliation is this session's first deliverable. Register tallies by my grep: 48 `FIXED ADR-015x` appends; residue = 72 - 48 = 24-ish rows across {close-as-fine, intake, ruling-needed- now-ruled, fix-session-candidate-not-taken} -- derive the true partition, do not trust these figures.
* Rows the arc opened (audit their existence and price): `#oracle- coverage-roots` (P3 -- re-prioritise BELOW live work; the arc close is where the prompt of 2026-08-19 said this happens), `#bed-ready- vacancy-cascade`, `#commit-msg-ascii-hook` (CLOSED 0157), `#edit- root-worktree-residue` (CLOSED 0158), `#intake-staging-dir` (CLOSED 0158), `#setup-md-hook-citations`, `#two-clocks-asset-field-audit`, `#reader-path-fences-manual-usecases` (the 34), the D1-9/D1-10 row, the corpus-player row, D8-2's remainder row -- enumerate from the roadmap, not this list.
* Review 3's twelve-row watch-list (ADR-0139 :464-482): each row's final disposition across THIS arc must be stated (several closed: C-1 by 0152, C-4 verified 0154, D3-1 by 0157/0158, D8-5's survivor re-measured 0154 and partly gated 0158; others live on). The watch- list does not carry forward silently: review 5 gets a NEW list.
* Cadence: `rulings.md:189` `R-review-cadence-in-adrs` -- read the row and compute review 5's due point from its own text (the channel's arithmetic says ~ADR-0169 if the interval is 15; use the row's number, not this).

Read first

1. ADR-0139 whole (the sibling); ADR-0154..0158 + addenda (the arc); the register and plan whole (they are the subject).
2. The roadmap whole (every row the arc touched); `rulings.md` rows added this arc (`R-amend-unpushed-message-only`, `R-preflight-fail- closed` if it exists -- grep, the 0155 prompt made it conditional -- plus appends to `R-oracle-script-contract`, `R-io-result-or-loud`).
3. `state.md` + `state-derived.md`; reading-set budgets; the memory files the sessions updated (read-only context).
4. `rulings.md#R-review-cadence-in-adrs`, `#R-register-hygiene-at- close`, `#R-red-pushed-with-green` (n/a here -- docs-only session, say so), `#R-full-suite-before-push`, `#R-session-verifies-ci-via- gh`; build-session skill.

Author rulings, verbatim

* "go" on the arc close (2026-08-19), after "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones" (2026-08-18).
* Re-prioritisation of `#oracle-coverage-roots` below live work is channel-proposed, author-seen (message of 2026-08-19, unobjected); land it with the citation. If any OTHER priority move seems owed, propose it in the ADR, do not take it.
* Tag: no tag owed at Step 0. The arc-close tag: pay in-session if the tip run concludes success while open, else next Step 0 -- say which.

Step 0
Fresh clone, tip e967fd7; `bin/preflight`; baseline `make test` unpiped, MAKE_EXIT captured, wrapper ends `exit "$MAKE_EXIT"`, reconcile vs ADR-0158's 364 blocks / 4,070 tests / 18,304 assertions; `poly check`; budgets. This session is docs-only: predict close-suite delta ZERO and assert at close.

Step 1 -- the audit (ADR sections, no register edits yet)
(a) FIXED-append verification: for EVERY `FIXED ADR-015x` append in the register, verify the citing ADR exists and its text actually claims that row (spot-checking is not enough at 48 rows -- script it: extract (row-id, adr) pairs, grep each ADR for the row id; misses listed). (b) Residue partition: every non-FIXED row -> exactly one of {close-as-fine CONFIRMED (re-derive the row's probe if cheap, else mark confirmed-by-review), intake->rowed (verify the roadmap row exists), deferred-with-row (the 34-fence class), superseded (say by what)}; a row fitting none is a finding. (c) The arc's own errata sweep: every deviation/correction the five session reports disclosed (the 4d6ff78 sha, the four-reading-sets claim, the D6-1 remedy gap, the R4-Q4 cheap-fences premise, the two D8-2 partials, fix-2's non- ASCII commit, fix-1's unrun oracle) -- verify each is repo-recorded (ADR or register), not transcript-only; unrecorded ones are findings. (d) Ledger reconciliation: the roadmap row's per-session tallies vs (a)'s counts. (e) Rulings-landed check: all ten R4-Qs, where each landed, one line each.

Step 2 -- review 5's inheritance (ADR section + register)
The new watch-list, built from THIS arc (candidates -- judge, trim, add: the born-red-gate discipline (0158's tripwire precedent: a gate may land red only with its finding rowed); the `exempt` disposition's ratchet (exempt count can only shrink or carry a reason -- is that gated? if not, watch it); `#two-clocks-asset-field-audit` (redraw owed); the 34-fence session; D8-2's remainder; the historical-red technique (0158) as a standing instrument worth a skill line NEXT review if used twice; the multi-home-wards ingredient D6-1's remedy omitted (does any OTHER property test have a fixed-shape blind spot? -- a review-5 probe, not today's); `state-derived` self-list adoption (does anything else deserve L3-3's treatment?); preflight fail-closed in the wild (any session friction?)). Each watch row: what to probe, which dimension, what would count as fired. Review 5 due point per `R-review-cadence-in-adrs`; write it into the review-5 row.

Step 3 -- register hygiene (the edits)
Register: residue dispositions from Step 1(b) as dated appends; header gains "ARC CLOSED ADR-0159" line. Plan: Part 2 all-landed line. Roadmap: `#repo-review-4` -> CLOSED under `## Done` (the close text carries the arc ledger: 5 ADRs, 48+ rows fixed, 10 rulings, the residue count); NEW row `#repo-review-5` (`## Next`, due point, the new watch-list's location); `#oracle-coverage-roots` re-prioritised below live work with the citation. `rulings.md`: ONLY if Step 1 found a law the arc made that no row states (grep first; expect none -- 0155-0158 each landed their rows in-session).

Close (self-archive FIRST)
Archive to `.agents/prompts/2026-08-19-repo-review-4-arc-close.md`; open the session record; then ADR-0159 (the audit tables: (a) misses, (b) partition, (c) errata-recording status, (d) ledger, (e) rulings; the watch-list; the cadence computation), registers, session record with `gh run view` id/conclusion, full `make test` reconciled vs Step 0 (delta ZERO or explain), `bin/post-push-verify`, tag per ruling. Commit: "docs: ADR-0159 -- repo-review-4 arc close: audited, residue dispositioned, review 5 chartered"

Fences
Docs and registers ONLY: the register, the plan, roadmap, `rulings.md` (conditional), ADR-0159, prompt archive, session record, state-derived (regenerated); NO src, NO test, NO bin, NO skill edit (a skill amendment candidate goes on the watch-list); NO fix of any audit finding (rowed); oracle untouched and unrun (docs-only -- state that `R-oracle-script-contract` makes an unrun oracle UNCLAIMED, not asserted-identical); no planted reds (nothing to plant); exit codes unpiped; ASCII messages; anchored register edits, dated appends; R-RP. READ-BACK: files touched vs this list; the audit's four counts ((a) misses, (b) partition sizes, (c) unrecorded errata, (d) ledger delta); close-suite delta.
