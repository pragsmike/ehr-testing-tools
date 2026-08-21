# 2026-08-21 -- committee SKILL.md mojibake repaired from pinned upstream; divergence annotations rewritten as history

**Prompt:** [`.agents/prompts/2026-08-21-committee-skill-mojibake-repair.md`](../prompts/2026-08-21-committee-skill-mojibake-repair.md)
**Mode:** R30 ceremony, push withheld by the prompt (author pushes).
**Base:** `de950fa`. **ADR:** none -- docs-only
repair under a direct author ruling, no reasoning-of-record change.

Docs-only. The committee `SKILL.md` had been round-tripped through a
non-UTF-8 transcode at some earlier point: every em-dash, en-dash,
right arrow, right single quote, `>=` and horizontal-ellipsis in the
file had become a literal `??` or `???` sequence, in all three tracked
copies. The author ruled: repair it, from the upstream library the
file was vendored from, pinned by sha.

## Step 0 -- receipts

`git fetch` / `git pull --ff-only`: **already up to date**. HEAD
`de950fab25a4d91864be44950b01b7be52e440ab`, tree clean.

`bin/preflight` **exit 1**, two findings, both disclosed and neither
blocking:

- **FINDING: a red run appears among the last five on `main`** --
  `c44d240d` (the `sim-trajectory` -> `patient-simulator` rename).
  Superseded: the three runs at and after it (`b1c0965b`, `6ce2160c`,
  `de950fab`) are all green, and ADR-0162's own addendum records CI
  green at the tip. Historical, already reasoned about.
- **FINDING: working tree is not clean** -- the three
  `committee/SKILL.md` copies, this session's own Step 2 edits.
  Expected: preflight ran after the repair was staged in the working
  tree, not before it.

Clean on everything else: edit root `/home/mg/src/ehr-testing-tools`,
not under `/mnt/`; `core.fileMode` **true**; `core.ignorecase` unset;
HEAD == `origin/main`; last stable tag
`stable-20260821-patient-simulator-charter` @ `6ce2160c`, HEAD
untagged, DISCLOSED, **no tag owed**.

**The damage fingerprint, measured, not assumed.** All three copies at
22,586 bytes / 426 lines / sha256
`4907b815323c4fe9bc06da1dcca4bf8a0b79bcf40ef83e347eb0ca0bc96c6bd2`;
`cmp` byte-identical A==B and A==C. Per copy: **40** `??` sequences
total, of which **31** `??"`, **7** `??'`, **2** `???`. 31 + 7 + 2 =
40 exactly, which also proves the three classes do not overlap (a
`???"` would have made the parts sum above the whole) -- that is what
licensed the substitution ordering in Step 1. The prompt's stated
fingerprint held in every particular.

## Step 1 -- line-wise reconstruction

Upstream pinned at `pragsmike/skills@f033b321891a3bb70ee388343e2f428505ef4925`,
`skills/cyberneutics/committee/SKILL.md`: **425 lines, zero `??`**,
valid UTF-8.

The algorithm, run exactly as specified (embedded as a throwaway
script, run, deleted): a line with no `??` is kept verbatim; a damaged
line is turned into an anchored regex by `re.escape`, its escaped
`??"` / `??'` / `???` replaced by alternations over the plausible
originals, and matched against the library's lines. Exactly one match
takes the library line; zero or several is a STOP.

**32 damaged lines** (carrying the 40 sequences) resolved. **0 stops.
0 residual `??`.** The prompt's expectation was met without a single
ambiguous line -- which is the real result here: the reconstruction is
not a guess per character, it is a lookup keyed by the 99% of each
line that survived intact.

## Step 2 -- the oracle, then propagation

`diff repaired /tmp/lib.md` -- **exactly the 4 divergences the prompt
predicted, and nothing else**:

- **line 7** -- description wording (ETT names the deliberations
  folder in prose; upstream writes the path).
- **line 10** -- `allowed-tools:` vs upstream `compatibility:`.
- **line 35** -- the config step carrying the 2026-07-23 divergence
  annotation.
- **line 426** -- a trailing blank line ETT has and upstream does not
  (`426d425`), which is why the two files are 426 and 425 lines.

Every other line of the repaired file is byte-identical to upstream.
The oracle is what makes this a repair rather than a rewrite: the
divergent lines are exactly the lines ETT meant to diverge on, so
nothing local was silently reverted to upstream on the way through.

Installed to `.agents/skills/committee/SKILL.md` (canonical), copied
to `.claude/skills/committee/SKILL.md` (mirror, per `AGENTS.md`'s
`.claude/` section -- generated/copied, never independently authored)
and `notes/tools/agents/skills/committee/SKILL.md` (frozen record).
Post-install, all three: **zero `??`**, `iconv -f UTF-8 -t UTF-8`
clean, sha256
`7e450e52a3becc05af2dd45f8d91b252ad8df056f0970f932dbcfa6188156b66`,
`cmp` byte-identical.

**COMMIT 1** `272fad8` -- 3 files, 96 insertions / 96 deletions (32
lines x 3 copies). `git diff --cached --stat` read before committing;
`gitleaks git --staged -v` **no leaks found**; message from a file,
ASCII-verified before `git commit -F`.

## Step 3 -- the divergence annotations become history

**Gated on an author ruling the prompt did not carry.** Step 3 read
"ONLY IF the author ruled C1(a)" and recorded no such ruling. Both
readings were defensible, so the session asked in chat rather than
inferring from the prompt's completeness. **The author ruled C1(a)
YES.**

**The claim was re-derived before it was written down.** Per the
prompt's own "re-derive, don't trust", all three upstream skills were
fetched at `88c5bf2` and confirmed: `committee`, `probe` and
`scenarios` each now read `situations_root` from
`.agents/cyberneutics-config.yaml`, with the legacy
`.agents/committee-config.yml` and `.claude/cyberneutics-config.yaml`
demoted to fallbacks. Upstream did adopt this repo's convention. The
annotation was telling the truth in 2026-07-23 terms and had simply
stopped being true.

**Census, before editing.** The annotation appears exactly once in
each of nine files: three skills x three trees. Six were rewritten
(`.agents/` + `.claude/`); the three under
`notes/tools/agents/skills/` were deliberately left alone -- they are
the frozen record of what the skills said, and a record that silently
tracks the live copy is not a record.

**One deviation, fix-forward with disclosure.** The prompt's span
boundary -- "...through `...see AGENTS.md.`" -- is `committee`'s
shape. In `probe` and `scenarios` the annotation runs to end-of-line
and swallows the append clause, citing `AGENTS.md` parenthetically
mid-span, so there is no terminator to cut at. The prompt's stated
INVARIANT is unambiguous, though ("keep the canonical-path sentence
and `Then append <topic-slug>/.` intact"), so the edit was written to
that invariant rather than to the literal boundary: prefix + History
sentence + `Then append \`<topic-slug>/\`.`, asserted per file before
writing (prefix match and tail match both checked, and the single-hit
census re-asserted). One defensible reading, so fix-forward, not a
stop (`rulings.md#R-stop-only-on-two-defensible-readings`). The result
is the same sentence in all three skills, which is what the rewrite
was for.

**COMMIT 2** `009f384` -- 6 files, 6 insertions / 6 deletions, one
line each. `.agents` and `.claude` copies `cmp`-identical per skill
before staging; staged stat read; gitleaks clean; message from file,
ASCII-verified.

## Step 4 -- gates

`make test`, unpiped, to a full log, wrapper ending `exit
"$MAKE_EXIT"`:

    MAKE_EXIT=0
    368 zero-failure blocks / 4,100 tests / 18,378 assertions
    0 blocks with any failure or error
    Execution time: 14 minutes 22 seconds

`clojure -M:poly check` **OK** (`make test`'s own first line).
`ehrt.docs-tooling.skill-mirror-currency-test` ran and passed in both
projects that carry it -- the gate this change most directly aimed at.
`bin/verify-nist-lock` OK, 6 coordinates matching
`artifacts.lock.edn`.

**DISCLOSED, standing and not this session's:** `make test` runs
`clojure -M:poly test :all skip:integration`. The integration tier is
skipped, so a green `make test` is not a green suite -- review-4's
W-1, unchanged here and unaffected by a docs-only change.

**Reading-set budgets: unmoved, and measured rather than asserted.**
The three edited skills are in no reading set -- `.agents/skills/`
contributes only `README.md` and `build-session/SKILL.md` to
`.agents/reading-sets.edn`, neither of which this session touched.
Both commits are line-neutral besides (96/96 and 6/6). Nothing to
ratchet, nothing to compact.

**No roadmap row.** This work had none: it arrived as a direct author
ruling on a defect, not off the register. No rows to move to `Done`,
so no attic rotation is owed at this close.

## Close

**COMMIT 3** -- the close artifacts (this commit; its own sha is
not quotable from inside its own body). `bin/close-scaffold`
wrote this record, the prompt archive, and regenerated
`.agents/session-records/INDEX.md`, `.agents/prompts/INDEX.md` and
`.agents/state-derived.md` (records 165 -> 166, prompts 158 -> 159).
The prompt named only two commits; this third is the standing close
ceremony the prompt itself invoked ("self-archive this prompt ... per
convention"), and `ehrt.docs-tooling.prompt-record-pairing-test` gates
it in both directions -- an archived prompt with no paired record
fails the build, so the archive could not land alone.

**Not pushed.** The prompt withheld the push explicitly; three commits
sit local at `main`, `de950fa..HEAD`. `bin/post-push-verify` is
therefore not run and is not owed until the author pushes. No tag is
licensed or paid.
