(ns ehrt.judge.sampling-test
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (h),
  ruling D1): gating at scale.

  THE DETERMINISM GATE IS WRITTEN THE AWKWARD WAY ON PURPOSE. ADR-0175
  section 2(h) spells out why: select over a corpus, shuffle it, select
  again, assert equality -- AND ASSERT EVERY STRATUM NON-EMPTY FIRST,
  because a selector that returns nothing passes a set-equality check
  trivially. That last clause is the whole reason to write it this way
  rather than the obvious way, and it is
  `rulings.md#R-empty-population-is-red` applied to a gate about
  sampling."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.judge.sampling :as sampling]))

(def ^:private skeleton
  #{"ADT^A01" "ADT^A03" "ADT^A20" "ORU^R01"})

(defn- entry [msh-9 n]
  {:path (str "/corpus/" msh-9 "-" (format "%04d" n) ".hl7")
   :msh-9 msh-9
   :msh-10 (format "MRN%04d-%s" n msh-9)})

(def ^:private corpus
  "Four skeleton families and three add-on ones, at deliberately
  lopsided sizes -- ADT^A13 is the rare family ruling D3 (a uniform
  sample across the whole corpus) would lose."
  (vec (concat (map #(entry "ADT^A01" %) (range 40))
               (map #(entry "ADT^A03" %) (range 40))
               (map #(entry "ADT^A20" %) (range 120))
               (map #(entry "ORU^R01" %) (range 12))
               (map #(entry "ADT^A08" %) (range 300))
               (map #(entry "ADT^A31" %) (range 90))
               (map #(entry "DFT^P03" %) (range 50)))))

(defn- select [entries cap]
  (sampling/stratified-selection entries {:skeleton-types skeleton :cap cap}))

(deftest header-reads-msh-9-and-msh-10-off-the-first-line-only
  (is (= {:msh-9 "ADT^A08" :msh-10 "MRN000001-A08-0-0"}
         (sampling/header (str "MSH|^~\\&|EHR-TESTING-SIM|SIM|||20240101000000+0000||"
                               "ADT^A08|MRN000001-A08-0-0|P|2.4\rEVN|A08|20240101000000+0000\r"))))
  (testing "a file that is not MSH-led, or an MSH too short to carry
            MSH-10, reads as nil -- and `stratified-selection` gates
            those in FULL rather than dropping them"
    (is (nil? (sampling/header "PID|1||MRN1")))
    (is (nil? (sampling/header "")))
    (is (nil? (sampling/header nil)))
    (is (nil? (sampling/header "MSH|^~\\&|A|B")))))

(deftest skeleton-families-are-gated-in-full-and-add-ons-are-capped
  (let [{:keys [selected strata]} (select corpus 5)]
    (testing "every stratum has a population, before anything is
              asserted about the selection over it"
      (is (= 7 (count strata)))
      (is (every? pos? (map :n (vals strata)))))
    (testing "full width on skeleton kinds -- the half of the policy
              with no cap"
      (is (= {:n 40 :gated 40 :add-on? false} (get strata "ADT^A01")))
      (is (= {:n 120 :gated 120 :add-on? false} (get strata "ADT^A20")))
      (is (= {:n 12 :gated 12 :add-on? false} (get strata "ORU^R01"))))
    (testing "one stratum per MSH-9 on add-ons, min(n, cap) each"
      (is (= {:n 300 :gated 5 :add-on? true} (get strata "ADT^A08")))
      (is (= {:n 90 :gated 5 :add-on? true} (get strata "ADT^A31")))
      (is (= {:n 50 :gated 5 :add-on? true} (get strata "DFT^P03"))))
    (is (= (+ 40 40 120 12 5 5 5) (count selected)))
    (testing "and a cap ABOVE a stratum's own size takes all of it, not
              a padded selection"
      (is (= {:n 50 :gated 50 :add-on? true} (get (:strata (select corpus 500)) "DFT^P03"))))
    (testing "no cap at all is ruling D2 -- full width always -- as a
              configuration rather than a second code path"
      (is (= (count corpus) (count (:selected (select corpus nil)))))
      (is (= (count corpus) (count (:selected (select corpus 0))))))))

(deftest the-sample-is-derived-from-msh-10-not-drawn
  (let [{:keys [selected]} (select corpus 3)
        a08 (filterv #(= "ADT^A08" (:msh-9 %)) selected)]
    (is (= ["MRN0000-ADT^A08" "MRN0001-ADT^A08" "MRN0002-ADT^A08"]
           (mapv :msh-10 a08))
        "the first `cap` by MSH-10 order -- a total order over a value
         this project mints itself, recomputable by any reader from the
         corpus alone")))

(deftest the-selection-is-deterministic-under-a-shuffled-corpus
  (testing "every stratum is non-empty FIRST -- a selector that
            returned nothing would pass the equality below trivially,
            which is the vacuity ADR-0175 section 2(h) says to guard
            against explicitly"
    (let [{:keys [selected strata]} (select corpus 5)]
      (is (= 7 (count strata)))
      (is (every? pos? (map :gated (vals strata))))
      (is (pos? (count selected)))
      (testing "and the same corpus in a different order selects the
                identical messages, in the identical order"
        (doseq [seed [1 2 3 4 5]]
          (let [shuffled (shuffle corpus)]
            (is (not= corpus shuffled) (str "shuffle " seed " must actually reorder"))
            (is (= selected (:selected (select shuffled 5))))
            (is (= strata (:strata (select shuffled 5))))))))))

(deftest an-unreadable-msh-9-is-gated-in-full-never-dropped
  (let [mixed (conj corpus {:path "/corpus/junk.hl7" :msh-9 nil :msh-10 "junk.hl7"})
        {:keys [selected strata]} (select mixed 5)]
    (is (= {:n 1 :gated 1 :add-on? false} (get strata sampling/unknown-stratum)))
    (is (some #(= "/corpus/junk.hl7" (:path %)) selected)
        "a sampler that quietly dropped what it could not parse would
         hide exactly the damage the gate exists to find")))

(deftest every-stratum-prints-its-own-n-and-gated
  (testing "`no silent caps` is a promise about OUTPUT, so it is a
            function rather than a comment"
    (let [lines (sampling/render-strata (:strata (select corpus 5)))]
      (is (= 7 (count lines)))
      (is (every? #(re-find #"n=\d+\s+gated=\d+" %) lines))
      (is (some #(re-find #"ADT\^A08\s+n=300\s+gated=5\s+sampled \(add-on\)" %) lines))
      (is (some #(re-find #"ADT\^A01\s+n=40\s+gated=40\s+full \(skeleton\)" %) lines)))))

(deftest an-empty-corpus-selects-nothing-and-says-so
  (is (= {:selected [] :strata {}} (select [] 5))))
