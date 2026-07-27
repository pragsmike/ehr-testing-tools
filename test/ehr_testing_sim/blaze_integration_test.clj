(ns ehr-testing-sim.blaze-integration-test
  "Task 3 (M6): the ecological loop -- samply/blaze (a same-language
  Clojure FHIR server, .agents/plans/roadmap.md's own M6 consumer-plan
  citation) is EmitState's natural first real-world consumer, the same
  role ehr-testing-tools plays for EmitHL7 (README.md's own 'never
  graded on its own homework' claim). If a Blaze server is reachable at
  a configured base URL, this test POSTs a run's own end-of-run
  Bundle(s), reads the resources back, and asserts round-trip
  equivalence on the fields we wrote. Unreachable -> a clean skip, the
  SAME skip-when-absent pattern ehr-testing-tools' own sim-harness/
  available? establishes for ITS cross-repo consumer loop (ADR-0001's
  dependency-arrow discipline, applied here to an external service
  rather than a sibling repo) -- this project never depends on Blaze
  being present; it merely checks against it when it is.

  Start a local Blaze to actually exercise this test:

      docker run -p 8080:8080 samply/blaze:latest

  (the same one-liner README.md's own demo section names). Base URL is
  configurable via the BLAZE_BASE_URL environment variable, defaulting
  to http://localhost:8080/fhir. Uses only java.net.HttpURLConnection
  (JDK 1.1+) rather than the newer java.net.http.HttpClient (JDK 11+,
  unavailable on this session's own JDK 8 runtime -- notes/facts-
  register.md F13) -- no new runtime dependency for one integration
  test's own HTTP calls."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehr-testing-sim.emit-state :as emit-state]
            [ehr-testing-sim.engine :as engine])
  (:import [java.net URL HttpURLConnection SocketTimeoutException]))

(def base-url
  (or (System/getenv "BLAZE_BASE_URL") "http://localhost:8080/fhir"))

(defn- open-connection
  ^HttpURLConnection [url method]
  (doto ^HttpURLConnection (.openConnection (URL. url))
    (.setRequestMethod method)
    (.setConnectTimeout 2000)
    (.setReadTimeout 2000)))

(defn- read-body
  [^HttpURLConnection conn]
  (let [status (.getResponseCode conn)
        stream (if (< status 400) (.getInputStream conn) (.getErrorStream conn))]
    {:status status :body (when stream (slurp stream))}))

(defn reachable?
  "A short-timeout GET against Blaze's own /metadata capability
  statement -- any exception (connection refused, DNS failure, timeout)
  or non-200 means 'treat as absent,' never a test error."
  []
  (try
    (= 200 (:status (read-body (open-connection (str base-url "/metadata") "GET"))))
    (catch Exception _ false)))

(def absence-message
  (str "Blaze unreachable at " base-url " -- skipping the ecological-loop test. "
       "Start one with: docker run -p 8080:8080 samply/blaze:latest"))

(defn- as-transaction
  "ehr-testing-sim.emit-state/patient-bundle's own Bundle is :type
  \"collection\" (a snapshot, not a submission payload -- EmitState's
  own rendering law says nothing about how a consumer submits one) --
  this test's OWN concern converts it to a FHIR transaction Bundle (one
  POST-to-its-own-resourceType entry per resource), the shape Blaze's
  base URL actually accepts. Kept local here, not added to emit-state's
  public surface."
  [bundle]
  (assoc bundle
         :type "transaction"
         :entry (mapv (fn [{:keys [resource] :as entry}]
                        (assoc entry :request {:method "POST" :url (:resourceType resource)}))
                      (:entry bundle))))

(defn- post-bundle!
  [bundle]
  (let [conn (open-connection base-url "POST")]
    (doto conn
      (.setDoOutput true)
      (.setRequestProperty "Content-Type" "application/fhir+json"))
    (with-open [w (io/writer (.getOutputStream conn))]
      (.write w (json/write-str (as-transaction bundle))))
    (read-body conn)))

(defn- get-resource!
  [relative-location]
  (read-body (open-connection (str base-url "/" relative-location) "GET")))

(deftest ^:integration blaze-accepts-end-of-run-bundles-and-round-trips-patient-fields
  (if-not (reachable?)
    (do (println absence-message) (is true absence-message))
    (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 2})
          bundles (emit-state/bundle-run ground-truth "2024-01-01" "+00:00" :end)]
      (is (= 2 (count bundles)) "sanity: both patients produced a Bundle")
      (doseq [[patient-id bundle] bundles]
        (let [{:keys [status body]} (post-bundle! bundle)]
          (testing (str "Blaze's own verdict on patient " patient-id "'s Bundle -- any rejection here is a"
                        " FINDING (facts-register row: 'verified against Blaze <version>, <date>'), not a"
                        " test-authoring bug -- fix our shapes if we're wrong, record theirs if strict beyond R4")
            (is (= 200 status) body))
          (when (= 200 status)
            (let [transaction-response (json/read-str body :key-fn keyword)
                  patient-entry (first (filter #(= "Patient" (get-in % [:resource :resourceType]))
                                               (:entry transaction-response)))
                  posted (:resource patient-entry)
                  location (get-in patient-entry [:response :location])
                  fetched (json/read-str (:body (get-resource! location)) :key-fn keyword)]
              (testing "round-trip equivalence on the fields we wrote"
                (is (= (:birthDate posted) (:birthDate fetched)))
                (is (= (:gender posted) (:gender fetched)))
                (is (= (get-in posted [:name 0 :family]) (get-in fetched [:name 0 :family])))
                (is (= (get-in posted [:identifier 0 :value]) (get-in fetched [:identifier 0 :value])))))))))))
