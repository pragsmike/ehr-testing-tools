;; Archived as evidence (Cowork cloud session, 2026-07-30) alongside
;; judge-v2-nist-spike-notes.md -- landed by the judge-v2-nist landing
;; session (notes/prompts/2026-07-30-ehr-testing-judge-v2-nist-landing.md).
;; NOT RUNNABLE HERE: fixture-dir below is a cloud-session-local absolute
;; path (/home/claude/...) that does not exist on this machine. Kept
;; verbatim as provenance for the spike's own execution-verified claims,
;; not as a script anyone should try to run in this repo.

;; Spike: drive the NIST v2-validation engine (1.7.3) directly from Clojure.
;; Proves: direct Scala interop (no CDC wrapper), offline, sync API,
;; and maps the raw Report into the ehrt.judge Finding envelope shape.
(ns spike
  (:require [clojure.pprint :as pp])
  (:import [hl7.v2.validation ValidationContextBuilder SyncHL7Validator]
           [gov.nist.validation.report Report Entry]
           [java.io FileInputStream InputStream]
           [java.util Arrays]))

(def fixture-dir "/home/claude/nist/lib-hl7v2-nist-validator/src/test/resources/")
(def profile-dir (str fixture-dir "COVID19_ELR-v2.3.1/"))

(defn in [f] (FileInputStream. (str profile-dir f)))

(println "== building validation context (PROFILE.xml + CONSTRAINTS.xml) ...")
(def validator
  (let [b (ValidationContextBuilder. ^InputStream (in "PROFILE.xml"))]
    (.useConformanceContext b ^java.util.List (Arrays/asList (into-array InputStream [(in "CONSTRAINTS.xml")])))
    (SyncHL7Validator. (.getValidationContext b))))

(def msg-ids
  (-> (scala.jdk.javaapi.CollectionConverters/asJava (.messages (.profile validator)))
      .keySet vec))
(println "== profile message ids:" msg-ids)

(def message (slurp (str fixture-dir "covidELR/231HL7TestFilewithHHSData.txt")))
(println "== validating" (count message) "chars of ER7 against msg id" (first msg-ids))

(def ^Report report (.check validator message (first msg-ids)))

;; ---- raw engine output ----
(def entries-by-area (.getEntries report))
(println "\n== raw report areas -> entry counts:")
(doseq [[k v] entries-by-area] (println "  " k "->" (count v)))

;; ---- interpret: raw Entry -> ehrt.judge Finding envelope ----
(def sev-map {"ERROR" :error "WARNING" :warning "INFORMATIONAL" :information
              "INFORMATION" :information "ALERT" :warning "AFFIRMATIVE" :information})

(defn entry->finding [area ^Entry e]
  {:severity  (get sev-map (.getClassification e) :information)
   :code      (str area "/" (.getCategory e))
   :locator   {:path (.getPath e) :line (.getLine e) :column (.getColumn e)}
   :message   (.getDescription e)
   :engine    {:name "nist-v2-validation" :version "1.7.3"}
   :native-ref {:classification (.getClassification e) :category (.getCategory e)}})

(def findings
  (vec (for [[area es] entries-by-area, e es] (entry->finding area e))))

(println "\n== findings (ehrt.judge Finding envelope), first 8 of" (count findings) ":")
(pp/pprint (mapv #(dissoc % :native-ref) (take 8 findings)))

(let [by-class (frequencies (map #(get-in % [:native-ref :classification]) findings))
      verdict  (if (some #(= "Error" (get-in % [:native-ref :classification])) findings)
                 :rejected :pass)]
  (println "\n== classification frequencies:" by-class)
  (println "== worst-of style verdict:" verdict))
