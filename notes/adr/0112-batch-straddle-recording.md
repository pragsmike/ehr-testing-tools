## ADR-0112 — Batch-straddle recording: use case, rulings, and the user-guide opening

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Two author quotes, 2026-08-11, in the ADR-0111 window's own batching-
documentation exchange, verbatim (`.agents/rulings.md`, "From
ADR-0112"): *"We need to add this batch-boundary-straddling encounter
message scenario to the documentation. Should it be a use case? It
should be a demo, and featured prominently in the tool user guide, and
in the more general EHR testing guide as it's something that happens
in the real world and can trip up the unaware."* And, accepting the
channel's proposed recording sequence: *"ok, but this session is
getting old. Let's put that in the next session to record in the
repo, and in the continuity prompt."* This session executes that
deferred recording: a DOCS-AND-REGISTERS-ONLY session, zero `src`
change anywhere.

### Tag ceremony

Design channel verified the ADR-0111 landing at `ed5f51d` by fresh
public clone on 2026-08-11: lineage (`b5b9b9e` -> `1e0a1d6` ->
`d1f8fa1` -> `ed5f51d`), ASCII byte-check on all three commit messages
(clean), and the four deep re-derivations ADR-0111's own close left
INHERITED — all four completed and clean: (1) an independent
partition-semantics model (half-open epoch-aligned buckets confirmed
for ts >= 0; global cross-file MSH-7 sort with stable tie order; UTC
DTM interpretation matches `parse-dtm-lenient`'s own contract; one
benign channel note: Clojure `quot` truncates toward zero, so the
half-open law would invert for pre-1970 timestamps — domain-irrelevant
for MSH-7, recorded, not a defect); (2) the `:batch` codec against the
v2 batch-protocol segment definitions (minimal BHS-1/BHS-2 legal,
BTS-1 true-count verified on decode, byte-level throughout, round-trip
closes including the empty-items leg); (3) the straddle witness's
arithmetic against the demo's own witnessed epoch values
(`1786406400000` = `2026-08-11T00:00:00Z` exactly; A01 `00:30:26Z` ->
bucket `[00:00,01:00)`, A03 `01:34:19Z` -> `[01:00,02:00)`, adjacent;
the `08:00Z`-`10:00Z` gap real: batch-031 ends `1786521600000` =
`08:00Z`, batch-032 starts `1786528800000` = `10:00Z`); (4) the
two-file cross-file-ordering test's genuineness (synthetic `batch-msh`
messages, epoch-1970 MSH-7, nothing sim-shaped; file b's bucket-1
message chronologically precedes file a's and the test asserts byte
order inside the written file). `git fetch` confirmed `origin/main`
already at `ed5f51d` at session start; the last five CI runs on `main`
were all `completed`/`success`. `stable-20260811-corpus-batching`
tagged annotated at `ed5f51d`; pushed; peeled ref confirmed
`ed5f51dc2c81b05f978d54ca9181f4fa26e7db59` — exact match.

### Decision

**[A] The use case.** `components/corpus/docs/use-cases.edn` gains
`:supply-batch-straddling-traffic`, placed immediately after
`:play-a-generated-corpus-back-over-time` (its transport-realism
sibling): the generate-sim + `corpus batch` command pair reused
verbatim from ADR-0111's own witnessed demo run, an equation chain
ending in an `{external: true}` `YourReceiver` stage (the
`:black-box-transform-surround` shape), and a footnote-marker
cross-link into `demos/scenarios/ed-tuesday/README.md#batched-delivery`
(the `link-footnote-gate-test` licensed shape). `make docsgen`
regenerated `docs/use-cases.md` and the new per-case page
(`docs/use-cases/supply-batch-straddling-traffic.md`) as the only
delta; `docs/cli.md`, `docs/pipeline.md`, `docs/operators.md` came out
byte-unchanged, confirmed by `git status` after regen.

**[A] The rulings.** `.agents/rulings.md` gains "From ADR-0112," two
entries: "Batch-straddle documentation placements" ([A], carrying both
author quotes above in full, naming the three placements — demo
landed ADR-0111, tool-specific user guide opened this session, EHR
Testing Guide Ch 24 the author's own queue); "User-guide trigger read"
([C], the channel's own reading of "featured prominently" plus the
un-vetoed "ok" as ratifying the trigger and opening the user-guide
work, provenance disclosed as channel-read, not author-verbatim).

**[A] The roadmap.** `.agents/plans/roadmap.md`'s own user-guide
trigger paragraph (the one that carried "PENDING AUTHOR RATIFICATION"
since ADR-0110) is updated in place: RATIFIED 2026-08-11, citing the
rulings entry above, the channel-read provenance named in the note
itself. A new Next-section row lands: "Tool-specific user-guide design
pass," status awaiting-design-pass, not chartered to any executing
session yet — the design channel frames structure/audience
voice/gap-analysis before any writing session executes it; SETUP.md's
own unspoiled-human-reader rewalk (Externals) is that pass's smoke
test. A new Externals row lands: "EHR Testing Guide Ch 24
'completeness illusion' section notes," not a session charter — the
channel may draft notes on request, grounded in the ADR-0111 demo's
witnessed run; the guide itself lives outside this workspace,
`AGENTS.md`.

### Deviations, dated 2026-08-11

- **Fence amendment, author-licensed mid-session** — the driving
  prompt's own Step 1 gate list named `ehrt.docs-tooling.usecases-test`
  as a co-verifying gate this step "must pass by name," but that
  namespace's `committed-use-cases-edn-has-twenty-cases-test`
  (`components/docs-tooling/test/ehrt/docs_tooling/usecases_test.clj`)
  hardcodes the exact case count and carries a running comment-log of
  every prior bump — adding the 21st case broke it
  (`expected: (= 20 (count (:cases data))) actual: (not (= 20 21))`),
  and fixing it required a one-`deftest` edit under `test/`, which the
  same prompt's own fence forbade outright ("zero test-code change
  anywhere," "ZERO changes under any `src/` or `test/` directory"). The
  session STOPPED-AND-REPORTED rather than resolving silently. The
  author licensed the amendment, verbatim: *"a"*, accepting the
  channel's drafted amendment text — rename the deftest to
  `committed-use-cases-edn-has-twenty-one-cases-test`, bump `20` to
  `21`, append one comment-log line matching the six precedents already
  in that file, co-land it in commit 1. Applied exactly as licensed;
  `make test` went green afterward with no further gaps. **The channel
  error, named:** this session's own driving prompt's Read-first list
  omitted `usecases_test.clj` even though the same prompt's Step 3
  explicitly plans to bump `notes/adr/README.md`'s own count lock
  (109 -> 110) while fencing the sibling count lock in `test/` out
  entirely — an incomplete-probe error the channel that drafted the
  prompt owns, not a defect in the use case itself or in the fence
  discipline that caught it.
- **A property-test flake, unrelated to this session's fence.** The
  first post-amendment `make test` run failed
  `ehrt.sim-engine.engine-test`'s
  `mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`
  (a `clojure.test.check` generative test, random seed
  `7844068501`, `failing-size 110`) — a namespace this session never
  touched (`ehrt.sim-engine` is not on the fence's touch list at all).
  A second full `make test` run, same tree, no code change in between,
  passed the same test clean under a different seed
  (`1786504775396`). Recorded as a pre-existing, seed-dependent flake
  in `engine-test`'s own generative suite, not a regression this
  session introduced or is chartered to fix; not further investigated,
  per this session's own docs-only scope.

### Oracle bracket

**Pre-analysis:** pure identity on all 35 roots was the prediction —
this session's own footprint is `components/corpus/docs/use-cases.edn`,
generated `docs/use-cases*` surfaces, one `test/` deftest (the licensed
amendment), and `.agents/*`/`notes/*` registers only; no `src` change
anywhere, no vendored-root content touched.

**Bracket result.** `bin/regression-oracle ed5f51d 9bdc346` (`9bdc346`:
this session's own rulings+roadmap commit, run before the close-phase
commit, per the driving prompt's own step ordering): `IDENTICAL: every
root's digest matches between ed5f51d and 9bdc346` — all 35 roots,
matching the pre-analysis; no STOP-AND-REPORT needed.

### Full gate

`make docsgen`: `docs/use-cases.md` and the new per-case page the only
delta; `docs/cli.md`/`docs/pipeline.md`/`docs/operators.md`
byte-unchanged. `make test` (`clojure -M:poly check` + `clojure -M:poly
test :all skip:integration` + `bin/verify-nist-lock`): green, zero
`FAIL`/`ERROR` anywhere in the run that landed commit 1 and in the
independent re-run after the docs-only rulings/roadmap edits (the
`engine-test` flake disclosed above cleared on re-run, unrelated to
either edit). `ehrt.docs-tooling.usecases-test`: green post-amendment
(21 cases, unique ids, narrative-field conservation, Malli-valid).
`ehrt.docs-tooling.invocation-lint-test`: green, the new case's
commands and generated page resolve under the fence-path machinery.
`ehrt.docs-tooling.link-footnote-gate-test`: green, the footnote-marker
cross-link is the licensed shape. `bin/verify-nist-lock`: OK, 6
hit-nexus-sourced coordinates matched. `gitleaks git --staged -v`
(pre-commit, each checkpoint) and `gitleaks detect` (pre-push): no
leaks found.

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start): all `completed`/`success` —
`ed5f51d` (ADR-0111 session-record close, 4m30s), `d1f8fa1` (ADR-0111
demo commit, 4m31s), `1e0a1d6` (ADR-0111 batch feature commit, 4m39s),
`b5b9b9e` (ADR-0110 fix-forward session-record, 3m31s), `2faa5ba`
(ADR-0109 session-record close, 4m45s) — no red among the five.
Post-push, the same five re-checked green through commit 2
(`9bdc346`); commit 1's own run (`abed772`) shows GitHub's own
`displayTitle` as the literal workflow name `"test"` rather than the
commit subject — a cosmetic `gh run list` display quirk, not a
run-content anomaly (`headSha` matches `abed772` exactly, `conclusion`
`success`), disclosed rather than silently normalized away.

### Fences

Touched: `components/corpus/docs/use-cases.edn`; generated
`docs/use-cases.md` + `docs/use-cases/supply-batch-straddling-traffic.md`;
`components/docs-tooling/test/ehrt/docs_tooling/usecases_test.clj`
(the one licensed deftest, disclosed above as a deviation, not a
silent fence violation); `.agents/rulings.md`; `.agents/plans/roadmap.md`;
`.agents/prompts/2026-08-11-batch-straddle-recording.md` (self-archive)
plus its README index line; `.agents/session-records/2026-08-11-batch-
straddle-recording.md` plus its README index line; `notes/adr/0112-*.md`
(this file); `notes/ADRs.md`; `notes/adr/README.md`. `docs/cli.md`,
`docs/pipeline.md`, `docs/operators.md`: regenerated by `make docsgen`,
byte-unchanged, verified by `git status` after regen. ZERO changes
under any other `src/` or `test/` path, any `demos/scenarios/*/config*.edn`,
`docs/dev/`, `README.md`, `AUTHORS-GUIDE.md`, `Makefile`, or CI
workflows. `out/scenarios/*` not regenerated — the use case's commands
document ADR-0111's own witnessed run, not commands this session
re-executes.

### Index line

```
- 2026-08-11 — batch-straddle-recording — ADR-0112
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Batch-straddle recording: use case, rulings, and the user-guide opening — a docs-and-registers-only session executing the author's own deferred-to-next-session ruling: a new use case, `:supply-batch-straddling-traffic`, lands in `components/corpus/docs/use-cases.edn` next to its transport-realism sibling, commands reused verbatim from ADR-0111's own witnessed demo run, cross-linked into the "Batched delivery" section by footnote marker; `.agents/rulings.md` records both author quotes and the three documentation placements (demo landed, tool-specific user guide opened, EHR Testing Guide Ch 24 the author's own queue) plus a channel-read flag on the user-guide trigger, disclosed honestly as channel-read rather than author-verbatim; `.agents/plans/roadmap.md`'s own user-guide trigger paragraph moves from PENDING AUTHOR RATIFICATION to RATIFIED, and gains a new awaiting-design-pass row plus an author's-queue Externals row; a mid-session STOP-AND-REPORT surfaces a fence conflict the driving prompt's own Read-first list missed — a hardcoded use-case-count test the new case's own landing breaks — resolved by one author-licensed deftest edit, disclosed as a deviation naming the channel's own incomplete-probe error; a same-tree property-test flake in an untouched namespace (`ehrt.sim-engine.engine-test`) is disclosed and shown to clear on re-run; the oracle holds pure identity across all 35 roots

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- **EHR Testing Guide Ch 24 "completeness illusion" section notes**
  (not a session charter): the batch-straddle scenario's guide-side
  treatment (`.agents/rulings.md` "From ADR-0112", "Batch-straddle
  documentation placements", placement (c)). The channel may draft
  notes on request, grounded in the ADR-0111 demo's witnessed run
  (`demos/scenarios/ed-tuesday/README.md` "Batched delivery"); the
  guide itself lives outside this workspace, per `AGENTS.md`.
