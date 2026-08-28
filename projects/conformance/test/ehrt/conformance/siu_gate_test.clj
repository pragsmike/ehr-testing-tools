(ns ehrt.conformance.siu-gate-test
  "ARC 4 SWEEP 4 (`notes/adr/0175-arc-4-emission-add-ons.md` ruling B1),
  the two claims neither `components/sim` nor `components/judge` can
  make alone, over ONE laddered-free SIU corpus:

  (a) THE SAMPLER ABSORBS SIU WITH NO CODE CHANGE. Design (h)'s policy
      splits the wire into SKELETON families -- gated in full, always --
      and ADD-ON families, which are capped per stratum.
      `skeleton-message-types` is DERIVED from `message-type-registry`,
      so the four SIU families became skeleton the instant they got
      entries, and every SIU is gated in FULL with no list to widen.
      That is a claim about a derivation, and a derivation is exactly
      the kind of thing that reads as obviously true and is worth
      executing once.

  (b) THE JUDGE TIER RESOLVES EVERY SIU TO A REAL v2.4 STRUCTURE. Sweep
      1 flipped MSH-12 to \"2.4\" and made `gate v2` non-vacuous over
      this project's own output for the first time; sweep 4 is the first
      to put a NON-ADT family on that wire, so the flip's payoff has to
      be re-earned rather than inherited. `2.4.properties` maps
      `SIU_S13` .. `SIU_S24` and `SIU_S26` onto `SIU_S12`, which is why
      four triggers resolve to one structure and why zero of them fall
      back to `GenericMessage`.

  IT PARSES THROUGH THE JUDGE'S OWN CONTEXT, not a copy of it:
  `#'hapi/new-context` is the private constructor
  `ehrt.judge-v2-hapi.v2/execute` itself calls
  (`ehrt.conformance.v2-structure-resolution-test`'s own reasoning)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.judge.interface :as judge]
            [ehrt.judge-v2-hapi.v2 :as hapi]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.interface :as sim]))

(def ^:private opts
  "A run wide enough that the claims below are about a CORPUS. Every
  scheduling rate is well clear of zero so all four triggers occur, and
  chatter is on so the strata under assertion include a REAL add-on
  family beside SIU's four skeleton ones -- a sampling claim whose
  corpus contains nothing samplable would prove only that nothing was
  capped because nothing could be."
  {:seed 202 :patients 40 :arrival-gap 90
   :reference-date "2024-01-01" :utc-offset "+00:00"
   :emit "hl7"
   :encounters true
   :scheduling {:scheduled-fraction 0.7 :lead-time-days [3 21]
                :no-show-rate 0.15 :reschedule-rate 0.25 :cancel-rate 0.15
                :follow-up {:rate 0.6 :interval-days [30 120]}}
   :siu {}
   :persons {:count 40 :years 20}
   :chatter {:demographic-update 1.0 :coverage-change 1.0 :registered 1.0
             :restatement {:rate-per-patient-day 1.0}}})

(def ^:private siu-triggers ["SIU^S12" "SIU^S14" "SIU^S15" "SIU^S26"])

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))

(defn- resolved-structure
  "The class HAPI resolves `er7` to, through the judge's own context.
  Returns the class, or `{:threw <message>}` when the parse itself
  fails -- the shape a primitive-validation failure takes, because
  `PipeParser` validates primitives while parsing rather than after."
  [er7]
  (try
    (class (.parse (.getPipeParser (#'hapi/new-context)) er7))
    (catch Exception e {:threw (.getMessage e)})))

(def ^:private corpus (delay (:messages (:payload (sim/run-command opts)))))

(deftest siu-lands-in-skeleton-strata-and-is-gated-in-full
  (let [messages @corpus
        entries (mapv (fn [m] (assoc (judge/sampling-header m) :path (str (hash m)))) messages)
        {:keys [selected strata]} (judge/stratified-selection
                                   entries
                                   {:skeleton-types emit-hl7/skeleton-message-types :cap 5})
        counts (into {} (map (juxt key (comp :n val))) strata)]
    (testing "the corpus really carries all four SIU triggers plus a samplable add-on"
      (is (pos? (count messages)))
      (doseq [t siu-triggers]
        (is (pos? (get counts t 0)) t))
      (is (pos? (apply + (keep (fn [[t n]] (when (#{"ADT^A08" "ADT^A31" "ADT^A28"} t) n))
                               counts)))
          "chatter, the add-on half"))
    (testing "every SIU stratum reports n and gated, and gates in FULL"
      (doseq [t siu-triggers]
        (let [{:keys [n gated add-on?]} (get strata t)]
          (is (= n gated) (str t " is a skeleton family: every message of it is gated"))
          (is (false? add-on?)
              (str t " is derived from the registry, so it is never sampled")))))
    (testing "and the add-on families ARE capped, so this run exercises both halves"
      (is (some (fn [[_ {:keys [n gated add-on?]}]] (and add-on? (< gated n))) strata)
          "at cap 5 with chatter on, at least one add-on stratum must be truncated -- if
           none is, the cap is doing nothing and the full/sampled contrast is untested"))
    (is (= (count selected) (apply + (map :gated (vals strata))))
        "no silent caps: the selection size is exactly the sum of the printed per-stratum
         gated counts")
    (testing "the per-stratum census is REPORTED, not just counted"
      (println "  SIU strata:")
      (doseq [line (judge/render-strata (select-keys strata siu-triggers))]
        (println "   " line))
      (is true))))

(deftest every-siu-resolves-to-the-real-v2-4-siu-s12-and-none-is-generic
  (let [messages @corpus
        siu (filterv #(str/starts-with? (msh-9 %) "SIU^") messages)
        resolutions (mapv (juxt msh-9 resolved-structure) siu)
        threw (filter (comp map? second) resolutions)
        generic (filter #(and (class? (second %))
                              (str/includes? (.getName ^Class (second %)) "GenericMessage"))
                        resolutions)]
    (testing "sanity: there is an SIU corpus to speak about (R-empty-population-is-red)"
      (is (<= 30 (count siu))
          (str "this claim is only worth making at corpus scale; " (count siu) " SIU messages"))
      (is (= (set siu-triggers) (set (map first resolutions)))
          "all four triggers, or three quarters of the claim is vacuous"))
    (testing "every SIU parses -- SCH's EI/CE/TQ fields are as much subject to v2.4's
              primitive rules as PID-13 was, and PID-13 is what killed 346 of the probe
              corpus's 747 before sweep 1 rendered it `(NNN)NNN-NNNN`"
      (is (empty? threw)
          (str (count threw) " of " (count siu) " SIU messages failed to parse. First: "
               (pr-str (first threw)))))
    (testing "none falls back to GenericMessage"
      (is (empty? generic)
          (str "Offenders: "
               (pr-str (map (fn [[t c]] [t (.getName ^Class c)]) generic)))))
    (testing "and ALL FOUR TRIGGERS resolve to the SAME structure, `SIU_S12` -- which is
              what `2.4.properties` says and is the reason one builder serves four kinds"
      (is (= {"ca.uhn.hl7v2.model.v24.message.SIU_S12" (count siu)}
             (frequencies (map (fn [[_ c]] (.getName ^Class c)) resolutions)))))
    (testing "the resolved structures are reported, not just counted"
      (println "  resolved v2.4 structures over" (count siu) "SIU messages:"
               (pr-str (into (sorted-map)
                             (frequencies (map (fn [[t c]]
                                                 [t (when (class? c) (.getSimpleName ^Class c))])
                                               resolutions)))))
      (is true))))
