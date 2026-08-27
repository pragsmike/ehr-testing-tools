(ns ehrt.docs-tooling.ground-truth-bracket-test
  "`bin/ground-truth-bracket`, gated (arc 4 sweep 1, 2026-08-27,
  `notes/adr/0175-arc-4-emission-add-ons.md` ruling E1).

  WHY THE INSTRUMENT EXISTS. `ehrt.oracle.digest/-main` writes the
  `{:ground-truth :hl7}` pair as ONE file per root, so
  `bin/regression-oracle` sha256s both halves together. Arc 4 changes
  emission and nothing else, which makes every engine-layer root DIFFER
  -- the correct outcome, and one that leaves the oracle unable to say
  anything at all about the half arc 4 promises did not move. ADR-0175
  section 4 asserted the oracle *does* report per-half; it never could.
  `bin/ground-truth-bracket` is what that sentence names.

  WHY A GATE AND NOT JUST A SESSION-TIME RUN. The instrument's whole
  value is a claim it makes about a DIFFERENT commit pair every time, so
  nothing about a past run constrains the next one. What must hold
  forever is the shape of the claim, and there are exactly three
  properties worth keeping:

    (a) THE `:hl7` HALF IS EXCLUDED. Two roots whose ground truth is
        equal and whose messages differ must digest the same. This is
        the entire mechanism; if it regresses, the bracket silently
        becomes a second, slower regression-oracle that reports DIFFERS
        on every arc-4 sweep and proves nothing.

    (b) A ROOT WITH NO `:ground-truth` IS NAMED, NOT DROPPED. The three
        interpreter-layer batch roots write a vector of walks rather
        than a pair. Dropping them silently would make the coverage
        line a fiction -- `rulings.md#R-population-closure`.

    (c) THE VERDICT REFUSES A VACUOUS POPULATION. A bracket over zero
        roots reports IDENTICAL and proves nothing, which is
        `rulings.md#R-empty-population-is-red` exactly, and ADR-0175
        section 2(b) already names it as the trap a later arc-4 sweep
        is most likely to fall into (no oracle root places an order).

  HOW IT TESTS THEM WITHOUT RUNNING THE ORACLE. (a) and (b) go through
  the real `bin/ground-truth-bracket-src/manifest.clj`, on a scratch
  directory of hand-written pairs -- the actual program, not a
  reimplementation of it. (c) goes through the real `gt_bracket_verdict`
  from `bin/oracle-lib.sh`, called with fabricated manifests; that
  function lives in the library rather than inline in the script
  precisely so it can be called this way. Neither needs a worktree, so
  this namespace costs the suite one JVM start and no oracle run."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private manifest-script "bin/ground-truth-bracket-src/manifest.clj")

(def ^:private oracle-lib "bin/oracle-lib.sh")

(defn- scratch-dir
  "A throwaway directory holding `files` (name -> EDN text)."
  [files]
  (let [d (java.io.File/createTempFile "gt-bracket" "")]
    (.delete d)
    (.mkdirs d)
    (.deleteOnExit d)
    (doseq [[n text] files]
      (let [f (io/file d n)]
        (spit f text)
        (.deleteOnExit f)))
    d))

(defn- manifest
  "The real manifest generator's stdout over `dir`, as a vector of
  `[digest-or-marker file]` pairs."
  [dir]
  (let [{:keys [exit out err]}
        (shell/sh "clojure" "-Sdeps" "{:deps {org.clojure/clojure {:mvn/version \"1.12.5\"}}}"
                  "-M" (.getAbsolutePath (io/file manifest-script)) (.getPath dir)
                  :dir (.getPath dir))]
    (when-not (zero? exit)
      (throw (ex-info (str "manifest.clj exited " exit ": " err) {:exit exit :err err})))
    (->> (str/split-lines (str/trim out))
         (remove str/blank?)
         (mapv #(vec (str/split % #"\s+" 2)))
         (mapv (fn [[a b]] [a (str/trim b)])))))

(defn- verdict
  "The real `gt_bracket_verdict` from the shared library, extracted and
  run over two fabricated manifests. Extracted rather than
  reimplemented, for the same reason
  `ehrt.docs-tooling.oracle-coverage-test` extracts `digest_body_of`: a
  reimplementation would test this test's idea of the rule."
  [baseline-lines target-lines]
  (let [lib (slurp oracle-lib)
        start (str/index-of lib "gt_bracket_verdict() {")
        end (str/index-of lib "\n}" start)
        fn-text (subs lib start (+ end 2))
        d (scratch-dir {"base.sha256" (str (str/join "\n" baseline-lines) "\n")
                        "target.sha256" (str (str/join "\n" target-lines) "\n")})]
    (shell/sh "bash" "-c"
              (str "set -euo pipefail\n" fn-text
                   "\ngt_bracket_verdict \"$1\" \"$2\" base target no yes")
              "--"
              (.getPath (io/file d "base.sha256"))
              (.getPath (io/file d "target.sha256")))))

;; ---------------------------------------------------------------------
;; (a) the :hl7 half is excluded, and only it
;; ---------------------------------------------------------------------

(deftest the-manifest-digests-ground-truth-and-ignores-the-wire-test
  (let [d (scratch-dir
           {"same-gt-a.edn"  "{:ground-truth [{:kind :admission :t 1}] :hl7 [\"MSH|^~\\\\&|A\"]}"
            "same-gt-b.edn"  "{:ground-truth [{:kind :admission :t 1}] :hl7 [\"MSH|^~\\\\&|TOTALLY DIFFERENT\"]}"
            "other-gt.edn"   "{:ground-truth [{:kind :admission :t 2}] :hl7 [\"MSH|^~\\\\&|A\"]}"})
        rows (into {} (map (fn [[dig f]] [f dig]) (manifest d)))]
    (testing "sanity: every scratch root produced a line"
      (is (= #{"same-gt-a.edn" "same-gt-b.edn" "other-gt.edn"} (set (keys rows)))
          (str "the generator must emit exactly one line per .edn file. Got " (pr-str rows))))
    (testing "(a) two roots with equal ground truth and different messages digest the SAME"
      (is (= (rows "same-gt-a.edn") (rows "same-gt-b.edn"))
          (str "this is the whole mechanism: an emission-only change must not move a "
               "ground-truth digest. If these differ, the bracket has become a second "
               "regression-oracle and every arc-4 sweep's ground-truth claim is unprovable.")))
    (testing "a real ground-truth difference still moves the digest"
      (is (not= (rows "same-gt-a.edn") (rows "other-gt.edn"))
          (str "the converse half -- excluding :hl7 must not also flatten :ground-truth. "
               "Without this assertion a generator that hashed a constant would pass the "
               "assertion above.")))))

;; ---------------------------------------------------------------------
;; (b) a root with no :ground-truth is named, not dropped
;; ---------------------------------------------------------------------

(deftest a-root-without-ground-truth-is-named-not-dropped-test
  (let [d (scratch-dir
           {"pair.edn"  "{:ground-truth [{:kind :admission}] :hl7 [\"MSH|x\"]}"
            "batch.edn" "[{:walk 1} {:walk 2}]"})
        rows (manifest d)
        by-file (into {} (map (fn [[dig f]] [f dig]) rows))]
    (testing "(b) the batch-shaped root appears, marked, rather than vanishing"
      (is (= 2 (count rows))
          (str "both files must produce a line. `rulings.md#R-population-closure`: a "
               "population the bracket does not cover has to be visible in its own output, "
               "or the coverage count it prints is a fiction. Got " (pr-str rows)))
      (is (= "skipped-no-ground-truth" (by-file "batch.edn"))
          "the interpreter-layer shape (a vector of walks) is skipped BY NAME")
      (is (re-matches #"[0-9a-f]{64}" (by-file "pair.edn"))
          "the pair-shaped root is digested"))))

;; ---------------------------------------------------------------------
;; (c) the verdict's own guards
;; ---------------------------------------------------------------------

(deftest the-verdict-refuses-an-empty-population-test
  (let [{:keys [exit out err]} (verdict ["skipped-no-ground-truth  a.edn"]
                                        ["skipped-no-ground-truth  a.edn"])]
    (testing "(c) all-skipped is red, not IDENTICAL"
      (is (= 1 exit)
          (str "two manifests that agree perfectly and cover NOTHING must not read as "
               "agreement. `rulings.md#R-empty-population-is-red`; ADR-0175 section 2(b) "
               "names this exact trap for the ladder sweep, whose subject no oracle root "
               "contains. out=" out " err=" err))
      (is (str/includes? err "EMPTY population")
          "and the reason has to be on stderr, not inferable from an exit code alone"))))

(deftest the-verdict-refuses-a-drifting-skip-set-test
  (let [{:keys [exit err]} (verdict ["deadbeef  x.edn" "skipped-no-ground-truth  a.edn"]
                                    ["deadbeef  x.edn" "deadbeef  a.edn"])]
    (testing "a root that gains :ground-truth on one side is a finding, not a diff"
      (is (= 1 exit) "the two sides cover different populations and cannot be compared")
      (is (str/includes? err "skip different roots")
          "and the message must say which side skipped what"))))

(deftest the-verdict-reports-identical-and-differs-test
  (testing "matching manifests over a non-empty population are IDENTICAL"
    (let [{:keys [exit out]} (verdict ["deadbeef  x.edn" "skipped-no-ground-truth  a.edn"]
                                      ["deadbeef  x.edn" "skipped-no-ground-truth  a.edn"])]
      (is (= 0 exit) out)
      (is (str/includes? out "IDENTICAL"))
      (is (str/includes? out "1 roots carry :ground-truth")
          "the coverage count is printed, so an IDENTICAL always says how wide it is")))
  (testing "a moved ground-truth digest is DIFFERS"
    (let [{:keys [exit err]} (verdict ["deadbeef  x.edn"] ["cafebabe  x.edn"])]
      (is (= 1 exit))
      (is (str/includes? err "DIFFERS")))))
