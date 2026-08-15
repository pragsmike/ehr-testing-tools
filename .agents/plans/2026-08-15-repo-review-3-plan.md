# Repo review 3 — mitigation plan, for the author's ruling

Companion to `.agents/plans/2026-08-15-repo-review-findings.md`
(register, 40 rows). This is the `repo-review` skill's step 5: the
register's fix-session candidates batched into small fenced sessions,
the rulings needed stated with options and a recommendation each, and
what is deliberately fine named as such.

**RULINGS ARE THE AUTHOR'S — this plan proposes.** Nothing below has
been executed. The only mutations this session made are the Step-0
rubric amendment (`dbbeb1f`), the deferred micro-arc tag
(`stable-20260815-result-nodes`), the register, and this file.

---

## The one-paragraph version

Every finding reviews 1 and 2 left open in D4, D6, D7 and D8 is closed,
verified against live code rather than the fix ADRs. Two dimensions
regressed, both for the same reason and neither from new decay: the
Step-0 amendment widened D5's and D1's probe populations from a
registry to the tree, and both immediately surfaced defects that had
been sitting outside the old population for weeks. The headline is
**three shipped string-diagram teaching examples that are stale against
their own converter and now demonstrate exactly the defect ADR-0135 was
chartered to fix**. Four small fix sessions clear the whole register;
two questions need a ruling first; three probes did not run and are
named rather than quietly dropped.

---

## Proposed fix sessions

Four sessions, each small, each fenced, each with its co-landed gate.
Session A is the one that matters; the rest are cheap.

### Session A — Register every derivation (D5-3, D5-4, D2-4)

**Why first:** it is the only session closing a RED dimension, and its
finding is live and reader-visible today.

- Add a `make` target per unregistered derivation: `sim-theory`
  (producing both `components/sim/docs/sim-theory-diagram.mermaid` and
  the `.md`'s embedded block) and `palgebra-examples` (the three
  `components/palgebra/examples/*.mermaid`). Fold both into `docsgen`.
- Extend CI's freshness step to diff those paths alongside the existing
  five.
- Regenerate the three stale examples. Retire the header-recipe
  workflow in `sim-theory-diagram.md` and `sim-theory-equations.txt`,
  replacing the hand recipe with a pointer to the make target — this is
  what makes the ADR-0135 debt structurally unrepeatable rather than
  fixed once.
- **Co-landed gate, red-first:** the extended CI diff must be witnessed
  failing against the current tree before the regeneration lands. It
  will fail three times, once per stale example — that red is the
  proof the gate has the right population.
- **Fence:** `Makefile`, `.github/workflows/test.yml`, the five derived
  artifacts, the two sim-theory headers. Zero `src`, zero converter
  changes — the converter is correct; only its outputs are stale.

### Session B — Widen the scan root, then fix what it finds (D1-2)

- Widen `stale_path_test.clj`'s scan root from `docs/` (plus the one
  `use-cases.edn`) to every tracked `*/docs/**/*.md` surface, and add
  dead-markdown-link resolution to it.
- **The exclusion list is load-bearing** and must be encoded, or the
  gate lands noisy and gets weakened: (a) this repo's shorthand
  citation convention (`sim/run.clj`), (b) generator template sources
  whose links resolve at the generated output's location, (c)
  `polylith-brief.md`'s external tutorial examples, (d) `%20`-encoded
  filenames. All four are documented with evidence at register row
  D1-8.
- Fix the 25 dead links: 19 are a mechanical `../` → `../../../`
  rewrite (the un-re-depthed pre-merge prefix); 6 point at genuinely
  removed targets (`.agents/memory/patterns.md`,
  `.agents/plans/archive/judge-gate-refactor.md`) and need the author's
  view on whether to re-point or delete the sentence.
- **Priority within the session:** `components/sim/docs/third-party-sources.md`
  first — six dead links to `notes/facts-register.md`, the register its
  own licensing claims rest on.
- **Co-landed gate:** the widened test, witnessed red against the
  current tree (25 hits) before the fixes land.

### Session C — Ceremony-script correctness (D1-6, D2-6)

- `bin/post-push-verify`: derive the default base from the remote's
  pre-push tip rather than `tip^1`, and fail loudly rather than
  defaulting when it cannot be derived. Its header already documents
  the correct behaviour — the code is what is wrong.
- **Co-landed gate:** a test that constructs a synthetic multi-commit
  range and asserts every message in it is checked. Without this the
  fix is untestable by inspection and will drift back.
- Add one line to `build-session`'s verification step: gate output is
  captured to a file with its exit code recorded explicitly, never read
  through a pipe that can eat it (the ADR-0135 incident class).
- **Fence:** `bin/post-push-verify`, one new docs-tooling test,
  `.agents/skills/build-session/SKILL.md` + its `.claude/` mirror.

### Session D — Trivial ride-alongs (D4-3, D7-4, and D7-3/D1-5 if ruled)

Single-line changes, no design content; can ride along with any session
touching the same files, or land together in ten minutes.

- `ehrt gate` reports `:file-not-found` for a file that exists but is
  unreadable; its sibling `ehrt show` correctly reports
  `:path-unreadable`. Route `gate` through the same category.
- Roadmap row for the loopback flake, which has been carried in
  `state.md` alone for 18 days — the third instance of the pattern
  review 2 diagnosed and half-closed.
- If ruled: the two roadmap rows from D7-3 and the disposition of
  `bin/check-palgebra-drift` from D1-5.

---

## Rulings needed

### R-1 — `bin/check-palgebra-drift` (register D1-5)

The script's header calls it a "Nightly drift check." Nothing invokes
it — not the `Makefile`, not either workflow, not any script. It would
additionally clean-skip even if invoked, because it compares against a
sibling `../ehr-testing-sim` checkout that this workspace's own merge
retired.

- **(a) Delete it,** with a `notes/carve-loss-audit.md` row recording
  the disposition. *Recommended* — the repo it guards no longer exists
  as a separate consumer, and a script that cannot fire is worse than
  no script because its presence reads as coverage.
- **(b) Keep it, correct the header** to state it is a manual,
  sibling-checkout-only tool that nothing schedules.
- **(c) Keep it and schedule it** — only sensible if an
  `ehr-testing-sim` checkout is still a thing you maintain.

This is the unfound sibling of review 2's D2-4 (`verify-nist-lock`'s
false enforcement claim), which was found, ruled and fixed. Same shape,
same cheapness.

### R-2 — Two unregistered standing requests (register D7-3)

Both surfaced by the amended D7 probe; both aged entirely outside the
carried-item aging probe's field of view because that probe enumerates
the registers.

- **(a) `components/sim-model/resources/sim-model/demographics/NOTICE:26`**
  — "A future session WITH a Synthea checkout available can replace the
  content of these three files wholesale with a real extraction."
  Unregistered since 2026-08-05. *Recommendation:* a Deferred roadmap
  row with the revisit trigger stated, since its trigger is already
  literally written ("a session with a Synthea checkout").
- **(b) `docs/dev/source-sink-design.md:56`, row OPEN-4** — whether
  `corpus generate` grows an `--engine` flag. Marked **Open** in its own
  table since 2026-07-29, 17 days, never in any register.
  *Recommendation:* the author's view on whether this is still live. If
  yes, a Next row; if no, close the table row in place with a
  disclosure phrase (the shape the Deferred lint already requires).

### R-3 — D5's score (register, Dimension 5 verdict)

Scored **RED**, with the counter-argument stated in the register: the
three stale artifacts are teaching examples, not shipped documentation,
and nothing in the product is wrong. RED was scored on the other axis
the rubric names — a whole population outside every gate with
demonstrated live drift inside it, held green by three consecutive
reviews. **You may reasonably re-score this to YELLOW; the finding rows
do not change either way.** Flagged explicitly because the scoreboard
is a communication instrument and this is the one cell in it that is a
judgement call rather than a count.

---

## Deliberately fine — named, so review 4 does not re-flag them

- **`corpus --nonexistent-flag` reporting `:unknown-command`** rather
  than `:unknown-flag` (D8-2). The group-with-no-verb path never
  reaches the flag; the missing verb genuinely is the first error.
- **The four path-sweep false-positive classes** (D1-8): the shorthand
  citation convention, generator template sources, `polylith-brief.md`'s
  external tutorial examples, and `%20`-encoded filenames. Documented
  with evidence so they are inherited rather than rediscovered.
- **`state.md`'s `notes/adr/NNNN-slug.md`** (D1-7) — a template string,
  correct as written.
- **The Synthea-vendored `TODO`s** in module JSON and census EDN, and
  `bin/close-scaffold`'s template placeholders — matched by the amended
  D7 grep, all correctly excluded. Upstream bytes must stay verbatim.
- **Publish-prep Externals** — unchanged, correctly parked as
  author-action-only.

---

## Probes that did not run — named, not dropped

All three are the direct cost of running this review without sub-agents
over a 44-ADR window (review 2's was 11 ADRs, with five parallel
sub-agents). None is a finding; each is a gap in this review's own
coverage that review 4 inherits.

| probe | why it did not run | what it would cover | recommendation |
|---|---|---|---|
| **D8-5 — live fence battery** (every command fence across README, `docs/**`, the 21 use-case pages, `components/*/docs/**`, `demos/**`, plus `make quickstart` and `make integration`) | Execution budget went to the full-suite baseline (~25 min wall clock) and the amended probes; `make integration` also needs a primed artifact cache | **The largest gap in this register.** The window landed the entire user manual (ADR-0119–0125) plus a manual review (ADR-0134) — exactly this surface, entirely unverified this run | Run it as its own session before review 4, or budget it explicitly into review 4. It should not lapse twice |
| **D3-1 — local cold-clone probe** (HOME repointed, `EHR_TESTING_TOOLS_CACHE` repointed, full suite) | Substituted with CI's own cold-runner run at `dbbeb1f`, which is genuinely a fresh clone with a cold cache | Author-machine-only assumptions that GitHub's runner image would not reveal | Restore the review-2 method at review 4 |
| **D6-4 — full window deviation read** | 44 ADRs, each with a Deviations section; read at heading depth, not in full | New sampling-adequacy misses disclosed in ADRs this review did not read closely | Either narrow review 4's window or budget the full read. A 44-ADR window exceeds what one session covers at review-2 depth |

**A structural note on cadence, offered not ruled:** reviews 1→2 were
two days and 11 ADRs apart; review 2→3 was six days and 44 ADRs. The
review instrument's own coverage degraded measurably at that window
size — three probes unrun, and this register says so rather than
scoring around it. Either a tighter cadence or an explicitly multi-
session review shape would keep the instrument honest. Your call, and
it belongs with the rulings above rather than inside a fix session.

---

## What this review says about the amendment

The Step-0 amendment was landed and executed in the same session, per
the author's choice (b), so that it would prove itself by running. It
did:

- **D5's patch** predicted one unregistered derivation and found five,
  three of them demonstrably stale against their own converter — the
  register's headline, invisible to `make docsgen`, to CI, and to
  ADR-0135's own careful manual sweep.
- **D1's patch** found 25 dead markdown links, all 25 inside the
  scan root the amendment added, zero outside it. It also found that
  the gate which should have caught them (`stale_path_test.clj`) was
  itself scoped to `docs/` — and that its own origin, P1-1 on
  2026-07-31, was this exact link family. Third recorded hit of the
  scan-root class, first one found inside a gate rather than a probe.
- **D7's patch** confirmed the sim-theory instance discharged (as
  predicted) and found two further standing requests aged 10 and 17
  days outside every register.

Beyond the three patched dimensions, the population-closure law's own
first question — *"how do I know this is all of them?"* — surfaced two
further instances the amendment did not anticipate:
`bin/post-push-verify` deriving "the pushed range" from a commit's
parent link, and review 2's own D1-4 equating two registries by their
cardinality. Five instances of one error class in one review.
