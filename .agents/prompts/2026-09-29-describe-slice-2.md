# describe slice 2: the manifest records what each arrival was assigned, and the config it ran (ADR-0183)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-29-describe-slice-2.md`, which records
> where the live tree differed from this prompt. Steps 3 and 4 share
> `components/sim/src/ehrt/sim/run.clj`, so they landed as one commit.
> The channel's expectations held: `:schema-version` stays "1.1" (no
> test pins the manifest's key set), and `:config :hashed` marks which
> bytes were hashed.

Context: ADR-0183 names slice 2 (notes/adr/0183-*.md:110-117): (1) a manifest assignment record --
which pathway and module each ordinal was assigned -- so describe's pathway rows stop reading
`:unprovable` and module rows stop inferring from `:configured-detail`; (2) the manifest's `:config` is
always `{:path "(inline)" :sha256 "000..."}` because `merge-config-file` drops the path (run.clj:378-381)
and run-command hard-codes the placeholder (run.clj:906-907). Provenance only: no ground-truth byte,
no draw. Baseline f4251226. Downstream generates with `--format ground-truth` (no envelope), so a
`--manifest` sidecar path beside a bare log is part of the slice.

Read first:
- components/sim-engine/src/ehrt/sim_engine/run.clj:330-600 (prelude: pathway-for, module-for, steps-for,
  registered-entry-for, bindings, first-ordinal, owner-ordinal), 1483-1545 (final-result, extra)
- components/sim/src/ehrt/sim/run.clj:357-388 (merge-config-file), 850-915 (run-command, manifest/build call)
- components/sim/src/ehrt/sim/manifest.clj:46-90; components/provenance/src/ehrt/provenance/manifest.clj:84-107
- components/sim/test/ehrt/sim/manifest_test.clj; components/corpus/src/ehrt/corpus/generators.clj:195-245
- components/sim-check/src/ehrt/sim_check/describe_catalog.clj:60-80, 275-300 (pathway rows); describe.clj (identity)
- docs/formats.md (manifest + describe report sections); docs/consuming-ground-truth.md (describing a corpus)

Author ruling 2026-09-29: "next" -- slice 2 lands now. Channel expectations (not rulings; correct from
the tree): manifest `:schema-version` stays "1.1" (open map, additive key) unless a provenance test pins
the key set -- then stop and report; `:config :sha256` is the sha256 of the FILE BYTES when `--config`
was given (downstream's own pin convention) and, for an inline config, of `(pr-str engine-params)`
with `:path "(inline)"` and a `:hashed :file | :engine-params` marker so a reader knows which.

Steps:
1. Red tests. (a) engine: `run/run`'s result carries `:assignments`, one entry per ordinal
   `{:ordinal :patient-id :pathway <name|nil> :module <id|nil> :person-id <|nil> :repeat? :placeholder?}`,
   and for a fixture with an explicit-ordinal cohort the entries match the config; (b) sim: run-command
   with `--config <file>` writes `:config {:path <as given> :sha256 <sha of file bytes> :hashed :file}`;
   inline writes `:hashed :engine-params` and a non-zero sha; (c) describe: pathway rows read
   `:configured :yes :observed <n>` with witnesses when the envelope (or `--manifest`) carries
   `:assignments`, and `:unprovable` as today when it does not; module rows are observed from the record,
   not inferred; (d) `--manifest PATH` beside a bare log yields `:identity {:source :bare-log+manifest}`.
   Invariant: all red at f4251226 for the stated reasons only.  Gate: brick:sim-engine, brick:sim,
   brick:sim-check show these only.
2. Engine: capture the pathway NAME and module id at the EXISTING resolution sites in prelude (the
   `steps-for`/`registered-entry-for` call and `module-for`) -- never a second `pathway-for` call --
   plus binding, owner and placeholder facts already computed; hand `:assignments` out through
   `final-result`'s extra on every exit path (normal and exhausted).
   Invariant: `bin/ground-truth-bracket f4251226 HEAD` (no flag) IDENTICAL on every root -- the proof
   no draw was added.  Gate: banner; step 1(a) green.
3. Manifest: run-command threads the config path through `merge-config-file` (return it beside the
   merged opts; the two rejection paths unchanged) and writes real `:config`; `manifest/build` gains
   `:assignments` from the engine result. `corpus generate sim`'s manifest.edn carries it verbatim.
   Invariant: provenance's `valid-v1-1?` still passes on the built manifest; manifest_test green.
   Gate: brick:sim, brick:provenance, brick:corpus green; poly check OK.
4. Describe: `--manifest PATH` for a bare log; pathway rows observed from `:assignments` (assigned
   count; patients with at least one non-`:registered` event; first-k witnesses by first event index);
   module rows observed from assignments joined to `:citation :module`; golden report regenerated and
   the diff shown in the record; `:describe-version` 1.0.0 -> 1.1.0 (additive: new identity source, rows
   change from :unprovable to counts).  Invariant: a report over an envelope WITHOUT `:assignments`
   (any pre-slice-2 corpus) is unchanged except the version.  Gate: step 1(c)(d) green; brick:sim-check.
5. Docs: formats.md manifest section (`:assignments`, `:config :hashed`) and describe section (1.1.0,
   `--manifest`); consuming-ground-truth.md: "generate with the envelope or keep manifest.edn beside
   events.edn"; cli.md regenerates. ADR-0183 gains a dated "Slice 2, landed" section; no new ADR.
   Invariant: `make docsgen && git diff --exit-code` clean.
6. `bin/regression-oracle f4251226 HEAD` (no flag).  Invariant: IDENTICAL on every root (the manifest
   is not digested; nothing rendered moves).  Gate: banner.
7. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: "regenerate with the envelope, or keep manifest.edn beside events.edn, and describe
   answers configured-versus-realized per pathway; your retained bare-vector corpus is unchanged."
