(ns ehrt.conformance.v2-structure-resolution-test
  "Does `gate v2` actually see a STRUCTURE when it parses this project's
  own output? (arc 4 sweep 1, `notes/adr/0175-arc-4-emission-add-ons.md`
  ruling A1.)

  THE FINDING THIS GATE EXISTS FOR. ADR-0175 section 1(iv) measured
  what HAPI does with every message this project emits at MSH-12
  \"2.3\": all 747 of the probe corpus resolve to
  `ca.uhn.hl7v2.model.GenericMessage$V23`. There is no v2.3 structure
  library on any classpath in this tree -- `components/judge-v2-hapi`
  vendors `hapi-structures-v24` and nothing else -- so HAPI falls back
  to a generic message with no segment order, no cardinality, no
  required-segment check and no primitive typing. The base-structural
  tier this project ships has therefore been VACUOUS over this
  project's own corpus for its whole life. It still catches encoding
  and delimiter damage on the foreign fixtures its own tests use; over
  our own output it checks nothing.

  WHY THIS NAMESPACE LIVES IN `projects/conformance`. It needs the
  emitter and a judge engine in one place, and the dependency law
  forbids `components/sim-emit-hl7` depending on anything but
  `components/sim-model`. This project is the seam that already
  composes both.

  IT PARSES THROUGH THE JUDGE'S OWN CONTEXT, not a copy of it:
  `#'hapi/new-context` is the private constructor
  `ehrt.judge-v2-hapi.v2/execute` itself calls. A hand-built
  `DefaultHapiContext` here would be a second opinion about what
  `gate v2` does, and this whole namespace is a claim about what it
  actually does.

  SWEEP 1 SPLITS THE CLAIM IN TWO, deliberately, and the split is why
  the version flip is two commits rather than one:

    commit 1 (PID-13) -- a message at MSH-12 \"2.4\" PARSES AT ALL.
      Today it does not: HAPI's v2.4 TN primitive rule wants
      `(NNN)NNN-NNNN`, the persona regex is `^\\d{3}-\\d{3}-\\d{4}$`,
      and `PipeParser` applies primitive validation DURING the parse,
      so it throws rather than merely warning. 346 of the probe
      corpus's 747 died there. The version is forced with a site
      profile here, because the emitter still declares \"2.3\".

    commit 2 (MSH-12) -- the DEFAULT declares \"2.4\", so the
      corpus-wide claim can drop the site-profile override and assert
      over every kind the registry emits.

  GROUND TRUTH IS NOT INVOLVED. The persona's own `:phone` keeps its
  `NNN-NNN-NNNN` shape; only `pid-segment`'s rendering moves. That is
  `bin/ground-truth-bracket`'s job to prove, and it is proven per
  commit rather than asserted here."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.judge-v2-hapi.v2 :as hapi]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-engine.engine :as engine]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private v24-profile
  "Sweep 1 declares nothing: `site-profile/default-msh` still says
  \"2.3\" until sweep 1's second commit. Forcing the version through
  the profile is what makes THIS commit's claim -- that a 2.4 message
  built from our own PID parses -- testable before the default moves.
  It also demonstrates the escape hatch the flip leaves behind: a site
  that must speak 2.3 keeps `{:msh {:version \"2.3\"}}`."
  {:msh {:version "2.4"}})

(defn- messages-at
  [site-profile]
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 3})]
    (emit-hl7/emit ground-truth ref-date utc-offset facility providers site-profile)))

(defn- resolved-structure
  "The class HAPI resolves `er7` to, through the judge's own context.
  Returns the class, or `{:threw <message>}` when the parse itself
  fails -- which is the shape a primitive-validation failure takes,
  because `PipeParser` validates primitives while parsing rather than
  after."
  [er7]
  (try
    (class (.parse (.getPipeParser (#'hapi/new-context)) er7))
    (catch Exception e {:threw (.getMessage e)})))

(defn- msh-field
  "MSH-`n` of `er7`. MSH-1 is the field separator itself, so the value
  at MSH-n sits at index n-1 of the split -- and MSH-12 is the LAST
  field, terminated by the segment separator rather than a trailing
  `|`, which is why this splits the segment instead of matching on
  `\"|2.4|\"`."
  [er7 n]
  (nth (str/split (first (str/split er7 #"\r")) #"\|" -1) (dec n) nil))

(defn- msh-9-of [er7] (msh-field er7 9))

(deftest an-a01-at-2-4-resolves-to-a-real-v2-4-structure-test
  (let [a01 (first (filter #(str/includes? (msh-9-of %) "A01") (messages-at v24-profile)))
        resolved (resolved-structure a01)]
    (testing "sanity: the fixture really is an A01 declaring 2.4"
      (is (some? a01) "the seeded run must produce at least one admission message")
      (is (= "2.4" (msh-field a01 12)) (str "MSH-12 must read 2.4 in:\n" a01)))
    (testing "it parses at all -- the PID-13 rendering rule (ADR-0175 A1, commit 1)"
      (is (class? resolved)
          (str "HAPI's v2.4 TN primitive rule wants a parenthesised area code, and "
               "`PipeParser` enforces primitives DURING the parse. A PID-13 of the persona's "
               "own `NNN-NNN-NNNN` shape therefore does not warn -- it throws, and the message "
               "never resolves to a structure at all. 346 of the probe corpus's 747 died here. "
               "Got: " (pr-str resolved) "\n\n" a01)))
    (testing "and it resolves to the real v2.4 ADT_A01, not a generic fallback"
      (is (= "ca.uhn.hl7v2.model.v24.message.ADT_A01" (.getName ^Class resolved))
          (str "at MSH-12 \"2.3\" this reads `ca.uhn.hl7v2.model.GenericMessage$V23` for every "
               "message this project emits, because no v2.3 structure library is on any "
               "classpath here -- which is what makes `gate v2` structurally vacuous over our "
               "own corpus (ADR-0175 section 1(iv)).")))))

(deftest no-message-in-a-2-4-corpus-falls-back-to-a-generic-structure-test
  (let [messages (messages-at v24-profile)
        resolutions (into {} (map (juxt identity resolved-structure)) messages)
        threw (filter (comp map? val) resolutions)
        generic (filter #(and (class? (val %))
                              (str/includes? (.getName ^Class (val %)) "GenericMessage"))
                        resolutions)]
    (testing "sanity: there is a corpus to speak about (R-empty-population-is-red)"
      (is (seq messages) "the seeded run emitted nothing -- every assertion below would be vacuous"))
    (testing "every message parses"
      (is (empty? threw)
          (str (count threw) " of " (count messages) " messages failed to parse at 2.4. First: "
               (pr-str (first threw)))))
    (testing "and none of them falls back to GenericMessage"
      (is (empty? generic)
          (str "a message that resolves to GenericMessage is one `gate v2` cannot check the "
               "structure of. Offenders: "
               (pr-str (map (fn [[m c]] [(msh-9-of m) (.getName ^Class c)]) generic)))))
    (testing "the resolved structures are reported, not just counted"
      (println "  resolved v2.4 structures over" (count messages) "messages:"
               (pr-str (frequencies (map (fn [[m c]]
                                           [(msh-9-of m)
                                            (when (class? c) (.getSimpleName ^Class c))])
                                         resolutions))))
      (is true))))
