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
[glossary](GLOSSARY.md).

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
{;; 1–2: your units
 :facility {:id "stmarys"
            :wards [{:id "ED"    :name "Emergency"  :beds 20
                     :surge-slots 8 :surge-format "%s-H%02d" :class :ed}
                    {:id "MED4W" :name "4 West Med" :beds 24
                     :surge-slots 4 :surge-format "%s-HALL-%d" :class :inpatient}
                    {:id "TELE"  :name "Telemetry"  :beds 16
                     :surge-slots 0 :class :inpatient}]}

 ;; 3: your people (synthetic names; NPIs are generated, valid-format)
 :providers [{:name {:family "Okafor" :given "A."} :role :attending
              :specialty "Hospital Medicine" :wards ["MED4W" "TELE"]}
             {:name {:family "Reyes" :given "M."} :role :attending
              :specialty "Emergency Medicine" :wards ["ED"]}]

 ;; 4: your payer mix
 :payers [{:id :medicare   :name "Medicare"        :type :medicare   :weight 45}
          {:id :bcbs       :name "BCBS Commercial" :type :commercial :weight 35}
          {:id :medicaid   :name "State Medicaid"  :type :medicaid   :weight 15}
          {:id :self       :name "Self-pay"        :type :self-pay   :weight 5}]

 ;; clinical content: weighted scenarios and/or disease modules.
 ;; A patient may have a pathway OR a module, never both -- a pathway
 ;; that admits/visits AND a module (which also opens its own
 ;; encounter) would double-book one patient's single visit and is
 ;; rejected at config time, before a run ever starts.
 :pathways [{:pathway "admit-cbc-discharge" :weight 3}
            {:pathway "simple-admission"    :weight 7}]
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
  :code-tables {:patient-class {:inpatient "IN" :outpatient "CLI"}
                :discharge-disposition {:home "01H"}}
  :z-segments [{:segment "ZPI"
                :on [:admission]
                :fields [[:persona :payer :type]
                         [:location :ward]
                         "STM-PAYER-V2"]}]}}
```

Field names above are illustrative of the real schema — the shipped
examples in [demos/](demos/) are always exact and runnable; start
from one of those and edit.

## How do I run it, and get messages?

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

You wouldn't have to hunt for it; you can list it exactly. The same
config-plus-seed that generated a corpus regenerates it byte-for-byte, so the
complete inventory of every identifier it ever contained (MRNs, visit numbers,
message control IDs, FHIR resource ids) is always recoverable — feed that list
to your system's purge tooling. On the FHIR side, every resource additionally
carries the standard HTEST "test data" security label and a generator tag, so
it's searchable and bulk-deletable by query. On the HL7v2 side, synthetic
identifiers are deliberately fingerprinted (invalid-range SSNs, documented MRN
and control-ID formats). And the write-safety gates exist so this question stays
hypothetical.

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
- **Network delivery.** Output is files/streams; MLLP delivery is
  deliberately out of scope here (point your interface engine's
  file reader at it, or ask about the tools sibling).
- **Broken-feed conditions.** This simulator *always* produces
  coherent, ordered traffic — that's a guarantee, not a gap.
  Out-of-order, dropped, or mangled delivery is injected downstream
  by the [tools](https://github.com/pragsmike/ehr-testing-tools)
  sibling, with a record of exactly what was damaged.

## Who do I ask?

If your facility has a quirk this page doesn't cover — a location
scheme, a code value, a workflow — there's a good chance it's in
[clinical-realities.md](clinical-realities.md) already (post-mortem
transfers, hallway parking, newborn merges, results after
discharge...). If it isn't, that catalog is exactly where it should
be added: open an issue describing the reality, and how you'd know
the simulation got it right.
