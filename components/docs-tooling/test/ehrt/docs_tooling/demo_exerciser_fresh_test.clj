(ns ehrt.docs-tooling.demo-exerciser-fresh-test
  "R3 (notes/ADRs.md ADR-0113, author verbatim: 'The demos must be known
  to work, and exercised as documented to make sure they actually play
  out as written'): demos/scenarios/ed-tuesday/README.md's own fenced
  command sequence and bin/demo-exerciser-ed-tuesday's taught command
  list must be IDENTICAL, in order -- the fast, per-push half of the
  demo-exerciser's own two-gate shape (the slow half, actually running
  the commands, is bin/demo-exerciser-ed-tuesday itself, integration-
  tier, wired into `make integration`)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.demo-exerciser-fresh :as def]))

;; ---- the committed files: the real proof this check exists for ----

(deftest committed-readme-and-script-agree-test
  (let [{:keys [ok? readme-count script-count divergence]} (def/check)]
    (is ok? (str "divergence: " divergence
                  " -- readme-count " readme-count " script-count " script-count))
    (is (nil? divergence))
    (is (= 21 readme-count))
    (is (= readme-count script-count))))

;; ---- extraction on its own: multi-fence, mixed bash/`$`-transcript
;; style, continuation lines kept as their own list entries, unmerged,
;; board-snapshot/JSON-payload output with no command prefix skipped ----

(defn- temp-file!
  [content]
  (let [f (java.io.File/createTempFile "demo-exerciser-fresh-fixture" ".md")]
    (spit f content)
    (.getAbsolutePath f)))

(deftest readme-command-lines-walks-every-fence-in-order-test
  (let [readme (temp-file!
                (str "# Scenario\n\n"
                     "## Generate\n\n"
                     "```bash\n"
                     "bin/ehrt corpus generate sim --seed 1 --patients 5 \\\n"
                     "  --out-dir out/scenarios/x\n"
                     "```\n\n"
                     "Some prose between fences, not a command.\n\n"
                     "```\n"
                     "-- board snapshot: 2026-08-11T00:00:00Z --\n"
                     "inpatients: 4  active outpatients: 0\n"
                     "```\n\n"
                     "```\n"
                     "$ diff a/events.edn b/events.edn\n"
                     "$ sha256sum a/events.edn b/events.edn\n"
                     "deadbeef  a/events.edn\n"
                     "deadbeef  b/events.edn\n"
                     "```\n"))]
    (is (= ["bin/ehrt corpus generate sim --seed 1 --patients 5 \\"
            "  --out-dir out/scenarios/x"
            "diff a/events.edn b/events.edn"
            "sha256sum a/events.edn b/events.edn"]
           (def/readme-command-lines readme)))))

(deftest readme-command-lines-skips-blank-and-prose-lines-inside-a-fence-test
  (let [readme (temp-file!
                (str "```bash\n"
                     "bin/ehrt help\n"
                     "\n"
                     "bin/ehrt corpus generate\n"
                     "```\n"))]
    (is (= ["bin/ehrt help" "bin/ehrt corpus generate"]
           (def/readme-command-lines readme)))))

(deftest script-command-lines-nil-when-script-absent-test
  (is (nil? (def/script-command-lines "/nonexistent/bin/demo-exerciser-ed-tuesday"))))

(deftest script-command-lines-unwraps-expect-and-expect-eval-test
  (let [script (temp-file!
                (str "#!/usr/bin/env bash\n"
                     "rm -rf out/scenarios/x\n"
                     "# BEGIN ed-tuesday commands (verbatim from demos/scenarios/ed-tuesday/README.md)\n"
                     "expect 0 bin/ehrt corpus generate sim --seed 1 --patients 5 \\\n"
                     "  --out-dir out/scenarios/x\n"
                     "expect 0 diff a/events.edn b/events.edn\n"
                     "expect_eval 0 'tail -c 45 out/x.hl7 | cat -A'\n"
                     "# END ed-tuesday commands\n"
                     "echo done\n"))]
    (is (= ["bin/ehrt corpus generate sim --seed 1 --patients 5 \\"
            "  --out-dir out/scenarios/x"
            "diff a/events.edn b/events.edn"
            "tail -c 45 out/x.hl7 | cat -A"]
           (def/script-command-lines script)))))

;; ---- seeded divergence: the check must be able to fail before it's
;; trusted to pass ----

(deftest check-catches-an-altered-line-test
  (let [readme (temp-file! (str "```bash\nbin/ehrt help\nbin/ehrt corpus generate\n```\n"))
        script (temp-file!
                (str "# BEGIN ed-tuesday commands (verbatim from demos/scenarios/ed-tuesday/README.md)\n"
                     "expect 0 bin/ehrt help\n"
                     "expect 0 bin/ehrt corpus generate --typo\n"
                     "# END ed-tuesday commands\n"))
        {:keys [ok? divergence]} (def/check {:readme-path readme :script-path script})]
    (is (not ok?))
    (is (= 1 (:index divergence)))
    (is (= "bin/ehrt corpus generate" (:readme divergence)))
    (is (= "bin/ehrt corpus generate --typo" (:script divergence)))))

;; ---- ADR-0130: marker-open/marker-close widened to explicit params,
;; so a SECOND demo-exerciser script (bin/demo-exerciser-busy-tuesday)
;; can supply its own, honestly-named markers instead of sharing
;; ed-tuesday's own literal marker text. Every test above stays
;; unmodified and green -- these are additions, not replacements. ----

(deftest script-command-lines-honors-a-non-ed-tuesday-marker-pair-test
  (let [script (temp-file!
                (str "#!/usr/bin/env bash\n"
                     "# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)\n"
                     "expect 0 bin/ehrt corpus generate sim --seed 1 --patients 5 \\\n"
                     "  --out-dir out/scenarios/busy-tuesday\n"
                     "expect 0 bin/ehrt play out/scenarios/busy-tuesday --board 60\n"
                     "# END busy-tuesday commands\n"
                     "echo done\n"))]
    (is (= ["bin/ehrt corpus generate sim --seed 1 --patients 5 \\"
            "  --out-dir out/scenarios/busy-tuesday"
            "bin/ehrt play out/scenarios/busy-tuesday --board 60"]
           (def/script-command-lines
             script
             "# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)"
             "# END busy-tuesday commands")))))

(deftest script-command-lines-default-arity-still-only-matches-ed-tuesday-markers-test
  (let [script (temp-file!
                (str "# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)\n"
                     "expect 0 bin/ehrt help\n"
                     "# END busy-tuesday commands\n"))]
    (is (nil? (def/script-command-lines script))
        "the 1-arity call still searches for ed-tuesday's own literal markers -- a script using a different marker pair correctly reports absent, not a false match")))

(deftest check-honors-explicit-marker-open-and-close-test
  (let [readme (temp-file! (str "```bash\nbin/ehrt help\n```\n"))
        script (temp-file!
                (str "# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)\n"
                     "expect 0 bin/ehrt help\n"
                     "# END busy-tuesday commands\n"))
        {:keys [ok? readme-count script-count]}
        (def/check {:readme-path readme :script-path script
                     :marker-open "# BEGIN busy-tuesday commands (verbatim from demos/scenarios/busy-tuesday/README.md)"
                     :marker-close "# END busy-tuesday commands"})]
    (is ok?)
    (is (= 1 readme-count script-count))))
