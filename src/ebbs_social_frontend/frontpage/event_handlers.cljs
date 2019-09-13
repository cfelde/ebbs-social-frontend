(ns ebbs-social-frontend.frontpage.event-handlers
  (:require [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [re-frame.core :as rf]
            [promesa.core :as p :include-macros true]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-fx
  :find-network
  (fn [[provider]]
    (p/alet [result (p/await (. (.. provider -eth -net) getNetworkType))]
      (rf/dispatch [:set-network result]))))

(rf/reg-fx
  :deploy-forum
  (fn [[handler forum-contract-def data meta]]
    (let [abi (clj->js (get forum-contract-def "abi"))
          bytecode (clj->js (get forum-contract-def "bytecode"))
          data (clj->js data)
          meta (clj->js meta)]
      (p/alet [instance (p/await (.deployForum handler abi bytecode data meta))
               contract-address (aget instance "options" "address")]
        (set! (.. js/window -location -href) (str "/eth/" contract-address))))))

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    {:db {:provider nil
          :ebbs-handler nil
          :network nil

          :forum-contract-def nil

          :no-provider false

          :create-new false

          :new-forum {:title nil
                      :body nil
                      :post-link nil
                      :image-link nil
                      :meta {"Force moderation" false "Force NSFW" false}}

          :submitting-new-forum false}

     :http-xhrio {:method          :get
                  :uri             "/_contracts/EbbsSocialForum.json"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? false})
                  :on-success      [:load-contract-success]
                  :on-failure      [:load-contract-error]}}))

(rf/reg-event-fx
  :load-contract-success
  (fn [{:keys [db]} [_ content]]
    {:db (assoc db :forum-contract-def content)}))

(rf/reg-event-fx
  :load-contract-error
  (fn [{:keys [db]} [_ result]]
    (js/console.error (clj->js result))
    {:db db}))

(rf/reg-event-fx
  :inject-provider
  (fn [{:keys [db]} [_ provider]]
    {:db (assoc db :provider provider
                   :ebbs-handler (js/EbbsHandler. provider nil))
     :find-network [provider]}))

(rf/reg-event-fx
  :no-provider
  (fn [{:keys [db]} _]
    {:db (assoc db :no-provider true)}))

(rf/reg-event-fx
  :set-network
  (fn [{:keys [db]} [_ network]]
    {:db (assoc db :network network)}))

(rf/reg-event-fx
  :create-new-forum
  (fn [{:keys [db]} _]
    {:db (assoc db :create-new true)}))

(rf/reg-event-fx
  :edit-forum
  (fn [{:keys [db]} [_ field value]]
    (if (:submitting-new-forum db)
      {:db db}
      (let [value (when (not (empty? value)) value)]
        (if value
          {:db (assoc-in db [:new-forum field] value)}
          {:db (update-in db [:new-forum] dissoc field)})))))

(rf/reg-event-fx
  :edit-forum-meta
  (fn [{:keys [db]} [_ tag]]
    {:db (if (:submitting-new-forum db)
           db
           (update-in db [:new-forum :meta tag] not))}))

(rf/reg-event-fx
  :submit-new-forum
  (fn [{:keys [db]} _]
    (let [ebbs-handler (:ebbs-handler db)
          forum-contract-def (:forum-contract-def db)
          new-forum (:new-forum db)
          data (u/post-data->encoded-data new-forum)
          meta (u/post-data->encoded-meta new-forum)]
      (if (and ebbs-handler forum-contract-def)
        {:db (assoc db :submitting-new-forum true)
         :deploy-forum [ebbs-handler forum-contract-def data meta]}
        {:db db}))))
