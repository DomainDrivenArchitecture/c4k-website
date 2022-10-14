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
(s/def ::fqdns (s/coll-of pred/fqdn-string?))

(def ingress? (s/keys :req-un [::fqdns ::service-name ::port]
                      :opt-un [::issuer]))

; generate a list of host-rules from a list of fqdns
(defn make-host-rules-from-fqdns
  [rule fqdns]
  ;function that creates a rule from host names
  (mapv #(assoc-in rule [:host] %) fqdns))

(defn generate-http-ingress
  [config]
  (let [{:keys [fqdn service-name]} config]
    (->
     (yaml/load-as-edn "ingress/http-ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "SERVICENAME" service-name)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))