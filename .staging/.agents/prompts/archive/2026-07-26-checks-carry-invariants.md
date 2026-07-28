AUTHORS-GUIDE §7 — verification checks carry their invariants

You are working in `ehr-testing-tools`. Two consecutive sessions hit the same class of authoring defect from opposite sides, and both were saved only by interpretive room the prompts happened to leave. This session records the lesson as a numbered convention in `AUTHORS-GUIDE.md`, so future prompt authors specify it deliberately instead of surviving it by luck. One new section, one commit, plus the close ritual. Doc-only.

The two instances, which the new section cites:

* Corpus adoption (Opus, 2026-07-26). The prompt's reference figures described the corpus framing as "segment delimiter CR" — correct but coarser than reality (messages are additionally separated by a blank LF line). The prompt's clause "a reference figure fails to reproduce → the discrepancy is the deliverable; record it, do not adjust toward this prompt" is what let the executing agent treat the finer truth as a finding (now F25) rather than a failure. Reality disagreed with the check; the check's intent was sound.
* Pack elide (Sonnet, 2026-07-26). The prompt mandated `grep -c "simhospital" <skills pack> # must be 0`. The count came back 8 — every hit prose inside the archived corpus-adoption prompt, which legitimately belongs in the skills pack. The stated invariant (no corpus file enters `pack-skills`) held; the command misencoded it as a substring check, and a prompt that names an artifact guarantees its own archived copy trips such a check — the false positive was structurally certain, not unlucky. The agent stopped, reported, and the author issued a corrected membership check in-flight. The check disagreed with its own invariant; reality was fine.

Read first: `AGENTS.md`; `AUTHORS-GUIDE.md` in full (you are adding §7; match the register and density of §§3–5); the two archived prompts — `.agents/prompts/archive/2026-07-26-simhospital-corpus-adoption.md` (its "Reference figures" section and final tripwire) and `.agents/prompts/archive/2026-07-26-pack-elide-and-research-errata.md` (its Step 1 verification block, containing the defective grep as issued); `notes/facts-register.md` row F25.

Sequencing gate: the pack-elide session must already be on `origin/main` — confirm its archived prompt exists at the path above. If it does not, stop; this session cites it and cannot run first.

Commits from WSL. `make test` green before and after. Save this prompt to `.agents/prompts/2026-07-26-checks-carry-invariants.md`; the final commit archives it.

## Author rulings in effect

1. Home is a new `AUTHORS-GUIDE.md` §7, nothing else. No ADR (this is authoring guidance, not a mechanism decision); no F-rows (the instances are already evidenced — F25 and the archived prompts); no skill; no AGENTS.md pointer (the guide is already on every session's read path).
2. Cite in-repo artifacts only. The section's two instances cite the archived prompts by path and F25 by row number. Chat-side session reports and author rulings are not committed artifacts; paraphrase their substance, cite the files.
3. Prompts archive as issued. The section states this explicitly: a defective check is corrected by an author ruling recorded in the session report, and the archived prompt keeps the defect — the archive is a record of what was asked, not what should have been.
4. Scope fence. `AUTHORS-GUIDE.md` gains §7 and nothing else changes in it; no other file is touched except the prompt-archive move. If §7's numbering collides because the guide gained a section since this prompt was written, take the next free number and say so in the session report.

## Step 1 — Write §7

Title: "Session-prompt verification checks carry their invariants." Content, in the guide's own voice (normative, compact, evidence-cited — model on §4's structure of rule-then-discipline):

1. The rule. Every verification command a session prompt mandates states, alongside the command, the invariant it encodes. The command is the measurement; the invariant is the claim. They can disagree, and the executing agent must be able to tell which one broke.
2. The two failure modes and their protocols. (a) Reality disagrees with a sound check → a finding; record it (F-row if it's a fact), never adjust reality's numbers toward the prompt's. (b) The check misencodes its stated invariant → an escalation; stop, report, and wait for a corrected check from the author — never silently patch the check to pass, and never silently wave the failure through. Without the invariant written down, (a) and (b) are indistinguishable, and both protocols collapse into the executing agent's guess.
3. Craft discipline for authors. Prefer checks on structure and membership (file lists, `FILE:` framing lines, exit codes, counts of structural markers) over substring checks that prose can satisfy. Beware self-reference: session prompts are archived and packed, so any prompt that names an artifact will itself contain that name — a substring check over a pack that includes the prompt archive is a structurally guaranteed false positive, not a low-probability one. Reference figures are things to reproduce, and the prompt should say what reproduction failure means (the corpus-adoption prompt's "the discrepancy is the deliverable" clause is the model).
4. Prompts archive as issued (ruling 3's text, one sentence).
5. The two instances, two or three sentences each, cited per ruling 2: the adoption prompt's coarse framing figure resolved as a finding (F25); the pack-elide prompt's substring check resolved as an in-flight corrected check, with the archived prompt retaining the defective grep as a record.

Length discipline: the section should sit comfortably beside §§3–6 — if it outgrows §4, it is over-explaining; cut examples before cutting rules.

Commit: `AUTHORS-GUIDE §7: verification checks carry their invariants (two instances cited)`

## Step 2 — Session close

1. `make test`, `make coverage` (nothing moves — doc-only; if either budges, stop), `make lint-pipeline`, `make lint-deps`.
2. Golden check: `make pipeline && make use-cases && git diff --exit-code docs/pipeline.md docs/use-cases.md docs/signature.edn` — any diff is scope breach, stop.
3. Session report: confirm the sequencing gate's observation, the section number actually used, and any wording where you departed from Step 1's outline and why.
4. Move this prompt to `.agents/prompts/archive/`.
5. Final commit, `git push origin` from WSL; read the pre-push hook output; the session ends when the push is confirmed on the remote.

Stop-tripwires, collected

* Sequencing gate: pack-elide archived prompt absent from `origin/main` → stop; wrong session order.
* Step 1: the guide gained sections since this prompt was written → not a stop; take the next free number, report it. But if an existing section already covers prompt-authoring checks → stop and report; this prompt assumed none does.
* Step 2: coverage or golden files move → doc-only session wrote something it shouldn't have; stop before further commits.
* Anywhere: this prompt's own summaries of the two instances conflict with the archived prompts or F25 as committed → the committed artifacts win; write §7 from them and note the divergence in the session report.

## Session deviation record (author ruling, 2026-07-26)

This ruling arrived mid-session, after §7 had already been written and committed once (`8d622f6`) following ruling 3 above as issued. The author observed that the pack-elide prompt's own archived copy (`.agents/prompts/archive/2026-07-26-pack-elide-and-research-errata.md`, on `origin/main` since `0b3aef8`) already carries a delimited, dated "Session deviation record" appendix recording its own in-flight correction — and ratified that shape as the convention going forward, symmetric with the research-doc errata convention (`docs/research/HL7v2-sanitized-corpus-research.md`): a prompt's **body** archives as issued; an in-flight author ruling that corrects a defective check appends as a dated deviation record, because chat-side session reports are not committed artifacts and rulings must be citable in-repo.

This supersedes ruling 3 as issued above, which spoke only of "the session report" as the ruling's home. §7 was revised (`90f2f0d`) to state the deviation-record shape explicitly and to cite the pack-elide instance via its appendix directly, rather than via an unrecorded chat-side report. One clause was added noting that the pack-elide prompt's own appendix predates this ratification — its closing sentence ("this prompt file itself archives unedited... the deviation and ruling live in the session report, not in a retroactive edit here") was written before the deviation-record convention existed as a named thing, and is left unedited per the very convention it anticipated. The final tripwire ("committed artifacts win") governs here: §7 is written from the tree as it now stands, not from ruling 3 as originally issued in this prompt.
