# Simulate *your* facility — an FAQ for domain experts

You know your hospital: which units board patients in hallways, what
your registration system calls an observation stay, which Z-segment
your downstream systems will reject a feed without. This page is for
you. It answers, in order: what you can customize, what information
to gather, where it goes, how to run it, how to check the output
"sounds like us," and — honestly — what this simulator can't do for
you yet.

You will not write code. Everything below is one configuration file.
Unfamiliar words (ours or the domain's) are in the
[glossary](glossary.md).

<!-- MAINTENANCE: the example config below must be validated against
     config.clj's schemas (m/validate) whenever schemas change; the
     committing session for this file should run that check. -->

## What can I actually customize today?

Two kinds of knobs, and the distinction matters:

**Knobs that change what happens** — your facility's structure and
behavior:
- **Units and beds**: your ward list, bed counts per ward, how many
  hallway/surge spots each has, and the naming scheme for them
  (every hospital labels hallway spots differently; yours is
  config, not our guess).
- **Load**: how fast patients arrive relative to your capacity.
  Crowding effects — boarding, hallway placement, transfers when a
  bed frees — are not scripted; they *emerge* from pressure, the
  way they do in your building. Want a corpus that feels like your
  worst Tuesday? Turn up arrivals.
- **People**: a provider pool (names, roles, which wards they
  cover) and a payer mix (Medicare/Medicaid/commercial/self-pay
  weights; age-linked automatically).
- **Mess**: how often transfers get cancelled, discharges get
  reversed, beds get swapped, duplicate registrations get merged —
  the administrative churn that breaks interfaces, at rates you
  choose.
- **Clinical content**: which visit/order scenarios run and how
  often (weighted), which disease modules drive patient
  trajectories, which lab panels exist (with real LOINC codes,
  units, and reference ranges).

**Knobs that change only how it's written down** — your dialect:
- Your **message-header identity** (sending/receiving application
  and facility, the HL7 version your systems expect in MSH-12).
- Your **local code values** — what YOUR site puts in patient class
  (PV1-2) and discharge disposition (PV1-36), including values no
  standard table lists.
- Your **Z-segments** — house segments with fields drawn from
  patient state (payer type, ward, whatever your downstream
  expects).

The guarantee behind the split: dialect never changes the underlying
events. Run the same seed with two different site profiles and you
get the *same hospital day* written in two accents — which also
means you can share findings across configurations meaningfully.

## What information should I gather first?

The site interview — most analysts can fill this from memory:

1. Unit list: name, bed count, hallway/surge spot count, and your
   labeling scheme for surge spots (e.g. `ED-H01`, `4W-HALL-3`).
2. Which unit is your ED.
3. A handful of attending physicians per unit (names can be
   invented — see the safety note below).
4. Your rough payer mix, as percentages.
5. Your MSH identities: sending app/facility, receiving
   app/facility, HL7 version.
6. Your local patient-class and disposition code values, if they
   differ from the standard tables (they do).
7. Any Z-segments your downstream systems expect, and what data
   rides in each field.
8. Optional, for realism tuning: rough rates — cancellations per
   hundred admissions, how often your ED boards, typical
   length-of-stay range. Summary numbers only.

## Where does it all go?

One EDN file (EDN is JSON's tidier cousin; edit it in any text
editor). An annotated sketch — section by section, matching the
interview:

```clojure
{;; 1–2: your units (ward :id is a keyword; :surge-format is required
 ;; even at zero surge slots -- it's just never used to render one)
 :facility {:id :stmarys
            :wards [{:id :ed    :name "Emergency"  :beds 20
                     :surge-slots 8 :surge-format "%s-H%02d" :class :ed}
                    {:id :med4w :name "4 West Med" :beds 24
                     :surge-slots 4 :surge-format "%s-HALL-%d" :class :inpatient}
                    {:id :tele  :name "Telemetry"  :beds 16
                     :surge-slots 0 :surge-format "%s-HALL-%d" :class :inpatient}]}

 ;; 3: your people (synthetic names; NPIs are generated, valid-format;
 ;; :wards references ward :id keywords, not names)
 :providers [{:name {:family "Okafor" :given "A."} :role :attending
              :specialty "Hospital Medicine" :wards [:med4w :tele]}
             {:name {:family "Reyes" :given "M."} :role :attending
              :specialty "Emergency Medicine" :wards [:ed]}]

 ;; 4: your payer mix (:id is a string, not a keyword)
 :payers [{:id "medicare" :name "Medicare"        :type :medicare   :weight 45}
          {:id "bcbs"     :name "BCBS Commercial" :type :commercial :weight 35}
          {:id "medicaid" :name "State Medicaid"  :type :medicaid   :weight 15}
          {:id "self"     :name "Self-pay"        :type :self-pay   :weight 5}]

 ;; clinical content: weighted scenarios and/or disease modules. A
 ;; :pathways entry's own :pathway is a full pathway definition
 ;; (:name + :steps), not a name reference -- there is no named-
 ;; pathway registry to resolve a bare string against. :location
 ;; matches a ward's :name (above), not its :id. A patient may have a
 ;; pathway OR a module, never both -- a pathway that admits/visits AND
 ;; a module (which also opens its own encounter) would double-book
 ;; one patient's single visit and is rejected at config time, before
 ;; a run ever starts.
 :pathways [{:pathway {:name "admit-cbc-discharge"
                        :steps [{:type :admission :location "4 West Med"}
                                {:type :order :profile :cbc}
                                {:type :discharge}]}
             :weight 3}
            {:pathway {:name "simple-admission"
                        :steps [{:type :admission :location "4 West Med"}
                                {:type :delay :from 60 :to 240}
                                {:type :discharge}]}
             :weight 7}]
 ;; :modules IS a vector of name strings -- resolved against
 ;; resources/modules/<name>.json, unlike :pathways above.
 :modules ["sinusitis"]

 ;; 8: your mess, at your rates
 :churn-profile {:cancel-transfer 0.05 :cancel-discharge 0.02
                 :bed-swap 0.03 :merge 0.01}

 ;; 5–7: your dialect (changes rendering only, never the events)
 :site-profile
 {:name "stmarys-prod-dialect"
  :msh {:version "2.5.1"
        :sending-app "STM-EHR" :sending-facility "STMARYS"
        :receiving-app "RHAPSODY" :receiving-facility "STM-HUB"}
  ;; each code-table entry is a map ({:code ... :coding-system
  ;; optional}), not a bare string
  :code-tables {:patient-class {:inpatient {:code "IN"} :outpatient {:code "CLI"}}
                :discharge-disposition {:home {:code "01H"}}}
  :z-segments [{:segment "ZPI"
                :trigger #{:admission}
                :fields [{:path [:persona :payer :type]}
                         {:path [:location :ward]}
                         {:literal "STM-PAYER-V2"}]}]}}
```

This example is schema-checked against `ehrt.sim.config`,
`ehrt.sim.pathway`, `ehrt.sim.persona`, and
`ehrt.sim-emit-hl7.site-profile`'s malli schemas (2026-07-27,
`notes/facts-register.md` F18) — every field name and value shape
above is real, not just illustrative. The shipped examples in
[demos/](../components/sim/docs/demos/) are always exact and runnable; start from one of
those and edit.

## How do I run it, and get messages?

(First time running this at all? [`SETUP.md`](../SETUP.md) covers
installing the three prerequisites and verifying they work — come
back here once `clojure -X:test` passes.)

```bash
clojure -M:cli run --seed 42 --patients 40 --churn \
        --config stmarys.edn --emit hl7
```

Same seed + same file = byte-identical output, every time, on any
machine. That means: pin a seed you like and it's a permanent test
case; share the config file instead of gigabytes of messages; and
when something interesting happens at seed 4217, everyone can see
exactly the same thing.

## How do I check it "sounds like us"?

Three passes, cheapest first: read a page of messages next to a page
of your real feed (identities, code values, Z-segments should look
native); confirm the operational texture (with load turned up, you
should see boarding in YOUR hallway labels, transfers when beds
free, and your configured churn); and if you have downstream test
environments, point the output at one — messages arrive as files
today (see "not yet" for delivery).

## Is any of this a privacy risk?

No real patient exists anywhere in the pipeline — the generator is
rules plus public statistical tables, so there is nothing to leak,
by construction. Two practices keep it that way on your side: invent
provider names rather than listing your actual staff, and when
tuning realism, provide *summary statistics* (rates, mixes) — never
excerpts of your real feed.

## What if synthetic data ever reached a real system — how would we find and remove it?

You wouldn't have to hunt for it; you can list it exactly. Run
`ehrt sim identifiers --seed <seed> --patients <n> [--config <file>]` against the
same config-plus-seed that generated the corpus and get back the complete
inventory of every identifier it ever contained — patient ids, MRNs (every one,
including any merged away), visit-relevant ids, message control IDs, FHIR
resource ids, provider NPIs, and the run's own id — as machine-readable EDN;
feed that list straight to your system's purge tooling. On the FHIR side, every
resource additionally carries the standard HTEST "test data" security label and
a generator tag, so it's searchable and bulk-deletable by query. On the HL7v2
side, synthetic identifiers are deliberately fingerprinted (invalid-range SSNs,
documented MRN and control-ID formats). And the simulator itself only ever
writes files — nothing in it sends data to any server.

## What can't it do yet? (Honest list)

- **Learn your rates automatically.** Today you hand-tune churn and
  load to your observed numbers; a calibration mode that fits them
  from your summary statistics is designed but not built.
- **Guarantor (GT1) and version-specific message restructuring.**
  The MSH-12 version is your literal; segment layouts don't reshape
  per version yet.
- **Your order catalog wholesale.** Lab panels ship with a starter
  set (real LOINC); adding your own panels is a config-format away
  rather than a today feature — ask.
- **Network delivery.** Output is files/streams; actually sending
  it live over the wire is deliberately out of scope here (point
  your interface engine's file reader at it). MLLP *framing* itself
  is available, though — see the piped `corpus mutate`/`corpus
  intake` workflow in [Mutate's own output, piped straight into intake -- no intermediate directory](use-cases/mutate-output-piped-straight-into-intake.md).
- **Broken-feed conditions.** This simulator *always* produces
  coherent, ordered traffic — that's a guarantee, not a gap.
  Out-of-order, dropped, or mangled delivery is injected downstream
  by [`ehrt corpus mutate`](operators.md), with a record of exactly
  what was damaged.

## Who do I ask?

If your facility has a quirk this page doesn't cover — a location
scheme, a code value, a workflow — there's a good chance it's in
[clinical-realities.md](../components/sim/docs/clinical-realities.md) already (post-mortem
transfers, hallway parking, newborn merges, results after
discharge...). If it isn't, that catalog is exactly where it should
be added: [open an issue](https://github.com/pragsmike/ehrt.sim/issues) describing the reality, and how you'd know
the simulation got it right.
