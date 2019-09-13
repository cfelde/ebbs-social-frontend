(ns ebbs-social-frontend.frontpage.event-dispatch
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-create-new-forum
  [_]
  (rf/dispatch [:create-new-forum]))

(defn dispatch-edit-forum
  [field value]
  (rf/dispatch [:edit-forum field value]))

(defn dispatch-edit-forum-tag
  [tag]
  (rf/dispatch [:edit-forum-meta tag]))

(defn dispatch-submit-new-forum
  [e]
  (rf/dispatch [:submit-new-forum])
  (.preventDefault e))
