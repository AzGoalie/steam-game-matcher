(ns core
  (:require [steam-api :as api]
            [db]
            [clojure.set :refer [intersection]]))

(def multiplayer-id 1)
(def coop-id 9)

(defn nil->false
  [b]
  (if (nil? b)
    false
    b))

(defn get-app-categories
  [{:keys [appid]}]
  (-> appid
      api/get-app-info
      :categories))

(defn multiplayer?
  [app]
  (->> app
       get-app-categories
       (map :id)
       (some #(or (= multiplayer-id %) (= coop-id %)))
       nil->false))

(defn update-database
  []
  (->> (db/get-all-unknown-multiplayer-apps)
       (map #(assoc % :multiplayer (multiplayer? %)))
       (map db/update-app)))

(defn owned-multiplayer-apps
  [steamid]
  (->> (api/get-owned-apps steamid)
       (db/get-apps)
       (remove #(or (false? (:multiplayer %)) (nil? (:multiplayer %))))))

(defn -main [& args]
  (if (< (count args) 2)
    (println "Usage: clojure -m core steamid-1 steamid-2 ...")
    (->> args
         (map owned-multiplayer-apps)
         (map set)
         (apply intersection)
         (map :name)
         (run! println))))