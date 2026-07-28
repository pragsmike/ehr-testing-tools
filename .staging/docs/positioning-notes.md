# Positioning notes (raw material — pre-positioning scratch)

**This is not a positioning document.** It's the raw material one will
eventually draw from — three observations mined from
[`docs/research/SimHospital-Synthea-limitations-considered.md`](research/SimHospital-Synthea-limitations-considered.md)
(retrieved 2026-07-26) that bear directly on how this project would
explain itself to someone comparing it against the two tools it mined.
Nothing here is a claim ready to ship in a README or a talk; it's notes
for whoever writes that document later, kept here so the mining session
that surfaced them isn't lost before that document exists.

## 1. Capacity realism is a differentiator

Synthea's own COVID-19 modeling paper concedes the tool "did not
constrain care or supplies by capacity" — its authors call the model's
output an *upper bound* on delivered care, not a resource-constrained
simulation, and note it would need extra pathways to represent
unavailable ventilators or other scarce resources (§4.3 of the research
report, citing Walonoski et al. 2020). This project's boarding
mechanic, ladder exhaustion, and cross-patient bed-ready coupling
(`docs/operational-models.md`, landed M1) are not a nice-to-have
add-on — they are exactly the thing Synthea's own authors say their
tool doesn't do. Worth saying plainly, eventually: *capacity is not
decoration on top of this simulator's clinical content, it emerges
from the same engine that produces the content.*

## 2. The gap is open — say so honestly

The report's own negative finding (§7, "Event-sourced alternative"):
no checkable project was found that positions itself as an
event-sourced successor to either SimHospital or Synthea. That's a
finding about the *market*, not a finding about this project's
technical merit, and the two shouldn't be conflated when this gets
written up. The honest framing is "this is an open opportunity," not
"the market has converged on this being the right architecture and we
built it" — the report explicitly warns against the latter overclaim
(§8, evidence gaps: "no public comparative experiment shows that an
event-sourced rewrite would improve realism, extensibility, or
correctness"). Whatever positioning document eventually gets written
should carry that same honesty rather than borrow more confidence from
this research than the research itself claims.

## 3. The log player answers a documented need

SyntheaWeb's own stated rationale (§4.5 of the research report) is that
Synthea's CLI is a significant technical barrier for clinical
researchers, and raw FHIR JSON output is an "interpretability gap" for
end users who aren't going to read JSON to understand a patient's
story. This project's own future log player / bed-board consumer
(named in `.agents/plans/roadmap.md`'s consumer plan, not yet built) is
aimed at exactly that same gap, independently arrived at — a human
wants to *watch* a simulated hospital, not grep its ground-truth log.
Worth citing SyntheaWeb specifically when that consumer's own
motivation gets written up, since it's independent confirmation the
need is real rather than a need this project invented to justify a
planned feature.

## Provenance

Every claim above traces to a specific section of
`docs/research/SimHospital-Synthea-limitations-considered.md`, cited
inline. Nothing here restates a fact from that report without a
pointer back to it — per `AGENTS.md`'s "do not invent facts about
upstream sources" discipline, this document treats the report as the
evidence artifact and does not re-verify or re-derive its findings.
