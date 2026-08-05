<!-- Attic file: notes/adr/0015-cli-trial-ux.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0015 — CLI trial-UX: generate sources front door, play directories, gate v2-nist verb, breadcrumbs pretty-only

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012/ADR-0013/
ADR-0014's own precedent):** this record shares its number with the
frozen `notes/tools/ADRs.md` ADR-0015 ("The gate loop maintains TWO
baselines: legacy-floor and full-capability"). Per this file's own
preamble citation rule: a bare `ADR-0015` anywhere in this workspace's
live documents means *this* record; the tools-era one is always cited
as `notes/tools/ADRs.md` ADR-0015 or `tools/ADR-0015`.

### Context

A design-channel UX audit (chat, 2026-07-30) walked the trial-user
journeys against `docs/cli.md` and found the consume side (`show`,
`gate`, playing a file, `--json`) genuinely simple — one verb, one
path, sniffing does the rest — with every friction point concentrated
on the produce side, none of it inherent to the underlying capability:

1. Sim generation is reachable only through `corpus intake 'sim:'` — a
   cataloging verb (SS-2) fronting for generation, with a URL spelling
   that needs shell quotes the moment params appear.
2. `play` rejects directories (`:play-input-unsupported`, the
   ADR-0014 deferral) while the sim generator emits one `.hl7` file
   per message — the natural sim→play pipeline needs a manual `cat`
   step the audit found nowhere documented as such.
3. The NIST profile-tier judge (`judge-v2-nist`, ADR-0012) — the
   headline capability of that arc — has no CLI verb at all (ADR-0012's
   own "CLI expansion deferred" note, never picked up), so a shell user
   cannot try it regardless of how the rest of the surface reads.
4. Produce commands (`generate`, `mutate`) end without telling the user
   what to do next — no bridge from "a corpus now exists" to "here is
   how to look at it."
5. The deliberate `:out-dir-exists` rerun rejection (D9's determinism
   law: a zero-flag command derives a stable path, so a second
   zero-flag run must not silently clobber the first) is doctrinally
   right but reads as a bare refusal unless its own `:hint` carries the
   literal remedy.

The audit's proposals were reviewed in chat and this session's own
prompt was commissioned on them. The `generate` restructuring carried
the one real design decision this record has to make (subcommands vs.
a `--source` flag); the recommendation — subcommands — stood
unobjected in that review and is ruled below.

### Decision

**[A] `ehrt corpus generate` grows source subcommands.** `corpus
generate synthea` and `corpus generate sim`, each with its own flag
spellings (`--seed`, `--patients`, …) mapped onto
`ehrt.tools.corpus.generators`' existing registry entries and their own
`:default-params` — the registry, not this CLI layer, remains the
single source of what each generator source *does*. Bare `corpus
generate` (no subcommand) stays exactly Synthea, byte-for-byte
unchanged, calling the same `generate!` function it always has —
compatibility with every existing doc, strip, and script that already
types the bare form, `bin/quickstart-demo` included. Both sources sit
under the same D9 zero-flag contract (frozen `tools/ADR-0019`): derived
`out/corpus/…` out-dirs, byte-reproducible, rejected-not-overwritten on
rerun (`:out-dir-exists`). The registry already shares
`generate/default-seed` across both entries, so `corpus generate sim`
with zero flags is a complete, deterministic command on its own.

*Why subcommands, not a `--source sim|synthea` flag* (the rejected
alternative): a subcommand is discoverable through `ehrt help corpus`
the same way every other multi-shape verb in this CLI already is (`gate
v2`/`gate fhir` is the in-house precedent this record follows, not a
new parsing mechanism), where a flag would need to already be known to
type before a reader could find it — the same discoverability argument
ADR-0013 already made for why `out/` had to be the *default*, not an
opt-in flag, applies here one level up.

**Amendment (2026-07-30, added by the cold-start UX session, ADR-0015
self-amendment).** This record's own compatibility sentence above
("Bare `corpus generate` (no subcommand) stays exactly Synthea,
byte-for-byte unchanged... compatibility with every existing doc,
strip, and script that already types the bare form, `bin/quickstart-demo`
included") is reversed here, fix-forward, not a revert of what this
record correctly decided for its own session: **bare `corpus generate`
now means `generate sim`**, not `generate synthea`. Ruled by the author
one session later, from a genuine cold-environment run of bare `bin/ehrt
corpus generate` (author's machine, Git Bash/Windows side, 2026-07-30):
a `run!` shadowing warning from `ehrt.tools.sim` was the first line of
output; the run then rejected `:not-cached` (the Temurin JDK archive
wasn't in the local artifact cache — Synthea's lane needs fetched
artifacts before it can run at all) with no remedy text; and the
rejected run left behind an empty `out/corpus/` directory. Rationale:
sim is this project's own engine, mounted in-process per ADR-0005 — zero
external artifacts, zero subprocess, zero network — so with sim as the
default, the first command a cold user types succeeds with nothing
fetched, where Synthea's default forced a fetch step before any success
was possible at all. `generate synthea` remains the explicit spelling
for the Synthea lane, unchanged in behavior; only the bare form's
routing flips — `generate sim` remains valid and identical to bare.
Consequence: `bin/quickstart-demo` (this record's own named beneficiary
of the old compatibility guarantee) now pins `generate synthea`
explicitly, since it deliberately exercises the Synthea→FHIR gate lane
the rest of the script depends on — the flip changes what it types, not
what it tests. Full ruling record, including the cold-run transcript in
full and every downstream doc/test site touched:
`notes/prompts/2026-07-30-ehr-testing-cold-start-ux.md`.

**[A] `corpus intake`'s generator-URL form (SS-2) and stdin form (SS-3)
are retained, unchanged in behavior.** They were designed as
composition features — generate-then-catalog, pipe-then-catalog — and
remain exactly that; no deprecation, no warning output, no behavior
change. What changes is positioning only: docs stop presenting `intake
'sim:'` as *the* way to run the simulator and present it as the
one-command compose it always was; `generate sim` is the front door for
"I just want a sim corpus."

**[A] `ehrt play` accepts a directory.** Files sharing the sniffed v2
format, concatenated in lexical filename order — which preserves the
sim generator's own `msg-%03d` emission order by construction — then
planned and paced as one stream, exactly as if the directory's content
had been `cat`-ed into one file first. The ordering rule is stated in
the verb's own help text: lexical order is the contract, deterministic
and disclosed, not an implementation detail a caller has to discover by
reading source — matching ADR-0014's own "the corpus's own order is
the corpus's own statement" doctrine, extended here to "the directory
listing's own order is the corpus's own statement" for exactly the same
reason (a caller who wants a different order names it in the
filenames). FHIR paths remain the named, disclosed unsupported input;
a bare directory of mixed or unclassifiable files is the same
`:play-input-unsupported` shape as before, not a new error family. This
half-retires ADR-0014's own `:play-input-unsupported` deferral — a
dated fulfillment note goes into that record, fix-forward, not a
revert.

**[A] `ehrt gate v2-nist PATH --profile BUNDLE_DIR` lands.** Picks up
ADR-0012's own skipped CLI-expansion step: builds the validator once
per invocation from the Π bundle (context construction dominates cost,
ADR-0012 — never a per-file rebuild), gates PATH (file or directory)
through the existing `ehrt.tools.interface` `v2-nist-*` re-exports, and
reports through the standard per-file verdict summary and envelope
machinery every sibling gate already uses (`gate-command`'s own
generic shape, unchanged). `--profile` is required — no default bundle
is silently assumed, since there is no project-owned profile yet
(ADR-0012's own "stand-in, not this project's own profile" disclosure)
— an absent `--profile` is a clear, named rejection, not a crash or a
silent no-op. The committed CDC fixture
(`components/tools/test-fixtures/v2-nist/COVID19_ELR-v2.3.1`) is the
documented try-it value, named in help text and the `docs/use-cases.md`
strip, never an implicit fallback a caller could stumble into
unknowingly. Bare `ehrt gate PATH` sniffing does NOT dispatch to
v2-nist — it structurally cannot, since sniffing has no bundle to build
a validator from — so `sniff-gate-command`'s own D11 dispatch table is
untouched by this record. A malformed `--profile` directory (missing
`PROFILE.xml`, or anything else the engine's own `make-validator`
throws on — `ehrt.judge-v2-nist.v2/make-validator` is one of this
workspace's few deliberate throw sites, a caller-contract violation
per ADR-0012's own Result-not-throw carve-out) is caught at this CLI
seam and surfaced as a named operational error, not an uncaught
stack trace.

**[A] Breadcrumbs.** The PRETTY summaries (`render-pretty`, ADR-0013 —
never the EDN/JSON envelope, whose shape is the machine contract and is
unaffected by this record) of produce commands end with one
copy-pasteable next command: `generate synthea`/`generate sim` →
`try: bin/ehrt show <out-dir>`; `corpus mutate` → `try: bin/ehrt gate
<mutants-dir>`. Two breadcrumbs, ruled here; more is permitted-skip
territory, named if added or explicitly declined during implementation.

**[A] The `:out-dir-exists` rejection's `:hint` carries the literal
remedy.** The exact `rm -rf <derived-dir>` for a fresh identical rerun,
and the `--out-dir` alternative for keeping the old run around; the
pretty rendering of this rejection presents it as the determinism
story ("same inputs, same directory, never silently overwritten"), not
as a bare refusal a reader has no way to act on. The envelope shape is
unchanged — `:hint` already existed as a key; only its text, and the
pretty rendering built around it, improve.

**[C] Subcommand grammar follows the cli-spec's existing positional
pattern for group verbs** (`gate v2`/`gate fhir` is the in-house
precedent) — no new parsing mechanism invented for `generate`'s own
subcommands.

**[C] One combined capture-and-build session, unattended (R30),**
matching this workspace's own 2026-07-30 session shape (ADR-0012,
ADR-0013, ADR-0014).

### Alternatives rejected

*A `--source sim|synthea` flag on `corpus generate` instead of
subcommands* — considered and rejected for the discoverability reason
stated above; recorded as the fallback this session's own decision
procedures name if the cli-spec's grammar genuinely cannot express
subcommands without new parsing machinery (it turned out it could; see
the deviation record if this alternative was ever actually taken).
*Sorting `play`'s directory input by MSH-7 timestamp instead of
filename* — rejected for the same reason ADR-0014 already rejected
sorting a single file's own message order: the corpus's own order (here,
the directory listing's own order) is part of what it says, and a
generator that names its files `msg-000.hl7`, `msg-001.hl7`, … is
already stating an intended order the way a single multi-message file's
internal sequence does.

### Consequence

`corpus generate` gains two named subcommands with no change to its
zero-flag/bare-command behavior; `intake`'s generator-URL and stdin
forms are unaffected in code, only in how docs introduce them; `play`
gains directory input via the existing `er7-multi` splitter, composed
rather than reimplemented; a fourth gate verb (`gate v2-nist`) joins
`gate v2`/`gate fhir` with no change to either sibling or to bare
`gate`'s own sniff table; every produce command's pretty rendering
gains one hint line with no change to its EDN/JSON contract, verified
by test. `notes/tools/ADRs.md` ADR-0015 (the two-baseline gate loop) is
untouched, frozen provenance, cited origin-qualified wherever this
record's own trial-UX work happens to reference gate-loop baselines at
all (it does not, directly).

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.

---

