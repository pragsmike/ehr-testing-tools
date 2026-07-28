# 2026-07-28 — CI red fix: executable bits, index-mode guard, AGENTS.md correction

Context

CI was red on `1ebf4ce` (run 30405350913): `bin/ehr` exited 126
("Permission denied") in the conformance loopback test
(`ehrt.tools.mutate-stdout-stdin-loopback-test`), cascading into a
`FileNotFoundException` on the never-written loopback catalog. Root
cause: `bin/ehr`, `bin/quickstart-demo`, and `bin/check-palgebra-drift`
were tracked as mode `100644` in the index. CI's fresh clone inherits
the index's mode; this workspace's own `core.fileMode=false` (NTFS/
DrvFs) makes a plain `git add` ignore filesystem permission changes
entirely, so the on-disk mode and the index mode had silently
diverged — local `poly test :all` runs stayed green throughout because
the working tree's own executable bit (however it got there) is what
local execution actually uses, never the index's.

This session's scope: repair the executable bits, add a permanent
guard test, and correct the one stale hook-doctrine claim named in the
prompt (`AGENTS.md`) plus a sweep of three more workspace-root docs. No
closeout-sweep steps were performed — that session
(`2026-07-28-ehr-testing-h2-closeout-sweep.md`) stopped at its own
step 0 precisely because of this red CI and resumes separately once
this fix lands and CI is green.

### Deviation record

**Probe stanza (step 0).**

```
git config --show-origin core.fileMode   -> file:.git/config  false
git config --global --show-origin core.fileMode -> (unset)
```

Full mode listing, before repair, of every affected file (`git ls-files -s`
scoped to `bin/`, `.githooks/`, and every other tracked file whose
content starts with `#!`, found by scanning the entire `git ls-files -s`
output byte-by-byte rather than trusting any single directory):

```
100644  .agents/skills/string-diagram/tools/resource_equations_to_mermaid.py
100644  .githooks/pre-commit
100644  .githooks/pre-push
100644  bin/check-palgebra-drift
100644  bin/ehr
100644  bin/quickstart-demo
100644  components/palgebra/tools/resource_equations_to_mermaid.py
100644  notes/tools/agents/skills/repo-adaptation/scripts/generate-migration-report.sh
100644  notes/tools/agents/skills/repo-adaptation/scripts/inspect-repo.sh
100644  notes/tools/agents/skills/repo-adaptation/scripts/scaffold-standard-layout.sh
100644  notes/tools/agents/skills/wsl-windows-git-hygiene/scripts/check-eol-noise.sh
```

Eleven hits, not three — CI only ever tripped on `bin/ehr` because
that's the one path the conformance suite's loopback test actually
subprocess-invokes; the other ten were equally broken and silent.
Fresh-clone reproduction (`git clone . /tmp/ehr-testing-repro`):
`bin/ehr` checked out `-rw-r--r--`; `./bin/ehr help` → `bash:
./bin/ehr: Permission denied`, exit 126 — matches the CI failure
signature exactly.

**Archaeology (step 1) — a prior convention exists; this session
conforms to it, does not invent a new one.**

`git log --all` surfaced `406482e` ("fix(bin): restore executable bit
on check-palgebra-drift", 2026-07-27) — the identical bug class, one
file, already fixed once. Its own prompt,
`notes/tools/prompts/2026-07-27-palgebra-drift-check.md`, names the
exact mechanism this session independently rediscovered: *"The
executable bit was lost between `chmod` and commit... this repo's
`core.fileMode` is `false`, so a plain `git add` ignores filesystem
permission changes entirely... Fixed with `git update-index
--chmod=+x` (which bypasses the `core.fileMode` ignore)."*
`notes/tools/agents/handoffs/handoff-2026-07-26.md` and
`notes/tools/agents/plans/archive/user-docs.md` both independently
corroborate: `bin/ehr`/`bin/quickstart-demo` are expected at `100755`
in the index specifically because of this same NTFS/`core.fileMode`
interaction. No CI-side or Makefile-side `chmod` workaround was found
anywhere (`.github/workflows/test.yml` has none; no Makefile survives
per ADR-0002's own disclosure; no script wraps `bin/ehr` in `bash
bin/ehr` to sidestep the +x requirement). Verdict: this is the third
occurrence of the same root cause, not a new failure mode — the fix
below conforms to the established `git update-index --chmod=+x`
remedy and adds the guard test that should have existed after the
first occurrence.

**Repair (step 2).** `git update-index --chmod=+x` plus a matching
working-tree `chmod +x`, applied to all eleven files from the survey
above (not just the three CI-known ones — the prompt's own instruction
that "the fix must cover every hit" is deliberate: a metadata-only
mode change doesn't touch file bytes, so it doesn't conflict with
ADR-0001 R8's byte-identical-provenance guarantee for the
`notes/tools/` and `notes/sim/` trees, which is about content, not
unix permission bits). Verified: `git ls-files -s` now shows `100755`
for all eleven.

**Verification method deviated from the prompt's literal wording, for
a stated reason.** Step 2 says to re-run the `/tmp` fresh-clone
reproduction and confirm it now executes `bin/ehr help` successfully.
`git clone` reads from committed refs, not the working index — and
per ADR-0001 R6, committing is the author's ceremony, not this
session's, so nothing was committed to re-clone. Substituted `git
checkout-index --prefix=/tmp/ehr-index-checkout/ -a`, which checks out
working files from the *current index* using the same mechanism a real
clone's checkout would use against a commit's tree — it is the
narrowest faithful stand-in for "what would ship if this were
committed" without actually running `git commit`. Result:
`.githooks/pre-push` and `bin/ehr` both `-rwxr-xr-x` in the checkout;
`./bin/ehr help` exit 0, full usage text printed. This is the same
claim the prompt asked to verify, checked by the mechanism available
to a session that doesn't commit.

**Guard test (step 3).** No established repo-hygiene test home was
found in archaeology, so placed per the prompt's own default:
`bases/ehr-cli/test/ehrt/ehr_cli/executable_bits_test.clj`
(`tracked-scripts-are-executable-in-the-index-test`). Shells out to
`git ls-files -s` (cwd = workspace root, confirmed by ADR-0002's own
prior finding that `poly test` always runs there), flags any tracked
file under `bin/`/`.githooks/`, or any tracked file starting with a
shebang, that isn't `100755`. Ran under `clojure -M:poly test :all`
and visibly executed: `Testing ehrt.ehr-cli.executable-bits-test` /
`Test results: 1 passes, 0 failures, 0 errors.`

**AGENTS.md correction, and the three-doc sweep (step 4).** `AGENTS.md`
(the one file the prompt named directly), `AUTHORS-GUIDE.md`, and
`CONTRIBUTING.md` all previously claimed `.githooks/pre-push` gates on
`clojure -M:poly test :project`; corrected to state the hook's actual
contents (WSL enforcement, gitleaks, `clojure -M:poly check` only),
each citing `notes/ADRs.md` ADR-0003 forward (not yet written — the
closeout-sweep session's job — per this prompt's own instruction not
to add doctrine prose here). `docs/way-of-working.md`'s one hit on
"pre-push" is historical prose describing *sim's own* pre-push hook
from a prior repo, in a deviation-record appendix — correct as
written, not touched.

**Named, disclosed, out of scope.** `.github/workflows/test.yml`'s own
comment on its `poly test :project` step ("Mirrors .githooks/pre-push's
own gate (AGENTS.md)") is now equally stale — pre-push no longer runs
`:project` — but the CI workflow file wasn't one of the four docs this
session's step 4 named, and editing CI config wasn't in this session's
commit plan. Left as a found-but-unfixed item for the closeout-sweep
session or a future one, rather than silently expanding this session's
scope or silently leaving it unmentioned.

**Verify (step 5).** `clojure -M:poly check` → `OK`. `clojure -M:poly
test :all` → every namespace green, 0 failures / 0 errors throughout,
16m40s wall time, guard test visibly executed and passing (above).
`poly test :project` was not separately re-run this session — the
prompt's own step 5 names `:all` plus the guard test's visible
execution as the bar; CI runs `:project` on its own as a distinct
workflow step and will exercise it against the real commit.

**Commits not made.** Per ADR-0001 R6, this session prepared the
working tree only — the eleven mode changes are staged, the new guard
test and the three doc corrections are unstaged/untracked, ready for
the author's four commits (C1–C4) below. `.claude/` remains untracked
and untouched (that's the closeout-sweep session's own step 3, not
this session's).
