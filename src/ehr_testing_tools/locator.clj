(ns ehr-testing-tools.locator
  "Locator (pattern nursery #4): one type for \"a place in a datum\", with
  a per-format grammar. Only the trivial envelope checks land here --
  non-empty path, known format keyword. Real per-format grammars (FHIRPath,
  v2 segment/field/component, table.column, XPath) arrive with mutation
  and gates; this is the shared shape they'll plug into."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [ehr-testing-tools.result :as result]))

(def known-formats #{:fhir :v2 :table :xpath})

(def Locator
  [:map
   [:format (into [:enum] known-formats)]
   [:path [:string {:min 1}]]])

(defn valid?
  [loc]
  (m/validate Locator loc))

(defn make
  "Builds a locator envelope. Rejects (not throws) an unknown format or an
  empty path -- grammar-specific validity is a later, format-dispatched
  concern."
  [format path]
  (let [candidate {:format format :path path}]
    (if (valid? candidate)
      (result/ok candidate)
      (result/rejected :invalid-locator {:format format :path path}))))

;; ---- FHIR grammar (P4, first real per-format grammar -- pattern
;; nursery #4): a data-path form, dotted field access plus bracketed
;; integer indices, into plain-data (data.json-shaped) parsed FHIR
;; JSON -- the representation EXP-B2 established mutation operates on.
;; This is deliberately an operational SUBSET of FHIRPath, not an
;; implementation of it: no wildcards, no filters (`where(...)`), no
;; functions, no union/type operators. Full FHIRPath is future work;
;; this exists because corpus.mutate needs *a* way to name "this exact
;; spot in this exact datum" today.

(def ^:private fhir-path-segment-re
  #"^([A-Za-z_][A-Za-z0-9_]*)(\[\d+\])?$")

(def ^:private fhir-path-index-re
  #"\[(\d+)\]")

(defn- parse-segment
  "\"given\" -> [\"given\"]; \"entry[0]\" -> [\"entry\" 0]; nil on any
  segment that doesn't match the grammar (empty, malformed brackets,
  a leading digit, etc.)."
  [segment]
  (when-let [[_ field brackets] (re-matches fhir-path-segment-re segment)]
    (into [field] (map (fn [[_ n]] (Long/parseLong n)) (re-seq fhir-path-index-re (or brackets ""))))))

(defn fhir-data-path
  "Parses a FHIR locator's :path string (\"entry[0].resource.gender\")
  into a data-path vector of string keys and integer indices, directly
  usable with get-in/assoc-in/update-in against plain-data parsed FHIR
  JSON. Returns result/ok [...] or result/rejected :invalid-fhir-path
  on an empty string or any segment outside the grammar."
  [path-str]
  (if (empty? path-str)
    (result/rejected :invalid-fhir-path {:path path-str})
    (let [segments (str/split path-str #"\.")
          parsed (map parse-segment segments)]
      (if (or (empty? segments) (some empty? segments) (some nil? parsed))
        (result/rejected :invalid-fhir-path {:path path-str})
        (result/ok (vec (mapcat identity parsed)))))))
