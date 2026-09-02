# 2026-09-02 — brick charters: every brick gets its one-page contract

Archived verbatim. Session record:
[`2026-09-02-brick-charters.md`](../session-records/2026-09-02-brick-charters.md).

---

SESSION: brick charters — every brick gets its one-page contract
Repo: pragsmike/ehr-testing-tools, tip (successor of the check-config
session's close). RULING (2026-09-01): the deferred brick-charter row
ACTIVATES. Purpose, verbatim from the author's 2026-08 concern: an
agent must be able to reason about bricks from their contracts —
responsibility, interface, invariants — without reading source. The
charters are DRAFTS FOR THE AUTHOR'S EDIT: derive everything from
shipped surfaces; invent nothing; where a contract is genuinely
unclear, say UNCLEAR with the competing readings rather than pick.

TEMPLATE (author-agreed shape, one page per brick):
1. Mission — one sentence, what problem this brick owns.
2. Interface contract — every interface.clj var, one line each:
   signature shape + what it promises (derived from docstrings and
   call sites, not invented).
3. Data shapes owned — the schemas/records this brick is the
   authority for.
4. Invariants guaranteed — what holds after its functions run
   (fixed-draw laws, byte-identity warranties, opt-in-key law
   membership, etc., where they apply).
5. Non-goals — what it deliberately does not do (lowering vs
   content layers, the demoted file-operator scope, etc.).
6. Forbidden edges — which bricks it must never require, from
   poly deps + the census doctrine.

STEPS (one gate each; full make test per push — docs-only)
1. Inventory: every component and base from the tree; note the two
   ADR-era charters (0162, 0172) — their bricks get the new format
   too, with one line citing the ADR as ancestry. Gate: count
   recorded; the charter set is the inventory, no brick skipped.
2. Charters, one file each at components/<brick>/docs/charter.md
   (bases/<brick>/docs/charter.md), drafted from: interface.clj,
   ns docstrings, the extraction Done doctrine, limitations
   registers, ADRs, consuming-ground-truth.md. Every interface var
   MUST appear in exactly one charter — that is the completeness
   gate; script it and show it. Commit in reviewable batches
   (engine family / emitter family / corpus+check+judges /
   cli+bases) so the author can read by cluster. Gate per batch:
   completeness script green over the batch.
3. The index + wiring: a generated or hand-kept docs/charters.md
   index (one line per brick: name + mission sentence); AGENTS.md
   gains one pointer line so agents find charters before source;
   reading-set placement PROPOSED in the record, not taken —
   budget re-triage is the author's. Gate: index lists every
   charter; state-derived LAST.
4. Push; CI via gh; close marker. Record: every UNCLEAR verbatim
   (these are the author's review queue); any interface var whose
   docstring contradicts its call sites (found-not-caused class);
   a priced note on the corpus-brick split IF the charter work
   surfaced its natural seam — describe, don't design.
FENCES: docs only — no src, no interface edits, no renames; no
invented promises: every charter line must trace to a shipped
surface; UNCLEAR over guess, always.
SELF-ARCHIVE: prompt and record in the final push.

---

## In-session author ruling

**AGENTS.md pointer vs the `:docs` budget** (step 3). The `:docs`
reading set was found **already over budget at the tip** — 787 against
a 785 budget and a 785 ratchet baseline, headroom −2, pre-existing and
matching `.agents/state-derived.md` as regenerated at `eff7a0f`.
AGENTS.md is in all five sets, so the pointer line would deepen it to
−3, and `.agents/rulings.md#R-budget-stop` says a session over budget
compacts or stops, never bumps. Put to the author with three options.

**Ruled: add the pointer, and compact to pay for it** — land `:docs`
at or under 785 in the same commit. Executed; see the record's
"`:docs` budget" section for what was compacted and why.
