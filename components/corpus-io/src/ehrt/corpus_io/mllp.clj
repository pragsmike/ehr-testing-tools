(ns ehrt.corpus-io.mllp
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (g)):
  the SOCKET half of `:mllp`. Nothing here is skeleton and nothing here
  is even emission content -- it is DELIVERY. It renders no field and
  derives no value.

  ADR-0014's own assessment, quoted because it still stands: building
  the MLLP sink was \"found to cross three namespace boundaries rather
  than one\", and \"a half-built network sink with no ACK handling and
  untested lifecycle is a worse outcome than a clearly named
  deferral.\" This namespace is what pays that price: framing is reused
  rather than rewritten, ACK pairing is a stated law with a gate, and
  the lifecycle (timeout, negative acknowledgement, stream close) has
  one named error per failure and a test for each.

  THE CODEC IS NEVER DUPLICATED. `ehrt.corpus-io.framing` owns the
  VT/FS-CR block envelope -- the actual byte values are written down
  THERE and deliberately nowhere else, this docstring included -- with
  round-trip, charset-law, concrete-example and malformed-input tests
  already in the tree. Every byte this namespace frames goes through
  `framing/encode :mllp`, and
  every byte it unframes goes through `framing/decode :mllp` --
  including the incremental read below, which does not look for the
  end-of-block bytes itself: it appends one byte at a time and asks the
  CODEC whether the buffer is yet exactly one complete frame. There is
  no frame-byte literal anywhere in this file, and
  `mllp_test/the-sink-never-reimplements-the-codec` is the gate that
  keeps it that way (the shape `oracle_coverage_test` uses over
  `bin/oracle-lib.sh`) -- which is also why this paragraph names the
  bytes by their ASCII abbreviations rather than in hex.

  THE NAME COLLISION, resolved explicitly (ADR-0175 section 2(g)).
  `:mllp` as a `:framing` means \"these bytes are VT/FS-CR framed\";
  `:mllp` as a `:kind` means \"send these to a socket\". They live in
  different fields of the same map and never collide mechanically. A
  `:mllp` SINK THEREFORE IMPLIES `:framing :mllp`, and declaring any
  other framing on it is a construction-time error
  (`ehrt.corpus-io.source-sink/MllpSink`), never a silent override.

  THE ACK PAIRING LAW, and it is POSITIONAL.

    For a run that sends messages m_0 .. m_{n-1} over one connection,
    the k-th ACK read from that socket is the acknowledgement of m_k,
    and its MSA-2 must equal m_k's own MSH-10. There is no ACK for a
    message never sent.

  POSITIONAL, not a lookup, and that is load-bearing rather than
  convenient: `control-id-for` is NON-INJECTIVE over
  `:result-available` and two shipped corpora carry duplicate MSH-10s
  today (`roadmap.md#oru-control-id-collision`, arc 4 sweep 3's finding
  1 -- `seed-424242-clinic-decade` carries 6 duplicates in 2 groups and
  the clinic-decade demo carries 1). MSA-2 equality is therefore
  asserted PER PAIR and is NOT a global bijection: a control id may
  legitimately appear on two pairs. A pairing keyed on MSH-10 would
  acknowledge one twin twice and the other never; a positional pairing
  survives the duplicates untouched. Two gates say so:
  `mllp_test/positional-pairing-survives-duplicate-control-ids` over a
  hand-built pair, and
  `ehrt.conformance.mllp-pairing-test` over the REAL
  `seed-424242-clinic-decade` spool, whose six duplicates in two groups
  are generated rather than asserted from the record.

  MSA-1, stated and tested. \"AA\" CONTINUES. \"AE\" (application error)
  and \"AR\" (application reject) ABORT delivery at that message: the
  first negative acknowledgement is recorded, every later message is
  SKIPPED rather than pushed at a receiver that has just rejected one,
  and the run's own error names the index and the control id. Any other
  MSA-1 -- including enhanced-mode \"CA\"/\"CE\"/\"CR\" -- also aborts,
  as `:mllp-unrecognized-ack-code`: this project speaks original
  acknowledgement mode only, and a code it does not know is a receiver
  contract mismatch, not a pass.

  A MISSING ACK TIMES OUT VISIBLY. The socket carries an explicit
  SO_TIMEOUT (`default-ack-timeout-ms`), so a receiver that reads and
  never answers is `:mllp-ack-timeout` naming the index and control id
  -- never an indefinite hang and never a silent success."
  (:require [ehrt.corpus-io.framing :as framing]
            [ehrt.corpus-io.er7-fields :as er7-fields]
            [ehrt.kernel.interface :as kernel])
  (:import [java.io ByteArrayOutputStream InputStream OutputStream]
           [java.net InetAddress ServerSocket Socket SocketTimeoutException]))

(def default-ack-timeout-ms
  "How long one message waits for its own ACK before
  `:mllp-ack-timeout`. Five seconds: long enough that a real receiver
  doing real work answers, short enough that a silent one is a test
  failure rather than a hung build."
  5000)

(def accept-code
  "The one MSA-1 that continues delivery."
  "AA")

(def negative-codes
  "The two MSA-1 codes this project recognizes as a refusal. Both
  abort; they are distinguished only in the error payload."
  #{"AE" "AR"})

;; ---- frame IO: every byte through the codec -------------------------

(defn- write-frame!
  [^OutputStream out ^bytes payload]
  (let [encoded (framing/encode :mllp [payload])]
    (if-not (kernel/ok? encoded)
      encoded
      (do (.write out ^bytes (:payload encoded))
          (.flush out)
          (kernel/ok {:bytes (alength ^bytes (:payload encoded))})))))

(defn- read-frame!
  "Reads bytes from `in` until `framing/decode :mllp` accepts the
  accumulated buffer as EXACTLY ONE complete frame, and returns that
  frame's payload bytes. The terminator is never searched for here --
  the codec is asked, one byte at a time, whether the buffer is yet a
  whole frame. That is what makes the no-drift gate true rather than
  aspirational.

  `:mllp-ack-timeout` on SO_TIMEOUT, `:mllp-stream-closed` on EOF
  mid-frame."
  [^InputStream in]
  (let [buf (ByteArrayOutputStream.)]
    (loop []
      (let [b (try (.read in)
                   (catch SocketTimeoutException _ ::timeout))]
        (cond
          (= ::timeout b) (kernel/rejected :mllp-ack-timeout {:bytes-read (.size buf)})
          (neg? b) (kernel/rejected :mllp-stream-closed {:bytes-read (.size buf)})
          :else
          (do
            (.write buf (int b))
            (let [decoded (framing/decode :mllp (.toByteArray buf))]
              (if (and (kernel/ok? decoded) (= 1 (count (:payload decoded))))
                (kernel/ok (first (:payload decoded)))
                (recur)))))))))

;; ---- the ACK itself --------------------------------------------------

(defn ack-codes
  "One ACK message (as a string) -> `{:code <MSA-1> :control-id
  <MSA-2>}`, read through `ehrt.corpus-io.er7-fields`'s own lenient
  reader rather than a second field splitter. nil for either when the
  ACK carries no MSA segment or too few fields."
  [^String ack]
  {:code (er7-fields/segment-field-of ack "MSA" 1)
   :control-id (er7-fields/segment-field-of ack "MSA" 2)})

;; ---- the sink -------------------------------------------------------

(defn- classify-ack
  [index sent-control-id ^String ack]
  (let [{:keys [code control-id]} (ack-codes ack)]
    (cond
      (not= sent-control-id control-id)
      (kernel/rejected :mllp-ack-control-id-mismatch
                        {:index index :sent sent-control-id :acked control-id
                         :hint "MSA-2 must echo the MSH-10 of the message at this POSITION -- the pairing is positional, never a lookup"})

      (= accept-code code)
      (kernel/ok {:index index :control-id control-id :code code})

      (contains? negative-codes code)
      (kernel/rejected :mllp-negative-acknowledgement
                        {:index index :control-id control-id :code code
                         :hint "the receiver refused this message -- delivery stops here rather than pushing the rest of the stream at it"})

      :else
      (kernel/rejected :mllp-unrecognized-ack-code
                        {:index index :control-id control-id :code code
                         :valid-options (vec (sort (conj negative-codes accept-code)))
                         :hint "this project speaks ORIGINAL acknowledgement mode only -- an unknown MSA-1 is a receiver contract mismatch, not a pass"}))))

(defn open-sink!
  "Opens one MLLP connection to host:port and returns

    {:send-fn    message-string -> nil (the first failure makes every
                 later call a no-op)
     :failure-fn 0-arity -> the first kernel/rejected, or nil
     :summary-fn 0-arity -> {:sent n :acked n :pairs [...]}
     :close-fn   0-arity}

  One connection for the whole run, and the ACK pairing law above is
  stated over that connection's own socket order."
  ([host port] (open-sink! host port {}))
  ([host port {:keys [ack-timeout-ms] :or {ack-timeout-ms default-ack-timeout-ms}}]
   (try
     (let [socket (Socket. ^String host ^int (int port))
           _ (.setSoTimeout socket (int ack-timeout-ms))
           out (.getOutputStream socket)
           in (.getInputStream socket)
           state (atom {:sent 0 :acked 0 :pairs [] :failure nil})]
       (kernel/ok
        {:send-fn
         (fn [^String message]
           (when (nil? (:failure @state))
             (let [index (:sent @state)
                   control-id (er7-fields/message-control-id message)
                   written (write-frame! out (.getBytes message "UTF-8"))]
               (if-not (kernel/ok? written)
                 (swap! state assoc :failure written)
                 (let [_ (swap! state update :sent inc)
                       frame (read-frame! in)]
                   (if-not (kernel/ok? frame)
                     (swap! state assoc :failure
                            (update frame :payload merge {:index index :control-id control-id}))
                     (let [ack (String. ^bytes (:payload frame) "UTF-8")
                           verdict (classify-ack index control-id ack)]
                       (if (kernel/ok? verdict)
                         (swap! state #(-> % (update :acked inc)
                                             (update :pairs conj (:payload verdict))))
                         (swap! state assoc :failure verdict)))))))))
         :failure-fn (fn [] (:failure @state))
         :summary-fn (fn [] (select-keys @state [:sent :acked :pairs]))
         :close-fn (fn [] (try (.close socket) (catch Exception _ nil)))}))
     (catch Exception e
       (kernel/rejected :mllp-connect-failed
                         {:host host :port port :message (.getMessage e)})))))

;; ---- the loopback ACK responder -------------------------------------
;; THE GENERATOR NEVER EMITS AN ACK. An ACK is RECEIVED. This responder
;; is the TEST SERVER half -- the counterpart a loopback round trip
;; needs, used by this component's own tests and by
;; `bin/demo-exerciser-ed-tuesday`'s MLLP leg, and reachable from no
;; CLI verb.

(defn- ack-for
  "The ACK message for one received message: `[MSH MSA]` (ERR is
  optional in `ACK`'s own `[MSH MSA ERR]` structure and is not rendered
  for an accept), with MSA-2 echoing the received MSH-10 VERBATIM --
  including a duplicate, which is exactly what the positional law has
  to survive."
  [^String received ^String code]
  (let [control-id (or (er7-fields/message-control-id received) "")
        ts (or (er7-fields/segment-field-of received "MSH" 7) "")]
    (str "MSH|^~\\&|ACK-RESPONDER|LOOPBACK|||" ts "||ACK|" control-id "-ACK|P|2.4\r"
         "MSA|" code "|" control-id "\r")))

(defn ack-server!
  "Starts a loopback MLLP responder and returns
  `{:port n :received-fn 0-arity :stop! 0-arity}`.

  `:port` 0 (the default) binds an ephemeral port and the returned
  `:port` is the one actually bound -- a fixed port in a test suite is
  a collision waiting for a busy host. `:ack-code` is the MSA-1 it
  answers with (\"AA\" by default; a test drives \"AE\"/\"AR\"/garbage
  through it). `:silent?` reads every frame and answers NOTHING, which
  is how the timeout is proved. `:idle-timeout-ms` bounds the whole
  server so a forgotten responder cannot outlive its run -- an orphan
  JVM has cost this repository a build before."
  ([] (ack-server! {}))
  ([{:keys [port ack-code silent? idle-timeout-ms]
     :or {port 0 ack-code "AA" idle-timeout-ms 120000}}]
   (let [server (ServerSocket. (int port) 8 (InetAddress/getByName "127.0.0.1"))
         _ (.setSoTimeout server (int idle-timeout-ms))
         received (atom [])
         running (atom true)
         worker (future
                  (while @running
                    (try
                      (let [socket (.accept server)]
                        (.setSoTimeout socket (int idle-timeout-ms))
                        (with-open [^Socket s socket]
                          (let [in (.getInputStream s)
                                out (.getOutputStream s)]
                            (loop []
                              (let [frame (read-frame! in)]
                                (when (kernel/ok? frame)
                                  (let [message (String. ^bytes (:payload frame) "UTF-8")]
                                    (swap! received conj message)
                                    (when-not silent?
                                      (write-frame! out (.getBytes ^String (ack-for message ack-code) "UTF-8")))
                                    (recur)))))))) 
                      (catch Exception _ nil))))]
     {:port (.getLocalPort server)
      :received-fn (fn [] @received)
      :stop! (fn []
               (reset! running false)
               (try (.close server) (catch Exception _ nil))
               (future-cancel worker)
               nil)})))

(defn ack-server-main
  "`clojure -X:dev ehrt.corpus-io.mllp/ack-server-main :port-file
  '\"out/mllp-port\"'` -- the same loopback responder, reachable from a
  shell so `bin/demo-exerciser-ed-tuesday` can prove a real
  `ehrt play --sink mllp://...` round trip without a second
  implementation of it.

  DELIBERATELY NOT A CLI VERB. An ACK is RECEIVED, never emitted by
  this project's generator, and `ehrt` is the generator's front door;
  a `-X` entry point keeps the responder where its callers are (tests
  and one exerciser) without widening the CLI's own surface, which this
  sweep's fences forbid.

  Binds an EPHEMERAL port and writes the bound number to `:port-file`,
  which is how the caller learns it -- a fixed port in a script that
  runs on somebody else's machine is a collision waiting to happen.
  Exits on its own after `:idle-timeout-ms` (default 120s) whether or
  not anyone killed it: an orphaned JVM has cost this repository a
  35-minute build before."
  [{:keys [port-file idle-timeout-ms ack-code]
    :or {idle-timeout-ms 120000 ack-code "AA"}}]
  (let [{:keys [port stop!]} (ack-server! {:ack-code ack-code :idle-timeout-ms idle-timeout-ms})]
    (when port-file
      (spit port-file (str port "\n")))
    (println "mllp ack-server listening on 127.0.0.1:" port)
    (flush)
    (Thread/sleep (long idle-timeout-ms))
    (stop!)
    (println "mllp ack-server stopped after its idle timeout")))
