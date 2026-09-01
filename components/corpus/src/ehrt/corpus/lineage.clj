(ns ehrt.corpus.lineage
  "Lineage (pattern nursery #5): every derived datum carries a parent
  reference by content hash plus a transformation record; records are
  append-only, corrections are new records -- there is no
  update/amend function here, deliberately. A lineage record is
  self-verifying: its :id IS the content hash of its own remaining
  fields, computed here rather than supplied by the caller, so a
  record can never be built inconsistent with its own claimed
  identity. Merkle-style: correcting a mistake means building a new
  record whose :parent points at whatever the correction is relative
  to -- never mutating a record already produced."
  (:require [malli.core :as m]
            [ehrt.kernel.interface :as kernel]))

(def sha256-pattern
  [:re #"^[0-9a-f]{64}$"])

(def OperatorRef
  [:map [:id :keyword] [:version :string]])

(def Transformation
  "Two shapes, dispatched on the presence of :seed, and BOTH strict --
  not one loosened map with everything optional.

  The file shape (:fhir/:v2 operators, ADR-0004) records the LOCATOR it
  was handed. The event shape (:format :event, ADR-0176 section 2(iii),
  ruled 2026-09-01) records instead the operator's own :seed and the
  :site that one draw selected, plus the :expected-findings the
  operator declared -- so a mutant is reproducible from (parent-run
  identity, operator, seed) and nothing else, and so the defect class a
  consumer was handed is legible from the lineage record alone, without
  a registry lookup.

  Keeping the two separate rather than making :locator optional is
  deliberate: a file lineage record carrying no locator is malformed,
  and one loosened map with both keys optional would quietly stop
  saying so."
  [:multi {:dispatch (fn [t] (if (contains? t :seed) :event :file))}
   [:file [:map
           [:operator OperatorRef]
           [:locator :map]
           [:contract :map]]]
   [:event [:map
            [:operator OperatorRef]
            [:seed :int]
            [:site :int]
            [:contract :map]
            [:expected-findings [:set :keyword]]]]])

(def LineageRecord
  [:map
   [:id sha256-pattern]
   [:parent sha256-pattern]
   [:stage :keyword]
   [:transformation Transformation]
   [:produced sha256-pattern]])

(defn valid?
  [record]
  (m/validate LineageRecord record))

(defn record-content-hash
  "The content hash of a record's fields -- excludes :id itself, which
  IS this value; callers pass a record already without :id (e.g. via
  `(dissoc record :id)`)."
  [record-without-id]
  (kernel/sha256-string (pr-str record-without-id)))

(defn build
  "Builds a lineage record. :parent and :produced are content hashes
  (sha256 hex strings) of the base and mutant datum respectively --
  computing those from the actual datum bytes is the caller's job
  (e.g. `kernel/sha256-string`); this function only computes :id, from
  the rest of the record's own content."
  [{:keys [parent stage transformation produced]}]
  (let [without-id {:parent parent :stage stage :transformation transformation :produced produced}]
    (assoc without-id :id (record-content-hash without-id))))

(defn valid-content-hash?
  "True if record's :id matches the recomputed hash of its own
  remaining content -- the self-verification property every lineage
  record must hold."
  [record]
  (= (:id record) (record-content-hash (dissoc record :id))))
