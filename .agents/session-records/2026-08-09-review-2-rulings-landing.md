# 2026-08-09 — Review 2's rulings land

## Scope

Session prompt naming AR-RL2-0 through AR-RL2-7, the author's own six
rulings verbatim ("1 a. 2 a. 3 a. 4 b. 5 (a)-lite. 6 a.") on ADR-0092's
own plan draft. Lands everything those rulings make docs/law-only:
three standing-ruling appends, four roadmap rows, the front door's own
honesty disclosure, cluster C's trivial README path fix. Does not run
any fix cluster, does not touch the census tool, does not decide D8-4
(still awaiting the author's own call).

## Step 0 — Preflight + tag (AR-RL2-0)

Working directory confirmed the ext4 clone, HEAD `eefce23` exactly
(repo review 2, ADR-0092), branch up to date with `origin/main`,
working tree clean. `git config core.hooksPath` confirmed `.githooks`.
Untracked/ignored files disclosed (all standard build-cache/lock-file
noise — `.cpcache`, `.clj-kondo/.cache`, `.lsp/.cache`, `target`,
`out`, `.claude/scheduled_tasks.lock`, `.claude/settings.local.json` —
none tracked-relevant). `clojure -M:poly check`: OK. Oracle pre-digest
(`bin/regression-oracle eefce23 eefce23`): all THIRTY-FOUR roots
confirmed IDENTICAL, soundness "yes outside ns form" — the expected
trivial tip-against-itself result. Last five `test`-lane runs on main
all green; latest `Integration` lane run (`31312458033`,
`workflow_dispatch`) green — no open red on either lane.

Tagged `stable-20260809-repo-review-2` at `eefce23`, annotated, message
"repo review 2 landed, design-channel-verified 2026-08-09 (ADR-0092)";
pushed; peeled ref verified against `git ls-remote --tags origin`
(`eefce2314f557ddb9ac3ff1a1e132575795f232f` exactly).

## Step 1 — Law appends (AR-RL2-2/3/5)

Three standing rulings appended to `.agents/rulings.md` under a new
dated section, "From review 2's rulings (ADR-0092/0093)," matching the
file's own existing dated/tagged/provenance-marked shape: "Measurements
sample the claimed population" (D6-2), "Horizon items anchor in the
roadmap" (D7-7/D7-8's policy), "Post-push verification includes the
ASCII check" (H-6, the author's own "(a)-lite" narrowing — ceremony
text only, no new test/workflow/gate). No commit yet.

## Step 2 — Roadmap rows (AR-RL2-3's first execution, -4, -6)

`roadmap.md`'s Deferred section carrying no row named "Wave E" was
verified by grep before adding one (the design channel's own claim;
confirmed — a "Vital-sign channel" row named the same three modules
and the underlying vital-sign-register blocker, but no row named the
D7-13 aging pattern or carried its own revisit trigger). Four rows
landed: wellness-encounters (Deferred, blocker/status carried from
`state.md`'s own Live-work wording plus ADR-0070's original framing);
the `notice_verbatim_test` coverage gap (Deferred, same treatment);
Wave E (Deferred, parked, named trigger "the next content-vendoring
session with a vital-sign-adjacent candidate," `covid19`'s own
`:zero-on-every-seed` named as the genuinely blocked member); the
census `:closure-file-count` fix (Next, scheduled, citing the 3x
undercount record and review 1's own unactioned escalation). No other
roadmap content moved. No commit yet.

## Step 3 — README (AR-RL2-1, -7)

The busy-tuesday front door's "See it run" section gained a 3-sentence
disclosure in the README's own voice — the board mostly idle-skips
forward through a decade-spanning stream, only one inpatient is ever
admitted, the sparseness is genuine to the scenario's own population —
citing `demos/scenarios/busy-tuesday/README.md` for the full
closing-summary numbers rather than restating them. No demo parameter,
config, or witnessed-figure change. `README.md`'s own dangling
`docs/adr/0091-storefront-fixture.md` link corrected to `notes/adr/`.
No commit yet.

## Step 4 — ADR + ceremony surfaces + commit

`notes/adr/0093-review-2-rulings-landing.md` landed: the six rulings
quoted verbatim with dispositions, what landed where, the explicitly
deferred decisions (D8-4; ruling 1's own option b), ruling 6's
scheduled session, this session's own successor tag debt. `notes/
ADRs.md` gained its index line; `notes/adr/README.md`'s own file count
corrected 90→91 (`ls`-verified); the roadmap's Done section gained one
pointer.

Full local suite run fresh (`clojure -M:poly test :all
skip:integration`): 293 namespace blocks, 0 failures, 0 errors
anywhere. `bin/verify-nist-lock` run explicitly (D2-4's own still-open
finding — disclosed by name, not fixed this session): OK, all 6
hit-nexus-sourced coordinates match `artifacts.lock.edn` exactly.
`gitleaks git --staged -v`: clean.

Committed `c112364` ("docs: review-2 rulings land -- three laws
append, four anchors, the front door discloses (ADR-0093)"); pushed.
AR-RL2-5's own ASCII check run FIRST on the landed message: `git log
--format=%B -1 | LC_ALL=C grep -n '[^ -~]'` — EMPTY. Standard message
verification: pushed message diffed against the source file — only
delta is the trailing-blank-line artifact `git log --format=%B`
always adds, not a real mismatch. CI watched to conclusion: `test`
lane run `31323022534`, green, 3m21s.

## Step 5 — Ceremony (this record)

Session record and prompt archive land together, both READMEs
updated, same commit.

## Deviations, disclosed

None. All rulings (AR-RL2-0 through AR-RL2-7) executed as named; every
fence held — `git status --porcelain` clean before this session's
first tool call, clean at each commit boundary. The Wave-E absence
grep found a closely related but non-duplicate existing row (the
"Vital-sign channel" row); the new row cross-references rather than
duplicates it, a judgment call within the ruling's own instruction,
disclosed here rather than silently made.

## Close-out echo

**The three appended rulings, verbatim** (see `.agents/rulings.md`,
"From review 2's rulings (ADR-0092/0093)"):
- "Measurements sample the claimed population, standing" — a sweep or
  sample claiming to measure a population must draw from that
  population's own RNG path/generation mechanism, never an independent
  synthetic path assumed equivalent; a zero measured against a
  known-nonzero branch is the tripwire.
- "Horizon items anchor in the roadmap, standing" — any item surviving
  past ONE arc close purely in horizon-note prose gains a `roadmap.md`
  Deferred or Next row in the SAME close that first restates it;
  horizon notes narrate, the roadmap remembers.
- "Post-push verification includes the ASCII check, standing" — the
  standing post-push ceremony adds one mechanical line, `git log
  --format=%B -1 | LC_ALL=C grep -n '[^ -~]'`, expected EMPTY; any hit
  is disclosed in-session, not discovered by channel report later.

**The four roadmap rows, as landed:** wellness-encounters (Deferred);
`notice_verbatim_test` coverage gap (Deferred); Wave E, parked
(Deferred); census `:closure-file-count` fix, scheduled (Next). See
`.agents/plans/roadmap.md` for full text.

**The README disclosure paragraph, as landed:** "What actually renders
is sparser than 'busy' suggests — most of this scenario's own
population's care unfolds as intake and follow-up spread across a
decade, not a single shift, so the board mostly idle-skips forward
through quiet stretches rather than filling with beds, and only one
inpatient is ever admitted across the whole run. That sparseness is
genuine to this scenario's own module mix and patient population, not
a player defect. `demos/scenarios/busy-tuesday/README.md` carries the
full closing-summary numbers this session actually witnessed."

**Wave-E absence-grep result:** no row named "Wave E" existed; a
related "Vital-sign channel" row did (same three modules, the
underlying register blocker), without the D7-13 aging citation or the
ruling's own named trigger — added as a new, cross-referencing row.

**`bin/verify-nist-lock` result:** OK, 6 hit-nexus-sourced coordinates
match `artifacts.lock.edn` exactly.

**Both ASCII-check results:** Step 4 commit `c112364` — EMPTY. Step 5
commit (this record's own commit) — recorded below at HEAD landed.

**SHAs:** Step 0 tag `eefce23` (stable-20260809-repo-review-2). Step 4
commit `c112364`. Step 5 commit: this record's own landing commit.

**CI status, all lanes:** `test` lane green at every push this session
(`31323022534`, Step 4, 3m21s); `Integration` lane's latest run
(`31312458033`, before this session opened) still the most recent —
unchanged, no new Integration-lane trigger this session (docs-only,
`skip:integration` scope).

## HEAD landed

`c112364` (Step 4's own commit — Step 5's own commit lands after this
record, in the same push as the prompt archive).
