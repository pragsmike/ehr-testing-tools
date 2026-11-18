(ns ehrt.patient-simulator.end-reference-test
  "ADR-0188: an end that names its opening BY STATE references the
  latest opening of that state that no earlier end references -- for
  all three paired spans, medication, condition and care plan.

  Until this rule the reference was the FIRST opening of the state,
  whichever span the end was closing. A state that fires once cannot
  tell the two rules apart; a state that fires twice can, and every
  module below fires one twice."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.patient-simulator.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ^:private a-concept {:system :snomed :code "000001" :display "A thing that opens and ends"})

(def ^:private kinds
  "The three spans: the opening state's type, the end state's type, the
  key the end names its opening state under, and the `active` condition
  that reads the pair."
  [{:label "medication" :open :medication-order :end :medication-end :names :medication-order
    :active :active-medication}
   {:label "condition" :open :condition-onset :end :condition-end :names :condition-onset
    :active :active-condition}
   {:label "care plan" :open :care-plan-start :end :care-plan-end :names :careplan
    :active :active-careplan}])

(defn- below-two
  "`then` until the counter reaches 2, `else` once it has. `!=`, because
  an Attribute condition here compares for equality only."
  [attribute then else]
  {:type :simple
   :conditional-transition [{:condition {:condition-type :attribute :attribute attribute
                                         :operator "!=" :value 2}
                             :transition then}
                            {:transition else}]})

(defn- alternating
  "open, end, open, end."
  [{:keys [open end names]}]
  {:id "m" :name "Alternating"
   :states {:initial {:type :initial :direct-transition :open}
            :open {:type open :codes [a-concept] :direct-transition :end}
            :end {:type end names :open :direct-transition :count}
            :count {:type :counter :attribute "n" :action :increment :direct-transition :again}
            :again (below-two "n" :open :done)
            :done {:type :terminal}}})

(defn- nested
  "open, open, end, end."
  [{:keys [open end names]}]
  {:id "m" :name "Nested"
   :states {:initial {:type :initial :direct-transition :open}
            :open {:type open :codes [a-concept] :direct-transition :count-opens}
            :count-opens {:type :counter :attribute "opens" :action :increment :direct-transition :open-again}
            :open-again (below-two "opens" :open :end)
            :end {:type end names :open :direct-transition :count-ends}
            :count-ends {:type :counter :attribute "ends" :action :increment :direct-transition :end-again}
            :end-again (below-two "ends" :end :done)
            :done {:type :terminal}}})

(defn- one-open-two-ends
  "open, end, end: the second end fires with nothing open."
  [{:keys [open end names]}]
  {:id "m" :name "EndedTwice"
   :states {:initial {:type :initial :direct-transition :open}
            :open {:type open :codes [a-concept] :direct-transition :end}
            :end {:type end names :open :direct-transition :count-ends}
            :count-ends {:type :counter :attribute "ends" :action :increment :direct-transition :end-again}
            :end-again (below-two "ends" :end :done)
            :done {:type :terminal}}})

(defn- never-opened
  "An end naming a state that never fired."
  [{:keys [open end names]}]
  {:id "m" :name "NeverOpened"
   :states {:initial {:type :initial :direct-transition :end}
            :open {:type open :codes [a-concept] :direct-transition :end}
            :end {:type end names :open :direct-transition :done}
            :done {:type :terminal}}})

(defn- walk [module]
  (let [ctx (interp/initial-context (sim-model/persona (Random. 1) {}))
        result (interp/walk-module module (Random. 1) ctx)]
    (is (= :terminal (:status result)))
    {:ctx (assoc ctx :trajectory (:trajectory result))
     :shape (mapv :event (:trajectory result))
     :references (mapv :references (:trajectory result))}))

(deftest the-second-end-references-the-second-opening
  (doseq [{:keys [label open end] :as kind} kinds]
    (testing label
      (let [{:keys [shape references]} (walk (alternating kind))]
        (is (= [open end open end] shape))
        (is (= [nil 0 nil 2] references)
            "each end references the opening it closes, not the first of that state")))))

(deftest nested-openings-end-latest-first
  (doseq [{:keys [label open end] :as kind} kinds]
    (testing label
      (let [{:keys [shape references]} (walk (nested kind))]
        (is (= [open open end end] shape))
        (is (= [nil nil 1 0] references)
            "the later opening is ended first, then the earlier")))))

(deftest after-a-correct-end-nothing-is-active
  (doseq [{:keys [label active] :as kind} kinds
          [shape-label module] [["alternating" (alternating kind)] ["nested" (nested kind)]]]
    (testing (str label ", " shape-label)
      (is (false? (interp/evaluate-condition "m" (:ctx (walk module))
                                             {:condition-type active :codes [a-concept]}))
          "every opening has its own end, so the `active` condition is false"))))

(deftest an-end-with-nothing-open-keeps-the-behaviour-it-had
  (doseq [{:keys [label open end] :as kind} kinds]
    (testing (str label ": every opening of the state already ended")
      (let [{:keys [shape references]} (walk (one-open-two-ends kind))]
        (is (= [open end end] shape))
        (is (= [nil 0 0] references)
            "measured at 164fae67: the end references the state's first opening again")))
    (testing (str label ": the state never fired")
      (let [{:keys [shape references]} (walk (never-opened kind))]
        (is (= [end] shape))
        (is (= [nil] references) "measured at 164fae67: no reference")))))
