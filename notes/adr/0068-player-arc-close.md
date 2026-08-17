## ADR-0068 — Player arc close: the hospital is watchable, the suite is honest, the state regenerates

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the player board landed and was design-channel-verified
(`f9e4afc`, `notes/adr/0067-player-board.md`). This session closes the
player arc (ADR-0066–0068) per the standing close pattern (ADR-0064 is
the model, ADR-0055 its ancestor): rulings appends, the dependency-
review cadence, `state.md` regeneration, budget re-derivation, Done
rotation, and the closing ADR. Docs-only; anything new found is
next-arc intake, never an act.

The arc being closed: two sessions — player fold (ADR-0066, `01d9459`:
the accumulator total over the emitter's trigger set, absolute time,
the coherence property over full churn as spec) and player board
(ADR-0067, `f9e4afc`: the `--board` whiteboard, the corpus→sim-emit-hl7
edge, and the rider that made the suite green on a fresh clone for the
first time since ADR-0060). The interlude session ux epilogue
(ADR-0065, `03a8698`) preceded the arc and rotates with it (AR-PC-5),
not counted among the two.

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07):

**AR-PC-0 (tag, standing ceremony).** Annotated
`stable-20260807-player-board` at `f9e4afc`, message "player board
landed, design-channel-verified 2026-08-07 (ADR-0067)"; push; verify
peeled ref.

**AR-PC-1 (rulings appends).** Under a new "From the player arc
(ADR-0066–0068)" section in `.agents/rulings.md`: (a) `[A]` tests build
their own directories, standing — the author's own sentence verbatim
("Tests should build their own directories as needed"), ruled
2026-08-07, recorded ADR-0067 AR-BB2-R with its append explicitly
deferred to this close per the register's own contract; a test that
reads a live mutable repo directory is the violation, tracked
test-fixture directories are fine. (b) `[C]` folds stay strict, sinks
stay lenient — the coherence-law fold rejects what it cannot faithfully
reconstruct (`:unsupported-trigger`); a display sink absorbing foreign
traffic skips-with-cue and counts, never crashes and never silently
mis-folds (ADR-0066's strict boundary + ADR-0067's board-sink design,
generalized; channel-inferred — the author may strike it).

**AR-PC-2 (dependency-review cadence).** `clojure -M:poly libs
:outdated`; dated report lands in this ADR; no edit follows; urgent-
looking upgrades are intake notes.

**AR-PC-3 (state regenerates).** Every `[V]` claim probe-backed THIS
session; skeleton preserved; content updates at minimum: the component
graph (corpus → sim-emit-hl7 is a new edge, the graph's first change
since the regeneration-of-record); the `emit_hl7.clj` require-form
claim reworded per ADR-0065 AR-EP-5; the gate inventory; the suite
posture (fresh-clone-green for the first time since ADR-0060); the
founding-incident section (now SIX guarded failures); the CLI surface
(`--width`/COLUMNS, `--board`); the tag census; Deferred/Next by fresh
count. Regeneration table below; a wrong-in-kind discrepancy is
STOP-AND-REPORT.

**AR-PC-4 (budgets).** Re-derive every reading set whose member paths
changed across `2e77096..HEAD`, after the regeneration lands.

**AR-PC-5 (rotation).** Done currently holds four pointers (0064–0067,
design-channel probed at `f9e4afc`). Rotate: ADR-0064's pointer appends
to the attic's existing UX-arc section with a dated note (the
disclosed-leftover class); ADR-0065 (ux epilogue) also appends to the
UX-arc section with a dated note naming it the arc's epilogue; ADR-0066
–0067 rotate under a new `## Player arc — closed 2026-08-07
(ADR-0066–0068)` header. ADR-0068's pointer lands as the sole current
entry (Step 3, after the index line). The Now section's stale
2026-08-05 explanatory text refreshes to one line stating nothing is
in progress at this close.

**AR-PC-6 (the closing ADR).** `notes/adr/0068-player-arc-close.md`:
rulings verbatim; the arc summary one line per session with ADRs and
tips; the arc narrative — ADR-0014's own deferred surface exists, fed
by a fold the coherence property specifies, over any ER7 traffic
foreign or own; the fresh-clone-green milestone with the rider's
mechanism; the intake list for the next arc, each item cited: fold
leniency for absent PID fields on foreign traffic (`hl7-date->iso`
NPEs on a missing PID-7 — ADR-0067's own recorded finding), the sim
event-log input adapter (roadmap Next, ADR-0014), `ehrt.corpus.display`
placement (roadmap Next, ADR-0018), the `:mllp` sink (Deferred, on its
trigger); the open Externals restated unchanged; this close's own
mechanical debt recorded HERE (the next opening session tags
`stable-20260807-player-close` at this session's own closing tip under
standing ceremony); and the horizon note (verbatim, below).

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (ext4, `df -T`
reports `ext4` on `/dev/sdd`); tip `f9e4afc` exactly; working tree
fully clean, no untracked files at all (a stronger baseline than any
prior close — no `config/busy-weekday.md` disclosure needed, since
ADR-0067's own AR-BB2-R rider made that fixture's presence irrelevant
to any test). Baseline: `clojure -M:poly check`: OK. Full suite
(`clojure -M:poly test :all skip:integration`): 227 `Test results:`
lines, 0 `FAIL`/`ERROR`/`Exception` anywhere — fresh-clone-green, the
standing expectation this session's own prompt named. `gitleaks detect
-v`: 699 commits scanned, no leaks. Oracle pre-digest
(`bin/regression-oracle f9e4afc f9e4afc`): all eleven roots IDENTICAL,
soundness "yes outside ns form."

**AR-PC-0 — the tag, executed.** `stable-20260807-player-board` did
not exist locally or on origin (checked both); created annotated at
`f9e4afc`, message "player board landed, design-channel-verified
2026-08-07 (ADR-0067)"; pushed; verified — peeled ref resolves to
`f9e4afc` exactly (`git ls-remote --tags origin`, `git tag -v`,
`git rev-parse stable-20260807-player-board^{commit}`).

### Step 1 — appends + report (AR-PC-1/2)

Two rulings landed in `.agents/rulings.md` under "From the player arc
(ADR-0066–0068)": tests build their own directories (standing law);
folds stay strict, sinks stay lenient (channel-inferred). Committed
`854e679` ("docs: the player arc's law is appended -- tests own their
directories, folds stay strict (arc close, AR-PC-1/2)"), pushed.
Post-push verification: one delta against the message file, the known
harmless trailing-newline artifact.

**AR-PC-2 — the `libs :outdated` report, captured 2026-08-07 against
tip `f9e4afc`:**

```
library                                   version  latest   type      KB
------------------------------------------------------------------------
ca.uhn.hapi.fhir/hapi-fhir-base           8.2.0    8.10.1   maven  1,164
ca.uhn.hapi.fhir/hapi-fhir-structures-r4  8.2.0    8.10.1   maven     29
ca.uhn.hapi/hapi-base                     2.6.0             maven    653
ca.uhn.hapi/hapi-structures-v24           2.6.0             maven  1,446
gov.nist/hl7-v2-parser                    1.7.3             maven    229
gov.nist/hl7-v2-profile                   1.7.3             maven    123
gov.nist/hl7-v2-validation                1.7.3             maven  1,051
io.github.cognitect-labs/test-runner      dfb30dd           git       26
metosin/malli                             0.20.1            maven     97
org.babashka/cli                          0.12.79  0.12.86  maven     35
org.clojars.cmiles74/clojure-hl7-parser   3.5.1             maven     18
org.clojure/clojure                       1.12.5            maven  4,129
org.clojure/data.json                     2.5.2             maven      9
org.clojure/test.check                    1.1.3             maven     39
org.slf4j/slf4j-nop                       2.0.17            maven      4
```

**Unchanged from the UX arc's own AR-UC-2 report** (`notes/adr/
0064-ux-arc-close.md`) — every coordinate, version, and `latest` value
identical: no new upstream release surfaced during the entire player
arc. The same three coordinates still show a newer `latest`:
`ca.uhn.hapi.fhir/hapi-fhir-base` and `ca.uhn.hapi.fhir/hapi-fhir-
structures-r4` (8.2.0→8.10.1), and `org.babashka/cli` (0.12.79→0.12.86,
dev-tooling-only). No listed upgrade reads as security-relevant — a
NOTE for next-arc intake per AR-PC-2's own fence, not an act. No
`deps.edn` edit made or considered.

### Step 2 — state + budgets + rotation (AR-PC-3/4/5)

**State.md regeneration table (claim → probe → disposition).** Held
claims are not re-listed exhaustively; this table records every claim
that changed since the prior regeneration (`f5af489`, ADR-0064) or that
this session's own fresh probe corrected:

| # | Claim | Probe | Disposition |
|---|---|---|---|
| 1 | Component graph — corpus→sim-emit-hl7 edge | `grep -n "fold-message" .../sim_emit_hl7/interface.clj`; fresh caller-grep for `sim-emit-hl7.interface` | **NEW claim, confirmed live.** `corpus`'s board sink is `v2-replay`'s first real external caller (ADR-0067 AR-BB2-1), exported as `fold-message` (2-arg) alone; zero `deps.edn` edits needed anywhere. |
| 2 | `sim-engine` external caller set | Fresh grep of every real `ehrt.sim-engine.interface` requirer | **HELD at 5** — `sim-check/check.clj`, `sim-emit-fhir/emit_fhir.clj`, `sim/identifiers.clj`, `sim/run.clj`, `oracle/digest.clj` — unchanged, the player arc touched `sim-emit-hl7`/`corpus`, never `sim-engine`. |
| 3 | `emit_hl7.clj`'s own `:require` form claim | Fresh read of the `ns` form | **REWORDED, not changed in substance** (ADR-0065 AR-EP-5's own disclosed correction, discharged here) — the form holds FOUR requires; the two named `ehrt.*` ones are the only `ehrt.*` requires, not the entire form. The gated invariant held throughout; only the prior PHRASING was imprecise. |
| 4 | `docs-tooling` gate-family file count | `ls components/docs-tooling/test/ehrt/docs_tooling/ \| wc -l` | **UPDATED 21→22** — one new gate this arc (ux epilogue, which precedes and rotates with the UX arc): `cli_tombstone_test.clj`. |
| 5 | `bases/cli` gate files | `ls bases/cli/test/ehrt/cli/` | **UPDATED 6→7** — `retired_test.clj` new (ux epilogue, ADR-0065 AR-EP-1); `core_test.clj`/`help_wrap_test.clj` both GREW (the `--width`/COLUMNS tests, the `--board` cadence/precedence/cue tests) without gaining new files. |
| 6 | `components/corpus` test tree | `ls components/corpus/test/ehrt/corpus/` | **NEW claim, confirmed live** — `board_test.clj` is the one new file (player board, ADR-0067 AR-BB2-3/4); `run_test.clj` (sim) grew by one test (the AR-BB2-R rider) without a new file. |
| 7 | Suite posture | `clojure -M:poly test :all skip:integration` | **UPDATED — fresh-clone-green for the first time since ADR-0060.** 227 namespaces, 227 `Test results:` lines, 0 failures/0 errors, confirmed at this session's own Step 0 baseline — no pre-existing failure disclosed, unlike every close since ADR-0060 landed. Mechanism: ADR-0067's own AR-BB2-R rider rewrote `merge-config-file-suggests-a-same-stem-sibling-file` to build its own temp-dir fixture instead of reading the live, author-held `config/busy-weekday.md`. |
| 8 | Founding-incident live status | Live `bin/ehrt`/`clojure -M:cli` probes: the founding command shape, config crash, typo, bare invocation, sibling near-miss, tombstone | **UPDATED — now SIX guarded failures**, two new since the UX arc's own close: the runtime tombstone of `clojure -M:cli` (ux epilogue, ADR-0065 AR-EP-1) and the founding fixture's own retirement from load-bearing test state (player board, ADR-0067 AR-BB2-R). All six re-confirmed live this session — full transcript in `.agents/state.md`'s own regenerated "Live work" section. |
| 9 | Config-crash payload shape | `bin/ehrt sim run --seed 1 --patients 1 --config config/busy-weekday.edn` | **CORRECTED** — the prior regeneration's own recorded transcript showed a `:did-you-mean` key present; on a tree where the sibling `.md` fixture is ceremonial/absent (the now-standard case), the payload omits that key. Both shapes are correct, by design; the claim is now probed against the leaner, correct assumption. |
| 10 | CLI surface | `grep -n width\|board` across `help.clj`/`core.clj`; live `--width`/`--board` probes | **GROWN, confirmed live** — `--width N` (ux epilogue, AR-EP-3: explicit flag beats `COLUMNS` beats 80-column default, user input rejected by name, ambient input falls back silently); `ehrt play PATH --board N` (player board, AR-BB2-2/3/4: display-only, ignored when `--sink` given, wins over `--ticker`). |
| 11 | `stable-*` continuity tag count | `git tag -l 'stable-*'`, excluding the three frozen legacy tags | **UPDATED 16→20** — four new since the UX arc's own close: `-ux-close`, `-ux-epilogue`, `-player-fold`, and this session's own `-player-board`. |
| 12 | Deferred row count | `awk '/^## Deferred/,/^## Done/' roadmap.md \| grep -c '^- '` | **HELD at 11** — the UX epilogue's own AR-EP-4 relocated two long-dangling rows to Done before this arc opened; the player arc itself touched zero Deferred rows. |
| 13 | Next row count | `awk '/^## Next/,/^## Externals/' roadmap.md \| grep -c '^- '` | **UPDATED 11→9** — the two corpus-player named futures this arc delivered (bed board/census sink, accumulator wiring) moved to Done in ADR-0067; the sim event-log input adapter is the one corpus-player row still Next, this arc's own named intake item. |
| 14 | Reading-set budgets | Diff every set's `:paths` against `git log 2e77096..HEAD --name-only` | **`:onboarding` and `:corpus` re-derived** (AR-PC-4, below) — the two sets with a touched member path; `:sim`/`:judge`/`:docs` HELD unchanged. |

Full regenerated content landed in `.agents/state.md` (this session's
own commit `0ebca6d`); see that file directly for the complete text,
not reproduced here per this arc's own inherited "session narrative
hierarchy" discipline.

**Budget re-derivation (AR-PC-4).** `git log 2e77096..HEAD --name-only`
(`2e77096` = the UX arc's own closing commit, the base since which
`state.md`/`reading-sets.edn` were last touched) diffed against every
reading set's own `:paths`: two sets have a touched member.
`:onboarding` — `.agents/plans/roadmap.md` (Now/Done churn across the
ux-epilogue/player-fold/player-board sessions, shrunk again this
session by AR-PC-5's own rotation). Fresh actual, measured AFTER the
rotation landed: 284 (`AGENTS.md`) + 49 + 57 + 141 + 89 + 33 (the five
`.agents/*/README.md` files) + 197 (`roadmap.md`, post-rotation) + 172
(`build-session/SKILL.md`) = **1022**. Re-applying the standing formula
(actual × 1.15, rounded up to the nearest 5): 1022 × 1.15 = 1175.3 →
**1180**. Budget moves **1205 → 1180** — a decrease, since the
rotation shrank `roadmap.md` faster than the arc's own churn grew it.
`:corpus` — `components/corpus/src/ehrt/corpus/interface.clj` (the new
`board-fold-event`/`board-render-snapshot` re-exports, ADR-0067
AR-BB2-1). Fresh actual: 284 (`AGENTS.md`) + 150
(`corpus/interface.clj`) + 104 + 119 + 212 + 732 + 172
(`build-session/SKILL.md`) = **1773**. Re-applying the standing
formula: 1773 × 1.15 = 2038.95 → **2040**. Budget moves **1995 →
2040**. Both landed in `.agents/reading-sets.edn` (this session's own
commit `0ebca6d`), dated comment blocks matching the file's own
established re-derivation-note convention. `:sim`/`:judge`/`:docs`
confirmed untouched by the diff — no re-derivation.

**Done rotation (AR-PC-5).** ADR-0064's own pointer — the UX arc's own
closing ADR, left as the live roadmap's sole current entry at that
arc's own close — relocates into the attic's EXISTING `## UX arc —
closed 2026-08-06 (ADR-0056–0064)` section, with a dated append note,
the same disclosed-leftover class both prior closes handled for their
own predecessors. ADR-0065 (the UX epilogue) ALSO joins that same
section, with a dated note naming it the arc's own epilogue — it
patched the UX arc's own surface (the tombstone, `--width`), so it
rests with that arc rather than the player arc that follows it. A new
`## Player arc — closed 2026-08-07 (ADR-0066–0068)` header holds
ADR-0066 and ADR-0067's own Done pointers, relocated verbatim. The live
roadmap's own Done section holds an HTML-comment marker (not a
pointer) recording that this ADR's own pointer is deferred to Step 3 —
the same dangling-reference sentinel-avoidance ADR-0055's own AR-AC-5
and ADR-0064's own AR-UC-5 both disclosed, applied preemptively this
time rather than caught live. The Now section's own stale 2026-08-05
explanatory text refreshed to one line: nothing in progress at this
close.

Full suite green throughout (227 `Test results:` lines, 0 failures/0
errors, matching Step 0's own baseline shape exactly — a docs-only
step). `clojure -M:poly check`: OK.

Committed `0ebca6d` ("docs: the state regenerates, the budgets
re-derive, three arcs rest in the attic (arc close, AR-PC-3/4/5)"),
pushed. Post-push verification: one delta against the message file,
the known harmless trailing-newline artifact.

### The founding incident, now six mechanisms

The player arc adds two mechanisms to the four the UX arc closed
(`notes/adr/0064-ux-arc-close.md`'s own founding-incident closure
narrative: stale invocation → lint+path gates; opaque config crash →
named Result with did-you-mean; silent typo → spec-derived rejection;
agent-voice help → operator voice, gated, wrapped):

5. **The runtime half of the stale-invocation failure was never
   actually tombstoned** — `clojure -M:cli` had no alias at all in the
   monorepo root, so it silently mis-parsed a bare positional argument
   as a script path. Closed by the UX epilogue (ADR-0065 AR-EP-1): a
   real `:cli` alias now redirects with a named message and exit 2,
   gated two ways (`retired_test.clj`, `cli_tombstone_test.clj`).
6. **The founding fixture's own load-bearing status was itself a
   hazard** — a test depending on an untracked, author-held scratch
   file (`config/busy-weekday.md`) meant the suite had never been green
   on a genuinely fresh clone since ADR-0060. Closed by the player arc
   (ADR-0067 AR-BB2-R, the author's own verbatim ruling: *"Tests should
   build their own directories as needed"*): the test now builds its
   own temp-dir fixture.

### Intake for the next arc

- **Fold leniency for absent PID fields on foreign traffic** (ADR-0067's
  own recorded finding, Step 3's live-probe fixture work): the frozen
  `v2_replay.clj` fold path throws a nil `subs` NPE inside
  `hl7-date->iso` when a message is missing PID-7 (date of birth) — this
  session's own synthetic test fixtures needed PID-7 added to avoid it,
  but the underlying fold itself has no leniency for the field's
  absence. `.agents/rulings.md`'s own new "folds stay strict, sinks
  stay lenient" ruling (AR-PC-1(b)) makes this a real design question,
  not a bug report: a foreign ER7 feed that omits PID-7 crashes the
  fold today. Named here, unruled — the next session that wires the
  board (or any future sink) against real foreign traffic owns this.
- **Corpus player: sim event-log input adapter** (`notes/adr/
  0014-corpus-player.md`, roadmap Next) — an input adapter reading the
  sim's own event log directly, as opposed to the HL7 v2 file/directory
  input the player accepts today. The last of ADR-0014's own three
  named corpus-player futures still unbuilt; the other two (bed
  board/census sink, accumulator wiring) are this arc's own delivery.
- **`ehrt.corpus.display` placement** (ADR-0018 named-future, roadmap
  Next) — presentation-leaning code whose component home is still an
  open question; `ehrt.corpus.board` (this arc's own new namespace)
  deliberately did NOT resolve it, landing in its own namespace instead
  (ADR-0067's own disclosed reasoning: `board.clj` needs a
  `sim-emit-hl7` require `player.clj` deliberately doesn't carry, and
  entangling `display.clj`'s own open question wasn't this arc's job).
- **The corpus player's own `:mllp` transport sink** (Deferred, `notes/
  adr/0014-corpus-player.md`'s own bail-out, restated unchanged since
  the alignment arc): still deferred whole, on its own trigger (a
  session needs wire transport and a lands-small shape is identified).
  Not new to this arc — restated here because the board sink this arc
  built is exactly the kind of consumer that could eventually want it.

### Open Externals, restated unchanged

**NIST licensing inquiry** — narrowed, not resolved; still "maintained
privately by the author," sending it is AUTHOR ACTION. **IG pinning**
— the profile-tier conformance target still undecided. **SETUP
rewalk** — still owed, an unspoiled human reader. **Clojars publish**
— ruled, deferred; F-5/F-6 remain open decisions; F-7's own close-as-
fine disposition still carries its standing forward note (re-run its
three-point pre-publish checklist immediately before the actual first
Clojars publish). `/mnt/c` disposition — closed (ADR-0047 AR-C-3),
unchanged. None of these five rows was touched by the player arc;
restated here, not re-decided, per this session's own docs-only fence.

### This close's own mechanical debt, recorded here

Unlike the UX epilogue's own disclosed citation-precision miss
(`notes/adr/0065-ux-epilogue.md` AR-EP-5(2): the `stable-20260806-ux-
close` tag debt was recorded in the archived close prompt only, never
in ADR-0064 itself) — this close's own debt is recorded in BOTH places
from the start, the lesson applied rather than repeated: **the next
arc's opening session tags `stable-20260807-player-close` at THIS
session's own closing tip under standing ceremony.** No tag is created
by this session for its own closing tip — the tag law's own case (ii)
licenses a session to tag its PREDECESSOR's verified stable point, not
its own mid-flight tip; the next session inherits that debt exactly as
every prior close has passed it forward.

### The horizon note (verbatim, per this session's own prompt)

"The horizon: the vendoring arc (ratified, ADR-0066 AR-BB1-R — census
substance qualifier first, then a design-channel curation pass over the
ranked catalog, then vendoring sessions batched by closure family), the
pairing-as-data design pass (design channel, landing spot `judge`,
ADR-0050 D-3), `sim-emit-cda` on its trigger. Publish-prep gates: F-5/
F-6 decisions plus the alignment register's F-7 checklist."

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line; `notes/adr/
README.md`'s own file count corrected 65→66 ("as of ADR-0068"), verified
by `ls`, not arithmetic. Roadmap gets its Done pointer, the sole
current entry:

```
- 2026-08-07 — player-arc-close — ADR-0068
```

**Oracle bracket** (`bin/regression-oracle f9e4afc <this session's own
tip>`): this session touched no `src/`, no `test/`, no `deps.edn`, no
`workspace.edn`, no Makefile — docs/plans/rulings/state only. All
eleven vendored-root batches expected and confirmed byte-identical; see
Verification, below.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`,
  every staged scan, every push).
- Post-push message verification, every checkpoint: one delta each
  against the message file, the known harmless trailing-newline
  artifact prior sessions already name.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (227 namespaces, 0/0) and again after Step 2's own
  edits (227 namespaces, 0/0, identical shape) — fresh-clone-green
  throughout, the first player-arc session that gets to make that claim
  without a disclosed pre-existing failure.
- `bin/regression-oracle f9e4afc f9e4afc` (Step 0): all eleven
  vendored-root batches IDENTICAL, soundness "yes outside ns form."
  Re-run after this record's own closing commit expected and confirmed
  unchanged, since nothing after the Step 2 commit touches `src/` or
  `test/`.
- Founding-incident live re-probe (Step 0, this session): all six
  failures confirmed mechanically impossible against the BUILT
  `bin/ehrt`/`clojure -M:cli`, not only `clojure.test` — full
  transcript in `.agents/state.md`'s own regenerated "Live work"
  section.
- Tag verification: `stable-20260807-player-board` peeled ref resolves
  to `f9e4afc` exactly.

### Fences

Docs-only: no `src/`, `test/`, `deps.edn`, `workspace.edn`, or Makefile
touched; no gate changes (every gate cited this session was read, not
edited). No new design work: the horizon note above RESTATES ruled
directions, it does not extend them; the intake list NAMES unruled
candidates, it decides nothing — in particular the vendoring arc's own
internal sequencing (substance qualifier first) is the design channel's
recorded recommendation, not yet a session plan. Frozen archives
untouched except the sanctioned acts: this ADR's own new file, and the
live-attic appends to `.agents/plans/roadmap-done-2026-08.md` (AR-PC-5,
the same act ADR-0046's own compaction-B pattern licensed, and every
prior arc close since has exercised for its own predecessor's pointer).

### Consequence

The player arc — the fold (ADR-0066, total over the emitter's real
trigger set, self-anchored in absolute epoch millis, the coherence
property as its own spec) and the board (ADR-0067, the whiteboard
itself, the corpus→sim-emit-hl7 edge, the fresh-clone-green rider) — is
complete. `.agents/state.md` regenerates with fourteen corrected or
newly-probed claims, every one backed by a probe run this session,
including a phrasing correction owed since the UX epilogue (AR-EP-5)
and a payload-shape correction the ceremonial fixture's own retirement
made necessary. The `:onboarding` and `:corpus` reading-set budgets
re-derive to reflect the final tree (1205→1180, a decrease; 1995→2040).
The arc's own two Done pointers rotate to a new attic header, its own
predecessor's disclosed leftover (ADR-0064) and the UX epilogue's own
pointer (ADR-0065) both join the UX-arc section they actually belong
to, and the live roadmap's Done section holds only this ADR's own
pointer. Two more mechanisms now guard the founding incident's own
surface — six in total — and, for the first time since ADR-0060
landed, the full test suite is green on a genuinely fresh clone: a
stranger cloning this repository today, cold, gets a green baseline
without inheriting the author's own scratch files. A stranger running
`ehrt play PATH --board N` against a paced HL7 v2 stream — their own,
or this project's — can watch a hospital breathe: beds fill, patients
transfer, discharge, and merge, without reading a line of this
project's own source. The next arc opens with four named, unruled
intake items (fold leniency for absent PID fields, the sim event-log
input adapter, `ehrt.corpus.display` placement, the `:mllp` sink) and
one piece of mechanical debt (the `stable-20260807-player-close` tag),
both recorded here rather than left to be rediscovered.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Player arc close: the hospital is watchable, the suite is honest, the state regenerates — six founding-incident mechanisms now guard the CLI, fresh-clone-green for the first time since ADR-0060, the state and its budgets re-derive
