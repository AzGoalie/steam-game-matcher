(ns service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :refer [negotiate-content]]
            [cheshire.core :as json]
            [core :refer [match-multiplayer-apps]]))

(defn response [status body & {:as headers}]
  {:status status
   :body body
   :headers headers})

(def ok (partial response 200))
(def not-found (response 404 "Not Found"))

(def supported-types #{"application/edn" "application/json" "text/plain"})
(def content-type-interceptor (negotiate-content supported-types))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "application/edn" (pr-str body)
    "application/json" (json/encode body)
    "text/plain" (:apps body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body
  {:name ::coerce-body
   :leave
   (fn [context]
     (if (get-in context [:response :headers "Content-Type"])
       context
       (update-in context [:response] coerce-to (accepted-type context))))})

(defn match-apps
  [request]
  (let [ids (get-in request [:query-params :ids])]
    (ok {:apps (match-multiplayer-apps ids)})))

(def routes
  #{["/api/match-apps" :get [coerce-body content-type-interceptor match-apps] :route-name :match-apps]})

(def service
  {:env :prod
   ::http/routes routes
   ::http/type :jetty
   ::http/port 8080})
