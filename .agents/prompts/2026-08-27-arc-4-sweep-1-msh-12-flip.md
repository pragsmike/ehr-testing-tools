# Session prompt — arc 4 sweep 1: the 2.4 flip (ADR-0175 ruling A1)

Archived verbatim as delivered. Record:
[`2026-08-27-arc-4-sweep-1-msh-12-flip.md`](../session-records/2026-08-27-arc-4-sweep-1-msh-12-flip.md).

Three of its premises did not survive contact with the tree, all three
disclosed in the record: the oracle cannot report per-half (the
instrument in step 0's proof shape is what the sweep built); the mover
set is 34/5 rather than 36/3; and `gate v2` raises ZERO findings after
the flip rather than "some real ones", because the corpus is clean —
the flip's teeth are primitive typing alone.

---

Session prompt -- arc 4 sweep 1: the 2.4 flip (ADR-0175 ruling A1)

Context. HEAD 892201a. ADR-0175 is Proposed; the author ruled 2026-08-27
**A1 B1 C1 D1 E1**. A1: declare MSH-12 `"2.4"`, in its own sweep, PID-13's
rendering FIRST -- two commits, each a declared digest change with one
cause. The census measured why: at `"2.3"` every message this project
emits resolves to `GenericMessage$V23` under the judge's own parser (ADR
:168-171) -- `gate v2` has been structurally vacuous over our own output;
at `"2.4"` alone 346/747 fail, all PID-13 (HAPI's US-phone rule); with
PID-13 rendered `(NNN)NNN-NNNN`, 747/747 parse into real v2.4 structures.
Ground truth NEVER moves: the persona `:phone` regex (`persona.clj:108`,
`^\d{3}-\d{3}-\d{4}$`) and its two draws (:232-233) are untouched; only
`pid-segment` (`emit_hl7.clj:364-365`) and `site-profile/default-msh`
(`site_profile.clj`) change. This is the sweep that gives every prior
v2 gate its teeth. Payload session; re-derive every line.

Step 0. ADR-0175 -> Accepted, five rulings quoted where they land. Commit.

The proof shape (ADR-0175 E1, applied for the first time). The oracle
digests `{:ground-truth :hl7}` as ONE hash per root (`digest.clj:209`),
so an emission-only change makes every engine-layer root DIFFER -- which
is the CORRECT outcome here, and the oracle alone cannot then show ground
truth held. So this sweep owes a second instrument, landed FIRST as an
output-identical change: `bin/ground-truth-bracket <a> <b>` (or the split
the ADR designed, if E1 names one) that digests `:ground-truth` ONLY per
root across the 36 engine-layer configs and reports IDENTICAL/DIFFERS
in the regression-oracle's own idiom. Prove it on `892201a..HEAD` of its
own commit (IDENTICAL, 36 roots) and by mutation (a one-draw change ->
DIFFERS). Commit: `feat(oracle): ground-truth-only bracket -- the
emission sweeps' proof that facts held while the wire moved`.

Step 1. PID-13. RED: a `pid-segment` unit test asserting `(NNN)NNN-NNNN`
from a `NNN-NNN-NNNN` persona phone, empty stays empty; a judge-tier test
that parses one A01 at `"2.4"` and asserts a v2.4 structure class (this
is RED for the phone reason today -- capture it). GREEN. Re-pin once:
the four `arc0_gated_*` fixtures are GROUND TRUTH -- they must NOT move
(`git diff --stat` empty, and say so); message-side pins move: both
conformance baselines, `demos/traces` (`make traces`), message digests
in `run_test`/`engine_test` if any, scenario README wire excerpts.
Brackets: `bin/ground-truth-bracket 892201a HEAD` IDENTICAL, 36 roots;
`bin/regression-oracle <step-0 sha> HEAD --declared-digest-change`: all
36 engine-layer roots DIFFER, the 3 interpreter-layer roots IDENTICAL,
0 added/removed. Commit: `feat(emit-hl7): PID-13 as (NNN)NNN-NNNN --
every message moves once, ground truth identical (ADR-0175 A1, 1 of 2)`.

Step 2. MSH-12. `site-profile/default-msh` -> `"2.4"`; the registry
comments at `emit_hl7.clj:86-105` rewritten to state the new fact and
what it unblocks (SIU, sweep 4 -- not this one). The judge-tier test
from step 1 now asserts the REAL class per message family for every kind
in the registry (A01 A02 A03 A04 A08? no -- only kinds emitted today:
enumerate from the registry) -- 747/747-shaped: a corpus-wide assertion
that no message resolves to `GenericMessage`. Re-pin once more (same
list). Both brackets again: ground truth IDENTICAL; oracle all 36 DIFFER.
Then the conformance tier's verdict counts BEFORE and AFTER, per gate --
this is the number the sweep exists for: how many findings `gate v2`
issues now that it parses structures (expect: some real ones; each is a
FINDING for the record, not something to fix in this sweep). Commit:
`feat(emit-hl7): MSH-12 2.4 -- gate v2 is no longer vacuous (A1, 2 of 2)`.

Step 3. `make test` + `make integration` unpiped; push; CI; no tag.
Record one page: both bracket lines per commit, the structure-class
table, the before/after verdict counts with the first ten real findings
listed, what re-pinned, ADR premises the tree contradicted. Roadmap:
`[emission-add-ons]` sweep 1 of 6 landed; MSH-12 question CLOSED.

Fences. No draw, no persona, no engine change. Two message-digest
declarations, one cause each. Findings `gate v2` raises after the flip
are ROWED, not fixed. No chatter, no new message family, no SIU.
If ground truth moves under either commit, STOP.
