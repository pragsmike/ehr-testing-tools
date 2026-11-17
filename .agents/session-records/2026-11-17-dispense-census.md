# Medication-order census by class: what a chronic-refill rule would yield (measurement only)

2026-11-17. Base `24e19cfd`. Ceremony: R30, from the session prompt
(`.agents/prompts/2026-11-17-dispense-census.md`), which stated no
prepare-only mode. Measurement only: no source, config, schema, test or
consumer-doc change; this record is the output. No local `make test`
owed (AGENTS.md 22).

`bin/preflight` exited **1** with one finding, disclosed: two RED runs
(`6e38bc25`, `68d972d2`) are still among the last five on main. They
are the NIST-repository failure closed at `e5cd16f8`; the three runs
since are green. Everything else OK.

Author ruling, verbatim, 2026-11-17: "b" -- `chronic: true` orders
refill at a fixed period until `:medication-end` or the horizon, no
draw, one authored-provisional constant; every other order fills once.
Measuring before building is the channel's sequencing, not a ruling.

## The answer

**Rule B produces no multi-fill order in any of the four logs.** None
of them holds a single `chronic: true` medication order, in the log or
before the horizon. Under rule B every fill in these roots is a
one-per-order fill, and rule B is indistinguishable from rule A.

## Step 1: the class map

`ehrt.patient-simulator.gmf/load-module` over every `.json` under
`components/sim/resources/sim/modules`, the module id being the path
under that directory without `.json` (the call-path `ehrt.sim.run`
resolves, and the string a citation's `:module` carries). Key
`[module-id state-keyword]`, the state keyword as the loader returned
it. No slug was re-derived.

- 66 modules, all `:ok`. **134 entries**, one per MedicationOrder
  state.
- 37 `:chronic`: `medications/maintenance_inhaler` 21,
  `medications/emergency_inhaler` 11, `metabolic_syndrome/medications`
  4, `osteoporosis` 1.
- 13 `:administration`: `sepsis` 4, `veteran_lung_cancer` 4,
  `colorectal_cancer` 2, `veteran_prostate_cancer` 2, `uti/abx_tx` 1.
- 83 `:has-prescription`. No state is both chronic and administration.
- The two inhaler submodules are called from `asthma.json` only;
  `metabolic_syndrome/medications` from `metabolic_syndrome_care.json`
  only; `osteoporosis` holds its own state.

## Step 2: the four logs

All `bin/ehrt sim run ... --format ground-truth`, stdout to a scratch
file.

| log | invocation after `sim run` | events | sha-256 of stdout |
| --- | --- | --- | --- |
| dense-7500, 750 | `--seed 20260824 --patients 750 --churn --config demos/scenarios/dense-7500/config.edn` | 33,296 | `eb78147ace51f746606595c812c95cafd014fa3a8228cfbace51e29d0b8d636b` |
| dense-7500, 7,500 | `--seed 20260824 --patients 7500 --churn --config demos/scenarios/dense-7500/config.edn` | 167,715 | `0ad0c3419c7f37d6dc1213422d8c2c32c8d5d77a3eb5833756bd7bde0e07c363` |
| clinic-decade | `--seed 20260807 --patients 200 --config demos/scenarios/clinic-decade/config.edn` | 1,569 | `a0ca96e0371b630c1609ccbb9172ca2cfefe93ba5f1361646f44a18ad263edcf` |
| ed-tuesday | `--seed 20260811 --patients 100 --reference-date 2026-08-11 --churn --config demos/scenarios/ed-tuesday/config.edn` | 1,267 | `dfa00d70ada5d12155d1f24d996348e13b9f9151b325ab2a47900239885d5e20` |

Against the tree's own figures, all four match:

- 750 and 7,500: `demos/scenarios/dense-7500/figures.edn` `:asserted`
  holds 33,296 and 167,715.
- clinic-decade: its README's "1,569 ground-truth events".
- ed-tuesday: its README's "1,267 ground-truth events". The README's
  digest `c8be0ef2...c57a` is of `events.edn`, which `corpus generate
  sim` writes; this session ran that command too, got that digest, and
  found the file VALUE-equal to the `sim run` stdout (`=` over the two
  parsed vectors) and not byte-equal. So the stdout digests above are
  this session's own and match nothing recorded in the tree.

No digest for the other three exists in the tree to compare with.

## Step 3: the census

Top-level vector only for in-log counts; `:registered` ->
`:pre-horizon-facts` only for pre-horizon counts. Classes: chronic,
administration, other; "no module state" is an order whose citation is
not in the class map.

### (a) in-log `:medication-order` by class

| log | orders | chronic | administration | other | no module state |
| --- | --- | --- | --- | --- | --- |
| dense 750 | 492 | 0 | 0 | 5 | 487 |
| dense 7,500 | 4,986 | 0 | 0 | 79 | 4,907 |
| clinic-decade | 13 | 0 | 0 | 13 | 0 |
| ed-tuesday | 0 | 0 | 0 | 0 | 0 |

Each row sums to the log's `:medication-order` kind count. The "no
module state" orders are all two citations, `{:module
"dense-inpatient" :state :antibiotic-start}` and `:analgesia-start`.
They are explained: `dense-inpatient` is a hand-authored pathway in
`demos/scenarios/dense-7500/config.edn` (104-148) whose two
`:medication-order` steps carry authored citations. No vendored module
stands behind them, so they have no `chronic`, `administration` or
`prescription` to read. Each is closed by an authored `:medication-end`
in the same stay: order-to-end span min 0.00, median 0.09, max 0.26
days at 7,500 (n 4,888); median 0.11, max 0.25 at 750 (n 480).

### (b) pre-horizon `:medication-order` facts by class

| log | facts | chronic | administration | other | no module state |
| --- | --- | --- | --- | --- | --- |
| dense 750 | 143 | 0 | 0 | 143 | 0 |
| dense 7,500 | 881 | 0 | 0 | 881 | 0 |
| clinic-decade | 158 | 0 | 3 | 155 | 0 |
| ed-tuesday | 17 | 0 | 0 | 17 | 0 |

clinic-decade's three administration facts are `sepsis`.

### (c) chronic orders: closure and span

Zero chronic orders in every log, in-log and pre-horizon, so every
cell is zero and no span exists: closed by an in-log end 0, closed by
a pre-horizon end 0, still open at the last `:t` 0.

### (d) distinct subjects holding a chronic order

Zero in every log, in-log and pre-horizon.

### Why the chronic states are never reached

- **dense-7500** lists `metabolic_syndrome_care`, and it walks: 204
  in-log events cite it at 7,500 (26 at 750). All of them are
  `:wellness-encounter` / `:end-wellness-encounter` (102 visits and
  their ends). Nothing cites `metabolic_syndrome/medications`, in-log
  or pre-horizon.
- **clinic-decade** lists `asthma` at weight 10. No event and no
  pre-horizon fact cites `asthma` or either inhaler submodule.
- **ed-tuesday** lists none of the three.

Why those walks stop where they do was not investigated.

## Step 4: the projection

Arithmetic over step 3 only. Fills per chronic order = 1 + floor(span
/ P); every other non-administration order = 1 (the authored
`dense-inpatient` orders included); administration = 0. With no
chronic order anywhere, P drops out and the pre-horizon column adds
nothing.

| log | P=30, in-log | P=30, + pre-horizon chronic | P=90, in-log | P=90, + pre-horizon chronic |
| --- | --- | --- | --- | --- |
| dense 750 (33,296) | 492, +1.48% | 492, +1.48% | 492, +1.48% | 492, +1.48% |
| dense 7,500 (167,715) | 4,986, +2.97% | 4,986, +2.97% | 4,986, +2.97% | 4,986, +2.97% |
| clinic-decade (1,569) | 13, +0.83% | 13, +0.83% | 13, +0.83% | 13, +0.83% |
| ed-tuesday (1,267) | 0, +0.00% | 0, +0.00% | 0, +0.00% | 0, +0.00% |

- dense 750: rule B produces no order with more than one fill.
- dense 7,500: rule B produces no order with more than one fill.
- clinic-decade: rule B produces no order with more than one fill.
- ed-tuesday: rule B produces no fill at all; the log has no in-log
  medication order.

Two things the table does not say. Nearly all of dense's projected fills
(487 of 492; 4,907 of 4,986) would be fills of orders that open and
close inside one inpatient stay, within about six hours. And the
orders a module does author mostly sit before the horizon: 881 facts
against 79 in-log at 7,500, 158 against 13 in clinic-decade, 17
against 0 in ed-tuesday; a slice that skips pre-horizon orders skips
those.

## Where the tree differed from the prompt

1. **"Every in-log `:medication-order` carries a citation naming
   exactly one MedicationOrder state" is false in dense-7500.** 98% of
   its in-log orders cite an authored pathway (3(a)). It holds in the
   module-mix trace, in clinic-decade, and vacuously in ed-tuesday.
2. **Three of the four "gated invocations" are not `--format
   ground-truth`.** The 7,500 cell, clinic-decade and ed-tuesday are
   gated as `corpus generate sim ... --out-dir`; only the 750 cell is a
   `sim run --format ground-truth`. This session ran `sim run` with
   each gated command's seed, patient count, flags and config, and
   checked the result against the gated figure (step 2). One defensible
   reading; disclosed rather than halted on.
3. Everything else held, including the module-mix trace: 249 events,
   11 in-log orders (all `sinusitis`, non-chronic), 83 pre-horizon
   order facts (non-chronic), no in-log `:medication-end`.

## Findings

- **A second pre-horizon `:medication-end` can reference the first
  order, not its own.** module-mix carries 164 pre-horizon
  `:medication-end` facts for 83 pre-horizon orders. In one
  registration two `sinusitis :prescribe-antibiotic` orders are each
  followed by an `:end-antibiotic` fact, and both ends carry
  `:references 17`. Seen while pairing ends to orders; cause not
  investigated; it moved no figure above, since no chronic fact exists.
- The census pairs a pre-horizon end to an order by the MedicationEnd
  state's own target (state name or attribute), latest open order
  first, because a fact's `:references` is a trajectory index the log
  does not carry. End facts that found no open order: 15 (750), 75
  (7,500), 19 (clinic-decade), 2 (ed-tuesday), 81 (module-mix).

## The script

Run from the workspace root as `clojure -M:dev census.clj NAME LOG.edn
...`. Kept here whole; it lives nowhere else in the tree.

```clojure
(require '[ehrt.patient-simulator.gmf :as gmf]
         '[clojure.java.io :as io] '[clojure.string :as str] '[clojure.edn :as edn])

(def root (io/file "components/sim/resources/sim/modules"))

(def modules
  ;; id is the path under sim/modules without ".json" -- the call-path
  ;; ehrt.sim.run resolves, and what a citation's :module carries.
  (for [^java.io.File f (sort (file-seq root))
        :when (str/ends-with? (.getName f) ".json")
        :let [id (-> (.getPath f) (subs (inc (count (.getPath root)))) (str/replace #"\.json$" ""))]]
    [id (gmf/load-module id (slurp f))]))

(def states
  (for [[id r] modules, [k s] (:states (:payload r))] [[id k] s]))

(def class-map
  (into {} (for [[k s] states :when (= :medication-order (:type s))]
             [k (cond-> #{}
                  (true? (:chronic s)) (conj :chronic)
                  (true? (:administration s)) (conj :administration)
                  (contains? s :prescription) (conj :has-prescription))])))
(def order-attr (into {} (for [[k s] states :when (= :medication-order (:type s))] [k (:assign-to-attribute s)])))
(def end-target (into {} (for [[k s] states :when (= :medication-end (:type s))]
                           [k (select-keys s [:medication-order :referenced-by-attribute])])))

(defn klass [c] (let [fs (class-map c ::none)]
                  (cond (= fs ::none) :unmatched (:chronic fs) :chronic (:administration fs) :administration :else :other)))
(defn ckey [citation] [(:module citation) (:state citation)])
(defn subject [e] (:patient-id (first (filter #(= :subject (:role %)) (:participants e)))))
(defn days [secs] (/ secs 86400.0))
(defn stats [xs] (when (seq xs) (let [v (vec (sort xs)) n (count v)]
                                  {:n n :min (first v) :max (peek v)
                                   :median (if (odd? n) (v (quot n 2)) (/ (+ (v (quot n 2)) (v (dec (quot n 2)))) 2.0))})))
(defn fmt [x] (if (float? x) (format "%.1f" x) (str x)))

(println "modules" (count modules) "not-ok" (vec (for [[id r] modules :when (not= :ok (:status r))] [id (:status r)])))
(println "class-map entries" (count class-map)
         "chronic" (count (filter :chronic (vals class-map)))
         "administration" (count (filter :administration (vals class-map)))
         "has-prescription" (count (filter :has-prescription (vals class-map)))
         "chronic+administration" (count (filter #(and (:chronic %) (:administration %)) (vals class-map))))
(println "chronic by module" (into (sorted-map) (frequencies (for [[[m _] fs] class-map :when (:chronic fs)] m))))
(println "administration by module" (into (sorted-map) (frequencies (for [[[m _] fs] class-map :when (:administration fs)] m))))

(defn census [nm path]
  (let [log (edn/read-string (slurp path))
        n (count log)
        last-t (apply max (map :t log))
        idx (map-indexed vector log)
        orders (for [[i e] idx :when (= :medication-order (:event e))]
                 {:i i :t (:t e) :subj (subject e) :c (ckey (:citation e)) :k (klass (ckey (:citation e)))})
        ends (for [[i e] idx :when (= :medication-end (:event e))] (assoc e :i i))
        end-by-order (into {} (for [e ends :when (:order-event-id e)] [(:order-event-id e) e]))
        bad-ids (count (for [e ends :when (:order-event-id e)
                             :when (not= :medication-order (:event (nth log (:order-event-id e) nil)))] e))
        ;; pre-horizon: walk each :registered's facts in order
        regs (for [[i e] idx :when (and (= :registered (:event e)) (seq (:pre-horizon-facts e)))]
               (loop [fs (:pre-horizon-facts e) j 0 open [] closed [] unres 0]
                 (if-let [f (first fs)]
                   (case (:event f)
                     :medication-order
                     (recur (rest fs) (inc j)
                            (conj open {:reg i :j j :t (:t e) :subj (subject e) :c (ckey (:citation f)) :k (klass (ckey (:citation f)))})
                            closed unres)
                     :medication-end
                     (let [{mo :medication-order attr :referenced-by-attribute} (end-target (ckey (:citation f)))
                           hit? (fn [o] (or (and mo (= (:c o) [(:module (:citation f)) mo]))
                                            (and attr (= attr (order-attr (:c o))))))
                           pos (last (keep-indexed (fn [p o] (when (hit? o) p)) open))]
                       (if pos
                         (recur (rest fs) (inc j) (into (subvec open 0 pos) (subvec open (inc pos)))
                                (conj closed (assoc (open pos) :closed :pre-horizon)) unres)
                         (recur (rest fs) (inc j) open closed (inc unres))))
                     (recur (rest fs) (inc j) open closed unres))
                   {:open open :closed closed :unres unres})))
        pre-closed (mapcat :closed regs)
        pre-open0 (vec (mapcat :open regs))
        ;; in-log ends whose order is not a log event: close the latest
        ;; still-open pre-horizon order of that subject and citation
        [pre-open pre-inlog-closed unres-inlog]
        (reduce (fn [[open done unres] e]
                  (let [c (ckey (:order-citation e)) s (subject e)
                        pos (last (keep-indexed (fn [p o] (when (and (= s (:subj o)) (= c (:c o))) p)) open))]
                    (if pos
                      [(into (subvec open 0 pos) (subvec open (inc pos)))
                       (conj done (assoc (open pos) :closed :in-log :end-t (:t e))) unres]
                      [open done (inc unres)])))
                [pre-open0 [] 0]
                (remove :order-event-id ends))
        pre-all (concat pre-closed pre-inlog-closed pre-open)
        chron-in (for [o orders :when (= :chronic (:k o))]
                   (let [e (end-by-order (:i o))]
                     (assoc o :closed (if e :in-log :open) :span (days (- (if e (:t e) last-t) (:t o))))))
        chron-pre (for [o pre-all :when (= :chronic (:k o))]
                    (assoc o :span (when (not= :pre-horizon (:closed o)) (days (- (or (:end-t o) last-t) (:t o))))))
        fills (fn [p o] (inc (long (Math/floor (/ (:span o) p)))))
        other-in (count (filter #(#{:other :unmatched} (:k %)) orders))
        proj (fn [p] (let [in (+ other-in (reduce + (map #(fills p %) chron-in)))
                           pre (reduce + (map #(fills p %) (filter :span chron-pre)))]
                       {:in-log in :in-log-pct (* 100.0 (/ in n))
                        :with-pre (+ in pre) :with-pre-pct (* 100.0 (/ (+ in pre) n))
                        :multi-in (count (filter #(> (fills p %) 1) chron-in))
                        :multi-pre (count (filter #(> (fills p %) 1) (filter :span chron-pre)))}))]
    (println "\n==" nm "events" n "last-t-days" (fmt (days last-t)))
    (println "kind counts" (select-keys (frequencies (map :event log)) [:registered :medication-order :medication-end]))
    (println "(a) in-log orders by class" (into (sorted-map) (frequencies (map :k orders)))
             "sum" (count orders) "unmatched" (vec (distinct (map :c (filter #(= :unmatched (:k %)) orders)))))
    (println "    in-log by module" (into (sorted-map) (frequencies (map (comp first :c) orders))))
    (println "(b) pre-horizon orders by class" (into (sorted-map) (frequencies (map :k pre-all)))
             "sum" (count pre-all) "unmatched" (vec (distinct (map :c (filter #(= :unmatched (:k %)) pre-all)))))
    (println "    pre-horizon by module" (into (sorted-map) (frequencies (map (comp first :c) pre-all))))
    (println "    pre-horizon ends unresolved (in facts)" (reduce + (map :unres regs))
             "in-log ends: total" (count ends) "with order-event-id" (count end-by-order)
             "bad order-event-id" bad-ids "not matched to a pre-horizon order" unres-inlog)
    (println "(c) chronic in-log" (count chron-in) (frequencies (map :closed chron-in))
             "span-days" (some-> (stats (map :span chron-in)) (update-vals fmt)))
    (println "    chronic in-log by state" (into (sorted-map) (frequencies (map :c chron-in))))
    (println "    chronic pre-horizon" (count chron-pre) (frequencies (map #(or (:closed %) :open) chron-pre))
             "span-days-from-registration (not closed pre-horizon)" (some-> (stats (keep :span chron-pre)) (update-vals fmt)))
    (println "    chronic pre-horizon by state" (into (sorted-map) (frequencies (map :c chron-pre))))
    (println "(d) distinct subjects with a chronic order: in-log" (count (distinct (map :subj chron-in)))
             "pre-horizon" (count (distinct (map :subj chron-pre)))
             "either" (count (distinct (map :subj (concat chron-in chron-pre)))))
    (doseq [p [30 90]]
      (let [r (proj p)]
        (println (format "P=%d in-log: %d dispenses (+%.2f%%), orders>1 fill: %d | with pre-horizon chronic: %d (+%.2f%%), pre-horizon orders>1 fill: %d"
                         p (:in-log r) (:in-log-pct r) (:multi-in r) (:with-pre r) (:with-pre-pct r) (:multi-pre r)))))))

(doseq [[nm path] (partition 2 *command-line-args*)] (census nm path))
```

## Close

