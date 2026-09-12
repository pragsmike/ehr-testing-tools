# 2026-09-12 -- errata: the person-encounter guard's citation

Ceremony mode: R30 (commit and push at each checkpoint), taken from the prompt,
which states no prepare-only exception. Prompt archived at
[`../prompts/2026-09-12-errata-opener-guard.md`](../prompts/2026-09-12-errata-opener-guard.md).
No ADR was written and none was owed: nothing here decides anything. No roadmap
row moved.

## 1. Scope

The 2026-09-10 same-instant-opener fix (`c282409f`, tip `8ab8fd59`, CI green)
left two errata in its own record, and this session fixes both in place.

1. `decide :person-encounter`'s comment cited `prelude`'s `encounter-free?` as
   the static half of its guard. That var does not exist; the static half is
   `prelude`'s LOCAL `clinically-idle?` (`run.clj:655-663`), which tests that the
   patient's compiled steps are empty and that every authored step is a
   `:registered`. The 2026-09-10 record named this finding and fenced it out.
2. That record states the fix's premise as "a valid corpus never has two pending
   openers at ONE INSTANT". The mechanism is wider: `run.clj:1721-1728` adds a
   patient to `reserved'` whenever an opener heads their remaining tail,
   regardless of `advance`, so the reservation also holds across a positive delay
   ahead of an unguarded authored opener.

The author ruled "fix the docs" (2026-09-12). Shape A -- is a same-minute
reselection a second arrival at all? -- is PARKED, not ruled, and is recorded as
parked in both the corrected record and here.

## 2. Red-to-green evidence

A docs-and-comments session: its proof is the suite staying green and the payload
untouched.

* `bin/preflight` before the first edit: **exit 0, no findings** (last five CI
  runs on main green, tree clean, HEAD matches `origin/main`; the standing
  "HEAD is not tagged stable-*" disclosure).
* `clojure -M:poly check`: **OK**, after the comment edit.
* The src diff is comment-only, proven mechanically rather than asserted:
  `git diff 8ab8fd59 -- components/ | grep '^[+-]' | grep -v '^+++\|^---' |
  grep -v '^[+-]\s*;;'` prints nothing. R-full-suite-before-push's docs-only
  exemption applies on that evidence, and CI runs the suite at the tip.
* `make docsgen && git diff --exit-code`: clean, with both new files in place.
* `grep -rn "encounter-free?" components/ bases/ projects/`: empty.

## 3. Judgment calls and their ratification status

(a) **The replacement comment says what `clinically-idle?` tests, not just its
name.** The prompt asked for both; the wording names the two halves the `let`
binding actually reads (compiled steps empty, every authored step `:registered`)
and flags that it is a local binding rather than a var, which is the fact that
made the original citation ungreppable. Unratified, low stakes: comment text.

(b) **The correction section is appended below section 5 and rewrites nothing.**
The prompt's invariant is additions only, and line 72's original sentence stands
untouched -- the correction states the wider mechanism beside it rather than
replacing it, because that sentence is a true statement of what the bracket
tested. Per the prompt.

(c) **The wider arm is called CORRECT rather than merely harmless.** Stated with
its reason: a gated opener refused in front of a committed unguarded one is the
same answer the log-time invariant gives, and no shipped pathway puts a `:delay`
before an opener, so the arm is unreachable in every corpus the bracket digests.
Unratified.

## 4. Findings and HEAD landed

* Both errata closed. Nothing new was found in the cited sites.
* Shape A remains PARKED, not ruled, with the author's own reason recorded: with
  shape B landed, a pre-loop drop would produce the same log.
* The consumer reply carrying both disclosures was sent 2026-09-12, before this
  session; this repo's own copy is what landed here.
