(ns ehr-testing-tools.corpus.mutate
  "The mutation capability (ADR-0004), test-first, FHIR only this
  session (ADR-0004; v2 mutation deferred to post-EXP-A3). Pure core:
  `mutate` takes a base datum, an operator entry (ehr-testing-tools.
  corpus.operators), and a locator, and returns the mutant plus its
  lineage record -- no I/O here; the CLI (ehr corpus mutate) is the
  thin, impure shell around it.

  Informed by EXP-B2's applied decision rule: operates on plain
  Clojure data (data.json-shaped FHIR JSON), never a HAPI-parsed tree.
  `content-hash` canonicalizes via `clojure.data.json/write-str`
  (compact, deterministic) rather than `pr-str` -- EXP-B2 found this
  representation's round-trip faithful (modulo whitespace only), and
  JSON is what actually gets written to disk, so hashing the same
  bytes that would be persisted is the meaningful content identity,
  not an incidental Clojure-printed form."
  (:require [clojure.data.json :as json]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.locator :as locator]
            [ehr-testing-tools.lineage :as lineage]
            [ehr-testing-tools.result :as result]))

(defn content-hash
  "The content hash `corpus.mutate` and lineage records use to
  identify a datum: sha256 of its canonical (compact) JSON
  serialization."
  [data]
  (digest/sha256-string (json/write-str data)))

(defn mutate
  "Applies operator (a corpus.operators registry entry) to base-data
  at locator (a locator envelope, {:format :path} -- ehr-testing-tools.
  locator). Returns result/ok {:mutant :lineage}, or:
    - the locator path's own parse rejection (:invalid-fhir-path), if
      the locator's :path doesn't parse under the FHIR grammar
    - result/rejected :locator-not-found if the parsed path doesn't
      resolve anywhere in base-data (distinguished from \"resolves to
      nil\" via a sentinel -- a legitimately null field is not the
      same as a missing one)
  operator's own :fn is assumed pure and total once the path is known
  to resolve; validation of *that* is this function's job, not the
  operator's."
  [base-data operator locator-envelope]
  (let [path-result (locator/fhir-data-path (:path locator-envelope))]
    (if-not (result/ok? path-result)
      path-result
      (let [path (:payload path-result)
            sentinel ::not-found]
        (if (= sentinel (get-in base-data path sentinel))
          (result/rejected :locator-not-found {:path path})
          (let [mutant ((:fn operator) base-data path)
                lineage-record (lineage/build
                                 {:parent (content-hash base-data)
                                  :stage :mutate
                                  :transformation {:operator {:id (:id operator) :version (:version operator)}
                                                    :locator locator-envelope
                                                    :contract (:contract operator)}
                                  :produced (content-hash mutant)})]
            (result/ok {:mutant mutant :lineage lineage-record})))))))
