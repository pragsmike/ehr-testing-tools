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

;; ---- v2 grammar (P5, first real per-format grammar beyond FHIR --
;; pattern nursery #4): an operational SUBSET of HL7 v2's own
;; segment/field/component addressing, matching exactly the fields
;; ca.uhn.hl7v2.Location (HAPI's own location type, attached to every
;; HL7Exception/ValidationException judge.v2 catches) exposes:
;; segment name, segment repetition, field number, component. No
;; field-repetition, no sub-component -- a genuinely fuller subset is
;; future work; this exists because judge.v2 needs *a* way to name
;; "this exact spot in this exact v2 message" today, mirroring why the
;; FHIR grammar above exists.

(def ^:private v2-path-re
  #"^([A-Z][A-Z0-9]{2})(?:\[(\d+)\])?-(\d+)(?:-(\d+))?$")

(defn v2-data-path
  "Parses a v2 locator's :path string (\"PID-3\", \"PID[0]-7\",
  \"OBX[2]-5-1\") into a [segment index field component] tuple:
  segment (a 3-character segment id, e.g. \"PID\"), index (the
  segment's repetition, defaulting to 0 when the [n] suffix is
  omitted), field (the 1-based field number), component (1-based, or
  nil when absent -- field-level, not component-level). Returns
  result/ok [...] or result/rejected :invalid-v2-path on any string
  outside this grammar."
  [path-str]
  (if-let [[_ segment index field component] (re-matches v2-path-re (or path-str ""))]
    (result/ok [segment (if index (Long/parseLong index) 0) (Long/parseLong field)
                (when component (Long/parseLong component))])
    (result/rejected :invalid-v2-path {:path path-str})))
