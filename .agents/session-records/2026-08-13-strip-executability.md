# 2026-08-13 -- strip executability: exercisers, citation gate, ADR-0127 erratum (ADR-0129)

Chartered from a fresh public clone at HEAD `56613c7` (ADR-0128 close;
all four commits CI-green, verified by author gh list + channel API).
Closes `.agents/plans/2026-08-13-manual-review-1.md`'s dimension 1
(strip executability, FAIL) -- the manual-review arc's own front-of-
queue finding.

## Step 0 -- Ceremony + tag

`bin/preflight`:

```
== bin/preflight (main) ==

-- 1. Last five CI runs on main --
  green  56613c75  2026-08-13T21:43:29Z  docs: session record and prompt archive -- agent-facing hardening (AD..
  green  dba20a9f  2026-08-13T21:29:56Z  feat: close-scaffold --expect-tag -- mechanical step-0 receipts check..
  green  fda0b706  2026-08-13T21:20:40Z  docs: anti-fabrication tripwire and step-0 receipts in build-session ..
  green  22a97599  2026-08-13T21:11:27Z  docs: ADR-0127 addendum -- fabricated-draft near-miss recorded; stand..
  green  a884967a  2026-08-13T18:42:37Z  docs: session record and prompt archive -- ceremony scripts and sim-i..
OK: last five runs all green (or none found)

-- 2. Edit-root confirmation --
OK: repo root '/home/mg/src/ehr-testing-tools' is not under /mnt/

-- 3. Tree-clean check (untracked included) --
OK: working tree clean, including untracked files

-- 4. HEAD-vs-remote tip match --
OK: local HEAD (56613c75c35bd1de5e9a66fb57edd84848196a6b) matches origin/main

-- 5. Last stable-* tag / HEAD tagged? --
Last stable-* tag: stable-20260813-ceremony-scripts (a884967aa43cc1f4b7b8ba32524b470d3ce4e525)
DISCLOSED: HEAD is not currently tagged stable-*

== bin/preflight complete ==
```

HEAD confirmed `56613c75c35bd1de5e9a66fb57edd84848196a6b`, matching
the driving prompt's own stated premise exactly.

`bin/tag-ceremony stable-20260813-hardening 56613c75c35bd1de5e9a66fb57edd84848196a6b <msg-file> --push`:

```
OK: created annotated tag 'stable-20260813-hardening' at 56613c75c35bd1de5e9a66fb57edd84848196a6b
7:25PM INF 882 commits scanned.
7:25PM INF scanned ~22596958 bytes (22.60 MB) in 1.7s
7:25PM INF no leaks found
OK
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260813-hardening -> stable-20260813-hardening
OK: pushed refs/tags/stable-20260813-hardening
OK: remote peeled ref for 'stable-20260813-hardening' is 56613c75c35bd1de5e9a66fb57edd84848196a6b, matches target exactly
```

License: the driving prompt's own Step 0, citing the design channel's
fresh-clone verification (HEAD 56613c7, all four ADR-0128 commits
CI-green per author gh list + channel API) -- matching `bin/preflight`'s
own live check exactly.

Oracle pre-digest basis: all 35 roots; predicted end-state pure
identity -- `docs-tooling` is not a pipeline root, and no pipeline
`src` is in this session's own fence. Confirmed at close (below).

## Step 1 -- ADR-0127 erratum, commit `3c9333d`

Append-only dated erratum to `notes/adr/0127-*.md`, matching
`notes/adr/0121-*.md`'s own erratum form: Step 3's own `:sim`
`1170/1295, "none needing a bump"` figure was arithmetically wrong
when recorded -- the five `:sim` paths at that session's own closing
commit (`21114e3`) already summed to 1293 lines, 2 lines of headroom,
not 1170; the 123-line undercount happened not to trip the gate at
the time and went uncorrected until ADR-0128's own +5-line tripwire
edit pushed the real total to 1298, tripping the gate for real and
surfacing the original error. Budget since re-derived to 1495 per
ADR-0128 (not re-derived again here -- that number is unchanged).
Register-line marker added to `notes/ADRs.md`'s own ADR-0127 entry,
matching the ADR-0121 line's own inline-parenthetical convention.

**Process note, disclosed.** The session-record draft (this file) was
created early per the Step-0-receipts practice, before its own paired
prompt archive existed -- tripping `prompt-record-pairing-test` on the
first `make test` run (`session record(s) with no paired .agents/
prompts/ entry ... #{"2026-08-13-strip-executability"}`). Fixed
directly: the driving prompt was self-archived to `.agents/prompts/
2026-08-13-strip-executability.md` immediately (rather than deferred
to Step 5's close-out), and `bin/close-scaffold 2026-08-13
strip-executability "..."` run to add both directories' own README
index lines (both stub files already existed, so both scaffolding
steps reported `SKIP`, both index-line steps reported `UPDATED`).
Re-run: green.

`gitleaks git --staged -v`: clean. `clojure -M:poly check`: OK. Full
`make test`: green, 535 assertions, 0 failures, 0 errors,
`bin/verify-nist-lock` OK. Pushed; `bin/post-push-verify 56613c7
HEAD`: remote tip matched (`3c9333d7`), ASCII clean, CI reported
`in_progress`/pending (un-awaited, AR-CI-4).

(Steps 2-5 recorded below as the session proceeds.)
