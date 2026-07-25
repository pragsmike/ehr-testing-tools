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

;; ---- v2 grammar (P7, arrives with mutation as the locator envelope's
;; docstring long promised -- pattern nursery #4): HL7 v2's own
;; segment/field/component/subcomponent addressing, operating on the
;; delimiter-split ER7 substrate (corpus.er7, EXP-B2's applied decision
;; rule -- see that namespace's docstring), not a HAPI-parsed tree.
;; Six forms, from coarsest to finest: segment (\"PID\"), segment+repeat
;; (\"OBX[2]\"), field (\"PID-3\"), field+repeat (\"PID-3[2]\"),
;; component (\"PID-3.1\"), subcomponent (\"PID-3.1.2\"). Segment-level
;; locators exist because some defects (drop a segment, corrupt a
;; segment's own name) target the segment as a whole, not one of its
;; fields -- the earlier P5 grammar required a field always, which
;; couldn't name those; this grammar corrects that.
;;
;; MSH-1/MSH-2 off-by-one convention: at the GRAMMAR level, \"MSH-1\"
;; and \"MSH-2\" parse exactly like any other segment's field locator
;; (field 1, field 2) -- the grammar makes no MSH exception, deliberately,
;; so the parser stays one regex for every segment. The off-by-one
;; itself is real but lives one layer down, in how a parsed locator maps
;; to a position in the delimiter-split substrate (corpus.er7): MSH-1 is
;; the field separator character itself (\"|\" canonically), which never
;; appears as a split token -- it IS the delimiter the split consumes --
;; so it has no split-array slot; MSH-2 (the encoding characters,
;; \"^~\\&\") is consequently the FIRST split token after the segment
;; name, at split-index 1, not field-index 2. Concretely: for MSH, field
;; N (N >= 2) resolves to split-index (N - 1); for every other segment,
;; field N resolves to split-index N (split-index 0 being the segment
;; name itself). corpus.er7 applies this mapping when resolving a parsed
;; locator against split message data; nothing here needs to know it.
;;
;; Every numeric component (segment-repeat, field, field-repeat,
;; component, subcomponent) is constrained to a positive integer
;; ([1-9]\\d* -- no leading zero, no zero, no sign) directly in the
;; regex: HL7 v2 numbers fields/components from 1, so 0 and negative
;; values are simply not expressible in this grammar rather than
;; accepted-then-rejected by a separate check. The regex is fully
;; anchored (^...$), so a trailing separator with nothing after it
;; (\"PID-\", \"PID-3.\", \"PID-3-\") fails to match rather than parsing
;; a partial path. Segment names are exactly three characters, a
;; leading letter followed by two more letters/digits, uppercase only
;; -- HL7's own segment-ID convention; lowercase, wrong length, or a
;; leading digit are all unknown segment-name shapes and rejected.
(def ^:private v2-path-re
  #"^([A-Z][A-Z0-9]{2})(?:\[([1-9]\d*)\])?(?:-([1-9]\d*)(?:\[([1-9]\d*)\])?(?:\.([1-9]\d*)(?:\.([1-9]\d*))?)?)?$")

(defn v2-data-path
  "Parses a v2 locator's :path string under the grammar above into a
  structured path map -- {:segment ...} plus whichever of
  :segment-repeat, :field, :field-repeat, :component, :subcomponent the
  string named (absent keys, not nil values, for anything not present).
  Returns result/ok {...} or result/rejected :invalid-v2-path on any
  string outside the grammar. The operator fns corpus.operators
  registers for :format :v2 consume this structured map, never the raw
  path string."
  [path-str]
  (if-let [[_ segment seg-repeat field field-repeat component subcomponent]
           (re-matches v2-path-re (or path-str ""))]
    (result/ok (cond-> {:segment segment}
                 seg-repeat (assoc :segment-repeat (Long/parseLong seg-repeat))
                 field (assoc :field (Long/parseLong field))
                 field-repeat (assoc :field-repeat (Long/parseLong field-repeat))
                 component (assoc :component (Long/parseLong component))
                 subcomponent (assoc :subcomponent (Long/parseLong subcomponent))))
    (result/rejected :invalid-v2-path {:path path-str})))
