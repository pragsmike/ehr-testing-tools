(ns ehrt.corpus-io.mllp-test
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (g)):
  the socket half of `:mllp`.

  ADR-0014 deferred this and named exactly two things a transport sink
  would have to be judged on -- FRAMING and ACK PAIRING. Both are
  gated here, and the deferral's own sentence is what sets the bar: 'a
  half-built network sink with no ACK handling and untested lifecycle
  is a worse outcome than a clearly named deferral.'

  Every server in this namespace is a LOOPBACK responder on 127.0.0.1
  at an EPHEMERAL port. An ACK is RECEIVED, never emitted by this
  project's generator: `ehrt.corpus-io.mllp/ack-server!` is test
  apparatus that happens to live in src because the demo exerciser
  needs the same responder the tests do."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [ehrt.corpus-io.framing :as framing]
            [ehrt.corpus-io.mllp :as mllp]
            [ehrt.corpus-io.source-sink :as ss]
            [ehrt.corpus-io.source-sink-url :as url]
            [ehrt.kernel.interface :as kernel]))

;; ---- the kind, and the implication rule ----------------------------

(deftest mllp-is-an-implemented-sink-kind
  (is (contains? ss/known-sink-kinds :mllp))
  (is (contains? ss/implemented-sink-kinds :mllp)))

(deftest mllp-sink-implies-mllp-framing
  (testing "ADR-0175 section 2(g): a `:mllp` sink IMPLIES `:framing
            :mllp`, and declaring any other framing on it is a
            construction-time ERROR rather than a silent override --
            the one sentence that pays for the kind/framing name
            collision"
    (is (kernel/ok? (ss/mllp-sink {:host "127.0.0.1" :port 2575 :format :v2-er7})))
    (is (kernel/ok? (ss/mllp-sink {:host "127.0.0.1" :port 2575 :format :v2-er7 :framing :mllp})))
    (doseq [wrong [:er7-multi :file-per-item :ndjson :bundle-entries]]
      (let [r (ss/mllp-sink {:host "127.0.0.1" :port 2575 :format :v2-er7 :framing wrong})]
        (is (kernel/rejected? r) (str "framing " wrong " was accepted on a :mllp sink"))
        (is (= :invalid-sink (:category r))))))
  (testing "and the ordinary Sink requirements still bite"
    (is (kernel/rejected? (ss/mllp-sink {:host "127.0.0.1" :port 2575})) "no :format")
    (is (kernel/rejected? (ss/mllp-sink {:port 2575 :format :v2-er7})) "no :host")
    (is (kernel/rejected? (ss/mllp-sink {:host "h" :format :v2-er7})) "no :port")
    (is (kernel/rejected? (ss/mllp-sink {:host "h" :port "2575" :format :v2-er7}))
        "a string port is rejected here -- a socket needs a number")))

(deftest mllp-designator-parses-and-round-trips
  (testing "ADR-0175 section 2(g): `--sink mllp://host:port` costs no
            new flag -- it inherits ADR-0017's designator vocabulary
            and its round-trip parse/print law"
    (let [r (url/parse-sink-designator "mllp://127.0.0.1:2575?format=v2-er7")]
      (is (kernel/ok? r))
      (is (= {:kind :mllp :host "127.0.0.1" :port 2575 :format :v2-er7} (:payload r)))
      (is (= "mllp://127.0.0.1:2575?format=v2-er7"
             (:payload (url/print-sink-designator (:payload r))))))
    (testing "a non-numeric port is named rather than silently dropped"
      (is (kernel/rejected? (url/parse-sink-designator "mllp://h:notaport?format=v2-er7"))))
    (testing "and a bad framing on the designator is the same construction error"
      (is (kernel/rejected?
           (url/parse-sink-designator "mllp://127.0.0.1:2575?format=v2-er7&framing=er7-multi"))))))

;; ---- the no-drift gate ---------------------------------------------

(def ^:private mllp-src "components/corpus-io/src/ehrt/corpus_io/mllp.clj")
(def ^:private framing-src "components/corpus-io/src/ehrt/corpus_io/framing.clj")

(deftest the-sink-never-reimplements-the-codec
  (testing "the codec is never duplicated (this sweep's own fence, and
            ADR-0175 section 2(g)'s 'the sink reuses the codec rather
            than re-implementing it'). Shaped like
            `oracle_coverage_test`'s gate over `bin/oracle-lib.sh`:
            name the functions, and prove the bytes live in exactly one
            file."
    (let [sink (slurp mllp-src)
          codec (slurp framing-src)]
      (is (str/includes? sink "framing/encode"))
      (is (str/includes? sink "framing/decode"))
      (doseq [literal ["0x0B" "0x1C" "0x0D" "\\u000b" "\\u000B"]]
        (is (not (str/includes? sink literal))
            (str "the MLLP sink carries the frame byte literal " literal
                 " -- the envelope belongs to ehrt.corpus-io.framing alone")))
      (testing "and the gate is not vacuous: the codec really does own them"
        (is (str/includes? codec "0x0B"))
        (is (str/includes? codec "0x1C"))
        (is (str/includes? codec "0x0D"))))))

;; ---- framing round trip over a real corpus -------------------------

(def ^:private fixture-messages
  (->> (.listFiles (java.io.File. "test-fixtures/v2"))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".hl7"))
       (sort-by #(.getName ^java.io.File %))
       (mapv slurp)))

(deftest a-real-corpus-survives-the-wire-byte-for-byte
  (testing "R-empty-population-is-red"
    (is (pos? (count fixture-messages))))
  (let [{:keys [port received-fn stop!]} (mllp/ack-server!)]
    (try
      (let [opened (mllp/open-sink! "127.0.0.1" port)
            {:keys [send-fn failure-fn summary-fn close-fn]} (:payload opened)]
        (is (kernel/ok? opened))
        (doseq [m fixture-messages] (send-fn m))
        (close-fn)
        (is (nil? (failure-fn)) (str "delivery failed: " (pr-str (failure-fn))))
        (is (= (count fixture-messages) (:sent (summary-fn))))
        (is (= (count fixture-messages) (:acked (summary-fn))))
        (testing "and what arrived is byte-identical to what was sent --
                  the framing round trip, over messages this repository
                  ships rather than over generated bytes"
          (is (= fixture-messages (received-fn)))))
      (finally (stop!)))))

;; ---- the ACK pairing law -------------------------------------------

(defn- msg
  [control-id]
  (str "MSH|^~\\&|EHR-TESTING-SIM|SIM|||20260811003000+0000||ADT^A01|"
       control-id "|P|2.4\rEVN|A01|20260811003000+0000\r"))

(defn- deliver-all!
  "Sends `messages` through one connection to a loopback responder
  answering `ack-code`, and returns `{:summary :failure :received}`."
  [messages & [{:keys [ack-code silent?] :or {ack-code "AA"}}]]
  (let [{:keys [port received-fn stop!]} (mllp/ack-server! {:ack-code ack-code :silent? silent?})]
    (try
      (let [{:keys [send-fn failure-fn summary-fn close-fn]}
            (:payload (mllp/open-sink! "127.0.0.1" port {:ack-timeout-ms 1500}))]
        (doseq [m messages] (send-fn m))
        (close-fn)
        {:summary (summary-fn) :failure (failure-fn) :received (received-fn)})
      (finally (stop!)))))

(deftest ack-pairing-is-positional-and-msa-2-is-checked-per-pair
  (let [ids ["MRN1-A01-10" "MRN2-A01-20" "MRN3-A01-30"]
        {:keys [summary failure]} (deliver-all! (mapv msg ids))]
    (is (nil? failure))
    (is (= 3 (:sent summary)))
    (is (= 3 (:acked summary)))
    (is (= [0 1 2] (mapv :index (:pairs summary)))
        "the k-th ACK is the acknowledgement of the k-th message SENT")
    (is (= ids (mapv :control-id (:pairs summary)))
        "and MSA-2 echoed each message's own MSH-10, checked per pair")))

(deftest positional-pairing-survives-duplicate-control-ids
  (testing "THE REASON THE PAIRING IS POSITIONAL rather than a lookup.
            `control-id-for` is NON-INJECTIVE over `:result-available`
            -- two results for one patient at one second mint the same
            MSH-10 -- and two shipped corpora carry duplicates today
            (`roadmap.md#oru-control-id-collision`, arc 4 sweep 3's
            finding 1). MSA-2 equality is asserted PER PAIR and is NOT a
            global bijection: a control id may legitimately appear on
            two pairs, and a pairing keyed on MSH-10 would acknowledge
            one twin twice and the other never. This gate uses a
            hand-built pair; `ehrt.conformance.mllp-pairing-test` runs
            the same law over the REAL seed-424242 spool."
    (let [ids ["MRN189-R01-119086260" "MRN189-R01-119086260" "MRN2-A01-20"]
          {:keys [summary failure received]} (deliver-all! (mapv msg ids))]
      (is (nil? failure))
      (is (= 3 (:sent summary)))
      (is (= 3 (:acked summary)))
      (is (= ids (mapv :control-id (:pairs summary))))
      (testing "the population really does contain a duplicate, or this
                gate proves nothing (R-empty-population-is-red's twin)"
        (is (> (count ids) (count (set ids)))))
      (testing "every message was delivered exactly once, in order"
        (is (= (mapv msg ids) received))))))

;; ---- MSA-1, stated and tested --------------------------------------

(deftest aa-continues
  (let [{:keys [summary failure]} (deliver-all! (mapv msg ["a" "b"]) {:ack-code "AA"})]
    (is (nil? failure))
    (is (= 2 (:acked summary)))))

(deftest ae-and-ar-abort-delivery-at-that-message
  (doseq [code ["AE" "AR"]]
    (testing code
      (let [{:keys [summary failure received]} (deliver-all! (mapv msg ["a" "b" "c"]) {:ack-code code})]
        (is (some? failure))
        (is (= :mllp-negative-acknowledgement (:category failure)))
        (is (= 0 (:index (:payload failure))))
        (is (= code (:code (:payload failure))))
        (is (= 0 (:acked summary)))
        (is (= 1 (count received))
            "the receiver refused message 0, so messages 1 and 2 were never pushed at it")))))

(deftest an-unrecognized-msa-1-aborts-rather-than-passing
  (testing "this project speaks ORIGINAL acknowledgement mode only, so
            enhanced-mode CA/CE/CR -- and anything else -- is a receiver
            contract mismatch, not a pass"
    (doseq [code ["CA" "CE" "CR" "ZZ" ""]]
      (let [{:keys [failure]} (deliver-all! [(msg "a")] {:ack-code code})]
        (is (some? failure) (str "MSA-1 " (pr-str code) " was treated as an accept"))
        (is (= :mllp-unrecognized-ack-code (:category failure)))))))

(deftest a-missing-ack-times-out-visibly
  (testing "a receiver that reads and never answers is a named error
            with the message's own index and control id -- never an
            indefinite hang and never a silent success"
    (let [{:keys [summary failure]} (deliver-all! [(msg "MRN1-A01-10")] {:silent? true})]
      (is (some? failure))
      (is (= :mllp-ack-timeout (:category failure)))
      (is (= 0 (:index (:payload failure))))
      (is (= "MRN1-A01-10" (:control-id (:payload failure))))
      (is (= 0 (:acked summary))))))

(deftest a-refused-connection-is-an-error-not-a-throw
  (let [{:keys [port stop!]} (mllp/ack-server!)
        _ (stop!)
        r (mllp/open-sink! "127.0.0.1" port {:ack-timeout-ms 500})]
    (is (kernel/rejected? r))
    (is (= :mllp-connect-failed (:category r)))))

(deftest the-responder-echoes-msh-10-verbatim
  (testing "the loopback responder is not allowed to normalize anything
            -- MSA-2 is the received MSH-10 byte for byte, duplicates
            included, which is what makes the duplicate gate above a
            real test rather than a self-fulfilling one"
    (let [ack (#'mllp/ack-for (msg "MRN189-R01-119086260") "AA")]
      (is (= "AA" (:code (mllp/ack-codes ack))))
      (is (= "MRN189-R01-119086260" (:control-id (mllp/ack-codes ack))))
      (testing "and it is a real ACK frame: [MSH MSA], per the jar's own
                `ACK [MSH MSA ERR]` structure with the optional ERR
                absent on an accept"
        (is (= ["MSH" "MSA"] (mapv #(subs % 0 3) (remove str/blank? (str/split ack #"\r")))))))))

(deftest a-frame-is-read-through-the-codec-not-by-scanning-for-the-terminator
  (testing "the incremental reader asks `framing/decode` whether the
            buffer is yet exactly one frame -- so a frame and its
            successor concatenated on the wire are still delivered as
            two, which is the property a scanner gets wrong"
    (let [{:keys [port received-fn stop!]} (mllp/ack-server! {:silent? true})]
      (try
        (let [payloads (mapv #(.getBytes ^String (msg %) "UTF-8") ["a" "b" "c"])
              all (:payload (framing/encode :mllp payloads))]
          (with-open [s (java.net.Socket. "127.0.0.1" ^int (int port))]
            (doto (.getOutputStream s) (.write ^bytes all) (.flush))
            ;; the responder is silent, so nothing comes back; give the
            ;; three frames a bounded window to be read
            (loop [tries 0]
              (when (and (< (count (received-fn)) 3) (< tries 100))
                (Thread/sleep 20)
                (recur (inc tries)))))
          (is (= (mapv #(msg %) ["a" "b" "c"]) (received-fn))))
        (finally (stop!))))))
