(ns ebbs-social-frontend.misc.utils)

(def max-post-data-length 256)
(def max-post-meta-length 512)

(defn post-data->encoded-data
  [post]
  (into {} (remove #(-> % second nil?) {"1" (:title post)
                                        "2" (:body post)
                                        "3" (:post-link post)
                                        "4" (:image-link post)
                                        "5" (if (-> post :tags (get "Hide")) 1)
                                        "6" (if (-> post :tags (get "NSFW")) 1)})))

(defn post-data->encoded-meta
  [forum]
  (into {} (remove #(-> % second nil?) {"m" (if (-> forum :meta (get "Force moderation")) 1)
                                        "n" (if (-> forum :meta (get "Force NSFW")) 1)})))

(defn post->internal-post
  [post]
  (let [self-vote (get post "vote")]
    {:post-id     (or (get post "postId") -1)
     :author      (get post "author")
     :points      (get post "pointCounter")
     :timestamp   (get post "timestamp")
     :in-reply-to (get post "inReplyTo")
     :reply-count (get post "replyCounter")
     :title       (get-in post ["postData" "1"])
     :body        (get-in post ["postData" "2"])
     :post-link   (get-in post ["postData" "3"])
     :image-link  (get-in post ["postData" "4"])
     :tags        {"Hide" (> (get-in post ["postData" "5"]) 0)
                   "NSFW" (> (get-in post ["postData" "6"]) 0)}
     :meta        {"Force moderation" (> (get-in post ["postMeta" "m"]) 0)
                   "Force NSFW" (> (get-in post ["postMeta" "n"]) 0)}
     :vote        (cond
                    (> self-vote 0) :up
                    (< self-vote 0) :down
                    :else :none)}))

(defn author-meta->internal-meta
  [meta]
  {:karma (get meta "karma")})

(defn admins->internal-admins
  [admins]
  (letfn [(internal-admin
            [admin]
            {:address (get admin "address")
             :admin-status (get admin "adminStatus")
             :saving false})]
    (into {} (map #(vector (:address %) %) (map internal-admin admins)))))

(defn deflate-content
  [handler content]
  (let [content (clj->js content)]
    (.deflateContent handler content)))

