(ns kitchen-async.specs.promise-macros
  (:require [clojure.spec.alpha :as s]
            [kitchen-async.utils :as utils]))

(s/fdef kitchen-async.promise/promise
  :args (s/cat :params (s/or :arity-0 (s/and vector? #(= (count %) 0))
                             :arity-1 (s/tuple simple-symbol?)
                             :arity-2 (s/tuple simple-symbol?
                                               simple-symbol?))
               :body (s/* any?)))

(defn- maybe-qualified? [sym x]
  (and (symbol? x)
       (or (= x sym)
           (= (utils/fixup-alias x)
              (symbol "kitchen-async.promise" (name sym))))))

(defn catch? [x]
  (maybe-qualified? 'catch x))

(defn finally? [x]
  (maybe-qualified? 'finally x))

(s/def ::error-type
  (s/or :type-name symbol?
        :default #{:default}))
(s/def ::error-name simple-symbol?)

(s/def ::try-body
  (s/* (s/or :simple-expr (complement seq?)
             :compound-expr (s/cat :op (fn [x]
                                         (not (or (catch? x)
                                                  (finally? x))))
                                    :args (s/* any?)))))

(s/def ::catch-clause
  (s/cat :catch catch?
         :error-type ::error-type
         :error-name ::error-name
         :catch-body (s/* any?)))

(s/def ::finally-clause
  (s/cat :finally finally?
         :finally-body (s/* any?)))

(s/def ::try-args
  (s/cat :try-body ::try-body
         :catch-clauses (s/* (s/spec ::catch-clause))
         :finally-clause (s/? (s/spec ::finally-clause))))

(s/fdef kitchen-async.promise/try
  :args ::try-args)
