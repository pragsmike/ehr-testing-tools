# ORU control id — the premise did not hold, and the row was wrong twice (ADR-0181)

2026-09-23. Base `e81f7026`; **one commit, characterization only**; this
record and the CI marker ride it. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt, which stated no prepare-only
mode.

`bin/preflight` ran **mid-session rather than first** — disclosed, not
excused. Its one finding at that point was this session's own
uncommitted work; everything else green: the last five CI runs on `main`
all green at `e81f7026`, repo root `/home/mg/src/ehr-testing-tools` (not
under `/mnt/`), `core.fileMode` true, `core.ignorecase` unset, local HEAD
matching `origin/main`, HEAD not tagged `stable-*`.

## What this session was asked to do, and why it did not

The prompt asked for a six-step build closing
`roadmap.md#oru-control-id-collision`: give `:result-available` a
four-part MSH-10 keyed on its own `:order-event-id`, prove it red then
green, bracket it, close the row.

**Step 1's premise did not hold.** Before writing the red test I
measured the population it was supposed to name, and the six duplicate
MSH-10s in `seed-424242-clinic-decade` are not `:result-available` — that
corpus contains no `:result-available` event at all. The session halted
under `docs/dev/way-of-working.md` §2 and put the finding to the author
rather than adapting the prompt to fit.

**Author ruled 2026-09-23, two questions, in the same exchange:**

- **Scope: halt — characterize only.** Land nothing behavioural; write
  the measurement up; return the key shape to the design channel.
- **ADR form: A, a new ADR-0181** (the prompt had left `[A | B]`
  unfilled).

Everything below is that ruling executed.

## What landed

One commit. `notes/adr/0181-msh-10-collisions-are-a-default-branch-property.md`
(**Proposed**, enacting nothing), its index line in `notes/ADRs.md`, the
roadmap row rewritten, seven readers corrected, and
`.agents/state-derived.md` regenerated.

## The measurement

Seven roots, each at its own committed opts — the four `gated-runs` of
`ehrt.sim.run-test` plus three demo scenarios at the seed and arrival
count their own shipped exercisers run. `control-id-for` applied to every
ground-truth event; duplicates by whole id.

Three findings, all in ADR-0181 with the tables:

1. **The rowed corpora carry no `:result-available` at all.**
   `seed-424242`'s 6 duplicates in 2 groups and the clinic-decade demo's
   10 in 3 are `:observation`/`:diagnostic-report`, which share the
   default `mrn-trigger-t` branch and carry no `:order-event-id`
   (measured absent, not nil). Three event types render `ORU^R01`, not
   one. **Applying the ruled key shape to `:result-available` alone
   leaves both corpora unchanged** — measured, by re-keying the log both
   ways in the probe, not reasoned.
2. **The rowed collision has an empty population everywhere.**
   `:result-available` against itself on one `(active-mrn, t)`: zero in
   all seven roots, including the 1,028 result events of the dense-7500
   750-arrival cell. Real by construction, never once produced.
3. **The live defect is five classes wide and mostly ADT.** That cell
   carries 43 duplicate groups / 86 messages: 25 A02 `:transfer`, 8 A12
   `:cancel-transfer`, 2 A40 `:merge`, 8 R01 cross-type. Thirty-five of
   forty-three are not ORU at all.

So the defect is a property of the default branch — `mrn-trigger-t` is
non-injective wherever two events of one trigger family land on one
patient in one second — and `:merge` is the sharpest reading of it,
because `:merge` already *has* a discriminator arm and still collides.

## Judgment calls, and their ratification status

1. **Halting before writing step 1's red test** — RATIFIED by the author
   in-session, and the scope ruling followed.
2. **Correcting seven readers' prose in the characterization commit**
   rather than leaving them for the fix session — NOT separately ruled.
   The halt ruling said land nothing *behavioural*; seven docstrings and
   one roadmap row asserted a population this session measured false, and
   leaving them would have re-seeded the exact premise mismatch that
   halted this session. Prose only: **no assertion, no fixture value and
   no test-name change.** Reversible by revert if the author disagrees.
3. **Keeping the anchor `oru-control-id-collision`** although `oru` now
   misnames the row. Ten citations resolve through it; renaming is a
   ten-file rewrite this session has no ruling for. Named as a retained
   misnomer in the row itself.
4. **Leaving the conformance gate's `-R01-` assertion message alone**
   ("a NEW collision shape appeared, outside the rowed one"), although
   it is corpus-limited. The gate's docstring now says so at length;
   changing the message is a gate edit and outside the fence.

## Verification

- `clojure -M:poly check` — **OK**.
- `bin/ascii-scan` — **exit 0**.
- `make docsgen` — exit 0; regenerated `.agents/state-derived.md` only
  (ADR files 178 → 179; `:onboarding` 1,464 → 1,472 lines, headroom
  66 → 58, budget untouched at 1,530, so the ADR-0143 ratchet holds).
  `git diff --exit-code` clean afterwards.
- Namespace load check on all four edited `.clj` files, `:reload` —
  clean, `control-id-for`'s docstring intact.
- `make test`, unpiped, on the payload tree, `MAKE_EXIT` recorded by the
  wrapper, which ends `exit "$MAKE_EXIT"` — **`MAKE_EXIT=0`, 428 `Test
  results:` lines, 28,561 passes, 0 failures, 0 errors.** Those are the
  same four figures the 2026-09-12 pre-push run recorded at `8ab8fd59`,
  to the assertion. **That identity is itself the proof this session
  wanted**: a prose-only commit must move no assertion count, and none
  moved.

**NO REGRESSION-ORACLE CLAIM IS MADE HERE, and none is needed.** This
commit changes no executable form: every `.clj` edit is inside a
docstring or a `testing` string. The byte-identity that matters is
already asserted by committed gates this suite run exercises —
`ehrt.sim.run-test` pins the gated roots' ground-truth digests, including
`seed-424242-clinic-decade` — and those are what went green, not
`bin/regression-oracle`, which was not run. Naming the weaker method
rather than dressing it up, per ADR-0030 J2.

## The downstream notice the prompt asked for

**Moot, and deliberately so.** The prompt asked this record to carry a
line telling the 32k-subject retained corpus at `e81f7026` the new commit
sha and key shape. **No key shape landed and no emitted byte moved**, so
there is nothing for that corpus to adopt: it stands unchanged against
this commit exactly as it stood against `e81f7026`. When a fix session
does land a shape, the notice is owed then — and ADR-0181 records that
ground truth will not move under any of the three candidates, because
MSH-10 is minted at emission out of fields the log already carries.

## Findings for the design channel

1. **The key-shape question is open and now costed.** ADR-0181 leaves
   three candidates standing — per-arm discriminators, a uniform ordinal
   on the default branch, the log index — with the threading problem that
   makes (2) and (3) an `event->messages` API change rather than an arm
   change.
2. **Four collision classes have been live and unseen.** The only gate
   that measures duplicates on a real corpus runs one root
   (`seed-424242`), where every duplicate happens to be ORU. The A02/A12/
   A40 classes are visible in a cell no gate reads. Whether the
   conformance gate should widen its population is a design question this
   session did not answer.
3. **The row's own numbers were stale as well as misattributed.** It
   priced the clinic-decade demo at one duplicate; it carries ten, in
   three groups.

## HEAD landed

| sha | commit |
|---|---|
| `98281818` | ADR-0181, the roadmap correction, and seven readers |
| (this one) | this record and the prompt archive |

Base `e81f7026`. CI green on the tip is the close marker.
