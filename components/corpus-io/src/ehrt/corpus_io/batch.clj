(ns ehrt.corpus-io.batch
  "The corpus batcher's pure core (ADR-0111): partitions a corpus's own
  messages into schedule-aligned delivery batches -- no clock, no IO,
  no wall-clock default anywhere (this session's driving prompt names
  the batcher DETERMINISTIC by design: same input, same batches,
  byte-stable). The CLI shell (bases/cli) owns reading candidate files
  from a directory, splitting multi-message files, and writing each
  occupied bucket through ehrt.corpus-io.framing's own :batch codec;
  this namespace only does the partitioning arithmetic.

  Author ruling (2026-08-11, Q1 a, verbatim): the batcher 'should work
  on any corpus, even an existing directory of foreign (but valid)
  message files' -- this fn takes bare tagged messages, not anything
  shaped by this repo's own generator or manifest machinery, so a
  foreign corpus is exactly as batchable as one this repo generated.

  MSH-7 extraction reuses ehrt.corpus-io.er7-fields/message-timestamp-ms
  (ADR-0111's own move-don't-improve micro-relocation from
  ehrt.corpus.player, disclosed in that namespace's own docstring) --
  one source of truth for lenient MSH-7 reads, never a second
  implementation.

  Bucketing law: bucket k spans [k*interval-ms, (k+1)*interval-ms)
  against the Unix epoch (never a caller-supplied anchor -- an
  --anchor option is a NAMED DEFERRAL, docs/adr/0111-*.md), so daily
  batches align to UTC midnight and hourly batches align to the hour,
  matching real-world delivery schedules. Empty buckets are skipped
  entirely in v1 (a named deferral -- interior-empty-batch realism,
  i.e. a receiver seeing bucket N then bucket N+2 with no N+1 file at
  all, is not modeled); this fn's own :buckets vector never contains
  an empty bucket.

  Fail-fast law: a message whose own MSH-7 cannot be parsed is a
  categorized kernel/error naming the message's own :source (the
  caller-supplied label, ordinarily a filename) -- the author's own
  premise is a foreign-but-VALID corpus, so an unparseable transmit
  time is a defect to surface, never a silent skip."
  (:require [ehrt.corpus-io.er7-fields :as er7-fields]
            [ehrt.kernel.interface :as kernel]))

(defn- timestamp-tagged
  "tagged-messages ({:message :source} maps) -> kernel/ok the same maps
  with :ts-ms assoc'd (each message's own MSH-7, epoch ms), or
  kernel/error :unparseable-transmit-time naming the first offending
  :source -- short-circuits on the first failure via `reduced`, never
  processes the rest once one is found (fail-fast, module docstring)."
  [tagged-messages]
  (let [result (reduce
                (fn [acc {:keys [message source] :as m}]
                  (let [ts (er7-fields/message-timestamp-ms message)]
                    (if (nil? ts)
                      (reduced (kernel/error :unparseable-transmit-time
                                              {:source source
                                               :hint "MSH-7 is absent or unparseable -- ehrt corpus batch requires every message to carry a parseable transmit time (the corpus is presumed foreign-but-valid, never silently skipped)"}))
                      (conj acc (assoc m :ts-ms ts)))))
                []
                tagged-messages)]
    (if (kernel/error? result)
      result
      (kernel/ok result))))

(defn- bucket-of
  [interval-ms ts-ms]
  (quot ^long ts-ms ^long interval-ms))

(defn partition-messages
  "tagged-messages (a seq of {:message <raw ER7 string> :source <caller
  label, e.g. a filename>}, in ANY order -- this fn does its own global
  sort, never trusting input order) x {:interval-ms} -> kernel/ok
  {:buckets [{:bucket-index k :start-ms :end-ms :messages [{:message
  :source :ts-ms} ...]} ...]}, ascending by :bucket-index, each
  bucket's own :messages ascending by :ts-ms -- the sort is GLOBAL
  (across every message this call was given, regardless of which
  :source it came from) then partitioned, never sorted per-bucket
  after the fact, so cross-source interleaving is exact.

  Bucket k spans [k*interval-ms, (k+1)*interval-ms) against the Unix
  epoch (module docstring); buckets with zero messages are never
  present in :buckets (skipped, v1, named deferral).

  Returns kernel/error :unparseable-transmit-time {:source ...} on the
  first message (in input order) whose own MSH-7 doesn't parse --
  fail-fast, before any sorting or bucketing happens; see
  timestamp-tagged's own docstring. :interval-ms must be a positive
  number -- this fn trusts its caller on that (the CLI shell owns
  rejecting a non-positive --interval by name before ever calling
  here)."
  [tagged-messages {:keys [interval-ms]}]
  (let [tagged-result (timestamp-tagged tagged-messages)]
    (if-not (kernel/ok? tagged-result)
      tagged-result
      (let [sorted (sort-by :ts-ms (:payload tagged-result))
            grouped (group-by #(bucket-of interval-ms (:ts-ms %)) sorted)]
        (kernel/ok
         {:buckets (mapv (fn [k]
                            {:bucket-index k
                             :start-ms (* ^long k ^long interval-ms)
                             :end-ms (* (inc ^long k) ^long interval-ms)
                             :messages (get grouped k)})
                          (sort (keys grouped)))})))))
