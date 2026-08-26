(ns ehrt.docs-tooling.oracle-coverage-test
  "ADR-0156, closing register rows L1-2, L1-3, L1-4, L1-5 -- the oracle's
  coverage claim, and the soundness check that is supposed to protect it.

  Review 4's L-1 dimension found `IDENTICAL` is read as meaning more
  than it does. Three separate gaps, one subject:

  (a) `digest.clj`'s docstring opened `Six roots, ...` and its dated
      notes account for 11. The map holds **35**. Not a false claim --
      each note is dated and scoped -- but a reader who stops at the
      docstring gets a third of the population (L1-5).

  (b) Nothing anywhere states what no root can move. 13 of the event
      contract's 23 closed kinds are witnessed; 8 are not, and the
      capacity witness is ONE ROOT DEEP -- lose `death-fixture` and
      `:transfer`, `ADT^A02`, `:bed-ready` and ladder rung 3 all go dark
      together (L1-1, L1-2).

  (c) `bin/regression-oracle`'s soundness check diffed `digest.clj`
      OUTSIDE its `(ns ...)` form, so a `:require` change -- the exact
      class the standing-equipment promotion made, which turned
      `run-walk`'s interpreter call from 6-arg to 8-arg -- passed
      silently. `rulings.md#R-oracle-script-contract` said the script
      \"aborts on an undeclared digest-source diff\", which overstated
      what it did (L1-4). Author ruling R4-Q6 (iii) (c): widen the diff
      AND amend the rule's text.

  WHAT THIS FILE GATES, and what it deliberately does not. The claim in
  `digest.clj` is a set of event kinds and message types. Asserting it
  against a FRESH 35-root digest costs 114 seconds (ADR-0156 Step 0 b),
  so that half lives in the scheduled lane:
  `ehrt.integration.oracle-coverage-test`. Putting only that there would
  leave the claim ungated on every push, so the checks below are the
  per-push half -- shape, population, membership, and LOCATION.

  The location check is the one that is easy to skip and the one that
  matters most. A coverage claim that sits outside the soundness body
  can drift without any oracle bracket noticing -- which is exactly how
  the `Six roots` line survived 29 roots' worth of growth. Committing
  the claim INSIDE the compared region means widening or narrowing
  coverage forces `--declared-digest-change`, and the session that pays
  it has to say why."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private digest-path
  "components/oracle/src/ehrt/oracle/digest.clj")

(def ^:private oracle-script "bin/regression-oracle")

(def ^:private schema-path
  "The committed export, itself freshness-gated against the Clojure
  source by `ehrt.docs-tooling.event-schema-test`. Read the export
  rather than requiring `sim-engine`: `docs-tooling` does not compose it
  and does not need to, and the export is the contract a consumer sees."
  "components/sim-engine/resources/sim-engine/event-schema.edn")

;; ---------------------------------------------------------------------
;; reading the committed claim out of digest.clj
;; ---------------------------------------------------------------------

(defn- def-form
  "The value of a top-level `(def <name> ...)` in `source`, read as EDN.
  Returns nil when there is no such def -- which is the red state this
  file was written against, not an error to swallow: every caller below
  asserts on it."
  [source name]
  (when-let [i (some #(str/index-of source %)
                     [(str "(def " name) (str "(def ^:private " name)])]
    (let [after (subs source i)
          open (str/index-of after "#{")]
      (when open
        (let [close (str/index-of after "}" open)]
          (edn/read-string (subs after open (inc close))))))))

(defn- digest-source [] (slurp digest-path))

(defn- closed-event-kinds
  "The 23-kind closed vocabulary, read from the committed schema export's
  own `:multi` dispatch rather than retyped here."
  []
  (let [schema (edn/read-string (slurp schema-path))
        multi (letfn [(walk [form]
                        (cond (and (vector? form) (= :multi (first form))) form
                              (coll? form) (some walk form)
                              :else nil))]
                (walk schema))]
    (into (sorted-set) (map first (drop 2 multi)))))

;; ---------------------------------------------------------------------
;; the soundness body, run through the script's OWN function
;; ---------------------------------------------------------------------

(defn- digest-body-of
  "`bin/regression-oracle`'s own `digest_body_of`, extracted from the
  committed script and run over `file`. Extracted rather than
  reimplemented: a reimplementation would test this test's idea of the
  rule, and the whole point of L1-4 is that the SCRIPT's idea of it was
  narrower than the rule it was cited for."
  [file]
  (let [script (slurp oracle-script)
        start (str/index-of script "digest_body_of() {")
        end (str/index-of script "\n}" start)
        fn-text (subs script start (+ end 2))
        {:keys [exit out err]}
        (shell/sh "bash" "-c" (str fn-text "\ndigest_body_of \"$1\"") "--" (str file))]
    (when-not (zero? exit)
      (throw (ex-info (str "extracted digest_body_of failed: " err) {:exit exit})))
    out))

(defn- temp-copy
  "digest.clj copied to a scratch file, with `edit` applied to its text."
  [edit]
  (let [f (java.io.File/createTempFile "digest-scratch" ".clj")]
    (.deleteOnExit f)
    (spit f (edit (digest-source)))
    f))

;; ---------------------------------------------------------------------
;; (a) the claim exists, is populated, and says something true
;; ---------------------------------------------------------------------

(deftest the-committed-coverage-claim-is-populated-and-drawn-from-the-closed-vocabulary-test
  (let [source (digest-source)
        kinds (def-form source "witnessed-event-kinds")
        types (def-form source "witnessed-message-types")
        closed (closed-event-kinds)]
    (testing "the claim exists at all (L1-2: nothing stated what no root can move)"
      (is (some? kinds)
          (str "`" digest-path "` must commit a `witnessed-event-kinds` set -- the gate "
               "R4-Q6 (ii) (a) rules for, so that widening or narrowing coverage forces the "
               "coverage claim to move with it."))
      (is (some? types)
          (str "`" digest-path "` must commit a `witnessed-message-types` set: the emitter "
               "half of the same claim. ORM^O01 is emitted by no root (L1-7), and a claim "
               "that named only event kinds would not say so.")))
    (testing "populated -- rulings.md#R-empty-population-is-red"
      (is (seq kinds)
          "an empty witnessed set would make every membership assertion below vacuously true")
      (is (seq types) "likewise for the message-type half"))
    (testing "every committed kind is a real event kind"
      (is (= 23 (count closed))
          (str "sanity on this test's own population source: the committed export must still "
               "hold the 23-kind closed vocabulary. Found " (count closed) "."))
      (is (every? closed kinds)
          (str "a committed kind outside the closed vocabulary is a typo or a stale claim. "
               "Offenders: " (pr-str (remove closed kinds)))))
    (testing "the claim is a PROPER subset -- coverage is thin, and saying so is the point"
      (is (< (count kinds) (count closed))
          (str "if every closed kind were witnessed there would be no vacuous set to name, and "
               "L1-2's finding would be void. Committed " (count kinds) " of " (count closed) ".")))))

;; ---------------------------------------------------------------------
;; (b) the claim sits INSIDE the compared region
;; ---------------------------------------------------------------------

(deftest the-coverage-claim-lives-inside-the-soundness-body-test
  (let [body (digest-body-of digest-path)]
    (testing "sanity: the extracted body is not empty"
      (is (str/includes? body "(def ^:private roots")
          "the `roots` map must be inside the soundness body -- if it is not, this test's own
          premise is broken and every assertion below is vacuous"))
    (testing "the coverage claim is inside it (L1-5's real lesson)"
      (is (str/includes? body "witnessed-event-kinds")
          "a coverage claim outside the compared region can drift with no bracket noticing --
          which is how `Six roots` survived to 35 roots")
      (is (str/includes? body "witnessed-message-types")
          "same for the emitter half"))))

;; ---------------------------------------------------------------------
;; (c) the soundness check must see the ns form -- L1-4, R4-Q6 (iii)
;; ---------------------------------------------------------------------

(deftest the-soundness-check-sees-a-require-only-change-test
  (let [before (digest-body-of digest-path)
        scratch (temp-copy #(str/replace %
                                         "[clojure.string :as str]"
                                         "[clojure.string :as str]\n            [clojure.set :as set]"))
        after (digest-body-of scratch)]
    (testing "the edit really landed (else this test proves nothing)"
      (is (str/includes? (slurp scratch) "[clojure.set :as set]")
          "scratch copy must actually carry the added require"))
    (testing "a `:require`-only change is a digest-source change (L1-4)"
      (is (not= before after)
          (str "`digest_body_of` must report a `:require` change as a difference. The old awk "
               "started at the first `^(defn` line, so the whole `(ns ...)` form -- 4 requires -- "
               "was outside the compared region. That is the exact class the standing-equipment "
               "promotion made: it repointed every require from implementation to interface "
               "namespaces and turned `run-walk`'s call from 6-arg to 8-arg. "
               "`rulings.md#R-oracle-script-contract` claims the script aborts on an undeclared "
               "digest-source diff; until this passes, it does not.")))))

(deftest the-soundness-body-still-excludes-the-leading-docstring-test
  (let [before (digest-body-of digest-path)
        scratch (temp-copy #(str/replace % "Standing regression-oracle equipment"
                                         "Standing regression-oracle equipment (prose tweak)"))
        after (digest-body-of scratch)]
    (testing "R4-Q6 (iii) (a) is 'whole file MINUS the docstring', not 'whole file'"
      (is (= before after)
          (str "editing the leading docstring must NOT force `--declared-digest-change`. The "
               "docstring is dated historical narrative; every claim that must not drift "
               "silently goes in the COVERAGE block inside the body instead. Widening to the "
               "whole file would make every dated note a declared oracle change.")))))

(deftest the-soundness-body-prints-each-line-once-test
  (let [body (digest-body-of digest-path)
        source (digest-source)
        defn-lines (count (filter #(str/starts-with? % "(defn") (str/split-lines source)))
        body-defn-lines (count (filter #(str/starts-with? % "(defn") (str/split-lines body)))]
    (testing "the old awk printed every `(defn` line twice"
      (is (= defn-lines body-defn-lines)
          (str "`awk 'found{print} /^\\(defn/{found=1; print}'` matched BOTH rules on every "
               "`(defn` line after the first, printing each one twice -- 484 real lines rendered "
               "as a 524-line body. Harmless to the diff (both sides duplicated alike) and "
               "wrong in every line count taken from it, including register row L1-4's own. "
               "Source has " defn-lines " `(defn` lines, body has " body-defn-lines ".")))))

;; ---------------------------------------------------------------------
;; (d) the docstring's own root count -- L1-5
;; ---------------------------------------------------------------------

(deftest the-docstring-accounts-for-every-root-test
  (let [source (digest-source)
        roots-map (subs source
                        (str/index-of source "(def ^:private roots")
                        (str/index-of source "(defn -main"))
        roots (count (re-seq #"\"[a-z0-9-]+\"\s+[a-z0-9-]+(?:-pair|-batch)" roots-map))
        docstring (subs source 0 (str/index-of source "(:require"))]
    (testing "sanity: the roots map parses"
      (is (pos? roots) "must find at least one root in the map")
      (is (= 35 roots)
          (str "35 roots today; a root added or removed moves this number and the docstring "
               "paragraph together. Found " roots ".")))
    (testing "the docstring states the CURRENT population (L1-5)"
      (is (str/includes? docstring (str roots " roots"))
          (str "the docstring must state its own root count. It opened `Six roots, matching "
               "this session's own J1 ruling verbatim` and its dated notes add three then two "
               "-- 11 of " roots ". A cold reader who stops at the docstring gets a third of "
               "the population."))
      (is (not (str/includes? (str/replace docstring #"(?s)`[^`]*`" "") "Six roots"))
          (str "the superseded `Six roots` opening must no longer be STATED. Quoting it inside "
               "backticks, as the history of what this paragraph replaced, is exactly what "
               "`rulings.md#R-dated-addendum-not-silent-edit` asks for -- so backtick-quoted "
               "spans are stripped before this check, and only a bare restatement is red.")))))
