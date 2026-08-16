# D8-5 live fence battery — findings register

**Date:** 2026-08-16. **Tip at run:** `6b85227` (session's own C-4
commit) atop `abb0239` (review-3 arc close, ADR-0139).
**Charter:** author ruling **Q2 a** ("Concur. Go.", 2026-08-15) —
the battery runs as its own session before review 4. Register row
`.agents/plans/2026-08-15-repo-review-findings.md` **D8-5** (`:309`),
never executed across two consecutive reviews; ADR-0139's own
watch-list: *"Its own session, with live-execution latitude and a
primed artifact cache. The window it covers landed the entire user
manual."*

**This register fixes nothing.** Every finding below is a row awaiting
the author's ruling (the repo-review skill's law, inherited). The two
riders this session was chartered to carry — C-4 and C-2 — are recorded
in their own section at the end and are the only changes it made
outside this file and the enumerator.

---

## Method, and why the population is not the registry

The population is enumerated **from the tree**, by
[`bin/fence-census`](../../bin/fence-census), committed with this
register so it is reproducible. `exercised-sources.edn` is read as a
**subject** of the audit, never as the population — the
population-closure law (repo-review rubric, adopted at `dbbeb1f`), and
rule 9 of ADR-0139 ("a probe, gate, or tool whose population is a
registry rather than the tree") applied to the instrument before the
instrument is applied to anything else.

Two method points, both discovered by running rather than assumed, and
both changing the headline number:

**D8-5's own command-head list is a floor, not a closed set.** The row
names "`bin/ehrt`, `make`, `bin/`, `clojure`, `git` (or `$ ` prompt
lines)". Taken literally the census finds **39** bare fences. It misses
`java -version` (SETUP.md), `diff out/… out/…` and `head -c 100 …`
(the manual), `jet … | jq …` (formats.md), and the whole `poly …`
family (the Polylith brief). Widened, the count is **58**. A census
that read the row's list as closed would have under-reported the
battery's own headline by a third.

**Exercised-vs-bare is decided per FENCE, not per file.** A fence is
`exercised` only when its own first command line appears inside the
marker block of the `bin/` script `exercised-sources.edn` pairs to its
source. `README.md` carries two registry rows and still holds a bare
fence — the "See it run" block at `README.md:27`, which that registry's
own comment already documents as correctly outside the `:paired`
adjacency rule. Deciding coverage per file would have credited it.

---

## Step 1 — the population

**102 files in scope; 202 fenced blocks; population closed** (the four
classes sum exactly to the total, and `bin/fence-census` prints that
sum rather than asserting it).

| class | count |
|---|---|
| command / **exercised** | **18** |
| command / **bare** | **58** |
| output | 29 |
| other (code / config / prose) | 97 |
| **total** | **202** |

**The headline: 58 of 76 command fences — 76% — have no exerciser.**
The registry covers 8 rows across 7 source files. Everything else in
the reader-facing doc tree had never been run by any gate, across the
window that landed the entire user manual.

Classification rules, stated so the counts can be checked: a fence is
`command` when its first non-comment line is a shell invocation (or the
page wrote an explicit `$ ` prompt); `output` when a non-command fence
follows a command fence with no intervening markdown heading — the
section, not strict blank-line adjacency, is the pairing unit; `other`
otherwise. Data-tagged fences (`json`, `edn`, `clojure`, `mermaid`,
`hl7`, …) are data even when a line inside them parses as a command,
unless the page wrote a `$ ` prompt.

---

## Step 2 — the exercised set, as the repo already runs it

Both unpiped to full logs with `MAKE_EXIT` captured explicitly
(ADR-0138's law, the H-2 incident class).

| target | MAKE_EXIT | result |
|---|---|---|
| `make quickstart` | **0** | green |
| `make integration` | **0** | green — every exerciser passed, both README "What you get" pairs matched their own expected fences |

**The repo's claim about its exercised fences holds.** No
parity-test disagreement.

**Disclosed, because the first two runs were red and the reason
matters:** `make quickstart` failed twice before this green, both times
on its own postconditions rather than on any fence — first
`tree not clean after a full run (ADR-0005 postcondition violated): ??
bin/fence-census`, then
`tracked-scripts-are-executable-in-the-index-test` catching this
session's own commit landing `bin/fence-census` as mode `100644`
(`core.fileMode=false` hid the `chmod +x` locally, exactly the
executable-bit incident class ADR-0004 names). Both were this session's
own artifacts, fixed before the green. **Recording them because the
`MAKE_EXIT` capture is what made them visible at all:** the background
runner's own exit code was `0` both times — it reported the exit of the
trailing `echo`, not of `make`. Read through that, a red run would have
been filed as green.

---

## Step 3 — the 58 bare command fences

Every fence run verbatim from the repo root (or a throwaway cwd for the
six that create directories), unpiped, per-fence log, exit code
captured, 420 s timeout.

| verdict | count |
|---|---|
| **GREEN** | **42** |
| **RED** | **7** |
| **YELLOW** | **5** |
| **SKIPPED-WITH-REASON** | **4** |
| **total** | **58** |

### What a reader would hit first

**Zero RED on `README.md`, `SETUP.md`, or the manual.** All seven REDs
sit in one developer-facing file, `docs/dev/migration/polylith-brief.md`.
The manual's 20 bare fences are GREEN except two sequencing YELLOWs.
Ordered by what a reader meets first:

1. **`docs/manual/04-time-on-the-wire.md:23` — YELLOW.** The chapter
   plays `out/scenarios/ed-tuesday-latency` 45 lines *before* the fence
   at `:68` that creates it. A reader working the chapter in order gets
   `{:status :error, :category :gate-path-not-found}`, exit 2. The
   prose frames it as quoting ed-tuesday's own README, but it is
   presented as a runnable fence with no note that its input comes
   later. **Recommend: fix the page** (one line, or move the fence).
2. **`docs/manual/08-your-own-data.md:82` — YELLOW.** `bin/ehrt check
   out/corpus/synthea-s1-p5/fhir …` needs a Synthea corpus the chapter
   never generates. The prose names the provenance ("copied verbatim
   from the root `README.md`'s own Quickstart… Re-derived fresh this
   session") but not as a prerequisite *step*, so it reads as
   already-available. From a clean `out/`: `{:status :error, :category
   :invalid-target, :reason :not-found}`, exit 2. **Recommend: fix the
   page** — one line naming the prerequisite command.
3. **`docs/manual/02-setup-first-corpus.md:54` — GREEN, with a
   sequencing note.** In isolation it exits 0. Run after Chapter 1's
   identical bare `bin/ehrt corpus generate` at
   `01-what-this-is.md:8`, it hits the `:out-dir-exists` guard, exit 2.
   Not filed as a defect because the very next fence on the same page
   (`:79`) teaches the `rm -rf` and explains the guard — the page
   resolves it 25 lines later. Recorded so a future reader-path probe
   does not re-discover it as new.
4. **`docs/formats.md:506` and `:518` — YELLOW (2).** Both use
   undeclared placeholder paths (`some/corpus`, `some/report.edn`), so
   both fail for a copy-paster: `:506` exits 2 with `jq` printing
   `null`; `:518` exits 1 before `jet` is even reached. `:518`
   additionally teaches `jet`, which is **not on PATH** in this
   workspace and is not vendored. **Recommend: accept-with-disclosure
   or fix the page** — the placeholder convention is real elsewhere in
   the tree (see `in/v2-corpus`, below) but is nowhere declared.
5. **`components/corpus/docs/research/HL7v2-sanitized-corpus-research.md:126`
   — YELLOW.** Runs clean (exit 0), but `curl -L … -o messages.out`
   writes a **2,026-line file into the repo root**, where the reader's
   cwd happens to be. It is not gitignored, so it turns the working
   tree dirty and would have been swept into this session's own close
   commit had the staging-hygiene review not caught it — which is
   exactly how it was found. Every other write-producing fence in the
   tree lands under `out/`, the single tool-owned output root
   (ADR-0013, "always safe to delete"); this one does not. **Recommend:
   fix the page** — `-o out/messages.out`, or a one-line note. Low
   severity, but it is the only fence in the battery that dirties a
   tracked-file surface.
6. **`docs/dev/migration/polylith-brief.md` — RED ×7** (`:333`,
   `:340`, `:357`, `:378`, `:500`, `:521`, `:534`). One root cause:
   **`poly` is not on PATH** in this workspace, which invokes it as
   `clojure -M:poly` everywhere else, `AGENTS.md` included. Six exit
   **127** (command not found); `:340` exits 1 because `cd acme` fails,
   the downstream consequence of `:333` never having created it.
   **Recommend: fix the page** — one prerequisite line, or `clojure
   -M:poly` throughout. Severity is genuinely low (a Polylith reference
   doc for a hypothetical `acme` workspace, not this repo's own
   surface), but seven dead fences in one file is the largest single
   cluster the battery found, and it is exactly the class two reviews
   would have caught had this probe run.

### SKIPPED-WITH-REASON (4), each named, never silent

| fence | why not run |
|---|---|
| `SETUP.md:60` | `sudo apt update && sudo apt install …` — system mutation, requires root. Never run. |
| `docs/dev/migration/polylith-brief.md:347` | `git remote add billing ../billing` / `git fetch` / `git merge --allow-unrelated-histories` — **repo surgery on this very repo.** Recorded, not run, per the battery's own fence. |
| `docs/simulate-your-facility.md:169` | `--config stmarys.edn` — needs a config the reader authors; the page's own EDN block is illustrative and it points at `demos/` for runnable ones. Exits 2 with `:config-not-found`. **Recommend: accept-with-disclosure**, or one line saying "save the block above as `stmarys.edn`". |
| `docs/use-cases/mutate-output-piped-straight-into-intake.md:16` | needs `in/v2-corpus`, which no step on the page creates. **Already ruled: repo review 2's D8-9, close-as-fine** ("the page's own 'You bring' framing already sets the right expectation"). Not re-opened. One nuance worth recording: the pipeline **masks the real error** — the reader sees `:malformed-mllp-frame` from the intake side, while the mutate side's own honest `{:status :error, :category :file-not-found, :payload {:path "in/v2-corpus"}}` (exit 2) is swallowed by the pipe. |

### Expected non-zero exits, verified as taught rather than assumed

Four fences exit non-zero **by design**, and the pages disclose it. Each
was checked against the page's own output fence rather than waved
through:

- **`06-breaking-data-on-purpose.md:108`** — exit 1,
  `:gate-rejected`. The page prints that exact status, and the live
  `:by-code {"invalid" 1, "invariant" 1}` matches its output fence.
  Rejection is the chapter's whole point. **GREEN.**
- **`07-judging.md:79`** — exit **3**. The page prints the
  `no-verdict` block and then explains the exit code explicitly: *"the
  CLI's own default exit code for a no-verdict aggregate (`3`, distinct
  from both `0` and `1`)"*. Live output diffs against the page's fence
  with **zero content-line differences** (only the fence markers).
  **GREEN.**
- **`07-judging.md:107`** — exit 1. Live `by-code {"hl7-exception" 1}`
  matches the page's stated `after:` line exactly. **GREEN.**
- **`use-cases/test-a-validator-with-contract-pairing.md:16`** —
  recorded honestly: the fence *embeds `make integration`*, and the
  page says so ("takes minutes, not seconds"). It hit this session's
  420 s harness cap (exit 124), **not a page defect**: `make
  integration` was run to completion independently in Step 2
  (MAKE_EXIT 0), and the fence's final command
  (`gate fhir out/demo-mutants --report …`) was run separately, exit 1,
  the expected rejection. **GREEN by composition** — stated this way
  rather than claimed as a single clean run.

### A method finding, disclosed because it nearly produced a false RED

The first pass ran the manual against an `out/` already populated by
`make quickstart` and `make integration`. Under that state,
`docs/manual/02-setup-first-corpus.md:97` — the manual's **most
emphatic** claim, that `diff -rq` between two same-seed runs prints
nothing and exits 0, *"not a claim about past testing you're being
asked to trust… run it on your own machine right now"* — came back
**exit 1, `manifest.edn` differing**.

Re-probed from a cleared `out/`, the claim **holds**: `DIFF_EXIT=0`.
The failure was contamination from a stale `sim-s1-p1-first-run`
directory left by earlier work, not a defect. **The entire manual batch
was then re-run from a cleared `out/`**, and every verdict in this
register is from that clean pass. Two other fences
(`use-cases/simulator-traffic-as-intake-source.md:16`,
`02-setup-first-corpus.md:54`) were likewise re-run in isolation and
went green.

Recorded because it is the battery's own instance of rule 9: **an
accumulated `out/` is a registry standing in for a population.** Any
future run of this battery that does not clear `out/` first will
mis-score the manual — in both directions.

### Full per-fence verdicts

| # | fence | verdict |
|---|---|---|
| 01 | `README.md:27` | GREEN |
| 02 | `SETUP.md:36` | GREEN |
| 03 | `SETUP.md:60` | SKIPPED |
| 04 | `SETUP.md:79` | GREEN |
| 05 | `components/corpus/docs/research/HL7v2-sanitized-corpus-research.md:126` | **YELLOW** |
| 06 | `components/judge-v2-nist/docs/nist-mirror.md:42` | GREEN |
| 07 | `demos/traces/boarding-transfer/README.md:16` | GREEN |
| 08 | `demos/traces/emit-state/README.md:13` | GREEN |
| 09 | `demos/traces/module-mix/README.md:12` | GREEN |
| 10 | `demos/traces/order-result/README.md:14` | GREEN |
| 11 | `demos/traces/persona-enriched/README.md:16` | GREEN |
| 12 | `demos/traces/persona-enriched/README.md:23` | GREEN |
| 13 | `demos/traces/site-profiles/README.md:11` | GREEN |
| 14 | `docs/dev/migration/polylith-brief.md:333` | **RED** |
| 15 | `docs/dev/migration/polylith-brief.md:340` | **RED** |
| 16 | `docs/dev/migration/polylith-brief.md:347` | SKIPPED |
| 17 | `docs/dev/migration/polylith-brief.md:357` | **RED** |
| 18 | `docs/dev/migration/polylith-brief.md:378` | **RED** |
| 19 | `docs/dev/migration/polylith-brief.md:500` | **RED** |
| 20 | `docs/dev/migration/polylith-brief.md:521` | **RED** |
| 21 | `docs/dev/migration/polylith-brief.md:534` | **RED** |
| 22 | `docs/dev/simulator-architecture.md:120` | GREEN |
| 23 | `docs/formats.md:506` | **YELLOW** |
| 24 | `docs/formats.md:518` | **YELLOW** |
| 25 | `docs/manual/01-what-this-is.md:8` | GREEN |
| 26 | `docs/manual/02-setup-first-corpus.md:46` | GREEN |
| 27 | `docs/manual/02-setup-first-corpus.md:54` | GREEN (sequencing note) |
| 28 | `docs/manual/02-setup-first-corpus.md:79` | GREEN |
| 29 | `docs/manual/02-setup-first-corpus.md:97` | GREEN |
| 30 | `docs/manual/03-a-simulated-hospital.md:24` | GREEN |
| 31 | `docs/manual/04-time-on-the-wire.md:23` | **YELLOW** |
| 32 | `docs/manual/04-time-on-the-wire.md:68` | GREEN |
| 33 | `docs/manual/04-time-on-the-wire.md:87` | GREEN |
| 34 | `docs/manual/04-time-on-the-wire.md:110` | GREEN |
| 35 | `docs/manual/05-batch-delivery.md:36` | GREEN |
| 36 | `docs/manual/05-batch-delivery.md:100` | GREEN |
| 37 | `docs/manual/05-batch-delivery.md:143` | GREEN |
| 38 | `docs/manual/06-breaking-data-on-purpose.md:26` | GREEN |
| 39 | `docs/manual/06-breaking-data-on-purpose.md:108` | GREEN (expected rejection) |
| 40 | `docs/manual/07-judging.md:79` | GREEN (expected exit 3) |
| 41 | `docs/manual/07-judging.md:107` | GREEN (expected rejection) |
| 42 | `docs/manual/08-your-own-data.md:27` | GREEN |
| 43 | `docs/manual/08-your-own-data.md:82` | **YELLOW** |
| 44 | `docs/manual/08-your-own-data.md:139` | GREEN |
| 45 | `docs/simulate-your-facility.md:169` | SKIPPED |
| 46 | `docs/use-cases/audit-regulatory-evidence-trail.md:16` | GREEN |
| 47 | `docs/use-cases/generate-conforming-data.md:16` | GREEN |
| 48 | `docs/use-cases/generate-controlled-fault-data.md:16` | GREEN |
| 49 | `docs/use-cases/generate-sim-traffic.md:16` | GREEN |
| 50 | `docs/use-cases/judge-user-supplied-data.md:16` | GREEN |
| 51 | `docs/use-cases/mutate-output-piped-straight-into-intake.md:16` | SKIPPED |
| 52 | `docs/use-cases/piped-hl7-traffic-as-intake-source.md:16` | GREEN |
| 53 | `docs/use-cases/play-a-generated-corpus-back-over-time.md:16` | GREEN |
| 54 | `docs/use-cases/reproduction-packages.md:16` | GREEN |
| 55 | `docs/use-cases/simulator-traffic-as-intake-source.md:16` | GREEN |
| 56 | `docs/use-cases/supply-batch-straddling-traffic.md:16` | GREEN |
| 57 | `docs/use-cases/test-a-validator-with-contract-pairing.md:16` | GREEN (by composition) |
| 58 | `docs/use-cases/training-material.md:16` | GREEN |

**Summary re-derived from the rows above by count, not copied:**
GREEN 42, RED 7, YELLOW 5, SKIPPED 4 — **58**, matching the census's own
bare-fence count exactly (the arithmetic law).

---

## Dispositions recommended, for the author's ruling

Nothing below is decided. Ordered by reader impact.

| finding | recommendation |
|---|---|
| `04-time-on-the-wire.md:23` plays a corpus its own chapter creates later | **fix the page** |
| `08-your-own-data.md:82` needs a Synthea corpus the chapter never generates | **fix the page** |
| `polylith-brief.md` ×7 teach bare `poly`, not on PATH here | **fix the page** (prerequisite line, or `clojure -M:poly` throughout) |
| `formats.md:506`/`:518` undeclared placeholder paths; `:518` also teaches un-vendored `jet` | **accept-with-disclosure** or fix |
| `HL7v2-sanitized-corpus-research.md:126` curls `messages.out` into the repo root, dirtying the working tree | **fix the page** (`-o out/messages.out`) |
| `simulate-your-facility.md:169` needs an authored `stmarys.edn` | **accept-with-disclosure** |
| `mutate-output-piped-straight-into-intake.md:16` pipeline masks `:file-not-found` behind `:malformed-mllp-frame` | **fix the tool** (diagnosability), or accept — the input gap itself is already ruled close-as-fine (review 2 D8-9) |
| 58 bare fences / 76 command fences have no exerciser | **standing question for review 4**: is the exercised-sources registry meant to grow toward the tree, or is 24% coverage the intended equilibrium? The battery can state the number; only a ruling can say whether it is the right one. |

**CLOSED 2026-08-16 — ruled and executed.** Author ruling *"Accept
recommendations."*; every row above dispositioned in its own
micro-session, recorded in
[`notes/adr/0140-fence-battery-ruled-fixes.md`](../../notes/adr/0140-fence-battery-ruled-fixes.md).
Four page fixes and the `poly`-alias note at `14c9348`; the two
accept-with-disclosure rows at `43aec70`; R-F7's tool fix, red-first,
at `07a9566`. The last row (76% unexercised) was ruled **out** of that
session and handed to **review 4's D2** with its proposed rule quoted
in the ADR. Rows above are left exactly as the battery wrote them —
the register records what was found, the ADR records what was done
about it.

---

## Riders carried by this session

**C-4 — CLOSED.** `state_staleness_tripwire_test` now enumerates arc
closes by each ADR's own first heading, with a second assertion holding
the filename convention to what the headings declare. **The watch-list
row understated it: two files escaped, not one** —
`0047-scaffolding-compaction-c.md` (heading ends "arc closes") as well
as the named `0125-manual-s5-chapter8-review-close.md`. Combined
inbound references were 12, tripping the driving prompt's own "more
than a handful" STOP; **the author ruled to rename both**. Red
witnessed on both before the rename, green after; full docs-tooling
brick green. Landed at `6b85227`.

**C-2 — ALREADY LANDED, no action taken.** The prompt directed this
session to add a roadmap row for the CarePlan/Guard standing request.
**That row already exists** at `.agents/plans/roadmap.md:82`, landed by
ADR-0139's own close ("registered visibility-first per ruling R-2's own
precedent"). The prompt's premise does not hold against the live tree;
recorded fix-forward rather than duplicating the row. C-2's *other*
half — re-running the D7 probe without using `roadmap.md` as its
exclusion oracle — remains open and is review 4's, not this session's.

**`:onboarding` budget:** re-derived before and after. 2658/2690 at
session start (32 lines headroom, as ADR-0139 predicted); **2657/2690
after** (33 lines) — closing C-4's roadmap row freed more than this
session's Done pointer consumed. **No budget bump needed.**

---

## What this session did NOT do

- **No fence was fixed.** Every finding is a row above.
- **No ADR.** Deferred to the ruled fixes' own session, per the
  battery's prompt.
- **No `src`.** The only code change is the C-4 test and the
  enumerator.
- **`.agents/state.md` not regenerated** — no arc closed here.
