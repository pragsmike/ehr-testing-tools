# The `:dispense` key derives the inpatient dispense, after a drawn pharmacy turnaround set per ward (ADR-0189)

2026-11-19. Base `046e611c`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 1 on one finding, reported and not a
premise of this session: among the last five CI runs, the `Integration`
workflow at `164fae67` is red. That is the stale 46-invariant pin the
2026-11-18 record describes, fixed in `f8df1749`; the `test` workflow
is green at `046e611c`.

Prompt: `.agents/prompts/2026-11-19-inpatient-dispense-derived.md`.

## Steps 1 and 2: red tests, then the build

- `05e47820` the config half: a ward's optional `:dispense-minutes`,
  `default-dispense-minutes` (`:ed [5 25]`, `:inpatient [15 60]`,
  provisional) and the one reader, in `sim-model`.
- `bf24dfcc` the red tests: 16 failing assertions in
  `inpatient_dispense_derivation_test`, all because nothing was
  derived. Green by construction there, as that commit says: (c) both
  cases, (d), and (e)'s equality.
- `18a7f84f` the build. `poly check` OK; `brick:sim-engine` 60
  namespaces, 14,202 passes, 0 failures; `brick:sim-check` 12
  namespaces, 1,230 passes, 0 failures.

**The draw site.** The stream scheme did offer one that moves no other
draw: a family of its own. `:pharmacy`, tag 7, keyed by arrival ordinal
as `:patient` is, built only under the key. No existing stream's seed
changed and the scheme marker stays "1.0". ADR-0187's erasure test is
green for the whole key and was not edited, its stays-under-churn case
included, which now erases inpatient dispenses as well.

## Step 3: the prediction, written before either instrument ran

**Oracle roots that will DIFFER: none.** One root sets the key,
`osteoporosis-dispense`. Its module's only Encounter state is a
wellness encounter, which compiles to an outpatient visit, so no order
in that root is written during an admission. The `dispense` root
authors its dispenses and does not set the key. Every other root does
not set the key, and without the key no code path differs. Both
instruments: all roots IDENTICAL.

**Gated corpora that will move:** the three `dense-7500` configs that
set the key (`config.edn`, `config-nobed.edn`, `config-28024.edn`), at
every cell. `dense-inpatient` writes two orders per stay.

**Gated corpora that will not move:** `config-bare.edn`; the four arc-0
pins (two `ed-tuesday`/`clinic-decade` seeds each side of `adhd`); the
`ed-tuesday` and `clinic-decade` scenarios. None sets the key.
