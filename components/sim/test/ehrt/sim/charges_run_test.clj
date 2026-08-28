(ns ehrt.sim.charges-run-test
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (c),
  rulings B1/C1/E1, landed DARK): `:charges` as a `:config` passenger,
  at POPULATION scale.

  WHAT THIS RUN DOES NOT CARRY, said here rather than left to be
  noticed: `:procedure`. It is produced only by vendored GMF modules,
  and no cheap run found in this session produces one -- the two
  scenario configs' own module tails are documented as producing zero
  live encounters at their horizons. So the FT1-25 procedure line is
  witnessed at unit scale (`ehrt.sim-emit-hl7.charges-test`) and NOT
  here, and the `pos?` assertions below are only over the two line
  types this population actually has. An assertion over an empty
  population proves nothing (`rulings.md#R-empty-population-is-red`),
  so it is not written."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.run :as run]))

(def ^:private ordering-pathway
  "An admit / order / dwell / discharge script, so this population has
  BOTH chargeable line types the modelled world actually produces: an
  order's own concept code, and the bed-days its dwell incurs."
  {:name "charges-witness"
   :steps [{:type :admission :location "Emergency" :reason "Chest pain, low risk"}
           {:type :delay :from 60 :to 120}
           {:type :order :profile :cbc}
           {:type :delay :from 240 :to 2880}
           {:type :discharge}]})

(def ^:private base-opts
  {:seed 202 :patients 12
   :pathway ordering-pathway
   :persons {:count 24 :years 20}
   :encounters true
   :emit "hl7"})

(def ^:private charges
  {:price-table {"58410-2" {:amount 148.00 :display "CBC panel"}
                 emit-hl7/room-and-board-code {:amount 1875.00 :display "Room and board, per day"}}})

(defn- run-payload [opts] (:payload (run/run-command opts)))
(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(def ^:private dft? (comp #{"DFT^P03"} msh-9))

(deftest charges-are-emission-only-and-ground-truth-does-not-move
  (let [without (run-payload (dissoc base-opts :emit))
        with (run-payload (assoc (dissoc base-opts :emit) :charges charges))]
    (is (seq (:ground-truth without)))
    (is (= (:ground-truth without) (:ground-truth with))
        "the price table is emission config -- ADR-0175 section 2(c)
         makes the EMISSION classification conditional on exactly this")
    (testing "and the key provably cannot reach the engine at all"
      (is (not (contains? (set engine/config-keys) :charges))))))

(deftest charges-absent-renders-the-byte-identical-stream
  (let [without (run-payload base-opts)
        with (run-payload (assoc base-opts :charges charges))]
    (is (pos? (count (:messages without))))
    (is (= (:messages without) (filterv (complement dft?) (:messages with))))))

(deftest charges-witness-table-at-population-scale
  (let [{:keys [ground-truth messages]} (run-payload (assoc base-opts :charges charges))
        {:keys [lines skipped]} (emit-hl7/plan-charges ground-truth charges)
        dfts (filterv dft? messages)
        all-lines (mapcat val lines)
        closes (count (filter #(#{:discharge :outpatient-visit-end} (:event %)) ground-truth))]
    (testing "one DFT per encounter close that has at least one priced
              line, and every one of them carries FT1s"
      (is (pos? (count dfts)))
      (is (= (count dfts) (count lines)))
      (is (<= (count dfts) closes))
      (is (every? #(str/includes? % "\rFT1|") dfts)))
    (testing "both line types this population produces have a NON-EMPTY
              population"
      (is (pos? (count (filter #(= emit-hl7/room-and-board-code (:code %)) all-lines))))
      (is (pos? (count (filter #(= "58410-2" (:code %)) all-lines)))))
    (testing "the skip census is REPORTED, not inferred from a short
              DFT -- and at this table it is empty, which is itself the
              statement"
      (is (= {} skipped))
      (testing "and pulling one price out moves it, by exactly the
                number of facts that code covered"
        (let [thin (update charges :price-table dissoc "58410-2")
              {s2 :skipped} (emit-hl7/plan-charges ground-truth thin)]
          (is (= (set (keys s2)) #{"58410-2"}))
          (is (pos? (get s2 "58410-2"))))))
    (testing "a DFT's MSH-10 is its own, distinct from the ADT the same
              close renders, and the whole stream stays unique"
      (is (= (count messages) (count (distinct (map msh-10 messages))))))))

(deftest a-malformed-charges-profile-is-rejected-before-the-engine-starts
  (doseq [[label bad] [["keyword codes rather than code strings"
                        {:price-table {:58410-2 {:amount 1.0}}}]
                       ["a price with no amount" {:price-table {"58410-2" {}}}]
                       ["a misspelled key" {:prices {}}]]]
    (testing label
      (let [r (run/run-command (assoc base-opts :charges bad))]
        (is (result/error? r))
        (is (= :invalid-charges (:category r)))))))

(deftest run-command-config-file-passthrough-carries-charges
  (let [path (str (System/getProperty "java.io.tmpdir") "/ehrt-charges-config-test.edn")]
    (try
      (spit path (pr-str {:charges charges
                          :pathway ordering-pathway
                          :persons {:count 24 :years 20}
                          :encounters true}))
      (let [r (run/run-command {:seed 202 :patients 12 :emit "hl7" :config path})]
        (is (result/ok? r))
        (is (some dft? (:messages (:payload r)))))
      (finally (.delete (java.io.File. path))))))
