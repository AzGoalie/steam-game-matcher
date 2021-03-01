(ns db
  (:require [clojure.java.jdbc :as jdbc]
            [steam-api :as api]
            [clojure.string :refer [join]]))

(def db-spec
  {:dbtype "h2"
   :dbname "./apps"})

(def apps-table-ddl
  (jdbc/create-table-ddl :apps
                         [[:appid :int :primary :key]
                          [:name "varchar(256)"]
                          [:multiplayer :boolean]]
                         [[:conditional? true]]))

(defn initialize-database
  []
  ;; Create DB
  (jdbc/db-do-commands db-spec [apps-table-ddl])
  ;; Add all appids
  (jdbc/insert-multi! db-spec :apps
                      [:appid :name]
                      (map vals api/get-all-apps)))

(defn get-all-unknown-multiplayer-apps
  []
  (jdbc/query db-spec
              ["SELECT appid,name FROM apps WHERE multiplayer IS NULL"]))

(defn insert-app
  [{:keys [appid name multiplayer]}]
  (jdbc/insert! db-spec :apps {:appid appid
                               :name name
                               :multiplayer multiplayer}))

(defn update-app
  [{:keys [appid name multiplayer]}]
  (jdbc/update! db-spec :apps {:name name
                               :multiplayer multiplayer}
                ["appid = ?" appid]))

(defn get-app
  [appid]
  (jdbc/query db-spec ["SELECT * FROM apps WHERE appid = ?" appid]
              {:result-set-fn first}))

(defn get-apps
  [appids]
  (jdbc/query db-spec [(str "SELECT * FROM apps WHERE appid in (" (join ", " appids) ")")]))

(defn delete-app
  [appid]
  (jdbc/delete! db-spec :apps ["appid = ?" appid]))
