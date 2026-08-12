(ns ehrt.docs-tooling.invocation-lint-test
  "AR-U1-2 (ux fixes 1, `notes/ADRs.md` ADR-0059): the stale `clojure
  -M:cli` invocation alias -- swept from every live doc surface this
  session (register row U1, `.agents/plans/2026-08-06-ux-audit-
  findings.md`) -- is forbidden outright on the same surface set the
  sweep covered, so a future demo doesn't silently reintroduce it.
  `bin/ehrt` is the taught, cwd-safe entry point; `clojure -M:cli` is a
  dead alias, not a documented alternate (the alternate that IS
  documented, bare `clojure -M:ehrt`, is untouched by this gate).

  Scoped exactly as the sweep was: README.md, AUTHORS-GUIDE.md, every
  .md and .edn file under docs/**, and every .md and .edn file under
  components/*/docs/**. Frozen archives, `.agents/prompts/`,
  `.agents/session-records/`, dated one-shot plan files, and the audit
  registers are out of scope by construction -- none of them live under
  any of the scanned roots, same scoping discipline
  `ehrt.docs-tooling.stale-path-test`'s own family uses throughout.

  Widened to demos/** and .github/** (R3-B5-4, `.agents/plans/
  2026-08-12-review-3-user-surface-findings.md`, ruled [C, un-vetoed],
  ADR-0118): the demo tree relocated wholesale out of
  components/*/docs/demos/ to a new top-level demos/ tree (ADR-0073)
  after this gate's scan roots were last set, leaving the successor
  tree unprotected; `.github/` is a genuinely operator-facing surface
  (issue templates) this gate never covered at all -- same
  recurrence-prevention logic as every other scanned root, not a new
  category of thing to guard against."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private forbidden-invocation "clojure -M:cli")

(defn- doc-like-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(or (str/ends-with? (.getName ^java.io.File %) ".md")
                     (str/ends-with? (.getName ^java.io.File %) ".edn")))
       (map #(.getPath ^java.io.File %))))

(defn- component-docs-roots []
  (->> (.listFiles (io/file "components"))
       (filter #(.isDirectory ^java.io.File %))
       (map #(io/file % "docs"))
       (filter #(.exists ^java.io.File %))))

(defn- scan-sources []
  (concat ["README.md" "AUTHORS-GUIDE.md"]
          (doc-like-files (io/file "docs"))
          (mapcat doc-like-files (component-docs-roots))
          (doc-like-files (io/file "demos"))
          (doc-like-files (io/file ".github"))))

(deftest no-stale-cli-alias-invocation-anywhere-in-live-docs-test
  (doseq [path (scan-sources)]
    (let [content (slurp path)]
      (is (not (str/includes? content forbidden-invocation))
          (str path " teaches the stale `clojure -M:cli` invocation -- "
               "bin/ehrt is the live entry point (register row U1, AR-U1-2)")))))

(deftest forbidden-invocation-pattern-is-actually-caught-test
  (is (str/includes? "run it with clojure -M:cli run --seed 1" forbidden-invocation))
  (is (not (str/includes? "run it with bin/ehrt sim run --seed 1" forbidden-invocation))))

;; AR-U2-R (ux fixes 2, ADR-0060): grammar-validity is not path-validity.
;; The invocation-lint gate above only ever checked that a fence's
;; TEXT was correct (no stale alias); it never checked that a fence's
;; own path ARGUMENTS actually resolve -- which is exactly how the
;; swept demo fences (AR-U1-1) kept a cwd-relative `--config` value
;; that grammar-checked fine and still resolved to nothing under
;; `bin/ehrt`'s forced root cwd. This extension resolves, not just
;; parses.

(def ^:private path-resolving-flags
  "File-path-carrying flags whose literal value in a live doc fence must
  resolve to an existing file/dir from workspace root. --out-dir,
  --report, and --baseline are deliberately NOT in this set: every
  in-scope literal value for those three flag names is something the
  command itself CREATES (an out/... destination) -- exempted BY FLAG
  NAME, per AR-U2-R, not by path."
  #{"--config" "--profile" "--path"})

(def ^:private illustrative-path-exemptions
  "Explicit, disclosed exemptions: a literal value the reader is meant
  to author and save themselves, never a repo path, so it can never
  resolve from workspace root under any correction. One entry:
  `--config stmarys.edn` in docs/simulate-your-facility.md's own
  walkthrough, whose surrounding prose already says the shipped
  demos/ examples are the exact, runnable ones and this inline one is
  for the reader to build from the interview above and save under a
  name of their own choosing."
  #{["docs/simulate-your-facility.md" "stmarys.edn"]})

(defn- strip-line-comments
  "A shell `#` comment (start-of-line or preceded by whitespace, the
  same rule the shell itself uses) is prose ABOUT a flag, not an
  invocation of one -- e.g. `# --path takes a file, or a directory of
  files ...` (docs/use-cases/generate-controlled-fault-data.md's own
  walkthrough comment) must never be misread as a real `--path`
  argument."
  [fence-body]
  (->> (str/split-lines fence-body)
       (map #(str/replace % #"(^|\s)#.*$" "$1"))
       (str/join "\n")))

(defn- join-line-continuations
  "A `\\`-continued fence line is one logical command line -- joining
  before tokenizing keeps a flag and its value together regardless of
  which physical line either one landed on."
  [fence-body]
  (str/replace fence-body #"\\\r?\n[ \t]*" " "))

(defn- fence-bodies
  "Every ```bash/```sh fenced block's own body, verbatim."
  [content]
  (map second (re-seq #"(?s)```(?:bash|sh)\n(.*?)```" content)))

(defn- strip-quotes
  [^String s]
  (if (and (>= (count s) 2) (#{\" \'} (first s)) (= (first s) (last s)))
    (subs s 1 (dec (count s)))
    s))

(defn- flag-values
  "Every {:flag :value} pair in `fence-body` whose flag is in `flag-set`,
  value taken as the very next whitespace-delimited token (line
  continuations already joined by the caller)."
  [fence-body flag-set]
  (let [tokens (str/split (join-line-continuations (strip-line-comments fence-body)) #"\s+")]
    (keep-indexed (fn [i tok]
                    (when (and (contains? flag-set tok) (< (inc i) (count tokens)))
                      {:flag tok :value (strip-quotes (nth tokens (inc i)))}))
                  tokens)))

(defn- path-argument-problems
  "Every path-resolving-flags value in `path`'s own fences that is
  neither a shell variable (`$...`, uncheckable from the doc text
  alone) nor a disclosed illustrative exemption, and does not resolve
  to an existing file/dir from workspace root."
  [path]
  (let [content (slurp path)]
    (for [fence (fence-bodies content)
          {:keys [flag value]} (flag-values fence path-resolving-flags)
          :when (not (str/starts-with? value "$"))
          :when (not (contains? illustrative-path-exemptions [path value]))
          :when (not (.exists (io/file value)))]
      {:flag flag :value value})))

(deftest fence-path-arguments-resolve-from-workspace-root-test
  (doseq [path (scan-sources)]
    (let [problems (path-argument-problems path)]
      (is (empty? problems)
          (str path " has a command fence path argument that does not "
               "resolve from workspace root: " (pr-str problems)
               " -- grammar-validity is not path-validity (AR-U2-R, ADR-0060)")))))

(deftest fence-path-detection-mechanism-is-actually-caught-test
  (let [content (str "before\n```bash\nbin/ehrt sim run --seed 1 \\\n"
                      "  --config no/such/file.edn --emit hl7\n```\nafter")
        bodies (fence-bodies content)]
    (is (= 1 (count bodies)))
    (is (str/includes? (join-line-continuations (first bodies))
                        "--config no/such/file.edn --emit hl7"))
    (is (= [{:flag "--config" :value "no/such/file.edn"}]
           (flag-values (first bodies) path-resolving-flags))))
  (is (empty? (flag-values "bin/ehrt corpus mutate --out-dir out/x" path-resolving-flags))
      "--out-dir is exempted by flag name, never even extracted as a candidate")
  (is (= [{:flag "--config" :value "$CFG"}]
         (flag-values "bin/ehrt sim run --config $CFG" path-resolving-flags))
      "a shell variable IS extracted here -- it is path-argument-problems' own job to skip it")
  (is (= [{:flag "--config" :value "components/sim/docs/demos/module-mix/config.edn"}]
         (flag-values "bin/ehrt sim run --config components/sim/docs/demos/module-mix/config.edn"
                       path-resolving-flags)))
  (is (empty? (flag-values "# Pick one patient bundle. --path takes a file, or a directory"
                            path-resolving-flags))
      "a shell comment about a flag is prose, not an invocation of it (generate-controlled-fault-data.md's own false-positive case)"))
