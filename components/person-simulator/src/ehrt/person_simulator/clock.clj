(ns ehrt.person-simulator.clock
  "The one time unit this component speaks.

  `ehrt.sim-engine.engine`'s own time doctrine (`sim/ADR-0011`, M2a):
  every event's `:t` is integer SECONDS from run start. Person events
  are events in that same log, so they carry the same clock -- there is
  no second calendar here, and `initial-persona`'s `t0` instant and a
  compiled trajectory's death instant are both read in these units.

  A year is 365 days flat. No leap handling, and none is owed: the
  hazards this constant divides a lifetime into are authored to one
  significant figure (see `hazards`), so a quarter-day per year is
  three orders of magnitude below the noise in the rate itself. What
  it buys is that a person's year boundaries are exact multiples of
  one constant, which is what makes the fixed-consumption law
  countable."
  (:import [java.time LocalDate]))

(def seconds-per-day 86400)
(def days-per-year 365)
(def seconds-per-year (* seconds-per-day days-per-year))

(defn days ^long [n] (* (long n) seconds-per-day))

(defn dob-epoch-day
  "A persona's `:dob` as an epoch day. `sim-model`'s own Persona
  carries `:dob` as an ISO date string; the person process needs it as
  a number to age a person across a horizon."
  ^long [persona]
  (.toEpochDay (LocalDate/parse (:dob persona))))

(defn age-at-year
  "A person's age in whole years at year `y` of the walk. `:age` is the
  Persona's own age at its t0, computed against
  `sim-model/reference-birth-year`, so ageing is addition -- the
  Persona's DOB is not re-read here, and no second anchor is
  introduced."
  ^long [persona ^long y]
  (+ (long (:age persona)) y))
