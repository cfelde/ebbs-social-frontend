#!/usr/bin/env bash

# Build minified version..
lein do clean, cljsbuild once min-frontpage min-posts min-admin
