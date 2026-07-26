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
  on an empty string or any segment outside the grammar.

  The split uses limit -1 (LOC-1, 2026-07-25) so that a trailing empty
  token survives to the empty-segment guard below rather than being
  discarded before it: clojure.string/split's default limit drops
  trailing empty tokens, which made \"entry[0].resource.\" parse
  silently as \"entry[0].resource\" and left the guard unreachable for
  that one input. With -1 the guard fires, and this grammar is
  anchored at both ends exactly like the v2 grammar below -- a
  separator with nothing after it is a parse error in both."
  [path-str]
  (if (empty? path-str)
    (result/rejected :invalid-fhir-path {:path path-str})
    (let [segments (str/split path-str #"\." -1)
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
;; MSH-1/MSH-2 off-by-one convention: MSH-1 is the field separator
;; character itself (\"|\" canonically), which never appears as a split
;; token -- it IS the delimiter the split consumes -- so it has no
;; split-array slot; MSH-2 (the encoding characters, \"^~\\&\") is
;; consequently the FIRST split token after the segment name, at
;; split-index 1, not field-index 2. Concretely: for MSH, field N
;; (N >= 2) resolves to split-index (N - 1); for every other segment,
;; field N resolves to split-index N (split-index 0 being the segment
;; name itself). corpus.er7 applies that mapping when resolving a parsed
;; locator against split message data; nothing here needs to know it.
;;
;; The convention has exactly one grammar-level consequence, and it is
;; enforced here rather than left to the substrate (LOC-1, 2026-07-25):
;; MSH-1 is not addressable, so `v2-data-path` refuses it, with a hint
;; that teaches why. Everything else about MSH parses like any other
;; segment -- one regex still serves every segment, and the refusal is
;; a single post-match check, not a second grammar. Before LOC-1,
;; \"MSH-1\" parsed like an ordinary field locator and then landed on
;; MSH-2's slot, since corpus.er7/field-index shifts only for N >= 2:
;; it silently addressed the encoding characters, which is the worst
;; kind of success. Field 1 is ordinary data in every OTHER segment
;; (\"PID-1\", \"ZZ1-1\"), so the check is MSH-specific.
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

(def msh-1-hint
  "Why MSH-1 is refused, in one sentence a reader can act on without a
  human in the loop (AUTHORS-GUIDE section 6; DOC-1's enumerable-options
  errors are the house precedent). Stated from corpus.er7's own account
  of the delimiter convention -- MSH-1 is the character immediately
  after the literal \"MSH\", MSH-2 is the four characters after that --
  not a competing explanation."
  (str "MSH-1 is the field separator character itself (the character right "
       "after the literal \"MSH\"), not an addressable field: the split that "
       "produces a segment's fields consumes it, so it holds no position of "
       "its own. The encoding characters are MSH-2."))

(defn v2-data-path
  "Parses a v2 locator's :path string under the grammar above into a
  structured path map -- {:segment ...} plus whichever of
  :segment-repeat, :field, :field-repeat, :component, :subcomponent the
  string named (absent keys, not nil values, for anything not present).
  Returns result/ok {...} or result/rejected :invalid-v2-path on any
  string outside the grammar. The operator fns corpus.operators
  registers for :format :v2 consume this structured map, never the raw
  path string.

  One string inside the regex's grammar is still refused, per the MSH
  note above: anything naming MSH's field 1 (\"MSH-1\", \"MSH-1[2]\",
  \"MSH-1.1\"). It is the same :invalid-v2-path category as any other
  refusal -- callers dispatching on the category see nothing new -- but
  its payload carries a :hint teaching the convention rather than only
  refusing."
  [path-str]
  (if-let [[_ segment seg-repeat field field-repeat component subcomponent]
           (re-matches v2-path-re (or path-str ""))]
    (if (and (= "MSH" segment) (= "1" field))
      (result/rejected :invalid-v2-path {:path path-str :hint msh-1-hint})
      (result/ok (cond-> {:segment segment}
                   seg-repeat (assoc :segment-repeat (Long/parseLong seg-repeat))
                   field (assoc :field (Long/parseLong field))
                   field-repeat (assoc :field-repeat (Long/parseLong field-repeat))
                   component (assoc :component (Long/parseLong component))
                   subcomponent (assoc :subcomponent (Long/parseLong subcomponent)))))
    (result/rejected :invalid-v2-path {:path path-str})))
