(ns ehrt.docs-tooling.artifact-provenance-test
  "ADR-0158, review-4 register rows L3-3, L3-5, L3-6, L3-7, L3-8, L3-11
  -- one defect in six places: a generated artifact that does not name
  what moves it.

  That is what made both of `state-derived.md`'s undocumented-mover
  discoveries (ADR-0143, ADR-0152) and the ADR-0135 converter blast
  radius DISCOVERIES rather than predictions. Each was found as a
  pre-push red by a session that had no way to know its edit was an
  input. The remedy in every case is the same shape: the artifact names
  its own inputs, generated from the thing that actually reads them, so
  the naming cannot go stale.

  Five claims, each red at this session's own red-first commit:

  (a) L3-3 -- `.agents/state-derived.md` carries its own input list, the
      list is non-empty, and every path in it exists. A generated
      enumeration that had silently emptied would otherwise render a
      heading with nothing under it and read as 'nothing moves this'.

  (b) L3-6/L3-7 -- every generated artifact carrying converter-rendered
      mermaid names the converter. The population is derived from CI's
      own freshness diff list, filtered to files that actually hold a
      mermaid block, and pinned at 28 so a silent narrowing is loud.

  (c) L3-8 -- `AGENTS.md` points at the generated list instead of
      hand-listing four of 53 files, and no longer makes the four-file
      claim.

  (d) L3-11 -- `demos/traces/README.md`, the largest generated tree's
      own front door, names `make traces`.

  (e) L3-5 -- `docs/formats.md`'s generated banner names BOTH inputs.
      `event_log_doc/render` slurps the schema AND the examples, and
      `event-examples.edn` supplies every rendered example on the page,
      while the banner named only the schema."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.make-graph :as mg]))

(def ^:private state-derived ".agents/state-derived.md")
(def ^:private agents-md "AGENTS.md")
(def ^:private traces-readme "demos/traces/README.md")
(def ^:private formats-md "docs/formats.md")

(def ^:private converter
  "components/palgebra/tools/resource_equations_to_mermaid.py")

(def ^:private input-list-heading "## What this page reads")
(def ^:private generated-surface-heading "## Generated surface")

;; ---- (a) state-derived names its own inputs ----

(defn- input-list-paths
  "Every `path` in a backticked first cell of the input-list table."
  [content]
  (let [section (second (str/split content (re-pattern input-list-heading) 2))
        table (first (str/split (or section "") #"\n## " 2))]
    (->> (str/split-lines (or table ""))
         (keep #(second (re-find #"^\| `([^`]+)` \|" %)))
         vec)))

(deftest state-derived-carries-its-own-input-list-test
  (let [content (slurp state-derived)]
    (testing "(a) the section exists"
      (is (str/includes? content input-list-heading)
          (str state-derived " has no `" input-list-heading "` section -- register row L3-3's "
               "whole remedy is that this page enumerates what moves it")))
    (let [paths (input-list-paths content)]
      (testing "(a) the list is non-empty"
        (is (seq paths)
            "the input-list section is present but lists no paths -- a heading over an empty table reads as 'nothing moves this'"))
      (testing "(a) every listed input exists"
        (doseq [p paths]
          (is (.exists (io/file p))
              (str state-derived " lists `" p "` as an input, and it does not exist")))))))

;; ---- (b) converter-rendered artifacts name the converter ----

(defn- tracked-under [path]
  (let [{:keys [exit out]} (shell/sh "git" "ls-files" "--" path)]
    (when-not (zero? exit)
      (throw (ex-info (str "git ls-files failed for " path) {:path path})))
    (->> (str/split-lines out) (remove str/blank?) vec)))

(defn- generated-files
  "Every tracked file CI's generated-doc freshness step diffs."
  []
  (->> (or (mg/freshness-diff-paths (slurp mg/workflow)) [])
       (mapcat tracked-under)
       distinct
       vec))

(defn- carries-mermaid? [path]
  (or (str/ends-with? path ".mermaid")
      (str/includes? (slurp path) "```mermaid")))

(deftest every-converter-rendered-artifact-names-the-converter-test
  (let [population (filterv carries-mermaid? (generated-files))]
    (testing "(b) the population is non-empty and has not silently narrowed"
      (is (seq population)
          "no generated artifact carrying mermaid was found -- the derivation has stopped matching")
      (is (= 28 (count population))
          (str "the converter-rendered population is " (count population) ", pinned at 28 by "
               "ADR-0158's own measurement (docs/dev/pipeline.md, 22 use-case pages, "
               "sim-theory-diagram.{md,mermaid}, 3 palgebra .mermaid). If an artifact was "
               "legitimately added or removed, move the pin in the same commit.\n"
               (str/join "\n" (map #(str "  " %) (sort population))))))
    (testing "(b) each names the converter that produced its diagram"
      (doseq [p (sort population)]
        (is (str/includes? (slurp p) converter)
            (str p " holds converter-rendered mermaid but never names "
                 converter ". A converter change moves every one of these; unless each "
                 "points back, the blast radius is not greppable from the artifacts "
                 "(register row L3-6 -- this is the ADR-0135 incident's own shape)."))))))

;; ---- (c) AGENTS.md points at the generated list ----

(deftest agents-md-points-at-the-generated-list-test
  (let [content (slurp agents-md)]
    (testing "(c) it points at the generated enumeration"
      (is (str/includes? content generated-surface-heading)
          (str agents-md " does not point at `" state-derived "`'s `" generated-surface-heading
               "` section. Register row L3-8: this file told a cold agent that FOUR files are "
               "generated when docsgen writes 53.")))
    (testing "(c) the superseded hand list is gone"
      (is (not (re-find #"(?s)GENERATED, never hand-edited:.{0,400}two record `INDEX\.md` files" content))
          (str agents-md " still carries the four-file hand list. A pointer beside a stale "
               "list is worse than either alone: a reader believes the list.")))))

;; ---- (d) the traces front door names its own target ----

(deftest traces-front-door-names-make-traces-test
  (testing "(d) demos/traces/README.md names `make traces`"
    (is (str/includes? (slurp traces-readme) "make traces")
        (str traces-readme " never mentions `make traces`. It is the front door of the "
             "largest generated tree in the repo and the slowest docsgen leaf, and it is the "
             "surface that drifted twice (ADR-0142, ADR-0149) for want of exactly this "
             "pointer (register row L3-11)."))))

;; ---- (e) formats.md names both of its inputs ----

(deftest formats-banner-names-both-inputs-test
  (testing "(e) the generated banner names the examples file as well as the schema"
    (let [banner (or (second (re-find #"(?m)^(<!-- Generated by .*?-->)" (slurp formats-md))) "")]
      (is (seq banner) (str "no generated banner found in " formats-md))
      (is (str/includes? banner "event-schema.edn")
          "the banner no longer names the schema input")
      (is (str/includes? banner "event-examples.edn")
          (str "the banner names only one of the page's TWO inputs. `event_log_doc/render` "
               "slurps the examples too, and they supply every rendered example on the page, "
               "so a session tracing why formats.md moved is pointed at the wrong file "
               "(register row L3-5).")))))
