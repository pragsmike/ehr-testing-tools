# 2026-08-16 -- D8-5 live fence battery: 202 fences enumerated from the tree, 58 bare fences run, riders C-4 and C-2

Chartered standalone by author ruling **Q2 a** ("Concur. Go.",
2026-08-15) against register row D8-5, never executed across two
consecutive reviews. Ceremony mode: **R30** (commit and push at each
checkpoint, unattended) -- the prompt did not declare prepare-only.

Product: [`.agents/plans/2026-08-16-fence-battery-findings.md`](../plans/2026-08-16-fence-battery-findings.md)
and its enumerator, [`bin/fence-census`](../../bin/fence-census).

## Step 0 -- preflight, and the STOP that fired first

`bin/preflight` plain, every finding disclosed:

- CI: last five runs on `main` all green.
- Edit root: `/home/mg/src/ehr-testing-tools`, not under `/mnt/`.
- Tree: clean, untracked included.
- HEAD `abb02398` matches `origin/main`.
- HEAD not tagged `stable-*` (disclosed by the script).

**The tag fence fired.** The prompt required verifying two standing
tags. `stable-20260815-result-nodes^{}` = `b139de58...` on the remote,
as predicted. **`stable-20260815-review-3-fixes` was ABSENT from the
remote entirely** -- present locally, annotated, at the right commit.

**The prompt's own premise did not hold.** It asserted the tag was
*"paid after ADR-0139 per the author's 'Pay it, message verbatim'
pattern."* It was not. ADR-0139 (`:573`) and the arc-close session
record both state the opposite in detail: that close's own tag license
was conditional on an author-side CI relay which never arrived, so the
tag was created **without `--push`** and the push **held** as declared
mechanical debt, with the paying command written down.

The session **STOPPED and reported** rather than paying it unilaterally
-- the prompt's named STOP, and `build-session` step 12
(fix-forward-with-disclosure on premise mismatch) pointing the same
way. Evidence gathered session-side and handed to the author in one
message: CI run **`31912592325`, conclusion `success`, `headSha
b96c246430038b4d38aa60a391de5e376e61cd24`** -- the tag target exactly;
the message file recoverable verbatim from
`.agents/prompts/2026-08-15-review-3-arc-close.md`; and
`bin/tag-ceremony:95-100` verifying rather than re-creating a tag
already at the exact commit and message.

**Author ruled: "Pay it, message verbatim -- then run the battery."**
The message was reconstructed from the archived prompt, checked pure
ASCII, and **diffed byte-identical against the existing local tag
object** before paying:

```
DISCLOSED: tag 'stable-20260815-review-3-fixes' already exists at b96c246... with the exact message -- verified, not re-created
OK: pushed refs/tags/stable-20260815-review-3-fixes
OK: remote peeled ref for 'stable-20260815-review-3-fixes' is b96c246430038b4d38aa60a391de5e376e61cd24, matches target exactly
TAG_EXIT=0
```

ADR-0139's mechanical debt is discharged. The substitution --
session-side preflight evidence in place of a relayed author-side check
-- is disclosed here beside it, the shape ADR-0134 used.

**Artifact cache primed:** all three pinned artifacts already resident
at `/home/mg/.cache/ehr-testing-tools/artifacts/` (`synthea 4.0.0`,
`temurin-jdk 21.0.12+8`, `fhir-validator-cli 6.9.12`), each returning
`{:status :ok ... :cached true}`.

**`:onboarding` budget re-derived FIRST**, per the prompt: 2658/2690,
**32 lines headroom**, matching ADR-0139's tripwire exactly.

## Step 1 -- population enumerated from the tree

`bin/fence-census` committed with the register so the population is
reproducible. 102 files, **202 fenced blocks, population closed**:
command/exercised **18**, command/bare **58**, output **29**, other
**97**.

Two method points, both found by running rather than assuming, both
moving the headline: D8-5's own command-head list is a floor, not a
closed set (39 bare fences if read literally, **58** widened); and
exercised-vs-bare is decided **per fence**, not per file (`README.md`
carries two registry rows and still holds a bare fence at `:27`).

## Step 2 -- the exercised set

`make quickstart` **MAKE_EXIT=0**, `make integration` **MAKE_EXIT=0**.
Both unpiped to full logs with the exit captured explicitly.

**Two red runs preceded the green, both this session's own artifacts,
both disclosed in the register:** the ADR-0005 tree-clean postcondition
tripping on the uncommitted enumerator, then
`tracked-scripts-are-executable-in-the-index-test` catching this
session's commit landing `bin/fence-census` as mode `100644`
(`core.fileMode=false` hid the `chmod +x`). Fixed with `git
update-index --chmod=+x` and an amend of the not-yet-pushed commit.

**The H-2 law paid off immediately:** the background runner's own exit
code was `0` for both red runs -- it reported the trailing `echo`'s
exit, not `make`'s. Only the explicit `MAKE_EXIT` capture showed red.

## Step 3 -- 58 bare fences

**GREEN 42, RED 7, YELLOW 5, SKIPPED-WITH-REASON 4 = 58**, re-derived
by count from the register's own rows.

Headline: **zero RED on `README.md`, `SETUP.md`, or the manual.** All
seven REDs are one root cause in one developer-facing file
(`docs/dev/migration/polylith-brief.md` teaches bare `poly`, not on
PATH here). The manual's two YELLOWs are both sequencing: chapter 4
plays a corpus its own chapter creates 45 lines later; chapter 8 needs
a Synthea corpus it never generates.

**A near-miss worth recording.** The first manual pass ran against an
`out/` populated by the exercisers, and returned **exit 1** on
`02-setup-first-corpus.md:97` -- the manual's most emphatic claim, that
two same-seed runs diff to nothing. Re-probed from a cleared `out/`,
the claim **holds** (`DIFF_EXIT=0`); the failure was a stale
`sim-s1-p1-first-run` directory. The whole manual batch was re-run
clean and every register verdict comes from that pass. This is the
battery's own instance of rule 9: an accumulated `out/` is a registry
standing in for a population. Filing that RED unverified would have
been the session's worst possible output.

## Step 4 -- rider C-4 (CLOSED)

Red witnessed **before** any rename, the run's own output naming both
offenders; the watch-list row named one. Enumerating by heading found
**two**: `0047-scaffolding-compaction-c.md` (heading ends "arc closes")
as well as `0125-manual-s5-chapter8-review-close.md`. Combined inbound
references **12**, tripping the prompt's own "more than a handful"
STOP; **reported and ruled by the author: rename both.**

Both fix options were taken rather than one: the population now reads
each ADR's own first heading, and a second assertion holds the filename
convention to what the headings declare. Green after; `clojure -M:poly
test brick:docs-tooling` **POLY_EXIT=0** confirming no gate keyed on the
old filenames. Landed `6b85227`.

The rename direction is worth stating: ADR-0139 recorded that renaming
a close ADR *outside* the gate's regex would have been the dishonest
path. This renames *into* the population, and pairs it with a gate that
catches the escape either way.

## Step 5 -- rider C-2 (already landed), register, close

**C-2 required no action.** The prompt directed adding a roadmap row
for the CarePlan/Guard standing request; **it already exists** at
`.agents/plans/roadmap.md:82`, landed by ADR-0139's own close. Verified
by grep before acting; recorded fix-forward rather than duplicated.

Roadmap: C-4's open row flipped to CLOSED with what the fix actually
found, and this session's Done pointer added. **`:onboarding`
re-derived after: 2657/2690, 33 lines headroom** -- closing C-4's row
freed more than the Done pointer consumed, so **no budget bump**.

## Deviations from the prompt, recorded

1. **The Step 0.1 STOP fired and was taken**, on a premise the prompt
   stated incorrectly. Resolved by author ruling, not by adaptation.
2. **C-2 was already landed** -- the prompt's Step 5.1 premise did not
   hold. No duplicate row created.
3. **C-4 was twice its stated size** (two files, 12 references), which
   tripped the prompt's own STOP. Escalated, ruled, then executed.
4. **A checkpoint commit landed the enumerator ahead of Step 2**, since
   `make quickstart`/`make integration` both assert a clean tree --
   `build-session` step 7 sanctions exactly this.
5. **One fence (`test-a-validator-with-contract-pairing.md:16`) is
   GREEN by composition, not by a single clean run** -- it embeds `make
   integration`, which exceeded the harness's 420 s cap; the embedded
   target was verified independently in Step 2 and the fence's final
   command run separately. Stated rather than claimed as one run.

## Verification

- `bin/preflight` plain: five checks, all reported above.
- `bin/tag-ceremony ... --push`: peeled ref verified on the remote.
- `make quickstart` / `make integration`: full logs, `MAKE_EXIT`
  captured explicitly, both **0**.
- C-4: red-before-green, both runs' own output, plus full docs-tooling
  brick green.
- Register arithmetic: 42+7+5+4 = 58, matching the census's own
  independent bare-fence count.
- **No regression-oracle claim is made or owed** -- this session
  changed no vendored-root-producing code.
