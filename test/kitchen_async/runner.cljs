(ns kitchen-async.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            kitchen-async.promise-test))

(doo-tests 'kitchen-async.promise-test)
