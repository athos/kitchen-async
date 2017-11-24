(defproject kitchen-async "0.1.0-SNAPSHOT"
  :description "Yet another async library for ClojureScript aiming for seamless use of Promises and core.async channels with ease"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-RC1"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"
                  :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:output-to "target/dev.js"
                           :optimizations :whitespace
                           :pretty-print true}}
               {:id "test"
                :source-paths ["src" "test"]
                :compiler {:output-to "target/test.js"
                           :main kitchen-async.runner
                           :optimizations :none}}]}

  :profiles {:dev {:dependencies [[doo "0.1.8"]]}})
