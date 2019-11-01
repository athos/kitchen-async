(ns puppeteer-example.main
  (:require [kitchen-async.promise :as p]))

(def puppeteer (js/require "puppeteer"))

(defn -main []
  (p/let [browser (.launch puppeteer)
          page (.newPage browser)]
    (p/try
      (.goto page "https://clojure.org")
      (.screenshot page #js{:path "screenshot.png"})
      (p/catch :default e
        (js/console.error e))
      (p/finally
        (.close browser)))))

(comment

  ;; Without kitchen-async, you'll have to write something like:

  (defn -main [& args]
    (-> (.launch puppeteer)
        (.then (fn [browser]
                 (-> (.newPage browser)
                     (.then (fn [page]
                              (-> (.goto page "https://clojure.org")
                                  (.then #(.screenshot page #js{:path "screenshot.png"}))
                                  (.catch js/console.error)
                                  (.then #(.close browser))))))))))

  )
