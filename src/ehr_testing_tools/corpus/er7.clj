(ns ehr-testing-tools.corpus.er7
  "The v2 mutation substrate (P7): plain delimiter-split ER7 data --
  segments split on the segment terminator, fields split on MSH-1's own
  field separator -- never a HAPI-parsed tree. Same applied decision
  rule EXP-B2 imposed on FHIR (docs/experiments/EXP-B2-results.md,
  applied to FHIR by ehr-testing-tools.corpus.mutate): HAPI HL7v2's
  PipeParser round-trip is faithful for realistically-populated
  messages but silently canonicalizes away trailing empty fields on a
  message crafted to end a segment that way -- exactly the hazard a
  mutation substrate cannot tolerate underneath it (Mutate's
  intended-diff-only law), even though PipeParser remains perfectly
  fine for *judging* (judge.v2 uses it unchanged; only the mutation
  substrate is disqualified). This namespace's split/join is built to
  be a mathematically exact inverse pair rather than merely tested
  toward one: every clojure.string/split call below uses limit -1, so
  no trailing empty token -- the exact canonicalization PipeParser
  performs -- is ever silently dropped.

  MSH-1/MSH-2 as the delimiter source: a message's own MSH segment
  names its delimiters -- MSH-1 (the character immediately after the
  literal \"MSH\") is the field separator; MSH-2 (the four characters
  after that) are the component, repetition, escape, and subcomponent
  separators, in that fixed order (\"^~\\&\" canonically, per this
  session's fixtures). Every other segment in the message is split
  using those same characters, read once from the message's own head --
  HL7 v2 does not vary delimiters segment-to-segment.

  Field granularity only: components/repeats/subcomponents are carried
  verbatim inside a field's string value, never further decomposed
  here -- no seed operator this session needs component-level access
  (ehr-testing-tools.locator's v2 grammar supports naming one, for
  future operators that do). This mirrors the FHIR locator's own
  scope note: a genuinely fuller substrate is future work, not
  required by what exists to consume it today.

  Content identity: `content-hash` is sha256 of the serialized ER7
  string -- mirroring corpus.mutate/content-hash's rationale (hash what
  actually gets persisted to disk, not an incidental in-memory form)."
  (:require [clojure.string :as str]
            [ehr-testing-tools.digest :as digest]))

(def segment-terminator
  "\r")

(defn delimiters
  "The four MSH-2 delimiter characters plus MSH-1's field separator,
  read directly from content's own head (\"MSH\" followed by the field
  separator, then the four MSH-2 characters) -- never hardcoded,
  honoring MSH-1/MSH-2 as the source of these characters."
  [content]
  {:field (subs content 3 4)
   :component (subs content 4 5)
   :repetition (subs content 5 6)
   :escape (subs content 6 7)
   :subcomponent (subs content 7 8)})

(defn- split-all
  "clojure.string/split with limit -1: every token survives, including
  trailing empty ones -- the exact property PipeParser's canonicalizing
  round-trip lacks (EXP-B2) and this substrate must not reproduce."
  [s sep]
  (str/split s (re-pattern (java.util.regex.Pattern/quote sep)) -1))

(defn parse
  "content (a raw ER7 string) -> {:delimiters {...} :segments [[field
  field ...] ...]}, each segment a vector of field strings with the
  segment name itself at index 0 (so field N sits at index N for every
  segment except MSH -- see ehr-testing-tools.locator's v2 grammar
  docstring for MSH's own off-by-one, which this function's
  field-index-per-segment-name split already produces correctly without
  any MSH special-case: MSH's field separator immediately follows the
  literal \"MSH\", so MSH-2 -- the encoding characters -- lands at
  split-index 1, one before what a naive field-number-equals-index
  reading would expect)."
  [content]
  (let [delims (delimiters content)
        field-sep (:field delims)]
    {:delimiters delims
     :segments (mapv #(split-all % field-sep) (split-all content segment-terminator))}))

(defn serialize
  "The exact inverse of `parse`: fields rejoined on the field
  separator, segments rejoined on the segment terminator. Exact because
  `parse` never drops a token (limit -1 throughout) -- there is nothing
  for `serialize` to fail to reconstruct."
  [{:keys [delimiters segments]}]
  (let [field-sep (:field delimiters)]
    (str/join segment-terminator (map #(str/join field-sep %) segments))))

(defn content-hash
  "The content hash corpus.mutate's v2 path uses to identify a
  message: sha256 of its serialized ER7 string -- the same bytes that
  would be written to disk, per corpus.mutate/content-hash's own
  hash-what-gets-persisted rationale, applied here to the v2 substrate."
  [content]
  (digest/sha256-string content))
