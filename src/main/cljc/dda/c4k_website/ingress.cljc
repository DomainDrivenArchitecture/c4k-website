(ns dda.c4k-website.ingress
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:clj [clojure.edn :as edn]
      :cljs [cljs.reader :as edn])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as pred]
   [clojure.string :as str]))


(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::service-name string?)
(s/def ::service-port pos-int?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))

(def ingress? (s/keys :req-un [::fqdns ::service-name ::service-port]
                      :opt-un [::issuer]))

(defn-spec generate-rule  pred/map-or-seq?
  [service-name ::service-name
   service-port ::service-port
   fqdn pred/fqdn-string?]
    (->
     (yaml/load-as-edn "ingress/rule.yaml")
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
     (cm/replace-all-matching-values-by-new-value "SERVICE_PORT" service-port)
     (cm/replace-all-matching-values-by-new-value "SERVICE_NAME" service-name)))

(defn-spec generate-http-ingress pred/map-or-seq?
  [config ingress?]
  (let [{:keys [service-name service-port fqdns]} config]
    (->
     (yaml/load-as-edn "ingress/http-ingress.yaml")
     (assoc-in [:metadata :name] (str service-name "-http-ingress"))
     (assoc-in [:spec :rules] (mapv (partial generate-rule service-name service-port) fqdns)))))