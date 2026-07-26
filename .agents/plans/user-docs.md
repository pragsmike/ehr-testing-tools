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

**Done (2026-07-25).** Landed as six commits, both phases complete
(prompt archived at
`.agents/prompts/archive/2026-07-25-doc3-reference-docs.md`).
Itemized:

- **Step 0, evidence.** `cli-spec` verified group-by-group against the
  option keys each command function actually destructures: rich enough
  everywhere except one flag. `corpus generate` reads `:lockfile-path`
  — a plain string that works from the shell — but `generate!`'s
  docstring lists it among the function-valued injection seams, so
  DOC-1's inventory missed it; added to the spec (spec-only, help grew
  one line, no test needed editing). Deliberately not added:
  `:jvm-args`/`:extra-args`, both vector-valued and so not expressible
  as a CLI argument. Registry keys confirmed from the live registry
  (ten entries, six keys each, `:doc` absent). Golden-check statement
  sites enumerated by grep and split into live statements (three) and
  historical records (nine) — see the Step 3 bullet.
- **Step 1, `:doc`.** `register!`'s `Operator` schema gains an optional
  `:doc`; all ten seed entries carry one. The register is deliberately
  distinct from `:contract/:target`'s: `:doc` states the edit (what
  changed in the file), `:target` states the conformance claim (which
  base-spec constraint the result violates). `ehr corpus operators`
  output is unchanged — confirmed by running it, not by reading the
  code.
- **Step 2, renderers.** New `ehr-testing-tools.docsgen`, sibling to
  `.pipeline`/`.usecases`: pure `render-*` plus `-X`-invokable
  `write-*!`. `make operators-doc` → `docs/operators.md` (per format: a
  summary table into per-operator sections carrying doc sentence,
  version, locator requirement, contract; closing pointers to
  judge-calibration.md for measured blind spots and dropped candidates,
  and to locators.md). `make cli-doc` → `docs/cli.md` (synopsis, global
  flags, exit codes, per-group sections with positional conventions and
  per-verb flag tables; no worked invocations, per the ruling — those
  are DOC-4's). Both wholly generated, both carrying the house banner
  and the pre-release line. `sorted-entries` sorts by `[format id]`
  because the registry is an atom-held map with unspecified val order —
  without it the freshness gate would test iteration order. Idempotence
  verified by running both targets twice and diffing.
- **Step 3, gate.** CI's freshness step covers four files. The
  incantation was extended at the three sites that state it as a live
  instruction (ci.yml, this plan's Operating rules, the 2026-07-25
  handoff), each saying so explicitly rather than presenting the new
  form as though it had always been. The nine historical statements —
  `corpus-foundations.md`'s ENF-1 row, this plan's DOC-1/DOC-2
  close-outs, six spent prompts — were left alone: the
  supersede-never-revert discipline applies to records too, and editing
  them would make the repo claim ENF-1 shipped a gate it didn't.
  Tripwire did not fire; `git status` after the change showed exactly
  ci.yml plus the two statement sites.
- **Step 4, `docs/locators.md`.** Both grammars in user register, cited
  to `locator.clj`/`er7.clj`. Three sharp edges found by probing the
  parsers rather than reading their docstrings, all now documented:
  `MSH-1` parses but resolves onto `MSH-2`'s position (it sits below
  the N≥2 shift), so it silently addresses the encoding characters; a
  component-level locator resolves at its field, because the substrate
  is field-granular; and a trailing dot in a FHIR path is silently
  ignored rather than refused, unlike the fully anchored v2 grammar.
  All 40 example locators pinned by `locators_doc_test` — 9 tests, 97
  assertions, in the ordinary `make test`.
- **Step 5, `docs/formats.md`.** Report, check report, manifest,
  lineage, `--json`, in audience-5 register, every field table citing
  its Malli schema and every shape backed by a dated real capture. The
  FHIR gate needed no schema-derived fallback: the artifact cache was
  warm, so a live `ehr gate fhir` run against a real mutant bundle
  supplied it (6554 findings, all three `:disposition` arms). Four
  consumer traps stated: `--report` and stdout are different shapes and
  `--report` is always EDN; `--baseline` changes the payload's type;
  `:schema-version` is a string on current manifests and an integer on
  the two frozen older ones; `:disposition`/`:cause` are FHIR-only.
- Extended golden check clean, full suite (466 tests / 1391
  assertions, from 439/1107 at DOC-2's close) and both lints green at
  close-out. The 27 new tests: 16 in `docsgen-test`, 9 in
  `locators-doc-test`, 2 in `operators-test`.

  *[Record check, DOC-4 ride-along 2026-07-26: this line is correct as
  written. Re-measured at DOC-3's own close commit (`75ddbeb`,
  extracted with `git archive` into a clean tree, `clojure -X:test`):
  466 tests / 1391 assertions, 0 failures. LOC-1's 467/1403 is not a
  competing baseline for the same commit — it appears in `1f43235`'s
  body as the red-phase count* after *LOC-1's own new tests were
  added. Nothing corrected; the annotation is the repair.]*

**Report to the author — one code-shaped finding, deliberately not
acted on:** the FHIR and v2 locator grammars disagree about a trailing
separator. `PID-3.` is rejected (`v2-path-re` is fully anchored);
`entry[0].resource.` is *accepted*, and parses as
`entry[0].resource` — `clojure.string/split`'s default limit discards
the trailing empty token before `fhir-data-path`'s own
`(some empty? segments)` guard ever sees it, so the guard can't fire.
Documented in `docs/locators.md` as a sharp edge with a "don't rely on
it," which is the honest thing for a docs session to do; whether the
two grammars should agree is a code question and a separate change.

*[Answered by the author 2026-07-25 — option (b), fix the code. See the
LOC-1 section below; the report above stands as the record of what
DOC-3 found and deliberately left alone.]*

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

## LOC-1 — Locator grammar micro-wave (interlude)

Not a DOC wave: a code change, slotted between DOC-3 and DOC-4 because
DOC-4's runnable strips should not be written against wart-y behavior.
**Decided by the author 2026-07-25**, option (b) of the DOC-3 close-out
report above: fix the code rather than keep documenting the warts.
Pre-release is exactly when a grammar wart is cheap to fix — nothing is
tagged, nothing is on Clojars, and both changes only *narrow* the
accepted-input surface, so no valid locator anyone has written stops
working.

**Done (2026-07-25).** Five commits, prompt archived at
`.agents/prompts/archive/2026-07-25-loc1-locator-grammar.md`. Two
deliberate behavior changes, both at parse time in `locator.clj` —
the locator string alone decides both conditions, so both fail before
any file I/O:

- **A trailing separator is a parse error in FHIR paths too.**
  `entry[0].resource.` used to parse as `entry[0].resource`, not
  because the grammar admitted it but because `clojure.string/split`'s
  default limit discarded the trailing empty token before
  `fhir-data-path`'s own `(some empty? segments)` guard could see it —
  the guard was written for exactly this and was unreachable for
  exactly this. Split limit `-1` brings it back to life; no second
  guard was added. Both grammars are now anchored at both ends, which
  is the point.
- **`MSH-1` is refused, and the refusal teaches.** It used to parse
  like any other field locator and then resolve onto `MSH-2`'s slot
  (`corpus.er7/field-index` shifts only for *N* ≥ 2), silently
  addressing the encoding characters — a successful mutation of the
  wrong field, the worst kind of success. Now refused at parse for any
  string naming MSH's field 1. The check is MSH-specific: `PID-1`,
  `ZZ1-1`, `OBX[2]-1` still parse, and segment-level `MSH` is
  untouched. `corpus.er7` was not modified — the substrate still maps
  field 1 as it always did; what changed is that no locator can ask it
  to.

Categories preserved, payloads enriched (author ruling): the FHIR
rejection flows through `:invalid-fhir-path` unchanged; the `MSH-1`
refusal stays `:invalid-v2-path` and gains a `:hint` in DOC-1's
enumerable-options house pattern, written from `corpus.er7`'s own
account of the delimiter convention. Callers dispatching on
`:category` see nothing new, and a test proves both categories
survived.

`docs/locators.md`'s two stale sharp-edge sections became grammar
facts, dated. The didactic content survived by ruling: the whole
account of *why* `MSH` is off by one — the header declaring its own
delimiters, the *N* ≥ 2 shift, the worked resolution table — is
teaching material and stayed; only the "don't rely on it" framing
around behavior that no longer exists was rewritten. The hint is
block-quoted verbatim and pinned as a string equality in
`locators_doc_test`, so the doc's quote can't drift from the parser's
wording.

**Out of scope by ruling: component-granularity.** DOC-3's third sharp
edge — `PID-5.1` parses and then resolves at its field, because the v2
substrate is field-granular — is still true and stays documented as
is. The v2 grammar is *intentionally* ahead of the substrate, parked
the way `lower`/`erase` are parked: the grammar is the more stable
layer, and future operators will need the finer forms. LOC-1
deliberately did not shrink the grammar to match today's substrate;
that would be reverting a decision, not fixing a wart.

**No ADR.** A bug-class fix, recorded here, in `docs/locators.md`, and
in the commit messages. Offered to the author to strike if they think
it rises to ADR grade.

## DOC-4 — Runnable strips for use cases

Each internally-drivable use case in `docs/use-cases.edn` gains its
command sequence; external-stage cases get explicit "you bring"
stubs. Decision (author): a `:commands` field in `use-cases.edn` +
renderer extension (single source of truth, freshness-gated —
recommended) vs. a separate `docs/cookbook.md` cross-linked per case
(cheaper, second place to rot). Tripwire either way: commands are
verified by actually running the internally-drivable ones once,
locally, before they're committed as documentation.

**Done (2026-07-26).** Landed as seven commits, route ratified as the
`:commands` field (prompt archived at
`.agents/prompts/archive/2026-07-26-doc4-runnable-strips.md`).
Itemized:

- **Step 0, evidence.** All 14 cases classified before any data was
  written — expected 10 strips, 4 stubs. Every expectation held on
  evidence, including the two flagged as genuinely uncertain:
  `training-material` (`:illustrative`, expected runnable — it runs)
  and `audit-regulatory-evidence-trail` (whose own `:get` names a gap
  — the three evidence artifacts are produced by real commands, and
  the strip says plainly that assembling them into one package is not
  a command). Step 0 also enumerated the operators-verb tests as
  Step 5's blast radius, and mapped README's quickstart lines to the
  case ids they already covered.
- **Step 1, schema + renderer.** `UseCase` gains two optional,
  mutually exclusive keys: `:commands` (a map of `:lines` — the
  literal lines of one fenced block — plus an optional `:note`) and
  `:no-commands` (the honest reason there is no strip). A `[:fn]`
  guard rejects a case carrying both. The `:lines`/`:note` split makes
  the paste-safety rule structural: markdown prose and cross-links
  have nowhere to go but `:note`, which renders below the fence.
  `case->commands-block` renders both arms from case data alone —
  no invocation is synthesized, and a case without either key still
  renders a stub derived from `:bring`. The strip sits after
  **Maturity:** and before the equations, above the formal grounding
  rather than below the diagram (AUTHORS-GUIDE.md §6). 10 additive
  tests.
- **Steps 2–4, the data in three run-verified batches.** Corpus-side
  (4 cases), gate/judge-side (5 cases), composition + stubs (1 strip,
  4 stubs). Every command was run as committed, exit codes and
  outcomes captured in each batch's commit body. The expensive runs
  were real: a 99-second `gate fhir` producing a 2.9 MB report, and
  `make integration` at 19m11s, 8 tests / 24 assertions green against
  the real validator.
- **Two strips changed because running them said so.**
  `reproduction-packages` first ended in `diff -r`, which exits 1:
  Synthea's `hospitalInformation<timestamp>.json` /
  `practitionerInformation<timestamp>.json` names embed a wall-clock
  timestamp no seed pins — exactly what EXP-A4 found and registered
  `:strip-run-timestamp-suffix` for. The strip now ends in
  `check ... --pair-by hash`, this repo's own equivalence judge, and
  says why. `training-material` first ended in a `diff` of original
  against mutant: 59,561 lines, because the mutant is canonical JSON
  and Synthea's output is pretty-printed. It now ends with the lineage
  record plus a two-line `grep` of the changed value.
- **Step 5, `:doc` at the shell (optional, taken).**
  `ehr corpus operators` rows carry the registry's one-line
  description. Additive: none of the five enumerated tests needed
  editing, one was added, and `cli.md` did not regenerate (confirmed
  by running the target, not by assuming).
- **README, one edit.** `README.md:154-158` spelled the integration
  suite `clojure -X:test :excludes '[]'` — stale since the suite moved
  to the `test-integration/` path, which AGENTS.md documents as a path
  split precisely because a tag filter does not select it. It now says
  `make integration`, matching the contract-pairing strip, in the same
  batch commit.
- Extended golden check clean, full suite (486 tests / 1487
  assertions, from 475/1467 at LOC-1's close) and both lints green at
  close-out. Link check across `use-cases.md` (41 links), `README.md`
  (25), `cli.md` (9), `operators.md` (24): every relative target
  exists and every `#anchor` matches a real heading slug.

**Final per-case classification** — strip (10): generate-conforming-
data, generate-controlled-fault-data, test-a-validator-with-contract-
pairing, judge-user-supplied-data, regression-baselining,
acceptance-qa-of-vendor-corpora, reproduction-packages,
audit-regulatory-evidence-trail, judge-tier-calibration-studies,
training-material. Stub (4): black-box-transform-surround,
mutation-adequacy-of-your-own-checks,
differential-ab-of-two-transform-versions,
bring-your-own-generator-augmentation. No Step-0 expectation flipped.

**Report to the author — two findings, deliberately not acted on:**

1. **`--report <path>` does not create the path's parent directory,
   and fails loudly in the wrong register.** `ehr gate v2 ... --report
   out/calibration/before.edn` with no `out/calibration` throws an
   uncaught `FileNotFoundException` — a raw stack trace plus "Full
   report at: /tmp/clojure-*.edn" — rather than a `result/error` with
   a category. ADR-0004 reserves exceptions for programmer error; an
   unwritable report path is an operational failure, and DOC-1's
   enumerable-options error family is the house pattern it should join
   (`:report-path-unwritable`, naming the directory). Every strip that
   writes a report now creates its directory first, with a comment
   saying why, which is the honest thing a docs session can do; making
   the CLI say it is a code change and a separate one.
2. **`make ehr ARGS="..."` cannot carry the CLI's exit code.** `make`
   exits 2 for any failed recipe, so ADR-0004's and ADR-0010's 0/1/2/3
   contract collapses to "2" at the wrapper. Measured both ways on the
   same invocations: a rejecting `gate v2` and a rejecting `gate fhir`
   each gave make 2, direct CLI 1. The invocation convention stays the
   README's per the ruling, and the three cases whose point is
   branching on a verdict now name the direct invocation in their
   notes — but if `make ehr` is meant to be the taught entry point, the
   recipe could propagate the child's status instead (`exit $?`-shaped
   change in the Makefile), and then the notes could be deleted.

Also found, not changed (this session's data work may not touch
tests): `test-integration/ehr_testing_tools/contract_pairing_test.clj`'s
ns docstring still says "Run explicitly with `clojure -X:test :excludes
'[]'`" — the same stale incantation the README edit above fixed.

*[All three answered by the author 2026-07-26 and executed in CLI-2
below: finding 1 fixed in code (`--report` creates its parent, residual
IO failures categorized); finding 2 answered as **option (b)**, a
`bin/ehr` wrapper rather than a Makefile change, the infeasibility
premise measured before anything was built on it; the docstring rider
fixed. The report above stands as the record of what DOC-4 found and
deliberately left alone.]*

## CLI-2 — Report writes and the entry point (interlude)

Not a DOC wave: the code micro-wave answering DOC-4's two deliberately-
unacted findings, plus the docstring rider, plus one cross-repo ADR the
author added mid-planning. Slotted after DOC-4 because both findings are
things DOC-4 could only document around.

**Done (2026-07-26).** Seven commits, prompt archived at
`.agents/prompts/archive/2026-07-26-cli2-report-and-entrypoint.md`.
Two phases, both ratified. Itemized:

- **Phase A, step 1 — `--report` conforms to ADR-0004.** A new private
  `cli/write-report!` does `io/make-parents` then `spit`: the user named
  where they want the file, so a missing intermediate directory is
  created rather than surfaced as the uncaught `FileNotFoundException`
  DOC-4 measured. Any residual `java.io.IOException` becomes
  `result/error :report-write-failed` (payload: `:path`, the cause's
  `:message`, a `:hint` — DOC-1's enumerable-options register) at exit 2
  through ADR-0004's generic mapping, no special case added to
  `result->exit-code`. Applied at all three write sites (gate's plain
  and `--baseline` branches, check's) and nowhere else; the write error
  outranks the verdict it would have accompanied, since a run whose
  recorded output didn't land is an operational failure rather than a
  judgment. Six additive tests, no existing test edited; red→green was
  5 errors → 0.
- **Phase A, step 2 — the strips shed the workaround.** All five
  `mkdir -p` lines and the note clause that explained them leave
  `use-cases.edn`. Verified first that nothing else in those strips
  depended on the directory (`corpus intake` and `corpus generate` each
  mkdir their own output). Four cheap strips re-run end to end; the
  Synthea/`gate fhir` strip deliberately not re-run.
- **Phase A, step 3 — the docstring rider.** `contract_pairing_test`'s
  ns docstring now says `make integration`. Rider in the same sentence:
  it also claimed the `^:integration` tag and the `:test` alias were
  what excluded the suite; AGENTS.md's rule is a path split, so that
  was restated too. Doc-only.
- **Phase A, step 4 — ADR-0012.** The `ehr-testing-sim` maintainer's
  mounting note is vendored verbatim
  (`notes/ehr-testing-sim-mounting-note.md`) and turned into an
  interface commitment: five load-bearing CLI properties (dispatch's
  parsed-in/Result-out shape; one babashka.cli parse with one spec;
  structural Result typing; the help-spec data shape; the `-fn`
  injection point), each with its safe/breaking line, plus the two
  manifest commitments (version-don't-mutate; the binding contract test
  belongs in this repo's `test-integration/`). A one-line comment at
  `cli.clj`'s dispatch site points at it. Every claim was re-verified
  against source, and **three of the note's claims did not match the
  code** and are recorded as corrections: the shell already interprets
  two `:category` values globally (`:gate-no-verdict`, `:cli-help`); the
  help-vs-dispatch coverage test is hand-mirrored, so a mounted group
  fails loudly rather than being covered for free; and `corpus intake`
  never reads a manifest at all. Mount-time design is explicitly out of
  scope.
- **Phase B, step 0 — the premise, measured.** The prompt's claim that
  `make ehr` structurally cannot carry the exit contract was probed
  before anything was built on it (a three-line makefile, GNU Make
  4.2.1): a recipe exiting 3 leaves make exiting 2, `.SHELLSTATUS` lets
  the makefile *read* the code but not *be* it, `-k` changes nothing,
  and the only recipe-level lever (`-`) makes the failure invisible
  instead. Premise holds. Also located the exit-3 case the fidelity
  evidence needed (no committed fixture produces one: judge.v2 never
  emits `:no-verdict`, and Synthea-derived FHIR carries genuine errors
  that outrank it), and enumerated every live `make ehr` site — which
  found README carrying 10, not the 1 the prompt assumed.
- **Phase B, step 1 — `bin/ehr`.** Twelve lines of bash: resolve the
  repo root from the script's own location, `exec` the same
  `clojure -M -m` the make recipe runs, so equivalence is by
  construction. Mode 100755 in the index (needed
  `git update-index --chmod=+x`: the tree is NTFS with
  `core.filemode=false`). All four exit codes proven through the
  wrapper — 0 `help`, 2 an unknown verb, 1 a rejecting `gate v2`, 3 a
  real `gate fhir` no-verdict aggregate — plus a check that a relative
  path argument still resolves against the repo root when invoked from
  a subdirectory. One CI smoke step in the fast tier (`bin/ehr help`
  exits 0 and prints usage). Recorded in that commit: the wrapper sits
  entirely outside the parse-and-dispatch boundary, so ADR-0012's
  properties are untouched by it.
- **Phase B, step 2 — the sweep, complete in one commit.** 54 live
  teaching sites flipped (README 10, SETUP 5, `use-cases.edn` 30,
  AGENTS 1, the renderer's two literals, four test expectations) and
  `use-cases.md` regenerated. The three verdict-branching strips' notes
  lose the "call `clojure -M -m ...` directly" escape hatch that
  existed only because make swallowed the code. `make ehr` survives
  unchanged as a compatibility spelling, with `make help` and a
  Makefile comment naming `bin/ehr` as primary and stating the
  collapse. CI's own three `make ehr` calls are deliberately left as-is
  (a machine invocation, not instruction — and it keeps the compat
  spelling exercised nightly).
- Extended golden check clean (only `use-cases.md` regenerated;
  `cli.md` carries no invocation spelling, confirmed by running the
  target). Full suite 492 tests / 1507 assertions (486/1487 at DOC-4's
  close), both lints green, coverage 90.93% forms / 94.08% lines. Link
  check across README, SETUP, AGENTS, `use-cases.md`, `cli.md`,
  `ADRs.md`, and the vendored note: 84 relative links, every file and
  `#anchor` resolves.

**Report to the author — SETUP.md has drifted from its cohort-validated
text.** The narrow fence lift was used exactly as granted: five
mechanical `make ehr ARGS="X"` → `bin/ehr X` substitutions in SETUP.md,
nothing else touched. But SETUP is the one document validated
externally (the trial cohort's 15-minute result), and the commands a
walkthrough tells you to type are the substance of that validation, not
packaging. The substitution is behavior-preserving and the wrapper is
proven, so this is not a suspected break — it is a note that the
validated artifact and the shipped artifact are no longer the same
bytes, and a cohort re-check (or one fresh run of SETUP end to end by
someone who hasn't seen it) would restore the claim. One related detail
worth a glance if that re-check happens: SETUP's WSL2 framing now
matters more than it did, since `bin/ehr` is a bash script rather than
a make target.

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
| DOC-3 | `docs/cli.md` + `docs/operators.md` generated (new `docsgen` renderers, two make targets, freshness-gated); `docs/locators.md` + `docs/formats.md` hand-written, examples/shapes machine-verified; registry gains `:doc`; golden check extended | **Done** (2026-07-25) | `.agents/prompts/archive/2026-07-25-doc3-reference-docs.md` |
| LOC-1 | Locator grammar micro-wave (interlude, not a DOC wave): FHIR paths reject a trailing separator (split limit `-1`, dead guard revived); `MSH-1` refused at parse with a teaching `:hint`; both rejection categories preserved; `docs/locators.md`'s two stale sharp edges rewritten as dated grammar facts, didactic MSH account preserved; component-granularity edge out of scope by ruling | **Done** (2026-07-25) | `.agents/prompts/archive/2026-07-25-loc1-locator-grammar.md` |
| DOC-4 | Per-use-case runnable command strips, route ratified as `:commands` in `use-cases.edn`: schema gains `:commands`/`:no-commands` (mutually exclusive, `[:fn]`-guarded), renderer emits a **You type:** fenced strip or a data-derived honest stub; 10 strips run-verified end to end (incl. a 99s `gate fhir` and `make integration` at 19m11s), 4 stubs naming their blocking external/planned stage; `ehr corpus operators` surfaces `:doc`; README's stale integration-suite incantation fixed to `make integration` | **Done** (2026-07-26) | `.agents/prompts/archive/2026-07-26-doc4-runnable-strips.md` |
| CLI-2 | Code micro-wave (interlude, not a DOC wave): `--report` creates its parent and residual IO failures become `:report-write-failed` (exit 2, ADR-0004), strips drop their `mkdir -p` workarounds, contract-pairing's ns docstring drops a stale incantation; **ADR-0012** records the five CLI properties (plus two manifest commitments) `ehr-testing-sim` mounts against, with the note vendored and three of its claims corrected against source; `bin/ehr` becomes the taught entry point — entry-point decision **option (b)**, decided 2026-07-26, the make-can't-propagate premise measured at Step B0 first — with all four exit codes proven through it and 54 teaching sites flipped in one commit, `make ehr` kept as compat | **Done** (2026-07-26) | `.agents/prompts/archive/2026-07-26-cli2-report-and-entrypoint.md` |
| DOC-5 | Quickstart-as-script, nightly-wired; README freshness link | Not started | — |

## Open decisions

- **Operator-listing verb name** (author; blocks DOC-1's Step 3;
  recommendation: `ehr corpus operators`).
- **DOC-3 generated vs. hand-written**, per document — **decided
  2026-07-25 (author), as recommended**: `operators.md` and `cli.md`
  are generated (from the registry and from DOC-1's `cli-spec`
  respectively, on the pipeline.md/use-cases.md
  renderer-plus-freshness-gate pattern); `locators.md` and
  `formats.md` are hand-written with source citations. The `cli.md`
  branch resolved in favor of generation: Step 0 found `cli-spec` rich
  enough, needing one added flag.
- **DOC-4 route**: `:commands` field vs. cookbook — **decided
  2026-07-26 (author), as recommended**: the `:commands` field in
  `docs/use-cases.edn` plus a renderer extension, so the strips are a
  single source of truth living in the same freshness-gated document
  as the equations they ground, and the golden check inherits them. A
  separate `docs/cookbook.md` would have been a second place to rot.
- **CLI entry point** (raised by DOC-4's second finding) — **decided
  2026-07-26 (author): option (b)**, a `bin/ehr` wrapper as the taught
  entry point, rather than trying to make the Makefile propagate the
  child's status. The premise was measured before the wrapper was
  built (CLI-2 Step B0): GNU make's own exit status is 0/1/2 by
  definition, so a recipe cannot carry ADR-0004's 1 or ADR-0010's 3 no
  matter how it is written. `make ehr` stays as a compatibility
  spelling.
- **Sequencing against first release** — **decided 2026-07-25
  (author): now, pre-release**, accepting the soft interface-hardening
  pressure a full CLI reference creates. Mitigation shipped with it:
  both generated docs carry a one-line pre-release notice pointing at
  README.md's maturity table.
