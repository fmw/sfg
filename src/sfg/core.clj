;; src/sfg/core.clj: minimal implementation of a field group to test parallelism
;;
;; Copyright 2013, F.M. de Waard <fmw@vixu.com>.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;; 
;; http://www.apache.org/licenses/LICENSE-2.0
;; 
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns sfg.core
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:use [backtype.storm clojure config])
  (:require [clojure.string :as string]
            [clj-time.core :as time-core]
            [clj-time.format :as time-format])
  (:gen-class))

(defn rfc3339-datestamp!
  "Returns the RFC3339 current time (e.g. 2012-07-09T09:31:01.579Z)."
  []
  (time-format/unparse
    (time-format/formatters :date-time)
    (time-core/now)))

(defspout my-spout ["letter" "number"] {:params [seed] :prepare true}
  [conf context collector]
  (spout
   (nextTuple []
              (emit-spout! collector (rand-nth seed)))
   (ack [id])))

(defbolt my-bolt ["capital" "number"] {:prepare true}
  [conf context collector]
  (bolt
   (execute [tuple]
            (let [letter (.getString tuple 0)
                  number (.getLong tuple 1)]
              (Thread/sleep 10000)
              (spit "/tmp/sfg.log"
                    (str letter " " (rfc3339-datestamp!) "\n")
                    :append true)
              (emit-bolt! collector
                          [(string/capitalize letter) number]
                          :anchor tuple)
              (ack! collector tuple)))))

(defn make-topology
  [seed]
  (topology
   {"1" (spout-spec (my-spout seed))}
   {"2" (bolt-spec {"1" ["letter"]}
                   my-bolt
                   :p 3)}))

(def cluster (atom nil))

(defn run-local! []
  (reset! cluster (LocalCluster.))
  (spit "/tmp/sfg.log" "") ;; overwrite /tmp/sfg.log
  (.submitTopology @cluster
                   "crawl"
                   {TOPOLOGY-DEBUG true}
                   (make-topology [["a" 1]
                                   ["b" 2]
                                   ["c" 3]
                                   ["d" 4]
                                   ["e" 5]])))

(defn shutdown-cluster! []
  (.shutdown @cluster))

(defn submit!
  []
  (StormSubmitter/submitTopology "sfg-test"
                                 {TOPOLOGY-DEBUG true
                                  TOPOLOGY-WORKERS 8}
                                 (make-topology [["a" 1]
                                                 ["b" 2]
                                                 ["c" 3]
                                                 ["d" 4]
                                                 ["e" 5]])))

(defn -main
  []
  (submit!))

;;(load "sfg/core")(require '[sfg.core :as core])
;;(core/run-local!)

;; lein clean; lein with-profile dev compile; lein uberjar; storm jar target/sfg-0.1.0-SNAPSHOT-standalone.jar sfg.core