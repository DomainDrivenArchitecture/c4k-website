(ns dda.c4k-website.website
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as st]
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

(defn keyword-string? 
  [input]
  (str/starts-with? input "fqdn"))

(defn keyword-string-list?
  [input]
  (every? true? (map keyword-string? input)))

(s/def ::fqdn pred/fqdn-string?)
(s/def ::fqdn1 pred/fqdn-string?)
(s/def ::fqdn2 pred/fqdn-string?)
(s/def ::single keyword-string?)
(s/def ::multi keyword-string-list?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::authtoken pred/bash-env-string?)
(s/def ::gitrepourl pred/bash-env-string?)

(def config? (s/keys :req-un [::fqdn ::single ::multi ::fqdn1 ::fqdn2]
                     :opt-un [::issuer]))

(def auth? (s/keys  :req-un [::authtoken ::gitrepourl ::singlegitrepourl]))

(def volume-size 3)

(defn unique-name-from-fqdn
  [fqdn]
  (st/replace fqdn #"\." "-"))

(defn generate-service-name
  [uname]
  (str (unique-name-from-fqdn uname) "-service"))

(defn generate-cert-name
  [uname]
  (str (unique-name-from-fqdn uname) "-cert"))

; ToDo: Move to common?
(defn-spec replace-all-matching-subvalues-in-string-start pred/map-or-seq?
  [col string? ;ToDo richtig spec-en
   value-to-partly-match string?
   value-to-inplace string?]
  (clojure.walk/postwalk #(if (and (= (type value-to-partly-match) (type %))
                                   (re-matches (re-pattern (str value-to-partly-match ".*")) %))
                            (st/replace % value-to-partly-match value-to-inplace) %)
                         col))

#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (case resource-name
       "website/single-certificate.yaml" (rc/inline "website/single-certificate.yaml")
       "website/multi-certificate.yaml" (rc/inline "website/multi-certificate.yaml")
       "website/single-ingress.yaml" (rc/inline "website/single-ingress.yaml")
       "website/multi-ingress.yaml" (rc/inline "website/multi-ingress.yaml")
       "website/single-nginx-configmap.yaml" (rc/inline "website/single-nginx-configmap.yaml")
       "website/multi-nginx-configmap.yaml" (rc/inline "website/multi-nginx-configmap.yaml")
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

; ability extend input map (e.g. ingress or cert) with additional values (e.g. FQDNs)
; use for website-ingress generation
(defn add-to-col-within-map [inmap keywordlist value]
  (-> inmap
      (get-in keywordlist)
      (conj value)
      (#(assoc-in inmap keywordlist %))))

; generate a list of host-rules from a list of fqdns
(defn make-host-rules-from-fqdns
  [rule fqdns]
  ;function that creates a rule from host names
  (map #(assoc-in rule [:host] %) fqdns))

;create working ingress
(defn generate-common-http-ingress [config]
  (let [{:keys [fqdn service-name]} config]
    (->
     (yaml/load-as-edn "website/http-ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "SERVICENAME" service-name)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn generate-website-http-ingress [config]
  (let [{:keys [uname fqdns]} config
        fqdn (first fqdns)
        spec-rules [:spec :rules]
        service-name (generate-service-name uname)]
    (->
     (generate-common-http-ingress
      {:fqdn fqdn :service-name service-name})
     (assoc-in
      [:metadata :name]
      (str (unique-name-from-fqdn uname) "-http-ingress"))
     (#(assoc-in %
                 spec-rules
                 (make-host-rules-from-fqdns
                  (-> % :spec :rules first) ;get first ingress rule
                  fqdns))))))

;create working ingress
(defn generate-common-https-ingress [config]
  (let [{:keys [fqdn service-name cert-name]} config]
    (->
     (yaml/load-as-edn "website/https-ingress.yaml")
     (cm/replace-all-matching-values-by-new-value "SERVICENAME" service-name)
     (cm/replace-all-matching-values-by-new-value "CERTNAME" cert-name)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn generate-website-https-ingress [config]
  (let [{:keys [uname fqdns]} config
        fqdn (first fqdns)
        spec-rules [:spec :rules]
        spec-tls-hosts [:spec :tls 0 :hosts]
        service-name (generate-service-name uname)
        cert-name (generate-cert-name uname)]
    (->
     (generate-common-https-ingress
      {:fqdn fqdn :service-name service-name :cert-name cert-name})
     (assoc-in
      [:metadata :name]
      (str (unique-name-from-fqdn uname) "-https-ingress"))
     (#(assoc-in %
                 spec-tls-hosts
                 fqdns))
     (#(add-to-col-within-map %
                              spec-rules
                              (make-host-rules-from-fqdns
                               (-> % :spec :rules first) ;get first ingress rule
                               fqdns))))))

(defn generate-common-certificate
  [config]
  (let [{:keys [uname fqdns issuer]
         :or {issuer "staging"}} config
        fqdn (first fqdns)        
        letsencrypt-issuer (name issuer)
        cert-name (generate-cert-name uname)]
    (->
     (yaml/load-as-edn "website/certificate.yaml")
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer)
     (cm/replace-all-matching-values-by-new-value "CERTNAME" cert-name)
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn generate-website-certificate
  [config]
  (let [{:keys [fqdns]} config
        spec-dnsNames [:spec :dnsNames]]
    (->
     (generate-common-certificate config)
     (assoc-in spec-dnsNames fqdns))))

(defn-spec generate-single-certificate pred/map-or-seq?
  [config config?]
  (let [{:keys [issuer single]
         :or {issuer "staging"}} config
        fqdn ((keyword single) config)
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "website/single-certificate.yaml")
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer)
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-single-ingress pred/map-or-seq?
  [config config?]
  (let [{:keys [single]} config
        fqdn ((keyword single) config)]
    (->
     (yaml/load-as-edn "website/single-ingress.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn))))

(defn-spec generate-single-nginx-configmap pred/map-or-seq?
  [config config?]
  (let [{:keys [single]} config
        fqdn ((keyword single) config)
        configmap (yaml/load-as-edn "website/single-nginx-configmap.yaml")]
    (->
     configmap
     (assoc-in [:data :website.conf] (st/replace (-> configmap :data :website.conf) #"FQDN" (str fqdn ";")))
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn)))))

(defn-spec generate-multi-certificate pred/map-or-seq?
  [config config?]
  (let [{:keys [issuer multi]
         :or {issuer "staging"}} config
        fqdn ((keyword (first multi)) config)
        fqdn1 ((keyword (second multi)) config)
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "website/multi-certificate.yaml")
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer)
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
     (cm/replace-all-matching-values-by-new-value "FQDN1" fqdn1))))

(defn-spec generate-multi-ingress pred/map-or-seq?
  [config config?]
  (let [{:keys [multi]} config
        fqdn ((keyword (first multi)) config)
        fqdn1 ((keyword (second multi)) config)]
    (->
     (yaml/load-as-edn "website/multi-ingress.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
     (cm/replace-all-matching-values-by-new-value "FQDN1" fqdn1))))

(defn-spec generate-multi-nginx-configmap pred/map-or-seq?
  [config config?]
  (let [{:keys [multi]} config
        fqdn ((keyword (first multi)) config)
        fqdn1 ((keyword (second multi)) config)
        configmap (yaml/load-as-edn "website/multi-nginx-configmap.yaml")]
    (->
     configmap
     (assoc-in [:data :website.conf] (st/replace (-> configmap :data :website.conf) #"FQDN\ FQDN1" (str fqdn " " fqdn1 ";")))     
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
  [config config?]
  (let [{:keys [fqdn]} config]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (unique-name-from-fqdn fqdn))
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str (str volume-size) "Gi")))))

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