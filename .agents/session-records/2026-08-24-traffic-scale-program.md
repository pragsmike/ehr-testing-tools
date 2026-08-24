# 2026-08-24 -- the traffic-scale program lands as documents: ADR-0168, the traffic-model doctrine, the program plan, nine register rows and four roadmap arcs

**Prompt:** [`.agents/prompts/2026-08-24-traffic-scale-program.md`](../prompts/2026-08-24-traffic-scale-program.md)
**ADR:** [`notes/adr/0168-traffic-scale-program.md`](../../notes/adr/0168-traffic-scale-program.md), minted this session.
**Mode:** docs-only landing session, one commit, local only. **Base:** `7c3418d`, tree clean.

**Result, one sentence:** a design-channel conversation that existed nowhere
in the repo is now repo-recorded in four places -- an anchoring ADR, a
doctrine document, a program plan, and nine standing register rows -- with
four roadmap arcs carrying it forward and every payload-vs-landed delta named
rather than absorbed.

## 1. Scope

The session was asked to land three authored payloads verbatim (an ADR, a
doctrine document, a program plan), append nine register rows, add four
roadmap rows, update a Q3 roadmap row, regenerate the derived indexes, and
commit once without pushing. It did all of that. Fence F1 held absolutely:
zero `src`, zero `test`, zero schema, zero vendored content changed --
verified by the final `git status`, which touches only `notes/`, `docs/dev/`,
`.agents/`, and the two generated indexes.

What landed:

| Artifact | State |
| --- | --- |
| `notes/adr/0168-traffic-scale-program.md` | new, 115 lines |
| `docs/dev/traffic-model.md` | new, 86 lines |
| `.agents/plans/2026-08-24-traffic-scale-program.md` | new, 75 lines |
| `.agents/rulings.md` | +20 lines, nine rows (`R-mix-1`..`R-mix-7`, `R-skeleton-or-emission`, `R-per-person-streams-before-generator-fixes`) |
| `.agents/plans/roadmap.md` | one row rotated out, four rows in; 301 -> 320 lines |
| `.agents/plans/roadmap-done-2026-08.md` | +8 lines, the rotated row verbatim |
| `.agents/plans/README.md`, `docs/dev/README.md` | one index entry each |
| `notes/ADRs.md`, both record `INDEX.md`s, `.agents/state-derived.md` | regenerated |

## 2. Verification before landing, and the F4 question

F4 made a tree-contradicts-payload claim a FINDING, so the four load-bearing
claims were re-probed against the live tree BEFORE the documents that cite
them were written. All four hold, and none triggered a stop:

- **Churn's six step types.** `components/sim-engine/src/ehrt/sim_engine/churn.clj`
  declares `#{:cancel-admit :cancel-transfer :cancel-discharge
  :transfer-in-error :bed-swap :merge}` under namespace
  `ehrt.sim-engine.churn`. The payload's list and namespace match exactly,
  set and spelling.
- **"~900 events, the largest witnessed single run (ADR-0090)."** Holds. A
  survey of every three-to-six-digit event count in `notes/adr/*.md` returns
  269 / 383 / 407 / 900 / 4,997, and the 4,997 is explicitly "across eleven
  runs" (ADR-0141), not a single run. 900 is the largest single-run figure
  the tree records.
- **"Gated corpora run 343-407."** Holds: ADR-0163's post-fix seed-424242 run
  is 343 events; `roadmap.md#ed-tuesday-module-tail-inert` records 407 at
  seed 202.
- **The decide-time scans the plan proposes to index away.** Real.
  `engine.clj` carries two whole-log scans (the `:order-citation` and
  care-plan resolution pair), patient-scoped rather than removed by ADR-0164;
  `engine.clj`'s own comments name the "whole log in hand" shape. The
  payload's `O(n^2)` LABEL is not in the tree and was not measured -- but
  PAYLOAD C already puts it behind the throughput spike and inside the
  estimates appendix, so it landed as written rather than promoted. This is
  the one place the payload writes with more specificity than the tree
  supports, and it is disclosed rather than silently accepted.

Also probed and consistent: `components/sim-model/src/ehrt/sim_model/persona.clj`
exists (the "Persona sampled once, static" claim); `check.clj` does carry the
referential family twice (ADR-0163/0166 lineage); the latency machinery is
ADR-0110's; and `corpus-player` is correctly referred to as chartered-unbuilt
-- `roadmap.md#corpus-player-slices` names exactly the bed-board sink and
`:mllp` slices the payload cites, from ADR-0014, with no row in any register
until that one.

## 3. Payload-vs-landed deltas -- four mechanical fix-forwards, each named

F2 requires every delta disclosed. There are four, all mechanical; none is a
content change, and no payload sentence was rewritten.

**(a) The doctrine document landed at `docs/dev/traffic-model.md`, not
`docs/traffic-model.md`.** This is the only delta worth argument, and three
independent reasons converge on it, which is why it was taken as fix-forward
rather than a STOP (`rulings.md#R-stop-only-on-two-defensible-readings`: a
STOP binds where two readings are BOTH defensible, and the `docs/` placement
is not defensible under any of the three):

1. `ehrt.docs-tooling.link-footnote-gate-test/no-visible-adr-token-in-prose-test`
   scans `docs/**/*.md` with `docs/dev/` explicitly excluded, and forbids a
   visible `ADR-NNNN` token anywhere in prose (ADR-0102's ruling). PAYLOAD B
   carries eight of them (`ADR-0168` twice, `ADR-0163/0166`, `ADR-0110`).
   Landing at `docs/traffic-model.md` would have been RED on arrival. The
   only alternative -- converting each to footnote markup -- rewrites the
   payload's own prose, which F2 protects.
2. `rulings.md#R-two-voices-two-homes` puts maintainer content in dev docs.
3. `docs/README.md`'s own standing promise is that `docs/` proper carries no
   Polylith vocabulary; PAYLOAD B says "New component" and names
   `components/sim-model`.

Relocating preserves the payload text verbatim where footnoting would not, so
the relocation is the delta that costs least. Internal cross-links in
PAYLOAD A and PAYLOAD C were updated to the landed path, which the prompt's
own preamble licenses ("adjust internal cross-links to the paths as landed").

**(b) PAYLOAD A's headings are demoted one level.** `notes/ADRs.md` is
GENERATED by `make adr-index` from a `^## ADR-(\d{4})\s*—` match plus a
`**Status:**` line in the header block. PAYLOAD A led with `#` and used `##`
for its sections; landed as `##` / `###`. Verified by regenerating: the index
now renders `- **ADR-0168** — the traffic-scale program: ... — Accepted`.

**(c) There is no Q3 row in `.agents/plans/roadmap.md` to update.** Step 6
asked for one. A repo-wide grep finds no roadmap, register, or docs row for
the shared-RNG limitation at all: it was recorded in ADR-0163's own "Blast
radius: one shared RNG, disclosed" section, with its measured two-seed table,
and never rowed anywhere. The absence admits one reading, not two, so this
was fix-forward rather than a STOP. The conversion is carried explicitly by
the arc-1 row `roadmap.md#stream-partition-design`, created already stating
it -- the shape ADR-0139's own close used when it found no review-3 arc row
to flip. No standalone Q3 row was minted, because it would have duplicated
arc 1's content and burned reading-set budget for a second copy. `Q4` was not
touched, as instructed; there is nothing named Q4 in the tree either.

**(d) The prompt's reading-list item 4 cites `docs/operational-models.md`,
which does not exist.** The sibling doctrine document is
`components/sim/docs/operational-models.md`, and that is what was read for
house style. Pointer correction only; nothing downstream depended on it.

## 4. Roadmap and register mechanics

**Attic rotation ran FIRST, before any line was added**, per step 1 and
ADR-0161. `## Done` measured **exactly 30 lines including its header** --
the extent `ehrt.docs-tooling.attic-rotation-test` counts -- i.e. at cap, as
the prompt said. The oldest whole row, `CLOSED 2026-08-21 ADR-0162
[patient-simulator-charter]` (6 lines), was appended verbatim to
`.agents/plans/roadmap-done-2026-08.md` under a new
`## Rotated 2026-08-24 by the ADR-0168 landing` heading and deleted from the
live section, which now stands at 24 lines. Byte-for-byte relocation: the
rotated block was captured with `sed -n` and appended unmodified.

**Four `## Next` rows** at PRIORITY 26-29, ascending after the existing 25,
one per arc, each within the six-line cap and each pointing at the plan
document. Slugs: `stream-partition-design`, `person-simulator`,
`engine-fold-extensions`, `emission-add-ons`. Both outbound
`roadmap.md#<slug>` citations they carry (`corpus-player-slices`,
`stream-partition-design`) resolve to real rows.

**Nine register rows**, each three lines or fewer, each citing ADR-0168, each
matching the gated grammar. The R-mix rows kept their channel-given numeric
slugs (`R-mix-1`..`R-mix-7`) rather than being renamed to semantic ones: the
register's slug pattern `[a-z0-9-]+` admits them, and PAYLOAD A and PAYLOAD B
both cite them by that name, so renaming would have broken two payloads to
satisfy nothing. The two unnumbered rulings took semantic slugs --
`R-skeleton-or-emission` for the classification principle,
`R-per-person-streams-before-generator-fixes` for the Q3(b) conversion.

**Both registers stay pure ASCII** (verified: zero non-ASCII bytes in each,
before and after), so the rows use `->` and `x` where the payloads used
arrows and multiplication signs. The three landed DOCUMENTS keep the
payloads' own typography.

**Reading-set budget, checked rather than assumed.** `:onboarding` carries
both registers and both `README`s this session grew. Actual moved 1,423 ->
1,463 against a baseline-ratcheted budget of **1,530**: green with 67 lines
of headroom, no bump needed, and `rulings.md#R-budget-stop` never engaged.
Row lengths were chosen with that ceiling in view.

## 5. Gate results

**`clojure -M:poly test brick:docs-tooling`, unpiped, exit 0** -- 108
namespace runs, **796 tests / 5,182 assertions, 0 failures, 0 errors**. Every
gate this session could have broken is in that population and ran green:

| gate | what it would have caught here |
| --- | --- |
| `roadmap-lint-test` | row tokens, slug uniqueness, the six-line cap, `PRIORITY` ordering, and that both `roadmap.md#<slug>` cites in the new rows resolve |
| `rulings-lint-test` | the nine new rows against the gated grammar, slug uniqueness file-wide, the three-line cap, and that `ADR-0168` names a real record |
| `attic-rotation-test` | the 30-line `## Done` cap and that no attic file deleted a line, over the file's whole committed history plus the working tree |
| `adr-index-test` | that `notes/ADRs.md` is EXACTLY what the generator renders, and that the new ADR carries a parseable `## ADR-NNNN — Title` heading and `**Status:**` line |
| `link-footnote-gate-test` | the gate that forced delta (a) -- it is why the doctrine document is in `docs/dev/` |
| `stale-path-test` | dead markdown links and retired-namespace strings across `docs/` and the four live `.agents/` plan files |
| `index-completeness-test` | both directions on `.agents/plans/`, `.agents/prompts/`, `.agents/session-records/` -- the new plan, prompt and record are all indexed, no ghosts |
| `prompt-record-pairing-test` | that this record and its prompt archive share a slug, both directions |
| `reading-set-budget-test` | `:onboarding` at 1,463 against its 1,530 ratchet |
| `state-staleness-tripwire-test` | that ADR-0168 is not an arc close wearing the wrong filename |

Indexes regenerated: `make adr-index` (exit 0, `notes/ADRs.md` gains the
ADR-0168 row) and `make state-derived` (exit 0, via `bin/close-scaffold`,
which also re-derives `docs/dev/pipeline.md` -- byte-identical, so it does
not appear in the diff). `.agents/state-derived.md` moves exactly as
predicted and by nothing else: ADR files 165 -> 166, rulings rows 116 -> 125,
roadmap `## Next` 23 -> 27, `## Done` 5 -> 4, records and prompts each +1.

**No full `make test`, and this is not a dispensation.** See section 6.

## 6. Judgment calls and their ratification status

- **The `docs/dev/` relocation (delta (a)) is not ratified.** It is a
  session judgement made on three convergent gate-and-rule grounds and is
  the one delta an author might want to overturn. Overturning it costs a
  `git mv` plus footnote conversion of eight ADR tokens in PAYLOAD B, plus
  three cross-link edits; nothing else depends on the path.
- **Folding the Q3 conversion into the arc-1 row instead of minting a
  standalone row (delta (c)) is not ratified.** The alternative -- a fifth
  row whose whole content restates arc 1 -- was rejected on duplication and
  budget grounds, not on principle.
- **Skipping the full suite is within standing law, not a dispensation this
  session invented.** `rulings.md#R-full-suite-before-push` binds a PUSH, and
  this session does not push. The prompt's step 0 called it "the standing
  docs-session dispensation"; the honest statement is narrower and is made
  here rather than borrowed: no push, therefore no full-suite obligation, and
  the doc gates that DO cover every file this session touched were run.

## 7. Findings and HEAD landed

**No F4 finding.** Nothing in the three payloads was contradicted by the
tree. Two soft observations worth carrying rather than losing:

- **F-1 (carried, not fixed).** The `O(n^2)` characterization of the
  decide-time scans is the plan's own and appears nowhere in the tree; the
  scans themselves are real and are patient-scoped, not removed. The plan
  already gates the claim behind its throughput spike, so this is a note for
  whoever commissions arc 3, not a defect.
- **F-3 (self-caught, corrected in session).** The first attempt at this
  record's closing line quoted the commit's own sha, which required a
  `git commit --amend` carrying a CONTENT change --
  `rulings.md#R-amend-unpushed-message-only` permits `--amend` only for a
  message. Caught immediately, the commit was rebuilt from
  `git reset --soft 7c3418d` rather than amended a second time, and the line
  now uses the prior docs-only session's own shape. The tree's single commit
  has therefore never had its content amended; the disclosure stands anyway,
  because the mistake was made before it was unmade.

- **F-2 (carried, not fixed).** The shared-RNG limitation had no register
  home anywhere before this session -- exactly the class
  `rulings.md#R-unregistered-request-gets-a-row` exists to catch, and it went
  uncaught from ADR-0163 (2026-08-23) until now. It has one now.

Landed as ONE commit on top of `7c3418d`, local only; no push, no tag, per
step 9. The commit's own sha is deliberately not quoted here: this record is
inside it, and naming it would force either a content `--amend` (which
`rulings.md#R-amend-unpushed-message-only` forbids) or a second commit the
prompt does not allow. The prior docs-only session's record uses the same
shape.
