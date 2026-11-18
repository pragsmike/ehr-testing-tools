# Why listed modules never reach their medications, and a pre-horizon end that references the wrong order (diagnosis only)

2026-11-17. Base `fd31ee2c` (the medication-dispense session's close).
Ceremony: R30, from the session prompt
(`.agents/prompts/2026-11-17-unreached-medications-and-references.md`),
which stated no prepare-only mode. Measurement and reading only: no
source, config, schema, test or consumer-doc change. No local `make
test` owed (AGENTS.md 22).

`bin/preflight` exit 0, no findings; disclosed by it: one run among the
last five still in progress (the previous session's docs-only close
commit), HEAD not tagged stable-*.

Author ruling, verbatim, 2026-11-17: "Go" -- given to "why `asthma` and
`metabolic_syndrome/medications` are never reached, plus the duplicate
`:references` finding. It is measurement only."

The traces below were run at `3f92de1b`, one commit before the base;
the base adds one session-record paragraph and no source.

## The answers

1. **Neither walk stalls.** An attribute nobody set reads as nil, so an
   `is not nil` guard is false and the walk takes the module's own
   default branch. `metabolic_syndrome_care` loops through its wellness
   visit for the whole walk; `asthma` takes its non-atopic branch, where
   2.3% of walks go on to asthma.
2. **"No shipped root holds a chronic order" is false.** The four demo
   logs hold none. The oracle's own `osteoporosis` root holds 156
   in-log chronic orders, and its `asthma` root 12 pre-horizon ones.
3. **The second end's `:references` is wrong**, and the cause is one
   first-match scan. It also makes every later end of that state fire
   when it should not.

## Step 1: where the walks stop

How an unset attribute reads: `attribute-condition-holds?`
(`gmf_interpreter.clj` 474-487) does `(get (:attributes ctx) k)` and,
for `is not nil`, `(some? actual)` (line 486). Nil is false; nothing
blocks, nothing throws.

Traced by rebinding `gmf-interpreter/run-module` around the real
`ehrt.cli.core/sim-run-command`, so every walk is one the scenario
itself made (scripts at the end).

| module | run | walks | where they stop | cause |
| --- | --- | --- | --- | --- |
| `metabolic_syndrome_care` | dense-7500, seed 20260824, 7,500, `--churn` | 268 | all 268 `:horizon-complete` at `:check-ckd` | `Check_Diabetes` reads `diabetes` and `prediabetes`, both nil, so the default goes to `End_Wellness_Encounter`, which loops to `Wellness_Encounter`. The walk cycles until the horizon ends it. |
| `metabolic_syndrome_care` | the oracle's own root config, seed 20260802, 300, horizon 36,500 days | 300 | all 300 the same | the same |
| `asthma` | clinic-decade, seed 20260807, 200 | 30 calls, 15 patients | all 30 `:terminal`, empty trajectory | `Delay_For_Atopy` reads `atopic`, nil, so `Not_Atopic`: 97.7% `Terminal`, 2.3% `Asthma_Incidence`. Fifteen patients at 2.3% is 0.35 expected. |
| `asthma` | the oracle's own root config, seed 20260802, 300, horizon 36,500 days | 300 | 295 `:terminal`; 5 `:horizon-complete` at `:potential-asthma-attack` | the same branch; 5 of 300 drew asthma |

The dense trace: 24,732 trajectory events over the 268 walks, every
one an `:encounter` or `:encounter-end` (12,500 and 12,232), 22,020 of
them pre-horizon; the only attribute any walk set is
`metabolic-syndrome-review`. clinic-decade lists asthma for 15 of 200
patients; each patient's walk is called twice (400 calls, 199 distinct
walks), which is why 30.

Every attribute the two root files read, and who writes it. Readers:
every `condition_type: Attribute` at any depth, plus
`referenced_by_attribute` and `value_attribute`. Writers: every
`SetAttribute` and `Counter` `attribute` and every
`assign_to_attribute`, over all 66 `.json` files under
`components/sim/resources/sim/modules`, parsed.

| module | attribute | written by |
| --- | --- | --- |
| asthma | `atopic` | NOBODY |
| asthma | `smoker` | NOBODY |
| asthma | `asthma_type` | asthma |
| asthma | `asthma_condition` | asthma |
| asthma | `asthma_careplan` | asthma |
| asthma | `maintenance_inhaler` | medications/maintenance_inhaler |
| asthma | `emergency_inhaler` | medications/emergency_inhaler |
| metabolic_syndrome_care | `diabetes` | NOBODY |
| metabolic_syndrome_care | `prediabetes` | NOBODY |
| metabolic_syndrome_care | `obesity` | NOBODY |
| metabolic_syndrome_care | `ckd` | NOBODY |
| metabolic_syndrome_care | `neuropathy` | NOBODY |
| metabolic_syndrome_care | `hypertension` | sleep_apnea only |
| metabolic_syndrome_care | `anemia` | anemia/anemia_sub |
| metabolic_syndrome_care | `diabetes_severity` | metabolic_syndrome_care |
| metabolic_syndrome_care | `hyperglycemia` | metabolic_syndrome_care |
| metabolic_syndrome_care | `hypertriglyceridemia` | metabolic_syndrome_care |

`hypertension` is in effect NOBODY too. An attribute is keyed by the
walk's ROOT module (`(keyword (root-id ctx module-id) (gmf/slug
attribute))`, line 481), and a patient walks one root, so what
`sleep_apnea` writes is never what `metabolic_syndrome_care` reads.

## Step 2: reach

**The roots that do hold chronic orders.** Run at each oracle root's
own config (`digest.clj` 356-364, 438-442, 472-479), orders classed by
the closure's own `chronic: true` states:

| oracle root | chronic states in closure | in-log chronic orders | pre-horizon chronic orders |
| --- | --- | --- | --- |
| `osteoporosis` | 1 | 156 (`:prescribe-bisphosphate`) | 18 |
| `asthma` | 32 | 0 | 12 (8 states) |
| `metabolic-syndrome-care` | 4 | 0 | 0 |

`osteoporosis` has no attribute condition at all and prescribes
inside the visit that diagnoses, so its order lands in the log.

**How module-authored orders split, and what sets it.** From the census
record: dense 7,500 holds 79 in-log against 881 pre-horizon;
clinic-decade 13 against 158. Three things set it, none a rate:

- The walk starts at birth and registration falls at the patient's age
  (`run-module`'s docstring, `gmf_interpreter.clj` 2197-2221), so
  everything before registration is pre-horizon.
- `:module-horizon-days` bounds the walk after registration: 1,825 in
  dense-7500 (`config.edn` 2080), 3,650 in clinic-decade (`config.edn`
  145).
- **The compile stops at the end of the first horizon-phase
  encounter** (`compile_trajectory.clj` 513-529, the `encounter-closed?`
  clause: "compile through the end of the FIRST horizon-phase
  encounter, then stop"). So one walk contributes at most one visit's
  orders to the log, and an end that fires after that visit closes
  never compiles. That is why the census found no module-authored
  in-log `:medication-end` in clinic-decade or module-mix.

**Options for a shipped root holding in-log chronic orders.** Not
chosen among.

- **List `osteoporosis` in a scenario's `:modules`.** No new
  mechanism: the module is vendored and its oracle root already yields
  156. Cost: an engine key changes, so that scenario's whole population
  reshuffles and its gated figures move.
- **Seed the attribute with `:module-initial-attributes`.** The
  mechanism exists (`components/sim/src/ehrt/sim/run.clj` 69-90;
  `docs/consuming-ground-truth.md` 139) and clinic-decade already uses
  it for `total_joint_replacement` (`config.edn` 31). It seeds EVERY
  patient who walks that module with the same value. `asthma` seeded
  `atopic` moves 2.3% to 45% incidence; its orders would still sit in
  childhood, before the horizon, as the oracle root's 12 do.
  `metabolic_syndrome_care` seeded `diabetes` leaves the default
  branch; how far it then gets was not traced. Same reshuffle cost.
- **A fixture module**, the `allergy-fixture.json` precedent
  (`components/patient-simulator/test/ehrt/sim/fixtures/`): a hand-authored chronic
  order inside one wellness visit. Reaches an oracle root, not a
  shipped scenario, since fixtures are on the test path.
- **Vendor the upstream modules that write the attributes.** The tree
  names none of them, so nothing here can say which they are or what
  they cost; a vendoring arc each, by this repo's practice.

## Step 3: the reference

**Verdict: wrong.** Reproduced at the module-mix command (seed 71, 100
patients, `--churn`, `demos/traces/module-mix/config.edn`; 249 events).
The smallest affected walk:

```
10 :medication-order :prescribe-antibiotic  references nil
13 :medication-end   :end-antibiotic        references 10
16 :medication-order :prescribe-antibiotic  references nil
19 :medication-end   :end-antibiotic        references 10
```

The end at 19 closes the order at 16 and says 10.

**The function.** `index-of-citation`, `gmf_interpreter.clj` 1225-1236,
called for a MedicationEnd that names its order by state at 1928-1929.
Its rule is `some` over the trajectory from index 0: the FIRST event of
that module, type and state. Its docstring promises "the event citing"
the state and does not say which when the state has fired more than
once. A MedicationEnd that names its order by attribute (line 1927)
does not go through it.

**A second effect, larger than the first.** `:active-medication` is
`active-onset-condition-holds?` (616-632, wired at 835): the MOST
RECENT matching order is active unless some end references ITS index.
After a second order the ends keep referencing the first, so the
latest order is never seen as ended and the condition stays true for
the rest of the walk. `sinusitis.json`'s `Sinusitis_Ends` routes to
`End_Antibiotic` on exactly that condition, so from the second
prescription on, every later episode emits an end whether or not it
prescribed. That is why module-mix has about two end facts per order.

**Counts.** Each `:medication-end` in each distinct walk, judged
against its own trajectory: "older" means a later order of the same
module and state precedes the end. Every "older" end also references
an order an earlier end had already referenced.

| run | trajectory orders | trajectory ends | pre-horizon ends: older / right | in-horizon ends: older / right |
| --- | --- | --- | --- | --- |
| module-mix | 105 | 208 | 120 / 44 | 36 / 8 |
| dense 750 | 151 | 119 | 25 / 84 | 4 / 6 |
| clinic-decade | 238 | 204 | 22 / 148 (and 1 nil) | 7 / 26 |

All of the "older" ends are `sinusitis :end-antibiotic`. Every other
ending state in the three runs is right: `bronchitis`, `injuries`,
`ear_infections`, `urinary_tract_infections`.

In the logs: module-mix carries 164 pre-horizon `:medication-end`
facts, exactly its 120 + 44, so **120 of its 164 end facts are
affected**. dense 750 carries 108 against 109 in the trajectories, 25
affected. clinic-decade carries 136 against 171; at most 22 are
affected, and the 35-fact difference was not reconciled.

**Can an in-log `:medication-end`'s `:order-event-id` be wrong the same
way? No.** The compile keeps only the referenced order's CITATION
(`medication-end->step`, `compile_trajectory.clj` 323-327), and
`index-of-citation` matched on state, so first and latest order carry
the same citation. The engine then resolves the index itself with
`last-cited-index` (`decide.clj` 1764), the patient's LATEST such
order. In the logs: 480 in-log ends at dense 750, none sharing an
`:order-event-id` with another, none nil; module-mix and clinic-decade
hold no in-log end at all. The in-horizon "older" ends in the table
never reach a log: they fire after the first horizon visit closed.

What a consumer sees today is therefore confined to
`:pre-horizon-facts`: end facts whose `:references` names an order
already ended, and more end facts than orders. Not fixed here.

## Where the prompt's premises differed

1. **"Both walks stall at their first attribute guard ... by
   construction" is wrong for both.** Neither stalls (step 1).
   `asthma`'s miss in clinic-decade is chance at 15 patients; only
   `metabolic_syndrome_care`'s is by construction, and it is a loop.
2. **"No shipped root holds a `chronic: true` medication order" is
   wrong** once the oracle's roots count (step 2). The census read the
   four demo logs only, and its record should be read that way.
3. **The `:references` duplicate is the first of the two candidates**,
   the interpreter's first-match resolution; the census did not misread
   it.
4. "At least 200 walks at the scenario's own seed and config": dense
   gives 268 for `metabolic_syndrome_care`. clinic-decade gives 30
   `asthma` calls and cannot give 200; the oracle root's 300 are the
   second sample, at that root's config and not the scenario's.
5. The attribute lists held: `asthma` reads `atopic`, `asthma_type`,
   `smoker` and four more; `metabolic_syndrome_care` reads the four
   named and six more. Upstream's writer modules are not named anywhere
   in this tree, so that half was not checked.

## Findings

- **`index-of-citation` returns the first match, and
  `:active-medication` depends on it.** Step 3. Its two other callers,
  ConditionEnd by state name (1849) and CarePlanEnd by state name
  (1944), apply the same rule; whether any vendored module repeats
  such a state was not measured. **Undecided: needs an author ruling.**
  A fix moves every root that walks `sinusitis`, in its pre-horizon
  facts at least.
- **One walk contributes one visit to the log** (step 2). Any rule
  that fills or refills module-authored orders in the log acts on
  that one visit's orders and nothing later in the walk.

## Scripts

Run from the workspace root. `attrs.py`: `python3 -I attrs.py asthma
metabolic_syndrome_care`. The three Clojure scripts: `clojure -M:dev
<script> SEED PATIENTS churn|nochurn CONFIG` (`oracle-roots.clj` takes
no arguments). Kept here whole; they live nowhere else in the tree.

### attrs.py

```python
import json, sys, pathlib, collections
root = pathlib.Path("components/sim/resources/sim/modules")
files = sorted(root.rglob("*.json"))
reads = collections.defaultdict(set); writes = collections.defaultdict(set)
def mid(f): return str(f.relative_to(root))[:-5]
def walk_cond(c, m):
    if isinstance(c, dict):
        if c.get("condition_type") == "Attribute" and "attribute" in c: reads[c["attribute"]].add(m)
        for v in c.values(): walk_cond(v, m)
    elif isinstance(c, list):
        for v in c: walk_cond(v, m)
for f in files:
    d = json.loads(f.read_text()); m = mid(f)
    for name, s in d.get("states", {}).items():
        t = s.get("type")
        if t == "SetAttribute": writes[s["attribute"]].add(m)
        if t == "Counter": writes[s["attribute"]].add(m)
        for k in ("assign_to_attribute",):
            if k in s: writes[s[k]].add(m)
        for k in ("referenced_by_attribute", "value_attribute"):
            if k in s: reads[s[k]].add(m)
        if isinstance(s.get("reason"), str): pass
        walk_cond(s, m)
print("files", len(files))
for mod in sys.argv[1:]:
    print("==", mod)
    for a in sorted(a for a, ms in reads.items() if mod in ms):
        print("  %-40s writers: %s" % (a, sorted(writes.get(a, [])) or "NONE"))
```

### trace.clj

```clojure
;; Trace every module walk of a real `ehrt sim run`, by rebinding the var
;; the engine calls. clojure -M:dev trace.clj SEED PATIENTS churn|nochurn CONFIG
(require '[ehrt.patient-simulator.gmf-interpreter :as gi] '[ehrt.cli.core :as cli] '[ehrt.kernel.interface :as result])
(def walks (atom []))
(def orig gi/run-module)
(defn traced [& args]
  (let [r (apply orig args)
        [module _rng persona registration-t horizon-end-t] args]
    (swap! walks conj {:module (:id module) :status (:status r) :current (:current r)
                       :age (:age persona) :events (count (:trajectory r))
                       :kinds (frequencies (map :event (:trajectory r)))
                       :pre (count (filter :pre-horizon (:trajectory r)))
                       :attrs (set (keys (:attributes r)))
                       :walk-error (:walk-error r)})
    r))
(let [[seed patients churn config] *command-line-args*
      opts (cond-> {:seed (parse-long seed) :patients (parse-long patients) :config config :format "ground-truth"}
             (= "churn" churn) (assoc :churn true))
      r (with-redefs [gi/run-module traced] (cli/sim-run-command opts))]
  (println "ok?" (result/ok? r) "events" (count (:ground-truth (:payload r))) "walks" (count @walks))
  (println "walks by module" (into (sorted-map) (frequencies (map :module @walks))))
  (doseq [m ["asthma" "metabolic_syndrome_care"]
          :let [ws (filter #(= m (:module %)) @walks)] :when (seq ws)]
    (println "==" m (count ws) "walks")
    (println " status x stopping state" (into (sorted-map-by #(compare (str %1) (str %2))) (frequencies (map (juxt :status :current) ws))))
    (println " trajectory kinds (all walks)" (apply merge-with + (map :kinds ws)))
    (println " pre-horizon events" (reduce + (map :pre ws)) "of" (reduce + (map :events ws)))
    (println " attribute keys ever set" (into (sorted-set) (mapcat :attrs ws)))
    (println " ages" (let [a (sort (map :age ws))] [(first a) (nth a (quot (count a) 2)) (last a)]))
    (println " walk-errors" (frequencies (keep :walk-error ws)))))
```

### oracle-roots.clj

```clojure
(require '[ehrt.patient-simulator.interface :as ps] '[ehrt.patient-simulator.gmf-interpreter :as gi]
         '[ehrt.sim-engine.run :as run] '[clojure.java.io :as io])
(def rc (fn [p] (some-> (io/resource (str "sim/modules/" p ".json")) slurp)))
(def rt (fn [n] (some-> (io/resource (str "sim/modules/lookup_tables/" n)) slurp)))
(defn closure [id file] (:payload (ps/load-closure id (slurp (io/resource (str "sim/modules/" file ".json"))) rc rt)))
(def walks (atom []))
(def orig gi/run-module)
(defn traced [& args]
  (let [r (apply orig args)]
    (swap! walks conj {:status (:status r) :current (:current r) :kinds (frequencies (map :event (:trajectory r)))
                       :attrs (set (keys (:attributes r)))})
    r))
(doseq [[id file] [["asthma" "asthma"] ["metabolic-syndrome-care" "metabolic_syndrome_care"] ["osteoporosis" "osteoporosis"]]]
  (reset! walks [])
  (let [c (closure id file)
        chronic (into #{} (for [[mid m] (:modules c) [k s] (:states m) :when (and (= :medication-order (:type s)) (true? (:chronic s)))] [mid k]))
        gt (with-redefs [gi/run-module traced]
             (:ground-truth (run/run {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                                      :modules [c] :module-assignment [{:module-id id :weight 1}] :module-horizon-days 36500})))
        ck (fn [e] [(:module (:citation e)) (:state (:citation e))])
        in (filter #(= :medication-order (:event %)) gt)
        pre (for [e gt f (:pre-horizon-facts e) :when (= :medication-order (:event f))] f)]
    (println "==" id "closure modules" (sort (keys (:modules c))) "chronic states in closure" (count chronic))
    (println " events" (count gt) "walks" (count @walks) "status x stop" (into (sorted-map-by #(compare (str %1) (str %2))) (frequencies (map (juxt :status :current) @walks))))
    (println " trajectory kinds" (apply merge-with + (map :kinds @walks)))
    (println " attrs set" (into (sorted-set) (mapcat :attrs @walks)))
    (println " in-log orders" (count in) "chronic" (count (filter (comp chronic ck) in))
             "| pre-horizon orders" (count pre) "chronic" (count (filter (comp chronic ck) pre)))
    (println " chronic in-log by state" (frequencies (map ck (filter (comp chronic ck) in))))
    (println " chronic pre by state" (frequencies (map ck (filter (comp chronic ck) pre))))))
```

### refs.clj

```clojure
;; Medication-end references, judged against the trajectory itself.
;; clojure -M:dev refs.clj SEED PATIENTS churn|nochurn CONFIG
(require '[ehrt.patient-simulator.gmf-interpreter :as gi] '[ehrt.cli.core :as cli] '[ehrt.kernel.interface :as result])
(def walks (atom #{}))
(def orig gi/run-module)
(defn traced [& args]
  (let [r (apply orig args) [module] args]
    (swap! walks conj {:module (:id module) :persona (nth args 2) :trajectory (vec (:trajectory r))})
    r))
(defn judge
  "Per :medication-end: :right when :references is the latest preceding
  order citing the same module/state as the order it references (or as
  no order at all when nil); :older when a LATER order of that same
  module/state precedes the end; :nil when it carries no reference."
  [traj]
  (for [[i e] (map-indexed vector traj) :when (= :medication-end (:event e))
        :let [r (:references e) o (when r (nth traj r nil))
              latest (when o (last (keep-indexed (fn [j x] (when (and (< j i) (= :medication-order (:event x))
                                                                       (= (:module o) (:module x)) (= (:state o) (:state x))) j))
                                                 traj)))
              prior-ends (when r (count (filter (fn [[j x]] (and (< j i) (= :medication-end (:event x)) (= r (:references x))))
                                                (map-indexed vector traj))))]]
    {:i i :pre (boolean (:pre-horizon e)) :state [(:module e) (:state e)] :references r
     :verdict (cond (nil? r) :nil (not= r latest) :older (pos? prior-ends) :repeat :else :right)
     :already-ended (boolean (and r (pos? prior-ends)))}))
(let [[seed patients churn config] *command-line-args*
      opts (cond-> {:seed (parse-long seed) :patients (parse-long patients) :config config :format "ground-truth"}
             (= "churn" churn) (assoc :churn true))
      r (with-redefs [gi/run-module traced] (cli/sim-run-command opts))
      gt (:ground-truth (:payload r))
      js (for [w @walks j (judge (:trajectory w))] (assoc j :module (:module w)))]
  (println "ok?" (result/ok? r) "events" (count gt) "distinct walks" (count @walks))
  (println "trajectory :medication-order" (count (for [w @walks e (:trajectory w) :when (= :medication-order (:event e))] e))
           ":medication-end" (count js))
  (println "ends by phase x verdict" (into (sorted-map-by #(compare (str %1) (str %2))) (frequencies (map (juxt :pre :verdict) js))))
  (println "ends referencing an order an earlier end already referenced, by phase" (frequencies (map :pre (filter :already-ended js))))
  (println "by end state x verdict" (into (sorted-map-by #(compare (str %1) (str %2))) (frequencies (map (juxt :state :verdict) js))))
  (println "log: pre-horizon facts :medication-order" (count (for [e gt f (:pre-horizon-facts e) :when (= :medication-order (:event f))] f))
           ":medication-end" (count (for [e gt f (:pre-horizon-facts e) :when (= :medication-end (:event f))] f)))
  (let [ends (filter #(= :medication-end (:event %)) gt)
        by (frequencies (keep :order-event-id ends))]
    (println "log: in-log :medication-end" (count ends) "sharing an :order-event-id with another end" (reduce + (filter #(> % 1) (vals by)))
             "nil :order-event-id" (count (remove :order-event-id ends))))
  (when-let [w (first (filter (fn [w] (some #(= :older (:verdict %)) (judge (:trajectory w)))) (sort-by (comp count :trajectory) @walks)))]
    (println "smallest walk with an :older end, module" (:module w))
    (doseq [[i e] (map-indexed vector (:trajectory w)) :when (#{:medication-order :medication-end} (:event e))]
      (println "  " i (:event e) (:state e) "pre-horizon" (boolean (:pre-horizon e)) "references" (:references e)))))
```

## Close

Pushed `fd31ee2c..ef4a8995` as one commit (this record, the prompt
archive, docsgen's three generated files). Before it: the docs-tooling
brick tests green (exit 0); the leak grep over the staged diff found
nothing identifying the downstream consumer; gitleaks clean.
`bin/post-push-verify fd31ee2c ef4a8995`: remote tip matches; the
commit message is ASCII; CI not yet indexed at that moment (disclosed
by the script). **CI run 44208032780 completed `success`** at
`ef4a899524e1e967d897db8dd0511e6966e6ed43`, attempt 1 -- the close
marker. No local `make test` (docs-only, AGENTS.md 22). No tag paid.
Background processes this session started: one `gh run watch`,
exited with the run; none left running.
