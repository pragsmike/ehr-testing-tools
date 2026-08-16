# 2026-08-16 -- D8-5 live fence battery: 202 fences enumerated from the tree, 58 bare fences run, riders C-4 and C-2

Drafted by the design channel 2026-08-15 against a fresh public clone
at `abb0239`. Chartered by author ruling **Q2 a** ("Concur. Go.",
2026-08-15): the battery runs as its own session before review 4.
Archived verbatim below, followed by the deviations this session
recorded against it.

## The prompt, as supplied

**What this session is, and is not.** An EXECUTION session, not a fix
session. Its product is a dated register of what every live command
fence in the reader-facing docs actually does when run, compared with
what the page claims. It fixes nothing (two riders excepted): red
fences become register rows for the author's ruling. Nothing in the
battery's own findings moves before rulings -- the repo-review skill's
law, inherited. Population scale, probed by the channel: ~198 fenced
blocks across README, `docs/**`, `components/*/docs/**`, `demos/**`.
Not all are commands; classification is the session's first job.

**Author rulings cited:** Q2 a (battery chartered standalone before
review 4); riders C-4 and C-2 channel-batched, no separate ruling owed,
each with its own STOP if non-trivial; any red fence's disposition NOT
pre-ruled -- Step 5 STOPs with the register.

**Read first:** register row D8-5 (`:309`); ADR-0139's watch-list rows
D8-5, C-4, C-2 and the `:onboarding` budget tripwire (`:465`);
`exercised-sources.edn` and `citation_gate.clj` as the REGISTRY of
exercised fences -- read to know what has an exerciser, **not** as the
population (the arc's law: the population is enumerated from the tree,
and the registry is one of the things being audited); `Makefile`
targets `quickstart` (`:55`), `integration` (`:45`), CI-parity cache
block (`:178-188`); `state_staleness_tripwire_test.clj` (C-4's subject).

**Step 0 -- preflight, cache, budget.** `bin/preflight` plain; verify
BOTH standing tags -- `stable-20260815-result-nodes^{}` =
`b139de58...` and `stable-20260815-review-3-fixes^{}` = `b96c246...`
(described as "paid after ADR-0139 per the author's 'Pay it, message
verbatim' pattern" -- **if ABSENT on the remote, STOP-AND-REPORT: the
arc's tag is unpaid and this session must not run atop an ambiguous
close**). Prime the artifact cache; STOP-AND-REPORT if any fetch fails
offline. Re-derive the `:onboarding` budget FIRST (32 lines headroom)
and plan row placement so the gate does not surprise the close.

**Step 1 -- enumerate the population from the tree.** A small
enumerator, committed with the register. Every fenced block in README,
`SETUP.md`, `docs/**/*.md`, `components/*/docs/**/*.md`,
`demos/**/*.md`, tagged file:line, fence language, classification
(command / output / code-config-prose). Every COMMAND fence gets a
second tag: exercised or bare. **Report the four counts BEFORE running
anything.** This split is the battery's headline.

**Step 2 -- run the exercised set as the repo already does.** `make
quickstart` and `make integration`, unpiped, full logs, `MAKE_EXIT`
captured for each (ADR-0138's law).

**Step 3 -- run the BARE command fences, one by one**, in file order:
verbatim from the repo root (or the cwd the prose names), unpiped, exit
captured, per-fence log. Compare against any paired OUTPUT fence
(byte-identical / identical-modulo-disclosed-nondeterminism /
divergent). Classify GREEN / YELLOW / RED / SKIPPED-WITH-REASON (never
silently). Mutating fences run in a throwaway worktree. Batch by
surface and report at each boundary so a stall is visible.

**Step 4 -- rider C-4.** Red first: the tripwire's population is the
`*-arc-close.md` glob; `0125-...-review-close.md` is titled "arc close"
and escapes it. Add an assertion enumerating arc closes by each ADR's
own first heading AND requiring the filename convention -- witness RED
on 0125. Then EITHER rename 0125 (grep inbound first; **STOP if more
than a handful**) OR make the tripwire read headings. Rename
recommended. Commit separately, message-via-file, citing ADR-0139 C-4.

**Step 5 -- rider C-2, then STOP for rulings.** C-2: one roadmap row
for the CarePlan/Guard standing request. Land the register at
`.agents/plans/<date>-fence-battery-findings.md` with population
counts, per-fence table, exercised/bare split, a summary re-derived
from the rows by count, and a "what a reader would hit first" ordering.
Commit register + enumerator; full `make test` unpiped with `MAKE_EXIT`;
push; `bin/post-push-verify`. STOP-AND-REPORT with the register.

**Fences:** fixes ONLY C-4 and C-2; runs in scratch/worktree; zero
`src` outside the C-4 test change. STOP-AND-REPORT on: arc tag absent
on the remote; cache priming failure; a bare fence that would mutate
tracked files; C-4's rename exceeding a handful of inbound references;
`:onboarding` budget not re-derivable to fit; any fence whose safe
execution is unclear.

## Deviations recorded against this prompt

Five, all disclosed in the session record and summarized here so the
archive stands alone.

1. **Step 0.1's STOP fired on a premise the prompt stated
   incorrectly.** The prompt asserted the arc tag was already paid; it
   was not -- ADR-0139 records the push as HELD, as declared mechanical
   debt. The session stopped, gathered the missing evidence itself (CI
   run `31912592325` green at the tag target), and the author ruled
   *"Pay it, message verbatim -- then run the battery."* Paid, peeled
   ref verified.
2. **C-2 needed no action** -- its roadmap row already existed at
   `roadmap.md:82`, landed by ADR-0139's own close. Verified by grep
   before acting; not duplicated.
3. **C-4 was twice its stated size.** Enumerating by heading found two
   escaping files, not the one named, with 12 combined inbound
   references -- tripping this prompt's own "more than a handful" STOP.
   Escalated; author ruled to rename both.
4. **A checkpoint commit landed the enumerator ahead of Step 2**, since
   both make targets assert a clean tree (`build-session` step 7).
5. **The prompt's command-head list was treated as a floor, not a
   closed set** -- read literally it under-counts the bare population
   by a third (39 vs 58). Widened, and the widening disclosed in the
   register and in `bin/fence-census`'s own header.
