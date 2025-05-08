(ns dda.c4k-website.core
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.monitoring :as mon]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.ingress :as ing]
   [dda.c4k-website.website :as web]))

(def websiteconfig? (s/keys :req-un [::web/unique-name
                                     ::web/fqdns
                                     ::web/forgejo-host
                                     ::web/repo-owner
                                     ::web/repo-name
                                     ::web/branchname]
                            :opt-un [::ing/issuer
                                     ::web/volume-size
                                     ::web/build-cpu-request
                                     ::web/build-cpu-limit
                                     ::web/build-memory-request
                                     ::web/build-memory-limit
                                     ::web/redirects]))
(def websiteauth? web/websiteauth?)

(s/def ::websiteconfigs (s/coll-of websiteconfig?))
(s/def ::websiteauths (s/coll-of websiteauth?))

(def config? (s/keys :req-un [::websiteconfigs]
                     :opt-un [::ing/issuer
                              ::web/volume-size
                              ::mon/mon-cfg
                              ::ing/average-rate
                              ::ing/burst-rate]))

(def auth? (s/keys :req-un [::websiteauths]
                   :opt-un [::mon/mon-auth]))

(def config-defaults {:issuer "staging"})

(def website-config-defaults (merge ing/default-config
                                    {:build-cpu-request "500m"
                                     :build-cpu-limit "1700m"
                                     :build-memory-request "256Mi"
                                     :build-memory-limit "512Mi"
                                     :volume-size "3"
                                     :redirects []
                                     :average-rate 20
                                     :burst-rate 40}))

(defn-spec mapize-config map?
  [config config?]
  (let [{:keys [issuer websiteconfigs]} (merge config-defaults config)]
    (apply merge
           (map
            (fn [websiteconfig]
              {(:unique-name websiteconfig)
               (merge website-config-defaults
                      websiteconfig
                      {:issuer issuer})})
            websiteconfigs))))

(defn-spec mapize-auth map?
  [auth auth?]
  (let [{:keys [websiteauths]} auth]
    (apply merge
           (map
            (fn [websiteauth]
              {(:unique-name websiteauth)
               websiteauth})
            websiteauths))))

(defn-spec config-objects seq?
  [config config?]
  (let [resolved-config (mapize-config config)]
    (map yaml/to-string
         (filter
          #(not (nil? %))
          (cm/concat-vec
           (flatten
            (map
             (fn [w] (web/config-objects (val w)))
             resolved-config)))))))

(defn-spec auth-objects seq?
  [config config?
   auth auth?]
  (let [resolved-config (mapize-config config)
        resolved-auth (mapize-auth auth)]
    (map yaml/to-string
         (filter
          #(not (nil? %))
          (cm/concat-vec
           (flatten
            (map
             (fn [w] (web/auth-objects
                      (val w)
                      (get resolved-auth (key w))))
             resolved-config)))))))
