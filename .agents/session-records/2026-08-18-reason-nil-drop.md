# 2026-08-18 -- S-1 lands under its own bump (event contract 1.1.0 -> 1.2.0) and the deprecation clause gains its no-external-consumer waiver

Autonomous session under R30. Reasoning-of-record: `notes/adr/0151-reason-nil-drop.md`.
Prompt archived at `.agents/prompts/2026-08-18-reason-nil-drop.md`.

## Step 0

`bin/preflight`: last five CI runs on `main` all green; edit root on
ext4, not under `/mnt/`; tree clean including untracked; local HEAD
`d4e73fc` == `origin/main`; last tag
`stable-20260818-event-log-shape-defects` @ `eeb0299`; HEAD untagged,
**no tag owed**.

One finding better than the prompt's Context stated: it recorded CI
green at `eeb0299`; the addendum commit `d4e73fc` is green too, so the
baseline tip is itself CI-verified rather than only its parent.

Baseline `make test`, unpiped, `MAKE_EXIT=0`: **346** blocks / **3,926**
tests / **17,638** assertions, all 346 blocks zero-failure --
reconciling exactly against ADR-0150's addendum. `poly check` OK.
Reading sets from generated `state-derived.md`: `:corpus` 1801/2045,
`:docs` 708/785, `:judge` 895/1000, `:onboarding` 1396/1530, `:sim`
1247/1405 -- all under budget, none touched by this session.

Four predictions written into the ADR before any `src` edit.

## The finding, and the author ruling it produced

**Prediction (d) as the prompt stated it -- oracle IDENTICAL -- is not
merely wrong, it is unsatisfiable by any correct implementation of
S-1.** `digest.clj:165-172`'s `engine-pair` digests
`{:ground-truth ... :hl7 ...}`; the ground-truth event log is exactly
what S-1 changes; and every `engine-pair` root is a `module-only` run,
so every encounter in it is module-compiled. A PRE-change digest run
over all 35 roots at `d4e73fc` predicted **32 movers / 3 identical**,
the 3 being the `interpreter-batch` roots that never run the engine.

Reported rather than routed around, per `docs/dev/way-of-working.md`
§2 and the build-session skill's step 12, at the point where the
answer changed what to do next -- after Step 1 (which does not depend
on it) had landed and been pushed. **Author ruling: "Proceed as
declared oracle change"**, on ADR-0142's own precedent.

## Checkpoints

**Step 1 -- the waiver, landed FIRST and alone (`43dc272`).** The
deprecation clause is waived while the event contract has no consumer
outside this repository; the waiver expires on the first such consumer
with nothing further to edit. Both surfaces stating the law moved in
the one commit (`rulings.md#R-law-surface-propagation`):
`schema-version`'s docstring and `docs/formats.md`'s Stability
section, the latter cited by footnote rather than as a bare ADR token.
The 1.1.0 note was amended in place and DATED, its original disclosure
left standing.

Gate: `make test` unpiped, `MAKE_EXIT=0`, 346 / 3,926 / 17,638 --
identical to baseline, as a docs-and-docstring change should be.

**Two self-inflicted reds on the way, both disclosed rather than
quietly fixed.** (1) The amendment quoted the sentence it replaced and
the quotation marks went unescaped into a Clojure docstring, breaking
`def` outright -- `MAKE_EXIT=2` after 7 blocks. (2) Adding an ADR file
moved the generated ADR count 148 -> 149 and `state-derived` was stale
-- `MAKE_EXIT=2` after 61 blocks. Both were caught by the full suite on
the first run that could see them, on a commit that *looked* docs-only.
`R-full-suite-before-push` earned its keep twice before this session
had changed a line of `src`.

**Step 2 -- red (`b52d8c9`).** Re-written from ADR-0150's prose (the
diff is in no tree). `Ran 1 tests containing 4 assertions. 2 failures,
0 errors.`, both failures the nil cases, both hand-authored assertions
passing. Held, and pushed with its green (`R-red-pushed-with-green`).

**Step 3 -- green, bump, one freeze (`7af2130`).** `reason-field` as a
sibling of `citation-fields`; both decides call it; `:reason`
`{:optional true}` on the two encounter kinds and nowhere else.
`classify-change` run BEFORE the bump: two rows, `:admission` and
`:outpatient-visit`, `required -> optional` -- **prediction (a) exact,
no residue.** Bumped 1.1.0 -> 1.2.0 MINOR with a dated note; ONE
`make event-schema-freeze`; `make docsgen`. Seven artifacts moved,
three traces byte-FROZEN, every changed line in a declared class, no
"other" and so no STOP.

One pinned test re-baselined SEMANTICALLY rather than by bumping its
literal: `manifest_test`'s `(= "1.1.0" ...)` was the same class
ADR-0150 corrected in `result_clock_test`, and broke on the first
legitimate bump after it. It now asserts agreement with
`engine/event-schema-version`, so no later bump has a literal here.

Gate: `make test` unpiped, `MAKE_EXIT=0`, **346 / 3,928 / 17,648**,
zero failures, reconciled PER NAMESPACE against Step 0: `engine-test`
81->82 tests / 317->321 assertions and `manifest-test` 5->6
assertions, each across two project contexts = **+2 tests, +10
assertions exactly**. No other namespace moved. `poly check` OK.

**Step 4 -- register hygiene.** Census S-1 CLOSED, dated, citing
ADR-0151, its ADR-0150 stop kept beneath rather than overwritten.
`roadmap.md#reason-nil-drop-owes-a-bump` moved to `## Done`. The
roadmap's six-line cap caught the first draft of that row at seven
lines; compacted, not exempted. Nothing else re-rowed.

## Verification

**Regression oracle -- a DECLARED change, predicted exactly.**
`bin/regression-oracle d4e73fc HEAD`, exit 1, `DIFFERS`, soundness line
`IDENTICAL outside the (ns ...) form` so both sides ran the same
instrument. **Actual 32 movers / 3 identical == predicted 32 / 3, no
residue in either direction**; the 3 are `appendicitis`,
`sore-throat`, `ear-infections`.

A second 35-root digest at HEAD, compared half by half against the
Step 0 one, shows **all 32 HL7 halves byte-IDENTICAL and all 32
ground-truth halves moved** -- so the prompt's emitter reasoning was
right at the artifact level, and the correction is about what the
oracle digests, not about what the emitter does.

**Manual invariance digest: re-witnessed, and it did NOT move** --
still `d00bf49c…`, `diff` silent, both out-dirs agreeing. Prediction
(c)'s manual half was wrong in the safe direction: ed-tuesday's 92
admissions all carry real reasons and it emits no outpatient visits at
all. The manual needs no edit.

**Manifest from a real run**, not a fixture:
`out/scenarios/ed-tuesday-base/manifest.edn` reads
`:event-schema-version "1.2.0"`.

`gitleaks git --staged -v` before every commit: no leaks. Every commit
message written to a file and committed with `-F`. All git run from
WSL.

## CI and tag

`gh run view 32156712987` at the close tip `8d4fac2`:
`status=completed conclusion=success`. The earlier push's run,
`32148358728` at `43dc272`, also concluded `completed / success`.

Tag PAID IN SESSION per `rulings.md#R-session-verifies-ci-via-gh` --
the tip run concluded success while this session was still open, which
is the condition the prompt's licence named. `bin/tag-ceremony
stable-20260818-reason-nil-drop 8d4fac2 <msg-file> --push`: annotated
tag created, pushed, and the remote PEELED ref verified as
`8d4fac27aacdb28e8f5d17addff2d1642f02d89b`, matching target exactly.
No tag is owed at the next Step 0.

## Receipts

    push 1   d4e73fc..43dc272   the waiver, alone and first
    push 2   43dc272..8d4fac2   red + green + close
    oracle   bin/regression-oracle d4e73fc 7af2130 -- DIFFERS (exit 1),
             32 movers / 3 identical, == predicted, no residue
    CI       32148358728 @ 43dc272 -- completed, success
             32156712987 @ 8d4fac2 -- completed, success
    tag      stable-20260818-reason-nil-drop @ 8d4fac2, paid in
             session, remote peeled ref verified

`bin/post-push-verify` ran after both pushes: remote tip matched, every
commit message in range pure ASCII, CI reported once per AR-CI-4.
