(ns ehrt.tools.quickstart-fresh-test
  "DOC-5: README.md's Quickstart fence and bin/quickstart-demo teach the
  identical commands, in the identical order (AUTHORS-GUIDE.md sec7 --
  a check proven able to fail before it's trusted to pass)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.tools.quickstart-fresh :as qf])
  (:import [java.io File]))

(defn- temp-file!
  [content]
  (let [f (File/createTempFile "quickstart-fresh-fixture" ".txt")]
    (spit f content)
    (.getAbsolutePath f)))

;; ---- the committed files: the real proof this check exists for ----

(deftest committed-readme-and-script-agree-test
  (let [{:keys [ok? readme-count script-count divergence]} (qf/check)]
    (is ok? (str "divergence: " divergence))
    (is (nil? divergence))
    (is (= 14 readme-count))
    (is (= readme-count script-count))))

;; ---- extraction on its own: comments/blanks stripped, continuation
;; lines kept as separate entries, order preserved ----

(deftest readme-command-lines-strips-comments-and-blanks-test
  (let [readme (temp-file! (str "# prelude, not a fence\n"
                                 "```sh\n"
                                 "# a leading comment\n"
                                 "\n"
                                 "bin/ehr help\n"
                                 "\n"
                                 "bin/ehr corpus generate --seed 1 \\\n"
                                 "  --output-dir out/x\n"
                                 "```\n"
                                 "not part of the fence\n"))]
    (is (= ["bin/ehr help"
            "bin/ehr corpus generate --seed 1 \\"
            "  --output-dir out/x"]
           (qf/readme-command-lines readme)))))

(deftest script-command-lines-unwraps-expect-and-expect-eval-test
  (let [script (temp-file! (str "#!/usr/bin/env bash\n"
                                 "# setup, not a taught command\n"
                                 "echo hello\n"
                                 "# BEGIN quickstart commands (verbatim from README.md's Quickstart fence)\n"
                                 "expect 0 bin/ehr help\n"
                                 "\n"
                                 "expect_eval 0 'X=$(echo hi)'\n"
                                 "expect 1 bin/ehr gate fhir out --report out/r.edn\n"
                                 "# END quickstart commands\n"
                                 "echo done\n"))]
    (is (= ["bin/ehr help"
            "X=$(echo hi)"
            "bin/ehr gate fhir out --report out/r.edn"]
           (qf/script-command-lines script)))))

;; ---- seeded divergence: the check must be able to fail before it's
;; trusted to pass ----

(deftest check-catches-an-altered-line-test
  (let [readme (temp-file! (str "```sh\n"
                                 "bin/ehr help\n"
                                 "bin/ehr gate v2 test/fixtures/v2\n"
                                 "```\n"))
        script (temp-file! (str "# BEGIN quickstart commands (verbatim from README.md's Quickstart fence)\n"
                                 "expect 0 bin/ehr help\n"
                                 "expect 0 bin/ehr gate v2 test/fixtures/v2-typo\n"
                                 "# END quickstart commands\n"))
        {:keys [ok? divergence]} (qf/check {:readme-path readme :script-path script})]
    (is (not ok?))
    (is (= 1 (:index divergence)))
    (is (= "bin/ehr gate v2 test/fixtures/v2" (:readme divergence)))
    (is (= "bin/ehr gate v2 test/fixtures/v2-typo" (:script divergence)))))

(deftest check-catches-a-missing-trailing-command-test
  (let [readme (temp-file! (str "```sh\n"
                                 "bin/ehr help\n"
                                 "make test\n"
                                 "```\n"))
        script (temp-file! (str "# BEGIN quickstart commands (verbatim from README.md's Quickstart fence)\n"
                                 "expect 0 bin/ehr help\n"
                                 "# END quickstart commands\n"))
        {:keys [ok? divergence]} (qf/check {:readme-path readme :script-path script})]
    (is (not ok?))
    (is (= 1 (:index divergence)))
    (is (= "make test" (:readme divergence)))
    (is (= :ehrt.tools.quickstart-fresh/missing (:script divergence)))))
