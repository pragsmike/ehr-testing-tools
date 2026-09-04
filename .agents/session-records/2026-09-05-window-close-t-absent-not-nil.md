# 2026-09-05 -- `:window-close-t` is absent, never nil, and the run path validates its own schema

Finding 2 of `.agents/session-records/2026-09-05-p7-stop-derivation.md`,
executed. The author ruled option (b) there -- a schema-invalid parent
disqualifies an oracle population, so fix the engine first -- and this
is that session. It also rides the checker gap finding 1 measured,
`roadmap.md#cancel-invariant-has-no-time-clause`, which the same
rulings closed.

Base `d55b90d`. Ceremony: R30, commits at each checkpoint. No
sub-agents. ADR-0178.

## 0. `bin/preflight`

Ran before any git operation. **No findings, exit 0.** Edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; `core.fileMode`
true, `core.ignorecase` unset; tree clean including untracked; local
HEAD matched `origin/main` at `d55b90d`. Two disclosures, both the
correct state: HEAD is not tagged `stable-*`, and the last of the five
CI runs listed (`d55b90d` itself) was still PENDING and was not awaited
to conclusion (AR-CI-4). It went green during the session.

## 1. The derivation -- every oracle root classified

Step 1's gate is that every root is classified, and all 41 are. **Three
carry `:persons` at all**: no other root can produce a placeholder
registration, so no other root can carry `:window-close-t` in any form.
`decide.clj:331` is the ONLY producer of the key in the tree, measured
by grep over `components/**/src`.

Measured at `d55b90d`, before any change, by running each root's own
config through `sim/run-command`:

| oracle root | `:persons` | events | placeholder registrations | key present | **nil pairs** |
|---|---|---|---|---|---|
| `demographic-fold` | `{:count 240 :years 20}` | 671 | 10 | 10 | **0** |
| `encounter-horizon` | `{:count 20 :years 20}` | 170 | 0 | 0 | **0** |
| `chatter-charges` | `{:count 160 :years 20}` | 477 | 10 | 10 | **0** |
| the other 38 | absent | -- | 0 by construction | 0 | **0** |

**Every placeholder in all three resolves**, so no oracle root carries
the defect and the expected-mover set is EMPTY. See section 6 for what
that does to R-sweep.

What does carry it is a bigger person pool, where somebody dies inside
an open window:

| population | invocation | nil pairs | of key-total |
|---|---|---|---|
| `demos/scenarios/dense-7500/config.edn` | seed 5, `--patients 20 --churn` | **3** | 1,064 |
| `test-fixtures/downstream-calibration/config.edn` | seed 424242, `--patients 500` | **7** | 1,460 |
| same | seed 424242, `--patients 1000` | **7** | 1,460 |

The dense-7500 count reproduces the STOP record's own 3 exactly. The
two calibration counts are equal because the key rides the `:persons`
layer (`{:count 20000 :years 20}`) and not the arrival stream, which is
the same flatness the STOP record measured in wall time.

The ADR's payload sentence, derived here and written there: *the removal
is a payload move confined to the `:registered` events of `:persons`
configs, and on the oracle's own population it moves nothing at all.*

**Two more things were measured before the gates were written, because
a gate that convicts a shipped corpus is a gate that stops the build:**

- All four arc-0 gated corpora are **schema-valid**: 0 invalid events of
  1,213 / 1,774 / 1,412 / 97.
- None of the four carries **a cancel preceding its own target**: 0 of 4.

So both new clauses were known silent on every pinned corpus before
either was registered, which is exactly what the catalog pin's own
"WHICH INVARIANT MOVED" disclosure then had to say.

## 2. ADR-0178

`notes/adr/0178-window-close-t-absent-not-nil.md`, commit `624103d`.
Decision, mechanism, payload effect, ADR-0173 and ADR-0166 cited, the
sweep's expected movers named and narrowed. `make adr-index` and
`make state-derived` regenerated; `project:development` green
(27 assertions).

## 3. Red before green

Commit `e653dec`, deliberately red. **Counted, not filtered**, and taken
per namespace because `poly test` ABORTS a project's remaining
namespaces once one fails -- verified against a stashed clean tree,
where all three `sim-check` namespaces run and pass (90 / 140 / 56
assertions):

| namespace | red |
|---|---|
| `ehrt.sim-engine.persons-test` | **2 failures** -- the guards at `:651` and `:844` |
| `ehrt.sim-check.person-invariants-test` | **1 failure** -- the guard at `:537` |
| `ehrt.sim-check.check-test`, time clause only | **1 failure** -- `-detects-a-cancel-before-its-target` |
| `ehrt.sim-check.check-test`, with the schema gate | **does not compile**: `No such var: check/every-event-is-schema-valid` |

`-permits-a-same-instant-cancel` passed on BOTH sides, which is the
point of it: it pins the `:transfer-in-error` shape -- a transfer and
its cancel at ONE instant -- that the new clause must not convict, so
the clause is `<` and not `<=`.

**The third `nil?` guard was not where the STOP record put it.** That
record named `persons_test.clj:651`, `persons_test.clj:844` and
`check.clj:1596`. R-tests names three guards and gives their new form,
`(not (contains? e :window-close-t))`, which is an assertion over an
EVENT. Exactly three assertions of that shape exist, and the third is
`person_invariants_test.clj:537`, not the `check.clj` clause -- which
reads a destructured binding, not an event, and cannot take that form.
One defensible reading, so it was taken rather than escalated.

## 4. Green

Three commits, `916879f` / `ad6e23d` / `fc7a089`.

**R-fix** (`decide.clj`): `:window-close-t` moves out of the
`placeholder?` assoc into its own `(and placeholder? (some? ...))`
clause. Nothing else in the `cond->` moves, and a NON-placeholder
registration is untouched by construction.

**R-gate** (`check.clj`): `every-event-is-schema-valid`, registered
FIRST. Violations carry `:invalid-paths` -- Malli's `:in` -- rather than
whole explanations, so a finding reads `[[:window-close-t]]` instead of
a full explain over a 20-key event with a nested persona.

**R-time** (`check.clj`): the fifth disjunct, `(< (:t event) (:t target))`,
placed after the nil-target disjunct that establishes its guard.

**R-pins**: the arc-0 catalog pin 45 -> 46 in its own commit, the count
in the subject line. WHICH INVARIANT MOVED: none of the findings, per
section 1's two measurements. The new name is pinned FIRST, which is a
placement argument this catalog has not had to make before -- every
previous addition argued adjacency to a mirror or a converse; this one
is presupposed by every row after it.

`clojure -M:poly check` OK. `brick:sim-check`, `brick:sim-engine` and
`brick:sim` green in every composing project.

## 5. The witness, at a real shell

`bin/ehrt sim run ... --format ground-truth`, three real invocations,
each **exit 0**:

| run | before | after | delta | nil pairs after |
|---|---|---|---|---|
| dense-7500, seed 5, `--patients 20 --churn` | 6,100,230 B | 6,100,167 B | **-63 = 3 x 21** | **0** |
| calibration, seed 424242, `--patients 500` | 9,751,861 B | 9,751,714 B | **-147 = 7 x 21** | **0** |
| calibration, seed 424242, `--patients 1000` | 11,966,511 B | 11,966,364 B | **-147 = 7 x 21** | **0** |

`, :window-close-t nil` is 21 bytes, so the byte arithmetic alone
accounts for the whole delta. It was not left at that. **Proven
structurally, per moved root**: reading both logs as EDN, the after-log
equals the before-log with exactly the nil-valued key dissoc'd --
`(= (mapv strip-nil-key before) after)` is TRUE for all three, event
counts unchanged (18,466 / 29,063 / 35,408), and the number of events
that differ at all equals the number of nil pairs (3 / 7 / 7). That is
stronger than "every hunk is one removed pair" and is what the prompt's
proof obligation asked for.

**MOVE, not match, and it is the correct outcome.** Before the fix this
repository reproduced the downstream team's published SHA-256s BYTE FOR
BYTE -- `434232a913c3389fdc3856f9a6eb14854ff6174499e8a5caa0643085824a03d5`
at 500 and
`ddcfc319ffed230a1ce2edd13f62f2fbfd4fd4264eface5bf6a37967ba2deb11` at
1,000, and their own event counts, 29,063 and 35,408, agree to the
event. After the fix they are
`cd40af263c0c639266cd043fd2fe91b44a4fd2d6a8e66a1fc1224e3645bdb27c` and
`b431cfffcd45d5470c719abf4586da02219934dad1856c8fdb6115e9843d5301`.

**Which means the downstream team's own simulator carries this defect
too**: their payloads carry the same 7 nil pairs, at a commit 88 behind
this repository's `main`. Their `PROVENANCE.md` records THEIR values and
is unedited -- it is a third-party artifact and its bytes are the
fixture. The live claim that moves is
`roadmap.md#cancel-transfer-reinstates-a-new-subject`'s own 2026-09-03
sentence, which gets a dated clause naming ADR-0178 rather than being
rewritten: the match was real when measured, and what supersedes it is
this session, by exactly 7 removed pairs per payload.

## 6. The sweep -- UNSPENT, and R-sweep's premise narrowed

R-sweep expected the movers to be the `:persons` roots, each by removed
pairs and nothing else, proven per moved root and then declared.
**Section 1 measured the mover set EMPTY**, so there is nothing to
declare and no re-pin is owed. Both brackets agree:

    bin/ground-truth-bracket d55b90d fc7a089
    --- coverage: 38 roots carry :ground-truth and are digested;
        3 skipped (no such key): appendicitis.edn, ear-infections.edn,
        sore-throat.edn ---
    --- declared-digest-change: no (soundness: yes outside the leading
        docstring) ---
    IDENTICAL: every digested root's :ground-truth matches between
    d55b90d and fc7a089 (38 roots)

    bin/regression-oracle d55b90d fc7a089
    --- declared-digest-change: no (soundness: yes outside the leading
        docstring) ---
    IDENTICAL: every root's digest matches between d55b90d and fc7a089

The FULL oracle was run beside the ground-truth bracket rather than the
bracket alone, and it earns its keep here: R-fix moves a key from one
`assoc` to a later one, and an IDENTICAL over the whole
`{:ground-truth :hl7}` pair is what says neither the emitted wire nor
any serialisation order moved with it.

**This is fix-forward with disclosure, not a STOP.** A ruling that
expects a declaration and gets an IDENTICAL has had its obligation
discharged in the safe direction; the one defensible reading is that
the expected-mover set was an upper bound and measurement narrowed it.
Recorded here because the resumed P7 session's own scope sentence
should not carry the wrong expectation forward.

## 7. Docs

`docs/consuming-ground-truth.md`: the invariant list gains
`every-event-is-schema-valid` at the head (the list is in reporting
order, and that is where `catalog` puts it), and the three "45
invariants" counts become 46. The manifest section is **unchanged**,
deliberately: its `:event-schema-version "1.8.0"` row is the claim that
was false, and this session makes it true rather than restating it.
`components/corpus/docs/use-cases.edn` carries the same two counts and
`docs/use-cases/ground-truth-as-a-test-oracle.md` regenerates from it.

Full `make test` green.

## 8. Judgment calls, and their ratification status

1. **The RED commit was not pushed alone.** R30 commits and pushes at
   each checkpoint; a knowingly-red commit pushed by itself turns CI red
   for the minutes between checkpoints and buys nothing. The red commit
   and its green successors were pushed together, so the history carries
   the red-before-green proof and CI never sees the red tree. Not
   ratified; disclosed.
2. **The third `nil?` guard resolved to `person_invariants_test.clj:537`**
   rather than to `check.clj:1596`. Section 3 gives the argument. One
   defensible reading, so fix-forward.
3. **`check.clj:1596` left as written.** After R-fix a destructured
   absent key is nil and nothing else is, so `(nil? window-close-t)
   :unjudgeable` reads the absence it always meant to read. See the
   finding below for what that does NOT cover.
4. **The full regression oracle run in addition to the bracket.**
   Section 6. Consistent with precedent; not separately ratified.
5. **The counts in `docs/use-cases.edn` moved with the doc.** They are
   the same 45 the invariant list carries and would otherwise have been
   left stale by a fence read narrowly. Not ratified; it is what
   "the invariant list gains the name" means if the number beside it is
   to stay true.

## 9. Findings

- **The downstream QA team's simulator has this defect too.** Section 5.
  Their published 500/1,000 payloads carry 7 `:window-close-t nil` pairs
  each. Worth telling them, and it is the author's to send: nothing here
  reaches out.
- **`check.clj:1402` erases the absent-vs-nil distinction before any
  reader sees it.** `placeholder-registrations` builds
  `{:patient-id :at :window-close-t}` with `(:window-close-t ev)`, so
  its result map ALWAYS contains the key. No consumer of that function
  can tell absent from nil, whatever `classify` does. That is harmless
  today, because nothing produces nil any more -- and what would catch a
  regression that reintroduced it is `every-event-is-schema-valid`, not
  that row. Named rather than fixed: R-fix scopes the src change to
  `decide.clj`.
- **`poly test` aborts a project's remaining test namespaces after the
  first failure.** Not a defect, and it does not affect a green run --
  but a session counting red must run namespaces individually or it will
  under-count. Cost this session one confused measurement before it was
  verified against a stashed clean tree.

## 10. Fences honored

No `operators.clj`. No `mutate`. No P7 work: both
`roadmap.md#referential-corpus-population` and
`roadmap.md#event-mutation-catalog-gate` are untouched, and the
population ledger is unamended. The event schema is unchanged and
`:event-schema-version` stays `"1.8.0"`.
