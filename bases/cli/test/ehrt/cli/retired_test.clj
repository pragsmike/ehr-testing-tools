(ns ehrt.cli.retired-test
  "The tombstone gate (AR-EP-1, ux epilogue, `notes/adr/0065-ux-
  epilogue.md`): `retired-message` must read as an operator-facing
  redirect, not an internal note leaking into a stderr line a real
  user might paste into a bug report. Reuses `help-voice-test`'s own
  agent-speak pattern (same reasoning: no `ADR-NNNN` citation, no
  milestone/session shorthand) rather than redefining it, since the
  voice bar this message must clear is the same one `cli-spec`'s own
  rendered strings already clear."
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [ehrt.cli.retired :as retired]))

(def ^:private agent-speak-pattern
  #"ADR-\d+|\bD\d{1,2}\b|\bSS-\d+\b|\bruling \d+\b|\bM\d+[a-z]?\b")

(deftest retired-message-carries-no-agent-speak-test
  (let [msg (retired/retired-message)]
    (is (empty? (re-seq agent-speak-pattern msg))
        (str "agent-speak token(s) found in retired-message: " (pr-str msg)))))

(deftest retired-message-names-the-live-entry-point-test
  (is (str/includes? (retired/retired-message) "bin/ehrt")
      "retired-message must name bin/ehrt as the replacement"))

(deftest retired-message-does-not-teach-the-retired-invocation-test
  (is (not (str/includes? (retired/retired-message) "clojure -M:cli run"))
      "retired-message may NAME clojure -M:cli as retired, but must never show it as a runnable worked example again"))

(deftest retired-message-shows-a-worked-example-and-a-help-pointer-test
  (let [msg (retired/retired-message)]
    (is (str/includes? msg "bin/ehrt sim run")
        "retired-message must show one worked example mirroring the founding shape")
    (is (str/includes? msg "run bin/ehrt help for commands")
        "retired-message must end by pointing at bin/ehrt help")))
