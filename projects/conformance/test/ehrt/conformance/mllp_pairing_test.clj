(ns ehrt.conformance.mllp-pairing-test
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (g)):
  the ACK pairing law over a REAL generated spool.

  `ehrt.corpus-io.mllp-test` states the law over a hand-built pair,
  which proves the mechanism and nothing about a real spool's shape.
  This namespace is the other half, and it is here rather than in
  either component because only `projects/conformance` has both `sim`
  (which generates the corpus) and `corpus-io` (which delivers it) on
  one classpath.

  REBUILT 2026-09-23, ADR-0181 candidate 3. Until that day this file
  regenerated `seed-424242-clinic-decade` and asserted that its
  duplicate MSH-10s were STILL THERE before asserting anything about
  pairing -- a deliberate guard against going vacuous while
  `roadmap.md#oru-control-id-collision` stayed open. That row is now
  CLOSED: MSH-10 carries the log index, so no corpus this project
  generates can produce a duplicate ever again, and the old premise
  would have been not merely vacuous but FALSE. It is retired rather
  than weakened.

  WHAT REPLACES IT, and why the law is still worth gating:

  1. The spool is still REAL -- the same gated run, generated here,
     delivered over a real socket. That half proves what it always
     proved: every message of a real corpus is delivered once, in
     order, and acknowledged by position.
  2. The DUPLICATE is now HAND-BUILT and appended to that spool,
     because nothing this project generates mints one any more. The
     pairing law does not belong to this project's own output: a
     `--sink mllp://` carries FOREIGN corpora too, and a foreign
     corpus may repeat an MSH-10 for any reason of its own. Positional
     pairing is what makes that survivable, and it has to keep being
     true of a spool that contains a duplicate -- so this gate builds
     the duplicate rather than hoping for one.
  3. The injectivity of this project's OWN ids is gated where the
     population is, not here:
     `ehrt.sim.run-test/control-id-for-is-injective-over-every-corpus-
     this-lane-runs` measures every message of four gated corpora and
     the dense-7500 750 cell. That gate is ADR-0181's own answer to
     the caption this file used to carry -- 'a NEW collision shape
     appeared, outside the rowed one' -- which was scoped to ONE root
     and so never saw the four non-ORU classes the dense cell carried."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as result]
            [ehrt.sim.interface :as sim]))

(def ^:private corpus
  "The gated `seed-424242-clinic-decade` run, verbatim from
  `ehrt.sim.run-test`'s own `gated-runs` -- the same opts, so this gate
  and the corpus a reader generates by hand cannot disagree."
  (delay (sim/run-command {:seed 424242 :patients 200 :reference-date "2026-08-04"
                           :churn true
                           :config "demos/scenarios/clinic-decade/config.edn"
                           :emit "hl7"})))

(defn- control-ids [messages] (mapv corpus-io/message-control-id messages))

(def ^:private foreign-twins
  "A hand-built pair sharing one MSH-10, plus a third that does not,
  appended to the real spool.

  THE ID IS A REAL ONE FROM THIS CORPUS'S OWN PAST: until ADR-0181,
  `seed-424242-clinic-decade` really did render
  `MRN000189-R01-119086260` six times -- five `:observation` and one
  `:diagnostic-report`, one patient, one ED bundle, one instant. It is
  kept verbatim so a reader of this file can still see what the
  collision looked like, and it is HAND-BUILT so that no assertion here
  depends on the generator ever making one again. `ehrt.corpus-io.mllp-
  test` builds the same shape from the same id, one layer down."
  (let [msg (fn [id]
              (str "MSH|^~\\&|FOREIGN|SITE|||20260804000000+0000||ORU^R01|" id "|P|2.4\r"
                   "PID|1||MRN000189\r"))]
    (mapv msg ["MRN000189-R01-119086260" "MRN000189-R01-119086260" "MRN000189-R01-119086261"])))

(deftest the-generated-corpus-carries-no-duplicate-control-id
  (testing "ADR-0181: the row is closed, so this is an ordinary
            injectivity statement rather than the 'duplicates are still
            there' premise this file used to open with. Stated here as
            well as in `ehrt.sim.run-test` because the spool below is
            built on top of it -- the ONLY duplicate the pairing
            assertions meet must be the hand-built one, or they would be
            testing something other than what they say."
    (let [r @corpus]
      (is (result/ok? r) (str "the gated run failed: " (pr-str (:payload r))))
      (let [ids (control-ids (:messages (:payload r)))
            dupes (into (sorted-map) (filter (fn [[_ n]] (> n 1))) (frequencies ids))]
        (is (pos? (count ids)))
        (is (zero? (count dupes))
            (str "seed-424242-clinic-decade carries " (reduce + (vals dupes))
                 " messages sharing " (count dupes) " MSH-10s: "
                 (pr-str (into (sorted-map) (take 8 dupes)))))))))

(deftest positional-pairing-survives-a-duplicate-in-a-real-spool
  (testing "THE PAIRING LAW: for every message sent, an ACK whose MSA-2
            equals THAT message's MSH-10, and no ACK for a message never
            sent. Positional, so a duplicated control id is delivered
            and acknowledged TWICE -- once per position -- rather than
            once with the twin silently dropped."
    (let [messages (into (vec (:messages (:payload @corpus))) foreign-twins)
          {:keys [port received-fn stop!]} (corpus-io/mllp-ack-server!)]
      (try
        (let [opened (corpus-io/mllp-open-sink! "127.0.0.1" port)
              {:keys [send-fn failure-fn summary-fn close-fn]} (:payload opened)]
          (is (result/ok? opened))
          (doseq [m messages] (send-fn m))
          (close-fn)
          (is (nil? (failure-fn)) (str "delivery failed: " (pr-str (failure-fn))))
          (let [{:keys [sent acked pairs]} (summary-fn)
                ids (control-ids messages)]
            (is (= (count messages) sent))
            (is (= (count messages) acked) "no ACK was skipped")
            (is (= (vec (range (count messages))) (mapv :index pairs))
                "the k-th ACK is the acknowledgement of the k-th message SENT")
            (is (= ids (mapv :control-id pairs))
                "MSA-2 echoed each message's own MSH-10, per pair, in order")
            (testing "R-empty-population-is-red, in its sharpest form: every
                      assertion here is about what a DUPLICATE does to
                      positional pairing, so the spool must really carry
                      one or they are all vacuous"
              (is (> (count ids) (count (set ids)))))
            (testing "MSA-2 EQUALITY IS PER PAIR AND NOT A GLOBAL BIJECTION
                      -- which is the whole reason the pairing is
                      positional. A duplicated id appears on more pairs
                      than there are distinct ids."
              (is (> (count pairs) (count (set ids)))))
            (testing "and every message arrived on the wire, byte for byte"
              (is (= messages (received-fn))))))
        (finally (stop!))))))
