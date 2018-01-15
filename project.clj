(defproject kitchen-async "0.1.0-SNAPSHOT"
  :description "A Promise library for ClojureScript, or a poor man's core.async"
  :url "https://github.com/athos/kitchen-async"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.4.474"
                  :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:output-to "target/dev/kitchen_async.js"
                           :output-dir "target/dev"
                           :optimizations :none
                           :pretty-print true}}
               {:id "test"
                :source-paths ["src" "test/common" "test/jvm"]
                :compiler {:output-to "target/test/kitchen_async.js"
                           :output-dir "target/test"
                           :main kitchen-async.runner
                           :optimizations :whitespace}}
               {:id "min-test"
                :source-paths ["src" "test/common" "test/jvm"]
                :compiler {:output-to "target/min-test/kitchen_async.js"
                           :output-dir "target/min-test"
                           :main kitchen-async.runner
                           :optimizations :advanced}}
               {:id "node-test"
                :source-paths ["src" "test/common" "test/jvm"]
                :compiler {:output-to "target/node-test/kitchen_async.js"
                           :output-dir "target/node-test"
                           :main kitchen-async.runner
                           :target :nodejs
                           :optimizations :none}}
               {:id "node-min-test"
                :source-paths ["src" "test/common" "test/jvm"]
                :compiler {:output-to "target/node-min-test/kitchen_async.js"
                           :output-dir "target/node-min-test"
                           :main kitchen-async.runner
                           :target :nodejs
                           :optimizations :advanced}}]}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [doo "0.1.8"]]}}

  :aliases {"test-all" ["do" ["test"] ["test-min"]]
            "test" ["do"
                    ["doo" "phantom" "test" "once"]
                    ["doo" "node" "node-test" "once"]]
            "test-min" ["do"
                        ["doo" "phantom" "min-test" "once"]
                        ["doo" "node" "node-min-test" "once"]]}
  )
