(ns ehrt.corpus.simhospital-corpus
  "Test support (not `src/`): loads the vendored SimHospital corpus
  (`test-fixtures/v2/simhospital/messages.out`, ADR-0011) and names the
  hazard-selected slice the committed tests assert over. Shared by
  `corpus.er7`'s and `corpus.intake`'s corpus tests so both exercise the
  *same* messages -- the intake slice test is only meaningful as
  \"intake handles what er7 proved it can carry.\"

  Framing, measured rather than assumed (facts register F25, and
  `PROVENANCE.md` beside the corpus): segments within a message are
  CR-terminated -- classic ER7 -- but messages are separated from each
  other by a blank LF line, and the last segment of a message carries no
  CR. So the file splits into messages on \\n\\n (yielding a trailing
  empty block, since the file ends with one) and each message is
  internally CR-framed, exactly the shape `corpus.er7` consumes.

  Deliberately independent of `corpus.er7`: this namespace does its own
  splitting rather than calling the substrate, so selecting the slice
  cannot beg the question the round-trip test asks about that same
  substrate."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def corpus-path
  "test-fixtures/v2/simhospital/messages.out")

(def message-separator
  "A blank LF line between messages -- NOT the segment terminator, which
  is CR. See the namespace docstring."
  "\n\n")

(defn- load-messages
  "The corpus as a vector of raw ER7 message strings. `slurp` performs no
  newline translation (unlike, say, Python's text mode), so the CR
  segment terminators arrive intact -- which is what the framing guard
  in `er7-test` checks, since a *checkout* that normalized them would
  produce a file this function reads without complaint."
  []
  (let [raw (slurp (io/file corpus-path))
        blocks (str/split raw (re-pattern message-separator) -1)]
    ;; the file ends with the separator, so the final block is empty;
    ;; dropping it here is not canonicalization -- it is the absence of
    ;; a message, not a message whose content is "".
    (vec (cond-> blocks (= "" (last blocks)) butlast))))

(def ^:private loaded
  (delay (load-messages)))

(defn messages
  "The 1,013 messages, read once per JVM (1.1 MB; several test
  namespaces consume it)."
  []
  @loaded)

(defn segments
  "message -> its segments, split on the CR terminator only. Field-level
  splitting is `corpus.er7`'s job; this is the minimum needed to select
  messages by structure without depending on the code under test."
  [message]
  (str/split message #"\r" -1))

(defn segment-name
  [segment]
  (first (str/split segment #"\|" -1)))

(defn message-type
  "MSH-9 verbatim, e.g. \"ORU^R01\". Split-index 8 in the MSH segment,
  per the MSH off-by-one `corpus.er7`'s docstring documents: MSH-1 (the
  field separator) occupies no split slot."
  [message]
  (nth (str/split (first (segments message)) #"\|" -1) 8))

(defn- segment-count
  [message name]
  (count (filter #(= name (segment-name %)) (segments message))))

;; ---- the hazard slice ----
;;
;; Three messages, each earning its place by exhibiting a hazard the
;; hand-written fixtures in test-fixtures/v2/ chose the same way, and
;; each selected by a stable structural predicate rather than a byte
;; offset or a hard-coded index -- so the selection survives any future
;; re-vendoring of the corpus that keeps its structure.

(defn- first-matching
  [pred]
  (first (filter pred (messages))))

(defn hazard-slice
  "A vector of {:label :why :message}, in a fixed order.

  :pid-3-repetition   -- PID-3 carries a repetition (`MRN~NHS number`),
                         the repeat separator that field-level split/join
                         must carry verbatim rather than mis-split
                         around. This is the corpus-scale twin of the
                         hand-written
                         `adt-a01-admit-repeated-identifiers.hl7`
                         fixture. (Notable in its own right: all 1,013
                         messages qualify -- F25 -- so \"first matching\"
                         is a predicate that happens to land on the head
                         of the file, not an arbitrary pick.)

  :oru-long-obx-tail  -- the ORU^R01 carrying the most OBX segments (17,
                         the corpus maximum against a median of 2), with
                         NTE segments interleaved between them: many
                         repetitions of one segment name, and the
                         longest message shape the corpus contains.
                         Ties are broken by first occurrence, so the
                         choice is deterministic.

  :lone-adt-a34       -- the corpus's single ADT^A34 (patient merge), and
                         with it the only MRG segment anywhere in the
                         file -- a segment shape that occurs exactly
                         once and would otherwise never be exercised."
  []
  (let [max-obx (->> (messages)
                     (filter #(= "ORU^R01" (message-type %)))
                     (map #(segment-count % "OBX"))
                     (reduce max 0))]
    [{:label :pid-3-repetition
      :why "PID-3 repetition (MRN ~ NHS number) must survive field-level split/join"
      :message (first-matching
                (fn [m]
                  (some (fn [seg]
                          (and (= "PID" (segment-name seg))
                               (str/includes? (nth (str/split seg #"\|" -1) 3 "") "~")))
                        (segments m))))}
     {:label :oru-long-obx-tail
      :why "the corpus's longest OBX tail: many repeats of one segment name, NTEs interleaved"
      :message (first-matching
                (fn [m]
                  (and (= "ORU^R01" (message-type m))
                       (= max-obx (segment-count m "OBX")))))}
     {:label :lone-adt-a34
      :why "the only ADT^A34 in the corpus, and the only MRG segment"
      :message (first-matching #(= "ADT^A34" (message-type %)))}]))
