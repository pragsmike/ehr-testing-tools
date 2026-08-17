(ns emitter-jsonl
  "A second worked example, and a different KIND of example: where
  `bin/example-custom-emitter` proves only the seam (one line out per
  event in, a deliberately useless format), this one proves the part
  that is actually hard -- a target shape that does not line up with the
  log one-to-one, and the mapping decisions that gap forces you to make
  on purpose rather than by accident.

  The target: a line-delimited JSON **encounter feed**, the shape a
  hospital-adjacent system plausibly already ingests -- one object per
  encounter, not per event, with a status and a length of stay. Nothing
  about it is special; it was chosen because it disagrees with the log
  in the three ways a real target always does.

  THE THREE MAPPING DECISIONS, each made visibly:

  1. **Two events fold into one record.** The log emits `:admission` and
     `:discharge` as separate facts at separate `:t`. The target wants
     one encounter object. So this emitter accumulates: an `:admission`
     opens a record, the matching `:discharge` closes it. An encounter
     still open when the run ends is emitted with `\"status\":\"open\"`
     and a null discharge -- NOT dropped, and not silently rounded off
     to the end of the run. A consumer that sees `open` knows the run's
     window ended mid-stay; that is true, and inventing a discharge
     would not be.

  2. **Fields the log has, that the target lacks: dropped and
     COUNTED.** An `:admission` carries `:attending`, `:reason`, and a
     `:location`/`:placement` telling you whether the bed was a licensed
     one or a surge slot. This encounter feed has nowhere to put any of
     them, so they are dropped -- deliberately, and the summary says how
     many records lost each, because \"we dropped a field\" and \"we
     never noticed a field\" look identical downstream unless somebody
     counts. `:warm-up` is reported separately and in two numbers,
     present-vs-true: it is on every event by contract, so what a
     consumer actually wants to know is how many records came from
     inside the warm-up window (traffic the run was still settling
     into), which is a filtering decision the log hands you and this
     format cannot carry.

  3. **A field the target has, that the log lacks: null, and SAID.**
     The target's own schema wants an absolute `admission_datetime`. The
     log has no absolute time at all -- `:t` is an integer of seconds
     since the run began, and `docs/formats.md` states that as a
     property of the contract, not an omission. So the datetime fields
     are `null`, the relative `:t` values are carried through unchanged
     under honest names, and the summary says why. Anchoring is the
     consumer's own decision (it needs a real reference date, which is
     an emit-time input, not a log fact) and this emitter refuses to
     make it up.

  Plus the thing every custom emitter owes its operator, same as the
  first example: a count of what it did NOT translate, broken down by
  kind, so a log that starts carrying something new is visible instead
  of quiet.

  Determinism: records are emitted sorted by (admitted seconds, patient
  id), every JSON object writes its keys in one fixed order, and no
  output depends on map or set iteration order. Same log in, same bytes
  out, always.

  No dependency on anything in this repo, and no dependency outside
  `org.clojure/clojure` either -- the JSON writer below is twenty lines
  because this shape needs twenty lines, not because a library would be
  wrong. The contract is `docs/formats.md`, \"The event log\"."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

;; ---- a JSON writer, for exactly the value shapes this record uses ----

(defn- json-escape
  [s]
  (str/escape s {\" "\\\"" \\ "\\\\" \newline "\\n" \return "\\r" \tab "\\t"}))

(defn- json-scalar
  [v]
  (cond
    (nil? v) "null"
    (string? v) (str \" (json-escape v) \")
    (integer? v) (str v)
    (boolean? v) (str v)
    :else (throw (ex-info "unexpected scalar in a record" {:value v}))))

(defn- json-object
  "kvs is a flat vector of alternating key/value -- a VECTOR on purpose,
  so key order is the emitter's decision and not a map's."
  [kvs]
  (str "{"
       (str/join "," (for [[k v] (partition 2 kvs)]
                       (str (json-scalar k) ":" (json-scalar v))))
       "}"))

;; ---- the mapping ----

(def ^:private opens :admission)
(def ^:private closes :discharge)

(defn- subject
  "`:participants` is the universal key; `:active-mrn` is not (it is
  absent from `:bed-swap`, `:merge` and `:step-rejected`). The first
  participant is the event's primary subject."
  [event]
  (:patient-id (first (:participants event))))

(defn- fold-event
  "One event into the accumulator. Decision 1 lives here: `:admission`
  opens an encounter keyed by patient, `:discharge` closes the open one.
  Everything else is counted by kind and skipped."
  [acc event]
  (let [kind (:event event)
        pid (subject event)]
    (cond
      (= kind opens)
      (as-> acc a
        (assoc-in a [:open pid] {:patient-id pid
                                 :mrn (:active-mrn event)
                                 :ward (get-in event [:location :ward])
                                 :bed (get-in event [:location :bed])
                                 :admitted-t (:t event)})
        ;; Decision 2: count every field this format cannot carry, by
        ;; PRESENCE on the source event -- not by truthiness, which
        ;; would silently report a present-but-false field as absent.
        (reduce (fn [m [label present?]]
                  (cond-> m present? (update-in [:dropped label] (fnil inc 0))))
                a
                [[:attending (contains? event :attending)]
                 [:reason (contains? event :reason)]
                 [:placement (some? (get-in event [:location :placement]))]
                 [:warm-up (contains? event :warm-up)]])
        (update a :warm-up-true (if (:warm-up event) inc identity)))

      (= kind closes)
      (if-let [open (get-in acc [:open pid])]
        (-> acc
            (update :records conj (assoc open :discharged-t (:t event)))
            (update :open dissoc pid))
        ;; A discharge with no admission in this log is not an error the
        ;; emitter may hide: the run's window can open mid-stay exactly
        ;; as it can close mid-stay.
        (update acc :orphan-discharges inc))

      :else
      (update-in acc [:untranslated kind] (fnil inc 0)))))

(defn- record->json
  "Decision 3 lives here: the two `*_datetime` fields the target's own
  schema wants are `null`, because the log carries no absolute time and
  will not be guessed at."
  [{:keys [patient-id mrn ward bed admitted-t discharged-t]}]
  (json-object
   ["record_type" "encounter"
    "patient_id" patient-id
    "mrn" mrn
    "ward" ward
    "bed" bed
    "admitted_t_seconds" admitted-t
    "discharged_t_seconds" discharged-t
    "length_of_stay_seconds" (when discharged-t (- discharged-t admitted-t))
    "admission_datetime" nil
    "discharge_datetime" nil
    "status" (if discharged-t "closed" "open")]))

(defn -main
  [& [path]]
  (when-not path
    (binding [*out* *err*]
      (println "usage: bin/example-custom-emitter-jsonl <events.edn>"))
    (System/exit 2))
  (let [events (edn/read-string (slurp path))
        {:keys [records open untranslated dropped warm-up-true orphan-discharges]}
        (reduce fold-event
                {:records [] :open {} :untranslated {} :dropped {}
                 :warm-up-true 0 :orphan-discharges 0}
                events)
        ;; Encounters still open when the run's window ended are real
        ;; encounters and are emitted as such.
        all (sort-by (juxt :admitted-t :patient-id)
                     (into records (vals open)))
        skipped (reduce + 0 (vals untranslated))]
    (run! (comp println record->json) all)
    (binding [*out* *err*]
      (println (format "# %d events in, %d encounters out (%d closed, %d still open at end of run)"
                       (count events) (count all)
                       (count (filter :discharged-t all))
                       (count (remove :discharged-t all))))
      (println (format "# %d events untranslated, by kind: %s"
                       skipped
                       (if (seq untranslated)
                         (str/join ", " (for [k (sort (keys untranslated))]
                                          (str k "=" (get untranslated k))))
                         "none")))
      (println (format "# dropped fields this format cannot carry: %s"
                       (if (seq dropped)
                         (str/join ", " (for [k (sort (keys dropped))]
                                          (str k " on " (get dropped k) " record(s)")))
                         "none")))
      (println (format "# of those, %d record(s) came from inside the warm-up window (:warm-up true) -- filter on this if your consumer cares"
                       warm-up-true))
      (println (format "# %d discharge(s) with no admission in this log (run window opened mid-stay)"
                       orphan-discharges))
      (println "# admission_datetime/discharge_datetime are null: the log carries no absolute time (:t is seconds from run start). Anchoring needs a reference date, which is an emit-time input, not a log fact."))))
