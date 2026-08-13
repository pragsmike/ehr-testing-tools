## ADR-0121 — User manual S3: the transport pair, chapters 4-5

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

S3 of the five-session user-manual arc (ADR-0119's own charter),
landing the arc's "transport pair" -- chapter 4
(`docs/manual/04-time-on-the-wire.md`, pacing and the latency second
clock) and chapter 5 (`docs/manual/05-batch-delivery.md`, schedule
batching and the batch-straddle case), the latter the arc's own
FEATURED chapter per the author's own charter (`.agents/rulings.md`,
"From ADR-0112," "Batch-straddle documentation placements," author
verbatim: *"We need to add this batch-boundary-straddling encounter
message scenario to the documentation... it should be a demo, and
featured prominently in the tool user guide, and in the more general
EHR testing guide..."*). This session pulls forward the "realism you
didn't script" material earlier working titles had proposed for
Chapter 7 (`docs/manual/00-front.md`, pre-session state) into Chapters
4-5, two sessions early -- disclosed and reconciled below. Read first:
`demos/scenarios/ed-tuesday/README.md`'s second-clock and
batched-delivery sections; `docs/manual/00-front.md` through
`03-*.md` (voice, structure, the ch-3 SVG's own figure convention);
`docs/dev/simulator-architecture.md` §4/§5 (the latency seam); the
supply-batch-straddling-traffic and play use cases; `.agents/
rulings.md` R2/R6/R7 and the ADR-0112 featured-placement ruling.

### Tag ceremony

`origin/main` at `6c000aa` (ADR-0120 close) at session start -- matched
the driving prompt's own stated premise exactly. The last five `main`
CI runs (`gh run list --limit 5 --branch main`, checked at session
start): all `completed`/`success` -- `6c000aa` (4m43s), `9473c81`
(4m34s), `07dbc5d` (4m39s), `800ae28` (4m40s), `0b6d74f` (3m39s) -- no
red among the five.

Tag `stable-20260812-manual-s2` created ANNOTATED at `6c000aa`; pushed;
peeled ref verified exact (`git rev-parse
stable-20260812-manual-s2^{commit}` and `git rev-list -n1
stable-20260812-manual-s2` both return `6c000aa7...`, matching).
License: case (i), channel fresh-clone verification 2026-08-12 per the
driving prompt's own citation (lineage, ASCII x3, zero `src`, exec bit
staged, straddle invariants byte-shared script/README), CI confirmed
green per this preflight.

### Decision

#### Commit 1 (`750de99`) -- chapter 4, time on the wire

`docs/manual/04-time-on-the-wire.md`: `ehrt play`'s own pacing
(`--rate`, `--board` as the downstream-receiver stand-in Chapter 3
already used), linked to `docs/use-cases/play-a-generated-corpus-
back-over-time.md` rather than restated. The second clock:
`docs/dev/simulator-architecture.md` §5's own extension point (`GT x
LatencyParams -> TimedWire`, a second independently seeded RNG at the
emitter seam) taught via its own field audit -- exactly two
timestamp-bearing fields the emitter renders, MSH-7 (transmit,
shiftable) and EVN-2 (clinical, never shifted). Two reader-side
identity anchors, each a passing test cited by name rather than an
unverifiable claim: **huge rate is `show`**
(`play-command-at-huge-rate-matches-show-identity-test`,
`bases/cli/test/ehrt/cli/core_test.clj`) and **zero offsets is plain
emit**
(`emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit`,
`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj`).
The chapter's own worked instance is Walker, William (MRN000013)'s own
admission message: EVN-2 `2026-08-11T03:36:00Z`, MSH-7
`2026-08-11T04:36:46Z` -- both values quoted verbatim from the demo
README's own "The second clock" section, re-verified byte-identical by
fresh regeneration this session (below).

**The figure.** `docs/manual/assets/two-clocks.svg`: one message box
(Walker's own A01) with both fields shown, two horizontal clock axes
(ground-truth/EVN-2 above, wire/MSH-7 below) and a dashed arrow naming
the sampled delay between them -- hand-authored, not mechanically
generated, cited in an SVG source comment to `simulator-
architecture.md` §5's own field audit plus this session's own
re-verification.

#### Commit 2 (`35ea29b`) -- chapter 5, batch delivery [FEATURED]

`docs/manual/05-batch-delivery.md`: `ehrt corpus batch`'s own
sim-independence -- the author's own verbatim ruling quoted (`.agents/
rulings.md`, "From ADR-0111": *"It should work on any corpus, even an
existing directory of foreign (but valid) message files"*) -- BHS/BTS
wrapping, epoch-aligned buckets, BTS-1 self-verification
(`write-and-verify-batch!`), and the interior-gap v1 deferral, all
grounded in the demo README's own witnessed 34-batch listing. The
chapter's own spine is Smith, James (MRN000002): admitted (A01) in
`batch-000.hl7`, discharged (A03) in `batch-001.hl7`, both files
individually BTS-verified clean -- taught as the receiver-side
question, "do I have all of this encounter?", explicitly not as a flag
list (`--baseline` named once, only to disclaim it as the wrong
answer). A new observation this chapter makes, not stated by the demo
README itself: Smith's own EVN-2 clinical times (`00:06:00Z`,
`00:38:00Z`) sit entirely inside `batch-000`'s own window -- it is the
discharge message's own sampled *transmit* delay, Chapter 4's own
second clock, that carries its MSH-7 across the `01:00Z` boundary, not
a long clinical encounter. This ties the "transport pair" together:
the straddle is latency (ch4) compounding with batching (ch5), not two
unrelated phenomena. The taxonomy note (transport realism simulates
CORRECT behavior; mutation injects INCORRECT content) is restated from
the demo README, cited.

**The figure.** `docs/manual/assets/straddle-timeline.svg`: one
encounter bar (Smith's own admission-to-discharge transmit-time span)
crossing a dashed `01:00Z` boundary line, both adjacent batch windows
drawn as clean, verified boxes beneath it -- hand-authored, cited in
an SVG source comment to the demo README's own witnessed batch listing
and straddle prose, re-verified byte-identical by fresh regeneration
this session.

**Witnessed excerpts, re-derived this session.** Because this
session's own fence is DOCS-AND-REGISTERS-ONLY (zero `src`/`test`/
`demos` edits), every strip in both chapters is copied verbatim from
`demos/scenarios/ed-tuesday/README.md`'s own already-published
witnessed output -- but rather than trust that prior witnessing at a
distance, this session re-ran the exact generating commands
(`bin/ehrt corpus generate sim` for the base and latency wires, `bin/
ehrt corpus batch` over the latency wire, seed 20260811,
`config-latency.edn`/`config.edn`) directly against this session's own
tree, writing only to the gitignored `out/` tree, and compared every
resulting value against the README's own prose:

- Ground-truth invariance: `diff` empty, `sha256sum` digest
  `b4e776f773...` identical on both out-dirs -- matches the README
  exactly.
- The 34-batch listing: `batch-000` count 3, `batch-001` count 4,
  `batch-002` count 5, ..., `batch-031` count 1, `batch-032` count 2,
  `batch-033` count 1, all `:verified true`, span
  `{1786406400000, 1786539600000}` -- matches the README exactly,
  including the `batch-031`/`batch-032` interior gap.
- Walker's own admission/discharge MSH-7 and EVN-2 values (`msg-026.hl7`
  A03 discharge, `msg-027.hl7` A01 admission) -- matches the README's
  own prose exactly (`03:36:00Z`/`04:13:00Z` clinical, `04:33:54Z`/
  `04:36:46Z` transmit).
- Smith's own two MSH segments (`batch-000.hl7`/`batch-001.hl7`,
  `MRN000002-A01-`/`MRN000002-A03-`) -- MSH-7 values match the README's
  own "A straddling encounter" prose exactly (`00:30:26Z`/
  `01:34:19Z`); EVN-2 clinical values (`00:06:00Z`/`00:38:00Z`), not
  stated in the README's own prose, are this session's own
  re-derivation.

No divergence found anywhere -- the STOP-AND-REPORT clause this
session's own prompt named for exactly that case never fired.

#### Front-page resequencing, disclosed

`docs/manual/00-front.md`: Chapters 4-5 drop their working-title
markers and gain firm one-liners; the arc-status prose moves from
"Chapters 1-3 are landed" to "Chapters 1-5 are landed," and "Chapters
4-8" narrows to "Chapters 6-8" throughout; the currency contract's
per-chapter witnessing-commit list extends to name Chapters 4 and 5's
own landing commits. **A genuine resequencing conflict, resolved and
disclosed, not silently absorbed:** pre-session, Chapter 7's own
working title was "Realism you didn't script -- the latency and
batching phenomena Chapter 1 only previewed, in depth" -- exactly what
this session's own Chapters 4-5 now deliver, two sessions early. Left
untouched, the page would have shown that material twice under two
different chapter numbers, one landed and one still "proposed." This
session's own resolution (channel-inferred, disclosed as such, NOT a
ruling): Mutate keeps its own chapter at 6, Gate its own at 7 --
neither moved -- and Check's own topic folds into Chapter 8 alongside
verdict-reading at scale, since a per-file assertion's own verdict is
exactly the thing Chapter 8 already teaches reading at scale. This
keeps the ratified eight-chapter, five-session shape (`.agents/
rulings.md` "From ADR-0119" R-M1/R-M2) fully intact -- S4 still lands
two chapters (6-7), S5 still lands one (8) -- and only reallocates
*which* topics sit in the three still-proposed slots. The author may
correct this reading; nothing about it is executed as a ruling, and no
chapter 1-5 prose changed.

**The ADR-0112 featured-placement ruling, quoted here per the driving
prompt's own instruction** (not in chapter prose): `.agents/
rulings.md`, "From ADR-0112," "Batch-straddle documentation
placements" -- author verbatim, *"We need to add this batch-boundary-
straddling encounter message scenario to the documentation. Should it
be a use case? It should be a demo, and featured prominently in the
tool user guide, and in the more general EHR testing guide as it's
something that happens in the real world and can trip up the
unaware."* Chapter 5 is that placement's own landing, three sessions
after the demo (b) and the use case landed.

### Oracle bracket

Pre-analysis: pure identity expected -- every file touched this
session is `docs/manual/*` (new/edited docs), `docs/manual/assets/*`
(two new SVGs), registers, and this ADR/session-record/prompt-archive
set; nothing touches any oracle root's own `src`.

`bin/regression-oracle 6c000aa 35ea29b` ->
**`IDENTICAL: every root's digest matches between 6c000aa and
35ea29b`**, all 35 roots. Matches the pre-analysis exactly.

### Verification

`clojure -M:poly check`: OK, before each commit. Full `clojure -M:poly
test :all skip:integration` run before commit 1's push: 1 failure --
`ehrt.sim-engine.engine-test`'s own
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`,
at an UNPINNED random seed (`1786589996178`, `failing-size 144`), not
the chartered repro seed `7844068501` (ADR-0114 R8). Re-run
immediately after: GREEN, same test passing at a different random seed
(`1786590169276`) -- the standing-license characterization (R8: "a
`clojure.test.check` generative test that fails at a given seed will
fail at that exact seed again, every time... 'flake' here names the
SYMPTOM... rather than the underlying cause") confirmed live, not
merely cited: this session touched zero `src`/`test` anywhere
(`git status --porcelain` before either run showed only `docs/manual/`
files), so the failure is pre-existing and unrelated by construction,
not something this session introduced. No STOP fired -- R8's own
standing license covers exactly this case, and the fence forbids this
session from touching `sim-engine` `src`/`test` regardless. Full `make
test`: run before commit 2's push -- GREEN, 535 assertions, 0
failures, 0 errors, `bin/verify-nist-lock` OK.
`gitleaks git --staged -v`: clean, both commits. `git diff --cached
--stat` reviewed before each commit: exactly the fenced files. Post-
push verification: both commits' pushed messages diffed against their
own message files -- the only delta either time was `git log
--format=%B`'s own trailing-blank-line formatting artifact; the
ASCII-only check on each commit message empty both times.

### Deviations

**No premise mismatch.** Every Read-first document matched its own
characterization in the driving prompt; the tag license's stated
preflight conditions (origin at `6c000aa`, CI green) held exactly;
every command excerpted from `demos/scenarios/ed-tuesday/README.md`
ran exactly as written when re-run this session, with no divergence
from the README's own witnessed output.

**One self-caught process error, fixed before it reached a commit:**
this session's own first drafts of both chapters and both SVG source
comments used literal Unicode em-dashes/arrows/multiplication signs
(`—`/`→`/`×`), then over-corrected by converting the ENTIRE files to
ASCII `--`/`->`/`x`, mistakenly generalizing the ADRs/session-record
"ASCII x3" gate (which the driving prompt's own tag-license language
names, and which checks git commit MESSAGES via `git log --format=%B`,
not doc prose) onto the manual's own body text. Chapters 1-3 were
re-read and confirmed to use real Unicode typographic dashes
throughout (`LC_ALL=C grep -c '[^ -~\t]'` non-zero on all three);
`docs/dev/simulator-architecture.md`, the notation source both new
chapters cite, does too. Both new chapters and the visible `<text>`
content of both new SVGs were rewritten to match that established
manual-specific convention before either commit; literal `--` was
preserved only where it is real CLI-flag syntax (`--rate`, `--board`,
`--interval`, etc.) or, in the SVG XML comments, matching
`gt-emitters.svg`'s own established ASCII-comment precedent. Caught
and fixed before commit 1 was staged -- no push ever carried the
over-corrected version.

**A second self-caught error, also fixed pre-commit:** the front
page's own first-drafted resequencing disclosure used a bare
`ADR-0112` token in prose, tripping `ehrt.docs-tooling.link-footnote-
gate-test`'s own no-visible-ADR-token-in-prose check (run as part of
this session's own pre-push `clojure -M:poly test :all
skip:integration`). Fixed by converting the reference to a footnote
marker (`[^featured-placement]`) with a definition line at the file's
own end -- the same footnote-definition-line exemption
`docs/use-cases/supply-batch-straddling-traffic.md`'s own
`[^adr-0111]` already uses. Red observed once, before any commit;
green confirmed by re-running the same test file before commit 1's
push.

**`docs/manual/00-front.md` landed in the close commit, not split
across commits 1-2.** Unlike S2's own prompt (ADR-0120), which named
the front-page rider explicitly inside its own Commit 2 narrative,
this session's own prompt names `00-front.md` only in the Fences list,
with no per-commit assignment. All of its own edits (Chapter 4's
one-liner, Chapter 5's one-liner plus the resequencing disclosure, the
arc-status prose, the currency contract) were drafted together before
either content commit and, by process oversight, never staged into
either — surfacing only when `git status` was checked ahead of this
close commit. Since both content commits were already pushed by then,
amending either (rather than landing `00-front.md` in a new commit)
was never on the table under this repo's own standing git-safety
discipline; landing it here, fully disclosed, is the fix-forward.
Nothing about the front page's own content is affected -- Chapters
4-5's own prose already stood complete and correct without it.

**The front-page resequencing** (above) is disclosed at length as a
genuine, non-mechanical judgment call -- not something ADR-0119's own
R-M2 ruling, or any other standing ruling, pre-decided by name. No
design option was foreclosed (Chapters 1-5's own landed prose is
unaffected either way; a future session may re-split Mutate/Gate/Check
differently across Chapters 6-8 without reopening anything landed
here), matching the same reasoning ADR-0119/ADR-0120's own disclosed-
but-unratified decisions used.

### Fences

Touched: `docs/manual/04-time-on-the-wire.md` (new);
`docs/manual/05-batch-delivery.md` (new); `docs/manual/assets/
two-clocks.svg` (new); `docs/manual/assets/straddle-timeline.svg`
(new); `docs/manual/00-front.md` (resequencing disclosure, one-liners,
arc-status prose, currency contract); `.agents/plans/roadmap.md` (S3
LANDED row); `.agents/rulings.md` (untouched -- no mid-session ruling
occurred); `notes/adr/0121-manual-s3-transport-pair.md` (this file);
`notes/ADRs.md`; `notes/adr/README.md`; `.agents/session-records/*`;
`.agents/prompts/*`. ZERO `src`/`test`/`demos` touched anywhere. ZERO
edits to Chapters 1-3.

### Index line

```
- 2026-08-12 — manual-s3-transport-pair — ADR-0121
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)
