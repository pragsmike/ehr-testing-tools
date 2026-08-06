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
  ([label text] (assert-lines-within-width label text width))
  ([label text at-width]
   (doseq [line (str/split-lines text)]
     (is (or (<= (count line) at-width) (unbreakable-overflow? line))
         (str label ": line exceeds " at-width " columns and is not a single "
              "unbreakable token: " (pr-str line))))))

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

;; ---- AR-EP-3 (ux epilogue, `notes/adr/0065-ux-epilogue.md`): the
;; --width/COLUMNS resolution the arc close's own intake row deferred.
;; Every-line-fits and content-preservation both re-proven at 40 (the
;; floor), 60, and 120 -- not just the 80-column default above -- since
;; the whole point of a real knob is that other widths are now live,
;; not merely a test-only comparison arm. ----

(deftest top-level-lines-fit-at-non-default-widths-test
  (doseq [w [40 60 120]]
    (assert-lines-within-width (str "render-top-level@" w) (help/render-top-level help/cli-spec w) w)))

(deftest every-group-page-lines-fit-at-non-default-widths-test
  (doseq [w [40 60 120]
          g (help/group-names help/cli-spec)]
    (assert-lines-within-width (str "render-group " g "@" w) (help/render-group help/cli-spec g w) w)))

(deftest wrapped-pages-carry-identical-content-at-non-default-widths-test
  (doseq [w [40 60 120]]
    (testing (str "top-level page @ " w)
      (is (= (normalize-whitespace (help/render-top-level help/cli-spec 100000))
             (normalize-whitespace (help/render-top-level help/cli-spec w)))))
    (doseq [g (help/group-names help/cli-spec)]
      (testing (str "group " g " page @ " w)
        (is (= (normalize-whitespace (help/render-group help/cli-spec g 100000))
               (normalize-whitespace (help/render-group help/cli-spec g w))))))))

;; ---- AR-EP-3: resolution order + validation ----

(deftest resolve-width-explicit-beats-columns-env-beats-default-test
  (testing "explicit --width wins outright, even with a usable COLUMNS present"
    (is (= 100 (help/resolve-width {:explicit-width 100 :columns-env "50"}))))
  (testing "no explicit width: a usable COLUMNS wins"
    (is (= 50 (help/resolve-width {:columns-env "50"}))))
  (testing "no explicit width, no COLUMNS: the default"
    (is (= help/default-wrap-width (help/resolve-width {})))))

(deftest resolve-width-columns-env-falls-back-silently-on-a-bad-value-test
  (testing "non-numeric COLUMNS never errors -- falls back to the default"
    (is (= help/default-wrap-width (help/resolve-width {:columns-env "not-a-number"}))))
  (testing "COLUMNS under the floor falls back to the default, not the floor"
    (is (= help/default-wrap-width (help/resolve-width {:columns-env "10"}))))
  (testing "COLUMNS unset (nil) falls back to the default"
    (is (= help/default-wrap-width (help/resolve-width {:columns-env nil})))))

(deftest resolve-width-columns-env-exactly-at-the-floor-is-usable-test
  (is (= 40 (help/resolve-width {:columns-env "40"}))))

(deftest parse-width-flag-accepts-an-integer-at-or-above-the-floor-test
  (is (= {:width 40} (help/parse-width-flag "40")))
  (is (= {:width 200} (help/parse-width-flag "200"))))

(deftest parse-width-flag-rejects-by-name-below-the-floor-or-non-numeric-test
  (is (= {:error {:value "39" :expected "an integer >= 40"}} (help/parse-width-flag "39")))
  (is (= {:error {:value "abc" :expected "an integer >= 40"}} (help/parse-width-flag "abc")))
  (is (= {:error {:value "12.5" :expected "an integer >= 40"}} (help/parse-width-flag "12.5"))))
