(ns ehrt.docs-tooling.make-graph
  "Shared Makefile/workflow parsers for the docs-tooling gates that make
  claims about the committed BUILD GRAPH rather than about a rendered
  artifact.

  Three of these existed as byte-identical private copies in
  `sim-theory-head-hop-test` (ADR-0152) and `traces-fresh-test`
  (ADR-0149) -- each session that closed the ungated-artifact class for
  one more artifact copied the previous session's helper. ADR-0155
  closes the class itself (register L3-1), which needs the same parsers
  from a third namespace; three copies is where a duplicated parser
  starts drifting, so they move here and both original namespaces now
  require this one.

  Everything here reads the committed TEXT of `Makefile` and
  `.github/workflows/test.yml`. That is deliberate and is the same
  choice ADR-0149 recorded: the claim is about what the tracked build
  graph SAYS, not about what a `make -np` on one developer's machine
  resolves it to -- a gate that shelled out to `make` would pass or fail
  on local state CI never sees."
  (:require [clojure.string :as str]))

(def makefile "Makefile")
(def workflow ".github/workflows/test.yml")

(defn target-prerequisites
  "The prerequisite list of `target` in `makefile-text`, as a vector of
  words -- nil if the target has no rule."
  [makefile-text target]
  (when-let [[_ prereqs] (re-find (re-pattern (str "(?m)^" target ":(.*)$")) makefile-text)]
    (vec (remove str/blank? (str/split (str/trim prereqs) #"\s+")))))

(defn target-recipe
  "The recipe lines of `target` in `makefile-text` -- every TAB-indented
  line following its rule line, as a vector, in order."
  [makefile-text target]
  (when-let [after (second (str/split makefile-text (re-pattern (str "(?m)^" target ":.*$")) 2))]
    (vec (take-while #(str/starts-with? % "\t") (rest (str/split-lines after))))))

(defn transitive-prerequisites
  "Every target reachable from `target` through prerequisite edges,
  `target` itself excluded. Cycle-safe."
  [makefile-text target]
  (loop [frontier (vec (target-prerequisites makefile-text target))
         seen #{}]
    (if-let [t (first frontier)]
      (if (seen t)
        (recur (rest frontier) seen)
        (recur (into (vec (rest frontier)) (target-prerequisites makefile-text t))
               (conj seen t)))
      seen)))

(defn freshness-diff-paths
  "The paths `.github/workflows/test.yml`'s generated-doc freshness step
  hands `git diff --exit-code`, read out of the committed workflow: every
  backslash-continued line after the command, to the first line that does
  not continue."
  [workflow-text]
  (when-let [after (second (str/split workflow-text #"git diff --exit-code \\\R" 2))]
    (loop [lines (str/split-lines after) out []]
      (if-let [line (first lines)]
        (let [trimmed (str/trim line)
              continues? (str/ends-with? trimmed "\\")
              path (str/replace trimmed #"\s*\\$" "")]
          (if (str/blank? path)
            out
            (let [out (conj out path)]
              (if continues? (recur (rest lines) out) out))))
        out))))
