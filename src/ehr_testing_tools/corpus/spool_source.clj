(ns ehr-testing-tools.corpus.spool-source
  "The two spool resolutions (ruling 5, docs/source-sink-design.md
  Part I.2): a `:stdin` Source always needs spooling (there is no
  directory yet); a `:file` Source whose own `:framing` isn't the
  identity framing (`:file-per-item`, source-sink/default-framing)
  needs it too -- its bytes are multiple items packed into one file,
  not yet a dir-shaped corpus-tree. Both resolve through the SAME
  mechanism (ehr-testing-tools.corpus.spool), mirroring
  generator-source's own execute-then-wrap-as-dir shape (SS-2) for the
  read side instead of the generate side.

  `:dir` sources are NOT resolved here this session (ruling 5): a
  directory of multi-item files stays a recorded OPEN item, never
  silently spooled -- SS-3's own scope fence."
  (:require [clojure.java.io :as io]
            [ehr-testing-tools.corpus.source-sink :as source-sink]
            [ehr-testing-tools.corpus.spool :as spool]
            [ehr-testing-tools.result :as result]))

(defn needs-spooling?
  "true when source's own bytes must ride the spool before intake can
  walk them as a dir -- a :stdin source always does; a :file source
  does only when its own :framing (defaulting to source-sink/default-
  framing when absent) isn't :file-per-item. A :dir source is never
  routed here this session (the recorded OPEN item, ruling 5)."
  [source]
  (case (:kind source)
    :stdin true
    :file (not= (or (:framing source) source-sink/default-framing) :file-per-item)
    false))

(defn resolve!
  "source -- a :stdin or :file Source for which needs-spooling? is
  true. captured-at -- required, explicit (D8: never read from the
  wall clock here -- the CLI shell is the impure boundary, matching
  corpus.intake's own :received discipline). in-override -- for tests,
  an InputStream to spool instead of the real System/in (:stdin) or
  the file's own bytes (:file). out-dir/max-bytes -- passed straight
  through to spool/spool! when given.

  Returns result/ok a canonical :dir Source over the spooled capture
  directory, or spool/spool!'s own rejection, propagated unchanged."
  [{:keys [source captured-at in-override out-dir max-bytes]}]
  (let [[in origin] (case (:kind source)
                      :stdin [(or in-override System/in) "stdin"]
                      :file [(or in-override (io/file (:path source))) (:path source)])
        spool-result (spool/spool! (cond-> {:in in
                                             :framing (:framing source)
                                             :format (:format source)
                                             :origin origin
                                             :captured-at captured-at}
                                     out-dir (assoc :out-dir out-dir)
                                     max-bytes (assoc :max-bytes max-bytes)))]
    (if-not (result/ok? spool-result)
      spool-result
      (source-sink/dir-source {:path (:out-dir (:payload spool-result))}))))
