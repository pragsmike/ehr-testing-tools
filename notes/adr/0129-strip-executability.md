## ADR-0129 — Strip executability: exercisers, citation gate, ADR-0127 erratum

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

Chartered from a fresh public clone at HEAD `56613c7` (ADR-0128's own
close; all four commits CI-green, verified by author `gh` list +
channel API). Closes manual-review dimension 1 (strip executability,
FAIL — `.agents/plans/2026-08-13-manual-review-1.md`), the manual
arc's own front-of-queue finding: three "neither" rows (Chapter 6's
`README.md` "What you get" section; Chapter 7's two
`docs/use-cases/*.md` citations; Chapter 8's `acceptance-qa`/
`regression-baselining` citations) that neither `bin/demo-exerciser-
ed-tuesday` nor `bin/quickstart-demo` re-run. R3's own charter
(`notes/ADRs.md` ADR-0113, author verbatim): *"The demos must be known
to work, and exercised as documented to make sure they actually play
out as written."*

### Step 0 — Ceremony and tag payment

`bin/preflight`: last five CI runs on `main` all green (`56613c7`,
`dba20a9`, `fda0b70`, `2297599`, `a884967`); edit-root confirmed ext4;
tree clean; local HEAD matched `origin/main` at `56613c7`; last
`stable-*` tag `stable-20260813-ceremony-scripts`, HEAD not yet
tagged.

Tag `stable-20260813-hardening` created ANNOTATED at
`56613c75c35bd1de5e9a66fb57edd84848196a6b` via `bin/tag-ceremony ...
--push`, licensed by this session's own driving prompt citing the
design channel's fresh-clone verification. Full receipts pasted into
the session-record draft before Step 1 began (Step-0 receipts
practice, ADR-0128):

```
OK: created annotated tag 'stable-20260813-hardening' at 56613c75c35bd1de5e9a66fb57edd84848196a6b
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260813-hardening -> stable-20260813-hardening
OK: pushed refs/tags/stable-20260813-hardening
OK: remote peeled ref for 'stable-20260813-hardening' is 56613c75c35bd1de5e9a66fb57edd84848196a6b, matches target exactly
```

Oracle pre-digest basis: all 35 roots; predicted end-state pure
identity — `docs-tooling` is not a pipeline root, and no pipeline
`src` is in this session's own fence. Confirmed at close (below).

### Step 1 — ADR-0127 erratum, commit `3c9333d`

Append-only dated erratum to `notes/adr/0127-*.md`, matching
`notes/adr/0121-*.md`'s own erratum form: Step 3's own `:sim`
`1170/1295, "none needing a bump"` figure was arithmetically wrong
when recorded — the five `:sim` paths at that session's own closing
commit (`21114e3`) already summed to 1293, 2 lines of headroom, not
1170; the 123-line undercount happened not to trip the gate at the
time (1293 still cleared 1295) and went uncorrected until ADR-0128's
own +5-line tripwire edit pushed the real total to 1298, surfacing the
original error. Budget re-derived to 1495 by ADR-0128 (not
re-derived again here — unchanged). Register-line marker added to
`notes/ADRs.md`'s own ADR-0127 entry, matching the ADR-0121 line's own
inline-parenthetical convention.

**Process note.** The session-record draft (created early per the
Step-0-receipts practice) tripped `prompt-record-pairing-test` before
its own paired prompt archive existed — fixed by self-archiving the
driving prompt immediately and running `bin/close-scaffold` to add
both directories' own README index lines, landed as a small
administrative commit (`185018c`) ahead of this checkpoint's own real
commit.

### Step 2 — Extraction extension + exercised-sources register, commit `47a1ab8`

Two new extraction shapes in a new namespace,
`ehrt.docs-tooling.strip-fresh`, alongside a new registry namespace,
`ehrt.docs-tooling.exercised-sources` (schema + loader, `ehrt.judge.
pairing`'s own load-registry shape) and its committed EDN resource
(`components/docs-tooling/resources/docs-tooling/
exercised-sources.edn`, nested under `docs-tooling/` per
`resource-nesting-test`'s own per-brick convention).
`ehrt.docs-tooling.quickstart-fresh` and `ehrt.docs-tooling.demo-
exerciser-fresh` are UNTOUCHED — zero edits, verified by an empty
`git diff` against those four files across the whole session's span —
`strip-fresh` delegates to their own `check` fns verbatim for the two
pre-existing register rows, and duplicates (rather than extracts a
shared helper from) their small private unwrap/marker-extraction logic
for its own two new rows, so neither existing namespace's own tested
contract becomes answerable to an outside caller.

- `:single-fence` — the first fence of a given language, comment/blank
  stripped, everything else kept verbatim — quickstart-fresh's own
  algorithm, generalized past its own hardcoded ```sh/README.md pair.
- `:paired` — every fence of a given language immediately followed
  (blank-lines-only gap, no prose) by a DIFFERENT-language fence
  yields a (command-lines, output-lines) pair; unpaired blocks still
  contribute command-lines with nil output-lines. `check-entry`'s own
  :paired branch filters to genuinely-paired blocks for its flattened
  command list — verified live: `command-output-pairs` over README.md
  returns THREE ```bash blocks (busy-tuesday's own "See it run" fence,
  correctly unpaired since prose follows it; the two "What you get"
  pairs), filtering to paired-only correctly drops busy-tuesday with
  zero section-heading logic needed.

Extraction verified against the five real, live sources before any
script existed (scratch, pasted into the session record): 9/3/6/4
command lines for the four use-case pages; 3 bash blocks total for
README.md, 2 paired, 6 command lines across the 2 paired.

Red witnessed, scratch, before commit: the two pre-existing rows
delegate green (`:ok? true`, 15/21 lines); the five new rows are RED
(`:script-absent`, script-count 0) since their scripts land next
commit. Per this session's own disclosed discretion, the five new
freshness TEST CASES are NOT committed in this red state (a failing
test would break the "make test green before every push" fence) —
they co-land with the scripts in commit 3 instead.

Committed test coverage: extraction unit tests on synthetic fixtures;
seeded-divergence tests for both new `check-entry` branches plus the
absent-script case; two live smoke tests proving delegation to
quickstart-fresh/demo-exerciser-fresh; a live extraction-count test
against all five real new sources; registry-loader tests.

### Step 3 — Five exercisers + integration wiring, commit `076d5b1`

`bin/usecase-judge-tier-calibration`, `bin/usecase-profile-tier-v2`,
`bin/usecase-acceptance-qa`, `bin/usecase-regression-baselining`,
`bin/readme-what-you-get` — house style (`expect`/`expect_eval`
wrapper, BEGIN/END markers matching the register's own marker strings
exactly, commands verbatim from source). Wired into `Makefile`'s
`integration:` target, five new lines after the existing
`bin/demo-exerciser-ed-tuesday` line. Exec bits via `git update-index
--chmod=+x`, verified `100755` before commit.

Real exit codes dry-run live before writing any script, so `expect`
assertions state the true, witnessed code: judge-tier-calibration
(0, 0, 0, 1 — the `blank-required-field` mutant genuinely rejected,
matching the use case's own `{:pass 1 ...}`/`{:rejected 1 ...}` claim
byte-for-byte); profile-tier-v2 (exit 3, `:no-verdict`, matching
"comes back :no-verdict/:profile-spec-error"); acceptance-qa (0, 0,
0); regression-baselining (0, 0, "both runs exit 0"); readme-what-
you-get (0; 0, 1, matching the fence's own `:status :rejected`).

**A real finding: README.md's own "What you get" ```clojure fences are
hand-formatted, elided EXCERPTS of the real CLI output, not verbatim
captures.** The real `gate fhir` output is single-line and carries
`:engine`/`:native-ref` keys the fence never shows (elided with a
literal trailing `...`). The driving prompt's own "normalize only what
quickstart-demo already normalizes" had an empty base to inherit —
`quickstart-demo` asserts only exit codes, zero output-text comparison
anywhere. Resolved with a new, disclosed design:
`ehrt.docs-tooling.strip-fresh/subset-match?` — every value the fence
states must be present and equal in the real captured output; extra
real fields always allowed; vectors must match length and
element-wise. `parse-elided-edn` strips the fence's own `...` markers
before `edn/read-string`. `paired-output-check!` (`-X`-invokable) is
`bin/readme-what-you-get`'s own runtime call, per pair, against real
captured stdout (teed to a per-step log file). Unit-tested on
synthetic fixtures plus one test asserting the real live README fence
against this session's own real captured output.

Five new freshness test cases co-landed with the scripts (Step 2's own
commit could not carry them green): each asserts `check-entry` against
the real registry row is `ok? true` with matching counts (9, 3, 6, 4,
6) — all five pass, closing Step 2's own witnessed red.

Executed each script end-to-end, in-session, real artifacts, once per
script, before committing. All five: every real command/exit-code
invariant held; `readme-what-you-get`'s own paired-output check
reported `OK` on both pairs. Every run's own tree-clean postcondition
FAILED at that point — a disclosed false positive (ADR-0120's own
Commit 1 precedent): this session's own in-progress, not-yet-committed
files were still in the working tree mid-development. The genuine
clean-tree `make integration` run is recorded below, after commit 4
(the tree stayed dirty with Step 4's own in-progress work through the
first clean-tree attempt too — a second attempt, after commit 4,
finally ran with nothing outstanding).

### Step 4 — Citation gate, commit `35bad55`

`ehrt.docs-tooling.citation-gate`: extracts every "Strip source
citations" table row across `docs/manual/0*.md` (a four-state machine
— `:before`/`:seeking-header`/`:seeking-separator`/`:in-rows` — over
marker/header/separator/data lines), restricted to rows whose own
source cell names a citable doc path (`README.md`, a
`demos/scenarios/*/README.md`, or a `docs/use-cases/*.md` page —
verified against every real citation table before choosing this
scope; several real rows cite a fixture path, a config file, or
nothing at all, correctly out of scope).

**A real state-machine bug, caught live before any test existed.** The
first draft collapsed "marker seen" and "header seen" into one boolean
and reset on the blank line between the marker and the table — a
scratch run against the real manual returned ZERO rows from five
chapters with real tables. Rewritten as the explicit four-state
machine; re-run found the correct 14 rows.
`manual-citations-finds-the-expected-count-across-all-five-chapters-
test` exists specifically because of this near-miss.

**A real precision gap, found and fixed before landing.** README.md
is the one citable source with TWO unrelated register rows
(Quickstart, What you get) — a coverage check keyed on :source alone
would silently miss exactly the Chapter-6 gap dimension 1 found. Fixed
by adding an optional `:section` field to the register schema
(README.md's own two rows only) and a `covered?` rule: a :source with
exactly one register row is covered by :source alone; a :source with
more than one row requires a case-insensitive substring match between
the citation's own section label and some candidate row's own
:section. Proven on synthetic fixtures (`covered-distinguishes-two-
different-sections-of-the-same-source-test`).

Red witnessed, scratch, against a simulated pre-session register (the
two pre-existing rows only): 4 of the 5 real dimension-1 gaps
surfaced (both `docs/use-cases/*.md` citations in Chapter 7, both in
Chapter 8). Disclosed limitation: the 5th (Chapter 6) does not surface
in this particular reconstruction, since a single-row source is
covered by :source alone regardless of section — correct for every
OTHER source today, but it means this specific simulation cannot
mechanically reproduce dimension 1's own human judgment about
Chapter 6. The synthetic two-row test is what proves the
disambiguation mechanism itself works; the live-tree test
(`committed-manual-is-fully-covered-test`) proves the real, final
register resolves all 14 real citations, Chapter 6 included.

`clojure -M:poly check` caught a real gate on the first attempt:
`io_vocabulary_lint_test` forbids a bare `.listFiles` call outside
`ehrt.kernel.io`'s own allowlist (ADR-0078, AR-RL-4). Fixed by routing
through `ehrt.kernel.interface/list-files` (result-or-loud) — no new
project wiring needed, `docs-tooling` and `kernel` already share the
`conformance`/`ehrt-cli`/`integration` projects.

### `make integration`, licensed once after commit 3, once after commit 4

First attempt (after commit 3): every real invariant held through
`bin/demo-exerciser-ed-tuesday`'s own named checks; the run's own
FINAL step, the tree-clean postcondition, failed on Step 4's own
in-progress files — the ADR-0120-precedented false positive. Second
attempt (after commit 4, session-record checkpoint committed first):
also failed the same way, on a session-record edit made while the run
was still executing. Third attempt, against a genuinely untouched
tree: **GREEN** — all five new scripts plus `bin/demo-exerciser-
ed-tuesday` ran end-to-end, real artifacts, every named invariant
held, every run's own tree-clean postcondition passed, including both
of `readme-what-you-get`'s own paired-output checks (`OK: pair 0`,
`OK: pair 1`).

### Step 5 — Re-score, records, close

Targeted manual-review re-run, dimension 1 only: **PASS** — full
account in `.agents/plans/2026-08-13-manual-review-1.md`'s own new
"Dimension 1 re-run" section, file:line evidence per chapter. Dim-4's
own targeted-re-run precedent (ADR-0126) followed for form; this
session's own explicit fence additionally licensed appending the
re-score directly to the review-run file itself, done in the same
commit.

### Oracle

`bin/regression-oracle 56613c7 <this session's own closing commit>`:
see the session record's own close-out section for the real run and
its output — predicted pure identity (zero `src`/`test` touched
outside `components/docs-tooling/`, which is not an oracle root; the
five new `bin/` scripts write only to gitignored `out/`).

### Fences honored

Zero edits to `docs/manual/` chapter prose, `use-cases.edn`,
`README.md`, `demos/`, any other `bin/` script, `.github/`, or any
skill. `test-fixtures/` untouched — `acceptance-qa` bound to the
already-committed `test-fixtures/v2`, no new fixture needed. `notes/
adr/0127-*.md` append-only (diffed before commit: existing text
unchanged). `Makefile` touched only inside the `integration:` target's
own line range.

### Disposition

Manual-review dimension 1 (strip executability): CLOSED — PASS. The
manual arc (S1-S5, `.agents/plans/roadmap.md`) is now in its first
all-dimensions-addressed state: dimension 4 (ADR-0126) and dimension 1
(here) both closed, dimensions 2/3/6/7/8 passed at the original run
and never regressed. Citation gate: a standing mechanism, not a
one-time fix — a future chapter citing an un-registered source now
fails `make test`, not just a future manual-review run.
