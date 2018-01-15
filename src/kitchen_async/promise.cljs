(ns kitchen-async.promise
  (:refer-clojure :exclude [resolve])
  (:require-macros [kitchen-async.promise :as p])
  (:require goog.Promise))

(def ^:private %promise-impl
  (let [init (if (exists? js/Promise)
               js/Promise
               goog.Promise)]
    (atom init)))

(defn promise-impl []
  @%promise-impl)

(defn set-promise-impl! [impl]
  (reset! %promise-impl impl))

(defn resolve [x]
  (let [p (promise-impl)]
    (new p (fn [resolve] (resolve x)))))

(defn reject [x]
  (let [p (promise-impl)]
    (new p (fn [_ reject] (reject x)))))

(declare ->promise)

(defn then
  ([p f]
   (.then (->promise p) f))
  ([p f g]
   (.then (->promise p) f g)))

(defn catch* [p f]
  (then p nil f))

(defn finally* [p f]
  (then p f f))

(defn all [ps]
  (goog.Promise.all (clj->js (map ->promise ps))))

(defn race [ps]
  (goog.Promise.race (clj->js (map ->promise ps))))

(defn timeout
  ([ms] (timeout ms nil))
  ([ms v]
   (p/promise [resolve]
     (js/setTimeout #(resolve v) ms))))

(defprotocol Promisable
  (->promise* [this]))

(extend-protocol Promisable
  goog.Promise
  (->promise* [p] p)

  default
  (->promise* [x]
    (resolve x)))

(when (exists? js/Promise)
  (extend-type js/Promise
    Promisable
    (->promise* [p] p)))

(defn ->promise [x]
  (->promise* x))

(defn callback->promise [f & args]
  (p/promise [resolve reject]
    (letfn [(callback [err val]
              (if err
                (reject err)
                (resolve val)))]
      (apply f (concat args [callback])))))
