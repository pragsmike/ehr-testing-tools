;; The `:ground-truth`-only manifest generator (arc 4 sweep 1,
;; `notes/adr/0175-arc-4-emission-add-ons.md` ruling E1). Read by
;; `bin/ground-truth-bracket`; not an entry point of its own.
;;
;; WHAT IT DOES. Given a directory of `<root>.edn` files written by
;; `ehrt.oracle.digest/-main`, it prints one `sha256sum`-shaped line per
;; root that CARRIES a `:ground-truth` key, digesting `(pr-str
;; (:ground-truth m))` and nothing else -- so the `:hl7` half of the pair
;; is excluded from the hash. Roots with no `:ground-truth` key are
;; NAMED on a `skipped ` line rather than silently dropped
;; (`rulings.md#R-population-closure`: a population a bracket does not
;; cover has to be visible in its own output).
;;
;; WHY parse-and-reprint IS SOUND. Both sides are read and printed by
;; THIS process, with one reader and one printer. Byte-identical
;; `:ground-truth` text therefore reads to an equal value and prints to
;; identical bytes, so unchanged ground truth cannot produce a spurious
;; DIFFERS; and a changed value has to print differently for anything
;; the emitter is capable of putting in the log. The hash is never
;; compared against a stored constant -- only against the other side of
;; the same invocation -- so the printer's own conventions cancel out.
;;
;; The three interpreter-layer roots (`appendicitis`, `sore-throat`,
;; `ear-infections`) write a VECTOR of walks rather than the
;; `{:ground-truth :hl7}` pair, which is why the skip list exists and why
;; it is expected to hold exactly those three. It is measured per run,
;; never assumed.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '[java.security MessageDigest])

(defn- sha256-hex [^String s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (.getBytes s "UTF-8"))
         (map #(format "%02x" (bit-and % 0xff)))
         (str/join))))

(let [dir (io/file (first *command-line-args*))
      files (sort-by #(.getName ^java.io.File %)
                     (filter #(str/ends-with? (.getName ^java.io.File %) ".edn")
                             (file-seq dir)))]
  (when (empty? files)
    (binding [*out* *err*]
      (println "gt-manifest: no .edn files under" (.getPath dir) "-- STOP"))
    (System/exit 1))
  (doseq [^java.io.File f files]
    (let [v (edn/read-string (slurp f))]
      (if (and (map? v) (contains? v :ground-truth))
        (println (str (sha256-hex (pr-str (:ground-truth v))) "  " (.getName f)))
        (println (str "skipped-no-ground-truth  " (.getName f)))))))
