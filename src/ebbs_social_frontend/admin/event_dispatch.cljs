(ns ebbs-social-frontend.admin.event-dispatch
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-edit-post
  [field value]
  (rf/dispatch [:edit-post field value]))

(defn dispatch-edit-post-meta
  [tag]
  (rf/dispatch [:edit-post-meta tag]))

(defn dispatch-save-forum
  [e]
  (rf/dispatch [:save-forum])
  (.preventDefault e))

(defn dispatch-save-meta
  [e]
  (rf/dispatch [:save-meta])
  (.preventDefault e))

(defn dispatch-admin-status
  [address value]
  (let [admin-status (case value
                       "Admin + moderator" 3
                       "Only admin" 1
                       "Only moderator" 2
                       "Remove access" 0)]
    (rf/dispatch [:edit-admin-status address admin-status])))

(defn dispatch-save-admin-status
  [address e]
  (rf/dispatch [:save-admin-status address])
  (.preventDefault e))

(defn dispatch-new-admin-address
  [address]
  (rf/dispatch [:save-new-admin-address address]))
