# Session: docs errata -- third-party-sources and two future-features entries (docs-only)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-docs-errata-third-party-and-future-features.md`,
> which records that step 3's classpath stop did not fire (Synthea runs
> as a `java -jar` subprocess), and that step 4's "does a later record
> supersede the stop" held in part: ADR-0175 ruled the MLLP delivery
> sink in, and nothing lifts the stop for fault work.

Context. The related-work session (ce0520d1) recorded three pieces of
stale text in its session record but, being out of scope, did not edit
them. This session corrects them. Docs-only: no source, config or test
changes. Read the "Stale text" findings in
.agents/session-records/2026-09-30-related-work-essays.md first; they
are the brief.

Read first:
- components/sim/docs/third-party-sources.md:19-55 (Synthea tier-1 entry,
  HAPI/NIST tier-2 entry, "The load-bearing distinction")
- docs/future-features.md:97-111 (transport entry and its footnote) and
  :130-160 (summarize entry and its *Today:* line)
- notes/adr/0183-sim-describe.md (what `sim describe` covers, and what it doesn't)
- docs/cli.md, the `play --sink` row; artifacts.lock.edn's synthea entry

Author ruling, verbatim: "Write that prompt." (It licenses the three
errata the related-work record names, and nothing wider.)

Steps:
0. Pull and confirm the tree is clean and CI is green at the tip.
1. third-party-sources.md, Synthea entry: replace "not yet built" /
   "planned" with the interpreter's actual status (probe
   components/patient-simulator, gmf_interpreter). Replace "85 GMF module
   JSONs" with the vendored count and location as the tree stands
   (components/sim/resources/sim/modules/ and its NOTICE). Invariant:
   every figure is one you counted this session. Gate: the count and its
   command go in the record.
2. third-party-sources.md, HAPI/NIST entry: correct "Not wired into CI
   yet" to match what .github/workflows/ and components/judge-v2-nist
   actually do. If they're partly wired, say which part. Gate: cite the
   workflow line.
3. third-party-sources.md, "The load-bearing distinction": `corpus
   generate synthea` runs the pinned Synthea jar, so "nothing in this
   repo executes their code" is false. First determine HOW it runs
   (a separate process, or loaded onto this repo's classpath). If it's a
   separate process: restate the distinction truthfully (Synthea runs
   only as a separately fetched, pinned external artifact in one corpus
   lane, never linked into the sim pipeline) and keep the glass-box and
   license bullets consistent with that. If it's on the classpath: STOP;
   do not edit this section; report it, because it's a licensing
   question and the author must rule on it.
4. future-features.md, transport entry: the "nothing transmits over a
   socket" sentence is false (`ehrt play --sink mllp://` sends and
   checks ACKs). Correct the fact. Transport *fault injection* stays
   unbuilt, so the entry stays on the menu. Check whether a later design
   record supersedes the stop decision the footnote cites. If none does,
   keep the footnote and say playback is delivery, not fault work. If
   one does, re-point the footnote. Invariant: no decision is implied
   that the tree doesn't record.
5. future-features.md, summarize entry: the *Today:* line names
   `ehrt sim describe` and states what it covers per ADR-0183. The entry
   stays on the menu for the parts describe doesn't cover. ADR
   references stay marker-only footnotes. Gate: docs-tooling gates pass.
6. Write the session record and its INDEX line, and update the
   state-derived counts if the gate asks. Commit with an ASCII-only
   message and push. Close on `gh run view` showing CI green at the
   pushed tip.
