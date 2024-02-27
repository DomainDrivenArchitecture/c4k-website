(ns dda.c4k-website.website.website-internal
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as pred]
   [dda.c4k-common.ingress :as ing]))

(defn fqdn-list?
  [input]
  (every? true? (map pred/fqdn-string? input)))

(s/def ::unique-name string?)
(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::volume-size pred/integer-string?)
(s/def ::authtoken pred/bash-env-string?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))
(s/def ::forgejo-host pred/fqdn-string?)
(s/def ::forgejo-repo string?)
(s/def ::branchname string?)
(s/def ::username string?)
(s/def ::build-cpu-request string?)
(s/def ::build-memory-request string?)
(s/def ::build-cpu-limit string?)
(s/def ::build-memory-limit string?)

(def websiteconfig? (s/keys :req-un [::unique-name
                                     ::fqdns
                                     ::forgejo-host
                                     ::forgejo-repo
                                     ::branchname
                                     ::issuer
                                     ::volume-size
                                     ::build-cpu-request
                                     ::build-cpu-limit
                                     ::build-memory-request
                                     ::build-memory-limit]))

(def websiteauth? (s/keys :req-un [::unique-name ::username ::authtoken]))

(s/def ::websites (s/coll-of websiteconfig?))

(s/def ::auth (s/coll-of websiteauth?))

(def websites? (s/keys :req-un [::websites]))

(def auth? (s/keys :req-un [::auth]))

(defn-spec replace-dots-by-minus string?
  [fqdn pred/fqdn-string?]
  (str/replace fqdn #"\." "-"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/<branch>.zip
(defn-spec generate-gitrepourl string?
  [host pred/fqdn-string?
   repo string?
   user string?
   branch string?]
  (str "https://" host "/api/v1/repos/" user "/" repo "/archive/" branch ".zip"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/git/commits/HEAD
(defn-spec generate-gitcommiturl string?
  [host pred/fqdn-string?
   repo string?
   user string?]
  (str "https://" host "/api/v1/repos/" user "/" repo "/git/" "commits/" "HEAD"))

(defn-spec replace-all-matching-substrings-beginning-with pred/map-or-seq?
  [col pred/map-or-seq?
   value-to-partly-match string?
   value-to-inplace string?]
  (clojure.walk/postwalk #(if (and (= (type value-to-partly-match) (type %))
                                   (re-matches (re-pattern (str value-to-partly-match ".*")) %))
                            (str/replace % value-to-partly-match value-to-inplace) %)
                         col))


(defn-spec generate-nginx-deployment map?
  [config websiteconfig?]
  (let [{:keys [unique-name build-cpu-request build-cpu-limit 
                build-memory-request build-memory-limit]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (assoc-in [:metadata :namespace] name)
     (replace-all-matching-substrings-beginning-with "NAME" name)
     (cm/replace-all-matching-values-by-new-value "BUILD_CPU_REQUEST" build-cpu-request)
     (cm/replace-all-matching-values-by-new-value "BUILD_CPU_LIMIT" build-cpu-limit)
     (cm/replace-all-matching-values-by-new-value "BUILD_MEMORY_REQUEST" build-memory-request)
     (cm/replace-all-matching-values-by-new-value "BUILD_MEMORY_LIMIT" build-memory-limit))))


(defn-spec generate-nginx-configmap map?
  [config websiteconfig?]
  (let [{:keys [fqdns unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-configmap.yaml")
     (assoc-in [:metadata :namespace] name)
     (replace-all-matching-substrings-beginning-with "NAME" name)
     (#(assoc-in %
                 [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf) #"FQDN" (str (str/join " " fqdns) ";")))))))


(defn-spec generate-nginx-service map?
  [config websiteconfig?]
  (let [{:keys [unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (assoc-in [:metadata :namespace] name)
     (replace-all-matching-substrings-beginning-with "NAME" name))))


(defn-spec generate-website-build-cron map?
  [config websiteconfig?]
  (let [{:keys [unique-name build-cpu-request build-cpu-limit build-memory-request 
                build-memory-limit]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/website-build-cron.yaml")
     (replace-all-matching-substrings-beginning-with "NAME" name)
     (cm/replace-all-matching-values-by-new-value "BUILD_CPU_REQUEST" build-cpu-request)
     (cm/replace-all-matching-values-by-new-value "BUILD_CPU_LIMIT" build-cpu-limit)
     (cm/replace-all-matching-values-by-new-value "BUILD_MEMORY_REQUEST" build-memory-request)
     (cm/replace-all-matching-values-by-new-value "BUILD_MEMORY_LIMIT" build-memory-limit))))


(defn-spec generate-website-content-volume map?
  [config websiteconfig?]
  (let [{:keys [unique-name volume-size]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/website-content-volume.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] name)
     (replace-all-matching-substrings-beginning-with "NAME" name) 
     (cm/replace-all-matching-values-by-new-value "WEBSITESTORAGESIZE" (str volume-size "Gi")))))


(defn-spec generate-hashfile-volume map?
  [config websiteconfig?]
  (let [{:keys [unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/hashfile-volume.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] name)
     (replace-all-matching-substrings-beginning-with "NAME" name))))


; TODO: Non-Secret-Parts should be config map
(defn-spec generate-website-build-secret pred/map-or-seq?
  [config websiteconfig?
   auth websiteauth?]
  (let [{:keys [unique-name
                forgejo-host
                forgejo-repo
                branchname]} config
        {:keys [authtoken
                username]} auth
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/website-build-secret.yaml")
     (assoc-in [:metadata :labels :app.kubernetes.part-of] name)
     (replace-all-matching-substrings-beginning-with "NAME" name)
     (cm/replace-all-matching-values-by-new-value "TOKEN" (b64/encode authtoken))
     (cm/replace-all-matching-values-by-new-value "REPOURL" (b64/encode
                                                             (generate-gitrepourl
                                                              forgejo-host
                                                              forgejo-repo
                                                              username
                                                              branchname)))
     (cm/replace-all-matching-values-by-new-value "COMMITURL" (b64/encode
                                                               (generate-gitcommiturl
                                                                forgejo-host
                                                                forgejo-repo
                                                                username))))))


#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (get (inline-resources "website") resource-name)))

