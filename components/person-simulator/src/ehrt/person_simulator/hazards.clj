(ns ehrt.person-simulator.hazards
  "The bespoke hazard-rate processes (`rulings.md#R-mix-1`: never GMF
  modules). Every rate below is a rate PER PERSON-YEAR.

  Every rate in this namespace is authored-provisional -- a
  general-knowledge order of magnitude, with no table read and no
  source cited (author ruling E1, 2026-08-25). Each carries its own
  marker in its own comment block, and
  `ehrt.docs-tooling.person-simulator-charter-test`'s drift lint holds
  every one of those markers to a covering row in
  `docs/limitations.md` (row 9). Promoting any of these to fact
  without a named measurement would be the unearned-specificity class
  this repo's error ledger already tracks.

  Fixed consumption is a law here, not an aspiration: every hazard
  draws its per-interval variate whether or not the event fires, and
  every branch draws whether or not it is taken -- the same law
  `ehrt.sim-engine.engine/assign-pathway`, `ehrt.sim-engine.churn/
  roll-gap` and `ehrt.sim-model.persona/persona`'s own 13-draw
  contract already state."
  (:require [ehrt.person-simulator.clock :as clock]))

;; --- the rates ------------------------------------------------------------

;; PROVISIONAL residence-move rate. ~0.11 moves per person-year overall,
;; age-tilted: highest 20-34, falling steeply after 55. The tilt is the
;; part with real traffic consequence -- a population whose moves are
;; flat in age produces an A08 stream with the wrong shape even when
;; the total is right -- and it is the part with no source at all.
(def residence-move-base-rate 0.11)

(defn residence-move-rate
  "The residence-move hazard at `age`, per person-year."
  [age]
  (* residence-move-base-rate
     (cond (< age 18) 0.55
           (< age 20) 1.4
           (< age 35) 2.2
           (< age 55) 0.85
           :else 0.30)))

;; PROVISIONAL employment-change rate. ~0.20 per working-age
;; person-year, with the retirement hazard concentrated at 62-67. Only
;; the working-age window is even approximately knowable without a
;; table; the retirement concentration is an authored shape.
(def employment-change-base-rate 0.20)

(def working-age-min 18)
(def working-age-max 67)
(def retirement-window [62 67])

(defn employment-change-rate
  [age]
  (cond (< age working-age-min) 0.0
        (> age working-age-max) 0.0
        (<= (first retirement-window) age (second retirement-window)) 0.55
        :else employment-change-base-rate))

;; PROVISIONAL name-change rate, ~0.02 per adult person-year. Adult
;; only, because a minor's legal name change reaches the same PID-5 by
;; the same path and modelling it separately would buy nothing.
(def name-change-rate 0.02)

;; PROVISIONAL identity-correction rate, ~0.01 per person-year. This
;; one is NOT a world rate and is labelled as one on purpose: it is an
;; authored registrar-error knob, and a tester dialling it is dialling
;; how much correction traffic a corpus carries, not a claim about how
;; often clerks mistype a date of birth.
(def identity-correction-rate 0.01)

;; PROVISIONAL household-event rate, ~0.08 per adult person-year --
;; forming, joining or leaving a household. Adult only. One rate for
;; three transitions because the branch a person takes is determined by
;; the household state they are already in, not by a second hazard.
(def household-rate 0.08)

;; PROVISIONAL pregnancy rate, ~0.055 per person-year, and ZERO
;; anywhere outside `:sex :female` and age 15-44. The band edges are
;; hard, not tapered: a tapered band is a second authored shape on top
;; of an authored rate, and the traffic difference is nil.
(def pregnancy-rate 0.055)
(def pregnancy-age-band [15 44])

;; PROVISIONAL gestation: 280 days from `:pregnancy` to `:delivery`,
;; plus a jitter draw uniform over +/- 21 days. The delivery is NOT a
;; hazard -- it is deterministic given its pregnancy plus this one
;; jitter variate, which is what makes limitations row 11's bijection
;; a property of the construction rather than of the rates.
(def gestation-days 280)
(def gestation-jitter-days 21)

;; PROVISIONAL mortality, Gompertz-shaped: `a * exp(b * age)` per
;; person-year. ADR-0172 names three anchors -- ~0.0009 at 30, ~0.02 at
;; 70, ~0.15 at 90 -- and they are NOT collinear in log space, so no
;; two-parameter Gompertz passes through all three. This fit takes the
;; OUTER two (30 and 90) and lands 0.027 at 70 against the charter's
;; 0.02. Recorded here rather than smoothed away: an authored curve
;; that silently misses one of its own anchors is exactly the folklore
;; ruling E1's marker mechanism exists to keep visible.
(def mortality-a 6.97e-5)
(def mortality-b 0.08527)

(defn mortality-rate
  [age]
  (* mortality-a (Math/exp (* mortality-b (double age)))))

;; PROVISIONAL occupational-injury rate, ~0.028 per EMPLOYED
;; person-year and zero otherwise. The conditioning is the load-bearing
;; half: this hazard is the person process's own contribution to the
;; ADR-0107 injuries family, and an injury drawn for an unemployed or
;; retired person would put an arrival cause on the engine that the
;; person's own state contradicts.
(def occupational-injury-rate 0.028)

;; PROVISIONAL residence-loss rate, ~0.006 per HOUSED person-year, and
;; zero for a person already unhoused or in a household. Both halves of
;; the conditioning carry more than the number does. Zero-when-unhoused
;; is what makes the residence sum a two-state process rather than a
;; ratchet; zero-when-in-a-household is limitations row 13, and it is
;; what keeps ruling B1's propagation honest -- a head's move is copied
;; to every member verbatim, so a member who could lose housing on
;; their own would receive copies that report a change they did not
;; have. The rate itself is a defect-surface knob dressed as a world
;; rate, and is labelled as neither more nor less.
(def residence-loss-rate 0.006)

;; PROVISIONAL rehousing rate, ~1.2 per UNHOUSED person-year -- the
;; hazard that returns an unhoused person to a `places.edn` row. It is
;; drawn from the SAME variate a housed person's move uses, with only
;; the RATE conditioned on the state: one variate, two rates. A second
;; hazard would be a draw whose count depended on the person's housing
;; status, which the fixed-consumption law forbids. Deliberately much
;; larger than the move rate and still an order of magnitude, not a
;; measurement: most spells are short, and a spell measured in years
;; would put a corpus's unhoused registrations all in one place.
(def rehousing-rate 1.2)

;; PROVISIONAL identity-unavailable rate, ~0.004 per person-year, and a
;; window of 1-30 days. A defect-surface knob, not a world rate: it
;; sets how much of a corpus exercises the unidentified-arrival path,
;; which is what a tester generating for an MPI consumer is actually
;; dialling.
(def identity-unavailable-rate 0.004)
(def identity-unavailable-window-days [1 30])

;; --- hazard arithmetic ----------------------------------------------------

(defn annual-probability
  "The probability that a constant-rate process with `rate` events per
  person-year fires at least once in one year: `1 - exp(-rate)`."
  ^double [rate]
  (- 1.0 (Math/exp (- (double rate)))))

(defn fires?
  "Whether the already-drawn variate `u` fires a hazard of `rate`.
  `u` is consumed by the caller whether or not this returns true --
  that is the fixed-consumption law, and it is why this takes a
  variate rather than an rng."
  [^double u rate]
  (< u (annual-probability rate)))

(defn within-year-offset
  "The instant inside the year, in seconds, at which a fired hazard
  landed -- derived from the SAME variate that fired it, by rescaling
  `u` over the firing interval. No second draw: a within-year time
  drawn separately would be a variate whose count depends on whether
  the hazard fired, which the fixed-consumption law forbids."
  ^long [^double u rate]
  (let [p (annual-probability rate)]
    (if (<= p 0.0)
      0
      (long (* (min 0.999999 (/ u p)) clock/seconds-per-year)))))
