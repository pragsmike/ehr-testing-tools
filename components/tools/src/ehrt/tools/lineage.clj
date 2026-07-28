(ns ehrt.tools.lineage
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
            [ehrt.tools.digest :as digest]
            [ehrt.tools.result :as result]))

(def sha256-pattern
  [:re #"^[0-9a-f]{64}$"])

(def LineageRecord
  [:map
   [:id sha256-pattern]
   [:parent sha256-pattern]
   [:stage :keyword]
   [:transformation [:map
                      [:operator [:map [:id :keyword] [:version :string]]]
                      [:locator :map]
                      [:contract :map]]]
   [:produced sha256-pattern]])

(defn valid?
  [record]
  (m/validate LineageRecord record))

(defn record-content-hash
  "The content hash of a record's fields -- excludes :id itself, which
  IS this value; callers pass a record already without :id (e.g. via
  `(dissoc record :id)`)."
  [record-without-id]
  (digest/sha256-string (pr-str record-without-id)))

(defn build
  "Builds a lineage record. :parent and :produced are content hashes
  (sha256 hex strings) of the base and mutant datum respectively --
  computing those from the actual datum bytes is the caller's job
  (e.g. `digest/sha256-string`); this function only computes :id, from
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
