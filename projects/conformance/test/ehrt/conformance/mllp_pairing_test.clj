(ns ehrt.conformance.mllp-pairing-test
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (g)):
  the ACK pairing law over a REAL corpus that really does carry
  duplicate MSH-10s.

  `ehrt.corpus-io.mllp-test` states the law over a hand-built pair,
  which proves the mechanism and nothing about this project's own
  output. This namespace is the other half, and it is here rather than
  in either component because only `projects/conformance` has both
  `sim` (which generates the corpus) and `corpus-io` (which delivers
  it) on one classpath.

  THE POPULATION IS GENERATED, NOT QUOTED. Arc 4 sweep 3's finding 1
  measured `seed-424242-clinic-decade` as carrying 6 duplicate MSH-10s
  in 2 groups -- `control-id-for` is non-injective over
  `:result-available`, two results for one patient at one second mint
  the same id, and the row is open and priced
  (`roadmap.md#oru-control-id-collision`). A gate that quoted that
  number from a session record would go quiet the day the corpus
  reshuffled. This one regenerates the corpus and asserts the
  duplicates are STILL THERE before asserting anything about pairing --
  because if they ever stop being there, this gate is vacuous and
  should say so rather than pass."
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

(deftest the-corpus-really-does-carry-duplicate-control-ids
  (testing "R-empty-population-is-red, in its sharpest form: every
            assertion below is about what a DUPLICATE does to positional
            pairing, so a corpus with no duplicate makes them all
            vacuous"
    (let [r @corpus]
      (is (result/ok? r) (str "the gated run failed: " (pr-str (:payload r))))
      (let [ids (control-ids (:messages (:payload r)))
            dupes (into {} (filter (fn [[_ n]] (> n 1))) (frequencies ids))]
        (is (pos? (count ids)))
        (is (seq dupes)
            "seed-424242-clinic-decade no longer carries a duplicate MSH-10 -- either
             `roadmap.md#oru-control-id-collision` was fixed (in which case delete this
             gate and say so) or the corpus reshuffled and this gate is now vacuous")
        (testing "and the collision is the one the row names: same patient,
                  same second, the default ORU control-id branch"
          (is (every? #(re-find #"-R01-" %) (keys dupes))
              (str "a NEW collision shape appeared, outside the rowed one: " (pr-str dupes))))
        (println "  seed-424242-clinic-decade duplicate MSH-10s:"
                 (pr-str (into (sorted-map) dupes))
                 (str "(" (reduce + (vals dupes)) " messages in " (count dupes) " groups)"))))))

(deftest positional-pairing-survives-the-real-duplicates
  (testing "THE PAIRING LAW: for every message sent, an ACK whose MSA-2
            equals THAT message's MSH-10, and no ACK for a message never
            sent. Positional, so a duplicated control id is delivered
            and acknowledged TWICE -- once per position -- rather than
            once with the twin silently dropped."
    (let [messages (:messages (:payload @corpus))
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
            (testing "MSA-2 EQUALITY IS PER PAIR AND NOT A GLOBAL BIJECTION
                      -- which is the whole reason the pairing is
                      positional. A duplicated id appears on more pairs
                      than there are distinct ids."
              (is (> (count pairs) (count (set ids)))))
            (testing "and every message arrived on the wire, byte for byte"
              (is (= messages (received-fn))))))
        (finally (stop!))))))
