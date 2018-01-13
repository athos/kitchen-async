(defproject puppeteer-example "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [kitchen-async "0.1.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds
              [{:id "app"
                :source-paths ["src"]
                :compiler {:main puppeteer-example.main
                           :output-dir "target/compiled"
                           :output-to "target/main.js"
                           :optimizations :none
                           :target :nodejs
                           :install-deps true
                           :npm-deps {:puppeteer "1.0.0"}}}]})
