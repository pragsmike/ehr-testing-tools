(ns ehrt.cli.cli-parse-guard-lint-test
  "Cluster B, D4-5/D4-6/D4-7/D8-3 (repo review 2,
  `.agents/plans/2026-08-09-repo-review-findings.md`; landed
  `notes/adr/0096-cluster-b-parse-guards.md`): four CLI reads --
  `read-base-data`'s `:fhir` branch, `gate-command`'s `--baseline`
  read, `check-command`'s `--assertions` read, and `sniff-path-
  format` (the shared sniff helper `sniff-gate-command`/`show-
  command`/`show-file` all delegate to) -- used to `slurp`/
  `edn/read-string`/`json/read-str` an operator-supplied path with no
  guard: a malformed or unreadable file crashed the whole command
  with a raw, uncaught stack trace instead of a categorized
  `result/error`. This is the recurrence gate: a future CLI command
  that reintroduces the bare idiom fails here instead of waiting for
  another review to find it by hand -- the same discipline
  `ehrt.docs-tooling.io-vocabulary-lint-test` already applies to the
  `.listFiles`/`.list`/`.renameTo` class (D4-1/D3-4/D8-2/D8-3).

  Function-granular, not whole-file (the sibling's own family, one
  level finer): each top-level `defn`/`defn-` form in
  `bases/cli/src/ehrt/cli/core.clj` is parsed with the Clojure
  reader (never a regex over the raw file text -- the same
  `ehrt.docs-tooling.project-classpath-test`/`sim-emit-hl7-
  dependency-test` discipline, so a docstring or comment MENTIONING
  `slurp` can never be a false positive: it is never even part of the
  read s-expression tree) and walked for a `(edn/read-string ...)`,
  `(json/read-str ...)`, or bare `(slurp ...)` call with no enclosing
  `(try ...)` ancestor WITHIN THAT SAME top-level form -- a `try` two
  functions away guards nothing here, matching the charter's own
  words (\"inside a top-level defn with no enclosing try in that same
  form\").

  Allowlist is BY FUNCTION NAME, disclosed rather than silent
  (`ehrt.sim.run`'s own grandfather clause in the sibling lint is the
  precedent for the shape, not the reason): `play-events-from-file`
  and `play-events-from-dir` (`ehrt play`'s own file-reading helpers)
  each carry the identical bare-`slurp`-on-an-operator-path shape
  this gate exists to catch, discovered live while building this very
  gate -- but `ehrt play` was never named in ADR-0092's D4-5/D4-6/
  D4-7/D8-3 register rows, nor in this session's own charter (ADR-
  0096's Cluster B), and fixing it would touch a fifth command this
  session was never authorized to touch (build-session's own
  STOP-AND-REPORT discipline for a live, unnamed site). Recorded here
  as deferred, disclosed cargo for a future session's own register
  row -- exempted BY NAME, not by path, so a future session that
  fixes `ehrt play`'s own reads must also delete its two entries here
  as part of that fix, the same closing-the-loop shape the sibling's
  own `ehrt.sim.run` entry expects of whoever migrates it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private forbidden-call-heads
  "The three read idioms D4-5/D4-6/D4-7/D8-3 found bare: `edn/read-
  string`/`json/read-str` exactly as this file's own `:require`
  aliases them (`[clojure.edn :as edn]`, `[clojure.data.json :as
  json]`), plus bare `slurp` (no alias needed, clojure.core)."
  '#{edn/read-string json/read-str slurp})

(def ^:private allowlisted-fn-names
  "Disclosed, deferred cargo -- see this namespace's own docstring.
  `ehrt play`'s own file-reading helpers, not this session's own
  charter (D4-5/D4-6/D4-7/D8-3)."
  #{"play-events-from-file" "play-events-from-dir"})

(defn- clj-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
       (map #(.getPath ^java.io.File %))))

(defn- scan-sources []
  (clj-files (io/file "bases/cli/src")))

(defn- read-all-forms
  "Every top-level form in the file at `path`, via the Clojure reader
  (`*read-eval*` false, matching this repo's other reader-based
  gates) -- never a regex over raw text."
  [path]
  (with-open [rdr (java.io.PushbackReader. (io/reader path))]
    (binding [*read-eval* false]
      (loop [forms []]
        (let [f (read {:eof ::eof} rdr)]
          (if (= f ::eof)
            forms
            (recur (conj forms f))))))))

(defn- defn-form? [form]
  (and (seq? form) (contains? #{'defn 'defn-} (first form))))

(defn- defn-name [form]
  (str (second form)))

(defn- unguarded-forbidden-call?
  "Walks `node` (a parsed s-expression, or any nested vector/map/set/
  seq within it) for a forbidden-call-heads invocation that is not
  inside a `(try ...)` ancestor already seen on this same walk --
  `in-try?` tracks that ancestry across the recursion, reset to true
  the instant a `try` special form is entered, never cleared again
  within that branch (a `try` anywhere upstream guards everything
  downstream of it, exactly matching the actual runtime exception-
  propagation shape this gate cares about)."
  [node in-try?]
  (cond
    (and (seq? node) (seq node))
    (let [head (first node)]
      (if (= 'try head)
        (some #(unguarded-forbidden-call? % true) (rest node))
        (or (and (not in-try?) (contains? forbidden-call-heads head))
            (some #(unguarded-forbidden-call? % in-try?) node))))

    (or (vector? node) (set? node))
    (some #(unguarded-forbidden-call? % in-try?) node)

    (map? node)
    (some #(unguarded-forbidden-call? % in-try?) (mapcat identity node))

    :else false))

(defn- violating-defn-names
  "Every `defn`/`defn-` top-level form's own name in `path` that
  contains an unguarded forbidden-call-heads invocation, excluding
  allowlisted-fn-names (disclosed above)."
  [path]
  (->> (read-all-forms path)
       (filter defn-form?)
       (remove #(contains? allowlisted-fn-names (defn-name %)))
       (filter #(unguarded-forbidden-call? % false))
       (map defn-name)))

(deftest no-unguarded-cli-parse-read-outside-a-try-test
  (doseq [path (scan-sources)]
    (let [violators (violating-defn-names path)]
      (is (empty? violators)
          (str path " has a top-level defn calling edn/read-string, "
               "json/read-str, or slurp with no enclosing try in the "
               "same form -- wrap the read (ADR-0096, D4-5/D4-6/D4-7/"
               "D8-3): " (pr-str violators))))))

(deftest forbidden-pattern-detection-is-actually-caught-test
  (testing "a bare slurp with no enclosing try trips"
    (is (unguarded-forbidden-call?
         (read-string "(defn- f [file] (slurp file))") false)))
  (testing "a bare edn/read-string+slurp with no enclosing try trips"
    (is (unguarded-forbidden-call?
         (read-string "(defn f [p] (edn/read-string (slurp p)))") false)))
  (testing "a bare json/read-str+slurp with no enclosing try trips"
    (is (unguarded-forbidden-call?
         (read-string "(defn f [p] (json/read-str (slurp p)))") false)))
  (testing "the same read wrapped in try/catch in the SAME form does not trip"
    (is (not (unguarded-forbidden-call?
              (read-string "(defn f [p] (try (result/ok (slurp p)) (catch Exception e (result/error :x {}))))")
              false))))
  (testing "a try guarding one branch does not guard a sibling bare read in the same form"
    (is (unguarded-forbidden-call?
         (read-string "(defn f [p q] (let [a (try (slurp p) (catch Exception e nil))] (edn/read-string (slurp q))))")
         false)))
  (testing "a docstring or comment MENTIONING slurp is never a false positive -- it is never part of the read s-expression tree at all"
    (is (not (unguarded-forbidden-call?
              (read-string "(defn f \"reads via slurp internally\" [p] (str p))") false))))
  (testing "a try in an unrelated, earlier top-level form never guards this one -- each defn form is walked independently"
    (is (unguarded-forbidden-call?
         (read-string "(defn f [p] (slurp p))") false))))

;; ---- Minimal-reproduction witness pair (cluster A's own method,
;; `ehrt.docs-tooling.project-classpath-test`'s
;; `violation-predicate-reproduces-the-2088763-incident-test`): each
;; charter site's own PRE-FIX shape, reduced to its structural
;; essence -- not the real docstrings, which the predicate never
;; reads anyway -- reproduced here as a permanent regression proof
;; that survives independent of git history. The session's own live
;; run of this same predicate against the real pre-fix and post-fix
;; trees (git show b8fac5a vs. this commit) is pasted into
;; notes/adr/0096-cluster-b-parse-guards.md; this test is what keeps
;; that proof from ever silently rotting stale. ----

(def ^:private pre-fix-read-base-data
  "(defn- read-base-data [format file]
     (case format
       :fhir (json/read-str (slurp file))
       :v2 (slurp file)))")

(def ^:private post-fix-read-base-data
  "(defn- read-base-data [format file]
     (try
       (result/ok (case format
                    :fhir (json/read-str (slurp file))
                    :v2 (slurp file)))
       (catch Exception e
         (result/error :base-data-unreadable {:path (str file) :message (.getMessage e)}))))")

(def ^:private pre-fix-gate-baseline
  "(defn gate-command [gate-file-fn gate-dir-fn gate-label]
     (fn [{:keys [baseline]}]
       (let [baseline-report (edn/read-string (slurp baseline))]
         (report/baseline-relative-report baseline-report))))")

(def ^:private post-fix-gate-baseline
  "(defn gate-command [gate-file-fn gate-dir-fn gate-label]
     (fn [{:keys [baseline]}]
       (let [baseline-result (try
                               (result/ok (edn/read-string (slurp baseline)))
                               (catch Exception e
                                 (result/error :baseline-unreadable {:path baseline :message (.getMessage e)})))]
         (if-not (result/ok? baseline-result)
           baseline-result
           (report/baseline-relative-report (:payload baseline-result))))))")

(def ^:private pre-fix-check-assertions
  "(defn check-command [{:keys [path assertions]}]
     (let [assertions-data (when assertions (edn/read-string (slurp assertions)))]
       (check/check-corpus {:candidate-dir path :assertions assertions-data})))")

(def ^:private post-fix-check-assertions
  "(defn check-command [{:keys [path assertions]}]
     (let [assertions-result (if assertions
                               (try
                                 (result/ok (edn/read-string (slurp assertions)))
                                 (catch Exception e
                                   (result/error :assertions-unreadable {:path assertions :message (.getMessage e)})))
                               (result/ok nil))]
       (if-not (result/ok? assertions-result)
         assertions-result
         (check/check-corpus {:candidate-dir path :assertions (:payload assertions-result)}))))")

(def ^:private pre-fix-sniff-path-format
  "(defn- sniff-path-format [f]
     (get sniffed-format->gate-label (intake/sniff-format (slurp f))))")

(def ^:private post-fix-sniff-path-format
  "(defn- sniff-path-format [f]
     (try
       (result/ok (get sniffed-format->gate-label (intake/sniff-format (slurp f))))
       (catch Exception e
         (result/error :path-unreadable {:path (.getPath f) :message (.getMessage e)}))))")

(deftest violation-predicate-reproduces-the-four-charter-sites-test
  (testing "D4-5: read-base-data's :fhir branch, pre-fix trips, post-fix does not"
    (is (unguarded-forbidden-call? (read-string pre-fix-read-base-data) false))
    (is (not (unguarded-forbidden-call? (read-string post-fix-read-base-data) false))))
  (testing "D4-6: gate-command's --baseline read, pre-fix trips, post-fix does not"
    (is (unguarded-forbidden-call? (read-string pre-fix-gate-baseline) false))
    (is (not (unguarded-forbidden-call? (read-string post-fix-gate-baseline) false))))
  (testing "D4-7: check-command's --assertions read, pre-fix trips, post-fix does not"
    (is (unguarded-forbidden-call? (read-string pre-fix-check-assertions) false))
    (is (not (unguarded-forbidden-call? (read-string post-fix-check-assertions) false))))
  (testing "D8-3: sniff-path-format (bare gate + show's shared helper), pre-fix trips, post-fix does not"
    (is (unguarded-forbidden-call? (read-string pre-fix-sniff-path-format) false))
    (is (not (unguarded-forbidden-call? (read-string post-fix-sniff-path-format) false)))))
