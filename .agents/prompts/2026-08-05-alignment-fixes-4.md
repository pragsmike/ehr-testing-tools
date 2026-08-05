# 2026-08-05 — Alignment fixes 4

Context: `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`),
tip `57ba010` at session start (`notes/adr/0052-alignment-fixes-3.md`,
alignment fixes 3, design-channel-verified). Session record:
[`2026-08-05-alignment-fixes-4.md`](../session-records/2026-08-05-alignment-fixes-4.md).
Decision-of-record: `notes/adr/0053-alignment-fixes-4.md`.

## Prompt, verbatim

2026-08-05 — alignment fixes 4: offline determinism without redistribution — the NIST mirror lives user-side, the lockfile grows teeth
Session prompt (design channel, 2026-08-05). Prior: alignment fixes 3 landed and was design-channel-verified (`57ba010`). Register row A-4's original charter (vendor the NIST jars into a `file://` repo per the wiring notes) is FORECLOSED by ADR-0005's 2026-07-24 amendment (facts register F9), use is permitted specifically as user-initiated fetch from NIST's official channel, and the lockfile states verbatim that this repo redistributes none of them. The author ruled option (a): a USER-SIDE mirror plus mechanized lockfile verification — the offline-determinism goal without redistribution. The NIST licensing inquiry stays an open External; a favorable reply may upgrade this later. R30 ceremony. Read-first: `artifacts.lock.edn` in full (it is this session's authoritative artifact inventory); `components/judge-v2-nist/deps.edn` (its comment block prescribes the foreclosed path — a fix target here); ADR-0005 and its 2026-07-24 amendment (resolve via index); register row A-4; `bin/regression-oracle` as the house style for `bin/` scripts.
Critical environment rule: this session runs on the author's machine. It MUST NOT edit `~/.m2/settings.xml` or anything else under `~/.m2` beyond what normal dependency resolution writes. Creating `~/.ehrt/nist-mirror/` via the new make target's own single proving run is licensed and disclosed; nothing else outside the repo is touched.
Author rulings (record verbatim in ADR-0053)

1. AR-F4-0 (tag). Per the standing mechanic: annotated tag `stable-20260805-alignment-fixes-3` at `57ba010`, message `alignment fixes 3 landed, design-channel-verified 2026-08-05 (ADR-0052)`; push; verify on origin.
2. AR-F4-1 (the mirror, user-side). New `bin/mirror-nist` (bash, house style) + `make mirror-nist`: builds `~/.ehrt/nist-mirror/` in Maven repository layout from the user's own `~/.m2/repository` cache, covering EVERY hit-nexus-sourced artifact listed in `artifacts.lock.edn` (enumerate from the lockfile, not from this prompt — it names more than the three `gov.nist` coordinates; jar AND pom per coordinate, since resolution needs both). Each jar's sha256 is verified against its lockfile entry AT COPY TIME — a mismatch aborts the mirror build with the offending coordinate named. A coordinate absent from `~/.m2` produces a distinct "not yet resolved — run a full build first" exit, not a failure. The script takes `--m2` and `--dest` overrides for testability; defaults are the real paths.
3. AR-F4-2 (activation is the operator's, documented not performed). The mirror is activated via a Maven `<mirror>` entry (`mirrorOf` → `nist-hit`, URL `file://$HOME/.ehrt/nist-mirror`) in the user's own `~/.m2/settings.xml`, which tools.deps honors. The session writes the exact XML into the documentation (AR-F4-5) and does NOT apply it. If the session finds evidence tools.deps does not honor settings.xml mirrors for this case, STOP-AND-REPORT with the evidence — do not improvise an alternative activation mechanism; the fallback design (a documented local `:mvn/repos` URL swap, acknowledged as tree-dirtying) needs an author ruling, not a session's choice.
4. AR-F4-3 (the lockfile grows teeth). New `bin/verify-nist-lock` + `make verify-nist-lock`: reads `artifacts.lock.edn`, locates each listed artifact in `~/.m2` (or `--repo` override), compares sha256s. Three distinct exits: all-match (0), any-mismatch (nonzero, coordinates named — the alarm case), any-absent (distinct nonzero, "not yet resolved"). Wire it into the Makefile target where jar resolution actually already happens — determine WHICH target that is by inspection (`integration`? `quickstart`? the suite itself?), wire there, and disclose the choice and evidence in ADR-0053. Red→green witnessed: run the script against a SCRATCH fixture directory (never `~/.m2`) containing one deliberately wrong-sha jar copy — capture the mismatch red; then against the real cache — capture the pass (or the honest "not yet resolved" if this machine's cache lacks them, in which case run the resolution first since the suite needs it anyway). Transcripts in ADR-0053.
5. AR-F4-4 (surfaces agree with the amendment — law-surface lesson, instance two). Fix-forward, dated, citing ADR-0053 + ADR-0005's amendment: (a) `components/judge-v2-nist/deps.edn`'s comment block — the "vendoring the six resolved jars into a file:// repo mirror is the safe end-state" prescription is replaced by the user-side-mirror posture (fetch stays user-initiated; mirror lives outside the repo; `make mirror-nist` / `verify-nist-lock`; nothing redistributed). (b) Grep the tree for any OTHER surface prescribing in-repo vendoring of these jars (search terms: `file:// repo`, `vendor`, `CDC does exactly this`, `safe end-state`) — each hit gets the same fix-forward or, if historical/frozen, is left with the register-style note recorded in ADR-0053. (c) `artifacts.lock.edn` MAY gain pom sha256 entries as a dated extension if the mirror work makes them cheap to record — optional, disclosed either way. ADR-0053 records the standing lesson for the arc-close append: this is the second instance of a ruling landing while an earlier artifact kept prescribing the foreclosed path — surfaces that prescribe must be swept when the law that governs them changes.
6. AR-F4-5 (documentation). A short ops doc — `components/judge-v2-nist/docs/nist-mirror.md` unless the component's existing docs conventions dictate otherwise (disclose placement): why the mirror exists (no-SLA host, operator change, ADR-0005 posture), `make mirror-nist` usage, the exact settings.xml activation XML, `make verify-nist-lock` semantics and its three exits, and what changes if the NIST licensing External resolves favorably.

Steps
Step 0 — Preflight + tag. Cwd ext4; tip `57ba010` or later-with-disclosure; full suite green baseline (this also forces jar resolution into `~/.m2` if absent); oracle pre-digest. Execute AR-F4-0.
Step 1 — Tooling (AR-F4-1/3). Both scripts + make targets; the scratch-fixture mismatch red and the real-cache pass captured; ONE proving run of `make mirror-nist` against the real cache (the licensed `~/.ehrt` side effect, disclosed); Makefile wiring per AR-F4-3's inspection. Full suite green. Commit: `feat: offline determinism without redistribution — the NIST mirror lives user-side, the lockfile grows teeth (alignment fixes 4, AR-F4-1/3)`
Step 2 — Surfaces (AR-F4-4/5). The deps.edn comment fix-forward, the tree-wide prescription sweep, the ops doc, optional lockfile pom extension. Verify the deps.edn edit is COMMENT-ONLY (`git diff` shows no coordinate or structural changes — resolution behavior identical). Commit: `docs: the vendoring prescription retires — surfaces agree with ADR-0005's amendment (alignment fixes 4, AR-F4-4/5)`
Step 3 — ADR-0053 + record. ADR-0053: rulings verbatim; the foreclosure narrative (wiring notes' prescription vs ADR-0005's amendment, register row A-4's real meaning); both red/green transcripts; the Makefile wiring evidence; the sweep's hit table; the pending arc-close appends (now three: AR-F1-6's two + the law-surface lesson with both instances). Index line; Done pointer `- 2026-08-05 — alignment-fixes-4 — ADR-0053`. Oracle bracket (`57ba010` → tip): all ELEVEN batches identical — scripts, Makefile, comments, and docs change no emitted bytes; any change is STOP-AND-ESCALATE. Session record + prompt self-archive. Final commit: `docs: alignment fixes 4 record — determinism gained, nothing redistributed (ADR-0053)`
Fences
NO jar, pom, or any binary artifact enters the repo — that is the entire point; a staged `.jar` anywhere is an immediate STOP. No edits under `~/.m2` beyond normal resolution; no settings.xml changes (AR-F4-2). `deps.edn` edits are comment-only; `:mvn/repos` and every coordinate stay exactly as they are. No `src/` changes. No gate weakening. Frozen archives untouched (ADR-0053 + index sanctioned); the design-channel wiring-notes document is NOT in this repo — if the sweep finds an in-repo copy or derivative, annotate it dated rather than editing its body. Deferred cluster untouched: LICENSE/NOTICE hygiene is session 5 — the F-4 rows stay open even though this session works adjacent to them.
After landing: design channel verifies by fresh probe; session 5 (LICENSE hygiene, the arc's last fix cluster) follows, then arc close — rulings appends, state.md regeneration, final tag.

## Notable deviations, disclosed

- **All six NIST coordinates were already present in `~/.m2`** at
  session start, resolved by prior sessions — Step 0's own "this also
  forces jar resolution into `~/.m2` if absent" contingency never
  fired. Disclosed, not a premise mismatch: the baseline full-suite run
  still executed exactly as specified, it simply found nothing left to
  resolve.
- **Makefile wiring target: `test`.** Determined by inspection
  (`projects/conformance/deps.edn` declares both the `nist-hit`
  `:mvn/repos` entry and `poly/judge-v2-nist`, confirmed by direct
  read and by facts register F9's own account) — `make test`'s
  `clojure -M:poly test :all skip:integration` step is where that
  classpath, and therefore the six coordinates, actually resolves.
  `bin/verify-nist-lock` was wired as `test`'s own third line. Neither
  `integration` nor `quickstart` (the prompt's own named candidates)
  turned out to be the right target.
- **`ci-parity`'s own inline `poly` command pair was left
  unmodified**, deliberately — the ruling named one target to wire
  into, determined by inspection; `ci-parity` echoes the same two
  commands independently for a different purpose (fresh-clone parity),
  and widening the ruling's own scope to a second call site wasn't
  asked for. Disclosed in ADR-0053 rather than silently done or
  silently skipped.
- **`artifacts.lock.edn` pom sha256 extension: declined.** AR-F4-4(c)
  offered this as optional; judged out of scope this session (Maven's
  own resolver already validates POM structure at use time; the jar
  sha256 is the artifact this repo's supply-chain concern is actually
  about) and recorded as a disclosed non-action in ADR-0053, not a
  silent skip.
- No other deviation. The tree-wide sweep found exactly two live hits
  beyond `deps.edn` itself (the archived spike-notes document,
  annotated per the fence rather than rewritten; one already-archived
  session prompt, correctly historical, no action) plus one already-
  resolved case (ADR-0005's own frozen "Consequence" clause, already
  superseded in place by its own inline amendment) — full disposition
  table in ADR-0053. The oracle bracket (`57ba010` → `d43c143`) showed
  all eleven batches identical, exactly as the fences required — this
  session touched no `src/` at any point.
