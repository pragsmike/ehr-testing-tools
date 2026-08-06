(ns ehrt.docs-tooling.tag-law-test
  "AR-T-3 (tag law, ADR-0057, 2026-08-06): the tag law's own propagation
  lesson (`.agents/rulings.md`, 'Law-surface propagation lesson,
  standing') recorded two prior instances of an amendment to standing
  law landing on some but not all of the surfaces that state it. This
  is the third instance: `.agents/rulings.md`'s own AR-R-2 said tagging
  stays the author's act alone (R30) while ADR-0049's AR-AU-0 had
  already amended that mechanic three sessions earlier; ADR-0051
  reconciled `AGENTS.md` alone, leaving `AUTHORS-GUIDE.md`, both
  `build-session` `SKILL.md` copies, `.agents/state.md`, and
  `.agents/rulings.md` itself still stating (or implying) the
  author-only law after the mechanic had already changed underneath
  them.

  AR-T-1 restates the law canonically (a session executes a licensed
  `stable-*` tag; deferral is now the deviation) and AR-T-2 lands it on
  every enumerated surface in the same session. This gate is AR-T-3: a
  freshness tripwire, not a one-time sweep -- it pins the two retired
  formulations that made the old, superseded law true and forbids them
  outright on the enumerated live surfaces, so a future edit can't
  reintroduce either phrase (a copy-paste from an old draft, a partial
  revert) without a red test naming exactly what came back. Frozen
  archives, ADRs, session records, prompt archives, and dated one-shots
  are deliberately out of scope -- they narrate history and legitimately
  quote the retired phrasing verbatim (this ADR's own surface table
  among them); this test never reads them, same scoping discipline
  `ehrt.docs-tooling.stale-path-test`'s own family uses throughout."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]))

(def ^:private live-tag-law-surfaces
  "The six files AR-T-2's sweep touches -- every live surface that
  states tag law, enumerated by the fresh grep this session ran before
  writing this gate (ADR-0057's own preflight; no seventh surface
  found)."
  [".agents/rulings.md"
   ".agents/state.md"
   ".agents/skills/build-session/SKILL.md"
   ".claude/skills/build-session/SKILL.md"
   "AGENTS.md"
   "AUTHORS-GUIDE.md"])

(def ^:private retired-phrasings
  "The two exact formulations AR-T-3 names -- both stated the old,
  now-superseded law (tags are author-only in every mode, full stop).
  After AR-T-2's rewrite, nothing true on any live surface is stated by
  either phrase anymore: the standing law is that a session EXECUTES a
  licensed `stable-*` tag, with release tags and repo-level `gh`
  mutations the narrower carve-out that stays author-only."
  ["stay author-only in every mode"
   "AUTHOR ACTION in every ceremony mode"])

(defn- retired-phrasing-violations
  "Every retired phrase from `retired-phrasings` present in `content`,
  distinct, in list order -- empty when `content` carries none."
  [content]
  (filterv #(str/includes? content %) retired-phrasings))

(deftest no-retired-tag-law-phrasing-on-live-surfaces-test
  (doseq [path live-tag-law-surfaces]
    (let [found (retired-phrasing-violations (slurp path))]
      (is (empty? found)
          (str path " still states the retired tag-law phrasing " found
               " -- AR-T-1/AR-T-2 (ADR-0057) restated and swept this law;"
               " a session tags under license, deferral is the deviation")))))

;; -- mechanism-sanity: prove the extraction function actually catches
;; what it claims to, same pairing shape `ehrt.docs-tooling.done-
;; pointer-adr-test` and `ehrt.docs-tooling.reading-set-budget-test`
;; both use --

(deftest retired-phrasing-extraction-is-actually-caught-test
  (testing "each retired phrase, standalone, is caught"
    (is (= ["stay author-only in every mode"]
           (retired-phrasing-violations
            "AUTHOR ACTION checkpoints stay author-only in every mode.")))
    (is (= ["AUTHOR ACTION in every ceremony mode"]
           (retired-phrasing-violations
            "tags are AUTHOR ACTION in every ceremony mode, including R30."))))
  (testing "both phrases in the same doc are both caught, in order"
    (is (= ["stay author-only in every mode" "AUTHOR ACTION in every ceremony mode"]
           (retired-phrasing-violations
            (str "stay author-only in every mode. "
                 "separately, tags are AUTHOR ACTION in every ceremony mode.")))))
  (testing "the AR-T-1 rewrite -- a session executes a licensed tag -- does not trip it"
    (is (empty? (retired-phrasing-violations
                 (str "a session creates and pushes a `stable-*` continuity tag"
                      " when its own prompt licenses a specific tag at a specific"
                      " commit; release tags and repo-level `gh` mutations remain"
                      " AUTHOR ACTION.")))))
  (testing "a bare 'AUTHOR ACTION' or 'author-only' mention that isn't the retired full phrase does not trip it"
    (is (empty? (retired-phrasing-violations
                 "git surgery and placing external documents are AUTHOR ACTION, unchanged.")))
    (is (empty? (retired-phrasing-violations
                 "repo-level gh mutations stay author-only, unchanged.")))))
