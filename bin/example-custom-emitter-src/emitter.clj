(ns emitter
  "A deliberately trivial CUSTOM emitter: ground-truth event log in,
  a made-up pipe-delimited format out.

  This exists to prove one seam end to end -- that a consumer can take
  `ehrt sim run --format ground-truth` and render it into a format this
  project has never heard of, using nothing but the documented contract
  (`docs/formats.md`, 'The event log'). It is a WORKED EXAMPLE, not a
  feature: nobody should ship this format, and the point is that you
  would not want to.

  Everything it does is deliberately small enough to read in one sitting:

  - It iterates the TOP-LEVEL VECTOR ONLY. That is the first thing
    `docs/formats.md` tells a consumer, because `:registered` events
    carry `:pre-horizon-facts` whose entries have an `:event` key of
    their own, four of whose values collide with real log kinds. A
    tree-walking emitter would emit admissions that never happened.
  - It keys patients off `:participants`, never `:active-mrn` -- which
    is absent from `:bed-swap`, `:merge`, and `:step-rejected`.
  - It reads `:t` as an integer of seconds since the run began, not as
    a timestamp, and anchors nothing.
  - It ignores every kind it does not care about, by design, and says
    how many it skipped rather than silently dropping them. A real
    custom emitter's most important property is knowing what it did not
    translate.

  No dependency on anything in this repo -- not the schema, not a
  component. That is the demonstration: if this needed to require
  `ehrt.sim-engine.event-schema` to work, the log would not really be a
  consumable contract."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private translated
  "The kinds this toy format carries. Everything else is counted and
  skipped -- explicitly, so the summary can say so."
  #{:admission :discharge})

(defn- subject
  "The primary participant's patient-id. `:participants` is the
  universal key; `:active-mrn` is not."
  [event]
  (:patient-id (first (:participants event))))

(defn- render
  "One event -> one pipe-delimited line. `:t` stays an integer: the log
  carries no wall-clock time, so neither does this."
  [event]
  (str/join "|" [(name (:event event))
                 (:t event)
                 (subject event)
                 (or (:active-mrn event) "")
                 (get-in event [:location :ward] "")
                 (get-in event [:location :bed] "")]))

(defn -main
  [& [path]]
  (when-not path
    (binding [*out* *err*] (println "usage: bin/example-custom-emitter <events.edn>"))
    (System/exit 2))
  (let [events (edn/read-string (slurp path))
        wanted (filter #(translated (:event %)) events)]
    (println "# kind|t-seconds|patient-id|mrn|ward|bed")
    (run! (comp println render) wanted)
    (binding [*out* *err*]
      (println (format "# %d events in, %d translated, %d skipped (kinds not in this format)"
                       (count events) (count wanted) (- (count events) (count wanted)))))))
