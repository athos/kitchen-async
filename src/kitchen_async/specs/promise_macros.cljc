(ns kitchen-async.specs.promise-macros
  (:require [clojure.spec.alpha :as s]
            [kitchen-async.utils :as utils]))

(defn catch? [sym]
  (and (symbol? sym)
       (or (= sym 'catch)
           (= (utils/fixup-alias sym) 'kitchen-async.promise/catch))))

(s/def ::error-type
  (s/or :type-name symbol?
        :default #{:default}))
(s/def ::error-name simple-symbol?)

(s/def ::try-body
  (s/* (s/or :simple-expr (complement seq?)
             :non-catch-expr (s/cat :op (complement catch?)
                                    :args (s/* any?)))))

(s/def ::catch-clause
  (s/cat :catch catch?
         :error-type ::error-type
         :error-name ::error-name
         :catch-body (s/* any?)))

(s/def ::try-args
  (s/cat :try-body ::try-body
         :catch-clauses (s/* (s/spec ::catch-clause))))

(s/fdef kitchen-async.promise/try
  :args ::try-args)
