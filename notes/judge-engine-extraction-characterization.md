# Judge engine extraction: characterization baseline (2026-07-29)

Step 3 of the judge-v2-hapi/judge-fhir-official extraction session
(`notes/prompts/2026-07-29-judge-engine-extraction.md`). Captured
*before* any move, per ADR-0009's own disclosure that no committed
`gate`/`check` baseline already existed in this repo -- this is this
session's own baseline, not a regeneration of a prior one. Re-run
verbatim at the session's own Step 5 and diffed byte-for-byte against
what's recorded here.

## Fixture set

Small and reproducible from committed content plus one inline literal:

- **v2**: two already-committed fixtures, unmodified --
  `components/tools/test-fixtures/v2/adt-a01-admit.hl7` and
  `components/tools/test-fixtures/v2/adt-a02-transfer.hl7` -- copied
  into a scratch directory (`$SCRATCH/char-v2/`, this session's own
  scratchpad, not repo-tracked).
- **fhir**: one minimal, inline Patient resource, not committed
  anywhere else, reproduced here verbatim so this baseline doesn't
  depend on the scratchpad surviving:

  ```json
  {"resourceType":"Patient","id":"example","name":[{"family":"Doe","given":["Jane"]}],"gender":"female","birthDate":"1980-01-01"}
  ```

  Written to `$SCRATCH/char-fhir/patient1.json`.
- **check**: the same two v2 fixtures above, copied identically into
  `$SCRATCH/char-check-cand/` and `$SCRATCH/char-check-exp/` (candidate
  == expected, the trivial matches-expected-and-passes case) --
  `check` never touches the v2/fhir gate engines directly (it only
  calls `judge/build-report`, which stays in `components/judge`
  unmoved), so this is a low-stakes command to characterize, included
  anyway per the session's own zero-behavior-change ruling.

## Commands and exit codes

```
./bin/ehrt gate v2 "$SCRATCH/char-v2" --report "$SCRATCH/baseline/v2-report.edn"
./bin/ehrt gate fhir "$SCRATCH/char-fhir" --report "$SCRATCH/baseline/fhir-report.edn" --out-dir "$SCRATCH/char-fhir-out"
./bin/ehrt check "$SCRATCH/char-check-cand" --expected "$SCRATCH/char-check-exp" --report "$SCRATCH/baseline/check-report.edn"
```

All three: `EXIT:0`.

## stdout, verbatim (the `--report` file's own content is the same EDN map, minus the `{:status :ok :payload ...}` wrapper -- the CLI prints the whole result, `--report` writes just `:payload`)

`gate v2`:

```
{:status :ok, :payload {:run {:gate :v2, :path "$SCRATCH/char-v2"}, :totals {:pass 2, :rejected 0, :indeterminate 0, :no-verdict 0}, :by-code {}, :files [{:path "$SCRATCH/char-v2/adt-a01-admit.hl7", :verdict :pass, :finding-count 0, :findings []} {:path "$SCRATCH/char-v2/adt-a02-transfer.hl7", :verdict :pass, :finding-count 0, :findings []}]}}
```

`gate fhir`:

```
{:status :ok, :payload {:run {:gate :fhir, :path "$SCRATCH/char-fhir"}, :totals {:pass 1, :rejected 0, :indeterminate 0, :no-verdict 0}, :by-code {"invariant" 1}, :files [{:path "$SCRATCH/char-fhir/patient1.json", :verdict :pass, :finding-count 1, :findings [{:severity :warning, :code "invariant", :locator {:format :fhir, :path "Patient"}, :message "Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)", :engine {:name "fhir-validator-cli", :version "6.9.12"}, :disposition :pass, :native-ref {:expression ["Patient"]}}]}]}}
```

`check`:

```
{:status :ok, :payload {:run {:check {:name "check", :version "v1"}, :candidate-dir "$SCRATCH/char-check-cand", :expected-dir "$SCRATCH/char-check-exp", :assertions [{:kind :matches-expected}], :pair-by :path, :canonicalizers []}, :totals {:pass 2, :rejected 0, :indeterminate 0, :no-verdict 0}, :by-code {}, :files [{:path "adt-a01-admit.hl7", :verdict :pass, :finding-count 0, :findings []} {:path "adt-a02-transfer.hl7", :verdict :pass, :finding-count 0, :findings []}]}}
```

(`$SCRATCH` is this session's own scratchpad path, literal and
identical between the baseline and the Step-5 re-run since both run
inside the same session -- the exact string is irrelevant to the
comparison, which is about whether the two runs produce the same
bytes given the same input, not about the absolute path itself.)

## Report-file checksums (SHA-256, this session's own scratchpad copies)

```
41a7c7ef8cd99c0061589dbb3572fa1e3fd87fd4f09ab908c4065f391fbcd951  check-report.edn
6627655d137f79573737cb995b8ffed9e7416b5f420852e6fc90ef2d7fce1ac5  fhir-report.edn
1e7ad5c2d1e390a607affe115d98122eee419e75cf269f4fbd1a840f594343a4  v2-report.edn
302818d1d46b6226ee14c258b2ffc46fc3748653522ee3e990538ba0cb343ae8  check-stdout.log
7d2d1b456853b6e262c99d6f0700fb92627ab2d9b95263b5d56923b9f2ad253a  fhir-stdout.log
411dd0c2741c2660ae93eef479c0860d229bd1c2429f5ff816fbce4736433d04  v2-stdout.log
```

## Census (namespace -> real callers -> destination)

Every real `:require`/`:import` of `ehrt.judge.*` across `src` + `test`
+ every project, grepped whole-tree (not prose):

| Namespace | Real callers | Disposition |
|---|---|---|
| `ehrt.judge.v2` | `ehrt.judge.interface` only | -> `ehrt.judge-v2-hapi.v2` (component `judge-v2-hapi`) |
| `ehrt.judge.fhir` | `ehrt.judge.interface` only | -> `ehrt.judge-fhir-official.fhir` (component `judge-fhir-official`) |
| `ehrt.judge.finding` | `ehrt.judge.v2`, `ehrt.judge.fhir`, `ehrt.judge.report`, `ehrt.judge.interface` | stays in `judge` (vocabulary) |
| `ehrt.judge.report` | `ehrt.judge.interface` only | stays in `judge` (vocabulary) |
| `ehrt.judge.verdict-cache` | `ehrt.judge.fhir` only (**single consumer today** -- disclosed, see ADR-0011) | stays in `judge` per the session's own ruling (keys generically on engine name/version + input hash; the planned NIST v2 engine is its expected second consumer) |
| `ehrt.judge.interface` | `ehrt.tools.interface`, `ehrt.tools.check`, `components/tools/test/ehrt/tools/v2_contract_pairing_test.clj`, `components/tools/test/ehrt/tools/check_test.clj` | narrows to vocabulary-only re-exports (`Report`/`build-report`/`diff-reports`/`baseline-relative-report`/`report-valid?`/`finding-valid?`); gate re-exports move to the two new interfaces |

No engine-to-engine `:require` found in either direction (`ehrt.judge.v2`
does not require `ehrt.judge.fhir` or vice versa) -- confirms the
session prompt's own "structurally clean" premise rather than
contradicting it. `ehrt.judge.verdict-cache` IS consumed by only one
engine today, contradicting nothing in the [C] ruling (the ruling
itself already named and accepted this as a disclosed fact, not an
assumption to verify away).

Every downstream consumer of the gate functions (`bases/cli/src/ehrt/cli/core.clj`,
`projects/integration/test/ehrt/tools/contract_pairing_test.clj`) goes
through `ehrt.tools.interface` only, never `ehrt.judge.interface`
directly -- so `ehrt.tools.interface`'s own re-exported names
(`v2-gate-file`, `v2-gate-dir`, `fhir-gate-file`, `fhir-gate-dir`,
`fhir-gate-batch`) are the zero-behavior-change contract surface that
matters for the CLI; nothing downstream of `ehrt.tools.interface`
changes name or shape.
