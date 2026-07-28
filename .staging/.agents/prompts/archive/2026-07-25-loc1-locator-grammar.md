# LOC-1 — Locator grammar micro-wave: reject the trailing dot, refuse MSH-1

You are working in `ehr-testing-tools` (public). This session executes
LOC-1, a grammar micro-wave the author ordered on 2026-07-25 (option
(b) of the report in `.agents/plans/user-docs.md`'s DOC-3 close-out),
slotted before DOC-4 so the runnable strips never demonstrate against
wart-y behavior. Two deliberate behavior changes to locator parsing,
both tightenings of accepted-input surface, done now because
pre-release is exactly the time to fix grammar warts: (1) a FHIR data
path with a trailing separator (`entry[0].resource.`) is **rejected**
instead of silently parsing as its dot-less form — bringing the FHIR
grammar to the same fully-anchored standard as v2; (2) `MSH-1` is
**refused at parse time** with an error that teaches the convention —
today it parses and silently addresses the encoding characters
(MSH-2's position), the worst kind of success. This session
*intentionally* changes behavior, so the contract tripwire inverts:
Step 0 enumerates exactly which existing tests pin the two warts, and
**only those tests may change**; any other test going red means the
change leaked past its intended blast radius — stop and report. The
third DOC-3 sharp edge (component-level locators like `PID-5.1`
resolving at their field, because the substrate is field-granular)
is **out of scope by ruling**: the v2 grammar is intentionally ahead
of the substrate — parked the way lower/erase are parked — and this
session does not shrink the grammar to match today's substrate.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`,
`.agents/plans/user-docs.md` (DOC-3 close-out's "code-shaped finding"
paragraph — the problem statement this session answers),
`src/ehr_testing_tools/locator.clj` (whole file: `fhir-data-path`'s
split-then-guard structure — the dead guard at its line ~67 is the
fix site — and the v2 grammar block, `v2-path-re`, where the MSH-1
check lands), `src/ehr_testing_tools/corpus/er7.clj`'s ns docstring
and `parse`/`resolve` docstrings (the MSH off-by-one convention the
new error text must teach, and must not contradict),
`docs/locators.md` (both sharp-edge sections — this session rewrites
them), `test/ehr_testing_tools/locators_doc_test.clj` (the pinning
test; its wart entries are the enumerable set allowed to change),
`test/ehr_testing_tools/locator_test.clj` (the grammar's own tests),
`src/ehr_testing_tools/cli.clj`'s enumerable-options error sites from
DOC-1 (the honest-error house pattern the MSH-1 payload follows).
Ritual: commit → `git push origin`. Save this prompt to
`.agents/prompts/2026-07-25-loc1-locator-grammar.md`; final commit
archives it to `.agents/prompts/archive/`.

Author rulings in effect: **Parse-time, not resolve-time** — both
rejections happen in `locator.clj`'s parse functions (the locator
string alone determines both conditions; failing before any file I/O
is the point). **Categories preserved, payloads enriched** — the
trailing-dot rejection flows through the existing
`:invalid-fhir-path` category untouched; MSH-1's refusal stays inside
`:invalid-v2-path` but its payload gains a `:hint` in the DOC-1
house pattern, teaching rather than just refusing — to the effect
that MSH-1 *is* the field separator character itself and is not
addressable as a field, and that the encoding characters live at
`MSH-2` (write it from er7.clj's own docstring account, don't invent
a competing explanation). Callers matching on the categories are
unaffected and a test proves both categories survived. **The
mechanism for the FHIR fix is the split limit** — pass `-1` so the
trailing empty token survives to the existing `(some empty?
segments)` guard; the guard comes back to life rather than a second
guard being added. If that mechanism doesn't produce exactly the
intended change (Step 0's probe matrix is the oracle), stop and
report rather than reaching for a different mechanism — a surprise
here means the premise is wrong somewhere. **No ADR** (my call,
author may strike): this is a bug-class fix recorded in the plan,
the doc, and the commit messages; if you believe it genuinely rises
to ADR grade, propose that at close-out — do not write one unbidden.
**Didactic content survives the fix** — locators.md's explanation of
*why* MSH is off by one is teaching material and stays; only the
"sharp edge / don't rely on it" framing around behavior that no
longer exists is rewritten. **Golden check must stay clean** — the
registry and cli-spec are untouched, so a diff in any generated doc
is scope creep, stop.

## Step 0 — Evidence: the probe matrix and the blast radius

Two enumerations recorded in the commit body. (1) A probe matrix run
against both parsers as they stand: for FHIR — a valid path, the
trailing dot, a leading dot, a doubled dot, a bare `.`; for v2 —
`MSH-1`, `MSH-2`, `MSH-9.1`, `PID-3`, `PID-3.`, a non-MSH field 1
(`PID-1`, which must remain valid). Record each input's current
result (ok/rejected + category); this matrix re-runs after each fix
and only the two intended cells may flip. (2) The blast radius:
every existing test asserting on either wart's current behavior
(expected: the `entry[0].resource.` entry in `locators_doc_test` and
possibly grammar tests in `locator_test`; expected absent: any
MSH-1-accepting test — confirm), and a repo-wide grep for the two
wart forms in fixtures, docs, archived prompts, and the quickstart —
live uses must be zero (archived/historical mentions are fine and
stay put); if a live use exists, stop and report before changing
anything.

Commit: `LOC-1: probe matrix + blast radius (evidence before behavior change)`.

## Step 1 — FHIR: the trailing separator is rejected

The split-limit fix per the ruling. The probe matrix re-run flips
exactly one cell (`entry[0].resource.` → rejected
`:invalid-fhir-path`); leading dot, doubled dot, and bare `.` were
already rejected and stay so; every valid example in
`locators_doc_test` still parses. Test changes confined to Step 0's
enumerated set: the wart-pinning entry now pins the rejection (same
test, inverted expectation — keep it in the doc-examples test so
locators.md's rewritten section stays pinned). A grammar test in
`locator_test.clj` asserting trailing-separator rejection for both
formats side by side, so the parity this fix buys is itself pinned.

Commit: `LOC-1: fhir-data-path rejects trailing separator (split limit -1; guard live again)`.

## Step 2 — v2: MSH-1 is refused, and the refusal teaches

Parse-time check in `locator.clj`'s v2 parse: segment `MSH`, field 1
→ `result/rejected :invalid-v2-path` with the payload's `:hint` per
the ruling. `MSH-2` and every other MSH field stay valid; `PID-1`
stays valid (the check is MSH-specific — field 1 is ordinary data in
every other segment). Probe matrix re-run: exactly one cell flips.
Tests: the refusal with its hint asserted; the category-survival
test; `MSH-2`/`PID-1` positive cases pinned adjacent so the check's
edges are explicit.

Commit: `LOC-1: MSH-1 refused at parse with teaching hint (encoding characters live at MSH-2)`.

## Step 3 — locators.md tells the truth again

The trailing-dot sharp-edge section is rewritten to a one-line
grammar note: both grammars are fully anchored; a trailing separator
is a parse error (dated: tightened by LOC-1, 2026-07-25 — same
annotation style as DOC-3's handoff edit). The MSH-1 section keeps
its didactic account of the off-by-one and the delimiter convention,
but the "don't write MSH-1 / it parses" warning becomes "you can't
write MSH-1 — here is the error you'll get and why," quoting the
hint's substance. The component-granularity sharp edge is untouched
(out of scope by ruling; it is still true). Confirm the pinning test
and the rewritten prose agree — the test is the doc's enforcement.

Commit: `LOC-1: locators.md — sharp edges become grammar facts (didactic content preserved)`.

## Step 4 — Close out

Plan updates in `.agents/plans/user-docs.md`: a short LOC-1 section
(interlude, not a DOC wave — placed after DOC-3's section) recording
the author decision (option (b), decided 2026-07-25), what changed,
and the component-granularity edge's explicit out-of-scope status
with its parked-like-lower/erase rationale; a LOC-1 tracker row →
Done with itemized summary and prompt path. Full suite + both lints
green — with the test-change set at close-out matching Step 0's
enumeration exactly (any extra changed test file is the leak the
inverted tripwire exists to catch; report it, don't rationalize it).
Extended golden check clean. Final probe matrix in the close-out
commit body: all cells, before → after, exactly two flips. Archive
this prompt.

Commit: `LOC-1 complete: both grammars fully anchored, MSH-1 teaches (archives prompt)`.
