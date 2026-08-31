# emit_hl7 extraction 6 -- the messages cluster, plus the authorized second engine-narrative compaction

Session prompt, 2026-08-31. Archived as issued, transliterated to ASCII
per this directory's own convention; the session's own record is
`.agents/session-records/2026-08-31-emit-extraction-messages.md`.

[A] Adopt.

---

SESSION: emit_hl7 extraction 6 -- the messages cluster, plus the
authorized second engine-narrative compaction
Repo: pragsmike/ehr-testing-tools, tip (a1380fa or descendant).
Roadmap row P5, emit phase, order per census section 2a. Rulings: C1(a)
with C7 (^:private delegating defs for #'-reached privates); constraint
5 as prohibition; S1(a); C8(a) -- the author authorizes compacting the
P5 row's remaining ENGINE-phase instance detail to pointers at the nine
engine session records; standing doctrine (tripwire recipe, bracket
hazard, constraint-5 prohibition, backlogs) stays. Do this FIRST as its
own docs commit; record headroom before and after.

READ FIRST
- Census section 2 `messages` (13 forms), section 2a, section 3b
  (segments -> messages 62 pairs INCOMING -- they stay behind until
  facade; messages' own outgoing: derive, expect segments, er7,
  registry, hl7-time, timelines candidates).
- The segments session record: the requalify-through-facade class (bare
  names resolving only via delegating defs must qualify to their owning
  namespaces -- count and name them in advance); the
  banner-falsified-by-later-move class (recount every prior banner's
  claims); dead-require watch; #' census re-run whole.

STEPS (one gate each; full make test before every push)
0. C8(a) compaction commit: "docs: compact the P5 row's engine-phase
   instance detail -- author-ruled C8(a)". Gate: roadmap-lint green;
   headroom recorded.
1. Derivations (markers, spans, re-exports, test + #' call sites, edges
   both directions, all-callers-travel analysis). Gate: recorded.
2. Constraint-6 sweep, all established levels including
   found-not-caused (disclose-and-backlog); dispositions committed
   before the move or absence disclosed; predicted reds RED-FIRST with
   successor; state-derived regen LAST. Gate: hit list or absence first.
3. Extract to ehrt.sim-emit-hl7.messages: verbatim except forced
   requalifications named in advance; widenings only where forced; C7
   defs where owed. Commit: "refactor: extract messages namespace from
   emit_hl7.clj -- output-identical". Gate: suite delta explained to the
   assertion, in-clone baseline; bin/regression-oracle IDENTICAL
   (load-bearing), no declaration -- delta = defect, stop and report.
4. bin/ground-truth-bracket (near-vacuous; run, say so). Gate:
   IDENTICAL.
5. Push; CI via gh; close marker. Record: census corrections; P5 row ->
   sixteen landings inside the recovered headroom; require set
   confirmed.
FENCES: no interface.clj edits; emitted bytes identical; no var
renames; no engine-side edits; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
