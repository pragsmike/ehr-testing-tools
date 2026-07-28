# CAL-1 — v2 blind-spot calibration: P7's dropped candidates become data

You are working in `ehr-testing-tools` (public). This is a short
documentation-and-register session, no production code: P7's
empirical probing established that `judge.v2`'s base-structural tier
does not convict three plausible defect classes (dropping a required
segment; corrupting PID's segment name; blanking a non-header
required field — all probed `:pass`, recorded in
`corpus/operators.clj`'s docstring). That finding currently lives
only in an operator-namespace docstring. This session moves it to
where calibration readers and the EXP-D3 adoption case will actually
find it, and registers the underlying HAPI capability claim as an
evidence-backed fact.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`,
`src/ehr_testing_tools/corpus/operators.clj` (the v2-scope docstring
paragraph — this session's source material),
`src/ehr_testing_tools/judge/v2.clj` (its own tier description),
`docs/judge-calibration.md` (structure and voice — match it),
`docs/experiments.md` (the EXP-D3 row), `notes/facts-register.md`
(row format and evidence discipline),
`test/ehr_testing_tools/v2_contract_pairing_test.clj` (the probe's
surviving evidence). Ritual: commit → `git push origin`. Save this
prompt to `.agents/prompts/2026-07-XX-cal1-v2-blindspots.md`; final
commit archives it.

Author rulings in effect: **documentation and register only** — no
operator, judge, or test changes; if you find yourself editing a
`.clj` file for anything beyond a docstring cross-reference, stop.
**Evidence over memory** — every claim written cites either the P7
probe (reproducible: the dropped candidates' definitions are in git
history at the P7 session's commits) or HAPI's own
documentation/source.

## Step 1 — The facts-register row

Add the F-row for the tool-capability claim underneath the finding:
HAPI HL7v2's `defaultValidation` context (as `judge.v2` configures
it) does not enforce segment presence or field
cardinality/requiredness — its validation at this tier is
primitive-type and parse-level checking; segment requiredness is
conformance-profile knowledge, outside the default context.
Evidence: (a) the P7 empirical probe (three candidates → `:pass`,
commits referenced by hash), (b) HAPI HL7v2's own
documentation/source for `ValidationContextFactory.defaultValidation`
— read it, link the specific class/docs, and state what it actually
checks rather than paraphrasing the probe backwards into the docs.
Last-verified: today. If the HAPI source reveals nuance the probe
didn't (e.g. some cardinality IS checked in some message types),
record the nuance — the row must survive an adversarial reader.

Commit: `Facts-register: HAPI defaultValidation tier limits
(evidence: P7 probe + HAPI source)`.

## Step 2 — judge-calibration.md gains the v2 section

A section in the existing doc's voice: what the base-structural v2
tier convicts (the five shipped operators name the classes) and what
it demonstrably cannot (the three dropped candidates), framed as
measured calibration — a consumer of `judge.v2` verdicts should know
that a structurally clean `:pass` says nothing about segment
presence or field requiredness. Cite the F-row from Step 1 and the
pairing suite. Cross-reference both directions:
`corpus/operators.clj`'s docstring paragraph gains one line pointing
at the calibration section (docstring edit only — permitted), and
the calibration section points back at the docstring as the
catalog-side record.

Commit: `judge-calibration: v2 tier blind spots documented as
measured calibration data`.

## Step 3 — EXP-D3 gains its named customers

`docs/experiments.md`, the EXP-D3 row: append the demand-side case —
a profile-aware v2 judge (the NIST/CDC engine this experiment
adopts) has three concrete, already-written customers: the dropped
operators graduate from the operators.clj dropped-list to the
registered catalog the day a resident judge can convict them, and
the v2 contract-pairing suite extends to pair them. State it as the
experiment's acceptance criterion made concrete: adoption succeeds
when the dropped candidates convict. One or two sentences in the
row's style; do not restructure the row or touch its
license-contingency framing (F1/F15 status is unchanged).

Commit: `EXP-D3: adoption case gains three named customers (P7's
dropped operators)`.

## Step 4 — Close out

Full suite + both lints green (nothing should have changed them);
`corpus-foundations.md`: one line in the P7 row's summary noting the
blind-spot finding is now calibrated documentation (CAL-1). Archive
this prompt.

Commit: `CAL-1 complete: v2 blind spots are calibration data, EXP-D3
has customers (archives prompt)`.
