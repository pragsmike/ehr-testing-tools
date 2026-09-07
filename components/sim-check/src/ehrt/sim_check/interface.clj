(ns ehrt.sim-check.interface
  "Public seam for `ehrt.sim-check` (sim split B, M4, `notes/ADRs.md`
  ADR-0043, `.agents/plans/2026-08-04-sim-split-b-plan.md`): the
  invariant catalog (`check`), extracted from `components/sim`.
  Contents are exactly the union of what residual sim's own src-scope
  callers (`interface.clj`'s own façade delegation, all four arities;
  `run.clj`'s 3-arity call) reach today, found by fresh call-position
  grep, not by interface-design judgment (the fat-component
  disclosure's own exception, ADR-0018's from-live-consumers
  precedent) -- test-scope callers repoint to this component's
  internal `check` namespace directly (Polylith permits reaching
  implementation from test), never through this seam.

  THE SENTENCE ABOVE SAID \"all four arities\" AND THERE ARE FIVE, since
  ADR-0180 site 7 (2026-09-07): `run.clj`'s call is the 5-arity now,
  and it is the only caller of it. The union rule is unchanged -- the
  arity is here because a live src-scope caller reaches it, which is
  what put the other four here too."
  (:require [ehrt.sim-check.check :as check]))

(defn check-all
  ([ground-truth] (check/check-all ground-truth))
  ([ground-truth facility-config] (check/check-all ground-truth facility-config))
  ([ground-truth facility-config warm-up-seconds]
   (check/check-all ground-truth facility-config warm-up-seconds))
  ([ground-truth facility-config warm-up-seconds order-profiles-config]
   (check/check-all ground-truth facility-config warm-up-seconds order-profiles-config))
  ;; ADR-0180 site 7 (2026-09-07, ruling R-check-once): the 5-arity
  ;; carries `records`, `engine/replay`'s own projection, so a caller
  ;; that already has one -- `ehrt.sim.run`'s in-run self-check, over
  ;; the entries `engine/run` builds and used to discard -- hands it
  ;; over instead of making the catalog fold the log again. Added HERE
  ;; and deliberately NOT to `ehrt.sim.interface`, whose surface is
  ;; frozen at four arities by AR-M4-3: nothing needs it there.
  ([ground-truth facility-config warm-up-seconds order-profiles-config records]
   (check/check-all ground-truth facility-config warm-up-seconds order-profiles-config records)))
