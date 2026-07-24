# Engine Onboarding Checklist

The short list every future engine wrapper (a new subprocess-based
generator, mutator, or gate engine — anything wrapping an external
tool per ADR-0005) must answer before it's trusted enough to feed a
committed manifest. This is a checklist, not a tutorial: each item
names a question the wrapper's author must have a concrete answer for,
citing the pattern or ADR that motivates it. Every engine this
checklist governs is one of the components described in
[`docs/components.md`](components.md) — that page describes what each
engine is and how it's currently used; this one is what a *new* engine
has to clear before it earns a section there.

**Motivating case.** EXP-A4's clinician-seed finding: Synthea's own
patient-generation seed (`:seed` / `-s`) was pinned from the start, but
every practitioner assignment (name, gender, email, practitioner ID —
and every `Reference.display` pointing at one, across every patient
file) still varied run to run. The cause was a *second*, independent
RNG stream — Synthea's clinician seed (`-cs`) — silently defaulting to
`System.currentTimeMillis()` whenever it wasn't passed explicitly.
`:seed` alone did not determine output; nothing in Synthea's CLI usage
text called this out as a second entropy source until the `--help`
text and `synthea.properties` were read directly. An engine wrapper
that assumes "the seed I was told about is the only one" will ship a
manifest that looks fully pinned and isn't.

## The checklist

1. **All entropy sources enumerated.** How many independent RNG
   streams does the engine actually consume — not how many the
   top-level CLI flags advertise? What does each stream default to
   when its seed isn't passed explicitly (wall-clock time is the
   dangerous default; confirm by reading the engine's own source or
   properties file, not just its `--help` text)? Every stream that
   affects output must be a required, pinned manifest field — see the
   clinician-seed case above.
2. **Environment forced-and-recorded** (pattern [#15](../.agents/memory/patterns.md)).
   Locale, timezone, JVM version, and any other ambient field the
   engine's output could plausibly depend on must be forced explicitly
   into the subprocess invocation (not left to inherit from the host),
   and the manifest must record the *forced* value — never a value
   read from the orchestrating process's own environment, which can
   silently differ from what the subprocess actually saw.
3. **Native output preserved verbatim** (pattern
   [#1](../.agents/memory/patterns.md), two-step engines). The
   execution step writes the engine's raw output tree unmodified;
   interpretation into canonical data is a separate, later, pure step.
   This is what let EXP-A4's driver-hash fix apply by recomputation
   over already-preserved outputs, with zero regeneration.
4. **Every external input resolved via the three lockfile targets**
   (pattern [#13](../.agents/memory/patterns.md)). Anything the engine
   consumes as input — its own binary/distribution, module sets,
   profile packages, a runtime it needs (a JVM, an interpreter) — must
   resolve to exactly one of: `artifacts.lock.edn` (acquired/external/
   binary, ADR-0005), `deps.edn` (JVM/Clojure library dependencies), or
   hashed repo-authored config (text we wrote and version, referenced
   by path plus content-hash). An input that resolves to none of the
   three is a gap, not an oversight to paper over.
5. **License row in the facts register.** The engine's license status
   gets an F-row in `notes/facts-register.md` (claim, evidence, date)
   before its output is trusted for anything beyond local
   experimentation, per `AUTHORS-GUIDE.md` section 4.
6. **`docs/components.md` section.** A short section describing what
   the engine is, its steward and license (citing the F-row above),
   what it's used for in this repo, and what it's deliberately *not*
   used for.
