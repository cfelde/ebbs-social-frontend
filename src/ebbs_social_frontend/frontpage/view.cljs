(ns ebbs-social-frontend.frontpage.view
  (:require [re-frame.core :as rf]
            [goog.string.format]
            [ebbs-social-frontend.frontpage.event-dispatch :as d]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn top-header
  []
  [:h1#top-header.container-fluid
   [:a {:href "/"} "EBBS social"]])

(defn create-forum
  []
  (let [forum @(rf/subscribe [:db :new-forum])
        submitting @(rf/subscribe [:db :submitting-new-forum])
        data-length @(rf/subscribe [:compressed-forum-data-length])
        remaining-length (- u/max-post-data-length data-length)]
    (letfn [(render-tag
              [[tag value]]
              (let [tag-id (gensym "tag-")]
                [:div
                 {:key (gensym)
                  :class "form-check form-check-inline"}
                 [:input {:class     "form-check-input"
                          :type      "checkbox"
                          :id        tag-id
                          :value     tag
                          :checked   value
                          :on-change #(d/dispatch-edit-forum-tag tag)}]
                 [:label {:class "form-check-label"
                          :for tag-id}
                  tag]]))]

      [:div#new-forum.container
       [:div.row
        [:h1#new-forum-header "Create a new forum"]]

       [:div.row
        [:form
         [:div#new-forum-title-form-group.form-group.row
          [:label
           {:for "edit-forum-title"
            :class "col-sm-3 col-form-label"}
           "Title (optional)"]
          [:div.col-sm-9
           [:input
            {:type "text"
             :class "form-control"
             :id "edit-forum-title"
             :value (:title forum)
             :on-change #(d/dispatch-edit-forum :title (-> % .-target .-value))}]]]
         [:div#new-forum-body-form-group.form-group.row
          [:label
           {:for "edit-forum-desc"
            :class "col-sm-3 col-form-label"}
           "Description (optional)"]
          [:div.col-sm-9
           [:textarea
            {:type "textarea"
             :class "form-control"
             :id "edit-forum-desc"
             :value (:body forum)
             :on-change #(d/dispatch-edit-forum :body (-> % .-target .-value))}]]]
         [:div#new-forum-link-form-group.form-group.row
          [:label
           {:for "edit-forum-link"
            :class "col-sm-3 col-form-label"}
           "External forum link URL (optional)"]
          [:div.col-sm-9
           [:input
            {:type "text"
             :class "form-control"
             :id "edit-forum-link"
             :value (:post-link forum)
             :on-change #(d/dispatch-edit-forum :post-link (-> % .-target .-value))}]]]
         [:div#new-forum-image-form-group.form-group.row
          [:label
           {:for "edit-forum-image"
            :class "col-sm-3 col-form-label"}
           "Forum image icon URL (optional)"]
          [:div.col-sm-9
           [:input
            {:type "text"
             :class "form-control"
             :id "edit-forum-image"
             :value (:image-link forum)
             :on-change #(d/dispatch-edit-forum :image-link (-> % .-target .-value))}]]]
         [:div#new-forum-tags
          (map render-tag (sort (:meta forum)))]

         [:div#new-forum-submit-form-group.form-group.row
          [:div.col-sm-3
           (if submitting
             [:button
              {:type "submit"
               :class "btn btn-primary"
               :disabled true}
              [:span {:class "spinner-border spinner-border-sm"
                      :role "status"
                      :aria-hidden "true"}]
              [:span.sr-only "Submitting.."]]
             (if (>= remaining-length 0)
               [:button
                {:type "submit"
                 :class "btn btn-primary"
                 :on-click d/dispatch-submit-new-forum}
                "Create"]
               [:button
                {:type "submit"
                 :class "btn btn-primary"
                 :disabled true}
                "Create"]))]
          (if (>= remaining-length 0)
            [:div.remaining-storage-space.text-green.col-sm-9 "Remaining storage space: " remaining-length]
            [:div.remaining-storage-space.text-red.col-sm-9 "Remaining storage space: " remaining-length])]]]])))

(defn create-badge
  []
  (when @(rf/subscribe [:db :provider])
    [:div#create-badge
     {:on-click d/dispatch-create-new-forum}
     "Create new forum"]))

(defn intro-text
  []
  [:div#intro-text
   [:div "EBBS social is a social media platform that uses the Ethereum blockchain to store and distribute content."]
   [:div "Censorship resistant, privacy friendly, and easy to use. " [:a {:href "https://github.com/cfelde/ebbs-social" :target "_blank"} "It's also open source software."]]
   [:div "Browse existing forums below or create a new above. The only thing you need to get started is an Ethereum enabled web browser."]
   [:div "This is early stage beta software, so expect bugs and breaking changes.."]])

(defn no-provider
  []
  (when @(rf/subscribe [:db :no-provider])
    [:div#no-provider
     [:div "It doesn't look like your web browser is Ethereum enabled! :-/"]
     [:div "But don't worry! It's easy to get started, just install one of the below plugins :-)"]
     [:div#plugins
      [:div#dapper [:a {:href "https://www.meetdapper.com/" :target "_blank"} [:img {:src "/_img/dapper-logo.png"}]]]
      [:div#metamask [:a {:href "https://metamask.io/" :target "_blank"} [:img {:src "/_img/metamask-logo.png"}]]]]
     [:div "If you're new to blockchain and Ethereum, Dapper is probably your easier option. If you already have Ether, then you should try MetaMask."]]))

(defn main-forums
  []
  [:div [:a {:href "/eth/0x04481981d3b3096067e370eCaA9a3D95885e74FA"} "Mainnet EBBS forum"]])

(defn ropsten-forums
  []
  [:div [:a {:href "/eth/0xFFFb5D13dE1BFa814C6A4bf93a50983ceA54a764"} "Ropsten test forum"]])

(defn existing-forums
  []
  (if-let [network @(rf/subscribe [:db :network])]
    [:div#network
     [:div "You are connected to " network ".."]
     (case network
       "main" [main-forums]
       "ropsten" [ropsten-forums]
       [:div "No forums listed for this network"])]))

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

  (when @(rf/subscribe [:db :create-new])
    [:div
     [create-forum]])

  (when (not @(rf/subscribe [:db :create-new]))
    [:div
     [create-badge]
     [intro-text]
     [:hr]
     [no-provider]
     [existing-forums]])

  [footer-clearance]
  [footer]])

