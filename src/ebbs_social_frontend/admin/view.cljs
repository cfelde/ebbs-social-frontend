(ns ebbs-social-frontend.admin.view
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [ebbs-social-frontend.admin.event-dispatch :as d]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn parse-body
  [container body]
  (let [body-parts (str/split-lines body)
        body-parts (into [] (butlast (apply concat [container] (map #(list % [:br]) body-parts))))]
    body-parts))

(defn top-header
  []
  (let [forum-link @(rf/subscribe [:db :forum-link])]
    [:h1#top-header.container-fluid
     [:a {:href "/"} "ES/"]
     [:img#top-header-eth-logo {:src "/_img/ethereum-logo.svg"}]
     "/"
     [:a {:href (str "/eth/" forum-link)} forum-link]]))

(defn top-desc
  []
  (let [title @(rf/subscribe [:db :posts 0 :title])
        body @(rf/subscribe [:db :posts 0 :body])
        image-link @(rf/subscribe [:db :posts 0 :image-link])
        post-link @(rf/subscribe [:db :posts 0 :post-link])]
    [:div#top-description.container-fluid
       (when image-link
         (if post-link
           [:div#top-description-image [:a {:href post-link :target "_blank"} [:img {:src image-link}]]]
           [:div#top-description-image [:img {:src image-link}]]))
       [:div#top-description-text
        (when title [:h2#top-description-title title])
        (when body (parse-body :div#top-description-body body))]]))

(defn edit-forum
  []
  (let [post @(rf/subscribe [:db :posts 0])
        saving @(rf/subscribe [:db :saving :forum])
        data-length @(rf/subscribe [:compressed-forum-data-length])
        remaining-length (- u/max-post-data-length data-length)]
    [:div#edit-post.container
     [:div.row
      [:h1#edit-post-header "Edit forum"]]

     [:div.row
      [:form
       [:div#edit-post-title-form-group.form-group.row
        [:label
         {:for "edit-post-title"
          :class "col-sm-2 col-form-label"}
         "Title"]
        [:div.col-sm-10
         [:input
          {:type "text"
           :class "form-control"
           :id "edit-post-title"
           :value (:title post)
           :on-change #(d/dispatch-edit-post :title (-> % .-target .-value))}]]]
       [:div#edit-post-body-form-group.form-group.row
        [:label
         {:for "edit-post-body"
          :class "col-sm-2 col-form-label"}
         "Description"]
        [:div.col-sm-10
         [:textarea
          {:type "textarea"
           :class "form-control"
           :id "edit-post-body"
           :value (:body post)
           :on-change #(d/dispatch-edit-post :body (-> % .-target .-value))}]]]
       [:div#edit-post-link-form-group.form-group.row
        [:label
         {:for "edit-post-link"
          :class "col-sm-2 col-form-label"}
         "Link URL"]
        [:div.col-sm-10
         [:input
          {:type "text"
           :class "form-control"
           :id "edit-post-link"
           :value (:post-link post)
           :on-change #(d/dispatch-edit-post :post-link (-> % .-target .-value))}]]]
       [:div#edit-post-image-form-group.form-group.row
        [:label
         {:for "edit-post-image"
          :class "col-sm-2 col-form-label"}
         "Image URL"]
        [:div.col-sm-10
         [:input
          {:type "text"
           :class "form-control"
           :id "edit-post-image"
           :value (:image-link post)
           :on-change #(d/dispatch-edit-post :image-link (-> % .-target .-value))}]]]

       [:div#edit-post-submit-form-group.form-group.row
        [:div.col-sm-2
         (if saving
           [:button
            {:type "submit"
             :class "btn btn-primary"
             :disabled true}
            [:span {:class "spinner-border spinner-border-sm"
                    :role "status"
                    :aria-hidden "true"}]
            [:span.sr-only "Saving.."]]
           (if (>= remaining-length 0)
             [:button {:type "submit" :class "btn btn-primary" :on-click d/dispatch-save-forum} "Save"]
             [:button {:type "submit" :class "btn btn-primary" :disabled true} "Save"]))]
        (if (>= remaining-length 0)
          [:div.remaining-storage-space.text-green.col-sm-10 "Remaining space: " remaining-length]
          [:div.remaining-storage-space.text-red.col-sm-10 "Remaining space: " remaining-length])]]]]))

(defn edit-meta
  []
  (let [post @(rf/subscribe [:db :posts 0])
        saving @(rf/subscribe [:db :edit-post-mode :saving])]
    (letfn [(render-tag
              [[tag value]]
              (let [tag-id (gensym "tag-")]
                [:div
                 {:key (gensym)
                  :class "form-check form-check-inline"}
                 [:input {:class "form-check-input"
                          :type "checkbox"
                          :id tag-id
                          :value tag
                          :checked value
                          :on-change #(d/dispatch-edit-post-meta tag)}]
                 [:label {:class "form-check-label"
                          :for tag-id}
                  tag]]))]

      [:div#edit-meta.container
       [:div.row
        [:h1#edit-meta-header "Edit meta"]]

       [:div.row
        [:form
         [:div#edit-meta-tags
          (map render-tag (sort (:meta post)))]

         [:div#edit-meta-submit-form-group.form-group.row
          [:div.col-sm-2
           (if saving
             [:button
              {:type "submit"
               :class "btn btn-primary"
               :disabled true}
              [:span {:class "spinner-border spinner-border-sm"
                      :role "status"
                      :aria-hidden "true"}]
              [:span.sr-only "Saving.."]]
             [:button {:type "submit" :class "btn btn-primary" :on-click d/dispatch-save-meta} "Save"])]]]]])))

(defn edit-admins
  []
  (let [new-admin (:new @(rf/subscribe [:db :admins]))
        existing-admins (sort-by :address (vals (dissoc @(rf/subscribe [:db :admins]) :new)))]
    (letfn [(render-admin
              [{:keys [address admin-status saving]}]
              [:div {:key address
                     :class "row edit-admin-row"}
               [:form
                [:div.form-row.align-items-center
                 [:div.col-auto
                  [:label {:for (str "admin-status-" address)} address]]
                 [:div.col-auto
                  [:select {:id (str "admin-status-" address)
                            :class "form-control"
                            :disabled saving
                            :on-change #(d/dispatch-admin-status address (-> % (.-target) (.-value)))
                            :value (case admin-status
                                     3 "Admin + moderator"
                                     2 "Only moderator"
                                     1 "Only admin"
                                     0 "Remove access"
                                     :else nil)}
                   (map #(vector :option {:key (gensym)} %)
                        ["Admin + moderator" "Only admin" "Only moderator" "Remove access"])]]
                 [:div.col-auto
                  (if saving
                    [:button {:type "submit"
                              :class "btn btn-primary"
                              :disabled true}
                     [:span {:class "spinner-border spinner-border-sm"
                             :role "status"
                             :aria-hidden "true"}]
                     [:span.sr-only "Saving.."]]

                    [:button {:type "submit"
                              :class "btn btn-primary"
                              :on-click (partial d/dispatch-save-admin-status address)}
                     "Save"])]]]])]

      [:div#edit-admin.container
       [:div.row
        [:h1#edit-admin-header "Edit admins"]]

       [:div.row.edit-admin-row
        [:form
         [:div.form-row.align-items-center
          [:div.col-auto
           [:input {:type "text"
                    :placeholder "New admin address"
                    :disabled (:saving new-admin)
                    :value (:address new-admin)
                    :on-change #(d/dispatch-new-admin-address (-> % (.-target) (.-value)))}]]
          [:div.col-auto
           [:select {:id (str "admin-status-new")
                     :class "form-control"
                     :on-change #(d/dispatch-admin-status :new (-> % (.-target) (.-value)))
                     :disabled (:saving new-admin)
                     :value (case (:admin-status new-admin)
                              3 "Admin + moderator"
                              2 "Only moderator"
                              1 "Only admin"
                              0 "Remove access"
                              "Admin + moderator")}
            (map #(vector :option {:key (gensym)} %1) ["Admin + moderator" "Only admin" "Only moderator" "Remove access"])]]
          [:div.col-auto
           (if (:saving new-admin)
             [:button {:type "submit"
                       :class "btn btn-primary"
                       :disabled true}
              [:span {:class "spinner-border spinner-border-sm"
                      :role "status"
                      :aria-hidden "true"}]
              [:span.sr-only "Saving.."]]

             [:button {:type "submit"
                       :class "btn btn-primary"
                       :on-click (partial d/dispatch-save-admin-status :new)
                       :disabled (if (re-matches #"^0x[a-fA-F0-9]{40}$" (or (:address new-admin) "")) false true)}
              "Save"])]]]]

       [:div (map render-admin existing-admins)]])))

(defn footer-clearance
  []
  [:div#footer-clearance])

(defn footer
  []
  [:div#footer
   [:a {:href "https://ebbs.social"} "EBBS social"]
   ", a social media platform running on "
   [:a {:href "https://www.ethereum.org"} "Ethereum"]
   ", is "
   [:a {:href "https://github.com/cfelde/ebbs-social"} "open source software"]
   ". Copyright 2019, all rights reserved.
    User posted content is copyrighted the author of that content. You are viewing data stored on the "
   [:a {:href "https://en.wikipedia.org/wiki/Blockchain"} "blockchain"]
   "."])

(defn ui
  []
  [:div#ui
   [top-header]
   [top-desc]
   (if (and @(rf/subscribe [:db :ebbs-handler])
            @(rf/subscribe [:db :posts 0]))
     [:div
      [edit-forum]
      [edit-meta]
      [edit-admins]])
   [footer-clearance]
   [footer]])
