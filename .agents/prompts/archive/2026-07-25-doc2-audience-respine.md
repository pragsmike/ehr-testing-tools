# DOC-2 — Audience register + docs re-spine

You are working in `ehr-testing-tools` (public). This session executes
DOC-2 of `.agents/plans/user-docs.md`: the seven documentation
audiences become canonical in `docs/positioning.md` §Audience (it owns
three of them today), and `docs/README.md` is reshaped from a single
reading-order spine into per-audience entry paths, with the existing
spine preserved as the deep walk one of those paths uses. Docs-only —
no code, no new documentation files (DOC-3 owns the reference pages),
no generated-doc drift. The organizing principle carried over from the
audit: most audiences don't need new content this session, they need a
front door that routes them to what already exists — including the
CLI help surface DOC-1 just landed (`ehr help`,
`ehr corpus operators`), which several paths below lean on.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`,
`.agents/plans/user-docs.md` (the Audience register section — the
seven-audience list this session re-expresses — and the DOC-2 scope
section), `docs/positioning.md` (whole file: §Audience is the edit
site, but the constellation/referral-trigger/first-release sections
constrain what the new segments may promise), `docs/README.md` (the
spine being reshaped), `README.md` (what the front door already says —
DOC-2 does not edit it, but paths must not contradict it),
`SETUP.md`'s intro and step 5 (the copy-paste-prompt cohort — evidence
for the agent-reader segment), `docs/judge-calibration.md` §No-verdict
and §Reading this table (the downstream-consumer path's best current
landing), `docs/use-cases.md`'s header (what the practitioner path
sends people into). Ritual: commit → `git push origin`. Save this
prompt to `.agents/prompts/2026-07-25-doc2-audience-respine.md`; final
commit archives it to `.agents/prompts/archive/`.

Author rulings in effect: **positioning.md is the canonical audience
home** — the plan's own register gains a status line deferring to it
(status line, not deletion; history stays put). **Entry paths
reference only what exists at HEAD** — where a path wants a document
DOC-3 hasn't written yet (formats, locators, cli.md), it points at the
best real landing today (`ehr help`, judge-calibration, source-cited
schemas) and may name the gap plainly with a pointer to
`.agents/plans/user-docs.md` (precedent: README.md already links
`.agents/plans/corpus-foundations.md`); no dead links, no "coming
soon" without the plan pointer. **The existing eight-step spine
survives** as a titled section of `docs/README.md` — the method-first
path walks it, and its anchors may have inbound links; reshape around
it, don't delete it. **Voice** — positioning.md's §Audience entries
are written in that document's own working-doc voice (on-ramps and
needs, not marketing); the plan's register is source material to
re-express, not paste. **Claims are verified, not inherited** — every
factual assertion a new segment makes (the trial cohort's traits, what
the agent audience consumes, what a Python reader can actually do
today) is checked against the repo before it's written; if the plan's
register says something HEAD doesn't support, write what HEAD
supports and note the divergence at close-out. **Scope fence** —
top-level `README.md` and `SETUP.md` untouched; `AUTHORS-GUIDE.md`
gains at most one short paragraph (Step 3, my recommendation — strike
that step if unwanted).

## Step 1 — positioning.md: three segments become seven

§Audience grows from three entries to seven, keeping the existing
three (renumbered as needed) with their current text substantively
intact — the trial-cohort sharpening in segment 2 stays, it is
evidence. New segments, per the plan's register: the **AI assistant
as reader** (SETUP.md's copy-paste prompt and the cohort's
agent-assisted default make it a first-class consumer; what it needs:
exact commands, stable anchors, self-explanatory errors — cite
DOC-1's help surface as the first deliberate serving of this
audience); the **downstream data consumer** (reads report/manifest/
lineage EDN or `--json`, never runs the tools; the format reference
they need is a named gap, plan pointer per the ruling); the **Clojure
library consumer** (post-first-release; today's serving is source
docstrings; ties to the existing first-release section — cross-link
rather than restate); the **evaluator/decision-maker** (served by
README's maturity table, scope fence, and the problem statement —
name that explicitly so the front-door routing in Step 2 has an
anchor). One sentence somewhere in the section stating the section's
new role: this is the canonical audience register; `docs/README.md`
routes by it.

Commit: `DOC-2: positioning.md audience register — three segments become seven (canonical home)`.

## Step 2 — docs/README.md: per-audience entry paths, spine preserved

Reshape: a short preamble ("find yourself below; each path is 3–4
steps"), then one path per audience that arrives at docs at all —
expected: task-first practitioner (SETUP.md → README quickstart →
`ehr help` / `ehr corpus operators` → use-cases.md to find their
case → judge-calibration.md when a gate surprises them); method-first
guide reader (use-cases.md → the deep-walk spine below); AI assistant
(SETUP.md's step-5 prompt → `ehr help` → the practitioner path, plus
the one sentence that these docs are written to be agent-legible);
downstream data consumer (`--json`, judge-calibration's No-verdict
and Reading-this-table sections, the named formats gap); contributor
(AGENTS.md → AUTHORS-GUIDE.md → ADRs/facts-register — three lines,
then out: that audience's docs live at repo root); evaluator (README
maturity table → problem statement → positioning.md). The Clojure
library consumer gets one line pointing at positioning.md's
first-release framing rather than a path — there is nothing to walk
yet. Then the existing numbered spine, retitled as the deep walk
(e.g. "The deep walk: pipeline-first reading order"), content intact
except renumbering/link fixes if the preamble displaced anything.
Each path's steps are links that resolve at HEAD — mechanically check
every relative link in both edited files before committing (a stale
anchor is this session's likeliest defect).

Commit: `DOC-2: docs/README.md — per-audience entry paths; spine preserved as the deep walk`.

## Step 3 — AUTHORS-GUIDE.md: the agent-legibility paragraph (optional, author to ratify or strike)

One short paragraph in AUTHORS-GUIDE.md's docs-authoring vicinity:
user-facing docs are read by AI assistants acting for the trial
cohort, so prefer exact copy-pasteable commands over descriptions of
commands, keep heading anchors stable across regeneration, and make
error text self-explanatory (DOC-1's precedent). No new rules beyond
those three preferences; this is style guidance, not a gate.

Commit: `DOC-2: AUTHORS-GUIDE — docs are agent-read; three legibility preferences`.

## Step 4 — Close out

Plan updates in `.agents/plans/user-docs.md`: the Audience register
section gains its deferring status line ("canonical in
docs/positioning.md §Audience as of DOC-2; this list is the adoption-
time snapshot"); the DOC-2 tracker row → Done with an itemized
summary and the prompt path; and one record repair riding along —
the DOC-1 section's "Landed as five commits" is corrected to six
(Step 0 through Step 5; verify against `git log` before writing).
Golden check (`make pipeline && make use-cases && git diff
--exit-code docs/pipeline.md docs/use-cases.md`) must be clean — this
session touches no generated doc, a diff there is scope creep, stop.
Full suite + both lints green (nothing here should move them; run
them anyway — cheap proof this really was docs-only). Re-run the
link check on the final state of both edited files. Archive this
prompt.

Commit: `DOC-2 complete: audience register canonical, docs re-spined (archives prompt)`.
