(ns ehrt.cli.help-voice-test
  "The voice gate (AR-U4-3, ADR-0062): every rendered-string position in
  `cli-spec` -- `:doc`, `:meaning`, `:positional-doc`, `:default`, and
  `top-level-doc`'s own value (the last is just `cli-spec`'s own `:doc`,
  which this walk already reaches) -- must carry no agent-speak token:
  no `ADR-NNNN` citation, no milestone/session shorthand (`D9`, `SS-2`,
  `ruling 7`, `M1a`), maintainer vocabulary meaningless to an operator
  who never read this workspace's own design-channel register. The
  reasoning those tokens carried didn't disappear (2026-08-06 voice
  rewrite, ADR-0062): it relocated to a `;;` comment beside the data
  entry it used to decorate, or -- where a def already carries a
  docstring covering the same ground (`exit-codes`) -- stayed there,
  untouched, alongside a relocation comment (def-level docstrings are
  out of scope for this session, AR-U4-6; they never render, so they
  were never a voice problem).

  This gate walks the DATA (`clojure.walk/postwalk` over the realized
  `cli-spec` value), not the source text -- so it naturally excludes
  every docstring (metadata, not data) and every existing `;;` comment,
  and it naturally DE-duplicates nothing: a flag map shared across
  multiple verbs via `into` (`gate-common-flags`, reused by `gate v2`,
  `gate fhir`, and `gate v2-nist`) is walked once per verb, matching
  what a user actually sees at each of those three help pages, not the
  source's single literal definition."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [ehrt.cli.help :as help]))

(def ^:private rendered-string-positions
  #{:doc :meaning :positional-doc :default})

(defn- rendered-strings
  "Every string value at a rendered-string position, anywhere in spec
  -- walks every map node (groups, verbs, flags, exit-codes entries,
  and cli-spec's own top-level map, whose :doc IS top-level-doc's
  rendered value)."
  [spec]
  (let [acc (atom [])]
    (walk/postwalk (fn [x]
                      (when (map? x)
                        (doseq [k rendered-string-positions]
                          (let [v (get x k)]
                            (when (string? v) (swap! acc conj v)))))
                      x)
                    spec)
    @acc))

(def ^:private agent-speak-pattern
  "Word-bounded so \"MSH-7\", \"msg-%03d\", \"PID-3\", and plain dates
  survive -- verified below against every one of those exact strings."
  #"ADR-\d+|\bD\d{1,2}\b|\bSS-\d+\b|\bruling \d+\b|\bM\d+[a-z]?\b")

(defn- agent-speak-matches
  [s]
  (re-seq agent-speak-pattern s))

(deftest cli-spec-rendered-strings-carry-no-agent-speak-test
  (doseq [s (rendered-strings help/cli-spec)]
    (is (empty? (agent-speak-matches s))
        (str "agent-speak token(s) " (agent-speak-matches s)
             " found in a rendered cli-spec string: " (pr-str s)))))

;; ---- mechanism-sanity: prove the pattern actually catches what it
;; claims to, and doesn't false-positive on the legitimate lookalikes
;; already in clean cli-spec text (the pairing shape `e189418`-deleted
;; ehrt.docs-tooling.tag-law-test and -done-pointer-adr-test used) ----

(deftest agent-speak-pattern-catches-what-it-claims-test
  (testing "each token class is caught, standalone"
    (is (= ["ADR-0016"] (agent-speak-matches "skip the content-addressed verdict cache (ADR-0016)")))
    (is (= ["D13"] (agent-speak-matches "--all introduced in D13")))
    (is (= ["SS-2"] (agent-speak-matches "a generator URL, SS-2")))
    (is (= ["ruling 7"] (agent-speak-matches "a dir:/file: URL designator (ruling 7)")))
    (is (= ["M1a"] (agent-speak-matches "closed in M1a"))))
  (testing "multiple tokens in one string are all caught, in order"
    (is (= ["ADR-0015" "D9" "ADR-0019"]
           (agent-speak-matches "front door (ADR-0015); zero-flag defaults (D9, ADR-0019)"))))
  (testing "legitimate lookalikes already in clean cli-spec text survive untouched"
    (is (empty? (agent-speak-matches "against their own MSH-7 timestamps")))
    (is (empty? (agent-speak-matches "one compact MSH-7/MSH-9/PID-3 line per message")))
    (is (empty? (agent-speak-matches "the sim generator's msg-%03d output already is")))
    (is (empty? (agent-speak-matches "YYYYMMDD for synthea")))
    (is (empty? (agent-speak-matches "generation reference date" )))
    (is (empty? (agent-speak-matches "20260101")))
    (is (empty? (agent-speak-matches "received date, YYYY-MM-DD")))))
