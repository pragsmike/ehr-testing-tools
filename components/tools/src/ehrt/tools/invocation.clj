(ns ehrt.tools.invocation
  "The invocation record (pattern nursery #2): one engine-agnostic schema
  for \"a subprocess ran\". `run!` is the only impure seam in the engine
  layer -- everything downstream (corpus.generate, manifests) operates on
  the data it returns, never on the subprocess itself."
  (:refer-clojure :exclude [run!])
  (:require [malli.core :as m]
            [ehrt.tools.digest :as digest]
            [ehrt.tools.result :as result])
  (:import [java.io File]
           [java.lang ProcessBuilder$Redirect]
           [java.time Instant]))

(def InvocationRecord
  [:map
   [:command :string]
   [:args [:vector :string]]
   [:dir {:optional true} [:maybe :string]]
   [:env [:map-of :string :string]]
   [:started-at :string]
   [:duration-ms :int]
   [:exit-code :int]
   [:stdout-path :string]
   [:stderr-path :string]
   [:stdout-sha256 :string]
   [:stderr-sha256 :string]])

(defn valid?
  [record]
  (m/validate InvocationRecord record))

(defn sha256-file
  "Hex-encoded SHA-256 digest of a file's bytes."
  [path]
  (digest/sha256-file path))

(defn run!
  "Executes command+args as a subprocess, redirecting stdout/stderr to the
  given files, and returns a result/ok InvocationRecord -- or a
  result/error :spawn-failed if the process could not even be started.
  A nonzero exit code is a normal, successful invocation (the caller
  interprets exit-code); it is not itself a wrapper failure."
  [{:keys [command args dir env stdout-path stderr-path]
    :or {args [] env {}}}]
  (try
    (let [pb (ProcessBuilder. (into-array String (cons command args)))
          penv (.environment pb)]
      (when dir
        (.directory pb (File. ^String dir)))
      (doseq [[k v] env]
        (.put penv k v))
      (.redirectOutput pb (ProcessBuilder$Redirect/to (File. ^String stdout-path)))
      (.redirectError pb (ProcessBuilder$Redirect/to (File. ^String stderr-path)))
      (let [started-at (str (Instant/now))
            start-ms (System/currentTimeMillis)
            proc (.start pb)
            exit-code (.waitFor proc)
            duration-ms (- (System/currentTimeMillis) start-ms)]
        (result/ok {:command command
                    :args (vec args)
                    :dir dir
                    :env env
                    :started-at started-at
                    :duration-ms duration-ms
                    :exit-code exit-code
                    :stdout-path stdout-path
                    :stderr-path stderr-path
                    :stdout-sha256 (sha256-file stdout-path)
                    :stderr-sha256 (sha256-file stderr-path)})))
    (catch Exception e
      (result/error :spawn-failed {:command command
                                    :args (vec args)
                                    :message (.getMessage e)}))))
