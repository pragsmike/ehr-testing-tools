(ns ehrt.integration.oracle-coverage-test
  "ADR-0156, register rows L1-1 / L1-2 -- the half of the oracle coverage
  gate that has to actually RUN the oracle.

  `ehrt.docs-tooling.oracle-coverage-test` gates the committed claim's
  shape, population, membership and location on every push. It cannot
  gate the claim against reality: only a real 35-root digest knows which
  event kinds and message types the oracle can witness. That run is 114
  seconds (ADR-0156 Step 0 b), so it lives here, in the scheduled lane.

  WHY IT MATTERS THAT THIS ONE EXISTS. A gate over a claim that never
  meets the thing it describes proves only that the claim agrees with
  itself -- ADR-0152's own lesson, in the dimension the same review
  scored. The committed set is inside `digest.clj`'s soundness body, so
  a session that changes coverage must pass `--declared-digest-change`
  to get a bracket at all; this test is what tells that session WHICH
  WAY coverage moved, instead of leaving it to notice.

  HOW IT RUNS THE DIGEST. Through the same synthetic classpath
  `bin/regression-oracle`'s own `run_one` builds -- `clojure -Sdeps`
  with every `:local/root` pointed at this checkout, and the
  `:oracle-run` alias for `sim-trajectory`'s test resources. The oracle
  brick belongs to no testable project (`poly info`: `---` under
  conformance / ehrt-cli / integration, `s--` under dev; poly's own help
  says brick tests run from every project EXCEPT development), so
  requiring `ehrt.oracle.interface` here would not compile. Shelling out
  the way the harness does is not a workaround for that -- it is the
  mechanism under audit, which is the form this repo's own review rubric
  asks evidence to take."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private repo-root
  "Tests run from the workspace root (`poly test`'s own cwd)."
  ".")

(def ^:private digest-path
  "components/oracle/src/ehrt/oracle/digest.clj")

(defn- deps-string
  "`bin/regression-oracle`'s `run_one` deps block, with the worktree root
  replaced by this checkout. Kept in the same order and shape as the
  script's own heredoc so a drift between them is visible on sight."
  [root]
  (str "{\n"
       " :deps {org.clojure/clojure {:mvn/version \"1.12.5\"}\n"
       "        poly/kernel {:local/root \"" root "/components/kernel\"}\n"
       "        poly/oracle {:local/root \"" root "/components/oracle\"}\n"
       "        poly/sim {:local/root \"" root "/components/sim\"}\n"
       "        poly/sim-engine {:local/root \"" root "/components/sim-engine\"}\n"
       "        poly/sim-model {:local/root \"" root "/components/sim-model\"}\n"
       "        poly/sim-trajectory {:local/root \"" root "/components/sim-trajectory\"}\n"
       "        poly/sim-emit-hl7 {:local/root \"" root "/components/sim-emit-hl7\"}}\n"
       " :aliases {:oracle-run {:extra-paths [\"" root "/components/sim-trajectory/test\"]}}}"))

(defn- run-digest!
  "One fresh 35-root digest into `out`. Returns the process result."
  [out]
  (let [root (.getCanonicalPath (io/file repo-root))]
    (shell/sh "clojure" "-Sdeps" (deps-string root)
              "-M:oracle-run" "-m" "ehrt.oracle.interface" (.getPath out)
              :dir root)))

(defn- engine-roots
  "The 32 engine-layer roots, keyed by name. The other three are
  interpreter batches whose facts are a DIFFERENT vocabulary -- see the
  nested-`:event` hazard in `ehrt.sim-engine.event-schema`, which is
  exactly the mistake a naive tree-walk for `:event` makes."
  [out]
  (into (sorted-map)
        (for [f (sort-by #(.getName ^java.io.File %) (.listFiles ^java.io.File out))
              :when (str/ends-with? (.getName ^java.io.File f) ".edn")
              :let [v (edn/read-string (slurp f))]
              :when (and (map? v) (contains? v :ground-truth))]
          [(str/replace (.getName ^java.io.File f) #"\.edn$" "") v])))

(defn- witnessed-event-kinds [roots]
  (into (sorted-set) (mapcat (fn [[_ v]] (map :event (:ground-truth v))) roots)))

(defn- witnessed-message-types [roots]
  (into (sorted-set)
        (mapcat (fn [[_ v]]
                  (for [m (:hl7 v)
                        :let [fields (str/split (first (str/split-lines (str m))) #"\|")]
                        :when (> (count fields) 8)]
                    (nth fields 8)))
                roots)))

(defn- committed
  "The value of the top-level `(def <name> #{...})` in `digest.clj`, read
  as EDN. Matches BOTH the bare and the `^:private` form.

  WHY THE TWO FORMS. This file's first version searched only `\"(def \"`
  plus the name, while `digest.clj` writes `(def ^:private
  witnessed-event-kinds`, so `str/index-of` returned nil and `subs` threw
  NPE before a single coverage assertion ran. The gate had therefore
  never once been green: nightly `Integration` run 32344505291 is its
  first execution and its standing red witness (ADR-0160, review-4 F-5).

  CANONICAL TWIN: `ehrt.docs-tooling.oracle-coverage-test`'s own
  `def-form`, which ADR-0156 had already refined to exactly this
  two-prefix `some` and which this half did not inherit. The two copies
  are not shared, because `projects/integration` deliberately does not
  compose `docs-tooling` (its own `deps.edn` records that twice, AR-3),
  and composing it to share seven lines would pull docs-tooling's whole
  test tree into the nightly lane. `roadmap.md#oracle-coverage-extractor-dedup`
  holds the dedup.

  A miss returns nil rather than throwing, and every caller asserts on
  what comes back: a gate that cannot find its subject has to read as a
  failed claim, not as an uncaught NPE that looks like a broken test."
  [name]
  (let [source (slurp digest-path)]
    (when-let [i (some #(str/index-of source %)
                       [(str "(def " name) (str "(def ^:private " name)])]
      (let [after (subs source i)
            open (str/index-of after "#{")]
        (when open
          (edn/read-string (subs after open (inc (str/index-of after "}" open)))))))))

(deftest a-fresh-digest-witnesses-exactly-the-committed-coverage-claim-test
  (let [out (io/file (System/getProperty "java.io.tmpdir")
                     (str "ehrt-oracle-coverage-" (System/currentTimeMillis)))]
    (.mkdirs out)
    (try
      (let [{:keys [exit err]} (run-digest! out)
            edns (filter #(str/ends-with? (.getName ^java.io.File %) ".edn") (.listFiles out))
            roots (engine-roots out)]
        (testing "the digest ran at all -- a failed run must never read as agreement"
          (is (zero? exit) (str "the digest process must exit 0. stderr:\n" err))
          (is (= 35 (count edns))
              (str "35 roots today. A root added or removed lands here first. Found "
                   (count edns) "."))
          (is (= 32 (count roots))
              (str "32 engine-layer roots produce `{:ground-truth :hl7}`; the other three are "
                   "interpreter batches. Found " (count roots) ".")))
        (let [kinds (witnessed-event-kinds roots)
              types (witnessed-message-types roots)]
          (testing "rulings.md#R-empty-population-is-red"
            (is (seq kinds) "a digest witnessing no event kinds at all means the run produced
                nothing -- an equality assertion against an empty set proves nothing")
            (is (seq types) "likewise for emitted messages"))
          (testing "the committed claim equals what the oracle can actually witness"
            (is (= (committed "witnessed-event-kinds") kinds)
                (str "coverage moved and the claim did not. Fresh digest witnesses "
                     (pr-str kinds) "; `" digest-path "` commits "
                     (pr-str (committed "witnessed-event-kinds"))
                     ". Update the COVERAGE block in the same commit -- it is inside the "
                     "soundness body, so the bracket will ask for --declared-digest-change "
                     "anyway (R4-Q6 ii a)."))
            (is (= (committed "witnessed-message-types") types)
                (str "fresh digest emits " (pr-str types) "; committed "
                     (pr-str (committed "witnessed-message-types")))))
          (testing "the capacity witness is one root deep, and the claim says so"
            (is (= ["death-fixture"]
                   (vec (for [[n v] roots
                              :when (some #(= :transfer (:event %)) (:ground-truth v))] n)))
                "`:transfer` -- and with it ADT^A02, :bed-ready and ladder rung 3 -- is
                witnessed by `death-fixture` alone. If this ever passes with a different
                root list, the coverage paragraph in digest.clj is stale."))))
      (finally
        (doseq [f (.listFiles out)] (.delete ^java.io.File f))
        (.delete out)))))
