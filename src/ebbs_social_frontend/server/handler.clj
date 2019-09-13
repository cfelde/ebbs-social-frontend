(ns ebbs-social-frontend.server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.util.response :as response]))

(defn frontpage-file
  []
  (-> (response/resource-response "frontpage.html" {:root "public"})
      (response/content-type "text/html")))

(defn posts-file
  []
  (-> (response/resource-response "posts.html" {:root "public"})
      (response/content-type "text/html")))

(defn admin-file
  []
  (-> (response/resource-response "admin.html" {:root "public"})
      (response/content-type "text/html")))

(defroutes app-routes
  (GET "/" [] (frontpage-file))

  ;(GET "/eth/address" [] (posts-file))
  (GET "/eth/*" [] (posts-file))

  ;(GET "/new-post@address" [] (posts-file))
  (GET "/new-post@*" [] (posts-file))

  ;(GET "/post/123@address" [] (posts-file))
  (GET "/post/*" [] (posts-file))

  ;(GET "/edit-post/123@address" [] (posts-file))
  (GET "/edit-post/*" [] (posts-file))

  ;(GET "/reply-to-post/123@address" [] (posts-file))
  (GET "/reply-to-post/*" [] (posts-file))

  ;(GET "/posts/user-address@address" [] (posts-file))
  (GET "/posts/*" [] (posts-file))

  ;(GET "/replies/user-address@address" [] (posts-file))
  (GET "/replies/*" [] (posts-file))

  ;(GET "/admin@address" [] (posts-file))
  (GET "/admin@*" [] (admin-file))
  (GET "/mod@*" [] (posts-file))

  (route/resources "/" {:root "public"})
  (route/not-found "Not Found"))

(def dev-app (wrap-reload (wrap-defaults #'app-routes site-defaults)))
