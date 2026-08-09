# 2026-08-09 — ehr-testing-tools: review-2 rulings landing (build session)

## Context

Archived 2026-08-09. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `eefce23` (repo review 2, ADR-0092) and closed
at `c112364` (ADR-0093) plus this record's own commit. Original prompt
follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

2026-08-09 -- ehr-testing-tools: review-2 rulings landing

Context
Conventions read at HEAD `eefce23` (repo review 2, ADR-0092), design channel, 2026-08-09, verified by fresh public clone. The author ruled on ADR-0092's six rulings-needed, 2026-08-09, verbatim: "1 a. 2 a. 3 a. 4 b. 5 (a)-lite. 6 a." This session lands everything those rulings make docs/law-only: three standing-ruling appends, four roadmap rows, and the front door's honesty disclosure, plus cluster C's trivial README path fix. It does NOT run any fix cluster, does NOT touch the census tool (ruling 6 schedules that as its own session, next), and does NOT decide D8-4 (bare-level unknown-flag tolerance -- still awaiting the author's call, parked for cluster B).
R30 ceremony, standing. Ext4 clone at its UNC path; fast-forward, record HEAD (expect `eefce23`; anything later escalates unless explained). Commit messages ASCII-only (standing practice; this session also lands its enforcement line -- see AR-RL2-5).
Read first

1. `notes/adr/0092-repo-review-2.md` -- the six rulings' full options text; this session executes rulings 1-5's docs surface and records ruling 6's scheduling.
2. `.agents/plans/2026-08-09-repo-review-findings.md` -- rows D8-6, D8-7, D7-7, D7-8, D7-13, D6-2, D6-1, H-6: the evidence each edit cites.
3. `.agents/rulings.md` -- tail section format; the three appends match the existing dated, tagged, provenance-marked shape exactly.
4. `.agents/plans/roadmap.md` -- Next (line 17) and Deferred (line 104) section conventions; Deferred rows carry revisit triggers.
5. `README.md` "See it run" (line 22) and `demos/scenarios/busy-tuesday/README.md` (lines 50-62) -- the disclosure prose to carry up, and the honest sibling wording it comes from.

Author rulings

* AR-RL2-0 [A] (ADR-0092, "This session's own successor tag debt"): tag `stable-20260809-repo-review-2` at `eefce23`, Step 0, ANNOTATED, standing ceremony. Verify-and-disclose if present.
* AR-RL2-1 [A] (ruling 1 = D8-6 option a): carry the sibling README's honesty disclosure up into README's "See it run" -- 2-4 sentences in the README's own voice stating what the demo actually shows (sparse traffic, idle-skips across a decade-spanning stream, the sparseness genuine to the scenario's population), citing the sibling README for the full closing-summary numbers. Do NOT change demo parameters, config, or the witnessed figures -- the byte-reproducible fence holds. Option (b) is explicitly NOT ruled; a busier demo may ride the future bed-board/census sink slice (ADR-0014), noted in ADR-0093's horizon, not acted on.
* AR-RL2-2 [A] (ruling 2 = D6-2 option a): append to `.agents/rulings.md`: "Measurements sample the claimed population, standing" -- a sweep or sample claiming to measure a population must draw from that population's own RNG path/generation mechanism, never an independent synthetic path assumed equivalent; a zero measured against a known-nonzero branch is the tripwire (ADR-0087's own self-caught miss is the precedent). Tag [A], date, cite ADR-0087/0092 D6-2.
* AR-RL2-3 [A] (ruling 3 = D7-7/D7-8 policy option a): append: "Horizon items anchor in the roadmap, standing" -- any item surviving past ONE arc close purely in horizon-note prose gains a `roadmap.md` Deferred or Next row in the SAME close that first restates it; horizon notes narrate, the roadmap remembers (ADR-0092 D7-7/D7-8's A/B evidence is the precedent). Tag [A], date, cite. This session executes the law's first two instances (Step 2).
* AR-RL2-4 [A] (ruling 4 = D7-13 option b): Wave E (vital-sign/CHF/contraceptives/covid19 cluster) parks. FIRST verify by grep that `roadmap.md` carries no existing Wave-E row (the design channel's claim; evidence outranks it) -- then add ONE Deferred row with the named revisit trigger: "the next content-vendoring session with a vital-sign-adjacent candidate." Status text names covid19's `:zero-on-every-seed` as the genuinely blocked member, per D7-13.
* AR-RL2-5 [A] (ruling 5 = H-6, the author's "(a)-lite"): append: "Post-push verification includes the ASCII check, standing" -- the standing post-push ceremony adds one mechanical line, `git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`, expected EMPTY; any hit is disclosed in-session, not discovered by channel report later. This is ceremony boilerplate for session prompts and session practice -- NO new repo test, NO workflow change, NO gate file. Tag [A], date, cite ADR-0091 AR-SD-6 / ADR-0092 H-6. THIS session's own Step-4/5 verifications run the line first.
* AR-RL2-6 [A] (ruling 6 = D6-1 option a): the census `:closure-file-count` fix is SCHEDULED, not executed here. Add one Next row: small census-tool session, extend closure-file counting from the JSON-module resolver to the CSV lookup-table resolver; cite the 3x undercount record (ADR-0074, register D6-1) and review 1's unactioned escalation. NO src touches this session.
* AR-RL2-7 [C] (cluster C rider): README.md line 140's dangling `docs/adr/0091-storefront-fixture.md` link corrects to `notes/adr/`. One line (register D8-7). D8-4 is NOT in scope.

Steps
Step 0 -- Preflight + tag (AR-RL2-0). Standard preflight (clean tree, HEAD `eefce23`, untracked disclosure, `clojure -M:poly check`, oracle pre-digest `eefce23 eefce23` -- 34 roots IDENTICAL expected, all workflow lanes' latest conclusions disclosed). Tag. No commit.
Step 1 -- Law appends (AR-RL2-2/3/5). Three appends to `.agents/rulings.md` under a new dated section "From review 2's rulings (ADR-0092/0093)". No commit yet.
Step 2 -- Roadmap rows (AR-RL2-3 first execution, -4, -6). Four rows: wellness-encounters anchor (Deferred; blocker/status from `state.md`'s own Live-work wording), notice-verbatim coverage-gap anchor (Deferred), Wave-E park (Deferred, named trigger, after the absence grep), census session (Next). No other roadmap content moves. No commit yet.
Step 3 -- README (AR-RL2-1, -7). The disclosure prose and the one-line link fix. No commit yet.
Step 4 -- ADR + ceremony surfaces + commit. `notes/adr/0093-review-2-rulings-landing.md`: the six rulings quoted verbatim with dispositions, what landed where, the two explicitly deferred decisions (D8-4 awaiting ruling; option-b busier demo as a possible bed-board-slice rider), ruling 6's scheduled session, this session's own successor tag debt. Index line in `notes/ADRs.md`; README count 90->91; roadmap Done pointer. Full local suite first (the register's own D2-4 finding stands until cluster A: also run `bin/verify-nist-lock` explicitly and disclose its result). Commit:

```
docs: review-2 rulings land -- three laws append, four anchors, the front door discloses (ADR-0093)

```

Push; run the AR-RL2-5 ASCII check on the landed message FIRST, then the standard message verification; watch CI to conclusion, all lanes noted.
Step 5 -- Ceremony. Self-archive this prompt at the START of the close phase (`2026-08-09-review-2-rulings-landing.md`), session record, both READMEs, one commit:

```
docs: session record and prompt archive -- review-2 rulings landing

```

Same verification order: ASCII check, message, CI.
Fences
No src/test/deps touches anywhere. No census-tool changes (scheduled only). No fix-cluster work (A, B, and the D8-4 call all await rulings/sessions of their own). No demo parameter or config changes. No new tests, gates, or workflow edits -- AR-RL2-5 is ceremony text, not mechanism. No roadmap content moves beyond the four named rows and the Done pointer. Law appends are exactly the three named.
Close-out
Echo to chat: the three appended rulings verbatim; the four roadmap rows as landed; the README disclosure paragraph as landed; the Wave-E absence-grep result; the `bin/verify-nist-lock` result; both ASCII-check results; shas, CI status across all lanes.

## Deviation record

None. All eight author rulings (AR-RL2-0 through AR-RL2-7) executed as
named; every fence held throughout — `git status --porcelain`
confirmed clean before this session's first tool call and at each
commit boundary. The one judgment call not explicit in the prompt's
own text: AR-RL2-4's absence grep found a closely related but
non-identical existing row ("Vital-sign channel," naming the same
three modules and the underlying vital-sign-register blocker, but not
the D7-13 aging pattern or its own named trigger) — resolved by adding
a new, cross-referencing Deferred row rather than either duplicating
the existing row's content or silently folding the new material into
it, disclosed in the session record rather than left implicit.
