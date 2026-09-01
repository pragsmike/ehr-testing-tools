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

  SWEEP 1 SPLIT THE CLAIM IN TWO, deliberately, and BOTH halves have
  now landed:

    commit 1 (PID-13) -- a message at MSH-12 \"2.4\" PARSES AT ALL.
      Before it, one did not: HAPI's v2.4 TN primitive rule wants
      `(NNN)NNN-NNNN`, the persona regex is `^\\d{3}-\\d{3}-\\d{4}$`,
      and `PipeParser` applies primitive validation DURING the parse,
      so it threw rather than merely warning. 346 of the probe
      corpus's 747 died there.

    commit 2 (MSH-12) -- `site-profile/default-msh` declares \"2.4\",
      so the claim below needs NO override at all and covers every kind
      the registry actually emits.

  THE OVERRIDE IS KEPT, INVERTED, as `v23-profile`: it now forces the
  OLD version, and pins what a site that must speak 2.3 still gets --
  `GenericMessage$V23`, every message, structurally unchecked. That is
  not a curiosity; it is the cost of the escape hatch, stated as a test
  rather than as prose, so a future reader cannot conclude the profile
  makes 2.3 safe.

  GROUND TRUTH IS NOT INVOLVED. The persona's own `:phone` keeps its
  `NNN-NNN-NNNN` shape; only `pid-segment`'s rendering moves. That is
  `bin/ground-truth-bracket`'s job to prove, and it is proven per
  commit rather than asserted here."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.judge-v2-hapi.v2 :as hapi]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.run :as run]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private v23-profile
  "The escape hatch the flip leaves behind, exercised rather than
  described: a site that must speak 2.3 sets this and gets exactly the
  bytes this project emitted before commit 2 -- MSH-12 aside, which is
  the point. What it also gets is the vacuity, and the test below says
  so."
  {:msh {:version "2.3"}})

(defn- messages-at
  "`nil` means the DEFAULT profile -- which is the whole claim of commit
  2, so the corpus-wide test passes nil rather than forcing a version."
  [site-profile]
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 3})]
    (emit/emit ground-truth ref-date utc-offset facility providers site-profile)))

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

(def ^:private corpus-config
  "A run wide enough that \"no message resolves to GenericMessage\" is a
  claim about a CORPUS rather than about two triggers. Admission,
  order, transfer and discharge on the pathway; `churn/sample-profile`
  for the cancel/swap/merge family; a GMF module for the ambulatory
  half. 237 messages over 8 of the registry's 12 families.

  WHAT IT DOES NOT REACH, named rather than left to be inferred from a
  passing test (`rulings.md#R-population-closure`): **ADT^A11**
  (cancel-admit), **ADT^A13** (cancel-discharge) and **ADT^A17**
  (bed-swap) are churn's own lottery and this seed does not draw them
  -- A13 in particular went unreached across all of arc 3b too;
  **ADT^A20** (bed-status-change) needs the `:bed-cycle` opt-in, which
  rides `:config` through `ehrt.sim.run` and is out of `engine/run`'s
  reach here. All four DO resolve to real v2.4 structures -- ADR-0175
  section 2(e) measured the full 12-family table over its own 747-
  message probe corpus -- but that is the ADR's measurement, not this
  gate's, and the distinction is the point."
  {:seed 7 :patients 40})

(defn- corpus-messages []
  (let [module (:payload (patient-simulator/load-module
                          "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json"))))
        {:keys [ground-truth facility providers]}
        (run/run (assoc corpus-config
                           :pathways [{:pathway {:name "cbc"
                                                 :steps [{:type :admission :location "Renal"}
                                                         {:type :order :profile :cbc}
                                                         {:type :delay :from 30 :to 30}
                                                         {:type :transfer :location "Cardiology"}
                                                         {:type :delay :from 30 :to 30}
                                                         {:type :discharge}]}
                                       :weight 1}]
                           :churn-profile churn/sample-profile
                           :modules [(patient-simulator/singleton-closure module)]
                           :module-assignment [{:module-id "sinusitis" :weight 1}]
                           :module-horizon-days 3650))]
    (emit/emit ground-truth ref-date utc-offset facility providers nil)))

(deftest an-a01-under-the-default-profile-resolves-to-a-real-v2-4-structure-test
  (let [a01 (first (filter #(str/includes? (msh-9-of %) "A01") (messages-at nil)))
        resolved (resolved-structure a01)]
    (testing "sanity: the DEFAULT profile now declares 2.4 -- commit 2's whole claim"
      (is (some? a01) "the seeded run must produce at least one admission message")
      (is (= "2.4" (msh-field a01 12))
          (str "MSH-12 must read 2.4 with NO site profile at all -- if this needs an "
               "override, `site-profile/default-msh` did not move:\n" a01)))
    (testing "it parses at all -- the PID-13 rendering rule (ADR-0175 A1, commit 1)"
      (is (class? resolved)
          (str "HAPI's v2.4 TN primitive rule wants a parenthesised area code, and "
               "`PipeParser` enforces primitives DURING the parse. A PID-13 of the persona's "
               "own `NNN-NNN-NNNN` shape therefore does not warn -- it throws, and the message "
               "never resolves to a structure at all. 346 of the probe corpus's 747 died here. "
               "Got: " (pr-str resolved) "\n\n" a01)))
    (testing "and it resolves to the real v2.4 ADT_A01, not a generic fallback"
      (is (= "ca.uhn.hl7v2.model.v24.message.ADT_A01" (.getName ^Class resolved))
          "at MSH-12 \"2.3\" this read `ca.uhn.hl7v2.model.GenericMessage$V23`"))))

(deftest no-message-in-the-default-corpus-falls-back-to-a-generic-structure-test
  (let [messages (corpus-messages)
        resolutions (mapv (juxt identity resolved-structure) messages)
        threw (filter (comp map? second) resolutions)
        generic (filter #(and (class? (second %))
                              (str/includes? (.getName ^Class (second %)) "GenericMessage"))
                        resolutions)
        families (frequencies (map (comp msh-9-of first) resolutions))]
    (testing "sanity: there is a corpus to speak about (R-empty-population-is-red)"
      (is (seq messages) "the seeded run emitted nothing -- every assertion below would be vacuous")
      (is (<= 200 (count messages))
          (str "this claim is only worth making at corpus scale; " (count messages) " messages")))
    (testing "the family spread is PINNED, so a reshuffle that narrows it turns this red"
      ;; Not a decoration. Without it, a config change that silently
      ;; dropped every family but A01/A03 would leave the two
      ;; assertions below passing over a corpus that proves much less.
      (is (= #{"ADT^A01" "ADT^A02" "ADT^A03" "ADT^A04" "ADT^A12" "ADT^A40" "ORM^O01" "ORU^R01"}
             (set (keys families)))
          (str "measured 2026-08-27 at " (pr-str corpus-config) ". Got " (pr-str families))))
    (testing "every message parses"
      (is (empty? threw)
          (str (count threw) " of " (count messages) " messages failed to parse. First: "
               (pr-str (first threw)))))
    (testing "and none of them falls back to GenericMessage -- `gate v2` is no longer vacuous"
      (is (empty? generic)
          (str "a message that resolves to GenericMessage is one `gate v2` cannot check the "
               "structure of. Before this sweep EVERY message this project emitted was one. "
               "Offenders: "
               (pr-str (map (fn [[m c]] [(msh-9-of m) (.getName ^Class c)]) generic)))))
    (testing "the resolved structures are reported, not just counted"
      (println "  resolved v2.4 structures over" (count messages) "messages:"
               (pr-str (into (sorted-map)
                             (frequencies (map (fn [[m c]]
                                                 [(msh-9-of m)
                                                  (when (class? c) (.getSimpleName ^Class c))])
                                               resolutions)))))
      (is true))))

(deftest the-2-3-escape-hatch-still-works-and-is-still-structurally-vacuous-test
  (let [messages (messages-at v23-profile)
        resolutions (mapv resolved-structure messages)]
    (testing "sanity: the override really forces the old version"
      (is (seq messages))
      (is (every? #(= "2.3" (msh-field % 12)) messages)
          "a site profile must still be able to declare 2.3 -- that is the flip's escape hatch"))
    (testing "and what such a site gets is a GENERIC message, every time"
      ;; The cost of the hatch, pinned as a test rather than described
      ;; in prose, so nobody concludes the profile makes 2.3 safe. There
      ;; is no v2.3 structure library on any classpath in this tree, so
      ;; HAPI has nothing to resolve against: no segment order, no
      ;; cardinality, no required-segment check, no primitive typing.
      ;; This is exactly the state the WHOLE corpus was in before this
      ;; sweep (ADR-0175 section 1(iv)).
      (is (every? #(and (class? %) (str/includes? (.getName ^Class %) "GenericMessage"))
                  resolutions)
          (str "expected every 2.3 message to fall back to GenericMessage. Got: "
               (pr-str (map #(if (class? %) (.getName ^Class %) %) resolutions)))))))

;; ---------------------------------------------------------------------
;; WHAT THE FLIP ACTUALLY BUYS -- measured, and narrower than the ADR
;; ---------------------------------------------------------------------

(defn- at-version
  "The same message with MSH-12 rewritten."
  [er7 v]
  (let [[msh & rest] (str/split er7 #"\r")
        fields (vec (str/split msh #"\|" -1))]
    (str/join "\r" (cons (str/join "|" (assoc fields 11 v)) rest))))

(defn- gate-finds-something?
  "True when `ehrt.judge-v2-hapi.v2/execute` -- the real engine behind
  `gate v2` -- reports anything at all about `er7`."
  [er7]
  (let [raw (hapi/execute er7)]
    (boolean (or (:parse-exception raw) (seq (:validation-exceptions raw))))))

(deftest the-2-4-flip-buys-primitive-typing-and-only-primitive-typing-test
  (let [a01 (first (filter #(str/includes? (msh-9-of %) "A01") (messages-at nil)))
        segs (vec (str/split a01 #"\r"))
        damaged
        {:malformed-primitive
         (str/join "\r" (map #(if (str/starts-with? % "PID|")
                                (str/replace % #"\|19(\d{6})\|" "|NOTADATE|") %) segs))
         :missing-required-segment
         (str/join "\r" (remove #(str/starts-with? % "PID|") segs))
         :unknown-segment
         (str/join "\r" (conj segs "ZZZ|1|nonsense"))
         :segments-out-of-order
         (str/join "\r" (let [i (first (keep-indexed (fn [i x] (when (str/starts-with? x "PID|") i)) segs))
                              j (first (keep-indexed (fn [i x] (when (str/starts-with? x "EVN|") i)) segs))]
                          (assoc segs i (segs j) j (segs i))))}]
    (testing "sanity: the fixture is a real A01 and is itself clean at 2.4"
      (is (some? a01))
      (is (not (gate-finds-something? a01))
          "an undamaged message must pass, or every contrast below is meaningless"))

    (testing "PRIMITIVE TYPING is what the flip restores -- caught at 2.4, missed at 2.3"
      ;; This is the sweep's headline, reduced to one falsifiable pair.
      ;; At MSH-12 "2.3" HAPI has no v2.3 structures on any classpath
      ;; here, resolves to GenericMessage$V23, and applies no datatype
      ;; rule to any field. At "2.4" it resolves the real structure and
      ;; `PipeParser` enforces primitives DURING the parse.
      (is (not (gate-finds-something? (at-version (:malformed-primitive damaged) "2.3")))
          "at 2.3 a garbage PID-7 passes -- that IS the vacuity this sweep closed")
      (is (gate-finds-something? (:malformed-primitive damaged))
          "at 2.4 the same garbage PID-7 must be caught"))

    (testing "AND NOTHING ELSE DOES. ADR-0175 section 1(iv) is too broad, measured."
      ;; The ADR describes the 2.3 state as "no segment order, no
      ;; cardinality, no required-segment check, no primitive typing".
      ;; Of those four the flip restores ONLY the last. HAPI's
      ;; `PipeParser` under `ValidationContextFactory/defaultValidation`
      ;; is lenient about which segments are present, about segments it
      ;; does not recognise, and about their order -- with a real v2.4
      ;; structure resolved, all three still pass.
      ;;
      ;; Pinned as a test rather than left as prose so that "gate v2 is
      ;; no longer vacuous" cannot grow into "gate v2 checks structure".
      ;; It checks TYPES. Structural conformance needs the profile tier
      ;; (`gate v2-nist`), which ADR-0175 section 1(iii) records as
      ;; unable to run over this project's own corpus at all today.
      (doseq [k [:missing-required-segment :unknown-segment :segments-out-of-order]]
        (is (not (gate-finds-something? (damaged k)))
            (str "measured 2026-08-27: " (name k) " passes at 2.4 too. If this ever starts "
                 "failing, HAPI's default validation got stricter and the claim above -- that "
                 "the flip buys typing alone -- needs re-measuring, not deleting."))))))
