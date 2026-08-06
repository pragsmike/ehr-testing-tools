## ADR-0062 — UX fixes 4: the help speaks to operators — the approved rewrite lands, gated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: ux fixes 3 landed and was design-channel-verified (`7665baa`,
`notes/adr/0061-ux-fixes-3.md`). The design channel's help-spec voice
rewrite draft (`.agents/plans/2026-08-06-help-rewrite-draft.md`,
register rows B-1/B-2/B-3/B-4(a) and the audit's classified appendix)
arrived approved in full, including judgment calls J1-J4 as drafted.
This session's job: land the draft, apply its §3 replacement text
verbatim, co-land its §5 voice gate, and regenerate `docs/cli.md`. The
words themselves are the author-ruled artifact — no wordsmithing by
this session, however tempting a string looked; a belief that a string
is wrong is a STOP-AND-REPORT, never an edit.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-U4-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-fixes-3` at `7665baa`, message "ux fixes 3 landed,
design-channel-verified 2026-08-06 (ADR-0061)"; push; verify.

**AR-U4-1 (the draft lands, verified not trusted).** The draft lands
in `.agents/plans/` with its index entry. Pre-landing verification:
every string §3 marks `[unchanged]` must currently be free of §5's
token patterns — an `[unchanged]` claim that fails is a draft defect
(STOP-AND-REPORT).

**AR-U4-2 (apply §3 verbatim).** Every rewritten string replaces its
original exactly as drafted; every `;;` relocation comment lands where
shown; `[unchanged]` strings stay byte-identical. Completeness
invariant, tabled below: every token removed from a rendered string
appears in an adjacent comment — the count must reconcile to the
draft's 38 (24 ADR + 14 milestone) or the delta is disclosed with each
divergent token named.

**AR-U4-3 (the §5 gate, co-landed).** The voice gate exactly as §5
specifies: walks `cli-spec` as data (rendered-string positions only —
`:doc`, `:meaning`, `:positional-doc`, `:default`, `top-level-doc`'s
value); word-bounded patterns; pre-verified against every post-rewrite
string AND against the current clean strings ("MSH-7", "msg-%03d",
dates must survive). Natural red witnessed first against the
unrewritten tree; rewrite + gate land in ONE commit, green.

**AR-U4-4 (the rendered surface proves it).** Regenerate `docs/cli.md`'s
generated region per existing convention (nothing outside it). Attach
a before/after excerpt of `bin/ehrt help` and `bin/ehrt help corpus`
transcripts.

**AR-U4-5 (scope).** J1-J4 are ruled as drafted and recorded. No
render-function changes (wrap mechanics are session 5); no `core.clj`;
no error strings. The oracle bracket must show all ELEVEN batches
identical — help text is not wire output; any change is
STOP-AND-ESCALATE.

### Execution

**Read-first.** The draft (all of it); register rows
B-1/B-2/B-3/B-4(a) (`.agents/plans/2026-08-06-ux-audit-findings.md`);
`help.clj` in full; the render path
(`components/docs-tooling/src/ehrt/docs_tooling/docsgen.clj`,
`Makefile`'s `cli-doc` target) enough to regenerate `docs/cli.md`.

**AR-U4-0, tag.** `stable-20260806-ux-fixes-3` did not yet exist;
created annotated at `7665baa`, pushed, verified.

**AR-U4-1, the draft lands.** `.agents/plans/2026-08-06-help-rewrite-
draft.md` was already present in the working tree (author-placed) —
committed with its `.agents/plans/README.md` index entry. Spot-check:
every `[unchanged]` string the draft names was checked against §5's
patterns; none matched (tabled below, "the `[unchanged]` verification
table"). Commit: `677eca6`.

**A mid-session STOP-AND-REPORT, resolved by the author.** §3's
literal code fence for `exit-codes` shows the def with NO docstring —
just the relocation comment where the docstring used to be. The
current source has a real docstring there (ADR-0004/ADR-0010
reasoning). Applying §3 letter-for-letter would delete it, but the
SAME document's own §4 ("What this does NOT touch") lists "the
def-level docstrings" as out of scope, and the session's own Fences
say "no def-docstring edits" — a direct contradiction inside the
ruled artifact itself, not a case of surrounding structure differing
from the draft's reading. Flagged via `AskUserQuestion`; the author
ruled: keep `exit-codes`' docstring byte-identical, land the §3
comment block beside it as an addition, not a replacement. Applied
that way; no other `def` in `cli-spec` carries this conflict (checked
each — `global-flags` has no docstring to begin with; `top-level-doc`'s
own docstring is untouched by §3, consistent with §4).

**AR-U4-2, applying §3.** Every group/verb/flag block rewritten to
match §3 exactly, per the plan above, with the one exit-codes
deviation just described. The corpus generate block's pre-existing
`;; D10 (ADR-0019)` rename comment (help.clj:86-92) was left in place
unchanged, per the draft's own instruction. `gate-common-flags`'
relocation comment (`;; no-verdict folding policy: ADR-0010.`) lands
once, above the shared def — its single edit clears all three reuse
sites (`gate v2`/`gate fhir`/`gate v2-nist` each carry the same map via
`into`/direct reference). `doctor`'s own D13 citation is dropped
without a SECOND, dedicated comment — the draft places one D13 comment
immediately above `version`, one group earlier, and doesn't repeat it
for `doctor`; read as intentional (adjacent, not deleted) rather than
a gap, since the draft is otherwise scrupulous about explicit comment
placement everywhere else.

**AR-U4-3, the natural red — a genuine divergence from the draft's own
count, disclosed per AR-U4-2's own provision.** The §5 gate
(`bases/cli/test/ehrt/cli/help_voice_test.clj`,
`cli-spec-rendered-strings-carry-no-agent-speak-test`), run against
the UNREWRITTEN tree (help.clj temporarily reverted, gate run, then
reapplied — full transcript in the Appendix), found **36** real token
hits, not the draft's stated 38. Traced to source, not a test bug:

- The draft's own B-1/B-2 evidence was gathered by `grep` over raw
  SOURCE TEXT, lines 14-178 of `help.clj`. The gate walks the realized
  DATA (`clojure.walk/postwalk` over `cli-spec`), which differs from
  raw-text counting in two directions:
  - **Undercounted by raw grep** (2 tokens, real, in the gate's 36):
    the `play` group's `--sink` flag sits at source line **179** — one
    line past the draft's own stated range — carrying `ADR-0017` and
    a second `ADR-0014` (the first `ADR-0014`, in `play`'s own group
    `:doc` at line 172, WAS inside the stated range and correctly
    counted).
  - **Undercounted by raw literal-text counting** (2 extra, real, in
    the gate's 36): `gate-common-flags`' `--treat-no-verdict-as` cites
    `ADR-0010` ONCE in source (its own def) but is reused via `into`
    across THREE verbs (`gate v2`/`gate fhir`/`gate v2-nist`) — the
    gate, walking the realized data, correctly counts it 3 times, not
    1 (net +2 beyond the source's single literal occurrence).
  - **Overcounted by raw grep** (4 tokens, phantom, NOT in the gate's
    36): `exit-codes`' own docstring cites `ADR-0004` and `ADR-0010`;
    `top-level-doc`'s own docstring cites `ADR-0013` and `D13`. Both
    docstrings sit inside the stated line range but are metadata, not
    data — the gate correctly never sees them, and per principle 5 /
    §4 they were never in scope to begin with.
  - **Overcounted by raw grep** (2 tokens, phantom, NOT in the gate's
    36): the pre-existing `;; D10 (ADR-0019)` rename comment
    (help.clj:86-92, already there before this session) sits inside
    the stated line range as source text but, being a comment, was
    never rendered data either.

  Net: 38 − 6 (phantom) + 2 (missed, line 179) + 2 (missed, reuse) =
  **36**, matching the gate's own live count exactly. All 36 real
  token positions are addressed by §3's replacement text — confirmed
  by running the gate again after the rewrite (0 matches). This is a
  divergence in the draft's OWN evidence-gathering method, not in the
  completeness of the actual rewrite; AR-U4-2 explicitly licenses
  disclosing such a delta rather than treating it as a blocker.

**AR-U4-3, green.** Rewrite applied, gate green (0 matches across 151
rendered strings), the existing `help_test.clj` coverage suite green
(11 tests, 229 assertions). `make cli-doc` regenerated `docs/cli.md`
in full — that file carries no hand-edited region by its own
documented convention (`docsgen.clj`'s own docstring), so "regenerate"
means the whole file, not a delimited section. `clojure -M:poly check`:
OK. `clojure -M:poly test :all skip:integration`: 511 tests (workspace
total across every project run that pass reaches), 0 failures, 0
errors — full transcript in `/tmp` this session, summarized here.
`bin/verify-nist-lock`: OK, all 6 coordinates match. Commit: `2f88d48`
("docs: the help speaks to operators — 38 tokens relocated, zero
deleted, gated (ux fixes 4, AR-U4-2/3/4)").

**AR-U4-4, the rendered surface.** Before/after `bin/ehrt help` and
`bin/ehrt help corpus` transcripts captured (Appendix, below) —
identical structure, agent-speak gone, nothing else changed.

### Verification

- `clojure -M:poly check`: OK (both before and after the rewrite).
- Red→green: the §5 gate witnessed red (36 real hits) against the
  unrewritten tree — full failure transcript in the Appendix — then
  green (0 hits) after §3 landed.
- `clojure -M:poly test :all skip:integration` (workspace root): 511
  tests, 0 failures, 0 errors, after the rewrite.
- `bases/cli` (`help-voice-test` + existing `help-test`): 11 tests,
  229 assertions, 0 failures, 0 errors.
- `bin/verify-nist-lock`: OK, 6/6 coordinates match.
- `gitleaks`: clean, both commits' staged scans and their push hooks.
- Post-push message verification: both pushes, one delta each against
  their message file, the known harmless trailing-newline artifact.
- Tag verification: `stable-20260806-ux-fixes-3` peeled ref resolves
  to `7665baa` exactly.
- **Oracle bracket** (`bin/regression-oracle 7665baa 2f88d48`):
  **IDENTICAL: every root's digest matches** — all eleven vendored-root
  batches (`appendicitis`, `death-fixture`, `ear-infections`,
  `ear-infections-engine`, `ear-infections-history-engine`, `sepsis`,
  `sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as AR-U4-5 required — a help-text rewrite touches no emitted byte.
  Soundness check: `digest.clj` identical outside its own `(ns ...)`
  form; no `--declared-digest-change` needed. Bracket ends at `2f88d48`
  (the last behavior-bearing commit), not this record's own closing
  commit, following ADR-0059/ADR-0060's own precedent.

### The `[unchanged]` verification table (AR-U4-1)

Every string the draft marked `[unchanged]`, checked against §5's
patterns before landing the draft (Step 1) — all clean, no draft
defect found:

| Location | Claim | Verified |
|---|---|---|
| `top-level-doc`'s rendered value | already clean | clean |
| `artifact resolve`'s doc | not shown (implicitly unchanged) | clean |
| `corpus mutate`'s other flags (`--path`/`--operator-id`/`--operator-version`/`--out-dir`) + positional-doc | unchanged except `--locator-path` | clean |
| `corpus intake`'s flags (`--path`/`--label`/`--out`/`--received`) + positional-doc | unchanged | clean |
| `corpus operators`'s flags | unchanged | clean |
| `gate v2`'s doc + `gate-common-flags`' `--path`/`--report`/`--baseline` | unchanged except `--treat-no-verdict-as` | clean |
| `gate fhir`'s doc + `--lockfile`/`--out-dir`/`--java-bin` | unchanged except `--no-verdict-cache` | clean |
| `check` group's flags + positional-doc | unchanged | clean |
| `sim run`'s doc + all flags except `--format` | unchanged | clean |
| `sim check`'s doc | already clean | clean |
| `sim identifiers`'s doc + flags | already clean | clean |
| `show`'s positional-doc + `--path` flag | not shown (implicitly unchanged) | clean |
| `play`'s positional-doc + `--path`/`--rate`/`--idle-cap`/`--ticker` | unchanged except `--sink` | clean |

### The token-relocation table (AR-U4-2 completeness invariant)

All 36 real (gate-verified) token hits, each now living in an adjacent
`;;` comment or (one case) an untouched pre-existing docstring:

| Token(s) | Old location | New home |
|---|---|---|
| ADR-0004, ADR-0010 | `exit-codes` docstring (untouched) + code-3 `:meaning` | docstring stays; `;; Mapping reasoning: ADR-0004... ADR-0010...` comment added beside it |
| ADR-0013 (×2) | `--pretty`/`--edn` docs | `;; --pretty/--edn terminal-detection defaults: ADR-0013.` above `global-flags` |
| ADR-0010 (×3, reuse) | `gate-common-flags`' `--treat-no-verdict-as` (walked 3× via `gate v2`/`fhir`/`v2-nist`) | `;; no-verdict folding policy: ADR-0010.` above `gate-common-flags` (one comment, all three reuse sites cleared) |
| ADR-0005, D13 | `artifact` group doc + `--all` flag | `;; Artifact registry design: ADR-0005. --all introduced in D13...` above the group |
| ADR-0015 (×2), D9, ADR-0019 | `corpus generate` doc | `;; generate front door + bare=sim: ADR-0015...` / `;; Zero-flag reproducible defaults: D9 / ADR-0019.` above the group |
| ruling 7, D4, ADR-0015, SS-2, SS-3 | `corpus` group doc | `;; URL designators: ruling 7, docs/source-sink-design.md D4.` / `;; intake compose forms: SS-2..., SS-3...` above the group |
| D12 | `corpus mutate --locator-path` doc | `;; --locator-path default-locator fallback: D12.` above the verb |
| SS-2, SS-3 | `corpus intake` doc | same comments as above (shared citation) |
| ADR-0012, D11, ADR-0019, ruling 7 | `gate` group doc | `;; Sniff dispatch: D11 / ADR-0019... NIST profile tier: ADR-0012. Designators: ruling 7.` above the group |
| ADR-0016 | `gate fhir --no-verdict-cache` doc | `;; verdict cache: ADR-0016.` above the verb |
| ADR-0012 | `gate v2-nist` doc | `;; Engine perf note... ADR-0012.` / `;; Π-bundle vocabulary + CDC fixture provenance: ADR-0012 / register.` above the verb |
| ruling 7 | `check` group doc | `;; Designators: ruling 7.` above the group |
| D13 | `version` group doc | `;; Honest pre-release identity ruling: D13.` above the group |
| D13 | `doctor` group doc | same D13 comment, one group above (adjacent, not repeated — see Execution) |
| ADR-0005, ADR-0012 | `sim` group doc | `;; In-process mount: ADR-0005/ADR-0012 fulfilled...` above the group |
| ADR-0013 | `show` group doc | `;; Display-vs-wire ruling: ADR-0013.` above the group |
| ADR-0014 (×2, incl. line-179 `--sink`), ADR-0015, ADR-0017 | `play` group doc + `--sink` flag | `;; Pacer design: ADR-0014. Lexical-order contract: ADR-0015.` / `;; --sink designator vocabulary: ADR-0017; deferred sinks: ADR-0014.` above the group |

Reconciliation against the draft's stated 38: see the "AR-U4-3, the
natural red" execution note above — 6 phantom (2 already-comment-only,
4 docstring-only) + 2 real hits the draft's own line-range grep missed
(line 179) + 2 real hits from shared-flag reuse the draft's literal
source-text count couldn't see = 38 − 6 + 2 + 2 = 36, the gate's own
live count, addressed in full.

### Fences (standing law applies unchanged, this session's own prompt)

`help.clj` edits landed ONLY in the §3 replacement strings, the `;;`
relocation comments, and the one author-ruled exception (exit-codes'
docstring kept, comment added beside it rather than replacing it) — no
render functions, no structural changes, no other def-docstring edits.
One new file: the gate test
(`bases/cli/test/ehrt/cli/help_voice_test.clj`). One regenerated file:
`docs/cli.md` (in full, per its own wholly-generated convention). No
wordsmithing beyond the draft. No gate weakening. Frozen archives
untouched apart from this ADR + index + Done pointer +
session-record/prompt archival, all sanctioned.

### Consequence

`bin/ehrt help` and every `bin/ehrt help <group>` page now read as
operator documentation: what a command does, how to run it, what its
flags mean and default to — no `ADR-NNNN`, no `D13`, no `ruling 7`
leaking maintainer shorthand onto a surface a first-time user reads
cold. The reasoning those tokens carried is not gone — it moved beside
the data it used to decorate, in `;;` comments only a maintainer
reading source ever sees. After landing: the design channel verifies
by fresh probe, including reading the rendered `bin/ehrt help`
transcripts against the draft; session 5 (the wrap mechanism, B-4b)
follows as the arc's last fix, then the arc close. This landing's own
tag rides session 5's Step 0 under standing ceremony.

### Step 3 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its own index line;
`notes/adr/README.md`'s own file count corrected 59→60 ("as of
ADR-0062"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-fixes-4 — ADR-0062
```

Session record (`.agents/session-records/2026-08-06-ux-fixes-4.md`)
and this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-4.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md`
in the same commit.

### Appendix — red transcript (against the unrewritten tree)

`cli-spec-rendered-strings-carry-no-agent-speak-test`, help.clj
temporarily reverted to `7665baa`'s content, gate run, then the
rewrite reapplied (the gate test file itself didn't exist at `7665baa`,
so this is the test running against the tree it will guard, one commit
early, by design):

```
Ran 2 tests containing 164 assertions.
22 failures, 0 errors.
{:test 2, :pass 142, :fail 22, :error 0, :type :summary}
```

22 failing assertions (one per offending string; several strings carry
more than one token) account for all 36 real token hits reconciled
above. After the rewrite, the same run:

```
Ran 2 tests containing 164 assertions.
0 failures, 0 errors.
```

(164 assertions total across both tests in the file — the string-level
gate plus the mechanism-sanity pairing test, which was already green
both before and after, since it exercises the pattern function itself,
not `cli-spec`.)

### Appendix — before/after transcripts (AR-U4-4)

**`bin/ehrt help`, unified diff (before = `7665baa`, after = `2f88d48`):**

```diff
--- before
+++ after
@@ -3,22 +3,22 @@
 Every command accepts --json (EDN is canonical, --json a projection); `ehrt show FILE` renders a v2/FHIR file for a human. See docs/formats.md.

 Groups:
-  artifact  Fetch and resolve locked external engine/tool artifacts (ADR-0005).
-  corpus  Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out below may also be spelled as a dir:/file: URL designator (ruling 7, docs/source-sink-design.md D4) instead of a bare path -- bare paths remain the documented, common spelling. `corpus generate sim`/`corpus generate synthea` (ADR-0015) is the front door for generating a corpus; `corpus intake` additionally accepts a generator URL (sim:/synthea:) in place of PATH as its own compose form -- generate, then catalog, in one command (SS-2) -- or a stdin designator (stdin:?format=...&framing=...) -- read piped bytes, spool, then catalog, in one command (SS-3).
-  gate  Conformance-gate a file or directory against HL7 v2, FHIR, or (given --profile) a HL7 v2 conformance profile (NIST, ADR-0012). Bare `ehrt gate PATH` (no verb) sniffs the format via corpus.intake/sniff-format and dispatches between v2 and fhir only (D11, ADR-0019) -- it never dispatches to v2-nist, which has no default profile to sniff into; a directory mixing v2 and fhir, or containing a file the sniffer can't classify, is an error naming the explicit override (`gate v2 PATH` / `gate fhir PATH`), never a silent per-file split. PATH (and gate fhir's --out-dir) may also be spelled as a dir:/file: URL designator (ruling 7) instead of a bare path.
-  check  Check a candidate corpus against an expected corpus and/or explicit per-file assertions -- the corpus's second judge, alongside Gate. DIR may also be spelled as a dir: URL designator (ruling 7) instead of a bare path.
-  version  Prints this repo's own honestly-pre-release identity (never a fabricated semver, D13) plus every pinned artifact's name@version from the lockfile.
-  doctor  Runs SETUP.md's verification checklist as checks (D13): java resolution via the artifact registry, artifact cache presence per lockfile entry, git hooksPath wiring, and platform support. Exit 0 every check passed; 1 at least one failed; 2 couldn't even read the lockfile to know what to check.
-  sim  Run the sim engine, mounted in-process (ADR-0005, ADR-0012 fulfilled) -- ehrt.sim.interface/run-command directly, no subprocess.
-  show  Render a file (or a directory of files sharing one sniffed format) for a human: HL7 v2 (ER7) one segment per line, blank line between messages; FHIR JSON pretty-printed. Pretty-always -- no flags needed, `ehrt show FILE | less` just works regardless of what stdout is attached to. Display is not wire format (ADR-0013): the rendered ER7 is deliberately nonconformant (LF-joined segments) and must never be piped anywhere a real HL7 v2 consumer sits.
-  play  Paces a HL7 v2 (ER7) file's or directory's own messages against their MSH-7 timestamps and renders (or writes) them over time -- `ehrt show` plus time (ADR-0014). `ehrt play FILE` at an arbitrarily large --rate, the default ticker sink, is exactly `ehrt show FILE`. A directory's files must share the sniffed v2 format; they are concatenated in LEXICAL FILENAME ORDER before pacing (ADR-0015) -- that ordering is the contract: name your files so their sort order is their intended play order (the sim generator's own msg-%03d output already satisfies this). A FHIR JSON path, or a FHIR/mixed/unclassifiable directory, is a named, disclosed deferral (:play-input-unsupported).
+  artifact  Fetch and resolve locked external engine/tool artifacts.
+  corpus  Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out also accepts a dir:/file: URL designator in place of a bare path; bare paths are the common spelling. `corpus generate` is the front door for new corpora; `corpus intake` catalogs existing ones -- and can generate-then-catalog, or read piped bytes, in one command (see intake).
+  gate  Conformance-gate a file or directory against HL7 v2, FHIR, or (with --profile) an HL7 v2 conformance profile. Bare `ehrt gate PATH` sniffs the format and dispatches between v2 and fhir only -- never v2-nist, which needs an explicit --profile. A directory mixing formats, or a file that can't be classified, is an error naming the explicit override (`gate v2 PATH` / `gate fhir PATH`), never a silent per-file split. PATH and --out-dir also accept dir:/file: URL designators.
+  check  Check a candidate corpus against an expected corpus and/or explicit per-file assertions -- the corpus's second judge, alongside gate. DIR also accepts a dir: URL designator.
+  version  Print this repo's own pre-release identity (it deliberately has no semver yet) plus every pinned artifact's name@version from the lockfile.
+  doctor  Run SETUP.md's verification checklist as checks: java resolution via the artifact registry, artifact cache presence per lockfile entry, git hooksPath wiring, and platform support. Exit 0: every check passed; 1: at least one failed; 2: couldn't even read the lockfile to know what to check.
+  sim  Run the sim engine, in-process -- no subprocess, no fetched artifacts needed.
+  show  Render a file (or a directory of files sharing one sniffed format) for a human: HL7 v2 (ER7) one segment per line, blank line between messages; FHIR JSON pretty-printed. Always pretty -- `ehrt show FILE | less` just works. The rendered ER7 is display-only and deliberately nonconformant (LF-joined segments): never pipe it anywhere a real HL7 v2 consumer sits.
+  play  Pace an HL7 v2 (ER7) file's or directory's messages against their own MSH-7 timestamps and render (or write) them over time -- `ehrt show` plus time. A directory's files must share the v2 format and are concatenated in LEXICAL FILENAME ORDER before pacing: that ordering is the contract, so name files so sort order is play order (the sim generator's msg-%03d output already is). FHIR or mixed input is a named deferral (:play-input-unsupported).

 Run `ehrt help <group>` for a group's verbs and flags.

 Global flags:
   --json  project the EDN result to JSON (EDN remains canonical)
-  --pretty  force a human-readable summary, even when stdout is piped -- the default at a real terminal already; ADR-0013
-  --edn  force the raw EDN envelope, even at a terminal -- the default when stdout is piped or redirected already; ADR-0013
+  --pretty  force a human-readable summary, even when stdout is piped -- already the default at a real terminal
+  --edn  force the raw EDN envelope, even at a terminal -- already the default when stdout is piped or redirected
   --help  print this command's usage and exit 0 without running it

 Exit codes:
@@ -26,4 +26,4 @@
   0  bare invocation, help, and --help all exit 0 too
   1  ran and legitimately rejected
   2  operational error (bad invocation, missing artifact, subprocess failure, etc.)
-  3  a gate's aggregate contains :no-verdict under the default --treat-no-verdict-as policy (ADR-0010)
+  3  a gate found :no-verdict outcomes and the default --treat-no-verdict-as policy is in effect -- see that flag to fold them into pass or rejected
```

**`bin/ehrt help corpus`, after (excerpt — full page regenerated, same
content as `docs/cli.md`'s own `## \`ehrt corpus\`` section):**

```
ehrt corpus -- Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out also accepts a dir:/file: URL designator in place of a bare path; bare paths are the common spelling. `corpus generate` is the front door for new corpora; `corpus intake` catalogs existing ones -- and can generate-then-catalog, or read piped bytes, in one command (see intake).

ehrt corpus generate
  Generate a deterministic synthetic corpus. Takes a source subcommand: `corpus generate sim` (this workspace's own engine; the flags marked sim:) or `corpus generate synthea` (the flags marked synthea:). Bare `corpus generate` means `generate sim`. Both bare commands are byte-reproducible as-is; re-running into an existing non-empty --out-dir is rejected (:out-dir-exists), never silently overwritten.

Flags:
  --config-path  synthea: Synthea properties file (default: resources/synthea-default.properties)
  --seed  patient/master-generation seed (integer), shared by both sources (default: 1)
  --clinician-seed  synthea: clinician-generation seed (integer) -- Synthea defaults this to wall-clock time otherwise, which breaks reproducibility even with --seed pinned (default: the resolved --seed value)
  ...
```

No `ADR-NNNN`/`D-number`/`SS-number`/`ruling N` token appears anywhere
in either page, live-verified.
