(ns ebbs-social-frontend.posts.event-handlers
  (:require [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [re-frame.core :as rf]
            [promesa.core :as p :include-macros true]
            [ebbs-social-frontend.misc.utils :as u]
            [clojure.string :as str]))

;; -- Domino 2 - Event Handlers -----------------------------------------------

(defn manage-ebbs-handler-init
  [db]
  (if (:ebbs-handler db)
    {:db db}
    (let [provider (:provider db)
          forum-contract-def (:forum-contract-def db)
          forum-address (:forum-address db)
          user-address (-> db :user :address)
          abi (clj->js (get forum-contract-def "abi"))
          contract (if provider (.. provider -eth -Contract))
          forum (if (and contract forum-address user-address abi)
                  (new contract abi forum-address))
          ebbs-handler (if (and provider forum)
                         (js/EbbsHandler. provider forum))]
      (if ebbs-handler
        (let [load-post-ids #{0}
              load-post-ids (if (> (:selected-post-id db) 0)
                              (conj load-post-ids (:selected-post-id db))
                              load-post-ids)

              load-post-ids (if (:reply-to-post-id db)
                              (conj load-post-ids (:reply-to-post-id db))
                              load-post-ids)

              result {:db (assoc db :ebbs-handler ebbs-handler)
                      :init-user-details [ebbs-handler user-address]
                      :load-post-by-ids [ebbs-handler load-post-ids]}

              result (if (:load-posts-in-reply-to-selected db)
                       (assoc result :load-posts [ebbs-handler (:selected-post-id db) (:from-within db)])
                       result)

              result (if (:load-posts-on-address db)
                       (assoc result :load-posts-on-address [ebbs-handler (:from-within db)])
                       result)

              result (if (:load-replies-on-address db)
                       (assoc result :load-replies-on-address [ebbs-handler (:from-within db)])
                       result)]

          result)

        {:db db}))))

(rf/reg-fx
  :init-user-address
  (fn [provider]
    (p/alet [user-addresses (p/await (.getAccounts (.-eth provider)))]
      (if (and user-addresses (> (alength user-addresses) 0))
        (rf/dispatch [:set-user-address (aget user-addresses 0)])))))

(rf/reg-fx
  :init-forum-address
  (fn [provider]
    (letfn [(extract-only-address
              [path]
              (cond
                (str/starts-with? path "/eth/") (str/replace-first path "/eth/" "")
                (str/includes? path "@") (last (str/split path #"@" 2))))

            (is-pure-address?
              [address]
              (re-matches #"^0x[a-fA-F0-9]{40}$" address))

            (resolve-address
              [domain]
              ; TODO
              nil)]

      (let [path js/window.location.pathname
            address (extract-only-address path)]
        (if (is-pure-address? address)
          (do
            (rf/dispatch [:set-forum-address address])
            (rf/dispatch [:set-forum-link address]))
          (resolve-address address))))))

(rf/reg-fx
  :init-user-details
  (fn [[handler user-address]]
    (p/alet [user-details (p/await (.getUserDetails handler user-address))
             user-details (js->clj user-details)]
      (rf/dispatch [:set-user-details
                    (get user-details "isAdmin")
                    (get user-details "isMod")
                    (get user-details "karma")]))))

(rf/reg-fx
  :load-post-by-ids
  (fn [[handler postIds]]
    (letfn [(load
              [postId]
              (p/alet [post (p/await (.getPost handler postId))
                       post (js->clj post)
                       post (u/post->internal-post post)]
                (rf/dispatch [:set-posts [post]])))]
      (doall (map load (into (sorted-set) postIds))))))

(rf/reg-fx
  :load-posts
  (fn [[handler in-reply-to-post-id from-within]]
    (let [time-range (condp = from-within
                       :last-24-hours (* 1000 60 60 24)
                       :last-3-days (* 1000 60 60 24 3)
                       :last-7-days (* 1000 60 60 24 7))]

      (if in-reply-to-post-id
        (p/alet [posts (p/await (.getLatestPostsByReplyId handler in-reply-to-post-id time-range))
                 posts (js->clj posts)
                 posts (map u/post->internal-post posts)]
          (rf/dispatch [:set-posts posts]))

        (p/alet [posts (p/await (.getLatestPostsOverall handler time-range))
                 posts (js->clj posts)
                 posts (map u/post->internal-post posts)]
          (rf/dispatch [:set-posts posts]))))))

(rf/reg-fx
  :load-posts-on-address
  (fn [[handler from-within]]
    (let [time-range (condp = from-within
                       :last-24-hours (* 1000 60 60 24)
                       :last-3-days (* 1000 60 60 24 3)
                       :last-7-days (* 1000 60 60 24 7))
          path js/window.location.pathname
          address (if (re-matches #"/posts/0x([0-9a-fA-F]+)@.*" path) (str "0x" (last (re-matches #"/posts/0x([0-9a-fA-F]+)@.*" path))))]

      (if address
        (p/alet [posts (p/await (.getLatestPostsByAddress handler address time-range))
                 posts (js->clj posts)
                 posts (map u/post->internal-post posts)]
          (rf/dispatch [:set-posts posts]))))))

(rf/reg-fx
  :load-replies-on-address
  (fn [[handler from-within]]
    (let [time-range (condp = from-within
                       :last-24-hours (* 1000 60 60 24)
                       :last-3-days (* 1000 60 60 24 3)
                       :last-7-days (* 1000 60 60 24 7))
          path js/window.location.pathname
          address (if (re-matches #"/replies/0x([0-9a-fA-F]+)@.*" path) (str "0x" (last (re-matches #"/replies/0x([0-9a-fA-F]+)@.*" path))))]

      (if address
        (p/alet [posts (p/await (.getLatestRepliesByAddress handler address time-range))
                 posts (js->clj posts)
                 posts (map u/post->internal-post posts)]
          (rf/dispatch [:set-posts posts]))))))

(rf/reg-fx
  :load-post-meta
  (fn [[handler authors]]
    (letfn [(load-authors-meta
              [author]
              (p/alet [meta (p/await (.getAuthorMeta handler author))
                       meta (js->clj meta)
                       meta (u/author-meta->internal-meta meta)]
                (rf/dispatch [:set-author-meta author meta])))]
      (doall (map load-authors-meta (into #{} authors))))))

(rf/reg-fx
  :save-new-post
  (fn [[handler in-reply-to-post-id post-data forum-link]]
    (p/alet [post-id (p/await (.createPost handler in-reply-to-post-id (clj->js post-data)))]
      (set! (.. js/window -location -href) (str "/post/" post-id "@" forum-link)))))

(rf/reg-fx
  :save-existing-post
  (fn [[handler post-id post-data forum-link]]
    (p/alet [result (p/await (.updatePost handler post-id (clj->js post-data)))]
      (set! (.. js/window -location -href) (str "/post/" post-id "@" forum-link)))))

(rf/reg-fx
  :vote
  (fn [[handler post-id points]]
    (p/alet [result (p/await (.vote handler post-id points))])))

(rf/reg-fx
  :save-mod-status
  (fn [[handler post-id mod-status]]
    (p/alet [result (p/await (.setModStatus handler post-id mod-status))])))

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    (letfn [(determine-edit-post-mode
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/new-post@") {:enabled true
                                                        :saving false
                                                        :title-mode :new}
                  (str/starts-with? path "/edit-post/") {:enabled true
                                                         :saving false
                                                         :title-mode :edit}
                  (str/starts-with? path "/reply-to-post/") {:enabled true
                                                             :saving false
                                                             :title-mode :reply}

                  :else {:enabled false})))

            (determine-reply-to-post
              []
              (let [path js/window.location.pathname]
                (cond
                  (re-matches #"/reply-to-post/([0-9]+)@.*" path) (js/parseInt (last (re-matches #"/reply-to-post/([0-9]+)@.*" path)))
                  :else nil)))

            (determine-selected-post
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/new-post@") -1
                  (str/starts-with? path "/reply-to-post/") -1
                  (str/starts-with? path "/mod@") nil
                  (re-matches #"/post/([0-9]+)@.*" path) (js/parseInt (last (re-matches #"/post/([0-9]+)@.*" path)))
                  (re-matches #"/edit-post/([0-9]+)@.*" path) (js/parseInt (last (re-matches #"/edit-post/([0-9]+)@.*" path)))

                  :else 0)))

            (determine-if-loading-posts
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/eth/") true
                  (str/starts-with? path "/post/") true
                  (str/starts-with? path "/mod@") true

                  :else false)))

            (determine-if-loading-posts-on-address
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/posts/") true

                  :else false)))

            (determine-if-loading-replies-on-address
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/replies/") true

                  :else false)))

            (determine-if-mod-mode
              []
              (let [path js/window.location.pathname]
                (cond
                  (str/starts-with? path "/mod@") true

                  :else false)))]

      {:db {:provider nil
            :ebbs-handler nil

            :forum-contract-def nil

            :no-provider false

            :sort-by :most-upvoted
            :from-within :last-24-hours

            :edit-post-mode (determine-edit-post-mode)

            :reply-to-post-id (determine-reply-to-post)
            :selected-post-id (determine-selected-post)
            :load-posts-in-reply-to-selected (determine-if-loading-posts)
            :load-posts-on-address (determine-if-loading-posts-on-address)
            :load-replies-on-address (determine-if-loading-replies-on-address)
            :is-mod-mode (determine-if-mod-mode)

            :user {:address nil
                   :is-admin false
                   :is-mod false
                   :karma nil}

            :forum-link nil
            :forum-address nil

            :posts {-1 (u/post->internal-post nil)}

            :meta-data nil}

       :http-xhrio {:method          :get
                    :uri             "/_contracts/IEbbsSocialForum.json"
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? false})
                    :on-success      [:load-contract-success]
                    :on-failure      [:load-contract-error]}})))

(rf/reg-event-fx
  :load-contract-success
  (fn [{:keys [db]} [_ content]]
    (let [db (assoc db :forum-contract-def content)]
      (manage-ebbs-handler-init db))))

(rf/reg-event-fx
  :load-contract-error
  (fn [{:keys [db]} [_ result]]
    (js/console.error (clj->js result))
    {:db db}))

(rf/reg-event-fx
  :inject-provider
  (fn [{:keys [db]} [_ provider]]
    (let [db (assoc db :provider provider)]
      (assoc (manage-ebbs-handler-init db)
        :init-user-address provider
        :init-forum-address provider))))

(rf/reg-event-fx
  :no-provider
  (fn [{:keys [db]} _]
    {:db (assoc db :no-provider true)}))

(rf/reg-event-fx
  :set-user-address
  (fn [{:keys [db]} [_ user-address]]
    (manage-ebbs-handler-init (-> db
                                  (assoc-in [:user :address] user-address)
                                  (assoc-in [:posts -1 :author] user-address)))))

(rf/reg-event-fx
  :set-user-details
  (fn [{:keys [db]} [_ is-admin is-mod karma]]
    {:db (-> db
             (assoc-in [:user :is-admin] is-admin)
             (assoc-in [:user :is-mod] is-mod)
             (assoc-in [:user :karma] karma))}))

(rf/reg-event-fx
  :set-forum-address
  (fn [{:keys [db]} [_ forum-address]]
    (manage-ebbs-handler-init (assoc db :forum-address forum-address))))

(rf/reg-event-fx
  :set-forum-link
  (fn [{:keys [db]} [_ forum-link]]
    {:db (assoc db :forum-link forum-link)}))

(rf/reg-event-fx
  :set-posts
  (fn [{:keys [db]} [_ posts]]
    (let [handler (:ebbs-handler db)]
      {:db (reduce #(assoc-in %1 [:posts (:post-id %2)] %2) db posts)
       :load-post-meta [handler (map :author posts)]})))

(rf/reg-event-fx
  :set-author-meta
  (fn [{:keys [db]} [_ author meta]]
    {:db (assoc-in db [:meta-data :authors author] meta)}))

(rf/reg-event-fx
  :ordering
  (fn [{:keys [db]} [_ type value]]
    (let [handler (:ebbs-handler db)]
      (if (and handler (= :from-within type))
        (cond
          (:load-posts-in-reply-to-selected db)
          {:db (-> db
                   (assoc type value)
                   (assoc :posts (->> db :posts (remove #(-> % first (> 0))) (into {}))))
           :load-posts [handler (:selected-post-id db) value]}

          (:load-posts-on-address db)
          {:db (-> db
                   (assoc type value)
                   (assoc :posts (->> db :posts (remove #(-> % first (> 0))) (into {}))))
           :load-posts-on-address [handler value]}

          (:load-replies-on-address db)
          {:db (-> db
                   (assoc type value)
                   (assoc :posts (->> db :posts (remove #(-> % first (> 0))) (into {}))))
           :load-replies-on-address [handler value]})

        {:db (assoc db type value)}))))

(rf/reg-event-fx
  :vote
  (fn [{:keys [db]} [_ post-id vote]]
    (let [handler (:ebbs-handler db)]
      (if (get (:posts db) post-id)
        {:db (assoc-in db [:posts post-id :new-vote] vote)

         :vote [handler post-id (case vote
                                  :up 1
                                  :down -1
                                  0)]}
        {:db db}))))

(rf/reg-event-fx
  :edit-post
  (fn [{:keys [db]} [_ field value]]
    (let [saving (-> db :edit-post-mode :saving)
          selected-post-id (:selected-post-id db)
          value (when (not (empty? value)) value)]
      (if saving
        {:db db}
        (if value
          {:db (assoc-in db [:posts selected-post-id field] value)}
          {:db (update-in db [:posts selected-post-id] dissoc field)})))))

(rf/reg-event-fx
  :edit-post-tag
  (fn [{:keys [db]} [_ tag]]
    (let [saving (-> db :edit-post-mode :saving)
          selected-post-id (:selected-post-id db)]
      (if saving
        {:db db}
        {:db (update-in db [:posts selected-post-id :tags tag] not)}))))

(rf/reg-event-fx
  :nsfw-override
  (fn [{:keys [db]} [_ post-id]]
    {:db (assoc-in db [:posts post-id :nsfw-override] true)}))

(rf/reg-event-fx
  :show-post-details
  (fn [{:keys [db]} [_ post-id]]
    {:db (update-in db [:posts post-id :show-details] not)}))

(rf/reg-event-fx
  :save-selected-post
  (fn [{:keys [db]} _]
    (or (if-let [handler (:ebbs-handler db)]
          (let [mode (-> db :edit-post-mode :title-mode)
                selected-post-id (:selected-post-id db)]
            (condp = mode
              :new {:db (assoc-in db [:edit-post-mode :saving] true)
                    :save-new-post [handler 0 (u/post-data->encoded-data (-> db :posts (get -1))) (:forum-link db)]}
              :reply {:db (assoc-in db [:edit-post-mode :saving] true)
                      :save-new-post [handler (:reply-to-post-id db) (u/post-data->encoded-data (-> db :posts (get -1))) (:forum-link db)]}
              :edit {:db (assoc-in db [:edit-post-mode :saving] true)
                     :save-existing-post [handler selected-post-id (u/post-data->encoded-data (-> db :posts (get selected-post-id))) (:forum-link db)]})))
        {:db db})))

(rf/reg-event-fx
  :mod-post
  (fn [{:keys [db]} [_ post-id]]
    (or (if-let [handler (:ebbs-handler db)]
          (let [db (update-in db [:posts post-id :meta "Force moderation"] not)
                mod-status (-> db :posts (get post-id) :meta (get "Force moderation"))]
            {:db db
             :save-mod-status [handler post-id mod-status]}))

        {:db db})))
