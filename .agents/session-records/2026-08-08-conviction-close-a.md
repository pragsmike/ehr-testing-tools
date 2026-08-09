# 2026-08-08 — Conviction arc close, session A (appends + cadence)

## Scope

Session prompt driving the first of two pre-split close sessions for
the conviction arc (ADR-0085–0088: colorectal investigation → straddle
fix → colorectal payoff → pairing registry) — the author's own ruling,
2026-08-08, verbatim: **"Close. adopt, two close sessions."**, adopting
ADR-0084's own intake suggestion (a first session scoped to Steps 0–1
only, a second to Steps 2–3), first executed here. This session's own
scope: Step 0 (tag verification) and Step 1 (the rulings appends plus
the dependency-review cadence) only. State regeneration, budget
re-derivation, Done rotation, and the closing ADR-0089 are explicitly
OUT of scope, reserved for session B.

Preflight: working directory confirmed the ext4 clone
(`~/src/ehr-testing-tools`), `git config core.hooksPath` confirmed
`.githooks`. HEAD `f8df2cc` exactly (pairing registry, ADR-0088),
branch up to date with `origin/main`, working tree clean, no untracked
files. Last five CI runs on `main` disclosed, all `success`:
`31282587609` (`f8df2cc`), `31282341319`, `31282107053`, `31276965870`,
`31276534167` — no red window. `clojure -M:poly check`: OK. Oracle
pre-digest (`bin/regression-oracle f8df2cc f8df2cc`): all 29 roots
IDENTICAL, byte-for-byte, self-bracket; `declared-digest-change: no
(soundness: yes outside ns form)`.

## Step 0 — Tag (AR-CA-0)

`stable-20260808-pairing-registry` did not already exist locally or on
the remote. Created at `f8df2cc`.

**Fix-forward, disclosed:** the first creation used `git tag <name>
<commit>` (no `-a`), landing a LIGHTWEIGHT tag — a deviation from this
repo's own standing annotated-tag convention (every prior `stable-*`
tag, per direct precedent in ADR-0083/0086/0087/0088, carries an
annotation message). Caught before any downstream use (verification
step, same session, `git cat-file -t` returned `commit` not `tag`).
Corrected in place: the lightweight tag deleted both locally and on the
remote (`git push origin :refs/tags/stable-20260808-pairing-registry`),
then recreated annotated (`git tag -a stable-20260808-pairing-registry
f8df2cc -m "stable-20260808-pairing-registry at f8df2cc (pairing
registry landed, ADR-0088)"`), pushed. Verified: `git cat-file -t`
returns `tag`; peeled ref (`stable-20260808-pairing-registry^{}`)
resolves exactly to `f8df2cc4f5f707ca5284f49ec997644e963f042c`, both
locally and via `git ls-remote --tags origin`.

## Step 1 — Appends + cadence (AR-CA-1/2), commit `ed90706`

**AR-CA-1 — the rulings append.** `.agents/rulings.md` gained a new
section, "From the conviction arc (ADR-0085–0089)", with exactly the
two law texts the driving prompt specified verbatim:

> **Witnessed rows only** — the pairing registry (ADR-0088) holds
> per-operator rows that exist ONLY when the mutate→judge loop was
> actually executed against a real fixture; unwitnessed cells do not
> appear; every pinned expectation is MEASURED before it is written (a
> wrong first measurement is disclosed, never silently discarded —
> ADR-0087/0088's own precedent); tier promotions (report-only →
> gating) happen only by dated author ruling.
>
> **Licenses bind at their own granularity** — a licensed oracle mover
> is licensed by NAME and at the EVIDENCE GRANULARITY the license
> states (ADR-0086: `sleep-apnea`, walks #17/#58/#269); the
> post-change bracket must match at that granularity, and any
> deviation — a different root, a different walk set, a surprise
> identical — is a fresh STOP-AND-REPORT, never absorbed by the
> existing license.

Both law texts checked against the live ADRs before landing: (i)
against ADR-0088's own AR-PD-1 (granularity) and AR-PD-5 (measurement
discipline); (ii) against ADR-0086's own STOP-AND-REPORT license terms
(the `sleep-apnea` mover, walks #17/#58/#269, license terms 1–4).

**AR-CA-2 — the dependency-review cadence report, 2026-08-08, against
tip `f8df2cc`:**

```
library                                   version  latest   type      KB
------------------------------------------------------------------------
ca.uhn.hapi.fhir/hapi-fhir-base           8.2.0    8.10.1   maven  1,164
ca.uhn.hapi.fhir/hapi-fhir-structures-r4  8.2.0    8.10.1   maven     29
ca.uhn.hapi/hapi-base                     2.6.0             maven    653
ca.uhn.hapi/hapi-structures-v24           2.6.0             maven  1,446
gov.nist/hl7-v2-parser                    1.7.3             maven    229
gov.nist/hl7-v2-profile                   1.7.3             maven    123
gov.nist/hl7-v2-validation                1.7.3             maven  1,051
io.github.cognitect-labs/test-runner      dfb30dd           git       26
metosin/malli                             0.20.1            maven     97
org.babashka/cli                          0.12.79  0.12.86  maven     35
org.clojars.cmiles74/clojure-hl7-parser   3.5.1             maven     18
org.clojure/clojure                       1.12.5            maven  4,129
org.clojure/data.json                     2.5.2             maven      9
org.clojure/test.check                    1.1.3             maven     39
org.slf4j/slf4j-nop                       2.0.17            maven      4
```

**Unchanged from the fidelity arc's own AR-FC-2 report** (ADR-0084) —
every coordinate, version, and `latest` value identical: no new
upstream release surfaced across the entire span from the quality-review
arc's own AR-QC-2 report through this session (four sessions/arcs now,
none touching `deps.edn`). The same three coordinates still show a
newer `latest` (`hapi-fhir-base`/`hapi-fhir-structures-r4` 8.2.0→8.10.1;
`org.babashka/cli` 0.12.79→0.12.86, dev-tooling-only). No listed upgrade
reads as security-relevant — a NOTE for the next arc's own intake, not
an act. No `deps.edn` edit made or considered. Captured here verbatim
for session B's own closing ADR (ADR-0089) to cite.

**Gates.** `clojure -M:poly check`: OK, before and after the edit.
Docs-tooling gates touching `rulings.md` (`state_staleness_tripwire_test`,
`tag_law_test`, `roadmap_deferred_closure_lint_test`) confirmed green.
Full suite (`clojure -M:poly test :all skip:integration`) also run,
beyond what this docs-only checkpoint strictly required, as a stronger
check: 566 project-block "0 failures, 0 errors" confirmations
(grepped across the entire run output, not sampled), zero other
fail/error lines, exit code 0 — the same count ADR-0088's own Step 2
reported, confirming nothing regressed since. Staging hygiene:
`git diff --cached --stat` reviewed before commit — exactly
`.agents/rulings.md`, 18 insertions, nothing else staged. `gitleaks git
--staged -v`: clean.

Committed `ed90706` ("docs: the conviction arc's law is appended —
witnessed rows only, licenses bind at their own grain (arc close A,
AR-CA-1/2)"). Pushed; post-push verification (`git log --format=%B -1`
diffed against the source message file): one delta, the known
trailing-blank-line artifact. CI watched to conclusion: run
`31286289031`, `success`, 3m24s.

## AR-CA-3 — the pre-split adoption, recorded verbatim

The author ruled 2026-08-08, design channel, verbatim: **"Close. adopt,
two close sessions."** — adopting ADR-0084's own intake suggestion (a
first session scoped to Steps 0–1 only, a second to Steps 2–3) as
standing practice for arc closes going forward, first executed by this
session. The formal adoption record (why, and any generalization beyond
this one arc) lands in ADR-0089, session B's own closing ADR — this
session's record states only that the ruling was made and that this
session is its first execution.

## AR-CA-4 — the inter-session seam, the debt recorded

This session's own closing tip (`ed90706`, after this record's own
commit lands) is session B's Step-0 tag target:
`stable-20260808-conviction-appends`, to be created ONLY by session B,
and ONLY after the design channel independently verifies this session's
landing by fresh probe — the verification gap is the adopted pattern's
own point, not an accident of scheduling. No `ed90706`-tip tag is
created by this session for its own closing work — the tag law's own
case (ii) licenses a session to tag its PREDECESSOR's verified stable
point, never its own mid-flight tip.

## Judgment calls and their ratification status

- **The lightweight-tag error and its in-session correction (Step 0).**
  Not author-ratified separately — a mechanical slip (a plain `git tag`
  omitting `-a`) caught by this session's own verification step before
  any downstream reliance, corrected by delete-and-recreate rather than
  left standing or silently amended. Disclosed in full above, not
  smoothed past.
- **Running the full suite beyond what the checkpoint required.** The
  driving prompt states the full suite is "NOT required this session
  (docs-only appends; the suite ran green at `948f5e5` and no src path
  is touched)." This session ran it anyway as a stronger confirmation,
  once already mid-verification — a strictly additional check, not a
  substitute for or contradiction of the targeted-gates instruction.
  Disclosed as a deviation-toward-more-verification, not silently
  presented as if it had been required.

## Findings, disclosed not acted

None beyond the tag-creation slip above (self-caught, self-corrected,
not carried forward as an open finding).

## Fences held

No `state.md` edit. No budget re-derivation. No Done rotation. No
reading-set edit. No ADR authored (ADR-0089 is session B's). No roadmap
edit. No `src/`/`test/`/`deps.edn` touch of any kind this session.

## HEAD landed

`ed90706` (Step 1's own commit; this record's own commit lands after,
same push as the prompt archive, per Step 2 of the driving prompt).
