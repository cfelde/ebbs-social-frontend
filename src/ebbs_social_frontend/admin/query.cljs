(ns ebbs-social-frontend.admin.query
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :db
  (fn [db [_ & keys]]
    (get-in db keys)))

(rf/reg-sub
  :compressed-forum-data-length
  (fn [db _]
    (if-let [handler (:ebbs-handler db)]
      (let [post-data (u/post-data->encoded-data (-> db :posts (get 0)))
            deflated-data (u/deflate-content handler post-data)]
        (.-length deflated-data)))))
