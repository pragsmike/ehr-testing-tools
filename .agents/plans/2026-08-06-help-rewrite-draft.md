# help-spec voice rewrite — design-channel draft for author ruling

Authored 2026-08-06 against `7665baa`, from register rows B-1/B-2/B-3/B-4(a)
and the audit's classified appendix. This document IS the implementable
artifact: §3 is the complete replacement text for every rendered string in
`cli-spec`, with every removed citation relocated to an adjacent `;;` source
comment — nothing deleted, everything moved or translated. The landing
session applies §3 verbatim, co-lands the §5 gate, and regenerates
`docs/cli.md`.

## 1. Principles applied

1. **Rendered strings speak operator language.** What it does, how to run
   it, what the values mean. History, rulings, and internal names live in
   source comments beside the entry they used to decorate.
2. **Nothing is deleted.** Every ADR/milestone/session token relocates to a
   `;;` comment adjacent to its entry. Tally: 24 ADR citations, 14 milestone
   tags (D-numbers, SS-numbers, ruling-numbers), 0 deletions.
3. **Operator-critical warnings stay in user text**, shortened: `show`'s
   display-is-not-wire warning, `play`'s lexical-order contract, `generate`'s
   out-dir-exists rejection, `--clinician-seed`'s wall-clock trap. These are
   things a user is hurt by not knowing — the opposite of history.
4. **Internal names become behavior.** `ehrt.sim.interface/run-command`,
   `corpus.intake/sniff-format`, `cli/result->exit-code` disappear from
   rendered text; the behaviors they name are described instead.
5. **Maintainer def-docstrings are untouched** (the `ns` docstring,
   `exit-codes`'s and `top-level-doc`'s own docstrings) — B-1 scoped them
   out because they never render; the gate scopes identically.

## 2. Judgment calls for your ruling (defaults applied in §3; strike any)

- **J1** — The Π symbol leaves `--profile`'s text (theory vocabulary is
  agent-speak on this surface); the bundle is described by its files.
- **J2** — Exit code 3's meaning is rewritten self-contained ("a gate found
  :no-verdict outcomes...") instead of citing ADR-0010; the flag reference
  stays because it's the user's remedy.
- **J3** — "determinism is a feature, not a default" stays on `--seed` — it
  is voice, not jargon, and it teaches the right expectation.
- **J4** — `--format`'s and the `corpus`/`gate`/`play` group docs are
  content-tightened, not just token-stripped — the largest cuts are 668→~300
  and 707→~380 chars. Wrap mechanics remain session 5's; these cuts are
  about saying less, not wrapping more.

## 3. The replacement text (complete; apply verbatim)

```clojure
(def exit-codes
  ;; Mapping reasoning: ADR-0004 (ok/rejected/error), extended by ADR-0010
  ;; (no-verdict arm). Authoritative logic: cli/result->exit-code.
  [{:code 0 :meaning "ran and passed"}
   {:code 0 :meaning "bare invocation, help, and --help all exit 0 too"}
   {:code 1 :meaning "ran and legitimately rejected"}
   {:code 2 :meaning "operational error (bad invocation, missing artifact, subprocess failure, etc.)"}
   {:code 3 :meaning "a gate found :no-verdict outcomes and the default --treat-no-verdict-as policy is in effect -- see that flag to fold them into pass or rejected"}])

(def global-flags
  ;; --pretty/--edn terminal-detection defaults: ADR-0013.
  [{:flag "--json" :doc "project the EDN result to JSON (EDN remains canonical)"}
   {:flag "--pretty" :doc "force a human-readable summary, even when stdout is piped -- already the default at a real terminal"}
   {:flag "--edn" :doc "force the raw EDN envelope, even at a terminal -- already the default when stdout is piped or redirected"}
   {:flag "--help" :doc "print this command's usage and exit 0 without running it"}])
```

`top-level-doc`'s rendered value is already clean — unchanged.

```clojure
    ;; Artifact registry design: ADR-0005. --all introduced in D13,
    ;; replacing SETUP.md's multi-fetch walkthrough.
    {:group "artifact"
     :doc "Fetch and resolve locked external engine/tool artifacts."
     :verbs
     [{:verb "fetch" :doc "Fetch a locked artifact into the local content-addressed cache."
       :flags (into artifact-flags
                    [{:flag "--all" :doc "fetch every artifact the lockfile names; --name/--version are ignored when given. One failing artifact does not abort the rest -- the overall result is the worst individual outcome." :default "false"}])}
      {:verb "resolve" :doc "Resolve an already-fetched artifact to a filesystem path."
       :flags artifact-flags}]}

    ;; URL designators: ruling 7, docs/source-sink-design.md D4.
    ;; generate front door + bare=sim: ADR-0015 (+ 2026-07-30 amendment:
    ;; sim needs no fetched artifacts, so the cold first command succeeds).
    ;; intake compose forms: SS-2 (generator URL), SS-3 (stdin designator).
    ;; Zero-flag reproducible defaults: D9 / ADR-0019.
    {:group "corpus"
     :doc "Generate, mutate, intake, and inspect synthetic corpora. Any PATH, --out-dir, or --out also accepts a dir:/file: URL designator in place of a bare path; bare paths are the common spelling. `corpus generate` is the front door for new corpora; `corpus intake` catalogs existing ones -- and can generate-then-catalog, or read piped bytes, in one command (see intake)."
     :verbs
     [{:verb "generate" :doc "Generate a deterministic synthetic corpus. Takes a source subcommand: `corpus generate sim` (this workspace's own engine; the flags marked sim:) or `corpus generate synthea` (the flags marked synthea:). Bare `corpus generate` means `generate sim`. Both bare commands are byte-reproducible as-is; re-running into an existing non-empty --out-dir is rejected (:out-dir-exists), never silently overwritten."
       :flags [...unchanged except:
               {:flag "--clinician-seed" :doc "synthea: clinician-generation seed (integer) -- Synthea otherwise defaults this to wall-clock time, which breaks reproducibility even with --seed pinned" :default "the resolved --seed value"}
               ;; (the existing D10 rename comment above --lockfile stays)
               ]}
      ;; --locator-path default-locator fallback: D12.
      {:verb "mutate" :doc "Apply one mutation operator at one locator to every matching file under PATH."
       :flags [...unchanged except:
               {:flag "--locator-path" :doc "format-specific locator string (FHIR data-path, or v2 segment/field grammar) -- falls back to the operator's own :default-locator when it declares one; required otherwise"}]}
      {:verb "intake" :doc "Catalog a foreign corpus batch (one not generated by this repo). In place of PATH: a generator URL (sim:?seed=42, synthea:?seed=1&population=5) generates the corpus first and then catalogs it; a stdin designator (stdin:?format=v2-er7&framing=er7-multi) reads piped bytes, spools them one file per item, and catalogs the spool."
       :flags [...unchanged]}
      {:verb "operators" :doc "List the registered mutation-operator catalog. Candidates that were considered and dropped are documented in docs/judge-calibration.md, not here."
       :flags [...unchanged]}]}

    ;; Sniff dispatch: D11 / ADR-0019, via corpus.intake/sniff-format.
    ;; NIST profile tier: ADR-0012. Designators: ruling 7.
    {:group "gate"
     :doc "Conformance-gate a file or directory against HL7 v2, FHIR, or (with --profile) an HL7 v2 conformance profile. Bare `ehrt gate PATH` sniffs the format and dispatches between v2 and fhir only -- never v2-nist, which needs an explicit --profile. A directory mixing formats, or a file that can't be classified, is an error naming the explicit override (`gate v2 PATH` / `gate fhir PATH`), never a silent per-file split. PATH and --out-dir also accept dir:/file: URL designators."
     ...
     [{:verb "v2" :doc "Gate against HL7 v2 base-structural conformance (HAPI)."}
      ;; verdict cache: ADR-0016.
      {:verb "fhir" :doc [unchanged] ...
       {:flag "--no-verdict-cache" :doc "skip the content-addressed verdict cache; always re-run the validator subprocess" :default "false (caching on)"}}
      ;; Engine perf note (validator built once per invocation, reused
      ;; across files -- context construction dominates): ADR-0012.
      ;; Π-bundle vocabulary + CDC fixture provenance: ADR-0012 / register.
      {:verb "v2-nist" :doc "Gate against HL7 v2 profile-tier conformance (the NIST engine): profile usage, cardinality, length, conformance statements, co-constraints, slicing, and value-set bindings -- what the structural v2 tier cannot check. Complementary to `gate v2`, not a replacement."
       :flags (into gate-common-flags
                    [{:flag "--profile" :doc "REQUIRED: a conformance-profile bundle directory -- PROFILE.xml required; CONSTRAINTS.xml, VALUESETS.xml, VALUESETBINDINGS.xml, COCONSTRAINTS.xml, SLICINGS.xml optional. No default. To try one: components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1"}])}]}
```

`gate-common-flags`: `--treat-no-verdict-as` drops `(ADR-0010)` (comment
above the def: `;; no-verdict folding policy: ADR-0010.`); others unchanged.

```clojure
    ;; Designators: ruling 7.
    {:group "check"
     :doc "Check a candidate corpus against an expected corpus and/or explicit per-file assertions -- the corpus's second judge, alongside gate. DIR also accepts a dir: URL designator."
     ...flags unchanged}

    ;; Honest pre-release identity ruling: D13.
    {:group "version"
     :doc "Print this repo's own pre-release identity (it deliberately has no semver yet) plus every pinned artifact's name@version from the lockfile."}

    {:group "doctor"
     :doc "Run SETUP.md's verification checklist as checks: java resolution via the artifact registry, artifact cache presence per lockfile entry, git hooksPath wiring, and platform support. Exit 0: every check passed; 1: at least one failed; 2: couldn't even read the lockfile to know what to check."}

    ;; In-process mount: ADR-0005/ADR-0012 fulfilled; the entry point is
    ;; ehrt.sim.interface/run-command.
    {:group "sim"
     :doc "Run the sim engine, in-process -- no subprocess, no fetched artifacts needed."
     :verbs
     [{:verb "run" :doc [unchanged]
       :flags [...unchanged except:
               {:flag "--format" :doc "\"er7\": bare wire messages to stdout (requires --emit hl7). \"ground-truth\": the bare ground-truth EDN vector -- pipe straight into `ehrt sim check`. Default: the full EDN envelope; --json works as always."}]}
      {:verb "check" :doc [unchanged -- already clean]}
      {:verb "identifiers" :doc [unchanged -- already clean]}
      {:verb "version" :doc "Print sim's own library version and git SHA -- the same source the run manifest's :generator block stamps."}]}

    ;; Display-vs-wire ruling: ADR-0013.
    {:group "show"
     :doc "Render a file (or a directory of files sharing one sniffed format) for a human: HL7 v2 (ER7) one segment per line, blank line between messages; FHIR JSON pretty-printed. Always pretty -- `ehrt show FILE | less` just works. The rendered ER7 is display-only and deliberately nonconformant (LF-joined segments): never pipe it anywhere a real HL7 v2 consumer sits."}

    ;; Pacer design: ADR-0014. Lexical-order contract: ADR-0015.
    ;; --sink designator vocabulary: ADR-0017; deferred sinks: ADR-0014.
    {:group "play"
     :doc "Pace an HL7 v2 (ER7) file's or directory's messages against their own MSH-7 timestamps and render (or write) them over time -- `ehrt show` plus time. A directory's files must share the v2 format and are concatenated in LEXICAL FILENAME ORDER before pacing: that ordering is the contract, so name files so sort order is play order (the sim generator's msg-%03d output already is). FHIR or mixed input is a named deferral (:play-input-unsupported)."
     :flags [...unchanged except:
             {:flag "--sink" :doc "a file: destination designator -- write the paced output (byte-identical to unpaced) there instead of showing the ticker. dir:, blaze:, and mllp: are recognized but deferred."}]}
```

Every `[unchanged]` above is literal: the current string is already
user-clean and stays byte-identical, so the landing diff is exactly the
strings shown plus the comments.

## 4. What this does NOT touch

The `ns` docstring, the def-level docstrings, `docs/cli.md` prose outside
the generated region (the session regenerates the generated region only),
and the wrap mechanics (session 5).

## 5. The gate the landing session co-lands

A `docs-tooling` deftest walking `cli-spec` as data: every rendered-string
position (`:doc`, `:meaning`, `:positional-doc`, `:default`, and
`top-level-doc`'s value) must not match `ADR-\d+`, `\bD\d{1,2}\b`,
`\bSS-\d+\b`, `\bruling \d+\b`, or `\bM\d+[a-z]?\b` (word-bounded, so
"MSH-7" and dates survive; the session verifies the patterns against every
CURRENT clean string before landing — a false positive on legitimate text
is a STOP, not a pattern loosened silently). Natural red today: 38 tokens.
Def-docstrings and comments are out of scope by construction — the gate
walks the data, not the file.
