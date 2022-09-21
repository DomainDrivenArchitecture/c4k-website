(ns dda.c4k-website.website
  (:require
   [clojure.spec.alpha :as s]
   [clojure.math.numeric-tower :as m]
   [clojure.string :as st]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:clj [clojure.edn :as edn]
      :cljs [cljs.reader :as edn])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]   
   [dda.c4k-common.predicate :as pred]))

(defn domain-list?
  [input]
  (or
   (st/blank? input)
   (pred/string-of-separated-by? pred/fqdn-string? #"," input)))

(s/def ::fqdn pred/fqdn-string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::volume-total-storage-size (partial pred/int-gt-n? 5))

(def config-defaults {:issuer "staging"})

(def config? (s/keys :req-un [::fqdn]
                     :opt-un [::issuer]))

(def vol? (s/keys :req-un [::volume-total-storage-size
                           ::number-of-websites]))

(defn data-storage-by-volume-size
  [total number-of-websites-on-node]
  (m/floor (/ total number-of-websites-on-node))) ; ToDo: This might be a terrible idea

#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (case resource-name
       "website/certificate.yaml" (rc/inline "website/certificate.yaml")
       "website/ingress.yaml" (rc/inline "website/ingress.yaml")
       "website/nginx-configmap.yaml" (rc/inline "website/nginx-configmap.yaml")
       "website/nginx-deployment.yaml" (rc/inline "website/nginx-deployment.yaml")
       "website/nginx-service.yaml" (rc/inline "website/nginx-service.yaml")              
       "website/website-content-volume.yaml" (rc/inline "website/website-content-volume.yaml")
       (throw (js/Error. "Undefined Resource!")))))

#?(:cljs
   (defmethod yaml/load-as-edn :website [resource-name]
     (yaml/from-string (yaml/load-resource resource-name))))

(defn-spec generate-certificate pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn issuer]
         :or {issuer "staging"}} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "website/certificate.yaml")
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-ingress pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn issuer]} config]
    (->
     (yaml/load-as-edn "website/ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-nginx-configmap pred/map-or-seq?  
  [config config?]
  (let [{:keys [fqdn]} config
        configmap (yaml/load-as-edn "website/nginx-configmap.yaml")]         
    (-> 
     configmap
     (assoc-in [:data :website.conf] (st/replace (-> configmap :data :website.conf) #"FQDN" fqdn))
     )
  ))     

(defn-spec generate-nginx-deployment pred/map-or-seq?
  []
  (yaml/load-as-edn "website/nginx-deployment.yaml"))

(defn-spec generate-nginx-service pred/map-or-seq?
  []
  (yaml/load-as-edn "website/nginx-service.yaml"))

(defn-spec generate-website-content-volume pred/map-or-seq?
  [config vol?]
  (let [{:keys [volume-total-storage-size number-of-websites]} config
        data-storage-size (data-storage-by-volume-size volume-total-storage-size number-of-websites)]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str (str data-storage-size) "Gi")))))
