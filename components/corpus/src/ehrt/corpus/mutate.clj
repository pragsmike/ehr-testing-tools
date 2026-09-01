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

  THE THIRD FORMAT ARRIVED (2026-09-01, ADR-0176, all nine questions
  ruled (a)): :event, whose substrate is a ground-truth EVENT LOG --
  the vector `ehrt sim run --format ground-truth` prints -- rather than
  a file. The prediction above held: `mutate-event` plus one `case`
  branch, no structural change. Two things about it are genuinely
  different, and both are rulings rather than accidents.

  FIRST, the third argument is a SEED, not a locator envelope. A file
  operator is HANDED its site; an event operator DRAWS its own, once,
  over the candidate sites the log offers (Q3(a)/Q4(a)), from its own
  seed -- independent of the run's master seed, so the stage works on
  any log including one whose master seed the caller does not have.
  `mutate`'s third parameter is therefore best read as \"how this
  format's site is determined\", which is the locator for :fhir/:v2 and
  the seed for :event.

  SECOND, this stage is POST-RUN and OUTSIDE `engine/run` entirely
  (Q1(a)), which is where the ADR's recommendation departed from the
  channel's own stated expectation of a post-decide, pre-apply
  transform. The short form of the four reasons: `fold/apply-events`
  sees one decide's batch and not the log, so most referential defect
  shapes are inexpressible there; a mutation folded into `:world` is
  republished to `decide` by `:log-mirror`, so the fault cascades and
  \"class X and nothing else\" cannot close; `run`'s :rejected decide arm
  silently REPAIRS some injections; and rulings.md#R-transport-realism-
  vs-mutation already assigns wrong-WORLD to `:churn-profile`, while a
  content fault means the record is wrong and the world was right.
  Being outside `run` also makes the opt-in-key law hold in its
  strongest form -- there is no key to be absent, `engine/config-keys`
  is untouched, and every corpus this repository ships is
  byte-identical whether or not this stage exists.

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

(defn event-content-hash
  "The content hash identifying a ground-truth EVENT LOG: sha256 of
  `pr-str` over the log as a vector. `pr-str` and not JSON because EDN
  is the log's own persisted form -- `ehrt sim run --format
  ground-truth` prints exactly this, and `ehrt sim check` reads it back
  -- and the round trip is byte-identical (measured at 7096394 over a
  335,546-byte log), so this hash identifies the bytes a consumer
  actually holds rather than a re-encoding of them."
  [events]
  (kernel/sha256-string (pr-str (vec events))))

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

(defn- event-lineage-for
  [{:keys [parent operator seed site produced]}]
  (lineage/build
   {:parent parent
    :stage :mutate
    :transformation {:operator {:id (:id operator) :version (:version operator)}
                     :seed seed
                     :site site
                     :contract (:contract operator)
                     :expected-findings (:expected-findings operator)}
    :produced produced}))

(defn- mutate-event
  "events is a ground-truth event log (a vector of event maps --
  `ehrt sim run --format ground-truth`'s own output). seed is the
  operator's OWN seed, and the single draw it funds selects the site.

  ONE DRAW, ONE SITE (Q3(a)). Multiplicity comes from applying an
  operator N times with N seeds, never from one application mutating N
  sites -- which is what keeps a defect class unambiguous, keeps the
  oracle loop's set EQUALITY achievable, and keeps the lineage record
  exact.

  An empty candidate set is a REJECTION, never an :ok carrying the
  input back unchanged (rulings.md#R-empty-population-is-red). An
  operator that silently mutates nothing on the corpus it is run
  against is ADR-0165's own silence one layer up: a fault injector
  reporting success while injecting nothing."
  [events operator seed]
  (let [v (vec events)
        sites (vec ((:candidate-sites operator) v))]
    (if (empty? sites)
      (kernel/rejected :no-candidate-site
                       {:operator-id (:id operator)
                        :operator-version (:version operator)
                        :events (count v)})
      (let [site (nth sites (.nextInt (java.util.Random. (long seed)) (count sites)))
            mutant ((:fn operator) v site)]
        (kernel/ok {:mutant mutant
                    :lineage (event-lineage-for {:parent (event-content-hash v)
                                                 :operator operator
                                                 :seed seed
                                                 :site site
                                                 :produced (event-content-hash mutant)})})))))

(defn mutate
  "Applies operator (a corpus.operators registry entry) to base-data
  at site-selector. Dispatches on operator's own :format (:fhir, :v2,
  or :event) to the matching substrate; both base-data's shape and
  site-selector's meaning are format-dependent (see mutate-fhir/
  mutate-v2/mutate-event's docstrings):

    :fhir   base-data is parsed FHIR JSON;   site-selector is a locator
            envelope, {:format :path} (ehrt.kernel.locator)
    :v2     base-data is a raw ER7 string;   site-selector is a locator
            envelope
    :event  base-data is a ground-truth event log (a vector of event
            maps); site-selector is the operator's OWN SEED, and the
            operator draws its one site itself

  Returns kernel/ok {:mutant :lineage}, or:
    - the locator path's own parse rejection (:invalid-fhir-path or
      :invalid-v2-path), if the locator's :path doesn't parse under
      its format's grammar
    - kernel/rejected :locator-not-found if the parsed path doesn't
      resolve anywhere in base-data
    - kernel/rejected :no-candidate-site (:event only) if the log
      offers this operator nowhere to inject
  operator's own :fn is assumed pure and total once the site is known
  to resolve; validation of *that* is this function's job, not the
  operator's."
  [base-data operator site-selector]
  (case (:format operator)
    :fhir (mutate-fhir base-data operator site-selector)
    :v2 (mutate-v2 base-data operator site-selector)
    :event (mutate-event base-data operator site-selector)))
