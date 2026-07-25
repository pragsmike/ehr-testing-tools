(ns ehr-testing-tools.judge.finding
  "The finding envelope (pattern nursery #6): one canonical shape for a
  single conformance observation, shared by every judge engine
  regardless of format -- {severity, code, locator, message, engine,
  native-ref}. `judge.fhir` and `judge.v2` both interpret their engine's
  raw output into this shape; nothing format-specific lives here.

  Verdicts are the Judge stage kind's own vocabulary (docs/notation.md,
  docs/palgebra-design.md D10/ADR-0010): :pass / :rejected /
  :indeterminate / :no-verdict. :indeterminate is RESERVED as of
  ADR-0010 -- kept in the enum because old baseline reports still
  serialize it (O1's own conservatism), but nothing in this repo
  produces it anymore; `judge.fhir`'s former :indeterminate case is now
  :no-verdict. :no-verdict is a legitimate outcome (the judge could not
  fully apply the criterion, e.g. terminology-suppressed offline) --
  distinct from the criterion simply not deciding the subject -- and it
  carries a :cause, in a sibling field, present if and only if the
  verdict is :no-verdict (`valid-cause-pairing?`, Malli-enforced).
  `worst-of` is the composition law across a file's findings:
  :rejected beats :no-verdict beats :indeterminate beats :pass -- a
  confirmed violation still dominates the aggregate over incidental
  partiality elsewhere in the same file (see `worst-of`'s own
  docstring for why this isn't the ranking ADR-0010 originally
  specified)."
  (:require [malli.core :as m]))

(def Severity
  "The four values FHIR's own IssueSeverity ValueSet defines --
  :fatal included (found during P5's contract-pairing exercise: the
  official validator emits it for a resource missing resourceType
  entirely, which neither EXP-C5's own corpus nor its five defect
  operators' other mutants happened to trigger -- v2's HL7Exception
  severities only ever surfaced :error in this repo's own testing, but
  the shared envelope carries the full FHIR vocabulary since judge.fhir
  needs it)."
  [:enum :error :warning :information :fatal])

(def Verdict
  "Four values (ADR-0010): :pass / :rejected / :indeterminate (RESERVED
  -- no producer as of this migration, kept only for old serialized
  data) / :no-verdict (paired with a :cause, see Cause/VerdictOutcome
  below)."
  [:enum :pass :rejected :indeterminate :no-verdict])

(def Cause
  "The no-verdict cause taxonomy (O2), minimum viable: judge.fhir's
  terminology-suppressed classification is the first live specimen.
  Grows as new operational-partiality causes are identified."
  [:enum :terminology-suppressed])

(def VerdictOutcome
  "A verdict paired with its cause -- cause is required if and only if
  verdict is :no-verdict (author ruling, ADR-0010): the fourth arm
  carries its cause in a distinct sibling field, not folded into the
  verdict keyword itself. Malli-enforced via the :fn refinement below."
  [:and
   [:map
    [:verdict Verdict]
    [:cause {:optional true} Cause]]
   [:fn {:error/message "cause is required iff verdict is :no-verdict"}
    (fn [{:keys [verdict cause]}]
      (if (= verdict :no-verdict) (some? cause) (nil? cause)))]])

(defn valid-cause-pairing?
  "True iff cause is present exactly when verdict is :no-verdict -- the
  totality rule for the fourth arm (ADR-0010), Malli-enforced via
  VerdictOutcome. cause is omitted from the validated map entirely
  when nil, matching :cause's :optional (key-absence) semantics rather
  than validating an explicit nil value against the Cause enum."
  [verdict cause]
  (m/validate VerdictOutcome (cond-> {:verdict verdict} (some? cause) (assoc :cause cause))))

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

;; :no-verdict ranked ABOVE :indeterminate but BELOW :rejected
;; (ADR-0010, revised during this same session's Step 5 integration
;; run): a corpus the judge couldn't fully apply its criterion to is
;; worse than one the criterion simply didn't decide, but a confirmed
;; violation elsewhere in the same file still dominates the aggregate.
;; ADR-0010's first draft ranked :no-verdict above :rejected outright
;; ("a corpus that couldn't be fully judged is not a corpus that
;; passed") -- reverted after `make integration` showed every real,
;; US-Core-profiled Synthea file mixes terminology-suppressed findings
;; with genuine profile-driven violations in the SAME file (EXP-C5);
;; under the original ranking, EVERY real file's aggregate verdict
;; became :no-verdict regardless of an actual injected defect,
;; overriding contract-pairing's and baseline-gating's polarity
;; regression rather than merely losing to it. This ordering remains
;; policy-flavored, not a neutral fact -- pre-registered as an
;; O3-adjacent exhibit (docs/palgebra-design.md §II.5): routing
;; decisions above the judge may reasonably rank these differently.
(def ^:private verdict-rank {:pass 0 :indeterminate 1 :no-verdict 2 :rejected 3})
(def ^:private rank->verdict {0 :pass 1 :indeterminate 2 :no-verdict 3 :rejected})

(defn worst-of
  "The Judge kind's composition law across a seq of verdicts:
  :rejected > :no-verdict > :indeterminate > :pass. An empty seq (no
  findings, or no files) is :pass by definition -- there is nothing to
  reject, leave indeterminate, or fail to judge."
  [verdicts]
  (if (empty? verdicts)
    :pass
    (rank->verdict (apply max (map verdict-rank verdicts)))))
