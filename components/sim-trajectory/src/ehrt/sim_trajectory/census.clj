(ns ehrt.sim-trajectory.census
  "GMF census tool (parity plan `.agents/plans/2026-08-02-gmf-parity-plan.md`
  §3, ADR-0031 AR-1, ADR-0034). A `sim-trajectory` DEV ENTRY POINT, not a
  CLI verb (AR-1: promotable later as a curation decision once the
  verdict vocabulary stabilizes) -- walking an arbitrary external
  Synthea checkout on disk is a dev-tool concern, not something
  `components/sim-trajectory`'s own product interface should carry, so
  this namespace is not re-exported there. Standing-equipment promotion
  (2026-08-05, `notes/ADRs.md` promotion ADR, AR-P-1): moved INTO this
  component from `development/src` -- equipment, not API, so gmf/
  gmf-interpreter access is now an ordinary intra-component call rather
  than the foreign-component reach `development/src` required; the
  interface itself does not grow.

  Invocation: `clojure -M:dev -m ehrt.sim-trajectory.census
  <synthea-checkout-dir> <out-dir>` from the workspace root (unchanged
  by the move -- the `:dev` alias already wires `poly/sim-trajectory`
  as a `:local/root` dep, which was already how this namespace's own
  `gmf`/`gmf-interpreter` requires resolved even under `development/src`).
  Reads
  `<synthea-checkout-dir>/src/main/resources/modules/**` -- no network at
  run time, no vendoring of the catalog (installed != used, AR-1).
  Writes one dated EDN artifact into `<out-dir>`.

  Verdict vocabulary (AR-2): `:ok-walked` (closure loaded AND every
  smoke-walk seed completed without throwing -- terminal, blocked, or
  horizon-complete all count; only a THROW fails a module),
  `:load-failed` (`ehrt.sim-trajectory.gmf/load-closure` itself
  rejected), `:walk-failed` (closure loaded, at least one smoke-walk
  seed threw), `:out-of-scope-by-ruling` (AR-2 emptied its largest
  bucket at the time, but the category was always meant to fill again;
  it now does -- a `:load-failed` closure whose ENTIRE gap is a RULED
  exclusion, `Physiology` the first, `notes/ADRs.md` ADR-0037 AR-5,
  `out-of-scope-by-ruling?` below the single-cause classifier). A
  module that throws never
  aborts the census: the throw is caught, recorded, and the walk for
  that module moves on (AR-2's own 'the census itself NEVER aborts on a
  module's failure')."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp])
  (:import [java.security MessageDigest]
           [java.util Random]))

;; --- Census parameters (AR-4/AR-5): every one of these lands in the
;; artifact header verbatim -- the census must be re-runnable to the
;; byte. Fixed, global, not tuned per module (a per-module-tuned bound
;; would not be one census). ---------------------------------------------

(def synthea-pin
  "The pin `docs/gmf-interpreter.md` and `notes/ADRs.md` ADR-0031/-0032/
  -0033 all cite: `synthetichealth/synthea`, master, fetched 2026-07-27."
  "7e08387c68a7f0e21d13076609a159fd473fc902")

(def tool-version "1.0.0")
(def default-seed-count 3)
(def default-mixer-seed 20260803)

(def default-registration-offset-years
  "Registration at age 30 -- old enough that most acute/chronic onset
  conditions gating this catalog's modules have had a chance to fire
  during the history-phase fast-forward, per `docs/gmf-interpreter.md`
  §3's own no-fixed-tick design (RNG consumption scales with transitions
  crossed, not elapsed calendar time -- age 30 is cheap even though it
  is 30 simulated years)."
  30)

(def default-horizon-years
  "50 more years past registration (age 30 -> 80): large enough to give
  a module's own horizon-phase content (Encounters, Procedures,
  wellness-cycle onsets) real room to fire, small enough that a
  genuinely runaway module (a zero-time-advance transition cycle) still
  hits `gmf-interpreter`'s own `max-steps` backstop well inside a smoke
  walk's budget -- that backstop firing IS a real `:walk-failed` finding
  for this census, not a bug in the census itself."
  50)

;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-8): fixed, disclosed race/
;; socioeconomic weight pools -- NOT a demographic-accuracy claim (equal
;; weights across Synthea's own closed vocabularies, Logic.java's own
;; Race/SocioeconomicStatus classes, source-grounded), only enough to
;; exercise the new Race/Socioeconomic Status condition guards during
;; this census's own smoke walks. `persona`'s own AR-5 conditional-draw
;; law means every OTHER field/draw in this census is byte-identical to
;; every pre-Wave-F run -- these two keys are the only header delta.
;; GMF coverage Wave LC (2026-08-03, ADR-0038 AR-3): a THIRD, fixed,
;; disclosed key -- `:state-weights`, a SINGLE-option pool (unlike
;; race/ses's own multi-option closed vocabularies above, real Synthea
;; has no closed :state enumeration to exercise, only the ~50-entry US
;; state-name vocabulary the lookup-table CSVs themselves key on) so
;; every census persona deterministically carries the SAME value, "Alabama"
;; -- transcribed verbatim from a real row,
;; `ace_arb_amlodipine_benazepril_product_distribution.csv`'s own first
;; data row (`26-35,M,Alabama,...`, confirmed by direct read at the pin,
;; reached via myocardial_infarction.json's own closure). A single-
;; option pool still draws (the SAME fixed-consumption law, AR-3), only
;; its OUTCOME is fixed -- exercises the new module-set-attribute-vs-
;; persona-field resolution path (AR-1(c)) without needing all ~50
;; states represented.
(def default-persona-config
  {:race-weights [{:race "White" :weight 1.0} {:race "Black" :weight 1.0}
                  {:race "Hispanic" :weight 1.0} {:race "Asian" :weight 1.0}
                  {:race "Native" :weight 1.0} {:race "Other" :weight 1.0}]
   :socioeconomic-weights [{:category "High" :weight 1.0} {:category "Middle" :weight 1.0}
                           {:category "Low" :weight 1.0}]
   :state-weights [{:state "Alabama" :weight 1.0}]})

;; --- Pin verification (AR-1) --------------------------------------------

(defn- sha256-hex [^String s]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest digest (.getBytes s "UTF-8"))))))

(defn- content-hash
  "Fallback pin evidence when `checkout-dir` carries no `.git` (a tarball
  extract, AR-1): sha256 over the sorted relative-path + content of the
  whole modules tree. Not proof of a SPECIFIC upstream commit -- only of
  what this run actually read -- which is why `verify-pin` marks it
  `:pin-unverified-by-git` rather than treating it as equivalent."
  [checkout-dir]
  (let [root (io/file checkout-dir "src" "main" "resources" "modules")
        root-path (.getPath ^java.io.File root)
        files (->> (file-seq root)
                   (filter #(.isFile ^java.io.File %))
                   (sort-by #(.getPath ^java.io.File %)))]
    (sha256-hex
     (str/join "\n"
                (map (fn [^java.io.File f]
                       (str (subs (.getPath f) (inc (count root-path))) ":" (slurp f)))
                     files)))))

(defn verify-pin
  "A census is a claim AT a pin (parity plan §1); an artifact that
  cannot name its pin is not a census. `checkout-dir` a git checkout:
  the ONLY acceptable outcome is `git rev-parse HEAD` matching
  `expected-pin` EXACTLY -- a mismatch REFUSES the run (`:error`,
  never silently censusing the wrong commit). No `.git` at all: falls
  back to `content-hash`, `:ok` with `:pin-unverified-by-git true`
  disclosed in the payload for the artifact header to carry forward."
  [checkout-dir expected-pin]
  (let [git-dir (io/file checkout-dir ".git")]
    (if (.exists git-dir)
      (let [{:keys [exit out]} (sh "git" "rev-parse" "HEAD" :dir (str checkout-dir))
            actual (str/trim out)]
        (if (and (zero? exit) (= actual expected-pin))
          (result/ok {:method :git :pin actual})
          (result/error :pin-mismatch
                        {:expected expected-pin
                         :actual (if (zero? exit) actual :git-rev-parse-failed)})))
      (result/ok {:method :sha256-content
                   :pin (content-hash checkout-dir)
                   :pin-unverified-by-git true
                   :expected-pin expected-pin}))))

;; --- Catalog discovery + closure resolution ------------------------------

(defn discover-root-modules
  "Every top-level module JSON directly under `modules/` -- NOT
  recursing into a subdirectory (those are submodules/lookup tables,
  resolved only via a closure that actually calls them, per D3's own
  search-path discipline) and not `lookup_tables/`'s own CSV siblings.
  `:id` is `gmf/slug` of the filename minus `.json` -- the same
  transform every vendored module's own id already uses
  (`ear_infections.json` -> `\"ear-infections\"`).

  Result or loud (ADR-0078): returns result/ok a vector of {:id :file}
  maps, or result/error :listing-failed when the modules/ directory
  can't be listed -- previously a nil `.listFiles` here silently
  `filter`ed to zero root modules ((filter pred nil) => () in
  Clojure), so an I/O failure and 'this checkout genuinely has no
  root modules' were indistinguishable."
  [checkout-dir]
  (let [dir (io/file checkout-dir "src" "main" "resources" "modules")
        listing-result (result/list-files dir)]
    (if-not (result/ok? listing-result)
      listing-result
      (result/ok
       (->> (:payload listing-result)
            (filter (fn [^java.io.File f] (and (.isFile f) (str/ends-with? (.getName f) ".json"))))
            (map (fn [^java.io.File f]
                   {:id (gmf/slug (str/replace (.getName f) #"\.json$" "")) :file f}))
            (sort-by :id)
            vec)))))

(defn- checkout-modules-file ^java.io.File [checkout-dir & parts]
  (apply io/file checkout-dir "src" "main" "resources" "modules" parts))

(defn- slurp-if-exists [^java.io.File f]
  (when (.exists f) (slurp f)))

(defn- make-resolve-fn
  "`gmf/load-closure`'s own caller-supplied `resolve-fn`
  (call-path -> json-text|nil), wrapping a thin `io/file` read over the
  D3 search path (`modules/<call-path>.json`) -- and, additively,
  recording every call-path it actually reads into `fetched` (an atom),
  regardless of whether the loader goes on to accept or reject that
  text. `wellness-substitution?` (below) scans `fetched`, not the
  loader's own normalized output, so a `:load-failed` closure still
  gets AR-3's substitution tag from whatever it managed to read before
  failing."
  [checkout-dir fetched]
  (fn [call-path]
    (when-let [text (slurp-if-exists (checkout-modules-file checkout-dir (str call-path ".json")))]
      (swap! fetched assoc call-path text)
      text)))

(defn- make-table-resolve-fn
  "The table-reading twin of `make-resolve-fn`, above -- records every
  table-name it actually reads into the SAME `fetched` atom, under the
  collision-proof key `(str \"lookup_tables/\" table-name)` (matching the
  on-disk relative path, disjoint by construction from a module
  call-path key, which never contains a `/`) so `:closure-file-count`
  (below) counts a table read before a `:load-failed` closure's own
  failure, not only the JSON modules `make-resolve-fn` records (ADR-0094,
  ruling 6 = D6-1, \"a\")."
  [checkout-dir fetched]
  (fn [table-name]
    (when-let [text (slurp-if-exists (checkout-modules-file checkout-dir "lookup_tables" table-name))]
      (swap! fetched assoc (str "lookup_tables/" table-name) text)
      text)))

;; --- Substitution tagging (AR-3) -----------------------------------------
;;
;; RETIRED (2026-08-03, notes/ADRs.md ADR-0037 AR-5): `wellness-
;; substitution?` scanned for the loader's own create-now substitution
;; clause (ADR-0031 AR-5(b)) -- that clause no longer exists (ADR-0037
;; AR-3 retired it; `wellness: true` now loads as its own distinct
;; `:wellness-wait` state type and the interpreter genuinely waits).
;; Kept as history, not deleted outright, per this project's own
;; fix-forward-with-disclosure discipline. The `:disclosed-
;; substitutions` tag VECTOR stays -- extensible for a future
;; substitution this census finds, never itself the thing retired.

(defn wellness-substitution?
  "RETIRED (see comment above) -- no longer called from `census-one`.
  Kept as a historical record of what the substitution trigger looked
  like; scanned against RAW json (string keys, unnormalized) because a
  loaded module could not be told apart from one that genuinely
  authored `encounter_class: wellness` once normalized."
  [raw-texts]
  (boolean
   (some (fn [text]
           (let [raw (json/read-str text)]
             (some (fn [state]
                     (and (map? state)
                          (= "Encounter" (get state "type"))
                          (true? (get state "wellness"))
                          (not (contains? state "encounter_class"))))
                   (vals (get raw "states")))))
         (vals raw-texts))))

;; --- Out-of-scope-by-ruling classification (AR-5) -------------------------

(def ^:private out-of-scope-state-types
  "GMF coverage Wave G (2026-08-03, ADR-0037 AR-5): state types the
  census classifies as `:out-of-scope-by-ruling` rather than
  `:load-failed` -- a RULED exclusion (this project has no equivalent,
  by author decision), not a genuine load gap still to close.
  `Physiology` (Synthea's own ODE-based physiology engine, found in
  `gallstones.json`) is the first entry, citing this ADR. Grows by
  ruling, never by convenience -- adding a type here without a matching
  author ruling is not this set's own discipline."
  #{"Physiology"})

(defn out-of-scope-by-ruling?
  "AR-5: a `:load-failed` closure reclassifies to `:out-of-scope-by-
  ruling` ONLY when its ENTIRE load gap is explained by ruled-out state
  types -- every other gap category empty, so this stays a clean,
  single-cause classification rather than papering over a genuinely
  mixed gap. Public (like `wellness-substitution?`, above) so it is
  directly testable against hand-built gap maps -- `load-module`'s own
  short-circuiting `cond` (first bad state wins) makes a genuinely
  mixed gap hard to construct honestly through the loader alone."
  [gap]
  (boolean
   (and (seq (:unrecognized-state-types gap))
        (every? out-of-scope-state-types (:unrecognized-state-types gap))
        (empty? (:unresolved-submodules gap))
        (empty? (:unresolved-tables gap))
        (empty? (:malformed-lookup-table-ranges gap))
        (empty? (:attribute-collisions gap))
        (nil? (:cyclic-closure gap))
        (empty? (:other-rejections gap)))))

;; --- Verdict + gap extraction (AR-2) --------------------------------------

(defn- flatten-rejection
  "A `:submodule-rejected` rejection's own `:payload :reason` is itself
  a full Result -- possibly ANOTHER `:submodule-rejected`, arbitrarily
  deep down a closure's own call graph. Walks that chain to the root
  cause; gap extraction reads whichever categories it recognizes off
  EVERY link, not just the outermost one."
  [rejection]
  (->> rejection (iterate #(get-in % [:payload :reason])) (take-while some?)))

;; GMF coverage Wave LC (2026-08-03, ADR-0038 AR-1): H2's own
;; `:unrecognized-lookup-table-column` rejection is RETIRED (the column
;; whitelist it named is gone, gmf.clj's own docstring) -- replaced by
;; `:malformed-lookup-table-range` (a structurally invalid `age`/`time`
;; cell, the ONLY load-time rejection a lookup table's own content can
;; still trigger).
(def ^:private recognized-gap-categories
  #{:unsupported-state-type :submodule-not-found :lookup-table-not-found
    :malformed-lookup-table-range :attribute-collision :cyclic-closure
    :submodule-rejected})

(defn- extract-load-gap [rejection]
  (let [links (flatten-rejection rejection)
        by-cat (fn [cat k] (into #{} (keep (fn [{:keys [category payload]}]
                                             (when (= cat category) (get payload k))) links)))]
    {:unrecognized-state-types (by-cat :unsupported-state-type :raw-type)
     :unresolved-submodules (by-cat :submodule-not-found :call-path)
     :unresolved-tables (by-cat :lookup-table-not-found :table-name)
     :malformed-lookup-table-ranges (into #{} (keep (fn [{:keys [category payload]}]
                                                       (when (= :malformed-lookup-table-range category) payload))
                                                     links))
     :attribute-collisions (by-cat :attribute-collision :attribute)
     :cyclic-closure (some (fn [{:keys [category payload]}] (when (= :cyclic-closure category) (:cycle payload))) links)
     :other-rejections (into [] (keep (fn [{:keys [category payload]}]
                                        (when-not (recognized-gap-categories category) {:category category :payload payload}))
                                      links))}))

(defn- exception-detail [^Throwable e]
  {:message (.getMessage e) :data (ex-data e) :class (.getSimpleName (class e))})

;; --- Smoke walks + digests (AR-4) ----------------------------------------

(defn- mixed-seeds
  "The same mixer-RNG derivation `bin/oracle-src/ehrt/oracle/digest.clj`
  uses (docstring cited, per this session's own read-first discipline):
  sequential small `java.util.Random` seeds are NOT well-distributed for
  their own first draw, so seeds are derived from one mixer RNG's own
  `.nextLong` stream instead of counting up."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- walk-one
  "One smoke-walk seed: a fresh persona at `seed` (uniform sampling,
  `default-persona-config`), registered at
  `default-registration-offset-years`, horizon-bounded at
  `default-horizon-years` further. Never throws past this function --
  `run-module` blowing up (max-steps, an unresolved condition/vital-sign,
  a blocked-submodule-call, ...) is caught and returned as data, per
  AR-2's own 'the census itself NEVER aborts.'

  EncounterEnd fix (2026-08-08, ADR-0082, R2/AR-EE-2(iv)): an `:ok?
  true` row ADDITIVELY carries `:suppressed-encounter-ends` when
  nonzero (`run-module`'s own ctx, R2's zero-cost diagnostic) -- absent
  entirely when zero, the same 'no third bucket for the common case'
  discipline `:substance`'s own `keep` (below, `census-one`'s summary)
  already establishes."
  [root-module modules tables seed reg-offset-years horizon-years]
  (try
    (let [persona (sim-model/persona (Random. seed) default-persona-config)
          reg-t (+ (interp/dob-epoch-day persona) (* 365 reg-offset-years))
          end-t (+ reg-t (* 365 horizon-years))
          ctx (interp/run-module root-module (Random. seed) persona reg-t end-t modules {} tables)
          canon (pr-str {:status (:status ctx) :trajectory (:trajectory ctx)})]
      (cond-> {:seed seed :ok? true :status (:status ctx)
               :event-count (count (:trajectory ctx)) :digest (sha256-hex canon)}
        (pos? (:suppressed-encounter-ends ctx)) (assoc :suppressed-encounter-ends (:suppressed-encounter-ends ctx))))
    (catch Throwable e
      {:seed seed :ok? false :error (exception-detail e)})))

;; --- Per-module census -----------------------------------------------------

(defn census-one
  "Censuses one top-level module: resolves its closure (AR-1's no-
  network read over `checkout-dir`), tags AR-3's wellness substitution
  off whatever was fetched regardless of outcome, and -- ONLY if the
  closure loaded -- runs `seed-count` smoke walks (AR-4). Never throws;
  a smoke walk that throws is caught inside `walk-one` and turns the
  module's own verdict into `:walk-failed`, not a census-aborting
  exception.

  A full-catalog sweep (Step 2, this session) found `load-closure`
  itself is NOT actually exception-free for every real module: a
  `gmf_version 2` `GAUSSIAN` timing distribution (a real kind this
  loader's own `gmf-v2-timing->v1` `case` has no clause for, unlike
  `UNIFORM`/`EXACT`) throws a raw `IllegalArgumentException` rather than
  a `:rejected` Result -- a genuine `ehrt.sim-trajectory.gmf` robustness
  gap the census's own full-sweep evidence surfaced (named, not fixed,
  per this session's own fence: the census OBSERVES the loader as it
  stands). `load-closure` is therefore ALSO wrapped in try/catch here,
  the same discipline `walk-one` already applies one layer down --
  otherwise this one module would crash the whole census run, exactly
  the failure mode AR-2's 'never aborts on a module's failure' rules
  out.

  Census substance (2026-08-07, ADR-0069, AR-VC-2): an `:ok-walked` row
  ADDITIVELY carries `:event-counts` (the per-seed `:event-count` vector,
  surfaced at row level so curation-time ranking never re-digs `:walks`)
  and `:substance` (`:zero-on-every-seed` iff every seed's count is 0,
  else `:produces-content`), derived from the walks' own already-recorded
  counts -- no new sampling. Neither key is present on a `:load-failed`/
  `:walk-failed`/`:out-of-scope-by-ruling` row; the verdict enum itself
  does not change."
  [checkout-dir {:keys [seed-count mixer-seed registration-offset-years horizon-years]} {:keys [id ^java.io.File file]}]
  (let [root-json-text (slurp file)
        fetched (atom {id root-json-text})
        resolve-fn (make-resolve-fn checkout-dir fetched)
        table-resolve-fn (make-table-resolve-fn checkout-dir fetched)
        closure (try
                  (gmf/load-closure id root-json-text resolve-fn table-resolve-fn)
                  (catch Throwable e
                    {:status :error :category :loader-exception :payload (exception-detail e)}))
        ;; :wellness-timing retired (ADR-0037 AR-5, see the comment
        ;; above wellness-substitution?) -- the tag vocabulary stays
        ;; extensible, this census just has none active right now.
        substitutions []]
    (if-not (result/ok? closure)
      ;; AR-D-6: same definition as the ok-walked branch, below -- every
      ;; DISTINCT module/table file actually read before the failure.
      ;; `fetched` already carries both kinds (`make-resolve-fn`/
      ;; `make-table-resolve-fn` both record into it), so `count` alone
      ;; needs no branch-specific arithmetic here.
      (let [gap (assoc (extract-load-gap closure) :closure-file-count (count @fetched))]
        {:id id :file (.getName file)
         :verdict (if (out-of-scope-by-ruling? gap) :out-of-scope-by-ruling :load-failed)
         :disclosed-substitutions substitutions
         :gap gap
         :walks []})
      (let [{:keys [modules tables]} (:payload closure)
            root-module (get modules id)
            seeds (mixed-seeds seed-count mixer-seed)
            walks (mapv #(walk-one root-module modules tables % registration-offset-years horizon-years) seeds)
            failed (remove :ok? walks)
            ok-walked? (empty? failed)
            event-counts (when ok-walked? (mapv :event-count walks))]
        (cond-> {:id id :file (.getName file)
                 :verdict (if ok-walked? :ok-walked :walk-failed)
                 :disclosed-substitutions substitutions
                 ;; AR-D-6: :closure-file-count means the number of
                 ;; DISTINCT files read into the closure -- root module +
                 ;; transitively-called submodules + lookup-table CSVs.
                 :gap {:closure-file-count (+ (count modules) (count tables))
                       :walk-errors (mapv #(select-keys % [:seed :error]) failed)}
                 :walks (mapv #(dissoc % :ok?) walks)}
          ok-walked? (assoc :event-counts event-counts
                             :substance (if (every? zero? event-counts)
                                          :zero-on-every-seed
                                          :produces-content)))))))

;; --- Summary (AR-5: appended to the interpreter doc as a dated section) --

(defn summarize [modules]
  {:total (count modules)
   :by-verdict (frequencies (map :verdict modules))
   :substitution-count (count (filter (comp seq :disclosed-substitutions) modules))
   ;; Census substance (2026-08-07, ADR-0069, AR-VC-2): a tally over
   ;; `:ok-walked` rows' own additive `:substance` key -- `keep` rather
   ;; than `map` since every non-`:ok-walked` row carries no `:substance`
   ;; at all (nil, filtered out, never counted as a third bucket).
   :ok-walked-by-substance (frequencies (keep :substance modules))
   :top-gap-mechanisms
   (->> modules
        (filter #(= :load-failed (:verdict %)))
        (mapcat (fn [m] (map (fn [t] [:unrecognized-state-type t]) (get-in m [:gap :unrecognized-state-types]))))
        frequencies
        (sort-by (comp - val))
        (into []))})

;; --- Top-level run + artifact emission (AR-5) ----------------------------

(defn run-census
  "Returns a Result: `:error :pin-mismatch` (verify-pin refused, no
  modules censused) or `:ok` with `{:header {...} :modules [...]
  :summary {...}}` -- the full re-runnable artifact payload, `:header`
  carrying every AR-4/AR-5 parameter this run actually used."
  [checkout-dir]
  (let [pin-result (verify-pin checkout-dir synthea-pin)]
    (if-not (result/ok? pin-result)
      pin-result
      (let [opts {:seed-count default-seed-count :mixer-seed default-mixer-seed
                  :registration-offset-years default-registration-offset-years
                  :horizon-years default-horizon-years}
            roots-result (discover-root-modules checkout-dir)]
        (if-not (result/ok? roots-result)
          roots-result
          (let [roots (:payload roots-result)
                modules (mapv #(census-one checkout-dir opts %) roots)]
            (result/ok
             {:header (merge opts
                             {:tool-version tool-version
                              :synthea-pin synthea-pin
                              :pin-verification (:payload pin-result)
                              :census-date (str (java.time.LocalDate/now))
                              :module-count (count modules)
                              :persona-config default-persona-config
                              :checkout-dir (str checkout-dir)})
              :modules modules
              :summary (summarize modules)})))))))

(defn artifact-filename
  "Census substance (2026-08-07, ADR-0069, AR-VC-3), roadmap 'Census tool
  refinements' item (c): the same-calendar-day collision two prior
  re-runs (Wave F0, Wave F) worked around by hand-appending a wave
  suffix -- now a real, tested, optional third segment. `label` nil or
  blank leaves the filename shape byte-identical to every run before
  this session; a non-blank label appends `-<label>` before the
  extension."
  [census-date pin7 label]
  (str census-date "-synthea-" pin7 (when-not (str/blank? label) (str "-" label)) ".edn"))

(defn -main
  "`clojure -M:dev -m ehrt.sim-trajectory.census <synthea-checkout-dir>
  <out-dir> [label]`. Writes `<out-dir>/<census-date>-synthea-<pin7>.edn`
  (or, with `label`, `<out-dir>/<census-date>-synthea-<pin7>-<label>.edn`,
  `artifact-filename`, AR-VC-3) and prints the summary; a refused pin
  verification exits non-zero and writes nothing."
  ([checkout-dir out-dir] (-main checkout-dir out-dir nil))
  ([checkout-dir out-dir label]
   (let [result (run-census checkout-dir)]
     (if-not (result/ok? result)
       (do (println "CENSUS REFUSED:" (pr-str result))
           (System/exit 1))
       (let [payload (:payload result)
             pin7 (subs synthea-pin 0 7)
             out-file (io/file out-dir (artifact-filename (:census-date (:header payload)) pin7 label))]
         (.mkdirs (io/file out-dir))
         (spit out-file (with-out-str (pprint/pprint payload)))
         (println "wrote" (.getPath out-file))
         (println "summary:" (pr-str (:summary payload))))))))
