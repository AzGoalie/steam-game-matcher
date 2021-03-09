(ns server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [service]))

;; This is an adapted service map, that can be started and stopped
;; From the REPL you can call server/start and server/stop on this service
(defonce runnable-service (http/create-server service/service))

(defn run-dev
  [& args]
  (println "Starting DEV server...")
  (-> service/service
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes #(route/expand-routes (deref #'service/routes))
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;; Content Security Policy (CSP) is mostly turned off in dev mode
              ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
      http/create-server
      http/start))

(defn -main
  []
  (println "Starting PROD server...")
  (http/start runnable-service))
