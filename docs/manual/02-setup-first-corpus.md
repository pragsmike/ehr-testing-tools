# Chapter 2 — Setup and your first corpus

This chapter narrates two reference docs as one story:
[`SETUP.md`](../../SETUP.md) gets your machine ready; the root
[`README.md`](../../README.md#quickstart)'s Quickstart generates,
breaks, and judges your first corpus. Neither doc repeats the other,
and neither repeats here — this chapter adds the narrative connecting
them: why each step exists, what you should actually see at each
checkpoint, and what it means when you do.

## Getting your machine ready

You need three things on your machine before any of this runs: `git`
(to clone the repo), a JDK (to run Clojure and `poly`, this workspace's
build tool), and the Clojure CLI itself (to resolve dependencies and
run everything — `poly`, the test suite, and `ehrt` itself).
[`SETUP.md`](../../SETUP.md#1-what-you-need-and-why)'s own table names
exactly which versions and how to verify each one; its platform-guidance
section has the actual install commands for your OS. Nothing here
repeats them — go run them, then come back.

The **why** worth knowing before you do: nothing about this workspace
needs a network connection once it's set up. Every external artifact
this workspace ever fetches — Synthea, the FHIR validator, a JDK build
for a subprocess — gets pinned and cached locally on first use, never
re-fetched silently later. Setup is the one time you're online; running
`ehrt` afterward isn't.

## The verification ladder, and what each rung proves

[`SETUP.md`](../../SETUP.md#3-verification-ladder)'s own ladder — clone,
then `clojure -M:poly version`, then `clojure -M:poly check` — isn't
busywork. Each rung answers one question: does the Clojure CLI resolve
this workspace's dependency graph at all (`version`), and does every
component's own declared interface boundary actually hold (`check`) —
this workspace is built as a [Polylith](https://polylith.gitbook.io/)
workspace internally, though you never need to know that vocabulary to
use it. A clean `OK` from `check` is your checkpoint: the workspace
itself is structurally sound before you generate a single message.

## Your first corpus, and the story behind each line

The root README's own Quickstart, one command at a time — this chapter
adds the *why* between the lines the README itself doesn't repeat:

```sh
bin/ehrt help
```

Orientation, not output you need to read closely yet — every command
group `bin/ehrt` accepts, so you know what's available before you run
any of it.

```sh
bin/ehrt corpus generate
```

This is the checkpoint that matters. Bare, zero flags, no downloads —
this workspace's own built-in simulator produces a real, deterministic,
seeded corpus. "Seeded" is the word to hold onto:
[determinism](../glossary.md) here means the same configuration and the
same seed always produce byte-identical output, and the corpus you just
generated carries a [manifest](../glossary.md) recording exactly what
would reproduce it. Chapter 1 already showed this command's own real
output and what it left behind
([`out/corpus/sim-s1-p1`](../../README.md#quickstart)'s four files); the
rest of the Quickstart — fetching Synthea, mutating a patient on
purpose, gating it, checking it — is this manual's own Chapters 3
through 6, not repeated here.

## The determinism contract, proven by you

Every other claim in this manual rests on one property: run the same
configuration with the same seed twice, get the same bytes back, always.
Don't take that on faith — the same bare command Chapter 1 already ran
once, run again, proves it directly. Witnessed this session, using
exactly the Quickstart's own strip, twice:

```sh
bin/ehrt corpus generate
# -> out/corpus/sim-s1-p1

# Save this run aside so there's something to diff against.
cp -r out/corpus/sim-s1-p1 out/corpus/sim-s1-p1-first-run

# SETUP.md's own troubleshooting note is the reproducibility contract
# stated as an error message: a zero-flag generate never overwrites a
# prior run, so regenerating means removing it first.
rm -rf out/corpus/sim-s1-p1

bin/ehrt corpus generate
# -> out/corpus/sim-s1-p1, again
```

Two runs, same seed, same everything. Diff the two:

```sh
diff -rq out/corpus/sim-s1-p1-first-run out/corpus/sim-s1-p1
```

Real output, this session: nothing. `diff -rq` printed no lines at all
and exited `0` — all four files (`events.edn`, `manifest.edn`,
`msg-000.hl7`, `msg-001.hl7`) came back byte-for-byte identical, down to
the [ground-truth log](../glossary.md) itself. That's not a claim about
past testing you're being asked to trust; it's a command you can run on
your own machine, against your own clone, right now, and get the same
empty diff back. Chapters 3 onward build on exactly this property —
every corpus this workspace ever generates for you is, underneath,
this same guarantee.
