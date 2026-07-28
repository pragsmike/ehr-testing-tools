Prompt: nightly drift check on the vendored palgebra copies

Context. `ehr-testing-sim` now vendors four palgebra files pinned to tools@7ecce38 (sim's ADR-0016). Tools is the repo allowed to know about sim (sim ADR-0001), so the drift guard lives here: the existing nightly integration workflow already exercises sim as a consumer; this adds a byte-diff between tools' authoritative palgebra copies and sim's vendored ones.

Read first: the nightly integration workflow under `.github/workflows/` (the one running the cross-repo consumer loop); `palgebra/tools/resource_equations_to_mermaid.py` and `palgebra/examples/{ai-study,lemon-pie,decision-monad}-equations.txt`; sim's `notes/ADRs.md` ADR-0016 (in the workflow's checkout of sim).

Author rulings.
1. Diff ignores provenance headers only. Sim's copies carry a prepended provenance comment block; the comparison strips exactly that block (or compares from the first non-provenance line) — no fuzzier normalization. Everything else must be byte-identical.
2. Failure names the remedy. The failing step's message says the files have drifted, names sim ADR-0016, and states the fix is a re-vendor session in sim (or a deliberate divergence ADR), never a silent edit on either side.
3. This is a check in the existing nightly workflow, not a new workflow.

Step 1 — Add the drift-check step. In the nightly workflow, after the sim checkout step, add a step that for each of the four file pairs strips sim's provenance header and diffs against tools' copy; nonzero diff fails the job with ruling 2's message. Keep it as a small shell step or a script under `bin/`/`scripts/` per this repo's existing convention — follow whichever the workflow already uses.
Commit: `ci(nightly): drift check — palgebra copies vendored in ehr-testing-sim vs. authoritative copies here (sim ADR-0016)`

Step 2 — Prove it fires. Run the check locally (or via workflow_dispatch) twice: once as-is (expect pass), once with a one-character local mutation to a tools-side example file (expect fail with the ruling-2 message), then revert the mutation. Record both probe results in the final report.

Step 3 — Archive this prompt. Per this repo's prompt-archiving convention, with deviation appendix if any.
Commit: `prompts: archive 2026-07-27 palgebra drift-check session`

Final report: workflow step location, both probe results (pass and induced fail), deviations if any.

---

## Session deviation record (added post-execution, per AUTHORS-GUIDE.md section 7)

- **No sim checkout step existed to add this step "after."** The prompt's Step 1 assumed the nightly workflow already checks out `ehr-testing-sim`; it did not — the workflow's own header comment explicitly recorded that the four `sim_*_test.clj` tests clean-skip every night because "this runner has no sibling checkout... cloning a private sibling repo with credentials this runner isn't given," and that a future session wanting real coverage would add that step deliberately. This session is that future session, but only for the drift check, not to newly exercise those four tests — whether they should now run for real is left as a separate, undecided call, noted inline in `integration.yml`'s own comment.
- **The credential concern in that old comment no longer applies.** `ehr-testing-sim` went public 2026-07-27 (that repo's own ADR-0015, same date as this session), so the added checkout step is a plain unauthenticated `git clone` of a public HTTPS URL — no deploy key or token was created, requested, or needed. This was verified by reading sim's ADR-0015 directly (not assumed) before writing the checkout step.
- **The sibling-checkout path was not invented new.** `../ehr-testing-sim`, the path the added checkout step clones into and the path `bin/check-palgebra-drift` defaults to, is the exact path `test-integration/ehr_testing_tools/sim_harness.clj`'s `sim-repo-dir` already uses — reused, not a second convention for the same relationship.
- **A real bug was caught and fixed before this landed, not silently.** The first version of `bin/check-palgebra-drift` applied the provenance-header-stripping function to BOTH sides of each comparison — including this repo's own authoritative files, which carry no such header. For the three `.txt` example pairs this produced an honest failure (caught immediately, by design). For the `.py` pair it produced a **false pass**: the script's `.py`-header stripper matched on any line that was purely `#` + dashes, which also matches several pre-existing, unrelated section-delimiter comments already inside `resource_equations_to_mermaid.py` (e.g. around "Data model", "Parser"); applied symmetrically to two files whose bodies are otherwise identical, the bug happened to strip a matching amount from both sides and still report a match. Caught by testing against the real sibling checkout (Step 2's own "prove it fires" instruction) before committing, not assumed correct from code review alone. Fixed two ways: (1) the strip function is now applied only to sim's copy, never to this repo's own authoritative file; (2) the `.py` stripper was rewritten to key on the two literal anchor lines ADR-0016's provenance header fixes the wording of (`# Vendored copy.` / `# Vendored per ADR-0016.`) rather than on the shape of any dashed comment line. The fix was independently verified with `md5sum` on the stripped output, not just a clean script exit, before trusting the "OK" result.
- **The executable bit was lost between `chmod` and commit, then recovered.** `bin/check-palgebra-drift` was created with `chmod +x` set, but the commit still recorded mode `100644`. Root cause: this repo's `core.fileMode` is `false`, so a plain `git add` ignores filesystem permission changes entirely — `bin/quickstart-demo`'s own `100755` mode in the index predates that setting or was set explicitly. Fixed with `git update-index --chmod=+x` (which bypasses the `core.fileMode` ignore) in a follow-up commit, verified against `git ls-files -s` afterward.
