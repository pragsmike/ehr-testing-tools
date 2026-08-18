(ns ehrt.docs-tooling.trace-capture
  "ADR-0149: the one repo-owned formatting step `bin/regen-traces` needs,
  and nothing more.

  `demos/traces/**`'s committed files are three shapes, and the CLI hands
  back exactly two of them without help:

  - `messages*.txt` -- bare ER7 wire bytes. `ehrt sim run --format er7`
    prints these verbatim, so the script captures that stdout directly
    and this namespace never touches those bytes. Where a trace's README
    teaches no `--format er7` command, the same bytes are rebuilt here
    from the envelope's own `:messages` by the join `sim-er7-bare-text`
    documents -- messages separated by one blank line -- plus the single
    trailing newline `main!`'s `println` adds. The script `cmp`s the two
    derivations against each other wherever a README teaches both, so the
    rebuild is never trusted on its own say-so.

  - `ground-truth.edn` -- the run's own event vector, PRETTY. The CLI's
    `--format ground-truth` is `pr-str`, one line: readable straight back
    by `edn/read` (that is the point -- it feeds `ehrt sim check`), but
    not what is committed. `clojure.pprint/pprint` at its default margin
    is: probed at 922e55a against all six committed files, five
    reproduce BYTE-FOR-BYTE with no tuning of any kind. The sixth
    (`module-mix`) reproduces nothing, because its committed bytes were
    never its own command's output -- see ADR-0149's census.

  - `fhir-bundle-patient1.json` -- ONE patient's bundle out of the
    `--emit fhir` envelope's `{patient-id -> Bundle}` map, pretty JSON.
    Patient 1 is the lowest patient ordinal, and the ids sort by it
    (`PID-<ordinal6>-<hash>`, `ehrt.sim-engine.engine/patient-id-for`),
    so `first`-of-`sort` names it without a literal id here that would
    silently stop matching if the run changed.

  Deliberately NOT a general pretty-printer: no margin parameter, no
  format options, no per-file overrides. The committed FHIR bundle was
  captured at `*print-right-margin*` 71 (found by probe -- 71 reproduces
  it exactly, the default 72 re-wraps a single `\"identifier\":` line).
  Pinning 71 here would buy a zero-byte diff with a magic number no
  future reader could justify, and would leave this namespace printing
  EDN at one margin and JSON at another. The default is used for both,
  the one re-wrapped line is regenerated, and ADR-0149 discloses it."
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(defn- read-envelope
  "One captured `ehrt sim run` stdout, as data. Throws with the file named
  if it does not read as EDN or carries no `:payload` -- a truncated or
  error-shaped capture must fail loudly here rather than silently
  materialize an empty trace file over a good one."
  [path]
  (let [env (try (edn/read-string (slurp path))
                 (catch Exception e
                   (throw (ex-info "captured envelope does not read as EDN"
                                   {:envelope path} e))))]
    (when-not (map? (:payload env))
      (throw (ex-info "captured envelope carries no :payload map"
                      {:envelope path :read (type env)})))
    env))

(defn er7-text
  "The envelope's own `:messages`, rendered as the bytes `--format er7`
  prints: messages joined by one blank line
  (`ehrt.cli.core/sim-er7-bare-text`) plus the one trailing newline
  `main!`'s `println` adds."
  [env]
  (let [messages (get-in env [:payload :messages])]
    (when-not (seq messages)
      (throw (ex-info "envelope carries no :messages -- was it run without --emit hl7?"
                      {:payload-keys (sort (keys (:payload env)))})))
    (str (str/join "\n\n" messages) "\n")))

(defn ground-truth-text
  "The envelope's own `:ground-truth` vector, pretty-printed at
  `clojure.pprint`'s default margin."
  [env]
  (let [gt (get-in env [:payload :ground-truth])]
    (when-not (seq gt)
      (throw (ex-info "envelope carries no :ground-truth events"
                      {:payload-keys (sort (keys (:payload env)))})))
    (with-out-str (pprint/pprint gt))))

(defn fhir-bundle-text
  "Patient 1's own bundle out of the envelope's `:fhir-bundles` map --
  lowest patient ordinal, named by sorting the ids rather than by a
  literal -- as pretty JSON at the same default margin."
  [env]
  (let [bundles (get-in env [:payload :fhir-bundles])]
    (when-not (seq bundles)
      (throw (ex-info "envelope carries no :fhir-bundles -- was it run without --emit fhir?"
                      {:payload-keys (sort (keys (:payload env)))})))
    (with-out-str (json/pprint (get bundles (first (sort (keys bundles))))))))

(def ^:private renderers
  {:er7 er7-text :ground-truth ground-truth-text :fhir-bundle fhir-bundle-text})

(defn materialize!
  "-X entry (`clojure -X:dev ehrt.docs-tooling.trace-capture/materialize!
  :plan '\"<path>\"'`): renders every entry of the EDN plan at `:plan`,
  each `{:kind :er7|:ground-truth|:fhir-bundle :envelope <captured
  stdout> :out <committed path>}`, and writes it.

  One JVM for the whole tree on purpose. `bin/regen-traces` already pays
  one `bin/ehrt` start per taught command; a second start per derived
  file would roughly double `make traces`, and the wall clock of a
  `docsgen` leaf is a per-push cost CI pays on every push."
  [{:keys [plan]}]
  (let [entries (edn/read-string (slurp plan))]
    (doseq [{:keys [kind envelope out]} entries]
      (let [render (or (get renderers kind)
                       (throw (ex-info "unknown trace-capture kind"
                                       {:kind kind :known (sort (keys renderers))})))
            text (render (read-envelope envelope))]
        (io/make-parents out)
        (spit out text :encoding "UTF-8")
        (println (format "  %-14s %-52s %7d bytes" (name kind) out (count text)))))
    (println (str "Materialized " (count entries) " trace file(s)"))))
