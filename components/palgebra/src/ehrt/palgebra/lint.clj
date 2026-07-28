(ns ehrt.palgebra.lint
  "Generic catalytic-resource lint mechanism (design D13,
  docs/palgebra-design.md; claimed R2,
  `.agents/plans/judge-gate-refactor.md` Phase 2): every catalytic
  resource named in a loaded signature's stages, or in a raw
  equation-line string, must resolve to a target the caller's own
  taxonomy declares -- and where the caller's mapping supplies a
  verifier, resolution is checked mechanically, not just classified.

  This namespace has no opinion on what a target IS (a lockfile, a
  dependency file, an in-repo registry) -- that's the caller's own
  taxonomy and verification logic, supplied as plain functions to
  `lint` below. Only the extraction-and-verification mechanism lives
  here: pulling catalytic resource names out of the palgebra equation
  grammar's `{catalytic: ...}` annotation, and running each one
  through the caller's classify/verify functions.

  External stages ({external: true}) are exempt from raw equation-
  line extraction: a black-box stage's own catalytic inputs are not
  the instantiating signature's claim to verify."
  (:require [clojure.string :as str]))

(defn stages-catalytic-resources
  "Every catalytic resource name across a loaded signature's :stages
  (ehrt.palgebra.signature's Stage shape -- each stage's own :catalytic
  vector)."
  [signature]
  (set (mapcat :catalytic (:stages signature))))

(defn line-catalytic-resources
  "Extracts the {catalytic: a, b, c} resource names from one raw
  equation-line string (the string-diagram skill's own grammar). A
  line whose annotation block contains \"external: true\" contributes
  nothing -- external stages are exempt."
  [line]
  (if (re-find #"\{[^}]*external:\s*true" line)
    []
    (when-let [m (re-find #"catalytic:\s*([^;}]+)" line)]
      (mapv str/trim (str/split (second m) #",")))))

(defn lines-catalytic-resources
  "Every catalytic resource name across a seq of raw equation-line
  strings."
  [lines]
  (set (mapcat line-catalytic-resources lines)))

(defn lint
  "Returns {:ok? bool :violations [{:resource :issue :note} ...]}.

  `resources` is the set of catalytic resource names to check;
  `classify` is a fn resource-name -> classification-or-nil (the
  caller's taxonomy lookup); `verify` is a fn classification ->
  {:ok? bool :note string} (the caller's per-target verification).
  :issue on a violation is :unclassified (classify returned nil) or
  :unresolved (classified, but verify returned not-ok)."
  [{:keys [resources classify verify]}]
  (let [violations
        (reduce
         (fn [acc resource]
           (if-let [classification (classify resource)]
             (let [{:keys [ok? note]} (verify classification)]
               (if ok? acc (conj acc {:resource resource :issue :unresolved :note note})))
             (conj acc {:resource resource :issue :unclassified
                        :note "no declared target classification for this resource in the caller's taxonomy"})))
         []
         (sort resources))]
    {:ok? (empty? violations) :violations violations}))
