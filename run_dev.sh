#!/usr/bin/env bash

# This will build in dev mode with figwheel and open the browser
lein do clean, figwheel dev-frontpage dev-posts dev-admin
