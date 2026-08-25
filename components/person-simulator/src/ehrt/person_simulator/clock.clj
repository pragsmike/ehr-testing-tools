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
  "A person's age in whole years at walk year `y`. `:age` is the
  Persona's own age at `origin-year` -- year 0 for a person present at
  t0, and the BIRTH year for a newborn, whose derived Persona carries
  `:age` 0 as of the delivery. Ageing is then addition, and the
  Persona's DOB is not re-read: no second calendar anchor is
  introduced.

  The origin is a parameter and not an assumption because assuming it
  was zero is a bug this arc actually shipped and caught: a newborn
  whose `:age` 0 was read at absolute year 12 came out twelve years
  old the year after its birth, drew adult hazards, and joined
  households as a minor -- which is precisely what limitations row 3's
  gate went red on."
  ^long [persona ^long origin-year ^long y]
  (+ (long (:age persona)) (- y origin-year)))
