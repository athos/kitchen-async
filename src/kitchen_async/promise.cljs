(ns kitchen-async.promise
  (:require-macros [kitchen-async.promise :as p]
                   [cljs.core.async.macros :refer [go]])
  (:require [clojure.core.async :as a]))

(defn resolve [x]
  (js/Promise.resolve x))

(defn reject [x]
  (js/Promise.reject x))

(defn then [p f]
  (.then p f))

(defn catch* [p f]
  (.catch p f))

(defn ->chan [p]
  (let [ch (a/chan)]
    (then p #(a/put! ch %))
    ch))

(defn <-chan [ch]
  (p/promise []
    (go (p/resolved (a/<! ch)))))
