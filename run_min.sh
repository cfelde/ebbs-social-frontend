#!/usr/bin/env bash

# Build and run minified version..
lein do clean, cljsbuild once min-frontpage min-posts min-admin, ring server
