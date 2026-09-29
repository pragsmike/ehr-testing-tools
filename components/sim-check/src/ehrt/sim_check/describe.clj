(ns ehrt.sim-check.describe
  "`ehrt sim describe` (ADR-0183): what a corpus PROVES, set beside what
  its configuration merely made POSSIBLE. A deterministic, versioned,
  machine-readable report -- identity, kind counts and subject
  participation, temporal range, configured-versus-observed families
  (a family configured with zero witnesses included), and four named
  relationship predicates, each with small exact witnesses.

  THE RULE THE WHOLE REPORT OBEYS: it never contains a claim the log
  does not carry. What the input does not say is `:unknown`; what the
  log cannot say even in principle is `:unprovable`, with the reason.
  There is no inferred narrative.

  ONE PASS over `engine/replay`'s records. The accumulators hold counts,
  per-kind and per-module subject sets, a few per-patient facts (last
  opener, last transfer, an unclosed candidate), and at most `k`
  witnesses per row, taken in ascending log index; nothing retains an
  event beyond replay's own records. Two cheap walks of the RAW log sit
  beside it: `check/merges-forward`, which is how the checker resolves a
  result's subject through merges (a merge may share a `:t` with a
  result logged before it, so an incremental reading would disagree
  with the checker), and the input hash.

  A WITNESS is `{:index i :t .. :patient-id .. :encounter-id ..
  :related {..}}` -- `:index` is the event's log position, which is
  stable (the same log gives the same index) and, since ADR-0181, the
  suffix of every MSH-10 the event lowers to. Absent fields are absent,
  not nil.

  The predicates REUSE `ehrt.sim-check.check`'s own readings of the
  relationships they count (opener/closer sets, the per-encounter
  stamp test, merge resolution), because their contract is that a
  predicate's `:fails` count IS the corresponding invariant's violation
  count on the same log -- `describe-test` asserts that against
  `check-all`. They are not an independent instrument, and do not claim
  to be one."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-check.describe-catalog :as catalog]
            [ehrt.sim-engine.interface :as engine])
  (:import (java.io OutputStreamWriter)
           (java.security DigestOutputStream MessageDigest)
           (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(def describe-version
  "The report's OWN version, independent of the event schema's and the
  manifest's. Semver: a new key or row is minor, a changed meaning or a
  removed key is major."
  "1.0.0")

(def default-witnesses 3)

;; --- identity -------------------------------------------------------------

(defn log-sha256
  "sha256 of the log as `pr` renders it -- the bytes `sim run --format
  ground-truth` writes, before its trailing newline. Streamed into the
  digest, never built as one string, so hashing costs no copy of the
  log."
  [log]
  (let [md (MessageDigest/getInstance "SHA-256")
        null-out (proxy [java.io.OutputStream] [] (write ([_]) ([_ _ _])))]
    (with-open [w (OutputStreamWriter. (DigestOutputStream. null-out md) "UTF-8")]
      (binding [*out* w *print-length* nil *print-level* nil *print-meta* false *print-namespace-maps* false]
        (pr log)))
    (apply str (map #(format "%02x" %) (.digest md)))))

(defn- identity-section
  [log manifest configuration]
  (let [base {:events (count log) :log-sha256 (log-sha256 log)}]
    (if manifest
      (merge base
             {:source :envelope
              :seed (get-in manifest [:seeds :primary])
              :config (:config manifest)
              :event-schema-version (:event-schema-version manifest)
              :generator (select-keys (:generator manifest) [:name :version :sha256])
              :churn-flag (true? (get-in manifest [:invocation :opts :churn]))
              :configured-from (:source configuration)})
      (cond-> (assoc base :source :bare-log
                     :configured-from (or (:source configuration) :none))
        (:config configuration) (assoc :config (:config configuration))))))

;; --- temporal -------------------------------------------------------------

(def ^:private iso-local (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss"))

(defn- iso-at
  "The instant `t` seconds after `reference-date`'s midnight, with the
  run's fixed offset suffixed -- the same anchoring the HL7 emitter
  applies (`hl7-time/reference-instant`)."
  [reference-date utc-offset t]
  (str (.format (.plusSeconds (.atStartOfDay (LocalDate/parse reference-date)) t) iso-local)
       (or utc-offset "+00:00")))

(defn- temporal-section
  [min-t max-t manifest]
  (let [{:keys [reference-date utc-offset]} (:engine-params manifest)]
    (cond-> {:min-t min-t :max-t max-t}
      (and reference-date min-t) (assoc :reference-date reference-date
                                        :min-iso (iso-at reference-date utc-offset min-t)
                                        :max-iso (iso-at reference-date utc-offset max-t)))))

;; --- witnesses ------------------------------------------------------------

(defn- witness
  [{:keys [index event patient-id]} fields]
  (let [enc (when patient-id (check/encounter-id-of event patient-id))
        related (into (sorted-map) (keep (fn [f] (when (contains? event f) [f (get event f)]))) fields)]
    (cond-> {:index index :t (:t event)}
      patient-id (assoc :patient-id patient-id)
      enc (assoc :encounter-id enc)
      (seq related) (assoc :related related))))

(defn- add-witness [ws k w] (if (< (count ws) k) (conj ws w) ws))

;; --- predicates -----------------------------------------------------------

(def ^:private during-encounter-kinds
  "The kinds `order-only-when-admitted` and
  `clinical-content-only-when-admitted` scope to an open stay."
  #{:order-placed :procedure :observation :medication-order :diagnostic-report
    :care-plan-start :immunization})

(defn- predicate-verdicts
  "For one record: `[[predicate holds? related] ...]` for every predicate
  the record is a candidate of. Each reading is the invariant's own."
  [{:keys [log merges last-opener]} {:keys [event before patient-id]}]
  (let [kind (:event event)]
    (cond
      (check/encounter-closers kind)
      [[:same-subject-opener-closer (some? (:encounter before))
        (when-let [o (get last-opener patient-id)] {:opener-index o})]]

      (= :result-available kind)
      (let [target (get log (:order-event-id event))
            belongs? (boolean
                      (and target (= :order-placed (:event target))
                           (some #(check/resolves-through-merges? merges (:patient-id %) patient-id (:t event))
                                 (:participants target))))
            before? (boolean (and target (<= (:t target) (:t event))))
            rel {:order-event-id (:order-event-id event)}]
        [[:order-before-result before? rel]
         [:result-belongs-to-order belongs? rel]])

      (during-encounter-kinds kind)
      [[:during-encounter
        (and (= :admitted (:status before))
             (not (check/carried-encounter-is-not-the-open-one? event before patient-id)))
        nil]]

      :else nil)))

(def predicate-cites
  {:same-subject-opener-closer "discharge-closes-an-open-encounter"
   :order-before-result "result-references-existing-order-and-follows-it-in-time (its temporal half)"
   :result-belongs-to-order "result-references-existing-order-and-follows-it-in-time (its referential half, resolved through merges)"
   :during-encounter "order-only-when-admitted + clinical-content-only-when-admitted (by :encounter-id when stamped, by replay state otherwise)"})

;; --- the pass -------------------------------------------------------------

(defn- patient-ids [event] (into [] (keep :patient-id) (:participants event)))

(defn- step
  [k rows acc {:keys [event patient-id index] :as rec}]
  (let [kind (:event event)
        pids (patient-ids event)
        ctx acc
        acc (-> acc
                (update-in [:by-kind kind :events] (fnil inc 0))
                (update-in [:by-kind kind :subjects] (fnil into #{}) pids)
                (update :subjects into pids)
                (update :min-t #(if % (min % (:t event)) (:t event)))
                (update :max-t #(if % (max % (:t event)) (:t event))))
        ;; families
        acc (reduce
             (fn [acc [i row]]
               (let [acc (if-let [closed (and (:closed-by row) ((:closed-by row) ctx rec))]
                           (update-in acc [:open i] dissoc closed)
                           acc)]
                 (if ((:observed-by row) ctx rec)
                   (if (:closed-by row)
                     (assoc-in acc [:open i patient-id] (witness rec (:witness-fields row)))
                     (-> acc
                         (update-in [:family-counts i] (fnil inc 0))
                         (update-in [:family-witnesses i] (fnil add-witness []) k
                                    (witness rec (:witness-fields row)))))
                   acc)))
             acc rows)
        ;; modules
        acc (if-let [m (get-in event [:citation :module])]
              (-> acc
                  (update-in [:modules m :events] (fnil inc 0))
                  (update-in [:modules m :subjects] (fnil into #{}) pids)
                  (update-in [:modules m :witnesses] (fnil add-witness []) k (witness rec [])))
              acc)
        ;; predicates
        acc (reduce (fn [acc [p holds? related]]
                      (let [side (if holds? :holds :fails)
                            w (cond-> (witness rec []) related (assoc :related (into (sorted-map) related)))]
                        (-> acc
                            (update-in [:predicates p side] (fnil inc 0))
                            (update-in [:predicates p :witnesses side] (fnil add-witness []) k w))))
                    acc (predicate-verdicts ctx rec))]
    ;; per-patient facts, updated AFTER this record's rows read them
    (cond-> acc
      (check/encounter-openers kind) (assoc-in [:last-opener patient-id] index)
      (= :transfer kind) (assoc-in [:last-transfer patient-id] index))))

(defn- family-section
  [rows configuration acc]
  (let [fixed (map-indexed
               (fn [i row]
                 (let [ws (if (:closed-by row)
                            (->> (vals (get-in acc [:open i])) (sort-by :index) (take (:k acc)) vec)
                            (get-in acc [:family-witnesses i] []))
                       n (if (:closed-by row)
                           (count (get-in acc [:open i]))
                           (get-in acc [:family-counts i] 0))
                       configured ((:configured-by row) configuration)]
                   (cond-> {:group (:group row) :family (:family row) :cites (:cites row)
                            :configured configured :observed n :witnesses ws}
                     (= :unknown configured)
                     (assoc :configured-reason
                            (if (nil? configuration)
                              "the input names no configuration"
                              "the setting that decides this is not recorded in the input")))))
               rows)
        modules (for [row (catalog/module-rows configuration (keys (:modules acc)))
                      :let [{:keys [events subjects witnesses]} (get-in acc [:modules (:family row)])]]
                  (cond-> (assoc row :observed (or events 0) :subjects (count subjects)
                                 :witnesses (or witnesses []))
                    (nil? configuration) (assoc :configured-reason "the input names no configuration")))
        pathways (catalog/pathway-rows configuration)]
    (vec (concat fixed modules pathways))))

(defn- canonical
  "Every map sorted by key, so equal inputs print equal bytes whatever
  size a map grew to (an array-map past eight keys becomes a hash-map,
  whose print order is not a contract)."
  [x]
  (walk/postwalk #(if (and (map? %) (not (record? %))) (into (sorted-map) %) %) x))

(defn describe
  "The report over `log`. `opts`:
    :manifest       the run manifest when the input was an envelope
    :configuration  `{:source .. :opts .. :churn-profile .. :config ..}`,
                    or nil when nothing names the configuration
    :witnesses      witnesses per row (default 3)
    :records        `engine/replay`'s records when the caller has them"
  ([log] (describe log {}))
  ([log {:keys [manifest configuration witnesses records]}]
   (let [k (or witnesses default-witnesses)
         log (vec log)
         rows catalog/rows
         indexed-rows (vec (map-indexed vector rows))
         records (or records (engine/replay log))
         init {:k k :log log :merges (check/merges-forward log)
               :subjects #{} :by-kind {} :last-opener {} :last-transfer {}}
         acc (reduce (fn [acc [i rec]] (step k indexed-rows acc (assoc rec :index i)))
                     init (map-indexed vector records))
         by-kind (into (sorted-map)
                       (map (fn [[kind {:keys [events subjects]}]]
                              [kind {:events events :subjects (count subjects)}]))
                       (:by-kind acc))
         predicates (into (sorted-map)
                          (map (fn [p]
                                 (let [{:keys [holds fails witnesses]} (get-in acc [:predicates p])]
                                   [p {:cites (predicate-cites p)
                                       :holds (or holds 0) :fails (or fails 0)
                                       :witnesses {:holds (get witnesses :holds [])
                                                   :fails (get witnesses :fails [])}}])))
                          (keys predicate-cites))]
     (canonical
      {:describe-version describe-version
       :identity (identity-section log manifest configuration)
       :counts {:events (count log) :subjects (count (:subjects acc)) :by-kind by-kind}
       :temporal (temporal-section (:min-t acc) (:max-t acc) manifest)
       :families (family-section rows configuration acc)
       :predicates predicates}))))

;; --- the human view ---------------------------------------------------------

(defn- fam-line
  [{:keys [group family configured observed subjects witnesses]}]
  (format "  %-10s %-36s configured %-9s observed %s%s%s"
          (name group) (if (keyword? family) (name family) family) (name configured)
          (if (keyword? observed) (name observed) observed)
          (if subjects (str " (" subjects " subjects)") "")
          (if (seq witnesses) (str "  e.g. #" (str/join " #" (map :index witnesses))) "")))

(defn render-text
  "A concise human view of the SAME report map -- nothing here is
  computed that the map does not already carry."
  [report]
  (let [{:keys [identity counts temporal families predicates]} report]
    (str/join
     "\n"
     (concat
      [(str "describe " (:describe-version report) " -- " (name (:source identity))
            ", " (:events identity) " events, log sha256 " (:log-sha256 identity))]
      (when (:seed identity) [(str "seed " (:seed identity) ", schema " (:event-schema-version identity)
                                   ", generator " (get-in identity [:generator :version])
                                   ", churn flag " (:churn-flag identity))])
      [(str "configured from: " (name (:configured-from identity)))
       (str "subjects " (:subjects counts) "; t " (:min-t temporal) ".." (:max-t temporal)
            (when (:min-iso temporal) (str " (" (:min-iso temporal) " .. " (:max-iso temporal) ")")))
       ""
       "kinds:"]
      (for [[kind {:keys [events subjects]}] (:by-kind counts)]
        (format "  %-22s %8d events %7d subjects" (name kind) events subjects))
      ["" "families:"]
      (map fam-line families)
      ["" "predicates:"]
      (for [[p {:keys [holds fails]}] predicates]
        (format "  %-28s holds %d  fails %d" (name p) holds fails))))))
