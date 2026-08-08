# 2026-08-08 — Pairing registry: inject X, expect X, on the record

## Scope

Session prompt naming AR-PD-0 through AR-PD-6, executing the roadmap's
own Next-section row "Pairing-as-data (review P3-3): mutate↔judge
conviction registry" — the design pass (four shape questions) ruled by
the author 2026-08-08 in the design channel, "Approve recommendations"
over the four numbered recommendations quoted verbatim in the driving
prompt. This session builds and lands the registry itself: a committed
EDN resource of witnessed operator×judge rows, `ehrt.judge.pairing`
(schema/loader/coverage), a NIST finding-vocabulary taxonomy snapshot
with its own currency gate, and a tier-one executable test that closes
the inject-X-expect-X loop by actually running it.

Preflight: working directory confirmed the ext4 clone
(`~/src/ehr-testing-tools`), HEAD `5168d3b` exactly (the colorectal
payoff, ADR-0087), branch up to date with `origin/main`, working tree
clean, no untracked files. `clojure -M:poly check`: OK. Oracle
self-bracket (`bin/regression-oracle 5168d3b 5168d3b`): all 29 roots
IDENTICAL, byte-for-byte, both sides the same commit. Last five CI runs
on `main` all `success`: `31276965870` (`5168d3b`), `31276534167`,
`31276085600`, `31274667607`, `31274259259` — no red window.

## Step 0 — Tag (AR-PD-0)

`stable-20260808-colorectal-payoff` did not already exist locally or on
the remote. Created annotated at `5168d3b`, message "stable-20260808-
colorectal-payoff at 5168d3b (colorectal payoff close, ADR-0087)";
pushed; peeled ref verified — resolves exactly to `5168d3b`. No commit
this step, per the prompt.

## Step 1 — Prior art + measurement (no commit)

**Prior art located:** `ehrt.judge.finding`'s own `Severity` docstring
(`components/judge/src/ehrt/judge/finding.clj:27-36`) names "P5's
contract-pairing exercise." P5 itself:
`notes/tools/prompts/2026-07-24-p5-gates.md`; its FHIR exemplar,
`projects/integration/test/ehrt/integration/contract_pairing_test.clj`;
its v2 twin, `components/corpus/test/ehrt/corpus/v2_contract_pairing_test.clj`
(P7). Also read: `.agents/rulings.md`'s own alignment-arc entry (ADR-0050
AR-F1-6/D-3, the registry's own landing-spot ruling).

**judge-v2-hapi measured** against all three committed ADT fixtures,
all five v2 operators: identical outcome on every fixture, and
identical to `v2_contract_pairing_test.clj`'s own pinned codes — no
catalog contradiction. Full table in `notes/adr/0088-pairing-
registry.md`'s own Measurement section.

**judge-v2-nist measured** against the covidELR fixture (Π
`COVID19_ELR-v2.3.1`), all five v2 operators: two witnessed
(`:blank-required-field`, `:truncate-segment-fields`, both convicting a
NEW `structure/Usage` category), three skipped and named — two
engine-level `check-exception`s with zero findings
(`:corrupt-encoding-characters`, `:corrupt-segment-name`), one
(`:malformed-datetime-value`) masked by the baseline's own 473-finding,
already-`:profile-spec-error` noise floor. None a catalog contradiction
(AR-PD-6's STOP-AND-REPORT trigger did not fire).

**Taxonomy derived** from the resolved `gov.nist:hl7-v2-validation:1.7.3`
jar's own `reference.conf`, via `com.typesafe.config.ConfigFactory`
(already a transitive dependency): 7 classifications, 52 categories,
extracted and committed verbatim.

## Step 2 — The registry lands, commit `948f5e5`

**Landed together** (co-landed invariants law): `ehrt.judge.pairing`
(`PairingRow`/`Registry` malli schema, `load-registry`, `coverage`);
the committed registry EDN (7 rows, `components/judge/resources/judge/
pairing-registry.edn`); the NIST taxonomy snapshot
(`components/judge-v2-nist/resources/judge-v2-nist/taxonomy.edn`); the
currency-gate test (`taxonomy_currency_test.clj`, two deftests); the
tier-one per-row conviction test (`pairing_conviction_test.clj`, one
deftest, 7 sub-assertions); a small unit-test file for the loader/
coverage fn (`pairing_test.clj`). `ehrt.judge.interface` gained five new
re-exports, no collision. `judge-v2-nist/deps.edn` gained `"resources"`
on its own `:paths` (its first resources directory).

**Row-shape disclosure, not silent:** the driving prompt's own stated
row shape carried no locator field, and every v2 operator is
`:locator-required? true` with no `:default-locator` populated — a row
without its own replay locator cannot be replayed by the tier-one
test. Two fields extend the stated shape, both documented in the
resource's own header comment and `PairingRow`'s own docstring:
`:locator` (every row) and `:profile` (optional, `judge-v2-nist` rows
only — the conformance-profile bundle directory `make-validator`
needs).

**Red→green witnessed directly, in-session, for all three new gates,
before commit:**
- Currency-gate test: corrupted one committed category name
  (`"Usage"` → `"Usage-CORRUPTED"`) in the resource file — both
  deftests failed (3 assertion failures: the drift check, and the
  registry-cross-check that now saw an unknown category); restored
  byte-identical (diffed against a pre-edit backup); re-ran green.
- Tier-one conviction test: corrupted one row's `:expected` set to a
  nonsense code in the registry EDN — 3 of 7 rows failed, each failure
  message naming the actual observed class; restored byte-identical;
  re-ran green.

**Full suite** (`clojure -M:poly test :all skip:integration`): green
throughout — 566 project-block "0 failures, 0 errors" confirmations
across the full run output (grepped, not sampled), exit code 0, 3m00s
elapsed. Both new tiers' own tests confirmed present and green in both
project groupings that include them, the standard doubled appearance
this workspace's own project composition produces. `clojure -M:poly
check`: OK. `gitleaks git --staged -v`: clean. Staging hygiene:
`git diff --cached --stat` reviewed — exactly the eight intended files,
nothing else staged.

**Oracle bracket** (`bin/regression-oracle 5168d3b 948f5e5`): all 29
roots IDENTICAL, byte-for-byte — this session touches no
sim/compile/engine/emitter path, exactly as predicted.

Committed `948f5e5` ("feat: the pairing registry lands — inject X,
expect X, on the record (pairing-as-data, AR-PD-1/2/3/4)"). Pushed;
post-push verification: one delta, the known trailing-blank-line
artifact. CI watched to conclusion: run `31282107053`, `success`,
3m27s.

## Step 3 — Record, commit `939e201`

`notes/adr/0088-pairing-registry.md` authored in full (both measurement
tables, the skipped-pairs disclosure, the taxonomy derivation, the
tier-two coverage table, the NIST-licensing-inquiry note left open
rather than assumed settled). `notes/ADRs.md` gained its index line.
`notes/adr/README.md`'s own file count corrected 85→86 (`ls
notes/adr/*.md | grep -v README | wc -l`, not arithmetic). Roadmap: the
"Pairing-as-data" Next-section row removed (executed by this session);
the Done pointer (`- 2026-08-08 — pairing-registry — ADR-0088`) added;
the Storefront-demo-fixture Next row gained a cross-reference naming it
as the future landing spot for the registry's own FHIR rows and the
tier-two-to-gate promotion (AR-PD-2/AR-PD-4's own deferrals).

`clojure -M:poly check`: OK. `ehrt.docs-tooling.index-completeness-test`,
`ehrt.docs-tooling.readme-presence-test`, and
`ehrt.docs-tooling.done-pointer-adr-test` re-run green before staging.
`git diff --cached --stat` reviewed: exactly the four intended files
(`roadmap.md`, `ADRs.md`, `README.md`, the new ADR file). `gitleaks
git --staged -v`: clean.

Committed `939e201` ("docs: the pairing registry recorded — the carry
ends at two closes, witnessed rows only (ADR-0088)"). Pushed; post-push
verification: one delta, the known trailing-blank-line artifact. CI
watched to conclusion: run `31282341319`, `success`, 3m34s.

## Step 4 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-pairing-registry.md` (the
driving prompt, archived verbatim) land together, indexed in both
READMEs' own entry lists.

## Successor tag debt

Recorded in `notes/adr/0088-pairing-registry.md`: the next session that
opens fresh work tags `stable-20260808-pairing-registry` at this
session's own closing tip.

## Judgment calls and their ratification status

- **The two row-shape additions, `:locator` and `:profile` (AR-PD-1).**
  Not separately ratified — the driving prompt's own stated shape did
  not name a replay locator or a NIST profile-bundle path, and neither
  is optional in practice (every v2 operator requires a locator; every
  `judge-v2-nist` gate call requires a profile bundle). Disclosed in
  the ADR's own Decision section under AR-PD-1, in the committed
  resource's own header comment, and in `PairingRow`'s own docstring —
  not silently added.
- **Which finding field counts as a NIST "class" (`:category`, not the
  full `:code`).** `judge-v2-nist`'s finding `:code` is `"area/category"`
  (e.g. `"structure/Usage"`); the taxonomy snapshot only names
  categories (and classifications), not the area prefix. `:expected`
  values are therefore bare category strings (`"Usage"`), checked
  against `(:category (:native-ref finding))` rather than the full
  `:code` — the schema-level cross-check in
  `taxonomy_currency_test.clj` would otherwise have nothing to validate
  against (no "area" vocabulary exists in `reference.conf`).
- **Fixture choice for the registered HAPI rows.** All five operators
  produced byte-identical outcomes across all three committed ADT
  fixtures; `adt-a01-admit.hl7` was registered as the canonical
  fixture (matching the existing corpus suite's own choice) rather than
  registering three redundant rows per operator — the trio-wide
  identical result is disclosed in the ADR's own Measurement section,
  not silently discarded.

## Findings, disclosed not acted

- **Three NIST pairs genuinely un-witnessable at the finding-class
  grain, on this fixture, at this tier** — named individually in
  `notes/adr/0088-pairing-registry.md`'s own Measurement section, not
  forced into rows: two engine-level parser rejections carrying zero
  findings, one mutation whose own effect is masked by this profile
  bundle's own pre-existing 473-finding defect-noise floor (the
  bundle deliberately ships without `VALUESETBINDINGS.xml`/
  `COCONSTRAINTS.xml`/`SLICINGS.xml`, per `test-fixtures/v2-nist/
  NOTICE.md`). None is a contradiction of the operator catalog's own
  `:contract` prose (AR-PD-5's STOP-AND-REPORT trigger did not fire) —
  each is a fixture/engine-combination finding, named for whichever
  future session builds a second NIST fixture with a cleaner profile.

## HEAD landed

`939e201` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).
