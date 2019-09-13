(ns ebbs-social-frontend.admin.event-handlers
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
          abi (clj->js (get forum-contract-def "abi"))
          contract (if provider (.. provider -eth -Contract))
          forum (if (and contract forum-address abi)
                  (new contract abi forum-address))
          ebbs-handler (if (and provider forum)
                         (js/EbbsHandler. provider forum))]
      (if ebbs-handler
        (let [load-post-ids #{0}

              result {:db (assoc db :ebbs-handler ebbs-handler)
                      :load-post-by-ids [ebbs-handler load-post-ids]
                      :load-admins [ebbs-handler]}]

          result)

        {:db db}))))

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
  :load-admins
  (fn [[handler]]
    (p/alet [admins (p/await (.getAdmins handler))
             admins (js->clj admins)
             admins (u/admins->internal-admins admins)]
      (rf/dispatch [:set-admins admins]))))

(rf/reg-fx
  :save-forum
  (fn [[handler post-data]]
    (p/alet [result (p/await (.updatePost handler 0 (clj->js post-data)))]
      (rf/dispatch [:set-save-status :forum false]))))

(rf/reg-fx
  :save-meta
  (fn [[handler post-meta]]
    (p/alet [result (p/await (.updateMeta handler 0 (clj->js post-meta)))]
      (rf/dispatch [:set-save-status :meta false]))))

(rf/reg-fx
  :save-admin
  (fn [[handler {:keys [address admin-status]}]]
    (p/alet [result (p/await (.saveAdmin handler address admin-status))]
      (rf/dispatch [:reload-admins]))))

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    (letfn []

      {:db {:provider nil
            :ebbs-handler nil

            :forum-contract-def nil

            :no-provider false

            :forum-link nil
            :forum-address nil

            :posts {}

            :admins {:new {:address ""
                           :admin-status 3
                           :saving false}}

            :saving {:forum false
                     :meta false}}

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
        :init-forum-address provider))))

(rf/reg-event-fx
  :no-provider
  (fn [{:keys [db]} _]
    {:db (assoc db :no-provider true)}))

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
    {:db (reduce #(assoc-in %1 [:posts (:post-id %2)] %2) db posts)}))

(rf/reg-event-fx
  :edit-post
  (fn [{:keys [db]} [_ field value]]
    (let [saving (-> db :saving :forum)
          value (when (not (empty? value)) value)]
      (if saving
        {:db db}
        (if value
          {:db (assoc-in db [:posts 0 field] value)}
          {:db (update-in db [:posts 0] dissoc field)})))))

(rf/reg-event-fx
  :edit-post-meta
  (fn [{:keys [db]} [_ tag]]
    (let [saving (-> db :saving :meta)]
      (if saving
        {:db db}
        {:db (update-in db [:posts 0 :meta tag] not)}))))

(rf/reg-event-fx
  :save-forum
  (fn [{:keys [db]} _]
    (or (if-let [handler (:ebbs-handler db)]
          {:db (assoc-in db [:saving :forum] true)
           :save-forum [handler (u/post-data->encoded-data (-> db :posts (get 0)))]})
        {:db db})))

(rf/reg-event-fx
  :save-meta
  (fn [{:keys [db]} _]
    (or (if-let [handler (:ebbs-handler db)]
          {:db (assoc-in db [:saving :meta] true)
           :save-meta [handler (u/post-data->encoded-meta (-> db :posts (get 0)))]})
        {:db db})))

(rf/reg-event-fx
  :set-save-status
  (fn [{:keys [db]} [_ k v]]
    {:db (assoc-in db [:saving k] v)}))

(rf/reg-event-fx
  :edit-admin-status
  (fn [{:keys [db]} [_ address admin-status]]
    {:db (assoc-in db [:admins address :admin-status] admin-status)}))

(rf/reg-event-fx
  :save-admin-status
  (fn [{:keys [db]} [_ address]]
    (if-let [handler (:ebbs-handler db)]
      {:db (assoc-in db [:admins address :saving] true)
       :save-admin [handler (get-in db [:admins address])]}
      {:db db})))

(rf/reg-event-fx
  :save-new-admin-address
  (fn [{:keys [db]} [_ address]]
    {:db (assoc-in db [:admins :new :address] address)}))

(rf/reg-event-fx
  :set-admins
  (fn [{:keys [db]} [_ admins]]
    {:db (assoc db :admins (merge (:admins db) admins))}))

(rf/reg-event-fx
  :reload-admins
  (fn [{:keys [db]} _]
    {:db (assoc db :admins {:new {:address ""
                                  :admin-status 3
                                  :saving false}})
     :load-admins [(:ebbs-handler db)]}))
