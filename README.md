# ebbs-social-frontend

This is the frontend UI code for [EBBS social](https://github.com/cfelde/ebbs-social).

## Running locally

Assuming you have [Leiningen](https://leiningen.org/) installed you can run the UI locally with these two commands:

run_dev.sh: Will build and start in dev mode with Figwheel enabled
run_min.sh: Will build and start in production mode.

If you just want to try it out, see [ebbs.social](https://ebbs.social)

## Not too keen on "wacky lisp code"?

The UI is written in ClojureScript using [re-frame](https://github.com/Day8/re-frame) (Reagent/React).
If you're not used to that setup it can be a bit alien.

Instead, if you'd consider building a UI not using this code base, a good starting point can be the blockchain.js file you'll find under _js in resources.
This file contains all the logic we're using to fetch posts, add new posts, do replies, voting, admin, moderation, etc.

In addition, it's good to check out [EBBS social](https://github.com/cfelde/ebbs-social) which contains details on data formats.
