(ns ehr-testing-tools.locator
  "Locator (pattern nursery #4): one type for \"a place in a datum\", with
  a per-format grammar. Only the trivial envelope checks land here --
  non-empty path, known format keyword. Real per-format grammars (FHIRPath,
  v2 segment/field/component, table.column, XPath) arrive with mutation
  and gates; this is the shared shape they'll plug into."
  (:require [malli.core :as m]
            [ehr-testing-tools.result :as result]))

(def known-formats #{:fhir :v2 :table :xpath})

(def Locator
  [:map
   [:format (into [:enum] known-formats)]
   [:path [:string {:min 1}]]])

(defn valid?
  [loc]
  (m/validate Locator loc))

(defn make
  "Builds a locator envelope. Rejects (not throws) an unknown format or an
  empty path -- grammar-specific validity is a later, format-dispatched
  concern."
  [format path]
  (let [candidate {:format format :path path}]
    (if (valid? candidate)
      (result/ok candidate)
      (result/rejected :invalid-locator {:format format :path path}))))
