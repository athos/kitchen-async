# kitchen-async
[![Clojars Project](https://img.shields.io/clojars/v/kitchen-async.svg)](https://clojars.org/kitchen-async)
[![CircleCI](https://circleci.com/gh/athos/kitchen-async.svg?style=shield)](https://circleci.com/gh/athos/kitchen-async)

A Promise library for ClojureScript, or a poor man's core.async

It features:
- syntactic support for writing asynchronous code handling Promises as easily as with `async/await` in ECMAScript
- available on self-hosted ClojureScript environments, such as [Lumo](https://github.com/anmonteiro/lumo)/[Planck](https://github.com/mfikes/planck)
- seamless integration with core.async channels

## Example

Assume you are writing some `Promise`-heavy async code in ClojureScript (e.g. [Google's Puppeteer](https://github.com/GoogleChrome/puppeteer) provides such a collection of APIs). Then, if you only use raw JavaScript interop facilities for it, you would have to write something like this:

```clj
(-> (puppeteer/launch)
    (.then (fn [browser]
             (-> (.newPage browser)
                 (.then (fn [page]
                          (.then (.goto page "https://www.google.com")
                                 #(.screenshot page #js{:path "screenshot.png"}))))
                 (.then #(.close browser))))))
```

`kitchen-async` provides more succinct, "direct style" syntactic sugar for those things, which you may find similar to `async/await` in ECMAScript 2017:

```clj
(require '[kitchen-async.promise :as p])

(p/let [browser (puppeteer/launch)
        page (.newPage browser)]
  (.goto page "https://www.google.com")
  (.screenshot page #js{:path "screenshot.png"})
  (.close browser))
```

## Installation

Add the following to your `:dependencies`:

[![Clojars Project](https://clojars.org/kitchen-async/latest-version.svg)](https://clojars.org/kitchen-async)

Or, if you'd rather use an unstable version of the library, you can do that easily via [`deps.edn`](https://clojure.org/guides/deps_and_cli) as well:

```clj
github-athos/kitchen-async {:git/url "https://github.com/athos/kitchen-async.git" :sha <commit sha hash>}
```

## Usage

FIXME

<!--

## Why not use core.async?

[`core.async`](https://github.com/clojure/core.async) also provides similar async functionalities to `kitchen-async` (and as you may know, it's more powerful in fact), while I believe there are still some rooms where `kitchen-async` shines, such as blah blah blah

-->

## License

Copyright © 2017 Shogo Ohta

Distributed under the Eclipse Public License 1.0.
