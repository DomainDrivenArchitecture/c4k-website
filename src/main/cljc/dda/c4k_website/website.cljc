(ns dda.c4k-website.website
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

(defn fqdn-list?
  [input]
  (every? true? (map pred/fqdn-string? input)))

(s/def ::unique-name pred/fqdn-string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::authtoken pred/bash-env-string?)
(s/def ::fqdns fqdn-list?)
(s/def ::gitea-host pred/fqdn-string?)
(s/def ::gitea-repo string?)
(s/def ::branchname string?)
(s/def ::username string?)

(def websitedata? (s/keys :req-un [::unique-name ::fqdns ::gitea-host ::gitea-repo ::branchname]
                          :opt-un [::issuer]))

(def websiteauth? (s/keys :req-un [::unique-name ::username ::authtoken]))

(s/def ::auth (s/coll-of websiteauth?))

(s/def ::websites (s/coll-of websitedata?))

(def auth? (s/keys  :req-un [::auth]))

(def config? (s/keys :req-un [::websites]
                     :opt-un [::issuer]))

(def volume-size 3)

(defn unique-name-from-fqdn
  [fqdn]
  (str/replace fqdn #"\." "-"))

(defn generate-service-name
  [unique-name]
  (str (unique-name-from-fqdn unique-name) "-service"))

(defn generate-cert-name
  [unique-name]
  (str (unique-name-from-fqdn unique-name) "-cert"))

(defn generate-http-ingress-name
  [unique-name]
  (str (unique-name-from-fqdn unique-name) "-http-ingress"))

(defn generate-https-ingress-name
  [unique-name]
  (str (unique-name-from-fqdn unique-name) "-https-ingress"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/main.zip
(defn make-gitrepourl 
  [host repo user branch]
  (str "https://" host "/api/v1/repos/" user "/" repo "/archive/" branch ".zip"))

; ToDo: Move to common?
(defn replace-all-matching-subvalues-in-string-start 
  [col ;ToDo richtig spec-en
   value-to-partly-match
   value-to-inplace]
  (clojure.walk/postwalk #(if (and (= (type value-to-partly-match) (type %))
                                   (re-matches (re-pattern (str value-to-partly-match ".*")) %))
                            (str/replace % value-to-partly-match value-to-inplace) %)
                         col))

; generate a list of host-rules from a list of fqdns
(defn make-host-rules-from-fqdns
  [rule fqdns]
  ;function that creates a rule from host names
  (mapv #(assoc-in rule [:host] %) fqdns))

#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (case resource-name
       "website/certificate.yaml" (rc/inline "website/certificate.yaml")       
       "website/http-ingress.yaml" (rc/inline "website/http-ingress.yaml")
       "website/https-ingress.yaml" (rc/inline "website/https-ingress.yaml")       
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

; generate a list of host-rules from a list of fqdns
(defn make-host-rules-from-fqdns
  [rule fqdns]
  ;function that creates a rule from host names
  (mapv #(assoc-in rule [:host] %) fqdns))

;create working ingress
; todo: move to common/ingress
(defn generate-common-http-ingress
  [config]
  (let [{:keys [fqdn service-name]} config]
    (->
     (yaml/load-as-edn "website/http-ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "SERVICENAME" service-name)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-website-http-ingress pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config
        spec-rules [:spec :rules]]
    (->
     (generate-common-http-ingress
      {:fqdn (first fqdns) :service-name (generate-service-name unique-name)})
     (cm/replace-all-matching-values-by-new-value "c4k-common-http-ingress" (generate-http-ingress-name unique-name))
     (#(assoc-in %
                 spec-rules
                 (make-host-rules-from-fqdns
                  (-> % :spec :rules first) ;get first ingress rule
                  fqdns))))))

;create working ingress
(defn generate-common-https-ingress 
  [config]
  (let [{:keys [fqdn service-name]} config]
    (->
     (yaml/load-as-edn "website/https-ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "SERVICENAME" service-name)     
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-website-https-ingress pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config
        spec-rules [:spec :rules]
        spec-tls-hosts [:spec :tls 0 :hosts]]
    (->
     (generate-common-https-ingress
      {:fqdn (first fqdns) :service-name (generate-service-name unique-name)})
     (cm/replace-all-matching-values-by-new-value "c4k-common-https-ingress" (generate-https-ingress-name unique-name))
     (cm/replace-all-matching-values-by-new-value "c4k-common-cert" (generate-cert-name unique-name))
     (#(assoc-in % spec-tls-hosts fqdns))
     (#(assoc-in % spec-rules (make-host-rules-from-fqdns (-> % :spec :rules first) fqdns))))))

(defn generate-common-certificate
  [config]
  (let [{:keys [fqdn issuer]
         :or {issuer "staging"}} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "website/certificate.yaml")
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer)     
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-website-certificate pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name issuer fqdns]} config
        spec-dnsNames [:spec :dnsNames]]
    (->
     (generate-common-certificate 
      {:issuer issuer, :fqdn (first fqdns)})
     (cm/replace-all-matching-values-by-new-value "c4k-common-cert" (generate-cert-name unique-name))
     (assoc-in spec-dnsNames fqdns))))

(defn-spec generate-nginx-configmap pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config]
    (->
     (yaml/load-as-edn "website/nginx-configmap.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name))
     (#(assoc-in %
                 [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf) #"FQDN" (str (str/join " " fqdns) ";")))))))

(defn-spec generate-nginx-deployment pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name)))))

(defn-spec generate-nginx-service pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name)))))

(defn-spec generate-website-content-volume pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name))
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str (str volume-size) "Gi")))))

(defn-spec generate-website-build-cron pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-cron.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name)))))

(defn-spec generate-website-build-deployment pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name)))))

(defn-spec generate-website-build-secret pred/map-or-seq?
  [auth websiteauth?]
  (let [{:keys [unique-name
                authtoken
                gitea-host
                gitea-repo
                username
                branchname]} auth]
    (->
     (yaml/load-as-edn "website/website-build-secret.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn unique-name))
     (cm/replace-all-matching-values-by-new-value "TOKEN" (b64/encode authtoken))
     (cm/replace-all-matching-values-by-new-value "URL" (b64/encode
                                                         (make-gitrepourl
                                                          gitea-host
                                                          gitea-repo
                                                          username
                                                          branchname))))))