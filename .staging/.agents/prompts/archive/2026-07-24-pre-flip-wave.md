# Pre-flip wave — history rewrite, README opening, inquiry-draft eviction

You are working in `ehr-testing-tools`. The repo is NOT yet public — this session must complete before the author flips visibility, because it rewrites git history. Three jobs: (1) redact republished personal email addresses and expunge them (and the NIST inquiry draft) from all history; (2) give the README the missing "why should I care" opening; (3) close the push gap in the session ritual. Commits from WSL; `make test` green before and after. Save this prompt to `.agents/prompts/2026-07-24-pre-flip-wave.md`; final commit archives it.

Safety first: before ANY rewrite, create a backup: `git bundle create ~/ehr-testing-tools-backup-$(date +%Y%m%d).bundle --all` and verify the bundle is readable. Confirm the working tree is clean. Also confirm origin state (`git fetch origin && git status`) — the author reports local is ahead of origin; that's expected and fine (origin will be force-pushed at the end; nothing on origin is ahead of local — verify that specifically, and stop if it is).

## Step 1 — Content redaction (the surviving version)

In `docs/research/License Status of NIST HL7 v2 Validation Software…` (find exact filename): replace the contributor-email lists in the authorship section with domain-level phrasing carrying the same evidentiary weight, e.g.: "all identifiable commit authors on `v2-validation` used `@nist.gov` addresses (four distinct authors); all identifiable commit authors on `hl7-igamt` likewise used `@nist.gov` addresses (three distinct authors), with one author also appearing under a personal address — consistent with one individual committing from two identities rather than an outside contributor." No individual addresses remain anywhere in the file (robert.snelick@nist.gov elsewhere in the repo is the intended public contact and stays). Add a dated note at the top of the report: "Redaction 2026-07-24: individual contributor email addresses replaced with domain-level descriptions; the underlying evidence (public commit metadata) is unchanged and re-derivable." Check the facts register and components.md for any copied addresses (expected: none beyond Snelick's) — fix if found.

Commit: `Redact contributor email addresses from research report (dated note)`.

## Step 2 — Evict the inquiry draft to a secret gist

1. Create a secret gist containing the current `docs/experiments/EXP-SBOM-inquiry-draft.md` (`gh gist create --desc "NIST licensing inquiry draft (private)"`). Report the gist URL in your final report ONLY — it must not appear in any committed file (a secret-gist URL in a public repo is not secret).
2. `git rm` the draft from the repo. Update every reference: `docs/experiments.md`'s EXP-SBOM row, the EXP-SBOM results file's pointer if any, facts-register F1's wording — each becomes "inquiry draft maintained privately by the author" (no URL).
3. Commit: `Move NIST inquiry draft out of repo (maintained privately)`.

## Step 3 — History rewrite

1. Pre-check: grep all committed files for references to commit SHAs (7–40 hex strings that resolve via `git cat-file -t`) — archived prompts, plan file, docs. Report any found; none are expected. If any exist, note that they will dangle and flag rather than fix.
2. Install `git-filter-repo` user-locally if absent (`pip install --user git-filter-repo` or pipx; report which).
3. Run ONE filter pass over the full history combining:
   * `--replace-text` with an expressions file mapping each of the seven contributor addresses (the six `@nist.gov` individuals' addresses and the one personal Gmail — take them from the pre-redaction blob, handle them only inside the filter run, never write them to a committed file or the report) to `[redacted-contributor-address]`.
   * `--invert-paths --path docs/experiments/EXP-SBOM-inquiry-draft.md` so the draft never existed in history.
4. Verify the rewrite: `git log --all -S "@gmail.com" --oneline` returns nothing relevant; a full-history grep (`git grep <fragment> $(git rev-list --all)` spot checks) finds no contributor addresses and no inquiry-draft blob; `make test` green; the working tree's redacted report reads correctly; HEAD history still contains all session commits with rewritten SHAs.
5. Re-point origin if filter-repo removed the remote (it does by design): `git remote add origin <ssh url>` and `git push --force --all origin && git push --force --tags origin`. Verify `git status` shows up-to-date with origin/main.

No separate commit — this step rewrites; the report documents it.

## Step 4 — Packs repo reset

The public `pragsmike/packs` history contains pre-redaction packs. In `~/.packs`: regenerate is not needed yet (Step 6's pack-push will add the fresh pack) — first reset history: create an orphan branch, commit the CURRENT files only after replacing both pack files with the freshly generated post-rewrite packs (run `make pack` and `make pack-skills` after Step 5's README work, or defer this whole step until after Step 5 — sequencing note: do Step 4 LAST, folded into Step 6, so the squashed single commit contains only final packs). Force-push main. Result: packs repo = one commit, current packs only, no email-bearing history.

Also overwrite the retired gist (`4fcd1abb…`)'s pack file content with a one-line stub: "Retired transport — packs now at github.com/pragsmike/packs" (its revision history persists unlisted; noted as accepted residual).

## Step 5 — README opening: the why

The README currently launches into mechanics. Prepend a proper opening (2–4 short paragraphs before the pipeline diagram), covering — in the repo's plain engineer-to-engineer voice, no marketing:

1. The problem: testing EHR integrations requires realistic clinical test data at volume, deliberately broken variants of it, and conformance gates — and teams typically hand-roll all three, badly, per project. One or two sentences.
2. What this offers: reproducible synthetic patient corpora (byte-identical regeneration from a manifest — proven, cite EXP-A4), controlled defect injection with full lineage (every mutant traceable to its base, operator, and the constraint it violates), conformance gating (planned; link the plan). Concrete capabilities, present tense for what exists, honest labels for what doesn't.
3. Who it's for: practitioners testing EHR integrations — interface analysts, QA engineers, data engineers — comfortable working alongside AI assistants; no Clojure knowledge needed to use the outputs (plain FHIR JSON + EDN manifests), and SETUP.md includes a copy-paste prompt so your assistant can handle installation. (Describes the cohort without naming it.)
4. Who maintains it and why: maintained by the author of the ehr-testing-guide (link) as the operational companion to that method — the guide explains why these capabilities belong in a test plan; this repo makes them runnable. Pre-release; interfaces may move; the maturity table below is the contract.

Then the existing content (diagram, maturity table, quickstart) follows, trimmed of anything the new opening now says. Keep total README length reasonable — the opening earns attention; it doesn't double the file.

Commit: `README: problem, capabilities, audience, maintainer opening`.

## Step 6 — Ritual fix and finalize

1. AUTHORS-GUIDE session ritual amendment: sessions end with commit → `git push origin` → `make pack-push` — the repo push was previously manual and commits accumulated unpushed; now it's part of the ritual. Add the same line to AGENTS.md's maintainer section.
2. `make test` green; archive this prompt; final commit; push origin (verify up-to-date); regenerate both packs; execute Step 4's packs squash with the fresh packs; verify the raw fetch shows the new single-commit packs repo with final HEAD and clean tree.
3. Report: backup bundle path; SHA-reference pre-check result; filter-repo verification evidence (the searches and their empty results); old HEAD → new HEAD mapping for this session's tip; the secret gist URL (report only); README opening as written (verbatim — the author reviews it before flipping); packs-repo reset confirmation; commits.

## Out of scope

No visibility flip (author's click, after review). No capability code. No edits to other dated records beyond the redaction note mechanism. No CI changes. Do not write the seven addresses into any committed file, the report, or the pack — they exist only inside the filter run.
