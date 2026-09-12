# Session prompt -- errata: the person-encounter guard's citation

Repo `ehr-testing-tools`, ext4 clone of record `/home/mg/src/ehr-testing-tools`,
HEAD at session start `8ab8fd59`, working tree clean. Ceremony mode: R30, the
standing default -- the prompt states no prepare-only exception. Paired record:
[`../session-records/2026-09-12-errata-opener-guard.md`](../session-records/2026-09-12-errata-opener-guard.md).

## The prompt, verbatim

> # Errata: the person-encounter guard's citation, and what the reservation actually holds
>
> Context: the 2026-09-10 same-instant-opener fix (c282409f, tip 8ab8fd59, CI green) left two
> errata in its own record. (1) `decide :person-encounter`'s comment cites `prelude`'s
> `encounter-free?` as the static half of its guard; that var does not exist -- the static half
> is `prelude`'s LOCAL `clinically-idle?`. (2) The record states the fix's premise as "a valid
> corpus never has two pending openers at ONE INSTANT". The mechanism is wider: `reserved'` is
> added whenever an opener heads a patient's remaining tail, regardless of `advance`, so it also
> holds across a positive delay ahead of an unguarded authored opener. No shipped pathway puts a
> `:delay` before an opener, so the bracket's IDENTICAL stands; the premise is narrower than the
> code. Consumer reply carrying both disclosures was sent 2026-09-12; nothing here changes
> behaviour.
>
> Read first:
> - components/sim-engine/src/ehrt/sim_engine/decide.clj:540-548
> - components/sim-engine/src/ehrt/sim_engine/run.clj:655-663 (clinically-idle?), 1543, 1613, 1721-1728
> - components/sim-engine/src/ehrt/sim_engine/encounters.clj:49-73
> - .agents/session-records/2026-09-10-same-instant-openers.md:68-73, 160-175
> - .agents/session-records/README.md:14-20, .agents/prompts/README.md
> - .agents/rulings.md:274-280 (R-full-suite-before-push, R-preflight-fail-closed)
>
> Author ruling 2026-09-12: "Fix the docs." Shape A (is a same-minute reselection a second
> arrival at all?) is PARKED, not ruled: with B landed a pre-loop drop would produce the same log.
>
> Steps:
> 1. bin/preflight before the first edit.  Invariant: exit 0.  Gate: its own exit code.
> 2. decide.clj:543-548 -- replace the `encounter-free?` citation with `prelude`'s local
>    `clinically-idle?` and say what it tests: no compiled steps, nothing authored past
>    `:registered`. Comment lines only; touch no form.
>    Invariant: `grep -rn "encounter-free?" components/` is empty.
>    Gate: clojure -M:poly check OK.
> 3. Append a dated "Correction 2026-09-12" section to the 2026-09-10 record, below section 5,
>    stating the wider premise above with the run.clj site, and that the wider case is CORRECT
>    (a gated opener is refused in front of a committed unguarded one rather than opening a stay
>    the log would pass only by luck). Do not rewrite line 72; the original sentence stands.
>    Invariant: `git diff` on that file is additions only.
>    Gate: the section reads against the cited lines.
> 4. Same section: shape A parked, not ruled, one sentence with the reason.
>    Invariant: the word "ruled" does not appear next to shape A except as "not ruled".
>    Gate: reread.
> 5. Record .agents/session-records/2026-09-12-errata-opener-guard.md (short) and archive this
>    prompt at .agents/prompts/2026-09-12-errata-opener-guard.md; make docsgen.
>    Invariant: `make docsgen && git diff --exit-code` clean after both files exist.
>    Gate: that command.
> 6. Confirm the src diff is comment-only:
>    `git diff 8ab8fd59 -- components/ | grep '^[+-]' | grep -v '^+++\|^---' | grep -v '^[+-]\s*;;'`
>    must print nothing; then R-full-suite-before-push's docs-only exemption applies. If it prints
>    anything, run the full suite unpiped with MAKE_EXIT recorded before pushing -- that is the
>    rule, not a choice. Push; gh run view the tip; CI green is the close marker.
>

## Deviation record

1. **The correction section names a third site the prompt did not.**
   `encounters/encounter-openable?`'s own `:pending-openers` test
   (`encounters.clj:49-73`, which the prompt did list as reading) is cited
   alongside `run.clj:1721-1728`, because the reservation's width is only
   observable where it is WRITTEN and where it is READ together. No other
   deviation of substance.

2. **The comment replacement runs to eight lines where the original ran to
   six.** The prompt asked for both the corrected name and what it tests; that
   does not fit the original six. Comment lines only, no form touched -- the
   src diff is mechanically proven comment-only in step 6.

3. **Nothing else in the 2026-09-10 record was touched**, line 72 included: the
   correction is appended below section 5 and `git diff` on that file is
   additions only.
