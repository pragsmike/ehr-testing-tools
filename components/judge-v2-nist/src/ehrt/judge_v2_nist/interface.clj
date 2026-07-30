(ns ehrt.judge-v2-nist.interface
  "The NIST-backed HL7 v2 PROFILE-tier engine's delegation surface --
  third judge engine, sibling of ehrt.judge-v2-hapi (base-structural
  tier) and ehrt.judge-fhir-official. Unqualified gate-file/gate-dir,
  same reasoning as judge-v2-hapi's interface: per-engine interfaces
  have nothing to qualify against; ehrt.tools.interface applies its own
  qualification (`v2-nist-gate-file` suggested) at its re-export layer.

  This engine validates against an IGAMT-exported conformance-profile
  bundle (Π): PROFILE.xml required; CONSTRAINTS.xml, VALUESETS.xml,
  VALUESETBINDINGS.xml, COCONSTRAINTS.xml, SLICINGS.xml optional. It
  therefore checks what the HAPI tier structurally cannot: profile
  usage/cardinality/length, conformance statements, co-constraints,
  slicing, and value-set bindings. Complementary gate, not a
  replacement (ADR-0012: direct-engine adoption, msg-id contract,
  Cause growth)."
  (:require [ehrt.judge-v2-nist.v2 :as v2]))

(def make-validator v2/make-validator)
(def gate-file v2/gate-file)
(def gate-dir v2/gate-dir)
