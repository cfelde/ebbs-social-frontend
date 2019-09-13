(ns ebbs-social-frontend.posts.view
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [ebbs-social-frontend.posts.event-dispatch :as d]
            [ebbs-social-frontend.misc.utils :as u]))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn blockies-data
  [opts]
  (let [address (:seed opts)
        color (str/lower-case (str "#" (str/join (take 3 (drop 2 address)))))
        bgcolor (str/lower-case (str "#" (str/join (take 3 (drop 5 address)))))
        spotcolor (str/lower-case (str "#" (str/join (take 3 (drop 8 address)))))
        blockies-opts (merge {:seed address
                              :size 7
                              :scale 22
                              :color color
                              :bgcolor bgcolor
                              :spotcolor spotcolor}
                             opts)
        blockies-data (.toDataURL (js/window.blockies.create (clj->js blockies-opts)))]
    (str "url(" blockies-data ")")))

(defn parse-body
  [container body]
  (let [body-parts (str/split-lines body)
        body-parts (into [] (butlast (apply concat [container] (map #(list % [:br]) body-parts))))]
    body-parts))

(defn render-points
  [points vote new-vote]
  (let [extra-point (case [vote new-vote]
                      [:none :up] 1
                      [:none :down] -1
                      [:up :none] -1
                      [:up :down] -2
                      [:down :none] 1
                      [:down :up] 2
                      0)
        points (+ points extra-point)]
    (cond
      (> points 1000000) (str (gstring/format "%.1f" (/ points 1000000)) "M")
      (> points 1000) (str (gstring/format "%.1f" (/ points 1000)) "k")
      (< points -1000000) (str (gstring/format "%.1f" (/ points 1000000)) "M")
      (< points -1000) (str (gstring/format "%.1f" (/ points 1000)) "k")
      :else (str points))))

(defn render-post
  [id-prefix user-address is-admin is-mod forum-link meta-data post-zero-meta {:keys [post-id author timestamp points reply-count in-reply-to title body post-link image-link vote new-vote tags meta nsfw-override show-details]}]
  (let [nsfw (->> tags (filter second) (map first) (map str/lower-case) (filter #(= "nsfw" %)) empty? not)
        nsfw (or nsfw (->> meta (filter second) (map first) (map str/lower-case) (filter #(= "force nsfw" %)) empty? not))
        nsfw (or nsfw (->> post-zero-meta (filter second) (map first) (map str/lower-case) (filter #(= "force nsfw" %)) empty? not))
        nsfw (and nsfw (not nsfw-override))
        can-edit (and (>= post-id 0)
                      (or
                        (and is-admin (= 0 post-id))
                        (and (= user-address author) (> timestamp (- (.getTime (js/Date.)) (* 1000 60 5))))))
        author-points (-> meta-data :authors (get author) :karma)
        last-vote (or new-vote vote)
        is-moderated (->> meta (remove second) (map first) (map str/lower-case) (filter #(= "force moderation" %)) empty? not)]

    [:div {:key (str id-prefix post-id)
           :id (str id-prefix post-id)
           :class (if nsfw "post-container post-nsfw" "post-container")}
     [:div.post-item
      [:h2.post-title (if (>= post-id 0)
                        [:a {:href (str "/post/" post-id "@" forum-link)} title]
                        title)]
      (if (or body (not image-link))
        (let [body-parts (parse-body :div.post-text body)]
          [:div.post-body
           body-parts
           (if image-link
             [:div.post-image-right [:a {:href (or post-link image-link)
                                         :target "_blank"
                                         :on-click (partial d/dispatch-img-link-click post-id nsfw)} [:img {:src image-link}]]]
             [:div.post-image-none])])

        [:div.post-body
         [:div.post-image-full [:a {:href (or post-link image-link)
                                    :target "_blank"
                                    :on-click (partial d/dispatch-img-link-click post-id nsfw)} [:img {:src image-link}]]]])

      [:div.post-actions
       (if (> post-id 0)
         [:div.post-actions-left
          [:div.post-avatar {:style {:backgroundImage (blockies-data {:seed author})}
                             :on-click #(d/dispatch-show-post-details post-id)}]

          [:i {:class (str "post-upvote fas fa-thumbs-up " (when (= :up last-vote) "post-voted-up"))
               :on-click #(d/dispatch-vote post-id (if (= :up last-vote) :none :up))}]
          " "
          (render-points points vote new-vote)
          " "
          [:i {:class (str "post-downvote fas fa-thumbs-down " (when (= :down last-vote) "post-voted-down"))
               :on-click #(d/dispatch-vote post-id (if (= :down last-vote) :none :down))}]

          [:a {:href (str "/post/" post-id "@" forum-link)} (str reply-count (if (= 1 reply-count) " reply" " replies"))]

          " "

          [:a {:href (str "/reply-to-post/" post-id "@" forum-link)} [:i.post-reply-to-this-icon.fas.fa-reply] "Reply to this"]]

         [:div.post-actions-left
          [:div.post-avatar {:style {:backgroundImage (blockies-data {:seed author})}
                             :on-click #(d/dispatch-show-post-details post-id)}]])

       [:div.post-actions-right
        (when can-edit
          [:a {:href (str "/edit-post/" post-id "@" forum-link)} [:i.post-edit-post-icon.fas.fa-pen-square]])
        (when (> in-reply-to 0)
          [:a {:href (str "/post/" in-reply-to "@" forum-link)} [:i.post-parent-icon.fas.fa-arrow-up]])
        (when (or post-link image-link)
          [:a {:href (or post-link image-link) :target "_blank"} [:i.post-external-link.fas.fa-external-link-square-alt]])
        (when is-mod
          [:a.post-mod-status {:href "#"
                               :title "Click to change moderation status on post"
                               :on-click (partial d/dispatch-mod-post post-id)}
           "Mod status: " (if is-moderated "Moderated" "Allowed")])]]

      [:div.post-bottom {:class (when show-details "post-bottom-show")}
       [:div.post-bottom-author "Author: " author]
       [:div.post-bottom-karma "Author karma: " author-points]]]]))

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

(defn avatar-badge
  []
  (let [blockies-seed @(rf/subscribe [:db :user :address])
        is-admin @(rf/subscribe [:db :user :is-admin])
        is-mod @(rf/subscribe [:db :user :is-mod])
        karma @(rf/subscribe [:db :user :karma])
        user-address @(rf/subscribe [:db :user :address])
        forum-link @(rf/subscribe [:db :forum-link])]

    [:div#avatar-badge
      [:div#avatar-badge-img-div {:style {:backgroundImage (blockies-data {:seed blockies-seed})}}]
      [:div#avatar-badge-details
       (if (and is-admin forum-link) [:div [:a {:href (str "/admin@" forum-link)} [:i#avatar-badge-admin-check.fas.fa-check-square] "You're admin"]])
       (if (and is-mod forum-link) [:div [:a {:href (str "/mod@" forum-link)} [:i#avatar-badge-mod-check.fas.fa-check-square] "You're mod"]])
       (if karma [:div [:i#avatar-badge-karma.fas.fa-thumbs-up] (render-points karma nil nil) " karma"])
       (if forum-link [:div [:a {:href (str "/new-post@" forum-link)} [:i#avatar-badge-new-post.fas.fa-pen] "New post"]])
       (if (and user-address forum-link) [:div [:a {:href (str "/posts/" user-address "@" forum-link)} [:i#avatar-badge-view-posts.fas.fa-envelope-square] "View posts"]])
       (if (and user-address forum-link) [:div [:a {:href (str "/replies/" user-address "@" forum-link)} [:i#avatar-badge-view-replies.fas.fa-reply] "View replies"]])]]))

(defn edit-post
  []
  (if-let [edit-post-mode @(rf/subscribe [:db :edit-post-mode])]
    (let [title-mode (:title-mode edit-post-mode)
          selected-post-id @(rf/subscribe [:db :selected-post-id])
          post @(rf/subscribe [:db :posts selected-post-id])
          saving @(rf/subscribe [:db :edit-post-mode :saving])
          data-length @(rf/subscribe [:compressed-forum-data-length])
          remaining-length (- u/max-post-data-length data-length)]
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
                            :on-change #(d/dispatch-edit-post-tag tag)}]
                   [:label {:class "form-check-label"
                            :for tag-id}
                    tag]]))]

        [:div#edit-post.container
         [:div.row
          [:h1#edit-post-header (case title-mode
                                  :new "New post"
                                  :edit "Edit post"
                                  :reply "Reply"
                                  "")]]

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
             "Body"]
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
           [:div#edit-post-tags
            (map render-tag (sort (:tags post)))]

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
                 [:button {:type "submit" :class "btn btn-primary" :on-click d/dispatch-save-selected-post} "Save post"]
                 [:button {:type "submit" :class "btn btn-primary" :disabled true} "Save post"]))]
            (if (>= remaining-length 0)
              [:div.remaining-storage-space.text-green.col-sm-10 "Remaining message space: " remaining-length]
              [:div.remaining-storage-space.text-red.col-sm-10 "Remaining message space: " remaining-length])]]]]))))

(defn reply-to-post
  []
  (if-let [reply-to-post @(rf/subscribe [:reply-to-post])]
    (let [address @(rf/subscribe [:db :user :address])
          is-admin @(rf/subscribe [:db :user :is-admin])
          is-mod @(rf/subscribe [:db :user :is-mod])
          forum-link @(rf/subscribe [:db :forum-link])
          meta-data @(rf/subscribe [:db :meta-data])
          post-zero-meta @(rf/subscribe [:db :posts 0 :meta])]
      [:div#reply-to-post-container
       (render-post "reply-to-post-" address is-admin is-mod forum-link meta-data post-zero-meta reply-to-post)])))

(defn selected-post
  []
  (if-let [selected-post @(rf/subscribe [:selected-post])]
    (let [address @(rf/subscribe [:db :user :address])
          is-admin @(rf/subscribe [:db :user :is-admin])
          is-mod @(rf/subscribe [:db :user :is-mod])
          forum-link @(rf/subscribe [:db :forum-link])
          meta-data @(rf/subscribe [:db :meta-data])
          post-zero-meta @(rf/subscribe [:db :posts 0 :meta])]
      [:div#selected-post-container
       (render-post "selected-post-" address is-admin is-mod forum-link meta-data post-zero-meta selected-post)])))

(defn view-options
  []
  (if (seq @(rf/subscribe [:db :posts]))
    [:div#view-options.container
     [:div.row
      [:form.form-inline
       [:div.form-group
        [:label {:for "view-options-sort-by"} "Sort by "]
        [:select#view-options-sort-by.form-control.form-control-sm
         {:value (str/replace (name @(rf/subscribe [:db :sort-by])) "-" " ")
          :on-change #(d/dispatch-ordering :sort-by (-> % (.-target) (.-value)))}
         [:option "most upvoted"]
         [:option "most replies"]
         [:option "newest"]
         [:option "oldest"]
         [:option "most downvoted"]]]

       [:div.form-group
        [:label {:for "view-options-time-limit"} " from within "]
        [:select#view-options-time-limit.form-control.form-control-sm
         {:value (str/replace (name @(rf/subscribe [:db :from-within])) "-" " ")
          :on-change #(d/dispatch-ordering :from-within (-> % (.-target) (.-value)))}
         [:option "last 24 hours"]
         [:option "last 3 days"]
         [:option "last 7 days"]]]]]]))

(defn posts
  []
  (let [address @(rf/subscribe [:db :user :address])
        is-admin @(rf/subscribe [:db :user :is-admin])
        is-mod @(rf/subscribe [:db :user :is-mod])
        forum-link @(rf/subscribe [:db :forum-link])
        meta-data @(rf/subscribe [:db :meta-data])
        post-zero-meta @(rf/subscribe [:db :posts 0 :meta])
        posts @(rf/subscribe [:posts])]
    [:div (map (partial render-post "post-id-" address is-admin is-mod forum-link meta-data post-zero-meta) posts)]))

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

(defn no-provider
  []
  [:div#no-provider
   [:div "It doesn't look like your web browser is Ethereum enabled! :-/"]
   [:div "But don't worry! It's easy to get started, just install one of the below plugins :-)"]
   [:div#plugins
    [:div#dapper [:a {:href "https://www.meetdapper.com/" :target "_blank"} [:img {:src "/_img/dapper-logo.png"}]]]
    [:div#metamask [:a {:href "https://metamask.io/" :target "_blank"} [:img {:src "/_img/metamask-logo.png"}]]]]
   [:div "If you're new to blockchain and Ethereum, Dapper is probably your easier option. If you already have Ether, then you should try MetaMask."]])

(defn ui
  []
  [:div#ui
   (when @(rf/subscribe [:db :no-provider])
     [no-provider])

   (when @(rf/subscribe [:db :ebbs-handler])
     [:div
      [top-header]
      [top-desc]
      (if (not @(rf/subscribe [:db :is-mod-mode]))
        [avatar-badge])
      [reply-to-post]
      (if @(rf/subscribe [:db :edit-post-mode :enabled])
        [edit-post])
      (if (not= 0 @(rf/subscribe [:db :selected-post-id]))
        [selected-post])
      (if (or @(rf/subscribe [:db :load-posts-in-reply-to-selected])
              @(rf/subscribe [:db :load-posts-on-address])
              @(rf/subscribe [:db :load-replies-on-address]))
        [:div
         [view-options]
         [posts]])])

   [footer-clearance]
   [footer]])

