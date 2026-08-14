## ADR-0133 — Exact-name state resolution: collision fix, vendoring-rider row (Step 1 of 4)

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-14.

### Context

Chartered from `.agents/plans/roadmap.md`'s own "Vendoring rider:
per-pair collision corrections, 5 modules" row (ADR-0131). That row's
own original framing assumed the fix would be PER-MODULE JSON
corrections (renaming one member of each colliding pair in the
vendored source, a real content decision, oracle-declaring whichever
roots move) and would trigger the load-time collision guard's own
charted escalation from WARN to hard-error.

This session's own driving prompt supersedes that framing with a new,
verbatim author ruling (`.agents/rulings.md` gets this entry):

- **Resolution option "b" — loader-side exact-name resolution.** A
  raw-name -> key table is built at load time; colliding raw names get
  deterministic disambiguated keys; every name-valued reference
  resolves by EXACT raw string through that table, never through
  `slug`. Vendored module JSONs stay verbatim (ADR-0071's own
  vendoring precedent preserved untouched; NOTICE hashes do not move).
- **Riding (b):** the WARN -> hard-error escalation ADR-0131 chartered
  is DISCHARGED, not executed as originally planned — collisions are
  now handled (both members load as real, distinct states), not merely
  tolerated-and-announced. The guard's own warning becomes a
  disambiguation DISCLOSURE (still to `*err*`, new text, never a load-
  blocking condition on its own). The one sanctioned NEW strictness:
  a name-valued reference that misses the table is a load REJECTION
  (`:unresolved-state-reference`) — stronger than today's silent
  dangling keyword, and the only content-behavior addition this
  session makes beyond RESOLUTION.

### Step 1a — Collision census, re-derived against the live tree

Re-run from scratch (a direct Python walk of all 66 module JSONs
under `components/sim/resources/sim/modules/`, recursive, `slug`
re-implemented byte-for-byte from `gmf.clj`'s own current fold set —
comma plus the reader's thirteen terminating-macro characters, plus
the pre-existing `_`/whitespace fold — the SAME fold ADR-0131 already
landed and this session does not touch):

**10 collision pairs across 5 distinct module files — EXACT match to
ADR-0131's own recorded census, no drift:**

| Module | Slug | Colliding raw keys |
|---|---|---|
| `colorectal_cancer.json` | `postoperative-care` | `Postoperative Care` / `Postoperative_Care` |
| `hypothyroidism.json` | `hypothyroidism` | `Hypothyroidism` / `hypothyroidism` |
| `injuries.json` | `end-dme` | `End DME` / `End_DME` |
| `injuries.json` | `postoperative-care` | `Postoperative Care` / `Postoperative_Care` |
| `sleep_apnea.json` | `2nd-assessment` | `2nd Assessment` / `2nd_Assessment` |
| `sleep_apnea.json` | `intraoral-appliance` | `Intraoral Appliance` / `Intraoral_Appliance` |
| `sleep_apnea.json` | `home-cpap-unit` | `Home CPAP Unit` / `Home_CPAP_Unit` |
| `sleep_apnea.json` | `nasal-mask-supplies` | `Nasal Mask Supplies` / `Nasal_Mask_Supplies` |
| `veteran_ptsd.json` | `columbia-suicide-risk-assessment` | `Columbia Suicide Risk Assessment` / `Columbia_Suicide_Risk_Assessment` |
| `veteran_ptsd.json` | `phq2-q9-assessment` | `PHQ2_Q9 Assessment` / `PHQ2_Q9_Assessment` |

No other module in the 66-file tree carries a collision. STOP
condition (count disagrees with ADR-0131's recorded 10/5) did NOT
fire.

**Hypothyroidism's own chain, direct-read confirmed** (the driving
prompt's own named restored-semantics witness): `Hypothyroid symptom`
(Symptom) `direct_transition` -> `Hypothyroidism` (Symptom)
`direct_transition` -> `hypothyroidism` (Symptom) `direct_transition`
-> `Hypothyroid Condition Onset` (ConditionOnset). All four are
distinct, real states; the middle two ARE the collision pair. Today,
the `hypothyroidism` -> `ConditionOnset` transition edge and the
`Hypothyroidism` state's own SetAttribute-adjacent Symptom content
collapse onto whichever of the two the JSON parser processes last
(`json/read-str`'s own key-fn application order, not file order,
confirmed below) — one is silently unreachable.

### Step 1b — Complete inventory of name-valued fields

The driving prompt's own channel walker found seven field categories,
46 `PriorState` refs, ~2,331 total. Re-derived from `gmf.clj`'s own
live `normalize-*` call sites (every `(keyword (slug ...))` call site
that resolves a RAW STATE NAME, not a vocabulary term — `grep -n
"keyword (slug" gmf.clj`, then classified each hit by hand): the
walker undercounted, as the driving prompt itself predicted. The
REAL inventory is **twelve** field categories, not seven — five more
found live, each a genuine state-name back-reference this loader
already resolves via `(keyword (slug t))` at `normalize-state`'s own
`:else` cond-> (gmf.clj:742-753), never touched by the transition-
only walker:

| Field | Owning state type | Refs found |
|---|---|---|
| `direct_transition` | any | 1,420 |
| `transition` (distributed/conditional/complex/lookup entries, incl. nested `:distributions`) | any | 1,212 |
| `type_of_care_transition` (map values) | Simple | 3 |
| `PriorState` condition `:name` | any condition context (`:allow`, transition entries' `:condition`, recursively under And/Or/Not/At-Least) | 46 |
| `target_encounter` | ConditionOnset/AllergyOnset/Immunization-family | 114 |
| `condition_onset` **(new, undercounted by the walker)** | ConditionEnd — back-reference to the ConditionOnset it closes | 26 |
| `careplan` **(new)** | CarePlanEnd — back-reference to the CarePlanStart it closes | 8 |
| `medication_order` **(new)** | MedicationEnd — back-reference to the MedicationOrder it closes | 6 |
| `device` **(new)** | DeviceEnd — back-reference to the Device it closes | 4 |

**Grand total: 2,839 name-valued references** across all 66 modules
(not ~2,331 — the walker's own undercount, exactly as the driving
prompt predicted it would be). `:encounter-class`/`:action` are
vocabulary fields (closed enums), NOT name-valued, correctly excluded.
The lookup-table CSV weight-column matcher (`parse-lookup-table`,
gmf.clj:1631/1648) matches a CSV header string against an ALREADY-
resolved `:transition` keyword set — a different data source (CSV
files, not JSON module text) — checked for overlap with the 5
collision modules below (Step 1d) and found none; out of this
session's own fence.

**Zero exact-match misses** (a direct script re-walk of all 2,839
refs against each module's own raw state-name set, per-module):
strict-miss rejection is safe to land as the sanctioned new
strictness — no existing reference in the live tree will trip it.

### Step 1c — Capture-avoidance check, against the REAL `slug`

The driving prompt's own charter: "no existing raw name slugs to any
candidate disambiguation key... capture-avoiding by construction, not
by luck." Empirical check (candidate keys `<base>-2` through
`<base>-5`, the plain-hyphen suffix form a naive implementation might
reach for first): **zero captures** in the live tree.

Live-tree absence is not construction, so the fix does NOT use a
plain-hyphen suffix. `slug`'s own fold collapses every run of 2+
hyphens to one (`(str/replace #"-{2,}" "-")`, gmf.clj:79) — no string
`slug` can EVER produce contains a doubled hyphen. The disambiguation
suffix uses `--N` (a literal double hyphen) as the separator: `<base>`
for the first-occurring member of a collision group, `<base>--2`,
`<base>--3`, ... for the rest, in file order. No raw name's own plain
slug can EVER equal a disambiguated key, for any module, any content,
by the shape of `slug`'s own output alphabet — a property, not a
scan result. (The generative test in Step 2 proves this against
`raw-gmf-name-gen`, the same generator ADR-0131's own round-trip
property already exercises.)

### Step 1c′ — A load-bearing ordering defect found live, not anticipated

"First occurrence in file order keeps the bare slug key" requires a
reliable file-order read of each module's own top-level `"states"`
object keys. `raw-state-names` (gmf.clj:1318-1327, ADR-0131) computes
this via `(-> json-text json/read-str (get "states") keys)` — checked
directly against `clojure.data.json` 2.5.2's own `read-object`
(`~/.m2/.../data.json-2.5.2.jar`, decompiled and read): objects with
**more than 8 entries** upgrade from `PersistentArrayMap` (insertion-
ordered) to `PersistentHashMap` (hash-ordered, NOT file order),
confirmed empirically —

```
(keys (json/read-str "{\"a\":1,...,\"j\":10}"))
=> (d f e j a i b g h c)   ; NOT a b c d e f g h i j
```

Every one of the 5 collision-bearing modules' own `"states"` object
has far more than 8 entries (18-200 states each) — `raw-state-names`
was NEVER giving file order for any real module, only ADR-0131's own
WARN-mode guard (order-independent: it prints one warning per group,
regardless of which raw name is listed first) never depended on this,
so the defect was latent, not yet load-bearing. This session's own
"first occurrence in file order" requirement makes it load-bearing
for the first time. Fixed by scanning `json-text` directly (string-
and-escape-aware, brace/bracket-depth-tracked, using `json/read-str`
itself only to unescape an already-delimited key substring — never
reimplementing JSON string escaping) rather than trusting the parsed
map's own iteration order. Verified against all 66 modules: identical
KEY SETS to the standard parse (no key lost, no duplicate), and hand-
verified in-order output on the 5 collision modules (e.g.
`hypothyroidism.json`: `Hypothyroidism` precedes `hypothyroidism` —
the first keeps the bare `:hypothyroidism` key, the second becomes
`:hypothyroidism--2`).

### Step 1d — Closure census: which oracle roots carry any of the 5 modules

`components/oracle/src/ehrt/oracle/digest.clj`'s own `roots` map: 35
entries, unchanged in membership since ADR-0131 (re-counted directly).
Grepped every vendored module for `"submodule"` references naming any
of the 5 collision-bearing modules by call-path: **none found** — all
5 are called ONLY as their own root (never as a `CallSubmodule` target
from another module), so exactly 5 of the 35 roots are touched:
`colorectal`, `hypothyroidism`, `injuries`, `sleep-apnea`,
`veteran-ptsd` — the same 5 ADR-0131 already named as "WARN, no move."

**Structural reachability, all 20 collision-pair members, both raws
of every pair, BFS from each module's own `Initial` state over
`direct_transition`/`distributed_transition`/`conditional_transition`/
`complex_transition`/`type_of_care_transition` edges:** every single
one reachable. (Necessary, not sufficient — ADR-0131's own
`veteran-lung-cancer` counter-example proved structural reachability
alone can still be empirically dark at a fixed seed/population; this
session's own Step 3 oracle run is the actual empirical confirmation,
matching that same discipline.)

### Prediction (declared-oracle-change declaration)

Recorded BEFORE any `src` edit, per this session's own driving
prompt's declared-oracle-change requirement (ADR-0131/ADR-0086
precedent). The fix changes RETURNED behavior only for a module whose
own raw state names collide under `slug` — for every one of the other
61 modules, `name->key`'s table is identity-equivalent to the old
`(keyword (slug raw))` transform (no group has more than one member),
so every root whose own closure contains ONLY non-colliding modules
is predicted byte-identical, including every root that shares a
submodule (`anemia/anemia_sub`, `dme/wheelchair`, `medications/*`,
`snf/*`, `injuries/broken_jaw`) with one of the 5 — those submodules
are themselves collision-free and normalize identically regardless of
which root calls them.

- **MOVE (5 roots):** `colorectal`, `hypothyroidism`, `injuries`,
  `sleep-apnea`, `veteran-ptsd` — previously-overwritten states resume
  executing as real, distinct, correctly-routed states; real
  trajectory content changes predicted (not merely a warning-text
  change).
- **NO MOVE (30 roots):** everything else — pure identity.
- **The 10 ADR-0131-recorded `WARN:` lines are REPLACED** by
  disambiguation disclosures (new text, still `*err*`-only, still
  invisible to `digest.clj`'s own `-main`, which only ever captures a
  producer function's RETURNED value — confirmed by direct re-read,
  `components/oracle/src/ehrt/oracle/digest.clj:589-591`, ADR-0131's
  own finding, unchanged).

Step 3 re-runs the official `bin/regression-oracle` bracket; this
prediction must match exactly (5 movers, 30 identical, 10 disclosures
in place of 10 warnings) or this session STOPs.

### Fences

Committed this step: this ADR file, `notes/ADRs.md` index line,
`.agents/plans/roadmap.md`'s vendoring-rider row (prediction recorded
in place, row stays open — closes in Step 4). Zero `src`/`test`/module
JSON touched — docs-only, matching ADR-0131's own Step 1 precedent.
