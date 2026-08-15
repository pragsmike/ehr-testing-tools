(ns ehrt.docs-tooling.mermaid-render-test
  "ADR-0135: terminal-output result nodes in the string-diagram
  renderer. Every single-equation palgebra diagram in the 21 generated
  use-case pages used to dead-end at the operation box -- the signature
  line promised `datum × profile-artifact → pass + rejected +
  no-verdict [Gate]` but nothing wired out, because
  resource_equations_to_mermaid.py emitted output wires only for
  {discard:} sinks and {feedback:} edges. This is the script-level red
  for the fix: a codomain that is neither discarded, consumed
  downstream, nor fed back now gets its own result node and wire.

  RUNTIME DEPENDENCY: shells out to `python3` (the renderer is a Python
  script; the Clojure side of palgebra's emitter is Phase 4 debt,
  docs/palgebra-design.md §I.6). `make pipeline` and `make use-cases`
  already assume python3 and CI pins it explicitly with
  actions/setup-python (.github/workflows/test.yml), so this test adds
  no dependency the regeneration path did not already carry. This is
  the only test in the suite that runs the renderer for real -- the
  hermetic rendering tests in pipeline_test.clj and usecases_test.clj
  deliberately work over already-rendered text instead."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def ^:private script "components/palgebra/tools/resource_equations_to_mermaid.py")

(defn- render!
  "Write `equations` to a temp file, run the renderer over it, and
  return {:exit :out :err :mermaid}."
  [equations]
  (let [in (java.io.File/createTempFile "mermaid-render-in" ".txt")
        out (java.io.File/createTempFile "mermaid-render-out" ".mermaid")]
    (spit in equations)
    (let [{:keys [exit err]} (shell/sh "python3" script
                                       (.getAbsolutePath in)
                                       "-o" (.getAbsolutePath out))]
      {:exit exit
       :err err
       :mermaid (slurp out)})))

(deftest terminal-outputs-render-as-result-nodes-test
  (let [{:keys [exit err mermaid]} (render! "datum × ctx → pass + rejected  [Op]  {catalytic: ctx}\n")]
    (is (zero? exit) (str "renderer exited non-zero: " err))
    (testing "one result node per coproduct summand, _out-suffixed"
      (is (str/includes? mermaid "pass_out([\"pass\"])"))
      (is (str/includes? mermaid "rejected_out([\"rejected\"])")))
    (testing "each summand is wired out of the operation box"
      (is (str/includes? mermaid "Op -- \"pass\" --> pass_out"))
      (is (str/includes? mermaid "Op -- \"rejected\" --> rejected_out")))
    (testing "the catalytic input wire is still dashed (no regression)"
      (is (str/includes? mermaid "ctx -. ctx .-> Op")))))
