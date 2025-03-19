(ns dda.c4k-website.website
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.ingress :as ing]))

(defn fqdn-list?
  [input]
  (every? true? (map cp/fqdn-string? input)))

(s/def ::unique-name string?)
(s/def ::issuer cp/letsencrypt-issuer?)
(s/def ::volume-size cp/integer-string?)
(s/def ::authtoken cp/bash-env-string?)
(s/def ::fqdns (s/coll-of cp/fqdn-string?))
(s/def ::forgejo-host cp/fqdn-string?)
(s/def ::repo-name string?)
(s/def ::branchname string?)
(s/def ::repo-owner string?)
(s/def ::build-cpu-request string?)
(s/def ::build-memory-request string?)
(s/def ::build-cpu-limit string?)
(s/def ::build-memory-limit string?)
(s/def ::redirect (s/tuple string? string?))
(s/def ::redirects (s/coll-of ::redirect))
(s/def ::average-rate ::ing/average-rate)
(s/def ::burst-rate ::ing/burst-rate)

(def websiteconfig? (s/keys :req-un [::unique-name
                                     ::fqdns
                                     ::forgejo-host
                                     ::repo-owner
                                     ::repo-name
                                     ::branchname
                                     ::build-cpu-request
                                     ::build-cpu-limit
                                     ::build-memory-request
                                     ::build-memory-limit
                                     ::issuer
                                     ::volume-size
                                     ::redirects]))

(def website-config-defaults {:build-cpu-request "500m"
                              :build-cpu-limit "1700m"
                              :build-memory-request "256Mi"
                              :build-memory-limit "512Mi"
                              :volume-size "3"
                              :redirects []
                              :average-rate 20
                              :burst-rate 40})

(def websiteauth? (s/keys :req-un [::unique-name ::authtoken]))

(defn-spec replace-dots-by-minus string?
  [fqdn cp/fqdn-string?]
  (str/replace fqdn #"\." "-"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/archive/<branch>.zip
(defn-spec generate-gitrepourl string?
  [host cp/fqdn-string?
   owner string?
   repo string?
   branch string?]
  (str "https://" host "/api/v1/repos/" owner "/" repo "/archive/" branch ".zip"))

; https://your.gitea.host/api/v1/repos/<owner>/<repo>/git/commits/HEAD
(defn-spec generate-gitcommiturl string?
  [host cp/fqdn-string?
   owner string?
   repo string?]
  (str "https://" host "/api/v1/repos/" owner "/" repo "/git/" "commits/" "HEAD"))


(defn-spec replace-all-matching-prefixes map?
  [col map?
   value-to-partly-match string?
   value-to-inplace string?]
  (clojure.walk/postwalk #(if (and (= (type value-to-partly-match) (type %))
                                   (re-matches (re-pattern (str value-to-partly-match ".*")) %))
                            (str/replace % value-to-partly-match value-to-inplace) %)
                         col))


(defn-spec generate-redirects string?
  [config websiteconfig? 
   indent (s/or :pos pos-int? :zero zero?)]
  (let [{:keys [redirects]} config]
    (str/join
     (str "\n" (str/join (take indent (repeat " "))))
     (map 
      #(str "rewrite ^" (first %1) "\\$ " (second %1) " permanent;") 
      redirects))))

(defn-spec generate-nginx-configmap map?
  [config websiteconfig?]
  (let [{:keys [fqdns unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-configmap.yaml")
     (replace-all-matching-prefixes "NAME" name)
     (#(assoc-in % [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf)
                  #"FQDN"
                  (str (str/join " " fqdns) ";"))))
     (#(assoc-in % [:data :website.conf]
                 (str/replace
                  (-> % :data :website.conf)
                  #"REDIRECTS"
                  (generate-redirects config 2)))))))

(defn-spec generate-build-configmap cp/map-or-seq?
  [config websiteconfig?]
  (let [{:keys [unique-name
                forgejo-host
                repo-owner
                repo-name
                branchname]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/build-configmap.yaml")
     (replace-all-matching-prefixes "NAME" name)
     (cm/replace-all-matching "GITHOST" forgejo-host)
     (cm/replace-all-matching "REPOURL" (generate-gitrepourl
                                                              forgejo-host
                                                              repo-owner
                                                              repo-name
                                                              branchname))
     (cm/replace-all-matching "COMMITURL" (generate-gitcommiturl
                                                                forgejo-host
                                                                repo-owner
                                                                repo-name)))))

(defn-spec generate-build-secret cp/map-or-seq?
  [auth websiteauth?]
  (let [{:keys [unique-name
                authtoken]} auth
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/build-secret.yaml")
     (replace-all-matching-prefixes "NAME" name)
     (cm/replace-all-matching "TOKEN" (b64/encode authtoken)))))


(defn-spec generate-content-pvc map?
  [config websiteconfig?]
  (let [{:keys [unique-name volume-size]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/content-pvc.yaml")
     (replace-all-matching-prefixes "NAME" name) 
     (cm/replace-all-matching "WEBSITESTORAGESIZE" (str volume-size "Gi")))))


; TODO: Non-Secret-Parts should be config map
(defn-spec generate-hash-state-pvc map?
  [config websiteconfig?]
  (let [{:keys [unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/hash-state-pvc.yaml")
     (replace-all-matching-prefixes "NAME" name))))


(defn-spec generate-nginx-deployment map?
  [config websiteconfig?]
  (let [{:keys [unique-name build-cpu-request build-cpu-limit 
                build-memory-request build-memory-limit]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-deployment.yaml")
     (assoc-in [:metadata :namespace] name)
     (replace-all-matching-prefixes "NAME" name)
     (cm/replace-all-matching "BUILD_CPU_REQUEST" build-cpu-request)
     (cm/replace-all-matching "BUILD_CPU_LIMIT" build-cpu-limit)
     (cm/replace-all-matching "BUILD_MEMORY_REQUEST" build-memory-request)
     (cm/replace-all-matching "BUILD_MEMORY_LIMIT" build-memory-limit))))


(defn-spec generate-build-cron map?
  [config websiteconfig?]
  (let [{:keys [unique-name build-cpu-request build-cpu-limit build-memory-request 
                build-memory-limit]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/build-cron.yaml")
     (replace-all-matching-prefixes "NAME" name)
     (cm/replace-all-matching "BUILD_CPU_REQUEST" build-cpu-request)
     (cm/replace-all-matching "BUILD_CPU_LIMIT" build-cpu-limit)
     (cm/replace-all-matching "BUILD_MEMORY_REQUEST" build-memory-request)
     (cm/replace-all-matching "BUILD_MEMORY_LIMIT" build-memory-limit))))


(defn-spec generate-nginx-service map?
  [config websiteconfig?]
  (let [{:keys [unique-name]} config
        name (replace-dots-by-minus unique-name)]
    (->
     (yaml/load-as-edn "website/nginx-service.yaml")
     (assoc-in [:metadata :namespace] name)
     (replace-all-matching-prefixes "NAME" name))))

(defn-spec generate-ingress seq?
  [config websiteconfig?]
  (let [name (replace-dots-by-minus (:unique-name config))
        final-config (merge website-config-defaults
                            {:service-name name
                             :service-port 80
                             :namespace name}
                            config)]
    (ing/generate-simple-ingress final-config)))


#?(:cljs
   (defmethod yaml/load-resource :website [resource-name]
     (get (inline-resources "website") resource-name)))

