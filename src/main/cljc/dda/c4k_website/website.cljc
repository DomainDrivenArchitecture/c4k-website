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
   [dda.c4k-common.ingress :as ing]
   [clojure.string :as str]))

(defn fqdn-list?
  [input]
  (every? true? (map pred/fqdn-string? input)))

(s/def ::unique-name string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::authtoken pred/bash-env-string?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))
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

(defn replace-dots-by-minus
  [fqdn]
  (str/replace fqdn #"\." "-"))

(defn generate-service-name
  [unique-name]
  (str (replace-dots-by-minus unique-name) "-service"))

(defn generate-cert-name
  [unique-name]
  (str (replace-dots-by-minus unique-name) "-cert"))

(defn generate-http-ingress-name
  [unique-name]
  (str (replace-dots-by-minus unique-name) "-http-ingress"))

(defn generate-https-ingress-name
  [unique-name]
  (str (replace-dots-by-minus unique-name) "-https-ingress"))

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

#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (case resource-name       
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

(defn-spec generate-website-http-ingress pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config]
    (ing/generate-http-ingress {:fqdns fqdns 
                                :ingress-name (generate-http-ingress-name unique-name)
                                :service-name (generate-service-name unique-name)
                                :service-port 80})))

(defn-spec generate-website-https-ingress pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config]
    (ing/generate-https-ingress {:fqdns fqdns
                                 :cert-name (generate-cert-name unique-name)
                                 :ingress-name (generate-https-ingress-name unique-name)
                                 :service-name (generate-service-name unique-name)
                                 :service-port 80})))

(defn-spec generate-website-certificate pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name issuer fqdns]
         :or {issuer "staging"}} config]
    (ing/generate-certificate {:fqdns fqdns
                               :cert-name (generate-cert-name unique-name)
                               :issuer issuer})))

(defn-spec generate-nginx-configmap pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name fqdns]} config]
    (->
     (yaml/load-as-edn "website/nginx-configmap.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (#(assoc-in %
                 [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf) #"FQDN" (str (str/join " " fqdns) ";")))))))

(defn-spec generate-nginx-deployment pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-nginx-service pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-website-content-volume pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str (str volume-size) "Gi")))))

(defn-spec generate-website-build-cron pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-cron.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-website-build-deployment pred/map-or-seq?
  [config websitedata?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-deployment.yaml")
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

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
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (cm/replace-all-matching-values-by-new-value "TOKEN" (b64/encode authtoken))
     (cm/replace-all-matching-values-by-new-value "URL" (b64/encode
                                                         (make-gitrepourl
                                                          gitea-host
                                                          gitea-repo
                                                          username
                                                          branchname))))))