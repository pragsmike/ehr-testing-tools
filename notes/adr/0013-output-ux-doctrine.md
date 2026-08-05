<!-- Attic file: notes/adr/0013-output-ux-doctrine.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0013 — Output UX doctrine: single `out/` root, artifact-vs-display boundary (the TTY rule), the `show` verb, jet/`--json` surfacing

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012's own
precedent):** this record shares its number with the frozen
`notes/tools/ADRs.md` ADR-0013 (the cross-repo consumer loop: sim
consumed by subprocess, findings not failures, baseline-delta drift
detection). Per this file's own preamble citation rule: a bare
`ADR-0013` anywhere in this workspace's live documents means *this*
record; the tools-era one is always cited as `notes/tools/ADRs.md`
ADR-0013 or `tools/ADR-0013`.

### Context

Two end-user complaints drove this session, both raised in chat,
2026-07-30. **(1)** The tool's output directories are unintuitive:
`target/` is where every zero-flag default writes
(`target/corpus/synthea-…`, `target/spool/…`, `target/gate-fhir`) and is
gitignored, while `out/` exists only as the path `docs/use-cases.md`
teaches users to type — created by no default, and NOT gitignored — so
following the quickstart with an explicit `--out-dir out/...` dirties
`git status` the moment a reader instead runs a zero-flag command and
gets `target/` back. The two names are one concept split by accident of
provenance (`target/` inherited from JVM build tooling convention,
`out/` invented for the docs): under the determinism doctrine
(everything derived is reproducible from seeds and inputs, `tools/ADR-0019`
via `docs/source-sink-design.md` D9) there is no second purpose either
name is protecting. **(2)** Users who run gates get a raw EDN envelope
on stdout, don't recognize EDN, and can't `jq` it — while `--json` has
existed on every command all along (`docs/formats.md`). A
discoverability defect, not a capability gap. A third, smaller finding
surfaced in the same session: ER7 content is unreadable in a terminal
because segment separators are bare CR, with no display verb to make it
legible.

One prior design constrains the shape of the fix to (2)/(3): the corpus
player named in the founding design chat (2026-07-27, unarchived —
cited here by the decisions it settled) decomposes into four separable
parts — an **input adapter** (something → a time-ordered event stream),
a **pacer** (stream-time → wallclock at rate R, with idle-skip), an
optional **accumulator** (the M6 v2-replay accumulator, already tested
code), and **sinks**, of which a **ticker** (one line per event) is the
primary visual one and paced file/MLLP emission is the non-visual
sleeper that makes the player a load/soak instrument. `ehrt show`,
below, is built as the ticker sink at infinite rate with no pacer — not
a rival mechanism the player will later have to absorb.

### Decision

**[A] Single tool-owned output root, named `out/`.** The docs-facing
name wins over the JVM-conventional one: it is what `docs/use-cases.md`
already trained users to type, it is self-explanatory to the
non-Clojure audiences `docs/dev/positioning.md` names, and it separates
`ehrt`'s own data from build tooling's own `target/` (poly/clojure
artifacts, lint caches). Doctrine sentence, verbatim, into this record
and into the user docs it touches:

> ehrt writes only under `out/` unless you pass `--out-dir`; `out/` is
> ignored and always safe to delete — everything in it is reproducible
> from seeds and inputs; `target/` belongs to the build.

Substructure: `out/corpus/` (generated corpora, `corpus generate` and
the `synthea:`/`sim:` generator-URL kinds), `out/spool/` (SS-3's spooled
stdin capture), `out/scratch/gate-fhir` (the FHIR validator's scratch
directory, renamed from a bare `gate-fhir` to name what it is now that
it sits under a shared root with siblings). `out/` enters
`.gitignore`. The mutate default (`<PATH>-mutants/…`, input-adjacent by
design, D12) is **not** moved — it derives from the input path, not
from a tool root, and the two are orthogonal: `out/` names where a tool
invents a fresh location; `<PATH>-mutants/` names a location relative
to something the caller already gave it. Nothing about this doctrine
touches the `--out-dir`/`--out` escape hatch on any command: an
explicit flag still wins outright, exactly as before.

**[A] The EDN envelope is no longer the unconditional stdout default.**
Rule: if stdout is a live terminal, default to human-readable rendering
(`--pretty` behavior, below); if stdout is a pipe or redirect, default
to the EDN envelope exactly as today. Doctrinal basis, stated here so
no future session mistakes this for a collision with `corpus.generate`'s
own refusal of ambient-dependent defaults (D9's determinism law): that
law governs **artifacts** — files, `--report` output, and any piped or
redirected bytes, because those are read by another program or kept as
a record, and a record must not vary with who's watching. A live
terminal is a human (or an assistant driving a shell for one), not an
artifact; interactive rendering is outside the doctrine's scope for the
same reason `ehrt help`'s own plain-text exception already sits outside
it (`bases/cli/src/ehrt/cli/core.clj`'s ns docstring). The sniff
therefore biases conservative: any doubt — either stream redirected, no
console attached at all — resolves to the machine format, so no
downstream consumer ever silently receives sniffed variance because of
where or how the process happened to run.

**[A] Flag precedence over the sniff.** `--pretty` forces human
rendering even into a pipe; `--edn` forces the raw envelope even at a
terminal; `--json` behaves exactly as it does today (a projection of
whichever envelope form was chosen — see the pretty/JSON interaction
below). Sniffing applies only when none of the three is given.
`--report` files are untouched by all of this: always EDN, always the
bare report (no `:status`/`:payload` wrapper), per `docs/formats.md`'s
existing, unchanged contract — the TTY rule governs stdout only.

**[A] Pretty means, by command class.** Envelope-emitting commands
(`gate`, `generate`, `mutate`, `intake`, and kin) render a compact
human summary, never a prettified EDN envelope — the envelope is the
machine form, full stop, and pretty is a different rendering entirely,
not indentation applied to the same data:

- **`gate`** (and `check`, same report shape): one verdict line per
  file, then aggregate finding counts by code, then any paths actually
  written (`--report`'s own path, when given) — a human scanning this
  can tell what happened without reading a single brace.
- **Every other envelope command** (`generate`, `mutate`, `intake`,
  `artifact fetch`/`resolve`, `version`, `doctor`): a brief generic
  rendering of `:status`/`:category` plus the payload's key counts and
  paths (whatever it already carries — a file count, an out-dir, a
  cached flag), plus one hint line naming both `--edn` and `--json` for
  the full envelope.

Tailoring beyond `gate` is a **permitted skip**: where a command's
payload resists a sane generic summary (deeply nested, no obvious
counts), the hint line plus a pretty-printed payload is the fallback,
recorded as this ruling's own named allowance rather than silently
shipped as a polish gap.

**[A] `ehrt show PATH`.** A new display verb, pretty-always regardless
of stdout's destination — its entire job is rendering for eyes, so
`ehrt show foo.hl7 | less` must work with no flag at all. It joins
`gate`'s own D11 sniff-format dispatch (`corpus.intake/sniff-format`):
ER7 renders one segment per line (CR → LF, trailing separator
stripped), with a blank line between messages; FHIR JSON renders
pretty-printed. **Display renderings are not wire format** — LF-joined
ER7 segments are nonconformant ER7 by construction, and that is
correct: the eyes/pipes split is structural (a distinct verb), never a
flag bolted onto a wire-emitting path that would tempt a caller into
piping `show`'s own output somewhere a real HL7 v2 consumer sits. `show`
never modifies the file it reads; it is read-only by construction, not
merely by convention.

`show` is designed, in its code shape, as the corpus player's ticker
sink at rate ∞ (Context, above) — not a mechanism the player will later
have to reimplement or absorb. The render function is **per-message**:
one ER7 message in, one rendered block out, with no knowledge of a
stream. The stream-level concerns — splitting the input into messages,
mapping the renderer over them, joining with blank lines — live in
`show`'s own thin CLI-adjacent layer, never inside the renderer. This
is the exact call shape a future pacer will need: call the renderer
once per event, at whatever cadence the pacer computes, with no
stream-splitting logic to route around. The player itself — the
pacer, the accumulator wiring, bed-board/census sinks, paced
file/MLLP emission — remains future work per the founding chat's own
ruling; this record only keeps `show`'s internals from foreclosing it
by accident.

**[A] jet and `--json` discoverability.** `docs/formats.md` gains a
"Reading these from a shell" section, sibling to its existing "Reading
these from Python": `--json | jq` as the zero-install route for the
EDN-projected-to-JSON path; `jet` (borkdude/jet) named as the
EDN-native equivalent — querying EDN directly, or converting an
existing `--report` EDN file to JSON for `jq` without a full rerun.
One-line mentions land in `README.md`'s Quickstart, the first gate strip
in `docs/use-cases.md`, and `ehrt help`'s own top-level doc line (which
flows into `docs/cli.md` via regeneration, `make cli-doc`).

**[A] One combined capture-and-build session, unattended (R30).**

**[C] The TTY probe is an injected seam, not a scattered ambient
call.** `bases/cli/src/ehrt/cli/core.clj`'s `main!` already injects
`:println-fn`/`:exit-fn`; this record adds `:tty?-fn` to the same map,
defaulting to a real check — `(some? (System/console))`, the classic
JVM idiom, chosen because its property of returning `nil` the moment
either stream is redirected is exactly the conservative bias the TTY
rule calls for. Tests pin both branches deterministically by injecting
the seam; the sniff is ambient only in real, un-instrumented use.

**[C] `docs/cli.md` is generated, never hand-edited.** Any doc claim
this session makes about a default or a flag comes from `make cli-doc`
regenerating it from `bases/cli/src/ehrt/cli/help.clj`'s own spec, not
from a manual edit to the rendered file.

**Frozen-era default strings, superseded in behavior, not in text.**
`tools/ADR-0019` (`notes/tools/ADRs.md`) and D9
(`docs/source-sink-design.md` Part IX.2) are the frozen/live records
that established `target/…` as the zero-flag default family this record
now moves to `out/…`. Every live citation of D9/`ADR-0019` in
`bases/cli/src/ehrt/cli/help.clj`, `core.clj`, and
`components/tools/src/ehrt/tools/corpus/generate.clj` names the
*determinism-of-defaults doctrine* those records state, which this
record does not revise — only the one concrete default-path family that
doctrine happened to produce, which this record supersedes in
behavior. Frozen `notes/tools/ADRs.md` itself is never edited; live
docstrings citing `ADR-0019` by bare number are left as-is (they remain
accurate about the doctrine) rather than swept for qualification as a
side effect of this session's own default-path change — a future
citation-hygiene pass, not this one, is the right place to qualify
every bare frozen-era reference workspace-wide.

### Alternatives rejected

*Naming the shared root `target/` instead of `out/`* — `target/` is
already the JVM/Clojure build-tooling convention (compiled classes,
poly/lint caches) and overloading it with `ehrt`'s own run output would
re-introduce exactly the ambiguity this record exists to remove, just
under the other name. *Making `out/` opt-in via a flag rather than the
default* — the whole complaint is that the zero-flag path and the
docs-taught path disagree; a flag a reader has to already know to pass
doesn't fix a discoverability gap, it relocates it. *Sniffing pretty-vs-EDN
inside `--report`/file-writing paths too* — the determinism doctrine
governs artifacts precisely because they're read by something other
than the terminal that produced them; making a `--report` file's shape
depend on how it happened to be invoked would reintroduce the sniffed-variance
hazard the conservative bias exists to prevent. *A flag instead of a verb
for `show`* — `docs/formats.md`'s own display-vs-wire doctrine (this
record) needs a structural boundary a caller cannot accidentally pipe
into a wire-format consumer; a `--pretty`-style flag on `gate`/`generate`
already exists and means something different (a summary of a Result
envelope), and reusing it for "render the file's own bytes for a human"
would conflate two unrelated meanings under one name.

### Consequence

Every command's zero-flag output moves under `out/`, `.gitignore` gains
one entry, and `docs/use-cases.md`'s existing strips (which already
spell explicit `--out-dir out/...` throughout) need no strip-content
edits — only the default-path prose describing what happens with no
flag at all. Every envelope command gains a human-facing rendering path
with no change to its EDN/JSON contract for anything already piping or
redirecting it. `ehrt show` is new surface, zero interaction with any
existing verb's exit-code or output contract. The corpus player
(`docs/dev/` design lineage, founding chat 2026-07-27) inherits a
tested, per-message renderer and a message-splitter it can reuse rather
than reinvent, once a future session builds the pacer around it.

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.

---

