(ns ebbs-social-frontend.posts.event-dispatch
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn dispatch-vote
  [post-id vote]
  (rf/dispatch [:vote post-id vote]))

(defn dispatch-ordering
  [type value]
  (rf/dispatch [:ordering type (keyword (str/replace value " " "-"))]))

(defn dispatch-edit-post
  [field value]
  (rf/dispatch [:edit-post field value]))

(defn dispatch-edit-post-tag
  [tag]
  (rf/dispatch [:edit-post-tag tag]))

(defn dispatch-img-link-click
  [post-id nsfw e]
  (when nsfw
    (rf/dispatch [:nsfw-override post-id])
    (.preventDefault e)))

(defn dispatch-show-post-details
  [post-id]
  (rf/dispatch [:show-post-details post-id]))

(defn dispatch-save-selected-post
  [e]
  (rf/dispatch [:save-selected-post])
  (.preventDefault e))

(defn dispatch-mod-post
  [post-id e]
  (rf/dispatch [:mod-post post-id])
  (.preventDefault e))
