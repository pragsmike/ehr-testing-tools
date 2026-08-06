(ns ehrt.cli.help-wrap-test
  "The wrap gate (AR-U5-1/2, ADR-0063): the render functions word-wrap
  every description at `help/default-wrap-width` columns with a hanging
  indent aligned to each layout's own description column, so a long
  string degrades gracefully at a real terminal instead of running one
  unbroken line (the defect `help_test.clj`'s own suite never caught,
  since it only ever asserts substring membership, never line width).

  Two properties, both spec-derived so future strings are covered
  automatically:

  - AR-U5-2(a), the width test: no rendered line of `ehrt help` or any
    `ehrt help <group>` exceeds the width, UNLESS the line is a single
    token too long to break at all (AR-U5-1's own unbroken-token
    exception -- confirmed live in `cli-spec` today: both `--config`
    flags' shared `(:pathway/:pathways/...)` parenthetical is one
    78-char token no indent-plus-token combination here keeps under 80).
  - AR-U5-2(b), content preservation: wrapping only ever turns an
    existing inter-word space into a line break -- it never adds,
    drops, or reorders a character of actual content. Proven two ways:
    directly on `wrap-lines` (joining its output with single spaces
    reconstructs the input exactly, for every rendered-string-position
    value in `cli-spec`, the same walk `help-voice-test` already
    trusts), and at the whole-page level (an 80-column render and an
    effectively-unwrapped one, all whitespace runs normalized to a
    single space on both sides, are identical -- word content and order
    survive wrapping, which is exactly what AR-U5-1's byte-identical-
    content claim requires)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [ehrt.cli.help :as help]))

;; ---- AR-U5-2(a): width ----

(def ^:private width help/default-wrap-width)

(defn- unbreakable-overflow?
  "True only when a too-long line is explained entirely by AR-U5-1's own
  exception: strip its leading indent and what remains is one token
  with no space in it (so it genuinely could not have been broken
  further without splitting the token itself)."
  [line]
  (let [trimmed (str/triml line)]
    (and (seq trimmed) (not (str/includes? trimmed " ")))))

(defn- assert-lines-within-width
  [label text]
  (doseq [line (str/split-lines text)]
    (is (or (<= (count line) width) (unbreakable-overflow? line))
        (str label ": line exceeds " width " columns and is not a single "
             "unbreakable token: " (pr-str line)))))

(deftest top-level-lines-fit-width-test
  (assert-lines-within-width "render-top-level" (help/render-top-level help/cli-spec)))

(deftest every-group-page-lines-fit-width-test
  (doseq [g (help/group-names help/cli-spec)]
    (assert-lines-within-width (str "render-group " g) (help/render-group help/cli-spec g))))

;; ---- AR-U5-2(b): content preservation ----

(def ^:private rendered-string-positions
  "Same rendered-string positions help-voice-test already walks --
  reused here (redefined locally, not required cross-test, matching
  this file's own dependency shape) so this test stays spec-derived
  rather than hand-listing surfaces."
  #{:doc :meaning :positional-doc :default})

(defn- rendered-strings
  [spec]
  (let [acc (atom [])]
    (walk/postwalk (fn [x]
                      (when (map? x)
                        (doseq [k rendered-string-positions]
                          (let [v (get x k)]
                            (when (string? v) (swap! acc conj v)))))
                      x)
                    spec)
    @acc))

(deftest wrap-lines-round-trips-every-rendered-spec-string-test
  (testing "joining wrap-lines' own output with single spaces reconstructs the input exactly"
    (doseq [s (rendered-strings help/cli-spec)]
      (is (= s (str/join " " (#'help/wrap-lines s width)))
          (str "wrap-lines lost or altered content for: " (pr-str s))))))

(defn- normalize-whitespace
  [s]
  (-> s (str/replace #"\s+" " ") str/trim))

(deftest wrapped-and-unwrapped-pages-carry-identical-content-test
  (testing "top-level page"
    (is (= (normalize-whitespace (help/render-top-level help/cli-spec 100000))
           (normalize-whitespace (help/render-top-level help/cli-spec width)))))
  (testing "every group page"
    (doseq [g (help/group-names help/cli-spec)]
      (is (= (normalize-whitespace (help/render-group help/cli-spec g 100000))
             (normalize-whitespace (help/render-group help/cli-spec g width)))
          (str "group " g ": wrapped/unwrapped content diverges")))))

;; ---- mechanism sanity: prove wrap-lines actually wraps, and respects
;; the unbroken-token exception, on synthetic input independent of
;; cli-spec's own current strings ----

(deftest wrap-lines-mechanism-sanity-test
  (testing "wraps on spaces at the given width"
    (is (= ["one two" "three four"]
           (#'help/wrap-lines "one two three four" 10))))
  (testing "a single token longer than width lands alone, unbroken"
    (is (= ["a" "reallylongunbreakabletoken" "b"]
           (#'help/wrap-lines "a reallylongunbreakabletoken b" 5))))
  (testing "wrap-with-hanging-indent aligns continuation lines under the prefix"
    (is (= "  --x  one two\n       three\n       four"
           (#'help/wrap-with-hanging-indent "  --x  " "one two three four" 15)))))
