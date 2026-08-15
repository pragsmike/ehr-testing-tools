(ns ehrt.docs-tooling.stale-path-test
  "P1-1 (2026-07-31 review catch-up, finding 4): a stale-path family
  (pre-Polylith `ehr_testing_tools` paths, `test-integration/`, and a
  `docs/experiments/` link missing its component-adjacent prefix)
  recurred across four live docs -- this tripwire scans every
  docs/**/*.md file plus components/corpus/docs/use-cases.edn (the
  rendered form, docs/use-cases.md, is generated and covered by the
  same scan) so the species can't silently re-accumulate. The
  component-adjacent citation form, `components/corpus/docs/experiments/...`,
  is correct and must NOT trip this -- tested both directions below.

  Stage 3 (ADR-0018, AR-7) retired the tools component and added its
  namespace prefix, `ehrt.tools.`, to the forbidden list: no
  current-tense doc may cite a namespace under the retired prefix.
  Populations, and how each is enumerated (the population-closure law,
  applied to this gate's own scan roots -- review 3 D1-2 found the
  sentence that used to sit here, 'Deliberately scoped: this scan
  covers docs/ ... only', was itself the defect: a gate whose
  population excluded the tree where the violations lived). This
  namespace runs four independent scans over three populations:

  1. The retired-name denylist family (`violations` below) reads
     docs/**/*.md, enumerated by file-seq from `docs`, plus
     components/corpus/docs/use-cases.edn and the four named live
     .agents/ index files. notes/ADRs.md, notes/prompts/, and
     .agents/session-records/ narrate history and legitimately cite the
     old names, and this test never reads them (confirmed at AR-7's own
     request, not assumed).
  2. The README register-code tripwire reads README.md only.
  3. The notes/prompts/ archive-instruction tripwire reads
     docs/**/*.md plus AGENTS.md plus every .agents/skills/**/SKILL.md,
     enumerated by file-seq from `.agents/skills`.
  4. The dead-markdown-link scan (2026-08-15 addendum below) reads
     EVERY *.md under docs/** AND under components/<x>/docs/** -- the
     component doc roots enumerated from the filesystem
     (`component-doc-roots`), never a hand-maintained list, so a
     component gaining a docs/ directory joins the population without
     anyone remembering to add it.

  2026-08-15 addendum (repo review 3, register rows D1-2/D1-8; fix
  session B): the dead-markdown-link scan. Review 3 resolved every
  cited path across the live doc surfaces and found 25 dead markdown
  links, all 25 under components/<x>/docs/ and zero under docs/ --
  precisely the population this namespace's other three scans exclude.
  Two sub-classes: 19 un-re-depthed `../` prefixes (files that moved
  from a repo-root docs/ into components/<x>/docs/ at the merge gained
  two path segments, and their relative prefixes were never adjusted),
  and 6 the register recorded as pointing at genuinely removed targets.
  Both sub-classes are fixed in the same commit this scan lands in, red
  witnessed at exactly 25 first -- the same fix-before-gate discipline
  as every entry in this family.

  The 6 turned out NOT to be a second class (author ruling R-B1,
  2026-08-15, 'Re-point all six'): both targets exist, frozen. These
  docs came from the pre-merge tools repo, where `.agents/memory/
  patterns.md` and `.agents/plans/archive/judge-gate-refactor.md` were
  live sibling paths; the merge froze that whole tree into
  notes/tools/agents/, so the citations are the SAME un-re-depthed
  relocation defect as the other 19, differing only in that the
  destination moved too. `.agents/memory/patterns.md` was never created
  at the post-merge path (that directory has only ever held README.md,
  which says so itself), and the frozen file's numbered pattern 15,
  'Provenance is measured at the point of execution', is verbatim what
  all five citing sentences invoke.

  Deliberately NOT built here, and registered rather than improvised
  (author rulings R-B2 and R-B3, 2026-08-15). (i) This scan resolves
  markdown link destinations only. The root-anchored BACKTICKED path
  half cannot be green against the current tree under D1-8's four
  exclusions: a first-segment-is-a-repo-root-entry reading leaves 216
  dead, 95 even after component-root resolution, and many of those sit
  inside docs/, which D1-2 measured as clean. The residue is dominated
  by a class D1-8 never named -- post-relocation BASENAME shorthand
  (`docs/notation.md` cited from anywhere, meaning that doc wherever it
  now lives) -- alongside backticked command lines, globs, and
  file.clj:21-23 line suffixes. (ii) The retired-name denylist family
  (scan 1 above) keeps its docs/ population: widening it to
  components/<x>/docs/ turns 15 more files red, most of them the same
  basename-shorthand false positive. Both are register rows for a later
  session, with real findings inside them; neither is a silent loosening
  of this gate.

  The false-positive control is load-bearing (register row D1-8 names
  four classes with evidence; a gate that lands noisy gets weakened
  later, so each is encoded here rather than left to a future reader's
  judgment). (a) This repo's shorthand backticked citation convention
  (`sim/run.clj` for the full brick path) is excluded STRUCTURALLY, not
  by list: this scan resolves markdown link destinations only, and a
  backticked citation is never one. (b) Generator template sources
  (components/corpus/docs/use-cases.edn) author their links to resolve
  at the GENERATED output's location, docs/use-cases/, not at the
  template's own -- excluded structurally again, since the scan reads
  *.md and the template is .edn (its rendered output IS in the
  population, and resolves). (c) docs/dev/migration/polylith-brief.md's
  external tutorial examples (projects/billing, components/invoice) cite
  Polylith's own documentation, not this tree, and are excluded by name
  -- the one class that cannot be structural, because those paths are
  shaped exactly like real ones. (d) Percent-encoding is DECODED before
  resolution rather than excluded: components/corpus/docs/experiments.md
  links a research file whose name contains literal spaces via `%20`,
  which resolves in any markdown renderer and must resolve here too.

  2026-08-01 addendum (storefront + ruled literals session, AR-3): a
  second, unrelated tripwire in the same family, scanning README.md
  specifically. README.md is this workspace's storefront, read by
  people who have never seen an ADR number and shouldn't need to --
  internal provenance codes (`ADR-\\d+`, `EXP-[A-Z]?\\d+`, `DOC-\\d+`,
  and bare `D\\d+` ruling codes like source-sink-design.md's D9/D13)
  leaking into its body prose is exactly the kind of internal-logbook
  drift the storefront must not carry. Deliberately narrower than the
  scan above: markdown link destinations (`](...)`) and HTML comments
  are exempt and stripped before matching, because the Maturity
  table's own Evidence-column hrefs legitimately point at files named
  `EXP-A4-results.md` -- a citation, not a leak -- and an editorial
  HTML comment is invisible prose, not storefront-facing text.

  2026-08-01 addendum (agent-ux capture session, `notes/ADRs.md`
  ADR-0023, AR-4): `positioning.md` joins the forbidden-string family
  above -- `docs/dev/positioning.md` was renamed `docs/dev/AUDIENCES.md`
  this session when agents joined its audience register as an explicit
  class, and every live citation across `docs/` was swept to the new
  name. A stray `positioning.md` reference surviving anywhere under
  `docs/**/*.md` is by construction stale -- the file no longer exists
  at that path -- so it is forbidden outright, the same denylist shape
  as `ehrt.tools.` above, not scoped to a prefix or suffix pattern.
  `notes/`'s own historical citations of the old name (pre-rename
  prompts, ADR context) are untouched, out of this test's scan scope,
  same as every other entry in this family.

  2026-08-02 addendum (migration session 2, item 12): a third,
  independent tripwire in the family, forbidding *current-tense
  instruction* that (new) session prompts archive to the now-frozen
  `notes/prompts/` (item 1's own ruling: `.agents/prompts/` is the only
  live destination going forward). Deliberately narrower than a bare
  substring ban on `notes/prompts/` -- the directory's own history is
  legitimately narrated in prose that must keep citing it by name
  (`docs/dev/way-of-working.md`'s own '...the archived session prompt
  under `notes/prompts/` once step 12 lands it' is exactly this: past-
  participle narration of one already-completed session's own
  archival, not an instruction). The line this addendum draws is verb
  tense/mood, not the path token: present-tense/imperative verbs
  (archive(s), land(s), go(es)) immediately governing `notes/prompts/`
  are forbidden; past-participle narration (`archived`) and citations
  of a specific file under the directory are untouched. Scanned over a
  wider source set than the family above (`AGENTS.md` and every
  `.agents/skills/**/SKILL.md` join `docs/**/*.md`) since those, not
  just `docs/`, are this workspace's own current-tense instructional
  surfaces (`.agents/plans/`, `.agents/session-records/`,
  `.agents/prompts/`, and `notes/` itself stay out of scope, same
  narrative-legitimacy reasoning as the family above).

  2026-08-02 addendum (sim split S2, `.agents/plans/2026-08-02-sim-
  split-plan.md`, R-5): `ehrt.sim.gmf` (also catches `ehrt.sim.gmf-
  interpreter` as a substring, intentionally) and `ehrt.sim.compile-
  trajectory` join the retired-namespace family above -- both moved to
  `ehrt.sim-trajectory.*` this session, same denylist shape as
  `ehrt.tools.` (stage 3). Scoped the same as the family above (`docs/`
  plus `components/corpus/docs/use-cases.edn` only); `notes/sim/`'s own
  historical citations of the pre-split namespace stay out of this
  test's scan scope, confirmed before this addendum landed.

  2026-08-02 addendum (sim split S3 / GMF coverage Wave D stage D0,
  `notes/ADRs.md` ADR-0029 R1): `ehrt.sim.emit-hl7`, `ehrt.sim.v2-replay`,
  and `ehrt.sim.site-profile` join the retired-namespace family above --
  all three moved to `ehrt.sim-emit-hl7.*` this session, same denylist
  shape as the S2 addendum immediately above. Two REAL violations
  existed under `docs/` this time (`docs/site-profiles.md`,
  `docs/simulate-your-facility.md` -- both bare-cited `ehrt.sim.site-
  profile`/`ehrt.sim.emit-hl7` as the root user-facing doc's own
  explanation of the site-profiles feature), fixed forward to the
  `ehrt.sim-emit-hl7.*` form in the same commit this addendum lands in,
  confirmed clean before this addendum's own patterns were added --
  same scope and same fix-before-gate discipline as every entry in this
  family.

  2026-08-04 addendum (sim split B stage M2, `notes/ADRs.md` ADR-0043,
  AR-M2-6): `ehrt.sim.engine`, `ehrt.sim.churn`, and `ehrt.sim.order-
  profiles` join the retired-namespace family above -- all three moved
  to `ehrt.sim-engine.*` this session, same denylist shape as the S2/S3
  addenda immediately above (a leading-dot form so `ehrt.sim-engine.
  engine` etc. never trip the retired-prefix pattern the way
  `ehrt.corpus.` never trips `ehrt.tools.` -- confirmed both directions
  below). The path-form citations (`ehrt/sim/engine` etc., the way a doc
  might cite the pre-move file path) join too, same scope. Two REAL
  violations existed under `docs/` this time (`docs/site-profiles.md`,
  bare-citing `ehrt.sim.engine/run` and `ehrt.sim.engine/config-keys`
  in its own naming-transform and dialect-selection sections), fixed
  forward to the `ehrt.sim-engine.engine` form in the same commit this
  addendum lands in, confirmed clean before this addendum's own
  patterns were added -- same scope and same fix-before-gate discipline
  as every entry in this family.

  2026-08-04 addendum (sim split B stage M3, `notes/ADRs.md` ADR-0043,
  AR-M3-6): `ehrt.sim.emit-state` joins the retired-namespace family
  above -- moved to `ehrt.sim-emit-fhir.emit-fhir` this session, same
  denylist shape as the S2/S3/M2 addenda immediately above (`ehrt.sim-
  emit-fhir.` never trips the retired-prefix pattern, since
  `ehrt.sim.emit-state` is not a substring of it -- confirmed below).
  The path-form citation (`ehrt/sim/emit_state`) joins too, same scope.
  This scan (`docs/**/*.md` plus `components/corpus/docs/use-cases.edn`)
  found no real violations -- `components/sim/docs/`'s own deep theory
  docs (sim-theory.md/.edn, event-sourcing.md, the emit-state demo's own
  README) DO carry stale mentions, but that tree is component-owned,
  outside this test's scan scope (same as every other entry in this
  family); those were swept forward anyway, live, as part of this same
  session's own current-tense-surface discipline, just not gated by
  this test.

  2026-08-04 addendum (sim split B stage M4, `notes/ADRs.md` ADR-0043,
  AR-M4-6): `ehrt.sim.check` joins the retired-namespace family above --
  moved to `ehrt.sim-check.check` this session, same denylist shape as
  the S2/S3/M2/M3 addenda immediately above (`ehrt.sim-check.` never
  trips the retired-prefix pattern, since `ehrt.sim.check` is not a
  substring of it -- confirmed below). The path-form citation
  (`ehrt/sim/check`) joins too, same scope. This scan (`docs/**/*.md`
  plus `components/corpus/docs/use-cases.edn`) found no real
  violations -- `components/sim/docs/` (sim-theory.md, patient-state-
  model.md) and `components/sim-trajectory/docs/gmf-interpreter.md` DO
  carry stale mentions, but that tree is component-owned, outside this
  test's scan scope (same as every other entry in this family); those
  were swept forward anyway, live, as part of this same session's own
  current-tense-surface discipline, just not gated by this test.

  2026-08-05 addendum (alignment fixes 1, `notes/ADRs.md` ADR-0050,
  register row S7): the alignment-audit session's own AR-AU-1 fix found
  a stale `ehrt.tools.*` citation sitting in `.agents/plans/roadmap.md`
  -- live, current-tense, frequently-edited, and yet outside every scan
  root above (none of which cover `.agents/`). The gap S7 named: this
  family's scope was drawn around 'frozen archive vs. live docs/' and
  never anticipated a THIRD class -- live, current-tense, non-`docs/`
  planning surfaces. Rather than open the scan to all of `.agents/plans/
  *.md` (which would trip on dated one-shot session prompts and briefs
  that deliberately freeze a moment's prose -- this file's own M2-M4
  addenda document exactly that legitimacy class, and the audit brief
  itself, `.agents/plans/2026-08-05-alignment-audit-brief.md`, quotes
  the roadmap's own THEN-current stale text on purpose), the scope
  widens by an EXPLICIT INCLUDE-LIST of four named files, not a
  directory glob: `.agents/plans/roadmap.md`, `.agents/plans/README.md`,
  `.agents/prompts/README.md`, `.agents/session-records/README.md`.
  These four share one property the dated one-shot files don't: they
  are perpetually-live indexes/plans, edited in place session after
  session, never frozen at authoring time -- the same 'current-tense
  instructional surface' reasoning the `notes/prompts/`-archive
  addendum above already applies to `.agents/plans/`,
  `.agents/session-records/`, and `.agents/prompts/` themselves (as
  directories whose CONTENTS stay out of scope, while these four index/
  plan FILES now join the scan directly). Attic files
  (`.agents/plans/roadmap-done-*.md`) and every other dated one-shot
  plan/prompt/brief file are deliberately NOT included -- they narrate
  history at authoring time, the same legitimacy class `notes/` already
  carries, and an include-list makes that boundary an explicit,
  auditable list rather than an exclude-pattern someone has to reason
  through. Same session: `ehrt.sim-cli.` joins the forbidden-prefix
  family below (ADR-0021's base retirement, 2026-08-01, register row
  E-7 -- never folded into this list before, though no live doc ever
  violated it)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- markdown-files []
  (->> (file-seq (io/file "docs"))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".md"))
       (map #(.getPath %))))

(def ^:private live-agents-plan-files
  "Explicit include-list (S7/AR-F1-3, 2026-08-05 addendum above) --
  perpetually-live index/plan files under .agents/, never a directory
  glob. Dated one-shot files (session prompts, briefs, the roadmap-done-
  *.md attic) stay out of scope on purpose."
  [".agents/plans/roadmap.md"
   ".agents/plans/README.md"
   ".agents/prompts/README.md"
   ".agents/session-records/README.md"])

(defn- scan-sources []
  (concat (markdown-files)
          ["components/corpus/docs/use-cases.edn"]
          live-agents-plan-files))

(defn- violations [content]
  (cond-> []
    (str/includes? content "ehr_testing_tools")
    (conj :ehr-testing-tools-underscore-path)
    (str/includes? content "test-integration/")
    (conj :test-integration-path)
    (re-find #"(?<!corpus/)docs/experiments/" content)
    (conj :docs-experiments-missing-corpus-prefix)
    (str/includes? content "ehrt.tools.")
    (conj :retired-ehrt-tools-namespace)
    (str/includes? content "ehrt.sim-cli.")
    (conj :retired-ehrt-sim-cli-namespace)
    (str/includes? content "positioning.md")
    (conj :retired-positioning-filename)
    (str/includes? content "ehrt.sim.gmf")
    (conj :retired-ehrt-sim-gmf-namespace)
    (str/includes? content "ehrt.sim.compile-trajectory")
    (conj :retired-ehrt-sim-compile-trajectory-namespace)
    (str/includes? content "ehrt.sim.emit-hl7")
    (conj :retired-ehrt-sim-emit-hl7-namespace)
    (str/includes? content "ehrt.sim.v2-replay")
    (conj :retired-ehrt-sim-v2-replay-namespace)
    (str/includes? content "ehrt.sim.site-profile")
    (conj :retired-ehrt-sim-site-profile-namespace)
    (str/includes? content "ehrt.sim.engine")
    (conj :retired-ehrt-sim-engine-namespace)
    (str/includes? content "ehrt.sim.churn")
    (conj :retired-ehrt-sim-churn-namespace)
    (str/includes? content "ehrt.sim.order-profiles")
    (conj :retired-ehrt-sim-order-profiles-namespace)
    (re-find #"ehrt/sim/engine\b" content)
    (conj :retired-ehrt-sim-engine-path)
    (re-find #"ehrt/sim/churn\b" content)
    (conj :retired-ehrt-sim-churn-path)
    (re-find #"ehrt/sim/order_profiles\b" content)
    (conj :retired-ehrt-sim-order-profiles-path)
    (str/includes? content "ehrt.sim.emit-state")
    (conj :retired-ehrt-sim-emit-state-namespace)
    (re-find #"ehrt/sim/emit_state\b" content)
    (conj :retired-ehrt-sim-emit-state-path)
    (str/includes? content "ehrt.sim.check")
    (conj :retired-ehrt-sim-check-namespace)
    (re-find #"ehrt/sim/check\b" content)
    (conj :retired-ehrt-sim-check-path)))

(deftest no-stale-path-family-anywhere-in-docs-or-use-cases-edn-test
  (doseq [path (scan-sources)]
    (let [found (violations (slurp path))]
      (is (empty? found) (str path " carries stale-path residue: " found)))))

(deftest the-component-adjacent-form-does-not-trip-the-tripwire-test
  (testing "components/corpus/docs/experiments/... is the correct citation form"
    (is (empty? (violations "see components/corpus/docs/experiments/EXP-A4-results.md")))))

(deftest scan-sources-includes-exactly-the-ruled-live-agents-plan-files-test
  (testing "the four named live .agents/ index/plan files are in scope (S7/AR-F1-3)"
    (doseq [f live-agents-plan-files]
      (is (some #{f} (scan-sources)) (str f " missing from scan-sources"))))
  (testing "the attic and dated one-shot files stay out of scope, on purpose"
    (is (not (some #{".agents/plans/roadmap-done-2026-08.md"} (scan-sources))))
    (is (not (some #(str/includes? % "alignment-audit-brief") (scan-sources))))))

(deftest each-forbidden-pattern-is-actually-caught-test
  (is (= [:ehr-testing-tools-underscore-path] (violations "test/ehr_testing_tools/foo_test.clj")))
  (is (= [:test-integration-path] (violations "lives on the test-integration/ path")))
  (is (= [:docs-experiments-missing-corpus-prefix] (violations "see docs/experiments/EXP-A4-results.md")))
  (is (= [:retired-ehrt-tools-namespace] (violations "see ehrt.tools.corpus.manifest/ManifestV1_1")))
  (is (= [:retired-ehrt-sim-cli-namespace] (violations "see ehrt.sim-cli.core/-main")))
  (testing "the retired sim-cli prefix does not trip on unrelated live sim-* citation forms"
    (is (empty? (violations "see ehrt.sim-check.check/check-all")))
    (is (empty? (violations "see ehrt.sim-engine.engine/run"))))
  (is (= [:retired-ehrt-sim-gmf-namespace] (violations "see ehrt.sim.gmf/load-module")))
  (is (= [:retired-ehrt-sim-gmf-namespace] (violations "see ehrt.sim.gmf-interpreter/run-module")))
  (is (= [:retired-ehrt-sim-compile-trajectory-namespace] (violations "see ehrt.sim.compile-trajectory/compile-trajectory")))
  (testing "the sim-trajectory citation form does not trip either retired-prefix pattern"
    (is (empty? (violations "see ehrt.sim-trajectory.gmf/load-module")))
    (is (empty? (violations "see ehrt.sim-trajectory.compile-trajectory/compile-trajectory"))))
  (testing "the stage-3 citation form does not trip the retired-prefix pattern"
    (is (empty? (violations "see ehrt.corpus.manifest/ManifestV1_1"))))
  (is (= [:retired-positioning-filename] (violations "see docs/dev/positioning.md for the audience register")))
  (testing "the post-rename citation form does not trip the retired-filename pattern"
    (is (empty? (violations "see docs/dev/AUDIENCES.md for the audience register"))))
  (is (= [:retired-ehrt-sim-emit-hl7-namespace] (violations "see ehrt.sim.emit-hl7/emit")))
  (is (= [:retired-ehrt-sim-v2-replay-namespace] (violations "see ehrt.sim.v2-replay/fold-message")))
  (is (= [:retired-ehrt-sim-site-profile-namespace] (violations "see ehrt.sim.site-profile/code-for")))
  (testing "the sim-emit-hl7 citation form does not trip any retired-prefix pattern"
    (is (empty? (violations "see ehrt.sim-emit-hl7.emit-hl7/emit")))
    (is (empty? (violations "see ehrt.sim-emit-hl7.v2-replay/fold-message")))
    (is (empty? (violations "see ehrt.sim-emit-hl7.site-profile/code-for")))
    (is (empty? (violations "see ehrt.sim-emit-hl7.interface/emit"))))
  (is (= [:retired-ehrt-sim-engine-namespace] (violations "see ehrt.sim.engine/run")))
  (is (= [:retired-ehrt-sim-churn-namespace] (violations "see ehrt.sim.churn/inject")))
  (is (= [:retired-ehrt-sim-order-profiles-namespace] (violations "see ehrt.sim.order-profiles/default-profiles")))
  (is (= [:retired-ehrt-sim-engine-path] (violations "components/sim/src/ehrt/sim/engine.clj")))
  (is (= [:retired-ehrt-sim-churn-path] (violations "components/sim/src/ehrt/sim/churn.clj")))
  (is (= [:retired-ehrt-sim-order-profiles-path] (violations "components/sim/src/ehrt/sim/order_profiles.clj")))
  (testing "the sim-engine citation form does not trip any retired-prefix or retired-path pattern"
    (is (empty? (violations "see ehrt.sim-engine.engine/run")))
    (is (empty? (violations "see ehrt.sim-engine.churn/inject")))
    (is (empty? (violations "see ehrt.sim-engine.order-profiles/default-profiles")))
    (is (empty? (violations "see ehrt.sim-engine.interface/run")))
    (is (empty? (violations "components/sim-engine/src/ehrt/sim_engine/engine.clj"))))
  (is (= [:retired-ehrt-sim-emit-state-namespace] (violations "see ehrt.sim.emit-state/bundle-run")))
  (is (= [:retired-ehrt-sim-emit-state-path] (violations "components/sim/src/ehrt/sim/emit_state.clj")))
  (testing "the sim-emit-fhir citation form does not trip any retired-prefix or retired-path pattern"
    (is (empty? (violations "see ehrt.sim-emit-fhir.emit-fhir/bundle-run")))
    (is (empty? (violations "see ehrt.sim-emit-fhir.interface/bundle-run")))
    (is (empty? (violations "components/sim-emit-fhir/src/ehrt/sim_emit_fhir/emit_fhir.clj"))))
  (is (= [:retired-ehrt-sim-check-namespace] (violations "see ehrt.sim.check/check-all")))
  (is (= [:retired-ehrt-sim-check-path] (violations "components/sim/src/ehrt/sim/check.clj")))
  (testing "the sim-check citation form does not trip any retired-prefix or retired-path pattern"
    (is (empty? (violations "see ehrt.sim-check.check/check-all")))
    (is (empty? (violations "see ehrt.sim-check.interface/check-all")))
    (is (empty? (violations "components/sim-check/src/ehrt/sim_check/check.clj")))))

;; README register tripwire (2026-08-01, AR-3) -- separate from the scan
;; above: different source (README.md only), different exemptions (link
;; destinations and HTML comments, not a path-prefix distinction).

(defn- strip-exempt-spans
  "Strips markdown link destinations (`](...)`) and HTML comments
  (`<!-- ... -->`) from README.md's text before the register-code scan
  below -- both are legitimate places for an internal code to appear
  (the Maturity table's own Evidence-column hrefs, an editorial aside)
  and must not trip the tripwire. Link targets are blanked, not
  deleted, so surrounding prose offsets/structure survive intact."
  [content]
  (-> content
      (str/replace #"\]\([^)]*\)" "]()")
      (str/replace #"(?s)<!--.*?-->" "")))

(def ^:private register-code-re
  #"ADR-\d+|EXP-[A-Z]?\d+|DOC-\d+|\bD\d+\b")

(defn- register-code-violations
  "Every internal provenance-code match in README.md's prose, link
  targets and HTML comments already stripped. Distinct, in match
  order."
  [content]
  (->> (re-seq register-code-re (strip-exempt-spans content))
       distinct
       vec))

(deftest readme-body-carries-no-internal-provenance-codes-test
  (let [found (register-code-violations (slurp "README.md"))]
    (is (empty? found)
        (str "README.md's storefront prose cites internal provenance codes: " found))))

(deftest each-register-code-pattern-is-actually-caught-test
  (is (= ["ADR-0012"] (register-code-violations "ratified in ADR-0012, see below")))
  (is (= ["EXP-A4"] (register-code-violations "results in EXP-A4 confirm this")))
  (is (= ["EXP-5"] (register-code-violations "results in EXP-5 confirm this")))
  (is (= ["DOC-5"] (register-code-violations "landed under DOC-5")))
  (is (= ["D9"] (register-code-violations "the zero-flag defaults, D9"))))

(deftest link-destinations-and-html-comments-are-exempt-test
  (testing "a real Evidence-column href citing an EXP results file"
    (is (empty? (register-code-violations
                  "[Byte-reproducibility proof](components/corpus/docs/experiments/EXP-A4-results.md) in a clean environment."))))
  (testing "an HTML comment"
    (is (empty? (register-code-violations "<!-- ADR-0012 predates this rename -->"))))
  (testing "the same code outside both exemptions still trips it"
    (is (= ["ADR-0012"] (register-code-violations "predates ADR-0012, unlike the comment above")))))

;; notes/prompts/ archive-instruction tripwire (2026-08-02, item 12) --
;; separate again: different source set (docs/**/*.md + AGENTS.md +
;; every skill's SKILL.md), different shape (a verb-tense-scoped
;; pattern, not a path-prefix or exact-string ban).

(defn- skill-md-files []
  (->> (file-seq (io/file ".agents/skills"))
       (filter #(.isFile %))
       (filter #(= "SKILL.md" (.getName %)))
       (map #(.getPath %))))

(defn- notes-prompts-archive-sources []
  (concat (markdown-files) ["AGENTS.md"] (skill-md-files)))

(def ^:private archive-instruction-re
  #"(?i)\barchives?\s+(?:to|in)\s+notes/prompts|\blands?\s+(?:in|at)\s+notes/prompts|\bgoes?\s+to\s+notes/prompts")

(defn- archives-to-notes-prompts?
  "True when `content` gives a present-tense/imperative instruction that
  (new) work archives, lands, or goes to notes/prompts/ -- the retired
  destination, per item 1's ruling. Past-participle narration
  ('archived ... under notes/prompts/') and citations of a specific
  file under the directory do not trip this."
  [content]
  (boolean (re-find archive-instruction-re content)))

(deftest no-current-instruction-archives-to-notes-prompts-test
  (doseq [path (notes-prompts-archive-sources)]
    (let [content (slurp path)]
      (is (not (archives-to-notes-prompts? content))
          (str path " instructs archiving to the retired notes/prompts/ destination -- .agents/prompts/ is the only live one (item 1, 2026-08-02)")))))

(deftest archive-instruction-pattern-is-actually-caught-test
  (testing "present-tense/imperative instruction trips it"
    (is (archives-to-notes-prompts? "New session prompts archive to notes/prompts/ from here on."))
    (is (archives-to-notes-prompts? "Prompts land in notes/prompts/ going forward.")))
  (testing "past-participle narration of one already-completed session's own archival does not trip it"
    (is (not (archives-to-notes-prompts?
               "see the archived session prompt under `notes/prompts/` once step 12 lands it"))))
  (testing "a citation of a specific file does not trip it"
    (is (not (archives-to-notes-prompts?
               "see notes/prompts/2026-07-30-ehr-testing-doctor-rendering.md for the prompt")))))

;; Dead-markdown-link scan (2026-08-15, review 3 D1-2/D1-8) -- the fourth
;; scan, over the widest population in this namespace: every *.md under
;; docs/** AND components/<x>/docs/**. See the population statement and
;; the four-class false-positive control in the ns docstring above.

(defn- component-doc-roots
  "Every components/<x>/docs directory that exists, enumerated from the
  filesystem rather than a hand-maintained list. A component gaining a
  docs/ directory joins the link-scan population by existing -- the
  population-closure law, which review 3 D1-2 found this namespace's own
  scan root violating."
  []
  (->> (.listFiles (io/file "components"))
       (filter #(.isDirectory %))
       sort
       (map #(io/file % "docs"))
       (filter #(.isDirectory %))))

(defn- md-files-under [dir]
  (->> (file-seq dir)
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".md"))
       (sort-by #(.getPath %))))

(defn- link-scan-files
  "The dead-link scan's population: docs/**/*.md plus every
  components/<x>/docs/**/*.md."
  []
  (concat (md-files-under (io/file "docs"))
          (mapcat md-files-under (component-doc-roots))))

(def ^:private external-tutorial-example-files
  "False-positive class (c), register row D1-8 -- the ONE class that
  cannot be excluded structurally. This brief walks through Polylith's
  own documentation examples (projects/billing, components/invoice,
  bases/billing-rest-api), which are shaped exactly like this tree's
  real paths but describe someone else's repository."
  #{"docs/dev/migration/polylith-brief.md"})

(def ^:private markdown-link-re
  "Markdown link/image destinations: the `](target)` form. Destinations
  stop at whitespace, so a `](path \"Title\")` form yields just the path."
  #"\]\(([^)\s]+)\)")

(defn- percent-decode
  "Decodes %XX escapes -- false-positive class (d), an encoding step
  rather than an exclusion: `%20` is how a markdown link spells a
  literal space, and the target it names really is on disk. Deliberately
  not URLDecoder/decode, which would also turn `+` into a space and
  corrupt any path legitimately containing one."
  [s]
  (str/replace s #"%([0-9A-Fa-f]{2})"
               (fn [[_ hex]] (str (char (Integer/parseInt hex 16))))))

(defn- repo-root-entries []
  (->> (.listFiles (io/file ".")) (map #(.getName %)) set))

(defn- root-anchored?
  "True when `target` is written from the repo root -- no ./ or ../
  prefix, and a first segment naming something that actually exists at
  the repo root. Such a link is resolved from the repo root as well as
  from the citing file's own directory; everything else resolves from
  the citing file's directory only, so resolution never depends on
  anything outside this repository."
  [target]
  (and (not (str/starts-with? target "/"))
       (not (str/starts-with? target "./"))
       (not (str/starts-with? target "../"))
       (contains? (repo-root-entries) (first (str/split target #"/")))))

(defn- dead-link-target?
  "True when `target`, cited from a file in `dir`, resolves to nothing."
  [dir target]
  (let [path (-> target (str/split #"#") first percent-decode)]
    (and (seq path)
         (not (re-find #"^(?:[a-zA-Z][a-zA-Z0-9+.-]*:|//|#)" target))
         (not (.exists (io/file dir path)))
         (not (and (root-anchored? path) (.exists (io/file path)))))))

(defn- dead-links-in
  "Every dead markdown link in one file, as {:file :line :target}."
  [file]
  (let [path (.getPath file)
        dir (.getParentFile file)]
    (when-not (contains? external-tutorial-example-files path)
      (->> (str/split-lines (slurp file))
           (map-indexed vector)
           (mapcat (fn [[idx line]]
                     (for [[_ target] (re-seq markdown-link-re line)
                           :when (dead-link-target? dir target)]
                       {:file path :line (inc idx) :target target})))))))

(defn- all-dead-links []
  (mapcat dead-links-in (link-scan-files)))

(deftest no-dead-markdown-links-on-any-tracked-doc-surface-test
  (let [dead (all-dead-links)]
    (is (empty? dead)
        (str "dead markdown links (" (count dead) "):\n"
             (str/join "\n" (map #(str "  " (:file %) ":" (:line %)
                                       "  -> " (:target %))
                                 dead))))))

(deftest link-scan-population-is-enumerated-from-the-tree-test
  (testing "every components/<x>/docs directory that exists is in the population"
    (let [paths (set (map #(.getPath %) (link-scan-files)))]
      (doseq [root (component-doc-roots)]
        (is (some #(str/starts-with? % (.getPath root)) paths)
            (str (.getPath root) " contributes no file to the link-scan population")))))
  (testing "docs/ and the component doc roots are both represented"
    (let [paths (map #(.getPath %) (link-scan-files))]
      (is (some #(str/starts-with? % "docs/") paths))
      (is (some #(re-find #"^components/[^/]+/docs/" %) paths)))))

(deftest dead-link-resolution-is-actually-caught-test
  (let [dir (io/file "components/sim/docs")]
    (testing "a link resolving from the citing file's own directory is live"
      (is (not (dead-link-target? dir "sim-theory.md"))))
    (testing "an un-re-depthed ../ prefix (the review-3 D1-2 defect) is dead"
      (is (dead-link-target? dir "../notes/facts-register.md")))
    (testing "the correctly re-depthed form is live"
      (is (not (dead-link-target? dir "../../../notes/facts-register.md"))))
    (testing "a root-anchored target resolves from the repo root too"
      (is (not (dead-link-target? dir "notes/facts-register.md")))
      (is (dead-link-target? dir "notes/no-such-register.md")))
    (testing "external and in-page targets are never resolved as paths"
      (is (not (dead-link-target? dir "https://example.invalid/x.md")))
      (is (not (dead-link-target? dir "mailto:nobody@example.invalid")))
      (is (not (dead-link-target? dir "#a-heading-on-this-page"))))
    (testing "a fragment on a real file resolves to the file"
      (is (not (dead-link-target? dir "sim-theory.md#global-laws"))))))

(deftest each-false-positive-class-is-encoded-test
  (testing "(a) shorthand backticked citations are excluded structurally"
    (is (empty? (re-seq markdown-link-re "the runner lives in `sim/run.clj`, see it")))
    (is (= ["x.md"] (map second (re-seq markdown-link-re "a real [link](x.md) here")))))
  (testing "(b) the generator template source is not in the link population"
    (let [paths (set (map #(.getPath %) (link-scan-files)))]
      (is (not (contains? paths "components/corpus/docs/use-cases.edn")))
      (is (some #(str/starts-with? % "docs/use-cases/") paths)
          "the template's RENDERED output is in the population, and resolves")))
  (testing "(c) polylith-brief.md's external tutorial examples are excluded by name"
    (is (contains? external-tutorial-example-files "docs/dev/migration/polylith-brief.md"))
    (is (empty? (dead-links-in (io/file "docs/dev/migration/polylith-brief.md"))))
    (testing "the same shape of target is still dead when cited from any other file"
      (is (dead-link-target? (io/file "docs/dev") "projects/billing/deps.edn"))))
  (testing "(d) percent-encoding is decoded, not excluded"
    (is (= "a b/c d.md" (percent-decode "a%20b/c%20d.md")))
    (is (= "a+b.md" (percent-decode "a+b.md")))
    (is (not (dead-link-target?
               (io/file "components/corpus/docs")
               "research/License%20Status%20of%20NIST%20HL7%20v2%20Validation%20Software%20%20Evidence-Based%20Classification.md")))))
