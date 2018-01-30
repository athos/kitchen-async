(ns kitchen-async.protocols.promisable)

(defprotocol Promisable
  (->promise* [this]))
