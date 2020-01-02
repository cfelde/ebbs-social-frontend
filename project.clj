(defproject ebbs-social-frontend "0.1.0-SNAPSHOT"
  :description "Web frontend for EBBS social"
  :url "https://ebbs.social"
  :license {:name "Apache License Version 2.0"
            :url "http://www.apache.org/licenses/"}

  :min-lein-version "2.9.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent  "0.8.1"]
                 [reagent-utils "0.3.3"]
                 [re-frame "0.10.8"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [funcool/promesa "2.0.1"]

                 ; Server side stuff
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]]

  :plugins [[lein-figwheel "0.5.19"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-ring "0.12.5"]]

  :source-paths ["src"]

  ; Build with these:
  ; lein do clean, figwheel dev-frontpage dev-posts dev-admin
  ; lein do clean, cljsbuild once min-frontpage min-posts min-admin, ring server

  :cljsbuild {:builds
              [{:id "dev-posts"
                :source-paths ["src"]

                :figwheel {:on-jsload "ebbs-social-frontend.posts.core/on-js-reload"}

                :compiler {:main ebbs-social-frontend.posts.core
                           :asset-path "/_js/compiled/out-posts"
                           :output-to "resources/public/_js/compiled/ebbs_social_frontend_posts.js"
                           :output-dir "resources/public/_js/compiled/out-posts"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               {:id "dev-admin"
                :source-paths ["src"]

                :figwheel {:on-jsload "ebbs-social-frontend.admin.core/on-js-reload"}

                :compiler {:main ebbs-social-frontend.admin.core
                           :asset-path "/_js/compiled/out-admin"
                           :output-to "resources/public/_js/compiled/ebbs_social_frontend_admin.js"
                           :output-dir "resources/public/_js/compiled/out-admin"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               {:id "dev-frontpage"
                :source-paths ["src"]

                :figwheel {:on-jsload "ebbs-social-frontend.frontpage.core/on-js-reload"
                           :open-urls ["http://localhost:3449/"]}

                :compiler {:main ebbs-social-frontend.frontpage.core
                           :asset-path "/_js/compiled/out-frontpage"
                           :output-to "resources/public/_js/compiled/ebbs_social_frontend_frontpage.js"
                           :output-dir "resources/public/_js/compiled/out-frontpage"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               {:id "min-posts"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/_js/compiled/ebbs_social_frontend_posts.js"
                           :output-dir "target/out-posts-min"
                           :main ebbs-social-frontend.posts.core
                           :optimizations :advanced
                           :pretty-print false
                           :pseudo-names false
                           :externs ["externs/externs.js"]}}

               {:id "min-admin"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/_js/compiled/ebbs_social_frontend_admin.js"
                           :output-dir "target/out-admin-min"
                           :main ebbs-social-frontend.admin.core
                           :optimizations :advanced
                           :pretty-print false
                           :pseudo-names false
                           :externs ["externs/externs.js"]}}

               {:id "min-frontpage"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/_js/compiled/ebbs_social_frontend_frontpage.js"
                           :output-dir "target/out-frontpage-min"
                           :main ebbs-social-frontend.frontpage.core
                           :optimizations :advanced
                           :pretty-print false
                           :pseudo-names false
                           :externs ["externs/externs.js"]}}]}

  :ring {:handler ebbs-social-frontend.server.handler/dev-app}

  :figwheel {:css-dirs ["resources/public/_css"]
             :ring-handler ebbs-social-frontend.server.handler/dev-app}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.19"]]
                   :source-paths ["src" "dev"]
                   :clean-targets ^{:protect false} ["resources/public/_js/compiled"
                                                     :target-path]}})
