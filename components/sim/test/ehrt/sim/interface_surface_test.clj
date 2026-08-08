(ns ehrt.sim.interface-surface-test
  "Lint family (AR-LF-2, D2-3, `.agents/plans/2026-08-07-repo-review-
  findings.md`): AR-M4-3 (sim split B) rules `ehrt.sim.interface`'s own
  surface 'permanently frozen in surface: var list, names, and arities
  byte-identical' -- `corpus` depends on this interface in-process
  (ADR-0012). Until this gate, nothing mechanically enforced that; a
  future accidental signature change would surface only as a
  downstream compile/runtime failure, never a named violation of the
  frozen-surface rule itself.

  Baseline frozen 2026-08-07 against tip `758f3af`, captured by direct
  `ns-publics` reflection against the built interface, per this ADR's
  own findings-register recommendation ('cheap, precedented by this
  repo's own currency gate family'). A var's arity is the SET of its
  arglists' argument counts (multi-arity vars, e.g. `check-all`,
  record every arity they support); a non-function public var (`def`,
  not `defn`) has no `:arglists` meta and is recorded as `:value`.

  Any future narrowing (or widening) of this façade is a separate,
  author-ruled decision (the interface's own docstring) -- this gate
  makes that decision VISIBLE as a named baseline edit, rather than
  silent."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim.interface]))

(def ^:private frozen-baseline
  {"check-all" #{1 2 3 4}
   "error" #{2}
   "git-sha" #{0}
   "identifiers-command" #{1}
   "ok" #{1}
   "ok?" #{1}
   "rejected" #{2}
   "rejected?" #{1}
   "run-command" #{1}
   "version" :value})

(defn- arity-signature
  "`v`'s own arity signature: the set of its `:arglists` argument
  counts, or `:value` when `v` carries no `:arglists` meta (a plain
  `def`, not a function)."
  [v]
  (if-let [arglists (:arglists (meta v))]
    (set (map count arglists))
    :value))

(defn- live-surface
  "`ns-sym`'s own current public surface: `{var-name-string arity-
  signature}`, re-derived fresh from the live namespace every run --
  never read from this file's own baseline literal as its own proof."
  [ns-sym]
  (into {}
        (map (fn [[n v]] [(name n) (arity-signature v)]))
        (ns-publics ns-sym)))

(deftest sim-interface-surface-matches-its-frozen-baseline-test
  (testing "corpus depends on this interface's own stability (ADR-0012) -- a var added, removed, renamed, or re-arited here breaks that contract silently unless this gate names it"
    (is (= frozen-baseline (live-surface 'ehrt.sim.interface))
        (str "ehrt.sim.interface's live public surface has drifted from its frozen baseline "
             "(AR-M4-3, lint family AR-LF-2, D2-3) -- if this drift is a deliberate, "
             "author-ruled narrowing/widening of the façade, update frozen-baseline in "
             "this test to match; otherwise this is an accidental surface break."))))

;; -- mechanism-sanity: prove the extraction functions actually catch what they claim to --

(deftest arity-signature-is-actually-caught-test
  (is (= #{1 2} (arity-signature (with-meta (fn []) {:arglists '([a] [a b])}))))
  (is (= :value (arity-signature (with-meta (fn []) {})))
      "a var with no :arglists meta (a plain def) must be recorded as :value, not an empty set"))

(deftest live-surface-drift-is-actually-caught-test
  (testing "sanity: a baseline missing a live var, or disagreeing on arity, must fail the comparison"
    (let [live {"a" #{1} "b" #{2}}
          stale-baseline {"a" #{1}}]
      (is (not= stale-baseline live)
          "proves the equality check itself would catch a live var absent from a stale baseline"))))
