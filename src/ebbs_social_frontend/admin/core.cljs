(ns ebbs-social-frontend.admin.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as rf]
              [ebbs-social-frontend.admin.event-dispatch :as ed]
              [ebbs-social-frontend.admin.event-handlers :as eh]
              [ebbs-social-frontend.admin.query :as q]
              [ebbs-social-frontend.admin.view :as v]))

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
