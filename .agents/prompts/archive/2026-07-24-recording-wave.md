# Recording wave — MIT relicense, NIST findings, pack transport v2

You are working in `ehr-testing-tools`. Small consolidation session: no
capability code. It records decisions and findings from the design
channel (MIT relicense; NIST licensing research; P4/EXP-B2
interpretation), fixes the pack-transport reliability gap, and extends
the permissions allowlist empirically.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md`,
`notes/facts-register.md`, `.agents/memory/patterns.md`,
`docs/components.md`, `docs/experiments/EXP-B2-results.md` (or the
results file P4 produced), `docs/research/` (the NIST licensing report
the author committed — find its exact filename), `docs/notation.md`,
`.agents/plans/corpus-foundations.md`. Commits from WSL. Save this
prompt to `.agents/prompts/2026-07-24-recording-wave.md`; final commit
archives it.

## Step 0 — Pack transport v2 and allowlist extension

1. Packs repo. Create a public repo `pragsmike/packs` (`gh repo create
   pragsmike/packs --public --description "generated pack snapshots"` —
   the gh token has repo scope; if creation fails, stop this sub-step and
   flag). Maintain a local clone at `~/.packs` (clone if absent). Rework
   `pack-push` to: build both packs (`pack` and `pack-skills`), copy both
   into `~/.packs/`, commit with a message containing repo name + HEAD,
   and push. Remove the gist PATCH logic (note in the Makefile comment
   that the gist at 4fcd1abb... is retired as transport; leave the gist
   itself alone). Rationale (record in AUTHORS-GUIDE's pack ritual):
   raw.github content is CDN-served and reliably fetchable by the design
   channel, unlike the gist API's rate-limited shared pool; and with the
   skills pack published the same way, no manual uploads remain anywhere
   in the workflow.
2. Allowlist extension (`.claude/settings.json`): add the command
   families P4 empirically prompted for — `sha256sum`, `diff`, `tar`,
   `unzip`, `mkdir`, `cp`, `mv`, `find`, plus script execution of
   repo-local `.clj`/`.sh` via `clojure`/`bash` where the current rules
   don't already cover them. Do NOT broaden `Write`/`Edit` beyond the
   repo (a mis-pathed write outside the repo was caught by prompt this
   week — that gate stays). Note the two-strikes rule in the AGENTS.md
   paragraph.

Commit: `Pack transport v2 (packs repo, gist retired); allowlist
extension`. Then run the new `pack-push` once now so the transport is
proven mid-session (it will be run again at finalize).

## Step 1 — ADR-0007: MIT relicense (supersedes ADR-0001)

House format, author-directed, superseding not editing ADR-0001:

* Context. ADR-0001 chose Apache 2.0 for the patent grant and ecosystem
  fit. The author's other public projects are MIT; a single license
  posture across projects reduces per-repo reasoning for humans and
  agents alike; the repo is pre-release with no external contributors or
  downstream users, making this the cheapest moment a relicense will ever
  be. All EXP-SBOM compatibility findings were evaluated against
  "permissive open-source distribution" and transfer unchanged — MIT and
  Apache 2.0 are in the same compatibility class for every dependency
  examined (EPL entries, the MPL election, the LGPL flag included).
* Decision. MIT for all original code and documentation. Attribution
  semantics are equivalent for the project's purpose (retain the notice
  in copies).
* Alternatives rejected. Staying on Apache 2.0 — its explicit patent
  grant and §5 contribution term are real but modest for testing tooling;
  the trade is accepted knowingly. Dual licensing — complexity without a
  constituency.
* Consequence. LICENSE replaced with MIT (standard text, author's
  copyright line, current year). Forward-looking docs that name the
  Apache target are updated: components.md framing where it says "against
  the Apache-2.0 target", positioning.md's go-public gate, the
  dependency-review sentence in ADR-0001's consequence is superseded by
  "compatible with MIT distribution?" (same class, same answers).
  Historical documents — the EXP-SBOM protocol and results, archived
  prompts — are dated records and are NOT edited. Status: Accepted
  (author-directed).

Swap the LICENSE file accordingly. Commit: `ADR-0007: relicense MIT
(supersedes ADR-0001)`.

## Step 2 — Record the NIST licensing research

The author has committed the deep-research report under `docs/research/`
(find it; if somehow absent, stop and flag). Record its findings:

1. Facts register. Update F1: inventory + deep research executed;
   official public distribution channel identified (hit-nexus.nist.gov
   serving all six coordinates); use-rights basis = NIST's general
   software statement; residual = foreign-copyright redistribution
   confirmation + hl7-tools provenance; inquiry narrowed, recipient
   identified. Add new rows (next numbers, house style, evidence links,
   dates from the report):
   * hit-nexus.nist.gov is live, publicly reachable without
     authentication, and serves all six vendored coordinates (.pom +
     .jar); byte-identical to the CDC-vendored copies. Mark this row's
     nature as volatile (hosted service; the IGAMT/GVT
     Prometheus-transition notices make longevity uncertain) — its
     Last-verified date is load-bearing.
   * NIST's software licensing statement permits use/copy/
     modification/distribution with notice, notes possible foreign
     copyright per 17 U.S.C. §105, and — unlike NIST's parallel data
     statement — contains no affirmative worldwide grant. Evidence:
     nist.gov/open/license vs the data statement, per the report.
   * `com.github.hl7-tools:validation-report` has a live GitHub upstream
     (github.com/hl7-tools/validation-report; README retrieved HTTP 200
     on master) with no LICENSE file on master or main (both HTTP 404),
     probed 2026-07-24 from the design channel; org provenance (who
     publishes hl7-tools) unknown — distinct from the usnistgov §105
     argument.
   * HAPI FHIR Bundle round-trip: P4/EXP-B2 measured that parse→
     serialize drops `resource.id` from Bundle entries under default
     parser configuration. VERIFICATION DUTY: check HAPI FHIR's parser
     documentation/source for the Bundle-entry id-vs-fullUrl override
     behavior (there is believed to be a parser setting controlling
     whether entry resource ids are overridden/derived from `fullUrl` on
     parse). Record the row with whichever wording the evidence
     supports: configurable default (name the setting, cite it) or
     unconditional (cite the absence). Do not register the unqualified
     claim if the qualifier is real.
2. components.md. NIST v2-validation + CDC wrapper sections gain the
   hit-nexus/fetch-at-build sentences (official channel; Mode-2 adoption
   path; inquiry pending on the narrow residual). HAPI FHIR section gains
   one caveat sentence reflecting the F-row above (round-trip not
   identity-preserving under default config; used as parse-validation aid
   only, never in hash/diff paths — decided in P4).
3. ADR-0005 consequence amendment (append to its Consequence — additive
   clarification, not a reversal; note the date): artifacts divide into
   redistributed-by-this-repo (must be license-verified) and
   fetched-by-users from official sources (lockfile records provenance +
   hash; the user's fetch is their use; repo redistributes nothing). New
   license-status value admitted for the latter:
   `:use-permitted--unstated--confirmation-pending`. positioning.md's
   go-public gate gains the same distinction in one sentence.
4. Inquiry draft (`docs/experiments/EXP-SBOM-inquiry-draft.md`): revise
   per the research — addressed to the named tool-family contact (Robert
   Snelick, per the report); four narrow questions: (a) confirmation the
   two repos + six coordinates fall under the portfolio's "public domain
   resources" statement; (b) NIST's position on foreign-copyright
   redistribution for this software, given the software statement's
   silence where the data statement speaks; (c) provenance/terms of the
   hl7-tools GitHub org's validation-report; (d) continuity of
   hit-nexus.nist.gov through the Prometheus transition. Keep it short
   and answerable. Not sent — author's channel.
5. experiments.md: EXP-D3's row updated — the mirroring problem is
   dissolved (fetch six coordinates from hit-nexus into the standard
   artifact cache via the existing registry); remaining scope is
   lockfile entries + offline wrapper build. EXP-SBOM row notes the
   deep-research extension and links the docs/research report.

Commits: split sensibly (register+components; ADR-0005 amendment +
positioning; inquiry + experiments).

## Step 3 — Notation and nursery bookkeeping

1. notation.md + pattern #13: ratify the fourth catalytic resolution
   target — in-repo code registries, referenced by `{id, version}`
   (operator catalog, canonicalizer registry) — alongside lockfile /
   deps.edn / hashed repo-authored config. Update #13's nursery entry:
   status remains on trial through the Gate equations (P5); evidence
   recorded: generated diagram caught a real cross-stage wiring mismatch
   (Normalize output vs Mutate input); equation authoring anticipated but
   did not correct the Mutate design; rule gap (this fourth target) found
   and now ratified.
2. No other promotions this session.

Commit: `Notation: fourth catalytic target; #13 trial evidence`.

## Finalize

`make test` green (regression check — nothing here touches code paths);
archive this prompt; run the new `pack-push`; verify by fetching
`https://raw.githubusercontent.com/pragsmike/packs/main/ehr-testing-tools-pack.txt`
and confirming its header shows this session's final HEAD and a clean
tree. Report: packs-repo creation outcome and both packs' presence;
allowlist families added; ADR-0007 as landed; every F-row added/updated
(with the HAPI verification outcome and its citation); inquiry draft diff
summary; notation amendments; commits.

## Out of scope

No capability code, no gate work, no EXP-D3 execution, no sending the
inquiry, no guide-repo edits, no editing ADR-0001 or any historical/
archived document, no gist deletion.
