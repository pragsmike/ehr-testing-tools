# Publication wave — editorial coherence, minimal CI, go-public gate

You are working in `ehr-testing-tools`. The author intends to make this repository public today. This session prepares it: an editorial pass that turns the accumulated documents into one coherent presentation, minimal CI so the repo's own go-public gate is satisfied rather than waived, and ADR-0008 recording the publication decision. The visibility flip itself is the author's manual action afterward — not yours.

Read first (all of it — this session is mostly writing, and coherence requires holding the whole): `README.md`, everything in `docs/` (including `docs/research/` titles, not contents), `notes/ADRs.md`, `notes/facts-register.md`, `.agents/memory/patterns.md`, `docs/positioning.md`, `.agents/plans/corpus-foundations.md`, `AGENTS.md`. Commits from WSL. `make test` green before and after. Save this prompt to `.agents/prompts/2026-07-24-publication-wave.md`; final commit archives it.

## The editorial brief (Step 1 — the bulk of the session)

Problem: the docs were written by different sessions for different immediate purposes and read as disjoint agent artifacts — duplicated context-setting, session-relative phrasing ("this session", "per the author's ruling"), inconsistent voice, no reading order.

Organizing principle: the corpus pipeline, in the resource-equation view, is the spine. Every document gets a stated role relative to it: the pipeline diagram is the map; components are the catalytic resources; the notation doc is the language the map is written in; the experiments are how the stages' laws were verified; the capabilities are the stages made runnable. Rewrite so a newcomer can walk README → pipeline → any depth they choose, with each document opening by locating itself in that walk.

Voice and honesty constraints:

* Engineer-to-engineer, plain, confident where evidence exists, explicit where it doesn't. No marketing register.
* Every factual claim keeps its F-row backing or verification date; the rewrite must not mint new claims or strip qualifiers (the F17 three-layer HAPI finding is the model — precision survives editing).
* Pre-release honesty is a feature: maturity labels appear wherever a capability is described.
* Remove session-relative phrasing; convert "decided in P4" style references to ADR/experiment citations, which are stable.

Per-document directives:

1. README.md (rewrite; the front door, one screen-ish):
   * One-paragraph what-this-is: operational tooling for testing EHR integrations — reproducible synthetic-corpus construction and conformance gating — Clojure/JVM, offline-first.
   * The pipeline diagram (or a compact inline mermaid excerpt of it) appears early: generate → normalize → mutate → gate, with gate marked planned. A picture of the whole before any prose detail.
   * Maturity table: generate — usable (byte-reproducibility proven; cite EXP-A4); mutate — experimental (works, days old, interfaces may move; cite EXP-B2); gates — planned (link the plan). These exact levels; the author has set them.
   * Quickstart: the real commands (`make test`, `ehr corpus generate ...` with a working example invocation, `ehr corpus mutate ...`), honest about the JDK/artifact fetch prerequisite.
   * The guide relationship paragraph (read-vs-run criterion, link), scope fence (the four non-goals), license line (MIT), and a "how this repo works" pointer to docs/ and the conventions (ADRs, facts register, experiments) — the discipline is part of the value proposition; say so in two sentences, not a sermon.
2. `docs/README.md` (new; the reading order): a short index — pipeline → notation → components → experiments (with per- experiment one-liners and status) → positioning → engine- onboarding → research/. One line each on what question the document answers.
3. `docs/pipeline.md` / `pipeline.edn`: ensure the rendered doc opens with two sentences of orientation (what a reader is looking at, what catalytic means, link to notation.md) — the diagram must not open cold. Gate/Report stages present as `:status :planned` and visibly marked so in the rendering. Regenerate via `make pipeline`; hand-edit nothing generated.
4. `docs/notation.md`: light edit — open with why the notation exists here (checking + alignment of isolated sessions), then the semantics; move any trial-status commentary out (that lives in patterns.md); one closing line noting the formalization's exploratory status and its cyberneutics origin (attribution + epistemic honesty).
5. `docs/components.md`: keep the fact tables intact (they're good); unify section shape (every component: what/who/role-in- pipeline/deliberately-not-used-for); add one orientation sentence at top tying components to the catalytic-resource idea.
6. `docs/experiments.md` + protocol/results files: experiments.md gets a two-sentence preamble (experiments verify stage laws and pin design decisions; protocols precede execution; results are classification-only, interpretation lives in the design record). Do not rewrite executed protocols/results beyond fixing session- relative phrasing in their headers — they are dated records; their bodies stand.
7. `docs/positioning.md`: update to publication reality — repo is public as of ADR-0008; the go-public gate section becomes a record of the gate walk (past tense) rather than a future condition; the "guide references tools only after first release" contract stays (publication ≠ release — state it).
8. `docs/engine-onboarding.md`: light pass for voice; add a line linking it from the components preamble.
9. AGENTS.md / AUTHORS-GUIDE.md: not part of the public-facing editorial arc but scan for anything that would confuse an outside reader (they will be read); fix only clarity, not substance.

What NOT to touch: archived prompts, ADR bodies 0001–0007, executed experiment results' findings, the facts register's rows (wording included), `docs/research/` contents.

Commits: split by document group, messages naming the editorial intent.

## Step 2 — Minimal CI (gate condition 3)

`.github/workflows/ci.yml`: on push and PR — checkout, JDK (Temurin 17), Clojure CLI setup, dependency cache, `make test`, `make coverage` (upload the coverage summary as a build artifact or print the headline; no threshold gating yet — that's the enforcement wave).

First: verify the suite is hermetic. Inspect tests for anything touching the network or the real artifact cache; if any exist, tag them (e.g. `^:integration` metadata) and exclude that tag from the default `make test` runner, documenting the tag in AGENTS.md. The CI must be green from a cold clone with no network beyond dependency download. Run the equivalent locally to prove it (fresh temp clone, `make test`) before committing the workflow. Add a CI badge to the README once the workflow file exists (it will go green after the author's first public push).

Commit: `Minimal CI: hermetic test+coverage workflow; integration tag`.

## Step 3 — ADR-0008: publication

House format, author-directed:

* Context: the positioning doc's go-public gate set four conditions; the author has decided to publish today. Walk each condition with evidence: (1) licensing — MIT (ADR-0007); all distributed dependencies verified permissive-compatible (EXP-SBOM); NIST-origin artifacts are not redistributed — fetched by users from the official channel per the ADR-0005 amendment, recorded `:use-permitted--unstated--confirmation-pending`, inquiry pending as a narrowing not a blocker; (2) at least one capability usable — generation, with EXP-A4's proven clean-environment byte-reproducibility as the evidence; mutation ships as experimental, honestly labeled; (3) CI green — added this session (hermetic suite); (4) referral README — rewritten this session.
* Decision: publish the repository publicly. Publication is not a release: no version tag, no Clojars artifacts, and the guide's register entry waits for first release per the standing contract.
* Alternatives rejected: waiting for the gates capability (publishes later but no condition requires it; pre-release labeling covers it); waiving the CI condition (a verification-tools repo waiving its own verification gate on publication day is self-refuting).
* Consequence: pre-release expectations govern (interfaces may move; maturity table is the contract with readers); the NIST inquiry's eventual answer updates F1/F14 and may upgrade the full-gate plan; first release becomes the next gate-shaped milestone.

Update the plan file: publication recorded; first-release milestone sketched (one line: gates landed + notation trial concluded + coverage gate).

Commit: `ADR-0008: publish (gate walk recorded)`.

## Finalize

`make test` green; archive this prompt; `make pack-push`; verify the raw fetch shows this session's final HEAD, clean tree. Report: the document map as rewritten (old → new role, one line each); claims whose wording changed in ways worth author review (list them — the author reads these docs before flipping visibility); hermeticity findings (any tests tagged); CI workflow summary; ADR-0008 as landed; commits.

Then stop. The author reviews the rendered docs and flips visibility manually.

## Out of scope

No capability code, no gate work, no visibility change, no release tagging, no Clojars, no guide-repo edits, no rewriting of dated records (results, register rows, archived prompts, ADR bodies), no new factual claims anywhere.
