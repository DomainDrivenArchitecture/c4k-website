(ns dda.c4k-website.website
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [clojure.math.numeric-tower :as m]
      :cljs [cljs.math :as m])
   [clojure.string :as st]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:clj [clojure.edn :as edn]
      :cljs [cljs.reader :as edn])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.base64 :as b64]
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

(def auth? (s/keys  :req-un [::authtoken ::gitrepourl]))

(def vol? (s/keys :req-un [::volume-total-storage-size
                           ::number-of-websites]))

(defn volume-size-by-total-available-space
  [total number-of-websites-on-node]
  (m/floor (/ total number-of-websites-on-node))) ; ToDo: This might be a terrible idea

(defn unique-name-from-fqdn
  [fqdn]
  (st/replace fqdn #"\." "-"))

; ToDo: Move to common?
(defn-spec replace-all-matching-subvalues-in-string-start pred/map-or-seq?
  [col string?
   value-to-partly-match string?
   value-to-inplace string?]
  (clojure.walk/postwalk #(if (and (= (type value-to-partly-match) (type %))
                                   (re-matches (re-pattern (str value-to-partly-match ".*")) %))
                            (st/replace % value-to-partly-match value-to-inplace) %)
                         col))

#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (case resource-name
       "website/certificate.yaml" (rc/inline "website/certificate.yaml")
       "website/ingress.yaml" (rc/inline "website/ingress.yaml")
       "website/nginx-configmap.yaml" (rc/inline "website/nginx-configmap.yaml")
       "website/nginx-deployment.yaml" (rc/inline "website/nginx-deployment.yaml")
       "website/nginx-service.yaml" (rc/inline "website/nginx-service.yaml")              
       "website/website-content-volume.yaml" (rc/inline "website/website-content-volume.yaml")
       "website/website-build-cron.yaml" (rc/inline "website/website-build-cron.yaml")
       "website/website-build-deployment.yaml" (rc/inline "website/website-build-deployment.yaml")
       "website/website-build-secret.yaml" (rc/inline "website/website-build-secret.yaml")
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
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-ingress pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/ingress.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-nginx-configmap pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config
        configmap (yaml/load-as-edn "website/nginx-configmap.yaml")]
    (->
     configmap
     (assoc-in [:data :website.conf] (st/replace (-> configmap :data :website.conf) #"FQDN" (str fqdn ";")))
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn)))))     

(defn-spec generate-nginx-deployment pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn)))))

(defn-spec generate-nginx-service pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn)))))

(defn-spec generate-website-content-volume pred/map-or-seq?
  [config vol?]
  (let [{:keys [volume-total-storage-size number-of-websites fqdn]} config
        data-storage-size (volume-size-by-total-available-space volume-total-storage-size number-of-websites)]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str (str data-storage-size) "Gi")))))

(defn-spec generate-website-build-cron pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/website-build-cron.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-website-build-deployment pred/map-or-seq?
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/website-build-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-website-build-secret pred/map-or-seq?
  [auth auth?]
  (let [{:keys [fqdn 
                authtoken 
                gitrepourl]} auth]
    (->
     (yaml/load-as-edn "website/website-build-secret.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "TOKEN" (b64/encode authtoken))
     (cm/replace-all-matching-values-by-new-value "URL" (b64/encode gitrepourl)))))