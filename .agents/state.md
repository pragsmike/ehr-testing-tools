# State of the project — continuity register

**REGENERATED 2026-08-20, at the review-4 arc's own close
(`notes/adr/0159-review-4-arc-close.md`, ADR-0159), against tip
`e967fd7`; halved at the compression arc's close, ADR-0147.** This file
is the HAND-OWNED half of the register: judgement, watch items,
environment, and the design-channel contract. Every countable claim it
used to carry — component graph, vendored inventory, gate roster,
register sizes, reading-set actuals — is re-derived from the live tree
on every `make docsgen` into [`state-derived.md`](state-derived.md) and
diffed by CI, so it can no longer be stale. The 724-line prior text is
verbatim in [`plans/state-history-2026-08.md`](plans/state-history-2026-08.md).

Capped at 120 lines and linted (`ehrt.docs-tooling.state-residue-test`);
currency gated by `ehrt.docs-tooling.state-staleness-tripwire-test`; the
regeneration contract itself is `rulings.md#R-state-regeneration`.
Provenance tags: `[A]` author-ruled, `[C]` channel-inferred. There is no
`[V]` tag any more — a claim that needed one is derived now.

## Registers, and what gates each

| register | what it is | gate |
|---|---|---|
| `.agents/state.md` | this file: hand-owned continuity | `ehrt.docs-tooling.state-residue-test` |
| `.agents/state-derived.md` | every countable fact, generated | `ehrt.docs-tooling.state-derived-test` |
| `.agents/rulings.md` | standing rules only, one row each | `ehrt.docs-tooling.rulings-lint-test` |
| `.agents/reading-sets.edn` | per-task-class reading sets | `ehrt.docs-tooling.reading-set-budget-test` |
| `.agents/reading-sets-baseline.edn` | the budget ratchet | `ehrt.docs-tooling.reading-set-budget-test` |
| `.agents/plans/roadmap.md` | live intent, one gated row each | `ehrt.docs-tooling.roadmap-lint-test` |
| `.agents/session-records/README.md` | the record convention | `ehrt.docs-tooling.index-completeness-test` |
| `.agents/prompts/README.md` | the prompt-archive convention | `ehrt.docs-tooling.index-completeness-test` |
| `notes/ADRs.md` | the ADR index, generated | `ehrt.docs-tooling.adr-index-test` |

Attic'd history carries a dated pointer, never a summary:
`plans/state-history-2026-08.md`, `plans/roadmap-done-2026-08.md`,
`plans/reading-sets-history.md`, each ADR's own `### Index summary`.

## What this repo is `[A]`

Clojure/Polylith workspace generating deterministic synthetic hospital
traffic (Synthea GMF at pinned commit
`7e08387c68a7f0e21d13076609a159fd473fc902`), injecting defects, judging
conformance (HL7v2/FHIR). **Formats are emitters of the patient state
machine** (ADR-0043's "sibling emitters over one state machine"), and
the event log is the ground truth they render (ADR-0141). Book repo
`ehr-testing-guide` is separate, permanently out of scope (ADR-0001 R2).

A stranger can watch that traffic breathe as a bed board (`ehrt play
PATH --board N`, ADR-0066/0067), or generate and replay a demo from a
top-level operator front door (`demos/`, ADR-0073), without reading a
line of source. An emitter author has a named path in (ADR-0146).

## Live work carried here and nowhere else

Everything with a home elsewhere lives there: open work is
`plans/roadmap.md` (cite `roadmap.md#<slug>`), standing rules are
`rulings.md#R-<slug>`, and the numbers are `state-derived.md`. What
follows has no other anchor.

* **The channel writes with more specificity than its evidence
  supports** `[C]`. Live probes have overturned survey rows, channel
  claims and the author's own summaries in every arc — six in the review-3
  arc (ADR-0139), six more disclosed across review 4's and four found by
  its own close (ADR-0159). **Probe before acting** has
  caught every instance so far; it is the mitigation, and it is why the
  regeneration contract says nothing is carried forward on the prior
  version's authority.
* **A population narrower than the class it is read as covering** `[C]` —
  review 3 found it as *registry rather than tree* (ten instances, one in
  the gate on this file); review 4 found the successor, *a gate narrower
  than the law it enforces* (five, plus two in `roadmap.md`/`rulings.md`
  found by ADR-0159's own audit). Every gate owes the question: *how do I
  know this population is all of them?* `rulings.md#R-population-closure`.
* **A generated chain whose SOURCE hop is unregistered** `[C]` -- a
  freshness gate excluding its own source proves only that the middle
  agrees with itself. `sim-theory.edn` had already drifted and did not
  validate against its declared Malli; ADR-0152 closed it, class first.
* **Oracle roots and vendored round-trip tests are different
  populations**, compared as sets, never by cardinality — review 2 read a
  correspondence off two 34s that have since diverged (ADR-0139 D1-4).
* **The founding incident: still six guarded failures** `[A]`. Held, not re-walked since 2026-08-15.
* Externals are author-only rows on the roadmap, not tracked here.

## Environment `[A]`

Edit root = the ext4 clone at `/home/mg/src/ehr-testing-tools`, never a
`/mnt/*` checkout (`rulings.md#R-mnt-c-retired`, permanent).
`bin/preflight` checks it mechanically -- plus `core.fileMode` and
`core.ignorecase` residue (ADR-0157), the last five CI runs, tree
cleanliness, HEAD-vs-remote, tag state -- and is fail-closed on any
FINDING/UNKNOWN (`rulings.md#R-preflight-fail-closed`).

**Ceremony is scripts, not prose** (ADR-0127, R13): `bin/preflight`,
`bin/tag-ceremony`, `bin/post-push-verify`, `bin/close-scaffold`,
`bin/ascii-scan` — one definition each. `post-push-verify` derives its
base from `origin/<branch>@{1}` and fails loud (ADR-0138); the ASCII law
is gated before the commit exists by `.githooks/commit-msg`, which
shares `bin/ascii-scan` with check 2 (ADR-0157).

**A gate run captures its exit code explicitly** — `make test > <log>
2>&1; MAKE_EXIT=$?`, never through a pipe or `tail`, which return their
own status and truncate the counts a session reconciles against
(ADR-0138); **and a wrapper that captures `MAKE_EXIT` ENDS with `exit
"$MAKE_EXIT"`** -- what the harness reports is the wrapper's LAST
command, not the gate's (ADR-0152's own mask; ADR-0155).

**The `stable-*` tag census is deliberately not recorded here.** A tag is
pushed after the commit it points at, so any committed count is wrong on
arrival — `git tag -l 'stable-*' | wc -l` is current by construction.
Tag law: `rulings.md#R-tag-law`.

## Design-channel contract (how sessions run)

Two channels: design (chat) plans and verifies against a fresh public
clone; Code executes pasted prompts, self-archives, R30 ceremony.
**Evidence over ruling.** New design sessions read THIS file, then
`state-derived.md` for any number, then `plans/roadmap.md` and the
latest ADR execution record. Structural claims in a continuity prompt
carry citations or an explicit `[unverified]` tag `[A, AR-B-5]`.
