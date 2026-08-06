## ADR-0054 — Alignment fixes 5: the license text travels with the content — F-4 closes, gated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-05.

### Context

Prior: alignment fixes 4 landed and was design-channel-verified
(`05d6ed1`, `notes/adr/0053-alignment-fixes-4.md`). Register rows
F-2/F-3/F-4 (`.agents/plans/2026-08-05-alignment-audit-findings.md`)
found that of this repo's four Apache-2.0-sourced vendored roots —
Synthea GMF modules, CDC NIST validator fixtures, Google SimHospital
fixtures, and Synthea-derived `wellness-cadence.edn` — only the
SimHospital root (`components/corpus/test-fixtures/v2/simhospital/`)
vendored the actual Apache-2.0 license TEXT; the other three relied on
NOTICE narrative alone, citing "Apache License 2.0" without including
the license text itself. Apache-2.0 §4(a) expects a copy of the
license to travel with redistributed content — this repo's own root
`LICENSE` is its own MIT grant and does not substitute for a third
party's Apache-2.0 grant. Pre-tag was judged the cheap moment to fix
this, before formats freeze harder.

The author's ruling: option (a), ONE shared license-text file,
cross-referenced from the three NOTICEs (rather than option (b),
per-root `LICENSE` copies matching the SimHospital pattern) — less
duplicative.

Two precision gaps surfaced during Step 0 preflight, against the live
tree rather than the register's own summary of it, and were put to the
author before any code was written (both resolved by direct question,
recorded here verbatim as this session's own rulings, extending
AR-F5-2/AR-F5-3):

- **`components/sim-trajectory/resources/sim-trajectory/NOTICE`**
  (named target 3, for `wellness-cadence.edn`) contains **zero**
  occurrences of the word "Apache" anywhere in its live text — it
  documents the Synthea extraction (ADR-0037 AR-1) but never states a
  governing license. A content-test gate keyed on "cites Apache"
  cannot flag this file pre-fix, so it cannot participate in a genuine
  red trip the way the other two targets can.
- **`components/sim-model/resources/sim-model/demographics/NOTICE`**
  (register row F-2, explicitly ruled OUT of scope — hand-curated, no
  Apache obligation) contains the literal string "Apache-2.0" as
  background context ("Synthea (Apache-2.0, MITRE) as the mined
  source"), while explicitly disclaiming that this directory's content
  is derived from Synthea. A naive substring gate would false-positive
  on this file forever.

**AR-F5-2a (sim-trajectory append, ruled).** Full paragraph, disclosed
asymmetry: the append both asserts `wellness-cadence.edn` is
Apache-2.0-licensed (per ADR-0037 AR-1) AND points to
`LICENSES/Apache-2.0.txt` — self-consistent and gate-compliant going
forward. This file's pre-fix red trip is legitimately absent (the
register found it via extraction-fact reasoning, not text-search) —
disclosed here as a real, honest asymmetry: 3 live red trips (the
NOTICEs that already cited Apache) + 1 proactive fix (this file),
never uniform-by-construction.

**AR-F5-3a (gate predicate, ruled).** Declarative-pattern match, not a
plain substring search: the gate's `cites-apache?` predicate triggers
only on lines that assert governance — a `License:`/`License |` line
naming Apache, or an `under Apache`/`under the Apache` phrase —
excluding demographics/NOTICE's mention-only sentence structurally, by
content, never by a path allowlist.

### Decision

Ruled 2026-08-05, recorded verbatim (author rulings, this session's
own prompt), plus AR-F5-2a/AR-F5-3a above:

**AR-F5-0 (tag).** Annotated tag `stable-20260805-alignment-fixes-4`
at `05d6ed1`, message `alignment fixes 4 landed, design-channel-verified
2026-08-05 (ADR-0053)`; push; verify on origin.

**AR-F5-1 (the shared text).** `LICENSES/Apache-2.0.txt` at repo root
(REUSE-style layout). Content: byte-copy of
`v2/simhospital/LICENSE`, after verifying that file is the unmodified
canonical Apache-2.0 text. Root `LICENSE` (MIT) not edited; README's
license section may gain a one-line pointer.

**AR-F5-2 (three cross-refs, dated).** Each target NOTICE gains a
short dated line: the complete Apache License 2.0 text is vendored at
`LICENSES/Apache-2.0.txt`, added 2026-08-05 per ADR-0054 (register
F-4). NOTICE narrative otherwise untouched — appends only.

**AR-F5-3 (the gate, co-landed).** New deftest in the `docs-tooling`
gate family: (a) `LICENSES/Apache-2.0.txt` exists and is byte-identical
to `v2/simhospital/LICENSE`; (b) every NOTICE file in the tree whose
text cites the Apache license also contains the string
`LICENSES/Apache-2.0.txt`. Exclusion semantics by content-test, not
path allowlist. Red→green witnessed before the cross-refs land.

**AR-F5-4 (determinism).** The oracle bracket (`05d6ed1` → tip) must
show all eleven batches identical — no `.json`/`.edn`/`.csv` content
touched anywhere.

### Step 0 — preflight + tag

Working directory confirmed `~/src/ehr-testing-tools` (the same ext4
clone `pwd`/`git rev-parse --show-toplevel` both resolve to); tip
`05d6ed1` exactly; working tree clean.

**Canonicality verification (AR-F5-1).**
`components/corpus/test-fixtures/v2/simhospital/LICENSE` read in
full: standard Apache-2.0 preamble, §§1–9 verbatim, unfilled appendix
(`Copyright [yyyy] [name of copyright owner]` — the boilerplate
brackets, never filled in with a source-specific copyright line). No
STOP-AND-REPORT trigger — this is the canonical, unmodified upstream
text, the clean source AR-F5-1 requires.

**`components/sim/NOTICE` inspection (disclose-either-way clause).**
This file's own "narrower NOTICE files" list states, for
`resources/modules/NOTICE`: "vendored verbatim under Apache-2.0" — and
separately, discussing SNOMED CT codes riding the vendored Synthea
module: "ride that file's own Apache-2.0 distribution from Synthea...
Synthea's own license... is what covers their presence here." Both
assert Apache-2.0 governs content this file itself describes, without
a text pointer. **Disclosed: `components/sim/NOTICE` is a fourth
target**, alongside the three the prompt named — treated identically
(terse dated cross-ref append) in Step 1.

**Full suite baseline.** `clojure -M:poly test :all skip:integration`:
216 `Test results:` lines (a slight increase from ADR-0053's own 214
is Step 1's own new test namespace counted across two project builds,
not a baseline drift), 0 `FAIL`/`ERROR` anywhere, 511 assertions in the
sim-trajectory family (spot-checked, identical to ADR-0053's own
tip-of-session count — nothing between the two sessions touched test
code before this session's own Step 1).

**Oracle pre-digest** (`bin/regression-oracle 05d6ed1 05d6ed1`, a
self-comparison confirming the harness before this session's own
changes land): all eleven roots (`appendicitis`, `death-fixture`,
`ear-infections`, `ear-infections-engine`,
`ear-infections-history-engine`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`,
`urinary-tract-infections-history-engine`) IDENTICAL; soundness check
"yes outside ns form".

**Tag act, verified on origin:**

```
$ git tag -a stable-20260805-alignment-fixes-4 05d6ed1 \
    -m "alignment fixes 4 landed, design-channel-verified 2026-08-05 (ADR-0053)"
$ git push origin stable-20260805-alignment-fixes-4

$ git rev-parse stable-20260805-alignment-fixes-4^{commit}
05d6ed15cf29009542ec1b0771e1e0605123af3d
$ git ls-remote --tags origin "stable-20260805-alignment-fixes-4^{}"
05d6ed15cf29009542ec1b0771e1e0605123af3d  refs/tags/stable-20260805-alignment-fixes-4^{}
```

The peeled ref resolves to `05d6ed1` exactly, both locally and on
origin; `git tag -l -n1` confirms the message matches the ruling
verbatim. (Tag creation/push is AUTHOR ACTION per the build-session
skill — ADR-0003's own trust boundary — executed by the author, not
the session, per that skill's standing rule.)

### Step 1 — red, then text + cross-refs + gate (AR-F5-1/2/3, AR-F5-2a/3a)

New gate:
`components/docs-tooling/test/ehrt/docs_tooling/license_text_pointer_test.clj`.
`cites-apache?` is a per-line regex,
`#"(?i)license\s*[:|]\s*.*apache|under (?:the )?apache"` — matches a
`License:`/`License |` line naming Apache or an `under Apache`/`under
the Apache` phrase; does not match a bare mention. `notice-files`
walks the repo tree (`target/`, `.git/` pruned) for files named exactly
`NOTICE` or `NOTICE.md`. A `mechanism-sanity-test` proves both
directions against literal excerpts of the real files: a `License:`
line is caught, a `License |` table row is caught, an `under
Apache-2.0` phrase is caught, the demographics NOTICE's own
mention-only sentence is NOT caught, and a NOTICE mentioning no Apache
at all is NOT caught — the same "prove the predicate actually catches
what it claims to" shape `resource-nesting-test`/`skill-mirror-currency-test`
already establish for their own drift classes.

**Red, against the unmodified tree** (captured before any of AR-F5-1/2
landed):

```
FAIL in (every-apache-citing-notice-points-at-the-shared-license-text-test)
./components/corpus/test-fixtures/v2-nist/NOTICE.md cites the Apache license
as governing its own content but does not point at LICENSES/Apache-2.0.txt

FAIL in (every-apache-citing-notice-points-at-the-shared-license-text-test)
./components/sim/NOTICE cites the Apache license as governing its own
content but does not point at LICENSES/Apache-2.0.txt

FAIL in (every-apache-citing-notice-points-at-the-shared-license-text-test)
./components/sim/resources/sim/modules/NOTICE cites the Apache license
as governing its own content but does not point at LICENSES/Apache-2.0.txt

FAIL in (license-text-file-exists-and-matches-canonical-source-test)
LICENSES/Apache-2.0.txt is missing -- ADR-0054 AR-F5-1 vendors the
Apache-2.0 text there, byte-copied from
components/corpus/test-fixtures/v2/simhospital/LICENSE
```

Four failures, genuine: the three NOTICEs that already cite Apache
without a pointer, plus the missing shared text file. **Exactly as
AR-F5-2a's own disclosure predicted:** `components/sim-model/resources/
sim-model/demographics/NOTICE` and `components/sim-trajectory/resources/
sim-trajectory/NOTICE` did NOT trip — the former correctly excluded by
the declarative-pattern predicate (register F-2, no Apache obligation),
the latter because it cites no Apache text at all pre-fix (the register
found its own obligation via extraction-fact reasoning, not
text-search; its fix below is proactive, not gate-caught).

**Green, after the fix lands.**
`LICENSES/Apache-2.0.txt` created, byte-copied (`diff` confirmed
zero-diff) from `v2/simhospital/LICENSE`. Four NOTICE appends:

- `components/sim/resources/sim/modules/NOTICE` — terse dated line.
- `components/corpus/test-fixtures/v2-nist/NOTICE.md` — terse dated
  line, under a new `## License text` heading (the file's own
  table-and-heading convention).
- `components/sim/NOTICE` — terse dated line (the disclosed fourth
  target).
- `components/sim-trajectory/resources/sim-trajectory/NOTICE` — full
  paragraph per AR-F5-2a: states the Apache-2.0 governance explicitly
  (citing ADR-0037 AR-1 and the sibling modules NOTICE for the same
  license terms), then the dated pointer.

`README.md`'s License section gains one line: "Third-party license
texts live in [`LICENSES/`](LICENSES/)."

Gate re-run: green, 3 `deftest`s / 13 assertions, 0 failures. Full
suite: 216 `Test results:` lines (same count as Step 0's own baseline
— the new test namespace was already present at baseline since the
gate was written before the fix, per the red→green discipline), 0
`FAIL`/`ERROR` anywhere, 511 assertions in the sim-trajectory family
(unchanged from Step 0). `clojure -M:poly check`: OK. `gitleaks git
--staged -v`: clean (18.15 KB scanned, no leaks). Staged set matched
exactly the seven files this checkpoint touches (`git status`
reviewed before `git add`).

Committed `0d36820` ("docs: the license text travels with the content
— three NOTICEs point home, gated (alignment fixes 5,
AR-F5-1/2/3)"), pushed. Post-push verification: one delta against the
message file, the known harmless trailing-newline artifact.

### Register F-2/F-3/F-4 closure narrative

- **F-2 (demographics/wellness-cadence split) — closed.**
  `components/sim-model/resources/sim-model/demographics/NOTICE` is
  confirmed hand-curated, no Apache obligation, and confirmed
  structurally excluded from the new gate by its declarative-pattern
  predicate (not merely by omission) — `mechanism-sanity-test` proves
  this directly against the file's own live sentence.
  `components/sim-trajectory/resources/sim-trajectory/NOTICE`'s own
  `wellness-cadence.edn` obligation is now stated explicitly in its own
  text (AR-F5-2a) rather than left implicit against ADR-0037 AR-1
  alone.
- **F-3 (v2/simhospital vs v2-nist LICENSE-text inconsistency) —
  closed.** Rather than vendoring a second per-root `LICENSE` copy
  into `v2-nist/` (option (b), matching the SimHospital pattern), the
  author's option (a) makes `v2/simhospital/LICENSE` the canonical
  source for a single shared `LICENSES/Apache-2.0.txt`, which
  `v2-nist/NOTICE.md` now points at. The inconsistency closes by
  making the shared file authoritative, not by duplicating the
  pattern a second time.
- **F-4 (cross-cutting license-text gap) — closed.** All four
  Apache-2.0-citing NOTICEs in the tree (`sim/resources/sim/modules/NOTICE`,
  `corpus/test-fixtures/v2-nist/NOTICE.md`, `sim/NOTICE`,
  `sim-trajectory/resources/sim-trajectory/NOTICE`) now point at
  `LICENSES/Apache-2.0.txt`, and the gate proves this structurally: a
  future vendored root that writes an Apache-citing NOTICE without the
  pointer trips the build, the same shape the resource-nesting and
  skill-mirror-currency gates already established for their own drift
  classes.

### Step 2 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its index line. Roadmap gets its
Done pointer:

```
- 2026-08-05 — alignment-fixes-5 — ADR-0054
```

**Oracle bracket** (`bin/regression-oracle 05d6ed1 0d36820`): all
eleven vendored-root batches byte-identical
(`appendicitis`, `death-fixture`, `ear-infections`,
`ear-infections-engine`, `ear-infections-history-engine`, `sepsis`,
`sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
`urinary-tract-infections-engine`,
`urinary-tract-infections-history-engine`); soundness check "yes
outside ns form"; `IDENTICAL: every root's digest matches between
05d6ed1 and 0d36820`. Expected: this session touched no `.json`/
`.edn`/`.csv` content anywhere — one new script-free test namespace,
one new license-text file, four NOTICE appends, one README line.

**Deftest/assertion parity:** 195 `deftest`s / 511 assertions in the
sim-trajectory family, unchanged before and after Step 1 — the new
gate (3 `deftest`s / 13 assertions) is additive, in `docs-tooling`, not
a substitution anywhere in the tested-code assertion count.

### Verification

- `clojure -M:poly check`: OK, both checkpoints this session.
- `gitleaks`: clean at every scan this session (staged scan pre-commit,
  the push-time scan).
- Post-push message verification: one delta against the message file,
  the known harmless trailing-newline artifact prior sessions already
  name.
- Red→green witnessed for the new gate, both directions, against the
  live tree (not a synthetic fixture) — the four genuine red failures
  above, then a clean 13-assertion green after the fix landed.
- `bin/regression-oracle 05d6ed1 0d36820`: all eleven batches
  identical (Step 2, above) — the AR-F5-4 determinism claim this ADR
  makes names this script's own output, not an assertion-count
  comparison.

### Pending arc-close register append

Per AR-C-2's own contract (`.agents/rulings.md`'s header), the queue
carried from ADR-0053 is unchanged by this session — no new
standing-rulings-style item surfaced here beyond the two precision
rulings (AR-F5-2a/AR-F5-3a) already folded into this ADR's own
Decision section, not deferred to the queue:

- **A-3, dependency review cadence** (ADR-0050 AR-F1-6).
- **D-3, pairing-as-data registry landing spot** (ADR-0050 AR-F1-6).
- **The law-surface propagation lesson, now two instances** (ADR-0051
  AR-F2-0; ADR-0053 AR-F4-4).
- **S1/C-1 closed** (ADR-0052, carried forward unchanged).

Not appended this session — this is the note, not the append. This IS
the arc's last fix session (F-4 register row closes here); the arc
close session follows next, per the prompt's own after-landing note.

### Consequence

Every Apache-2.0-sourced vendored root in this repo now carries, or
points at, the actual license text — not narrative alone — closing the
§4(a) gap register row F-4 flagged. The gate makes this structural: a
future vendored root that cites Apache without pointing at
`LICENSES/Apache-2.0.txt` fails the build, the same "next drift is
caught structurally, not by another audit" shape this repo's other
content-based gates already establish. Two precision gaps the register
summary didn't carry — sim-trajectory's silent-on-license text,
demographics' mention-only false-positive risk — were caught and ruled
before code was written, not discovered after a wrong gate shipped.
Session 6 (arc close) follows: the three pending rulings-register
appends, `state.md` regeneration per AR-C-1, the first `libs
:outdated` report under the new A-3 cadence, reading-set budget
re-derivation per AR-D-3 if owed, and the arc's final tags.
