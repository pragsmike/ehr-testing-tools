# HL7v2 ER7 Encoding and MLLP Transport: A Reference for Tool Authors and Testers

## 1. ER7 Encoding

**ER7** ("Encoding Rules 7") is the correct and official name for the traditional pipe-delimited wire encoding of HL7v2 messages. The "7" refers to OSI Layer 7 (the application layer), reflecting HL7's origins as an application-level messaging standard [18][26]. HL7v2 messages are encoded according to the standard ER7 rules, which describe data segments, fields, components, and subcomponents [18]. ER7 is the right choice for almost all production exchanges, as opposed to the XML encoding alternative [9].

The ER7 encoding defines a set of delimiter characters declared in the MSH segment:

| Delimiter | Character | Purpose |
|---|---|---|
| Field separator | `\|` | Separates fields within a segment |
| Component separator | `^` | Separates components within a field |
| Repetition separator | `~` | Separates repeated values in a field |
| Escape character | `\` | Escapes special characters |
| Subcomponent separator | `&` | Separates subcomponents within a component |

These delimiters are declared in MSH-1 (field separator) and MSH-2 (encoding characters), allowing them to be redefined per message — a critical consideration for any parser or test tool [1][7].

**Key reference links:**

- [Caristix: HL7-ER7 Encoding](https://caristix.com/help-center/v3/test/task/hl7-er7-encoding/) [1]
- [ETLWorks: HL7 2.x Formats](https://support.etlworks.com/hc/en-us/articles/360014078373-HL7-2-x-Formats) [9]
- [Liechtenstein eHealth: HL7 Conformance Statement (PDF)](https://www.llv.li/serviceportal/de/amtsstellen/ehealthsolutions_va53a_hl7conformancestatement.pdf) [18]
- [NIST: HL7 over HTTP Specification](https://pages.nist.gov/v2plusDemo/hl7-over-http.html) [26]

---

## 2. The Hierarchical Message Structure

The HL7v2 message framework is explicitly hierarchical. The HL7 v2 Conformance Methodology describes the building blocks as "segment groups, segments, fields, and data types (i.e., components and sub-components)" [6]. Each level nests within the one above it:

| Level | Description | Delimiter / Boundary |
|---|---|---|
| **Message** | The complete unit, defined by a trigger event (e.g., ADT^A01) | — |
| **Segment group** | A named, nestable logical container of segments and/or sub-groups | Structural (inferred from message definition) |
| **Segment** | A logical grouping of related fields, identified by a 3-character Segment ID | Carriage return `\r` |
| **Field** | An individual data element within a segment | Pipe `\|` |
| **Component** | A sub-element of a complex (composite) field | Caret `^` |
| **Subcomponent** | The finest granularity of data | Ampersand `&` |
| **Repetition** | Multiple values within a single field position | Tilde `~` |

**Key reference links:**

- [HL7 V2 Conformance Methodology: Introduction](https://v2.hl7.org/conformance/HL7v2_Conformance_Methodology_R1_O1_Ballot_Revised_D9_-_September_2019_Introduction.html) [6]
- [Caristix: HL7-Definition V2 Home](https://hl7-definition.caristix.com/v2/) [16]
- [HL7 v2.5.1 Chapter 2](https://www.hl7.eu/HL7v2x/v251/std251/ch02.html) [17]
- [HL7 v2.5 Chapter 2](https://www.hl7.eu/HL7v2x/v25/std25/ch02.html) [21]
- [Microsoft BizTalk: HL7 Message Structure](https://learn.microsoft.com/en-us/biztalk/adapters-and-accelerators/accelerator-hl7/hl7-message-structure) [29]

---

## 3. Segments

A **segment** is a logical grouping of data fields that convey a particular set of related information [11]. Each segment begins with a unique three-character identifier called the **Segment ID** (e.g., MSH, PID, PV1, OBX) and terminates with a carriage return [7][13]. The first segment of every message is always **MSH** (Message Header), which carries the delimiters, message type, and version [16][22].

Common segments include:

| Segment ID | Name | Typical Content |
|---|---|---|
| MSH | Message Header | Delimiters, message type, version, sender/receiver |
| EVN | Event Type | Trigger event code and timestamp |
| PID | Patient Identification | Demographics: name, DOB, address, phone |
| NK1 | Next of Kin | Related person contact information |
| PV1 | Patient Visit | Visit/admission details |
| OBX | Observation/Result | Lab results, vital signs, clinical observations |
| ORC | Common Order | Order control, ordering provider |
| OBR | Observation Request | Test ordered, specimen details |
| DG1 | Diagnosis | Diagnosis codes and descriptions |
| ACC | Accident | Accident-related patient information |

Each segment exists independently and can be utilized in multiple message types [10]. Segments may be required or optional, and may occur only once or may repeat within a message [16][25].

**Key reference links:**

- [Saga IT: HL7 v2 Segment Reference](https://saga-it.com/docs/hl7/segments) [7]
- [Parsehog: HL7 Segment & Field Lookup](https://www.parsehog.com/hl7/lookup) [11]
- [InterSystems Community: What's HL7v2?!](https://community.intersystems.com/post/whats-hl7v2) [13]
- [Rhapsody: HL7 Segments](https://rhapsody.health/resources/hl7-segments/) [10]
- [HL7 v2.3.1 Segment List (Caristix)](https://hl7-definition.caristix.com/v2/HL7v2.3.1/Segments) [3]
- [AHRQ: Templates for using HL7 v2.5 messages](https://hcup-us.ahrq.gov/datainnovations/clinicaldata/TemplatesforusingHL7v2.jsp) [22]
- [NLM/NIH: HL7 Version 2](https://www.nlm.nih.gov/oet/ed/healthdatastandards/03-300.html) [23]

---

## 4. Segment Groups and the Abstract Message Definition

Segments are organized into **segment groups** — named logical containers that can nest segments and even other groups, providing structural hierarchy within a message [6][20]. The abstract message definition for each message type (e.g., ADT_A01, ORU_R01) specifies which segments and groups appear, in what order, and whether each is required, optional, or repeating [29][21].

The abstract syntax uses bracket notation:

- `{ SEG }` — the segment or group may repeat
- `[ SEG ]` — the segment or group is optional
- Bare segment — required, exactly once

For example, the ADT_A01 abstract message definition includes:

```
ADT_A01
├── MSH          (required, once)
├── { SFT }      (optional, repeating — software segment)
├── EVN          (required, once)
├── PID          (required, once)
├── [ PD1 ]      (optional — patient demographics)
├── { ROL }      (optional, repeating — roles)
├── { NK1 }      (optional, repeating — next of kin)
├── PV1          (required, once)
├── [ PV2 ]      (optional — visit info)
├── { ROL }      (optional, repeating)
├── ... further optional groups (DG1, PR1, GT1, IN1, ACC, etc.)
```

Groups are structural constructs defined by the message type's abstract syntax; they do not have their own delimiters in the ER7 wire format. Parsers infer group membership from segment ordering and the message structure definition [9][20]. This implicit grouping is a critical challenge for tool authors: a parser must consult the message structure definition to correctly assign segments to groups.

**Key reference links:**

- [HL7 V2 Conformance Methodology: Introduction](https://v2.hl7.org/conformance/HL7v2_Conformance_Methodology_R1_O1_Ballot_Revised_D9_-_September_2019_Introduction.html) [6]
- [Google Cloud Healthcare API: HL7v2 Custom Parser](https://docs.cloud.google.com/healthcare-api/docs/how-tos/hl7v2-custom-parser) [20]
- [Microsoft BizTalk: HL7 Message Structure](https://learn.microsoft.com/en-us/biztalk/adapters-and-accelerators/accelerator-hl7/hl7-message-structure) [29]
- [Nebraska DHHS: HL7 2.5.1 Implementation Guide (PDF)](https://dhhs.ne.gov/epi%20docs/HL7-2.5.1-Guide.pdf) [12]

---

## 5. Annotated Example: ADT^A01 Message

The following is a complete ER7-encoded ADT^A01 (patient admission) message with each hierarchical level annotated [27][30]:

```
MSH|^~\&|HIMS01|CALADAN HOSPITAL|LAB01|CALADAN HOSPITAL|20231115123246||ADT^A01^ADT_A01|934576120110613083617|P|2.8||||
EVN|A01|20110613083617|||
PID|1||135769||ATRIEDES^LETO^I^||19870628|M|||House of Atriedes^Castle^Caladan^Caladan^Dune||(0)161-123-4567^^^leto.I@atriedes.caladan|||||1719|||||Caladan||||||||||||||||
NK1|1|JESSICA^LADY|WIFE||||||
PV1|1|O|||||^^^^^^^^|^^^^^^^^
```

### 5.1 Segment Level

Each line is a **segment**, terminated by a carriage return. The first three characters are the Segment ID [7][30]:

| Segment ID | Meaning | Role in this message |
|---|---|---|
| MSH | Message Header | Declares delimiters, message type, version |
| EVN | Event Type | Records the trigger event (A01 = admit) |
| PID | Patient Identification | Demographics: name, DOB, address, phone |
| NK1 | Next of Kin | Related person contact |
| PV1 | Patient Visit | Visit/admission details |

### 5.2 Field Level

Within each segment, the pipe (`|`) separates **fields**. The MSH segment fields:

| Position | Field | Value |
|---|---|---|
| 1 | Field Separator | `\|` |
| 2 | Encoding Characters | `^~\&` |
| 3 | Sending Application | HIMS01 |
| 4 | Sending Facility | CALADAN HOSPITAL |
| 5 | Receiving Application | LAB01 |
| 6 | Receiving Facility | CALADAN HOSPITAL |
| 7 | Date/Time | 20231115123246 |
| 8 | Security | (empty) |
| 9 | Message Type | ADT^A01^ADT_A01 |
| 10 | Message Control ID | 934576120110613083617 |
| 11 | Processing ID | P |
| 12 | Version ID | 2.8 |

### 5.3 Component and Subcomponent Level

Field 9 (`ADT^A01^ADT_A01`) is a **complex field** — the caret (`^`) separates its **components**: message code (ADT), trigger event (A01), and message structure (ADT_A01) [30].

The PID segment's patient name field demonstrates deeper nesting:

```
PID|1||135769||ATRIEDES^LETO^I^||19870628...
                    |       |    |
                    |       |    └─ Prefix (component 3)
                    |       └────── Given Name (component 2)
                    └────────────── Family Name (component 1)
```

If a component itself had sub-parts, the ampersand (`&`) would delimit **subcomponents** — the finest granularity in the hierarchy [27][30].

### 5.4 Repetition Level

The tilde (`~`) indicates that a field **repeats** within the same position. For example, a phone field with two numbers: `(0)161-123-4567~(0)161-987-6543` [30].

**Key reference links:**

- [Saga IT: HL7 v2 Sample Messages](https://saga-it.com/docs/hl7/samples) [27]
- [Innovate Cybersecurity: Securing the Weakest Link — HL7 v2 Protocol](https://innovatecybersecurity.com/news/securing-the-weakest-link-hl7-v2-protocol-in-healthcare-networks/) [30]
- [Ringholm: HL7 Message Examples (v2 and v3)](https://ringholm.com/docs/04300_en.htm) [32]

---

## 6. Network Transport: MLLP

HL7v2 messages are most commonly transmitted using the **Minimum Lower Layer Protocol (MLLP)**, sometimes called "Minimal Lower Layer Protocol," which runs over raw TCP/IP [30][31]. MLLP is a thin framing layer that solves a specific problem: TCP is a stream protocol with no concept of message boundaries, so the receiver needs a way to know where one HL7 message ends and the next begins.

### 6.1 MLLP Frame Structure

MLLP wraps each ER7 message in a frame using three control bytes [30]:

| Byte | Hex | Meaning |
|---|---|---|
| Start block | `0x0B` | Marks the beginning of an HL7 message |
| End block | `0x1C` | Marks the end of the HL7 data |
| Carriage return | `0x0D` | Terminates the frame |

The complete on-wire structure:

```
[0x0B] MSH|^~\&|...full ER7 message... [0x1C] [0x0D]
```

The receiving system reads bytes from the TCP stream until it sees the `0x1C 0x0D` sequence, at which point it knows it has a complete message to parse [30].

### 6.2 Acknowledgement Flow

After receiving a message, the recipient sends back an **ACK (acknowledgement)** message, also MLLP-framed. The ACK contains an MSA segment with an acknowledgement code [30]:

```
MSH|^~\&|LAB01|CALADAN HOSPITAL|HIMS01|CALADAN HOSPITAL|20231124201917||ACK^A01|35175470||2.5
MSA|AA|934576120110613083617
```

The MSA segment carries the acknowledgement code (`AA` = accept, `AE` = error, `AR` = reject) and echoes back the sender's Message Control ID so the sender can correlate the response [30].

### 6.3 Port and Security Considerations

IANA has assigned **port 2575** as the standard port for HL7 over MLLP, though implementations frequently use non-standard ports [30]. A critical characteristic of MLLP is that it provides **no encryption, authentication, or message integrity** — the HL7 standard itself is "largely silent about the issues of privacy, authentication and confidentiality of data" [30]. Messages are typically sent in cleartext, making network-level protections (TLS, VPNs, VLAN segmentation) essential [30][28].

**Key reference links:**

- [Innovate Cybersecurity: Securing the Weakest Link — HL7 v2 Protocol](https://innovatecybersecurity.com/news/securing-the-weakest-link-hl7-v2-protocol-in-healthcare-networks/) [30]
- [InterfaceWare: Secure Protocols for HL7](https://help.interfaceware.com/kb/164) [28]
- [TXOne: HL7 Protocol Vulnerabilities and Mitigation](https://www.txone.com/resources/blog/hl7-protocol-vulnerabilities-mitigation/) [34]
- [NIST: HL7 over HTTP Specification](https://pages.nist.gov/v2plusDemo/hl7-over-http.html) [26]

---

## 7. Alternative Transport Mechanisms

While MLLP dominates real-time system-to-system exchange, other transports are used in practice [31][34][26]:

| Transport | Use Case | Characteristics |
|---|---|---|
| MLLP over TCP | Real-time interface engine connections | Most common; no built-in security |
| MLLP over TLS/SSL | Secure real-time exchange | Adds encryption to MLLP framing [28] |
| SFTP/FTPS | Batch file exchange | Messages dumped to files, processed nightly [31][34] |
| HL7 over HTTP | Web-based transport | Wraps ER7 or XML messages in HTTP requests/responses [26] |
| SOAP/REST APIs | Modern integrations | More common with HL7 v3 and FHIR [31] |
| File transfer (FTP, Kermit) | Legacy batch exchange | Messages grouped in files and transferred [33] |

The MLLP framing bytes are sometimes visible even when TLS is in use — a packet capture on an unencrypted link will show the `0x0B` start byte immediately followed by the `MSH|` segment header, which is how tools like Wireshark identify HL7v2 traffic [30].

**Key reference links:**

- [NIST: HL7 over HTTP Specification](https://pages.nist.gov/v2plusDemo/hl7-over-http.html) [26]
- [InterfaceWare: Secure Protocols for HL7](https://help.interfaceware.com/kb/164) [28]
- [TXOne: HL7 Protocol Vulnerabilities and Mitigation](https://www.txone.com/resources/blog/hl7-protocol-vulnerabilities-mitigation/) [34]
- [HL7 V2.3.1 Introduction (transport methods)](https://www.hl7.eu/HL7v2x/v231/std231/CH1.html) [33]
- [Wikipedia: HL7](https://en.wikipedia.org/wiki/HL7) [35]

---

## 8. Practical Notes for Tool Authors and Testers

### 8.1 Parser Considerations

- **Delimiters are per-message.** Always read MSH-1 and MSH-2 to determine the delimiter set; do not hardcode `|^~\&`.
- **Group boundaries are implicit.** A parser must consult the abstract message definition for the message type to correctly assign segments to groups. There are no wire-level group delimiters [9][20].
- **Segment termination.** Segments are terminated by a carriage return (`\r`, 0x0D), not a line feed or `\r\n`. Some real-world systems emit `\r\n`, so robust parsers should handle both.
- **Trailing delimiters.** Trailing field separators at the end of a segment are common and generally should be tolerated.

### 8.2 MLLP Framing for Test Tools

- When building an MLLP sender for testing, always wrap messages with `0x0B` ... `0x1C 0x0D`.
- When building an MLLP receiver, read the TCP stream until you encounter `0x1C` followed by `0x0D`, then deliver the content between the start and end bytes as one message.
- Test tools should generate and validate ACKs, including the three acknowledgement codes (AA, AE, AR).

### 8.3 Testing Tips

- Use sample messages from the [Saga IT sample catalog](https://saga-it.com/docs/hl7/samples) [27] or [Ringholm's examples](https://ringholm.com/docs/04300_en.htm) [32] as test fixtures.
- Test with optional segments present and absent to verify group-assignment logic.
- Test with repeating segments (e.g., multiple NK1 or OBX segments) to verify repetition handling.
- Test with non-default delimiter sets to verify MSH-1/MSH-2 parsing.
- Test MLLP framing edge cases: partial frames, multiple messages in one TCP send, and messages split across TCP segments.
- For security testing, verify that cleartext MLLP traffic can be intercepted, and test TLS-wrapped MLLP configurations.

---

## Complete Reference List

1. [Caristix: HL7-ER7 Encoding](https://caristix.com/help-center/v3/test/task/hl7-er7-encoding/)
2. [HL7 V2 Conformance Methodology: Introduction](https://v2.hl7.org/conformance/HL7v2_Conformance_Methodology_R1_O1_Ballot_Revised_D9_-_September_2019_Introduction.html)
3. [Caristix: HL7 Standard Segment List (v2.3.1)](https://hl7-definition.caristix.com/v2/HL7v2.3.1/Segments)
7. [Saga IT: HL7 v2 Segment Reference](https://saga-it.com/docs/hl7/segments)
9. [ETLWorks: HL7 2.x Formats](https://support.etlworks.com/hc/en-us/articles/360014078373-HL7-2-x-Formats)
10. [Rhapsody: HL7 Segments](https://rhapsody.health/resources/hl7-segments/)
11. [Parsehog: HL7 Segment & Field Lookup](https://www.parsehog.com/hl7/lookup)
12. [Nebraska DHHS: HL7 2.5.1 Implementation Guide (PDF)](https://dhhs.ne.gov/epi%20docs/HL7-2.5.1-Guide.pdf)
13. [InterSystems Community: What's HL7v2?!](https://community.intersystems.com/post/whats-hl7v2)
16. [Caristix: HL7-Definition V2 Home](https://hl7-definition.caristix.com/v2/)
17. [HL7 v2.5.1 Chapter 2](https://www.hl7.eu/HL7v2x/v251/std251/ch02.html)
18. [Liechtenstein eHealth: HL7 Conformance Statement (PDF)](https://www.llv.li/serviceportal/de/amtsstellen/ehealthsolutions_va53a_hl7conformancestatement.pdf)
20. [Google Cloud Healthcare API: HL7v2 Custom Parser](https://docs.cloud.google.com/healthcare-api/docs/how-tos/hl7v2-custom-parser)
21. [HL7 v2.5 Chapter 2](https://www.hl7.eu/HL7v2x/v25/std25/ch02.html)
22. [AHRQ: Templates for using HL7 v2.5 messages](https://hcup-us.ahrq.gov/datainnovations/clinicaldata/TemplatesforusingHL7v2.jsp)
23. [NLM/NIH: HL7 Version 2](https://www.nlm.nih.gov/oet/ed/healthdatastandards/03-300.html)
26. [NIST: HL7 over HTTP Specification](https://pages.nist.gov/v2plusDemo/hl7-over-http.html)
27. [Saga IT: HL7 v2 Sample Messages](https://saga-it.com/docs/hl7/samples)
28. [InterfaceWare: Secure Protocols for HL7](https://help.interfaceware.com/kb/164)
29. [Microsoft BizTalk: HL7 Message Structure](https://learn.microsoft.com/en-us/biztalk/adapters-and-accelerators/accelerator-hl7/hl7-message-structure)
30. [Innovate Cybersecurity: Securing the Weakest Link — HL7 v2 Protocol](https://innovatecybersecurity.com/news/securing-the-weakest-link-hl7-v2-protocol-in-healthcare-networks/)
32. [Ringholm: HL7 Message Examples (v2 and v3)](https://ringholm.com/docs/04300_en.htm)
33. [HL7 V2.3.1 Introduction](https://www.hl7.eu/HL7v2x/v231/std231/CH1.html)
34. [TXOne: HL7 Protocol Vulnerabilities and Mitigation](https://www.txone.com/resources/blog/hl7-protocol-vulnerabilities-mitigation/)
35. [Wikipedia: HL7](https://en.wikipedia.org/wiki/HL7)
