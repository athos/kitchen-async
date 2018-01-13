(ns puppeteer-example.main
  (:require [clojure.string :as str]
            [kitchen-async.promise :as p]
            puppeteer))

(enable-console-print!)

(defn main []
  (p/let [browser (puppeteer/launch)
          page (.newPage browser)]
    (.goto page "https://www.google.com")
    (.screenshot page #js{:path "screenshot.png"})
    (.close browser)))

(comment

  ;; Without kitchen-async, you have to write something like:

  (defn main []
    (-> (puppeteer/launch)
        (.then (fn [browser]
                 (-> (.newPage browser)
                     (.then (fn [page]
                              (.then (.goto page "https://www.google.com")
                                     #(.screenshot page #js{:path "screenshot.png"}))))
                     (.then #(.close browser)))))))

  )

(set! *main-cli-fn* main)
