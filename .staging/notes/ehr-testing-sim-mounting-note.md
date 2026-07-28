# Note to the ehr-testing-tools maintainer: the `sim` mounting seam

**From:** ehr-testing-sim
**Subject:** What sim relies on in your CLI architecture, so refactors
don't silently break the `ehr sim` mount.
**Suggested disposition:** record as an ADR in tools (this is a
cross-repo interface commitment, which is exactly what your ADR
discipline exists for), and leave a one-line comment at the dispatch
site pointing at it.

## Why this note exists

ehr-testing-sim was designed to mount into `ehr` as a subcommand group
with roughly four lines of change on your side. That ease is not an
accident — it is a consequence of five specific properties of your CLI
architecture (mostly your ADR-0004). None of them is individually
precious, but each is load-bearing for the mount. If a refactor
preserves these five properties, sim keeps working no matter how much
else moves; if one changes, the mount breaks in ways that may not
surface until runtime, because nothing in tools' compile path touches
sim today.

## The five load-bearing properties

**1. Dispatch is a data-in/data-out function keyed on `[group action]`
positionals.** Sim mounts as one arm in your dispatch `case`:
`"sim" (sim-cli/dispatch-action action opts)`. What sim assumes:
dispatch receives the *parsed* form — a string action and an opts map
with coercions already applied — and returns a Result map to a shell
that renders and exits. Safe: renaming `dispatch`, reorganizing arms,
adding groups, moving the `case` into data-driven routing (a map of
group → handler is fine — sim's `dispatch-action` already has that
signature). Breaking: dispatching on raw unparsed argv, requiring
handlers to print or `System/exit` themselves, or passing opts in a
different shape per group.

**2. One babashka.cli parse with a merged spec.** Sim exports
`cli-spec` (currently `:seed`, `:patients`, `:arrival-gap`, `:json`,
`:help` coercions) to be merged into yours before parsing. What sim
assumes: flag coercion happens once, host-side, before dispatch.
Two hazards: (a) *collision* — sim's `:seed`/`:json` deliberately
match your existing coercions, but if either repo adds a flag the
other coerces differently, last-merge-wins silently; a startup
assertion that merged specs agree on shared keys would convert that
to a loud failure. (b) *coercion drift* — if you move off babashka.cli
or change auto-coercion behavior (e.g., the digit-string identifier
carve-out you already document for `:reference-date`), sim's flags
inherit the change invisibly.

**3. Result maps are structurally typed.** Sim's capability functions
return `{:status :ok|:rejected|:error, :category kw, :payload any}`
built by sim's own `result` ns — a deliberate copy of your doctrine,
not a dependency (the arrow points tools → sim only). Your shell
renders and exit-codes these maps without knowing which namespace
constructed them. Safe: adding *optional* keys, adding categories,
extending the exit-code table (sim only produces statuses mapping to
0/1/2; your `:no-verdict`/3 arm is unused by sim). Breaking: required
new keys, records/protocols in place of plain maps, or interpreting
`:category` values globally rather than per-status — sim's category
vocabulary (`:unknown-command`, `:missing-required-opt`,
`:invariant-violation`, ...) is its own.

**4. Help is data your machinery walks.** Sim exports `help-group` in
your help-spec shape: `{:group, :doc, :verbs [{:verb, :doc, :flags
[{:flag, :doc, :default}]}]}`, appended to your `:groups` vector so
`ehr help sim` renders through your existing code. Safe: any change
to *rendering*. Breaking: reshaping the help data model without a
shim. Note your own coverage test that walks help data against the
CLI surface will start covering sim's group for free once mounted —
that's a feature; let it.

**5. The `-fn` injection pattern in dispatch.** Your tests stub
command functions via injectable keys. Keep an injection point for the
sim arm (`:sim-fn`, defaulting to `sim-cli/dispatch-action`) so your
CLI tests never load the simulation engine. Sim's own dispatch has the
same pattern internally, so both repos' tests stay hermetic.

## The sixth commitment: the manifest schema

Sim emits run manifests shaped to your `corpus/manifest.clj`
ManifestV1_1 so `ehr corpus intake` can ingest a sim run. Sim holds a
*mirror* of that schema with a tripwire test, but a mirror can only
detect drift after the fact. Two asks:

- **Version, don't mutate.** Your own V0→V1→V1.1 discipline (frozen
  historical schemas, explicit versions) is exactly right — please
  keep it. Sim targets a named version and migrates deliberately.
- **Host the binding contract test.** A test in your
  `test-integration/` tree that runs `sim run` and validates the
  emitted manifest against your authoritative schema (and, ideally,
  round-trips it through `corpus intake`) turns cross-repo drift into
  a failing test in the repo that owns the schema. Sim cannot host
  this test without inverting the dependency arrow.

## Summary for the dispatch-site comment

> The "sim" arm mounts ehr-testing-sim per its ADR-0001. Preserve:
> parsed-[group action]-in / Result-map-out dispatch; single merged
> babashka.cli spec; structural Result typing; help-group data shape;
> the -fn injection point. Manifest schema changes require a version
> bump; the binding contract test lives in test-integration/. See
> notes/ADRs.md ADR-NNNN (this commitment) before refactoring any of
> these.
