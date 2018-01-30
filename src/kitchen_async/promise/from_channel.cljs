(ns kitchen-async.promise.from-channel
  (:require [clojure.core.async :as a]
            [clojure.core.async.impl.channels :refer [ManyToManyChannel]]
            [kitchen-async.promise :as p]
            [kitchen-async.protocols.promisable :refer [Promisable]]))

(extend-protocol Promisable
  ManyToManyChannel
  (->promise* [c]
    (p/promise [resolve reject]
      (a/go
        (let [x (a/<! c)]
          (if (instance? js/Error x)
            (reject x)
            (resolve x)))))))
