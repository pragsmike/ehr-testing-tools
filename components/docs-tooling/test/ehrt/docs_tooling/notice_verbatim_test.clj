(ns ehrt.docs-tooling.notice-verbatim-test
  "Vendoring batch 3 rider (AR-VB3-R1, `notes/adr/0072-vendoring-batch-3.md`):
  the verbatim law (`components/sim/resources/sim/modules/NOTICE`'s own
  closing paragraph, \"no reformatting, no re-indentation\") had never
  actually been MECHANICALLY checked -- every prior 'zero problems'
  claim (ADR-0070, ADR-0071) was a manual `sha256sum` re-check against
  the table, done by a human-in-the-loop reading, never a gate. This
  session's own design-channel probe found `lookup_tables/
  uti_recurrence.csv` had silently drifted: its NOTICE row has always
  recorded the CRLF upstream bytes' own hash
  (`baf597d2...`), but the repo's root `.gitattributes` rule
  (`* text=auto eol=lf`) normalized the COMMITTED blob to LF
  (`b83c2960...`) -- a fresh clone gets the wrong bytes even though the
  NOTICE row was never edited and stayed honest about upstream. This
  test promotes that probe to a standing gate: every markdown table row
  in every NOTICE file that names a vendored file's own SHA-256 is
  re-hashed against that file's real on-disk bytes.

  Only tables whose header EXACTLY matches
  `| Filename | Upstream URL | Commit SHA | SHA-256 | Retrieved |` are
  eligible -- this is deliberately not a generic pipe-table heuristic;
  a NOTICE file with some other table shape (or none) is silently
  skipped, never mistaken for a provenance table. Filenames are
  resolved relative to their own NOTICE file's directory, matching how
  every vendoring ADR to date has written them (`lookup_tables/
  uti_recurrence.csv`, `medications/otc_pain_reliever.json`, etc, all
  relative to `components/sim/resources/sim/modules/`)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def ^:private excluded-dir-names #{"target" ".git"})

(defn- under-excluded-dir? [^java.io.File f]
  (some excluded-dir-names
        (map str (-> f .toPath .iterator iterator-seq))))

(defn- notice-files
  "Every file named exactly NOTICE or NOTICE.md under the repo root,
  target/ and .git/ pruned -- same convention
  `ehrt.docs-tooling.license-text-pointer-test` already establishes."
  []
  (->> (file-seq (io/file "."))
       (filter #(.isFile ^java.io.File %))
       (remove under-excluded-dir?)
       (filter #(#{"NOTICE" "NOTICE.md"} (.getName ^java.io.File %)))
       sort))

(def ^:private expected-header
  ["Filename" "Upstream URL" "Commit SHA" "SHA-256" "Retrieved"])

(defn- table-cells [line]
  (->> (str/split line #"\|")
       (map str/trim)
       (remove str/blank?)))

(defn- unbacktick [s]
  (str/replace s #"^`|`$" ""))

(defn- separator-row? [cells]
  (and (seq cells) (every? #(re-matches #"-+" %) cells)))

(defn- parse-provenance-table
  "Parses `content`'s own markdown table, if any, whose header line's
  cells are EXACTLY `expected-header` -- into a vector of
  {:filename :sha256} maps, both backtick-stripped. Returns nil (no
  rows) when no such header is present. Table rows are every
  contiguous `|`-prefixed line following the header and its `|---|...`
  separator, stopping at the first line that doesn't start with `|`."
  [content]
  (let [lines (str/split-lines content)
        header-idx (->> lines
                         (map-indexed vector)
                         (filter (fn [[_ l]] (= expected-header (table-cells l))))
                         ffirst)]
    (when header-idx
      (->> (drop (+ 2 header-idx) lines)
           (take-while #(str/starts-with? (str/trim %) "|"))
           (map table-cells)
           (remove separator-row?)
           (keep (fn [cells]
                   (when (>= (count cells) 4)
                     {:filename (unbacktick (nth cells 0))
                      :sha256 (unbacktick (nth cells 3))})))))))

(defn- sha256-hex [^java.io.File f]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (with-open [in (io/input-stream f)] (.readAllBytes in))]
    (->> (.digest digest bytes)
         (map #(format "%02x" %))
         (apply str))))

(deftest every-notice-table-row-matches-its-named-files-on-disk-bytes-test
  (doseq [notice-file (notice-files)]
    (let [rows (parse-provenance-table (slurp notice-file))
          dir (.getParentFile ^java.io.File notice-file)]
      (doseq [{:keys [filename sha256]} rows]
        (let [target (io/file dir filename)]
          (testing (str notice-file " row " filename)
            (is (.exists target)
                (str filename " named in " notice-file
                     " does not exist on disk at " target))
            (when (.exists target)
              (is (= sha256 (sha256-hex target))
                  (str target
                       " does not match its NOTICE row's recorded SHA-256 ("
                       sha256 ") -- the on-disk bytes are not verbatim "
                       "against the upstream fetch this row records "
                       "(git may have rewritten line endings on checkout; "
                       "see .gitattributes). Actual on-disk hash: "
                       (sha256-hex target))))))))))

;; -- mechanism-sanity: prove the extraction/hashing functions actually catch what they claim to --

(deftest parse-provenance-table-extraction-is-actually-caught-test
  (let [content (str "Some prose.\n\n"
                      "| Filename | Upstream URL | Commit SHA | SHA-256 | Retrieved |\n"
                      "|---|---|---|---|---|\n"
                      "| `a.json` | `https://example/a.json` | `deadbeef` | `abc123` | 2026-08-07 |\n"
                      "| `b.json` | `https://example/b.json` | `deadbeef` | `def456` | 2026-08-07 |\n"
                      "\nMore prose after the table, never parsed as a row.\n")]
    (is (= [{:filename "a.json" :sha256 "abc123"}
            {:filename "b.json" :sha256 "def456"}]
           (parse-provenance-table content)))))

(deftest parse-provenance-table-skips-non-matching-tables-test
  (let [content (str "| Name | Value |\n"
                      "|---|---|\n"
                      "| `x` | `y` |\n")]
    (is (nil? (parse-provenance-table content))
        "a table whose header isn't the exact provenance shape must never be mistaken for one")))

(deftest sha256-hex-is-actually-caught-test
  (let [f (java.io.File/createTempFile "notice-verbatim-test" ".txt")]
    (try
      (spit f "hello\n")
      (is (= "5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"
             (sha256-hex f))
          "known SHA-256 of \"hello\\n\" -- proves sha256-hex against a fixed input, not just self-consistency")
      (finally (.delete f)))))
