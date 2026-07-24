(ns ehr-testing-tools.gate.finding
  "The finding envelope (pattern nursery #6): one canonical shape for a
  single conformance observation, shared by every gate engine
  regardless of format -- {severity, code, locator, message, engine,
  native-ref}. `gate.fhir` and `gate.v2` both interpret their engine's
  raw output into this shape; nothing format-specific lives here.

  Verdicts are the Gate stage kind's own ternary vocabulary
  (docs/notation.md): :pass / :rejected / :indeterminate, with
  :indeterminate first-class -- a legitimate outcome (a check that
  could not run, e.g. terminology-suppressed offline), not an error.
  `worst-of` is the composition law across a file's findings:
  :rejected beats :indeterminate beats :pass."
  (:require [malli.core :as m]))

(def Severity [:enum :error :warning :information])

(def Verdict [:enum :pass :rejected :indeterminate])

(def Finding
  [:map
   [:severity Severity]
   [:code :string]
   [:locator :map]
   [:message :string]
   [:engine [:map [:name :string] [:version :string]]]
   [:native-ref {:optional true} :any]])

(defn valid?
  [finding]
  (m/validate Finding finding))

(def ^:private verdict-rank {:pass 0 :indeterminate 1 :rejected 2})
(def ^:private rank->verdict {0 :pass 1 :indeterminate 2 :rejected})

(defn worst-of
  "The Gate kind's ternary composition law across a seq of verdicts:
  :rejected > :indeterminate > :pass. An empty seq (no findings, or no
  files) is :pass by definition -- there is nothing to reject or leave
  indeterminate."
  [verdicts]
  (if (empty? verdicts)
    :pass
    (rank->verdict (apply max (map verdict-rank verdicts)))))
