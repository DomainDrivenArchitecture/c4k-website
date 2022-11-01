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
   [dda.c4k-website.ingress-cert :as ing]
   [clojure.string :as str]))

(defn fqdn-list?
  [input]
  (every? true? (map pred/fqdn-string? input)))

(s/def ::unique-name string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::volume-size pred/integer-string?)
(s/def ::authtoken pred/bash-env-string?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))
(s/def ::gitea-host pred/fqdn-string?)
(s/def ::gitea-repo string?)
(s/def ::branchname string?)
(s/def ::username string?)

(def websitedata? (s/keys :req-un [::unique-name ::fqdns ::gitea-host ::gitea-repo ::branchname]
                          :opt-un [::issuer ::volume-size]))

(def websiteauth? (s/keys :req-un [::unique-name ::username ::authtoken]))

(def flattened-and-reduced-config? (s/and websitedata? websiteauth?))

(s/def ::auth (s/coll-of websiteauth?))

(s/def ::websites (s/coll-of websitedata?))

(def auth? (s/keys  :req-un [::auth]))

(def config? (s/keys :req-un [::websites]
                     :opt-un [::issuer ::volume-size]))

(defn-spec replace-dots-by-minus string?
  [fqdn pred/fqdn-string?]
  (str/replace fqdn #"\." "-"))

(defn-spec generate-app-name string?
  [unique-name pred/fqdn-string?]
  (str (replace-dots-by-minus unique-name) "-website"))

(defn-spec generate-service-name string?
  [unique-name pred/fqdn-string?]
  (str (replace-dots-by-minus unique-name) "-service"))

(defn-spec generate-cert-name string?
  [unique-name pred/fqdn-string?]
  (str (replace-dots-by-minus unique-name) "-cert"))

(defn-spec generate-http-ingress-name string?
  [unique-name pred/fqdn-string?]
  (str (replace-dots-by-minus unique-name) "-http-ingress"))

(defn-spec generate-https-ingress-name string?
  [unique-name pred/fqdn-string?]
  (str (replace-dots-by-minus unique-name) "-https-ingress"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/main.zip
(defn-spec make-gitrepourl string?
  [host pred/fqdn-string?
   repo string?
   user string?
   branch string?]
  (str "https://" host "/api/v1/repos/" user "/" repo "/archive/" branch ".zip"))

(defn-spec replace-all-matching-subvalues-in-string-start pred/map-or-seq?
  [col pred/map-or-seq?
   value-to-partly-match string?
   value-to-inplace string?]
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
       "website/website-build-cron.yaml" (rc/inline "website/website-build-cron.yaml")
       "website/website-build-deployment.yaml" (rc/inline "website/website-build-deployment.yaml")
       "website/website-build-secret.yaml" (rc/inline "website/website-build-secret.yaml")
       "website/website-content-volume.yaml" (rc/inline "website/website-content-volume.yaml")
       (throw (js/Error. "Undefined Resource!")))))

; TODO: Review jem 2022/10/26: move this fkt. to a more general place
#?(:cljs
   (defmethod yaml/load-as-edn :website [resource-name]
     (yaml/from-string (yaml/load-resource resource-name))))

(defn-spec generate-website-http-ingress pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name fqdns]} config]
    (ing/generate-http-ingress {:fqdns fqdns
                                :app-name (generate-app-name unique-name)
                                :ingress-name (generate-http-ingress-name unique-name)
                                :service-name (generate-service-name unique-name)
                                :service-port 80})))

(defn-spec generate-website-https-ingress pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name fqdns]} config]
    (ing/generate-https-ingress {:fqdns fqdns
                                 :cert-name (generate-cert-name unique-name)
                                 :app-name (generate-app-name unique-name)
                                 :ingress-name (generate-https-ingress-name unique-name)
                                 :service-name (generate-service-name unique-name)
                                 :service-port 80})))

(defn-spec generate-website-certificate pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name issuer fqdns]
         :or {issuer "staging"}} config]
    (ing/generate-certificate {:fqdns fqdns
                               :app-name (generate-app-name unique-name)
                               :cert-name (generate-cert-name unique-name)
                               :issuer issuer})))

(defn-spec generate-nginx-configmap pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name fqdns]} config]
    (->
     (yaml/load-as-edn "website/nginx-configmap.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (#(assoc-in %
                 [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf) #"FQDN" (str (str/join " " fqdns) ";")))))))

(defn-spec generate-nginx-deployment pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-nginx-service pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-website-content-volume pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name volume-size]
         :or {volume-size "3"}} config]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str volume-size "Gi")))))

(defn-spec generate-website-build-cron pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-cron.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-website-build-deployment pred/map-or-seq?
  [config flattened-and-reduced-config?]
  (let [{:keys [unique-name]} config]
    (->
     (yaml/load-as-edn "website/website-build-deployment.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name)))))

(defn-spec generate-website-build-secret pred/map-or-seq?
  [auth flattened-and-reduced-config?]
  (let [{:keys [unique-name
                authtoken
                gitea-host
                gitea-repo
                username
                branchname]} auth]
    (->
     (yaml/load-as-edn "website/website-build-secret.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] (generate-app-name unique-name))
     (replace-all-matching-subvalues-in-string-start "NAME" (replace-dots-by-minus unique-name))
     (cm/replace-all-matching-values-by-new-value "TOKEN" (b64/encode authtoken))
     (cm/replace-all-matching-values-by-new-value "URL" (b64/encode
                                                         (make-gitrepourl
                                                          gitea-host
                                                          gitea-repo
                                                          username
                                                          branchname))))))