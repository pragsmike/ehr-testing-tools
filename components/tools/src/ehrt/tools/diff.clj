(ns ehrt.tools.diff
  "Structural value diffing. `diff-paths` originated as
  corpus.mutate's own intended-diff-only law-test helper
  (mutate_test.clj); it now has a second real consumer
  (ehrt.tools.check's golden-equivalence assertion), which is
  what earns its promotion to src -- a single private test helper
  duplicated into a second namespace would be the premature
  abstraction this repo avoids, but a second genuine consumer is
  exactly the point a shared utility pays for itself."
  (:require [clojure.string :as str]))

(defn diff-paths
  "The minimal set of paths at which a and b differ -- if a whole
  subtree differs (added, removed, or replaced wholesale, including a
  vector whose own length differs), reports that subtree's path once,
  not every path beneath it. Recurses into both maps (by key) and
  vectors of equal length (by index); anything else (scalars, unequal-
  length vectors, a map vs a vector) is compared by simple equality at
  the current path."
  ([a b] (diff-paths a b []))
  ([a b path]
   (cond
     (= a b) #{}
     (and (map? a) (map? b))
     (reduce (fn [acc k]
               (into acc (diff-paths (get a k ::missing) (get b k ::missing) (conj path k))))
             #{}
             (into (set (keys a)) (keys b)))
     (and (vector? a) (vector? b) (= (count a) (count b)))
     (reduce (fn [acc i]
               (into acc (diff-paths (nth a i) (nth b i) (conj path i))))
             #{}
             (range (count a)))
     :else #{path})))

(defn path->locator-path
  "Renders a diff-paths path (a vector of string keys and integer
  indices) in the FHIR locator grammar's own dotted/bracketed string
  form (ehrt.tools.locator/fhir-data-path is this function's
  inverse): [\"entry\" 0 \"resource\" \"gender\"] ->
  \"entry[0].resource.gender\". An empty path (the whole value differs,
  not a specific field within it) renders as \"\"."
  [path]
  (let [segments (reduce (fn [acc seg]
                            (cond
                              (not (integer? seg)) (conj acc (str seg))
                              (seq acc) (conj (pop acc) (str (peek acc) "[" seg "]"))
                              :else (conj acc (str "[" seg "]"))))
                          []
                          path)]
    (str/join "." segments)))
