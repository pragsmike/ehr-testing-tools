# Root README front example: show the generator we actually have

Session prompt, 2026-08-29 (design channel). Archived verbatim as this
session's driving prompt, per `README.md`'s own archive convention.

---

Session prompt -- root README front example: show the generator we actually have

Context. HEAD 079072e. Author-flagged 2026-08-29: `README.md:24-43` still leads with sparse clinic-decade and apologizes for itself ("the board mostly..."), and the ground-truth one-liner at :181 is `--patients 5`. Five arcs landed since either was written. Docs-only, one session, no config change: `ed-tuesday` already carries every opt-in key, and its README was regenerated at HEAD two commits ago (`06ce007`) -- its witness lines (35 encounter openers, 21 scheduled second encounters, 421 ADT^A20s, the reschedule/cancel/no-show split) are your source material. `docs/consuming-ground-truth.md` :488-503 is the measured table to quote. Nothing hand-typed: every excerpt and number comes from a run you regenerate or a shipped surface you cite.

Steps.

1. "See it run" (:24-43) leads with ed-tuesday: the run command, the `play --board` command at a rate that shows a busy day, and 3-5 lines of what the board shows that the old lead could not (beds cycling dirty->cleaning->ready, scheduled arrivals, a John Doe if one appears at the demo seed -- CHECK, do not assert). Clinic-decade moves to a one-paragraph "and the longitudinal version" with its link, no apology.
2. The ground-truth one-liner (:181) becomes the rich invocation: the ed-tuesday config with `--format ground-truth`, plus a SHORT excerpt (5-8 events, elided) from the real regenerated stream chosen to show kind variety -- an opener, a bed-status transition, a scheduled second-encounter arrival, a demographic/coverage event, whatever the actual log offers at the demo seed; each line verbatim. Link `docs/consuming-ground-truth.md` as the contract in the same breath.
3. One measured sentence with the doc's own numbers (:492: 1.3574 msg/event at 10^5 all-keys vs 0.643 with no key) cited to the doc, phrased per its own caveat (order of magnitude, not benchmark).
4. Regenerate anything the exercisers gate; `bin/demo-exerciser- ed-tuesday` green (it asserts README excerpts -- your new excerpts join its witness set if the seam exists; say either way); the hand-owned-asset tripwire handled BEFORE the push; `make docsgen` moves nothing unexpected; full `make test`; push; CI; no tag. Record: half a page -- what moved, the run that sourced each excerpt, exerciser outcome.

Fences. Docs-only. No config, no seed change, no new scenario. No number without a shipped-surface citation. No excerpt the demo seed did not produce. The old example is demoted, not deleted.
