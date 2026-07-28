(ns ehr-testing-tools.digest
  "Shared hashing helpers. Content hashes are how this repo tells claims
  from facts (ADR-0005): a version number is a claim, a sha256 is a fact."
  (:require [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

(defn sha256-file
  "Hex-encoded SHA-256 digest of a file's bytes."
  [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream (io/file path))]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn sha256-string
  "Hex-encoded SHA-256 digest of a string's UTF-8 bytes."
  [s]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest digest (.getBytes ^String s "UTF-8"))))))

(defn sha256-bytes
  "Hex-encoded SHA-256 digest of a raw byte array -- the spool's own
  per-item hashing (ehr-testing-tools.corpus.spool, SS-3) needs this
  directly rather than through sha256-string's UTF-8 re-encoding, since
  a framed item's bytes are not guaranteed to be valid UTF-8 text
  (the charset law, docs/source-sink-design.md Part II)."
  [^bytes bs]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest digest bs)))))
