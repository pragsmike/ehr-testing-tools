## ADR-0088 — The pairing registry: inject X, expect X, on the record

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-08.

### Context

Prior: the colorectal payoff (`notes/adr/0087-colorectal-payoff.md`, tip
`5168d3b`) closed the colorectal thread and named this session's own
brief on its own horizon note: "the pairing-as-data registry session."
The roadmap's own Next row (`.agents/plans/roadmap.md`) has carried it
since the quality review: "Pairing-as-data (review P3-3): mutate↔judge
conviction registry — design pass in the design channel first;
vocabulary is load-bearing." `.agents/rulings.md`'s own alignment-arc
entry (from ADR-0050 AR-F1-6) already ruled the registry's landing
spot: "`judge` is the accepted acyclic home for the mutate↔judge
conviction registry; the design pass starts from there" (D-3).

The design pass itself — four shape questions — was ruled by the
author 2026-08-08 in the design channel, "Approve recommendations"
over the four recommendations quoted verbatim below (AR-PD-1 through
AR-PD-4), plus a measurement-discipline ruling (AR-PD-5) and
STOP-AND-REPORT conditions (AR-PD-6). This session executes that
ruled design: the registry itself, its schema/loader, a taxonomy
snapshot for the NIST judge, and the tier-one executable test that
closes the inject-X-expect-X loop by actually running it.

**Prior art (read first, per the driving prompt's own instruction):**
`ehrt.judge.finding`'s `Severity` docstring
(`components/judge/src/ehrt/judge/finding.clj:27-36`) names "P5's
contract-pairing exercise" — the ad hoc precedent this registry
generalizes into checked data. P5 itself is
`notes/tools/prompts/2026-07-24-p5-gates.md` ("P5 — the gates (FHIR +
v2), intake, EXP-C5, contract pairing"); its own executable exemplar
is `projects/integration/test/ehrt/integration/contract_pairing_test.clj`
(FHIR, real official validator subprocess) and its v2 twin,
`components/corpus/test/ehrt/corpus/v2_contract_pairing_test.clj`
(P7, in-process judge-v2-hapi) — this session's own HAPI measurements
(below) reproduce that twin's own pinned codes exactly, confirming
nothing about judge-v2-hapi's own finding vocabulary has drifted since
P7. This registry does not contradict either precedent; it is their
generalization into data checked by execution rather than restated in
a fresh session's own prose.

### Decision

Author rulings, recorded verbatim (design-channel deliberation,
2026-08-08, "Approve recommendations" over four numbered
recommendations plus two further rulings). `[A]` author-ruled.

1. **AR-PD-0** `[A — tag law, case (ii), ADR-0087's own successor tag
   debt]`. `stable-20260808-colorectal-payoff` annotated and pushed at
   `5168d3b` (ADR-0087's own closing tip, confirmed HEAD at session
   start, working tree clean). Tag did not already exist locally or on
   the remote; created fresh. **Executed Step 0.**

2. **AR-PD-1** `[A — granularity]`. The registry is PER-OPERATOR
   WITNESSED ROWS, never a matrix: a row lands only when exercised
   in-session (mutate → judge → observed class); unwitnessed cells do
   not exist. Row shape: `{:operator {:id … :version …} :judge …
   :expected #{…finding classes…} :fixture "…path…" :witness {:adr
   "0088" :date …}}`. Landing spot: the `judge` component — a
   committed EDN resource (nested per the live convention,
   `components/<name>/resources/<name>/…`) + an `ehrt.judge.pairing`
   namespace (loader, malli schema, interface re-export). Operators
   reference the registry, never the reverse.

   **This session's own disclosed shape addition, not silent:** the
   stated row shape carries no field naming WHERE to mutate. Every v2
   operator in the catalog is `:locator-required? true`
   (`ehrt.corpus.operators`) and none declares a `:default-locator`
   yet (that field is calibration work the catalog's own docstring
   defers, out of this session's scope — no operator/catalog edits,
   AR-PD-6). A row without its own replay locator cannot be replayed
   by the tier-one test AR-PD-4 requires. Two fields therefore extend
   the stated shape: `:locator` (every row) and `:profile` (optional,
   `judge-v2-nist` rows only — the conformance-profile bundle
   directory `make-validator` needs alongside the message `:fixture`).
   Both are documented in the committed resource's own header comment
   and in `ehrt.judge.pairing/PairingRow`'s own docstring.

3. **AR-PD-2** `[A — ordering]`. v2 FIRST, on the existing committed
   fixtures — the ADT trio for judge-v2-hapi, the covidELR Π fixture
   (and its test message) for judge-v2-nist. NO FHIR rows this
   session. Rows: each v2 operator the session can genuinely witness
   against each of the two v2 judges — witnessed subset, not forced
   completeness; a pair that can't be cleanly witnessed is SKIPPED and
   named here, not forced.

4. **AR-PD-3** `[A — the taxonomy snapshot]`. A NAMES-ONLY EDN
   snapshot of the NIST engine's finding vocabulary (classifications +
   category names, engine version recorded, NO config bodies),
   committed in `judge-v2-nist`'s resources, plus a CURRENCY GATE test
   that re-derives the names from the resolved jar's own
   `reference.conf` on every run and fails on drift. Registry rows
   whose `:judge` is the NIST judge draw `:expected` from this
   snapshot (a schema-level check). Noted rather than assumed settled:
   the pending NIST licensing inquiry (`notes/facts-register.md` F8;
   `artifacts.lock.edn`'s six NIST-origin coordinates,
   `:license-status :use-permitted--unstated--confirmation-pending`;
   `.agents/plans/roadmap.md`'s own open "NIST licensing inquiry" row)
   stays open regardless of this snapshot — this file transcribes
   FACTS (bare classification/category display-name strings a public
   Report already exposes at runtime through this repo's own
   use-permitted dependency), never the `reference.conf` file itself
   or any of its template/documentation text, but the inquiry's own
   resolution is unaffected either way and this ADR does not claim it
   settled.

5. **AR-PD-4** `[A — consumers, two tiers]`. TIER ONE, gated,
   co-landed with the rows it protects: a per-row executable test (in
   `judge`'s test tree; test context may cross bricks) — for every
   registry row: load `:fixture`, apply `:operator` via the corpus
   interface, run `:judge`, assert at least one `:expected` class
   among the findings. TIER TWO, REPORT-ONLY: adequacy-as-coverage
   (operators lacking any witnessed row) is COMPUTED (a pure helper
   fn, `ehrt.judge.pairing/coverage`) and RECORDED below as a table —
   it does NOT gate.

6. **AR-PD-5** `[A — measurement discipline]`. Every `:expected` set
   is MEASURED in-session before it is written: run the mutation, run
   the judge, transcribe the observed classes, then pin. An observed
   class contradicting the operator's own `:contract` prose is a
   FINDING about the catalog, STOP-AND-REPORTED rather than fixed
   here.

7. **AR-PD-6** `[A — STOP-AND-REPORT conditions]`. `reference.conf`
   not cleanly derivable to names-only; any needed change to operator
   or judge SRC behavior; the P5 prior art contradicting a measured
   row; fixture insufficiency tempting new-fixture authoring beyond a
   trivially-derived mutant input. None of these fired this session
   (see Measurement and Consequence, below).

### Measurement (AR-PD-5, disclosed in full)

**judge-v2-hapi, all five v2 operators, measured against all three
committed ADT fixtures** (`adt-a01-admit.hl7`, `adt-a02-transfer.hl7`,
`adt-a03-discharge.hl7`) — identical outcome on every fixture, and
identical to `v2_contract_pairing_test.clj`'s own pinned codes (P7's
own precedent reproduced, not contradicted):

| operator | locator | verdict | observed `:code` |
|---|---|---|---|
| `:blank-required-field` | `MSH-9` | `:rejected` | `hl7-exception` |
| `:corrupt-encoding-characters` | `MSH-2` | `:rejected` | `hl7-exception` |
| `:malformed-datetime-value` | `PID-7` | `:rejected` | `data-type-exception` |
| `:truncate-segment-fields` | `MSH-9` | `:rejected` | `hl7-exception` |
| `:corrupt-segment-name` | `MSH` | `:rejected` | `encoding-not-supported-exception` |

All five registered as rows against `:judge-v2-hapi`, fixture
`adt-a01-admit.hl7` (the existing corpus suite's own canonical
fixture — the trio-wide identical result is disclosed here, not
re-registered three times over).

**judge-v2-nist, all five v2 operators, measured against the covidELR
fixture** (`components/corpus/test-fixtures/v2-nist/covidELR/
231HL7TestFilewithHHSData.txt`, profile
`COVID19_ELR-v2.3.1`). Baseline (unmutated) already carries 473
findings, `:no-verdict`/`:profile-spec-error` — the profile's own
pre-existing defectiveness (`v2_engine_test.clj`'s own pinned
provenance; reproduced here: `structure/O-Usage` 109, `structure/Usage`
103, `structure/Length Spec Error` 221, `structure/Dynamic Mapping
Match` 8, `value-set/VS Not Found` 28, `content/Constraint Success` 4 —
matches the committed engine test exactly, environment confirmed
consistent):

| operator | locator | verdict | NEW category vs. baseline | disposition |
|---|---|---|---|---|
| `:blank-required-field` | `MSH-9` | `:rejected` | `structure/Usage` +104 | **witnessed** — `Usage` |
| `:truncate-segment-fields` | `MSH-9` | `:rejected` | `structure/Usage` +107 | **witnessed** — `Usage` |
| `:corrupt-encoding-characters` | `MSH-2` | `:rejected` | none (0 findings — engine-level `check-exception`, message unparseable) | **skipped** |
| `:corrupt-segment-name` | `MSH` | `:rejected` | none (0 findings — same, `check-exception`) | **skipped** |
| `:malformed-datetime-value` | `PID-7` | `:no-verdict`/`:profile-spec-error` | none (473 findings, identical per-category frequencies to baseline) | **skipped** |

Two witnessed rows land against `:judge-v2-nist`. Three skipped, per
AR-PD-2's own fence (a pair that can't be cleanly witnessed is skipped
and named, not forced) — none is a catalog contradiction (AR-PD-5's
STOP-AND-REPORT trigger did not fire):

- `:corrupt-encoding-characters` and `:corrupt-segment-name`: the NIST
  engine's own parser (`gov.nist/hl7-v2-parser`, distinct from HAPI's)
  throws before producing any `Report` entries at all — `:rejected` is
  correct (`interpret`'s own `check-exception` arm), but there is no
  finding-level *class* to witness: the verdict is right, the registry
  has nothing to check at the finding-class grain for this engine on
  this input. Honestly recorded as "undetectable at this gate tier" at
  the finding-class grain, the same honest-classification discipline
  P5's own contract-pairing suite established.
- `:malformed-datetime-value`: PID-7's mutated value produces no
  observable NEW category — the per-category finding frequencies are
  byte-identical before and after the mutation. The baseline's own
  473-finding, already-`:profile-spec-error` noise floor (this
  profile bundle ships without `VALUESETBINDINGS.xml`/
  `COCONSTRAINTS.xml`/`SLICINGS.xml`, and `VALUESETS-disabled.xml`'s
  own deliberately-mismatched name keeps value-set checking off,
  `test-fixtures/v2-nist/NOTICE.md`) masks whatever this mutation's
  own effect would otherwise be. Not a catalog contradiction — the
  operator's own `:contract` claims a DTM lexical-format violation
  against HAPI's primitive-type checking specifically (verified true,
  above); it makes no claim about this specific profile bundle's own
  NIST-side field-format checking, which this measurement shows is
  masked at this tier, on this fixture.

### Taxonomy derivation (AR-PD-3)

Derived via `com.typesafe.config.ConfigFactory/load("reference.conf")`
against the resolved `gov.nist:hl7-v2-validation:1.7.3` jar (the
`com.typesafe:config` coordinate already resolves transitively per
`judge-v2-nist/deps.edn`'s own comment) — `report.classification` and
`report.category`, each subtree's own `.root().unwrapped()`, string
values only. **7 classifications, 52 categories**, committed verbatim
at `components/judge-v2-nist/resources/judge-v2-nist/taxonomy.edn`.
Currency gate (`taxonomy_currency_test.clj`) re-derives the same way on
every run and asserts equality against the committed file — witnessed
RED against a deliberately corrupted copy (one category renamed) before
being proven GREEN against the real committed snapshot, both directions
disclosed under Verification, below. `reference.conf` proved cleanly
derivable to names-only (AR-PD-6's own first STOP-AND-REPORT condition
did not fire).

### Tier-two coverage (AR-PD-4, report-only — does not gate)

Computed via `ehrt.judge.pairing/coverage` against
`ehrt.corpus.interface/operator-entries`'s own live v2 catalog (five
operators; `:default-locator` is absent from all five, unrelated to
this table):

| operator | `:judge-v2-hapi` | `:judge-v2-nist` |
|---|---|---|
| `:blank-required-field` | witnessed | witnessed |
| `:corrupt-encoding-characters` | witnessed | not witnessed (skipped, undetectable at finding grain) |
| `:corrupt-segment-name` | witnessed | not witnessed (skipped, undetectable at finding grain) |
| `:malformed-datetime-value` | witnessed | not witnessed (skipped, masked by baseline noise) |
| `:truncate-segment-fields` | witnessed | witnessed |

FHIR operators (`:remove-required-element`, `:duplicate-element`,
`:invalid-code-value`, `:malformed-date`, `:wrong-type-value`, five
total) carry zero rows against zero FHIR judges this session — out of
scope by AR-PD-2, not a gap this table should read as missing
coverage; the storefront-fixture session lands FHIR rows as its own
acceptance proof (roadmap's own existing Next row).

Promoting this table to a gate (e.g. failing when a v2 operator has
zero witnessed rows against a live v2 judge) is explicitly deferred to
a future dated ruling, once FHIR rows exist and the full grid's own
shape is known (AR-PD-4's own text).

### Execution record

**Step 0 (no commit).** Cwd confirmed the ext4 clone
(`~/src/ehr-testing-tools`), tip `5168d3b`, working tree clean, no
untracked files. Last five CI runs on `main` disclosed, all `success`
(`31276965870`/`31276534167`/`31276085600`/`31274667607`/`31274259259`
— no red window). `clojure -M:poly check` OK. Oracle self-bracket
(`bin/regression-oracle 5168d3b 5168d3b`): IDENTICAL, all 29 roots,
byte-for-byte. `stable-20260808-colorectal-payoff` created annotated at
`5168d3b`, pushed, verified.

**Step 1 (no commit).** Prior art located and read (above). Measured
in-session against real fixtures and real judges (Measurement, above).
Taxonomy derived from the resolved jar (above).

**Step 2.** `ehrt.judge.pairing` (schema `PairingRow`/`Registry`,
`load-registry`, `coverage`), the committed registry EDN (7 rows), the
taxonomy snapshot EDN, the currency-gate test, and the tier-one
per-row conviction test — landed together (co-landed invariants law).
`judge-v2-nist/deps.edn` gained `"resources"` on its own `:paths` (its
first resources directory). `ehrt.judge.interface` gained five new
re-exports (`PairingRow`, `PairingRegistry`, `PairingJudgeId`,
`load-pairing-registry`, `pairing-coverage`) — no collision with any
existing export.

Red→green witnessed directly, in-session, for all three new gates
before commit:
- Currency-gate test: corrupted one committed category name
  (`"Usage"` → `"Usage-CORRUPTED"`) — both the taxonomy-drift assertion
  AND the registry-cross-check assertion failed (3 failures); restored
  byte-identical (diffed against a pre-edit backup), re-ran green.
- Tier-one conviction test: corrupted one row's `:expected` set to a
  nonsense code — 3 of 7 rows failed with the actual observed class
  named in the failure message; restored byte-identical, re-ran green.

Full suite (`clojure -M:poly test :all skip:integration`): green
throughout — 566 project-block "0 failures, 0 errors" confirmations
across the full run output (grepped, not sampled), exit code 0,
3m00s elapsed; both new tiers' own tests (`ehrt.judge.pairing-test`,
`ehrt.judge.pairing-conviction-test`, `ehrt.judge-v2-nist.taxonomy-
currency-test`) confirmed present and green in both project groupings
that include them (`development`/`ehrt-cli`, the standard doubled
appearance this workspace's own project composition produces, per
ADR-0087's own precedent note). `clojure -M:poly check`: OK. `gitleaks
git --staged -v`: clean. Staging hygiene: `git diff --cached --stat`
reviewed before commit, showed exactly this checkpoint's own eight
files, nothing else.

Oracle bracket: `bin/regression-oracle 5168d3b 948f5e5` — this session
touches no sim/compile/engine/emitter path; all 29 roots IDENTICAL, as
predicted — byte-for-byte, zero moved.

Commit `948f5e5` ("feat: the pairing registry lands — inject X,
expect X, on the record (pairing-as-data, AR-PD-1/2/3/4)"). Pushed;
post-push verification (`git log --format=%B -1` diffed against the
source message file): one delta, the known harmless trailing-blank-
line artifact. CI watched to conclusion: run `31282107053`, `success`,
3m27s.

**Step 3 (this record).** `notes/adr/0088-pairing-registry.md`
authored directly; index line appended to `notes/ADRs.md`;
`notes/adr/README.md`'s own stale file count corrected (85→86,
verified by `ls notes/adr/*.md | grep -v README | wc -l`, not
arithmetic); roadmap's "Pairing-as-data" Next row removed (executed by
this session) and a Done pointer added; session record and prompt
archive land in the same commit as this record's own citation-index
update.

### Verification

- `bin/regression-oracle 5168d3b 5168d3b` (pre-digest): IDENTICAL, all
  29 roots.
- `bin/regression-oracle 5168d3b 948f5e5` (post-landing bracket):
  IDENTICAL, all 29 roots, byte-for-byte (this session touches no
  sim/compile/engine/emitter path, as predicted).
- Three new gates, each witnessed RED before GREEN, in-session (above):
  `taxonomy_currency_test.clj`'s two deftests (3 failures on a
  corrupted category name, restored, re-ran green), and
  `pairing_conviction_test.clj`'s single deftest (3 of 7 rows failed
  on a corrupted `:expected` set, restored, re-ran green).
- Full suite (`clojure -M:poly test :all skip:integration`): green,
  566 "0 failures, 0 errors" confirmations, exit code 0, confirmed by
  grepping the entire run output (not just the tail).
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean.
- Post-push message verification: one delta, the known harmless
  trailing-blank-line artifact.
- Tag verification: `stable-20260808-colorectal-payoff` peeled ref
  resolves to `5168d3b` exactly, both locally and via `git ls-remote`.
- CI: last-five on `main` at session start disclosed above (five
  green, no red window); this session's own push watched to
  conclusion, run `31282107053`, `success`, 3m27s.

### Successor tag debt, recorded here

**The next session that opens fresh work tags
`stable-20260808-pairing-registry` at THIS session's own closing tip**
— the same tag-law case (ii) pattern every prior close in this repo
has used for its own predecessor.

### Index line

```
- 2026-08-08 — pairing-registry — ADR-0088
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 85→86, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

The pairing-as-data design pass (review P3-3, ADR-0050 D-3) lands its
first executable registry: seven witnessed rows, two v2 judges,
zero FHIR rows (deliberately, AR-PD-2), a report-only coverage table
naming exactly which cells remain open. Untouched, carried forward
from ADR-0087's own horizon note: the carry-across emission row's own
compile-layer half, Wave E's risk-attribute/vital-sign register,
vendoring batch 4 (the veteran family), the census closure-count
refinement, publish-prep (F-5/F-6 + F-7), review 2, `sim-emit-cda`,
the fixture-relocation and ADR-footnote Next rows, and the sleep-apnea
latent-defect intake named for review 2. New: FHIR rows and the
tier-two-to-gate promotion both wait on the storefront-fixture
session's own acceptance-proof rows.

### Consequence

The mutate↔judge pairing — inject defect class X, expect finding class
X — is now DATA, checked by execution: a future engine upgrade or
catalog edit that silently changes what a mutation actually convicts
breaks a committed test, not a stale sentence in an operator's own
`:contract` prose. The three skipped NIST pairs are the session's own
honest register of what this tier genuinely cannot witness yet (two
parser-level rejections with no finding-level class, one profile-noise-
masked mutation) — recorded as findings about the fixture/engine
combination, not smoothed into a false completeness the catalog's own
prose does not claim either.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

The pairing registry: inject X, expect X, on the record — the pairing-as-data design pass (review P3-3, ADR-0050 D-3) lands its first executable registry, seven witnessed rows across two v2 judges (five judge-v2-hapi, two judge-v2-nist), zero FHIR rows by design; a names-only NIST taxonomy snapshot derived from the resolved jar's own `reference.conf`, gated by a currency test; three skipped NIST pairs honestly named (two parser-level rejections with no finding-level class, one masked by baseline profile noise); a report-only tier-two coverage table; all 29 oracle roots byte-identical
