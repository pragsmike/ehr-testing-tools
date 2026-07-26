# Plan: User Documentation (DOC waves)

**Status (2026-07-25).** Adopted as an active driver plan following a
documentation audit (design-channel session, 2026-07-25; the audit's
findings are compressed into this document so it stands alone —
evidence over memory). No wave has executed yet. Tracker table at the
bottom; one prompt-session per wave, prompts archive to
`.agents/prompts/archive/` as usual.

**Companions:** `docs/positioning.md` (owns the audience definitions;
DOC-2 extends them), `docs/README.md` (the reading-order spine DOC-2
reshapes), `docs/use-cases.md` / `docs/use-cases.edn` (DOC-4's
target), `README.md` (quickstart — DOC-5's extraction source),
`SETUP.md` (**out of scope**: externally validated by the trial
cohort's 15-minute result; nothing below touches it except
cross-links).

**Goal served:** the run-the-tools audiences can find their use case,
type the commands for it, and read the outputs — without opening
Clojure source or contributor-register documents (ADRs, the design
doc). The organizing finding: most user-needed knowledge already
exists but in the wrong register; the work is extraction and
re-expression, plus one code wave (CLI help) and one enforcement wave
(executable quickstart), not research.

**Operating rules** (inherited from the house discipline): one
semantic change per commit; golden check
(`make pipeline && make use-cases && make operators-doc && make
cli-doc && git diff --exit-code docs/pipeline.md docs/use-cases.md
docs/operators.md docs/cli.md` — extended by DOC-3 from the
two-target form these rules were written against) proves
behavior-neutral sessions and trips on scope creep; evidence over
memory — every claim about the current CLI/doc surface is re-verified
against the repo at session time, not taken from this plan.

---

## Audience register (provisional home)

**Status (2026-07-25, DOC-2):** canonical in `docs/positioning.md`
§Audience as of DOC-2; this list is the adoption-time snapshot the
plan was drafted against, not the source of truth going forward — read
positioning.md for the current register.

Seven audiences. `docs/positioning.md` §Audience owns three of them
today; DOC-2 makes positioning.md the canonical home for all seven
and this section then defers to it (status line here, not deletion).

1. **Task-first practitioner** — EHR domain expert, Python-comfortable,
   not-Clojure, agent-assisted, Windows 11/WSL2 (the trial cohort).
   Primary audience; currently the worst-served path past the
   quickstart's literal commands.
2. **Method-first guide reader** — arrives from `ehr-testing-guide`;
   wants method → capability. Deliberately underserved until first
   release (positioning's referral trigger).
3. **Contributor** — best-served audience (AGENTS.md,
   AUTHORS-GUIDE.md, ADRs, facts register, design doc). Out of scope.
4. **AI assistant as reader** — implicit but load-bearing: SETUP.md's
   copy-paste prompt and the cohort's agent-assisted default make the
   agent a first-class doc consumer. Wants exact commands, stable
   anchors, self-explanatory errors. Named nowhere today; DOC-2 fixes
   that.
5. **Downstream data consumer** — Python/SQL reader of `report.edn`,
   manifests, lineage records; never runs the tools. README's
   "readable from Python" promise currently has no format reference
   behind it (`--json` exists; its shape is also undocumented).
6. **Clojure library consumer** — post-release; will `require`
   namespaces. Zero API docs beyond source docstrings, no
   public-vs-internal demarcation. Mostly deferred (cljdoc rides on
   Clojars coordinates); the docstring demarcation convention is
   cheap and can land in any wave.
7. **Evaluator / decision-maker** — deciding whether to adopt.
   Well served already (README maturity table, scope fence, problem
   statement).

## Audit findings the waves answer (2026-07-25)

- **No CLI help surface.** No `ehr help`, no `--help`, no usage text;
  bare `ehr` and unknown commands return a bare EDN
  `:unknown-command` map (exit 2). The complete flag surface
  (`--treat-no-verdict-as`, `--baseline`, `--report`, `--json`,
  `--seed`, …) is documented only in README quickstart comments and
  source. → DOC-1.
- **No user-facing operator catalog.** The five FHIR + five v2
  operator IDs live in `corpus/operators.clj` (registry data +
  docstrings); `judge-calibration.md` covers conviction but is
  organized by judge tier, not "what can I break." → DOC-1 (listing
  verb) + DOC-3 (reference page).
- **No user-facing locator reference.** FHIR path syntax appears once
  as a README comment; the v2 grammar (`SEG`…`SEG-F.C.S`, MSH
  off-by-one) exists only in `locator.clj`. → DOC-3.
- **No output-format reference.** Report / manifest / lineage /
  judgment shapes are Malli schemas in source; semantics are in
  ADR-0009/0010 and the design doc — contributor register. → DOC-3.
- **Zero commands in `use-cases.md`.** 811 lines, 14 use cases, no
  `ehr` invocation anywhere; the equation → shell bridge exists only
  for the ~3 cases the quickstart happens to cover. → DOC-4.
- **Single-spine reading order.** `docs/README.md` serves the
  method-first reader; the other audiences need per-audience entry
  paths. → DOC-2.
- **Doc-rot exposure.** ENF-1's freshness gate covers generated docs;
  README's quickstart commands are unverified prose — the classic rot
  site. → DOC-5.
- **Error messages are documentation.** The enumerable-options error
  family (unknown command/action, `:invalid-operator`) names nothing
  valid. → DOC-1 (bounded pass).

---

## DOC-1 — CLI help surface (code)

**Done (2026-07-25).** Landed as six commits (Step 0 inventory through
Step 5 close-out; prompt archived at
`.agents/prompts/archive/2026-07-25-doc1-cli-help.md`). Itemized:

- `ehr-testing-tools.cli.help`: one `cli-spec` data structure (every
  group/verb, flags with one-line docs and defaults, gate/check
  positional conventions, the 0/1/2/3 exit-code table citing
  ADR-0004/0010) plus pure renderers (`render-top-level`,
  `render-group`) -- text out, no parsing change. A coverage test
  cross-checks the spec's [group verb] pairs against `dispatch`'s own
  routing in both directions.
- `ehr help`, `ehr help <group>`, and `--help` anywhere print that
  plain text (exit 0) via `main!`'s injectable `println-fn`, bypassing
  `render`/`--json` entirely -- the one deliberate EDN-out exception,
  documented in `cli.clj`'s ns docstring. Bare `ehr` prints the same
  top-level text but stays exit 2 (an incomplete invocation is still
  an operational error). `--help` short-circuits before any capability
  function runs.
- `ehr corpus operators`: a pure registry read (`corpus.operators`'s
  in-memory catalog), `:id`/`:format`/`:version`/`:locator-required?`/
  contract `:type`/`:target` per row, optional `--format` filter,
  sorted `[format id]`, ordinary `result/ok` so `--json` works for
  free.
- Bounded error-message pass: dispatch's four `:unknown-command` sites
  (unknown group; unknown action under artifact/corpus/gate) gain
  `:valid-options` (sourced from the same `cli-spec` `ehr help`
  renders from) and `:hint "run: ehr help"`. `mutate-command`'s
  `:unknown-operator` gains `:valid-options` (all registered operator
  ids) and `:hint "run: ehr corpus operators"`. Every category is
  unchanged -- proven by tests at each site.
- README's Quickstart gained exactly two lines (`make ehr ARGS="help"`
  at the top; a pointer to `ehr corpus operators` by the mutate
  example); `make help`'s `ehr` line gained a pointer to `ehr help`.
  No other README prose touched.
- Full suite (439 tests), both lints, and the golden check
  (`make pipeline && make use-cases`, no diff) all green at close-out;
  coverage 90.49% forms / 93.85% lines, above the 85% floor. The new
  surface was run for real (`help`, `help gate`, `corpus operators`,
  a wrong verb) and confirmed exit codes 0/0/0/2 by eye.

**Report to the author -- ruling-vs-source discrepancy found at Step
0:** the DOC-1 prompt's Step 4 ruling names `:invalid-operator` and
"operators.clj's validation" as the site to extend with valid IDs "for
the requested format." The actual `:invalid-operator` site
(`corpus/operators.clj`'s `register!`) is a schema check on hardcoded
seed-catalog entries at namespace-load time -- never reachable from a
user's CLI invocation, and entries there already carry their own
`:format`, so there is no "requested format" to enumerate against. The
CLI-reachable equivalent -- "you named an operator id/version that
doesn't exist" -- is `cli.clj`'s own `:unknown-operator`
(`mutate-command`), which this session extended instead (see the Step
4 commit body for the full reasoning). `operators.clj`'s
`:invalid-operator` was left untouched. Also from Step 0: no README
quickstart flag was found to name something the source doesn't read,
or vice versa, beyond what this wave itself added.

The one code wave, first because it makes every later doc shorter
(reference pages can say "run `ehr help gate`" instead of duplicating
flag tables) and because it serves audiences 1 and 4 at once.

Scope: `ehr help` / `ehr help <group>` / `--help` anywhere → plain
human text (not EDN), exit 0; bare `ehr` → usage text, exit 2 (an
incomplete invocation stays an error; ADR-0004's exit-code contract
untouched). Help text is **data-first**: one spec structure (verbs,
flags, one-line docs, the 0/1/2/3 exit-code table) rendered to text —
the registry pattern, and DOC-3's `cli.md` can later be generated
from it. An operator-listing verb reads `corpus/operators.clj`'s
registry (normal EDN result, `--json` works). A bounded
error-message pass: the enumerable-options family only (unknown
command/action gains valid options + a help pointer;
`:invalid-operator` names the valid IDs for the format). Existing
verb behavior does not change — any existing test needing edits means
the contract moved: stop and report.

Decisions: listing-verb name (author; recommendation
`ehr corpus operators`); error-pass inclusion (recommended in,
bounded as above).

## DOC-2 — Audience map + docs re-spine (docs only)

Consolidate the seven audiences into `docs/positioning.md` §Audience
(it owns three already; the agent-as-reader audience gets its
deliberate sentence). Rewrite `docs/README.md` from single spine to
per-audience entry paths — 3–4 steps each, task-first practitioner
first. This plan's audience register gains a status line deferring to
positioning.md. Cheap, orients everything after; no code, golden
check must show no generated-doc drift.

**Done (2026-07-25).** Landed as four commits (prompt archived at
`.agents/prompts/archive/2026-07-25-doc2-audience-respine.md`).
Itemized:

- `docs/positioning.md` §Audience: three segments become seven,
  keeping the original three's text substantively intact. Added: the
  AI assistant as reader (cites DOC-1's help surface and
  enumerable-options error family as its first deliberate serving),
  the downstream data consumer (`--json`, judge-calibration's
  No-verdict/Reading-this-table sections, the formats gap named with
  a pointer to this plan's DOC-3 wave rather than left dead), the
  Clojure library consumer (cross-linked to the existing
  Go-public-gate-vs-first-release section rather than restated), and
  the evaluator/decision-maker (named against README's maturity
  table, Scope section, and the problem statement). One sentence
  states the section's new role as the canonical register.
- `docs/README.md`: reshaped from the single reading-order spine into
  one entry path per audience that arrives at docs at all (task-first
  practitioner, method-first guide reader, AI assistant, downstream
  data consumer, contributor, evaluator; the Clojure library consumer
  gets one line pointing at positioning.md instead of a path — nothing
  to walk yet). The original eight-step spine (0 through 8) survives
  verbatim, retitled "The deep walk: pipeline-first reading order" —
  the method-first path's second and final step. Every relative link
  and heading anchor in both edited files was mechanically checked
  against the files on disk (a small Python script run under WSL,
  since Windows had no working `python3`) — all resolved; nothing
  found to name a document that isn't there.
- `AUTHORS-GUIDE.md` gained one short section (§6, after the existing
  placeholder §5): the same three agent-legibility preferences
  restated as authoring guidance for future doc sessions.
- **Record repair riding along:** DOC-1's "Landed as five commits" was
  wrong — `git log` shows six (`230344f` inventory through `01371c1`
  close-out); corrected above.
- Golden check (`make pipeline && make use-cases`, no diff), full
  suite, and both lints all green — this session touched no generated
  doc and no code, confirmed rather than assumed.

## DOC-3 — Reference docs

- `docs/cli.md` — command reference. Thin if DOC-1's help-spec is
  authoritative: anchors the docs tree, points into `ehr help`,
  carries only what help text can't (worked examples, exit-code
  discussion).
- `docs/locators.md` — FHIR path grammar + v2 grammar (MSH
  convention, examples per operator), extracted from `locator.clj` /
  `corpus/er7.clj` and re-expressed for audience 1.
- `docs/operators.md` — the catalog: id, format, contract target,
  locator-required?, dropped candidates (pointing at
  judge-calibration.md's CAL-1 section for the why).
- `docs/formats.md` — report / manifest / lineage / judgment shapes
  (EDN and the `--json` projection), field semantics in user
  register, citing the Malli schemas and ADR-0009/0010 rather than
  restating them.

Decision (author): generated vs. hand-written. Recommendation:
**generate `operators.md` from the registry** (it is literally data;
the pipeline.md/use-cases.md renderer-plus-freshness-gate pattern
applies directly, ~one extra renderer) and **hand-write `formats.md`
with schema citations** (Malli → prose rendering is a bigger lift
than it looks). `cli.md` generated from DOC-1's help-spec if that
spec proves rich enough; otherwise hand-written and thin.

## DOC-4 — Runnable strips for use cases

Each internally-drivable use case in `docs/use-cases.edn` gains its
command sequence; external-stage cases get explicit "you bring"
stubs. Decision (author): a `:commands` field in `use-cases.edn` +
renderer extension (single source of truth, freshness-gated —
recommended) vs. a separate `docs/cookbook.md` cross-linked per case
(cheaper, second place to rot). Tripwire either way: commands are
verified by actually running the internally-drivable ones once,
locally, before they're committed as documentation.

## DOC-5 — Executable quickstart (enforcement)

README's quickstart extracted to a script (`make quickstart-demo` or
similar), wired into the nightly `integration.yml` (it fetches
artifacts — nightly tier, never per-push, per ENF-1's fast/slow
split). README's code block either includes-by-reference or gains a
freshness check against the script. ENF-1's sibling: enforcing
existing green behavior, not authoring new checks.

## Deferred register (not scheduled; ride with first release)

- **cljdoc** — automatic once Clojars/Maven coordinates exist
  (`docs/positioning.md` open decision; the First-release row in
  `corpus-foundations.md`).
- **Public/internal namespace demarcation** — a docstring convention;
  near-zero cost, may slot into any wave above opportunistically.
- **Guide → tools cross-references** — positioning's referral
  trigger; waits for release by design.
- **"Since version" maturity markers** — premature before a first
  version tag exists.

## Tracker

| Wave | Deliverables | Status | Prompt |
|---|---|---|---|
| DOC-1 | `ehr help` surface (data-first spec, plain-text render, exit codes documented), operator-listing verb, bounded error-message pass | **Done** (2026-07-25) | `.agents/prompts/archive/2026-07-25-doc1-cli-help.md` |
| DOC-2 | Seven-audience register canonical in `positioning.md`; `docs/README.md` per-audience entry paths | **Done** (2026-07-25) | `.agents/prompts/archive/2026-07-25-doc2-audience-respine.md` |
| DOC-3 | `docs/cli.md`, `docs/locators.md`, `docs/operators.md`, `docs/formats.md` | Not started | — |
| DOC-4 | Per-use-case runnable command strips (route: `:commands` in use-cases.edn vs. cookbook — open) | Not started | — |
| DOC-5 | Quickstart-as-script, nightly-wired; README freshness link | Not started | — |

## Open decisions

- **Operator-listing verb name** (author; blocks DOC-1's Step 3;
  recommendation: `ehr corpus operators`).
- **DOC-3 generated vs. hand-written**, per document (recommendation
  recorded in the DOC-3 section).
- **DOC-4 route**: `:commands` field vs. cookbook (recommendation:
  the field).
- **Sequencing against first release**: a full CLI reference softly
  hardens interfaces the README still labels pre-release; whether
  DOC-3 waits for, rides with, or precedes the release session is the
  author's call. Nothing in DOC-1/2 depends on it.
