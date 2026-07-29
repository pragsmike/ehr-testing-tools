# 2026-07-28 — Discipline parity

## Scope

Restored the workspace's discipline apparatus to parity with the union
of sim's and tools' own final conventions (R24), instantiated live
infrastructure neither parent's carve had brought forward (R25),
promoted five workspace-native lessons into standing doctrine (R26),
filled ADR-0003's reserved slot (R27), relocated root `test/`/
`test-integration/` fixtures to brick-owned directories (R28), and
tidied two top-level accidents (`doc/`/`docs/` split, a provenance-path
stutter, R29). Superseded the never-executed
`2026-07-28-ehr-testing-h2-closeout-sweep` prompt, folding its intended
items into this session's own step 6.

## Red→green evidence highlights

No engine/business logic changed — this was a docs/discipline/tooling
session, plus one real fixture relocation. Verification instead: `poly
check` green four times across the session (after the interface/docsgen
change, after the sweep commit, after the fixture relocation, after the
tidy commit); `poly test :all skip:integration` green twice, full run,
0 failures / 0 errors both times (once after restoring docsgen +
exporting `write-cli-md!` through `ehrt.tools.interface`, once after the
fixture move — the second run is the load-bearing one, since it's the
empirical proof that cwd-relative fixture paths resolve correctly across
`components/tools`, `bases/ehr-cli`, and `projects/conformance`'s
separate test classpaths, not merely assumed); `make ci-parity` green
once, against the committed fixture-relocation state specifically
(fresh clone, cold artifact cache).

## Judgment calls and their ratification status

Recorded in full in `notes/ADRs.md` ADR-0006's own deviation record and
this session's own archived prompt
(`notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`); not
repeated here beyond a pointer. Not yet author-ratified beyond the two
live rulings already made in-chat this session (commit delegation for
the session; no UNDECIDED disposition rows reached the author, since
the audit produced zero). The largest judgment call: R28's own
instruction to convert fixture reads to `io/resource` was not followed
literally — cwd-relative literal paths were kept, re-affirmed after a
wider call-site survey than the prompt's own estimate found real CLI
path-handling tests that would break under a resource conversion
regardless of brick placement. ADR-0002 gained a dated erratum
correcting a mischaracterization of what it had claimed (not a
mischaracterization it made).

## Findings and HEAD landed

Two operational gaps found and fixed that weren't named in the prompt:
`agent/scenario-roster.md` missing from the live tree despite ADR-0005's
own skills union (skill definitions moved live, their one operational
dependency didn't); `.gitattributes` missing tools' own pre-carve
`-text` overrides for the v2 HL7 fixtures (corpus bytes checked against
recorded sha256 and found intact, not corrupted, but genuinely
unprotected). One smaller finding: a broken cross-workspace ADR citation
in `PROVENANCE.md` (pointed at this workspace's own ADR file while
citing tools' pre-merge ADR-0011). One self-caught process finding: the
R26 doctrine section landed inside the step-3 guide-union commit instead
of its own step-5 commit — noticed only while drafting the deviation
record, not caught in real time by the very staging-hygiene ritual
written into that same commit; recorded rather than silently
corrected-after-the-fact. HEAD at session close: `610a296` (before this
record's own commit).
