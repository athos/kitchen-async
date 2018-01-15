(ns kitchen-async.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            kitchen-async.promise-test))

(enable-console-print!)

(doo-tests 'kitchen-async.promise-test)
