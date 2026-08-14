## ADR-0132 — Scenario rename (busy-tuesday -> clinic-decade) + exerciser completion

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-14.

### Context

Chartered from `.agents/plans/roadmap.md`'s own "Scenario rename +
busy-tuesday exerciser completion" row (ADR-0130, UNBLOCKED 2026-08-14
once ADR-0131 fixed `events.edn` read-back for this scenario's own
module mix -- the blocker this row was sequenced behind). Resumes the
exerciser work ADR-0130's own session drafted (a working `bin/demo-
exerciser-busy-tuesday`, never committed, full text preserved verbatim
in that ADR's own Appendix) and completes the row that ADR-0130 closed
partial-with-open-rows.

**Author ruling, verbatim, 2026-08-13** (`.agents/rulings.md`, "From
ADR-0131"), naming the scenario's own future name, left open by
ADR-0130: *"clinic-decade it is."* The scenario is renamed busy-tuesday
-> clinic-decade. Frozen records (`notes/adr/` bodies, session
records, prompt archives, register history lines) KEEP the old name;
this ADR carries the mapping.

### Step 0 -- Ceremony and tag

`bin/preflight`: last five CI runs on `main` all green (`c27bdd3d`,
`25e595c4`, `e1a9b9a5`, `e3813a53`, `ef15885a`); edit-root ext4; tree
clean; local HEAD matched `origin/main` at
`c27bdd3dad529fc66e4a41d7ac32910c9541ea25`; last `stable-*` tag
`stable-20260813-busy-tuesday-deferral`, HEAD not yet tagged. Tag paid:
`bin/tag-ceremony stable-20260814-slug-fix c27bdd3d... --push` --
created ANNOTATED at `c27bdd3d`, pushed, peeled ref verified exact
match.

**Oracle pre-digest** (`bin/regression-oracle c27bdd3d c27bdd3d`, the
same-ref sanity check this repo's own precedent uses at session
start): IDENTICAL, all 35 roots. **Verified, not assumed, that no
digested artifact embeds the scenario path string**: `grep -rn
"busy.tuesday\|busy_tuesday" components/oracle/` returned zero hits,
and none of the 35 oracle roots' own module closures resolve through
`demos/scenarios/` at all (they compile directly from
`components/sim/resources/sim/modules/`) -- the oracle roots and the
demo scenario are structurally disjoint, so the predicted end-state
(pure identity; the rename touches no engine behavior) holds by
construction, not merely by absence of a positive hit.

### Step 1 -- Rename sweep (commit `214b0ec`)

`git mv demos/scenarios/busy-tuesday demos/scenarios/clinic-decade`.
Full live-reference sweep, re-derived against the live tree rather
than assumed from the driving prompt's own pre-probe (which named 22
live-reference files at `ef15885`, an earlier commit -- ADR-0131's own
docs-only commits could have added or removed sites since):

| Class | Files touched |
|---|---|
| Scenario's own dir | `demos/scenarios/clinic-decade/{README.md,config.edn}` (moved + edited: title, path strings, the `:pathway :name` field, the quoted "busy Tuesday"/"clinic-decade" self-reference) |
| Cross-ref READMEs | `demos/README.md`, `demos/scenarios/README.md`, `demos/scenarios/ed-tuesday/README.md` (5 sites), root `README.md` ("See it run" section) |
| Sibling config comments | `demos/scenarios/ed-tuesday/config.edn` (3 comment sites) |
| Emitted CLI text | `bases/cli/src/ehrt/cli/help.clj` (`play` group's own sourced `:example`, B2/ADR-0118's sourcing rule -- the example's own source, README's "See it run" fence, changed, so the sourced copy follows) |
| Doc generation source | `components/corpus/docs/use-cases.edn` (one citation), regenerated companion `docs/use-cases/play-a-generated-corpus-back-over-time.md` in the same commit (`make use-cases`) |
| Docs-tooling comments | `components/docs-tooling/resources/docs-tooling/exercised-sources.edn` (header comment), `components/docs-tooling/src/ehrt/docs_tooling/{demo_exerciser_fresh,strip_fresh}.clj` (docstrings), `bin/readme-what-you-get` (comment) |
| Test marker fixtures | `components/docs-tooling/test/ehrt/docs_tooling/{demo_exerciser_fresh_test,strip_fresh_test}.clj` (fixture strings only -- the tests still prove parameterization against a non-default, non-ed-tuesday marker pair, now spelled `clinic-decade` instead of `busy-tuesday`), `components/docs-tooling/test/ehrt/docs_tooling/citation_gate_test.clj` (one explanatory comment) |
| Roadmap | `.agents/plans/roadmap.md` (19 live mentions across the redesign-arc paragraph, the deferred-exerciser rows, and this session's own row's surface text -- see Fences below for the frozen/live boundary this sweep drew) |

**docsgen regen, same commit**: `make use-cases` and `make cli-doc`
both run; `docs/use-cases/play-a-generated-corpus-back-over-time.md`
changed (one link), `docs/cli.md` unchanged (it renders no `:example`
field at all -- confirmed by direct inspection, not assumed).

**Frozen/live boundary, disclosed.** The driving prompt's own frozen
list -- "notes/adr/ bodies, session records, prompt archives, register
history lines" -- resolves against this repo's live tree as four
concrete classes, applied mechanically:

1. `notes/adr/*.md` (every ADR body, including `0130-*.md`/`0131-*.md`
   themselves) -- untouched.
2. `.agents/session-records/*.md` -- untouched.
3. `.agents/prompts/*.md` -- untouched.
4. `notes/ADRs.md` (the ADR register/index) and `.agents/rulings.md`
   (the ruling register, organized entirely as dated "From ADR-NNNN"
   history entries) -- their EXISTING lines untouched; this ADR adds a
   new line/section to each, which is append, not edit.

Two more classes, not named in the driving prompt's own four-item
list but judged the same append-only-history shape by this session,
disclosed as a judgment call rather than silently assumed: `.agents/
state.md` (a CITATION-ONLY append log, each entry explicitly marked
"CONTENT NOT RE-PROBED" -- editing a past entry would misrepresent
what that entry captured at the time) and `.agents/plans/
2026-08-09-repo-review-findings.md` (a dated, closed findings register
from a since-superseded review, the same class as a session record).
`.agents/session-records/README.md`/`.agents/prompts/README.md` (the
star-bullet indexes) are pointers to frozen filenames whose own
description text mirrors the frozen file's own title -- left untouched
so the index and the file it points at keep saying the same thing.

**Residue check, re-derived after every edit, not assumed clean from
the plan alone**: `grep -rln "busy.tuesday\|busy_tuesday\|busy
Tuesday" .` outside the classes above returned ZERO after this
commit -- the only surviving hit anywhere in a live file is this
session's own roadmap.md mapping sentence ("busy-tuesday ->
clinic-decade"), which states the mapping by name, not a residual.

**`make test` green** (`clojure -M:poly check` OK; `clojure -M:poly
test :all skip:integration`: 632 "0 failures, 0 errors" blocks,
matching ADR-0131's own pre-session count exactly -- no test moved;
`bin/verify-nist-lock` OK).

### Step 2 -- Exerciser completion (commit `20770dc`)

`bin/demo-exerciser-clinic-decade` adapted from ADR-0130's own drafted
Appendix script (recovered verbatim from that ADR's own record, per
the driving prompt's own instruction to treat it as "the worked
starting point, not a design redo"), names throughout renamed to
clinic-decade. One real, disclosed fix to the drafted script itself,
found live rather than assumed correct: the Appendix's own
`inpatients: 0` claim-extraction regex, `#"every board snapshot reads
\`([^\`]+)\`"`, does not match the README's own actual prose, because
the sentence wraps across a markdown line break ("...so every board" /
"snapshot reads \`inpatients: 0\`...") -- tested directly (`re-find`
against the live file returns `nil`), not merely suspected. Fixed by
widening the fixed-text portion of the pattern to `\s+` between words
(`#"every\s+board\s+snapshot\s+reads\s+\`([^\`]+)\`"`), tolerant of
any whitespace including a newline; re-tested, matches. This is a bug
in the never-executed drafted script, not a design change -- the
closing-summary regex (already `(?s)`-tolerant for its own multi-line
EDN span) needed no fix.

**Register row** (`components/docs-tooling/resources/docs-tooling/
exercised-sources.edn`), `:demo-exerciser-fresh` with explicit
`:marker-open`/`:marker-close` -- the ADR-0130-widened parameterization
now carries its own first second-instance consumer as data, not a code
change:

```clojure
{:source "demos/scenarios/clinic-decade/README.md"
 :script "bin/demo-exerciser-clinic-decade"
 :extraction :demo-exerciser-fresh
 :marker-open "# BEGIN clinic-decade commands (verbatim from demos/scenarios/clinic-decade/README.md)"
 :marker-close "# END clinic-decade commands"
 :env {}
 :witness {:adr "ADR-0132" :date "2026-08-14"}}
```

**Freshness case, red-before-green, witnessed directly** (the script
was written before this check, so red was captured by temporarily
moving it aside rather than by natural before/after sequencing --
disclosed, same net evidence):

```
RED (script absent): {..., :ok? false, :readme-count 5, :script-count 0,
  :divergence {:index 0, :readme "bin/ehrt corpus generate sim ...",
               :script :ehrt.docs-tooling.demo-exerciser-fresh/missing}}
GREEN (script restored): {..., :ok? true, :readme-count 5, :script-count 5,
  :divergence nil}
```

**Register count-lock bumped**, 7 -> 8
(`exercised_sources_test.clj`'s own `registry-loads-and-validates-test`),
with a new dedicated test (`registry-seeds-the-clinic-decade-row-test`)
asserting the row's own source/script/extraction/markers, and a new
live-delegation test in `strip_fresh_test.clj`
(`check-entry-delegates-live-to-clinic-decade-exerciser-test`) proving
`check-entry` genuinely reaches the real, committed non-ed-tuesday
marker pair, not only the synthetic fixtures ADR-0130 already covered.

**`Makefile` integration line added**, `bin/demo-exerciser-clinic-decade`
alongside `bin/demo-exerciser-ed-tuesday`; the `integration` target's
own help text updated to name both.

**Executed end-to-end, in-session, real artifacts** (seed 20260807, 200
patients, exec bit verified `100755` both before and after the
red-witness detour):

- Command 1 (generate): `{:status :ok, :payload {:out-dir
  "out/scenarios/clinic-decade"}}` (4 collision warnings from
  `sleep_apnea.json`, part of this scenario's own module mix -- expected,
  ADR-0131's own disclosed WARN-mode behavior, no run-blocking effect).
- Command 2 (`--board` play): closing summary `{:unparseable-count 0,
  :snapshot-count 48, :skip-count 41, :rate 100000.0, :idle-cap-ms
  5000, :stream-span-ms 279155640000, :clamped-count 0, :emitted 68,
  :unfolded-count 0, :sink "ticker"}` (`:wallclock-ms` excluded, run-
  volatile) -- BYTE-FOR-BYTE the same `68/48/41` figures ADR-0130 and
  ADR-0131 both witnessed; `inpatients: 0` on every one of the 48 board
  snapshots (the script's own runtime-derived assertion, matched).
- Command 3 (events.edn play): `{:status :ok, :payload {:unparseable-count
  0, :skip-count 49, :rate 100000.0, :emitted 367, :sink "ticker"}}` --
  the SAME `367`/`49` first-witnessed figures ADR-0131's own acceptance
  section recorded, reproduced exactly.

No figure moved; no README/figure edit. Full run wallclock: 504s (~8m24s),
the new lane's own first-witnessed timing, noted per the driving
prompt's own instruction ("note added lane wallclock").

**`make test` green** after both new-row commits (632 "0 failures, 0
errors" blocks, unchanged count -- the new tests land inside existing
namespaces, not new ones); `bin/verify-nist-lock` OK; `clojure -M:poly
check` OK; `gitleaks` clean at both commits.

### Step 3 -- Records and close (this commit)

**Roadmap**: the "Scenario rename + busy-tuesday exerciser completion"
row (renamed in place at Step 1, per the residue-check discipline) is
now marked CLOSED, citing this ADR. R3 (`notes/ADRs.md` ADR-0113) is
now fully discharged for every shipped scenario README: `README.md`'s
own Quickstart (`bin/quickstart-demo`), `demos/scenarios/ed-tuesday/
README.md` (`bin/demo-exerciser-ed-tuesday`), and `demos/scenarios/
clinic-decade/README.md` (`bin/demo-exerciser-clinic-decade`,
this session) are all register-exercised, integration-tier, asserting
both exit codes and every one of their own named invariants.

**Rulings**: `.agents/rulings.md` gains a new "From ADR-0132" section
recording the clinic-decade name ruling verbatim (already stated
above, restated there per this repo's own dated-ruling-register
convention).

**State**: `.agents/state.md` gains a new CITATION-ONLY append entry
pointing at this record, content not re-probed, per that file's own
standing convention.

**Tag verification**: `bin/close-scaffold --expect-tag
stable-20260814-slug-fix@c27bdd3dad529fc66e4a41d7ac32910c9541ea25`.

**Count-lock probe, `:onboarding` reading-set budget** -- routine, not
a STOP-worthy surprise (the same class every recent close has hit and
fixed inline: ADR-0107, ADR-0115, ADR-0125, ADR-0128). This session's
own close-phase edits (`.agents/plans/roadmap.md`'s own row-close text,
two new README index lines) pushed `:onboarding` to 2338 measured
lines against its own 2335-line budget, red by 3 -- accumulated churn
since the manual-arc-close re-derivation (ADR-0125): `roadmap.md`
781 -> 1284 lines across every session since, the two README indexes'
own five sessions' worth of new entries, and `build-session/SKILL.md`'s
own +6 lines (ADR-0130's checkpoint-commit sentence, landed after
ADR-0128's own 240-line measurement). Re-applying the standing formula
(actual x1.15, rounded up to the nearest 5): 2338 x 1.15 = 2688.7 ->
2690. Budget moves 2335 -> 2690 in `.agents/reading-sets.edn`, dated
comment recording the full per-path breakdown. No other set carries
`roadmap.md` or any other `:onboarding`-only path touched this
session. Re-run confirmed green: `reading-set-budget-test`, 5 tests,
15 assertions, 0 failures.

**Final verification**: `make test` green; `make integration` green
(real artifacts, all seven exercisers including the new
`bin/demo-exerciser-clinic-decade`); tree clean throughout every write
path (all under gitignored `/out/`).

**Oracle bracket**: `bin/regression-oracle c27bdd3d 20770dc7` (Step 0's
own baseline vs Step 2's own tip) -- **IDENTICAL, all 35 roots**,
matching Step 0's own verified prediction (pure identity; the rename
touches no engine behavior, and the oracle roots never resolve through
`demos/scenarios/`) exactly. No STOP condition anywhere this session.

### Fences

Committed Step 1 (`214b0ec`): `demos/scenarios/**` (the `git mv` +
README/config name edits), `bases/cli/src/ehrt/cli/help.clj` (name
string only) + regenerated `docs/use-cases/play-a-generated-corpus-
back-over-time.md`, `components/corpus/docs/use-cases.edn`,
`components/docs-tooling/resources/docs-tooling/exercised-sources.edn`
(comment only), `components/docs-tooling/src/ehrt/docs_tooling/
{demo_exerciser_fresh,strip_fresh}.clj` (docstring comments only),
`components/docs-tooling/test/ehrt/docs_tooling/{demo_exerciser_fresh_test,
strip_fresh_test,citation_gate_test}.clj` (fixture strings/comment
only), `bin/readme-what-you-get` (comment only), `.agents/plans/
roadmap.md`, root `README.md`, `demos/README.md`,
`demos/scenarios/README.md`. Zero `notes/adr/`, zero `.agents/session-
records/`, zero `.agents/prompts/`, zero `notes/ADRs.md`/`.agents/
rulings.md`/`.agents/state.md` line edited (all append-only, this
step).

Committed Step 2 (`20770dc`): `bin/demo-exerciser-clinic-decade` (new,
100755), `Makefile` (integration line + help text), `components/
docs-tooling/resources/docs-tooling/exercised-sources.edn` (new row),
`components/docs-tooling/test/ehrt/docs_tooling/{exercised_sources_test,
strip_fresh_test}.clj` (count-lock + new tests). Zero module JSONs,
zero engine/sim `src`, zero README/figure edits.

Committed this step: this ADR file, `notes/ADRs.md` index line,
`.agents/rulings.md` new section, `.agents/plans/roadmap.md` (CLOSED
note on this row), `.agents/state.md` new citation-only entry,
`.agents/session-records/2026-08-14-clinic-decade-rename-and-
exerciser.md`, `.agents/prompts/2026-08-14-clinic-decade-rename-and-
exerciser.md`, both directories' own README index lines, and
`.agents/reading-sets.edn` (the `:onboarding` budget re-derivation
above -- a count-lock companion, within the driving prompt's own
`.agents/ tree` fence). Zero
`src`/`test`/module-JSON/README-figure edit -- records-only, per this
step's own charter.

### Verification

`clojure -M:poly check`: OK, all three commits. `make test`: green at
every checkpoint (632 "0 failures, 0 errors" blocks throughout,
unchanged count both times). `make integration`: green this step, real
artifacts, all seven exercisers. `gitleaks git --staged -v`: clean at
every commit. Exec bit: `bin/demo-exerciser-clinic-decade` verified
`100755` via `git ls-files -s`, both before and after this step's own
`git log` shows `create mode 100755` at its own landing commit. Oracle:
`bin/regression-oracle c27bdd3d 20770dc7` IDENTICAL, all 35 roots,
matching Step 0's own verified pure-identity prediction exactly.
