(ns ehrt.docs-tooling.cli-tombstone-test
  "AR-EP-1 (ux epilogue, `notes/adr/0065-ux-epilogue.md`): the retired
  `clojure -M:cli` shape now redirects instead of throwing
  FileNotFoundException (`clojure.main` treating a bare verb as an
  init-script path when the alias itself doesn't resolve) -- but
  nothing GATES that the redirect itself keeps existing. This test
  promotes that ruling to a standing gate: the root `deps.edn`'s `:cli`
  alias must exist with exactly the `:main-opts` that route to
  `ehrt.cli.retired`, so the tombstone is not silently removable by a
  future alias cleanup that doesn't know what it's for.

  Reads `deps.edn` as EDN via `clojure.edn/read-string`, same
  discipline `root-alias-completeness-test` already established for
  this file -- a text-based grep would be fragile against the file's
  own extensive comment provenance."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]))

(defn- deps-edn [] (edn/read-string (slurp "deps.edn")))

(deftest cli-alias-redirects-to-retired-test
  (let [cli-alias (get-in (deps-edn) [:aliases :cli])]
    (is (some? cli-alias)
        "root deps.edn must keep a :cli alias -- the tombstone, not a silent removal")
    (is (= ["-m" "ehrt.cli.retired"] (:main-opts cli-alias))
        (str ":cli's :main-opts must route to ehrt.cli.retired, got: " (:main-opts cli-alias)))
    (is (= #{"bases/cli/src"} (set (:extra-paths cli-alias)))
        (str ":cli's :extra-paths must reach ehrt.cli.retired, got: " (:extra-paths cli-alias)))))

;; -- mechanism-sanity: prove the extraction actually catches drift --

(deftest cli-alias-drift-is-actually-caught-test
  (let [wrong-main-opts (get-in {:aliases {:cli {:main-opts ["-m" "ehr-testing-sim.cli"]}}}
                                 [:aliases :cli])]
    (is (not= ["-m" "ehrt.cli.retired"] (:main-opts wrong-main-opts))
        "the pre-monorepo main-opts value must not be mistaken for the tombstone's own")))
