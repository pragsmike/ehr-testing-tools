<!-- Attic file: notes/adr/0041-gmf-coverage-wave-i2.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0041 — GMF coverage Wave I2: the last two — parity frontier CLOSES

**Status:** Accepted (author-ruled 2026-08-04, design channel, AR-1
through AR-4 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
2026-08-04.

### Context

Post-I census (`2026-08-04-synthea-7e08387-wave-i.edn`, ADR-0040 AR-7):
82/85 `:ok-walked`, 1 `:out-of-scope-by-ruling`, 2 `:walk-failed` —
both unmasked findings from Wave I's own six-mechanism landing, neither
fixed there (real design/scope work, not a mechanical vocabulary
completion): `congestive-heart-failure`'s `Dead_within_28_days` state
(and its three siblings) use Death's `:condition-onset`/`:referenced-
by-attribute` cause-of-death forms — a named, disclosed, UNBUILT
limitation from Wave C (ADR-0028 C1/C2's own docstring: "no vendored
module needs them yet"; one now does). `wellness-encounters` (via its
own closure member, `encounter/depression_screening.json`) uses
`:active-careplan` (Active CarePlan) — a log-query family member never
built. Both pinned at `7e08387c68a7f0e21d13076609a159fd473fc902`.

### Decision

**AR-1 (Death cause forms) — a corrected reading of the pin, found live.**
`State.java`'s own `Death.process` was re-read fresh this session (not
re-derived from docs/gmf-interpreter.md section 10's own C1 paraphrase,
which turns out to have the priority order BACKWARDS): the real
if/else-if chain checks `:codes` FIRST, then `:condition-onset`, then
`:referenced-by-attribute` — never the "condition-onset, referenced-
by-attribute, codes" order section 10's own prose account states. No
vendored Death state this project has ever combines more than one
form (`stroke.json`: `:codes` alone; `congestive_heart_failure.json`'s
own four Death states: `:referenced-by-attribute` alone, confirmed by
direct read — none carries `:condition-onset` at all), so the ordering
is proven this session by fixture tests, not by any real closure's own
co-presence. Section 10 gets a dated resolution note correcting the
paraphrase.

`:condition-onset` resolves via the SAME `index-of-citation` shape
`ConditionEnd`'s own `:condition-onset` field already uses — the named
state's own `:codes` when it fired on this walk, else nil. This is a
disclosed, NOT-ported simplification against the real source: upstream
has a SECOND fallback when `person.hadPriorState` is false (read the
named state's own JSON-declared `:codes` directly off the module,
regardless of whether it ever actually fired) — not built, since no
vendored module needs it and this project's own trajectory-query idiom
has no "read a state's declared content without having walked it"
mechanism to reuse.

`:referenced-by-attribute` reads the SAME index-based indirection
`:medication-order`/`:medication-end`'s own `:assign-to-attribute`/
`:referenced-by-attribute` pair already establishes — ported onto
`:condition-onset` this session (found live, necessary: without it,
`congestive_heart_failure.json`'s own `chf` attribute, which its `CHF
Condition Start` ConditionOnset state authors `assign_to_attribute`
for, would never be written, and the referenced-by-attribute form
would always see an absent attribute). Absent attribute resolves to
nil — a disclosed departure from upstream's own behavior (a
`RuntimeException`, "referenced but not set"): chosen because this
project's own SetAttribute/assign-to-attribute family already treats a
not-yet-written attribute as an honest, non-fatal absence everywhere
else it reads one (`:value-attribute`'s own `containsKey` guard,
ADR-0040 AR-2), and Death's own cause is supplementary content (feeds
a terminal event's own `:codes` field, never gates the walk's own
progress the way a Guard's condition does) — never a module-authoring-
shape bug this interpreter refuses to run over.

**AR-2 (`:active-careplan` condition) — the parent class read, per the
ruling's own instruction.** `Logic.java`'s `ActiveCarePlan` class is a
four-method override of an abstract parent, `ActiveLogic`, which is
where the actual dispatch lives (read in full, not merely the
subclass):

```java
private abstract static class ActiveLogic extends Logic {
  protected List<Code> codes;
  protected String referencedByAttribute;
  ...
  public boolean test(Person person, long time) {
    if (this.codes != null) {
      for (Code code : this.codes) {
        if (checkCode(person, code)) { return true; }
        ...
      }
      return false;
    } else if (this.referencedByAttribute != null) {
      if (person.attributes.containsKey(this.referencedByAttribute)) {
        return checkAttribute(person, (HealthRecord.Entry) person.attributes.get(this.referencedByAttribute));
      } else {
        return false;
      }
    }
    throw new RuntimeException(...);
  }
}
public static class ActiveCarePlan extends ActiveLogic {
  boolean checkCode(Person person, Code code) { return person.record.careplanActive(code.code); }
  boolean checkAttribute(Person person, Entry entry) { return person.record.careplanActive(entry.type); }
  ...
}
```

`:codes` is checked FIRST (matching ANY listed code); `:referenced-by-
attribute` is checked ONLY when `:codes` is absent, and — the detail
the subclass alone would hide — `checkAttribute` RE-TESTS the
referenced entry's own active status (`careplanActive`), never merely
"the attribute exists." Confirmed the real vendored use,
`depression_screening.json`'s own `Check Eligibility` At-Least guard
(reached via `wellness-encounters.json`'s own `Depression Screening`
CallSubmodule), uses the `:codes` form only (`{"condition_type":
"Active CarePlan", "codes": [{"system": "SNOMED-CT", "code":
"183401008", ...}]}`) — `:referenced-by-attribute` is installed per
the ruling's own "both forms" instruction, proven by a hand-built ctx
in this session's own tests, not yet vendored-exercised. Neither form
present resolves to FALSE here (not upstream's own throw) — no real
closure authors a bare Active CarePlan condition with neither, and a
malformed condition map is this project's own `evaluate-condition`
unsupported-condition-type throw's job one layer up, not a second,
narrower one here. No active careplan (either form): FALSE, the
natural answer — activity is what this condition tests, the SAME
distinction ADR-0040 AR-3's own observation-condition ruling draws
between "not yet true" and "unconfigured input."

**AR-3 (oracle bracket — pure identity).** A fresh recursive scan of
every currently-vendored root (the regression-oracle's own 9 roots,
`components/sim/resources/sim/modules` plus the `death-fixture.json`
test fixture) for both mechanisms, run before any edit: zero hits for
Death's `:condition-onset`/`:referenced-by-attribute` forms and for
`:active-careplan` anywhere in the current vendored catalog — the
implementation is purely additive by construction, not merely by
intent. `bin/regression-oracle dd6a9d4 14e8dce`: all 9 roots IDENTICAL
(Verification baselines, below).

**AR-4 (census + the parity declaration) — MET.** Re-run (same
params, disambiguated filename,
`2026-08-04-synthea-7e08387-wave-i2.edn`): **84 `:ok-walked`, 1
`:out-of-scope-by-ruling` (`gallstones`, Physiology, ADR-0037 AR-5),
ZERO `:load-failed`, ZERO `:walk-failed`** — both `congestive-heart-
failure` and `wellness-encounters` now walk clean (`:ok-walked`,
`:walk-errors []`), and no other module's own verdict shifted. This is
exactly the countable definition ADR-0031 AR-4 states ("the census
shows zero load failures and every walked module's own smoke-walk
digest recorded") — **PARITY ACHIEVED, at pin
`7e08387c68a7f0e21d13076609a159fd473fc902`**, dated 2026-08-04, the
artifact named above. The dated declaration note lands on the parity
plan §1 and the interpreter doc's own census section; the roadmap
retires this row and names Wave H the sole remaining wave (per
ADR-0031 AR-7's own re-ordering, already in place).

### Verification baselines

`bin/regression-oracle dd6a9d4 14e8dce` (the tip before Step 1 → this
session's own single landing commit): all 9 vendored root batches
IDENTICAL — `appendicitis`, `death-fixture`, `ear-infections`,
`ear-infections-engine`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement-engine`, `urinary-tract-infections-engine`.
AR-3's pure-identity claim holds, byte-verified. `clojure -M:poly
check` clean.

### Execution record

**Step 1+2 (AR-1 + AR-2, `14e8dce`).** Landed as ONE commit rather
than the two the session prompt named — AR-1/AR-2 touch the same two
source files (`gmf.clj`, `gmf-interpreter.clj`) in adjacent,
non-overlapping regions; a clean split would cost more than it buys,
the same disclosed shape ADR-0040 AR-5 already took for the identical
reason. `death-cause-codes` (new) resolves all three cause forms per
AR-1's own priority order; `:condition-onset`'s `step` case gains
`:assign-to-attribute` (mirrors `:medication-order`'s own case
verbatim); `active-careplan-condition-holds?`/`careplan-active-by-
reference?` (new) resolve `:active-careplan` per AR-2.
`condition-type->keyword` gains an explicit `"Active CarePlan"` entry
(grep-ability, matching the map's own stated registry discipline — the
slug fallback would already have produced the same keyword).
`:condition-onset`'s own loader schema gains `:assign-to-attribute`.
Red→green: the two retired tests (`death-throws-on-unbuilt-condition-
onset-cause-form`/`death-throws-on-unbuilt-referenced-by-attribute-
cause-form`) asserted throws the pre-fix code actually produced; the
post-I census's own walk-errors show the identical `:active-careplan`
"unsupported condition type" throw for real, pre-fix. 13 new tests
(net +11 against the 2 retired), covering both cause forms, the
priority order, the assign-to-attribute wiring, and both Active
CarePlan forms including the ended-entry/never-started/never-written
negatives. Full sim-trajectory-adjacent suite green: 299 tests, 802
assertions, 0 failures/errors
(`gmf-interpreter-test`, `gmf-test`, `death-fixture-test`, `compile-
trajectory-test`, `vendored-ear-infections-test`).

**Step 3 (oracle bracket).** See Verification baselines, above.

**Step 4 (census + parity determination, this commit).** See AR-4,
above, for the full classification. `2026-08-04-synthea-7e08387-
wave-i2.edn` committed (the census tool's own same-day filename
collision bug, ADR-0034/ADR-0035's own precedent, worked around by
hand-copy again, unfixed, named again).

`gitleaks git -v`: clean, every commit.

### Fence

No Wave H mechanics (pre-roll, fold boundary — untouched, and now the
SOLE remaining wave per this ADR's own AR-4). No census-tool
overwrite-bug fix (worked around by hand-copy, per ADR-0035's own
precedent). Upstream's own second `:condition-onset` fallback (read a
named state's own declared codes without having walked it) and
upstream's own throw-on-missing-`:referenced-by-attribute` are both
named, disclosed departures, not ported — a future session finding a
real closure that needs either reopens this record, does not silently
extend it.

---

