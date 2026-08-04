(ns ehrt.docs-tooling.prompt-record-pairing-test
  "AR-M1-7 (sim split B, M1 step 5, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` M1 session prompt):
  the prompt/record pairing invariant -- every session record archives
  its own driving prompt, ADR-0023's own convention -- has been
  followed by every session since it was adopted, but nothing made
  forgetting fail until this test. Freshness-gate pattern, same shape
  as `ehrt.docs-tooling.notes-prompts-frozen-test`'s pinned set and
  `ehrt.docs-tooling.index-completeness-test`'s exact-token-both-
  directions checks.

  Two directions, both real:

  - **Presence**: every `.agents/session-records/*.md` (README
    excluded) has a same-slug `.agents/prompts/*.md` -- EXCEPT the
    seven pre-cutover records below, whose own prompts predate
    `.agents/prompts/`'s existence and live in the frozen
    `notes/prompts/` tree instead, under the older `ehr-testing-`
    slug prefix (`ehrt.docs-tooling.notes-prompts-frozen-test` pins
    that directory's own file set -- it can never receive a renamed
    copy of one of these seven, so the allowlist is permanent, not a
    todo).
  - **Absence**: every `.agents/prompts/*.md` has a same-slug
    `.agents/session-records/*.md` -- holds today with zero
    exceptions; a future prompt archived with no paired record would
    mean a session skipped R-A (`.agents/skills/build-session/
    SKILL.md`'s own close-out step).

  The pre-cutover allowlist below is derived from a fresh directory
  diff at this test's own authoring time (comm -23 between the two
  slug sets), not hand-enumerated from memory -- the third deftest
  below proves the allowlist is exactly that gap, not a superset that
  would silently mask a real future miss."
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def ^:private pre-cutover-record-slugs
  "The seven session records (2026-07-28/2026-07-29, before
  `.agents/prompts/` existed) whose own driving prompt lives in frozen
  `notes/prompts/` instead, under the older `ehr-testing-` prefix.
  Derived 2026-08-04 (this gate's own authoring session) via `comm -23`
  between `.agents/session-records/`'s and `.agents/prompts/`'s own
  slug sets -- every one confirmed to resolve to a real file in
  `notes/prompts/` at the expected `<date>-ehr-testing-<rest>.md` path
  before this allowlist was written."
  #{"2026-07-28-discipline-parity"
    "2026-07-29-development-resumption"
    "2026-07-29-exp-d3-nist-validator"
    "2026-07-29-judge-engine-extraction"
    "2026-07-29-sim-sibling-errata-sweep"
    "2026-07-29-storefront-polish"
    "2026-07-29-wsl-clone-igamt-hygiene"})

(defn- slugs
  "Every regular file's own slug (filename minus '.md') in `dir`,
  README.md excluded."
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isFile %))
       (map #(.getName %))
       (remove #{"README.md"})
       (map #(str/replace % #"\.md$" ""))
       set))

(defn- frozen-prompt-path
  "The notes/prompts/ path a pre-cutover record's own prompt is
  expected at: the same date, `ehr-testing-` inserted before the rest
  of the slug -- the naming convention every one of the 29 frozen
  files in that directory actually uses."
  [slug]
  (str "notes/prompts/" (subs slug 0 10) "-ehr-testing-" (subs slug 11) ".md"))

(deftest every-session-record-has-a-paired-prompt-test
  (let [records (slugs ".agents/session-records")
        prompts (slugs ".agents/prompts")
        unpaired (set/difference records prompts)
        unexpected (set/difference unpaired pre-cutover-record-slugs)]
    (is (empty? unexpected)
        (str "session record(s) with no paired .agents/prompts/ entry, "
             "and not on the pre-cutover allowlist -- did this session "
             "forget to archive its own driving prompt (R-A)? " unexpected))
    (doseq [slug (set/intersection unpaired pre-cutover-record-slugs)]
      (is (.isFile (io/file (frozen-prompt-path slug)))
          (str slug "'s allowlisted frozen prompt does not actually "
               "exist at " (frozen-prompt-path slug))))))

(deftest every-prompt-has-a-paired-session-record-test
  (let [records (slugs ".agents/session-records")
        prompts (slugs ".agents/prompts")
        orphans (set/difference prompts records)]
    (is (empty? orphans)
        (str "prompt(s) archived with no paired .agents/session-records/ "
             "entry -- a record must land in the same commit as its own "
             "prompt archive (R-A): " orphans))))

(deftest pre-cutover-allowlist-matches-live-gap-exactly-test
  (let [records (slugs ".agents/session-records")
        prompts (slugs ".agents/prompts")
        unpaired (set/difference records prompts)]
    (is (= pre-cutover-record-slugs unpaired)
        (str "the allowlist must equal exactly the current record/prompt "
             "gap, no more no less -- a superset would silently mask a "
             "future session's real miss: expected " pre-cutover-record-slugs
             ", got " unpaired))))

;; -- mechanism-sanity: prove the two real gates' own core logic
;; actually catches bad data, using synthetic sets rather than the
;; live file tree (same shape as
;; ehrt.docs-tooling.reading-set-budget-test's own three mechanism-
;; sanity tests) --

(deftest an-unpaired-record-outside-the-allowlist-is-caught-test
  (let [records #{"2026-01-01-foo" "2026-01-02-bar"}
        prompts #{"2026-01-01-foo"}
        allowlist #{"2026-01-01-foo"} ;; deliberately does not cover "bar"
        unpaired (set/difference records prompts)
        unexpected (set/difference unpaired allowlist)]
    (is (= #{"2026-01-02-bar"} unexpected)
        "an unpaired record not on the allowlist must show up as unexpected -- proves the presence check's own core logic actually flags a real gap, not vacuously true")))

(deftest an-orphaned-prompt-is-caught-test
  (let [records #{"2026-01-01-foo"}
        prompts #{"2026-01-01-foo" "2026-01-02-bar"}
        orphans (set/difference prompts records)]
    (is (= #{"2026-01-02-bar"} orphans)
        "a prompt with no paired record must show up as an orphan -- proves the absence check's own core logic actually flags a real gap, not vacuously true")))
