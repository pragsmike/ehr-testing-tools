(ns ehrt.corpus.mutate
  "The mutation capability (ADR-0004), test-first. Pure core: `mutate`
  takes a base datum, an operator entry (ehrt.corpus.
  operators), and a locator, and returns the mutant plus its lineage
  record -- no I/O here; the CLI (ehr corpus mutate) is the thin,
  impure shell around it. Dispatches on operator's :format: FHIR (P4)
  and v2 (P7) are the two live formats, each with its own substrate and
  content-identity function, unified only by this fn's own shape and by
  `mutate`'s law (docs/pipeline.edn's Mutate stage: intended-diff-only)
  -- adding a third format is a new private mutate-<format> plus one
  `case` branch, not a structural change here.

  Informed by EXP-B2's applied decision rule, for both formats: never a
  HAPI-parsed tree as the mutation substrate. FHIR operates on plain
  Clojure data (data.json-shaped FHIR JSON, `content-hash` below) --
  HAPI FHIR's round-trip was found to silently drop resource.id,
  disqualifying it. v2 operates on ehrt.corpus-io.er7's
  delimiter-split substrate (its own `content-hash`) -- HAPI HL7v2's
  PipeParser round-trip was found to canonicalize away trailing empty
  fields, the same class of hazard for the same reason; PipeParser
  remains fine for *judging* (judge.v2 uses it unchanged), only
  disqualified as the mutation substrate. See EXP-B2's results
  (docs/experiments/EXP-B2-results.md) for both findings' evidence."
  (:require [clojure.data.json :as json]
            [ehrt.corpus.lineage :as lineage]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as kernel]))

(defn content-hash
  "The content hash `corpus.mutate`'s FHIR path and lineage records
  use to identify a FHIR datum: sha256 of its canonical (compact) JSON
  serialization. v2 uses ehrt.corpus-io.er7/content-hash
  instead (sha256 of the serialized ER7 string) -- there is no
  analogous \"canonical parsed form\" to serialize for v2 the way JSON
  serves FHIR; the ER7 string itself already is the persisted form."
  [data]
  (kernel/sha256-string (json/write-str data)))

(defn- lineage-for
  [{:keys [parent operator locator-envelope produced]}]
  (lineage/build
   {:parent parent
    :stage :mutate
    :transformation {:operator {:id (:id operator) :version (:version operator)}
                      :locator locator-envelope
                      :contract (:contract operator)}
    :produced produced}))

(defn- mutate-fhir
  "base-data is parsed FHIR JSON (plain Clojure data, string keys and
  integer indices -- e.g. clojure.data.json/read-str's own output)."
  [base-data operator locator-envelope]
  (let [path-result (kernel/fhir-data-path (:path locator-envelope))]
    (if-not (kernel/ok? path-result)
      path-result
      (let [path (:payload path-result)
            sentinel ::not-found]
        (if (= sentinel (get-in base-data path sentinel))
          (kernel/rejected :locator-not-found {:path path})
          (let [mutant ((:fn operator) base-data path)]
            (kernel/ok {:mutant mutant
                        :lineage (lineage-for {:parent (content-hash base-data)
                                                :operator operator
                                                :locator-envelope locator-envelope
                                                :produced (content-hash mutant)})})))))))

(defn- mutate-v2
  "base-content is a raw ER7 string (e.g. (slurp a .hl7 file)) --
  ehrt.corpus-io.er7/parse is called internally, so the
  substrate parsing stays this namespace's own concern rather than
  every caller's."
  [base-content operator locator-envelope]
  (let [path-result (kernel/v2-data-path (:path locator-envelope))]
    (if-not (kernel/ok? path-result)
      path-result
      (let [loc (:payload path-result)
            parsed (corpus-io/parse base-content)]
        (if-not (corpus-io/resolve-locator parsed loc)
          (kernel/rejected :locator-not-found {:path loc})
          (let [mutant (corpus-io/serialize ((:fn operator) parsed loc))]
            (kernel/ok {:mutant mutant
                        :lineage (lineage-for {:parent (corpus-io/content-hash base-content)
                                                :operator operator
                                                :locator-envelope locator-envelope
                                                :produced (corpus-io/content-hash mutant)})})))))))

(defn mutate
  "Applies operator (a corpus.operators registry entry) to base-data
  at locator (a locator envelope, {:format :path} -- ehrt.kernel.
  locator). Dispatches on operator's own :format (:fhir or :v2) to the
  matching substrate; base-data's own shape is format-dependent (see
  mutate-fhir/mutate-v2's docstrings). Returns kernel/ok {:mutant
  :lineage}, or:
    - the locator path's own parse rejection (:invalid-fhir-path or
      :invalid-v2-path), if the locator's :path doesn't parse under
      its format's grammar
    - kernel/rejected :locator-not-found if the parsed path doesn't
      resolve anywhere in base-data
  operator's own :fn is assumed pure and total once the path is known
  to resolve; validation of *that* is this function's job, not the
  operator's."
  [base-data operator locator-envelope]
  (case (:format operator)
    :fhir (mutate-fhir base-data operator locator-envelope)
    :v2 (mutate-v2 base-data operator locator-envelope)))
