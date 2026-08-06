## ADR-0061 — UX fixes 3: the typo that succeeded — unknown flags rejected, near-misses named

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-06.

### Context

Prior: ux fixes 2 landed and was design-channel-verified (`e222908`,
`notes/adr/0060-ux-fixes-2.md`). This session executes register row
C-4 (`.agents/plans/2026-08-06-ux-audit-findings.md`), assessed there
as the single most user-hostile gap surveyed: `bin/ehrt sim run
--patiens 200` (a typo of `--patients`) used to succeed silently,
absorbed into `:opts` and echoed back in the run's own manifest as if
intended, `--patients`'s own default silently kept underneath it —
the run looks fine and is wrong. After this session, every flag token
the parser resolves to flag position must be declared for its verb or
the command rejects it by name, with a near-miss suggestion when one
exists.

### Decision

Ruled 2026-08-06, recorded verbatim (author rulings, this session's
own prompt):

**AR-U3-0 (tag, standing ceremony).** Annotated
`stable-20260806-ux-fixes-2` at `e222908`, message "ux fixes 2 landed,
design-channel-verified 2026-08-06 (ADR-0060)"; push; verify.

**AR-U3-1 (the flag universe is the spec's).** The set of valid flags
per verb derives from `cli-spec` itself (per-verb declared flags plus
any global set) — no hand-maintained duplicate list anywhere. If the
ADR-0013 coverage infrastructure exposes this enumeration, reuse it;
if not, a small derivation fn beside the spec — disclosed which.

**AR-U3-2 (the validation).** At the point where the parser has
resolved the verb and is consuming option tokens: any token the
PARSER ITSELF treats as a flag (not a value — see AR-U3-3) that is not
in the verb's valid set → `result/error :unknown-flag {:flag <token>
:verb <group+verb>}`, plus `:did-you-mean <declared-flag>` when a near
match exists (nearest declared flag within Levenshtein distance 2;
ties broken alphabetically; no match → no key). Exit 2 via the
standard mapping; rendered through the same path as ADR-0060's
categories (C-2's pattern). Parsing stops at the first unknown flag —
one clear error beats a cascade.

**AR-U3-3 (values are not flags — the one hazard).** Flag VALUES may
begin with `-` (negative offsets, date-like strings). The validator
judges only tokens in flag position per the parser's own consumption
logic — it must not reclassify a value. Read the parser first; if its
structure conflates flag and value positions such that a local
validator would misfire on any legal value in the spec, STOP-AND-REPORT
with the case — restructuring the parser is not licensed here.

**AR-U3-4 (red-first, three ways).** Witnessed failing tests BEFORE
the fix: (a) the founding-adjacent case — `--patiens 200` currently
succeeds; the test asserts rejection naming `--patiens`, suggesting
`--patients`, exit 2; (b) an unknown flag with no near match rejects
without `:did-you-mean`; (c) the acceptance property — iterate the
spec itself: every declared flag of every verb, supplied with a
plausible value, parses without `:unknown-flag` (this test is the
guard against the validator breaking a legal flag, and it must be
spec-derived so future flags are covered automatically). Transcripts
below.

**AR-U3-5 (scope).** No help.clj changes at all this session. No
voice rewrite (session 4), no wrap (session 5). The oracle bracket
must show all ELEVEN batches identical — flag validation touches no
emitted byte; any change is STOP-AND-ESCALATE.

### Execution

**The parser read, before any validation point was chosen.**
`bases/cli/src/ehrt/cli/core.clj`'s `parse` calls
`babashka.cli/parse-args` once, host-side, against a single flat
`cli-spec` map shared across every verb (coercion types only, e.g.
`:seed {:coerce :long}` — not a per-verb restriction; babashka.cli's
own `:restrict` option WOULD enforce a flag allowlist, but it throws
an `ExceptionInfo` on an unknown flag rather than returning a value —
incompatible with this repo's result-not-throw doctrine, ADR-0004 —
and it is global, not per-verb, since `parse` runs before `[group
verb]` is known from `args`. So the validation point is NOT inside
`parse`/`babashka.cli` itself, but a step added in `dispatch`, once
`[group action]` is destructured from `args` — exactly what AR-U3-2
means by "the point where the parser has resolved the verb."

Confirmed live (`clojure -M -e`, against `babashka.cli` 0.12.79
directly) that the parser's own arity-aware consumption already keeps
flag and value positions apart correctly for every case this repo's
declared flags exercise: a DECLARED flag consumes exactly the token
count its own `:coerce` type implies, so `--reference-date -20260101`,
`--at -5`, and `--utc-offset -05:00` all parse with the dash-prefixed
value correctly attached to the flag's OWN key, never split into a
second, spurious flag. An UNDECLARED flag falls back to the library's
own default heuristic — boolean true when immediately followed by
another `--`-prefixed token, else the next token consumed as its
value (even when that token itself starts with `-`, e.g. `--bogus-flag
-5` → `{:bogus-flag -5}`) — behavior already shipped, unrelated to
this session's own change. Because `validate-known-flags` (below)
inspects only `(keys opts)` — the parser's own finished output, never
raw tokens, never re-deriving arity — it cannot misclassify a value as
a flag or vice versa; it can only ever judge a key the parser itself
already decided was a flag. AR-U3-3's hazard therefore does not
materialize: no STOP-AND-REPORT was needed.

**AR-U3-1, the flag universe.** `bases/cli/test/ehrt/cli/help_test.clj`
carries the only existing coverage test that walks `help/cli-spec`
against `dispatch`'s own routing (`command-pairs`, group/verb pairs
only — no existing test or fn walks per-verb FLAGS). `help/cli-spec`
itself, however, already carries the full per-verb flag data DOC-1
built (ADR-0013): every group's `:flags` (or, for a group with
`:verbs`, every verb's own `:flags`), plus a `:global-flags` vector
(`--json`/`--pretty`/`--edn`/`--help`). This is the "if the ADR-0013
coverage infrastructure exposes this enumeration, reuse it" case —
the DATA already existed (the register's own reading was right), but
no derivation FN turning it into a flag-keyword set existed yet, so a
small one was added, beside the spec's own consumer
(`bases/cli/src/ehrt/cli/core.clj`, not `help.clj` itself — no
help.clj changes, per AR-U3-5): `declared-flag-keywords` (a group spec
+ verb name → the set of `:global-flags` ∪ that verb's/group's own
`:flags`, each `"--out-dir"`-shaped string keywordized the same way
`babashka.cli` itself keys `:opts`, dropping the leading `--`).

**AR-U3-2/3-3, the validation point and its one named exception.**
`validate-known-flags` runs inside `dispatch`, immediately after the
final `opts` (post `resolve-path-designators`) is computed and before
the `case group` that routes to a real capability function — wrapped
as `(or (validate-known-flags group action opts) (case group ...))`,
so an unknown-flag error short-circuits the same way `:help`/`help`/
bare-invocation already do above it. `flag-validation-context`
resolves `[group action]` to a `[valid-keywords verb-label]` pair, or
nil when there's nothing to validate against YET because dispatch's
own `case group` is about to produce its own `:unknown-command` error
a moment later for this same `[group action]` — an unrecognized
group, or (for a group requiring one) an unrecognized verb. Flag
validation steps aside in both cases rather than piling a confusing
second error on top of the real one; this is the same "one clear
error beats a cascade" spirit AR-U3-2 names for multiple unknown
flags, applied to unknown-verb vs. unknown-flag precedence too.

`"gate"` is the one named, disclosed exception, not a hand-waved
default: `ehrt gate PATH` with no explicit `v2`/`fhir`/`v2-nist` token
(D11's bare sniff-dispatch) has no single verb identity to validate
against — sniffing decides between `gate v2` and `gate fhir` only
AFTER the file is read, never `gate v2-nist` (no default profile to
sniff into). Its valid-flags target, absent an explicit verb token,
is therefore the UNION of `gate v2`'s and `gate fhir`'s own declared
flags — the full reachable set before sniffing decides which one
actually runs. One consequence, disclosed rather than silently
accepted: `--profile` (a `gate v2-nist`-only flag) is not independently
checked against a bare `ehrt gate PATH` invocation — harmless, since
bare gate already never routes to v2-nist by construction (D11), so a
stray `--profile` alongside it is inert either way, exactly as it was
before this session. Live-verified: `ehrt gate v2 --profile foo
somefile.hl7` (an explicit verb given `--profile`, which IS v2-nist-
only) correctly rejects — the union only widens the BARE case, never
an explicit-verb one.

**AR-U3-2, the near-miss.** `ehrt.kernel` has no distance/similarity
helper — checked fresh this session (`grep -rn` across
`components/kernel/src/`), confirmed absent, the same discipline
U4's own sibling-config check applied (ADR-0060). A small, local,
iterative-DP Levenshtein implementation (`levenshtein-distance`,
scoped to `bases/cli/src/ehrt/cli/core.clj`, not a reusable API) feeds
`nearest-declared-flag`: the nearest declared flag name within
distance 2, ties broken alphabetically (`(sort-by (juxt first second)
...)`), nil — meaning `:did-you-mean` is absent entirely, never
present-and-nil — when nothing is that close. Verified against the
classic `kitten`/`sitting` = 3 case and `patiens`/`patients` = 1
before wiring it in.

**Red-first (AR-U3-4).** Three tests added to
`bases/cli/test/ehrt/cli/core_test.clj`, captured failing against the
unfixed tree:

(a) `dispatch-unknown-flag-is-rejected-by-name-test` — `{:patiens 200
:seed 1}` against `sim run`; before the fix, `(:status r)` was `:ok`
and every other assertion failed against `nil` (the flag silently
absorbed).

(b) `dispatch-unknown-flag-with-no-near-match-has-no-did-you-mean-test`
— `:completely-unrelated-nonsense`; before the fix, `(:category r)`
was `nil` (again silently absorbed, no error at all).

(c) `dispatch-every-declared-flag-of-every-verb-parses-without-
unknown-flag-test` — the acceptance property, iterating
`help/command-pairs` and each pair's own declared `:flags` READ
DIRECTLY off the raw spec structure in the test (not via the
validator's own derivation fn — a bug in that fn must be independently
catchable). This one is NOT red before the fix — trivially green,
since before the fix everything is accepted; it is the safety net
proven to hold green THROUGH the fix, guarding against the validator
over-rejecting a legal flag. Every `-fn` in `dispatch`'s own injectable
map is stubbed to a no-op `(constantly (result/ok {}))`, so only the
flag validator's own accept/reject behavior is under test — no real
command function, subprocess, or filesystem access runs.

**Green.** `clojure -M:poly test project:ehrt-cli`: 233 tests, 679
assertions, 0 failures, 0 errors (up from 230/671 before this
session's own three new tests — the exact +3 tests / +8 assertions
this session added). `clojure -M:poly test` (workspace root): every
brick green. `clojure -M:poly check`: OK.

**Live sanity** (`bin/ehrt`, not only `clojure.test`):

```
$ bin/ehrt sim run --patiens 200 --seed 1
{:status :error, :category :unknown-flag, :payload {:flag "--patiens", :verb "sim run", :did-you-mean "--patients"}}
$ echo $?
2

$ bin/ehrt sim run --seed 1 --patients 1 --bogus-flag foo
{:status :error, :category :unknown-flag, :payload {:flag "--bogus-flag", :verb "sim run"}}
$ echo $?
2

$ bin/ehrt gate --path nonexistent.json
{:status :error, :category :gate-path-not-found, :payload {:path "nonexistent.json", :hint "no such file or directory -- run: ehrt help gate"}}
$ echo $?
2

$ bin/ehrt gate v2 --profile foo somefile.hl7
{:status :error, :category :unknown-flag, :payload {:flag "--profile", :verb "gate v2"}}
$ echo $?
2
```

The founding-adjacent case (`--patiens`) now names itself, suggests
the real flag, and exits 2 — never a silently-wrong successful run. A
genuinely unrelated typo gets the same clean rejection with no
suggestion attached. A bare-gate invocation with a real, union-valid
flag (`--path`) still reaches its own real error unchanged (proving
the union case doesn't over-reject). An explicit-verb invocation with
a flag that's valid ONLY for a sibling verb (`--profile` on `gate v2`,
not `gate v2-nist`) is correctly rejected — proving the union widening
is scoped to the bare case only, not leaking into explicit-verb
validation.

**Commit** (ONE commit, staging hygiene confirmed — exactly the two
files touched, `bases/cli/src/ehrt/cli/core.clj` and
`bases/cli/test/ehrt/cli/core_test.clj`; `config/busy-weekday.md`
confirmed not staged, untouched): `c8c1c9c` ("fix: unknown flags are
rejected by name — the silent typo dies (ux fixes 3,
AR-U3-1/2/3/4)"). `gitleaks git --staged -v`: clean. Pushed;
post-push verification: one delta against the message file, the known
harmless trailing-newline artifact.

**The derivation-fn disposition (AR-U3-1's own disclosure clause).**
Reused: the per-verb flag DATA (`help/cli-spec`'s own `:flags`/
`:global-flags`), exactly as declared, no duplication. Added, local to
`core.clj`, not `help.clj`: `declared-flag-keywords` (the string-flags
→ keyword-set derivation) and `flag-validation-context` (the
[group action] → [valid-keywords verb-label] resolution, including
gate's own union exception) — neither existed anywhere before this
session; both are small, single-purpose, and live beside their only
caller, matching U4's own placement precedent (ADR-0060) for a
similarly-scoped helper.

### Verification

- `clojure -M:poly check`: OK.
- Red→green: 2 genuinely red tests (a, b) — silently accepted →
  `:unknown-flag`, exit 2, with/without `:did-you-mean` as designed;
  1 acceptance-property test (c) green before and after, proving the
  fix never regresses a legal flag.
- `clojure -M:poly test project:ehrt-cli`: 233 passes (671→679
  assertions), 0 failures, 0 errors.
- `clojure -M:poly test` (workspace root): every brick green.
- `gitleaks`: clean, staged scan and the push's own pre-push hook.
- Post-push message verification: one delta against the message file,
  the known harmless trailing-newline artifact.
- Tag verification: `stable-20260806-ux-fixes-2` peeled ref resolves
  to `e222908` exactly.
- **Oracle bracket** (`bin/regression-oracle e222908 c8c1c9c`):
  **IDENTICAL: every root's digest matches between e222908 and
  c8c1c9c** — all eleven vendored-root batches (`appendicitis`,
  `death-fixture`, `ear-infections`, `ear-infections-engine`,
  `ear-infections-history-engine`, `sepsis`, `sinusitis`,
  `sore-throat`, `total-joint-replacement-engine`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`) byte-identical, exactly
  as AR-U3-5 required — flag validation touches no emitted byte.
  Soundness check: `digest.clj` identical outside its own `(ns ...)`
  form; no `--declared-digest-change` needed.
- Manual fresh-probe: the founding-adjacent case, a no-near-match
  case, the bare-gate union case, and the explicit-verb cross-verb
  case all confirmed live against the built `bin/ehrt`, not only under
  `clojure.test`.

### Fences (standing law applies unchanged, this session's own prompt)

Src edits landed ONLY in the option-parsing/validation path
(`bases/cli/src/ehrt/cli/core.clj`: `declared-flag-keywords`,
`flag-validation-context`, `levenshtein-distance`,
`nearest-declared-flag`, `unknown-flag-error`, `validate-known-flags`,
and `dispatch`'s own one-line wiring) and its rendering (unchanged —
`:unknown-flag` renders through the same generic error path every
other `result/error` category already does, C-2's pattern, no new
rendering code). No `help.clj` changes. No parser restructuring
(AR-U3-3's STOP never triggered — see Execution above). No gate
weakening. Tests co-landed in the owning brick's own tree
(`bases/cli/test/ehrt/cli/core_test.clj`). Frozen archives untouched
apart from this ADR + index + Done pointer + session-record/prompt
archival, all sanctioned.

### Consequence

`bin/ehrt sim run --patiens 200` — the register's own C-4 finding,
assessed as the single most user-hostile gap in the entire UX audit —
can no longer produce a silently-wrong successful run: every flag
token is now spoken for, by name, at exit 2, with a suggestion when
one is close enough to be useful. After landing: the design channel
verifies by fresh probe; session 4 is the help-spec voice rewrite —
the design channel's draft arrives for author ruling before that
session is prompted; this landing's own tag rides its Step 0 under
standing ceremony.

### Step 2 (this entry) — record

This ADR lands; `notes/ADRs.md` gains its own index line;
`notes/adr/README.md`'s own file count corrected 58→59 ("as of
ADR-0061"). Done pointer added in the same commit as the index line:

```
- 2026-08-06 — ux-fixes-3 — ADR-0061
```

Session record (`.agents/session-records/2026-08-06-ux-fixes-3.md`)
and this session's own driving prompt archived
(`.agents/prompts/2026-08-06-ux-fixes-3.md`), both indexed in
`.agents/session-records/README.md` and `.agents/prompts/README.md`
in the same commit.

### Appendix — red transcripts

**(a) `dispatch-unknown-flag-is-rejected-by-name-test`, against the
unfixed tree (before `validate-known-flags` existed):**

```
FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:134)
expected: (= :error (:status r))
  actual: (not (= :error :ok))

FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:135)
expected: (= :unknown-flag (:category r))
  actual: (not (= :unknown-flag nil))

FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:136)
expected: (= "--patiens" (:flag (:payload r)))
  actual: (not (= "--patiens" nil))

FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:137)
expected: (= "sim run" (:verb (:payload r)))
  actual: (not (= "sim run" nil))

FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:138)
expected: (= "--patients" (:did-you-mean (:payload r)))
  actual: (not (= "--patients" nil))

FAIL in (dispatch-unknown-flag-is-rejected-by-name-test) (core_test.clj:139)
expected: (= 2 (cli/result->exit-code r))
  actual: (not (= 2 0))
```

**(b) `dispatch-unknown-flag-with-no-near-match-has-no-did-you-mean-
test`, same unfixed tree:**

```
FAIL in (dispatch-unknown-flag-with-no-near-match-has-no-did-you-mean-test) (core_test.clj:147)
expected: (= :unknown-flag (:category r))
  actual: (not (= :unknown-flag nil))

FAIL in (dispatch-unknown-flag-with-no-near-match-has-no-did-you-mean-test) (core_test.clj:148)
expected: (= "--completely-unrelated-nonsense" (:flag (:payload r)))
  actual: (not (= "--completely-unrelated-nonsense" nil))
```

Full run, unfixed tree: `Ran 233 tests containing 679 assertions. 8
failures, 0 errors.` (the two tests above account for all 8; test (c),
the acceptance property, was already green — nothing to break yet).

After the fix: `Ran 233 tests containing 679 assertions. 0 failures,
0 errors.`
