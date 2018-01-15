(ns kitchen-async.runner
  (:require [clojure.test :refer [run-tests]]
            kitchen-async.promise-test))

(enable-console-print!)

(run-tests 'kitchen-async.promise-test)
