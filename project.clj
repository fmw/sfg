(defproject sfg "0.1.0-SNAPSHOT"
  :description "Minimal example for Storm field grouping/parallelism question."
  :url "https://github.com/ofmw/sfg"
  :license {:name "Apache License, version 2"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[slingshot "0.10.3"]
                 [clj-time "0.4.5"]]
  :aot :all
  :profiles
  {:dev {:dependencies [[storm "0.8.2"]
                        [org.clojure/clojure "1.4.0"]]}}
  :min-lein-version "2.0.0")