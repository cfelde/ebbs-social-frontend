(ns ebbs-social-frontend.frontpage.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as rf]
              [ebbs-social-frontend.frontpage.event-dispatch :as ed]
              [ebbs-social-frontend.frontpage.event-handlers :as eh]
              [ebbs-social-frontend.frontpage.query :as q]
              [ebbs-social-frontend.frontpage.view :as v]))

(defn on-js-reload
  [])

(defn ^:export inject-provider
  [provider]
  (when provider
    (rf/dispatch [:inject-provider provider]))
  (when (not provider)
    (rf/dispatch [:no-provider])))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [v/ui] (js/document.getElementById "app")))
