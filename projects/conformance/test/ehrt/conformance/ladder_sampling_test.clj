(ns ehrt.conformance.ladder-sampling-test
  "ARC 4 SWEEP 3 (`notes/adr/0175-arc-4-emission-add-ons.md` design (b),
  ruling B1), the one assertion neither brick can make alone: the
  SAMPLER absorbs the ladder's new volume WITH NO CODE CHANGE.

  `components/judge` deliberately knows no emitter (its `sampling`
  namespace says so in its own docstring: the classification set is the
  CALLER's), and `components/sim` deliberately knows no judge. This
  project composes both, so it is where a real laddered wire can be
  handed to the real selector.

  WHAT IT PROVES, and why it is worth a namespace. Design (h)'s policy
  splits the wire into SKELETON families -- gated in full, always --
  and ADD-ON families, which are capped per stratum.
  `skeleton-message-types` is DERIVED from `message-type-registry`, so
  ORM^O01 and ORU^R01 have been skeleton since M3 and a ladder rung is
  gated in FULL from the moment it exists. That is a claim about a
  derivation, and a derivation is exactly the kind of thing that reads
  as obviously true and is worth executing once."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.judge.interface :as judge]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.interface :as sim]))

(def ^:private opts
  {:seed 202 :patients 8 :arrival-gap 90
   :pathways [{:pathway {:name "workup"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :order :profile :cbc}
                                 {:type :delay :from 180 :to 900}
                                 {:type :order :profile :bmp}
                                 {:type :discharge}]}
               :weight 1}]
   :emit "hl7"
   :ladders {:rungs [0.25 0.5] :order-rungs [0.1]}
   ;; Chatter too, so the strata under assertion include a REAL add-on
   ;; family beside the ladder's two skeleton ones. A sampling claim
   ;; whose corpus contains nothing samplable would prove only that
   ;; nothing was capped because nothing could be.
   :persons {:count 16 :years 20}
   :encounters true
   :chatter {:demographic-update 1.0 :coverage-change 1.0 :registered 1.0
             :restatement {:rate-per-patient-day 1.0}}})

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))

(deftest the-ladder-s-volume-lands-in-skeleton-strata-and-is-gated-in-full
  (let [{:keys [messages]} (:payload (sim/run-command opts))
        entries (mapv (fn [m] (assoc (judge/sampling-header m) :path (str (hash m)))) messages)
        {:keys [selected strata]} (judge/stratified-selection
                                   entries
                                   {:skeleton-types emit-hl7/skeleton-message-types :cap 5})
        counts (into {} (map (juxt key (comp :n val))) strata)]
    (testing "the corpus really carries all three kinds of traffic"
      (is (pos? (count messages)))
      (is (pos? (get counts "ORM^O01" 0)) "orders, plus their rungs")
      (is (pos? (get counts "ORU^R01" 0)) "results, plus their rungs")
      (is (pos? (apply + (keep (fn [[t n]] (when (#{"ADT^A08" "ADT^A31" "ADT^A28"} t) n))
                               counts)))
          "and chatter, the add-on half"))
    (testing "the ORM/ORU strata report n and gated, and gate in FULL"
      (doseq [t ["ORM^O01" "ORU^R01"]]
        (let [{:keys [n gated add-on?]} (get strata t)]
          (is (= n gated) (str t " is a skeleton family: every message of it is gated"))
          (is (false? add-on?) (str t " is derived from the registry, so it is never sampled")))))
    (testing "the ladder's rungs are IN those strata, not somewhere else"
      (let [rung-count (count (filterv #(= 4 (count (str/split
                                                     (nth (str/split (first (str/split % #"\r")) #"\|") 9)
                                                     #"-")))
                                       messages))
            base-orm+oru (count (filterv #(#{"ORM^O01" "ORU^R01"} (msh-9 %)) messages))]
        (is (pos? rung-count))
        (is (= base-orm+oru (+ (:n (get strata "ORM^O01")) (:n (get strata "ORU^R01")))))))
    (testing "and the add-on families ARE capped, so this run exercises both halves"
      (is (some (fn [[_ {:keys [n gated add-on?]}]] (and add-on? (< gated n))) strata)
          "at cap 5 with chatter on, at least one add-on stratum must be truncated -- if none
           is, the cap is doing nothing and the full/sampled contrast is untested"))
    (is (= (count selected)
           (apply + (map :gated (vals strata))))
        "no silent caps: the selection size is exactly the sum of the printed per-stratum
         gated counts")))
