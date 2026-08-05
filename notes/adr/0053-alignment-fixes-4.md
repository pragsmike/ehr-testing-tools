## ADR-0053 — Alignment fixes 4: offline determinism without redistribution — the NIST mirror lives user-side, the lockfile grows teeth

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: alignment fixes 3 landed and was design-channel-verified
(`57ba010`, `notes/adr/0052-alignment-fixes-3.md`). Register row A-4
(`.agents/plans/2026-08-05-alignment-audit-findings.md`) found the
workspace still `hit-nexus-live`: `deps.edn` line 5 declares
`:mvn/repos {"nist-hit" {:url "https://hit-nexus.nist.gov/repository/releases/"}}`,
a live network Nexus with no stated SLA that just changed operators
(NIST → Prometheus Computing, Aug 2026 transition) — the supply-chain
risk the register flagged. `components/judge-v2-nist/deps.edn`'s own
comment block, and its source
(`components/corpus/docs/research/judge-v2-nist-spike-notes.md`, item
4 of "Wiring into the workspace"), already prescribed a fix: mirror the
six resolved jars into an in-repo `file://` repo, the same pattern
`gov.cdc:lib-hl7v2-nist-validator` uses.

That prescription is FORECLOSED, not merely stale. `notes/tools/ADRs.md`
ADR-0005's 2026-07-24 amendment drew a line the original wiring notes
predate: artifacts divide into those this repo **redistributes**
(vendored into what it ships — full license verification required, no
exceptions) and those **fetched by users from an official upstream
source at their own initiative** (this repo's lockfile records
provenance and hash; resolution happens on the user's own machine
against the artifact's official source; this repo redistributes
nothing). The six NIST-origin coordinates are `license-status
:use-permitted--unstated--confirmation-pending` — permitted for the
second category only. Checking their jars into this repo's own git
tree, even as a `file://`-served mirror, would be exactly the
redistribution the amendment forbids. `artifacts.lock.edn`'s own
comment already named this in passing ("the file://-mirror end-state
ADR-0012 named as a future risk is still open, unaffected by this
ruling") — this session is where that tension is resolved rather than
merely disclosed.

The author's ruling: option (a), a **user-side mirror** plus
**mechanized lockfile verification** — the offline-determinism goal
the original wiring notes wanted, achieved without redistribution. The
NIST licensing inquiry (`docs/experiments/EXP-SBOM-inquiry-draft.md`,
ADR-0008) stays open; a favorable reply may relax this later, but
nothing here depends on its answer.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's own
prompt):

**AR-F4-0 (tag).** Per the standing mechanic: annotated tag
`stable-20260805-alignment-fixes-3` at `57ba010`, message `alignment
fixes 3 landed, design-channel-verified 2026-08-05 (ADR-0052)`; push;
verify on origin.

**AR-F4-1 (the mirror, user-side).** New `bin/mirror-nist` (bash, house
style) + `make mirror-nist`: builds `~/.ehrt/nist-mirror/` in Maven
repository layout from the user's own `~/.m2/repository` cache,
covering every hit-nexus-sourced artifact listed in
`artifacts.lock.edn` (enumerated from the lockfile itself, not
hardcoded — six coordinates, more than the three bare `gov.nist`
group-id entries); jar AND pom per coordinate, since resolution needs
both. Each jar's sha256 is verified against its lockfile entry at copy
time — a mismatch aborts the mirror build with the offending
coordinate named. A coordinate absent from `~/.m2` produces a distinct
"not yet resolved" exit, not a failure. `--m2`/`--dest` overrides for
testability; defaults are the real paths.

**AR-F4-2 (activation is the operator's, documented not performed).**
The mirror is activated via a Maven `<mirror>` entry (`mirrorOf` →
`nist-hit`, URL `file://$HOME/.ehrt/nist-mirror`) in the user's own
`~/.m2/settings.xml`. The session writes the exact XML into
documentation (AR-F4-5) and does not apply it.

**AR-F4-3 (the lockfile grows teeth).** New `bin/verify-nist-lock` +
`make verify-nist-lock`: reads `artifacts.lock.edn`, locates each
listed hit-nexus-sourced artifact in `~/.m2` (or `--repo` override),
compares sha256s. Three distinct exits: all-match (0), any-mismatch
(nonzero, coordinates named — the alarm case), any-absent (distinct
nonzero, "not yet resolved"). Wired into the Makefile target where jar
resolution actually already happens, determined by inspection.
Red→green witnessed against a scratch fixture (never `~/.m2`); then
against the real cache.

**AR-F4-4 (surfaces agree with the amendment — law-surface lesson,
instance two).** Fix-forward, dated, citing this ADR + ADR-0005's
amendment: (a) `components/judge-v2-nist/deps.edn`'s comment block —
the vendoring prescription replaced by the user-side-mirror posture.
(b) Grep the tree for any other surface prescribing in-repo vendoring
of these jars — each hit gets the same fix-forward or, if
historical/frozen, is left with a register-style note here. (c)
`artifacts.lock.edn` may gain pom sha256 entries as a dated extension
if cheap — optional, disclosed either way.

**AR-F4-5 (documentation).** A short ops doc —
`components/judge-v2-nist/docs/nist-mirror.md` (a new `docs/` subdir
for this component; disclosed, since the component had none before):
why the mirror exists, `make mirror-nist` usage, the exact
`settings.xml` activation XML, `make verify-nist-lock` semantics and
its three exits, and what changes if the NIST licensing External
resolves favorably.

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (ext4, `df -T`
reports `ext4`); tip `57ba010` exactly; no `stable-20260805-alignment-
fixes-3` tag existed yet, locally or on origin. All six NIST
coordinates were already present in `~/.m2/repository` from prior
sessions' own resolution (`gov/nist/{hl7-v2-parser,hl7-v2-profile,
hl7-v2-validation,hit/hl7-v2-schemas}`, `gov/nist/xml-util`,
`com/github/hl7-tools/validation-report`) — no fresh resolution was
needed to satisfy this session's own tooling. No `~/.m2/settings.xml`
existed (confirmed both before and after this session — AR-F4-2's
fence held throughout).

Baseline: `clojure -M:poly check`: OK. Full suite (`clojure -M:poly
test :all skip:integration`): 214 `Test results:` lines, 0
`FAIL`/`ERROR`/`Exception` anywhere (identical shape to ADR-0052's own
tip, as expected — nothing between the two sessions touched test
code). `gitleaks detect -v`: 659 commits scanned, no leaks found.

**Tag act, verified on origin:**

```
$ git tag -a stable-20260805-alignment-fixes-3 57ba010 \
    -m "alignment fixes 3 landed, design-channel-verified 2026-08-05 (ADR-0052)"
$ git push origin stable-20260805-alignment-fixes-3
 * [new tag]         stable-20260805-alignment-fixes-3 -> stable-20260805-alignment-fixes-3

$ git ls-remote --tags origin | grep alignment-fixes-3
de666b151bb1b3c07d796fcfe4680b5fb94d0721	refs/tags/stable-20260805-alignment-fixes-3
57ba010afd643a30aa39a0c41d75e6d3108a6d83	refs/tags/stable-20260805-alignment-fixes-3^{}
```

The peeled ref resolves to `57ba010` exactly; `git tag -l -n99` confirms
the message matches the ruling verbatim.

### Step 1 — tooling (AR-F4-1/3)

`bin/verify-nist-lock` and `bin/mirror-nist` both enumerate the six
hit-nexus-sourced lockfile rows directly from `artifacts.lock.edn`
(filtering on `(str/includes? source "hit-nexus.nist.gov")`, never a
hardcoded coordinate list), deriving each one's Maven-repository-relative
path from its own `:source` URL (everything after `.../releases/`) —
no group/artifact/version parsing needed, and the derivation is
therefore correct for any future hit-nexus-sourced row added to the
lockfile, not just today's six.

**Red, scratch fixture (never `~/.m2`):**

```
$ echo "deliberately wrong bytes, not the real jar" > \
    $SCRATCH/gov/nist/hl7-v2-parser/1.7.3/hl7-v2-parser-1.7.3.jar
$ bin/verify-nist-lock --repo $SCRATCH
MISMATCH -- artifacts.lock.edn disagrees with $SCRATCH:
  nist-hl7-v2-parser: expected 62fac887d19842b26ddde2b9916db39dd7a2cdcdafeeeb1cac6a0e52c0d17999, got 99fb1249f2f4cfb4f0f4efb4625d611c997ec1a778b528e5ea03a78bf702c5f2 (.../hl7-v2-parser-1.7.3.jar)
$ echo $?
1
```

`bin/mirror-nist --m2 $SCRATCH --dest $SCRATCH_DEST` against the same
fixture: identical mismatch message, exit 1, and `$SCRATCH_DEST` was
never created — nothing copied on a mismatch, as ruled.

A second scratch case (empty repo dir) proved the third, distinct exit:
`bin/verify-nist-lock --repo $SCRATCH_EMPTY` → `not yet resolved -- run
a full build first` naming all six coordinates, exit 2 — never
confused with the mismatch exit.

**Green, real cache:**

```
$ bin/verify-nist-lock
OK: 6 hit-nexus-sourced coordinate(s) match artifacts.lock.edn exactly
  nist-hl7-v2-parser
  nist-hl7-v2-profile
  nist-hl7-v2-validation
  nist-xml-util
  nist-hl7-v2-schemas
  nist-validation-report
$ echo $?
0
```

**One proving run of `make mirror-nist` against the real cache** — the
licensed `~/.ehrt` side effect, disclosed:

```
$ bin/mirror-nist
mirror built at /home/mg/.ehrt/nist-mirror: 6 coordinate(s) verified and copied
  nist-hl7-v2-parser
  nist-hl7-v2-profile
  nist-hl7-v2-validation
  nist-xml-util
  nist-hl7-v2-schemas
  nist-validation-report
```

`find ~/.ehrt/nist-mirror -type f` confirmed all 12 files (jar + pom ×
6) landed at their correct Maven-layout relative paths. No other path
outside the repo was touched; `~/.m2` (including the absence of
`settings.xml`) was read, never written.

**Makefile wiring (AR-F4-3).** Jar resolution for the NIST coordinates
happens whenever `projects/conformance`'s own classpath is resolved —
its `deps.edn` declares both the `nist-hit` `:mvn/repos` entry and
`poly/judge-v2-nist` (confirmed by direct read; matches facts register
F9's own account of the three project-level `nist-hit` entries). That
resolution is exactly what `make test`'s own `clojure -M:poly test :all
skip:integration` step performs — the per-push lane. `bin/verify-nist-
lock` is wired as `test`'s third line, run unconditionally after the
suite: on a normal run its six coordinates are already resolved by that
point, so `verify-nist-lock` should report `OK` every time; a sha256
drift there is a real, actionable finding and now fails the per-push
lane instead of passing silently. `ci-parity`'s own inline replica of
the two `poly` commands (a separate, deliberately independent
fresh-clone check) was left unmodified — out of this ruling's own
scope, which named one target by inspection, not every place the two
`poly` commands are echoed.

Verified: `make verify-nist-lock` (OK, same six coordinates); `make
mirror-nist` (delegates to `bin/mirror-nist`, confirmed via `make help`
listing both new targets); full `make test` run green end-to-end,
`bin/verify-nist-lock`'s own `OK` line printed as the lane's last step.

Committed `a659cbf` ("feat: offline determinism without redistribution
— the NIST mirror lives user-side, the lockfile grows teeth (alignment
fixes 4, AR-F4-1/3)"), pushed. Post-push verification: one delta
against the message file, the known harmless trailing-newline artifact.

### Step 2 — surfaces (AR-F4-4/5)

**(a) `components/judge-v2-nist/deps.edn`, comment-only.** The
"Determinism note" paragraph replaced: the old text ("vendoring the six
resolved jars into a file:// repo mirror is the safe end-state")
retired in favor of the user-side-mirror posture, citing this ADR and
ADR-0005's amendment by name, and pointing at
`components/judge-v2-nist/docs/nist-mirror.md`. `git diff` confirmed:
only comment lines changed; `:deps`, `:mvn/repos`'s own guidance
comment above it, `:aliases`, and every coordinate are byte-identical
to before.

**(b) Tree-wide sweep.** Search terms per the ruling: `file:// repo`,
`vendor`, `CDC does exactly this`, `safe end-state`.

| Hit | Disposition |
| --- | --- |
| `components/judge-v2-nist/deps.edn` (the prescription itself) | Fixed forward, comment-only — see (a) above. |
| `components/corpus/docs/research/judge-v2-nist-spike-notes.md:73` ("Wiring into the workspace" item 4: "mirroring the resolved jars into a `file://` repo (CDC's own pattern) is the safe end-state") | This IS the design-channel wiring-notes document's own in-repo landed copy — "Archived verbatim from a Cowork cloud session," frozen at authoring time (2026-07-30, six days AFTER ADR-0005's own 2026-07-24 amendment; the spike session simply never accounted for it). Per this session's own fence ("annotate it dated rather than editing its body"): a dated annotation block was added directly below the file's existing archival header, above the original title, pointing at this ADR and explaining the foreclosure — the original body (including item 4's own prose) is untouched, byte-for-byte. |
| `.agents/prompts/2026-08-05-alignment-audit.md:17` (an already-self-archived session prompt, describing the audit's own probe A: "grep ... for whether the NIST jar mirroring end-state (file:// repo + engine-jar sha256s) landed or remains hit-nexus-live") | Historical — a dated, already-executed session's own prompt archive, accurately describing what that session checked at the time. Not a live prescription; no action, per AR-F1-2's judgment rule (same class as this arc's own E-4/E-7 dispositions). |
| `notes/tools/ADRs.md` ADR-0005's own original "Consequence" clause ("EXP-D3's NIST-artifact mirroring becomes `ehr artifact fetch` against a local mirror source") | Frozen ADR record, already carrying its own inline 2026-07-24 dated amendment immediately below that correctly narrates the real, resolved posture (redistribution vs. user-initiated fetch) — the earlier aspirational clause is superseded in place by the record's own later paragraph, not left as an active prescription. No edit — this is the ADR discipline working as designed, not a gap. |

No other hits for any of the four search terms, across `*.md`, `*.edn`,
and `*.clj`.

**(c) `artifacts.lock.edn` pom sha256 extension: declined this
session, disclosed.** `bin/mirror-nist` already copies each
coordinate's pom alongside its jar, but does not independently verify
pom bytes — Maven's own resolver validates POM structure at use time,
and the jar sha256 (already lockfile-tracked) is the artifact this
repo's supply-chain concern is actually about. Widening the lockfile
schema to carry a second hash per artifact was judged out of this
session's own scope; left as a candidate for a future session if a
concrete need for pom-level verification surfaces.

**(d) Ops doc (AR-F4-5).** `components/judge-v2-nist/docs/nist-
mirror.md` written: why the mirror exists (no-SLA host, operator
change, ADR-0005's posture), `make mirror-nist` usage, the exact
`<mirror>` activation XML for the user's own `~/.m2/settings.xml`
(never applied by this session), `make verify-nist-lock`'s three exits,
and what a favorable NIST licensing answer would (and would not)
change.

`clojure -M:poly check`: OK. `git status --porcelain`: exactly the
three files this checkpoint touches (`deps.edn`, the spike-notes
annotation, the new ops doc) — nothing else staged.

Committed `d43c143` ("docs: the vendoring prescription retires —
surfaces agree with ADR-0005's amendment (alignment fixes 4,
AR-F4-4/5)"), pushed. Post-push verification: one delta against the
message file, the known harmless trailing-newline artifact.

### Verification

- **A genuine finding, caught by an existing gate, fixed forward.**
  `ehrt.cli.executable-bits-test` (the same "executable-bit incident"
  class ADR-0004 first named) failed at this step's own suite run:
  `bin/mirror-nist` and `bin/verify-nist-lock` were `chmod +x`'d on
  disk before Step 1's commit, but this clone's own
  `core.fileMode=false` masked the fact that the git INDEX still
  recorded both as `100644` (non-executable) — a fresh clone (what CI
  checks out) would have seen them as non-executable scripts. Fixed
  forward, this step: `git update-index --chmod=+x bin/mirror-nist
  bin/verify-nist-lock`; full suite re-run green (214 `Test results:`
  lines, 0 failures/errors) immediately after. Not amended into the
  already-pushed Step 1 commit — the fix lands in this step's own
  commit instead, disclosed here rather than silently folded in.
- `bin/regression-oracle 57ba010 d43c143`: **all eleven vendored-root
  batches byte-identical** (`appendicitis`, `death-fixture`,
  `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`); soundness check "yes
  outside ns form"; `IDENTICAL: every root's digest matches between
  57ba010 and d43c143`. Expected: this session touched no `src/` at
  all (two new `bin/` scripts, a `Makefile` target, one comment block,
  one dated annotation, one new doc) — every downstream byte was
  always going to be identical. No `--declared-digest-change` licensed
  or needed.
- Assertion-count delta vs. Step 0: none — this session added zero
  `deftest`s (no gate, no test code; `bin/` scripts are shell, not
  Clojure test namespaces). Full suite stayed at 214 `Test results:`
  lines, 511 assertions in the sim-trajectory family alone (spot-checked
  identical to Step 0's own count), 0 failures/errors, at every
  checkpoint this session ran it.
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks`: clean at every scan this session (baseline `detect`, both
  staged scans, both pushes).
- Post-push message verification, both checkpoints: one delta each
  against the message file, the known harmless trailing-newline
  artifact prior sessions already name.
- AR-F4-2's environment fence held throughout: `~/.m2/settings.xml` did
  not exist before this session and does not exist after it (checked
  both times); the only path this session ever wrote outside the repo
  tree is `~/.ehrt/nist-mirror/`, exactly the licensed proving-run side
  effect AR-F4-1 names.

### Pending arc-close register append

Per AR-C-2's own contract (`.agents/rulings.md`'s header), the queue
now stands at three standing-rulings-style entries (joining ADR-0050
AR-F1-6's own two) plus one register-row-closure note carried from
ADR-0052:

- **A-3, dependency review cadence** (ADR-0050 AR-F1-6): `clojure -M:poly
  libs :outdated` is report-only, run at each arc close plus mandatory
  before any publish; upgrades are never taken as a side effect.
- **D-3, pairing-as-data registry landing spot** (ADR-0050 AR-F1-6):
  `judge` is the accepted acyclic home; the design pass starts from
  there.
- **The law-surface propagation lesson, now two instances** (ADR-0051
  AR-F2-0; this ADR's own AR-F4-4): an amendment to standing law must
  land on every surface that states the law, in the same session that
  rules it. Instance one: `AGENTS.md`'s tag rule lagged ADR-0049's
  AR-AU-0 by three sessions before ADR-0051 closed the gap. Instance
  two: `components/judge-v2-nist/deps.edn`'s own comment block (and its
  source, the archived spike-notes document) kept prescribing in-repo
  NIST-jar vendoring for eleven days after ADR-0005's 2026-07-24
  amendment made that prescription unlawful, until this session closed
  it. Two independent instances of the same standing-process defect —
  worth naming as a pattern, not just fixing twice.
- **S1/C-1 closed** (ADR-0052, carried forward unchanged — not this
  session's own work; noted here only so the append queue stays
  complete when the arc-close session reads this file last): `sim-
  model`'s resource-nesting drift is fixed and gated; no register row
  remains open in that cluster.

Not appended this session — this is the note, not the append.

### Consequence

The workspace no longer needs a live network Nexus at build time to
resolve deterministically: any user who has already fetched the six
NIST jars once can build `~/.ehrt/nist-mirror/` from their own `~/.m2`
cache and, by adding one `<mirror>` entry to their own
`~/.m2/settings.xml`, resolve entirely offline against it — without
this repo ever redistributing a NIST-origin byte. `artifacts.lock.edn`'s
supply-chain claims are no longer narrative-only: `make verify-nist-
lock` is now part of the per-push lane, and a sha256 drift between the
lockfile and `~/.m2` fails the build instead of passing silently. The
in-repo prescription that would have violated ADR-0005's own amendment
is retired everywhere it was found, with the one frozen exception
(the archived spike-notes document) left annotated rather than rewritten,
per this repo's own frozen-archive discipline. This session touched
register row A-4 directly (closed: the supply-chain risk it flagged now
has a mechanized, license-respecting answer) and surfaced a second
instance of the law-surface-propagation pattern ADR-0051 first named.
Session 5 (LICENSE/NOTICE hygiene, the arc's last fix cluster, register
row F-4) follows next, then arc close.
