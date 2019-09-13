(ns ebbs-social-frontend.posts.query
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :db
  (fn [db [_ & keys]]
    (get-in db keys)))

(rf/reg-sub
  :selected-post
  (fn [db _]
    (let [posts (:posts db)
          edit-post-mode-enabled (-> db :edit-post-mode :enabled)
          selected-post-id (:selected-post-id db)
          post (get posts selected-post-id)]
      (if (and edit-post-mode-enabled post)
        (assoc post :title (if (> (count (:title post)) 0)
                             (str "Preview: " (:title post))
                             "Preview"))
        post))))

(rf/reg-sub
  :reply-to-post
  (fn [db _]
    (let [posts (:posts db)
          selected-post-id (:reply-to-post-id db)
          post (get posts selected-post-id)]
        post)))

(rf/reg-sub
  :posts
  (fn [db _]
    (letfn [(should-hide
              [post]
              (->> post :tags (filter second) (map first) (map str/lower-case) (filter #(= "hide" %)) empty? not))
            (is-moderated
              [post]
              (->> post :meta (filter second) (map first) (map str/lower-case) (filter #(= "force moderation" %)) empty? not))]
      (let [selected-post-id (:selected-post-id db)
            post-zero (-> db :posts (get 0))
            moderation-enabled (and (not (:is-mod-mode db)) (is-moderated post-zero))
            posts (remove should-hide (vals (dissoc (:posts db) selected-post-id -1 0)))
            posts (if moderation-enabled (filter is-moderated posts) posts)
            sort-by-type (:sort-by db)]
        (case sort-by-type
          :most-upvoted (reverse (sort-by :points posts))
          :most-replies (reverse (sort-by :reply-count posts))
          :newest (reverse (sort-by :timestamp posts))
          :oldest (sort-by :timestamp posts)
          :most-downvoted (sort-by :points posts)
          posts)))))

(rf/reg-sub
  :compressed-forum-data-length
  (fn [db _]
    (if-let [handler (:ebbs-handler db)]
      (let [post-data (u/post-data->encoded-data (-> db :posts (get (:selected-post-id db))))
            deflated-data (u/deflate-content handler post-data)]
        (.-length deflated-data)))))
